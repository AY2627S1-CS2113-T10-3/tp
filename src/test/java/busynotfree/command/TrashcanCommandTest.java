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

import busynotfree.exception.ProjectException;
import busynotfree.model.Client;
import busynotfree.model.ClientManager;
import busynotfree.model.Project;
import busynotfree.model.ProjectManager;
import busynotfree.model.ProjectStatus;
import busynotfree.model.Task;
import busynotfree.ui.Ui;

/** Exercises trashcan commands with the same linked managers used by the application. */
class TrashcanCommandTest {
    private final ClientManager clients = new ClientManager();
    private final ProjectManager projects = new ProjectManager(clients);
    private final RecordingUi ui = new RecordingUi();
    private final CommandHandler handler = new CommandHandler(new CommandParser(), ui, projects, clients);

    /** Creates a project with completed and incomplete tasks plus an event. */
    private Project addProject() {
        clients.addClient("Acme");
        Project project = projects.addProject("C001", "Website", LocalDate.of(2026, 11, 30));
        projects.addTask("P001", "Wireframe", LocalDate.of(2026, 11, 1));
        projects.markTaskComplete("T001");
        projects.addTask("P001", "Review", LocalDate.of(2026, 11, 2));
        projects.addEvent("P001", "Meeting", LocalDate.of(2026, 11, 3));
        project.changeStatus(ProjectStatus.PAUSED);
        return project;
    }

    @Test
    public void execute_listEmptyTrashcan_reportsEmpty() {
        handler.execute("trashcan.list");

        assertEquals(List.of("Trashcan is empty."), ui.messages);
        assertTrue(ui.errors.isEmpty());
    }

    @Test
    public void execute_list_showsDeletedProjectsActivitiesAndClientNotes() {
        Project original = addProject();
        Client client = clients.getClient("C001");
        clients.addNote("C001", "Call on Friday");
        clients.addNote("C001", "Send proposal");
        handler.execute("project.delete P001");
        assertTrue(projects.getDeletedProjects().isEmpty());
        handler.execute("yes");
        handler.execute("client.delete C001");
        handler.execute("yes");

        // Old references cannot change the captured trash entries.
        original.changeStatus(ProjectStatus.COMPLETED);
        client.setName("Changed after deletion");
        ui.messages.clear();
        handler.execute("trashcan.list");

        assertTrue(ui.messages.contains("Project: P001 \"Website\" | Client: C001"
                + " | Status: PAUSED | Due: 2026-11-30"));
        assertTrue(ui.messages.stream().anyMatch(message -> message.contains("T001")
                && message.contains("Wireframe") && message.contains("COMPLETED")));
        assertTrue(ui.messages.stream().anyMatch(message -> message.contains("T002")
                && message.contains("Review")));
        assertTrue(ui.messages.stream().anyMatch(message -> message.contains("E001")
                && message.contains("Meeting")));
        assertTrue(ui.messages.contains("C001 | Acme"));
        assertTrue(ui.messages.contains("Note 1: Call on Friday"));
        assertTrue(ui.messages.contains("Note 2: Send proposal"));
        assertTrue(projects.getProjects().isEmpty());
        assertTrue(clients.getClients().isEmpty());
        assertTrue(ui.errors.isEmpty());
    }

