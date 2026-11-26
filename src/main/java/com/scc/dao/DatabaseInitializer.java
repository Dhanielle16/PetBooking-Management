
package com.scc.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void initialize() {

        try (Connection conn = DatabaseConnection.getConnection()) {

            conn.setAutoCommit(false);

            try {

                // Verify required existing tables.
                if (!tableExists(conn, "users")) {
                    throw new SQLException("The users table does not exist.");
                }

                if (!tableExists(conn, "appointments")) {
                    throw new SQLException("The appointments table does not exist.");
                }

                if (!tableExists(conn, "services")) {
                    throw new SQLException("The services table does not exist.");
                }

                // Verify actual primary keys.
                if (!isPrimaryKey(conn, "users", "user_id")) {
                    throw new SQLException(
                            "users.user_id must be the primary key."
                    );
                }

                if (!isPrimaryKey(conn, "appointments", "id")) {
                    throw new SQLException(
                            "appointments.id must be the primary key."
                    );
                }

                if (!isPrimaryKey(conn, "services", "service_id")) {
                    throw new SQLException(
                            "services.service_id must be the primary key."
                    );
                }

                // Additional appointment booking columns.
                ensureColumn(
                        conn,
                        "appointments",
                        "total_duration_minutes",
                        "INTEGER NOT NULL DEFAULT 0"
                );

                ensureColumn(
                        conn,
                        "appointments",
                        "buffer_minutes",
                        "INTEGER NOT NULL DEFAULT 15"
                );

                ensureColumn(
                        conn,
                        "appointments",
                        "reserved_until",
                        "TEXT"
                );

                // Repair activity_logs foreign key if necessary.
                ensureActivityLogsForeignKey(conn);

                // Repair or create multi-service booking table.
                ensureAppointmentServicesTable(conn);

                // Indexes.
                try (Statement stmt = conn.createStatement()) {

                    stmt.execute(
                            "CREATE INDEX IF NOT EXISTS "
                                    + "idx_appointment_services_appointment "
                                    + "ON appointment_services(appointment_id)"
                    );

                    stmt.execute(
                            "CREATE INDEX IF NOT EXISTS "
                                    + "idx_appointment_services_service "
                                    + "ON appointment_services(service_id)"
                    );

                    stmt.execute(
                            "CREATE INDEX IF NOT EXISTS "
                                    + "idx_appointments_staff_schedule "
                                    + "ON appointments(staff_id, appointment_date)"
                    );
                }

                // Insert initial services without duplicates.
                insertInitialServices(conn);

                // Verify foreign key relationships.
                verifyForeignKeys(conn);

                conn.commit();

                System.out.println(
                        "-> Database initialization completed successfully."
                );
                System.out.println(
                        "-> Activity logs foreign key verified."
                );
                System.out.println(
                        "-> Multi-service booking structure verified."
                );
                System.out.println(
                        "-> Initial services checked and inserted when missing."
                );
                System.out.println(
                        "-> Existing database tables and records preserved."
                );

            } catch (SQLException e) {

                conn.rollback();
                throw e;

            } finally {

                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {

            System.err.println(
                    "Error initializing database: " + e.getMessage()
            );
        }
    }

    // =========================================================
    // ACTIVITY LOGS FOREIGN KEY
    // =========================================================

    private static void ensureActivityLogsForeignKey(
            Connection conn
    ) throws SQLException {

        if (!tableExists(conn, "activity_logs")) {

            String sql = """
                    CREATE TABLE activity_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER NOT NULL,
                        action_type VARCHAR(50) NOT NULL,
                        description TEXT NOT NULL,
                        timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (user_id)
                            REFERENCES users(user_id)
                            ON DELETE CASCADE
                    )
                    """;

            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }

            System.out.println(
                    "-> Created activity_logs with the correct foreign key."
            );

            return;
        }

        boolean correctForeignKey = hasForeignKey(
                conn,
                "activity_logs",
                "user_id",
                "users",
                "user_id"
        );

        if (correctForeignKey) {
            System.out.println(
                    "-> activity_logs foreign key is already correct."
            );
            return;
        }

        System.out.println(
                "-> Repairing activity_logs foreign key..."
        );

        // Check that existing records can reference users.user_id.
        int orphanCount = countOrphanActivityLogs(conn);

        if (orphanCount > 0) {
            throw new SQLException(
                    "Cannot safely repair activity_logs: "
                            + orphanCount
                            + " existing log record(s) reference "
                            + "users that do not exist. "
                            + "No records were deleted."
            );
        }

        // Create corrected temporary table.
        String createSQL = """
                CREATE TABLE activity_logs_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    action_type VARCHAR(50) NOT NULL,
                    description TEXT NOT NULL,
                    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id)
                        REFERENCES users(user_id)
                        ON DELETE CASCADE
                )
                """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createSQL);

            // Preserve all existing activity log records.
            stmt.execute("""
                    INSERT INTO activity_logs_new (
                        id,
                        user_id,
                        action_type,
                        description,
                        timestamp
                    )
                    SELECT
                        id,
                        user_id,
                        action_type,
                        description,
                        timestamp
                    FROM activity_logs
                    """);

            // Replace only the activity_logs table.
            stmt.execute("DROP TABLE activity_logs");

            stmt.execute(
                    "ALTER TABLE activity_logs_new RENAME TO activity_logs"
            );
        }

        System.out.println(
                "-> activity_logs foreign key repaired successfully."
        );
        System.out.println(
                "-> Existing activity log records preserved."
        );
    }

    private static int countOrphanActivityLogs(
            Connection conn
    ) throws SQLException {

        String sql = """
                SELECT COUNT(*)
                FROM activity_logs a
                LEFT JOIN users u
                    ON a.user_id = u.user_id
                WHERE u.user_id IS NULL
                """;

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // =========================================================
    // APPOINTMENT SERVICES TABLE
    // =========================================================

    private static void ensureAppointmentServicesTable(
            Connection conn
    ) throws SQLException {

        if (!tableExists(conn, "appointment_services")) {

            createAppointmentServicesTable(conn);

            System.out.println(
                    "-> Created appointment_services table."
            );

            return;
        }

        boolean appointmentFK = hasForeignKey(
                conn,
                "appointment_services",
                "appointment_id",
                "appointments",
                "id"
        );

        boolean serviceFK = hasForeignKey(
                conn,
                "appointment_services",
                "service_id",
                "services",
                "service_id"
        );

        if (appointmentFK && serviceFK) {
            System.out.println(
                    "-> appointment_services foreign keys are correct."
            );
            return;
        }

        System.out.println(
                "-> Repairing appointment_services foreign keys..."
        );

        String createSQL = """
                CREATE TABLE appointment_services_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    appointment_id INTEGER NOT NULL,
                    service_id INTEGER NOT NULL,
                    service_name_at_booking TEXT NOT NULL,
                    duration_minutes INTEGER NOT NULL,
                    price_at_booking REAL NOT NULL,
                    FOREIGN KEY (appointment_id)
                        REFERENCES appointments(id)
                        ON DELETE CASCADE,
                    FOREIGN KEY (service_id)
                        REFERENCES services(service_id)
                        ON DELETE RESTRICT,
                    UNIQUE (appointment_id, service_id)
                )
                """;

        try (Statement stmt = conn.createStatement()) {

            stmt.execute(createSQL);

            // Preserve existing booking service records.
            stmt.execute("""
                    INSERT INTO appointment_services_new (
                        id,
                        appointment_id,
                        service_id,
                        service_name_at_booking,
                        duration_minutes,
                        price_at_booking
                    )
                    SELECT
                        id,
                        appointment_id,
                        service_id,
                        service_name_at_booking,
                        duration_minutes,
                        price_at_booking
                    FROM appointment_services
                    """);

            stmt.execute("DROP TABLE appointment_services");

            stmt.execute(
                    "ALTER TABLE appointment_services_new "
                            + "RENAME TO appointment_services"
            );
        }

        System.out.println(
                "-> appointment_services foreign keys repaired."
        );
        System.out.println(
                "-> Existing booking service records preserved."
        );
    }

    private static void createAppointmentServicesTable(
            Connection conn
    ) throws SQLException {

        String sql = """
                CREATE TABLE appointment_services (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    appointment_id INTEGER NOT NULL,
                    service_id INTEGER NOT NULL,
                    service_name_at_booking TEXT NOT NULL,
                    duration_minutes INTEGER NOT NULL,
                    price_at_booking REAL NOT NULL,
                    FOREIGN KEY (appointment_id)
                        REFERENCES appointments(id)
                        ON DELETE CASCADE,
                    FOREIGN KEY (service_id)
                        REFERENCES services(service_id)
                        ON DELETE RESTRICT,
                    UNIQUE (appointment_id, service_id)
                )
                """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    // =========================================================
    // INITIAL SERVICES
    // =========================================================

    private static void insertInitialServices(
            Connection conn
    ) throws SQLException {

        String sql = """
                INSERT INTO services (
                    service_name,
                    category,
                    duration_minutes,
                    description,
                    price,
                    availability
                )
                SELECT ?, ?, ?, ?, ?, ?
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM services
                    WHERE LOWER(service_name) = LOWER(?)
                )
                """;

        Object[][] services = {
                {
                        "Basic Grooming", "GROOMING", 60,
                        "Basic pet grooming service.", 500.00, "AVAILABLE"
                },
                {
                        "Bath and Dry", "GROOMING", 45,
                        "Pet bathing and drying service.", 350.00, "AVAILABLE"
                },
                {
                        "Nail Trim", "GROOMING", 15,
                        "Nail trimming for pets.", 150.00, "AVAILABLE"
                },
                {
                        "Ear Cleaning", "GROOMING", 15,
                        "Gentle ear cleaning service.", 150.00, "AVAILABLE"
                },
                {
                        "Dental Cleaning", "GROOMING", 30,
                        "Basic pet dental cleaning.", 300.00, "AVAILABLE"
                },
                {
                        "Vet Checkup", "VETERINARY", 30,
                        "General veterinary consultation.", 600.00, "AVAILABLE"
                },
                {
                        "Vaccination", "VETERINARY", 30,
                        "Routine pet vaccination service.", 450.00, "AVAILABLE"
                },
                {
                        "Deworming", "VETERINARY", 20,
                        "Pet deworming treatment.", 250.00, "AVAILABLE"
                },
                {
                        "Flea Treatment", "VETERINARY", 30,
                        "Flea prevention and treatment.", 400.00, "AVAILABLE"
                },
                {
                        "Microchipping", "VETERINARY", 30,
                        "Pet microchip implantation.", 500.00, "AVAILABLE"
                }
        };

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (Object[] service : services) {

                for (int i = 0; i < 6; i++) {
                    pstmt.setObject(i + 1, service[i]);
                }

                pstmt.setString(7, (String) service[0]);

                int inserted = pstmt.executeUpdate();

                if (inserted > 0) {
                    System.out.println(
                            "-> Added service: " + service[0]
                    );
                }
            }
        }
    }

    // =========================================================
    // FOREIGN KEY VALIDATION
    // =========================================================

    private static boolean hasForeignKey(
            Connection conn,
            String childTable,
            String childColumn,
            String parentTable,
            String parentColumn
    ) throws SQLException {

        String sql = "PRAGMA foreign_key_list(" + childTable + ")";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {

                if (parentTable.equalsIgnoreCase(rs.getString("table"))
                        && childColumn.equalsIgnoreCase(rs.getString("from"))
                        && parentColumn.equalsIgnoreCase(rs.getString("to"))) {

                    return true;
                }
            }
        }

        return false;
    }

    private static void verifyForeignKeys(
            Connection conn
    ) throws SQLException {

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA foreign_key_check")) {

            if (rs.next()) {
                throw new SQLException(
                        "Foreign key validation failed in table: "
                                + rs.getString("table")
                );
            }
        }
    }

    // =========================================================
    // TABLE AND COLUMN HELPERS
    // =========================================================

    private static boolean tableExists(
            Connection conn,
            String tableName
    ) throws SQLException {

        String sql = """
                SELECT name
                FROM sqlite_master
                WHERE type = 'table' AND name = ?
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, tableName);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static boolean columnExists(
            Connection conn,
            String tableName,
            String columnName
    ) throws SQLException {

        String sql = "PRAGMA table_info(" + tableName + ")";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {

                if (columnName.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean isPrimaryKey(
            Connection conn,
            String tableName,
            String columnName
    ) throws SQLException {

        String sql = "PRAGMA table_info(" + tableName + ")";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {

                if (columnName.equalsIgnoreCase(rs.getString("name"))) {
                    return rs.getInt("pk") > 0;
                }
            }
        }

        return false;
    }

    private static void ensureColumn(
            Connection conn,
            String tableName,
            String columnName,
            String columnDefinition
    ) throws SQLException {

        if (!columnExists(conn, tableName, columnName)) {

            String sql = "ALTER TABLE " + tableName
                    + " ADD COLUMN " + columnName
                    + " " + columnDefinition;

            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }

            System.out.println(
                    "-> Added column: " + tableName + "." + columnName
            );
        }
    }
}
