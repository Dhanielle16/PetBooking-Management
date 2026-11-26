package com.scc.dao;

import com.scc.model.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ServiceDao {

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
                services.add(new Service(
                        rs.getInt("service_id"),
                        rs.getString("service_name"),
                        rs.getString("category"),
                        rs.getInt("duration_minutes"),
                        rs.getString("description"),
                        rs.getDouble("price")
                ));
            }

        } catch (SQLException e) {
            System.err.println("Error retrieving services: " + e.getMessage());
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

        } catch (SQLException e) {
            System.err.println("Error retrieving service: " + e.getMessage());
        }

        return null;
    }
}