package busynotfree.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import busynotfree.model.PlaceholderClientLookup;
import busynotfree.model.Project;
import busynotfree.model.ProjectManager;
import busynotfree.ui.Ui;

/**
 * Checks each add command against the same input-validation and recovery cases.
 * Every dynamic test creates its own fixture so commands cannot affect another test.
 */
class AddCommandValidationTest {
    private static final List<String> ENTITIES = List.of("project", "task", "event");
    private static final LocalDate DATE = LocalDate.of(2026, 11, 30);

    /** Checks all six argument orders for each add command. */
    @TestFactory
    public List<DynamicTest> execute_argumentOrders_createsExpectedEntity() {
        List<DynamicTest> tests = new ArrayList<>();
        List<List<Integer>> orders = List.of(List.of(0, 1, 2), List.of(0, 2, 1), List.of(1, 0, 2),
                List.of(1, 2, 0), List.of(2, 0, 1), List.of(2, 1, 0));
        for (String entity : ENTITIES) {
            List<String> arguments = validArguments(entity, "Design work", "2026-11-30");
            for (List<Integer> order : orders) {
                String input = entity + ".add " + arguments.get(order.get(0)) + " "
                        + arguments.get(order.get(1)) + " " + arguments.get(order.get(2));
                tests.add(dynamicTest(input, () -> assertSuccessfulAdd(entity, input, "Design work", DATE)));
            }
        }
        return tests;
    }

    /** Checks missing, blank and duplicate values for every required prefix. */
    @TestFactory
    public List<DynamicTest> execute_invalidRequiredArguments_reportsErrorWithoutMutation() {
        List<DynamicTest> tests = new ArrayList<>();
        for (String entity : ENTITIES) {
            List<String> arguments = validArguments(entity, "Design work", "2026-11-30");
            for (int index = 0; index < arguments.size(); index++) {
                String argument = arguments.get(index);
                String prefix = argument.substring(0, argument.indexOf('/'));
                List<String> missing = new ArrayList<>(arguments);
                missing.remove(index);
                String missingInput = entity + ".add " + String.join(" ", missing);
                tests.add(dynamicTest("Missing " + prefix + " in " + entity,
                        () -> assertRejectedAdd(entity, missingInput, "Missing required prefix: " + prefix + "/.")));

                List<String> blank = new ArrayList<>(arguments);
                blank.set(index, prefix + "/ \t ");
                String blankInput = entity + ".add " + String.join(" ", blank);
                tests.add(dynamicTest("Blank " + prefix + " in " + entity,
                        () -> assertRejectedAdd(entity, blankInput, "A value is required after " + prefix + "/.")));

                String duplicateInput = entity + ".add " + String.join(" ", arguments) + " " + argument;
                tests.add(dynamicTest("Repeated " + prefix + " in " + entity,
                        () -> assertRejectedAdd(entity, duplicateInput, "Repeated prefix: " + prefix + "/.")));
            }
        }
        return tests;
    }

    /** Checks recognized prefixes that do not belong to the current command. */
    @TestFactory
    public List<DynamicTest> execute_inappropriatePrefixes_reportsErrorWithoutMutation() {
        List<DynamicTest> tests = new ArrayList<>();
        for (String entity : ENTITIES) {
            List<String> inappropriate = entity.equals("project")
                    ? List.of("project", "date") : List.of("client", "due");
            for (String prefix : inappropriate) {
                String input = validCommand(entity) + " " + prefix + "/unexpected";
                tests.add(dynamicTest("Unexpected " + prefix + " in " + entity,
                        () -> assertRejectedAdd(entity, input, "Unexpected prefix: " + prefix + "/.")));
            }
        }
        return tests;
    }

