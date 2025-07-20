package org.pancakelab.http.dto;

public class CreateOrderRequest {
    private int building;
    private int room;

    // Required for JSON deserialization
    public CreateOrderRequest() {}

    public int getBuilding() {
        return building;
    }

    public void setBuilding(int building) {
        this.building = building;
    }

    public int getRoom() {
        return room;
    }

    public void setRoom(int room) {
        this.room = room;
    }
}
