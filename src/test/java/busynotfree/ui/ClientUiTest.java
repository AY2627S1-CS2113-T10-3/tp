package busynotfree.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import busynotfree.model.Client;

/** Checks complete client response layouts, wrapping boundaries and unchanged client data. */
class ClientUiTest {
    private final RecordingUi output = new RecordingUi();
    private final ClientUi ui = new ClientUi(output);

    @Test
    public void showClient_shortNotes_preservesMessagesAndGroupsDetails() {
        Client client = new Client("C001", "Acme Studio");
        client.addNote("Prefers weekly updates");

        ui.showClient(client);

        assertEquals(List.of("=".repeat(30), "C001 | Acme Studio", "", "Note 1: Prefers weekly updates",
                "=".repeat(30)), output.lines);
    }

    @Test
    public void showClient_withoutNotes_preservesEmptyMessageAndMinimumSeparatorWidth() {
        ui.showClient(new Client("C001", "Acme"));

        assertEquals(List.of("=".repeat(24), "C001 | Acme", "", "Notes: No notes found.", "=".repeat(24)),
                output.lines);
    }

    @Test
    public void showClients_emptyList_groupsExistingEmptyMessage() {
        ui.showClients(List.of());

        assertEquals(List.of("=".repeat(24), "No clients found.", "=".repeat(24)), output.lines);
    }

    @Test
    public void showSearchResults_emptyList_groupsSummaryAndEmptyMessage() {
        ui.showSearchResults(List.of());

        assertEquals(List.of("=".repeat(25), "Found 0 matching clients.", "", "No clients found.",
                "=".repeat(25)), output.lines);
    }

    @Test
    public void showSearchResults_singleMatch_preservesSingularSummary() {
        ui.showSearchResults(List.of(new Client("C003", "Acme")));

        assertEquals(List.of("=".repeat(24), "Found 1 matching client.", "", "1. C003 | Acme", "=".repeat(24)),
                output.lines);
    }

    @Test
    public void showSearchResults_multipleMatches_preservesSuppliedOrderAndHidesNotes() {
        Client first = new Client("C999", "Acme");
        first.addNote("Only visible in client.view");
        ui.showSearchResults(List.of(first, new Client("C1000", "Acme again")));

        assertEquals(List.of("=".repeat(25), "Found 2 matching clients.", "", "1. C999 | Acme",
                "2. C1000 | Acme again", "=".repeat(25)), output.lines);
    }

    @Test
    public void showClients_manyEntries_alignsNumbersAcrossDigitBoundaries() {
        for (int count : List.of(9, 10, 100)) {
            List<Client> clients = new ArrayList<>();
            for (int i = 1; i <= count; i++) {
                clients.add(new Client("C" + (1000 + i), "Client " + i));
            }
            output.lines.clear();

            ui.showClients(clients);

            int width = Integer.toString(count).length();
            assertEquals(" ".repeat(width - 1) + "1. C1001 | Client 1", output.lines.get(1));
            assertEquals(count + ". C" + (1000 + count) + " | Client " + count, output.lines.get(count));
            assertEquals(count + 2, output.lines.size());
            for (String row : output.lines.subList(1, count + 1)) {
                assertEquals(width, row.indexOf('.'));
            }
        }
    }

    @Test
    public void showClient_longNote_wrapsAtWordsAndKeepsStoredNoteUnchanged() {
        Client client = new Client("C001", "Acme");
        String note = "one ".repeat(25) + "end";
        client.addNote(note);

        ui.showClient(client);

        assertEquals(List.of("=".repeat(79), "C001 | Acme", "",
                "Note 1: " + "one ".repeat(18).stripTrailing(), "        " + "one ".repeat(7) + "end",
                "=".repeat(79)), output.lines);
        assertEquals(List.of(note), client.getNotes());
    }

    @Test
    public void showClient_noteAtWidthBoundary_doesNotAddEmptyContinuation() {
        Client client = new Client("C001", "Acme");
        client.addNote("x".repeat(72));

        ui.showClient(client);

        assertEquals(List.of("=".repeat(80), "C001 | Acme", "", "Note 1: " + "x".repeat(72),
                "=".repeat(80)), output.lines);
    }

    @Test
    public void showClient_spaceAtWidthBoundary_usesFullLineBeforeWrapping() {
        Client client = new Client("C001", "Acme");
        client.addNote("x".repeat(72) + " next");

        ui.showClient(client);

        assertEquals("Note 1: " + "x".repeat(72), output.lines.get(3));
        assertEquals("        next", output.lines.get(4));
    }

