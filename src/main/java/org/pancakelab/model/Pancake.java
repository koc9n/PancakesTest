package org.pancakelab.model;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class Pancake {
    private final AtomicReference<UUID> id = new AtomicReference<>(UUID.randomUUID());
    private final AtomicReference<List<Ingredient>> ingredients = new AtomicReference<>(new ArrayList<>());

    public UUID getId() {
        return id.get();
    }

    public List<Ingredient> ingredients() {
        return Collections.unmodifiableList(ingredients.get());
    }

    public void addIngredient(Ingredient ingredient) {
        int maxRetries = 3;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            List<Ingredient> currentIngredients = ingredients.get();
            List<Ingredient> newIngredients = new ArrayList<>(currentIngredients);
            newIngredients.add(ingredient);

            if (ingredients.compareAndSet(currentIngredients, Collections.unmodifiableList(newIngredients))) {
                return;
            }

            retryCount++;
            if (retryCount == maxRetries) {
                throw new ConcurrentModificationException("Failed to add ingredient after " + maxRetries + " retries");
            }

            try {
                Thread.sleep(10 * retryCount); // Exponential backoff
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Ingredient addition interrupted", e);
            }
        }
    }

    public void removeIngredient(UUID ingredientId) {
        int maxRetries = 3;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            List<Ingredient> currentIngredients = ingredients.get();
            List<Ingredient> newIngredients = new ArrayList<>(currentIngredients);
            boolean removed = newIngredients.removeIf(ingredient -> ingredient.getId().equals(ingredientId));

            if (!removed) {
                return; // Ingredient not found, nothing to remove
            }

            if (ingredients.compareAndSet(currentIngredients, Collections.unmodifiableList(newIngredients))) {
                return;
            }

            retryCount++;
            if (retryCount == maxRetries) {
                throw new ConcurrentModificationException("Failed to remove ingredient after " + maxRetries + " retries");
            }

            try {
                Thread.sleep(10 * retryCount);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Ingredient removal interrupted", e);
            }
        }
    }

    public String description() {
        return "Delicious pancake with %s!".formatted(
                ingredients.get().stream()
                        .map(Ingredient::getName)
                        .collect(Collectors.joining(", "))
        );
    }
}
