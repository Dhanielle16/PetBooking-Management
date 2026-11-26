
package com.scc.ui;

import com.scc.dao.ServiceDao;
import com.scc.model.Service;

import java.io.FileWriter;
import java.io.IOException;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.DatabaseConnection;
import com.scc.dao.UserDao;
import com.scc.model.ActivityLog;
import com.scc.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

import java.util.List;
import java.util.Scanner;

public class AdminMenu {

    private final UserDao userDao;
    private final ActivityLogDao activityLogDao = new ActivityLogDao();
    private final Scanner sc;
    private final ServiceDao serviceDao = new ServiceDao();

    public AdminMenu(UserDao userDao, Scanner sc) {
        this.userDao = userDao;
        this.sc = sc;
    }

    // ==========================================
    // ADMIN DASHBOARD
    // ==========================================

    public void display(User admin) {

        boolean loggedIn = true;

        while (loggedIn) {

            System.out.println("\n==========================================");
            System.out.println("             ADMIN DASHBOARD");
            System.out.println("==========================================");
            System.out.println("1. Manage User Accounts");
            System.out.println("2. Manage Services");
            System.out.println("3. Manage Service Prices and Availability");
            System.out.println("4. View Revenue & Service Utilization Reports");
            System.out.println("5. View System Activity & Security Audit Logs");
            System.out.println("6. Manage Staff Schedules");
            System.out.println("7. View Appointments");
            System.out.println("8. Logout");
            System.out.print("Choose an option (1-8): ");

            String choice = sc.nextLine().trim();

            switch (choice) {

                case "1" -> manageUserAccounts(admin);

                case "2" -> manageServices();


                case "3" -> manageServicePricesAndAvailability();


                case "4" -> viewRevenueAndServiceReports(admin);


                case "5" -> viewSystemAuditLogs();

                case "6" -> manageStaffSchedules();

                case "7" -> viewAppointments(admin);


                case "8" -> {
                    System.out.println("Logging out from Admin Dashboard...");
                    loggedIn = false;
                }

                default -> System.out.println("-> [ERROR] Invalid Option");
            }
        }
    }

    // ==========================================
    // SYSTEM AUDIT LOGS
    // ==========================================

    private void viewSystemAuditLogs() {

        boolean viewingLogs = true;

        while (viewingLogs) {

            System.out.println("\n==========================================");
            System.out.println("         SYSTEM AUDIT LOG MENU");
            System.out.println("==========================================");
            System.out.println("1. View All System Logs");
            System.out.println("2. Filter Logs by User ID");
            System.out.println("3. Return to Admin Dashboard");
            System.out.print("Choose an option (1-3): ");

            String choice = sc.nextLine().trim();

            switch (choice) {

                case "1" ->
                        printAuditTable(activityLogDao.getAllLogs());

                case "2" -> {
                    int userId = readIntInput("Enter User ID to filter: ");
                    printAuditTable(activityLogDao.getLogsByUserId(userId));
                }

                case "3" -> viewingLogs = false;

                default -> System.out.println("-> [ERROR] Invalid Option");
            }
        }
    }

    private void printAuditTable(List<ActivityLog> logs) {

        if (logs.isEmpty()) {
            System.out.println("\n-> No activity logs found.");
            return;
        }

        System.out.println("\n==========================================================================================================================");
        System.out.printf("%-6s | %-8s | %-20s | %-52s | %-22s%n",
                "ID", "User ID", "Action Type", "Description", "Timestamp");
        System.out.println("==========================================================================================================================");

        for (ActivityLog log : logs) {

            System.out.printf("%-6d | %-8d | %-20s | %-52s | %-22s%n",
                    log.getId(),
                    log.getUserId(),
                    log.getActionType(),
                    log.getDescription(),
                    log.getTimestamp().toString());
        }

        System.out.println("==========================================================================================================================\n");
    }

    // ==========================================
    // USER ACCOUNT MANAGEMENT
    // ==========================================

    private void manageUserAccounts(User admin) {

        boolean managingUsers = true;

        while (managingUsers) {

            System.out.println("\n==========================================");
            System.out.println("            USER ACCOUNT MANAGEMENT");
            System.out.println("==========================================");
            System.out.println("1. View All Users");
            System.out.println("2. View Pending User Approvals");
            System.out.println("3. Approve User Registration");
            System.out.println("4. Update User Role / Status");
            System.out.println("5. Return to Admin Dashboard");
            System.out.print("Choose an option (1-5): ");

            String choice = sc.nextLine().trim();

            switch (choice) {

                case "1" -> viewAllUser();

                case "2" -> viewPendingUsers();

                case "3" -> approveUser();

                case "4" -> updateUser(admin);

                case "5" -> managingUsers = false;

                default -> System.out.println("-> [ERROR] Invalid Option");
            }
        }
    }

