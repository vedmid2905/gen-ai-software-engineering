package com.example.support.dto;

import com.example.support.model.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import java.util.List;

public class UpdateTicketRequest {
    @Email(message = "customer_email must be a valid email address")
    @JsonProperty("customer_email") private String customerEmail;
    @JsonProperty("customer_name") private String customerName;
    @Size(min = 1, max = 200, message = "subject must be between 1 and 200 characters")
    private String subject;
    @Size(min = 10, max = 2000, message = "description must be between 10 and 2000 characters")
    private String description;
    private Category category;
    private Priority priority;
    private Status status;
    @JsonProperty("assigned_to") private String assignedTo;
    private List<String> tags;
    private Source source;
    private String browser;
    @JsonProperty("device_type") private DeviceType deviceType;

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public Source getSource() { return source; }
    public void setSource(Source source) { this.source = source; }
    public String getBrowser() { return browser; }
    public void setBrowser(String browser) { this.browser = browser; }
    public DeviceType getDeviceType() { return deviceType; }
    public void setDeviceType(DeviceType deviceType) { this.deviceType = deviceType; }
}
