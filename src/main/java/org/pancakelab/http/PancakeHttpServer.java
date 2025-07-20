package org.pancakelab.http;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.pancakelab.service.ServiceFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class PancakeHttpServer {
    private static final int REQUEST_TIMEOUT_MS = 30_000; // 30 seconds
    private final HttpServer server;
    private final ExecutorService executor;

    public PancakeHttpServer(int port, int poolSize, ServiceFactory serviceFactory) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 100); // Backlog size of 100
        executor = createExecutor(poolSize);
        server.setExecutor(executor);

        // Register handlers with timeout wrapper and dependency injection
        HttpHandler orderHandler = new TimeoutHandler(new OrderHandler(serviceFactory), REQUEST_TIMEOUT_MS);
        server.createContext("/api/orders", orderHandler);
    }

    private ExecutorService createExecutor(int poolSize) {
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger threadCount = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("PancakeServer-" + threadCount.getAndIncrement());
                thread.setUncaughtExceptionHandler((t, e) ->
                        System.err.println("Uncaught exception in thread " + t.getName() + ": " + e.getMessage())
                );
                return thread;
            }
        };

        return Executors.newFixedThreadPool(poolSize, threadFactory);
    }

    public void start() {
        server.start();
        System.out.println("Server started on port " + server.getAddress().getPort());
    }

    public void stop() {
        // Graceful shutdown with timeout
        server.stop(0);
        try {
            // Allow up to 30 seconds for existing requests to complete
            executor.shutdown();
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
