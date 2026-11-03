package busynotfree.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import busynotfree.model.Client;
import busynotfree.model.ClientManager;
import busynotfree.model.ProjectManager;

/** Checks malformed client Unicode and complete persistence of large note collections. */
class DataStorageClientEdgeTest {
    @TempDir
    private Path directory;

    @Test
    public void load_unpairedClientSurrogates_rejectsFileWithoutRewritingIt() throws IOException {
        Path file = directory.resolve("clients.properties");
        DataStorage storage = new DataStorage(file);
        ClientManager clients = new ClientManager();
        clients.addClient("Acme").addNote("Existing note");
        storage.save(clients, new ProjectManager(clients));
        String baseline = Files.readString(file, StandardCharsets.UTF_8);

        for (String field : List.of("name", "note.0")) {
            for (String escaped : List.of("\\uD800", "\\uDC00", "a\\uD800b", "a\\uDC00b")) {
                String malformed = baseline + "\nclient.0." + field + "=" + escaped + "\n";
                Files.writeString(file, malformed, StandardCharsets.UTF_8);

                IOException error = assertThrows(IOException.class, storage::load);

                assertTrue(error.getMessage().contains("valid Unicode text"));
                assertEquals(malformed, Files.readString(file, StandardCharsets.UTF_8));
            }
        }
    }

    @Test
    public void saveAndLoad_validUnicodeAndControls_preservesStoredClientText() throws IOException {
        DataStorage storage = new DataStorage(directory.resolve("unicode.properties"));
        ClientManager clients = new ClientManager();
        Client client = clients.addClient("Acme 😀");
        String note = "Call 📞\tweekly\u001b[31m";
        client.addNote(note);
        storage.save(clients, new ProjectManager(clients));

        DataStorage.LoadedData loaded = storage.load();
        storage.save(loaded.clientManager(), loaded.projectManager());
        Client restored = storage.load().clientManager().getClient("C001");

        assertEquals("Acme 😀", restored.getName());
        assertEquals(List.of(note), restored.getNotes());
    }

    @Test
    public void saveAndLoad_manyActiveAndDeletedNotes_preservesEveryNoteAndCounter() throws IOException {
        DataStorage storage = new DataStorage(directory.resolve("many-notes.properties"));
        ClientManager clients = new ClientManager();
        ProjectManager projects = new ProjectManager(clients);
        Client active = clients.addClient("Active client");
        Client deleted = clients.addClient("Deleted client");
        for (int i = 0; i < 4000; i++) {
            active.addNote("Active note " + i);
            deleted.addNote("Deleted note " + i);
        }
        List<String> deletedNotes = deleted.getNotes();
        clients.deleteClient("C002", projects);

        storage.save(clients, projects);
        ClientManager loaded = storage.load().clientManager();

        assertEquals(active.getNotes(), loaded.getClient("C001").getNotes());
        assertEquals(deletedNotes, loaded.getDeletedClients().getFirst().getNotes());
        assertEquals(3, loaded.getNextClientNumber());
    }
}
