package org.pancakelab.http.validation;

import org.pancakelab.http.dto.CreateOrderRequest;
import org.pancakelab.http.dto.IngredientRequest;

import java.util.UUID;

public class RequestValidator {
    public static void validateCreateOrder(CreateOrderRequest request) {
        if (request == null) {
            throw new ValidationException("Request body cannot be null", 400);
        }
        validateOrderCreation(request.building(), request.room());
    }

    private static void validateOrderCreation(int building, int room) {
        if (building <= 0) {
            throw new ValidationException("Building number must be positive", 400);
        }
        if (room <= 0) {
            throw new ValidationException("Room number must be positive", 400);
        }
    }

    public static void validateIngredient(IngredientRequest request) {
        if (request == null) {
            throw new ValidationException("Request body cannot be null", 400);
        }
        if (request.name() == null || request.name().trim().isEmpty()) {
            throw new ValidationException("Ingredient name cannot be empty", 400);
        }
        if (request.name().length() > 50) {
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
