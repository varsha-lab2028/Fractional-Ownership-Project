package edu.iiitd.dbms.data_access;

import edu.iiitd.dbms.config.ServerConnector;
import edu.iiitd.dbms.domain.IPO;

import java.sql.*;
import java.math.*;
import java.util.*;

//why this DAO exists = asset ↔ IPO link + list for market/admin
public class IpoDAO {
    private IPO matchIpoColumns(ResultSet rs) throws SQLException {
        return new IPO(
                rs.getInt("ipo_id"),
                rs.getInt("asset_id"),
                rs.getInt("total_units"),
                rs.getBigDecimal("price_per_unit"),
                rs.getDate("ipo_start_date").toLocalDate(),
                rs.getDate("ipo_end_date").toLocalDate(),
                rs.getInt("lock_in_period")
        );
    }

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
}
