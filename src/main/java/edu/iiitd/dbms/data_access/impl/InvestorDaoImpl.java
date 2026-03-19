package edu.iiitd.dbms.data_access.impl;

import edu.iiitd.dbms.data_access.InvestorDAO;
import edu.iiitd.dbms.domain.Investor;

import java.sql.SQLException;
import java.util.List;

public class InvestorDaoImpl {

    private final InvestorDAO investorDAO;

    public InvestorDaoImpl() {
        this.investorDAO = new InvestorDAO();
    }

    //Return every investor, ordered by investor_id. */
    public List<Investor> getAll() throws SQLException {
        return investorDAO.listInvestors();
    }

    //Fetch a single investor by primary key*/
    public Investor getById(int investorId) throws SQLException {
        return investorDAO.findByInvestorId(investorId);
    }

    //Fetch an investor by their unique email address. */
    public Investor getByEmail(String email) throws SQLException {
        return investorDAO.findByEmail(email);
    }

    public boolean register(String name, String email, String phone) throws SQLException {
        return investorDAO.registerInvestor(name, email, phone);
    }

    //Update mutable profile fields (name and phone). */
    public boolean updateProfile(int investorId, String newName, String newPhone) throws SQLException {
        return investorDAO.updateInvestorProfile(investorId, newName, newPhone);
    }

    //Remove an investor and all cascade-linked rows. */
    public boolean delete(int investorId) throws SQLException {
        return investorDAO.deleteInvestor(investorId);
    }

    //Calculate the total capital invested at IPO prices for this investor. */
    public double getTotalInvested(int investorId) throws SQLException {
        return investorDAO.getTotalInvestedAmount(investorId);
    }

    //Calculate the current market value of the investor's entire portfolio. */
    public double getCurrentPortfolioValue(int investorId) throws SQLException {
        return investorDAO.getCurrentPortfolioValue(investorId);
    }

    //Read the investor's wallet balance directly from the investor row. */
    public double getWalletBalance(int investorId) throws SQLException {
        return investorDAO.getWalletBalance(investorId);
    }

    //Q17 — All investors ordered by wallet balance descending. */
    public List<Investor> listByWalletBalance() throws SQLException {
        return investorDAO.listInvestorsByWalletBalance();
    }

    //Q20 — Investors who have never made any wallet transaction (NOT EXISTS). */
    public List<Investor> listWithNoWalletTransactions() throws SQLException {
        return investorDAO.listInvestorsWithNoWalletTransactions();
    }

    //Q21 — Investors with a positive wallet balance (active wallets). */
    public List<Investor> listWithPositiveBalance() throws SQLException {
        return investorDAO.listInvestorsWithPositiveBalance();
    }
}
