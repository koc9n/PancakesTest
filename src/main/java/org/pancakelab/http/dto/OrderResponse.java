package org.pancakelab.http.dto;

public class OrderResponse {
    private String orderId;
    private int building;
    private int room;
    private java.util.List<String> pancakes;

    public OrderResponse(String orderId, int building, int room, java.util.List<String> pancakes) {
        this.orderId = orderId;
        this.building = building;
        this.room = room;
        this.pancakes = pancakes;
    }

    public String getOrderId() {
        return orderId;
    }

    public int getBuilding() {
        return building;
    }

    public int getRoom() {
        return room;
    }

    public java.util.List<String> getPancakes() {
        return pancakes;
    }
}
