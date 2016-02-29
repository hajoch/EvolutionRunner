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
            // Write BT individual to text file for the Spring Bot to read and parse
            writeToFile("tree", Collections.singletonList(treeString));

            //Long startTime = System.nanoTime();
            double f = getFitness();

            KozaFitness fitness = (KozaFitness) individual.fitness;
            fitness.setStandardizedFitness(evolutionState, f);
            individual.evaluated = true;
        }
    }

    // Start game from  commandline using our custom script.
    public List<String> runZerok() throws IOException{
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
        executor.schedule(task, 1000, TimeUnit.SECONDS);
        //
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        // Read console output
        String line;
        while (process.isAlive()) {
            line = reader.readLine();
            if (null != line) {
                output.add(line);
            }
        }

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
        while(running) {
            try {
                output = runZerok();
                running = false;
            } catch (IOException e) {
                System.out.println("zero K crashed. Restarting");
            }
        }

        // Write the console output to file
        writeToFile("out"+START_TIME, output);

        int winner = 0;
        int teamid = 1;
        int time = 0;
        int avgEco = 0;
        int soldiers = 0;
        for (String s : output) {
            if (s.contains("game_message: Alliance ") && s.contains("wins!")) {
                winner = Integer.parseInt(s.substring(s.indexOf("Alliance ") + 9, s.indexOf(" wins!")));
            }
            if (s.contains("END")) {
                teamid = Integer.parseInt(s.substring(s.indexOf("teamId: ") + 8, s.indexOf(" time: ")));
                //System.out.println("Teamid: " + teamid);
                time = Integer.parseInt(s.substring(s.indexOf("time: ") + 6, s.indexOf(" Soldiers: ")));
                //System.out.println("Time: " + time);
                soldiers = Integer.parseInt(s.substring(s.indexOf("Soldiers: ") + 10, s.indexOf(" avgEco: ")));
                //System.out.println("Soldiers: " + soldiers);
                avgEco = Integer.parseInt(s.substring(s.indexOf("avgEco: ") + 8, s.length()));
                //System.out.println("avgEco: " + avgEco);
            }
        }

        double ecoFitness = avgEco/50d;
        double timeFitness = 0d;
        if (winner == teamid) {
            System.out.println("We won." + " Time: " + time + " Soldiers: " + soldiers + " avgEco: "+ avgEco);
            timeFitness =  1d - (time/1000d);
        } else {
            System.out.println("We lost." + " Time: " + time + " Soldiers: " + soldiers + " avgEco: "+ avgEco);
            timeFitness =  0d + (time/1000d);
        }

        System.out.println("Fitness = " + (1d-ecoFitness));
        return 1d-ecoFitness;
        // Calculate the fitness //TODO
    }

    /**
     * Creates a new file in the OUT_URL containing the content and the given name.
     *
     * @param name    filename. Will be postfixed with the time at run-start.
     * @param content content of the file
     * @return returns true if no error occurred during the process
     */
    private boolean writeToFile(String name, List<String> content) {

        PrintWriter printer;

        File file = new File(new StringBuilder(OUT_URL).append(name).append(".txt").toString());
        try {
            file.getParentFile().mkdirs();
            if (!file.exists())
                file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            printer = new PrintWriter(file);
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
