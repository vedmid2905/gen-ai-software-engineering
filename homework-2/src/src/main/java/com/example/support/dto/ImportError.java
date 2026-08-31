package com.example.support.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ImportError {
    @JsonProperty("record_index") private int recordIndex;
    private String message;

    public ImportError() {}
    public ImportError(int recordIndex, String message) {
        this.recordIndex = recordIndex;
        this.message = message;
    }
    public int getRecordIndex() { return recordIndex; }
    public void setRecordIndex(int recordIndex) { this.recordIndex = recordIndex; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
