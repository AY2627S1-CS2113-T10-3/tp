package busynotfree.command;

import busynotfree.exception.CommandException;
import busynotfree.model.Project;
import busynotfree.model.ProjectManager;
import busynotfree.model.TrashcanManager;
import busynotfree.ui.Ui;

/** Parses and executes trashcan commands independently of other command routing. */
public final class TrashcanCommandHandler {
    private final TrashcanManager trashcanManager;
    private final ProjectManager projectManager;
    private final Ui ui;

    /** Creates a handler using the application's shared trashcan and project manager. */
    public TrashcanCommandHandler(TrashcanManager trashcanManager, ProjectManager projectManager, Ui ui) {
        this.trashcanManager = trashcanManager;
        this.projectManager = projectManager;
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
            ui.showMessage("Format: trashcan.restore PROJECT_ID");
            break;
        default:
            break;
        }
    }

    /** Requires one project ID before attempting restoration. */
    private void handleRestore(String arguments) {
        if (arguments.isBlank()) {
            throw new CommandException("A project ID must not be blank.");
        }
        if (arguments.trim().split("\\s+").length != 1) {
            throw new CommandException("Provide exactly one project ID.");
        }
        Project project = trashcanManager.restoreProject(arguments.trim(), projectManager);
        int tasks = project.getTasks().size();
        int events = project.getEvents().size();
        ui.showMessage("Restored project " + project.getProjectId() + " \"" + project.getName()
                + "\" with " + tasks + (tasks == 1 ? " task and " : " tasks and ")
                + events + (events == 1 ? " event." : " events."));
    }
}
