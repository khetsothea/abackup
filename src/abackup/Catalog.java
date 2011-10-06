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
    private boolean useCRC32;

    private static DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd-HHmmss");

    public Catalog(Config config) {
        System.out.println("Building list of included files...");
        FileCollector collector = new FileCollector(config.getIncludedFiles());
        collector.retainAll(config.getIncludedNames());
        collector.removeAll(config.getExcludedNames());
        Set<String> included = collector.collectedNames();
        System.out.println("Building list of excluded files...");
        Set<String> excluded = new FileCollector(config.getExcludedFiles()).collectedNames();
        included.removeAll(excluded);
        useCRC32 = config.useCRC32();
        System.out.println("Getting file information...");
        for (String s : included) {
            FileItem fi = new FileItem();
            fi.name = s;
            File file = new File(s);
            fi.size = file.length();
            fi.time = DATE_FORMAT.format(new Date(file.lastModified()));
            files.put(fi.name, fi);
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

    public List<String> getUpdateList(Catalog previous) {
        if (previous == null) {
            return new LinkedList<String>(files.keySet());
        } else {
            System.out.print("Checking for updated files");
            List<String> forUpdate = new LinkedList<String>();
            int cnt = 0;
            for (FileItem cfile : files.values()) {
                FileItem pfile = previous.files.get(cfile.name);
                if (pfile == null
                        || pfile.size != cfile.size
                        || (useCRC32 ? pfile.crc32 != crc32(cfile)
                                     : !cfile.time.equals(pfile.time))) {
                    forUpdate.add(cfile.name);
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

        @Override
        public String toString() {
            return name + "\t" + size + "\t" + time + "\t" + Long.toString(crc32, 16);
        }
    }
}
