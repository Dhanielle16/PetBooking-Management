package com.scc.ui;

import com.scc.model.User;

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
            System.out.println("1. View Assigned Medical Appointments");
            System.out.println("2. View Pet Medical History");
            System.out.println("3. Add Consultation / Treatment Notes");
            System.out.println("4. Logout");
            System.out.print("Choose an option (1-4): ");

            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1" -> System.out.println("-> Fetching assigned appointments...");
                case "2" -> System.out.println("-> Fetching pet medical history...");
                case "3" -> System.out.println("-> Treatment notes module...");
                case "4" -> {
                    System.out.println("-> Logging out from Veterinarian Dashboard...");
                    active = false;
                }
                default -> System.out.println("-> [ERROR] Invalid option.");
            }
        }
    }
}