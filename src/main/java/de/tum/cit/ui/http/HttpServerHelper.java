package de.tum.cit.ui.http;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.Executors;

public final class HttpServerHelper {
    private HttpServerHelper() {}

    public static void start(int port) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/logs", exchange -> {
                String msg = "Phobos UI alive @ " + Instant.now();
                exchange.sendResponseHeaders(200, msg.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(msg.getBytes(StandardCharsets.UTF_8));
                }
            });
            server.setExecutor(Executors.newSingleThreadExecutor());
            server.start();
            System.out.printf("Log endpoint running on http://localhost:%d/logs%n", port);
        } catch (IOException e) {
            System.err.println("HTTP helper failed: " + e.getMessage());
        }
    }
}
