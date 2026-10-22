package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.Scanner;

public class Authentication {

    private final UserDao userDao;
    private final Scanner sc;

    public Authentication(UserDao userDao, Scanner sc){
        this.userDao = userDao;
        this.sc = sc;

    }


        public void start(){
            System.out.println("==========================================");
            System.out.println("  PawSync: Pet Care Booking & Management");
            System.out.println("==========================================");
            System.out.print("1. Login\n");
            System.out.print("2. Register\n");
            System.out.print("3. Exit\n");
            System.out.print("Chose an option: ");
            String choice = sc.nextLine();

            switch(choice){
                case "1":
                    handleLogin();

                    break;
                case "2":
                    System.out.print("Enter username: ");
                    String user = sc.nextLine();
                    System.out.print("Enter password: ");
                    String pass = sc.nextLine();

                    System.out.print("Select Role [1] PET OWNER | [2] RECEPTIONIST | [3] VETERINARIAN: ");
                    int roleChoice = sc.nextInt();
                    String role;
                    switch(roleChoice){
                        case 1:
                            role = "PET OWNER";
                            break;
                        case 2:
                            role = "RECEPTIONIST";
                            break;
                        case 3:
                            role = "VETERINARIAN";
                            break;
                        default:
                            role = null;



                    }

                    if(role != null){
                        userDao.registerUser(user, pass, role);

                    }else{

                        System.out.println("Invalid role selection!");
                    }
                    break;



            }


        }
    private void handleLogin(){
        System.out.print("Enter username: ");
        String username = sc.nextLine();
        System.out.print("Enter password: ");
        String password = sc.nextLine();

        User user = userDao.login(username, password);

        if(user != null){
            System.out.println("\n > Welcome " +user.username()+" | ROLE: "+user.role()+"");
            switch (user.role()){
                case "ADMIN" -> new AdminMenu(userDao, sc).display(user);
                default -> System.out.println("Account Role not found!");

            }

        }


    }


}
