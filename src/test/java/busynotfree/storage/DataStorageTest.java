package busynotfree.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import busynotfree.model.ClientManager;
import busynotfree.model.Project;
import busynotfree.model.ProjectManager;
import busynotfree.model.ProjectStatus;

/** Tests that saved application data survives a new application session. */
class DataStorageTest {
    @TempDir
    private Path directory;

    @Test
    public void saveAndLoad_preservesAllDataAndNextIds() throws IOException {
        DataStorage storage = new DataStorage(directory.resolve("data").resolve("busynotfree.properties"));
        ClientManager clients = new ClientManager();
        clients.addClient("Acme Studio");
        clients.addNote("C001", "Call on Friday: café / 新加坡");
        clients.addClient("Bright Ideas");
        ProjectManager projects = new ProjectManager(clients);
        projects.addProject("C001", "Website redesign", LocalDate.of(2026, 11, 30));
        projects.addProject("C002", "Logo design", LocalDate.of(2026, 12, 15));
        projects.changeProjectStatus("P001", ProjectStatus.PAUSED);
        projects.addTask("P001", "First wireframe", LocalDate.of(2026, 10, 15));
        projects.addTask("P002", "Discarded sketch", LocalDate.of(2026, 10, 16));
        projects.addTask("P001", "Final wireframe", LocalDate.of(2026, 10, 17));
        projects.addEvent("P001", "Review", LocalDate.of(2026, 10, 20));
        projects.addEvent("P002", "Discarded meeting", LocalDate.of(2026, 10, 21));
        projects.deleteProject("P002");

        storage.save(clients, projects);
        DataStorage.LoadedData loaded = storage.load();

        assertEquals(List.of("Call on Friday: café / 新加坡"), loaded.clientManager().getClient("C001").getNotes());
        assertEquals("Bright Ideas", loaded.clientManager().getClient("C002").getName());
        Project restored = loaded.projectManager().getProject("P001");
        assertEquals("Website redesign", restored.getName());
        assertEquals(ProjectStatus.PAUSED, restored.getStatus());
        assertEquals(LocalDate.of(2026, 11, 30), restored.getDeadline());
        assertEquals(List.of("T001", "T003"), restored.getTasks().stream().map(task -> task.getId()).toList());
        assertEquals(List.of("E001"), restored.getEvents().stream().map(event -> event.getId()).toList());
        assertEquals(1, loaded.projectManager().getProjects().size());
        assertEquals("P003", loaded.projectManager().addProject("C002", "New project",
                LocalDate.of(2027, 1, 1)).getProjectId());
        assertEquals("T004", loaded.projectManager().addTask("P001", "Next task",
                LocalDate.of(2026, 10, 18)).getId());
        assertEquals("E003", loaded.projectManager().addEvent("P001", "Next event",
                LocalDate.of(2026, 10, 22)).getId());
        assertEquals("C003", loaded.clientManager().addClient("Next client").getClientId());
    }

    @Test
    public void saveAndLoad_editedItems_preservesUpdatedDetailsAndIds() throws IOException {
        DataStorage storage = new DataStorage(directory.resolve("edited.properties"));
        ClientManager clients = new ClientManager();
        clients.addClient("Acme Studio");
        ProjectManager projects = new ProjectManager(clients);
        projects.addProject("C001", "Website", LocalDate.of(2026, 11, 30));
        projects.addTask("P001", "Wireframe", LocalDate.of(2026, 10, 15));
        projects.addEvent("P001", "Meeting", LocalDate.of(2026, 10, 20));
        projects.editTask("T001", "Revised UI/UX wireframe", LocalDate.of(2026, 12, 1));
        projects.editEvent("E001", "Rescheduled meeting", LocalDate.of(2026, 12, 2));

        storage.save(clients, projects);
        ProjectManager loaded = storage.load().projectManager();

        assertEquals("Revised UI/UX wireframe", loaded.getTask("T001").getDescription());
        assertEquals(LocalDate.of(2026, 12, 1), loaded.getTask("T001").getDate());
        assertEquals("P001", loaded.getTask("T001").getProject().getProjectId());
        assertEquals("Rescheduled meeting", loaded.getEvent("E001").getDescription());
        assertEquals(LocalDate.of(2026, 12, 2), loaded.getEvent("E001").getDate());
        assertEquals("P001", loaded.getEvent("E001").getProject().getProjectId());
        assertEquals("T002", loaded.addTask("P001", "Next task", LocalDate.of(2026, 12, 3)).getId());
    }

    @Test
    public void load_missingFile_startsWithEmptyManagers() throws IOException {
        DataStorage storage = new DataStorage(directory.resolve("missing.properties"));

        DataStorage.LoadedData loaded = storage.load();

        assertTrue(loaded.clientManager().getClients().isEmpty());
        assertTrue(loaded.projectManager().getProjects().isEmpty());
        assertEquals("P001", loaded.projectManager().addProject(
                loaded.clientManager().addClient("Acme").getClientId(), "Website",
                LocalDate.of(2026, 11, 30)).getProjectId());
    }

    @Test
    public void load_invalidFile_reportsError() throws IOException {
        Path file = directory.resolve("invalid.properties");
        Files.writeString(file, "version=1\nclient.count=1\n", StandardCharsets.UTF_8);

        IOException exception = assertThrows(IOException.class, () -> new DataStorage(file).load());

        assertTrue(exception.getMessage().contains("Saved data is invalid"));
    }
}
