package edu.iiitd.dbms.ui.investor;

import edu.iiitd.dbms.data_access.OwnershipDAO;
import edu.iiitd.dbms.domain.Ownership;

import edu.iiitd.dbms.auth.LoginManager;
import edu.iiitd.dbms.data_access.AssetDAO;
import edu.iiitd.dbms.data_access.InvestorDAO;
import edu.iiitd.dbms.data_access.TradeOrderDAO;
import edu.iiitd.dbms.data_access.WalletTransactionDAO;
import edu.iiitd.dbms.dto.InvestorMarketView.VerifiedAssetRow;
import edu.iiitd.dbms.domain.TradeOrder;

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
import java.util.List;

public class BuyOrderUI extends JFrame {

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

    private JComboBox<String> assetCombo;
    private JTextField unitsField;
    private JTextField priceField;
    private JLabel estimatedTotalLabel;
    private JLabel walletBalanceLabel;
    private JLabel assetPriceHintLabel;
    private JPanel assetListPanel;
    private JComboBox<String> paymentSourceCombo;  // NEW: payment source

    private int currentInvestorId = LoginManager.getCurrentUser() != null
            ? LoginManager.getCurrentUser().getLinkedId() : 1;

    private final AssetDAO assetDAO              = new AssetDAO();
    private final InvestorDAO investorDAO        = new InvestorDAO();
    private final TradeOrderDAO tradeOrderDAO    = new TradeOrderDAO();
    private final WalletTransactionDAO walletDAO = new WalletTransactionDAO();
    private final OwnershipDAO ownershipDAO = new OwnershipDAO();
    private List<VerifiedAssetRow> verifiedAssets;

    private String getDisplayName() {
        if (LoginManager.getCurrentUser() == null) return "INVESTOR";
        String full = LoginManager.getCurrentUser().getName();
        if (full == null || full.isBlank()) return "INVESTOR";
        String[] p = full.trim().split("\\s+");
        return p[0].toUpperCase() + (p.length > 1 ? " " + Character.toUpperCase(p[p.length-1].charAt(0)) + "." : "");
    }

