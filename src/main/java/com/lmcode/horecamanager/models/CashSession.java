package com.lmcode.horecamanager.models;

import java.time.LocalDateTime;

public record CashSession(
        int id,
        LocalDateTime openedAt,
        LocalDateTime closedAt,
        double initialAmount,
        double expectedAmount,
        Double realAmount,
        Double differenceAmount,
        String status,
        String note
) {
}
