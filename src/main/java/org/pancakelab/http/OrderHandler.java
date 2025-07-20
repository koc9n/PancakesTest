package org.pancakelab.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.pancakelab.http.dto.CreateOrderRequest;
import org.pancakelab.http.dto.OrderResponse;
import org.pancakelab.model.Order;
import org.pancakelab.service.PancakeService;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OrderHandler implements HttpHandler {
    private final PancakeService pancakeService;
    private final Pattern orderPattern = Pattern.compile("/api/orders/([^/]+)/?$");
    private final Pattern pancakePattern = Pattern.compile("/api/orders/([^/]+)/pancakes/?$");
    private final Pattern ingredientPattern = Pattern.compile("/api/orders/([^/]+)/pancakes/([^/]+)/ingredient/?$");

    public OrderHandler() {
        this.pancakeService = new PancakeService();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            if (path.equals("/api/orders")) {
                if ("POST".equals(method)) {
                    handleCreateOrder(exchange);
                } else {
                    sendError(exchange, 405, "Method not allowed");
                }
            } else {
                Matcher orderMatcher = orderPattern.matcher(path);
                Matcher pancakeMatcher = pancakePattern.matcher(path);
                Matcher ingredientMatcher = ingredientPattern.matcher(path);

                if (orderMatcher.matches()) {
                    handleOrderEndpoint(exchange, orderMatcher.group(1));
                } else if (pancakeMatcher.matches()) {
                    handlePancakeEndpoint(exchange, pancakeMatcher.group(1));
                } else if (ingredientMatcher.matches()) {
                    handleIngredientEndpoint(exchange, ingredientMatcher.group(1), ingredientMatcher.group(2));
                } else {
                    sendError(exchange, 404, "Not found");
                }
            }
        } catch (Exception e) {
            sendError(exchange, 500, "Internal Server Error: " + e.getMessage());
        }
    }

    private void handleOrderEndpoint(HttpExchange exchange, String orderId) throws IOException {
        switch (exchange.getRequestMethod()) {
            case "GET" -> handleGetOrder(exchange, orderId);
            case "DELETE" -> handleCancelOrder(exchange, orderId);
            default -> sendError(exchange, 405, "Method not allowed");
        }
    }

    private void handlePancakeEndpoint(HttpExchange exchange, String orderId) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {
            handleCreatePancake(exchange, orderId);
        } else {
            sendError(exchange, 405, "Method not allowed");
        }
    }

    private void handleIngredientEndpoint(HttpExchange exchange, String orderId, String pancakeId) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {
            handleAddIngredient(exchange, orderId, pancakeId);
        } else {
            sendError(exchange, 405, "Method not allowed");
        }
    }

    private void handleCreateOrder(HttpExchange exchange) throws IOException {
        CreateOrderRequest request = JsonUtil.fromJson(exchange.getRequestBody(), CreateOrderRequest.class);
        Order order = pancakeService.createOrder(request.getBuilding(), request.getRoom());
        OrderResponse response = new OrderResponse(
            order.getId().toString(),
            order.getBuilding(),
            order.getRoom(),
            pancakeService.viewOrder(order.getId())
        );
        sendResponse(exchange, 201, JsonUtil.toJson(response));
    }

    private void handleGetOrder(HttpExchange exchange, String orderId) throws IOException {
        UUID orderUUID = UUID.fromString(orderId);
        Order order = pancakeService.getOrder(orderUUID)
            .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        OrderResponse response = new OrderResponse(
            orderId,
            order.getBuilding(),
            order.getRoom(),
            pancakeService.viewOrder(orderUUID)
        );
        sendResponse(exchange, 200, JsonUtil.toJson(response));
    }

    private void handleCreatePancake(HttpExchange exchange, String orderId) throws IOException {
        UUID pancakeId = pancakeService.createPancake(UUID.fromString(orderId));
        Map<String, String> response = new HashMap<>();
        response.put("pancakeId", pancakeId.toString());
        sendResponse(exchange, 201, JsonUtil.toJson(response));
    }

    private void handleAddIngredient(HttpExchange exchange, String orderId, String pancakeId) throws IOException {
        Map<String, Object> request = JsonUtil.fromJson(exchange.getRequestBody(), Map.class);
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

    private void handleCancelOrder(HttpExchange exchange, String orderId) throws IOException {
        pancakeService.cancelOrder(UUID.fromString(orderId));
        sendResponse(exchange, 204, "");
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] responseBytes = response.getBytes();
        exchange.sendResponseHeaders(statusCode, responseBytes.length > 0 ? responseBytes.length : -1);
        if (responseBytes.length > 0) {
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
    }

    private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        sendResponse(exchange, statusCode, JsonUtil.toJson(error));
    }
}
