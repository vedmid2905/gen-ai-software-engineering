package banking.dto;

import java.math.BigDecimal;

/**
 * Response DTO for a single transaction.
 * The timestamp is serialised as an ISO 8601 string rather than an Instant
 * so that the JSON representation is human-readable.
 *
 * Requirements: 1.1, 9.3, 10.1, 10.2
 */
public record TransactionResponse(
        String id,
        String type,
        BigDecimal amount,
        String currency,
        String fromAccount,
        String toAccount,
        String status,
        String timestamp    // ISO 8601 string, e.g. "2024-01-31T10:00:00Z"
) {}
