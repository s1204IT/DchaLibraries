package com.android.server;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.AppOpsManager;
import android.app.IActivityManager;
import android.app.IUidObserver;
import android.app.usage.UsageStatsManagerInternal;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManagerInternal;
import android.os.PowerSaveState;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.ArraySet;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseBooleanArray;
import android.util.SparseSetArray;
import android.util.proto.ProtoOutputStream;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.IAppOpsCallback;
import com.android.internal.app.IAppOpsService;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.internal.util.StatLogger;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.slice.SliceClientPermissions;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class AppStateTracker {
    private static final boolean DEBUG = false;
    private static final String TAG = "AppStateTracker";

    @VisibleForTesting
    static final int TARGET_OP = 70;
    ActivityManagerInternal mActivityManagerInternal;
    AppOpsManager mAppOpsManager;
    IAppOpsService mAppOpsService;

    @GuardedBy("mLock")
    boolean mBatterySaverEnabled;
    private final Context mContext;

    @VisibleForTesting
    FeatureFlagsObserver mFlagsObserver;

    @GuardedBy("mLock")
    boolean mForceAllAppStandbyForSmallBattery;

    @GuardedBy("mLock")
    boolean mForceAllAppsStandby;

    @GuardedBy("mLock")
    boolean mForcedAppStandbyEnabled;
    private final MyHandler mHandler;
    IActivityManager mIActivityManager;

    @GuardedBy("mLock")
    boolean mIsPluggedIn;
    PowerManagerInternal mPowerManagerInternal;
    StandbyTracker mStandbyTracker;

    @GuardedBy("mLock")
    boolean mStarted;
    UsageStatsManagerInternal mUsageStatsManagerInternal;
    private final Object mLock = new Object();

    @GuardedBy("mLock")
    final ArraySet<Pair<Integer, String>> mRunAnyRestrictedPackages = new ArraySet<>();

    @GuardedBy("mLock")
    final SparseBooleanArray mActiveUids = new SparseBooleanArray();

    @GuardedBy("mLock")
    final SparseBooleanArray mForegroundUids = new SparseBooleanArray();

    @GuardedBy("mLock")
    private int[] mPowerWhitelistedAllAppIds = new int[0];

    @GuardedBy("mLock")
    private int[] mPowerWhitelistedUserAppIds = new int[0];

    @GuardedBy("mLock")
    private int[] mTempWhitelistedAppIds = this.mPowerWhitelistedAllAppIds;

    @GuardedBy("mLock")
    private final SparseSetArray<String> mExemptedPackages = new SparseSetArray<>();

    @GuardedBy("mLock")
    final ArraySet<Listener> mListeners = new ArraySet<>();
    private final StatLogger mStatLogger = new StatLogger(new String[]{"UID_FG_STATE_CHANGED", "UID_ACTIVE_STATE_CHANGED", "RUN_ANY_CHANGED", "ALL_UNWHITELISTED", "ALL_WHITELIST_CHANGED", "TEMP_WHITELIST_CHANGED", "EXEMPT_CHANGED", "FORCE_ALL_CHANGED", "FORCE_APP_STANDBY_FEATURE_FLAG_CHANGED", "IS_UID_ACTIVE_CACHED", "IS_UID_ACTIVE_RAW"});

    interface Stats {
        public static final int ALL_UNWHITELISTED = 3;
        public static final int ALL_WHITELIST_CHANGED = 4;
        public static final int EXEMPT_CHANGED = 6;
        public static final int FORCE_ALL_CHANGED = 7;
        public static final int FORCE_APP_STANDBY_FEATURE_FLAG_CHANGED = 8;
        public static final int IS_UID_ACTIVE_CACHED = 9;
        public static final int IS_UID_ACTIVE_RAW = 10;
        public static final int RUN_ANY_CHANGED = 2;
        public static final int TEMP_WHITELIST_CHANGED = 5;
        public static final int UID_ACTIVE_STATE_CHANGED = 1;
        public static final int UID_FG_STATE_CHANGED = 0;
    }

    @VisibleForTesting
    class FeatureFlagsObserver extends ContentObserver {
        FeatureFlagsObserver() {
            super(null);
        }

        void register() {
            AppStateTracker.this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("forced_app_standby_enabled"), false, this);
            AppStateTracker.this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("forced_app_standby_for_small_battery_enabled"), false, this);
        }

        boolean isForcedAppStandbyEnabled() {
            return AppStateTracker.this.injectGetGlobalSettingInt("forced_app_standby_enabled", 1) == 1;
        }

        boolean isForcedAppStandbyForSmallBatteryEnabled() {
            return AppStateTracker.this.injectGetGlobalSettingInt("forced_app_standby_for_small_battery_enabled", 0) == 1;
        }

        @Override
        public void onChange(boolean z, Uri uri) {
            if (Settings.Global.getUriFor("forced_app_standby_enabled").equals(uri)) {
                boolean zIsForcedAppStandbyEnabled = isForcedAppStandbyEnabled();
                synchronized (AppStateTracker.this.mLock) {
                    if (AppStateTracker.this.mForcedAppStandbyEnabled == zIsForcedAppStandbyEnabled) {
                        return;
                    }
                    AppStateTracker.this.mForcedAppStandbyEnabled = zIsForcedAppStandbyEnabled;
                    AppStateTracker.this.mHandler.notifyForcedAppStandbyFeatureFlagChanged();
                    return;
                }
            }
            if (Settings.Global.getUriFor("forced_app_standby_for_small_battery_enabled").equals(uri)) {
                boolean zIsForcedAppStandbyForSmallBatteryEnabled = isForcedAppStandbyForSmallBatteryEnabled();
                synchronized (AppStateTracker.this.mLock) {
                    if (AppStateTracker.this.mForceAllAppStandbyForSmallBattery == zIsForcedAppStandbyForSmallBatteryEnabled) {
                        return;
                    }
                    AppStateTracker.this.mForceAllAppStandbyForSmallBattery = zIsForcedAppStandbyForSmallBatteryEnabled;
                    AppStateTracker.this.updateForceAllAppStandbyState();
                    return;
                }
            }
            Slog.w(AppStateTracker.TAG, "Unexpected feature flag uri encountered: " + uri);
        }
    }

    public static abstract class Listener {
        private void onRunAnyAppOpsChanged(AppStateTracker appStateTracker, int i, String str) {
            updateJobsForUidPackage(i, str, appStateTracker.isUidActive(i));
            if (!appStateTracker.areAlarmsRestricted(i, str, false)) {
                unblockAlarmsForUidPackage(i, str);
            } else if (!appStateTracker.areAlarmsRestricted(i, str, true)) {
                unblockAllUnrestrictedAlarms();
            }
            if (!appStateTracker.isRunAnyInBackgroundAppOpsAllowed(i, str)) {
                Slog.v(AppStateTracker.TAG, "Package " + str + SliceClientPermissions.SliceAuthority.DELIMITER + i + " toggled into fg service restriction");
                stopForegroundServicesForUidPackage(i, str);
            }
        }

        private void onUidForegroundStateChanged(AppStateTracker appStateTracker, int i) {
            onUidForeground(i, appStateTracker.isUidInForeground(i));
        }

        private void onUidActiveStateChanged(AppStateTracker appStateTracker, int i) {
            boolean zIsUidActive = appStateTracker.isUidActive(i);
            updateJobsForUid(i, zIsUidActive);
            if (zIsUidActive) {
                unblockAlarmsForUid(i);
            }
        }

        private void onPowerSaveUnwhitelisted(AppStateTracker appStateTracker) {
            updateAllJobs();
            unblockAllUnrestrictedAlarms();
        }

        private void onPowerSaveWhitelistedChanged(AppStateTracker appStateTracker) {
            updateAllJobs();
        }

        private void onTempPowerSaveWhitelistChanged(AppStateTracker appStateTracker) {
            updateAllJobs();
        }

        private void onExemptChanged(AppStateTracker appStateTracker) {
            updateAllJobs();
            unblockAllUnrestrictedAlarms();
        }

        private void onForceAllAppsStandbyChanged(AppStateTracker appStateTracker) {
            updateAllJobs();
            if (!appStateTracker.isForceAllAppsStandbyEnabled()) {
                unblockAllUnrestrictedAlarms();
            }
        }

        public void updateAllJobs() {
        }

        public void updateJobsForUid(int i, boolean z) {
        }

        public void updateJobsForUidPackage(int i, String str, boolean z) {
        }

        public void stopForegroundServicesForUidPackage(int i, String str) {
        }

        public void unblockAllUnrestrictedAlarms() {
        }

        public void unblockAlarmsForUid(int i) {
        }

        public void unblockAlarmsForUidPackage(int i, String str) {
        }

        public void onUidForeground(int i, boolean z) {
        }
    }

    public AppStateTracker(Context context, Looper looper) {
        this.mContext = context;
        this.mHandler = new MyHandler(looper);
    }

    public void onSystemServicesReady() {
        synchronized (this.mLock) {
            if (this.mStarted) {
                return;
            }
            this.mStarted = true;
            this.mIActivityManager = (IActivityManager) Preconditions.checkNotNull(injectIActivityManager());
            this.mActivityManagerInternal = (ActivityManagerInternal) Preconditions.checkNotNull(injectActivityManagerInternal());
            this.mAppOpsManager = (AppOpsManager) Preconditions.checkNotNull(injectAppOpsManager());
            this.mAppOpsService = (IAppOpsService) Preconditions.checkNotNull(injectIAppOpsService());
            this.mPowerManagerInternal = (PowerManagerInternal) Preconditions.checkNotNull(injectPowerManagerInternal());
            this.mUsageStatsManagerInternal = (UsageStatsManagerInternal) Preconditions.checkNotNull(injectUsageStatsManagerInternal());
            this.mFlagsObserver = new FeatureFlagsObserver();
            this.mFlagsObserver.register();
            this.mForcedAppStandbyEnabled = this.mFlagsObserver.isForcedAppStandbyEnabled();
            this.mForceAllAppStandbyForSmallBattery = this.mFlagsObserver.isForcedAppStandbyForSmallBatteryEnabled();
            this.mStandbyTracker = new StandbyTracker();
            this.mUsageStatsManagerInternal.addAppIdleStateChangeListener(this.mStandbyTracker);
            try {
                this.mIActivityManager.registerUidObserver(new UidObserver(), 15, -1, (String) null);
                this.mAppOpsService.startWatchingMode(70, (String) null, new AppOpsWatcher());
            } catch (RemoteException e) {
            }
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.USER_REMOVED");
            intentFilter.addAction("android.intent.action.BATTERY_CHANGED");
            this.mContext.registerReceiver(new MyReceiver(), intentFilter);
            refreshForcedAppStandbyUidPackagesLocked();
            this.mPowerManagerInternal.registerLowPowerModeObserver(11, new Consumer() {
                @Override
                public final void accept(Object obj) {
                    AppStateTracker.lambda$onSystemServicesReady$0(this.f$0, (PowerSaveState) obj);
                }
            });
            this.mBatterySaverEnabled = this.mPowerManagerInternal.getLowPowerState(11).batterySaverEnabled;
            updateForceAllAppStandbyState();
        }
    }

    public static void lambda$onSystemServicesReady$0(AppStateTracker appStateTracker, PowerSaveState powerSaveState) {
        synchronized (appStateTracker.mLock) {
            appStateTracker.mBatterySaverEnabled = powerSaveState.batterySaverEnabled;
            appStateTracker.updateForceAllAppStandbyState();
        }
    }

    @VisibleForTesting
    AppOpsManager injectAppOpsManager() {
        return (AppOpsManager) this.mContext.getSystemService(AppOpsManager.class);
    }

    @VisibleForTesting
    IAppOpsService injectIAppOpsService() {
        return IAppOpsService.Stub.asInterface(ServiceManager.getService("appops"));
    }

    @VisibleForTesting
    IActivityManager injectIActivityManager() {
        return ActivityManager.getService();
    }

    @VisibleForTesting
    ActivityManagerInternal injectActivityManagerInternal() {
        return (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
    }

    @VisibleForTesting
    PowerManagerInternal injectPowerManagerInternal() {
        return (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
    }

    @VisibleForTesting
    UsageStatsManagerInternal injectUsageStatsManagerInternal() {
        return (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class);
    }

    @VisibleForTesting
    boolean isSmallBatteryDevice() {
        return ActivityManager.isSmallBatteryDevice();
    }

    @VisibleForTesting
    int injectGetGlobalSettingInt(String str, int i) {
        return Settings.Global.getInt(this.mContext.getContentResolver(), str, i);
    }

    @GuardedBy("mLock")
    private void refreshForcedAppStandbyUidPackagesLocked() {
        this.mRunAnyRestrictedPackages.clear();
        List packagesForOps = this.mAppOpsManager.getPackagesForOps(new int[]{70});
        if (packagesForOps == null) {
            return;
        }
        int size = packagesForOps.size();
        for (int i = 0; i < size; i++) {
            AppOpsManager.PackageOps packageOps = (AppOpsManager.PackageOps) packagesForOps.get(i);
            List ops = ((AppOpsManager.PackageOps) packagesForOps.get(i)).getOps();
            for (int i2 = 0; i2 < ops.size(); i2++) {
                AppOpsManager.OpEntry opEntry = (AppOpsManager.OpEntry) ops.get(i2);
                if (opEntry.getOp() == 70 && opEntry.getMode() != 0) {
                    this.mRunAnyRestrictedPackages.add(Pair.create(Integer.valueOf(packageOps.getUid()), packageOps.getPackageName()));
                }
            }
        }
    }

    private void updateForceAllAppStandbyState() {
        synchronized (this.mLock) {
            if (this.mForceAllAppStandbyForSmallBattery && isSmallBatteryDevice()) {
                toggleForceAllAppsStandbyLocked(!this.mIsPluggedIn);
            } else {
                toggleForceAllAppsStandbyLocked(this.mBatterySaverEnabled);
            }
        }
    }

    @GuardedBy("mLock")
    private void toggleForceAllAppsStandbyLocked(boolean z) {
        if (z == this.mForceAllAppsStandby) {
            return;
        }
        this.mForceAllAppsStandby = z;
        this.mHandler.notifyForceAllAppsStandbyChanged();
    }

    @GuardedBy("mLock")
    private int findForcedAppStandbyUidPackageIndexLocked(int i, String str) {
        int size = this.mRunAnyRestrictedPackages.size();
        if (size > 8) {
            return this.mRunAnyRestrictedPackages.indexOf(Pair.create(Integer.valueOf(i), str));
        }
        for (int i2 = 0; i2 < size; i2++) {
            Pair<Integer, String> pairValueAt = this.mRunAnyRestrictedPackages.valueAt(i2);
            if (((Integer) pairValueAt.first).intValue() == i && str.equals(pairValueAt.second)) {
                return i2;
            }
        }
        return -1;
    }

    @GuardedBy("mLock")
    boolean isRunAnyRestrictedLocked(int i, String str) {
        return findForcedAppStandbyUidPackageIndexLocked(i, str) >= 0;
    }

    @GuardedBy("mLock")
    boolean updateForcedAppStandbyUidPackageLocked(int i, String str, boolean z) {
        int iFindForcedAppStandbyUidPackageIndexLocked = findForcedAppStandbyUidPackageIndexLocked(i, str);
        if ((iFindForcedAppStandbyUidPackageIndexLocked >= 0) == z) {
            return false;
        }
        if (z) {
            this.mRunAnyRestrictedPackages.add(Pair.create(Integer.valueOf(i), str));
        } else {
            this.mRunAnyRestrictedPackages.removeAt(iFindForcedAppStandbyUidPackageIndexLocked);
        }
        return true;
    }

    private static boolean addUidToArray(SparseBooleanArray sparseBooleanArray, int i) {
        if (UserHandle.isCore(i) || sparseBooleanArray.get(i)) {
            return false;
        }
        sparseBooleanArray.put(i, true);
        return true;
    }

    private static boolean removeUidFromArray(SparseBooleanArray sparseBooleanArray, int i, boolean z) {
        if (UserHandle.isCore(i) || !sparseBooleanArray.get(i)) {
            return false;
        }
        if (z) {
            sparseBooleanArray.delete(i);
            return true;
        }
        sparseBooleanArray.put(i, false);
        return true;
    }

    private final class UidObserver extends IUidObserver.Stub {
        private UidObserver() {
        }

        public void onUidStateChanged(int i, int i2, long j) {
            AppStateTracker.this.mHandler.onUidStateChanged(i, i2);
        }

        public void onUidActive(int i) {
            AppStateTracker.this.mHandler.onUidActive(i);
        }

        public void onUidGone(int i, boolean z) {
            AppStateTracker.this.mHandler.onUidGone(i, z);
        }

        public void onUidIdle(int i, boolean z) {
            AppStateTracker.this.mHandler.onUidIdle(i, z);
        }

        public void onUidCachedChanged(int i, boolean z) {
        }
    }

    private final class AppOpsWatcher extends IAppOpsCallback.Stub {
        private AppOpsWatcher() {
        }

        public void opChanged(int i, int i2, String str) throws RemoteException {
            boolean z = false;
            try {
                if (AppStateTracker.this.mAppOpsService.checkOperation(70, i2, str) != 0) {
                    z = true;
                }
            } catch (RemoteException e) {
            }
            synchronized (AppStateTracker.this.mLock) {
                if (AppStateTracker.this.updateForcedAppStandbyUidPackageLocked(i2, str, z)) {
                    AppStateTracker.this.mHandler.notifyRunAnyAppOpsChanged(i2, str);
                }
            }
        }
    }

    private final class MyReceiver extends BroadcastReceiver {
        private MyReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.USER_REMOVED".equals(intent.getAction())) {
                int intExtra = intent.getIntExtra("android.intent.extra.user_handle", -1);
                if (intExtra > 0) {
                    AppStateTracker.this.mHandler.doUserRemoved(intExtra);
                    return;
                }
                return;
            }
            if ("android.intent.action.BATTERY_CHANGED".equals(intent.getAction())) {
                synchronized (AppStateTracker.this.mLock) {
                    AppStateTracker.this.mIsPluggedIn = intent.getIntExtra("plugged", 0) != 0;
                }
                AppStateTracker.this.updateForceAllAppStandbyState();
            }
        }
    }

    final class StandbyTracker extends UsageStatsManagerInternal.AppIdleStateChangeListener {
        StandbyTracker() {
        }

        public void onAppIdleStateChanged(String str, int i, boolean z, int i2, int i3) {
            if (i2 == 5 ? AppStateTracker.this.mExemptedPackages.add(i, str) : AppStateTracker.this.mExemptedPackages.remove(i, str)) {
                AppStateTracker.this.mHandler.notifyExemptChanged();
            }
        }

        public void onParoleStateChanged(boolean z) {
        }
    }

    private Listener[] cloneListeners() {
        Listener[] listenerArr;
        synchronized (this.mLock) {
            listenerArr = (Listener[]) this.mListeners.toArray(new Listener[this.mListeners.size()]);
        }
        return listenerArr;
    }

    private class MyHandler extends Handler {
        private static final int MSG_ALL_UNWHITELISTED = 4;
        private static final int MSG_ALL_WHITELIST_CHANGED = 5;
        private static final int MSG_EXEMPT_CHANGED = 10;
        private static final int MSG_FORCE_ALL_CHANGED = 7;
        private static final int MSG_FORCE_APP_STANDBY_FEATURE_FLAG_CHANGED = 9;
        private static final int MSG_ON_UID_ACTIVE = 12;
        private static final int MSG_ON_UID_GONE = 13;
        private static final int MSG_ON_UID_IDLE = 14;
        private static final int MSG_ON_UID_STATE_CHANGED = 11;
        private static final int MSG_RUN_ANY_CHANGED = 3;
        private static final int MSG_TEMP_WHITELIST_CHANGED = 6;
        private static final int MSG_UID_ACTIVE_STATE_CHANGED = 0;
        private static final int MSG_UID_FG_STATE_CHANGED = 1;
        private static final int MSG_USER_REMOVED = 8;

        public MyHandler(Looper looper) {
            super(looper);
        }

        public void notifyUidActiveStateChanged(int i) {
            obtainMessage(0, i, 0).sendToTarget();
        }

        public void notifyUidForegroundStateChanged(int i) {
            obtainMessage(1, i, 0).sendToTarget();
        }

        public void notifyRunAnyAppOpsChanged(int i, String str) {
            obtainMessage(3, i, 0, str).sendToTarget();
        }

        public void notifyAllUnwhitelisted() {
            removeMessages(4);
            obtainMessage(4).sendToTarget();
        }

        public void notifyAllWhitelistChanged() {
            removeMessages(5);
            obtainMessage(5).sendToTarget();
        }

        public void notifyTempWhitelistChanged() {
            removeMessages(6);
            obtainMessage(6).sendToTarget();
        }

        public void notifyForceAllAppsStandbyChanged() {
            removeMessages(7);
            obtainMessage(7).sendToTarget();
        }

        public void notifyForcedAppStandbyFeatureFlagChanged() {
            removeMessages(9);
            obtainMessage(9).sendToTarget();
        }

        public void notifyExemptChanged() {
            removeMessages(10);
            obtainMessage(10).sendToTarget();
        }

        public void doUserRemoved(int i) {
            obtainMessage(8, i, 0).sendToTarget();
        }

        public void onUidStateChanged(int i, int i2) {
            obtainMessage(11, i, i2).sendToTarget();
        }

        public void onUidActive(int i) {
            obtainMessage(12, i, 0).sendToTarget();
        }

        public void onUidGone(int i, boolean z) {
            obtainMessage(13, i, z ? 1 : 0).sendToTarget();
        }

        public void onUidIdle(int i, boolean z) {
            obtainMessage(14, i, z ? 1 : 0).sendToTarget();
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what != 8) {
                synchronized (AppStateTracker.this.mLock) {
                    if (AppStateTracker.this.mStarted) {
                        AppStateTracker appStateTracker = AppStateTracker.this;
                        long time = AppStateTracker.this.mStatLogger.getTime();
                        boolean z = true;
                        switch (message.what) {
                            case 0:
                                for (Listener listener : AppStateTracker.this.cloneListeners()) {
                                    listener.onUidActiveStateChanged(appStateTracker, message.arg1);
                                }
                                AppStateTracker.this.mStatLogger.logDurationStat(1, time);
                                return;
                            case 1:
                                for (Listener listener2 : AppStateTracker.this.cloneListeners()) {
                                    listener2.onUidForegroundStateChanged(appStateTracker, message.arg1);
                                }
                                AppStateTracker.this.mStatLogger.logDurationStat(0, time);
                                return;
                            case 2:
                            default:
                                return;
                            case 3:
                                for (Listener listener3 : AppStateTracker.this.cloneListeners()) {
                                    listener3.onRunAnyAppOpsChanged(appStateTracker, message.arg1, (String) message.obj);
                                }
                                AppStateTracker.this.mStatLogger.logDurationStat(2, time);
                                return;
                            case 4:
                                for (Listener listener4 : AppStateTracker.this.cloneListeners()) {
                                    listener4.onPowerSaveUnwhitelisted(appStateTracker);
                                }
                                AppStateTracker.this.mStatLogger.logDurationStat(3, time);
                                return;
                            case 5:
                                for (Listener listener5 : AppStateTracker.this.cloneListeners()) {
                                    listener5.onPowerSaveWhitelistedChanged(appStateTracker);
                                }
                                AppStateTracker.this.mStatLogger.logDurationStat(4, time);
                                return;
                            case 6:
                                for (Listener listener6 : AppStateTracker.this.cloneListeners()) {
                                    listener6.onTempPowerSaveWhitelistChanged(appStateTracker);
                                }
                                AppStateTracker.this.mStatLogger.logDurationStat(5, time);
                                return;
                            case 7:
                                for (Listener listener7 : AppStateTracker.this.cloneListeners()) {
                                    listener7.onForceAllAppsStandbyChanged(appStateTracker);
                                }
                                AppStateTracker.this.mStatLogger.logDurationStat(7, time);
                                return;
                            case 8:
                                AppStateTracker.this.handleUserRemoved(message.arg1);
                                return;
                            case 9:
                                synchronized (AppStateTracker.this.mLock) {
                                    if (AppStateTracker.this.mForcedAppStandbyEnabled || AppStateTracker.this.mForceAllAppsStandby) {
                                        z = false;
                                    }
                                    break;
                                }
                                for (Listener listener8 : AppStateTracker.this.cloneListeners()) {
                                    listener8.updateAllJobs();
                                    if (z) {
                                        listener8.unblockAllUnrestrictedAlarms();
                                    }
                                }
                                AppStateTracker.this.mStatLogger.logDurationStat(8, time);
                                return;
                            case 10:
                                for (Listener listener9 : AppStateTracker.this.cloneListeners()) {
                                    listener9.onExemptChanged(appStateTracker);
                                }
                                AppStateTracker.this.mStatLogger.logDurationStat(6, time);
                                return;
                            case 11:
                                handleUidStateChanged(message.arg1, message.arg2);
                                return;
                            case 12:
                                handleUidActive(message.arg1);
                                return;
                            case 13:
                                handleUidGone(message.arg1, message.arg1 != 0);
                                return;
                            case 14:
                                handleUidIdle(message.arg1, message.arg1 != 0);
                                return;
                        }
                    }
                    return;
                }
            }
            AppStateTracker.this.handleUserRemoved(message.arg1);
        }

        public void handleUidStateChanged(int i, int i2) {
            synchronized (AppStateTracker.this.mLock) {
                try {
                    if (i2 > 5) {
                        if (AppStateTracker.removeUidFromArray(AppStateTracker.this.mForegroundUids, i, false)) {
                            AppStateTracker.this.mHandler.notifyUidForegroundStateChanged(i);
                        }
                    } else if (AppStateTracker.addUidToArray(AppStateTracker.this.mForegroundUids, i)) {
                        AppStateTracker.this.mHandler.notifyUidForegroundStateChanged(i);
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
        }

        public void handleUidActive(int i) {
            synchronized (AppStateTracker.this.mLock) {
                if (AppStateTracker.addUidToArray(AppStateTracker.this.mActiveUids, i)) {
                    AppStateTracker.this.mHandler.notifyUidActiveStateChanged(i);
                }
            }
        }

        public void handleUidGone(int i, boolean z) {
            removeUid(i, true);
        }

        public void handleUidIdle(int i, boolean z) {
            removeUid(i, false);
        }

        private void removeUid(int i, boolean z) {
            synchronized (AppStateTracker.this.mLock) {
                if (AppStateTracker.removeUidFromArray(AppStateTracker.this.mActiveUids, i, z)) {
                    AppStateTracker.this.mHandler.notifyUidActiveStateChanged(i);
                }
                if (AppStateTracker.removeUidFromArray(AppStateTracker.this.mForegroundUids, i, z)) {
                    AppStateTracker.this.mHandler.notifyUidForegroundStateChanged(i);
                }
            }
        }
    }

    void handleUserRemoved(int i) {
        synchronized (this.mLock) {
            for (int size = this.mRunAnyRestrictedPackages.size() - 1; size >= 0; size--) {
                if (UserHandle.getUserId(((Integer) this.mRunAnyRestrictedPackages.valueAt(size).first).intValue()) == i) {
                    this.mRunAnyRestrictedPackages.removeAt(size);
                }
            }
            cleanUpArrayForUser(this.mActiveUids, i);
            cleanUpArrayForUser(this.mForegroundUids, i);
            this.mExemptedPackages.remove(i);
        }
    }

    private void cleanUpArrayForUser(SparseBooleanArray sparseBooleanArray, int i) {
        for (int size = sparseBooleanArray.size() - 1; size >= 0; size--) {
            if (UserHandle.getUserId(sparseBooleanArray.keyAt(size)) == i) {
                sparseBooleanArray.removeAt(size);
            }
        }
    }

    public void setPowerSaveWhitelistAppIds(int[] iArr, int[] iArr2, int[] iArr3) {
        synchronized (this.mLock) {
            int[] iArr4 = this.mPowerWhitelistedAllAppIds;
            int[] iArr5 = this.mTempWhitelistedAppIds;
            this.mPowerWhitelistedAllAppIds = iArr;
            this.mTempWhitelistedAppIds = iArr3;
            this.mPowerWhitelistedUserAppIds = iArr2;
            if (isAnyAppIdUnwhitelisted(iArr4, this.mPowerWhitelistedAllAppIds)) {
                this.mHandler.notifyAllUnwhitelisted();
            } else if (!Arrays.equals(iArr4, this.mPowerWhitelistedAllAppIds)) {
                this.mHandler.notifyAllWhitelistChanged();
            }
            if (!Arrays.equals(iArr5, this.mTempWhitelistedAppIds)) {
                this.mHandler.notifyTempWhitelistChanged();
            }
        }
    }

    @VisibleForTesting
    static boolean isAnyAppIdUnwhitelisted(int[] iArr, int[] iArr2) {
        int i = 0;
        int i2 = 0;
        while (true) {
            boolean z = i >= iArr.length;
            boolean z2 = i2 >= iArr2.length;
            if (z || z2) {
                break;
            }
            int i3 = iArr[i];
            int i4 = iArr2[i2];
            if (i3 == i4) {
                i++;
                i2++;
            } else {
                if (i3 < i4) {
                    return true;
                }
                i2++;
            }
        }
    }

    public void addListener(Listener listener) {
        synchronized (this.mLock) {
            this.mListeners.add(listener);
        }
    }

    public boolean areAlarmsRestricted(int i, String str, boolean z) {
        return isRestricted(i, str, false, z);
    }

    public boolean areJobsRestricted(int i, String str, boolean z) {
        return isRestricted(i, str, true, z);
    }

    public boolean areForegroundServicesRestricted(int i, String str) {
        boolean zIsRunAnyRestrictedLocked;
        synchronized (this.mLock) {
            zIsRunAnyRestrictedLocked = isRunAnyRestrictedLocked(i, str);
        }
        return zIsRunAnyRestrictedLocked;
    }

    private boolean isRestricted(int i, String str, boolean z, boolean z2) {
        if (isUidActive(i)) {
            return false;
        }
        synchronized (this.mLock) {
            int appId = UserHandle.getAppId(i);
            if (ArrayUtils.contains(this.mPowerWhitelistedAllAppIds, appId)) {
                return false;
            }
            if (z && ArrayUtils.contains(this.mTempWhitelistedAppIds, appId)) {
                return false;
            }
            if (this.mForcedAppStandbyEnabled && isRunAnyRestrictedLocked(i, str)) {
                return true;
            }
            if (z2) {
                return false;
            }
            if (this.mExemptedPackages.contains(UserHandle.getUserId(i), str)) {
                return false;
            }
            return this.mForceAllAppsStandby;
        }
    }

    public boolean isUidActive(int i) {
        boolean z;
        if (UserHandle.isCore(i)) {
            return true;
        }
        synchronized (this.mLock) {
            z = this.mActiveUids.get(i);
        }
        return z;
    }

    public boolean isUidActiveSynced(int i) {
        if (isUidActive(i)) {
            return true;
        }
        long time = this.mStatLogger.getTime();
        boolean zIsUidActive = this.mActivityManagerInternal.isUidActive(i);
        this.mStatLogger.logDurationStat(10, time);
        return zIsUidActive;
    }

    public boolean isUidInForeground(int i) {
        boolean z;
        if (UserHandle.isCore(i)) {
            return true;
        }
        synchronized (this.mLock) {
            z = this.mForegroundUids.get(i);
        }
        return z;
    }

    boolean isForceAllAppsStandbyEnabled() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mForceAllAppsStandby;
        }
        return z;
    }

    public boolean isRunAnyInBackgroundAppOpsAllowed(int i, String str) {
        boolean z;
        synchronized (this.mLock) {
            z = !isRunAnyRestrictedLocked(i, str);
        }
        return z;
    }

    public boolean isUidPowerSaveWhitelisted(int i) {
        boolean zContains;
        synchronized (this.mLock) {
            zContains = ArrayUtils.contains(this.mPowerWhitelistedAllAppIds, UserHandle.getAppId(i));
        }
        return zContains;
    }

    public boolean isUidPowerSaveUserWhitelisted(int i) {
        boolean zContains;
        synchronized (this.mLock) {
            zContains = ArrayUtils.contains(this.mPowerWhitelistedUserAppIds, UserHandle.getAppId(i));
        }
        return zContains;
    }

    public boolean isUidTempPowerSaveWhitelisted(int i) {
        boolean zContains;
        synchronized (this.mLock) {
            zContains = ArrayUtils.contains(this.mTempWhitelistedAppIds, UserHandle.getAppId(i));
        }
        return zContains;
    }

    @Deprecated
    public void dump(PrintWriter printWriter, String str) {
        dump(new IndentingPrintWriter(printWriter, "  ").setIndent(str));
    }

    public void dump(IndentingPrintWriter indentingPrintWriter) {
        synchronized (this.mLock) {
            indentingPrintWriter.println("Forced App Standby Feature enabled: " + this.mForcedAppStandbyEnabled);
            indentingPrintWriter.print("Force all apps standby: ");
            indentingPrintWriter.println(isForceAllAppsStandbyEnabled());
            indentingPrintWriter.print("Small Battery Device: ");
            indentingPrintWriter.println(isSmallBatteryDevice());
            indentingPrintWriter.print("Force all apps standby for small battery device: ");
            indentingPrintWriter.println(this.mForceAllAppStandbyForSmallBattery);
            indentingPrintWriter.print("Plugged In: ");
            indentingPrintWriter.println(this.mIsPluggedIn);
            indentingPrintWriter.print("Active uids: ");
            dumpUids(indentingPrintWriter, this.mActiveUids);
            indentingPrintWriter.print("Foreground uids: ");
            dumpUids(indentingPrintWriter, this.mForegroundUids);
            indentingPrintWriter.print("Except-idle + user whitelist appids: ");
            indentingPrintWriter.println(Arrays.toString(this.mPowerWhitelistedAllAppIds));
            indentingPrintWriter.print("User whitelist appids: ");
            indentingPrintWriter.println(Arrays.toString(this.mPowerWhitelistedUserAppIds));
            indentingPrintWriter.print("Temp whitelist appids: ");
            indentingPrintWriter.println(Arrays.toString(this.mTempWhitelistedAppIds));
            indentingPrintWriter.println("Exempted packages:");
            indentingPrintWriter.increaseIndent();
            for (int i = 0; i < this.mExemptedPackages.size(); i++) {
                indentingPrintWriter.print("User ");
                indentingPrintWriter.print(this.mExemptedPackages.keyAt(i));
                indentingPrintWriter.println();
                indentingPrintWriter.increaseIndent();
                for (int i2 = 0; i2 < this.mExemptedPackages.sizeAt(i); i2++) {
                    indentingPrintWriter.print((String) this.mExemptedPackages.valueAt(i, i2));
                    indentingPrintWriter.println();
                }
                indentingPrintWriter.decreaseIndent();
            }
            indentingPrintWriter.decreaseIndent();
            indentingPrintWriter.println();
            indentingPrintWriter.println("Restricted packages:");
            indentingPrintWriter.increaseIndent();
            for (Pair<Integer, String> pair : this.mRunAnyRestrictedPackages) {
                indentingPrintWriter.print(UserHandle.formatUid(((Integer) pair.first).intValue()));
                indentingPrintWriter.print(" ");
                indentingPrintWriter.print((String) pair.second);
                indentingPrintWriter.println();
            }
            indentingPrintWriter.decreaseIndent();
            this.mStatLogger.dump(indentingPrintWriter);
        }
    }

    private void dumpUids(PrintWriter printWriter, SparseBooleanArray sparseBooleanArray) {
        printWriter.print("[");
        String str = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        for (int i = 0; i < sparseBooleanArray.size(); i++) {
            if (sparseBooleanArray.valueAt(i)) {
                printWriter.print(str);
                printWriter.print(UserHandle.formatUid(sparseBooleanArray.keyAt(i)));
                str = " ";
            }
        }
        printWriter.println("]");
    }

    public void dumpProto(ProtoOutputStream protoOutputStream, long j) {
        synchronized (this.mLock) {
            long jStart = protoOutputStream.start(j);
            protoOutputStream.write(1133871366145L, this.mForceAllAppsStandby);
            protoOutputStream.write(1133871366150L, isSmallBatteryDevice());
            protoOutputStream.write(1133871366151L, this.mForceAllAppStandbyForSmallBattery);
            protoOutputStream.write(1133871366152L, this.mIsPluggedIn);
            for (int i = 0; i < this.mActiveUids.size(); i++) {
                if (this.mActiveUids.valueAt(i)) {
                    protoOutputStream.write(2220498092034L, this.mActiveUids.keyAt(i));
                }
            }
            for (int i2 = 0; i2 < this.mForegroundUids.size(); i2++) {
                if (this.mForegroundUids.valueAt(i2)) {
                    protoOutputStream.write(2220498092043L, this.mForegroundUids.keyAt(i2));
                }
            }
            for (int i3 : this.mPowerWhitelistedAllAppIds) {
                protoOutputStream.write(2220498092035L, i3);
            }
            for (int i4 : this.mPowerWhitelistedUserAppIds) {
                protoOutputStream.write(2220498092044L, i4);
            }
            for (int i5 : this.mTempWhitelistedAppIds) {
                protoOutputStream.write(2220498092036L, i5);
            }
            for (int i6 = 0; i6 < this.mExemptedPackages.size(); i6++) {
                for (int i7 = 0; i7 < this.mExemptedPackages.sizeAt(i6); i7++) {
                    long jStart2 = protoOutputStream.start(2246267895818L);
                    protoOutputStream.write(1120986464257L, this.mExemptedPackages.keyAt(i6));
                    protoOutputStream.write(1138166333442L, (String) this.mExemptedPackages.valueAt(i6, i7));
                    protoOutputStream.end(jStart2);
                }
            }
            for (Pair<Integer, String> pair : this.mRunAnyRestrictedPackages) {
                long jStart3 = protoOutputStream.start(2246267895813L);
                protoOutputStream.write(1120986464257L, ((Integer) pair.first).intValue());
                protoOutputStream.write(1138166333442L, (String) pair.second);
                protoOutputStream.end(jStart3);
            }
            this.mStatLogger.dumpProto(protoOutputStream, 1146756268041L);
            protoOutputStream.end(jStart);
        }
    }
}
