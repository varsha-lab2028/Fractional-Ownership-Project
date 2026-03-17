package edu.iiitd.dbms.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public class WalletTransaction {
    private final int transactionId;
    private final int investorId;
    private final double amount;
    private final String transactionType;
    private final String transferCategory;
    private final LocalDateTime transactionDate;

    public WalletTransaction(int transactionId,
                             int investorId,
                             double amount,
                             String transactionType,
                             String transferCategory,
                             LocalDateTime transactionDate) {

        if (transactionId <= 0)
            throw new IllegalArgumentException("transactionId must be positive");

        if (investorId <= 0)
            throw new IllegalArgumentException("investorId must be positive");

        if (transactionType == null || transactionType.isBlank())
            throw new IllegalArgumentException("transactionType cannot be blank");

        if (!transactionType.equals("CREDIT") && !transactionType.equals("DEBIT"))
            throw new IllegalArgumentException("transactionType must be CREDIT or DEBIT");

        if (transactionDate == null)
            throw new IllegalArgumentException("transactionDate cannot be null");

        this.transactionId    = transactionId;
        this.investorId       = investorId;
        this.amount           = amount;
        this.transactionType  = transactionType;
        this.transferCategory = transferCategory;
        this.transactionDate  = transactionDate;
    }

    //getters
    public int getTransactionId()        { return transactionId; }
    public int getInvestorId()           { return investorId; }
    public double getAmount()            { return amount; }
    public String getTransactionType()   { return transactionType; }
    public String getTransferCategory()  { return transferCategory; }
    public LocalDateTime getTransactionDate() { return transactionDate; }

    @Override
    public String toString() {
        return "WalletTransaction{" +
                "transactionId=" + transactionId +
                ", investorId=" + investorId +
                ", amount=" + amount +
                ", transactionType='" + transactionType + '\'' +
                ", transferCategory='" + transferCategory + '\'' +
                ", transactionDate=" + transactionDate +
                '}';
    }
}
