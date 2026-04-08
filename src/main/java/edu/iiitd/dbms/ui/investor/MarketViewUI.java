package edu.iiitd.dbms.ui.investor;

import edu.iiitd.dbms.auth.LoginManager;
import edu.iiitd.dbms.dto.InvestorMarketView.MarketViewRow;
import edu.iiitd.dbms.service.LiveMarketDataManager;
import edu.iiitd.dbms.service.MarketService;
import edu.iiitd.dbms.ui.components.LiveMarketChartPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Map;

/**
 * MarketViewUI — Secondary market view for investors.
 *
 * Changes vs original
 * ───────────────────
 *  • Replaced static MarketChartPanel with LiveMarketChartPanel (live data, IPO dropdown)
 *  • Asset screener rows show live prices from LiveMarketDataManager
 *  • "P2P TRADE" button in order ticket navigates to P2PTradeUI
 *  • Market OPEN badge tracks live state
 */
public class MarketViewUI extends JFrame {

    private final Color bgAbsoluteDark = new Color(14, 14, 14);
    private final Color cardGlass      = new Color(26, 26, 26);
    private final Color borderSubtle   = new Color(45, 45, 45);
    private final Color textPrimary    = new Color(250, 250, 250);
    private final Color textMuted      = new Color(150, 150, 150);
    private final Color pastelGreen    = new Color(119, 221, 119);
    private final Color pastelRed      = new Color(255, 105, 97);
    private final Color pastelBlue     = new Color(174, 198, 207);

    private Point dragPoint;
    private GrainySidebar sidebar;

    private final MarketService marketService = new MarketService();
    private final LiveMarketDataManager lmd   = LiveMarketDataManager.getInstance();

    private JPanel assetList;
    private AestheticSearchBar searchField;
    private LiveMarketChartPanel liveChart;

    // Screener live-update listener
    private final PropertyChangeListener screenerListener;

