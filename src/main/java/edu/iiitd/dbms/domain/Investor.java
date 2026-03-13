package edu.iiitd.dbms.domain;

import java.time.LocalDate;

public class Investor {
    private int investorId;
    private String name;
    private String email;
    private String phone; // Added phone field
    private LocalDate registrationDate;

    // Updated Constructor to accept 5 parameters
    public Investor(int investorId, String name, String email, String phone, LocalDate registrationDate) {
        this.investorId = investorId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.registrationDate = registrationDate;
    }

    // --- GETTERS ---
    public int getInvestorId() {
        return investorId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public LocalDate getRegistrationDate() {
        return registrationDate;
    }

    // --- SETTERS ---
    public void setInvestorId(int investorId) {
        this.investorId = investorId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setRegistrationDate(LocalDate registrationDate) {
        this.registrationDate = registrationDate;
    }

    @Override
    public String toString() {
        return "Investor{" +
                "investorId=" + investorId +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", registrationDate=" + registrationDate +
                '}';
    }
}