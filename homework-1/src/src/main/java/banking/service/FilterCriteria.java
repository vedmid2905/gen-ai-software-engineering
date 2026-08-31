package banking.service;

import java.time.Instant;

/**
 * Conjunctive filter parameters for {@code GET /transactions}. A {@code null}
 * field means "no filter applied" for that criterion.
 *
 * Requirements: 6.2, 6.3, 6.4, 6.5
 */
public record FilterCriteria(
        String accountId,
        String type,
        Instant fromDate,   // inclusive, start of day UTC
        Instant toDate      // inclusive, end of day UTC
) {}
