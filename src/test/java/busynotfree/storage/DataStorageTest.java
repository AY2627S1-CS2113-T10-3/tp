package busynotfree.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import busynotfree.exception.ProjectException;
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
        projects.markTaskComplete("T001");
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
        assertTrue(loaded.projectManager().getTask("T001").isCompleted());
        assertFalse(loaded.projectManager().getTask("T003").isCompleted());
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
    public void saveAndLoad_trashcan_preservesSnapshotsAndRestoresOlderProject() throws IOException {
        DataStorage storage = new DataStorage(directory.resolve("trashcan.properties"));
        ClientManager clients = new ClientManager();
        clients.addClient("Acme");
        clients.addClient("Deleted client");
        clients.addNote("C002", "Call on Friday");
        ProjectManager projects = new ProjectManager(clients);
        projects.addProject("C001", "Website", LocalDate.of(2026, 11, 30));
        projects.addTask("P001", "Wireframe", LocalDate.of(2026, 11, 1));
        projects.markTaskComplete("T001");
        projects.addEvent("P001", "Meeting", LocalDate.of(2026, 11, 2));
        projects.changeProjectStatus("P001", ProjectStatus.PAUSED);
        projects.deleteProject("P001");
        clients.deleteClient("C002", projects);
        projects.addProject("C001", "Newer", LocalDate.of(2026, 12, 1));
        storage.save(clients, projects);

        DataStorage.LoadedData loaded = storage.load();

        assertEquals(1, loaded.projectManager().getProjects().size());
        assertEquals(1, loaded.projectManager().getDeletedProjects().size());
        assertEquals(List.of("Call on Friday"), loaded.clientManager().getDeletedClients().getFirst().getNotes());
        assertFalse(loaded.clientManager().exists("C002"));
        Project restored = loaded.projectManager().restoreProject("P001");
        assertEquals(ProjectStatus.PAUSED, restored.getStatus());
        assertTrue(loaded.projectManager().getTask("T001").isCompleted());
        assertEquals("Meeting", loaded.projectManager().getEvent("E001").getDescription());
        storage.save(loaded.clientManager(), loaded.projectManager());

        DataStorage.LoadedData reloaded = storage.load();
        assertEquals(2, reloaded.projectManager().getProjects().size());
        assertTrue(reloaded.projectManager().getDeletedProjects().isEmpty());
        assertEquals(2, reloaded.projectManager().getProject("P001").getItemCount());
        assertEquals("P003", reloaded.projectManager().addProject(
                "C001", "Next", LocalDate.of(2027, 1, 1)).getProjectId());
        assertEquals("T002", reloaded.projectManager().addTask(
                "P001", "Next task", LocalDate.of(2026, 12, 2)).getId());
        assertEquals("E002", reloaded.projectManager().addEvent(
                "P001", "Next event", LocalDate.of(2026, 12, 3)).getId());
        assertEquals("C003", reloaded.clientManager().addClient("Next client").getClientId());
    }

    @Test
    public void saveAndLoad_deletedProjectWithDeletedClient_retainsBothInTrashcan() throws IOException {
        DataStorage storage = new DataStorage(directory.resolve("deleted-parent.properties"));
        ClientManager clients = new ClientManager();
        clients.addClient("Acme");
        clients.addNote("C001", "Retain this note");
        ProjectManager projects = new ProjectManager(clients);
        projects.addProject("C001", "Website", LocalDate.of(2026, 11, 30));
        projects.addTask("P001", "Wireframe", LocalDate.of(2026, 11, 1));
        projects.deleteProject("P001");
        clients.deleteClient("C001", projects);
        storage.save(clients, projects);

        DataStorage.LoadedData loaded = storage.load();

        assertEquals(1, loaded.projectManager().getDeletedProjects().getFirst().getItemCount());
        assertEquals(List.of("Retain this note"), loaded.clientManager().getDeletedClients().getFirst().getNotes());
        assertThrows(ProjectException.class,
                () -> loaded.projectManager().restoreProject("P001"));
        assertEquals(1, loaded.projectManager().getDeletedProjects().size());
    }

    @Test
    public void load_legacyFileWithoutTrashSection_startsWithEmptyTrashcan() throws IOException {
        Path file = directory.resolve("without-trash.properties");
        DataStorage storage = new DataStorage(file);
        ClientManager clients = new ClientManager();
        clients.addClient("Acme");
        ProjectManager projects = new ProjectManager(clients);
        storage.save(clients, projects);
        Properties data = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            data.load(reader);
        }
        data.keySet().removeIf(key -> key.toString().startsWith("trash."));
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            data.store(writer, "Legacy data without trashcan");
        }

        DataStorage.LoadedData loaded = storage.load();
        assertEquals(1, loaded.clientManager().getClients().size());
        assertTrue(loaded.clientManager().getDeletedClients().isEmpty());
        assertTrue(loaded.projectManager().getDeletedProjects().isEmpty());
    }

    @Test
    public void load_trashcanWithConflictingProjectId_rejectsInvalidFile() throws IOException {
        Path file = directory.resolve("conflicting-trash.properties");
        DataStorage storage = new DataStorage(file);
        ClientManager clients = new ClientManager();
        clients.addClient("Acme");
        ProjectManager projects = new ProjectManager(clients);
        projects.addProject("C001", "Deleted", LocalDate.of(2026, 11, 30));
        projects.deleteProject("P001");
        projects.addProject("C001", "Active", LocalDate.of(2026, 12, 1));
        storage.save(clients, projects);
        Properties data = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            data.load(reader);
        }
        data.setProperty("trash.project.0.id", "P002");
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            data.store(writer, "Conflicting active and deleted project IDs");
        }

        IOException exception = assertThrows(IOException.class, storage::load);
        assertTrue(exception.getMessage().contains("an ID is already in use"));
    }

    @Test
    public void saveAndLoad_deletedEvent_staysDeletedWithoutReusingId() throws IOException {
        DataStorage storage = new DataStorage(directory.resolve("deleted-event.properties"));
        ClientManager clients = new ClientManager();
        clients.addClient("Acme");
        ProjectManager projects = new ProjectManager(clients);
        projects.addProject("C001", "Website", LocalDate.of(2026, 11, 30));
        projects.addEvent("P001", "Keep", LocalDate.of(2026, 11, 1));
        projects.addEvent("P001", "Delete", LocalDate.of(2026, 11, 2));
        projects.addTask("P001", "Wireframe", LocalDate.of(2026, 11, 3));
        projects.markTaskComplete("T001");
        storage.save(clients, projects);

        projects.deleteEvent("E002");
        storage.save(clients, projects);
        DataStorage.LoadedData loaded = storage.load();

        Project restored = loaded.projectManager().getProject("P001");
        assertEquals(List.of("E001"), restored.getEvents().stream().map(event -> event.getId()).toList());
        assertEquals("Keep", loaded.projectManager().getEvent("E001").getDescription());
        assertEquals(List.of("T001"), restored.getTasks().stream().map(task -> task.getId()).toList());
        assertTrue(loaded.projectManager().getTask("T001").isCompleted());
        assertEquals("C001", restored.getClientId());
        assertEquals("E003", loaded.projectManager().addEvent(
                "P001", "Next meeting", LocalDate.of(2026, 11, 4)).getId());
    }

    @Test
    public void saveAndLoad_deletedTask_staysDeletedWithoutReusingId() throws IOException {
        DataStorage storage = new DataStorage(directory.resolve("deleted-task.properties"));
        ClientManager clients = new ClientManager();
        clients.addClient("Acme");
        ProjectManager projects = new ProjectManager(clients);
        projects.addProject("C001", "Website", LocalDate.of(2026, 11, 30));
        projects.addTask("P001", "Keep", LocalDate.of(2026, 11, 1));
        projects.addTask("P001", "Delete", LocalDate.of(2026, 11, 2));
        projects.markTaskComplete("T001");
        projects.addEvent("P001", "Meeting", LocalDate.of(2026, 11, 3));
        storage.save(clients, projects);

        projects.deleteTask("T002");
        storage.save(clients, projects);
        DataStorage.LoadedData loaded = storage.load();

        Project restored = loaded.projectManager().getProject("P001");
        assertEquals(List.of("T001"), restored.getTasks().stream().map(task -> task.getId()).toList());
        assertTrue(loaded.projectManager().getTask("T001").isCompleted());
        assertEquals(List.of("E001"), restored.getEvents().stream().map(event -> event.getId()).toList());
        assertEquals("C001", restored.getClientId());
        assertEquals("T003", loaded.projectManager().addTask(
                "P001", "Next", LocalDate.of(2026, 11, 4)).getId());
    }

    @Test
    public void saveAndLoad_deletedNote_preservesRemainingNotesAndProjectLinks() throws IOException {
        DataStorage storage = new DataStorage(directory.resolve("deleted-note.properties"));
        ClientManager clients = new ClientManager();
        clients.addClient("Acme");
        clients.addNote("C001", "First");
        clients.addNote("C001", "Remove");
        clients.addNote("C001", "Last");
        ProjectManager projects = new ProjectManager(clients);
        projects.addProject("C001", "Website", LocalDate.of(2026, 11, 30));
        storage.save(clients, projects);

        clients.deleteNote("C001", 2);
        storage.save(clients, projects);
        DataStorage.LoadedData loaded = storage.load();

        assertEquals(List.of("First", "Last"), loaded.clientManager().getClient("C001").getNotes());
        assertEquals("C001", loaded.projectManager().getProject("P001").getClientId());
        assertEquals("Website", loaded.projectManager().getProject("P001").getName());
    }

    @Test
    public void saveAndLoad_deletedClient_staysDeletedWithoutReusingId() throws IOException {
        DataStorage storage = new DataStorage(directory.resolve("deleted-client.properties"));
        ClientManager clients = new ClientManager();
        ProjectManager projects = new ProjectManager(clients);
        clients.addClient("Keep");
        clients.addNote("C001", "Keep this note");
        clients.addClient("Delete");
        clients.addNote("C002", "Remove this note");
        storage.save(clients, projects);

        clients.deleteClient("C002", projects);
        storage.save(clients, projects);
        DataStorage.LoadedData loaded = storage.load();

        assertFalse(loaded.clientManager().exists("C002"));
        assertEquals(List.of("C001"), loaded.clientManager().getClients().stream()
                .map(client -> client.getClientId()).toList());
        assertEquals(List.of("Keep this note"), loaded.clientManager().getClient("C001").getNotes());
        assertEquals("C003", loaded.clientManager().addClient("Next").getClientId());
    }

    @Test
    public void load_legacyTaskWithoutCompletionState_defaultsToIncomplete() throws IOException {
        Path file = directory.resolve("legacy.properties");
        DataStorage storage = new DataStorage(file);
        ClientManager clients = new ClientManager();
        clients.addClient("Acme Studio");
        ProjectManager projects = new ProjectManager(clients);
        projects.addProject("C001", "Website", LocalDate.of(2026, 11, 30));
        projects.addTask("P001", "Wireframe", LocalDate.of(2026, 10, 15));
        storage.save(clients, projects);

        Properties legacyData = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            legacyData.load(reader);
        }
        legacyData.remove("task.0.completed");
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            legacyData.store(writer, "V1.0 data without task completion");
        }

        assertFalse(storage.load().projectManager().getTask("T001").isCompleted());
    }

    @Test
    public void load_invalidFile_reportsError() throws IOException {
        Path file = directory.resolve("invalid.properties");
        Files.writeString(file, "version=1\nclient.count=1\n", StandardCharsets.UTF_8);

        IOException exception = assertThrows(IOException.class, () -> new DataStorage(file).load());

        assertTrue(exception.getMessage().contains("Saved data is invalid"));
    }
}
