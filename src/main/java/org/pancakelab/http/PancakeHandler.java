package org.pancakelab.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.pancakelab.http.dto.CreatePancakeRequest;
import org.pancakelab.service.PancakeService;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PancakeHandler implements HttpHandler {
    private final PancakeService pancakeService;

    public PancakeHandler() {
        this.pancakeService = PancakeService.getInstance();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            if (path.matches("/api/pancakes/?")) {
                if (method.equals("POST")) {
                    handleCreatePancake(exchange);
                } else {
                    sendError(exchange, 405, "Method not allowed");
                }
            } else if (path.matches("/api/pancakes/[^/]+/ingredients/?")) {
                if (method.equals("POST")) {
                    handleAddIngredient(exchange);
                } else {
                    sendError(exchange, 405, "Method not allowed");
                }
            } else {
                sendError(exchange, 404, "Not found");
            }
        } catch (Exception e) {
            sendError(exchange, 500, "Internal Server Error: " + e.getMessage());
        }
    }

    private void handleCreatePancake(HttpExchange exchange) throws IOException {
        CreatePancakeRequest request = JsonUtil.fromJson(exchange.getRequestBody(), CreatePancakeRequest.class);
        UUID pancakeId = pancakeService.createPancake(UUID.fromString(request.orderId()));

        Map<String, String> response = new HashMap<>();
        response.put("pancakeId", pancakeId.toString());
        sendResponse(exchange, 201, JsonUtil.toJson(response));
    }

    private void handleAddIngredient(HttpExchange exchange) throws IOException {
        String pancakeId = exchange.getRequestURI().getPath().split("/")[3];
        Map<String, Object> request = JsonUtil.fromJson(exchange.getRequestBody(), Map.class);
        String orderId = request.get("orderId").toString();
        String ingredient = request.get("ingredient").toString();

        pancakeService.addIngredientToPancake(
            UUID.fromString(orderId),
            UUID.fromString(pancakeId),
            ingredient
        );

        Map<String, String> response = new HashMap<>();
        response.put("status", "Ingredient added successfully");
        sendResponse(exchange, 200, JsonUtil.toJson(response));
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] responseBytes = response.getBytes();
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        sendResponse(exchange, statusCode, JsonUtil.toJson(error));
    }
}
