package banking.service;

import banking.domain.Transaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure stateless component that computes an account's balance, partitioned
 * by currency, from a list of completed transactions.
 *
 * Balance formula per currency:
 * {@code balance = Σ amount (toAccount == accountId) - Σ amount (fromAccount == accountId)}
 * rounded to 2 decimal places using {@link RoundingMode#HALF_UP}.
 *
 * Requirements: 7.1, 7.5, 7.6
 */
@Component
public class BalanceCalculator {

    public Map<String, BigDecimal> computeByCurrency(String accountId, List<Transaction> completedTransactions) {
        Map<String, BigDecimal> balances = new LinkedHashMap<>();
        for (Transaction t : completedTransactions) {
            if (accountId.equals(t.toAccount())) {
                balances.merge(t.currency(), t.amount(), BigDecimal::add);
            }
            if (accountId.equals(t.fromAccount())) {
                balances.merge(t.currency(), t.amount().negate(), BigDecimal::add);
            }
        }
        balances.replaceAll((currency, balance) -> balance.setScale(2, RoundingMode.HALF_UP));
        return balances;
    }
}
