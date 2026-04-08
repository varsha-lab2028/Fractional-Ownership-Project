package edu.iiitd.dbms.data_access;

import edu.iiitd.dbms.config.ServerConnector;
import edu.iiitd.dbms.domain.Ownership;
import edu.iiitd.dbms.dto.AssetUnitsSummaryDTO;
import edu.iiitd.dbms.dto.InvestorUnitsSummaryDTO;
import edu.iiitd.dbms.dto.OwnershipDetailDTO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OwnershipDAO {

    public Ownership findOwnership(int investorId, int assetId) throws SQLException {
        try (Connection c = ServerConnector.DBConnection()) { return findOwnership(c, investorId, assetId); }
    }

    // 7.4 — FOR UPDATE lock on ownership row inside shared transaction
    public Ownership findOwnership(Connection c, int investorId, int assetId) throws SQLException {
        String sql = "SELECT investor_id, asset_id, units_held FROM ownership WHERE investor_id = ? AND asset_id = ? FOR UPDATE";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, investorId); ps.setInt(2, assetId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new Ownership(rs.getInt("investor_id"), rs.getInt("asset_id"), rs.getInt("units_held")) : null;
            }
        }
    }

    public void updateUnits(int investorId, int assetId, int newUnits) throws SQLException {
        try (Connection c = ServerConnector.DBConnection()) { updateUnits(c, investorId, assetId, newUnits); }
    }

    public void updateUnits(Connection c, int investorId, int assetId, int newUnits) throws SQLException {
        // Use ON CONFLICT (Postgres) — upsert ownership row
        String sql = """
            INSERT INTO ownership (investor_id, asset_id, units_held)
            VALUES (?, ?, ?)
            ON CONFLICT (investor_id, asset_id)
            DO UPDATE SET units_held = EXCLUDED.units_held
            """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, investorId); ps.setInt(2, assetId); ps.setInt(3, newUnits);
            ps.executeUpdate();
        }
    }

    public void insert(int investorId, int assetId, int units) throws SQLException {
        try (Connection c = ServerConnector.DBConnection()) { insert(c, investorId, assetId, units); }
    }

    public void insert(Connection c, int investorId, int assetId, int units) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO ownership (investor_id, asset_id, units_held) VALUES (?, ?, ?)")) {
            ps.setInt(1, investorId); ps.setInt(2, assetId); ps.setInt(3, units);
            ps.executeUpdate();
        }
    }

    public void deleteIfZero(int investorId, int assetId) throws SQLException {
        try (Connection c = ServerConnector.DBConnection()) { deleteIfZero(c, investorId, assetId); }
    }

    public void deleteIfZero(Connection c, int investorId, int assetId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "DELETE FROM ownership WHERE investor_id = ? AND asset_id = ? AND units_held = 0")) {
            ps.setInt(1, investorId); ps.setInt(2, assetId); ps.executeUpdate();
        }
    }

    public List<Ownership> listByInvestor(int investorId) throws SQLException { return listHoldings(investorId); }

    public List<Ownership> listHoldings(int investorId) throws SQLException {
        List<Ownership> list = new ArrayList<>();
        try (Connection c = ServerConnector.DBConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT investor_id, asset_id, units_held FROM ownership WHERE investor_id = ? ORDER BY asset_id")) {
            ps.setInt(1, investorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(new Ownership(rs.getInt("investor_id"), rs.getInt("asset_id"), rs.getInt("units_held")));
            }
        }
        return list;
    }

    public List<OwnershipDetailDTO> getAllOwnershipDetails() throws SQLException {
        String sql = "SELECT o.investor_id, inv.investor_name AS investor_name, a.asset_name AS asset_name, o.units_held FROM ownership o JOIN investor inv ON inv.investor_id = o.investor_id JOIN asset a ON a.asset_id = o.asset_id ORDER BY o.investor_id, a.asset_id";
        List<OwnershipDetailDTO> list = new ArrayList<>();
        try (Connection c = ServerConnector.DBConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(new OwnershipDetailDTO(rs.getInt("investor_id"), rs.getString("investor_name"), rs.getString("asset_name"), rs.getInt("units_held")));
        }
        return list;
    }

    public List<AssetUnitsSummaryDTO> getTotalUnitsByAsset() throws SQLException {
        List<AssetUnitsSummaryDTO> list = new ArrayList<>();
        try (Connection c = ServerConnector.DBConnection();
             PreparedStatement ps = c.prepareStatement("SELECT asset_id, SUM(units_held) AS total_units FROM ownership GROUP BY asset_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(new AssetUnitsSummaryDTO(rs.getInt("asset_id"), rs.getInt("total_units")));
        }
        return list;
    }

    public List<InvestorUnitsSummaryDTO> getTotalUnitsByInvestor() throws SQLException {
        List<InvestorUnitsSummaryDTO> list = new ArrayList<>();
        try (Connection c = ServerConnector.DBConnection();
             PreparedStatement ps = c.prepareStatement("SELECT investor_id, SUM(units_held) AS total_units FROM ownership GROUP BY investor_id ORDER BY total_units DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(new InvestorUnitsSummaryDTO(rs.getInt("investor_id"), rs.getInt("total_units")));
        }
        return list;
    }

    public List<Integer> getAssetIdsWithNoTradeOrders() throws SQLException {
        List<Integer> list = new ArrayList<>();
        try (Connection c = ServerConnector.DBConnection();
             PreparedStatement ps = c.prepareStatement("SELECT asset_id FROM asset a WHERE NOT EXISTS (SELECT 1 FROM trade_order o WHERE o.asset_id = a.asset_id)");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(rs.getInt("asset_id"));
        }
        return list;
    }

    public List<Integer> getInvestorIdsWithTradeOrders() throws SQLException {
        List<Integer> list = new ArrayList<>();
        try (Connection c = ServerConnector.DBConnection();
             PreparedStatement ps = c.prepareStatement("SELECT investor_id FROM investor i WHERE EXISTS (SELECT 1 FROM trade_order o WHERE o.investor_id = i.investor_id)");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(rs.getInt("investor_id"));
        }
        return list;
    }

    public List<Integer> getActiveInvestorIds() throws SQLException {
        List<Integer> list = new ArrayList<>();
        try (Connection c = ServerConnector.DBConnection();
             PreparedStatement ps = c.prepareStatement("SELECT investor_id FROM ownership UNION SELECT investor_id FROM trade_order");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(rs.getInt("investor_id"));
        }
        return list;
    }

    public List<AssetUnitsSummaryDTO> getOverAllocatedAssets() throws SQLException {
        String sql = "SELECT i.asset_id, i.total_units, SUM(o.units_held) AS total_held FROM ipo i LEFT JOIN ownership o ON o.asset_id = i.asset_id GROUP BY i.asset_id, i.total_units HAVING SUM(o.units_held) > i.total_units";
        List<AssetUnitsSummaryDTO> list = new ArrayList<>();
        try (Connection c = ServerConnector.DBConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(new AssetUnitsSummaryDTO(rs.getInt("asset_id"), rs.getInt("total_held")));
        }
        return list;
    }

    public List<Integer> getInvestorIdsNeverTraded() throws SQLException {
        List<Integer> list = new ArrayList<>();
        try (Connection c = ServerConnector.DBConnection();
             PreparedStatement ps = c.prepareStatement("SELECT investor_id FROM ownership WHERE investor_id NOT IN (SELECT investor_id FROM trade_order)");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(rs.getInt("investor_id"));
        }
        return list;
    }
}
