package banking.pipeline.agent;

import banking.pipeline.domain.AuditInfo;
import banking.pipeline.domain.PipelineSummary;
import banking.pipeline.domain.ProcessingResult;
import banking.pipeline.io.SharedDirectoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReportingAgentTest {

    @TempDir
    Path tempDir;

    @Test
    void writeSummaryAggregatesCountsAndPersistsFile() throws Exception {
        SharedDirectoryService sharedDirectoryService = new SharedDirectoryService(tempDir.resolve("shared"));
        Files.createDirectories(sharedDirectoryService.resultsDir());
        ReportingAgent reportingAgent = new ReportingAgent(sharedDirectoryService);

        AuditInfo audit = new AuditInfo(Instant.now(), "compliance_settlement");
        List<ProcessingResult> results = List.of(
                new ProcessingResult("TXN001", "approved", "valid", 10, null, audit),
                new ProcessingResult("TXN002", "flagged", "valid", 80, "high risk", audit),
                new ProcessingResult("TXN003", "rejected", "invalid", null, "bad currency", audit)
        );

        PipelineSummary summary = reportingAgent.writeSummary(results);

        assertThat(summary.totalProcessed()).isEqualTo(3);
        assertThat(summary.approved()).isEqualTo(1);
        assertThat(summary.flagged()).isEqualTo(1);
        assertThat(summary.rejected()).isEqualTo(1);

        Path summaryFile = sharedDirectoryService.resultsDir().resolve("pipeline-summary.json");
        assertThat(Files.exists(summaryFile)).isTrue();

        PipelineSummary reloaded = sharedDirectoryService.read(summaryFile, PipelineSummary.class);
        assertThat(reloaded.totalProcessed()).isEqualTo(3);
    }
}
