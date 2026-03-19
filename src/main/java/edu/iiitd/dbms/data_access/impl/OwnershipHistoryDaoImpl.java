package edu.iiitd.dbms.data_access.impl;

import edu.iiitd.dbms.data_access.OwnershipHistoryDAO;
import edu.iiitd.dbms.domain.OwnershipHistory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class OwnershipHistoryDaoImpl {
    private final OwnershipHistoryDAO historyDAO;

    public OwnershipHistoryDaoImpl() {
        this.historyDAO = new OwnershipHistoryDAO();
    }

    public void insert(Connection conn,
                       int investorId, int assetId,
                       int unitsBefore, int unitsAfter,
                       String changeType,
                       Integer tradeId, Integer ipoId) throws SQLException {
        historyDAO.insert(conn, investorId, assetId, unitsBefore, unitsAfter, changeType, tradeId, ipoId);
    }

    public void insert(int investorId, int assetId,
                       int unitsBefore, int unitsAfter,
                       String changeType,
                       Integer tradeId, Integer ipoId) throws SQLException {
        historyDAO.insert(investorId, assetId, unitsBefore, unitsAfter, changeType, tradeId, ipoId);
    }

    /*Return all history rows for a given investor, newest first*/
    public List<OwnershipHistory> listByInvestor(int investorId) throws SQLException {
        return historyDAO.listByInvestorId(investorId);
    }

    /*Return all history rows for a given asset, newest first*/
    public List<OwnershipHistory> listByAsset(int assetId) throws SQLException {
        return historyDAO.listByAssetId(assetId);
    }
}
