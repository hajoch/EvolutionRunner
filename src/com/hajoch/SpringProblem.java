package com.hajoch;

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
     *
     * @param evolutionState    evolutionstate.
     * @param individual        individual
     * @param i                 i
     * @param i1                i1
     */
    @Override
    public void evaluate(EvolutionState evolutionState, Individual individual, int i, int i1) {
        if(!individual.evaluated) {
            BooleanData input = (BooleanData)this.input;
            String treeString = (((GPIndividual)individual).trees[0].child).toString();
            // Write BT individual to text file for the Spring Bot to read and parse
            writeToFile("bt", Collections.singletonList(treeString));

            double f = getFitness();

            KozaFitness fitness = (KozaFitness)individual.fitness;
            fitness.setStandardizedFitness(evolutionState, f);
            individual.evaluated = true;
        }
    }

    /**
     * Run game, interpret results and return fitness.
     * NB: This method is problem specific.
     * @return                  fitness (Kozafirness)
     */
    private double getFitness() {

        List<String> output = new ArrayList<>();

        String[] cmdArgs = new String[]{"cmd.exe", "/c",
                new StringBuilder("cd ").append(SCRIPT_URL).append("&& runHeadless.bat").toString()};

        // Start game from  commandline using our custom script.
        try {
            ProcessBuilder builder = new ProcessBuilder(cmdArgs);
            builder.redirectErrorStream(true);
            Process process = builder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            // Read console output
            String line;
            while(process.isAlive()) {
                line = reader.readLine();
                if(null != line) {
                    output.add(line);
                }
            }
        } catch (IOException e) { e.printStackTrace(); }


        // Write the console output to file
        writeToFile("out", output);

        int winner = 0;
        int teamid = 0;
        int time = 0;
        int avgEco = 0;
        int soldiers = 0;
        for(String s: output){
            if(s.contains("game_message: Alliance ")){
                winner = Integer.parseInt(s.substring(s.indexOf("Alliance ")+9, s.indexOf(" wins")));
            }
            if(s.contains("END")){
                teamid = Integer.parseInt(s.substring(s.indexOf("teamid: ")+8, s.indexOf(" time: ")));
                time = Integer.parseInt(s.substring(s.indexOf("time: ")+6, s.indexOf(" Soldiers: ")));
                soldiers = Integer.parseInt(s.substring(s.indexOf("Soldiers: ")+10, s.indexOf(" avgEco: ")));
                avgEco = Integer.parseInt(s.substring(s.indexOf("avgEco: ")+8, s.length()));
            }
        }

        if(winner == teamid)
            return 1;
        else
            return -1;
        // Calculate the fitness //TODO
    }

    /**
     * Creates a new file in the OUT_URL containing the content and the given name.
     * @param name              filename. Will be postfixed with the time at run-start.
     * @param content           content of the file
     * @return                  returns true if no error occurred during the process
     */
    private boolean writeToFile(String name, List<String> content){

        PrintWriter printer;

        File file = new File(new StringBuilder(OUT_URL).append(name).append(START_TIME).append(".txt").toString());
        try {
            file.getParentFile().mkdirs();
            if(!file.exists())
                file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            printer = new PrintWriter(file);
            for(String s : content)
                printer.write(s+"\n");
            printer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
