package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.UserDao;
import com.scc.model.ActivityLog;
import com.scc.model.User;

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
                case "2" -> System.out.println("Service Management is not implemented yet.");
                case "3" -> System.out.println("Service Price and Availability is not implemented yet.");
                case "4" -> System.out.println("Reports are not implemented yet.");
                case "5" -> viewSystemAuditLogs();
                case "6" -> System.out.println("Staff Schedule Management is not implemented yet.");
                case "7" -> System.out.println("Appointment Management is not implemented yet.");
                case "8" -> {
                    System.out.println("Logging out from Admin Dashboard...");
                    loggedIn = false;
                }
                default -> System.out.println("-> [ERROR] Invalid Option");
            }
        }
    }

    private void viewSystemAuditLogs() {
        boolean viewingLogs = true;
        while (viewingLogs) {
            System.out.println("\n==========================================");
            System.out.println("         SYSTEM AUDIT LOG MENU            ");
            System.out.println("==========================================");
            System.out.println("1. View All System Logs");
            System.out.println("2. Filter Logs by User ID");
            System.out.println("3. Return to Admin Dashboard");
            System.out.print("Choose an option (1-3): ");

            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> printAuditTable(activityLogDao.getAllLogs());
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
        System.out.printf("%-6s | %-8s | %-20s | %-52s | %-22s%n", "ID", "User ID", "Action Type", "Description", "Timestamp");
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

    private void viewPendingUsers(){
        displayUserTable(userDao.getPendingUser(), "PENDING APPROVALS");
    }

    private void approveUser() {
        List<User> pendingUsers = userDao.getPendingUser();
        displayUserTable(pendingUsers, "PENDING APPROVALS");

        if (pendingUsers.isEmpty()) {
            return;
        }

        int userId = readIntInput("Enter User ID to Approve (or 0 to cancel): ");

        if (userId == 0){
            System.out.println("-> Operation Cancelled by the user.");
            return;
        }

        boolean exists = pendingUsers.stream().anyMatch(u -> u.id() == userId);
        if (!exists){
            System.out.println("-> [ERROR] User ID not found.");
            return;
        }

        userDao.approveUser(userId);
        System.out.println("-> [SUCCESS] User approved successfully.");
    }

    private void updateUser(User currentAdmin){
        List<User> users = userDao.getAllUser();
        displayUserTable(users, "SELECT USER TO UPDATE");

        if(users.isEmpty()) {
            return;
        }
        int userId = readIntInput("Enter selected ID to update (or 0 to cancel): ");
        if(userId == 0){
            System.out.println("-> Operation Cancelled.");
            return;
        }

        if(userId == currentAdmin.id()){
            System.out.println("-> [WARNING] Modifying current active Admin is restricted.");
            return;
        }

        String newRole = readValidRole();
        if(newRole == null){
            System.out.println("-> Operation Cancelled.");
            return;
        }

        String newStatus = readValidStatus();
        if(newStatus.equals("0") || newStatus.equals("CANCEL")){
            System.out.println("-> Operation Cancelled.");
            return;
        }

        userDao.updateUserAndStatus(userId, newRole, newStatus);
    }

    private String readValidRole(){
        while(true){
            System.out.print("Enter Role (PET OWNER/RECEPTIONIST/VETERINARIAN/ADMIN [0 to cancel]): ");
            String role = sc.nextLine().trim().toUpperCase();
            if(role.equals("0") || role.equals("CANCEL")){
                return null;
            }

            if(role.equals("PET OWNER") || role.equals("RECEPTIONIST") || role.equals("VETERINARIAN") || role.equals("ADMIN")){
                return role;
            }
            System.out.println(" ->[ERROR] Invalid Role.");
        }
    }

    private String readValidStatus(){
        while(true){
            System.out.print("Enter Status (APPROVED / PENDING) [0 to cancel]: ");
            String status = sc.nextLine().trim().toUpperCase();
            if(status.equals("0") || status.equals("CANCEL")){
                return status;
            }
            if(status.equals("APPROVED") || status.equals("PENDING")){
                return status;
            }
            System.out.println(" -> [ERROR] Invalid status. Must be APPROVED or PENDING.");
        }
    }

    private int readIntInput(String prompt){
        while(true){
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            try{
                return Integer.parseInt(input);
            } catch (NumberFormatException e){
                System.out.println(" -> [ERROR] Please enter a valid numeric ID.");
            }
        }
    }
}