
package com.scc.ui;

import com.scc.dao.DatabaseConnection;
import com.scc.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

import java.util.Scanner;

public class VeterinarianMenu {

    private final User veterinarian;
    private final Scanner sc;

    public VeterinarianMenu(User veterinarian, Scanner sc) {
        this.veterinarian = veterinarian;
        this.sc = sc;
    }

    public void display() {

        boolean active = true;

        while (active) {

            System.out.println("\n==========================================");
            System.out.println("           VETERINARIAN DASHBOARD");
            System.out.println("==========================================");
            System.out.println("Welcome, " + veterinarian.fullName());
            System.out.println("------------------------------------------");
            System.out.println("1. View Assigned Appointments");
            System.out.println("2. Manage Availability / Schedules");
            System.out.println("3. View Pet Medical History");
            System.out.println("4. Add Consultation / Treatment Notes");
            System.out.println("5. Logout");
            System.out.println("==========================================");
            System.out.print("Choose an option (1-5): ");

            String choice = sc.nextLine().trim();

            switch (choice) {

                case "1" -> viewAssignedAppointments();

                case "2" -> manageSchedules();

                case "3" -> viewPetMedicalHistory();

                case "4" -> addTreatmentNotes();

                case "5" -> {
                    System.out.println(
                            "-> Logging out from Veterinarian Dashboard...");
                    active = false;
                }

                default -> System.out.println(
                        "-> [ERROR] Invalid option. Please try again.");
            }
        }
    }

    // ==========================================
    // 1. VIEW ASSIGNED APPOINTMENTS
    // ==========================================

    private void viewAssignedAppointments() {

        String sql = """
                SELECT
                    a.appointment_id,
                    a.appointment_date,
                    a.start_time,
                    a.end_time,
                    a.status,
                    p.pet_name,
                    GROUP_CONCAT(
                        aps.service_name_at_booking, ', '
                    ) AS selected_services
                FROM appointments a
                JOIN pets p
                    ON a.pet_id = p.pet_id
                LEFT JOIN appointment_services aps
                    ON a.appointment_id = aps.appointment_id
                WHERE a.staff_id = ?
                GROUP BY a.appointment_id
                ORDER BY a.appointment_date, a.start_time
                """;

        System.out.println("\n==========================================");
        System.out.println("          ASSIGNED APPOINTMENTS");
        System.out.println("==========================================");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, veterinarian.id());

