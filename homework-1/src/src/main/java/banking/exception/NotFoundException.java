package banking.exception;

/**
 * Thrown when a requested resource (transaction or account) does not exist in the store.
 * Maps to HTTP 404 in the global exception handler.
 *
 * Requirements: 5.2, 7.4, 8.2, 10.5
 */
public class NotFoundException extends RuntimeException {

    private final String error;

    public NotFoundException(String message) {
        this("Not found", message);
    }

    public NotFoundException(String error, String message) {
        super(message);
        this.error = error;
    }

    public String getError() {
        return error;
    }
}
