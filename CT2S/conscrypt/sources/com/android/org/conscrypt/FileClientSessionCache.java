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
import java.util.Set;
import java.util.TreeSet;
import javax.net.ssl.SSLSession;

public class FileClientSessionCache {
    public static final int MAX_SIZE = 12;
    static final Map<File, Impl> caches = new HashMap();

    private FileClientSessionCache() {
    }

    static class Impl implements SSLClientSessionCache {
        Map<String, File> accessOrder = newAccessOrder();
        final File directory;
        String[] initialFiles;
        int size;

        Impl(File directory) throws IOException {
            boolean exists = directory.exists();
            if (exists && !directory.isDirectory()) {
                throw new IOException(directory + " exists but is not a directory.");
            }
            if (exists) {
                this.initialFiles = directory.list();
                if (this.initialFiles == null) {
                    throw new IOException(directory + " exists but cannot list contents.");
                }
                Arrays.sort(this.initialFiles);
                this.size = this.initialFiles.length;
            } else {
                if (!directory.mkdirs()) {
                    throw new IOException("Creation of " + directory + " directory failed.");
                }
                this.size = 0;
            }
            this.directory = directory;
        }

        private static Map<String, File> newAccessOrder() {
            return new LinkedHashMap(12, 0.75f, true);
        }

        private static String fileName(String host, int port) {
            if (host == null) {
                throw new NullPointerException("host == null");
            }
            return host + "." + port;
        }

        @Override
        public synchronized byte[] getSessionData(String host, int port) {
            byte[] data;
            FileInputStream in;
            String name = fileName(host, port);
            File file = this.accessOrder.get(name);
            try {
                if (file == null) {
                    if (this.initialFiles != null && Arrays.binarySearch(this.initialFiles, name) >= 0) {
                        file = new File(this.directory, name);
                        this.accessOrder.put(name, file);
                    } else {
                        data = null;
                    }
                }
                int size = (int) file.length();
                data = new byte[size];
                new DataInputStream(in).readFully(data);
                if (in != null) {
                    try {
                        in.close();
                    } catch (RuntimeException rethrown) {
                        throw rethrown;
                    } catch (Exception e) {
                    }
                }
            } catch (Throwable th) {
                if (in != null) {
                    try {
                        in.close();
                    } catch (RuntimeException rethrown2) {
                        throw rethrown2;
                    } catch (Exception e2) {
                    }
                }
                throw th;
            }
            in = new FileInputStream(file);
            return data;
        }

        static void logReadError(String host, File file, Throwable t) {
            System.err.println("FileClientSessionCache: Error reading session data for " + host + " from " + file + ".");
            t.printStackTrace();
        }

