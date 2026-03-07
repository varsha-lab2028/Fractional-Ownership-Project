package edu.iiitd.dbms.auth;

import java.time.LocalDateTime;

public class AuthenticatingUser {
    private int userId;
    private String name;
    private String email;
    private String role;          //shows admin or investor
    private String authStatus;    //show whether active/disabled/frozen
    private String passwordHash;
    private LocalDateTime lastLogin;

    public AuthenticatingUser(int userId, String name, String email, String role,
                              String authStatus, String passwordHash, LocalDateTime lastLogin){
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.authStatus = authStatus;
        this.passwordHash = passwordHash;
        this.lastLogin = lastLogin;
    }

    //getters and setters
    public int getUserId() {
        return userId;
    }
    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }
    public void setRole(String role) {
        this.role = role;
    }

    public String getAuthStatus() {
        return authStatus;
    }
    public void setAuthStatus(String authStatus) {
        this.authStatus = authStatus;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }
    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }
}
