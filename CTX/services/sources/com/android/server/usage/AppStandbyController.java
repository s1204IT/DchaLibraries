package com.android.server.usage;

import android.R;
import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.usage.AppStandbyInfo;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManagerInternal;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ParceledListSlice;
import android.database.ContentObserver;
import android.hardware.display.DisplayManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.NetworkScoreManager;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.Handler;
import android.os.IDeviceIdleController;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.ArraySet;
import android.util.KeyValueListParser;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.TimeUtils;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.IBatteryStats;
import com.android.internal.os.SomeArgs;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.ConcurrentUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.LocalServices;
import com.android.server.job.JobPackageTracker;
import com.android.server.pm.DumpState;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.Settings;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.usage.AppIdleHistory;
import com.android.server.usb.descriptors.UsbTerminalTypes;
import java.io.File;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class AppStandbyController {
    static final boolean COMPRESS_TIME = false;
    static final boolean DEBUG = false;
    private static final long DEFAULT_PREDICTION_TIMEOUT = 43200000;
    static final int MSG_CHECK_IDLE_STATES = 5;
    static final int MSG_CHECK_PACKAGE_IDLE_STATE = 11;
    static final int MSG_CHECK_PAROLE_TIMEOUT = 6;
    static final int MSG_FORCE_IDLE_STATE = 4;
    static final int MSG_INFORM_LISTENERS = 3;
    static final int MSG_ONE_TIME_CHECK_IDLE_STATES = 10;
    static final int MSG_PAROLE_END_TIMEOUT = 7;
    static final int MSG_PAROLE_STATE_CHANGED = 9;
    static final int MSG_REPORT_CONTENT_PROVIDER_USAGE = 8;
    static final int MSG_REPORT_EXEMPTED_SYNC_SCHEDULED = 12;
    static final int MSG_REPORT_EXEMPTED_SYNC_START = 13;
    static final int MSG_UPDATE_STABLE_CHARGING = 14;
    private static final long ONE_DAY = 86400000;
    private static final long ONE_HOUR = 3600000;
    private static final long ONE_MINUTE = 60000;
    private static final String TAG = "AppStandbyController";
    private static final long WAIT_FOR_ADMIN_DATA_TIMEOUT_MS = 10000;

    @GuardedBy("mActiveAdminApps")
    private final SparseArray<Set<String>> mActiveAdminApps;
    private final CountDownLatch mAdminDataAvailableLatch;
    volatile boolean mAppIdleEnabled;

    @GuardedBy("mAppIdleLock")
    private AppIdleHistory mAppIdleHistory;
    private final Object mAppIdleLock;
    long mAppIdleParoleDurationMillis;
    long mAppIdleParoleIntervalMillis;
    long mAppIdleParoleWindowMillis;
    boolean mAppIdleTempParoled;
    long[] mAppStandbyElapsedThresholds;
    long[] mAppStandbyScreenThresholds;
    private AppWidgetManager mAppWidgetManager;

    @GuardedBy("mAppIdleLock")
    private List<String> mCarrierPrivilegedApps;
    boolean mCharging;
    boolean mChargingStable;
    long mCheckIdleIntervalMillis;
    private ConnectivityManager mConnectivityManager;
    private final Context mContext;
    private final DeviceStateReceiver mDeviceStateReceiver;
    private final DisplayManager.DisplayListener mDisplayListener;
    long mExemptedSyncScheduledDozeTimeoutMillis;
    long mExemptedSyncScheduledNonDozeTimeoutMillis;
    long mExemptedSyncStartTimeoutMillis;
    private final AppStandbyHandler mHandler;

    @GuardedBy("mAppIdleLock")
    private boolean mHaveCarrierPrivilegedApps;
    Injector mInjector;
    private long mLastAppIdleParoledTime;
    private final ConnectivityManager.NetworkCallback mNetworkCallback;
    private final NetworkRequest mNetworkRequest;
    long mNotificationSeenTimeoutMillis;

    @GuardedBy("mPackageAccessListeners")
    private ArrayList<UsageStatsManagerInternal.AppIdleStateChangeListener> mPackageAccessListeners;
    private PackageManager mPackageManager;
    private boolean mPendingInitializeDefaults;
    private volatile boolean mPendingOneTimeCheckIdleStates;
    private PowerManager mPowerManager;
    long mPredictionTimeoutMillis;
    long mStableChargingThresholdMillis;
    long mStrongUsageTimeoutMillis;
    long mSyncAdapterTimeoutMillis;
    long mSystemInteractionTimeoutMillis;
    private boolean mSystemServicesReady;
    long mSystemUpdateUsageTimeoutMillis;
    static final long[] SCREEN_TIME_THRESHOLDS = {0, 0, 3600000, SettingsObserver.DEFAULT_SYSTEM_UPDATE_TIMEOUT};
    static final long[] ELAPSED_TIME_THRESHOLDS = {0, 43200000, 86400000, 172800000};
    static final int[] THRESHOLD_BUCKETS = {10, 20, 30, 40};
    static final ArrayList<StandbyUpdateRecord> sStandbyUpdatePool = new ArrayList<>(4);

    static class Lock {
        Lock() {
        }
    }

    public static class StandbyUpdateRecord {
        int bucket;
        boolean isUserInteraction;
        String packageName;
        int reason;
        int userId;

        StandbyUpdateRecord(String str, int i, int i2, int i3, boolean z) {
            this.packageName = str;
            this.userId = i;
            this.bucket = i2;
            this.reason = i3;
            this.isUserInteraction = z;
        }

        public static StandbyUpdateRecord obtain(String str, int i, int i2, int i3, boolean z) {
            synchronized (AppStandbyController.sStandbyUpdatePool) {
                int size = AppStandbyController.sStandbyUpdatePool.size();
                if (size >= 1) {
                    StandbyUpdateRecord standbyUpdateRecordRemove = AppStandbyController.sStandbyUpdatePool.remove(size - 1);
                    standbyUpdateRecordRemove.packageName = str;
                    standbyUpdateRecordRemove.userId = i;
                    standbyUpdateRecordRemove.bucket = i2;
                    standbyUpdateRecordRemove.reason = i3;
                    standbyUpdateRecordRemove.isUserInteraction = z;
                    return standbyUpdateRecordRemove;
                }
                return new StandbyUpdateRecord(str, i, i2, i3, z);
            }
        }

        public void recycle() {
            synchronized (AppStandbyController.sStandbyUpdatePool) {
                AppStandbyController.sStandbyUpdatePool.add(this);
            }
        }
    }

    AppStandbyController(Context context, Looper looper) {
        this(new Injector(context, looper));
    }

    AppStandbyController(Injector injector) {
        this.mAppIdleLock = new Lock();
        this.mPackageAccessListeners = new ArrayList<>();
        this.mActiveAdminApps = new SparseArray<>();
        this.mAdminDataAvailableLatch = new CountDownLatch(1);
        this.mAppStandbyScreenThresholds = SCREEN_TIME_THRESHOLDS;
        this.mAppStandbyElapsedThresholds = ELAPSED_TIME_THRESHOLDS;
        this.mSystemServicesReady = false;
        this.mNetworkRequest = new NetworkRequest.Builder().build();
        this.mNetworkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                AppStandbyController.this.mConnectivityManager.unregisterNetworkCallback(this);
                AppStandbyController.this.checkParoleTimeout();
            }
        };
        this.mDisplayListener = new DisplayManager.DisplayListener() {
            @Override
            public void onDisplayAdded(int i) {
            }

            @Override
            public void onDisplayRemoved(int i) {
            }

            @Override
            public void onDisplayChanged(int i) {
                if (i == 0) {
                    boolean zIsDisplayOn = AppStandbyController.this.isDisplayOn();
                    synchronized (AppStandbyController.this.mAppIdleLock) {
                        AppStandbyController.this.mAppIdleHistory.updateDisplay(zIsDisplayOn, AppStandbyController.this.mInjector.elapsedRealtime());
                    }
                }
            }
        };
        this.mInjector = injector;
        this.mContext = this.mInjector.getContext();
        this.mHandler = new AppStandbyHandler(this.mInjector.getLooper());
        this.mPackageManager = this.mContext.getPackageManager();
        this.mDeviceStateReceiver = new DeviceStateReceiver();
        IntentFilter intentFilter = new IntentFilter("android.os.action.CHARGING");
        intentFilter.addAction("android.os.action.DISCHARGING");
        intentFilter.addAction("android.os.action.DEVICE_IDLE_MODE_CHANGED");
        this.mContext.registerReceiver(this.mDeviceStateReceiver, intentFilter);
        synchronized (this.mAppIdleLock) {
            this.mAppIdleHistory = new AppIdleHistory(this.mInjector.getDataSystemDirectory(), this.mInjector.elapsedRealtime());
        }
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.intent.action.PACKAGE_ADDED");
        intentFilter2.addAction("android.intent.action.PACKAGE_CHANGED");
        intentFilter2.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter2.addDataScheme(Settings.ATTR_PACKAGE);
        this.mContext.registerReceiverAsUser(new PackageReceiver(), UserHandle.ALL, intentFilter2, null, this.mHandler);
    }

    void setAppIdleEnabled(boolean z) {
        synchronized (this.mAppIdleLock) {
            if (this.mAppIdleEnabled != z) {
                boolean zIsParoledOrCharging = isParoledOrCharging();
                this.mAppIdleEnabled = z;
                if (isParoledOrCharging() != zIsParoledOrCharging) {
                    postParoleStateChanged();
                }
            }
        }
    }

    public void onBootPhase(int i) {
        this.mInjector.onBootPhase(i);
        if (i == 500) {
            Slog.d(TAG, "Setting app idle enabled state");
            SettingsObserver settingsObserver = new SettingsObserver(this.mHandler);
            settingsObserver.registerObserver();
            settingsObserver.updateSettings();
            this.mAppWidgetManager = (AppWidgetManager) this.mContext.getSystemService(AppWidgetManager.class);
            this.mConnectivityManager = (ConnectivityManager) this.mContext.getSystemService(ConnectivityManager.class);
            this.mPowerManager = (PowerManager) this.mContext.getSystemService(PowerManager.class);
            this.mInjector.registerDisplayListener(this.mDisplayListener, this.mHandler);
            synchronized (this.mAppIdleLock) {
                this.mAppIdleHistory.updateDisplay(isDisplayOn(), this.mInjector.elapsedRealtime());
            }
            this.mSystemServicesReady = true;
            if (this.mPendingInitializeDefaults) {
                initializeDefaultsForSystemApps(0);
            }
            if (this.mPendingOneTimeCheckIdleStates) {
                postOneTimeCheckIdleStates();
                return;
            }
            return;
        }
        if (i == 1000) {
            setChargingState(this.mInjector.isCharging());
        }
    }

    void reportContentProviderUsage(String str, String str2, int i) throws Throwable {
        int i2;
        int i3;
        String[] strArr;
        Object obj;
        int i4 = i;
        if (this.mAppIdleEnabled) {
            String[] syncAdapterPackagesForAuthorityAsUser = ContentResolver.getSyncAdapterPackagesForAuthorityAsUser(str, i4);
            long jElapsedRealtime = this.mInjector.elapsedRealtime();
            int length = syncAdapterPackagesForAuthorityAsUser.length;
            int i5 = 0;
            while (i5 < length) {
                String str3 = syncAdapterPackagesForAuthorityAsUser[i5];
                try {
                    PackageInfo packageInfoAsUser = this.mPackageManager.getPackageInfoAsUser(str3, DumpState.DUMP_DEXOPT, i4);
                    if (packageInfoAsUser == null || packageInfoAsUser.applicationInfo == null) {
                        i2 = i5;
                        i3 = length;
                        strArr = syncAdapterPackagesForAuthorityAsUser;
                    } else if (!str3.equals(str2)) {
                        Object obj2 = this.mAppIdleLock;
                        synchronized (obj2) {
                            try {
                                AppIdleHistory.AppUsageHistory appUsageHistoryReportUsage = this.mAppIdleHistory.reportUsage(str3, i4, 10, 8, 0L, jElapsedRealtime + this.mSyncAdapterTimeoutMillis);
                                obj = obj2;
                                i2 = i5;
                                i3 = length;
                                strArr = syncAdapterPackagesForAuthorityAsUser;
                                try {
                                    maybeInformListeners(str3, i4, jElapsedRealtime, appUsageHistoryReportUsage.currentBucket, appUsageHistoryReportUsage.bucketingReason, false);
                                } catch (Throwable th) {
                                    th = th;
                                    try {
                                        throw th;
                                    } catch (PackageManager.NameNotFoundException e) {
                                    }
                                }
                            } catch (Throwable th2) {
                                th = th2;
                                obj = obj2;
                                i2 = i5;
                                i3 = length;
                                strArr = syncAdapterPackagesForAuthorityAsUser;
                            }
                        }
                    } else {
                        i2 = i5;
                        i3 = length;
                        strArr = syncAdapterPackagesForAuthorityAsUser;
                    }
                } catch (PackageManager.NameNotFoundException e2) {
                    i2 = i5;
                    i3 = length;
                    strArr = syncAdapterPackagesForAuthorityAsUser;
                }
                i5 = i2 + 1;
                i4 = i;
                syncAdapterPackagesForAuthorityAsUser = strArr;
                length = i3;
            }
        }
    }

    void reportExemptedSyncScheduled(String str, int i) throws Throwable {
        int i2;
        char c;
        long j;
        if (this.mAppIdleEnabled) {
            if (!this.mInjector.isDeviceIdleMode()) {
                i2 = 10;
                c = 11;
                j = this.mExemptedSyncScheduledNonDozeTimeoutMillis;
            } else {
                i2 = 20;
                c = '\f';
                j = this.mExemptedSyncScheduledDozeTimeoutMillis;
            }
            int i3 = i2;
            ?? r9 = c;
            long jElapsedRealtime = this.mInjector.elapsedRealtime();
            Object obj = this.mAppIdleLock;
            synchronized (obj) {
                try {
                    try {
                        AppIdleHistory.AppUsageHistory appUsageHistoryReportUsage = this.mAppIdleHistory.reportUsage(str, i, i3, r9, 0L, jElapsedRealtime + j);
                        maybeInformListeners(str, i, jElapsedRealtime, appUsageHistoryReportUsage.currentBucket, appUsageHistoryReportUsage.bucketingReason, false);
                    } catch (Throwable th) {
                        th = th;
                        r9 = obj;
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            }
        }
    }

    void reportExemptedSyncStart(String str, int i) {
        if (this.mAppIdleEnabled) {
            long jElapsedRealtime = this.mInjector.elapsedRealtime();
            synchronized (this.mAppIdleLock) {
                AppIdleHistory.AppUsageHistory appUsageHistoryReportUsage = this.mAppIdleHistory.reportUsage(str, i, 10, 13, 0L, jElapsedRealtime + this.mExemptedSyncStartTimeoutMillis);
                maybeInformListeners(str, i, jElapsedRealtime, appUsageHistoryReportUsage.currentBucket, appUsageHistoryReportUsage.bucketingReason, false);
            }
        }
    }

    void setChargingState(boolean z) {
        synchronized (this.mAppIdleLock) {
            if (this.mCharging != z) {
                this.mCharging = z;
                if (z) {
                    this.mHandler.sendEmptyMessageDelayed(14, this.mStableChargingThresholdMillis);
                } else {
                    this.mHandler.removeMessages(14);
                    updateChargingStableState();
                }
            }
        }
    }

    void updateChargingStableState() {
        synchronized (this.mAppIdleLock) {
            if (this.mChargingStable != this.mCharging) {
                this.mChargingStable = this.mCharging;
                postParoleStateChanged();
            }
        }
    }

    void setAppIdleParoled(boolean z) {
        synchronized (this.mAppIdleLock) {
            long jCurrentTimeMillis = this.mInjector.currentTimeMillis();
            if (this.mAppIdleTempParoled != z) {
                this.mAppIdleTempParoled = z;
                if (z) {
                    postParoleEndTimeout();
                } else {
                    this.mLastAppIdleParoledTime = jCurrentTimeMillis;
                    postNextParoleTimeout(jCurrentTimeMillis, false);
                }
                postParoleStateChanged();
            }
        }
    }

    boolean isParoledOrCharging() {
        boolean z = true;
        if (!this.mAppIdleEnabled) {
            return true;
        }
        synchronized (this.mAppIdleLock) {
            if (!this.mAppIdleTempParoled && !this.mChargingStable) {
                z = false;
            }
        }
        return z;
    }

    private void postNextParoleTimeout(long j, boolean z) {
        this.mHandler.removeMessages(6);
        long j2 = (this.mLastAppIdleParoledTime + this.mAppIdleParoleIntervalMillis) - j;
        if (z) {
            j2 += this.mAppIdleParoleWindowMillis;
        }
        long j3 = 0;
        if (j2 >= 0) {
            j3 = j2;
        }
        this.mHandler.sendEmptyMessageDelayed(6, j3);
    }

    private void postParoleEndTimeout() {
        this.mHandler.removeMessages(7);
        this.mHandler.sendEmptyMessageDelayed(7, this.mAppIdleParoleDurationMillis);
    }

    private void postParoleStateChanged() {
        this.mHandler.removeMessages(9);
        this.mHandler.sendEmptyMessage(9);
    }

    void postCheckIdleStates(int i) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(5, i, 0));
    }

    void postOneTimeCheckIdleStates() {
        if (this.mInjector.getBootPhase() < 500) {
            this.mPendingOneTimeCheckIdleStates = true;
        } else {
            this.mHandler.sendEmptyMessage(10);
            this.mPendingOneTimeCheckIdleStates = false;
        }
    }

    boolean checkIdleStates(int i) {
        if (!this.mAppIdleEnabled) {
            return false;
        }
        try {
            int[] runningUserIds = this.mInjector.getRunningUserIds();
            if (i != -1) {
                if (!ArrayUtils.contains(runningUserIds, i)) {
                    return false;
                }
            }
            long jElapsedRealtime = this.mInjector.elapsedRealtime();
            for (int i2 : runningUserIds) {
                if (i == -1 || i == i2) {
                    List installedPackagesAsUser = this.mPackageManager.getInstalledPackagesAsUser(512, i2);
                    int i3 = 0;
                    for (int size = installedPackagesAsUser.size(); i3 < size; size = size) {
                        PackageInfo packageInfo = (PackageInfo) installedPackagesAsUser.get(i3);
                        checkAndUpdateStandbyState(packageInfo.packageName, i2, packageInfo.applicationInfo.uid, jElapsedRealtime);
                        i3++;
                    }
                }
            }
            return true;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private void checkAndUpdateStandbyState(String str, int i, int i2, long j) {
        int packageUidAsUser;
        int i3;
        int i4;
        int i5;
        int i6;
        int i7;
        if (i2 <= 0) {
            try {
                packageUidAsUser = this.mPackageManager.getPackageUidAsUser(str, i);
            } catch (PackageManager.NameNotFoundException e) {
                return;
            }
        } else {
            packageUidAsUser = i2;
        }
        if (isAppSpecial(str, UserHandle.getAppId(packageUidAsUser), i)) {
            synchronized (this.mAppIdleLock) {
                this.mAppIdleHistory.setAppStandbyBucket(str, i, j, 5, 256);
            }
            maybeInformListeners(str, i, j, 5, 256, false);
            return;
        }
        synchronized (this.mAppIdleLock) {
            AppIdleHistory.AppUsageHistory appUsageHistory = this.mAppIdleHistory.getAppUsageHistory(str, i, j);
            int i8 = appUsageHistory.bucketingReason;
            int i9 = 65280 & i8;
            if (i9 == 1024) {
                return;
            }
            int i10 = appUsageHistory.currentBucket;
            int iMax = Math.max(i10, 10);
            boolean zPredictionTimedOut = predictionTimedOut(appUsageHistory, j);
            if (i9 == 256 || i9 == 768 || i9 == 512 || zPredictionTimedOut) {
                if (!zPredictionTimedOut && appUsageHistory.lastPredictedBucket >= 10 && appUsageHistory.lastPredictedBucket <= 40) {
                    iMax = appUsageHistory.lastPredictedBucket;
                    i8 = UsbTerminalTypes.TERMINAL_TELE_PHONELINE;
                } else {
                    iMax = getBucketForLocked(str, i, j);
                    i8 = 512;
                }
            }
            long elapsedTime = this.mAppIdleHistory.getElapsedTime(j);
            if (iMax < 10) {
                i3 = i10;
            } else {
                i3 = i10;
                if (appUsageHistory.bucketActiveTimeoutTime > elapsedTime) {
                    i5 = appUsageHistory.bucketingReason;
                    i6 = 10;
                    i4 = i3;
                }
                if (i4 >= i6 || zPredictionTimedOut) {
                    this.mAppIdleHistory.setAppStandbyBucket(str, i, j, i6, i5);
                    maybeInformListeners(str, i, j, i6, i5, false);
                }
            }
            if (iMax < 20 || appUsageHistory.bucketWorkingSetTimeoutTime <= elapsedTime) {
                i4 = i3;
                i5 = i8;
                i6 = iMax;
            } else {
                i4 = i3;
                if (20 == i4) {
                    i7 = appUsageHistory.bucketingReason;
                } else {
                    i7 = UsbTerminalTypes.TERMINAL_OUT_LFSPEAKER;
                }
                i5 = i7;
                i6 = 20;
            }
            if (i4 >= i6) {
                this.mAppIdleHistory.setAppStandbyBucket(str, i, j, i6, i5);
                maybeInformListeners(str, i, j, i6, i5, false);
            }
        }
    }

    private boolean predictionTimedOut(AppIdleHistory.AppUsageHistory appUsageHistory, long j) {
        return appUsageHistory.lastPredictedTime > 0 && this.mAppIdleHistory.getElapsedTime(j) - appUsageHistory.lastPredictedTime > this.mPredictionTimeoutMillis;
    }

    private void maybeInformListeners(String str, int i, long j, int i2, int i3, boolean z) {
        synchronized (this.mAppIdleLock) {
            if (this.mAppIdleHistory.shouldInformListeners(str, i, j, i2)) {
                this.mHandler.sendMessage(this.mHandler.obtainMessage(3, StandbyUpdateRecord.obtain(str, i, i2, i3, z)));
            }
        }
    }

    @GuardedBy("mAppIdleLock")
    int getBucketForLocked(String str, int i, long j) {
        return THRESHOLD_BUCKETS[this.mAppIdleHistory.getThresholdIndex(str, i, j, this.mAppStandbyScreenThresholds, this.mAppStandbyElapsedThresholds)];
    }

    void checkParoleTimeout() {
        boolean z;
        NetworkInfo activeNetworkInfo = this.mConnectivityManager.getActiveNetworkInfo();
        boolean z2 = false;
        boolean z3 = activeNetworkInfo != null && activeNetworkInfo.isConnected();
        synchronized (this.mAppIdleLock) {
            long jCurrentTimeMillis = this.mInjector.currentTimeMillis();
            if (this.mAppIdleTempParoled) {
                z = false;
            } else {
                long j = jCurrentTimeMillis - this.mLastAppIdleParoledTime;
                if (j <= this.mAppIdleParoleIntervalMillis) {
                    postNextParoleTimeout(jCurrentTimeMillis, false);
                    z = false;
                } else if (!z3 && j <= this.mAppIdleParoleIntervalMillis + this.mAppIdleParoleWindowMillis) {
                    postNextParoleTimeout(jCurrentTimeMillis, true);
                    z = false;
                    z2 = true;
                } else {
                    z = true;
                }
            }
        }
        if (z2) {
            this.mConnectivityManager.registerNetworkCallback(this.mNetworkRequest, this.mNetworkCallback);
        }
        if (z) {
            setAppIdleParoled(true);
        }
    }

    private void notifyBatteryStats(String str, int i, boolean z) {
        try {
            int packageUidAsUser = this.mPackageManager.getPackageUidAsUser(str, 8192, i);
            if (z) {
                this.mInjector.noteEvent(15, str, packageUidAsUser);
            } else {
                this.mInjector.noteEvent(16, str, packageUidAsUser);
            }
        } catch (PackageManager.NameNotFoundException | RemoteException e) {
        }
    }

    void onDeviceIdleModeChanged() {
        boolean z;
        boolean zIsDeviceIdleMode = this.mPowerManager.isDeviceIdleMode();
        synchronized (this.mAppIdleLock) {
            long jCurrentTimeMillis = this.mInjector.currentTimeMillis() - this.mLastAppIdleParoledTime;
            if (!zIsDeviceIdleMode && jCurrentTimeMillis >= this.mAppIdleParoleIntervalMillis) {
                z = true;
            } else if (!zIsDeviceIdleMode) {
                return;
            } else {
                z = false;
            }
            setAppIdleParoled(z);
        }
    }

    void reportEvent(UsageEvents.Event event, long j, int i) throws Throwable {
        Object obj;
        Object obj2;
        int i2;
        int i3;
        int i4;
        long j2;
        Object obj3;
        if (!this.mAppIdleEnabled) {
            return;
        }
        Object obj4 = this.mAppIdleLock;
        synchronized (obj4) {
            try {
                try {
                    boolean zIsIdle = this.mAppIdleHistory.isIdle(event.mPackage, i, j);
                    if (event.mEventType == 1 || event.mEventType == 2 || event.mEventType == 6 || event.mEventType == 7 || event.mEventType == 10 || event.mEventType == 14 || event.mEventType == 13) {
                        AppIdleHistory.AppUsageHistory appUsageHistory = this.mAppIdleHistory.getAppUsageHistory(event.mPackage, i, j);
                        int i5 = appUsageHistory.currentBucket;
                        int i6 = appUsageHistory.bucketingReason;
                        int iUsageEventToSubReason = usageEventToSubReason(event.mEventType);
                        int i7 = 768 | iUsageEventToSubReason;
                        try {
                            if (event.mEventType == 10 || event.mEventType == 14) {
                                obj2 = obj4;
                                i2 = i6;
                                i3 = i5;
                                i4 = 10;
                                this.mAppIdleHistory.reportUsage(appUsageHistory, event.mPackage, 20, iUsageEventToSubReason, 0L, j + this.mNotificationSeenTimeoutMillis);
                                j2 = this.mNotificationSeenTimeoutMillis;
                            } else if (event.mEventType == 6) {
                                this.mAppIdleHistory.reportUsage(appUsageHistory, event.mPackage, 10, iUsageEventToSubReason, 0L, j + this.mSystemInteractionTimeoutMillis);
                                j2 = this.mSystemInteractionTimeoutMillis;
                                obj2 = obj4;
                                i2 = i6;
                                i3 = i5;
                                i4 = 10;
                            } else {
                                obj2 = obj4;
                                i2 = i6;
                                i3 = i5;
                                i4 = 10;
                                this.mAppIdleHistory.reportUsage(appUsageHistory, event.mPackage, 10, iUsageEventToSubReason, j, j + this.mStrongUsageTimeoutMillis);
                                j2 = this.mStrongUsageTimeoutMillis;
                            }
                            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(11, i, -1, event.mPackage), j2);
                            obj3 = obj2;
                            maybeInformListeners(event.mPackage, i, j, appUsageHistory.currentBucket, i7, (appUsageHistory.currentBucket != i4 || i3 == appUsageHistory.currentBucket || (i2 & JobPackageTracker.EVENT_STOP_REASON_MASK) == 768) ? false : true);
                            if (zIsIdle) {
                                notifyBatteryStats(event.mPackage, i, false);
                            }
                        } catch (Throwable th) {
                            th = th;
                            obj = obj2;
                            throw th;
                        }
                    } else {
                        obj3 = obj4;
                    }
                } catch (Throwable th2) {
                    th = th2;
                    obj = obj4;
                }
            } catch (Throwable th3) {
                th = th3;
            }
        }
    }

    private int usageEventToSubReason(int i) {
        switch (i) {
            case 1:
                return 4;
            case 2:
                return 5;
            case 6:
                return 1;
            case 7:
                return 3;
            case 10:
                return 2;
            case 13:
                return 10;
            case 14:
                return 9;
            default:
                return 0;
        }
    }

    void forceIdleState(String str, int i, boolean z) {
        int appId;
        int idle;
        if (this.mAppIdleEnabled && (appId = getAppId(str)) >= 0) {
            long jElapsedRealtime = this.mInjector.elapsedRealtime();
            boolean zIsAppIdleFiltered = isAppIdleFiltered(str, appId, i, jElapsedRealtime);
            synchronized (this.mAppIdleLock) {
                idle = this.mAppIdleHistory.setIdle(str, i, z, jElapsedRealtime);
            }
            boolean zIsAppIdleFiltered2 = isAppIdleFiltered(str, appId, i, jElapsedRealtime);
            if (zIsAppIdleFiltered != zIsAppIdleFiltered2) {
                maybeInformListeners(str, i, jElapsedRealtime, idle, 1024, false);
                if (!zIsAppIdleFiltered2) {
                    notifyBatteryStats(str, i, z);
                }
            }
        }
    }

    public void setLastJobRunTime(String str, int i, long j) {
        synchronized (this.mAppIdleLock) {
            this.mAppIdleHistory.setLastJobRunTime(str, i, j);
        }
    }

    public long getTimeSinceLastJobRun(String str, int i) {
        long timeSinceLastJobRun;
        long jElapsedRealtime = this.mInjector.elapsedRealtime();
        synchronized (this.mAppIdleLock) {
            timeSinceLastJobRun = this.mAppIdleHistory.getTimeSinceLastJobRun(str, i, jElapsedRealtime);
        }
        return timeSinceLastJobRun;
    }

    public void onUserRemoved(int i) {
        synchronized (this.mAppIdleLock) {
            this.mAppIdleHistory.onUserRemoved(i);
            synchronized (this.mActiveAdminApps) {
                this.mActiveAdminApps.remove(i);
            }
        }
    }

    private boolean isAppIdleUnfiltered(String str, int i, long j) {
        boolean zIsIdle;
        synchronized (this.mAppIdleLock) {
            zIsIdle = this.mAppIdleHistory.isIdle(str, i, j);
        }
        return zIsIdle;
    }

    void addListener(UsageStatsManagerInternal.AppIdleStateChangeListener appIdleStateChangeListener) {
        synchronized (this.mPackageAccessListeners) {
            if (!this.mPackageAccessListeners.contains(appIdleStateChangeListener)) {
                this.mPackageAccessListeners.add(appIdleStateChangeListener);
            }
        }
    }

    void removeListener(UsageStatsManagerInternal.AppIdleStateChangeListener appIdleStateChangeListener) {
        synchronized (this.mPackageAccessListeners) {
            this.mPackageAccessListeners.remove(appIdleStateChangeListener);
        }
    }

    int getAppId(String str) {
        try {
            return this.mPackageManager.getApplicationInfo(str, 4194816).uid;
        } catch (PackageManager.NameNotFoundException e) {
            return -1;
        }
    }

    boolean isAppIdleFilteredOrParoled(String str, int i, long j, boolean z) {
        if (isParoledOrCharging()) {
            return false;
        }
        if (z && this.mInjector.isPackageEphemeral(i, str)) {
            return false;
        }
        return isAppIdleFiltered(str, getAppId(str), i, j);
    }

    boolean isAppSpecial(String str, int i, int i2) {
        if (str == null) {
            return false;
        }
        if (!this.mAppIdleEnabled || i < 10000 || str.equals(PackageManagerService.PLATFORM_PACKAGE_NAME)) {
            return true;
        }
        if (this.mSystemServicesReady) {
            try {
                if (this.mInjector.isPowerSaveWhitelistExceptIdleApp(str) || isActiveDeviceAdmin(str, i2) || isActiveNetworkScorer(str)) {
                    return true;
                }
                if ((this.mAppWidgetManager != null && this.mInjector.isBoundWidgetPackage(this.mAppWidgetManager, str, i2)) || isDeviceProvisioningPackage(str)) {
                    return true;
                }
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return isCarrierApp(str);
    }

    boolean isAppIdleFiltered(String str, int i, int i2, long j) {
        if (isAppSpecial(str, i, i2)) {
            return false;
        }
        return isAppIdleUnfiltered(str, i2, j);
    }

    int[] getIdleUidsForUser(int i) {
        int i2 = 0;
        if (!this.mAppIdleEnabled) {
            return new int[0];
        }
        long jElapsedRealtime = this.mInjector.elapsedRealtime();
        try {
            ParceledListSlice installedApplications = AppGlobals.getPackageManager().getInstalledApplications(0, i);
            if (installedApplications == null) {
                return new int[0];
            }
            List list = installedApplications.getList();
            SparseIntArray sparseIntArray = new SparseIntArray();
            for (int size = list.size() - 1; size >= 0; size--) {
                ApplicationInfo applicationInfo = (ApplicationInfo) list.get(size);
                boolean zIsAppIdleFiltered = isAppIdleFiltered(applicationInfo.packageName, UserHandle.getAppId(applicationInfo.uid), i, jElapsedRealtime);
                int iIndexOfKey = sparseIntArray.indexOfKey(applicationInfo.uid);
                if (iIndexOfKey < 0) {
                    sparseIntArray.put(applicationInfo.uid, (zIsAppIdleFiltered ? 65536 : 0) + 1);
                } else {
                    sparseIntArray.setValueAt(iIndexOfKey, sparseIntArray.valueAt(iIndexOfKey) + 1 + (zIsAppIdleFiltered ? 65536 : 0));
                }
            }
            int i3 = 0;
            for (int size2 = sparseIntArray.size() - 1; size2 >= 0; size2--) {
                int iValueAt = sparseIntArray.valueAt(size2);
                if ((iValueAt & 32767) == (iValueAt >> 16)) {
                    i3++;
                }
            }
            int[] iArr = new int[i3];
            for (int size3 = sparseIntArray.size() - 1; size3 >= 0; size3--) {
                int iValueAt2 = sparseIntArray.valueAt(size3);
                if ((iValueAt2 & 32767) == (iValueAt2 >> 16)) {
                    iArr[i2] = sparseIntArray.keyAt(size3);
                    i2++;
                }
            }
            return iArr;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    void setAppIdleAsync(String str, boolean z, int i) {
        if (str == null || !this.mAppIdleEnabled) {
            return;
        }
        this.mHandler.obtainMessage(4, i, z ? 1 : 0, str).sendToTarget();
    }

    public int getAppStandbyBucket(String str, int i, long j, boolean z) {
        int appStandbyBucket;
        if (this.mAppIdleEnabled) {
            if (z && this.mInjector.isPackageEphemeral(i, str)) {
                return 10;
            }
            synchronized (this.mAppIdleLock) {
                appStandbyBucket = this.mAppIdleHistory.getAppStandbyBucket(str, i, j);
            }
            return appStandbyBucket;
        }
        return 10;
    }

    public List<AppStandbyInfo> getAppStandbyBuckets(int i) {
        ArrayList<AppStandbyInfo> appStandbyBuckets;
        synchronized (this.mAppIdleLock) {
            appStandbyBuckets = this.mAppIdleHistory.getAppStandbyBuckets(i, this.mAppIdleEnabled);
        }
        return appStandbyBuckets;
    }

    void setAppStandbyBucket(String str, int i, int i2, int i3, long j) {
        setAppStandbyBucket(str, i, i2, i3, j, false);
    }

    void setAppStandbyBucket(String str, int i, int i2, int i3, long j, boolean z) {
        int i4;
        int i5;
        int i6;
        synchronized (this.mAppIdleLock) {
            AppIdleHistory.AppUsageHistory appUsageHistory = this.mAppIdleHistory.getAppUsageHistory(str, i, j);
            boolean z2 = (i3 & JobPackageTracker.EVENT_STOP_REASON_MASK) == 1280;
            if (appUsageHistory.currentBucket < 10) {
                return;
            }
            if ((appUsageHistory.currentBucket == 50 || i2 == 50) && z2) {
                return;
            }
            if ((65280 & appUsageHistory.bucketingReason) == 1024 && z2) {
                return;
            }
            if (z2) {
                long elapsedTime = this.mAppIdleHistory.getElapsedTime(j);
                this.mAppIdleHistory.updateLastPrediction(appUsageHistory, elapsedTime, i2);
                if (i2 > 10 && appUsageHistory.bucketActiveTimeoutTime > elapsedTime) {
                    i4 = appUsageHistory.bucketingReason;
                    i5 = 10;
                } else if (i2 > 20 && appUsageHistory.bucketWorkingSetTimeoutTime > elapsedTime) {
                    if (appUsageHistory.currentBucket != 20) {
                        i6 = UsbTerminalTypes.TERMINAL_OUT_LFSPEAKER;
                    } else {
                        i6 = appUsageHistory.bucketingReason;
                    }
                    i4 = i6;
                    i5 = 20;
                } else {
                    i4 = i3;
                    i5 = i2;
                }
            }
            this.mAppIdleHistory.setAppStandbyBucket(str, i, j, i5, i4, z);
            maybeInformListeners(str, i, j, i5, i4, false);
        }
    }

    @VisibleForTesting
    boolean isActiveDeviceAdmin(String str, int i) {
        boolean z;
        synchronized (this.mActiveAdminApps) {
            Set<String> set = this.mActiveAdminApps.get(i);
            z = set != null && set.contains(str);
        }
        return z;
    }

    public void addActiveDeviceAdmin(String str, int i) {
        synchronized (this.mActiveAdminApps) {
            Set<String> arraySet = this.mActiveAdminApps.get(i);
            if (arraySet == null) {
                arraySet = new ArraySet<>();
                this.mActiveAdminApps.put(i, arraySet);
            }
            arraySet.add(str);
        }
    }

    public void setActiveAdminApps(Set<String> set, int i) {
        synchronized (this.mActiveAdminApps) {
            try {
                if (set == null) {
                    this.mActiveAdminApps.remove(i);
                } else {
                    this.mActiveAdminApps.put(i, set);
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public void onAdminDataAvailable() {
        this.mAdminDataAvailableLatch.countDown();
    }

    private void waitForAdminData() {
        if (this.mContext.getPackageManager().hasSystemFeature("android.software.device_admin")) {
            ConcurrentUtils.waitForCountDownNoInterrupt(this.mAdminDataAvailableLatch, 10000L, "Wait for admin data");
        }
    }

    Set<String> getActiveAdminAppsForTest(int i) {
        Set<String> set;
        synchronized (this.mActiveAdminApps) {
            set = this.mActiveAdminApps.get(i);
        }
        return set;
    }

    private boolean isDeviceProvisioningPackage(String str) {
        String string = this.mContext.getResources().getString(R.string.activity_resolver_use_once);
        return string != null && string.equals(str);
    }

    private boolean isCarrierApp(String str) {
        synchronized (this.mAppIdleLock) {
            if (!this.mHaveCarrierPrivilegedApps) {
                fetchCarrierPrivilegedAppsLocked();
            }
            if (this.mCarrierPrivilegedApps != null) {
                return this.mCarrierPrivilegedApps.contains(str);
            }
            return false;
        }
    }

    void clearCarrierPrivilegedApps() {
        synchronized (this.mAppIdleLock) {
            this.mHaveCarrierPrivilegedApps = false;
            this.mCarrierPrivilegedApps = null;
        }
    }

    @GuardedBy("mAppIdleLock")
    private void fetchCarrierPrivilegedAppsLocked() {
        this.mCarrierPrivilegedApps = ((TelephonyManager) this.mContext.getSystemService(TelephonyManager.class)).getPackagesWithCarrierPrivileges();
        this.mHaveCarrierPrivilegedApps = true;
    }

    private boolean isActiveNetworkScorer(String str) {
        return str != null && str.equals(this.mInjector.getActiveNetworkScorer());
    }

    void informListeners(String str, int i, int i2, int i3, boolean z) {
        boolean z2 = i2 >= 40;
        synchronized (this.mPackageAccessListeners) {
            for (UsageStatsManagerInternal.AppIdleStateChangeListener appIdleStateChangeListener : this.mPackageAccessListeners) {
                appIdleStateChangeListener.onAppIdleStateChanged(str, i, z2, i2, i3);
                if (z) {
                    appIdleStateChangeListener.onUserInteractionStarted(str, i);
                }
            }
        }
    }

    void informParoleStateChanged() {
        boolean zIsParoledOrCharging = isParoledOrCharging();
        synchronized (this.mPackageAccessListeners) {
            Iterator<UsageStatsManagerInternal.AppIdleStateChangeListener> it = this.mPackageAccessListeners.iterator();
            while (it.hasNext()) {
                it.next().onParoleStateChanged(zIsParoledOrCharging);
            }
        }
    }

    void flushToDisk(int i) {
        synchronized (this.mAppIdleLock) {
            this.mAppIdleHistory.writeAppIdleTimes(i);
        }
    }

    void flushDurationsToDisk() {
        synchronized (this.mAppIdleLock) {
            this.mAppIdleHistory.writeAppIdleDurations();
        }
    }

    boolean isDisplayOn() {
        return this.mInjector.isDefaultDisplayOn();
    }

    void clearAppIdleForPackage(String str, int i) {
        synchronized (this.mAppIdleLock) {
            this.mAppIdleHistory.clearUsage(str, i);
        }
    }

    private class PackageReceiver extends BroadcastReceiver {
        private PackageReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.PACKAGE_ADDED".equals(action) || "android.intent.action.PACKAGE_CHANGED".equals(action)) {
                AppStandbyController.this.clearCarrierPrivilegedApps();
            }
            if (("android.intent.action.PACKAGE_REMOVED".equals(action) || "android.intent.action.PACKAGE_ADDED".equals(action)) && !intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                AppStandbyController.this.clearAppIdleForPackage(intent.getData().getSchemeSpecificPart(), getSendingUserId());
            }
        }
    }

    void initializeDefaultsForSystemApps(int i) {
        int i2;
        if (this.mSystemServicesReady) {
            Slog.d(TAG, "Initializing defaults for system apps on user " + i + ", appIdleEnabled=" + this.mAppIdleEnabled);
            long jElapsedRealtime = this.mInjector.elapsedRealtime();
            List installedPackagesAsUser = this.mPackageManager.getInstalledPackagesAsUser(512, i);
            int size = installedPackagesAsUser.size();
            synchronized (this.mAppIdleLock) {
                int i3 = 0;
                while (i3 < size) {
                    try {
                        PackageInfo packageInfo = (PackageInfo) installedPackagesAsUser.get(i3);
                        String str = packageInfo.packageName;
                        if (packageInfo.applicationInfo != null && packageInfo.applicationInfo.isSystemApp()) {
                            i2 = i3;
                            this.mAppIdleHistory.reportUsage(str, i, 10, 6, 0L, jElapsedRealtime + this.mSystemUpdateUsageTimeoutMillis);
                        } else {
                            i2 = i3;
                        }
                        i3 = i2 + 1;
                    } catch (Throwable th) {
                        throw th;
                    }
                }
            }
            return;
        }
        this.mPendingInitializeDefaults = true;
    }

    void postReportContentProviderUsage(String str, String str2, int i) {
        SomeArgs someArgsObtain = SomeArgs.obtain();
        someArgsObtain.arg1 = str;
        someArgsObtain.arg2 = str2;
        someArgsObtain.arg3 = Integer.valueOf(i);
        this.mHandler.obtainMessage(8, someArgsObtain).sendToTarget();
    }

    void postReportExemptedSyncScheduled(String str, int i) {
        this.mHandler.obtainMessage(12, i, 0, str).sendToTarget();
    }

    void postReportExemptedSyncStart(String str, int i) {
        this.mHandler.obtainMessage(13, i, 0, str).sendToTarget();
    }

    void dumpUser(IndentingPrintWriter indentingPrintWriter, int i, String str) {
        synchronized (this.mAppIdleLock) {
            this.mAppIdleHistory.dump(indentingPrintWriter, i, str);
        }
    }

    void dumpState(String[] strArr, PrintWriter printWriter) {
        synchronized (this.mAppIdleLock) {
            printWriter.println("Carrier privileged apps (have=" + this.mHaveCarrierPrivilegedApps + "): " + this.mCarrierPrivilegedApps);
        }
        printWriter.println();
        printWriter.println("Settings:");
        printWriter.print("  mCheckIdleIntervalMillis=");
        TimeUtils.formatDuration(this.mCheckIdleIntervalMillis, printWriter);
        printWriter.println();
        printWriter.print("  mAppIdleParoleIntervalMillis=");
        TimeUtils.formatDuration(this.mAppIdleParoleIntervalMillis, printWriter);
        printWriter.println();
        printWriter.print("  mAppIdleParoleWindowMillis=");
        TimeUtils.formatDuration(this.mAppIdleParoleWindowMillis, printWriter);
        printWriter.println();
        printWriter.print("  mAppIdleParoleDurationMillis=");
        TimeUtils.formatDuration(this.mAppIdleParoleDurationMillis, printWriter);
        printWriter.println();
        printWriter.print("  mExemptedSyncScheduledNonDozeTimeoutMillis=");
        TimeUtils.formatDuration(this.mExemptedSyncScheduledNonDozeTimeoutMillis, printWriter);
        printWriter.println();
        printWriter.print("  mExemptedSyncScheduledDozeTimeoutMillis=");
        TimeUtils.formatDuration(this.mExemptedSyncScheduledDozeTimeoutMillis, printWriter);
        printWriter.println();
        printWriter.print("  mExemptedSyncStartTimeoutMillis=");
        TimeUtils.formatDuration(this.mExemptedSyncStartTimeoutMillis, printWriter);
        printWriter.println();
        printWriter.println();
        printWriter.print("mAppIdleEnabled=");
        printWriter.print(this.mAppIdleEnabled);
        printWriter.print(" mAppIdleTempParoled=");
        printWriter.print(this.mAppIdleTempParoled);
        printWriter.print(" mCharging=");
        printWriter.print(this.mCharging);
        printWriter.print(" mChargingStable=");
        printWriter.print(this.mChargingStable);
        printWriter.print(" mLastAppIdleParoledTime=");
        TimeUtils.formatDuration(this.mLastAppIdleParoledTime, printWriter);
        printWriter.println();
        printWriter.print("mScreenThresholds=");
        printWriter.println(Arrays.toString(this.mAppStandbyScreenThresholds));
        printWriter.print("mElapsedThresholds=");
        printWriter.println(Arrays.toString(this.mAppStandbyElapsedThresholds));
        printWriter.print("mStableChargingThresholdMillis=");
        TimeUtils.formatDuration(this.mStableChargingThresholdMillis, printWriter);
        printWriter.println();
    }

    static class Injector {
        private IBatteryStats mBatteryStats;
        int mBootPhase;
        private final Context mContext;
        private IDeviceIdleController mDeviceIdleController;
        private DisplayManager mDisplayManager;
        private final Looper mLooper;
        private PackageManagerInternal mPackageManagerInternal;
        private PowerManager mPowerManager;

        Injector(Context context, Looper looper) {
            this.mContext = context;
            this.mLooper = looper;
        }

        Context getContext() {
            return this.mContext;
        }

        Looper getLooper() {
            return this.mLooper;
        }

        void onBootPhase(int i) {
            if (i == 500) {
                this.mDeviceIdleController = IDeviceIdleController.Stub.asInterface(ServiceManager.getService("deviceidle"));
                this.mBatteryStats = IBatteryStats.Stub.asInterface(ServiceManager.getService("batterystats"));
                this.mPackageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
                this.mDisplayManager = (DisplayManager) this.mContext.getSystemService("display");
                this.mPowerManager = (PowerManager) this.mContext.getSystemService(PowerManager.class);
            }
            this.mBootPhase = i;
        }

        int getBootPhase() {
            return this.mBootPhase;
        }

        long elapsedRealtime() {
            return SystemClock.elapsedRealtime();
        }

        long currentTimeMillis() {
            return System.currentTimeMillis();
        }

        boolean isAppIdleEnabled() {
            return this.mContext.getResources().getBoolean(R.^attr-private.floatingToolbarItemBackgroundDrawable) && (Settings.Global.getInt(this.mContext.getContentResolver(), "app_standby_enabled", 1) == 1 && Settings.Global.getInt(this.mContext.getContentResolver(), "adaptive_battery_management_enabled", 1) == 1);
        }

        boolean isCharging() {
            return ((BatteryManager) this.mContext.getSystemService(BatteryManager.class)).isCharging();
        }

        boolean isPowerSaveWhitelistExceptIdleApp(String str) throws RemoteException {
            return this.mDeviceIdleController.isPowerSaveWhitelistExceptIdleApp(str);
        }

        File getDataSystemDirectory() {
            return Environment.getDataSystemDirectory();
        }

        void noteEvent(int i, String str, int i2) throws RemoteException {
            this.mBatteryStats.noteEvent(i, str, i2);
        }

        boolean isPackageEphemeral(int i, String str) {
            return this.mPackageManagerInternal.isPackageEphemeral(i, str);
        }

        int[] getRunningUserIds() throws RemoteException {
            return ActivityManager.getService().getRunningUserIds();
        }

        boolean isDefaultDisplayOn() {
            return this.mDisplayManager.getDisplay(0).getState() == 2;
        }

        void registerDisplayListener(DisplayManager.DisplayListener displayListener, Handler handler) {
            this.mDisplayManager.registerDisplayListener(displayListener, handler);
        }

        String getActiveNetworkScorer() {
            return ((NetworkScoreManager) this.mContext.getSystemService("network_score")).getActiveScorerPackage();
        }

        public boolean isBoundWidgetPackage(AppWidgetManager appWidgetManager, String str, int i) {
            return appWidgetManager.isBoundWidgetPackage(str, i);
        }

        String getAppIdleSettings() {
            return Settings.Global.getString(this.mContext.getContentResolver(), "app_idle_constants");
        }

        public boolean isDeviceIdleMode() {
            return this.mPowerManager.isDeviceIdleMode();
        }
    }

    class AppStandbyHandler extends Handler {
        AppStandbyHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) throws Throwable {
            switch (message.what) {
                case 3:
                    StandbyUpdateRecord standbyUpdateRecord = (StandbyUpdateRecord) message.obj;
                    AppStandbyController.this.informListeners(standbyUpdateRecord.packageName, standbyUpdateRecord.userId, standbyUpdateRecord.bucket, standbyUpdateRecord.reason, standbyUpdateRecord.isUserInteraction);
                    standbyUpdateRecord.recycle();
                    break;
                case 4:
                    AppStandbyController.this.forceIdleState((String) message.obj, message.arg1, message.arg2 == 1);
                    break;
                case 5:
                    if (AppStandbyController.this.checkIdleStates(message.arg1) && AppStandbyController.this.mAppIdleEnabled) {
                        AppStandbyController.this.mHandler.sendMessageDelayed(AppStandbyController.this.mHandler.obtainMessage(5, message.arg1, 0), AppStandbyController.this.mCheckIdleIntervalMillis);
                        break;
                    }
                    break;
                case 6:
                    AppStandbyController.this.checkParoleTimeout();
                    break;
                case 7:
                    AppStandbyController.this.setAppIdleParoled(false);
                    break;
                case 8:
                    SomeArgs someArgs = (SomeArgs) message.obj;
                    AppStandbyController.this.reportContentProviderUsage((String) someArgs.arg1, (String) someArgs.arg2, ((Integer) someArgs.arg3).intValue());
                    someArgs.recycle();
                    break;
                case 9:
                    AppStandbyController.this.informParoleStateChanged();
                    break;
                case 10:
                    AppStandbyController.this.mHandler.removeMessages(10);
                    AppStandbyController.this.waitForAdminData();
                    AppStandbyController.this.checkIdleStates(-1);
                    break;
                case 11:
                    AppStandbyController.this.checkAndUpdateStandbyState((String) message.obj, message.arg1, message.arg2, AppStandbyController.this.mInjector.elapsedRealtime());
                    break;
                case 12:
                    AppStandbyController.this.reportExemptedSyncScheduled((String) message.obj, message.arg1);
                    break;
                case 13:
                    AppStandbyController.this.reportExemptedSyncStart((String) message.obj, message.arg1);
                    break;
                case 14:
                    AppStandbyController.this.updateChargingStableState();
                    break;
                default:
                    super.handleMessage(message);
                    break;
            }
        }
    }

    private class DeviceStateReceiver extends BroadcastReceiver {
        private DeviceStateReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            byte b;
            String action = intent.getAction();
            int iHashCode = action.hashCode();
            if (iHashCode != -54942926) {
                if (iHashCode != 870701415) {
                    b = (iHashCode == 948344062 && action.equals("android.os.action.CHARGING")) ? (byte) 0 : (byte) -1;
                } else if (action.equals("android.os.action.DEVICE_IDLE_MODE_CHANGED")) {
                    b = 2;
                }
            } else if (action.equals("android.os.action.DISCHARGING")) {
                b = 1;
            }
            switch (b) {
                case 0:
                    AppStandbyController.this.setChargingState(true);
                    break;
                case 1:
                    AppStandbyController.this.setChargingState(false);
                    break;
                case 2:
                    AppStandbyController.this.onDeviceIdleModeChanged();
                    break;
            }
        }
    }

    private class SettingsObserver extends ContentObserver {
        public static final long DEFAULT_EXEMPTED_SYNC_SCHEDULED_DOZE_TIMEOUT = 14400000;
        public static final long DEFAULT_EXEMPTED_SYNC_SCHEDULED_NON_DOZE_TIMEOUT = 600000;
        public static final long DEFAULT_EXEMPTED_SYNC_START_TIMEOUT = 600000;
        public static final long DEFAULT_NOTIFICATION_TIMEOUT = 43200000;
        public static final long DEFAULT_STABLE_CHARGING_THRESHOLD = 600000;
        public static final long DEFAULT_STRONG_USAGE_TIMEOUT = 3600000;
        public static final long DEFAULT_SYNC_ADAPTER_TIMEOUT = 600000;
        public static final long DEFAULT_SYSTEM_INTERACTION_TIMEOUT = 600000;
        public static final long DEFAULT_SYSTEM_UPDATE_TIMEOUT = 7200000;
        private static final String KEY_ELAPSED_TIME_THRESHOLDS = "elapsed_thresholds";
        private static final String KEY_EXEMPTED_SYNC_SCHEDULED_DOZE_HOLD_DURATION = "exempted_sync_scheduled_d_duration";
        private static final String KEY_EXEMPTED_SYNC_SCHEDULED_NON_DOZE_HOLD_DURATION = "exempted_sync_scheduled_nd_duration";
        private static final String KEY_EXEMPTED_SYNC_START_HOLD_DURATION = "exempted_sync_start_duration";

        @Deprecated
        private static final String KEY_IDLE_DURATION = "idle_duration2";

        @Deprecated
        private static final String KEY_IDLE_DURATION_OLD = "idle_duration";
        private static final String KEY_NOTIFICATION_SEEN_HOLD_DURATION = "notification_seen_duration";
        private static final String KEY_PAROLE_DURATION = "parole_duration";
        private static final String KEY_PAROLE_INTERVAL = "parole_interval";
        private static final String KEY_PAROLE_WINDOW = "parole_window";
        private static final String KEY_PREDICTION_TIMEOUT = "prediction_timeout";
        private static final String KEY_SCREEN_TIME_THRESHOLDS = "screen_thresholds";
        private static final String KEY_STABLE_CHARGING_THRESHOLD = "stable_charging_threshold";
        private static final String KEY_STRONG_USAGE_HOLD_DURATION = "strong_usage_duration";
        private static final String KEY_SYNC_ADAPTER_HOLD_DURATION = "sync_adapter_duration";
        private static final String KEY_SYSTEM_INTERACTION_HOLD_DURATION = "system_interaction_duration";
        private static final String KEY_SYSTEM_UPDATE_HOLD_DURATION = "system_update_usage_duration";

        @Deprecated
        private static final String KEY_WALLCLOCK_THRESHOLD = "wallclock_threshold";
        private final KeyValueListParser mParser;

        SettingsObserver(Handler handler) {
            super(handler);
            this.mParser = new KeyValueListParser(',');
        }

        void registerObserver() {
            ContentResolver contentResolver = AppStandbyController.this.mContext.getContentResolver();
            contentResolver.registerContentObserver(Settings.Global.getUriFor("app_idle_constants"), false, this);
            contentResolver.registerContentObserver(Settings.Global.getUriFor("app_standby_enabled"), false, this);
            contentResolver.registerContentObserver(Settings.Global.getUriFor("adaptive_battery_management_enabled"), false, this);
        }

        @Override
        public void onChange(boolean z) {
            updateSettings();
            AppStandbyController.this.postOneTimeCheckIdleStates();
        }

        void updateSettings() {
            try {
                this.mParser.setString(AppStandbyController.this.mInjector.getAppIdleSettings());
            } catch (IllegalArgumentException e) {
                Slog.e(AppStandbyController.TAG, "Bad value for app idle settings: " + e.getMessage());
            }
            synchronized (AppStandbyController.this.mAppIdleLock) {
                AppStandbyController.this.mAppIdleParoleIntervalMillis = this.mParser.getDurationMillis(KEY_PAROLE_INTERVAL, 86400000L);
                AppStandbyController.this.mAppIdleParoleWindowMillis = this.mParser.getDurationMillis(KEY_PAROLE_WINDOW, DEFAULT_SYSTEM_UPDATE_TIMEOUT);
                AppStandbyController.this.mAppIdleParoleDurationMillis = this.mParser.getDurationMillis(KEY_PAROLE_DURATION, 600000L);
                AppStandbyController.this.mAppStandbyScreenThresholds = parseLongArray(this.mParser.getString(KEY_SCREEN_TIME_THRESHOLDS, (String) null), AppStandbyController.SCREEN_TIME_THRESHOLDS);
                AppStandbyController.this.mAppStandbyElapsedThresholds = parseLongArray(this.mParser.getString(KEY_ELAPSED_TIME_THRESHOLDS, (String) null), AppStandbyController.ELAPSED_TIME_THRESHOLDS);
                AppStandbyController.this.mCheckIdleIntervalMillis = Math.min(AppStandbyController.this.mAppStandbyElapsedThresholds[1] / 4, 14400000L);
                AppStandbyController.this.mStrongUsageTimeoutMillis = this.mParser.getDurationMillis(KEY_STRONG_USAGE_HOLD_DURATION, 3600000L);
                AppStandbyController.this.mNotificationSeenTimeoutMillis = this.mParser.getDurationMillis(KEY_NOTIFICATION_SEEN_HOLD_DURATION, 43200000L);
                AppStandbyController.this.mSystemUpdateUsageTimeoutMillis = this.mParser.getDurationMillis(KEY_SYSTEM_UPDATE_HOLD_DURATION, DEFAULT_SYSTEM_UPDATE_TIMEOUT);
                AppStandbyController.this.mPredictionTimeoutMillis = this.mParser.getDurationMillis(KEY_PREDICTION_TIMEOUT, 43200000L);
                AppStandbyController.this.mSyncAdapterTimeoutMillis = this.mParser.getDurationMillis(KEY_SYNC_ADAPTER_HOLD_DURATION, 600000L);
                AppStandbyController.this.mExemptedSyncScheduledNonDozeTimeoutMillis = this.mParser.getDurationMillis(KEY_EXEMPTED_SYNC_SCHEDULED_NON_DOZE_HOLD_DURATION, 600000L);
                AppStandbyController.this.mExemptedSyncScheduledDozeTimeoutMillis = this.mParser.getDurationMillis(KEY_EXEMPTED_SYNC_SCHEDULED_DOZE_HOLD_DURATION, 14400000L);
                AppStandbyController.this.mExemptedSyncStartTimeoutMillis = this.mParser.getDurationMillis(KEY_EXEMPTED_SYNC_START_HOLD_DURATION, 600000L);
                AppStandbyController.this.mSystemInteractionTimeoutMillis = this.mParser.getDurationMillis(KEY_SYSTEM_INTERACTION_HOLD_DURATION, 600000L);
                AppStandbyController.this.mStableChargingThresholdMillis = this.mParser.getDurationMillis(KEY_STABLE_CHARGING_THRESHOLD, 600000L);
            }
            AppStandbyController.this.setAppIdleEnabled(AppStandbyController.this.mInjector.isAppIdleEnabled());
        }

        long[] parseLongArray(String str, long[] jArr) {
            if (str == null || str.isEmpty()) {
                return jArr;
            }
            String[] strArrSplit = str.split(SliceClientPermissions.SliceAuthority.DELIMITER);
            if (strArrSplit.length == AppStandbyController.THRESHOLD_BUCKETS.length) {
                long[] jArr2 = new long[AppStandbyController.THRESHOLD_BUCKETS.length];
                for (int i = 0; i < AppStandbyController.THRESHOLD_BUCKETS.length; i++) {
                    try {
                        if (strArrSplit[i].startsWith("P") || strArrSplit[i].startsWith("p")) {
                            jArr2[i] = Duration.parse(strArrSplit[i]).toMillis();
                        } else {
                            jArr2[i] = Long.parseLong(strArrSplit[i]);
                        }
                    } catch (NumberFormatException | DateTimeParseException e) {
                        return jArr;
                    }
                }
                return jArr2;
            }
            return jArr;
        }
    }
}
