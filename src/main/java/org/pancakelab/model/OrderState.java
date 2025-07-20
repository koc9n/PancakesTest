package org.pancakelab.model;

public enum OrderState {
    OPEN,           // Initial state when order is created
    COMPLETED,      // Order is submitted and ready for preparation
    PREPARED,       // Order is prepared and ready for delivery
    OUT_FOR_DELIVERY, // Order is out for delivery
    CANCELLED       // Order is cancelled
}
