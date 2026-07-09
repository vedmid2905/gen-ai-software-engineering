package banking.pipeline.agent;

import banking.pipeline.domain.PipelineSummary;
import banking.pipeline.domain.ProcessingResult;
import banking.pipeline.io.SharedDirectoryService;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/**
 * Aggregates final results from every agent and writes the pipeline summary
 * report to shared/results/pipeline-summary.json.
 */
public class ReportingAgent {

    private final SharedDirectoryService sharedDirectoryService;

    public ReportingAgent(SharedDirectoryService sharedDirectoryService) {
        this.sharedDirectoryService = sharedDirectoryService;
    }

    public PipelineSummary writeSummary(List<ProcessingResult> results) throws IOException {
        int approved = 0;
        int flagged = 0;
        int rejected = 0;
        for (ProcessingResult result : results) {
            switch (result.status()) {
                case "approved" -> approved++;
                case "flagged" -> flagged++;
                case "rejected" -> rejected++;
                default -> { /* unrecognized status is not counted in totals */ }
            }
        }

        PipelineSummary summary = new PipelineSummary(Instant.now(), results.size(), approved, flagged, rejected, results);
        sharedDirectoryService.write(sharedDirectoryService.resultsDir(), "pipeline-summary.json", summary);
        return summary;
    }
}
