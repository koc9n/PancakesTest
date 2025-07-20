package org.pancakelab.http.dto;

import java.util.List;

public record OrderResponse(String orderId, int building, int room, List<String> pancakes) {
}
