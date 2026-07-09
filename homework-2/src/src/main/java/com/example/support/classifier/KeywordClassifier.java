package com.example.support.classifier;

import com.example.support.dto.ClassificationResult;
import com.example.support.model.Category;
import com.example.support.model.Priority;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Keyword-based implementation of {@link Classifier}.
 *
 * <p>Priority tier resolution (highest wins): urgent &gt; high &gt; low &gt; medium (fallback).
 * Category is determined by the bucket with the most matching keywords; ties break
 * on declaration order; no match falls back to {@code other}.
 *
 * <p>Confidence score = matched keywords / total possible keywords across all tables,
 * capped at 1.0.
 */
@Component
public class KeywordClassifier implements Classifier {

    // -------------------------------------------------------------------------
    // Priority keyword tables (ordered: urgent first so highest tier wins)
    // -------------------------------------------------------------------------

    /** Maps each priority tier to its trigger keywords. */
    public static final Map<Priority, List<String>> PRIORITY_KEYWORDS;

    static {
        // LinkedHashMap preserves declaration order — urgent must be first so
        // the "highest tier wins" scan can short-circuit early if needed.
        PRIORITY_KEYWORDS = new LinkedHashMap<>();
        PRIORITY_KEYWORDS.put(Priority.urgent, List.of("can't access", "critical", "production down", "security"));
        PRIORITY_KEYWORDS.put(Priority.high,   List.of("important", "blocking", "asap"));
        PRIORITY_KEYWORDS.put(Priority.low,    List.of("minor", "cosmetic", "suggestion"));
        // medium is the implicit fallback — no keywords needed
    }

    // -------------------------------------------------------------------------
    // Category keyword tables (declaration order determines tie-break)
    // -------------------------------------------------------------------------

    /** Maps each category to its trigger keywords. */
    public static final Map<Category, List<String>> CATEGORY_KEYWORDS;

    static {
        CATEGORY_KEYWORDS = new LinkedHashMap<>();
        CATEGORY_KEYWORDS.put(Category.account_access,    List.of("login", "password", "account", "locked"));
        CATEGORY_KEYWORDS.put(Category.technical_issue,   List.of("error", "crash", "not working", "broken"));
        CATEGORY_KEYWORDS.put(Category.billing_question,  List.of("invoice", "charge", "payment", "refund"));
        CATEGORY_KEYWORDS.put(Category.feature_request,   List.of("feature", "enhancement", "would like", "can you add"));
        CATEGORY_KEYWORDS.put(Category.bug_report,        List.of("bug", "defect", "regression", "unexpected"));
    }

    // -------------------------------------------------------------------------
    // Total keyword count (used for confidence score denominator)
    // -------------------------------------------------------------------------

    private static final int TOTAL_KEYWORD_COUNT;

    static {
        int count = 0;
        for (List<String> kws : PRIORITY_KEYWORDS.values()) {
            count += kws.size();
        }
        for (List<String> kws : CATEGORY_KEYWORDS.values()) {
            count += kws.size();
        }
        TOTAL_KEYWORD_COUNT = count;
    }

    // -------------------------------------------------------------------------
    // classify()
    // -------------------------------------------------------------------------

    @Override
    public ClassificationResult classify(String subject, String description) {
        // Combine subject and description into a single lower-case search text.
        String subjectSafe      = subject     != null ? subject     : "";
        String descriptionSafe  = description != null ? description : "";
        String combinedLower    = (subjectSafe + " " + descriptionSafe).toLowerCase();

        // --- Priority resolution -------------------------------------------
        Priority assignedPriority = resolvePriority(combinedLower);

        // --- Category resolution -------------------------------------------
        CategoryMatch categoryMatch = resolveCategory(combinedLower);

        // --- Collect all matched keywords (priority + category) -----------
        List<String> keywordsFound = collectAllKeywords(combinedLower);

        // --- Confidence score ---------------------------------------------
        // (number of unique matched keywords across all tables) / total keywords,
        // capped at 1.0.
        double confidenceScore = TOTAL_KEYWORD_COUNT == 0
                ? 0.0
                : Math.min(1.0, (double) keywordsFound.size() / TOTAL_KEYWORD_COUNT);

        // --- Reasoning string --------------------------------------------
        String reasoning = buildReasoning(assignedPriority, categoryMatch.category, keywordsFound);

        // --- Assemble result ---------------------------------------------
        ClassificationResult result = new ClassificationResult();
        result.setPriority(assignedPriority);
        result.setCategory(categoryMatch.category);
        result.setConfidenceScore(confidenceScore);
        result.setReasoning(reasoning);
        result.setKeywordsFound(keywordsFound);
        // ticketId is not known at classification time; caller sets it if needed
        return result;
    }

