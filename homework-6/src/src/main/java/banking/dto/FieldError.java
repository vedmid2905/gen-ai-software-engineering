package banking.dto;

/**
 * A single field-level validation error entry included in ErrorResponse.details.
 *
 * Requirements: 1.2, 10.4
 */
public record FieldError(
        String field,
        String message
) {}
