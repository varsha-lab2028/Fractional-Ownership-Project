package edu.iiitd.dbms.dto;

/**
 * Q19 — Total amount transacted per investor per transaction type.
 */
public class WalletSummaryDTO {
    private final String investorName;
    private final String transactionType;
    private final double totalAmount;

    public WalletSummaryDTO(String investorName, String transactionType, double totalAmount) {
        this.investorName    = investorName;
        this.transactionType = transactionType;
        this.totalAmount     = totalAmount;
    }

    public String getInvestorName()    { return investorName; }
    public String getTransactionType() { return transactionType; }
    public double getTotalAmount()     { return totalAmount; }

    @Override
    public String toString() {
        return "WalletSummaryDTO{investorName='" + investorName + '\''
                + ", transactionType='" + transactionType + '\''
                + ", totalAmount=" + totalAmount + '}';
    }
}
