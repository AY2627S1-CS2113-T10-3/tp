package busynotfree.command;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import busynotfree.exception.CommandException;

/**
 * Parses required prefix/value arguments and strict calendar dates.
 * Used by the add and edit commands for projects, tasks and events, and by client commands.
 */
public final class ArgumentParser {
    /** Reserved argument names across commands, including those disallowed by the current command. */
    private static final Set<String> RECOGNIZED_PREFIXES = Set.of("client", "project", "name", "due", "date");
    private static final Pattern PREFIX_PATTERN = Pattern.compile("(?<!\\S)([A-Za-z][A-Za-z0-9]*)/");

    /**
     * Extracts each required prefix once, allowing any order and spaces within values.
     * Prefixes must start at the beginning of the input or after whitespace.
     * Recognized and required prefixes delimit arguments;
     * other slash-containing text remains part of a value.
     * Recognized prefixes are reserved and cannot appear literally after whitespace within a value.
     *
     * @param argumentText Argument text after the command word.
     * @param requiredPrefixes Allowed prefixes, without trailing slashes.
     * @return An unmodifiable map of prefixes to trimmed, non-blank values.
     * @throws CommandException If an argument is missing, repeated, blank or unexpected.
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
            if (!RECOGNIZED_PREFIXES.contains(matchedPrefix) && !allowedPrefixes.contains(matchedPrefix)) {
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
            if (!allowedPrefixes.contains(previousPrefix)) {
                throw new CommandException("Unexpected prefix: " + previousPrefix + "/.");
            }
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
