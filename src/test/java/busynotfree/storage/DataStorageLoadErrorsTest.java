package busynotfree.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import busynotfree.model.ClientManager;
import busynotfree.model.ProjectManager;
import busynotfree.model.ProjectStatus;

/** Checks that malformed saved fields identify the problem without exposing raw Java parser messages. */
class DataStorageLoadErrorsTest {
    @TempDir
    private Path directory;

    private int nextFixtureNumber = 1;

    @Test
    public void load_invalidProjectStatus_namesFieldAndAllowedValues() throws IOException {
        for (String value : List.of("active", "UNKNOWN", "")) {
            assertInvalidProperty("project.0.status", value,
                    "Expected one of [ACTIVE, PAUSED, COMPLETED, CANCELLED].");
        }
    }

    @Test
    public void load_invalidCounts_namesFieldAndNonnegativeRange() throws IOException {
        for (String key : List.of("client.count", "client.0.note.count", "project.count", "task.count",
                "event.count")) {
            for (String value : List.of("abc", "2147483648", "-1")) {
                assertInvalidProperty(key, value, "Expected a whole number from 0 to 2147483647.");
            }
        }
    }

    @Test
    public void load_invalidNextIds_namesFieldAndPositiveRange() throws IOException {
        for (String key : List.of("next.client", "next.project", "next.task", "next.event")) {
            for (String value : List.of("abc", "2147483648", "0", "-1")) {
                assertInvalidProperty(key, value, "Expected a whole number from 1 to 2147483647.");
            }
        }
    }

    @Test
    public void load_invalidDates_namesTheCorrectDateField() throws IOException {
        for (String key : List.of("project.0.deadline", "task.0.date", "event.0.date")) {
            for (String value : List.of("2026-02-30", "not-a-date", "2026-11")) {
                assertInvalidProperty(key, value, "Expected a valid date in YYYY-MM-DD format.");
            }
        }
    }

    @Test
    public void load_overflowingIds_namesFieldInsteadOfIntegerParserText() throws IOException {
        for (String prefix : List.of("client", "project", "task", "event")) {
            String idPrefix = prefix.substring(0, 1).toUpperCase(Locale.ROOT);
            assertInvalidProperty(prefix + ".0.id", idPrefix + "2147483648", "Expected an ID such as");
        }
    }

    @Test
    public void load_missingFields_preservesMissingPropertyDiagnostic() throws IOException {
        for (String key : List.of("project.0.status", "project.0.deadline", "task.0.date", "next.task")) {
            Path file = createFixture();
            changeProperty(file, key, null);
            byte[] before = Files.readAllBytes(file);

            IOException exception = assertThrows(IOException.class, () -> new DataStorage(file).load());

            assertEquals("Saved data is invalid: Missing " + key + ".", exception.getMessage());
            assertArrayEquals(before, Files.readAllBytes(file));
        }
    }

    @Test
    public void load_invalidTrashcanFields_identifiesSectionAndField() throws IOException {
        for (String key : List.of("trash.client.count", "trash.client.0.note.count", "trash.project.0.status",
                "trash.project.0.deadline", "trash.task.0.date", "trash.event.0.date")) {
            IOException exception = assertInvalidProperty(key, "invalid", "Expected");

            assertTrue(exception.getMessage().contains("Invalid trashcan data:"));
        }
    }

    @Test
    public void load_validStatuses_preservesEachSupportedValue() throws IOException {
        for (ProjectStatus status : ProjectStatus.values()) {
            Path file = createFixture();
            changeProperty(file, "project.0.status", status.name());

            assertEquals(status, new DataStorage(file).load().projectManager().getProject("P001").getStatus());
        }
    }

    /** Checks both the field-specific error and preservation of the rejected file's bytes. */
    private IOException assertInvalidProperty(String key, String value, String expected) throws IOException {
        Path file = createFixture();
        changeProperty(file, key, value);
        byte[] before = Files.readAllBytes(file);

        IOException exception = assertThrows(IOException.class, () -> new DataStorage(file).load());

        String message = exception.getMessage();
        String localKey = key.startsWith("trash.") ? key.substring("trash.".length()) : key;
        assertTrue(message.contains("Invalid " + localKey + "."), message);
        assertTrue(message.contains(expected), message);
        assertFalse(message.contains("No enum constant"), message);
        assertFalse(message.contains("For input string"), message);
        assertFalse(message.contains("could not be parsed"), message);
        assertNotNull(exception.getCause());
        assertArrayEquals(before, Files.readAllBytes(file));
        return exception;
    }

    /** Saves active and deleted records so the same parsers are exercised in both storage sections. */
    private Path createFixture() throws IOException {
        Path file = directory.resolve("data-" + nextFixtureNumber++ + ".properties");
        ClientManager clients = new ClientManager();
        ProjectManager projects = new ProjectManager(clients);
        LocalDate date = LocalDate.of(2026, 11, 30);
        clients.addClient("Acme");
        clients.addNote("C001", "Active note");
        clients.addClient("Archived client");
        clients.addNote("C002", "Archived note");
        projects.addProject("C001", "Website", date);
        projects.addTask("P001", "Active task", date);
        projects.addEvent("P001", "Active event", date);
        projects.addProject("C002", "Archived project", date);
        projects.addTask("P002", "Archived task", date);
        projects.addEvent("P002", "Archived event", date);
        projects.deleteProject("P002");
        clients.deleteClient("C002", projects);
        new DataStorage(file).save(clients, projects);
        return file;
    }

    /** Replaces or removes one saved field without changing the other fixture properties. */
    private void changeProperty(Path file, String key, String value) throws IOException {
        Properties data = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            data.load(reader);
        }
        if (value == null) {
            data.remove(key);
        } else {
            data.setProperty(key, value);
        }
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            data.store(writer, "Malformed-field regression fixture");
        }
    }
}
