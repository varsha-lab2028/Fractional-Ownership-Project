package edu.iiitd.dbms.ui.admin;

public class AdminControlCenter extends AdminDashUI {
    public AdminControlCenter() { super(); setTitle("Fractional. - Admin Control Center"); }
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> new AdminControlCenter().setVisible(true));
    }
}
