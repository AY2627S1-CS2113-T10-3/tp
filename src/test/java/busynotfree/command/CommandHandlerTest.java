package busynotfree.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import busynotfree.exception.BusyNotFreeException;
import busynotfree.exception.CommandException;
import busynotfree.exception.EventException;
import busynotfree.exception.ProjectException;
import busynotfree.exception.TaskException;
import busynotfree.model.PlaceholderClientLookup;
import busynotfree.model.Project;
import busynotfree.model.ProjectManager;
import busynotfree.ui.Ui;

/**
 * Tests creation-command routing, resulting model changes and error reporting.
 */
class CommandHandlerTest {
    private static final String PROJECT_FORMAT =
            "Format: project.add client/CLIENT_ID name/PROJECT_NAME due/YYYY-MM-DD";
    private static final String TASK_FORMAT = "Format: task.add project/PROJECT_ID name/DESCRIPTION date/YYYY-MM-DD";
    private static final String EVENT_FORMAT = "Format: event.add project/PROJECT_ID name/DESCRIPTION date/YYYY-MM-DD";

    private final RecordingUi ui = new RecordingUi();
    private final ProjectManager projectManager = new ProjectManager(new PlaceholderClientLookup());
    private final CommandHandler handler = new CommandHandler(new CommandParser(), ui, projectManager);

    @Test
    public void execute_exit_requestsShutdown() {
        assertTrue(handler.execute("  exit\t"));

        assertTrue(ui.messages.isEmpty());
        assertTrue(ui.errors.isEmpty());
        assertEquals(2, ui.separatorCount);
    }

    @Test
    public void execute_exitWithArguments_reportsError() {
        assertFalse(handler.execute("exit unexpected"));

        assertEquals(List.of("Use target.action, such as project.add."), ui.errors);
    }

    @Test
    public void execute_projectAdd_createsProject() {
        boolean shouldExit = handler.execute("project.add client/C001 name/Website redesign due/2026-11-30");

        assertFalse(shouldExit);
        assertEquals(List.of("Created project P001 for client C001."), ui.messages);
        Project project = projectManager.getProject("P001");
        assertEquals("Website redesign", project.getName());
        assertEquals(LocalDate.of(2026, 11, 30), project.getDeadline());
        assertTrue(ui.errors.isEmpty());
        assertEquals(2, ui.separatorCount);
    }

    @Test
    public void execute_taskAdd_attachesTaskToProject() {
        projectManager.addProject("C001", "Website redesign", LocalDate.of(2026, 11, 30));
        assertFalse(handler.execute("task.add project/P001 name/Create wireframe date/2026-10-15"));

        assertEquals(List.of("Added task T001 to project P001."), ui.messages);
        assertEquals("Create wireframe", projectManager.getProject("P001").getTasks().getFirst().getDescription());
        assertEquals(LocalDate.of(2026, 10, 15), projectManager.getProject("P001").getTasks().getFirst().getDate());
        assertTrue(ui.errors.isEmpty());
    }

    @Test
    public void execute_eventAdd_attachesEventToProject() {
        projectManager.addProject("C001", "Website redesign", LocalDate.of(2026, 11, 30));
        assertFalse(handler.execute("event.add project/P001 name/Stakeholder meeting date/2026-10-20"));

        assertEquals(List.of("Added event E001 to project P001."), ui.messages);
        assertEquals("Stakeholder meeting", projectManager.getProject("P001").getEvents().getFirst().getDescription());
        assertEquals(LocalDate.of(2026, 10, 20), projectManager.getProject("P001").getEvents().getFirst().getDate());
        assertTrue(ui.errors.isEmpty());
    }

    @Test
    public void execute_projectAddWithSlashInName_createsProject() {
        assertFalse(handler.execute("project.add client/C001 name/Create UI/UX redesign due/2026-11-30"));

        assertEquals(List.of("Created project P001 for client C001."), ui.messages);
        assertEquals("Create UI/UX redesign", projectManager.getProject("P001").getName());
        assertTrue(ui.errors.isEmpty());
    }

