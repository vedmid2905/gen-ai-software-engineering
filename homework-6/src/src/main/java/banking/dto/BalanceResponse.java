package banking.dto;

import java.math.BigDecimal;

/**
 * Response DTO for GET /accounts/{accountId}/balance when the account has
 * transactions in exactly one currency.
 *
 * Requirements: 7.2
 */
public record BalanceResponse(
        String accountId,
        BigDecimal balance,
        String currency
) {}
