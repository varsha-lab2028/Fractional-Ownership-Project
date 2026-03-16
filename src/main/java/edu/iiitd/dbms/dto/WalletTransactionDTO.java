package edu.iiitd.dbms.dto;

import java.sql.Timestamp;

public class WalletTransactionDTO {
    private final int transactionId;
    private final double amount;
    private final String transactionType;
    private final String transferCategory;
    private final Timestamp transactionDate;

    public WalletTransactionDTO(int transactionId, double amount, String transactionType, 
                                String transferCategory, Timestamp transactionDate) {
        this.transactionId = transactionId;
        this.amount = amount;
        this.transactionType = transactionType;
        this.transferCategory = transferCategory;
        this.transactionDate = transactionDate;
    }

    public int getTransactionId() { return transactionId; }
    public double getAmount() { return amount; }
    public String getTransactionType() { return transactionType; }
    public String getTransferCategory() { return transferCategory; }
    public Timestamp getTransactionDate() { return transactionDate; }
}