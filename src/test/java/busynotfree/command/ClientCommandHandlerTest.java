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

import busynotfree.model.ClientManager;
import busynotfree.model.ProjectManager;
import busynotfree.ui.Ui;

/**
 * Exercises client commands through the shared router, including error recovery.
 */
class ClientCommandHandlerTest {
    private static final String ADD_FORMAT = "Format: client.add name/CLIENT_NAME";

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
