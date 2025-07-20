package org.pancakelab.service;

import org.pancakelab.model.Order;
import org.pancakelab.model.pancakes.PancakeRecipe;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class PancakeService {
    private static final PancakeService INSTANCE = new PancakeService();

    private final ConcurrentMap<UUID, Order> orders = new ConcurrentHashMap<>();
    // Use ConcurrentHashMap with LinkedHashMap to maintain insertion order
    private final ConcurrentMap<UUID, Map<UUID, PancakeRecipe>> orderPancakes = new ConcurrentHashMap<>();
    private final Set<UUID> completedOrders = ConcurrentHashMap.newKeySet();
    private final Set<UUID> preparedOrders = ConcurrentHashMap.newKeySet();

    private final ReadWriteLock orderLock = new ReentrantReadWriteLock();

    private PancakeService() {
    }

    public static PancakeService getInstance() {
        return INSTANCE;
    }

    public Order createOrder(int building, int room) {
        Order order = new Order(building, room);
        orders.put(order.getId(), order);
        // Use LinkedHashMap to maintain insertion order
        orderPancakes.put(order.getId(), Collections.synchronizedMap(new LinkedHashMap<>()));
        return order;
    }

    public UUID addPancakeToOrder(UUID orderId) {
        Order order = orders.get(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found");
        }

        Map<UUID, PancakeRecipe> pancakesMap = orderPancakes.get(orderId);
        if (pancakesMap == null) {
            throw new IllegalStateException("Order pancakes not initialized");
        }

        PancakeRecipe pancake = new PancakeRecipe();
        pancake.setOrderId(orderId);

        synchronized (pancakesMap) {
            pancakesMap.put(pancake.getId(), pancake);
            OrderLog.logAddPancake(order, pancake.description(), new ArrayList<>(pancakesMap.values()));
        }

        return pancake.getId();
    }

    public void addIngredientToPancake(UUID orderId, UUID pancakeId, String ingredient) {
        Map<UUID, PancakeRecipe> pancakesMap = orderPancakes.get(orderId);
        if (pancakesMap == null) {
            throw new IllegalArgumentException("Order not found");
        }

        synchronized (pancakesMap) {
            PancakeRecipe pancake = pancakesMap.get(pancakeId);
            if (pancake == null) {
                throw new IllegalArgumentException("Pancake not found");
            }

            pancake.addIngredient(ingredient);
            Order order = orders.get(orderId);
            OrderLog.logAddPancake(order, pancake.description(), new ArrayList<>(pancakesMap.values()));
        }
    }

    public void removePancakes(String description, UUID orderId, int count) {
        Map<UUID, PancakeRecipe> pancakesMap = orderPancakes.get(orderId);
        if (pancakesMap == null) {
            throw new IllegalArgumentException("Order not found");
        }

        Order order = orders.get(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found");
        }

        synchronized (pancakesMap) {
            int removed = 0;
            Iterator<PancakeRecipe> iterator = pancakesMap.values().iterator();
            while (iterator.hasNext() && removed < count) {
                PancakeRecipe pancake = iterator.next();
                if (pancake.description().equals(description)) {
                    iterator.remove();
                    removed++;
                }
            }
            OrderLog.logRemovePancakes(order, description, removed, new ArrayList<>(pancakesMap.values()));
        }
    }

    public void cancelOrder(UUID orderId) {
        orderLock.writeLock().lock();
        try {
            Order order = orders.remove(orderId);
            if (order == null) {
                throw new IllegalArgumentException("Order not found");
            }

            Map<UUID, PancakeRecipe> pancakesMap = orderPancakes.remove(orderId);
            OrderLog.logCancelOrder(order, pancakesMap != null ? new ArrayList<>(pancakesMap.values()) : new ArrayList<>());

            completedOrders.remove(orderId);
            preparedOrders.remove(orderId);
        } finally {
            orderLock.writeLock().unlock();
        }
    }

    public void completeOrder(UUID orderId) {
        checkOrderExists(orderId);
        completedOrders.add(orderId);
    }

    public void prepareOrder(UUID orderId) {
        checkOrderExists(orderId);
        orderLock.writeLock().lock();
        try {
            preparedOrders.add(orderId);
            completedOrders.remove(orderId);
        } finally {
            orderLock.writeLock().unlock();
        }
    }

    private void checkOrderExists(UUID orderId) {
        if (!orders.containsKey(orderId)) {
            throw new IllegalArgumentException("Order not found");
        }
    }

    public Set<UUID> listCompletedOrders() {
        return Collections.unmodifiableSet(completedOrders);
    }

    public Set<UUID> listPreparedOrders() {
        return Collections.unmodifiableSet(preparedOrders);
    }

    public Optional<Order> getOrder(UUID orderId) {
        return Optional.ofNullable(orders.get(orderId));
    }

    public Object[] deliverOrder(UUID orderId) {
        if (!preparedOrders.contains(orderId)) {
            return null;
        }

        orderLock.writeLock().lock();
        try {
            Order order = orders.get(orderId);
            if (order == null) {
                throw new IllegalArgumentException("Order not found");
            }

            Map<UUID, PancakeRecipe> pancakesMap = orderPancakes.get(orderId);
            List<String> pancakesToDeliver = pancakesMap != null ?
                    pancakesMap.values().stream().map(PancakeRecipe::description).toList() :
                    new ArrayList<>();

            OrderLog.logDeliverOrder(order, pancakesMap != null ?
                    new ArrayList<>(pancakesMap.values()) : new ArrayList<>());

            orders.remove(orderId);
            orderPancakes.remove(orderId);
            preparedOrders.remove(orderId);

            return new Object[]{order, pancakesToDeliver};
        } finally {
            orderLock.writeLock().unlock();
        }
    }

    public List<String> viewOrder(UUID orderId) {
        Map<UUID, PancakeRecipe> pancakesMap = orderPancakes.get(orderId);
        if (pancakesMap == null) {
            return new ArrayList<>();
        }
        return pancakesMap.values().stream()
                .map(PancakeRecipe::description)
                .toList();
    }
}
