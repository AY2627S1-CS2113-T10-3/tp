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
     * @throws ClientException If the ID or name is null, blank or contains malformed Unicode.
     */
    public Client(String clientId, String name) {
        if (clientId == null || clientId.trim().isBlank()) {
            throw new ClientException("A client ID must not be blank.");
        }
        validateUnicode(clientId, "ID");
        this.clientId = clientId.trim();
        setName(name);
    }

    public String getClientId() {
        return clientId;
    }

    public String getName() {
        return name;
    }

    /** Updates only the name, validating nonblank, well-formed Unicode before changing the client. */
    public void setName(String name) {
        if (name == null || name.trim().isBlank()) {
            throw new ClientException("A client name must not be blank.");
        }
        validateUnicode(name, "name");
        this.name = name.trim();
    }

    /**
     * Appends a note without replacing previously recorded notes.
     *
     * @param note Non-blank note text; internal spaces are preserved.
     * @throws ClientException If the note is null, blank or contains malformed Unicode.
     */
    public void addNote(String note) {
        if (note == null || note.trim().isBlank()) {
            throw new ClientException("A client note must not be blank.");
        }
        validateUnicode(note, "note");
        notes.add(note.trim());
    }

    /** Rejects isolated UTF-16 surrogates that cannot be written to the UTF-8 save file. */
    private static void validateUnicode(String text, String field) {
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (Character.isHighSurrogate(character) && i + 1 < text.length()
                    && Character.isLowSurrogate(text.charAt(i + 1))) {
                i++;
            } else if (Character.isSurrogate(character)) {
                throw new ClientException("A client " + field + " must contain valid Unicode text.");
            }
        }
    }

    /**
     * Returns an unmodifiable snapshot of notes in insertion order.
     *
     * @return Notes recorded for this client.
     */
    public List<String> getNotes() {
        return List.copyOf(notes);
    }

    /**
     * Returns a note using its displayed, one-based index.
     *
     * @param noteIndex Position starting at 1.
     * @return Selected note.
     * @throws ClientException If the index does not identify a note.
     */
    public String getNote(int noteIndex) {
        if (noteIndex < 1 || noteIndex > notes.size()) {
            throw new ClientException("Note index " + noteIndex
                    + " does not exist for client " + clientId + ".");
        }
        return notes.get(noteIndex - 1);
    }

    /**
     * Deletes one note while preserving the order of the remaining notes.
     *
     * @param noteIndex Position starting at 1.
     * @return Deleted note text.
     * @throws ClientException If the index does not identify a note.
     */
    public String deleteNote(int noteIndex) {
        getNote(noteIndex);
        return notes.remove(noteIndex - 1);
    }

    /** Removes every note when the owning client is deleted. */
    void clearNotes() {
        notes.clear();
    }

    /** Copies client details so trash entries cannot be changed through old references. */
    Client copy() {
        Client copy = new Client(clientId, name);
        notes.forEach(copy::addNote);
        return copy;
    }
}
