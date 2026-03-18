package edu.iiitd.dbms.data_access;

import edu.iiitd.dbms.config.ServerConnector;
import edu.iiitd.dbms.dto.WalletTransactionDTO;
import edu.iiitd.dbms.dto.WalletTransactionWithNameDTO;
import edu.iiitd.dbms.dto.WalletSummaryDTO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WalletTransactionDAO {
    public boolean insertWalletTransaction(Connection conn,
                                           int investorId,
                                           double amount,
                                           String transactionType,
                                           String transferCategory) throws SQLException {
        String sql = """
                INSERT INTO wallet_transaction
                (investor_id, amount, transaction_type, transfer_category, transaction_date)
                VALUES (?, ?, ?, ?, NOW())
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, investorId);
            stmt.setDouble(2, amount);
            stmt.setString(3, transactionType);
            stmt.setString(4, transferCategory);
            return stmt.executeUpdate() > 0;
        }
    }
    public boolean insertWalletTransaction(int investorId,
                                           double amount,
                                           String transactionType,
                                           String transferCategory) throws SQLException {
        try (Connection conn = ServerConnector.DBConnection()) {
            return insertWalletTransaction(conn, investorId, amount, transactionType, transferCategory);
        }
    }

    public double getStoredWalletBalance(int investorId) throws SQLException {
        String sql = "SELECT wallet_balance FROM investor WHERE investor_id = ?";
        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, investorId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("wallet_balance");
                }
            }
        }
        throw new SQLException("Investor not found for investor_id = " + investorId);
    }

    public List<String> getWalletTransactionsByInvestor(int investorId) throws SQLException {
        List<String> transactions = new ArrayList<>();

        String sql = """
                SELECT transaction_id, amount, transaction_type, transfer_category, transaction_date
                FROM wallet_transaction
                WHERE investor_id = ?
                ORDER BY transaction_date DESC
                """;

        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, investorId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String row = "Txn ID: " + rs.getInt("transaction_id")
                            + ", Amount: " + rs.getDouble("amount")
                            + ", Type: " + rs.getString("transaction_type")
                            + ", Category: " + rs.getString("transfer_category")
                            + ", Date: " + rs.getTimestamp("transaction_date");
                    transactions.add(row);
                }
            }
        }
        return transactions;
    }

    //getting the transaction history for a specific investor
    public List<WalletTransactionDTO> getTransactionHistory(int investorId) throws SQLException {
        List<WalletTransactionDTO> transactions = new ArrayList<>();
        String query = "SELECT transaction_id, amount, transaction_type, transfer_category, transaction_date " +
                       "FROM WALLET_TRANSACTION WHERE investor_id = ? ORDER BY transaction_date DESC";
        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, investorId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(new WalletTransactionDTO(
                            rs.getInt("transaction_id"),
                            rs.getDouble("amount"),
                            rs.getString("transaction_type"),
                            rs.getString("transfer_category"),
                            rs.getTimestamp("transaction_date")
                    ));
                }
            }
        }
        return transactions;
    }

    //Process a new wallet transaction (like funding the account or buying an asset)
    public boolean recordTransaction(int investorId, double amount, String type, String category) throws SQLException {
        try (Connection conn = ServerConnector.DBConnection()) {
            conn.setAutoCommit(false);
            try {
                boolean inserted = insertWalletTransaction(conn, investorId, amount, type, category);
                conn.commit();
                return inserted;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // ── Named insert methods (each maps to one real-world wallet event) ────────

    /** DEPOSIT — investor funds their account (e.g. bank transfer). Amount must be > 0. */
    public boolean deposit(int investorId, double amount, String category) throws SQLException {
        if (amount <= 0) throw new IllegalArgumentException("Deposit amount must be positive.");
        return recordTransaction(investorId, amount, "DEPOSIT", category);
    }

    /** DEPOSIT with default category "Bank Transfer". */
    public boolean deposit(int investorId, double amount) throws SQLException {
        return deposit(investorId, amount, "Bank Transfer");
    }

    /** WITHDRAWAL — investor withdraws cash from their wallet. Amount must be > 0;
     *  stored as a negative value so the trigger deducts it. */
    public boolean withdraw(int investorId, double amount, String category) throws SQLException {
        if (amount <= 0) throw new IllegalArgumentException("Withdrawal amount must be positive.");
        return recordTransaction(investorId, -amount, "WITHDRAWAL", category);
    }

    /** WITHDRAWAL with default category "Bank Transfer". */
    public boolean withdraw(int investorId, double amount) throws SQLException {
        return withdraw(investorId, amount, "Bank Transfer");
    }

    /** DIVIDEND — credit a dividend or rental-income payout. Amount must be > 0. */
    public boolean creditDividend(int investorId, double amount, String category) throws SQLException {
        if (amount <= 0) throw new IllegalArgumentException("Dividend amount must be positive.");
        return recordTransaction(investorId, amount, "DIVIDEND", category);
    }

    /** DIVIDEND with default category "Yield Payout". */
    public boolean creditDividend(int investorId, double amount) throws SQLException {
        return creditDividend(investorId, amount, "Yield Payout");
    }

    /** ASSET_PURCHASE — deduct funds when an investor buys asset units.
     *  amount must be > 0; stored as negative. */
    public boolean deductForAssetPurchase(int investorId, double amount, String category) throws SQLException {
        if (amount <= 0) throw new IllegalArgumentException("Purchase amount must be positive.");
        return recordTransaction(investorId, -amount, "ASSET_PURCHASE", category);
    }

    /** ASSET_PURCHASE with default category "Secondary Market Order". */
    public boolean deductForAssetPurchase(int investorId, double amount) throws SQLException {
        return deductForAssetPurchase(investorId, amount, "Secondary Market Order");
    }

    /** ASSET_SALE — credit funds when an investor sells asset units. Amount must be > 0. */
    public boolean creditAssetSale(int investorId, double amount, String category) throws SQLException {
        if (amount <= 0) throw new IllegalArgumentException("Sale amount must be positive.");
        return recordTransaction(investorId, amount, "ASSET_SALE", category);
    }

    /** ASSET_SALE with default category "Trade Execution". */
    public boolean creditAssetSale(int investorId, double amount) throws SQLException {
        return creditAssetSale(investorId, amount, "Trade Execution");
    }

    /** REFUND — credit a refund back to the investor's wallet. Amount must be > 0. */
    public boolean refund(int investorId, double amount, String category) throws SQLException {
        if (amount <= 0) throw new IllegalArgumentException("Refund amount must be positive.");
        return recordTransaction(investorId, amount, "REFUND", category);
    }

    /** ADMIN_ADJUSTMENT — admin-initiated credit or debit (amount can be positive or negative). */
    public boolean adminAdjustment(int investorId, double amount, String reason) throws SQLException {
        return recordTransaction(investorId, amount, "ADMIN_ADJUSTMENT", reason);
    }

    // ── In-transaction overloads (for use inside executeTrade / other atomic blocks) ──

    /** DEPOSIT within an already-open connection (no commit — caller manages transaction). */
    public boolean deposit(Connection conn, int investorId, double amount, String category) throws SQLException {
        if (amount <= 0) throw new IllegalArgumentException("Deposit amount must be positive.");
        return insertWalletTransaction(conn, investorId, amount, "DEPOSIT", category);
    }

    /** WITHDRAWAL within an already-open connection. */
    public boolean withdraw(Connection conn, int investorId, double amount, String category) throws SQLException {
        if (amount <= 0) throw new IllegalArgumentException("Withdrawal amount must be positive.");
        return insertWalletTransaction(conn, investorId, -amount, "WITHDRAWAL", category);
    }

    /** ASSET_PURCHASE within an already-open connection (used by TradingService.executeTrade). */
    public boolean deductForAssetPurchase(Connection conn, int investorId, double amount, String category) throws SQLException {
        if (amount <= 0) throw new IllegalArgumentException("Purchase amount must be positive.");
        return insertWalletTransaction(conn, investorId, -amount, "ASSET_PURCHASE", category);
    }

    /** ASSET_SALE within an already-open connection (used by TradingService.executeTrade). */
    public boolean creditAssetSale(Connection conn, int investorId, double amount, String category) throws SQLException {
        if (amount <= 0) throw new IllegalArgumentException("Sale amount must be positive.");
        return insertWalletTransaction(conn, investorId, amount, "ASSET_SALE", category);
    }

    /** DIVIDEND within an already-open connection. */
    public boolean creditDividend(Connection conn, int investorId, double amount, String category) throws SQLException {
        if (amount <= 0) throw new IllegalArgumentException("Dividend amount must be positive.");
        return insertWalletTransaction(conn, investorId, amount, "DIVIDEND", category);
    }

    //Q18 - all wallet transactions joined with investor name, newest first
    public List<WalletTransactionWithNameDTO> getAllTransactionsWithInvestorName() throws SQLException {
        String sql = """
            SELECT wt.transaction_id,
                   i.investor_name AS investor_name,
                   wt.amount,
                   wt.transaction_type,
                   wt.transfer_category,
                   wt.transaction_date
            FROM WALLET_TRANSACTION wt
            JOIN INVESTOR i ON i.investor_id = wt.investor_id
            ORDER BY wt.transaction_date DESC
        """;
        List<WalletTransactionWithNameDTO> result = new ArrayList<>();
        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new WalletTransactionWithNameDTO(
                        rs.getInt("transaction_id"),
                        rs.getString("investor_name"),
                        rs.getDouble("amount"),
                        rs.getString("transaction_type"),
                        rs.getString("transfer_category"),
                        rs.getTimestamp("transaction_date")
                ));
            }
        }
        return result;
    }

    //Q19 - total amount transacted per investor per transaction type
    public List<WalletSummaryDTO> getTransactionSummaryByInvestorAndType() throws SQLException {
        String sql = """
            SELECT i.investor_name AS investor_name,
                   wt.transaction_type,
                   SUM(wt.amount) AS total_amount
            FROM WALLET_TRANSACTION wt
            JOIN INVESTOR i ON i.investor_id = wt.investor_id
            GROUP BY i.investor_id, i.investor_name, wt.transaction_type
            ORDER BY i.investor_name, wt.transaction_type
        """;
        List<WalletSummaryDTO> result = new ArrayList<>();
        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new WalletSummaryDTO(
                        rs.getString("investor_name"),
                        rs.getString("transaction_type"),
                        rs.getDouble("total_amount")
                ));
            }
        }
        return result;
    }
}