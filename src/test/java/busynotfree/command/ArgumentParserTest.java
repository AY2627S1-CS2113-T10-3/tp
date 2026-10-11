package busynotfree.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

import org.junit.jupiter.api.Test;

import busynotfree.exception.CommandException;

/**
 * Tests prefix validation, multi-word values and strict calendar-date parsing.
 */
class ArgumentParserTest {
    private final ArgumentParser parser = new ArgumentParser();

    @Test
    public void parse_projectArguments_preservesMultiWordName() {
        Map<String, String> values = parser.parse(
                "client/C001 name/Website  redesign due/2026-11-30", "client", "name", "due");

        assertEquals(Map.of("client", "C001", "name", "Website  redesign", "due", "2026-11-30"), values);
    }

    @Test
    public void parse_reorderedPrefixes_acceptsWhitespace() {
        Map<String, String> values = parser.parse(
                " \tdate/2026-10-15\tname/ Create wireframe  project/P001  ", "project", "name", "date");

        assertEquals(Map.of("project", "P001", "name", "Create wireframe", "date", "2026-10-15"), values);
    }

    @Test
    public void parse_slashWithinValue_preservesValue() {
        Map<String, String> values = parser.parse("name/Website/redesign", "name");

        assertEquals("Website/redesign", values.get("name"));
    }

    @Test
    public void parse_missingPrefix_throwsException() {
        CommandException exception = assertThrows(CommandException.class,
                () -> parser.parse("client/C001 name/Website", "client", "name", "due"));

        assertEquals("Missing required prefix: due/.", exception.getMessage());
    }

    @Test
    public void parse_noArguments_throwsException() {
        assertThrows(CommandException.class, () -> parser.parse("", "client", "name", "due"));
    }

    @Test
    public void parse_blankValues_throwsException() {
        assertThrows(CommandException.class,
                () -> parser.parse("client/C001 name/ \t due/2026-11-30", "client", "name", "due"));
        assertThrows(CommandException.class, () -> parser.parse("name/ ", "name"));
    }

    @Test
    public void parse_repeatedPrefix_throwsException() {
        CommandException exception = assertThrows(CommandException.class,
                () -> parser.parse("name/Website name/Other", "name"));

        assertEquals("Repeated prefix: name/.", exception.getMessage());
    }

    @Test
    public void parse_unexpectedPrefix_throwsException() {
        CommandException exception = assertThrows(CommandException.class,
                () -> parser.parse("project/P001 name/Wireframe due/2026-10-15", "project", "name", "date"));

        assertEquals("Unexpected prefix: due/.", exception.getMessage());
    }

    @Test
    public void parse_leadingUnprefixedText_throwsException() {
        assertThrows(CommandException.class, () -> parser.parse("unexpected name/Website", "name"));
    }

    @Test
    public void parse_result_isUnmodifiable() {
        Map<String, String> values = parser.parse("name/Website", "name");

        assertThrows(UnsupportedOperationException.class, () -> values.put("name", "Other"));
    }

    @Test
    public void parseDate_validLeapDay_returnsDate() {
        assertEquals(LocalDate.of(2028, 2, 29), parser.parseDate("2028-02-29"));
    }

    @Test
    public void parseDate_impossibleDate_throwsException() {
        assertThrows(CommandException.class, () -> parser.parseDate("2026-02-29"));
        assertThrows(CommandException.class, () -> parser.parseDate("2026-04-31"));
        assertThrows(CommandException.class, () -> parser.parseDate("2026-13-01"));
    }

    @Test
    public void parseDate_invalidCalendarDate_preservesCause() {
        CommandException exception = assertThrows(CommandException.class, () -> parser.parseDate("2026-02-30"));

        assertEquals("Use a valid date in YYYY-MM-DD format.", exception.getMessage());
        assertInstanceOf(DateTimeParseException.class, exception.getCause());
    }

    @Test
    public void parseDate_inexactFormat_throwsException() {
        assertThrows(CommandException.class, () -> parser.parseDate("2026-1-01"));
        assertThrows(CommandException.class, () -> parser.parseDate("01-01-2026"));
        assertThrows(CommandException.class, () -> parser.parseDate("2026-01-01 extra"));
    }
}
