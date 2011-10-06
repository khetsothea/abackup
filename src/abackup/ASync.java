/*
 * Cognity
 * Copyright (c) 2011 FinAnalytica
 */
package abackup;

import java.io.*;
import java.net.*;
import java.util.*;

public class ASync {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Arguments:");
            System.out.println("<job name> - name of a job description file");
            return;
        }

        // load job settings
        Config config;
        try {
            config = new Config(args[0]);
        } catch (FileNotFoundException e) {
            System.out.println("Job file not found: " + e.getMessage());
            return;
        } catch (IOException e) {
            System.out.println("Unable to load job settings: " + e.getMessage());
            return;
        }
        sync(config);
    }

    public static void sync(Config config) {
        for (String destination : config.getCopyLocations()) {
            try {
                System.out.println("Synchroning to: " + destination);
                syncTo(config.getOutputFile(), createFileDistributor(destination));
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            } catch (IOException e) {
                System.out.println("Failed to synchronize to: " + destination + ". " + e.getMessage());
            }
        }
    }

    private static void syncTo(String outputFile, FileDistributor distributor) throws IOException {
        Set<String> destFiles = new HashSet<String>(Arrays.asList(distributor.getFiles()));
        File sourceFile = new File(outputFile);
        String sourceFileName = sourceFile.getName();
        File outputDir = sourceFile.getParentFile();
        String[] sourceFiles = outputDir.list();
        Arrays.sort(sourceFiles);

        String maxDestFile = "";
        for (String dFile : destFiles) {
            if (dFile.startsWith(sourceFileName)) {
                if (maxDestFile.compareTo(dFile) < 0) {
                    maxDestFile = dFile;
                }
            }
        }

        for (String file : sourceFiles) {
            if (file.startsWith(sourceFileName) && file.compareTo(maxDestFile) > 0) {
                File source = new File(outputDir, file);
                System.out.print(source.getName());
                distributor.distibuteFile(source);
                System.out.println();
            }
        }
    }

    public static FileDistributor createFileDistributor(String destination) {
        try {
            URL url = new URL(destination);
            if ("ftp".equals(url.getProtocol())) {
                return new FTPFileDistributor(url);
            }
        } catch (MalformedURLException e) {
            File file = new File(destination);
            if (file.exists() && file.isDirectory()) {
                return new CopyFileDistributor(file);
            }
        }
        throw new IllegalArgumentException("Invalid destination: " + destination);
    }

}
