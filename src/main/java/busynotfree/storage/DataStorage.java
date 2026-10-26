package busynotfree.storage;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import busynotfree.model.Client;
import busynotfree.model.ClientManager;
import busynotfree.model.Event;
import busynotfree.model.Project;
import busynotfree.model.ProjectManager;
import busynotfree.model.ProjectStatus;
import busynotfree.model.ScheduledItem;
import busynotfree.model.Task;
import busynotfree.model.TrashcanManager;

/**
 * Saves and loads all application data in one UTF-8 properties file.
 * The file includes the next ID numbers so deleted IDs are not reused after a restart.
 */
public final class DataStorage {
    private static final String VERSION = "1";
    private final Path dataFile;

    /** Creates storage at the supplied file path. */
    public DataStorage(Path dataFile) {
        this.dataFile = dataFile;
    }

    /** Holds the linked managers reconstructed from one saved file. */
    public record LoadedData(ClientManager clientManager, ProjectManager projectManager) {
    }

    /**
     * Loads data, or starts with empty managers when no saved file exists.
     *
     * @return managers sharing the same client lookup
     * @throws IOException if a saved file cannot be read or is invalid
     */
    public LoadedData load() throws IOException {
        TrashcanManager trashcan = new TrashcanManager();
        ClientManager clients = new ClientManager(trashcan);
        ProjectManager projects = new ProjectManager(clients, trashcan);
        if (!Files.exists(dataFile)) {
            return new LoadedData(clients, projects);
        }
        Properties data = new Properties();
        try {
            try (Reader reader = Files.newBufferedReader(dataFile, StandardCharsets.UTF_8)) {
                data.load(reader);
            }
            if (!VERSION.equals(required(data, "version"))) {
                throw new IllegalArgumentException("Unsupported data version.");
            }
            loadClients(data, clients);
            loadProjects(data, projects);
            loadTasks(data, projects);
            loadEvents(data, projects);
            loadTrashcan(data, clients, projects);
            clients.advanceNextClientNumber(positiveNumber(data, "next.client"));
            projects.advanceNextNumbers(positiveNumber(data, "next.project"),
                    positiveNumber(data, "next.task"), positiveNumber(data, "next.event"));
            return new LoadedData(clients, projects);
        } catch (RuntimeException exception) {
            throw new IOException("Saved data is invalid: " + exception.getMessage(), exception);
        }
    }

