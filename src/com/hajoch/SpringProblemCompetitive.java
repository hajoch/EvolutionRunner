package com.hajoch;

import ec.EvolutionState;
import ec.Individual;
import ec.Population;
import ec.Problem;
import ec.coevolve.GroupedProblemForm;
import ec.gp.GPIndividual;
import ec.simple.SimpleFitness;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Jonatan on 06-Apr-16.
 */

public class SpringProblemCompetitive extends Problem implements GroupedProblemForm {

    private final long START_TIME = System.currentTimeMillis();
    // Path to the script that runs Zero-K
    private final String SCRIPT_URL = "src\\com\\hajoch\\script";
    // Path to where the BehaviourTree individual and gameLog should be saved
    private final String OUT_URL = "out\\";

    @Override
    public void preprocessPopulation(EvolutionState evolutionState, Population population, boolean[] updateFitness, boolean countVictoriesOnly) {
        if (updateFitness[0]) {
            for (int i = 0; i < population.subpops[0].individuals.length; i++) {
                ((SimpleFitness) (population.subpops[0].individuals[i].fitness)).trials = new ArrayList();
            }
        }
    }

    @Override
    public void postprocessPopulation(EvolutionState evolutionState, Population population, boolean[] updateFitness, boolean countVictoriesOnly) {
        if (updateFitness[0]) {
            for (int i = 0; i < population.subpops[0].individuals.length; i++) {
                SimpleFitness fit = ((SimpleFitness) (population.subpops[0].individuals[i].fitness));

                // average of the trials we got
                int len = fit.trials.size();
                double sum = 0;
                for (int l = 0; l < len; l++)
                    sum += ((Double) (fit.trials.get(l))).doubleValue();
                sum /= len;

                fit.setFitness(evolutionState, sum, false);
                population.subpops[0].individuals[i].evaluated = true;

                fit.trials = null;
            }
        }
    }

    @Override
    public void evaluate(EvolutionState evolutionState, Individual[] individuals, boolean[] updateFitness, boolean b, int[] ints, int i) {
        String tree0 = ((GPIndividual) individuals[0]).trees[0].child.toString();
        String tree1 = ((GPIndividual) individuals[1]).trees[0].child.toString();

        writeToFile("tree", Collections.singletonList(tree0));
        writeToFile("tree2", Collections.singletonList(tree1));

        //This variable defines the amount of times the evaluation will be run
        int p0Score = 0;
        int p1Score = 0;
        int evalCounts = 3;
        for(int e = 0; e < 3; e++){
            if(getFitness() == 0)
                p0Score += 1;
            else if(getFitness() == 1)
                p1Score += 1;
        }

        if(p0Score == p1Score){
            p0Score = 0;
            p1Score = 0;
        }


        if(updateFitness[0]){
            SimpleFitness fit = ((SimpleFitness) (individuals[0].fitness));
            fit.trials.add(new Double(p0Score));
        }

        if(updateFitness[1]){
            SimpleFitness fit = ((SimpleFitness) (individuals[1].fitness));
            fit.trials.add(new Double(p1Score));
        }
    }

    // Start game from  commandline using our custom script.
    public List<String> runZerok() throws IOException {
        List<String> output = new ArrayList<>();

        String[] cmdArgs = new String[]{"cmd.exe", "/c",
                new StringBuilder("cd ").append(SCRIPT_URL).append("&& runCompetitive.bat").toString()};


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
        executor.schedule(task, 180, TimeUnit.SECONDS);
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
    private int getFitness() {
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

        for (String s : output) {
            if (s.contains("game_message: Alliance ") && s.contains("wins!"))
                return Integer.parseInt(s.substring(s.indexOf("Alliance ") + 9, s.indexOf(" wins!")));
        }
        return 3;
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
