package org.pancakelab.service.impl;

import org.pancakelab.model.Order;
import org.pancakelab.model.OrderState;
import org.pancakelab.service.OrderService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class OrderServiceImpl implements OrderService {
    private final Map<UUID, Order> orders = new ConcurrentHashMap<>();

    public OrderServiceImpl() {
        // Remove singleton pattern - allow normal instantiation
    }

    @Override
    public Order createOrder(int building, int room) {
        Order order = new Order(building, room);
        orders.put(order.getId(), order);
        return order;
    }

    @Override
    public Optional<Order> getOrder(UUID orderId) {
        return Optional.ofNullable(orders.get(orderId));
    }

    @Override
    public List<Order> getAllOrders() {
        return new ArrayList<>(orders.values());
    }

    @Override
    public List<Order> getOrdersByState(OrderState state) {
        return orders.values().stream()
                .filter(order -> order.getState() == state)
                .collect(Collectors.toList());
    }

    @Override
    public void completeOrder(UUID orderId) {
        updateOrderState(orderId, OrderState.COMPLETED);
    }

    @Override
    public void prepareOrder(UUID orderId) {
        updateOrderState(orderId, OrderState.PREPARED);
    }

    @Override
    public void startDelivery(UUID orderId) {
        updateOrderState(orderId, OrderState.OUT_FOR_DELIVERY);
        // Remove from active orders since it's now out for delivery
        orders.remove(orderId);
    }

    @Override
    public void cancelOrder(UUID orderId) {
        updateOrderState(orderId, OrderState.CANCELLED);
        // Remove from active orders since it's cancelled
        orders.remove(orderId);
    }

    @Override
    public void deleteOrder(UUID orderId) {
        if (orders.remove(orderId) == null) {
            throw new IllegalArgumentException("Order not found");
        }
    }

    @Override
    public boolean isOrderNotFound(UUID orderId) {
        return !orders.containsKey(orderId);
    }

    private synchronized void updateOrderState(UUID orderId, OrderState newState) {
        Order order = orders.get(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found");
        }

        OrderState currentState = order.getState();
        validateStateTransition(currentState, newState);

        // Simple state update without retry logic
        if (!order.compareAndSetState(currentState, newState)) {
            // If CAS fails, try once more with current state
            currentState = order.getState();
            validateStateTransition(currentState, newState);
            if (!order.compareAndSetState(currentState, newState)) {
                throw new IllegalStateException("Failed to update order state due to concurrent modification");
            }
        }

        // Log the successful state change
        OrderLogServiceImpl.logOrderStateChange(order, currentState, newState);
    }

    private void validateStateTransition(OrderState currentState, OrderState newState) {
        // Define valid state transitions
        if (currentState == newState) {
            return;
        }

        boolean isValid = switch (currentState) {
            case OPEN -> newState == OrderState.COMPLETED || newState == OrderState.CANCELLED;
            case COMPLETED -> newState == OrderState.PREPARED || newState == OrderState.CANCELLED;
            case PREPARED -> newState == OrderState.OUT_FOR_DELIVERY || newState == OrderState.CANCELLED;
            case OUT_FOR_DELIVERY, CANCELLED -> false; // Terminal states
        };

        if (!isValid) {
            throw new IllegalStateException(
                    "Invalid state transition from " + currentState + " to " + newState);
        }
    }
}
