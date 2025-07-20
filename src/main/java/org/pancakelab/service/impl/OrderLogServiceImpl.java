package org.pancakelab.service.impl;

import org.pancakelab.model.Ingredient;
import org.pancakelab.model.Order;
import org.pancakelab.model.OrderState;
import org.pancakelab.model.Pancake;

import java.util.UUID;

public class OrderLogServiceImpl {
    private static final StringBuilder log = new StringBuilder();

    public static void logAddPancake(Order order) {
        log.append("[%s] Added new pancake to order %s (Building %d, Room %d). Current pancakes count: %d\n"
                .formatted(
                        getCurrentTime(),
                        order.getId(),
                        order.getBuilding(),
                        order.getRoom(),
                        order.getPancakes().size()
                ));
    }

    public static void logAddIngredient(Order order, Pancake pancake, Ingredient ingredient) {
        log.append("[%s] Added ingredient '%s' to pancake %s in order %s\n"
                .formatted(
                        getCurrentTime(),
                        ingredient.getName(),
                        pancake.getId(),
                        order.getId()
                ));
    }

    public static void logRemoveIngredient(Order order, Pancake pancake, UUID ingredientId) {
        log.append("[%s] Removed ingredient %s from pancake %s in order %s\n"
                .formatted(
                        getCurrentTime(),
                        ingredientId,
                        pancake.getId(),
                        order.getId()
                ));
    }

    public static void logRemovePancake(Order order, UUID pancakeId) {
        log.append("[%s] Removed pancake %s from order %s. Current pancakes count: %d\n"
                .formatted(
                        getCurrentTime(),
                        pancakeId,
                        order.getId(),
                        order.getPancakes().size()
                ));
    }

    public static void logOrderStateChange(Order order, OrderState oldState, OrderState newState) {
        log.append("[%s] Order %s state changed from %s to %s (Building %d, Room %d)\n"
                .formatted(
                        getCurrentTime(),
                        order.getId(),
                        oldState,
                        newState,
                        order.getBuilding(),
                        order.getRoom()
                ));
    }

    public static String getFullLog() {
        return log.toString();
    }

    private static String getCurrentTime() {
        return java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
