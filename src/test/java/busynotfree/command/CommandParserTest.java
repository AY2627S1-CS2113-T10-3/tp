package busynotfree.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import busynotfree.exception.CommandException;

/**
 * Tests entity/action parsing while preserving unparsed argument text.
 */
class CommandParserTest {
    private final CommandParser parser = new CommandParser();

    @Test
    public void parse_projectAdd_preservesArguments() {
        ParsedCommand command = parser.parse(
                "project.add client/C001 name/Website redesign due/2026-11-30");

        assertEquals("project", command.getEntityType());
        assertEquals("add", command.getAction());
        assertEquals("client/C001 name/Website redesign due/2026-11-30", command.getArgumentText());
    }

    @Test
    public void parse_taskAdd_preservesArguments() {
        ParsedCommand command = parser.parse(
                "task.add project/P001 name/Create wireframe date/2026-10-15");

        assertEquals("task", command.getEntityType());
        assertEquals("add", command.getAction());
        assertEquals("project/P001 name/Create wireframe date/2026-10-15", command.getArgumentText());
    }

    @Test
    public void parse_eventAdd_preservesArguments() {
        ParsedCommand command = parser.parse(
                "event.add project/P001 name/Stakeholder meeting date/2026-10-20");

        assertEquals("event", command.getEntityType());
        assertEquals("add", command.getAction());
        assertEquals("project/P001 name/Stakeholder meeting date/2026-10-20", command.getArgumentText());
    }

    @Test
    public void parse_extraWhitespace_preservesInternalSpaces() {
        ParsedCommand command = parser.parse("  task.add\t  project/P001 name/Create  wireframe  ");

        assertEquals("task", command.getEntityType());
        assertEquals("add", command.getAction());
        assertEquals("project/P001 name/Create  wireframe", command.getArgumentText());
    }

    @Test
    public void parse_noArguments_returnsEmptyArguments() {
        ParsedCommand command = parser.parse("project.add");

        assertEquals("project", command.getEntityType());
        assertEquals("add", command.getAction());
        assertEquals("", command.getArgumentText());
    }

    @Test
    public void parse_emptyInput_throwsException() {
        assertThrows(CommandException.class, () -> parser.parse(""));
    }

    @Test
    public void parse_blankInput_throwsException() {
        assertThrows(CommandException.class, () -> parser.parse(" \t "));
    }

    @Test
    public void parse_otherAction_preservesAction() {
        ParsedCommand command = parser.parse("project.list");

        assertEquals("project", command.getEntityType());
        assertEquals("list", command.getAction());
        assertEquals("", command.getArgumentText());
    }

    @Test
    public void parse_otherEntity_preservesEntityType() {
        ParsedCommand command = parser.parse("client.add name/Acme");

        assertEquals("client", command.getEntityType());
        assertEquals("add", command.getAction());
        assertEquals("name/Acme", command.getArgumentText());
    }

    @Test
    public void parse_missingDot_throwsException() {
        assertThrows(CommandException.class, () -> parser.parse("project"));
    }

    @Test
    public void parse_missingAction_throwsException() {
        assertThrows(CommandException.class, () -> parser.parse("task. project/P001"));
    }

    @Test
    public void parse_missingEntity_throwsException() {
        assertThrows(CommandException.class, () -> parser.parse(".add name/Website"));
    }

    @Test
    public void parse_extraDot_throwsException() {
        assertThrows(CommandException.class, () -> parser.parse("event.add.more project/P001"));
    }
}
