package edu.iiitd.dbms.dto.InvestorDashboard;

public class ChartPointDTO {
    private final String label;
    private final double value;

    //constructor
    public ChartPointDTO(String label, double value) {
        this.label = label;
        this.value = value;
    }

    //getters
    public String getLabel() {
        return label;
    }
    public double getValue() {
        return value;
    }
}
