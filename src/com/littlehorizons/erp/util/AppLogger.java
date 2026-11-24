package com.littlehorizons.erp.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Minimal file logger. Every entry is appended to error_log.txt.
 */
public final class AppLogger {

    private static final Path LOG_FILE = Path.of("error_log.txt");
    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private AppLogger() {
    }

    public static void info(String message) {
        write("INFO", message, null);
    }

    public static void error(String message, Throwable cause) {
        write("ERROR", message, cause);
    }

    private static synchronized void write(String level, String message, Throwable cause) {
        StringBuilder entry = new StringBuilder();
        entry.append(LocalDateTime.now().format(TIMESTAMP))
             .append(" [").append(level).append("] ")
             .append(message).append(System.lineSeparator());

        if (cause != null) {
            StringWriter stack = new StringWriter();
            cause.printStackTrace(new PrintWriter(stack));
            entry.append(stack);
        }

        try {
            if (LOG_FILE.getParent() != null) {
                Files.createDirectories(LOG_FILE.getParent());
            }
            Files.writeString(LOG_FILE, entry.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            // Last resort: the logger itself must never crash the application.
            System.err.println("Could not write to log file: " + e.getMessage());
        }
    }
}
