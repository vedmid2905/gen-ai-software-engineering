package banking.exception;

/**
 * Thrown when a requested resource (transaction or account) does not exist in the store.
 * Maps to HTTP 404 in the global exception handler.
 *
 * Requirements: 5.2, 7.4, 8.2, 10.5
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
