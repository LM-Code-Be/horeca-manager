package com.lmcode.horecamanager.repositories;

import com.lmcode.horecamanager.database.DatabaseManager;
import com.lmcode.horecamanager.models.AppSettings;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class SettingsRepository {
    public AppSettings findAll() {
        Map<String, String> values = new LinkedHashMap<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT key, value FROM settings ORDER BY key");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                values.put(resultSet.getString("key"), resultSet.getString("value"));
            }
            return new AppSettings(values);
        } catch (SQLException exception) {
            throw new IllegalStateException("Erreur de lecture des parametres.", exception);
        }
    }

    public String get(String key, String fallback) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT value FROM settings WHERE key = ?")) {
            statement.setString(1, key);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString("value") : fallback;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Erreur de lecture du parametre.", exception);
        }
    }

    public void set(String key, String value) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO settings(key, value) VALUES (?, ?)
                     ON CONFLICT(key) DO UPDATE SET value = excluded.value
                     """)) {
            statement.setString(1, key);
            statement.setString(2, value == null ? "" : value);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible d'enregistrer le parametre.", exception);
        }
    }

    public void saveAll(Map<String, String> values) {
        values.forEach(this::set);
    }
}
