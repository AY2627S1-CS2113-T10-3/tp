package busynotfree.model;

import java.time.LocalDate;

/**
 * Stores the immutable data shared by a project's dated tasks and events.
 * This abstract implementation type does not introduce an Activity entity or ID.
 */
public abstract class ScheduledItem {
    private final String id;
    private final Project project;
    private final String description;
    private final LocalDate date;

    /**
     * Creates a dated item with an externally assigned ID and owning project.
     *
     * @param id type-specific ID assigned by the application's model
     * @param project project that owns this item
     * @param description non-blank description
     * @param date calendar date of the item
     * @throws IllegalArgumentException if required data is missing
     */
    protected ScheduledItem(String id, Project project, String description, LocalDate date) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("An item ID must not be blank.");
        }
        if (project == null) {
            throw new IllegalArgumentException("An item must belong to a project.");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("An item description must not be blank.");
        }
        if (date == null) {
            throw new IllegalArgumentException("An item date is required.");
        }
        this.id = id.trim();
        this.project = project;
        this.description = description.trim();
        this.date = date;
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
     * @return task or event
     */
    public abstract String getType();

    /** Returns this item with its type-specific ID, description and date. */
    @Override
    public String toString() {
        return "[" + id + "] " + description + " - " + date;
    }
}
