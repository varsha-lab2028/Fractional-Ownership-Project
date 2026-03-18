package edu.iiitd.dbms.data_access.impl;

import edu.iiitd.dbms.data_access.TradeOrderDAO;
import edu.iiitd.dbms.domain.TradeOrder;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Concrete implementation of trade-order data-access operations.
 */
public class TradeOrderDaoImpl {

    private final TradeOrderDAO tradeOrderDAO;

    public TradeOrderDaoImpl() {
        this.tradeOrderDAO = new TradeOrderDAO();
    }

    /** Insert a new order row. */
    public void insert(TradeOrder order) throws SQLException {
        tradeOrderDAO.insertOrder(order);
    }

    /** Insert within an existing transaction connection. */
    public void insert(Connection conn, TradeOrder order) throws SQLException {
        tradeOrderDAO.insertOrder(conn, order);
    }

    /** Fetch an order by its primary key; returns null if not found. */
    public TradeOrder getById(int orderId) throws SQLException {
        return tradeOrderDAO.findByOrderId(orderId);
    }

    /** Same lookup within an existing transaction connection. */
    public TradeOrder getById(Connection conn, int orderId) throws SQLException {
        return tradeOrderDAO.findByOrderId(conn, orderId);
    }

    /** Return all orders for a given investor, newest first. */
    public List<TradeOrder> listByInvestor(int investorId) throws SQLException {
        return tradeOrderDAO.listOrdersByInvestor(investorId);
    }

    /** Return all OPEN orders for a specific asset (used for order matching). */
    public List<TradeOrder> listOpenByAsset(int assetId) throws SQLException {
        return tradeOrderDAO.listOpenOrdersForAsset(assetId);
    }

    /** Return every order in the system (admin view). */
    public List<TradeOrder> listAll() throws SQLException {
        return tradeOrderDAO.listOrders();
    }

    /** Change the status of an order (e.g. OPEN → FILLED / CANCELLED). */
    public void updateStatus(int orderId, String newStatus) throws SQLException {
        tradeOrderDAO.updateStatus(orderId, newStatus);
    }

    /** updateStatus within an existing transaction connection. */
    public void updateStatus(Connection conn, int orderId, String newStatus) throws SQLException {
        tradeOrderDAO.updateStatus(conn, orderId, newStatus);
    }
}
