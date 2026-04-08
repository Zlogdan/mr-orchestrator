package com.mrOrchestrator.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Утилита логирования: пишет в файл через SLF4J и уведомляет UI
 */
public class AppLogger {

    private static final Logger log = LoggerFactory.getLogger(AppLogger.class);
    private static final AppLogger INSTANCE = new AppLogger();

    private Consumer<String> uiCallback;

    private AppLogger() {}

    public static AppLogger getInstance() {
        return INSTANCE;
    }

    public void setUiCallback(Consumer<String> callback) {
        this.uiCallback = callback;
    }

    public void info(String message) {
        log.info(message);
        notifyUi("[INFO] " + message);
    }

    public void warn(String message) {
        log.warn(message);
        notifyUi("[WARN] " + message);
    }

    public void error(String message, Throwable t) {
        log.error(message, t);
        notifyUi("[ERROR] " + message + (t != null ? ": " + t.getMessage() : ""));
    }

    public void error(String message) {
        log.error(message);
        notifyUi("[ERROR] " + message);
    }

    private void notifyUi(String message) {
        if (uiCallback != null) {
            uiCallback.accept(message);
        }
    }
}
