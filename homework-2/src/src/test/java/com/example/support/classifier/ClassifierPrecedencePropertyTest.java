package com.example.support.classifier;

// Feature: customer-support-system, Property 7: Classifier priority precedence

import com.example.support.dto.ClassificationResult;
import com.example.support.model.Priority;
import net.jqwik.api.*;
import net.jqwik.api.Tuple.Tuple2;
import net.jqwik.api.constraints.NotEmpty;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property 7: Classifier priority precedence
 *
 * For any combination of keywords drawn from different priority tiers present in
 * ticket subject and description text, the Classifier SHALL assign the highest
 * applicable priority tier (urgent > high > low > medium), regardless of keyword
 * order or position in the text.
 *
 * Validates: Requirements 11.2, 11.3, 11.4, 11.5, 11.6
 */
class ClassifierPrecedencePropertyTest {

    private final KeywordClassifier classifier = new KeywordClassifier();

    // Priority tiers in descending order (urgent is highest)
    private static final Priority[] TIER_ORDER = { Priority.urgent, Priority.high, Priority.low, Priority.medium };

    // Keywords per tier (mirrors KeywordClassifier.PRIORITY_KEYWORDS)
    private static final List<String> URGENT_KEYWORDS = List.of("can't access", "critical", "production down", "security");
    private static final List<String> HIGH_KEYWORDS   = List.of("important", "blocking", "asap");
    private static final List<String> LOW_KEYWORDS    = List.of("minor", "cosmetic", "suggestion");

    /**
     * Property 7a: When text contains keywords from multiple tiers,
     * the highest tier always wins regardless of keyword order.
     */
    @Property(tries = 100)
    void highestTierWinsOverLowerTiers(
            @ForAll("multiTierKeywordCombinations") Tuple2<Priority, String> combination) {

        Priority expectedPriority = combination.get1();
        String text = combination.get2();

        ClassificationResult result = classifier.classify(text, "");

        assertThat(result.getPriority())
                .as("Expected priority %s for text: [%s]", expectedPriority, text)
                .isEqualTo(expectedPriority);
    }

    /**
     * Property 7b: A single urgent keyword always results in urgent priority,
     * even when mixed with high/low keywords.
     */
    @Property(tries = 100)
    void urgentKeywordAlwaysBeatsHighAndLow(
            @ForAll("urgentWithLowerTierKeywords") String text) {

        ClassificationResult result = classifier.classify(text, "");

        assertThat(result.getPriority())
                .as("Expected urgent priority for text containing an urgent keyword: [%s]", text)
                .isEqualTo(Priority.urgent);
    }

    /**
     * Property 7c: A high keyword beats low keywords when no urgent keyword is present.
     */
    @Property(tries = 100)
    void highKeywordBeatsLowKeyword(
            @ForAll("highWithLowKeywords") String text) {

        ClassificationResult result = classifier.classify(text, "");

        assertThat(result.getPriority())
                .as("Expected high priority for text containing a high keyword but no urgent keyword: [%s]", text)
                .isEqualTo(Priority.high);
    }

    /**
     * Property 7d: A low keyword results in low priority when no urgent or high keyword is present.
     */
    @Property(tries = 100)
    void lowKeywordWithNoHigherTierGivesLow(
            @ForAll("lowKeywordsOnly") String text) {

        ClassificationResult result = classifier.classify(text, "");

        assertThat(result.getPriority())
                .as("Expected low priority for text containing only low-tier keywords: [%s]", text)
                .isEqualTo(Priority.low);
    }

    /**
     * Property 7e: Keywords can be split across subject and description;
     * the highest tier still wins.
     */
    @Property(tries = 100)
    void keywordsSplitAcrossSubjectAndDescriptionStillRespectPrecedence(
            @ForAll("splitKeywordCombinations") Tuple2<String, String> subjectDescription) {

        String subject = subjectDescription.get1();
        String description = subjectDescription.get2();

        // Determine expected priority by finding highest tier present in either field
        Priority expected = expectedPriorityForText(subject + " " + description);

        ClassificationResult result = classifier.classify(subject, description);

        assertThat(result.getPriority())
                .as("Expected priority %s for subject=[%s], description=[%s]", expected, subject, description)
                .isEqualTo(expected);
    }

    // -------------------------------------------------------------------------
    // Arbitraries (generators)
    // -------------------------------------------------------------------------

    /**
     * Generates a (expectedPriority, text) tuple where text contains keywords from
     * at least two different tiers. The expected priority is the highest tier present.
     */
    @Provide
    Arbitrary<Tuple2<Priority, String>> multiTierKeywordCombinations() {
        // Pick a random highest tier (urgent, high, or low - not medium since that's fallback)
        return Arbitraries.of(Priority.urgent, Priority.high, Priority.low)
                .flatMap(highestTier -> buildTextWithHighestTier(highestTier)
                        .map(text -> Tuple.of(highestTier, text)));
    }

