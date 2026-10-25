package busynotfree.model;

/**
 * Test stub that accepts every client ID, so project, task and event tests do not need to register clients.
 * Tests that need a client to be missing supply a lambda such as {@code clientId -> false} instead.
 */
public final class PlaceholderClientLookup implements ClientLookup {
    @Override
    public boolean exists(String clientId) {
        return true;
    }
}
