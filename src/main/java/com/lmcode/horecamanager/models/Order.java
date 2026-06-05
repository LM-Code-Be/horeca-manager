package com.lmcode.horecamanager.models;

import java.time.LocalDateTime;

public record Order(
        int id,
        Integer tableId,
        String orderType,
        String status,
        double subtotal,
        double discount,
        double vatAmount,
        double total,
        LocalDateTime createdAt,
        LocalDateTime closedAt
) {
}
