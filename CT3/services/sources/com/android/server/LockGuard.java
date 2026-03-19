package com.android.server;

import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class LockGuard {
    private static final String TAG = "LockGuard";
    private static ArrayMap<Object, LockInfo> sKnown = new ArrayMap<>(0, true);

    private static class LockInfo {
        public ArraySet<Object> children;
        public String label;

        LockInfo(LockInfo lockInfo) {
            this();
        }

        private LockInfo() {
            this.children = new ArraySet<>(0, true);
        }
    }

    private static LockInfo findOrCreateLockInfo(Object lock) {
        LockInfo lockInfo = null;
        LockInfo info = sKnown.get(lock);
        if (info == null) {
            LockInfo info2 = new LockInfo(lockInfo);
            info2.label = "0x" + Integer.toHexString(System.identityHashCode(lock)) + " [" + new Throwable().getStackTrace()[2].toString() + "]";
            sKnown.put(lock, info2);
            return info2;
        }
        return info;
    }

    public static Object guard(Object lock) {
        if (lock == null || Thread.holdsLock(lock)) {
            return lock;
        }
        boolean triggered = false;
        LockInfo info = findOrCreateLockInfo(lock);
        for (int i = 0; i < info.children.size(); i++) {
            Object child = info.children.valueAt(i);
            if (child != null && Thread.holdsLock(child)) {
                Slog.w(TAG, "Calling thread " + Thread.currentThread().getName() + " is holding " + lockToString(child) + " while trying to acquire " + lockToString(lock), new Throwable());
                triggered = true;
            }
        }
        if (!triggered) {
            for (int i2 = 0; i2 < sKnown.size(); i2++) {
                Object test = sKnown.keyAt(i2);
                if (test != null && test != lock && Thread.holdsLock(test)) {
                    sKnown.valueAt(i2).children.add(lock);
                }
            }
        }
        return lock;
    }

    public static void installLock(Object lock, String label) {
        LockInfo info = findOrCreateLockInfo(lock);
        info.label = label;
    }

    private static String lockToString(Object lock) {
        LockInfo info = sKnown.get(lock);
        if (info != null) {
            return info.label;
        }
        return "0x" + Integer.toHexString(System.identityHashCode(lock));
    }

    public static void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        for (int i = 0; i < sKnown.size(); i++) {
            Object lock = sKnown.keyAt(i);
            LockInfo info = sKnown.valueAt(i);
            pw.println("Lock " + lockToString(lock) + ":");
            for (int j = 0; j < info.children.size(); j++) {
                pw.println("  Child " + lockToString(info.children.valueAt(j)));
            }
            pw.println();
        }
    }
}
