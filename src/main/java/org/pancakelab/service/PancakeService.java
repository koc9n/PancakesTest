package org.pancakelab.service;

import org.pancakelab.model.Order;
import org.pancakelab.model.pancakes.PancakeRecipe;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class PancakeService {
    private static final PancakeService INSTANCE = new PancakeService();

    private final List<Order> orders = new ArrayList<>();
    private final Set<UUID> completedOrders = new HashSet<>();
    private final Set<UUID> preparedOrders = new HashSet<>();
    private final List<PancakeRecipe> pancakes = new ArrayList<>();

    private PancakeService() {
        // private constructor to prevent instantiation
    }

    public static PancakeService getInstance() {
        return INSTANCE;
    }

    public Order createOrder(int building, int room) {
        Order order = new Order(building, room);
        orders.add(order);
        return order;
    }

    public UUID addPancakeToOrder(UUID orderId) {
        Order order = orders.stream()
                .filter(o -> o.getId().equals(orderId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        PancakeRecipe pancake = new PancakeRecipe();
        pancake.setOrderId(orderId);
        pancakes.add(pancake);

        OrderLog.logAddPancake(order, pancake.description(), pancakes);
        return pancake.getId();
    }

    private void addPancake(PancakeRecipe pancake, Order order) {
        pancake.setOrderId(order.getId());
        pancakes.add(pancake);

        OrderLog.logAddPancake(order, pancake.description(), pancakes);
    }

    public void removePancakes(String description, UUID orderId, int count) {
        final AtomicInteger removedCount = new AtomicInteger(0);
        pancakes.removeIf(pancake -> {
            return pancake.getOrderId().equals(orderId) &&
                   pancake.description().equals(description) &&
                   removedCount.getAndIncrement() < count;
        });

        Order order = orders.stream().filter(o -> o.getId().equals(orderId)).findFirst().get();
        OrderLog.logRemovePancakes(order, description, removedCount.get(), pancakes);
    }

    public void cancelOrder(UUID orderId) {
        Order order = orders.stream().filter(o -> o.getId().equals(orderId)).findFirst().get();
        OrderLog.logCancelOrder(order, this.pancakes);

        pancakes.removeIf(pancake -> pancake.getOrderId().equals(orderId));
        orders.removeIf(o -> o.getId().equals(orderId));
        completedOrders.removeIf(u -> u.equals(orderId));
        preparedOrders.removeIf(u -> u.equals(orderId));

        OrderLog.logCancelOrder(order,pancakes);
    }

    public void completeOrder(UUID orderId) {
        completedOrders.add(orderId);
    }

    public Set<UUID> listCompletedOrders() {
        return completedOrders;
    }

    public void prepareOrder(UUID orderId) {
        preparedOrders.add(orderId);
        completedOrders.removeIf(u -> u.equals(orderId));
    }

    public Set<UUID> listPreparedOrders() {
        return preparedOrders;
    }

    public Object[] deliverOrder(UUID orderId) {
        if (!preparedOrders.contains(orderId)) return null;

        Order order = orders.stream().filter(o -> o.getId().equals(orderId)).findFirst().get();
        List<String> pancakesToDeliver = viewOrder(orderId);
        OrderLog.logDeliverOrder(order, this.pancakes);

        pancakes.removeIf(pancake -> pancake.getOrderId().equals(orderId));
        orders.removeIf(o -> o.getId().equals(orderId));
        preparedOrders.removeIf(u -> u.equals(orderId));

        return new Object[] {order, pancakesToDeliver};
    }

    public List<String> viewOrder(UUID orderId) {
        return pancakes.stream()
                       .filter(pancake -> pancake.getOrderId().equals(orderId))
                       .map(PancakeRecipe::description).toList();
    }

    public Optional<Order> getOrder(UUID orderId) {
        return orders.stream()
                .filter(o -> o.getId().equals(orderId))
                .findFirst();
    }

    public UUID createPancake(UUID orderId) {
        Order order = orders.stream()
                .filter(o -> o.getId().equals(orderId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        PancakeRecipe pancake = new PancakeRecipe();
        pancake.setOrderId(orderId);
        pancakes.add(pancake);

        OrderLog.logAddPancake(order, pancake.description(), pancakes);
        return pancake.getId();  // Return pancake.getId() instead of orderId
    }

    public void addIngredientToPancake(UUID orderId, UUID pancakeId, String ingredient) {
        Order order = orders.stream()
                .filter(o -> o.getId().equals(orderId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        PancakeRecipe pancake = pancakes.stream()
                .filter(p -> p.getId().equals(pancakeId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Pancake not found"));

        if (!pancake.getOrderId().equals(orderId)) {
            throw new IllegalArgumentException("Pancake does not belong to this order");
        }

        pancake.addIngredient(ingredient);
        OrderLog.logAddPancake(order, pancake.description(), pancakes);
    }
}
