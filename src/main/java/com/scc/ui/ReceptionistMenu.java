
package com.scc.ui;

import com.scc.dao.BillingDao;
import com.scc.dao.DatabaseConnection;
import com.scc.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.Scanner;

public class ReceptionistMenu {

    private final User receptionist;
    private final Scanner sc;
    private final BillingDao billingDao = new BillingDao();

    public ReceptionistMenu(User receptionist, Scanner sc) {
        this.receptionist = receptionist;
        this.sc = sc;
    }

    // ==========================================
    // RECEPTIONIST DASHBOARD
    // ==========================================

    public void display() {

        boolean active = true;

        while (active) {

            System.out.println("\n==========================================");
            System.out.println("         RECEPTIONIST DASHBOARD");
            System.out.println("==========================================");
            System.out.println("1. View All Appointments");
            System.out.println("2. Confirm / Update Appointment Status");
            System.out.println("3. Process Client Billing & Payment");
            System.out.println("4. View Client Directory");
            System.out.println("5. Logout");
            System.out.print("Choose an option (1-5): ");

            String choice = sc.nextLine().trim();

            switch (choice) {

                case "1" -> viewAllAppointments();

                case "2" -> updateAppointmentStatus();

                case "3" -> processBillingFlow();

                case "4" -> viewClientDirectory();

                case "5" -> {
                    System.out.println(
                            "-> Logging out from Receptionist Dashboard...");
                    active = false;
                }

                default ->
                        System.out.println("-> [ERROR] Invalid option.");
            }
        }
    }

    // ==========================================
    // 1. VIEW ALL APPOINTMENTS
    // ==========================================

