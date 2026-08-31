package banking.service;

import banking.domain.Transaction;
import banking.dto.CreateTransactionRequest;
import banking.dto.FieldError;
import banking.dto.TransactionResponse;
import banking.exception.NotFoundException;
import banking.exception.ValidationException;
import banking.store.TransactionStore;
import banking.validation.Validator;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates transaction creation, retrieval, and filtered listing.
 *
 * Requirements: 1.1, 5.1, 5.2, 5.3, 6.1-6.7, 9.1, 12.1, 12.3
 */
@Service
public class TransactionService {

    private final Validator validator;
    private final TransactionStore store;
    private final FilterEngine filterEngine;

    public TransactionService(Validator validator, TransactionStore store, FilterEngine filterEngine) {
        this.validator = validator;
        this.store = store;
        this.filterEngine = filterEngine;
    }

    public TransactionResponse createTransaction(CreateTransactionRequest request) {
        List<FieldError> errors = validator.validateCreateTransaction(request);
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
                "pending",
                Instant.now()
        );
        store.save(transaction);
        return toResponse(transaction);
    }

    public TransactionResponse getTransaction(String id) {
        validator.validateTransactionId(id);
        Transaction transaction = store.findById(id)
                .orElseThrow(() -> new NotFoundException("Transaction not found",
                        "No transaction exists with id: " + id));
        return toResponse(transaction);
    }

    public List<TransactionResponse> listTransactions(String accountId, String type, String from, String to) {
        validator.validateListFilters(type, from, to);

        FilterCriteria criteria = new FilterCriteria(
                accountId,
                type,
                from == null ? null : parseStartOfDay(from),
                to == null ? null : parseEndOfDay(to)
        );

        return filterEngine.apply(store.findAll(), criteria).stream()
                .map(this::toResponse)
                .toList();
    }

    private Instant parseStartOfDay(String value) {
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
            // fall through to date-only parsing
        }
        return LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private Instant parseEndOfDay(String value) {
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
            // fall through to date-only parsing
        }
        return LocalDate.parse(value).atTime(23, 59, 59).atZone(ZoneOffset.UTC).toInstant();
    }

    private TransactionResponse toResponse(Transaction t) {
        return new TransactionResponse(
                t.id(), t.type(), t.amount(), t.currency(),
                t.fromAccount(), t.toAccount(), t.status(), t.timestamp().toString()
        );
    }
}
