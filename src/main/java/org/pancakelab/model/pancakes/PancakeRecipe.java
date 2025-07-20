package org.pancakelab.model.pancakes;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class PancakeRecipe implements Recipe {
    private final AtomicReference<UUID> id = new AtomicReference<>(UUID.randomUUID());
    private final AtomicReference<UUID> orderId = new AtomicReference<>();
    private final List<String> ingredients = new CopyOnWriteArrayList<>();

    @Override
    public UUID getId() {
        return id.get();
    }

    @Override
    public UUID getOrderId() {
        return orderId.get();
    }

    @Override
    public void setOrderId(UUID newOrderId) {
        if (!orderId.compareAndSet(null, newOrderId)) {
            throw new IllegalStateException("Order ID can only be set once");
        }
    }

    @Override
    public List<String> ingredients() {
        return ingredients;
    }

    @Override
    public void addIngredient(String ingredient) {
        if (orderId.get() == null) {
            throw new IllegalStateException("Cannot add ingredients before assigning to an order");
        }
        ingredients.add(ingredient);
    }
}
