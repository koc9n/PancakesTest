package org.pancakelab.http.controller;

import com.sun.net.httpserver.HttpExchange;
import org.pancakelab.http.HttpUtils;
import org.pancakelab.http.JsonUtil;
import org.pancakelab.http.dto.CreateOrderRequest;
import org.pancakelab.http.dto.OrderResponse;
import org.pancakelab.http.validation.RequestValidator;
import org.pancakelab.model.Order;
import org.pancakelab.model.OrderState;
import org.pancakelab.service.OrderService;
import org.pancakelab.util.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    public void createOrder(HttpExchange exchange, Map<String, String> pathParams) throws IOException {
        try {
            CreateOrderRequest request = JsonUtil.fromJson(exchange, CreateOrderRequest.class);
            RequestValidator.validateCreateOrder(request);

            Order order = orderService.createOrder(request.building(), request.room());
            OrderResponse response = OrderResponse.fromOrder(order);

            Logger.info("Created order: %s for building %d, room %d",
                    order.getId(), request.building(), request.room());
            HttpUtils.sendJson(exchange, 201, response);
        } catch (Exception e) {
            Logger.error("Failed to create order: %s", e.getMessage());
            HttpUtils.sendError(exchange, 400, e.getMessage());
        }
    }

    public void getAllOrders(HttpExchange exchange, Map<String, String> pathParams) throws IOException {
        try {
            String stateParam = HttpUtils.getQueryParam(exchange, "state");
            List<Order> orders;

            if (stateParam != null) {
                OrderState state = OrderState.valueOf(stateParam.toUpperCase());
                orders = orderService.getOrdersByState(state);
            } else {
                orders = orderService.getAllOrders();
            }

            List<OrderResponse> responses = orders.stream()
                    .map(OrderResponse::fromOrder)
                    .toList();

            HttpUtils.sendJson(exchange, 200, responses);
        } catch (IllegalArgumentException e) {
            HttpUtils.sendError(exchange, 400, "Invalid state parameter");
        } catch (Exception e) {
            Logger.error("Failed to get orders: %s", e.getMessage());
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }

    public void getOrder(HttpExchange exchange, Map<String, String> pathParams) throws IOException {
        try {
            UUID orderId = UUID.fromString(pathParams.get("orderId"));
            Order order = orderService.getOrder(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Order not found"));

            OrderResponse response = OrderResponse.fromOrder(order);
            HttpUtils.sendJson(exchange, 200, response);
        } catch (IllegalArgumentException e) {
            HttpUtils.sendError(exchange, 404, e.getMessage());
        } catch (Exception e) {
            Logger.error("Failed to get order: %s", e.getMessage());
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }

    public void deleteOrder(HttpExchange exchange, Map<String, String> pathParams) throws IOException {
        try {
            UUID orderId = UUID.fromString(pathParams.get("orderId"));
            orderService.deleteOrder(orderId);

            Logger.info("Deleted order: %s", orderId);
            HttpUtils.sendEmpty(exchange, 204);
        } catch (IllegalArgumentException e) {
            HttpUtils.sendError(exchange, 404, e.getMessage());
        } catch (Exception e) {
            Logger.error("Failed to delete order: %s", e.getMessage());
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }

    public void completeOrder(HttpExchange exchange, Map<String, String> pathParams) throws IOException {
        try {
            UUID orderId = UUID.fromString(pathParams.get("orderId"));
            orderService.completeOrder(orderId);

            Logger.info("Completed order: %s", orderId);
            HttpUtils.sendEmpty(exchange, 200);
        } catch (IllegalArgumentException e) {
            HttpUtils.sendError(exchange, 404, e.getMessage());
        } catch (IllegalStateException e) {
            HttpUtils.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            Logger.error("Failed to complete order: %s", e.getMessage());
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }

    public void prepareOrder(HttpExchange exchange, Map<String, String> pathParams) throws IOException {
        try {
            UUID orderId = UUID.fromString(pathParams.get("orderId"));
            orderService.prepareOrder(orderId);

            Logger.info("Prepared order: %s", orderId);
            HttpUtils.sendEmpty(exchange, 200);
        } catch (IllegalArgumentException e) {
            HttpUtils.sendError(exchange, 404, e.getMessage());
        } catch (IllegalStateException e) {
            HttpUtils.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            Logger.error("Failed to prepare order: %s", e.getMessage());
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }

    public void startDelivery(HttpExchange exchange, Map<String, String> pathParams) throws IOException {
        try {
            UUID orderId = UUID.fromString(pathParams.get("orderId"));
            orderService.startDelivery(orderId);

            Logger.info("Started delivery for order: %s", orderId);
            HttpUtils.sendEmpty(exchange, 200);
        } catch (IllegalArgumentException e) {
            HttpUtils.sendError(exchange, 404, e.getMessage());
        } catch (IllegalStateException e) {
            HttpUtils.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            Logger.error("Failed to start delivery: %s", e.getMessage());
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }

    public void cancelOrder(HttpExchange exchange, Map<String, String> pathParams) throws IOException {
        try {
            UUID orderId = UUID.fromString(pathParams.get("orderId"));
            orderService.cancelOrder(orderId);

            Logger.info("Cancelled order: %s", orderId);
            HttpUtils.sendEmpty(exchange, 200);
        } catch (IllegalArgumentException e) {
            HttpUtils.sendError(exchange, 404, e.getMessage());
        } catch (Exception e) {
            Logger.error("Failed to cancel order: %s", e.getMessage());
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }
}
