package com.scc;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.Scanner;

public class Main {
    private static final Scanner sc = new Scanner(System.in);
    private static final UserDao userDao = new UserDao();

    public static void main(String[] args){

        System.out.println("==========================================");
        System.out.println("  PawSync: Pet Care Booking & Management");
        System.out.println("==========================================");
        System.out.print("\n1. Login");
        System.out.print("\n2. Register");
        System.out.print("\n3. Exit");
        System.out.print("\nChose an option: ");
        String choice = sc.nextLine();

        switch(choice){
            case "1":
                break;
            case "2":
                System.out.print("\nEnter username: ");
                String user = sc.nextLine();
                System.out.print("\nEnter password: ");
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

}