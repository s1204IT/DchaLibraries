package com.android.server.wifi;

import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.WorkSource;
import android.util.Slog;
import com.android.internal.app.IBatteryStats;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class WifiLockManager {
    private static final String TAG = "WifiLockManager";
    private final IBatteryStats mBatteryStats;
    private final Context mContext;
    private int mFullHighPerfLocksAcquired;
    private int mFullHighPerfLocksReleased;
    private int mFullLocksAcquired;
    private int mFullLocksReleased;
    private int mScanLocksAcquired;
    private int mScanLocksReleased;
    private boolean mVerboseLoggingEnabled = false;
    private final List<WifiLock> mWifiLocks = new ArrayList();

    WifiLockManager(Context context, IBatteryStats iBatteryStats) {
        this.mContext = context;
        this.mBatteryStats = iBatteryStats;
    }

    public boolean acquireWifiLock(int i, String str, IBinder iBinder, WorkSource workSource) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.WAKE_LOCK", null);
        if (!isValidLockMode(i)) {
            throw new IllegalArgumentException("lockMode =" + i);
        }
        if (workSource == null || workSource.isEmpty()) {
            workSource = new WorkSource(Binder.getCallingUid());
        } else {
            this.mContext.enforceCallingOrSelfPermission("android.permission.UPDATE_DEVICE_STATS", null);
        }
        return addLock(new WifiLock(i, str, iBinder, workSource));
    }

    public boolean releaseWifiLock(IBinder iBinder) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.WAKE_LOCK", null);
        return releaseLock(iBinder);
    }

    public synchronized int getStrongestLockMode() {
        if (this.mWifiLocks.isEmpty()) {
            return 0;
        }
        if (this.mFullHighPerfLocksAcquired > this.mFullHighPerfLocksReleased) {
            return 3;
        }
        if (this.mFullLocksAcquired > this.mFullLocksReleased) {
            return 1;
        }
        return 2;
    }

    public synchronized WorkSource createMergedWorkSource() {
        WorkSource workSource;
        workSource = new WorkSource();
        Iterator<WifiLock> it = this.mWifiLocks.iterator();
        while (it.hasNext()) {
            workSource.add(it.next().getWorkSource());
        }
        return workSource;
    }

    public synchronized void updateWifiLockWorkSource(IBinder iBinder, WorkSource workSource) {
        WorkSource workSource2;
        this.mContext.enforceCallingOrSelfPermission("android.permission.UPDATE_DEVICE_STATS", null);
        WifiLock wifiLockFindLockByBinder = findLockByBinder(iBinder);
        if (wifiLockFindLockByBinder == null) {
            throw new IllegalArgumentException("Wifi lock not active");
        }
        if (workSource == null || workSource.isEmpty()) {
            workSource2 = new WorkSource(Binder.getCallingUid());
        } else {
            workSource2 = new WorkSource(workSource);
        }
        if (this.mVerboseLoggingEnabled) {
            Slog.d(TAG, "updateWifiLockWakeSource: " + wifiLockFindLockByBinder + ", newWorkSource=" + workSource2);
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            try {
                this.mBatteryStats.noteFullWifiLockAcquiredFromSource(workSource2);
                this.mBatteryStats.noteFullWifiLockReleasedFromSource(wifiLockFindLockByBinder.mWorkSource);
                wifiLockFindLockByBinder.mWorkSource = workSource2;
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            } catch (RemoteException e) {
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private static boolean isValidLockMode(int i) {
        if (i == 1 || i == 2 || i == 3) {
            return true;
        }
        return false;
    }

    private synchronized boolean addLock(WifiLock wifiLock) {
        if (this.mVerboseLoggingEnabled) {
            Slog.d(TAG, "addLock: " + wifiLock);
        }
        if (findLockByBinder(wifiLock.getBinder()) != null) {
            if (this.mVerboseLoggingEnabled) {
                Slog.d(TAG, "attempted to add a lock when already holding one");
            }
            return false;
        }
        this.mWifiLocks.add(wifiLock);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        boolean z = true;
        try {
            this.mBatteryStats.noteFullWifiLockAcquiredFromSource(wifiLock.mWorkSource);
            switch (wifiLock.mMode) {
                case 1:
                    this.mFullLocksAcquired++;
                    break;
                case 2:
                    this.mScanLocksAcquired++;
                    break;
                case 3:
                    this.mFullHighPerfLocksAcquired++;
                    break;
            }
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        } catch (RemoteException e) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            z = false;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
        return z;
    }

    private synchronized WifiLock removeLock(IBinder iBinder) {
        WifiLock wifiLockFindLockByBinder;
        wifiLockFindLockByBinder = findLockByBinder(iBinder);
        if (wifiLockFindLockByBinder != null) {
            this.mWifiLocks.remove(wifiLockFindLockByBinder);
            wifiLockFindLockByBinder.unlinkDeathRecipient();
        }
        return wifiLockFindLockByBinder;
    }

    private synchronized boolean releaseLock(IBinder iBinder) {
        WifiLock wifiLockRemoveLock = removeLock(iBinder);
        if (wifiLockRemoveLock == null) {
            return false;
        }
        if (this.mVerboseLoggingEnabled) {
            Slog.d(TAG, "releaseLock: " + wifiLockRemoveLock);
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mBatteryStats.noteFullWifiLockReleasedFromSource(wifiLockRemoveLock.mWorkSource);
            switch (wifiLockRemoveLock.mMode) {
                case 1:
                    this.mFullLocksReleased++;
                    break;
                case 2:
                    this.mScanLocksReleased++;
                    break;
                case 3:
                    this.mFullHighPerfLocksReleased++;
                    break;
            }
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        } catch (RemoteException e) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
        return true;
    }

    private synchronized WifiLock findLockByBinder(IBinder iBinder) {
        for (WifiLock wifiLock : this.mWifiLocks) {
            if (wifiLock.getBinder() == iBinder) {
                return wifiLock;
            }
        }
        return null;
    }

    protected void dump(PrintWriter printWriter) {
        printWriter.println("Locks acquired: " + this.mFullLocksAcquired + " full, " + this.mFullHighPerfLocksAcquired + " full high perf, " + this.mScanLocksAcquired + " scan");
        printWriter.println("Locks released: " + this.mFullLocksReleased + " full, " + this.mFullHighPerfLocksReleased + " full high perf, " + this.mScanLocksReleased + " scan");
        printWriter.println();
        printWriter.println("Locks held:");
        for (WifiLock wifiLock : this.mWifiLocks) {
            printWriter.print("    ");
            printWriter.println(wifiLock);
        }
    }

    protected void enableVerboseLogging(int i) {
        if (i > 0) {
            this.mVerboseLoggingEnabled = true;
        } else {
            this.mVerboseLoggingEnabled = false;
        }
    }

    private class WifiLock implements IBinder.DeathRecipient {
        IBinder mBinder;
        int mMode;
        String mTag;
        int mUid = Binder.getCallingUid();
        WorkSource mWorkSource;

        WifiLock(int i, String str, IBinder iBinder, WorkSource workSource) {
            this.mTag = str;
            this.mBinder = iBinder;
            this.mMode = i;
            this.mWorkSource = workSource;
            try {
                this.mBinder.linkToDeath(this, 0);
            } catch (RemoteException e) {
                binderDied();
            }
        }

        protected WorkSource getWorkSource() {
            return this.mWorkSource;
        }

        protected int getUid() {
            return this.mUid;
        }

        protected IBinder getBinder() {
            return this.mBinder;
        }

        @Override
        public void binderDied() {
            WifiLockManager.this.releaseLock(this.mBinder);
        }

        public void unlinkDeathRecipient() {
            this.mBinder.unlinkToDeath(this, 0);
        }

        public String toString() {
            return "WifiLock{" + this.mTag + " type=" + this.mMode + " uid=" + this.mUid + " workSource=" + this.mWorkSource + "}";
        }
    }
}