    @Test
    public void execute_taskAddWithSlashInDescription_createsTask() {
        Project project = projectManager.addProject("C001", "Website", LocalDate.of(2026, 11, 30));

        assertFalse(handler.execute("task.add project/P001 name/Create UI/UX mockup date/2026-10-15"));

        assertEquals(List.of("Added task T001 to project P001."), ui.messages);
        assertEquals("Create UI/UX mockup", project.getTasks().getFirst().getDescription());
        assertTrue(ui.errors.isEmpty());
    }

    @Test
    public void execute_eventAddWithPathInDescription_createsEvent() {
        Project project = projectManager.addProject("C001", "Website", LocalDate.of(2026, 11, 30));

        assertFalse(handler.execute("event.add project/P001 date/2026-10-20 name/Review src/main changes"));

        assertEquals(List.of("Added event E001 to project P001."), ui.messages);
        assertEquals("Review src/main changes", project.getEvents().getFirst().getDescription());
        assertTrue(ui.errors.isEmpty());
    }

    @Test
    public void execute_unsupportedTarget_reportsError() {
        assertFalse(handler.execute("client.add name/Acme"));

        assertEquals(List.of("Unsupported target: client."), ui.errors);
        assertTrue(ui.messages.isEmpty());
    }

    @Test
    public void execute_unsupportedAction_reportsError() {
        assertFalse(handler.execute("project.delete P001"));

        assertEquals(List.of("Unsupported action: delete."), ui.errors);
        assertTrue(ui.messages.isEmpty());
    }

    @Test
    public void execute_malformedWord_reportsError() {
        assertFalse(handler.execute("task.add.more project/P001"));

        assertEquals(List.of("Use target.action, such as project.add."), ui.errors);
        assertTrue(ui.messages.isEmpty());
        assertEquals(2, ui.separatorCount);
    }

    @Test
    public void execute_blankInput_reportsError() {
        assertFalse(handler.execute(" \t "));

        assertEquals(List.of("Use target.action, such as project.add."), ui.errors);
        assertTrue(ui.messages.isEmpty());
    }

    @Test
    public void execute_missingArguments_reportsErrorWithoutCreation() {
        assertFalse(handler.execute("project.add"));

        assertEquals(List.of("Missing required prefix: client/."), ui.errors);
        assertEquals(List.of(PROJECT_FORMAT), ui.messages);
        assertTrue(projectManager.getProjects().isEmpty());
    }

    @Test
    public void execute_invalidDate_reportsErrorWithoutConsumingId() {
        assertFalse(handler.execute("project.add client/C001 name/Website due/2026-02-30"));
        assertTrue(projectManager.getProjects().isEmpty());
        assertEquals(List.of(PROJECT_FORMAT), ui.messages);
        assertEquals(List.of("Use a valid date in YYYY-MM-DD format."), ui.errors);

        handler.execute("project.add client/C001 name/Website due/2026-11-30");
        assertEquals(List.of(PROJECT_FORMAT, "Created project P001 for client C001."), ui.messages);
    }

    @Test
    public void execute_missingProject_reportsErrorForTaskAndEvent() {
        assertFalse(handler.execute("task.add project/P999 name/Wireframe date/2026-10-15"));
        assertFalse(handler.execute("event.add project/P999 name/Meeting date/2026-10-20"));

        assertEquals(List.of("Project P999 does not exist.", "Project P999 does not exist."), ui.errors);
        assertEquals(List.of(TASK_FORMAT, EVENT_FORMAT), ui.messages);
        assertTrue(projectManager.getProjects().isEmpty());
    }

    @Test
    public void execute_invalidItemDates_doesNotMutateProject() {
        Project project = projectManager.addProject("C001", "Website", LocalDate.of(2026, 11, 30));
        handler.execute("task.add project/P001 name/Wireframe date/2026-02-29");
        handler.execute("event.add project/P001 name/Meeting date/2026-1-20");

        assertEquals(2, ui.errors.size());
        assertEquals(0, project.getItemCount());
        assertEquals(List.of(TASK_FORMAT, EVENT_FORMAT), ui.messages);
        handler.execute("task.add project/P001 name/Wireframe date/2026-10-15");
        handler.execute("event.add project/P001 name/Meeting date/2026-10-20");
        assertEquals(List.of(TASK_FORMAT, EVENT_FORMAT,
                "Added task T001 to project P001.", "Added event E001 to project P001."), ui.messages);
    }

