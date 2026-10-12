package busynotfree.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import busynotfree.exception.ClientException;

/**
 * Tests client validation and preservation of multiple notes.
 */
class ClientTest {
    @Test
    public void constructor_paddedValues_trimsEdgesAndStartsWithoutNotes() {
        Client client = new Client(" C001 ", " Acme  Studio ");

        assertEquals("C001", client.getClientId());
        assertEquals("Acme  Studio", client.getName());
        assertTrue(client.getNotes().isEmpty());
    }

    @Test
    public void constructor_missingValues_throwsClientException() {
        assertThrows(ClientException.class, () -> new Client(null, "Acme"));
        assertThrows(ClientException.class, () -> new Client(" \t ", "Acme"));
        assertThrows(ClientException.class, () -> new Client("C001", null));
        assertThrows(ClientException.class, () -> new Client("C001", " \t "));
    }

    @Test
    public void addNote_multipleNotes_preservesTextAndInsertionOrder() {
        Client client = new Client("C001", "Acme Studio");
        client.addNote("  Prefers  weekly updates  ");
        client.addNote("Review UI/UX at https://example.com");
        client.addNote("Prefers  weekly updates");

        assertEquals(List.of("Prefers  weekly updates", "Review UI/UX at https://example.com",
                "Prefers  weekly updates"), client.getNotes());
    }

    @Test
    public void addNote_blankNote_preservesExistingNotes() {
        Client client = new Client("C001", "Acme");
        client.addNote("Existing note");

        assertThrows(ClientException.class, () -> client.addNote(null));
        assertThrows(ClientException.class, () -> client.addNote(""));
        assertThrows(ClientException.class, () -> client.addNote(" \t "));
        assertEquals(List.of("Existing note"), client.getNotes());
    }

    @Test
    public void getNotes_snapshot_cannotChangeClientOrObserveLaterNotes() {
        Client client = new Client("C001", "Acme");
        client.addNote("First");
        List<String> snapshot = client.getNotes();

        assertThrows(UnsupportedOperationException.class, () -> snapshot.add("External note"));
        client.addNote("Second");
        assertEquals(List.of("First"), snapshot);
        assertEquals(List.of("First", "Second"), client.getNotes());
    }
}
