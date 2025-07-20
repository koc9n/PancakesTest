package org.pancakelab.model.pancakes;

import java.util.List;
import java.util.UUID;

public interface Recipe {

    default String description()
    {
        return "Delicious pancake with %s!".formatted(String.join(", ", ingredients()));
    }

    UUID getId();

    UUID getOrderId();

    void setOrderId(UUID orderId);

    List<String> ingredients();

    void addIngredient(String ingredient);
}
