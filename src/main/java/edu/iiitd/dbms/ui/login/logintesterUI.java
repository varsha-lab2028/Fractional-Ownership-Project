package edu.iiitd.dbms.ui.login;

import edu.iiitd.dbms.auth.AuthenticationService;
import edu.iiitd.dbms.domain.AuthClass;
import edu.iiitd.dbms.ui.investor.InvestorDashUI;
import edu.iiitd.dbms.ui.admin.AdminDashUI;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

public class logintesterUI extends JFrame {

    private static final Color BG_LEFT   = new Color(12,  12,  12);
    private static final Color BG_RIGHT  = new Color(17,  17,  19);
    private static final Color TEXT_HI   = new Color(240, 240, 240);
    private static final Color TEXT_MID  = new Color(120, 120, 120);
    private static final Color TEXT_DIM  = new Color(46,  46,  46);
    private static final Color DIVIDER   = new Color(37,  37,  37);
    private static final Color CRIMSON   = new Color(139, 0,   0);
    private static final Color PILL_ACT  = new Color(240, 240, 240);
    private static final Color PILL_IDLE = new Color(255, 255, 255, 15);
    private static final Color FLD_BG    = new Color(255, 255, 255, 13);
    private static final Color FLD_BD    = new Color(255, 255, 255, 23);
    private static final Color FLD_BD_HI = new Color(255, 255, 255, 55);

    private final AuthenticationService authService = new AuthenticationService();
    private Point dragPoint;
    private boolean investorActive = true;
    private JPanel investorPill, adminPill;

    public logintesterUI() {
        setTitle("Fractional");
        setSize(900, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setUndecorated(true);
        setResizable(false);

        // ── ROOT ─────────────────────────────────────────────────────────────
        JLayeredPane root = new JLayeredPane();
        root.setPreferredSize(new Dimension(900, 550));
        setContentPane(root);

        // ── LEFT PANEL ───────────────────────────────────────────────────────
        JPanel left = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(BG_LEFT);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Faint right-edge separator line with gradient fade
                for (int i = 0; i < getHeight(); i++) {
                    double t = (double) i / getHeight();
                    // fade in from 15% and out from 85%
                    double fade = t < 0.15 ? t / 0.15 : t > 0.85 ? (1 - t) / 0.15 : 1.0;
                    int alpha = (int)(38 * fade);
                    g2.setColor(new Color(60, 60, 60, Math.max(0, alpha)));
                    g2.drawLine(getWidth() - 1, i, getWidth() - 1, i);
                }
                g2.dispose();
            }
        };
        left.setOpaque(false);
        left.setBounds(0, 0, 378, 550);
        root.add(left, JLayeredPane.DEFAULT_LAYER);

        // Logo
        JLabel logo = new JLabel("FRACTIONAL");
        logo.setFont(new Font("Serif", Font.BOLD | Font.ITALIC, 36));
        logo.setForeground(TEXT_HI);
        logo.setBounds(44, 186, 290, 48);
        left.add(logo);

        JLabel tagline = new JLabel("Modern Asset Ownership");
        tagline.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tagline.setForeground(TEXT_MID);
        tagline.setBounds(46, 234, 260, 20);
        left.add(tagline);

