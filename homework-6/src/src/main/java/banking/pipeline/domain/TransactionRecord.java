package banking.pipeline.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Raw transaction as read from sample-transactions.json.
 */
public record TransactionRecord(
        String transactionId,
        Instant timestamp,
        String sourceAccount,
        String destinationAccount,
        BigDecimal amount,
        String currency,
        String transactionType,
        String description,
        Map<String, String> metadata
) {
}
