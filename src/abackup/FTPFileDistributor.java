package abackup;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.*;
import java.net.*;

class FTPFileDistributor implements FileDistributor {
    private URL url;
    private FTPClient client;

    FTPFileDistributor(URL url) {
        this.url = url;
    }

    public void distibuteFile(final File source) throws IOException {
        executeThreaded(getClient(), new Runnable() {
            public void run() {
                try {
                    FileInputStream in = new FileInputStream(source);
                    try {
                        client.storeFile(source.getName(), in);
                    } finally {
                        in.close();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }, 3600);
    }

    public String[] getFiles() throws IOException {
        final String[][] result = new String[1][];
        executeThreaded(getClient(), new Runnable() {
            public void run() {
                try {
                    String path = URLDecoder.decode(url.getPath());
                    result[0] = client.listNames(path);
                    if (result[0] != null) {
                        for (int i = 0; i < result[0].length; i++) {
                            if (result[0][i].startsWith(path)) {
                                result[0][i] = result[0][i].substring(path.length());
                            }
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }, 60);
        return result[0] == null ? new String[0] : result[0];
    }

    private FTPClient getClient() throws IOException {
        if (client == null) {
            this.client = createClient();
        }
        return client;
    }

    private void executeThreaded(FTPClient client, Runnable runnable, int timeoutSec) throws IOException {
        Thread thread = new Thread(runnable); 
        EH eh = new EH();
        thread.setUncaughtExceptionHandler(eh);
        thread.start();
        try {
            thread.join(timeoutSec * 1000);
        } catch (InterruptedException ignored) {
            throw new IOException("Failed to login to: host");
        }
        eh.throwException();
        if (thread.getState() != Thread.State.TERMINATED) {
            client.disconnect();
            throw new IOException("Failed to login to: host");
        }
    }

    private FTPClient createClient() throws IOException {
        final FTPClient client = new FTPClientWithExposedSocket();
        executeThreaded(client, new Runnable() {
            public void run() {
                try {
                    String host = URLDecoder.decode(url.getHost());
                    if (url.getPort() != -1) {
                        client.connect(host, url.getPort());
                    } else {
                        client.connect(host);
                    }
                    client.setFileType(FTP.BINARY_FILE_TYPE);
                    client.setRemoteVerificationEnabled(false);
                    client.enterLocalPassiveMode();
                    boolean loggedIn = false;
                    if (url.getUserInfo() != null) {
                        String[] userInfo = url.getUserInfo().split(":");
                        loggedIn = client.login(URLDecoder.decode(userInfo[0]), URLDecoder.decode(userInfo[1]));
                    }
                    if (!loggedIn) {
                        loggedIn = client.login("anonymous", "");
                    }
                    if (!loggedIn) {
                        throw new IOException("Failed to login to: " + host);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }, 30);
        return client;
    }

    private static class EH implements Thread.UncaughtExceptionHandler {
        public Throwable e;
        public void uncaughtException(Thread t, Throwable e) {
            this.e = e;
        }
        public void throwException() throws IOException {
            if (e != null) {
                if (RuntimeException.class.equals(e.getClass())) {
                    if (IOException.class.isAssignableFrom(e.getCause().getClass())) {
                        throw (IOException) e.getCause();
                    }
                } else if (IOException.class.isAssignableFrom(e.getClass())) {
                    throw (IOException) e;
                } else {
                    throw new IOException(e);
                }
            }
        }

    }

    public static class FTPClientWithExposedSocket extends FTPClient {
        protected Socket lastOpenDataSocket;
        @Override
        protected Socket _openDataConnection_(int command, String arg) throws IOException {
             lastOpenDataSocket = super._openDataConnection_(command, arg);
             return lastOpenDataSocket;
        }
        @Override
        public void disconnect() throws IOException {
            try{
                if (lastOpenDataSocket != null) {
                    lastOpenDataSocket.close();
                }
            } catch (IOException ignored) {
                // do nothing
            }
            super.disconnect();
        }
    }

}
