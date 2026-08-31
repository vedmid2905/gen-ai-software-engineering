package banking.pipeline.domain;

import java.util.List;

/**
 * Fraud risk assessment for a single transaction.
 *
 * @param riskScore 0-100, higher means riskier
 * @param riskLabel low | medium | high
 */
public record FraudAssessment(
        String transactionId,
        int riskScore,
        String riskLabel,
        List<String> reasons,
        AuditInfo audit
) {
}
