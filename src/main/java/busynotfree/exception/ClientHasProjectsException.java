package busynotfree.exception;

/** Indicates that existing projects prevent a client from being deleted. */
public final class ClientHasProjectsException extends BusyNotFreeException {
    private static final long serialVersionUID = 1L;

    /** Creates an error identifying the client whose projects block deletion. */
    public ClientHasProjectsException(String clientId) {
        super("Client " + clientId
                + " has associated projects. Delete those projects first.");
    }
}
