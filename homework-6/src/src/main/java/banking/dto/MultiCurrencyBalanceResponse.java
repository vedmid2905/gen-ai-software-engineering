package banking.dto;

import java.util.List;

/**
 * Response DTO for GET /accounts/{accountId}/balance when the account has
 * completed transactions in two or more distinct currencies.
 *
 * Requirements: 7.6
 */
public record MultiCurrencyBalanceResponse(
        String accountId,
        List<CurrencyBalance> balances
) {}
