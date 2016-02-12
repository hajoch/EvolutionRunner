package com.hajoch;

import ec.EvolutionState;
import ec.util.Parameter;

/**
 * Created by Jonatan on 12-Feb-16.
 */
public class Finisher extends ec.Finisher{
    @Override
    public void finishPopulation(EvolutionState evolutionState, int i) {
        System.out.println("Finisher");
        Utility.archiveCheckpoints();
    }

    @Override
    public void setup(EvolutionState evolutionState, Parameter parameter) {
    }
}
