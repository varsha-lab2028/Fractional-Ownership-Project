package edu.iiitd.dbms.ui.components;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.DefaultHighLowDataset;
import org.jfree.data.xy.XYDataset;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MarketChartPanel extends JPanel {

    private final Color cardBg = new Color(26, 26, 26);
    private final Color textPrimary = new Color(250, 250, 250);
    private final Color gridColor = new Color(45, 45, 45);
    private final Color pastelGreen = new Color(119, 221, 119);
    private final Color pastelRed = new Color(255, 105, 97);
    private final Color pastelBlue = new Color(174, 198, 207);

    private ChartPanel chartPanel;
    private JFreeChart lineChart;
    private JFreeChart candleChart;

    public MarketChartPanel(String titleText) {
        setLayout(new BorderLayout());
        setOpaque(false);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 15, 0));

        JLabel title = new JLabel(titleText);
        title.setForeground(textPrimary);
        title.setFont(new Font("SansSerif", Font.BOLD, 14));


        JPanel togglePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        togglePanel.setOpaque(false);
        
        CustomToggle btnLine = new CustomToggle("Line View", true);
        CustomToggle btnCandle = new CustomToggle("Candlestick", false);


        XYDataset dataset = createMockData(60);

        lineChart = createLineChart(dataset);
        candleChart = createCandleChart(dataset);

        chartPanel = new ChartPanel(lineChart);
        chartPanel.setOpaque(true);
        chartPanel.setBackground(cardBg);
        chartPanel.setPreferredSize(new Dimension(800, 320));
        chartPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0)); 

        // Toggle Actions
        btnLine.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                btnLine.setActive(true);
                btnCandle.setActive(false);
                chartPanel.setChart(lineChart);
            }
        });
        
        btnCandle.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                btnLine.setActive(false);
                btnCandle.setActive(true);
                chartPanel.setChart(candleChart);
            }
        });

        togglePanel.add(btnLine);
        togglePanel.add(btnCandle);

        header.add(title, BorderLayout.WEST);
        header.add(togglePanel, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);
        add(chartPanel, BorderLayout.CENTER);
    }

    class CustomToggle extends JLabel {
        private boolean isActive;
        public CustomToggle(String text, boolean isActive) {
            super(text, SwingConstants.CENTER);
            this.isActive = isActive;
            setFont(new Font("SansSerif", Font.BOLD, 11));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(90, 30));
            updateStyle();
        }
        public void setActive(boolean active) {
            this.isActive = active;
            updateStyle();
        }
        private void updateStyle() {
            setForeground(isActive ? new Color(14, 14, 14) : textPrimary);
            repaint();
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(isActive ? pastelBlue : gridColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            super.paintComponent(g2);
            g2.dispose();
        }
    }

    private XYDataset createMockData(int days) {
        long day = 24 * 60 * 60 * 1000L;
        long now = System.currentTimeMillis();
        
        Date[] date = new Date[days];
        double[] high = new double[days];
        double[] low = new double[days];
        double[] open = new double[days];
        double[] close = new double[days];
        double[] volume = new double[days];

        double currentPrice = 24.50; 

        for (int i = 0; i < days; i++) {
            date[i] = new Date(now - (days - 1 - i) * day);
            open[i] = currentPrice;
            close[i] = open[i] + (Math.random() * 1.5 - 0.75); 
            high[i] = Math.max(open[i], close[i]) + (Math.random() * 0.5);
            low[i] = Math.min(open[i], close[i]) - (Math.random() * 0.5);
            volume[i] = 1000 + Math.random() * 500;
            currentPrice = close[i]; 
        }
        return new DefaultHighLowDataset("JDN1", date, high, low, open, close, volume);
    }

    private JFreeChart createLineChart(XYDataset dataset) {
        JFreeChart chart = ChartFactory.createTimeSeriesChart(null, null, null, dataset, false, false, false);
        stylePlot((XYPlot) chart.getPlot(), true);
        chart.setBackgroundPaint(cardBg); // FIX: explicitly removes white bleed
        return chart;
    }

    private JFreeChart createCandleChart(XYDataset dataset) {
        JFreeChart chart = ChartFactory.createCandlestickChart(null, null, null, (DefaultHighLowDataset) dataset, false);
        stylePlot((XYPlot) chart.getPlot(), false);
        chart.setBackgroundPaint(cardBg); // FIX: explicitly removes white bleed
        return chart;
    }

    private void stylePlot(XYPlot plot, boolean isLine) {
        plot.setBackgroundPaint(cardBg);
        plot.setDomainGridlinePaint(gridColor);
        plot.setRangeGridlinePaint(gridColor);
        plot.setOutlineVisible(false);

        DateAxis domainAxis = (DateAxis) plot.getDomainAxis();
        domainAxis.setTickLabelPaint(new Color(150, 150, 150));
        domainAxis.setDateFormatOverride(new SimpleDateFormat("MMM dd"));
        domainAxis.setAxisLinePaint(gridColor);

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setTickLabelPaint(new Color(150, 150, 150));
        rangeAxis.setAutoRangeIncludesZero(false);
        rangeAxis.setAxisLinePaint(gridColor);

        if (isLine) {
            XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
            renderer.setSeriesPaint(0, pastelBlue);
            renderer.setSeriesStroke(0, new BasicStroke(2.0f));
            plot.setRenderer(renderer);
        } else {
            CandlestickRenderer renderer = new CandlestickRenderer();
            renderer.setUpPaint(pastelGreen);
            renderer.setDownPaint(pastelRed);
            renderer.setUseOutlinePaint(false);
            renderer.setAutoWidthMethod(CandlestickRenderer.WIDTHMETHOD_AVERAGE); 
            plot.setRenderer(renderer);
        }
    }
}