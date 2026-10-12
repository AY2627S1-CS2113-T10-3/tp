package busynotfree.ui;

import busynotfree.model.Client;

/**
 * Formats client responses through the application's shared user interface.
 */
public final class ClientUi {
    private final Ui ui;

    public ClientUi(Ui ui) {
        this.ui = ui;
    }

    /** Displays the new client's assigned ID and name. */
    public void showClientAdded(Client client) {
        ui.showMessage("Added client " + client.getClientId() + ": " + client.getName() + ".");
    }

    /** Confirms that a note was attached to its client. */
    public void showNoteAdded(Client client) {
        ui.showMessage("Note added to client " + client.getClientId() + ".");
    }

    /** Displays the expected syntax after a supported client command fails. */
    public void showCommandUsage(String action) {
        switch (action) {
        case "add":
            ui.showMessage("Format: client.add name/CLIENT_NAME");
            break;
        case "note":
            ui.showMessage("Format: client.note CLIENT_ID text/NOTE");
            break;
        default:
            break;
        }
    }

}
