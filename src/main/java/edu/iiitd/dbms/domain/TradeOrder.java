package edu.iiitd.dbms.domain;

import java.time.*;
import java.util.*;

public class TradeOrder {
    private final int orderId;
    private final Integer investorId;
    private final Integer assetId;
    private final String orderType;
    private final Double price;
    private final Integer units;
    private final LocalDate orderDate;
    private final String status;

    //constructor
    public TradeOrder(int orderId,
                      Integer investorId,
                      Integer assetId,
                      String orderType,
                      Double price,
                      Integer units,
                      LocalDate orderDate,
                      String status) {
        if (orderId <= 0) throw new IllegalArgumentException("Order ID cannot be negative");

        if (units != null && units < 0)
            throw new IllegalArgumentException("Units cannot be negative");

        if (price < 0)
            throw new IllegalArgumentException("Price cannot be negative");

        if (orderType != null && orderType.isBlank())
            throw new IllegalArgumentException("Order Type cannot be blank");

        if (status != null && status.isBlank())
            throw new IllegalArgumentException("Status cannot be blank");

        this.orderId = orderId;
        this.investorId = investorId;
        this.assetId = assetId;
        this.orderType = orderType;
        this.price = price;
        this.units = units;
        this.orderDate = orderDate;
        this.status = status;
    }
    //getters
    public int getOrderId() { return orderId; }
    public Integer getInvestorId() { return investorId; }
    public Integer getAssetId() { return assetId; }
    public String getOrderType() { return orderType; }
    public Double getPrice() { return price; }
    public Integer getUnits() { return units; }
    public LocalDate getOrderDate() { return orderDate; }
    public String getStatus() { return status; }

    @Override
    public String toString() {
        return "TradeOrder{" +
                "orderId=" + orderId +
                ", investorId=" + investorId +
                ", assetId=" + assetId +
                ", orderType='" + orderType + '\'' +
                ", price=" + price +
                ", units=" + units +
                ", orderDate=" + orderDate +
                ", status='" + status + '\'' +
                '}';
    }
}