        @Override
        public synchronized void putSessionData(SSLSession session, byte[] sessionData) {
            FileOutputStream out;
            String host = session.getPeerHost();
            if (sessionData == null) {
                throw new NullPointerException("sessionData == null");
            }
            String name = fileName(host, session.getPeerPort());
            File file = new File(this.directory, name);
            boolean existedBefore = file.exists();
            try {
                out = new FileOutputStream(file);
                if (!existedBefore) {
                    this.size++;
                    makeRoom();
                }
            } catch (FileNotFoundException e) {
                logWriteError(host, file, e);
            }
            try {
                try {
                    out.write(sessionData);
                    boolean closeSuccessful = false;
                    try {
                        try {
                            out.close();
                            closeSuccessful = true;
                            if (1 == 0 || 1 == 0) {
                                delete(file);
                            } else {
                                this.accessOrder.put(name, file);
                            }
                        } catch (IOException e2) {
                            logWriteError(host, file, e2);
                            if (1 == 0 || 0 == 0) {
                                delete(file);
                            } else {
                                this.accessOrder.put(name, file);
                            }
                        }
                    } catch (Throwable th) {
                        if (1 == 0 || !closeSuccessful) {
                            delete(file);
                        } else {
                            this.accessOrder.put(name, file);
                        }
                        throw th;
                    }
                } catch (IOException e3) {
                    logWriteError(host, file, e3);
                    boolean closeSuccessful2 = false;
                    try {
                        try {
                            out.close();
                            closeSuccessful2 = true;
                            if (0 == 0 || 1 == 0) {
                                delete(file);
                            } else {
                                this.accessOrder.put(name, file);
                            }
                        } catch (IOException e4) {
                            logWriteError(host, file, e4);
                            if (0 == 0 || 0 == 0) {
                                delete(file);
                            } else {
                                this.accessOrder.put(name, file);
                            }
                        }
                    } catch (Throwable th2) {
                        if (0 == 0 || !closeSuccessful2) {
                            delete(file);
                        } else {
                            this.accessOrder.put(name, file);
                        }
                        throw th2;
                    }
                }
            } catch (Throwable th3) {
                boolean closeSuccessful3 = false;
                try {
                    try {
                        out.close();
                        closeSuccessful3 = true;
                        if (0 == 0 || 1 == 0) {
                            delete(file);
                        } else {
                            this.accessOrder.put(name, file);
                        }
                    } catch (Throwable th4) {
                        if (0 == 0 || !closeSuccessful3) {
                            delete(file);
                        } else {
                            this.accessOrder.put(name, file);
                        }
                        throw th4;
                    }
                } catch (IOException e5) {
                    logWriteError(host, file, e5);
                    if (0 == 0 || 0 == 0) {
                        delete(file);
                    } else {
                        this.accessOrder.put(name, file);
                    }
                }
                throw th3;
            }
        }

        private void makeRoom() {
            if (this.size > 12) {
                indexFiles();
                int removals = this.size - 12;
                Iterator<File> i = this.accessOrder.values().iterator();
                do {
                    delete(i.next());
                    i.remove();
                    removals--;
                } while (removals > 0);
            }
        }

        private void indexFiles() {
            String[] initialFiles = this.initialFiles;
            if (initialFiles != null) {
                this.initialFiles = null;
                Set<CacheFile> diskOnly = new TreeSet<>();
                for (String name : initialFiles) {
                    if (!this.accessOrder.containsKey(name)) {
                        diskOnly.add(new CacheFile(this.directory, name));
                    }
                }
                if (!diskOnly.isEmpty()) {
                    Map<String, File> newOrder = newAccessOrder();
                    for (CacheFile cacheFile : diskOnly) {
                        newOrder.put(cacheFile.name, cacheFile);
                    }
                    newOrder.putAll(this.accessOrder);
                    this.accessOrder = newOrder;
                }
            }
        }

        private void delete(File file) {
            if (!file.delete()) {
                new IOException("FileClientSessionCache: Failed to delete " + file + ".").printStackTrace();
            }
            this.size--;
        }

        static void logWriteError(String host, File file, Throwable t) {
            System.err.println("FileClientSessionCache: Error writing session data for " + host + " to " + file + ".");
            t.printStackTrace();
        }
    }

    public static synchronized SSLClientSessionCache usingDirectory(File directory) throws IOException {
        Impl cache;
        cache = caches.get(directory);
        if (cache == null) {
            cache = new Impl(directory);
            caches.put(directory, cache);
        }
        return cache;
    }

    static synchronized void reset() {
        caches.clear();
    }

    static class CacheFile extends File {
        long lastModified;
        final String name;

        CacheFile(File dir, String name) {
            super(dir, name);
            this.lastModified = -1L;
            this.name = name;
        }

        @Override
        public long lastModified() {
            long lastModified = this.lastModified;
            if (lastModified == -1) {
                long lastModified2 = super.lastModified();
                this.lastModified = lastModified2;
                return lastModified2;
            }
            return lastModified;
        }

        @Override
        public int compareTo(File another) {
            long result = lastModified() - another.lastModified();
            if (result == 0) {
                return super.compareTo(another);
            }
            return result < 0 ? -1 : 1;
        }
    }
}
