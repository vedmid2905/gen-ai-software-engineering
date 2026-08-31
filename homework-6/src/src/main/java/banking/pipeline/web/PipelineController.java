package banking.pipeline.web;

import banking.pipeline.PipelineOrchestrator;
import banking.pipeline.domain.PipelineSummary;
import banking.pipeline.domain.ProcessingResult;
import banking.pipeline.io.SharedDirectoryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Exposes the file-based agent pipeline over HTTP so the frontend shell can
 * trigger a run and inspect the latest results without a CLI.
 */
@RestController
@RequestMapping("/api/pipeline")
public class PipelineController {

    private static final String SUMMARY_FILE_NAME = "pipeline-summary.json";

    private final PipelineOrchestrator orchestrator;
    private final SharedDirectoryService sharedDirectoryService;
    private final Path inputFile;

    public PipelineController(
            @Value("${pipeline.shared-dir:shared}") String sharedDir,
            @Value("${pipeline.input-file:}") String inputFileProperty) {
        Path sharedPath = Path.of(sharedDir);
        this.orchestrator = new PipelineOrchestrator(sharedPath);
        this.sharedDirectoryService = new SharedDirectoryService(sharedPath);
        this.inputFile = inputFileProperty.isBlank()
                ? PipelineOrchestrator.resolveDefaultInputFile()
                : Path.of(inputFileProperty);
    }

    @PostMapping("/run")
    public PipelineSummary run() throws IOException {
        return orchestrator.run(inputFile);
    }

    @GetMapping("/summary")
    public ResponseEntity<PipelineSummary> summary() throws IOException {
        Path summaryFile = sharedDirectoryService.resultsDir().resolve(SUMMARY_FILE_NAME);
        if (!Files.isRegularFile(summaryFile)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(sharedDirectoryService.read(summaryFile, PipelineSummary.class));
    }

    @GetMapping("/results")
    public List<ProcessingResult> results() throws IOException {
        Path resultsDir = sharedDirectoryService.resultsDir();
        if (!Files.isDirectory(resultsDir)) {
            return List.of();
        }
        List<ProcessingResult> results = new ArrayList<>();
        try (Stream<Path> files = Files.list(resultsDir)) {
            List<Path> sorted = files
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .filter(p -> !p.getFileName().toString().equals(SUMMARY_FILE_NAME))
                    .sorted(Comparator.comparing(Path::getFileName))
                    .toList();
            for (Path file : sorted) {
                results.add(sharedDirectoryService.read(file, ProcessingResult.class));
            }
        }
        return results;
    }
}
