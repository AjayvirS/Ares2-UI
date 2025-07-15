package de.tum.cit.ui.http;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.Executors;

public final class LogServer {

    private final HttpServer server;

    public LogServer(HttpServer server) {
        this.server = server;
    }

    public static LogServer start(int port) throws IOException {
        HttpServer s = HttpServer.create(new InetSocketAddress(port), 0);
        s.createContext("/logs", ex -> {
            String msg = "Phobos UI alive @ " + Instant.now();
            ex.sendResponseHeaders(200, msg.length());
            try (OutputStream os = ex.getResponseBody()) {
                os.write(msg.getBytes(StandardCharsets.UTF_8));
            }
        });
        s.setExecutor(Executors.newSingleThreadExecutor());
        s.start();
        System.out.printf("Log server running on http://localhost:%d/logs%n", port);
        return new LogServer(s);
    }


    public void stop() {
        System.out.println("Shutting down log server");
        server.stop(0);
    }
}
