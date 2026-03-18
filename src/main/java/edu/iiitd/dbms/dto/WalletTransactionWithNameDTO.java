package edu.iiitd.dbms.dto;

import java.sql.Timestamp;

/**
 * Q18 — Wallet transaction row enriched with the investor's name.
 */
public class WalletTransactionWithNameDTO {
    private final int       transactionId;
    private final String    investorName;
    private final double    amount;
    private final String    transactionType;
    private final String    transferCategory;
    private final Timestamp transactionDate;

    public WalletTransactionWithNameDTO(int transactionId, String investorName,
                                        double amount, String transactionType,
                                        String transferCategory, Timestamp transactionDate) {
        this.transactionId   = transactionId;
        this.investorName    = investorName;
        this.amount          = amount;
        this.transactionType = transactionType;
        this.transferCategory = transferCategory;
        this.transactionDate = transactionDate;
    }

    public int       getTransactionId()    { return transactionId; }
    public String    getInvestorName()     { return investorName; }
    public double    getAmount()           { return amount; }
    public String    getTransactionType()  { return transactionType; }
    public String    getTransferCategory() { return transferCategory; }
    public Timestamp getTransactionDate()  { return transactionDate; }

    @Override
    public String toString() {
        return "WalletTransactionWithNameDTO{transactionId=" + transactionId
                + ", investorName='" + investorName + '\''
                + ", amount=" + amount
                + ", transactionType='" + transactionType + '\''
                + ", transferCategory='" + transferCategory + '\''
                + ", transactionDate=" + transactionDate + '}';
    }
}
