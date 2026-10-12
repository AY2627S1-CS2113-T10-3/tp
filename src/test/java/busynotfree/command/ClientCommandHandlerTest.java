package busynotfree.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import busynotfree.model.Client;
import busynotfree.model.ClientManager;
import busynotfree.model.ProjectManager;
import busynotfree.ui.Ui;

/**
 * Exercises client commands through the shared router, including error recovery.
 */
class ClientCommandHandlerTest {
    private static final String ADD_FORMAT = "Format: client.add name/CLIENT_NAME";
    private static final String NOTE_FORMAT = "Format: client.note CLIENT_ID text/NOTE";
    private static final String VIEW_FORMAT = "Format: client.view CLIENT_ID";

    private final ClientManager manager = new ClientManager();
    private final ProjectManager projectManager = new ProjectManager(manager);
    private final RecordingUi ui = new RecordingUi();
    private final CommandHandler handler = new CommandHandler(new CommandParser(), ui, projectManager, manager);

    @Test
    public void execute_addClient_displaysAssignedIdAndName() {
        assertFalse(handler.execute("client.add name/Acme Studio"));
        assertFalse(handler.execute("client.add name/Bright Ideas"));

        assertEquals(List.of("Added client C001: Acme Studio.", "Added client C002: Bright Ideas."), ui.messages);
        assertEquals("Acme Studio", manager.getClient("C001").getName());
        assertEquals("Bright Ideas", manager.getClient("C002").getName());
        assertTrue(ui.errors.isEmpty());
    }

