package edu.iiitd.dbms.test;

import edu.iiitd.dbms.config.ServerConnector;
import edu.iiitd.dbms.data_access.*;
import edu.iiitd.dbms.domain.*;
import edu.iiitd.dbms.service.TradingService;

import java.sql.*;
import java.time.LocalDate;
import java.util.List;

/**
 * MainTest — Task 7.6 demo runner.
 *
 * Demonstrates:
 *   1. A SUCCESSFUL trade end-to-end (ownership changes, wallets update, orders → MATCHED)
 *   2. A FAILED trade with rollback (seller has insufficient units — nothing changes)
 *
 * Run this class directly.  It prints before/after state for both scenarios.
 *
 * Prerequisites:
 *   - db.properties filled in with valid Supabase credentials
 *   - supabase_schema_and_data.sql + seed files already run on Supabase
 */
public class MainTest {

    private static final TradingService    tradingService    = new TradingService();
    private static final TradeOrderDAO     tradeOrderDAO     = new TradeOrderDAO();
    private static final OwnershipDAO      ownershipDAO      = new OwnershipDAO();
    private static final WalletTransactionDAO walletDAO      = new WalletTransactionDAO();

    // ── Adjust these IDs to match live data in your Supabase instance ────────
    // Investor 1 (buyer)  holds some wallet balance
    // Investor 4 (seller) holds units of asset 2
    private static final int BUYER_ID   = 1;
    private static final int SELLER_ID  = 4;
    private static final int ASSET_ID   = 2;
    private static final int UNITS      = 5;
    private static final double PRICE   = 1000.00;

    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("  MainTest — Task 7.6 Trade Demo");
        System.out.println("=".repeat(60));

