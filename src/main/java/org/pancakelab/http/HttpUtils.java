package org.pancakelab.http;

import com.sun.net.httpserver.HttpExchange;
import org.pancakelab.exception.PancakeLabException;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Utility class for common HTTP response operations
 */
public class HttpUtils {

    public static void sendJson(HttpExchange exchange, int statusCode, Object response) throws IOException {
        byte[] responseBody = JsonUtil.serialize(response);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, responseBody.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBody);
        }
    }

    public static void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        ErrorResponse error = new ErrorResponse(statusCode, message);
        sendJson(exchange, statusCode, error);
    }

    public static void sendError(HttpExchange exchange, PancakeLabException exception) throws IOException {
        sendError(exchange, exception.getStatusCode(), exception.getMessage());
    }

    public static void sendNoContent(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(204, -1);
    }

    public static void sendMethodNotAllowed(HttpExchange exchange) throws IOException {
        sendError(exchange, 405, "Method not allowed");
    }

    public static void sendNotFound(HttpExchange exchange, String resource) throws IOException {
        sendError(exchange, 404, resource + " not found");
    }

    public static void sendBadRequest(HttpExchange exchange, String message) throws IOException {
        sendError(exchange, 400, message);
    }

    public static void sendInternalServerError(HttpExchange exchange, String message) throws IOException {
        sendError(exchange, 500, message != null ? message : "Internal Server Error");
    }

    /**
     * Standard error response format
     */
    public static class ErrorResponse {
        private final int status;
        private final String error;
        private final long timestamp;

        public ErrorResponse(int status, String error) {
            this.status = status;
            this.error = error;
            this.timestamp = System.currentTimeMillis();
        }

        public int getStatus() {
            return status;
        }

        public String getError() {
            return error;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}
