package org.pancakelab.service.impl;

import org.pancakelab.model.Ingredient;
import org.pancakelab.model.Order;
import org.pancakelab.model.OrderState;
import org.pancakelab.model.Pancake;
import org.pancakelab.service.OrderService;
import org.pancakelab.service.PancakeService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PancakeServiceImpl implements PancakeService {
    private static final PancakeServiceImpl INSTANCE = new PancakeServiceImpl();
    private final OrderService orderService;

    private PancakeServiceImpl() {
        this.orderService = OrderService.getInstance();
    }

    public static PancakeServiceImpl getInstance() {
        return INSTANCE;
    }

    @Override
    public UUID createPancake(UUID orderId) {
        Order order = orderService.getOrder(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (order.getState() != OrderState.OPEN) {
            throw new IllegalStateException("Can only add pancakes to orders in OPEN state");
        }

        Pancake pancake = new Pancake();
        order.addPancake(pancake);

        // Log pancake creation
        OrderLogServiceImpl.logAddPancake(order);

        return pancake.getId();
    }

    @Override
    public Optional<Pancake> getPancake(UUID orderId, UUID pancakeId) {
        return orderService.getOrder(orderId)
                .flatMap(order -> order.getPancake(pancakeId));
    }

    @Override
    public List<Pancake> getPancakesByOrder(UUID orderId) {
        return orderService.getOrder(orderId)
                .map(Order::getPancakes)
                .orElse(Collections.emptyList());
    }

    @Override
    public Ingredient addIngredientToPancake(UUID orderId, UUID pancakeId, Ingredient ingredient) {
        Order order = orderService.getOrder(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (order.getState() != OrderState.OPEN) {
            throw new IllegalStateException("Can only modify pancakes in orders that are in OPEN state");
        }

        Pancake pancake = order.getPancake(pancakeId)
                .orElseThrow(() -> new IllegalArgumentException("Pancake not found"));

        pancake.addIngredient(ingredient);

        // Log ingredient addition
        OrderLogServiceImpl.logAddIngredient(order, pancake, ingredient);

        return ingredient;
    }

    @Override
    public void removeIngredientFromPancake(UUID orderId, UUID pancakeId, UUID ingredientId) {
        Order order = orderService.getOrder(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (order.getState() != OrderState.OPEN) {
            throw new IllegalStateException("Can only modify pancakes in orders that are in OPEN state");
        }

        Pancake pancake = order.getPancake(pancakeId)
                .orElseThrow(() -> new IllegalArgumentException("Pancake not found"));

        // Log before removal to have the ingredient info
        OrderLogServiceImpl.logRemoveIngredient(order, pancake, ingredientId);

        pancake.removeIngredient(ingredientId);
    }

    @Override
    public void removePancake(UUID orderId, UUID pancakeId) {
        Order order = orderService.getOrder(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (order.getState() != OrderState.OPEN) {
            throw new IllegalStateException("Can only remove pancakes from orders that are in OPEN state");
        }

        // Log before removal to have the correct pancake count
        OrderLogServiceImpl.logRemovePancake(order, pancakeId);

        order.removePancake(pancakeId);
    }
}
