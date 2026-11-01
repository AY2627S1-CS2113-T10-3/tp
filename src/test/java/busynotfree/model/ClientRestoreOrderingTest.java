package busynotfree.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import busynotfree.storage.DataStorage;

/** Checks numeric client ordering after restoration and persistence. */
class ClientRestoreOrderingTest {
    @TempDir
    private Path directory;

    /** Checks list and search ordering while preserving notes and the ID counter. */
    @Test
    public void restore_olderClient_sortsClientsAndPreservesState() {
        ClientManager clients = new ClientManager();
        ProjectManager projects = new ProjectManager(clients);
        clients.addClient("Acme First");
        clients.addClient("Acme Second");
        clients.addNote("C001", "First note");
        clients.addNote("C001", "Second note");
        clients.deleteClient("C001", projects);

        clients.getTrashcanManager().restoreClient("C001", clients);

        assertEquals(List.of("C001", "C002"), clientIds(clients.getClients()));
        assertEquals(List.of("C001", "C002"), clientIds(clients.findClients("Acme")));
        assertEquals("Acme First", clients.getClient("C001").getName());
        assertEquals(List.of("First note", "Second note"), clients.getClient("C001").getNotes());
        assertTrue(clients.getDeletedClients().isEmpty());
        assertEquals(3, clients.getNextClientNumber());
    }

    /** Checks that numeric ordering remains correct beyond three-digit IDs. */
    @Test
    public void restore_idsBeyondThreeDigits_sortsNumerically() {
        ClientManager clients = new ClientManager();
        ProjectManager projects = new ProjectManager(clients);
        clients.advanceNextClientNumber(999);
        clients.addClient("Client 999");
        clients.addClient("Client 1000");
        clients.deleteClient("C999", projects);

        clients.getTrashcanManager().restoreClient("C999", clients);

        assertEquals(List.of("C999", "C1000"), clientIds(clients.getClients()));
        assertEquals(1001, clients.getNextClientNumber());
    }

    /** Checks that saving and loading preserve the restored client order. */
    @Test
    public void restore_saveAndLoad_preservesNumericOrder() throws IOException {
        ClientManager clients = new ClientManager();
        ProjectManager projects = new ProjectManager(clients);
        clients.addClient("First");
        clients.addClient("Second");
        clients.deleteClient("C001", projects);
        clients.getTrashcanManager().restoreClient("C001", clients);

        DataStorage storage = new DataStorage(directory.resolve("busynotfree.properties"));
        storage.save(clients, projects);
        DataStorage.LoadedData loaded = storage.load();

        assertEquals(List.of("C001", "C002"), clientIds(loaded.clientManager().getClients()));
    }

    /** Extracts IDs while retaining the supplied display order. */
    private List<String> clientIds(List<Client> clients) {
        return clients.stream().map(Client::getClientId).toList();
    }
}
