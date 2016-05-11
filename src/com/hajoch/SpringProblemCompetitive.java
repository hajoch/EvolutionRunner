package com.hajoch;

import ec.EvolutionState;
import ec.Individual;
import ec.Population;
import ec.Problem;
import ec.coevolve.GroupedProblemForm;
import ec.gp.GPIndividual;
import ec.gp.koza.KozaFitness;
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


    //clear trials
    @Override
    public void preprocessPopulation(EvolutionState evolutionState, Population population, boolean[] updateFitness, boolean countVictoriesOnly) {
        for (int i = 0; i < population.subpops.length; i++) {
            if (updateFitness[i]) {
                for (int j = 0; j < population.subpops[i].individuals.length; j++) {
                    SimpleFitness fit = (SimpleFitness) (population.subpops[i].individuals[j].fitness);
                    fit.trials = new ArrayList();
                }
            }
        }
    }

    @Override
    public void postprocessPopulation(EvolutionState evolutionState, Population population, boolean[] updateFitness, boolean countVictoriesOnly) {
        for(int i = 0; i < population.subpops.length; i++){
            if (updateFitness[i]) {
                for (int j = 0; j < population.subpops[i].individuals.length; j++) {
                    SimpleFitness fit = (SimpleFitness) (population.subpops[i].individuals[j].fitness);

                    if(!countVictoriesOnly) {
                        //set fitness to average of trials
                        int len = fit.trials.size();
                        double sum = 0;
                        for (int l = 0; l < len; l++) {
                            sum += ((Double) (fit.trials.get(l))).doubleValue();
                        }

                        //fit.setFitness(evolutionState, sum / len, false);
                    }
                    population.subpops[i].individuals[j].evaluated = true;
                }
            }
        }
    }

    @Override
    public void evaluate(EvolutionState evolutionState, Individual[] individuals, boolean[] updateFitness, boolean b, int[] ints, int i) {
        String tree0 = ((GPIndividual) individuals[0]).trees[0].child.toString();
        String tree1 = ((GPIndividual) individuals[1]).trees[0].child.toString();

        //e3
        //tree0 = "randomSelector[buildSolar, buildMex, randomSelector[buildSolar, buildMex, repairUnit]]";
        //largepop
        //tree1 = "sequence[succeeder(randomSelector[randomSelector[highMetal,buildMex,highEnergy],buildSolar,inverter(highMetal)]),sequence[topOfHill,untilFail(selector[sequence[topOfHill,highTension,lowMetal,highMetal],sequence[topOfHill,sequence[inverter(highEnergy),highMetal,highEnergy,selector[isAreaControlled,topOfHill],repairUnit],lowMetal,highMetal]]),randomSelector[topOfHill,selector[sequence[topOfHill,highTension,untilFail(sequence[topOfHill,highMetal,highEnergy,lowHealth,repairUnit]),highMetal],sequence[topOfHill,inverter(highEnergy),lowMetal,highMetal]],selector[succeeder(inverter(highEnergy)),untilFail(sequence[topOfHill,highMetal,highEnergy,inverter(highEnergy),repairUnit])]],sequence[lowMetal,succeeder(untilFail(inverter(lowMetal))),topOfHill,highMetal],topOfHill],succeeder(untilFail(sequence[topOfHill,highMetal,highEnergy,inverter(highEnergy),repairUnit]))]";

        writeToFile("tree", Collections.singletonList(tree0));
        writeToFile("tree2", Collections.singletonList(tree1));


        System.out.println(individuals[0].toString());
        System.out.println("        VS");
        System.out.println(individuals[1].toString());

        int p0Wins = 0;
        double p0Fitness = 0;
        double avgp0Fitness = 0;
        int p1Wins = 0;
        double p1Fitness = 0;
        double avgp1Fitness = 0;
        //This variable defines the amount of times the evaluation will be run
        int evalCounts = 3;

        //Long startTime = System.nanoTime();
        double f = 0;
        //This variable defines the amount of times the evaluation will be run
/*        for (int e = 0; e < evalCounts; e++) {
            List<String>output = getFitness();
            p0Fitness = getFitness(0, output);
            p1Fitness = getFitness(1, output);
            avgp0Fitness += p0Fitness;
            avgp1Fitness += p1Fitness;

            System.out.println("p0fitness = " + p0Fitness);
            System.out.println("p1Fitness = " + p1Fitness);
            System.out.println();
            if (p0Fitness < 0.5)
                p0Wins++;
            else if(p1Fitness < 0.5)
                p1Wins++;
            else{
                if (p0Fitness < p1Fitness)
                    p0Wins++;
                else
                    p1Wins++;

            }
        }

        System.out.println("p0wins = " + p0Wins);
        System.out.println("p1wins = " + p1Wins);
        System.out.println("avgp0Fitness = " + avgp0Fitness / evalCounts);
        System.out.println("avgp1Fitness = " + avgp1Fitness/evalCounts);*/
        for (int e = 0; e < evalCounts; e++) {
            List<String>output = getFitness();
            p0Fitness = getFitness(0, output);
            p1Fitness = getFitness(1, output);
            if (p0Fitness > 0.5)
                p0Wins++;
            else if(p1Fitness < 0.5)
                p1Wins++;
            else{
                if (p0Fitness > p1Fitness)
                    p0Wins++;
                else
                    p1Wins++;

            }

            if(p0Wins == 2) {
                SimpleFitness fit = ((SimpleFitness) (individuals[0].fitness));
                fit.trials.add(new Double(1));
                System.out.println("Player 0 wins");
                break;
            }
            if(p1Wins == 2) {
                SimpleFitness fit = ((SimpleFitness) (individuals[1].fitness));
                fit.trials.add(new Double(1));
                System.out.println("Player 1 wins");
                break;
            }
        }

        System.out.println(" ");
    }

    /**
     * Run game, interpret results and return fitness.
     * NB: This method is problem specific.
     *
     * @return fitness (Kozafirness)
     */
    private List<String> getFitness() {
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
        return output;
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
        executor.schedule(task, 120, TimeUnit.SECONDS);
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
