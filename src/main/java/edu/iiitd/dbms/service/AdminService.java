package edu.iiitd.dbms.service;

import edu.iiitd.dbms.data_access.*;
import edu.iiitd.dbms.domain.*;
import edu.iiitd.dbms.dto.*;
import edu.iiitd.dbms.dto.InvestorMarketView.MarketViewRow;

import java.sql.SQLException;
import java.util.List;

public class AdminService {
    private final AdminDAO            adminDAO;
    private final InvestorDAO         investorDAO;
    private final AssetDAO            assetDAO;
    private final IpoDAO              ipoDAO;
    private final TradeDAO            tradeDAO;
    private final TradeOrderDAO       tradeOrderDAO;
    private final OwnershipDAO        ownershipDAO;
    private final OwnershipHistoryDAO historyDAO;
    private final WalletTransactionDAO walletDAO;
    private final ValuationDAO        valuationDAO;

    public AdminService() {
        this.adminDAO    = new AdminDAO();
        this.investorDAO = new InvestorDAO();
        this.assetDAO    = new AssetDAO();
        this.ipoDAO      = new IpoDAO();
        this.tradeDAO    = new TradeDAO();
        this.tradeOrderDAO = new TradeOrderDAO();
        this.ownershipDAO  = new OwnershipDAO();
        this.historyDAO  = new OwnershipHistoryDAO();
        this.walletDAO   = new WalletTransactionDAO();
        this.valuationDAO = new ValuationDAO();
    }

    // ── Existing methods ──────────────────────────────────────────────────────

    public List<Investor> getAllInvestors() throws Exception {
        try { return investorDAO.listInvestors(); }
        catch (SQLException e) { throw new Exception("Failed to fetch investors: " + e.getMessage()); }
    }

    public List<Asset> getAllAssets() throws Exception {
        try { return assetDAO.listAssets(); }
        catch (SQLException e) { throw new Exception("Failed to fetch assets: " + e.getMessage()); }
    }

    public List<IPO> getAllIPOs() throws Exception {
        try { return ipoDAO.listIPOs(); }
        catch (SQLException e) { throw new Exception("Failed to fetch IPOs: " + e.getMessage()); }
    }

    public List<MarketViewRow> getAllMarketRows() throws Exception {
        try { return ipoDAO.getAllMarketRows(); }
        catch (SQLException e) { throw new Exception("Failed to fetch market rows: " + e.getMessage()); }
    }

    public List<Trade> getAllTrades() throws Exception {
        try { return tradeDAO.listTrades(); }
        catch (SQLException e) { throw new Exception("Failed to fetch trades: " + e.getMessage()); }
    }

    public List<TradeOrder> getAllOrders() throws Exception {
        try { return tradeOrderDAO.listOrders(); }
        catch (SQLException e) { throw new Exception("Failed to fetch orders: " + e.getMessage()); }
    }

    public List<OwnershipHistory> getOwnershipHistory(int investorId) throws Exception {
        try { return historyDAO.listByInvestorId(investorId); }
        catch (SQLException e) { throw new Exception("Failed to fetch ownership history: " + e.getMessage()); }
    }

    public List<Admin> getAllAdmins() throws Exception {
        try { return adminDAO.listAdmins(); }
        catch (SQLException e) { throw new Exception("Failed to fetch admins: " + e.getMessage()); }
    }

    // ── Q2: assets with verifying admin name ─────────────────────────────────

    public List<AssetWithAdminDTO> getAssetsWithAdmin() throws Exception {
        try { return assetDAO.getAssetsWithAdmin(); }
        catch (SQLException e) { throw new Exception("Failed to fetch assets with admin: " + e.getMessage()); }
    }

    // ── Q7: total units held per asset ────────────────────────────────────────

    public List<AssetUnitsSummaryDTO> getTotalUnitsByAsset() throws Exception {
        try { return ownershipDAO.getTotalUnitsByAsset(); }
        catch (SQLException e) { throw new Exception("Failed to fetch units by asset: " + e.getMessage()); }
    }

    // ── Q8: total units held per investor, most invested first ───────────────

    public List<InvestorUnitsSummaryDTO> getTotalUnitsByInvestor() throws Exception {
        try { return ownershipDAO.getTotalUnitsByInvestor(); }
        catch (SQLException e) { throw new Exception("Failed to fetch units by investor: " + e.getMessage()); }
    }

    // ── Q10: assets that have never had a trade order ─────────────────────────

