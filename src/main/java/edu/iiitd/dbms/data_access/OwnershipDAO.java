package edu.iiitd.dbms.data_access;

import edu.iiitd.dbms.config.ServerConnector;
import edu.iiitd.dbms.domain.Ownership;
import edu.iiitd.dbms.dto.AssetUnitsSummaryDTO;
import edu.iiitd.dbms.dto.InvestorUnitsSummaryDTO;
import edu.iiitd.dbms.dto.OwnershipDetailDTO;

import javax.xml.transform.Result;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

//finding the ownership, updating the units held by an investor and listing out the contents in ownership
//updating the units is essential for trading
public class OwnershipDAO {
    public Ownership findOwnership(int investorId, int assetId) throws SQLException {
        try(Connection connect = ServerConnector.DBConnection()){
            return findOwnership(connect, investorId, assetId);
        }
    }

    public Ownership findOwnership(Connection connect, int investorId, int assetId) throws SQLException {
        String sqlQuery = "SELECT investor_id, asset_id, units_held FROM ownership WHERE investor_id = ? AND asset_id = ?";
        try(PreparedStatement ps = connect.prepareStatement(sqlQuery)){
            ps.setInt(1, investorId);
            ps.setInt(2, assetId);
            try (ResultSet rs = ps.executeQuery()){
                if (rs.next()){
                    return new Ownership(rs.getInt("investor_id"), rs.getInt("asset_id"), rs.getInt("units_held"));
                }
                return null;
            }
        }
    }

    //updating units method
    public void updateUnits(Connection connect, int investorId, int assetId, int newUnitsHeld) throws SQLException {
        String sqlQuery = "INSERT INTO ownership(investor_id, asset_id, units_held) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE units_held = VALUES(units_held)";
        try(PreparedStatement ps = connect.prepareStatement(sqlQuery)){
            ps.setInt(1, investorId);
            ps.setInt(2, assetId);
            ps.setInt(3, newUnitsHeld);
            ps.executeUpdate();
        }
    }

