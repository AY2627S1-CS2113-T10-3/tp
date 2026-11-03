package busynotfree.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests exclusive directory reservations and reuse without deleting the persistent lock file. */
class DataDirectoryLockTest {
    @TempDir
    private Path directory;

    @Test
    public void acquire_sameDirectory_rejectsUntilFirstSessionCloses() throws IOException {
        Path data = directory.resolve("data");
        try (DataDirectoryLock first = DataDirectoryLock.acquire(data)) {
            IOException exception = assertThrows(IOException.class, () -> DataDirectoryLock.acquire(data));

            assertTrue(exception.getMessage().contains("already in use by another BusyNotFree instance"));
            assertThrows(IOException.class, () -> DataDirectoryLock.acquire(data.resolve(".")));
        }

        assertTrue(Files.exists(data.resolve(".busynotfree.lock")));
        try (DataDirectoryLock next = DataDirectoryLock.acquire(data)) {
            assertThrows(IOException.class, () -> DataDirectoryLock.acquire(data));
        }
    }

    @Test
    public void acquire_differentDirectories_allowsIndependentSessions() throws IOException {
        Path firstDirectory = directory.resolve("first");
        Path secondDirectory = directory.resolve("second");
        try (DataDirectoryLock first = DataDirectoryLock.acquire(firstDirectory);
                DataDirectoryLock second = DataDirectoryLock.acquire(secondDirectory)) {
            assertThrows(IOException.class, () -> DataDirectoryLock.acquire(firstDirectory));
            assertThrows(IOException.class, () -> DataDirectoryLock.acquire(secondDirectory));
        }
    }

    @Test
    public void acquire_pathIsRegularFile_reportsFailureWithoutChangingIt() throws IOException {
        Path file = directory.resolve("data");
        Files.writeString(file, "Keep this file.");

        assertThrows(IOException.class, () -> DataDirectoryLock.acquire(file));
        assertEquals("Keep this file.", Files.readString(file));
    }

    @Test
    public void close_repeatedAfterNewSession_keepsNewSessionReserved() throws IOException {
        DataDirectoryLock first = DataDirectoryLock.acquire(directory);
        first.close();
        try (DataDirectoryLock next = DataDirectoryLock.acquire(directory)) {
            first.close();

            assertThrows(IOException.class, () -> DataDirectoryLock.acquire(directory));
        }
    }
}