    /** Checks impossible dates and non-exact formats without silently correcting them. */
    @TestFactory
    public List<DynamicTest> execute_invalidDates_reportsErrorWithoutMutation() {
        List<DynamicTest> tests = new ArrayList<>();
        List<String> invalidDates = List.of("2026-1-01", "2026-01-1", "26-01-01", "10000-01-01",
                "2026/01/01", "2026-00-10", "2026-13-10", "2026-01-00", "2026-01-32", "2026-04-31",
                "1900-02-29", "2100-02-29", "2026-02-29", "2026-02-30", "2026-11-31", "+2026-01-01",
                "2026-01-01T12:00", "2026-01-01 extra", "tomorrow", "２０２６-０１-０１");
        for (String entity : ENTITIES) {
            for (String date : invalidDates) {
                String input = entity + ".add " + String.join(" ", validArguments(entity, "Design work", date));
                tests.add(dynamicTest(entity + " rejects " + date,
                        () -> assertRejectedAdd(entity, input, "Use a valid date in YYYY-MM-DD format.")));
            }
        }
        return tests;
    }

    /** Checks valid month, leap-year and four-digit year boundaries. */
    @TestFactory
    public List<DynamicTest> execute_validDateBoundaries_createsExpectedEntity() {
        List<DynamicTest> tests = new ArrayList<>();
        List<LocalDate> dates = List.of(LocalDate.of(1, 1, 1), LocalDate.of(9999, 12, 31),
                LocalDate.of(2000, 2, 29), LocalDate.of(2026, 4, 30), LocalDate.of(2026, 1, 31));
        for (String entity : ENTITIES) {
            for (LocalDate date : dates) {
                String input = entity + ".add "
                        + String.join(" ", validArguments(entity, "Design work", date.toString()));
                tests.add(dynamicTest(entity + " accepts " + date,
                        () -> assertSuccessfulAdd(entity, input, "Design work", date)));
            }
        }
        return tests;
    }

    /** Checks free-text names without introducing escaping or new reserved prefixes. */
    @TestFactory
    public List<DynamicTest> execute_freeTextNames_preservesText() {
        List<DynamicTest> tests = new ArrayList<>();
        List<String> names = List.of("Create UI/UX mockup", "Review src/main changes", "Visit https://example.com",
                "Prepare café 客户 review", "Compare foo/bar and API/v2", "Keep filename/value inline",
                "Preserve  double spaces", "Discuss 'client feedback'", "Plan " + "x".repeat(2000));
        for (String entity : ENTITIES) {
            for (String name : names) {
                List<String> arguments = validArguments(entity, name, "2026-11-30");
                String input = entity + ".add " + arguments.get(0) + " " + arguments.get(2) + " " + arguments.get(1);
                tests.add(dynamicTest(entity + " preserves name " + names.indexOf(name),
                        () -> assertSuccessfulAdd(entity, input, name, DATE)));
            }
        }
        return tests;
    }

    /** Checks whitespace around the command, prefixes and values. */
    @TestFactory
    public List<DynamicTest> execute_paddedArguments_createsExpectedEntity() {
        List<DynamicTest> tests = new ArrayList<>();
        for (String entity : ENTITIES) {
            List<String> arguments = validArguments(entity, "Design work", "2026-11-30");
            List<String> padded = arguments.stream().map(argument -> argument.replace("/", "/ \t ")).toList();
            String input = " \t" + entity + ".add\t" + String.join(" \t ", padded) + " \t ";
            tests.add(dynamicTest(entity + " accepts padded arguments",
                    () -> assertSuccessfulAdd(entity, input, "Design work", DATE)));
        }
        return tests;
    }

    /** Checks the documented reservation of recognized prefix names within values. */
    @TestFactory
    public List<DynamicTest> execute_reservedPrefixInName_reportsRepeatedPrefix() {
        List<DynamicTest> tests = new ArrayList<>();
        for (String entity : ENTITIES) {
            String input = entity + ".add "
                    + String.join(" ", validArguments(entity, "Discuss name/example", "2026-11-30"));
            tests.add(dynamicTest(entity + " reserves name/",
                    () -> assertRejectedAdd(entity, input, "Repeated prefix: name/.")));
        }
        return tests;
    }

    /** Supplies valid arguments in their documented order. */
    private List<String> validArguments(String entity, String name, String date) {
        String parent = entity.equals("project") ? "client/C002" : "project/P001";
        String datePrefix = entity.equals("project") ? "due/" : "date/";
        return List.of(parent, "name/" + name, datePrefix + date);
    }

