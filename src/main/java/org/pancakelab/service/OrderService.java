package org.pancakelab.service;

import org.pancakelab.model.Order;
import org.pancakelab.model.OrderState;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderService {
    // Create
    Order createOrder(int building, int room);

    // Read
    Optional<Order> getOrder(UUID orderId);

    List<Order> getAllOrders();

    List<Order> getOrdersByState(OrderState state);

    // Update state
    void completeOrder(UUID orderId);    // Changes state to COMPLETED

    void prepareOrder(UUID orderId);     // Changes state to PREPARED

    void startDelivery(UUID orderId);    // Changes state to OUT_FOR_DELIVERY

    void cancelOrder(UUID orderId);      // Changes state to CANCELLED

    // Delete (archived orders)
    void deleteOrder(UUID orderId);

    // Utility
    boolean isOrderNotFound(UUID orderId);
}
