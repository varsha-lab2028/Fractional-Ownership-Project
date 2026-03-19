package edu.iiitd.dbms.data_access.impl;

import edu.iiitd.dbms.data_access.ValuationDAO;
import edu.iiitd.dbms.domain.Valuation;

import java.sql.Date;
import java.sql.SQLException;
import java.util.List;

public class ValuationDaoImpl {

    private final ValuationDAO valuationDAO;

    public ValuationDaoImpl() {
        this.valuationDAO = new ValuationDAO();
    }

    /*Fetch the single most-recent valuation for an asset.*/
    public Valuation getLatestByAssetId(int assetId) throws SQLException {
        return valuationDAO.getLatestForAsset(assetId);
    }

    /*Return the full valuation history for an asset, newest first. */
    public List<Valuation> getHistoryByAssetId(int assetId) throws SQLException {
        return valuationDAO.getHistory(assetId);
    }

    /*Return every valuation row in the system, ordered by valuation_id. */
    public List<Valuation> listAll() throws SQLException {
        return valuationDAO.listValuations();
    }

    public void insert(int assetId, double amount, Date valuationDate) throws SQLException {
        valuationDAO.insertValuationToSimulate(assetId, amount, valuationDate);
    }

    /*Q15 — Latest valuation snapshot for every asset in a single query. */
    public List<Valuation> getAllLatestValuations() throws SQLException {
        return valuationDAO.getAllLatestValuations();
    }
}
