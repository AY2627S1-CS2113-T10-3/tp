package busynotfree;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Scanner;

import busynotfree.command.CommandHandler;
import busynotfree.command.CommandParser;
import busynotfree.model.ClientManager;
import busynotfree.model.ProjectManager;
import busynotfree.storage.DataStorage;
import busynotfree.ui.Ui;

/**
 * Runs the BusyNotFree command-line interface.
 */
public class BusyNotFree {
    private final Ui ui;
    private final CommandHandler commandHandler;
    private final SaveAction saveAction;

    /** Saves the current model before the CLI reports a successful exit. */
    @FunctionalInterface
    interface SaveAction {
        void save() throws IOException;
    }

    /** Creates a CLI using the supplied UI, command handler and save action. */
    BusyNotFree(Ui ui, CommandHandler commandHandler, SaveAction saveAction) {
        this.ui = ui;
        this.commandHandler = commandHandler;
        this.saveAction = saveAction;
    }

    /**
     * Starts the application and reads commands from standard input.
     *
     * @param args Command-line arguments, which are not used.
     */
    public static void main(String[] args) {
        Ui ui = new Ui();
        DataStorage storage = new DataStorage(Path.of("data", "busynotfree.properties"));
        DataStorage.LoadedData savedData;
        try {
            savedData = storage.load();
        } catch (IOException exception) {
            ui.showError("Unable to load saved data: " + exception.getMessage());
            return;
        }
        ClientManager clientManager = savedData.clientManager();
        ProjectManager projectManager = savedData.projectManager();
        CommandHandler commandHandler = new CommandHandler(new CommandParser(), ui, projectManager, clientManager);
        BusyNotFree busyNotFree = new BusyNotFree(ui, commandHandler,
                () -> storage.save(clientManager, projectManager));
        try (Scanner scanner = new Scanner(System.in)) {
            busyNotFree.run(scanner);
        }
    }

    /** Reads commands, allowing a save during the session or before exit. */
    void run(Scanner scanner) {
        ui.showWelcome();
        while (true) {
            if (!commandHandler.isAwaitingDeletionConfirmation()) {
                ui.showPrompt();
            }
            if (!scanner.hasNextLine()) {
                commandHandler.cancelPendingDeletion();
                saveAndReportExit();
                return;
            }
            String userInput = scanner.nextLine();
            String[] commandParts = userInput.trim().split("\\s+", 2);
            if (!commandHandler.isAwaitingDeletionConfirmation()
                    && commandParts[0].equals("save")) {
                ui.showSeparator();
                if (commandParts.length > 1) {
                    ui.showError("Use save without arguments.");
                } else if (saveData()) {
                    ui.showDataSaved();
                }
                ui.showSeparator();
                continue;
            }
            if (commandHandler.execute(userInput)) {
                if (saveAndReportExit()) {
                    return;
                }
            }
        }
    }

    /** Keeps the command loop running after a failed explicit save. */
    private boolean saveAndReportExit() {
        if (!saveData()) {
            return false;
        }
        ui.showGoodbye();
        return true;
    }

    /** Tries the shared storage action and reports an I/O failure without ending the session. */
    private boolean saveData() {
        try {
            saveAction.save();
            return true;
        } catch (IOException exception) {
            ui.showError("Critical ERROR!!! Unable to save data: " + exception.getMessage());
            return false;
        }
    }
}
