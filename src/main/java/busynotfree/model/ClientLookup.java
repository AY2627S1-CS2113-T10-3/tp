package busynotfree.model;

/**
 * Provides the client-existence check needed when creating a project.
 * {@code ClientManager} implements it, so {@code ProjectManager} does not depend on client storage directly.
 */
@FunctionalInterface
public interface ClientLookup {
    /**
     * Checks whether a client exists.
     *
     * @param clientId Non-blank client ID to look up.
     * @return True if the client exists.
     */
    boolean exists(String clientId);
}
