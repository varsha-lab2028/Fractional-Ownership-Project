package edu.iiitd.dbms.data_access;

import edu.iiitd.dbms.config.ServerConnector;
import edu.iiitd.dbms.domain.Trade;
import edu.iiitd.dbms.dto.TradeWithPartiesDTO;

import java.sql.*;
import java.sql.Date;
import java.util.*;

public class TradeDAO {
    private Trade matchTradeColumns(ResultSet rs) throws SQLException {
        return new Trade(
                rs.getInt("trade_id"),
                rs.getDouble("trade_price"),
                rs.getInt("trade_units"),
                rs.getDate("trade_date").toLocalDate(),
                rs.getInt("buy_order_id"),
                rs.getInt("sell_order_id")
        );
    }

    //list all
    public List<Trade> listTrades() throws SQLException {
        String sql = """
            SELECT *
            FROM trade
            ORDER BY trade_id
        """;
        List<Trade> tradeList = new ArrayList<>();
        try (Connection connect = ServerConnector.DBConnection();
             PreparedStatement ps = connect.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                tradeList.add(matchTradeColumns(rs));
            }
        }
        return tradeList;
    }

    //get by id
    public Trade findByTradeId(int tradeId) throws SQLException {
        try (Connection connect = ServerConnector.DBConnection()) {
            return findByTradeId(connect, tradeId);
        }
    }
    public Trade findByTradeId(Connection connect, int tradeId) throws SQLException {
        String sql = """
            SELECT *
            FROM trade
            WHERE trade_id = ?
        """;
        try (PreparedStatement ps = connect.prepareStatement(sql)) {
            ps.setInt(1, tradeId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return matchTradeColumns(rs);
                }
                return null;
            }
        }
    }

    //insert method
    public void insertTrade(Trade trade) throws SQLException {
        try (Connection connect = ServerConnector.DBConnection()) {
            insertTrade(connect, trade);
        }
    }
    public void insertTrade(Connection connect, Trade trade) throws SQLException {
        String sql = """
            INSERT INTO trade (trade_id, trade_price, trade_units, trade_date, buy_order_id, sell_order_id)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = connect.prepareStatement(sql)) {
            ps.setInt(1, trade.getTradeId());
            ps.setDouble(2, trade.getTradePrice());
            ps.setInt(3, trade.getTradeUnits());
            ps.setDate(4, Date.valueOf(trade.getTradeDate()));
            ps.setInt(5, trade.getBuyOrderId());
            ps.setInt(6, trade.getSellOrderId());

            ps.executeUpdate();
        }
    }

    public List<Trade> listTradesByBuyOrder(int buyOrderId) throws SQLException {
        String sql = """
            SELECT *
            FROM trade
            WHERE buy_order_id = ?
            ORDER BY trade_date DESC, trade_id DESC
        """;

        List<Trade> trades = new ArrayList<>();

        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, buyOrderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    trades.add(matchTradeColumns(rs));
                }
            }
        }
        return trades;
    }

    public List<Trade> listTradesBySellOrder(int sellOrderId) throws SQLException {
        String sql = """
            SELECT trade_id, trade_price, trade_units, trade_date, buy_order_id, sell_order_id
            FROM trade
            WHERE sell_order_id = ?
            ORDER BY trade_date DESC, trade_id DESC
        """;
        List<Trade> trades = new ArrayList<>();
        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sellOrderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    trades.add(matchTradeColumns(rs));
                }
            }
        }
        return trades;
    }

    //Q16 - matched trades enriched with buyer and seller investor names
    public List<TradeWithPartiesDTO> getTradesWithParties() throws SQLException {
        String sql = """
            SELECT t.trade_id,
                   t.trade_date,
                   t.trade_price,
                   t.trade_units,
                   bo.asset_id,
                   bi.investor_id AS buyer_id,
                   bi.investor_name AS buyer_name,
                   si.investor_id  AS seller_id,
                   si.investor_name AS seller_name
            FROM TRADE t
            JOIN TRADE_ORDER bo ON t.buy_order_id  = bo.order_id
            JOIN TRADE_ORDER so ON t.sell_order_id = so.order_id
            JOIN INVESTOR bi ON bo.investor_id = bi.investor_id
            JOIN INVESTOR si ON so.investor_id = si.investor_id
            ORDER BY t.trade_date DESC, t.trade_id DESC
        """;
        List<TradeWithPartiesDTO> result = new ArrayList<>();
        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new TradeWithPartiesDTO(
                        rs.getInt("trade_id"),
                        rs.getDate("trade_date").toLocalDate(),
                        rs.getDouble("trade_price"),
                        rs.getInt("trade_units"),
                        rs.getInt("asset_id"),
                        rs.getInt("buyer_id"),
                        rs.getString("buyer_name"),
                        rs.getInt("seller_id"),
                        rs.getString("seller_name")
                ));
            }
        }
        return result;
    }

}
