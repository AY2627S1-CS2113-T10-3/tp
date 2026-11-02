package busynotfree.storage;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;

/**
 * Reserves a data directory for one running application, from before loading until after the final save.
 * The separate lock file stays in place because replacing or deleting it could let another process
 * lock a different file while the original session is still running.
 */
public final class DataDirectoryLock implements AutoCloseable {
    private static final String IN_USE_MESSAGE = "This data folder is already in use by another BusyNotFree instance. "
            + "Close it before trying again.";

    /** Avoids opening another channel to a file already locked by this JVM. */
    private static final Set<Path> LOCKED_DIRECTORIES = new HashSet<>();

    private final Path directory;
    private final FileChannel channel;

    private DataDirectoryLock(Path directory, FileChannel channel) {
        this.directory = directory;
        this.channel = channel;
    }

    /**
     * Creates the directory if needed and acquires its exclusive session lock without waiting.
     *
     * @param dataDirectory Directory containing the application's save file.
     * @return a reservation that must remain open throughout loading, command handling and saving.
     * @throws IOException if the directory cannot be accessed or another session already owns it.
     */
    public static synchronized DataDirectoryLock acquire(Path dataDirectory) throws IOException {
        Files.createDirectories(dataDirectory);
        Path directory = dataDirectory.toRealPath();
        if (LOCKED_DIRECTORIES.contains(directory)) {
            throw new IOException(IN_USE_MESSAGE);
        }
        FileChannel channel = FileChannel.open(directory.resolve(".busynotfree.lock"),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        try {
            if (channel.tryLock() == null) {
                throw new IOException(IN_USE_MESSAGE);
            }
            LOCKED_DIRECTORIES.add(directory);
            return new DataDirectoryLock(directory, channel);
        } catch (IOException | RuntimeException exception) {
            try {
                channel.close();
            } catch (IOException closeException) {
                exception.addSuppressed(closeException);
            }
            if (exception instanceof OverlappingFileLockException) {
                throw new IOException(IN_USE_MESSAGE, exception);
            }
            throw exception;
        }
    }

    /** Releases the operating-system lock by closing its channel, keeping the lock file reusable. */
    @Override
    public void close() throws IOException {
        synchronized (DataDirectoryLock.class) {
            if (!channel.isOpen()) {
                return;
            }
            try {
                channel.close();
            } finally {
                LOCKED_DIRECTORIES.remove(directory);
            }
        }
    }
}
