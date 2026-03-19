package com.android.org.conscrypt;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLSession;

public final class FileClientSessionCache {
    public static final int MAX_SIZE = 12;
    private static final Logger logger = Logger.getLogger(FileClientSessionCache.class.getName());
    static final Map<File, Impl> caches = new HashMap();

    private FileClientSessionCache() {
    }

    static class Impl implements SSLClientSessionCache {
        Map<String, File> accessOrder = newAccessOrder();
        final File directory;
        String[] initialFiles;
        int size;

        Impl(File file) throws IOException {
            boolean zExists = file.exists();
            if (zExists && !file.isDirectory()) {
                throw new IOException(file + " exists but is not a directory.");
            }
            if (zExists) {
                this.initialFiles = file.list();
                if (this.initialFiles == null) {
                    throw new IOException(file + " exists but cannot list contents.");
                }
                Arrays.sort(this.initialFiles);
                this.size = this.initialFiles.length;
            } else {
                if (!file.mkdirs()) {
                    throw new IOException("Creation of " + file + " directory failed.");
                }
                this.size = 0;
            }
            this.directory = file;
        }

        private static Map<String, File> newAccessOrder() {
            return new LinkedHashMap(12, 0.75f, true);
        }

        private static String fileName(String str, int i) {
            if (str == null) {
                throw new NullPointerException("host == null");
            }
            return str + "." + i;
        }

        @Override
        public synchronized byte[] getSessionData(String str, int i) {
            ?? FileName = fileName(str, i);
            File file = this.accessOrder.get(FileName);
            if (file == null) {
                if (this.initialFiles == null) {
                    return null;
                }
                if (Arrays.binarySearch(this.initialFiles, (Object) FileName) < 0) {
                    return null;
                }
                file = new File(this.directory, (String) FileName);
                this.accessOrder.put((String) FileName, file);
            }
            try {
                try {
                    FileName = new FileInputStream(file);
                    try {
                        byte[] bArr = new byte[(int) file.length()];
                        new DataInputStream(FileName).readFully(bArr);
                        return bArr;
                    } catch (IOException e) {
                        logReadError(str, file, e);
                        try {
                            FileName.close();
                        } catch (Exception e2) {
                        }
                        return null;
                    }
                } catch (FileNotFoundException e3) {
                    logReadError(str, file, e3);
                    return null;
                }
            } finally {
                try {
                    FileName.close();
                } catch (Exception e4) {
                }
            }
        }

        static void logReadError(String str, File file, Throwable th) {
            FileClientSessionCache.logger.log(Level.WARNING, "FileClientSessionCache: Error reading session data for " + str + " from " + file + ".", th);
        }

        @Override
        public synchronized void putSessionData(SSLSession sSLSession, byte[] bArr) {
            String peerHost = sSLSession.getPeerHost();
            if (bArr == null) {
                throw new NullPointerException("sessionData == null");
            }
            String strFileName = fileName(peerHost, sSLSession.getPeerPort());
            File file = new File(this.directory, strFileName);
            boolean zExists = file.exists();
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                if (!zExists) {
                    this.size++;
                    makeRoom();
                }
                try {
                    try {
                        fileOutputStream.write(bArr);
                        try {
                            try {
                                fileOutputStream.close();
                                this.accessOrder.put(strFileName, file);
                            } catch (Throwable th) {
                                throw th;
                            }
                        } catch (IOException e) {
                            logWriteError(peerHost, file, e);
                        }
                    } catch (Throwable th2) {
                        try {
                            try {
                                fileOutputStream.close();
                            } catch (Throwable th3) {
                                delete(file);
                                throw th3;
                            }
                        } catch (IOException e2) {
                            logWriteError(peerHost, file, e2);
                        }
                        throw th2;
                    }
                } catch (IOException e3) {
                    logWriteError(peerHost, file, e3);
                    try {
                        try {
                            fileOutputStream.close();
                        } catch (IOException e4) {
                            logWriteError(peerHost, file, e4);
                        }
                    } finally {
                        delete(file);
                    }
                }
            } catch (FileNotFoundException e5) {
                logWriteError(peerHost, file, e5);
            }
        }

        private void makeRoom() {
            if (this.size <= 12) {
                return;
            }
            indexFiles();
            int i = this.size - 12;
            Iterator<File> it = this.accessOrder.values().iterator();
            do {
                delete(it.next());
                it.remove();
                i--;
            } while (i > 0);
        }

        private void indexFiles() {
            String[] strArr = this.initialFiles;
            if (strArr != null) {
                this.initialFiles = null;
                TreeSet<CacheFile> treeSet = new TreeSet();
                for (String str : strArr) {
                    if (!this.accessOrder.containsKey(str)) {
                        treeSet.add(new CacheFile(this.directory, str));
                    }
                }
                if (!treeSet.isEmpty()) {
                    Map<String, File> mapNewAccessOrder = newAccessOrder();
                    for (CacheFile cacheFile : treeSet) {
                        mapNewAccessOrder.put(cacheFile.name, cacheFile);
                    }
                    mapNewAccessOrder.putAll(this.accessOrder);
                    this.accessOrder = mapNewAccessOrder;
                }
            }
        }

        private void delete(File file) {
            if (!file.delete()) {
                IOException iOException = new IOException("FileClientSessionCache: Failed to delete " + file + ".");
                FileClientSessionCache.logger.log(Level.WARNING, iOException.getMessage(), (Throwable) iOException);
            }
            this.size--;
        }

        static void logWriteError(String str, File file, Throwable th) {
            FileClientSessionCache.logger.log(Level.WARNING, "FileClientSessionCache: Error writing session data for " + str + " to " + file + ".", th);
        }
    }

    public static synchronized SSLClientSessionCache usingDirectory(File file) throws IOException {
        Impl impl;
        impl = caches.get(file);
        if (impl == null) {
            impl = new Impl(file);
            caches.put(file, impl);
        }
        return impl;
    }

    static synchronized void reset() {
        caches.clear();
    }

    static class CacheFile extends File {
        long lastModified;
        final String name;

        CacheFile(File file, String str) {
            super(file, str);
            this.lastModified = -1L;
            this.name = str;
        }

        @Override
        public long lastModified() {
            long j = this.lastModified;
            if (j == -1) {
                long jLastModified = super.lastModified();
                this.lastModified = jLastModified;
                return jLastModified;
            }
            return j;
        }

        @Override
        public int compareTo(File file) {
            long jLastModified = lastModified() - file.lastModified();
            if (jLastModified == 0) {
                return super.compareTo(file);
            }
            return jLastModified < 0 ? -1 : 1;
        }
    }
}
