package org.pancakelab.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.pancakelab.http.controller.OrderController;
import org.pancakelab.http.controller.PancakeController;
import org.pancakelab.service.ServiceFactory;
import org.pancakelab.util.Logger;

import java.io.IOException;

public class ApiHandler implements HttpHandler {
    private final Router router;
    private final RateLimiter rateLimiter;

    public ApiHandler(ServiceFactory serviceFactory) {
        this.router = new Router();
        this.rateLimiter = new RateLimiter();

        // Initialize controllers
        OrderController orderController = new OrderController(serviceFactory.getOrderService());
        PancakeController pancakeController = new PancakeController(serviceFactory.getPancakeService());

        setupRoutes(orderController, pancakeController);
    }

    private void setupRoutes(OrderController orderController, PancakeController pancakeController) {
        // Order management routes
        router.addRoute("POST", "/api/orders", orderController::createOrder);
        router.addRoute("GET", "/api/orders", orderController::getAllOrders);
        router.addRoute("GET", "/api/orders/{orderId}", orderController::getOrder);
        router.addRoute("DELETE", "/api/orders/{orderId}", orderController::deleteOrder);

        // Order state management routes
        router.addRoute("POST", "/api/orders/{orderId}/complete", orderController::completeOrder);
        router.addRoute("POST", "/api/orders/{orderId}/prepare", orderController::prepareOrder);
        router.addRoute("POST", "/api/orders/{orderId}/deliver", orderController::startDelivery);
        router.addRoute("POST", "/api/orders/{orderId}/cancel", orderController::cancelOrder);

        // Pancake management routes
        router.addRoute("POST", "/api/orders/{orderId}/pancakes", pancakeController::createPancake);
        router.addRoute("GET", "/api/orders/{orderId}/pancakes", pancakeController::getPancakes);
        router.addRoute("DELETE", "/api/orders/{orderId}/pancakes/{pancakeId}", pancakeController::deletePancake);

        // Ingredient management routes
        router.addRoute("POST", "/api/orders/{orderId}/pancakes/{pancakeId}/ingredients", pancakeController::addIngredient);
        router.addRoute("DELETE", "/api/orders/{orderId}/pancakes/{pancakeId}/ingredients/{ingredientId}", pancakeController::removeIngredient);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String clientIp = exchange.getRemoteAddress().getAddress().getHostAddress();
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        Logger.info("Incoming request: %s %s from %s", method, path, clientIp);

        // Rate limiting
        if (!rateLimiter.allowRequest(clientIp)) {
            Logger.warn("Rate limit exceeded for client: %s", clientIp);
            HttpUtils.sendError(exchange, 429, "Too Many Requests");
            return;
        }

        try {
            // Try to handle the request with the router
            if (!router.handleRequest(exchange)) {
                // No route matched
                Logger.warn("No route found for: %s %s", method, path);
                HttpUtils.sendError(exchange, 404, "Not Found");
            }
        } catch (Exception e) {
            Logger.error("Unexpected error handling request: %s", e.getMessage());
            HttpUtils.sendError(exchange, 500, "Internal Server Error");
        }
    }
}
