package busynotfree;

import java.util.Scanner;

import busynotfree.command.CommandHandler;
import busynotfree.command.CommandParser;
import busynotfree.model.ClientManager;
import busynotfree.model.ProjectManager;
import busynotfree.ui.Ui;

/**
 * Runs the BusyNotFree command-line interface.
 */
public class BusyNotFree {
    private final Ui ui;
    private final CommandHandler commandHandler;

    /** Creates a CLI using the supplied user interface and command handler. */
    BusyNotFree(Ui ui, CommandHandler commandHandler) {
        this.ui = ui;
        this.commandHandler = commandHandler;
    }

    /**
     * Starts the application and reads commands from standard input.
     *
     * @param args command-line arguments, which are not used
     */
    public static void main(String[] args) {
        Ui ui = new Ui();
        ClientManager clientManager = new ClientManager();
        ProjectManager projectManager = new ProjectManager(clientManager);
        CommandHandler commandHandler = new CommandHandler(new CommandParser(), ui, projectManager, clientManager);
        BusyNotFree busyNotFree = new BusyNotFree(ui, commandHandler);
        try (Scanner scanner = new Scanner(System.in)) {
            busyNotFree.run(scanner);
        }
    }

    /** Reads and dispatches commands until exit is requested or input ends. */
    void run(Scanner scanner) {
        ui.showWelcome();
        while (true) {
            ui.showPrompt();
            if (!scanner.hasNextLine()) {
                break;
            }
            String userInput = scanner.nextLine();
            if (commandHandler.execute(userInput)) {
                break;
            }
        }
        ui.showGoodbye();
    }
}
