package banking.service;

import banking.domain.Transaction;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Pure stateless component that applies conjunctive filters to a list of
 * transactions. Each non-null criterion in {@link FilterCriteria} is applied
 * as an AND predicate.
 *
 * Requirements: 6.2, 6.3, 6.4, 6.5
 */
@Component
public class FilterEngine {

    public List<Transaction> apply(List<Transaction> transactions, FilterCriteria criteria) {
        return transactions.stream()
                .filter(t -> criteria.accountId() == null
                        || criteria.accountId().equals(t.fromAccount())
                        || criteria.accountId().equals(t.toAccount()))
                .filter(t -> criteria.type() == null || criteria.type().equals(t.type()))
                .filter(t -> criteria.fromDate() == null || !t.timestamp().isBefore(criteria.fromDate()))
                .filter(t -> criteria.toDate() == null || !t.timestamp().isAfter(criteria.toDate()))
                .toList();
    }
}
