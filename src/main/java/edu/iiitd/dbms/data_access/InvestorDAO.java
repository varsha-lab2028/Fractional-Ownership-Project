package edu.iiitd.dbms.data_access;

import edu.iiitd.dbms.config.ServerConnector;
import edu.iiitd.dbms.domain.Investor;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InvestorDAO {
    private Investor matchInvestorColumns(ResultSet rs) throws SQLException {
        return new Investor(
                rs.getInt("investor_id"),
                rs.getString("investor_name"),
                rs.getString("email"),
                rs.getString("phone"), 
                rs.getDate("registration_date").toLocalDate(),
                rs.getDouble("wallet_balance")
        );
    }

    //create methods
    public boolean registerInvestor(String name, String email, String phone) throws SQLException {
        String sql = """
            INSERT INTO investor (investor_name, email, phone, registration_date, wallet_balance)
            VALUES (?, ?, ?, CURRENT_DATE, 0.00)
        """;
        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, phone);
            return ps.executeUpdate() > 0;
        }
    }

    //listing methods
    //gets all the investors
    public List<Investor> listInvestors() throws SQLException {
        String sql = """
            SELECT investor_id, investor_name, email, phone, registration_date, wallet_balance
            FROM investor
            ORDER BY investor_id
        """;
        List<Investor> investorList = new ArrayList<>();
        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) investorList.add(matchInvestorColumns(rs));
        }
        return investorList;
    }

    //finding methods
    //get by investor id
    public Investor findByInvestorId(int investorId) throws SQLException {
        try (Connection connect = ServerConnector.DBConnection()) {
            return findByInvestorId(connect, investorId);
        }
    }
    public Investor findByInvestorId(Connection connect, int investorId) throws SQLException {
        String sql = """
            SELECT investor_id, investor_name, email, phone, registration_date, wallet_balance
            FROM investor
            WHERE investor_id = ?
        """;
        try (PreparedStatement ps = connect.prepareStatement(sql)) {
            ps.setInt(1, investorId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? matchInvestorColumns(rs) : null;
            }
        }
    }

    public Investor findByEmail(String email) throws SQLException {
        String sql = """
            SELECT investor_id, investor_name, email, phone, registration_date, wallet_balance
            FROM investor
            WHERE email = ?
        """;
        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? matchInvestorColumns(rs) : null;
            }
        }
    }

    //updating methods (profile settings)
    public boolean updateInvestorProfile(int investorId, String newName, String newPhone) throws SQLException {
        String sql = """
            UPDATE investor 
            SET investor_name = ?, phone = ? 
            WHERE investor_id = ?
        """;
        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newName);
            ps.setString(2, newPhone);
            ps.setInt(3, investorId);
            return ps.executeUpdate() > 0;
        }
    }

    //deletion methods
    public boolean deleteInvestor(int investorId) throws SQLException {
        // Note: Make sure ON DELETE CASCADE is set up in your schema for related tables
        String sql = "DELETE FROM investor WHERE investor_id = ?";
        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, investorId);
            return ps.executeUpdate() > 0;
        }
    }

    //wallet operations
    // Convenience method used by UI classes — reads wallet_balance from the investor row
    // (kept in sync by the DB trigger on wallet_transaction)
    public double getWalletBalance(int investorId) throws SQLException {
        String sql = "SELECT wallet_balance FROM investor WHERE investor_id = ?";
        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, investorId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getDouble("wallet_balance") : 0.00;
            }
        }
    }

    //financial aggregations (for dashboard)
    
    //Calculates the total initial capital invested based on units held and original IPO price
    public double getTotalInvestedAmount(int investorId) throws SQLException {
        String sql = """
            SELECT SUM(o.units_held * i.price_per_unit) as total_invested
            FROM OWNERSHIP o
            JOIN IPO i ON o.asset_id = i.asset_id
            WHERE o.investor_id = ?
        """;
        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, investorId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble("total_invested") : 0.00;
            }
        }
    }

     //Calculates the current real-time portfolio value using the latest valuations.
     //Logic: (Units Held / Total Asset Units) * Latest Asset Valuation
    public double getCurrentPortfolioValue(int investorId) throws SQLException {
        String sql = """
            SELECT SUM((o.units_held * 1.0 / i.total_units) * v.valuation_amount) as current_value
            FROM OWNERSHIP o
            JOIN IPO i ON o.asset_id = i.asset_id
            JOIN VALUATION v ON o.asset_id = v.asset_id
            WHERE o.investor_id = ?
            AND v.valuation_date = (
                SELECT MAX(valuation_date) 
                FROM VALUATION 
                WHERE asset_id = o.asset_id
            )
        """;
        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, investorId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble("current_value") : 0.00;
            }
        }
    }

    //Q17 - all investors ordered by wallet balance descending
    public List<Investor> listInvestorsByWalletBalance() throws SQLException {
        String sql = """
            SELECT investor_id, investor_name, email, phone, registration_date, wallet_balance
            FROM investor
            ORDER BY wallet_balance DESC
        """;
        List<Investor> result = new ArrayList<>();
        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(matchInvestorColumns(rs));
        }
        return result;
    }

    //Q20 - investors who have NEVER made a wallet transaction (NOT EXISTS)
    public List<Investor> listInvestorsWithNoWalletTransactions() throws SQLException {
        String sql = """
            SELECT investor_id, investor_name, email, phone, registration_date, wallet_balance
            FROM INVESTOR i
            WHERE NOT EXISTS (
                SELECT 1 FROM WALLET_TRANSACTION wt WHERE wt.investor_id = i.investor_id
            )
        """;
        List<Investor> result = new ArrayList<>();
        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(matchInvestorColumns(rs));
        }
        return result;
    }

    //Q21 - investors with a positive wallet balance (active wallets)
    public List<Investor> listInvestorsWithPositiveBalance() throws SQLException {
        String sql = """
            SELECT investor_id, investor_name, email, phone, registration_date, wallet_balance
            FROM INVESTOR
            WHERE wallet_balance > 0
            ORDER BY wallet_balance DESC
        """;
        List<Investor> result = new ArrayList<>();
        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(matchInvestorColumns(rs));
        }
        return result;
    }
}