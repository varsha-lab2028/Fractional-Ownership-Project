package edu.iiitd.dbms.domain;

import java.util.*;
import java.time.*;

public class OwnershipHistory {
    private final int historyId;
    private final Integer investorId;
    private final Integer assetId;
    private final Integer unitsBefore;
    private final Integer unitsAfter;
    private final LocalDate changeDate;
    private final String changeType;
    private final Integer tradeId;
    private final Integer ipoId;

    //constructor
    public OwnershipHistory(int historyId, Integer investorId, Integer assetId, Integer unitsBefore, Integer unitsAfter, LocalDate changeDate, String changeType, Integer tradeId, Integer ipoId){
        if (historyId <= 0) {
            throw new IllegalArgumentException("History ID must be positive");
        }
        if (unitsBefore != null && unitsBefore < 0){
            throw new IllegalArgumentException("Units Before cannot be negative");
        }
        if (unitsAfter != null && unitsAfter < 0){
            throw new IllegalArgumentException("Units After cannot be negative");
        }
        if (changeType != null && changeType.isBlank()){
            throw new IllegalArgumentException("Change Type cannot be blank");
        }
        this.historyId = historyId;
        this.investorId = investorId;
        this.assetId = assetId;
        this.unitsBefore = unitsBefore;
        this.unitsAfter = unitsAfter;
        this.changeDate = changeDate;
        this.changeType = changeType;
        this.tradeId = tradeId;
        this.ipoId = ipoId;
    }

    //getters
    public int getHistoryId() { return historyId; }
    public Integer getInvestorId() { return investorId; }
    public Integer getAssetId() { return assetId; }
    public Integer getUnitsBefore() { return unitsBefore; }
    public Integer getUnitsAfter() { return unitsAfter; }
    public LocalDate getChangeDate() { return changeDate; }
    public String getChangeType() { return changeType; }
    public Integer getTradeId() { return tradeId; }
    public Integer getIpoId() { return ipoId; }

    @Override
    public String toString() {
        return "OwnershipHistory{" +
                "historyId=" + historyId +
                ", investorId=" + investorId +
                ", assetId=" + assetId +
                ", unitsBefore=" + unitsBefore +
                ", unitsAfter=" + unitsAfter +
                ", changeDate=" + changeDate +
                ", changeType='" + changeType + '\'' +
                ", tradeId=" + tradeId +
                ", ipoId=" + ipoId +
                '}';
    }

}