    // ────────────────────────────────────────────────────────────────────────
    public MarketViewUI() {
        setTitle("Fractional. - Secondary Market");
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
        searchField = new AestheticSearchBar("🔍  Search ticker or asset name...");
        searchField.setPreferredSize(new Dimension(350, 40));
        searchContainer.add(searchField);
        JPanel profilePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5));
        profilePanel.setOpaque(false);
        JLabel roleLabel = new JLabel("INVESTOR");
        roleLabel.setForeground(textMuted); roleLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
        JLabel profileLabel = new JLabel(getDisplayName());
        profileLabel.setForeground(textPrimary); profileLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        profilePanel.add(roleLabel); profilePanel.add(profileLabel);
        topBar.add(searchContainer, BorderLayout.WEST);
        topBar.add(profilePanel, BorderLayout.EAST);

        sidebar = new GrainySidebar();

        // ── LEFT COLUMN ───────────────────────────────────────────────────
        JPanel leftColumn = new JPanel();
        leftColumn.setLayout(new BoxLayout(leftColumn, BoxLayout.Y_AXIS));
        leftColumn.setOpaque(false);
        leftColumn.setBorder(new EmptyBorder(0, 0, 0, 20));

        // Live chart card
        JPanel chartCard = createRoundedPanel();
        chartCard.setLayout(new BorderLayout());
        chartCard.setBorder(new EmptyBorder(20, 20, 20, 20));
        liveChart = new LiveMarketChartPanel();
        chartCard.add(liveChart, BorderLayout.CENTER);

        // Screener card
        JPanel screenerPanel = createRoundedPanel();
        screenerPanel.setLayout(new BorderLayout());
        screenerPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        JPanel screenerHeader = new JPanel(new BorderLayout());
        screenerHeader.setOpaque(false);
        JLabel screenerTitle = new JLabel("SECONDARY MARKETPLACE");
        screenerTitle.setForeground(textPrimary);
        screenerTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        JLabel liveTag = new JLabel("● LIVE");
        liveTag.setForeground(lmd.isMarketOpen() ? pastelGreen : textMuted);
        liveTag.setFont(new Font("SansSerif", Font.BOLD, 11));
        screenerHeader.add(screenerTitle, BorderLayout.WEST);
        screenerHeader.add(liveTag, BorderLayout.EAST);
        screenerPanel.add(screenerHeader, BorderLayout.NORTH);

        assetList = new JPanel();
        assetList.setLayout(new BoxLayout(assetList, BoxLayout.Y_AXIS));
        assetList.setBackground(cardGlass);

        JScrollPane screenerScroll = new JScrollPane(assetList);
        screenerScroll.setBorder(null);
        screenerScroll.getViewport().setBackground(cardGlass);
        screenerScroll.setPreferredSize(new Dimension(0, 260));
        applyCustomScrollbar(screenerScroll);
        screenerPanel.add(screenerScroll, BorderLayout.CENTER);

        leftColumn.add(chartCard);
        leftColumn.add(Box.createRigidArea(new Dimension(0, 20)));
        leftColumn.add(screenerPanel);

        // ── RIGHT COLUMN ──────────────────────────────────────────────────
        JPanel rightColumn = new JPanel();
        rightColumn.setLayout(new BoxLayout(rightColumn, BoxLayout.Y_AXIS));
        rightColumn.setOpaque(false);
        rightColumn.setPreferredSize(new Dimension(380, 0));

        // Quick stats card
        JPanel statsCard = createRoundedPanel();
        statsCard.setLayout(new BoxLayout(statsCard, BoxLayout.Y_AXIS));
        statsCard.setBorder(new EmptyBorder(20, 20, 20, 20));
        JLabel statsTitle = new JLabel("MARKET OVERVIEW");
        statsTitle.setForeground(textMuted); statsTitle.setFont(new Font("SansSerif", Font.BOLD, 11));
        statsCard.add(statsTitle);
        statsCard.add(Box.createRigidArea(new Dimension(0, 12)));
        Map<Integer, String> names = lmd.getAssetNames();
        statsCard.add(createStatRow("Total Tracked Assets", String.valueOf(names.size())));
        statsCard.add(Box.createRigidArea(new Dimension(0, 8)));
        JLabel marketStateRow = new JLabel("Market Status:  "
            + (lmd.isMarketOpen() ? "OPEN" : "CLOSED"));
        marketStateRow.setForeground(lmd.isMarketOpen() ? pastelGreen : pastelRed);
        marketStateRow.setFont(new Font("SansSerif", Font.BOLD, 13));
        statsCard.add(marketStateRow);

        // Order ticket card
        JPanel orderTicket = createRoundedPanel();
        orderTicket.setLayout(new BoxLayout(orderTicket, BoxLayout.Y_AXIS));
        orderTicket.setBorder(new EmptyBorder(25, 25, 25, 25));

        JLabel ticketTitle = new JLabel("TRADE ACTIONS");
        ticketTitle.setForeground(textPrimary);
        ticketTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        ticketTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel ticketSub = new JLabel("Use the P2P market to trade directly with other investors.");
        ticketSub.setForeground(textMuted);
        ticketSub.setFont(new Font("SansSerif", Font.PLAIN, 12));
        ticketSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Main P2P button
        JButton p2pBtn = new JButton("🔄  OPEN P2P MARKET");
        p2pBtn.setBackground(pastelBlue);
        p2pBtn.setForeground(bgAbsoluteDark);
        p2pBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        p2pBtn.setFocusPainted(false); p2pBtn.setOpaque(true);
        p2pBtn.setBorderPainted(false);
        p2pBtn.setMaximumSize(new Dimension(400, 48));
        p2pBtn.setBorder(new EmptyBorder(12, 0, 12, 0));
        p2pBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        p2pBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        p2pBtn.addActionListener(e -> { dispose(); new P2PTradeUI().setVisible(true); });

        JButton sellBtn = new JButton("📤  LIST MY UNITS FOR SALE");
        sellBtn.setBackground(pastelRed);
        sellBtn.setForeground(bgAbsoluteDark);
        sellBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        sellBtn.setFocusPainted(false); sellBtn.setOpaque(true);
        sellBtn.setBorderPainted(false);
        sellBtn.setMaximumSize(new Dimension(400, 44));
        sellBtn.setBorder(new EmptyBorder(10, 0, 10, 0));
        sellBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        sellBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        sellBtn.addActionListener(e -> { dispose(); new SellOrderUI().setVisible(true); });

        JButton buyIpoBtn = new JButton("📥  BUY IPO UNITS");
        buyIpoBtn.setBackground(pastelGreen);
        buyIpoBtn.setForeground(bgAbsoluteDark);
        buyIpoBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        buyIpoBtn.setFocusPainted(false); buyIpoBtn.setOpaque(true);
        buyIpoBtn.setBorderPainted(false);
        buyIpoBtn.setMaximumSize(new Dimension(400, 44));
        buyIpoBtn.setBorder(new EmptyBorder(10, 0, 10, 0));
        buyIpoBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        buyIpoBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        buyIpoBtn.addActionListener(e -> { dispose(); new BuyOrderUI().setVisible(true); });

        orderTicket.add(ticketTitle);
        orderTicket.add(Box.createRigidArea(new Dimension(0, 8)));
        orderTicket.add(ticketSub);
        orderTicket.add(Box.createRigidArea(new Dimension(0, 20)));
        orderTicket.add(p2pBtn);
        orderTicket.add(Box.createRigidArea(new Dimension(0, 12)));
        orderTicket.add(sellBtn);
        orderTicket.add(Box.createRigidArea(new Dimension(0, 10)));
        orderTicket.add(buyIpoBtn);

        rightColumn.add(statsCard);
        rightColumn.add(Box.createRigidArea(new Dimension(0, 20)));
        rightColumn.add(orderTicket);

        // ── Layout ────────────────────────────────────────────────────────
        JPanel gridContent = new JPanel(new BorderLayout());
        gridContent.setOpaque(false);
        gridContent.setBorder(new EmptyBorder(20, 25, 30, 25));
        gridContent.add(leftColumn, BorderLayout.CENTER);
        gridContent.add(rightColumn, BorderLayout.EAST);

        JScrollPane mainScroll = new JScrollPane(gridContent);
        mainScroll.setBorder(null);
        mainScroll.getViewport().setBackground(bgAbsoluteDark);
        mainScroll.getVerticalScrollBar().setUnitIncrement(16);
        applyCustomScrollbar(mainScroll);

        JPanel contentWrapper = new JPanel(new BorderLayout());
        contentWrapper.setOpaque(false);
        contentWrapper.add(topBar, BorderLayout.NORTH);
        contentWrapper.add(mainScroll, BorderLayout.CENTER);

        add(dragBar, BorderLayout.NORTH);
        add(sidebar, BorderLayout.WEST);
        add(contentWrapper, BorderLayout.CENTER);

        // ── Live screener updates ─────────────────────────────────────────
        screenerListener = evt -> {
            if ("priceUpdate".equals(evt.getPropertyName())) {
                @SuppressWarnings("unchecked")
                Map<Integer, Double> prices = (Map<Integer, Double>) evt.getNewValue();
                SwingUtilities.invokeLater(() -> refreshScreenerPrices(prices));
            } else if ("marketState".equals(evt.getPropertyName())) {
                boolean open = (Boolean) evt.getNewValue();
                SwingUtilities.invokeLater(() -> {
                    liveTag.setText("● " + (open ? "LIVE" : "CLOSED"));
                    liveTag.setForeground(open ? pastelGreen : textMuted);
                    marketStateRow.setText("Market Status:  " + (open ? "OPEN" : "CLOSED"));
                    marketStateRow.setForeground(open ? pastelGreen : pastelRed);
                });
            }
        };
        lmd.addListener(screenerListener);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent e) {
                lmd.removeListener(screenerListener);
            }
        });

        loadAllMarketRows();
    }

    // ── Screener loading / updating ───────────────────────────────────────────
    private void loadAllMarketRows() {
        try {
            List<MarketViewRow> rows = marketService.getAllMarketRows();
            populateAssetList(rows, lmd.getAllCurrentPrices());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Failed to load market data: " + e.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void populateAssetList(List<MarketViewRow> rows, Map<Integer, Double> livePrices) {
        assetList.removeAll();
        assetList.add(Box.createRigidArea(new Dimension(0, 10)));
        if (rows == null || rows.isEmpty()) {
            JLabel empty = new JLabel("No market listings found.");
            empty.setForeground(textMuted); empty.setFont(new Font("SansSerif", Font.PLAIN, 14));
            assetList.add(empty);
        } else {
            for (int i = 0; i < rows.size(); i++) {
                MarketViewRow row = rows.get(i);
                double live = livePrices.getOrDefault(row.getAssetId(), row.getPricePerUnit());
                double base = row.getPricePerUnit();
                double pct  = base > 0 ? (live - base) / base * 100 : 0;
                String sign = pct >= 0 ? "+" : "";
                Color  col  = pct >= 0 ? pastelGreen : pastelRed;
                String pctStr = String.format("%s%.2f%%", sign, pct);
                JPanel rowPanel = createAssetRow(
                    row.getAssetName(), "ID: " + row.getAssetId() + "  ·  " + row.getCategory(),
                    String.format("$%.4f", live), pctStr, col, row.getAssetId());
                assetList.add(rowPanel);
                if (i < rows.size() - 1) assetList.add(Box.createRigidArea(new Dimension(0, 8)));
            }
        }
        assetList.revalidate();
        assetList.repaint();
    }

    private void refreshScreenerPrices(Map<Integer, Double> livePrices) {
        Component[] comps = assetList.getComponents();
        for (Component c : comps) {
            if (c instanceof JPanel row) {
                Object aidObj = ((JPanel) c).getClientProperty("assetId");
                if (aidObj instanceof Integer assetId) {
                    double live = livePrices.getOrDefault(assetId, 0.0);
                    if (live > 0) {
                        // Update price + change labels within this row
                        updateRowPrice(row, assetId, live);
                    }
                }
            }
        }
    }

    private void updateRowPrice(JPanel row, int assetId, double live) {
        // Row structure: BorderLayout  WEST=leftCol  EAST=rightCol(GridLayout 2x1)
        try {
            BorderLayout bl = (BorderLayout) row.getLayout();
            Component east = bl.getLayoutComponent(BorderLayout.EAST);
            if (east instanceof JPanel rightCol) {
                Component[] labels = rightCol.getComponents();
                if (labels.length >= 2 && labels[0] instanceof JLabel pLbl && labels[1] instanceof JLabel cLbl) {
                    Double base = (Double) row.getClientProperty("basePrice");
                    if (base == null || base == 0) base = live;
                    double pct = (live - base) / base * 100;
                    String sign = pct >= 0 ? "+" : "";
                    pLbl.setText(String.format("$%.4f", live));
                    cLbl.setText(String.format("%s%.2f%%", sign, pct));
                    cLbl.setForeground(pct >= 0 ? pastelGreen : pastelRed);
                }
            }
        } catch (Exception ignored) {}
    }

    // ── UI helpers ────────────────────────────────────────────────────────────
    private JPanel createAssetRow(String name, String sub, String price, String change,
                                  Color changeColor, int assetId) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, borderSubtle),
            new EmptyBorder(10, 5, 10, 5)));
        row.putClientProperty("assetId",   assetId);
        row.putClientProperty("basePrice", 0.0); // will be set externally if needed

        JPanel leftCol = new JPanel(new GridLayout(2, 1));
        leftCol.setOpaque(false);
        JLabel nLabel = new JLabel(name);
        nLabel.setForeground(textPrimary); nLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        JLabel tLabel = new JLabel(sub);
        tLabel.setForeground(textMuted); tLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        leftCol.add(nLabel); leftCol.add(tLabel);

        JPanel rightCol = new JPanel(new GridLayout(2, 1));
        rightCol.setOpaque(false);
        JLabel pLabel = new JLabel(price, SwingConstants.RIGHT);
        pLabel.setForeground(textPrimary); pLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        JLabel cLabel = new JLabel(change, SwingConstants.RIGHT);
        cLabel.setForeground(changeColor); cLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        rightCol.add(pLabel); rightCol.add(cLabel);

        row.add(leftCol, BorderLayout.WEST);
        row.add(rightCol, BorderLayout.EAST);
        return row;
    }

    private JPanel createStatRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(400, 28));
        JLabel lbl = new JLabel(label); lbl.setForeground(textMuted); lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        JLabel val = new JLabel(value);  val.setForeground(textPrimary); val.setFont(new Font("SansSerif", Font.BOLD, 13));
        row.add(lbl, BorderLayout.WEST); row.add(val, BorderLayout.EAST);
        return row;
    }

    private String getDisplayName() {
        if (LoginManager.getCurrentUser() == null) return "INVESTOR";
        String n = LoginManager.getCurrentUser().getName();
        if (n == null || n.isBlank()) return "INVESTOR";
        String[] p = n.trim().split("\\s+");
        return p[0].toUpperCase() + (p.length > 1 ? " " + Character.toUpperCase(p[p.length-1].charAt(0)) + "." : "");
    }

    private JPanel createRoundedPanel() {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(cardGlass); g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20);
                g2.setColor(borderSubtle); g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20);
                g2.dispose();
            }
        };
        p.setOpaque(false); return p;
    }

    private void applyCustomScrollbar(JScrollPane sp) {
        sp.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() { thumbColor = borderSubtle; trackColor = cardGlass; }
            @Override protected JButton createDecreaseButton(int o) { return z(); }
            @Override protected JButton createIncreaseButton(int o) { return z(); }
            private JButton z() { JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0)); return b; }
        });
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
            logoLabel.setBounds(25, 20, 200, 40); logoLabel.setVisible(false);
            add(logoLabel);
            String[] nav = {"Main Dashboard","Market View","Wallet","Holdings","Profile Settings"};
            navButtons = new JButton[nav.length];
            for (int i = 0; i < nav.length; i++) {
                navButtons[i] = new JButton(nav[i]);
                navButtons[i].setFont(new Font("SansSerif", i == 1 ? Font.BOLD : Font.PLAIN, 15));
                navButtons[i].setForeground(i == 1 ? textPrimary : textMuted);
                navButtons[i].setContentAreaFilled(false); navButtons[i].setBorderPainted(false);
                navButtons[i].setFocusPainted(false); navButtons[i].setHorizontalAlignment(SwingConstants.LEFT);
                navButtons[i].setCursor(new Cursor(Cursor.HAND_CURSOR)); navButtons[i].setVisible(false);
                int yp = (i == nav.length-1) ? 650 : 100 + i*50;
                navButtons[i].setBounds(20, yp, 200, 40);
                add(navButtons[i]);
            }
            navButtons[0].addActionListener(e -> { MarketViewUI.this.dispose(); new InvestorDashUI().setVisible(true); });
            navButtons[1].addActionListener(e -> { MarketViewUI.this.dispose(); new MarketViewUI().setVisible(true); });
            navButtons[2].addActionListener(e -> { MarketViewUI.this.dispose(); new WalletViewUI().setVisible(true); });
            navButtons[3].addActionListener(e -> { MarketViewUI.this.dispose(); new HoldingsViewUI_updated().setVisible(true); });
            navButtons[4].addActionListener(e -> { MarketViewUI.this.dispose(); new ProfileSettingsUI().setVisible(true); });
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
            int w = getWidth(), h = getHeight();
            GeneralPath path = new GeneralPath();
            path.moveTo(0, 0);
            int cd = (int)(25 * ((double)currentWidth/EXPANDED_WIDTH));
            path.lineTo(w-cd, 0); path.quadTo(w, h/2.0, w-cd, h);
            path.lineTo(0, h); path.closePath();
            g2.setColor(new Color(30,30,30,150)); g2.fill(path);
            g2.setClip(path); g2.drawImage(noiseOverlay, 0, 0, null);
            g2.dispose();
        }
    }

    class AestheticSearchBar extends JTextField {
        AestheticSearchBar(String placeholder) {
            setOpaque(false); setForeground(textMuted); setCaretColor(textPrimary);
            setBorder(new EmptyBorder(5, 20, 5, 20)); setFont(new Font("SansSerif", Font.PLAIN, 14));
            setText(placeholder);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bgAbsoluteDark); g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20);
            g2.setColor(borderSubtle); g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20);
            super.paintComponent(g2); g2.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MarketViewUI().setVisible(true));
    }
}
