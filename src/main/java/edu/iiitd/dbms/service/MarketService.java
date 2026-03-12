package edu.iiitd.dbms.service;

import edu.iiitd.dbms.data_access.AssetDAO;
import edu.iiitd.dbms.data_access.IpoDAO;
import edu.iiitd.dbms.dto.InvestorMarketView.MarketViewRow;
import edu.iiitd.dbms.dto.InvestorMarketView.AssetValuationRow;
import edu.iiitd.dbms.dto.InvestorMarketView.VerifiedAssetRow;

import java.sql.*;
import java.util.*;

public class MarketService {
    private final AssetDAO assetDao;
    private final IpoDAO ipoDao;

    public MarketService(){
        this.assetDao = new AssetDAO();
        this.ipoDao = new IpoDAO();
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

    /*public List<MarketViewRow> getActiveMarketRows() {
        try {
            return ipoDao.getActiveMarketRows();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch active market rows", e);
        }
    }*/

    public List<MarketViewRow> getMarketRowsByCategory(String category) {
        try {
            return ipoDao.getMarketRowsByCategory(category);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch market rows by category", e);
        }
    }

    /*public List<MarketViewRow> getMarketRowsByStatus(String status) {
        try {
            return ipoDao.getMarketRowsByStatus(status);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch market rows by status", e);
        }
    }*/

    /*public List<MarketViewRow> getMarketRows(String keyword, String category,
                                             String status, String sortBy, String direction) {
        try {
            return ipoDao.getMarketRows(keyword, category, status, sortBy, direction);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch filtered market rows", e);
        }
    }*/



}
