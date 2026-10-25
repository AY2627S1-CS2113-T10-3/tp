package busynotfree.ui;

import java.util.List;

import busynotfree.model.Client;
import busynotfree.model.Event;
import busynotfree.model.Project;
import busynotfree.model.Task;

/**
 * Displays command responses and errors for BusyNotFree.
 */
public class Ui {
    /**
     * Displays the application name and currently supported commands.
     */
    public void showWelcome() {
        showSeparator();
        showMessage("Welcome to BusyNotFree!");
        showMessage("Commands: client.add, client.note, client.view, client.list, "
                + "project.add, project.status, project.list, project.filter, project.deadlines, "
                + "project.overdue, project.view, project.delete, "
                + "task.add, event.add, help, exit");
        showSeparator();
    }

    /** Displays command names and descriptions with aligned columns. */
    public void showHelp() {
        showMessage("Available commands:");
        showMessage("client.add     |   Add a client");
        showMessage("client.note    |   Add a client note");
        showMessage("client.view    |   View a client");
        showMessage("project.add    |   Add a project");
        showMessage("project.delete |   Delete a project");
        showMessage("project.status |   Update a project status");
        showMessage("project.list   |   List all projects");
        showMessage("project.filter |   Filter projects by status");
        showMessage("project.deadlines |   List projects by deadline");
        showMessage("project.overdue |   List overdue active projects");
        showMessage("project.view   |   View a project summary");
        showMessage("task.add       |   Add a task");
        showMessage("event.add      |   Add an event");
        showMessage("help           |   View help");
        showMessage("exit           |   Save data and exit");
    }

    /**
     * Prompts the user for the next command.
     */
    public void showPrompt() {
        showMessage("Enter a command:");
    }

    /**
     * Confirms that application data was saved before exiting.
     */
    public void showGoodbye() {
        showMessage("Data saved. Goodbye!");
    }

    /**
     * Displays confirmation after a project is successfully registered.
     *
     * @param project project that was created
     */
    public void showProjectAdded(Project project) {
        showMessage("Created project " + project.getProjectId() + " for client " + project.getClientId() + ".");
    }

    /**
     * Displays confirmation after a task is successfully attached to its project.
     *
     * @param task task that was added
     */
    public void showTaskAdded(Task task) {
        showMessage("Added task " + task.getId() + " to project " + task.getProject().getProjectId() + ".");
    }

    /**
     * Displays confirmation after an event is successfully attached to its project.
     *
     * @param event event that was added
     */
    public void showEventAdded(Event event) {
        showMessage("Added event " + event.getId() + " to project " + event.getProject().getProjectId() + ".");
    }

    /**
     * Displays the deleted project's name and the number of activities removed.
     *
     * @param projectName name of the deleted project
     * @param removedCount total number of tasks and events removed
     */
    public void showProjectDeleted(String projectName, int removedCount) {
        assert removedCount >= 0 : "Removed activity count must not be negative";

        showMessage(projectName + " has been deleted");
        showMessage(removedCount + " associated "
                + (removedCount == 1 ? "activity" : "activities") + " removed.");
    }

    /** Displays the expected format for deleting a project. */
    public void showProjectDeleteUsage() {
        showMessage("Format: project.delete PROJECT_ID");
    }

    /** Displays confirmation after a project's status is changed. */
    public void showProjectStatusChanged(Project project) {
        assert project != null;
        showMessage("Project " + project.getProjectId() + " status changed to " + project.getStatus() + ".");
    }

    /** Displays projects in creation order, or a message when the list is empty. */
    public void showProjects(List<Project> projects) {
        showProjects(projects, "No projects found.");
    }

    /** Displays projects using a caller-supplied empty-result message. */
    public void showProjects(List<Project> projects, String emptyMessage) {
        assert projects != null;
        assert emptyMessage != null;
        if (projects.isEmpty()) {
            showMessage(emptyMessage);
            return;
        }
        for (int i = 0; i < projects.size(); i++) {
            Project project = projects.get(i);
            showMessage((i + 1) + ". " + project.getProjectId() + "|" + project.getName()
                    + "|" + project.getStatus() + "|Due " + project.getDeadline());
        }
    }

    /** Displays one project and separates its tasks from its events. */
    public void showProjectSummary(Project project, Client client) {
        showMessage("Project: " + project.getProjectId() + " - " + project.getName());
        showMessage("Client: " + client.getClientId() + " - " + client.getName());
        showMessage("Status: " + project.getStatus());
        showMessage("Deadline: " + project.getDeadline());
        showMessage("Tasks:");
        if (project.getTasks().isEmpty()) {
            showMessage("No tasks found.");
        } else {
            for (Task task : project.getTasks()) {
                showMessage(task.toString());
            }
        }
        showMessage("Events:");
        if (project.getEvents().isEmpty()) {
            showMessage("No events found.");
        } else {
            for (Event event : project.getEvents()) {
                showMessage(event.toString());
            }
        }
    }

    /** Displays the accepted forms of the project view command. */
    public void showProjectViewUsage() {
        showMessage("Format: project.view [PROJECT_ID|all]");
    }

    /**
     * Displays the expected format for a supported add command after an error.
     * Unrecognised entity types have no add-command hint.
     *
     * @param entityType project, task or event
     */
    public void showAddCommandUsage(String entityType) {
        switch (entityType) {
        case "project":
            showMessage("Format: project.add client/CLIENT_ID name/PROJECT_NAME due/YYYY-MM-DD");
            break;
        case "task":
            showMessage("Format: task.add project/PROJECT_ID name/DESCRIPTION date/YYYY-MM-DD");
            break;
        case "event":
            showMessage("Format: event.add project/PROJECT_ID name/DESCRIPTION date/YYYY-MM-DD");
            break;
        default:
            break;
        }
    }

    /** Displays the expected format for changing a project's status. */
    public void showProjectStatusUsage() {
        showMessage("Format: project.status PROJECT_ID STATUS");
    }

    /** Displays the expected format for filtering projects by status. */
    public void showProjectFilterUsage() {
        showMessage("Format: project.filter STATUS");
    }

    /** Displays the expected format for listing projects by deadline. */
    public void showProjectDeadlinesUsage() {
        showMessage("Format: project.deadlines");
    }

    /** Displays the expected format for listing overdue projects. */
    public void showProjectOverdueUsage() {
        showMessage("Format: project.overdue");
    }

    /**
     * Displays a separator between command responses.
     */
    public void showSeparator() {
        System.out.println("-".repeat(60));
    }

    /**
     * Displays a command response.
     *
     * @param message response to display
     */
    public void showMessage(String message) {
        System.out.println(message);
    }

    /**
     * Displays a user-facing error.
     *
     * @param message error to display
     */
    public void showError(String message) {
        System.out.println("Error: " + message);
    }
}
