package edu.iiitd.dbms.ui.admin;

import edu.iiitd.dbms.auth.LoginManager;
import edu.iiitd.dbms.data_access.InvestorDAO;
import edu.iiitd.dbms.data_access.OwnershipDAO;
import edu.iiitd.dbms.domain.Investor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class InvestorsListViewUI extends JFrame {

    private final Color bgAbsoluteDark = new Color(14, 14, 14);
    private final Color cardGlass = new Color(26, 26, 26);
    private final Color borderSubtle = new Color(45, 45, 45);
    private final Color textPrimary = new Color(250, 250, 250);
    private final Color textMuted = new Color(150, 150, 150);
    private final Color pastelGreen = new Color(119, 221, 119);
    private final Color pastelBlue = new Color(174, 198, 207);

    private Point dragPoint;
    private GrainySidebar sidebar;
    private JPanel investorListPanel;
    private AestheticSearchBar searchField;
    private JLabel totalInvestorsLabel;
    private List<Investor> allInvestors;

    private final InvestorDAO investorDAO = new InvestorDAO();

    private String getAdminDisplayName() {
        if (LoginManager.getCurrentUser() != null) {
            String name = LoginManager.getCurrentUser().getName();
            if (name != null && !name.isBlank()) {
                String[] parts = name.trim().split(" ");
                return parts[0].toUpperCase() + (parts.length > 1 ? " " + Character.toUpperCase(parts[parts.length-1].charAt(0)) + "." : "");
            }
        }
        return "ADMIN";
    }

    public InvestorsListViewUI() {
        setTitle("Fractional. - Admin: Investors");
        setSize(1350, 850);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setUndecorated(true);
        setLayout(new BorderLayout());
        getContentPane().setBackground(bgAbsoluteDark);

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
        JLabel profileLabel = new JLabel(getAdminDisplayName());
        profileLabel.setForeground(textPrimary);
        profileLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        profilePanel.add(roleLabel); profilePanel.add(profileLabel);
        topBar.add(searchContainer, BorderLayout.WEST);
        topBar.add(profilePanel, BorderLayout.EAST);

        sidebar = new GrainySidebar();

        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setOpaque(false);
        mainContent.setBorder(new EmptyBorder(20, 40, 40, 40));

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

        JPanel statsRow = new JPanel(new GridLayout(1, 3, 20, 0));
        statsRow.setOpaque(false);
        statsRow.setMaximumSize(new Dimension(1350, 100));
        statsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel totalCard = createStatCard("TOTAL INVESTORS", "—", textPrimary);
        JPanel activeCard = createStatCard("WITH HOLDINGS", "—", pastelGreen);
        JPanel walletCard = createStatCard("WITH POSITIVE BALANCE", "—", pastelBlue);
        statsRow.add(totalCard); statsRow.add(activeCard); statsRow.add(walletCard);

        JPanel tableCard = createRoundedPanel();
        tableCard.setLayout(new BorderLayout());
        tableCard.setBorder(new EmptyBorder(25, 30, 30, 30));
        tableCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        tableCard.add(createTableHeader(), BorderLayout.NORTH);

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
        mainContent.add(Box.createRigidArea(new Dimension(0, 15)));
        mainContent.add(statsRow);
        mainContent.add(Box.createRigidArea(new Dimension(0, 15)));
        mainContent.add(tableCard);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(topBar, BorderLayout.NORTH);
        wrapper.add(mainContent, BorderLayout.CENTER);

        add(dragBar, BorderLayout.NORTH);
        add(sidebar, BorderLayout.WEST);
        add(wrapper, BorderLayout.CENTER);

        loadInvestors(totalCard, activeCard, walletCard);
    }

    private void loadInvestors(JPanel totalCard, JPanel activeCard, JPanel walletCard) {
        try {
            allInvestors = investorDAO.listInvestors();
            totalInvestorsLabel.setText(allInvestors.size() + " total investors registered");
            updateStatCardValue(totalCard, String.valueOf(allInvestors.size()));

            long withBalance = allInvestors.stream().filter(i -> i.getWalletBalance() != null && i.getWalletBalance() > 0).count();
            updateStatCardValue(walletCard, String.valueOf(withBalance));

            try {
                OwnershipDAO ownershipDAO = new OwnershipDAO();
                Set<Integer> activeIds = new HashSet<>(ownershipDAO.getActiveInvestorIds());
                updateStatCardValue(activeCard, String.valueOf(activeIds.size()));
            } catch (Exception ignored) { updateStatCardValue(activeCard, "—"); }

            populateList(allInvestors);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to load investors: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void filterInvestors(String query) {
        if (allInvestors == null) return;
        String q = query.toLowerCase().trim();
        if (q.isEmpty() || q.startsWith("🔍")) { populateList(allInvestors); return; }
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

    private JPanel createTableHeader() {
        JPanel header = new JPanel(new GridLayout(1, 5));
        header.setOpaque(false);
        header.setMaximumSize(new Dimension(1350, 35));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, borderSubtle));
        String[] cols = {"ID", "NAME", "EMAIL", "REGISTERED", "WALLET BALANCE"};
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
        
        row.add(makeCell(inv.getRegistrationDate() != null ? (inv.getRegistrationDate() != null ? inv.getRegistrationDate().toString() : "N/A") : "—", textMuted, false));
        double bal = inv.getWalletBalance() != null ? inv.getWalletBalance() : 0;
        JLabel balLabel = new JLabel(String.format("$%,.2f", bal));
        balLabel.setForeground(bal > 0 ? pastelGreen : textMuted);
        balLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        row.add(balLabel);
        row.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { if (e.getClickCount() == 2) showInvestorDetail(inv); }
            public void mouseEntered(MouseEvent e) { row.setBackground(new Color(30, 30, 30)); row.setOpaque(true); }
            public void mouseExited(MouseEvent e) { row.setOpaque(false); }
        });
        return row;
    }

    private void showInvestorDetail(Investor inv) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(cardGlass);
        p.setBorder(new EmptyBorder(20, 20, 20, 20));
        Font tf = new Font("SansSerif", Font.BOLD, 13);
        Font vf = new Font("SansSerif", Font.PLAIN, 13);
        p.add(makeDetailRow("Investor ID:", "#" + inv.getInvestorId(), tf, vf));
        p.add(Box.createRigidArea(new Dimension(0, 10)));
        p.add(makeDetailRow("Name:", inv.getName(), tf, vf));
        p.add(Box.createRigidArea(new Dimension(0, 10)));
        p.add(makeDetailRow("Email:", inv.getEmail(), tf, vf));
        p.add(Box.createRigidArea(new Dimension(0, 10)));
        p.add(makeDetailRow("Phone:", inv.getPhone() != null ? inv.getPhone() : "—", tf, vf));
        p.add(Box.createRigidArea(new Dimension(0, 10)));
        p.add(makeDetailRow("Registered:", inv.getRegistrationDate() != null ? (inv.getRegistrationDate() != null ? inv.getRegistrationDate().toString() : "N/A") : "—", tf, vf));
        p.add(Box.createRigidArea(new Dimension(0, 10)));
        p.add(makeDetailRow("Wallet:", String.format("$%,.2f", inv.getWalletBalance() != null ? inv.getWalletBalance() : 0), tf, vf));
        JOptionPane.showMessageDialog(this, p, "Investor: " + inv.getName(), JOptionPane.PLAIN_MESSAGE);
    }

    private JPanel makeDetailRow(String label, String value, Font lf, Font vf) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        p.setOpaque(false);
        JLabel l = new JLabel(label); l.setFont(lf); l.setForeground(textMuted);
        JLabel v = new JLabel(value); v.setFont(vf); v.setForeground(textPrimary);
        p.add(l); p.add(v);
        return p;
    }

    private JLabel makeCell(String text, Color color, boolean bold) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(color);
        lbl.setFont(new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, 13));
        return lbl;
    }

    private JPanel createStatCard(String title, String value, Color valueColor) {
        JPanel card = createRoundedPanel();
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(20, 25, 20, 25));
        JLabel tl = new JLabel(title); tl.setForeground(textMuted); tl.setFont(new Font("SansSerif", Font.BOLD, 11));
        JLabel vl = new JLabel(value); vl.setForeground(valueColor); vl.setFont(new Font("SansSerif", Font.BOLD, 28)); vl.setName("VALUE");
        card.add(tl, BorderLayout.NORTH); card.add(vl, BorderLayout.SOUTH);
        return card;
    }

    private void updateStatCardValue(JPanel card, String value) {
        for (Component c : card.getComponents())
            if (c instanceof JLabel && "VALUE".equals(((JLabel) c).getName())) { ((JLabel) c).setText(value); break; }
    }

    private JPanel createRoundedPanel() {
        JPanel panel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(cardGlass); g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20);
                g2.setColor(borderSubtle); g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        return panel;
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
            logoLabel.setFont(new Font("Serif", Font.BOLD, 26));
            logoLabel.setForeground(textPrimary);
            logoLabel.setBounds(25, 20, 200, 40); logoLabel.setVisible(false); add(logoLabel);
            String[] navItems = {"Main Dashboard", "Manage IPOs", "Investors List ↗", "Asset Verification", "Settings"};
            navButtons = new JButton[navItems.length];
            for (int i = 0; i < navItems.length; i++) {
                navButtons[i] = new JButton(navItems[i]);
                navButtons[i].setFont(new Font("SansSerif", i == 2 ? Font.BOLD : Font.PLAIN, 15));
                navButtons[i].setForeground(i == 2 ? textPrimary : textMuted);
                navButtons[i].setContentAreaFilled(false); navButtons[i].setBorderPainted(false);
                navButtons[i].setFocusPainted(false); navButtons[i].setHorizontalAlignment(SwingConstants.LEFT);
                navButtons[i].setCursor(new Cursor(Cursor.HAND_CURSOR)); navButtons[i].setVisible(false);
                navButtons[i].setBounds(20, (i == navItems.length-1) ? 650 : 100+(i*50), 200, 40);
                add(navButtons[i]);
            }
            navButtons[0].addActionListener(e -> { InvestorsListViewUI.this.dispose();; new AdminControlCenter().setVisible(true); });
            navButtons[1].addActionListener(e -> { InvestorsListViewUI.this.dispose(); new AdminAssetsViewUI().setVisible(true); });
            navButtons[2].addActionListener(e -> { InvestorsListViewUI.this.dispose(); new Investors_AdminListViewUI().setVisible(true); });
            navButtons[3].addActionListener(e -> { InvestorsListViewUI.this.dispose(); new AdminAssetsViewUI().setVisible(true); });
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
                currentWidth += step; if (currentWidth < 0) currentWidth = 0; if (currentWidth > EXPANDED_WIDTH) currentWidth = EXPANDED_WIDTH;
                setPreferredSize(new Dimension(currentWidth, 0)); revalidate(); repaint();
                if (currentWidth == targetWidth) { isExpanded = !isExpanded; ((Timer) e.getSource()).stop();
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
                        noiseOverlay.setRGB(x, y, new Color(255, 255, 255, (int)(Math.random()*10)).getRGB());
        }

        @Override protected void paintComponent(Graphics g) {
            if (currentWidth == 0) return;
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            GeneralPath path = new GeneralPath();
            path.moveTo(0, 0); int cd = (int)(25*((double)currentWidth/EXPANDED_WIDTH));
            path.lineTo(getWidth()-cd, 0); path.quadTo(getWidth(), getHeight()/2.0, getWidth()-cd, getHeight());
            path.lineTo(0, getHeight()); path.closePath();
            g2.setColor(new Color(30,30,30,150)); g2.fill(path); g2.setClip(path); g2.drawImage(noiseOverlay,0,0,null);
            g2.dispose();
        }
    }

    class AestheticSearchBar extends JTextField {
        public AestheticSearchBar(String placeholder) {
            setOpaque(false); setForeground(textMuted); setCaretColor(textPrimary);
            setBorder(new EmptyBorder(5,20,5,20)); setFont(new Font("SansSerif",Font.PLAIN,14)); setText(placeholder);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(cardGlass); g2.fillRoundRect(0,0,getWidth()-1,getHeight()-1,20,20);
            super.paintComponent(g); g2.dispose();
        }
    }
}
