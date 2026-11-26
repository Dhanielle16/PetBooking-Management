
package com.scc.dao;

import com.scc.model.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.List;
import java.util.stream.Collectors;

public class AppointmentDao {

    public boolean bookCombinedAppointment(
            int ownerId,
            int petId,
            int veterinarianId,
            String appointmentDate,
            String startTime,
            String endTime,
            String reservedUntil,
            int totalDurationMinutes,
            int bufferMinutes,
            double totalAmount,
            List<Service> selectedServices
    ) {

        if (selectedServices == null || selectedServices.isEmpty()) {
            System.out.println("No services were selected.");
            return false;
        }

        // Store all selected service names in the required service_type column.
        String serviceType = selectedServices.stream()
                .map(Service::serviceName)
                .collect(Collectors.joining(", "));

        // The first selected service is also stored in the legacy service_id column.
        int primaryServiceId = selectedServices.get(0).serviceId();

        String appointmentSQL = """
                INSERT INTO appointments (
                    user_id,
                    pet_id,
                    service_type,
                    appointment_date,
                    status,
                    staff_id,
                    start_time,
                    end_time,
                    total_amount,
                    service_id,
                    total_duration_minutes,
                    buffer_minutes,
                    reserved_until
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        String updateAppointmentIdSQL = """
                UPDATE appointments
                SET appointment_id = ?
                WHERE id = ?
                """;

        String appointmentServiceSQL = """
                INSERT INTO appointment_services (
                    appointment_id,
                    service_id,
                    service_name_at_booking,
                    duration_minutes,
                    price_at_booking
                )
                VALUES (?, ?, ?, ?, ?)
                """;

        String auditSQL = """
            INSERT INTO activity_logs (
                user_id,
                action_type,
                description
            )
            VALUES (?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.getConnection()) {

            conn.setAutoCommit(false);

            try {

                int appointmentId;

                // ==========================================
                // 1. INSERT MAIN APPOINTMENT
                // ==========================================

                try (PreparedStatement pstmt = conn.prepareStatement(
                        appointmentSQL,
                        Statement.RETURN_GENERATED_KEYS)) {

                    pstmt.setInt(1, ownerId);
                    pstmt.setInt(2, petId);
                    pstmt.setString(3, serviceType);
                    pstmt.setString(4, appointmentDate);
                    pstmt.setString(5, "PENDING");
                    pstmt.setInt(6, veterinarianId);
                    pstmt.setString(7, startTime);
                    pstmt.setString(8, endTime);
                    pstmt.setDouble(9, totalAmount);
                    pstmt.setInt(10, primaryServiceId);
                    pstmt.setInt(11, totalDurationMinutes);
                    pstmt.setInt(12, bufferMinutes);
                    pstmt.setString(13, reservedUntil);

                    pstmt.executeUpdate();

                    try (ResultSet keys = pstmt.getGeneratedKeys()) {

                        if (!keys.next()) {
                            throw new SQLException(
                                    "Could not retrieve the new appointment ID."
                            );
                        }

                        appointmentId = keys.getInt(1);
                    }
                }

                // ==========================================
                // 2. SET APPOINTMENT ID
                // ==========================================

                try (PreparedStatement pstmt =
                             conn.prepareStatement(updateAppointmentIdSQL)) {

                    pstmt.setInt(1, appointmentId);
                    pstmt.setInt(2, appointmentId);

                    pstmt.executeUpdate();
                }

                // ==========================================
                // 3. INSERT SELECTED APPOINTMENT SERVICES
                // ==========================================

                try (PreparedStatement pstmt =
                             conn.prepareStatement(appointmentServiceSQL)) {

                    for (Service service : selectedServices) {

                        pstmt.setInt(1, appointmentId);
                        pstmt.setInt(2, service.serviceId());
                        pstmt.setString(3, service.serviceName());
                        pstmt.setInt(4, service.durationMinutes());
                        pstmt.setDouble(5, service.price());

                        pstmt.addBatch();
                    }

                    pstmt.executeBatch();
                }

                // ==========================================
                // 4. INSERT AUDIT LOG
                // ==========================================

                String description =
                        "Pet owner ID " + ownerId
                                + " booked appointment ID " + appointmentId
                                + " with veterinarian ID " + veterinarianId
                                + " for " + selectedServices.size()
                                + " service(s): " + serviceType;

                try (PreparedStatement pstmt =
                             conn.prepareStatement(auditSQL)) {

                    pstmt.setInt(1, ownerId);
                    pstmt.setString(2, "APPOINTMENT_BOOKED");
                    pstmt.setString(3, description);

                    pstmt.executeUpdate();
                }

                // ==========================================
                // 5. COMMIT TRANSACTION
                // ==========================================

                conn.commit();

                System.out.println("\nAppointment successfully booked!");
                System.out.println("Appointment ID: " + appointmentId);
                System.out.println("Selected Services: " + serviceType);

                return true;

            } catch (SQLException e) {

                conn.rollback();

                System.err.println(
                        "Booking failed. No appointment was saved: "
                                + e.getMessage()
                );

                return false;

            } finally {

                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {

            System.err.println(
                    "Database error: " + e.getMessage()
            );

            return false;
        }
    }
}
