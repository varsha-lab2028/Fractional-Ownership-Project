package edu.iiitd.dbms.ui.investor;

import edu.iiitd.dbms.data_access.InvestorDAO;
import edu.iiitd.dbms.data_access.WalletDAO;
import edu.iiitd.dbms.dto.WalletTransactionDTO;
import edu.iiitd.dbms.service.MarketService;
import edu.iiitd.dbms.dto.InvestorMarketView.MarketViewRow;
import edu.iiitd.dbms.ui.components.CompactChartPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.util.List;

public class InvestorDashUI extends JFrame {

    private final Color bgAbsoluteDark = new Color(14, 14, 14); 
    private final Color cardGlass = new Color(26, 26, 26);      
    private final Color borderSubtle = new Color(45, 45, 45);   
    private final Color textPrimary = new Color(250, 250, 250); 
    private final Color textMuted = new Color(150, 150, 150);   
    private final Color pastelGreen = new Color(119, 221, 119); 
    private final Color pastelRed = new Color(255, 105, 97);

    private Point dragPoint;
    private GrainySidebar sidebar;

    // Dynamic UI Elements
    private JLabel plAmount;
    private JLabel plPercentage;
    private JLabel wallAmt;
    private JPanel activityCard;
    private JPanel ipoListPanel;

    // Data Access (Assuming Investor ID 1 for now)
    private final int currentInvestorId = 1;
    private final InvestorDAO investorDAO = new InvestorDAO();
    private final WalletDAO walletDAO = new WalletDAO();
    private final MarketService marketService = new MarketService();

