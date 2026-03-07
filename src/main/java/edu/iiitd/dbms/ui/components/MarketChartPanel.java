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

        JComboBox<String> viewSelector = new JComboBox<>(new String[]{"Line Chart", "Candlestick"});
        viewSelector.setBackground(new Color(40, 40, 40));
        viewSelector.setForeground(textPrimary);
        viewSelector.setFocusable(false);

        // Generate Mock Data
        XYDataset dataset = createMockData();

        lineChart = createLineChart(dataset);
        candleChart = createCandleChart(dataset);

        chartPanel = new ChartPanel(lineChart);
        chartPanel.setOpaque(false);
        chartPanel.setBackground(cardBg);
        chartPanel.setPreferredSize(new Dimension(800, 350));

        viewSelector.addActionListener(e -> {
            if (viewSelector.getSelectedIndex() == 0) chartPanel.setChart(lineChart);
            else chartPanel.setChart(candleChart);
        });

        header.add(title, BorderLayout.WEST);
        header.add(viewSelector, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);
        add(chartPanel, BorderLayout.CENTER);
    }

    private XYDataset createMockData() {
        // Generating 5 days of OHLC (Open, High, Low, Close) trading data
        long day = 24 * 60 * 60 * 1000L;
        long now = System.currentTimeMillis();
        
        Date[] date = new Date[5];
        double[] high = new double[5];
        double[] low = new double[5];
        double[] open = new double[5];
        double[] close = new double[5];
        double[] volume = new double[5];

        for (int i = 0; i < 5; i++) {
            date[i] = new Date(now - (4 - i) * day);
            open[i] = 24.0 + (Math.random() * 2);
            close[i] = open[i] + (Math.random() * 2 - 1);
            high[i] = Math.max(open[i], close[i]) + Math.random();
            low[i] = Math.min(open[i], close[i]) - Math.random();
            volume[i] = 1000 + Math.random() * 500;
        }

        return new DefaultHighLowDataset("JDN1", date, high, low, open, close, volume);
    }

    private JFreeChart createLineChart(XYDataset dataset) {
        JFreeChart chart = ChartFactory.createTimeSeriesChart(null, null, null, dataset, false, true, false);
        stylePlot((XYPlot) chart.getPlot(), true);
        return chart;
    }

    private JFreeChart createCandleChart(XYDataset dataset) {
        JFreeChart chart = ChartFactory.createCandlestickChart(null, null, null, (DefaultHighLowDataset) dataset, false);
        stylePlot((XYPlot) chart.getPlot(), false);
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

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setTickLabelPaint(new Color(150, 150, 150));
        rangeAxis.setAutoRangeIncludesZero(false);

        if (isLine) {
            XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
            renderer.setSeriesPaint(0, pastelBlue);
            renderer.setSeriesStroke(0, new BasicStroke(2.5f));
            plot.setRenderer(renderer);
        } else {
            CandlestickRenderer renderer = new CandlestickRenderer();
            renderer.setUpPaint(pastelGreen);
            renderer.setDownPaint(pastelRed);
            renderer.setUseOutlinePaint(false);
            plot.setRenderer(renderer);
        }
    }
}