package de.tum.cit.ui;

import de.tum.cit.ui.http.HttpServerHelper;

public class Launcher {
    public static void main(String[] args) {
        int port = 9001;
        for (String a : args) {
            if (a.startsWith("--httpPort=")) {
                port = Integer.parseInt(a.substring("--httpPort=".length()));
            }
        }

        /* Start tiny HTTP server in parallel to JavaFX (logs endpoint) */
        HttpServerHelper.start(port);

        /* Delegate to JavaFX Application */
        MainApp.launch(MainApp.class, args);
    }
}
