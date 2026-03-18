package edu.iiitd.dbms.data_access;

import edu.iiitd.dbms.config.ServerConnector;
import edu.iiitd.dbms.domain.AuthClass;

import java.sql.*;

public class AuthDAO {

    /**
     * Look up a user by email in the user_auth table.
     * Two-step: first fetch auth row, then fetch display name separately.
     * This avoids CASE/JOIN column ambiguity on TiDB and other MySQL-compatible DBs.
     */
    public AuthClass findByEmail(String email) throws SQLException {
        String authSql = """
            SELECT auth_id, email, password_hash, user_type, linked_id, status, last_login
            FROM user_auth
            WHERE email = ?
        """;

        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement stmt = conn.prepareStatement(authSql)) {

            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) return null;

                AuthClass user = new AuthClass();
                user.setAuthId(rs.getInt("auth_id"));
                user.setEmail(rs.getString("email"));
                user.setPasswordHash(rs.getString("password_hash"));
                user.setUserType(rs.getString("user_type"));
                user.setLinkedId(rs.getInt("linked_id"));
                user.setAuthStatus(rs.getString("status"));

                Timestamp ts = rs.getTimestamp("last_login");
                if (ts != null) user.setLastLogin(ts.toLocalDateTime());

                // Fetch display name from the appropriate profile table
                String name = fetchDisplayName(conn, user.getUserType(), user.getLinkedId());
                user.setName(name != null ? name : user.getEmail());

                return user;
            }
        }
    }

    /**
     * Fetch the human-readable name from either INVESTOR or ADMIN table.
     * Tries both possible column names (investor_name and name) for compatibility.
     */
    private String fetchDisplayName(Connection conn, String userType, int linkedId) {
        if ("INVESTOR".equalsIgnoreCase(userType)) {
            // Try investor_name first, then name as fallback
            for (String col : new String[]{"investor_name", "name"}) {
                try {
                    String sql = "SELECT " + col + " FROM investor WHERE investor_id = ?";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, linkedId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                String val = rs.getString(1);
                                if (val != null && !val.isBlank()) return val;
                            }
                        }
                    }
                } catch (SQLException ignored) {
                    // Column doesn't exist, try the next one
                }
            }
        } else if ("ADMIN".equalsIgnoreCase(userType)) {
            // Try name first, then admin_name as fallback
            for (String col : new String[]{"name", "admin_name"}) {
                try {
                    String sql = "SELECT " + col + " FROM admin WHERE admin_id = ?";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, linkedId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                String val = rs.getString(1);
                                if (val != null && !val.isBlank()) return val;
                            }
                        }
                    }
                } catch (SQLException ignored) {
                    // Column doesn't exist, try the next one
                }
            }
        }
        return null;
    }

    /**
     * Stamp the last_login timestamp in user_auth whenever a user successfully logs in.
     */
    public void updateLastLogin(int authId) throws SQLException {
        String sql = "UPDATE user_auth SET last_login = NOW() WHERE auth_id = ?";
        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, authId);
            stmt.executeUpdate();
        }
    }
}
