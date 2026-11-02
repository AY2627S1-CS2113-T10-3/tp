package busynotfree;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests startup and restart behavior using real application processes and isolated data folders. */
class BusyNotFreeIntegrationTest {
    @TempDir
    private Path directory;

    @Test
    public void main_saveAndRestart_restoresCompletedTask() throws IOException, InterruptedException {
        String firstOutput = runApplication("client.add name/Acme Studio\n"
                + "project.add client/C001 name/Website due/2026-11-30\n"
                + "task.add project/P001 name/Wireframe date/2026-10-15\n"
                + "task.mark T001\nsave\nexit\n");

        assertTrue(firstOutput.contains("Data saved."));
        assertTrue(Files.exists(directory.resolve("data").resolve("busynotfree.properties")));

        String secondOutput = runApplication("project.view P001\nexit\n");

        assertTrue(secondOutput.contains("Project: P001 - Website"));
        assertTrue(secondOutput.contains("Client: C001 - Acme Studio"));
        assertTrue(secondOutput.contains("[T001] Wireframe - 2026-10-15 [COMPLETED]"));
        assertTrue(secondOutput.contains("Progress: 1 of 1 task completed."));
        assertFalse(secondOutput.contains("Error:"));
    }

    @Test
    public void main_invalidSavedFile_reportsErrorWithoutOverwritingIt() throws IOException, InterruptedException {
        Path file = directory.resolve("data").resolve("busynotfree.properties");
        Files.createDirectories(file.getParent());
        String invalidData = "version=2\n";
        Files.writeString(file, invalidData, StandardCharsets.UTF_8);

        String output = runApplication("");

        assertTrue(output.contains("Unable to load saved data: Saved data is invalid: Unsupported data version."));
        assertEquals(invalidData, Files.readString(file, StandardCharsets.UTF_8));
        assertFalse(output.contains("Data saved. Goodbye!"));
    }

    @Test
    public void main_secondInstance_rejectsBeforeLoadingAndPreservesSavedWork()
            throws IOException, InterruptedException {
        Path output = directory.resolve("first-output.log");
        Process first = applicationProcess(directory).redirectOutput(output.toFile()).start();
        try {
            awaitOutput(output, "Enter a command:");
            assertSecondInstanceRejected();
            sendCommands(first, "client.add name/Acme Studio\n"
                    + "project.add client/C001 name/Website due/2026-11-30\n"
                    + "task.add project/P001 name/Wireframe date/2026-10-15\n"
                    + "task.mark T001\nsave\n");
            awaitOutput(output, "Data saved.");
            Path data = directory.resolve("data").resolve("busynotfree.properties");
            byte[] saved = Files.readAllBytes(data);

            assertSecondInstanceRejected();

            assertArrayEquals(saved, Files.readAllBytes(data));
            assertTrue(first.isAlive());
            sendCommands(first, "exit\n");
            first.getOutputStream().close();
            assertTrue(first.waitFor(10, TimeUnit.SECONDS));
            assertEquals(0, first.exitValue());
            assertTrue(Files.readString(output).contains("Data saved. Goodbye!"));
        } finally {
            stopApplication(first);
        }

        String restarted = runApplication("project.view P001\nexit\n");
        assertTrue(restarted.contains("Project: P001 - Website"));
        assertTrue(restarted.contains("[T001] Wireframe - 2026-10-15 [COMPLETED]"));
        assertTrue(restarted.contains("Progress: 1 of 1 task completed."));
        assertFalse(restarted.contains("Error:"));
    }

    @Test
    public void main_differentDataFolders_allowsConcurrentSessions() throws IOException, InterruptedException {
        Path output = directory.resolve("first-output.log");
        Process first = applicationProcess(directory).redirectOutput(output.toFile()).start();
        try {
            awaitOutput(output, "Enter a command:");
            sendCommands(first, "client.add name/First\nsave\n");
            awaitOutput(output, "Data saved.");
            Path otherDirectory = Files.createDirectory(directory.resolve("other"));

            String second = runApplication(otherDirectory, "client.add name/Other\nexit\n");

            assertTrue(second.contains("Data saved. Goodbye!"));
            assertFalse(second.contains("Error:"));
            assertTrue(first.isAlive());
            String firstData = Files.readString(directory.resolve("data").resolve("busynotfree.properties"));
            String otherData = Files.readString(otherDirectory.resolve("data").resolve("busynotfree.properties"));
            assertTrue(firstData.contains("First"));
            assertFalse(firstData.contains("Other"));
            assertTrue(otherData.contains("Other"));
        } finally {
            stopApplication(first);
        }
    }