        try {
            runSuccessfulTradeTest();
            runFailedTradeRollbackTest();
        } catch (Exception e) {
            System.err.println("Unexpected error in MainTest: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // =========================================================================
    // TEST 1 — Successful trade
    // =========================================================================
    private static void runSuccessfulTradeTest() throws Exception {
        System.out.println("\n" + "─".repeat(60));
        System.out.println("TEST 1: Successful Trade");
        System.out.println("─".repeat(60));

        // ── Setup: create matching OPEN buy + sell orders ─────────────────────
        TradeOrder buyOrder  = tradingService.placeBuyOrder(BUYER_ID,  ASSET_ID, UNITS, PRICE);
        TradeOrder sellOrder = tradingService.placeSellOrder(SELLER_ID, ASSET_ID, UNITS, PRICE);
        System.out.printf("Created BUY  order #%d (investor %d, %d units @ $%.2f)%n",
                buyOrder.getOrderId(),  BUYER_ID,  UNITS, PRICE);
        System.out.printf("Created SELL order #%d (investor %d, %d units @ $%.2f)%n",
                sellOrder.getOrderId(), SELLER_ID, UNITS, PRICE);

        // ── Print state BEFORE trade ──────────────────────────────────────────
        System.out.println("\n--- BEFORE TRADE ---");
        printOwnership("Seller",  SELLER_ID, ASSET_ID);
        printOwnership("Buyer",   BUYER_ID,  ASSET_ID);
        printWalletBalance("Buyer",  BUYER_ID);
        printWalletBalance("Seller", SELLER_ID);
        printOrderStatus("BUY  order",  buyOrder.getOrderId());
        printOrderStatus("SELL order", sellOrder.getOrderId());

        // ── Execute trade ─────────────────────────────────────────────────────
        System.out.println("\n>>> Executing trade...");
        Trade trade = tradingService.executeTrade(buyOrder.getOrderId(), sellOrder.getOrderId());

        // ── Print state AFTER trade ───────────────────────────────────────────
        System.out.println("\n--- AFTER TRADE ---");
        System.out.printf("Trade row inserted: trade_id=%d, units=%d, price=%.2f, date=%s%n",
                trade.getTradeId(), trade.getTradeUnits(), trade.getTradePrice(), trade.getTradeDate());

        printOwnership("Buyer  (should have gained  units)", BUYER_ID,  ASSET_ID);
        printOwnership("Seller (should have lost    units)", SELLER_ID, ASSET_ID);
        printWalletBalance("Buyer  (should be lower)", BUYER_ID);
        printWalletBalance("Seller (should be higher)", SELLER_ID);
        printOrderStatus("BUY  order (should be MATCHED)", buyOrder.getOrderId());
        printOrderStatus("SELL order (should be MATCHED)", sellOrder.getOrderId());

        System.out.println("\n✅ TEST 1 PASSED — trade executed successfully.");
    }

    // =========================================================================
    // TEST 2 — Failed trade (rollback)
    // =========================================================================
    private static void runFailedTradeRollbackTest() throws Exception {
        System.out.println("\n" + "─".repeat(60));
        System.out.println("TEST 2: Failed Trade — Rollback Verification");
        System.out.println("─".repeat(60));

        // ── Setup: seller tries to sell MORE units than they own ──────────────
        // Force a sell order for 9999 units (seller definitely doesn't have that many)
        int badUnits = 9999;

        // Place buy order normally (buyer has wallet funds)
        TradeOrder badBuyOrder;
        try {
            badBuyOrder = tradingService.placeBuyOrder(BUYER_ID, ASSET_ID, badUnits, PRICE);
        } catch (Exception e) {
            System.out.println("(Buyer rejected at order placement — creating order via DAO directly for test)");
            // Insert directly so we can attempt the trade and prove rollback
            badBuyOrder = insertOrderDirectly(BUYER_ID, ASSET_ID, badUnits, PRICE, "BUY");
        }

        TradeOrder badSellOrder;
        try {
            badSellOrder = tradingService.placeSellOrder(SELLER_ID, ASSET_ID, badUnits, PRICE);
        } catch (Exception e) {
            System.out.println("(Seller rejected at order placement — creating order via DAO directly for test)");
            badSellOrder = insertOrderDirectly(SELLER_ID, ASSET_ID, badUnits, PRICE, "SELL");
        }

        // ── Snapshot state BEFORE failed attempt ─────────────────────────────
        System.out.println("\n--- BEFORE FAILED TRADE ATTEMPT ---");
        long tradeCountBefore    = countTrades();
        int  buyerUnitsBefore    = getUnits(BUYER_ID,  ASSET_ID);
        int  sellerUnitsBefore   = getUnits(SELLER_ID, ASSET_ID);
        double buyerBalBefore    = walletDAO.getStoredWalletBalance(BUYER_ID);
        double sellerBalBefore   = walletDAO.getStoredWalletBalance(SELLER_ID);
        String buyStatusBefore   = getOrderStatus(badBuyOrder.getOrderId());
        String sellStatusBefore  = getOrderStatus(badSellOrder.getOrderId());

        System.out.printf("Trade rows in DB:         %d%n",   tradeCountBefore);
        System.out.printf("Buyer  units (asset %d):  %d%n",   ASSET_ID, buyerUnitsBefore);
        System.out.printf("Seller units (asset %d):  %d%n",   ASSET_ID, sellerUnitsBefore);
        System.out.printf("Buyer  wallet:            $%.2f%n", buyerBalBefore);
        System.out.printf("Seller wallet:            $%.2f%n", sellerBalBefore);
        System.out.printf("BUY  order status:        %s%n",   buyStatusBefore);
        System.out.printf("SELL order status:        %s%n",   sellStatusBefore);

        // ── Attempt the trade — it MUST fail ─────────────────────────────────
        System.out.println("\n>>> Attempting trade with insufficient seller units...");
        boolean threwException = false;
        String  errorMessage   = "";
        try {
            tradingService.executeTrade(badBuyOrder.getOrderId(), badSellOrder.getOrderId());
        } catch (Exception e) {
            threwException = true;
            errorMessage   = e.getMessage();
        }

        // ── Confirm rollback — nothing should have changed ────────────────────
        System.out.println("\n--- AFTER FAILED TRADE ATTEMPT ---");
        long tradeCountAfter   = countTrades();
        int  buyerUnitsAfter   = getUnits(BUYER_ID,  ASSET_ID);
        int  sellerUnitsAfter  = getUnits(SELLER_ID, ASSET_ID);
        double buyerBalAfter   = walletDAO.getStoredWalletBalance(BUYER_ID);
        double sellerBalAfter  = walletDAO.getStoredWalletBalance(SELLER_ID);
        String buyStatusAfter  = getOrderStatus(badBuyOrder.getOrderId());
        String sellStatusAfter = getOrderStatus(badSellOrder.getOrderId());

        System.out.printf("Exception thrown:         %s%n", threwException);
        System.out.printf("Error message:            %s%n", errorMessage);
        System.out.println();
        System.out.printf("Trade rows (before/after):  %d / %d  %s%n",
                tradeCountBefore,  tradeCountAfter,  tradeCountAfter == tradeCountBefore    ? "✅ unchanged" : "❌ CHANGED");
        System.out.printf("Buyer  units (before/after): %d / %d  %s%n",
                buyerUnitsBefore,  buyerUnitsAfter,  buyerUnitsAfter  == buyerUnitsBefore   ? "✅ unchanged" : "❌ CHANGED");
        System.out.printf("Seller units (before/after): %d / %d  %s%n",
                sellerUnitsBefore, sellerUnitsAfter, sellerUnitsAfter == sellerUnitsBefore  ? "✅ unchanged" : "❌ CHANGED");
        System.out.printf("Buyer  wallet (before/after): $%.2f / $%.2f  %s%n",
                buyerBalBefore,    buyerBalAfter,    buyerBalAfter    == buyerBalBefore      ? "✅ unchanged" : "❌ CHANGED");
        System.out.printf("Seller wallet (before/after): $%.2f / $%.2f  %s%n",
                sellerBalBefore,   sellerBalAfter,   sellerBalAfter   == sellerBalBefore     ? "✅ unchanged" : "❌ CHANGED");
        System.out.printf("BUY  order status (before/after): %s / %s  %s%n",
                buyStatusBefore,   buyStatusAfter,   buyStatusAfter.equals(buyStatusBefore)  ? "✅ unchanged" : "❌ CHANGED");
        System.out.printf("SELL order status (before/after): %s / %s  %s%n",
                sellStatusBefore,  sellStatusAfter,  sellStatusAfter.equals(sellStatusBefore) ? "✅ unchanged" : "❌ CHANGED");

        boolean allUnchanged = threwException
                && tradeCountAfter  == tradeCountBefore
                && buyerUnitsAfter  == buyerUnitsBefore
                && sellerUnitsAfter == sellerUnitsBefore
                && buyerBalAfter    == buyerBalBefore
                && sellerBalAfter   == sellerBalBefore
                && buyStatusAfter.equals(buyStatusBefore)
                && sellStatusAfter.equals(sellStatusBefore);

        System.out.println();
        if (allUnchanged) {
            System.out.println("✅ TEST 2 PASSED — transaction rolled back correctly. DB state is unchanged.");
        } else {
            System.out.println("❌ TEST 2 FAILED — some state changed despite rollback. Check logs above.");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static void printOwnership(String label, int investorId, int assetId) throws SQLException {
        Ownership o = ownershipDAO.findOwnership(investorId, assetId);
        int units = (o != null && o.getUnitsHeld() != null) ? o.getUnitsHeld() : 0;
        System.out.printf("  %-42s → investor_id=%d, asset_id=%d, units_held=%d%n",
                label, investorId, assetId, units);
    }

    private static void printWalletBalance(String label, int investorId) throws SQLException {
        double bal = walletDAO.getStoredWalletBalance(investorId);
        System.out.printf("  %-42s → investor_id=%d, wallet_balance=$%.2f%n", label, investorId, bal);
    }

    private static void printOrderStatus(String label, int orderId) throws SQLException {
        String status = getOrderStatus(orderId);
        System.out.printf("  %-42s → order_id=%d, status=%s%n", label, orderId, status);
    }

    private static String getOrderStatus(int orderId) throws SQLException {
        TradeOrder o = tradeOrderDAO.findByOrderId(orderId);
        return o != null ? o.getStatus() : "NOT FOUND";
    }

    private static int getUnits(int investorId, int assetId) throws SQLException {
        Ownership o = ownershipDAO.findOwnership(investorId, assetId);
        return (o != null && o.getUnitsHeld() != null) ? o.getUnitsHeld() : 0;
    }

    private static long countTrades() throws SQLException {
        try (Connection c = ServerConnector.DBConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM trade");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    /** Inserts a trade order directly via DAO, bypassing service-level balance checks. */
    private static TradeOrder insertOrderDirectly(int investorId, int assetId,
                                                   int units, double price,
                                                   String type) throws SQLException {
        List<TradeOrder> all = tradeOrderDAO.listOrders();
        int newId = all.stream().mapToInt(TradeOrder::getOrderId).max().orElse(0) + 1;
        TradeOrder order = new TradeOrder(newId, investorId, assetId, type, price, units, LocalDate.now(), "OPEN");
        tradeOrderDAO.insertOrder(order);
        System.out.printf("  (Inserted %s order #%d directly for rollback test)%n", type, newId);
        return order;
    }
}
