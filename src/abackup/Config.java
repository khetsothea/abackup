/*
 * Cognity
 * Copyright (c) 2007 FinAnalytica, Inc.
 * All Rights Reserved.
 *
 * 2007-11-26 - Alex - created
 */
package abackup;

import java.io.*;
import java.util.*;

public class Config {

    private List<String> includedFiles = new LinkedList<String>();
    private List<String> atomicIncludes = new LinkedList<String>();
    private List<String> excludedFiles = new LinkedList<String>();
    private List<String> includedNames = new LinkedList<String>();
    private List<String> excludedNames = new LinkedList<String>();
    private List<String> copyLocations = new LinkedList<String>();
    private Set<String> compressedExtensions = new HashSet<String>();
    private String outputFile = "Backup";

    private String catalogFile;
    private boolean useCRC32 = false;

    private static final String INCLUDE_FILE = "include-file:";
    private static final String INCLUDE_ATOMIC = "include-atomic:";
    private static final String EXCLUDE_FILE = "exclude-file:";
    private static final String INCLUDE_NAMES = "include-names:";
    private static final String EXCLUDE_NAMES = "exclude-names:";
    private static final String NOZIP = "nozip:";
    private static final String OUTPUT = "output:";
    private static final String CRC32 = "crc32";
    private static final String CATALOG_FILE = "catalog-file:";
    private static final String COPY_TO = "copy-to:";

    public Config(String fileName) throws IOException {
        BufferedReader r = new BufferedReader(new FileReader(fileName));
        String line;
        while ((line = r.readLine()) != null) {
            line = line.trim();
            if (line.length() == 0 || line.charAt(0) == '#' || line.charAt(0) == ';') {
                continue;
            }
            if (line.startsWith(INCLUDE_FILE)) {
                includedFiles.add(line.substring(INCLUDE_FILE.length()).trim());
            } else if (line.startsWith(INCLUDE_ATOMIC)) {
                atomicIncludes.add(line.substring(INCLUDE_ATOMIC.length()).trim());
            } else if (line.startsWith(EXCLUDE_FILE)) {
                excludedFiles.add(line.substring(EXCLUDE_FILE.length()).trim());
            } else if (line.startsWith(INCLUDE_NAMES)) {
                includedNames.add(line.substring(INCLUDE_NAMES.length()).trim());
            } else if (line.startsWith(EXCLUDE_NAMES)) {
                excludedNames.add(line.substring(EXCLUDE_NAMES.length()).trim());
            } else if (line.startsWith(NOZIP)) {
                compressedExtensions.add(line.substring(NOZIP.length()).trim());
            } else if (line.startsWith(OUTPUT)) {
                outputFile = line.substring(OUTPUT.length()).trim();
            } else if (line.startsWith(CATALOG_FILE)) {
                catalogFile = line.substring(CATALOG_FILE.length()).trim();
            } else if (line.startsWith(COPY_TO)) {
                copyLocations.add(line.substring(COPY_TO.length()).trim());
            } else if (line.equals(CRC32)) {
                useCRC32 = true;
            } else {
                System.out.println("Unrecognized directive:");
                System.out.println(line);
                System.out.println("Line was ignored.");
            }
        }
    }


    public List<String> getIncludedFiles() {
        return Collections.unmodifiableList(includedFiles);
    }

    public List<String> getAtomicIncludes() {
        return Collections.unmodifiableList(atomicIncludes);
    }

    public List<String> getExcludedFiles() {
        return Collections.unmodifiableList(excludedFiles);
    }

    public List<String> getIncludedNames() {
        return Collections.unmodifiableList(includedNames);
    }

    public List<String> getExcludedNames() {
        return Collections.unmodifiableList(excludedNames);
    }

    public Set<String> getCompressedExtensions() {
        return Collections.unmodifiableSet(compressedExtensions);
    }

    public List<String> getCopyLocations() {
        return copyLocations;
    }

    public String getOutputFile() {
        return outputFile;
    }

    public String getCatalogFile() {
        return catalogFile;
    }

    public boolean useCRC32() {
        return useCRC32;
    }
}
