package edu.iiitd.dbms.ui.investor;

import edu.iiitd.dbms.auth.LoginManager;

import edu.iiitd.dbms.data_access.OwnershipHistoryDAO;
import edu.iiitd.dbms.data_access.TradeOrderDAO;
import edu.iiitd.dbms.data_access.AssetDAO;
import edu.iiitd.dbms.domain.OwnershipHistory;
import edu.iiitd.dbms.domain.TradeOrder;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.util.List;

public class TradeHistoryUI extends JFrame {

    private final Color bgAbsoluteDark = new Color(14, 14, 14);
    private final Color cardGlass = new Color(26, 26, 26);
    private final Color borderSubtle = new Color(45, 45, 45);
    private final Color textPrimary = new Color(250, 250, 250);
    private final Color textMuted = new Color(150, 150, 150);
    private final Color pastelGreen = new Color(119, 221, 119);
    private final Color pastelRed = new Color(255, 105, 97);
    private final Color pastelBlue = new Color(174, 198, 207);

    private Point dragPoint;
    private GrainySidebar sidebar;

    private JPanel ordersListPanel;
    private JPanel historyListPanel;
    private JButton ordersTabBtn;
    private JButton historyTabBtn;
    private JPanel tabContent;

    private int currentInvestorId = LoginManager.getCurrentUser() != null ? LoginManager.getCurrentUser().getLinkedId() : 1;
    private final TradeOrderDAO tradeOrderDAO = new TradeOrderDAO();
    private final OwnershipHistoryDAO ownershipHistoryDAO = new OwnershipHistoryDAO();
    private final AssetDAO assetDAO = new AssetDAO();

    private String getDisplayName() {
        if (LoginManager.getCurrentUser() == null) return "INVESTOR";
        String full = LoginManager.getCurrentUser().getName();
        if (full == null || full.isBlank()) return "INVESTOR";
        String[] p = full.trim().split("\s+");
        return p[0].toUpperCase() + (p.length > 1 ? " " + Character.toUpperCase(p[p.length-1].charAt(0)) + "." : "");
    }

