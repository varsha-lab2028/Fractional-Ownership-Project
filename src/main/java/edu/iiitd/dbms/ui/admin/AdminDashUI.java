package edu.iiitd.dbms.ui.admin;
// Redirect to the updated version
public class AdminDashUI extends AdminDashUI_updated {
    public AdminDashUI() { super(); setTitle("Fractional. - Admin Control Center"); }
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> new AdminDashUI().setVisible(true));
    }
}
