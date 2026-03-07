package edu.iiitd.dbms.auth;

import edu.iiitd.dbms.domain.AuthClass;

public class TestingAuth {
    public static void main(String[] args) {
        AuthenticationService authService = new AuthenticationService();
        System.out.println("TEST 1: Correct Investor Login");
        try {
            AuthClass investor = authService.login("aman@gmail.com", "aman123");
            System.out.println("Login successful");
            System.out.println(investor.getName() + " | " + investor.getUserType());
            authService.logout();
        } catch (Exception e) {
            System.out.println("Login failed: " + e.getMessage());
        }

        System.out.println("\nTEST 2: Correct Admin Login");
        try {
            AuthClass admin = authService.login("ananya@platform.com", "ananya123");
            System.out.println("Login successful");
            System.out.println(admin.getName() + " | " + admin.getUserType());
            authService.logout();
        } catch (Exception e) {
            System.out.println("Login failed: " + e.getMessage());
        }

        System.out.println("\nTEST 3: Wrong Password");
        try {
            AuthClass wrong = authService.login("aman@gmail.com", "wrong123");
            System.out.println("Login successful");
            System.out.println(wrong.getName() + " | " + wrong.getUserType());
            authService.logout();
        } catch (Exception e) {
            System.out.println("Login failed: " + e.getMessage());
        }

        System.out.println("\nTEST 4: Non-existent Email");
        try {
            AuthClass fake = authService.login("fake@gmail.com", "fake123");
            System.out.println("Login successful");
            System.out.println(fake.getName() + " | " + fake.getUserType());
            authService.logout();
        } catch (Exception e) {
            System.out.println("Login failed: " + e.getMessage());
        }
    }
}
