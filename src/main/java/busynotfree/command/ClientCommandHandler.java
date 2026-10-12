package busynotfree.command;

import java.util.Map;

import busynotfree.exception.CommandException;
import busynotfree.model.Client;
import busynotfree.model.ClientManager;
import busynotfree.ui.ClientUi;
import busynotfree.ui.Ui;

/**
 * Parses and executes client commands independently of project command routing.
 */
public final class ClientCommandHandler {
    private final ClientManager clientManager;
    private final ClientUi clientUi;
    private final ArgumentParser argumentParser = new ArgumentParser();

    public ClientCommandHandler(ClientManager clientManager, Ui ui) {
        this.clientManager = clientManager;
        this.clientUi = new ClientUi(ui);
    }

    /**
     * Executes a client operation, leaving error reporting to the shared handler.
     *
     * @param command Parsed command whose entity type is client.
     * @throws CommandException If the action or arguments are invalid.
     */
    public void execute(ParsedCommand command) {
        String arguments = command.getArgumentText();
        switch (command.getAction()) {
        case "add":
            handleAdd(arguments);
            break;
        case "note":
            handleNote(arguments);
            break;
        case "view":
            handleView(arguments);
            break;
        case "list":
            // The guide ignores extra arguments for commands that take no parameters.
            clientUi.showClients(clientManager.getClients());
            break;
        default:
            throw new CommandException("Unsupported action: " + command.getAction() + ".");
        }
    }

    /** Displays the syntax for a supported client action after an error. */
    public void showCommandUsage(String action) {
        clientUi.showCommandUsage(action);
    }

    /** Parses the name before creating a client, so invalid input cannot consume an ID. */
    private void handleAdd(String arguments) {
        Map<String, String> values = argumentParser.parse(arguments, "name");
        clientUi.showClientAdded(clientManager.addClient(values.get("name")));
    }

    /** Separates the positional ID from the note's prefixed free text. */
    private void handleNote(String arguments) {
        String[] parts = arguments.trim().split("\\s+", 2);
        String clientId = parts[0];
        if (clientId.isBlank() || clientId.contains("/")) {
            throw new CommandException("A client ID is required before text/.");
        }
        String noteArguments = parts.length > 1 ? parts[1] : "";
        Map<String, String> values = argumentParser.parse(noteArguments, "text");
        Client client = clientManager.addNote(clientId, values.get("text"));
        clientUi.showNoteAdded(client);
    }

    /** Requires exactly one positional ID for a client view. */
    private void handleView(String arguments) {
        if (arguments.isBlank()) {
            throw new CommandException("A client ID must not be blank.");
        }
        String[] parts = arguments.trim().split("\\s+");
        if (parts.length != 1) {
            throw new CommandException("Provide exactly one client ID.");
        }
        clientUi.showClient(clientManager.getClient(parts[0]));
    }
}
