package busynotfree.command;

import java.util.Map;

import busynotfree.exception.BusyNotFreeException;
import busynotfree.exception.CommandException;
import busynotfree.model.Event;
import busynotfree.model.Project;
import busynotfree.model.ProjectManager;
import busynotfree.model.Task;
import busynotfree.ui.Ui;

/**
 * Parses and routes BusyNotFree creation commands to the in-memory project manager.
 * Adapted from the MimiMeow individual project's command-handler structure.
 */
public class CommandHandler {
    private final CommandParser commandParser;
    private final Ui ui;
    private final ProjectManager projectManager;
    private final ArgumentParser argumentParser = new ArgumentParser();

    /**
     * Creates a handler with the supplied parser and user interface.
     *
     * @param commandParser parser for the entity type, action and argument text
     * @param ui user interface for responses
     * @param projectManager manager for projects and their scheduled items
     */
    public CommandHandler(CommandParser commandParser, Ui ui, ProjectManager projectManager) {
        this.commandParser = commandParser;
        this.ui = ui;
        this.projectManager = projectManager;
    }

    /**
     * Parses and routes one command, displaying errors without stopping the caller.
     * Failed add commands also display their expected argument format.
     *
     * @param userInput non-null raw user input
     * @return true if the user enters exit; false otherwise
     */
    public boolean execute(String userInput) {
        ui.showSeparator();
        boolean shouldExit = false;
        ParsedCommand command = null;
        try {
            if (userInput.trim().equals("exit")) {
                shouldExit = true;
            } else {
                command = commandParser.parse(userInput);
                routeCommand(command);
            }
        } catch (BusyNotFreeException exception) {
            ui.showError(exception.getMessage());
            if (command != null && command.getAction().equals("add")) {
                ui.showAddCommandUsage(command.getEntityType());
            }
        }
        ui.showSeparator();
        return shouldExit;
    }

    /** Routes the supported add action to its target-specific handler. */
    private void routeCommand(ParsedCommand command) {
        if (!command.getAction().equals("add")) {
            throw new CommandException("Unsupported action: " + command.getAction() + ".");
        }
        String argumentText = command.getArgumentText();
        switch (command.getEntityType()) {
        case "project":
            handleProjectAdd(argumentText);
            break;
        case "task":
            handleTaskAdd(argumentText);
            break;
        case "event":
            handleEventAdd(argumentText);
            break;
        default:
            throw new CommandException("Unsupported target: " + command.getEntityType() + ".");
        }
    }

    /** Parses project arguments and reports the newly created project. */
    private void handleProjectAdd(String arguments) {
        Map<String, String> values = argumentParser.parse(arguments, "client", "name", "due");
        Project project = projectManager.addProject(values.get("client"), values.get("name"),
                argumentParser.parseDate(values.get("due")));
        ui.showProjectAdded(project);
    }

    /** Parses task arguments and reports the task added to its owning project. */
    private void handleTaskAdd(String arguments) {
        Map<String, String> values = argumentParser.parse(arguments, "project", "name", "date");
        Task task = projectManager.addTask(values.get("project"), values.get("name"),
                argumentParser.parseDate(values.get("date")));
        ui.showTaskAdded(task);
    }

    /** Parses event arguments and reports the event added to its owning project. */
    private void handleEventAdd(String arguments) {
        Map<String, String> values = argumentParser.parse(arguments, "project", "name", "date");
        Event event = projectManager.addEvent(values.get("project"), values.get("name"),
                argumentParser.parseDate(values.get("date")));
        ui.showEventAdded(event);
    }
}
