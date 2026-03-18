package edu.iiitd.dbms.ui.components;

import edu.iiitd.dbms.auth.LoginManager;
import edu.iiitd.dbms.data_access.PortfolioDAO;
import edu.iiitd.dbms.data_access.ValuationDAO;
import edu.iiitd.dbms.dto.AssetHoldingDTO;
import edu.iiitd.dbms.domain.Valuation;

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
import java.util.*;
import java.util.List;

public class CompactChartPanel extends JPanel {

    private final Color cardBg      = new Color(26, 26, 26);
    private final Color textPrimary = new Color(250, 250, 250);
    private final Color textMuted   = new Color(150, 150, 150);
    private final Color borderColor = new Color(45, 45, 45);
    private final Color pastelBlue  = new Color(174, 198, 207);
    private final Color pastelGreen = new Color(119, 221, 119);
    private final Color pastelLilac = new Color(200, 162, 200);
    private final Color pastelPeach = new Color(255, 179, 71);
    private final Color pastelRed   = new Color(255, 105, 97);

    private ChartPanel chartPanel;
    private JFreeChart viewOne;
    private JFreeChart viewTwo;
    private final JComboBox<String> viewSelector;

    public CompactChartPanel(int chartMode, String titleText) {
        setLayout(new BorderLayout());
        setOpaque(false);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 8, 0));

        JLabel title = new JLabel(titleText);
        title.setForeground(textPrimary);
        title.setFont(new Font("SansSerif", Font.BOLD, 12));

        viewSelector = new JComboBox<>();
        viewSelector.setBackground(new Color(40, 40, 40));
        viewSelector.setForeground(textPrimary);
        viewSelector.setFocusable(false);
        viewSelector.setFont(new Font("SansSerif", Font.PLAIN, 11));

        if (chartMode == 1) {
            viewSelector.addItem("Line View");
            viewSelector.addItem("Bar View");
        } else {
            viewSelector.addItem("Doughnut View");
            viewSelector.addItem("Pie View");
        }

        // Start with a simple placeholder
        chartPanel = new ChartPanel(makePlaceholder(chartMode));
        chartPanel.setOpaque(false);
        chartPanel.setBackground(cardBg);

        viewSelector.addActionListener(e -> {
            if (viewOne != null && viewTwo != null) {
                chartPanel.setChart(viewSelector.getSelectedIndex() == 0 ? viewOne : viewTwo);
            }
        });

        header.add(title, BorderLayout.WEST);
        header.add(viewSelector, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
        add(chartPanel, BorderLayout.CENTER);

        // Load real data asynchronously
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    int investorId = LoginManager.getCurrentUser() != null
                            ? LoginManager.getCurrentUser().getLinkedId() : 1;

                    if (chartMode == 1) {
                        buildGrowthCharts(investorId);
                    } else {
                        buildAllocationCharts(investorId);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    viewOne = makePlaceholder(chartMode);
                    viewTwo = makePlaceholder(chartMode);
                }
                return null;
            }

            @Override
            protected void done() {
                if (viewOne != null) chartPanel.setChart(viewOne);
                chartPanel.revalidate();
                chartPanel.repaint();
            }
        };
        worker.execute();
    }

    // ── Chart builders ────────────────────────────────────────────────────────

    private void buildGrowthCharts(int investorId) throws Exception {
        PortfolioDAO portfolioDAO = new PortfolioDAO();
        ValuationDAO valuationDAO = new ValuationDAO();
        List<AssetHoldingDTO> holdings = portfolioDAO.getInvestorHoldings(investorId);

        // Build month → portfolio value map using valuation history
        // For each holding: investor's value = (unitsHeld / totalIPOUnits) * totalAssetValuation
        TreeMap<String, Double> monthlyTotal = new TreeMap<>();

        if (!holdings.isEmpty()) {
            for (AssetHoldingDTO h : holdings) {
                List<Valuation> history = valuationDAO.getHistory(h.getAssetId());
                // totalIPOUnits is NOT in AssetHoldingDTO directly, but:
                // totalInvested = unitsHeld * avgBuyPrice
                // fractionalOwnership = unitsHeld / ipoTotalUnits
                // We can approximate: currentValue is already correctly calculated
                // For historical: (unitsHeld / ipoTotalUnits) * historicalValuation
                // ipoTotalUnits = totalInvested / avgBuyPrice = unitsHeld * avgBuyPrice / avgBuyPrice = unitsHeld
                // Wait - totalInvested = unitsHeld * ipoPrice, avgBuyPrice = ipoPrice
                // We need ipoTotalUnits from the DB. Use currentValue as proxy:
                // currentValue = (unitsHeld / ipoTotalUnits) * latestValuation
                // fractional = unitsHeld / ipoTotalUnits
                // We derive fractional from: currentValue / latestValuation
                double latestValuation = history.isEmpty() ? 0 :
                        history.get(0).getValuationAmount();
                double fractional = latestValuation > 0
                        ? h.getCurrentValue() / latestValuation
                        : 0;

                for (Valuation v : history) {
                    if (v.getValuationDate() == null) continue;
                    String monthKey = String.format("%d-%02d",
                            v.getValuationDate().getYear(),
                            v.getValuationDate().getMonthValue());
                    double portionValue = fractional * v.getValuationAmount();
                    monthlyTotal.merge(monthKey, portionValue, Double::sum);
                }
            }
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        if (monthlyTotal.isEmpty()) {
            // Fallback: show invested vs current
            double invested = holdings.stream().mapToDouble(AssetHoldingDTO::getTotalInvested).sum();
            double current  = holdings.stream().mapToDouble(AssetHoldingDTO::getCurrentValue).sum();
            dataset.addValue(invested, "Value", "Invested");
            dataset.addValue(current,  "Value", "Current");
        } else {
            // Show last 12 months only for clarity
            List<Map.Entry<String,Double>> entries = new ArrayList<>(monthlyTotal.entrySet());
            int start = Math.max(0, entries.size() - 12);
            for (int i = start; i < entries.size(); i++) {
                Map.Entry<String,Double> e = entries.get(i);
                // Display as short label: month/year
                String[] parts = e.getKey().split("-");
                String label = parts.length == 2
                        ? getMonthAbbr(Integer.parseInt(parts[1])) + " '" + parts[0].substring(2)
                        : e.getKey();
                dataset.addValue(e.getValue(), "Portfolio Value", label);
            }
        }

        // Line chart
        JFreeChart lineChart = ChartFactory.createLineChart(
                null, null, null, dataset, PlotOrientation.VERTICAL, false, false, false);
        styleCategory(lineChart, pastelBlue, true);
        viewOne = lineChart;

        // Bar chart
        JFreeChart barChart = ChartFactory.createBarChart(
                null, null, null, dataset, PlotOrientation.VERTICAL, false, false, false);
        styleCategory(barChart, new Color(174, 198, 207, 160), false);
        viewTwo = barChart;
    }

    private void buildAllocationCharts(int investorId) throws Exception {
        PortfolioDAO portfolioDAO = new PortfolioDAO();
        List<AssetHoldingDTO> holdings = portfolioDAO.getInvestorHoldings(investorId);

        DefaultPieDataset dataset = new DefaultPieDataset();

        if (holdings.isEmpty()) {
            dataset.setValue("No Holdings", 1);
        } else {
            // Group by category using current value
            Map<String, Double> categoryValues = new LinkedHashMap<>();
            for (AssetHoldingDTO h : holdings) {
                String cat = h.getCategory() != null ? h.getCategory() : "Other";
                double val = h.getCurrentValue() > 0 ? h.getCurrentValue() : h.getTotalInvested();
                categoryValues.merge(cat, val, Double::sum);
            }
            for (Map.Entry<String, Double> e : categoryValues.entrySet()) {
                dataset.setValue(e.getKey(), e.getValue());
            }
        }

        // Ring chart
        JFreeChart ringChart = ChartFactory.createRingChart(null, dataset, true, false, false);
        stylePie(ringChart, dataset, true);
        viewOne = ringChart;

        // Pie chart
        JFreeChart pieChart = ChartFactory.createPieChart(null, dataset, true, false, false);
        stylePie(pieChart, dataset, false);
        viewTwo = pieChart;
    }

    // ── Styling helpers ───────────────────────────────────────────────────────

    private void styleCategory(JFreeChart chart, Color seriesColor, boolean isLine) {
        chart.setBackgroundPaint(cardBg);
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(cardBg);
        plot.setOutlineVisible(false);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinePaint(borderColor);
        plot.getDomainAxis().setTickLabelPaint(textMuted);
        plot.getDomainAxis().setTickLabelFont(new Font("SansSerif", Font.PLAIN, 9));
        plot.getDomainAxis().setAxisLinePaint(borderColor);
        plot.getRangeAxis().setTickLabelPaint(textMuted);
        plot.getRangeAxis().setTickLabelFont(new Font("SansSerif", Font.PLAIN, 9));
        plot.getRangeAxis().setAxisLinePaint(borderColor);
        plot.getRenderer().setSeriesPaint(0, seriesColor);
        if (isLine) {
            plot.getRenderer().setSeriesStroke(0, new BasicStroke(2.5f));
        }
    }

    private void stylePie(JFreeChart chart, DefaultPieDataset dataset, boolean isRing) {
        chart.setBackgroundPaint(cardBg);
        if (chart.getLegend() != null) {
            chart.getLegend().setBackgroundPaint(cardBg);
            chart.getLegend().setItemPaint(textMuted);
            chart.getLegend().setItemFont(new Font("SansSerif", Font.PLAIN, 10));
        }
        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setBackgroundPaint(cardBg);
        plot.setOutlineVisible(false);
        plot.setLabelGenerator(null);
        plot.setShadowPaint(null);

        Color[] colors = {pastelLilac, pastelGreen, pastelPeach, pastelBlue, pastelRed,
                          new Color(130, 200, 200), new Color(200, 230, 150)};
        int ci = 0;
        for (Object key : dataset.getKeys()) {
            plot.setSectionPaint((Comparable<?>) key, colors[ci % colors.length]);
            ci++;
        }

        if (isRing) {
            ((RingPlot) plot).setSectionDepth(0.3);
            ((RingPlot) plot).setShadowPaint(null);
        }
    }

    private JFreeChart makePlaceholder(int mode) {
        if (mode == 1) {
            DefaultCategoryDataset ds = new DefaultCategoryDataset();
            ds.addValue(0, "V", "Loading...");
            JFreeChart c = ChartFactory.createLineChart(null, null, null, ds,
                    PlotOrientation.VERTICAL, false, false, false);
            c.setBackgroundPaint(cardBg);
            c.getCategoryPlot().setBackgroundPaint(cardBg);
            c.getCategoryPlot().setOutlineVisible(false);
            return c;
        } else {
            DefaultPieDataset ds = new DefaultPieDataset();
            ds.setValue("Loading...", 1);
            JFreeChart c = ChartFactory.createPieChart(null, ds, false, false, false);
            c.setBackgroundPaint(cardBg);
            ((PiePlot) c.getPlot()).setBackgroundPaint(cardBg);
            ((PiePlot) c.getPlot()).setOutlineVisible(false);
            ((PiePlot) c.getPlot()).setSectionPaint("Loading...", borderColor);
            return c;
        }
    }

    private String getMonthAbbr(int month) {
        String[] months = {"Jan","Feb","Mar","Apr","May","Jun",
                           "Jul","Aug","Sep","Oct","Nov","Dec"};
        return (month >= 1 && month <= 12) ? months[month - 1] : "?";
    }
}
