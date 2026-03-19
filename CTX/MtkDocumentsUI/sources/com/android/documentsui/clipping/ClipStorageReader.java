package com.android.documentsui.clipping;

import android.net.Uri;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

class ClipStorageReader implements Closeable, Iterable<Uri> {
    static final boolean $assertionsDisabled = false;
    private static final Map<String, FileLockEntry> sLocks = new HashMap();
    private final String mCanonicalPath;
    private final Scanner mScanner;

    ClipStorageReader(File file) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(file);
        this.mScanner = new Scanner(fileInputStream);
        this.mCanonicalPath = file.getCanonicalPath();
        synchronized (sLocks) {
            if (sLocks.containsKey(this.mCanonicalPath)) {
                FileLockEntry.access$008(sLocks.get(this.mCanonicalPath));
            } else {
                sLocks.put(this.mCanonicalPath, new FileLockEntry(1, fileInputStream.getChannel().lock(0L, Long.MAX_VALUE, true), this.mScanner));
            }
        }
    }

    @Override
    public java.util.Iterator<Uri> iterator2() {
        return new Iterator(this.mScanner);
    }

    @Override
    public void close() throws IOException {
        FileLockEntry fileLockEntry;
        synchronized (sLocks) {
            fileLockEntry = sLocks.get(this.mCanonicalPath);
            if (FileLockEntry.access$006(fileLockEntry) == 0) {
                fileLockEntry.mLock.release();
                fileLockEntry.mScanner.close();
                sLocks.remove(this.mCanonicalPath);
            }
        }
        if (this.mScanner != fileLockEntry.mScanner) {
            this.mScanner.close();
        }
    }

    private static final class Iterator implements java.util.Iterator {
        private final Scanner mScanner;

        private Iterator(Scanner scanner) {
            this.mScanner = scanner;
        }

        @Override
        public boolean hasNext() {
            return this.mScanner.hasNextLine();
        }

        @Override
        public Uri next() {
            return Uri.parse(this.mScanner.nextLine());
        }
    }

    private static final class FileLockEntry {
        private int mCount;
        private final FileLock mLock;
        private final Scanner mScanner;

        static int access$006(FileLockEntry fileLockEntry) {
            int i = fileLockEntry.mCount - 1;
            fileLockEntry.mCount = i;
            return i;
        }

        static int access$008(FileLockEntry fileLockEntry) {
            int i = fileLockEntry.mCount;
            fileLockEntry.mCount = i + 1;
            return i;
        }

        private FileLockEntry(int i, FileLock fileLock, Scanner scanner) {
            this.mCount = i;
            this.mLock = fileLock;
            this.mScanner = scanner;
        }
    }
}
