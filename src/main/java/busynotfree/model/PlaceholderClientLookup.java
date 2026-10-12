package busynotfree.model;

/**
 * Temporarily assumes that supplied client IDs exist, without creating any clients.
 * Replace this lookup with the teammate's client implementation before release.
 */
public final class PlaceholderClientLookup implements ClientLookup {
    @Override
    public boolean exists(String clientId) {
        // TODO: Replace the placeholder with a lookup in the actual client collection.
        return true;
    }
}
