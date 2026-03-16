package com.android.providers.downloads;

import android.content.Context;
import android.content.pm.IPackageDataObserver;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Process;
import android.system.ErrnoException;
import android.system.Os;
import android.system.StructStat;
import android.system.StructStatVfs;
import android.util.Slog;
import com.google.android.collect.Lists;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class StorageUtils {
    static boolean sForceFullEviction = false;

    public static void ensureAvailableSpace(Context context, FileDescriptor fd, long bytes) throws IOException, StopRequestException {
        if (getAvailableBytes(fd) < bytes) {
            try {
                long dev = Os.fstat(fd).st_dev;
                long dataDev = getDeviceId(Environment.getDataDirectory());
                long cacheDev = getDeviceId(Environment.getDownloadCacheDirectory());
                long externalDev = getDeviceId(Environment.getExternalStorageDirectory());
                if (dev == dataDev || (dev == externalDev && Environment.isExternalStorageEmulated())) {
                    PackageManager pm = context.getPackageManager();
                    ObserverLatch observer = new ObserverLatch();
                    pm.freeStorageAndNotify(sForceFullEviction ? Long.MAX_VALUE : bytes, observer);
                    try {
                        if (!observer.latch.await(30L, TimeUnit.SECONDS)) {
                            throw new IOException("Timeout while freeing disk space");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                } else if (dev == cacheDev) {
                    freeCacheStorage(bytes);
                }
                long availBytes = getAvailableBytes(fd);
                if (availBytes < bytes) {
                    throw new StopRequestException(198, "Not enough free space; " + bytes + " requested, " + availBytes + " available");
                }
            } catch (ErrnoException e2) {
                throw e2.rethrowAsIOException();
            }
        }
    }

    private static void freeCacheStorage(long bytes) {
        List<ConcreteFile> files = listFilesRecursive(Environment.getDownloadCacheDirectory(), "partial_downloads", Process.myUid());
        Slog.d("DownloadManager", "Found " + files.size() + " downloads on cache");
        Collections.sort(files, new Comparator<ConcreteFile>() {
            @Override
            public int compare(ConcreteFile lhs, ConcreteFile rhs) {
                return (int) (lhs.file.lastModified() - rhs.file.lastModified());
            }
        });
        long now = System.currentTimeMillis();
        for (ConcreteFile file : files) {
            if (bytes <= 0) {
                return;
            }
            if (now - file.file.lastModified() < 86400000) {
                Slog.d("DownloadManager", "Skipping recently modified " + file.file);
            } else {
                long len = file.file.length();
                Slog.d("DownloadManager", "Deleting " + file.file + " to reclaim " + len);
                bytes -= len;
                file.file.delete();
            }
        }
    }

    private static long getAvailableBytes(FileDescriptor fd) throws IOException {
        try {
            StructStatVfs stat = Os.fstatvfs(fd);
            return (stat.f_bavail * stat.f_bsize) - 33554432;
        } catch (ErrnoException e) {
            throw e.rethrowAsIOException();
        }
    }

    private static long getDeviceId(File file) {
        try {
            return Os.stat(file.getAbsolutePath()).st_dev;
        } catch (ErrnoException e) {
            return -1L;
        }
    }

    static List<ConcreteFile> listFilesRecursive(File startDir, String exclude, int uid) {
        File[] children;
        ArrayList<ConcreteFile> files = Lists.newArrayList();
        LinkedList<File> dirs = new LinkedList<>();
        dirs.add(startDir);
        while (!dirs.isEmpty()) {
            File dir = dirs.removeFirst();
            if (!Objects.equals(dir.getName(), exclude) && (children = dir.listFiles()) != null) {
                for (File child : children) {
                    if (child.isDirectory()) {
                        dirs.add(child);
                    } else if (child.isFile()) {
                        try {
                            ConcreteFile file = new ConcreteFile(child);
                            if (uid == -1 || file.stat.st_uid == uid) {
                                files.add(file);
                            }
                        } catch (ErrnoException e) {
                        }
                    }
                }
            }
        }
        return files;
    }

    static class ConcreteFile {
        public final File file;
        public final StructStat stat;

        public ConcreteFile(File file) throws ErrnoException {
            this.file = file;
            this.stat = Os.lstat(file.getAbsolutePath());
        }

        public int hashCode() {
            int result = ((int) (this.stat.st_dev ^ (this.stat.st_dev >>> 32))) + 31;
            return (result * 31) + ((int) (this.stat.st_ino ^ (this.stat.st_ino >>> 32)));
        }

        public boolean equals(Object o) {
            if (!(o instanceof ConcreteFile)) {
                return false;
            }
            ConcreteFile f = (ConcreteFile) o;
            return f.stat.st_dev == this.stat.st_dev && f.stat.st_ino == this.stat.st_ino;
        }
    }

    static class ObserverLatch extends IPackageDataObserver.Stub {
        public final CountDownLatch latch = new CountDownLatch(1);

        ObserverLatch() {
        }

        public void onRemoveCompleted(String packageName, boolean succeeded) {
            this.latch.countDown();
        }
    }
}
