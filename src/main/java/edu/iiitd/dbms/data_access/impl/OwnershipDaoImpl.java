package edu.iiitd.dbms.data_access.impl;

import edu.iiitd.dbms.data_access.OwnershipDAO;
import edu.iiitd.dbms.domain.Ownership;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class OwnershipDaoImpl {

    private final OwnershipDAO ownershipDAO;

    public OwnershipDaoImpl() {
        this.ownershipDAO = new OwnershipDAO();
    }

    /*Fetch the ownership record for a specific investor + asset pair*/
    public Ownership getByInvestorAndAsset(int investorId, int assetId) throws SQLException {
        return ownershipDAO.findOwnership(investorId, assetId);
    }

    public Ownership getByInvestorAndAsset(Connection conn, int investorId, int assetId) throws SQLException {
        return ownershipDAO.findOwnership(conn, investorId, assetId);
    }

    /*Return all ownership rows for a given investor, ordered by asset_id. */
    public List<Ownership> listByInvestor(int investorId) throws SQLException {
        return ownershipDAO.listByInvestor(investorId);
    }

    public void insert(int investorId, int assetId, int unitsHeld) throws SQLException {
        ownershipDAO.insert(investorId, assetId, unitsHeld);
    }

    /** Insert within an existing transaction connection. */
    public void insert(Connection conn, int investorId, int assetId, int unitsHeld) throws SQLException {
        ownershipDAO.insert(conn, investorId, assetId, unitsHeld);
    }

    /**
     * Upsert the units_held value for an investor-asset pair.
     * Uses INSERT … ON DUPLICATE KEY UPDATE so it handles both insert and update.
     */
    public void updateUnits(Connection conn, int investorId, int assetId, int newUnitsHeld) throws SQLException {
        ownershipDAO.updateUnits(conn, investorId, assetId, newUnitsHeld);
    }

    /*Remove the ownership row if units_held has reached zero — keeps the table
     * clean after a full sell-off.*/
    public void deleteIfZero(int investorId, int assetId) throws SQLException {
        ownershipDAO.deleteIfZero(investorId, assetId);
    }

    /*deleteIfZero within an existing transaction connection. */
    public void deleteIfZero(Connection conn, int investorId, int assetId) throws SQLException {
        ownershipDAO.deleteIfZero(conn, investorId, assetId);
    }

    //sql queries
    /*Q6 — All ownership rows enriched with investor and asset names (admin view). */
    public List<edu.iiitd.dbms.dto.OwnershipDetailDTO> getAllOwnershipDetails() throws SQLException {
        return ownershipDAO.getAllOwnershipDetails();
    }

    /*Q7 — Total units held per asset, grouped. */
    public List<edu.iiitd.dbms.dto.AssetUnitsSummaryDTO> getTotalUnitsByAsset() throws SQLException {
        return ownershipDAO.getTotalUnitsByAsset();
    }

    /*Q8 — Total units held per investor, most invested first. */
    public List<edu.iiitd.dbms.dto.InvestorUnitsSummaryDTO> getTotalUnitsByInvestor() throws SQLException {
        return ownershipDAO.getTotalUnitsByInvestor();
    }

    /*Q10 — Asset IDs with no trade orders */
    public List<Integer> getAssetIdsWithNoTradeOrders() throws SQLException {
        return ownershipDAO.getAssetIdsWithNoTradeOrders();
    }

    /*Q11 — Investor IDs who have placed at least one trade order*/
    public List<Integer> getInvestorIdsWithTradeOrders() throws SQLException {
        return ownershipDAO.getInvestorIdsWithTradeOrders();
    }

    /*Q12 — All investor IDs active in ownership OR trade_order*/
    public List<Integer> getActiveInvestorIds() throws SQLException {
        return ownershipDAO.getActiveInvestorIds();
    }

    /*Q13 — Assets where held units exceed IPO total_units*/
    public List<edu.iiitd.dbms.dto.AssetUnitsSummaryDTO> getOverAllocatedAssets() throws SQLException {
        return ownershipDAO.getOverAllocatedAssets();
    }

    /*Q14 — Investor IDs who hold assets but have never placed a trade order*/
    public List<Integer> getInvestorIdsNeverTraded() throws SQLException {
        return ownershipDAO.getInvestorIdsNeverTraded();
    }
}
