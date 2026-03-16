package edu.iiitd.dbms.ui.login;

import edu.iiitd.dbms.auth.AuthenticationService;
import edu.iiitd.dbms.domain.AuthClass;
import edu.iiitd.dbms.ui.investor.InvestorDashUI;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class logintesterUI extends JFrame {
    private final Color bgDark = new Color(15, 15, 15);
    private final Color panelDark = new Color(25, 25, 25);
    private final Color accentCrimson = new Color(139, 0, 0); 
    private final Color textMuted = new Color(150, 150, 150);
    private final Color textLight = new Color(240, 240, 240);

    private final AuthenticationService authService = new AuthenticationService();

    // FIXED: Constructor name must match class name
    public logintesterUI() {
        setTitle("Fractional - Access Portal");
        setSize(900, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setUndecorated(true); 
        setLayout(new BorderLayout());

        // LEFT PANEL: Logo & Branding
        JPanel leftPanel = new JPanel();
        leftPanel.setBackground(bgDark);
        leftPanel.setPreferredSize(new Dimension(400, 550));
        leftPanel.setLayout(new GridBagLayout());

        JLabel logoLabel = new JLabel("FRACTIONAL");
        logoLabel.setFont(new Font("Serif", Font.BOLD | Font.ITALIC, 38));
        logoLabel.setForeground(textLight);

        JLabel subText = new JLabel("Modern Asset Ownership");
        subText.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subText.setForeground(textMuted);

        GridBagConstraints gbcLeft = new GridBagConstraints();
        gbcLeft.gridx = 0; gbcLeft.gridy = 0;
        leftPanel.add(logoLabel, gbcLeft);
        gbcLeft.gridy = 1; gbcLeft.insets = new Insets(10, 0, 0, 0);
        leftPanel.add(subText, gbcLeft);

        // RIGHT PANEL: Login Form
        JPanel rightPanel = new JPanel();
        rightPanel.setBackground(panelDark);
        rightPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel welcomeLabel = new JLabel("Welcome Back");
        welcomeLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        welcomeLabel.setForeground(textLight);

        JPanel rolePanel = new JPanel(new GridLayout(1, 2, 10, 0));
        rolePanel.setBackground(panelDark);
        
        JRadioButton investorBtn = new JRadioButton("Investor");
        JRadioButton adminBtn = new JRadioButton("Admin");
        styleRadioButton(investorBtn, true); 
        styleRadioButton(adminBtn, false);

        ButtonGroup roleGroup = new ButtonGroup();
        roleGroup.add(investorBtn);
        roleGroup.add(adminBtn);
        
        rolePanel.add(investorBtn);
        rolePanel.add(adminBtn);

        JLabel userLabel = new JLabel("EMAIL");
        styleLabel(userLabel);
        JTextField userField = new JTextField();
        styleTextField(userField);

        JLabel passLabel = new JLabel("PASSWORD");
        styleLabel(passLabel);
        JPasswordField passField = new JPasswordField();
        styleTextField(passField);

        JButton loginBtn = new JButton("ACCESS ACCOUNT");
        loginBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        loginBtn.setBackground(textLight);
        loginBtn.setForeground(Color.BLACK);
        loginBtn.setFocusPainted(false);
        loginBtn.setOpaque(true); 
        loginBtn.setBorderPainted(false); 
        loginBtn.setBorder(new EmptyBorder(12, 0, 12, 0));
        loginBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        loginBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                loginBtn.setBackground(accentCrimson);
                loginBtn.setForeground(Color.WHITE);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                loginBtn.setBackground(textLight);
                loginBtn.setForeground(Color.BLACK);
            }
        });

        loginBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String email = userField.getText().trim();
                String password = new String(passField.getPassword()).trim();
                String selectedRole = investorBtn.isSelected() ? "INVESTOR" : "ADMIN";

                if (email.isEmpty() || password.isEmpty()) {
                    JOptionPane.showMessageDialog(rightPanel, "Enter credentials.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                try {
                    AuthClass loggedInUser = authService.login(email, password);
                    if (!loggedInUser.getUserType().equalsIgnoreCase(selectedRole)) {
                        throw new Exception("Role mismatch.");
                    }

                    dispose(); 
                    
                    if (loggedInUser.getUserType().equalsIgnoreCase("ADMIN")) {
                        // Routing to Admin will happen here once AdminDashUI is ready
                    } else {
                        // Routing to Investor Dashboard
                        new InvestorDashUI().setVisible(true);
                    }

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(rightPanel, ex.getMessage(), "Login Failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(10, 40, 30, 40); 
        rightPanel.add(welcomeLabel, gbc);
        gbc.gridy = 1; gbc.insets = new Insets(5, 40, 15, 40); rightPanel.add(rolePanel, gbc);
        gbc.gridy = 2; gbc.insets = new Insets(5, 40, 5, 40); rightPanel.add(userLabel, gbc);
        gbc.gridy = 3; gbc.insets = new Insets(0, 40, 20, 40); rightPanel.add(userField, gbc);
        gbc.gridy = 4; gbc.insets = new Insets(5, 40, 5, 40); rightPanel.add(passLabel, gbc);
        gbc.gridy = 5; gbc.insets = new Insets(0, 40, 30, 40); rightPanel.add(passField, gbc);
        gbc.gridy = 6; gbc.insets = new Insets(10, 40, 10, 40); rightPanel.add(loginBtn, gbc);

        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.CENTER);
    }

    private void styleLabel(JLabel label) {
        label.setFont(new Font("SansSerif", Font.BOLD, 10));
        label.setForeground(textMuted);
    }

    private void styleTextField(JTextField field) {
        field.setPreferredSize(new Dimension(300, 40));
        field.setBackground(bgDark);
        field.setForeground(textLight);
        field.setCaretColor(textLight);
        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(60, 60, 60), 1),
                new EmptyBorder(5, 10, 5, 10)
        ));
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
    }

    private void styleRadioButton(JRadioButton btn, boolean selected) {
        btn.setBackground(panelDark);
        btn.setForeground(textLight);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 14));
        btn.setFocusPainted(false);
        btn.setSelected(selected);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    public static void main(String[] args) {
        // FIXED: Launching logintesterUI correctly
        SwingUtilities.invokeLater(() -> new logintesterUI().setVisible(true));
    }
}