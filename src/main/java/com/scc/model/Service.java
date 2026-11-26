package com.scc.model;

public record Service(
        int serviceId,
        String serviceName,
        String category,
        int durationMinutes,
        String description,
        double price
) {
    @Override
    public String toString() {
        return String.format(
                "[%d] %s | %s | %d minutes | ₱%.2f",
                serviceId,
                serviceName,
                category,
                durationMinutes,
                price
        );
    }
}