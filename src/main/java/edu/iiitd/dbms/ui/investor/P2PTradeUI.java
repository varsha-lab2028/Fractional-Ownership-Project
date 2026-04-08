package edu.iiitd.dbms.ui.investor;

import edu.iiitd.dbms.auth.LoginManager;
import edu.iiitd.dbms.data_access.*;
import edu.iiitd.dbms.domain.*;
import edu.iiitd.dbms.service.LiveMarketDataManager;
import edu.iiitd.dbms.service.TradingService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * P2PTradeUI — Investor-to-investor secondary market trades.
 *
 * Layout
 * ──────
 *  LEFT  – Open sell listings with live price context.
 *           Each row shows: asset, seller's asking price, units, and the
 *           allowed range (market price ±10 %).  A "BUY" button populates
 *           the order form on the right.
 *
 *  RIGHT – Place order form (BUY or SELL).
 *           For BUY  : shows min / max price bounds and P&L vs market price.
 *           For SELL : shows how the asking price compares to market price.
 */
public class P2PTradeUI extends JFrame {

    // ── Palette ──────────────────────────────────────────────────────────────
    private final Color bgAbsoluteDark = new Color(14, 14, 14);
    private final Color cardGlass      = new Color(26, 26, 26);
    private final Color borderSubtle   = new Color(45, 45, 45);
    private final Color textPrimary    = new Color(250, 250, 250);
    private final Color textMuted      = new Color(150, 150, 150);
    private final Color pastelGreen    = new Color(119, 221, 119);
    private final Color pastelRed      = new Color(255, 105, 97);
    private final Color pastelBlue     = new Color(174, 198, 207);
    private final Color accentAmber    = new Color(255, 179, 71);

    // ── State ────────────────────────────────────────────────────────────────
    private final int currentInvestorId = LoginManager.getCurrentUser() != null
        ? LoginManager.getCurrentUser().getLinkedId() : 1;

    private final TradingService  tradingService  = new TradingService();
    private final TradeOrderDAO   tradeOrderDAO   = new TradeOrderDAO();
    private final InvestorDAO     investorDAO     = new InvestorDAO();
    private final OwnershipDAO    ownershipDAO    = new OwnershipDAO();
    private final LiveMarketDataManager lmd       = LiveMarketDataManager.getInstance();

    // Form fields
    private JTextField unitsField;
    private JTextField priceField;
    private JLabel     assetIdLabel;
    private JLabel     marketPriceLabel;
    private JLabel     minPriceLabel;
    private JLabel     maxPriceLabel;
    private JLabel     plLabel;
    private JLabel     totalLabel;
    private JButton    formSubmitBtn;
    private JPanel     listingsPanel;

    private int     formAssetId    = -1;
    private boolean formIsBuyMode  = true;
    private int     formSellOrderId = -1;   // set when user clicks Buy on a listing

    private GrainySidebar sidebar;
    private Point dragPoint;

    // Live-price listener for label updates
    private final PropertyChangeListener liveListener;

