package edu.iiitd.dbms.service;

import edu.iiitd.dbms.config.ServerConnector;
import edu.iiitd.dbms.data_access.*;
import edu.iiitd.dbms.domain.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

//TradingService - this enables the full buy/sell/execute flow.
public class TradingService {
    private final TradeOrderDAO    tradeOrderDAO;
    private final TradeDAO         tradeDAO;
    private final OwnershipDAO     ownershipDAO;
    private final InvestorDAO      investorDAO;
    private final WalletTransactionDAO walletTransactionDAO;

    public TradingService() {
        this.tradeOrderDAO = new TradeOrderDAO();
        this.tradeDAO      = new TradeDAO();
        this.ownershipDAO  = new OwnershipDAO();
        this.investorDAO   = new InvestorDAO();
        this.walletTransactionDAO = new WalletTransactionDAO();
    }

    //place a buy order
    public TradeOrder placeBuyOrder(int investorId, int assetId,
                                    int units, double pricePerUnit) throws Exception {
        if (units <= 0){throw new Exception("Units must be greater than 0.");}
        if (pricePerUnit <= 0){throw new Exception("Price must be greater than 0.");}

        // Check investor has sufficient wallet funds
        double required = units * pricePerUnit;
        double balance  = walletTransactionDAO.getStoredWalletBalance(investorId);
        if (balance < required) {
            throw new Exception(String.format(
                "Insufficient wallet balance. Required: $%.2f, Available: $%.2f",
                required, balance));
        }

        try {
            int newId = nextOrderId();
            TradeOrder order = new TradeOrder(
                newId, investorId, assetId, "BUY",
                pricePerUnit, units, LocalDate.now(), "OPEN"
            );
            tradeOrderDAO.insertOrder(order);
            return order;
        } catch (SQLException e) {
            throw new Exception("Failed to place buy order: " + e.getMessage());
        }
    }

    //place a sell order
    public TradeOrder placeSellOrder(int investorId, int assetId,
                                     int units, double pricePerUnit) throws Exception {
        if (units <= 0){throw new Exception("Units must be greater than 0.");}
        if (pricePerUnit <= 0){throw new Exception("Price must be greater than 0.");}

        // Check investor actually holds enough units
        Ownership holding = ownershipDAO.findOwnership(investorId, assetId);
        int currentUnits = (holding != null && holding.getUnitsHeld() != null)
                           ? holding.getUnitsHeld() : 0;
        if (currentUnits < units) {
            throw new Exception(String.format(
                "Cannot sell %d units — you only hold %d units of asset %d.",
                units, currentUnits, assetId));
        }

        try {
            int newId = nextOrderId();
            TradeOrder order = new TradeOrder(
                newId, investorId, assetId, "SELL",
                pricePerUnit, units, LocalDate.now(), "OPEN"
            );
            tradeOrderDAO.insertOrder(order);
            return order;
        } catch (SQLException e) {
            throw new Exception("Failed to place sell order: " + e.getMessage());
        }
    }

