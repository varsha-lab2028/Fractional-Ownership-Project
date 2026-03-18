package edu.iiitd.dbms.ui.investor;

import edu.iiitd.dbms.auth.LoginManager;

import edu.iiitd.dbms.data_access.InvestorDAO;
import edu.iiitd.dbms.data_access.PortfolioDAO;
import edu.iiitd.dbms.dto.AssetHoldingDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.util.List;

public class HoldingsViewUI extends JFrame {

    // Grayscale & Pastel Palette
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
    private JPanel holdingsListPanel;
    private JLabel totalValueLabel;
    private JLabel totalInvestedLabel;
    private JLabel totalPLLabel;

    // Data Access (Assuming Investor ID 1 for now)
    private int currentInvestorId = LoginManager.getCurrentUser() != null ? LoginManager.getCurrentUser().getLinkedId() : 1;
    private final InvestorDAO investorDAO = new InvestorDAO();
    private final PortfolioDAO portfolioDAO = new PortfolioDAO();

    public HoldingsViewUI() {
        setTitle("Fractional. - Asset Holdings");
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
            public void mouseEntered(MouseEvent e) { closeBtn.setForeground(textPrimary); }
            public void mouseExited(MouseEvent e) { closeBtn.setForeground(textMuted); }
        });
        dragBar.add(closeBtn, BorderLayout.EAST);
        dragBar.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { dragPoint = e.getPoint(); }
        });
        dragBar.addMouseMotionListener(new MouseAdapter() {
            public void mouseDragged(MouseEvent e) { setLocation(getLocation().x + e.getX() - dragPoint.x, getLocation().y + e.getY() - dragPoint.y); }
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
        JLabel profileLabel = new JLabel(LoginManager.getCurrentUser() != null ? LoginManager.getCurrentUser().getName().toUpperCase().split(" ")[0] + " " + (LoginManager.getCurrentUser().getName().split(" ").length > 1 ? String.valueOf(LoginManager.getCurrentUser().getName().split(" ")[LoginManager.getCurrentUser().getName().split(" ").length-1].charAt(0)).toUpperCase() + "." : "") : "INVESTOR");
        profileLabel.setForeground(textPrimary);
        profileLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        profilePanel.add(roleLabel);
        profilePanel.add(profileLabel);

        topBar.add(searchContainer, BorderLayout.WEST);
        topBar.add(profilePanel, BorderLayout.EAST);

        // --- SIDEBAR ---
        sidebar = new GrainySidebar();

        // --- MAIN CONTENT AREA ---
        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setOpaque(false);
        mainContent.setBorder(new EmptyBorder(20, 40, 40, 40));

        // 1. TOP SECTION: PORTFOLIO SUMMARY CARDS
        JPanel summaryGrid = new JPanel(new GridLayout(1, 3, 20, 0));
        summaryGrid.setOpaque(false);
        summaryGrid.setMaximumSize(new Dimension(1350, 120));

        JPanel cvCard = createSummaryCard("CURRENT PORTFOLIO VALUE");
        totalValueLabel = new JLabel("$0.00");
        styleMetricLabel(totalValueLabel, textPrimary);
        cvCard.add(totalValueLabel, BorderLayout.SOUTH);

        JPanel tiCard = createSummaryCard("TOTAL CAPITAL INVESTED");
        totalInvestedLabel = new JLabel("$0.00");
        styleMetricLabel(totalInvestedLabel, textMuted);
        tiCard.add(totalInvestedLabel, BorderLayout.SOUTH);

        JPanel plCard = createSummaryCard("ALL-TIME RETURN");
        totalPLLabel = new JLabel("0.00%");
        styleMetricLabel(totalPLLabel, textPrimary);
        plCard.add(totalPLLabel, BorderLayout.SOUTH);

        summaryGrid.add(cvCard);
        summaryGrid.add(tiCard);
        summaryGrid.add(plCard);

        // 2. BOTTOM SECTION: ASSET HOLDINGS LIST
        JPanel listCard = createRoundedPanel();
        listCard.setLayout(new BorderLayout());
        listCard.setBorder(new EmptyBorder(30, 30, 30, 30));

        JLabel listTitle = new JLabel("YOUR FRACTIONAL POSSESSIONS");
        listTitle.setForeground(textPrimary);
        listTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        listCard.add(listTitle, BorderLayout.NORTH);

        holdingsListPanel = new JPanel();
        holdingsListPanel.setLayout(new BoxLayout(holdingsListPanel, BoxLayout.Y_AXIS));
        holdingsListPanel.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(holdingsListPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getViewport().setBackground(cardGlass);
        applyCustomScrollbar(scrollPane);

        listCard.add(scrollPane, BorderLayout.CENTER);

        // Assembly
        mainContent.add(summaryGrid);
        mainContent.add(Box.createRigidArea(new Dimension(0, 30)));
        mainContent.add(listCard);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(topBar, BorderLayout.NORTH);
        wrapper.add(mainContent, BorderLayout.CENTER);

        add(dragBar, BorderLayout.NORTH);
        add(sidebar, BorderLayout.WEST);
        add(wrapper, BorderLayout.CENTER);

        loadHoldingsData();
    }

    // --- DATA LOADING ---
    private void loadHoldingsData() {
        try {
            // Fetch Aggregates
            double totalInvested = investorDAO.getTotalInvestedAmount(currentInvestorId);
            double currentValuation = investorDAO.getCurrentPortfolioValue(currentInvestorId);
            double totalPL = totalInvested == 0 ? 0 : ((currentValuation - totalInvested) / totalInvested) * 100;

            totalInvestedLabel.setText(String.format("$%,.2f", totalInvested));
            totalValueLabel.setText(String.format("$%,.2f", currentValuation));
            
            boolean isPositiveTotal = totalPL >= 0;
            totalPLLabel.setText(String.format("%s%.2f%%", isPositiveTotal ? "+" : "", totalPL));
            totalPLLabel.setForeground(isPositiveTotal ? pastelGreen : pastelRed);

            // Fetch Individual Assets
            List<AssetHoldingDTO> holdings = portfolioDAO.getInvestorHoldings(currentInvestorId);
            holdingsListPanel.removeAll();

            if (holdings.isEmpty()) {
                JLabel empty = new JLabel("You currently hold no assets.");
                empty.setForeground(textMuted);
                empty.setFont(new Font("SansSerif", Font.PLAIN, 14));
                holdingsListPanel.add(empty);
            } else {
                for (AssetHoldingDTO holding : holdings) {
                    holdingsListPanel.add(createExpandableHoldingRow(holding));
                    holdingsListPanel.add(Box.createRigidArea(new Dimension(0, 15)));
                }
            }
            holdingsListPanel.revalidate();
            holdingsListPanel.repaint();

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to load holdings.", "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // --- AESTHETIC & EXPANDABLE ROW ---
    private JPanel createExpandableHoldingRow(AssetHoldingDTO holding) {
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setOpaque(false);
        wrapper.setMaximumSize(new Dimension(1350, 150));

        // Top Visible Row
        JPanel mainRow = new JPanel(new BorderLayout());
        mainRow.setOpaque(false);
        mainRow.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, borderSubtle),
            new EmptyBorder(15, 10, 15, 10)
        ));

        // Left Col (Name/Category)
        JPanel leftCol = new JPanel(new GridLayout(2, 1, 0, 5));
        leftCol.setOpaque(false);
        JLabel nameLabel = new JLabel(holding.getAssetName());
        nameLabel.setForeground(textPrimary);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        JLabel catLabel = new JLabel(holding.getCategory() + "  |  " + holding.getUnitsHeld() + " Units");
        catLabel.setForeground(textMuted);
        catLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        leftCol.add(nameLabel); leftCol.add(catLabel);

        // Center Col (Values)
        JPanel centerCol = new JPanel(new GridLayout(2, 1, 0, 5));
        centerCol.setOpaque(false);
        JLabel valLabel = new JLabel(String.format("$%,.2f", holding.getCurrentValue()), SwingConstants.RIGHT);
        valLabel.setForeground(textPrimary);
        valLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        
        double plPercent = holding.getProfitLossPercentage();
        boolean isPositive = plPercent >= 0;
        JLabel plLabel = new JLabel(String.format("%s%.2f%%", isPositive ? "+" : "", plPercent), SwingConstants.RIGHT);
        plLabel.setForeground(isPositive ? pastelGreen : pastelRed);
        plLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        centerCol.add(valLabel); centerCol.add(plLabel);

        // Right Col (Expand Button)
        JPanel rightCol = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        rightCol.setOpaque(false);
        JButton expandBtn = new JButton("Details ▼");
        styleActionButton(expandBtn, cardGlass, pastelBlue, true);
        rightCol.add(expandBtn);

        mainRow.add(leftCol, BorderLayout.WEST);
        mainRow.add(centerCol, BorderLayout.CENTER);
        mainRow.add(rightCol, BorderLayout.EAST);

        // Expandable Details Panel (Hidden by default)
        JPanel detailsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 40, 15));
        detailsPanel.setBackground(new Color(20, 20, 20)); // Slightly darker to show depth
        detailsPanel.setVisible(false);
        detailsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        detailsPanel.add(createDetailItem("Capital Invested:", String.format("$%,.2f", holding.getTotalInvested())));
        detailsPanel.add(createDetailItem("Avg. Buy Price:", String.format("$%,.2f", holding.getAverageBuyPrice())));
        
        // Expand/Collapse Logic
        expandBtn.addActionListener(e -> {
            boolean isVisible = detailsPanel.isVisible();
            detailsPanel.setVisible(!isVisible);
            expandBtn.setText(!isVisible ? "Hide ▲" : "Details ▼");
            wrapper.setMaximumSize(new Dimension(1350, !isVisible ? 220 : 150));
            wrapper.revalidate();
        });

        wrapper.add(mainRow);
        wrapper.add(detailsPanel);
        return wrapper;
    }

    private JPanel createDetailItem(String label, String value) {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 2));
        p.setOpaque(false);
        JLabel l1 = new JLabel(label);
        l1.setForeground(textMuted);
        l1.setFont(new Font("SansSerif", Font.PLAIN, 11));
        JLabel l2 = new JLabel(value);
        l2.setForeground(textPrimary);
        l2.setFont(new Font("SansSerif", Font.BOLD, 13));
        p.add(l1); p.add(l2);
        return p;
    }

    private JPanel createSummaryCard(String title) {
        JPanel panel = createRoundedPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(new EmptyBorder(25, 25, 25, 25));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(textMuted);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        panel.add(titleLabel, BorderLayout.NORTH);
        return panel;
    }

    private void styleMetricLabel(JLabel label, Color fg) {
        label.setForeground(fg);
        label.setFont(new Font("SansSerif", Font.BOLD, 32));
    }

    private void styleActionButton(JButton btn, Color bg, Color fg, boolean isOutline) {
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        if (isOutline) {
            btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderSubtle, 1),
                new EmptyBorder(8, 15, 8, 15)
            ));
        } else {
            btn.setBorder(new EmptyBorder(8, 15, 8, 15));
        }
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
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
            private JButton createZeroButton() { JButton jb = new JButton(); jb.setPreferredSize(new Dimension(0,0)); return jb; }
        });
    }

    // --- SIDEBAR CLASS ---
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
                navButtons[i].setFont(new Font("SansSerif", i == 3 ? Font.BOLD : Font.PLAIN, 15)); // Holdings bolded
                navButtons[i].setForeground(i == 3 ? textPrimary : textMuted);
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
            int w = getWidth(), h = getHeight();
            GeneralPath path = new GeneralPath();
            path.moveTo(0, 0);
            int curveDepth = (int)(25 * ((double)currentWidth / EXPANDED_WIDTH));
            path.lineTo(w - curveDepth, 0);
            path.quadTo(w, h / 2.0, w - curveDepth, h);
            path.lineTo(0, h);
            path.closePath();
            g2.setColor(new Color(30, 30, 30, 150)); 
            g2.fill(path);
            g2.setClip(path);
            g2.drawImage(noiseOverlay, 0, 0, null);
            g2.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new HoldingsViewUI().setVisible(true));
    }
}