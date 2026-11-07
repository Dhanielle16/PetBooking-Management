package com.scc.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class PetDao {
    private final ActivityLogDao logDao = new ActivityLogDao();

    public boolean registerPet(int ownerId, String petName, String species, String breed, int age) {
        String sql = "INSERT INTO pets (owner_id, pet_name, species, breed, age) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, ownerId);
            pstmt.setString(2, petName);
            pstmt.setString(3, species);
            pstmt.setString(4, breed);
            pstmt.setInt(5, age);

            int rowsInserted = pstmt.executeUpdate();
            if (rowsInserted > 0) {
                // --- DOMAIN-SPECIFIC AUDIT LOG ---
                logDao.logAction(
                        ownerId,
                        "PET_REGISTERED",
                        "User ID " + ownerId + " registered pet '" + petName + "' (" + species + " - " + breed + ")."
                );
                System.out.println("-> Pet registered successfully and audit log recorded.");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error registering pet: " + e.getMessage());
        }
        return false;
    }
}