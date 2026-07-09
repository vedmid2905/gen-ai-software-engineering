package banking.service;

import banking.domain.Transaction;
import banking.dto.AccountSummaryResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Pure stateless component that aggregates transaction statistics for an
 * account.
 *
 * Requirements: 8.1, 8.4
 */
@Component
public class SummaryCalculator {

    public AccountSummaryResponse compute(String accountId, List<Transaction> accountTransactions) {
        BigDecimal totalDeposits = accountTransactions.stream()
                .filter(t -> "completed".equals(t.status()))
                .filter(t -> "deposit".equals(t.type()))
                .filter(t -> accountId.equals(t.toAccount()))
                .map(Transaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalWithdrawals = accountTransactions.stream()
                .filter(t -> "completed".equals(t.status()))
                .filter(t -> "withdrawal".equals(t.type()))
                .filter(t -> accountId.equals(t.fromAccount()))
                .map(Transaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        int transactionCount = accountTransactions.size();

        String mostRecent = accountTransactions.stream()
                .map(Transaction::timestamp)
                .max(java.time.Instant::compareTo)
                .map(Object::toString)
                .orElse(null);

        return new AccountSummaryResponse(accountId, totalDeposits, totalWithdrawals, transactionCount, mostRecent);
    }
}
