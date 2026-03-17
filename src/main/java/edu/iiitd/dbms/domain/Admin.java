package edu.iiitd.dbms.domain;

import java.util.*;

public class Admin {
    private final int adminId;
    private final String name;
    private final String email;
    private final String role;

    public Admin(int adminId, String name, String email, String role) {
        if (adminId <= 0) {
            throw new IllegalArgumentException("adminId must be positive");
        }
        if (email != null && email.isBlank()) {
            throw new IllegalArgumentException("email cannot be blank");
        }
        this.adminId = adminId;
        this.name = name;
        this.email = email;
        this.role = role;
    }
    //getters
    public int getAdminId() {
        return adminId;
    }
    public String getName() {
        return name;
    }
    public String getEmail() {
        return email;
    }
    public String getRole() {
        return role;
    }

    @Override
    public String toString() {
        return "Admin{" +
                "adminId=" + adminId +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}


