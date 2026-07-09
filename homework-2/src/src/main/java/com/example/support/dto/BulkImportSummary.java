package com.example.support.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class BulkImportSummary {
    @JsonProperty("total_records") private int totalRecords;
    private int successful;
    private int failed;
    private List<ImportError> errors = new ArrayList<>();

    public int getTotalRecords() { return totalRecords; }
    public void setTotalRecords(int totalRecords) { this.totalRecords = totalRecords; }
    public int getSuccessful() { return successful; }
    public void setSuccessful(int successful) { this.successful = successful; }
    public int getFailed() { return failed; }
    public void setFailed(int failed) { this.failed = failed; }
    public List<ImportError> getErrors() { return errors; }
    public void setErrors(List<ImportError> errors) { this.errors = errors; }
}
