package com.example.support.dto;

import com.example.support.model.*;

public class TicketFilter {
    private Category category;
    private Priority priority;
    private Status status;

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}
