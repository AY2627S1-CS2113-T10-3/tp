package busynotfree.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import busynotfree.storage.DataStorage;

/** Checks numeric project ordering after restoration and persistence. */
class ProjectRestoreOrderingTest {
    @TempDir
    private Path directory;

    /** Checks ordering, retained activity state and unchanged ID counters. */
    @Test
    public void restore_olderProject_sortsProjectsAndPreservesState() {
        ClientManager clients = new ClientManager();
        clients.addClient("Acme");
        ProjectManager projects = new ProjectManager(clients);
        LocalDate date = LocalDate.of(2026, 11, 30);

        projects.addProject("C001", "First", date);
        projects.addProject("C001", "Second", date);
        projects.addTask("P001", "Wireframe", date);
        projects.markTaskComplete("T001");
        projects.addEvent("P001", "Meeting", date);
        projects.deleteProject("P001");

        Project restored = projects.restoreProject("P001");

        assertEquals(List.of("P001", "P002"), projectIds(projects.getProjects()));
        assertEquals(List.of("P001", "P002"), projectIds(projects.getProjectsByDeadline()));
        assertEquals("T001", restored.getTasks().getFirst().getId());
        assertTrue(restored.getTasks().getFirst().isCompleted());
        assertEquals("E001", restored.getEvents().getFirst().getId());
        assertTrue(projects.getDeletedProjects().isEmpty());
        assertEquals(3, projects.getNextProjectNumber());
        assertEquals(2, projects.getNextTaskNumber());
        assertEquals(2, projects.getNextEventNumber());
    }

    /** Checks that numeric ordering remains correct beyond three-digit IDs. */
    @Test
    public void restore_idsBeyondThreeDigits_sortsNumerically() {
        ClientManager clients = new ClientManager();
        clients.addClient("Acme");
        ProjectManager projects = new ProjectManager(clients);
        LocalDate date = LocalDate.of(2026, 11, 30);

        projects.advanceNextNumbers(999, 1, 1);
        projects.addProject("C001", "Project 999", date);
        projects.addProject("C001", "Project 1000", date);
        projects.deleteProject("P999");

        projects.restoreProject("P999");

        assertEquals(List.of("P999", "P1000"), projectIds(projects.getProjects()));
        assertEquals(1001, projects.getNextProjectNumber());
    }

    /** Checks that saving and loading preserve the restored project order. */
    @Test
    public void restore_saveAndLoad_preservesNumericOrder() throws IOException {
        ClientManager clients = new ClientManager();
        clients.addClient("Acme");
        ProjectManager projects = new ProjectManager(clients);
        LocalDate date = LocalDate.of(2026, 11, 30);

        projects.addProject("C001", "First", date);
        projects.addProject("C001", "Second", date);
        projects.deleteProject("P001");
        projects.restoreProject("P001");

        DataStorage storage = new DataStorage(directory.resolve("busynotfree.properties"));
        storage.save(clients, projects);
        DataStorage.LoadedData loaded = storage.load();

        assertEquals(List.of("P001", "P002"),
                projectIds(loaded.projectManager().getProjects()));
    }

    /** Extracts IDs while retaining the supplied display order. */
    private List<String> projectIds(List<Project> projects) {
        return projects.stream().map(Project::getProjectId).toList();
    }
}
