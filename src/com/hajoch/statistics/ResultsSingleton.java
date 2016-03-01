package com.hajoch.statistics;

import com.hajoch.visualization.PopFitVsSizeChart;
import com.hajoch.visualization.TerminalVsFitChart;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Jonatan on 07-Oct-15.
 */
public class ResultsSingleton {

    public static HashMap<String, Double> getNodeOcc() {
        return nodeOcc;
    }

    public static void setNodeOcc(HashMap<String, Double> nodeOcc) {
        ResultsSingleton.nodeOcc = nodeOcc;
    }

    public static volatile HashMap<String, Double> nodeOcc = new HashMap<>();
    public static ArrayList<Double> getAvgFitness() {
        return avgFitness;
    }

    public static void setAvgFitness(ArrayList<Double> avgFitness) {
        ResultsSingleton.avgFitness = avgFitness;
    }

    public static volatile ArrayList<Double> avgFitness = new ArrayList<>();

    public static ArrayList<Double> getAvgSize() {
        return avgSize;
    }

    public static void setAvgSize(ArrayList<Double> avgSize) {
        ResultsSingleton.avgSize = avgSize;

    }

    public static volatile ArrayList<Double> bestInds = new ArrayList<>();

    public static ArrayList<Double> getBestInds() {
        return bestInds;
    }

    public static void setBestInds(ArrayList<Double> bestInds) {
        ResultsSingleton.bestInds = bestInds;

    }

    public static volatile ArrayList<Double> avgSize = new ArrayList<>();
    private static final ResultsSingleton ourInstance = new ResultsSingleton();

    public static final ResultsSingleton getInstance() {
        return ourInstance;
    }

    private ResultsSingleton() {
    }
    public static void drawChart(String name, boolean visible){
        PopFitVsSizeChart chart = new PopFitVsSizeChart(name);
        TerminalVsFitChart chart2 = new TerminalVsFitChart(name);
    }

}
