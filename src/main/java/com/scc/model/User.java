package com.scc.model;

public record User(int id, String fullName, String email, String password, String phoneNumber, String role, String status) {
    @Override
    public String toString(){
        return String.format("[%d] %s (%s) - Status: %s", id, fullName, role, status);
    }
}