    private void displayUserTable(List<User> users, String title) {

        System.out.println("\n================ " + title + " ================");

        if (users.isEmpty()) {
            System.out.println("-> No Records Found");
            return;
        }

        System.out.println("+------+----------------------+---------------------------+--------------+------------+");
        System.out.printf("| %-4s | %-20s | %-25s | %-12s | %-10s |%n",
                "ID", "Full Name", "Email", "Role", "Status");
        System.out.println("+------+----------------------+---------------------------+--------------+------------+");

        for (User u : users) {

            System.out.printf("| %-4d | %-20s | %-25s | %-12s | %-10s |%n",
                    u.id(), u.fullName(), u.email(), u.role(), u.status());
        }

        System.out.println("+------+----------------------+---------------------------+--------------+------------+");
    }

    private void viewAllUser() {
        displayUserTable(userDao.getAllUser(), "ALL REGISTERED USERS");
    }

    private void viewPendingUsers() {
        displayUserTable(userDao.getPendingUser(), "PENDING APPROVALS");
    }

    private void approveUser() {

        List<User> pendingUsers = userDao.getPendingUser();

        displayUserTable(pendingUsers, "PENDING APPROVALS");

        if (pendingUsers.isEmpty()) {
            return;
        }

        int userId = readIntInput("Enter User ID to Approve (or 0 to cancel): ");

        if (userId == 0) {
            System.out.println("-> Operation Cancelled by the user.");
            return;
        }

        boolean exists = pendingUsers.stream()
                .anyMatch(u -> u.id() == userId);

        if (!exists) {
            System.out.println("-> [ERROR] User ID not found.");
            return;
        }

        userDao.approveUser(userId);

        System.out.println("-> [SUCCESS] User approved successfully.");
    }

    private void updateUser(User currentAdmin) {

        List<User> users = userDao.getAllUser();

        displayUserTable(users, "SELECT USER TO UPDATE");

        if (users.isEmpty()) {
            return;
        }

        int userId = readIntInput("Enter selected ID to update (or 0 to cancel): ");

        if (userId == 0) {
            System.out.println("-> Operation Cancelled.");
            return;
        }

        if (userId == currentAdmin.id()) {
            System.out.println("-> [WARNING] Modifying current active Admin is restricted.");
            return;
        }

        String newRole = readValidRole();

        if (newRole == null) {
            System.out.println("-> Operation Cancelled.");
            return;
        }

        String newStatus = readValidStatus();

        if (newStatus.equals("0") || newStatus.equals("CANCEL")) {
            System.out.println("-> Operation Cancelled.");
            return;
        }

        userDao.updateUserAndStatus(userId, newRole, newStatus);

        System.out.println("-> [SUCCESS] User information updated.");
    }

    private String readValidRole() {

        while (true) {

            System.out.print("Enter Role (PET OWNER/RECEPTIONIST/VETERINARIAN/ADMIN [0 to cancel]): ");

            String role = sc.nextLine().trim().toUpperCase();

            if (role.equals("0") || role.equals("CANCEL")) {
                return null;
            }

            if (role.equals("PET OWNER")
                    || role.equals("RECEPTIONIST")
                    || role.equals("VETERINARIAN")
                    || role.equals("ADMIN")) {
                return role;
            }

            System.out.println("->[ERROR] Invalid Role.");
        }
    }

    private String readValidStatus() {

        while (true) {

            System.out.print("Enter Status (APPROVED / PENDING) [0 to cancel]: ");

            String status = sc.nextLine().trim().toUpperCase();

            if (status.equals("0") || status.equals("CANCEL")) {
                return status;
            }

            if (status.equals("APPROVED") || status.equals("PENDING")) {
                return status;
            }

            System.out.println("->[ERROR] Invalid status. Must be APPROVED or PENDING.");
        }
    }

    // ==========================================
    // STAFF SCHEDULE MANAGEMENT
    // ==========================================

    private void manageStaffSchedules() {

        boolean managingSchedules = true;

        while (managingSchedules) {

            System.out.println("\n==========================================");
            System.out.println("          STAFF SCHEDULE MANAGEMENT");
            System.out.println("==========================================");
            System.out.println("1. View All Veterinarian Schedules");
            System.out.println("2. View Schedules by Veterinarian");
            System.out.println("3. Add Available Schedule");
            System.out.println("4. Update Schedule Date and Time");
            System.out.println("5. Change Schedule Availability");
            System.out.println("6. Return to Admin Dashboard");
            System.out.print("Choose an option (1-6): ");

            String choice = sc.nextLine().trim();

            switch (choice) {

                case "1" -> viewAllVeterinarianSchedules();

                case "2" -> viewSchedulesByVeterinarian();

                case "3" -> addVeterinarianSchedule();

                case "4" -> updateVeterinarianSchedule();

                case "5" -> changeScheduleAvailability();

                case "6" -> managingSchedules = false;

                default -> System.out.println("-> [ERROR] Invalid Option.");
            }
        }
    }

    // ==========================================
    // VIEW ALL VETERINARIAN SCHEDULES
    // ==========================================

