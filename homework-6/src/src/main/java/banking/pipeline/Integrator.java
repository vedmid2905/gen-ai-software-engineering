package banking.pipeline;

import banking.pipeline.domain.PipelineSummary;
import banking.pipeline.domain.ProcessingResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Command-line entry point for the banking transaction pipeline.
 *
 * <p>Usage: {@code java -cp ... banking.pipeline.Integrator [input-file] [shared-dir]}
 * Both arguments are optional; the input file defaults to sample-transactions.json,
 * searched first in the current directory and then the parent directory.
 */
public final class Integrator {

    private Integrator() {
    }

    public static void main(String[] args) throws IOException {
        Path inputFile = args.length > 0 ? Path.of(args[0]) : PipelineOrchestrator.resolveDefaultInputFile();
        Path sharedDir = args.length > 1 ? Path.of(args[1]) : Path.of("shared");

        if (!Files.isRegularFile(inputFile)) {
            System.err.println("Input file not found: " + inputFile.toAbsolutePath());
            System.exit(1);
            return;
        }

        System.out.println("Loading transactions from " + inputFile.toAbsolutePath());
        PipelineOrchestrator orchestrator = new PipelineOrchestrator(sharedDir);
        PipelineSummary summary = orchestrator.run(inputFile);

        System.out.println();
        System.out.println("Pipeline run complete. Results written to " + sharedDir.resolve("results").toAbsolutePath());
        System.out.printf("Total: %d | Approved: %d | Flagged: %d | Rejected: %d%n",
                summary.totalProcessed(), summary.approved(), summary.flagged(), summary.rejected());

        boolean anyRejectedOrFlagged = summary.results().stream()
                .anyMatch(r -> !"approved".equals(r.status()));
        if (anyRejectedOrFlagged) {
            System.out.println();
            System.out.println("Transactions requiring attention:");
            for (ProcessingResult result : summary.results()) {
                if (!"approved".equals(result.status())) {
                    System.out.printf("  - %s [%s] %s%n", result.transactionId(), result.status(),
                            result.reason() == null ? "" : result.reason());
                }
            }
        }
    }

}
