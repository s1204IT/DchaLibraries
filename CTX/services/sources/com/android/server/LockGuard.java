package com.android.server;

import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class LockGuard {
    public static final int INDEX_ACTIVITY = 6;
    public static final int INDEX_APP_OPS = 0;
    public static final int INDEX_DPMS = 7;
    public static final int INDEX_PACKAGES = 3;
    public static final int INDEX_POWER = 1;
    public static final int INDEX_STORAGE = 4;
    public static final int INDEX_USER = 2;
    public static final int INDEX_WINDOW = 5;
    private static final String TAG = "LockGuard";
    private static Object[] sKnownFixed = new Object[8];
    private static ArrayMap<Object, LockInfo> sKnown = new ArrayMap<>(0, true);

    private static class LockInfo {
        public ArraySet<Object> children;
        public boolean doWtf;
        public String label;

        private LockInfo() {
            this.children = new ArraySet<>(0, true);
        }
    }

    private static LockInfo findOrCreateLockInfo(Object obj) {
        LockInfo lockInfo = sKnown.get(obj);
        if (lockInfo == null) {
            LockInfo lockInfo2 = new LockInfo();
            lockInfo2.label = "0x" + Integer.toHexString(System.identityHashCode(obj)) + " [" + new Throwable().getStackTrace()[2].toString() + "]";
            sKnown.put(obj, lockInfo2);
            return lockInfo2;
        }
        return lockInfo;
    }

    public static Object guard(Object obj) {
        if (obj == null || Thread.holdsLock(obj)) {
            return obj;
        }
        LockInfo lockInfoFindOrCreateLockInfo = findOrCreateLockInfo(obj);
        boolean z = false;
        for (int i = 0; i < lockInfoFindOrCreateLockInfo.children.size(); i++) {
            Object objValueAt = lockInfoFindOrCreateLockInfo.children.valueAt(i);
            if (objValueAt != null && Thread.holdsLock(objValueAt)) {
                doLog(obj, "Calling thread " + Thread.currentThread().getName() + " is holding " + lockToString(objValueAt) + " while trying to acquire " + lockToString(obj));
                z = true;
            }
        }
        if (!z) {
            for (int i2 = 0; i2 < sKnown.size(); i2++) {
                Object objKeyAt = sKnown.keyAt(i2);
                if (objKeyAt != null && objKeyAt != obj && Thread.holdsLock(objKeyAt)) {
                    sKnown.valueAt(i2).children.add(obj);
                }
            }
        }
        return obj;
    }

    public static void guard(int i) {
        for (int i2 = 0; i2 < i; i2++) {
            Object obj = sKnownFixed[i2];
            if (obj != null && Thread.holdsLock(obj)) {
                doLog(sKnownFixed[i], "Calling thread " + Thread.currentThread().getName() + " is holding " + lockToString(i2) + " while trying to acquire " + lockToString(i));
            }
        }
    }

    private static void doLog(Object obj, String str) {
        if (obj != null && findOrCreateLockInfo(obj).doWtf) {
            final RuntimeException runtimeException = new RuntimeException(str);
            new Thread(new Runnable() {
                @Override
                public final void run() {
                    Slog.wtf(LockGuard.TAG, runtimeException);
                }
            }).start();
        } else {
            Slog.w(TAG, str, new Throwable());
        }
    }

    public static Object installLock(Object obj, String str) {
        findOrCreateLockInfo(obj).label = str;
        return obj;
    }

    public static Object installLock(Object obj, int i) {
        return installLock(obj, i, false);
    }

    public static Object installLock(Object obj, int i, boolean z) {
        sKnownFixed[i] = obj;
        LockInfo lockInfoFindOrCreateLockInfo = findOrCreateLockInfo(obj);
        lockInfoFindOrCreateLockInfo.doWtf = z;
        lockInfoFindOrCreateLockInfo.label = "Lock-" + lockToString(i);
        return obj;
    }

    public static Object installNewLock(int i) {
        return installNewLock(i, false);
    }

    public static Object installNewLock(int i, boolean z) {
        Object obj = new Object();
        installLock(obj, i, z);
        return obj;
    }

    private static String lockToString(Object obj) {
        LockInfo lockInfo = sKnown.get(obj);
        if (lockInfo != null && !TextUtils.isEmpty(lockInfo.label)) {
            return lockInfo.label;
        }
        return "0x" + Integer.toHexString(System.identityHashCode(obj));
    }

    private static String lockToString(int i) {
        switch (i) {
            case 0:
                return "APP_OPS";
            case 1:
                return "POWER";
            case 2:
                return "USER";
            case 3:
                return "PACKAGES";
            case 4:
                return "STORAGE";
            case 5:
                return "WINDOW";
            case 6:
                return "ACTIVITY";
            case 7:
                return "DPMS";
            default:
                return Integer.toString(i);
        }
    }

    public static void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        for (int i = 0; i < sKnown.size(); i++) {
            Object objKeyAt = sKnown.keyAt(i);
            LockInfo lockInfoValueAt = sKnown.valueAt(i);
            printWriter.println("Lock " + lockToString(objKeyAt) + ":");
            for (int i2 = 0; i2 < lockInfoValueAt.children.size(); i2++) {
                printWriter.println("  Child " + lockToString(lockInfoValueAt.children.valueAt(i2)));
            }
            printWriter.println();
        }
    }
}
