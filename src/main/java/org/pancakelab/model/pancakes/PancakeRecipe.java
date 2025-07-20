package org.pancakelab.model.pancakes;

import java.util.*;

public class PancakeRecipe implements Recipe {
    private UUID id = UUID.randomUUID();
    private UUID orderId;
    protected List<String> ingredients = new ArrayList<>();

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public List<String> ingredients() {
        return ingredients;
    }

    public void addIngredient(String ingredient) {
        ingredients.add(ingredient);
    }
}
