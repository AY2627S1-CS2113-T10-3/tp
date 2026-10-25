package busynotfree.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import busynotfree.model.Event;
import busynotfree.model.PlaceholderClientLookup;
import busynotfree.model.Project;
import busynotfree.model.ProjectManager;
import busynotfree.model.ScheduledItem;
import busynotfree.model.Task;
import busynotfree.ui.Ui;

/**
 * Tests task.edit and event.edit through the public command handler.
 * Every dynamic test creates its own fixture so commands cannot affect another test.
 */
class EditCommandTest {
    private static final List<String> ITEM_TYPES = List.of("task", "event");
    private static final LocalDate ORIGINAL_DATE = LocalDate.of(2026, 10, 15);
    private static final LocalDate NEW_DATE = LocalDate.of(2026, 12, 1);
    private static final String TASK_EDIT_FORMAT = "Format: task.edit TASK_ID name/DESCRIPTION date/YYYY-MM-DD";
    private static final String EVENT_EDIT_FORMAT = "Format: event.edit EVENT_ID name/DESCRIPTION date/YYYY-MM-DD";

    @Test
    public void execute_taskEdit_updatesTaskAndDisplaysDetails() {
        CommandFixture fixture = new CommandFixture();

        assertFalse(fixture.handler.execute("task.edit T001 name/Revised wireframe date/2026-12-01"));

        assertEquals(List.of("Updated task in project P001 \"Website\": "
                + "[T001] Revised wireframe - 2026-12-01 [In Progress]"), fixture.ui.messages);
        assertTrue(fixture.ui.errors.isEmpty());
        assertEquals("Revised wireframe", fixture.task.getDescription());
        assertEquals(NEW_DATE, fixture.task.getDate());
    }

    @Test
    public void execute_eventEdit_updatesEventAndDisplaysDetails() {
        CommandFixture fixture = new CommandFixture();

        assertFalse(fixture.handler.execute("event.edit E001 name/Rescheduled meeting date/2026-12-01"));

        assertEquals(List.of("Updated event in project P001 \"Website\": [E001] Rescheduled meeting - 2026-12-01"),
                fixture.ui.messages);
        assertTrue(fixture.ui.errors.isEmpty());
        assertEquals("Rescheduled meeting", fixture.event.getDescription());
        assertEquals(NEW_DATE, fixture.event.getDate());
    }

    @Test
    public void execute_taskEditOnCompletedTask_preservesCompletion() {
        CommandFixture fixture = new CommandFixture();
        fixture.manager.markTaskComplete("T001");

        fixture.handler.execute("task.edit T001 name/Final wireframe date/2026-12-01");

        assertTrue(fixture.task.isCompleted());
        assertEquals(List.of("Updated task in project P001 \"Website\": "
                + "[T001] Final wireframe - 2026-12-01 [COMPLETED]"), fixture.ui.messages);
    }

    /** Checks that valid input in any form edits the item while keeping its ID, project and position. */
    @TestFactory
    public List<DynamicTest> execute_validEditForms_updatesOnlyTargetItem() {
        List<String> argumentForms = List.of("name/Revised work date/2026-12-01",
                "date/2026-12-01 name/Revised work", " \t name/ Revised work \t date/ 2026-12-01 \t ");
        List<DynamicTest> tests = new ArrayList<>();
        for (String itemType : ITEM_TYPES) {
            for (String arguments : argumentForms) {
                String input = itemType + ".edit " + idOf(itemType, 1) + " " + arguments;
                tests.add(dynamicTest(input, () -> {
                    CommandFixture fixture = new CommandFixture();
                    assertFalse(fixture.handler.execute(input));
                    assertTrue(fixture.ui.errors.isEmpty());
                    ScheduledItem item = fixture.item(itemType);
                    assertEquals("Revised work", item.getDescription());
                    assertEquals(NEW_DATE, item.getDate());
                    assertEquals(idOf(itemType, 1), item.getId());
                    assertSame(fixture.project, item.getProject());
                    assertEquals(2, fixture.project.getItemCount());
                    assertEquals("Other work", fixture.otherItem(itemType).getDescription());
                }));
            }
        }
        return tests;
    }

