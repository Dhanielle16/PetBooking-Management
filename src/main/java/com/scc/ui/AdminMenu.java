
package com.scc.ui;

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

                case "2" ->
                        System.out.println("Service Management is not implemented yet.");

                case "3" ->
                        System.out.println("Service Price and Availability is not implemented yet.");

                case "4" ->
                        System.out.println("Reports are not implemented yet.");

                case "5" -> viewSystemAuditLogs();

                case "6" -> manageStaffSchedules();

                case "7" ->
                        System.out.println("Appointment Management is not implemented yet.");

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
