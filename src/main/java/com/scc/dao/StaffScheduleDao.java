
package com.scc.dao;

import com.scc.model.StaffMember;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class StaffScheduleDao {

    public List<StaffMember> getAvailableVeterinarians(
            String appointmentDate,
            String startTime,
            String reservedUntil
    ) {

        List<StaffMember> veterinarians = new ArrayList<>();

        String sql = """
                SELECT DISTINCT
                    u.user_id,
                    u.full_name
                FROM users u

                INNER JOIN staff_schedules ss
                    ON u.user_id = ss.staff_id

                WHERE UPPER(TRIM(u.user_role)) = 'VETERINARIAN'

                  AND UPPER(TRIM(COALESCE(u.status, '')))
                      IN ('APPROVED', 'ACTIVE')

                  AND ss.schedule_date = ?

                  AND UPPER(TRIM(ss.availability_status)) = 'AVAILABLE'

                  AND time(ss.start_time) <= time(?)

                  AND time(ss.end_time) >= time(?)

                  AND NOT EXISTS (
                      SELECT 1
                      FROM appointments a

                      WHERE a.staff_id = u.user_id

                        AND a.appointment_date = ?

                        AND UPPER(TRIM(COALESCE(a.status, '')))
                            NOT IN ('CANCELLED', 'REJECTED')

                        AND time(a.start_time) < time(?)

                        AND time(
                            COALESCE(a.reserved_until, a.end_time)
                        ) > time(?)
                  )

                ORDER BY u.full_name
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, appointmentDate);
            pstmt.setString(2, startTime);
            pstmt.setString(3, reservedUntil);

            pstmt.setString(4, appointmentDate);
            pstmt.setString(5, reservedUntil);
            pstmt.setString(6, startTime);

            try (ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {

                    StaffMember veterinarian = new StaffMember(
                            rs.getInt("user_id"),
                            rs.getString("full_name")
                    );

                    veterinarians.add(veterinarian);
                }
            }

        } catch (SQLException e) {

            System.err.println(
                    "Error checking veterinarian availability: "
                            + e.getMessage()
            );
        }

        return veterinarians;
    }

    public boolean isValidTimeRange(
            String startTime,
            String endTime
    ) {

        try {

            LocalTime start = LocalTime.parse(startTime);
            LocalTime end = LocalTime.parse(endTime);

            return start.isBefore(end);

        } catch (Exception e) {

            return false;
        }
    }
}
