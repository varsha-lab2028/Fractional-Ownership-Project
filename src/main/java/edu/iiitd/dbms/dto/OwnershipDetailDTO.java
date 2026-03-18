package edu.iiitd.dbms.dto;

/**
 * Q6 — Ownership row enriched with investor name and asset name.
 * Used for the admin "all holdings" view.
 */
public class OwnershipDetailDTO {
    private final int    investorId;
    private final String investorName;
    private final String assetName;
    private final int    unitsHeld;

    public OwnershipDetailDTO(int investorId, String investorName,
                              String assetName, int unitsHeld) {
        this.investorId   = investorId;
        this.investorName = investorName;
        this.assetName    = assetName;
        this.unitsHeld    = unitsHeld;
    }

    public int    getInvestorId()   { return investorId; }
    public String getInvestorName() { return investorName; }
    public String getAssetName()    { return assetName; }
    public int    getUnitsHeld()    { return unitsHeld; }

    @Override
    public String toString() {
        return "OwnershipDetailDTO{investorId=" + investorId
                + ", investorName='" + investorName + '\''
                + ", assetName='" + assetName + '\''
                + ", unitsHeld=" + unitsHeld + '}';
    }
}
