package edu.iiitd.dbms.data_access;

import edu.iiitd.dbms.config.ServerConnector;
import edu.iiitd.dbms.dto.WalletTransactionDTO;
import edu.iiitd.dbms.dto.WalletTransactionWithNameDTO;
import edu.iiitd.dbms.dto.WalletSummaryDTO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WalletTransactionDAO {

    public boolean insertWalletTransaction(Connection c, int investorId, double amount, String type, String category) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO wallet_transaction (investor_id, amount, transaction_type, transfer_category, transaction_date) VALUES (?, ?, ?, ?, NOW())")) {
            ps.setInt(1, investorId); ps.setDouble(2, amount); ps.setString(3, type); ps.setString(4, category);
            return ps.executeUpdate() > 0;
        }
    }
    public boolean insertWalletTransaction(int investorId, double amount, String type, String category) throws SQLException {
        try (Connection c = ServerConnector.DBConnection()) {
            return insertWalletTransaction(c, investorId, amount, type, category);
        }
    }

    public double getStoredWalletBalance(int investorId) throws SQLException {
        try (Connection c = ServerConnector.DBConnection();
             PreparedStatement ps = c.prepareStatement("SELECT wallet_balance FROM investor WHERE investor_id = ?")) {
            ps.setInt(1, investorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        }
        throw new SQLException("Investor not found: " + investorId);
    }

    public List<WalletTransactionDTO> getTransactionHistory(int investorId) throws SQLException {
        List<WalletTransactionDTO> list = new ArrayList<>();
        String sql = "SELECT transaction_id, amount, transaction_type, transfer_category, transaction_date FROM wallet_transaction WHERE investor_id = ? ORDER BY transaction_date DESC";
        try (Connection c = ServerConnector.DBConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, investorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new WalletTransactionDTO(
                        rs.getInt("transaction_id"),
                        rs.getDouble("amount"),
                        rs.getString("transaction_type"),
                        rs.getString("transfer_category"),
                        rs.getTimestamp("transaction_date")));
                }
            }
        }
        return list;
    }

    private boolean record(int investorId, double amount, String type, String category) throws SQLException {
        try (Connection c = ServerConnector.DBConnection()) {
            c.setAutoCommit(false);
            try {
                insertWalletTransaction(c, investorId, amount, type, category);
                c.commit();
                return true;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public boolean deposit(int id, double amount, String cat) throws SQLException {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        return record(id, amount, "DEPOSIT", cat);
    }
    public boolean deposit(int id, double amount) throws SQLException { return deposit(id, amount, "Bank Transfer"); }
    public boolean deposit(Connection c, int id, double amount, String cat) throws SQLException {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        return insertWalletTransaction(c, id, amount, "DEPOSIT", cat);
    }

    public boolean withdraw(int id, double amount, String cat) throws SQLException {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        return record(id, -amount, "WITHDRAWAL", cat);
    }
    public boolean withdraw(int id, double amount) throws SQLException { return withdraw(id, amount, "Bank Transfer"); }
    public boolean withdraw(Connection c, int id, double amount, String cat) throws SQLException {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        return insertWalletTransaction(c, id, -amount, "WITHDRAWAL", cat);
    }

    public boolean creditDividend(int id, double amount, String cat) throws SQLException {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        return record(id, amount, "DIVIDEND", cat);
    }
    public boolean creditDividend(int id, double amount) throws SQLException { return creditDividend(id, amount, "Yield Payout"); }
    public boolean creditDividend(Connection c, int id, double amount, String cat) throws SQLException {
        return insertWalletTransaction(c, id, amount, "DIVIDEND", cat);
    }

    public boolean deductForAssetPurchase(int id, double amount, String cat) throws SQLException {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        return record(id, -amount, "ASSET_PURCHASE", cat);
    }
    public boolean deductForAssetPurchase(int id, double amount) throws SQLException { return deductForAssetPurchase(id, amount, "Secondary Market Order"); }
    public boolean deductForAssetPurchase(Connection c, int id, double amount, String cat) throws SQLException {
        return insertWalletTransaction(c, id, -amount, "ASSET_PURCHASE", cat);
    }

    public boolean creditAssetSale(int id, double amount, String cat) throws SQLException {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        return record(id, amount, "ASSET_SALE", cat);
    }
    public boolean creditAssetSale(int id, double amount) throws SQLException { return creditAssetSale(id, amount, "Trade Execution"); }
    public boolean creditAssetSale(Connection c, int id, double amount, String cat) throws SQLException {
        return insertWalletTransaction(c, id, amount, "ASSET_SALE", cat);
    }

    public boolean refund(int id, double amount, String cat) throws SQLException {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        return record(id, amount, "REFUND", cat);
    }
    public boolean adminAdjustment(int id, double amount, String reason) throws SQLException {
        return record(id, amount, "ADMIN_ADJUSTMENT", reason);
    }

    // Q18 — all transactions with investor name
    public List<WalletTransactionWithNameDTO> getAllTransactionsWithInvestorName() throws SQLException {
        String sql = """
            SELECT wt.transaction_id, i.investor_name AS investor_name, wt.amount,
                   wt.transaction_type, wt.transfer_category, wt.transaction_date
            FROM WALLET_TRANSACTION wt
            JOIN INVESTOR i ON i.investor_id = wt.investor_id
            ORDER BY wt.transaction_date DESC
            """;
        List<WalletTransactionWithNameDTO> list = new ArrayList<>();
        try (Connection c = ServerConnector.DBConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new WalletTransactionWithNameDTO(
                    rs.getInt("transaction_id"),
                    rs.getString("investor_name"),
                    rs.getDouble("amount"),
                    rs.getString("transaction_type"),
                    rs.getString("transfer_category"),
                    rs.getTimestamp("transaction_date")));
            }
        }
        return list;
    }

    // Q19 — total per investor per type
    public List<WalletSummaryDTO> getTransactionSummaryByInvestorAndType() throws SQLException {
        String sql = """
            SELECT i.investor_name AS investor_name, wt.transaction_type, SUM(wt.amount) AS total_amount
            FROM WALLET_TRANSACTION wt
            JOIN INVESTOR i ON i.investor_id = wt.investor_id
            GROUP BY i.investor_id, i.investor_name, wt.transaction_type
            ORDER BY i.investor_name, wt.transaction_type
            """;
        List<WalletSummaryDTO> list = new ArrayList<>();
        try (Connection c = ServerConnector.DBConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new WalletSummaryDTO(
                    rs.getString("investor_name"),
                    rs.getString("transaction_type"),
                    rs.getDouble("total_amount")));
            }
        }
        return list;
    }
}
