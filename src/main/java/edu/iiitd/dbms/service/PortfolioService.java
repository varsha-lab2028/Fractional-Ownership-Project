package edu.iiitd.dbms.service;

import edu.iiitd.dbms.data_access.InvestorDAO;
import edu.iiitd.dbms.data_access.OwnershipHistoryDAO;
import edu.iiitd.dbms.data_access.PortfolioDAO;
import edu.iiitd.dbms.data_access.WalletTransactionDAO;
import edu.iiitd.dbms.domain.OwnershipHistory;
import edu.iiitd.dbms.dto.AssetHoldingDTO;

import java.sql.SQLException;
import java.util.List;

public class PortfolioService {

    private final PortfolioDAO portfolioDAO;
    private final InvestorDAO  investorDAO;
    private final OwnershipHistoryDAO historyDAO;
    private final WalletTransactionDAO walletTransactionDAO;

    public PortfolioService() {
        this.portfolioDAO = new PortfolioDAO();
        this.investorDAO  = new InvestorDAO();
        this.historyDAO   = new OwnershipHistoryDAO();
        this.walletTransactionDAO = new WalletTransactionDAO();
    }

    //list investor holdings
    public List<AssetHoldingDTO> listInvestorHoldings(int investorId) throws Exception {
        try {
            return portfolioDAO.getInvestorHoldings(investorId);
        } catch (SQLException e) {
            throw new Exception("Failed to fetch holdings: " + e.getMessage());
        }
    }

    //current portfolio value
    public double getCurrentPortfolioValue(int investorId) throws Exception {
        try {
            return investorDAO.getCurrentPortfolioValue(investorId);
        } catch (SQLException e) {
            throw new Exception("Failed to calculate portfolio value: " + e.getMessage());
        }
    }

    //total capital invested
    public double getTotalInvested(int investorId) throws Exception {
        try {
            return investorDAO.getTotalInvestedAmount(investorId);
        } catch (SQLException e) {
            throw new Exception("Failed to calculate total invested: " + e.getMessage());
        }
    }

    //P&L amount
    public double getProfitLoss(int investorId) throws Exception {
        double currentValue = getCurrentPortfolioValue(investorId);
        double totalInvested = getTotalInvested(investorId);
        return currentValue - totalInvested;
    }

    //P&L percentage
    public double getProfitLossPercentage(int investorId) throws Exception {
        double totalInvested = getTotalInvested(investorId);
        if (totalInvested == 0) return 0.0;
        return (getProfitLoss(investorId) / totalInvested) * 100.0;
    }

    //wallet balance
    public double getWalletBalance(int investorId) throws Exception {
        try {
            return walletTransactionDAO.getStoredWalletBalance(investorId);
        } catch (SQLException e) {
            throw new Exception("Failed to fetch wallet balance: " + e.getMessage());
        }
    }

    //show ownership history (written by Trigger 2)
    public List<OwnershipHistory> getOwnershipHistory(int investorId) throws Exception {
        try {
            return historyDAO.listByInvestorId(investorId);
        } catch (SQLException e) {
            throw new Exception("Failed to fetch ownership history: " + e.getMessage());
        }
    }
}