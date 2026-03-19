package edu.iiitd.dbms.ui.investor;

import edu.iiitd.dbms.auth.LoginManager;

import edu.iiitd.dbms.data_access.InvestorDAO;
import edu.iiitd.dbms.data_access.OwnershipDAO;
import edu.iiitd.dbms.data_access.TradeOrderDAO;
import edu.iiitd.dbms.data_access.AssetDAO;
import edu.iiitd.dbms.domain.Ownership;
import edu.iiitd.dbms.domain.TradeOrder;
import edu.iiitd.dbms.dto.InvestorMarketView.VerifiedAssetRow;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SellOrderUI extends JFrame {

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

    private JComboBox<String> holdingCombo;
    private JTextField unitsField;
    private JTextField priceField;
    private JLabel estimatedTotalLabel;
    private JLabel unitsAvailableLabel;
    private JPanel holdingsListPanel;

    private int currentInvestorId = LoginManager.getCurrentUser() != null ? LoginManager.getCurrentUser().getLinkedId() : 1;
    private final OwnershipDAO ownershipDAO = new OwnershipDAO();
    private final TradeOrderDAO tradeOrderDAO = new TradeOrderDAO();
    private final InvestorDAO investorDAO = new InvestorDAO();
    private final AssetDAO assetDAO = new AssetDAO();
    private List<Ownership> holdings;
    private List<VerifiedAssetRow> verifiedAssets;

    private String getDisplayName() {
        if (LoginManager.getCurrentUser() == null) return "INVESTOR";
        String full = LoginManager.getCurrentUser().getName();
        if (full == null || full.isBlank()) return "INVESTOR";
        String[] p = full.trim().split("\s+");
        return p[0].toUpperCase() + (p.length > 1 ? " " + Character.toUpperCase(p[p.length-1].charAt(0)) + "." : "");
    }

    public SellOrderUI() {
        setTitle("Fractional. - Place Sell Order");
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

        // --- MAIN LAYOUT ---
        JPanel mainContent = new JPanel(new BorderLayout(25, 0));
        mainContent.setOpaque(false);
        mainContent.setBorder(new EmptyBorder(20, 40, 40, 40));

        // ===== LEFT: SELL FORM =====
        JPanel formColumn = new JPanel();
        formColumn.setLayout(new BoxLayout(formColumn, BoxLayout.Y_AXIS));
        formColumn.setOpaque(false);
        formColumn.setPreferredSize(new Dimension(480, 0));

        JLabel pageTitle = new JLabel("PLACE SELL ORDER");
        pageTitle.setForeground(pastelRed);
        pageTitle.setFont(new Font("SansSerif", Font.BOLD, 22));
        pageTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel pageSubtitle = new JLabel("List your fractional units for sale on the secondary market");
        pageSubtitle.setForeground(textMuted);
        pageSubtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        pageSubtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel formCard = createRoundedPanel();
        formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));
        formCard.setBorder(new EmptyBorder(35, 35, 35, 35));
        formCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Units available hint
        unitsAvailableLabel = new JLabel("Select a holding to see available units");
        unitsAvailableLabel.setForeground(pastelBlue);
        unitsAvailableLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        unitsAvailableLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Holding selector
        holdingCombo = new JComboBox<>();
        holdingCombo.setBackground(bgAbsoluteDark);
        holdingCombo.setForeground(textPrimary);
        holdingCombo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        holdingCombo.setMaximumSize(new Dimension(500, 40));
        holdingCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        holdingCombo.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(borderSubtle, 1), new EmptyBorder(5, 10, 5, 10)));
        holdingCombo.addActionListener(e -> updateUnitsHint());

        // Units field
        unitsField = createInputField();
        unitsField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) { updateTotal(); }
        });

        // Price field
        priceField = createInputField();
        priceField.setText("0.00");
        priceField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) { updateTotal(); }
        });

        // Estimated total
        JPanel totalSection = new JPanel(new BorderLayout());
        totalSection.setOpaque(false);
        totalSection.setMaximumSize(new Dimension(500, 60));
        totalSection.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 1, 0, borderSubtle),
            new EmptyBorder(15, 0, 15, 0)
        ));
        JLabel totalTitle = new JLabel("ESTIMATED PROCEEDS");
        totalTitle.setForeground(textMuted);
        totalTitle.setFont(new Font("SansSerif", Font.BOLD, 11));
        estimatedTotalLabel = new JLabel("$0.00");
        estimatedTotalLabel.setForeground(textPrimary);
        estimatedTotalLabel.setFont(new Font("SansSerif", Font.BOLD, 26));
        totalSection.add(totalTitle, BorderLayout.NORTH);
        totalSection.add(estimatedTotalLabel, BorderLayout.SOUTH);

        // Warning notice
        JPanel warningBox = new JPanel(new BorderLayout());
        warningBox.setBackground(new Color(35, 20, 20));
        warningBox.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 30, 30), 1),
            new EmptyBorder(12, 15, 12, 15)
        ));
        warningBox.setMaximumSize(new Dimension(500, 60));
        JLabel warningLabel = new JLabel("Sells are subject to trigger validation. Overselling will be blocked.");
        warningLabel.setForeground(pastelRed);
        warningLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        warningBox.add(warningLabel, BorderLayout.CENTER);
        warningBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Submit button
        JButton submitBtn = new JButton("CONFIRM SELL ORDER");
        submitBtn.setBackground(pastelRed);
        submitBtn.setForeground(bgAbsoluteDark);
        submitBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        submitBtn.setFocusPainted(false);
        submitBtn.setOpaque(true);
        submitBtn.setBorderPainted(false);
        submitBtn.setMaximumSize(new Dimension(500, 48));
        submitBtn.setBorder(new EmptyBorder(14, 0, 14, 0));
        submitBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        submitBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        submitBtn.addActionListener(e -> handleSubmit());

        // Back button
        JButton backBtn = new JButton("← Back to Holdings");
        backBtn.setContentAreaFilled(false);
        backBtn.setBorderPainted(false);
        backBtn.setForeground(textMuted);
        backBtn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        backBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        backBtn.addActionListener(e -> { dispose(); new HoldingsViewUI_updated().setVisible(true); });

        formCard.add(makeLabel("SELECT HOLDING TO SELL"));
        formCard.add(Box.createRigidArea(new Dimension(0, 6)));
        formCard.add(holdingCombo);
        formCard.add(Box.createRigidArea(new Dimension(0, 4)));
        formCard.add(unitsAvailableLabel);
        formCard.add(Box.createRigidArea(new Dimension(0, 20)));
        formCard.add(makeLabel("QUANTITY TO SELL (UNITS)"));
        formCard.add(Box.createRigidArea(new Dimension(0, 6)));
        formCard.add(unitsField);
        formCard.add(Box.createRigidArea(new Dimension(0, 20)));
        formCard.add(makeLabel("ASKING PRICE PER UNIT ($)"));
        formCard.add(Box.createRigidArea(new Dimension(0, 6)));
        formCard.add(priceField);
        formCard.add(Box.createRigidArea(new Dimension(0, 25)));
        formCard.add(totalSection);
        formCard.add(Box.createRigidArea(new Dimension(0, 15)));
        formCard.add(warningBox);
        formCard.add(Box.createRigidArea(new Dimension(0, 20)));
        formCard.add(submitBtn);
        formCard.add(Box.createRigidArea(new Dimension(0, 15)));
        formCard.add(backBtn);

        formColumn.add(pageTitle);
        formColumn.add(Box.createRigidArea(new Dimension(0, 6)));
        formColumn.add(pageSubtitle);
        formColumn.add(Box.createRigidArea(new Dimension(0, 20)));
        formColumn.add(formCard);

        // ===== RIGHT: CURRENT HOLDINGS =====
        JPanel rightColumn = new JPanel(new BorderLayout());
        rightColumn.setOpaque(false);

        JPanel holdingsCard = createRoundedPanel();
        holdingsCard.setLayout(new BorderLayout());
        holdingsCard.setBorder(new EmptyBorder(25, 25, 25, 25));

        JLabel holdingsCardTitle = new JLabel("YOUR CURRENT HOLDINGS");
        holdingsCardTitle.setForeground(textPrimary);
        holdingsCardTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        holdingsCard.add(holdingsCardTitle, BorderLayout.NORTH);

        holdingsListPanel = new JPanel();
        holdingsListPanel.setLayout(new BoxLayout(holdingsListPanel, BoxLayout.Y_AXIS));
        holdingsListPanel.setOpaque(false);

        JScrollPane holdingsScroll = new JScrollPane(holdingsListPanel);
        holdingsScroll.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));
        holdingsScroll.setOpaque(false);
        holdingsScroll.getViewport().setOpaque(false);
        holdingsScroll.getViewport().setBackground(cardGlass);
        applyCustomScrollbar(holdingsScroll);
        holdingsCard.add(holdingsScroll, BorderLayout.CENTER);
        rightColumn.add(holdingsCard, BorderLayout.CENTER);

        mainContent.add(formColumn, BorderLayout.WEST);
        mainContent.add(rightColumn, BorderLayout.CENTER);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(topBar, BorderLayout.NORTH);
        wrapper.add(mainContent, BorderLayout.CENTER);

        add(dragBar, BorderLayout.NORTH);
        add(sidebar, BorderLayout.WEST);
        add(wrapper, BorderLayout.CENTER);

        loadData();
    }

    // --- DATA ---
    private void loadData() {
        try {
            holdings = ownershipDAO.listHoldings(currentInvestorId);
            verifiedAssets = assetDAO.getVerifiedAssets();

            holdingCombo.removeAllItems();
            holdingsListPanel.removeAll();
            holdingsListPanel.add(Box.createRigidArea(new Dimension(0, 5)));

            if (holdings.isEmpty()) {
                holdingCombo.addItem("No holdings available");
                JLabel empty = new JLabel("You hold no assets to sell.");
                empty.setForeground(textMuted);
                empty.setFont(new Font("SansSerif", Font.PLAIN, 14));
                holdingsListPanel.add(empty);
            } else {
                for (Ownership o : holdings) {
                    String assetName = getAssetName(o.getAssetId());
                    holdingCombo.addItem("[" + o.getAssetId() + "] " + assetName + " (" + o.getUnitsHeld() + " units)");
                    holdingsListPanel.add(createHoldingInfoRow(o, assetName));
                    holdingsListPanel.add(Box.createRigidArea(new Dimension(0, 8)));
                }
                updateUnitsHint();
            }

            holdingsListPanel.revalidate();
            holdingsListPanel.repaint();

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to load holdings: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String getAssetName(int assetId) {
        if (verifiedAssets != null) {
            for (VerifiedAssetRow a : verifiedAssets) {
                if (a.getAssetId() == assetId) return a.getName();
            }
        }
        return "Asset " + assetId;
    }

    private void updateUnitsHint() {
        int idx = holdingCombo.getSelectedIndex();
        if (holdings != null && idx >= 0 && idx < holdings.size()) {
            int available = holdings.get(idx).getUnitsHeld();
            unitsAvailableLabel.setText("Available: " + available + " units");
        }
    }

    private void updateTotal() {
        try {
            int units = Integer.parseInt(unitsField.getText().trim());
            double price = Double.parseDouble(priceField.getText().trim());
            estimatedTotalLabel.setText(String.format("$%,.2f", units * price));
        } catch (NumberFormatException ex) {
            estimatedTotalLabel.setText("$—");
        }
    }

    private void handleSubmit() {
        try {
            int idx = holdingCombo.getSelectedIndex();
            if (holdings == null || holdings.isEmpty() || idx < 0) {
                JOptionPane.showMessageDialog(this, "No holdings available to sell.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Ownership holding = holdings.get(idx);
            int units = Integer.parseInt(unitsField.getText().trim());
            double price = Double.parseDouble(priceField.getText().trim());

            if (units <= 0) { JOptionPane.showMessageDialog(this, "Units must be greater than 0.", "Validation Error", JOptionPane.WARNING_MESSAGE); return; }
            if (price <= 0) { JOptionPane.showMessageDialog(this, "Price must be greater than 0.", "Validation Error", JOptionPane.WARNING_MESSAGE); return; }
            if (units > holding.getUnitsHeld()) {
                JOptionPane.showMessageDialog(this,
                    "Cannot sell " + units + " units. You only hold " + holding.getUnitsHeld() + " units of this asset.\n(The trigger will also block this.)",
                    "Insufficient Units", JOptionPane.WARNING_MESSAGE);
                return;
            }

            List<TradeOrder> existing = tradeOrderDAO.listOrders();
            int newId = existing.stream().mapToInt(TradeOrder::getOrderId).max().orElse(0) + 1;

            TradeOrder order = new TradeOrder(newId, currentInvestorId, holding.getAssetId(), "SELL", price, units, LocalDate.now(), "OPEN");
            tradeOrderDAO.insertOrder(order);

            JOptionPane.showMessageDialog(this,
                String.format("Sell order #%d placed successfully!\n%d units of Asset %d listed at $%.2f each.", newId, units, holding.getAssetId(), price),
                "Order Placed", JOptionPane.INFORMATION_MESSAGE);

            unitsField.setText("");
            priceField.setText("0.00");
            estimatedTotalLabel.setText("$0.00");
            loadData();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter valid numeric values.", "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to place order: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // --- HELPERS ---
    private JPanel createHoldingInfoRow(Ownership o, String assetName) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(800, 56));
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, borderSubtle),
            new EmptyBorder(10, 5, 10, 5)
        ));
        JPanel leftCol = new JPanel(new GridLayout(2, 1));
        leftCol.setOpaque(false);
        JLabel nameLabel = new JLabel(assetName);
        nameLabel.setForeground(textPrimary);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        JLabel subLabel = new JLabel("Asset ID: " + o.getAssetId());
        subLabel.setForeground(textMuted);
        subLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        leftCol.add(nameLabel);
        leftCol.add(subLabel);
        JLabel unitsLabel = new JLabel(o.getUnitsHeld() + " units");
        unitsLabel.setForeground(pastelGreen);
        unitsLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        row.add(leftCol, BorderLayout.WEST);
        row.add(unitsLabel, BorderLayout.EAST);
        return row;
    }

    private JTextField createInputField() {
        JTextField field = new JTextField();
        field.setMaximumSize(new Dimension(500, 42));
        field.setBackground(bgAbsoluteDark);
        field.setForeground(textPrimary);
        field.setCaretColor(textPrimary);
        field.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(borderSubtle, 1), new EmptyBorder(8, 15, 8, 15)));
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        return field;
    }

    private JLabel makeLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(textMuted);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
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
            String[] navItems = {"Main Dashboard", "Market View", "Wallet", "Holdings", "Profile Settings"};
            navButtons = new JButton[navItems.length];
            for (int i = 0; i < navItems.length; i++) {
                navButtons[i] = new JButton(navItems[i]);
                navButtons[i].setFont(new Font("SansSerif", i == 3 ? Font.BOLD : Font.PLAIN, 15));
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
            navButtons[0].addActionListener(e -> { SellOrderUI.this.dispose(); new InvestorDashUI().setVisible(true); });
            navButtons[1].addActionListener(e -> { SellOrderUI.this.dispose(); new MarketViewUI_updated().setVisible(true); });
            navButtons[2].addActionListener(e -> { SellOrderUI.this.dispose(); new WalletViewUI().setVisible(true); });
            navButtons[3].addActionListener(e -> { SellOrderUI.this.dispose(); new HoldingsViewUI_updated().setVisible(true); });
            navButtons[4].addActionListener(e -> { SellOrderUI.this.dispose(); new ProfileSettingsUI().setVisible(true); });
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
        SwingUtilities.invokeLater(() -> new SellOrderUI().setVisible(true));
    }
}