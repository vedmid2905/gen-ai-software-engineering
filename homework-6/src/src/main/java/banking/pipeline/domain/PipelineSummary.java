package banking.pipeline.domain;

import java.time.Instant;
import java.util.List;

/**
 * Aggregated outcome of a full pipeline run, written to shared/results/pipeline-summary.json.
 */
public record PipelineSummary(
        Instant generatedAt,
        int totalProcessed,
        int approved,
        int flagged,
        int rejected,
        List<ProcessingResult> results
) {
}
