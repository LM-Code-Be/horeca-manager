package com.lmcode.horecamanager.models;

import java.time.LocalDateTime;

public record Ticket(
        int id,
        int orderId,
        String ticketNumber,
        String content,
        double total,
        LocalDateTime createdAt
) {
}
