package com.android.server.wifi;

import android.util.ArrayMap;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.WakeupConfigStoreData;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class WakeupLock {

    @VisibleForTesting
    static final int CONSECUTIVE_MISSED_SCANS_REQUIRED_TO_EVICT = 3;

    @VisibleForTesting
    static final long MAX_LOCK_TIME_MILLIS = 600000;
    private static final String TAG = WakeupLock.class.getSimpleName();
    private final Clock mClock;
    private boolean mIsInitialized;
    private long mLockTimestamp;
    private final Map<ScanResultMatchInfo, Integer> mLockedNetworks = new ArrayMap();
    private int mNumScans;
    private boolean mVerboseLoggingEnabled;
    private final WifiConfigManager mWifiConfigManager;
    private final WifiWakeMetrics mWifiWakeMetrics;

    public WakeupLock(WifiConfigManager wifiConfigManager, WifiWakeMetrics wifiWakeMetrics, Clock clock) {
        this.mWifiConfigManager = wifiConfigManager;
        this.mWifiWakeMetrics = wifiWakeMetrics;
        this.mClock = clock;
    }

    public void setLock(Collection<ScanResultMatchInfo> collection) {
        this.mLockTimestamp = this.mClock.getElapsedSinceBootMillis();
        this.mIsInitialized = false;
        this.mNumScans = 0;
        this.mLockedNetworks.clear();
        Iterator<ScanResultMatchInfo> it = collection.iterator();
        while (it.hasNext()) {
            this.mLockedNetworks.put(it.next(), 3);
        }
        Log.d(TAG, "Lock set. Number of networks: " + this.mLockedNetworks.size());
        this.mWifiConfigManager.saveToStore(false);
    }

    private void maybeSetInitializedByScans(int i) {
        boolean z;
        if (this.mIsInitialized) {
            return;
        }
        if (i < 3) {
            z = false;
        } else {
            z = true;
        }
        if (z) {
            this.mIsInitialized = true;
            Log.d(TAG, "Lock initialized by handled scans. Scans: " + i);
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "State of lock: " + this.mLockedNetworks);
            }
            this.mWifiWakeMetrics.recordInitializeEvent(this.mNumScans, this.mLockedNetworks.size());
        }
    }

    private void maybeSetInitializedByTimeout(long j) {
        boolean z;
        if (this.mIsInitialized) {
            return;
        }
        long j2 = j - this.mLockTimestamp;
        if (j2 <= MAX_LOCK_TIME_MILLIS) {
            z = false;
        } else {
            z = true;
        }
        if (z) {
            this.mIsInitialized = true;
            Log.d(TAG, "Lock initialized by timeout. Elapsed time: " + j2);
            if (this.mNumScans == 0) {
                Log.w(TAG, "Lock initialized with 0 handled scans!");
            }
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "State of lock: " + this.mLockedNetworks);
            }
            this.mWifiWakeMetrics.recordInitializeEvent(this.mNumScans, this.mLockedNetworks.size());
        }
    }

    public boolean isInitialized() {
        return this.mIsInitialized;
    }

    private void addToLock(Collection<ScanResultMatchInfo> collection) {
        if (this.mVerboseLoggingEnabled) {
            Log.d(TAG, "Initializing lock with networks: " + collection);
        }
        boolean z = false;
        for (ScanResultMatchInfo scanResultMatchInfo : collection) {
            if (!this.mLockedNetworks.containsKey(scanResultMatchInfo)) {
                this.mLockedNetworks.put(scanResultMatchInfo, 3);
                z = true;
            }
        }
        if (z) {
            this.mWifiConfigManager.saveToStore(false);
        }
        maybeSetInitializedByScans(this.mNumScans);
    }

    private void removeFromLock(Collection<ScanResultMatchInfo> collection) {
        if (this.mVerboseLoggingEnabled) {
            Log.d(TAG, "Filtering lock with networks: " + collection);
        }
        Iterator<Map.Entry<ScanResultMatchInfo, Integer>> it = this.mLockedNetworks.entrySet().iterator();
        boolean z = false;
        while (it.hasNext()) {
            Map.Entry<ScanResultMatchInfo, Integer> next = it.next();
            if (collection.contains(next.getKey())) {
                if (this.mVerboseLoggingEnabled) {
                    Log.d(TAG, "Found network in lock: " + next.getKey().networkSsid);
                }
                next.setValue(3);
            } else {
                next.setValue(Integer.valueOf(next.getValue().intValue() - 1));
                if (next.getValue().intValue() <= 0) {
                    Log.d(TAG, "Removed network from lock: " + next.getKey().networkSsid);
                    it.remove();
                    z = true;
                }
            }
        }
        if (z) {
            this.mWifiConfigManager.saveToStore(false);
        }
        if (isUnlocked()) {
            Log.d(TAG, "Lock emptied. Recording unlock event.");
            this.mWifiWakeMetrics.recordUnlockEvent(this.mNumScans);
        }
    }

    public void update(Collection<ScanResultMatchInfo> collection) {
        if (isUnlocked()) {
            return;
        }
        maybeSetInitializedByTimeout(this.mClock.getElapsedSinceBootMillis());
        this.mNumScans++;
        if (this.mIsInitialized) {
            removeFromLock(collection);
        } else {
            addToLock(collection);
        }
    }

    public boolean isUnlocked() {
        return this.mIsInitialized && this.mLockedNetworks.isEmpty();
    }

    public WakeupConfigStoreData.DataSource<Set<ScanResultMatchInfo>> getDataSource() {
        return new WakeupLockDataSource();
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("WakeupLock: ");
        printWriter.println("mNumScans: " + this.mNumScans);
        printWriter.println("mIsInitialized: " + this.mIsInitialized);
        printWriter.println("Locked networks: " + this.mLockedNetworks.size());
        for (Map.Entry<ScanResultMatchInfo, Integer> entry : this.mLockedNetworks.entrySet()) {
            printWriter.println(entry.getKey() + ", scans to evict: " + entry.getValue());
        }
    }

    public void enableVerboseLogging(boolean z) {
        this.mVerboseLoggingEnabled = z;
    }

    private class WakeupLockDataSource implements WakeupConfigStoreData.DataSource<Set<ScanResultMatchInfo>> {
        private WakeupLockDataSource() {
        }

        @Override
        public Set<ScanResultMatchInfo> getData() {
            return WakeupLock.this.mLockedNetworks.keySet();
        }

        @Override
        public void setData(Set<ScanResultMatchInfo> set) {
            WakeupLock.this.mLockedNetworks.clear();
            Iterator<ScanResultMatchInfo> it = set.iterator();
            while (it.hasNext()) {
                WakeupLock.this.mLockedNetworks.put(it.next(), 3);
            }
            WakeupLock.this.mIsInitialized = true;
        }
    }
}
