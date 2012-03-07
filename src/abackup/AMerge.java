package abackup;

import java.io.*;
import java.util.*;
import java.util.zip.*;

public class AMerge {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Arguments:");
            System.out.println("<job name> - name of a job description file");
            System.out.println("<from file> - zip file of the first performed backup to merge");
            System.out.println("<to file> - zip file of the last performed backup to merge");
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

        final String fromFile = args.length > 1 ? args[1] : null;
        final String toFile = args.length > 2 ? args[2] : null;

        final File outputFileName = new File(config.getOutputFile());
        File outputPath = outputFileName.getParentFile();
        String[] backups = outputPath.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith(outputFileName.getName())
                        && (fromFile == null || fromFile.compareTo(name) <= 0)
                        && (toFile == null || toFile.compareTo(name) >= 0);
            }
        });
        if (backups.length == 0) {
            System.out.println("No matching backups.");
            return;
        }
        if (backups.length == 1) {
            System.out.println("Only one matching backup, nothing to merge.");
            return;
        }
        Arrays.sort(backups);

        Map<String, String> allFiles = getAllFiles(backups, outputPath);

        Catalog firstCat = ABackup.loadCatalog(new File(outputPath, backups[0]).getPath());
        Catalog lastCat = ABackup.loadCatalog(new File(outputPath, backups[backups.length - 1]).getPath());

        Iterable<String> forMerge = lastCat.getUpdateList(firstCat);

        String mergedName = backups[backups.length - 1] + "-" + backups[0];
        System.out.println("Copying files to merged backup: " + mergedName);
        try {
            ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(new File(outputPath, mergedName)));
            for (String file : forMerge) {
                try {
                    if (!allFiles.containsKey(file)) {
                        System.out.println("Unable to find file, SKIPPED: " + file);
                        continue;
                    }
                    System.out.print(file);
                    ZipFile zf = new ZipFile(new File(outputPath, allFiles.get(file)));
                    ZipEntry entry = zf.getEntry(file);
                    zout.putNextEntry(entry);
                    InputStream in = zf.getInputStream(entry);
                    ABackup.copyStream(in, zout);
                    in.close();
                    System.out.println();
                } catch (IOException e) {
                    System.out.println(". Unable to read or write backup file, SKIPPED. " + e.getMessage());
                }
            }
            zout.flush();
            zout.close();
        } catch (FileNotFoundException e) {
            System.out.println("Unable to create merged backup file.");
        } catch (IOException e) {
            System.out.println("Unable to write merged backup file. " + e.getMessage());
        }
    }

    private static Map<String, String> getAllFiles(String[] backups, File outputPath) {
        System.out.println("Building list of files for merged backup...");
        Map<String, String> allFiles = new LinkedHashMap<String, String>();
        for (String backup : backups) {
            try {
                System.out.println(backup);
                ZipFile zf = new ZipFile(new File(outputPath, backup));
                Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>) zf.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    allFiles.put(entry.getName(), backup);
                }
            } catch (IOException e) {
                System.out.println("Unable to read backup file: " + backup + ", SKIPPED. " + e.getMessage());
            }
        }
        return allFiles;
    }

}
