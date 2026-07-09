package banking.pipeline.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * File-based message envelope exchanged between agents via shared/ directories.
 */
public record ProcessingMessage(
        String messageId,
        Instant timestamp,
        String sourceAgent,
        String targetAgent,
        String messageType,
        Object data
) {
    public static ProcessingMessage of(String sourceAgent, String targetAgent, String messageType, Object data) {
        return new ProcessingMessage(UUID.randomUUID().toString(), Instant.now(), sourceAgent, targetAgent, messageType, data);
    }
}
