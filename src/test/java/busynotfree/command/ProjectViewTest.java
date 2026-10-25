package busynotfree.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import busynotfree.model.ClientManager;
import busynotfree.model.ProjectManager;
import busynotfree.model.ProjectStatus;
import busynotfree.ui.Ui;

/** Tests project summaries through the public command handler. */
class ProjectViewTest {
    private final RecordingUi ui = new RecordingUi();
    private final ClientManager clients = new ClientManager();
    private final ProjectManager projects = new ProjectManager(clients);
    private final CommandHandler handler = new CommandHandler(new CommandParser(), ui, projects, clients);

    @Test
    public void execute_projectView_displaysClientStatusAndSeparateActivities() {
        clients.addClient("Acme Studio");
        projects.addProject("C001", "Website redesign", LocalDate.of(2026, 11, 30));
        projects.changeProjectStatus("P001", ProjectStatus.PAUSED);
        projects.addTask("P001", "Create homepage wireframe", LocalDate.of(2026, 10, 15));
        projects.addEvent("P001", "Stakeholder meeting", LocalDate.of(2026, 10, 20));

        assertFalse(handler.execute("project.view P001"));

        assertEquals(List.of("Project: P001 - Website redesign", "Client: C001 - Acme Studio",
                "Status: PAUSED", "Deadline: 2026-11-30", "Tasks:",
                "[T001] Create homepage wireframe - 2026-10-15 [In Progress]", "Events:",
                "[E001] Stakeholder meeting - 2026-10-20"), ui.messages);
        assertTrue(ui.errors.isEmpty());
    }

    @Test
    public void execute_projectViewWithNoActivities_displaysEmptySections() {
        clients.addClient("Acme Studio");
        projects.addProject("C001", "Logo design", LocalDate.of(2026, 12, 15));

        handler.execute("project.view P001");

        assertEquals(List.of("Project: P001 - Logo design", "Client: C001 - Acme Studio",
                "Status: ACTIVE", "Deadline: 2026-12-15", "Tasks:", "No tasks found.",
                "Events:", "No events found."), ui.messages);
    }

    @Test
    public void execute_projectViewAfterTaskMark_showsCompletedTask() {
        clients.addClient("Acme Studio");
        projects.addProject("C001", "Website", LocalDate.of(2026, 11, 30));
        projects.addTask("P001", "Wireframe", LocalDate.of(2026, 10, 15));
        handler.execute("task.mark T001");
        ui.messages.clear();

        handler.execute("project.view P001");

        assertTrue(ui.messages.contains("[T001] Wireframe - 2026-10-15 [COMPLETED]"));
        assertTrue(ui.errors.isEmpty());
    }

    @Test
    public void execute_projectViewAfterTaskUnmark_showsInProgressLabel() {
        clients.addClient("Acme Studio");
        projects.addProject("C001", "Website", LocalDate.of(2026, 11, 30));
        projects.addTask("P001", "Wireframe", LocalDate.of(2026, 10, 15));
        handler.execute("task.mark T001");
        handler.execute("task.unmark T001");
        ui.messages.clear();

        handler.execute("project.view P001");

        assertTrue(ui.messages.contains("[T001] Wireframe - 2026-10-15 [In Progress]"));
        assertFalse(ui.messages.stream().anyMatch(line -> line.contains("[COMPLETED]")));
        assertTrue(ui.errors.isEmpty());
    }

    @Test
    public void execute_projectViewAfterEdits_showsUpdatedActivities() {
        clients.addClient("Acme Studio");
        projects.addProject("C001", "Website", LocalDate.of(2026, 11, 30));
        projects.addTask("P001", "Wireframe", LocalDate.of(2026, 10, 15));
        projects.addEvent("P001", "Meeting", LocalDate.of(2026, 10, 20));
        handler.execute("task.mark T001");
        handler.execute("task.edit T001 name/Revised wireframe date/2026-12-01");
        handler.execute("event.edit E001 date/2026-12-02 name/Rescheduled meeting");
        ui.messages.clear();

        handler.execute("project.view P001");

        assertTrue(ui.messages.contains("[T001] Revised wireframe - 2026-12-01 [COMPLETED]"));
        assertTrue(ui.messages.contains("[E001] Rescheduled meeting - 2026-12-02"));
        assertFalse(ui.messages.stream().anyMatch(line -> line.contains("] Wireframe") || line.contains("] Meeting")));
        assertTrue(ui.errors.isEmpty());
    }

    @Test
    public void execute_projectViewAllAndNoId_displaySameSummaries() {
        clients.addClient("Acme Studio");
        projects.addProject("C001", "Website", LocalDate.of(2026, 11, 30));
        projects.addProject("C001", "Logo", LocalDate.of(2026, 12, 15));

        handler.execute("project.view all");
        List<String> allMessages = List.copyOf(ui.messages);
        ui.messages.clear();
        handler.execute("project.view");

        assertEquals(allMessages, ui.messages);
        assertEquals(1, ui.messages.stream().filter(String::isEmpty).count());
        assertTrue(ui.messages.contains("Project: P001 - Website"));
        assertTrue(ui.messages.contains("Project: P002 - Logo"));
    }

    @Test
    public void execute_projectViewWithNoProjects_displaysEmptyMessage() {
        handler.execute("project.view");
        handler.execute("project.view all");

        assertEquals(List.of("No projects found.", "No projects found."), ui.messages);
    }

    @Test
    public void execute_projectViewWithMissingOrMultipleIds_reportsError() {
        handler.execute("project.view P67");
        handler.execute("project.view P001 P002");

        assertEquals(List.of("Project P67 was not found.",
                "Provide exactly one project ID, or use all."), ui.errors);
        assertEquals(List.of("Format: project.view [PROJECT_ID|all]",
                "Format: project.view [PROJECT_ID|all]"), ui.messages);
    }

    /** Captures summary lines and errors without console output. */
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
            // Decorative separators are not part of summary content.
        }
    }
}
