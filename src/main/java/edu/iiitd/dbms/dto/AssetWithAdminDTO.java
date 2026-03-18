package edu.iiitd.dbms.dto;

/**
 * Q2 — Asset rows joined with the admin who verified them.
 * admin fields are nullable (LEFT JOIN) for unverified assets.
 */
public class AssetWithAdminDTO {
    private final int     assetId;
    private final String  assetName;
    private final String  verificationStatus;
    private final Integer adminId;       // null if not verified by anyone
    private final String  adminName;     // null if not verified by anyone

    public AssetWithAdminDTO(int assetId, String assetName, String verificationStatus,
                             Integer adminId, String adminName) {
        this.assetId            = assetId;
        this.assetName          = assetName;
        this.verificationStatus = verificationStatus;
        this.adminId            = adminId;
        this.adminName          = adminName;
    }

    public int     getAssetId()            { return assetId; }
    public String  getAssetName()          { return assetName; }
    public String  getVerificationStatus() { return verificationStatus; }
    public Integer getAdminId()            { return adminId; }
    public String  getAdminName()          { return adminName; }

    @Override
    public String toString() {
        return "AssetWithAdminDTO{assetId=" + assetId
                + ", assetName='" + assetName + '\''
                + ", verificationStatus='" + verificationStatus + '\''
                + ", adminId=" + adminId
                + ", adminName='" + adminName + '\'' + '}';
    }
}
