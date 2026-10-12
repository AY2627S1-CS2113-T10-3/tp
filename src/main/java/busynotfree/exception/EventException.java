package busynotfree.exception;

/**
 * Represents invalid event data or an event that cannot be attached to a project.
 */
public final class EventException extends BusyNotFreeException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception with a user-facing message.
     *
     * @param message explanation of the failure
     */
    public EventException(String message) {
        super(message);
    }

    /**
     * Creates an exception while retaining its original cause.
     *
     * @param message explanation of the failure
     * @param cause original exception that caused the failure
     */
    public EventException(String message, Throwable cause) {
        super(message, cause);
    }
}