        // Short separator line under tagline
        JPanel sepLine = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                GradientPaint gp = new GradientPaint(
                    0, 0, new Color(255, 255, 255, 40),
                    120, 0, new Color(255, 255, 255, 0));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), 1);
                g2.dispose();
            }
        };
        sepLine.setOpaque(false);
        sepLine.setBounds(46, 268, 120, 1);
        left.add(sepLine);

        // Three punchy brand lines — tone, not data
        String[] lines = {
            "The future of asset investing."
        };
        for (int i = 0; i < lines.length; i++) {
            JLabel l = new JLabel(lines[i]);
            l.setFont(new Font("SansSerif", Font.PLAIN, 12));
            int alpha = 110 - i * 28;
            l.setForeground(new Color(200, 200, 200, alpha));
            l.setBounds(46, 282 + i * 22, 300, 18);
            left.add(l);
        }

        // ── RIGHT PANEL ──────────────────────────────────────────────────────
        JPanel right = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();

                // Base fill — slightly lighter than left, noticeably different
                g2.setColor(BG_RIGHT);
                g2.fillRect(0, 0, w, h);

                // Horizontal scan lines — very faint texture, key to the vibe
                for (int y = 0; y < h; y += 4) {
                    g2.setColor(new Color(255, 255, 255, 3));
                    g2.drawLine(0, y, w, y);
                }

                // Top-edge light bleed — like a screen backlight
                for (int i = 0; i < 140; i++) {
                    int alpha = (int)(22 * Math.pow(1.0 - (double) i / 140, 2));
                    g2.setColor(new Color(255, 255, 255, Math.max(0, alpha)));
                    g2.drawLine(0, i, w, i);
                }

                // Subtle bottom vignette
                for (int i = 0; i < 100; i++) {
                    int alpha = (int)(30 * ((double) i / 100));
                    g2.setColor(new Color(0, 0, 0, alpha));
                    g2.drawLine(0, h - 100 + i, w, h - 100 + i);
                }

                g2.dispose();
            }
        };
        right.setOpaque(false);
        right.setBounds(378, 0, 522, 550);
        root.add(right, JLayeredPane.DEFAULT_LAYER);

        // ── WINDOW TRAFFIC LIGHTS — float top-right, no bar ──────────────────
        // macOS-style coloured circles
        int[] dotX = {right.getX() + right.getWidth() - 76,
                      right.getX() + right.getWidth() - 52,
                      right.getX() + right.getWidth() - 28};
        Color[] dotC = {new Color(255, 95,  86),   // red   = close
                        new Color(255, 189, 46),   // amber = min
                        new Color(40,  200, 64)};  // green = (unused, decorative)
        String[] dotTip = {"✕", "—", "+"};

        for (int i = 0; i < 3; i++) {
            final int idx = i;
            JPanel dot = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(dotC[idx]);
                    g2.fillOval(0, 0, 12, 12);
                    g2.dispose();
                }
            };
            dot.setOpaque(false);
            dot.setBounds(dotX[i], 16, 12, 12);
            dot.setCursor(new Cursor(Cursor.HAND_CURSOR));
            dot.setToolTipText(dotTip[i]);
            final int fi = i;
            dot.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (fi == 0) System.exit(0);
                    else if (fi == 1) setState(Frame.ICONIFIED);
                }
            });
            root.add(dot, JLayeredPane.MODAL_LAYER);
        }

        // Drag from the left panel or right panel background
        MouseAdapter dragger = new MouseAdapter() {
            public void mousePressed(MouseEvent e)  { dragPoint = SwingUtilities.convertPoint((Component)e.getSource(), e.getPoint(), root); }
            public void mouseDragged(MouseEvent e)  {
                Point p = SwingUtilities.convertPoint((Component)e.getSource(), e.getPoint(), root);
                setLocation(getLocation().x + p.x - dragPoint.x, getLocation().y + p.y - dragPoint.y);
                dragPoint = p;
            }
        };
        left.addMouseListener(dragger);
        left.addMouseMotionListener(dragger);
        right.addMouseListener(dragger);
        right.addMouseMotionListener(dragger);

        // ── FORM CONTENT ─────────────────────────────────────────────────────
        int lx = 56, fw = 410, cy = 82;

        // Welcome Back
        JLabel welcome = new JLabel("Welcome Back");
        welcome.setFont(new Font("SansSerif", Font.BOLD, 26));
        welcome.setForeground(TEXT_HI);
        welcome.setBounds(lx, cy, 380, 36);
        right.add(welcome);

        JLabel sub = new JLabel("Sign in to your account to continue");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 12));
        sub.setForeground(TEXT_MID);
        sub.setBounds(lx, cy + 40, 360, 18);
        right.add(sub);
        cy += 82;

        // ── PILLS ─────────────────────────────────────────────────────────────
        int pillW = (fw - 12) / 2;
        investorPill = buildPill("Investor");
        adminPill    = buildPill("Admin");
        investorPill.setBounds(lx,             cy, pillW, 42);
        adminPill   .setBounds(lx + pillW + 12, cy, pillW, 42);
        setPill(investorPill, true);
        setPill(adminPill, false);
        right.add(investorPill);
        right.add(adminPill);

        investorPill.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { investorActive = true;  setPill(investorPill,true);  setPill(adminPill,false); }
        });
        adminPill.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { investorActive = false; setPill(investorPill,false); setPill(adminPill,true);  }
        });
        cy += 60;

        // EMAIL
        right.add(fieldLabel("EMAIL ADDRESS", lx, cy));
        cy += 18;
        JTextField emailField = glassField(false);
        emailField.setBounds(lx, cy, fw, 44);
        right.add(emailField);
        cy += 60;

        // PASSWORD
        right.add(fieldLabel("PASSWORD", lx, cy));
        cy += 18;
        JPasswordField passField = (JPasswordField) glassField(true);
        passField.setBounds(lx, cy, fw, 44);
        right.add(passField);
        cy += 58;

        // Gradient divider line
        JPanel divider = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                GradientPaint gp = new GradientPaint(
                    0, 0, new Color(37,37,37,0),
                    getWidth()/2f, 0, DIVIDER,
                    false
                );
                // symmetric: dark → light → dark
                g2.setColor(DIVIDER);
                for (int i = 0; i <= getWidth(); i++) {
                    double t = (double)i/getWidth();
                    double v = t < 0.5 ? t * 2 : (1 - t) * 2;
                    int alpha = (int)(180 * v);
                    g2.setColor(new Color(55,55,55, Math.max(0,Math.min(255,alpha))));
                    g2.drawLine(i, 0, i, getHeight());
                }
                g2.dispose();
            }
        };
        divider.setOpaque(false);
        divider.setBounds(lx, cy, fw, 1);
        right.add(divider);
        cy += 18;

        // ACCESS ACCOUNT button
        JButton loginBtn = new JButton("ACCESS ACCOUNT") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                FontMetrics fm = g.getFontMetrics(getFont());
                g.setColor(getForeground());
                g.setFont(getFont());
                int tx = (getWidth() - fm.stringWidth(getText())) / 2;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g.drawString(getText(), tx, ty);
            }
        };
        loginBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        loginBtn.setBackground(new Color(235, 235, 235));
        loginBtn.setForeground(new Color(10, 10, 10));
        loginBtn.setOpaque(false);
        loginBtn.setContentAreaFilled(false);
        loginBtn.setBorderPainted(false);
        loginBtn.setFocusPainted(false);
        loginBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginBtn.setBounds(lx, cy, fw, 46);
        right.add(loginBtn);

        loginBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { loginBtn.setBackground(CRIMSON); loginBtn.setForeground(Color.WHITE); loginBtn.repaint(); }
            public void mouseExited(MouseEvent e)  { loginBtn.setBackground(new Color(235,235,235)); loginBtn.setForeground(new Color(10,10,10)); loginBtn.repaint(); }
        });

        loginBtn.addActionListener(e -> {
            String email    = emailField.getText().trim();
            String password = new String(passField.getPassword()).trim();
            String role     = investorActive ? "INVESTOR" : "ADMIN";
            if (email.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(right, "Please enter both email and password.", "Login Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                AuthClass user = authService.login(email, password);
                if (!user.getUserType().equalsIgnoreCase(role)) {
                    JOptionPane.showMessageDialog(right, "Selected role does not match this account.", "Login Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                // Store session so all UIs have access to the logged-in user
                edu.iiitd.dbms.auth.LoginManager.login(user);
                JOptionPane.showMessageDialog(right, "Welcome, " + user.getName() + "!", "Success", JOptionPane.INFORMATION_MESSAGE);
                dispose();
                if (user.getUserType().equalsIgnoreCase("ADMIN")) new AdminDashUI().setVisible(true);
                else new InvestorDashUI().setVisible(true);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(right, ex.getMessage(), "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        passField.addActionListener(e -> loginBtn.doClick());
        emailField.addActionListener(e -> passField.requestFocus());

        // Footer
        JLabel footer = new JLabel("Fractional  ·  IIIT Delhi  ·  v1.0");
        footer.setFont(new Font("SansSerif", Font.PLAIN, 10));
        footer.setForeground(TEXT_DIM);
        footer.setHorizontalAlignment(SwingConstants.CENTER);
        footer.setBounds(0, 510, 522, 14);
        right.add(footer);
    }

    // ── HELPERS ──────────────────────────────────────────────────────────────

    private JPanel buildPill(String text) {
        JPanel p = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean on = Boolean.TRUE.equals(getClientProperty("on"));
                if (on) {
                    g2.setColor(PILL_ACT);
                    g2.fillRoundRect(0,0,getWidth(),getHeight(),12,12);
                } else {
                    g2.setColor(PILL_IDLE);
                    g2.fillRoundRect(0,0,getWidth(),getHeight(),12,12);
                    g2.setColor(new Color(255,255,255,22));
                    g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,12,12);
                }
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setCursor(new Cursor(Cursor.HAND_CURSOR));
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setName("L");
        p.add(l, BorderLayout.CENTER);
        return p;
    }

    private void setPill(JPanel pill, boolean on) {
        pill.putClientProperty("on", on);
        for (Component c : pill.getComponents())
            if (c instanceof JLabel && "L".equals(c.getName())) {
                JLabel l = (JLabel)c;
                l.setFont(new Font("SansSerif", on ? Font.BOLD : Font.PLAIN, 13));
                l.setForeground(on ? new Color(12,12,12) : new Color(90,90,90));
            }
        pill.repaint();
    }

    private JLabel fieldLabel(String text, int x, int y) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 9));
        l.setForeground(new Color(80,80,80));
        l.setBounds(x, y, 300, 14);
        return l;
    }

    private JTextField glassField(boolean isPassword) {
        JTextField f = isPassword ? new JPasswordField() : new JTextField();
        f.setUI(new javax.swing.plaf.basic.BasicTextFieldUI() {
            @Override protected void paintBackground(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean focused = f.isFocusOwner();
                g2.setColor(focused ? new Color(255,255,255,20) : FLD_BG);
                g2.fillRoundRect(0,0,f.getWidth(),f.getHeight(),10,10);
                g2.setColor(focused ? FLD_BD_HI : FLD_BD);
                g2.drawRoundRect(0,0,f.getWidth()-1,f.getHeight()-1,10,10);
                g2.dispose();
            }
        });
        f.setOpaque(false);
        f.setForeground(TEXT_HI);
        f.setCaretColor(TEXT_HI);
        f.setFont(new Font("SansSerif", Font.PLAIN, 13));
        f.setBorder(new EmptyBorder(8, 14, 8, 14));
        f.setSelectionColor(new Color(255,255,255,55));
        f.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { f.repaint(); }
            public void focusLost(FocusEvent e)   { f.repaint(); }
        });
        return f;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new logintesterUI().setVisible(true));
    }
}