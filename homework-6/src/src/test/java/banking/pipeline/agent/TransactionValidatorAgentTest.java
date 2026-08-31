package banking.pipeline.agent;

import banking.pipeline.domain.ProcessingResult;
import banking.pipeline.domain.TransactionRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionValidatorAgentTest {

    private final TransactionValidatorAgent agent = new TransactionValidatorAgent();

    private TransactionRecord validTransaction() {
        return new TransactionRecord(
                "TXN001", Instant.parse("2026-03-16T09:00:00Z"),
                "ACC-1001", "ACC-2001", new BigDecimal("1500.00"), "USD",
                "transfer", "Monthly rent payment", Map.of("channel", "online", "country", "US"));
    }

    @Test
    void validTransactionIsValidated() {
        ProcessingResult result = agent.validateTransaction(validTransaction());

        assertThat(result.status()).isEqualTo("validated");
        assertThat(result.decision()).isEqualTo("valid");
        assertThat(result.reason()).isNull();
        assertThat(result.audit().agent()).isEqualTo("transaction_validator");
    }

    @Test
    void nonPositiveAmountIsRejected() {
        TransactionRecord tx = new TransactionRecord(
                "TXN007", Instant.parse("2026-03-16T10:10:00Z"),
                "ACC-1007", "ACC-8800", new BigDecimal("-100.00"), "GBP",
                "refund", "Refund", Map.of("channel", "online", "country", "GB"));

        ProcessingResult result = agent.validateTransaction(tx);

        assertThat(result.status()).isEqualTo("rejected");
        assertThat(result.reason()).contains("amount must be greater than zero");
    }

    @Test
    void unsupportedCurrencyIsRejected() {
        TransactionRecord tx = new TransactionRecord(
                "TXN006", Instant.parse("2026-03-16T10:05:00Z"),
                "ACC-1006", "ACC-7700", new BigDecimal("200.00"), "XYZ",
                "transfer", "Test payment", Map.of("channel", "online", "country", "US"));

        ProcessingResult result = agent.validateTransaction(tx);

        assertThat(result.status()).isEqualTo("rejected");
        assertThat(result.reason()).contains("not a supported ISO 4217 code");
    }

    @Test
    void missingRequiredFieldsAreRejected() {
        TransactionRecord tx = new TransactionRecord(
                null, null, null, null, null, null, null, null, null);

        ProcessingResult result = agent.validateTransaction(tx);

        assertThat(result.status()).isEqualTo("rejected");
        assertThat(result.reason())
                .contains("transaction_id is required")
                .contains("timestamp is required")
                .contains("source_account is missing or malformed")
                .contains("destination_account is missing or malformed")
                .contains("amount is required");
    }

    @Test
    void malformedAccountReferenceIsRejected() {
        TransactionRecord tx = new TransactionRecord(
                "TXN010", Instant.now(), "not-an-account", "ACC-2001",
                new BigDecimal("100.00"), "USD", "transfer", "desc", Map.of());

        ProcessingResult result = agent.validateTransaction(tx);

        assertThat(result.status()).isEqualTo("rejected");
        assertThat(result.reason()).contains("source_account is missing or malformed");
    }
}
