package org.pancakelab.http.controller;

import com.sun.net.httpserver.HttpExchange;
import org.pancakelab.http.HttpUtils;
import org.pancakelab.http.JsonUtil;
import org.pancakelab.http.dto.IngredientRequest;
import org.pancakelab.http.dto.PancakeResponse;
import org.pancakelab.http.validation.RequestValidator;
import org.pancakelab.http.validation.ValidationException;
import org.pancakelab.model.Ingredient;
import org.pancakelab.model.Pancake;
import org.pancakelab.service.PancakeService;
import org.pancakelab.util.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PancakeController {
    private final PancakeService pancakeService;

    public PancakeController(PancakeService pancakeService) {
        this.pancakeService = pancakeService;
    }

    public void createPancake(HttpExchange exchange, Map<String, String> pathParams) throws IOException {
        try {
            UUID orderId = UUID.fromString(pathParams.get("orderId"));
            UUID pancakeId = pancakeService.createPancake(orderId);

            Logger.info("Created pancake %s for order %s", pancakeId, orderId);
            HttpUtils.sendJson(exchange, 201, Map.of("id", pancakeId.toString()));
        } catch (IllegalArgumentException e) {
            HttpUtils.sendError(exchange, 404, e.getMessage());
        } catch (IllegalStateException e) {
            HttpUtils.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            Logger.error("Failed to create pancake: %s", e.getMessage());
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }

    public void getPancakes(HttpExchange exchange, Map<String, String> pathParams) throws IOException {
        try {
            UUID orderId = UUID.fromString(pathParams.get("orderId"));
            List<Pancake> pancakes = pancakeService.getPancakesByOrder(orderId);

            List<PancakeResponse> responses = pancakes.stream()
                    .map(PancakeResponse::fromPancake)
                    .toList();

            HttpUtils.sendJson(exchange, 200, responses);
        } catch (Exception e) {
            Logger.error("Failed to get pancakes: %s", e.getMessage());
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }

    public void deletePancake(HttpExchange exchange, Map<String, String> pathParams) throws IOException {
        try {
            UUID orderId = UUID.fromString(pathParams.get("orderId"));
            UUID pancakeId = UUID.fromString(pathParams.get("pancakeId"));

            pancakeService.removePancake(orderId, pancakeId);

            Logger.info("Deleted pancake %s from order %s", pancakeId, orderId);
            HttpUtils.sendEmpty(exchange, 204);
        } catch (IllegalArgumentException e) {
            HttpUtils.sendError(exchange, 404, e.getMessage());
        } catch (IllegalStateException e) {
            HttpUtils.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            Logger.error("Failed to delete pancake: %s", e.getMessage());
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }

    public void addIngredient(HttpExchange exchange, Map<String, String> pathParams) throws IOException {
        try {
            UUID orderId = UUID.fromString(pathParams.get("orderId"));
            UUID pancakeId = UUID.fromString(pathParams.get("pancakeId"));

            IngredientRequest request = JsonUtil.fromJson(exchange, IngredientRequest.class);
            RequestValidator.validateIngredient(request);

            Ingredient ingredient = new Ingredient(request.name());
            pancakeService.addIngredientToPancake(orderId, pancakeId, ingredient);

            Logger.info("Added ingredient %s to pancake %s", ingredient.getName(), pancakeId);
            HttpUtils.sendJson(exchange, 201, Map.of("ingredientId", ingredient.getId().toString()));
        } catch (ValidationException e) {
            HttpUtils.sendError(exchange, e.getStatusCode(), e.getMessage());
        } catch (IllegalArgumentException e) {
            HttpUtils.sendError(exchange, 404, e.getMessage());
        } catch (IllegalStateException e) {
            HttpUtils.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            Logger.error("Failed to add ingredient: %s", e.getMessage());
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }

    public void removeIngredient(HttpExchange exchange, Map<String, String> pathParams) throws IOException {
        try {
            UUID orderId = UUID.fromString(pathParams.get("orderId"));
            UUID pancakeId = UUID.fromString(pathParams.get("pancakeId"));
            UUID ingredientId = UUID.fromString(pathParams.get("ingredientId"));

            pancakeService.removeIngredientFromPancake(orderId, pancakeId, ingredientId);

            Logger.info("Removed ingredient %s from pancake %s", ingredientId, pancakeId);
            HttpUtils.sendEmpty(exchange, 204);
        } catch (IllegalArgumentException e) {
            HttpUtils.sendError(exchange, 404, e.getMessage());
        } catch (IllegalStateException e) {
            HttpUtils.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            Logger.error("Failed to remove ingredient: %s", e.getMessage());
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }
}
