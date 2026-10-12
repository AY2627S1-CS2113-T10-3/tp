package busynotfree.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests shared messages and cause preservation across the application exception hierarchy.
 */
class BusyNotFreeExceptionTest {
    @Test
    public void constructor_messageOnly_preservesMessageForEverySubtype() {
        String message = "Validation failed.";
        List<BusyNotFreeException> exceptions = List.of(new CommandException(message), new ProjectException(message),
                new TaskException(message), new EventException(message));

        for (BusyNotFreeException exception : exceptions) {
            assertEquals(message, exception.getMessage());
            assertNull(exception.getCause());
        }
    }

    @Test
    public void constructor_messageAndCause_preservesCauseForEverySubtype() {
        String message = "Validation failed.";
        Throwable cause = new IllegalArgumentException("Original failure.");
        List<BusyNotFreeException> exceptions = List.of(new CommandException(message, cause),
                new ProjectException(message, cause), new TaskException(message, cause),
                new EventException(message, cause));

        for (BusyNotFreeException exception : exceptions) {
            assertEquals(message, exception.getMessage());
            assertSame(cause, exception.getCause());
        }
    }
}
