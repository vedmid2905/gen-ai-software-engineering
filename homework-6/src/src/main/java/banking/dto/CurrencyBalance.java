package banking.dto;

import java.math.BigDecimal;

/**
 * One entry in the multi-currency balance response, representing the computed
 * balance for a single currency.
 *
 * Requirements: 7.6
 */
public record CurrencyBalance(
        String currency,
        BigDecimal balance
) {}