    /** Checks free-text descriptions, including slashes that are not reserved prefixes. */
    @TestFactory
    public List<DynamicTest> execute_freeTextDescriptions_preservesText() {
        List<String> descriptions = List.of("Review UI/UX mockup", "Review src/main changes",
                "Visit https://example.com", "Preserve  double spaces", "Prepare café 客户 review");
        List<DynamicTest> tests = new ArrayList<>();
        for (String itemType : ITEM_TYPES) {
            for (String description : descriptions) {
                String input = itemType + ".edit " + idOf(itemType, 1) + " date/2026-12-01 name/" + description;
                tests.add(dynamicTest(input, () -> {
                    CommandFixture fixture = new CommandFixture();
                    fixture.handler.execute(input);
                    assertTrue(fixture.ui.errors.isEmpty());
                    assertEquals(description, fixture.item(itemType).getDescription());
                }));
            }
        }
        return tests;
    }

    /** Checks malformed input, reporting one error and a usage hint without changing the item. */
    @TestFactory
    public List<DynamicTest> execute_invalidEditInput_reportsErrorWithoutMutation() {
        List<DynamicTest> tests = new ArrayList<>();
        for (String itemType : ITEM_TYPES) {
            String id = idOf(itemType, 1);
            String missingId = "Provide the " + itemType + " ID before name/ and date/.";
            String otherTypeId = itemType.equals("task") ? "E001" : "T001";
            // Only event lookups add a recovery hint; task lookups use the existing getTask message from #63.
            String notFound = capitalize(itemType) + " %s was not found."
                    + (itemType.equals("event") ? " Use project.view to see event IDs." : "");
            List<String> arguments = List.of("", "name/Revised date/2026-12-01", id,
                    id + " date/2026-12-01", id + " name/Revised",
                    id + " name/ date/2026-12-01", id + " name/Revised date/ \t ",
                    id + " name/Revised name/Again date/2026-12-01",
                    id + " name/Revised date/2026-12-01 date/2026-12-02",
                    id + " name/Revised date/2026-02-30", id + " name/Revised date/2026-1-01",
                    id + " name/Revised date/tomorrow", id + " name/Revised date/2026-12-01 project/P002",
                    id + " name/Revised date/2026-12-01 due/2026-12-02", id + " extra name/Revised date/2026-12-01",
                    idOf(itemType, 999) + " name/Revised date/2026-12-01",
                    id.toLowerCase(Locale.ROOT) + " name/Revised date/2026-12-01",
                    otherTypeId + " name/Revised date/2026-12-01");
            List<String> errors = List.of(missingId, missingId, "Missing required prefix: name/.",
                    "Missing required prefix: name/.", "Missing required prefix: date/.",
                    "A value is required after name/.", "A value is required after date/.",
                    "Repeated prefix: name/.", "Repeated prefix: date/.",
                    "Use a valid date in YYYY-MM-DD format.", "Use a valid date in YYYY-MM-DD format.",
                    "Use a valid date in YYYY-MM-DD format.", "Unexpected prefix: project/.",
                    "Unexpected prefix: due/.", "Arguments must start with a prefix such as name/.",
                    String.format(notFound, idOf(itemType, 999)),
                    String.format(notFound, id.toLowerCase(Locale.ROOT)),
                    String.format(notFound, otherTypeId));
            for (int i = 0; i < arguments.size(); i++) {
                String input = (itemType + ".edit " + arguments.get(i)).trim();
                String error = errors.get(i);
                tests.add(dynamicTest(input, () -> assertRejectedEdit(itemType, input, error)));
            }
        }
        return tests;
    }

