package edu.iiitd.dbms.data_access;

import edu.iiitd.dbms.config.ServerConnector;
import edu.iiitd.dbms.domain.TradeOrder;

import java.sql.*;
import java.util.*;
import java.sql.Date;

public class TradeOrderDAO {

    private TradeOrder matchTradeOrderColumns(ResultSet rs) throws SQLException {
        return new TradeOrder(
                rs.getInt("order_id"),
                rs.getInt("investor_id"),
                rs.getInt("asset_id"),
                rs.getString("order_type"),
                rs.getDouble("price"),
                rs.getInt("units"),
                rs.getDate("order_date").toLocalDate(),
                rs.getString("status")
        );
    }

    public List<TradeOrder> listOrders() throws SQLException {
        List<TradeOrder> orderList = new ArrayList<>();
        try (Connection c = ServerConnector.DBConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM trade_order ORDER BY order_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) orderList.add(matchTradeOrderColumns(rs));
        }
        return orderList;
    }

    public List<TradeOrder> listOpenOrdersForAsset(int assetId) throws SQLException {
        List<TradeOrder> orders = new ArrayList<>();
        try (Connection c = ServerConnector.DBConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT * FROM trade_order WHERE asset_id = ? AND status = 'OPEN' ORDER BY order_date")) {
            ps.setInt(1, assetId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) orders.add(matchTradeOrderColumns(rs));
            }
        }
        return orders;
    }

    // Convenience overload — opens its own connection
    public TradeOrder findByOrderId(int orderId) throws SQLException {
        try (Connection c = ServerConnector.DBConnection()) {
            return findByOrderId(c, orderId);
        }
    }

    // 7.1 / 7.4 — shared conn + FOR UPDATE lock
    public TradeOrder findByOrderId(Connection conn, int orderId) throws SQLException {
        String sql = "SELECT * FROM trade_order WHERE order_id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? matchTradeOrderColumns(rs) : null;
            }
        }
    }

    public void insertOrder(TradeOrder order) throws SQLException {
        try (Connection c = ServerConnector.DBConnection()) { insertOrder(c, order); }
    }

    public void insertOrder(Connection conn, TradeOrder order) throws SQLException {
        String sql = "INSERT INTO trade_order (order_id, investor_id, asset_id, order_type, price, units, order_date, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, order.getOrderId());
            ps.setInt(2, order.getInvestorId());
            ps.setInt(3, order.getAssetId());
            ps.setString(4, order.getOrderType());
            ps.setDouble(5, order.getPrice());
            ps.setInt(6, order.getUnits());
            ps.setDate(7, Date.valueOf(order.getOrderDate()));
            ps.setString(8, order.getStatus());
            ps.executeUpdate();
        }
    }

    public void updateStatus(int orderId, String newStatus) throws SQLException {
        try (Connection c = ServerConnector.DBConnection()) { updateStatus(c, orderId, newStatus); }
    }

    public void updateStatus(Connection conn, int orderId, String newStatus) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE trade_order SET status = ? WHERE order_id = ?")) {
            ps.setString(1, newStatus);
            ps.setInt(2, orderId);
            ps.executeUpdate();
        }
    }

    public List<TradeOrder> listOrdersByInvestor(int investorId) throws SQLException {
        List<TradeOrder> orders = new ArrayList<>();
        try (Connection c = ServerConnector.DBConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT * FROM trade_order WHERE investor_id = ? ORDER BY order_date DESC, order_id DESC")) {
            ps.setInt(1, investorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) orders.add(matchTradeOrderColumns(rs));
            }
        }
        return orders;
    }
}
