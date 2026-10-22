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
     * @param taskId Unique task ID assigned by the application's model.
     * @param project Project that owns the task.
     * @param description Non-blank description of the work.
     * @param date Calendar date of the task.
     * @throws TaskException If required data is missing.
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

    /** Reopens this task without changing its identifying or scheduled details. */
    public void markIncomplete() {
        completed = false;
    }

    /**
     * Updates this task's description and date while preserving its ID, project and completion state.
     * Package-private so that edits go through the project manager's ID lookup.
     *
     * @param description New non-blank description of the work.
     * @param date New calendar date of the task.
     * @throws TaskException If either value is missing; the task is then left unchanged.
     */
    void edit(String description, LocalDate date) {
        updateDetails(description, date, TaskException::new);
    }

    /** Returns the task type for polymorphic messages. */
    @Override
    public String getType() {
        return "task";
    }

    /** Shows whether this task is complete or still in progress. */
    @Override
    public String toString() {
        return super.toString() + (completed ? " [COMPLETED]" : " [In Progress]");
    }
}
