package busynotfree.ui;

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
                + "project.add, project.status, task.add, event.add, exit");
        showMessage("Data is kept in memory only and is not saved on exit.");
        showSeparator();
    }

    /**
     * Prompts the user for the next command.
     */
    public void showPrompt() {
        showMessage("Enter a command:");
    }

    /**
     * Displays a farewell without claiming that data has been saved.
     */
    public void showGoodbye() {
        showMessage("Goodbye!");
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

    /** Displays confirmation after a project's status is changed. */
    public void showProjectStatusChanged(Project project) {
        showMessage("Project " + project.getProjectId() + " status changed to " + project.getStatus() + ".");
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
