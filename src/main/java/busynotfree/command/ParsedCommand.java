package busynotfree.command;

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
     */
    public ParsedCommand(String entityType, String action, String argumentText) {
        this.entityType = entityType;
        this.action = action;
        this.argumentText = argumentText;
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
