package edu.iiitd.dbms.dto;

public class AssetHoldingDTO {
    private final int assetId;
    private final String assetName;
    private final String category;
    private final int unitsHeld;
    private final double averageBuyPrice; // Simplified to IPO price for now
    private final double currentValue;
    private final double totalInvested;

    public AssetHoldingDTO(int assetId, String assetName, String category, int unitsHeld, 
                           double averageBuyPrice, double currentValue, double totalInvested) {
        this.assetId = assetId;
        this.assetName = assetName;
        this.category = category;
        this.unitsHeld = unitsHeld;
        this.averageBuyPrice = averageBuyPrice;
        this.currentValue = currentValue;
        this.totalInvested = totalInvested;
    }

    public int getAssetId() { return assetId; }
    public String getAssetName() { return assetName; }
    public String getCategory() { return category; }
    public int getUnitsHeld() { return unitsHeld; }
    public double getAverageBuyPrice() { return averageBuyPrice; }
    public double getCurrentValue() { return currentValue; }
    public double getTotalInvested() { return totalInvested; }
    
    // Quick helper to calculate P&L percentage for the UI
    public double getProfitLossPercentage() {
        if (totalInvested == 0) return 0.0;
        return ((currentValue - totalInvested) / totalInvested) * 100.0;
    }
}