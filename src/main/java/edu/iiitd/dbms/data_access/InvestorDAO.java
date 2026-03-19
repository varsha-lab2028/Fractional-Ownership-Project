package edu.iiitd.dbms.data_access;

import edu.iiitd.dbms.config.ServerConnector;
import edu.iiitd.dbms.domain.Investor;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InvestorDAO {
    private static final String SELECT =
        "SELECT investor_id, name, email, wallet_balance FROM investor";

    private Investor map(ResultSet rs) throws SQLException {
        return new Investor(
            rs.getInt("investor_id"),
            rs.getString("name"),
            rs.getString("email"),
            null,
            null,
            rs.getDouble("wallet_balance")
        );
    }

    //register investor
    public boolean registerInvestor(String name, String email, String phone) throws SQLException {
        String sqlQuery = """
        INSERT INTO investor (name, email, phone)
        VALUES (?, ?, ?)""";

        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement ps = conn.prepareStatement(sqlQuery)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, phone);

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        }
    }

    //delete investor
    public boolean deleteInvestor(int investorId) throws SQLException {
        String sqlQuery = "DELETE FROM investor WHERE investor_id = ?";
        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement ps = conn.prepareStatement(sqlQuery)) {

            ps.setInt(1, investorId);

            int rowsAffected = ps.executeUpdate();

            return rowsAffected > 0;
        }
    }

    public List<Investor> listInvestors() throws SQLException {
        List<Investor> list = new ArrayList<>();
        try (Connection c = ServerConnector.DBConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + " ORDER BY investor_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public Investor findByInvestorId(int id) throws SQLException {
        try (Connection c = ServerConnector.DBConnection()) { return findByInvestorId(c, id); }
    }
    public Investor findByInvestorId(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT + " WHERE investor_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public Investor findByEmail(String email) throws SQLException {
        try (Connection c = ServerConnector.DBConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + " WHERE email = ?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public boolean updateInvestorProfile(int id, String name, String phone) throws SQLException {
        // Only update name since phone doesn't exist in live DB
        try (Connection c = ServerConnector.DBConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE investor SET name = ? WHERE investor_id = ?")) {
            ps.setString(1, name); ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    public double getWalletBalance(int id) throws SQLException {
        try (Connection c = ServerConnector.DBConnection();
             PreparedStatement ps = c.prepareStatement("SELECT wallet_balance FROM investor WHERE investor_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getDouble(1) : 0.0; }
        }
    }

    public List<Investor> listInvestorsByWalletBalance() throws SQLException {
        List<Investor> list = new ArrayList<>();
        try (Connection c = ServerConnector.DBConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + " ORDER BY wallet_balance DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<Investor> listInvestorsWithNoWalletTransactions() throws SQLException {
        List<Investor> list = new ArrayList<>();
        String sql = SELECT + " WHERE investor_id NOT IN (SELECT DISTINCT investor_id FROM wallet_transaction)";
        try (Connection c = ServerConnector.DBConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<Investor> listInvestorsWithPositiveBalance() throws SQLException {
        List<Investor> list = new ArrayList<>();
        try (Connection c = ServerConnector.DBConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + " WHERE wallet_balance > 0 ORDER BY wallet_balance DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public double getTotalInvestedAmount(int id) throws SQLException {
        String sql = "SELECT COALESCE(SUM(o.units_held * i.price_per_unit), 0) FROM ownership o JOIN ipo i ON o.asset_id = i.asset_id WHERE o.investor_id = ?";
        try (Connection c = ServerConnector.DBConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getDouble(1) : 0.0; }
        }
    }

    public double getCurrentPortfolioValue(int id) throws SQLException {
        String sql = """
            SELECT COALESCE(SUM((o.units_held * 1.0 / i.total_units) * v.valuation_amount), 0)
            FROM ownership o
            JOIN ipo i ON o.asset_id = i.asset_id
            JOIN valuation v ON o.asset_id = v.asset_id
            WHERE o.investor_id = ?
              AND v.valuation_date = (SELECT MAX(valuation_date) FROM valuation WHERE asset_id = o.asset_id)
            """;
        try (Connection c = ServerConnector.DBConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getDouble(1) : 0.0; }
        }
    }
}
