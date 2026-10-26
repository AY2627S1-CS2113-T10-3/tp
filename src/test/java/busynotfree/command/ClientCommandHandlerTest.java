package busynotfree.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import busynotfree.model.Client;
import busynotfree.model.ClientManager;
import busynotfree.model.Project;
import busynotfree.model.ProjectManager;
import busynotfree.model.ProjectStatus;
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
    public void execute_nonClientCommand_detectsIncorrectInternalRouting() {
        ClientCommandHandler clientHandler = new ClientCommandHandler(manager, ui);
        ParsedCommand command = new ParsedCommand("project", "add", "name/Acme Studio");

        assertThrows(AssertionError.class, () -> clientHandler.execute(command));
        assertTrue(manager.getClients().isEmpty());
        assertTrue(ui.messages.isEmpty());
        assertTrue(ui.errors.isEmpty());
    }

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

        assertEquals(List.of("C001 | Acme Studio", "Note 1: Prefers weekly updates",
                "Note 2: Follow up on Friday"), ui.messages);
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
                "C001 | Acme  UI/UX Studio", "Note 1: Review  src/main at https://example.com"), ui.messages);
        assertTrue(ui.errors.isEmpty());
    }

    @Test
    public void execute_unsupportedClientAction_reportsErrorWithoutMutation() {
        assertFalse(handler.execute("client.unknown C001"));

        assertEquals(List.of("Unsupported action: unknown."), ui.errors);
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

        assertEquals(List.of("Client C001 does not exist. Use client.list to see client IDs."), ui.errors);
        assertEquals(1, projectManager.getProjects().size());
        assertEquals("C001", projectManager.getProject("P001").getClientId());
        assertEquals(2, projectManager.getProject("P001").getItemCount());
        assertTrue(ui.messages.contains("Created project P001 \"Website\" for client C001 \"Acme Studio\"."));
        assertTrue(ui.messages.contains("Added task T001 \"Wireframe\" to project P001 \"Website\"."));
        assertTrue(ui.messages.contains("Added event E001 \"Meeting\" to project P001 \"Website\"."));
    }

    /** Invalid deletion arguments must leave clients and notes unchanged. */
    @TestFactory
    public List<DynamicTest> execute_invalidDeleteArguments_preservesClient() {
        List<String> inputs = List.of("client.delete", "client.delete   ", "client.delete invalid",
                "client.delete C999", "client.delete c001", "client.delete C001 C002");
        List<String> errors = List.of("A client ID must not be blank.", "A client ID must not be blank.",
                "Client invalid does not exist.", "Client C999 does not exist.",
                "Client c001 does not exist.", "Provide exactly one client ID.");
        List<DynamicTest> tests = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            String input = inputs.get(i);
            String expectedError = errors.get(i);
            tests.add(dynamicTest(input, () -> {
                ClientCommandHandlerTest fixture = new ClientCommandHandlerTest();
                Client client = fixture.manager.addClient("Acme");
                fixture.manager.addNote("C001", "Keep this note");

                assertFalse(fixture.handler.execute(input));

                assertEquals(List.of(expectedError), fixture.ui.errors);
                assertEquals(List.of("Format: client.delete CLIENT_ID"), fixture.ui.messages);
                assertFalse(fixture.handler.isAwaitingDeletionConfirmation());
                assertEquals(List.of(client), fixture.manager.getClients());
                assertEquals(List.of("Keep this note"), client.getNotes());
            }));
        }
        return tests;
    }

    @Test
    public void execute_deleteConfirmed_removesClientAndNotesOnlyAfterYes() {
        Client client = manager.addClient("Acme");
        manager.addNote("C001", "First note");
        manager.addNote("C001", "Second note");
        Client otherClient = manager.addClient("Other");
        manager.addNote("C002", "Keep this note");

        handler.execute("client.delete C001");

        assertTrue(handler.isAwaitingDeletionConfirmation());
        assertTrue(manager.exists("C001"));
        assertEquals(List.of("First note", "Second note"), client.getNotes());
        assertEquals(List.of("Delete client C001 \"Acme\" and 2 associated notes?",
                "Type yes to confirm deletion or no to cancel."), ui.messages);

        handler.execute("yes");

        assertFalse(handler.isAwaitingDeletionConfirmation());
        assertFalse(manager.exists("C001"));
        assertTrue(client.getNotes().isEmpty());
        assertEquals(List.of(otherClient), manager.getClients());
        assertEquals(List.of("Keep this note"), otherClient.getNotes());
        assertEquals("Deleted client C001: Acme and all associated notes.", ui.messages.getLast());
        assertTrue(ui.errors.isEmpty());
    }

    @Test
    public void execute_deleteCancelled_preservesClientAndNotes() {
        Client client = manager.addClient("Acme");
        manager.addNote("C001", "Keep this note");

        handler.execute("client.delete C001");
        handler.execute("no");

        assertFalse(handler.isAwaitingDeletionConfirmation());
        assertEquals(List.of(client), manager.getClients());
        assertEquals(List.of("Keep this note"), client.getNotes());
        assertEquals("Client deletion cancelled.", ui.messages.getLast());
        assertTrue(ui.errors.isEmpty());

        // Cancellation must return the handler to normal command processing.
        handler.execute("client.view C001");
        assertEquals("Note 1: Keep this note", ui.messages.getLast());
    }

    @Test
    public void execute_invalidDeleteAnswers_keepsConfirmationPending() {
        Client client = manager.addClient("Acme");
        manager.addNote("C001", "Keep this note");
        handler.execute("client.delete C001");

        for (String answer : List.of("", "YES", "yes please", "exit", "help", "client.delete C001")) {
            assertFalse(handler.execute(answer));
            assertTrue(handler.isAwaitingDeletionConfirmation());
            assertEquals(List.of(client), manager.getClients());
            assertEquals(List.of("Keep this note"), client.getNotes());
            assertEquals("Please type yes to confirm deletion or no to cancel.", ui.errors.getLast());
            assertEquals("Type yes to confirm deletion or no to cancel.", ui.messages.getLast());
        }

        handler.execute("no");
        assertFalse(handler.isAwaitingDeletionConfirmation());
    }

    /** Every project status blocks deletion, including completed and cancelled. */
    @TestFactory
    public List<DynamicTest> execute_deleteClientWithProject_rejectsEveryStatus() {
        List<DynamicTest> tests = new ArrayList<>();
        for (ProjectStatus status : ProjectStatus.values()) {
            tests.add(dynamicTest(status.name(), () -> {
                ClientCommandHandlerTest fixture = new ClientCommandHandlerTest();
                Client client = fixture.manager.addClient("Acme");
                fixture.manager.addNote("C001", "Keep this note");
                Project project = fixture.projectManager.addProject(
                        "C001", "Website", LocalDate.of(2026, 11, 30));
                fixture.projectManager.changeProjectStatus(project.getProjectId(), status);

                fixture.handler.execute("client.delete C001");

                assertFalse(fixture.handler.isAwaitingDeletionConfirmation());
                assertEquals(List.of("Client C001 has associated projects. Delete those projects first."),
                        fixture.ui.errors);
                assertTrue(fixture.ui.messages.isEmpty());
                assertEquals(List.of(client), fixture.manager.getClients());
                assertEquals(List.of("Keep this note"), client.getNotes());
                assertEquals(List.of(project), fixture.projectManager.getProjects());
                assertEquals(status, project.getStatus());
            }));
        }
        return tests;
    }

    @Test
    public void execute_deleteConfirmation_rechecksProjectLinksBeforeDeleting() {
        Client client = manager.addClient("Acme");
        manager.addNote("C001", "Keep this note");
        handler.execute("client.delete C001");

        // Simulates another caller changing the model while confirmation is pending.
        Project project = projectManager.addProject("C001", "Website", LocalDate.of(2026, 11, 30));
        handler.execute("yes");

        assertFalse(handler.isAwaitingDeletionConfirmation());
        assertEquals(List.of(client), manager.getClients());
        assertEquals(List.of("Keep this note"), client.getNotes());
        assertEquals(List.of(project), projectManager.getProjects());
        assertEquals(List.of("Client C001 has associated projects. Delete those projects first."), ui.errors);
        assertFalse(ui.messages.contains("Deleted client C001: Acme and all associated notes."));
    }

    /** Missing or invalid client IDs must not change any notes. */
    @TestFactory
    public List<DynamicTest> execute_deleteNoteInvalidClient_preservesNotes() {
        return invalidNoteDeletionTests(List.of("client.delete_note", "client.delete_note index/1",
                "client.delete_note invalid index/1", "client.delete_note C999 index/1",
                "client.delete_note c001 index/1", "client.delete_note C001 C002 index/1"));
    }

    /** Missing, malformed, repeated and out-of-range indexes must be rejected. */
    @TestFactory
    public List<DynamicTest> execute_deleteNoteInvalidIndex_preservesNotes() {
        return invalidNoteDeletionTests(List.of("client.delete_note C001", "client.delete_note C001 index/",
                "client.delete_note C001 index/   ", "client.delete_note C001 index/abc",
                "client.delete_note C001 index/1.5", "client.delete_note C001 index/0",
                "client.delete_note C001 index/-1", "client.delete_note C001 index/3",
                "client.delete_note C001 index/99999999999999999999",
                "client.delete_note C001 index/1 index/2", "client.delete_note C001 index/1 extra"));
    }

    /** Creates isolated validation tests and verifies that no deletion is pending. */
    private List<DynamicTest> invalidNoteDeletionTests(List<String> inputs) {
        List<DynamicTest> tests = new ArrayList<>();
        for (String input : inputs) {
            tests.add(dynamicTest(input, () -> {
                ClientCommandHandlerTest fixture = new ClientCommandHandlerTest();
                Client client = fixture.manager.addClient("Acme");
                fixture.manager.addNote("C001", "First note");
                fixture.manager.addNote("C001", "Second note");

                assertFalse(fixture.handler.execute(input));

                assertEquals(1, fixture.ui.errors.size());
                assertFalse(fixture.ui.errors.getFirst().isBlank());
                assertEquals(List.of("Format: client.delete_note CLIENT_ID index/NOTE_INDEX"), fixture.ui.messages);
                assertFalse(fixture.handler.isAwaitingDeletionConfirmation());
                assertEquals(List.of(client), fixture.manager.getClients());
                assertEquals(List.of("First note", "Second note"), client.getNotes());
            }));
        }
        return tests;
    }

    /** Verifies one-based indexing and preservation of remaining note order. */
    @TestFactory
    public List<DynamicTest> execute_deleteNoteConfirmed_removesSelectedPosition() {
        List<List<String>> notes = List.of(List.of("A", "B", "C"), List.of("A", "B", "C"),
                List.of("A", "B", "C"), List.of("A"), List.of("Same", "Other", "Same"));
        List<Integer> indexes = List.of(1, 2, 3, 1, 3);
        List<List<String>> remaining = List.of(List.of("B", "C"), List.of("A", "C"),
                List.of("A", "B"), List.of(), List.of("Same", "Other"));
        List<String> names = List.of("first", "middle", "last", "only", "duplicate");
        List<DynamicTest> tests = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            List<String> original = notes.get(i);
            List<String> expected = remaining.get(i);
            int index = indexes.get(i);
            tests.add(dynamicTest(names.get(i), () -> {
                ClientCommandHandlerTest fixture = new ClientCommandHandlerTest();
                Client client = fixture.manager.addClient("Acme");
                for (String note : original) {
                    fixture.manager.addNote("C001", note);
                }
                Client other = fixture.manager.addClient("Other client");
                fixture.manager.addNote("C002", "Keep this note");

                fixture.handler.execute("client.delete_note C001 index/" + index);

                assertTrue(fixture.handler.isAwaitingDeletionConfirmation());
                assertEquals(original, client.getNotes());
                assertEquals(List.of("Delete note " + index + " from client C001: \""
                        + original.get(index - 1) + "\"?", "Type yes to confirm deletion or no to cancel."),
                        fixture.ui.messages);

                fixture.handler.execute("yes");

                assertFalse(fixture.handler.isAwaitingDeletionConfirmation());
                assertEquals(expected, client.getNotes());
                assertEquals(List.of("Keep this note"), other.getNotes());
                assertEquals(List.of(client, other), fixture.manager.getClients());
                assertEquals("Deleted note " + index + " from client C001.", fixture.ui.messages.getLast());
                assertTrue(fixture.ui.errors.isEmpty());
                fixture.ui.messages.clear();
                fixture.handler.execute("client.view C001");
                for (int position = 0; position < expected.size(); position++) {
                    assertEquals("Note " + (position + 1) + ": " + expected.get(position),
                            fixture.ui.messages.get(position + 1));
                }
                if (expected.isEmpty()) {
                    assertEquals("Notes: No notes found.", fixture.ui.messages.getLast());
                }
            }));
        }
        return tests;
    }

    @Test
    public void execute_deleteNoteCancelled_preservesNotes() {
        Client client = manager.addClient("Acme");
        manager.addNote("C001", "Keep this note");
        handler.execute("client.delete_note C001 index/1");
        handler.execute("no");

        assertFalse(handler.isAwaitingDeletionConfirmation());
        assertEquals(List.of("Keep this note"), client.getNotes());
        assertEquals("Note deletion cancelled.", ui.messages.getLast());
        assertTrue(ui.errors.isEmpty());
        handler.execute("client.view C001");
        assertEquals("Note 1: Keep this note", ui.messages.getLast());
    }

    @Test
    public void execute_deleteNoteInvalidAnswers_keepsConfirmationPending() {
        Client client = manager.addClient("Acme");
        manager.addNote("C001", "Keep this note");
        handler.execute("client.delete_note C001 index/1");
        for (String answer : List.of("", "YES", "yes please", "exit", "save", "help")) {
            assertFalse(handler.execute(answer));
            assertTrue(handler.isAwaitingDeletionConfirmation());
            assertEquals(List.of("Keep this note"), client.getNotes());
            assertEquals("Please type yes to confirm deletion or no to cancel.", ui.errors.getLast());
            assertEquals("Type yes to confirm deletion or no to cancel.", ui.messages.getLast());
        }
        handler.execute("no");
        assertFalse(handler.isAwaitingDeletionConfirmation());
    }

    @Test
    public void execute_deleteNoteFromEmptyClient_reportsErrorWithoutConfirmation() {
        Client client = manager.addClient("Acme");
        handler.execute("client.delete_note C001 index/1");

        assertEquals(List.of("Note index 1 does not exist for client C001."), ui.errors);
        assertEquals(List.of("Format: client.delete_note CLIENT_ID index/NOTE_INDEX"), ui.messages);
        assertFalse(handler.isAwaitingDeletionConfirmation());
        assertEquals(List.of(client), manager.getClients());
        assertTrue(client.getNotes().isEmpty());
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
