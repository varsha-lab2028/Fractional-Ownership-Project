package edu.iiitd.dbms.data_access;

import edu.iiitd.dbms.config.ServerConnector;
import edu.iiitd.dbms.domain.IPO;
import edu.iiitd.dbms.dto.InvestorMarketView.MarketViewRow;

import java.sql.*;
import java.util.*;

//this DAO exists for the relationship = asset ↔ IPO link + list for market/admin
public class IpoDAO {
    private IPO matchIpoColumns(ResultSet rs) throws SQLException {
        return new IPO(
                rs.getInt("ipo_id"),
                rs.getInt("asset_id"),
                rs.getInt("total_units"),
                rs.getDouble("price_per_unit"),
                rs.getDate("ipo_start_date").toLocalDate(),
                rs.getDate("ipo_end_date").toLocalDate(),
                rs.getInt("lock_in_period")
        );
    }

    //required methods
    //get all the IPOs
    public List<IPO> listIPOs() throws SQLException {
        String sqlQuery = """
                SELECT * FROM ipo ORDER BY ipo_id
                """;
        List<IPO> IPOList = new ArrayList<>();
        try(Connection connect = ServerConnector.DBConnection();
            PreparedStatement ps = connect.prepareStatement(sqlQuery);
            ResultSet rs = ps.executeQuery()){
            while(rs.next()){
                IPOList.add(matchIpoColumns(rs));
            }
        }
        return IPOList;
    }

