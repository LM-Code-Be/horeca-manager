package com.lmcode.horecamanager.models;

public record Category(
        int id,
        String name,
        String description,
        String color,
        boolean active
) {
    @Override
    public String toString() {
        return name;
    }
}
