package edu.iiitd.dbms.service;

import edu.iiitd.dbms.data_access.InvestorDAO;
import edu.iiitd.dbms.domain.Investor;

import java.sql.SQLException;

public class InvestorService {
    private final InvestorDAO investorDAO;

    public InvestorService() {
        this.investorDAO = new InvestorDAO();
    }

    public Investor getInvestorDetails(int investorId) throws Exception {
        try {
            Investor investor = investorDAO.findByInvestorId(investorId);
            if (investor == null) {
                throw new Exception("Investor profile not found.");
            }
            return investor;
        } catch (SQLException e) {
            throw new Exception("Database error while fetching profile: " + e.getMessage());
        }
    }

    public boolean updateProfile(int investorId, String newName, String newPhone) throws Exception {
        if (newName == null || newName.trim().isEmpty()) {
            throw new Exception("Name cannot be empty.");
        }
        if (newPhone == null || newPhone.trim().isEmpty()) {
            throw new Exception("Phone number cannot be empty.");
        }

        try {
            boolean isUpdated = investorDAO.updateInvestorProfile(investorId, newName.trim(), newPhone.trim());
            if (!isUpdated) {
                throw new Exception("Profile update failed. No changes were made.");
            }
            return true;
        } catch (SQLException e) {
            throw new Exception("Database error while updating profile: " + e.getMessage());
        }
    }
}