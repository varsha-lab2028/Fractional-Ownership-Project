package edu.iiitd.dbms;

import edu.iiitd.dbms.ui.login.logintesterUI;

import javax.swing.*;

/**
 * Main entry point for the Fractional Ownership Marketplace.
 *
 * Flow:
 *  1. Opens the Login screen (logintesterUI)
 *  2. On successful authentication, LoginManager stores the session
 *  3. Based on user_type (INVESTOR or ADMIN), routes to the correct dashboard:
 *       INVESTOR → InvestorDashUI  (with investor's own data)
 *       ADMIN    → AdminDashUI_updated  (platform overview)
 *
 * Credentials:
 *   Investors: aman@gmail.com / aman123, riya@gmail.com / riya123, ...
 *   Admins:    ananya@platform.com / ananya123, raghav@platform.com / raghav123, ...
 */
public class Main {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            System.out.println("=== Fractional Ownership Marketplace ===");
            System.out.println("Starting application...");
            new logintesterUI().setVisible(true);
        });
    }
}
