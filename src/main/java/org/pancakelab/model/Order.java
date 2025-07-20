package org.pancakelab.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class Order {
    private final UUID id;
    private final int building;
    private final int room;
    private final List<Pancake> pancakes;
    private volatile OrderState state;

    public Order(int building, int room) {
        this.id = UUID.randomUUID();
        this.building = building;
        this.room = room;
        this.pancakes = new ArrayList<>();
        this.state = OrderState.OPEN;
    }

    public UUID getId() {
        return id;
    }

    public int getBuilding() {
        return building;
    }

    public int getRoom() {
        return room;
    }

    public synchronized OrderState getState() {
        return state;
    }

    public synchronized boolean compareAndSetState(OrderState expect, OrderState update) {
        if (state == expect) {
            state = update;
            return true;
        }
        return false;
    }

    public synchronized List<Pancake> getPancakes() {
        return new ArrayList<>(pancakes);
    }

    public synchronized void addPancake(Pancake pancake) {
        if (state != OrderState.OPEN) {
            throw new IllegalStateException("Can only add pancakes to OPEN orders");
        }
        pancakes.add(pancake);
    }

    public synchronized void removePancake(UUID pancakeId) {
        if (state != OrderState.OPEN) {
            throw new IllegalStateException("Can only remove pancakes from OPEN orders");
        }
        pancakes.removeIf(p -> p.getId().equals(pancakeId));
    }

    public synchronized Optional<Pancake> getPancake(UUID pancakeId) {
        return pancakes.stream()
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
        return id.hashCode();
    }
}
