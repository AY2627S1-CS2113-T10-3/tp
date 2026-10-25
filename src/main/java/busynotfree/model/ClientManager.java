package busynotfree.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import busynotfree.exception.ClientException;

/**
 * Keeps clients in memory and supplies the client lookup used by project creation.
 * IDs advance only after successful additions; data is not saved between runs.
 */
public final class ClientManager implements ClientLookup {
    private final Map<String, Client> clients = new LinkedHashMap<>();
    private int nextClientNumber = 1;

    /**
     * Registers a client with an ID padded to at least three digits.
     * Clients with the same name receive distinct IDs.
     *
     * @param name Non-blank client name.
     * @return Newly registered client.
     * @throws ClientException If the name is null or blank.
     */
    public Client addClient(String name) {
        // These checks detect internal ID-allocation bugs, not invalid user input.
        assert nextClientNumber > 0 : "The next client number must remain positive.";
        String clientId = String.format(Locale.ROOT, "C%03d", nextClientNumber);
        assert !clients.containsKey(clientId) : "A generated client ID must not replace an existing client.";
        Client client = new Client(clientId, name);
        clients.put(clientId, client);
        nextClientNumber++;
        return client;
    }

    /**
     * Looks up a client by its case-sensitive ID without creating missing clients.
     *
     * @param clientId ID to look up, optionally padded with whitespace.
     * @return Existing client.
     * @throws ClientException If the ID is blank or does not identify a client.
     */
    public Client getClient(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw new ClientException("A client ID must not be blank.");
        }
        Client client = clients.get(clientId.trim());
        if (client == null) {
            throw new ClientException("Client " + clientId.trim() + " does not exist.");
        }
        assert clientId.trim().equals(client.getClientId()) : "Client lookup returned a different client.";
        return client;
    }

    /**
     * Records a note for an existing client.
     *
     * @param clientId Owning client's ID.
     * @param note Non-blank note text.
     * @return Client whose notes were updated.
     * @throws ClientException If the client does not exist or the note is blank.
     */
    public Client addNote(String clientId, String note) {
        Client client = getClient(clientId);
        client.addNote(note);
        return client;
    }

    /** Renames an existing client without replacing its ID, notes or project links. */
    public Client editClient(String clientId, String name) {
        Client client = getClient(clientId);
        client.setName(name);
        return client;
    }

    /** Finds case-insensitive name substrings in creation order, rejecting blank searches. */
    public List<Client> findClients(String name) {
        if (name == null || name.isBlank()) {
            throw new ClientException("A search name must not be blank.");
        }
        String search = name.trim().toLowerCase(Locale.ROOT);
        return clients.values().stream()
                .filter(client -> client.getName().toLowerCase(Locale.ROOT).contains(search))
                .toList();
    }

    /**
     * Returns an unmodifiable snapshot of clients in creation order.
     *
     * @return Clients registered in this application run.
     */
    public List<Client> getClients() {
        return List.copyOf(clients.values());
    }

    /** Returns the next client number so storage can preserve IDs across runs. */
    public int getNextClientNumber() {
        return nextClientNumber;
    }

    /** Advances the next client number while restoring a saved data file. */
    public void advanceNextClientNumber(int number) {
        if (number < nextClientNumber) {
            throw new ClientException("Saved client IDs are out of order.");
        }
        nextClientNumber = number;
    }

    @Override
    public boolean exists(String clientId) {
        return clientId != null && clients.containsKey(clientId.trim());
    }
}
