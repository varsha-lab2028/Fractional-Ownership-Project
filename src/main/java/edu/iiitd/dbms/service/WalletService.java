package edu.iiitd.dbms.service;

import edu.iiitd.dbms.data_access.WalletTransactionDAO;
import edu.iiitd.dbms.dto.WalletTransactionDTO;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class WalletService {

    private final WalletTransactionDAO walletDAO;

    public WalletService() {
        this.walletDAO = new WalletTransactionDAO();
    }

    //Return the investor's current wallet balance
    public double getWalletBalance(int investorId) throws SQLException {
        return walletDAO.getStoredWalletBalance(investorId);
    }

    //Return the investor's full transaction history as rich DTOs, newest first
    public List<WalletTransactionDTO> getTransactionHistory(int investorId) throws SQLException {
        return walletDAO.getTransactionHistory(investorId);
    }

    public boolean deposit(int investorId, double amount, String category) throws SQLException {
        return walletDAO.deposit(investorId, amount, category);
    }

    /*public boolean deposit(int investorId, double amount) throws SQLException {
        return walletDAO.deposit(investorId, amount);
    }*/

    public boolean withdraw(int investorId, double amount, String category) throws SQLException {
        return walletDAO.withdraw(investorId, amount, category);
    }

    /*public boolean withdraw(int investorId, double amount) throws SQLException {
        return walletDAO.withdraw(investorId, amount);
    }*/

    public boolean creditDividend(int investorId, double amount, String category) throws SQLException {
        return walletDAO.creditDividend(investorId, amount, category);
    }

    /*public boolean creditDividend(int investorId, double amount) throws SQLException {
        return walletDAO.creditDividend(investorId, amount);
    }*/

    public boolean deductForAssetPurchase(int investorId, double amount, String category) throws SQLException {
        return walletDAO.deductForAssetPurchase(investorId, amount, category);
    }

    /*public boolean deductForAssetPurchase(int investorId, double amount) throws SQLException {
        return walletDAO.deductForAssetPurchase(investorId, amount);
    }*/

    public boolean creditAssetSale(int investorId, double amount, String category) throws SQLException {
        return walletDAO.creditAssetSale(investorId, amount, category);
    }

    /*public boolean creditAssetSale(int investorId, double amount) throws SQLException {
        return walletDAO.creditAssetSale(investorId, amount);
    }*/

    public boolean refund(int investorId, double amount, String category) throws SQLException {
        return walletDAO.refund(investorId, amount, category);
    }

    public boolean adminAdjustment(int investorId, double amount, String reason) throws SQLException {
        return walletDAO.adminAdjustment(investorId, amount, reason);
    }


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
