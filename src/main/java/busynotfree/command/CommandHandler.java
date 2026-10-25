package busynotfree.command;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import busynotfree.exception.BusyNotFreeException;
import busynotfree.exception.CommandException;
import busynotfree.exception.ProjectException;
import busynotfree.model.Client;
import busynotfree.model.ClientManager;
import busynotfree.model.Event;
import busynotfree.model.Project;
import busynotfree.model.ProjectManager;
import busynotfree.model.ProjectStatus;
import busynotfree.model.Task;
import busynotfree.ui.Ui;

/**
 * Parses and routes BusyNotFree commands to the in-memory client and project managers.
 * Adapted from the MimiMeow individual project's command-handler structure.
 */
public class CommandHandler {
    /** Records project view diagnostics when fine logging is enabled. */
    private static final Logger LOGGER = Logger.getLogger(CommandHandler.class.getName());

    private final CommandParser commandParser;
    private final Ui ui;
    private final ProjectManager projectManager;
    private final ClientManager clientManager;
    private final ClientCommandHandler clientCommandHandler;
    private final ArgumentParser argumentParser = new ArgumentParser();

    /**
     * Creates a handler with the supplied parser and user interface.
     * Uses an independent client manager; use the overload for shared client lookup.
     *
     * @param commandParser Parser for the entity type, action and argument text.
     * @param ui User interface for responses.
     * @param projectManager Manager for projects and their scheduled items.
     */
    public CommandHandler(CommandParser commandParser, Ui ui, ProjectManager projectManager) {
        this(commandParser, ui, projectManager, new ClientManager());
    }

    /**
     * Creates a handler using the client manager shared with project creation.
     *
     * @param commandParser Parser for the entity type, action and argument text.
     * @param ui User interface for responses.
     * @param projectManager Manager for projects and their scheduled items.
     * @param clientManager Client manager also supplied to the project manager as its lookup.
     */
    public CommandHandler(CommandParser commandParser, Ui ui, ProjectManager projectManager,
            ClientManager clientManager) {
        this.commandParser = commandParser;
        this.ui = ui;
        this.projectManager = projectManager;
        this.clientManager = clientManager;
        this.clientCommandHandler = new ClientCommandHandler(clientManager, ui);
    }

    /**
     * Parses and routes one command, displaying errors without stopping the caller.
     * Failed client, add, edit, task mark, project status, project filtering, project view,
     * project delete and project deadline commands display usage hints.
     *
     * @param userInput Non-null raw user input.
     * @return True if the user enters exit; false otherwise.
     */
    public boolean execute(String userInput) {
        ui.showSeparator();
        boolean shouldExit = false;
        ParsedCommand command = null;
        try {
            if (userInput.trim().split("\\s+", 2)[0].equals("exit")) {
                shouldExit = true;
            } else if (userInput.trim().equals("help")) {
                ui.showHelp();
            } else {
                command = commandParser.parse(userInput);
                routeCommand(command);
            }
        } catch (BusyNotFreeException exception) {
            ui.showError(exception.getMessage());
            if (command != null) {
                showCommandUsage(command);
            }
        }
        ui.showSeparator();
        return shouldExit;
    }

    /** Selects a usage hint without changing the error handling for other targets. */
    private void showCommandUsage(ParsedCommand command) {
        if (command.getEntityType().equals("client")) {
            clientCommandHandler.showCommandUsage(command.getAction());
        } else if (command.getEntityType().equals("project") && command.getAction().equals("status")) {
            ui.showProjectStatusUsage();
        } else if (command.getEntityType().equals("project") && command.getAction().equals("delete")) {
            ui.showProjectDeleteUsage();
        } else if (command.getEntityType().equals("project") && command.getAction().equals("view")) {
            ui.showProjectViewUsage();
        } else if (command.getEntityType().equals("project") && command.getAction().equals("filter")) {
            ui.showProjectFilterUsage();
        } else if (command.getEntityType().equals("project") && command.getAction().equals("deadlines")) {
            ui.showProjectDeadlinesUsage();
        } else if (command.getEntityType().equals("project") && command.getAction().equals("overdue")) {
            ui.showProjectOverdueUsage();
        } else if (command.getEntityType().equals("task") && command.getAction().equals("mark")) {
            ui.showTaskMarkUsage();
        } else if (command.getEntityType().equals("task") && command.getAction().equals("unmark")) {
            ui.showTaskUnmarkUsage();
        } else if (command.getEntityType().equals("task") && command.getAction().equals("edit")) {
            ui.showTaskEditUsage();
        } else if (command.getEntityType().equals("event") && command.getAction().equals("edit")) {
            ui.showEventEditUsage();
        } else if (command.getAction().equals("add")) {
            ui.showAddCommandUsage(command.getEntityType());
        }
    }

