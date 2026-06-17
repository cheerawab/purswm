package com.grinderwolf.swm.plugin.log;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Logging utility for SWM
 */
public class Logging {

    private static final Logger LOGGER = Logger.getLogger("SWM");

    public static void info(String message) {
        LOGGER.info(message);
    }

    public static void warning(String message) {
        LOGGER.warning(message);
    }

    public static void warning(String message, Throwable t) {
        LOGGER.log(Level.WARNING, message, t);
    }

    public static void error(String message) {
        LOGGER.severe(message);
    }

    public static void error(String message, Throwable t) {
        LOGGER.log(Level.SEVERE, message, t);
    }
}
