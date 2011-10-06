/*
 * Cognity
 * Copyright (c) 2011 FinAnalytica
 */
package abackup;

import java.io.File;
import java.io.IOException;

public interface FileDistributor {
    void distibuteFile(File source) throws IOException;
    String[] getFiles() throws IOException;
}
