package edu.iiitd.dbms.data_access.impl;

import edu.iiitd.dbms.data_access.WalletTransactionDAO;
import edu.iiitd.dbms.dto.WalletTransactionDTO;
import edu.iiitd.dbms.dto.WalletTransactionWithNameDTO;
import edu.iiitd.dbms.dto.WalletSummaryDTO;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Concrete implementation of wallet-transaction data-access operations.
 *
 * The underlying DB trigger on WALLET_TRANSACTION automatically keeps
 * INVESTOR.wallet_balance in sync whenever a row is inserted, so this
 * class never manually updates that column.
 *
 * Supported transaction types:
 *   DEPOSIT, WITHDRAWAL, DIVIDEND, ASSET_PURCHASE, ASSET_SALE, REFUND, ADMIN_ADJUSTMENT
 */
public class WalletTransactionImpl {

    private final WalletTransactionDAO walletDAO;

    public WalletTransactionImpl() {
        this.walletDAO = new WalletTransactionDAO();
    }

    public double getBalance(int investorId) throws SQLException {
        return walletDAO.getStoredWalletBalance(investorId);
    }

    public List<WalletTransactionDTO> getTransactionHistory(int investorId) throws SQLException {
        return walletDAO.getTransactionHistory(investorId);
    }

    // Q18 - all wallet transactions enriched with investor name
    public List<WalletTransactionWithNameDTO> getAllTransactionsWithInvestorName() throws SQLException {
        return walletDAO.getAllTransactionsWithInvestorName();
    }

    // Q19 - total transacted per investor per transaction type
    public List<WalletSummaryDTO> getTransactionSummaryByInvestorAndType() throws SQLException {
        return walletDAO.getTransactionSummaryByInvestorAndType();
    }

    public boolean insert(Connection conn, int investorId, double amount,
                          String transactionType, String transferCategory) throws SQLException {
        return walletDAO.insertWalletTransaction(conn, investorId, amount, transactionType, transferCategory);
    }

    public boolean insert(int investorId, double amount,
                          String transactionType, String transferCategory) throws SQLException {
        return walletDAO.insertWalletTransaction(investorId, amount, transactionType, transferCategory);
    }

    public boolean deposit(int investorId, double amount, String category) throws SQLException {
        return walletDAO.deposit(investorId, amount, category);
    }

    public boolean deposit(int investorId, double amount) throws SQLException {
        return walletDAO.deposit(investorId, amount);
    }

    public boolean deposit(Connection conn, int investorId, double amount, String category) throws SQLException {
        return walletDAO.deposit(conn, investorId, amount, category);
    }

    public boolean withdraw(int investorId, double amount, String category) throws SQLException {
        return walletDAO.withdraw(investorId, amount, category);
    }

    public boolean withdraw(int investorId, double amount) throws SQLException {
        return walletDAO.withdraw(investorId, amount);
    }

    public boolean withdraw(Connection conn, int investorId, double amount, String category) throws SQLException {
        return walletDAO.withdraw(conn, investorId, amount, category);
    }

    public boolean creditDividend(int investorId, double amount, String category) throws SQLException {
        return walletDAO.creditDividend(investorId, amount, category);
    }

    public boolean creditDividend(int investorId, double amount) throws SQLException {
        return walletDAO.creditDividend(investorId, amount);
    }

    public boolean creditDividend(Connection conn, int investorId, double amount, String category) throws SQLException {
        return walletDAO.creditDividend(conn, investorId, amount, category);
    }

    public boolean deductForPurchase(int investorId, double amount, String category) throws SQLException {
        return walletDAO.deductForAssetPurchase(investorId, amount, category);
    }

    public boolean deductForPurchase(int investorId, double amount) throws SQLException {
        return walletDAO.deductForAssetPurchase(investorId, amount);
    }

    public boolean deductForPurchase(Connection conn, int investorId, double amount, String category) throws SQLException {
        return walletDAO.deductForAssetPurchase(conn, investorId, amount, category);
    }

    public boolean creditAssetSale(int investorId, double amount, String category) throws SQLException {
        return walletDAO.creditAssetSale(investorId, amount, category);
    }

    public boolean creditAssetSale(int investorId, double amount) throws SQLException {
        return walletDAO.creditAssetSale(investorId, amount);
    }

    public boolean creditAssetSale(Connection conn, int investorId, double amount, String category) throws SQLException {
        return walletDAO.creditAssetSale(conn, investorId, amount, category);
    }

    public boolean refund(int investorId, double amount, String category) throws SQLException {
        return walletDAO.refund(investorId, amount, category);
    }

    public boolean adminAdjustment(int investorId, double amount, String reason) throws SQLException {
        return walletDAO.adminAdjustment(investorId, amount, reason);
    }
}
