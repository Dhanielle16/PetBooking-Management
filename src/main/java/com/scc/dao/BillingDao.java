package com.scc.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class BillingDao {
    private final ActivityLogDao logDao = new ActivityLogDao();

    public boolean processBill(int staffUserId, int clientId, double amount, String paymentMethod) {
        String sql = "INSERT INTO invoices (client_id, staff_id, amount, payment_method, status) VALUES (?, ?, ?, ?, 'PAID')";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, clientId);
            pstmt.setInt(2, staffUserId);
            pstmt.setDouble(3, amount);
            pstmt.setString(4, paymentMethod);

            int rowsInserted = pstmt.executeUpdate();
            if (rowsInserted > 0) {
                // --- DOMAIN-SPECIFIC AUDIT LOG ---
                logDao.logAction(
                        staffUserId,
                        "BILL_PROCESSED",
                        "Staff ID " + staffUserId + " processed payment of ₱" + amount + " via " + paymentMethod + " for Client ID " + clientId + "."
                );
                System.out.println("-> Bill processed successfully and audit log recorded.");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error processing bill: " + e.getMessage());
        }
        return false;
    }
}