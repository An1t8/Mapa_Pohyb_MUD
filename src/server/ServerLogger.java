package server;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class ServerLogger {
    private final Path logPath;

    public ServerLogger(String filePath) {
        this.logPath = Path.of(filePath);
    }

    public synchronized void log(String level, String message) {
        String line = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                + " [" + level + "] " + message + System.lineSeparator();
        try {
            Files.createDirectories(logPath.getParent());
            Files.writeString(logPath, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }
}
