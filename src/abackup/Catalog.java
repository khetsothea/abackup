/*
 * Cognity
 * Copyright (c) 2007 FinAnalytica, Inc.
 * All Rights Reserved.
 *
 * 2007-11-26 - Alex - created
 */
package abackup;

import java.util.*;
import java.util.zip.*;
import java.io.*;
import java.text.*;

public class Catalog {

    private Map<String, FileItem> files = new LinkedHashMap<String, FileItem>();
    private Map<String, Set<String>> groups = new LinkedHashMap<String, Set<String>>();
    private boolean useCRC32;

    private static DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd-HHmmss");

    public Catalog(Config config) {
        System.out.println("Building list of included files...");
        FileCollector includeCollector = new FileCollector();
        for (String pattern : config.getIncludedFiles()) {
            System.out.print(pattern);
            includeCollector.collect(pattern, false);
            System.out.println();
        }
        for (String pattern : config.getAtomicIncludes()) {
            System.out.print(pattern);
            includeCollector.collect(pattern, true);
            System.out.println();
        }
        includeCollector.retainAll(config.getIncludedNames());
        includeCollector.removeAll(config.getExcludedNames());
        Map<String, Set<String>> included = includeCollector.collectedNames();

        System.out.println("Building list of excluded files...");
        FileCollector excludeCollector = new FileCollector();
        for (String pattern : config.getExcludedFiles()) {
            System.out.print(pattern);
            excludeCollector.collect(pattern, false);
            System.out.println();
        }
        Map<String, Set<String>> excluded = excludeCollector.collectedNames();

        included.keySet().removeAll(excluded.keySet());
        useCRC32 = config.useCRC32();
        System.out.println("Getting file information...");
        for (Map.Entry<String, Set<String>> entry : included.entrySet()) {
            FileItem fi = new FileItem();
            fi.name = entry.getKey();
            File file = new File(entry.getKey());
            fi.size = file.length();
            fi.time = DATE_FORMAT.format(new Date(file.lastModified()));
            if (entry.getValue() != null) {
                addToGroups(fi, entry.getValue());
            }
            files.put(fi.name, fi);
        }
    }

    private void addToGroups(FileItem fi, Set<String> fileGroups) {
        fi.groups = fileGroups.toArray(new String[fileGroups.size()]);
        for (String group : fi.groups) {
            Set<String> filesInGroup = this.groups.get(group);
            if (filesInGroup == null) {
                filesInGroup = new LinkedHashSet<String>();
                this.groups.put(group, filesInGroup);
            }
            filesInGroup.add(fi.name);
        }
    }

    public Catalog(InputStream catalog) throws IOException, ParseException {
        if (catalog == null) {
            return;
        }
        BufferedReader r = new BufferedReader(new InputStreamReader(catalog));
        String line;
        while ((line = r.readLine()) != null) {
            String[] items = line.split("\t");
            if (items.length != 4) {
                continue;
            }
            FileItem fi = new FileItem();
            fi.name = items[0];
            fi.size = Long.valueOf(items[1]);
            fi.time = items[2];
            fi.crc32 = Long.valueOf(items[3], 16);
            files.put(fi.name, fi);
        }
    }

    public void printToStream(OutputStream out) {
        PrintWriter p = new PrintWriter(out);
        for (FileItem file : files.values()) {
            p.println(file);
        }
        p.flush();
    }

    public Collection<String> getUpdateList(Catalog previous) {
        if (previous == null) {
            return new LinkedList<String>(files.keySet());
        } else {
            System.out.print("Checking for updated files");
            Set<String> forUpdate = new LinkedHashSet<String>();
            int cnt = 0;
            for (FileItem cfile : files.values()) {
                FileItem pfile = previous.files.get(cfile.name);
                if (pfile == null
                        || pfile.size != cfile.size
                        || (useCRC32 ? pfile.crc32 != crc32(cfile)
                                     : !cfile.time.equals(pfile.time))) {
                    forUpdate.add(cfile.name);
                    if (cfile.groups != null) {
                        for (String group : cfile.groups) {
                            Set<String> atomicSet = this.groups.get(group);
                            forUpdate.addAll(atomicSet);
                        }
                    }
                } else if (!useCRC32) {
                    cfile.crc32 = pfile.crc32; // crc32 will not be calculated, so just copy it from the previos state
                }
                if (++cnt % 100 == 0) {
                    System.out.print(".");
                }
            }
            System.out.println();
            return forUpdate;
        }
    }

    public void updateCRC32(String file, long value) {
        FileItem fi = files.get(file);
        assert fi != null;
        fi.crc32 = value;
    }

    private static long crc32(FileItem file) {
        if (file.crc32 < 0) {
            try {
                file.crc32 = crc32(file.name);
            } catch (IOException e) {
                System.out.println("Unable to calculate file crc32, file will be included: " + file.name + " (" + e.getMessage() + ")");
            }
        }
        return file.crc32;
    }

    public static long crc32(String file) throws IOException {
        CheckedInputStream is = new CheckedInputStream(new FileInputStream(file), new CRC32());
        byte[] buf = new byte[4096];
        while (is.read(buf) >= 0) {
            /* nothing, just calculate crc32 */
        }
        return is.getChecksum().getValue();
    }

    private static class FileItem {
        String name;
        long size;
        String time;
        long crc32 = -1;
        String[] groups;

        @Override
        public String toString() {
            return name + "\t" + size + "\t" + time + "\t" + Long.toString(crc32, 16);
        }
    }
}
