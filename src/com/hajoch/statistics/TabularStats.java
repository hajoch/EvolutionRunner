package com.hajoch.statistics;

import com.hajoch.Utility;
import ec.*;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.gp.koza.KozaFitness;
import ec.simple.SimpleProblemForm;
import ec.steadystate.SteadyStateStatisticsForm;
import ec.util.Parameter;

import java.io.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by Jonatan on 18-Sep-15.
 */


public class TabularStats extends Statistics implements SteadyStateStatisticsForm {
    public Individual[] getBestSoFar() {
        return best_of_run;
    }

    /**
     * log file parameter
     */
    public static final String P_STATISTICS_FILE = "file";


    //data stored for visualisation
    ArrayList<Double> avgFitnessPerGen = new ArrayList<>();
    ArrayList<Double> avgSizePerGen = new ArrayList<>();
    ArrayList<Double> bestInds = new ArrayList<>();
    HashMap<String, Double> nodeUsage = new HashMap<>();

    String runName = "";
    /**
     * The Statistics' log
     */
    public int statisticslog = 0;  // stdout

    /**
     * The best individual we've found so far
     */
    public Individual[] best_of_run = null;

    /**
     * Should we compress the file?
     */
    public static PrintWriter popForHumansPrinter;
    public static PrintWriter popForECJPrinter;

    public void setup(final EvolutionState state, final Parameter base) {
        super.setup(state, base);
        Date date = new Date();
        runName = "" + date.getTime();

        File statisticsFile = new File(".\\runs\\" + runName + "\\out.txt");
        statisticsFile.getParentFile().mkdirs();
        try {
            statisticslog = state.output.addLog(statisticsFile, true);

            popForHumansPrinter = new PrintWriter(new BufferedWriter(new FileWriter("runs\\" + runName + "\\population.txt", true)));
            popForECJPrinter = new PrintWriter(new BufferedWriter(new FileWriter("runs\\" + runName + "\\popDetails.txt", true)));
        } catch (IOException i) {
            state.output.fatal("An IOException occurred while trying to create the log " + statisticsFile + ":\n" + i);
        }
    }

    public void postInitializationStatistics(final EvolutionState state) {
        super.postInitializationStatistics(state);

        // set up our best_of_run array -- can't do this in setup, because
        // we don't know if the number of subpopulations has been determined yet
        best_of_run = new Individual[state.population.subpops.length];
    }

    /**
     * Logs the best individual of the generation.
     */
    boolean warned = false;

    public void setNodeCount(GPNode node){
        String nodeName = node.toString().replaceAll("[^a-zA-Z]", " ");;
        String arr[] = nodeName.split(" ");
        for(String s: arr){
            if(s.length() > 1)
                nodeUsage.put(s, nodeUsage.get(s) != null ? nodeUsage.get(s) + 1 : 1);
        }
    }

    public String getTree(GPNode node) {
        if (node.children.length == 2)
            return "(" + getTree(node.children[0]) + node.toStringForHumans() + getTree(node.children[1]) + ")";
        else if (node.children.length == 1)
            return node.toStringForHumans() + "(" + getTree(node.children[0]) + ")";
        else
            return node.toStringForHumans();
    }

    public void initPrintWriter() {
        try {
            popForHumansPrinter = new PrintWriter(new BufferedWriter(new FileWriter("runs\\" + runName + "\\population.txt", true)));
            popForECJPrinter = new PrintWriter(new BufferedWriter(new FileWriter("runs\\" + runName + "\\popDetails.txt", true)));
        } catch (IOException i) {
            System.out.println("initprintwriter error ");
        }
    }

