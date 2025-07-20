package org.pancakelab.util;

import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Centralized logger utility using Java's built-in logging framework
 */
public class Logger {
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger("PancakeLab");

    static {
        // Configure the logger
        logger.setLevel(Level.INFO);
        logger.setUseParentHandlers(false);

        // Create console handler with custom formatting
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.ALL);
        consoleHandler.setFormatter(new PancakeLabFormatter());

        logger.addHandler(consoleHandler);
    }

    public static void info(String message) {
        logger.info(message);
    }

    public static void info(String message, Object... params) {
        logger.info(String.format(message, params));
    }

    public static void warn(String message) {
        logger.warning(message);
    }

    public static void warn(String message, Object... params) {
        logger.warning(String.format(message, params));
    }

    public static void error(String message) {
        logger.severe(message);
    }

    public static void error(String message, Object... params) {
        logger.severe(String.format(message, params));
    }

    public static void error(String message, Throwable throwable) {
        logger.log(Level.SEVERE, message, throwable);
    }

    public static void debug(String message) {
        logger.fine(message);
    }

    public static void debug(String message, Object... params) {
        logger.fine(String.format(message, params));
    }

    public static void setDebugLevel() {
        logger.setLevel(Level.FINE);
        logger.getHandlers()[0].setLevel(Level.FINE);
    }

    /**
     * Custom formatter for consistent log output
     */
    private static class PancakeLabFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            return String.format("[%s] %s - %s: %s%n",
                    java.time.LocalDateTime.now(),
                    record.getLevel(),
                    record.getLoggerName(),
                    record.getMessage());
        }
    }
}
