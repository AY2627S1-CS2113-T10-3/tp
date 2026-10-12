package busynotfree.ui;

import java.util.List;

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

    /** Displays the client's details and every note in insertion order. */
    public void showClient(Client client) {
        ui.showMessage(formatClient(client));
        List<String> notes = client.getNotes();
        if (notes.isEmpty()) {
            ui.showMessage("Notes: No notes found.");
            return;
        }
        for (String note : notes) {
            ui.showMessage("Notes: " + note);
        }
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
        case "view":
            ui.showMessage("Format: client.view CLIENT_ID");
            break;
        default:
            break;
        }
    }

    private String formatClient(Client client) {
        return client.getClientId() + " | " + client.getName();
    }
}
