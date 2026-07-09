package banking.service;

import banking.domain.Transaction;
import banking.dto.AccountSummaryResponse;
import banking.exception.NotFoundException;
import banking.store.TransactionStore;
import banking.validation.Validator;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Computes account transaction summaries, delegating aggregation to
 * {@link SummaryCalculator}.
 *
 * Requirements: 8.1, 8.2, 8.3, 8.4
 */
@Service
public class SummaryService {

    private final Validator validator;
    private final TransactionStore store;
    private final SummaryCalculator summaryCalculator;

    public SummaryService(Validator validator, TransactionStore store, SummaryCalculator summaryCalculator) {
        this.validator = validator;
        this.store = store;
        this.summaryCalculator = summaryCalculator;
    }

    public AccountSummaryResponse getSummary(String accountId) {
        validator.validateAccountId(accountId);

        List<Transaction> accountTransactions = store.findAll().stream()
                .filter(t -> accountId.equals(t.fromAccount()) || accountId.equals(t.toAccount()))
                .toList();

        if (accountTransactions.isEmpty()) {
            throw new NotFoundException("Account not found",
                    "No transactions found for account: " + accountId);
        }

        return summaryCalculator.compute(accountId, accountTransactions);
    }
}
