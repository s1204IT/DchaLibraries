package com.android.server.usage;

import android.app.AppOpsManager;
import android.app.usage.ConfigurationStats;
import android.app.usage.IUsageStatsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManagerInternal;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ParceledListSlice;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.SystemService;
import com.android.server.usage.UserUsageStatsService;
import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class UsageStatsService extends SystemService implements UserUsageStatsService.StatsUpdatedListener {
    static final boolean DEBUG = false;
    private static final long FLUSH_INTERVAL = 1200000;
    static final int MSG_FLUSH_TO_DISK = 1;
    static final int MSG_REMOVE_USER = 2;
    static final int MSG_REPORT_EVENT = 0;
    static final String TAG = "UsageStatsService";
    private static final long TEN_SECONDS = 10000;
    private static final long TIME_CHANGE_THRESHOLD_MILLIS = 2000;
    private static final long TWENTY_MINUTES = 1200000;
    AppOpsManager mAppOps;
    Handler mHandler;
    private final Object mLock;
    long mRealTimeSnapshot;
    long mSystemTimeSnapshot;
    private File mUsageStatsDir;
    UserManager mUserManager;
    private final SparseArray<UserUsageStatsService> mUserState;

    public UsageStatsService(Context context) {
        super(context);
        this.mLock = new Object();
        this.mUserState = new SparseArray<>();
    }

    @Override
    public void onStart() {
        this.mAppOps = (AppOpsManager) getContext().getSystemService("appops");
        this.mUserManager = (UserManager) getContext().getSystemService("user");
        this.mHandler = new H(BackgroundThread.get().getLooper());
        File systemDataDir = new File(Environment.getDataDirectory(), "system");
        this.mUsageStatsDir = new File(systemDataDir, "usagestats");
        this.mUsageStatsDir.mkdirs();
        if (!this.mUsageStatsDir.exists()) {
            throw new IllegalStateException("Usage stats directory does not exist: " + this.mUsageStatsDir.getAbsolutePath());
        }
        getContext().registerReceiver(new UserRemovedReceiver(), new IntentFilter("android.intent.action.USER_REMOVED"));
        synchronized (this.mLock) {
            cleanUpRemovedUsersLocked();
        }
        this.mRealTimeSnapshot = SystemClock.elapsedRealtime();
        this.mSystemTimeSnapshot = System.currentTimeMillis();
        publishLocalService(UsageStatsManagerInternal.class, new LocalService());
        publishBinderService("usagestats", new BinderService());
    }

    private class UserRemovedReceiver extends BroadcastReceiver {
        private UserRemovedReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            int userId;
            if (intent != null && intent.getAction().equals("android.intent.action.USER_REMOVED") && (userId = intent.getIntExtra("android.intent.extra.user_handle", -1)) >= 0) {
                UsageStatsService.this.mHandler.obtainMessage(2, userId, 0).sendToTarget();
            }
        }
    }

    @Override
    public void onStatsUpdated() {
        this.mHandler.sendEmptyMessageDelayed(1, 1200000L);
    }

    private void cleanUpRemovedUsersLocked() {
        List<UserInfo> users = this.mUserManager.getUsers(true);
        if (users == null || users.size() == 0) {
            throw new IllegalStateException("There can't be no users");
        }
        ArraySet<String> toDelete = new ArraySet<>();
        String[] fileNames = this.mUsageStatsDir.list();
        if (fileNames != null) {
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
    }

    private static void deleteRecursively(File f) {
        File[] files = f.listFiles();
        if (files != null) {
            for (File subFile : files) {
                deleteRecursively(subFile);
            }
        }
        if (!f.delete()) {
            Slog.e(TAG, "Failed to delete " + f);
        }
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
        if (Math.abs(actualSystemTime - expectedSystemTime) > TIME_CHANGE_THRESHOLD_MILLIS) {
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
            convertToSystemTimeLocked(event);
            UserUsageStatsService service = getUserDataAndInitializeIfNeededLocked(userId, timeNow);
            service.reportEvent(event);
        }
    }

    void flushToDisk() {
        synchronized (this.mLock) {
            flushToDiskLocked();
        }
    }

    void removeUser(int userId) {
        synchronized (this.mLock) {
            Slog.i(TAG, "Removing user " + userId + " and all data.");
            this.mUserState.remove(userId);
            cleanUpRemovedUsersLocked();
        }
    }

    List<UsageStats> queryUsageStats(int userId, int bucketType, long beginTime, long endTime) {
        List<UsageStats> listQueryUsageStats;
        synchronized (this.mLock) {
            long timeNow = checkAndGetTimeLocked();
            if (!validRange(timeNow, beginTime, endTime)) {
                listQueryUsageStats = null;
            } else {
                UserUsageStatsService service = getUserDataAndInitializeIfNeededLocked(userId, timeNow);
                listQueryUsageStats = service.queryUsageStats(bucketType, beginTime, endTime);
            }
        }
        return listQueryUsageStats;
    }

    List<ConfigurationStats> queryConfigurationStats(int userId, int bucketType, long beginTime, long endTime) {
        List<ConfigurationStats> listQueryConfigurationStats;
        synchronized (this.mLock) {
            long timeNow = checkAndGetTimeLocked();
            if (!validRange(timeNow, beginTime, endTime)) {
                listQueryConfigurationStats = null;
            } else {
                UserUsageStatsService service = getUserDataAndInitializeIfNeededLocked(userId, timeNow);
                listQueryConfigurationStats = service.queryConfigurationStats(bucketType, beginTime, endTime);
            }
        }
        return listQueryConfigurationStats;
    }

    UsageEvents queryEvents(int userId, long beginTime, long endTime) {
        UsageEvents usageEventsQueryEvents;
        synchronized (this.mLock) {
            long timeNow = checkAndGetTimeLocked();
            if (!validRange(timeNow, beginTime, endTime)) {
                usageEventsQueryEvents = null;
            } else {
                UserUsageStatsService service = getUserDataAndInitializeIfNeededLocked(userId, timeNow);
                usageEventsQueryEvents = service.queryEvents(beginTime, endTime);
            }
        }
        return usageEventsQueryEvents;
    }

    private static boolean validRange(long currentTime, long beginTime, long endTime) {
        if (beginTime > currentTime || beginTime >= endTime) {
            return DEBUG;
        }
        return true;
    }

    private void flushToDiskLocked() {
        int userCount = this.mUserState.size();
        for (int i = 0; i < userCount; i++) {
            UserUsageStatsService service = this.mUserState.valueAt(i);
            service.persistActiveStats();
        }
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
                }
                idpw.decreaseIndent();
            }
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
                    UsageStatsService.this.removeUser(msg.arg1);
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    }

    private class BinderService extends IUsageStatsManager.Stub {
        private BinderService() {
        }

        private boolean hasPermission(String callingPackage) {
            int callingUid = Binder.getCallingUid();
            if (callingUid == 1000) {
                return true;
            }
            int mode = UsageStatsService.this.mAppOps.checkOp(43, callingUid, callingPackage);
            if (mode == 3) {
                if (UsageStatsService.this.getContext().checkCallingPermission("android.permission.PACKAGE_USAGE_STATS") != 0) {
                    return UsageStatsService.DEBUG;
                }
                return true;
            }
            if (mode != 0) {
                return UsageStatsService.DEBUG;
            }
            return true;
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

        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (UsageStatsService.this.getContext().checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
                pw.println("Permission Denial: can't dump UsageStats from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " without permission android.permission.DUMP");
            } else {
                UsageStatsService.this.dump(args, pw);
            }
        }
    }

    private class LocalService extends UsageStatsManagerInternal {
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

        public void prepareShutdown() {
            UsageStatsService.this.shutdown();
        }
    }
}
