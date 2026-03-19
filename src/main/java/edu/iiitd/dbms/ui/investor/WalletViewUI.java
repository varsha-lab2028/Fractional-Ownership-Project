package edu.iiitd.dbms.ui.investor;

import edu.iiitd.dbms.auth.LoginManager;

import edu.iiitd.dbms.data_access.InvestorDAO;
import edu.iiitd.dbms.data_access.WalletTransactionDAO;
import edu.iiitd.dbms.dto.WalletTransactionDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.util.List;

public class WalletViewUI extends JFrame {

    // Grayscale & Pastel Palette (Matched to InvestorDashUI)
    private final Color bgAbsoluteDark = new Color(14, 14, 14); 
    private final Color cardGlass = new Color(26, 26, 26);      
    private final Color borderSubtle = new Color(45, 45, 45);   
    private final Color textPrimary = new Color(250, 250, 250); 
    private final Color textMuted = new Color(150, 150, 150);   
    private final Color pastelGreen = new Color(119, 221, 119); 
    private final Color pastelRed = new Color(255, 105, 97);    

    private Point dragPoint;
    private GrainySidebar sidebar;
    private JPanel transactionListPanel;
    private JLabel balanceLabel;

    // Data Access
    private int currentInvestorId = LoginManager.getCurrentUser() != null ? LoginManager.getCurrentUser().getLinkedId() : 1; // Assuming Aman for now
    private final InvestorDAO investorDAO = new InvestorDAO();
    private final WalletTransactionDAO walletDAO = new WalletTransactionDAO();

    private String getDisplayName() {
        if (LoginManager.getCurrentUser() == null) return "INVESTOR";
        String full = LoginManager.getCurrentUser().getName();
        if (full == null || full.isBlank()) return "INVESTOR";
        String[] p = full.trim().split("\s+");
        return p[0].toUpperCase() + (p.length > 1 ? " " + Character.toUpperCase(p[p.length-1].charAt(0)) + "." : "");
    }

