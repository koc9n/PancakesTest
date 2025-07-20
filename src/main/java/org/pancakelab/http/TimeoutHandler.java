package org.pancakelab.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.concurrent.*;

public class TimeoutHandler implements HttpHandler {
    private final HttpHandler delegate;
    private final int timeoutMs;
    private final ExecutorService timeoutExecutor;

    public TimeoutHandler(HttpHandler delegate, int timeoutMs) {
        this.delegate = delegate;
        this.timeoutMs = timeoutMs;
        this.timeoutExecutor = Executors.newCachedThreadPool();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Future<?> task = timeoutExecutor.submit(() -> {
            try {
                delegate.handle(exchange);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        });

        try {
            task.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            task.cancel(true);
            sendError(exchange, 408, "Request timeout");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            sendError(exchange, 500, "Request processing interrupted");
        } catch (ExecutionException e) {
            throw new IOException("Request processing failed", e.getCause());
        }
    }

    private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        byte[] response = ("{\"error\":\"" + message + "\"}").getBytes();
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }
}
