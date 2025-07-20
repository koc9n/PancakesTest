package org.pancakelab.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.pancakelab.http.dto.*;
import org.pancakelab.http.validation.RequestValidator;
import org.pancakelab.http.validation.ValidationException;
import org.pancakelab.model.Ingredient;
import org.pancakelab.model.Order;
import org.pancakelab.model.Pancake;
import org.pancakelab.service.OrderService;
import org.pancakelab.service.PancakeService;
import org.pancakelab.service.impl.PancakeServiceImpl;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OrderHandler implements HttpHandler {
    private final OrderService orderService;
    private final PancakeService pancakeService;
    private final RateLimiter rateLimiter;

    // URL patterns for order management
    private final Pattern orderPattern = Pattern.compile("/api/orders/([^/]+)/?$");
    private final Pattern pancakePattern = Pattern.compile("/api/orders/([^/]+)/pancakes/?$");
    private final Pattern pancakeDetailPattern = Pattern.compile("/api/orders/([^/]+)/pancakes/([^/]+)/?$");
    private final Pattern ingredientPattern = Pattern.compile("/api/orders/([^/]+)/pancakes/([^/]+)/ingredients/?$");
    private final Pattern ingredientDetailPattern = Pattern.compile("/api/orders/([^/]+)/pancakes/([^/]+)/ingredients/([^/]+)/?$");

    public OrderHandler() {
        this.orderService = OrderService.getInstance();
        this.pancakeService = PancakeServiceImpl.getInstance();
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

            if (path.equals("/api/orders") && method.equals("POST")) {
                handleCreateOrder(exchange);
                return;
            }

            if (path.equals("/api/orders") && method.equals("GET")) {
                handleGetAllOrders(exchange);
                return;
            }

            Matcher orderMatcher = orderPattern.matcher(path);
            if (orderMatcher.matches()) {
                UUID orderId = UUID.fromString(orderMatcher.group(1));
                switch (method) {
                    case "GET" -> handleGetOrder(exchange, orderId);
                    case "DELETE" -> handleDeleteOrder(exchange, orderId);
                    default -> sendError(exchange, 405, "Method not allowed");
                }
                return;
            }

            // Check pancake-specific endpoints
            Matcher pancakeMatcher = pancakePattern.matcher(path);
            if (pancakeMatcher.matches()) {
                UUID orderId = UUID.fromString(pancakeMatcher.group(1));
                if (method.equals("POST")) {
                    handleCreatePancake(exchange, orderId);
                } else if (method.equals("GET")) {
                    handleGetPancakes(exchange, orderId);
                } else {
                    sendError(exchange, 405, "Method not allowed");
                }
                return;
            }

            // Check pancake detail endpoints
            Matcher pancakeDetailMatcher = pancakeDetailPattern.matcher(path);
            if (pancakeDetailMatcher.matches()) {
                UUID orderId = UUID.fromString(pancakeDetailMatcher.group(1));
                UUID pancakeId = UUID.fromString(pancakeDetailMatcher.group(2));
                if (method.equals("DELETE")) {
                    handleDeletePancake(exchange, orderId, pancakeId);
                } else {
                    sendError(exchange, 405, "Method not allowed");
                }
                return;
            }

            // Check ingredient-specific endpoints
            Matcher ingredientMatcher = ingredientPattern.matcher(path);
            if (ingredientMatcher.matches()) {
                UUID orderId = UUID.fromString(ingredientMatcher.group(1));
                UUID pancakeId = UUID.fromString(ingredientMatcher.group(2));
                switch (method) {
                    case "POST" -> handleAddIngredient(exchange, orderId, pancakeId);
                    case "GET" -> handleGetIngredients(exchange, orderId, pancakeId);
                    default -> sendError(exchange, 405, "Method not allowed");
                }
                return;
            }

            // Check ingredient detail endpoints
            Matcher ingredientDetailMatcher = ingredientDetailPattern.matcher(path);
            if (ingredientDetailMatcher.matches()) {
                UUID orderId = UUID.fromString(ingredientDetailMatcher.group(1));
                UUID pancakeId = UUID.fromString(ingredientDetailMatcher.group(2));
                UUID ingredientId = UUID.fromString(ingredientDetailMatcher.group(3));
                if (method.equals("DELETE")) {
                    handleRemoveIngredient(exchange, orderId, pancakeId, ingredientId);
                } else {
                    sendError(exchange, 405, "Method not allowed");
                }
                return;
            }

            sendError(exchange, 404, "Not Found");

        } catch (ValidationException e) {
            sendError(exchange, e.getStatusCode(), e.getMessage());
        } catch (IllegalArgumentException e) {
            sendError(exchange, 400, "Invalid request: " + e.getMessage());
        } catch (IllegalStateException e) {
            sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            sendError(exchange, 500, "Internal Server Error");
        } finally {
            exchange.close();
        }
    }

    private void handleCreateOrder(HttpExchange exchange) throws IOException {
        CreateOrderRequest request = JsonUtil.deserialize(exchange.getRequestBody(), CreateOrderRequest.class);
        try {
            RequestValidator.validateCreateOrder(request);
            Order order = orderService.createOrder(request.building(), request.room());
            sendJson(exchange, 201, OrderResponse.from(order));
        } catch (ValidationException e) {
            sendError(exchange, 400, e.getMessage());
        }
    }

    private void handleGetOrder(HttpExchange exchange, UUID orderId) throws IOException {
        Optional<Order> order = orderService.getOrder(orderId);
        if (order.isEmpty()) {
            sendError(exchange, 404, "Order not found");
            return;
        }
        sendJson(exchange, 200, OrderResponse.from(order.get()));
    }

    private void handleGetAllOrders(HttpExchange exchange) throws IOException {
        List<OrderResponse> orders = orderService.getAllOrders().stream()
                .map(OrderResponse::from)
                .toList();
        sendJson(exchange, 200, orders);
    }

    private void handleDeleteOrder(HttpExchange exchange, UUID orderId) throws IOException {
        if (orderService.isOrderNotFound(orderId)) {
            sendError(exchange, 404, "Order not found");
            return;
        }
        orderService.cancelOrder(orderId);
        exchange.sendResponseHeaders(204, -1);
    }

    private void handleCreatePancake(HttpExchange exchange, UUID orderId) throws IOException {
        if (orderService.isOrderNotFound(orderId)) {
            sendError(exchange, 404, "Order not found");
            return;
        }
        try {
            UUID pancakeId = pancakeService.createPancake(orderId);
            Pancake pancake = pancakeService.getPancake(orderId, pancakeId)
                    .orElseThrow(() -> new IllegalStateException("Failed to create pancake"));
            sendJson(exchange, 201, PancakeResponse.from(pancake));
        } catch (IllegalStateException e) {
            sendError(exchange, 400, e.getMessage());
        }
    }

    private void handleGetPancakes(HttpExchange exchange, UUID orderId) throws IOException {
        if (orderService.isOrderNotFound(orderId)) {
            sendError(exchange, 404, "Order not found");
            return;
        }
        List<PancakeResponse> pancakes = pancakeService.getPancakesByOrder(orderId).stream()
                .map(PancakeResponse::from)
                .toList();
        sendJson(exchange, 200, pancakes);
    }

    private void handleDeletePancake(HttpExchange exchange, UUID orderId, UUID pancakeId) throws IOException {
        if (orderService.isOrderNotFound(orderId)) {
            sendError(exchange, 404, "Order not found");
            return;
        }
        pancakeService.removePancake(orderId, pancakeId);
        exchange.sendResponseHeaders(204, -1);
    }

    private void handleAddIngredient(HttpExchange exchange, UUID orderId, UUID pancakeId) throws IOException {
        IngredientRequest request = JsonUtil.deserialize(exchange.getRequestBody(), IngredientRequest.class);

        if (orderService.isOrderNotFound(orderId)) {
            sendError(exchange, 404, "Order not found");
            return;
        }

        Optional<Pancake> pancake = pancakeService.getPancake(orderId, pancakeId);
        if (pancake.isEmpty()) {
            sendError(exchange, 404, "Pancake not found");
            return;
        }

        RequestValidator.validateIngredient(request);  // This might throw ValidationException

        pancakeService.addIngredientToPancake(orderId, pancakeId, request.name());

        // Get the updated pancake to return the latest ingredient
        Optional<Pancake> updatedPancake = pancakeService.getPancake(orderId, pancakeId);
        Ingredient addedIngredient = updatedPancake.get().ingredients()
                .stream()
                .filter(i -> i.getName().equals(request.name()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Failed to add ingredient"));

        sendJson(exchange, 201, IngredientResponse.from(addedIngredient));
    }

    private void handleGetIngredients(HttpExchange exchange, UUID orderId, UUID pancakeId) throws IOException {
        Optional<Pancake> pancake = pancakeService.getPancake(orderId, pancakeId);
        if (pancake.isEmpty()) {
            sendError(exchange, 404, "Pancake not found");
            return;
        }

        List<IngredientResponse> ingredients = pancake.get().ingredients().stream()
                .map(IngredientResponse::from)
                .toList();
        sendJson(exchange, 200, ingredients);
    }

    private void handleRemoveIngredient(HttpExchange exchange, UUID orderId, UUID pancakeId, UUID ingredientId) throws IOException {
        if (orderService.isOrderNotFound(orderId)) {
            sendError(exchange, 404, "Order not found");
            return;
        }
        pancakeService.removeIngredientFromPancake(orderId, pancakeId, ingredientId);
        exchange.sendResponseHeaders(204, -1);
    }


    private void sendJson(HttpExchange exchange, int statusCode, Object response) throws IOException {
        byte[] responseBody = JsonUtil.serialize(response);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, responseBody.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBody);
        }
    }

    private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        Map<String, String> error = Map.of("error", message);
        sendJson(exchange, statusCode, error);
    }
}
