package com.android.server.am;

import android.bluetooth.BluetoothActivityEnergyInfo;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.net.wifi.IWifiManager;
import android.net.wifi.WifiActivityEnergyInfo;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.SynchronousResultReceiver;
import android.os.SystemClock;
import android.telephony.ModemActivityInfo;
import android.telephony.TelephonyManager;
import android.util.IntArray;
import android.util.Slog;
import android.util.TimeUtils;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.os.BatteryStatsImpl;
import com.android.internal.util.function.TriConsumer;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.stats.StatsCompanionService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import libcore.util.EmptyArray;

class BatteryExternalStatsWorker implements BatteryStatsImpl.ExternalStatsSync {
    private static final boolean DEBUG = false;
    private static final long EXTERNAL_STATS_SYNC_TIMEOUT_MILLIS = 2000;
    private static final long MAX_WIFI_STATS_SAMPLE_ERROR_MILLIS = 750;
    private static final String TAG = "BatteryExternalStatsWorker";

    @GuardedBy("this")
    private Future<?> mBatteryLevelSync;
    private final Context mContext;

    @GuardedBy("this")
    private long mLastCollectionTimeStamp;

    @GuardedBy("this")
    private boolean mOnBattery;

    @GuardedBy("this")
    private boolean mOnBatteryScreenOff;
    private final BatteryStatsImpl mStats;