    /**
     * Builds a shuffled text that guarantees the given tier is the highest present,
     * plus at least one keyword from a lower tier.
     */
    private Arbitrary<String> buildTextWithHighestTier(Priority highestTier) {
        List<String> highestTierKws = keywordsForTier(highestTier);
        List<String> lowerTierKws = lowerTierKeywords(highestTier);

        // Pick 1+ keywords from the highest tier
        Arbitrary<List<String>> fromHighest = Arbitraries.of(highestTierKws)
                .list().ofMinSize(1).ofMaxSize(highestTierKws.size());

        // Pick 0–2 keywords from lower tiers (to test that lower tiers don't override)
        Arbitrary<List<String>> fromLower = lowerTierKws.isEmpty()
                ? Arbitraries.just(Collections.emptyList())
                : Arbitraries.of(lowerTierKws).list().ofMinSize(0).ofMaxSize(
                        Math.min(2, lowerTierKws.size()));

        return Combinators.combine(fromHighest, fromLower)
                .as((hi, lo) -> {
                    List<String> all = new ArrayList<>(hi);
                    all.addAll(lo);
                    // Shuffle so keyword order doesn't matter
                    Collections.shuffle(all, new Random());
                    return "ticket text: " + String.join(". ", all) + ".";
                });
    }

    /**
     * Generates text that contains at least one urgent keyword plus optional high/low keywords.
     */
    @Provide
    Arbitrary<String> urgentWithLowerTierKeywords() {
        Arbitrary<String> urgentKw = Arbitraries.of(URGENT_KEYWORDS);
        Arbitrary<List<String>> lowerKws = Arbitraries.of(
                new ArrayList<>(HIGH_KEYWORDS) {{
                    addAll(LOW_KEYWORDS);
                }})
                .list().ofMinSize(0).ofMaxSize(3);

        return Combinators.combine(urgentKw, lowerKws)
                .as((urgent, lower) -> {
                    List<String> all = new ArrayList<>(lower);
                    all.add(urgent);
                    Collections.shuffle(all, new Random());
                    return String.join(" and ", all);
                });
    }

    /**
     * Generates text that contains at least one high-tier keyword plus optional low keywords,
     * but NO urgent keywords.
     */
    @Provide
    Arbitrary<String> highWithLowKeywords() {
        Arbitrary<String> highKw = Arbitraries.of(HIGH_KEYWORDS);
        Arbitrary<List<String>> lowKws = Arbitraries.of(LOW_KEYWORDS)
                .list().ofMinSize(0).ofMaxSize(3);

        return Combinators.combine(highKw, lowKws)
                .as((high, lows) -> {
                    List<String> all = new ArrayList<>(lows);
                    all.add(high);
                    Collections.shuffle(all, new Random());
                    return String.join(" and ", all);
                });
    }

    /**
     * Generates text that contains only low-tier keywords (no urgent or high keywords).
     */
    @Provide
    Arbitrary<String> lowKeywordsOnly() {
        return Arbitraries.of(LOW_KEYWORDS)
                .list().ofMinSize(1).ofMaxSize(LOW_KEYWORDS.size())
                .map(kws -> String.join(", ", kws));
    }

    /**
     * Generates (subject, description) pairs where keywords are distributed across both fields
     * and includes at least one non-medium tier keyword.
     */
    @Provide
    Arbitrary<Tuple2<String, String>> splitKeywordCombinations() {
        List<String> allTieredKws = new ArrayList<>(URGENT_KEYWORDS);
        allTieredKws.addAll(HIGH_KEYWORDS);
        allTieredKws.addAll(LOW_KEYWORDS);

        // Pick 1–4 keywords total
        Arbitrary<List<String>> selectedKws = Arbitraries.of(allTieredKws)
                .list().ofMinSize(1).ofMaxSize(4)
                .uniqueElements();

        return selectedKws.flatMap(kws -> {
            // Randomly split the keywords across subject and description
            int splitPoint = kws.isEmpty() ? 0 : new Random().nextInt(kws.size() + 1);
            List<String> subjectKws = kws.subList(0, splitPoint);
            List<String> descKws = kws.subList(splitPoint, kws.size());

            String subject = subjectKws.isEmpty() ? "support request" : String.join(" and ", subjectKws);
            String description = descKws.isEmpty() ? "please help" : String.join(". ", descKws);

            return Arbitraries.just(Tuple.of(subject, description));
        });
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private List<String> keywordsForTier(Priority tier) {
        switch (tier) {
            case urgent:
                return URGENT_KEYWORDS;
            case high:
                return HIGH_KEYWORDS;
            case low:
                return LOW_KEYWORDS;
            case medium:
                return Collections.emptyList();
            default:
                return Collections.emptyList();
        }
    }

    /**
     * Returns keywords from all tiers that are lower than the given tier.
     * Tier order: urgent > high > low > medium
     */
    private List<String> lowerTierKeywords(Priority tier) {
        List<String> lower = new ArrayList<>();
        boolean collecting = false;
        for (Priority t : TIER_ORDER) {
            if (t == tier) {
                collecting = true;
                continue;
            }
            if (collecting && t != Priority.medium) {
                lower.addAll(keywordsForTier(t));
            }
        }
        return lower;
    }

    /**
     * Determines the expected priority for a combined text by scanning tiers in order.
     */
    private Priority expectedPriorityForText(String text) {
        String lower = text.toLowerCase();
        for (String kw : URGENT_KEYWORDS) {
            if (lower.contains(kw)) return Priority.urgent;
        }
        for (String kw : HIGH_KEYWORDS) {
            if (lower.contains(kw)) return Priority.high;
        }
        for (String kw : LOW_KEYWORDS) {
            if (lower.contains(kw)) return Priority.low;
        }
        return Priority.medium;
    }
}
