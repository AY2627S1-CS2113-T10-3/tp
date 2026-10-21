package busynotfree.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import busynotfree.exception.ClientException;
import busynotfree.exception.ProjectException;

/**
 * Tests client registration, note ownership and the project lookup integration.
 */
class ClientManagerTest {
    private final ClientManager manager = new ClientManager();

    @Test
    public void addClient_duplicateNames_assignsDistinctSequentialIds() {
        Client first = manager.addClient("Acme Studio");
        Client second = manager.addClient("Acme Studio");

        assertEquals("C001", first.getClientId());
        assertEquals("C002", second.getClientId());
        assertEquals(List.of(first, second), manager.getClients());
        assertSame(first, manager.getClient(" C001 "));
    }

    @Test
    public void addClient_invalidNames_doesNotConsumeId() {
        assertThrows(ClientException.class, () -> manager.addClient(null));
        assertThrows(ClientException.class, () -> manager.addClient(""));
        assertThrows(ClientException.class, () -> manager.addClient(" \t "));
        assertTrue(manager.getClients().isEmpty());
        assertEquals("C001", manager.addClient("Acme").getClientId());
    }

    @Test
    public void addClient_moreThan999Clients_keepsUniqueIds() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 1001; i++) {
            assertTrue(ids.add(manager.addClient("Client " + i).getClientId()));
        }

        assertEquals(1001, manager.getClients().size());
        assertEquals("C1000", manager.getClients().get(999).getClientId());
        assertEquals("C1001", manager.getClients().getLast().getClientId());
    }

    @Test
    public void getClient_missingOrInvalidId_doesNotCreateClient() {
        assertThrows(ClientException.class, () -> manager.getClient(null));
        assertThrows(ClientException.class, () -> manager.getClient(" \t "));
        for (String id : List.of("C001", "C999", "C1", "c001", "P001", "invalid")) {
            ClientException exception = assertThrows(ClientException.class, () -> manager.getClient(id));
            assertEquals("Client " + id + " does not exist.", exception.getMessage());
        }
        assertTrue(manager.getClients().isEmpty());
    }

    @Test
    public void addNote_existingClient_changesOnlyThatClient() {
        Client first = manager.addClient("Acme");
        Client second = manager.addClient("Bright Ideas");

        assertSame(second, manager.addNote("C002", "Second client's note"));
        assertTrue(first.getNotes().isEmpty());
        assertEquals(List.of("Second client's note"), second.getNotes());
    }

    @Test
    public void addNote_invalidData_preservesClientsAndNotes() {
        Client client = manager.addClient("Acme");
        manager.addNote("C001", "First note");

        assertThrows(ClientException.class, () -> manager.addNote("C999", "Missing client"));
        assertThrows(ClientException.class, () -> manager.addNote("C001", " \t "));
        assertEquals(List.of(client), manager.getClients());
        assertEquals(List.of("First note"), client.getNotes());
    }

    @Test
    public void getClients_snapshot_isUnmodifiableAndPreservesOrder() {
        Client first = manager.addClient("Acme");
        List<Client> snapshot = manager.getClients();

        assertThrows(UnsupportedOperationException.class, () -> snapshot.clear());
        Client second = manager.addClient("Bright Ideas");
        assertEquals(List.of(first), snapshot);
        assertEquals(List.of(first, second), manager.getClients());
    }

    @Test
    public void exists_suppliedIds_checksRegisteredClientsOnly() {
        assertFalse(manager.exists("C001"));
        manager.addClient("Acme");

        assertTrue(manager.exists("C001"));
        assertTrue(manager.exists(" C001 "));
        assertFalse(manager.exists("c001"));
        assertFalse(manager.exists("C999"));
        assertFalse(manager.exists(null));
        assertFalse(manager.exists(" \t "));
    }

    @Test
    public void addProject_realClientLookup_requiresRegisteredClient() {
        ProjectManager projectManager = new ProjectManager(manager);
        LocalDate deadline = LocalDate.of(2026, 11, 30);

        assertThrows(ProjectException.class, () -> projectManager.addProject("C001", "Website", deadline));
        assertTrue(projectManager.getProjects().isEmpty());
        manager.addClient("Acme");
        Project project = projectManager.addProject("C001", "Website", deadline);
        assertEquals("P001", project.getProjectId());
        assertEquals("C001", project.getClientId());
    }

    @Test
    public void addClient_separateManagers_startWithIndependentState() {
        manager.addClient("Acme");
        ClientManager otherManager = new ClientManager();

        assertFalse(otherManager.exists("C001"));
        assertEquals("C001", otherManager.addClient("Bright Ideas").getClientId());
        assertEquals("Acme", manager.getClient("C001").getName());
    }
}
