package busynotfree.exception;

/**
 * Represents invalid project data, missing projects or rejected client references.
 */
public final class ProjectException extends BusyNotFreeException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception with a user-facing message.
     *
     * @param message Explanation of the failure.
     */
    public ProjectException(String message) {
        super(message);
    }

    /**
     * Creates an exception while retaining its original cause.
     *
     * @param message Explanation of the failure.
     * @param cause Original exception that caused the failure.
     */
    public ProjectException(String message, Throwable cause) {
        super(message, cause);
    }
}
