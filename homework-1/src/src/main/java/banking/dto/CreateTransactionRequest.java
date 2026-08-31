package banking.dto;

import java.math.BigDecimal;

/**
 * Request DTO for POST /transactions.
 * The server always generates id, status, and timestamp — they are never accepted from the caller.
 *
 * Requirements: 1.1, 12.3
 */
public record CreateTransactionRequest(
        String type,
        BigDecimal amount,
        String currency,
        String fromAccount,
        String toAccount
) {}
