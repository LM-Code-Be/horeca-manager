package com.lmcode.horecamanager.repositories;

import com.lmcode.horecamanager.database.DatabaseManager;
import com.lmcode.horecamanager.models.Product;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ProductRepository {
    public static final int DEFAULT_LOW_STOCK_THRESHOLD = 5;

    public List<Product> findAll() {
        return find("SELECT * FROM products ORDER BY name");
    }

    public List<Product> findAvailable() {
        return find("SELECT * FROM products WHERE is_available = 1 AND (stock_quantity IS NULL OR stock_quantity > 0) ORDER BY name");
    }

    public List<Product> findByCategory(Integer categoryId) {
        if (categoryId == null) {
            return findAvailable();
        }
        List<Product> products = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT * FROM products
                     WHERE is_available = 1
                       AND (stock_quantity IS NULL OR stock_quantity > 0)
                       AND category_id = ?
                     ORDER BY name
                     """)) {
            statement.setInt(1, categoryId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    products.add(map(resultSet));
                }
            }
            return products;
        } catch (SQLException exception) {
            throw new IllegalStateException("Erreur de lecture des produits.", exception);
        }
    }

    private List<Product> find(String sql) {
        List<Product> products = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                products.add(map(resultSet));
            }
            return products;
        } catch (SQLException exception) {
            throw new IllegalStateException("Erreur de lecture des produits.", exception);
        }
    }

    public Product findById(int id) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM products WHERE id = ?")) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Erreur de lecture du produit.", exception);
        }
    }

    public int save(Product product) {
        String sql = """
                INSERT INTO products(name, description, category_id, price, vat_rate, is_available, stock_quantity, preparation_time, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, product, false);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible d'enregistrer le produit.", exception);
        }
    }

    public void update(Product product) {
        String sql = """
                UPDATE products SET name = ?, description = ?, category_id = ?, price = ?, vat_rate = ?,
                    is_available = ?, stock_quantity = ?, preparation_time = ?
                WHERE id = ?
                """;
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, product, true);
            statement.setInt(9, product.id());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de modifier le produit.", exception);
        }
    }

    public void updateAvailability(int productId, boolean available) {
        Product product = findById(productId);
        if (available && product != null && product.stockQuantity() != null && product.stockQuantity() <= 0) {
            throw new IllegalArgumentException("Stock a zero. Reapprovisionnez le produit avant de l'activer.");
        }
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE products SET is_available = ? WHERE id = ?")) {
            statement.setInt(1, available ? 1 : 0);
            statement.setInt(2, productId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de changer la disponibilite.", exception);
        }
    }

    public void decreaseStock(int productId, int quantity) {
        if (quantity <= 0) {
            return;
        }
        Product product = findById(productId);
        if (product == null || product.stockQuantity() == null) {
            return;
        }
        if (product.stockQuantity() < quantity) {
            throw new IllegalArgumentException("Stock insuffisant pour " + product.name() + ".");
        }
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE products
                     SET stock_quantity = stock_quantity - ?,
                         is_available = CASE WHEN stock_quantity - ? <= 0 THEN 0 ELSE is_available END
                     WHERE id = ?
                     """)) {
            statement.setInt(1, quantity);
            statement.setInt(2, quantity);
            statement.setInt(3, productId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de mettre a jour le stock.", exception);
        }
    }

    public void restock(int productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantite de reapprovisionnement invalide.");
        }
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE products
                     SET stock_quantity = COALESCE(stock_quantity, 0) + ?,
                         is_available = 1
                     WHERE id = ?
                     """)) {
            statement.setInt(1, quantity);
            statement.setInt(2, productId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de reapprovisionner le produit.", exception);
        }
    }

    public List<Product> findLowStock(int threshold) {
        List<Product> products = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT * FROM products
                     WHERE stock_quantity IS NOT NULL
                       AND stock_quantity > 0
                       AND stock_quantity <= ?
                     ORDER BY stock_quantity ASC, name
                     """)) {
            statement.setInt(1, threshold);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    products.add(map(resultSet));
                }
            }
            return products;
        } catch (SQLException exception) {
            throw new IllegalStateException("Erreur de lecture des stocks faibles.", exception);
        }
    }

    public List<Product> findOutOfStock() {
        return find("""
                SELECT * FROM products
                WHERE stock_quantity IS NOT NULL AND stock_quantity <= 0
                ORDER BY name
                """);
    }

    private void bind(PreparedStatement statement, Product product, boolean update) throws SQLException {
        statement.setString(1, product.name());
        statement.setString(2, product.description());
        RepositorySupport.setNullableInt(statement, 3, product.categoryId());
        statement.setDouble(4, product.price());
        statement.setDouble(5, product.vatRate());
        boolean available = product.available() && (product.stockQuantity() == null || product.stockQuantity() > 0);
        statement.setInt(6, available ? 1 : 0);
        RepositorySupport.setNullableInt(statement, 7, product.stockQuantity());
        RepositorySupport.setNullableInt(statement, 8, product.preparationTime());
        if (!update) {
            statement.setString(9, (product.createdAt() == null ? LocalDateTime.now() : product.createdAt()).toString());
        }
    }

    private Product map(ResultSet resultSet) throws SQLException {
        Integer stock = RepositorySupport.nullableInt(resultSet, "stock_quantity");
        return new Product(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getString("description"),
                RepositorySupport.nullableInt(resultSet, "category_id"),
                resultSet.getDouble("price"),
                resultSet.getDouble("vat_rate"),
                resultSet.getInt("is_available") == 1 && (stock == null || stock > 0),
                stock,
                RepositorySupport.nullableInt(resultSet, "preparation_time"),
                LocalDateTime.parse(resultSet.getString("created_at"))
        );
    }
}
