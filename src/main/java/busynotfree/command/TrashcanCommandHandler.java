package busynotfree.command;

import busynotfree.exception.CommandException;
import busynotfree.model.Client;
import busynotfree.model.ClientManager;
import busynotfree.model.Project;
import busynotfree.model.ProjectManager;
import busynotfree.model.TrashcanManager;
import busynotfree.ui.Ui;

/** Parses and executes trashcan commands independently of other command routing. */
public final class TrashcanCommandHandler {
    private final TrashcanManager trashcanManager;
    private final ProjectManager projectManager;
    private final ClientManager clientManager;
    private final Ui ui;

    /** Creates a handler using the application's shared trashcan and active-data managers. */
    public TrashcanCommandHandler(TrashcanManager trashcanManager, ProjectManager projectManager,
            ClientManager clientManager, Ui ui) {
        this.trashcanManager = trashcanManager;
        this.projectManager = projectManager;
        this.clientManager = clientManager;
        this.ui = ui;
    }

    /**
     * Executes a trashcan operation, leaving error reporting to the shared handler.
     *
     * @param command Parsed trashcan command.
     * @throws CommandException If the action or arguments are invalid.
     */
    public void execute(ParsedCommand command) {
        assert "trashcan".equals(command.getEntityType()) : "Trashcan handler received a non-trashcan command.";
        String arguments = command.getArgumentText();
        switch (command.getAction()) {
        case "list":
            if (!arguments.isBlank()) {
                throw new CommandException("Use trashcan.list without arguments.");
            }
            ui.showTrashcan(trashcanManager.getDeletedProjects(), trashcanManager.getDeletedClients());
            break;
        case "restore":
            handleRestore(arguments);
            break;
        default:
            throw new CommandException("Unsupported action: " + command.getAction() + ".");
        }
    }

    /** Displays syntax for supported commands after an error. */
    public void showCommandUsage(String action) {
        switch (action) {
        case "empty":
            ui.showMessage("Format: trashcan.empty");
            break;
        case "list":
            ui.showMessage("Format: trashcan.list");
            break;
        case "restore":
            ui.showMessage("Format: trashcan.restore PROJECT_ID/CLIENT_ID");
            break;
        default:
            break;
        }
    }

    /** Requires exactly one project or client ID and restores its retained contents. */
    private void handleRestore(String arguments) {
        if (arguments.isBlank()) {
            throw new CommandException("A project ID or client ID must not be blank.");
        }
        if (arguments.trim().split("\\s+").length != 1) {
            throw new CommandException("Provide exactly one project ID or client ID.");
        }
        String id = arguments.trim();
        if (id.matches("C[0-9]{3,}")) {
            Client client = trashcanManager.restoreClient(id, clientManager);
            int notes = client.getNotes().size();
            ui.showMessage("Restored client " + client.getClientId() + " \"" + client.getName()
                    + "\" with " + notes + (notes == 1 ? " note." : " notes."));
            return;
        }
        if (!id.matches("P[0-9]{3,}")) {
            throw new CommandException("Provide a valid project ID or client ID, such as P001 or C001.");
        }
        Project project = trashcanManager.restoreProject(id, projectManager);
        int tasks = project.getTasks().size();
        int events = project.getEvents().size();
        ui.showMessage("Restored project " + project.getProjectId() + " \"" + project.getName()
                + "\" with " + tasks + (tasks == 1 ? " task and " : " tasks and ")
                + events + (events == 1 ? " event." : " events."));
    }
}
