package busynotfree.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import busynotfree.exception.TaskException;

/**
 * Tests task data, validation and behaviour through the shared scheduled-item type.
 */
class TaskTest {
    private final Project project = new Project("P001", "C001", "Website redesign", LocalDate.of(2026, 11, 30));
    private final LocalDate date = LocalDate.of(2026, 10, 15);

    @Test
    public void constructor_validData_preservesFields() {
        Task task = new Task("T001", project, "Create wireframe", date);

        assertEquals("T001", task.getId());
        assertSame(project, task.getProject());
        assertEquals("Create wireframe", task.getDescription());
        assertEquals(date, task.getDate());
        assertEquals(0, project.getItemCount());
    }

    @Test
    public void constructor_extraWhitespace_trimsDescription() {
        Task task = new Task("T001", project, "  Create  wireframe  ", date);

        assertEquals("Create  wireframe", task.getDescription());
    }

    @Test
    public void constructor_missingDescription_throwsException() {
        assertThrows(TaskException.class, () -> new Task("T001", project, " \t ", date));
        assertThrows(TaskException.class, () -> new Task("T001", project, null, date));
    }

    @Test
    public void constructor_missingId_throwsException() {
        assertThrows(TaskException.class, () -> new Task("", project, "Create wireframe", date));
        assertThrows(TaskException.class, () -> new Task(null, project, "Create wireframe", date));
    }

    @Test
    public void constructor_missingProject_throwsException() {
        assertThrows(TaskException.class, () -> new Task("T001", null, "Create wireframe", date));
    }

    @Test
    public void constructor_missingDate_throwsException() {
        assertThrows(TaskException.class, () -> new Task("T001", project, "Create wireframe", null));
    }

    @Test
    public void getType_baseReference_returnsTaskType() {
        ScheduledItem item = new Task("T001", project, "Create wireframe", date);

        assertEquals("task", item.getType());
        assertEquals("[T001] Create wireframe - 2026-10-15", item.toString());
    }

    @Test
    public void markCompleted_preservesTaskDetailsAndShowsCompletion() {
        Task task = new Task("T001", project, "Create wireframe", date);
        assertFalse(task.isCompleted());

        task.markCompleted();
        task.markCompleted();

        assertTrue(task.isCompleted());
        assertEquals("T001", task.getId());
        assertSame(project, task.getProject());
        assertEquals("Create wireframe", task.getDescription());
        assertEquals(date, task.getDate());
        assertEquals("[T001] Create wireframe - 2026-10-15 [COMPLETED]", task.toString());
    }
}
