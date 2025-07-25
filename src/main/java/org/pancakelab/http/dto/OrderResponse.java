package org.pancakelab.http.dto;

import org.pancakelab.model.Order;
import org.pancakelab.model.OrderState;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public record OrderResponse(UUID orderId, int building, int room, OrderState state, List<PancakeResponse> pancakes) {
    public static OrderResponse fromOrder(Order order) {
        List<PancakeResponse> pancakeResponses = order.getPancakes().stream()
                .map(PancakeResponse::fromPancake)
                .collect(Collectors.toList());
        return new OrderResponse(
                order.getId(),
                order.getBuilding(),
                order.getRoom(),
                order.getState(),
                pancakeResponses
        );
    }

    // Keep existing method for backward compatibility
    public static OrderResponse from(Order order) {
        return fromOrder(order);
    }
}
