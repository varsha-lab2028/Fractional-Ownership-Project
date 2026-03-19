package edu.iiitd.dbms.data_access.impl;

import edu.iiitd.dbms.data_access.TradeOrderDAO;
import edu.iiitd.dbms.domain.TradeOrder;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class TradeOrderDaoImpl {

    private final TradeOrderDAO tradeOrderDAO;

    public TradeOrderDaoImpl() {
        this.tradeOrderDAO = new TradeOrderDAO();
    }

    public void insert(TradeOrder order) throws SQLException {
        tradeOrderDAO.insertOrder(order);
    }

    public void insert(Connection conn, TradeOrder order) throws SQLException {
        tradeOrderDAO.insertOrder(conn, order);
    }

    /*Fetch an order by its primary key*/
    public TradeOrder getById(int orderId) throws SQLException {
        return tradeOrderDAO.findByOrderId(orderId);
    }

    /*Same lookup within an existing transaction connection. */
    public TradeOrder getById(Connection conn, int orderId) throws SQLException {
        return tradeOrderDAO.findByOrderId(conn, orderId);
    }

    /*Return all orders for a given investor, newest first. */
    public List<TradeOrder> listByInvestor(int investorId) throws SQLException {
        return tradeOrderDAO.listOrdersByInvestor(investorId);
    }

    /*Return all OPEN orders for a specific asset*/
    public List<TradeOrder> listOpenByAsset(int assetId) throws SQLException {
        return tradeOrderDAO.listOpenOrdersForAsset(assetId);
    }

    /*Return every order in the system*/
    public List<TradeOrder> listAll() throws SQLException {
        return tradeOrderDAO.listOrders();
    }

    /*Change the status of an order*/
    public void updateStatus(int orderId, String newStatus) throws SQLException {
        tradeOrderDAO.updateStatus(orderId, newStatus);
    }

    /*updateStatus within an existing transaction connection. */
    public void updateStatus(Connection conn, int orderId, String newStatus) throws SQLException {
        tradeOrderDAO.updateStatus(conn, orderId, newStatus);
    }
}
