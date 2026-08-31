package com.example.support.audit;

import com.example.support.model.Category;
import com.example.support.model.Priority;

import java.time.Instant;
import java.util.UUID;

public interface ClassificationLogger {
    void log(UUID ticketId, Category category, Priority priority,
             double confidenceScore, String reasoning, Instant timestamp);
}
