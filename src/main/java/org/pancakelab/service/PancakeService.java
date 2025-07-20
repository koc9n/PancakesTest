package org.pancakelab.service;

import org.pancakelab.model.Ingredient;
import org.pancakelab.model.Pancake;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PancakeService {
    // Pancake operations
    UUID createPancake(UUID orderId);

    Optional<Pancake> getPancake(UUID orderId, UUID pancakeId);

    List<Pancake> getPancakesByOrder(UUID orderId);

    void removePancake(UUID orderId, UUID pancakeId);

    // Ingredient operations
    Ingredient addIngredientToPancake(UUID orderId, UUID pancakeId, Ingredient ingredient);

    void removeIngredientFromPancake(UUID orderId, UUID pancakeId, UUID ingredientId);
}
