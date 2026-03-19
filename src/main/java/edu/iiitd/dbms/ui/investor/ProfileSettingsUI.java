package edu.iiitd.dbms.ui.investor;

import edu.iiitd.dbms.auth.LoginManager;

import edu.iiitd.dbms.data_access.InvestorDAO;
import edu.iiitd.dbms.domain.Investor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;

public class ProfileSettingsUI extends JFrame {

    private final Color bgAbsoluteDark = new Color(14, 14, 14); 
    private final Color cardGlass = new Color(26, 26, 26);      
    private final Color borderSubtle = new Color(45, 45, 45);   
    private final Color textPrimary = new Color(250, 250, 250); 
    private final Color textMuted = new Color(150, 150, 150);   
    private final Color pastelGreen = new Color(119, 221, 119); 
    private final Color pastelRed = new Color(255, 105, 97);

    private Point dragPoint;
    private GrainySidebar sidebar;

    // Fields
    private JTextField nameField;
    private JTextField phoneField;
    private JLabel emailLabel;
    private JLabel memberSinceLabel;

    private int currentInvestorId = LoginManager.getCurrentUser() != null ? LoginManager.getCurrentUser().getLinkedId() : 1; // Testing with ID 1
    private final InvestorDAO investorDAO = new InvestorDAO();

    private String getDisplayName() {
        if (LoginManager.getCurrentUser() == null) return "INVESTOR";
        String full = LoginManager.getCurrentUser().getName();
        if (full == null || full.isBlank()) return "INVESTOR";
        String[] p = full.trim().split("\s+");
        return p[0].toUpperCase() + (p.length > 1 ? " " + Character.toUpperCase(p[p.length-1].charAt(0)) + "." : "");
    }

