package busynotfree.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import busynotfree.exception.BusyNotFreeException;
import busynotfree.exception.EventException;
import busynotfree.exception.ProjectException;
import busynotfree.exception.TaskException;

/**
 * Stores a client's project and owns separate collections of tasks and events.
 * Client existence and application-wide ID uniqueness are checked by the model,
 * not by this entity. New projects start as active.
 */
public final class Project {
    private final String projectId;
    private final String clientId;
    private final String name;
    private final LocalDate deadline;
    private final List<Task> tasks = new ArrayList<>();
    private final List<Event> events = new ArrayList<>();
    private ProjectStatus status = ProjectStatus.ACTIVE;

    /**
     * Creates an empty active project with externally assigned identifiers.
     *
     * @param projectId Unique project ID assigned by the application's model.
     * @param clientId ID of the existing client who owns the project.
     * @param name Non-blank project name.
     * @param deadline Project deadline as a calendar date.
     * @throws ProjectException If required data is missing.
     */
    public Project(String projectId, String clientId, String name, LocalDate deadline) {
        if (projectId == null || projectId.isBlank()) {
            throw new ProjectException("A project ID must not be blank.");
        }
        if (clientId == null || clientId.isBlank()) {
            throw new ProjectException("A client ID must not be blank.");
        }
        if (name == null || name.isBlank()) {
            throw new ProjectException("A project name must not be blank.");
        }
        if (deadline == null) {
            throw new ProjectException("A project deadline is required.");
        }
        this.projectId = projectId.trim();
        this.clientId = clientId.trim();
        this.name = name.trim();
        this.deadline = deadline;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getClientId() {
        return clientId;
    }

    public String getName() {
        return name;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    /**
     * Returns an unmodifiable snapshot of the project's tasks.
     *
     * @return Tasks in insertion order.
     */
    public List<Task> getTasks() {
        return List.copyOf(tasks);
    }

    /**
     * Returns an unmodifiable snapshot of the project's events.
     *
     * @return Events in insertion order.
     */
    public List<Event> getEvents() {
        return List.copyOf(events);
    }

    /**
     * Adds a task that belongs to this project.
     *
     * @param task Task to add.
     * @throws TaskException If the task is null, belongs elsewhere or has a duplicate ID.
     */
    public void addTask(Task task) {
        validateNewItem(task, tasks, TaskException::new);
        tasks.add(task);
    }

    /**
     * Adds an event that belongs to this project.
     *
     * @param event Event to add.
     * @throws EventException If the event is null, belongs elsewhere or has a duplicate ID.
     */
    public void addEvent(Event event) {
        validateNewItem(event, events, EventException::new);
        events.add(event);
    }

    /**
     * Changes the project's status without changing its scheduled items.
     *
     * @param status New project status.
     * @throws ProjectException If the status is null.
     */
    public void changeStatus(ProjectStatus status) {
        if (status == null) {
            throw new ProjectException("A project status is required.");
        }
        this.status = status;
    }

    /**
     * Returns the combined number of tasks and events owned by this project.
     *
     * @return Number of scheduled items.
     */
    public int getItemCount() {
        return tasks.size() + events.size();
    }

    /**
     * Removes all tasks and events owned by this project.
     *
     * @return total number of activities removed
     */
    int removeAllActivities() {
        int removedCount = getItemCount();
        tasks.clear();
        events.clear();

        assert tasks.isEmpty() : "All project tasks must be removed";
        assert events.isEmpty() : "All project events must be removed";

        return removedCount;
    }

    /** Checks ownership and ID uniqueness using the task or event exception supplied by the caller. */
    private void validateNewItem(ScheduledItem item, List<? extends ScheduledItem> existingItems,
            Function<String, ? extends BusyNotFreeException> exceptionFactory) {
        if (item == null) {
            throw exceptionFactory.apply("A scheduled item is required.");
        }
        if (item.getProject() != this) {
            throw exceptionFactory.apply("The " + item.getType() + " belongs to another project.");
        }
        for (ScheduledItem existingItem : existingItems) {
            if (existingItem.getId().equals(item.getId())) {
                throw exceptionFactory.apply("Item ID " + item.getId() + " already exists in this project.");
            }
        }
    }
}
