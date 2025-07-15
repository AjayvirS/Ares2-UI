package de.tum.cit.ui;

import de.tum.cit.ui.http.LogServer;

import java.io.IOException;

public class Launcher {
    public static void main(String[] args) throws IOException {
        MainApp.launch(MainApp.class, "--logPort=9001");
    }
}
