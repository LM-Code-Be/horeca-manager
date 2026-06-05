package com.lmcode.horecamanager.models;

public record RestaurantTable(
        int id,
        String number,
        String name,
        int capacity,
        String zone,
        String status,
        String note
) {
    public String displayName() {
        return "Table " + number + (name == null || name.isBlank() ? "" : " - " + name);
    }

    @Override
    public String toString() {
        return displayName();
    }
}
