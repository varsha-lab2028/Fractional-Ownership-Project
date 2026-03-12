package edu.iiitd.dbms.ui.admin;

import edu.iiitd.dbms.ui.components.CompactChartPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;

public class AdminDashUI extends JFrame {

    // Grayscale & Pastel Palette (Matched to Investor View)
    private final Color bgAbsoluteDark = new Color(14, 14, 14); 
    private final Color cardGlass = new Color(26, 26, 26);      
    private final Color borderSubtle = new Color(45, 45, 45);   
    private final Color textPrimary = new Color(250, 250, 250); 
    private final Color textMuted = new Color(150, 150, 150);   
    private final Color pastelGreen = new Color(119, 221, 119); // Open / Verified
    private final Color pastelRed = new Color(255, 105, 97);    // Closed / Sus Flag
    private final Color pastelBlue = new Color(174, 198, 207);  // Upcoming

    private Point dragPoint;
    private GrainySidebar sidebar;
    private boolean isMarketOpen = true; // State for Market Toggle

    public AdminDashUI() {
        setTitle("Fractional. - Admin Control Center");
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
        
        AestheticSearchBar searchField = new AestheticSearchBar("🔍 Search assets, users, or system logs...");
        searchField.setPreferredSize(new Dimension(350, 40));
        searchContainer.add(searchField);

        JPanel profilePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5));
        profilePanel.setOpaque(false);
        JLabel roleLabel = new JLabel("PLATFORM ADMIN");
        roleLabel.setForeground(pastelBlue); // Distinguishes from Investor
        roleLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
        JLabel profileLabel = new JLabel("DISHA K.");
        profileLabel.setForeground(textPrimary);
        profileLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        profilePanel.add(roleLabel);
        profilePanel.add(profileLabel);

        topBar.add(searchContainer, BorderLayout.WEST);
        topBar.add(profilePanel, BorderLayout.EAST);

        // --- SIDEBAR ---
        sidebar = new GrainySidebar();

        // --- RIGHT PANEL (Controls & Flags) ---
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setOpaque(false);
        rightPanel.setPreferredSize(new Dimension(340, 0)); 
        rightPanel.setBorder(new EmptyBorder(0, 10, 30, 25));

        // Req #2: Market Open/Close Control
        JPanel marketCard = createRoundedPanel();
        marketCard.setLayout(new BoxLayout(marketCard, BoxLayout.Y_AXIS));
        marketCard.setBorder(new EmptyBorder(30, 25, 30, 25));
        
        JLabel mkTitle = new JLabel("SECONDARY MARKET STATUS");
        mkTitle.setForeground(textMuted);
        mkTitle.setFont(new Font("SansSerif", Font.BOLD, 11));
        
        JLabel mkStatus = new JLabel("LIVE & TRADING");
        mkStatus.setForeground(pastelGreen);
        mkStatus.setFont(new Font("SansSerif", Font.BOLD, 26)); 
        
        JButton marketToggleBtn = new JButton("CLOSE MARKET");
        marketToggleBtn.setBackground(pastelRed);
        marketToggleBtn.setForeground(bgAbsoluteDark);
        marketToggleBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        marketToggleBtn.setFocusPainted(false);
        marketToggleBtn.setMaximumSize(new Dimension(200, 35));
        marketToggleBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Toggle Logic
        marketToggleBtn.addActionListener(e -> {
            isMarketOpen = !isMarketOpen;
            if (isMarketOpen) {
                mkStatus.setText("LIVE & TRADING");
                mkStatus.setForeground(pastelGreen);
                marketToggleBtn.setText("CLOSE MARKET");
                marketToggleBtn.setBackground(pastelRed);
            } else {
                mkStatus.setText("MARKET CLOSED");
                mkStatus.setForeground(pastelRed);
                marketToggleBtn.setText("OPEN MARKET");
                marketToggleBtn.setBackground(pastelGreen);
            }
        });

        marketCard.add(mkTitle); marketCard.add(Box.createRigidArea(new Dimension(0, 15)));
        marketCard.add(mkStatus); marketCard.add(Box.createRigidArea(new Dimension(0, 15)));
        marketCard.add(marketToggleBtn);

        // Req #3 Ext: Suspicious Activity Monitor
        JPanel alertCard = createRoundedPanel();
        alertCard.setLayout(new BoxLayout(alertCard, BoxLayout.Y_AXIS));
        alertCard.setBorder(new EmptyBorder(25, 25, 25, 25));
        JLabel alTitle = new JLabel("SECURITY & FLAGS");
        alTitle.setForeground(textMuted);
        alTitle.setFont(new Font("SansSerif", Font.BOLD, 11));
        alertCard.add(alTitle);
        alertCard.add(Box.createRigidArea(new Dimension(0, 15)));
        alertCard.add(createAlertRow("Inv_902", "Oversell Attempt", pastelRed));
        alertCard.add(Box.createRigidArea(new Dimension(0, 10)));
        alertCard.add(createAlertRow("Asset_24", "Missing KYC", pastelBlue));

        rightPanel.add(marketCard);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        rightPanel.add(alertCard);

        // --- CENTER PANEL (Charts & Admin IPO Feed) ---
        JPanel centerPanel = new JPanel(new BorderLayout(0, 20));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(0, 0, 30, 15));

        // Req #5: Dynamic Dash Charts (Adapted for Admin)
        JPanel chartsGrid = new JPanel(new GridLayout(1, 2, 20, 0));
        chartsGrid.setOpaque(false);
        chartsGrid.setPreferredSize(new Dimension(0, 300));

        JPanel chartBox1 = createRoundedPanel();
        chartBox1.setLayout(new BorderLayout());
        chartBox1.setBorder(new EmptyBorder(15, 15, 15, 15));
        chartBox1.add(new CompactChartPanel(1, "PLATFORM TRADE VOLUME"), BorderLayout.CENTER);

        JPanel chartBox2 = createRoundedPanel();
        chartBox2.setLayout(new BorderLayout());
        chartBox2.setBorder(new EmptyBorder(15, 15, 15, 15));
        chartBox2.add(new CompactChartPanel(2, "USER DEMOGRAPHICS"), BorderLayout.CENTER);

        chartsGrid.add(chartBox1);
        chartsGrid.add(chartBox2);

        // Req #4: IPO Listing (Admin View with extra status flags)
        JPanel ipoContainer = createRoundedPanel();
        ipoContainer.setLayout(new BorderLayout());
        ipoContainer.setBorder(new EmptyBorder(20, 20, 20, 20));
        JLabel ipoTitle = new JLabel("IPO & ASSET MANAGEMENT");
        ipoTitle.setForeground(textPrimary);
        ipoTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        ipoContainer.add(ipoTitle, BorderLayout.NORTH);

        JPanel ipoListPanel = new JPanel();
        ipoListPanel.setLayout(new BoxLayout(ipoListPanel, BoxLayout.Y_AXIS));
        ipoListPanel.setBackground(cardGlass);

        // Admin-specific rows showing Status & Review Status
        ipoListPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        ipoListPanel.add(createAdminIpoRow("Vintage Rolex Daytona", "10,000 Units", "OPEN", "VERIFIED", pastelGreen));
        ipoListPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        ipoListPanel.add(createAdminIpoRow("1985 Air Jordan 1", "5,000 Units", "UPCOMING", "PENDING", pastelBlue));
        ipoListPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        ipoListPanel.add(createAdminIpoRow("Basquiat Original Sketch", "20,000 Units", "CLOSED", "VERIFIED", textMuted));
        ipoListPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        ipoListPanel.add(createAdminIpoRow("Ferrari F40 Fraction", "50,000 Units", "OPEN", "VERIFIED", pastelGreen));

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
    // SIDEBAR NAVIGATION (With Investors List View Link)
    // ==========================================
    class GrainySidebar extends JPanel {
        private boolean isExpanded = false; 
        private final int EXPANDED_WIDTH = 250;
        private final int COLLAPSED_WIDTH = 0; 
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
            logoLabel.setVisible(false); 
            add(logoLabel);

            // Req #3: "Investors List View (shown in different window)"
            String[] navItems = {"Main Dashboard", "Manage IPOs", "Investors List ↗", "Asset Verification", "Settings"};
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
                
                int yPos = (i == navItems.length - 1) ? 650 : 100 + (i * 50);
                navButtons[i].setBounds(20, yPos, 200, 40);
                add(navButtons[i]);
            }
        }

        public void toggleSidebar() {
            if (animator != null && animator.isRunning()) return;
            int targetWidth = isExpanded ? COLLAPSED_WIDTH : EXPANDED_WIDTH;
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
                revalidate();
                repaint();
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
            int w = getWidth(); int h = getHeight();
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

    // --- Aesthetic Helpers ---

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

    private JPanel createAlertRow(String user, String issue, Color color) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        JLabel uLabel = new JLabel(user);
        uLabel.setForeground(textPrimary);
        uLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        JLabel iLabel = new JLabel(issue);
        iLabel.setForeground(color);
        iLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        row.add(uLabel, BorderLayout.WEST);
        row.add(iLabel, BorderLayout.EAST);
        return row;
    }

    // Req #4: Custom Admin Row with Status & Review Flags
    private JPanel createAdminIpoRow(String name, String units, String status, String reviewStatus, Color statusColor) {
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
        JLabel uLabel = new JLabel("Supply: " + units);
        uLabel.setForeground(textMuted);
        uLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        leftCol.add(nLabel); leftCol.add(uLabel);

        JPanel rightCol = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5));
        rightCol.setOpaque(false);
        
        JLabel revLabel = new JLabel("[" + reviewStatus + "]");
        revLabel.setForeground(textMuted);
        revLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        
        JLabel statLabel = new JLabel(status);
        statLabel.setForeground(statusColor); 
        statLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        
        JButton editBtn = new JButton("Manage");
        editBtn.setBackground(borderSubtle);
        editBtn.setForeground(textPrimary); 
        editBtn.setFocusPainted(false);
        editBtn.setFont(new Font("SansSerif", Font.PLAIN, 11));

        rightCol.add(revLabel);
        rightCol.add(statLabel);
        rightCol.add(editBtn);

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
        SwingUtilities.invokeLater(() -> new AdminDashUI().setVisible(true));
    }
}