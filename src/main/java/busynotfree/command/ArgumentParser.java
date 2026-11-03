package busynotfree.command;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import busynotfree.exception.CommandException;

/**
 * Parses required prefix/value arguments and strict calendar dates.
 * Used by the add and edit commands for projects, tasks and events, and by client commands.
 */
public final class ArgumentParser {
    private static final Pattern PREFIX_PATTERN = Pattern.compile("(?<!\\S)([A-Za-z][A-Za-z0-9]*)/");

    /**
     * Extracts each required prefix once, allowing any order and spaces within values.
     * Prefixes must start at the beginning of the input or after whitespace.
     * Only the command's own prefixes delimit arguments, so they cannot appear after whitespace
     * within a value. Other slash-containing text, including other commands' prefixes such as
     * date/ in a note, remains part of a value.
     *
     * @param argumentText Argument text after the command word.
     * @param requiredPrefixes Allowed prefixes, without trailing slashes.
     * @return An unmodifiable map of prefixes to trimmed, non-blank values.
     * @throws CommandException If an argument is missing, repeated, blank or not first.
     */
    public Map<String, String> parse(String argumentText, String... requiredPrefixes) {
        assert requiredPrefixes.length > 0 : "A command must require at least one prefix.";
        List<String> allowedPrefixes = List.of(requiredPrefixes);
        Map<String, String> arguments = new LinkedHashMap<>();
        Matcher matcher = PREFIX_PATTERN.matcher(argumentText);
        String previousPrefix = null;
        int valueStart = 0;
        while (matcher.find()) {
            String matchedPrefix = matcher.group(1);
            if (!allowedPrefixes.contains(matchedPrefix)) {
                continue;
            }
            if (previousPrefix == null) {
                if (!argumentText.substring(0, matcher.start()).isBlank()) {
                    throw new CommandException("Arguments must start with a prefix such as name/.");
                }
            } else {
                putArgument(arguments, previousPrefix, argumentText.substring(valueStart, matcher.start()));
            }
            previousPrefix = matchedPrefix;
            valueStart = matcher.end();
        }
        if (previousPrefix != null) {
            putArgument(arguments, previousPrefix, argumentText.substring(valueStart));
        }
        validateRequiredArguments(arguments, allowedPrefixes);
        assert arguments.size() == allowedPrefixes.size()
                : "Each required prefix must have exactly one value.";
        return Map.copyOf(arguments);
    }

    /**
     * Parses an exact YYYY-MM-DD date without correcting impossible calendar dates.
     *
     * @param dateText Date supplied by the user.
     * @return The validated calendar date.
     * @throws CommandException If the format or calendar date is invalid.
     */
    public LocalDate parseDate(String dateText) {
        Objects.requireNonNull(dateText, "Date text must come from a parsed date argument.");
        if (!dateText.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
            throw new CommandException("Use a valid date in YYYY-MM-DD format.");
        }
        try {
            return LocalDate.parse(dateText);
        } catch (DateTimeParseException exception) {
            throw new CommandException("Use a valid date in YYYY-MM-DD format.", exception);
        }
    }

    /** Stores one non-blank value without silently replacing an earlier argument. */
    private void putArgument(Map<String, String> arguments, String prefix, String value) {
        if (arguments.containsKey(prefix)) {
            throw new CommandException("Repeated prefix: " + prefix + "/.");
        }
        if (value.isBlank()) {
            throw new CommandException("A value is required after " + prefix + "/.");
        }
        arguments.put(prefix, value.trim());
    }

    /** Checks for missing arguments in the command's documented prefix order. */
    private void validateRequiredArguments(Map<String, String> arguments, List<String> requiredPrefixes) {
        for (String prefix : requiredPrefixes) {
            if (!arguments.containsKey(prefix)) {
                throw new CommandException("Missing required prefix: " + prefix + "/.");
            }
        }
    }
}
