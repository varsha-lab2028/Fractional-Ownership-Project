package edu.iiitd.dbms.ui.investor;

import edu.iiitd.dbms.ui.components.CompactChartPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;

public class investorDash extends JFrame {

    private final Color bgAbsoluteDark = new Color(14, 14, 14); 
    private final Color cardGlass = new Color(26, 26, 26);      
    private final Color borderSubtle = new Color(45, 45, 45);   
    private final Color textPrimary = new Color(250, 250, 250); 
    private final Color textMuted = new Color(150, 150, 150);   
    private final Color pastelGreen = new Color(119, 221, 119); 

    private Point dragPoint;
    private GrainySidebar sidebar;

    public investorDash() {
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

        // --- THE CUSTOM COLLAPSIBLE SIDEBAR ---
        sidebar = new GrainySidebar();

        // --- RIGHT PANEL ---
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
        JLabel plAmount = new JLabel("+$14,250.00");
        plAmount.setForeground(pastelGreen);
        plAmount.setFont(new Font("SansSerif", Font.BOLD, 28)); 
        JLabel plPercentage = new JLabel("▲ 12.4% All Time");
        plPercentage.setForeground(pastelGreen);
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
        JLabel wallAmt = new JLabel("$5,430.00");
        wallAmt.setForeground(textPrimary);
        wallAmt.setFont(new Font("SansSerif", Font.BOLD, 22));
        walletCard.add(wallTitle, BorderLayout.NORTH);
        walletCard.add(wallAmt, BorderLayout.SOUTH);

        JPanel activityCard = createRoundedPanel();
        activityCard.setLayout(new BoxLayout(activityCard, BoxLayout.Y_AXIS));
        activityCard.setBorder(new EmptyBorder(25, 25, 25, 25));
        JLabel actTitle = new JLabel("RECENT ACTIVITY");
        actTitle.setForeground(textMuted);
        actTitle.setFont(new Font("SansSerif", Font.BOLD, 11));
        activityCard.add(actTitle);
        activityCard.add(Box.createRigidArea(new Dimension(0, 15)));
        activityCard.add(createActivityRow("Bought Jordan 1", "-$240.00"));
        activityCard.add(Box.createRigidArea(new Dimension(0, 10)));
        activityCard.add(createActivityRow("Dividend Received", "+$15.50"));
        activityCard.add(Box.createRigidArea(new Dimension(0, 10)));
        activityCard.add(createActivityRow("Deposit Funds", "+$1000.00"));

        rightPanel.add(plCard);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        rightPanel.add(walletCard);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        rightPanel.add(activityCard); 

        // --- CENTER PANEL ---
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

        JPanel ipoListPanel = new JPanel();
        ipoListPanel.setLayout(new BoxLayout(ipoListPanel, BoxLayout.Y_AXIS));
        ipoListPanel.setBackground(cardGlass);

        ipoListPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        ipoListPanel.add(createIpoRow("Vintage Rolex Daytona", "10,000 Units", "$15.50 / unit"));
        ipoListPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        ipoListPanel.add(createIpoRow("1985 Air Jordan 1", "5,000 Units", "$24.00 / unit"));
        ipoListPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        ipoListPanel.add(createIpoRow("Basquiat Original Sketch", "20,000 Units", "$5.00 / unit"));
        ipoListPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        ipoListPanel.add(createIpoRow("Ferrari F40 Fraction", "50,000 Units", "$10.00 / unit"));

        JScrollPane scrollPane = new JScrollPane(ipoListPanel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(cardGlass);
        scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() { this.thumbColor = borderSubtle; this.trackColor = cardGlass; }
            @Override protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
            @Override protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }
            private JButton createZeroButton() { JButton jb = new JButton(); jb.setPreferredSize(new Dimension(0,0)); return jb; }
        });

        ipoContainer.add(scrollPane, BorderLayout.CENTER);
        centerPanel.add(chartsGrid, BorderLayout.NORTH);
        centerPanel.add(ipoContainer, BorderLayout.CENTER);

        // --- Assembly ---
        add(dragBar, BorderLayout.NORTH);
        add(sidebar, BorderLayout.WEST);
        
        JPanel mainContent = new JPanel(new BorderLayout());
        mainContent.setOpaque(false);
        mainContent.add(topBar, BorderLayout.NORTH);
        mainContent.add(centerPanel, BorderLayout.CENTER);
        mainContent.add(rightPanel, BorderLayout.EAST);
        add(mainContent, BorderLayout.CENTER);
    }

    // ==========================================
    // UPGRADED TRANSLUCENT GRAINY SIDEBAR
    // ==========================================
    class GrainySidebar extends JPanel {
        private boolean isExpanded = false; // Starts completely collapsed
        private final int EXPANDED_WIDTH = 250;
        private final int COLLAPSED_WIDTH = 0; // Hides entirely when closed
        private int currentWidth = COLLAPSED_WIDTH;
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
            logoLabel.setVisible(false); // Hidden initially
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
                navButtons[i].setVisible(false); // Hidden initially
                
                int yPos = (i == navItems.length - 1) ? 650 : 100 + (i * 50);
                navButtons[i].setBounds(20, yPos, 200, 40);
                add(navButtons[i]);
            }
        }

        public void toggleSidebar() {
            if (animator != null && animator.isRunning()) return;
            
            int targetWidth = isExpanded ? COLLAPSED_WIDTH : EXPANDED_WIDTH;
            int step = isExpanded ? -15 : 15; // Smooth animation speed
            
            // Hide text immediately if collapsing
            if (isExpanded) {
                logoLabel.setVisible(false);
                for (JButton btn : navButtons) btn.setVisible(false);
            }

            animator = new Timer(10, e -> {
                currentWidth += step;
                if (currentWidth < 0) currentWidth = 0;
                if (currentWidth > EXPANDED_WIDTH) currentWidth = EXPANDED_WIDTH;
                
                setPreferredSize(new Dimension(currentWidth, 0));
                revalidate();
                repaint();

                if (currentWidth == targetWidth) {
                    isExpanded = !isExpanded;
                    ((Timer) e.getSource()).stop();
                    
                    // Show text only when fully expanded
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
                    // Much less grainy (only 15% probability) and extremely faint alpha (max 10)
                    if (Math.random() > 0.85) {
                        int alpha = (int)(Math.random() * 10); 
                        noiseOverlay.setRGB(x, y, new Color(255, 255, 255, alpha).getRGB());
                    }
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            // Do not paint anything if fully collapsed (removes the background completely)
            if (currentWidth == 0) return;

            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            GeneralPath path = new GeneralPath();
            path.moveTo(0, 0);
            
            // Smoothly reduce the curve bulge as it collapses
            int curveDepth = (int)(25 * ((double)currentWidth / EXPANDED_WIDTH));
            path.lineTo(w - curveDepth, 0);
            path.quadTo(w, h / 2.0, w - curveDepth, h);
            path.lineTo(0, h);
            
            path.closePath();

            // 1. More Transparent Background (Reduced alpha to 150)
            g2.setColor(new Color(30, 30, 30, 150)); 
            g2.fill(path);

            // 2. Subtle Grain Overlay
            g2.setClip(path);
            g2.drawImage(noiseOverlay, 0, 0, null);

            g2.dispose();
        }
    }

    // --- Aesthetic Helpers ---

    private JPanel createActivityRow(String action, String amount) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        JLabel actLabel = new JLabel(action);
        actLabel.setForeground(textPrimary);
        actLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        JLabel amtLabel = new JLabel(amount);
        amtLabel.setForeground(amount.startsWith("+") ? pastelGreen : textMuted);
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
        JLabel nLabel = new JLabel(name);
        nLabel.setForeground(textPrimary);
        nLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        JLabel uLabel = new JLabel("Issued: " + units);
        uLabel.setForeground(textMuted);
        uLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        leftCol.add(nLabel); leftCol.add(uLabel);

        JPanel rightCol = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightCol.setOpaque(false);
        JLabel pLabel = new JLabel(price);
        pLabel.setForeground(textPrimary); 
        pLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        
        JButton buyBtn = new JButton("Invest");
        buyBtn.setBackground(textPrimary);
        buyBtn.setForeground(bgAbsoluteDark); 
        buyBtn.setFocusPainted(false);
        buyBtn.setFont(new Font("SansSerif", Font.BOLD, 12));

        rightCol.add(pLabel);
        rightCol.add(Box.createRigidArea(new Dimension(15, 0)));
        rightCol.add(buyBtn);

        row.add(leftCol, BorderLayout.WEST);
        row.add(rightCol, BorderLayout.EAST);
        return row;
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
        SwingUtilities.invokeLater(() -> new investorDash().setVisible(true));
    }
}