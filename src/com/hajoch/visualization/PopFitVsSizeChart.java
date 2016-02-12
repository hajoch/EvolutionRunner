package com.hajoch.visualization;

import com.hajoch.statistics.ResultsSingleton;
import org.jfree.chart.*;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by Jonatan on 07-Oct-15.
 */
public class PopFitVsSizeChart extends JFrame {

    public PopFitVsSizeChart(String name) {
        super("PopFitVsSizeChart");

        JPanel chartPanel = createChartPanel(name);
        add(chartPanel, BorderLayout.CENTER);

        setSize(640, 480);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);


    }

    private JPanel createChartPanel(String name) {
        String chartTitle = "PopFitVsSizeChart";
        String xAxisLabel = "Generation";
        String yAxisLabel = "Average Fitness/Size";
        XYDataset dataset = createDataSet(ResultsSingleton.getAvgFitness(), ResultsSingleton.getAvgSize());

        JFreeChart chart = ChartFactory.createXYLineChart(chartTitle,
                xAxisLabel, yAxisLabel, dataset);

        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        plot.setRenderer(renderer);

        plot.setBackgroundPaint(Color.DARK_GRAY);
        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.BLACK);
        plot.setDomainGridlinesVisible(true);
        plot.setDomainGridlinePaint(Color.BLACK);

        createPng(chart, name);
        return new ChartPanel(chart);
    }

    private void createPng(JFreeChart chart, String name){
        File imageFile = new File("runs\\" + name + "\\popvsfitchart.png");
        int width = 1920;
        int height = 1080;

        try {
            ChartUtilities.saveChartAsPNG(imageFile, chart, width, height);
        } catch (IOException ex) {
            System.err.println(ex);
        }
    }


    private static XYSeries createSeries(ArrayList<Double> input, String name) {
        XYSeries series = new XYSeries(name);
        double max = Collections.max(input);
        double min = Collections.min(input);
        if (name == "avgFitness") {
            for (int i = 0; i < input.size(); i++) {
                series.add((double) i, 1 - input.get(i));
                //(1 - input.get(i)) * 100
            }
        } else {
            for (int i = 0; i < input.size(); i++) {
                series.add((double) i, (input.get(i) - min)/(max-min));
            }
        }
        return series;
    }

    public static XYDataset createDataSet(ArrayList<Double>... args) {

        XYSeriesCollection dataset = new XYSeriesCollection();
        for (int i = 0; i < args.length; i++) {
            if (i == 0)
                dataset.addSeries(createSeries(args[i], "avgFitness"));
            else
                dataset.addSeries(createSeries(args[i], "avgSize"));
        }
        return dataset;
    }
}
