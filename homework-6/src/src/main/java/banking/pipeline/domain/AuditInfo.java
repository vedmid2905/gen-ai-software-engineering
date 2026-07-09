package banking.pipeline.domain;

import java.time.Instant;

/**
 * Safe audit metadata attached to every agent decision.
 * Deliberately excludes account numbers, names, and other PII.
 */
public record AuditInfo(
        Instant timestamp,
        String agent
) {
}
