package com.scc.model;

public record StaffMember(
        int staffId,
        String fullName
) {
    @Override
    public String toString() {
        return "[" + staffId + "] " + fullName;
    }
}