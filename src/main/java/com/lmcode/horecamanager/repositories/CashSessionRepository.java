package com.lmcode.horecamanager.repositories;

import com.lmcode.horecamanager.database.DatabaseManager;
import com.lmcode.horecamanager.models.CashSession;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;

public class CashSessionRepository {
    public CashSession findOpen() {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM cash_sessions WHERE status = 'OUVERTE' ORDER BY opened_at DESC LIMIT 1");
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? map(resultSet) : null;
        } catch (SQLException exception) {
            throw new IllegalStateException("Erreur de lecture de la caisse.", exception);
        }
    }

    public CashSession findLatest() {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM cash_sessions ORDER BY opened_at DESC LIMIT 1");
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? map(resultSet) : null;
        } catch (SQLException exception) {
            throw new IllegalStateException("Erreur de lecture de la caisse.", exception);
        }
    }

    public boolean isOpen() {
        return findOpen() != null;
    }

    public int open(double initialAmount, String note) {
        String sql = """
                INSERT INTO cash_sessions(opened_at, initial_amount, expected_amount, status, note)
                VALUES (?, ?, ?, 'OUVERTE', ?)
                """;
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, LocalDateTime.now().toString());
            statement.setDouble(2, initialAmount);
            statement.setDouble(3, initialAmount);
            statement.setString(4, note);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible d'ouvrir la caisse.", exception);
        }
    }

    public void increaseExpected(double amount) {
        CashSession open = findOpen();
        if (open == null) {
            return;
        }
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE cash_sessions SET expected_amount = expected_amount + ? WHERE id = ?")) {
            statement.setDouble(1, amount);
            statement.setInt(2, open.id());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de mettre a jour la caisse.", exception);
        }
    }

    public void close(int sessionId, double realAmount, String note) {
        CashSession session = findLatest();
        if (session == null || session.id() != sessionId) {
            throw new IllegalStateException("Session de caisse introuvable.");
        }
        double difference = realAmount - session.expectedAmount();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE cash_sessions
                     SET closed_at = ?, real_amount = ?, difference_amount = ?, status = 'FERMEE', note = ?
                     WHERE id = ?
                     """)) {
            statement.setString(1, LocalDateTime.now().toString());
            statement.setDouble(2, realAmount);
            statement.setDouble(3, difference);
            statement.setString(4, note);
            statement.setInt(5, sessionId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de fermer la caisse.", exception);
        }
    }

    private CashSession map(ResultSet resultSet) throws SQLException {
        return new CashSession(
                resultSet.getInt("id"),
                LocalDateTime.parse(resultSet.getString("opened_at")),
                RepositorySupport.nullableDateTime(resultSet, "closed_at"),
                resultSet.getDouble("initial_amount"),
                resultSet.getDouble("expected_amount"),
                RepositorySupport.nullableDouble(resultSet, "real_amount"),
                RepositorySupport.nullableDouble(resultSet, "difference_amount"),
                resultSet.getString("status"),
                resultSet.getString("note")
        );
    }
}
