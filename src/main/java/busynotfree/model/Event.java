package busynotfree.model;

import java.time.LocalDate;

/**
 * Represents a dated meeting, review or other event belonging to one project.
 */
public final class Event extends ScheduledItem {
    /**
     * Creates an event without automatically adding it to its project.
     *
     * @param eventId unique event ID assigned by the application's model
     * @param project project that owns the event
     * @param description non-blank description of the event
     * @param date calendar date of the event
     * @throws IllegalArgumentException if required data is missing
     */
    public Event(String eventId, Project project, String description, LocalDate date) {
        super(eventId, project, description, date);
    }

    /** Returns the event type for polymorphic messages. */
    @Override
    public String getType() {
        return "event";
    }
}
