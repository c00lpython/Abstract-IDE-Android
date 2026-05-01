package com.xcore.abstractide.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;

/**
 * Аналог: utils/logger.py
 * Обёртка над java.util.logging
 */
public class Logger {

    private static final java.util.logging.Logger LOGGER =
            java.util.logging.Logger.getLogger("AbstractIDE");

    static {
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        LOGGER.addHandler(handler);
        LOGGER.setLevel(Level.ALL);
    }

    public static void info(String message) {
        LOGGER.info(format(message));
    }

    public static void warn(String message) {
        LOGGER.warning(format(message));
    }

    public static void error(String message) {
        LOGGER.severe(format(message));
    }

    public static void debug(String message) {
        LOGGER.fine(format(message));
    }

    private static String format(String message) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        return "[" + time + "] " + message;
    }
}