    public InvestorDashUI() {
        setTitle("Fractional. - Dashboard");
        setSize(1350, 850);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setUndecorated(true);
        setLayout(new BorderLayout());
        getContentPane().setBackground(bgAbsoluteDark);

        // --- CUSTOM DRAG BAR ---
        JPanel dragBar = new JPanel(new BorderLayout());
        dragBar.setBackground(bgAbsoluteDark);
        dragBar.setPreferredSize(new Dimension(1350, 30));
        
        JLabel closeBtn = new JLabel("  ✕  ");
        closeBtn.setForeground(textMuted);
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeBtn.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { System.exit(0); }
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
        
        AestheticSearchBar searchField = new AestheticSearchBar("🔍 Search assets, IPOs, or users...");
        searchField.setPreferredSize(new Dimension(350, 40));
        searchContainer.add(searchField);

        JPanel profilePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5));
        profilePanel.setOpaque(false);
        JLabel roleLabel = new JLabel("INVESTOR");
        roleLabel.setForeground(textMuted);
        roleLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
        JLabel profileLabel = new JLabel("DISHA K.");
        profileLabel.setForeground(textPrimary);
        profileLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        profilePanel.add(roleLabel);
        profilePanel.add(profileLabel);

        topBar.add(searchContainer, BorderLayout.WEST);
        topBar.add(profilePanel, BorderLayout.EAST);

        sidebar = new GrainySidebar();

        // --- RIGHT PANEL (Stats & Activity) ---
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setOpaque(false);
        rightPanel.setPreferredSize(new Dimension(340, 0)); 
        rightPanel.setBorder(new EmptyBorder(0, 10, 30, 25));

        JPanel plCard = createRoundedPanel();
        plCard.setLayout(new BoxLayout(plCard, BoxLayout.Y_AXIS));
        plCard.setBorder(new EmptyBorder(30, 25, 30, 25));
        JLabel plTitle = new JLabel("PROFITS OR LOSS");
        plTitle.setForeground(textMuted);
        plTitle.setFont(new Font("SansSerif", Font.BOLD, 11));
        plAmount = new JLabel("$0.00");
        plAmount.setForeground(pastelGreen);
        plAmount.setFont(new Font("SansSerif", Font.BOLD, 28)); 
        plPercentage = new JLabel("0.0% All Time");
        plPercentage.setForeground(textMuted);
        plPercentage.setFont(new Font("SansSerif", Font.PLAIN, 14));
        plCard.add(plTitle); plCard.add(Box.createRigidArea(new Dimension(0, 15)));
        plCard.add(plAmount); plCard.add(Box.createRigidArea(new Dimension(0, 5)));
        plCard.add(plPercentage);

        JPanel walletCard = createRoundedPanel();
        walletCard.setLayout(new BorderLayout());
        walletCard.setBorder(new EmptyBorder(25, 25, 25, 25));
        walletCard.setMaximumSize(new Dimension(340, 90));
        JLabel wallTitle = new JLabel("Wallet (Immediate Funds)");
        wallTitle.setForeground(textMuted);
        wallTitle.setFont(new Font("SansSerif", Font.BOLD, 11));
        wallAmt = new JLabel("$0.00");
        wallAmt.setForeground(textPrimary);
        wallAmt.setFont(new Font("SansSerif", Font.BOLD, 22));
        walletCard.add(wallTitle, BorderLayout.NORTH);
        walletCard.add(wallAmt, BorderLayout.SOUTH);

        activityCard = createRoundedPanel();
        activityCard.setLayout(new BoxLayout(activityCard, BoxLayout.Y_AXIS));
        activityCard.setBorder(new EmptyBorder(25, 25, 25, 25));

        rightPanel.add(plCard);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        rightPanel.add(walletCard);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        rightPanel.add(activityCard); 

        // --- CENTER PANEL (Charts & IPOs) ---
        JPanel centerPanel = new JPanel(new BorderLayout(0, 20));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(0, 0, 30, 15));

        JPanel chartsGrid = new JPanel(new GridLayout(1, 2, 20, 0));
        chartsGrid.setOpaque(false);
        chartsGrid.setPreferredSize(new Dimension(0, 300));

        JPanel chartBox1 = createRoundedPanel();
        chartBox1.setLayout(new BorderLayout());
        chartBox1.setBorder(new EmptyBorder(15, 15, 15, 15));
        chartBox1.add(new CompactChartPanel(1, "PORTFOLIO GROWTH"), BorderLayout.CENTER);

        JPanel chartBox2 = createRoundedPanel();
        chartBox2.setLayout(new BorderLayout());
        chartBox2.setBorder(new EmptyBorder(15, 15, 15, 15));
        chartBox2.add(new CompactChartPanel(2, "ASSET ALLOCATION"), BorderLayout.CENTER);

        chartsGrid.add(chartBox1);
        chartsGrid.add(chartBox2);

        JPanel ipoContainer = createRoundedPanel();
        ipoContainer.setLayout(new BorderLayout());
        ipoContainer.setBorder(new EmptyBorder(20, 20, 20, 20));
        JLabel ipoTitle = new JLabel("TRENDING IPO OFFERINGS");
        ipoTitle.setForeground(textPrimary);
        ipoTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        ipoContainer.add(ipoTitle, BorderLayout.NORTH);

        ipoListPanel = new JPanel();
        ipoListPanel.setLayout(new BoxLayout(ipoListPanel, BoxLayout.Y_AXIS));
        ipoListPanel.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(ipoListPanel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getViewport().setBackground(cardGlass);
        applyCustomScrollbar(scrollPane);

        ipoContainer.add(scrollPane, BorderLayout.CENTER);
        centerPanel.add(chartsGrid, BorderLayout.NORTH);
        centerPanel.add(ipoContainer, BorderLayout.CENTER);

        add(dragBar, BorderLayout.NORTH);
        add(sidebar, BorderLayout.WEST);
        
        JPanel mainContent = new JPanel(new BorderLayout());
        mainContent.setOpaque(false);
        mainContent.add(topBar, BorderLayout.NORTH);
        mainContent.add(centerPanel, BorderLayout.CENTER);
        mainContent.add(rightPanel, BorderLayout.EAST);
        add(mainContent, BorderLayout.CENTER);

        loadDashboardData();
    }

    private void loadDashboardData() {
        try {
            double balance = investorDAO.getWalletBalance(currentInvestorId);
            wallAmt.setText(String.format("$%,.2f", balance));

            double totalInvested = investorDAO.getTotalInvestedAmount(currentInvestorId);
            double currentValue = investorDAO.getCurrentPortfolioValue(currentInvestorId);
            double profitLossAmount = currentValue - totalInvested;
            double profitLossPercentage = totalInvested == 0 ? 0 : (profitLossAmount / totalInvested) * 100;
            
            boolean isPositivePL = profitLossAmount >= 0;
            plAmount.setText(String.format("%s$%,.2f", isPositivePL ? "+" : "", profitLossAmount));
            plAmount.setForeground(isPositivePL ? pastelGreen : pastelRed);
            plPercentage.setText(String.format("%s%.2f%% All Time", isPositivePL ? "▲ " : "▼ ", profitLossPercentage));
            plPercentage.setForeground(isPositivePL ? pastelGreen : pastelRed);

            activityCard.removeAll();
            JLabel actTitle = new JLabel("RECENT ACTIVITY");
            actTitle.setForeground(textMuted);
            actTitle.setFont(new Font("SansSerif", Font.BOLD, 11));
            activityCard.add(actTitle);
            activityCard.add(Box.createRigidArea(new Dimension(0, 15)));

            List<WalletTransactionDTO> transactions = walletDAO.getTransactionHistory(currentInvestorId);
            int count = 0;
            for (WalletTransactionDTO tx : transactions) {
                if (count >= 4) break;
                activityCard.add(createActivityRow(tx.getTransactionType().replace("_", " "), tx.getAmount()));
                activityCard.add(Box.createRigidArea(new Dimension(0, 10)));
                count++;
            }

            ipoListPanel.removeAll();
            List<MarketViewRow> activeMarkets = marketService.getAllMarketRows();
            for (MarketViewRow row : activeMarkets) {
                ipoListPanel.add(createIpoRow(row.getAssetName(), String.format("%,d Units", row.getTotalUnits()), String.format("$%.2f / unit", row.getPricePerUnit())));
                ipoListPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            }

            activityCard.revalidate(); activityCard.repaint();
            ipoListPanel.revalidate(); ipoListPanel.repaint();

        } catch (Exception e) { e.printStackTrace(); }
    }

    private JPanel createActivityRow(String action, double amount) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        JLabel actLabel = new JLabel(action);
        actLabel.setForeground(textPrimary);
        actLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        boolean isPos = amount >= 0;
        JLabel amtLabel = new JLabel(String.format("%s$%,.2f", isPos ? "+" : "", amount));
        amtLabel.setForeground(isPos ? pastelGreen : textMuted);
        amtLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        row.add(actLabel, BorderLayout.WEST);
        row.add(amtLabel, BorderLayout.EAST);
        return row;
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

    private JPanel createIpoRow(String name, String units, String price) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(800, 60));
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, borderSubtle),
            new EmptyBorder(10, 5, 10, 5)
        ));
        JPanel leftCol = new JPanel(new GridLayout(2, 1));
        leftCol.setOpaque(false);
        JLabel nLabel = new JLabel(name); nLabel.setForeground(textPrimary);
        JLabel uLabel = new JLabel("Issued: " + units); uLabel.setForeground(textMuted);
        leftCol.add(nLabel); leftCol.add(uLabel);
        JPanel rightCol = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightCol.setOpaque(false);
        JLabel pLabel = new JLabel(price); pLabel.setForeground(textPrimary);
        JButton buyBtn = new JButton("Invest");
        buyBtn.setBackground(textPrimary); buyBtn.setForeground(bgAbsoluteDark);
        buyBtn.setOpaque(true); buyBtn.setBorderPainted(false);
        rightCol.add(pLabel); rightCol.add(Box.createRigidArea(new Dimension(15, 0))); rightCol.add(buyBtn);
        row.add(leftCol, BorderLayout.WEST); row.add(rightCol, BorderLayout.EAST);
        return row;
    }

    private void applyCustomScrollbar(JScrollPane scrollPane) {
        scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() { this.thumbColor = borderSubtle; this.trackColor = cardGlass; }
            @Override protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
            @Override protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }
            private JButton createZeroButton() { JButton jb = new JButton(); jb.setPreferredSize(new Dimension(0,0)); return jb; }
        });
    }

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

            String[] navItems = {"Main Dashboard", "Market View", "Wallet", "Holdings", "Profile Settings"};
            navButtons = new JButton[navItems.length];
            
            for (int i = 0; i < navItems.length; i++) {
                navButtons[i] = new JButton(navItems[i]);
                navButtons[i].setFont(new Font("SansSerif", i == 0 ? Font.BOLD : Font.PLAIN, 15));
                navButtons[i].setForeground(i == 0 ? textPrimary : textMuted);
                navButtons[i].setContentAreaFilled(false);
                navButtons[i].setBorderPainted(false);
                navButtons[i].setFocusPainted(false);
                navButtons[i].setHorizontalAlignment(SwingConstants.LEFT);
                navButtons[i].setCursor(new Cursor(Cursor.HAND_CURSOR));
                navButtons[i].setVisible(false); 
                navButtons[i].setBounds(20, 100 + (i * 50), 200, 40);
                add(navButtons[i]);
            }

            // ==========================================
            // ADDED: SIDEBAR ROUTING LOGIC
            // ==========================================
            navButtons[0].addActionListener(e -> { 
                dispose(); 
                new InvestorDashUI().setVisible(true); 
            });
            navButtons[1].addActionListener(e -> { 
                dispose(); 
                new MarketViewUI().setVisible(true); 
            });
            navButtons[2].addActionListener(e -> { 
                dispose(); 
                new WalletViewUI().setVisible(true); 
            });
            navButtons[3].addActionListener(e -> { 
                dispose(); 
                new HoldingsViewUI().setVisible(true); 
            });
            navButtons[4].addActionListener(e -> { 
                dispose(); 
                new ProfileSettingsUI().setVisible(true); 
            });
        }

        public void toggleSidebar() {
            if (animator != null && animator.isRunning()) return;
            int targetWidth = isExpanded ? 0 : EXPANDED_WIDTH;
            int step = isExpanded ? -15 : 15; 
            if (isExpanded) {
                logoLabel.setVisible(false);
                for (JButton btn : navButtons) btn.setVisible(false);
            }
            animator = new Timer(10, e -> {
                currentWidth += step;
                if (currentWidth < 0) currentWidth = 0;
                if (currentWidth > EXPANDED_WIDTH) currentWidth = EXPANDED_WIDTH;
                setPreferredSize(new Dimension(currentWidth, 0));
                revalidate(); repaint();
                if (currentWidth == targetWidth) {
                    isExpanded = !isExpanded;
                    ((Timer) e.getSource()).stop();
                    if (isExpanded) {
                        logoLabel.setVisible(true);
                        for (JButton btn : navButtons) btn.setVisible(true);
                    }
                }
            });
            animator.start();
        }

        private void generateNoiseTexture() {
            noiseOverlay = new BufferedImage(EXPANDED_WIDTH, 1200, BufferedImage.TYPE_INT_ARGB);
            for (int x = 0; x < EXPANDED_WIDTH; x++) {
                for (int y = 0; y < 1200; y++) {
                    if (Math.random() > 0.85) {
                        int alpha = (int)(Math.random() * 10); 
                        noiseOverlay.setRGB(x, y, new Color(255, 255, 255, alpha).getRGB());
                    }
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (currentWidth == 0) return;
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            GeneralPath path = new GeneralPath();
            path.moveTo(0, 0);
            int curveDepth = (int)(25 * ((double)currentWidth / EXPANDED_WIDTH));
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

    class AestheticSearchBar extends JTextField {
        private String placeholder;
        public AestheticSearchBar(String placeholder) {
            this.placeholder = placeholder;
            setOpaque(false);
            setForeground(textMuted);
            setCaretColor(textPrimary);
            setBorder(new EmptyBorder(5, 20, 5, 20));
            setFont(new Font("SansSerif", Font.PLAIN, 14));
            setText(placeholder);
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(cardGlass); 
            g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20); 
            super.paintComponent(g);
            g2.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new InvestorDashUI().setVisible(true));
    }
}