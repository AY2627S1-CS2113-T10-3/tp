package busynotfree.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import busynotfree.command.CommandHandler;
import busynotfree.command.CommandParser;
import busynotfree.model.ClientManager;
import busynotfree.model.ProjectManager;

/** Checks the real terminal output boundary, including shared client deletion and restoration messages. */
class UiTerminalSafetyTest {
    @Test
    public void showMessages_controls_areVisibleWhileUnicodeAndLineBreaksArePreserved() {
        String output = captureOutput(() -> {
            Ui ui = new Ui();
            ui.showMessage("Hello\u001b[2Jworld\t café😀\r\nnext");
            ui.showError("Bad\u009b31m\rvalue\u0000");
        });

        assertEquals("Hello\\u001B[2Jworld\\t café😀\nnext" + System.lineSeparator()
                + "Error: Bad\\u009B31m\\rvalue\\u0000" + System.lineSeparator(), output);
    }

    @Test
    public void showMessage_c0AndC1Controls_neverEmitsRawTerminalControls() {
        String output = captureOutput(() -> {
            Ui ui = new Ui();
            for (int control : new int[]{0, 1, 7, 8, 11, 12, 27, 31, 127, 128, 155, 159}) {
                ui.showMessage("Before" + (char) control + "After");
            }
        });
        String normalized = output.replace("\r\n", "\n");

        assertFalse(normalized.codePoints().anyMatch(codePoint -> Character.isISOControl(codePoint)
                && codePoint != '\n'));
        assertTrue(output.contains("\\u001B"));
        assertTrue(output.contains("\\u009B"));
    }

    @Test
    public void clientLifecycle_previewsAndRestoration_escapeTextWithoutChangingStoredValues() {
        ClientManager clients = new ClientManager();
        ProjectManager projects = new ProjectManager(clients);
        String name = "Acme\u001b[31m";
        String note = "Hello\u001b[2Jworld";
        String output = captureOutput(() -> {
            CommandHandler handler = new CommandHandler(new CommandParser(), new Ui(), projects, clients);
            handler.execute("client.add name/" + name);
            handler.execute("client.note C001 text/" + note);
            handler.execute("client.delete_note C001 index/1");
            handler.execute("no");
            handler.execute("client.delete C001");
            handler.execute("yes");
            handler.execute("trashcan.list");
            handler.execute("trashcan.restore C001");
        });

        assertFalse(output.contains("\u001b"));
        assertTrue(output.contains("Acme\\u001B[31m"));
        assertTrue(output.contains("Hello\\u001B[2Jworld"));
        assertEquals(name, clients.getClient("C001").getName());
        assertEquals(note, clients.getClient("C001").getNote(1));
    }

    /** Captures real printing and restores System.out even when an assertion or command fails. */
    private String captureOutput(Runnable action) {
        PrintStream original = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream capture = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setOut(capture);
            action.run();
        } finally {
            System.setOut(original);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }
}
