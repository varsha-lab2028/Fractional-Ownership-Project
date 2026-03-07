package edu.iiitd.dbms.ui.components;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import java.awt.*;

public class DynamicChartPanel extends JPanel {

    private final Color bgBlack = new Color(0, 0, 0); // Absolute Pure Black
    private final Color credNeon = new Color(0, 255, 163); // CRED's signature neon mint

    public DynamicChartPanel() {
        setLayout(new BorderLayout());
        setBackground(bgBlack);

        JFreeChart lineChart = createCredChart();
        ChartPanel chartPanel = new ChartPanel(lineChart);
        chartPanel.setPreferredSize(new Dimension(800, 250));
        chartPanel.setBackground(bgBlack);
        chartPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        add(chartPanel, BorderLayout.CENTER);
    }

    private JFreeChart createCredChart() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(78000, "V", "1");
        dataset.addValue(79500, "V", "2");
        dataset.addValue(78200, "V", "3");
        dataset.addValue(81000, "V", "4");
        dataset.addValue(84250, "V", "5");

        JFreeChart chart = ChartFactory.createLineChart(
                null, null, null, dataset, PlotOrientation.VERTICAL, false, true, false);

        chart.setBackgroundPaint(bgBlack);
        chart.setBorderVisible(false);

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(bgBlack);
        plot.setOutlineVisible(false);
        
        // Strip absolutely everything
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(false);
        plot.getDomainAxis().setVisible(false);
        plot.getRangeAxis().setVisible(false); 

        // The glowing neon line
        plot.getRenderer().setSeriesPaint(0, credNeon);
        plot.getRenderer().setSeriesStroke(0, new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        return chart;
    }
}