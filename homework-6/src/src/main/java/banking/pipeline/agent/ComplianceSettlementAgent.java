package banking.pipeline.agent;

import banking.pipeline.domain.AuditInfo;
import banking.pipeline.domain.FraudAssessment;
import banking.pipeline.domain.ProcessingResult;
import banking.pipeline.domain.TransactionRecord;

import java.time.Instant;

/**
 * Applies the final policy decision for a validated transaction based on its
 * fraud assessment: approved, or flagged for manual review.
 *
 * <p>Only called for transactions that already passed {@link TransactionValidatorAgent};
 * rejected transactions never reach this stage.
 */
public class ComplianceSettlementAgent {

    static final String AGENT_NAME = "compliance_settlement";
    private static final int HIGH_RISK_THRESHOLD = 70;

    public ProcessingResult evaluateSettlement(TransactionRecord transaction, FraudAssessment assessment) {
        AuditInfo audit = new AuditInfo(Instant.now(), AGENT_NAME);

        if (assessment.riskScore() >= HIGH_RISK_THRESHOLD) {
            String reason = "flagged for manual review: " + String.join(", ", assessment.reasons());
            return new ProcessingResult(transaction.transactionId(), "flagged", "valid",
                    assessment.riskScore(), reason, audit);
        }

        return new ProcessingResult(transaction.transactionId(), "approved", "valid",
                assessment.riskScore(), null, audit);
    }
}