    public TradeHistoryUI() {
        setTitle("Fractional. - Trade History");
        setSize(1350, 850);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setUndecorated(true);
        setLayout(new BorderLayout());
        getContentPane().setBackground(bgAbsoluteDark);

        // --- DRAG BAR ---
        JPanel dragBar = new JPanel(new BorderLayout());
        dragBar.setBackground(bgAbsoluteDark);
        dragBar.setPreferredSize(new Dimension(1350, 30));
        JLabel closeBtn = new JLabel("  ✕  ");
        closeBtn.setForeground(textMuted);
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeBtn.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { System.exit(0); }
            public void mouseEntered(MouseEvent e) { closeBtn.setForeground(textPrimary); }
            public void mouseExited(MouseEvent e) { closeBtn.setForeground(textMuted); }
        });
        dragBar.add(closeBtn, BorderLayout.EAST);
        dragBar.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { dragPoint = e.getPoint(); }
        });
        dragBar.addMouseMotionListener(new MouseAdapter() {
            public void mouseDragged(MouseEvent e) {
                setLocation(getLocation().x + e.getX() - dragPoint.x, getLocation().y + e.getY() - dragPoint.y);
            }
        });

        // --- TOP NAV BAR ---
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(bgAbsoluteDark);
        topBar.setBorder(new EmptyBorder(10, 15, 10, 25));
        JButton toggleBtn = new JButton("☰");
        toggleBtn.setFont(new Font("SansSerif", Font.BOLD, 20));
        toggleBtn.setForeground(textPrimary);
        toggleBtn.setContentAreaFilled(false);
        toggleBtn.setBorderPainted(false);
        toggleBtn.setFocusPainted(false);
        toggleBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        toggleBtn.addActionListener(e -> sidebar.toggleSidebar());
        JPanel searchContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        searchContainer.setOpaque(false);
        searchContainer.add(toggleBtn);
        JPanel profilePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5));
        profilePanel.setOpaque(false);
        JLabel roleLabel = new JLabel("INVESTOR");
        roleLabel.setForeground(textMuted);
        roleLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
        JLabel profileLabel = new JLabel(getDisplayName());
        profileLabel.setForeground(textPrimary);
        profileLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        profilePanel.add(roleLabel);
        profilePanel.add(profileLabel);
        topBar.add(searchContainer, BorderLayout.WEST);
        topBar.add(profilePanel, BorderLayout.EAST);

        sidebar = new GrainySidebar();

        // --- MAIN CONTENT ---
        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setOpaque(false);
        mainContent.setBorder(new EmptyBorder(20, 40, 40, 40));

        // Page title
        JLabel pageTitle = new JLabel("TRADE & OWNERSHIP HISTORY");
        pageTitle.setForeground(textPrimary);
        pageTitle.setFont(new Font("SansSerif", Font.BOLD, 22));
        pageTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        // --- SUMMARY STATS ROW ---
        JPanel statsRow = new JPanel(new GridLayout(1, 3, 20, 0));
        statsRow.setOpaque(false);
        statsRow.setMaximumSize(new Dimension(1350, 100));
        statsRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel openOrdersCard = createStatCard("OPEN ORDERS", "—", textPrimary);
        JPanel filledOrdersCard = createStatCard("FILLED / MATCHED", "—", pastelGreen);
        JPanel historyEntriesCard = createStatCard("HISTORY ENTRIES", "—", pastelBlue);

        statsRow.add(openOrdersCard);
        statsRow.add(filledOrdersCard);
        statsRow.add(historyEntriesCard);

        // --- TABS ---
        JPanel tabBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabBar.setOpaque(false);
        tabBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        tabBar.setMaximumSize(new Dimension(1350, 50));

        ordersTabBtn = createTabButton("MY ORDERS", true);
        historyTabBtn = createTabButton("OWNERSHIP HISTORY", false);

        ordersTabBtn.addActionListener(e -> switchTab(true));
        historyTabBtn.addActionListener(e -> switchTab(false));

        tabBar.add(ordersTabBtn);
        tabBar.add(Box.createRigidArea(new Dimension(5, 0)));
        tabBar.add(historyTabBtn);

        // --- CONTENT CARD (holds tab panels) ---
        JPanel contentCard = createRoundedPanel();
        contentCard.setLayout(new BorderLayout());
        contentCard.setBorder(new EmptyBorder(25, 30, 30, 30));
        contentCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Orders panel
        ordersListPanel = new JPanel();
        ordersListPanel.setLayout(new BoxLayout(ordersListPanel, BoxLayout.Y_AXIS));
        ordersListPanel.setOpaque(false);

        // History panel
        historyListPanel = new JPanel();
        historyListPanel.setLayout(new BoxLayout(historyListPanel, BoxLayout.Y_AXIS));
        historyListPanel.setOpaque(false);

        tabContent = new JPanel(new CardLayout());
        tabContent.setOpaque(false);

        JScrollPane ordersScroll = buildScrollPane(ordersListPanel);
        JScrollPane historyScroll = buildScrollPane(historyListPanel);

        tabContent.add(ordersScroll, "ORDERS");
        tabContent.add(historyScroll, "HISTORY");

        contentCard.add(tabContent, BorderLayout.CENTER);

        // --- ASSEMBLY ---
        mainContent.add(pageTitle);
        mainContent.add(Box.createRigidArea(new Dimension(0, 20)));
        mainContent.add(statsRow);
        mainContent.add(Box.createRigidArea(new Dimension(0, 20)));
        mainContent.add(tabBar);
        mainContent.add(Box.createRigidArea(new Dimension(0, 10)));
        mainContent.add(contentCard);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(topBar, BorderLayout.NORTH);
        wrapper.add(mainContent, BorderLayout.CENTER);

        add(dragBar, BorderLayout.NORTH);
        add(sidebar, BorderLayout.WEST);
        add(wrapper, BorderLayout.CENTER);

        loadData(openOrdersCard, filledOrdersCard, historyEntriesCard);
    }

    // --- DATA ---
    private void loadData(JPanel openCard, JPanel filledCard, JPanel histCard) {
        try {
            List<TradeOrder> orders = tradeOrderDAO.listOrdersByInvestor(currentInvestorId);
            long openCount = orders.stream().filter(o -> "OPEN".equalsIgnoreCase(o.getStatus())).count();
            long filledCount = orders.stream().filter(o -> "FILLED".equalsIgnoreCase(o.getStatus()) || "MATCHED".equalsIgnoreCase(o.getStatus())).count();

            // Update stat cards
            updateStatCardValue(openCard, String.valueOf(openCount));
            updateStatCardValue(filledCard, String.valueOf(filledCount));

            // Populate orders panel
            ordersListPanel.removeAll();
            if (orders.isEmpty()) {
                ordersListPanel.add(emptyLabel("No orders found."));
            } else {
                // Header row
                ordersListPanel.add(createOrdersHeader());
                ordersListPanel.add(Box.createRigidArea(new Dimension(0, 8)));
                for (TradeOrder o : orders) {
                    ordersListPanel.add(createOrderRow(o));
                    ordersListPanel.add(Box.createRigidArea(new Dimension(0, 6)));
                }
            }
            ordersListPanel.revalidate();
            ordersListPanel.repaint();

            // Ownership history
            List<OwnershipHistory> history = ownershipHistoryDAO.listByInvestorId(currentInvestorId);
            updateStatCardValue(histCard, String.valueOf(history.size()));

            historyListPanel.removeAll();
            if (history.isEmpty()) {
                historyListPanel.add(emptyLabel("No ownership history found."));
            } else {
                historyListPanel.add(createHistoryHeader());
                historyListPanel.add(Box.createRigidArea(new Dimension(0, 8)));
                for (OwnershipHistory h : history) {
                    historyListPanel.add(createHistoryRow(h));
                    historyListPanel.add(Box.createRigidArea(new Dimension(0, 6)));
                }
            }
            historyListPanel.revalidate();
            historyListPanel.repaint();

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to load history: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // --- ROW BUILDERS ---
    private JPanel createOrdersHeader() {
        JPanel row = new JPanel(new GridLayout(1, 5));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(1350, 30));
        row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, borderSubtle));
        String[] cols = {"ORDER ID", "ASSET ID", "TYPE", "UNITS / PRICE", "STATUS"};
        for (String col : cols) {
            JLabel lbl = new JLabel(col);
            lbl.setForeground(textMuted);
            lbl.setFont(new Font("SansSerif", Font.BOLD, 10));
            row.add(lbl);
        }
        return row;
    }

    private JPanel createOrderRow(TradeOrder o) {
        JPanel row = new JPanel(new GridLayout(1, 5));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(1350, 50));
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(35, 35, 35)),
            new EmptyBorder(10, 0, 10, 0)
        ));

        boolean isBuy = "BUY".equalsIgnoreCase(o.getOrderType());
        Color typeColor = isBuy ? pastelGreen : pastelRed;
        String statusStr = o.getStatus() != null ? o.getStatus() : "—";
        Color statusColor = "OPEN".equalsIgnoreCase(statusStr) ? pastelBlue
                         : ("FILLED".equalsIgnoreCase(statusStr) || "MATCHED".equalsIgnoreCase(statusStr)) ? pastelGreen : textMuted;

        row.add(makeCell("#" + o.getOrderId(), textMuted, false));
        row.add(makeCell("Asset " + (o.getAssetId() != null ? o.getAssetId() : "—"), textPrimary, false));
        row.add(makeCell(o.getOrderType() != null ? o.getOrderType() : "—", typeColor, true));
        row.add(makeCell((o.getUnits() != null ? o.getUnits() : "—") + " @ $" + String.format("%.2f", o.getPrice() != null ? o.getPrice() : 0), textPrimary, false));
        row.add(makeCell(statusStr, statusColor, true));

        return row;
    }

    private JPanel createHistoryHeader() {
        JPanel row = new JPanel(new GridLayout(1, 5));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(1350, 30));
        row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, borderSubtle));
        String[] cols = {"DATE", "ASSET ID", "CHANGE TYPE", "UNITS BEFORE → AFTER", "DELTA"};
        for (String col : cols) {
            JLabel lbl = new JLabel(col);
            lbl.setForeground(textMuted);
            lbl.setFont(new Font("SansSerif", Font.BOLD, 10));
            row.add(lbl);
        }
        return row;
    }

    private JPanel createHistoryRow(OwnershipHistory h) {
        JPanel row = new JPanel(new GridLayout(1, 5));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(1350, 50));
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(35, 35, 35)),
            new EmptyBorder(10, 0, 10, 0)
        ));

        int before = h.getUnitsBefore() != null ? h.getUnitsBefore() : 0;
        int after = h.getUnitsAfter() != null ? h.getUnitsAfter() : 0;
        int delta = after - before;
        Color deltaColor = delta >= 0 ? pastelGreen : pastelRed;
        String deltaStr = (delta >= 0 ? "+" : "") + delta;
        String dateStr = h.getChangeDate() != null ? h.getChangeDate().toString() : "—";
        String changeType = h.getChangeType() != null ? h.getChangeType() : "—";

        row.add(makeCell(dateStr, textMuted, false));
        row.add(makeCell("Asset " + (h.getAssetId() != null ? h.getAssetId() : "—"), textPrimary, false));
        row.add(makeCell(changeType, pastelBlue, false));
        row.add(makeCell(before + " → " + after + " units", textPrimary, false));
        row.add(makeCell(deltaStr, deltaColor, true));

        return row;
    }

    // --- HELPERS ---
    private JLabel makeCell(String text, Color color, boolean bold) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(color);
        lbl.setFont(new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, 13));
        return lbl;
    }

    private JLabel emptyLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(textMuted);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 14));
        lbl.setBorder(new EmptyBorder(20, 5, 20, 5));
        return lbl;
    }

    private void switchTab(boolean showOrders) {
        CardLayout cl = (CardLayout) tabContent.getLayout();
        cl.show(tabContent, showOrders ? "ORDERS" : "HISTORY");
        styleTabButton(ordersTabBtn, showOrders);
        styleTabButton(historyTabBtn, !showOrders);
    }

    private JButton createTabButton(String text, boolean active) {
        JButton btn = new JButton(text);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, active ? 2 : 0, 0, active ? textPrimary : borderSubtle),
            new EmptyBorder(8, 0, 8, 25)
        ));
        btn.setForeground(active ? textPrimary : textMuted);
        return btn;
    }

    private void styleTabButton(JButton btn, boolean active) {
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, active ? 2 : 0, 0, active ? textPrimary : borderSubtle),
            new EmptyBorder(8, 0, 8, 25)
        ));
        btn.setForeground(active ? textPrimary : textMuted);
    }

    private JPanel createStatCard(String title, String initialValue, Color valueColor) {
        JPanel card = createRoundedPanel();
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(20, 25, 20, 25));
        JLabel titleLbl = new JLabel(title);
        titleLbl.setForeground(textMuted);
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 11));
        JLabel valLbl = new JLabel(initialValue);
        valLbl.setForeground(valueColor);
        valLbl.setFont(new Font("SansSerif", Font.BOLD, 28));
        valLbl.setName("VALUE");
        card.add(titleLbl, BorderLayout.NORTH);
        card.add(valLbl, BorderLayout.SOUTH);
        return card;
    }

    private void updateStatCardValue(JPanel card, String value) {
        for (Component c : card.getComponents()) {
            if (c instanceof JLabel && "VALUE".equals(((JLabel) c).getName())) {
                ((JLabel) c).setText(value);
                break;
            }
        }
    }

    private JScrollPane buildScrollPane(JPanel content) {
        JScrollPane sp = new JScrollPane(content);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getViewport().setBackground(cardGlass);
        applyCustomScrollbar(sp);
        return sp;
    }

    private JPanel createRoundedPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(cardGlass);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                g2.setColor(borderSubtle);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        return panel;
    }

    private void applyCustomScrollbar(JScrollPane scrollPane) {
        scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() { this.thumbColor = borderSubtle; this.trackColor = cardGlass; }
            @Override protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
            @Override protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }
            private JButton createZeroButton() { JButton jb = new JButton(); jb.setPreferredSize(new Dimension(0, 0)); return jb; }
        });
    }

    // --- SIDEBAR ---
    class GrainySidebar extends JPanel {
        private boolean isExpanded = false;
        private final int EXPANDED_WIDTH = 250;
        private int currentWidth = 0;
        private Timer animator;
        private BufferedImage noiseOverlay;
        private JLabel logoLabel;
        private JButton[] navButtons;

        public GrainySidebar() {
            setLayout(null);
            setOpaque(false);
            setPreferredSize(new Dimension(currentWidth, 0));
            generateNoiseTexture();

            logoLabel = new JLabel("Fractional.");
            logoLabel.setFont(new Font("Serif", Font.BOLD, 26));
            logoLabel.setForeground(textPrimary);
            logoLabel.setBounds(25, 20, 200, 40);
            logoLabel.setVisible(false);
            add(logoLabel);

            // Trade History is not a main nav item — highlight Holdings as parent
            String[] navItems = {"Main Dashboard", "Market View", "Wallet", "Holdings", "Profile Settings"};
            navButtons = new JButton[navItems.length];
            for (int i = 0; i < navItems.length; i++) {
                navButtons[i] = new JButton(navItems[i]);
                navButtons[i].setFont(new Font("SansSerif", Font.PLAIN, 15));
                navButtons[i].setForeground(textMuted);
                navButtons[i].setContentAreaFilled(false);
                navButtons[i].setBorderPainted(false);
                navButtons[i].setFocusPainted(false);
                navButtons[i].setHorizontalAlignment(SwingConstants.LEFT);
                navButtons[i].setCursor(new Cursor(Cursor.HAND_CURSOR));
                navButtons[i].setVisible(false);
                int yPos = (i == navItems.length - 1) ? 650 : 100 + (i * 50);
                navButtons[i].setBounds(20, yPos, 200, 40);
                add(navButtons[i]);
            }

            navButtons[0].addActionListener(e -> { dispose(); new InvestorDashUI().setVisible(true); });
            navButtons[1].addActionListener(e -> { dispose(); new MarketViewUI_updated().setVisible(true); });
            navButtons[2].addActionListener(e -> { dispose(); new WalletViewUI().setVisible(true); });
            navButtons[3].addActionListener(e -> { dispose(); new HoldingsViewUI_updated().setVisible(true); });
            navButtons[4].addActionListener(e -> { dispose(); new ProfileSettingsUI().setVisible(true); });
        }

        public void toggleSidebar() {
            if (animator != null && animator.isRunning()) return;
            int targetWidth = isExpanded ? 0 : EXPANDED_WIDTH;
            int step = isExpanded ? -15 : 15;
            if (isExpanded) { logoLabel.setVisible(false); for (JButton b : navButtons) b.setVisible(false); }
            animator = new Timer(10, e -> {
                currentWidth += step;
                if (currentWidth < 0) currentWidth = 0;
                if (currentWidth > EXPANDED_WIDTH) currentWidth = EXPANDED_WIDTH;
                setPreferredSize(new Dimension(currentWidth, 0));
                revalidate(); repaint();
                if (currentWidth == targetWidth) {
                    isExpanded = !isExpanded;
                    ((Timer) e.getSource()).stop();
                    if (isExpanded) { logoLabel.setVisible(true); for (JButton b : navButtons) b.setVisible(true); }
                }
            });
            animator.start();
        }

        private void generateNoiseTexture() {
            noiseOverlay = new BufferedImage(EXPANDED_WIDTH, 1200, BufferedImage.TYPE_INT_ARGB);
            for (int x = 0; x < EXPANDED_WIDTH; x++)
                for (int y = 0; y < 1200; y++)
                    if (Math.random() > 0.85)
                        noiseOverlay.setRGB(x, y, new Color(255, 255, 255, (int)(Math.random() * 10)).getRGB());
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (currentWidth == 0) return;
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            GeneralPath path = new GeneralPath();
            path.moveTo(0, 0);
            int curveDepth = (int)(25 * ((double) currentWidth / EXPANDED_WIDTH));
            path.lineTo(getWidth() - curveDepth, 0);
            path.quadTo(getWidth(), getHeight() / 2.0, getWidth() - curveDepth, getHeight());
            path.lineTo(0, getHeight());
            path.closePath();
            g2.setColor(new Color(30, 30, 30, 150));
            g2.fill(path);
            g2.setClip(path);
            g2.drawImage(noiseOverlay, 0, 0, null);
            g2.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TradeHistoryUI().setVisible(true));
    }
}