package edu.iiitd.dbms.data_access;

import edu.iiitd.dbms.config.ServerConnector;
import edu.iiitd.dbms.domain.Valuation;

import java.sql.*;
import java.sql.Date;
import java.util.*;

//this DAO is for latest valuation + history
//this is for 'latest per asset' = For each asset, get the most recent record
// from another table (usually based on a date column).
public class ValuationDAO {
    private Valuation matchValuationColumns(ResultSet rs) throws SQLException {
        return new Valuation(
                rs.getInt("valuation_id"),
                rs.getInt("asset_id"),
                rs.getDouble("valuation_amount"),
                rs.getDate("valuation_date").toLocalDate()
        );
    }
    public Valuation getLatestForAsset(int assetId) throws SQLException {
        String sqlQuery = """
            SELECT valuation_id, asset_id, valuation_amount, valuation_date
            FROM valuation
            WHERE asset_id = ?
            ORDER BY valuation_date DESC
            LIMIT 1
        """;
        try (Connection connect = ServerConnector.DBConnection();
             PreparedStatement ps = connect.prepareStatement(sqlQuery)) {
            ps.setInt(1, assetId);
            try (ResultSet rs = ps.executeQuery()) {
                if(rs.next()){
                    return matchValuationColumns(rs);
                } else {
                    return null;
                }
            }
        }
    }

    public List<Valuation> getHistory(int assetId) throws SQLException {
        String sql = """
            SELECT valuation_id, asset_id, valuation_amount, valuation_date
            FROM valuation
            WHERE asset_id = ?
            ORDER BY valuation_date DESC
        """;
        List<Valuation> historyList = new ArrayList<>();
        try (Connection connect = ServerConnector.DBConnection();
             PreparedStatement ps = connect.prepareStatement(sql)) {
            ps.setInt(1, assetId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    historyList.add(matchValuationColumns(rs));
                }
            }
        }
        return historyList;
    }

    //to simulate valuation updates
    public void insertValuationToSimulate(int assetId, double amount, Date valuationDate) throws SQLException {
        String sqlQuery = "INSERT INTO valuation(asset_id, valuation_amount, valuation_date) VALUES (?, ?, ?)";
        try (Connection connect = ServerConnector.DBConnection();
             PreparedStatement ps = connect.prepareStatement(sqlQuery)) {
            ps.setInt(1, assetId);
            ps.setDouble(2, amount);
            ps.setDate(3, valuationDate);
            ps.executeUpdate();
        }
    }
}
