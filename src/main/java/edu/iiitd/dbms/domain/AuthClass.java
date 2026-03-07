package edu.iiitd.dbms.domain;

import java.time.LocalDateTime;

public class AuthClass {
    private int authId;
    private int linkedId;
    private String name;
    private String email;
    private String userType;
    private String authStatus;
    private String passwordHash;
    private LocalDateTime lastLogin;
    //constructor
    public AuthClass() {}

    public int getAuthId() {
        return authId;
    }
    public void setAuthId(int authId) {
        this.authId = authId;
    }

    public int getLinkedId() {
        return linkedId;
    }
    public void setLinkedId(int linkedId) {
        this.linkedId = linkedId;
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

    public String getUserType() {
        return userType;
    }
    public void setUserType(String userType) {
        this.userType = userType;
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
