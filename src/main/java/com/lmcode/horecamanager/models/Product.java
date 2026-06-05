package com.lmcode.horecamanager.models;

import java.time.LocalDateTime;

public record Product(
        int id,
        String name,
        String description,
        Integer categoryId,
        double price,
        double vatRate,
        boolean available,
        Integer stockQuantity,
        Integer preparationTime,
        LocalDateTime createdAt
) {
}
