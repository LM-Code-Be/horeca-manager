package com.lmcode.horecamanager.utils;

import java.text.NumberFormat;
import java.util.Locale;

public final class MoneyUtils {
    private static final NumberFormat FORMAT = NumberFormat.getCurrencyInstance(Locale.FRANCE);

    private MoneyUtils() {
    }

    public static String format(double amount) {
        return FORMAT.format(amount);
    }

    public static double round(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }

    public static double parseAmount(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        String normalized = value.trim().replace("€", "").replace(" ", "").replace(',', '.');
        return Double.parseDouble(normalized);
    }
}
