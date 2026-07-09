package banking.exception;

/**
 * Thrown when a path parameter (accountId or transactionId) does not conform
 * to the expected format.
 * Maps to HTTP 400 in the global exception handler.
 *
 * Requirements: 5.3, 7.3, 8.3, 10.4
 */
public class InvalidIdFormatException extends RuntimeException {

    private final String error;

    public InvalidIdFormatException(String message) {
        this("Invalid ID format", message);
    }

    public InvalidIdFormatException(String error, String message) {
        super(message);
        this.error = error;
    }

    public String getError() {
        return error;
    }
}
