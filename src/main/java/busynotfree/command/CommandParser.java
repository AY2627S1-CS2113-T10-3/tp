package busynotfree.command;

import busynotfree.exception.CommandException;

/**
 * Extracts the entity type, action and argument text from raw input.
 * Adapted from the MimiMeow individual project's CommandParser class.
 */
public class CommandParser {
    /**
     * Parses a dotted command name while preserving spaces within its argument text.
     * Does not check supported operations or parse argument prefixes.
     *
     * @param userInput Non-null raw user input.
     * @return Entity type, action and argument text.
     * @throws CommandException If the command name is not in the form target.action.
     */
    public ParsedCommand parse(String userInput) {
        String[] commandParts = userInput.trim().split("\\s+", 2);
        String commandWord = commandParts[0];
        String argumentText = commandParts.length > 1 ? commandParts[1] : "";
        return parseCommandWord(commandWord, argumentText);
    }

    /**
     * Splits and validates a command name before combining it with its argument text.
     * For project.add client/CLIENT_ID name/PROJECT_NAME due/YYYY-MM-DD:
     * wordParts[0] is project, and wordParts[1] is add.
     * The remaining prefixes stay in argumentText.
     */
    private ParsedCommand parseCommandWord(String word, String argumentText) {
        String[] wordParts = word.split("\\.", -1);
        if (wordParts.length != 2 || wordParts[0].isBlank() || wordParts[1].isBlank()) {
            throw new CommandException("Use target.action, such as project.add.");
        }
        return new ParsedCommand(wordParts[0], wordParts[1], argumentText);
    }
}
