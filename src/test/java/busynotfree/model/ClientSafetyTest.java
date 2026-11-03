package busynotfree.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import busynotfree.exception.ClientException;

/** Checks that invalid client registration cannot change collections or ID counters. */
class ClientSafetyTest {
    private final ClientManager clients = new ClientManager();

    @Test
    public void addClient_exhaustedCounter_doesNotInsertOrOverflow() {
        clients.advanceNextClientNumber(Integer.MAX_VALUE - 1);
        Client last = clients.addClient("Last client");

        assertEquals("C2147483646", last.getClientId());
        assertEquals(Integer.MAX_VALUE, clients.getNextClientNumber());
        assertThrows(ClientException.class, () -> clients.addClient("Cannot allocate"));
        assertEquals(List.of(last), clients.getClients());
        assertEquals(Integer.MAX_VALUE, clients.getNextClientNumber());
    }

    @Test
    public void addClient_conflictingId_doesNotReplaceExistingClient() {
        Client existing = new Client("C001", "Existing client");
        existing.addNote("Keep this note");
        clients.registerRestoredClient(existing);

        assertThrows(ClientException.class, () -> clients.addClient("Replacement"));

        assertSame(existing, clients.getClient("C001"));
        assertEquals(List.of("Keep this note"), existing.getNotes());
        assertEquals(1, clients.getNextClientNumber());
    }

    @Test
    public void loadDeletedClient_invalidIds_preservesTrashcanAndCounter() {
        clients.addClient("Active client");
        clients.loadDeletedClient(new Client("C005", "Retained client"));
        List<String> invalidIds = List.of("Cabc", "P006", "c006", "C0", "C000", "C6", "C0006",
                "C+006", "C-006", "C2147483647", "C2147483648", "C999999999999999999999");

        for (String id : invalidIds) {
            assertThrows(ClientException.class, () -> clients.loadDeletedClient(new Client(id, "Invalid")));
            assertEquals(List.of("C005"), clients.getDeletedClients().stream().map(Client::getClientId).toList());
            assertEquals(6, clients.getNextClientNumber());
            assertEquals("Active client", clients.getClient("C001").getName());
        }
    }

    @Test
    public void loadDeletedClient_duplicateIds_preservesOriginalRecords() {
        Client active = clients.addClient("Active client");
        Client deleted = new Client("C005", "Retained client");
        deleted.addNote("Keep this note");
        clients.loadDeletedClient(deleted);

        assertThrows(ClientException.class, () -> clients.loadDeletedClient(new Client("C001", "Conflict")));
        assertThrows(ClientException.class, () -> clients.loadDeletedClient(new Client("C005", "Conflict")));

        assertSame(active, clients.getClient("C001"));
        assertEquals(1, clients.getDeletedClients().size());
        assertEquals("Retained client", clients.getDeletedClients().getFirst().getName());
        assertEquals(List.of("Keep this note"), clients.getDeletedClients().getFirst().getNotes());
        assertEquals(6, clients.getNextClientNumber());
    }

    @Test
    public void loadDeletedClient_validIds_advancesCounterWithoutReusingDeletedIds() {
        clients.loadDeletedClient(new Client("C1000", "Later client"));
        clients.loadDeletedClient(new Client("C999", "Earlier client"));

        assertEquals(1001, clients.getNextClientNumber());
        assertTrue(clients.getClients().isEmpty());
        assertEquals(List.of("C999", "C1000"),
                clients.getDeletedClients().stream().map(Client::getClientId).toList());
        assertEquals("C1001", clients.addClient("Next client").getClientId());
    }

    @Test
    public void loadDeletedClient_lastUsableId_keepsPositiveCounter() {
        clients.loadDeletedClient(new Client("C2147483646", "Last client"));

        assertEquals(Integer.MAX_VALUE, clients.getNextClientNumber());
        assertThrows(ClientException.class, () -> clients.addClient("Cannot allocate"));
        assertEquals(1, clients.getDeletedClients().size());
        assertTrue(clients.getClients().isEmpty());
    }

    @Test
    public void restoreClient_invalidId_preservesActiveClientsAndRetainedSnapshot() {
        Client active = clients.addClient("Active client");
        Client invalid = new Client("Cabc", "Invalid client");
        invalid.addNote("Retained note");
        // Simulate a corrupt retained record without going through the storage loader.
        clients.getTrashcanManager().storeClient(invalid);

        assertThrows(ClientException.class, () -> clients.getTrashcanManager().restoreClient("Cabc", clients));

        assertEquals(List.of(active), clients.getClients());
        assertSame(active, clients.getClient("C001"));
        Client retained = clients.getDeletedClients().getFirst();
        assertEquals("Cabc", retained.getClientId());
        assertEquals(List.of("Retained note"), retained.getNotes());
        assertEquals(2, clients.getNextClientNumber());
    }

    @Test
    public void registerRestoredClient_invalidIds_doesNotRegisterClient() {
        Client active = clients.addClient("Active client");
        for (String id : List.of("P002", "C2", "C0002", "C000", "C2147483647", "C2147483648")) {
            assertThrows(ClientException.class, () -> clients.registerRestoredClient(new Client(id, "Invalid")));
            assertEquals(List.of(active), clients.getClients());
            assertEquals(2, clients.getNextClientNumber());
        }
    }
}
