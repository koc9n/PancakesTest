package org.pancakelab.http;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.pancakelab.config.Configuration;
import org.pancakelab.service.ServiceFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class PancakeHttpServer {
    private final HttpServer server;
    private final ExecutorService executor;
    private final Configuration config;

    public PancakeHttpServer(int port, int poolSize, ServiceFactory serviceFactory) throws IOException {
        this.config = Configuration.getInstance();
        server = HttpServer.create(new InetSocketAddress(port), config.getServerBacklogSize());
        executor = createExecutor(poolSize);
        server.setExecutor(executor);

        // Register handlers with timeout wrapper and dependency injection
        HttpHandler orderHandler = new TimeoutHandler(new OrderHandler(serviceFactory), config.getRequestTimeoutMs());
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
            // Allow configured time for existing requests to complete
            executor.shutdown();
            if (!executor.awaitTermination(config.getShutdownTimeoutSeconds(), TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
