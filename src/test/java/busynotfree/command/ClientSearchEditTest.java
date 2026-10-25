package busynotfree.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import busynotfree.exception.ClientException;
import busynotfree.model.Client;
import busynotfree.model.ClientManager;
import busynotfree.model.ProjectManager;
import busynotfree.storage.DataStorage;
import busynotfree.ui.Ui;

/** Tests client searching and renaming through the shared command router. */
class ClientSearchEditTest {
    private final ClientManager clients = new ClientManager();
    private final ProjectManager projects = new ProjectManager(clients);
    private final RecordingUi ui = new RecordingUi();
    private final CommandHandler handler = new CommandHandler(new CommandParser(), ui, projects, clients);

    @Test
    public void find_matchesNamesIgnoringCase_displaysIdsInCreationOrder() {
        clients.addClient("Acme Studio");
        clients.addClient("Bright Ideas");
        clients.addClient("ACME UI/UX");

        handler.execute("client.find  aCmE ");
        assertEquals(List.of("Found 2 matching clients.", "1. C001 | Acme Studio", "2. C003 | ACME UI/UX"),
                ui.messages);
        ui.messages.clear();
        handler.execute("client.find acme ui/ux");
        assertEquals(List.of("Found 1 matching client.", "1. C003 | ACME UI/UX"), ui.messages);
        assertTrue(ui.errors.isEmpty());
    }

    @Test
    public void find_noMatches_displaysEmptyMessage() {
        handler.execute("client.find Acme");
        clients.addClient("Bright Ideas");
        handler.execute("client.find Acme");

        assertEquals(List.of("Found 0 matching clients.", "No clients found.",
                "Found 0 matching clients.", "No clients found."), ui.messages);
        assertTrue(ui.errors.isEmpty());
    }

    @Test
    public void edit_existingClient_preservesIdentityNotesAndProjects(@TempDir Path directory) throws IOException {
        Client client = clients.addClient("Acme");
        clients.addClient("Bright Ideas");
        client.addNote("Original note");
        projects.addProject("C001", "Website", LocalDate.of(2026, 11, 30));

        handler.execute("client.edit C001 name/ Revised  UI/UX ");

        assertEquals(List.of("Updated client C001 from \"Acme\" to \"Revised  UI/UX\"."), ui.messages);
        assertTrue(ui.errors.isEmpty());
        assertSame(client, clients.getClient("C001"));
        assertEquals("Revised  UI/UX", client.getName());
        assertEquals(List.of("Original note"), client.getNotes());
        assertEquals("C001", projects.getProject("P001").getClientId());
        assertEquals("Bright Ideas", clients.getClient("C002").getName());
        assertEquals(3, clients.getNextClientNumber());
        DataStorage storage = new DataStorage(directory.resolve("clients.properties"));
        storage.save(clients, projects);
        DataStorage.LoadedData loaded = storage.load();
        assertEquals("Revised  UI/UX", loaded.clientManager().getClient("C001").getName());
        assertEquals(List.of("Original note"), loaded.clientManager().getClient("C001").getNotes());
        assertEquals("C001", loaded.projectManager().getProject("P001").getClientId());
    }

    @Test
    public void execute_invalidSearchOrEdit_reportsErrorAndUsageWithoutRenaming() {
        Client client = clients.addClient("Acme");
        for (String input : List.of("client.find", "client.find \t ", "client.edit", "client.edit name/New",
                "client.edit C001", "client.edit C001 name/", "client.edit C001 name/ \t ",
                "client.edit C999 name/New", "client.edit c001 name/New", "client.edit invalid name/New",
                "client.edit C001 C002 name/New", "client.edit C001 name/First name/Second",
                "client.edit C001 name/New due/2026-11-30")) {
            ui.messages.clear();
            ui.errors.clear();
            handler.execute(input);
            assertEquals(1, ui.errors.size(), input);
            assertEquals(List.of(input.startsWith("client.find") ? "Format: client.find NAME"
                    : "Format: client.edit CLIENT_ID name/NEW_NAME"), ui.messages, input);
            assertEquals("Acme", client.getName(), input);
            assertEquals(1, clients.getClients().size(), input);
        }
    }

    @Test
    public void model_invalidNames_rejectsNullAndBlankValuesWithoutRenaming() {
        Client client = clients.addClient("Acme");
        for (String name : new String[]{null, "", " \t "}) {
            assertThrows(ClientException.class, () -> clients.findClients(name));
            assertThrows(ClientException.class, () -> clients.editClient("C001", name));
            assertEquals("Acme", client.getName());
        }
    }

    /** Captures responses so tests verify visible results as well as model state. */
    private static class RecordingUi extends Ui {
        private final List<String> messages = new ArrayList<>();
        private final List<String> errors = new ArrayList<>();

        @Override
        public void showMessage(String message) {
            messages.add(message);
        }

        @Override
        public void showError(String message) {
            errors.add(message);
        }

        @Override
        public void showSeparator() {
            // Separators do not affect command behavior.
        }
    }
}
