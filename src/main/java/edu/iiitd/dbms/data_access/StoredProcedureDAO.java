package edu.iiitd.dbms.data_access;

import edu.iiitd.dbms.config.*;
import edu.iiitd.dbms.dto.*;
import edu.iiitd.dbms.data_access.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StoredProcedureDAO {
    public static class ProcedureResult {
        public final boolean success;
        public final String  message;
        public final Integer generatedId; // null when not applicable

        public ProcedureResult(boolean success, String message, Integer generatedId) {
            this.success     = success;
            this.message     = message;
            this.generatedId = generatedId;
        }
        public ProcedureResult(boolean success, String message) {
            this(success, message, null);
        }

        @Override
        public String toString() {
            return String.format("ProcedureResult{success=%b, generatedId=%s, message='%s'}",
                    success, generatedId, message);
        }
    }

    public static class InvestorSummary {
        public final String  name;
        public final double  walletBalance;
        public final double  portfolioValue;
        public final int     assetCount;
        public final int     tradeOrderCount;

        public InvestorSummary(String name, double walletBalance,
                               double portfolioValue, int assetCount,
                               int tradeOrderCount) {
            this.name            = name;
            this.walletBalance   = walletBalance;
            this.portfolioValue  = portfolioValue;
            this.assetCount      = assetCount;
            this.tradeOrderCount = tradeOrderCount;
        }

        @Override
        public String toString() {
            return String.format(
                    "InvestorSummary{name='%s', wallet=$%.2f, portfolio=$%.2f, assets=%d, orders=%d}",
                    name, walletBalance, portfolioValue, assetCount, tradeOrderCount);
        }
    }

    // =========================================================================
    // SECTION 1 — SCALAR FUNCTIONS
    // Syntax: {? = call fn_name(param, ...)}
    // The first ? is registered as the return value (OUT at position 1).
    // =========================================================================

    /**
     * fn_get_wallet_balance(investor_id)
     * Returns the investor's current wallet_balance directly from the INVESTOR row.
     */
    public double getWalletBalance(int investorId) throws SQLException {
        try (Connection c = ServerConnector.DBConnection();
             CallableStatement cs = c.prepareCall("{? = call fn_get_wallet_balance(?)}")) {

            cs.registerOutParameter(1, Types.DECIMAL);
            cs.setInt(2, investorId);
            cs.execute();
            return cs.getDouble(1);
        }
    }

    /**
     * fn_get_latest_valuation(asset_id)
     * Returns the most recent valuation_amount for the asset (0.00 if none).
     */
    public double getLatestValuation(int assetId) throws SQLException {
        try (Connection c = ServerConnector.DBConnection();
             CallableStatement cs = c.prepareCall("{? = call fn_get_latest_valuation(?)}")) {

            cs.registerOutParameter(1, Types.DECIMAL);
            cs.setInt(2, assetId);
            cs.execute();
            return cs.getDouble(1);
        }
    }

    /**
     * fn_get_total_units_held(asset_id)
     * Returns the sum of units_held across all investors for this asset.
     */
    public int getTotalUnitsHeld(int assetId) throws SQLException {
        try (Connection c = ServerConnector.DBConnection();
             CallableStatement cs = c.prepareCall("{? = call fn_get_total_units_held(?)}")) {

            cs.registerOutParameter(1, Types.INTEGER);
            cs.setInt(2, assetId);
            cs.execute();
            return cs.getInt(1);
        }
    }

    /**
     * fn_get_investor_portfolio_value(investor_id)
     * Returns the current market value of the investor's entire portfolio.
     */
    public double getInvestorPortfolioValue(int investorId) throws SQLException {
        try (Connection c = ServerConnector.DBConnection();
             CallableStatement cs = c.prepareCall("{? = call fn_get_investor_portfolio_value(?)}")) {

            cs.registerOutParameter(1, Types.DECIMAL);
            cs.setInt(2, investorId);
            cs.execute();
            return cs.getDouble(1);
        }
    }

    /**
     * fn_count_investor_holdings(investor_id)
     * Returns the number of distinct assets (with units_held > 0) the investor holds.
     */
    public int countInvestorHoldings(int investorId) throws SQLException {
        try (Connection c = ServerConnector.DBConnection();
             CallableStatement cs = c.prepareCall("{? = call fn_count_investor_holdings(?)}")) {

            cs.registerOutParameter(1, Types.INTEGER);
            cs.setInt(2, investorId);
            cs.execute();
            return cs.getInt(1);
        }
    }

    /**
     * fn_get_total_trade_volume(asset_id)
     * Returns the total number of units ever traded for this asset.
     */
    public int getTotalTradeVolume(int assetId) throws SQLException {
        try (Connection c = ServerConnector.DBConnection();
             CallableStatement cs = c.prepareCall("{? = call fn_get_total_trade_volume(?)}")) {

            cs.registerOutParameter(1, Types.INTEGER);
            cs.setInt(2, assetId);
            cs.execute();
            return cs.getInt(1);
        }
    }

    /**
     * fn_investor_has_sufficient_balance(investor_id, required_amount)
     * Returns true if the investor's wallet >= requiredAmount, false otherwise.
     */
    public boolean investorHasSufficientBalance(int investorId, double requiredAmount)
            throws SQLException {
        try (Connection c = ServerConnector.DBConnection();
             CallableStatement cs = c.prepareCall(
                     "{? = call fn_investor_has_sufficient_balance(?, ?)}")) {

            cs.registerOutParameter(1, Types.TINYINT);
            cs.setInt(2, investorId);
            cs.setDouble(3, requiredAmount);
            cs.execute();
            return cs.getInt(1) == 1;
        }
    }


    // =========================================================================
    // SECTION 2 — STORED PROCEDURES
    // Syntax: {call sp_name(in_param, ..., out_param, ...)}
    // IN params are set with setXxx(); OUT params are registered first.
    // =========================================================================

    /**
     * sp_get_investor_holdings(investor_id)
     * Returns a result set of all holdings for this investor, enriched with
     * asset name, category, latest valuation, and current holding value.
     * Mapped to List<AssetHoldingDTO> to stay consistent with PortfolioDAO.
     */
    public List<AssetHoldingDTO> getInvestorHoldings(int investorId) throws SQLException {
        List<AssetHoldingDTO> list = new ArrayList<>();

        try (Connection c = ServerConnector.DBConnection();
             CallableStatement cs = c.prepareCall("{call sp_get_investor_holdings(?)}")) {

            cs.setInt(1, investorId);

            boolean hasResults = cs.execute();
            if (hasResults) {
                try (ResultSet rs = cs.getResultSet()) {
                    while (rs.next()) {
                        int    assetId      = rs.getInt("asset_id");
                        String assetName    = rs.getString("asset_name");
                        String category     = rs.getString("category");
                        int    unitsHeld    = rs.getInt("units_held");
                        double ipoPrice     = rs.getDouble("ipo_price_per_unit");
                        double currentValue = rs.getDouble("current_holding_value");
                        double totalInvested = unitsHeld * ipoPrice;

                        list.add(new AssetHoldingDTO(
                                assetId, assetName, category,
                                unitsHeld, ipoPrice, currentValue, totalInvested));
                    }
                }
            }
        }
        return list;
    }

    /**
     * sp_deposit_to_wallet(investor_id, amount, category, OUT success, OUT message)
     * Deposits a positive amount into the investor's wallet.
     * The existing AFTER INSERT trigger fires automatically to update wallet_balance.
     */
    public ProcedureResult depositToWallet(int investorId, double amount, String category)
            throws SQLException {
        try (Connection c = ServerConnector.DBConnection();
             CallableStatement cs = c.prepareCall(
                     "{call sp_deposit_to_wallet(?, ?, ?, ?, ?)}")) {

            cs.setInt(1, investorId);
            cs.setDouble(2, amount);
            cs.setString(3, category);
            cs.registerOutParameter(4, Types.TINYINT);    // OUT p_success
            cs.registerOutParameter(5, Types.VARCHAR);    // OUT p_message

            cs.execute();

            boolean success = cs.getInt(4) == 1;
            String  message = cs.getString(5);
            return new ProcedureResult(success, message);
        }
    }

    /**
     * sp_withdraw_from_wallet(investor_id, amount, category, OUT success, OUT message)
     * Withdraws from the investor's wallet.
     * The existing BEFORE INSERT trigger blocks overdrafts automatically.
     */
    public ProcedureResult withdrawFromWallet(int investorId, double amount, String category)
            throws SQLException {
        try (Connection c = ServerConnector.DBConnection();
             CallableStatement cs = c.prepareCall(
                     "{call sp_withdraw_from_wallet(?, ?, ?, ?, ?)}")) {

            cs.setInt(1, investorId);
            cs.setDouble(2, amount);
            cs.setString(3, category);
            cs.registerOutParameter(4, Types.TINYINT);    // OUT p_success
            cs.registerOutParameter(5, Types.VARCHAR);    // OUT p_message

            cs.execute();

            boolean success = cs.getInt(4) == 1;
            String  message = cs.getString(5);
            return new ProcedureResult(success, message);
        }
    }

    /**
     * sp_place_buy_order(investor_id, asset_id, units, price,
     *                    OUT order_id, OUT success, OUT message)
     * Validates wallet balance and inserts a BUY order.
     * generatedId in the result holds the new order_id.
     */
    public ProcedureResult placeBuyOrder(int investorId, int assetId,
                                         int units, double price)
            throws SQLException {
        try (Connection c = ServerConnector.DBConnection();
             CallableStatement cs = c.prepareCall(
                     "{call sp_place_buy_order(?, ?, ?, ?, ?, ?, ?)}")) {

            cs.setInt(1, investorId);
            cs.setInt(2, assetId);
            cs.setInt(3, units);
            cs.setDouble(4, price);
            cs.registerOutParameter(5, Types.INTEGER);    // OUT p_order_id
            cs.registerOutParameter(6, Types.TINYINT);    // OUT p_success
            cs.registerOutParameter(7, Types.VARCHAR);    // OUT p_message

            cs.execute();

            int     orderId = cs.getInt(5);
            boolean success = cs.getInt(6) == 1;
            String  message = cs.getString(7);
            return new ProcedureResult(success, message, success ? orderId : null);
        }
    }

    /**
     * sp_place_sell_order(investor_id, asset_id, units, price,
     *                     OUT order_id, OUT success, OUT message)
     * Validates units held and inserts a SELL order.
     * generatedId in the result holds the new order_id.
     */
    public ProcedureResult placeSellOrder(int investorId, int assetId,
                                          int units, double price)
            throws SQLException {
        try (Connection c = ServerConnector.DBConnection();
             CallableStatement cs = c.prepareCall(
                     "{call sp_place_sell_order(?, ?, ?, ?, ?, ?, ?)}")) {

            cs.setInt(1, investorId);
            cs.setInt(2, assetId);
            cs.setInt(3, units);
            cs.setDouble(4, price);
            cs.registerOutParameter(5, Types.INTEGER);    // OUT p_order_id
            cs.registerOutParameter(6, Types.TINYINT);    // OUT p_success
            cs.registerOutParameter(7, Types.VARCHAR);    // OUT p_message

            cs.execute();

            int     orderId = cs.getInt(5);
            boolean success = cs.getInt(6) == 1;
            String  message = cs.getString(7);
            return new ProcedureResult(success, message, success ? orderId : null);
        }
    }

    /**
     * sp_execute_trade(buy_order_id, sell_order_id,
     *                  OUT trade_id, OUT success, OUT message)
     * Matches a buy and sell order, settles ownership and wallets.
     * All 5 existing triggers cooperate inside this procedure.
     * generatedId in the result holds the new trade_id.
     */
    public ProcedureResult executeTrade(int buyOrderId, int sellOrderId)
            throws SQLException {
        try (Connection c = ServerConnector.DBConnection();
             CallableStatement cs = c.prepareCall(
                     "{call sp_execute_trade(?, ?, ?, ?, ?)}")) {

            cs.setInt(1, buyOrderId);
            cs.setInt(2, sellOrderId);
            cs.registerOutParameter(3, Types.INTEGER);    // OUT p_trade_id
            cs.registerOutParameter(4, Types.TINYINT);    // OUT p_success
            cs.registerOutParameter(5, Types.VARCHAR);    // OUT p_message

            cs.execute();

            int     tradeId = cs.getInt(3);
            boolean success = cs.getInt(4) == 1;
            String  message = cs.getString(5);
            return new ProcedureResult(success, message, success ? tradeId : null);
        }
    }

    /**
     * sp_verify_asset(asset_id, admin_id, status, OUT success, OUT message)
     * Admin action: updates an asset's verification_status.
     * Valid statuses: "Verified", "Pending", "Rejected"
     */
    public ProcedureResult verifyAsset(int assetId, int adminId, String status)
            throws SQLException {
        try (Connection c = ServerConnector.DBConnection();
             CallableStatement cs = c.prepareCall(
                     "{call sp_verify_asset(?, ?, ?, ?, ?)}")) {

            cs.setInt(1, assetId);
            cs.setInt(2, adminId);
            cs.setString(3, status);
            cs.registerOutParameter(4, Types.TINYINT);    // OUT p_success
            cs.registerOutParameter(5, Types.VARCHAR);    // OUT p_message

            cs.execute();

            boolean success = cs.getInt(4) == 1;
            String  message = cs.getString(5);
            return new ProcedureResult(success, message);
        }
    }

    /**
     * sp_get_investor_summary(investor_id,
     *   OUT name, OUT wallet_balance, OUT portfolio_value,
     *   OUT asset_count, OUT trade_order_count)
     * Returns a complete financial snapshot for one investor.
     */
    public InvestorSummary getInvestorSummary(int investorId) throws SQLException {
        try (Connection c = ServerConnector.DBConnection();
             CallableStatement cs = c.prepareCall(
                     "{call sp_get_investor_summary(?, ?, ?, ?, ?, ?)}")) {

            cs.setInt(1, investorId);
            cs.registerOutParameter(2, Types.VARCHAR);    // OUT p_name
            cs.registerOutParameter(3, Types.DECIMAL);    // OUT p_wallet_balance
            cs.registerOutParameter(4, Types.DECIMAL);    // OUT p_portfolio_value
            cs.registerOutParameter(5, Types.INTEGER);    // OUT p_asset_count
            cs.registerOutParameter(6, Types.INTEGER);    // OUT p_trade_order_count

            cs.execute();

            String name           = cs.getString(2);
            double walletBalance  = cs.getDouble(3);
            double portfolioValue = cs.getDouble(4);
            int    assetCount     = cs.getInt(5);
            int    tradeCount     = cs.getInt(6);

            return new InvestorSummary(name, walletBalance, portfolioValue,
                    assetCount, tradeCount);
        }
    }
}
