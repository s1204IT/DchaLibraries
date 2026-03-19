package com.android.server.content;

import android.R;
import android.accounts.Account;
import android.accounts.AccountAndUser;
import android.accounts.AccountManager;
import android.accounts.AccountManagerInternal;
import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.app.usage.UsageStatsManagerInternal;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ISyncAdapter;
import android.content.ISyncAdapterUnsyncableAccountCallback;
import android.content.ISyncContext;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.PeriodicSync;
import android.content.ServiceConnection;
import android.content.SyncActivityTooManyDeletes;
import android.content.SyncAdapterType;
import android.content.SyncAdaptersCache;
import android.content.SyncInfo;
import android.content.SyncResult;
import android.content.SyncStatusInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ProviderInfo;
import android.content.pm.RegisteredServicesCache;
import android.content.pm.RegisteredServicesCacheListener;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.os.BenesseExtension;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.WorkSource;
import android.provider.Settings;
import android.text.format.Time;
import android.util.EventLog;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.app.IBatteryStats;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.function.QuadConsumer;
import com.android.server.DeviceIdleController;
import com.android.server.LocalServices;
import com.android.server.accounts.AccountManagerService;
import com.android.server.backup.AccountSyncSettingsBackupHelper;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.content.SyncManager;
import com.android.server.content.SyncStorageEngine;
import com.android.server.job.JobSchedulerInternal;
import com.android.server.pm.DumpState;
import com.android.server.slice.SliceClientPermissions;
import com.google.android.collect.Lists;
import com.google.android.collect.Maps;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Predicate;

public class SyncManager {
    private static final boolean DEBUG_ACCOUNT_ACCESS = false;
    private static final int DELAY_RETRY_SYNC_IN_PROGRESS_IN_SECONDS = 10;
    private static final String HANDLE_SYNC_ALARM_WAKE_LOCK = "SyncManagerHandleSyncAlarm";
    private static final int MAX_SYNC_JOB_ID = 110000;
    private static final int MIN_SYNC_JOB_ID = 100000;
    private static final int SYNC_ADAPTER_CONNECTION_FLAGS = 21;
    private static final long SYNC_DELAY_ON_CONFLICT = 10000;
    private static final long SYNC_DELAY_ON_LOW_STORAGE = 3600000;
    private static final String SYNC_LOOP_WAKE_LOCK = "SyncLoopWakeLock";
    private static final int SYNC_MONITOR_PROGRESS_THRESHOLD_BYTES = 10;
    private static final long SYNC_MONITOR_WINDOW_LENGTH_MILLIS = 60000;
    private static final int SYNC_OP_STATE_INVALID = 1;
    private static final int SYNC_OP_STATE_INVALID_NO_ACCOUNT_ACCESS = 2;
    private static final int SYNC_OP_STATE_VALID = 0;
    private static final String SYNC_WAKE_LOCK_PREFIX = "*sync*/";
    static final String TAG = "SyncManager";

