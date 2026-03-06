package edu.iiitd.dbms.data_access;

import edu.iiitd.dbms.config.ServerConnector;
import edu.iiitd.dbms.domain.Admin;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AdminDAO {
    private Admin matchAdminColumns(ResultSet rs) throws SQLException {
        return new Admin(
                rs.getInt("admin_id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("role")
        );
    }

    public List<Admin> listAdmins() throws SQLException {
        String sqlQuery = """
            SELECT admin_id, name, email, role
            FROM admin
            ORDER BY admin_id
        """;
        List<Admin> adminList = new ArrayList<>();
        try (Connection connect = ServerConnector.DBConnection();
             PreparedStatement ps = connect.prepareStatement(sqlQuery);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                adminList.add(matchAdminColumns(rs));
            }
        }
        return adminList;
    }

    public Admin findByAdminId(int adminId) throws SQLException {
        String sqlQuery = """
            SELECT admin_id, name, email, role
            FROM admin
            WHERE admin_id = ?
        """;
        try (Connection connect = ServerConnector.DBConnection();
             PreparedStatement ps = connect.prepareStatement(sqlQuery)) {
            ps.setInt(1, adminId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return matchAdminColumns(rs);
                }
                return null;
            }
        }
    }

    public List<Admin> findByAdminRole(String role) throws SQLException {
        String sqlQuery = """
            SELECT admin_id, name, email, role
            FROM admin
            WHERE role = ?
            ORDER BY name
        """;
        List<Admin> admins = new ArrayList<>();
        try (Connection connect = ServerConnector.DBConnection();
             PreparedStatement ps = connect.prepareStatement(sqlQuery)) {
            ps.setString(1, role);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    admins.add(matchAdminColumns(rs));
                }
            }
        }
        return admins;
    }

    public void insertAdmin(Admin admin) throws SQLException {
        String sqlQuery = """
            INSERT INTO admin (admin_id, name, email, role)
            VALUES (?, ?, ?, ?)
        """;
        try (Connection connect = ServerConnector.DBConnection();
             PreparedStatement ps = connect.prepareStatement(sqlQuery)) {
            ps.setInt(1, admin.getAdminId());
            ps.setString(2, admin.getName());
            ps.setString(3, admin.getEmail());
            ps.setString(4, admin.getRole());
            ps.executeUpdate();
        }
    }

    public void updateAdminRole(int adminId, String newRole) throws SQLException {
        String sqlQuery = "UPDATE admin SET role = ? WHERE admin_id = ?";
        try (Connection connect = ServerConnector.DBConnection();
             PreparedStatement ps = connect.prepareStatement(sqlQuery)) {
            ps.setString(1, newRole);
            ps.setInt(2, adminId);
            ps.executeUpdate();
        }
    }

    public void deleteAdmin(int adminId) throws SQLException {
        String sqlQuery = "DELETE FROM admin WHERE admin_id = ?";
        try (Connection connect = ServerConnector.DBConnection();
             PreparedStatement ps = connect.prepareStatement(sqlQuery)) {
            ps.setInt(1, adminId);
            ps.executeUpdate();
        }
    }
}
