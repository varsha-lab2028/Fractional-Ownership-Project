package edu.iiitd.dbms.data_access;

import edu.iiitd.dbms.config.ServerConnector;
import edu.iiitd.dbms.domain.Asset;

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

    public List<Asset> listAssets() throws SQLException {
        String sqlQuery = """
                SELECT asset_id, name, category, description, storage_location, verification_reference, " +
                verification_status, verified_by FROM asset ORDER BY asset_id""";
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
                SELECT asset_id, name, category, description, storage_location, " +
                "verification_reference, verification_status, verified_by FROM asset 
                WHERE asset_id LIKE ? OR name LIKE ? OR category LIKE ? OR 
                storage_location LIKE ?
                """;

        List<Asset> assetList = new ArrayList<>();
        try (Connection connect = ServerConnector.DBConnection();
             PreparedStatement ps = connect.prepareStatement(sqlQuery)) {
            ps.setString(1, like);
            ps.setString(2, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    assetList.add(matchAssetColumns(rs));
                }
            }
        }
        return assetList;
    }

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


}