    // ────────────────────────────────────────────────────────────────────────
    public P2PTradeUI() {
        setTitle("Fractional. - P2P Trade");
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
        closeBtn.setForeground(textMuted);
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
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
        JPanel navLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        navLeft.setOpaque(false);
        navLeft.add(toggleBtn);
        JLabel pageTitle = new JLabel("PEER-TO-PEER MARKET");
        pageTitle.setForeground(textMuted);
        pageTitle.setFont(new Font("SansSerif", Font.BOLD, 13));
        navLeft.add(pageTitle);
        JPanel profilePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5));
        profilePanel.setOpaque(false);
        JLabel roleLabel = new JLabel("INVESTOR");
        roleLabel.setForeground(textMuted);
        roleLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
        JLabel profileLabel = new JLabel(getDisplayName());
        profileLabel.setForeground(textPrimary);
        profileLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        profilePanel.add(roleLabel); profilePanel.add(profileLabel);
        topBar.add(navLeft, BorderLayout.WEST);
        topBar.add(profilePanel, BorderLayout.EAST);

        sidebar = new GrainySidebar();

        // ── Main split layout ─────────────────────────────────────────────
        JPanel main = new JPanel(new BorderLayout(25, 0));
        main.setOpaque(false);
        main.setBorder(new EmptyBorder(20, 25, 25, 25));

        main.add(buildListingsPanel(), BorderLayout.CENTER);
        main.add(buildOrderFormPanel(), BorderLayout.EAST);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(topBar, BorderLayout.NORTH);
        wrapper.add(main, BorderLayout.CENTER);

        add(dragBar, BorderLayout.NORTH);
        add(sidebar, BorderLayout.WEST);
        add(wrapper, BorderLayout.CENTER);

        // ── Live price listener ───────────────────────────────────────────
        liveListener = evt -> {
            if ("priceUpdate".equals(evt.getPropertyName())) {
                @SuppressWarnings("unchecked")
                Map<Integer, Double> prices = (Map<Integer, Double>) evt.getNewValue();
                SwingUtilities.invokeLater(() -> {
                    updateMarketPriceLabels(prices);
                    refreshListingPrices(prices);
                });
            }
        };
        lmd.addListener(liveListener);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent e) {
                lmd.removeListener(liveListener);
            }
        });

        loadListings();
    }

    // ── Listings panel ───────────────────────────────────────────────────────
    private JPanel buildListingsPanel() {
        JPanel outer = createRoundedPanel();
        outer.setLayout(new BorderLayout());
        outer.setBorder(new EmptyBorder(25, 25, 25, 25));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("OPEN SELL LISTINGS");
        title.setForeground(textPrimary);
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        JLabel subtitle = new JLabel("Price range = market price ±10 %");
        subtitle.setForeground(textMuted);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 11));
        JButton refreshBtn = new JButton("↻  Refresh");
        refreshBtn.setContentAreaFilled(false); refreshBtn.setBorderPainted(false);
        refreshBtn.setForeground(pastelBlue); refreshBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        refreshBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshBtn.addActionListener(e -> loadListings());
        JPanel titleBlock = new JPanel(new GridLayout(2, 1));
        titleBlock.setOpaque(false);
        titleBlock.add(title); titleBlock.add(subtitle);
        header.add(titleBlock, BorderLayout.WEST);
        header.add(refreshBtn, BorderLayout.EAST);

        listingsPanel = new JPanel();
        listingsPanel.setLayout(new BoxLayout(listingsPanel, BoxLayout.Y_AXIS));
        listingsPanel.setOpaque(false);

        JScrollPane scroll = new JScrollPane(listingsPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        applyCustomScrollbar(scroll);

        outer.add(header, BorderLayout.NORTH);
        outer.add(scroll, BorderLayout.CENTER);
        return outer;
    }

    // ── Order form panel ─────────────────────────────────────────────────────
    private JPanel buildOrderFormPanel() {
        JPanel outer = createRoundedPanel();
        outer.setLayout(new BoxLayout(outer, BoxLayout.Y_AXIS));
        outer.setBorder(new EmptyBorder(28, 28, 28, 28));
        outer.setPreferredSize(new Dimension(360, 0));

        JLabel formTitle = new JLabel("PLACE ORDER");
        formTitle.setForeground(textPrimary);
        formTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        formTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        assetIdLabel = makeMuted("Select a listing on the left to fill the form");
        assetIdLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Price context block
        JPanel priceContext = new JPanel(new GridLayout(3, 2, 8, 6));
        priceContext.setOpaque(false);
        priceContext.setMaximumSize(new Dimension(400, 80));
        priceContext.setAlignmentX(Component.LEFT_ALIGNMENT);
        priceContext.add(makeFieldLabel("MARKET PRICE"));
        marketPriceLabel = new JLabel("—"); marketPriceLabel.setForeground(pastelBlue); marketPriceLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        priceContext.add(marketPriceLabel);
        priceContext.add(makeFieldLabel("MIN PRICE  (−10%)"));
        minPriceLabel = new JLabel("—"); minPriceLabel.setForeground(pastelGreen); minPriceLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        priceContext.add(minPriceLabel);
        priceContext.add(makeFieldLabel("MAX PRICE  (+10%)"));
        maxPriceLabel = new JLabel("—"); maxPriceLabel.setForeground(pastelRed); maxPriceLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        priceContext.add(maxPriceLabel);

        unitsField = createInputField("Units");
        priceField = createInputField("Price per unit ($)");

        // P&L estimate
        JPanel plRow = new JPanel(new BorderLayout());
        plRow.setOpaque(false);
        plRow.setMaximumSize(new Dimension(400, 55));
        plRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        plRow.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 1, 0, borderSubtle),
            new EmptyBorder(10, 0, 10, 0)));
        JLabel plTitle = new JLabel("EST. TOTAL / P&L vs MARKET");
        plTitle.setForeground(textMuted); plTitle.setFont(new Font("SansSerif", Font.BOLD, 10));
        plLabel = new JLabel("—"); plLabel.setForeground(textPrimary); plLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        totalLabel = makeMuted("Fill in units & price to preview");
        JPanel plInner = new JPanel(new GridLayout(2, 1)); plInner.setOpaque(false);
        plInner.add(plTitle); plInner.add(plLabel);
        plRow.add(plInner, BorderLayout.WEST);
        plRow.add(totalLabel, BorderLayout.EAST);

        // Live update on field changes
        javax.swing.event.DocumentListener docL = new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { updatePreview(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { updatePreview(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updatePreview(); }
        };
        unitsField.getDocument().addDocumentListener(docL);
        priceField.getDocument().addDocumentListener(docL);

        // Submit button
        formSubmitBtn = new JButton("CONFIRM ORDER");
        formSubmitBtn.setBackground(pastelGreen);
        formSubmitBtn.setForeground(bgAbsoluteDark);
        formSubmitBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        formSubmitBtn.setFocusPainted(false); formSubmitBtn.setOpaque(true);
        formSubmitBtn.setBorderPainted(false);
        formSubmitBtn.setMaximumSize(new Dimension(400, 48));
        formSubmitBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        formSubmitBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        formSubmitBtn.addActionListener(e -> handleSubmit());

        JButton placeOwnSellBtn = new JButton("+ List My Units for Sale");
        placeOwnSellBtn.setContentAreaFilled(false); placeOwnSellBtn.setBorderPainted(false);
        placeOwnSellBtn.setForeground(textMuted); placeOwnSellBtn.setFont(new Font("SansSerif", Font.PLAIN, 12));
        placeOwnSellBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        placeOwnSellBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        placeOwnSellBtn.addActionListener(e -> { dispose(); new SellOrderUI().setVisible(true); });

        addBoxRow(outer, formTitle);
        addBoxRow(outer, Box.createRigidArea(new Dimension(0, 4)));
        addBoxRow(outer, assetIdLabel);
        addBoxRow(outer, Box.createRigidArea(new Dimension(0, 18)));
        addBoxRow(outer, priceContext);
        addBoxRow(outer, Box.createRigidArea(new Dimension(0, 18)));
        addBoxRow(outer, makeFieldLabel("QUANTITY (UNITS)"));
        addBoxRow(outer, Box.createRigidArea(new Dimension(0, 5)));
        addBoxRow(outer, unitsField);
        addBoxRow(outer, Box.createRigidArea(new Dimension(0, 14)));
        addBoxRow(outer, makeFieldLabel("YOUR OFFER PRICE ($)"));
        addBoxRow(outer, Box.createRigidArea(new Dimension(0, 5)));
        addBoxRow(outer, priceField);
        addBoxRow(outer, Box.createRigidArea(new Dimension(0, 18)));
        addBoxRow(outer, plRow);
        addBoxRow(outer, Box.createRigidArea(new Dimension(0, 18)));
        addBoxRow(outer, formSubmitBtn);
        addBoxRow(outer, Box.createRigidArea(new Dimension(0, 10)));
        addBoxRow(outer, placeOwnSellBtn);

        return outer;
    }

    // ── Load listings from DB ─────────────────────────────────────────────────
    private void loadListings() {
        listingsPanel.removeAll();
        listingsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        try {
            // Load ALL open sell orders (not just for current investor)
            List<TradeOrder> allOrders = tradeOrderDAO.listOrders();
            List<TradeOrder> sellOrders = new ArrayList<>();
            for (TradeOrder o : allOrders) {
                if ("SELL".equalsIgnoreCase(o.getOrderType())
                    && "OPEN".equalsIgnoreCase(o.getStatus())
                    && o.getInvestorId() != currentInvestorId) { // hide own listings
                    sellOrders.add(o);
                }
            }
            if (sellOrders.isEmpty()) {
                JLabel empty = new JLabel("No open sell listings right now.");
                empty.setForeground(textMuted);
                empty.setFont(new Font("SansSerif", Font.PLAIN, 14));
                empty.setBorder(new EmptyBorder(20, 5, 0, 0));
                listingsPanel.add(empty);
            } else {
                for (TradeOrder order : sellOrders) {
                    listingsPanel.add(createListingRow(order));
                    listingsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
                }
            }
        } catch (Exception e) {
            JLabel err = new JLabel("Could not load listings: " + e.getMessage());
            err.setForeground(pastelRed);
            err.setFont(new Font("SansSerif", Font.PLAIN, 12));
            listingsPanel.add(err);
        }
        listingsPanel.revalidate();
        listingsPanel.repaint();
    }

    private JPanel createListingRow(TradeOrder order) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderSubtle, 1, true),
            new EmptyBorder(14, 18, 14, 18)));

        // Asset name lookup
        Map<Integer, String> names = lmd.getAssetNames();
        String assetName = names.getOrDefault(order.getAssetId(), "Asset " + order.getAssetId());

        // Price context
        double market = lmd.getCurrentPrice(order.getAssetId());
        double ask    = order.getPrice();
        double minP   = market > 0 ? market * 0.90 : ask * 0.90;
        double maxP   = market > 0 ? market * 1.10 : ask * 1.10;
        boolean inRange = ask >= minP && ask <= maxP;

        // Left block
        JPanel leftBlock = new JPanel(new GridLayout(3, 1, 0, 2));
        leftBlock.setOpaque(false);
        JLabel nameLbl = new JLabel(assetName);
        nameLbl.setForeground(textPrimary); nameLbl.setFont(new Font("SansSerif", Font.BOLD, 14));
        JLabel unitsLbl = new JLabel("Units: " + order.getUnits()
            + "   ·   Seller Order #" + order.getOrderId());
        unitsLbl.setForeground(textMuted); unitsLbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
        JLabel rangeLbl = new JLabel(String.format("Allowed range: $%.2f – $%.2f", minP, maxP));
        rangeLbl.setForeground(new Color(100, 130, 100)); rangeLbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
        leftBlock.add(nameLbl); leftBlock.add(unitsLbl); leftBlock.add(rangeLbl);

        // Right block
        JPanel rightBlock = new JPanel();
        rightBlock.setLayout(new BoxLayout(rightBlock, BoxLayout.Y_AXIS));
        rightBlock.setOpaque(false);

        JLabel askLbl = new JLabel(String.format("$%.4f", ask));
        askLbl.setForeground(inRange ? textPrimary : pastelRed);
        askLbl.setFont(new Font("SansSerif", Font.BOLD, 16));
        askLbl.setAlignmentX(Component.RIGHT_ALIGNMENT);

        // % vs market
        String vs = market > 0
            ? String.format("%+.2f%% vs market", (ask - market) / market * 100)
            : "—";
        JLabel vsLbl = new JLabel(vs);
        vsLbl.setForeground(ask <= market ? pastelGreen : pastelRed);
        vsLbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
        vsLbl.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JButton buyBtn = new JButton("BUY");
        buyBtn.setPreferredSize(new Dimension(70, 30));
        buyBtn.setMaximumSize(new Dimension(70, 30));
        buyBtn.setBackground(pastelGreen); buyBtn.setForeground(bgAbsoluteDark);
        buyBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        buyBtn.setFocusPainted(false); buyBtn.setOpaque(true); buyBtn.setBorderPainted(false);
        buyBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        buyBtn.setAlignmentX(Component.RIGHT_ALIGNMENT);
        buyBtn.addActionListener(e -> populateFormForBuy(order));

        rightBlock.add(askLbl);
        rightBlock.add(vsLbl);
        rightBlock.add(Box.createRigidArea(new Dimension(0, 6)));
        rightBlock.add(buyBtn);

        row.add(leftBlock, BorderLayout.CENTER);
        row.add(rightBlock, BorderLayout.EAST);
        row.putClientProperty("orderId",   order.getOrderId());
        row.putClientProperty("assetId",   order.getAssetId());
        row.putClientProperty("askPrice",  ask);
        return row;
    }

    private void populateFormForBuy(TradeOrder sellOrder) {
        formAssetId     = sellOrder.getAssetId();
        formIsBuyMode   = true;
        formSellOrderId = sellOrder.getOrderId();

        Map<Integer, String> names = lmd.getAssetNames();
        assetIdLabel.setText("[" + formAssetId + "] " + names.getOrDefault(formAssetId, "")
            + "  ·  Buying from Order #" + sellOrder.getOrderId());
        assetIdLabel.setForeground(pastelBlue);

        unitsField.setText(String.valueOf(sellOrder.getUnits()));
        priceField.setText(String.format("%.4f", sellOrder.getPrice()));

        formSubmitBtn.setText("EXECUTE BUY");
        formSubmitBtn.setBackground(pastelGreen);
        updateMarketPriceLabelForAsset(formAssetId);
    }

    private void updateMarketPriceLabelForAsset(int assetId) {
        if (assetId < 0) return;
        double market = lmd.getCurrentPrice(assetId);
        if (market > 0) {
            marketPriceLabel.setText(String.format("$%.4f", market));
            minPriceLabel.setText(String.format("$%.4f", market * 0.90));
            maxPriceLabel.setText(String.format("$%.4f", market * 1.10));
        } else {
            marketPriceLabel.setText("Unavailable");
            minPriceLabel.setText("—"); maxPriceLabel.setText("—");
        }
        updatePreview();
    }

    private void updatePreview() {
        try {
            int    units  = Integer.parseInt(unitsField.getText().trim());
            double price  = Double.parseDouble(priceField.getText().trim());
            double market = formAssetId >= 0 ? lmd.getCurrentPrice(formAssetId) : 0;
            double total  = units * price;
            totalLabel.setText(String.format("Total: $%,.2f", total));

            if (market > 0) {
                double minP = market * 0.90, maxP = market * 1.10;
                if (price < minP || price > maxP) {
                    plLabel.setText("OUT OF RANGE");
                    plLabel.setForeground(pastelRed);
                } else {
                    double plPerUnit = formIsBuyMode ? (market - price) : (price - market);
                    double plTotal   = plPerUnit * units;
                    String sign      = plTotal >= 0 ? "+" : "";
                    plLabel.setText(String.format("%s$%,.2f", sign, plTotal));
                    plLabel.setForeground(plTotal >= 0 ? pastelGreen : pastelRed);
                }
            } else {
                plLabel.setText(String.format("$%,.2f", total));
                plLabel.setForeground(textPrimary);
            }
        } catch (NumberFormatException ignored) {
            plLabel.setText("—"); totalLabel.setText("Fill in units & price");
        }
    }

    private void updateMarketPriceLabels(Map<Integer, Double> prices) {
        if (formAssetId >= 0) updateMarketPriceLabelForAsset(formAssetId);
    }

    private void refreshListingPrices(Map<Integer, Double> prices) {
        // Re-render listing rows to reflect new market prices
        // (lightweight: just reload the whole panel)
        loadListings();
    }

    // ── Form submit ───────────────────────────────────────────────────────────
    private void handleSubmit() {
        if (formAssetId < 0) {
            JOptionPane.showMessageDialog(this,
                "Please select a listing first.", "No Asset Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            int    units  = Integer.parseInt(unitsField.getText().trim());
            double price  = Double.parseDouble(priceField.getText().trim());
            double market = lmd.getCurrentPrice(formAssetId);

            // Validate price range
            if (market > 0) {
                double minP = market * 0.90, maxP = market * 1.10;
                if (price < minP || price > maxP) {
                    JOptionPane.showMessageDialog(this,
                        String.format("Price must be between $%.4f and $%.4f (market ±10%%).",
                            minP, maxP),
                        "Price Out of Range", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }

            if (formIsBuyMode) {
                // Place buy order then immediately execute against the chosen sell order
                TradeOrder buyOrder = tradingService.placeBuyOrder(
                    currentInvestorId, formAssetId, units, price);
                Trade trade = tradingService.executeTrade(buyOrder.getOrderId(), formSellOrderId);
                double total = units * price;
                double plTotal = market > 0 ? (market - price) * units : 0;
                JOptionPane.showMessageDialog(this,
                    String.format("""
                        Trade #%d executed successfully!
                        Bought %d units of Asset %d at $%.4f each.
                        Total paid: $%,.2f
                        P&L vs market: %s$%,.2f""",
                        trade.getTradeId(), units, formAssetId, price,
                        total, plTotal >= 0 ? "+" : "", plTotal),
                    "Trade Executed", JOptionPane.INFORMATION_MESSAGE);
            } else {
                tradingService.placeSellOrder(currentInvestorId, formAssetId, units, price);
                JOptionPane.showMessageDialog(this,
                    String.format("Sell order placed: %d units of Asset %d at $%.4f.",
                        units, formAssetId, price),
                    "Order Placed", JOptionPane.INFORMATION_MESSAGE);
            }
            loadListings();
            resetForm();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter valid numbers.", "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Trade failed: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void resetForm() {
        formAssetId = -1; formSellOrderId = -1;
        assetIdLabel.setText("Select a listing on the left");
        assetIdLabel.setForeground(textMuted);
        unitsField.setText(""); priceField.setText("");
        marketPriceLabel.setText("—"); minPriceLabel.setText("—"); maxPriceLabel.setText("—");
        plLabel.setText("—"); totalLabel.setText("Fill in units & price");
    }

    // ── Utility ───────────────────────────────────────────────────────────────
    private String getDisplayName() {
        if (LoginManager.getCurrentUser() == null) return "INVESTOR";
        String full = LoginManager.getCurrentUser().getName();
        if (full == null || full.isBlank()) return "INVESTOR";
        String[] p = full.trim().split("\\s+");
        return p[0].toUpperCase() + (p.length > 1 ? " " + Character.toUpperCase(p[p.length-1].charAt(0)) + "." : "");
    }

    private void addBoxRow(JPanel panel, Component c) {
        if (c instanceof JComponent jc) jc.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(c);
    }

    private JTextField createInputField(String placeholder) {
        JTextField f = new JTextField();
        f.setMaximumSize(new Dimension(400, 42));
        f.setBackground(bgAbsoluteDark);
        f.setForeground(textPrimary); f.setCaretColor(textPrimary);
        f.setFont(new Font("SansSerif", Font.PLAIN, 14));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        f.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(borderSubtle, 1), new EmptyBorder(8, 14, 8, 14)));
        return f;
    }

    private JLabel makeFieldLabel(String t) {
        JLabel l = new JLabel(t);
        l.setForeground(textMuted); l.setFont(new Font("SansSerif", Font.BOLD, 10));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JLabel makeMuted(String t) {
        JLabel l = new JLabel(t);
        l.setForeground(textMuted); l.setFont(new Font("SansSerif", Font.PLAIN, 12));
        return l;
    }

    private JPanel createRoundedPanel() {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(cardGlass);
                g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20);
                g2.setColor(borderSubtle);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        return p;
    }

    private void applyCustomScrollbar(JScrollPane sp) {
        sp.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() { thumbColor = borderSubtle; trackColor = cardGlass; }
            @Override protected JButton createDecreaseButton(int o) { return zeroBtn(); }
            @Override protected JButton createIncreaseButton(int o) { return zeroBtn(); }
            private JButton zeroBtn() { JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0)); return b; }
        });
    }

    // ── Grainy sidebar (same as rest of app) ─────────────────────────────────
    class GrainySidebar extends JPanel {
        private boolean isExpanded = false;
        private final int EXPANDED_WIDTH = 250;
        private int currentWidth = 0;
        private Timer animator;
        private BufferedImage noiseOverlay;
        private JLabel logoLabel;
        private JButton[] navButtons;

        GrainySidebar() {
            setLayout(null); setOpaque(false);
            setPreferredSize(new Dimension(0, 0));
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
                navButtons[i].setFont(new Font("SansSerif", Font.PLAIN, 15));
                navButtons[i].setForeground(textMuted);
                navButtons[i].setContentAreaFilled(false); navButtons[i].setBorderPainted(false);
                navButtons[i].setFocusPainted(false); navButtons[i].setHorizontalAlignment(SwingConstants.LEFT);
                navButtons[i].setCursor(new Cursor(Cursor.HAND_CURSOR)); navButtons[i].setVisible(false);
                int y = (i == nav.length-1) ? 650 : 100 + i*50;
                navButtons[i].setBounds(20, y, 200, 40);
                add(navButtons[i]);
            }
            navButtons[0].addActionListener(e -> { dispose(); new InvestorDashUI().setVisible(true); });
            navButtons[1].addActionListener(e -> { dispose(); new MarketViewUI().setVisible(true); });
            navButtons[2].addActionListener(e -> { dispose(); new WalletViewUI().setVisible(true); });
            navButtons[3].addActionListener(e -> { dispose(); new HoldingsViewUI_updated().setVisible(true); });
            navButtons[4].addActionListener(e -> { dispose(); new ProfileSettingsUI().setVisible(true); });
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new P2PTradeUI().setVisible(true));
    }
}
