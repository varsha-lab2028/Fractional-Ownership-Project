package edu.iiitd.dbms.dto.InvestorDashboard;

public class TrendingIPODTO {
    private final int ipoId;
    private final int assetId;
    private final String assetName;
    private final int totalUnits;
    private final double pricePerUnit;

    public TrendingIPODTO(int ipoId, int assetId, String assetName, int totalUnits, double pricePerUnit) {
        this.ipoId = ipoId;
        this.assetId = assetId;
        this.assetName = assetName;
        this.totalUnits = totalUnits;
        this.pricePerUnit = pricePerUnit;
    }

    //getters
    public int getIpoId() {
        return ipoId;
    }
    public int getAssetId() {
        return assetId;
    }
    public String getAssetName() {
        return assetName;
    }
    public int getTotalUnits() {
        return totalUnits;
    }
    public double getPricePerUnit() {
        return pricePerUnit;
    }
}
