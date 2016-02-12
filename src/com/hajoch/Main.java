package com.hajoch;

import ec.Evolve;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        String[] params = new String[]{"-file", "src\\com\\hajoch\\params\\problem.params"};

            String checkpoint = getLatestCheckpoint();
            if (checkpoint.equals("")) {
                Evolve.main(params);
            } else {
                String[] fromCheckpoint = new String[]{"-checkpoint", "src\\com\\hajoch\\checkpoints\\" + checkpoint};
                Evolve.main(fromCheckpoint);
            }
    }

    public static String getLatestCheckpoint() {
        File folder = new File("src\\com\\hajoch\\checkpoints\\");
        ArrayList<File> files = Utility.listFilesForFolder(folder);

        String output = "";
        int latest = 0;
        for (File f : files) {
            int temp = Integer.parseInt(f.getName().substring(3, 4));
            if (temp > latest) {
                latest = temp;
                output = f.getName();
            }
        }
        return output;
    }
}