package com.littlehorizons.erp.model;

public record Club(int id, String name, String supervisor, int capacity, int activeMembers, String schedule) {

    public boolean isFull() {
        return activeMembers >= capacity;
    }
}
