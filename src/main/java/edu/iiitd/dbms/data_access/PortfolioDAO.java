package edu.iiitd.dbms.data_access;

import edu.iiitd.dbms.config.ServerConnector;
import edu.iiitd.dbms.dto.AssetHoldingDTO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PortfolioDAO {

    /**
     * Returns all asset holdings for a given investor, with current valuation.
     */
    public List<AssetHoldingDTO> getInvestorHoldings(int investorId) throws SQLException {
        String sql = """
            SELECT
                a.asset_id,
                a.asset_name AS asset_name,
                a.category,
                o.units_held,
                i.price_per_unit AS ipo_price,
                i.total_units,
                (SELECT v.valuation_amount
                 FROM valuation v
                 WHERE v.asset_id = a.asset_id
                 ORDER BY v.valuation_date DESC LIMIT 1) AS latest_valuation
            FROM ownership o
            JOIN asset a ON o.asset_id = a.asset_id
            JOIN ipo i ON a.asset_id = i.asset_id
            WHERE o.investor_id = ? AND o.units_held > 0
            """;
        List<AssetHoldingDTO> list = new ArrayList<>();
        try (Connection c = ServerConnector.DBConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, investorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int assetId      = rs.getInt("asset_id");
                    String assetName = rs.getString("asset_name");
                    String category  = rs.getString("category");
                    int unitsHeld    = rs.getInt("units_held");
                    double ipoPrice  = rs.getDouble("ipo_price");
                    int totalUnits   = rs.getInt("total_units");
                    double latestVal = rs.getDouble("latest_valuation");
                    if (rs.wasNull()) latestVal = ipoPrice * totalUnits;

                    double totalInvested = unitsHeld * ipoPrice;
                    double fraction      = totalUnits > 0 ? (double) unitsHeld / totalUnits : 0;
                    double currentValue  = fraction * latestVal;

                    list.add(new AssetHoldingDTO(assetId, assetName, category,
                            unitsHeld, ipoPrice, currentValue, totalInvested));
                }
            }
        }
        return list;
    }

    /**
     * Task 8 — SQL Function: get_portfolio_value(investor_id)
     * Calls the Postgres function via CallableStatement and returns
     * SUM(units_held × latest valuation) for the given investor.
     */
    public double getPortfolioValue(int investorId) throws SQLException {
        // fn_get_investor_portfolio_value is defined in supabase_functions_and_procedures.sql
        String sql = "SELECT fn_get_investor_portfolio_value(?)";
        try (Connection c = ServerConnector.DBConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, investorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        }
        return 0.0;
    }
}
