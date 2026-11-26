
package com.scc.dao;

import com.scc.model.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ServiceDao {

    // View services that customers can book.
    public List<Service> getAvailableServices() {
        List<Service> services = new ArrayList<>();

        String sql = """
                SELECT service_id, service_name, category,
                       duration_minutes, description, price
                FROM services
                WHERE is_available = 1
                ORDER BY category, service_name
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                services.add(mapService(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error retrieving services: " + e.getMessage());
        }

        return services;
    }

    // View all services, including unavailable services.
    public List<Service> getAllServices() {
        List<Service> services = new ArrayList<>();

        String sql = """
                SELECT service_id, service_name, category,
                       duration_minutes, description, price
                FROM services
                ORDER BY category, service_name
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                services.add(mapService(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error retrieving all services: " + e.getMessage());
        }

        return services;
    }

    public Service getServiceById(int serviceId) {
        String sql = """
                SELECT service_id, service_name, category,
                       duration_minutes, description, price
                FROM services
                WHERE service_id = ? AND is_available = 1
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, serviceId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapService(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error retrieving service: " + e.getMessage());
        }

        return null;
    }

    // Add a new service.
    public boolean addService(
            String name,
            String description,
            double price,
            String category,
            int duration
    ) {
        String sql = """
                INSERT INTO services (
                    service_name, description, price,
                    availability, category, duration_minutes,
                    is_available
                )
                VALUES (?, ?, ?, 'AVAILABLE', ?, ?, 1)
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            pstmt.setString(2, description);
            pstmt.setDouble(3, price);
            pstmt.setString(4, category);
            pstmt.setInt(5, duration);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error adding service: " + e.getMessage());
            return false;
        }
    }

    // Update service information.
    public boolean updateService(
            int serviceId,
            String name,
            String description,
            String category,
            int duration
    ) {
        String sql = """
                UPDATE services
                SET service_name = ?,
                    description = ?,
                    category = ?,
                    duration_minutes = ?
                WHERE service_id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            pstmt.setString(2, description);
            pstmt.setString(3, category);
            pstmt.setInt(4, duration);
            pstmt.setInt(5, serviceId);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error updating service: " + e.getMessage());
            return false;
        }
    }

    // Change the price of a service.
    public boolean updateServicePrice(int serviceId, double price) {
        String sql = """
                UPDATE services
                SET price = ?
                WHERE service_id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, price);
            pstmt.setInt(2, serviceId);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error updating service price: " + e.getMessage());
            return false;
        }
    }

    // Set service availability.
    public boolean updateAvailability(int serviceId, boolean available) {
        String status = available ? "AVAILABLE" : "UNAVAILABLE";

        String sql = """
                UPDATE services
                SET is_available = ?,
                    availability = ?
                WHERE service_id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, available ? 1 : 0);
            pstmt.setString(2, status);
            pstmt.setInt(3, serviceId);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error changing availability: " + e.getMessage());
            return false;
        }
    }

    // Deactivate a service instead of permanently deleting it.
    public boolean deactivateService(int serviceId) {
        return updateAvailability(serviceId, false);
    }

    private Service mapService(ResultSet rs) throws SQLException {
        return new Service(
                rs.getInt("service_id"),
                rs.getString("service_name"),
                rs.getString("category"),
                rs.getInt("duration_minutes"),
                rs.getString("description"),
                rs.getDouble("price")
        );
    }
}
