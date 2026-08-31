package banking.dto;

import java.math.BigDecimal;

/**
 * Response DTO for GET /accounts/{accountId}/summary.
 *
 * Requirements: 8.1, 8.4
 */
public record AccountSummaryResponse(
        String accountId,
        BigDecimal totalDeposits,
        BigDecimal totalWithdrawals,
        int transactionCount,
        String mostRecentTransactionDate   // ISO 8601 string
) {}
