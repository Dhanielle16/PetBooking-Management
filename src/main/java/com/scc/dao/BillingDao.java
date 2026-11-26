
package com.scc.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class BillingDao {

    public boolean processBill(
            int staffUserId,
            int clientId,
            double amount,
            String paymentMethod
    ) {

        if (staffUserId <= 0 || clientId <= 0) {
            System.out.println("-> [ERROR] Invalid staff or client ID.");
            return false;
        }

        if (!Double.isFinite(amount) || amount <= 0) {
            System.out.println("-> [ERROR] Invalid payment amount.");
            return false;
        }

        if (paymentMethod == null || paymentMethod.isBlank()) {
            System.out.println("-> [ERROR] Payment method is required.");
            return false;
        }

        String sql = """
                INSERT INTO invoices
                (client_id, staff_id, amount, payment_method, status)
                VALUES (?, ?, ?, ?, 'PAID')
                """;

        String logSql = """
                INSERT INTO audit_logs
                (user_id, action_type, description)
                VALUES (?, ?, ?)
                """;

        try (Connection conn = DatabaseConnection.getConnection()) {

            conn.setAutoCommit(false);

            try {

                // SAVE INVOICE
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

                    pstmt.setInt(1, clientId);
                    pstmt.setInt(2, staffUserId);
                    pstmt.setDouble(3, amount);
                    pstmt.setString(4, paymentMethod);

                    int rowsInserted = pstmt.executeUpdate();

                    if (rowsInserted == 0) {
                        conn.rollback();
                        return false;
                    }
                }

                // SAVE AUDIT LOG
                String description =
                        "Staff ID " + staffUserId
                                + " processed payment of ₱"
                                + String.format("%.2f", amount)
                                + " via " + paymentMethod
                                + " for Client ID " + clientId + ".";

                try (PreparedStatement logStmt =
                             conn.prepareStatement(logSql)) {

                    logStmt.setInt(1, staffUserId);
                    logStmt.setString(2, "BILL_PROCESSED");
                    logStmt.setString(3, description);

                    logStmt.executeUpdate();
                }

                conn.commit();

                System.out.println(
                        "-> Bill processed successfully and audit log recorded.");

                return true;

            } catch (SQLException e) {

                try {
                    conn.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }

                throw e;

            } finally {

                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    System.err.println(
                            "Error restoring database connection: "
                                    + e.getMessage());
                }
            }

        } catch (SQLException e) {

            System.err.println(
                    "Error processing bill: " + e.getMessage());

            return false;
        }
    }
}
