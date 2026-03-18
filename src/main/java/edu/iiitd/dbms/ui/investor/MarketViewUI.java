package edu.iiitd.dbms.ui.investor;

import edu.iiitd.dbms.auth.LoginManager;

import edu.iiitd.dbms.ui.components.MarketChartPanel;
import edu.iiitd.dbms.dto.InvestorMarketView.MarketViewRow;
import edu.iiitd.dbms.service.MarketService;
import java.util.List;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;

public class MarketViewUI extends JFrame {
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
    private boolean isBuyMode = true;

    //adding class fields
    private final MarketService marketService = new MarketService();
    private JPanel assetList;
    private AestheticSearchBar searchField;

    public MarketViewUI() {
        setTitle("Fractional. - Secondary Market");
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
        
        searchField = new AestheticSearchBar("🔍 Search ticker or asset name...");
        searchField.setPreferredSize(new Dimension(350, 40));
        searchContainer.add(searchField);

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

        sidebar = new GrainySidebar();

        // SCROLLABLE MAIN LAYOUT
        // LEFT COLUMN (Chart + Screener)
        JPanel leftColumn = new JPanel();
        leftColumn.setLayout(new BoxLayout(leftColumn, BoxLayout.Y_AXIS));
        leftColumn.setOpaque(false);
        leftColumn.setBorder(new EmptyBorder(0, 0, 0, 20));

        //Dynamic Market Chart (Line/Candle)
        JPanel chartContainer = createRoundedPanel();
        chartContainer.setLayout(new BorderLayout());
        chartContainer.setBorder(new EmptyBorder(20, 20, 20, 20));
        chartContainer.add(new MarketChartPanel("JDN1 - LIVE TRADING DATA"), BorderLayout.CENTER);
        
        //Asset Screener
        JPanel screenerPanel = createRoundedPanel();
        screenerPanel.setLayout(new BorderLayout());
        screenerPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JLabel screenerTitle = new JLabel("SECONDARY MARKETPLACE");
        screenerTitle.setForeground(textPrimary);
        screenerTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        screenerPanel.add(screenerTitle, BorderLayout.NORTH);

        assetList = new JPanel();
        assetList.setLayout(new BoxLayout(assetList, BoxLayout.Y_AXIS));
        assetList.setBackground(cardGlass);

        // INNER SCROLL PANE: Strict bounds apply independent scrolling
        JScrollPane screenerScroll = new JScrollPane(assetList);
        screenerScroll.setBorder(null);
        screenerScroll.getViewport().setBackground(cardGlass);
        screenerScroll.setPreferredSize(new Dimension(0, 280)); 
        applyCustomScrollbar(screenerScroll);

        screenerPanel.add(screenerScroll, BorderLayout.CENTER);

        leftColumn.add(chartContainer);
        leftColumn.add(Box.createRigidArea(new Dimension(0, 20)));
        leftColumn.add(screenerPanel);

        // RIGHT COLUMN (Order Book + Ticket)
        JPanel rightColumn = new JPanel();
        rightColumn.setLayout(new BoxLayout(rightColumn, BoxLayout.Y_AXIS));
        rightColumn.setOpaque(false);
        rightColumn.setPreferredSize(new Dimension(380, 0));

        JPanel orderBookCard = createRoundedPanel();
        orderBookCard.setLayout(new BoxLayout(orderBookCard, BoxLayout.Y_AXIS));
        orderBookCard.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JLabel obTitle = new JLabel("ORDER BOOK: JDN1");
        obTitle.setForeground(textMuted);
        obTitle.setFont(new Font("SansSerif", Font.BOLD, 11));
        
        orderBookCard.add(obTitle);
        orderBookCard.add(Box.createRigidArea(new Dimension(0, 15)));
        
        orderBookCard.add(createOrderBookRow("125 Units", "$24.70", pastelRed));
        orderBookCard.add(Box.createRigidArea(new Dimension(0, 5)));
        orderBookCard.add(createOrderBookRow("40 Units", "$24.60", pastelRed));
        
        orderBookCard.add(Box.createRigidArea(new Dimension(0, 15)));
        JLabel spreadLabel = new JLabel("SPREAD: $0.05");
        spreadLabel.setForeground(pastelBlue);
        spreadLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        spreadLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        orderBookCard.add(spreadLabel);
        orderBookCard.add(Box.createRigidArea(new Dimension(0, 15)));

        orderBookCard.add(createOrderBookRow("50 Units", "$24.50", pastelGreen));
        orderBookCard.add(Box.createRigidArea(new Dimension(0, 5)));
        orderBookCard.add(createOrderBookRow("200 Units", "$24.45", pastelGreen));

        JPanel orderTicket = createRoundedPanel();
        orderTicket.setLayout(new BoxLayout(orderTicket, BoxLayout.Y_AXIS));
        orderTicket.setBorder(new EmptyBorder(25, 25, 25, 25));

        JPanel togglePanel = new JPanel(new GridLayout(1, 2, 10, 0));
        togglePanel.setOpaque(false);
        JButton buyBtn = new JButton("BUY JDN1");
        JButton sellBtn = new JButton("SELL JDN1");
        
        // Initial state
        styleOrderToggle(buyBtn, true, pastelGreen);
        styleOrderToggle(sellBtn, false, pastelRed);

        buyBtn.addActionListener(e -> {
            styleOrderToggle(buyBtn, true, pastelGreen);
            styleOrderToggle(sellBtn, false, pastelRed);
            isBuyMode = true;
        });
        sellBtn.addActionListener(e -> {
            styleOrderToggle(buyBtn, false, pastelGreen);
            styleOrderToggle(sellBtn, true, pastelRed);
            isBuyMode = false;
        });

        togglePanel.add(buyBtn); togglePanel.add(sellBtn);

        orderTicket.add(togglePanel);
        orderTicket.add(Box.createRigidArea(new Dimension(0, 20)));
        orderTicket.add(createInputLabel("QUANTITY (UNITS)"));
        orderTicket.add(new AestheticSearchBar("0"));
        orderTicket.add(Box.createRigidArea(new Dimension(0, 15)));
        orderTicket.add(createInputLabel("LIMIT PRICE ($)"));
        orderTicket.add(new AestheticSearchBar("$0.00"));
        orderTicket.add(Box.createRigidArea(new Dimension(0, 20)));

        // Fixed invisible text on PLACE ORDER button
        JButton submitBtn = new JButton("PLACE ORDER");
        submitBtn.setBackground(pastelGreen); 
        submitBtn.setForeground(bgAbsoluteDark); // Dark text on light background
        submitBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        submitBtn.setFocusPainted(false);
        submitBtn.setMaximumSize(new Dimension(400, 45));
        submitBtn.setBorder(new EmptyBorder(12, 0, 12, 0));
        orderTicket.add(submitBtn);

        rightColumn.add(orderBookCard);
        rightColumn.add(Box.createRigidArea(new Dimension(0, 20)));
        rightColumn.add(orderTicket);

        // OUTER SCROLL PANE: Scrolls the entire page if window is small
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

        loadAllMarketRows();
    }

