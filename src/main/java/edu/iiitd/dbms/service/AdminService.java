package edu.iiitd.dbms.service;

import edu.iiitd.dbms.data_access.*;
import edu.iiitd.dbms.domain.*;
import edu.iiitd.dbms.dto.InvestorMarketView.MarketViewRow;

import java.sql.SQLException;
import java.util.List;

public class AdminService {
    private final AdminDAO           adminDAO;
    private final InvestorDAO        investorDAO;
    private final AssetDAO           assetDAO;
    private final IpoDAO             ipoDAO;
    private final TradeDAO           tradeDAO;
    private final TradeOrderDAO      tradeOrderDAO;
    private final OwnershipHistoryDAO historyDAO;

    public AdminService() {
        this.adminDAO    = new AdminDAO();
        this.investorDAO = new InvestorDAO();
        this.assetDAO    = new AssetDAO();
        this.ipoDAO      = new IpoDAO();
        this.tradeDAO    = new TradeDAO();
        this.tradeOrderDAO = new TradeOrderDAO();
        this.historyDAO  = new OwnershipHistoryDAO();
    }

    //View investors
    public List<Investor> getAllInvestors() throws Exception {
        try {
            return investorDAO.listInvestors();
        } catch (SQLException e) {
            throw new Exception("Failed to fetch investors: " + e.getMessage());
        }
    }

    //View assets
    public List<Asset> getAllAssets() throws Exception {
        try {
            return assetDAO.listAssets();
        } catch (SQLException e) {
            throw new Exception("Failed to fetch assets: " + e.getMessage());
        }
    }

    //View IPOs
    public List<IPO> getAllIPOs() throws Exception {
        try {
            return ipoDAO.listIPOs();
        } catch (SQLException e) {
            throw new Exception("Failed to fetch IPOs: " + e.getMessage());
        }
    }

    //View IPOs as market rows (richer DTO with asset name + price)
    public List<MarketViewRow> getAllMarketRows() throws Exception {
        try {
            return ipoDAO.getAllMarketRows();
        } catch (SQLException e) {
            throw new Exception("Failed to fetch market rows: " + e.getMessage());
        }
    }

    //View all executed trades
    public List<Trade> getAllTrades() throws Exception {
        try {
            return tradeDAO.listTrades();
        } catch (SQLException e) {
            throw new Exception("Failed to fetch trades: " + e.getMessage());
        }
    }

    //View all trade orders
    public List<TradeOrder> getAllOrders() throws Exception {
        try {
            return tradeOrderDAO.listOrders();
        } catch (SQLException e) {
            throw new Exception("Failed to fetch orders: " + e.getMessage());
        }
    }

    //View ownership history for an investor
    public List<OwnershipHistory> getOwnershipHistory(int investorId) throws Exception {
        try {
            return historyDAO.listByInvestorId(investorId);
        } catch (SQLException e) {
            throw new Exception("Failed to fetch ownership history: " + e.getMessage());
        }
    }

    //View all admins
    public List<Admin> getAllAdmins() throws Exception {
        try {
            return adminDAO.listAdmins();
        } catch (SQLException e) {
            throw new Exception("Failed to fetch admins: " + e.getMessage());
        }
    }
}