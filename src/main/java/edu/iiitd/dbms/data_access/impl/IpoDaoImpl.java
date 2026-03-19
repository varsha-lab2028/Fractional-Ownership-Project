package edu.iiitd.dbms.data_access.impl;

import edu.iiitd.dbms.data_access.IpoDAO;
import edu.iiitd.dbms.domain.IPO;
import edu.iiitd.dbms.dto.InvestorMarketView.MarketViewRow;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class IpoDaoImpl {

    private final IpoDAO ipoDAO;

    public IpoDaoImpl() {
        this.ipoDAO = new IpoDAO();
    }

    //Return every IPO record ordered by ipo_id. */
    public List<IPO> getAll() throws SQLException {
        return ipoDAO.listIPOs();
    }

    //Fetch an IPO by its primary key*/
    public IPO getById(int ipoId) throws SQLException {
        return ipoDAO.findByIpoId(ipoId);
    }

    //Fetch the IPO linked to a specific asset*/
    public IPO getByAssetId(int assetId) throws SQLException {
        return ipoDAO.findByAssetId(assetId);
    }

    //Fetch the IPO linked to a specific asset within an existing connection*/
    public IPO getByAssetId(Connection conn, int assetId) throws SQLException {
        return ipoDAO.findByAssetId(conn, assetId);
    }

    //Return only IPOs whose window is currently open (CURDATE BETWEEN start AND end). */
    public List<IPO> getActive() throws SQLException {
        return ipoDAO.listActiveIpos();
    }

    //Q5 — Market-view rows for IPOs whose window contains a specific reference date. */
    public List<MarketViewRow> getAllMarketRows() throws SQLException {
        return ipoDAO.getAllMarketRows();
    }

    //Filter market-view rows by asset name keyword*/
    public List<MarketViewRow> searchByAssetName(String keyword) throws SQLException {
        return ipoDAO.searchMarketRowsByAssetName(keyword);
    }

    //Filter market-view rows by asset category (case-insensitive equality)*/
    public List<MarketViewRow> getByCategory(String category) throws SQLException {
        return ipoDAO.getMarketRowsByCategory(category);
    }

    /*Q5 — Market-view rows for IPOs whose window contains a specific reference date.*/
    public List<MarketViewRow> getByReferenceDate(java.time.LocalDate referenceDate) throws SQLException {
        return ipoDAO.listIposByReferenceDate(referenceDate);
    }
}