    // --- Database Logic ---
    private void loadAllMarketRows() {
        try {
            List<MarketViewRow> rows = marketService.getAllMarketRows();
            populateAssetList(rows);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to load market data: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void loadMarketRowsByCategory(String category) {
        try {
            List<MarketViewRow> rows = marketService.getMarketRowsByCategory(category);
            populateAssetList(rows);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to load filtered market data: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void populateAssetList(List<MarketViewRow> rows) {
        assetList.removeAll();
        assetList.add(Box.createRigidArea(new Dimension(0, 15)));

        if (rows == null || rows.isEmpty()) {
            JLabel emptyLabel = new JLabel("No market listings found.");
            emptyLabel.setForeground(textMuted);
            emptyLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
            assetList.add(emptyLabel);
        } else {
            for (int i = 0; i < rows.size(); i++) {
                MarketViewRow row = rows.get(i);
                String name = row.getAssetName();
                String subText = row.getCategory();
                String price = String.format("$%.2f", row.getPricePerUnit());
                
                // Using pastel green for mock "positive" change for now
                assetList.add(createAssetRow(name, subText, price, "+0.0%", pastelGreen));

                if (i < rows.size() - 1) {
                    assetList.add(Box.createRigidArea(new Dimension(0, 10)));
                }
            }
        }
        assetList.revalidate();
        assetList.repaint();
    }

    private Color getStatusColor(String status) {
        if (status == null) return textMuted;
        if (status.equalsIgnoreCase("Open") || status.equalsIgnoreCase("Active")) return pastelGreen;
        else if (status.equalsIgnoreCase("Closed") || status.equalsIgnoreCase("Rejected")) return pastelRed;
        else return pastelBlue;
    }

    // --- Aesthetics & Custom Components ---
    
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

    private JPanel createAssetRow(String name, String ticker, String price, String change, Color changeColor) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(800, 60));
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, borderSubtle),
            new EmptyBorder(10, 5, 10, 5)
        ));

        JPanel leftCol = new JPanel(new GridLayout(2, 1));
        leftCol.setOpaque(false);
        JLabel nLabel = new JLabel(name);
        nLabel.setForeground(textPrimary);
        nLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        JLabel tLabel = new JLabel(ticker);
        tLabel.setForeground(textMuted);
        tLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        leftCol.add(nLabel); leftCol.add(tLabel);

        JPanel rightCol = new JPanel(new GridLayout(2, 1));
        rightCol.setOpaque(false);
        JLabel pLabel = new JLabel(price, SwingConstants.RIGHT);
        pLabel.setForeground(textPrimary); 
        pLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        JLabel cLabel = new JLabel(change, SwingConstants.RIGHT);
        cLabel.setForeground(changeColor);
        cLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        rightCol.add(pLabel); rightCol.add(cLabel);

        row.add(leftCol, BorderLayout.WEST);
        row.add(rightCol, BorderLayout.EAST);
        return row;
    }

    private JPanel createOrderBookRow(String qty, String price, Color priceColor) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(400, 25));

        JLabel qLabel = new JLabel(qty);
        qLabel.setForeground(textMuted);
        qLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));

        JLabel pLabel = new JLabel(price);
        pLabel.setForeground(priceColor);
        pLabel.setFont(new Font("SansSerif", Font.BOLD, 13));

        row.add(qLabel, BorderLayout.WEST);
        row.add(pLabel, BorderLayout.EAST);
        return row;
    }

    private JLabel createInputLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(textMuted);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 10));
        return lbl;
    }

    private void styleOrderToggle(JButton btn, boolean isActive, Color activeColor) {
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(10, 0, 10, 0));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        if (isActive) {
            btn.setBackground(activeColor);
            btn.setForeground(bgAbsoluteDark); 
        } else {
            btn.setBackground(cardGlass); 
            btn.setForeground(textPrimary); 
        }
    }

    private void applyCustomScrollbar(JScrollPane scrollPane) {
        scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() { this.thumbColor = borderSubtle; this.trackColor = cardGlass; }
            @Override protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
            @Override protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }
            private JButton createZeroButton() { JButton jb = new JButton(); jb.setPreferredSize(new Dimension(0,0)); return jb; }
        });
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
            g2.setColor(bgAbsoluteDark); 
            g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20); 
            g2.setColor(borderSubtle);
            g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20); 
            super.paintComponent(g);
            g2.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MarketViewUI().setVisible(true));
    }
}