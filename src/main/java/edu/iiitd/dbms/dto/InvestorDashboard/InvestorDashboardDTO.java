package edu.iiitd.dbms.dto.InvestorDashboard;

import java.util.*;

public class InvestorDashboardDTO{
    private final int investorId;
    private final String investorName;
    private final double walletBalance;
    private final double totalInvested;
    private final double currentPortfolioValue;
    private final double profitLoss;
    private final double profitLossPercent;
    private final List<ActivityItemDTO> recentActivities;
    private final List<TrendingIPODTO> trendingIpos;
    private final List<ChartPointDTO> portfolioGrowthPoints;
    private final List<AllocationSliceDTO> allocationSlices;

    //constructor
    public InvestorDashboardDTO(
            int investorId,
            String investorName,
            double walletBalance,
            double totalInvested,
            double currentPortfolioValue,
            double profitLoss,
            double profitLossPercent,
            List<ActivityItemDTO> recentActivities,
            List<TrendingIPODTO> trendingIpos,
            List<ChartPointDTO> portfolioGrowthPoints,
            List<AllocationSliceDTO> allocationSlices
    ){
        this.investorId = investorId;
        this.investorName = investorName;
        this.walletBalance = walletBalance;
        this.totalInvested = totalInvested;
        this.currentPortfolioValue = currentPortfolioValue;
        this.profitLoss = profitLoss;
        this.profitLossPercent = profitLossPercent;
        this.recentActivities = recentActivities;
        this.trendingIpos = trendingIpos;
        this.portfolioGrowthPoints = portfolioGrowthPoints;
        this.allocationSlices = allocationSlices;
    }

    //getters
    public int getInvestorId() {
        return investorId;
    }
    public String getInvestorName() {
        return investorName;
    }
    public double getWalletBalance() {
        return walletBalance;
    }
    public double getTotalInvested() {
        return totalInvested;
    }
    public double getCurrentPortfolioValue() {
        return currentPortfolioValue;
    }
    public double getProfitLoss() {
        return profitLoss;
    }
    public double getProfitLossPercent() {
        return profitLossPercent;
    }
    public List<ActivityItemDTO> getRecentActivities() {
        return recentActivities;
    }
    public List<TrendingIPODTO> getTrendingIpos() {
        return trendingIpos;
    }
    public List<ChartPointDTO> getPortfolioGrowthPoints() {
        return portfolioGrowthPoints;
    }
    public List<AllocationSliceDTO> getAllocationSlices() {
        return allocationSlices;
    }
}
