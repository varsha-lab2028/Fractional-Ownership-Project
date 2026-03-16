package edu.iiitd.dbms.service;

import edu.iiitd.dbms.config.ServerConnector;
import edu.iiitd.dbms.data_access.*;
import edu.iiitd.dbms.domain.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * TradingService — orchestrates the full buy/sell/execute flow.
 *
 * Flow B (Task 5):
 *   1. placeBuyOrder  — INSERT into trade_order (type=BUY,  status=OPEN)
 *   2. placeSellOrder — INSERT into trade_order (type=SELL, status=OPEN)
 *   3. executeTrade   — matches a buy+sell order, INSERTs into trade,
 *                       UPDATEs ownership for both parties,
 *                       marks both orders FILLED,
 *                       debits/credits wallets.
 *                       Trigger 2 (AFTER UPDATE on ownership) then
 *                       auto-writes to ownership_history automatically.
 *
 * Trigger integration:
 *   Trigger 1 (BEFORE UPDATE ownership): fires inside executeTrade when
 *     updateUnits() is called — rejects the UPDATE if new units < 0,
 *     causing a SQLException which we surface as a meaningful error.
 *   Trigger 2 (AFTER UPDATE ownership): fires automatically — no code
 *     needed here; the history is written by the DB itself.
 */
public class TradingService {

    private final TradeOrderDAO    tradeOrderDAO;
    private final TradeDAO         tradeDAO;
    private final OwnershipDAO     ownershipDAO;
    private final InvestorDAO      investorDAO;

    public TradingService() {
        this.tradeOrderDAO = new TradeOrderDAO();
        this.tradeDAO      = new TradeDAO();
        this.ownershipDAO  = new OwnershipDAO();
        this.investorDAO   = new InvestorDAO();
    }

    // ── Flow B step 1: place a buy order ─────────────────────────────────────
    public TradeOrder placeBuyOrder(int investorId, int assetId,
                                    int units, double pricePerUnit) throws Exception {
        if (units <= 0)        throw new Exception("Units must be greater than 0.");
        if (pricePerUnit <= 0) throw new Exception("Price must be greater than 0.");

        // Check investor has sufficient wallet funds
        double required = units * pricePerUnit;
        double balance  = investorDAO.getWalletBalance(investorId);
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

    // ── Flow B step 2: place a sell order ────────────────────────────────────
    public TradeOrder placeSellOrder(int investorId, int assetId,
                                     int units, double pricePerUnit) throws Exception {
        if (units <= 0)        throw new Exception("Units must be greater than 0.");
        if (pricePerUnit <= 0) throw new Exception("Price must be greater than 0.");

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

    // ── Flow B step 3: execute trade — matches buy + sell ────────────────────
    /**
     * Runs the full trade execution inside a single DB transaction:
     *   INSERT into trade
     *   UPDATE ownership for buyer  (Trigger 2 fires → auto-logs history)
     *   UPDATE ownership for seller (Trigger 2 fires → auto-logs history)
     *     └─ If Trigger 1 detects seller would go negative → rolls back
     *   UPDATE order status FILLED for both orders
     *   Debit buyer wallet, credit seller wallet
     */
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

            // 1. INSERT into trade
            int newTradeId = nextTradeId();
            Trade trade = new Trade(newTradeId, tradePrice, tradeUnits,
                                    LocalDate.now(), buyOrderId, sellOrderId);
            tradeDAO.insertTrade(conn, trade);

            // 2. UPDATE buyer ownership (Trigger 2 fires here → auto-logs history)
            int buyerPrev  = getUnits(conn, buyOrder.getInvestorId(), assetId);
            ownershipDAO.updateUnits(conn, buyOrder.getInvestorId(), assetId,
                                     buyerPrev + tradeUnits);

            // 3. UPDATE seller ownership (Trigger 1 + 2 fire here)
            //    Trigger 1 will throw if newUnits < 0 — handled by catch below
            int sellerPrev = getUnits(conn, sellOrder.getInvestorId(), assetId);
            ownershipDAO.updateUnits(conn, sellOrder.getInvestorId(), assetId,
                                     sellerPrev - tradeUnits);

            // 4. Mark orders FILLED
            tradeOrderDAO.updateStatus(conn, buyOrderId,  "FILLED");
            tradeOrderDAO.updateStatus(conn, sellOrderId, "FILLED");

            // 5. Debit buyer wallet, credit seller wallet
            double totalCost = tradeUnits * tradePrice;
            investorDAO.updateWalletBalance(conn, buyOrder.getInvestorId(),  -totalCost);
            investorDAO.updateWalletBalance(conn, sellOrder.getInvestorId(), +totalCost);

            conn.commit();
            return trade;

        } catch (SQLException e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ignored) {} }
            // Surface Trigger 1 violations clearly
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

    // ── List open orders for an asset (for matching UI) ───────────────────────
    public List<TradeOrder> getOpenOrdersForAsset(int assetId) throws Exception {
        try {
            return tradeOrderDAO.listOpenOrdersForAsset(assetId);
        } catch (SQLException e) {
            throw new Exception("Failed to fetch open orders: " + e.getMessage());
        }
    }

    // ── List all orders for an investor ───────────────────────────────────────
    public List<TradeOrder> getOrdersByInvestor(int investorId) throws Exception {
        try {
            return tradeOrderDAO.listOrdersByInvestor(investorId);
        } catch (SQLException e) {
            throw new Exception("Failed to fetch orders: " + e.getMessage());
        }
    }

    // ── List all executed trades ───────────────────────────────────────────────
    public List<Trade> getAllTrades() throws Exception {
        try {
            return tradeDAO.listTrades();
        } catch (SQLException e) {
            throw new Exception("Failed to fetch trades: " + e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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