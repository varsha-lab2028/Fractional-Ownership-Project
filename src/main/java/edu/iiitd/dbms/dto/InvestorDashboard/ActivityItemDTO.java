package edu.iiitd.dbms.dto.InvestorDashboard;

public class ActivityItemDTO {
    private final String actionLabel;
    private final double amount;

    public ActivityItemDTO(String actionLabel, double amount) {
        this.actionLabel = actionLabel;
        this.amount = amount;
    }
    public String getActionLabel() {
        return actionLabel;
    }
    public double getAmount() {
        return amount;
    }
}
