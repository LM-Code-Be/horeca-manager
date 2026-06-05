package com.lmcode.horecamanager.utils;

import java.time.LocalDate;
import java.util.regex.Pattern;

public final class ValidationUtils {
    private static final Pattern EMAIL = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE = Pattern.compile("^[+0-9 ()./-]{6,24}$");

    private ValidationUtils() {
    }

    public static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void requirePositive(double value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void requireNonNegative(double value, String message) {
        if (value < 0) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void validateEmail(String email) {
        if (email != null && !email.isBlank() && !EMAIL.matcher(email).matches()) {
            throw new IllegalArgumentException("Email invalide.");
        }
    }

    public static void validatePhone(String phone) {
        if (phone != null && !phone.isBlank() && !PHONE.matcher(phone).matches()) {
            throw new IllegalArgumentException("Telephone invalide.");
        }
    }

    public static void validateReservationDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date de reservation requise.");
        }
        if (date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Date de reservation invalide.");
        }
    }
}
