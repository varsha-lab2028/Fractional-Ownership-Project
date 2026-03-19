package edu.iiitd.dbms.ui.admin;

import edu.iiitd.dbms.auth.LoginManager;

import edu.iiitd.dbms.data_access.AssetDAO;
import edu.iiitd.dbms.data_access.TradeDAO;
import edu.iiitd.dbms.data_access.TradeOrderDAO;
import edu.iiitd.dbms.data_access.IpoDAO;
import edu.iiitd.dbms.dto.InvestorMarketView.VerifiedAssetRow;
import edu.iiitd.dbms.dto.InvestorMarketView.MarketViewRow;
import edu.iiitd.dbms.domain.Trade;
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

public class AdminAssetsViewUI extends JFrame {

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

    private JPanel assetsPanel;
    private JPanel iposPanel;
    private JPanel tradesPanel;
    private JButton assetsTab, iposTab, tradesTab;
    private JPanel tabContent;

    private final AssetDAO assetDAO = new AssetDAO();
    private final IpoDAO ipoDAO = new IpoDAO();
    private final TradeDAO tradeDAO = new TradeDAO();
    private final TradeOrderDAO tradeOrderDAO = new TradeOrderDAO();

    public AdminAssetsViewUI() {
        setTitle("Fractional. - Admin: Assets & Trades");
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
        dragBar.addMouseListener(new MouseAdapter() { public void mousePressed(MouseEvent e) { dragPoint = e.getPoint(); } });
        dragBar.addMouseMotionListener(new MouseAdapter() {
            public void mouseDragged(MouseEvent e) {
                setLocation(getLocation().x + e.getX() - dragPoint.x, getLocation().y + e.getY() - dragPoint.y);
            }
        });

        // --- TOP NAV ---
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
        JLabel roleLabel = new JLabel("PLATFORM ADMIN");
        roleLabel.setForeground(pastelBlue);
        roleLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
        JLabel profileLabel = new JLabel(LoginManager.getCurrentUser() != null ? LoginManager.getCurrentUser().getName().toUpperCase().split(" ")[0] + " " + (LoginManager.getCurrentUser().getName().split(" ").length > 1 ? String.valueOf(LoginManager.getCurrentUser().getName().split(" ")[LoginManager.getCurrentUser().getName().split(" ").length-1].charAt(0)).toUpperCase() + "." : "") : "ADMIN");
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
        JLabel pageTitle = new JLabel("ASSETS, IPOs & TRADES");
        pageTitle.setForeground(textPrimary);
        pageTitle.setFont(new Font("SansSerif", Font.BOLD, 22));
        pageTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Summary stats
        JPanel statsRow = new JPanel(new GridLayout(1, 3, 20, 0));
        statsRow.setOpaque(false);
        statsRow.setMaximumSize(new Dimension(1350, 100));
        statsRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel assetStatCard = createStatCard("TOTAL ASSETS", "—", pastelBlue);
        JPanel ipoStatCard = createStatCard("ACTIVE IPOs", "—", pastelGreen);
        JPanel tradeStatCard = createStatCard("TOTAL TRADES", "—", textPrimary);
        statsRow.add(assetStatCard);
        statsRow.add(ipoStatCard);
        statsRow.add(tradeStatCard);

        // Tabs
        JPanel tabBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabBar.setOpaque(false);
        tabBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        tabBar.setMaximumSize(new Dimension(1350, 50));

        assetsTab = createTabButton("ASSETS", true);
        iposTab = createTabButton("IPOs / MARKET", false);
        tradesTab = createTabButton("TRADES", false);

        assetsTab.addActionListener(e -> switchTab(0));
        iposTab.addActionListener(e -> switchTab(1));
        tradesTab.addActionListener(e -> switchTab(2));

        tabBar.add(assetsTab);
        tabBar.add(Box.createRigidArea(new Dimension(5, 0)));
        tabBar.add(iposTab);
        tabBar.add(Box.createRigidArea(new Dimension(5, 0)));
        tabBar.add(tradesTab);

        // Content card
        JPanel contentCard = createRoundedPanel();
        contentCard.setLayout(new BorderLayout());
        contentCard.setBorder(new EmptyBorder(25, 30, 30, 30));
        contentCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        assetsPanel = new JPanel();
        assetsPanel.setLayout(new BoxLayout(assetsPanel, BoxLayout.Y_AXIS));
        assetsPanel.setOpaque(false);

        iposPanel = new JPanel();
        iposPanel.setLayout(new BoxLayout(iposPanel, BoxLayout.Y_AXIS));
        iposPanel.setOpaque(false);

        tradesPanel = new JPanel();
        tradesPanel.setLayout(new BoxLayout(tradesPanel, BoxLayout.Y_AXIS));
        tradesPanel.setOpaque(false);

        tabContent = new JPanel(new CardLayout());
        tabContent.setOpaque(false);
        tabContent.add(buildScrollPane(assetsPanel), "ASSETS");
        tabContent.add(buildScrollPane(iposPanel), "IPOS");
        tabContent.add(buildScrollPane(tradesPanel), "TRADES");
        contentCard.add(tabContent, BorderLayout.CENTER);

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

        loadAllData(assetStatCard, ipoStatCard, tradeStatCard);
    }

