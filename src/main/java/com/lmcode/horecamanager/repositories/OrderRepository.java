package com.lmcode.horecamanager.repositories;

import com.lmcode.horecamanager.database.DatabaseManager;
import com.lmcode.horecamanager.models.Order;
import com.lmcode.horecamanager.models.OrderItem;
import com.lmcode.horecamanager.models.Product;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OrderRepository {
    public int create(Integer tableId, String orderType) {
        String sql = """
                INSERT INTO orders(table_id, order_type, status, subtotal, discount, vat_amount, total, created_at)
                VALUES (?, ?, 'EN_COURS', 0, 0, 0, 0, ?)
                """;
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            RepositorySupport.setNullableInt(statement, 1, tableId);
            statement.setString(2, orderType);
            statement.setString(3, LocalDateTime.now().toString());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de creer la commande.", exception);
        }
    }

    public List<Order> findAll() {
        return find("SELECT * FROM orders ORDER BY created_at DESC");
    }

    public List<Order> findOpenOrders() {
        return find("SELECT * FROM orders WHERE status IN ('EN_COURS', 'EN_ATTENTE') ORDER BY created_at DESC");
    }

    public Order findOpenByTableId(int tableId) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT * FROM orders
                     WHERE table_id = ? AND status IN ('EN_COURS', 'EN_ATTENTE')
                     ORDER BY created_at DESC
                     LIMIT 1
                     """)) {
            statement.setInt(1, tableId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapOrder(resultSet) : null;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Erreur de lecture de la commande de table.", exception);
        }
    }

    private List<Order> find(String sql) {
        List<Order> orders = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                orders.add(mapOrder(resultSet));
            }
            return orders;
        } catch (SQLException exception) {
            throw new IllegalStateException("Erreur de lecture des commandes.", exception);
        }
    }

    public Order findById(int id) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM orders WHERE id = ?")) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapOrder(resultSet) : null;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Erreur de lecture de commande.", exception);
        }
    }

    public List<OrderItem> findItems(int orderId) {
        List<OrderItem> items = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM order_items WHERE order_id = ? ORDER BY id")) {
            statement.setInt(1, orderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    items.add(mapItem(resultSet));
                }
            }
            return items;
        } catch (SQLException exception) {
            throw new IllegalStateException("Erreur de lecture du panier.", exception);
        }
    }

    public int reservedQuantityForProduct(int productId, int excludedOrderId) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT COALESCE(SUM(oi.quantity), 0)
                     FROM order_items oi
                     JOIN orders o ON o.id = oi.order_id
                     WHERE oi.product_id = ?
                       AND o.status IN ('EN_COURS', 'EN_ATTENTE')
                       AND o.id <> ?
                     """)) {
            statement.setInt(1, productId);
            statement.setInt(2, excludedOrderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Erreur de lecture du stock reserve.", exception);
        }
    }

    public OrderItem findItemById(int itemId) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM order_items WHERE id = ?")) {
            statement.setInt(1, itemId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapItem(resultSet) : null;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Erreur de lecture de ligne de commande.", exception);
        }
    }

    public void addItem(int orderId, Product product, int quantity) {
        String selectSql = "SELECT id, quantity FROM order_items WHERE order_id = ? AND product_id = ?";
        try (Connection connection = DatabaseManager.getConnection()) {
            try (PreparedStatement select = connection.prepareStatement(selectSql)) {
                select.setInt(1, orderId);
                select.setInt(2, product.id());
                try (ResultSet resultSet = select.executeQuery()) {
                    if (resultSet.next()) {
                        int itemId = resultSet.getInt("id");
                        int newQuantity = resultSet.getInt("quantity") + quantity;
                        updateItemQuantity(connection, itemId, newQuantity, product.price());
                    } else {
                        insertItem(connection, orderId, product, quantity);
                    }
                }
            }
            recalculateTotals(connection, orderId);
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible d'ajouter le produit.", exception);
        }
    }

    public void updateItemQuantity(int itemId, int quantity) {
        try (Connection connection = DatabaseManager.getConnection()) {
            int orderId = findOrderIdForItem(connection, itemId);
            if (quantity <= 0) {
                try (PreparedStatement statement = connection.prepareStatement("DELETE FROM order_items WHERE id = ?")) {
                    statement.setInt(1, itemId);
                    statement.executeUpdate();
                }
            } else {
                try (PreparedStatement statement = connection.prepareStatement("UPDATE order_items SET quantity = ?, total = unit_price * ? WHERE id = ?")) {
                    statement.setInt(1, quantity);
                    statement.setInt(2, quantity);
                    statement.setInt(3, itemId);
                    statement.executeUpdate();
                }
            }
            recalculateTotals(connection, orderId);
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de modifier la quantite.", exception);
        }
    }

    public void applyDiscount(int orderId, double discount) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE orders SET discount = ? WHERE id = ?")) {
            statement.setDouble(1, Math.max(0, discount));
            statement.setInt(2, orderId);
            statement.executeUpdate();
            recalculateTotals(connection, orderId);
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible d'appliquer la remise.", exception);
        }
    }

    public void markWaiting(int orderId) {
        updateStatus(orderId, "EN_ATTENTE", null);
    }

    public void resume(int orderId) {
        updateStatus(orderId, "EN_COURS", null);
    }

    public void close(int orderId) {
        updateStatus(orderId, "PAYEE", LocalDateTime.now());
    }

    public void cancel(int orderId) {
        updateStatus(orderId, "ANNULEE", LocalDateTime.now());
    }

    private void updateStatus(int orderId, String status, LocalDateTime closedAt) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE orders SET status = ?, closed_at = ? WHERE id = ?")) {
            statement.setString(1, status);
            statement.setString(2, closedAt == null ? null : closedAt.toString());
            statement.setInt(3, orderId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de modifier la commande.", exception);
        }
    }

    public long countOpen() {
        return count("SELECT COUNT(*) FROM orders WHERE status IN ('EN_COURS', 'EN_ATTENTE')");
    }

    public double totalPaidToday() {
        return sum("""
                SELECT COALESCE(SUM(total), 0) FROM orders
                WHERE status = 'PAYEE' AND date(closed_at) = date(?)
                """, LocalDate.now().toString());
    }

    public double averageBasketToday() {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT COALESCE(AVG(total), 0) FROM orders
                     WHERE status = 'PAYEE' AND date(closed_at) = date(?)
                     """)) {
            statement.setString(1, LocalDate.now().toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getDouble(1) : 0;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Erreur de calcul du panier moyen.", exception);
        }
    }

    public Map<String, Double> topProducts() {
        Map<String, Double> rows = new LinkedHashMap<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT oi.product_name, SUM(oi.quantity) AS qty
                     FROM order_items oi
                     JOIN orders o ON o.id = oi.order_id
                     WHERE o.status = 'PAYEE'
                     GROUP BY oi.product_name
                     ORDER BY qty DESC
                     LIMIT 6
                     """);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                rows.put(resultSet.getString("product_name"), resultSet.getDouble("qty"));
            }
            return rows;
        } catch (SQLException exception) {
            throw new IllegalStateException("Erreur de lecture des ventes produits.", exception);
        }
    }

    public Map<String, Double> salesByCategory() {
        Map<String, Double> rows = new LinkedHashMap<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT COALESCE(c.name, 'Autres') AS category, SUM(oi.total) AS amount
                     FROM order_items oi
                     JOIN orders o ON o.id = oi.order_id
                     JOIN products p ON p.id = oi.product_id
                     LEFT JOIN categories c ON c.id = p.category_id
                     WHERE o.status = 'PAYEE'
                     GROUP BY category
                     ORDER BY amount DESC
                     """);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                rows.put(resultSet.getString("category"), resultSet.getDouble("amount"));
            }
            return rows;
        } catch (SQLException exception) {
            throw new IllegalStateException("Erreur de lecture des ventes par categorie.", exception);
        }
    }

    private long count(String sql) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getLong(1) : 0;
        } catch (SQLException exception) {
            throw new IllegalStateException("Erreur de comptage des commandes.", exception);
        }
    }

    private double sum(String sql, String value) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getDouble(1) : 0;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Erreur de calcul des ventes.", exception);
        }
    }

    private void insertItem(Connection connection, int orderId, Product product, int quantity) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO order_items(order_id, product_id, product_name, quantity, unit_price, vat_rate, total)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setInt(1, orderId);
            statement.setInt(2, product.id());
            statement.setString(3, product.name());
            statement.setInt(4, quantity);
            statement.setDouble(5, product.price());
            statement.setDouble(6, product.vatRate());
            statement.setDouble(7, product.price() * quantity);
            statement.executeUpdate();
        }
    }

    private void updateItemQuantity(Connection connection, int itemId, int quantity, double price) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("UPDATE order_items SET quantity = ?, total = ? WHERE id = ?")) {
            statement.setInt(1, quantity);
            statement.setDouble(2, price * quantity);
            statement.setInt(3, itemId);
            statement.executeUpdate();
        }
    }

    private int findOrderIdForItem(Connection connection, int itemId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT order_id FROM order_items WHERE id = ?")) {
            statement.setInt(1, itemId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        }
    }

    private void recalculateTotals(Connection connection, int orderId) throws SQLException {
        double subtotalHt = 0;
        double vat = 0;
        double gross = 0;
        try (PreparedStatement statement = connection.prepareStatement("SELECT quantity, unit_price, vat_rate FROM order_items WHERE order_id = ?")) {
            statement.setInt(1, orderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    double lineTotal = resultSet.getInt("quantity") * resultSet.getDouble("unit_price");
                    double lineHt = lineTotal / (1 + resultSet.getDouble("vat_rate") / 100.0);
                    gross += lineTotal;
                    subtotalHt += lineHt;
                    vat += lineTotal - lineHt;
                }
            }
        }
        double discount;
        try (PreparedStatement statement = connection.prepareStatement("SELECT discount FROM orders WHERE id = ?")) {
            statement.setInt(1, orderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                discount = resultSet.next() ? resultSet.getDouble("discount") : 0;
            }
        }
        double total = Math.max(0, gross - discount);
        try (PreparedStatement statement = connection.prepareStatement("UPDATE orders SET subtotal = ?, vat_amount = ?, total = ? WHERE id = ?")) {
            statement.setDouble(1, subtotalHt);
            statement.setDouble(2, vat);
            statement.setDouble(3, total);
            statement.setInt(4, orderId);
            statement.executeUpdate();
        }
    }

    private Order mapOrder(ResultSet resultSet) throws SQLException {
        return new Order(
                resultSet.getInt("id"),
                RepositorySupport.nullableInt(resultSet, "table_id"),
                resultSet.getString("order_type"),
                resultSet.getString("status"),
                resultSet.getDouble("subtotal"),
                resultSet.getDouble("discount"),
                resultSet.getDouble("vat_amount"),
                resultSet.getDouble("total"),
                LocalDateTime.parse(resultSet.getString("created_at")),
                RepositorySupport.nullableDateTime(resultSet, "closed_at")
        );
    }

    private OrderItem mapItem(ResultSet resultSet) throws SQLException {
        return new OrderItem(
                resultSet.getInt("id"),
                resultSet.getInt("order_id"),
                resultSet.getInt("product_id"),
                resultSet.getString("product_name"),
                resultSet.getInt("quantity"),
                resultSet.getDouble("unit_price"),
                resultSet.getDouble("vat_rate"),
                resultSet.getDouble("total")
        );
    }
}
