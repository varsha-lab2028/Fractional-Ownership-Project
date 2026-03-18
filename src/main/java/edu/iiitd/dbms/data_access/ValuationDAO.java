package edu.iiitd.dbms.data_access;

import edu.iiitd.dbms.config.ServerConnector;
import edu.iiitd.dbms.domain.TradeOrder;
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

    //list all valuations
    public List<Valuation> listValuations() throws SQLException {
        String sqlQuery = """
            SELECT *
            FROM valuation
            ORDER BY valuation_id
        """;
        List<Valuation> valuationList = new ArrayList<>();
        try (Connection connect = ServerConnector.DBConnection();
             PreparedStatement ps = connect.prepareStatement(sqlQuery);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                valuationList.add(matchValuationColumns(rs));
            }
        }
        return valuationList;
    }

    //get latest by asset
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

    //get history by asset id
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

    //Q15 - latest valuation snapshot for EVERY asset in a single query
    public List<Valuation> getAllLatestValuations() throws SQLException {
        String sql = """
            SELECT v.valuation_id, v.asset_id, v.valuation_amount, v.valuation_date
            FROM VALUATION v
            JOIN ASSET a ON v.asset_id = a.asset_id
            WHERE (v.asset_id, v.valuation_date) IN (
                SELECT asset_id, MAX(valuation_date)
                FROM VALUATION
                GROUP BY asset_id
            )
            ORDER BY v.asset_id
        """;
        List<Valuation> result = new ArrayList<>();
        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(matchValuationColumns(rs));
            }
        }
        return result;
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
