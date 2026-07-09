package com.example.support.classifier;

// Feature: customer-support-system, Property 8: Classification result completeness and keywords accuracy

import com.example.support.dto.ClassificationResult;
import com.example.support.model.Category;
import com.example.support.model.Priority;
import net.jqwik.api.*;
import net.jqwik.api.constraints.NotBlank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property 8: Classification result completeness and keywords accuracy.
 *
 * <p>Validates: Requirements 11.1, 11.7
 *
 * <p>For any ticket text containing a known set of keywords the ClassificationResult SHALL:
 * <ul>
 *   <li>have a non-null category</li>
 *   <li>have a non-null priority</li>
 *   <li>have a confidence_score in [0.0, 1.0]</li>
 *   <li>have non-null reasoning</li>
 *   <li>have keywords_found ⊆ keywords that actually appear in the ticket text</li>
 * </ul>
 */
class ClassifierResultPropertyTest {

    private final KeywordClassifier classifier = new KeywordClassifier();

    // -------------------------------------------------------------------------
    // All known keywords (flat list for generators)
    // -------------------------------------------------------------------------

    private static final List<String> ALL_PRIORITY_KEYWORDS = KeywordClassifier.PRIORITY_KEYWORDS
            .values().stream()
            .flatMap(List::stream)
            .collect(Collectors.toList());

    private static final List<String> ALL_CATEGORY_KEYWORDS = KeywordClassifier.CATEGORY_KEYWORDS
            .values().stream()
            .flatMap(List::stream)
            .collect(Collectors.toList());

    private static final List<String> ALL_KNOWN_KEYWORDS = Stream
            .concat(ALL_PRIORITY_KEYWORDS.stream(), ALL_CATEGORY_KEYWORDS.stream())
            .collect(Collectors.toList());

    // -------------------------------------------------------------------------
    // Property 8
    // -------------------------------------------------------------------------

    /**
     * Validates: Requirements 11.1, 11.7
     *
     * <p>Generates ticket text by embedding a randomly chosen subset of known keywords
     * into filler sentences. Asserts that the returned ClassificationResult is complete
     * (non-null fields, valid confidence range) and that every keyword in keywords_found
     * actually appears in the combined subject + description text.
     */
    @Property(tries = 100)
    void classificationResultIsCompleteAndKeywordsAreAccurate(
            @ForAll("ticketTextWithKeywords") TicketText ticketText) {

        ClassificationResult result = classifier.classify(ticketText.subject, ticketText.description);

        // Non-null category and priority
        assertThat(result.getCategory())
                .as("category must not be null")
                .isNotNull();

        assertThat(result.getPriority())
                .as("priority must not be null")
                .isNotNull();

        // confidence_score ∈ [0.0, 1.0]
        assertThat(result.getConfidenceScore())
                .as("confidence_score must be >= 0.0")
                .isGreaterThanOrEqualTo(0.0);

        assertThat(result.getConfidenceScore())
                .as("confidence_score must be <= 1.0")
                .isLessThanOrEqualTo(1.0);

        // Non-null reasoning
        assertThat(result.getReasoning())
                .as("reasoning must not be null")
                .isNotNull();

        // keywords_found ⊆ keywords that actually appear in the ticket text
        String combinedText = (ticketText.subject + " " + ticketText.description).toLowerCase();

        if (result.getKeywordsFound() != null) {
            for (String foundKeyword : result.getKeywordsFound()) {
                assertThat(combinedText)
                        .as("keyword '%s' reported in keywords_found must appear in the ticket text",
                                foundKeyword)
                        .contains(foundKeyword.toLowerCase());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Provider: arbitrary ticket text containing known keywords
    // -------------------------------------------------------------------------

    /**
     * Generates a {@link TicketText} by picking a non-empty subset of known keywords
     * and embedding them in subject and description filler sentences.
     */
    @Provide
    Arbitrary<TicketText> ticketTextWithKeywords() {
        // Pick 1–5 keywords from the full known set
        Arbitrary<List<String>> keywordSubsets = Arbitraries.of(ALL_KNOWN_KEYWORDS)
                .list()
                .ofMinSize(1)
                .ofMaxSize(5)
                .uniqueElements();

        // Filler words to pad around keywords
        Arbitrary<String> fillerWords = Arbitraries.of(
                "please", "help", "I", "need", "assistance", "with", "my", "the", "a",
                "ticket", "system", "issue", "problem", "request", "question");

        return keywordSubsets.flatMap(keywords -> {
            // Split keywords between subject and description
            int splitPoint = Math.max(1, keywords.size() / 2);
            List<String> subjectKeywords = keywords.subList(0, splitPoint);
            List<String> descKeywords = keywords.subList(splitPoint, keywords.size());

            String subject = buildSentence(subjectKeywords);
            String description = buildSentence(descKeywords.isEmpty()
                    ? List.of(keywords.get(0)) : descKeywords);

            return Arbitraries.just(new TicketText(subject, description, keywords));
        });
    }

    /**
     * Embeds the given keywords into a simple sentence: "I need help with <kw1> and <kw2>".
     */
    private static String buildSentence(List<String> keywords) {
        return "I need help with " + String.join(" and ", keywords) + " please";
    }

    // -------------------------------------------------------------------------
    // Helper record
    // -------------------------------------------------------------------------

    /**
     * Carries subject, description, and the embedded keywords for assertion.
     */
    static class TicketText {
        final String subject;
        final String description;
        final List<String> embeddedKeywords;

        TicketText(String subject, String description, List<String> embeddedKeywords) {
            this.subject = subject;
            this.description = description;
            this.embeddedKeywords = Collections.unmodifiableList(embeddedKeywords);
        }

        @Override
        public String toString() {
            return "TicketText{subject='" + subject + "', description='" + description
                    + "', embeddedKeywords=" + embeddedKeywords + '}';
        }
    }
}