    /** Tests missing, empty, repeated and inappropriate name arguments in fresh fixtures. */
    @TestFactory
    public List<DynamicTest> execute_invalidAddArguments_reportsErrorWithoutConsumingId() {
        List<String> inputs = List.of("client.add", "client.add Acme", "client.add name/",
                "client.add name/ \t ", "client.add name/Acme name/Other",
                "client.add name/Acme due/2026-11-30", "client.add stray name/Acme");
        List<String> errors = List.of("Missing required prefix: name/.", "Missing required prefix: name/.",
                "A value is required after name/.", "A value is required after name/.", "Repeated prefix: name/.",
                "Unexpected prefix: due/.", "Arguments must start with a prefix such as name/.");
        List<DynamicTest> tests = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            String input = inputs.get(i);
            String error = errors.get(i);
            tests.add(dynamicTest(input, () -> {
                ClientCommandHandlerTest fixture = new ClientCommandHandlerTest();
                assertFalse(fixture.handler.execute(input));
                assertEquals(List.of(error), fixture.ui.errors);
                assertEquals(List.of(ADD_FORMAT), fixture.ui.messages);
                assertTrue(fixture.manager.getClients().isEmpty());
                fixture.handler.execute("client.add name/Acme");
                assertEquals("C001", fixture.manager.getClients().getFirst().getClientId());
            }));
        }
        return tests;
    }

    @Test
    public void execute_noteClient_appendsNotesToSpecifiedClient() {
        Client first = manager.addClient("Acme Studio");
        Client second = manager.addClient("Bright Ideas");
        assertFalse(handler.execute("client.note C002 text/Prefers weekly updates"));
        assertFalse(handler.execute("client.note C002 text/Follow up on Friday"));

        assertEquals(List.of("Note added to client C002.", "Note added to client C002."), ui.messages);
        assertEquals(List.of("Prefers weekly updates", "Follow up on Friday"), second.getNotes());
        assertTrue(first.getNotes().isEmpty());
        assertTrue(ui.errors.isEmpty());
    }

    /** Tests malformed note arguments without mutating previously recorded notes. */
    @TestFactory
    public List<DynamicTest> execute_invalidNoteArguments_reportsErrorWithoutMutation() {
        List<String> inputs = List.of("client.note", "client.note text/Note", "client.note C001",
                "client.note C001 Note", "client.note C001 text/", "client.note C001 text/ \t ",
                "client.note C001 text/First text/Second", "client.note C001 text/Note name/Other",
                "client.note C001 extra text/Note", "client.note C999 text/Note",
                "client.note P001 text/Note", "client.note c001 text/Note", "client.note C1 text/Note");
        List<String> errors = List.of("A client ID is required before text/.",
                "A client ID is required before text/.", "Missing required prefix: text/.",
                "Missing required prefix: text/.", "A value is required after text/.",
                "A value is required after text/.", "Repeated prefix: text/.", "Unexpected prefix: name/.",
                "Arguments must start with a prefix such as name/.", "Client C999 does not exist.",
                "Client P001 does not exist.", "Client c001 does not exist.", "Client C1 does not exist.");
        List<DynamicTest> tests = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            String input = inputs.get(i);
            String error = errors.get(i);
            tests.add(dynamicTest(input, () -> {
                ClientCommandHandlerTest fixture = new ClientCommandHandlerTest();
                Client client = fixture.manager.addClient("Acme");
                client.addNote("Original note");
                assertFalse(fixture.handler.execute(input));
                assertEquals(List.of(error), fixture.ui.errors);
                assertEquals(List.of(NOTE_FORMAT), fixture.ui.messages);
                assertEquals(List.of(client), fixture.manager.getClients());
                assertEquals(List.of("Original note"), client.getNotes());
                fixture.handler.execute("client.note C001 text/Corrected note");
                assertEquals(List.of("Original note", "Corrected note"), client.getNotes());
            }));
        }
        return tests;
    }

    @Test
    public void execute_viewClientWithoutNotes_displaysDetailsAndEmptyMessage() {
        manager.addClient("Acme Studio");

        assertFalse(handler.execute("client.view C001"));

        assertEquals(List.of("C001 | Acme Studio", "Notes: No notes found."), ui.messages);
        assertTrue(ui.errors.isEmpty());
    }

    @Test
    public void execute_viewClientWithNotes_displaysAllNotesInOrder() {
        manager.addClient("Acme Studio");
        manager.addNote("C001", "Prefers weekly updates");
        manager.addNote("C001", "Follow up on Friday");

        assertFalse(handler.execute("client.view C001"));

        assertEquals(List.of("C001 | Acme Studio", "Notes: Prefers weekly updates",
                "Notes: Follow up on Friday"), ui.messages);
        assertTrue(ui.errors.isEmpty());
    }

    /** Checks missing, unknown, wrong-case and extra positional IDs. */
    @TestFactory
    public List<DynamicTest> execute_invalidViewArguments_reportsErrorAndUsage() {
        List<String> inputs = List.of("client.view", "client.view C999", "client.view P001",
                "client.view c001", "client.view C1", "client.view C001 C002");
        List<String> errors = List.of("A client ID must not be blank.", "Client C999 does not exist.",
                "Client P001 does not exist.", "Client c001 does not exist.", "Client C1 does not exist.",
                "Provide exactly one client ID.");
        List<DynamicTest> tests = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            String input = inputs.get(i);
            String error = errors.get(i);
            tests.add(dynamicTest(input, () -> {
                ClientCommandHandlerTest fixture = new ClientCommandHandlerTest();
                Client client = fixture.manager.addClient("Acme");
                assertFalse(fixture.handler.execute(input));
                assertEquals(List.of(error), fixture.ui.errors);
                assertEquals(List.of(VIEW_FORMAT), fixture.ui.messages);
                assertEquals(List.of(client), fixture.manager.getClients());
                assertTrue(client.getNotes().isEmpty());
            }));
        }
        return tests;
    }

    @Test
    public void execute_listClients_displaysNumberedIdsAndNamesInCreationOrder() {
        manager.addClient("Acme Studio");
        manager.addClient("Bright Ideas");
        manager.addNote("C001", "Only visible with client.view");

        assertFalse(handler.execute("client.list"));

        assertEquals(List.of("1. C001 | Acme Studio", "2. C002 | Bright Ideas"), ui.messages);
        assertTrue(ui.errors.isEmpty());
    }

    @Test
    public void execute_listWithoutClients_displaysEmptyMessage() {
        assertFalse(handler.execute("client.list"));

        assertEquals(List.of("No clients found."), ui.messages);
        assertTrue(ui.errors.isEmpty());
    }

    @Test
    public void execute_listWithExtraArguments_ignoresArgumentsAsGuideSpecifies() {
        assertFalse(handler.execute("client.list ignored arguments"));

        assertEquals(List.of("No clients found."), ui.messages);
        assertTrue(ui.errors.isEmpty());
    }

    @Test
    public void execute_paddedInputAndFreeText_preservesSlashesAndInternalSpaces() {
        handler.execute(" \tclient.add\tname/ Acme  UI/UX Studio \t ");
        handler.execute(" \tclient.note\tC001\ttext/ Review  src/main at https://example.com \t ");
        handler.execute(" \tclient.view\tC001 \t ");

        assertEquals("Acme  UI/UX Studio", manager.getClient("C001").getName());
        assertEquals(List.of("Review  src/main at https://example.com"), manager.getClient("C001").getNotes());
        assertEquals(List.of("Added client C001: Acme  UI/UX Studio.", "Note added to client C001.",
                "C001 | Acme  UI/UX Studio", "Notes: Review  src/main at https://example.com"), ui.messages);
        assertTrue(ui.errors.isEmpty());
    }

    @Test
    public void execute_unsupportedClientAction_reportsErrorWithoutMutation() {
        assertFalse(handler.execute("client.delete C001"));

        assertEquals(List.of("Unsupported action: delete."), ui.errors);
        assertTrue(ui.messages.isEmpty());
        assertTrue(manager.getClients().isEmpty());
    }

    @Test
    public void execute_clientThenProjectCommands_shareRealClientLookup() {
        handler.execute("project.add client/C001 name/Website due/2026-11-30");
        handler.execute("client.add name/Acme Studio");
        handler.execute("project.add client/C001 name/Website due/2026-11-30");
        handler.execute("task.add project/P001 name/Wireframe date/2026-10-15");
        handler.execute("event.add project/P001 name/Meeting date/2026-10-20");

        assertEquals(List.of("Client C001 does not exist."), ui.errors);
        assertEquals(1, projectManager.getProjects().size());
        assertEquals("C001", projectManager.getProject("P001").getClientId());
        assertEquals(2, projectManager.getProject("P001").getItemCount());
        assertTrue(ui.messages.contains("Created project P001 for client C001."));
        assertTrue(ui.messages.contains("Added task T001 to project P001."));
        assertTrue(ui.messages.contains("Added event E001 to project P001."));
    }

    /** Captures output through the shared UI, including delegated client responses. */
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
            // Command assertions focus on responses rather than decorative separators.
        }
    }
}
