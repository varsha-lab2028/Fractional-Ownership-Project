package edu.iiitd.dbms.ui.investor;

/**
 * MarketViewUI_updated — thin alias for MarketViewUI.
 *
 * Several other UI classes (SellOrderUI, HoldingsViewUI_updated, etc.) navigate
 * to "new MarketViewUI_updated()" from their sidebar.  This class simply
 * delegates to the real MarketViewUI so both entry points show the same
 * live-market view.
 *
 * No logic lives here — maintain everything in MarketViewUI.java.
 */
public class MarketViewUI_updated extends MarketViewUI {
    public MarketViewUI_updated() {
        super();
        setTitle("Fractional. - Secondary Market");
    }

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> new MarketViewUI_updated().setVisible(true));
    }
}
