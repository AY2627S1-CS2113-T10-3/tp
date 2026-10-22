package busynotfree.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import busynotfree.exception.EventException;
import busynotfree.exception.ProjectException;
import busynotfree.exception.TaskException;

/**
 * Checks creation diagnostics without writing logs to the console or changing other component loggers.
 */
class ProjectManagerLoggingTest {
    private final Logger logger = Logger.getLogger(ProjectManager.class.getName());
    private final RecordingHandler recordingHandler = new RecordingHandler();
    private final LocalDate date = LocalDate.of(2026, 11, 30);
    private Level previousLevel;
    private boolean previouslyUsedParentHandlers;

    /** Enables and captures only this component's diagnostic messages for each test. */
    @BeforeEach
    public void setUp() {
        previousLevel = logger.getLevel();
        previouslyUsedParentHandlers = logger.getUseParentHandlers();
        logger.setLevel(Level.FINE);
        logger.setUseParentHandlers(false);
        logger.addHandler(recordingHandler);
    }

    /** Restores the logger configuration so later tests retain their original logging behavior. */
    @AfterEach
    public void tearDown() {
        logger.removeHandler(recordingHandler);
        logger.setLevel(previousLevel);
        logger.setUseParentHandlers(previouslyUsedParentHandlers);
        recordingHandler.close();
    }

    @Test
    public void add_validEntities_logsIdsWithoutPrivateText() {
        ProjectManager manager = new ProjectManager(new PlaceholderClientLookup());
        manager.addProject("C001", "Private project name", date);
        manager.addTask("P001", "Private task description", date);
        manager.addEvent("P001", "Private meeting details", date);

        assertEquals(List.of("Created project P001.", "Added task T001 to project P001.",
                "Added event E001 to project P001."), recordingHandler.messages());
        assertTrue(recordingHandler.records.stream().allMatch(record -> record.getLevel().equals(Level.FINE)));
        assertEquals(2, manager.getProject("P001").getItemCount());
        assertEquals(3, recordingHandler.records.size());
    }

    @Test
    public void add_missingParents_logsRejectionWithoutCreatingEntities() {
        ProjectManager rejectingManager = new ProjectManager(clientId -> false);
        assertThrows(ProjectException.class, () -> rejectingManager.addProject("C999", "Private name", date));
        assertTrue(rejectingManager.getProjects().isEmpty());

        ProjectManager manager = new ProjectManager(new PlaceholderClientLookup());
        assertThrows(ProjectException.class, () -> manager.addTask("P999", "Private task", date));
        assertThrows(ProjectException.class, () -> manager.addEvent("P999", "Private meeting", date));

        assertEquals(List.of("Project creation rejected because the client does not exist.",
                "Project lookup failed because the project does not exist.",
                "Project lookup failed because the project does not exist."), recordingHandler.messages());
        assertTrue(manager.getProjects().isEmpty());
    }

    @Test
    public void add_invalidItems_doesNotLogSuccessfulCreation() {
        ProjectManager manager = new ProjectManager(new PlaceholderClientLookup());
        Project project = manager.addProject("C001", "Website", date);
        recordingHandler.records.clear();

        assertThrows(TaskException.class, () -> manager.addTask("P001", " ", date));
        assertThrows(EventException.class, () -> manager.addEvent("P001", "Meeting", null));

        assertTrue(recordingHandler.records.isEmpty());
        assertEquals(0, project.getItemCount());
    }

    /** Captures log records in memory, without a console handler or a file. */
    private static class RecordingHandler extends Handler {
        private final List<LogRecord> records = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            if (isLoggable(record)) {
                records.add(record);
            }
        }

        @Override
        public void flush() {
            // In-memory records do not need flushing.
        }

        @Override
        public void close() {
            // No external resources are owned by this handler.
        }

        /** Returns resolved messages from the captured diagnostic records. */
        private List<String> messages() {
            return records.stream().map(LogRecord::getMessage).toList();
        }
    }
}
