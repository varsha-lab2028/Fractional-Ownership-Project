package edu.iiitd.dbms.ui.components;

import edu.iiitd.dbms.service.LiveMarketDataManager;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.DefaultOHLCDataset;
import org.jfree.data.xy.OHLCDataItem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * Live market chart panel driven by {@link LiveMarketDataManager}.
 *
 * Features
 * ─────────
 *  • IPO dropdown — select any tracked asset; chart updates immediately
 *  • Line / Candlestick toggle
 *  • Live price + % change label that updates every tick
 *  • Market OPEN / CLOSED badge
 *  • Subscribes to LiveMarketDataManager's "priceUpdate" and "marketState" events
 *    and unsubscribes when the panel is removed from its parent window.
 */
public class LiveMarketChartPanel extends JPanel {

    // ── Palette (matches app-wide dark theme) ────────────────────────────────
    private final Color cardBg      = new Color(26, 26, 26);
    private final Color bgDark      = new Color(14, 14, 14);
    private final Color textPrimary = new Color(250, 250, 250);
    private final Color textMuted   = new Color(150, 150, 150);
    private final Color gridColor   = new Color(45, 45, 45);
    private final Color pastelGreen = new Color(119, 221, 119);
    private final Color pastelRed   = new Color(255, 105, 97);
    private final Color pastelBlue  = new Color(174, 198, 207);
    private final Color accentAmber = new Color(255, 179, 71);

    // ── Chart state ──────────────────────────────────────────────────────────
    private ChartPanel chartPanel;
    private TimeSeries  lineSeries;
    private JFreeChart  lineChart;
    private JFreeChart  candleChart;
    private DefaultOHLCDataset ohlcDataset;
    private boolean isLineMode = true;

    // ── UI references ────────────────────────────────────────────────────────
    private JComboBox<String> ipoDropdown;
    private JLabel priceLabel;
    private JLabel changePctLabel;
    private JLabel marketStatusBadge;
    private JLabel selectedAssetLabel;

    // ── Live data state ──────────────────────────────────────────────────────
    private int    selectedAssetId   = -1;
    private double prevTickPrice     = 0;
    private final Map<String, Integer> comboToAssetId = new LinkedHashMap<>();

    private final LiveMarketDataManager lmd = LiveMarketDataManager.getInstance();
    private final PropertyChangeListener marketListener;

