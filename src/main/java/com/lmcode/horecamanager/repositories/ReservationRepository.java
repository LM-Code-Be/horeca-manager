package com.lmcode.horecamanager.repositories;

import com.lmcode.horecamanager.database.DatabaseManager;
import com.lmcode.horecamanager.models.Reservation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ReservationRepository {
    public List<Reservation> findAll() {
        return find("""
                SELECT * FROM reservations
                ORDER BY reservation_date, reservation_time
                """);
    }

    public List<Reservation> findToday() {
        List<Reservation> reservations = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT * FROM reservations
                     WHERE reservation_date = ?
                     ORDER BY reservation_time
                     """)) {
            statement.setString(1, LocalDate.now().toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    reservations.add(map(resultSet));
                }
            }
            return reservations;
        } catch (SQLException exception) {
            throw new IllegalStateException("Erreur de lecture des reservations.", exception);
        }
    }

    public Reservation findById(int id) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM reservations WHERE id = ?")) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Erreur de lecture de reservation.", exception);
        }
    }

    public List<Reservation> search(LocalDate date, String query) {
        List<Reservation> reservations = new ArrayList<>();
        String sql = """
                SELECT * FROM reservations
                WHERE (? IS NULL OR reservation_date = ?)
                  AND (
                    ? IS NULL
                    OR lower(customer_name) LIKE lower(?)
                    OR lower(COALESCE(phone, '')) LIKE lower(?)
                    OR lower(COALESCE(email, '')) LIKE lower(?)
                    OR lower(status) LIKE lower(?)
                    OR lower(COALESCE(note, '')) LIKE lower(?)
                    OR reservation_date LIKE ?
                    OR reservation_time LIKE ?
                    OR CAST(guests_count AS TEXT) LIKE ?
                    OR CAST(COALESCE(table_id, '') AS TEXT) LIKE ?
                  )
                ORDER BY reservation_date, reservation_time
                """;
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            String dateValue = date == null ? null : date.toString();
            String queryValue = query == null || query.isBlank() ? null : "%" + query.trim() + "%";
            statement.setString(1, dateValue);
            statement.setString(2, dateValue);
            for (int index = 3; index <= 12; index++) {
                statement.setString(index, queryValue);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    reservations.add(map(resultSet));
                }
            }
            return reservations;
        } catch (SQLException exception) {
            throw new IllegalStateException("Erreur de recherche des reservations.", exception);
        }
    }

    private List<Reservation> find(String sql) {
        List<Reservation> reservations = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                reservations.add(map(resultSet));
            }
            return reservations;
        } catch (SQLException exception) {
            throw new IllegalStateException("Erreur de lecture des reservations.", exception);
        }
    }

    public int save(Reservation reservation) {
        String sql = """
                INSERT INTO reservations(customer_name, phone, email, reservation_date, reservation_time, guests_count, table_id, status, note, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, reservation);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible d'enregistrer la reservation.", exception);
        }
    }

    public void update(Reservation reservation) {
        String sql = """
                UPDATE reservations SET customer_name = ?, phone = ?, email = ?, reservation_date = ?, reservation_time = ?,
                    guests_count = ?, table_id = ?, status = ?, note = ?, created_at = ?
                WHERE id = ?
                """;
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, reservation);
            statement.setInt(11, reservation.id());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de modifier la reservation.", exception);
        }
    }

    public void updateStatus(int id, String status) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE reservations SET status = ? WHERE id = ?")) {
            statement.setString(1, status);
            statement.setInt(2, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de modifier le statut de reservation.", exception);
        }
    }

    public void delete(int id) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM reservations WHERE id = ?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de supprimer la reservation.", exception);
        }
    }

    private void bind(PreparedStatement statement, Reservation reservation) throws SQLException {
        statement.setString(1, reservation.customerName());
        statement.setString(2, reservation.phone());
        statement.setString(3, reservation.email());
        statement.setString(4, reservation.reservationDate().toString());
        statement.setString(5, reservation.reservationTime().toString());
        statement.setInt(6, reservation.guestsCount());
        RepositorySupport.setNullableInt(statement, 7, reservation.tableId());
        statement.setString(8, reservation.status());
        statement.setString(9, reservation.note());
        statement.setString(10, (reservation.createdAt() == null ? LocalDateTime.now() : reservation.createdAt()).toString());
    }

    private Reservation map(ResultSet resultSet) throws SQLException {
        return new Reservation(
                resultSet.getInt("id"),
                resultSet.getString("customer_name"),
                resultSet.getString("phone"),
                resultSet.getString("email"),
                LocalDate.parse(resultSet.getString("reservation_date")),
                LocalTime.parse(resultSet.getString("reservation_time")),
                resultSet.getInt("guests_count"),
                RepositorySupport.nullableInt(resultSet, "table_id"),
                resultSet.getString("status"),
                resultSet.getString("note"),
                LocalDateTime.parse(resultSet.getString("created_at"))
        );
    }
}
