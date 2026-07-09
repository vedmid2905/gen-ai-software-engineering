package com.example.support.service;

import java.util.List;

public class ValidationResult {
    private final List<FieldError> errors;

    public ValidationResult(List<FieldError> errors) {
        this.errors = errors;
    }

    public boolean isValid() {
        return errors.isEmpty();
    }

    public List<FieldError> getErrors() {
        return errors;
    }

    public String getSummary() {
        return errors.stream()
                .map(FieldError::toString)
                .reduce((a, b) -> a + "; " + b)
                .orElse("");
    }
}
