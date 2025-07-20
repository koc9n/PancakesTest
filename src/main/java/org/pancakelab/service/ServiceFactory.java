package org.pancakelab.service;

import org.pancakelab.service.impl.OrderServiceImpl;
import org.pancakelab.service.impl.PancakeServiceImpl;

/**
 * Simple service factory to manage dependencies and service instantiation.
 * This replaces the singleton pattern with proper dependency injection.
 */
public class ServiceFactory {
    private final OrderService orderService;
    private final PancakeService pancakeService;

    public ServiceFactory() {
        // Create services with proper dependency injection
        this.orderService = new OrderServiceImpl();
        this.pancakeService = new PancakeServiceImpl(orderService);
    }

    public OrderService getOrderService() {
        return orderService;
    }

    public PancakeService getPancakeService() {
        return pancakeService;
    }
}
