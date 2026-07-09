package banking.domain;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Core domain entity representing a financial transaction.
 * Immutable record backed by the in-memory Transaction_Store.
 *
 * Requirements: 1.1, 9.3, 12.1
 */
public record Transaction(
        String id,           // UUID v4, server-generated
        String type,         // "deposit" | "withdrawal" | "transfer"
        BigDecimal amount,   // positive, max 2 decimal places, max 999,999,999.99
        String currency,     // ISO 4217, always stored uppercase
        String fromAccount,  // nullable for deposits; ACC-[A-Z0-9]{5}
        String toAccount,    // nullable for withdrawals; ACC-[A-Z0-9]{5}
        String status,       // "pending" | "completed" | "failed"
        Instant timestamp    // server-generated at creation time
) {}
