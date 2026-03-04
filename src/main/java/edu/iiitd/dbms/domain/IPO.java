package edu.iiitd.dbms.domain;

import java.util.*;
import java.math.*;
import java.time.*;

public class IPO {
    private final int ipoId;
    private final Integer assetId; //this is nullable
    private final Integer totalUnits;
    private final BigDecimal pricePerUnit;
    private final LocalDate ipoStartDate;
    private final LocalDate ipoEndDate;
    private final Integer lockInPeriod;

    //constructor
    public IPO(int ipoId, Integer assetId, Integer totalUnits, BigDecimal pricePerUnit, LocalDate ipoStartDate, LocalDate ipoEndDate, Integer lockInPeriod){
        if(ipoId < 0){
            throw new IllegalArgumentException("IPO ID cannot be negative");
        }
        if (totalUnits != null && totalUnits < 0)
            throw new IllegalArgumentException("Total units cannot be negative");

        if (pricePerUnit != null && pricePerUnit.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Price per unit value cannot be negative");

        if (ipoStartDate != null && ipoEndDate != null && ipoStartDate.isAfter(ipoEndDate))
            throw new IllegalArgumentException("IPO start date cannot be after end date");

        if (lockInPeriod != null && lockInPeriod < 0)
            throw new IllegalArgumentException("Lock in Period cannot be negative");

        this.ipoId = ipoId;
        this.assetId = assetId;
        this.totalUnits = totalUnits;
        this.pricePerUnit = pricePerUnit;
        this.ipoStartDate = ipoStartDate;
        this.ipoEndDate = ipoEndDate;
        this.lockInPeriod = lockInPeriod;
    }

    //getters
    public int getIpoId() { return ipoId; }
    public Integer getAssetId() { return assetId; }
    public Integer getTotalUnits() { return totalUnits; }
    public BigDecimal getPricePerUnit() { return pricePerUnit; }
    public LocalDate getIpoStartDate() { return ipoStartDate; }
    public LocalDate getIpoEndDate() { return ipoEndDate; }
    public Integer getLockInPeriod() { return lockInPeriod; }
}
