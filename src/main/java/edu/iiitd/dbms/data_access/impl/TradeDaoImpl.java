package edu.iiitd.dbms.data_access.impl;

import edu.iiitd.dbms.data_access.TradeDAO;
import edu.iiitd.dbms.domain.Trade;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Concrete implementation of trade (executed match) data-access operations.
 */
public class TradeDaoImpl {

    private final TradeDAO tradeDAO;

    public TradeDaoImpl() {
        this.tradeDAO = new TradeDAO();
    }

    /** Insert a newly executed trade record. */
    public void insert(Trade trade) throws SQLException {
        tradeDAO.insertTrade(trade);
    }

    /** Insert within an existing transaction connection. */
    public void insert(Connection conn, Trade trade) throws SQLException {
        tradeDAO.insertTrade(conn, trade);
    }

    /** Fetch a trade by its primary key; returns null if not found. */
    public Trade getById(int tradeId) throws SQLException {
        return tradeDAO.findByTradeId(tradeId);
    }

    /** Same lookup within an existing transaction connection. */
    public Trade getById(Connection conn, int tradeId) throws SQLException {
        return tradeDAO.findByTradeId(conn, tradeId);
    }

    /** Return every executed trade in the system (admin / reporting view). */
    public List<Trade> listAll() throws SQLException {
        return tradeDAO.listTrades();
    }

    /** Return all trades that matched against a specific buy order. */
    public List<Trade> listByBuyOrder(int buyOrderId) throws SQLException {
        return tradeDAO.listTradesByBuyOrder(buyOrderId);
    }

    /** Return all trades that matched against a specific sell order. */
    public List<Trade> listBySellOrder(int sellOrderId) throws SQLException {
        return tradeDAO.listTradesBySellOrder(sellOrderId);
    }

    /** Q16 — All executed trades enriched with buyer and seller investor names. */
    public List<edu.iiitd.dbms.dto.TradeWithPartiesDTO> getTradesWithParties() throws SQLException {
        return tradeDAO.getTradesWithParties();
    }
}
