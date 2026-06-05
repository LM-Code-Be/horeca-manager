package com.lmcode.horecamanager.models;

import java.util.Map;

public record AppSettings(Map<String, String> values) {
    public String get(String key, String fallback) {
        return values.getOrDefault(key, fallback);
    }

    public double getDouble(String key, double fallback) {
        try {
            return Double.parseDouble(values.getOrDefault(key, Double.toString(fallback)));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    public boolean getBoolean(String key, boolean fallback) {
        return Boolean.parseBoolean(values.getOrDefault(key, Boolean.toString(fallback)));
    }
}