    /** Invalid restore requests leave both active data and the trashcan unchanged. */
    @TestFactory
    public List<DynamicTest> execute_restoreInvalidProjectId_preservesTrashcan() {
        List<String> inputs = List.of("trashcan.restore", "trashcan.restore   ",
                "trashcan.restore invalid", "trashcan.restore P999", "trashcan.restore p001",
                "trashcan.restore C001", "trashcan.restore P001 P002");
        List<String> errors = List.of("A project ID must not be blank.", "A project ID must not be blank.",
                "Project invalid is not in the trashcan.", "Project P999 is not in the trashcan.",
                "Project p001 is not in the trashcan.", "Project C001 is not in the trashcan.",
                "Provide exactly one project ID.");
        List<DynamicTest> tests = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            String input = inputs.get(i);
            String error = errors.get(i);
            tests.add(dynamicTest(input, () -> {
                TrashcanCommandTest fixture = new TrashcanCommandTest();
                fixture.addProject();
                fixture.projects.deleteProject("P001");

                assertFalse(fixture.handler.execute(input));

                assertEquals(List.of(error), fixture.ui.errors);
                assertEquals(List.of("Format: trashcan.restore PROJECT_ID"), fixture.ui.messages);
                assertTrue(fixture.projects.getProjects().isEmpty());
                assertEquals(3, fixture.projects.getDeletedProjects().getFirst().getItemCount());
                assertFalse(fixture.handler.isAwaitingDeletionConfirmation());
            }));
        }
        return tests;
    }

    @Test
    public void execute_restore_preservesIdsDetailsAndCountersAndRemovesTrashEntry() {
        addProject();
        projects.deleteProject("P001");
        projects.addProject("C001", "Other", LocalDate.of(2026, 12, 1));

        handler.execute("  trashcan.restore  P001  ");

        Project restored = projects.getProject("P001");
        assertEquals("Website", restored.getName());
        assertEquals("C001", restored.getClientId());
        assertEquals(ProjectStatus.PAUSED, restored.getStatus());
        assertEquals(LocalDate.of(2026, 11, 30), restored.getDeadline());
        assertEquals(List.of("T001", "T002"), restored.getTasks().stream().map(task -> task.getId()).toList());
        assertTrue(projects.getTask("T001").isCompleted());
        assertFalse(projects.getTask("T002").isCompleted());
        assertEquals(LocalDate.of(2026, 11, 1), projects.getTask("T001").getDate());
        assertEquals("P001", projects.getEvent("E001").getProject().getProjectId());
        assertEquals(2, projects.getProjects().size());
        assertTrue(projects.getDeletedProjects().isEmpty());
        assertEquals(List.of("Restored project P001 \"Website\" with 2 tasks and 1 event."), ui.messages);
        assertTrue(ui.errors.isEmpty());
        assertEquals("P003", projects.addProject("C001", "Next", LocalDate.of(2027, 1, 1)).getProjectId());
        assertEquals("T003", projects.addTask("P001", "Next task", LocalDate.of(2026, 12, 1)).getId());
        assertEquals("E002", projects.addEvent("P001", "Next event", LocalDate.of(2026, 12, 2)).getId());

        handler.execute("trashcan.restore P001");
        assertEquals("Project P001 is not in the trashcan.", ui.errors.getLast());
        assertEquals(3, restored.getTasks().size());
    }

    @Test
    public void execute_restoreWithDeletedClient_rejectsWithoutLosingTrashEntry() {
        addProject();
        projects.deleteProject("P001");
        clients.deleteClient("C001", projects);

        handler.execute("trashcan.restore P001");

        assertEquals(List.of("Cannot restore project P001: client C001 is no longer active."), ui.errors);
        assertTrue(projects.getProjects().isEmpty());
        assertEquals(1, projects.getDeletedProjects().size());
        assertEquals(1, clients.getDeletedClients().size());
    }

    @Test
    public void execute_restoreActiveProject_rejectsWithoutChangingIt() {
        Project project = addProject();
        handler.execute("trashcan.restore P001");

        assertEquals(List.of("Project P001 is not in the trashcan."), ui.errors);
        assertEquals(List.of(project), projects.getProjects());
        assertEquals(3, project.getItemCount());
    }

    @Test
    public void execute_cancelDeletion_doesNotCreateTrashEntry() {
        addProject();
        handler.execute("project.delete P001");
        handler.execute("no");
        assertTrue(projects.getDeletedProjects().isEmpty());
        assertEquals(3, projects.getProject("P001").getItemCount());
    }

    @Test
    public void execute_listWithArgumentsOrUnknownAction_reportsUsageOrError() {
        handler.execute("trashcan.list extra");
        handler.execute("trashcan.unknown");

        assertEquals(List.of("Use trashcan.list without arguments.", "Unsupported action: unknown."), ui.errors);
        assertEquals(List.of("Format: trashcan.list"), ui.messages);
    }

    @Test
    public void restoreProject_blankIdAndRepeatedDelete_preservesLatestSnapshot() {
        addProject();
        assertThrows(ProjectException.class, () -> projects.restoreProject(null));
        projects.deleteProject("P001");
        projects.restoreProject("P001");
        projects.editTask("T001", "Revised wireframe", LocalDate.of(2026, 11, 5));
        projects.deleteProject("P001");

        assertEquals(1, projects.getDeletedProjects().size());
        assertEquals("Revised wireframe", projects.restoreProject("P001").getTasks().getFirst().getDescription());
    }

    @Test
    public void restoreProject_conflictingTaskId_keepsBothCollectionsUnchanged() {
        addProject();
        projects.deleteProject("P001");
        Project other = projects.addProject("C001", "Other", LocalDate.of(2026, 12, 1));
        // Simulates inconsistent data supplied through the project's lower-level API.
        other.addTask(new Task("T001", other, "Conflicting task", LocalDate.of(2026, 11, 1)));

        assertThrows(ProjectException.class, () -> projects.restoreProject("P001"));

        assertEquals(List.of(other), projects.getProjects());
        assertEquals(1, projects.getDeletedProjects().size());
        assertEquals(3, projects.getDeletedProjects().getFirst().getItemCount());
        assertEquals("Conflicting task", projects.getTask("T001").getDescription());
    }

    @Test
    public void restoreProject_emptyProjectAndSnapshotCopies_preservesStoredSnapshot() {
        clients.addClient("Acme");
        projects.addProject("C001", "Empty", LocalDate.of(2026, 11, 30));
        projects.deleteProject("P001");
        projects.getDeletedProjects().getFirst().changeStatus(ProjectStatus.COMPLETED);

        handler.execute("trashcan.restore P001");

        assertEquals(ProjectStatus.ACTIVE, projects.getProject("P001").getStatus());
        assertEquals(0, projects.getProject("P001").getItemCount());
        assertEquals(List.of("Restored project P001 \"Empty\" with 0 tasks and 0 events."), ui.messages);
        assertTrue(projects.getDeletedProjects().isEmpty());
    }

    /** Captures responses without decorative separators. */
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
            // Tests assert command responses rather than separators.
        }
    }
}
