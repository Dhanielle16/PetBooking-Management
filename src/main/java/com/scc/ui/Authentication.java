package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.Scanner;

public class Authentication {

    private final UserDao userDao;
    private final ActivityLogDao activityLogDao = new ActivityLogDao();
    private final Scanner sc;

    public Authentication(UserDao userDao, Scanner sc){
        this.userDao = userDao;
        this.sc = sc;
    }

    public void start(){
        while(true) {
            System.out.println("\n==========================================");
            System.out.println("  PawSync: Pet Care Booking & Management");
            System.out.println("==========================================");
            System.out.print("1. Login\n");
            System.out.print("2. Register\n");
            System.out.print("3. Exit\n");
            System.out.print("Choose an option: ");
            String choice = sc.nextLine().trim();

            switch(choice){
                case "1":
                    handleLogin();
                    break;
                case "2":
                    System.out.print("Enter Full Name: ");
                    String fullName = sc.nextLine();
                    System.out.print("Enter Email Address: ");
                    String email = sc.nextLine();
                    System.out.print("Enter Password: ");
                    String pass = sc.nextLine();
                    System.out.print("Enter Phone Number: ");
                    String phone = sc.nextLine();

                    System.out.print("Select Role [1] PET OWNER | [2] RECEPTIONIST | [3] VETERINARIAN: ");
                    String roleInput = sc.nextLine().trim();
                    String role;
                    switch(roleInput){
                        case "1" -> role = "PET OWNER";
                        case "2" -> role = "RECEPTIONIST";
                        case "3" -> role = "VETERINARIAN";
                        default -> role = null;
                    }

                    if(role != null){
                        userDao.registerUser(fullName, email, pass, phone, role);
                    }else{
                        System.out.println("-> [ERROR] Invalid role selection!");
                    }
                    break;
                case "3":
                    System.out.println("Exiting PawSync. Goodbye!");
                    return;
                default:
                    System.out.println("-> [ERROR] Invalid option.");
            }
        }
    }

    private void handleLogin(){
        System.out.print("Enter Email or Full Name: ");
        String identifier = sc.nextLine();
        System.out.print("Enter Password: ");
        String password = sc.nextLine();

        User user = userDao.login(identifier, password);

        if(user != null){
            // Automatically log login action
            activityLogDao.logAction(user.id(), "USER_LOGIN", "User " + user.fullName() + " (ID: " + user.id() + ") logged in.");

            System.out.println("\n > Welcome " + user.fullName() + " | ROLE: " + user.role());

            // Route user based on their role
            switch (user.role()){
                case "ADMIN" -> new AdminMenu(userDao, sc).display(user);
                case "PET OWNER" -> new PetOwnerMenu(user, sc).display();
                case "RECEPTIONIST" -> new ReceptionistMenu(user, sc).display();
                case "VETERINARIAN" -> new VeterinarianMenu(user, sc).display();
                default -> System.out.println("-> [ERROR] Account role not recognized.");
            }

            // Automatically log logout action after exiting the dashboard
            activityLogDao.logAction(user.id(), "USER_LOGOUT", "User " + user.fullName() + " (ID: " + user.id() + ") logged out.");
        }
    }
}