    public ProfileSettingsUI() {
        setTitle("Fractional. - Profile Settings");
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

        JPanel navLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        navLeft.setOpaque(false);
        navLeft.add(toggleBtn);

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

        topBar.add(navLeft, BorderLayout.WEST);
        topBar.add(profilePanel, BorderLayout.EAST);

        sidebar = new GrainySidebar();

        // --- MAIN CONTENT AREA ---
        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setOpaque(false);
        mainContent.setBorder(new EmptyBorder(40, 80, 40, 80));

        JLabel pageTitle = new JLabel("ACCOUNT SETTINGS");
        pageTitle.setForeground(textPrimary);
        pageTitle.setFont(new Font("SansSerif", Font.BOLD, 24));
        pageTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel settingsCard = createRoundedPanel();
        settingsCard.setLayout(new BoxLayout(settingsCard, BoxLayout.Y_AXIS));
        settingsCard.setBorder(new EmptyBorder(40, 40, 40, 40));
        settingsCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        settingsCard.setMaximumSize(new Dimension(800, 500));

        // Form Fields
        nameField = createInputField();
        phoneField = createInputField();
        emailLabel = new JLabel("loading...");
        emailLabel.setForeground(textMuted);
        emailLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
        
        memberSinceLabel = new JLabel("Member Since: loading...");
        memberSinceLabel.setForeground(pastelGreen);
        memberSinceLabel.setFont(new Font("SansSerif", Font.BOLD, 12));

        settingsCard.add(memberSinceLabel);
        settingsCard.add(Box.createRigidArea(new Dimension(0, 30)));
        
        settingsCard.add(createInputLabel("FULL NAME"));
        settingsCard.add(Box.createRigidArea(new Dimension(0, 5)));
        settingsCard.add(nameField);
        settingsCard.add(Box.createRigidArea(new Dimension(0, 20)));

        settingsCard.add(createInputLabel("PHONE NUMBER"));
        settingsCard.add(Box.createRigidArea(new Dimension(0, 5)));
        settingsCard.add(phoneField);
        settingsCard.add(Box.createRigidArea(new Dimension(0, 20)));

        settingsCard.add(createInputLabel("EMAIL ADDRESS (UNEDITABLE)"));
        settingsCard.add(Box.createRigidArea(new Dimension(0, 5)));
        settingsCard.add(emailLabel);
        settingsCard.add(Box.createRigidArea(new Dimension(0, 40)));

        // Action Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        btnPanel.setOpaque(false);
        
        JButton saveBtn = new JButton("Save Changes");
        styleActionButton(saveBtn, pastelGreen, bgAbsoluteDark, false);
        
        JButton logoutBtn = new JButton("Sign Out");
        styleActionButton(logoutBtn, cardGlass, pastelRed, true);
        logoutBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(ProfileSettingsUI.this, "Are you sure you want to sign out?", "Sign Out", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                edu.iiitd.dbms.auth.LoginManager.logout();
                dispose();
                new edu.iiitd.dbms.ui.login.logintesterUI().setVisible(true);
            }
        });
        
        btnPanel.add(saveBtn);
        btnPanel.add(Box.createRigidArea(new Dimension(20, 0)));
        btnPanel.add(logoutBtn);

        settingsCard.add(btnPanel);

        // Update Logic
        saveBtn.addActionListener(e -> {
            try {
                boolean success = investorDAO.updateInvestorProfile(currentInvestorId, nameField.getText(), null);
                if (success) {
                    JOptionPane.showMessageDialog(this, "Profile updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to update: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        mainContent.add(pageTitle);
        mainContent.add(Box.createRigidArea(new Dimension(0, 30)));
        mainContent.add(settingsCard);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(topBar, BorderLayout.NORTH);
        wrapper.add(mainContent, BorderLayout.CENTER);

        add(dragBar, BorderLayout.NORTH);
        add(sidebar, BorderLayout.WEST);
        add(wrapper, BorderLayout.CENTER);

        loadProfileData();
    }

    private void loadProfileData() {
        try {
            Investor user = investorDAO.findByInvestorId(currentInvestorId);
            if (user != null) {
                nameField.setText(user.getName());
                emailLabel.setText(user.getEmail());
                if (user.getPhone() != null) phoneField.setText(user.getPhone()); else phoneField.setText("N/A");
                memberSinceLabel.setText("MEMBER SINCE: " + (user.getRegistrationDate() != null ? user.getRegistrationDate().toString() : "N/A"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- AESTHETIC HELPERS ---
    private JLabel createInputLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(textMuted);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
        return lbl;
    }

    private JTextField createInputField() {
        JTextField field = new JTextField();
        field.setMaximumSize(new Dimension(400, 40));
        field.setPreferredSize(new Dimension(400, 40));
        field.setBackground(bgAbsoluteDark);
        field.setForeground(textPrimary);
        field.setCaretColor(textPrimary);
        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(borderSubtle, 1),
                new EmptyBorder(5, 15, 5, 15)
        ));
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        return field;
    }

    private void styleActionButton(JButton btn, Color bg, Color fg, boolean isOutline) {
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        if (isOutline) {
            btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderSubtle, 1),
                new EmptyBorder(10, 20, 10, 20)
            ));
        } else {
            btn.setBorder(new EmptyBorder(10, 20, 10, 20));
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
                navButtons[i].setFont(new Font("SansSerif", i == 4 ? Font.BOLD : Font.PLAIN, 15)); 
                navButtons[i].setForeground(i == 4 ? textPrimary : textMuted);
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
        

            navButtons[0].addActionListener(e -> { ProfileSettingsUI.this.dispose(); new edu.iiitd.dbms.ui.investor.InvestorDashUI().setVisible(true); });
            navButtons[1].addActionListener(e -> { ProfileSettingsUI.this.dispose(); new edu.iiitd.dbms.ui.investor.MarketViewUI_updated().setVisible(true); });
            navButtons[2].addActionListener(e -> { ProfileSettingsUI.this.dispose(); new edu.iiitd.dbms.ui.investor.WalletViewUI().setVisible(true); });
            navButtons[3].addActionListener(e -> { ProfileSettingsUI.this.dispose(); new edu.iiitd.dbms.ui.investor.HoldingsViewUI_updated().setVisible(true); });
            navButtons[4].addActionListener(e -> { ProfileSettingsUI.this.dispose(); new edu.iiitd.dbms.ui.investor.ProfileSettingsUI().setVisible(true); });}
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
        SwingUtilities.invokeLater(() -> new ProfileSettingsUI().setVisible(true));
    }
}