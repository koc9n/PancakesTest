package org.pancakelab.http.dto;

import org.pancakelab.model.Pancake;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public record PancakeResponse(UUID id, List<IngredientResponse> ingredients) {
    public static PancakeResponse from(Pancake pancake) {
        List<IngredientResponse> ingredientResponses = pancake.ingredients().stream()
                .map(IngredientResponse::from)
                .collect(Collectors.toList());
        return new PancakeResponse(pancake.getId(), ingredientResponses);
    }
}