    @Test
    public void showClient_longUrl_preservesEveryCharacterAcrossHardBreaks() {
        Client client = new Client("C001", "Acme");
        String url = "https://example.com/" + "a".repeat(150);
        client.addNote(url);

        ui.showClient(client);

        List<String> noteLines = output.lines.subList(3, output.lines.size() - 1);
        String reconstructed = noteLines.stream().map(line -> line.substring(8)).reduce("", String::concat);
        assertEquals(url, reconstructed);
        assertTrue(output.lines.stream().allMatch(line -> line.length() <= 80));
        assertEquals(List.of(url), client.getNotes());
    }

    @Test
    public void showClient_supplementaryUnicode_doesNotSplitSurrogatePairs() {
        Client client = new Client("C001", "Acme");
        client.addNote("😀".repeat(73));

        ui.showClient(client);

        assertEquals("Note 1: " + "😀".repeat(72), output.lines.get(3));
        assertEquals("        😀", output.lines.get(4));
        assertTrue(output.lines.stream().allMatch(line -> line.codePointCount(0, line.length()) <= 80));
        assertFalse(output.lines.stream().flatMapToInt(String::codePoints)
                .anyMatch(codePoint -> codePoint >= 0xd800 && codePoint <= 0xdfff));
    }

    @Test
    public void showClient_existingLineBreaksAndInternalSpaces_preservesTextStructure() {
        Client client = new Client("C001", "Acme  Studio");
        client.addNote("First  line\r\n\r\nSecond line");

        ui.showClient(client);

        assertEquals(List.of("=".repeat(24), "C001 | Acme  Studio", "", "Note 1: First  line", "        ",
                "        Second line", "=".repeat(24)), output.lines);
        assertEquals(List.of("First  line\r\n\r\nSecond line"), client.getNotes());
    }

    @Test
    public void showClient_tenthNote_alignsContinuationAfterLongerLabel() {
        Client client = new Client("C001", "Acme");
        for (int i = 0; i < 9; i++) {
            client.addNote("Short note");
        }
        client.addNote("x".repeat(75));

        ui.showClient(client);

        assertEquals("Note 10: " + "x".repeat(71), output.lines.get(12));
        assertEquals("         xxxx", output.lines.get(13));
        assertEquals(10, client.getNotes().size());
    }

    @Test
    public void showClients_longName_indentsContinuationAndCapsSeparatorWidth() {
        Client client = new Client("C001", "N".repeat(100));

        ui.showClients(List.of(client));

        assertEquals(List.of("=".repeat(80), "1. C001 | " + "N".repeat(70), "          " + "N".repeat(30),
                "=".repeat(80)), output.lines);
        assertEquals("N".repeat(100), client.getName());
    }

    @Test
    public void confirmations_preserveDocumentedMessagesWithoutDecorations() {
        Client client = new Client("C001", "Acme Studio");

        ui.showClientAdded(client);
        ui.showNoteAdded(client);
        ui.showClientEdited(client, "Acme");

        assertEquals(List.of("Added client C001: Acme Studio.", "Note added to client C001.",
                "Updated client C001 from \"Acme\" to \"Acme Studio\"."), output.lines);
    }

    @Test
    public void showClient_controlsAndTabs_wrapsVisibleEscapesWithoutChangingNotes() {
        Client client = new Client("C001", "Acme");
        String note = "x" + "\t".repeat(40) + "\u001b[2Jworld";
        client.addNote(note);

        ui.showClient(client);

        assertFalse(output.lines.stream().anyMatch(line -> line.contains("\u001b") || line.contains("\t")));
        assertTrue(output.lines.stream().anyMatch(line -> line.contains("\\u001B[2J")));
        assertTrue(output.lines.stream().allMatch(line -> line.codePointCount(0, line.length()) <= 80));
        assertEquals(List.of(note), client.getNotes());
    }

    @Test
    public void confirmations_controls_areEscapedEvenWithOverriddenUiPrinting() {
        Client client = new Client("C001", "Acme\u001b[31m");

        ui.showClientAdded(client);
        ui.showClientEdited(client, "Old\u0008name");

        assertEquals(List.of("Added client C001: Acme\\u001B[31m.",
                "Updated client C001 from \"Old\\u0008name\" to \"Acme\\u001B[31m\"."), output.lines);
    }

    /** Records every rendered line, including whitespace and decoration. */
    private static class RecordingUi extends Ui {
        private final List<String> lines = new ArrayList<>();

        @Override
        public void showMessage(String message) {
            lines.add(message);
        }
    }
}
