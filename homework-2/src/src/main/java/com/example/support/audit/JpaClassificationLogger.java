package com.example.support.audit;

import com.example.support.model.Category;
import com.example.support.model.ClassificationLogEntry;
import com.example.support.model.Priority;
import com.example.support.repository.ClassificationLogRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class JpaClassificationLogger implements ClassificationLogger {

    private final ClassificationLogRepository repo;

    public JpaClassificationLogger(ClassificationLogRepository repo) {
        this.repo = repo;
    }

    @Override
    public void log(UUID ticketId, Category category, Priority priority,
                    double confidenceScore, String reasoning, Instant timestamp) {
        ClassificationLogEntry entry = new ClassificationLogEntry();
        entry.setTicketId(ticketId);
        entry.setCategory(category);
        entry.setPriority(priority);
        entry.setConfidenceScore(confidenceScore);
        entry.setReasoning(reasoning);
        entry.setTimestamp(timestamp);
        repo.save(entry);
    }
}
