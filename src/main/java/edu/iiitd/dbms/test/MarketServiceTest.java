package edu.iiitd.dbms.test;

import edu.iiitd.dbms.dto.AssetValuationRow;
import edu.iiitd.dbms.dto.MarketViewRow;
import edu.iiitd.dbms.dto.VerifiedAssetRow;
import edu.iiitd.dbms.service.MarketService;

import java.util.List;

public class MarketServiceTest {
    public static void main(String[] args) {
        MarketService marketService = new MarketService();

        System.out.println("========== VERIFIED ASSETS (Q1) ==========");
        List<VerifiedAssetRow> verifiedAssets = marketService.getVerifiedAssets();
        for (VerifiedAssetRow row : verifiedAssets) {
            System.out.println(row);
        }

        System.out.println("\n========== ASSETS WITH VALUATION (Q8) ==========");
        List<AssetValuationRow> valuations = marketService.getAssetsWithValuation();
        for (AssetValuationRow row : valuations) {
            System.out.println(row);
        }

        System.out.println("\n========== ALL MARKET ROWS (Q3) ==========");
        List<MarketViewRow> marketRows = marketService.getAllMarketRows();
        for (MarketViewRow row : marketRows) {
            System.out.println(row);
        }

        /*System.out.println("\n========== ACTIVE MARKET ROWS (Q4) ==========");
        List<MarketViewRow> activeRows = marketService.getActiveMarketRows();
        for (MarketViewRow row : activeRows) {
            System.out.println(row);
        }*/

        /*System.out.println("\n========== SEARCH BY ASSET NAME ==========");
        List<MarketViewRow> searchedRows = marketService.searchMarketRowsByAssetName("wine");
        for (MarketViewRow row : searchedRows) {
            System.out.println(row);
        }*/

        System.out.println("\n========== FILTER BY CATEGORY ==========");
        List<MarketViewRow> categoryRows = marketService.getMarketRowsByCategory("Luxury");
        for (MarketViewRow row : categoryRows) {
            System.out.println(row);
        }

        /*System.out.println("\n========== FILTER BY STATUS ==========");
        List<MarketViewRow> statusRows = marketService.getMarketRowsByStatus("Open");
        for (MarketViewRow row : statusRows) {
            System.out.println(row);
        }*/

        /*System.out.println("\n========== COMBINED QUERY ==========");
        List<MarketViewRow> combinedRows =
                marketService.getMarketRows("wine", "Luxury", "Open", "price", "DESC");
        for (MarketViewRow row : combinedRows) {
            System.out.println(row);
        }*/
    }
}
