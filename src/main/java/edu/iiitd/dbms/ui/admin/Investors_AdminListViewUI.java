package edu.iiitd.dbms.ui.admin;

import edu.iiitd.dbms.auth.LoginManager;

import edu.iiitd.dbms.data_access.InvestorDAO;
import edu.iiitd.dbms.domain.Investor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.stream.Collectors;

public class Investors_AdminListViewUI extends JFrame {

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

    private JPanel investorListPanel;
    private AestheticSearchBar searchField;
    private JLabel totalInvestorsLabel;
    private List<Investor> allInvestors;

    private final InvestorDAO investorDAO = new InvestorDAO();

    public Investors_AdminListViewUI() {
        setTitle("Fractional. - Admin: Investors");
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
        searchField = new AestheticSearchBar("🔍 Search by name or email...");
        searchField.setPreferredSize(new Dimension(320, 40));
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) { filterInvestors(searchField.getText()); }
        });
        searchContainer.add(searchField);

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

        // Header row
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.setMaximumSize(new Dimension(1350, 60));
        headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel pageTitle = new JLabel("REGISTERED INVESTORS");
        pageTitle.setForeground(textPrimary);
        pageTitle.setFont(new Font("SansSerif", Font.BOLD, 22));

        totalInvestorsLabel = new JLabel("Loading...");
        totalInvestorsLabel.setForeground(textMuted);
        totalInvestorsLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));

        headerRow.add(pageTitle, BorderLayout.WEST);
        headerRow.add(totalInvestorsLabel, BorderLayout.EAST);

        // Table card
        JPanel tableCard = createRoundedPanel();
        tableCard.setLayout(new BorderLayout());
        tableCard.setBorder(new EmptyBorder(25, 30, 30, 30));
        tableCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Column headers
        JPanel colHeader = createTableHeader();
        tableCard.add(colHeader, BorderLayout.NORTH);

        // Investors list
        investorListPanel = new JPanel();
        investorListPanel.setLayout(new BoxLayout(investorListPanel, BoxLayout.Y_AXIS));
        investorListPanel.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(investorListPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getViewport().setBackground(cardGlass);
        applyCustomScrollbar(scrollPane);

        tableCard.add(scrollPane, BorderLayout.CENTER);

        mainContent.add(headerRow);
        mainContent.add(Box.createRigidArea(new Dimension(0, 20)));
        mainContent.add(tableCard);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(topBar, BorderLayout.NORTH);
        wrapper.add(mainContent, BorderLayout.CENTER);

        add(dragBar, BorderLayout.NORTH);
        add(sidebar, BorderLayout.WEST);
        add(wrapper, BorderLayout.CENTER);

        loadInvestors();
    }

    // --- DATA ---
    private void loadInvestors() {
        try {
            allInvestors = investorDAO.listInvestors();
            totalInvestorsLabel.setText(allInvestors.size() + " total investors registered");
            populateList(allInvestors);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to load investors: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void filterInvestors(String query) {
        if (allInvestors == null) return;
        String q = query.toLowerCase().trim();
        if (q.isEmpty() || q.equals("🔍 search by name or email...")) {
            populateList(allInvestors);
            return;
        }
        List<Investor> filtered = allInvestors.stream()
            .filter(inv -> inv.getName().toLowerCase().contains(q) || inv.getEmail().toLowerCase().contains(q))
            .collect(Collectors.toList());
        populateList(filtered);
    }

    private void populateList(List<Investor> investors) {
        investorListPanel.removeAll();
        if (investors.isEmpty()) {
            JLabel empty = new JLabel("No investors found.");
            empty.setForeground(textMuted);
            empty.setFont(new Font("SansSerif", Font.PLAIN, 14));
            empty.setBorder(new EmptyBorder(20, 5, 20, 5));
            investorListPanel.add(empty);
        } else {
            for (Investor inv : investors) {
                investorListPanel.add(createInvestorRow(inv));
                investorListPanel.add(Box.createRigidArea(new Dimension(0, 4)));
            }
        }
        investorListPanel.revalidate();
        investorListPanel.repaint();
    }

    // --- ROW BUILDERS ---
    private JPanel createTableHeader() {
        JPanel header = new JPanel(new GridLayout(1, 5));
        header.setOpaque(false);
        header.setMaximumSize(new Dimension(1350, 35));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, borderSubtle));
        String[] cols = {"ID", "NAME", "EMAIL", "REGISTERED", "ACTIONS"};
        for (String col : cols) {
            JLabel lbl = new JLabel(col);
            lbl.setForeground(textMuted);
            lbl.setFont(new Font("SansSerif", Font.BOLD, 10));
            header.add(lbl);
        }
        return header;
    }

    private JPanel createInvestorRow(Investor inv) {
        JPanel row = new JPanel(new GridLayout(1, 5));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(1350, 52));
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(35, 35, 35)),
            new EmptyBorder(12, 0, 12, 0)
        ));

        row.add(makeCell("#" + inv.getInvestorId(), textMuted, false));
        row.add(makeCell(inv.getName(), textPrimary, true));
        row.add(makeCell(inv.getEmail(), textMuted, false));
        row.add(makeCell(inv.getRegistrationDate() != null ? inv.getRegistrationDate().toString() : "—", textMuted, false));

        // Actions panel
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        JButton viewBtn = createMiniButton("View", pastelBlue);
        viewBtn.addActionListener(e -> showInvestorDetail(inv));
        actions.add(viewBtn);
        row.add(actions);

        // Row hover effect
        row.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { row.setBackground(new Color(30, 30, 30)); row.setOpaque(true); }
            public void mouseExited(MouseEvent e) { row.setOpaque(false); }
        });

        return row;
    }

    private void showInvestorDetail(Investor inv) {
        JPanel detailPanel = new JPanel();
        detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.Y_AXIS));
        detailPanel.setBackground(cardGlass);
        detailPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        Font titleFont = new Font("SansSerif", Font.BOLD, 13);
        Font valueFont = new Font("SansSerif", Font.PLAIN, 13);

        detailPanel.add(makeDetailRow("Investor ID:", "#" + inv.getInvestorId(), titleFont, valueFont));
        detailPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        detailPanel.add(makeDetailRow("Name:", inv.getName(), titleFont, valueFont));
        detailPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        detailPanel.add(makeDetailRow("Email:", inv.getEmail(), titleFont, valueFont));
        detailPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        detailPanel.add(makeDetailRow("Phone:", inv.getPhone() != null ? inv.getPhone() : "—", titleFont, valueFont));
        detailPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        detailPanel.add(makeDetailRow("Registered:", inv.getRegistrationDate() != null ? inv.getRegistrationDate().toString() : "—", titleFont, valueFont));

        JOptionPane.showMessageDialog(this, detailPanel, "Investor: " + inv.getName(), JOptionPane.PLAIN_MESSAGE);
    }

    private JPanel makeDetailRow(String label, String value, Font labelFont, Font valueFont) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        p.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(labelFont);
        lbl.setForeground(new Color(150, 150, 150));
        JLabel val = new JLabel(value);
        val.setFont(valueFont);
        val.setForeground(new Color(250, 250, 250));
        p.add(lbl);
        p.add(val);
        return p;
    }

    // --- HELPERS ---
    private JLabel makeCell(String text, Color color, boolean bold) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(color);
        lbl.setFont(new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, 13));
        return lbl;
    }

    private JButton createMiniButton(String text, Color fg) {
        JButton btn = new JButton(text);
        btn.setBackground(new Color(35, 35, 35));
        btn.setForeground(fg);
        btn.setFont(new Font("SansSerif", Font.BOLD, 11));
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderSubtle, 1),
            new EmptyBorder(4, 12, 4, 12)
        ));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
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
                navButtons[i].setFont(new Font("SansSerif", i == 2 ? Font.BOLD : Font.PLAIN, 15));
                navButtons[i].setForeground(i == 2 ? textPrimary : textMuted);
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
            navButtons[2].addActionListener(e -> { dispose(); new InvestorsListViewUI().setVisible(true); });
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

    // --- AESTHETIC SEARCH BAR ---
    class AestheticSearchBar extends JTextField {
        public AestheticSearchBar(String placeholder) {
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
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
            super.paintComponent(g);
            g2.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new InvestorsListViewUI().setVisible(true));
    }
}