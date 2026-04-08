package edu.iiitd.dbms.ui.admin;

import edu.iiitd.dbms.auth.LoginManager;
import edu.iiitd.dbms.data_access.*;
import edu.iiitd.dbms.domain.*;
import edu.iiitd.dbms.dto.InvestorMarketView.MarketViewRow;
import edu.iiitd.dbms.service.LiveMarketDataManager;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

public class AdminDashUI extends JFrame {

    private final Color bgAbsoluteDark = new Color(14, 14, 14);
    private final Color cardGlass      = new Color(26, 26, 26);
    private final Color borderSubtle   = new Color(45, 45, 45);
    private final Color textPrimary    = new Color(250, 250, 250);
    private final Color textMuted      = new Color(150, 150, 150);
    private final Color pastelGreen    = new Color(119, 221, 119);
    private final Color pastelRed      = new Color(255, 105, 97);
    private final Color pastelBlue     = new Color(174, 198, 207);
    private final Color pastelLilac    = new Color(200, 162, 200);
    private final Color pastelPeach    = new Color(255, 179, 71);

    private Point dragPoint;
    private GrainySidebar sidebar;
    // isMarketOpen is now DERIVED from the singleton — the field is kept for
    // backward compatibility with AdminControlCenter which extends this class.
    private boolean isMarketOpen = false;

    private JLabel totalInvestorsVal;
    private JLabel totalAssetsVal;
    private JLabel totalTradesVal;
    private JPanel ipoListPanel;
    private JPanel chartBox1;
    private JPanel chartBox2;
    private JLabel activeInvLabel;
    private JLabel openOrdersLabel;
    // Live-market UI handles (needed for sync)
    private JLabel  mkStatus;
    private JButton marketToggleBtn;

    private final InvestorDAO   investorDAO   = new InvestorDAO();
    private final AssetDAO      assetDAO      = new AssetDAO();
    private final IpoDAO        ipoDAO        = new IpoDAO();
    private final TradeDAO      tradeDAO      = new TradeDAO();
    private final OwnershipDAO  ownershipDAO  = new OwnershipDAO();
    private final TradeOrderDAO tradeOrderDAO = new TradeOrderDAO();

    /** The singleton live-market engine — shared with investor views. */
    private final LiveMarketDataManager lmd = LiveMarketDataManager.getInstance();

    // ── Display name helper ───────────────────────────────────────────────────
    private String getAdminDisplayName() {
        if (LoginManager.getCurrentUser() != null) {
            String name = LoginManager.getCurrentUser().getName();
            if (name != null && !name.isBlank()) {
                String[] parts = name.trim().split(" ");
                return parts[0].toUpperCase()
                    + (parts.length > 1 ? " " + Character.toUpperCase(parts[parts.length-1].charAt(0)) + "." : "");
            }
        }
        return "ADMIN";
    }

