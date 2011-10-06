/*
 * Cognity
 * Copyright (c) 2007 FinAnalytica, Inc.
 * All Rights Reserved.
 *
 * 2007-11-26 - Alex - created
 */
package abackup;

import java.io.*;
import java.util.zip.*;
import java.util.List;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.ParseException;

@SuppressWarnings({ "UseOfSystemOutOrSystemErr" })
public class ABackup {
    private static final String CATALOG_FILE = "catalog.txt";

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Arguments:");
            System.out.println("<job name> - name of a job description file");
            System.out.println("<catalog file> - (optional) name of a catalog file from the last performed backup or zip file of the last performed backup");
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

        // load previous catalog if specified
        Catalog lastBackup = null;
        String lastCatalog = args.length > 1 ? args[1] : config.getCatalogFile();
        if (lastCatalog != null) {
            lastBackup = loadCatalog(lastCatalog);
        }
        if (lastBackup == null) {
            System.out.println("Will make FULL backup");
        }

        // get current files
        Catalog job = new Catalog(config);
        // find changed files
        List<String> forUpdate = job.getUpdateList(lastBackup);
        // make the archive
        try {
            makeZip(forUpdate, config, job);
        } catch (IOException e) {
            System.out.println("Backup operation FAILED: " + e.getMessage());
            return;
        }
        // copy/upload the backup everywhere
        ASync.sync(config);
    }

    static Catalog loadCatalog(String lastCatPath) {
        System.out.println("Loading catalog from: " + lastCatPath);
        Catalog lastBackup = null;
        InputStream lastCatStream = getLastCatalog(lastCatPath);
        try {
            lastBackup = new Catalog(lastCatStream);
            if (lastCatStream != null) {
                lastCatStream.close();
            }
        } catch (IOException e) {
            System.out.println("Unable to read the specified catalog. " + e.getMessage());
        } catch (ParseException e) {
            System.out.println("Unable to parse the specified catalog. " + e.getMessage());
        }
        return lastBackup;
    }

    static InputStream getLastCatalog(String catFile) {
        InputStream lastCat = null;
        if (catFile.endsWith(".zip")) {
            try {
                ZipFile zf = new ZipFile(catFile);
                ZipEntry catEntry = zf.getEntry(CATALOG_FILE);
                if (catEntry != null) {
                    lastCat = zf.getInputStream(catEntry);
                } else {
                    System.out.println(CATALOG_FILE + " not found in specified zip.");
                }
            } catch (IOException e) {
                System.out.println("Unable to read the specified zip. " + e.getMessage());
            }
        } else {
            try {
                lastCat = new FileInputStream(catFile);
            } catch (FileNotFoundException ignored) {
                System.out.println("Specified catalog file was not found.");
            }
        }
        return lastCat;
    }

    private static File makeZip(List<String> forUpdate, Config config, Catalog catalog) throws IOException {
        // build file name
        String fileName = config.getOutputFile();
        fileName += new SimpleDateFormat("yyyyMMdd-HHmm").format(new Date()) + ".zip";

        // put files in zip
        long totalBytes = 0;
        ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(fileName));
        for (String file : forUpdate) {
            if (new File(file).exists()) {
                totalBytes += putFileInZip(file, zout, config, catalog);
            }
        }

        // add catalog file to zip
        ZipEntry ze = new ZipEntry(CATALOG_FILE);
        zout.putNextEntry(ze);
        catalog.printToStream(zout);
        zout.close();

        // store catalog file separately if specified
        if (config.getCatalogFile() != null) {
            OutputStream catos = new FileOutputStream(config.getCatalogFile());
            catalog.printToStream(catos);
            catos.flush();
            catos.close();
        }
        File zipFile = new File(fileName);
        System.out.println("Total files:  " + forUpdate.size());
        System.out.println("Total bytes:  " + totalBytes);
        System.out.println("Total packed: " + zipFile.length());
        return zipFile;
    }

    private static long putFileInZip(String file, ZipOutputStream zout, Config config, Catalog catalog) throws IOException {
        CheckedInputStream in;
        try {
            in = new CheckedInputStream(new FileInputStream(file), new CRC32());
        } catch (FileNotFoundException e) {
            System.out.println("Unable to open file: " + file + "(" + e.getMessage() + ")");
            return 0;
        }
        ZipEntry ze = new ZipEntry(file);
        ze.setMethod(getCompressionMethod(file, config));
        File f = new File(file);
        ze.setTime(f.lastModified());
        ze.setSize(f.length());
        if (ze.getMethod() == ZipEntry.STORED) {
            ze.setCrc(Catalog.crc32(file));
        }
        zout.putNextEntry(ze);
        System.out.print(file);
        int fileBytes = copyStream(in, zout);
        System.out.println();
        catalog.updateCRC32(file, in.getChecksum().getValue());
        in.close();
        return fileBytes;
    }

    static int copyStream(InputStream in, OutputStream out) {
        byte[] buf = new byte[4096];
        int len;
        int fileBytes = 0;
        int i = 0;
        try {
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
                fileBytes += len;
                if ((++i & 0xFF) == 0) {
                    System.out.print(".");
                }
            }
        } catch (IOException e) {
            System.out.println(".Error reading file: " + e.getMessage() + ". File may be corrupted in archive.");
        }
        return fileBytes;
    }

    private static int getCompressionMethod(String file, Config config) {
        for (String ext : config.getCompressedExtensions()) {
            if (file.endsWith(ext)) {
                return ZipEntry.STORED;
            }
        }
        return ZipEntry.DEFLATED;
    }
}
