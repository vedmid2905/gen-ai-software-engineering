package com.example.support.classifier;

import com.example.support.dto.ClassificationResult;
import com.example.support.model.Category;
import com.example.support.model.Priority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link KeywordClassifier}.
 *
 * Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.6, 11.7
 */
class KeywordClassifierTest {

    private KeywordClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new KeywordClassifier();
    }

    // =========================================================================
    // Priority tier tests (Requirements 11.2, 11.3, 11.4, 11.5, 11.6)
    // =========================================================================

    @Nested
    @DisplayName("Priority: urgent tier")
    class UrgentPriority {

        @Test
        @DisplayName("'can't access' triggers urgent")
        void cantAccessTriggersUrgent() {
            ClassificationResult result = classifier.classify("I can't access my account", "");
            assertThat(result.getPriority()).isEqualTo(Priority.urgent);
        }

        @Test
        @DisplayName("'critical' triggers urgent")
        void criticalTriggersUrgent() {
            ClassificationResult result = classifier.classify("Critical issue found", "");
            assertThat(result.getPriority()).isEqualTo(Priority.urgent);
        }

        @Test
        @DisplayName("'production down' triggers urgent")
        void productionDownTriggersUrgent() {
            ClassificationResult result = classifier.classify("Production down since 2am", "");
            assertThat(result.getPriority()).isEqualTo(Priority.urgent);
        }

        @Test
        @DisplayName("'security' triggers urgent")
        void securityTriggersUrgent() {
            ClassificationResult result = classifier.classify("Security vulnerability detected", "");
            assertThat(result.getPriority()).isEqualTo(Priority.urgent);
        }

        @Test
        @DisplayName("urgent keyword in description also triggers urgent")
        void urgentKeywordInDescriptionTriggersUrgent() {
            ClassificationResult result = classifier.classify("Login issue", "This is critical for our operations");
            assertThat(result.getPriority()).isEqualTo(Priority.urgent);
        }
    }

    @Nested
    @DisplayName("Priority: high tier")
    class HighPriority {

        @Test
        @DisplayName("'important' triggers high")
        void importantTriggersHigh() {
            ClassificationResult result = classifier.classify("Important update needed", "");
            assertThat(result.getPriority()).isEqualTo(Priority.high);
        }

        @Test
        @DisplayName("'blocking' triggers high")
        void blockingTriggersHigh() {
            ClassificationResult result = classifier.classify("Blocking our release", "");
            assertThat(result.getPriority()).isEqualTo(Priority.high);
        }

        @Test
        @DisplayName("'asap' triggers high")
        void asapTriggersHigh() {
            ClassificationResult result = classifier.classify("Need fix asap", "");
            assertThat(result.getPriority()).isEqualTo(Priority.high);
        }

        @Test
        @DisplayName("high keyword in description also triggers high")
        void highKeywordInDescriptionTriggersHigh() {
            ClassificationResult result = classifier.classify("Feature request", "This is important for our workflow");
            assertThat(result.getPriority()).isEqualTo(Priority.high);
        }
    }

    @Nested
    @DisplayName("Priority: low tier")
    class LowPriority {

        @Test
        @DisplayName("'minor' triggers low")
        void minorTriggersLow() {
            ClassificationResult result = classifier.classify("Minor display issue", "");
            assertThat(result.getPriority()).isEqualTo(Priority.low);
        }

        @Test
        @DisplayName("'cosmetic' triggers low")
        void cosmeticTriggersLow() {
            ClassificationResult result = classifier.classify("Cosmetic change needed", "");
            assertThat(result.getPriority()).isEqualTo(Priority.low);
        }

        @Test
        @DisplayName("'suggestion' triggers low")
        void suggestionTriggersLow() {
            ClassificationResult result = classifier.classify("Just a suggestion", "");
            assertThat(result.getPriority()).isEqualTo(Priority.low);
        }
    }

    @Nested
    @DisplayName("Priority: medium fallback")
    class MediumPriorityFallback {

        @Test
        @DisplayName("no matching keyword falls back to medium")
        void noKeywordFallsBackToMedium() {
            ClassificationResult result = classifier.classify("General question about the product", "I need some help please");
            assertThat(result.getPriority()).isEqualTo(Priority.medium);
        }

        @Test
        @DisplayName("empty text falls back to medium")
        void emptyTextFallsBackToMedium() {
            ClassificationResult result = classifier.classify("", "");
            assertThat(result.getPriority()).isEqualTo(Priority.medium);
        }

        @Test
        @DisplayName("null subject and description falls back to medium")
        void nullInputsFallBackToMedium() {
            ClassificationResult result = classifier.classify(null, null);
            assertThat(result.getPriority()).isEqualTo(Priority.medium);
        }
    }

    // =========================================================================
    // Category keyword bucket tests (Requirements 11.1, 11.7)
    // =========================================================================

    @Nested
    @DisplayName("Category: account_access")
    class AccountAccessCategory {

        @Test
        @DisplayName("'login' maps to account_access")
        void loginMapsToAccountAccess() {
            ClassificationResult result = classifier.classify("Can't login to my account", "");
            assertThat(result.getCategory()).isEqualTo(Category.account_access);
        }

        @Test
        @DisplayName("'password' maps to account_access")
        void passwordMapsToAccountAccess() {
            ClassificationResult result = classifier.classify("Forgot my password", "");
            assertThat(result.getCategory()).isEqualTo(Category.account_access);
        }

        @Test
        @DisplayName("'account' maps to account_access")
        void accountMapsToAccountAccess() {
            ClassificationResult result = classifier.classify("My account is suspended", "");
            assertThat(result.getCategory()).isEqualTo(Category.account_access);
        }

        @Test
        @DisplayName("'locked' maps to account_access")
        void lockedMapsToAccountAccess() {
            ClassificationResult result = classifier.classify("Account locked after failed attempts", "");
            assertThat(result.getCategory()).isEqualTo(Category.account_access);
        }
    }

    @Nested
    @DisplayName("Category: technical_issue")
    class TechnicalIssueCategory {

        @Test
        @DisplayName("'error' maps to technical_issue")
        void errorMapsToTechnicalIssue() {
            ClassificationResult result = classifier.classify("Getting an error message", "");
            assertThat(result.getCategory()).isEqualTo(Category.technical_issue);
        }

        @Test
        @DisplayName("'crash' maps to technical_issue")
        void crashMapsToTechnicalIssue() {
            ClassificationResult result = classifier.classify("App crash on startup", "");
            assertThat(result.getCategory()).isEqualTo(Category.technical_issue);
        }

        @Test
        @DisplayName("'not working' maps to technical_issue")
        void notWorkingMapsToTechnicalIssue() {
            ClassificationResult result = classifier.classify("Feature is not working", "");
            assertThat(result.getCategory()).isEqualTo(Category.technical_issue);
        }

        @Test
        @DisplayName("'broken' maps to technical_issue")
        void brokenMapsToTechnicalIssue() {
            ClassificationResult result = classifier.classify("Something is broken on the page", "");
            assertThat(result.getCategory()).isEqualTo(Category.technical_issue);
        }
    }

    @Nested
    @DisplayName("Category: billing_question")
    class BillingQuestionCategory {

        @Test
        @DisplayName("'invoice' maps to billing_question")
        void invoiceMapsToaBillingQuestion() {
            ClassificationResult result = classifier.classify("Invoice not received", "");
            assertThat(result.getCategory()).isEqualTo(Category.billing_question);
        }

        @Test
        @DisplayName("'charge' maps to billing_question")
        void chargeMapsToaBillingQuestion() {
            ClassificationResult result = classifier.classify("Unexpected charge on my bill", "");
            assertThat(result.getCategory()).isEqualTo(Category.billing_question);
        }

        @Test
        @DisplayName("'payment' maps to billing_question")
        void paymentMapsToaBillingQuestion() {
            ClassificationResult result = classifier.classify("Payment failed", "");
            assertThat(result.getCategory()).isEqualTo(Category.billing_question);
        }

        @Test
        @DisplayName("'refund' maps to billing_question")
        void refundMapsToaBillingQuestion() {
            ClassificationResult result = classifier.classify("Request a refund for my order", "");
            assertThat(result.getCategory()).isEqualTo(Category.billing_question);
        }
    }

    @Nested
    @DisplayName("Category: feature_request")
    class FeatureRequestCategory {

        @Test
        @DisplayName("'feature' maps to feature_request")
        void featureMapsToFeatureRequest() {
            ClassificationResult result = classifier.classify("New feature idea", "");
            assertThat(result.getCategory()).isEqualTo(Category.feature_request);
        }

        @Test
        @DisplayName("'enhancement' maps to feature_request")
        void enhancementMapsToFeatureRequest() {
            ClassificationResult result = classifier.classify("Enhancement to the UI", "");
            assertThat(result.getCategory()).isEqualTo(Category.feature_request);
        }

        @Test
        @DisplayName("'would like' maps to feature_request")
        void wouldLikeMapsToFeatureRequest() {
            ClassificationResult result = classifier.classify("I would like a dark mode option", "");
            assertThat(result.getCategory()).isEqualTo(Category.feature_request);
        }

        @Test
        @DisplayName("'can you add' maps to feature_request")
        void canYouAddMapsToFeatureRequest() {
            ClassificationResult result = classifier.classify("Can you add export to PDF?", "");
            assertThat(result.getCategory()).isEqualTo(Category.feature_request);
        }
    }

    @Nested
    @DisplayName("Category: bug_report")
    class BugReportCategory {

        @Test
        @DisplayName("'bug' maps to bug_report")
        void bugMapsToBugReport() {
            ClassificationResult result = classifier.classify("Found a bug in checkout", "");
            assertThat(result.getCategory()).isEqualTo(Category.bug_report);
        }

        @Test
        @DisplayName("'defect' maps to bug_report")
        void defectMapsToBugReport() {
            ClassificationResult result = classifier.classify("Defect in the payment flow", "");
            assertThat(result.getCategory()).isEqualTo(Category.bug_report);
        }

        @Test
        @DisplayName("'regression' maps to bug_report")
        void regressionMapsToBugReport() {
            ClassificationResult result = classifier.classify("Regression after last release", "");
            assertThat(result.getCategory()).isEqualTo(Category.bug_report);
        }

        @Test
        @DisplayName("'unexpected' maps to bug_report")
        void unexpectedMapsToBugReport() {
            ClassificationResult result = classifier.classify("Unexpected behavior in the app", "");
            assertThat(result.getCategory()).isEqualTo(Category.bug_report);
        }
    }

    @Nested
    @DisplayName("Category: other fallback")
    class OtherCategoryFallback {

        @Test
        @DisplayName("no keyword match falls back to other")
        void noKeywordFallsBackToOther() {
            ClassificationResult result = classifier.classify("General question", "Please help me understand");
            assertThat(result.getCategory()).isEqualTo(Category.other);
        }

        @Test
        @DisplayName("empty text falls back to other")
        void emptyTextFallsBackToOther() {
            ClassificationResult result = classifier.classify("", "");
            assertThat(result.getCategory()).isEqualTo(Category.other);
        }
    }

    // =========================================================================
    // Mixed-tier priority tie-breaking (Requirement 11.6)
    // =========================================================================

    @Nested
    @DisplayName("Mixed-tier priority tie-breaking")
    class MixedTierTieBreaking {

        @Test
        @DisplayName("urgent beats high when both present")
        void urgentBeatsHigh() {
            ClassificationResult result = classifier.classify("Critical issue - blocking our release", "");
            assertThat(result.getPriority()).isEqualTo(Priority.urgent);
        }

        @Test
        @DisplayName("urgent beats low when both present")
        void urgentBeatsLow() {
            ClassificationResult result = classifier.classify("Minor cosmetic issue but security risk found", "");
            assertThat(result.getPriority()).isEqualTo(Priority.urgent);
        }

        @Test
        @DisplayName("urgent beats medium fallback")
        void urgentBeatsMedium() {
            ClassificationResult result = classifier.classify("Production down on our server", "No other keywords here");
            assertThat(result.getPriority()).isEqualTo(Priority.urgent);
        }

        @Test
        @DisplayName("high beats low when both present")
        void highBeatsLow() {
            ClassificationResult result = classifier.classify("Important but minor suggestion", "");
            assertThat(result.getPriority()).isEqualTo(Priority.high);
        }

        @Test
        @DisplayName("high beats medium fallback")
        void highBeatsMedium() {
            ClassificationResult result = classifier.classify("Need this asap", "No specific keywords here");
            assertThat(result.getPriority()).isEqualTo(Priority.high);
        }

        @Test
        @DisplayName("low beats medium fallback")
        void lowBeatsMedium() {
            ClassificationResult result = classifier.classify("Just a cosmetic suggestion", "Nothing urgent here");
            assertThat(result.getPriority()).isEqualTo(Priority.low);
        }

        @Test
        @DisplayName("urgent wins with all four tiers of keywords present")
        void urgentWinsAllFourTiers() {
            // Contains keywords from all tiers
            ClassificationResult result = classifier.classify(
                    "Critical production down issue - blocking and important minor suggestion",
                    "");
            assertThat(result.getPriority()).isEqualTo(Priority.urgent);
        }
    }

    // =========================================================================
    // Edge cases: empty text, no keywords (Requirements 11.1, 11.7)
    // =========================================================================

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("empty subject and description yields medium priority and other category")
        void emptyTextYieldsDefaults() {
            ClassificationResult result = classifier.classify("", "");
            assertThat(result.getPriority()).isEqualTo(Priority.medium);
            assertThat(result.getCategory()).isEqualTo(Category.other);
        }

        @Test
        @DisplayName("null subject and description yields medium priority and other category")
        void nullTextYieldsDefaults() {
            ClassificationResult result = classifier.classify(null, null);
            assertThat(result.getPriority()).isEqualTo(Priority.medium);
            assertThat(result.getCategory()).isEqualTo(Category.other);
        }

        @Test
        @DisplayName("text with no keywords yields medium priority and other category")
        void textWithNoKeywordsYieldsDefaults() {
            ClassificationResult result = classifier.classify("Hello team", "I have a question about the product");
            assertThat(result.getPriority()).isEqualTo(Priority.medium);
            assertThat(result.getCategory()).isEqualTo(Category.other);
        }

        @Test
        @DisplayName("keywords_found is empty when no keywords match")
        void noKeywordsFoundWhenNoneMatch() {
            ClassificationResult result = classifier.classify("Hello team", "I have a question");
            assertThat(result.getKeywordsFound()).isEmpty();
        }

        @Test
        @DisplayName("keywords_found contains matched keywords")
        void keywordsFoundContainsMatchedKeywords() {
            ClassificationResult result = classifier.classify("Critical login failure", "");
            assertThat(result.getKeywordsFound())
                    .contains("critical", "login");
        }

        @Test
        @DisplayName("keywords_found only contains keywords present in the text")
        void keywordsFoundOnlyContainsActualMatches() {
            ClassificationResult result = classifier.classify("Payment failed unexpectedly", "");
            List<String> found = result.getKeywordsFound();
            String combinedText = "payment failed unexpectedly";
            for (String kw : found) {
                assertThat(combinedText).contains(kw.toLowerCase());
            }
        }

        @Test
        @DisplayName("matching is case-insensitive")
        void matchingIsCaseInsensitive() {
            ClassificationResult result = classifier.classify("CRITICAL SECURITY ISSUE", "");
            assertThat(result.getPriority()).isEqualTo(Priority.urgent);
        }

        @Test
        @DisplayName("keyword match works across subject and description together")
        void keywordMatchWorksAcrossSubjectAndDescription() {
            ClassificationResult result = classifier.classify("Login help", "I forgot my password and account is locked");
            assertThat(result.getCategory()).isEqualTo(Category.account_access);
            // multiple keywords from account_access: login, password, account, locked
            assertThat(result.getKeywordsFound())
                    .containsAnyOf("login", "password", "account", "locked");
        }

        @Test
        @DisplayName("confidence_score is within [0.0, 1.0]")
        void confidenceScoreIsInRange() {
            ClassificationResult result = classifier.classify("Critical production down security login password error", "");
            assertThat(result.getConfidenceScore()).isBetween(0.0, 1.0);
        }

        @Test
        @DisplayName("confidence_score is 0.0 for empty text")
        void confidenceScoreIsZeroForEmptyText() {
            ClassificationResult result = classifier.classify("", "");
            assertThat(result.getConfidenceScore()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("reasoning is non-null")
        void reasoningIsNonNull() {
            ClassificationResult result = classifier.classify("Some subject", "Some description");
            assertThat(result.getReasoning()).isNotNull();
        }

        @Test
        @DisplayName("reasoning mentions no keywords for empty text")
        void reasoningMentionsNoKeywordsForEmptyText() {
            ClassificationResult result = classifier.classify("", "");
            assertThat(result.getReasoning()).contains("No keywords detected");
        }

        @Test
        @DisplayName("category tie is broken by declaration order (account_access before technical_issue)")
        void categoryTieBreaksByDeclarationOrder() {
            // "login" hits account_access; "error" hits technical_issue — equal counts (1 each)
            // account_access is declared first, so it should win
            ClassificationResult result = classifier.classify("login error", "");
            assertThat(result.getCategory()).isEqualTo(Category.account_access);
        }
    }
}
