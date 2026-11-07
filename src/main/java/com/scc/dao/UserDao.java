package com.scc.dao;

import com.scc.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    private final ActivityLogDao logDao = new ActivityLogDao();

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
        String insertSql = "INSERT INTO users(full_name, email, password, phone_number, role, status) "
                + "VALUES('System Administrator', 'admin@pawsync.com', 'admin123', '09123456789', 'ADMIN', 'APPROVED')";

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

    // REGISTER NEW USER WITH FULL ARCHITECTURE COLUMNS
    public boolean registerUser(String fullName, String email, String password, String phoneNumber, String role) {
        String sql = "INSERT INTO users(full_name, email, password, phone_number, role, status) "
                + "VALUES(?, ?, ?, ?, ?, 'PENDING')";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, fullName);
            pstmt.setString(2, email);
            pstmt.setString(3, password);
            pstmt.setString(4, phoneNumber);
            pstmt.setString(5, role.toUpperCase());

            pstmt.executeUpdate();

            System.out.println("-> Successful registration. Waiting for administrator approval!");
            return true;

        } catch (SQLException e) {
            System.err.println("Failed to register user: " + e.getMessage());
            return false;
        }
    }

    // LOGIN BY EMAIL OR FULL NAME
    public User login(String identifier, String password) {
        String sql = "SELECT id, full_name, email, password, phone_number, role, status "
                + "FROM users WHERE (email = ? OR full_name = ?) AND password = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, identifier);
            pstmt.setString(2, identifier);
            pstmt.setString(3, password);

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
                            rs.getString("full_name"),
                            rs.getString("email"),
                            rs.getString("password"),
                            rs.getString("phone_number"),
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

        String sql = "SELECT id, full_name, email, password, phone_number, role, status "
                + "FROM users ORDER BY id";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                userList.add(new User(
                        rs.getInt("id"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getString("phone_number"),
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

        try(Connection conn = DatabaseConnection.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)){

            while(rs.next()){
                pendingList.add(new User(
                        rs.getInt("id"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getString("phone_number"),
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
                logDao.logAction(1, "USER_APPROVED", "Admin approved account for User ID " + userId);
                return true;
            }else{
                System.out.println("-> ID not found.");
            }

        } catch (Exception e){
            System.out.println("Error approving users: "+e.getMessage());
        }
        return false;
    }

    public boolean updateUserAndStatus(int userId, String newrole, String newStatus){
        String sql = "UPDATE users SET role = ?, status = ? WHERE id = ?";

        try(Connection conn = DatabaseConnection.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, newrole.toUpperCase());
            pstmt.setString(2, newStatus.toUpperCase());
            pstmt.setInt(3, userId);

            int rowsUpdated = pstmt.executeUpdate();
            if(rowsUpdated > 0){
                System.out.println("-> User Id: "+userId+ " updated successfully!");
                logDao.logAction(1, "USER_UPDATED", "Admin changed role of User " + userId + " to " + newrole.toUpperCase());
                return true;
            }else{
                System.out.println("-> User ID not found.");
            }
            return false;

        } catch (SQLException e) {
            System.err.println("Error updating user: "+e.getMessage());
        }
        return false;
    }
}