    private void viewAllAppointments() {

        String sql = """
                SELECT
                    a.id AS appointment_id,
                    owner.full_name AS owner_name,
                    p.pet_name,
                    a.service_type,
                    vet.full_name AS veterinarian_name,
                    a.appointment_date,
                    a.start_time,
                    a.end_time,
                    a.status,
                    a.total_amount
                FROM appointments a
                INNER JOIN users owner
                    ON a.user_id = owner.user_id
                INNER JOIN pets p
                    ON a.pet_id = p.pet_id
                LEFT JOIN users vet
                    ON a.staff_id = vet.user_id
                ORDER BY a.appointment_date DESC, a.start_time DESC
                """;

        System.out.println("\n==========================================");
        System.out.println("             ALL APPOINTMENTS");
        System.out.println("==========================================");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            boolean found = false;

            while (rs.next()) {

                found = true;

                System.out.println("\n------------------------------------------");
                System.out.println("Appointment ID: "
                        + rs.getInt("appointment_id"));

                System.out.println("Pet Owner: "
                        + rs.getString("owner_name"));

                System.out.println("Pet Name: "
                        + rs.getString("pet_name"));

                System.out.println("Service(s): "
                        + rs.getString("service_type"));

                String veterinarian =
                        rs.getString("veterinarian_name");

                System.out.println("Veterinarian: "
                        + (veterinarian == null
                        ? "Not Assigned" : veterinarian));

                System.out.println("Date: "
                        + rs.getString("appointment_date"));

                System.out.println("Time: "
                        + rs.getString("start_time")
                        + " - " + rs.getString("end_time"));

                System.out.println("Status: "
                        + rs.getString("status"));

                System.out.printf("Total Amount: ₱%.2f%n",
                        rs.getDouble("total_amount"));
            }

            if (!found) {
                System.out.println("-> No appointments found.");
            }

            System.out.println("------------------------------------------");

        } catch (SQLException e) {

            System.out.println(
                    "-> [ERROR] Unable to retrieve appointments: "
                            + e.getMessage());
        }
    }

    // ==========================================
    // 2. UPDATE APPOINTMENT STATUS
    // ==========================================

    private void updateAppointmentStatus() {

        System.out.println("\n==========================================");
        System.out.println("       UPDATE APPOINTMENT STATUS");
        System.out.println("==========================================");

        viewAllAppointments();

        int appointmentId = readIntInput(
                "\nEnter Appointment ID to update (0 to cancel): ");

        if (appointmentId == 0) {
            System.out.println("-> Operation cancelled.");
            return;
        }

        String currentStatus;

        String checkSQL = """
                SELECT status
                FROM appointments
                WHERE id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(checkSQL)) {

            pstmt.setInt(1, appointmentId);

            try (ResultSet rs = pstmt.executeQuery()) {

                if (!rs.next()) {
                    System.out.println(
                            "-> [ERROR] Appointment ID not found.");
                    return;
                }

                currentStatus = rs.getString("status");
            }

        } catch (SQLException e) {

            System.out.println(
                    "-> [ERROR] Unable to check appointment: "
                            + e.getMessage());
            return;
        }

        System.out.println("\nCurrent Status: " + currentStatus);

        System.out.println("\nAvailable Appointment Statuses:");
        System.out.println("1. PENDING");
        System.out.println("2. CONFIRMED");
        System.out.println("3. CANCELLED");
        System.out.println("4. COMPLETED");
        System.out.println("5. Cancel Operation");

        System.out.print("\nSelect New Status (1-5): ");

        String choice = sc.nextLine().trim();

        String newStatus;

        switch (choice) {

            case "1" -> newStatus = "PENDING";

            case "2" -> newStatus = "CONFIRMED";

            case "3" -> newStatus = "CANCELLED";

            case "4" -> newStatus = "COMPLETED";

            case "5" -> {
                System.out.println("-> Operation cancelled.");
                return;
            }

            default -> {
                System.out.println("-> [ERROR] Invalid status option.");
                return;
            }
        }

        if (newStatus.equalsIgnoreCase(currentStatus)) {
            System.out.println(
                    "-> Appointment already has this status.");
            return;
        }

        System.out.println("\nAppointment ID: " + appointmentId);
        System.out.println("Current Status: " + currentStatus);
        System.out.println("New Status: " + newStatus);

        System.out.print("\nConfirm status update? (Y/N): ");

        String confirmation = sc.nextLine().trim();

        if (!confirmation.equalsIgnoreCase("Y")) {
            System.out.println("-> Status update cancelled.");
            return;
        }

        String updateSQL = """
                UPDATE appointments
                SET status = ?
                WHERE id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {

            pstmt.setString(1, newStatus);
            pstmt.setInt(2, appointmentId);

            int updated = pstmt.executeUpdate();

            if (updated > 0) {

                System.out.println(
                        "-> [SUCCESS] Appointment status updated to "
                                + newStatus + ".");

            } else {

                System.out.println(
                        "-> [ERROR] Appointment status was not updated.");
            }

        } catch (SQLException e) {

            System.out.println(
                    "-> [ERROR] Unable to update appointment status: "
                            + e.getMessage());
        }
    }

    // ==========================================
    // 3. PROCESS CLIENT BILLING AND PAYMENT
    // ==========================================

    private void processBillingFlow() {

        System.out.println("\n==========================================");
        System.out.println("         PROCESS CLIENT BILLING");
        System.out.println("==========================================");

        int clientId = readIntInput("Enter Client User ID: ");

        double amount = readPositiveAmount();

        String method = readPaymentMethod();

        System.out.println("\n==========================================");
        System.out.println("              BILLING SUMMARY");
        System.out.println("==========================================");

        System.out.println("Receptionist: " + receptionist.fullName());
        System.out.println("Client ID: " + clientId);

        System.out.printf("Total Amount: ₱%.2f%n", amount);

        System.out.println("Payment Method: " + method);

        System.out.print("\nConfirm payment? (Y/N): ");

        String confirmation = sc.nextLine().trim();

        if (confirmation.equalsIgnoreCase("Y")) {

            billingDao.processBill(
                    receptionist.id(),
                    clientId,
                    amount,
                    method
            );

        } else {

            System.out.println("-> Billing transaction cancelled.");
        }
    }

    // ==========================================
    // 4. VIEW CLIENT DIRECTORY
    // ==========================================

    private void viewClientDirectory() {

        String sql = """
                SELECT
                    user_id,
                    full_name,
                    email,
                    phone_number,
                    status
                FROM users
                WHERE UPPER(TRIM(user_role)) = 'PET OWNER'
                ORDER BY full_name
                """;

        System.out.println("\n==========================================");
        System.out.println("             CLIENT DIRECTORY");
        System.out.println("==========================================");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            boolean found = false;

            System.out.printf(
                    "%-8s %-25s %-30s %-18s %-12s%n",
                    "USER ID",
                    "FULL NAME",
                    "EMAIL",
                    "PHONE",
                    "STATUS"
            );

            System.out.println(
                    "-----------------------------------------------------------------------------------------------");

            while (rs.next()) {

                found = true;

                System.out.printf(
                        "%-8d %-25s %-30s %-18s %-12s%n",
                        rs.getInt("user_id"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("phone_number"),
                        rs.getString("status")
                );
            }

            if (!found) {
                System.out.println("-> No registered Pet Owners found.");
            }

            System.out.println(
                    "-----------------------------------------------------------------------------------------------");

        } catch (SQLException e) {

            System.out.println(
                    "-> [ERROR] Unable to retrieve client directory: "
                            + e.getMessage());
        }
    }

    // ==========================================
    // INPUT VALIDATION
    // ==========================================

    private int readIntInput(String prompt) {

        while (true) {

            System.out.print(prompt);

            try {

                int value = Integer.parseInt(sc.nextLine().trim());

                if (value > 0 || value == 0) {
                    return value;
                }

                System.out.println(
                        "-> [ERROR] Please enter a valid positive number.");

            } catch (NumberFormatException e) {

                System.out.println(
                        "-> [ERROR] Please enter a valid whole number.");
            }
        }
    }

    private double readPositiveAmount() {

        while (true) {

            System.out.print("Enter Total Amount (₱): ");

            try {

                double amount =
                        Double.parseDouble(sc.nextLine().trim());

                if (Double.isFinite(amount) && amount > 0) {
                    return amount;
                }

                System.out.println(
                        "-> [ERROR] Amount must be greater than zero.");

            } catch (NumberFormatException e) {

                System.out.println(
                        "-> [ERROR] Please enter a valid decimal amount.");
            }
        }
    }

    private String readPaymentMethod() {

        while (true) {

            System.out.println("\nPayment Methods:");
            System.out.println("1. Cash");
            System.out.println("2. GCash");
            System.out.println("3. Credit Card");

            System.out.print("Choose Payment Method: ");

            String choice = sc.nextLine().trim();

            switch (choice) {

                case "1" -> {
                    return "Cash";
                }

                case "2" -> {
                    return "GCash";
                }

                case "3" -> {
                    return "Credit Card";
                }

                default -> System.out.println(
                        "-> [ERROR] Invalid payment method. Please try again.");
            }
        }
    }
}
