package busynotfree.command;

import java.util.Objects;

/**
 * Stores an entity type, action and argument text without executing the command.
 * Adapted from the MimiMeow individual project's Command class.
 */
public final class ParsedCommand {
    private final String entityType;
    private final String action;
    private final String argumentText;

    /**
     * Creates a parsed command with the supplied entity type, action and argument text.
     *
     * @param entityType object category, such as project, task or event
     * @param action operation, such as add
     * @param argumentText remaining argument text, or an empty string when absent
     * @throws NullPointerException if any part is null, which indicates a parser bug
     */
    public ParsedCommand(String entityType, String action, String argumentText) {
        // Fails fast here so a parser bug is not reported later as a confusing routing error.
        this.entityType = Objects.requireNonNull(entityType, "A parsed command needs an entity type.");
        this.action = Objects.requireNonNull(action, "A parsed command needs an action.");
        this.argumentText = Objects.requireNonNull(argumentText, "Use an empty string when there are no arguments.");
    }

    public String getEntityType() {
        return entityType;
    }

    public String getAction() {
        return action;
    }

    public String getArgumentText() {
        return argumentText;
    }
}
