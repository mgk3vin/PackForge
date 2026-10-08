package me.mangokevin.packforge;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/** Tiny web server that hands out the current pack.zip. */
final class PackHost {

    private final HttpServer server;
    private final ExecutorService executor;

    PackHost(String bind, int port, Supplier<byte[]> pack) throws IOException {
        server = HttpServer.create(new InetSocketAddress(bind, port), 0);
        executor = Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "PackForge-Host");
            thread.setDaemon(true);
            return thread;
        });
        server.setExecutor(executor);
        server.createContext("/", exchange -> {
            try (exchange) {
                byte[] zip = pack.get();
                if (zip == null || !exchange.getRequestMethod().equals("GET")) {
                    exchange.sendResponseHeaders(404, -1);
                    return;
                }
                exchange.getResponseHeaders().set("Content-Type", "application/zip");
                exchange.sendResponseHeaders(200, zip.length);
                try (OutputStream body = exchange.getResponseBody()) {
                    body.write(zip);
                }
            }
        });
        server.start();
    }

    void stop() {
        server.stop(0);
        executor.shutdownNow();
    }
}
