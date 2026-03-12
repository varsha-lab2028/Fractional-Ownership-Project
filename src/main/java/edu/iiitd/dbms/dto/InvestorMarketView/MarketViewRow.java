package edu.iiitd.dbms.dto.InvestorMarketView;

import java.sql.Date;

public class MarketViewRow {
    private final int ipoId;
    private final int assetId;
    private final String assetName;
    private final String category;
    private final int totalUnits;
    private final double pricePerUnit;
    private final Date ipoStartDate;
    private final Date ipoEndDate;
    private final int ipoLockInPeriod;

    public MarketViewRow(int ipoId, int assetId, String assetName, String category,
                         int totalUnits, double pricePerUnit,
                         Date ipoStartDate, Date ipoEndDate, int ipoLockInPeriod) {
        this.ipoId = ipoId;
        this.assetId = assetId;
        this.assetName = assetName;
        this.category = category;
        this.totalUnits = totalUnits;
        this.pricePerUnit = pricePerUnit;
        this.ipoStartDate = ipoStartDate;
        this.ipoEndDate = ipoEndDate;
        this.ipoLockInPeriod = ipoLockInPeriod;
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
    public String getCategory() {
        return category;
    }
    public int getTotalUnits() {
        return totalUnits;
    }
    public double getPricePerUnit() {
        return pricePerUnit;
    }
    public Date getIpoStartDate() {
        return ipoStartDate;
    }
    public Date getIpoEndDate() {
        return ipoEndDate;
    }
    public int getIpoLockInPeriod() {
        return ipoLockInPeriod;
    }

    @Override
    public String toString() {
        return "MarketViewRow{" +
                "ipoId=" + ipoId +
                ", assetId=" + assetId +
                ", assetName='" + assetName + '\'' +
                ", category='" + category + '\'' +
                ", totalUnits=" + totalUnits +
                ", pricePerUnit=" + pricePerUnit +
                ", ipoStartDate=" + ipoStartDate +
                ", ipoEndDate=" + ipoEndDate +
                ", ipoLockInPeriod='" + ipoLockInPeriod + '\'' +
                '}';
    }
}
