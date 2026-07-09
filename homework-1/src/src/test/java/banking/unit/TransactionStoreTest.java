package banking.unit;

import banking.domain.Transaction;
import banking.store.TransactionStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TransactionStore}.
 *
 * Requirements: 11.3, 12.2
 */
class TransactionStoreTest {

    private TransactionStore store;

    @BeforeEach
    void setUp() {
        store = new TransactionStore();
    }

    // -------------------------------------------------------------------------
    // Helper factory
    // -------------------------------------------------------------------------

    private Transaction buildTransaction(String id) {
        return new Transaction(
                id,
                "deposit",
                new BigDecimal("100.00"),
                "USD",
                null,
                "ACC-AAAAA",
                "pending",
                Instant.now()
        );
    }

    // -------------------------------------------------------------------------
    // save / findById round-trip
    // -------------------------------------------------------------------------

    @Test
    void saveAndFindById_roundTrip() {
        String id = UUID.randomUUID().toString();
        Transaction tx = buildTransaction(id);

        store.save(tx);

        Optional<Transaction> result = store.findById(id);

        assertTrue(result.isPresent(), "findById should return the saved transaction");
        assertEquals(tx, result.get(), "Returned transaction should equal the saved one");
    }

    // -------------------------------------------------------------------------
    // findAll
    // -------------------------------------------------------------------------

    @Test
    void findAll_returnsAllSaved() {
        String id1 = UUID.randomUUID().toString();
        String id2 = UUID.randomUUID().toString();
        String id3 = UUID.randomUUID().toString();

        store.save(buildTransaction(id1));
        store.save(buildTransaction(id2));
        store.save(buildTransaction(id3));

        List<Transaction> all = store.findAll();

        assertEquals(3, all.size(), "findAll should return all 3 saved transactions");

        Set<String> returnedIds = new HashSet<>();
        for (Transaction tx : all) {
            returnedIds.add(tx.id());
        }
        assertTrue(returnedIds.contains(id1), "findAll result should contain id1");
        assertTrue(returnedIds.contains(id2), "findAll result should contain id2");
        assertTrue(returnedIds.contains(id3), "findAll result should contain id3");
    }

    // -------------------------------------------------------------------------
    // existsById
    // -------------------------------------------------------------------------

    @Test
    void existsById_afterSave_returnsTrue() {
        String id = UUID.randomUUID().toString();
        store.save(buildTransaction(id));

        assertTrue(store.existsById(id), "existsById should return true after saving");
    }

    @Test
    void existsById_missingId_returnsFalse() {
        String unknownId = UUID.randomUUID().toString();

        assertFalse(store.existsById(unknownId), "existsById should return false for an id that was never saved");
    }

    // -------------------------------------------------------------------------
    // Concurrent writes — no silent loss (Requirements 11.3, 12.2)
    // -------------------------------------------------------------------------

    @Test
    void concurrentSave_noLoss() throws InterruptedException {
        final int THREAD_COUNT = 50;
        List<String> ids = new ArrayList<>(THREAD_COUNT);
        for (int i = 0; i < THREAD_COUNT; i++) {
            ids.add(UUID.randomUUID().toString());
        }

        // Ensure all threads start at the same moment
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        try {
            for (int i = 0; i < THREAD_COUNT; i++) {
                final String id = ids.get(i);
                executor.submit(() -> {
                    try {
                        startLatch.await(); // wait for the signal to start simultaneously
                        store.save(buildTransaction(id));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown(); // release all threads at once
            boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
            assertTrue(completed, "All threads should complete within the timeout");
        } finally {
            executor.shutdown();
        }

        List<Transaction> all = store.findAll();
        assertEquals(THREAD_COUNT, all.size(),
                "All " + THREAD_COUNT + " transactions should be present — no silent loss");

        Set<String> storedIds = new HashSet<>();
        for (Transaction tx : all) {
            storedIds.add(tx.id());
        }
        assertEquals(THREAD_COUNT, storedIds.size(),
                "All stored transaction IDs should be distinct");

        // Verify every expected id is accounted for
        for (String id : ids) {
            assertTrue(storedIds.contains(id),
                    "Expected transaction id " + id + " to be present in the store");
        }
    }
}
