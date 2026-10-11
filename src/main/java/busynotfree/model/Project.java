package busynotfree.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
     * @param projectId unique project ID assigned by the application's model
     * @param clientId ID of the existing client who owns the project
     * @param name non-blank project name
     * @param deadline project deadline as a calendar date
     * @throws IllegalArgumentException if required data is missing
     */
    public Project(String projectId, String clientId, String name, LocalDate deadline) {
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalArgumentException("A project ID must not be blank.");
        }
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("A client ID must not be blank.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("A project name must not be blank.");
        }
        if (deadline == null) {
            throw new IllegalArgumentException("A project deadline is required.");
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
     * @return tasks in insertion order
     */
    public List<Task> getTasks() {
        return List.copyOf(tasks);
    }

    /**
     * Returns an unmodifiable snapshot of the project's events.
     *
     * @return events in insertion order
     */
    public List<Event> getEvents() {
        return List.copyOf(events);
    }

    /**
     * Adds a task that belongs to this project.
     *
     * @param task task to add
     * @throws IllegalArgumentException if the task is null, belongs elsewhere or has a duplicate ID
     */
    public void addTask(Task task) {
        validateNewItem(task, tasks);
        tasks.add(task);
    }

    /**
     * Adds an event that belongs to this project.
     *
     * @param event event to add
     * @throws IllegalArgumentException if the event is null, belongs elsewhere or has a duplicate ID
     */
    public void addEvent(Event event) {
        validateNewItem(event, events);
        events.add(event);
    }

    /**
     * Changes the project's status without changing its scheduled items.
     *
     * @param status new project status
     * @throws IllegalArgumentException if the status is null
     */
    public void changeStatus(ProjectStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("A project status is required.");
        }
        this.status = status;
    }

    /**
     * Returns the combined number of tasks and events owned by this project.
     *
     * @return number of scheduled items
     */
    public int getItemCount() {
        return tasks.size() + events.size();
    }

    /** Checks ownership and ID uniqueness before either collection is changed. */
    private void validateNewItem(ScheduledItem item, List<? extends ScheduledItem> existingItems) {
        if (item == null) {
            throw new IllegalArgumentException("A scheduled item is required.");
        }
        if (item.getProject() != this) {
            throw new IllegalArgumentException("The " + item.getType() + " belongs to another project.");
        }
        for (ScheduledItem existingItem : existingItems) {
            if (existingItem.getId().equals(item.getId())) {
                throw new IllegalArgumentException("Item ID " + item.getId() + " already exists in this project.");
            }
        }
    }
}