    /**
     * Writes all clients, notes, projects, tasks and events before replacing the old file.
     *
     * @throws IOException if the data cannot be saved
     */
    public void save(ClientManager clients, ProjectManager projects) throws IOException {
        Properties data = new Properties();
        data.setProperty("version", VERSION);
        // Restoring an older project appends it to memory; storage loaders require ID order.
        saveClients(data, clients.getClients());
        saveProjects(data, projects.getProjects().stream()
                .sorted(Comparator.comparingInt(project -> idNumber(project.getProjectId(), "P"))).toList());
        Properties trash = new Properties();
        saveClients(trash, clients.getTrashcanManager().getDeletedClients());
        saveProjects(trash, projects.getTrashcanManager().getDeletedProjects());
        for (String key : trash.stringPropertyNames()) {
            data.setProperty("trash." + key, trash.getProperty(key));
        }
        data.setProperty("next.client", Integer.toString(clients.getNextClientNumber()));
        data.setProperty("next.project", Integer.toString(projects.getNextProjectNumber()));
        data.setProperty("next.task", Integer.toString(projects.getNextTaskNumber()));
        data.setProperty("next.event", Integer.toString(projects.getNextEventNumber()));

        Path directory = dataFile.toAbsolutePath().getParent();
        Files.createDirectories(directory);
        Path temporaryFile = Files.createTempFile(directory, "busynotfree-", ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temporaryFile, StandardCharsets.UTF_8)) {
                data.store(writer, "BusyNotFree data");
            }
            try {
                Files.move(temporaryFile, dataFile, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, dataFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    /**
     * Reads optional trash data from the existing file format.
     * Old files without a trash section remain valid. Deleted projects may refer
     * to deleted clients, but such projects cannot be restored until their client is active.
     */
    private void loadTrashcan(Properties data, ClientManager clients, ProjectManager projects) {
        Properties trash = new Properties();
        for (String key : data.stringPropertyNames()) {
            if (key.startsWith("trash.")) {
                trash.setProperty(key.substring("trash.".length()), data.getProperty(key));
            }
        }
        if (trash.isEmpty()) {
            return;
        }
        ClientManager deletedClients = new ClientManager();
        loadClients(trash, deletedClients);
        ProjectManager deletedProjects = new ProjectManager(
                id -> clients.exists(id) || deletedClients.exists(id));
        loadProjects(trash, deletedProjects);
        loadTasks(trash, deletedProjects);
        loadEvents(trash, deletedProjects);
        deletedClients.getClients().forEach(clients::loadDeletedClient);
        deletedProjects.getProjects().forEach(projects::loadDeletedProject);
    }

    /** Restores clients and their notes in ID order. */
    private void loadClients(Properties data, ClientManager clients) {
        for (int index = 0; index < count(data, "client.count"); index++) {
            String key = "client." + index + ".";
            String id = required(data, key + "id");
            clients.advanceNextClientNumber(idNumber(id, "C"));
            Client client = clients.addClient(required(data, key + "name"));
            requireMatchingId(id, client.getClientId());
            for (int note = 0; note < count(data, key + "note.count"); note++) {
                client.addNote(required(data, key + "note." + note));
            }
        }
    }

    /** Restores projects before their tasks and events. */
    private void loadProjects(Properties data, ProjectManager projects) {
        for (int index = 0; index < count(data, "project.count"); index++) {
            String key = "project." + index + ".";
            String id = required(data, key + "id");
            projects.advanceNextNumbers(idNumber(id, "P"), projects.getNextTaskNumber(),
                    projects.getNextEventNumber());
            Project project = projects.addProject(required(data, key + "client"),
                    required(data, key + "name"), LocalDate.parse(required(data, key + "deadline")));
            requireMatchingId(id, project.getProjectId());
            project.changeStatus(ProjectStatus.valueOf(required(data, key + "status")));
        }
    }

    /** Restores tasks in their global ID order. */
    private void loadTasks(Properties data, ProjectManager projects) {
        for (int index = 0; index < count(data, "task.count"); index++) {
            String key = "task." + index + ".";
            String id = required(data, key + "id");
            projects.advanceNextNumbers(projects.getNextProjectNumber(), idNumber(id, "T"),
                    projects.getNextEventNumber());
            Task task = projects.addTask(required(data, key + "project"), required(data, key + "description"),
                    LocalDate.parse(required(data, key + "date")));
            requireMatchingId(id, task.getId());
            String completed = data.getProperty(key + "completed", "false");
            if ("true".equals(completed)) {
                task.markCompleted();
            } else if (!"false".equals(completed)) {
                throw new IllegalArgumentException("Invalid " + key + "completed.");
            }
        }
    }

    /** Restores events in their global ID order. */
    private void loadEvents(Properties data, ProjectManager projects) {
        for (int index = 0; index < count(data, "event.count"); index++) {
            String key = "event." + index + ".";
            String id = required(data, key + "id");
            projects.advanceNextNumbers(projects.getNextProjectNumber(), projects.getNextTaskNumber(),
                    idNumber(id, "E"));
            Event event = projects.addEvent(required(data, key + "project"), required(data, key + "description"),
                    LocalDate.parse(required(data, key + "date")));
            requireMatchingId(id, event.getId());
        }
    }

    /** Stores clients and all notes in their creation order. */
    private void saveClients(Properties data, List<Client> clientList) {
        data.setProperty("client.count", Integer.toString(clientList.size()));
        for (int index = 0; index < clientList.size(); index++) {
            Client client = clientList.get(index);
            String key = "client." + index + ".";
            data.setProperty(key + "id", client.getClientId());
            data.setProperty(key + "name", client.getName());
            data.setProperty(key + "note.count", Integer.toString(client.getNotes().size()));
            for (int note = 0; note < client.getNotes().size(); note++) {
                data.setProperty(key + "note." + note, client.getNotes().get(note));
            }
        }
    }

    /** Stores projects followed by activities sorted by their application-wide IDs. */
    private void saveProjects(Properties data, List<Project> projectList) {
        List<Task> tasks = new ArrayList<>();
        List<Event> events = new ArrayList<>();
        data.setProperty("project.count", Integer.toString(projectList.size()));
        for (int index = 0; index < projectList.size(); index++) {
            Project project = projectList.get(index);
            String key = "project." + index + ".";
            data.setProperty(key + "id", project.getProjectId());
            data.setProperty(key + "client", project.getClientId());
            data.setProperty(key + "name", project.getName());
            data.setProperty(key + "deadline", project.getDeadline().toString());
            data.setProperty(key + "status", project.getStatus().name());
            tasks.addAll(project.getTasks());
            events.addAll(project.getEvents());
        }
        tasks.sort(Comparator.comparingInt(task -> idNumber(task.getId(), "T")));
        events.sort(Comparator.comparingInt(event -> idNumber(event.getId(), "E")));
        data.setProperty("task.count", Integer.toString(tasks.size()));
        data.setProperty("event.count", Integer.toString(events.size()));
        for (int index = 0; index < tasks.size(); index++) {
            String key = "task." + index + ".";
            Task task = tasks.get(index);
            saveItem(data, key, task);
            data.setProperty(key + "completed", Boolean.toString(task.isCompleted()));
        }
        for (int index = 0; index < events.size(); index++) {
            saveItem(data, "event." + index + ".", events.get(index));
        }
    }

    /** Stores fields shared by tasks and events. */
    private void saveItem(Properties data, String key, ScheduledItem item) {
        data.setProperty(key + "id", item.getId());
        data.setProperty(key + "project", item.getProject().getProjectId());
        data.setProperty(key + "description", item.getDescription());
        data.setProperty(key + "date", item.getDate().toString());
    }

    /** Reads a required property, rejecting a missing field. */
    private String required(Properties data, String key) {
        String value = data.getProperty(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing " + key + ".");
        }
        return value;
    }

    /** Parses a nonnegative collection size. */
    private int count(Properties data, String key) {
        int value = Integer.parseInt(required(data, key));
        if (value < 0) {
            throw new IllegalArgumentException("Invalid " + key + ".");
        }
        return value;
    }

    /** Parses a positive ID counter. */
    private int positiveNumber(Properties data, String key) {
        int value = Integer.parseInt(required(data, key));
        if (value < 1) {
            throw new IllegalArgumentException("Invalid " + key + ".");
        }
        return value;
    }

    /** Parses the numeric part of an application ID. */
    private int idNumber(String id, String prefix) {
        if (!id.matches(prefix + "[0-9]{3,}")) {
            throw new IllegalArgumentException("Invalid ID " + id + ".");
        }
        int number = Integer.parseInt(id.substring(1));
        if (number < 1) {
            throw new IllegalArgumentException("Invalid ID " + id + ".");
        }
        return number;
    }

    /** Rejects IDs that cannot be produced by the application's ID formatter. */
    private void requireMatchingId(String saved, String generated) {
        if (!saved.equals(generated)) {
            throw new IllegalArgumentException("Invalid ID " + saved + ".");
        }
    }
}
