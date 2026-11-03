package busynotfree.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import busynotfree.exception.ClientException;

/** Checks Unicode case variants and normalized literal name searches. */
class ClientSearchEdgeTest {
    private final ClientManager clients = new ClientManager();

    @Test
    public void findClients_greekSigmaVariants_matchesRegardlessOfPositionOrCase() {
        Client upper = clients.addClient("ΟΣ Studio");
        Client ordinary = clients.addClient("οσ Studio");
        Client terminal = clients.addClient("ος Studio");

        for (String query : List.of("ΟΣ", "οσ", "ος", "Σ", "σ", "ς")) {
            assertEquals(List.of(upper, ordinary, terminal), clients.findClients(query));
        }
    }

    @Test
    public void findClients_supplementaryAndTurkishLetters_matchesUnicodeCaseVariants() {
        Client deseret = clients.addClient("\uD801\uDC00 Studio");
        Client istanbul = clients.addClient("İSTANBUL Studio");

        assertEquals(List.of(deseret), clients.findClients("\uD801\uDC28"));
        assertEquals(List.of(istanbul), clients.findClients("istanbul"));
    }

    @Test
    public void findClients_regexMetacharacters_areLiteralAndResultsStayUnmodifiable() {
        Client client = clients.addClient("Acme [draft].* + UI/UX");
        clients.addClient("Unrelated client");

        for (String query : List.of("[draft].*", "[draft", ".*", "+", "ui/ux")) {
            List<Client> matches = clients.findClients(query);
            assertEquals(List.of(client), matches);
            assertThrows(UnsupportedOperationException.class, matches::clear);
        }
        assertTrue(clients.findClients("^Acme").isEmpty());
    }

    @Test
    public void findClients_blankAfterTrimming_rejectsSearchWithoutChangingClients() {
        Client client = clients.addClient("Acme");
        for (String query : new String[]{null, "", " \t ", "\u0000", "\u0000\u2003\u001f"}) {
            assertThrows(ClientException.class, () -> clients.findClients(query));
        }
        assertEquals(List.of(client), clients.getClients());
        assertEquals(2, clients.getNextClientNumber());
    }
}
