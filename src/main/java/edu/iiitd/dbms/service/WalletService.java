package edu.iiitd.dbms.service;

import edu.iiitd.dbms.data_access.WalletTransactionDAO;
import edu.iiitd.dbms.dto.WalletTransactionDTO;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Service layer for all wallet operations.
 *
 * Every public method maps to one real-world wallet event.
 * The DB trigger (trg_update_wallet_balance_after_transaction) automatically
 * updates INVESTOR.wallet_balance on every successful INSERT into WALLET_TRANSACTION,
 * so none of these methods need to touch the investor row directly.
 *
 * Supported transaction types:
 *   DEPOSIT          - investor adds cash to their wallet
 *   WITHDRAWAL       - investor withdraws cash from their wallet
 *   DIVIDEND         - yield / rental income credited automatically
 *   ASSET_PURCHASE   - funds deducted when buying asset units
 *   ASSET_SALE       - funds credited when selling asset units
 *   REFUND           - partial or full refund credited back
 *   ADMIN_ADJUSTMENT - admin-initiated correction (positive or negative)
 */
public class WalletService {

    private final WalletTransactionDAO walletDAO;

    public WalletService() {
        this.walletDAO = new WalletTransactionDAO();
    }

    // READ

    /** Return the investor's current wallet balance. */
    public double getWalletBalance(int investorId) throws SQLException {
        return walletDAO.getStoredWalletBalance(investorId);
    }

    /** Return the investor's full transaction history as rich DTOs, newest first. */
    public List<WalletTransactionDTO> getTransactionHistory(int investorId) throws SQLException {
        return walletDAO.getTransactionHistory(investorId);
    }

    // DEPOSIT

    public boolean deposit(int investorId, double amount, String category) throws SQLException {
        return walletDAO.deposit(investorId, amount, category);
    }

    public boolean deposit(int investorId, double amount) throws SQLException {
        return walletDAO.deposit(investorId, amount);
    }

    // WITHDRAWAL

    public boolean withdraw(int investorId, double amount, String category) throws SQLException {
        return walletDAO.withdraw(investorId, amount, category);
    }

    public boolean withdraw(int investorId, double amount) throws SQLException {
        return walletDAO.withdraw(investorId, amount);
    }

    // DIVIDEND

    public boolean creditDividend(int investorId, double amount, String category) throws SQLException {
        return walletDAO.creditDividend(investorId, amount, category);
    }

    public boolean creditDividend(int investorId, double amount) throws SQLException {
        return walletDAO.creditDividend(investorId, amount);
    }

    // ASSET PURCHASE

    public boolean deductForAssetPurchase(int investorId, double amount, String category) throws SQLException {
        return walletDAO.deductForAssetPurchase(investorId, amount, category);
    }

    public boolean deductForAssetPurchase(int investorId, double amount) throws SQLException {
        return walletDAO.deductForAssetPurchase(investorId, amount);
    }

    // ASSET SALE

    public boolean creditAssetSale(int investorId, double amount, String category) throws SQLException {
        return walletDAO.creditAssetSale(investorId, amount, category);
    }

    public boolean creditAssetSale(int investorId, double amount) throws SQLException {
        return walletDAO.creditAssetSale(investorId, amount);
    }

    // REFUND

    public boolean refund(int investorId, double amount, String category) throws SQLException {
        return walletDAO.refund(investorId, amount, category);
    }

    // ADMIN ADJUSTMENT

    public boolean adminAdjustment(int investorId, double amount, String reason) throws SQLException {
        return walletDAO.adminAdjustment(investorId, amount, reason);
    }

    // IN-TRANSACTION VARIANTS (caller manages commit/rollback)

    public boolean deposit(Connection conn, int investorId, double amount, String category) throws SQLException {
        return walletDAO.deposit(conn, investorId, amount, category);
    }

    public boolean withdraw(Connection conn, int investorId, double amount, String category) throws SQLException {
        return walletDAO.withdraw(conn, investorId, amount, category);
    }

    public boolean deductForAssetPurchase(Connection conn, int investorId, double amount, String category) throws SQLException {
        return walletDAO.deductForAssetPurchase(conn, investorId, amount, category);
    }

    public boolean creditAssetSale(Connection conn, int investorId, double amount, String category) throws SQLException {
        return walletDAO.creditAssetSale(conn, investorId, amount, category);
    }

    public boolean creditDividend(Connection conn, int investorId, double amount, String category) throws SQLException {
        return walletDAO.creditDividend(conn, investorId, amount, category);
    }
}
