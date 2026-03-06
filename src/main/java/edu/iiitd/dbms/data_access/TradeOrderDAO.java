package edu.iiitd.dbms.data_access;

import edu.iiitd.dbms.config.ServerConnector;
import edu.iiitd.dbms.domain.TradeOrder;

import java.sql.*;
import java.util.*;
import java.sql.Date;

//this is a request which is made by an investor
//this DAO will enable to place order + update the status
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
        String sqlQuery = """
            SELECT *
            FROM trade_order
            ORDER BY order_id
        """;
        List<TradeOrder> orderList = new ArrayList<>();
        try (Connection connect = ServerConnector.DBConnection();
             PreparedStatement ps = connect.prepareStatement(sqlQuery);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                orderList.add(matchTradeOrderColumns(rs));
            }
        }
        return orderList;
    }

    public List<TradeOrder> listOpenOrdersForAsset(int assetId) throws SQLException {
        String sqlQuery = """
            SELECT *
            FROM trade_order
            WHERE asset_id = ? AND status = 'OPEN'
            ORDER BY order_date
        """;
        List<TradeOrder> orders = new ArrayList<>();
        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement ps = conn.prepareStatement(sqlQuery)) {
            ps.setInt(1, assetId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    orders.add(matchTradeOrderColumns(rs));
                }
            }
        }
        return orders;
    }

    public TradeOrder findByOrderId(int orderId) throws SQLException {
        try (Connection connect = ServerConnector.DBConnection()) {
            return findByOrderId(connect, orderId);
        }
    }
    public TradeOrder findByOrderId(Connection connect, int orderId) throws SQLException {
        String sqlQuery = """
            SELECT *
            FROM trade_order
            WHERE order_id = ?
        """;
        try (PreparedStatement ps = connect.prepareStatement(sqlQuery)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return matchTradeOrderColumns(rs);
                }
                return null;
            }
        }
    }

    public void insertOrder(TradeOrder order) throws SQLException {
        try (Connection connect = ServerConnector.DBConnection()) {
            insertOrder(connect, order);
        }
    }
    public void insertOrder(Connection connect, TradeOrder order) throws SQLException {
        String sqlQuery = """
            INSERT INTO trade_order (order_id, investor_id, asset_id, order_type, price, units, order_date, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = connect.prepareStatement(sqlQuery)) {
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
        try (Connection connect = ServerConnector.DBConnection()) {
            updateStatus(connect, orderId, newStatus);
        }
    }
    public void updateStatus(Connection connect, int orderId, String newStatus) throws SQLException {
        String sql = "UPDATE trade_order SET status = ? WHERE order_id = ?";
        try (PreparedStatement ps = connect.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, orderId);
            ps.executeUpdate();
        }
    }

    public List<TradeOrder> listOrdersByInvestor(int investorId) throws SQLException {
        String sql = """
            SELECT *
            FROM trade_order
            WHERE investor_id = ?
            ORDER BY order_date DESC, order_id DESC
        """;
        List<TradeOrder> orders = new ArrayList<>();
        try (Connection conn = ServerConnector.DBConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, investorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    orders.add(matchTradeOrderColumns(rs));
                }
            }
        }
        return orders;
    }
}
