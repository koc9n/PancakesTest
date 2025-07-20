package org.pancakelab.model;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class Order {
    private final AtomicReference<UUID> id;
    private final int building;
    private final int room;
    private final AtomicReference<List<Pancake>> pancakes;
    private final AtomicReference<OrderState> state;

    public Order(int building, int room) {
        this.id = new AtomicReference<>(UUID.randomUUID());
        this.building = building;
        this.room = room;
        this.pancakes = new AtomicReference<>(new ArrayList<>());
        this.state = new AtomicReference<>(OrderState.OPEN);
    }

    public UUID getId() {
        return id.get();
    }

    public int getBuilding() {
        return building;
    }

    public int getRoom() {
        return room;
    }

    public OrderState getState() {
        return state.get();
    }

    public boolean compareAndSetState(OrderState expect, OrderState update) {
        return state.compareAndSet(expect, update);
    }

    public List<Pancake> getPancakes() {
        return Collections.unmodifiableList(pancakes.get());
    }

    public void addPancake(Pancake pancake) {
        if (state.get() != OrderState.OPEN) {
            throw new IllegalStateException("Can only add pancakes to OPEN orders");
        }

        int maxRetries = 3;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            List<Pancake> currentPancakes = pancakes.get();
            List<Pancake> newPancakes = new ArrayList<>(currentPancakes);
            newPancakes.add(pancake);

            if (pancakes.compareAndSet(currentPancakes, Collections.unmodifiableList(newPancakes))) {
                return;
            }

            retryCount++;
            if (retryCount == maxRetries) {
                throw new ConcurrentModificationException("Failed to add pancake after " + maxRetries + " retries");
            }

            try {
                Thread.sleep(10L * retryCount); // Exponential backoff
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Pancake addition interrupted", e);
            }
        }
    }

    public void removePancake(UUID pancakeId) {
        if (state.get() != OrderState.OPEN) {
            throw new IllegalStateException("Can only remove pancakes from OPEN orders");
        }

        int maxRetries = 3;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            List<Pancake> currentPancakes = pancakes.get();
            List<Pancake> newPancakes = new ArrayList<>(currentPancakes);
            boolean removed = newPancakes.removeIf(p -> p.getId().equals(pancakeId));

            if (!removed) {
                return; // Pancake not found, nothing to remove
            }

            if (pancakes.compareAndSet(currentPancakes, Collections.unmodifiableList(newPancakes))) {
                return;
            }

            retryCount++;
            if (retryCount == maxRetries) {
                throw new ConcurrentModificationException("Failed to remove pancake after " + maxRetries + " retries");
            }

            try {
                Thread.sleep(10L * retryCount);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Pancake removal interrupted", e);
            }
        }
    }

    public Optional<Pancake> getPancake(UUID pancakeId) {
        return pancakes.get().stream()
                .filter(p -> p.getId().equals(pancakeId))
                .findFirst();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return getId().equals(order.getId());
    }

    @Override
    public int hashCode() {
        return id.get().hashCode();
    }
}
