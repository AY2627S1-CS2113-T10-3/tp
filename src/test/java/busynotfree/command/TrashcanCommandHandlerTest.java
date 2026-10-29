package busynotfree.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import busynotfree.exception.CommandException;
import busynotfree.exception.ProjectException;
import busynotfree.model.ClientManager;
import busynotfree.model.ProjectManager;
import busynotfree.model.TrashcanManager;
import busynotfree.ui.Ui;

/** Tests delegated command handling without the main handler's error-reporting boundary. */
class TrashcanCommandHandlerTest {
    private final TrashcanManager trashcan = new TrashcanManager();
    private final ClientManager clients = new ClientManager(trashcan);
    private final ProjectManager projects = new ProjectManager(clients, trashcan);
    private final RecordingUi ui = new RecordingUi();
    private final TrashcanCommandHandler handler = new TrashcanCommandHandler(trashcan, projects, clients, ui);

    @Test
    public void execute_wrongEntity_detectsIncorrectRouting() {
        assertThrows(AssertionError.class, () -> handler.execute(new ParsedCommand("client", "list", "")));
        assertTrue(ui.messages.isEmpty());
    }

    @Test
    public void execute_emptyList_displaysSharedTrashcan() {
        handler.execute(new ParsedCommand("trashcan", "list", ""));
        assertEquals(List.of("Trashcan is empty."), ui.messages);
    }

    @Test
    public void execute_invalidArgumentsAndMissingProject_leavesErrorReportingToCaller() {
        assertThrows(CommandException.class,
                () -> handler.execute(new ParsedCommand("trashcan", "restore", "")));
        assertThrows(CommandException.class,
                () -> handler.execute(new ParsedCommand("trashcan", "restore", "P001 P002")));
        assertThrows(CommandException.class,
                () -> handler.execute(new ParsedCommand("trashcan", "list", "extra")));
        assertThrows(CommandException.class,
                () -> handler.execute(new ParsedCommand("trashcan", "unknown", "")));
        assertThrows(ProjectException.class,
                () -> handler.execute(new ParsedCommand("trashcan", "restore", "P999")));
        assertTrue(ui.messages.isEmpty());
    }

    @Test
    public void execute_restore_usesSharedManagerAndPreservesResponse() {
        LocalDate date = LocalDate.of(2026, 11, 30);
        clients.addClient("Acme");
        projects.addProject("C001", "Website", date);
        projects.addTask("P001", "Wireframe", date);
        projects.addEvent("P001", "Meeting", date);
        projects.deleteProject("P001");

        handler.execute(new ParsedCommand("trashcan", "restore", "P001"));

        assertEquals(List.of("Restored project P001 \"Website\" with 1 task and 1 event."), ui.messages);
        assertEquals(2, projects.getProject("P001").getItemCount());
        assertTrue(trashcan.getDeletedProjects().isEmpty());
    }

    @Test
    public void showCommandUsage_displaysOnlySupportedActions() {
        handler.showCommandUsage("list");
        handler.showCommandUsage("restore");
        handler.showCommandUsage("empty");
        handler.showCommandUsage("unknown");

        assertEquals(List.of("Format: trashcan.list", "Format: trashcan.restore PROJECT_ID/CLIENT_ID",
                "Format: trashcan.empty"), ui.messages);
    }

    /** Captures delegated responses without printing to the console. */
    private static class RecordingUi extends Ui {
        private final List<String> messages = new ArrayList<>();

        @Override
        public void showMessage(String message) {
            messages.add(message);
        }
    }
}
