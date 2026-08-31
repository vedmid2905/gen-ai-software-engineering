package banking.dto;

import java.util.List;

/**
 * Standard error response body returned on 400 validation failures.
 *
 * Example:
 * {
 *   "error": "Validation failed",
 *   "details": [
 *     { "field": "amount", "message": "Amount must be a positive number" }
 *   ]
 * }
 *
 * Requirements: 1.2, 10.4, 10.5
 */
public record ErrorResponse(
        String error,
        List<FieldError> details
) {}
