/*
 * Cognity
 * Copyright (c) 2008 FinAnalytica, Inc.
 * All Rights Reserved.
 *
 * 2009-8-14 - alex - created
 */
package abackup;

import java.nio.channels.FileChannel;
import java.io.*;
import java.net.Socket;

public class FileUtils {
    private FileUtils() {
    }

    public static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        FileChannel source = null;
        FileChannel destination = null;


        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        }
        finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }

}