            try (ResultSet rs = pstmt.executeQuery()) {

                boolean found = false;

                while (rs.next()) {

                    found = true;

                    System.out.println("------------------------------------------");
                    System.out.println(
                            "Appointment ID : " + rs.getInt("appointment_id"));
                    System.out.println(
                            "Pet             : " + rs.getString("pet_name"));
                    System.out.println(
                            "Date            : " + rs.getString("appointment_date"));
                    System.out.println(
                            "Start Time      : " + rs.getString("start_time"));
                    System.out.println(
                            "End Time        : " + rs.getString("end_time"));
                    System.out.println(
                            "Services        : " + rs.getString("selected_services"));
                    System.out.println(
                            "Status          : " + rs.getString("status"));
                }

                if (!found) {
                    System.out.println(
                            "-> No appointments are currently assigned to you.");
                }
            }

        } catch (SQLException e) {
            System.out.println(
                    "Error retrieving appointments: " + e.getMessage());
        }
    }

    // ==========================================
    // 2. MANAGE AVAILABILITY / SCHEDULES
    // ==========================================

    private void manageSchedules() {

        boolean active = true;

        while (active) {

            System.out.println("\n==========================================");
            System.out.println("          VETERINARIAN SCHEDULES");
            System.out.println("==========================================");
            System.out.println("1. View My Schedules");
            System.out.println("2. Add Available Schedule");
            System.out.println("3. Back");
            System.out.print("Choose an option: ");

            String choice = sc.nextLine().trim();

            switch (choice) {

                case "1" -> viewSchedules();

                case "2" -> addSchedule();

                case "3" -> active = false;

                default -> System.out.println("-> Invalid option.");
            }
        }
    }

    private void viewSchedules() {

        String sql = """
                SELECT
                    schedule_id,
                    schedule_date,
                    start_time,
                    end_time,
                    availability_status
                FROM staff_schedules
                WHERE staff_id = ?
                ORDER BY schedule_date, start_time
                """;

        System.out.println("\n==========================================");
        System.out.println("             MY SCHEDULES");
        System.out.println("==========================================");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, veterinarian.id());

            try (ResultSet rs = pstmt.executeQuery()) {

                boolean found = false;

                while (rs.next()) {

                    found = true;

                    System.out.println("------------------------------------------");
                    System.out.println(
                            "Schedule ID : " + rs.getInt("schedule_id"));
                    System.out.println(
                            "Date        : " + rs.getString("schedule_date"));
                    System.out.println(
                            "Start Time  : " + rs.getString("start_time"));
                    System.out.println(
                            "End Time    : " + rs.getString("end_time"));
                    System.out.println(
                            "Availability: " + rs.getString("availability_status"));
                }

                if (!found) {
                    System.out.println("-> You have no registered schedules.");
                }
            }

        } catch (SQLException e) {
            System.out.println(
                    "Error retrieving schedules: " + e.getMessage());
        }
    }

    private void addSchedule() {

        System.out.println("\n==========================================");
        System.out.println("          ADD AVAILABLE SCHEDULE");
        System.out.println("==========================================");

        try {

            System.out.print("Enter schedule date (YYYY-MM-DD): ");
            String dateInput = sc.nextLine().trim();

            LocalDate scheduleDate = LocalDate.parse(dateInput);

            if (scheduleDate.isBefore(LocalDate.now())) {
                System.out.println(
                        "-> Schedule date cannot be in the past.");
                return;
            }

            System.out.print("Enter start time (HH:mm): ");
            String startInput = sc.nextLine().trim();

            System.out.print("Enter end time (HH:mm): ");
            String endInput = sc.nextLine().trim();

            LocalTime startTime = LocalTime.parse(startInput);
            LocalTime endTime = LocalTime.parse(endInput);

            if (!endTime.isAfter(startTime)) {
                System.out.println(
                        "-> End time must be later than start time.");
                return;
            }

            if (scheduleDate.equals(LocalDate.now())
                    && !startTime.isAfter(LocalTime.now())) {
                System.out.println(
                        "-> Start time must be later than the current time.");
                return;
            }

            String sql = """
                    INSERT INTO staff_schedules (
                        staff_id,
                        schedule_date,
                        start_time,
                        end_time,
                        availability_status
                    )
                    VALUES (?, ?, ?, ?, 'AVAILABLE')
                    """;

            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setInt(1, veterinarian.id());
                pstmt.setString(2, scheduleDate.toString());
                pstmt.setString(3, startTime.toString());
                pstmt.setString(4, endTime.toString());

                int result = pstmt.executeUpdate();

                if (result > 0) {
                    System.out.println(
                            "\n-> Your available schedule was added successfully.");
                    System.out.println(
                            "-> You can now receive bookings for this schedule.");
                }
            }

        } catch (DateTimeParseException e) {

            System.out.println(
                    "-> Invalid date or time format. Please use YYYY-MM-DD and HH:mm.");

        } catch (SQLException e) {

            System.out.println(
                    "Error adding schedule: " + e.getMessage());
        }
    }

    // ==========================================
    // 3. VIEW PET MEDICAL HISTORY
    // ==========================================

    private void viewPetMedicalHistory() {

        System.out.println("\n==========================================");
        System.out.println("          PET MEDICAL HISTORY");
        System.out.println("==========================================");

        System.out.print("Enter Pet ID: ");

        try {

            int petId = Integer.parseInt(sc.nextLine().trim());

            String sql = """
                    SELECT
                        p.pet_name,
                        sr.record_type,
                        sr.diagnosis,
                        sr.treatment_notes,
                        sr.service_notes,
                        sr.record_date
                    FROM appointments a
                    JOIN pets p
                        ON a.pet_id = p.pet_id
                    JOIN service_records sr
                        ON a.appointment_id = sr.appointment_id
                    WHERE p.pet_id = ?
                    AND a.staff_id = ?
                    ORDER BY sr.record_date DESC
                    """;

            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setInt(1, petId);
                pstmt.setInt(2, veterinarian.id());

                try (ResultSet rs = pstmt.executeQuery()) {

                    boolean found = false;

                    while (rs.next()) {

                        found = true;

                        System.out.println("------------------------------------------");
                        System.out.println(
                                "Pet Name        : " + rs.getString("pet_name"));
                        System.out.println(
                                "Record Type     : " + rs.getString("record_type"));
                        System.out.println(
                                "Diagnosis       : " + rs.getString("diagnosis"));
                        System.out.println(
                                "Treatment Notes : " + rs.getString("treatment_notes"));
                        System.out.println(
                                "Service Notes   : " + rs.getString("service_notes"));
                        System.out.println(
                                "Record Date     : " + rs.getString("record_date"));
                    }

                    if (!found) {
                        System.out.println(
                                "-> No medical records found for this pet "
                                        + "under your assigned appointments.");
                    }
                }
            }

        } catch (NumberFormatException e) {

            System.out.println("-> Invalid Pet ID.");

        } catch (SQLException e) {

            System.out.println(
                    "Error retrieving medical history: " + e.getMessage());
        }
    }

    // ==========================================
    // 4. ADD CONSULTATION / TREATMENT NOTES
    // ==========================================

    private void addTreatmentNotes() {

        System.out.println("\n==========================================");
        System.out.println("       ADD CONSULTATION / TREATMENT");
        System.out.println("==========================================");

        try {

            System.out.print("Enter Appointment ID: ");
            int appointmentId = Integer.parseInt(sc.nextLine().trim());

            // Verify that this appointment belongs to the veterinarian.
            String checkSQL = """
                    SELECT appointment_id
                    FROM appointments
                    WHERE appointment_id = ?
                    AND staff_id = ?
                    """;

            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement checkStmt =
                         conn.prepareStatement(checkSQL)) {

                checkStmt.setInt(1, appointmentId);
                checkStmt.setInt(2, veterinarian.id());

                try (ResultSet rs = checkStmt.executeQuery()) {

                    if (!rs.next()) {
                        System.out.println(
                                "-> Appointment not found or not assigned to you.");
                        return;
                    }
                }

                System.out.print("Enter Record Type: ");
                String recordType = sc.nextLine().trim();

                System.out.print("Enter Diagnosis: ");
                String diagnosis = sc.nextLine().trim();

                System.out.print("Enter Treatment Notes: ");
                String treatmentNotes = sc.nextLine().trim();

                System.out.print("Enter Service Notes: ");
                String serviceNotes = sc.nextLine().trim();

                String insertSQL = """
                        INSERT INTO service_records (
                            appointment_id,
                            staff_id,
                            record_type,
                            diagnosis,
                            treatment_notes,
                            service_notes,
                            record_date
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """;

                try (PreparedStatement insertStmt =
                             conn.prepareStatement(insertSQL)) {

                    insertStmt.setInt(1, appointmentId);
                    insertStmt.setInt(2, veterinarian.id());
                    insertStmt.setString(3, recordType);
                    insertStmt.setString(4, diagnosis);
                    insertStmt.setString(5, treatmentNotes);
                    insertStmt.setString(6, serviceNotes);
                    insertStmt.setString(7, LocalDate.now().toString());

                    int result = insertStmt.executeUpdate();

                    if (result > 0) {
                        System.out.println(
                                "\n-> Consultation / treatment record saved successfully.");
                    }
                }
            }

        } catch (NumberFormatException e) {

            System.out.println("-> Invalid Appointment ID.");

        } catch (SQLException e) {

            System.out.println(
                    "Error saving treatment record: " + e.getMessage());
        }
    }
}
