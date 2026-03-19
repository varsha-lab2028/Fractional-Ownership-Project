package edu.iiitd.dbms.test;

import edu.iiitd.dbms.data_access.StoredProcedureDAO;
import edu.iiitd.dbms.data_access.StoredProcedureDAO.*;
import edu.iiitd.dbms.dto.*;

import java.sql.SQLException;
import java.util.List;

public class StoredProcedureTest {
    public static void main(String[] args) {
        StoredProcedureDAO dao = new StoredProcedureDAO();

        System.out.println("FUNCTIONS TESTS\n");

        // F1: fn_get_wallet_balance
        run("F1 fn_get_wallet_balance(investor=1)", () -> {
            double bal = dao.getWalletBalance(1);
            System.out.println("   Wallet balance of investor 1: $" + bal);
        });

        // F2: fn_get_latest_valuation
        run("F2 fn_get_latest_valuation(asset=1)", () -> {
            double val = dao.getLatestValuation(1);
            System.out.println("   Latest valuation of asset 1: $" + val);
        });

        // F3: fn_get_total_units_held
        run("F3 fn_get_total_units_held(asset=1)", () -> {
            int units = dao.getTotalUnitsHeld(1);
            System.out.println("   Total units held for asset 1: " + units);
        });

        // F4: fn_get_investor_portfolio_value
        run("F4 fn_get_investor_portfolio_value(investor=1)", () -> {
            double pv = dao.getInvestorPortfolioValue(1);
            System.out.println("   Portfolio value of investor 1: $" + pv);
        });

        // F5: fn_count_investor_holdings
        run("F5 fn_count_investor_holdings(investor=1)", () -> {
            int cnt = dao.countInvestorHoldings(1);
            System.out.println("   Assets held by investor 1: " + cnt);
        });

        // F6: fn_get_total_trade_volume
        run("F6 fn_get_total_trade_volume(asset=1)", () -> {
            int vol = dao.getTotalTradeVolume(1);
            System.out.println("   Total trade volume for asset 1: " + vol + " units");
        });

        // F7: fn_investor_has_sufficient_balance
        run("F7 fn_investor_has_sufficient_balance(investor=1, $10)", () -> {
            boolean ok = dao.investorHasSufficientBalance(1, 10.00);
            System.out.println("   Has $10? → " + ok);
        });
        run("F7b fn_investor_has_sufficient_balance(investor=1, $999999999)", () -> {
            boolean ok = dao.investorHasSufficientBalance(1, 999_999_999.00);
            System.out.println("   Has $999,999,999? → " + ok);
        });


        System.out.println("\nPROCEDURE TESTS\n");

        // P1: sp_get_investor_holdings
        run("P1 sp_get_investor_holdings(investor=1)", () -> {
            List<AssetHoldingDTO> holdings = dao.getInvestorHoldings(1);
            if (holdings.isEmpty()) {
                System.out.println("   No holdings found.");
            } else {
                for (AssetHoldingDTO h : holdings) {
                    System.out.printf("   Asset %-30s | Units: %3d | Value: $%,.2f%n",
                            h.getAssetName(), h.getUnitsHeld(), h.getCurrentValue());
                }
            }
        });

        // P2: sp_deposit_to_wallet
        run("P2 sp_deposit_to_wallet(investor=2, $1000)", () -> {
            ProcedureResult r = dao.depositToWallet(2, 1000.00, "Test Deposit");
            System.out.println("   success=" + r.success + " → " + r.message);
        });

        // P3: sp_withdraw_from_wallet (valid)
        run("P3 sp_withdraw_from_wallet(investor=2, $100)", () -> {
            ProcedureResult r = dao.withdrawFromWallet(2, 100.00, "Test Withdrawal");
            System.out.println("   success=" + r.success + " → " + r.message);
        });

        // P3b: sp_withdraw_from_wallet (overdraft — should fail)
        run("P3b sp_withdraw_from_wallet overdraft (investor=2, $999999)", () -> {
            ProcedureResult r = dao.withdrawFromWallet(2, 999_999.00, "Overdraft Attempt");
            System.out.println("   success=" + r.success + " → " + r.message);
        });

        // P4: sp_place_buy_order
        // Investor 1 buys 2 units of asset 3 at $6200 each
        run("P4 sp_place_buy_order(investor=1, asset=3, 2 units @ $6200)", () -> {
            ProcedureResult r = dao.placeBuyOrder(1, 3, 2, 6200.00);
            System.out.println("   success=" + r.success +
                    " | order_id=" + r.generatedId +
                    " → " + r.message);
        });

        // P5: sp_place_sell_order
        // Investor 2 sells 5 units of asset 1 at $13000 (investor 2 holds 30 from seed)
        run("P5 sp_place_sell_order(investor=2, asset=1, 5 units @ $13000)", () -> {
            ProcedureResult r = dao.placeSellOrder(2, 1, 5, 13000.00);
            System.out.println("   success=" + r.success +
                    " | order_id=" + r.generatedId +
                    " → " + r.message);
        });

        // P5b: sp_place_sell_order with insufficient units — should fail
        run("P5b sp_place_sell_order insufficient units (investor=2, asset=1, 9999 units)", () -> {
            ProcedureResult r = dao.placeSellOrder(2, 1, 9999, 13000.00);
            System.out.println("   success=" + r.success + " → " + r.message);
        });

        // P6: sp_execute_trade — uses the orders from P4 and P5 if they succeeded
        // Using seeded OPEN orders: order 3 (BUY) and order 13 (SELL) on asset 3
        run("P6 sp_execute_trade(buy=3, sell=13)", () -> {
            ProcedureResult r = dao.executeTrade(3, 13);
            System.out.println("   success=" + r.success +
                    " | trade_id=" + r.generatedId +
                    " → " + r.message);
        });

        // P7: sp_verify_asset
        run("P7 sp_verify_asset(asset=1, admin=1, Verified)", () -> {
            ProcedureResult r = dao.verifyAsset(1, 1, "Verified");
            System.out.println("   success=" + r.success + " → " + r.message);
        });

        // P7b: invalid status — should fail
        run("P7b sp_verify_asset invalid status", () -> {
            ProcedureResult r = dao.verifyAsset(1, 1, "WRONG");
            System.out.println("   success=" + r.success + " → " + r.message);
        });

        // P8: sp_get_investor_summary
        run("P8 sp_get_investor_summary(investor=1)", () -> {
            InvestorSummary s = dao.getInvestorSummary(1);
            System.out.println("   " + s);
        });

        System.out.println("  ALL TESTS COMPLETE");
    }

    // ── Helper — wraps each test in a try/catch so one failure doesn't stop the rest ──

    @FunctionalInterface
    interface TestBlock { void run() throws SQLException; }

    private static void run(String label, TestBlock block) {
        System.out.print("[TEST] " + label + " ... ");
        try {
            block.run();
        } catch (SQLException e) {
            System.out.println("\n   ✗ SQLException: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("\n   ✗ Error: " + e.getMessage());
        }
    }
}
