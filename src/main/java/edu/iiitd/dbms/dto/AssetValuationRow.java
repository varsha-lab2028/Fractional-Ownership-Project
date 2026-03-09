package edu.iiitd.dbms.dto;

public class AssetValuationRow {
    private final int assetId;
    private final String assetName;
    private final double valuationAmount;

    public AssetValuationRow(int assetId, String assetName, double valuationAmount) {
        this.assetId = assetId;
        this.assetName = assetName;
        this.valuationAmount = valuationAmount;
    }

    //getters
    public int getAssetId() {
        return assetId;
    }
    public String getAssetName() {
        return assetName;
    }
    public double getValuationAmount() {
        return valuationAmount;
    }

    @Override
    public String toString() {
        return "AssetValuationRow{" +
                "assetId=" + assetId +
                ", assetName='" + assetName + '\'' +
                ", valuationAmount=" + valuationAmount +
                '}';
    }
}