    public List<Integer> getAssetIdsWithNoTradeOrders() throws Exception {
        try { return ownershipDAO.getAssetIdsWithNoTradeOrders(); }
        catch (SQLException e) { throw new Exception("Failed to fetch assets with no orders: " + e.getMessage()); }
    }

    // ── Q11: investors who have placed at least one trade order (EXISTS) ──────

    public List<Integer> getInvestorIdsWithTradeOrders() throws Exception {
        try { return ownershipDAO.getInvestorIdsWithTradeOrders(); }
        catch (SQLException e) { throw new Exception("Failed to fetch investors with orders: " + e.getMessage()); }
    }

    // ── Q12: all active investor IDs (UNION of ownership + trade_order) ───────

    public List<Integer> getActiveInvestorIds() throws Exception {
        try { return ownershipDAO.getActiveInvestorIds(); }
        catch (SQLException e) { throw new Exception("Failed to fetch active investor IDs: " + e.getMessage()); }
    }

    // ── Q13: over-allocated assets (held units > IPO total_units) ─────────────

    public List<AssetUnitsSummaryDTO> getOverAllocatedAssets() throws Exception {
        try { return ownershipDAO.getOverAllocatedAssets(); }
        catch (SQLException e) { throw new Exception("Failed to fetch over-allocated assets: " + e.getMessage()); }
    }

    // ── Q14: investors who hold assets but have NEVER placed a trade order ────

    public List<Integer> getInvestorIdsNeverTraded() throws Exception {
        try { return ownershipDAO.getInvestorIdsNeverTraded(); }
        catch (SQLException e) { throw new Exception("Failed to fetch investors who never traded: " + e.getMessage()); }
    }

    // ── Q15: latest valuation for every asset in one query ───────────────────

    public List<Valuation> getAllLatestValuations() throws Exception {
        try { return valuationDAO.getAllLatestValuations(); }
        catch (SQLException e) { throw new Exception("Failed to fetch latest valuations: " + e.getMessage()); }
    }

    // ── Q16: executed trades with buyer and seller names ─────────────────────

    public List<TradeWithPartiesDTO> getTradesWithParties() throws Exception {
        try { return tradeDAO.getTradesWithParties(); }
        catch (SQLException e) { throw new Exception("Failed to fetch trades with parties: " + e.getMessage()); }
    }

    // ── Q17: investors sorted by wallet balance ───────────────────────────────

    public List<Investor> getInvestorsByWalletBalance() throws Exception {
        try { return investorDAO.listInvestorsByWalletBalance(); }
        catch (SQLException e) { throw new Exception("Failed to fetch investors by wallet balance: " + e.getMessage()); }
    }

    // ── Q18: all wallet transactions with investor name ───────────────────────

    public List<WalletTransactionWithNameDTO> getAllTransactionsWithInvestorName() throws Exception {
        try { return walletDAO.getAllTransactionsWithInvestorName(); }
        catch (SQLException e) { throw new Exception("Failed to fetch transactions with names: " + e.getMessage()); }
    }

    // ── Q19: total transacted per investor per type ───────────────────────────

    public List<WalletSummaryDTO> getTransactionSummaryByInvestorAndType() throws Exception {
        try { return walletDAO.getTransactionSummaryByInvestorAndType(); }
        catch (SQLException e) { throw new Exception("Failed to fetch transaction summary: " + e.getMessage()); }
    }

    // ── Q20: investors who have NEVER made a wallet transaction ───────────────

    public List<Investor> getInvestorsWithNoWalletTransactions() throws Exception {
        try { return investorDAO.listInvestorsWithNoWalletTransactions(); }
        catch (SQLException e) { throw new Exception("Failed to fetch investors with no transactions: " + e.getMessage()); }
    }

    // ── Q21: investors with a positive wallet balance ─────────────────────────

    public List<Investor> getInvestorsWithPositiveBalance() throws Exception {
        try { return investorDAO.listInvestorsWithPositiveBalance(); }
        catch (SQLException e) { throw new Exception("Failed to fetch investors with positive balance: " + e.getMessage()); }
    }

    // ── Q6: all ownership rows with investor name and asset name ──────────────

    public List<OwnershipDetailDTO> getAllOwnershipDetails() throws Exception {
        try { return ownershipDAO.getAllOwnershipDetails(); }
        catch (SQLException e) { throw new Exception("Failed to fetch ownership details: " + e.getMessage()); }
    }
}
