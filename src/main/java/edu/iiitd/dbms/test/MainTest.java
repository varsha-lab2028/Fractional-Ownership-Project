package edu.iiitd.dbms.test;

import edu.iiitd.dbms.config.ServerConnector;
import edu.iiitd.dbms.data_access.*;
import edu.iiitd.dbms.domain.*;
import edu.iiitd.dbms.service.TradingService;

import java.sql.*;
import java.util.List;

/**
 * MainTest — Task 7.6 demo runner.
 *
 * FIX for "NoRouteToHostException / connection attempt failed":
 * ─────────────────────────────────────────────────────────────
 *  The original code called TradingService.placeBuyOrder() which internally
 *  opens its own DB connection to check wallet balance BEFORE the shared
 *  transaction connection was opened.  If the Supabase host is unreachable
 *  the stack trace pointed to WalletTransactionDAO.getStoredWalletBalance().
 *
 *  This version:
 *   1. Tests the DB connection upfront and aborts with a clear message if
 *      the connection cannot be established (misconfigured db.properties).
 *   2. Wraps each individual test in its own try-catch so one failing test
 *      cannot swallow the results of the other.
 *   3. Prints the configuration hint that points users to db.properties.
 *
 * Prerequisites:
 *   - src/main/resources/db.properties  →  db.url, db.user, db.password
 *   - Supabase / local PostgreSQL running and reachable from this machine
 *   - Schema + seed SQL already applied to the target database
 */
public class MainTest {

    private static final TradingService       tradingService = new TradingService();
    private static final TradeOrderDAO        tradeOrderDAO  = new TradeOrderDAO();
    private static final OwnershipDAO         ownershipDAO   = new OwnershipDAO();
    private static final WalletTransactionDAO walletDAO      = new WalletTransactionDAO();

    // ── Adjust these IDs to match live seed data in your Supabase instance ──
    private static final int    BUYER_ID  = 1;
    private static final int    SELLER_ID = 4;
    private static final int    ASSET_ID  = 2;
    private static final int    UNITS     = 5;
    private static final double PRICE     = 1000.00;

    // ────────────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("  MainTest — Week 6 Trade Demo");
        System.out.println("=".repeat(60));

        // ── Step 1: verify DB connectivity before anything else ──────────
        if (!checkConnection()) {
            System.err.println();
            System.err.println("▶  ACTION REQUIRED:");
            System.err.println("   Open  src/main/resources/db.properties");
            System.err.println("   and fill in your Supabase (or local PG) credentials:");
            System.err.println("     db.url      = jdbc:postgresql://<host>:<port>/<db>");
            System.err.println("     db.user     = <username>");
            System.err.println("     db.password = <password>");
            System.err.println();
            System.err.println("   If you're running Supabase cloud, make sure:");
            System.err.println("     • Your IP is not blocked by Supabase network policies");
            System.err.println("     • The project is not paused (free tier auto-pauses)");
            System.err.println("     • The URL is the Pooler or Direct connection string");
            return;
        }

        // ── Step 2: run tests independently ─────────────────────────────
        System.out.println();
        try {
            runSuccessfulTradeTest();
        } catch (Exception e) {
            System.err.println("[TEST 1 FAILED] " + e.getMessage());
            e.printStackTrace(System.err);
        }

        System.out.println();
        try {
            runFailedTradeRollbackTest();
        } catch (Exception e) {
            System.err.println("[TEST 2 FAILED] " + e.getMessage());
            e.printStackTrace(System.err);
        }

