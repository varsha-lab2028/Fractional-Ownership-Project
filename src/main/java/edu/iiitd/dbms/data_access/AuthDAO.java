package edu.iiitd.dbms.data_access;

import edu.iiitd.dbms.config.ServerConnector;
import edu.iiitd.dbms.domain.AuthClass;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class AuthDAO {

    /**
     * Finds a user by email by checking both investor and admin tables.
     * This aligns with the seeded data currently in the database.
     */
    public AuthClass findByEmail(String email) throws SQLException {
        // 1. Try finding in the Investor table first
        AuthClass user = findInTable(email, "investor", "investor_id", "INVESTOR");
        
        // 2. If not found, try the Admin table
        if (user == null) {
            user = findInTable(email, "admin", "admin_id", "ADMIN");
        }
        
        return user;
    }

    private AuthClass findInTable(String email, String tableName, String idColumn, String type) throws SQLException {
        // Dynamically query the table where hashed passwords were seeded
        String sql = "SELECT " + idColumn + " as auth_id, name, email, password_hash, status, last_login " +
                     "FROM " + tableName + " WHERE email = ?";
                     
        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    AuthClass user = new AuthClass();
                    user.setAuthId(rs.getInt("auth_id"));
                    user.setName(rs.getString("name"));
                    user.setEmail(rs.getString("email"));
                    user.setPasswordHash(rs.getString("password_hash")); // Fetches BCrypt hash
                    user.setUserType(type);
                    user.setAuthStatus(rs.getString("status"));
                    user.setLinkedId(rs.getInt("auth_id")); 

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
     * Updates the last_login timestamp in the appropriate table.
     */
    public void updateLastLogin(int authId) throws SQLException {
        // Since we don't store userType here, we check both for a quick update 
        // Or better, let's just update based on the current session user type.
        // For a simple fix, we'll try investor first.
        String sqlInvestor = "UPDATE investor SET last_login = NOW() WHERE investor_id = ?";
        String sqlAdmin = "UPDATE admin SET last_login = NOW() WHERE admin_id = ?";

        try (Connection conn = ServerConnector.DBConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(sqlInvestor)) {
                stmt.setInt(1, authId);
                int updated = stmt.executeUpdate();
                if (updated == 0) {
                    try (PreparedStatement stmtAdmin = conn.prepareStatement(sqlAdmin)) {
                        stmtAdmin.setInt(1, authId);
                        stmtAdmin.executeUpdate();
                    }
                }
            }
        }
    }
}