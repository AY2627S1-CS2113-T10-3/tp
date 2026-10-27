package busynotfree.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import busynotfree.exception.ClientException;
import busynotfree.exception.ProjectException;

/** Tests shared trash ownership, snapshot isolation and restoration failure handling. */
class TrashcanManagerTest {
    private final TrashcanManager trashcan = new TrashcanManager();
    private final ClientManager clients = new ClientManager(trashcan);
    private final ProjectManager projects = new ProjectManager(clients, trashcan);
    private final LocalDate date = LocalDate.of(2026, 11, 30);

    @Test
    public void constructors_shareTrashcanForDefaultAndExplicitWiring() {
        ClientManager defaultClients = new ClientManager();
        ProjectManager defaultProjects = new ProjectManager(defaultClients);

        assertSame(defaultClients.getTrashcanManager(), defaultProjects.getTrashcanManager());
        assertSame(trashcan, clients.getTrashcanManager());
        assertSame(trashcan, projects.getTrashcanManager());
    }

    @Test
    public void deleteClientAndProject_storesIndependentSnapshotsInSharedTrashcan() {
        Client client = clients.addClient("Acme");
        clients.addNote("C001", "Call on Friday");
        Project project = projects.addProject("C001", "Website", date);
        projects.addTask("P001", "Wireframe", date);
        projects.markTaskComplete("T001");
        projects.addEvent("P001", "Meeting", date);

        projects.deleteProject("P001");
        clients.deleteClient("C001", projects);
        client.setName("Changed");
        project.changeStatus(ProjectStatus.COMPLETED);

        assertEquals("Acme", trashcan.getDeletedClients().getFirst().getName());
        assertEquals(List.of("Call on Friday"), trashcan.getDeletedClients().getFirst().getNotes());
        Project snapshot = trashcan.getDeletedProjects().getFirst();
        assertEquals(ProjectStatus.ACTIVE, snapshot.getStatus());
        assertEquals(2, snapshot.getItemCount());
        assertTrue(snapshot.getTasks().getFirst().isCompleted());
        assertTrue(client.getNotes().isEmpty());
        assertEquals(0, project.getItemCount());

        snapshot.removeAllActivities();
        trashcan.getDeletedClients().getFirst().clearNotes();
        assertEquals(2, trashcan.getDeletedProjects().getFirst().getItemCount());
        assertEquals(List.of("Call on Friday"), trashcan.getDeletedClients().getFirst().getNotes());
    }

    @Test
    public void restoreProject_successRegistersActivitiesBeforeRemovingTrashEntry() {
        clients.addClient("Acme");
        projects.addProject("C001", "Website", date);
        projects.addTask("P001", "Wireframe", date);
        projects.deleteProject("P001");

        Project restored = trashcan.restoreProject(" P001 ", projects);

        assertSame(restored, projects.getProject("P001"));
        assertSame(restored, projects.getTask("T001").getProject());
        assertTrue(trashcan.getDeletedProjects().isEmpty());
        assertEquals(2, projects.getNextProjectNumber());
        assertEquals(2, projects.getNextTaskNumber());
    }

    @Test
    public void restoreProject_failedRegistration_keepsSnapshot() {
        clients.addClient("Acme");
        projects.addProject("C001", "Website", date);
        projects.addEvent("P001", "Meeting", date);
        projects.deleteProject("P001");
        clients.deleteClient("C001", projects);

        assertThrows(ProjectException.class, () -> trashcan.restoreProject("P001", projects));

        assertTrue(projects.getProjects().isEmpty());
        assertEquals(1, trashcan.getDeletedProjects().size());
        assertEquals("E001", trashcan.getDeletedProjects().getFirst().getEvents().getFirst().getId());
    }

    @Test
    public void storeDuplicateClientOrProject_rejectsWithoutReplacingSnapshot() {
        Client client = new Client("C001", "Original client");
        client.addNote("Keep this note");
        trashcan.storeClient(client);
        Project project = new Project("P001", "C001", "Original project", date);
        trashcan.storeProject(project);

        assertThrows(ClientException.class,
                () -> trashcan.storeClient(new Client("C001", "Replacement")));
        assertThrows(ProjectException.class,
                () -> trashcan.storeProject(new Project("P001", "C001", "Replacement", date)));

        assertEquals("Original client", trashcan.getDeletedClients().getFirst().getName());
        assertEquals("Original project", trashcan.getDeletedProjects().getFirst().getName());
    }

    @Test
    public void storeProject_conflictingActivityIds_rejectsWithoutChangingTrashcan() {
        Project first = new Project("P001", "C001", "First", date);
        first.addTask(new Task("T001", first, "Task", date));
        first.addEvent(new Event("E001", first, "Event", date));
        trashcan.storeProject(first);
        Project taskConflict = new Project("P002", "C001", "Second", date);
        taskConflict.addTask(new Task("T001", taskConflict, "Duplicate", date));
        Project eventConflict = new Project("P003", "C001", "Third", date);
        eventConflict.addEvent(new Event("E001", eventConflict, "Duplicate", date));

        assertThrows(ProjectException.class, () -> trashcan.storeProject(taskConflict));
        assertThrows(ProjectException.class, () -> trashcan.storeProject(eventConflict));

        assertEquals(1, trashcan.getDeletedProjects().size());
        assertEquals(2, trashcan.getDeletedProjects().getFirst().getItemCount());
        assertFalse(trashcan.getDeletedProjects().getFirst().getTasks().isEmpty());
    }
}
