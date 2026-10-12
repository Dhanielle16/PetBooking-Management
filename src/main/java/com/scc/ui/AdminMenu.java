package com.scc.ui;
import com.scc.dao.UserDao;
import com.scc.model.User;
import java.util.Scanner;

public class AdminMenu {
    private final UserDao userDao;
    private final Scanner sc;



    public AdminMenu(UserDao userDao, Scanner sc){
        this.userDao = userDao;
        this.sc = sc;
    }



    public void display(User admin){
        boolean loggedIn = true;
        while(loggedIn){
            System.out.println("\n==========================================");
            System.out.println("             ADMIN DASHBOARD              ");
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


        }


    }


}
