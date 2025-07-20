package org.pancakelab.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class Pancake {
    private final UUID id = UUID.randomUUID();
    private final List<Ingredient> ingredients = new ArrayList<>();

    public UUID getId() {
        return id;
    }

    public synchronized List<Ingredient> ingredients() {
        return new ArrayList<>(ingredients);
    }

    public synchronized void addIngredient(Ingredient ingredient) {
        ingredients.add(ingredient);
    }

    public synchronized void removeIngredient(UUID ingredientId) {
        ingredients.removeIf(ingredient -> ingredient.getId().equals(ingredientId));
    }

    public synchronized String description() {
        return "Delicious pancake with %s!".formatted(
                ingredients.stream()
                        .map(Ingredient::getName)
                        .collect(Collectors.joining(", "))
        );
    }
}
