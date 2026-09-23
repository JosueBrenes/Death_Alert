package com.josuebrenes.toquedeathalert.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Single logging entry point, so every TOQUE line is prefixed and easy to grep. */
public final class ToqueLog {
    private static final Logger LOGGER = LoggerFactory.getLogger("TOQUE");
    private static final String PREFIX = "[TOQUE] ";

    private ToqueLog() {
    }

    public static void info(String message, Object... args) {
        LOGGER.info(PREFIX + message, args);
    }

    public static void warn(String message, Object... args) {
        LOGGER.warn(PREFIX + message, args);
    }
}