    // ────────────────────────────────────────────────────────────────────────
    public AdminDashUI() {
        setTitle("Fractional. - Admin Control Center");
        setSize(1350, 850);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setUndecorated(true);
        setLayout(new BorderLayout());
        getContentPane().setBackground(bgAbsoluteDark);

        // ── Drag bar ──────────────────────────────────────────────────────
        JPanel dragBar = new JPanel(new BorderLayout());
        dragBar.setBackground(bgAbsoluteDark);
        dragBar.setPreferredSize(new Dimension(1350, 30));
        JLabel closeBtn = new JLabel("  ✕  ");
        closeBtn.setForeground(textMuted); closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeBtn.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { System.exit(0); }
            public void mouseEntered(MouseEvent e) { closeBtn.setForeground(textPrimary); }
            public void mouseExited(MouseEvent e)  { closeBtn.setForeground(textMuted); }
        });
        dragBar.add(closeBtn, BorderLayout.EAST);
        dragBar.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { dragPoint = e.getPoint(); }
        });
        dragBar.addMouseMotionListener(new MouseAdapter() {
            public void mouseDragged(MouseEvent e) {
                setLocation(getLocation().x + e.getX() - dragPoint.x,
                            getLocation().y + e.getY() - dragPoint.y);
            }
        });

        // ── Top bar ───────────────────────────────────────────────────────
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(bgAbsoluteDark);
        topBar.setBorder(new EmptyBorder(10, 15, 10, 25));
        JButton toggleBtn = new JButton("☰");
        toggleBtn.setFont(new Font("SansSerif", Font.BOLD, 20));
        toggleBtn.setForeground(textPrimary);
        toggleBtn.setContentAreaFilled(false); toggleBtn.setBorderPainted(false);
        toggleBtn.setFocusPainted(false); toggleBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        toggleBtn.addActionListener(e -> sidebar.toggleSidebar());
        JPanel searchContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        searchContainer.setOpaque(false);
        searchContainer.add(toggleBtn);
        AestheticSearchBar searchField = new AestheticSearchBar("🔍 Search assets, users, or system logs...");
        searchField.setPreferredSize(new Dimension(350, 40));
        searchContainer.add(searchField);
        JPanel profilePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5));
        profilePanel.setOpaque(false);
        JLabel roleLabel = new JLabel("PLATFORM ADMIN");
        roleLabel.setForeground(pastelBlue); roleLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
        JLabel profileLabel = new JLabel(getAdminDisplayName());
        profileLabel.setForeground(textPrimary); profileLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        profilePanel.add(roleLabel); profilePanel.add(profileLabel);
        JButton adminLogoutBtn = new JButton("Sign Out");
        adminLogoutBtn.setForeground(pastelRed); adminLogoutBtn.setContentAreaFilled(false);
        adminLogoutBtn.setBorderPainted(false); adminLogoutBtn.setFocusPainted(false);
        adminLogoutBtn.setFont(new Font("SansSerif", Font.BOLD, 11));
        adminLogoutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        adminLogoutBtn.addActionListener(e -> {
            int c = JOptionPane.showConfirmDialog(AdminDashUI.this, "Sign out?", "Sign Out", JOptionPane.YES_NO_OPTION);
            if (c == JOptionPane.YES_OPTION) {
                LoginManager.logout();
                dispose();
                new edu.iiitd.dbms.ui.login.logintesterUI().setVisible(true);
            }
        });
        profilePanel.add(adminLogoutBtn);
        topBar.add(searchContainer, BorderLayout.WEST);
        topBar.add(profilePanel, BorderLayout.EAST);

        sidebar = new GrainySidebar();

        // ── Stats row ─────────────────────────────────────────────────────
        JPanel statsRow = new JPanel(new GridLayout(1, 3, 20, 0));
        statsRow.setOpaque(false);
        statsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        JPanel invCard   = createStatCard("TOTAL INVESTORS", "—", pastelBlue);
        totalInvestorsVal = getCardLabel(invCard);
        JPanel assetCard = createStatCard("VERIFIED ASSETS", "—", pastelGreen);
        totalAssetsVal   = getCardLabel(assetCard);
        JPanel tradeCard = createStatCard("TOTAL TRADES", "—", textPrimary);
        totalTradesVal   = getCardLabel(tradeCard);
        statsRow.add(invCard); statsRow.add(assetCard); statsRow.add(tradeCard);

        // ── Right panel ───────────────────────────────────────────────────
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setOpaque(false);
        rightPanel.setPreferredSize(new Dimension(340, 0));
        rightPanel.setBorder(new EmptyBorder(0, 10, 30, 25));

        // Market control card
        JPanel marketCard = createRoundedPanel();
        marketCard.setLayout(new BoxLayout(marketCard, BoxLayout.Y_AXIS));
        marketCard.setBorder(new EmptyBorder(30, 25, 30, 25));

        JLabel mkTitle = new JLabel("SECONDARY MARKET STATUS");
        mkTitle.setForeground(textMuted); mkTitle.setFont(new Font("SansSerif", Font.BOLD, 11));
        mkTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        mkStatus = new JLabel("—");
        mkStatus.setFont(new Font("SansSerif", Font.BOLD, 26));
        mkStatus.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Live tick counter label
        JLabel tickLabel = new JLabel("Tick interval: 2 s");
        tickLabel.setForeground(textMuted); tickLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        tickLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        marketToggleBtn = new JButton("—");
        marketToggleBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        marketToggleBtn.setFocusPainted(false); marketToggleBtn.setOpaque(true);
        marketToggleBtn.setBorderPainted(false);
        marketToggleBtn.setMaximumSize(new Dimension(220, 38));
        marketToggleBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        marketToggleBtn.addActionListener(e -> toggleMarket());

        marketCard.add(mkTitle);
        marketCard.add(Box.createRigidArea(new Dimension(0, 15)));
        marketCard.add(mkStatus);
        marketCard.add(Box.createRigidArea(new Dimension(0, 6)));
        marketCard.add(tickLabel);
        marketCard.add(Box.createRigidArea(new Dimension(0, 14)));
        marketCard.add(marketToggleBtn);

        // Analytics card
        JPanel analyticsCard = createRoundedPanel();
        analyticsCard.setLayout(new BoxLayout(analyticsCard, BoxLayout.Y_AXIS));
        analyticsCard.setBorder(new EmptyBorder(25, 25, 25, 25));
        JLabel alTitle = new JLabel("ANALYTICS SNAPSHOT");
        alTitle.setForeground(textMuted); alTitle.setFont(new Font("SansSerif", Font.BOLD, 11));
        analyticsCard.add(alTitle);
        analyticsCard.add(Box.createRigidArea(new Dimension(0, 15)));
        activeInvLabel  = new JLabel("Active Investors: loading...");
        activeInvLabel.setForeground(textPrimary); activeInvLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        openOrdersLabel = new JLabel("Open Orders: loading...");
        openOrdersLabel.setForeground(textPrimary); openOrdersLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        analyticsCard.add(activeInvLabel);
        analyticsCard.add(Box.createRigidArea(new Dimension(0, 8)));
        analyticsCard.add(openOrdersLabel);

        rightPanel.add(marketCard);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        rightPanel.add(analyticsCard);

        // ── Center panel ──────────────────────────────────────────────────
        JPanel centerPanel = new JPanel(new BorderLayout(0, 20));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(0, 0, 30, 15));

        JPanel chartsGrid = new JPanel(new GridLayout(1, 2, 20, 0));
        chartsGrid.setOpaque(false);
        chartsGrid.setPreferredSize(new Dimension(0, 285));
        chartBox1 = createRoundedPanel(); chartBox1.setLayout(new BorderLayout()); chartBox1.setBorder(new EmptyBorder(15,15,15,15));
        chartBox2 = createRoundedPanel(); chartBox2.setLayout(new BorderLayout()); chartBox2.setBorder(new EmptyBorder(15,15,15,15));
        JLabel l1 = new JLabel("Loading...", SwingConstants.CENTER); l1.setForeground(textMuted); chartBox1.add(l1);
        JLabel l2 = new JLabel("Loading...", SwingConstants.CENTER); l2.setForeground(textMuted); chartBox2.add(l2);
        chartsGrid.add(chartBox1); chartsGrid.add(chartBox2);

        // IPO management panel
        JPanel ipoContainer = createRoundedPanel();
        ipoContainer.setLayout(new BorderLayout());
        ipoContainer.setBorder(new EmptyBorder(20, 20, 20, 20));
        JPanel ipoHeader = new JPanel(new BorderLayout());
        ipoHeader.setOpaque(false);
        JLabel ipoTitle = new JLabel("IPO & ASSET MANAGEMENT");
        ipoTitle.setForeground(textPrimary); ipoTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        JLabel ipoSub = new JLabel("Click 'Verify & Track' to add a new IPO to the live market feed");
        ipoSub.setForeground(textMuted); ipoSub.setFont(new Font("SansSerif", Font.PLAIN, 11));
        JPanel ipoTitleBlock = new JPanel(new GridLayout(2,1)); ipoTitleBlock.setOpaque(false);
        ipoTitleBlock.add(ipoTitle); ipoTitleBlock.add(ipoSub);
        ipoHeader.add(ipoTitleBlock, BorderLayout.WEST);
        ipoContainer.add(ipoHeader, BorderLayout.NORTH);
        ipoListPanel = new JPanel();
        ipoListPanel.setLayout(new BoxLayout(ipoListPanel, BoxLayout.Y_AXIS));
        ipoListPanel.setOpaque(false);
        JScrollPane ipoScroll = new JScrollPane(ipoListPanel);
        ipoScroll.setBorder(null); ipoScroll.setOpaque(false);
        ipoScroll.getViewport().setOpaque(false);
        ipoScroll.getViewport().setBackground(cardGlass);
        ipoScroll.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() { thumbColor = borderSubtle; trackColor = cardGlass; }
            @Override protected JButton createDecreaseButton(int o) { JButton b = new JButton(); b.setPreferredSize(new Dimension(0,0)); return b; }
            @Override protected JButton createIncreaseButton(int o) { JButton b = new JButton(); b.setPreferredSize(new Dimension(0,0)); return b; }
        });
        ipoContainer.add(ipoScroll, BorderLayout.CENTER);

        JPanel topCenter = new JPanel(new BorderLayout(0, 15));
        topCenter.setOpaque(false);
        topCenter.add(statsRow, BorderLayout.NORTH);
        topCenter.add(chartsGrid, BorderLayout.CENTER);
        centerPanel.add(topCenter, BorderLayout.NORTH);
        centerPanel.add(ipoContainer, BorderLayout.CENTER);

        // ── Assemble ──────────────────────────────────────────────────────
        add(dragBar, BorderLayout.NORTH);
        add(sidebar, BorderLayout.WEST);
        JPanel mainContent = new JPanel(new BorderLayout());
        mainContent.setOpaque(false);
        mainContent.add(topBar, BorderLayout.NORTH);
        mainContent.add(centerPanel, BorderLayout.CENTER);
        mainContent.add(rightPanel, BorderLayout.EAST);
        add(mainContent, BorderLayout.CENTER);

        // ── Sync market button with singleton state ────────────────────────
        // (Handles re-opening this window while market is already open)
        syncMarketButton();

        loadDataAsync();
    }

    // ── Market toggle ─────────────────────────────────────────────────────────
    private void toggleMarket() {
        if (lmd.isMarketOpen()) {
            lmd.closeMarket();
        } else {
            lmd.openMarket();
        }
        syncMarketButton();
    }

    private void syncMarketButton() {
        isMarketOpen = lmd.isMarketOpen();
        if (isMarketOpen) {
            mkStatus.setText("LIVE & TRADING");
            mkStatus.setForeground(pastelGreen);
            marketToggleBtn.setText("CLOSE MARKET");
            marketToggleBtn.setBackground(pastelRed);
            marketToggleBtn.setForeground(bgAbsoluteDark);
        } else {
            mkStatus.setText("MARKET CLOSED");
            mkStatus.setForeground(pastelRed);
            marketToggleBtn.setText("OPEN MARKET");
            marketToggleBtn.setBackground(pastelGreen);
            marketToggleBtn.setForeground(bgAbsoluteDark);
        }
    }

    /**
     * Called when an admin verifies a new IPO — registers it with the live market
     * feed so it immediately starts being tracked when the market is open.
     */
    public void addAssetToLiveMarket(int assetId, String name, double ipoPrice) {
        lmd.addAsset(assetId, name, ipoPrice);
        System.out.printf("[Admin] Asset %d (%s @ $%.2f) added to live market feed.%n",
            assetId, name, ipoPrice);
    }

    // ── Async data loader ─────────────────────────────────────────────────────
    private void loadDataAsync() {
        new SwingWorker<Void, Void>() {
            int investors = 0, assets = 0, trades = 0, activeInv = 0, openOrders = 0;
            List<MarketViewRow> marketRows;
            final Map<String, Integer> categoryCount   = new LinkedHashMap<>();
            final Map<String, Double>  tradesByMonth   = new LinkedHashMap<>();

            @Override protected Void doInBackground() {
                try {
                    investors  = investorDAO.listInvestors().size();
                    assets     = assetDAO.getVerifiedAssets().size();
                    List<Trade> allTrades = tradeDAO.listTrades();
                    trades     = allTrades.size();
                    activeInv  = ownershipDAO.getActiveInvestorIds().size();
                    openOrders = (int) tradeOrderDAO.listOrders().stream()
                        .filter(o -> "OPEN".equalsIgnoreCase(o.getStatus())).count();
                    marketRows = ipoDAO.getAllMarketRows();

                    for (Asset a : assetDAO.listAssets())
                        categoryCount.merge(a.getCategory(), 1, Integer::sum);

                    for (Trade t : allTrades) {
                        if (t.getTradeDate() != null) {
                            String month = String.format("%d/%d",
                                t.getTradeDate().getMonthValue(), t.getTradeDate().getYear() % 100);
                            tradesByMonth.merge(month,
                                (double)(t.getTradeUnits() != null ? t.getTradeUnits() : 0), Double::sum);
                        }
                    }
                } catch (Exception e) { e.printStackTrace(); }
                return null;
            }

            @Override protected void done() {
                totalInvestorsVal.setText(String.valueOf(investors));
                totalAssetsVal.setText(String.valueOf(assets));
                totalTradesVal.setText(String.valueOf(trades));
                activeInvLabel.setText("Active Investors: " + activeInv);
                openOrdersLabel.setText("Open Orders: " + openOrders);

                // Trade volume bar chart
                DefaultCategoryDataset tradeDs = new DefaultCategoryDataset();
                if (tradesByMonth.isEmpty()) {
                    tradeDs.addValue(10, "Vol", "Jan"); tradeDs.addValue(15, "Vol", "Mar");
                    tradeDs.addValue(8,  "Vol", "Jun"); tradeDs.addValue(20, "Vol", "Oct");
                } else {
                    for (Map.Entry<String,Double> e : tradesByMonth.entrySet())
                        tradeDs.addValue(e.getValue(), "Units", e.getKey());
                }
                JFreeChart barChart = ChartFactory.createBarChart(
                    null, null, null, tradeDs, PlotOrientation.VERTICAL, false, false, false);
                barChart.setBackgroundPaint(cardGlass);
                CategoryPlot bp = barChart.getCategoryPlot();
                bp.setBackgroundPaint(cardGlass); bp.setOutlineVisible(false);
                bp.setRangeGridlinePaint(borderSubtle); bp.setDomainGridlinesVisible(false);
                bp.getDomainAxis().setTickLabelPaint(textMuted); bp.getDomainAxis().setAxisLinePaint(borderSubtle);
                bp.getRangeAxis().setTickLabelPaint(textMuted); bp.getRangeAxis().setAxisLinePaint(borderSubtle);
                bp.getRenderer().setSeriesPaint(0, pastelBlue);
                ChartPanel cp1 = new ChartPanel(barChart); cp1.setOpaque(false); cp1.setBackground(cardGlass);
                chartBox1.removeAll();
                JLabel t1 = new JLabel("TRADE VOLUME"); t1.setForeground(textPrimary); t1.setFont(new Font("SansSerif", Font.BOLD, 12));
                chartBox1.add(t1, BorderLayout.NORTH); chartBox1.add(cp1, BorderLayout.CENTER);
                chartBox1.revalidate(); chartBox1.repaint();

                // Asset category pie chart
                DefaultPieDataset pieDs = new DefaultPieDataset();
                if (categoryCount.isEmpty()) {
                    pieDs.setValue("Art", 5); pieDs.setValue("Watch", 5); pieDs.setValue("Wine", 5);
                } else {
                    for (Map.Entry<String,Integer> e : categoryCount.entrySet())
                        pieDs.setValue(e.getKey(), e.getValue());
                }
                JFreeChart pieChart = ChartFactory.createPieChart(null, pieDs, true, false, false);
                pieChart.setBackgroundPaint(cardGlass);
                PiePlot pp = (PiePlot) pieChart.getPlot();
                pp.setBackgroundPaint(cardGlass); pp.setOutlineVisible(false);
                pp.setLabelGenerator(null); pp.setShadowPaint(null);
                Color[] cols = {pastelLilac, pastelGreen, pastelPeach, pastelBlue, pastelRed, new Color(130,200,200)};
                int ci = 0;
                for (Object k : pieDs.getKeys()) { pp.setSectionPaint((Comparable<?>)k, cols[ci%cols.length]); ci++; }
                if (pieChart.getLegend() != null) {
                    pieChart.getLegend().setBackgroundPaint(cardGlass);
                    pieChart.getLegend().setItemPaint(textMuted);
                }
                ChartPanel cp2 = new ChartPanel(pieChart); cp2.setOpaque(false); cp2.setBackground(cardGlass);
                chartBox2.removeAll();
                JLabel t2 = new JLabel("ASSET CATEGORIES"); t2.setForeground(textPrimary); t2.setFont(new Font("SansSerif", Font.BOLD, 12));
                chartBox2.add(t2, BorderLayout.NORTH); chartBox2.add(cp2, BorderLayout.CENTER);
                chartBox2.revalidate(); chartBox2.repaint();

                // IPO rows
                ipoListPanel.removeAll();
                ipoListPanel.add(Box.createRigidArea(new Dimension(0, 10)));
                if (marketRows != null && !marketRows.isEmpty()) {
                    for (MarketViewRow row : marketRows) {
                        boolean alreadyTracked = lmd.getAssetIds().contains(row.getAssetId());
                        ipoListPanel.add(createAdminIpoRow(row, alreadyTracked));
                        ipoListPanel.add(Box.createRigidArea(new Dimension(0, 6)));
                    }
                } else {
                    JLabel empty = new JLabel("No IPOs available.");
                    empty.setForeground(textMuted); empty.setBorder(new EmptyBorder(10,5,10,5));
                    ipoListPanel.add(empty);
                }
                ipoListPanel.revalidate(); ipoListPanel.repaint();
            }
        }.execute();
    }

    // ── IPO row with Verify & Track button ───────────────────────────────────
    private JPanel createAdminIpoRow(MarketViewRow row, boolean alreadyTracked) {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setOpaque(false); outer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        outer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,0,1,0, borderSubtle),
            new EmptyBorder(10,5,10,5)));

        JPanel leftCol = new JPanel(new GridLayout(2,1)); leftCol.setOpaque(false);
        JLabel nLabel = new JLabel(row.getAssetName());
        nLabel.setForeground(textPrimary); nLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        JLabel uLabel = new JLabel("Supply: " + String.format("%,d", row.getTotalUnits())
            + " units  ·  IPO price: $" + String.format("%.2f", row.getPricePerUnit()));
        uLabel.setForeground(textMuted); uLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        leftCol.add(nLabel); leftCol.add(uLabel);

        JPanel rightCol = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8)); rightCol.setOpaque(false);

        // Track status label
        JLabel trackLabel = new JLabel(alreadyTracked ? "● TRACKED" : "○ Untracked");
        trackLabel.setForeground(alreadyTracked ? pastelGreen : textMuted);
        trackLabel.setFont(new Font("SansSerif", Font.BOLD, 11));

        // Verify & Track button — calls LiveMarketDataManager.addAsset()
        JButton verifyBtn = new JButton(alreadyTracked ? "✓ Tracking" : "Verify & Track");
        verifyBtn.setBackground(alreadyTracked ? borderSubtle : pastelBlue);
        verifyBtn.setForeground(alreadyTracked ? textMuted : bgAbsoluteDark);
        verifyBtn.setFocusPainted(false); verifyBtn.setOpaque(true); verifyBtn.setBorderPainted(false);
        verifyBtn.setFont(new Font("SansSerif", Font.BOLD, 11));
        verifyBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        if (!alreadyTracked) {
            verifyBtn.addActionListener(e -> {
                addAssetToLiveMarket(row.getAssetId(), row.getAssetName(), row.getPricePerUnit());
                verifyBtn.setText("✓ Tracking");
                verifyBtn.setBackground(borderSubtle);
                verifyBtn.setForeground(textMuted);
                verifyBtn.setEnabled(false);
                trackLabel.setText("● TRACKED");
                trackLabel.setForeground(pastelGreen);
                JOptionPane.showMessageDialog(AdminDashUI.this,
                    "\"" + row.getAssetName() + "\" is now live in the market feed.\n"
                    + "Open the market to start broadcasting prices.",
                    "Asset Tracked", JOptionPane.INFORMATION_MESSAGE);
            });
        } else {
            verifyBtn.setEnabled(false);
        }

        // Manage button
        JButton editBtn = new JButton("Manage");
        editBtn.setBackground(borderSubtle); editBtn.setForeground(textPrimary);
        editBtn.setFocusPainted(false); editBtn.setFont(new Font("SansSerif", Font.PLAIN, 11));
        editBtn.setCursor(new Cursor(Cursor.HAND_CURSOR)); editBtn.setBorderPainted(false); editBtn.setOpaque(true);
        editBtn.addActionListener(e -> { dispose(); new AdminAssetsViewUI().setVisible(true); });

        rightCol.add(trackLabel); rightCol.add(verifyBtn); rightCol.add(editBtn);
        outer.add(leftCol, BorderLayout.WEST);
        outer.add(rightCol, BorderLayout.EAST);
        return outer;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private JLabel getCardLabel(JPanel card) {
        for (Component c : card.getComponents())
            if (c instanceof JLabel && "VALUE".equals(((JLabel)c).getName())) return (JLabel)c;
        return new JLabel();
    }

    private JPanel createStatCard(String title, String value, Color valueColor) {
        JPanel card = createRoundedPanel();
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(20, 25, 20, 25));
        JLabel tl = new JLabel(title); tl.setForeground(textMuted); tl.setFont(new Font("SansSerif", Font.BOLD, 11));
        JLabel vl = new JLabel(value); vl.setForeground(valueColor); vl.setFont(new Font("SansSerif", Font.BOLD, 28)); vl.setName("VALUE");
        card.add(tl, BorderLayout.NORTH); card.add(vl, BorderLayout.SOUTH);
        return card;
    }

    private JPanel createRoundedPanel() {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(cardGlass); g2.fillRoundRect(0,0,getWidth()-1,getHeight()-1,20,20);
                g2.setColor(borderSubtle); g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,20,20);
                g2.dispose();
            }
        };
        p.setOpaque(false); return p;
    }

    // ── Sidebar ───────────────────────────────────────────────────────────────
    class GrainySidebar extends JPanel {
        private boolean isExpanded = false;
        private final int EXPANDED_WIDTH = 250;
        private int currentWidth = 0;
        private Timer animator;
        private BufferedImage noiseOverlay;
        private JLabel logoLabel;
        private JButton[] navButtons;

        GrainySidebar() {
            setLayout(null); setOpaque(false); setPreferredSize(new Dimension(0, 0));
            noiseOverlay = new BufferedImage(EXPANDED_WIDTH, 1200, BufferedImage.TYPE_INT_ARGB);
            for (int x = 0; x < EXPANDED_WIDTH; x++)
                for (int y = 0; y < 1200; y++)
                    if (Math.random() > 0.85)
                        noiseOverlay.setRGB(x, y, new Color(255,255,255,(int)(Math.random()*10)).getRGB());
            logoLabel = new JLabel("Fractional.");
            logoLabel.setFont(new Font("Serif", Font.BOLD, 26)); logoLabel.setForeground(textPrimary);
            logoLabel.setBounds(25,20,200,40); logoLabel.setVisible(false); add(logoLabel);
            String[] nav = {"Main Dashboard","Manage IPOs","Investors List ↗","Asset Verification","Settings"};
            navButtons = new JButton[nav.length];
            for (int i = 0; i < nav.length; i++) {
                navButtons[i] = new JButton(nav[i]);
                navButtons[i].setFont(new Font("SansSerif", i==0 ? Font.BOLD : Font.PLAIN, 15));
                navButtons[i].setForeground(i==0 ? textPrimary : textMuted);
                navButtons[i].setContentAreaFilled(false); navButtons[i].setBorderPainted(false);
                navButtons[i].setFocusPainted(false); navButtons[i].setHorizontalAlignment(SwingConstants.LEFT);
                navButtons[i].setCursor(new Cursor(Cursor.HAND_CURSOR)); navButtons[i].setVisible(false);
                navButtons[i].setBounds(20, (i==nav.length-1)?650:100+(i*50), 200, 40);
                add(navButtons[i]);
            }
            navButtons[0].addActionListener(e -> { AdminDashUI.this.dispose(); new AdminControlCenter().setVisible(true); });
            navButtons[1].addActionListener(e -> { AdminDashUI.this.dispose(); new AdminAssetsViewUI().setVisible(true); });
            navButtons[2].addActionListener(e -> { AdminDashUI.this.dispose(); new Investors_AdminListViewUI().setVisible(true); });
            navButtons[3].addActionListener(e -> { AdminDashUI.this.dispose(); new AdminAssetsViewUI().setVisible(true); });
            navButtons[4].addActionListener(e -> {
                int c2 = JOptionPane.showConfirmDialog(AdminDashUI.this, "Sign out?", "Sign Out", JOptionPane.YES_NO_OPTION);
                if (c2 == JOptionPane.YES_OPTION) { LoginManager.logout(); dispose(); new edu.iiitd.dbms.ui.login.logintesterUI().setVisible(true); }
            });
        }

        void toggleSidebar() {
            if (animator != null && animator.isRunning()) return;
            int target = isExpanded ? 0 : EXPANDED_WIDTH, step = isExpanded ? -15 : 15;
            if (isExpanded) { logoLabel.setVisible(false); for (JButton b : navButtons) b.setVisible(false); }
            animator = new Timer(10, e -> {
                currentWidth += step;
                currentWidth = Math.max(0, Math.min(EXPANDED_WIDTH, currentWidth));
                setPreferredSize(new Dimension(currentWidth, 0)); revalidate(); repaint();
                if (currentWidth == target) {
                    isExpanded = !isExpanded; ((Timer)e.getSource()).stop();
                    if (isExpanded) { logoLabel.setVisible(true); for (JButton b : navButtons) b.setVisible(true); }
                }
            });
            animator.start();
        }

        @Override protected void paintComponent(Graphics g) {
            if (currentWidth == 0) return;
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            GeneralPath path = new GeneralPath();
            path.moveTo(0, 0);
            int cd = (int)(25 * ((double)currentWidth/EXPANDED_WIDTH));
            path.lineTo(getWidth()-cd, 0); path.quadTo(getWidth(), getHeight()/2.0, getWidth()-cd, getHeight());
            path.lineTo(0, getHeight()); path.closePath();
            g2.setColor(new Color(30,30,30,150)); g2.fill(path);
            g2.setClip(path); g2.drawImage(noiseOverlay, 0, 0, null);
            g2.dispose();
        }
    }

    class AestheticSearchBar extends JTextField {
        AestheticSearchBar(String placeholder) {
            setOpaque(false); setForeground(textMuted); setCaretColor(textPrimary);
            setBorder(new EmptyBorder(5,20,5,20)); setFont(new Font("SansSerif",Font.PLAIN,14)); setText(placeholder);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(cardGlass); g2.fillRoundRect(0,0,getWidth()-1,getHeight()-1,20,20);
            super.paintComponent(g); g2.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AdminDashUI().setVisible(true));
    }
}
