package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.AppointmentDao;
import com.scc.dao.PetDao;
import com.scc.model.User;

import java.util.Scanner;

public class PetOwnerMenu {
    private final User owner;
    private final Scanner sc;
    private final PetDao petDao = new PetDao();
    private final AppointmentDao appointmentDao = new AppointmentDao();
    private final ActivityLogDao logDao = new ActivityLogDao();

    public PetOwnerMenu(User owner, Scanner sc) {
        this.owner = owner;
        this.sc = sc;
    }

    public void display() {
        boolean active = true;
        while (active) {
            System.out.println("\n==========================================");
            System.out.println("          PET OWNER DASHBOARD");
            System.out.println("==========================================");
            System.out.println("1. View My Profile");
            System.out.println("2. Register New Pet");
            System.out.println("3. Book Service Appointment");
            System.out.println("4. View My Appointments");
            System.out.println("5. Logout");
            System.out.print("Choose an option (1-5): ");

            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1" -> viewProfile();
                case "2" -> registerPetFlow();
                case "3" -> bookAppointmentFlow();
                case "4" -> System.out.println("-> Appointment history view is under development.");
                case "5" -> {
                    System.out.println("-> Logging out from Pet Owner Dashboard...");
                    active = false;
                }
                default -> System.out.println("-> [ERROR] Invalid option.");
            }
        }
    }

    private void viewProfile() {
        System.out.println("\n================ MY PROFILE ================");
        System.out.println("ID        : " + owner.id());
        System.out.println("Full Name : " + owner.fullName());
        System.out.println("Email     : " + owner.email());
        System.out.println("Phone     : " + owner.phoneNumber());
        System.out.println("Role      : " + owner.role());
        System.out.println("Status    : " + owner.status());
        System.out.println("============================================");
    }

    private void registerPetFlow() {
        System.out.println("\n--- REGISTER A NEW PET ---");
        System.out.print("Enter Pet Name: ");
        String name = sc.nextLine().trim();
        System.out.print("Enter Species (e.g., Dog, Cat): ");
        String species = sc.nextLine().trim();
        System.out.print("Enter Breed: ");
        String breed = sc.nextLine().trim();
        System.out.print("Enter Age (Years): ");
        int age = readIntInput("Enter valid numeric age: ");

        petDao.registerPet(owner.id(), name, species, breed, age);
    }

    private void bookAppointmentFlow() {
        System.out.println("\n--- BOOK SERVICE APPOINTMENT ---");
        System.out.print("Enter Pet ID: ");
        int petId = readIntInput("Enter valid numeric Pet ID: ");
        System.out.print("Enter Service Type (e.g., Grooming, Vaccination, Consultation): ");
        String serviceType = sc.nextLine().trim();
        System.out.print("Enter Appointment Date (YYYY-MM-DD): ");
        String date = sc.nextLine().trim();

        appointmentDao.bookAppointment(owner.id(), petId, serviceType, date);
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
}