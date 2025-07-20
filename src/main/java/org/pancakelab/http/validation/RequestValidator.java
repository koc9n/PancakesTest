package org.pancakelab.http.validation;

import java.util.UUID;

public class RequestValidator {
    public static void validateOrderCreation(int building, int room) {
        if (building <= 0) {
            throw new ValidationException("Building number must be positive", 400);
        }
        if (room <= 0) {
            throw new ValidationException("Room number must be positive", 400);
        }
    }

    public static void validateIngredient(String ingredient) {
        if (ingredient == null || ingredient.trim().isEmpty()) {
            throw new ValidationException("Ingredient cannot be empty", 400);
        }
        if (ingredient.length() > 50) {
            throw new ValidationException("Ingredient name too long (max 50 characters)", 400);
        }
    }

    public static UUID validateUUID(String id, String paramName) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid " + paramName + " format", 400);
        }
    }
}
