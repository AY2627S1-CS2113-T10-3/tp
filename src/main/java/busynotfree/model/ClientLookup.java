package busynotfree.model;

/**
 * Provides the client-existence check needed when creating a project.
 * The teammate's client implementation can supply this lookup later.
 */
@FunctionalInterface
public interface ClientLookup {
    /**
     * Checks whether a client exists.
     *
     * @param clientId non-blank client ID to look up
     * @return true if the client exists
     */
    boolean exists(String clientId);
}
