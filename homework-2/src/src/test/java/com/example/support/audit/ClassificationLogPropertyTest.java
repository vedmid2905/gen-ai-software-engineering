package com.example.support.audit;

// Feature: customer-support-system, Property 9: Classification log entry always written

import com.example.support.model.Category;
import com.example.support.model.ClassificationLogEntry;
import com.example.support.model.Priority;
import com.example.support.repository.ClassificationLogRepository;
import net.jqwik.api.*;
import net.jqwik.api.constraints.DoubleRange;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Property 9: Classification log entry always written
 * <p>
 * For any successful classification event, the classification_log table SHALL contain
 * exactly one new entry recording the ticket id, assigned category, priority,
 * confidence_score, and a timestamp no earlier than the start of the operation.
 * <p>
 * Validates: Requirements 11.8, 12.2
 */
class ClassificationLogPropertyTest {

    /**
     * Property 9: Classification log entry always written
     * <p>
     * Validates: Requirements 11.8, 12.2
     */
    @Property(tries = 100)
    void classificationLogEntryAlwaysWritten(
            @ForAll("ticketIds") UUID ticketId,
            @ForAll("categories") Category category,
            @ForAll("priorities") Priority priority,
            @ForAll @DoubleRange(min = 0.0, max = 1.0) double confidenceScore,
            @ForAll("reasoningStrings") String reasoning
    ) {
        // Record the start time before the operation
        Instant operationStart = Instant.now();

        // Mock the repository
        ClassificationLogRepository mockRepo = mock(ClassificationLogRepository.class);
        when(mockRepo.save(any(ClassificationLogEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Create the logger under test
        JpaClassificationLogger logger = new JpaClassificationLogger(mockRepo);

        // Execute the classification log operation
        Instant timestamp = Instant.now();
        logger.log(ticketId, category, priority, confidenceScore, reasoning, timestamp);

        // Capture the saved entry
        ArgumentCaptor<ClassificationLogEntry> captor =
                ArgumentCaptor.forClass(ClassificationLogEntry.class);
        verify(mockRepo, times(1)).save(captor.capture());

        ClassificationLogEntry savedEntry = captor.getValue();

        // Assert exactly one entry was saved with all matching fields
        assertThat(savedEntry.getTicketId())
                .as("ticketId must match")
                .isEqualTo(ticketId);

        assertThat(savedEntry.getCategory())
                .as("category must match")
                .isEqualTo(category);

        assertThat(savedEntry.getPriority())
                .as("priority must match")
                .isEqualTo(priority);

        assertThat(savedEntry.getConfidenceScore())
                .as("confidenceScore must match")
                .isEqualTo(confidenceScore);

        assertThat(savedEntry.getReasoning())
                .as("reasoning must match")
                .isEqualTo(reasoning);

        assertThat(savedEntry.getTimestamp())
                .as("timestamp must be >= operation start time")
                .isAfterOrEqualTo(operationStart);
    }

    // ── Generators ──────────────────────────────────────────────────────────────

    @Provide
    Arbitrary<UUID> ticketIds() {
        return Arbitraries.create(UUID::randomUUID);
    }

    @Provide
    Arbitrary<Category> categories() {
        return Arbitraries.of(Category.values());
    }

    @Provide
    Arbitrary<Priority> priorities() {
        return Arbitraries.of(Priority.values());
    }

    @Provide
    Arbitrary<String> reasoningStrings() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .ofMinLength(0)
                .ofMaxLength(200);
    }
}