    /** Delegates client operations and routes project, task and event commands. */
    private void routeCommand(ParsedCommand command) {
        if (command.getEntityType().equals("client")) {
            clientCommandHandler.execute(command);
            return;
        }
        if (command.getEntityType().equals("project") && command.getAction().equals("status")) {
            handleProjectStatus(command.getArgumentText());
            return;
        }
        if (command.getEntityType().equals("project") && command.getAction().equals("list")) {
            handleProjectList();
            return;
        }
        if (command.getEntityType().equals("project") && command.getAction().equals("filter")) {
            handleProjectFilter(command.getArgumentText());
            return;
        }
        if (command.getEntityType().equals("project") && command.getAction().equals("deadlines")) {
            handleProjectDeadlines(command.getArgumentText());
            return;
        }
        if (command.getEntityType().equals("project") && command.getAction().equals("overdue")) {
            handleProjectOverdue(command.getArgumentText());
            return;
        }
        if (command.getEntityType().equals("project") && command.getAction().equals("view")) {
            handleProjectView(command.getArgumentText());
            return;
        }
        if (command.getEntityType().equals("project") && command.getAction().equals("delete")) {
            handleProjectDelete(command.getArgumentText());
            return;
        }
        if (command.getEntityType().equals("task") && command.getAction().equals("mark")) {
            handleTaskMark(command.getArgumentText());
            return;
        }
        if (command.getEntityType().equals("task") && command.getAction().equals("unmark")) {
            handleTaskUnmark(command.getArgumentText());
            return;
        }
        if (command.getEntityType().equals("task") && command.getAction().equals("edit")) {
            handleTaskEdit(command.getArgumentText());
            return;
        }
        if (command.getEntityType().equals("event") && command.getAction().equals("edit")) {
            handleEventEdit(command.getArgumentText());
            return;
        }
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

    /** Parses project arguments and reports the newly created project with its client's name. */
    private void handleProjectAdd(String arguments) {
        Map<String, String> values = argumentParser.parse(arguments, "client", "name", "due");
        Project project = projectManager.addProject(values.get("client"), values.get("name"),
                argumentParser.parseDate(values.get("due")));
        if (clientManager.exists(project.getClientId())) {
            ui.showProjectAdded(project, clientManager.getClient(project.getClientId()));
        } else {
            // Reachable only when the project manager checks clients through a different lookup,
            // as with the three-argument constructor; the project is already created, so do not fail here.
            ui.showProjectAdded(project);
        }
    }

    /** Parses task arguments and reports the task added to its owning project. */
    private void handleTaskAdd(String arguments) {
        Map<String, String> values = argumentParser.parse(arguments, "project", "name", "date");
        Task task = projectManager.addTask(values.get("project"), values.get("name"),
                argumentParser.parseDate(values.get("date")));
        ui.showTaskAdded(task);
    }

    /** Requires one task ID before marking the matching task complete. */
    private void handleTaskMark(String arguments) {
        if (arguments.isBlank() || arguments.trim().split("\\s+").length != 1) {
            throw new CommandException("Provide exactly one task ID.");
        }
        Task task = projectManager.markTaskComplete(arguments.trim());
        ui.showTaskMarked(task);
    }

    /** Requires one task ID before reopening the matching task. */
    private void handleTaskUnmark(String arguments) {
        if (arguments.isBlank() || arguments.trim().split("\\s+").length != 1) {
            throw new CommandException("Provide exactly one task ID.");
        }
        Task task = projectManager.markTaskIncomplete(arguments.trim());
        ui.showTaskUnmarked(task);
    }

    /** Parses every argument before editing, so invalid input cannot partially change the task. */
    private void handleTaskEdit(String arguments) {
        EditArguments values = parseEditArguments(arguments, "task");
        Task task = projectManager.editTask(values.itemId(), values.description(), values.date());
        ui.showTaskEdited(task);
    }

    /** Parses every argument before editing, so invalid input cannot partially change the event. */
    private void handleEventEdit(String arguments) {
        EditArguments values = parseEditArguments(arguments, "event");
        Event event = projectManager.editEvent(values.itemId(), values.description(), values.date());
        ui.showEventEdited(event);
    }

    /**
     * Separates the positional item ID from the name/ and date/ arguments of an edit command.
     * Other recognized prefixes, such as project/, are rejected so an edit cannot move an item.
     *
     * @param arguments Argument text after task.edit or event.edit.
     * @param itemType Task or event, used in the error message.
     * @return The item ID, new description and validated new date.
     * @throws CommandException If the ID is missing, an argument is invalid or the date is malformed.
     */
    private EditArguments parseEditArguments(String arguments, String itemType) {
        String[] parts = arguments.trim().split("\\s+", 2);
        String itemId = parts[0];
        if (itemId.isBlank() || itemId.contains("/")) {
            LOGGER.log(Level.FINE, "{0}.edit rejected because no item ID was given", itemType);
            throw new CommandException("Provide the " + itemType + " ID before name/ and date/.");
        }
        String prefixedArguments = parts.length > 1 ? parts[1] : "";
        Map<String, String> values = argumentParser.parse(prefixedArguments, "name", "date");
        LocalDate date = argumentParser.parseDate(values.get("date"));
        return new EditArguments(itemId, values.get("name"), date);
    }

    /** Holds the validated values of an edit command before the model is changed. */
    private record EditArguments(String itemId, String description, LocalDate date) {
    }

    /** Parses event arguments and reports the event added to its owning project. */
    private void handleEventAdd(String arguments) {
        Map<String, String> values = argumentParser.parse(arguments, "project", "name", "date");
        Event event = projectManager.addEvent(values.get("project"), values.get("name"),
                argumentParser.parseDate(values.get("date")));
        ui.showEventAdded(event);
    }

    /** Parses a project ID and status before changing the project's status. */
    private void handleProjectStatus(String arguments) {
        String[] parts = arguments.trim().split("\\s+");
        if (parts.length != 2) {
            throw new CommandException("Use project.status PROJECT_ID STATUS.");
        }
        assert parts.length == 2;
        ProjectStatus status = parseProjectStatus(parts[1]);
        assert status != null;
        Project project = projectManager.changeProjectStatus(parts[0], status);
        assert project.getStatus() == status;
        ui.showProjectStatusChanged(project);
    }

    /** Requires one positional project ID before deleting the project and its activities. */
    private void handleProjectDelete(String arguments) {
        if (arguments.isBlank()) {
            throw new CommandException("A project ID must not be blank.");
        }
        String[] parts = arguments.trim().split("\\s+");
        if (parts.length != 1) {
            throw new CommandException("Provide exactly one project ID.");
        }

        Project project = projectManager.getProject(parts[0]);
        int removedCount = projectManager.deleteProject(parts[0]);
        ui.showProjectDeleted(project.getName(), removedCount);
    }

    /** Displays all projects in their creation order and ignores extra arguments. */
    private void handleProjectList() {
        List<Project> projects = projectManager.getProjects();
        assert projects != null;
        assert projects.stream().allMatch(project -> project != null);
        ui.showProjects(projects);
    }

    /** Filters projects by one status without changing the existing project list command. */
    private void handleProjectFilter(String arguments) {
        String[] parts = arguments.trim().split("\\s+");
        if (arguments.isBlank() || parts.length != 1) {
            throw new CommandException("Use project.filter STATUS.");
        }
        ProjectStatus status = parseProjectStatus(parts[0]);
        List<Project> projects = projectManager.getProjectsByStatus(status);
        ui.showProjects(projects);
    }

    /** Displays all projects from the earliest deadline to the latest deadline. */
    private void handleProjectDeadlines(String arguments) {
        requireNoArguments(arguments, "Use project.deadlines.");
        ui.showProjects(projectManager.getProjectsByDeadline());
    }

    /** Displays active projects whose deadlines have passed as of the current date. */
    private void handleProjectOverdue(String arguments) {
        requireNoArguments(arguments, "Use project.overdue.");
        List<Project> projects = projectManager.getOverdueActiveProjects(LocalDate.now());
        ui.showProjects(projects, "No overdue projects found.");
    }

    /** Rejects unexpected arguments for commands that do not accept arguments. */
    private void requireNoArguments(String arguments, String usageMessage) {
        if (!arguments.isBlank()) {
            throw new CommandException(usageMessage);
        }
    }

    /** Displays one project or every project with its client and scheduled items. */
    private void handleProjectView(String arguments) {
        String projectId = arguments.trim();
        if (projectId.isEmpty() || projectId.equals("all")) {
            List<Project> projects = projectManager.getProjects();
            LOGGER.log(Level.FINE, "project.view requested all projects; count: {0}", projects.size());
            if (projects.isEmpty()) {
                ui.showMessage("No projects found.");
                return;
            }
            boolean hasPrintedAnyProject = false;
            for (Project project : projects) {
                if (hasPrintedAnyProject) {
                    ui.showMessage("");
                }
                showProjectSummary(project);
                hasPrintedAnyProject = true;
            }
            return;
        }
        if (projectId.split("\\s+").length != 1) {
            LOGGER.fine("project.view rejected multiple project IDs");
            throw new CommandException("Provide exactly one project ID, or use all.");
        }
        LOGGER.log(Level.FINE, "project.view requested project {0}", projectId);
        try {
            Project project = projectManager.getProject(projectId);
            assert projectId.equals(project.getProjectId()) : "Project lookup returned a different project.";
            showProjectSummary(project);
        } catch (ProjectException exception) {
            LOGGER.log(Level.FINE, "project.view could not find project {0}", projectId);
            throw new ProjectException("Project " + projectId + " was not found.");
        }
    }

    /** Resolves the project's client and checks lookup consistency before formatting its summary. */
    private void showProjectSummary(Project project) {
        Client client = clientManager.getClient(project.getClientId());
        assert project.getClientId().equals(client.getClientId()) : "Client lookup returned a different client.";
        ui.showProjectSummary(project, client);
    }

    /** Converts a status value into its project status without considering capitalisation. */
    private ProjectStatus parseProjectStatus(String status) {
        switch (status.toLowerCase(Locale.ROOT)) {
        case "active":
            return ProjectStatus.ACTIVE;
        case "paused":
            return ProjectStatus.PAUSED;
        case "completed":
            return ProjectStatus.COMPLETED;
        case "cancelled":
            return ProjectStatus.CANCELLED;
        default:
            throw new CommandException("Invalid Project Status.");
        }
    }
}
