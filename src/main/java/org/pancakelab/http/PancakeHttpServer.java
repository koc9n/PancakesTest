package org.pancakelab.http;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class PancakeHttpServer {
    private final HttpServer server;

    public PancakeHttpServer(int port, int poolSize) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(poolSize));

        // Register handlers - now only orders since pancakes are handled as nested resources
        server.createContext("/api/orders", new OrderHandler());
    }

    public void start() {
        server.start();
        System.out.println("Server started on port " + server.getAddress().getPort());
    }

    public void stop() {
        server.stop(0);
    }
}
