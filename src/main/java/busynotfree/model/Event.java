package busynotfree.model;

import java.time.LocalDate;

import busynotfree.exception.EventException;

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
     * @throws EventException if required data is missing
     */
    public Event(String eventId, Project project, String description, LocalDate date) {
        super(eventId, project, description, date, EventException::new);
    }

    /**
     * Updates this event's description and date while preserving its ID and project.
     * Package-private so that edits go through the project manager's ID lookup.
     *
     * @param description new non-blank description of the event
     * @param date new calendar date of the event
     * @throws EventException if either value is missing; the event is then left unchanged
     */
    void edit(String description, LocalDate date) {
        updateDetails(description, date, EventException::new);
    }

    /** Returns the event type for polymorphic messages. */
    @Override
    public String getType() {
        return "event";
    }
}
