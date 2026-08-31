package banking.pipeline.agent;

import banking.pipeline.domain.FraudAssessment;
import banking.pipeline.domain.TransactionRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FraudDetectorAgentTest {

    private final FraudDetectorAgent agent = new FraudDetectorAgent();

    private TransactionRecord txWith(String amount, String timestamp, String country, String type) {
        return new TransactionRecord(
                "TXN-X", Instant.parse(timestamp), "ACC-1001", "ACC-2001",
                new BigDecimal(amount), "USD", type, "desc", Map.of("channel", "online", "country", country));
    }

    @Test
    void lowValueDaytimeDomesticTransferIsLowRisk() {
        FraudAssessment assessment = agent.scoreTransaction(
                txWith("1500.00", "2026-03-16T09:00:00Z", "US", "transfer"));

        assertThat(assessment.riskLabel()).isEqualTo("low");
        assertThat(assessment.riskScore()).isZero();
    }

    @Test
    void amountJustBelowHighValueThresholdIsNotFlagged() {
        FraudAssessment assessment = agent.scoreTransaction(
                txWith("9999.99", "2026-03-16T09:30:00Z", "US", "transfer"));

        assertThat(assessment.riskScore()).isZero();
        assertThat(assessment.reasons()).isEmpty();
    }

    @Test
    void veryHighValueWireTransferIsHighRisk() {
        FraudAssessment assessment = agent.scoreTransaction(
                txWith("75000.00", "2026-03-16T10:00:00Z", "US", "wire_transfer"));

        assertThat(assessment.riskLabel()).isEqualTo("high");
        assertThat(assessment.riskScore()).isEqualTo(70);
        assertThat(assessment.reasons()).contains("very high value transfer", "wire transfer channel");
    }

    @Test
    void offHoursAndCrossBorderCombineToMediumRisk() {
        FraudAssessment assessment = agent.scoreTransaction(
                txWith("500.00", "2026-03-16T02:47:00Z", "DE", "transfer"));

        assertThat(assessment.riskLabel()).isEqualTo("medium");
        assertThat(assessment.reasons()).contains("unusual timing (off-hours activity)", "cross-border activity (DE)");
    }

    @Test
    void scoreIsCappedAtOneHundred() {
        FraudAssessment assessment = agent.scoreTransaction(
                txWith("999999.00", "2026-03-16T03:00:00Z", "RU", "wire_transfer"));

        assertThat(assessment.riskScore()).isEqualTo(100);
    }
}
