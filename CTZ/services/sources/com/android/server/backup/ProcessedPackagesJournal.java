package com.android.server.backup;

import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashSet;
import java.util.Set;

final class ProcessedPackagesJournal {
    private static final boolean DEBUG = true;
    private static final String JOURNAL_FILE_NAME = "processed";
    private static final String TAG = "ProcessedPackagesJournal";

    @GuardedBy("mProcessedPackages")
    private final Set<String> mProcessedPackages = new HashSet();
    private final File mStateDirectory;

    ProcessedPackagesJournal(File file) {
        this.mStateDirectory = file;
    }

    void init() {
        synchronized (this.mProcessedPackages) {
            loadFromDisk();
        }
    }

    boolean hasBeenProcessed(String str) {
        boolean zContains;
        synchronized (this.mProcessedPackages) {
            zContains = this.mProcessedPackages.contains(str);
        }
        return zContains;
    }

    void addPackage(String str) {
        RandomAccessFile randomAccessFile;
        Throwable th;
        synchronized (this.mProcessedPackages) {
            if (this.mProcessedPackages.add(str)) {
                File file = new File(this.mStateDirectory, JOURNAL_FILE_NAME);
                try {
                    randomAccessFile = new RandomAccessFile(file, "rws");
                    th = null;
                } catch (IOException e) {
                    Slog.e(TAG, "Can't log backup of " + str + " to " + file);
                }
                try {
                    randomAccessFile.seek(randomAccessFile.length());
                    randomAccessFile.writeUTF(str);
                } finally {
                    $closeResource(th, randomAccessFile);
                }
            }
        }
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    Set<String> getPackagesCopy() {
        HashSet hashSet;
        synchronized (this.mProcessedPackages) {
            hashSet = new HashSet(this.mProcessedPackages);
        }
        return hashSet;
    }

    void reset() {
        synchronized (this.mProcessedPackages) {
            this.mProcessedPackages.clear();
            new File(this.mStateDirectory, JOURNAL_FILE_NAME).delete();
        }
    }

    private void loadFromDisk() throws Exception {
        File file = new File(this.mStateDirectory, JOURNAL_FILE_NAME);
        if (!file.exists()) {
            return;
        }
        try {
            DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
            Throwable th = null;
            while (true) {
                try {
                    String utf = dataInputStream.readUTF();
                    Slog.v(TAG, "   + " + utf);
                    this.mProcessedPackages.add(utf);
                } catch (Throwable th2) {
                    $closeResource(th, dataInputStream);
                    throw th2;
                }
            }
        } catch (EOFException e) {
        } catch (IOException e2) {
            Slog.e(TAG, "Error reading processed packages journal", e2);
        }
    }
}
