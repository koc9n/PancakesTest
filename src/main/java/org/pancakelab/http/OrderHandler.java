package org.pancakelab.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.pancakelab.http.dto.CreateOrderRequest;
import org.pancakelab.http.dto.CreatePancakeResponse;
import org.pancakelab.http.dto.IngredientRequest;
import org.pancakelab.http.dto.OrderResponse;
import org.pancakelab.http.validation.RequestValidator;
import org.pancakelab.http.validation.ValidationException;
import org.pancakelab.model.Order;
import org.pancakelab.service.PancakeService;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OrderHandler implements HttpHandler {
    private final PancakeService pancakeService;
    private final RateLimiter rateLimiter;
    private final Pattern orderPattern = Pattern.compile("/api/orders/([^/]+)/?$");
    private final Pattern pancakePattern = Pattern.compile("/api/orders/([^/]+)/pancakes/?$");
    private final Pattern ingredientPattern = Pattern.compile("/api/orders/([^/]+)/pancakes/([^/]+)/ingredient/?$");

    public OrderHandler() {
        this.pancakeService = PancakeService.getInstance();
        this.rateLimiter = new RateLimiter();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String clientIp = exchange.getRemoteAddress().getAddress().getHostAddress();

        if (!rateLimiter.allowRequest(clientIp)) {
            sendError(exchange, 429, "Too Many Requests");
            return;
        }

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
        } catch (ValidationException e) {
            sendError(exchange, e.getStatusCode(), e.getMessage());
        } catch (Exception e) {
            System.err.println("Error processing request from " + clientIp + ": " + e.getMessage());
            e.printStackTrace();
            sendError(exchange, 500, "Internal Server Error");
        } finally {
            exchange.close();
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

        // Validate request
        RequestValidator.validateOrderCreation(request.building(), request.room());

        Order order = pancakeService.createOrder(request.building(), request.room());
        OrderResponse response = new OrderResponse(
            order.getId().toString(),
            order.getBuilding(),
            order.getRoom(),
            pancakeService.viewOrder(order.getId())
        );
        sendResponse(exchange, 201, JsonUtil.toJson(response));
    }

    private void handleGetOrder(HttpExchange exchange, String orderId) throws IOException {
        UUID orderUUID = RequestValidator.validateUUID(orderId, "orderId");
        Order order = pancakeService.getOrder(orderUUID)
            .orElseThrow(() -> new ValidationException("Order not found", 404));

        OrderResponse response = new OrderResponse(
            orderId,
            order.getBuilding(),
            order.getRoom(),
            pancakeService.viewOrder(orderUUID)
        );
        sendResponse(exchange, 200, JsonUtil.toJson(response));
    }

    private void handleCreatePancake(HttpExchange exchange, String orderId) throws IOException {
        UUID orderUUID = RequestValidator.validateUUID(orderId, "orderId");

        // Validate order exists
        pancakeService.getOrder(orderUUID)
            .orElseThrow(() -> new ValidationException("Order not found", 404));

        UUID pancakeId = pancakeService.addPancakeToOrder(orderUUID);
        CreatePancakeResponse response = new CreatePancakeResponse(pancakeId.toString());
        sendResponse(exchange, 201, JsonUtil.toJson(response));
    }

    private void handleAddIngredient(HttpExchange exchange, String orderId, String pancakeId) throws IOException {
        UUID orderUUID = RequestValidator.validateUUID(orderId, "orderId");
        UUID pancakeUUID = RequestValidator.validateUUID(pancakeId, "pancakeId");

        IngredientRequest request = JsonUtil.fromJson(exchange.getRequestBody(), IngredientRequest.class);

        // Validate ingredient
        RequestValidator.validateIngredient(request.ingredient());

        pancakeService.addIngredientToPancake(orderUUID, pancakeUUID, request.ingredient());

        Map<String, String> response = new HashMap<>();
        response.put("status", "Ingredient added successfully");
        sendResponse(exchange, 200, JsonUtil.toJson(response));
    }

    private void handleCancelOrder(HttpExchange exchange, String orderId) throws IOException {
        UUID orderUUID = RequestValidator.validateUUID(orderId, "orderId");

        // Validate order exists
        pancakeService.getOrder(orderUUID)
            .orElseThrow(() -> new ValidationException("Order not found", 404));

        pancakeService.cancelOrder(orderUUID);
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
