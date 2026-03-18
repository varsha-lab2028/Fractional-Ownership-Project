package edu.iiitd.dbms.dto;

/** Q8 — SUM(units_held) grouped by investor_id, ordered DESC. */
public class InvestorUnitsSummaryDTO {
    private final int investorId;
    private final int totalUnitsHeld;

    public InvestorUnitsSummaryDTO(int investorId, int totalUnitsHeld) {
        this.investorId     = investorId;
        this.totalUnitsHeld = totalUnitsHeld;
    }

    public int getInvestorId()     { return investorId; }
    public int getTotalUnitsHeld() { return totalUnitsHeld; }

    @Override
    public String toString() {
        return "InvestorUnitsSummaryDTO{investorId=" + investorId
                + ", totalUnitsHeld=" + totalUnitsHeld + '}';
    }
}
