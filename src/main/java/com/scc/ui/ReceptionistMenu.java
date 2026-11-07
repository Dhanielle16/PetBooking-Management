package com.scc.ui;

import com.scc.dao.BillingDao;
import com.scc.model.User;

import java.util.Scanner;

public class ReceptionistMenu {
    private final User receptionist;
    private final Scanner sc;
    private final BillingDao billingDao = new BillingDao();

    public ReceptionistMenu(User receptionist, Scanner sc) {
        this.receptionist = receptionist;
        this.sc = sc;
    }

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
                case "1" -> System.out.println("-> Viewing appointments list...");
                case "2" -> System.out.println("-> Updating appointment status...");
                case "3" -> processBillingFlow();
                case "4" -> System.out.println("-> Client directory view...");
                case "5" -> {
                    System.out.println("-> Logging out from Receptionist Dashboard...");
                    active = false;
                }
                default -> System.out.println("-> [ERROR] Invalid option.");
            }
        }
    }

    private void processBillingFlow() {
        System.out.println("\n--- PROCESS CLIENT BILLING ---");
        System.out.print("Enter Client User ID: ");
        int clientId = readIntInput("Enter valid client ID: ");
        System.out.print("Enter Total Amount (₱): ");
        double amount = readDoubleInput("Enter valid amount: ");
        System.out.print("Enter Payment Method (Cash / GCash / Credit Card): ");
        String method = sc.nextLine().trim();

        billingDao.processBill(receptionist.id(), clientId, amount, method);
    }

    private int readIntInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("-> [ERROR] Please enter a valid number.");
            }
        }
    }

    private double readDoubleInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Double.parseDouble(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("-> [ERROR] Please enter a valid decimal amount.");
            }
        }
    }
}