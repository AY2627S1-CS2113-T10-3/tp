package busynotfree.exception;

/**
 * Represents invalid task data or a task that cannot be attached to a project.
 */
public final class TaskException extends BusyNotFreeException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception with a user-facing message.
     *
     * @param message Explanation of the failure.
     */
    public TaskException(String message) {
        super(message);
    }

    /**
     * Creates an exception while retaining its original cause.
     *
     * @param message Explanation of the failure.
     * @param cause Original exception that caused the failure.
     */
    public TaskException(String message, Throwable cause) {
        super(message, cause);
    }
}
