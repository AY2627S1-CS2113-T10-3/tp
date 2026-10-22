package busynotfree.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import busynotfree.exception.EventException;

/**
 * Tests event data, validation and behaviour through the shared scheduled-item type.
 */
class EventTest {
    private final Project project = new Project("P001", "C001", "Website redesign", LocalDate.of(2026, 11, 30));
    private final LocalDate date = LocalDate.of(2026, 10, 20);

    @Test
    public void constructor_validData_preservesFields() {
        Event event = new Event("E001", project, "Stakeholder meeting", date);

        assertEquals("E001", event.getId());
        assertSame(project, event.getProject());
        assertEquals("Stakeholder meeting", event.getDescription());
        assertEquals(date, event.getDate());
        assertEquals(0, project.getItemCount());
    }

    @Test
    public void constructor_extraWhitespace_trimsDescription() {
        Event event = new Event("E001", project, "  Stakeholder  meeting  ", date);

        assertEquals("Stakeholder  meeting", event.getDescription());
    }

    @Test
    public void constructor_missingDescription_throwsException() {
        assertThrows(EventException.class, () -> new Event("E001", project, " \t ", date));
        assertThrows(EventException.class, () -> new Event("E001", project, null, date));
    }

    @Test
    public void constructor_missingId_throwsException() {
        assertThrows(EventException.class, () -> new Event("", project, "Stakeholder meeting", date));
        assertThrows(EventException.class, () -> new Event(null, project, "Stakeholder meeting", date));
    }

    @Test
    public void constructor_missingProject_throwsException() {
        assertThrows(EventException.class, () -> new Event("E001", null, "Stakeholder meeting", date));
    }

    @Test
    public void constructor_missingDate_throwsException() {
        assertThrows(EventException.class, () -> new Event("E001", project, "Stakeholder meeting", null));
    }

    @Test
    public void getType_baseReference_returnsEventType() {
        ScheduledItem item = new Event("E001", project, "Stakeholder meeting", date);

        assertEquals("event", item.getType());
        assertEquals("[E001] Stakeholder meeting - 2026-10-20", item.toString());
    }

    @Test
    public void edit_validDetails_updatesDetailsAndPreservesIdAndProject() {
        Event event = new Event("E001", project, "Stakeholder meeting", date);
        LocalDate newDate = LocalDate.of(2026, 12, 2);

        event.edit("  Rescheduled  meeting  ", newDate);

        assertEquals("Rescheduled  meeting", event.getDescription());
        assertEquals(newDate, event.getDate());
        assertEquals("E001", event.getId());
        assertSame(project, event.getProject());
        assertEquals("[E001] Rescheduled  meeting - 2026-12-02", event.toString());
    }

    @Test
    public void edit_invalidDetails_throwsAndLeavesEventUnchanged() {
        Event event = new Event("E001", project, "Stakeholder meeting", date);
        LocalDate newDate = LocalDate.of(2026, 12, 2);

        assertThrows(EventException.class, () -> event.edit(" \t ", newDate));
        assertThrows(EventException.class, () -> event.edit(null, newDate));
        assertThrows(EventException.class, () -> event.edit("Rescheduled meeting", null));

        assertEquals("Stakeholder meeting", event.getDescription());
        assertEquals(date, event.getDate());
    }
}
