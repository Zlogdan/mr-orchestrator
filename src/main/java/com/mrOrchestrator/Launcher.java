package com.mrOrchestrator;

import javafx.application.Application;

/**
 * Launcher для запуска JavaFX-приложения из fat jar.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        Application.launch(Main.class, args);
    }
}
