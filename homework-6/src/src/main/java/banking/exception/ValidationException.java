package banking.exception;

import banking.dto.FieldError;

import java.util.List;

/**
 * Thrown by the Validator when one or more field-level validation errors are found.
 * The exception carries the complete list of errors so the global exception handler
 * can build a single 400 response with all details at once (no short-circuit).
 *
 * Requirements: 1.2, 10.4
 */
public class ValidationException extends RuntimeException {

    private final List<FieldError> errors;

    public ValidationException(List<FieldError> errors) {
        super("Validation failed");
        this.errors = List.copyOf(errors);
    }

    public List<FieldError> getErrors() {
        return errors;
    }
}
