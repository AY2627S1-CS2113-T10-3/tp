package busynotfree.model;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import busynotfree.exception.EventException;
import busynotfree.exception.ProjectException;
import busynotfree.exception.TaskException;

/**
 * Keeps projects in memory and assigns independent project, task and event IDs.
 * Counters advance only after successful additions and are shared across projects.
 * This manager does not load or save data between application runs.
 */
public final class ProjectManager {
    /** Diagnostic creation logs are quiet by default and omit client names and item descriptions. */
    private static final Logger LOGGER = Logger.getLogger(ProjectManager.class.getName());

    private final ClientLookup clientLookup;
    private final Map<String, Project> projects = new LinkedHashMap<>();
    private int nextProjectNumber = 1;
    private int nextTaskNumber = 1;
    private int nextEventNumber = 1;

    /**
     * Creates an empty manager using the supplied client-existence check.
     *
     * @param clientLookup client lookup, temporarily supplied by a placeholder
     */
    public ProjectManager(ClientLookup clientLookup) {
        this.clientLookup = Objects.requireNonNull(clientLookup);
    }

    /**
     * Creates a project for a client accepted by the configured lookup.
     *
     * @param clientId owning client's ID
     * @param name non-blank project name
     * @param deadline project deadline
     * @return the newly registered project
     * @throws ProjectException if required data is missing or the client does not exist
     */
    public Project addProject(String clientId, String name, LocalDate deadline) {
        Project project = new Project(formatId("P", nextProjectNumber), clientId, name, deadline);
        if (!clientLookup.exists(project.getClientId())) {
            LOGGER.fine("Project creation rejected because the client does not exist.");
            throw new ProjectException("Client " + project.getClientId() + " does not exist.");
        }
        projects.put(project.getProjectId(), project);
        nextProjectNumber++;
        LOGGER.fine(() -> "Created project " + project.getProjectId() + ".");
        return project;
    }

    /**
     * Creates and attaches a task to an existing project.
     *
     * @param projectId owning project's ID
     * @param description non-blank description of the work
     * @param date task date
     * @return the newly attached task
     * @throws ProjectException if the project ID is blank or the project does not exist
     * @throws TaskException if required task data is missing or the task cannot be attached
     */
    public Task addTask(String projectId, String description, LocalDate date) {
        Project project = getProject(projectId);
        Task task = new Task(formatId("T", nextTaskNumber), project, description, date);
        project.addTask(task);
        nextTaskNumber++;
        LOGGER.fine(() -> "Added task " + task.getId() + " to project " + project.getProjectId() + ".");
        return task;
    }

    /** Finds a task by its application-wide ID, regardless of its project. */
    public Task getTask(String taskId) {
        if (taskId == null || taskId.isBlank()) {
            throw new TaskException("A task ID must not be blank.");
        }
        String requestedId = taskId.trim();
        for (Project project : projects.values()) {
            for (Task task : project.getTasks()) {
                if (task.getId().equals(requestedId)) {
                    return task;
                }
            }
        }
        throw new TaskException("Task " + requestedId + " was not found.");
    }

    /** Marks an existing task complete and returns it for the command response. */
    public Task markTaskComplete(String taskId) {
        Task task = getTask(taskId);
        task.markCompleted();
        return task;
    }

    /** Reopens an existing task and returns it for the command response. */
    public Task markTaskIncomplete(String taskId) {
        Task task = getTask(taskId);
        task.markIncomplete();
        return task;
    }

    /**
     * Updates an existing task's description and date.
     * Leaves ID counters unchanged because no new task is created.
     *
     * @param taskId application-wide ID of the task to edit
     * @param description new non-blank description of the work
     * @param date new task date
     * @return the edited task, with its ID, project and completion state preserved
     * @throws TaskException if the task does not exist or the new details are invalid
     */
    public Task editTask(String taskId, String description, LocalDate date) {
        Task task = getTask(taskId);
        task.edit(description, date);
        LOGGER.fine(() -> "Edited task " + task.getId() + ".");
        return task;
    }

