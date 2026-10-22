
package com.scc.dao;

import com.scc.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    public UserDao() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            defaultAdminGenerator(conn);
        } catch (SQLException e) {
            System.err.println("Error seeding default admin: " + e.getMessage());
        }
    }

    // CREATE DEFAULT ADMIN ACCOUNT
    public void defaultAdminGenerator(Connection conn) {
        String checkSql = "SELECT COUNT(*) FROM users WHERE role = 'ADMIN'";
        String insertSql = "INSERT INTO users(username, password, role, status) "
                + "VALUES('admin', 'admin123', 'ADMIN', 'APPROVED')";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(checkSql)) {

            if (rs.next() && rs.getInt(1) == 0) {
                stmt.executeUpdate(insertSql);
                System.out.println("-> Default Admin created!");
            }

        } catch (SQLException e) {
            System.err.println("Error seeding default admin: " + e.getMessage());
        }
    }

    // REGISTER NEW USER
    public boolean registerUser(String username, String password, String role) {
        String sql = "INSERT INTO users(username, password, role, status) "
                + "VALUES(?, ?, ?, 'PENDING')";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, role.toUpperCase());

            pstmt.executeUpdate();

            System.out.println("Successful registration. Waiting for approval!");
            return true;

        } catch (SQLException e) {
            System.err.println("Failed to register user: " + e.getMessage());
            return false;
        }
    }

    // LOGIN
    public User login(String username, String password) {
        String sql = "SELECT id, username, password, role, status "
                + "FROM users WHERE username = ? AND password = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String status = rs.getString("status");

                    if (!"APPROVED".equalsIgnoreCase(status)) {
                        System.out.println(
                                "Account is " + status
                                        + ". Please contact the administrator."
                        );
                        return null;
                    }

                    return new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("role"),
                            status
                    );
                } else {
                    System.out.println("Account not found or incorrect credentials.");
                }
            }

        } catch (SQLException e) {
            System.err.println("Error during login: " + e.getMessage());
        }

        return null;
    }

    // VIEW ALL USERS
    public List<User> getAllUser() {
        List<User> userList = new ArrayList<>();

        String sql = "SELECT id, username, password, role, status "
                + "FROM users ORDER BY id";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                userList.add(new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("role"),
                        rs.getString("status")
                ));
            }

        } catch (SQLException e) {
            System.err.println("Error fetching all users: " + e.getMessage());
        }

        return userList;
    }



public List<User> getPendingUser(){
    List<User> pendingList = new ArrayList<>();
    String sql = "SELECT * FROM users WHERE status = 'PENDING'";

        try(Connection conn = DatabaseConnection.getConnection()){
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while(rs.next()){
                pendingList.add(new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("role"),
                        rs.getString("status")
                        ));

                }
            } catch (SQLException e){
                System.out.println("Error fetching users: "+e.getMessage());
            }
            return pendingList;



        }

        public boolean approveUser(int userId) {
            String sql = "UPDATE users SET status = 'APPROVED' WHERE id = ?";

            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)){

                pstmt.setInt(1, userId);
                int rowsUpdated = pstmt.executeUpdate();

                if(rowsUpdated > 0){
                    System.out.println("-> User ID: "+userId+" approved successfully");
                    return true;
                }else{
                    System.out.println("-> ID not found.");
                }

            } catch (Exception e){
                System.out.println("Error approving users: "+e.getMessage());
            }
            return false;
        }







}



