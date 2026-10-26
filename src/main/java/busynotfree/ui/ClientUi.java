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

    /** Confirms the previous and updated names alongside the client's unchanged ID. */
    public void showClientEdited(Client client, String previousName) {
        ui.showMessage("Updated client " + client.getClientId() + " from \"" + previousName
                + "\" to \"" + client.getName() + "\".");
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
        for (int i = 0; i < notes.size(); i++) {
            ui.showMessage("Note " + (i + 1) + ": " + notes.get(i));
        }
    }

    /** Displays the number of matches before the search results, including zero matches. */
    public void showSearchResults(List<Client> clients) {
        ui.showMessage("Found " + clients.size() + " matching client" + (clients.size() == 1 ? "." : "s."));
        showClients(clients);
    }

    /** Displays numbered client entries, or a message when the list is empty. */
    public void showClients(List<Client> clients) {
        if (clients.isEmpty()) {
            ui.showMessage("No clients found.");
            return;
        }
        for (int i = 0; i < clients.size(); i++) {
            ui.showMessage((i + 1) + ". " + formatClient(clients.get(i)));
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
        case "list":
            ui.showMessage("Format: client.list");
            break;
        case "find":
            ui.showMessage("Format: client.find NAME");
            break;
        case "edit":
            ui.showMessage("Format: client.edit CLIENT_ID name/NEW_NAME");
            break;
        case "delete":
            ui.showMessage("Format: client.delete CLIENT_ID");
            break;
        case "delete_note":
            ui.showMessage("Format: client.delete_note CLIENT_ID index/NOTE_INDEX");
            break;
        default:
            break;
        }
    }

    private String formatClient(Client client) {
        return client.getClientId() + " | " + client.getName();
    }
}
