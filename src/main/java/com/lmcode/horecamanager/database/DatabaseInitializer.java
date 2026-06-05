package com.lmcode.horecamanager.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public final class DatabaseInitializer {
    private DatabaseInitializer() {
    }

    public static void initialize() {
        try (Connection connection = DatabaseManager.getConnection()) {
            createTables(connection);
            seedDefaults(connection);
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible d'initialiser la base SQLite.", exception);
        }
    }

    private static void createTables(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS restaurant_tables (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        number TEXT NOT NULL UNIQUE,
                        name TEXT,
                        capacity INTEGER NOT NULL,
                        zone TEXT,
                        status TEXT NOT NULL,
                        note TEXT
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS reservations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        customer_name TEXT NOT NULL,
                        phone TEXT,
                        email TEXT,
                        reservation_date TEXT NOT NULL,
                        reservation_time TEXT NOT NULL,
                        guests_count INTEGER NOT NULL,
                        table_id INTEGER,
                        status TEXT NOT NULL,
                        note TEXT,
                        created_at TEXT NOT NULL,
                        FOREIGN KEY(table_id) REFERENCES restaurant_tables(id) ON DELETE SET NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS categories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        description TEXT,
                        color TEXT,
                        is_active INTEGER DEFAULT 1
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS products (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        description TEXT,
                        category_id INTEGER,
                        price REAL NOT NULL,
                        vat_rate REAL DEFAULT 21,
                        is_available INTEGER DEFAULT 1,
                        stock_quantity INTEGER,
                        preparation_time INTEGER,
                        created_at TEXT NOT NULL,
                        FOREIGN KEY(category_id) REFERENCES categories(id) ON DELETE SET NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS orders (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        table_id INTEGER,
                        order_type TEXT NOT NULL,
                        status TEXT NOT NULL,
                        subtotal REAL DEFAULT 0,
                        discount REAL DEFAULT 0,
                        vat_amount REAL DEFAULT 0,
                        total REAL DEFAULT 0,
                        created_at TEXT NOT NULL,
                        closed_at TEXT,
                        FOREIGN KEY(table_id) REFERENCES restaurant_tables(id) ON DELETE SET NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS order_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        order_id INTEGER NOT NULL,
                        product_id INTEGER NOT NULL,
                        product_name TEXT NOT NULL,
                        quantity INTEGER NOT NULL,
                        unit_price REAL NOT NULL,
                        vat_rate REAL NOT NULL,
                        total REAL NOT NULL,
                        FOREIGN KEY(order_id) REFERENCES orders(id) ON DELETE CASCADE,
                        FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE RESTRICT
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS payments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        order_id INTEGER NOT NULL,
                        method TEXT NOT NULL,
                        amount REAL NOT NULL,
                        amount_received REAL,
                        change_due REAL,
                        paid_at TEXT NOT NULL,
                        FOREIGN KEY(order_id) REFERENCES orders(id) ON DELETE CASCADE
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS tickets (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        order_id INTEGER NOT NULL,
                        ticket_number TEXT NOT NULL UNIQUE,
                        content TEXT NOT NULL,
                        total REAL NOT NULL,
                        created_at TEXT NOT NULL,
                        FOREIGN KEY(order_id) REFERENCES orders(id) ON DELETE CASCADE
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS cash_sessions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        opened_at TEXT NOT NULL,
                        closed_at TEXT,
                        initial_amount REAL DEFAULT 0,
                        expected_amount REAL DEFAULT 0,
                        real_amount REAL,
                        difference_amount REAL,
                        status TEXT NOT NULL,
                        note TEXT
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS settings (
                        key TEXT PRIMARY KEY,
                        value TEXT NOT NULL
                    )
                    """);
            statement.execute("""
                    UPDATE orders
                    SET status = 'ANNULEE', closed_at = strftime('%Y-%m-%dT%H:%M:%f', 'now')
                    WHERE id IN (
                        SELECT id FROM (
                            SELECT id,
                                   ROW_NUMBER() OVER (PARTITION BY table_id ORDER BY created_at DESC, id DESC) AS row_rank
                            FROM orders
                            WHERE table_id IS NOT NULL AND status IN ('EN_COURS', 'EN_ATTENTE')
                        )
                        WHERE row_rank > 1
                    )
                    """);
            statement.execute("""
                    CREATE UNIQUE INDEX IF NOT EXISTS idx_orders_one_open_per_table
                    ON orders(table_id)
                    WHERE table_id IS NOT NULL AND status IN ('EN_COURS', 'EN_ATTENTE')
                    """);
        }
    }

    private static void seedDefaults(Connection connection) throws SQLException {
        if (isEmpty(connection, "restaurant_tables")) {
            seedTables(connection);
        }
        if (isEmpty(connection, "categories")) {
            seedCategories(connection);
        }
        if (isEmpty(connection, "products")) {
            seedProducts(connection);
        }
        if (isEmpty(connection, "reservations")) {
            seedReservations(connection);
        }
        if (isEmpty(connection, "orders")) {
            seedOrders(connection);
        }
        if (isEmpty(connection, "settings")) {
            seedSettings(connection);
        }
        if (isEmpty(connection, "payments")) {
            seedPaidSale(connection);
        }
    }

    private static boolean isEmpty(Connection connection, String table) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return resultSet.next() && resultSet.getInt(1) == 0;
        }
    }

    private static void seedTables(Connection connection) throws SQLException {
        String sql = "INSERT INTO restaurant_tables(number, name, capacity, zone, status, note) VALUES (?, ?, ?, ?, ?, ?)";
        Object[][] rows = {
                {"1", "Fenetre", 2, "Salle", "LIBRE", ""},
                {"2", "Salle centre", 4, "Salle", "OCCUPEE", "Commande en cours"},
                {"3", "Banquette", 4, "Salle", "RESERVEE", "19:30"},
                {"4", "Grande table", 6, "Salle", "LIBRE", ""},
                {"5", "Terrasse 1", 2, "Terrasse", "LIBRE", ""},
                {"6", "Terrasse 2", 4, "Terrasse", "A_NETTOYER", ""},
                {"7", "Bar gauche", 2, "Bar", "OCCUPEE", ""},
                {"8", "Bar droite", 2, "Bar", "LIBRE", ""},
                {"9", "Etage 1", 4, "Etage", "INDISPONIBLE", "Maintenance"},
                {"10", "Etage 2", 8, "Etage", "RESERVEE", "Groupe"}
        };
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Object[] row : rows) {
                bind(statement, row);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static void seedCategories(Connection connection) throws SQLException {
        String sql = "INSERT INTO categories(name, description, color, is_active) VALUES (?, ?, ?, ?)";
        Object[][] rows = {
                {"Boissons", "Boissons chaudes et froides", "#2563EB", 1},
                {"Entrees", "Assiettes legeres", "#22C55E", 1},
                {"Plats", "Cuisine principale", "#D97706", 1},
                {"Desserts", "Desserts maison", "#7C3AED", 1},
                {"Menus", "Formules et menus", "#0EA5E9", 1},
                {"Supplements", "Extras", "#F59E0B", 1}
        };
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Object[] row : rows) {
                bind(statement, row);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static void seedProducts(Connection connection) throws SQLException {
        String sql = """
                INSERT INTO products(name, description, category_id, price, vat_rate, is_available, stock_quantity, preparation_time, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        String now = LocalDateTime.now().toString();
        Object[][] rows = {
                {"Cafe", "Expresso", 1, 2.50, 6.0, 1, 200, 2, now},
                {"The", "Selection maison", 1, 3.00, 6.0, 1, 120, 3, now},
                {"Coca-Cola", "33 cl", 1, 3.20, 21.0, 1, 80, 1, now},
                {"Eau", "Bouteille 50 cl", 1, 2.40, 6.0, 1, 90, 1, now},
                {"Burger maison", "Boeuf, cheddar, sauce maison", 3, 16.90, 12.0, 1, 25, 14, now},
                {"Pizza Margherita", "Tomate, mozzarella, basilic", 3, 13.50, 12.0, 1, 30, 12, now},
                {"Salade Cesar", "Poulet, parmesan, croutons", 2, 12.80, 12.0, 1, 20, 8, now},
                {"Frites", "Portion classique", 6, 4.50, 12.0, 1, 60, 5, now},
                {"Tiramisu", "Dessert maison", 4, 6.90, 12.0, 1, 16, 4, now},
                {"Menu du jour", "Plat, boisson, dessert", 5, 19.90, 12.0, 1, 18, 15, now}
        };
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Object[] row : rows) {
                bind(statement, row);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static void seedReservations(Connection connection) throws SQLException {
        String sql = """
                INSERT INTO reservations(customer_name, phone, email, reservation_date, reservation_time, guests_count, table_id, status, note, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        LocalDate today = LocalDate.now();
        Object[][] rows = {
                {"Martin", "+32470111222", "martin@example.com", today.toString(), LocalTime.of(19, 30).toString(), 4, 3, "CONFIRMEE", "Pres de la fenetre", LocalDateTime.now().minusDays(1).toString()},
                {"Dubois", "+32472222333", "", today.toString(), LocalTime.of(20, 0).toString(), 2, 5, "EN_ATTENTE", "", LocalDateTime.now().toString()},
                {"Lambert", "+32473333444", "lambert@example.com", today.plusDays(1).toString(), LocalTime.of(12, 30).toString(), 6, 10, "CONFIRMEE", "Groupe", LocalDateTime.now().toString()}
        };
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Object[] row : rows) {
                bind(statement, row);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static void seedOrders(Connection connection) throws SQLException {
        int orderId;
        try (PreparedStatement order = connection.prepareStatement("""
                INSERT INTO orders(table_id, order_type, status, subtotal, discount, vat_amount, total, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            order.setInt(1, 2);
            order.setString(2, "SUR_PLACE");
            order.setString(3, "EN_COURS");
            order.setDouble(4, 27.40);
            order.setDouble(5, 0.0);
            order.setDouble(6, 2.94);
            order.setDouble(7, 27.40);
            order.setString(8, LocalDateTime.now().minusMinutes(35).toString());
            order.executeUpdate();
            try (ResultSet keys = order.getGeneratedKeys()) {
                keys.next();
                orderId = keys.getInt(1);
            }
        }
        try (PreparedStatement item = connection.prepareStatement("""
                INSERT INTO order_items(order_id, product_id, product_name, quantity, unit_price, vat_rate, total)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """)) {
            Object[][] rows = {
                    {orderId, 5, "Burger maison", 1, 16.90, 12.0, 16.90},
                    {orderId, 8, "Frites", 1, 4.50, 12.0, 4.50},
                    {orderId, 1, "Cafe", 2, 2.50, 6.0, 5.00}
            };
            for (Object[] row : rows) {
                bind(item, row);
                item.addBatch();
            }
            item.executeBatch();
        }
    }

    private static void seedPaidSale(Connection connection) throws SQLException {
        int orderId;
        LocalDateTime created = LocalDateTime.now().minusHours(2);
        LocalDateTime paid = LocalDateTime.now().minusHours(1).minusMinutes(35);
        try (PreparedStatement order = connection.prepareStatement("""
                INSERT INTO orders(table_id, order_type, status, subtotal, discount, vat_amount, total, created_at, closed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            order.setNull(1, java.sql.Types.INTEGER);
            order.setString(2, "A_EMPORTER");
            order.setString(3, "PAYEE");
            order.setDouble(4, 25.86);
            order.setDouble(5, 0.0);
            order.setDouble(6, 3.44);
            order.setDouble(7, 29.30);
            order.setString(8, created.toString());
            order.setString(9, paid.toString());
            order.executeUpdate();
            try (ResultSet keys = order.getGeneratedKeys()) {
                keys.next();
                orderId = keys.getInt(1);
            }
        }
        try (PreparedStatement item = connection.prepareStatement("""
                INSERT INTO order_items(order_id, product_id, product_name, quantity, unit_price, vat_rate, total)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """)) {
            Object[][] rows = {
                    {orderId, 6, "Pizza Margherita", 1, 13.50, 12.0, 13.50},
                    {orderId, 3, "Coca-Cola", 2, 3.20, 21.0, 6.40},
                    {orderId, 9, "Tiramisu", 1, 6.90, 12.0, 6.90},
                    {orderId, 1, "Cafe", 1, 2.50, 6.0, 2.50}
            };
            for (Object[] row : rows) {
                bind(item, row);
                item.addBatch();
            }
            item.executeBatch();
        }
        try (PreparedStatement payment = connection.prepareStatement("""
                INSERT INTO payments(order_id, method, amount, amount_received, change_due, paid_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """)) {
            payment.setInt(1, orderId);
            payment.setString(2, "CARTE");
            payment.setDouble(3, 29.30);
            payment.setDouble(4, 29.30);
            payment.setDouble(5, 0);
            payment.setString(6, paid.toString());
            payment.executeUpdate();
        }
        String ticketNumber = "T" + paid.toLocalDate().toString().replace("-", "") + "-DEMO-" + orderId;
        String content = """
                HorecaManager Demo
                Rue de la Caisse 12, 1000 Bruxelles
                +32 2 123 45 67

                Ticket %s
                Commande #%d

                1 x Pizza Margherita  13,50 EUR
                2 x Coca-Cola          6,40 EUR
                1 x Tiramisu           6,90 EUR
                1 x Cafe               2,50 EUR

                Total HT  25,86 EUR
                TVA        3,44 EUR
                Total TTC 29,30 EUR
                Paiement  CARTE

                Merci pour votre visite.
                """.formatted(ticketNumber, orderId);
        try (PreparedStatement ticket = connection.prepareStatement("""
                INSERT INTO tickets(order_id, ticket_number, content, total, created_at)
                VALUES (?, ?, ?, ?, ?)
                """)) {
            ticket.setInt(1, orderId);
            ticket.setString(2, ticketNumber);
            ticket.setString(3, content);
            ticket.setDouble(4, 29.30);
            ticket.setString(5, paid.toString());
            ticket.executeUpdate();
        }
        if (isEmpty(connection, "cash_sessions")) {
            try (PreparedStatement session = connection.prepareStatement("""
                    INSERT INTO cash_sessions(opened_at, closed_at, initial_amount, expected_amount, real_amount, difference_amount, status, note)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """)) {
                session.setString(1, created.minusMinutes(20).toString());
                session.setString(2, paid.plusHours(2).toString());
                session.setDouble(3, 100.0);
                session.setDouble(4, 129.30);
                session.setDouble(5, 129.30);
                session.setDouble(6, 0.0);
                session.setString(7, "FERMEE");
                session.setString(8, "Session de demonstration");
                session.executeUpdate();
            }
        }
    }

    private static void seedSettings(Connection connection) throws SQLException {
        String sql = "INSERT INTO settings(key, value) VALUES (?, ?)";
        Object[][] rows = {
                {"restaurant.name", "HorecaManager Demo"},
                {"restaurant.address", "Rue de la Caisse 12, 1000 Bruxelles"},
                {"restaurant.phone", "+32 2 123 45 67"},
                {"restaurant.email", "contact@horecamanager.local"},
                {"restaurant.vatNumber", "BE0123456789"},
                {"restaurant.currency", "EUR"},
                {"restaurant.defaultVatRate", "21"},
                {"ticket.message", "Merci pour votre visite."},
                {"app.theme", "light"},
                {"app.language", "fr"},
                {"app.exportFolder", "exports"},
                {"app.printingEnabled", "false"},
                {"app.autoBackup", "true"},
                {"stock.lowThreshold", "5"}
        };
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Object[] row : rows) {
                bind(statement, row);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static void bind(PreparedStatement statement, Object[] values) throws SQLException {
        for (int i = 0; i < values.length; i++) {
            statement.setObject(i + 1, values[i]);
        }
    }
}
