package edu.iiitd.dbms.dto.InvestorMarketView;

public class VerifiedAssetRow {
    private final int assetId;
    private final String name;
    private final String category;
    private final String storageLocation;
    private final String verificationStatus;
    private final double pricePerUnit;
    private final int totalUnits;

    // Original constructor (backward compat)
    public VerifiedAssetRow(int assetId, String name, String category,
                            String storageLocation, String verificationStatus) {
        this(assetId, name, category, storageLocation, verificationStatus, 0.0, 0);
    }

    // Full constructor with IPO data
    public VerifiedAssetRow(int assetId, String name, String category,
                            String storageLocation, String verificationStatus,
                            double pricePerUnit, int totalUnits) {
        this.assetId = assetId;
        this.name = name;
        this.category = category;
        this.storageLocation = storageLocation;
        this.verificationStatus = verificationStatus;
        this.pricePerUnit = pricePerUnit;
        this.totalUnits = totalUnits;
    }

    public int    getAssetId()           { return assetId; }
    public String getName()              { return name; }
    public String getCategory()          { return category; }
    public String getStorageLocation()   { return storageLocation; }
    public String getVerificationStatus(){ return verificationStatus; }
    public double getPricePerUnit()      { return pricePerUnit; }
    public int    getTotalUnits()        { return totalUnits; }

    @Override
    public String toString() {
        return "VerifiedAssetRow{assetId=" + assetId + ", name='" + name + "', pricePerUnit=" + pricePerUnit + "}";
    }
}