    @Test
    public void main_terminatedInstance_releasesLockAndRestoresSavedData() throws IOException, InterruptedException {
        Path output = directory.resolve("first-output.log");
        Process first = applicationProcess(directory).redirectOutput(output.toFile()).start();
        try {
            awaitOutput(output, "Enter a command:");
            sendCommands(first, "client.add name/Crash-safe client\nsave\n");
            awaitOutput(output, "Data saved.");
        } finally {
            stopApplication(first);
        }
        assertTrue(Files.exists(directory.resolve("data").resolve(".busynotfree.lock")));

        String restarted = runApplication("client.list\nexit\n");

        assertTrue(restarted.contains("Crash-safe client"));
        assertTrue(restarted.contains("Data saved. Goodbye!"));
        assertFalse(restarted.contains("Error:"));
    }

    @Test
    public void main_unavailableDataDirectory_reportsFailureWithoutAcceptingCommands()
            throws IOException, InterruptedException {
        Path file = directory.resolve("data");
        Files.writeString(file, "Keep this file.");

        String output = runApplication("");

        assertTrue(output.contains("Error: Unable to access application data:"));
        assertFalse(output.contains("Enter a command:"));
        assertFalse(output.contains("Data saved. Goodbye!"));
        assertEquals("Keep this file.", Files.readString(file));
    }

    /** Checks that a rejected process never starts its command loop or saves on end of input. */
    private void assertSecondInstanceRejected() throws IOException, InterruptedException {
        String output = runApplication("");

        assertTrue(output.contains("already in use by another BusyNotFree instance"));
        assertFalse(output.contains("Enter a command:"));
        assertFalse(output.contains("Data saved. Goodbye!"));
    }

    /** Waits for actual CLI output before starting the next process or checking the saved file. */
    private void awaitOutput(Path output, String expected) {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            while (!Files.readString(output, StandardCharsets.UTF_8).contains(expected)) {
                Thread.sleep(10);
            }
        });
    }

    /** Leaves terminal input open so the application remains alive between test actions. */
    private void sendCommands(Process process, String commands) throws IOException {
        process.getOutputStream().write(commands.getBytes(StandardCharsets.UTF_8));
        process.getOutputStream().flush();
    }

    /** Ensures failed tests and forced-termination scenarios do not leave a process holding the folder. */
    private void stopApplication(Process process) throws InterruptedException {
        if (process.isAlive()) {
            process.destroyForcibly();
            assertTrue(process.waitFor(10, TimeUnit.SECONDS), "Could not stop the test application.");
        }
    }

    /** Launches Java 25 in a temporary working directory so each test has its own save file. */
    private String runApplication(String commands) throws IOException, InterruptedException {
        return runApplication(directory, commands);
    }

    /** Runs one application to completion, with cleanup even when startup or an assertion fails. */
    private String runApplication(Path workingDirectory, String commands) throws IOException, InterruptedException {
        Process process = applicationProcess(workingDirectory).start();
        try {
            try (var input = process.getOutputStream()) {
                input.write(commands.getBytes(StandardCharsets.UTF_8));
            }
            assertTrue(process.waitFor(10, TimeUnit.SECONDS), "Application did not finish within 10 seconds.");
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(0, process.exitValue(), output);
            return output;
        } finally {
            stopApplication(process);
        }
    }

    /** Uses absolute class and Java paths so changing the child process's working directory is safe. */
    private ProcessBuilder applicationProcess(Path workingDirectory) throws IOException {
        String classpath;
        try {
            classpath = Path.of(BusyNotFree.class.getProtectionDomain().getCodeSource()
                    .getLocation().toURI()).toString();
        } catch (URISyntaxException exception) {
            throw new IOException("Could not locate application classes.", exception);
        }
        String javaName = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        String java = Path.of(System.getProperty("java.home"), "bin", javaName).toString();
        return new ProcessBuilder(java, "-ea", "-cp", classpath, "busynotfree.BusyNotFree")
                .directory(workingDirectory.toFile()).redirectErrorStream(true);
    }
}
