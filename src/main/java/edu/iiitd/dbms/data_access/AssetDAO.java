package edu.iiitd.dbms.data_access;

import edu.iiitd.dbms.config.ServerConnector;
import edu.iiitd.dbms.domain.Asset;
import edu.iiitd.dbms.dto.AssetWithAdminDTO;
import edu.iiitd.dbms.dto.InvestorMarketView.AssetValuationRow;
import edu.iiitd.dbms.dto.InvestorMarketView.VerifiedAssetRow;

import java.sql.*;
import java.util.*;

public class AssetDAO {

    private Asset map(ResultSet rs) throws SQLException {
        int verifiedBy = rs.getInt("verified_by");
        return new Asset(
            rs.getInt("asset_id"),
            rs.getString("name"),
            rs.getString("category"),
            rs.getString("description"),
            rs.getString("storage_location"),
            rs.getString("verification_reference"),
            rs.getString("verification_status"),
            rs.wasNull() ? null : verifiedBy
        );
    }

    public List<Asset> listAssets() throws SQLException {
        String sql = "SELECT asset_id, asset_name AS name, category, description, storage_location, verification_reference, verification_status, verified_by FROM asset ORDER BY asset_id";
        List<Asset> list = new ArrayList<>();
        try (Connection c = ServerConnector.DBConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<Asset> searchAssets(String keyword) throws SQLException {
        String like = "%" + keyword + "%";
        String sql = "SELECT asset_id, asset_name AS name, category, description, storage_location, verification_reference, verification_status, verified_by FROM asset WHERE CAST(asset_id AS CHAR) LIKE ? OR asset_name LIKE ? OR category LIKE ? OR storage_location LIKE ?";
        List<Asset> list = new ArrayList<>();
        try (Connection c = ServerConnector.DBConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, like); ps.setString(2, like); ps.setString(3, like); ps.setString(4, like);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(map(rs)); }
        }
        return list;
    }

    public Asset findByAssetId(int id) throws SQLException {
        try (Connection c = ServerConnector.DBConnection()) { return findByAssetId(c, id); }
    }
    public Asset findByAssetId(Connection c, int id) throws SQLException {
        String sql = "SELECT asset_id, asset_name AS name, category, description, storage_location, verification_reference, verification_status, verified_by FROM asset WHERE asset_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    // Q1 — verified assets
    public List<VerifiedAssetRow> getVerifiedAssets() throws SQLException {
        String sql = """
            SELECT a.asset_id, a.asset_name AS name, a.category, a.storage_location, a.verification_status,
                   COALESCE(i.price_per_unit, 0.0) AS price_per_unit,
                   COALESCE(i.total_units, 0) AS total_units
            FROM ASSET a
            LEFT JOIN IPO i ON a.asset_id = i.asset_id
            WHERE a.verification_status = 'Verified'
            ORDER BY a.asset_id
            """;
        List<VerifiedAssetRow> list = new ArrayList<>();
        try (Connection c = ServerConnector.DBConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new VerifiedAssetRow(
                    rs.getInt("asset_id"), rs.getString("name"), rs.getString("category"),
                    rs.getString("storage_location"), rs.getString("verification_status"),
                    rs.getDouble("price_per_unit"), rs.getInt("total_units")
                ));
            }
        }
        return list;
    }

    // Q2 — assets with admin
    public List<AssetWithAdminDTO> getAssetsWithAdmin() throws SQLException {
        String sql = "SELECT a.asset_id, a.asset_name AS asset_name, a.verification_status, ad.admin_id, ad.name AS admin_name FROM ASSET a LEFT JOIN ADMIN ad ON ad.admin_id = a.verified_by ORDER BY a.asset_id";
        List<AssetWithAdminDTO> list = new ArrayList<>();
        try (Connection c = ServerConnector.DBConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int adminId = rs.getInt("admin_id");
                list.add(new AssetWithAdminDTO(rs.getInt("asset_id"), rs.getString("asset_name"), rs.getString("verification_status"), rs.wasNull() ? null : adminId, rs.getString("admin_name")));
            }
        }
        return list;
    }

    // Q9 — assets with valuation
    public List<AssetValuationRow> getAssetsWithValuation() throws SQLException {
        String sql = "SELECT a.asset_id, a.asset_name AS asset_name, v.valuation_amount FROM ASSET a LEFT JOIN VALUATION v ON a.asset_id = v.asset_id";
        List<AssetValuationRow> list = new ArrayList<>();
        try (Connection c = ServerConnector.DBConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                double val = rs.getObject("valuation_amount") == null ? 0.0 : rs.getDouble("valuation_amount");
                list.add(new AssetValuationRow(rs.getInt("asset_id"), rs.getString("asset_name"), val));
            }
        }
        return list;
    }

    public List<VerifiedAssetRow> searchVerifiedAssetsByName(String keyword) throws SQLException {
        String sql = "SELECT asset_id, asset_name AS name, category, storage_location, verification_status FROM ASSET WHERE verification_status = 'Verified' AND LOWER(asset_name) LIKE LOWER(?)";
        List<VerifiedAssetRow> list = new ArrayList<>();
        try (Connection c = ServerConnector.DBConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new VerifiedAssetRow(rs.getInt("asset_id"), rs.getString("name"), rs.getString("category"), rs.getString("storage_location"), rs.getString("verification_status")));
                }
            }
        }
        return list;
    }

    public List<VerifiedAssetRow> getVerifiedAssetsByCategory(String category) throws SQLException {
        String sql = "SELECT asset_id, asset_name AS name, category, storage_location, verification_status FROM ASSET WHERE verification_status = 'Verified' AND LOWER(category) = LOWER(?)";
        List<VerifiedAssetRow> list = new ArrayList<>();
        try (Connection c = ServerConnector.DBConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, category);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new VerifiedAssetRow(rs.getInt("asset_id"), rs.getString("name"), rs.getString("category"), rs.getString("storage_location"), rs.getString("verification_status")));
                }
            }
        }
        return list;
    }
}
