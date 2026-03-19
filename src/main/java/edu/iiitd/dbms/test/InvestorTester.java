package edu.iiitd.dbms.test;

import edu.iiitd.dbms.ui.login.loginUI;
import javax.swing.SwingUtilities;

public class InvestorTester {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            System.out.println("Initializing Fractional Application Flow...");
            new loginUI().setVisible(true);
        });
    }
}