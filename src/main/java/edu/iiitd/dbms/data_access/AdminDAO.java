package edu.iiitd.dbms.data_access;

import edu.iiitd.dbms.config.ServerConnector;
import edu.iiitd.dbms.domain.Admin;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AdminDAO {

    public List<Admin> listAdmins() throws SQLException {
        String sql = """
            SELECT admin_id, name, email, role
            FROM admin
            ORDER BY admin_id
        """;
        List<Admin> admins = new ArrayList<>();
        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                admins.add(mapAdmin(rs));
            }
        }
        return admins;
    }

    public Admin findById(int adminId) throws SQLException {

        String sql = """
            SELECT admin_id, name, email, role
            FROM admin
            WHERE admin_id = ?
        """;

        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, adminId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapAdmin(rs);
                }
                return null;
            }
        }
    }

    public List<Admin> findByRole(String role) throws SQLException {

        String sql = """
            SELECT admin_id, name, email, role
            FROM admin
            WHERE role = ?
            ORDER BY name
        """;

        List<Admin> admins = new ArrayList<>();

        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, role);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    admins.add(mapAdmin(rs));
                }
            }
        }

        return admins;
    }

    public void insertAdmin(Admin admin) throws SQLException {

        String sql = """
            INSERT INTO admin (admin_id, name, email, role)
            VALUES (?, ?, ?, ?)
        """;

        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, admin.getAdminId());
            ps.setString(2, admin.getName());
            ps.setString(3, admin.getEmail());
            ps.setString(4, admin.getRole());

            ps.executeUpdate();
        }
    }

    public void updateRole(int adminId, String newRole) throws SQLException {

        String sql = "UPDATE admin SET role = ? WHERE admin_id = ?";

        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newRole);
            ps.setInt(2, adminId);

            ps.executeUpdate();
        }
    }

    public void deleteAdmin(int adminId) throws SQLException {

        String sql = "DELETE FROM admin WHERE admin_id = ?";

        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, adminId);
            ps.executeUpdate();
        }
    }

    private Admin mapAdmin(ResultSet rs) throws SQLException {

        return new Admin(
                rs.getInt("admin_id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("role")
        );
    }
}
