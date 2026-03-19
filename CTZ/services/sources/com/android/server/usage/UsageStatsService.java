package com.android.server.usage;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.IUidObserver;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManagerInternal;
import android.app.usage.AppStandbyInfo;
import android.app.usage.ConfigurationStats;
import android.app.usage.EventStats;
import android.app.usage.IUsageStatsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManagerInternal;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ParceledListSlice;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.net.util.NetworkConstants;
import android.os.Binder;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IDeviceIdleController;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import com.android.internal.content.PackageMonitor;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.pm.DumpState;
import com.android.server.pm.PackageManagerService;
import com.android.server.usage.AppTimeLimitController;
import com.android.server.usage.UserUsageStatsService;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class UsageStatsService extends SystemService implements UserUsageStatsService.StatsUpdatedListener {
    static final boolean COMPRESS_TIME = false;
    static final boolean DEBUG = false;
    private static final boolean ENABLE_KERNEL_UPDATES = true;
    private static final long FLUSH_INTERVAL = 1200000;
    static final int MSG_FLUSH_TO_DISK = 1;
    static final int MSG_REMOVE_USER = 2;
    static final int MSG_REPORT_EVENT = 0;
    static final int MSG_UID_STATE_CHANGED = 3;
    static final String TAG = "UsageStatsService";
    private static final long TEN_SECONDS = 10000;
    private static final long TIME_CHANGE_THRESHOLD_MILLIS = 2000;
    private static final long TWENTY_MINUTES = 1200000;
    AppOpsManager mAppOps;
    AppStandbyController mAppStandby;
    AppTimeLimitController mAppTimeLimit;
    IDeviceIdleController mDeviceIdleController;
    DevicePolicyManagerInternal mDpmInternal;
    Handler mHandler;
    private final Object mLock;
    PackageManager mPackageManager;
    PackageManagerInternal mPackageManagerInternal;
    PackageMonitor mPackageMonitor;
    long mRealTimeSnapshot;
    private UsageStatsManagerInternal.AppIdleStateChangeListener mStandbyChangeListener;
    long mSystemTimeSnapshot;
    private final IUidObserver mUidObserver;
    private final SparseIntArray mUidToKernelCounter;
    private File mUsageStatsDir;
    UserManager mUserManager;
    private final SparseArray<UserUsageStatsService> mUserState;
    public static final boolean ENABLE_TIME_CHANGE_CORRECTION = SystemProperties.getBoolean("persist.debug.time_correction", true);
    private static final File KERNEL_COUNTER_FILE = new File("/proc/uid_procstat/set");

    public UsageStatsService(Context context) {
        super(context);
        this.mLock = new Object();
        this.mUserState = new SparseArray<>();
        this.mUidToKernelCounter = new SparseIntArray();
        this.mStandbyChangeListener = new UsageStatsManagerInternal.AppIdleStateChangeListener() {
            public void onAppIdleStateChanged(String str, int i, boolean z, int i2, int i3) {
                UsageEvents.Event event = new UsageEvents.Event();
                event.mEventType = 11;
                event.mBucketAndReason = (i2 << 16) | (i3 & NetworkConstants.ARP_HWTYPE_RESERVED_HI);
                event.mPackage = str;
                event.mTimeStamp = SystemClock.elapsedRealtime();
                UsageStatsService.this.mHandler.obtainMessage(0, i, 0, event).sendToTarget();
            }

            public void onParoleStateChanged(boolean z) {
            }
        };
        this.mUidObserver = new IUidObserver.Stub() {
            public void onUidStateChanged(int i, int i2, long j) {
                UsageStatsService.this.mHandler.obtainMessage(3, i, i2).sendToTarget();
            }

            public void onUidIdle(int i, boolean z) {
            }

            public void onUidGone(int i, boolean z) {
                onUidStateChanged(i, 19, 0L);
            }

            public void onUidActive(int i) {
            }

            public void onUidCachedChanged(int i, boolean z) {
            }
        };
    }

    @Override
    public void onStart() {
        this.mAppOps = (AppOpsManager) getContext().getSystemService("appops");
        this.mUserManager = (UserManager) getContext().getSystemService("user");
        this.mPackageManager = getContext().getPackageManager();
        this.mPackageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        this.mHandler = new H(BackgroundThread.get().getLooper());
        this.mAppStandby = new AppStandbyController(getContext(), BackgroundThread.get().getLooper());
        this.mAppTimeLimit = new AppTimeLimitController(new AppTimeLimitController.OnLimitReachedListener() {
            @Override
            public final void onLimitReached(int i, int i2, long j, long j2, PendingIntent pendingIntent) {
                UsageStatsService.lambda$onStart$0(this.f$0, i, i2, j, j2, pendingIntent);
            }
        }, this.mHandler.getLooper());
        this.mAppStandby.addListener(this.mStandbyChangeListener);
        this.mUsageStatsDir = new File(new File(Environment.getDataDirectory(), "system"), "usagestats");
        this.mUsageStatsDir.mkdirs();
        if (!this.mUsageStatsDir.exists()) {
            throw new IllegalStateException("Usage stats directory does not exist: " + this.mUsageStatsDir.getAbsolutePath());
        }
        IntentFilter intentFilter = new IntentFilter("android.intent.action.USER_REMOVED");
        intentFilter.addAction("android.intent.action.USER_STARTED");
        getContext().registerReceiverAsUser(new UserActionsReceiver(), UserHandle.ALL, intentFilter, null, this.mHandler);
        synchronized (this.mLock) {
            cleanUpRemovedUsersLocked();
        }
        this.mRealTimeSnapshot = SystemClock.elapsedRealtime();
        this.mSystemTimeSnapshot = System.currentTimeMillis();
        publishLocalService(UsageStatsManagerInternal.class, new LocalService());
        publishBinderService("usagestats", new BinderService());
        getUserDataAndInitializeIfNeededLocked(0, this.mSystemTimeSnapshot);
    }

    public static void lambda$onStart$0(UsageStatsService usageStatsService, int i, int i2, long j, long j2, PendingIntent pendingIntent) {
        Intent intent = new Intent();
        intent.putExtra("android.app.usage.extra.OBSERVER_ID", i);
        intent.putExtra("android.app.usage.extra.TIME_LIMIT", j);
        intent.putExtra("android.app.usage.extra.TIME_USED", j2);
        try {
            pendingIntent.send(usageStatsService.getContext(), 0, intent);
        } catch (PendingIntent.CanceledException e) {
            Slog.w(TAG, "Couldn't deliver callback: " + pendingIntent);
        }
    }

    @Override
    public void onBootPhase(int i) {
        this.mAppStandby.onBootPhase(i);
        if (i == 500) {
            getDpmInternal();
            this.mDeviceIdleController = IDeviceIdleController.Stub.asInterface(ServiceManager.getService("deviceidle"));
            if (KERNEL_COUNTER_FILE.exists()) {
                try {
                    ActivityManager.getService().registerUidObserver(this.mUidObserver, 3, -1, (String) null);
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            } else {
                Slog.w(TAG, "Missing procfs interface: " + KERNEL_COUNTER_FILE);
            }
        }
    }

    private DevicePolicyManagerInternal getDpmInternal() {
        if (this.mDpmInternal == null) {
            this.mDpmInternal = (DevicePolicyManagerInternal) LocalServices.getService(DevicePolicyManagerInternal.class);
        }
        return this.mDpmInternal;
    }

    private class UserActionsReceiver extends BroadcastReceiver {
        private UserActionsReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            int intExtra = intent.getIntExtra("android.intent.extra.user_handle", -1);
            String action = intent.getAction();
            if ("android.intent.action.USER_REMOVED".equals(action)) {
                if (intExtra >= 0) {
                    UsageStatsService.this.mHandler.obtainMessage(2, intExtra, 0).sendToTarget();
                }
            } else if ("android.intent.action.USER_STARTED".equals(action) && intExtra >= 0) {
                UsageStatsService.this.mAppStandby.postCheckIdleStates(intExtra);
            }
        }
    }

    @Override
    public void onStatsUpdated() {
        this.mHandler.sendEmptyMessageDelayed(1, 1200000L);
    }

    @Override
    public void onStatsReloaded() {
        this.mAppStandby.postOneTimeCheckIdleStates();
    }

    @Override
    public void onNewUpdate(int i) {
        this.mAppStandby.initializeDefaultsForSystemApps(i);
    }

    private boolean shouldObfuscateInstantAppsForCaller(int i, int i2) {
        return !this.mPackageManagerInternal.canAccessInstantApps(i, i2);
    }

    private void cleanUpRemovedUsersLocked() {
        List users = this.mUserManager.getUsers(true);
        if (users == null || users.size() == 0) {
            throw new IllegalStateException("There can't be no users");
        }
        ArraySet arraySet = new ArraySet();
        String[] list = this.mUsageStatsDir.list();
        if (list == null) {
            return;
        }
        arraySet.addAll(Arrays.asList(list));
        int size = users.size();
        for (int i = 0; i < size; i++) {
            arraySet.remove(Integer.toString(((UserInfo) users.get(i)).id));
        }
        int size2 = arraySet.size();
        for (int i2 = 0; i2 < size2; i2++) {
            deleteRecursively(new File(this.mUsageStatsDir, (String) arraySet.valueAt(i2)));
        }
    }

    private static void deleteRecursively(File file) {
        File[] fileArrListFiles = file.listFiles();
        if (fileArrListFiles != null) {
            for (File file2 : fileArrListFiles) {
                deleteRecursively(file2);
            }
        }
        if (!file.delete()) {
            Slog.e(TAG, "Failed to delete " + file);
        }
    }

    private UserUsageStatsService getUserDataAndInitializeIfNeededLocked(int i, long j) {
        UserUsageStatsService userUsageStatsService = this.mUserState.get(i);
        if (userUsageStatsService == null) {
            UserUsageStatsService userUsageStatsService2 = new UserUsageStatsService(getContext(), i, new File(this.mUsageStatsDir, Integer.toString(i)), this);
            userUsageStatsService2.init(j);
            this.mUserState.put(i, userUsageStatsService2);
            return userUsageStatsService2;
        }
        return userUsageStatsService;
    }

    private long checkAndGetTimeLocked() {
        long jCurrentTimeMillis = System.currentTimeMillis();
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        long j = (jElapsedRealtime - this.mRealTimeSnapshot) + this.mSystemTimeSnapshot;
        long j2 = jCurrentTimeMillis - j;
        if (Math.abs(j2) > TIME_CHANGE_THRESHOLD_MILLIS && ENABLE_TIME_CHANGE_CORRECTION) {
            Slog.i(TAG, "Time changed in UsageStats by " + (j2 / 1000) + " seconds");
            int size = this.mUserState.size();
            for (int i = 0; i < size; i++) {
                this.mUserState.valueAt(i).onTimeChanged(j, jCurrentTimeMillis);
            }
            this.mRealTimeSnapshot = jElapsedRealtime;
            this.mSystemTimeSnapshot = jCurrentTimeMillis;
        }
        return jCurrentTimeMillis;
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

    void reportEvent(UsageEvents.Event event, int i) {
        synchronized (this.mLock) {
            long jCheckAndGetTimeLocked = checkAndGetTimeLocked();
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            convertToSystemTimeLocked(event);
            if (event.getPackageName() != null && this.mPackageManagerInternal.isPackageEphemeral(i, event.getPackageName())) {
                event.mFlags |= 1;
            }
            getUserDataAndInitializeIfNeededLocked(i, jCheckAndGetTimeLocked).reportEvent(event);
            this.mAppStandby.reportEvent(event, jElapsedRealtime, i);
            switch (event.mEventType) {
                case 1:
                    this.mAppTimeLimit.moveToForeground(event.getPackageName(), event.getClassName(), i);
                    break;
                case 2:
                    this.mAppTimeLimit.moveToBackground(event.getPackageName(), event.getClassName(), i);
                    break;
            }
        }
    }

    void flushToDisk() {
        synchronized (this.mLock) {
            flushToDiskLocked();
        }
    }

    void onUserRemoved(int i) {
        synchronized (this.mLock) {
            Slog.i(TAG, "Removing user " + i + " and all data.");
            this.mUserState.remove(i);
            this.mAppStandby.onUserRemoved(i);
            this.mAppTimeLimit.onUserRemoved(i);
            cleanUpRemovedUsersLocked();
        }
    }

    List<UsageStats> queryUsageStats(int i, int i2, long j, long j2, boolean z) {
        synchronized (this.mLock) {
            long jCheckAndGetTimeLocked = checkAndGetTimeLocked();
            if (!validRange(jCheckAndGetTimeLocked, j, j2)) {
                return null;
            }
            List<UsageStats> listQueryUsageStats = getUserDataAndInitializeIfNeededLocked(i, jCheckAndGetTimeLocked).queryUsageStats(i2, j, j2);
            if (listQueryUsageStats == null) {
                return null;
            }
            if (z) {
                for (int size = listQueryUsageStats.size() - 1; size >= 0; size--) {
                    UsageStats usageStats = listQueryUsageStats.get(size);
                    if (this.mPackageManagerInternal.isPackageEphemeral(i, usageStats.mPackageName)) {
                        listQueryUsageStats.set(size, usageStats.getObfuscatedForInstantApp());
                    }
                }
            }
            return listQueryUsageStats;
        }
    }

    List<ConfigurationStats> queryConfigurationStats(int i, int i2, long j, long j2) {
        synchronized (this.mLock) {
            long jCheckAndGetTimeLocked = checkAndGetTimeLocked();
            if (!validRange(jCheckAndGetTimeLocked, j, j2)) {
                return null;
            }
            return getUserDataAndInitializeIfNeededLocked(i, jCheckAndGetTimeLocked).queryConfigurationStats(i2, j, j2);
        }
    }

    List<EventStats> queryEventStats(int i, int i2, long j, long j2) {
        synchronized (this.mLock) {
            long jCheckAndGetTimeLocked = checkAndGetTimeLocked();
            if (!validRange(jCheckAndGetTimeLocked, j, j2)) {
                return null;
            }
            return getUserDataAndInitializeIfNeededLocked(i, jCheckAndGetTimeLocked).queryEventStats(i2, j, j2);
        }
    }

    UsageEvents queryEvents(int i, long j, long j2, boolean z) {
        synchronized (this.mLock) {
            long jCheckAndGetTimeLocked = checkAndGetTimeLocked();
            if (!validRange(jCheckAndGetTimeLocked, j, j2)) {
                return null;
            }
            return getUserDataAndInitializeIfNeededLocked(i, jCheckAndGetTimeLocked).queryEvents(j, j2, z);
        }
    }

    UsageEvents queryEventsForPackage(int i, long j, long j2, String str) {
        synchronized (this.mLock) {
            long jCheckAndGetTimeLocked = checkAndGetTimeLocked();
            if (!validRange(jCheckAndGetTimeLocked, j, j2)) {
                return null;
            }
            return getUserDataAndInitializeIfNeededLocked(i, jCheckAndGetTimeLocked).queryEventsForPackage(j, j2, str);
        }
    }

    private static boolean validRange(long j, long j2, long j3) {
        return j2 <= j && j2 < j3;
    }

    private void flushToDiskLocked() {
        int size = this.mUserState.size();
        for (int i = 0; i < size; i++) {
            this.mUserState.valueAt(i).persistActiveStats();
            this.mAppStandby.flushToDisk(this.mUserState.keyAt(i));
        }
        this.mAppStandby.flushDurationsToDisk();
        this.mHandler.removeMessages(1);
    }

    void dump(String[] strArr, PrintWriter printWriter) {
        boolean z;
        boolean z2;
        synchronized (this.mLock) {
            IndentingPrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "  ");
            String str = null;
            if (strArr != null) {
                int i = 0;
                z = false;
                z2 = false;
                while (true) {
                    if (i >= strArr.length) {
                        break;
                    }
                    String str2 = strArr[i];
                    if ("--checkin".equals(str2)) {
                        z = true;
                    } else if ("-c".equals(str2)) {
                        z2 = true;
                    } else if ("flush".equals(str2)) {
                        flushToDiskLocked();
                        printWriter.println("Flushed stats to disk");
                        return;
                    } else {
                        if ("is-app-standby-enabled".equals(str2)) {
                            printWriter.println(this.mAppStandby.mAppIdleEnabled);
                            return;
                        }
                        if (str2 != null && !str2.startsWith("-")) {
                            str = str2;
                            break;
                        }
                    }
                    i++;
                }
            } else {
                z = false;
                z2 = false;
            }
            int size = this.mUserState.size();
            for (int i2 = 0; i2 < size; i2++) {
                int iKeyAt = this.mUserState.keyAt(i2);
                indentingPrintWriter.printPair("user", Integer.valueOf(iKeyAt));
                indentingPrintWriter.println();
                indentingPrintWriter.increaseIndent();
                if (z) {
                    this.mUserState.valueAt(i2).checkin(indentingPrintWriter);
                } else {
                    this.mUserState.valueAt(i2).dump(indentingPrintWriter, str, z2);
                    indentingPrintWriter.println();
                }
                this.mAppStandby.dumpUser(indentingPrintWriter, iKeyAt, str);
                indentingPrintWriter.decreaseIndent();
            }
            if (str == null) {
                printWriter.println();
                this.mAppStandby.dumpState(strArr, printWriter);
            }
            this.mAppTimeLimit.dump(printWriter);
        }
    }

    class H extends Handler {
        public H(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            int i;
            switch (message.what) {
                case 0:
                    UsageStatsService.this.reportEvent((UsageEvents.Event) message.obj, message.arg1);
                    return;
                case 1:
                    UsageStatsService.this.flushToDisk();
                    return;
                case 2:
                    UsageStatsService.this.onUserRemoved(message.arg1);
                    return;
                case 3:
                    int i2 = message.arg1;
                    if (message.arg2 > 2) {
                        i = 1;
                    } else {
                        i = 0;
                    }
                    synchronized (UsageStatsService.this.mUidToKernelCounter) {
                        if (i != UsageStatsService.this.mUidToKernelCounter.get(i2, 0)) {
                            UsageStatsService.this.mUidToKernelCounter.put(i2, i);
                            try {
                                FileUtils.stringToFile(UsageStatsService.KERNEL_COUNTER_FILE, i2 + " " + i);
                            } catch (IOException e) {
                                Slog.w(UsageStatsService.TAG, "Failed to update counter set: " + e);
                            }
                            break;
                        } else {
                            break;
                        }
                    }
                    return;
                default:
                    super.handleMessage(message);
                    return;
            }
        }
    }

    private final class BinderService extends IUsageStatsManager.Stub {
        private BinderService() {
        }

        private boolean hasPermission(String str) {
            int callingUid = Binder.getCallingUid();
            if (callingUid == 1000) {
                return true;
            }
            int iNoteOp = UsageStatsService.this.mAppOps.noteOp(43, callingUid, str);
            return iNoteOp == 3 ? UsageStatsService.this.getContext().checkCallingPermission("android.permission.PACKAGE_USAGE_STATS") == 0 : iNoteOp == 0;
        }

        private boolean hasObserverPermission(String str) {
            int callingUid = Binder.getCallingUid();
            DevicePolicyManagerInternal dpmInternal = UsageStatsService.this.getDpmInternal();
            return callingUid == 1000 || (dpmInternal != null && dpmInternal.isActiveAdminWithPolicy(callingUid, -1)) || UsageStatsService.this.getContext().checkCallingPermission("android.permission.OBSERVE_APP_USAGE") == 0;
        }

        private void checkCallerIsSystemOrSameApp(String str) {
            if (isCallingUidSystem()) {
                return;
            }
            checkCallerIsSameApp(str);
        }

        private void checkCallerIsSameApp(String str) {
            int callingUid = Binder.getCallingUid();
            if (UsageStatsService.this.mPackageManagerInternal.getPackageUid(str, 0, UserHandle.getUserId(callingUid)) != callingUid) {
                throw new SecurityException("Calling uid " + str + " cannot query eventsfor package " + str);
            }
        }

        private boolean isCallingUidSystem() {
            return Binder.getCallingUid() == 1000;
        }

        public ParceledListSlice<UsageStats> queryUsageStats(int i, long j, long j2, String str) {
            if (!hasPermission(str)) {
                return null;
            }
            boolean zShouldObfuscateInstantAppsForCaller = UsageStatsService.this.shouldObfuscateInstantAppsForCaller(Binder.getCallingUid(), UserHandle.getCallingUserId());
            int callingUserId = UserHandle.getCallingUserId();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                List<UsageStats> listQueryUsageStats = UsageStatsService.this.queryUsageStats(callingUserId, i, j, j2, zShouldObfuscateInstantAppsForCaller);
                if (listQueryUsageStats != null) {
                    return new ParceledListSlice<>(listQueryUsageStats);
                }
                return null;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public ParceledListSlice<ConfigurationStats> queryConfigurationStats(int i, long j, long j2, String str) throws RemoteException {
            if (!hasPermission(str)) {
                return null;
            }
            int callingUserId = UserHandle.getCallingUserId();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                List<ConfigurationStats> listQueryConfigurationStats = UsageStatsService.this.queryConfigurationStats(callingUserId, i, j, j2);
                if (listQueryConfigurationStats != null) {
                    return new ParceledListSlice<>(listQueryConfigurationStats);
                }
                return null;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public ParceledListSlice<EventStats> queryEventStats(int i, long j, long j2, String str) throws RemoteException {
            if (!hasPermission(str)) {
                return null;
            }
            int callingUserId = UserHandle.getCallingUserId();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                List<EventStats> listQueryEventStats = UsageStatsService.this.queryEventStats(callingUserId, i, j, j2);
                if (listQueryEventStats != null) {
                    return new ParceledListSlice<>(listQueryEventStats);
                }
                return null;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public UsageEvents queryEvents(long j, long j2, String str) {
            if (hasPermission(str)) {
                boolean zShouldObfuscateInstantAppsForCaller = UsageStatsService.this.shouldObfuscateInstantAppsForCaller(Binder.getCallingUid(), UserHandle.getCallingUserId());
                int callingUserId = UserHandle.getCallingUserId();
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    return UsageStatsService.this.queryEvents(callingUserId, j, j2, zShouldObfuscateInstantAppsForCaller);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
            return null;
        }

        public UsageEvents queryEventsForPackage(long j, long j2, String str) {
            int userId = UserHandle.getUserId(Binder.getCallingUid());
            checkCallerIsSameApp(str);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return UsageStatsService.this.queryEventsForPackage(userId, j, j2, str);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public UsageEvents queryEventsForUser(long j, long j2, int i, String str) {
            if (!hasPermission(str)) {
                return null;
            }
            if (i != UserHandle.getCallingUserId()) {
                UsageStatsService.this.getContext().enforceCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "No permission to query usage stats for this user");
            }
            boolean zShouldObfuscateInstantAppsForCaller = UsageStatsService.this.shouldObfuscateInstantAppsForCaller(Binder.getCallingUid(), UserHandle.getCallingUserId());
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return UsageStatsService.this.queryEvents(i, j, j2, zShouldObfuscateInstantAppsForCaller);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public UsageEvents queryEventsForPackageForUser(long j, long j2, int i, String str, String str2) {
            if (!hasPermission(str2)) {
                return null;
            }
            if (i != UserHandle.getCallingUserId()) {
                UsageStatsService.this.getContext().enforceCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "No permission to query usage stats for this user");
            }
            checkCallerIsSystemOrSameApp(str);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return UsageStatsService.this.queryEventsForPackage(i, j, j2, str2);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public boolean isAppInactive(String str, int i) {
            try {
                int iHandleIncomingUser = ActivityManager.getService().handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), i, false, false, "isAppInactive", (String) null);
                boolean zShouldObfuscateInstantAppsForCaller = UsageStatsService.this.shouldObfuscateInstantAppsForCaller(Binder.getCallingUid(), iHandleIncomingUser);
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    return UsageStatsService.this.mAppStandby.isAppIdleFilteredOrParoled(str, iHandleIncomingUser, SystemClock.elapsedRealtime(), zShouldObfuscateInstantAppsForCaller);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }

        public void setAppInactive(String str, boolean z, int i) {
            try {
                int iHandleIncomingUser = ActivityManager.getService().handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), i, false, true, "setAppInactive", (String) null);
                UsageStatsService.this.getContext().enforceCallingPermission("android.permission.CHANGE_APP_IDLE_STATE", "No permission to change app idle state");
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    if (UsageStatsService.this.mAppStandby.getAppId(str) < 0) {
                        return;
                    }
                    UsageStatsService.this.mAppStandby.setAppIdleAsync(str, z, iHandleIncomingUser);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }

        public int getAppStandbyBucket(String str, String str2, int i) {
            int callingUid = Binder.getCallingUid();
            try {
                int iHandleIncomingUser = ActivityManager.getService().handleIncomingUser(Binder.getCallingPid(), callingUid, i, false, false, "getAppStandbyBucket", (String) null);
                int packageUid = UsageStatsService.this.mPackageManagerInternal.getPackageUid(str, 0, iHandleIncomingUser);
                if (packageUid != callingUid && !hasPermission(str2)) {
                    throw new SecurityException("Don't have permission to query app standby bucket");
                }
                if (packageUid >= 0) {
                    boolean zShouldObfuscateInstantAppsForCaller = UsageStatsService.this.shouldObfuscateInstantAppsForCaller(callingUid, iHandleIncomingUser);
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        return UsageStatsService.this.mAppStandby.getAppStandbyBucket(str, iHandleIncomingUser, SystemClock.elapsedRealtime(), zShouldObfuscateInstantAppsForCaller);
                    } finally {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    }
                }
                throw new IllegalArgumentException("Cannot get standby bucket for non existent package (" + str + ")");
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }

        public void setAppStandbyBucket(String str, int i, int i2) {
            int i3;
            UsageStatsService.this.getContext().enforceCallingPermission("android.permission.CHANGE_APP_IDLE_STATE", "No permission to change app standby state");
            if (i < 10 || i > 50) {
                throw new IllegalArgumentException("Cannot set the standby bucket to " + i);
            }
            int callingUid = Binder.getCallingUid();
            try {
                int iHandleIncomingUser = ActivityManager.getService().handleIncomingUser(Binder.getCallingPid(), callingUid, i2, false, true, "setAppStandbyBucket", (String) null);
                boolean z = callingUid == 0 || callingUid == 2000;
                if (UserHandle.isCore(callingUid)) {
                    i3 = 1024;
                } else {
                    i3 = 1280;
                }
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    int packageUid = UsageStatsService.this.mPackageManagerInternal.getPackageUid(str, DumpState.DUMP_CHANGES, iHandleIncomingUser);
                    if (packageUid == callingUid) {
                        throw new IllegalArgumentException("Cannot set your own standby bucket");
                    }
                    if (packageUid < 0) {
                        throw new IllegalArgumentException("Cannot set standby bucket for non existent package (" + str + ")");
                    }
                    UsageStatsService.this.mAppStandby.setAppStandbyBucket(str, iHandleIncomingUser, i, i3, SystemClock.elapsedRealtime(), z);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }

        public ParceledListSlice<AppStandbyInfo> getAppStandbyBuckets(String str, int i) {
            try {
                int iHandleIncomingUser = ActivityManager.getService().handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), i, false, false, "getAppStandbyBucket", (String) null);
                if (!hasPermission(str)) {
                    throw new SecurityException("Don't have permission to query app standby bucket");
                }
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    List<AppStandbyInfo> appStandbyBuckets = UsageStatsService.this.mAppStandby.getAppStandbyBuckets(iHandleIncomingUser);
                    return appStandbyBuckets == null ? ParceledListSlice.emptyList() : new ParceledListSlice<>(appStandbyBuckets);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }

        public void setAppStandbyBuckets(ParceledListSlice parceledListSlice, int i) {
            int i2;
            UsageStatsService.this.getContext().enforceCallingPermission("android.permission.CHANGE_APP_IDLE_STATE", "No permission to change app standby state");
            int callingUid = Binder.getCallingUid();
            try {
                int iHandleIncomingUser = ActivityManager.getService().handleIncomingUser(Binder.getCallingPid(), callingUid, i, false, true, "setAppStandbyBucket", (String) null);
                boolean z = callingUid == 0 || callingUid == 2000;
                if (z) {
                    i2 = 1024;
                } else {
                    i2 = 1280;
                }
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    long jElapsedRealtime = SystemClock.elapsedRealtime();
                    for (AppStandbyInfo appStandbyInfo : parceledListSlice.getList()) {
                        String str = appStandbyInfo.mPackageName;
                        int i3 = appStandbyInfo.mStandbyBucket;
                        if (i3 < 10 || i3 > 50) {
                            throw new IllegalArgumentException("Cannot set the standby bucket to " + i3);
                        }
                        if (UsageStatsService.this.mPackageManagerInternal.getPackageUid(str, DumpState.DUMP_CHANGES, iHandleIncomingUser) == callingUid) {
                            throw new IllegalArgumentException("Cannot set your own standby bucket");
                        }
                        UsageStatsService.this.mAppStandby.setAppStandbyBucket(str, iHandleIncomingUser, i3, i2, jElapsedRealtime, z);
                    }
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }

        public void whitelistAppTemporarily(String str, long j, int i) throws RemoteException {
            StringBuilder sb = new StringBuilder(32);
            sb.append("from:");
            UserHandle.formatUid(sb, Binder.getCallingUid());
            UsageStatsService.this.mDeviceIdleController.addPowerSaveTempWhitelistApp(str, j, i, sb.toString());
        }

        public void onCarrierPrivilegedAppsChanged() {
            UsageStatsService.this.getContext().enforceCallingOrSelfPermission("android.permission.BIND_CARRIER_SERVICES", "onCarrierPrivilegedAppsChanged can only be called by privileged apps.");
            UsageStatsService.this.mAppStandby.clearCarrierPrivilegedApps();
        }

        protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
            if (DumpUtils.checkDumpAndUsageStatsPermission(UsageStatsService.this.getContext(), UsageStatsService.TAG, printWriter)) {
                UsageStatsService.this.dump(strArr, printWriter);
            }
        }

        public void reportChooserSelection(String str, int i, String str2, String[] strArr, String str3) {
            if (str == null) {
                Slog.w(UsageStatsService.TAG, "Event report user selecting a null package");
                return;
            }
            UsageEvents.Event event = new UsageEvents.Event();
            event.mPackage = str;
            event.mTimeStamp = SystemClock.elapsedRealtime();
            event.mEventType = 9;
            event.mAction = str3;
            event.mContentType = str2;
            event.mContentAnnotations = strArr;
            UsageStatsService.this.mHandler.obtainMessage(0, i, 0, event).sendToTarget();
        }

        public void registerAppUsageObserver(int i, String[] strArr, long j, PendingIntent pendingIntent, String str) {
            if (!hasObserverPermission(str)) {
                throw new SecurityException("Caller doesn't have OBSERVE_APP_USAGE permission");
            }
            if (strArr == null || strArr.length == 0) {
                throw new IllegalArgumentException("Must specify at least one package");
            }
            if (pendingIntent == null) {
                throw new NullPointerException("callbackIntent can't be null");
            }
            int callingUid = Binder.getCallingUid();
            int userId = UserHandle.getUserId(callingUid);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                UsageStatsService.this.registerAppUsageObserver(callingUid, i, strArr, j, pendingIntent, userId);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void unregisterAppUsageObserver(int i, String str) {
            if (!hasObserverPermission(str)) {
                throw new SecurityException("Caller doesn't have OBSERVE_APP_USAGE permission");
            }
            int callingUid = Binder.getCallingUid();
            int userId = UserHandle.getUserId(callingUid);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                UsageStatsService.this.unregisterAppUsageObserver(callingUid, i, userId);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    void registerAppUsageObserver(int i, int i2, String[] strArr, long j, PendingIntent pendingIntent, int i3) {
        this.mAppTimeLimit.addObserver(i, i2, strArr, j, pendingIntent, i3);
    }

    void unregisterAppUsageObserver(int i, int i2, int i3) {
        this.mAppTimeLimit.removeObserver(i, i2, i3);
    }

    private final class LocalService extends UsageStatsManagerInternal {
        private LocalService() {
        }

        public void reportEvent(ComponentName componentName, int i, int i2) {
            if (componentName == null) {
                Slog.w(UsageStatsService.TAG, "Event reported without a component name");
                return;
            }
            UsageEvents.Event event = new UsageEvents.Event();
            event.mPackage = componentName.getPackageName();
            event.mClass = componentName.getClassName();
            event.mTimeStamp = SystemClock.elapsedRealtime();
            event.mEventType = i2;
            UsageStatsService.this.mHandler.obtainMessage(0, i, 0, event).sendToTarget();
        }

        public void reportEvent(String str, int i, int i2) {
            if (str == null) {
                Slog.w(UsageStatsService.TAG, "Event reported without a package name");
                return;
            }
            UsageEvents.Event event = new UsageEvents.Event();
            event.mPackage = str;
            event.mTimeStamp = SystemClock.elapsedRealtime();
            event.mEventType = i2;
            UsageStatsService.this.mHandler.obtainMessage(0, i, 0, event).sendToTarget();
        }

        public void reportConfigurationChange(Configuration configuration, int i) {
            if (configuration == null) {
                Slog.w(UsageStatsService.TAG, "Configuration event reported with a null config");
                return;
            }
            UsageEvents.Event event = new UsageEvents.Event();
            event.mPackage = PackageManagerService.PLATFORM_PACKAGE_NAME;
            event.mTimeStamp = SystemClock.elapsedRealtime();
            event.mEventType = 5;
            event.mConfiguration = new Configuration(configuration);
            UsageStatsService.this.mHandler.obtainMessage(0, i, 0, event).sendToTarget();
        }

        public void reportInterruptiveNotification(String str, String str2, int i) {
            if (str == null || str2 == null) {
                Slog.w(UsageStatsService.TAG, "Event reported without a package name or a channel ID");
                return;
            }
            UsageEvents.Event event = new UsageEvents.Event();
            event.mPackage = str.intern();
            event.mNotificationChannelId = str2.intern();
            event.mTimeStamp = SystemClock.elapsedRealtime();
            event.mEventType = 12;
            UsageStatsService.this.mHandler.obtainMessage(0, i, 0, event).sendToTarget();
        }

        public void reportShortcutUsage(String str, String str2, int i) {
            if (str == null || str2 == null) {
                Slog.w(UsageStatsService.TAG, "Event reported without a package name or a shortcut ID");
                return;
            }
            UsageEvents.Event event = new UsageEvents.Event();
            event.mPackage = str.intern();
            event.mShortcutId = str2.intern();
            event.mTimeStamp = SystemClock.elapsedRealtime();
            event.mEventType = 8;
            UsageStatsService.this.mHandler.obtainMessage(0, i, 0, event).sendToTarget();
        }

        public void reportContentProviderUsage(String str, String str2, int i) {
            UsageStatsService.this.mAppStandby.postReportContentProviderUsage(str, str2, i);
        }

        public boolean isAppIdle(String str, int i, int i2) {
            return UsageStatsService.this.mAppStandby.isAppIdleFiltered(str, i, i2, SystemClock.elapsedRealtime());
        }

        public int getAppStandbyBucket(String str, int i, long j) {
            return UsageStatsService.this.mAppStandby.getAppStandbyBucket(str, i, j, false);
        }

        public int[] getIdleUidsForUser(int i) {
            return UsageStatsService.this.mAppStandby.getIdleUidsForUser(i);
        }

        public boolean isAppIdleParoleOn() {
            return UsageStatsService.this.mAppStandby.isParoledOrCharging();
        }

        public void prepareShutdown() {
            UsageStatsService.this.shutdown();
        }

        public void addAppIdleStateChangeListener(UsageStatsManagerInternal.AppIdleStateChangeListener appIdleStateChangeListener) {
            UsageStatsService.this.mAppStandby.addListener(appIdleStateChangeListener);
            appIdleStateChangeListener.onParoleStateChanged(isAppIdleParoleOn());
        }

        public void removeAppIdleStateChangeListener(UsageStatsManagerInternal.AppIdleStateChangeListener appIdleStateChangeListener) {
            UsageStatsService.this.mAppStandby.removeListener(appIdleStateChangeListener);
        }

        public byte[] getBackupPayload(int i, String str) {
            synchronized (UsageStatsService.this.mLock) {
                try {
                    if (i == 0) {
                        return UsageStatsService.this.getUserDataAndInitializeIfNeededLocked(i, UsageStatsService.this.checkAndGetTimeLocked()).getBackupPayload(str);
                    }
                    return null;
                } catch (Throwable th) {
                    throw th;
                }
            }
        }

        public void applyRestoredPayload(int i, String str, byte[] bArr) {
            synchronized (UsageStatsService.this.mLock) {
                if (i == 0) {
                    try {
                        UsageStatsService.this.getUserDataAndInitializeIfNeededLocked(i, UsageStatsService.this.checkAndGetTimeLocked()).applyRestoredPayload(str, bArr);
                    } catch (Throwable th) {
                        throw th;
                    }
                }
            }
        }

        public List<UsageStats> queryUsageStatsForUser(int i, int i2, long j, long j2, boolean z) {
            return UsageStatsService.this.queryUsageStats(i, i2, j, j2, z);
        }

        public void setLastJobRunTime(String str, int i, long j) {
            UsageStatsService.this.mAppStandby.setLastJobRunTime(str, i, j);
        }

        public long getTimeSinceLastJobRun(String str, int i) {
            return UsageStatsService.this.mAppStandby.getTimeSinceLastJobRun(str, i);
        }

        public void reportAppJobState(String str, int i, int i2, long j) {
        }

        public void onActiveAdminAdded(String str, int i) {
            UsageStatsService.this.mAppStandby.addActiveDeviceAdmin(str, i);
        }

        public void setActiveAdminApps(Set<String> set, int i) {
            UsageStatsService.this.mAppStandby.setActiveAdminApps(set, i);
        }

        public void onAdminDataAvailable() {
            UsageStatsService.this.mAppStandby.onAdminDataAvailable();
        }

        public void reportExemptedSyncScheduled(String str, int i) {
            UsageStatsService.this.mAppStandby.postReportExemptedSyncScheduled(str, i);
        }

        public void reportExemptedSyncStart(String str, int i) {
            UsageStatsService.this.mAppStandby.postReportExemptedSyncStart(str, i);
        }
    }
}
