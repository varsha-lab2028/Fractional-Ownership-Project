package edu.iiitd.dbms.data_access;

import edu.iiitd.dbms.config.DBConnection;
import edu.iiitd.dbms.config.ServerConnector;
import edu.iiitd.dbms.domain.AuthClass;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class AuthDAO {
    public AuthClass findByEmail(String email) throws SQLException {
        String sql = """
                SELECT auth_id, email, password_hash, user_type, linked_id, status, last_login
                FROM user_auth
                WHERE email = ?
                """;
        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                AuthClass user = new AuthClass();
                user.setAuthId(rs.getInt("auth_id"));
                user.setEmail(rs.getString("email"));
                user.setPasswordHash(rs.getString("password_hash"));
                user.setUserType(rs.getString("user_type"));
                user.setLinkedId(rs.getInt("linked_id"));
                user.setAuthStatus(rs.getString("status"));

                Timestamp ts = rs.getTimestamp("last_login");
                if (ts != null) {
                    user.setLastLogin(ts.toLocalDateTime());
                }

                user.setName(fetchName(conn, user.getUserType(), user.getLinkedId()));
                return user;
            }
        }
    }

    private String fetchName(Connection conn, String userType, int linkedId) throws SQLException {
        String sql;

        if ("ADMIN".equalsIgnoreCase(userType)) {
            sql = "SELECT name FROM admin WHERE admin_id = ?";
        } else {
            sql = "SELECT name FROM investor WHERE investor_id = ?";
        }

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, linkedId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("name");
                }
            }
        }
        return null;
    }

    public void updateLastLogin(int authId) throws SQLException {
        String sql = "UPDATE user_auth SET last_login = NOW() WHERE auth_id = ?";

        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, authId);
            stmt.executeUpdate();
        }
    }
}
