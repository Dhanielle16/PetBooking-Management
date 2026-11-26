
package com.scc.ui;

import com.scc.dao.AppointmentDao;
import com.scc.dao.DatabaseConnection;
import com.scc.dao.PetDao;
import com.scc.dao.ServiceDao;
import com.scc.dao.StaffScheduleDao;
import com.scc.model.Service;
import com.scc.model.StaffMember;
import com.scc.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class PetOwnerMenu {

    private static final int BUFFER_MINUTES = 15;

    private final User owner;
    private final Scanner sc;

    private final PetDao petDao = new PetDao();
    private final ServiceDao serviceDao = new ServiceDao();
    private final StaffScheduleDao scheduleDao = new StaffScheduleDao();
    private final AppointmentDao appointmentDao = new AppointmentDao();

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm");

    public PetOwnerMenu(User owner, Scanner sc) {
        this.owner = owner;
        this.sc = sc;
    }

    // Maintains compatibility with Authentication.java
    public void display() {
        showMenu();
    }

    public void showMenu() {
        boolean running = true;

        while (running) {
            System.out.println("\n==================================");
            System.out.println("          PAWSYNC PET OWNER");
            System.out.println("==================================");
            System.out.println("1. View My Profile");
            System.out.println("2. Register New Pet");
            System.out.println("3. Book Service Appointment");
            System.out.println("4. View My Appointments");
            System.out.println("5. Logout");
            System.out.print("Enter your choice: ");

            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> viewProfile();
                case "2" -> registerPet();
                case "3" -> bookServiceAppointment();
                case "4" -> viewMyAppointments();
                case "5" -> {
                    System.out.println("Logging out...");
                    running = false;
                }
                default -> System.out.println("Invalid choice. Try again.");
            }
        }
    }

    private void viewProfile() {
        System.out.println("\n========== MY PROFILE ==========");
        System.out.println("User ID: " + owner.id());
        System.out.println("Full Name: " + owner.fullName());
        System.out.println("Email: " + owner.email());
        System.out.println("Phone Number: " + owner.phoneNumber());
        System.out.println("Role: " + owner.role());
        System.out.println("Status: " + owner.status());
    }

    private void registerPet() {
        System.out.println("\n========== REGISTER NEW PET ==========");

        System.out.print("Pet Name: ");
        String petName = sc.nextLine().trim();

        if (petName.isEmpty()) {
            System.out.println("Pet name cannot be empty.");
            return;
        }

        System.out.print("Species: ");
        String species = sc.nextLine().trim();

        if (species.isEmpty()) {
            System.out.println("Species cannot be empty.");
            return;
        }

        System.out.print("Breed: ");
        String breed = sc.nextLine().trim();

        System.out.print("Age in years: ");
        int age;

        try {
            age = Integer.parseInt(sc.nextLine().trim());

            if (age < 0) {
                System.out.println("Age cannot be negative.");
                return;
            }

        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid age.");
            return;
        }

        if (petDao.registerPet(owner.id(), petName, species, breed, age)) {
            System.out.println("Pet successfully registered!");
        } else {
            System.out.println("Unable to register pet.");
        }
    }

    private void bookServiceAppointment() {
        System.out.println("\n========== BOOK SERVICE APPOINTMENT ==========");

        int petId = selectPet();

        if (petId == -1) {
            return;
        }

        List<Service> selectedServices = selectServices();

        if (selectedServices.isEmpty()) {
            System.out.println("No services selected. Booking cancelled.");
            return;
        }

        int totalDuration = 0;
        double totalAmount = 0;

        for (Service service : selectedServices) {
            totalDuration += service.durationMinutes();
            totalAmount += service.price();
        }

        LocalDate appointmentDate = readAppointmentDate();

        if (appointmentDate == null) {
            return;
        }

        LocalTime startTime = readStartTime();

        if (startTime == null) {
            return;
        }

        LocalTime endTime = startTime.plusMinutes(totalDuration);
        LocalTime reservedUntil = startTime.plusMinutes(
                totalDuration + BUFFER_MINUTES
        );

        if (!endTime.isAfter(startTime)
                || !reservedUntil.isAfter(endTime)) {
            System.out.println("The selected appointment duration is invalid.");
            return;
        }

        if (appointmentDate.equals(LocalDate.now())
                && !startTime.isAfter(LocalTime.now())) {
            System.out.println("The selected start time has already passed.");
            return;
        }

        String date = appointmentDate.toString();
        String start = startTime.format(TIME_FORMAT);
        String end = endTime.format(TIME_FORMAT);
        String reserved = reservedUntil.format(TIME_FORMAT);

        List<StaffMember> availableVeterinarians =
                scheduleDao.getAvailableVeterinarians(
                        date, start, reserved
                );

        if (availableVeterinarians.isEmpty()) {
            System.out.println(
                    "\nNo veterinarian is available for the entire requested time slot."
            );
            System.out.println("Please try another date or start time.");
            return;
        }

        StaffMember selectedVeterinarian =
                selectVeterinarian(availableVeterinarians);

        if (selectedVeterinarian == null) {
            System.out.println("Booking cancelled.");
            return;
        }

        displayBookingSummary(
                petId,
                selectedServices,
                totalDuration,
                totalAmount,
                start,
                end,
                reserved,
                selectedVeterinarian
        );

        System.out.print("\nConfirm this appointment? (Y/N): ");
        String confirmation = sc.nextLine().trim();

        if (!confirmation.equalsIgnoreCase("Y")) {
            System.out.println(
                    "Booking cancelled. No appointment was saved."
            );
            return;
        }

        appointmentDao.bookCombinedAppointment(
                owner.id(),
                petId,
                selectedVeterinarian.staffId(),
                date,
                start,
                end,
                reserved,
                totalDuration,
                BUFFER_MINUTES,
                totalAmount,
                selectedServices
        );
    }

    private int selectPet() {
        String sql = """
                SELECT pet_id, pet_name, species, breed
                FROM pets
                WHERE owner_id = ?
                  AND UPPER(status) = 'ACTIVE'
                ORDER BY pet_name
                """;

        List<Integer> petIds = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, owner.id());

            try (ResultSet rs = pstmt.executeQuery()) {
                System.out.println("\n========== MY REGISTERED PETS ==========");

                while (rs.next()) {
                    int id = rs.getInt("pet_id");
                    petIds.add(id);

                    System.out.printf(
                            "%d. %s | %s | %s%n",
                            petIds.size(),
                            rs.getString("pet_name"),
                            rs.getString("species"),
                            rs.getString("breed")
                    );
                }
            }

        } catch (SQLException e) {
            System.err.println("Error retrieving pets: " + e.getMessage());
            return -1;
        }

        if (petIds.isEmpty()) {
            System.out.println("You have no active registered pets.");
            System.out.println("Please register a pet first.");
            return -1;
        }

        System.out.print("Select a pet: ");

        try {
            int choice = Integer.parseInt(sc.nextLine().trim());

            if (choice < 1 || choice > petIds.size()) {
                System.out.println("Invalid pet selection.");
                return -1;
            }

            return petIds.get(choice - 1);

        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number.");
            return -1;
        }
    }


    private List<Service> selectServices() {
        List<Service> selectedServices = new ArrayList<>();
        List<Service> availableServices = serviceDao.getAvailableServices();

        if (availableServices.isEmpty()) {
            System.out.println("\nNo services are currently available.");
            return selectedServices;
        }

        while (true) {
            System.out.println("\n");
            System.out.println("==============================================================");
            System.out.println("                    AVAILABLE SERVICES");
            System.out.println("==============================================================");
            System.out.printf(
                    "%-5s %-24s %-16s %10s %12s%n",
                    "ID", "SERVICE NAME", "CATEGORY", "DURATION", "PRICE"
            );
            System.out.println("--------------------------------------------------------------");

            for (Service service : availableServices) {
                System.out.printf(
                        "%-5d %-24.24s %-16.16s %7d min %12.2f%n",
                        service.serviceId(),
                        service.serviceName(),
                        service.category(),
                        service.durationMinutes(),
                        service.price()
                );
            }

            System.out.println("==============================================================");

            if (!selectedServices.isEmpty()) {
                System.out.println("\nSelected Services:");

                for (Service service : selectedServices) {
                    System.out.printf(
                            "- %s | %d minutes | ₱%.2f%n",
                            service.serviceName(),
                            service.durationMinutes(),
                            service.price()
                    );
                }
            }

            System.out.print("\nEnter Service ID (0 to finish): ");

            int serviceId;

            try {
                serviceId = Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid service ID.");
                continue;
            }

            if (serviceId == 0) {
                break;
            }

            Service selected = null;

            for (Service service : availableServices) {
                if (service.serviceId() == serviceId) {
                    selected = service;
                    break;
                }
            }

            if (selected == null) {
                System.out.println("Service not found or unavailable.");
                continue;
            }

            int selectedServiceId = selected.serviceId();

            boolean alreadySelected = selectedServices.stream()
                    .anyMatch(s -> s.serviceId() == selectedServiceId);

            if (alreadySelected) {
                System.out.println("This service has already been selected.");
                continue;
            }

            selectedServices.add(selected);

            System.out.println(
                    "\n" + selected.serviceName() + " successfully added!"
            );

            System.out.print("Would you like to add another service? (Y/N): ");
            String answer = sc.nextLine().trim();

            if (!answer.equalsIgnoreCase("Y")) {
                break;
            }
        }

        return selectedServices;
    }


    private LocalDate readAppointmentDate() {
        while (true) {
            System.out.print("Enter preferred date (YYYY-MM-DD): ");
            String input = sc.nextLine().trim();

            try {
                LocalDate date = LocalDate.parse(input);

                if (date.isBefore(LocalDate.now())) {
                    System.out.println("You cannot select a past date.");
                    continue;
                }

                return date;

            } catch (DateTimeParseException e) {
                System.out.println("Invalid date. Use YYYY-MM-DD.");
            }
        }
    }

    private LocalTime readStartTime() {
        while (true) {
            System.out.print("Enter preferred start time (HH:mm): ");
            String input = sc.nextLine().trim();

            try {
                return LocalTime.parse(input, TIME_FORMAT);

            } catch (DateTimeParseException e) {
                System.out.println("Invalid time. Example: 09:30 or 14:00.");
            }
        }
    }

    private StaffMember selectVeterinarian(
            List<StaffMember> availableVeterinarians
    ) {
        System.out.println("\n========== AVAILABLE VETERINARIANS ==========");

        for (int i = 0; i < availableVeterinarians.size(); i++) {
            System.out.println(
                    (i + 1) + ". " + availableVeterinarians.get(i).fullName()
            );
        }

        System.out.println("0. Cancel");
        System.out.print("Select a veterinarian: ");

        try {
            int choice = Integer.parseInt(sc.nextLine().trim());

            if (choice == 0) {
                return null;
            }

            if (choice < 1 || choice > availableVeterinarians.size()) {
                System.out.println("Invalid selection.");
                return null;
            }

            return availableVeterinarians.get(choice - 1);

        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number.");
            return null;
        }
    }

    private void displayBookingSummary(
            int petId,
            List<Service> services,
            int totalDuration,
            double totalAmount,
            String start,
            String end,
            String reserved,
            StaffMember veterinarian
    ) {
        System.out.println("\n==========================================");
        System.out.println("          APPOINTMENT SUMMARY");
        System.out.println("==========================================");
        System.out.println("Pet ID: " + petId);
        System.out.println("\nSelected Services:");

        for (Service service : services) {
            System.out.printf(
                    "- %s | %d minutes | ₱%.2f%n",
                    service.serviceName(),
                    service.durationMinutes(),
                    service.price()
            );
        }

        System.out.println("------------------------------------------");
        System.out.printf("Total Service Duration: %d minutes%n", totalDuration);
        System.out.printf("Buffer Time: %d minutes%n", BUFFER_MINUTES);
        System.out.printf(
                "Total Reserved Time: %d minutes%n",
                totalDuration + BUFFER_MINUTES
        );
        System.out.printf("Total Price: ₱%.2f%n", totalAmount);
        System.out.println("Preferred Start Time: " + start);
        System.out.println("Estimated Service Completion: " + end);
        System.out.println("End of Reserved Slot: " + reserved);
        System.out.println("Veterinarian: " + veterinarian.fullName());
        System.out.println("==========================================");
    }

    private void viewMyAppointments() {
        String sql = """
                SELECT a.appointment_id,
                       a.appointment_date,
                       a.start_time,
                       a.end_time,
                       a.reserved_until,
                       a.status,
                       a.total_amount,
                       u.full_name AS veterinarian_name,
                       p.pet_name
                FROM appointments a
                INNER JOIN pets p ON a.pet_id = p.pet_id
                INNER JOIN users u ON a.staff_id = u.user_id
                WHERE p.owner_id = ?
                ORDER BY a.appointment_date DESC, a.start_time DESC
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, owner.id());

            try (ResultSet rs = pstmt.executeQuery()) {
                System.out.println("\n========== MY APPOINTMENTS ==========");

                boolean found = false;

                while (rs.next()) {
                    found = true;

                    int appointmentId = rs.getInt("appointment_id");

                    System.out.println("----------------------------------");
                    System.out.println("Appointment ID: " + appointmentId);
                    System.out.println("Pet: " + rs.getString("pet_name"));
                    System.out.println("Date: " + rs.getString("appointment_date"));
                    System.out.println("Start Time: " + rs.getString("start_time"));
                    System.out.println("Service Completion: " + rs.getString("end_time"));
                    System.out.println("Reserved Until: " + rs.getString("reserved_until"));
                    System.out.println("Veterinarian: " + rs.getString("veterinarian_name"));
                    System.out.printf("Total Amount: ₱%.2f%n", rs.getDouble("total_amount"));
                    System.out.println("Status: " + rs.getString("status"));

                    displayAppointmentServices(conn, appointmentId);
                }

                if (!found) {
                    System.out.println("You have no appointments yet.");
                }
            }

        } catch (SQLException e) {
            System.err.println(
                    "Error retrieving appointments: " + e.getMessage()
            );
        }
    }

    private void displayAppointmentServices(
            Connection conn,
            int appointmentId
    ) throws SQLException {

        String sql = """
                SELECT service_name_at_booking,
                       duration_minutes,
                       price_at_booking
                FROM appointment_services
                WHERE appointment_id = ?
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, appointmentId);

            try (ResultSet rs = pstmt.executeQuery()) {
                System.out.println("Services:");

                while (rs.next()) {
                    System.out.printf(
                            "  - %s | %d minutes | ₱%.2f%n",
                            rs.getString("service_name_at_booking"),
                            rs.getInt("duration_minutes"),
                            rs.getDouble("price_at_booking")
                    );
                }
            }
        }
    }
}
