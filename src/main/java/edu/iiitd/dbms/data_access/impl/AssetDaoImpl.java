package edu.iiitd.dbms.data_access.impl;

import edu.iiitd.dbms.data_access.AssetDAO;
import edu.iiitd.dbms.domain.Asset;
import edu.iiitd.dbms.dto.InvestorMarketView.AssetValuationRow;
import edu.iiitd.dbms.dto.InvestorMarketView.VerifiedAssetRow;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class AssetDaoImpl {

    private final AssetDAO assetDAO;

    public AssetDaoImpl() {
        this.assetDAO = new AssetDAO();
    }

    //Return every asset row, ordered by asset_id. */
    public List<Asset> getAll() throws SQLException {
        return assetDAO.listAssets();
    }

    //Fetch a single asset by primary key; returns null if not found. */
    public Asset getById(int assetId) throws SQLException {
        return assetDAO.findByAssetId(assetId);
    }

    //Fetch a single asset by primary key within an existing connection (for transactions). */
    public Asset getById(Connection conn, int assetId) throws SQLException {
        return assetDAO.findByAssetId(conn, assetId);
    }

    //Return all verified assets*/
    public List<VerifiedAssetRow> getVerified() throws SQLException {
        return assetDAO.getVerifiedAssets();
    }

    //Return verified assets whose name matches the keyword*/
    public List<VerifiedAssetRow> searchVerifiedByName(String keyword) throws SQLException {
        return assetDAO.searchVerifiedAssetsByName(keyword);
    }

    public List<VerifiedAssetRow> getByCategory(String category) throws SQLException {
        return assetDAO.getVerifiedAssetsByCategory(category);
    }

    //Return all assets joined with their latest valuation amount (LEFT JOIN). */
    public List<AssetValuationRow> getWithValuation() throws SQLException {
        return assetDAO.getAssetsWithValuation();
    }

    //Q2 — All assets LEFT JOINed with the admin who verified them.
    public List<edu.iiitd.dbms.dto.AssetWithAdminDTO> getAssetsWithAdmin() throws SQLException {
        return assetDAO.getAssetsWithAdmin();
    }
}
