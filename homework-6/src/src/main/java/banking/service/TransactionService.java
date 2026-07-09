package banking.service;

import banking.domain.Transaction;
import banking.dto.AccountSummaryResponse;
import banking.dto.BalanceResponse;
import banking.dto.CreateTransactionRequest;
import banking.dto.CurrencyBalance;
import banking.dto.MultiCurrencyBalanceResponse;
import banking.dto.TransactionResponse;
import banking.exception.NotFoundException;
import banking.exception.ValidationException;
import banking.store.TransactionStore;
import banking.validation.Validator;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Business logic for creating and querying transactions, and for computing
 * per-account balances and summaries from the in-memory store.
 */
@Service
public class TransactionService {

    private final TransactionStore store;
    private final Validator validator;

    public TransactionService(TransactionStore store, Validator validator) {
        this.store = store;
        this.validator = validator;
    }

    public TransactionResponse createTransaction(CreateTransactionRequest request) {
        List<banking.dto.FieldError> errors = validator.validateCreateTransaction(request);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        Transaction transaction = new Transaction(
                UUID.randomUUID().toString(),
                request.type(),
                request.amount(),
                request.currency().toUpperCase(),
                request.fromAccount(),
                request.toAccount(),
                "completed",
                Instant.now());

        store.save(transaction);
        return toResponse(transaction);
    }

    public List<TransactionResponse> listTransactions(String type, String from, String to) {
        validator.validateListFilters(type, from, to);

        Instant fromInstant = parseFilterInstant(from);
        Instant toInstant = parseFilterInstant(to);

        return store.findAll().stream()
                .filter(t -> type == null || type.equals(t.type()))
                .filter(t -> fromInstant == null || !t.timestamp().isBefore(fromInstant))
                .filter(t -> toInstant == null || !t.timestamp().isAfter(toInstant))
                .sorted(Comparator.comparing(Transaction::timestamp))
                .map(this::toResponse)
                .toList();
    }

    public TransactionResponse getTransaction(String id) {
        validator.validateTransactionId(id);
        Transaction transaction = store.findById(id)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + id));
        return toResponse(transaction);
    }

    public Object getBalance(String accountId) {
        validator.validateAccountId(accountId);

        Map<String, BigDecimal> balancesByCurrency = new TreeMap<>();
        boolean accountSeen = false;
        for (Transaction t : store.findAll()) {
            if (!involvesAccount(t, accountId)) {
                continue;
            }
            accountSeen = true;
            applyToBalance(balancesByCurrency, t, accountId);
        }

        if (!accountSeen) {
            throw new NotFoundException("Account not found: " + accountId);
        }

        if (balancesByCurrency.size() == 1) {
            Map.Entry<String, BigDecimal> only = balancesByCurrency.entrySet().iterator().next();
            return new BalanceResponse(accountId, only.getValue(), only.getKey());
        }

        List<CurrencyBalance> balances = balancesByCurrency.entrySet().stream()
                .map(e -> new CurrencyBalance(e.getKey(), e.getValue()))
                .toList();
        return new MultiCurrencyBalanceResponse(accountId, balances);
    }

    public AccountSummaryResponse getSummary(String accountId) {
        validator.validateAccountId(accountId);

        BigDecimal totalDeposits = BigDecimal.ZERO;
        BigDecimal totalWithdrawals = BigDecimal.ZERO;
        int count = 0;
        Instant mostRecent = null;

        for (Transaction t : store.findAll()) {
            if (!involvesAccount(t, accountId)) {
                continue;
            }
            count++;
            if (mostRecent == null || t.timestamp().isAfter(mostRecent)) {
                mostRecent = t.timestamp();
            }
            if (accountId.equals(t.toAccount())) {
                totalDeposits = totalDeposits.add(t.amount());
            }
            if (accountId.equals(t.fromAccount())) {
                totalWithdrawals = totalWithdrawals.add(t.amount());
            }
        }

        if (count == 0) {
            throw new NotFoundException("Account not found: " + accountId);
        }

        return new AccountSummaryResponse(accountId, totalDeposits, totalWithdrawals, count, mostRecent.toString());
    }

    private boolean involvesAccount(Transaction t, String accountId) {
        return accountId.equals(t.fromAccount()) || accountId.equals(t.toAccount());
    }

    private void applyToBalance(Map<String, BigDecimal> balances, Transaction t, String accountId) {
        BigDecimal delta = BigDecimal.ZERO;
        if (accountId.equals(t.toAccount())) {
            delta = delta.add(t.amount());
        }
        if (accountId.equals(t.fromAccount())) {
            delta = delta.subtract(t.amount());
        }
        balances.merge(t.currency(), delta, BigDecimal::add);
    }

    private Instant parseFilterInstant(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (Exception ignored) {
            return LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toInstant();
        }
    }

    private TransactionResponse toResponse(Transaction t) {
        return new TransactionResponse(t.id(), t.type(), t.amount(), t.currency(),
                t.fromAccount(), t.toAccount(), t.status(), t.timestamp().toString());
    }
}
