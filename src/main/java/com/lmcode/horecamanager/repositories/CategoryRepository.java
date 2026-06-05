package com.lmcode.horecamanager.repositories;

import com.lmcode.horecamanager.database.DatabaseManager;
import com.lmcode.horecamanager.models.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CategoryRepository {
    public List<Category> findAll() {
        return find(false);
    }

    public List<Category> findActive() {
        return find(true);
    }

    private List<Category> find(boolean activeOnly) {
        String sql = activeOnly ? "SELECT * FROM categories WHERE is_active = 1 ORDER BY name" : "SELECT * FROM categories ORDER BY name";
        List<Category> categories = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                categories.add(map(resultSet));
            }
            return categories;
        } catch (SQLException exception) {
            throw new IllegalStateException("Erreur de lecture des categories.", exception);
        }
    }

    public Category findById(int id) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM categories WHERE id = ?")) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Erreur de lecture de categorie.", exception);
        }
    }

    public void save(Category category) {
        String sql = "INSERT INTO categories(name, description, color, is_active) VALUES (?, ?, ?, ?)";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, category.name());
            statement.setString(2, category.description());
            statement.setString(3, category.color());
            statement.setInt(4, category.active() ? 1 : 0);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible d'enregistrer la categorie.", exception);
        }
    }

    private Category map(ResultSet resultSet) throws SQLException {
        return new Category(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getString("description"),
                resultSet.getString("color"),
                resultSet.getInt("is_active") == 1
        );
    }
}