    // -------------------------------------------------------------------------
    // Priority helpers
    // -------------------------------------------------------------------------

    /**
     * Scans the combined lower-case text for priority keywords in tier order
     * (urgent → high → low). Assigns the highest tier that has at least one match.
     * Falls back to {@code medium} when nothing matches.
     */
    private Priority resolvePriority(String combinedLower) {
        // Tiers in descending priority order
        Priority[] tiersInOrder = { Priority.urgent, Priority.high, Priority.low };
        for (Priority tier : tiersInOrder) {
            List<String> keywords = PRIORITY_KEYWORDS.get(tier);
            for (String kw : keywords) {
                if (combinedLower.contains(kw)) {
                    return tier;
                }
            }
        }
        return Priority.medium;
    }

    // -------------------------------------------------------------------------
    // Category helpers
    // -------------------------------------------------------------------------

    private static final class CategoryMatch {
        final Category category;
        final int matchCount;

        CategoryMatch(Category category, int matchCount) {
            this.category   = category;
            this.matchCount = matchCount;
        }
    }

    /**
     * Counts matches per category bucket. The bucket with the highest count wins.
     * Ties are broken by declaration order (LinkedHashMap iteration order).
     * Falls back to {@code other} when no keyword matches.
     */
    private CategoryMatch resolveCategory(String combinedLower) {
        Category bestCategory  = Category.other;
        int      bestCount     = 0;
        Category tiedCategory = null;

        for (Map.Entry<Category, List<String>> entry : CATEGORY_KEYWORDS.entrySet()) {
            int count = 0;
            for (String kw : entry.getValue()) {
                if (combinedLower.contains(kw)) {
                    count++;
                }
            }
            if (count > bestCount) {
                bestCount = count;
                bestCategory = entry.getKey();
                tiedCategory = null;
            } else if (count == bestCount && count > 0) {
                tiedCategory = entry.getKey();
            }
        }

        if (bestCount > 0 && tiedCategory != null) {
            bestCategory = resolveTie(combinedLower, bestCategory, tiedCategory);
        }

        return new CategoryMatch(bestCategory, bestCount);
    }

    private Category resolveTie(String combinedLower, Category currentBest, Category tiedCategory) {
        boolean currentIsBug = currentBest == Category.bug_report;
        boolean tiedIsBug = tiedCategory == Category.bug_report;
        boolean currentIsBilling = currentBest == Category.billing_question;
        boolean tiedIsBilling = tiedCategory == Category.billing_question;

        if (currentIsBug && tiedIsBilling && containsAny(combinedLower, "defect", "bug", "regression")) {
            return Category.bug_report;
        }
        if (currentIsBilling && tiedIsBug && containsAny(combinedLower, "defect", "bug", "regression")) {
            return Category.bug_report;
        }
        if (currentIsBug && tiedIsBilling && containsAny(combinedLower, "charge", "invoice", "payment", "refund")) {
            return Category.billing_question;
        }
        if (currentIsBilling && tiedIsBug && containsAny(combinedLower, "charge", "invoice", "payment", "refund")) {
            return Category.billing_question;
        }

        return currentBest;
    }

    private boolean containsAny(String text, String... values) {
        for (String value : values) {
            if (text.contains(value)) {
                return true;
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Keyword collection
    // -------------------------------------------------------------------------

    /**
     * Returns a deduplicated, insertion-ordered list of every keyword (from both
     * priority and category tables) that appears in the combined lower-case text.
     */
    private List<String> collectAllKeywords(String combinedLower) {
        List<String> found = new ArrayList<>();

        for (List<String> keywords : PRIORITY_KEYWORDS.values()) {
            for (String kw : keywords) {
                if (combinedLower.contains(kw) && !found.contains(kw)) {
                    found.add(kw);
                }
            }
        }
        for (List<String> keywords : CATEGORY_KEYWORDS.values()) {
            for (String kw : keywords) {
                if (combinedLower.contains(kw) && !found.contains(kw)) {
                    found.add(kw);
                }
            }
        }

        return found;
    }

    // -------------------------------------------------------------------------
    // Reasoning builder
    // -------------------------------------------------------------------------

    private String buildReasoning(Priority priority, Category category, List<String> keywordsFound) {
        if (keywordsFound.isEmpty()) {
            return String.format(
                    "No keywords detected. Assigned default priority '%s' and category '%s'.",
                    priority, category);
        }
        return String.format(
                "Detected keywords %s. Assigned priority '%s' and category '%s'.",
                keywordsFound, priority, category);
    }
}
