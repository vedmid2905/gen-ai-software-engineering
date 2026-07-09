package banking.exception;

/**
 * Thrown when a path parameter (accountId or transactionId) does not conform
 * to the expected format.
 * Maps to HTTP 400 in the global exception handler.
 *
 * Requirements: 5.3, 7.3, 8.3, 10.4
 */
public class InvalidIdFormatException extends RuntimeException {

    public InvalidIdFormatException(String message) {
        super(message);
    }
}
