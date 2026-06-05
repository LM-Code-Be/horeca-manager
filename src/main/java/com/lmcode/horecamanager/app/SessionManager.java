package com.lmcode.horecamanager.app;

import com.lmcode.horecamanager.repositories.CashSessionRepository;
import com.lmcode.horecamanager.repositories.SettingsRepository;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Scene;

public final class SessionManager {
    private static final BooleanProperty darkTheme = new SimpleBooleanProperty(false);
    private static final BooleanProperty cashOpen = new SimpleBooleanProperty(false);
    private static final StringProperty pendingSearch = new SimpleStringProperty("");
    private static final SettingsRepository settingsRepository = new SettingsRepository();
    private static final CashSessionRepository cashSessionRepository = new CashSessionRepository();

    private SessionManager() {
    }

    public static void initialize() {
        darkTheme.set("dark".equals(settingsRepository.get("app.theme", "light")));
        refreshCashStatus();
    }

    public static BooleanProperty darkThemeProperty() {
        return darkTheme;
    }

    public static BooleanProperty cashOpenProperty() {
        return cashOpen;
    }

    public static void setPendingSearch(String query) {
        pendingSearch.set(query == null ? "" : query.trim());
    }

    public static String consumePendingSearch() {
        String value = pendingSearch.get();
        pendingSearch.set("");
        return value == null ? "" : value;
    }

    public static boolean isCashOpen() {
        return cashOpen.get();
    }

    public static void refreshCashStatus() {
        cashOpen.set(cashSessionRepository.isOpen());
    }

    public static void toggleTheme(Scene scene) {
        darkTheme.set(!darkTheme.get());
        settingsRepository.set("app.theme", darkTheme.get() ? "dark" : "light");
        applyTheme(scene);
    }

    public static void applyTheme(Scene scene) {
        if (scene == null) {
            return;
        }
        String light = resource("/styles/light-theme.css");
        String dark = resource("/styles/dark-theme.css");
        scene.getStylesheets().remove(light);
        scene.getStylesheets().remove(dark);
        scene.getStylesheets().add(darkTheme.get() ? dark : light);
    }

    public static String resource(String path) {
        return SessionManager.class.getResource(path).toExternalForm();
    }
}
