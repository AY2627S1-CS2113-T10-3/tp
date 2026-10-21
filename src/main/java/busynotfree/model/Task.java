package busynotfree.model;

import java.time.LocalDate;

import busynotfree.exception.TaskException;

/**
 * Represents a dated internal task belonging to one project.
 */
public final class Task extends ScheduledItem {
    private boolean completed;

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

    /** Returns whether this task has been marked complete. */
    public boolean isCompleted() {
        return completed;
    }

    /** Marks this task complete without changing its identifying or scheduled details. */
    public void markCompleted() {
        completed = true;
    }

    /** Returns the task type for polymorphic messages. */
    @Override
    public String getType() {
        return "task";
    }

    /** Shows the completion state only after the task has been finished. */
    @Override
    public String toString() {
        return super.toString() + (completed ? " [COMPLETED]" : "");
    }
}
