package edu.iiitd.dbms.service;

import edu.iiitd.dbms.data_access.WalletTransactionDAO;

import java.sql.SQLException;
import java.util.List;

public class WalletService {
    private final WalletTransactionDAO walletDAO;

    public WalletService() {
        this.walletDAO = new WalletTransactionDAO();
    }

    public boolean depositToWallet(int investorId, double amount) throws SQLException {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be greater than 0.");
        }
        return walletDAO.recordTransaction(
                investorId,
                amount,
                "DEPOSIT",
                "Bank Transfer"
        );
    }

    public boolean creditDividend(int investorId, double amount) throws SQLException {
        if (amount <= 0) {
            throw new IllegalArgumentException("Dividend amount must be greater than 0.");
        }

        return walletDAO.recordTransaction(
                investorId,
                amount,
                "DIVIDEND",
                "Yield Payout"
        );
    }

    public boolean deductForAssetPurchase(int investorId, double amount) throws SQLException {
        if (amount <= 0) {
            throw new IllegalArgumentException("Purchase amount must be greater than 0.");
        }

        return walletDAO.recordTransaction(
                investorId,
                -amount,
                "ASSET_PURCHASE",
                "Secondary Market Order"
        );
    }

    public double getWalletBalance(int investorId) throws SQLException {
        return walletDAO.getStoredWalletBalance(investorId);
    }

    public List<String> getWalletTransactions(int investorId) throws SQLException {
        return walletDAO.getWalletTransactionsByInvestor(investorId);
    }

}
