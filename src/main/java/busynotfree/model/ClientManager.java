package busynotfree.model;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import busynotfree.exception.ClientException;
import busynotfree.exception.ClientHasProjectsException;

/**
 * Keeps clients in memory and supplies the client lookup used by project creation.
 * IDs advance only after successful additions. Storage preserves active and deleted clients.
 */
public final class ClientManager implements ClientLookup {
    private final Map<String, Client> clients = new LinkedHashMap<>();
    private final TrashcanManager trashcanManager;
    private int nextClientNumber = 1;

    /** Creates a client manager with its own trashcan for a new application session. */
    public ClientManager() {
        this(new TrashcanManager());
    }

    /** Creates a client manager using the application's shared trashcan. */
    public ClientManager(TrashcanManager trashcanManager) {
        this.trashcanManager = Objects.requireNonNull(trashcanManager);
    }

    /** Returns the trashcan shared with project management and command routing. */
    public TrashcanManager getTrashcanManager() {
        return trashcanManager;
    }

    /**
     * Registers a client with an ID padded to at least three digits.
     * Clients with the same name receive distinct IDs.
     *
     * @param name Non-blank client name.
     * @return Newly registered client.
     * @throws ClientException If the name is blank, the counter is exhausted or the ID is already active.
     */
    public Client addClient(String name) {
        // Check before insertion so an exhausted counter cannot wrap or corrupt saved IDs.
        int followingNumber = nextNumberAfter(nextClientNumber);
        assert nextClientNumber > 0 : "The next client number must remain positive.";
        String clientId = String.format(Locale.ROOT, "C%03d", nextClientNumber);
        if (clients.containsKey(clientId)) {
            throw new ClientException("Cannot add client " + clientId + ": an ID is already in use.");
        }
        assert !clients.containsKey(clientId) : "A generated client ID must not replace an existing client.";
        Client client = new Client(clientId, name);
        clients.put(clientId, client);
        nextClientNumber = followingNumber;
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

    /**
     * Deletes a note belonging to an existing client.
     *
     * @param clientId Owning client's ID.
     * @param noteIndex One-based note position.
     * @return Deleted note text.
     * @throws ClientException If the client or note does not exist.
     */
    public String deleteNote(String clientId, int noteIndex) {
        return getClient(clientId).deleteNote(noteIndex);
    }

    /** Renames an existing client without replacing its ID, notes or project links. */
    public Client editClient(String clientId, String name) {
        Client client = getClient(clientId);
        client.setName(name);
        return client;
    }

    /**
     * Finds a client eligible for deletion, rejecting projects of every status.
     *
     * @param clientId ID of the client to check.
     * @param projectManager Manager containing all current projects.
     * @return Client with no associated projects.
     * @throws ClientException If the client ID is blank or the client is missing.
     * @throws ClientHasProjectsException If the client has any projects.
     */
    public Client getDeletableClient(String clientId, ProjectManager projectManager) {
        Client client = getClient(clientId);
        boolean hasProjects = projectManager.getProjects().stream()
                .anyMatch(project -> project.getClientId().equals(client.getClientId()));
        if (hasProjects) {
            throw new ClientHasProjectsException(client.getClientId());
        }
        return client;
    }

    /**
     * Deletes a client and its notes, preserving the next client ID.
     * Rechecks project links immediately before changing any data.
     *
     * @param clientId ID of the client to delete.
     * @param projectManager Manager containing all current projects.
     * @return Deleted client, with its notes cleared.
     * @throws ClientException If the client ID is blank or the client is missing.
     * @throws ClientHasProjectsException If the client has any projects.
     */
    public Client deleteClient(String clientId, ProjectManager projectManager) {
        Client client = getDeletableClient(clientId, projectManager);
        trashcanManager.storeClient(client);
        client.clearNotes();
        clients.remove(client.getClientId());
        return client;
    }

    /** Returns independent copies of deleted clients and their notes in ID order. */
    public List<Client> getDeletedClients() {
        return trashcanManager.getDeletedClients();
    }

    /**
     * Registers a restored client with its original ID and notes.
     * Keeps clients in numeric ID order without changing the ID counter.
     *
     * @param client Retained snapshot to make active.
     * @throws ClientException If the ID is invalid or already active.
     */
    void registerRestoredClient(Client client) {
        String id = client.getClientId();
        if (clients.containsKey(id)) {
            throw new ClientException("Cannot restore client " + id + ": an ID is already in use.");
        }
        // Active IDs were validated when allocated or restored; validate this one before insertion too.
        clientNumber(id);
        clients.put(id, client);
        sortClientsById();
    }

    /** Reorders validated active IDs, preparing the complete sorted list before clearing the map. */
    private void sortClientsById() {
        List<Client> sortedClients = clients.values().stream()
                .sorted(Comparator.comparingInt(
                        client -> Integer.parseInt(client.getClientId().substring(1))))
                .toList();

        clients.clear();
        for (Client client : sortedClients) {
            clients.put(client.getClientId(), client);
        }
    }

    /**
     * Registers a deleted client read from storage without making it active.
     * Rejects duplicate IDs and advances the counter to prevent ID reuse.
     *
     * @param client Saved deleted client including its notes.
     * @throws ClientException If its ID is invalid or already exists in active data or the trashcan.
     */
    public void loadDeletedClient(Client client) {
        String id = client.getClientId();
        if (clients.containsKey(id)) {
            throw new ClientException("Duplicate saved client " + id + ".");
        }
        int followingNumber = nextNumberAfter(clientNumber(id));
        // Validation and checked arithmetic precede both the snapshot and counter updates.
        trashcanManager.storeClient(client);
        nextClientNumber = Math.max(nextClientNumber, followingNumber);
    }

    /**
     * Parses only IDs that the client allocator can produce.
     * Rejects aliases such as C1 or C0001 so one numeric identity has one spelling.
     *
     * @param id Saved or retained client ID.
     * @return Positive client number with room for its following counter.
     * @throws ClientException If the format or numeric range is invalid.
     */
    private int clientNumber(String id) {
        if (!id.matches("C[0-9]{3,}")) {
            throw new ClientException("Invalid client ID " + id + ".");
        }
        int number;
        try {
            number = Integer.parseInt(id.substring(1));
        } catch (NumberFormatException exception) {
            throw new ClientException("Invalid client ID " + id + ".");
        }
        if (number < 1 || number == Integer.MAX_VALUE
                || !id.equals(String.format(Locale.ROOT, "C%03d", number))) {
            throw new ClientException("Invalid client ID " + id + ".");
        }
        return number;
    }

    /**
     * Calculates the successor before a client is registered.
     * Integer.MAX_VALUE is a valid exhausted counter, but cannot be allocated as an ID.
     *
     * @param number Current client number.
     * @return Following positive counter.
     * @throws ClientException If the counter is invalid or exhausted.
     */
    private int nextNumberAfter(int number) {
        if (number < 1 || number == Integer.MAX_VALUE) {
            throw new ClientException("No further client IDs can be allocated.");
        }
        return number + 1;
    }

    /** Finds Unicode case-insensitive literal name substrings in numeric ID order, rejecting blank searches. */
    public List<Client> findClients(String name) {
        String search = name == null ? "" : name.trim();
        if (search.isBlank()) {
            throw new ClientException("A search name must not be blank.");
        }
        // Literal matching keeps punctuation ordinary while handling Unicode case variants such as sigma.
        Pattern searchPattern = Pattern.compile(search,
                Pattern.LITERAL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        return clients.values().stream()
                .filter(client -> searchPattern.matcher(client.getName()).find())
                .toList();
    }

    /**
     * Returns an unmodifiable snapshot of active clients in numeric ID order.
     *
     * @return Active clients, including those loaded or restored during this session.
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
