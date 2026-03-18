package edu.iiitd.dbms.dto;

/** Q7 — SUM(units_held) grouped by asset_id. */
public class AssetUnitsSummaryDTO {
    private final int assetId;
    private final int totalUnitsHeld;

    public AssetUnitsSummaryDTO(int assetId, int totalUnitsHeld) {
        this.assetId        = assetId;
        this.totalUnitsHeld = totalUnitsHeld;
    }

    public int getAssetId()        { return assetId; }
    public int getTotalUnitsHeld() { return totalUnitsHeld; }

    @Override
    public String toString() {
        return "AssetUnitsSummaryDTO{assetId=" + assetId
                + ", totalUnitsHeld=" + totalUnitsHeld + '}';
    }
}