    /** Supplies a corrected command to check recovery and ID allocation after a failure. */
    private String validCommand(String entity) {
        return entity + ".add " + String.join(" ", validArguments(entity, "Design work", "2026-11-30"));
    }

    /** Verifies the stored entity as well as the success response. */
    private void assertSuccessfulAdd(String entity, String input, String name, LocalDate date) {
        CommandFixture fixture = new CommandFixture();
        assertFalse(fixture.handler.execute(input));
        assertTrue(fixture.ui.errors.isEmpty());
        assertCreatedEntity(fixture, entity, name, date);
    }

    /** Verifies failure atomicity, usage hints and successful recovery with unconsumed IDs. */
    private void assertRejectedAdd(String entity, String input, String expectedError) {
        CommandFixture fixture = new CommandFixture();
        assertFalse(fixture.handler.execute(input));
        assertEquals(List.of(expectedError), fixture.ui.errors);
        assertEquals(1, fixture.ui.messages.size());
        assertTrue(fixture.ui.messages.getFirst().startsWith("Format: " + entity + ".add "));
        assertEquals(List.of(fixture.project), fixture.manager.getProjects());
        assertEquals(0, fixture.project.getItemCount());
        fixture.ui.messages.clear();
        fixture.ui.errors.clear();

        assertFalse(fixture.handler.execute(validCommand(entity)));
        assertTrue(fixture.ui.errors.isEmpty());
        assertCreatedEntity(fixture, entity, "Design work", DATE);
    }

    /** Checks the exact ID, owner and values for the expected creation only. */
    private void assertCreatedEntity(CommandFixture fixture, String entity, String name, LocalDate date) {
        switch (entity) {
        case "project":
            Project created = fixture.manager.getProject("P002");
            assertEquals(List.of(fixture.project, created), fixture.manager.getProjects());
            assertEquals("C002", created.getClientId());
            assertEquals(name, created.getName());
            assertEquals(date, created.getDeadline());
            assertEquals(0, fixture.project.getItemCount());
            assertEquals(List.of("Created project P002 \"" + name + "\" for client C002."), fixture.ui.messages);
            break;
        case "task":
            assertEquals(List.of(fixture.project), fixture.manager.getProjects());
            assertEquals(1, fixture.project.getTasks().size());
            assertTrue(fixture.project.getEvents().isEmpty());
            assertEquals("T001", fixture.project.getTasks().getFirst().getId());
            assertSame(fixture.project, fixture.project.getTasks().getFirst().getProject());
            assertEquals(name, fixture.project.getTasks().getFirst().getDescription());
            assertEquals(date, fixture.project.getTasks().getFirst().getDate());
            assertEquals(List.of("Added task T001 \"" + name + "\" to project P001 \"Existing project\"."),
                    fixture.ui.messages);
            break;
        case "event":
            assertEquals(List.of(fixture.project), fixture.manager.getProjects());
            assertEquals(1, fixture.project.getEvents().size());
            assertTrue(fixture.project.getTasks().isEmpty());
            assertEquals("E001", fixture.project.getEvents().getFirst().getId());
            assertSame(fixture.project, fixture.project.getEvents().getFirst().getProject());
            assertEquals(name, fixture.project.getEvents().getFirst().getDescription());
            assertEquals(date, fixture.project.getEvents().getFirst().getDate());
            assertEquals(List.of("Added event E001 \"" + name + "\" to project P001 \"Existing project\"."),
                    fixture.ui.messages);
            break;
        default:
            throw new AssertionError("Unsupported test entity: " + entity);
        }
    }

    /** Provides isolated state and captured output for one command scenario. */
    private static class CommandFixture {
        private final RecordingUi ui = new RecordingUi();
        private final ProjectManager manager = new ProjectManager(new PlaceholderClientLookup());
        private final Project project = manager.addProject("C001", "Existing project", DATE);
        private final CommandHandler handler = new CommandHandler(new CommandParser(), ui, manager);
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
            // Separators are not part of the command-validation assertions.
        }
    }
}