    @Test
    public void execute_repeatedPrefix_reportsErrorWithoutCreation() {
        handler.execute("project.add client/C001 name/Website name/Other due/2026-11-30");

        assertEquals(List.of("Repeated prefix: name/."), ui.errors);
        assertEquals(List.of(PROJECT_FORMAT), ui.messages);
        assertTrue(projectManager.getProjects().isEmpty());
    }

    @Test
    public void execute_blankName_reportsErrorWithoutCreation() {
        handler.execute("project.add client/C001 name/ due/2026-11-30");

        assertEquals(List.of("A value is required after name/."), ui.errors);
        assertEquals(List.of(PROJECT_FORMAT), ui.messages);
        assertTrue(projectManager.getProjects().isEmpty());
    }

    @Test
    public void execute_reorderedPrefixes_createsProject() {
        handler.execute("project.add due/2026-11-30 name/Website redesign client/C001");

        assertEquals(List.of("Created project P001 for client C001."), ui.messages);
        assertEquals("Website redesign", projectManager.getProject("P001").getName());
        assertTrue(ui.errors.isEmpty());
    }

    @Test
    public void execute_realLookupRejectsClient_reportsError() {
        ProjectManager rejectingManager = new ProjectManager(clientId -> false);
        CommandHandler rejectingHandler = new CommandHandler(new CommandParser(), ui, rejectingManager);

        rejectingHandler.execute("project.add client/C999 name/Website due/2026-11-30");

        assertEquals(List.of("Client C999 does not exist."), ui.errors);
        assertTrue(rejectingManager.getProjects().isEmpty());
        assertEquals(List.of(PROJECT_FORMAT), ui.messages);
    }

    @Test
    public void execute_missingTaskDate_showsErrorAndTaskFormat() {
        assertFalse(handler.execute("task.add project/P001 name/Create wireframe"));

        assertEquals(List.of("Missing required prefix: date/."), ui.errors);
        assertEquals(List.of(TASK_FORMAT), ui.messages);
        assertTrue(projectManager.getProjects().isEmpty());
    }

    @Test
    public void execute_missingEventArguments_showsEventFormat() {
        assertFalse(handler.execute("event.add"));

        assertEquals(List.of("Missing required prefix: project/."), ui.errors);
        assertEquals(List.of(EVENT_FORMAT), ui.messages);
    }

    @Test
    public void execute_applicationExceptionSubtypes_reportsWithoutShutdown() {
        List<BusyNotFreeException> exceptions = List.of(new CommandException("Command failure."),
                new ProjectException("Project failure."), new TaskException("Task failure."),
                new EventException("Event failure."));
        for (BusyNotFreeException exception : exceptions) {
            CommandHandler failingHandler = new CommandHandler(new FailingParser(exception), ui, projectManager);

            assertFalse(failingHandler.execute("project.add"));
        }

        assertEquals(List.of("Command failure.", "Project failure.", "Task failure.", "Event failure."), ui.errors);
        assertTrue(ui.messages.isEmpty());
    }

    @Test
    public void execute_unrelatedProgrammingError_doesNotHideFailure() {
        IllegalArgumentException programmingError = new IllegalArgumentException("Unexpected parser failure.");
        CommandHandler failingHandler = new CommandHandler(new FailingParser(programmingError), ui, projectManager);

        assertEquals(programmingError, assertThrows(IllegalArgumentException.class,
                () -> failingHandler.execute("project.add")));
        assertTrue(ui.errors.isEmpty());
        assertTrue(ui.messages.isEmpty());
    }

    /** Simulates parser failures to verify the CLI's exception boundary. */
    private static class FailingParser extends CommandParser {
        private final RuntimeException exception;

        private FailingParser(RuntimeException exception) {
            this.exception = exception;
        }

        @Override
        public ParsedCommand parse(String userInput) {
            throw exception;
        }
    }

    /** Captures handler output without writing to the console. */
    private static class RecordingUi extends Ui {
        private final List<String> messages = new ArrayList<>();
        private final List<String> errors = new ArrayList<>();
        private int separatorCount;

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
            separatorCount++;
        }
    }
}