    // --- DATA ---
    private void loadAllData(JPanel assetCard, JPanel ipoCard, JPanel tradeCard) {
        try {
            // Assets
            List<VerifiedAssetRow> assets = assetDAO.getVerifiedAssets();
            updateStatCardValue(assetCard, String.valueOf(assets.size()));
            assetsPanel.removeAll();
            assetsPanel.add(createRowHeader(new String[]{"ID", "NAME", "CATEGORY", "STORAGE LOCATION", "VERIFICATION STATUS"}));
            assetsPanel.add(Box.createRigidArea(new Dimension(0, 8)));
            for (VerifiedAssetRow a : assets) {
                assetsPanel.add(createGenericRow(new Object[]{
                    "#" + a.getAssetId(), a.getName(), a.getCategory(),
                    a.getStorageLocation(), a.getVerificationStatus()
                }, new Color[]{textMuted, textPrimary, textMuted, textMuted, pastelGreen}));
                assetsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
            }
            assetsPanel.revalidate();
            assetsPanel.repaint();

            // IPOs / Market rows
            List<MarketViewRow> marketRows = ipoDAO.getAllMarketRows();
            long activeCount = marketRows.size();
            updateStatCardValue(ipoCard, String.valueOf(activeCount));
            iposPanel.removeAll();
            iposPanel.add(createRowHeader(new String[]{"IPO ID", "ASSET NAME", "CATEGORY", "TOTAL UNITS", "PRICE / UNIT"}));
            iposPanel.add(Box.createRigidArea(new Dimension(0, 8)));
            for (MarketViewRow m : marketRows) {
                iposPanel.add(createGenericRow(new Object[]{
                    "#" + m.getIpoId(), m.getAssetName(), m.getCategory(),
                    String.format("%,d", m.getTotalUnits()),
                    String.format("$%.2f", m.getPricePerUnit())
                }, new Color[]{textMuted, textPrimary, textMuted, textPrimary, pastelGreen}));
                iposPanel.add(Box.createRigidArea(new Dimension(0, 5)));
            }
            iposPanel.revalidate();
            iposPanel.repaint();

            // Trades
            List<Trade> trades = tradeDAO.listTrades();
            updateStatCardValue(tradeCard, String.valueOf(trades.size()));
            tradesPanel.removeAll();
            tradesPanel.add(createRowHeader(new String[]{"TRADE ID", "DATE", "UNITS", "PRICE", "BUY ORDER → SELL ORDER"}));
            tradesPanel.add(Box.createRigidArea(new Dimension(0, 8)));
            for (Trade t : trades) {
                tradesPanel.add(createGenericRow(new Object[]{
                    "#" + t.getTradeId(),
                    t.getTradeDate() != null ? t.getTradeDate().toString() : "—",
                    String.valueOf(t.getTradeUnits()),
                    String.format("$%.2f", t.getTradePrice()),
                    "Order #" + t.getBuyOrderId() + " → #" + t.getSellOrderId()
                }, new Color[]{textMuted, textMuted, textPrimary, pastelGreen, pastelBlue}));
                tradesPanel.add(Box.createRigidArea(new Dimension(0, 5)));
            }
            tradesPanel.revalidate();
            tradesPanel.repaint();

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to load data: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // --- ROW BUILDERS ---
    private JPanel createRowHeader(String[] cols) {
        JPanel row = new JPanel(new GridLayout(1, cols.length));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(1350, 32));
        row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, borderSubtle));
        for (String col : cols) {
            JLabel lbl = new JLabel(col);
            lbl.setForeground(textMuted);
            lbl.setFont(new Font("SansSerif", Font.BOLD, 10));
            row.add(lbl);
        }
        return row;
    }

    private JPanel createGenericRow(Object[] values, Color[] colors) {
        JPanel row = new JPanel(new GridLayout(1, values.length));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(1350, 50));
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(35, 35, 35)),
            new EmptyBorder(12, 0, 12, 0)
        ));
        for (int i = 0; i < values.length; i++) {
            JLabel lbl = new JLabel(values[i].toString());
            lbl.setForeground(i < colors.length ? colors[i] : textPrimary);
            lbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
            row.add(lbl);
        }
        return row;
    }

    // --- HELPERS ---
    private void switchTab(int tab) {
        CardLayout cl = (CardLayout) tabContent.getLayout();
        String[] keys = {"ASSETS", "IPOS", "TRADES"};
        JButton[] btns = {assetsTab, iposTab, tradesTab};
        cl.show(tabContent, keys[tab]);
        for (int i = 0; i < btns.length; i++) styleTabButton(btns[i], i == tab);
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

    private JPanel createStatCard(String title, String value, Color valueColor) {
        JPanel card = createRoundedPanel();
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(20, 25, 20, 25));
        JLabel titleLbl = new JLabel(title);
        titleLbl.setForeground(textMuted);
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 11));
        JLabel valLbl = new JLabel(value);
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

    private void applyCustomScrollbar(JScrollPane sp) {
        sp.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() { this.thumbColor = borderSubtle; this.trackColor = cardGlass; }
            @Override protected JButton createDecreaseButton(int o) { return zeroBtn(); }
            @Override protected JButton createIncreaseButton(int o) { return zeroBtn(); }
            private JButton zeroBtn() { JButton b = new JButton(); b.setPreferredSize(new Dimension(0,0)); return b; }
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
            String[] navItems = {"Main Dashboard", "Manage IPOs", "Investors List ↗", "Asset Verification", "Settings"};
            navButtons = new JButton[navItems.length];
            for (int i = 0; i < navItems.length; i++) {
                navButtons[i] = new JButton(navItems[i]);
                navButtons[i].setFont(new Font("SansSerif", i == 1 ? Font.BOLD : Font.PLAIN, 15));
                navButtons[i].setForeground(i == 1 ? textPrimary : textMuted);
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
            navButtons[0].addActionListener(e -> { dispose(); new AdminDashUI().setVisible(true); });
            navButtons[1].addActionListener(e -> { dispose(); new AdminAssetsViewUI().setVisible(true); });
            navButtons[2].addActionListener(e -> { dispose(); new Investors_AdminListViewUI().setVisible(true); });
            navButtons[3].addActionListener(e -> { dispose(); new AdminAssetsViewUI().setVisible(true); });
            navButtons[4].addActionListener(e -> {
                int c = javax.swing.JOptionPane.showConfirmDialog(null, "Logout?", "Settings", javax.swing.JOptionPane.YES_NO_OPTION);
                if (c == javax.swing.JOptionPane.YES_OPTION) { edu.iiitd.dbms.auth.LoginManager.logout(); dispose(); new edu.iiitd.dbms.ui.login.logintesterUI().setVisible(true); }
            });
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
            int cd = (int)(25 * ((double) currentWidth / EXPANDED_WIDTH));
            path.lineTo(getWidth() - cd, 0);
            path.quadTo(getWidth(), getHeight() / 2.0, getWidth() - cd, getHeight());
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
        SwingUtilities.invokeLater(() -> new AdminAssetsViewUI().setVisible(true));
    }
}