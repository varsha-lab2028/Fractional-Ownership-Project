package edu.iiitd.dbms.domain;

//import java.math.*;
import java.time.*;
import java.util.*;

public class Valuation {
    private final int valuationId;
    private final Integer assetId;
    private final double valuationAmount;
    private final LocalDate valuationDate;

    //constructor
    public Valuation(int valuationId,
                     Integer assetId,
                     double valuationAmount,
                     LocalDate valuationDate) {
        if (valuationId <= 0) {
            throw new IllegalArgumentException("Valuation Id must be positive");
        }
        if (valuationAmount < 0){
            throw new IllegalArgumentException("Valuation Amount cannot be negative");
        }
        this.valuationId = valuationId;
        this.assetId = assetId;
        this.valuationAmount = valuationAmount;
        this.valuationDate = valuationDate;
    }

    //getters
    public int getValuationId() {
        return valuationId;
    }
    public Integer getAssetId() {
        return assetId;
    }
    public double getValuationAmount() {
        return valuationAmount;
    }
    public LocalDate getValuationDate() {
        return valuationDate;
    }
}
