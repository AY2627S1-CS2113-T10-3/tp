package busynotfree.command;

import java.util.Map;

import busynotfree.exception.CommandException;
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

}