    public BuyOrderUI() {
        setTitle("Fractional. - Place Buy Order");
        setSize(1350, 850);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setUndecorated(true);
        setLayout(new BorderLayout());
        getContentPane().setBackground(bgAbsoluteDark);

        // Drag bar
        JPanel dragBar = new JPanel(new BorderLayout());
        dragBar.setBackground(bgAbsoluteDark);
        dragBar.setPreferredSize(new Dimension(1350, 30));
        JLabel closeBtn = new JLabel("  ✕  ");
        closeBtn.setForeground(textMuted);
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeBtn.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { System.exit(0); }
            public void mouseEntered(MouseEvent e) { closeBtn.setForeground(textPrimary); }
            public void mouseExited(MouseEvent e)  { closeBtn.setForeground(textMuted); }
        });
        dragBar.add(closeBtn, BorderLayout.EAST);
        dragBar.addMouseListener(new MouseAdapter() { public void mousePressed(MouseEvent e) { dragPoint = e.getPoint(); } });
        dragBar.addMouseMotionListener(new MouseAdapter() {
            public void mouseDragged(MouseEvent e) {
                setLocation(getLocation().x + e.getX() - dragPoint.x, getLocation().y + e.getY() - dragPoint.y);
            }
        });

        // Top nav
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
        JPanel profilePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5));
        profilePanel.setOpaque(false);
        JLabel roleLabel = new JLabel("INVESTOR");
        roleLabel.setForeground(textMuted);
        roleLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
        JLabel profileLabel = new JLabel(getDisplayName());
        profileLabel.setForeground(textPrimary);
        profileLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        profilePanel.add(roleLabel); profilePanel.add(profileLabel);
        topBar.add(searchContainer, BorderLayout.WEST);
        topBar.add(profilePanel, BorderLayout.EAST);

        sidebar = new GrainySidebar();

        // Main layout
        JPanel mainContent = new JPanel(new BorderLayout(25, 0));
        mainContent.setOpaque(false);
        mainContent.setBorder(new EmptyBorder(20, 40, 40, 40));

        // ===== LEFT: FORM =====
        JPanel formColumn = new JPanel();
        formColumn.setLayout(new BoxLayout(formColumn, BoxLayout.Y_AXIS));
        formColumn.setOpaque(false);
        formColumn.setPreferredSize(new Dimension(480, 0));

        JLabel pageTitle = new JLabel("PLACE BUY ORDER");
        pageTitle.setForeground(pastelGreen);
        pageTitle.setFont(new Font("SansSerif", Font.BOLD, 22));
        pageTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel pageSubtitle = new JLabel("Secondary market — acquire fractional asset units");
        pageSubtitle.setForeground(textMuted);
        pageSubtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        pageSubtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel formCard = createRoundedPanel();
        formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));
        formCard.setBorder(new EmptyBorder(35, 35, 35, 35));
        formCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Wallet balance
        JPanel balanceRow = new JPanel(new BorderLayout());
        balanceRow.setOpaque(false);
        balanceRow.setMaximumSize(new Dimension(500, 50));
        JLabel balanceTitle = new JLabel("AVAILABLE WALLET BALANCE");
        balanceTitle.setForeground(textMuted);
        balanceTitle.setFont(new Font("SansSerif", Font.BOLD, 10));
        walletBalanceLabel = new JLabel("$0.00");
        walletBalanceLabel.setForeground(pastelGreen);
        walletBalanceLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        balanceRow.add(balanceTitle, BorderLayout.NORTH);
        balanceRow.add(walletBalanceLabel, BorderLayout.SOUTH);

        // Asset selector
        assetCombo = new JComboBox<>();
        assetCombo.setBackground(bgAbsoluteDark); assetCombo.setForeground(textPrimary);
        assetCombo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        assetCombo.setMaximumSize(new Dimension(500, 40));
        assetCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        assetCombo.setBorder(BorderFactory.createCompoundBorder(new LineBorder(borderSubtle, 1), new EmptyBorder(5, 10, 5, 10)));
        assetPriceHintLabel = new JLabel(" ");
        assetPriceHintLabel.setForeground(pastelBlue);
        assetPriceHintLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        assetPriceHintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        assetCombo.addActionListener(e -> updatePriceHint());

        // Units
        unitsField = createInputField();
        unitsField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) { updateTotal(); }
        });

        // Price (auto-filled from IPO, read-only)
        priceField = createInputField();
        priceField.setText("0.00");
        priceField.setEditable(false);
        priceField.setBackground(new Color(30, 30, 30));
        priceField.setToolTipText("Price is fixed by the IPO — not editable");

        // Payment source (NEW)
        JPanel paymentRow = new JPanel(new BorderLayout());
        paymentRow.setOpaque(false);
        paymentRow.setMaximumSize(new Dimension(500, 60));
        JLabel paymentTitle = new JLabel("PAYMENT SOURCE");
        paymentTitle.setForeground(textMuted);
        paymentTitle.setFont(new Font("SansSerif", Font.BOLD, 11));
        paymentSourceCombo = new JComboBox<>(new String[]{
            "💳  Wallet Balance  (deducts from wallet)",
            "🏦  Bank Transfer   (records as incoming transaction)"
        });
        paymentSourceCombo.setBackground(bgAbsoluteDark);
        paymentSourceCombo.setForeground(textPrimary);
        paymentSourceCombo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        paymentSourceCombo.setMaximumSize(new Dimension(500, 40));
        paymentSourceCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        paymentSourceCombo.setBorder(BorderFactory.createCompoundBorder(new LineBorder(borderSubtle, 1), new EmptyBorder(5, 10, 5, 10)));

        // Estimated total
        JPanel totalSection = new JPanel(new BorderLayout());
        totalSection.setOpaque(false);
        totalSection.setMaximumSize(new Dimension(500, 60));
        totalSection.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 1, 0, borderSubtle), new EmptyBorder(15, 0, 15, 0)));
        JLabel totalTitle2 = new JLabel("ESTIMATED TOTAL");
        totalTitle2.setForeground(textMuted);
        totalTitle2.setFont(new Font("SansSerif", Font.BOLD, 11));
        estimatedTotalLabel = new JLabel("$0.00");
        estimatedTotalLabel.setForeground(textPrimary);
        estimatedTotalLabel.setFont(new Font("SansSerif", Font.BOLD, 26));
        totalSection.add(totalTitle2, BorderLayout.NORTH);
        totalSection.add(estimatedTotalLabel, BorderLayout.SOUTH);

        // Submit
        JButton submitBtn = new JButton("CONFIRM BUY ORDER");
        submitBtn.setBackground(pastelGreen); submitBtn.setForeground(bgAbsoluteDark);
        submitBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        submitBtn.setFocusPainted(false); submitBtn.setOpaque(true); submitBtn.setBorderPainted(false);
        submitBtn.setMaximumSize(new Dimension(500, 48));
        submitBtn.setBorder(new EmptyBorder(14, 0, 14, 0));
        submitBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        submitBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        submitBtn.addActionListener(e -> handleSubmit());

        JButton backBtn = new JButton("← Back to Market View");
        backBtn.setContentAreaFilled(false); backBtn.setBorderPainted(false);
        backBtn.setForeground(textMuted); backBtn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        backBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        backBtn.addActionListener(e -> { dispose(); new MarketViewUI_updated().setVisible(true); });

        // Assemble form
        formCard.add(balanceRow);
        formCard.add(Box.createRigidArea(new Dimension(0, 25)));
        formCard.add(makeLabel("SELECT ASSET"));
        formCard.add(Box.createRigidArea(new Dimension(0, 6)));
        formCard.add(assetCombo);
        formCard.add(Box.createRigidArea(new Dimension(0, 4)));
        formCard.add(assetPriceHintLabel);
        formCard.add(Box.createRigidArea(new Dimension(0, 18)));
        formCard.add(makeLabel("QUANTITY (UNITS)"));
        formCard.add(Box.createRigidArea(new Dimension(0, 6)));
        formCard.add(unitsField);
        formCard.add(Box.createRigidArea(new Dimension(0, 18)));
        formCard.add(makeLabel("LIMIT PRICE PER UNIT ($)"));
        formCard.add(Box.createRigidArea(new Dimension(0, 6)));
        formCard.add(priceField);
        formCard.add(Box.createRigidArea(new Dimension(0, 18)));
        formCard.add(makeLabel("PAYMENT SOURCE"));
        formCard.add(Box.createRigidArea(new Dimension(0, 6)));
        formCard.add(paymentSourceCombo);
        formCard.add(Box.createRigidArea(new Dimension(0, 20)));
        formCard.add(totalSection);
        formCard.add(Box.createRigidArea(new Dimension(0, 25)));
        formCard.add(submitBtn);
        formCard.add(Box.createRigidArea(new Dimension(0, 12)));
        formCard.add(backBtn);

        formColumn.add(pageTitle);
        formColumn.add(Box.createRigidArea(new Dimension(0, 6)));
        formColumn.add(pageSubtitle);
        formColumn.add(Box.createRigidArea(new Dimension(0, 20)));
        formColumn.add(formCard);

        // ===== RIGHT: ASSET LIST =====
        JPanel rightColumn = new JPanel(new BorderLayout());
        rightColumn.setOpaque(false);
        JPanel assetCard = createRoundedPanel();
        assetCard.setLayout(new BorderLayout());
        assetCard.setBorder(new EmptyBorder(25, 25, 25, 25));
        JLabel assetCardTitle = new JLabel("VERIFIED ASSETS ON MARKET");
        assetCardTitle.setForeground(textPrimary);
        assetCardTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        assetCard.add(assetCardTitle, BorderLayout.NORTH);
        assetListPanel = new JPanel();
        assetListPanel.setLayout(new BoxLayout(assetListPanel, BoxLayout.Y_AXIS));
        assetListPanel.setOpaque(false);
        JScrollPane assetScroll = new JScrollPane(assetListPanel);
        assetScroll.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));
        assetScroll.setOpaque(false);
        assetScroll.getViewport().setOpaque(false);
        assetScroll.getViewport().setBackground(cardGlass);
        applyCustomScrollbar(assetScroll);
        assetCard.add(assetScroll, BorderLayout.CENTER);
        rightColumn.add(assetCard, BorderLayout.CENTER);

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

    private void loadData() {
        try {
            double balance = investorDAO.getWalletBalance(currentInvestorId);
            walletBalanceLabel.setText(String.format("$%,.2f", balance));
            verifiedAssets = assetDAO.getVerifiedAssets();
            assetCombo.removeAllItems();
            assetListPanel.removeAll();
            assetListPanel.add(Box.createRigidArea(new Dimension(0, 5)));
            for (VerifiedAssetRow asset : verifiedAssets) {
                assetCombo.addItem("[" + asset.getAssetId() + "] " + asset.getName());
                assetListPanel.add(createAssetRow(asset));
                assetListPanel.add(Box.createRigidArea(new Dimension(0, 8)));
            }
            assetListPanel.revalidate(); assetListPanel.repaint();
            updatePriceHint();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to load data: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updatePriceHint() {
        int idx = assetCombo.getSelectedIndex();
        if (idx >= 0 && verifiedAssets != null && idx < verifiedAssets.size()) {
            VerifiedAssetRow sel = verifiedAssets.get(idx);
            // Auto-fill price from IPO — buyer cannot override it
            double ipoPrice = sel.getPricePerUnit();
            priceField.setText(ipoPrice > 0 ? String.format("%.2f", ipoPrice) : "0.00");
            String capacityInfo = sel.getTotalUnits() > 0
                ? String.format("  |  IPO Capacity: %,d units", sel.getTotalUnits()) : "";
            assetPriceHintLabel.setText("Category: " + sel.getCategory()
                + "  |  IPO Price: $" + String.format("%,.2f", ipoPrice) + capacityInfo);
            updateTotal();
        }
    }

    private void updateTotal() {
        try {
            int units    = Integer.parseInt(unitsField.getText().trim());
            double price = Double.parseDouble(priceField.getText().trim());
            estimatedTotalLabel.setText(String.format("$%,.2f", units * price));
        } catch (NumberFormatException ex) {
            estimatedTotalLabel.setText("$—");
        }
    }
    
    private void handleSubmit() {
        try {
            int idx = assetCombo.getSelectedIndex();
            if (idx < 0 || verifiedAssets == null || verifiedAssets.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please select an asset.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            VerifiedAssetRow selectedAsset = verifiedAssets.get(idx);
            int assetId  = selectedAsset.getAssetId();
            int units    = Integer.parseInt(unitsField.getText().trim());
            double price = selectedAsset.getPricePerUnit(); // always use IPO price — not user input
            if (units <= 0) { JOptionPane.showMessageDialog(this, "Units must be > 0.", "Error", JOptionPane.WARNING_MESSAGE); return; }
            if (price <= 0) { JOptionPane.showMessageDialog(this, "This asset has no IPO price set.", "Error", JOptionPane.WARNING_MESSAGE); return; }

            // IPO capacity enforcement
            if (selectedAsset.getTotalUnits() > 0) {
                List<TradeOrder> allOrders = tradeOrderDAO.listOrders();
                int unitsSold = allOrders.stream()
                    .filter(o -> o.getAssetId() == assetId && "BUY".equalsIgnoreCase(o.getOrderType()))
                    .mapToInt(TradeOrder::getUnits).sum();
                int remaining = selectedAsset.getTotalUnits() - unitsSold;
                if (units > remaining) {
                    JOptionPane.showMessageDialog(this,
                        String.format("IPO capacity exceeded.\nRequested: %d units  |  Remaining: %d units", units, remaining),
                        "Capacity Limit", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }

            double total        = units * price;
            boolean useWallet   = paymentSourceCombo.getSelectedIndex() == 0;
            String paymentLabel = useWallet ? "Wallet Balance" : "Bank Transfer";

            if (useWallet) {
                double balance = investorDAO.getWalletBalance(currentInvestorId);
                if (total > balance) {
                    JOptionPane.showMessageDialog(this,
                        String.format("Insufficient wallet balance.\nRequired: $%,.2f   Available: $%,.2f", total, balance),
                        "Insufficient Funds", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                walletDAO.deductForAssetPurchase(currentInvestorId, total, "Buy Order – Asset " + assetId);
            } else {
                walletDAO.deposit(currentInvestorId, total, "Bank Transfer – Buy Order");
                walletDAO.deductForAssetPurchase(currentInvestorId, total, "Buy Order – Asset " + assetId + " (Bank Transfer)");
            }

            // Place the trade order
            List<TradeOrder> existing = tradeOrderDAO.listOrders();
            int newId = existing.stream().mapToInt(TradeOrder::getOrderId).max().orElse(0) + 1;
            TradeOrder order = new TradeOrder(newId, currentInvestorId, assetId, "BUY", price, units, LocalDate.now(), "OPEN");
            tradeOrderDAO.insertOrder(order);

            // Grant ownership immediately (IPO purchase — no matching seller needed)
            Ownership existing_ownership = ownershipDAO.findOwnership(currentInvestorId, assetId);
            int currentUnits = (existing_ownership != null) ? existing_ownership.getUnitsHeld() : 0;
            ownershipDAO.updateUnits(currentInvestorId, assetId, currentUnits + units);

            JOptionPane.showMessageDialog(this,
                String.format("✓ Buy order #%d placed!\n%d units of Asset %d at $%.2f each.\nTotal: $%,.2f  |  Payment: %s", newId, units, assetId, price, total, paymentLabel),
                "Order Placed", JOptionPane.INFORMATION_MESSAGE);

            // Reset
            unitsField.setText(""); priceField.setText("0.00"); estimatedTotalLabel.setText("$0.00");
            loadData();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number of units.", "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to place order: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Helpers
    private JPanel createAssetRow(VerifiedAssetRow asset) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false); row.setMaximumSize(new Dimension(800, 56));
        row.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,0,1,0,borderSubtle), new EmptyBorder(10,5,10,5)));
        JPanel leftCol = new JPanel(new GridLayout(2,1)); leftCol.setOpaque(false);
        JLabel nameLabel = new JLabel(asset.getName()); nameLabel.setForeground(textPrimary); nameLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        JLabel subLabel  = new JLabel(asset.getCategory() + " · " + asset.getStorageLocation()); subLabel.setForeground(textMuted); subLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        leftCol.add(nameLabel); leftCol.add(subLabel);
        JLabel idLabel = new JLabel("ID: " + asset.getAssetId()); idLabel.setForeground(textMuted); idLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        row.add(leftCol, BorderLayout.WEST); row.add(idLabel, BorderLayout.EAST);
        return row;
    }

    private JTextField createInputField() {
        JTextField f = new JTextField();
        f.setMaximumSize(new Dimension(500, 42)); f.setBackground(bgAbsoluteDark); f.setForeground(textPrimary);
        f.setCaretColor(textPrimary); f.setBorder(BorderFactory.createCompoundBorder(new LineBorder(borderSubtle,1), new EmptyBorder(8,15,8,15)));
        f.setFont(new Font("SansSerif", Font.PLAIN, 14)); f.setAlignmentX(Component.LEFT_ALIGNMENT);
        return f;
    }

    private JLabel makeLabel(String text) {
        JLabel lbl = new JLabel(text); lbl.setForeground(textMuted); lbl.setFont(new Font("SansSerif", Font.BOLD, 11)); lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
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

    private void applyCustomScrollbar(JScrollPane sp) {
        sp.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() { this.thumbColor = borderSubtle; this.trackColor = cardGlass; }
            @Override protected JButton createDecreaseButton(int o) { JButton b = new JButton(); b.setPreferredSize(new Dimension(0,0)); return b; }
            @Override protected JButton createIncreaseButton(int o) { JButton b = new JButton(); b.setPreferredSize(new Dimension(0,0)); return b; }
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
            setLayout(null); setOpaque(false);
            setPreferredSize(new Dimension(currentWidth, 0));
            generateNoiseTexture();
            logoLabel = new JLabel("Fractional.");
            logoLabel.setFont(new Font("Serif", Font.BOLD, 26)); logoLabel.setForeground(textPrimary);
            logoLabel.setBounds(25,20,200,40); logoLabel.setVisible(false); add(logoLabel);
            String[] navItems = {"Main Dashboard","Market View","Wallet","Holdings","Profile Settings"};
            navButtons = new JButton[navItems.length];
            for (int i = 0; i < navItems.length; i++) {
                navButtons[i] = new JButton(navItems[i]);
                navButtons[i].setFont(new Font("SansSerif", i==1?Font.BOLD:Font.PLAIN, 15));
                navButtons[i].setForeground(i==1?textPrimary:textMuted);
                navButtons[i].setContentAreaFilled(false); navButtons[i].setBorderPainted(false);
                navButtons[i].setFocusPainted(false); navButtons[i].setHorizontalAlignment(SwingConstants.LEFT);
                navButtons[i].setCursor(new Cursor(Cursor.HAND_CURSOR)); navButtons[i].setVisible(false);
                navButtons[i].setBounds(20, (i==navItems.length-1)?650:100+(i*50), 200, 40);
                add(navButtons[i]);
            }
            navButtons[0].addActionListener(e -> { BuyOrderUI.this.dispose(); new InvestorDashUI().setVisible(true); });
            navButtons[1].addActionListener(e -> { BuyOrderUI.this.dispose(); new MarketViewUI_updated().setVisible(true); });
            navButtons[2].addActionListener(e -> { BuyOrderUI.this.dispose(); new WalletViewUI().setVisible(true); });
            navButtons[3].addActionListener(e -> { BuyOrderUI.this.dispose(); new HoldingsViewUI_updated().setVisible(true); });
            navButtons[4].addActionListener(e -> { BuyOrderUI.this.dispose(); new ProfileSettingsUI().setVisible(true); });
        }

        public void toggleSidebar() {
            if (animator != null && animator.isRunning()) return;
            int targetWidth = isExpanded ? 0 : EXPANDED_WIDTH;
            int step = isExpanded ? -15 : 15;
            if (isExpanded) { logoLabel.setVisible(false); for (JButton b : navButtons) b.setVisible(false); }
            animator = new Timer(10, e -> {
                currentWidth += step; if (currentWidth < 0) currentWidth = 0; if (currentWidth > EXPANDED_WIDTH) currentWidth = EXPANDED_WIDTH;
                setPreferredSize(new Dimension(currentWidth, 0)); revalidate(); repaint();
                if (currentWidth == targetWidth) { isExpanded = !isExpanded; ((Timer)e.getSource()).stop();
                    if (isExpanded) { logoLabel.setVisible(true); for (JButton b : navButtons) b.setVisible(true); } }
            });
            animator.start();
        }

        private void generateNoiseTexture() {
            noiseOverlay = new BufferedImage(EXPANDED_WIDTH, 1200, BufferedImage.TYPE_INT_ARGB);
            for (int x = 0; x < EXPANDED_WIDTH; x++)
                for (int y = 0; y < 1200; y++)
                    if (Math.random() > 0.85)
                        noiseOverlay.setRGB(x, y, new Color(255,255,255,(int)(Math.random()*10)).getRGB());
        }

        @Override protected void paintComponent(Graphics g) {
            if (currentWidth == 0) return;
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            GeneralPath path = new GeneralPath();
            path.moveTo(0,0); int cd=(int)(25*((double)currentWidth/EXPANDED_WIDTH));
            path.lineTo(getWidth()-cd,0); path.quadTo(getWidth(),getHeight()/2.0,getWidth()-cd,getHeight());
            path.lineTo(0,getHeight()); path.closePath();
            g2.setColor(new Color(30,30,30,150)); g2.fill(path); g2.setClip(path); g2.drawImage(noiseOverlay,0,0,null);
            g2.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BuyOrderUI().setVisible(true));
    }
}
