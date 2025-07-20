package org.pancakelab.exception;

/**
 * Exception thrown when an order is not found
 */
public class OrderNotFoundException extends PancakeLabException {
    public OrderNotFoundException(String orderId) {
        super("Order not found: " + orderId, 404);
    }
}
