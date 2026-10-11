package busynotfree.model;

import java.time.LocalDate;

import busynotfree.exception.TaskException;

/**
 * Represents a dated internal task belonging to one project.
 */
public final class Task extends ScheduledItem {
    /**
     * Creates an internal task without automatically adding it to its project.
     *
     * @param taskId unique task ID assigned by the application's model
     * @param project project that owns the task
     * @param description non-blank description of the work
     * @param date calendar date of the task
     * @throws TaskException if required data is missing
     */
    public Task(String taskId, Project project, String description, LocalDate date) {
        super(taskId, project, description, date, TaskException::new);
    }

    /** Returns the task type for polymorphic messages. */
    @Override
    public String getType() {
        return "task";
    }
}
