package edu.iiitd.dbms.service;

import edu.iiitd.dbms.config.ServerConnector;
import edu.iiitd.dbms.data_access.*;
import edu.iiitd.dbms.domain.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * TradingService — handles the full buy / sell / execute flow.
 *
 * Task 7.1: executeTrade() uses ONE shared Connection for all DB operations.
 *   - conn.setAutoCommit(false) at the start
 *   - conn.commit()             on success
 *   - conn.rollback()           in every catch block
 *   - conn.setAutoCommit(true) + conn.close() in finally
 *
 * Task 7.4: FOR UPDATE locking is applied inside TradeOrderDAO.findByOrderId(conn, id)
 *   and OwnershipDAO.findOwnership(conn, investorId, assetId).
 *   This prevents two concurrent sessions from matching the same order or
 *   simultaneously reading stale ownership counts (lost-update anomaly).
 */
public class TradingService {

    private final TradeOrderDAO       tradeOrderDAO;
    private final TradeDAO            tradeDAO;
    private final OwnershipDAO        ownershipDAO;
    private final OwnershipHistoryDAO ownershipHistoryDAO;
    private final WalletTransactionDAO walletTransactionDAO;

    public TradingService() {
        this.tradeOrderDAO        = new TradeOrderDAO();
        this.tradeDAO             = new TradeDAO();
        this.ownershipDAO         = new OwnershipDAO();
        this.ownershipHistoryDAO  = new OwnershipHistoryDAO();
        this.walletTransactionDAO = new WalletTransactionDAO();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Place a BUY order
    // ─────────────────────────────────────────────────────────────────────────
    public TradeOrder placeBuyOrder(int investorId, int assetId,
                                    int units, double pricePerUnit) throws Exception {
        if (units <= 0)        throw new Exception("Units must be greater than 0.");
        if (pricePerUnit <= 0) throw new Exception("Price must be greater than 0.");

        double required = units * pricePerUnit;
        double balance  = walletTransactionDAO.getStoredWalletBalance(investorId);
        if (balance < required)
            throw new Exception(String.format(
                "Insufficient wallet balance. Required: $%.2f, Available: $%.2f",
                required, balance));

        try {
            int newId = nextOrderId();
            TradeOrder order = new TradeOrder(
                newId, investorId, assetId, "BUY",
                pricePerUnit, units, LocalDate.now(), "OPEN");
            tradeOrderDAO.insertOrder(order);
            return order;
        } catch (SQLException e) {
            throw new Exception("Failed to place buy order: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Place a SELL order
    // ─────────────────────────────────────────────────────────────────────────
    public TradeOrder placeSellOrder(int investorId, int assetId,
                                     int units, double pricePerUnit) throws Exception {
        if (units <= 0)        throw new Exception("Units must be greater than 0.");
        if (pricePerUnit <= 0) throw new Exception("Price must be greater than 0.");

        Ownership holding = ownershipDAO.findOwnership(investorId, assetId);
        int currentUnits = (holding != null && holding.getUnitsHeld() != null)
                           ? holding.getUnitsHeld() : 0;
        if (currentUnits < units)
            throw new Exception(String.format(
                "Cannot sell %d units — you only hold %d units of asset %d.",
                units, currentUnits, assetId));

        try {
            int newId = nextOrderId();
            TradeOrder order = new TradeOrder(
                newId, investorId, assetId, "SELL",
                pricePerUnit, units, LocalDate.now(), "OPEN");
            tradeOrderDAO.insertOrder(order);
            return order;
        } catch (SQLException e) {
            throw new Exception("Failed to place sell order: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Execute a trade — matches an open buy order against an open sell order.
    //
    // Task 7.1  — one shared Connection, setAutoCommit(false), commit/rollback/finally
    // Task 7.2  — all validations listed in the spec
    // Task 7.3  — all DB operations listed in the spec
    // Task 7.4  — FOR UPDATE is applied inside findByOrderId() and findOwnership()
    // Task 7.5  — wallet debit/credit happen inside the same transaction
    // ─────────────────────────────────────────────────────────────────────────
    public Trade executeTrade(int buyOrderId, int sellOrderId) throws Exception {
        Connection conn = null;
        try {
            // 7.1 — one shared connection, autoCommit off
            conn = ServerConnector.DBConnection();
            conn.setAutoCommit(false);

            // ── 7.2 VALIDATION ────────────────────────────────────────────────

            // Fetch both orders — each call issues SELECT … FOR UPDATE (7.4)
            TradeOrder buyOrder  = tradeOrderDAO.findByOrderId(conn, buyOrderId);
            TradeOrder sellOrder = tradeOrderDAO.findByOrderId(conn, sellOrderId);

            if (buyOrder == null)
                throw new Exception("Buy order #" + buyOrderId + " not found.");
            if (sellOrder == null)
                throw new Exception("Sell order #" + sellOrderId + " not found.");

            if (!"BUY".equalsIgnoreCase(buyOrder.getOrderType()))
                throw new Exception("Order #" + buyOrderId + " is not a BUY order.");
            if (!"SELL".equalsIgnoreCase(sellOrder.getOrderType()))
                throw new Exception("Order #" + sellOrderId + " is not a SELL order.");

            if (!"OPEN".equalsIgnoreCase(buyOrder.getStatus()))
                throw new Exception("Buy order #" + buyOrderId + " is not OPEN. Status: " + buyOrder.getStatus());
            if (!"OPEN".equalsIgnoreCase(sellOrder.getStatus()))
                throw new Exception("Sell order #" + sellOrderId + " is not OPEN. Status: " + sellOrder.getStatus());

            if (!buyOrder.getAssetId().equals(sellOrder.getAssetId()))
                throw new Exception("Orders are for different assets — cannot match.");

            if (!buyOrder.getUnits().equals(sellOrder.getUnits()))
                throw new Exception(String.format(
                    "Partial fills are not supported. Buy units: %d, Sell units: %d.",
                    buyOrder.getUnits(), sellOrder.getUnits()));

            if (buyOrder.getInvestorId().equals(sellOrder.getInvestorId()))
                throw new Exception("Buyer and seller cannot be the same investor.");

            // Validate seller owns the asset and has enough units — FOR UPDATE lock (7.4)
            Ownership sellerHolding = ownershipDAO.findOwnership(conn, sellOrder.getInvestorId(), sellOrder.getAssetId());
            if (sellerHolding == null || sellerHolding.getUnitsHeld() == null || sellerHolding.getUnitsHeld() == 0)
                throw new Exception("Seller does not own asset #" + sellOrder.getAssetId() + ".");
            if (sellerHolding.getUnitsHeld() < sellOrder.getUnits())
                throw new Exception(String.format(
                    "Seller only holds %d units but is trying to sell %d.",
                    sellerHolding.getUnitsHeld(), sellOrder.getUnits()));

            // Validate buyer wallet
            int    assetId       = buyOrder.getAssetId();
            int    tradeUnits    = buyOrder.getUnits();
            double tradePrice    = sellOrder.getPrice();
            double totalCost     = tradeUnits * tradePrice;

            double buyerBalance = walletTransactionDAO.getStoredWalletBalance(buyOrder.getInvestorId());
            if (buyerBalance < totalCost)
                throw new Exception(String.format(
                    "Buyer has insufficient wallet balance. Required: $%.2f, Available: $%.2f",
                    totalCost, buyerBalance));

            // ── 7.3 OPERATIONS ────────────────────────────────────────────────

            // Insert trade record
            int newTradeId = nextTradeId();
            Trade trade = new Trade(newTradeId, tradePrice, tradeUnits, LocalDate.now(), buyOrderId, sellOrderId);
            tradeDAO.insertTrade(conn, trade);

            // Update buyer ownership (add units) — FOR UPDATE already held on buyer's row if it exists
            int buyerPrev = getUnits(conn, buyOrder.getInvestorId(), assetId);
            ownershipDAO.updateUnits(conn, buyOrder.getInvestorId(), assetId, buyerPrev + tradeUnits);
            ownershipHistoryDAO.insert(conn, buyOrder.getInvestorId(), assetId,
                    buyerPrev, buyerPrev + tradeUnits, "TRADE_BUY", newTradeId, null);

            // Update seller ownership (subtract units)
            int sellerPrev = sellerHolding.getUnitsHeld();
            ownershipDAO.updateUnits(conn, sellOrder.getInvestorId(), assetId, sellerPrev - tradeUnits);
            ownershipHistoryDAO.insert(conn, sellOrder.getInvestorId(), assetId,
                    sellerPrev, sellerPrev - tradeUnits, "TRADE_SELL", newTradeId, null);
            ownershipDAO.deleteIfZero(conn, sellOrder.getInvestorId(), assetId);

            // Update order statuses to MATCHED
            tradeOrderDAO.updateStatus(conn, buyOrderId,  "MATCHED");
            tradeOrderDAO.updateStatus(conn, sellOrderId, "MATCHED");

            // 7.5 — wallet debit/credit inside same transaction
            walletTransactionDAO.deductForAssetPurchase(conn, buyOrder.getInvestorId(),  totalCost, "Trade Execution");
            walletTransactionDAO.creditAssetSale(conn,        sellOrder.getInvestorId(), totalCost, "Trade Execution");

            // 7.1 — commit on success
            conn.commit();
            return trade;

        } catch (SQLException e) {
            // 7.1 — rollback on SQL failure (covers wallet rollback automatically — 7.5)
            if (conn != null) { try { conn.rollback(); } catch (SQLException ignored) {} }
            String msg = e.getMessage();
            if (msg != null && msg.toLowerCase().contains("units_held"))
                throw new Exception("Trade blocked by DB trigger: seller does not have enough units.");
            throw new Exception("Trade execution failed (SQL): " + msg);

        } catch (Exception e) {
            // 7.1 — rollback on any other failure
            if (conn != null) { try { conn.rollback(); } catch (SQLException ignored) {} }
            throw e;

        } finally {
            // 7.1 — always restore autoCommit and close
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Query helpers
    // ─────────────────────────────────────────────────────────────────────────
    public List<TradeOrder> getOpenOrdersForAsset(int assetId) throws Exception {
        try { return tradeOrderDAO.listOpenOrdersForAsset(assetId); }
        catch (SQLException e) { throw new Exception("Failed to fetch open orders: " + e.getMessage()); }
    }

    public List<TradeOrder> getOrdersByInvestor(int investorId) throws Exception {
        try { return tradeOrderDAO.listOrdersByInvestor(investorId); }
        catch (SQLException e) { throw new Exception("Failed to fetch orders: " + e.getMessage()); }
    }

    public List<Trade> getAllTrades() throws Exception {
        try { return tradeDAO.listTrades(); }
        catch (SQLException e) { throw new Exception("Failed to fetch trades: " + e.getMessage()); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Returns units held, re-using the shared transaction connection. */
    private int getUnits(Connection conn, int investorId, int assetId) throws SQLException {
        // Note: findOwnership(conn, …) issues FOR UPDATE — safe to call again;
        // Postgres upgrades the existing lock rather than deadlocking.
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
