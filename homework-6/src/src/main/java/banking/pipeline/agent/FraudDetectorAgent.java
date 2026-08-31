package banking.pipeline.agent;

import banking.pipeline.domain.AuditInfo;
import banking.pipeline.domain.FraudAssessment;
import banking.pipeline.domain.TransactionRecord;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Scores transaction risk using amount thresholds, unusual timing, and
 * cross-border metadata. Higher scores mean higher risk.
 */
public class FraudDetectorAgent {

    static final String AGENT_NAME = "fraud_detector";

    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("10000");
    private static final BigDecimal VERY_HIGH_VALUE_THRESHOLD = new BigDecimal("50000");
    private static final int OFF_HOURS_START = 22;
    private static final int OFF_HOURS_END = 6;
    private static final String HOME_COUNTRY = "US";

    private static final int HIGH_RISK_THRESHOLD = 70;
    private static final int MEDIUM_RISK_THRESHOLD = 30;

    public FraudAssessment scoreTransaction(TransactionRecord transaction) {
        int score = 0;
        List<String> reasons = new ArrayList<>();

        BigDecimal amount = transaction.amount() == null ? BigDecimal.ZERO : transaction.amount().abs();
        if (amount.compareTo(VERY_HIGH_VALUE_THRESHOLD) > 0) {
            score += 60;
            reasons.add("very high value transfer");
        } else if (amount.compareTo(HIGH_VALUE_THRESHOLD) > 0) {
            score += 30;
            reasons.add("high value transfer");
        }

        if (transaction.timestamp() != null) {
            int hour = transaction.timestamp().atZone(ZoneOffset.UTC).getHour();
            if (hour >= OFF_HOURS_START || hour < OFF_HOURS_END) {
                score += 20;
                reasons.add("unusual timing (off-hours activity)");
            }
        }

        String country = transaction.metadata() == null ? null : transaction.metadata().get("country");
        if (country != null && !HOME_COUNTRY.equalsIgnoreCase(country)) {
            score += 25;
            reasons.add("cross-border activity (" + country + ")");
        }

        if ("wire_transfer".equalsIgnoreCase(transaction.transactionType())) {
            score += 10;
            reasons.add("wire transfer channel");
        }

        score = Math.min(score, 100);
        String label = score >= HIGH_RISK_THRESHOLD ? "high"
                : score >= MEDIUM_RISK_THRESHOLD ? "medium" : "low";

        AuditInfo audit = new AuditInfo(Instant.now(), AGENT_NAME);
        return new FraudAssessment(transaction.transactionId(), score, label, List.copyOf(reasons), audit);
    }
}
