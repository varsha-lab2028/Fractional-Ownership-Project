package edu.iiitd.dbms.domain;

import java.util.*;
import java.time.*;
import java.math.*;

public class Trade {
    private final int tradeId;
    private final BigDecimal tradePrice;
    private final Integer tradeUnits;
    private final LocalDate tradeDate;
    private final Integer buyOrderId;
    private final Integer sellOrderId;

    //constructor
    public Trade(int tradeId,
                 BigDecimal tradePrice,
                 Integer tradeUnits,
                 LocalDate tradeDate,
                 Integer buyOrderId,
                 Integer sellOrderId) {
        if (tradeId <= 0) {
            throw new IllegalArgumentException("tradeId must be positive");
        }

        if (tradeUnits != null && tradeUnits < 0) {
            throw new IllegalArgumentException("tradeUnits cannot be negative");
        }
        if (tradePrice != null && tradePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("tradePrice cannot be negative");
        }
        if (buyOrderId == null && sellOrderId == null) {
            throw new IllegalArgumentException("Trade must reference at least one order (buy or sell).");
        }
        this.tradeId = tradeId;
        this.tradePrice = tradePrice;
        this.tradeUnits = tradeUnits;
        this.tradeDate = tradeDate;
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
    }
    //getters
    public int getTradeId() { return tradeId; }
    public BigDecimal getTradePrice() { return tradePrice; }
    public Integer getTradeUnits() { return tradeUnits; }
    public LocalDate getTradeDate() { return tradeDate; }
    public Integer getBuyOrderId() { return buyOrderId; }
    public Integer getSellOrderId() { return sellOrderId; }
}