    public void postEvaluationStatistics(final EvolutionState state) {
        super.postEvaluationStatistics(state);

        if (popForHumansPrinter == null || popForECJPrinter == null)
            initPrintWriter();
        // for now we just print the best fitness per subpopulation.
        Individual[] best_i = new Individual[state.population.subpops.length];  // quiets compiler complaints
        for (int x = 0; x < state.population.subpops.length; x++) {
            best_i[x] = state.population.subpops[x].individuals[0];
            for (int y = 1; y < state.population.subpops[x].individuals.length; y++) {
                if (state.population.subpops[x].individuals[y] == null) {
                    if (!warned) {
                        state.output.warnOnce("Null individuals found in subpopulation");
                        warned = true;  // we do this rather than relying on warnOnce because it is much faster in a tight loop
                    }
                } else if (best_i[x] == null || state.population.subpops[x].individuals[y].fitness.betterThan(best_i[x].fitness))
                    best_i[x] = state.population.subpops[x].individuals[y];
                if (best_i[x] == null) {
                    if (!warned) {
                        state.output.warnOnce("Null individuals found in subpopulation");
                        warned = true;  // we do this rather than relying on warnOnce because it is much faster in a tight loop
                    }
                }
            }

            // now test to see if it's the new best_of_run
            if (best_of_run[x] == null || best_i[x].fitness.betterThan(best_of_run[x].fitness))
                best_of_run[x] = (Individual) (best_i[x].clone());
        }

        // print the best-of-generation individual
        state.output.println("\nGeneration: " + state.generation, statisticslog);
        state.output.println("Best Individual:", statisticslog);
        for (int x = 0; x < state.population.subpops.length; x++) {

            state.output.println("Subpopulation " + x + ":", statisticslog);
            best_i[x].printIndividualForHumans(state, statisticslog);
            state.output.message("Subpop " + x + " best fitness of generation" +
                    (best_i[x].evaluated ? " " : " (evaluated flag not set): ") +
                    best_i[x].fitness.fitnessToStringForHumans());
            //TODO
            bestInds.add(1d - ((KozaFitness) best_i[x].fitness).standardizedFitness());

            // describe the winner if there is a description
            if (state.evaluator.p_problem instanceof SimpleProblemForm)
                ((SimpleProblemForm) (state.evaluator.p_problem.clone())).describe(state, best_i[x], x, 0, statisticslog);
        }
        //calculate average fitness and print
        double avgFitness = 0;
        double avgSize = 0;
        for (Individual ind : state.population.subpops[0].individuals) {
            avgFitness += ((KozaFitness) ind.fitness).standardizedFitness();
            avgSize += ind.size();
            setNodeCount(((GPIndividual) ind).trees[0].child);
        }
        avgFitness = avgFitness / state.population.subpops[0].individuals.length;
        avgFitnessPerGen.add(avgFitness);
        avgSize = avgSize / state.population.subpops[0].individuals.length;
        avgSizePerGen.add(avgSize);
        state.output.message("Average fitness: " + avgFitness + " Average size: " + avgSize);
        popForHumansPrinter.print("Average fitness: " + avgFitness + " Average size: " + avgSize);

        ResultsSingleton.setNodeOcc(nodeUsage);
        ResultsSingleton.setAvgFitness(avgFitnessPerGen);
        ResultsSingleton.setAvgSize(avgSizePerGen);
        ResultsSingleton.setBestInds(bestInds);
        ResultsSingleton.drawChart(runName, false);
        writePopulation(state.population.subpops[0].individuals, state);
    }

    public void writePopulation(Individual[] pop, EvolutionState state) {
        popForHumansPrinter.print("\r\n Generation " + state.generation + "\r\n");
        for (int i = 0; i < pop.length; i++) {
            popForHumansPrinter.print(i + " " + getTree(((GPIndividual) pop[i]).trees[0].child).replace(" ", "") + " " + ((KozaFitness) pop[i].fitness).standardizedFitness() + "\r\n");
            pop[0].printIndividual(state, popForECJPrinter);
        }
    }

    /**
     * Allows MultiObjectiveStatistics etc. to call super.super.finalStatistics(...) without
     * calling super.finalStatistics(...)
     */
    protected void bypassFinalStatistics(EvolutionState state, int result) {
        super.finalStatistics(state, result);
    }

    /**
     * Logs the best individual of the run.
     */
    public void finalStatistics(final EvolutionState state, final int result) {
        super.finalStatistics(state, result);

        // for now we just print the best fitness

        state.output.println("\nBest Individual of Run:", statisticslog);
        for (int x = 0; x < state.population.subpops.length; x++) {
            state.output.println("Subpopulation " + x + ":", statisticslog);
            best_of_run[x].printIndividualForHumans(state, statisticslog);
            state.output.message("Subpop " + x + " best fitness of run: " + best_of_run[x].fitness.fitnessToStringForHumans());

            // finally describe the winner if there is a description
            if (state.evaluator.p_problem instanceof SimpleProblemForm)
                ((SimpleProblemForm) (state.evaluator.p_problem.clone())).describe(state, best_of_run[x], x, 0, statisticslog);
        }

        ResultsSingleton.drawChart(runName, true);
        popForHumansPrinter.close();
        popForECJPrinter.close();

        //archive checkpoints
        Utility.archiveCheckpoints(".\\runs\\" + runName + "\\checkpoints");
    }
}
