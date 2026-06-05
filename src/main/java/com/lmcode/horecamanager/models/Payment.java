package com.lmcode.horecamanager.models;

import java.time.LocalDateTime;

public record Payment(
        int id,
        int orderId,
        String method,
        double amount,
        Double amountReceived,
        Double changeDue,
        LocalDateTime paidAt
) {
}
