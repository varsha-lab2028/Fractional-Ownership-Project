package edu.iiitd.dbms.data_access;

import edu.iiitd.dbms.config.DBConnection;
import edu.iiitd.dbms.config.ServerConnector;
import edu.iiitd.dbms.domain.Asset;

import edu.iiitd.dbms.dto.AssetWithAdminDTO;
import edu.iiitd.dbms.dto.InvestorMarketView.AssetValuationRow;
import edu.iiitd.dbms.dto.InvestorMarketView.VerifiedAssetRow;

import java.sql.*;
import java.util.*;

//listing Assets, search an asset using a keyword, finding an asset by asset id
//read + search
public class AssetDAO {
    private Asset matchAssetColumns(ResultSet rs) throws SQLException {
        return new Asset(
                rs.getInt("asset_id"),
                rs.getString("asset_name"),
                rs.getString("category"),
                rs.getString("description"),
                rs.getString("storage_location"),
                rs.getString("verification_reference"),
                rs.getString("verification_status"),
                rs.getInt("verified_by")
        );
    }

    //required methods
    //gets all the assets
    public List<Asset> listAssets() throws SQLException {
        String sqlQuery = """
                SELECT asset_id, asset_name, category, description, storage_location,
                       verification_reference, verification_status, verified_by
                FROM asset
                ORDER BY asset_id""";
        List<Asset> assetList = new ArrayList<>();
        try (Connection connect = ServerConnector.DBConnection();
             PreparedStatement ps = connect.prepareStatement(sqlQuery);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                assetList.add(matchAssetColumns(rs));
            }
        }
        return assetList;
    }

    public List<Asset> searchAssets(String keyword) throws SQLException {
        String like = "%" + keyword + "%";
        String sqlQuery = """
                SELECT asset_id, asset_name, category, description, storage_location,
                       verification_reference, verification_status, verified_by
                FROM asset
                WHERE CAST(asset_id AS CHAR) LIKE ? OR asset_name LIKE ?
                   OR category LIKE ? OR storage_location LIKE ?
                """;

        List<Asset> assetList = new ArrayList<>();
        try (Connection connect = ServerConnector.DBConnection();
             PreparedStatement ps = connect.prepareStatement(sqlQuery)) {
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    assetList.add(matchAssetColumns(rs));
                }
            }
        }
        return assetList;
    }

    //get by asset id
    public Asset findByAssetId(int assetId) throws SQLException {
        try (Connection connect = ServerConnector.DBConnection()) {
            return findByAssetId(connect, assetId);
        }
    }
    public Asset findByAssetId(Connection connect, int assetId) throws SQLException {
        String sqlQuery = """
            SELECT asset_id, asset_name, category, description, storage_location,
                   verification_reference, verification_status, verified_by
            FROM asset
            WHERE asset_id = ?
        """;

        try (PreparedStatement ps = connect.prepareStatement(sqlQuery)) {
            ps.setInt(1, assetId);
            try (ResultSet rs = ps.executeQuery()) {
                if(rs.next()){
                    return matchAssetColumns(rs);
                } else {
                    return null;
                }
            }
        }
    }

    //applying some of the 15 sql queries used here

    //Q2 - Asset list joined with the admin who verified it (LEFT JOIN so unverified assets appear too)
    public List<AssetWithAdminDTO> getAssetsWithAdmin() throws SQLException {
        String sql = """
            SELECT a.asset_id, a.asset_name, a.verification_status,
                   ad.admin_id, ad.name AS admin_name
            FROM ASSET a
            LEFT JOIN ADMIN ad ON ad.admin_id = a.verified_by
            ORDER BY a.asset_id
        """;
        List<AssetWithAdminDTO> result = new ArrayList<>();
        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int adminId = rs.getInt("admin_id");
                result.add(new AssetWithAdminDTO(
                        rs.getInt("asset_id"),
                        rs.getString("asset_name"),
                        rs.getString("verification_status"),
                        rs.wasNull() ? null : adminId,
                        rs.getString("admin_name")
                ));
            }
        }
        return result;
    }

    //Q1
    public List<VerifiedAssetRow> getVerifiedAssets() throws SQLException {
        List<VerifiedAssetRow> verifiedAssetRows = new ArrayList<>();
        String sqlQuery = """
                SELECT asset_id, asset_name, category, storage_location, verification_status
                FROM ASSET
                WHERE verification_status = 'Verified'
                """;
        try (Connection connect = ServerConnector.DBConnection();
             PreparedStatement ps = connect.prepareStatement(sqlQuery);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                VerifiedAssetRow row = new VerifiedAssetRow(
                        rs.getInt("asset_id"),
                        rs.getString("asset_name"),
                        rs.getString("category"),
                        rs.getString("storage_location"),
                        rs.getString("verification_status")
                );
                verifiedAssetRows.add(row);
            }
        }
        return verifiedAssetRows;
    }

    //Q8/Q9
    public List<AssetValuationRow> getAssetsWithValuation() throws SQLException {
        List<AssetValuationRow> assetValuationRows = new ArrayList<>();
        String sqlQuery = """
                SELECT a.asset_id, a.asset_name, v.valuation_amount
                FROM ASSET a
                LEFT JOIN VALUATION v ON a.asset_id = v.asset_id
                """;
        try (Connection connect = ServerConnector.DBConnection();
             PreparedStatement ps = connect.prepareStatement(sqlQuery);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                double valuation = rs.getObject("valuation_amount") == null
                        ? 0.0
                        : rs.getDouble("valuation_amount");

                AssetValuationRow row = new AssetValuationRow(
                        rs.getInt("asset_id"),
                        rs.getString("asset_name"),
                        valuation
                );
                assetValuationRows.add(row);
            }
        }
        return assetValuationRows;
    }

    //searching verified assets
    public List<VerifiedAssetRow> searchVerifiedAssetsByName(String keyword) throws SQLException {
        List<VerifiedAssetRow> assets = new ArrayList<>();
        String sqlQuery = """
                SELECT asset_id, asset_name, category, storage_location, verification_status
                FROM ASSET
                WHERE verification_status = 'Verified'
                  AND LOWER(asset_name) LIKE LOWER(?)
                """;
        try (Connection con = ServerConnector.DBConnection();
             PreparedStatement ps = con.prepareStatement(sqlQuery)) {
            ps.setString(1, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    VerifiedAssetRow row = new VerifiedAssetRow(
                            rs.getInt("asset_id"),
                            rs.getString("asset_name"),
                            rs.getString("category"),
                            rs.getString("storage_location"),
                            rs.getString("verification_status")
                    );
                    assets.add(row);
                }
            }
        }
        return assets;
    }

    //category filter
    public List<VerifiedAssetRow> getVerifiedAssetsByCategory(String category) throws SQLException {
        List<VerifiedAssetRow> verifiedAssetsByCategory = new ArrayList<>();
        String sqlQuery = """
                SELECT asset_id, asset_name, category, storage_location, verification_status
                FROM ASSET
                WHERE verification_status = 'Verified'
                  AND LOWER(category) = LOWER(?)
                """;
        try (Connection con = ServerConnector.DBConnection();
             PreparedStatement ps = con.prepareStatement(sqlQuery)) {
            ps.setString(1, category);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    VerifiedAssetRow row = new VerifiedAssetRow(
                            rs.getInt("asset_id"),
                            rs.getString("asset_name"),
                            rs.getString("category"),
                            rs.getString("storage_location"),
                            rs.getString("verification_status")
                    );
                    verifiedAssetsByCategory.add(row);
                }
            }
        }
        return verifiedAssetsByCategory;
    }
}
