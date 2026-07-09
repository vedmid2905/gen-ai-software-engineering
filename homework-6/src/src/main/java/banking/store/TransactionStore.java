package banking.store;

import banking.domain.Transaction;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory repository for {@link Transaction} entities.
 *
 * <p>Backed by a {@link ConcurrentHashMap} keyed by transaction id. All public
 * methods are safe for concurrent access without additional synchronisation —
 * {@code ConcurrentHashMap} provides atomic {@code put} semantics, and because
 * entries are never mutated after being stored, read operations are also safe.
 *
 * <p>Registered as a Spring singleton via {@code @Component} so it can be
 * injected into service-layer beans.
 *
 * Requirements: 11.1, 11.2, 11.3, 12.2
 */
@Component
public class TransactionStore {

    private final ConcurrentHashMap<String, Transaction> store = new ConcurrentHashMap<>();

    /**
     * Persist a transaction. If a transaction with the same id already exists it
     * will be silently overwritten (the design never issues duplicate UUIDs, so
     * this case should not arise in practice).
     *
     * @param transaction the transaction to store; must not be {@code null}
     */
    public void save(Transaction transaction) {
        store.put(transaction.id(), transaction);
    }

    /**
     * Look up a transaction by its id.
     *
     * @param id the UUID v4 string assigned at creation time
     * @return an {@link Optional} containing the transaction, or empty if not found
     */
    public Optional<Transaction> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    /**
     * Return a snapshot of all stored transactions.
     *
     * <p>The returned list is a new {@link ArrayList} copy taken at the time of
     * the call, so callers may safely iterate it even if the store is concurrently
     * modified.
     *
     * @return an unordered list of all transactions; never {@code null}
     */
    public List<Transaction> findAll() {
        return new ArrayList<>(store.values());
    }

    /**
     * Check whether a transaction with the given id exists in the store.
     *
     * @param id the UUID v4 string to look up
     * @return {@code true} if the store contains an entry for {@code id}
     */
    public boolean existsById(String id) {
        return store.containsKey(id);
    }
}
