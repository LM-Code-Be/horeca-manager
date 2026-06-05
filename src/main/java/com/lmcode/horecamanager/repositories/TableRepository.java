package com.lmcode.horecamanager.repositories;

import com.lmcode.horecamanager.database.DatabaseManager;
import com.lmcode.horecamanager.models.RestaurantTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class TableRepository {
    public List<RestaurantTable> findAll() {
        List<RestaurantTable> tables = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM restaurant_tables ORDER BY CAST(number AS INTEGER), number");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                tables.add(map(resultSet));
            }
            return tables;
        } catch (SQLException exception) {
            throw new IllegalStateException("Erreur de lecture des tables.", exception);
        }
    }

    public RestaurantTable findById(int id) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM restaurant_tables WHERE id = ?")) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Erreur de lecture de la table.", exception);
        }
    }

    public long countByStatus(String status) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM restaurant_tables WHERE status = ?")) {
            statement.setString(1, status);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : 0;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Erreur de comptage des tables.", exception);
        }
    }

    public int save(RestaurantTable table) {
        String sql = "INSERT INTO restaurant_tables(number, name, capacity, zone, status, note) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, table);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible d'enregistrer la table.", exception);
        }
    }

    public void update(RestaurantTable table) {
        String sql = "UPDATE restaurant_tables SET number = ?, name = ?, capacity = ?, zone = ?, status = ?, note = ? WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, table);
            statement.setInt(7, table.id());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de modifier la table.", exception);
        }
    }

    public void updateStatus(int tableId, String status) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE restaurant_tables SET status = ? WHERE id = ?")) {
            statement.setString(1, status);
            statement.setInt(2, tableId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de modifier le statut de table.", exception);
        }
    }

    public void delete(int id) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM restaurant_tables WHERE id = ?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de supprimer la table.", exception);
        }
    }

    private void bind(PreparedStatement statement, RestaurantTable table) throws SQLException {
        statement.setString(1, table.number());
        statement.setString(2, table.name());
        statement.setInt(3, table.capacity());
        statement.setString(4, table.zone());
        statement.setString(5, table.status());
        statement.setString(6, table.note());
    }

    private RestaurantTable map(ResultSet resultSet) throws SQLException {
        return new RestaurantTable(
                resultSet.getInt("id"),
                resultSet.getString("number"),
                resultSet.getString("name"),
                resultSet.getInt("capacity"),
                resultSet.getString("zone"),
                resultSet.getString("status"),
                resultSet.getString("note")
        );
    }
}
