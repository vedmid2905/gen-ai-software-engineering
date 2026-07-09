package com.example.support.service;

import com.example.support.dto.CreateTicketRequest;
import com.example.support.dto.UpdateTicketRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates {@link CreateTicketRequest} and {@link UpdateTicketRequest} using
 * Jakarta Bean Validation. Called by {@link TicketService} before any persistence
 * operation in the import pipeline. Single-ticket endpoints rely on Spring's
 * {@code @Valid} / {@code MethodArgumentNotValidException} path instead.
 */
@Component
public class TicketValidator {

    private final Validator validator;

    public TicketValidator(Validator validator) {
        this.validator = validator;
    }

    /**
     * Validates a {@link CreateTicketRequest}.
     *
     * @param request the request to validate
     * @return a {@link ValidationResult} that is valid when no constraint violations exist
     */
    public ValidationResult validate(CreateTicketRequest request) {
        Set<ConstraintViolation<CreateTicketRequest>> violations = validator.validate(request);
        return toValidationResult(violations);
    }

    /**
     * Validates an {@link UpdateTicketRequest}.
     *
     * @param request the request to validate
     * @return a {@link ValidationResult} that is valid when no constraint violations exist
     */
    public ValidationResult validate(UpdateTicketRequest request) {
        Set<ConstraintViolation<UpdateTicketRequest>> violations = validator.validate(request);
        return toValidationResult(violations);
    }

    private <T> ValidationResult toValidationResult(Set<ConstraintViolation<T>> violations) {
        List<FieldError> errors = violations.stream()
                .map(v -> new FieldError(
                        v.getPropertyPath().toString(),
                        v.getMessage()))
                .sorted((a, b) -> a.getField().compareTo(b.getField()))
                .collect(Collectors.toList());
        return new ValidationResult(errors);
    }
}
