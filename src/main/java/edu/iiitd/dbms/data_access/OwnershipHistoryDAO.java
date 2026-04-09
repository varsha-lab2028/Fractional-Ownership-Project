package edu.iiitd.dbms.data_access;

import edu.iiitd.dbms.config.ServerConnector;
import edu.iiitd.dbms.domain.OwnershipHistory;

import java.sql.*;
import java.util.*;
import java.time.*;

//list history - trigger proof
public class OwnershipHistoryDAO {

    // insert a history record manually (used when DB trigger is not present)
    public void insert(Connection conn,
                       int investorId, int assetId,
                       int unitsBefore, int unitsAfter,
                       String changeType,
                       Integer tradeId, Integer ipoId) throws SQLException {
        String sql = """
            INSERT INTO ownership_history
                (investor_id, asset_id, units_before, units_after,
                 change_date, change_type, trade_id, ipo_id)
            VALUES (?, ?, ?, ?, CURRENT_DATE, ?, ?, ?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, investorId);
            ps.setInt(2, assetId);
            ps.setInt(3, unitsBefore);
            ps.setInt(4, unitsAfter);
            ps.setString(5, changeType);
            if (tradeId != null) ps.setInt(6, tradeId); else ps.setNull(6, java.sql.Types.INTEGER);
            if (ipoId   != null) ps.setInt(7, ipoId);   else ps.setNull(7, java.sql.Types.INTEGER);
            ps.executeUpdate();
        }
    }
    public void insert(int investorId, int assetId,
                       int unitsBefore, int unitsAfter,
                       String changeType,
                       Integer tradeId, Integer ipoId) throws SQLException {
        try (Connection conn = ServerConnector.DBConnection()) {
            insert(conn, investorId, assetId, unitsBefore, unitsAfter, changeType, tradeId, ipoId);
        }
    }

    //list by investor
    public List<OwnershipHistory> listByInvestorId(int investorId) throws SQLException {
        String sql = """
            SELECT history_id, investor_id, asset_id, units_before, units_after, change_date, change_type, trade_id, ipo_id
            FROM ownership_history
            WHERE investor_id = ?
            ORDER BY change_date DESC
        """;

        List<OwnershipHistory> out = new ArrayList<>();
        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, investorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new OwnershipHistory(
                            rs.getInt("history_id"),
                            rs.getInt("investor_id"),
                            rs.getInt("asset_id"),
                            rs.getInt("units_before"),
                            rs.getInt("units_after"),
                            rs.getDate("change_date").toLocalDate(),
                            rs.getString("change_type"),
                            rs.getInt("trade_id"),
                            rs.getInt("ipo_id")
                    ));
                }
            }
        }
        return out;
    }

    // list by asset
    public List<OwnershipHistory> listByAssetId(int assetId) throws SQLException {
        String sql = """
            SELECT history_id, investor_id, asset_id, units_before, units_after, change_date, change_type, trade_id, ipo_id
            FROM ownership_history
            WHERE asset_id = ?
            ORDER BY change_date DESC
        """;

        List<OwnershipHistory> out = new ArrayList<>();
        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, assetId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new OwnershipHistory(
                            rs.getInt("history_id"),
                            rs.getInt("investor_id"),
                            rs.getInt("asset_id"),
                            rs.getInt("units_before"),
                            rs.getInt("units_after"),
                            rs.getDate("change_date").toLocalDate(),
                            rs.getString("change_type"),
                            rs.getInt("trade_id"),
                            rs.getInt("ipo_id")
                    ));
                }
            }
        }
        return out;
    }
}