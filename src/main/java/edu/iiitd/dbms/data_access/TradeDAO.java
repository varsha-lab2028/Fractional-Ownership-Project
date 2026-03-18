package edu.iiitd.dbms.data_access;

import edu.iiitd.dbms.config.ServerConnector;
import edu.iiitd.dbms.domain.Trade;
import edu.iiitd.dbms.dto.TradeWithPartiesDTO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TradeDAO {

    private Trade map(ResultSet rs) throws SQLException {
        return new Trade(
            rs.getInt("trade_id"), rs.getDouble("trade_price"), rs.getInt("trade_units"),
            rs.getDate("trade_date") != null ? rs.getDate("trade_date").toLocalDate() : null,
            rs.getInt("buy_order_id"), rs.getInt("sell_order_id")
        );
    }

    public List<Trade> listTrades() throws SQLException {
        List<Trade> list = new ArrayList<>();
        try (Connection c = ServerConnector.DBConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM trade ORDER BY trade_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public Trade findByTradeId(int id) throws SQLException {
        try (Connection c = ServerConnector.DBConnection()) { return findByTradeId(c, id); }
    }
    public Trade findByTradeId(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM trade WHERE trade_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public void insertTrade(Trade t) throws SQLException {
        try (Connection c = ServerConnector.DBConnection()) { insertTrade(c, t); }
    }
    public void insertTrade(Connection c, Trade t) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO trade (trade_id, trade_price, trade_units, trade_date, buy_order_id, sell_order_id) VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setInt(1, t.getTradeId());    ps.setDouble(2, t.getTradePrice());
            ps.setInt(3, t.getTradeUnits()); ps.setDate(4, Date.valueOf(t.getTradeDate()));
            ps.setInt(5, t.getBuyOrderId()); ps.setInt(6, t.getSellOrderId());
            ps.executeUpdate();
        }
    }

    public List<Trade> listTradesByBuyOrder(int buyOrderId) throws SQLException {
        List<Trade> list = new ArrayList<>();
        try (Connection c = ServerConnector.DBConnection();
             PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM trade WHERE buy_order_id = ? ORDER BY trade_date DESC, trade_id DESC")) {
            ps.setInt(1, buyOrderId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(map(rs)); }
        }
        return list;
    }

    public List<Trade> listTradesBySellOrder(int sellOrderId) throws SQLException {
        List<Trade> list = new ArrayList<>();
        try (Connection c = ServerConnector.DBConnection();
             PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM trade WHERE sell_order_id = ? ORDER BY trade_date DESC, trade_id DESC")) {
            ps.setInt(1, sellOrderId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(map(rs)); }
        }
        return list;
    }

    // Q16 — trades with buyer and seller names
    public List<TradeWithPartiesDTO> getTradesWithParties() throws SQLException {
        String sql = """
            SELECT t.trade_id, t.trade_date, t.trade_price, t.trade_units, bo.asset_id,
                   bi.investor_id AS buyer_id,  bi.name AS buyer_name,
                   si.investor_id AS seller_id, si.name AS seller_name
            FROM TRADE t
            JOIN TRADE_ORDER bo ON t.buy_order_id  = bo.order_id
            JOIN TRADE_ORDER so ON t.sell_order_id = so.order_id
            JOIN INVESTOR bi ON bo.investor_id = bi.investor_id
            JOIN INVESTOR si ON so.investor_id = si.investor_id
            ORDER BY t.trade_date DESC, t.trade_id DESC
            """;
        List<TradeWithPartiesDTO> list = new ArrayList<>();
        try (Connection c = ServerConnector.DBConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new TradeWithPartiesDTO(
                    rs.getInt("trade_id"), rs.getDate("trade_date").toLocalDate(),
                    rs.getDouble("trade_price"), rs.getInt("trade_units"), rs.getInt("asset_id"),
                    rs.getInt("buyer_id"), rs.getString("buyer_name"),
                    rs.getInt("seller_id"), rs.getString("seller_name")));
            }
        }
        return list;
    }
}
