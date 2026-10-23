package busynotfree.exception;

/**
 * Represents malformed command words, arguments or date text.
 */
public final class CommandException extends BusyNotFreeException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception with a user-facing message.
     *
     * @param message Explanation of the failure.
     */
    public CommandException(String message) {
        super(message);
    }

    /**
     * Creates an exception while retaining its original cause.
     *
     * @param message Explanation of the failure.
     * @param cause Original exception that caused the failure.
     */
    public CommandException(String message, Throwable cause) {
        super(message, cause);
    }
}
