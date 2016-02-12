package com.hajoch.visualization;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import statistics.ResultsSingleton;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Jonatan on 09-Oct-15.
 */
public class TerminalVsFitChart extends JFrame {
    public TerminalVsFitChart(String name) {
        super("NodeOccChart");

        JPanel chartPanel = createChartPanel(name);
        add(chartPanel, BorderLayout.CENTER);

        setSize(640, 480);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);


    }

    private JPanel createChartPanel(String name) {
        String chartTitle = "Node Occurrence Chart";
        CategoryDataset dataset = createDataSet(ResultsSingleton.getNodeOcc());
        String xAxisLabel = "nodes";
        String yAxisLabel = "Occurrences";
        JFreeChart chart = ChartFactory.createBarChart(chartTitle, xAxisLabel, yAxisLabel, dataset);

        final CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.DARK_GRAY);
        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.BLACK);
        plot.setDomainGridlinesVisible(true);
        plot.setDomainGridlinePaint(Color.BLACK);

        createPng(chart, name);
        return new ChartPanel(chart);
    }


    private void createPng(JFreeChart chart, String name){
        File imageFile = new File("runs\\" + name + "\\NodeOccChart.png");
        int width = 1280;
        int height = 920;

        try {
            ChartUtilities.saveChartAsPNG(imageFile, chart, width, height);
        } catch (IOException ex) {
            System.err.println(ex);
        }
    }
    public static  Map.Entry<String, Double> getMaxFromHashMap(HashMap<String, Double> input) {
        Map.Entry<String, Double> maxEntry = null;
        for (Map.Entry<String, Double> entry : input.entrySet()) {
            if (maxEntry == null || entry.getValue() > maxEntry.getValue()) {
                maxEntry = entry;
            }
        }
        return maxEntry;
    }

    public static Map.Entry<String, Double> getMinFromHashMap(HashMap<String, Double> input) {
        Map.Entry<String, Double> maxEntry = null;
        for (Map.Entry<String, Double> entry : input.entrySet()) {
            if (maxEntry == null || entry.getValue() < maxEntry.getValue()) {
                maxEntry = entry;
            }
        }
        return maxEntry;
    }

    public static HashMap<String, Double> rescaleData(HashMap<String, Double> arg){
        HashMap<String, Double> rescaledHashmap = new HashMap<>();

        double maxVal = getMaxFromHashMap(arg).getValue();
        double minVal = getMinFromHashMap(arg).getValue();
        for (Map.Entry<String, Double> entry : arg.entrySet()) {
            rescaledHashmap.put(entry.getKey(), (entry.getValue() - minVal)/(maxVal - minVal));
        }
        return rescaledHashmap;
    }

    public static CategoryDataset createDataSet(HashMap<String, Double> arg) {
        //row keys
        final String series = "Total times used";
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();


        for (Map.Entry<String, Double> entry : rescaleData(arg).entrySet()) {
            dataset.addValue(entry.getValue(), series, entry.getKey());
        }
        return dataset;
    }
}
