package busynotfree.model;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

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
     * @throws IllegalArgumentException if required data is missing or the client does not exist
     */
    public Project addProject(String clientId, String name, LocalDate deadline) {
        Project project = new Project(formatId("P", nextProjectNumber), clientId, name, deadline);
        if (!clientLookup.exists(project.getClientId())) {
            throw new IllegalArgumentException("Client " + project.getClientId() + " does not exist.");
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
     * @throws IllegalArgumentException if required data is missing or the project does not exist
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
     * @throws IllegalArgumentException if required data is missing or the project does not exist
     */
    public Event addEvent(String projectId, String description, LocalDate date) {
        Project project = getProject(projectId);
        Event event = new Event(formatId("E", nextEventNumber), project, description, date);
        project.addEvent(event);
        nextEventNumber++;
        return event;
    }

    /**
     * Looks up a project without creating a missing parent.
     *
     * @param projectId project ID to look up
     * @return the existing project
     * @throws IllegalArgumentException if the ID is blank or does not identify an existing project
     */
    public Project getProject(String projectId) {
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalArgumentException("A project ID must not be blank.");
        }
        Project project = projects.get(projectId.trim());
        if (project == null) {
            throw new IllegalArgumentException("Project " + projectId.trim() + " does not exist.");
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

    /** Pads each type-specific ID to at least three digits, allowing IDs beyond 999. */
    private String formatId(String prefix, int number) {
        return String.format(Locale.ROOT, "%s%03d", prefix, number);
    }
}
