package com.lmcode.horecamanager.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

public final class DateUtils {
    private static final Locale LOCALE = Locale.FRANCE;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(LOCALE);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private DateUtils() {
    }

    public static String todayLabel() {
        return LocalDate.now().format(DATE);
    }

    public static String formatDate(LocalDate date) {
        return date == null ? "" : date.format(DATE);
    }

    public static String formatTime(LocalTime time) {
        return time == null ? "" : time.format(TIME);
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "" : dateTime.format(DATE_TIME);
    }
}
