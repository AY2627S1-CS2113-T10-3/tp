package busynotfree.model;

import java.util.ArrayList;
import java.util.List;

import busynotfree.exception.ClientException;

/**
 * Stores a client's assigned ID, name and notes in the order they were added.
 * The client manager assigns IDs and checks application-wide uniqueness.
 */
public final class Client {
    private final String clientId;
    private String name;
    private final List<String> notes = new ArrayList<>();

    /**
     * Creates a client with no notes and an externally assigned ID.
     *
     * @param clientId Unique ID assigned by the client manager.
     * @param name Non-blank client name.
     * @throws ClientException If the ID or name is null or blank.
     */
    public Client(String clientId, String name) {
        if (clientId == null || clientId.isBlank()) {
            throw new ClientException("A client ID must not be blank.");
        }
        this.clientId = clientId.trim();
        setName(name);
    }

    public String getClientId() {
        return clientId;
    }

    public String getName() {
        return name;
    }

    /** Updates only the name, rejecting null or blank values before changing the client. */
    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new ClientException("A client name must not be blank.");
        }
        this.name = name.trim();
    }

    /**
     * Appends a note without replacing previously recorded notes.
     *
     * @param note Non-blank note text; internal spaces are preserved.
     * @throws ClientException If the note is null or blank.
     */
    public void addNote(String note) {
        if (note == null || note.isBlank()) {
            throw new ClientException("A client note must not be blank.");
        }
        notes.add(note.trim());
    }

    /**
     * Returns an unmodifiable snapshot of notes in insertion order.
     *
     * @return Notes recorded for this client.
     */
    public List<String> getNotes() {
        return List.copyOf(notes);
    }
}