    //get by ipo id
    public IPO findByIpoId(int ipoId) throws SQLException {
        String sqlQuery = """
                SELECT * FROM ipo WHERE ipo_id = ?
                """;
        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement ps = conn.prepareStatement(sqlQuery)) {
            ps.setInt(1, ipoId);
            try (ResultSet rs = ps.executeQuery()) {
                if(rs.next()){
                    return matchIpoColumns(rs);
                } else {
                    return null;
                }
            }
        }
    }

    //get by asset id
    public IPO findByAssetId(int assetId) throws SQLException {
        try (Connection connect = ServerConnector.DBConnection()){
            return findByAssetId(connect, assetId);
        }
    }
    public IPO findByAssetId(Connection connect, int assetId) throws SQLException {
        String sqlQuery = """
                SELECT * FROM ipo WHERE asset_id = ?
                """;
        try(PreparedStatement ps = connect.prepareStatement(sqlQuery)){
            ps.setInt(1, assetId);
            try (ResultSet rs = ps.executeQuery()){
                if(rs.next()){
                    return matchIpoColumns(rs);
                } else {
                    return null;
                }
            }
        }
    }

    //Active IPOs = today between start and end date
    //one of the 15 sql queries used here
    //Q4
    public List<IPO> listActiveIpos() throws SQLException {
        String sqlQuery = """
                SELECT * FROM ipo WHERE CURDATE() BETWEEN ipo_start_date AND ipo_end_date
                ORDER BY ipo_start_date DESC
                """;
        List<IPO> ActiveIposList = new ArrayList<>();
        try (Connection connect = ServerConnector.DBConnection();
            PreparedStatement ps = connect.prepareStatement(sqlQuery);
            ResultSet rs = ps.executeQuery()){
            while(rs.next()){
                ActiveIposList.add(matchIpoColumns(rs));
            }
        }
        return ActiveIposList;
    }

    //Q3
    public List<MarketViewRow> getAllMarketRows() throws SQLException {
        List<MarketViewRow> marketViewRowsList = new ArrayList<>();
        String sqlQuery = """
                SELECT i.ipo_id,
                       i.asset_id,
                       a.name AS asset_name,
                       a.category,
                       i.total_units,
                       i.price_per_unit,
                       i.ipo_start_date,
                       i.ipo_end_date,
                       i.lock_in_period
                FROM IPO i
                JOIN ASSET a ON a.asset_id = i.asset_id
                ORDER BY i.ipo_id
                """;
        try (Connection con = ServerConnector.DBConnection();
             PreparedStatement ps = con.prepareStatement(sqlQuery);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                MarketViewRow row = new MarketViewRow(
                        rs.getInt("ipo_id"),
                        rs.getInt("asset_id"),
                        rs.getString("asset_name"),
                        rs.getString("category"),
                        rs.getInt("total_units"),
                        rs.getDouble("price_per_unit"),
                        rs.getDate("ipo_start_date"),
                        rs.getDate("ipo_end_date"),
                        rs.getInt("lock_in_period")
                );
                marketViewRowsList.add(row);
            }
        }
        return marketViewRowsList;
    }

    //search by asset name
    public List<MarketViewRow> searchMarketRowsByAssetName(String keyword) throws SQLException {
        List<MarketViewRow> marketRowsByAssetNameList = new ArrayList<>();
        String sqlQuery = """
                SELECT i.ipo_id,
                       i.asset_id,
                       a.name AS asset_name,
                       a.category,
                       i.total_units,
                       i.price_per_unit,
                       i.ipo_start_date,
                       i.ipo_end_date,
                       i.status
                FROM IPO i
                JOIN ASSET a ON a.asset_id = i.asset_id
                WHERE LOWER(a.name) LIKE LOWER(?)
                ORDER BY i.ipo_id
                """;
        try (Connection con = ServerConnector.DBConnection();
             PreparedStatement ps = con.prepareStatement(sqlQuery)) {
            ps.setString(1, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MarketViewRow row = new MarketViewRow(
                            rs.getInt("ipo_id"),
                            rs.getInt("asset_id"),
                            rs.getString("asset_name"),
                            rs.getString("category"),
                            rs.getInt("total_units"),
                            rs.getDouble("price_per_unit"),
                            rs.getDate("ipo_start_date"),
                            rs.getDate("ipo_end_date"),
                            rs.getInt("lock_in_period")
                    );
                    marketRowsByAssetNameList.add(row);
                }
            }
        }
        return marketRowsByAssetNameList;
    }

    //filtering by category
    public List<MarketViewRow> getMarketRowsByCategory(String category) throws SQLException {
        List<MarketViewRow> marketRowsByCategoryList = new ArrayList<>();
        String sqlQuery = """
                SELECT i.ipo_id,
                       i.asset_id,
                       a.name AS asset_name,
                       a.category,
                       i.total_units,
                       i.price_per_unit,
                       i.ipo_start_date,
                       i.ipo_end_date,
                       i.lock_in_period
                FROM IPO i
                JOIN ASSET a ON a.asset_id = i.asset_id
                WHERE LOWER(a.category) = LOWER(?)
                ORDER BY i.ipo_id
                """;
        try(Connection connect = ServerConnector.DBConnection();
            PreparedStatement ps = connect.prepareStatement(sqlQuery)) {
            ps.setString(1, category);

            try (ResultSet rs = ps.executeQuery()){
                while(rs.next()){
                    MarketViewRow row = new MarketViewRow(
                            rs.getInt("ipo_id"),
                            rs.getInt("asset_id"),
                            rs.getString("asset_name"),
                            rs.getString("category"),
                            rs.getInt("total_units"),
                            rs.getDouble("price_per_unit"),
                            rs.getDate("ipo_start_date"),
                            rs.getDate("ipo_end_date"),
                            rs.getInt("lock_in_period")
                    );
                    marketRowsByCategoryList.add(row);
                }
            }
        }
        return marketRowsByCategoryList;
    }

    //filter by IPO status
    /*
    public List<MarketViewRow> getMarketRowsByStatus(String status) throws SQLException {
        List<MarketViewRow> marketStatusByStatusList = new ArrayList<>();
        String sqlQuery = """
                SELECT i.ipo_id,
                       i.asset_id,
                       a.name AS asset_name,
                       a.category,
                       i.total_units,
                       i.price_per_unit,
                       i.ipo_start_date,
                       i.ipo_end_date,
                       a.verification_status
                FROM IPO i
                JOIN ASSET a ON a.asset_id = i.asset_id
                WHERE LOWER(a.verification_status) = LOWER(?)
                ORDER BY i.ipo_id
                """;
        try(Connection connect = ServerConnector.DBConnection();
            PreparedStatement ps = connect.prepareStatement(sqlQuery)){
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()){
                while(rs.next()){
                    MarketViewRow row = new MarketViewRow(
                            rs.getInt("ipo_id"),
                            rs.getInt("asset_id"),
                            rs.getString("asset_name"),
                            rs.getString("category"),
                            rs.getInt("total_units"),
                            rs.getDouble("price_per_unit"),
                            rs.getDate("ipo_start_date"),
                            rs.getDate("ipo_end_date"),
                            rs.getString("verification_status")
                    );
                    marketStatusByStatusList.add(row);
                }
            }
        }
        return marketStatusByStatusList;
    }*/
}
