package com.android.server.usage;

import android.R;
import android.app.ActivityManagerNative;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.admin.DevicePolicyManager;
import android.app.usage.ConfigurationStats;
import android.app.usage.IUsageStatsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManagerInternal;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.hardware.display.DisplayManager;
import android.net.NetworkScoreManager;
import android.os.BatteryManager;
import android.os.Binder;
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
import android.os.UserManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.ArraySet;
import android.util.KeyValueListParser;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.TimeUtils;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.app.IBatteryStats;
import com.android.internal.os.BackgroundThread;
import com.android.internal.os.SomeArgs;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.SystemService;
import com.android.server.location.LocationFudger;
import com.android.server.pm.PackageManagerService;
import com.android.server.usage.UserUsageStatsService;
import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UsageStatsService extends SystemService implements UserUsageStatsService.StatsUpdatedListener {
    static final boolean COMPRESS_TIME = false;
    static final boolean DEBUG = false;
    private static final long FLUSH_INTERVAL = 1200000;
    static final int MSG_CHECK_IDLE_STATES = 5;
    static final int MSG_CHECK_PAROLE_TIMEOUT = 6;
    static final int MSG_FLUSH_TO_DISK = 1;
    static final int MSG_FORCE_IDLE_STATE = 4;
    static final int MSG_INFORM_LISTENERS = 3;
    static final int MSG_ONE_TIME_CHECK_IDLE_STATES = 10;
    static final int MSG_PAROLE_END_TIMEOUT = 7;
    static final int MSG_PAROLE_STATE_CHANGED = 9;
    static final int MSG_REMOVE_USER = 2;
    static final int MSG_REPORT_CONTENT_PROVIDER_USAGE = 8;
    static final int MSG_REPORT_EVENT = 0;
    private static final long ONE_MINUTE = 60000;
    static final String TAG = "UsageStatsService";
    private static final long TEN_SECONDS = 10000;
    private static final long TIME_CHANGE_THRESHOLD_MILLIS = 2000;
    private static final long TWENTY_MINUTES = 1200000;
    boolean mAppIdleEnabled;

    @GuardedBy("mLock")
    private AppIdleHistory mAppIdleHistory;
    long mAppIdleParoleDurationMillis;
    long mAppIdleParoleIntervalMillis;
    boolean mAppIdleParoled;
    long mAppIdleScreenThresholdMillis;
    long mAppIdleWallclockThresholdMillis;
    AppOpsManager mAppOps;
    AppWidgetManager mAppWidgetManager;
    private IBatteryStats mBatteryStats;
    private List<String> mCarrierPrivilegedApps;
    long mCheckIdleIntervalMillis;
    IDeviceIdleController mDeviceIdleController;
    private final DisplayManager.DisplayListener mDisplayListener;
    private DisplayManager mDisplayManager;
    Handler mHandler;
    private boolean mHaveCarrierPrivilegedApps;
    private long mLastAppIdleParoledTime;
    private final Object mLock;
    private ArrayList<UsageStatsManagerInternal.AppIdleStateChangeListener> mPackageAccessListeners;
    PackageManager mPackageManager;
    private volatile boolean mPendingOneTimeCheckIdleStates;
    private PowerManager mPowerManager;
    long mRealTimeSnapshot;
    private boolean mScreenOn;
    private boolean mSystemServicesReady;
    long mSystemTimeSnapshot;
    private File mUsageStatsDir;
    UserManager mUserManager;
    private final SparseArray<UserUsageStatsService> mUserState;

    public UsageStatsService(Context context) {
        super(context);
        this.mLock = new Object();
        this.mUserState = new SparseArray<>();
        this.mSystemServicesReady = false;
        this.mPackageAccessListeners = new ArrayList<>();
        this.mDisplayListener = new DisplayManager.DisplayListener() {
            @Override
            public void onDisplayAdded(int displayId) {
            }

            @Override
            public void onDisplayRemoved(int displayId) {
            }

            @Override
            public void onDisplayChanged(int displayId) {
                if (displayId != 0) {
                    return;
                }
                synchronized (UsageStatsService.this.mLock) {
                    UsageStatsService.this.mAppIdleHistory.updateDisplayLocked(UsageStatsService.this.isDisplayOn(), SystemClock.elapsedRealtime());
                }
            }
        };
    }

    @Override
    public void onStart() {
        UserActionsReceiver userActionsReceiver = null;
        Object[] objArr = 0;
        Object[] objArr2 = 0;
        Object[] objArr3 = 0;
        Object[] objArr4 = 0;
        this.mAppOps = (AppOpsManager) getContext().getSystemService("appops");
        this.mUserManager = (UserManager) getContext().getSystemService("user");
        this.mPackageManager = getContext().getPackageManager();
        this.mHandler = new H(BackgroundThread.get().getLooper());
        this.mUsageStatsDir = new File(new File(Environment.getDataDirectory(), "system"), "usagestats");
        this.mUsageStatsDir.mkdirs();
        if (!this.mUsageStatsDir.exists()) {
            throw new IllegalStateException("Usage stats directory does not exist: " + this.mUsageStatsDir.getAbsolutePath());
        }
        IntentFilter intentFilter = new IntentFilter("android.intent.action.USER_REMOVED");
        intentFilter.addAction("android.intent.action.USER_STARTED");
        getContext().registerReceiverAsUser(new UserActionsReceiver(this, userActionsReceiver), UserHandle.ALL, intentFilter, null, this.mHandler);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.intent.action.PACKAGE_ADDED");
        intentFilter2.addAction("android.intent.action.PACKAGE_CHANGED");
        intentFilter2.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter2.addDataScheme("package");
        getContext().registerReceiverAsUser(new PackageReceiver(this, objArr4 == true ? 1 : 0), UserHandle.ALL, intentFilter2, null, this.mHandler);
        this.mAppIdleEnabled = getContext().getResources().getBoolean(R.^attr-private.alwaysFocusable);
        if (this.mAppIdleEnabled) {
            IntentFilter intentFilter3 = new IntentFilter("android.os.action.CHARGING");
            intentFilter3.addAction("android.os.action.DISCHARGING");
            intentFilter3.addAction("android.os.action.DEVICE_IDLE_MODE_CHANGED");
            getContext().registerReceiver(new DeviceStateReceiver(this, objArr3 == true ? 1 : 0), intentFilter3);
        }
        synchronized (this.mLock) {
            cleanUpRemovedUsersLocked();
            this.mAppIdleHistory = new AppIdleHistory(SystemClock.elapsedRealtime());
        }
        this.mRealTimeSnapshot = SystemClock.elapsedRealtime();
        this.mSystemTimeSnapshot = System.currentTimeMillis();
        publishLocalService(UsageStatsManagerInternal.class, new LocalService(this, objArr2 == true ? 1 : 0));
        publishBinderService("usagestats", new BinderService(this, objArr == true ? 1 : 0));
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase == 500) {
            SettingsObserver settingsObserver = new SettingsObserver(this.mHandler);
            settingsObserver.registerObserver();
            settingsObserver.updateSettings();
            this.mAppWidgetManager = (AppWidgetManager) getContext().getSystemService(AppWidgetManager.class);
            this.mDeviceIdleController = IDeviceIdleController.Stub.asInterface(ServiceManager.getService("deviceidle"));
            this.mBatteryStats = IBatteryStats.Stub.asInterface(ServiceManager.getService("batterystats"));
            this.mDisplayManager = (DisplayManager) getContext().getSystemService("display");
            this.mPowerManager = (PowerManager) getContext().getSystemService(PowerManager.class);
            this.mDisplayManager.registerDisplayListener(this.mDisplayListener, this.mHandler);
            synchronized (this.mLock) {
                this.mAppIdleHistory.updateDisplayLocked(isDisplayOn(), SystemClock.elapsedRealtime());
            }
            if (this.mPendingOneTimeCheckIdleStates) {
                postOneTimeCheckIdleStates();
            }
            this.mSystemServicesReady = true;
            return;
        }
        if (phase != 1000) {
            return;
        }
        setAppIdleParoled(((BatteryManager) getContext().getSystemService(BatteryManager.class)).isCharging());
    }

    private boolean isDisplayOn() {
        return this.mDisplayManager.getDisplay(0).getState() == 2;
    }

    private class UserActionsReceiver extends BroadcastReceiver {
        UserActionsReceiver(UsageStatsService this$0, UserActionsReceiver userActionsReceiver) {
            this();
        }

        private UserActionsReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            int userId = intent.getIntExtra("android.intent.extra.user_handle", -1);
            String action = intent.getAction();
            if ("android.intent.action.USER_REMOVED".equals(action)) {
                if (userId < 0) {
                    return;
                }
                UsageStatsService.this.mHandler.obtainMessage(2, userId, 0).sendToTarget();
            } else {
                if (!"android.intent.action.USER_STARTED".equals(action) || userId < 0) {
                    return;
                }
                UsageStatsService.this.postCheckIdleStates(userId);
            }
        }
    }

    private class PackageReceiver extends BroadcastReceiver {
        PackageReceiver(UsageStatsService this$0, PackageReceiver packageReceiver) {
            this();
        }

        private PackageReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.PACKAGE_ADDED".equals(action) || "android.intent.action.PACKAGE_CHANGED".equals(action)) {
                UsageStatsService.this.clearCarrierPrivilegedApps();
            }
            if ((!"android.intent.action.PACKAGE_REMOVED".equals(action) && !"android.intent.action.PACKAGE_ADDED".equals(action)) || intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                return;
            }
            UsageStatsService.this.clearAppIdleForPackage(intent.getData().getSchemeSpecificPart(), getSendingUserId());
        }
    }

    private class DeviceStateReceiver extends BroadcastReceiver {
        DeviceStateReceiver(UsageStatsService this$0, DeviceStateReceiver deviceStateReceiver) {
            this();
        }

        private DeviceStateReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.os.action.CHARGING".equals(action) || "android.os.action.DISCHARGING".equals(action)) {
                UsageStatsService.this.setAppIdleParoled("android.os.action.CHARGING".equals(action));
            } else {
                if (!"android.os.action.DEVICE_IDLE_MODE_CHANGED".equals(action)) {
                    return;
                }
                UsageStatsService.this.onDeviceIdleModeChanged();
            }
        }
    }

    @Override
    public void onStatsUpdated() {
        this.mHandler.sendEmptyMessageDelayed(1, 1200000L);
    }

    @Override
    public void onStatsReloaded() {
        postOneTimeCheckIdleStates();
    }

    @Override
    public void onNewUpdate(int userId) {
        initializeDefaultsForSystemApps(userId);
    }

    private void initializeDefaultsForSystemApps(int userId) {
        Slog.d(TAG, "Initializing defaults for system apps on user " + userId);
        long elapsedRealtime = SystemClock.elapsedRealtime();
        List<PackageInfo> packages = this.mPackageManager.getInstalledPackagesAsUser(512, userId);
        int packageCount = packages.size();
        for (int i = 0; i < packageCount; i++) {
            PackageInfo pi = packages.get(i);
            String packageName = pi.packageName;
            if (pi.applicationInfo != null && pi.applicationInfo.isSystemApp()) {
                this.mAppIdleHistory.reportUsageLocked(packageName, userId, elapsedRealtime);
            }
        }
    }

    void clearAppIdleForPackage(String packageName, int userId) {
        synchronized (this.mLock) {
            this.mAppIdleHistory.clearUsageLocked(packageName, userId);
        }
    }

    private void cleanUpRemovedUsersLocked() {
        List<UserInfo> users = this.mUserManager.getUsers(true);
        if (users == null || users.size() == 0) {
            throw new IllegalStateException("There can't be no users");
        }
        ArraySet<String> toDelete = new ArraySet<>();
        String[] fileNames = this.mUsageStatsDir.list();
        if (fileNames == null) {
            return;
        }
        toDelete.addAll(Arrays.asList(fileNames));
        int userCount = users.size();
        for (int i = 0; i < userCount; i++) {
            UserInfo userInfo = users.get(i);
            toDelete.remove(Integer.toString(userInfo.id));
        }
        int deleteCount = toDelete.size();
        for (int i2 = 0; i2 < deleteCount; i2++) {
            deleteRecursively(new File(this.mUsageStatsDir, toDelete.valueAt(i2)));
        }
    }

    void setAppIdleParoled(boolean paroled) {
        synchronized (this.mLock) {
            if (this.mAppIdleParoled != paroled) {
                this.mAppIdleParoled = paroled;
                if (paroled) {
                    postParoleEndTimeout();
                } else {
                    this.mLastAppIdleParoledTime = checkAndGetTimeLocked();
                    postNextParoleTimeout();
                }
                postParoleStateChanged();
            }
        }
    }

    private void postNextParoleTimeout() {
        this.mHandler.removeMessages(6);
        long timeLeft = (this.mLastAppIdleParoledTime + this.mAppIdleParoleIntervalMillis) - checkAndGetTimeLocked();
        if (timeLeft < 0) {
            timeLeft = 0;
        }
        this.mHandler.sendEmptyMessageDelayed(6, timeLeft);
    }

    private void postParoleEndTimeout() {
        this.mHandler.removeMessages(7);
        this.mHandler.sendEmptyMessageDelayed(7, this.mAppIdleParoleDurationMillis);
    }

    private void postParoleStateChanged() {
        this.mHandler.removeMessages(9);
        this.mHandler.sendEmptyMessage(9);
    }

    void postCheckIdleStates(int userId) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(5, userId, 0));
    }

    void postOneTimeCheckIdleStates() {
        if (this.mDeviceIdleController == null) {
            this.mPendingOneTimeCheckIdleStates = true;
        } else {
            this.mHandler.sendEmptyMessage(10);
            this.mPendingOneTimeCheckIdleStates = false;
        }
    }

    boolean checkIdleStates(int checkUserId) {
        if (!this.mAppIdleEnabled) {
            return false;
        }
        try {
            int[] runningUserIds = ActivityManagerNative.getDefault().getRunningUserIds();
            if (checkUserId != -1) {
                if (!ArrayUtils.contains(runningUserIds, checkUserId)) {
                    return false;
                }
            }
            long elapsedRealtime = SystemClock.elapsedRealtime();
            for (int userId : runningUserIds) {
                if (checkUserId == -1 || checkUserId == userId) {
                    List<PackageInfo> packages = this.mPackageManager.getInstalledPackagesAsUser(512, userId);
                    int packageCount = packages.size();
                    for (int p = 0; p < packageCount; p++) {
                        PackageInfo pi = packages.get(p);
                        String packageName = pi.packageName;
                        boolean isIdle = isAppIdleFiltered(packageName, UserHandle.getAppId(pi.applicationInfo.uid), userId, elapsedRealtime);
                        this.mHandler.sendMessage(this.mHandler.obtainMessage(3, userId, isIdle ? 1 : 0, packageName));
                        if (isIdle) {
                            synchronized (this.mLock) {
                                this.mAppIdleHistory.setIdle(packageName, userId, elapsedRealtime);
                            }
                        }
                    }
                }
            }
            return true;
        } catch (RemoteException re) {
            throw re.rethrowFromSystemServer();
        }
    }

    void checkParoleTimeout() {
        synchronized (this.mLock) {
            if (!this.mAppIdleParoled) {
                long timeSinceLastParole = checkAndGetTimeLocked() - this.mLastAppIdleParoledTime;
                if (timeSinceLastParole > this.mAppIdleParoleIntervalMillis) {
                    setAppIdleParoled(true);
                } else {
                    postNextParoleTimeout();
                }
            }
        }
    }

    private void notifyBatteryStats(String packageName, int userId, boolean idle) {
        try {
            int uid = this.mPackageManager.getPackageUidAsUser(packageName, PackageManagerService.DumpState.DUMP_PREFERRED_XML, userId);
            if (idle) {
                this.mBatteryStats.noteEvent(15, packageName, uid);
            } else {
                this.mBatteryStats.noteEvent(16, packageName, uid);
            }
        } catch (PackageManager.NameNotFoundException | RemoteException e) {
        }
    }

    void onDeviceIdleModeChanged() {
        boolean deviceIdle = this.mPowerManager.isDeviceIdleMode();
        synchronized (this.mLock) {
            long timeSinceLastParole = checkAndGetTimeLocked() - this.mLastAppIdleParoledTime;
            if (!deviceIdle && timeSinceLastParole >= this.mAppIdleParoleIntervalMillis) {
                setAppIdleParoled(true);
            } else if (deviceIdle) {
                setAppIdleParoled(false);
            }
        }
    }

    private static void deleteRecursively(File f) {
        File[] files = f.listFiles();
        if (files != null) {
            for (File subFile : files) {
                deleteRecursively(subFile);
            }
        }
        if (f.delete()) {
            return;
        }
        Slog.e(TAG, "Failed to delete " + f);
    }

    private UserUsageStatsService getUserDataAndInitializeIfNeededLocked(int userId, long currentTimeMillis) {
        UserUsageStatsService service = this.mUserState.get(userId);
        if (service == null) {
            UserUsageStatsService service2 = new UserUsageStatsService(getContext(), userId, new File(this.mUsageStatsDir, Integer.toString(userId)), this);
            service2.init(currentTimeMillis);
            this.mUserState.put(userId, service2);
            return service2;
        }
        return service;
    }

    private long checkAndGetTimeLocked() {
        long actualSystemTime = System.currentTimeMillis();
        long actualRealtime = SystemClock.elapsedRealtime();
        long expectedSystemTime = (actualRealtime - this.mRealTimeSnapshot) + this.mSystemTimeSnapshot;
        long diffSystemTime = actualSystemTime - expectedSystemTime;
        if (Math.abs(diffSystemTime) > TIME_CHANGE_THRESHOLD_MILLIS) {
            Slog.i(TAG, "Time changed in UsageStats by " + (diffSystemTime / 1000) + " seconds");
            int userCount = this.mUserState.size();
            for (int i = 0; i < userCount; i++) {
                UserUsageStatsService service = this.mUserState.valueAt(i);
                service.onTimeChanged(expectedSystemTime, actualSystemTime);
            }
            this.mRealTimeSnapshot = actualRealtime;
            this.mSystemTimeSnapshot = actualSystemTime;
        }
        return actualSystemTime;
    }

    private void convertToSystemTimeLocked(UsageEvents.Event event) {
        event.mTimeStamp = Math.max(0L, event.mTimeStamp - this.mRealTimeSnapshot) + this.mSystemTimeSnapshot;
    }

    void shutdown() {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(0);
            flushToDiskLocked();
        }
    }

    void reportEvent(UsageEvents.Event event, int userId) {
        synchronized (this.mLock) {
            long timeNow = checkAndGetTimeLocked();
            long elapsedRealtime = SystemClock.elapsedRealtime();
            convertToSystemTimeLocked(event);
            UserUsageStatsService service = getUserDataAndInitializeIfNeededLocked(userId, timeNow);
            boolean previouslyIdle = this.mAppIdleHistory.isIdleLocked(event.mPackage, userId, elapsedRealtime);
            service.reportEvent(event);
            if (event.mEventType == 1 || event.mEventType == 2 || event.mEventType == 6 || event.mEventType == 7) {
                this.mAppIdleHistory.reportUsageLocked(event.mPackage, userId, elapsedRealtime);
                if (previouslyIdle) {
                    this.mHandler.sendMessage(this.mHandler.obtainMessage(3, userId, 0, event.mPackage));
                    notifyBatteryStats(event.mPackage, userId, false);
                }
            }
        }
    }

    void reportContentProviderUsage(String authority, String providerPkgName, int userId) {
        String[] packages = ContentResolver.getSyncAdapterPackagesForAuthorityAsUser(authority, userId);
        for (String packageName : packages) {
            try {
                PackageInfo pi = this.mPackageManager.getPackageInfoAsUser(packageName, PackageManagerService.DumpState.DUMP_DEXOPT, userId);
                if (pi != null && pi.applicationInfo != null && !packageName.equals(providerPkgName)) {
                    forceIdleState(packageName, userId, false);
                }
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
    }

    void forceIdleState(String packageName, int userId, boolean idle) {
        int appId = getAppId(packageName);
        if (appId < 0) {
            return;
        }
        synchronized (this.mLock) {
            long elapsedRealtime = SystemClock.elapsedRealtime();
            boolean previouslyIdle = isAppIdleFiltered(packageName, appId, userId, elapsedRealtime);
            this.mAppIdleHistory.setIdleLocked(packageName, userId, idle, elapsedRealtime);
            boolean stillIdle = isAppIdleFiltered(packageName, appId, userId, elapsedRealtime);
            if (previouslyIdle != stillIdle) {
                this.mHandler.sendMessage(this.mHandler.obtainMessage(3, userId, stillIdle ? 1 : 0, packageName));
                if (!stillIdle) {
                    notifyBatteryStats(packageName, userId, idle);
                }
            }
        }
    }

    void flushToDisk() {
        synchronized (this.mLock) {
            flushToDiskLocked();
        }
    }

    void onUserRemoved(int userId) {
        synchronized (this.mLock) {
            Slog.i(TAG, "Removing user " + userId + " and all data.");
            this.mUserState.remove(userId);
            this.mAppIdleHistory.onUserRemoved(userId);
            cleanUpRemovedUsersLocked();
        }
    }

    List<UsageStats> queryUsageStats(int userId, int bucketType, long beginTime, long endTime) {
        synchronized (this.mLock) {
            long timeNow = checkAndGetTimeLocked();
            if (!validRange(timeNow, beginTime, endTime)) {
                return null;
            }
            UserUsageStatsService service = getUserDataAndInitializeIfNeededLocked(userId, timeNow);
            return service.queryUsageStats(bucketType, beginTime, endTime);
        }
    }

    List<ConfigurationStats> queryConfigurationStats(int userId, int bucketType, long beginTime, long endTime) {
        synchronized (this.mLock) {
            long timeNow = checkAndGetTimeLocked();
            if (!validRange(timeNow, beginTime, endTime)) {
                return null;
            }
            UserUsageStatsService service = getUserDataAndInitializeIfNeededLocked(userId, timeNow);
            return service.queryConfigurationStats(bucketType, beginTime, endTime);
        }
    }

    UsageEvents queryEvents(int userId, long beginTime, long endTime) {
        synchronized (this.mLock) {
            long timeNow = checkAndGetTimeLocked();
            if (!validRange(timeNow, beginTime, endTime)) {
                return null;
            }
            UserUsageStatsService service = getUserDataAndInitializeIfNeededLocked(userId, timeNow);
            return service.queryEvents(beginTime, endTime);
        }
    }

    private boolean isAppIdleUnfiltered(String packageName, int userId, long elapsedRealtime) {
        boolean zIsIdleLocked;
        synchronized (this.mLock) {
            zIsIdleLocked = this.mAppIdleHistory.isIdleLocked(packageName, userId, elapsedRealtime);
        }
        return zIsIdleLocked;
    }

    void addListener(UsageStatsManagerInternal.AppIdleStateChangeListener listener) {
        synchronized (this.mLock) {
            if (!this.mPackageAccessListeners.contains(listener)) {
                this.mPackageAccessListeners.add(listener);
            }
        }
    }

    void removeListener(UsageStatsManagerInternal.AppIdleStateChangeListener listener) {
        synchronized (this.mLock) {
            this.mPackageAccessListeners.remove(listener);
        }
    }

    int getAppId(String packageName) {
        try {
            ApplicationInfo ai = this.mPackageManager.getApplicationInfo(packageName, 8704);
            return ai.uid;
        } catch (PackageManager.NameNotFoundException e) {
            return -1;
        }
    }

    boolean isAppIdleFilteredOrParoled(String packageName, int userId, long elapsedRealtime) {
        if (this.mAppIdleParoled) {
            return false;
        }
        return isAppIdleFiltered(packageName, getAppId(packageName), userId, elapsedRealtime);
    }

    private boolean isAppIdleFiltered(String packageName, int appId, int userId, long elapsedRealtime) {
        if (packageName == null || !this.mAppIdleEnabled || appId < 10000 || packageName.equals("android")) {
            return false;
        }
        if (this.mSystemServicesReady) {
            try {
                if (this.mDeviceIdleController.isPowerSaveWhitelistExceptIdleApp(packageName) || isActiveDeviceAdmin(packageName, userId) || isActiveNetworkScorer(packageName)) {
                    return false;
                }
                if (this.mAppWidgetManager != null && this.mAppWidgetManager.isBoundWidgetPackage(packageName, userId)) {
                    return false;
                }
            } catch (RemoteException re) {
                throw re.rethrowFromSystemServer();
            }
        }
        return isAppIdleUnfiltered(packageName, userId, elapsedRealtime) && !isCarrierApp(packageName);
    }

    int[] getIdleUidsForUser(int userId) {
        if (!this.mAppIdleEnabled) {
            return new int[0];
        }
        long elapsedRealtime = SystemClock.elapsedRealtime();
        try {
            ParceledListSlice<ApplicationInfo> slice = AppGlobals.getPackageManager().getInstalledApplications(0, userId);
            if (slice == null) {
                return new int[0];
            }
            List<ApplicationInfo> apps = slice.getList();
            SparseIntArray uidStates = new SparseIntArray();
            for (int i = apps.size() - 1; i >= 0; i--) {
                ApplicationInfo ai = apps.get(i);
                boolean idle = isAppIdleFiltered(ai.packageName, UserHandle.getAppId(ai.uid), userId, elapsedRealtime);
                int index = uidStates.indexOfKey(ai.uid);
                if (index < 0) {
                    uidStates.put(ai.uid, (idle ? PackageManagerService.DumpState.DUMP_INSTALLS : 0) + 1);
                } else {
                    uidStates.setValueAt(index, (idle ? PackageManagerService.DumpState.DUMP_INSTALLS : 0) + uidStates.valueAt(index) + 1);
                }
            }
            int numIdle = 0;
            for (int i2 = uidStates.size() - 1; i2 >= 0; i2--) {
                int value = uidStates.valueAt(i2);
                if ((value & 32767) == (value >> 16)) {
                    numIdle++;
                }
            }
            int[] res = new int[numIdle];
            int numIdle2 = 0;
            for (int i3 = uidStates.size() - 1; i3 >= 0; i3--) {
                int value2 = uidStates.valueAt(i3);
                if ((value2 & 32767) == (value2 >> 16)) {
                    res[numIdle2] = uidStates.keyAt(i3);
                    numIdle2++;
                }
            }
            return res;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    void setAppIdle(String packageName, boolean idle, int userId) {
        if (packageName == null) {
            return;
        }
        this.mHandler.obtainMessage(4, userId, idle ? 1 : 0, packageName).sendToTarget();
    }

    private boolean isActiveDeviceAdmin(String packageName, int userId) {
        DevicePolicyManager dpm = (DevicePolicyManager) getContext().getSystemService(DevicePolicyManager.class);
        if (dpm == null) {
            return false;
        }
        return dpm.packageHasActiveAdmins(packageName, userId);
    }

    private boolean isCarrierApp(String packageName) {
        synchronized (this.mLock) {
            if (!this.mHaveCarrierPrivilegedApps) {
                fetchCarrierPrivilegedAppsLocked();
            }
            if (this.mCarrierPrivilegedApps != null) {
                return this.mCarrierPrivilegedApps.contains(packageName);
            }
            return false;
        }
    }

    void clearCarrierPrivilegedApps() {
        synchronized (this.mLock) {
            this.mHaveCarrierPrivilegedApps = false;
            this.mCarrierPrivilegedApps = null;
        }
    }

    private void fetchCarrierPrivilegedAppsLocked() {
        TelephonyManager telephonyManager = (TelephonyManager) getContext().getSystemService(TelephonyManager.class);
        this.mCarrierPrivilegedApps = telephonyManager.getPackagesWithCarrierPrivileges();
        this.mHaveCarrierPrivilegedApps = true;
    }

    private boolean isActiveNetworkScorer(String packageName) {
        NetworkScoreManager nsm = (NetworkScoreManager) getContext().getSystemService("network_score");
        if (packageName != null) {
            return packageName.equals(nsm.getActiveScorerPackage());
        }
        return false;
    }

    void informListeners(String packageName, int userId, boolean isIdle) {
        for (UsageStatsManagerInternal.AppIdleStateChangeListener listener : this.mPackageAccessListeners) {
            listener.onAppIdleStateChanged(packageName, userId, isIdle);
        }
    }

    void informParoleStateChanged() {
        for (UsageStatsManagerInternal.AppIdleStateChangeListener listener : this.mPackageAccessListeners) {
            listener.onParoleStateChanged(this.mAppIdleParoled);
        }
    }

    private static boolean validRange(long currentTime, long beginTime, long endTime) {
        return beginTime <= currentTime && beginTime < endTime;
    }

    private void flushToDiskLocked() {
        int userCount = this.mUserState.size();
        for (int i = 0; i < userCount; i++) {
            UserUsageStatsService service = this.mUserState.valueAt(i);
            service.persistActiveStats();
            this.mAppIdleHistory.writeAppIdleTimesLocked(this.mUserState.keyAt(i));
        }
        this.mAppIdleHistory.writeElapsedTimeLocked();
        this.mHandler.removeMessages(1);
    }

    void dump(String[] args, PrintWriter pw) {
        synchronized (this.mLock) {
            IndentingPrintWriter idpw = new IndentingPrintWriter(pw, "  ");
            ArraySet<String> argSet = new ArraySet<>();
            argSet.addAll(Arrays.asList(args));
            int userCount = this.mUserState.size();
            for (int i = 0; i < userCount; i++) {
                idpw.printPair("user", Integer.valueOf(this.mUserState.keyAt(i)));
                idpw.println();
                idpw.increaseIndent();
                if (argSet.contains("--checkin")) {
                    this.mUserState.valueAt(i).checkin(idpw);
                } else {
                    this.mUserState.valueAt(i).dump(idpw);
                    idpw.println();
                    if (args.length > 0) {
                        if ("history".equals(args[0])) {
                            this.mAppIdleHistory.dumpHistory(idpw, this.mUserState.keyAt(i));
                        } else if ("flush".equals(args[0])) {
                            flushToDiskLocked();
                            pw.println("Flushed stats to disk");
                        }
                    }
                }
                this.mAppIdleHistory.dump(idpw, this.mUserState.keyAt(i));
                idpw.decreaseIndent();
            }
            pw.println();
            pw.println("Carrier privileged apps (have=" + this.mHaveCarrierPrivilegedApps + "): " + this.mCarrierPrivilegedApps);
            pw.println();
            pw.println("Settings:");
            pw.print("  mAppIdleDurationMillis=");
            TimeUtils.formatDuration(this.mAppIdleScreenThresholdMillis, pw);
            pw.println();
            pw.print("  mAppIdleWallclockThresholdMillis=");
            TimeUtils.formatDuration(this.mAppIdleWallclockThresholdMillis, pw);
            pw.println();
            pw.print("  mCheckIdleIntervalMillis=");
            TimeUtils.formatDuration(this.mCheckIdleIntervalMillis, pw);
            pw.println();
            pw.print("  mAppIdleParoleIntervalMillis=");
            TimeUtils.formatDuration(this.mAppIdleParoleIntervalMillis, pw);
            pw.println();
            pw.print("  mAppIdleParoleDurationMillis=");
            TimeUtils.formatDuration(this.mAppIdleParoleDurationMillis, pw);
            pw.println();
            pw.println();
            pw.print("mAppIdleEnabled=");
            pw.print(this.mAppIdleEnabled);
            pw.print(" mAppIdleParoled=");
            pw.print(this.mAppIdleParoled);
            pw.print(" mScreenOn=");
            pw.println(this.mScreenOn);
            pw.print("mLastAppIdleParoledTime=");
            TimeUtils.formatDuration(this.mLastAppIdleParoledTime, pw);
            pw.println();
        }
    }

    class H extends Handler {
        public H(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    UsageStatsService.this.reportEvent((UsageEvents.Event) msg.obj, msg.arg1);
                    break;
                case 1:
                    UsageStatsService.this.flushToDisk();
                    break;
                case 2:
                    UsageStatsService.this.onUserRemoved(msg.arg1);
                    break;
                case 3:
                    UsageStatsService.this.informListeners((String) msg.obj, msg.arg1, msg.arg2 == 1);
                    break;
                case 4:
                    UsageStatsService.this.forceIdleState((String) msg.obj, msg.arg1, msg.arg2 == 1);
                    break;
                case 5:
                    if (UsageStatsService.this.checkIdleStates(msg.arg1)) {
                        UsageStatsService.this.mHandler.sendMessageDelayed(UsageStatsService.this.mHandler.obtainMessage(5, msg.arg1, 0), UsageStatsService.this.mCheckIdleIntervalMillis);
                    }
                    break;
                case 6:
                    UsageStatsService.this.checkParoleTimeout();
                    break;
                case 7:
                    UsageStatsService.this.setAppIdleParoled(false);
                    break;
                case 8:
                    SomeArgs args = (SomeArgs) msg.obj;
                    UsageStatsService.this.reportContentProviderUsage((String) args.arg1, (String) args.arg2, ((Integer) args.arg3).intValue());
                    args.recycle();
                    break;
                case 9:
                    UsageStatsService.this.informParoleStateChanged();
                    break;
                case 10:
                    UsageStatsService.this.mHandler.removeMessages(10);
                    UsageStatsService.this.checkIdleStates(-1);
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    }

    private class SettingsObserver extends ContentObserver {
        private static final String KEY_IDLE_DURATION = "idle_duration2";

        @Deprecated
        private static final String KEY_IDLE_DURATION_OLD = "idle_duration";
        private static final String KEY_PAROLE_DURATION = "parole_duration";
        private static final String KEY_PAROLE_INTERVAL = "parole_interval";
        private static final String KEY_WALLCLOCK_THRESHOLD = "wallclock_threshold";
        private final KeyValueListParser mParser;

        SettingsObserver(Handler handler) {
            super(handler);
            this.mParser = new KeyValueListParser(',');
        }

        void registerObserver() {
            UsageStatsService.this.getContext().getContentResolver().registerContentObserver(Settings.Global.getUriFor("app_idle_constants"), false, this);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
            UsageStatsService.this.postOneTimeCheckIdleStates();
        }

        void updateSettings() {
            synchronized (UsageStatsService.this.mLock) {
                try {
                    this.mParser.setString(Settings.Global.getString(UsageStatsService.this.getContext().getContentResolver(), "app_idle_constants"));
                } catch (IllegalArgumentException e) {
                    Slog.e(UsageStatsService.TAG, "Bad value for app idle settings: " + e.getMessage());
                }
                UsageStatsService.this.mAppIdleScreenThresholdMillis = this.mParser.getLong(KEY_IDLE_DURATION, 43200000L);
                UsageStatsService.this.mAppIdleWallclockThresholdMillis = this.mParser.getLong(KEY_WALLCLOCK_THRESHOLD, 172800000L);
                UsageStatsService.this.mCheckIdleIntervalMillis = Math.min(UsageStatsService.this.mAppIdleScreenThresholdMillis / 4, 28800000L);
                UsageStatsService.this.mAppIdleParoleIntervalMillis = this.mParser.getLong(KEY_PAROLE_INTERVAL, UnixCalendar.DAY_IN_MILLIS);
                UsageStatsService.this.mAppIdleParoleDurationMillis = this.mParser.getLong(KEY_PAROLE_DURATION, LocationFudger.FASTEST_INTERVAL_MS);
                UsageStatsService.this.mAppIdleHistory.setThresholds(UsageStatsService.this.mAppIdleWallclockThresholdMillis, UsageStatsService.this.mAppIdleScreenThresholdMillis);
            }
        }
    }

    private final class BinderService extends IUsageStatsManager.Stub {
        BinderService(UsageStatsService this$0, BinderService binderService) {
            this();
        }

        private BinderService() {
        }

        private boolean hasPermission(String callingPackage) {
            int callingUid = Binder.getCallingUid();
            if (callingUid == 1000) {
                return true;
            }
            int mode = UsageStatsService.this.mAppOps.checkOp(43, callingUid, callingPackage);
            return mode == 3 ? UsageStatsService.this.getContext().checkCallingPermission("android.permission.PACKAGE_USAGE_STATS") == 0 : mode == 0;
        }

        public ParceledListSlice<UsageStats> queryUsageStats(int bucketType, long beginTime, long endTime, String callingPackage) {
            if (!hasPermission(callingPackage)) {
                return null;
            }
            int userId = UserHandle.getCallingUserId();
            long token = Binder.clearCallingIdentity();
            try {
                List<UsageStats> results = UsageStatsService.this.queryUsageStats(userId, bucketType, beginTime, endTime);
                if (results != null) {
                    return new ParceledListSlice<>(results);
                }
                Binder.restoreCallingIdentity(token);
                return null;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public ParceledListSlice<ConfigurationStats> queryConfigurationStats(int bucketType, long beginTime, long endTime, String callingPackage) throws RemoteException {
            if (!hasPermission(callingPackage)) {
                return null;
            }
            int userId = UserHandle.getCallingUserId();
            long token = Binder.clearCallingIdentity();
            try {
                List<ConfigurationStats> results = UsageStatsService.this.queryConfigurationStats(userId, bucketType, beginTime, endTime);
                if (results != null) {
                    return new ParceledListSlice<>(results);
                }
                Binder.restoreCallingIdentity(token);
                return null;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public UsageEvents queryEvents(long beginTime, long endTime, String callingPackage) {
            if (!hasPermission(callingPackage)) {
                return null;
            }
            int userId = UserHandle.getCallingUserId();
            long token = Binder.clearCallingIdentity();
            try {
                return UsageStatsService.this.queryEvents(userId, beginTime, endTime);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public boolean isAppInactive(String packageName, int userId) {
            try {
                int userId2 = ActivityManagerNative.getDefault().handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, true, "isAppInactive", (String) null);
                long token = Binder.clearCallingIdentity();
                try {
                    return UsageStatsService.this.isAppIdleFilteredOrParoled(packageName, userId2, SystemClock.elapsedRealtime());
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            } catch (RemoteException re) {
                throw re.rethrowFromSystemServer();
            }
        }

        public void setAppInactive(String packageName, boolean idle, int userId) {
            int callingUid = Binder.getCallingUid();
            try {
                int userId2 = ActivityManagerNative.getDefault().handleIncomingUser(Binder.getCallingPid(), callingUid, userId, false, true, "setAppIdle", (String) null);
                UsageStatsService.this.getContext().enforceCallingPermission("android.permission.CHANGE_APP_IDLE_STATE", "No permission to change app idle state");
                long token = Binder.clearCallingIdentity();
                try {
                    int appId = UsageStatsService.this.getAppId(packageName);
                    if (appId < 0) {
                        return;
                    }
                    UsageStatsService.this.setAppIdle(packageName, idle, userId2);
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            } catch (RemoteException re) {
                throw re.rethrowFromSystemServer();
            }
        }

        public void whitelistAppTemporarily(String packageName, long duration, int userId) throws RemoteException {
            StringBuilder reason = new StringBuilder(32);
            reason.append("from:");
            UserHandle.formatUid(reason, Binder.getCallingUid());
            UsageStatsService.this.mDeviceIdleController.addPowerSaveTempWhitelistApp(packageName, duration, userId, reason.toString());
        }

        public void onCarrierPrivilegedAppsChanged() {
            UsageStatsService.this.getContext().enforceCallingOrSelfPermission("android.permission.BIND_CARRIER_SERVICES", "onCarrierPrivilegedAppsChanged can only be called by privileged apps.");
            UsageStatsService.this.clearCarrierPrivilegedApps();
        }

        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (UsageStatsService.this.getContext().checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
                pw.println("Permission Denial: can't dump UsageStats from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " without permission android.permission.DUMP");
            } else {
                UsageStatsService.this.dump(args, pw);
            }
        }
    }

    private final class LocalService extends UsageStatsManagerInternal {
        LocalService(UsageStatsService this$0, LocalService localService) {
            this();
        }

        private LocalService() {
        }

        public void reportEvent(ComponentName component, int userId, int eventType) {
            if (component == null) {
                Slog.w(UsageStatsService.TAG, "Event reported without a component name");
                return;
            }
            UsageEvents.Event event = new UsageEvents.Event();
            event.mPackage = component.getPackageName();
            event.mClass = component.getClassName();
            event.mTimeStamp = SystemClock.elapsedRealtime();
            event.mEventType = eventType;
            UsageStatsService.this.mHandler.obtainMessage(0, userId, 0, event).sendToTarget();
        }

        public void reportEvent(String packageName, int userId, int eventType) {
            if (packageName == null) {
                Slog.w(UsageStatsService.TAG, "Event reported without a package name");
                return;
            }
            UsageEvents.Event event = new UsageEvents.Event();
            event.mPackage = packageName;
            event.mTimeStamp = SystemClock.elapsedRealtime();
            event.mEventType = eventType;
            UsageStatsService.this.mHandler.obtainMessage(0, userId, 0, event).sendToTarget();
        }

        public void reportConfigurationChange(Configuration config, int userId) {
            if (config == null) {
                Slog.w(UsageStatsService.TAG, "Configuration event reported with a null config");
                return;
            }
            UsageEvents.Event event = new UsageEvents.Event();
            event.mPackage = "android";
            event.mTimeStamp = SystemClock.elapsedRealtime();
            event.mEventType = 5;
            event.mConfiguration = new Configuration(config);
            UsageStatsService.this.mHandler.obtainMessage(0, userId, 0, event).sendToTarget();
        }

        public void reportContentProviderUsage(String name, String packageName, int userId) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = name;
            args.arg2 = packageName;
            args.arg3 = Integer.valueOf(userId);
            UsageStatsService.this.mHandler.obtainMessage(8, args).sendToTarget();
        }

        public boolean isAppIdle(String packageName, int uidForAppId, int userId) {
            return UsageStatsService.this.isAppIdleFiltered(packageName, uidForAppId, userId, SystemClock.elapsedRealtime());
        }

        public int[] getIdleUidsForUser(int userId) {
            return UsageStatsService.this.getIdleUidsForUser(userId);
        }

        public boolean isAppIdleParoleOn() {
            return UsageStatsService.this.mAppIdleParoled;
        }

        public void prepareShutdown() {
            UsageStatsService.this.shutdown();
        }

        public void addAppIdleStateChangeListener(UsageStatsManagerInternal.AppIdleStateChangeListener listener) {
            UsageStatsService.this.addListener(listener);
            listener.onParoleStateChanged(isAppIdleParoleOn());
        }

        public void removeAppIdleStateChangeListener(UsageStatsManagerInternal.AppIdleStateChangeListener listener) {
            UsageStatsService.this.removeListener(listener);
        }

        public byte[] getBackupPayload(int user, String key) {
            if (user == 0) {
                UserUsageStatsService userStats = UsageStatsService.this.getUserDataAndInitializeIfNeededLocked(user, UsageStatsService.this.checkAndGetTimeLocked());
                return userStats.getBackupPayload(key);
            }
            return null;
        }

        public void applyRestoredPayload(int user, String key, byte[] payload) {
            if (user != 0) {
                return;
            }
            UserUsageStatsService userStats = UsageStatsService.this.getUserDataAndInitializeIfNeededLocked(user, UsageStatsService.this.checkAndGetTimeLocked());
            userStats.applyRestoredPayload(key, payload);
        }
    }
}
