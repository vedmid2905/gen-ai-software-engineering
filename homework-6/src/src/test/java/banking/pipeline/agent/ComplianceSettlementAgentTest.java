package banking.pipeline.agent;

import banking.pipeline.domain.AuditInfo;
import banking.pipeline.domain.FraudAssessment;
import banking.pipeline.domain.ProcessingResult;
import banking.pipeline.domain.TransactionRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ComplianceSettlementAgentTest {

    private final ComplianceSettlementAgent agent = new ComplianceSettlementAgent();

    private TransactionRecord transaction() {
        return new TransactionRecord(
                "TXN005", Instant.now(), "ACC-1005", "ACC-6600",
                new BigDecimal("75000.00"), "USD", "wire_transfer", "desc", Map.of("country", "US"));
    }

    @Test
    void lowRiskTransactionIsApproved() {
        FraudAssessment assessment = new FraudAssessment("TXN005", 20, "low", List.of(),
                new AuditInfo(Instant.now(), "fraud_detector"));

        ProcessingResult result = agent.evaluateSettlement(transaction(), assessment);

        assertThat(result.status()).isEqualTo("approved");
        assertThat(result.reason()).isNull();
        assertThat(result.riskScore()).isEqualTo(20);
    }

    @Test
    void highRiskTransactionIsFlaggedWithReason() {
        FraudAssessment assessment = new FraudAssessment("TXN005", 70, "high",
                List.of("very high value transfer", "wire transfer channel"),
                new AuditInfo(Instant.now(), "fraud_detector"));

        ProcessingResult result = agent.evaluateSettlement(transaction(), assessment);

        assertThat(result.status()).isEqualTo("flagged");
        assertThat(result.reason()).contains("very high value transfer", "wire transfer channel");
        assertThat(result.audit().agent()).isEqualTo("compliance_settlement");
    }
}