    // insert a brand-new ownership row
    public void insert(int investorId, int assetId, int unitsHeld) throws SQLException {
        try (Connection connect = ServerConnector.DBConnection()) {
            insert(connect, investorId, assetId, unitsHeld);
        }
    }
    public void insert(Connection connect, int investorId, int assetId, int unitsHeld) throws SQLException {
        String sqlQuery = "INSERT INTO ownership(investor_id, asset_id, units_held) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connect.prepareStatement(sqlQuery)) {
            ps.setInt(1, investorId);
            ps.setInt(2, assetId);
            ps.setInt(3, unitsHeld);
            ps.executeUpdate();
        }
    }

    // delete the ownership row if units_held has dropped to zero
    public void deleteIfZero(int investorId, int assetId) throws SQLException {
        try (Connection connect = ServerConnector.DBConnection()) {
            deleteIfZero(connect, investorId, assetId);
        }
    }
    public void deleteIfZero(Connection connect, int investorId, int assetId) throws SQLException {
        String sqlQuery = "DELETE FROM ownership WHERE investor_id = ? AND asset_id = ? AND units_held = 0";
        try (PreparedStatement ps = connect.prepareStatement(sqlQuery)) {
            ps.setInt(1, investorId);
            ps.setInt(2, assetId);
            ps.executeUpdate();
        }
    }

    //list holdings by investor (alias kept for compatibility)
    public List<Ownership> listByInvestor(int investorId) throws SQLException {
        return listHoldings(investorId);
    }

    //get by investor and asset
    public List<Ownership> listHoldings(int investorId) throws SQLException{
        String sqlQuery = "SELECT investor_id, asset_id, units_held FROM ownership WHERE investor_id = ? ORDER BY asset_id";
        List<Ownership> ownershipList = new ArrayList<>();
        try (Connection connect = ServerConnector.DBConnection();
            PreparedStatement ps = connect.prepareStatement(sqlQuery)){
                ps.setInt(1, investorId);
                try(ResultSet rs = ps.executeQuery()){
                    while(rs.next()){
                        ownershipList.add(new Ownership(
                                rs.getInt("investor_id"),
                                rs.getInt("asset_id"),
                                rs.getInt("units_held")
                        ));
                    }
                }
        }
        return ownershipList;
    }

    //Q6 - all ownership rows joined with investor name and asset name (admin view)
    public List<OwnershipDetailDTO> getAllOwnershipDetails() throws SQLException {
        String sql = """
            SELECT o.investor_id,
                   inv.investor_name,
                   a.asset_name,
                   o.units_held
            FROM OWNERSHIP o
            JOIN INVESTOR inv ON inv.investor_id = o.investor_id
            JOIN ASSET a      ON a.asset_id      = o.asset_id
            ORDER BY o.investor_id, a.asset_id
        """;
        List<OwnershipDetailDTO> result = new ArrayList<>();
        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new OwnershipDetailDTO(
                        rs.getInt("investor_id"),
                        rs.getString("investor_name"),
                        rs.getString("asset_name"),
                        rs.getInt("units_held")
                ));
            }
        }
        return result;
    }

    //Q7 - total units held per asset (admin/reporting view)
    public List<AssetUnitsSummaryDTO> getTotalUnitsByAsset() throws SQLException {
        String sql = "SELECT asset_id, SUM(units_held) AS total_units FROM ownership GROUP BY asset_id";
        List<AssetUnitsSummaryDTO> result = new ArrayList<>();
        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new AssetUnitsSummaryDTO(
                        rs.getInt("asset_id"),
                        rs.getInt("total_units")
                ));
            }
        }
        return result;
    }

    //Q8 - total units held per investor, most invested first
    public List<InvestorUnitsSummaryDTO> getTotalUnitsByInvestor() throws SQLException {
        String sql = """
            SELECT investor_id, SUM(units_held) AS total_units
            FROM ownership
            GROUP BY investor_id
            ORDER BY total_units DESC
        """;
        List<InvestorUnitsSummaryDTO> result = new ArrayList<>();
        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new InvestorUnitsSummaryDTO(
                        rs.getInt("investor_id"),
                        rs.getInt("total_units")
                ));
            }
        }
        return result;
    }

    //Q10 - asset IDs that have never had a trade order placed against them
    public List<Integer> getAssetIdsWithNoTradeOrders() throws SQLException {
        String sql = """
            SELECT asset_id FROM ASSET a
            WHERE NOT EXISTS (
                SELECT 1 FROM TRADE_ORDER o WHERE o.asset_id = a.asset_id
            )
        """;
        List<Integer> result = new ArrayList<>();
        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(rs.getInt("asset_id"));
        }
        return result;
    }

    //Q11 - investor IDs who have at least one trade order (EXISTS)
    public List<Integer> getInvestorIdsWithTradeOrders() throws SQLException {
        String sql = """
            SELECT investor_id FROM INVESTOR i
            WHERE EXISTS (
                SELECT 1 FROM TRADE_ORDER o WHERE o.investor_id = i.investor_id
            )
        """;
        List<Integer> result = new ArrayList<>();
        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(rs.getInt("investor_id"));
        }
        return result;
    }

    //Q12 - all investor IDs active in either ownership or trade_order (UNION deduplicates)
    public List<Integer> getActiveInvestorIds() throws SQLException {
        String sql = """
            SELECT investor_id FROM OWNERSHIP
            UNION
            SELECT investor_id FROM TRADE_ORDER
        """;
        List<Integer> result = new ArrayList<>();
        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(rs.getInt("investor_id"));
        }
        return result;
    }

    //Q13 - assets where total units held across all investors EXCEEDS the IPO total_units
    //      (data integrity / over-allocation check)
    public List<AssetUnitsSummaryDTO> getOverAllocatedAssets() throws SQLException {
        String sql = """
            SELECT i.asset_id, i.total_units, SUM(o.units_held) AS total_held
            FROM IPO i
            LEFT JOIN OWNERSHIP o ON o.asset_id = i.asset_id
            GROUP BY i.asset_id, i.total_units
            HAVING SUM(o.units_held) > i.total_units
        """;
        List<AssetUnitsSummaryDTO> result = new ArrayList<>();
        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new AssetUnitsSummaryDTO(
                        rs.getInt("asset_id"),
                        rs.getInt("total_held")
                ));
            }
        }
        return result;
    }

    //Q14 - investors who hold assets but have never placed a trade order
    public List<Integer> getInvestorIdsNeverTraded() throws SQLException {
        String sql = """
            SELECT investor_id FROM OWNERSHIP
            WHERE investor_id NOT IN (SELECT investor_id FROM TRADE_ORDER)
        """;
        List<Integer> result = new ArrayList<>();
        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(rs.getInt("investor_id"));
        }
        return result;
    }
}
