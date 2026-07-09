package banking.pipeline.domain;

/**
 * Result emitted by an agent for a single transaction.
 *
 * @param status   validated | rejected | flagged | approved
 * @param decision valid | invalid (set by the validator, carried through the pipeline)
 * @param riskScore fraud risk score (0-100), null before fraud scoring happens
 * @param reason   human-readable explanation, null when there is nothing to explain
 */
public record ProcessingResult(
        String transactionId,
        String status,
        String decision,
        Integer riskScore,
        String reason,
        AuditInfo audit
) {
}