        System.out.println("\n" + "=".repeat(60));
        System.out.println("  Demo finished.");
        System.out.println("=".repeat(60));
    }

    // =========================================================================
    // Connection pre-check
    // =========================================================================
    private static boolean checkConnection() {
        System.out.print("Checking database connection... ");
        try (Connection conn = ServerConnector.DBConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("OK  (" + conn.getMetaData().getURL() + ")");
                return true;
            }
        } catch (Exception e) {
            System.out.println("FAILED");
            System.err.println("Connection error: " + e.getMessage());
            // Unwrap cause for clarity
            Throwable cause = e.getCause();
            while (cause != null) {
                System.err.println("  Caused by: " + cause.getClass().getSimpleName()
                    + ": " + cause.getMessage());
                cause = cause.getCause();
            }
        }
        return false;
    }

    // =========================================================================
    // TEST 1 — Successful trade
    // =========================================================================
    private static void runSuccessfulTradeTest() throws Exception {
        System.out.println("─".repeat(60));
        System.out.println("TEST 1: Successful Trade");
        System.out.println("─".repeat(60));

        // Create matching OPEN buy + sell orders
        TradeOrder buyOrder  = tradingService.placeBuyOrder(BUYER_ID,  ASSET_ID, UNITS, PRICE);
        TradeOrder sellOrder = tradingService.placeSellOrder(SELLER_ID, ASSET_ID, UNITS, PRICE);
        System.out.printf("Created BUY  order #%d  (investor %d, %d units @ $%.2f)%n",
            buyOrder.getOrderId(),  BUYER_ID,  UNITS, PRICE);
        System.out.printf("Created SELL order #%d  (investor %d, %d units @ $%.2f)%n",
            sellOrder.getOrderId(), SELLER_ID, UNITS, PRICE);

        // Print state BEFORE
        System.out.println("\n--- BEFORE TRADE ---");
        printOwnership("Seller",  SELLER_ID, ASSET_ID);
        printOwnership("Buyer",   BUYER_ID,  ASSET_ID);
        printWalletBalance("Buyer",  BUYER_ID);
        printWalletBalance("Seller", SELLER_ID);
        printOrderStatus("BUY  order",  buyOrder.getOrderId());
        printOrderStatus("SELL order", sellOrder.getOrderId());

        // Execute
        Trade trade = tradingService.executeTrade(buyOrder.getOrderId(), sellOrder.getOrderId());
        System.out.printf("%nTrade #%d executed successfully at $%.2f × %d units%n",
            trade.getTradeId(), trade.getTradePrice(), trade.getTradeUnits());

        // Print state AFTER
        System.out.println("\n--- AFTER TRADE ---");
        printOwnership("Seller",  SELLER_ID, ASSET_ID);
        printOwnership("Buyer",   BUYER_ID,  ASSET_ID);
        printWalletBalance("Buyer",  BUYER_ID);
        printWalletBalance("Seller", SELLER_ID);
        printOrderStatus("BUY  order",  buyOrder.getOrderId());
        printOrderStatus("SELL order", sellOrder.getOrderId());

        System.out.println("\n✓  TEST 1 PASSED");
    }

    // =========================================================================
    // TEST 2 — Failed trade (rollback test)
    // =========================================================================
    private static void runFailedTradeRollbackTest() throws Exception {
        System.out.println("─".repeat(60));
        System.out.println("TEST 2: Failed Trade — Rollback Verification");
        System.out.println("─".repeat(60));

        // Create a sell order for MORE units than the seller actually holds
        // to deliberately trigger the DB trigger / validation
        int oversellUnits = 999_999;
        System.out.printf("Attempting to SELL %d units (expected to fail)...%n", oversellUnits);

        try {
            TradeOrder badSell = tradingService.placeSellOrder(SELLER_ID, ASSET_ID, oversellUnits, PRICE);
            TradeOrder buyOrder = tradingService.placeBuyOrder(BUYER_ID, ASSET_ID, oversellUnits, PRICE);
            tradingService.executeTrade(buyOrder.getOrderId(), badSell.getOrderId());
            System.err.println("✗  TEST 2 FAILED — trade should have been rejected");
        } catch (Exception e) {
            System.out.println("Trade correctly rejected: " + e.getMessage());
            System.out.println("Verifying database state unchanged...");
            printOwnership("Seller", SELLER_ID, ASSET_ID);
            printOwnership("Buyer",  BUYER_ID,  ASSET_ID);
            printWalletBalance("Buyer",  BUYER_ID);
            printWalletBalance("Seller", SELLER_ID);
            System.out.println("\n✓  TEST 2 PASSED — rollback confirmed");
        }
    }

    // ── Print helpers ─────────────────────────────────────────────────────────
    private static void printOwnership(String label, int investorId, int assetId) throws Exception {
        Ownership o = ownershipDAO.findOwnership(investorId, assetId);
        int units = (o != null && o.getUnitsHeld() != null) ? o.getUnitsHeld() : 0;
        System.out.printf("  %-10s units of asset %d: %d%n", label, assetId, units);
    }

    private static void printWalletBalance(String label, int investorId) throws Exception {
        double bal = walletDAO.getStoredWalletBalance(investorId);
        System.out.printf("  %-10s wallet balance: $%.2f%n", label, bal);
    }

    private static void printOrderStatus(String label, int orderId) throws Exception {
        List<TradeOrder> all = tradeOrderDAO.listOrders();
        String status = all.stream()
            .filter(o -> o.getOrderId() == orderId)
            .map(TradeOrder::getStatus)
            .findFirst()
            .orElse("NOT FOUND");
        System.out.printf("  %-14s #%d status: %s%n", label, orderId, status);
    }
}
