package banking.pipeline;

import banking.pipeline.domain.PipelineSummary;
import banking.pipeline.domain.ProcessingResult;
import banking.pipeline.io.SharedDirectoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the full pipeline against the real sample-transactions.json, isolating
 * all shared/ output into a JUnit-managed temp directory.
 */
class PipelineIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void allSampleTransactionsAreProcessedAndPersistedToResults() throws Exception {
        Path inputFile = locateSampleTransactionsFile();
        Path sharedDir = tempDir.resolve("shared");

        PipelineOrchestrator orchestrator = new PipelineOrchestrator(sharedDir);
        PipelineSummary summary = orchestrator.run(inputFile);

        assertThat(summary.totalProcessed()).isEqualTo(8);
        assertThat(summary.rejected()).isEqualTo(2);
        assertThat(summary.flagged()).isEqualTo(1);
        assertThat(summary.approved()).isEqualTo(5);

        SharedDirectoryService sharedDirectoryService = new SharedDirectoryService(sharedDir);
        List<ProcessingResult> persistedResults = sharedDirectoryService.readAll(
                sharedDirectoryService.resultsDir(), ProcessingResult.class);
        // pipeline-summary.json also lands in results/, so filter it out by transaction id shape
        long perTransactionFiles = persistedResults.stream()
                .filter(r -> r.transactionId() != null && r.transactionId().startsWith("TXN"))
                .count();
        assertThat(perTransactionFiles).isEqualTo(8);

        assertThat(Files.exists(sharedDirectoryService.resultsDir().resolve("pipeline-summary.json"))).isTrue();

        Set<String> rejectedIds = persistedResults.stream()
                .filter(r -> "rejected".equals(r.status()))
                .map(ProcessingResult::transactionId)
                .collect(java.util.stream.Collectors.toSet());
        assertThat(rejectedIds).containsExactlyInAnyOrder("TXN006", "TXN007");

        Set<String> flaggedIds = persistedResults.stream()
                .filter(r -> "flagged".equals(r.status()))
                .map(ProcessingResult::transactionId)
                .collect(java.util.stream.Collectors.toSet());
        assertThat(flaggedIds).containsExactly("TXN005");
    }

    @Test
    void rerunningPipelineLeavesNoStaleFilesFromPreviousRun() throws Exception {
        Path inputFile = locateSampleTransactionsFile();
        Path sharedDir = tempDir.resolve("shared");

        PipelineOrchestrator orchestrator = new PipelineOrchestrator(sharedDir);
        orchestrator.run(inputFile);
        PipelineSummary secondRun = orchestrator.run(inputFile);

        SharedDirectoryService sharedDirectoryService = new SharedDirectoryService(sharedDir);
        List<ProcessingResult> resultsAfterSecondRun = sharedDirectoryService.readAll(
                sharedDirectoryService.resultsDir(), ProcessingResult.class);

        // 8 transactions + 1 summary file, not duplicated across runs
        assertThat(resultsAfterSecondRun).hasSize(9);
        assertThat(secondRun.totalProcessed()).isEqualTo(8);
    }

    private Path locateSampleTransactionsFile() throws IOException {
        Path here = Path.of("sample-transactions.json");
        if (Files.isRegularFile(here)) {
            return here;
        }
        Path parent = Path.of("..", "sample-transactions.json");
        if (Files.isRegularFile(parent)) {
            return parent;
        }
        throw new IOException("sample-transactions.json not found relative to " + Path.of("").toAbsolutePath());
    }
}
