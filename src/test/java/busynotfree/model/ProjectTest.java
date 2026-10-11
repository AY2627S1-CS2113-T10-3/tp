package busynotfree.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import busynotfree.exception.EventException;
import busynotfree.exception.ProjectException;
import busynotfree.exception.TaskException;

/**
 * Tests project data, status and exclusive ownership of scheduled items.
 */
class ProjectTest {
    private final LocalDate deadline = LocalDate.of(2026, 11, 30);
    private final Project project = new Project("P001", "C001", "Website redesign", deadline);

    @Test
    public void constructor_validData_createsEmptyActiveProject() {
        assertEquals("P001", project.getProjectId());
        assertEquals("C001", project.getClientId());
        assertEquals("Website redesign", project.getName());
        assertEquals(deadline, project.getDeadline());
        assertEquals(ProjectStatus.ACTIVE, project.getStatus());
        assertTrue(project.getTasks().isEmpty());
        assertTrue(project.getEvents().isEmpty());
        assertEquals(0, project.getItemCount());
    }

    @Test
    public void constructor_extraWhitespace_trimsName() {
        Project paddedProject = new Project("P002", "C001", "  Logo  design  ", deadline);

        assertEquals("Logo  design", paddedProject.getName());
    }

    @Test
    public void constructor_missingProjectId_throwsException() {
        assertThrows(ProjectException.class, () -> new Project("", "C001", "Website", deadline));
        assertThrows(ProjectException.class, () -> new Project(null, "C001", "Website", deadline));
    }

    @Test
    public void constructor_missingClientId_throwsException() {
        assertThrows(ProjectException.class, () -> new Project("P002", "", "Website", deadline));
        assertThrows(ProjectException.class, () -> new Project("P002", null, "Website", deadline));
    }

    @Test
    public void constructor_missingName_throwsException() {
        assertThrows(ProjectException.class, () -> new Project("P002", "C001", " \t ", deadline));
        assertThrows(ProjectException.class, () -> new Project("P002", "C001", null, deadline));
    }

    @Test
    public void constructor_missingDeadline_throwsException() {
        assertThrows(ProjectException.class, () -> new Project("P002", "C001", "Website", null));
    }

    @Test
    public void addTask_validTask_addsToTaskCollection() {
        Task task = new Task("T001", project, "Create wireframe", LocalDate.of(2026, 10, 15));

        project.addTask(task);

        assertEquals(List.of(task), project.getTasks());
        assertTrue(project.getEvents().isEmpty());
        assertSame(project, task.getProject());
    }

    @Test
    public void addEvent_validEvent_addsToEventCollection() {
        Event event = new Event("E001", project, "Stakeholder meeting", LocalDate.of(2026, 10, 20));

        project.addEvent(event);

        assertEquals(List.of(event), project.getEvents());
        assertTrue(project.getTasks().isEmpty());
        assertSame(project, event.getProject());
    }

    @Test
    public void addTask_otherProject_rejectsWithoutMutation() {
        Project otherProject = new Project("P002", "C001", "Logo design", deadline);
        Task task = new Task("T001", otherProject, "Create wireframe", deadline);

        TaskException exception = assertThrows(TaskException.class,
                () -> project.addTask(task));

        assertEquals("The task belongs to another project.", exception.getMessage());
        assertEquals(0, project.getItemCount());
    }

    @Test
    public void addEvent_otherProject_rejectsWithoutMutation() {
        Project otherProject = new Project("P002", "C001", "Logo design", deadline);
        Event event = new Event("E001", otherProject, "Client review", deadline);

        EventException exception = assertThrows(EventException.class,
                () -> project.addEvent(event));

        assertEquals("The event belongs to another project.", exception.getMessage());
        assertEquals(0, project.getItemCount());
    }

    @Test
    public void addTask_duplicateId_rejectsWithoutMutation() {
        Task original = new Task("T001", project, "Create wireframe", deadline);
        project.addTask(original);

        assertThrows(TaskException.class,
                () -> project.addTask(new Task("T001", project, "Another task", deadline)));

        assertEquals(List.of(original), project.getTasks());
    }

    @Test
    public void addEvent_duplicateId_rejectsWithoutMutation() {
        Event original = new Event("E001", project, "Client review", deadline);
        project.addEvent(original);

        assertThrows(EventException.class,
                () -> project.addEvent(new Event("E001", project, "Another meeting", deadline)));

        assertEquals(List.of(original), project.getEvents());
    }

    @Test
    public void addItem_nullItem_rejectsWithoutMutation() {
        assertThrows(TaskException.class, () -> project.addTask(null));
        assertThrows(EventException.class, () -> project.addEvent(null));

        assertEquals(0, project.getItemCount());
    }

    @Test
    public void getTasks_existingTasks_returnsUnmodifiableSnapshot() {
        Task original = new Task("T001", project, "Create wireframe", deadline);
        project.addTask(original);
        List<Task> snapshot = project.getTasks();

        assertThrows(UnsupportedOperationException.class, snapshot::clear);
        project.addTask(new Task("T002", project, "Create prototype", deadline));

        assertEquals(List.of(original), snapshot);
        assertEquals(2, project.getTasks().size());
    }

    @Test
    public void getEvents_existingEvents_returnsUnmodifiableSnapshot() {
        Event original = new Event("E001", project, "Client review", deadline);
        project.addEvent(original);
        List<Event> snapshot = project.getEvents();

        assertThrows(UnsupportedOperationException.class, snapshot::clear);
        project.addEvent(new Event("E002", project, "Presentation", deadline));

        assertEquals(List.of(original), snapshot);
        assertEquals(2, project.getEvents().size());
    }

    @Test
    public void changeStatus_supportedStatus_updatesStatus() {
        for (ProjectStatus status : ProjectStatus.values()) {
            project.changeStatus(status);
            assertEquals(status, project.getStatus());
        }
    }

    @Test
    public void changeStatus_nullStatus_preservesStatus() {
        project.changeStatus(ProjectStatus.PAUSED);

        assertThrows(ProjectException.class, () -> project.changeStatus(null));

        assertEquals(ProjectStatus.PAUSED, project.getStatus());
    }

    @Test
    public void addItem_pastAndLaterDates_addsWithoutExtraRestrictions() {
        project.changeStatus(ProjectStatus.COMPLETED);
        project.addTask(new Task("T001", project, "Record historical work", LocalDate.of(2020, 1, 1)));
        project.addEvent(new Event("E001", project, "Follow-up review", deadline.plusDays(1)));

        assertEquals(2, project.getItemCount());
        assertEquals(ProjectStatus.COMPLETED, project.getStatus());
    }
}
