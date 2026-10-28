package busynotfree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

    /** Launches Java 25 in a temporary working directory so each test has its own save file. */
    private String runApplication(String commands) throws IOException, InterruptedException {
        String classpath;
        try {
            classpath = Path.of(BusyNotFree.class.getProtectionDomain().getCodeSource()
                    .getLocation().toURI()).toString();
        } catch (URISyntaxException exception) {
            throw new IOException("Could not locate application classes.", exception);
        }
        String javaName = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        String java = Path.of(System.getProperty("java.home"), "bin", javaName).toString();
        Process process = new ProcessBuilder(java, "-ea", "-cp", classpath, "busynotfree.BusyNotFree")
                .directory(directory.toFile()).redirectErrorStream(true).start();
        try (var input = process.getOutputStream()) {
            input.write(commands.getBytes(StandardCharsets.UTF_8));
        }
        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IOException("Application did not finish within 10 seconds.");
        }
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, process.exitValue(), output);
        return output;
    }
}
