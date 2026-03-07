package edu.iiitd.dbms.ui.components;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.RingPlot;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class CompactChartPanel extends JPanel {

    // Grayscale UI Colors
    private final Color cardBg = new Color(26, 26, 26); // Deep Gray
    private final Color textPrimary = new Color(250, 250, 250); // White
    
    // Pastel Chart Colors
    private final Color pastelBlue = new Color(174, 198, 207);
    private final Color pastelGreen = new Color(119, 221, 119);
    private final Color pastelLilac = new Color(200, 162, 200);
    private final Color pastelPeach = new Color(255, 179, 71);

    private ChartPanel chartPanel;
    private JFreeChart viewOne;
    private JFreeChart viewTwo;

    public CompactChartPanel(int chartMode, String titleText) {
        setLayout(new BorderLayout());
        setOpaque(false);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 10, 0));

        JLabel title = new JLabel(titleText);
        title.setForeground(textPrimary);
        title.setFont(new Font("SansSerif", Font.BOLD, 12));

        JComboBox<String> viewSelector = new JComboBox<>();
        viewSelector.setBackground(new Color(40, 40, 40));
        viewSelector.setForeground(textPrimary);
        viewSelector.setFocusable(false);

        if (chartMode == 1) {
            // Mode 1: Now includes LINE chart as default
            viewOne = createLineChart();
            viewTwo = createAreaChart();
            viewSelector.addItem("Line View");
            viewSelector.addItem("Area View");
        } else {
            // Mode 2: Asset Holdings
            viewOne = createRingChart();
            viewTwo = createPieChart();
            viewSelector.addItem("Doughnut View");
            viewSelector.addItem("Pie View");
        }

        chartPanel = new ChartPanel(viewOne);
        chartPanel.setOpaque(false);
        chartPanel.setBackground(cardBg);
        
        viewSelector.addActionListener(e -> {
            if (viewSelector.getSelectedIndex() == 0) chartPanel.setChart(viewOne);
            else chartPanel.setChart(viewTwo);
        });

        header.add(title, BorderLayout.WEST);
        header.add(viewSelector, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);
        add(chartPanel, BorderLayout.CENTER);
    }

    // --- Chart Generators ---

    private JFreeChart createLineChart() {
        DefaultCategoryDataset data = new DefaultCategoryDataset();
        data.addValue(80, "Value", "Jan"); data.addValue(85, "Value", "Feb"); 
        data.addValue(82, "Value", "Mar"); data.addValue(90, "Value", "Apr");
        JFreeChart chart = ChartFactory.createLineChart(null, null, null, data, PlotOrientation.VERTICAL, false, true, false);
        styleCategoryChart(chart, pastelBlue, true);
        return chart;
    }

    private JFreeChart createAreaChart() {
        DefaultCategoryDataset data = new DefaultCategoryDataset();
        data.addValue(80, "Value", "Jan"); data.addValue(85, "Value", "Feb"); 
        data.addValue(82, "Value", "Mar"); data.addValue(90, "Value", "Apr");
        JFreeChart chart = ChartFactory.createAreaChart(null, null, null, data, PlotOrientation.VERTICAL, false, true, false);
        styleCategoryChart(chart, new Color(174, 198, 207, 120), false); // Semi-transparent pastel blue
        return chart;
    }

    private JFreeChart createRingChart() {
        DefaultPieDataset data = new DefaultPieDataset();
        data.setValue("Watches", 45); data.setValue("Sneakers", 35); data.setValue("Art", 20);
        JFreeChart chart = ChartFactory.createRingChart(null, data, false, true, false);
        stylePieChart(chart, true);
        return chart;
    }

    private JFreeChart createPieChart() {
        DefaultPieDataset data = new DefaultPieDataset();
        data.setValue("Watches", 45); data.setValue("Sneakers", 35); data.setValue("Art", 20);
        JFreeChart chart = ChartFactory.createPieChart(null, data, false, true, false);
        stylePieChart(chart, false);
        return chart;
    }

    // --- Styling Helpers ---
    private void styleCategoryChart(JFreeChart chart, Color seriesColor, boolean isLine) {
        chart.setBackgroundPaint(cardBg);
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(cardBg);
        plot.setOutlineVisible(false);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(false);
        plot.getDomainAxis().setTickLabelPaint(new Color(150, 150, 150));
        plot.getRangeAxis().setVisible(false);
        plot.getRenderer().setSeriesPaint(0, seriesColor);
        
        if (isLine) {
            plot.getRenderer().setSeriesStroke(0, new BasicStroke(3.0f)); // Thicker line
        }
    }

    private void stylePieChart(JFreeChart chart, boolean isRing) {
        chart.setBackgroundPaint(cardBg);
        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setBackgroundPaint(cardBg);
        plot.setOutlineVisible(false);
        plot.setLabelGenerator(null);
        
        // Applying Pastels
        plot.setSectionPaint("Watches", pastelLilac);
        plot.setSectionPaint("Sneakers", pastelGreen);
        plot.setSectionPaint("Art", pastelPeach);
        
        if (isRing) {
            ((RingPlot) plot).setSectionDepth(0.25);
            ((RingPlot) plot).setShadowPaint(null);
        } else {
            plot.setShadowPaint(null);
        }
    }
}