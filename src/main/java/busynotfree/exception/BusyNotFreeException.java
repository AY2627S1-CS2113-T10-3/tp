package busynotfree.exception;

/**
 * Provides the common type for application validation failures shown to the user.
 * This unchecked exception keeps model APIs simple while allowing the CLI to catch
 * expected application failures without catching unrelated programming errors.
 */
public abstract class BusyNotFreeException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates an application exception with a user-facing message.
     *
     * @param message Explanation of the failure.
     */
    protected BusyNotFreeException(String message) {
        super(message);
    }

    /**
     * Creates an application exception while retaining its original cause.
     *
     * @param message Explanation of the failure.
     * @param cause Original exception that caused the failure.
     */
    protected BusyNotFreeException(String message, Throwable cause) {
        super(message, cause);
    }
}
