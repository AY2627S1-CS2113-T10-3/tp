package busynotfree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import busynotfree.command.CommandHandler;
import busynotfree.command.CommandParser;
import busynotfree.model.PlaceholderClientLookup;
import busynotfree.model.ProjectManager;
import busynotfree.ui.Ui;

/**
 * Tests the CLI loop using simulated input and captured UI messages.
 */
class BusyNotFreeTest {
    private final RecordingUi ui = new RecordingUi();
    private final ProjectManager projectManager = new ProjectManager(new PlaceholderClientLookup());
    private final BusyNotFree busyNotFree = new BusyNotFree(ui,
            new CommandHandler(new CommandParser(), ui, projectManager));

    @Test
    public void run_creationCommands_displaysEachResponse() {
        runWithInput("project.add client/C001 name/Website due/2026-11-30\n"
                + "task.add project/P001 name/Wireframe date/2026-10-15\n"
                + "event.add project/P001 name/Meeting date/2026-10-20\nexit\n");

        assertTrue(ui.messages.contains("Created project P001 for client C001."));
        assertTrue(ui.messages.contains("Added task T001 to project P001."));
        assertTrue(ui.messages.contains("Added event E001 to project P001."));
        assertEquals(2, projectManager.getProject("P001").getItemCount());
        assertTrue(ui.errors.isEmpty());
        assertEquals(4, ui.messages.stream().filter("Enter a command:"::equals).count());
    }

    @Test
    public void run_invalidInput_continuesReading() {
        runWithInput("\ninvalid\nproject.add\nproject.add client/C001 name/Website due/2026-11-30\nexit\n");

        assertEquals(3, ui.errors.size());
        assertTrue(ui.messages.contains("Created project P001 for client C001."));
        assertEquals("Goodbye!", ui.messages.getLast());
    }

    @Test
    public void run_invalidAddCommands_showsHintsAndAcceptsCorrections() {
        runWithInput("project.add client/C001 name/Website due/2026-11-30\n"
                + "task.add project/P001 name/Wireframe\n"
                + "task.add project/P001 name/Wireframe date/2026-10-15\n"
                + "event.add project/P001 name/Meeting date/2026-02-30\n"
                + "event.add project/P001 name/Meeting date/2026-10-20\nexit\n");

        assertEquals(List.of("Missing required prefix: date/.",
                "Use a valid date in YYYY-MM-DD format."), ui.errors);
        String taskFormat = "Format: task.add project/PROJECT_ID name/DESCRIPTION date/YYYY-MM-DD";
        String eventFormat = "Format: event.add project/PROJECT_ID name/DESCRIPTION date/YYYY-MM-DD";
        assertTrue(ui.messages.contains(taskFormat));
        assertTrue(ui.messages.contains(eventFormat));
        assertEquals("Enter a command:", ui.messages.get(ui.messages.indexOf(taskFormat) + 1));
        assertEquals("Enter a command:", ui.messages.get(ui.messages.indexOf(eventFormat) + 1));
        assertTrue(ui.messages.contains("Added task T001 to project P001."));
        assertTrue(ui.messages.contains("Added event E001 to project P001."));
        assertEquals(2, projectManager.getProject("P001").getItemCount());
        assertEquals("Goodbye!", ui.messages.getLast());
    }

    @Test
    public void run_exit_stopsBeforeNextCommand() {
        runWithInput("exit\nproject.add\n");

        assertTrue(projectManager.getProjects().isEmpty());
        assertEquals("Goodbye!", ui.messages.getLast());
        assertEquals(1, ui.messages.stream().filter("Goodbye!"::equals).count());
    }

    @Test
    public void run_endOfInput_exitsCleanly() {
        runWithInput("project.add client/C001 name/Website due/2026-11-30\n");

        assertTrue(ui.messages.contains("Created project P001 for client C001."));
        assertEquals("Goodbye!", ui.messages.getLast());
        assertTrue(ui.errors.isEmpty());
    }

    @Test
    public void run_emptyInput_displaysWelcomeAndGoodbye() {
        runWithInput("");

        assertTrue(ui.messages.contains("Welcome to BusyNotFree!"));
        assertTrue(ui.messages.contains("Client lookup is a placeholder: supplied client IDs are assumed to exist."));
        assertTrue(ui.messages.contains("Data is kept in memory only and is not saved on exit."));
        assertEquals("Goodbye!", ui.messages.getLast());
        assertTrue(ui.errors.isEmpty());
    }

    /** Supplies simulated input while closing only the test's scanner. */
    private void runWithInput(String input) {
        try (Scanner scanner = new Scanner(input)) {
            busyNotFree.run(scanner);
        }
    }

    /** Captures output without changing the process's standard streams. */
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
            // Separators are not needed when testing the CLI loop's messages.
        }
    }
}