    /** Finds an event by its application-wide ID, regardless of its project. */
    public Event getEvent(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new EventException("An event ID must not be blank.");
        }
        String requestedId = eventId.trim();
        for (Project project : projects.values()) {
            for (Event event : project.getEvents()) {
                if (event.getId().equals(requestedId)) {
                    return event;
                }
            }
        }
        throw new EventException("Event " + requestedId + " was not found.");
    }

    /**
     * Updates an existing event's description and date.
     * Leaves ID counters unchanged because no new event is created.
     *
     * @param eventId application-wide ID of the event to edit
     * @param description new non-blank description of the event
     * @param date new event date
     * @return the edited event, with its ID and project preserved
     * @throws EventException if the event does not exist or the new details are invalid
     */
    public Event editEvent(String eventId, String description, LocalDate date) {
        Event event = getEvent(eventId);
        event.edit(description, date);
        LOGGER.fine(() -> "Edited event " + event.getId() + ".");
        return event;
    }

    /**
     * Creates and attaches an event to an existing project.
     *
     * @param projectId owning project's ID
     * @param description non-blank description of the event
     * @param date event date
     * @return the newly attached event
     * @throws ProjectException if the project ID is blank or the project does not exist
     * @throws EventException if required event data is missing or the event cannot be attached
     */
    public Event addEvent(String projectId, String description, LocalDate date) {
        Project project = getProject(projectId);
        Event event = new Event(formatId("E", nextEventNumber), project, description, date);
        project.addEvent(event);
        nextEventNumber++;
        LOGGER.fine(() -> "Added event " + event.getId() + " to project " + project.getProjectId() + ".");
        return event;
    }

    /**
     * Changes the status of an existing project.
     *
     * @param projectId project ID to update
     * @param status new project status
     * @return the updated project
     * @throws ProjectException if the project ID is blank, the project does not exist or the status is null
     */
    public Project changeProjectStatus(String projectId, ProjectStatus status) {
        Project project = getProject(projectId);
        assert project != null;
        project.changeStatus(status);
        assert project.getStatus() == status;
        return project;
    }

    /**
     * Deletes an existing project and all its activities.
     * Leaves ID counters unchanged so deleted IDs are not reused.
     *
     * @param projectId ID of the project to delete
     * @return total number of tasks and events removed
     * @throws ProjectException if the ID is blank or the project does not exist
     */
    public int deleteProject(String projectId) {
        Project project = getProject(projectId);
        int expectedCount = project.getItemCount();
        int removedCount = project.removeAllActivities();
        projects.remove(project.getProjectId());

        assert removedCount == expectedCount
                : "Removed activity count must match the original count";
        assert !projects.containsKey(project.getProjectId())
                : "Deleted project must no longer be registered";

        return removedCount;
    }

    /**
     * Looks up a project without creating a missing parent.
     *
     * @param projectId project ID to look up
     * @return the existing project
     * @throws ProjectException if the ID is blank or does not identify an existing project
     */
    public Project getProject(String projectId) {
        if (projectId == null || projectId.isBlank()) {
            throw new ProjectException("A project ID must not be blank.");
        }
        Project project = projects.get(projectId.trim());
        if (project == null) {
            LOGGER.fine("Project lookup failed because the project does not exist.");
            throw new ProjectException("Project " + projectId.trim() + " does not exist.");
        }
        return project;
    }

    /**
     * Returns an unmodifiable snapshot of projects in creation order.
     *
     * @return the projects currently registered in memory
     */
    public List<Project> getProjects() {
        return List.copyOf(projects.values());
    }

    /** Returns the next project number for saving the ID sequence. */
    public int getNextProjectNumber() {
        return nextProjectNumber;
    }

    /** Returns the next task number for saving the ID sequence. */
    public int getNextTaskNumber() {
        return nextTaskNumber;
    }

    /** Returns the next event number for saving the ID sequence. */
    public int getNextEventNumber() {
        return nextEventNumber;
    }

    /** Advances ID sequences while restoring saved projects and scheduled items. */
    public void advanceNextNumbers(int projectNumber, int taskNumber, int eventNumber) {
        if (projectNumber < nextProjectNumber || taskNumber < nextTaskNumber || eventNumber < nextEventNumber) {
            throw new ProjectException("Saved project or activity IDs are out of order.");
        }
        nextProjectNumber = projectNumber;
        nextTaskNumber = taskNumber;
        nextEventNumber = eventNumber;
    }

    /** Pads each type-specific ID to at least three digits, allowing IDs beyond 999. */
    private String formatId(String prefix, int number) {
        return String.format(Locale.ROOT, "%s%03d", prefix, number);
    }
}
