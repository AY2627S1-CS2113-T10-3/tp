package busynotfree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

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
            new CommandHandler(new CommandParser(), ui, projectManager), () -> {
            });

    @Test
    public void run_creationCommands_displaysEachResponse() {
        runWithInput("project.add client/C001 name/Website due/2026-11-30\n"
                + "task.add project/P001 name/Wireframe date/2026-10-15\n"
                + "event.add project/P001 name/Meeting date/2026-10-20\nexit\n");

        assertTrue(ui.messages.contains("Created project P001 \"Website\" for client C001."));
        assertTrue(ui.messages.contains("Added task T001 \"Wireframe\" to project P001 \"Website\"."));
        assertTrue(ui.messages.contains("Added event E001 \"Meeting\" to project P001 \"Website\"."));
        assertEquals(2, projectManager.getProject("P001").getItemCount());
        assertTrue(ui.errors.isEmpty());
        assertEquals(4, ui.messages.stream().filter("Enter a command:"::equals).count());
    }

    @Test
    public void run_invalidInput_continuesReading() {
        runWithInput("\ninvalid\nproject.add\nproject.add client/C001 name/Website due/2026-11-30\nexit\n");

        assertEquals(3, ui.errors.size());
        assertTrue(ui.messages.contains("Created project P001 \"Website\" for client C001."));
        assertEquals("Data saved. Goodbye!", ui.messages.getLast());
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
        assertTrue(ui.messages.contains("Added task T001 \"Wireframe\" to project P001 \"Website\"."));
        assertTrue(ui.messages.contains("Added event E001 \"Meeting\" to project P001 \"Website\"."));
        assertEquals(2, projectManager.getProject("P001").getItemCount());
        assertEquals("Data saved. Goodbye!", ui.messages.getLast());
    }

    @Test
    public void run_exit_stopsBeforeNextCommand() {
        runWithInput("exit\nproject.add\n");

        assertTrue(projectManager.getProjects().isEmpty());
        assertEquals("Data saved. Goodbye!", ui.messages.getLast());
        assertEquals(1, ui.messages.stream().filter("Data saved. Goodbye!"::equals).count());
    }

    @Test
    public void run_failedSave_keepsRunningUntilSaveSucceeds() {
        AtomicInteger saveAttempts = new AtomicInteger();
        BusyNotFree application = new BusyNotFree(ui,
                new CommandHandler(new CommandParser(), ui, projectManager), () -> {
                    if (saveAttempts.incrementAndGet() == 1) {
                        throw new IOException("Disk unavailable");
                    }
                });

        try (Scanner scanner = new Scanner("exit now\nproject.add client/C001 name/Website "
                + "due/2026-11-30\nexit\n")) {
            application.run(scanner);
        }

        assertEquals(2, saveAttempts.get());
        assertEquals(List.of("Critical ERROR!!! Unable to save data: Disk unavailable"), ui.errors);
        assertEquals("Website", projectManager.getProject("P001").getName());
        assertEquals(1, ui.messages.stream().filter("Data saved. Goodbye!"::equals).count());
    }

    @Test
    public void run_endOfInput_exitsCleanly() {
        runWithInput("project.add client/C001 name/Website due/2026-11-30\n");

        assertTrue(ui.messages.contains("Created project P001 \"Website\" for client C001."));
        assertEquals("Data saved. Goodbye!", ui.messages.getLast());
        assertTrue(ui.errors.isEmpty());
    }

    @Test
    public void run_emptyInput_displaysWelcomeAndGoodbye() {
        runWithInput("");

        assertTrue(ui.messages.contains("Welcome to BusyNotFree!"));
        assertTrue(ui.messages.stream().anyMatch(message -> message.contains("client.add, client.note")));
        assertTrue(ui.messages.stream().anyMatch(message -> message.contains("project.view")));
        assertEquals("Data saved. Goodbye!", ui.messages.getLast());
        assertTrue(ui.errors.isEmpty());
    }

    @Test
    public void run_mixedCommandsWithWindowsLineEndings_preservesDataAfterErrors() {
        List<String> commands = List.of(
                "project.add client/C001 name/Create UI/UX redesign due/2026-11-30",
                "task.add project/P001 name/First task date/2026-10-15",
                "event.add project/P001 name/First event date/2026-10-20",
                "project.add client/C001 name/Invalid due/2026-02-30",
                "task.add project/P001 name/Invalid date/2026-11-31",
                "event.add project/P999 name/Invalid date/2026-10-20",
                "task.add date/2026-10-15 name/Review src/main changes project/P001",
                "event.add project/P001 date/2026-10-20 name/Review UI/UX mockup",
                "exit");

        runWithInput(String.join("\r\n", commands) + "\r\n");

        assertEquals(List.of("Use a valid date in YYYY-MM-DD format.",
                "Use a valid date in YYYY-MM-DD format.",
                "Project P999 does not exist. Use project.list to see project IDs."), ui.errors);
        assertEquals(1, projectManager.getProjects().size());
        assertEquals("Create UI/UX redesign", projectManager.getProject("P001").getName());
        assertEquals(List.of("T001", "T002"), projectManager.getProject("P001").getTasks().stream()
                .map(task -> task.getId()).toList());
        assertEquals(List.of("E001", "E002"), projectManager.getProject("P001").getEvents().stream()
                .map(event -> event.getId()).toList());
        assertEquals("Review src/main changes",
                projectManager.getProject("P001").getTasks().getLast().getDescription());
        assertEquals("Review UI/UX mockup", projectManager.getProject("P001").getEvents().getLast().getDescription());
        assertEquals(commands.size(), ui.messages.stream().filter("Enter a command:"::equals).count());
        assertEquals("Data saved. Goodbye!", ui.messages.getLast());
    }

    @Test
    public void run_invalidCommandAtEndOfInput_reportsOnceAndExitsCleanly() {
        runWithInput("event.add project/P001 name/Meeting");

        assertEquals(List.of("Missing required prefix: date/."), ui.errors);
        assertTrue(projectManager.getProjects().isEmpty());
        assertEquals(2, ui.messages.stream().filter("Enter a command:"::equals).count());
        assertEquals(1, ui.messages.stream().filter("Data saved. Goodbye!"::equals).count());
        assertEquals("Data saved. Goodbye!", ui.messages.getLast());
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
