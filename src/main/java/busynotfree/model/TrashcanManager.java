package busynotfree.model;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import busynotfree.exception.ClientException;
import busynotfree.exception.ProjectException;

/**
 * Stores independent snapshots of deleted projects and clients.
 * Coordinates restoration without allocating replacement IDs.
 */
public final class TrashcanManager {
    private final Map<String, Project> deletedProjects = new LinkedHashMap<>();
    private final Map<String, Client> deletedClients = new LinkedHashMap<>();

    /**
     * Permanently removes all retained projects and clients, including their activities and notes.
     * Active records and ID counters are unchanged.
     */
    public void empty() {
        deletedProjects.clear();
        deletedClients.clear();
    }

    /**
     * Retains a project and all its activities before active data is removed.
     *
     * @param project Project to copy into the trashcan.
     * @throws ProjectException If a project or activity ID is already retained.
     */
    void storeProject(Project project) {
        for (Project existing : deletedProjects.values()) {
            boolean duplicateTask = project.getTasks().stream()
                    .anyMatch(task -> existing.getTasks().stream()
                            .anyMatch(other -> other.getId().equals(task.getId())));
            boolean duplicateEvent = project.getEvents().stream()
                    .anyMatch(event -> existing.getEvents().stream()
                            .anyMatch(other -> other.getId().equals(event.getId())));
            if (existing.getProjectId().equals(project.getProjectId()) || duplicateTask || duplicateEvent) {
                throw new ProjectException("Cannot restore or load project "
                        + project.getProjectId() + ": an ID is already in use.");
            }
        }
        deletedProjects.put(project.getProjectId(), project.copy());
    }

    /**
     * Retains a client and its notes before active data is removed.
     *
     * @param client Client to copy into the trashcan.
     * @throws ClientException If its ID is already retained.
     */
    void storeClient(Client client) {
        String id = client.getClientId();
        if (deletedClients.containsKey(id)) {
            throw new ClientException("Duplicate saved client " + id + ".");
        }
        deletedClients.put(id, client.copy());
    }

    /** Returns independent project snapshots in ID order. */
    public List<Project> getDeletedProjects() {
        return deletedProjects.values().stream()
                .sorted(Comparator.comparingInt(project -> Integer.parseInt(project.getProjectId().substring(1))))
                .map(Project::copy).toList();
    }

    /** Returns independent client snapshots, including notes, in ID order. */
    public List<Client> getDeletedClients() {
        return deletedClients.values().stream()
                .sorted(Comparator.comparingInt(client -> Integer.parseInt(client.getClientId().substring(1))))
                .map(Client::copy).toList();
    }

    /**
     * Restores a client and its notes only after active-ID validation succeeds.
     * A failed restoration leaves the trash entry available.
     *
     * @param clientId Deleted client's case-sensitive ID.
     * @param clientManager Manager receiving the restored client.
     * @return Restored client with its original notes.
     * @throws ClientException If the ID is blank, absent from the trashcan or already active.
     */
    public Client restoreClient(String clientId, ClientManager clientManager) {
        if (clientId == null || clientId.isBlank()) {
            throw new ClientException("A client ID must not be blank.");
        }
        String id = clientId.trim();
        Client client = deletedClients.get(id);
        if (client == null) {
            throw new ClientException("Client " + id + " is not in the trashcan.");
        }
        clientManager.registerRestoredClient(client);
        deletedClients.remove(id);
        return client;
    }

    /**
     * Restores a project only after active-data validation succeeds.
     * A failed restoration leaves the trash entry available.
     *
     * @param projectId Deleted project's case-sensitive ID.
     * @param projectManager Manager receiving the restored project.
     * @return Restored project with its original activities.
     * @throws ProjectException If the ID is invalid, its client is inactive,
     *     or an ID conflicts with active data.
     */
    public Project restoreProject(String projectId, ProjectManager projectManager) {
        if (projectId == null || projectId.isBlank()) {
            throw new ProjectException("A project ID must not be blank.");
        }
        String id = projectId.trim();
        Project project = deletedProjects.get(id);
        if (project == null) {
            throw new ProjectException("Project " + id + " is not in the trashcan.");
        }
        projectManager.registerRestoredProject(project);
        deletedProjects.remove(id);
        return project;
    }
}