    @GuardedBy("this")
    private Future<?> mWakelockChangesUpdate;
    private final ScheduledExecutorService mExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        @Override
        public final Thread newThread(Runnable runnable) {
            return BatteryExternalStatsWorker.lambda$new$0(runnable);
        }
    });

    @GuardedBy("this")
    private int mUpdateFlags = 0;

    @GuardedBy("this")
    private Future<?> mCurrentFuture = null;

    @GuardedBy("this")
    private String mCurrentReason = null;

    @GuardedBy("this")
    private boolean mUseLatestStates = true;

    @GuardedBy("this")
    private final IntArray mUidsToRemove = new IntArray();
    private final Object mWorkerLock = new Object();

    @GuardedBy("mWorkerLock")
    private IWifiManager mWifiManager = null;

    @GuardedBy("mWorkerLock")
    private TelephonyManager mTelephony = null;

    @GuardedBy("mWorkerLock")
    private WifiActivityEnergyInfo mLastInfo = new WifiActivityEnergyInfo(0, 0, 0, new long[]{0}, 0, 0, 0, 0);
    private final Runnable mSyncTask = new Runnable() {
        @Override
        public void run() {
            int i;
            String str;
            int[] array;
            boolean z;
            boolean z2;
            boolean z3;
            int i2;
            int i3;
            synchronized (BatteryExternalStatsWorker.this) {
                i = BatteryExternalStatsWorker.this.mUpdateFlags;
                str = BatteryExternalStatsWorker.this.mCurrentReason;
                array = BatteryExternalStatsWorker.this.mUidsToRemove.size() > 0 ? BatteryExternalStatsWorker.this.mUidsToRemove.toArray() : EmptyArray.INT;
                z = BatteryExternalStatsWorker.this.mOnBattery;
                z2 = BatteryExternalStatsWorker.this.mOnBatteryScreenOff;
                z3 = BatteryExternalStatsWorker.this.mUseLatestStates;
                BatteryExternalStatsWorker.this.mUpdateFlags = 0;
                BatteryExternalStatsWorker.this.mCurrentReason = null;
                BatteryExternalStatsWorker.this.mUidsToRemove.clear();
                BatteryExternalStatsWorker.this.mCurrentFuture = null;
                BatteryExternalStatsWorker.this.mUseLatestStates = true;
                if ((i & 31) != 0) {
                    BatteryExternalStatsWorker.this.cancelSyncDueToBatteryLevelChangeLocked();
                }
                i3 = i & 1;
                if (i3 != 0) {
                    BatteryExternalStatsWorker.this.cancelCpuSyncDueToWakelockChange();
                }
            }
            try {
                synchronized (BatteryExternalStatsWorker.this.mWorkerLock) {
                    BatteryExternalStatsWorker.this.updateExternalStatsLocked(str, i, z, z2, z3);
                }
                if (i3 != 0) {
                    BatteryExternalStatsWorker.this.mStats.copyFromAllUidsCpuTimes();
                }
                synchronized (BatteryExternalStatsWorker.this.mStats) {
                    for (int i4 : array) {
                        BatteryExternalStatsWorker.this.mStats.removeIsolatedUidLocked(i4);
                    }
                    BatteryExternalStatsWorker.this.mStats.clearPendingRemovedUids();
                }
            } catch (Exception e) {
                Slog.wtf(BatteryExternalStatsWorker.TAG, "Error updating external stats: ", e);
            }
            synchronized (BatteryExternalStatsWorker.this) {
                BatteryExternalStatsWorker.this.mLastCollectionTimeStamp = SystemClock.elapsedRealtime();
            }
        }
    };
    private final Runnable mWriteTask = new Runnable() {
        @Override
        public void run() {
            synchronized (BatteryExternalStatsWorker.this.mStats) {
                BatteryExternalStatsWorker.this.mStats.writeAsyncLocked();
            }
        }
    };

    static Thread lambda$new$0(Runnable runnable) {
        Thread thread = new Thread(runnable, "batterystats-worker");
        thread.setPriority(5);
        return thread;
    }

    BatteryExternalStatsWorker(Context context, BatteryStatsImpl batteryStatsImpl) {
        this.mContext = context;
        this.mStats = batteryStatsImpl;
    }

    public synchronized Future<?> scheduleSync(String str, int i) {
        return scheduleSyncLocked(str, i);
    }

    public synchronized Future<?> scheduleCpuSyncDueToRemovedUid(int i) {
        this.mUidsToRemove.add(i);
        return scheduleSyncLocked("remove-uid", 1);
    }

    public synchronized Future<?> scheduleCpuSyncDueToSettingChange() {
        return scheduleSyncLocked("setting-change", 1);
    }

    public Future<?> scheduleReadProcStateCpuTimes(boolean z, boolean z2, long j) {
        synchronized (this.mStats) {
            if (!this.mStats.trackPerProcStateCpuTimes()) {
                return null;
            }
            synchronized (this) {
                if (this.mExecutorService.isShutdown()) {
                    return null;
                }
                return this.mExecutorService.schedule((Runnable) PooledLambda.obtainRunnable(new TriConsumer() {
                    public final void accept(Object obj, Object obj2, Object obj3) {
                        ((BatteryStatsImpl) obj).updateProcStateCpuTimes(((Boolean) obj2).booleanValue(), ((Boolean) obj3).booleanValue());
                    }
                }, this.mStats, Boolean.valueOf(z), Boolean.valueOf(z2)).recycleOnUse(), j, TimeUnit.MILLISECONDS);
            }
        }
    }

    public Future<?> scheduleCopyFromAllUidsCpuTimes(boolean z, boolean z2) {
        synchronized (this.mStats) {
            if (!this.mStats.trackPerProcStateCpuTimes()) {
                return null;
            }
            synchronized (this) {
                if (this.mExecutorService.isShutdown()) {
                    return null;
                }
                return this.mExecutorService.submit((Runnable) PooledLambda.obtainRunnable(new TriConsumer() {
                    public final void accept(Object obj, Object obj2, Object obj3) {
                        ((BatteryStatsImpl) obj).copyFromAllUidsCpuTimes(((Boolean) obj2).booleanValue(), ((Boolean) obj3).booleanValue());
                    }
                }, this.mStats, Boolean.valueOf(z), Boolean.valueOf(z2)).recycleOnUse());
            }
        }
    }

    public Future<?> scheduleCpuSyncDueToScreenStateChange(boolean z, boolean z2) {
        Future<?> futureScheduleSyncLocked;
        synchronized (this) {
            if (this.mCurrentFuture == null || (this.mUpdateFlags & 1) == 0) {
                this.mOnBattery = z;
                this.mOnBatteryScreenOff = z2;
                this.mUseLatestStates = false;
            }
            futureScheduleSyncLocked = scheduleSyncLocked("screen-state", 1);
        }
        return futureScheduleSyncLocked;
    }

    public Future<?> scheduleCpuSyncDueToWakelockChange(long j) {
        Future<?> future;
        synchronized (this) {
            this.mWakelockChangesUpdate = scheduleDelayedSyncLocked(this.mWakelockChangesUpdate, new Runnable() {
                @Override
                public final void run() {
                    BatteryExternalStatsWorker.lambda$scheduleCpuSyncDueToWakelockChange$2(this.f$0);
                }
            }, j);
            future = this.mWakelockChangesUpdate;
        }
        return future;
    }

    public static void lambda$scheduleCpuSyncDueToWakelockChange$2(final BatteryExternalStatsWorker batteryExternalStatsWorker) {
        batteryExternalStatsWorker.scheduleSync("wakelock-change", 1);
        batteryExternalStatsWorker.scheduleRunnable(new Runnable() {
            @Override
            public final void run() {
                this.f$0.mStats.postBatteryNeedsCpuUpdateMsg();
            }
        });
    }

    public void cancelCpuSyncDueToWakelockChange() {
        synchronized (this) {
            if (this.mWakelockChangesUpdate != null) {
                this.mWakelockChangesUpdate.cancel(false);
                this.mWakelockChangesUpdate = null;
            }
        }
    }

    public Future<?> scheduleSyncDueToBatteryLevelChange(long j) {
        Future<?> future;
        synchronized (this) {
            this.mBatteryLevelSync = scheduleDelayedSyncLocked(this.mBatteryLevelSync, new Runnable() {
                @Override
                public final void run() {
                    this.f$0.scheduleSync("battery-level", 31);
                }
            }, j);
            future = this.mBatteryLevelSync;
        }
        return future;
    }

    @GuardedBy("this")
    private void cancelSyncDueToBatteryLevelChangeLocked() {
        if (this.mBatteryLevelSync != null) {
            this.mBatteryLevelSync.cancel(false);
            this.mBatteryLevelSync = null;
        }
    }

    @GuardedBy("this")
    private Future<?> scheduleDelayedSyncLocked(Future<?> future, Runnable runnable, long j) {
        if (this.mExecutorService.isShutdown()) {
            return CompletableFuture.failedFuture(new IllegalStateException("worker shutdown"));
        }
        if (future != null) {
            if (j == 0) {
                future.cancel(false);
            } else {
                return future;
            }
        }
        return this.mExecutorService.schedule(runnable, j, TimeUnit.MILLISECONDS);
    }

    public synchronized Future<?> scheduleWrite() {
        if (this.mExecutorService.isShutdown()) {
            return CompletableFuture.failedFuture(new IllegalStateException("worker shutdown"));
        }
        scheduleSyncLocked("write", 31);
        return this.mExecutorService.submit(this.mWriteTask);
    }

    public synchronized void scheduleRunnable(Runnable runnable) {
        if (!this.mExecutorService.isShutdown()) {
            this.mExecutorService.submit(runnable);
        }
    }

    public void shutdown() {
        this.mExecutorService.shutdownNow();
    }

    @GuardedBy("this")
    private Future<?> scheduleSyncLocked(String str, int i) {
        if (this.mExecutorService.isShutdown()) {
            return CompletableFuture.failedFuture(new IllegalStateException("worker shutdown"));
        }
        if (this.mCurrentFuture == null) {
            this.mUpdateFlags = i;
            this.mCurrentReason = str;
            this.mCurrentFuture = this.mExecutorService.submit(this.mSyncTask);
        }
        this.mUpdateFlags |= i;
        return this.mCurrentFuture;
    }

    long getLastCollectionTimeStamp() {
        long j;
        synchronized (this) {
            j = this.mLastCollectionTimeStamp;
        }
        return j;
    }

    @GuardedBy("mWorkerLock")
    private void updateExternalStatsLocked(String str, int i, boolean z, boolean z2, boolean z3) {
        SynchronousResultReceiver synchronousResultReceiver;
        SynchronousResultReceiver synchronousResultReceiver2;
        boolean zIsOnBatteryLocked;
        boolean zIsOnBatteryScreenOffLocked;
        BluetoothAdapter defaultAdapter;
        ResultReceiver synchronousResultReceiver3 = null;
        if ((i & 2) != 0) {
            if (this.mWifiManager == null) {
                this.mWifiManager = IWifiManager.Stub.asInterface(ServiceManager.getService("wifi"));
            }
            if (this.mWifiManager != null) {
                try {
                    synchronousResultReceiver = new SynchronousResultReceiver("wifi");
                    try {
                        this.mWifiManager.requestActivityInfo(synchronousResultReceiver);
                    } catch (RemoteException e) {
                    }
                } catch (RemoteException e2) {
                    synchronousResultReceiver = null;
                }
            }
        } else {
            synchronousResultReceiver = null;
        }
        if ((i & 8) != 0 && (defaultAdapter = BluetoothAdapter.getDefaultAdapter()) != null) {
            synchronousResultReceiver2 = new SynchronousResultReceiver("bluetooth");
            defaultAdapter.requestControllerActivityEnergyInfo(synchronousResultReceiver2);
        } else {
            synchronousResultReceiver2 = null;
        }
        if ((i & 4) != 0) {
            if (this.mTelephony == null) {
                this.mTelephony = TelephonyManager.from(this.mContext);
            }
            if (this.mTelephony != null) {
                synchronousResultReceiver3 = new SynchronousResultReceiver("telephony");
                this.mTelephony.requestModemActivityInfo(synchronousResultReceiver3);
            }
        }
        WifiActivityEnergyInfo wifiActivityEnergyInfo = (WifiActivityEnergyInfo) awaitControllerInfo(synchronousResultReceiver);
        BluetoothActivityEnergyInfo bluetoothActivityEnergyInfoAwaitControllerInfo = awaitControllerInfo(synchronousResultReceiver2);
        ModemActivityInfo modemActivityInfoAwaitControllerInfo = awaitControllerInfo(synchronousResultReceiver3);
        synchronized (this.mStats) {
            this.mStats.addHistoryEventLocked(SystemClock.elapsedRealtime(), SystemClock.uptimeMillis(), 14, str, 0);
            if ((i & 1) != 0) {
                if (z3) {
                    zIsOnBatteryLocked = this.mStats.isOnBatteryLocked();
                    zIsOnBatteryScreenOffLocked = this.mStats.isOnBatteryScreenOffLocked();
                } else {
                    zIsOnBatteryLocked = z;
                    zIsOnBatteryScreenOffLocked = z2;
                }
                this.mStats.updateCpuTimeLocked(zIsOnBatteryLocked, zIsOnBatteryScreenOffLocked);
            }
            if ((i & 31) != 0) {
                this.mStats.updateKernelWakelocksLocked();
                this.mStats.updateKernelMemoryBandwidthLocked();
            }
            if ((i & 16) != 0) {
                this.mStats.updateRpmStatsLocked();
            }
            if (bluetoothActivityEnergyInfoAwaitControllerInfo != null) {
                if (bluetoothActivityEnergyInfoAwaitControllerInfo.isValid()) {
                    this.mStats.updateBluetoothStateLocked(bluetoothActivityEnergyInfoAwaitControllerInfo);
                } else {
                    Slog.w(TAG, "bluetooth info is invalid: " + bluetoothActivityEnergyInfoAwaitControllerInfo);
                }
            }
        }
        if (wifiActivityEnergyInfo != null) {
            if (wifiActivityEnergyInfo.isValid()) {
                this.mStats.updateWifiState(extractDeltaLocked(wifiActivityEnergyInfo));
            } else {
                Slog.w(TAG, "wifi info is invalid: " + wifiActivityEnergyInfo);
            }
        }
        if (modemActivityInfoAwaitControllerInfo != null) {
            if (modemActivityInfoAwaitControllerInfo.isValid()) {
                this.mStats.updateMobileRadioState(modemActivityInfoAwaitControllerInfo);
                return;
            }
            Slog.w(TAG, "modem info is invalid: " + modemActivityInfoAwaitControllerInfo);
        }
    }

    private static <T extends Parcelable> T awaitControllerInfo(SynchronousResultReceiver synchronousResultReceiver) {
        if (synchronousResultReceiver == null) {
            return null;
        }
        try {
            SynchronousResultReceiver.Result resultAwaitResult = synchronousResultReceiver.awaitResult(EXTERNAL_STATS_SYNC_TIMEOUT_MILLIS);
            if (resultAwaitResult.bundle != null) {
                resultAwaitResult.bundle.setDefusable(true);
                T t = (T) resultAwaitResult.bundle.getParcelable(StatsCompanionService.RESULT_RECEIVER_CONTROLLER_KEY);
                if (t != null) {
                    return t;
                }
            }
            Slog.e(TAG, "no controller energy info supplied for " + synchronousResultReceiver.getName());
        } catch (TimeoutException e) {
            Slog.w(TAG, "timeout reading " + synchronousResultReceiver.getName() + " stats");
        }
        return null;
    }

    @GuardedBy("mWorkerLock")
    private WifiActivityEnergyInfo extractDeltaLocked(WifiActivityEnergyInfo wifiActivityEnergyInfo) {
        WifiActivityEnergyInfo wifiActivityEnergyInfo2;
        long j;
        long j2;
        long j3;
        long j4 = wifiActivityEnergyInfo.mTimestamp - this.mLastInfo.mTimestamp;
        long j5 = this.mLastInfo.mControllerScanTimeMs;
        long j6 = this.mLastInfo.mControllerIdleTimeMs;
        long j7 = this.mLastInfo.mControllerTxTimeMs;
        long j8 = this.mLastInfo.mControllerRxTimeMs;
        long j9 = this.mLastInfo.mControllerEnergyUsed;
        WifiActivityEnergyInfo wifiActivityEnergyInfo3 = this.mLastInfo;
        wifiActivityEnergyInfo3.mTimestamp = wifiActivityEnergyInfo.getTimeStamp();
        wifiActivityEnergyInfo3.mStackState = wifiActivityEnergyInfo.getStackState();
        long j10 = wifiActivityEnergyInfo.mControllerTxTimeMs - j7;
        long j11 = wifiActivityEnergyInfo.mControllerRxTimeMs - j8;
        long j12 = wifiActivityEnergyInfo.mControllerIdleTimeMs - j6;
        long j13 = wifiActivityEnergyInfo.mControllerScanTimeMs - j5;
        if (j10 < 0 || j11 < 0 || j13 < 0) {
            wifiActivityEnergyInfo2 = wifiActivityEnergyInfo3;
            wifiActivityEnergyInfo2.mControllerEnergyUsed = wifiActivityEnergyInfo.mControllerEnergyUsed;
            wifiActivityEnergyInfo2.mControllerRxTimeMs = wifiActivityEnergyInfo.mControllerRxTimeMs;
            wifiActivityEnergyInfo2.mControllerTxTimeMs = wifiActivityEnergyInfo.mControllerTxTimeMs;
            wifiActivityEnergyInfo2.mControllerIdleTimeMs = wifiActivityEnergyInfo.mControllerIdleTimeMs;
            wifiActivityEnergyInfo2.mControllerScanTimeMs = wifiActivityEnergyInfo.mControllerScanTimeMs;
            Slog.v(TAG, "WiFi energy data was reset, new WiFi energy data is " + wifiActivityEnergyInfo2);
        } else {
            long j14 = j10 + j11;
            if (j14 > j4) {
                if (j14 > j4 + MAX_WIFI_STATS_SAMPLE_ERROR_MILLIS) {
                    StringBuilder sb = new StringBuilder();
                    j = j13;
                    sb.append("Total Active time ");
                    TimeUtils.formatDuration(j14, sb);
                    sb.append(" is longer than sample period ");
                    TimeUtils.formatDuration(j4, sb);
                    sb.append(".\n");
                    sb.append("Previous WiFi snapshot: ");
                    sb.append("idle=");
                    TimeUtils.formatDuration(j6, sb);
                    sb.append(" rx=");
                    TimeUtils.formatDuration(j8, sb);
                    sb.append(" tx=");
                    TimeUtils.formatDuration(j7, sb);
                    sb.append(" e=");
                    j2 = j9;
                    sb.append(j2);
                    sb.append("\n");
                    sb.append("Current WiFi snapshot: ");
                    sb.append("idle=");
                    TimeUtils.formatDuration(wifiActivityEnergyInfo.mControllerIdleTimeMs, sb);
                    sb.append(" rx=");
                    TimeUtils.formatDuration(wifiActivityEnergyInfo.mControllerRxTimeMs, sb);
                    sb.append(" tx=");
                    TimeUtils.formatDuration(wifiActivityEnergyInfo.mControllerTxTimeMs, sb);
                    sb.append(" e=");
                    sb.append(wifiActivityEnergyInfo.mControllerEnergyUsed);
                    Slog.wtf(TAG, sb.toString());
                } else {
                    j = j13;
                    j2 = j9;
                }
                j3 = 0;
            } else {
                j = j13;
                j2 = j9;
                j3 = j4 - j14;
            }
            wifiActivityEnergyInfo2 = wifiActivityEnergyInfo3;
            wifiActivityEnergyInfo2.mControllerTxTimeMs = j10;
            wifiActivityEnergyInfo2.mControllerRxTimeMs = j11;
            wifiActivityEnergyInfo2.mControllerScanTimeMs = j;
            wifiActivityEnergyInfo2.mControllerIdleTimeMs = Math.min(j3, Math.max(0L, j12));
            wifiActivityEnergyInfo2.mControllerEnergyUsed = Math.max(0L, wifiActivityEnergyInfo.mControllerEnergyUsed - j2);
        }
        this.mLastInfo = wifiActivityEnergyInfo;
        return wifiActivityEnergyInfo2;
    }
}
