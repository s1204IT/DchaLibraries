package com.android.server.wifi;

import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import com.android.internal.app.IBatteryStats;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class WifiMulticastLockManager {
    private static final String TAG = "WifiMulticastLockManager";
    private final IBatteryStats mBatteryStats;
    private final FilterController mFilterController;
    private final List<Multicaster> mMulticasters = new ArrayList();
    private int mMulticastEnabled = 0;
    private int mMulticastDisabled = 0;
    private boolean mVerboseLoggingEnabled = false;

    public interface FilterController {
        void startFilteringMulticastPackets();

        void stopFilteringMulticastPackets();
    }

    public WifiMulticastLockManager(FilterController filterController, IBatteryStats iBatteryStats) {
        this.mBatteryStats = iBatteryStats;
        this.mFilterController = filterController;
    }

    private class Multicaster implements IBinder.DeathRecipient {
        IBinder mBinder;
        String mTag;
        int mUid = Binder.getCallingUid();

        Multicaster(String str, IBinder iBinder) {
            this.mTag = str;
            this.mBinder = iBinder;
            try {
                this.mBinder.linkToDeath(this, 0);
            } catch (RemoteException e) {
                binderDied();
            }
        }

        @Override
        public void binderDied() {
            Slog.e(WifiMulticastLockManager.TAG, "Multicaster binderDied");
            synchronized (WifiMulticastLockManager.this.mMulticasters) {
                int iIndexOf = WifiMulticastLockManager.this.mMulticasters.indexOf(this);
                if (iIndexOf != -1) {
                    WifiMulticastLockManager.this.removeMulticasterLocked(iIndexOf, this.mUid);
                }
            }
        }

        void unlinkDeathRecipient() {
            this.mBinder.unlinkToDeath(this, 0);
        }

        public int getUid() {
            return this.mUid;
        }

        public String toString() {
            return "Multicaster{" + this.mTag + " uid=" + this.mUid + "}";
        }
    }

    protected void dump(PrintWriter printWriter) {
        printWriter.println("mMulticastEnabled " + this.mMulticastEnabled);
        printWriter.println("mMulticastDisabled " + this.mMulticastDisabled);
        printWriter.println("Multicast Locks held:");
        for (Multicaster multicaster : this.mMulticasters) {
            printWriter.print("    ");
            printWriter.println(multicaster);
        }
    }

    protected void enableVerboseLogging(int i) {
        if (i > 0) {
            this.mVerboseLoggingEnabled = true;
        } else {
            this.mVerboseLoggingEnabled = false;
        }
    }

    public void initializeFiltering() {
        synchronized (this.mMulticasters) {
            if (this.mMulticasters.size() != 0) {
                return;
            }
            this.mFilterController.startFilteringMulticastPackets();
        }
    }

    public void acquireLock(IBinder iBinder, String str) {
        synchronized (this.mMulticasters) {
            this.mMulticastEnabled++;
            this.mMulticasters.add(new Multicaster(str, iBinder));
            this.mFilterController.stopFilteringMulticastPackets();
        }
        int callingUid = Binder.getCallingUid();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mBatteryStats.noteWifiMulticastEnabled(callingUid);
        } catch (RemoteException e) {
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
        Binder.restoreCallingIdentity(jClearCallingIdentity);
    }

    public void releaseLock() {
        int callingUid = Binder.getCallingUid();
        synchronized (this.mMulticasters) {
            this.mMulticastDisabled++;
            for (int size = this.mMulticasters.size() - 1; size >= 0; size--) {
                Multicaster multicaster = this.mMulticasters.get(size);
                if (multicaster != null && multicaster.getUid() == callingUid) {
                    removeMulticasterLocked(size, callingUid);
                }
            }
        }
    }

    private void removeMulticasterLocked(int i, int i2) {
        Multicaster multicasterRemove = this.mMulticasters.remove(i);
        if (multicasterRemove != null) {
            multicasterRemove.unlinkDeathRecipient();
        }
        if (this.mMulticasters.size() == 0) {
            this.mFilterController.startFilteringMulticastPackets();
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mBatteryStats.noteWifiMulticastDisabled(i2);
        } catch (RemoteException e) {
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
        Binder.restoreCallingIdentity(jClearCallingIdentity);
    }

    public boolean isMulticastEnabled() {
        boolean z;
        synchronized (this.mMulticasters) {
            z = this.mMulticasters.size() > 0;
        }
        return z;
    }
}
