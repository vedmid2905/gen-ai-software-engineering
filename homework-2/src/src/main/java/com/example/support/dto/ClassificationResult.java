package com.example.support.dto;

import com.example.support.model.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;

public class ClassificationResult {
    @JsonProperty("ticket_id") private UUID ticketId;
    private Category category;
    private Priority priority;
    @JsonProperty("confidence_score") private double confidenceScore;
    private String reasoning;
    @JsonProperty("keywords_found") private List<String> keywordsFound;

    public UUID getTicketId() { return ticketId; }
    public void setTicketId(UUID ticketId) { this.ticketId = ticketId; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }
    public String getReasoning() { return reasoning; }
    public void setReasoning(String reasoning) { this.reasoning = reasoning; }
    public List<String> getKeywordsFound() { return keywordsFound; }
    public void setKeywordsFound(List<String> keywordsFound) { this.keywordsFound = keywordsFound; }
}
