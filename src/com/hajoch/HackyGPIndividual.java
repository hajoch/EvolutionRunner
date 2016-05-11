package com.hajoch;

import ec.EvolutionState;
import ec.gp.GPIndividual;

import java.io.DataInput;
import java.io.IOException;

/**
 * Created by Jonatan on 11-May-16.
 */
public class HackyGPIndividual extends GPIndividual {

    @Override
    public void readGenotype(EvolutionState state, DataInput dataInput) throws IOException {
        if(this.trees == null) {
            state.output.fatal("Number of trees differ in GPIndividual when reading from readGenotype(EvolutionState, DataInput).");
        }

        for(int x = 0; x < this.trees.length; ++x) {
            this.trees[x].readTree(state, dataInput);
        }

    }

}
