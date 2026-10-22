
package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.List;
import java.util.Scanner;

public class AdminMenu {

    private final UserDao userDao;
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
                case "1" -> manageUserAccounts();
                case "2" -> System.out.println("Service Management is not implemented yet.");
                case "3" -> System.out.println("Service Price and Availability is not implemented yet.");
                case "4" -> System.out.println("Reports are not implemented yet.");
                case "5" -> System.out.println("Activity Logs are not implemented yet.");
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


    private void manageUserAccounts() {
        boolean managingUsers = true;

        while (managingUsers) {
            System.out.println("\n==========================================");
            System.out.println("           USER ACCOUNT MANAGEMENT");
            System.out.println("==========================================");
            System.out.println("1. View All Users");
            System.out.println("2. View Pending User Approvals");
            System.out.println("3. Approve User Registration");
            System.out.println("4. Add New User");
            System.out.println("5. Update User Role / Status");
            System.out.println("6. Delete User Account");
            System.out.println("7. Return to Admin Dashboard");
            System.out.print("Choose an option (1-7): ");

            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> viewAllUser();
                case "2" -> viewPendingUsers();
                case "3" -> approveUser();
                case "7" -> managingUsers = false;
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

        System.out.println("+------+-----------------+----------------+------------+");
        System.out.printf("| %-4s | %-15s | %-14s | %-10s |%n",
                "ID", "Username", "Role", "Status");
        System.out.println("+------+-----------------+----------------+------------+");

        for (User u : users) {
            System.out.printf("| %-4d | %-15s | %-14s | %-10s |%n",
                    u.id(), u.username(), u.role(), u.status());
        }

        System.out.println("+------+-----------------+----------------+------------+");
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

        System.out.print("Enter User ID to Approve (or 0 to cancel): ");
        int userId = sc.nextInt();

        if (userId == 0){
            System.out.println("-> Operation Cancelled by the user.");

    }

        boolean exits = pendingUsers.stream().anyMatch(u ->u.id() == userId);
        if (!exits){
            System.out.println("-> [ERROR] User ID not found.");
            return;
        }

        userDao.approveUser(userId);

    }

}


