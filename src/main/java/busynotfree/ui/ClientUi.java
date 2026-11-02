package busynotfree.ui;

import java.util.ArrayList;
import java.util.List;

import busynotfree.model.Client;

/**
 * Formats client responses through the application's shared user interface.
 */
public final class ClientUi {
    /** Bounds client response lines without relying on terminal-specific APIs. */
    private static final int MAX_LINE_WIDTH = 80;
    /** Keeps short response blocks visually distinct from their surrounding output. */
    private static final int MIN_SEPARATOR_WIDTH = 24;
    private final Ui ui;

    public ClientUi(Ui ui) {
        this.ui = ui;
    }

    /** Displays the new client's assigned ID and name. */
    public void showClientAdded(Client client) {
        showMessage("Added client " + client.getClientId() + ": " + client.getName() + ".");
    }

    /** Confirms the previous and updated names alongside the client's unchanged ID. */
    public void showClientEdited(Client client, String previousName) {
        showMessage("Updated client " + client.getClientId() + " from \"" + previousName
                + "\" to \"" + client.getName() + "\".");
    }

    /** Confirms that a note was attached to its client. */
    public void showNoteAdded(Client client) {
        showMessage("Note added to client " + client.getClientId() + ".");
    }

    /** Displays the client's details and every note in insertion order. */
    public void showClient(Client client) {
        List<String> lines = new ArrayList<>();
        appendClientEntry(lines, "", client);
        lines.add("");
        List<String> notes = client.getNotes();
        if (notes.isEmpty()) {
            lines.add("Notes: No notes found.");
        } else {
            for (int i = 0; i < notes.size(); i++) {
                appendWrappedText(lines, "Note " + (i + 1) + ": ", notes.get(i));
            }
        }
        showBlock(lines);
    }

    /** Displays the number of matches before the search results, including zero matches. */
    public void showSearchResults(List<Client> clients) {
        List<String> lines = new ArrayList<>();
        lines.add("Found " + clients.size() + " matching client" + (clients.size() == 1 ? "." : "s."));
        lines.add("");
        appendClientList(lines, clients);
        showBlock(lines);
    }

    /** Displays numbered client entries, or a message when the list is empty. */
    public void showClients(List<Client> clients) {
        List<String> lines = new ArrayList<>();
        appendClientList(lines, clients);
        showBlock(lines);
    }

    /** Builds list rows in the supplied order, aligning numbers at their right edge. */
    private void appendClientList(List<String> lines, List<Client> clients) {
        if (clients.isEmpty()) {
            lines.add("No clients found.");
            return;
        }
        int numberWidth = Integer.toString(clients.size()).length();
        for (int i = 0; i < clients.size(); i++) {
            String number = Integer.toString(i + 1);
            String prefix = " ".repeat(numberWidth - number.length()) + number + ". ";
            appendClientEntry(lines, prefix, clients.get(i));
        }
    }

    /** Keeps each client's ID and name together, indenting long names beneath the name column. */
    private void appendClientEntry(List<String> lines, String prefix, Client client) {
        appendWrappedText(lines, prefix + client.getClientId() + " | ", client.getName());
    }

    /**
     * Wraps free text while keeping short messages and internal spacing intact.
     * Continuation lines align with the text after the prefix. Existing line breaks
     * are respected, and long unbroken words are split without cutting a surrogate pair.
     */
    private void appendWrappedText(List<String> lines, String prefix, String text) {
        prefix = Ui.escapeControls(prefix);
        int prefixWidth = prefix.codePointCount(0, prefix.length());
        if (prefixWidth >= MAX_LINE_WIDTH) {
            // Render an oversized label on its own lines so the content always has room to advance.
            appendWrappedText(lines, "", prefix);
            prefix = "";
            prefixWidth = 0;
        }
        int textWidth = MAX_LINE_WIDTH - prefixWidth;
        String indentation = " ".repeat(prefixWidth);
        String linePrefix = prefix;
        for (String originalParagraph : text.split("\\R", -1)) {
            String paragraph = Ui.escapeControls(originalParagraph);
            int start = 0;
            int remaining = paragraph.codePointCount(0, paragraph.length());
            do {
                int limit = paragraph.offsetByCodePoints(start, Math.min(textWidth, remaining));
                int end = limit < paragraph.length() ? findWrapEnd(paragraph, start, limit) : limit;
                lines.add(linePrefix + paragraph.substring(start, end));
                int nextStart = end;
                if (nextStart < paragraph.length() && Character.isWhitespace(paragraph.codePointAt(nextStart))) {
                    // A line break takes the place of the single whitespace character at the boundary.
                    nextStart += Character.charCount(paragraph.codePointAt(nextStart));
                }
                remaining -= paragraph.codePointCount(start, nextStart);
                start = nextStart;
                linePrefix = indentation;
            } while (start < paragraph.length());
        }
    }

    /** Chooses the last word boundary that fits, falling back to a Unicode-safe hard break. */
    private int findWrapEnd(String text, int start, int limit) {
        int position = limit;
        while (position > start) {
            if (Character.isWhitespace(text.codePointAt(position))) {
                return position;
            }
            position = text.offsetByCodePoints(position, -1);
        }
        return limit;
    }

    /** Prints a response between matching separators sized to its rendered content. */
    private void showBlock(List<String> lines) {
        int width = MIN_SEPARATOR_WIDTH;
        for (String line : lines) {
            width = Math.max(width, line.codePointCount(0, line.length()));
        }
        String separator = "=".repeat(Math.min(width, MAX_LINE_WIDTH));
        showMessage(separator);
        for (String line : lines) {
            showMessage(line);
        }
        showMessage(separator);
    }

    /** Protects client output even when a supplied UI overrides the shared printing method. */
    private void showMessage(String message) {
        ui.showMessage(Ui.escapeControls(message));
    }

    /** Displays the expected syntax after a supported client command fails. */
    public void showCommandUsage(String action) {
        switch (action) {
        case "add":
            showMessage("Format: client.add name/CLIENT_NAME");
            break;
        case "note":
            showMessage("Format: client.note CLIENT_ID text/NOTE");
            break;
        case "view":
            showMessage("Format: client.view CLIENT_ID");
            break;
        case "list":
            showMessage("Format: client.list");
            break;
        case "find":
            showMessage("Format: client.find NAME");
            break;
        case "edit":
            showMessage("Format: client.edit CLIENT_ID name/NEW_NAME");
            break;
        case "delete":
            showMessage("Format: client.delete CLIENT_ID");
            break;
        case "delete_note":
            showMessage("Format: client.delete_note CLIENT_ID index/NOTE_INDEX");
            break;
        default:
            break;
        }
    }
}
