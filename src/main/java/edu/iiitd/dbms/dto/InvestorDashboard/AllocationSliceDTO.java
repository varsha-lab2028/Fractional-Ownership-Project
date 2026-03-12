package edu.iiitd.dbms.dto.InvestorDashboard;

public class AllocationSliceDTO {
    private final String category;
    private final double value;

    //constructor
    public AllocationSliceDTO(String category, double value) {
        this.category = category;
        this.value = value;
    }

    //getters
    public String getCategory() {
        return category;
    }
    public double getValue() {
        return value;
    }
}
