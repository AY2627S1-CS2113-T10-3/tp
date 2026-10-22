package busynotfree.model;

import java.time.LocalDate;
import java.util.function.Function;

import busynotfree.exception.BusyNotFreeException;

/**
 * Stores the data shared by a project's dated tasks and events.
 * The ID and owning project are fixed, while the description and date can be edited together.
 * This abstract implementation type does not introduce an Activity entity or ID.
 */
public abstract class ScheduledItem {
    private final String id;
    private final Project project;
    private String description;
    private LocalDate date;

    /**
     * Creates a dated item with an externally assigned ID and owning project.
     *
     * @param id Type-specific ID assigned by the application's model.
     * @param project Project that owns this item.
     * @param description Non-blank description.
     * @param date Calendar date of the item.
     * @param exceptionFactory Creates the subclass's exception from a validation message.
     * @throws BusyNotFreeException If required data is missing, using the supplied exception type.
     */
    protected ScheduledItem(String id, Project project, String description, LocalDate date,
            Function<String, ? extends BusyNotFreeException> exceptionFactory) {
        if (id == null || id.isBlank()) {
            throw exceptionFactory.apply("An item ID must not be blank.");
        }
        if (project == null) {
            throw exceptionFactory.apply("An item must belong to a project.");
        }
        validateDetails(description, date, exceptionFactory);
        this.id = id.trim();
        this.project = project;
        this.description = description.trim();
        this.date = date;
    }

    /**
     * Replaces the description and date together, leaving both unchanged if either is invalid.
     * The ID and owning project are final, so an edit cannot renumber or move the item.
     *
     * @param description New non-blank description.
     * @param date New calendar date.
     * @param exceptionFactory Creates the subclass's exception from a validation message.
     * @throws BusyNotFreeException If either value is missing, using the supplied exception type.
     */
    protected final void updateDetails(String description, LocalDate date,
            Function<String, ? extends BusyNotFreeException> exceptionFactory) {
        validateDetails(description, date, exceptionFactory);
        this.description = description.trim();
        this.date = date;
    }

    /** Checks the editable fields so creation and editing apply the same rules. */
    private static void validateDetails(String description, LocalDate date,
            Function<String, ? extends BusyNotFreeException> exceptionFactory) {
        if (description == null || description.isBlank()) {
            throw exceptionFactory.apply("An item description must not be blank.");
        }
        if (date == null) {
            throw exceptionFactory.apply("An item date is required.");
        }
    }

    public String getId() {
        return id;
    }

    public Project getProject() {
        return project;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getDate() {
        return date;
    }

    /**
     * Returns the item type for messages without requiring a subclass cast.
     *
     * @return Task or event.
     */
    public abstract String getType();

    /** Returns this item with its type-specific ID, description and date. */
    @Override
    public String toString() {
        return "[" + id + "] " + description + " - " + date;
    }
}