    @Test
    public void execute_help_listsEditCommands() {
        CommandFixture fixture = new CommandFixture();

        fixture.handler.execute("help");

        assertTrue(fixture.ui.messages.contains("task.edit      |   Edit a task"));
        assertTrue(fixture.ui.messages.contains("event.edit     |   Edit an event"));
    }

    /** Verifies the error, the usage hint, that nothing changed, and that a corrected command still works. */
    private void assertRejectedEdit(String itemType, String input, String expectedError) {
        CommandFixture fixture = new CommandFixture();
        if (itemType.equals("task")) {
            fixture.manager.markTaskComplete("T001");
        }

        assertFalse(fixture.handler.execute(input));

        assertEquals(List.of(expectedError), fixture.ui.errors);
        assertEquals(List.of(itemType.equals("task") ? TASK_EDIT_FORMAT : EVENT_EDIT_FORMAT), fixture.ui.messages);
        ScheduledItem item = fixture.item(itemType);
        assertEquals("Original work", item.getDescription());
        assertEquals(ORIGINAL_DATE, item.getDate());
        assertEquals("Other work", fixture.otherItem(itemType).getDescription());
        assertEquals(ORIGINAL_DATE, fixture.otherItem(itemType).getDate());
        assertEquals(itemType.equals("task"), fixture.task.isCompleted());
        assertEquals(1, fixture.otherProject.getTasks().size());
        fixture.ui.messages.clear();
        fixture.ui.errors.clear();

        fixture.handler.execute(itemType + ".edit " + idOf(itemType, 1) + " name/Corrected date/2026-12-01");
        assertTrue(fixture.ui.errors.isEmpty());
        assertEquals("Corrected", item.getDescription());
        assertEquals(NEW_DATE, item.getDate());
    }

    /** Returns the type-specific ID for a number, such as T001 or E999. */
    private static String idOf(String itemType, int number) {
        return String.format(Locale.ROOT, "%s%03d", itemType.equals("task") ? "T" : "E", number);
    }

    /** Converts task or event into the capitalized form used in lookup errors. */
    private static String capitalize(String text) {
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    /** Provides two projects, each with one task and one event, and captured output. */
    private static class CommandFixture {
        private final RecordingUi ui = new RecordingUi();
        private final ProjectManager manager = new ProjectManager(new PlaceholderClientLookup());
        private final Project project = manager.addProject("C001", "Website", LocalDate.of(2026, 11, 30));
        private final Project otherProject = manager.addProject("C001", "App", LocalDate.of(2026, 11, 30));
        // Created after both projects, so the other project's items become T002 and E002.
        private final Task task = manager.addTask("P001", "Original work", ORIGINAL_DATE);
        private final Event event = manager.addEvent("P001", "Original work", ORIGINAL_DATE);
        private final Task otherTask = manager.addTask("P002", "Other work", ORIGINAL_DATE);
        private final Event otherEvent = manager.addEvent("P002", "Other work", ORIGINAL_DATE);
        private final CommandHandler handler = new CommandHandler(new CommandParser(), ui, manager);

        /** Returns T001 or E001, the item each edit test targets. */
        private ScheduledItem item(String itemType) {
            return itemType.equals("task") ? task : event;
        }

        /** Returns the same-type item in the other project, which must never change. */
        private ScheduledItem otherItem(String itemType) {
            return itemType.equals("task") ? otherTask : otherEvent;
        }
    }

    /** Captures responses without changing standard output or error streams. */
    private static class RecordingUi extends Ui {
        private final List<String> messages = new ArrayList<>();
        private final List<String> errors = new ArrayList<>();

        @Override
        public void showMessage(String message) {
            messages.add(message);
        }

        @Override
        public void showError(String message) {
            errors.add(message);
        }

        @Override
        public void showSeparator() {
            // Separators are not part of the edit-command assertions.
        }
    }
}
