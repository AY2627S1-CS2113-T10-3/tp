package busynotfree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import busynotfree.command.CommandHandler;
import busynotfree.command.CommandParser;
import busynotfree.model.ClientManager;
import busynotfree.model.ProjectManager;
import busynotfree.ui.Ui;

/**
 * Tests the client workflow and error recovery through the application's CLI loop.
 */
class BusyNotFreeClientTest {
    @Test
    public void run_clientWorkflow_recoversFromErrorsAndSharesClientsWithProjects() {
        RecordingUi ui = new RecordingUi();
        ClientManager clients = new ClientManager();
        ProjectManager projects = new ProjectManager(clients);
        BusyNotFree application = new BusyNotFree(ui,
                new CommandHandler(new CommandParser(), ui, projects, clients), () -> {
                });
        List<String> commands = List.of("client.list", "client.add name/", "client.add name/Acme Studio",
                "client.view C001", "client.note C001 text/", "client.note C999 text/Missing client",
                "client.note C001 text/Prefers weekly updates", "client.note C001 text/Follow up Friday",
                "client.view C001", "client.view", "client.view C999", "client.add name/Bright Ideas",
                "client.list", "project.add client/C001 name/Website due/2026-11-30", "exit",
                "client.add name/Must not be added");

        try (Scanner scanner = new Scanner(String.join("\r\n", commands) + "\r\n")) {
            application.run(scanner);
        }

        assertEquals(List.of("A value is required after name/.", "A value is required after text/.",
                "Client C999 does not exist.", "A client ID must not be blank.",
                "Client C999 does not exist."), ui.errors);
        assertEquals(2, clients.getClients().size());
        assertEquals(List.of("Prefers weekly updates", "Follow up Friday"), clients.getClient("C001").getNotes());
        assertEquals("C001", projects.getProject("P001").getClientId());
        assertTrue(ui.messages.contains("No clients found."));
        assertTrue(ui.messages.contains("Notes: No notes found."));
        assertTrue(ui.messages.contains("Notes: Prefers weekly updates"));
        assertTrue(ui.messages.contains("Notes: Follow up Friday"));
        assertTrue(ui.messages.contains("1. C001 | Acme Studio"));
        assertTrue(ui.messages.contains("2. C002 | Bright Ideas"));
        assertEquals(commands.size() - 1, ui.messages.stream().filter("Enter a command:"::equals).count());
        assertEquals("Data saved. Goodbye!", ui.messages.getLast());
    }

    /** Captures responses without replacing the process's standard output. */
    private static class RecordingUi extends Ui {
        private final List<String> messages = new ArrayList<>();
        private final List<String> errors = new ArrayList<>();

        @Override
        public void showMessage(String message) {
            messages.add(message);
        }

        @Override
        public void showError(String message) {
            errors.add(message);
        }

        @Override
        public void showSeparator() {
            // Decorative separators do not affect workflow assertions.
        }
    }
}
