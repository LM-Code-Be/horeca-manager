package com.lmcode.horecamanager.repositories;

import com.lmcode.horecamanager.database.DatabaseManager;
import com.lmcode.horecamanager.models.Ticket;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TicketRepository {
    public int save(Ticket ticket) {
        String sql = "INSERT INTO tickets(order_id, ticket_number, content, total, created_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, ticket.orderId());
            statement.setString(2, ticket.ticketNumber());
            statement.setString(3, ticket.content());
            statement.setDouble(4, ticket.total());
            statement.setString(5, (ticket.createdAt() == null ? LocalDateTime.now() : ticket.createdAt()).toString());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible d'enregistrer le ticket.", exception);
        }
    }

    public List<Ticket> findAll() {
        List<Ticket> tickets = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM tickets ORDER BY created_at DESC");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                tickets.add(map(resultSet));
            }
            return tickets;
        } catch (SQLException exception) {
            throw new IllegalStateException("Erreur de lecture des tickets.", exception);
        }
    }

    public Ticket findLatest() {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM tickets ORDER BY created_at DESC LIMIT 1");
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? map(resultSet) : null;
        } catch (SQLException exception) {
            throw new IllegalStateException("Erreur de lecture du dernier ticket.", exception);
        }
    }

    public long countToday() {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM tickets WHERE date(created_at) = date(?)")) {
            statement.setString(1, LocalDate.now().toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : 0;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Erreur de comptage des tickets.", exception);
        }
    }

    private Ticket map(ResultSet resultSet) throws SQLException {
        return new Ticket(
                resultSet.getInt("id"),
                resultSet.getInt("order_id"),
                resultSet.getString("ticket_number"),
                resultSet.getString("content"),
                resultSet.getDouble("total"),
                LocalDateTime.parse(resultSet.getString("created_at"))
        );
    }
}