    // ────────────────────────────────────────────────────────────────────────
    public LiveMarketChartPanel() {
        setLayout(new BorderLayout(0, 10));
        setOpaque(false);

        // ── Top strip ──────────────────────────────────────────────────────
        JPanel topStrip = new JPanel(new BorderLayout(15, 0));
        topStrip.setOpaque(false);

        // Left: dropdown + asset title
        JPanel leftTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftTop.setOpaque(false);

        ipoDropdown = buildStyledCombo();
        ipoDropdown.setPreferredSize(new Dimension(260, 36));

        selectedAssetLabel = new JLabel("—");
        selectedAssetLabel.setForeground(textMuted);
        selectedAssetLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));

        leftTop.add(ipoDropdown);
        leftTop.add(selectedAssetLabel);

        // Centre: live price display
        JPanel pricePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        pricePanel.setOpaque(false);

        priceLabel = new JLabel("$—");
        priceLabel.setForeground(textPrimary);
        priceLabel.setFont(new Font("SansSerif", Font.BOLD, 22));

        changePctLabel = new JLabel("—");
        changePctLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        changePctLabel.setForeground(textMuted);

        marketStatusBadge = new JLabel("● MARKET CLOSED");
        marketStatusBadge.setFont(new Font("SansSerif", Font.BOLD, 11));
        marketStatusBadge.setForeground(pastelRed);

        pricePanel.add(priceLabel);
        pricePanel.add(changePctLabel);
        pricePanel.add(Box.createHorizontalStrut(12));
        pricePanel.add(marketStatusBadge);

        // Right: Line / Candle toggle
        JPanel togglePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 3));
        togglePanel.setOpaque(false);
        CustomToggle btnLine   = new CustomToggle("Line",   true);
        CustomToggle btnCandle = new CustomToggle("Candle", false);
        btnLine.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                btnLine.setActive(true);  btnCandle.setActive(false);
                isLineMode = true;
                if (chartPanel != null) chartPanel.setChart(lineChart);
            }
        });
        btnCandle.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                btnLine.setActive(false); btnCandle.setActive(true);
                isLineMode = false;
                if (chartPanel != null) chartPanel.setChart(candleChart);
            }
        });
        togglePanel.add(btnLine);
        togglePanel.add(btnCandle);

        topStrip.add(leftTop,    BorderLayout.WEST);
        topStrip.add(pricePanel, BorderLayout.CENTER);
        topStrip.add(togglePanel,BorderLayout.EAST);

        // ── Chart area ─────────────────────────────────────────────────────
        lineSeries = new TimeSeries("Price");
        TimeSeriesCollection tsc = new TimeSeriesCollection(lineSeries);
        lineChart  = buildLineChart(tsc);
        ohlcDataset = buildEmptyOHLC("—");
        candleChart = buildCandleChart(ohlcDataset);

        chartPanel = new ChartPanel(lineChart);
        chartPanel.setOpaque(true);
        chartPanel.setBackground(cardBg);
        chartPanel.setPreferredSize(new Dimension(800, 310));
        chartPanel.setBorder(null);
        chartPanel.setPopupMenu(null); // remove right-click context menu

        add(topStrip,   BorderLayout.NORTH);
        add(chartPanel, BorderLayout.CENTER);

        // ── Populate dropdown ───────────────────────────────────────────────
        populateDropdown();
        ipoDropdown.addItemListener(this::onAssetSelected);

        // ── Live listener ───────────────────────────────────────────────────
        marketListener = (PropertyChangeEvent evt) -> {
            if ("priceUpdate".equals(evt.getPropertyName())) {
                @SuppressWarnings("unchecked")
                Map<Integer, Double> prices = (Map<Integer, Double>) evt.getNewValue();
                SwingUtilities.invokeLater(() -> onPriceUpdate(prices));
            } else if ("marketState".equals(evt.getPropertyName())) {
                boolean open = (Boolean) evt.getNewValue();
                SwingUtilities.invokeLater(() -> updateMarketBadge(open));
            }
        };
        lmd.addListener(marketListener);
        updateMarketBadge(lmd.isMarketOpen());

        // Clean up listener when panel window is disposed
        addAncestorListener(new javax.swing.event.AncestorListener() {
            public void ancestorAdded(javax.swing.event.AncestorEvent e) {}
            public void ancestorMoved(javax.swing.event.AncestorEvent e) {}
            public void ancestorRemoved(javax.swing.event.AncestorEvent e) {
                lmd.removeListener(marketListener);
            }
        });
    }

    // ── Dropdown population ──────────────────────────────────────────────────
    private void populateDropdown() {
        ipoDropdown.removeAllItems();
        comboToAssetId.clear();
        Map<Integer, String> names = lmd.getAssetNames();
        Map<Integer, Double> prices = lmd.getAllCurrentPrices();

        if (names.isEmpty()) {
            ipoDropdown.addItem("No IPO data (DB offline)");
            return;
        }
        // Sort by assetId for stable ordering
        List<Integer> ids = new ArrayList<>(names.keySet());
        Collections.sort(ids);
        for (int id : ids) {
            String label = "[" + id + "] " + names.get(id);
            comboToAssetId.put(label, id);
            ipoDropdown.addItem(label);
        }
        if (!ids.isEmpty()) selectAsset(ids.get(0));
    }

    private void onAssetSelected(ItemEvent e) {
        if (e.getStateChange() != ItemEvent.SELECTED) return;
        String sel = (String) e.getItem();
        Integer id = comboToAssetId.get(sel);
        if (id != null) selectAsset(id);
    }

    private void selectAsset(int assetId) {
        selectedAssetId = assetId;
        String name = lmd.getAssetNames().getOrDefault(assetId, "Asset " + assetId);
        selectedAssetLabel.setText(name + "  ·  Asset ID: " + assetId);

        prevTickPrice = lmd.getCurrentPrice(assetId);

        // Rebuild line series from candle history
        lineSeries.clear();
        List<double[]> candles = lmd.getCandleSnapshot(assetId);
        for (double[] c : candles) {
            lineSeries.addOrUpdate(new Millisecond(new java.util.Date((long) c[0])), c[4]);
        }

        // Rebuild OHLC dataset
        rebuildOHLC(assetId, name);

        // Update price display
        priceLabel.setText(String.format("$%.2f", prevTickPrice));
        changePctLabel.setText("—");
        changePctLabel.setForeground(textMuted);
    }

    // ── Price update handler (called on EDT every tick) ──────────────────────
    private void onPriceUpdate(Map<Integer, Double> prices) {
        // Update all rows if needed (screener panel listens separately)
        if (selectedAssetId < 0) return;

        Double newPrice = prices.get(selectedAssetId);
        if (newPrice == null) return;

        // Update line chart
        lineSeries.addOrUpdate(new Millisecond(new java.util.Date()), newPrice);
        if (lineSeries.getItemCount() > LiveMarketDataManager.MAX_CANDLES)
            lineSeries.delete(0, 0);

        // Refresh candle chart
        rebuildOHLC(selectedAssetId, lmd.getAssetNames().getOrDefault(selectedAssetId, ""));

        // Price label + % change
        priceLabel.setText(String.format("$%.4f", newPrice));
        if (prevTickPrice > 0) {
            double pct = (newPrice - prevTickPrice) / prevTickPrice * 100.0;
            String sign = pct >= 0 ? "+" : "";
            changePctLabel.setText(String.format("%s%.2f%%", sign, pct));
            changePctLabel.setForeground(pct >= 0 ? pastelGreen : pastelRed);
            priceLabel.setForeground(pct >= 0 ? pastelGreen : pastelRed);
        }
        prevTickPrice = newPrice;
    }

    private void rebuildOHLC(int assetId, String name) {
        List<double[]> candles = lmd.getCandleSnapshot(assetId);
        OHLCDataItem[] items = new OHLCDataItem[candles.size()];
        for (int i = 0; i < candles.size(); i++) {
            double[] c = candles.get(i);
            items[i] = new OHLCDataItem(new java.util.Date((long) c[0]),
                c[1], c[2], c[3], c[4], c[5]);
        }
        DefaultOHLCDataset newDs = new DefaultOHLCDataset(name, items);
        // Replace dataset in candle chart
        XYPlot plot = (XYPlot) candleChart.getPlot();
        plot.setDataset(newDs);
    }

    private void updateMarketBadge(boolean open) {
        if (open) {
            marketStatusBadge.setText("● MARKET OPEN");
            marketStatusBadge.setForeground(pastelGreen);
        } else {
            marketStatusBadge.setText("● MARKET CLOSED");
            marketStatusBadge.setForeground(pastelRed);
        }
    }

    // ── Chart factory helpers ────────────────────────────────────────────────
    private JFreeChart buildLineChart(TimeSeriesCollection tsc) {
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
            null, null, null, tsc, false, false, false);
        styleXYPlot((XYPlot) chart.getPlot(), true);
        chart.setBackgroundPaint(cardBg);
        return chart;
    }

    private DefaultOHLCDataset buildEmptyOHLC(String key) {
        return new DefaultOHLCDataset(key, new OHLCDataItem[0]);
    }

    private JFreeChart buildCandleChart(DefaultOHLCDataset ds) {
        JFreeChart chart = ChartFactory.createCandlestickChart(
            null, null, null, ds, false);
        styleXYPlot((XYPlot) chart.getPlot(), false);
        chart.setBackgroundPaint(cardBg);
        return chart;
    }

    private void styleXYPlot(XYPlot plot, boolean isLine) {
        plot.setBackgroundPaint(cardBg);
        plot.setDomainGridlinePaint(gridColor);
        plot.setRangeGridlinePaint(gridColor);
        plot.setOutlineVisible(false);

        DateAxis domain = (DateAxis) plot.getDomainAxis();
        domain.setTickLabelPaint(textMuted);
        domain.setDateFormatOverride(new SimpleDateFormat("HH:mm:ss"));
        domain.setAxisLinePaint(gridColor);
        domain.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));

        NumberAxis range = (NumberAxis) plot.getRangeAxis();
        range.setTickLabelPaint(textMuted);
        range.setAutoRangeIncludesZero(false);
        range.setAxisLinePaint(gridColor);
        range.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));

        if (isLine) {
            XYLineAndShapeRenderer r = new XYLineAndShapeRenderer(true, false);
            r.setSeriesPaint(0, pastelBlue);
            r.setSeriesStroke(0, new BasicStroke(2.0f));
            plot.setRenderer(r);
        } else {
            CandlestickRenderer r = new CandlestickRenderer();
            r.setUpPaint(pastelGreen);
            r.setDownPaint(pastelRed);
            r.setUseOutlinePaint(false);
            r.setAutoWidthMethod(CandlestickRenderer.WIDTHMETHOD_AVERAGE);
            plot.setRenderer(r);
        }
    }

    // ── Helper: styled combo box ─────────────────────────────────────────────
    private JComboBox<String> buildStyledCombo() {
        JComboBox<String> combo = new JComboBox<>();
        combo.setBackground(new Color(36, 36, 36));
        combo.setForeground(textPrimary);
        combo.setFont(new Font("SansSerif", Font.BOLD, 13));
        combo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(55, 55, 55)),
            new EmptyBorder(4, 10, 4, 10)));
        return combo;
    }

    /** Call this after new IPOs are verified to refresh the dropdown. */
    public void refreshDropdown() {
        String prev = (String) ipoDropdown.getSelectedItem();
        populateDropdown();
        if (prev != null) ipoDropdown.setSelectedItem(prev);
    }

    // ── Inner: pill toggle ───────────────────────────────────────────────────
    class CustomToggle extends JLabel {
        private boolean active;
        CustomToggle(String text, boolean active) {
            super(text, CENTER);
            this.active = active;
            setFont(new Font("SansSerif", Font.BOLD, 11));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(75, 28));
            repaint();
        }
        void setActive(boolean a) { active = a; repaint(); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(active ? pastelBlue : gridColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            setForeground(active ? bgDark : textPrimary);
            super.paintComponent(g2);
            g2.dispose();
        }
    }
}
