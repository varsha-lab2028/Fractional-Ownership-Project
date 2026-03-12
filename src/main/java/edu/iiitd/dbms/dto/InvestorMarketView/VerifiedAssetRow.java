package edu.iiitd.dbms.dto.InvestorMarketView;

public class VerifiedAssetRow {
    private final int assetId;
    private final String name;
    private final String category;
    private final String storageLocation;
    private final String verificationStatus;

    public VerifiedAssetRow(int assetId, String name, String category,
                            String storageLocation, String verificationStatus) {
        this.assetId = assetId;
        this.name = name;
        this.category = category;
        this.storageLocation = storageLocation;
        this.verificationStatus = verificationStatus;
    }

    //getters
    public int getAssetId() {
        return assetId;
    }
    public String getName() {
        return name;
    }
    public String getCategory() {
        return category;
    }
    public String getStorageLocation() {
        return storageLocation;
    }
    public String getVerificationStatus() {
        return verificationStatus;
    }

    @Override
    public String toString() {
        return "VerifiedAssetRow{" +
                "assetId=" + assetId +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", storageLocation='" + storageLocation + '\'' +
                ", verificationStatus='" + verificationStatus + '\'' +
                '}';
    }
}
