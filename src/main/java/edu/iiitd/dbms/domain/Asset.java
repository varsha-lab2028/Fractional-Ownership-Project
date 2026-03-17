package edu.iiitd.dbms.domain;

public class Asset {
    private final int assetId;
    private final String assetName;
    private final String category;
    private final String description;
    private final String storageLocation;
    private final String verificationReference;
    private final String verificationStatus;
    private final Integer verifiedBy; //this is nullable

    //constructor
    public Asset(int assetId, String assetName, String category, String description, String storageLocation, String verificationReference, String verificationStatus, Integer verifiedBy){
        //certain fields need to exist, otherwise error
        if(assetId < 0){
            throw new IllegalArgumentException("Asset ID cannot be negative");
        }
        if (assetName == null || assetName.isBlank()){
            throw new IllegalArgumentException("Asset name should exist");
        }
        if (category == null || category.isBlank()){
            throw new IllegalArgumentException("Asset should be assigned to a category");
        }
        if(storageLocation == null || storageLocation.isBlank()){
            throw new IllegalArgumentException("Storage location for an asset should exist");
        }
        if(verificationStatus == null || verificationStatus.isBlank()){
            throw new IllegalArgumentException("Verification Status for an asset should exist");
        }
        if(verificationReference == null || verificationReference.isBlank()){
            throw new IllegalArgumentException("Verification reference for an asset should exist");
        }

        this.assetId = assetId;
        this.assetName = assetName;
        this.category = category;
        this.description = description;
        this.storageLocation = storageLocation;
        this.verificationReference = verificationReference;
        this.verificationStatus = verificationStatus;
        this.verifiedBy = verifiedBy;
    }

    //getters
    public int getAssetId(){ return assetId; }
    public String getAssetName(){
        return assetName;
    }
    public String getCategory(){
        return category;
    }
    public String getDescription(){
        return description;
    }
    public String getStorageLocation(){
        return storageLocation;
    }
    public String getVerificationReference(){
        return verificationReference;
    }
    public String getVerificationStatus(){
        return verificationStatus;
    }
    public Integer getVerifiedBy(){
        return verifiedBy;
    }

    @Override
    public String toString() {
        return "Asset{" +
                "assetId=" + assetId +
                ", assetName='" + assetName + '\'' +
                ", category='" + category + '\'' +
                ", description='" + description + '\'' +
                ", storageLocation='" + storageLocation + '\'' +
                ", verificationReference='" + verificationReference + '\'' +
                ", verificationStatus='" + verificationStatus + '\'' +
                ", verifiedBy=" + verifiedBy +
                '}';
    }
}
