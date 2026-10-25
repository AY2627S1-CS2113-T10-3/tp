package busynotfree.model;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import busynotfree.exception.EventException;
import busynotfree.exception.ProjectException;
import busynotfree.exception.TaskException;

/**
 * Keeps projects in memory and assigns independent project, task and event IDs.
 * Counters advance only after successful additions and are shared across projects.
 * This manager does not load or save data between application runs.
 */
public final class ProjectManager {
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
            throw new ProjectException("Client " + project.getClientId() + " does not exist.");
        }
        projects.put(project.getProjectId(), project);
        nextProjectNumber++;
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
        return task;
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
