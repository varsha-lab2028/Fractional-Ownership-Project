package edu.iiitd.dbms.data_access;

import edu.iiitd.dbms.config.ServerConnector;
import edu.iiitd.dbms.dto.AssetHoldingDTO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PortfolioDAO {
    /*Fetches all current asset holdings for a specific investor, including
     real-time fractional valuation calculations*/
    public List<AssetHoldingDTO> getInvestorHoldings(int investorId) throws SQLException {
        List<AssetHoldingDTO> holdings = new ArrayList<>();
        String query = """
            SELECT 
                a.asset_id, 
                a.asset_name AS asset_name, 
                a.category, 
                o.units_held, 
                i.price_per_unit AS ipo_price,
                i.total_units,
                (SELECT v.valuation_amount 
                 FROM VALUATION v 
                 WHERE v.asset_id = a.asset_id 
                 ORDER BY v.valuation_date DESC LIMIT 1) AS latest_valuation
            FROM OWNERSHIP o
            JOIN ASSET a ON o.asset_id = a.asset_id
            JOIN IPO i ON a.asset_id = i.asset_id
            WHERE o.investor_id = ? AND o.units_held > 0
        """;

        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
             
            stmt.setInt(1, investorId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int assetId = rs.getInt("asset_id");
                    String assetName = rs.getString("asset_name");
                    String category = rs.getString("category");
                    int unitsHeld = rs.getInt("units_held");
                    double ipoPrice = rs.getDouble("ipo_price");
                    int totalUnits = rs.getInt("total_units");

                    double latestValuation = rs.getDouble("latest_valuation");
                    if (rs.wasNull()) {
                        latestValuation = ipoPrice * totalUnits; // Fallback to IPO valuation
                    }

                    //Calculates the financial metrics
                    double totalInvested = unitsHeld * ipoPrice;
                    double fractionalShare = (double) unitsHeld / totalUnits;
                    double currentValue = fractionalShare * latestValuation;

                    holdings.add(new AssetHoldingDTO(
                            assetId, assetName, category, unitsHeld, 
                            ipoPrice, currentValue, totalInvested
                    ));
                }
            }
        }
        return holdings;
    }
}