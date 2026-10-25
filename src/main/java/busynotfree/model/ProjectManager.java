package busynotfree.model;

import java.time.LocalDate;
import java.util.Comparator;
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
 * {@code DataStorage} saves and restores the projects and counters,
 * so deleted IDs are not reused after a restart.
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
     * @param clientLookup Client-existence check, normally the application's {@code ClientManager}.
     */
    public ProjectManager(ClientLookup clientLookup) {
        this.clientLookup = Objects.requireNonNull(clientLookup);
    }

    /**
     * Creates a project for a client accepted by the configured lookup.
     *
     * @param clientId Owning client's ID.
     * @param name Non-blank project name.
     * @param deadline Project deadline.
     * @return The newly registered project.
     * @throws ProjectException If required data is missing or the client does not exist.
     */
    public Project addProject(String clientId, String name, LocalDate deadline) {
        Project project = new Project(formatId("P", nextProjectNumber), clientId, name, deadline);
        if (!clientLookup.exists(project.getClientId())) {
            LOGGER.fine("Project creation rejected because the client does not exist.");
            throw new ProjectException("Client " + project.getClientId()
                    + " does not exist. Use client.list to see client IDs.");
        }
        // Detects an ID-allocation bug, such as a counter restored too low, rather than invalid user input.
        assert !projects.containsKey(project.getProjectId())
                : "A new project ID must not replace an existing project.";
        projects.put(project.getProjectId(), project);
        nextProjectNumber++;
        LOGGER.fine(() -> "Created project " + project.getProjectId() + ".");
        return project;
    }

    /**
     * Creates and attaches a task to an existing project.
     *
     * @param projectId Owning project's ID.
     * @param description Non-blank description of the work.
     * @param date Task date.
     * @return The newly attached task.
     * @throws ProjectException If the project ID is blank or the project does not exist.
     * @throws TaskException If required task data is missing or the task cannot be attached.
     */
    public Task addTask(String projectId, String description, LocalDate date) {
        Project project = getProject(projectId);
        Task task = new Task(formatId("T", nextTaskNumber), project, description, date);
        project.addTask(task);
        assert project.getTasks().getLast() == task : "A new task must be appended to its own project.";
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
     * @param taskId Application-wide ID of the task to edit.
     * @param description New non-blank description of the work.
     * @param date New task date.
     * @return The edited task, with its ID, project and completion state preserved.
     * @throws TaskException If the task does not exist or the new details are invalid.
     */
    public Task editTask(String taskId, String description, LocalDate date) {
        Task task = getTask(taskId);
        boolean wasCompleted = task.isCompleted();
        task.edit(description, date);
        assert task.isCompleted() == wasCompleted : "Editing a task must not change its completion state.";
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
        LOGGER.fine("Event lookup failed because the event does not exist.");
        throw new EventException("Event " + requestedId
                + " was not found. Use project.view to see event IDs.");
    }

    /**
     * Updates an existing event's description and date.
     * Leaves ID counters unchanged because no new event is created.
     *
     * @param eventId Application-wide ID of the event to edit.
     * @param description New non-blank description of the event.
     * @param date New event date.
     * @return The edited event, with its ID and project preserved.
     * @throws EventException If the event does not exist or the new details are invalid.
     */
    public Event editEvent(String eventId, String description, LocalDate date) {
        Event event = getEvent(eventId);
        event.edit(description, date);
        assert event.getDate().equals(date) : "An edited event must hold its new date.";
        LOGGER.fine(() -> "Edited event " + event.getId() + ".");
        return event;
    }

    /**
     * Creates and attaches an event to an existing project.
     *
     * @param projectId Owning project's ID.
     * @param description Non-blank description of the event.
     * @param date Event date.
     * @return The newly attached event.
     * @throws ProjectException If the project ID is blank or the project does not exist.
     * @throws EventException If required event data is missing or the event cannot be attached.
     */
    public Event addEvent(String projectId, String description, LocalDate date) {
        Project project = getProject(projectId);
        Event event = new Event(formatId("E", nextEventNumber), project, description, date);
        project.addEvent(event);
        assert project.getEvents().getLast() == event : "A new event must be appended to its own project.";
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
     * @param projectId Project ID to look up.
     * @return The existing project.
     * @throws ProjectException If the ID is blank or does not identify an existing project.
     */
    public Project getProject(String projectId) {
        if (projectId == null || projectId.isBlank()) {
            throw new ProjectException("A project ID must not be blank.");
        }
        Project project = projects.get(projectId.trim());
        if (project == null) {
            LOGGER.fine("Project lookup failed because the project does not exist.");
            throw new ProjectException("Project " + projectId.trim()
                    + " does not exist. Use project.list to see project IDs.");
        }
        return project;
    }

    /**
     * Returns an unmodifiable snapshot of projects in creation order.
     *
     * @return The projects currently registered in memory.
     */
    public List<Project> getProjects() {
        return List.copyOf(projects.values());
    }

    /**
     * Returns projects with the requested status in their creation order.
     *
     * @param status status to match
     * @return an unmodifiable snapshot of matching projects
     * @throws ProjectException if the status is null
     */
    public List<Project> getProjectsByStatus(ProjectStatus status) {
        if (status == null) {
            throw new ProjectException("A project status is required.");
        }
        return projects.values().stream()
                .filter(project -> project.getStatus() == status)
                .toList();
    }

    /**
     * Returns projects ordered by the earliest deadline first.
     * Projects with the same deadline retain their creation order.
     *
     * @return an unmodifiable snapshot ordered by deadline
     */
    public List<Project> getProjectsByDeadline() {
        return projects.values().stream()
                .sorted(Comparator.comparing(Project::getDeadline))
                .toList();
    }

    /**
     * Returns active projects whose deadlines are before the supplied date.
     *
     * @param today date against which deadlines are compared
     * @return an unmodifiable snapshot of overdue active projects
     * @throws ProjectException if the comparison date is null
     */
    public List<Project> getOverdueActiveProjects(LocalDate today) {
        if (today == null) {
            throw new ProjectException("A comparison date is required.");
        }
        return projects.values().stream()
                .filter(project -> project.getStatus() == ProjectStatus.ACTIVE)
                .filter(project -> project.getDeadline().isBefore(today))
                .toList();
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