    private void viewAllVeterinarianSchedules() {

        String sql = """
                SELECT
                    ss.schedule_id,
                    u.user_id,
                    u.full_name,
                    ss.schedule_date,
                    ss.shift_time,
                    ss.start_time,
                    ss.end_time,
                    ss.availability_status
                FROM staff_schedules ss
                INNER JOIN users u
                    ON ss.staff_id = u.user_id
                WHERE UPPER(TRIM(u.user_role)) = 'VETERINARIAN'
                ORDER BY ss.schedule_date, ss.start_time
                """;

        System.out.println("\n==========================================");
        System.out.println("       ALL VETERINARIAN SCHEDULES");
        System.out.println("==========================================");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            printScheduleRows(rs);

        } catch (SQLException e) {

            System.out.println("Error retrieving schedules: " + e.getMessage());
        }
    }

    // ==========================================
    // VIEW SCHEDULES BY VETERINARIAN
    // ==========================================

    private void viewSchedulesByVeterinarian() {

        viewVeterinarianList();

        int veterinarianId = readIntInput(
                "Enter Veterinarian User ID (or 0 to cancel): ");

        if (veterinarianId == 0) {
            return;
        }

        String sql = """
                SELECT
                    ss.schedule_id,
                    u.user_id,
                    u.full_name,
                    ss.schedule_date,
                    ss.shift_time,
                    ss.start_time,
                    ss.end_time,
                    ss.availability_status
                FROM staff_schedules ss
                INNER JOIN users u
                    ON ss.staff_id = u.user_id
                WHERE u.user_id = ?
                  AND UPPER(TRIM(u.user_role)) = 'VETERINARIAN'
                ORDER BY ss.schedule_date, ss.start_time
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, veterinarianId);

            try (ResultSet rs = pstmt.executeQuery()) {
                printScheduleRows(rs);
            }

        } catch (SQLException e) {

            System.out.println("Error retrieving veterinarian schedules: " + e.getMessage());
        }
    }

    // ==========================================
    // DISPLAY VETERINARIAN LIST
    // ==========================================

    private void viewVeterinarianList() {

        String sql = """
                SELECT user_id, full_name, status
                FROM users
                WHERE UPPER(TRIM(user_role)) = 'VETERINARIAN'
                ORDER BY full_name
                """;

        System.out.println("\n==========================================");
        System.out.println("          REGISTERED VETERINARIANS");
        System.out.println("==========================================");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            System.out.printf("%-8s %-28s %-15s%n",
                    "USER ID", "FULL NAME", "STATUS");

            System.out.println("------------------------------------------");

            boolean found = false;

            while (rs.next()) {

                found = true;

                System.out.printf("%-8d %-28s %-15s%n",
                        rs.getInt("user_id"),
                        rs.getString("full_name"),
                        rs.getString("status"));
            }

            if (!found) {
                System.out.println("-> No veterinarian accounts found.");
            }

        } catch (SQLException e) {

            System.out.println("Error retrieving veterinarians: " + e.getMessage());
        }
    }

    // ==========================================
    // DISPLAY SCHEDULE TABLE
    // ==========================================

    private void printScheduleRows(ResultSet rs) throws SQLException {

        System.out.printf(
                "%-10s %-10s %-24s %-15s %-22s %-12s %-12s %-15s%n",
                "SCHEDULE", "USER ID", "VETERINARIAN",
                "DATE", "SHIFT", "START", "END", "AVAILABILITY");

        System.out.println("----------------------------------------------------------------------------------------------------------------");

        boolean found = false;

        while (rs.next()) {

            found = true;

            System.out.printf(
                    "%-10d %-10d %-24s %-15s %-22s %-12s %-12s %-15s%n",
                    rs.getInt("schedule_id"),
                    rs.getInt("user_id"),
                    rs.getString("full_name"),
                    rs.getString("schedule_date"),
                    rs.getString("shift_time"),
                    rs.getString("start_time"),
                    rs.getString("end_time"),
                    rs.getString("availability_status"));
        }

        if (!found) {
            System.out.println("-> No schedules found.");
        }

        System.out.println("----------------------------------------------------------------------------------------------------------------");
    }

    // ==========================================
    // ADD VETERINARIAN SCHEDULE
    // ==========================================

    private void addVeterinarianSchedule() {

        viewVeterinarianList();

        int veterinarianId = readIntInput(
                "Enter Veterinarian User ID (or 0 to cancel): ");

        if (veterinarianId == 0) {
            return;
        }

        try {

            if (!isApprovedVeterinarian(veterinarianId)) {
                System.out.println(
                        "-> [ERROR] Veterinarian not found or account is not approved.");
                return;
            }

            System.out.print("Enter Schedule Date (YYYY-MM-DD): ");
            LocalDate date = LocalDate.parse(sc.nextLine().trim());

            if (date.isBefore(LocalDate.now())) {
                System.out.println("-> Schedule date cannot be in the past.");
                return;
            }

            System.out.print("Enter Start Time (HH:mm): ");
            LocalTime start = LocalTime.parse(sc.nextLine().trim());

            System.out.print("Enter End Time (HH:mm): ");
            LocalTime end = LocalTime.parse(sc.nextLine().trim());

            if (!end.isAfter(start)) {
                System.out.println("-> End time must be later than start time.");
                return;
            }

            String startValue = start.toString();
            String endValue = end.toString();

            if (hasScheduleConflict(
                    veterinarianId,
                    date.toString(),
                    startValue,
                    endValue,
                    0)) {

                System.out.println(
                        "-> [ERROR] This schedule overlaps another available schedule.");
                return;
            }

            String shiftTime = startValue + " - " + endValue;

            String sql = """
                    INSERT INTO staff_schedules (
                        staff_id,
                        schedule_date,
                        shift_time,
                        availability_status,
                        start_time,
                        end_time
                    )
                    VALUES (?, ?, ?, 'AVAILABLE', ?, ?)
                    """;

            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setInt(1, veterinarianId);
                pstmt.setString(2, date.toString());
                pstmt.setString(3, shiftTime);
                pstmt.setString(4, startValue);
                pstmt.setString(5, endValue);

                pstmt.executeUpdate();

                System.out.println(
                        "-> [SUCCESS] Veterinarian schedule added successfully.");
            }

        } catch (DateTimeParseException e) {

            System.out.println(
                    "-> [ERROR] Invalid date or time format. Please use YYYY-MM-DD and HH:mm.");

        } catch (SQLException e) {

            System.out.println(
                    "-> [ERROR] Database error while adding schedule: "
                            + e.getMessage());
        }
    }

    // ==========================================
    // UPDATE SCHEDULE DATE AND TIME
    // ==========================================

    private void updateVeterinarianSchedule() {

        viewAllVeterinarianSchedules();

        int scheduleId = readIntInput(
                "Enter Schedule ID to update (or 0 to cancel): ");

        if (scheduleId == 0) {
            return;
        }

        try {

            int veterinarianId;
            String currentStatus;

            String checkSQL = """
                    SELECT staff_id, availability_status
                    FROM staff_schedules
                    WHERE schedule_id = ?
                    """;

            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(checkSQL)) {

                pstmt.setInt(1, scheduleId);

                try (ResultSet rs = pstmt.executeQuery()) {

                    if (!rs.next()) {
                        System.out.println("-> [ERROR] Schedule ID not found.");
                        return;
                    }

                    veterinarianId = rs.getInt("staff_id");
                    currentStatus = rs.getString("availability_status");
                }
            }

            if (!"AVAILABLE".equalsIgnoreCase(currentStatus)) {
                System.out.println("-> Only AVAILABLE schedules can be updated.");
                return;
            }

            System.out.print("Enter New Schedule Date (YYYY-MM-DD): ");
            LocalDate date = LocalDate.parse(sc.nextLine().trim());

            if (date.isBefore(LocalDate.now())) {
                System.out.println("-> Schedule date cannot be in the past.");
                return;
            }

            System.out.print("Enter New Start Time (HH:mm): ");
            LocalTime start = LocalTime.parse(sc.nextLine().trim());

            System.out.print("Enter New End Time (HH:mm): ");
            LocalTime end = LocalTime.parse(sc.nextLine().trim());

            if (!end.isAfter(start)) {
                System.out.println("-> End time must be later than start time.");
                return;
            }

            String startValue = start.toString();
            String endValue = end.toString();

            if (hasScheduleConflict(
                    veterinarianId,
                    date.toString(),
                    startValue,
                    endValue,
                    scheduleId)) {

                System.out.println(
                        "-> [ERROR] Updated schedule overlaps another available schedule.");
                return;
            }

            String shiftTime = startValue + " - " + endValue;

            String updateSQL = """
                    UPDATE staff_schedules
                    SET schedule_date = ?,
                        shift_time = ?,
                        start_time = ?,
                        end_time = ?
                    WHERE schedule_id = ?
                    """;

            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {

                pstmt.setString(1, date.toString());
                pstmt.setString(2, shiftTime);
                pstmt.setString(3, startValue);
                pstmt.setString(4, endValue);
                pstmt.setInt(5, scheduleId);

                int updated = pstmt.executeUpdate();

                if (updated > 0) {
                    System.out.println("-> [SUCCESS] Schedule updated successfully.");
                } else {
                    System.out.println("-> [ERROR] Schedule could not be updated.");
                }
            }

        } catch (DateTimeParseException e) {

            System.out.println("-> [ERROR] Invalid date or time format.");

        } catch (SQLException e) {

            System.out.println("Error updating schedule: " + e.getMessage());
        }
    }

    // ==========================================
    // CHANGE SCHEDULE AVAILABILITY
    // ==========================================

    private void changeScheduleAvailability() {

        viewAllVeterinarianSchedules();

        int scheduleId = readIntInput(
                "Enter Schedule ID to update availability (or 0 to cancel): ");

        if (scheduleId == 0) {
            return;
        }

        System.out.println("\n1. AVAILABLE");
        System.out.println("2. UNAVAILABLE");
        System.out.print("Select new availability: ");

        String choice = sc.nextLine().trim();

        String newStatus;

        switch (choice) {

            case "1" -> newStatus = "AVAILABLE";

            case "2" -> newStatus = "UNAVAILABLE";

            default -> {
                System.out.println("-> [ERROR] Invalid availability option.");
                return;
            }
        }

        String sql = """
                UPDATE staff_schedules
                SET availability_status = ?
                WHERE schedule_id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newStatus);
            pstmt.setInt(2, scheduleId);

            int updated = pstmt.executeUpdate();

            if (updated > 0) {
                System.out.println(
                        "-> [SUCCESS] Schedule availability updated to "
                                + newStatus + ".");
            } else {
                System.out.println("-> [ERROR] Schedule ID not found.");
            }

        } catch (SQLException e) {

            System.out.println(
                    "Error updating schedule availability: " + e.getMessage());
        }
    }

    // ==========================================
    // CHECK APPROVED VETERINARIAN
    // ==========================================

    private boolean isApprovedVeterinarian(int veterinarianId)
            throws SQLException {

        String sql = """
                SELECT user_id
                FROM users
                WHERE user_id = ?
                  AND UPPER(TRIM(user_role)) = 'VETERINARIAN'
                  AND UPPER(TRIM(status)) = 'APPROVED'
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, veterinarianId);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    // ==========================================
    // CHECK OVERLAPPING AVAILABLE SCHEDULES
    // ==========================================

    private boolean hasScheduleConflict(
            int veterinarianId,
            String scheduleDate,
            String startTime,
            String endTime,
            int excludeScheduleId) throws SQLException {

        String sql = """
                SELECT COUNT(*)
                FROM staff_schedules
                WHERE staff_id = ?
                  AND schedule_date = ?
                  AND UPPER(TRIM(availability_status)) = 'AVAILABLE'
                  AND schedule_id <> ?
                  AND time(start_time) < time(?)
                  AND time(end_time) > time(?)
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, veterinarianId);
            pstmt.setString(2, scheduleDate);
            pstmt.setInt(3, excludeScheduleId);
            pstmt.setString(4, endTime);
            pstmt.setString(5, startTime);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }


    // ==========================================
    // 2. SERVICE MANAGEMENT
    // ==========================================

    private void manageServices() {
        boolean managing = true;

        while (managing) {
            System.out.println("\n========== SERVICE MANAGEMENT ==========");
            System.out.println("1. Add New Service");
            System.out.println("2. View All Services");
            System.out.println("3. Update Service Information");
            System.out.println("4. Deactivate Service");
            System.out.println("5. Return to Admin Dashboard");
            System.out.print("Choose an option: ");

            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> addService();
                case "2" -> displayServices();
                case "3" -> updateServiceInformation();
                case "4" -> deactivateService();
                case "5" -> managing = false;
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private void displayServices() {
        List<Service> services = serviceDao.getAllServices();

        System.out.println("\n================ ALL SERVICES ================");

        if (services.isEmpty()) {
            System.out.println("No services found.");
            return;
        }

        String sql = """
                SELECT service_id, service_name, category,
                       duration_minutes, price, availability
                FROM services
                ORDER BY service_id
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            System.out.printf("%-5s %-22s %-15s %-10s %-12s %-15s%n",
                    "ID", "Service", "Category", "Minutes", "Price", "Availability");

            System.out.println(
                    "--------------------------------------------------------------------------");

            while (rs.next()) {
                System.out.printf("%-5d %-22s %-15s %-10d ₱%-11.2f %-15s%n",
                        rs.getInt("service_id"),
                        rs.getString("service_name"),
                        rs.getString("category"),
                        rs.getInt("duration_minutes"),
                        rs.getDouble("price"),
                        rs.getString("availability"));
            }

        } catch (SQLException e) {
            System.out.println("Error displaying services: " + e.getMessage());
        }
    }

    private void addService() {
        System.out.println("\n========== ADD NEW SERVICE ==========");

        System.out.print("Service Name: ");
        String name = sc.nextLine().trim();

        if (name.isEmpty()) {
            System.out.println("Service name cannot be empty.");
            return;
        }

        System.out.print("Description: ");
        String description = sc.nextLine().trim();

        System.out.print("Category: ");
        String category = sc.nextLine().trim();

        if (category.isEmpty()) {
            System.out.println("Category cannot be empty.");
            return;
        }

        double price = readDoubleInput("Price: ");

        if (price < 0) {
            System.out.println("Price cannot be negative.");
            return;
        }

        int duration = readIntInput("Duration in minutes: ");

        if (duration <= 0) {
            System.out.println("Duration must be greater than zero.");
            return;
        }

        boolean success = serviceDao.addService(
                name, description, price, category, duration
        );

        if (success) {
            System.out.println("Service added successfully.");
        } else {
            System.out.println("Unable to add service.");
        }
    }

    private void updateServiceInformation() {
        displayServices();

        int serviceId = readIntInput(
                "\nEnter Service ID to update (0 to cancel): ");

        if (serviceId == 0) {
            return;
        }

        Service service = serviceDao.getServiceById(serviceId);

        if (service == null) {
            System.out.println("Service not found or unavailable.");
            return;
        }

        System.out.println("Enter the new service information.");

        System.out.print("New Service Name: ");
        String name = sc.nextLine().trim();

        System.out.print("New Description: ");
        String description = sc.nextLine().trim();

        System.out.print("New Category: ");
        String category = sc.nextLine().trim();

        int duration = readIntInput("New Duration in minutes: ");

        if (name.isEmpty() || category.isEmpty() || duration <= 0) {
            System.out.println("Invalid service information.");
            return;
        }

        boolean success = serviceDao.updateService(
                serviceId, name, description, category, duration
        );

        if (success) {
            System.out.println("Service information updated.");
        } else {
            System.out.println("Unable to update service.");
        }
    }

    private void deactivateService() {
        displayServices();

        int serviceId = readIntInput(
                "\nEnter Service ID to deactivate (0 to cancel): ");

        if (serviceId == 0) {
            return;
        }

        Service service = serviceDao.getServiceById(serviceId);

        if (service == null) {
            System.out.println("Service not found or already unavailable.");
            return;
        }

        System.out.print("Deactivate this service? (Y/N): ");
        String answer = sc.nextLine().trim();

        if (!answer.equalsIgnoreCase("Y")) {
            System.out.println("Operation cancelled.");
            return;
        }

        if (serviceDao.deactivateService(serviceId)) {
            System.out.println("Service deactivated successfully.");
        } else {
            System.out.println("Unable to deactivate service.");
        }
    }


    // ==========================================
    // 3. SERVICE PRICE AND AVAILABILITY
    // ==========================================

    private void manageServicePricesAndAvailability() {
        boolean managing = true;

        while (managing) {
            System.out.println("\n===== SERVICE PRICE AND AVAILABILITY =====");
            System.out.println("1. View Services");
            System.out.println("2. Update Service Price");
            System.out.println("3. Change Service Availability");
            System.out.println("4. Return to Admin Dashboard");
            System.out.print("Choose an option: ");

            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> displayServices();
                case "2" -> updateServicePrice();
                case "3" -> changeServiceAvailability();
                case "4" -> managing = false;
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private void updateServicePrice() {
        displayServices();

        int serviceId = readIntInput(
                "\nEnter Service ID (0 to cancel): ");

        if (serviceId == 0) {
            return;
        }

        Service service = serviceDao.getServiceById(serviceId);

        if (service == null) {
            System.out.println("Service not found or unavailable.");
            return;
        }

        double newPrice = readDoubleInput("Enter new price: ");

        if (newPrice < 0) {
            System.out.println("Price cannot be negative.");
            return;
        }

        if (serviceDao.updateServicePrice(serviceId, newPrice)) {
            System.out.println("Service price updated successfully.");
        } else {
            System.out.println("Unable to update service price.");
        }
    }

    private void changeServiceAvailability() {
        displayServices();

        int serviceId = readIntInput(
                "\nEnter Service ID (0 to cancel): ");

        if (serviceId == 0) {
            return;
        }

        System.out.println("1. Available");
        System.out.println("2. Unavailable");
        System.out.print("Select new availability: ");

        String choice = sc.nextLine().trim();

        boolean available;

        if (choice.equals("1")) {
            available = true;
        } else if (choice.equals("2")) {
            available = false;
        } else {
            System.out.println("Invalid availability option.");
            return;
        }

        if (serviceDao.updateAvailability(serviceId, available)) {
            System.out.println("Service availability updated successfully.");
        } else {
            System.out.println("Service ID not found or update failed.");
        }
    }


    // ==========================================
    // 4. REVENUE AND SERVICE UTILIZATION REPORT
    // ==========================================

    private void viewRevenueAndServiceReports(User admin) {
        System.out.println("\n===== MONTHLY REVENUE AND SERVICE REPORT =====");

        System.out.print("Enter month (YYYY-MM): ");
        String month = sc.nextLine().trim();

        if (!month.matches("\\d{4}-(0[1-9]|1[0-2])")) {
            System.out.println("Invalid month. Use YYYY-MM, for example 2026-11.");
            return;
        }

        String revenueSQL = """
                SELECT COUNT(*) AS payment_count,
                       COALESCE(SUM(amount), 0) AS total_revenue
                FROM invoices
                WHERE status = 'PAID'
                  AND strftime('%Y-%m', created_at) = ?
                """;

        String utilizationSQL = """
                SELECT aps.service_name_at_booking AS service_name,
                       COUNT(DISTINCT aps.appointment_id) AS booking_count
                FROM appointment_services aps
                JOIN appointments a
                  ON a.appointment_id = aps.appointment_id
                WHERE strftime('%Y-%m', a.appointment_date) = ?
                  AND UPPER(a.status) NOT IN ('CANCELLED', 'REJECTED')
                GROUP BY aps.service_name_at_booking
                ORDER BY booking_count DESC, service_name
                """;

        double totalRevenue = 0;
        int paymentCount = 0;

        try (Connection conn = DatabaseConnection.getConnection()) {

            try (PreparedStatement pstmt =
                         conn.prepareStatement(revenueSQL)) {

                pstmt.setString(1, month);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        paymentCount = rs.getInt("payment_count");
                        totalRevenue = rs.getDouble("total_revenue");
                    }
                }
            }

            System.out.println("\nREPORT MONTH: " + month);
            System.out.println("----------------------------------------");
            System.out.println("Number of Paid Invoices: " + paymentCount);
            System.out.printf("Total Revenue: ₱%.2f%n", totalRevenue);

            System.out.println("\nSERVICE UTILIZATION");
            System.out.println("----------------------------------------");
            System.out.printf("%-30s %-15s%n",
                    "Service Name", "Bookings");

            try (PreparedStatement pstmt =
                         conn.prepareStatement(utilizationSQL)) {

                pstmt.setString(1, month);

                try (ResultSet rs = pstmt.executeQuery()) {
                    boolean found = false;

                    while (rs.next()) {
                        found = true;

                        System.out.printf("%-30s %-15d%n",
                                rs.getString("service_name"),
                                rs.getInt("booking_count"));
                    }

                    if (!found) {
                        System.out.println("No service bookings found for this month.");
                    }
                }
            }

        } catch (SQLException e) {
            System.out.println("Report error: " + e.getMessage());
            return;
        }

        System.out.print("\nExport this report to CSV? (Y/N): ");
        String export = sc.nextLine().trim();

        if (export.equalsIgnoreCase("Y")) {
            exportMonthlyReport(month, admin.id());
        }
    }

    private void exportMonthlyReport(String month, int adminId) {
        String fileName = "PawSync_Report_" + month + ".csv";

        String revenueSQL = """
                SELECT COUNT(*) AS payment_count,
                       COALESCE(SUM(amount), 0) AS total_revenue
                FROM invoices
                WHERE status = 'PAID'
                  AND strftime('%Y-%m', created_at) = ?
                """;

        String utilizationSQL = """
                SELECT aps.service_name_at_booking AS service_name,
                       COUNT(DISTINCT aps.appointment_id) AS booking_count
                FROM appointment_services aps
                JOIN appointments a
                  ON a.appointment_id = aps.appointment_id
                WHERE strftime('%Y-%m', a.appointment_date) = ?
                  AND UPPER(a.status) NOT IN ('CANCELLED', 'REJECTED')
                GROUP BY aps.service_name_at_booking
                ORDER BY booking_count DESC, service_name
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             FileWriter writer = new FileWriter(fileName)) {

            writer.write("PAWSYNC MONTHLY REPORT\n");
            writer.write("Month," + month + "\n\n");

            writer.write("REVENUE SUMMARY\n");
            writer.write("Number of Paid Invoices,Total Revenue\n");

            try (PreparedStatement pstmt =
                         conn.prepareStatement(revenueSQL)) {

                pstmt.setString(1, month);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        writer.write(
                                rs.getInt("payment_count") + ","
                                        + rs.getDouble("total_revenue") + "\n"
                        );
                    }
                }
            }

            writer.write("\nSERVICE UTILIZATION\n");
            writer.write("Service Name,Number of Bookings\n");

            try (PreparedStatement pstmt =
                         conn.prepareStatement(utilizationSQL)) {

                pstmt.setString(1, month);

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        writer.write(
                                csvValue(rs.getString("service_name")) + ","
                                        + rs.getInt("booking_count") + "\n"
                        );
                    }
                }
            }

            System.out.println("Report exported successfully: " + fileName);

            activityLogDao.logAction(
                    adminId,
                    "REPORT_EXPORTED",
                    "Monthly report exported for " + month
            );

        } catch (SQLException | IOException e) {
            System.out.println("Unable to export report: " + e.getMessage());
        }
    }

    private String csvValue(String value) {
        if (value == null) {
            return "";
        }

        return "\"" + value.replace("\"", "\"\"") + "\"";
    }


    // ==========================================
    // 7. APPOINTMENT MANAGEMENT
    // ==========================================

    private void viewAppointments(User admin) {
        boolean viewing = true;

        while (viewing) {
            System.out.println("\n========== APPOINTMENT MANAGEMENT ==========");
            System.out.println("1. View All Appointments");
            System.out.println("2. Search Appointment by ID");
            System.out.println("3. Update Appointment Status");
            System.out.println("4. Return to Admin Dashboard");
            System.out.print("Choose an option: ");

            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> displayAppointments(0);
                case "2" -> {
                    int appointmentId = readIntInput("Enter Appointment ID: ");
                    displayAppointments(appointmentId);
                }
                case "3" -> updateAppointmentStatus(admin);
                case "4" -> viewing = false;
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private void displayAppointments(int appointmentId) {
        String sql = """
                SELECT
                    a.id,
                    a.appointment_date,
                    a.start_time,
                    a.end_time,
                    a.service_type,
                    a.status,
                    a.total_amount,
                    owner.full_name AS owner_name,
                    p.pet_name,
                    vet.full_name AS veterinarian_name
                FROM appointments a
                JOIN users owner
                    ON a.user_id = owner.user_id
                JOIN pets p
                    ON a.pet_id = p.pet_id
                LEFT JOIN users vet
                    ON a.staff_id = vet.user_id
                WHERE (? = 0 OR a.id = ?)
                ORDER BY a.appointment_date DESC, a.start_time DESC
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, appointmentId);
            pstmt.setInt(2, appointmentId);

            try (ResultSet rs = pstmt.executeQuery()) {
                boolean found = false;

                System.out.println("\n================ APPOINTMENTS ================");

                System.out.printf(
                        "%-5s %-12s %-10s %-18s %-15s %-18s %-12s%n",
                        "ID", "Date", "Time", "Pet", "Owner",
                        "Veterinarian", "Status"
                );

                System.out.println(
                        "--------------------------------------------------------------------------------");

                while (rs.next()) {
                    found = true;

                    String time = rs.getString("start_time") == null
                            ? "N/A" : rs.getString("start_time");

                    String veterinarian =
                            rs.getString("veterinarian_name") == null
                                    ? "Unassigned"
                                    : rs.getString("veterinarian_name");

                    System.out.printf(
                            "%-5d %-12s %-10s %-18s %-15s %-18s %-12s%n",
                            rs.getInt("id"),
                            rs.getString("appointment_date"),
                            time,
                            rs.getString("pet_name"),
                            rs.getString("owner_name"),
                            veterinarian,
                            rs.getString("status")
                    );

                    System.out.println("Services: " + rs.getString("service_type"));
                    System.out.printf("Total Amount: ₱%.2f%n",
                            rs.getDouble("total_amount"));
                    System.out.println();
                }

                if (!found) {
                    System.out.println("No appointments found.");
                }
            }

        } catch (SQLException e) {
            System.out.println("Error retrieving appointments: " + e.getMessage());
        }
    }

    private void updateAppointmentStatus(User admin) {
        displayAppointments(0);

        int appointmentId = readIntInput(
                "\nEnter Appointment ID to update (0 to cancel): ");

        if (appointmentId == 0) {
            return;
        }

        String currentStatus = null;

        String checkSQL = """
                SELECT status
                FROM appointments
                WHERE id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(checkSQL)) {

            pstmt.setInt(1, appointmentId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    currentStatus = rs.getString("status");
                }
            }

        } catch (SQLException e) {
            System.out.println("Error checking appointment: " + e.getMessage());
            return;
        }

        if (currentStatus == null) {
            System.out.println("Appointment not found.");
            return;
        }

        System.out.println("\nCurrent Status: " + currentStatus);
        System.out.println("1. PENDING");
        System.out.println("2. CONFIRMED");
        System.out.println("3. CANCELLED");
        System.out.println("4. COMPLETED");
        System.out.println("5. Return");
        System.out.print("Choose the new status: ");

        String choice = sc.nextLine().trim();

        String newStatus = switch (choice) {
            case "1" -> "PENDING";
            case "2" -> "CONFIRMED";
            case "3" -> "CANCELLED";
            case "4" -> "COMPLETED";
            default -> null;
        };

        if (choice.equals("5")) {
            System.out.println("Operation cancelled.");
            return;
        }

        if (newStatus == null) {
            System.out.println("Invalid status option.");
            return;
        }

        if (currentStatus.equalsIgnoreCase(newStatus)) {
            System.out.println("Appointment already has that status.");
            return;
        }

        System.out.print("Confirm status update? (Y/N): ");
        String confirmation = sc.nextLine().trim();

        if (!confirmation.equalsIgnoreCase("Y")) {
            System.out.println("Operation cancelled.");
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
                System.out.println("Appointment status updated successfully.");

                activityLogDao.logAction(
                        admin.id(),
                        "APPOINTMENT_STATUS_UPDATED",
                        "Appointment ID " + appointmentId
                                + " changed from " + currentStatus
                                + " to " + newStatus
                );
            } else {
                System.out.println("Appointment status was not updated.");
            }

        } catch (SQLException e) {
            System.out.println("Error updating appointment: " + e.getMessage());
        }
    }


    // ==========================================
    // DECIMAL INPUT VALIDATION
    // ==========================================

    private double readDoubleInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();

            try {
                double value = Double.parseDouble(input);

                if (!Double.isFinite(value)) {
                    System.out.println("Please enter a valid finite number.");
                    continue;
                }

                return value;

            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid amount.");
            }
        }
    }


    // ==========================================
    // GENERAL INPUT
    // ==========================================

    private int readIntInput(String prompt) {

        while (true) {

            System.out.print(prompt);

            String input = sc.nextLine().trim();

            try {

                return Integer.parseInt(input);

            } catch (NumberFormatException e) {

                System.out.println(
                        " -> [ERROR] Please enter a valid numeric ID.");
            }
        }
    }
}
