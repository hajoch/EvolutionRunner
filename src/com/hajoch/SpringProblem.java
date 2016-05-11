package com.hajoch;

import ZKGPBTAI.utils.*;
import bt.utils.BooleanData;
import ec.EvolutionState;
import ec.Individual;
import ec.gp.GPIndividual;
import ec.gp.GPProblem;
import ec.gp.koza.KozaFitness;
import ec.simple.SimpleProblemForm;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


/**
 * Created by Hallvard on 20.01.2016
 */
public class SpringProblem extends GPProblem implements SimpleProblemForm {

    // TODO  maybe replace start_time with generation and individual, or of the like
    private final long START_TIME = System.currentTimeMillis();
    // Path to the script that runs Zero-K
    private final String SCRIPT_URL = "src\\com\\hajoch\\script";
    // Path to where the BehaviourTree individual and gameLog should be saved
    private final String OUT_URL = "out\\";
    private static int victoryCounter = 0;

    /**
     * @param evolutionState evolutionstate.
     * @param individual     individual
     * @param i              i
     * @param i1             i1
     */
    @Override
    public void evaluate(EvolutionState evolutionState, Individual individual, int i, int i1) {
        if (!individual.evaluated) {
            BooleanData input = (BooleanData) this.input;
            String treeString = (((GPIndividual) individual).trees[0].child).toString();

            //lowmut
           //String tree0 =  "randomSelector[buildSolar, buildMex, randomSelector[buildSolar, buildMex, repairUnit]]";
            // Write BT individual to text file for the Spring Bot to read and parse
            writeToFile("tree", Collections.singletonList(treeString));
           //Long startTime = System.nanoTime();
            double f = 0;
            //This variable defines the amount of times the evaluation will be run
            int evalCounts = 100;
            for (int e = 0; e < evalCounts; e++) {
                double fitness = getFitness();
                System.out.println("fitness = " + fitness);
                if (fitness < 0.5)
                    victoryCounter++;
                f += fitness;
            }

            System.out.println("win ratio = " + victoryCounter + "/100");
            f = f / evalCounts;

            System.out.println("avg Fitness = " + f);
            KozaFitness fitness = (KozaFitness) individual.fitness;
            fitness.setStandardizedFitness(evolutionState, f);
            individual.evaluated = true;
        }
    }

    // Start game from  commandline using our custom script.
    public List<String> runZerok() throws IOException {
        List<String> output = new ArrayList<>();

        String[] cmdArgs = new String[]{"cmd.exe", "/c",
                new StringBuilder("cd ").append(SCRIPT_URL).append("&& runHeadless.bat").toString()};


        ProcessBuilder builder = new ProcessBuilder(cmdArgs);
        builder.redirectErrorStream(true);
        Process process = builder.start();

        //kill process if it takes too long
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        Runnable task = () ->
        {
            //CAUTION - may cause errors if evaluating several individuals at once
            //This will kill all spring-headless processes.
            ProcessBuilder killer = new ProcessBuilder("cmd.exe", "/c", new StringBuilder("taskkill /f /im ").append("spring-headless.exe").toString());
            try {
                Process killerProc = killer.start();
                process.destroy();
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
        executor.schedule(task, 1500, TimeUnit.SECONDS);
        //
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            // Read console output
            String line;
            while (process.isAlive()) {
                line = reader.readLine();
                if (null != line) {
                    output.add(line);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        executor.shutdownNow();
        return output;
    }

    /**
     * Run game, interpret results and return fitness.
     * NB: This method is problem specific.
     *
     * @return fitness (Kozafirness)
     */
    private double getFitness() {
        List<String> output = new ArrayList<>();

        boolean running = true;
        while (running) {
            try {
                output = runZerok();
                running = false;
            } catch (IOException e) {
                System.out.println("zero K crashed. Restarting");
            }
        }

        // Write the console output to file
        writeToFile("out" + START_TIME, output);

        return getFitness(0, output);
    }

    public double getFitness(int teamid, List<String> output) {
        int winner = 0;
        int time = 0;
        double avgEco = 0;
        int soldiers = 0;
        double avgMex = 0;
        double peakIncome = 0;
        double killVsExpenditureMetal = 0;
        for (String s : output) {
            if (s.contains("game_message: Alliance ") && s.contains("wins!")) {
                winner = Integer.parseInt(s.substring(s.indexOf("Alliance ") + 9, s.indexOf(" wins!")));
            }
            if (s.contains("END") && (Integer.parseInt(s.substring(s.indexOf("teamId: ") + 8, s.indexOf(" time: "))) == teamid)) {
                time = Integer.parseInt(s.substring(s.indexOf("time: ") + 6, s.indexOf(" Soldiers: ")));
                //System.out.println("Time: " + time);
                soldiers = Integer.parseInt(s.substring(s.indexOf("Soldiers: ") + 10, s.indexOf(" avgEco: ")));
                //System.out.println("Soldiers: " + soldiers);
                avgEco = Double.parseDouble(s.substring(s.indexOf("avgEco: ") + 8, s.indexOf("avgMex: ")));
                //System.out.println("avgEco: " + avgEco);
                avgMex = Double.parseDouble(s.substring(s.indexOf("avgMex: ") + 8, s.indexOf("peakIncome: ")));
                peakIncome = Double.parseDouble(s.substring(s.indexOf("peakIncome: ") + 12, s.indexOf("killVsExpenditureMetal: ")));
                killVsExpenditureMetal = Double.parseDouble(s.substring(s.indexOf("killVsExpenditureMetal: ") + 23, s.length()));
            }
        }

        double ecoFitness = avgEco / 50d;
        double fitness = (ecoFitness * 0.25d) + (avgMex * 0.1d) + (peakIncome * 0.05d) + (killVsExpenditureMetal * 0.10d);
        if (winner == teamid) {
            fitness += 0.5d;
            //System.out.println("We won" + " Time: " + time + " avgEco: " + avgEco + " Soldiers: " + soldiers + " avgMex: " + avgMex + " peakIncome: " + peakIncome + " killVsExpenditureMetal: " + killVsExpenditureMetal);
        } else {
            //System.out.println("We lost" + " Time: " + time + " avgEco: " + avgEco + " Soldiers: " + soldiers + " avgMex: " + avgMex + " peakIncome: " + peakIncome + " killVsExpenditureMetal: " + killVsExpenditureMetal);
        }
        return 1d - fitness;
    }

    /**
     * Creates a new file in the OUT_URL containing the content and the given name.
     *
     * @param name    filename. Will be postfixed with the time at run-start.
     * @param content content of the file
     * @return returns true if no error occurred during the process
     */
    private boolean writeToFile(String name, List<String> content) {

        File file = new File(new StringBuilder(OUT_URL).append(name).append(".txt").toString());
        try {
            file.getParentFile().mkdirs();
            if (!file.exists())
                file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (PrintWriter printer = new PrintWriter(file)) {
            for (String s : content)
                printer.write(s + "\n");
            printer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

}
