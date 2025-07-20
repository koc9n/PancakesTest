package org.pancakelab.http.dto;

import org.pancakelab.model.Ingredient;

import java.util.UUID;

public record IngredientResponse(UUID id, String name) {
    public static IngredientResponse from(Ingredient ingredient) {
        return new IngredientResponse(ingredient.getId(), ingredient.getName());
    }
}
