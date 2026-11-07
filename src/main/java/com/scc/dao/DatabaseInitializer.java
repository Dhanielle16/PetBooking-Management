package com.scc.dao;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;

public class DatabaseInitializer {
    public static void initialize(){
        // Ensure users table matches the full architecture schema
        String createUserTableSQL = "CREATE TABLE IF NOT EXISTS users ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "full_name TEXT NOT NULL, "
                + "email TEXT NOT NULL, "
                + "password TEXT NOT NULL, "
                + "phone_number TEXT NOT NULL, "
                + "role TEXT NOT NULL, "
                + "status TEXT NOT NULL"
                + ");";

        // Ensure activity_logs table exists with foreign key constraint
        String createLogTableSQL = "CREATE TABLE IF NOT EXISTS activity_logs ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "user_id INTEGER NOT NULL, "
                + "action_type VARCHAR(50) NOT NULL, "
                + "description TEXT NOT NULL, "
                + "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP, "
                + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE"
                + ");";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createUserTableSQL);
            stmt.execute(createLogTableSQL);
        } catch (SQLException e) {
            System.err.println("Error initializing database tables: " + e.getMessage());
        }

        new UserDao();
        System.out.println("-> Database tables verified.");
    }
}