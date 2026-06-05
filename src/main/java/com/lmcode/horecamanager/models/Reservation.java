package com.lmcode.horecamanager.models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record Reservation(
        int id,
        String customerName,
        String phone,
        String email,
        LocalDate reservationDate,
        LocalTime reservationTime,
        int guestsCount,
        Integer tableId,
        String status,
        String note,
        LocalDateTime createdAt
) {
}