    public WalletViewUI() {
        setTitle("Fractional. - Wallet & Ledger");
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

        // FIX: Added the searchContainer structure back so it aligns properly
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

        // --- SIDEBAR ---
        sidebar = new GrainySidebar();

        // --- MAIN CONTENT AREA ---
        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setOpaque(false);
        mainContent.setBorder(new EmptyBorder(20, 40, 40, 40));

        // 1. TOP SECTION: BALANCE & ACTIONS
        JPanel balanceCard = createRoundedPanel();
        balanceCard.setLayout(new BorderLayout());
        balanceCard.setBorder(new EmptyBorder(40, 40, 40, 40));
        balanceCard.setMaximumSize(new Dimension(1350, 180));

        JPanel balanceInfo = new JPanel(new GridLayout(2, 1));
        balanceInfo.setOpaque(false);
        JLabel balTitle = new JLabel("IMMEDIATE FUNDS");
        balTitle.setForeground(textMuted);
        balTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        
        balanceLabel = new JLabel("$0.00"); 
        balanceLabel.setForeground(textPrimary);
        balanceLabel.setFont(new Font("SansSerif", Font.BOLD, 48));
        
        balanceInfo.add(balTitle);
        balanceInfo.add(balanceLabel);

        JPanel actionButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 20));
        actionButtons.setOpaque(false);
        
        JButton withdrawBtn = createActionButton("Withdraw", cardGlass, textPrimary, true);
        JButton depositBtn = createActionButton("Deposit Funds", pastelGreen, bgAbsoluteDark, false);
        
        actionButtons.add(withdrawBtn);
        actionButtons.add(depositBtn);

        depositBtn.addActionListener(e -> {
            String input = JOptionPane.showInputDialog(WalletViewUI.this, "Enter deposit amount ($):", "Deposit Funds", JOptionPane.QUESTION_MESSAGE);
            if (input != null && !input.isBlank()) {
                try {
                    double amount = Double.parseDouble(input.trim());
                    if (amount <= 0) { JOptionPane.showMessageDialog(WalletViewUI.this, "Amount must be positive.", "Error", JOptionPane.ERROR_MESSAGE); return; }
                    walletDAO.deposit(currentInvestorId, amount);
                    JOptionPane.showMessageDialog(WalletViewUI.this, String.format("Successfully deposited $%,.2f!", amount), "Deposit Successful", JOptionPane.INFORMATION_MESSAGE);
                    loadWalletData();
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(WalletViewUI.this, "Please enter a valid number.", "Error", JOptionPane.ERROR_MESSAGE);
                } catch (Exception ex) { JOptionPane.showMessageDialog(WalletViewUI.this, "Deposit failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); }
            }
        });

        withdrawBtn.addActionListener(e -> {
            String input = JOptionPane.showInputDialog(WalletViewUI.this, "Enter withdrawal amount ($):", "Withdraw", JOptionPane.QUESTION_MESSAGE);
            if (input != null && !input.isBlank()) {
                try {
                    double amount = Double.parseDouble(input.trim());
                    if (amount <= 0) { JOptionPane.showMessageDialog(WalletViewUI.this, "Amount must be positive.", "Error", JOptionPane.ERROR_MESSAGE); return; }
                    walletDAO.withdraw(currentInvestorId, amount);
                    JOptionPane.showMessageDialog(WalletViewUI.this, String.format("Successfully withdrew $%,.2f!", amount), "Withdrawal Successful", JOptionPane.INFORMATION_MESSAGE);
                    loadWalletData();
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(WalletViewUI.this, "Please enter a valid number.", "Error", JOptionPane.ERROR_MESSAGE);
                } catch (Exception ex) { JOptionPane.showMessageDialog(WalletViewUI.this, "Withdrawal failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); }
            }
        });

        balanceCard.add(balanceInfo, BorderLayout.WEST);
        balanceCard.add(actionButtons, BorderLayout.EAST);

        // 2. BOTTOM SECTION: TRANSACTION LEDGER
        JPanel ledgerCard = createRoundedPanel();
        ledgerCard.setLayout(new BorderLayout());
        ledgerCard.setBorder(new EmptyBorder(30, 30, 30, 30));

        JLabel ledgerTitle = new JLabel("TRANSACTION HISTORY & TRANSFERS");
        ledgerTitle.setForeground(textPrimary);
        ledgerTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        ledgerCard.add(ledgerTitle, BorderLayout.NORTH);

        transactionListPanel = new JPanel();
        transactionListPanel.setLayout(new BoxLayout(transactionListPanel, BoxLayout.Y_AXIS));
        transactionListPanel.setOpaque(false); // FIX: Transparent to avoid white flash

        JScrollPane scrollPane = new JScrollPane(transactionListPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        // FIX: The following 3 lines strictly remove the white bar glitch on Mac
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false); 
        scrollPane.getViewport().setBackground(cardGlass);
        applyCustomScrollbar(scrollPane);

        ledgerCard.add(scrollPane, BorderLayout.CENTER);

        // Assembly
        mainContent.add(balanceCard);
        mainContent.add(Box.createRigidArea(new Dimension(0, 30)));
        mainContent.add(ledgerCard);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(topBar, BorderLayout.NORTH);
        wrapper.add(mainContent, BorderLayout.CENTER);

        add(dragBar, BorderLayout.NORTH);
        add(sidebar, BorderLayout.WEST);
        add(wrapper, BorderLayout.CENTER);

        loadWalletData();
    }

    // --- DATA LOADING ---
    private void loadWalletData() {
        try {
            double balance = investorDAO.getWalletBalance(currentInvestorId);
            balanceLabel.setText(String.format("$%,.2f", balance));

            List<WalletTransactionDTO> transactions = walletDAO.getTransactionHistory(currentInvestorId);
            transactionListPanel.removeAll();

            if (transactions.isEmpty()) {
                JLabel empty = new JLabel("No recent transactions.");
                empty.setForeground(textMuted);
                empty.setFont(new Font("SansSerif", Font.PLAIN, 14));
                empty.setBorder(new EmptyBorder(20, 10, 0, 0));
                transactionListPanel.add(empty);
            } else {
                for (WalletTransactionDTO tx : transactions) {
                    transactionListPanel.add(createTransactionRow(
                        tx.getTransactionType(),
                        tx.getTransferCategory(),
                        tx.getTransactionDate().toString().substring(0, 10), 
                        tx.getAmount()
                    ));
                    transactionListPanel.add(Box.createRigidArea(new Dimension(0, 10)));
                }
            }
            transactionListPanel.revalidate();
            transactionListPanel.repaint();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- AESTHETIC HELPERS ---
    private JPanel createTransactionRow(String type, String category, String date, double amount) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(1350, 70));
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, borderSubtle),
            new EmptyBorder(15, 10, 15, 10)
        ));

        JPanel leftCol = new JPanel(new GridLayout(2, 1, 0, 5));
        leftCol.setOpaque(false);
        JLabel typeLabel = new JLabel(type.replace("_", " "));
        typeLabel.setForeground(textPrimary);
        typeLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        
        JLabel catLabel = new JLabel("Category: " + category);
        catLabel.setForeground(textMuted);
        catLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        
        leftCol.add(typeLabel); leftCol.add(catLabel);

        JLabel dateLabel = new JLabel(date, SwingConstants.CENTER);
        dateLabel.setForeground(textMuted);
        dateLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));

        boolean isPositive = amount >= 0;
        JLabel amtLabel = new JLabel((isPositive ? "+" : "") + String.format("$%,.2f", amount), SwingConstants.RIGHT);
        amtLabel.setForeground(isPositive ? pastelGreen : textPrimary);
        amtLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        row.add(leftCol, BorderLayout.WEST);
        row.add(dateLabel, BorderLayout.CENTER);
        row.add(amtLabel, BorderLayout.EAST);
        return row;
    }

    // FIX: Added setOpaque(true) and setBorderPainted(false) to force Mac to render the background colors
    private JButton createActionButton(String text, Color bg, Color fg, boolean isOutline) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setFocusPainted(false);
        btn.setOpaque(true); // REQUIRED FOR MAC
        btn.setBorderPainted(false); // REQUIRED FOR MAC
        
        if (isOutline) {
            btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderSubtle, 1),
                new EmptyBorder(12, 25, 12, 25)
            ));
        } else {
            btn.setBorder(new EmptyBorder(12, 25, 12, 25));
        }
        
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
                add(navButtons[i]);}
            navButtons[0].addActionListener(e -> { WalletViewUI.this.dispose(); new InvestorDashUI().setVisible(true); });
            navButtons[1].addActionListener(e -> { WalletViewUI.this.dispose(); new MarketViewUI_updated().setVisible(true); });
            navButtons[2].addActionListener(e -> { WalletViewUI.this.dispose(); new WalletViewUI().setVisible(true); });
            navButtons[3].addActionListener(e -> { WalletViewUI.this.dispose(); new HoldingsViewUI_updated().setVisible(true); });
            navButtons[4].addActionListener(e -> { WalletViewUI.this.dispose(); new ProfileSettingsUI().setVisible(true); });}
        
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
        SwingUtilities.invokeLater(() -> new WalletViewUI().setVisible(true));
    }
}