package busynotfree.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;

import busynotfree.exception.CommandException;

/**
 * Tests command-specific text prefixes without adding reservations to project names.
 */
class ClientArgumentParserTest {
    private final ArgumentParser parser = new ArgumentParser();

    @Test
    public void parse_textPrefix_preservesNoteContent() {
        assertEquals(Map.of("text", "Review UI/UX and src/main"),
                parser.parse("text/Review UI/UX and src/main", "text"));
    }

    @Test
    public void parse_duplicateTextPrefix_rejectsReplacement() {
        CommandException exception = assertThrows(CommandException.class,
                () -> parser.parse("text/First text/Second", "text"));

        assertEquals("Repeated prefix: text/.", exception.getMessage());
    }

    @Test
    public void parse_projectNameWithTextSlash_preservesExistingBehavior() {
        Map<String, String> values = parser.parse(
                "client/C001 name/Review text/example due/2026-11-30", "client", "name", "due");

        assertEquals("Review text/example", values.get("name"));
    }

    @Test
    public void parse_textWithinClientName_remainsLiteral() {
        assertEquals(Map.of("name", "Acme text/studio"), parser.parse("name/Acme text/studio", "name"));
    }
}
