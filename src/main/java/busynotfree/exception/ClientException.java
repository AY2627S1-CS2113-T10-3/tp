package busynotfree.exception;

/**
 * Represents invalid client data, missing clients or blank notes.
 */
public final class ClientException extends BusyNotFreeException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception with a message that can be shown to the user.
     *
     * @param message Explanation of the failure.
     */
    public ClientException(String message) {
        super(message);
    }
}
