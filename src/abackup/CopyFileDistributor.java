/*
 * Cognity
 * Copyright (c) 2011 FinAnalytica
 */
package abackup;

import java.io.File;
import java.io.IOException;

class CopyFileDistributor implements FileDistributor {
    private File destDir;

    CopyFileDistributor(File destDir) {
        this.destDir = destDir;
    }

    public void distibuteFile(File source) throws IOException {
        FileUtils.copyFile(source, new File(destDir, source.getName()));
    }

    public String[] getFiles() throws IOException {
        String[] files = destDir.list();
        return files == null ? new String[0] : files;
    }
}
