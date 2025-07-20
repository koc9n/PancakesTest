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
import org.pancakelab.service.ServiceFactory;
import org.pancakelab.util.Logger;

import java.io.IOException;
import java.util.List;
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

    public OrderHandler(ServiceFactory serviceFactory) {
        this.orderService = serviceFactory.getOrderService();
        this.pancakeService = serviceFactory.getPancakeService();
        this.rateLimiter = new RateLimiter();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String clientIp = exchange.getRemoteAddress().getAddress().getHostAddress();
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        Logger.info("Incoming request: %s %s from %s", method, path, clientIp);

        if (!rateLimiter.allowRequest(clientIp)) {
            Logger.warn("Rate limit exceeded for client: %s", clientIp);
            HttpUtils.sendError(exchange, 429, "Too Many Requests");
            return;
        }

        try {
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
                    default -> HttpUtils.sendMethodNotAllowed(exchange);
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
                    HttpUtils.sendMethodNotAllowed(exchange);
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
                    HttpUtils.sendMethodNotAllowed(exchange);
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
                    default -> HttpUtils.sendMethodNotAllowed(exchange);
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
                    HttpUtils.sendMethodNotAllowed(exchange);
                }
                return;
            }

            HttpUtils.sendError(exchange, 404, "Not Found");

        } catch (ValidationException e) {
            Logger.warn("Validation error: %s", e.getMessage());
            HttpUtils.sendError(exchange, e.getStatusCode(), e.getMessage());
        } catch (IllegalArgumentException e) {
            Logger.warn("Invalid request: %s", e.getMessage());
            HttpUtils.sendBadRequest(exchange, "Invalid request: " + e.getMessage());
        } catch (IllegalStateException e) {
            Logger.warn("Invalid state: %s", e.getMessage());
            HttpUtils.sendBadRequest(exchange, e.getMessage());
        } catch (Exception e) {
            Logger.error("Unexpected error processing request %s %s", e, method, path);
            HttpUtils.sendInternalServerError(exchange, "Internal Server Error");
        } finally {
            exchange.close();
        }
    }

    private void handleCreateOrder(HttpExchange exchange) throws IOException {
        CreateOrderRequest request = JsonUtil.deserialize(exchange.getRequestBody(), CreateOrderRequest.class);
        try {
            RequestValidator.validateCreateOrder(request);
            Order order = orderService.createOrder(request.building(), request.room());
            HttpUtils.sendJson(exchange, 201, OrderResponse.from(order));
        } catch (ValidationException e) {
            HttpUtils.sendBadRequest(exchange, e.getMessage());
        }
    }

    private void handleGetOrder(HttpExchange exchange, UUID orderId) throws IOException {
        Optional<Order> order = orderService.getOrder(orderId);
        if (order.isEmpty()) {
            HttpUtils.sendNotFound(exchange, "Order");
            return;
        }
        HttpUtils.sendJson(exchange, 200, OrderResponse.from(order.get()));
    }

    private void handleGetAllOrders(HttpExchange exchange) throws IOException {
        List<OrderResponse> orders = orderService.getAllOrders().stream()
                .map(OrderResponse::from)
                .toList();
        HttpUtils.sendJson(exchange, 200, orders);
    }

    private void handleDeleteOrder(HttpExchange exchange, UUID orderId) throws IOException {
        if (orderService.isOrderNotFound(orderId)) {
            HttpUtils.sendNotFound(exchange, "Order");
            return;
        }
        orderService.cancelOrder(orderId);
        HttpUtils.sendNoContent(exchange);
    }

    private void handleCreatePancake(HttpExchange exchange, UUID orderId) throws IOException {
        if (orderService.isOrderNotFound(orderId)) {
            HttpUtils.sendNotFound(exchange, "Order");
            return;
        }
        try {
            UUID pancakeId = pancakeService.createPancake(orderId);
            Pancake pancake = pancakeService.getPancake(orderId, pancakeId)
                    .orElseThrow(() -> new IllegalStateException("Failed to create pancake"));
            HttpUtils.sendJson(exchange, 201, PancakeResponse.from(pancake));
        } catch (IllegalStateException e) {
            HttpUtils.sendBadRequest(exchange, e.getMessage());
        }
    }

    private void handleGetPancakes(HttpExchange exchange, UUID orderId) throws IOException {
        if (orderService.isOrderNotFound(orderId)) {
            HttpUtils.sendNotFound(exchange, "Order");
            return;
        }
        List<PancakeResponse> pancakes = pancakeService.getPancakesByOrder(orderId).stream()
                .map(PancakeResponse::from)
                .toList();
        HttpUtils.sendJson(exchange, 200, pancakes);
    }

    private void handleDeletePancake(HttpExchange exchange, UUID orderId, UUID pancakeId) throws IOException {
        if (orderService.isOrderNotFound(orderId)) {
            HttpUtils.sendNotFound(exchange, "Order");
            return;
        }
        pancakeService.removePancake(orderId, pancakeId);
        HttpUtils.sendNoContent(exchange);
    }

    private void handleAddIngredient(HttpExchange exchange, UUID orderId, UUID pancakeId) throws IOException {
        IngredientRequest request = JsonUtil.deserialize(exchange.getRequestBody(), IngredientRequest.class);

        if (orderService.isOrderNotFound(orderId)) {
            HttpUtils.sendNotFound(exchange, "Order");
            return;
        }

        Optional<Pancake> pancake = pancakeService.getPancake(orderId, pancakeId);
        if (pancake.isEmpty()) {
            HttpUtils.sendNotFound(exchange, "Pancake");
            return;
        }

        RequestValidator.validateIngredient(request);

        Ingredient ingredient = new Ingredient(request.name());
        Ingredient addedIngredient = pancakeService.addIngredientToPancake(orderId, pancakeId, ingredient);

        HttpUtils.sendJson(exchange, 201, IngredientResponse.from(addedIngredient));
    }

    private void handleGetIngredients(HttpExchange exchange, UUID orderId, UUID pancakeId) throws IOException {
        Optional<Pancake> pancake = pancakeService.getPancake(orderId, pancakeId);
        if (pancake.isEmpty()) {
            HttpUtils.sendNotFound(exchange, "Pancake");
            return;
        }

        List<IngredientResponse> ingredients = pancake.get().ingredients().stream()
                .map(IngredientResponse::from)
                .toList();
        HttpUtils.sendJson(exchange, 200, ingredients);
    }

    private void handleRemoveIngredient(HttpExchange exchange, UUID orderId, UUID pancakeId, UUID ingredientId) throws IOException {
        if (orderService.isOrderNotFound(orderId)) {
            HttpUtils.sendNotFound(exchange, "Order");
            return;
        }
        pancakeService.removeIngredientFromPancake(orderId, pancakeId, ingredientId);
        HttpUtils.sendNoContent(exchange);
    }
}
