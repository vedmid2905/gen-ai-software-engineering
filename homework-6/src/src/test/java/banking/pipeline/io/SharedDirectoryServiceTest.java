package banking.pipeline.io;

import banking.pipeline.domain.AuditInfo;
import banking.pipeline.domain.ProcessingResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SharedDirectoryServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void resetDirectoriesCreatesAllFourEmptySubdirectories() throws Exception {
        SharedDirectoryService service = new SharedDirectoryService(tempDir);

        service.resetDirectories();

        assertThat(Files.isDirectory(service.inputDir())).isTrue();
        assertThat(Files.isDirectory(service.processingDir())).isTrue();
        assertThat(Files.isDirectory(service.outputDir())).isTrue();
        assertThat(Files.isDirectory(service.resultsDir())).isTrue();
    }

    @Test
    void resetDirectoriesClearsLeftoverFilesFromPreviousRun() throws Exception {
        SharedDirectoryService service = new SharedDirectoryService(tempDir);
        service.resetDirectories();
        service.write(service.resultsDir(), "stale.json",
                new ProcessingResult("OLD", "approved", "valid", 0, null,
                        new AuditInfo(Instant.now(), "test")));

        service.resetDirectories();

        assertThat(service.readAll(service.resultsDir(), ProcessingResult.class)).isEmpty();
    }

    @Test
    void writeAndReadRoundTripsAProcessingResult() throws Exception {
        SharedDirectoryService service = new SharedDirectoryService(tempDir);
        service.resetDirectories();
        ProcessingResult original = new ProcessingResult("TXN001", "approved", "valid", 15, null,
                new AuditInfo(Instant.parse("2026-03-16T10:00:00Z"), "compliance_settlement"));

        service.write(service.resultsDir(), "TXN001.json", original);
        List<ProcessingResult> all = service.readAll(service.resultsDir(), ProcessingResult.class);

        assertThat(all).hasSize(1);
        assertThat(all.get(0).transactionId()).isEqualTo("TXN001");
        assertThat(all.get(0).audit().agent()).isEqualTo("compliance_settlement");
    }
}
