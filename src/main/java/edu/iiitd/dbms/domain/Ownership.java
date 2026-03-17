package edu.iiitd.dbms.domain;

import java.util.*;

public class Ownership {
    private final int investorId;
    private final int assetId;
    private final Integer unitsHeld;

    //constructor
    public Ownership(int investorId, int assetId, Integer unitsHeld){
        if(investorId < 0){
            throw new IllegalArgumentException("Investor ID should not be negative");
        }
        if(assetId < 0){
            throw new IllegalArgumentException("Asset ID should not be negative");
        }
        if(unitsHeld!=null && unitsHeld < 0){
            throw new IllegalArgumentException("Units held cannot be negative");
        }

        this.investorId = investorId;
        this.assetId = assetId;
        this.unitsHeld = unitsHeld;
    }
    //getters
    public int getInvestorId(){return investorId;}
    public int getAssetId(){return assetId;}
    public Integer getUnitsHeld(){return unitsHeld;}

    @Override
    public String toString() {
        return "Ownership{" +
                "investorId=" + investorId +
                ", assetId=" + assetId +
                ", unitsHeld=" + unitsHeld +
                '}';
    }
}
