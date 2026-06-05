package com.lmcode.horecamanager.repositories;

import com.lmcode.horecamanager.database.DatabaseManager;
import com.lmcode.horecamanager.models.Payment;

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

public class PaymentRepository {
    public int save(Payment payment) {
        String sql = """
                INSERT INTO payments(order_id, method, amount, amount_received, change_due, paid_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, payment.orderId());
            statement.setString(2, payment.method());
            statement.setDouble(3, payment.amount());
            RepositorySupport.setNullableDouble(statement, 4, payment.amountReceived());
            RepositorySupport.setNullableDouble(statement, 5, payment.changeDue());
            statement.setString(6, (payment.paidAt() == null ? LocalDateTime.now() : payment.paidAt()).toString());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible d'enregistrer le paiement.", exception);
        }
    }

    public List<Payment> findRecent(int limit) {
        List<Payment> payments = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM payments ORDER BY paid_at DESC LIMIT ?")) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    payments.add(map(resultSet));
                }
            }
            return payments;
        } catch (SQLException exception) {
            throw new IllegalStateException("Erreur de lecture des paiements.", exception);
        }
    }

    public Map<String, Double> totalsByMethodToday() {
        Map<String, Double> totals = new LinkedHashMap<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT method, COALESCE(SUM(amount), 0) AS total
                     FROM payments
                     WHERE date(paid_at) = date(?)
                     GROUP BY method
                     ORDER BY method
                     """)) {
            statement.setString(1, LocalDate.now().toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    totals.put(resultSet.getString("method"), resultSet.getDouble("total"));
                }
            }
            return totals;
        } catch (SQLException exception) {
            throw new IllegalStateException("Erreur de lecture des paiements.", exception);
        }
    }

    public double totalToday() {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COALESCE(SUM(amount), 0) FROM payments WHERE date(paid_at) = date(?)")) {
            statement.setString(1, LocalDate.now().toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getDouble(1) : 0;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Erreur de calcul des paiements.", exception);
        }
    }

    public long countToday() {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM payments WHERE date(paid_at) = date(?)")) {
            statement.setString(1, LocalDate.now().toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : 0;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Erreur de comptage des paiements.", exception);
        }
    }

    private Payment map(ResultSet resultSet) throws SQLException {
        return new Payment(
                resultSet.getInt("id"),
                resultSet.getInt("order_id"),
                resultSet.getString("method"),
                resultSet.getDouble("amount"),
                RepositorySupport.nullableDouble(resultSet, "amount_received"),
                RepositorySupport.nullableDouble(resultSet, "change_due"),
                LocalDateTime.parse(resultSet.getString("paid_at"))
        );
    }
}
