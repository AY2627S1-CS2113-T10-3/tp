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
        showMessage("Commands: client.add, client.note, client.view, client.list, client.find, client.edit, "
                + "project.add, project.status, project.list, project.filter, project.deadlines, "
                + "project.overdue, project.view, project.delete, "
                + "task.add, task.mark, task.unmark, task.edit, event.add, event.edit, help, exit");
        showSeparator();
    }

    /** Displays command names and descriptions with aligned columns. */
    public void showHelp() {
        showMessage("Available commands:");
        showMessage("client.add     |   Add a client");
        showMessage("client.note    |   Add a client note");
        showMessage("client.view    |   View a client");
        showMessage("client.find    |   Find clients by name");
        showMessage("client.edit    |   Edit a client's name");
        showMessage("project.add    |   Add a project");
        showMessage("project.delete |   Delete a project");
        showMessage("project.status |   Update a project status");
        showMessage("project.list   |   List all projects");
        showMessage("project.filter |   Filter projects by status");
        showMessage("project.deadlines |   List projects by deadline");
        showMessage("project.overdue |   List overdue active projects");
        showMessage("project.view   |   View a project summary");
        showMessage("task.add       |   Add a task");
        showMessage("task.mark      |   Mark a task complete");
        showMessage("task.unmark    |   Reopen a task");
        showMessage("task.edit      |   Edit a task");
        showMessage("event.add      |   Add an event");
        showMessage("event.edit     |   Edit an event");
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
     * Displays confirmation after a project is successfully registered, naming its client
     * so the user can confirm the right client ID was used.
     *
     * @param project Project that was created.
     * @param client Client who owns the project.
     */
    public void showProjectAdded(Project project, Client client) {
        showMessage("Created project " + describeProject(project) + " for client " + client.getClientId()
                + " \"" + client.getName() + "\".");
    }

    /**
     * Displays confirmation after a project is successfully registered,
     * when its client's name is unavailable.
     *
     * @param project Project that was created.
     */
    public void showProjectAdded(Project project) {
        showMessage("Created project " + describeProject(project) + " for client "
                + project.getClientId() + ".");
    }

    /**
     * Displays confirmation after a task is successfully attached to its project.
     *
     * @param task Task that was added.
     */
    public void showTaskAdded(Task task) {
        showMessage("Added task " + task.getId() + " \"" + task.getDescription() + "\" to project "
                + describeProject(task.getProject()) + ".");
    }

    /** Confirms that a task was marked complete. */
    public void showTaskMarked(Task task) {
        showMessage("Task " + task.getId() + " marked as completed.");
    }

    /** Displays the expected format for marking a task complete. */
    public void showTaskMarkUsage() {
        showMessage("Format: task.mark TASK_ID");
    }

    /** Confirms that a task was reopened. */
    public void showTaskUnmarked(Task task) {
        showMessage("Task " + task.getId() + " reopened. Status: [In Progress]");
    }

    /** Displays the expected format for reopening a task. */
    public void showTaskUnmarkUsage() {
        showMessage("Format: task.unmark TASK_ID");
    }

    /**
     * Displays the edited task's updated details, including its completion state.
     *
     * @param task Task that was edited.
     */
    public void showTaskEdited(Task task) {
        showMessage("Updated task in project " + describeProject(task.getProject()) + ": " + task);
    }

    /** Displays the expected format for editing a task. */
    public void showTaskEditUsage() {
        showMessage("Format: task.edit TASK_ID name/DESCRIPTION date/YYYY-MM-DD");
    }

    /**
     * Displays confirmation after an event is successfully attached to its project.
     *
     * @param event Event that was added.
     */
    public void showEventAdded(Event event) {
        showMessage("Added event " + event.getId() + " \"" + event.getDescription() + "\" to project "
                + describeProject(event.getProject()) + ".");
    }

    /**
     * Displays the edited event's updated details.
     *
     * @param event Event that was edited.
     */
    public void showEventEdited(Event event) {
        showMessage("Updated event in project " + describeProject(event.getProject()) + ": " + event);
    }

    /** Displays the expected format for editing an event. */
    public void showEventEditUsage() {
        showMessage("Format: event.edit EVENT_ID name/DESCRIPTION date/YYYY-MM-DD");
    }

    /** Formats a project as its ID followed by its quoted name, such as P001 "Website redesign". */
    private String describeProject(Project project) {
        return project.getProjectId() + " \"" + project.getName() + "\"";
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
        List<Task> tasks = project.getTasks();
        int completedTasks = 0;
        if (tasks.isEmpty()) {
            showMessage("No tasks found.");
        } else {
            for (Task task : tasks) {
                showMessage(task.toString());
                if (task.isCompleted()) {
                    completedTasks++;
                }
            }
        }
        String taskLabel = tasks.size() == 1 ? "task" : "tasks";
        showMessage("Progress: " + completedTasks + " of " + tasks.size() + " " + taskLabel + " completed.");
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
     * Unrecognized entity types have no add-command hint.
     *
     * @param entityType Project, task or event.
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
     * @param message Response to display.
     */
    public void showMessage(String message) {
        System.out.println(message);
    }

    /**
     * Displays a user-facing error.
     *
     * @param message Error to display.
     */
    public void showError(String message) {
        System.out.println("Error: " + message);
    }
}
