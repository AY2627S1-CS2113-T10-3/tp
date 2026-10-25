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

    /** Reads commands and saves before reporting a successful exit. */
    void run(Scanner scanner) {
        ui.showWelcome();
        while (true) {
            ui.showPrompt();
            if (!scanner.hasNextLine()) {
                saveAndReportExit();
                return;
            }
            String userInput = scanner.nextLine();
            if (commandHandler.execute(userInput)) {
                if (saveAndReportExit()) {
                    return;
                }
            }
        }
    }

    /** Keeps the command loop running after a failed explicit save. */
    private boolean saveAndReportExit() {
        try {
            saveAction.save();
            ui.showGoodbye();
            return true;
        } catch (IOException exception) {
            ui.showError("Critical ERROR!!! Unable to save data: " + exception.getMessage());
            return false;
        }
    }
}
