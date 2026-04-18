package edu.iiitd.dbms.data_access;

import edu.iiitd.dbms.config.ServerConnector;
import edu.iiitd.dbms.dto.WalletTransactionDTO;
import edu.iiitd.dbms.dto.WalletTransactionWithNameDTO;
import edu.iiitd.dbms.dto.WalletSummaryDTO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WalletTransactionDAO {
    public boolean insertWalletTransaction(Connection connection, int investorId, double amount, String type, String category) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO wallet_transaction (investor_id, amount, transaction_type, transfer_category, transaction_date) VALUES (?, ?, ?, ?, NOW())")) {
            ps.setInt(1, investorId); ps.setDouble(2, amount); ps.setString(3, type); ps.setString(4, category);
            return ps.executeUpdate() > 0;
        }
    }
    public boolean insertWalletTransaction(int investorId, double amount, String type, String category) throws SQLException {
        try (Connection connection = ServerConnector.DBConnection()) {
            return insertWalletTransaction(connection, investorId, amount, type, category);
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

    /**
     * Core internal method: inserts a wallet_transaction row and updates
     * investor.wallet_balance within the same transaction.
     *
     * NOTE: If the DB trigger trg_update_wallet_balance_after_transaction is
     * installed it will also fire on the INSERT, causing a double-update.
     * Either remove that trigger or remove the explicit UPDATE below.
     *
     * @param checkBalance when true, verifies the investor has sufficient
     *                     funds before allowing a debit (amount < 0).
     */
    private boolean record(int investorId, double amount, String type,
                           String category, boolean checkBalance) throws SQLException {
        try (Connection c = ServerConnector.DBConnection()) {
            c.setAutoCommit(false);
            try {
                // --- Bug fix #2: overdraft guard ---
                if (checkBalance && amount < 0) {
                    // Lock the investor row so concurrent transactions cannot race past this check.
                    try (PreparedStatement ps = c.prepareStatement(
                            "SELECT wallet_balance FROM INVESTOR WHERE investor_id = ? FOR UPDATE")) {
                        ps.setInt(1, investorId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) throw new SQLException("Investor not found: " + investorId);
                            double balance = rs.getDouble(1);
                            if (balance + amount < 0) {
                                throw new SQLException(
                                    "Insufficient wallet balance. Available: " + String.format("%.2f", balance)
                                    + ", Requested debit: " + String.format("%.2f", -amount));
                            }
                        }
                    }
                }

                // Insert the transaction log entry
                insertWalletTransaction(c, investorId, amount, type, category);

                // --- Bug fix #1: sync wallet_balance in the same transaction ---
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE INVESTOR SET wallet_balance = wallet_balance + ? WHERE investor_id = ?")) {
                    ps.setDouble(1, amount);
                    ps.setInt(2, investorId);
                    int rows = ps.executeUpdate();
                    if (rows == 0) throw new SQLException("Investor not found when updating balance: " + investorId);
                }

                c.commit();
                return true;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** Convenience overload — no balance pre-check (used for credits). */
    private boolean record(int investorId, double amount, String type, String category) throws SQLException {
        return record(investorId, amount, type, category, false);
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
        return record(id, -amount, "WITHDRAWAL", cat, true); // true = enforce balance check
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
        return record(id, -amount, "ASSET_PURCHASE", cat, true); // true = enforce balance check
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