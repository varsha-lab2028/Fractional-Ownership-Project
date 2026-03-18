package edu.iiitd.dbms.service;

import edu.iiitd.dbms.data_access.AssetDAO;
import edu.iiitd.dbms.data_access.IpoDAO;
import edu.iiitd.dbms.data_access.ValuationDAO;
import edu.iiitd.dbms.domain.Valuation;
import edu.iiitd.dbms.dto.InvestorMarketView.MarketViewRow;
import edu.iiitd.dbms.dto.InvestorMarketView.AssetValuationRow;
import edu.iiitd.dbms.dto.InvestorMarketView.VerifiedAssetRow;

import java.sql.*;
import java.util.*;

public class MarketService {
    private final AssetDAO assetDao;
    private final IpoDAO ipoDao;
    private final ValuationDAO valuationDao;

    public MarketService(){
        this.assetDao     = new AssetDAO();
        this.ipoDao       = new IpoDAO();
        this.valuationDao = new ValuationDAO();
    }

    // List all assets (verified + unverified)
    public List<VerifiedAssetRow> listAssets() {
        try {
            return assetDao.getVerifiedAssets();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list assets", e);
        }
    }

    // Show full asset details by ID
    public edu.iiitd.dbms.domain.Asset showAssetDetails(int assetId) {
        try {
            return assetDao.findByAssetId(assetId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch asset details", e);
        }
    }

    // Show the latest valuation for an asset
    public edu.iiitd.dbms.domain.Valuation showLatestValuation(int assetId) {
        try {
            return valuationDao.getLatestForAsset(assetId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch latest valuation", e);
        }
    }

    // Show the full valuation history for an asset
    public List<edu.iiitd.dbms.domain.Valuation> showValuationHistory(int assetId) {
        try {
            return valuationDao.getHistory(assetId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch valuation history", e);
        }
    }

    // Show IPO details for a given asset
    public edu.iiitd.dbms.domain.IPO showIpoDetails(int assetId) {
        try {
            return ipoDao.findByAssetId(assetId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch IPO details", e);
        }
    }

    // Q4 — IPOs currently active (CURDATE BETWEEN start AND end)
    public List<edu.iiitd.dbms.domain.IPO> getActiveIpos() {
        try {
            return ipoDao.listActiveIpos();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch active IPOs", e);
        }
    }

    // Q5 — IPOs active on a specific reference date (parametric version of Q4)
    public List<MarketViewRow> getIposByReferenceDate(java.time.LocalDate referenceDate) {
        try {
            return ipoDao.listIposByReferenceDate(referenceDate);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch IPOs by reference date", e);
        }
    }

    public List<VerifiedAssetRow> getVerifiedAssets() {
        try {
            return assetDao.getVerifiedAssets();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch verified assets", e);
        }
    }

    public List<AssetValuationRow> getAssetsWithValuation() {
        try {
            return assetDao.getAssetsWithValuation();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch asset valuations", e);
        }
    }

    public List<MarketViewRow> getAllMarketRows() {
        try {
            return ipoDao.getAllMarketRows();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch market rows", e);
        }
    }

    public List<MarketViewRow> getMarketRowsByCategory(String category) {
        try {
            return ipoDao.getMarketRowsByCategory(category);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch market rows by category", e);
        }
    }

    // Q15 — latest valuation snapshot for every asset in a single call
    public List<Valuation> getAllLatestValuations() {
        try {
            return valuationDao.getAllLatestValuations();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch all latest valuations", e);
        }
    }
}
