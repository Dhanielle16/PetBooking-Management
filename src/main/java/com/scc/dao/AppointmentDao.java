package com.scc.dao;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AppointmentDao {
    private final ActivityLogDao logDao = new ActivityLogDao();

    public boolean bookAppointment(int userId, int petId, String serviceType, String appointmentDate) {
        String sql = "INSERT INTO appointments (user_id, pet_id, service_type, appointment_date, status) VALUES (?, ?, ?, ?, 'BOOKED')";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setInt(2, petId);
            pstmt.setString(3, serviceType);
            pstmt.setString(4, appointmentDate);

            int rowsInserted = pstmt.executeUpdate();
            if (rowsInserted > 0) {
                // --- DOMAIN-SPECIFIC AUDIT LOG ---
                logDao.logAction(
                        userId,
                        "APPOINTMENT_BOOKED",
                        "User ID " + userId + " booked " + serviceType + " for Pet ID " + petId + " on " + appointmentDate + "."
                );
                System.out.println("-> Appointment booked successfully and audit log recorded.");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error booking appointment: " + e.getMessage());
        }
        return false;
    }
}