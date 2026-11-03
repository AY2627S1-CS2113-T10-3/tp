package busynotfree.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import busynotfree.model.Client;

/** Verifies long-ID rendering in a bounded child JVM so a regression cannot exhaust the test runner. */
class ClientRenderingBoundaryTest {
    @Test
    public void showClient_prefixAtOrBeyondLineWidth_finishesWithoutLosingText() throws Exception {
        String javaName = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        String java = Path.of(System.getProperty("java.home"), "bin", javaName).toString();
        String testClasses = Path.of(LongIdProbe.class.getProtectionDomain().getCodeSource()
                .getLocation().toURI()).toString();
        String mainClasses = Path.of(Client.class.getProtectionDomain().getCodeSource()
                .getLocation().toURI()).toString();
        String classpath = testClasses + File.pathSeparator + mainClasses
                + File.pathSeparator + System.getProperty("java.class.path");
        Process process = new ProcessBuilder(java, "-Xmx32m", "-cp", classpath, LongIdProbe.class.getName())
                .redirectErrorStream(true).start();
        try {
            assertTrue(process.waitFor(10, TimeUnit.SECONDS), "Long-ID rendering failed to terminate.");
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(0, process.exitValue(), output);
            assertTrue(output.contains("PASS"), output);
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    /** Exercises the zero-width boundary and longer IDs without sharing the parent's heap. */
    public static class LongIdProbe {
        /** Renders view and list output, checking bounds and preservation of each client's ID and name. */
        public static void main(String[] args) {
            for (int idLength : new int[]{76, 77, 78, 1000}) {
                Client client = new Client("C" + "1".repeat(idLength - 1), "Acme");
                CapturingUi output = new CapturingUi();
                ClientUi ui = new ClientUi(output);
                ui.showClient(client);
                check(output.lines, client);
                output.lines.clear();
                ui.showClients(List.of(client));
                check(output.lines, client);
            }
            System.out.println("PASS");
        }

        /** Checks bounded lines and reconstructs wrapped IDs without depending on exact break positions. */
        private static void check(List<String> lines, Client client) {
            if (lines.stream().anyMatch(line -> line.codePointCount(0, line.length()) > 80)) {
                throw new AssertionError("A rendered line exceeded the width limit.");
            }
            String joined = String.join("", lines).replace(" ", "");
            if (!joined.contains(client.getClientId()) || !joined.contains(client.getName())) {
                throw new AssertionError("Client text was lost while wrapping the long prefix.");
            }
        }
    }

    /** Records the child renderer's output without writing thousands of characters to its pipe. */
    private static class CapturingUi extends Ui {
        private final List<String> lines = new ArrayList<>();

        @Override
        public void showMessage(String message) {
            lines.add(message);
        }
    }
}
