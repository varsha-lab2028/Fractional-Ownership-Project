package edu.iiitd.dbms.data_access;

import edu.iiitd.dbms.config.ServerConnector;
import edu.iiitd.dbms.domain.IPO;
import edu.iiitd.dbms.dto.InvestorMarketView.MarketViewRow;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

public class IpoDAO {

    private IPO map(ResultSet rs) throws SQLException {
        return new IPO(
            rs.getInt("ipo_id"),
            rs.getInt("asset_id"),
            rs.getInt("total_units"),
            rs.getDouble("price_per_unit"),
            rs.getDate("ipo_start_date") != null ? rs.getDate("ipo_start_date").toLocalDate() : null,
            rs.getDate("ipo_end_date")   != null ? rs.getDate("ipo_end_date").toLocalDate()   : null,
            rs.getInt("lock_in_period")
        );
    }

    private MarketViewRow mapRow(ResultSet rs) throws SQLException {
        return new MarketViewRow(
            rs.getInt("ipo_id"), rs.getInt("asset_id"), rs.getString("asset_name"),
            rs.getString("category"), rs.getInt("total_units"), rs.getDouble("price_per_unit"),
            rs.getDate("ipo_start_date"), rs.getDate("ipo_end_date"), rs.getInt("lock_in_period")
        );
    }

    public List<IPO> listIPOs() throws SQLException {
        List<IPO> list = new ArrayList<>();
        try (Connection c = ServerConnector.DBConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM ipo ORDER BY ipo_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public IPO findByIpoId(int id) throws SQLException {
        try (Connection c = ServerConnector.DBConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM ipo WHERE ipo_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public IPO findByAssetId(int assetId) throws SQLException {
        try (Connection c = ServerConnector.DBConnection()) { return findByAssetId(c, assetId); }
    }
    public IPO findByAssetId(Connection c, int assetId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM ipo WHERE asset_id = ?")) {
            ps.setInt(1, assetId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    // Q4 — active IPOs (CURDATE between start and end)
    public List<IPO> listActiveIpos() throws SQLException {
        List<IPO> list = new ArrayList<>();
        try (Connection c = ServerConnector.DBConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM ipo WHERE CURRENT_DATE BETWEEN ipo_start_date AND ipo_end_date ORDER BY ipo_start_date DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    // Q3 — all market rows
    public List<MarketViewRow> getAllMarketRows() throws SQLException {
        String sql = """
            SELECT i.ipo_id, i.asset_id, a.asset_name AS asset_name, a.category,
                   i.total_units, i.price_per_unit, i.ipo_start_date, i.ipo_end_date, i.lock_in_period
            FROM IPO i JOIN ASSET a ON a.asset_id = i.asset_id
            ORDER BY i.ipo_id
            """;
        List<MarketViewRow> list = new ArrayList<>();
        try (Connection c = ServerConnector.DBConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // Q5 — market rows by reference date
    public List<MarketViewRow> listIposByReferenceDate(LocalDate referenceDate) throws SQLException {
        String sql = """
            SELECT i.ipo_id, i.asset_id, a.asset_name AS asset_name, a.category,
                   i.total_units, i.price_per_unit, i.ipo_start_date, i.ipo_end_date, i.lock_in_period
            FROM IPO i JOIN ASSET a ON a.asset_id = i.asset_id
            WHERE DATE(?) BETWEEN DATE(i.ipo_start_date) AND DATE(i.ipo_end_date)
            ORDER BY i.ipo_id
            """;
        List<MarketViewRow> list = new ArrayList<>();
        try (Connection c = ServerConnector.DBConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(referenceDate));
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapRow(rs)); }
        }
        return list;
    }

    public List<MarketViewRow> searchMarketRowsByAssetName(String keyword) throws SQLException {
        String sql = """
            SELECT i.ipo_id, i.asset_id, a.asset_name AS asset_name, a.category,
                   i.total_units, i.price_per_unit, i.ipo_start_date, i.ipo_end_date, i.lock_in_period
            FROM IPO i JOIN ASSET a ON a.asset_id = i.asset_id
            WHERE LOWER(a.asset_name) LIKE LOWER(?)
            ORDER BY i.ipo_id
            """;
        List<MarketViewRow> list = new ArrayList<>();
        try (Connection c = ServerConnector.DBConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapRow(rs)); }
        }
        return list;
    }

    public List<MarketViewRow> getMarketRowsByCategory(String category) throws SQLException {
        String sql = """
            SELECT i.ipo_id, i.asset_id, a.asset_name AS asset_name, a.category,
                   i.total_units, i.price_per_unit, i.ipo_start_date, i.ipo_end_date, i.lock_in_period
            FROM IPO i JOIN ASSET a ON a.asset_id = i.asset_id
            WHERE LOWER(a.category) = LOWER(?)
            ORDER BY i.ipo_id
            """;
        List<MarketViewRow> list = new ArrayList<>();
        try (Connection c = ServerConnector.DBConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, category);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapRow(rs)); }
        }
        return list;
    }
}