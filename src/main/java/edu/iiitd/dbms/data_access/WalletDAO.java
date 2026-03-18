package edu.iiitd.dbms.data_access;

import edu.iiitd.dbms.config.ServerConnector;
import edu.iiitd.dbms.dto.WalletTransactionDTO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WalletDAO {
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
    public void recordTransaction(int investorId, double amount, String type, String category) throws SQLException {
        String insertQuery = "INSERT INTO WALLET_TRANSACTION (investor_id, amount, transaction_type, transfer_category) " +
                             "VALUES (?, ?, ?, ?)";
        String updateWalletQuery = "UPDATE INVESTOR SET wallet_balance = wallet_balance + ? WHERE investor_id = ?";
        try (Connection conn = ServerConnector.DBConnection()) {
            conn.setAutoCommit(false); // Start transaction

            try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
                 PreparedStatement updateStmt = conn.prepareStatement(updateWalletQuery)) {

                // 1. Record the ledger entry
                insertStmt.setInt(1, investorId);
                insertStmt.setDouble(2, amount);
                insertStmt.setString(3, type);
                insertStmt.setString(4, category);
                insertStmt.executeUpdate();

                // 2. Update the actual wallet balance
                updateStmt.setDouble(1, amount);
                updateStmt.setInt(2, investorId);
                updateStmt.executeUpdate();

                conn.commit(); // Commit transaction
            } catch (SQLException e) {
                conn.rollback(); // Rollback if either step fails
                throw e;
            }
        }
    }
}