    @GuardedBy("SyncManager.class")
    private static SyncManager sInstance;
    private final AccountManager mAccountManager;
    private final AccountManagerInternal mAccountManagerInternal;
    private final IBatteryStats mBatteryStats;
    private ConnectivityManager mConnManagerDoNotUseDirectly;
    private final SyncManagerConstants mConstants;
    private Context mContext;
    private volatile PowerManager.WakeLock mHandleAlarmWakeLock;
    private JobScheduler mJobScheduler;
    private JobSchedulerInternal mJobSchedulerInternal;
    private final SyncLogger mLogger;
    private final NotificationManager mNotificationMgr;
    private final PackageManagerInternal mPackageManagerInternal;
    private final PowerManager mPowerManager;
    private volatile boolean mProvisioned;
    private final Random mRand;
    protected final SyncAdaptersCache mSyncAdapters;
    private final SyncHandler mSyncHandler;
    private SyncJobService mSyncJobService;
    private volatile PowerManager.WakeLock mSyncManagerWakeLock;
    private SyncStorageEngine mSyncStorageEngine;
    private final HandlerThread mThread;
    private final UserManager mUserManager;
    private static final boolean ENABLE_SUSPICIOUS_CHECK = Build.IS_DEBUGGABLE;
    private static final long LOCAL_SYNC_DELAY = SystemProperties.getLong("sync.local_sync_delay", 30000);
    private static final AccountAndUser[] INITIAL_ACCOUNTS_ARRAY = new AccountAndUser[0];
    private static final Comparator<SyncOperation> sOpDumpComparator = new Comparator() {
        @Override
        public final int compare(Object obj, Object obj2) {
            return SyncManager.lambda$static$6((SyncOperation) obj, (SyncOperation) obj2);
        }
    };
    private static final Comparator<SyncOperation> sOpRuntimeComparator = new Comparator() {
        @Override
        public final int compare(Object obj, Object obj2) {
            return SyncManager.lambda$static$7((SyncOperation) obj, (SyncOperation) obj2);
        }
    };
    private volatile AccountAndUser[] mRunningAccounts = INITIAL_ACCOUNTS_ARRAY;
    private volatile boolean mDataConnectionIsConnected = false;
    private volatile boolean mStorageIsLow = false;
    private volatile boolean mDeviceIsIdle = false;
    private volatile boolean mReportedSyncActive = false;
    protected final ArrayList<ActiveSyncContext> mActiveSyncContexts = Lists.newArrayList();
    private final BroadcastReceiver mStorageIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.DEVICE_STORAGE_LOW".equals(action)) {
                if (Log.isLoggable("SyncManager", 2)) {
                    Slog.v("SyncManager", "Internal storage is low.");
                }
                SyncManager.this.mStorageIsLow = true;
                SyncManager.this.cancelActiveSync(SyncStorageEngine.EndPoint.USER_ALL_PROVIDER_ALL_ACCOUNTS_ALL, null, "storage low");
                return;
            }
            if ("android.intent.action.DEVICE_STORAGE_OK".equals(action)) {
                if (Log.isLoggable("SyncManager", 2)) {
                    Slog.v("SyncManager", "Internal storage is ok.");
                }
                SyncManager.this.mStorageIsLow = false;
                SyncManager.this.rescheduleSyncs(SyncStorageEngine.EndPoint.USER_ALL_PROVIDER_ALL_ACCOUNTS_ALL, "storage ok");
            }
        }
    };
    private final BroadcastReceiver mBootCompletedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            SyncManager.this.mBootCompleted = true;
            SyncManager.this.verifyJobScheduler();
            SyncManager.this.mSyncHandler.onBootCompleted();
        }
    };
    private final BroadcastReceiver mAccountsUpdatedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            SyncManager.this.updateRunningAccounts(new SyncStorageEngine.EndPoint(null, null, getSendingUserId()));
        }
    };
    private BroadcastReceiver mConnectivityIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean z = SyncManager.this.mDataConnectionIsConnected;
            SyncManager.this.mDataConnectionIsConnected = SyncManager.this.readDataConnectionState();
            if (SyncManager.this.mDataConnectionIsConnected && !z) {
                if (Log.isLoggable("SyncManager", 2)) {
                    Slog.v("SyncManager", "Reconnection detected: clearing all backoffs");
                }
                SyncManager.this.clearAllBackoffs("network reconnect");
            }
        }
    };
    private BroadcastReceiver mShutdownIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.w("SyncManager", "Writing sync state before shutdown...");
            SyncManager.this.getSyncStorageEngine().writeAllState();
            SyncManager.this.mLogger.log(SyncManager.this.getJobStats());
            SyncManager.this.mLogger.log("Shutting down.");
        }
    };
    private final BroadcastReceiver mOtherIntentsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.TIME_SET".equals(intent.getAction())) {
                SyncManager.this.mSyncStorageEngine.setClockValid();
            }
        }
    };
    private BroadcastReceiver mUserIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int intExtra = intent.getIntExtra("android.intent.extra.user_handle", -10000);
            if (intExtra == -10000) {
                return;
            }
            if ("android.intent.action.USER_REMOVED".equals(action)) {
                SyncManager.this.onUserRemoved(intExtra);
            } else if ("android.intent.action.USER_UNLOCKED".equals(action)) {
                SyncManager.this.onUserUnlocked(intExtra);
            } else if ("android.intent.action.USER_STOPPED".equals(action)) {
                SyncManager.this.onUserStopped(intExtra);
            }
        }
    };
    private volatile boolean mBootCompleted = false;
    private volatile boolean mJobServiceReady = false;

    interface OnReadyCallback {
        void onReady();
    }

    static boolean access$1876(SyncManager syncManager, int i) {
        ?? r2 = (byte) (i | (syncManager.mProvisioned ? 1 : 0));
        syncManager.mProvisioned = r2;
        return r2;
    }

    private boolean isJobIdInUseLockedH(int i, List<JobInfo> list) {
        Iterator<JobInfo> it = list.iterator();
        while (it.hasNext()) {
            if (it.next().getId() == i) {
                return true;
            }
        }
        Iterator<ActiveSyncContext> it2 = this.mActiveSyncContexts.iterator();
        while (it2.hasNext()) {
            if (it2.next().mSyncOperation.jobId == i) {
                return true;
            }
        }
        return false;
    }

    private int getUnusedJobIdH() {
        int iNextInt;
        do {
            iNextInt = MIN_SYNC_JOB_ID + this.mRand.nextInt(10000);
        } while (isJobIdInUseLockedH(iNextInt, this.mJobSchedulerInternal.getSystemScheduledPendingJobs()));
        return iNextInt;
    }

    private List<SyncOperation> getAllPendingSyncs() {
        verifyJobScheduler();
        List<JobInfo> systemScheduledPendingJobs = this.mJobSchedulerInternal.getSystemScheduledPendingJobs();
        ArrayList arrayList = new ArrayList(systemScheduledPendingJobs.size());
        Iterator<JobInfo> it = systemScheduledPendingJobs.iterator();
        while (it.hasNext()) {
            SyncOperation syncOperationMaybeCreateFromJobExtras = SyncOperation.maybeCreateFromJobExtras(it.next().getExtras());
            if (syncOperationMaybeCreateFromJobExtras != null) {
                arrayList.add(syncOperationMaybeCreateFromJobExtras);
            }
        }
        return arrayList;
    }

    private List<UserInfo> getAllUsers() {
        return this.mUserManager.getUsers();
    }

    private boolean containsAccountAndUser(AccountAndUser[] accountAndUserArr, Account account, int i) {
        for (int i2 = 0; i2 < accountAndUserArr.length; i2++) {
            if (accountAndUserArr[i2].userId == i && accountAndUserArr[i2].account.equals(account)) {
                return true;
            }
        }
        return false;
    }

    private void updateRunningAccounts(SyncStorageEngine.EndPoint endPoint) {
        if (Log.isLoggable("SyncManager", 2)) {
            Slog.v("SyncManager", "sending MESSAGE_ACCOUNTS_UPDATED");
        }
        Message messageObtainMessage = this.mSyncHandler.obtainMessage(9);
        messageObtainMessage.obj = endPoint;
        messageObtainMessage.sendToTarget();
    }

    private void doDatabaseCleanup() {
        for (UserInfo userInfo : this.mUserManager.getUsers(true)) {
            if (!userInfo.partial) {
                this.mSyncStorageEngine.doDatabaseCleanup(AccountManagerService.getSingleton().getAccounts(userInfo.id, this.mContext.getOpPackageName()), userInfo.id);
            }
        }
    }

    private void clearAllBackoffs(String str) {
        this.mSyncStorageEngine.clearAllBackoffsLocked();
        rescheduleSyncs(SyncStorageEngine.EndPoint.USER_ALL_PROVIDER_ALL_ACCOUNTS_ALL, str);
    }

    private boolean readDataConnectionState() {
        NetworkInfo activeNetworkInfo = getConnectivityManager().getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private String getJobStats() {
        JobSchedulerInternal jobSchedulerInternal = (JobSchedulerInternal) LocalServices.getService(JobSchedulerInternal.class);
        StringBuilder sb = new StringBuilder();
        sb.append("JobStats: ");
        sb.append(jobSchedulerInternal == null ? "(JobSchedulerInternal==null)" : jobSchedulerInternal.getPersistStats().toString());
        return sb.toString();
    }

    private ConnectivityManager getConnectivityManager() {
        ConnectivityManager connectivityManager;
        synchronized (this) {
            if (this.mConnManagerDoNotUseDirectly == null) {
                this.mConnManagerDoNotUseDirectly = (ConnectivityManager) this.mContext.getSystemService("connectivity");
            }
            connectivityManager = this.mConnManagerDoNotUseDirectly;
        }
        return connectivityManager;
    }

    private void cleanupJobs() {
        this.mSyncHandler.postAtFrontOfQueue(new Runnable() {
            @Override
            public void run() {
                List<SyncOperation> allPendingSyncs = SyncManager.this.getAllPendingSyncs();
                HashSet hashSet = new HashSet();
                for (SyncOperation syncOperation : allPendingSyncs) {
                    if (!hashSet.contains(syncOperation.key)) {
                        hashSet.add(syncOperation.key);
                        for (SyncOperation syncOperation2 : allPendingSyncs) {
                            if (syncOperation != syncOperation2 && syncOperation.key.equals(syncOperation2.key)) {
                                SyncManager.this.mLogger.log("Removing duplicate sync: ", syncOperation2);
                                SyncManager.this.cancelJob(syncOperation2, "cleanupJobs() x=" + syncOperation + " y=" + syncOperation2);
                            }
                        }
                    }
                }
            }
        });
    }

    private synchronized void verifyJobScheduler() {
        if (this.mJobScheduler != null) {
            return;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            if (Log.isLoggable("SyncManager", 2)) {
                Log.d("SyncManager", "initializing JobScheduler object.");
            }
            this.mJobScheduler = (JobScheduler) this.mContext.getSystemService("jobscheduler");
            this.mJobSchedulerInternal = (JobSchedulerInternal) LocalServices.getService(JobSchedulerInternal.class);
            List<JobInfo> allPendingJobs = this.mJobScheduler.getAllPendingJobs();
            Iterator<JobInfo> it = allPendingJobs.iterator();
            int i = 0;
            int i2 = 0;
            while (it.hasNext()) {
                SyncOperation syncOperationMaybeCreateFromJobExtras = SyncOperation.maybeCreateFromJobExtras(it.next().getExtras());
                if (syncOperationMaybeCreateFromJobExtras != null) {
                    if (syncOperationMaybeCreateFromJobExtras.isPeriodic) {
                        i++;
                    } else {
                        i2++;
                        this.mSyncStorageEngine.markPending(syncOperationMaybeCreateFromJobExtras.target, true);
                    }
                }
            }
            String str = "Loaded persisted syncs: " + i + " periodic syncs, " + i2 + " oneshot syncs, " + allPendingJobs.size() + " total system server jobs, " + getJobStats();
            Slog.i("SyncManager", str);
            this.mLogger.log(str);
            cleanupJobs();
            if (ENABLE_SUSPICIOUS_CHECK && i == 0 && likelyHasPeriodicSyncs()) {
                Slog.wtf("SyncManager", "Device booted with no persisted periodic syncs: " + str);
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private boolean likelyHasPeriodicSyncs() {
        try {
            return this.mSyncStorageEngine.getAuthorityCount() >= 6;
        } catch (Throwable th) {
            return false;
        }
    }

    private JobScheduler getJobScheduler() {
        verifyJobScheduler();
        return this.mJobScheduler;
    }

    public SyncManager(Context context, boolean z) {
        synchronized (SyncManager.class) {
            if (sInstance == null) {
                sInstance = this;
            } else {
                Slog.wtf("SyncManager", "SyncManager instantiated multiple times");
            }
        }
        this.mContext = context;
        this.mLogger = SyncLogger.getInstance();
        SyncStorageEngine.init(context, BackgroundThread.get().getLooper());
        this.mSyncStorageEngine = SyncStorageEngine.getSingleton();
        this.mSyncStorageEngine.setOnSyncRequestListener(new SyncStorageEngine.OnSyncRequestListener() {
            @Override
            public void onSyncRequest(SyncStorageEngine.EndPoint endPoint, int i, Bundle bundle, int i2) {
                SyncManager.this.scheduleSync(endPoint.account, endPoint.userId, i, endPoint.provider, bundle, -2, i2);
            }
        });
        this.mSyncStorageEngine.setPeriodicSyncAddedListener(new SyncStorageEngine.PeriodicSyncAddedListener() {
            @Override
            public void onPeriodicSyncAdded(SyncStorageEngine.EndPoint endPoint, Bundle bundle, long j, long j2) {
                SyncManager.this.updateOrAddPeriodicSync(endPoint, j, j2, bundle);
            }
        });
        this.mSyncStorageEngine.setOnAuthorityRemovedListener(new SyncStorageEngine.OnAuthorityRemovedListener() {
            @Override
            public void onAuthorityRemoved(SyncStorageEngine.EndPoint endPoint) {
                SyncManager.this.removeSyncsForAuthority(endPoint, "onAuthorityRemoved");
            }
        });
        this.mSyncAdapters = new SyncAdaptersCache(this.mContext);
        this.mThread = new HandlerThread("SyncManager", 10);
        this.mThread.start();
        this.mSyncHandler = new SyncHandler(this.mThread.getLooper());
        this.mSyncAdapters.setListener(new RegisteredServicesCacheListener<SyncAdapterType>() {
            public void onServiceChanged(SyncAdapterType syncAdapterType, int i, boolean z2) {
                if (!z2) {
                    SyncManager.this.scheduleSync(null, -1, -3, syncAdapterType.authority, null, -2, 0);
                }
            }
        }, this.mSyncHandler);
        this.mRand = new Random(System.currentTimeMillis());
        this.mConstants = new SyncManagerConstants(context);
        context.registerReceiver(this.mConnectivityIntentReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
        if (!z) {
            IntentFilter intentFilter = new IntentFilter("android.intent.action.BOOT_COMPLETED");
            intentFilter.setPriority(1000);
            context.registerReceiver(this.mBootCompletedReceiver, intentFilter);
        }
        IntentFilter intentFilter2 = new IntentFilter("android.intent.action.DEVICE_STORAGE_LOW");
        intentFilter2.addAction("android.intent.action.DEVICE_STORAGE_OK");
        context.registerReceiver(this.mStorageIntentReceiver, intentFilter2);
        IntentFilter intentFilter3 = new IntentFilter("android.intent.action.ACTION_SHUTDOWN");
        intentFilter3.setPriority(100);
        context.registerReceiver(this.mShutdownIntentReceiver, intentFilter3);
        IntentFilter intentFilter4 = new IntentFilter();
        intentFilter4.addAction("android.intent.action.USER_REMOVED");
        intentFilter4.addAction("android.intent.action.USER_UNLOCKED");
        intentFilter4.addAction("android.intent.action.USER_STOPPED");
        this.mContext.registerReceiverAsUser(this.mUserIntentReceiver, UserHandle.ALL, intentFilter4, null, null);
        context.registerReceiver(this.mOtherIntentsReceiver, new IntentFilter("android.intent.action.TIME_SET"));
        Handler handler = null;
        if (!z) {
            this.mNotificationMgr = (NotificationManager) context.getSystemService("notification");
        } else {
            this.mNotificationMgr = null;
        }
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        this.mUserManager = (UserManager) this.mContext.getSystemService("user");
        this.mAccountManager = (AccountManager) this.mContext.getSystemService("account");
        this.mAccountManagerInternal = (AccountManagerInternal) LocalServices.getService(AccountManagerInternal.class);
        this.mPackageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        this.mAccountManagerInternal.addOnAppPermissionChangeListener(new AccountManagerInternal.OnAppPermissionChangeListener() {
            public final void onAppPermissionChanged(Account account, int i) {
                SyncManager.lambda$new$0(this.f$0, account, i);
            }
        });
        this.mBatteryStats = IBatteryStats.Stub.asInterface(ServiceManager.getService("batterystats"));
        this.mHandleAlarmWakeLock = this.mPowerManager.newWakeLock(1, HANDLE_SYNC_ALARM_WAKE_LOCK);
        this.mHandleAlarmWakeLock.setReferenceCounted(false);
        this.mSyncManagerWakeLock = this.mPowerManager.newWakeLock(1, SYNC_LOOP_WAKE_LOCK);
        this.mSyncManagerWakeLock.setReferenceCounted(false);
        this.mProvisioned = isDeviceProvisioned();
        if (!this.mProvisioned) {
            final ContentResolver contentResolver = context.getContentResolver();
            ContentObserver contentObserver = new ContentObserver(handler) {
                @Override
                public void onChange(boolean z2) {
                    SyncManager.access$1876(SyncManager.this, SyncManager.this.isDeviceProvisioned() ? 1 : 0);
                    if (SyncManager.this.mProvisioned) {
                        SyncManager.this.mSyncHandler.onDeviceProvisioned();
                        contentResolver.unregisterContentObserver(this);
                    }
                }
            };
            synchronized (this.mSyncHandler) {
                contentResolver.registerContentObserver(Settings.Global.getUriFor("device_provisioned"), false, contentObserver);
                this.mProvisioned |= isDeviceProvisioned();
                if (this.mProvisioned) {
                    contentResolver.unregisterContentObserver(contentObserver);
                }
            }
        }
        if (!z) {
            this.mContext.registerReceiverAsUser(this.mAccountsUpdatedReceiver, UserHandle.ALL, new IntentFilter("android.accounts.LOGIN_ACCOUNTS_CHANGED"), null, null);
        }
        final Intent intent = new Intent(this.mContext, (Class<?>) SyncJobService.class);
        intent.putExtra(SyncJobService.EXTRA_MESSENGER, new Messenger(this.mSyncHandler));
        new Handler(this.mContext.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                SyncManager.this.mContext.startService(intent);
            }
        });
        whiteListExistingSyncAdaptersIfNeeded();
        this.mLogger.log("Sync manager initialized: " + Build.FINGERPRINT);
    }

    public static void lambda$new$0(SyncManager syncManager, Account account, int i) {
        if (syncManager.mAccountManagerInternal.hasAccountAccess(account, i)) {
            syncManager.scheduleSync(account, UserHandle.getUserId(i), -2, null, null, 3, 0);
        }
    }

    public void onStartUser(final int i) {
        this.mSyncHandler.post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.mLogger.log("onStartUser: user=", Integer.valueOf(i));
            }
        });
    }

    public void onUnlockUser(final int i) {
        this.mSyncHandler.post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.mLogger.log("onUnlockUser: user=", Integer.valueOf(i));
            }
        });
    }

    public void onStopUser(final int i) {
        this.mSyncHandler.post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.mLogger.log("onStopUser: user=", Integer.valueOf(i));
            }
        });
    }

    public void onBootPhase(int i) {
        if (i == 550) {
            this.mConstants.start();
        }
    }

    private void whiteListExistingSyncAdaptersIfNeeded() {
        SyncManager syncManager = this;
        if (!syncManager.mSyncStorageEngine.shouldGrantSyncAdaptersAccountAccess()) {
            return;
        }
        List users = syncManager.mUserManager.getUsers(true);
        int size = users.size();
        int i = 0;
        while (i < size) {
            UserHandle userHandle = ((UserInfo) users.get(i)).getUserHandle();
            int identifier = userHandle.getIdentifier();
            for (RegisteredServicesCache.ServiceInfo serviceInfo : syncManager.mSyncAdapters.getAllServices(identifier)) {
                String packageName = serviceInfo.componentName.getPackageName();
                Account[] accountsByTypeAsUser = syncManager.mAccountManager.getAccountsByTypeAsUser(((SyncAdapterType) serviceInfo.type).accountType, userHandle);
                int length = accountsByTypeAsUser.length;
                int i2 = 0;
                while (i2 < length) {
                    Account account = accountsByTypeAsUser[i2];
                    if (!syncManager.canAccessAccount(account, packageName, identifier)) {
                        syncManager.mAccountManager.updateAppPermission(account, "com.android.AccountManager.ACCOUNT_ACCESS_TOKEN_TYPE", serviceInfo.uid, true);
                    }
                    i2++;
                    syncManager = this;
                }
                syncManager = this;
            }
            i++;
            syncManager = this;
        }
    }

    private boolean isDeviceProvisioned() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) != 0;
    }

    private long jitterize(long j, long j2) {
        Random random = new Random(SystemClock.elapsedRealtime());
        long j3 = j2 - j;
        if (j3 > 2147483647L) {
            throw new IllegalArgumentException("the difference between the maxValue and the minValue must be less than 2147483647");
        }
        return j + ((long) random.nextInt((int) j3));
    }

    public SyncStorageEngine getSyncStorageEngine() {
        return this.mSyncStorageEngine;
    }

    private int getIsSyncable(Account account, int i, String str) {
        int isSyncable = this.mSyncStorageEngine.getIsSyncable(account, i, str);
        UserInfo userInfo = UserManager.get(this.mContext).getUserInfo(i);
        if (userInfo == null || !userInfo.isRestricted()) {
            return isSyncable;
        }
        RegisteredServicesCache.ServiceInfo serviceInfo = this.mSyncAdapters.getServiceInfo(SyncAdapterType.newKey(str, account.type), i);
        if (serviceInfo == null) {
            return 0;
        }
        try {
            PackageInfo packageInfo = AppGlobals.getPackageManager().getPackageInfo(serviceInfo.componentName.getPackageName(), 0, i);
            if (packageInfo == null || packageInfo.restrictedAccountType == null || !packageInfo.restrictedAccountType.equals(account.type)) {
                return 0;
            }
            return isSyncable;
        } catch (RemoteException e) {
            return 0;
        }
    }

    private void setAuthorityPendingState(SyncStorageEngine.EndPoint endPoint) {
        for (SyncOperation syncOperation : getAllPendingSyncs()) {
            if (!syncOperation.isPeriodic && syncOperation.target.matchesSpec(endPoint)) {
                getSyncStorageEngine().markPending(endPoint, true);
                return;
            }
        }
        getSyncStorageEngine().markPending(endPoint, false);
    }

    public void scheduleSync(Account account, int i, int i2, String str, Bundle bundle, int i3, int i4) {
        scheduleSync(account, i, i2, str, bundle, i3, 0L, true, i4);
    }

    private void scheduleSync(Account account, int i, final int i2, String str, Bundle bundle, final int i3, final long j, boolean z, final int i4) {
        int i5;
        AccountAndUser[] accountAndUserArr;
        AccountAndUser[] accountAndUserArr2;
        int i6;
        int i7;
        AccountAndUser[] accountAndUserArr3;
        Bundle bundle2;
        int i8;
        boolean z2;
        SyncManager syncManager;
        int i9;
        int i10;
        boolean z3;
        int i11;
        AccountAndUser[] accountAndUserArr4;
        Bundle bundle3;
        int i12;
        AccountAndUser accountAndUser;
        boolean z4;
        int i13;
        int i14;
        Iterator it;
        SyncManager syncManager2;
        int i15;
        int i16;
        Bundle bundle4;
        SyncManager syncManager3 = this;
        int i17 = i;
        String str2 = str;
        int i18 = i3;
        boolean z5 = z;
        boolean zIsLoggable = Log.isLoggable("SyncManager", 2);
        Bundle bundle5 = bundle == null ? new Bundle() : bundle;
        if (zIsLoggable) {
            StringBuilder sb = new StringBuilder();
            sb.append("one-time sync for: ");
            sb.append(account);
            sb.append(" ");
            sb.append(bundle5.toString());
            sb.append(" ");
            sb.append(str2);
            sb.append(" reason=");
            i5 = i2;
            sb.append(i5);
            sb.append(" checkIfAccountReady=");
            sb.append(z5);
            sb.append(" syncExemptionFlag=");
            sb.append(i4);
            Log.d("SyncManager", sb.toString());
        } else {
            i5 = i2;
        }
        if (account == null) {
            accountAndUserArr = syncManager3.mRunningAccounts;
        } else {
            if (i17 == -1) {
                AccountAndUser[] accountAndUserArr5 = syncManager3.mRunningAccounts;
                int length = accountAndUserArr5.length;
                int i19 = 0;
                AccountAndUser[] accountAndUserArr6 = null;
                while (i19 < length) {
                    AccountAndUser accountAndUser2 = accountAndUserArr5[i19];
                    int i20 = length;
                    if (account.equals(accountAndUser2.account)) {
                        accountAndUserArr6 = (AccountAndUser[]) ArrayUtils.appendElement(AccountAndUser.class, accountAndUserArr6, accountAndUser2);
                    }
                    i19++;
                    length = i20;
                }
                accountAndUserArr2 = accountAndUserArr6;
                if (!ArrayUtils.isEmpty(accountAndUserArr2)) {
                    if (zIsLoggable) {
                        Slog.v("SyncManager", "scheduleSync: no accounts configured, dropping");
                        return;
                    }
                    return;
                }
                boolean z6 = bundle5.getBoolean("upload", false);
                boolean z7 = bundle5.getBoolean("force", false);
                if (z7) {
                    i6 = 1;
                    bundle5.putBoolean("ignore_backoff", true);
                    bundle5.putBoolean("ignore_settings", true);
                } else {
                    i6 = 1;
                }
                boolean z8 = false;
                boolean z9 = bundle5.getBoolean("ignore_settings", false);
                int i21 = z6 ? i6 : z7 ? 3 : str2 == null ? 2 : bundle5.containsKey("feed") ? 5 : 0;
                int length2 = accountAndUserArr2.length;
                int i22 = 0;
                while (i22 < length2) {
                    AccountAndUser accountAndUser3 = accountAndUserArr2[i22];
                    if (i17 < 0 || accountAndUser3.userId < 0 || i17 == accountAndUser3.userId) {
                        HashSet hashSet = new HashSet();
                        Iterator it2 = syncManager3.mSyncAdapters.getAllServices(accountAndUser3.userId).iterator();
                        while (it2.hasNext()) {
                            hashSet.add(((SyncAdapterType) ((RegisteredServicesCache.ServiceInfo) it2.next()).type).authority);
                        }
                        if (str2 != null) {
                            boolean zContains = hashSet.contains(str2);
                            hashSet.clear();
                            if (zContains) {
                                hashSet.add(str2);
                            }
                        }
                        Iterator it3 = hashSet.iterator();
                        while (it3.hasNext()) {
                            final String str3 = (String) it3.next();
                            int i23 = i21;
                            int i24 = length2;
                            int iComputeSyncable = syncManager3.computeSyncable(accountAndUser3.account, accountAndUser3.userId, str3, !z5);
                            if (iComputeSyncable == 0) {
                                i21 = i23;
                                length2 = i24;
                            } else {
                                Iterator it4 = it3;
                                RegisteredServicesCache.ServiceInfo serviceInfo = syncManager3.mSyncAdapters.getServiceInfo(SyncAdapterType.newKey(str3, accountAndUser3.account.type), accountAndUser3.userId);
                                if (serviceInfo != null) {
                                    int i25 = serviceInfo.uid;
                                    if (iComputeSyncable == 3) {
                                        if (zIsLoggable) {
                                            Slog.v("SyncManager", "    Not scheduling sync operation: isSyncable == SYNCABLE_NO_ACCOUNT_ACCESS");
                                        }
                                        final Bundle bundle6 = new Bundle(bundle5);
                                        String packageName = serviceInfo.componentName.getPackageName();
                                        try {
                                        } catch (IllegalArgumentException e) {
                                            accountAndUserArr4 = accountAndUserArr2;
                                            bundle3 = bundle5;
                                            i12 = i22;
                                            accountAndUser = accountAndUser3;
                                            z4 = z5;
                                            i13 = i23;
                                            i14 = i24;
                                            it = it4;
                                        }
                                        if (syncManager3.mPackageManagerInternal.wasPackageEverLaunched(packageName, i17)) {
                                            i13 = i23;
                                            i14 = i24;
                                            final SyncManager syncManager4 = syncManager3;
                                            final AccountAndUser accountAndUser4 = accountAndUser3;
                                            it = it4;
                                            final int i26 = i17;
                                            accountAndUserArr4 = accountAndUserArr2;
                                            final int i27 = i5;
                                            bundle3 = bundle5;
                                            final int i28 = i18;
                                            i12 = i22;
                                            accountAndUser = accountAndUser3;
                                            z4 = z5;
                                            syncManager3.mAccountManagerInternal.requestAccountAccess(accountAndUser3.account, packageName, i17, new RemoteCallback(new RemoteCallback.OnResultListener() {
                                                public final void onResult(Bundle bundle7) {
                                                    SyncManager.lambda$scheduleSync$4(this.f$0, accountAndUser4, i26, i27, str3, bundle6, i28, j, i4, bundle7);
                                                }
                                            }));
                                            i5 = i2;
                                            accountAndUser3 = accountAndUser;
                                            length2 = i14;
                                            accountAndUserArr2 = accountAndUserArr4;
                                            it3 = it;
                                            i22 = i12;
                                            z5 = z4;
                                            i21 = i13;
                                            bundle5 = bundle3;
                                            syncManager3 = this;
                                            i18 = i3;
                                        }
                                    } else {
                                        AccountAndUser[] accountAndUserArr7 = accountAndUserArr2;
                                        Bundle bundle7 = bundle5;
                                        int i29 = i22;
                                        final AccountAndUser accountAndUser5 = accountAndUser3;
                                        boolean z10 = z5;
                                        boolean zAllowParallelSyncs = ((SyncAdapterType) serviceInfo.type).allowParallelSyncs();
                                        boolean zIsAlwaysSyncable = ((SyncAdapterType) serviceInfo.type).isAlwaysSyncable();
                                        if (z10 || iComputeSyncable >= 0 || !zIsAlwaysSyncable) {
                                            syncManager2 = this;
                                        } else {
                                            syncManager2 = this;
                                            syncManager2.mSyncStorageEngine.setIsSyncable(accountAndUser5.account, accountAndUser5.userId, str3, 1, -1);
                                            iComputeSyncable = 1;
                                        }
                                        if ((i3 == -2 || i3 == iComputeSyncable) && (((SyncAdapterType) serviceInfo.type).supportsUploading() || !z6)) {
                                            if (iComputeSyncable < 0 || z9 || (syncManager2.mSyncStorageEngine.getMasterSyncAutomatically(accountAndUser5.userId) && syncManager2.mSyncStorageEngine.getSyncAutomatically(accountAndUser5.account, accountAndUser5.userId, str3))) {
                                                long delayUntilTime = syncManager2.mSyncStorageEngine.getDelayUntilTime(new SyncStorageEngine.EndPoint(accountAndUser5.account, str3, accountAndUser5.userId));
                                                String packageName2 = serviceInfo.componentName.getPackageName();
                                                if (iComputeSyncable != -1) {
                                                    i15 = i23;
                                                    i16 = i3;
                                                    if (i16 == -2 || i16 == iComputeSyncable) {
                                                        if (zIsLoggable) {
                                                            StringBuilder sb2 = new StringBuilder();
                                                            sb2.append("scheduleSync: delay until ");
                                                            sb2.append(delayUntilTime);
                                                            sb2.append(", source ");
                                                            sb2.append(i15);
                                                            sb2.append(", account ");
                                                            sb2.append(accountAndUser5);
                                                            sb2.append(", authority ");
                                                            sb2.append(str3);
                                                            sb2.append(", extras ");
                                                            bundle4 = bundle7;
                                                            sb2.append(bundle4);
                                                            Slog.v("SyncManager", sb2.toString());
                                                        } else {
                                                            bundle4 = bundle7;
                                                        }
                                                        syncManager2.postScheduleSyncMessage(new SyncOperation(accountAndUser5.account, accountAndUser5.userId, i25, packageName2, i2, i15, str3, bundle4, zAllowParallelSyncs, i4), j);
                                                    } else {
                                                        bundle4 = bundle7;
                                                    }
                                                } else if (z10) {
                                                    final Bundle bundle8 = new Bundle(bundle7);
                                                    final SyncManager syncManager5 = syncManager2;
                                                    sendOnUnsyncableAccount(syncManager2.mContext, serviceInfo, accountAndUser5.userId, new OnReadyCallback() {
                                                        @Override
                                                        public final void onReady() {
                                                            SyncManager syncManager6 = this.f$0;
                                                            AccountAndUser accountAndUser6 = accountAndUser5;
                                                            syncManager6.scheduleSync(accountAndUser6.account, accountAndUser6.userId, i2, str3, bundle8, i3, j, false, i4);
                                                        }
                                                    });
                                                    i15 = i23;
                                                    bundle4 = bundle7;
                                                    i16 = i3;
                                                } else {
                                                    Bundle bundle9 = new Bundle();
                                                    bundle9.putBoolean("initialize", true);
                                                    if (zIsLoggable) {
                                                        StringBuilder sb3 = new StringBuilder();
                                                        sb3.append("schedule initialisation Sync:, delay until ");
                                                        sb3.append(delayUntilTime);
                                                        sb3.append(", run by ");
                                                        sb3.append(0);
                                                        sb3.append(", flexMillis ");
                                                        sb3.append(0);
                                                        sb3.append(", source ");
                                                        i15 = i23;
                                                        sb3.append(i15);
                                                        sb3.append(", account ");
                                                        sb3.append(accountAndUser5);
                                                        sb3.append(", authority ");
                                                        sb3.append(str3);
                                                        sb3.append(", extras ");
                                                        sb3.append(bundle9);
                                                        Slog.v("SyncManager", sb3.toString());
                                                    } else {
                                                        i15 = i23;
                                                    }
                                                    syncManager2.postScheduleSyncMessage(new SyncOperation(accountAndUser5.account, accountAndUser5.userId, i25, packageName2, i2, i15, str3, bundle9, zAllowParallelSyncs, i4), j);
                                                    bundle4 = bundle7;
                                                    i16 = i3;
                                                }
                                                bundle5 = bundle4;
                                                i18 = i16;
                                                i21 = i15;
                                                syncManager3 = syncManager2;
                                                accountAndUser3 = accountAndUser5;
                                                length2 = i24;
                                                accountAndUserArr2 = accountAndUserArr7;
                                                it3 = it4;
                                                i22 = i29;
                                                z5 = z10;
                                                i17 = i;
                                                i5 = i2;
                                            } else if (zIsLoggable) {
                                                Log.d("SyncManager", "scheduleSync: sync of " + accountAndUser5 + ", " + str3 + " is not allowed, dropping request");
                                            }
                                        }
                                        i5 = i2;
                                        i18 = i3;
                                        accountAndUser3 = accountAndUser5;
                                        length2 = i24;
                                        accountAndUserArr2 = accountAndUserArr7;
                                        it3 = it4;
                                        i22 = i29;
                                        i21 = i23;
                                        bundle5 = bundle7;
                                        syncManager3 = syncManager2;
                                        z5 = z10;
                                    }
                                }
                                i21 = i23;
                                length2 = i24;
                                it3 = it4;
                            }
                        }
                        i7 = length2;
                        accountAndUserArr3 = accountAndUserArr2;
                        bundle2 = bundle5;
                        i8 = i22;
                        z2 = z5;
                        syncManager = syncManager3;
                        i9 = i18;
                        i10 = 1;
                        z3 = false;
                        i11 = i21;
                    } else {
                        i7 = length2;
                        accountAndUserArr3 = accountAndUserArr2;
                        bundle2 = bundle5;
                        i8 = i22;
                        z2 = z5;
                        syncManager = syncManager3;
                        i9 = i18;
                        i11 = i21;
                        z3 = z8;
                        i10 = i6;
                    }
                    i6 = i10;
                    i18 = i9;
                    z8 = z3;
                    i21 = i11;
                    syncManager3 = syncManager;
                    length2 = i7;
                    z5 = z2;
                    i17 = i;
                    str2 = str;
                    i5 = i2;
                    i22 = i8 + 1;
                    bundle5 = bundle2;
                    accountAndUserArr2 = accountAndUserArr3;
                }
                return;
            }
            accountAndUserArr = new AccountAndUser[]{new AccountAndUser(account, i17)};
        }
        accountAndUserArr2 = accountAndUserArr;
        if (!ArrayUtils.isEmpty(accountAndUserArr2)) {
        }
    }

    public static void lambda$scheduleSync$4(SyncManager syncManager, AccountAndUser accountAndUser, int i, int i2, String str, Bundle bundle, int i3, long j, int i4, Bundle bundle2) {
        if (bundle2 != null && bundle2.getBoolean("booleanResult")) {
            syncManager.scheduleSync(accountAndUser.account, i, i2, str, bundle, i3, j, true, i4);
        }
    }

    public int computeSyncable(Account account, int i, String str, boolean z) {
        int isSyncable = getIsSyncable(account, i, str);
        if (isSyncable == 0) {
            return 0;
        }
        RegisteredServicesCache.ServiceInfo serviceInfo = this.mSyncAdapters.getServiceInfo(SyncAdapterType.newKey(str, account.type), i);
        if (serviceInfo == null) {
            return 0;
        }
        int i2 = serviceInfo.uid;
        String packageName = serviceInfo.componentName.getPackageName();
        try {
            if (ActivityManager.getService().isAppStartModeDisabled(i2, packageName)) {
                Slog.w("SyncManager", "Not scheduling job " + serviceInfo.uid + ":" + serviceInfo.componentName + " -- package not allowed to start");
                return 0;
            }
        } catch (RemoteException e) {
        }
        if (z && !canAccessAccount(account, packageName, i2)) {
            Log.w("SyncManager", "Access to " + account + " denied for package " + packageName + " in UID " + serviceInfo.uid);
            return 3;
        }
        return isSyncable;
    }

    private boolean canAccessAccount(Account account, String str, int i) {
        if (this.mAccountManager.hasAccountAccess(account, str, UserHandle.getUserHandleForUid(i))) {
            return true;
        }
        try {
            this.mContext.getPackageManager().getApplicationInfoAsUser(str, DumpState.DUMP_DEXOPT, UserHandle.getUserId(i));
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void removeSyncsForAuthority(SyncStorageEngine.EndPoint endPoint, String str) {
        this.mLogger.log("removeSyncsForAuthority: ", endPoint);
        verifyJobScheduler();
        for (SyncOperation syncOperation : getAllPendingSyncs()) {
            if (syncOperation.target.matchesSpec(endPoint)) {
                this.mLogger.log("canceling: ", syncOperation);
                cancelJob(syncOperation, str);
            }
        }
    }

    public void removePeriodicSync(SyncStorageEngine.EndPoint endPoint, Bundle bundle, String str) {
        SyncHandler syncHandler = this.mSyncHandler;
        SyncHandler syncHandler2 = this.mSyncHandler;
        Message messageObtainMessage = syncHandler.obtainMessage(14, Pair.create(endPoint, str));
        messageObtainMessage.setData(bundle);
        messageObtainMessage.sendToTarget();
    }

    public void updateOrAddPeriodicSync(SyncStorageEngine.EndPoint endPoint, long j, long j2, Bundle bundle) {
        this.mSyncHandler.obtainMessage(13, new UpdatePeriodicSyncMessagePayload(endPoint, j, j2, bundle)).sendToTarget();
    }

    public List<PeriodicSync> getPeriodicSyncs(SyncStorageEngine.EndPoint endPoint) {
        List<SyncOperation> allPendingSyncs = getAllPendingSyncs();
        ArrayList arrayList = new ArrayList();
        for (SyncOperation syncOperation : allPendingSyncs) {
            if (syncOperation.isPeriodic && syncOperation.target.matchesSpec(endPoint)) {
                arrayList.add(new PeriodicSync(syncOperation.target.account, syncOperation.target.provider, syncOperation.extras, syncOperation.periodMillis / 1000, syncOperation.flexMillis / 1000));
            }
        }
        return arrayList;
    }

    public void scheduleLocalSync(Account account, int i, int i2, String str, int i3) {
        Bundle bundle = new Bundle();
        bundle.putBoolean("upload", true);
        scheduleSync(account, i, i2, str, bundle, -2, LOCAL_SYNC_DELAY, true, i3);
    }

    public SyncAdapterType[] getSyncAdapterTypes(int i) {
        Collection allServices = this.mSyncAdapters.getAllServices(i);
        SyncAdapterType[] syncAdapterTypeArr = new SyncAdapterType[allServices.size()];
        Iterator it = allServices.iterator();
        int i2 = 0;
        while (it.hasNext()) {
            syncAdapterTypeArr[i2] = (SyncAdapterType) ((RegisteredServicesCache.ServiceInfo) it.next()).type;
            i2++;
        }
        return syncAdapterTypeArr;
    }

    public String[] getSyncAdapterPackagesForAuthorityAsUser(String str, int i) {
        return this.mSyncAdapters.getSyncAdapterPackagesForAuthority(str, i);
    }

    private void sendSyncFinishedOrCanceledMessage(ActiveSyncContext activeSyncContext, SyncResult syncResult) {
        if (Log.isLoggable("SyncManager", 2)) {
            Slog.v("SyncManager", "sending MESSAGE_SYNC_FINISHED");
        }
        Message messageObtainMessage = this.mSyncHandler.obtainMessage();
        messageObtainMessage.what = 1;
        messageObtainMessage.obj = new SyncFinishedOrCancelledMessagePayload(activeSyncContext, syncResult);
        this.mSyncHandler.sendMessage(messageObtainMessage);
    }

    private void sendCancelSyncsMessage(SyncStorageEngine.EndPoint endPoint, Bundle bundle, String str) {
        if (Log.isLoggable("SyncManager", 2)) {
            Slog.v("SyncManager", "sending MESSAGE_CANCEL");
        }
        this.mLogger.log("sendCancelSyncsMessage() ep=", endPoint, " why=", str);
        Message messageObtainMessage = this.mSyncHandler.obtainMessage();
        messageObtainMessage.what = 6;
        messageObtainMessage.setData(bundle);
        messageObtainMessage.obj = endPoint;
        this.mSyncHandler.sendMessage(messageObtainMessage);
    }

    private void postMonitorSyncProgressMessage(ActiveSyncContext activeSyncContext) {
        if (Log.isLoggable("SyncManager", 2)) {
            Slog.v("SyncManager", "posting MESSAGE_SYNC_MONITOR in 60s");
        }
        activeSyncContext.mBytesTransferredAtLastPoll = getTotalBytesTransferredByUid(activeSyncContext.mSyncAdapterUid);
        activeSyncContext.mLastPolledTimeElapsed = SystemClock.elapsedRealtime();
        this.mSyncHandler.sendMessageDelayed(this.mSyncHandler.obtainMessage(8, activeSyncContext), 60000L);
    }

    private void postScheduleSyncMessage(SyncOperation syncOperation, long j) {
        ScheduleSyncMessagePayload scheduleSyncMessagePayload = new ScheduleSyncMessagePayload(syncOperation, j);
        SyncHandler syncHandler = this.mSyncHandler;
        SyncHandler syncHandler2 = this.mSyncHandler;
        syncHandler.obtainMessage(12, scheduleSyncMessagePayload).sendToTarget();
    }

    private long getTotalBytesTransferredByUid(int i) {
        return TrafficStats.getUidRxBytes(i) + TrafficStats.getUidTxBytes(i);
    }

    private class SyncFinishedOrCancelledMessagePayload {
        public final ActiveSyncContext activeSyncContext;
        public final SyncResult syncResult;

        SyncFinishedOrCancelledMessagePayload(ActiveSyncContext activeSyncContext, SyncResult syncResult) {
            this.activeSyncContext = activeSyncContext;
            this.syncResult = syncResult;
        }
    }

    private class UpdatePeriodicSyncMessagePayload {
        public final Bundle extras;
        public final long flex;
        public final long pollFrequency;
        public final SyncStorageEngine.EndPoint target;

        UpdatePeriodicSyncMessagePayload(SyncStorageEngine.EndPoint endPoint, long j, long j2, Bundle bundle) {
            this.target = endPoint;
            this.pollFrequency = j;
            this.flex = j2;
            this.extras = bundle;
        }
    }

    private static class ScheduleSyncMessagePayload {
        final long minDelayMillis;
        final SyncOperation syncOperation;

        ScheduleSyncMessagePayload(SyncOperation syncOperation, long j) {
            this.syncOperation = syncOperation;
            this.minDelayMillis = j;
        }
    }

    private void clearBackoffSetting(SyncStorageEngine.EndPoint endPoint, String str) {
        Pair<Long, Long> backoff = this.mSyncStorageEngine.getBackoff(endPoint);
        if (backoff != null && ((Long) backoff.first).longValue() == -1 && ((Long) backoff.second).longValue() == -1) {
            return;
        }
        if (Log.isLoggable("SyncManager", 2)) {
            Slog.v("SyncManager", "Clearing backoffs for " + endPoint);
        }
        this.mSyncStorageEngine.setBackoff(endPoint, -1L, -1L);
        rescheduleSyncs(endPoint, str);
    }

    private void increaseBackoffSetting(SyncStorageEngine.EndPoint endPoint) {
        long jJitterize;
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        Pair<Long, Long> backoff = this.mSyncStorageEngine.getBackoff(endPoint);
        if (backoff == null) {
            jJitterize = -1;
        } else {
            if (jElapsedRealtime < ((Long) backoff.first).longValue()) {
                if (Log.isLoggable("SyncManager", 2)) {
                    Slog.v("SyncManager", "Still in backoff, do not increase it. Remaining: " + ((((Long) backoff.first).longValue() - jElapsedRealtime) / 1000) + " seconds.");
                    return;
                }
                return;
            }
            jJitterize = (long) (((Long) backoff.second).longValue() * this.mConstants.getRetryTimeIncreaseFactor());
        }
        if (jJitterize <= 0) {
            long initialSyncRetryTimeInSeconds = this.mConstants.getInitialSyncRetryTimeInSeconds() * 1000;
            jJitterize = jitterize(initialSyncRetryTimeInSeconds, (long) (initialSyncRetryTimeInSeconds * 1.1d));
        }
        long maxSyncRetryTimeInSeconds = ((long) this.mConstants.getMaxSyncRetryTimeInSeconds()) * 1000;
        long j = jJitterize > maxSyncRetryTimeInSeconds ? maxSyncRetryTimeInSeconds : jJitterize;
        long j2 = jElapsedRealtime + j;
        if (Log.isLoggable("SyncManager", 2)) {
            Slog.v("SyncManager", "Backoff until: " + j2 + ", delayTime: " + j);
        }
        this.mSyncStorageEngine.setBackoff(endPoint, j2, j);
        rescheduleSyncs(endPoint, "increaseBackoffSetting");
    }

    private void rescheduleSyncs(SyncStorageEngine.EndPoint endPoint, String str) {
        int i = 0;
        this.mLogger.log("rescheduleSyncs() ep=", endPoint, " why=", str);
        for (SyncOperation syncOperation : getAllPendingSyncs()) {
            if (!syncOperation.isPeriodic && syncOperation.target.matchesSpec(endPoint)) {
                i++;
                cancelJob(syncOperation, str);
                postScheduleSyncMessage(syncOperation, 0L);
            }
        }
        if (Log.isLoggable("SyncManager", 2)) {
            Slog.v("SyncManager", "Rescheduled " + i + " syncs for " + endPoint);
        }
    }

    private void setDelayUntilTime(SyncStorageEngine.EndPoint endPoint, long j) {
        long jElapsedRealtime;
        long j2 = j * 1000;
        long jCurrentTimeMillis = System.currentTimeMillis();
        if (j2 > jCurrentTimeMillis) {
            jElapsedRealtime = SystemClock.elapsedRealtime() + (j2 - jCurrentTimeMillis);
        } else {
            jElapsedRealtime = 0;
        }
        this.mSyncStorageEngine.setDelayUntilTime(endPoint, jElapsedRealtime);
        if (Log.isLoggable("SyncManager", 2)) {
            Slog.v("SyncManager", "Delay Until time set to " + jElapsedRealtime + " for " + endPoint);
        }
        rescheduleSyncs(endPoint, "delayUntil newDelayUntilTime: " + jElapsedRealtime);
    }

    private boolean isAdapterDelayed(SyncStorageEngine.EndPoint endPoint) {
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        Pair<Long, Long> backoff = this.mSyncStorageEngine.getBackoff(endPoint);
        if ((backoff != null && ((Long) backoff.first).longValue() != -1 && ((Long) backoff.first).longValue() > jElapsedRealtime) || this.mSyncStorageEngine.getDelayUntilTime(endPoint) > jElapsedRealtime) {
            return true;
        }
        return false;
    }

    public void cancelActiveSync(SyncStorageEngine.EndPoint endPoint, Bundle bundle, String str) {
        sendCancelSyncsMessage(endPoint, bundle, str);
    }

    private void scheduleSyncOperationH(SyncOperation syncOperation) {
        scheduleSyncOperationH(syncOperation, 0L);
    }

    private void scheduleSyncOperationH(SyncOperation syncOperation, long j) {
        long jMax;
        int i;
        boolean z;
        UsageStatsManagerInternal usageStatsManagerInternal;
        DeviceIdleController.LocalService localService;
        int iMax;
        long jLongValue;
        boolean zIsLoggable = Log.isLoggable("SyncManager", 2);
        if (syncOperation == null) {
            Slog.e("SyncManager", "Can't schedule null sync operation.");
            return;
        }
        if (!syncOperation.ignoreBackoff()) {
            Pair<Long, Long> backoff = this.mSyncStorageEngine.getBackoff(syncOperation.target);
            if (backoff == null) {
                Slog.e("SyncManager", "Couldn't find backoff values for " + syncOperation.target);
                backoff = new Pair<>(-1L, -1L);
            }
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            if (((Long) backoff.first).longValue() != -1) {
                jLongValue = ((Long) backoff.first).longValue() - jElapsedRealtime;
            } else {
                jLongValue = 0;
            }
            long delayUntilTime = this.mSyncStorageEngine.getDelayUntilTime(syncOperation.target);
            long j2 = delayUntilTime > jElapsedRealtime ? delayUntilTime - jElapsedRealtime : 0L;
            if (zIsLoggable) {
                Slog.v("SyncManager", "backoff delay:" + jLongValue + " delayUntil delay:" + j2);
            }
            jMax = Math.max(j, Math.max(jLongValue, j2));
        } else {
            jMax = j;
        }
        if (jMax < 0) {
            jMax = 0;
        }
        if (!syncOperation.isPeriodic) {
            Iterator<ActiveSyncContext> it = this.mActiveSyncContexts.iterator();
            while (it.hasNext()) {
                if (it.next().mSyncOperation.key.equals(syncOperation.key)) {
                    if (zIsLoggable) {
                        Log.v("SyncManager", "Duplicate sync is already running. Not scheduling " + syncOperation);
                        return;
                    }
                    return;
                }
            }
            syncOperation.expectedRuntime = SystemClock.elapsedRealtime() + jMax;
            List<SyncOperation> allPendingSyncs = getAllPendingSyncs();
            SyncOperation syncOperation2 = syncOperation;
            int i2 = 0;
            for (SyncOperation syncOperation3 : allPendingSyncs) {
                if (!syncOperation3.isPeriodic) {
                    if (syncOperation3.key.equals(syncOperation.key)) {
                        if (syncOperation2.expectedRuntime > syncOperation3.expectedRuntime) {
                            syncOperation2 = syncOperation3;
                        }
                        i2++;
                    }
                }
            }
            if (i2 > 1) {
                Slog.e("SyncManager", "FATAL ERROR! File a bug if you see this.");
            }
            if (syncOperation == syncOperation2 || jMax != 0 || syncOperation2.syncExemptionFlag >= syncOperation.syncExemptionFlag) {
                i = 0;
                iMax = 0;
            } else {
                i = 0;
                iMax = Math.max(0, syncOperation.syncExemptionFlag);
                syncOperation2 = syncOperation;
            }
            for (SyncOperation syncOperation4 : allPendingSyncs) {
                if (!syncOperation4.isPeriodic && syncOperation4.key.equals(syncOperation.key) && syncOperation4 != syncOperation2) {
                    if (zIsLoggable) {
                        Slog.v("SyncManager", "Cancelling duplicate sync " + syncOperation4);
                    }
                    int iMax2 = Math.max(iMax, syncOperation4.syncExemptionFlag);
                    cancelJob(syncOperation4, "scheduleSyncOperationH-duplicate");
                    iMax = iMax2;
                }
            }
            if (syncOperation2 != syncOperation) {
                if (zIsLoggable) {
                    Slog.v("SyncManager", "Not scheduling because a duplicate exists.");
                    return;
                }
                return;
            } else if (iMax > 0) {
                syncOperation.syncExemptionFlag = iMax;
            }
        } else {
            i = 0;
        }
        if (syncOperation.jobId == -1) {
            syncOperation.jobId = getUnusedJobIdH();
        }
        if (zIsLoggable) {
            Slog.v("SyncManager", "scheduling sync operation " + syncOperation.toString());
        }
        int iFindPriority = syncOperation.findPriority();
        int i3 = syncOperation.isNotAllowedOnMetered() ? 2 : 1;
        if (syncOperation.isAppStandbyExempted()) {
            i = 8;
        }
        JobInfo.Builder flags = new JobInfo.Builder(syncOperation.jobId, new ComponentName(this.mContext, (Class<?>) SyncJobService.class)).setExtras(syncOperation.toJobInfoExtras()).setRequiredNetworkType(i3).setPersisted(true).setPriority(iFindPriority).setFlags(i);
        if (syncOperation.isPeriodic) {
            flags.setPeriodic(syncOperation.periodMillis, syncOperation.flexMillis);
            z = true;
        } else {
            if (jMax > 0) {
                flags.setMinimumLatency(jMax);
            }
            z = true;
            getSyncStorageEngine().markPending(syncOperation.target, true);
        }
        if (syncOperation.extras.getBoolean("require_charging")) {
            flags.setRequiresCharging(z);
        }
        if (syncOperation.syncExemptionFlag == 2 && (localService = (DeviceIdleController.LocalService) LocalServices.getService(DeviceIdleController.LocalService.class)) != null) {
            localService.addPowerSaveTempWhitelistApp(1000, syncOperation.owningPackage, this.mConstants.getKeyExemptionTempWhitelistDurationInSeconds() * 1000, UserHandle.getUserId(syncOperation.owningUid), false, "sync by top app");
        }
        if (syncOperation.isAppStandbyExempted() && (usageStatsManagerInternal = (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class)) != null) {
            usageStatsManagerInternal.reportExemptedSyncScheduled(syncOperation.owningPackage, UserHandle.getUserId(syncOperation.owningUid));
        }
        getJobScheduler().scheduleAsPackage(flags.build(), syncOperation.owningPackage, syncOperation.target.userId, syncOperation.wakeLockName());
    }

    public void clearScheduledSyncOperations(SyncStorageEngine.EndPoint endPoint) {
        for (SyncOperation syncOperation : getAllPendingSyncs()) {
            if (!syncOperation.isPeriodic && syncOperation.target.matchesSpec(endPoint)) {
                cancelJob(syncOperation, "clearScheduledSyncOperations");
                getSyncStorageEngine().markPending(syncOperation.target, false);
            }
        }
        this.mSyncStorageEngine.setBackoff(endPoint, -1L, -1L);
    }

    public void cancelScheduledSyncOperation(SyncStorageEngine.EndPoint endPoint, Bundle bundle) {
        for (SyncOperation syncOperation : getAllPendingSyncs()) {
            if (!syncOperation.isPeriodic && syncOperation.target.matchesSpec(endPoint) && syncExtrasEquals(bundle, syncOperation.extras, false)) {
                cancelJob(syncOperation, "cancelScheduledSyncOperation");
            }
        }
        setAuthorityPendingState(endPoint);
        if (!this.mSyncStorageEngine.isSyncPending(endPoint)) {
            this.mSyncStorageEngine.setBackoff(endPoint, -1L, -1L);
        }
    }

    private void maybeRescheduleSync(SyncResult syncResult, SyncOperation syncOperation) {
        boolean zIsLoggable = Log.isLoggable("SyncManager", 3);
        if (zIsLoggable) {
            Log.d("SyncManager", "encountered error(s) during the sync: " + syncResult + ", " + syncOperation);
        }
        if (syncOperation.extras.getBoolean("ignore_backoff", false)) {
            syncOperation.extras.remove("ignore_backoff");
        }
        if (syncOperation.extras.getBoolean("do_not_retry", false) && !syncResult.syncAlreadyInProgress) {
            if (zIsLoggable) {
                Log.d("SyncManager", "not retrying sync operation because SYNC_EXTRAS_DO_NOT_RETRY was specified " + syncOperation);
                return;
            }
            return;
        }
        if (syncOperation.extras.getBoolean("upload", false) && !syncResult.syncAlreadyInProgress) {
            syncOperation.extras.remove("upload");
            if (zIsLoggable) {
                Log.d("SyncManager", "retrying sync operation as a two-way sync because an upload-only sync encountered an error: " + syncOperation);
            }
            scheduleSyncOperationH(syncOperation);
            return;
        }
        if (syncResult.tooManyRetries) {
            if (zIsLoggable) {
                Log.d("SyncManager", "not retrying sync operation because it retried too many times: " + syncOperation);
                return;
            }
            return;
        }
        if (syncResult.madeSomeProgress()) {
            if (zIsLoggable) {
                Log.d("SyncManager", "retrying sync operation because even though it had an error it achieved some success");
            }
            scheduleSyncOperationH(syncOperation);
            return;
        }
        if (syncResult.syncAlreadyInProgress) {
            if (zIsLoggable) {
                Log.d("SyncManager", "retrying sync operation that failed because there was already a sync in progress: " + syncOperation);
            }
            scheduleSyncOperationH(syncOperation, 10000L);
            return;
        }
        if (syncResult.hasSoftError()) {
            if (zIsLoggable) {
                Log.d("SyncManager", "retrying sync operation because it encountered a soft error: " + syncOperation);
            }
            scheduleSyncOperationH(syncOperation);
            return;
        }
        Log.d("SyncManager", "not retrying sync operation because the error is a hard error: " + syncOperation);
    }

    private void onUserUnlocked(int i) {
        AccountManagerService.getSingleton().validateAccounts(i);
        this.mSyncAdapters.invalidateCache(i);
        updateRunningAccounts(new SyncStorageEngine.EndPoint(null, null, i));
        for (Account account : AccountManagerService.getSingleton().getAccounts(i, this.mContext.getOpPackageName())) {
            scheduleSync(account, i, -8, null, null, -1, 0);
        }
    }

    private void onUserStopped(int i) {
        updateRunningAccounts(null);
        cancelActiveSync(new SyncStorageEngine.EndPoint(null, null, i), null, "onUserStopped");
    }

    private void onUserRemoved(int i) {
        this.mLogger.log("onUserRemoved: u", Integer.valueOf(i));
        updateRunningAccounts(null);
        this.mSyncStorageEngine.doDatabaseCleanup(new Account[0], i);
        for (SyncOperation syncOperation : getAllPendingSyncs()) {
            if (syncOperation.target.userId == i) {
                cancelJob(syncOperation, "user removed u" + i);
            }
        }
    }

    static Intent getAdapterBindIntent(Context context, ComponentName componentName, int i) {
        Intent intent = new Intent();
        intent.setAction("android.content.SyncAdapter");
        intent.setComponent(componentName);
        intent.putExtra("android.intent.extra.client_label", R.string.mediasize_na_arch_e);
        if (BenesseExtension.getDchaState() == 0) {
            intent.putExtra("android.intent.extra.client_intent", PendingIntent.getActivityAsUser(context, 0, new Intent("android.settings.SYNC_SETTINGS"), 0, null, UserHandle.of(i)));
        }
        return intent;
    }

    class ActiveSyncContext extends ISyncContext.Stub implements ServiceConnection, IBinder.DeathRecipient {
        boolean mBound;
        long mBytesTransferredAtLastPoll;
        String mEventName;
        final long mHistoryRowId;
        long mLastPolledTimeElapsed;
        final int mSyncAdapterUid;
        SyncInfo mSyncInfo;
        final SyncOperation mSyncOperation;
        final PowerManager.WakeLock mSyncWakeLock;
        boolean mIsLinkedToDeath = false;
        ISyncAdapter mSyncAdapter = null;
        final long mStartTime = SystemClock.elapsedRealtime();
        long mTimeoutStartTime = this.mStartTime;

        public ActiveSyncContext(SyncOperation syncOperation, long j, int i) {
            this.mSyncAdapterUid = i;
            this.mSyncOperation = syncOperation;
            this.mHistoryRowId = j;
            this.mSyncWakeLock = SyncManager.this.mSyncHandler.getSyncWakeLock(this.mSyncOperation);
            this.mSyncWakeLock.setWorkSource(new WorkSource(i));
            this.mSyncWakeLock.acquire();
        }

        public void sendHeartbeat() {
        }

        public void onFinished(SyncResult syncResult) {
            if (Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", "onFinished: " + this);
            }
            SyncLogger syncLogger = SyncManager.this.mLogger;
            Object[] objArr = new Object[4];
            objArr[0] = "onFinished result=";
            objArr[1] = syncResult;
            objArr[2] = " endpoint=";
            objArr[3] = this.mSyncOperation == null ? "null" : this.mSyncOperation.target;
            syncLogger.log(objArr);
            SyncManager.this.sendSyncFinishedOrCanceledMessage(this, syncResult);
        }

        public void toString(StringBuilder sb) {
            sb.append("startTime ");
            sb.append(this.mStartTime);
            sb.append(", mTimeoutStartTime ");
            sb.append(this.mTimeoutStartTime);
            sb.append(", mHistoryRowId ");
            sb.append(this.mHistoryRowId);
            sb.append(", syncOperation ");
            sb.append(this.mSyncOperation);
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Message messageObtainMessage = SyncManager.this.mSyncHandler.obtainMessage();
            messageObtainMessage.what = 4;
            messageObtainMessage.obj = SyncManager.this.new ServiceConnectionData(this, iBinder);
            SyncManager.this.mSyncHandler.sendMessage(messageObtainMessage);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Message messageObtainMessage = SyncManager.this.mSyncHandler.obtainMessage();
            messageObtainMessage.what = 5;
            messageObtainMessage.obj = SyncManager.this.new ServiceConnectionData(this, null);
            SyncManager.this.mSyncHandler.sendMessage(messageObtainMessage);
        }

        boolean bindToSyncAdapter(ComponentName componentName, int i) {
            if (Log.isLoggable("SyncManager", 2)) {
                Log.d("SyncManager", "bindToSyncAdapter: " + componentName + ", connection " + this);
            }
            Intent adapterBindIntent = SyncManager.getAdapterBindIntent(SyncManager.this.mContext, componentName, i);
            this.mBound = true;
            boolean zBindServiceAsUser = SyncManager.this.mContext.bindServiceAsUser(adapterBindIntent, this, 21, new UserHandle(this.mSyncOperation.target.userId));
            SyncManager.this.mLogger.log("bindService() returned=", Boolean.valueOf(this.mBound), " for ", this);
            if (!zBindServiceAsUser) {
                this.mBound = false;
            } else {
                try {
                    this.mEventName = this.mSyncOperation.wakeLockName();
                    SyncManager.this.mBatteryStats.noteSyncStart(this.mEventName, this.mSyncAdapterUid);
                } catch (RemoteException e) {
                }
            }
            return zBindServiceAsUser;
        }

        protected void close() {
            if (Log.isLoggable("SyncManager", 2)) {
                Log.d("SyncManager", "unBindFromSyncAdapter: connection " + this);
            }
            if (this.mBound) {
                this.mBound = false;
                SyncManager.this.mLogger.log("unbindService for ", this);
                SyncManager.this.mContext.unbindService(this);
                try {
                    SyncManager.this.mBatteryStats.noteSyncFinish(this.mEventName, this.mSyncAdapterUid);
                } catch (RemoteException e) {
                }
            }
            this.mSyncWakeLock.release();
            this.mSyncWakeLock.setWorkSource(null);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            toString(sb);
            return sb.toString();
        }

        @Override
        public void binderDied() {
            SyncManager.this.sendSyncFinishedOrCanceledMessage(this, null);
        }
    }

    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, boolean z) {
        IndentingPrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "  ");
        dumpSyncState(indentingPrintWriter, new SyncAdapterStateFetcher());
        this.mConstants.dump(printWriter, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        dumpSyncAdapters(indentingPrintWriter);
        if (z) {
            indentingPrintWriter.println("Detailed Sync History");
            this.mLogger.dumpAll(printWriter);
        }
    }

    static String formatTime(long j) {
        if (j == 0) {
            return "N/A";
        }
        Time time = new Time();
        time.set(j);
        return time.format("%Y-%m-%d %H:%M:%S");
    }

    static int lambda$static$6(SyncOperation syncOperation, SyncOperation syncOperation2) {
        int iCompare = Integer.compare(syncOperation.target.userId, syncOperation2.target.userId);
        if (iCompare != 0) {
            return iCompare;
        }
        Comparator comparator = String.CASE_INSENSITIVE_ORDER;
        int iCompare2 = comparator.compare(syncOperation.target.account.type, syncOperation2.target.account.type);
        if (iCompare2 != 0) {
            return iCompare2;
        }
        int iCompare3 = comparator.compare(syncOperation.target.account.name, syncOperation2.target.account.name);
        if (iCompare3 != 0) {
            return iCompare3;
        }
        int iCompare4 = comparator.compare(syncOperation.target.provider, syncOperation2.target.provider);
        if (iCompare4 != 0) {
            return iCompare4;
        }
        int iCompare5 = Integer.compare(syncOperation.reason, syncOperation2.reason);
        if (iCompare5 != 0) {
            return iCompare5;
        }
        int iCompare6 = Long.compare(syncOperation.periodMillis, syncOperation2.periodMillis);
        if (iCompare6 != 0) {
            return iCompare6;
        }
        int iCompare7 = Long.compare(syncOperation.expectedRuntime, syncOperation2.expectedRuntime);
        if (iCompare7 != 0) {
            return iCompare7;
        }
        int iCompare8 = Long.compare(syncOperation.jobId, syncOperation2.jobId);
        if (iCompare8 != 0) {
            return iCompare8;
        }
        return 0;
    }

    static int lambda$static$7(SyncOperation syncOperation, SyncOperation syncOperation2) {
        int iCompare = Long.compare(syncOperation.expectedRuntime, syncOperation2.expectedRuntime);
        return iCompare != 0 ? iCompare : sOpDumpComparator.compare(syncOperation, syncOperation2);
    }

    private static <T> int countIf(Collection<T> collection, Predicate<T> predicate) {
        Iterator<T> it = collection.iterator();
        int i = 0;
        while (it.hasNext()) {
            if (predicate.test(it.next())) {
                i++;
            }
        }
        return i;
    }

    protected void dumpPendingSyncs(PrintWriter printWriter, SyncAdapterStateFetcher syncAdapterStateFetcher) {
        List<SyncOperation> allPendingSyncs = getAllPendingSyncs();
        printWriter.print("Pending Syncs: ");
        printWriter.println(countIf(allPendingSyncs, new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return SyncManager.lambda$dumpPendingSyncs$8((SyncOperation) obj);
            }
        }));
        Collections.sort(allPendingSyncs, sOpRuntimeComparator);
        for (SyncOperation syncOperation : allPendingSyncs) {
            if (!syncOperation.isPeriodic) {
                printWriter.println(syncOperation.dump(null, false, syncAdapterStateFetcher));
            }
        }
        printWriter.println();
    }

    static boolean lambda$dumpPendingSyncs$8(SyncOperation syncOperation) {
        return !syncOperation.isPeriodic;
    }

    protected void dumpPeriodicSyncs(PrintWriter printWriter, SyncAdapterStateFetcher syncAdapterStateFetcher) {
        List<SyncOperation> allPendingSyncs = getAllPendingSyncs();
        printWriter.print("Periodic Syncs: ");
        printWriter.println(countIf(allPendingSyncs, new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return ((SyncOperation) obj).isPeriodic;
            }
        }));
        Collections.sort(allPendingSyncs, sOpDumpComparator);
        for (SyncOperation syncOperation : allPendingSyncs) {
            if (syncOperation.isPeriodic) {
                printWriter.println(syncOperation.dump(null, false, syncAdapterStateFetcher));
            }
        }
        printWriter.println();
    }

    public static StringBuilder formatDurationHMS(StringBuilder sb, long j) {
        long j2 = j / 1000;
        if (j2 < 0) {
            sb.append('-');
            j2 = -j2;
        }
        long j3 = j2 % 60;
        long j4 = j2 / 60;
        long j5 = j4 % 60;
        long j6 = j4 / 60;
        long j7 = j6 % 24;
        long j8 = j6 / 24;
        boolean z = false;
        if (j8 > 0) {
            sb.append(j8);
            sb.append('d');
            z = true;
        }
        if (!printTwoDigitNumber(sb, j3, 's', printTwoDigitNumber(sb, j5, 'm', printTwoDigitNumber(sb, j7, 'h', z)))) {
            sb.append("0s");
        }
        return sb;
    }

    private static boolean printTwoDigitNumber(StringBuilder sb, long j, char c, boolean z) {
        if (!z && j == 0) {
            return false;
        }
        if (z && j < 10) {
            sb.append('0');
        }
        sb.append(j);
        sb.append(c);
        return true;
    }

    protected void dumpSyncState(PrintWriter printWriter, SyncAdapterStateFetcher syncAdapterStateFetcher) {
        int i;
        final StringBuilder sb = new StringBuilder();
        printWriter.print("Data connected: ");
        printWriter.println(this.mDataConnectionIsConnected);
        printWriter.print("Battery saver: ");
        char c = 1;
        int i2 = 0;
        printWriter.println(this.mPowerManager != null && this.mPowerManager.isPowerSaveMode());
        printWriter.print("Background network restriction: ");
        ConnectivityManager connectivityManager = getConnectivityManager();
        int restrictBackgroundStatus = connectivityManager == null ? -1 : connectivityManager.getRestrictBackgroundStatus();
        switch (restrictBackgroundStatus) {
            case 1:
                printWriter.println(" disabled");
                break;
            case 2:
                printWriter.println(" whitelisted");
                break;
            case 3:
                printWriter.println(" enabled");
                break;
            default:
                printWriter.print("Unknown(");
                printWriter.print(restrictBackgroundStatus);
                printWriter.println(")");
                break;
        }
        printWriter.print("Auto sync: ");
        List<UserInfo> allUsers = getAllUsers();
        if (allUsers != null) {
            for (UserInfo userInfo : allUsers) {
                printWriter.print("u" + userInfo.id + "=" + this.mSyncStorageEngine.getMasterSyncAutomatically(userInfo.id) + " ");
            }
            printWriter.println();
        }
        printWriter.print("Memory low: ");
        printWriter.println(this.mStorageIsLow);
        printWriter.print("Device idle: ");
        printWriter.println(this.mDeviceIsIdle);
        printWriter.print("Reported active: ");
        printWriter.println(this.mReportedSyncActive);
        printWriter.print("Clock valid: ");
        printWriter.println(this.mSyncStorageEngine.isClockValid());
        AccountAndUser[] allAccounts = AccountManagerService.getSingleton().getAllAccounts();
        printWriter.print("Accounts: ");
        if (allAccounts != INITIAL_ACCOUNTS_ARRAY) {
            printWriter.println(allAccounts.length);
        } else {
            printWriter.println("not known yet");
        }
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        printWriter.print("Now: ");
        printWriter.print(jElapsedRealtime);
        printWriter.println(" (" + formatTime(System.currentTimeMillis()) + ")");
        sb.setLength(0);
        printWriter.print("Uptime: ");
        printWriter.print(formatDurationHMS(sb, jElapsedRealtime));
        printWriter.println();
        printWriter.print("Time spent syncing: ");
        sb.setLength(0);
        printWriter.print(formatDurationHMS(sb, this.mSyncHandler.mSyncTimeTracker.timeSpentSyncing()));
        printWriter.print(", sync ");
        printWriter.print(this.mSyncHandler.mSyncTimeTracker.mLastWasSyncing ? BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS : "not ");
        printWriter.println("in progress");
        printWriter.println();
        printWriter.println("Active Syncs: " + this.mActiveSyncContexts.size());
        PackageManager packageManager = this.mContext.getPackageManager();
        for (ActiveSyncContext activeSyncContext : this.mActiveSyncContexts) {
            long j = jElapsedRealtime - activeSyncContext.mStartTime;
            printWriter.print("  ");
            sb.setLength(0);
            printWriter.print(formatDurationHMS(sb, j));
            printWriter.print(" - ");
            printWriter.print(activeSyncContext.mSyncOperation.dump(packageManager, false, syncAdapterStateFetcher));
            printWriter.println();
        }
        printWriter.println();
        dumpPendingSyncs(printWriter, syncAdapterStateFetcher);
        dumpPeriodicSyncs(printWriter, syncAdapterStateFetcher);
        printWriter.println("Sync Status");
        ArrayList arrayList = new ArrayList();
        this.mSyncStorageEngine.resetTodayStats(false);
        int length = allAccounts.length;
        int i3 = 0;
        while (i3 < length) {
            AccountAndUser accountAndUser = allAccounts[i3];
            Object[] objArr = new Object[3];
            objArr[i2] = accountAndUser.account.name;
            objArr[c] = Integer.valueOf(accountAndUser.userId);
            objArr[2] = accountAndUser.account.type;
            printWriter.printf("Account %s u%d %s\n", objArr);
            printWriter.println("=======================================================================");
            final PrintTable printTable = new PrintTable(16);
            Object[] objArr2 = new Object[16];
            objArr2[i2] = "Authority";
            objArr2[c] = "Syncable";
            objArr2[2] = "Enabled";
            objArr2[3] = "Stats";
            objArr2[4] = "Loc";
            objArr2[5] = "Poll";
            objArr2[6] = "Per";
            objArr2[7] = "Feed";
            objArr2[8] = "User";
            objArr2[9] = "Othr";
            objArr2[10] = "Tot";
            objArr2[11] = "Fail";
            objArr2[12] = "Can";
            objArr2[13] = "Time";
            objArr2[14] = "Last Sync";
            objArr2[15] = "Backoff";
            printTable.set(i2, i2, objArr2);
            ArrayList arrayListNewArrayList = Lists.newArrayList();
            arrayListNewArrayList.addAll(this.mSyncAdapters.getAllServices(accountAndUser.userId));
            Collections.sort(arrayListNewArrayList, new Comparator<RegisteredServicesCache.ServiceInfo<SyncAdapterType>>() {
                @Override
                public int compare(RegisteredServicesCache.ServiceInfo<SyncAdapterType> serviceInfo, RegisteredServicesCache.ServiceInfo<SyncAdapterType> serviceInfo2) {
                    return ((SyncAdapterType) serviceInfo.type).authority.compareTo(((SyncAdapterType) serviceInfo2.type).authority);
                }
            });
            Iterator it = arrayListNewArrayList.iterator();
            while (it.hasNext()) {
                RegisteredServicesCache.ServiceInfo serviceInfo = (RegisteredServicesCache.ServiceInfo) it.next();
                if (((SyncAdapterType) serviceInfo.type).accountType.equals(accountAndUser.account.type)) {
                    int numRows = printTable.getNumRows();
                    AccountAndUser[] accountAndUserArr = allAccounts;
                    Iterator it2 = it;
                    Pair<SyncStorageEngine.AuthorityInfo, SyncStatusInfo> copyOfAuthorityWithSyncStatus = this.mSyncStorageEngine.getCopyOfAuthorityWithSyncStatus(new SyncStorageEngine.EndPoint(accountAndUser.account, ((SyncAdapterType) serviceInfo.type).authority, accountAndUser.userId));
                    SyncStorageEngine.AuthorityInfo authorityInfo = (SyncStorageEngine.AuthorityInfo) copyOfAuthorityWithSyncStatus.first;
                    SyncStatusInfo syncStatusInfo = (SyncStatusInfo) copyOfAuthorityWithSyncStatus.second;
                    arrayList.add(Pair.create(authorityInfo.target, syncStatusInfo));
                    String strSubstring = authorityInfo.target.provider;
                    if (strSubstring.length() > 50) {
                        strSubstring = strSubstring.substring(strSubstring.length() - 50);
                    }
                    printTable.set(numRows, 0, strSubstring, Integer.valueOf(authorityInfo.syncable), Boolean.valueOf(authorityInfo.enabled));
                    QuadConsumer quadConsumer = new QuadConsumer() {
                        public final void accept(Object obj, Object obj2, Object obj3, Object obj4) {
                            SyncManager.lambda$dumpSyncState$10(sb, printTable, (String) obj, (SyncStatusInfo.Stats) obj2, (Function) obj3, (Integer) obj4);
                        }
                    };
                    StringBuilder sb2 = sb;
                    int i4 = length;
                    quadConsumer.accept("Total", syncStatusInfo.totalStats, new Function() {
                        @Override
                        public final Object apply(Object obj) {
                            return Integer.toString(((Integer) obj).intValue());
                        }
                    }, Integer.valueOf(numRows));
                    int i5 = numRows + 1;
                    AccountAndUser accountAndUser2 = accountAndUser;
                    quadConsumer.accept("Today", syncStatusInfo.todayStats, new Function() {
                        @Override
                        public final Object apply(Object obj) {
                            return this.f$0.zeroToEmpty(((Integer) obj).intValue());
                        }
                    }, Integer.valueOf(i5));
                    quadConsumer.accept("Yestr", syncStatusInfo.yesterdayStats, new Function() {
                        @Override
                        public final Object apply(Object obj) {
                            return this.f$0.zeroToEmpty(((Integer) obj).intValue());
                        }
                    }, Integer.valueOf(numRows + 2));
                    if (authorityInfo.delayUntil > jElapsedRealtime) {
                        printTable.set(numRows, 15, "D: " + ((authorityInfo.delayUntil - jElapsedRealtime) / 1000));
                        if (authorityInfo.backoffTime > jElapsedRealtime) {
                            printTable.set(i5, 15, "B: " + ((authorityInfo.backoffTime - jElapsedRealtime) / 1000));
                            i = i3;
                            printTable.set(i5 + 1, 15, Long.valueOf(authorityInfo.backoffDelay / 1000));
                        } else {
                            i = i3;
                        }
                        if (syncStatusInfo.lastSuccessTime != 0) {
                            printTable.set(numRows, 14, SyncStorageEngine.SOURCES[syncStatusInfo.lastSuccessSource] + " SUCCESS");
                            printTable.set(i5, 14, formatTime(syncStatusInfo.lastSuccessTime));
                            numRows = i5 + 1;
                        }
                        if (syncStatusInfo.lastFailureTime != 0) {
                            int i6 = numRows + 1;
                            printTable.set(numRows, 14, SyncStorageEngine.SOURCES[syncStatusInfo.lastFailureSource] + " FAILURE");
                            printTable.set(i6, 14, formatTime(syncStatusInfo.lastFailureTime));
                            printTable.set(i6 + 1, 14, syncStatusInfo.lastFailureMesg);
                        }
                        allAccounts = accountAndUserArr;
                        it = it2;
                        sb = sb2;
                        length = i4;
                        accountAndUser = accountAndUser2;
                        i3 = i;
                    }
                }
            }
            printTable.writeTo(printWriter);
            i3++;
            c = 1;
            i2 = 0;
        }
        dumpSyncHistory(printWriter);
        printWriter.println();
        printWriter.println("Per Adapter History");
        printWriter.println("(SERVER is now split up to FEED and OTHER)");
        for (int i7 = 0; i7 < arrayList.size(); i7++) {
            Pair pair = (Pair) arrayList.get(i7);
            printWriter.print("  ");
            printWriter.print(((SyncStorageEngine.EndPoint) pair.first).account.name);
            printWriter.print('/');
            printWriter.print(((SyncStorageEngine.EndPoint) pair.first).account.type);
            printWriter.print(" u");
            printWriter.print(((SyncStorageEngine.EndPoint) pair.first).userId);
            printWriter.print(" [");
            printWriter.print(((SyncStorageEngine.EndPoint) pair.first).provider);
            printWriter.print("]");
            printWriter.println();
            printWriter.println("    Per source last syncs:");
            for (int i8 = 0; i8 < SyncStorageEngine.SOURCES.length; i8++) {
                printWriter.print("      ");
                printWriter.print(String.format("%8s", SyncStorageEngine.SOURCES[i8]));
                printWriter.print("  Success: ");
                printWriter.print(formatTime(((SyncStatusInfo) pair.second).perSourceLastSuccessTimes[i8]));
                printWriter.print("  Failure: ");
                printWriter.println(formatTime(((SyncStatusInfo) pair.second).perSourceLastFailureTimes[i8]));
            }
            printWriter.println("    Last syncs:");
            for (int i9 = 0; i9 < ((SyncStatusInfo) pair.second).getEventCount(); i9++) {
                printWriter.print("      ");
                printWriter.print(formatTime(((SyncStatusInfo) pair.second).getEventTime(i9)));
                printWriter.print(' ');
                printWriter.print(((SyncStatusInfo) pair.second).getEvent(i9));
                printWriter.println();
            }
            if (((SyncStatusInfo) pair.second).getEventCount() == 0) {
                printWriter.println("      N/A");
            }
        }
    }

    static void lambda$dumpSyncState$10(StringBuilder sb, PrintTable printTable, String str, SyncStatusInfo.Stats stats, Function function, Integer num) {
        sb.setLength(0);
        printTable.set(num.intValue(), 3, str, function.apply(Integer.valueOf(stats.numSourceLocal)), function.apply(Integer.valueOf(stats.numSourcePoll)), function.apply(Integer.valueOf(stats.numSourcePeriodic)), function.apply(Integer.valueOf(stats.numSourceFeed)), function.apply(Integer.valueOf(stats.numSourceUser)), function.apply(Integer.valueOf(stats.numSourceOther)), function.apply(Integer.valueOf(stats.numSyncs)), function.apply(Integer.valueOf(stats.numFailures)), function.apply(Integer.valueOf(stats.numCancels)), formatDurationHMS(sb, stats.totalElapsedTime));
    }

    private String zeroToEmpty(int i) {
        return i != 0 ? Integer.toString(i) : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
    }

    private void dumpTimeSec(PrintWriter printWriter, long j) {
        printWriter.print(j / 1000);
        printWriter.print('.');
        printWriter.print((j / 100) % 10);
        printWriter.print('s');
    }

    private void dumpDayStatistic(PrintWriter printWriter, SyncStorageEngine.DayStats dayStats) {
        printWriter.print("Success (");
        printWriter.print(dayStats.successCount);
        if (dayStats.successCount > 0) {
            printWriter.print(" for ");
            dumpTimeSec(printWriter, dayStats.successTime);
            printWriter.print(" avg=");
            dumpTimeSec(printWriter, dayStats.successTime / ((long) dayStats.successCount));
        }
        printWriter.print(") Failure (");
        printWriter.print(dayStats.failureCount);
        if (dayStats.failureCount > 0) {
            printWriter.print(" for ");
            dumpTimeSec(printWriter, dayStats.failureTime);
            printWriter.print(" avg=");
            dumpTimeSec(printWriter, dayStats.failureTime / ((long) dayStats.failureCount));
        }
        printWriter.println(")");
    }

    protected void dumpSyncHistory(PrintWriter printWriter) {
        dumpRecentHistory(printWriter);
        dumpDayStatistics(printWriter);
    }

    private void dumpRecentHistory(PrintWriter printWriter) {
        PrintWriter printWriter2;
        ArrayList<SyncStorageEngine.SyncHistoryItem> arrayList;
        int i;
        String str;
        String str2;
        String str3;
        String str4;
        int i2;
        ArrayList<SyncStorageEngine.SyncHistoryItem> arrayList2;
        String str5;
        PackageManager packageManager;
        String str6;
        String str7;
        String strValueOf;
        Iterator<SyncStorageEngine.SyncHistoryItem> it;
        String str8;
        String string;
        PrintWriter printWriter3 = printWriter;
        ArrayList<SyncStorageEngine.SyncHistoryItem> syncHistory = this.mSyncStorageEngine.getSyncHistory();
        if (syncHistory != null && syncHistory.size() > 0) {
            HashMap mapNewHashMap = Maps.newHashMap();
            int size = syncHistory.size();
            Iterator<SyncStorageEngine.SyncHistoryItem> it2 = syncHistory.iterator();
            long j = 0;
            long j2 = 0;
            int i3 = 0;
            int i4 = 0;
            while (it2.hasNext()) {
                SyncStorageEngine.SyncHistoryItem next = it2.next();
                SyncStorageEngine.AuthorityInfo authority = this.mSyncStorageEngine.getAuthority(next.authorityId);
                if (authority != null) {
                    str8 = authority.target.provider;
                    StringBuilder sb = new StringBuilder();
                    it = it2;
                    sb.append(authority.target.account.name);
                    sb.append(SliceClientPermissions.SliceAuthority.DELIMITER);
                    sb.append(authority.target.account.type);
                    sb.append(" u");
                    sb.append(authority.target.userId);
                    string = sb.toString();
                } else {
                    it = it2;
                    str8 = "Unknown";
                    string = "Unknown";
                }
                int length = str8.length();
                if (length > i3) {
                    i3 = length;
                }
                int length2 = string.length();
                if (length2 > i4) {
                    i4 = length2;
                }
                int i5 = i3;
                int i6 = i4;
                long j3 = next.elapsedTime;
                long j4 = j + j3;
                j2++;
                AuthoritySyncStats authoritySyncStats = (AuthoritySyncStats) mapNewHashMap.get(str8);
                if (authoritySyncStats == null) {
                    authoritySyncStats = new AuthoritySyncStats(str8);
                    mapNewHashMap.put(str8, authoritySyncStats);
                }
                authoritySyncStats.elapsedTime += j3;
                authoritySyncStats.times++;
                Map<String, AccountSyncStats> map = authoritySyncStats.accountMap;
                AccountSyncStats accountSyncStats = map.get(string);
                if (accountSyncStats == null) {
                    accountSyncStats = new AccountSyncStats(string);
                    map.put(string, accountSyncStats);
                }
                accountSyncStats.elapsedTime += j3;
                accountSyncStats.times++;
                it2 = it;
                i3 = i5;
                i4 = i6;
                j = j4;
            }
            if (j > 0) {
                printWriter.println();
                printWriter3.printf("Detailed Statistics (Recent history):  %d (# of times) %ds (sync time)\n", Long.valueOf(j2), Long.valueOf(j / 1000));
                ArrayList arrayList3 = new ArrayList(mapNewHashMap.values());
                Collections.sort(arrayList3, new Comparator<AuthoritySyncStats>() {
                    @Override
                    public int compare(AuthoritySyncStats authoritySyncStats2, AuthoritySyncStats authoritySyncStats3) {
                        int iCompare = Integer.compare(authoritySyncStats3.times, authoritySyncStats2.times);
                        if (iCompare == 0) {
                            return Long.compare(authoritySyncStats3.elapsedTime, authoritySyncStats2.elapsedTime);
                        }
                        return iCompare;
                    }
                });
                int iMax = Math.max(i3, i4 + 3);
                char[] cArr = new char[4 + iMax + 2 + 10 + 11];
                Arrays.fill(cArr, '-');
                String str9 = new String(cArr);
                String str10 = String.format("  %%-%ds: %%-9s  %%-11s\n", Integer.valueOf(iMax + 2));
                arrayList = syncHistory;
                String str11 = String.format("    %%-%ds:   %%-9s  %%-11s\n", Integer.valueOf(iMax));
                printWriter3.println(str9);
                Iterator it3 = arrayList3.iterator();
                while (it3.hasNext()) {
                    AuthoritySyncStats authoritySyncStats2 = (AuthoritySyncStats) it3.next();
                    String str12 = authoritySyncStats2.name;
                    Iterator it4 = it3;
                    int i7 = size;
                    long j5 = authoritySyncStats2.elapsedTime;
                    int i8 = authoritySyncStats2.times;
                    int i9 = i3;
                    String str13 = str9;
                    int i10 = i4;
                    printWriter.printf(str10, str12, String.format("%d/%d%%", Integer.valueOf(i8), Long.valueOf(((long) (i8 * 100)) / j2)), String.format("%ds/%d%%", Long.valueOf(j5 / 1000), Long.valueOf((j5 * 100) / j)));
                    ArrayList<AccountSyncStats> arrayList4 = new ArrayList(authoritySyncStats2.accountMap.values());
                    Collections.sort(arrayList4, new Comparator<AccountSyncStats>() {
                        @Override
                        public int compare(AccountSyncStats accountSyncStats2, AccountSyncStats accountSyncStats3) {
                            int iCompare = Integer.compare(accountSyncStats3.times, accountSyncStats2.times);
                            if (iCompare == 0) {
                                return Long.compare(accountSyncStats3.elapsedTime, accountSyncStats2.elapsedTime);
                            }
                            return iCompare;
                        }
                    });
                    for (AccountSyncStats accountSyncStats2 : arrayList4) {
                        long j6 = accountSyncStats2.elapsedTime;
                        int i11 = accountSyncStats2.times;
                        printWriter.printf(str11, accountSyncStats2.name, String.format("%d/%d%%", Integer.valueOf(i11), Long.valueOf(((long) (i11 * 100)) / j2)), String.format("%ds/%d%%", Long.valueOf(j6 / 1000), Long.valueOf((j6 * 100) / j)));
                        str10 = str10;
                    }
                    printWriter.println(str13);
                    str9 = str13;
                    printWriter3 = printWriter;
                    size = i7;
                    it3 = it4;
                    i3 = i9;
                    i4 = i10;
                }
                printWriter2 = printWriter3;
            } else {
                printWriter2 = printWriter3;
                arrayList = syncHistory;
            }
            int i12 = size;
            printWriter.println();
            printWriter2.println("Recent Sync History");
            printWriter2.println("(SERVER is now split up to FEED and OTHER)");
            String str14 = "  %-" + i4 + "s  %-" + i3 + "s %s\n";
            HashMap mapNewHashMap2 = Maps.newHashMap();
            SyncManager syncManager = this;
            PackageManager packageManager2 = syncManager.mContext.getPackageManager();
            int i13 = 0;
            while (true) {
                i = i12;
                if (i13 >= i) {
                    break;
                }
                ArrayList<SyncStorageEngine.SyncHistoryItem> arrayList5 = arrayList;
                SyncStorageEngine.SyncHistoryItem syncHistoryItem = arrayList5.get(i13);
                SyncStorageEngine.AuthorityInfo authority2 = syncManager.mSyncStorageEngine.getAuthority(syncHistoryItem.authorityId);
                if (authority2 != null) {
                    str3 = authority2.target.provider;
                    str4 = authority2.target.account.name + SliceClientPermissions.SliceAuthority.DELIMITER + authority2.target.account.type + " u" + authority2.target.userId;
                } else {
                    str3 = "Unknown";
                    str4 = "Unknown";
                }
                long j7 = syncHistoryItem.elapsedTime;
                Time time = new Time();
                long j8 = syncHistoryItem.eventTime;
                time.set(j8);
                String str15 = str3 + SliceClientPermissions.SliceAuthority.DELIMITER + str4;
                Long l = (Long) mapNewHashMap2.get(str15);
                if (l == null) {
                    strValueOf = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
                    str5 = str14;
                    packageManager = packageManager2;
                    i2 = i;
                    arrayList2 = arrayList5;
                } else {
                    i2 = i;
                    arrayList2 = arrayList5;
                    long jLongValue = (l.longValue() - j8) / 1000;
                    if (jLongValue < 60) {
                        strValueOf = String.valueOf(jLongValue);
                        str5 = str14;
                        packageManager = packageManager2;
                    } else {
                        if (jLongValue < 3600) {
                            str5 = str14;
                            packageManager = packageManager2;
                            str6 = str4;
                            str7 = str3;
                            strValueOf = String.format("%02d:%02d", Long.valueOf(jLongValue / 60), Long.valueOf(jLongValue % 60));
                        } else {
                            str5 = str14;
                            packageManager = packageManager2;
                            str6 = str4;
                            str7 = str3;
                            long j9 = jLongValue % 3600;
                            strValueOf = String.format("%02d:%02d:%02d", Long.valueOf(jLongValue / 3600), Long.valueOf(j9 / 60), Long.valueOf(j9 % 60));
                        }
                        mapNewHashMap2.put(str15, Long.valueOf(j8));
                        i13++;
                        printWriter2.printf("  #%-3d: %s %8s  %5.1fs  %8s", Integer.valueOf(i13), formatTime(j8), SyncStorageEngine.SOURCES[syncHistoryItem.source], Float.valueOf(j7 / 1000.0f), strValueOf);
                        PackageManager packageManager3 = packageManager;
                        String str16 = str5;
                        printWriter2.printf(str16, str6, str7, SyncOperation.reasonToString(packageManager3, syncHistoryItem.reason));
                        if (syncHistoryItem.event == 1 || syncHistoryItem.upstreamActivity != 0 || syncHistoryItem.downstreamActivity != 0) {
                            printWriter2.printf("    event=%d upstreamActivity=%d downstreamActivity=%d\n", Integer.valueOf(syncHistoryItem.event), Long.valueOf(syncHistoryItem.upstreamActivity), Long.valueOf(syncHistoryItem.downstreamActivity));
                        }
                        if (syncHistoryItem.mesg == null && !SyncStorageEngine.MESG_SUCCESS.equals(syncHistoryItem.mesg)) {
                            printWriter2.printf("    mesg=%s\n", syncHistoryItem.mesg);
                        }
                        str14 = str16;
                        packageManager2 = packageManager3;
                        i12 = i2;
                        arrayList = arrayList2;
                        syncManager = this;
                    }
                }
                str6 = str4;
                str7 = str3;
                mapNewHashMap2.put(str15, Long.valueOf(j8));
                i13++;
                printWriter2.printf("  #%-3d: %s %8s  %5.1fs  %8s", Integer.valueOf(i13), formatTime(j8), SyncStorageEngine.SOURCES[syncHistoryItem.source], Float.valueOf(j7 / 1000.0f), strValueOf);
                PackageManager packageManager32 = packageManager;
                String str162 = str5;
                printWriter2.printf(str162, str6, str7, SyncOperation.reasonToString(packageManager32, syncHistoryItem.reason));
                if (syncHistoryItem.event == 1) {
                }
                printWriter2.printf("    event=%d upstreamActivity=%d downstreamActivity=%d\n", Integer.valueOf(syncHistoryItem.event), Long.valueOf(syncHistoryItem.upstreamActivity), Long.valueOf(syncHistoryItem.downstreamActivity));
                if (syncHistoryItem.mesg == null) {
                }
                str14 = str162;
                packageManager2 = packageManager32;
                i12 = i2;
                arrayList = arrayList2;
                syncManager = this;
            }
            int i14 = i;
            ArrayList<SyncStorageEngine.SyncHistoryItem> arrayList6 = arrayList;
            String str17 = str14;
            printWriter.println();
            printWriter2.println("Recent Sync History Extras");
            printWriter2.println("(SERVER is now split up to FEED and OTHER)");
            int i15 = 0;
            while (true) {
                int i16 = i14;
                if (i15 < i16) {
                    ArrayList<SyncStorageEngine.SyncHistoryItem> arrayList7 = arrayList6;
                    SyncStorageEngine.SyncHistoryItem syncHistoryItem2 = arrayList7.get(i15);
                    Bundle bundle = syncHistoryItem2.extras;
                    if (bundle != null && bundle.size() != 0) {
                        SyncStorageEngine.AuthorityInfo authority3 = this.mSyncStorageEngine.getAuthority(syncHistoryItem2.authorityId);
                        if (authority3 != null) {
                            str = authority3.target.provider;
                            str2 = authority3.target.account.name + SliceClientPermissions.SliceAuthority.DELIMITER + authority3.target.account.type + " u" + authority3.target.userId;
                        } else {
                            str = "Unknown";
                            str2 = "Unknown";
                        }
                        Time time2 = new Time();
                        long j10 = syncHistoryItem2.eventTime;
                        time2.set(j10);
                        printWriter2.printf("  #%-3d: %s %8s ", Integer.valueOf(i15 + 1), formatTime(j10), SyncStorageEngine.SOURCES[syncHistoryItem2.source]);
                        printWriter2.printf(str17, str2, str, bundle);
                    }
                    i15++;
                    i14 = i16;
                    arrayList6 = arrayList7;
                } else {
                    return;
                }
            }
        }
    }

    private void dumpDayStatistics(PrintWriter printWriter) {
        SyncStorageEngine.DayStats dayStats;
        int i;
        SyncStorageEngine.DayStats[] dayStatistics = this.mSyncStorageEngine.getDayStatistics();
        if (dayStatistics != null && dayStatistics[0] != null) {
            printWriter.println();
            printWriter.println("Sync Statistics");
            printWriter.print("  Today:  ");
            dumpDayStatistic(printWriter, dayStatistics[0]);
            int i2 = dayStatistics[0].day;
            int length = 1;
            while (length <= 6 && length < dayStatistics.length && (dayStats = dayStatistics[length]) != null && (i = i2 - dayStats.day) <= 6) {
                printWriter.print("  Day-");
                printWriter.print(i);
                printWriter.print(":  ");
                dumpDayStatistic(printWriter, dayStats);
                length++;
            }
            int i3 = i2;
            while (length < dayStatistics.length) {
                SyncStorageEngine.DayStats dayStats2 = null;
                i3 -= 7;
                while (true) {
                    if (length >= dayStatistics.length) {
                        break;
                    }
                    SyncStorageEngine.DayStats dayStats3 = dayStatistics[length];
                    if (dayStats3 == null) {
                        length = dayStatistics.length;
                        break;
                    }
                    if (i3 - dayStats3.day > 6) {
                        break;
                    }
                    length++;
                    if (dayStats2 == null) {
                        dayStats2 = new SyncStorageEngine.DayStats(i3);
                    }
                    dayStats2.successCount += dayStats3.successCount;
                    dayStats2.successTime += dayStats3.successTime;
                    dayStats2.failureCount += dayStats3.failureCount;
                    dayStats2.failureTime += dayStats3.failureTime;
                }
                if (dayStats2 != null) {
                    printWriter.print("  Week-");
                    printWriter.print((i2 - i3) / 7);
                    printWriter.print(": ");
                    dumpDayStatistic(printWriter, dayStats2);
                }
            }
        }
    }

    private void dumpSyncAdapters(IndentingPrintWriter indentingPrintWriter) {
        indentingPrintWriter.println();
        List<UserInfo> allUsers = getAllUsers();
        if (allUsers != null) {
            for (UserInfo userInfo : allUsers) {
                indentingPrintWriter.println("Sync adapters for " + userInfo + ":");
                indentingPrintWriter.increaseIndent();
                Iterator it = this.mSyncAdapters.getAllServices(userInfo.id).iterator();
                while (it.hasNext()) {
                    indentingPrintWriter.println((RegisteredServicesCache.ServiceInfo) it.next());
                }
                indentingPrintWriter.decreaseIndent();
                indentingPrintWriter.println();
            }
        }
    }

    private static class AuthoritySyncStats {
        Map<String, AccountSyncStats> accountMap;
        long elapsedTime;
        String name;
        int times;

        private AuthoritySyncStats(String str) {
            this.accountMap = Maps.newHashMap();
            this.name = str;
        }
    }

    private static class AccountSyncStats {
        long elapsedTime;
        String name;
        int times;

        private AccountSyncStats(String str) {
            this.name = str;
        }
    }

    static void sendOnUnsyncableAccount(final Context context, RegisteredServicesCache.ServiceInfo<SyncAdapterType> serviceInfo, int i, OnReadyCallback onReadyCallback) {
        final OnUnsyncableAccountCheck onUnsyncableAccountCheck = new OnUnsyncableAccountCheck(serviceInfo, onReadyCallback);
        if (context.bindServiceAsUser(getAdapterBindIntent(context, serviceInfo.componentName, i), onUnsyncableAccountCheck, 21, UserHandle.of(i))) {
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public final void run() {
                    context.unbindService(onUnsyncableAccountCheck);
                }
            }, 5000L);
        } else {
            onUnsyncableAccountCheck.onReady();
        }
    }

    private static class OnUnsyncableAccountCheck implements ServiceConnection {
        static final long SERVICE_BOUND_TIME_MILLIS = 5000;
        private final OnReadyCallback mOnReadyCallback;
        private final RegisteredServicesCache.ServiceInfo<SyncAdapterType> mSyncAdapterInfo;

        OnUnsyncableAccountCheck(RegisteredServicesCache.ServiceInfo<SyncAdapterType> serviceInfo, OnReadyCallback onReadyCallback) {
            this.mSyncAdapterInfo = serviceInfo;
            this.mOnReadyCallback = onReadyCallback;
        }

        private void onReady() {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                this.mOnReadyCallback.onReady();
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            try {
                ISyncAdapter.Stub.asInterface(iBinder).onUnsyncableAccount(new ISyncAdapterUnsyncableAccountCallback.Stub() {
                    public void onUnsyncableAccountDone(boolean z) {
                        if (z) {
                            OnUnsyncableAccountCheck.this.onReady();
                        }
                    }
                });
            } catch (RemoteException e) {
                Slog.e("SyncManager", "Could not call onUnsyncableAccountDone " + this.mSyncAdapterInfo, e);
                onReady();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    }

    private class SyncTimeTracker {
        boolean mLastWasSyncing;
        private long mTimeSpentSyncing;
        long mWhenSyncStarted;

        private SyncTimeTracker() {
            this.mLastWasSyncing = false;
            this.mWhenSyncStarted = 0L;
        }

        public synchronized void update() {
            boolean z = !SyncManager.this.mActiveSyncContexts.isEmpty();
            if (z == this.mLastWasSyncing) {
                return;
            }
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            if (z) {
                this.mWhenSyncStarted = jElapsedRealtime;
            } else {
                this.mTimeSpentSyncing += jElapsedRealtime - this.mWhenSyncStarted;
            }
            this.mLastWasSyncing = z;
        }

        public synchronized long timeSpentSyncing() {
            if (!this.mLastWasSyncing) {
                return this.mTimeSpentSyncing;
            }
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            return this.mTimeSpentSyncing + (jElapsedRealtime - this.mWhenSyncStarted);
        }
    }

    class ServiceConnectionData {
        public final ActiveSyncContext activeSyncContext;
        public final IBinder adapter;

        ServiceConnectionData(ActiveSyncContext activeSyncContext, IBinder iBinder) {
            this.activeSyncContext = activeSyncContext;
            this.adapter = iBinder;
        }
    }

    public static boolean readyToSync() {
        boolean z;
        synchronized (SyncManager.class) {
            z = sInstance != null && sInstance.mProvisioned && sInstance.mBootCompleted && sInstance.mJobServiceReady;
        }
        return z;
    }

    class SyncHandler extends Handler {
        private static final int MESSAGE_ACCOUNTS_UPDATED = 9;
        private static final int MESSAGE_CANCEL = 6;
        static final int MESSAGE_JOBSERVICE_OBJECT = 7;
        private static final int MESSAGE_MONITOR_SYNC = 8;
        private static final int MESSAGE_RELEASE_MESSAGES_FROM_QUEUE = 2;
        static final int MESSAGE_REMOVE_PERIODIC_SYNC = 14;
        static final int MESSAGE_SCHEDULE_SYNC = 12;
        private static final int MESSAGE_SERVICE_CONNECTED = 4;
        private static final int MESSAGE_SERVICE_DISCONNECTED = 5;
        static final int MESSAGE_START_SYNC = 10;
        static final int MESSAGE_STOP_SYNC = 11;
        private static final int MESSAGE_SYNC_FINISHED = 1;
        static final int MESSAGE_UPDATE_PERIODIC_SYNC = 13;
        public final SyncTimeTracker mSyncTimeTracker;
        private List<Message> mUnreadyQueue;
        private final HashMap<String, PowerManager.WakeLock> mWakeLocks;

        void onBootCompleted() {
            if (Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", "Boot completed.");
            }
            checkIfDeviceReady();
        }

        void onDeviceProvisioned() {
            if (Log.isLoggable("SyncManager", 3)) {
                Log.d("SyncManager", "mProvisioned=" + SyncManager.this.mProvisioned);
            }
            checkIfDeviceReady();
        }

        void checkIfDeviceReady() {
            if (SyncManager.this.mProvisioned && SyncManager.this.mBootCompleted && SyncManager.this.mJobServiceReady) {
                synchronized (this) {
                    SyncManager.this.mSyncStorageEngine.restoreAllPeriodicSyncs();
                    obtainMessage(2).sendToTarget();
                }
            }
        }

        private boolean tryEnqueueMessageUntilReadyToRun(Message message) {
            synchronized (this) {
                if (SyncManager.this.mBootCompleted && SyncManager.this.mProvisioned && SyncManager.this.mJobServiceReady) {
                    return false;
                }
                this.mUnreadyQueue.add(Message.obtain(message));
                return true;
            }
        }

        public SyncHandler(Looper looper) {
            super(looper);
            this.mSyncTimeTracker = new SyncTimeTracker();
            this.mWakeLocks = Maps.newHashMap();
            this.mUnreadyQueue = new ArrayList();
        }

        @Override
        public void handleMessage(Message message) {
            try {
                SyncManager.this.mSyncManagerWakeLock.acquire();
                if (message.what == 7) {
                    Slog.i("SyncManager", "Got SyncJobService instance.");
                    SyncManager.this.mSyncJobService = (SyncJobService) message.obj;
                    SyncManager.this.mJobServiceReady = true;
                    checkIfDeviceReady();
                } else if (message.what == 9) {
                    if (Log.isLoggable("SyncManager", 2)) {
                        Slog.v("SyncManager", "handleSyncHandlerMessage: MESSAGE_ACCOUNTS_UPDATED");
                    }
                    updateRunningAccountsH((SyncStorageEngine.EndPoint) message.obj);
                } else if (message.what == 2) {
                    if (this.mUnreadyQueue != null) {
                        Iterator<Message> it = this.mUnreadyQueue.iterator();
                        while (it.hasNext()) {
                            handleSyncMessage(it.next());
                        }
                        this.mUnreadyQueue = null;
                    }
                } else if (!tryEnqueueMessageUntilReadyToRun(message)) {
                    handleSyncMessage(message);
                }
            } finally {
                SyncManager.this.mSyncManagerWakeLock.release();
            }
        }

        private void handleSyncMessage(Message message) {
            boolean zIsLoggable = Log.isLoggable("SyncManager", 2);
            try {
                SyncManager.this.mDataConnectionIsConnected = SyncManager.this.readDataConnectionState();
                boolean z = true;
                switch (message.what) {
                    case 1:
                        SyncFinishedOrCancelledMessagePayload syncFinishedOrCancelledMessagePayload = (SyncFinishedOrCancelledMessagePayload) message.obj;
                        if (!SyncManager.this.isSyncStillActiveH(syncFinishedOrCancelledMessagePayload.activeSyncContext)) {
                            Log.d("SyncManager", "handleSyncHandlerMessage: dropping since the sync is no longer active: " + syncFinishedOrCancelledMessagePayload.activeSyncContext);
                        } else {
                            if (zIsLoggable) {
                                Slog.v("SyncManager", "syncFinished" + syncFinishedOrCancelledMessagePayload.activeSyncContext.mSyncOperation);
                            }
                            SyncManager.this.mSyncJobService.callJobFinished(syncFinishedOrCancelledMessagePayload.activeSyncContext.mSyncOperation.jobId, false, "sync finished");
                            runSyncFinishedOrCanceledH(syncFinishedOrCancelledMessagePayload.syncResult, syncFinishedOrCancelledMessagePayload.activeSyncContext);
                        }
                        return;
                    case 2:
                    case 3:
                    case 7:
                    case 9:
                    default:
                        return;
                    case 4:
                        ServiceConnectionData serviceConnectionData = (ServiceConnectionData) message.obj;
                        if (Log.isLoggable("SyncManager", 2)) {
                            Log.d("SyncManager", "handleSyncHandlerMessage: MESSAGE_SERVICE_CONNECTED: " + serviceConnectionData.activeSyncContext);
                        }
                        if (SyncManager.this.isSyncStillActiveH(serviceConnectionData.activeSyncContext)) {
                            runBoundToAdapterH(serviceConnectionData.activeSyncContext, serviceConnectionData.adapter);
                        }
                        return;
                    case 5:
                        ActiveSyncContext activeSyncContext = ((ServiceConnectionData) message.obj).activeSyncContext;
                        if (Log.isLoggable("SyncManager", 2)) {
                            Log.d("SyncManager", "handleSyncHandlerMessage: MESSAGE_SERVICE_DISCONNECTED: " + activeSyncContext);
                        }
                        if (SyncManager.this.isSyncStillActiveH(activeSyncContext)) {
                            try {
                                if (activeSyncContext.mSyncAdapter != null) {
                                    SyncManager.this.mLogger.log("Calling cancelSync for SERVICE_DISCONNECTED ", activeSyncContext, " adapter=", activeSyncContext.mSyncAdapter);
                                    activeSyncContext.mSyncAdapter.cancelSync(activeSyncContext);
                                    SyncManager.this.mLogger.log("Canceled");
                                }
                            } catch (RemoteException e) {
                                SyncManager.this.mLogger.log("RemoteException ", Log.getStackTraceString(e));
                            }
                            SyncResult syncResult = new SyncResult();
                            syncResult.stats.numIoExceptions++;
                            SyncManager.this.mSyncJobService.callJobFinished(activeSyncContext.mSyncOperation.jobId, false, "service disconnected");
                            runSyncFinishedOrCanceledH(syncResult, activeSyncContext);
                            break;
                        }
                        return;
                    case 6:
                        SyncStorageEngine.EndPoint endPoint = (SyncStorageEngine.EndPoint) message.obj;
                        Bundle bundlePeekData = message.peekData();
                        if (Log.isLoggable("SyncManager", 3)) {
                            Log.d("SyncManager", "handleSyncHandlerMessage: MESSAGE_CANCEL: " + endPoint + " bundle: " + bundlePeekData);
                        }
                        cancelActiveSyncH(endPoint, bundlePeekData, "MESSAGE_CANCEL");
                        return;
                    case 8:
                        ActiveSyncContext activeSyncContext2 = (ActiveSyncContext) message.obj;
                        if (Log.isLoggable("SyncManager", 3)) {
                            Log.d("SyncManager", "handleSyncHandlerMessage: MESSAGE_MONITOR_SYNC: " + activeSyncContext2.mSyncOperation.target);
                        }
                        if (!isSyncNotUsingNetworkH(activeSyncContext2)) {
                            SyncManager.this.postMonitorSyncProgressMessage(activeSyncContext2);
                        } else {
                            Log.w("SyncManager", String.format("Detected sync making no progress for %s. cancelling.", activeSyncContext2));
                            SyncManager.this.mSyncJobService.callJobFinished(activeSyncContext2.mSyncOperation.jobId, false, "no network activity");
                            runSyncFinishedOrCanceledH(null, activeSyncContext2);
                        }
                        return;
                    case 10:
                        startSyncH((SyncOperation) message.obj);
                        return;
                    case 11:
                        SyncOperation syncOperation = (SyncOperation) message.obj;
                        if (zIsLoggable) {
                            Slog.v("SyncManager", "Stop sync received.");
                        }
                        ActiveSyncContext activeSyncContextFindActiveSyncContextH = findActiveSyncContextH(syncOperation.jobId);
                        if (activeSyncContextFindActiveSyncContextH != null) {
                            runSyncFinishedOrCanceledH(null, activeSyncContextFindActiveSyncContextH);
                            boolean z2 = message.arg1 != 0;
                            if (message.arg2 == 0) {
                                z = false;
                            }
                            if (zIsLoggable) {
                                Slog.v("SyncManager", "Stopping sync. Reschedule: " + z2 + "Backoff: " + z);
                            }
                            if (z) {
                                SyncManager.this.increaseBackoffSetting(syncOperation.target);
                            }
                            if (z2) {
                                deferStoppedSyncH(syncOperation, 0L);
                            }
                        }
                        return;
                    case 12:
                        ScheduleSyncMessagePayload scheduleSyncMessagePayload = (ScheduleSyncMessagePayload) message.obj;
                        SyncManager.this.scheduleSyncOperationH(scheduleSyncMessagePayload.syncOperation, scheduleSyncMessagePayload.minDelayMillis);
                        return;
                    case 13:
                        UpdatePeriodicSyncMessagePayload updatePeriodicSyncMessagePayload = (UpdatePeriodicSyncMessagePayload) message.obj;
                        updateOrAddPeriodicSyncH(updatePeriodicSyncMessagePayload.target, updatePeriodicSyncMessagePayload.pollFrequency, updatePeriodicSyncMessagePayload.flex, updatePeriodicSyncMessagePayload.extras);
                        return;
                    case 14:
                        Pair pair = (Pair) message.obj;
                        removePeriodicSyncH((SyncStorageEngine.EndPoint) pair.first, message.getData(), (String) pair.second);
                        return;
                }
            } finally {
                this.mSyncTimeTracker.update();
            }
            this.mSyncTimeTracker.update();
        }

        private PowerManager.WakeLock getSyncWakeLock(SyncOperation syncOperation) {
            String strWakeLockName = syncOperation.wakeLockName();
            PowerManager.WakeLock wakeLock = this.mWakeLocks.get(strWakeLockName);
            if (wakeLock == null) {
                PowerManager.WakeLock wakeLockNewWakeLock = SyncManager.this.mPowerManager.newWakeLock(1, SyncManager.SYNC_WAKE_LOCK_PREFIX + strWakeLockName);
                wakeLockNewWakeLock.setReferenceCounted(false);
                this.mWakeLocks.put(strWakeLockName, wakeLockNewWakeLock);
                return wakeLockNewWakeLock;
            }
            return wakeLock;
        }

        private void deferSyncH(SyncOperation syncOperation, long j, String str) {
            SyncLogger syncLogger = SyncManager.this.mLogger;
            Object[] objArr = new Object[8];
            objArr[0] = "deferSyncH() ";
            objArr[1] = syncOperation.isPeriodic ? "periodic " : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            objArr[2] = "sync.  op=";
            objArr[3] = syncOperation;
            objArr[4] = " delay=";
            objArr[5] = Long.valueOf(j);
            objArr[6] = " why=";
            objArr[7] = str;
            syncLogger.log(objArr);
            SyncManager.this.mSyncJobService.callJobFinished(syncOperation.jobId, false, str);
            if (syncOperation.isPeriodic) {
                SyncManager.this.scheduleSyncOperationH(syncOperation.createOneTimeSyncOperation(), j);
            } else {
                SyncManager.this.cancelJob(syncOperation, "deferSyncH()");
                SyncManager.this.scheduleSyncOperationH(syncOperation, j);
            }
        }

        private void deferStoppedSyncH(SyncOperation syncOperation, long j) {
            if (syncOperation.isPeriodic) {
                SyncManager.this.scheduleSyncOperationH(syncOperation.createOneTimeSyncOperation(), j);
            } else {
                SyncManager.this.scheduleSyncOperationH(syncOperation, j);
            }
        }

        private void deferActiveSyncH(ActiveSyncContext activeSyncContext, String str) {
            SyncOperation syncOperation = activeSyncContext.mSyncOperation;
            runSyncFinishedOrCanceledH(null, activeSyncContext);
            deferSyncH(syncOperation, 10000L, str);
        }

        private void startSyncH(SyncOperation syncOperation) {
            boolean zIsLoggable = Log.isLoggable("SyncManager", 2);
            if (zIsLoggable) {
                Slog.v("SyncManager", syncOperation.toString());
            }
            SyncManager.this.mSyncStorageEngine.setClockValid();
            SyncManager.this.mSyncJobService.markSyncStarted(syncOperation.jobId);
            if (SyncManager.this.mStorageIsLow) {
                deferSyncH(syncOperation, 3600000L, "storage low");
            }
            if (syncOperation.isPeriodic) {
                Iterator it = SyncManager.this.getAllPendingSyncs().iterator();
                while (it.hasNext()) {
                    if (((SyncOperation) it.next()).sourcePeriodicId == syncOperation.jobId) {
                        SyncManager.this.mSyncJobService.callJobFinished(syncOperation.jobId, false, "periodic sync, pending");
                        return;
                    }
                }
                Iterator<ActiveSyncContext> it2 = SyncManager.this.mActiveSyncContexts.iterator();
                while (it2.hasNext()) {
                    if (it2.next().mSyncOperation.sourcePeriodicId == syncOperation.jobId) {
                        SyncManager.this.mSyncJobService.callJobFinished(syncOperation.jobId, false, "periodic sync, already running");
                        return;
                    }
                }
                if (SyncManager.this.isAdapterDelayed(syncOperation.target)) {
                    deferSyncH(syncOperation, 0L, "backing off");
                    return;
                }
            }
            Iterator<ActiveSyncContext> it3 = SyncManager.this.mActiveSyncContexts.iterator();
            while (true) {
                if (!it3.hasNext()) {
                    break;
                }
                ActiveSyncContext next = it3.next();
                if (next.mSyncOperation.isConflict(syncOperation)) {
                    if (next.mSyncOperation.findPriority() >= syncOperation.findPriority()) {
                        if (zIsLoggable) {
                            Slog.v("SyncManager", "Rescheduling sync due to conflict " + syncOperation.toString());
                        }
                        deferSyncH(syncOperation, 10000L, "delay on conflict");
                        return;
                    }
                    if (zIsLoggable) {
                        Slog.v("SyncManager", "Pushing back running sync due to a higher priority sync");
                    }
                    deferActiveSyncH(next, "preempted");
                }
            }
            int iComputeSyncOpState = computeSyncOpState(syncOperation);
            switch (iComputeSyncOpState) {
                case 1:
                case 2:
                    SyncManager.this.mSyncJobService.callJobFinished(syncOperation.jobId, false, "invalid op state: " + iComputeSyncOpState);
                    break;
                default:
                    if (!dispatchSyncOperation(syncOperation)) {
                        SyncManager.this.mSyncJobService.callJobFinished(syncOperation.jobId, false, "dispatchSyncOperation() failed");
                    }
                    SyncManager.this.setAuthorityPendingState(syncOperation.target);
                    break;
            }
        }

        private ActiveSyncContext findActiveSyncContextH(int i) {
            for (ActiveSyncContext activeSyncContext : SyncManager.this.mActiveSyncContexts) {
                SyncOperation syncOperation = activeSyncContext.mSyncOperation;
                if (syncOperation != null && syncOperation.jobId == i) {
                    return activeSyncContext;
                }
            }
            return null;
        }

        private void updateRunningAccountsH(SyncStorageEngine.EndPoint endPoint) {
            AccountAndUser[] accountAndUserArr = SyncManager.this.mRunningAccounts;
            SyncManager.this.mRunningAccounts = AccountManagerService.getSingleton().getRunningAccounts();
            if (Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", "Accounts list: ");
                for (AccountAndUser accountAndUser : SyncManager.this.mRunningAccounts) {
                    Slog.v("SyncManager", accountAndUser.toString());
                }
            }
            if (SyncManager.this.mLogger.enabled()) {
                SyncManager.this.mLogger.log("updateRunningAccountsH: ", Arrays.toString(SyncManager.this.mRunningAccounts));
            }
            if (SyncManager.this.mBootCompleted) {
                SyncManager.this.doDatabaseCleanup();
            }
            AccountAndUser[] accountAndUserArr2 = SyncManager.this.mRunningAccounts;
            for (ActiveSyncContext activeSyncContext : SyncManager.this.mActiveSyncContexts) {
                if (!SyncManager.this.containsAccountAndUser(accountAndUserArr2, activeSyncContext.mSyncOperation.target.account, activeSyncContext.mSyncOperation.target.userId)) {
                    Log.d("SyncManager", "canceling sync since the account is no longer running");
                    SyncManager.this.sendSyncFinishedOrCanceledMessage(activeSyncContext, null);
                }
            }
            AccountAndUser[] accountAndUserArr3 = SyncManager.this.mRunningAccounts;
            int length = accountAndUserArr3.length;
            int i = 0;
            while (true) {
                if (i >= length) {
                    break;
                }
                AccountAndUser accountAndUser2 = accountAndUserArr3[i];
                if (SyncManager.this.containsAccountAndUser(accountAndUserArr, accountAndUser2.account, accountAndUser2.userId)) {
                    i++;
                } else {
                    if (Log.isLoggable("SyncManager", 3)) {
                        Log.d("SyncManager", "Account " + accountAndUser2.account + " added, checking sync restore data");
                    }
                    AccountSyncSettingsBackupHelper.accountAdded(SyncManager.this.mContext);
                }
            }
            AccountAndUser[] allAccounts = AccountManagerService.getSingleton().getAllAccounts();
            for (SyncOperation syncOperation : SyncManager.this.getAllPendingSyncs()) {
                if (!SyncManager.this.containsAccountAndUser(allAccounts, syncOperation.target.account, syncOperation.target.userId)) {
                    SyncManager.this.mLogger.log("canceling: ", syncOperation);
                    SyncManager.this.cancelJob(syncOperation, "updateRunningAccountsH()");
                }
            }
            if (endPoint != null) {
                SyncManager.this.scheduleSync(endPoint.account, endPoint.userId, -2, endPoint.provider, null, -1, 0);
            }
        }

        private void maybeUpdateSyncPeriodH(SyncOperation syncOperation, long j, long j2) {
            if (j != syncOperation.periodMillis || j2 != syncOperation.flexMillis) {
                if (Log.isLoggable("SyncManager", 2)) {
                    Slog.v("SyncManager", "updating period " + syncOperation + " to " + j + " and flex to " + j2);
                }
                SyncOperation syncOperation2 = new SyncOperation(syncOperation, j, j2);
                syncOperation2.jobId = syncOperation.jobId;
                SyncManager.this.scheduleSyncOperationH(syncOperation2);
            }
        }

        private void updateOrAddPeriodicSyncH(final SyncStorageEngine.EndPoint endPoint, final long j, final long j2, final Bundle bundle) {
            boolean zIsLoggable = Log.isLoggable("SyncManager", 2);
            SyncManager.this.verifyJobScheduler();
            long j3 = j * 1000;
            long j4 = j2 * 1000;
            if (zIsLoggable) {
                Slog.v("SyncManager", "Addition to periodic syncs requested: " + endPoint + " period: " + j + " flexMillis: " + j2 + " extras: " + bundle.toString());
            }
            for (SyncOperation syncOperation : SyncManager.this.getAllPendingSyncs()) {
                if (syncOperation.isPeriodic && syncOperation.target.matchesSpec(endPoint)) {
                    if (SyncManager.syncExtrasEquals(syncOperation.extras, bundle, true)) {
                        maybeUpdateSyncPeriodH(syncOperation, j3, j4);
                    }
                }
            }
            if (zIsLoggable) {
                Slog.v("SyncManager", "Adding new periodic sync: " + endPoint + " period: " + j + " flexMillis: " + j2 + " extras: " + bundle.toString());
            }
            RegisteredServicesCache.ServiceInfo serviceInfo = SyncManager.this.mSyncAdapters.getServiceInfo(SyncAdapterType.newKey(endPoint.provider, endPoint.account.type), endPoint.userId);
            if (serviceInfo != null) {
                SyncOperation syncOperation2 = new SyncOperation(endPoint, serviceInfo.uid, serviceInfo.componentName.getPackageName(), -4, 4, bundle, ((SyncAdapterType) serviceInfo.type).allowParallelSyncs(), true, -1, j3, j4, 0);
                switch (computeSyncOpState(syncOperation2)) {
                    case 1:
                        break;
                    case 2:
                        String str = syncOperation2.owningPackage;
                        int userId = UserHandle.getUserId(syncOperation2.owningUid);
                        if (SyncManager.this.mPackageManagerInternal.wasPackageEverLaunched(str, userId)) {
                            SyncManager.this.mAccountManagerInternal.requestAccountAccess(syncOperation2.target.account, str, userId, new RemoteCallback(new RemoteCallback.OnResultListener() {
                                public final void onResult(Bundle bundle2) {
                                    SyncManager.SyncHandler.lambda$updateOrAddPeriodicSyncH$0(this.f$0, endPoint, j, j2, bundle, bundle2);
                                }
                            }));
                            break;
                        }
                        break;
                    default:
                        SyncManager.this.scheduleSyncOperationH(syncOperation2);
                        SyncManager.this.mSyncStorageEngine.reportChange(1);
                        break;
                }
            }
        }

        public static void lambda$updateOrAddPeriodicSyncH$0(SyncHandler syncHandler, SyncStorageEngine.EndPoint endPoint, long j, long j2, Bundle bundle, Bundle bundle2) {
            if (bundle2 != null && bundle2.getBoolean("booleanResult")) {
                SyncManager.this.updateOrAddPeriodicSync(endPoint, j, j2, bundle);
            }
        }

        private void removePeriodicSyncInternalH(SyncOperation syncOperation, String str) {
            for (SyncOperation syncOperation2 : SyncManager.this.getAllPendingSyncs()) {
                if (syncOperation2.sourcePeriodicId == syncOperation.jobId || syncOperation2.jobId == syncOperation.jobId) {
                    ActiveSyncContext activeSyncContextFindActiveSyncContextH = findActiveSyncContextH(syncOperation.jobId);
                    if (activeSyncContextFindActiveSyncContextH != null) {
                        SyncManager.this.mSyncJobService.callJobFinished(syncOperation.jobId, false, "removePeriodicSyncInternalH");
                        runSyncFinishedOrCanceledH(null, activeSyncContextFindActiveSyncContextH);
                    }
                    SyncManager.this.mLogger.log("removePeriodicSyncInternalH-canceling: ", syncOperation2);
                    SyncManager.this.cancelJob(syncOperation2, str);
                }
            }
        }

        private void removePeriodicSyncH(SyncStorageEngine.EndPoint endPoint, Bundle bundle, String str) {
            SyncManager.this.verifyJobScheduler();
            for (SyncOperation syncOperation : SyncManager.this.getAllPendingSyncs()) {
                if (syncOperation.isPeriodic && syncOperation.target.matchesSpec(endPoint) && SyncManager.syncExtrasEquals(syncOperation.extras, bundle, true)) {
                    removePeriodicSyncInternalH(syncOperation, str);
                }
            }
        }

        private boolean isSyncNotUsingNetworkH(ActiveSyncContext activeSyncContext) {
            boolean z;
            long totalBytesTransferredByUid = SyncManager.this.getTotalBytesTransferredByUid(activeSyncContext.mSyncAdapterUid) - activeSyncContext.mBytesTransferredAtLastPoll;
            if (Log.isLoggable("SyncManager", 3)) {
                long j = totalBytesTransferredByUid % 1048576;
                z = true;
                Log.d("SyncManager", String.format("Time since last update: %ds. Delta transferred: %dMBs,%dKBs,%dBs", Long.valueOf((SystemClock.elapsedRealtime() - activeSyncContext.mLastPolledTimeElapsed) / 1000), Long.valueOf(totalBytesTransferredByUid / 1048576), Long.valueOf(j / 1024), Long.valueOf(j % 1024)));
            } else {
                z = true;
            }
            if (totalBytesTransferredByUid <= 10) {
                return z;
            }
            return false;
        }

        private int computeSyncOpState(SyncOperation syncOperation) {
            boolean zIsLoggable = Log.isLoggable("SyncManager", 2);
            SyncStorageEngine.EndPoint endPoint = syncOperation.target;
            if (!SyncManager.this.containsAccountAndUser(SyncManager.this.mRunningAccounts, endPoint.account, endPoint.userId)) {
                if (zIsLoggable) {
                    Slog.v("SyncManager", "    Dropping sync operation: account doesn't exist.");
                }
                return 1;
            }
            int iComputeSyncable = SyncManager.this.computeSyncable(endPoint.account, endPoint.userId, endPoint.provider, true);
            if (iComputeSyncable == 3) {
                if (zIsLoggable) {
                    Slog.v("SyncManager", "    Dropping sync operation: isSyncable == SYNCABLE_NO_ACCOUNT_ACCESS");
                }
                return 2;
            }
            if (iComputeSyncable == 0) {
                if (zIsLoggable) {
                    Slog.v("SyncManager", "    Dropping sync operation: isSyncable == NOT_SYNCABLE");
                }
                return 1;
            }
            boolean z = SyncManager.this.mSyncStorageEngine.getMasterSyncAutomatically(endPoint.userId) && SyncManager.this.mSyncStorageEngine.getSyncAutomatically(endPoint.account, endPoint.userId, endPoint.provider);
            boolean z2 = syncOperation.isIgnoreSettings() || iComputeSyncable < 0;
            if (z || z2) {
                return 0;
            }
            if (zIsLoggable) {
                Slog.v("SyncManager", "    Dropping sync operation: disallowed by settings/network.");
            }
            return 1;
        }

        private boolean dispatchSyncOperation(SyncOperation syncOperation) {
            UsageStatsManagerInternal usageStatsManagerInternal;
            if (Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", "dispatchSyncOperation: we are going to sync " + syncOperation);
                Slog.v("SyncManager", "num active syncs: " + SyncManager.this.mActiveSyncContexts.size());
                Iterator<ActiveSyncContext> it = SyncManager.this.mActiveSyncContexts.iterator();
                while (it.hasNext()) {
                    Slog.v("SyncManager", it.next().toString());
                }
            }
            if (syncOperation.isAppStandbyExempted() && (usageStatsManagerInternal = (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class)) != null) {
                usageStatsManagerInternal.reportExemptedSyncStart(syncOperation.owningPackage, UserHandle.getUserId(syncOperation.owningUid));
            }
            SyncStorageEngine.EndPoint endPoint = syncOperation.target;
            SyncAdapterType syncAdapterTypeNewKey = SyncAdapterType.newKey(endPoint.provider, endPoint.account.type);
            RegisteredServicesCache.ServiceInfo serviceInfo = SyncManager.this.mSyncAdapters.getServiceInfo(syncAdapterTypeNewKey, endPoint.userId);
            if (serviceInfo == null) {
                SyncManager.this.mLogger.log("dispatchSyncOperation() failed: no sync adapter info for ", syncAdapterTypeNewKey);
                Log.d("SyncManager", "can't find a sync adapter for " + syncAdapterTypeNewKey + ", removing settings for it");
                SyncManager.this.mSyncStorageEngine.removeAuthority(endPoint);
                return false;
            }
            int i = serviceInfo.uid;
            ComponentName componentName = serviceInfo.componentName;
            ActiveSyncContext activeSyncContext = SyncManager.this.new ActiveSyncContext(syncOperation, insertStartSyncEvent(syncOperation), i);
            if (Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", "dispatchSyncOperation: starting " + activeSyncContext);
            }
            activeSyncContext.mSyncInfo = SyncManager.this.mSyncStorageEngine.addActiveSync(activeSyncContext);
            SyncManager.this.mActiveSyncContexts.add(activeSyncContext);
            SyncManager.this.postMonitorSyncProgressMessage(activeSyncContext);
            if (!activeSyncContext.bindToSyncAdapter(componentName, endPoint.userId)) {
                SyncManager.this.mLogger.log("dispatchSyncOperation() failed: bind failed. target: ", componentName);
                Slog.e("SyncManager", "Bind attempt failed - target: " + componentName);
                closeActiveSyncContext(activeSyncContext);
                return false;
            }
            return true;
        }

        private void runBoundToAdapterH(ActiveSyncContext activeSyncContext, IBinder iBinder) {
            SyncOperation syncOperation = activeSyncContext.mSyncOperation;
            try {
                activeSyncContext.mIsLinkedToDeath = true;
                iBinder.linkToDeath(activeSyncContext, 0);
                SyncManager.this.mLogger.log("Sync start: account=" + syncOperation.target.account, " authority=", syncOperation.target.provider, " reason=", SyncOperation.reasonToString(null, syncOperation.reason), " extras=", SyncOperation.extrasToString(syncOperation.extras), " adapter=", activeSyncContext.mSyncAdapter);
                activeSyncContext.mSyncAdapter = ISyncAdapter.Stub.asInterface(iBinder);
                activeSyncContext.mSyncAdapter.startSync(activeSyncContext, syncOperation.target.provider, syncOperation.target.account, syncOperation.extras);
                SyncManager.this.mLogger.log("Sync is running now...");
            } catch (RemoteException e) {
                SyncManager.this.mLogger.log("Sync failed with RemoteException: ", e.toString());
                Log.d("SyncManager", "maybeStartNextSync: caught a RemoteException, rescheduling", e);
                closeActiveSyncContext(activeSyncContext);
                SyncManager.this.increaseBackoffSetting(syncOperation.target);
                SyncManager.this.scheduleSyncOperationH(syncOperation);
            } catch (RuntimeException e2) {
                SyncManager.this.mLogger.log("Sync failed with RuntimeException: ", e2.toString());
                closeActiveSyncContext(activeSyncContext);
                Slog.e("SyncManager", "Caught RuntimeException while starting the sync " + syncOperation, e2);
            }
        }

        private void cancelActiveSyncH(SyncStorageEngine.EndPoint endPoint, Bundle bundle, String str) {
            for (ActiveSyncContext activeSyncContext : new ArrayList(SyncManager.this.mActiveSyncContexts)) {
                if (activeSyncContext != null && activeSyncContext.mSyncOperation.target.matchesSpec(endPoint) && (bundle == null || SyncManager.syncExtrasEquals(activeSyncContext.mSyncOperation.extras, bundle, false))) {
                    SyncManager.this.mSyncJobService.callJobFinished(activeSyncContext.mSyncOperation.jobId, false, str);
                    runSyncFinishedOrCanceledH(null, activeSyncContext);
                }
            }
        }

        private void reschedulePeriodicSyncH(SyncOperation syncOperation) {
            SyncOperation syncOperation2;
            Iterator it = SyncManager.this.getAllPendingSyncs().iterator();
            while (true) {
                if (it.hasNext()) {
                    syncOperation2 = (SyncOperation) it.next();
                    if (syncOperation2.isPeriodic && syncOperation.matchesPeriodicOperation(syncOperation2)) {
                        break;
                    }
                } else {
                    syncOperation2 = null;
                    break;
                }
            }
            if (syncOperation2 != null) {
                SyncManager.this.scheduleSyncOperationH(syncOperation2);
            }
        }

        private void runSyncFinishedOrCanceledH(SyncResult syncResult, ActiveSyncContext activeSyncContext) {
            String strSyncErrorToString;
            boolean zIsLoggable = Log.isLoggable("SyncManager", 2);
            SyncOperation syncOperation = activeSyncContext.mSyncOperation;
            SyncStorageEngine.EndPoint endPoint = syncOperation.target;
            if (activeSyncContext.mIsLinkedToDeath) {
                activeSyncContext.mSyncAdapter.asBinder().unlinkToDeath(activeSyncContext, 0);
                activeSyncContext.mIsLinkedToDeath = false;
            }
            long jElapsedRealtime = SystemClock.elapsedRealtime() - activeSyncContext.mStartTime;
            SyncManager.this.mLogger.log("runSyncFinishedOrCanceledH() op=", syncOperation, " result=", syncResult);
            if (syncResult != null) {
                if (zIsLoggable) {
                    Slog.v("SyncManager", "runSyncFinishedOrCanceled [finished]: " + syncOperation + ", result " + syncResult);
                }
                closeActiveSyncContext(activeSyncContext);
                if (!syncOperation.isPeriodic) {
                    SyncManager.this.cancelJob(syncOperation, "runSyncFinishedOrCanceledH()-finished");
                }
                if (!syncResult.hasError()) {
                    strSyncErrorToString = SyncStorageEngine.MESG_SUCCESS;
                    SyncManager.this.clearBackoffSetting(syncOperation.target, "sync success");
                    if (syncOperation.isDerivedFromFailedPeriodicSync()) {
                        reschedulePeriodicSyncH(syncOperation);
                    }
                } else {
                    Log.w("SyncManager", "failed sync operation " + syncOperation + ", " + syncResult);
                    syncOperation.retries = syncOperation.retries + 1;
                    if (syncOperation.retries > SyncManager.this.mConstants.getMaxRetriesWithAppStandbyExemption()) {
                        syncOperation.syncExemptionFlag = 0;
                    }
                    SyncManager.this.increaseBackoffSetting(syncOperation.target);
                    if (!syncOperation.isPeriodic) {
                        SyncManager.this.maybeRescheduleSync(syncResult, syncOperation);
                    } else {
                        SyncManager.this.postScheduleSyncMessage(syncOperation.createOneTimeSyncOperation(), 0L);
                    }
                    strSyncErrorToString = ContentResolver.syncErrorToString(syncResultToErrorNumber(syncResult));
                }
                SyncManager.this.setDelayUntilTime(syncOperation.target, syncResult.delayUntil);
            } else {
                if (zIsLoggable) {
                    Slog.v("SyncManager", "runSyncFinishedOrCanceled [canceled]: " + syncOperation);
                }
                if (!syncOperation.isPeriodic) {
                    SyncManager.this.cancelJob(syncOperation, "runSyncFinishedOrCanceledH()-canceled");
                }
                if (activeSyncContext.mSyncAdapter != null) {
                    try {
                        SyncManager.this.mLogger.log("Calling cancelSync for runSyncFinishedOrCanceled ", activeSyncContext, "  adapter=", activeSyncContext.mSyncAdapter);
                        activeSyncContext.mSyncAdapter.cancelSync(activeSyncContext);
                        SyncManager.this.mLogger.log("Canceled");
                    } catch (RemoteException e) {
                        SyncManager.this.mLogger.log("RemoteException ", Log.getStackTraceString(e));
                    }
                }
                strSyncErrorToString = SyncStorageEngine.MESG_CANCELED;
                closeActiveSyncContext(activeSyncContext);
            }
            stopSyncEvent(activeSyncContext.mHistoryRowId, syncOperation, strSyncErrorToString, 0, 0, jElapsedRealtime);
            if (syncResult != null && syncResult.tooManyDeletions) {
                installHandleTooManyDeletesNotification(endPoint.account, endPoint.provider, syncResult.stats.numDeletes, endPoint.userId);
            } else {
                SyncManager.this.mNotificationMgr.cancelAsUser(Integer.toString(endPoint.account.hashCode() ^ endPoint.provider.hashCode()), 18, new UserHandle(endPoint.userId));
            }
            if (syncResult != null && syncResult.fullSyncRequested) {
                SyncManager.this.scheduleSyncOperationH(new SyncOperation(endPoint.account, endPoint.userId, syncOperation.owningUid, syncOperation.owningPackage, syncOperation.reason, syncOperation.syncSource, endPoint.provider, new Bundle(), syncOperation.allowParallelSyncs, syncOperation.syncExemptionFlag));
            }
        }

        private void closeActiveSyncContext(ActiveSyncContext activeSyncContext) {
            activeSyncContext.close();
            SyncManager.this.mActiveSyncContexts.remove(activeSyncContext);
            SyncManager.this.mSyncStorageEngine.removeActiveSync(activeSyncContext.mSyncInfo, activeSyncContext.mSyncOperation.target.userId);
            if (Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", "removing all MESSAGE_MONITOR_SYNC & MESSAGE_SYNC_EXPIRED for " + activeSyncContext.toString());
            }
            SyncManager.this.mSyncHandler.removeMessages(8, activeSyncContext);
            SyncManager.this.mLogger.log("closeActiveSyncContext: ", activeSyncContext);
        }

        private int syncResultToErrorNumber(SyncResult syncResult) {
            if (syncResult.syncAlreadyInProgress) {
                return 1;
            }
            if (syncResult.stats.numAuthExceptions > 0) {
                return 2;
            }
            if (syncResult.stats.numIoExceptions > 0) {
                return 3;
            }
            if (syncResult.stats.numParseExceptions > 0) {
                return 4;
            }
            if (syncResult.stats.numConflictDetectedExceptions > 0) {
                return 5;
            }
            if (syncResult.tooManyDeletions) {
                return 6;
            }
            if (syncResult.tooManyRetries) {
                return 7;
            }
            if (syncResult.databaseError) {
                return 8;
            }
            throw new IllegalStateException("we are not in an error state, " + syncResult);
        }

        private void installHandleTooManyDeletesNotification(Account account, String str, long j, int i) {
            ProviderInfo providerInfoResolveContentProvider;
            if (SyncManager.this.mNotificationMgr != null && (providerInfoResolveContentProvider = SyncManager.this.mContext.getPackageManager().resolveContentProvider(str, 0)) != null) {
                CharSequence charSequenceLoadLabel = providerInfoResolveContentProvider.loadLabel(SyncManager.this.mContext.getPackageManager());
                Intent intent = new Intent(SyncManager.this.mContext, (Class<?>) SyncActivityTooManyDeletes.class);
                intent.putExtra("account", account);
                intent.putExtra("authority", str);
                intent.putExtra("provider", charSequenceLoadLabel.toString());
                intent.putExtra("numDeletes", j);
                if (!isActivityAvailable(intent)) {
                    Log.w("SyncManager", "No activity found to handle too many deletes.");
                    return;
                }
                UserHandle userHandle = new UserHandle(i);
                PendingIntent activityAsUser = PendingIntent.getActivityAsUser(SyncManager.this.mContext, 0, intent, 268435456, null, userHandle);
                CharSequence text = SyncManager.this.mContext.getResources().getText(R.string.app_streaming_blocked_title_for_permission_dialog);
                Context contextForUser = SyncManager.this.getContextForUser(userHandle);
                Notification notificationBuild = new Notification.Builder(contextForUser, SystemNotificationChannels.ACCOUNT).setSmallIcon(R.drawable.pointer_hand_icon).setTicker(SyncManager.this.mContext.getString(R.string.app_streaming_blocked_title_for_fingerprint_dialog)).setWhen(System.currentTimeMillis()).setColor(contextForUser.getColor(R.color.car_colorPrimary)).setContentTitle(contextForUser.getString(R.string.app_streaming_blocked_title_for_microphone_dialog)).setContentText(String.format(text.toString(), charSequenceLoadLabel)).setContentIntent(activityAsUser).build();
                notificationBuild.flags |= 2;
                SyncManager.this.mNotificationMgr.notifyAsUser(Integer.toString(account.hashCode() ^ str.hashCode()), 18, notificationBuild, userHandle);
            }
        }

        private boolean isActivityAvailable(Intent intent) {
            List<ResolveInfo> listQueryIntentActivities = SyncManager.this.mContext.getPackageManager().queryIntentActivities(intent, 0);
            int size = listQueryIntentActivities.size();
            for (int i = 0; i < size; i++) {
                if ((listQueryIntentActivities.get(i).activityInfo.applicationInfo.flags & 1) != 0) {
                    return true;
                }
            }
            return false;
        }

        public long insertStartSyncEvent(SyncOperation syncOperation) {
            long jCurrentTimeMillis = System.currentTimeMillis();
            EventLog.writeEvent(2720, syncOperation.toEventLog(0));
            return SyncManager.this.mSyncStorageEngine.insertStartSyncEvent(syncOperation, jCurrentTimeMillis);
        }

        public void stopSyncEvent(long j, SyncOperation syncOperation, String str, int i, int i2, long j2) {
            EventLog.writeEvent(2720, syncOperation.toEventLog(1));
            SyncManager.this.mSyncStorageEngine.stopSyncEvent(j, j2, str, i2, i);
        }
    }

    private boolean isSyncStillActiveH(ActiveSyncContext activeSyncContext) {
        Iterator<ActiveSyncContext> it = this.mActiveSyncContexts.iterator();
        while (it.hasNext()) {
            if (it.next() == activeSyncContext) {
                return true;
            }
        }
        return false;
    }

    public static boolean syncExtrasEquals(Bundle bundle, Bundle bundle2, boolean z) {
        if (bundle == bundle2) {
            return true;
        }
        if (z && bundle.size() != bundle2.size()) {
            return false;
        }
        Bundle bundle3 = bundle.size() > bundle2.size() ? bundle : bundle2;
        if (bundle.size() > bundle2.size()) {
            bundle = bundle2;
        }
        for (String str : bundle3.keySet()) {
            if (z || !isSyncSetting(str)) {
                if (!bundle.containsKey(str) || !Objects.equals(bundle3.get(str), bundle.get(str))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isSyncSetting(String str) {
        return str.equals("expedited") || str.equals("ignore_settings") || str.equals("ignore_backoff") || str.equals("do_not_retry") || str.equals("force") || str.equals("upload") || str.equals("deletions_override") || str.equals("discard_deletions") || str.equals("expected_upload") || str.equals("expected_download") || str.equals("sync_priority") || str.equals("allow_metered") || str.equals("initialize");
    }

    static class PrintTable {
        private final int mCols;
        private ArrayList<String[]> mTable = Lists.newArrayList();

        PrintTable(int i) {
            this.mCols = i;
        }

        void set(int i, int i2, Object... objArr) {
            int i3;
            if (objArr.length + i2 > this.mCols) {
                throw new IndexOutOfBoundsException("Table only has " + this.mCols + " columns. can't set " + objArr.length + " at column " + i2);
            }
            int size = this.mTable.size();
            while (true) {
                i3 = 0;
                if (size > i) {
                    break;
                }
                String[] strArr = new String[this.mCols];
                this.mTable.add(strArr);
                while (i3 < this.mCols) {
                    strArr[i3] = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
                    i3++;
                }
                size++;
            }
            String[] strArr2 = this.mTable.get(i);
            while (i3 < objArr.length) {
                Object obj = objArr[i3];
                strArr2[i2 + i3] = obj == null ? BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS : obj.toString();
                i3++;
            }
        }

        void writeTo(PrintWriter printWriter) {
            int i;
            String[] strArr = new String[this.mCols];
            int i2 = 0;
            int i3 = 0;
            while (true) {
                if (i2 >= this.mCols) {
                    break;
                }
                Iterator<String[]> it = this.mTable.iterator();
                int i4 = 0;
                while (it.hasNext()) {
                    int length = it.next()[i2].toString().length();
                    if (length > i4) {
                        i4 = length;
                    }
                }
                i3 += i4;
                strArr[i2] = String.format("%%-%ds", Integer.valueOf(i4));
                i2++;
            }
            strArr[this.mCols - 1] = "%s";
            printRow(printWriter, strArr, this.mTable.get(0));
            int i5 = i3 + ((this.mCols - 1) * 2);
            for (int i6 = 0; i6 < i5; i6++) {
                printWriter.print("-");
            }
            printWriter.println();
            int size = this.mTable.size();
            for (i = 1; i < size; i++) {
                printRow(printWriter, strArr, this.mTable.get(i));
            }
        }

        private void printRow(PrintWriter printWriter, String[] strArr, Object[] objArr) {
            int length = objArr.length;
            for (int i = 0; i < length; i++) {
                printWriter.printf(String.format(strArr[i], objArr[i].toString()), new Object[0]);
                printWriter.print("  ");
            }
            printWriter.println();
        }

        public int getNumRows() {
            return this.mTable.size();
        }
    }

    private Context getContextForUser(UserHandle userHandle) {
        try {
            return this.mContext.createPackageContextAsUser(this.mContext.getPackageName(), 0, userHandle);
        } catch (PackageManager.NameNotFoundException e) {
            return this.mContext;
        }
    }

    private void cancelJob(SyncOperation syncOperation, String str) {
        if (syncOperation == null) {
            Slog.wtf("SyncManager", "Null sync operation detected.");
            return;
        }
        if (syncOperation.isPeriodic) {
            this.mLogger.log("Removing periodic sync ", syncOperation, " for ", str);
        }
        getJobScheduler().cancel(syncOperation.jobId);
    }

    private void wtfWithLog(String str) {
        Slog.wtf("SyncManager", str);
        this.mLogger.log("WTF: ", str);
    }

    public void resetTodayStats() {
        this.mSyncStorageEngine.resetTodayStats(true);
    }
}
