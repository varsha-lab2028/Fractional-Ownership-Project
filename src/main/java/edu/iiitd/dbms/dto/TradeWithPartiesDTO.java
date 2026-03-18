package edu.iiitd.dbms.dto;

import java.time.LocalDate;

/**
 * Q16 — Executed trade enriched with buyer and seller investor names.
 */
public class TradeWithPartiesDTO {
    private final int        tradeId;
    private final LocalDate  tradeDate;
    private final double     tradePrice;
    private final int        tradeUnits;
    private final int        assetId;
    private final int        buyerId;
    private final String     buyerName;
    private final int        sellerId;
    private final String     sellerName;

    public TradeWithPartiesDTO(int tradeId, LocalDate tradeDate, double tradePrice,
                               int tradeUnits, int assetId,
                               int buyerId, String buyerName,
                               int sellerId, String sellerName) {
        this.tradeId    = tradeId;
        this.tradeDate  = tradeDate;
        this.tradePrice = tradePrice;
        this.tradeUnits = tradeUnits;
        this.assetId    = assetId;
        this.buyerId    = buyerId;
        this.buyerName  = buyerName;
        this.sellerId   = sellerId;
        this.sellerName = sellerName;
    }

    public int       getTradeId()    { return tradeId; }
    public LocalDate getTradeDate()  { return tradeDate; }
    public double    getTradePrice() { return tradePrice; }
    public int       getTradeUnits() { return tradeUnits; }
    public int       getAssetId()    { return assetId; }
    public int       getBuyerId()    { return buyerId; }
    public String    getBuyerName()  { return buyerName; }
    public int       getSellerId()   { return sellerId; }
    public String    getSellerName() { return sellerName; }

    @Override
    public String toString() {
        return "TradeWithPartiesDTO{tradeId=" + tradeId
                + ", tradeDate=" + tradeDate
                + ", tradePrice=" + tradePrice
                + ", tradeUnits=" + tradeUnits
                + ", assetId=" + assetId
                + ", buyerId=" + buyerId
                + ", buyerName='" + buyerName + '\''
                + ", sellerId=" + sellerId
                + ", sellerName='" + sellerName + '\'' + '}';
    }
}