    //execute trade — matches buy + sell
    public Trade executeTrade(int buyOrderId, int sellOrderId) throws Exception {
        Connection conn = null;
        try {
            conn = ServerConnector.DBConnection();
            conn.setAutoCommit(false);

            // Load both orders
            TradeOrder buyOrder  = tradeOrderDAO.findByOrderId(conn, buyOrderId);
            TradeOrder sellOrder = tradeOrderDAO.findByOrderId(conn, sellOrderId);

            if (buyOrder == null)  throw new Exception("Buy order #"  + buyOrderId  + " not found.");
            if (sellOrder == null) throw new Exception("Sell order #" + sellOrderId + " not found.");

            if (!"OPEN".equalsIgnoreCase(buyOrder.getStatus()))
                throw new Exception("Buy order #"  + buyOrderId  + " is not OPEN.");
            if (!"OPEN".equalsIgnoreCase(sellOrder.getStatus()))
                throw new Exception("Sell order #" + sellOrderId + " is not OPEN.");

            if (!buyOrder.getAssetId().equals(sellOrder.getAssetId()))
                throw new Exception("Orders are for different assets — cannot match.");

            int    assetId    = buyOrder.getAssetId();
            int    tradeUnits = Math.min(buyOrder.getUnits(), sellOrder.getUnits());
            double tradePrice = sellOrder.getPrice(); // use seller's asking price

            int newTradeId = nextTradeId();
            Trade trade = new Trade(newTradeId, tradePrice, tradeUnits,
                                    LocalDate.now(), buyOrderId, sellOrderId);
            tradeDAO.insertTrade(conn, trade);


            int buyerPrev  = getUnits(conn, buyOrder.getInvestorId(), assetId);
            ownershipDAO.updateUnits(conn, buyOrder.getInvestorId(), assetId,
                                     buyerPrev + tradeUnits);


            int sellerPrev = getUnits(conn, sellOrder.getInvestorId(), assetId);
            ownershipDAO.updateUnits(conn, sellOrder.getInvestorId(), assetId,
                                     sellerPrev - tradeUnits);

            tradeOrderDAO.updateStatus(conn, buyOrderId,  "FILLED");
            tradeOrderDAO.updateStatus(conn, sellOrderId, "FILLED");

            /*double totalCost = tradeUnits * tradePrice;
            investorDAO.updateWalletBalance(conn, buyOrder.getInvestorId(),  -totalCost);
            investorDAO.updateWalletBalance(conn, sellOrder.getInvestorId(), +totalCost);*/

            double totalCost = tradeUnits * tradePrice;
            // Buyer pays money results in negative wallet transaction
            walletTransactionDAO.insertWalletTransaction(
                    conn,
                    buyOrder.getInvestorId(),
                    -totalCost,
                    "ASSET_PURCHASE",
                    "Trade Execution"
            );
            // Seller receives money results in positive wallet transaction
            walletTransactionDAO.insertWalletTransaction(
                    conn,
                    sellOrder.getInvestorId(),
                    totalCost,
                    "ASSET_SALE",
                    "Trade Execution"
            );

            conn.commit();
            return trade;

        } catch (SQLException e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ignored) {} }
            String msg = e.getMessage();
            if (msg != null && msg.toLowerCase().contains("units_held")) {
                throw new Exception("Trade blocked by DB trigger: seller does not have enough units.");
            }
            throw new Exception("Trade execution failed: " + msg);
        } catch (Exception e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ignored) {} }
            throw e;
        } finally {
            if (conn != null) { try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {} }
        }
    }

    //List open orders for an asset
    public List<TradeOrder> getOpenOrdersForAsset(int assetId) throws Exception {
        try {
            return tradeOrderDAO.listOpenOrdersForAsset(assetId);
        } catch (SQLException e) {
            throw new Exception("Failed to fetch open orders: " + e.getMessage());
        }
    }

    //List all orders for an investor
    public List<TradeOrder> getOrdersByInvestor(int investorId) throws Exception {
        try {
            return tradeOrderDAO.listOrdersByInvestor(investorId);
        } catch (SQLException e) {
            throw new Exception("Failed to fetch orders: " + e.getMessage());
        }
    }

    //List all executed trades
    public List<Trade> getAllTrades() throws Exception {
        try {
            return tradeDAO.listTrades();
        } catch (SQLException e) {
            throw new Exception("Failed to fetch trades: " + e.getMessage());
        }
    }

    //Helper methods
    private int getUnits(Connection conn, int investorId, int assetId) throws SQLException {
        Ownership o = ownershipDAO.findOwnership(conn, investorId, assetId);
        return (o != null && o.getUnitsHeld() != null) ? o.getUnitsHeld() : 0;
    }

    private int nextOrderId() throws SQLException {
        List<TradeOrder> all = tradeOrderDAO.listOrders();
        return all.stream().mapToInt(TradeOrder::getOrderId).max().orElse(0) + 1;
    }

    private int nextTradeId() throws SQLException {
        List<Trade> all = tradeDAO.listTrades();
        return all.stream().mapToInt(Trade::getTradeId).max().orElse(0) + 1;
    }
}