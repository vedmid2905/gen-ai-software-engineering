package banking.service;

import banking.domain.Transaction;
import banking.dto.BalanceResponse;
import banking.dto.CurrencyBalance;
import banking.dto.MultiCurrencyBalanceResponse;
import banking.exception.NotFoundException;
import banking.store.TransactionStore;
import banking.validation.Validator;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * Computes account balances, delegating the per-currency arithmetic to
 * {@link BalanceCalculator}.
 *
 * Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6
 */
@Service
public class BalanceService {

    private final Validator validator;
    private final TransactionStore store;
    private final BalanceCalculator balanceCalculator;

    public BalanceService(Validator validator, TransactionStore store, BalanceCalculator balanceCalculator) {
        this.validator = validator;
        this.store = store;
        this.balanceCalculator = balanceCalculator;
    }

    /**
     * @return either a {@link BalanceResponse} (single currency) or a
     * {@link MultiCurrencyBalanceResponse} (2+ currencies).
     */
    public Object getBalance(String accountId) {
        validator.validateAccountId(accountId);

        List<Transaction> accountTransactions = store.findAll().stream()
                .filter(t -> accountId.equals(t.fromAccount()) || accountId.equals(t.toAccount()))
                .toList();

        if (accountTransactions.isEmpty()) {
            throw new NotFoundException("Account not found",
                    "No transactions found for account: " + accountId);
        }

        List<Transaction> completed = accountTransactions.stream()
                .filter(t -> "completed".equals(t.status()))
                .toList();

        Map<String, BigDecimal> balances = balanceCalculator.computeByCurrency(accountId, completed);

        if (balances.isEmpty()) {
            String currency = accountTransactions.get(0).currency();
            return new BalanceResponse(accountId, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), currency);
        }

        if (balances.size() == 1) {
            Map.Entry<String, BigDecimal> entry = balances.entrySet().iterator().next();
            return new BalanceResponse(accountId, entry.getValue(), entry.getKey());
        }

        List<CurrencyBalance> currencyBalances = balances.entrySet().stream()
                .map(e -> new CurrencyBalance(e.getKey(), e.getValue()))
                .toList();
        return new MultiCurrencyBalanceResponse(accountId, currencyBalances);
    }
}
