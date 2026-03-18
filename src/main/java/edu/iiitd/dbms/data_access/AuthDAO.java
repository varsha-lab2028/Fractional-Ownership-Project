package edu.iiitd.dbms.data_access;

import edu.iiitd.dbms.config.ServerConnector;
import edu.iiitd.dbms.domain.AuthClass;

import java.sql.*;

public class AuthDAO {

    /**
     * Look up a user by email in the user_auth table.
     * Returns null if not found.
     */
    public AuthClass findByEmail(String email) throws SQLException {
        String sql = """
            SELECT ua.auth_id,
                   ua.email,
                   ua.password_hash,
                   ua.user_type,
                   ua.linked_id,
                   ua.status,
                   ua.last_login,
                   CASE ua.user_type
                       WHEN 'INVESTOR' THEN i.investor_name
                       WHEN 'ADMIN'    THEN a.name
                   END AS display_name
            FROM user_auth ua
            LEFT JOIN INVESTOR i ON ua.user_type = 'INVESTOR' AND ua.linked_id = i.investor_id
            LEFT JOIN ADMIN    a ON ua.user_type = 'ADMIN'    AND ua.linked_id = a.admin_id
            WHERE ua.email = ?
        """;

        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    AuthClass user = new AuthClass();
                    user.setAuthId(rs.getInt("auth_id"));
                    user.setName(rs.getString("display_name"));
                    user.setEmail(rs.getString("email"));
                    user.setPasswordHash(rs.getString("password_hash"));
                    user.setUserType(rs.getString("user_type"));
                    user.setLinkedId(rs.getInt("linked_id"));
                    user.setAuthStatus(rs.getString("status"));

                    Timestamp ts = rs.getTimestamp("last_login");
                    if (ts != null) {
                        user.setLastLogin(ts.toLocalDateTime());
                    }
                    return user;
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
