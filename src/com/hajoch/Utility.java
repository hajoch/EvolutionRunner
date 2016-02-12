package com.hajoch;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by Jonatan on 12-Feb-16.
 */
public final class Utility {
    public static ArrayList<File> listFilesForFolder(final File folder) {
        ArrayList<File> files = new ArrayList<>();
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                listFilesForFolder(fileEntry);
            } else {
                files.add(fileEntry);

            }
        }
        return files;
    }

    public static void moveFile(File f, File moveToDir) {
        if (f.renameTo(new File(moveToDir+"\\" + f.getName()))) {
            System.out.println("File is moved successful!");
        } else {
            System.out.println("File is failed to move!");
        }
    }

    public static void archiveCheckpoints() {
        long archiveTime = System.currentTimeMillis();
        File folder = new File("src\\com\\hajoch\\params\\checkpoints\\");
        ArrayList<File> files = listFilesForFolder(folder);

        try {
            File dir = new File("src\\com\\hajoch\\params\\checkpoints\\" + "run_" + archiveTime);
            dir.mkdir();
            for (File f : files) {
                moveFile(f, dir);
            }

            for (File f : files) {
                f.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
