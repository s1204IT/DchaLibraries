package com.android.server.content;

import android.R;
import android.accounts.Account;
import android.accounts.AccountAndUser;
import android.app.ActivityManagerNative;
import android.app.AppGlobals;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ISyncAdapter;
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
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.WorkSource;
import android.provider.Settings;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.EventLog;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import com.android.internal.app.IBatteryStats;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.LocalServices;
import com.android.server.accounts.AccountManagerService;
import com.android.server.backup.AccountSyncSettingsBackupHelper;
import com.android.server.content.SyncStorageEngine;
import com.android.server.job.JobSchedulerInternal;
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
import java.util.Set;

public class SyncManager {
    private static final long DEFAULT_MAX_SYNC_RETRY_TIME_IN_SECONDS = 3600;
    private static final int DELAY_RETRY_SYNC_IN_PROGRESS_IN_SECONDS = 10;
    private static final long EXPEDITED_SYNC_DELAY = 500;
    private static final String HANDLE_SYNC_ALARM_WAKE_LOCK = "SyncManagerHandleSyncAlarm";
    private static final int INITIALIZATION_UNBIND_DELAY_MS = 5000;
    private static final int MAX_SYNC_JOB_ID = 110000;
    private static final int MIN_SYNC_JOB_ID = 100000;
    private static final long SYNC_DELAY_ON_CONFLICT = 10000;
    private static final long SYNC_DELAY_ON_LOW_STORAGE = 3600000;
    private static final String SYNC_LOOP_WAKE_LOCK = "SyncLoopWakeLock";
    private static final int SYNC_MONITOR_PROGRESS_THRESHOLD_BYTES = 10;
    private static final long SYNC_MONITOR_WINDOW_LENGTH_MILLIS = 60000;
    private static final String SYNC_WAKE_LOCK_PREFIX = "*sync*/";
    static final String TAG = "SyncManager";
    private final IBatteryStats mBatteryStats;
    private ConnectivityManager mConnManagerDoNotUseDirectly;
    private Context mContext;
    private volatile PowerManager.WakeLock mHandleAlarmWakeLock;
    private JobScheduler mJobScheduler;
    private JobSchedulerInternal mJobSchedulerInternal;
    private final NotificationManager mNotificationMgr;
    private final PowerManager mPowerManager;
    private volatile boolean mProvisioned;
    private final Random mRand;
    protected SyncAdaptersCache mSyncAdapters;
    private final SyncHandler mSyncHandler;
    private SyncJobService mSyncJobService;
    private volatile PowerManager.WakeLock mSyncManagerWakeLock;
    private SyncStorageEngine mSyncStorageEngine;
    private final UserManager mUserManager;
    private static final long INITIAL_SYNC_RETRY_TIME_IN_MS = 30000;
    private static final long LOCAL_SYNC_DELAY = SystemProperties.getLong("sync.local_sync_delay", INITIAL_SYNC_RETRY_TIME_IN_MS);
    private static final AccountAndUser[] INITIAL_ACCOUNTS_ARRAY = new AccountAndUser[0];
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
                SyncManager.this.cancelActiveSync(SyncStorageEngine.EndPoint.USER_ALL_PROVIDER_ALL_ACCOUNTS_ALL, null);
                return;
            }
            if (!"android.intent.action.DEVICE_STORAGE_OK".equals(action)) {
                return;
            }
            if (Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", "Internal storage is ok.");
            }
            SyncManager.this.mStorageIsLow = false;
            SyncManager.this.rescheduleSyncs(SyncStorageEngine.EndPoint.USER_ALL_PROVIDER_ALL_ACCOUNTS_ALL);
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
            SyncManager.this.updateRunningAccounts(SyncStorageEngine.EndPoint.USER_ALL_PROVIDER_ALL_ACCOUNTS_ALL);
        }
    };
    private BroadcastReceiver mConnectivityIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean wasConnected = SyncManager.this.mDataConnectionIsConnected;
            SyncManager.this.mDataConnectionIsConnected = SyncManager.this.readDataConnectionState();
            if (!SyncManager.this.mDataConnectionIsConnected) {
                return;
            }
            if (!wasConnected && Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", "Reconnection detected: clearing all backoffs");
            }
            SyncManager.this.clearAllBackoffs();
        }
    };
    private BroadcastReceiver mShutdownIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.w("SyncManager", "Writing sync state before shutdown...");
            SyncManager.this.getSyncStorageEngine().writeAllState();
        }
    };
    private BroadcastReceiver mUserIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int userId = intent.getIntExtra("android.intent.extra.user_handle", -10000);
            if (userId == -10000) {
                return;
            }
            if ("android.intent.action.USER_REMOVED".equals(action)) {
                SyncManager.this.onUserRemoved(userId);
            } else if ("android.intent.action.USER_UNLOCKED".equals(action)) {
                SyncManager.this.onUserUnlocked(userId);
            } else {
                if (!"android.intent.action.USER_STOPPED".equals(action)) {
                    return;
                }
                SyncManager.this.onUserStopped(userId);
            }
        }
    };
    private volatile boolean mBootCompleted = false;
    private volatile boolean mJobServiceReady = false;

    private boolean isJobIdInUseLockedH(int jobId, List<JobInfo> pendingJobs) {
        for (JobInfo job : pendingJobs) {
            if (job.getId() == jobId) {
                return true;
            }
        }
        for (ActiveSyncContext asc : this.mActiveSyncContexts) {
            if (asc.mSyncOperation.jobId == jobId) {
                return true;
            }
        }
        return false;
    }

    private int getUnusedJobIdH() {
        int newJobId;
        do {
            newJobId = MIN_SYNC_JOB_ID + this.mRand.nextInt(10000);
        } while (isJobIdInUseLockedH(newJobId, this.mJobSchedulerInternal.getSystemScheduledPendingJobs()));
        return newJobId;
    }

    private List<SyncOperation> getAllPendingSyncs() {
        verifyJobScheduler();
        List<JobInfo> pendingJobs = this.mJobSchedulerInternal.getSystemScheduledPendingJobs();
        List<SyncOperation> pendingSyncs = new ArrayList<>(pendingJobs.size());
        for (JobInfo job : pendingJobs) {
            SyncOperation op = SyncOperation.maybeCreateFromJobExtras(job.getExtras());
            if (op != null) {
                pendingSyncs.add(op);
            }
        }
        return pendingSyncs;
    }

    private List<UserInfo> getAllUsers() {
        return this.mUserManager.getUsers();
    }

    private boolean containsAccountAndUser(AccountAndUser[] accounts, Account account, int userId) {
        for (int i = 0; i < accounts.length; i++) {
            if (accounts[i].userId == userId && accounts[i].account.equals(account)) {
                return true;
            }
        }
        return false;
    }

    private void updateRunningAccounts(SyncStorageEngine.EndPoint target) {
        if (Log.isLoggable("SyncManager", 2)) {
            Slog.v("SyncManager", "sending MESSAGE_ACCOUNTS_UPDATED");
        }
        Message m = this.mSyncHandler.obtainMessage(9);
        m.obj = target;
        m.sendToTarget();
    }

    private void doDatabaseCleanup() {
        for (UserInfo user : this.mUserManager.getUsers(true)) {
            if (!user.partial) {
                Account[] accountsForUser = AccountManagerService.getSingleton().getAccounts(user.id, this.mContext.getOpPackageName());
                this.mSyncStorageEngine.doDatabaseCleanup(accountsForUser, user.id);
            }
        }
    }

    private void clearAllBackoffs() {
        this.mSyncStorageEngine.clearAllBackoffsLocked();
        rescheduleSyncs(SyncStorageEngine.EndPoint.USER_ALL_PROVIDER_ALL_ACCOUNTS_ALL);
    }

    private boolean readDataConnectionState() {
        NetworkInfo networkInfo = getConnectivityManager().getActiveNetworkInfo();
        if (networkInfo != null) {
            return networkInfo.isConnected();
        }
        return false;
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
                List<SyncOperation> ops = SyncManager.this.getAllPendingSyncs();
                Set<String> cleanedKeys = new HashSet<>();
                for (SyncOperation opx : ops) {
                    if (!cleanedKeys.contains(opx.key)) {
                        cleanedKeys.add(opx.key);
                        for (SyncOperation opy : ops) {
                            if (opx != opy && opx.key.equals(opy.key)) {
                                SyncManager.this.mJobScheduler.cancel(opy.jobId);
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
        if (Log.isLoggable("SyncManager", 2)) {
            Log.d("SyncManager", "initializing JobScheduler object.");
        }
        this.mJobScheduler = (JobScheduler) this.mContext.getSystemService("jobscheduler");
        this.mJobSchedulerInternal = (JobSchedulerInternal) LocalServices.getService(JobSchedulerInternal.class);
        List<JobInfo> pendingJobs = this.mJobScheduler.getAllPendingJobs();
        for (JobInfo job : pendingJobs) {
            SyncOperation op = SyncOperation.maybeCreateFromJobExtras(job.getExtras());
            if (op != null && !op.isPeriodic) {
                this.mSyncStorageEngine.markPending(op.target, true);
            }
        }
        cleanupJobs();
    }

    private JobScheduler getJobScheduler() {
        verifyJobScheduler();
        return this.mJobScheduler;
    }

    public SyncManager(Context context, boolean factoryTest) {
        this.mContext = context;
        SyncStorageEngine.init(context);
        this.mSyncStorageEngine = SyncStorageEngine.getSingleton();
        this.mSyncStorageEngine.setOnSyncRequestListener(new SyncStorageEngine.OnSyncRequestListener() {
            @Override
            public void onSyncRequest(SyncStorageEngine.EndPoint info, int reason, Bundle extras) {
                SyncManager.this.scheduleSync(info.account, info.userId, reason, info.provider, extras, 0L, 0L, false);
            }
        });
        this.mSyncStorageEngine.setPeriodicSyncAddedListener(new SyncStorageEngine.PeriodicSyncAddedListener() {
            @Override
            public void onPeriodicSyncAdded(SyncStorageEngine.EndPoint target, Bundle extras, long pollFrequency, long flex) {
                SyncManager.this.updateOrAddPeriodicSync(target, pollFrequency, flex, extras);
            }
        });
        this.mSyncStorageEngine.setOnAuthorityRemovedListener(new SyncStorageEngine.OnAuthorityRemovedListener() {
            @Override
            public void onAuthorityRemoved(SyncStorageEngine.EndPoint removedAuthority) {
                SyncManager.this.removeSyncsForAuthority(removedAuthority);
            }
        });
        this.mSyncAdapters = new SyncAdaptersCache(this.mContext);
        this.mSyncHandler = new SyncHandler(BackgroundThread.get().getLooper());
        this.mSyncAdapters.setListener(new RegisteredServicesCacheListener<SyncAdapterType>() {
            public void onServiceChanged(SyncAdapterType type, int userId, boolean removed) {
                if (removed) {
                    return;
                }
                SyncManager.this.scheduleSync(null, -1, -3, type.authority, null, 0L, 0L, false);
            }
        }, this.mSyncHandler);
        this.mRand = new Random(System.currentTimeMillis());
        context.registerReceiver(this.mConnectivityIntentReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
        if (!factoryTest) {
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
        if (!factoryTest) {
            this.mNotificationMgr = (NotificationManager) context.getSystemService("notification");
        } else {
            this.mNotificationMgr = null;
        }
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        this.mUserManager = (UserManager) this.mContext.getSystemService("user");
        this.mBatteryStats = IBatteryStats.Stub.asInterface(ServiceManager.getService("batterystats"));
        this.mHandleAlarmWakeLock = this.mPowerManager.newWakeLock(1, HANDLE_SYNC_ALARM_WAKE_LOCK);
        this.mHandleAlarmWakeLock.setReferenceCounted(false);
        this.mSyncManagerWakeLock = this.mPowerManager.newWakeLock(1, SYNC_LOOP_WAKE_LOCK);
        this.mSyncManagerWakeLock.setReferenceCounted(false);
        this.mProvisioned = isDeviceProvisioned();
        if (!this.mProvisioned) {
            final ContentResolver resolver = context.getContentResolver();
            ContentObserver provisionedObserver = new ContentObserver(null) {
                @Override
                public void onChange(boolean selfChange) {
                    SyncManager.this.mProvisioned |= SyncManager.this.isDeviceProvisioned();
                    if (!SyncManager.this.mProvisioned) {
                        return;
                    }
                    SyncManager.this.mSyncHandler.onDeviceProvisioned();
                    resolver.unregisterContentObserver(this);
                }
            };
            synchronized (this.mSyncHandler) {
                resolver.registerContentObserver(Settings.Global.getUriFor("device_provisioned"), false, provisionedObserver);
                this.mProvisioned |= isDeviceProvisioned();
                if (this.mProvisioned) {
                    resolver.unregisterContentObserver(provisionedObserver);
                }
            }
        }
        if (!factoryTest) {
            this.mContext.registerReceiverAsUser(this.mAccountsUpdatedReceiver, UserHandle.ALL, new IntentFilter("android.accounts.LOGIN_ACCOUNTS_CHANGED"), null, null);
        }
        final Intent startServiceIntent = new Intent(this.mContext, (Class<?>) SyncJobService.class);
        startServiceIntent.putExtra(SyncJobService.EXTRA_MESSENGER, new Messenger(this.mSyncHandler));
        new Handler(this.mContext.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                SyncManager.this.mContext.startService(startServiceIntent);
            }
        });
    }

    private boolean isDeviceProvisioned() {
        ContentResolver resolver = this.mContext.getContentResolver();
        return Settings.Global.getInt(resolver, "device_provisioned", 0) != 0;
    }

    private long jitterize(long minValue, long maxValue) {
        Random random = new Random(SystemClock.elapsedRealtime());
        long spread = maxValue - minValue;
        if (spread > 2147483647L) {
            throw new IllegalArgumentException("the difference between the maxValue and the minValue must be less than 2147483647");
        }
        return ((long) random.nextInt((int) spread)) + minValue;
    }

    public SyncStorageEngine getSyncStorageEngine() {
        return this.mSyncStorageEngine;
    }

    public int getIsSyncable(Account account, int userId, String providerName) {
        RegisteredServicesCache.ServiceInfo<SyncAdapterType> syncAdapterInfo;
        int isSyncable = this.mSyncStorageEngine.getIsSyncable(account, userId, providerName);
        UserInfo userInfo = UserManager.get(this.mContext).getUserInfo(userId);
        if (userInfo == null || !userInfo.isRestricted() || (syncAdapterInfo = this.mSyncAdapters.getServiceInfo(SyncAdapterType.newKey(providerName, account.type), userId)) == null) {
            return isSyncable;
        }
        try {
            PackageInfo pInfo = AppGlobals.getPackageManager().getPackageInfo(syncAdapterInfo.componentName.getPackageName(), 0, userId);
            if (pInfo == null) {
                return isSyncable;
            }
            if (pInfo.restrictedAccountType == null || !pInfo.restrictedAccountType.equals(account.type)) {
                return 0;
            }
            return isSyncable;
        } catch (RemoteException e) {
            return isSyncable;
        }
    }

    private void setAuthorityPendingState(SyncStorageEngine.EndPoint info) {
        List<SyncOperation> ops = getAllPendingSyncs();
        for (SyncOperation op : ops) {
            if (!op.isPeriodic && op.target.matchesSpec(info)) {
                getSyncStorageEngine().markPending(info, true);
                return;
            }
        }
        getSyncStorageEngine().markPending(info, false);
    }

    public void scheduleSync(Account requestedAccount, int userId, int reason, String requestedAuthority, Bundle extras, long beforeRuntimeMillis, long runtimeMillis, boolean onlyThoseWithUnkownSyncableState) {
        AccountAndUser[] accounts;
        RegisteredServicesCache.ServiceInfo<SyncAdapterType> syncAdapterInfo;
        boolean isLoggable = Log.isLoggable("SyncManager", 2);
        if (extras == null) {
            extras = new Bundle();
        }
        if (isLoggable) {
            Log.d("SyncManager", "one-time sync for: " + requestedAccount + " " + extras.toString() + " " + requestedAuthority);
        }
        if (requestedAccount == null || userId == -1) {
            accounts = this.mRunningAccounts;
            if (accounts.length == 0) {
                if (isLoggable) {
                    Slog.v("SyncManager", "scheduleSync: no accounts configured, dropping");
                    return;
                }
                return;
            }
        } else {
            accounts = new AccountAndUser[]{new AccountAndUser(requestedAccount, userId)};
        }
        boolean uploadOnly = extras.getBoolean("upload", false);
        boolean manualSync = extras.getBoolean("force", false);
        if (manualSync) {
            extras.putBoolean("ignore_backoff", true);
            extras.putBoolean("ignore_settings", true);
        }
        boolean ignoreSettings = extras.getBoolean("ignore_settings", false);
        int source = uploadOnly ? 1 : manualSync ? 3 : requestedAuthority == null ? 2 : 0;
        int i = 0;
        int length = accounts.length;
        while (true) {
            int i2 = i;
            if (i2 >= length) {
                return;
            }
            AccountAndUser account = accounts[i2];
            if (userId < 0 || account.userId < 0 || userId == account.userId) {
                HashSet<String> syncableAuthorities = new HashSet<>();
                for (RegisteredServicesCache.ServiceInfo<SyncAdapterType> syncAdapter : this.mSyncAdapters.getAllServices(account.userId)) {
                    syncableAuthorities.add(((SyncAdapterType) syncAdapter.type).authority);
                }
                if (requestedAuthority != null) {
                    boolean hasSyncAdapter = syncableAuthorities.contains(requestedAuthority);
                    syncableAuthorities.clear();
                    if (hasSyncAdapter) {
                        syncableAuthorities.add(requestedAuthority);
                    }
                }
                for (String authority : syncableAuthorities) {
                    int isSyncable = getIsSyncable(account.account, account.userId, authority);
                    if (isSyncable != 0 && (syncAdapterInfo = this.mSyncAdapters.getServiceInfo(SyncAdapterType.newKey(authority, account.account.type), account.userId)) != null) {
                        int owningUid = syncAdapterInfo.uid;
                        String owningPackage = syncAdapterInfo.componentName.getPackageName();
                        if (ActivityManagerNative.getDefault().getAppStartMode(owningUid, owningPackage) == 2) {
                            Slog.w("SyncManager", "Not scheduling job " + syncAdapterInfo.uid + ":" + syncAdapterInfo.componentName + " -- package not allowed to start");
                        } else {
                            boolean allowParallelSyncs = ((SyncAdapterType) syncAdapterInfo.type).allowParallelSyncs();
                            boolean isAlwaysSyncable = ((SyncAdapterType) syncAdapterInfo.type).isAlwaysSyncable();
                            if (isSyncable < 0 && isAlwaysSyncable) {
                                this.mSyncStorageEngine.setIsSyncable(account.account, account.userId, authority, 1);
                                isSyncable = 1;
                            }
                            if (!onlyThoseWithUnkownSyncableState || isSyncable < 0) {
                                if (((SyncAdapterType) syncAdapterInfo.type).supportsUploading() || !uploadOnly) {
                                    boolean syncAllowed = (isSyncable < 0 || ignoreSettings) ? true : this.mSyncStorageEngine.getMasterSyncAutomatically(account.userId) ? this.mSyncStorageEngine.getSyncAutomatically(account.account, account.userId, authority) : false;
                                    if (syncAllowed) {
                                        SyncStorageEngine.EndPoint info = new SyncStorageEngine.EndPoint(account.account, authority, account.userId);
                                        long delayUntil = this.mSyncStorageEngine.getDelayUntilTime(info);
                                        if (isSyncable < 0) {
                                            Bundle newExtras = new Bundle();
                                            newExtras.putBoolean("initialize", true);
                                            if (isLoggable) {
                                                Slog.v("SyncManager", "schedule initialisation Sync:, delay until " + delayUntil + ", run by 0, flexMillis 0, source " + source + ", account " + account + ", authority " + authority + ", extras " + newExtras);
                                            }
                                            postScheduleSyncMessage(new SyncOperation(account.account, account.userId, owningUid, owningPackage, reason, source, authority, newExtras, allowParallelSyncs));
                                        }
                                        if (!onlyThoseWithUnkownSyncableState) {
                                            if (isLoggable) {
                                                Slog.v("SyncManager", "scheduleSync: delay until " + delayUntil + " run by " + runtimeMillis + " flexMillis " + beforeRuntimeMillis + ", source " + source + ", account " + account + ", authority " + authority + ", extras " + extras);
                                            }
                                            postScheduleSyncMessage(new SyncOperation(account.account, account.userId, owningUid, owningPackage, reason, source, authority, extras, allowParallelSyncs));
                                        }
                                    } else if (isLoggable) {
                                        Log.d("SyncManager", "scheduleSync: sync of " + account + ", " + authority + " is not allowed, dropping request");
                                    }
                                }
                            }
                        }
                    }
                }
            }
            i = i2 + 1;
        }
    }

    private void removeSyncsForAuthority(SyncStorageEngine.EndPoint info) {
        verifyJobScheduler();
        List<SyncOperation> ops = getAllPendingSyncs();
        for (SyncOperation op : ops) {
            if (op.target.matchesSpec(info)) {
                getJobScheduler().cancel(op.jobId);
            }
        }
    }

    public void removePeriodicSync(SyncStorageEngine.EndPoint target, Bundle extras) {
        Message m = this.mSyncHandler.obtainMessage(14, target);
        m.setData(extras);
        m.sendToTarget();
    }

    public void updateOrAddPeriodicSync(SyncStorageEngine.EndPoint target, long pollFrequency, long flex, Bundle extras) {
        UpdatePeriodicSyncMessagePayload payload = new UpdatePeriodicSyncMessagePayload(target, pollFrequency, flex, extras);
        this.mSyncHandler.obtainMessage(13, payload).sendToTarget();
    }

    public List<PeriodicSync> getPeriodicSyncs(SyncStorageEngine.EndPoint target) {
        List<SyncOperation> ops = getAllPendingSyncs();
        List<PeriodicSync> periodicSyncs = new ArrayList<>();
        for (SyncOperation op : ops) {
            if (op.isPeriodic && op.target.matchesSpec(target)) {
                periodicSyncs.add(new PeriodicSync(op.target.account, op.target.provider, op.extras, op.periodMillis / 1000, op.flexMillis / 1000));
            }
        }
        return periodicSyncs;
    }

    public void scheduleLocalSync(Account account, int userId, int reason, String authority) {
        Bundle extras = new Bundle();
        extras.putBoolean("upload", true);
        scheduleSync(account, userId, reason, authority, extras, LOCAL_SYNC_DELAY, 2 * LOCAL_SYNC_DELAY, false);
    }

    public SyncAdapterType[] getSyncAdapterTypes(int userId) {
        Collection<RegisteredServicesCache.ServiceInfo<SyncAdapterType>> serviceInfos = this.mSyncAdapters.getAllServices(userId);
        SyncAdapterType[] types = new SyncAdapterType[serviceInfos.size()];
        int i = 0;
        for (RegisteredServicesCache.ServiceInfo<SyncAdapterType> serviceInfo : serviceInfos) {
            types[i] = (SyncAdapterType) serviceInfo.type;
            i++;
        }
        return types;
    }

    public String[] getSyncAdapterPackagesForAuthorityAsUser(String authority, int userId) {
        return this.mSyncAdapters.getSyncAdapterPackagesForAuthority(authority, userId);
    }

    private void sendSyncFinishedOrCanceledMessage(ActiveSyncContext syncContext, SyncResult syncResult) {
        if (Log.isLoggable("SyncManager", 2)) {
            Slog.v("SyncManager", "sending MESSAGE_SYNC_FINISHED");
        }
        Message msg = this.mSyncHandler.obtainMessage();
        msg.what = 1;
        msg.obj = new SyncFinishedOrCancelledMessagePayload(syncContext, syncResult);
        this.mSyncHandler.sendMessage(msg);
    }

    private void sendCancelSyncsMessage(SyncStorageEngine.EndPoint info, Bundle extras) {
        if (Log.isLoggable("SyncManager", 2)) {
            Slog.v("SyncManager", "sending MESSAGE_CANCEL");
        }
        Message msg = this.mSyncHandler.obtainMessage();
        msg.what = 6;
        msg.setData(extras);
        msg.obj = info;
        this.mSyncHandler.sendMessage(msg);
    }

    private void postMonitorSyncProgressMessage(ActiveSyncContext activeSyncContext) {
        if (Log.isLoggable("SyncManager", 2)) {
            Slog.v("SyncManager", "posting MESSAGE_SYNC_MONITOR in 60s");
        }
        activeSyncContext.mBytesTransferredAtLastPoll = getTotalBytesTransferredByUid(activeSyncContext.mSyncAdapterUid);
        activeSyncContext.mLastPolledTimeElapsed = SystemClock.elapsedRealtime();
        Message monitorMessage = this.mSyncHandler.obtainMessage(8, activeSyncContext);
        this.mSyncHandler.sendMessageDelayed(monitorMessage, SYNC_MONITOR_WINDOW_LENGTH_MILLIS);
    }

    private void postScheduleSyncMessage(SyncOperation syncOperation) {
        this.mSyncHandler.obtainMessage(12, syncOperation).sendToTarget();
    }

    private long getTotalBytesTransferredByUid(int uid) {
        return TrafficStats.getUidRxBytes(uid) + TrafficStats.getUidTxBytes(uid);
    }

    private class SyncFinishedOrCancelledMessagePayload {
        public final ActiveSyncContext activeSyncContext;
        public final SyncResult syncResult;

        SyncFinishedOrCancelledMessagePayload(ActiveSyncContext syncContext, SyncResult syncResult) {
            this.activeSyncContext = syncContext;
            this.syncResult = syncResult;
        }
    }

    private class UpdatePeriodicSyncMessagePayload {
        public final Bundle extras;
        public final long flex;
        public final long pollFrequency;
        public final SyncStorageEngine.EndPoint target;

        UpdatePeriodicSyncMessagePayload(SyncStorageEngine.EndPoint target, long pollFrequency, long flex, Bundle extras) {
            this.target = target;
            this.pollFrequency = pollFrequency;
            this.flex = flex;
            this.extras = extras;
        }
    }

    private void clearBackoffSetting(SyncStorageEngine.EndPoint target) {
        Pair<Long, Long> backoff = this.mSyncStorageEngine.getBackoff(target);
        if (backoff != null && ((Long) backoff.first).longValue() == -1 && ((Long) backoff.second).longValue() == -1) {
            return;
        }
        if (Log.isLoggable("SyncManager", 2)) {
            Slog.v("SyncManager", "Clearing backoffs for " + target);
        }
        this.mSyncStorageEngine.setBackoff(target, -1L, -1L);
        rescheduleSyncs(target);
    }

    private void increaseBackoffSetting(SyncStorageEngine.EndPoint target) {
        long now = SystemClock.elapsedRealtime();
        Pair<Long, Long> previousSettings = this.mSyncStorageEngine.getBackoff(target);
        long newDelayInMs = -1;
        if (previousSettings != null) {
            if (now < ((Long) previousSettings.first).longValue()) {
                if (Log.isLoggable("SyncManager", 2)) {
                    Slog.v("SyncManager", "Still in backoff, do not increase it. Remaining: " + ((((Long) previousSettings.first).longValue() - now) / 1000) + " seconds.");
                    return;
                }
                return;
            }
            newDelayInMs = ((Long) previousSettings.second).longValue() * 2;
        }
        if (newDelayInMs <= 0) {
            newDelayInMs = jitterize(INITIAL_SYNC_RETRY_TIME_IN_MS, 33000L);
        }
        long maxSyncRetryTimeInSeconds = Settings.Global.getLong(this.mContext.getContentResolver(), "sync_max_retry_delay_in_seconds", DEFAULT_MAX_SYNC_RETRY_TIME_IN_SECONDS);
        if (newDelayInMs > 1000 * maxSyncRetryTimeInSeconds) {
            newDelayInMs = maxSyncRetryTimeInSeconds * 1000;
        }
        long backoff = now + newDelayInMs;
        if (Log.isLoggable("SyncManager", 2)) {
            Slog.v("SyncManager", "Backoff until: " + backoff + ", delayTime: " + newDelayInMs);
        }
        this.mSyncStorageEngine.setBackoff(target, backoff, newDelayInMs);
        rescheduleSyncs(target);
    }

    private void rescheduleSyncs(SyncStorageEngine.EndPoint target) {
        List<SyncOperation> ops = getAllPendingSyncs();
        int count = 0;
        for (SyncOperation op : ops) {
            if (!op.isPeriodic && op.target.matchesSpec(target)) {
                count++;
                getJobScheduler().cancel(op.jobId);
                postScheduleSyncMessage(op);
            }
        }
        if (!Log.isLoggable("SyncManager", 2)) {
            return;
        }
        Slog.v("SyncManager", "Rescheduled " + count + " syncs for " + target);
    }

    private void setDelayUntilTime(SyncStorageEngine.EndPoint target, long delayUntilSeconds) {
        long newDelayUntilTime;
        long delayUntil = delayUntilSeconds * 1000;
        long absoluteNow = System.currentTimeMillis();
        if (delayUntil > absoluteNow) {
            newDelayUntilTime = SystemClock.elapsedRealtime() + (delayUntil - absoluteNow);
        } else {
            newDelayUntilTime = 0;
        }
        this.mSyncStorageEngine.setDelayUntilTime(target, newDelayUntilTime);
        if (Log.isLoggable("SyncManager", 2)) {
            Slog.v("SyncManager", "Delay Until time set to " + newDelayUntilTime + " for " + target);
        }
        rescheduleSyncs(target);
    }

    private boolean isAdapterDelayed(SyncStorageEngine.EndPoint target) {
        long now = SystemClock.elapsedRealtime();
        Pair<Long, Long> backoff = this.mSyncStorageEngine.getBackoff(target);
        return !(backoff == null || ((Long) backoff.first).longValue() == -1 || ((Long) backoff.first).longValue() <= now) || this.mSyncStorageEngine.getDelayUntilTime(target) > now;
    }

    public void cancelActiveSync(SyncStorageEngine.EndPoint info, Bundle extras) {
        sendCancelSyncsMessage(info, extras);
    }

    private void scheduleSyncOperationH(SyncOperation syncOperation) {
        scheduleSyncOperationH(syncOperation, 0L);
    }

    private void scheduleSyncOperationH(SyncOperation syncOperation, long minDelay) {
        boolean isLoggable = Log.isLoggable("SyncManager", 2);
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
            long now = SystemClock.elapsedRealtime();
            long backoffDelay = ((Long) backoff.first).longValue() == -1 ? 0L : ((Long) backoff.first).longValue() - now;
            long delayUntil = this.mSyncStorageEngine.getDelayUntilTime(syncOperation.target);
            long delayUntilDelay = delayUntil > now ? delayUntil - now : 0L;
            if (isLoggable) {
                Slog.v("SyncManager", "backoff delay:" + backoffDelay + " delayUntil delay:" + delayUntilDelay);
            }
            minDelay = Math.max(minDelay, Math.max(backoffDelay, delayUntilDelay));
        }
        if (minDelay < 0) {
            minDelay = 0;
        }
        if (!syncOperation.isPeriodic) {
            for (ActiveSyncContext asc : this.mActiveSyncContexts) {
                if (asc.mSyncOperation.key.equals(syncOperation.key)) {
                    if (isLoggable) {
                        Log.v("SyncManager", "Duplicate sync is already running. Not scheduling " + syncOperation);
                        return;
                    }
                    return;
                }
            }
            int duplicatesCount = 0;
            syncOperation.expectedRuntime = SystemClock.elapsedRealtime() + minDelay;
            List<SyncOperation> pending = getAllPendingSyncs();
            SyncOperation opWithLeastExpectedRuntime = syncOperation;
            for (SyncOperation op : pending) {
                if (!op.isPeriodic && op.key.equals(syncOperation.key)) {
                    if (opWithLeastExpectedRuntime.expectedRuntime > op.expectedRuntime) {
                        opWithLeastExpectedRuntime = op;
                    }
                    duplicatesCount++;
                }
            }
            if (duplicatesCount > 1) {
                Slog.e("SyncManager", "FATAL ERROR! File a bug if you see this.");
            }
            for (SyncOperation op2 : pending) {
                if (!op2.isPeriodic && op2.key.equals(syncOperation.key) && op2 != opWithLeastExpectedRuntime) {
                    if (isLoggable) {
                        Slog.v("SyncManager", "Cancelling duplicate sync " + op2);
                    }
                    getJobScheduler().cancel(op2.jobId);
                }
            }
            if (opWithLeastExpectedRuntime != syncOperation) {
                if (isLoggable) {
                    Slog.v("SyncManager", "Not scheduling because a duplicate exists.");
                    return;
                }
                return;
            }
        }
        if (syncOperation.jobId == -1) {
            syncOperation.jobId = getUnusedJobIdH();
        }
        if (isLoggable) {
            Slog.v("SyncManager", "scheduling sync operation " + syncOperation.toString());
        }
        int priority = syncOperation.findPriority();
        int networkType = syncOperation.isNotAllowedOnMetered() ? 2 : 1;
        JobInfo.Builder b = new JobInfo.Builder(syncOperation.jobId, new ComponentName(this.mContext, (Class<?>) SyncJobService.class)).setExtras(syncOperation.toJobInfoExtras()).setRequiredNetworkType(networkType).setPersisted(true).setPriority(priority);
        if (syncOperation.isPeriodic) {
            b.setPeriodic(syncOperation.periodMillis, syncOperation.flexMillis);
        } else {
            if (minDelay > 0) {
                b.setMinimumLatency(minDelay);
            }
            getSyncStorageEngine().markPending(syncOperation.target, true);
        }
        if (syncOperation.extras.getBoolean("require_charging")) {
            b.setRequiresCharging(true);
        }
        getJobScheduler().scheduleAsPackage(b.build(), syncOperation.owningPackage, syncOperation.target.userId, syncOperation.wakeLockName());
    }

    public void clearScheduledSyncOperations(SyncStorageEngine.EndPoint info) {
        List<SyncOperation> ops = getAllPendingSyncs();
        for (SyncOperation op : ops) {
            if (!op.isPeriodic && op.target.matchesSpec(info)) {
                getJobScheduler().cancel(op.jobId);
                getSyncStorageEngine().markPending(op.target, false);
            }
        }
        this.mSyncStorageEngine.setBackoff(info, -1L, -1L);
    }

    public void cancelScheduledSyncOperation(SyncStorageEngine.EndPoint info, Bundle extras) {
        List<SyncOperation> ops = getAllPendingSyncs();
        for (SyncOperation op : ops) {
            if (!op.isPeriodic && op.target.matchesSpec(info) && syncExtrasEquals(extras, op.extras, false)) {
                getJobScheduler().cancel(op.jobId);
            }
        }
        setAuthorityPendingState(info);
        if (this.mSyncStorageEngine.isSyncPending(info)) {
            return;
        }
        this.mSyncStorageEngine.setBackoff(info, -1L, -1L);
    }

    private void maybeRescheduleSync(SyncResult syncResult, SyncOperation operation) {
        boolean isLoggable = Log.isLoggable("SyncManager", 3);
        if (isLoggable) {
            Log.d("SyncManager", "encountered error(s) during the sync: " + syncResult + ", " + operation);
        }
        if (operation.extras.getBoolean("ignore_backoff", false)) {
            operation.extras.remove("ignore_backoff");
        }
        if (operation.extras.getBoolean("do_not_retry", false) && !syncResult.syncAlreadyInProgress) {
            if (!isLoggable) {
                return;
            }
            Log.d("SyncManager", "not retrying sync operation because SYNC_EXTRAS_DO_NOT_RETRY was specified " + operation);
            return;
        }
        if (operation.extras.getBoolean("upload", false) && !syncResult.syncAlreadyInProgress) {
            operation.extras.remove("upload");
            if (isLoggable) {
                Log.d("SyncManager", "retrying sync operation as a two-way sync because an upload-only sync encountered an error: " + operation);
            }
            scheduleSyncOperationH(operation);
            return;
        }
        if (syncResult.tooManyRetries) {
            if (!isLoggable) {
                return;
            }
            Log.d("SyncManager", "not retrying sync operation because it retried too many times: " + operation);
            return;
        }
        if (syncResult.madeSomeProgress()) {
            if (isLoggable) {
                Log.d("SyncManager", "retrying sync operation because even though it had an error it achieved some success");
            }
            scheduleSyncOperationH(operation);
        } else if (syncResult.syncAlreadyInProgress) {
            if (isLoggable) {
                Log.d("SyncManager", "retrying sync operation that failed because there was already a sync in progress: " + operation);
            }
            scheduleSyncOperationH(operation, 10000L);
        } else {
            if (syncResult.hasSoftError()) {
                if (isLoggable) {
                    Log.d("SyncManager", "retrying sync operation because it encountered a soft error: " + operation);
                }
                scheduleSyncOperationH(operation);
                return;
            }
            Log.d("SyncManager", "not retrying sync operation because the error is a hard error: " + operation);
        }
    }

    private void onUserUnlocked(int userId) {
        AccountManagerService.getSingleton().validateAccounts(userId);
        this.mSyncAdapters.invalidateCache(userId);
        SyncStorageEngine.EndPoint target = new SyncStorageEngine.EndPoint(null, null, userId);
        updateRunningAccounts(target);
        Account[] accounts = AccountManagerService.getSingleton().getAccounts(userId, this.mContext.getOpPackageName());
        int i = 0;
        int length = accounts.length;
        while (true) {
            int i2 = i;
            if (i2 >= length) {
                return;
            }
            Account account = accounts[i2];
            scheduleSync(account, userId, -8, null, null, 0L, 0L, true);
            i = i2 + 1;
        }
    }

    private void onUserStopped(int userId) {
        updateRunningAccounts(null);
        cancelActiveSync(new SyncStorageEngine.EndPoint(null, null, userId), null);
    }

    private void onUserRemoved(int userId) {
        updateRunningAccounts(null);
        this.mSyncStorageEngine.doDatabaseCleanup(new Account[0], userId);
        List<SyncOperation> ops = getAllPendingSyncs();
        for (SyncOperation op : ops) {
            if (op.target.userId == userId) {
                getJobScheduler().cancel(op.jobId);
            }
        }
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

        public ActiveSyncContext(SyncOperation syncOperation, long historyRowId, int syncAdapterUid) {
            this.mSyncAdapterUid = syncAdapterUid;
            this.mSyncOperation = syncOperation;
            this.mHistoryRowId = historyRowId;
            this.mSyncWakeLock = SyncManager.this.mSyncHandler.getSyncWakeLock(this.mSyncOperation);
            this.mSyncWakeLock.setWorkSource(new WorkSource(syncAdapterUid));
            this.mSyncWakeLock.acquire();
        }

        public void sendHeartbeat() {
        }

        public void onFinished(SyncResult result) {
            if (Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", "onFinished: " + this);
            }
            SyncManager.this.sendSyncFinishedOrCanceledMessage(this, result);
        }

        public void toString(StringBuilder sb) {
            sb.append("startTime ").append(this.mStartTime).append(", mTimeoutStartTime ").append(this.mTimeoutStartTime).append(", mHistoryRowId ").append(this.mHistoryRowId).append(", syncOperation ").append(this.mSyncOperation);
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Message msg = SyncManager.this.mSyncHandler.obtainMessage();
            msg.what = 4;
            msg.obj = SyncManager.this.new ServiceConnectionData(this, service);
            SyncManager.this.mSyncHandler.sendMessage(msg);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Message msg = SyncManager.this.mSyncHandler.obtainMessage();
            msg.what = 5;
            msg.obj = SyncManager.this.new ServiceConnectionData(this, null);
            SyncManager.this.mSyncHandler.sendMessage(msg);
        }

        boolean bindToSyncAdapter(ComponentName serviceComponent, int userId) {
            if (Log.isLoggable("SyncManager", 2)) {
                Log.d("SyncManager", "bindToSyncAdapter: " + serviceComponent + ", connection " + this);
            }
            Intent intent = new Intent();
            intent.setAction("android.content.SyncAdapter");
            intent.setComponent(serviceComponent);
            intent.putExtra("android.intent.extra.client_label", R.string.foreground_service_app_in_background);
            if (BenesseExtension.getDchaState() == 0) {
                intent.putExtra("android.intent.extra.client_intent", PendingIntent.getActivityAsUser(SyncManager.this.mContext, 0, new Intent("android.settings.SYNC_SETTINGS"), 0, null, new UserHandle(userId)));
            }
            this.mBound = true;
            boolean bindResult = SyncManager.this.mContext.bindServiceAsUser(intent, this, 21, new UserHandle(this.mSyncOperation.target.userId));
            if (!bindResult) {
                this.mBound = false;
            } else {
                try {
                    this.mEventName = this.mSyncOperation.wakeLockName();
                    SyncManager.this.mBatteryStats.noteSyncStart(this.mEventName, this.mSyncAdapterUid);
                } catch (RemoteException e) {
                }
            }
            return bindResult;
        }

        protected void close() {
            if (Log.isLoggable("SyncManager", 2)) {
                Log.d("SyncManager", "unBindFromSyncAdapter: connection " + this);
            }
            if (this.mBound) {
                this.mBound = false;
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

    protected void dump(FileDescriptor fd, PrintWriter pw) {
        IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "  ");
        dumpPendingSyncs(pw);
        dumpPeriodicSyncs(pw);
        dumpSyncState(ipw);
        dumpSyncHistory(ipw);
        dumpSyncAdapters(ipw);
    }

    static String formatTime(long time) {
        Time tobj = new Time();
        tobj.set(time);
        return tobj.format("%Y-%m-%d %H:%M:%S");
    }

    protected void dumpPendingSyncs(PrintWriter pw) {
        pw.println("Pending Syncs:");
        List<SyncOperation> pendingSyncs = getAllPendingSyncs();
        int count = 0;
        for (SyncOperation op : pendingSyncs) {
            if (!op.isPeriodic) {
                pw.println(op.dump(null, false));
                count++;
            }
        }
        pw.println("Total: " + count);
        pw.println();
    }

    protected void dumpPeriodicSyncs(PrintWriter pw) {
        pw.println("Periodic Syncs:");
        List<SyncOperation> pendingSyncs = getAllPendingSyncs();
        int count = 0;
        for (SyncOperation op : pendingSyncs) {
            if (op.isPeriodic) {
                pw.println(op.dump(null, false));
                count++;
            }
        }
        pw.println("Total: " + count);
        pw.println();
    }

    protected void dumpSyncState(PrintWriter pw) {
        pw.print("data connected: ");
        pw.println(this.mDataConnectionIsConnected);
        pw.print("auto sync: ");
        List<UserInfo> users = getAllUsers();
        if (users != null) {
            for (UserInfo user : users) {
                pw.print("u" + user.id + "=" + this.mSyncStorageEngine.getMasterSyncAutomatically(user.id) + " ");
            }
            pw.println();
        }
        pw.print("memory low: ");
        pw.println(this.mStorageIsLow);
        pw.print("device idle: ");
        pw.println(this.mDeviceIsIdle);
        pw.print("reported active: ");
        pw.println(this.mReportedSyncActive);
        AccountAndUser[] accounts = AccountManagerService.getSingleton().getAllAccounts();
        pw.print("accounts: ");
        if (accounts != INITIAL_ACCOUNTS_ARRAY) {
            pw.println(accounts.length);
        } else {
            pw.println("not known yet");
        }
        long now = SystemClock.elapsedRealtime();
        pw.print("now: ");
        pw.print(now);
        pw.println(" (" + formatTime(System.currentTimeMillis()) + ")");
        pw.println(" (HH:MM:SS)");
        pw.print("uptime: ");
        pw.print(DateUtils.formatElapsedTime(now / 1000));
        pw.println(" (HH:MM:SS)");
        pw.print("time spent syncing: ");
        pw.print(DateUtils.formatElapsedTime(this.mSyncHandler.mSyncTimeTracker.timeSpentSyncing() / 1000));
        pw.print(" (HH:MM:SS), sync ");
        pw.print(this.mSyncHandler.mSyncTimeTracker.mLastWasSyncing ? "" : "not ");
        pw.println("in progress");
        pw.println();
        pw.println("Active Syncs: " + this.mActiveSyncContexts.size());
        PackageManager pm = this.mContext.getPackageManager();
        for (ActiveSyncContext activeSyncContext : this.mActiveSyncContexts) {
            long durationInSeconds = (now - activeSyncContext.mStartTime) / 1000;
            pw.print("  ");
            pw.print(DateUtils.formatElapsedTime(durationInSeconds));
            pw.print(" - ");
            pw.print(activeSyncContext.mSyncOperation.dump(pm, false));
            pw.println();
        }
        pw.println();
        pw.println("Sync Status");
        int i = 0;
        int length = accounts.length;
        while (true) {
            int i2 = i;
            if (i2 >= length) {
                return;
            }
            AccountAndUser account = accounts[i2];
            pw.printf("Account %s u%d %s\n", account.account.name, Integer.valueOf(account.userId), account.account.type);
            pw.println("=======================================================================");
            PrintTable table = new PrintTable(12);
            table.set(0, 0, "Authority", "Syncable", "Enabled", "Delay", "Loc", "Poll", "Per", "Serv", "User", "Tot", "Time", "Last Sync");
            List<RegisteredServicesCache.ServiceInfo<SyncAdapterType>> sorted = Lists.newArrayList();
            sorted.addAll(this.mSyncAdapters.getAllServices(account.userId));
            Collections.sort(sorted, new Comparator<RegisteredServicesCache.ServiceInfo<SyncAdapterType>>() {
                @Override
                public int compare(RegisteredServicesCache.ServiceInfo<SyncAdapterType> lhs, RegisteredServicesCache.ServiceInfo<SyncAdapterType> rhs) {
                    return ((SyncAdapterType) lhs.type).authority.compareTo(((SyncAdapterType) rhs.type).authority);
                }
            });
            for (RegisteredServicesCache.ServiceInfo<SyncAdapterType> syncAdapterType : sorted) {
                if (((SyncAdapterType) syncAdapterType.type).accountType.equals(account.account.type)) {
                    int row = table.getNumRows();
                    Pair<SyncStorageEngine.AuthorityInfo, SyncStatusInfo> syncAuthoritySyncStatus = this.mSyncStorageEngine.getCopyOfAuthorityWithSyncStatus(new SyncStorageEngine.EndPoint(account.account, ((SyncAdapterType) syncAdapterType.type).authority, account.userId));
                    SyncStorageEngine.AuthorityInfo settings = (SyncStorageEngine.AuthorityInfo) syncAuthoritySyncStatus.first;
                    SyncStatusInfo status = (SyncStatusInfo) syncAuthoritySyncStatus.second;
                    String authority = settings.target.provider;
                    if (authority.length() > 50) {
                        authority = authority.substring(authority.length() - 50);
                    }
                    table.set(row, 0, authority, Integer.valueOf(settings.syncable), Boolean.valueOf(settings.enabled));
                    table.set(row, 4, Integer.valueOf(status.numSourceLocal), Integer.valueOf(status.numSourcePoll), Integer.valueOf(status.numSourcePeriodic), Integer.valueOf(status.numSourceServer), Integer.valueOf(status.numSourceUser), Integer.valueOf(status.numSyncs), DateUtils.formatElapsedTime(status.totalElapsedTime / 1000));
                    int row1 = row;
                    if (settings.delayUntil > now) {
                        row1 = row + 1;
                        table.set(row, 12, "D: " + ((settings.delayUntil - now) / 1000));
                        if (settings.backoffTime > now) {
                            int row12 = row1 + 1;
                            table.set(row1, 12, "B: " + ((settings.backoffTime - now) / 1000));
                            row1 = row12 + 1;
                            table.set(row12, 12, Long.valueOf(settings.backoffDelay / 1000));
                        }
                    }
                    if (status.lastSuccessTime != 0) {
                        int row13 = row1 + 1;
                        table.set(row1, 11, SyncStorageEngine.SOURCES[status.lastSuccessSource] + " SUCCESS");
                        row1 = row13 + 1;
                        table.set(row13, 11, formatTime(status.lastSuccessTime));
                    }
                    if (status.lastFailureTime != 0) {
                        int row14 = row1 + 1;
                        table.set(row1, 11, SyncStorageEngine.SOURCES[status.lastFailureSource] + " FAILURE");
                        int row15 = row14 + 1;
                        table.set(row14, 11, formatTime(status.lastFailureTime));
                        int i3 = row15 + 1;
                        table.set(row15, 11, status.lastFailureMesg);
                    }
                }
            }
            table.writeTo(pw);
            i = i2 + 1;
        }
    }

    private void dumpTimeSec(PrintWriter pw, long time) {
        pw.print(time / 1000);
        pw.print('.');
        pw.print((time / 100) % 10);
        pw.print('s');
    }

    private void dumpDayStatistic(PrintWriter pw, SyncStorageEngine.DayStats ds) {
        pw.print("Success (");
        pw.print(ds.successCount);
        if (ds.successCount > 0) {
            pw.print(" for ");
            dumpTimeSec(pw, ds.successTime);
            pw.print(" avg=");
            dumpTimeSec(pw, ds.successTime / ((long) ds.successCount));
        }
        pw.print(") Failure (");
        pw.print(ds.failureCount);
        if (ds.failureCount > 0) {
            pw.print(" for ");
            dumpTimeSec(pw, ds.failureTime);
            pw.print(" avg=");
            dumpTimeSec(pw, ds.failureTime / ((long) ds.failureCount));
        }
        pw.println(")");
    }

    protected void dumpSyncHistory(PrintWriter pw) {
        dumpRecentHistory(pw);
        dumpDayStatistics(pw);
    }

    private void dumpRecentHistory(PrintWriter pw) {
        String authorityName;
        String accountKey;
        String authorityName2;
        String accountKey2;
        String diffString;
        String authorityName3;
        String accountKey3;
        ArrayList<SyncStorageEngine.SyncHistoryItem> items = this.mSyncStorageEngine.getSyncHistory();
        if (items == null || items.size() <= 0) {
            return;
        }
        Map<String, AuthoritySyncStats> authorityMap = Maps.newHashMap();
        long totalElapsedTime = 0;
        long totalTimes = 0;
        int N = items.size();
        int maxAuthority = 0;
        int maxAccount = 0;
        for (SyncStorageEngine.SyncHistoryItem item : items) {
            SyncStorageEngine.AuthorityInfo authorityInfo = this.mSyncStorageEngine.getAuthority(item.authorityId);
            if (authorityInfo != null) {
                authorityName3 = authorityInfo.target.provider;
                accountKey3 = authorityInfo.target.account.name + "/" + authorityInfo.target.account.type + " u" + authorityInfo.target.userId;
            } else {
                authorityName3 = "Unknown";
                accountKey3 = "Unknown";
            }
            int length = authorityName3.length();
            if (length > maxAuthority) {
                maxAuthority = length;
            }
            int length2 = accountKey3.length();
            if (length2 > maxAccount) {
                maxAccount = length2;
            }
            long elapsedTime = item.elapsedTime;
            totalElapsedTime += elapsedTime;
            totalTimes++;
            AuthoritySyncStats authoritySyncStats = authorityMap.get(authorityName3);
            if (authoritySyncStats == null) {
                authoritySyncStats = new AuthoritySyncStats(authorityName3, null);
                authorityMap.put(authorityName3, authoritySyncStats);
            }
            authoritySyncStats.elapsedTime += elapsedTime;
            authoritySyncStats.times++;
            Map<String, AccountSyncStats> accountMap = authoritySyncStats.accountMap;
            AccountSyncStats accountSyncStats = accountMap.get(accountKey3);
            if (accountSyncStats == null) {
                accountSyncStats = new AccountSyncStats(accountKey3, null);
                accountMap.put(accountKey3, accountSyncStats);
            }
            accountSyncStats.elapsedTime += elapsedTime;
            accountSyncStats.times++;
        }
        if (totalElapsedTime > 0) {
            pw.println();
            pw.printf("Detailed Statistics (Recent history):  %d (# of times) %ds (sync time)\n", Long.valueOf(totalTimes), Long.valueOf(totalElapsedTime / 1000));
            List<AuthoritySyncStats> sortedAuthorities = new ArrayList<>(authorityMap.values());
            Collections.sort(sortedAuthorities, new Comparator<AuthoritySyncStats>() {
                @Override
                public int compare(AuthoritySyncStats lhs, AuthoritySyncStats rhs) {
                    int compare = Integer.compare(rhs.times, lhs.times);
                    if (compare == 0) {
                        return Long.compare(rhs.elapsedTime, lhs.elapsedTime);
                    }
                    return compare;
                }
            });
            int maxLength = Math.max(maxAuthority, maxAccount + 3);
            int padLength = maxLength + 4 + 2 + 10 + 11;
            char[] chars = new char[padLength];
            Arrays.fill(chars, '-');
            String separator = new String(chars);
            String authorityFormat = String.format("  %%-%ds: %%-9s  %%-11s\n", Integer.valueOf(maxLength + 2));
            String accountFormat = String.format("    %%-%ds:   %%-9s  %%-11s\n", Integer.valueOf(maxLength));
            pw.println(separator);
            for (AuthoritySyncStats authoritySyncStats2 : sortedAuthorities) {
                String name = authoritySyncStats2.name;
                long elapsedTime2 = authoritySyncStats2.elapsedTime;
                int times = authoritySyncStats2.times;
                String timeStr = String.format("%ds/%d%%", Long.valueOf(elapsedTime2 / 1000), Long.valueOf((100 * elapsedTime2) / totalElapsedTime));
                String timesStr = String.format("%d/%d%%", Integer.valueOf(times), Long.valueOf(((long) (times * 100)) / totalTimes));
                pw.printf(authorityFormat, name, timesStr, timeStr);
                List<AccountSyncStats> sortedAccounts = new ArrayList<>(authoritySyncStats2.accountMap.values());
                Collections.sort(sortedAccounts, new Comparator<AccountSyncStats>() {
                    @Override
                    public int compare(AccountSyncStats lhs, AccountSyncStats rhs) {
                        int compare = Integer.compare(rhs.times, lhs.times);
                        if (compare == 0) {
                            return Long.compare(rhs.elapsedTime, lhs.elapsedTime);
                        }
                        return compare;
                    }
                });
                for (AccountSyncStats stats : sortedAccounts) {
                    long elapsedTime3 = stats.elapsedTime;
                    int times2 = stats.times;
                    String timeStr2 = String.format("%ds/%d%%", Long.valueOf(elapsedTime3 / 1000), Long.valueOf((100 * elapsedTime3) / totalElapsedTime));
                    String timesStr2 = String.format("%d/%d%%", Integer.valueOf(times2), Long.valueOf(((long) (times2 * 100)) / totalTimes));
                    pw.printf(accountFormat, stats.name, timesStr2, timeStr2);
                }
                pw.println(separator);
            }
        }
        pw.println();
        pw.println("Recent Sync History");
        String format = "  %-" + maxAccount + "s  %-" + maxAuthority + "s %s\n";
        Map<String, Long> lastTimeMap = Maps.newHashMap();
        PackageManager pm = this.mContext.getPackageManager();
        for (int i = 0; i < N; i++) {
            SyncStorageEngine.SyncHistoryItem item2 = items.get(i);
            SyncStorageEngine.AuthorityInfo authorityInfo2 = this.mSyncStorageEngine.getAuthority(item2.authorityId);
            if (authorityInfo2 != null) {
                authorityName2 = authorityInfo2.target.provider;
                accountKey2 = authorityInfo2.target.account.name + "/" + authorityInfo2.target.account.type + " u" + authorityInfo2.target.userId;
            } else {
                authorityName2 = "Unknown";
                accountKey2 = "Unknown";
            }
            long elapsedTime4 = item2.elapsedTime;
            Time time = new Time();
            long eventTime = item2.eventTime;
            time.set(eventTime);
            String key = authorityName2 + "/" + accountKey2;
            Long lastEventTime = lastTimeMap.get(key);
            if (lastEventTime == null) {
                diffString = "";
            } else {
                long diff = (lastEventTime.longValue() - eventTime) / 1000;
                if (diff < 60) {
                    diffString = String.valueOf(diff);
                } else if (diff < DEFAULT_MAX_SYNC_RETRY_TIME_IN_SECONDS) {
                    diffString = String.format("%02d:%02d", Long.valueOf(diff / 60), Long.valueOf(diff % 60));
                } else {
                    long sec = diff % DEFAULT_MAX_SYNC_RETRY_TIME_IN_SECONDS;
                    diffString = String.format("%02d:%02d:%02d", Long.valueOf(diff / DEFAULT_MAX_SYNC_RETRY_TIME_IN_SECONDS), Long.valueOf(sec / 60), Long.valueOf(sec % 60));
                }
            }
            lastTimeMap.put(key, Long.valueOf(eventTime));
            pw.printf("  #%-3d: %s %8s  %5.1fs  %8s", Integer.valueOf(i + 1), formatTime(eventTime), SyncStorageEngine.SOURCES[item2.source], Float.valueOf(elapsedTime4 / 1000.0f), diffString);
            pw.printf(format, accountKey2, authorityName2, SyncOperation.reasonToString(pm, item2.reason));
            if (item2.event != 1 || item2.upstreamActivity != 0 || item2.downstreamActivity != 0) {
                pw.printf("    event=%d upstreamActivity=%d downstreamActivity=%d\n", Integer.valueOf(item2.event), Long.valueOf(item2.upstreamActivity), Long.valueOf(item2.downstreamActivity));
            }
            if (item2.mesg != null && !SyncStorageEngine.MESG_SUCCESS.equals(item2.mesg)) {
                pw.printf("    mesg=%s\n", item2.mesg);
            }
        }
        pw.println();
        pw.println("Recent Sync History Extras");
        for (int i2 = 0; i2 < N; i2++) {
            SyncStorageEngine.SyncHistoryItem item3 = items.get(i2);
            Bundle extras = item3.extras;
            if (extras != null && extras.size() != 0) {
                SyncStorageEngine.AuthorityInfo authorityInfo3 = this.mSyncStorageEngine.getAuthority(item3.authorityId);
                if (authorityInfo3 != null) {
                    authorityName = authorityInfo3.target.provider;
                    accountKey = authorityInfo3.target.account.name + "/" + authorityInfo3.target.account.type + " u" + authorityInfo3.target.userId;
                } else {
                    authorityName = "Unknown";
                    accountKey = "Unknown";
                }
                Time time2 = new Time();
                long eventTime2 = item3.eventTime;
                time2.set(eventTime2);
                pw.printf("  #%-3d: %s %8s ", Integer.valueOf(i2 + 1), formatTime(eventTime2), SyncStorageEngine.SOURCES[item3.source]);
                pw.printf(format, accountKey, authorityName, extras);
            }
        }
    }

    private void dumpDayStatistics(PrintWriter pw) {
        int delta;
        SyncStorageEngine.DayStats[] dses = this.mSyncStorageEngine.getDayStatistics();
        if (dses == null || dses[0] == null) {
            return;
        }
        pw.println();
        pw.println("Sync Statistics");
        pw.print("  Today:  ");
        dumpDayStatistic(pw, dses[0]);
        int today = dses[0].day;
        int i = 1;
        while (i <= 6 && i < dses.length) {
            SyncStorageEngine.DayStats ds = dses[i];
            if (ds == null || (delta = today - ds.day) > 6) {
                break;
            }
            pw.print("  Day-");
            pw.print(delta);
            pw.print(":  ");
            dumpDayStatistic(pw, ds);
            i++;
        }
        int weekDay = today;
        while (i < dses.length) {
            SyncStorageEngine.DayStats aggr = null;
            weekDay -= 7;
            while (true) {
                if (i < dses.length) {
                    SyncStorageEngine.DayStats ds2 = dses[i];
                    if (ds2 == null) {
                        i = dses.length;
                        break;
                    }
                    if (weekDay - ds2.day > 6) {
                        break;
                    }
                    i++;
                    if (aggr == null) {
                        aggr = new SyncStorageEngine.DayStats(weekDay);
                    }
                    aggr.successCount += ds2.successCount;
                    aggr.successTime += ds2.successTime;
                    aggr.failureCount += ds2.failureCount;
                    aggr.failureTime += ds2.failureTime;
                } else {
                    break;
                }
            }
            if (aggr != null) {
                pw.print("  Week-");
                pw.print((today - weekDay) / 7);
                pw.print(": ");
                dumpDayStatistic(pw, aggr);
            }
        }
    }

    private void dumpSyncAdapters(IndentingPrintWriter pw) {
        pw.println();
        List<UserInfo> users = getAllUsers();
        if (users == null) {
            return;
        }
        for (UserInfo user : users) {
            pw.println("Sync adapters for " + user + ":");
            pw.increaseIndent();
            for (RegisteredServicesCache.ServiceInfo<?> info : this.mSyncAdapters.getAllServices(user.id)) {
                pw.println(info);
            }
            pw.decreaseIndent();
            pw.println();
        }
    }

    private static class AuthoritySyncStats {
        Map<String, AccountSyncStats> accountMap;
        long elapsedTime;
        String name;
        int times;

        AuthoritySyncStats(String name, AuthoritySyncStats authoritySyncStats) {
            this(name);
        }

        private AuthoritySyncStats(String name) {
            this.accountMap = Maps.newHashMap();
            this.name = name;
        }
    }

    private static class AccountSyncStats {
        long elapsedTime;
        String name;
        int times;

        AccountSyncStats(String name, AccountSyncStats accountSyncStats) {
            this(name);
        }

        private AccountSyncStats(String name) {
            this.name = name;
        }
    }

    private class SyncTimeTracker {
        boolean mLastWasSyncing;
        private long mTimeSpentSyncing;
        long mWhenSyncStarted;

        SyncTimeTracker(SyncManager this$0, SyncTimeTracker syncTimeTracker) {
            this();
        }

        private SyncTimeTracker() {
            this.mLastWasSyncing = false;
            this.mWhenSyncStarted = 0L;
        }

        public synchronized void update() {
            boolean isSyncInProgress = !SyncManager.this.mActiveSyncContexts.isEmpty();
            if (isSyncInProgress == this.mLastWasSyncing) {
                return;
            }
            long now = SystemClock.elapsedRealtime();
            if (isSyncInProgress) {
                this.mWhenSyncStarted = now;
            } else {
                this.mTimeSpentSyncing += now - this.mWhenSyncStarted;
            }
            this.mLastWasSyncing = isSyncInProgress;
        }

        public synchronized long timeSpentSyncing() {
            if (!this.mLastWasSyncing) {
                return this.mTimeSpentSyncing;
            }
            long now = SystemClock.elapsedRealtime();
            return this.mTimeSpentSyncing + (now - this.mWhenSyncStarted);
        }
    }

    class ServiceConnectionData {
        public final ActiveSyncContext activeSyncContext;
        public final IBinder adapter;

        ServiceConnectionData(ActiveSyncContext activeSyncContext, IBinder adapter) {
            this.activeSyncContext = activeSyncContext;
            this.adapter = adapter;
        }
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
            if (!SyncManager.this.mProvisioned || !SyncManager.this.mBootCompleted || !SyncManager.this.mJobServiceReady) {
                return;
            }
            synchronized (this) {
                SyncManager.this.mSyncStorageEngine.restoreAllPeriodicSyncs();
                obtainMessage(2).sendToTarget();
            }
        }

        private boolean tryEnqueueMessageUntilReadyToRun(Message msg) {
            synchronized (this) {
                if (!SyncManager.this.mBootCompleted || !SyncManager.this.mProvisioned || !SyncManager.this.mJobServiceReady) {
                    Message m = Message.obtain(msg);
                    this.mUnreadyQueue.add(m);
                    return true;
                }
                return false;
            }
        }

        public SyncHandler(Looper looper) {
            super(looper);
            this.mSyncTimeTracker = new SyncTimeTracker(SyncManager.this, null);
            this.mWakeLocks = Maps.newHashMap();
            this.mUnreadyQueue = new ArrayList();
        }

        @Override
        public void handleMessage(Message msg) {
            try {
                SyncManager.this.mSyncManagerWakeLock.acquire();
                if (msg.what == 7) {
                    Slog.i("SyncManager", "Got SyncJobService instance.");
                    SyncManager.this.mSyncJobService = (SyncJobService) msg.obj;
                    SyncManager.this.mJobServiceReady = true;
                    checkIfDeviceReady();
                } else if (msg.what == 9) {
                    if (Log.isLoggable("SyncManager", 2)) {
                        Slog.v("SyncManager", "handleSyncHandlerMessage: MESSAGE_ACCOUNTS_UPDATED");
                    }
                    SyncStorageEngine.EndPoint targets = (SyncStorageEngine.EndPoint) msg.obj;
                    updateRunningAccountsH(targets);
                } else if (msg.what == 2) {
                    if (this.mUnreadyQueue != null) {
                        for (Message m : this.mUnreadyQueue) {
                            handleSyncMessage(m);
                        }
                        this.mUnreadyQueue = null;
                    }
                } else if (!tryEnqueueMessageUntilReadyToRun(msg)) {
                    handleSyncMessage(msg);
                }
            } finally {
                SyncManager.this.mSyncManagerWakeLock.release();
            }
        }

        private void handleSyncMessage(Message msg) {
            boolean isLoggable = Log.isLoggable("SyncManager", 2);
            try {
                SyncManager.this.mDataConnectionIsConnected = SyncManager.this.readDataConnectionState();
                switch (msg.what) {
                    case 1:
                        SyncFinishedOrCancelledMessagePayload payload = (SyncFinishedOrCancelledMessagePayload) msg.obj;
                        if (!SyncManager.this.isSyncStillActiveH(payload.activeSyncContext)) {
                            Log.d("SyncManager", "handleSyncHandlerMessage: dropping since the sync is no longer active: " + payload.activeSyncContext);
                        } else {
                            if (isLoggable) {
                                Slog.v("SyncManager", "syncFinished" + payload.activeSyncContext.mSyncOperation);
                            }
                            SyncManager.this.mSyncJobService.callJobFinished(payload.activeSyncContext.mSyncOperation.jobId, false);
                            runSyncFinishedOrCanceledH(payload.syncResult, payload.activeSyncContext);
                        }
                        break;
                    case 4:
                        ServiceConnectionData msgData = (ServiceConnectionData) msg.obj;
                        if (Log.isLoggable("SyncManager", 2)) {
                            Log.d("SyncManager", "handleSyncHandlerMessage: MESSAGE_SERVICE_CONNECTED: " + msgData.activeSyncContext);
                        }
                        if (SyncManager.this.isSyncStillActiveH(msgData.activeSyncContext)) {
                            runBoundToAdapterH(msgData.activeSyncContext, msgData.adapter);
                        }
                        break;
                    case 5:
                        ActiveSyncContext currentSyncContext = ((ServiceConnectionData) msg.obj).activeSyncContext;
                        if (Log.isLoggable("SyncManager", 2)) {
                            Log.d("SyncManager", "handleSyncHandlerMessage: MESSAGE_SERVICE_DISCONNECTED: " + currentSyncContext);
                        }
                        if (SyncManager.this.isSyncStillActiveH(currentSyncContext)) {
                            try {
                                if (currentSyncContext.mSyncAdapter != null) {
                                    currentSyncContext.mSyncAdapter.cancelSync(currentSyncContext);
                                }
                                break;
                            } catch (RemoteException e) {
                            }
                            SyncResult syncResult = new SyncResult();
                            syncResult.stats.numIoExceptions++;
                            SyncManager.this.mSyncJobService.callJobFinished(currentSyncContext.mSyncOperation.jobId, false);
                            runSyncFinishedOrCanceledH(syncResult, currentSyncContext);
                        }
                        break;
                    case 6:
                        SyncStorageEngine.EndPoint endpoint = (SyncStorageEngine.EndPoint) msg.obj;
                        Bundle extras = msg.peekData();
                        if (Log.isLoggable("SyncManager", 3)) {
                            Log.d("SyncManager", "handleSyncHandlerMessage: MESSAGE_CANCEL: " + endpoint + " bundle: " + extras);
                        }
                        cancelActiveSyncH(endpoint, extras);
                        break;
                    case 8:
                        ActiveSyncContext monitoredSyncContext = (ActiveSyncContext) msg.obj;
                        if (Log.isLoggable("SyncManager", 3)) {
                            Log.d("SyncManager", "handleSyncHandlerMessage: MESSAGE_MONITOR_SYNC: " + monitoredSyncContext.mSyncOperation.target);
                        }
                        if (isSyncNotUsingNetworkH(monitoredSyncContext)) {
                            Log.w("SyncManager", String.format("Detected sync making no progress for %s. cancelling.", monitoredSyncContext));
                            SyncManager.this.mSyncJobService.callJobFinished(monitoredSyncContext.mSyncOperation.jobId, false);
                            runSyncFinishedOrCanceledH(null, monitoredSyncContext);
                        } else {
                            SyncManager.this.postMonitorSyncProgressMessage(monitoredSyncContext);
                        }
                        break;
                    case 10:
                        startSyncH((SyncOperation) msg.obj);
                        break;
                    case 11:
                        SyncOperation op = (SyncOperation) msg.obj;
                        if (isLoggable) {
                            Slog.v("SyncManager", "Stop sync received.");
                        }
                        ActiveSyncContext asc = findActiveSyncContextH(op.jobId);
                        if (asc != null) {
                            runSyncFinishedOrCanceledH(null, asc);
                            boolean reschedule = msg.arg1 != 0;
                            boolean applyBackoff = msg.arg2 != 0;
                            if (isLoggable) {
                                Slog.v("SyncManager", "Stopping sync. Reschedule: " + reschedule + "Backoff: " + applyBackoff);
                            }
                            if (applyBackoff) {
                                SyncManager.this.increaseBackoffSetting(op.target);
                            }
                            if (reschedule) {
                                deferStoppedSyncH(op, 0L);
                            }
                        }
                        break;
                    case 12:
                        SyncManager.this.scheduleSyncOperationH((SyncOperation) msg.obj);
                        break;
                    case 13:
                        UpdatePeriodicSyncMessagePayload data = (UpdatePeriodicSyncMessagePayload) msg.obj;
                        updateOrAddPeriodicSyncH(data.target, data.pollFrequency, data.flex, data.extras);
                        break;
                    case 14:
                        removePeriodicSyncH((SyncStorageEngine.EndPoint) msg.obj, msg.getData());
                        break;
                }
            } finally {
                this.mSyncTimeTracker.update();
            }
        }

        private PowerManager.WakeLock getSyncWakeLock(SyncOperation operation) {
            String wakeLockKey = operation.wakeLockName();
            PowerManager.WakeLock wakeLock = this.mWakeLocks.get(wakeLockKey);
            if (wakeLock == null) {
                String name = SyncManager.SYNC_WAKE_LOCK_PREFIX + wakeLockKey;
                PowerManager.WakeLock wakeLock2 = SyncManager.this.mPowerManager.newWakeLock(1, name);
                wakeLock2.setReferenceCounted(false);
                this.mWakeLocks.put(wakeLockKey, wakeLock2);
                return wakeLock2;
            }
            return wakeLock;
        }

        private void deferSyncH(SyncOperation op, long delay) {
            SyncManager.this.mSyncJobService.callJobFinished(op.jobId, false);
            if (op.isPeriodic) {
                SyncManager.this.scheduleSyncOperationH(op.createOneTimeSyncOperation(), delay);
            } else {
                SyncManager.this.getJobScheduler().cancel(op.jobId);
                SyncManager.this.scheduleSyncOperationH(op, delay);
            }
        }

        private void deferStoppedSyncH(SyncOperation op, long delay) {
            if (op.isPeriodic) {
                SyncManager.this.scheduleSyncOperationH(op.createOneTimeSyncOperation(), delay);
            } else {
                SyncManager.this.scheduleSyncOperationH(op, delay);
            }
        }

        private void deferActiveSyncH(ActiveSyncContext asc) {
            SyncOperation op = asc.mSyncOperation;
            runSyncFinishedOrCanceledH(null, asc);
            deferSyncH(op, 10000L);
        }

        private void startSyncH(SyncOperation op) {
            boolean isLoggable = Log.isLoggable("SyncManager", 2);
            if (isLoggable) {
                Slog.v("SyncManager", op.toString());
            }
            if (SyncManager.this.mStorageIsLow) {
                deferSyncH(op, SyncManager.SYNC_DELAY_ON_LOW_STORAGE);
                return;
            }
            if (op.isPeriodic) {
                List<SyncOperation> ops = SyncManager.this.getAllPendingSyncs();
                for (SyncOperation syncOperation : ops) {
                    if (syncOperation.sourcePeriodicId == op.jobId) {
                        SyncManager.this.mSyncJobService.callJobFinished(op.jobId, false);
                        return;
                    }
                }
                Iterator asc$iterator = SyncManager.this.mActiveSyncContexts.iterator();
                while (asc$iterator.hasNext()) {
                    if (((ActiveSyncContext) asc$iterator.next()).mSyncOperation.sourcePeriodicId == op.jobId) {
                        SyncManager.this.mSyncJobService.callJobFinished(op.jobId, false);
                        return;
                    }
                }
                if (SyncManager.this.isAdapterDelayed(op.target)) {
                    deferSyncH(op, 0L);
                    return;
                }
            }
            Iterator asc$iterator2 = SyncManager.this.mActiveSyncContexts.iterator();
            while (true) {
                if (!asc$iterator2.hasNext()) {
                    break;
                }
                ActiveSyncContext asc = (ActiveSyncContext) asc$iterator2.next();
                if (asc.mSyncOperation.isConflict(op)) {
                    if (asc.mSyncOperation.findPriority() >= op.findPriority()) {
                        if (isLoggable) {
                            Slog.v("SyncManager", "Rescheduling sync due to conflict " + op.toString());
                        }
                        deferSyncH(op, 10000L);
                        return;
                    } else {
                        if (isLoggable) {
                            Slog.v("SyncManager", "Pushing back running sync due to a higher priority sync");
                        }
                        deferActiveSyncH(asc);
                    }
                }
            }
            if (!isOperationValid(op) || !dispatchSyncOperation(op)) {
                SyncManager.this.mSyncJobService.callJobFinished(op.jobId, false);
            }
            SyncManager.this.setAuthorityPendingState(op.target);
        }

        private ActiveSyncContext findActiveSyncContextH(int jobId) {
            for (ActiveSyncContext asc : SyncManager.this.mActiveSyncContexts) {
                SyncOperation op = asc.mSyncOperation;
                if (op != null && op.jobId == jobId) {
                    return asc;
                }
            }
            return null;
        }

        private void updateRunningAccountsH(SyncStorageEngine.EndPoint syncTargets) {
            AccountAndUser[] oldAccounts = SyncManager.this.mRunningAccounts;
            SyncManager.this.mRunningAccounts = AccountManagerService.getSingleton().getRunningAccounts();
            if (Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", "Accounts list: ");
                for (AccountAndUser acc : SyncManager.this.mRunningAccounts) {
                    Slog.v("SyncManager", acc.toString());
                }
            }
            if (SyncManager.this.mBootCompleted) {
                SyncManager.this.doDatabaseCleanup();
            }
            AccountAndUser[] accounts = SyncManager.this.mRunningAccounts;
            for (ActiveSyncContext currentSyncContext : SyncManager.this.mActiveSyncContexts) {
                if (!SyncManager.this.containsAccountAndUser(accounts, currentSyncContext.mSyncOperation.target.account, currentSyncContext.mSyncOperation.target.userId)) {
                    Log.d("SyncManager", "canceling sync since the account is no longer running");
                    SyncManager.this.sendSyncFinishedOrCanceledMessage(currentSyncContext, null);
                }
            }
            AccountAndUser[] accountAndUserArr = SyncManager.this.mRunningAccounts;
            int i = 0;
            int length = accountAndUserArr.length;
            while (true) {
                if (i >= length) {
                    break;
                }
                AccountAndUser aau = accountAndUserArr[i];
                if (SyncManager.this.containsAccountAndUser(oldAccounts, aau.account, aau.userId)) {
                    i++;
                } else {
                    if (Log.isLoggable("SyncManager", 3)) {
                        Log.d("SyncManager", "Account " + aau.account + " added, checking sync restore data");
                    }
                    AccountSyncSettingsBackupHelper.accountAdded(SyncManager.this.mContext);
                }
            }
            AccountAndUser[] allAccounts = AccountManagerService.getSingleton().getAllAccounts();
            List<SyncOperation> ops = SyncManager.this.getAllPendingSyncs();
            for (SyncOperation op : ops) {
                if (!SyncManager.this.containsAccountAndUser(allAccounts, op.target.account, op.target.userId)) {
                    SyncManager.this.getJobScheduler().cancel(op.jobId);
                }
            }
            if (syncTargets == null) {
                return;
            }
            SyncManager.this.scheduleSync(syncTargets.account, syncTargets.userId, -2, syncTargets.provider, null, 0L, 0L, true);
        }

        private void maybeUpdateSyncPeriodH(SyncOperation syncOperation, long pollFrequencyMillis, long flexMillis) {
            if (pollFrequencyMillis == syncOperation.periodMillis && flexMillis == syncOperation.flexMillis) {
                return;
            }
            if (Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", "updating period " + syncOperation + " to " + pollFrequencyMillis + " and flex to " + flexMillis);
            }
            SyncOperation newOp = new SyncOperation(syncOperation, pollFrequencyMillis, flexMillis);
            newOp.jobId = syncOperation.jobId;
            SyncManager.this.scheduleSyncOperationH(newOp);
        }

        private void updateOrAddPeriodicSyncH(SyncStorageEngine.EndPoint target, long pollFrequency, long flex, Bundle extras) {
            boolean isLoggable = Log.isLoggable("SyncManager", 2);
            SyncManager.this.verifyJobScheduler();
            long pollFrequencyMillis = pollFrequency * 1000;
            long flexMillis = flex * 1000;
            if (isLoggable) {
                Slog.v("SyncManager", "Addition to periodic syncs requested: " + target + " period: " + pollFrequency + " flexMillis: " + flex + " extras: " + extras.toString());
            }
            List<SyncOperation> ops = SyncManager.this.getAllPendingSyncs();
            for (SyncOperation op : ops) {
                if (op.isPeriodic && op.target.matchesSpec(target) && SyncManager.syncExtrasEquals(op.extras, extras, true)) {
                    maybeUpdateSyncPeriodH(op, pollFrequencyMillis, flexMillis);
                    return;
                }
            }
            if (isLoggable) {
                Slog.v("SyncManager", "Adding new periodic sync: " + target + " period: " + pollFrequency + " flexMillis: " + flex + " extras: " + extras.toString());
            }
            RegisteredServicesCache.ServiceInfo<SyncAdapterType> syncAdapterInfo = SyncManager.this.mSyncAdapters.getServiceInfo(SyncAdapterType.newKey(target.provider, target.account.type), target.userId);
            if (syncAdapterInfo == null) {
                return;
            }
            SyncManager.this.scheduleSyncOperationH(new SyncOperation(target, syncAdapterInfo.uid, syncAdapterInfo.componentName.getPackageName(), -4, 4, extras, ((SyncAdapterType) syncAdapterInfo.type).allowParallelSyncs(), true, -1, pollFrequencyMillis, flexMillis));
            SyncManager.this.mSyncStorageEngine.reportChange(1);
        }

        private void removePeriodicSyncInternalH(SyncOperation syncOperation) {
            List<SyncOperation> ops = SyncManager.this.getAllPendingSyncs();
            for (SyncOperation op : ops) {
                if (op.sourcePeriodicId == syncOperation.jobId || op.jobId == syncOperation.jobId) {
                    ActiveSyncContext asc = findActiveSyncContextH(syncOperation.jobId);
                    if (asc != null) {
                        SyncManager.this.mSyncJobService.callJobFinished(syncOperation.jobId, false);
                        runSyncFinishedOrCanceledH(null, asc);
                    }
                    SyncManager.this.getJobScheduler().cancel(op.jobId);
                }
            }
        }

        private void removePeriodicSyncH(SyncStorageEngine.EndPoint target, Bundle extras) {
            SyncManager.this.verifyJobScheduler();
            List<SyncOperation> ops = SyncManager.this.getAllPendingSyncs();
            for (SyncOperation op : ops) {
                if (op.isPeriodic && op.target.matchesSpec(target) && SyncManager.syncExtrasEquals(op.extras, extras, true)) {
                    removePeriodicSyncInternalH(op);
                }
            }
        }

        private boolean isSyncNotUsingNetworkH(ActiveSyncContext activeSyncContext) {
            long bytesTransferredCurrent = SyncManager.this.getTotalBytesTransferredByUid(activeSyncContext.mSyncAdapterUid);
            long deltaBytesTransferred = bytesTransferredCurrent - activeSyncContext.mBytesTransferredAtLastPoll;
            if (Log.isLoggable("SyncManager", 3)) {
                long mb = deltaBytesTransferred / 1048576;
                long remainder = deltaBytesTransferred % 1048576;
                long kb = remainder / 1024;
                Log.d("SyncManager", String.format("Time since last update: %ds. Delta transferred: %dMBs,%dKBs,%dBs", Long.valueOf((SystemClock.elapsedRealtime() - activeSyncContext.mLastPolledTimeElapsed) / 1000), Long.valueOf(mb), Long.valueOf(kb), Long.valueOf(remainder % 1024)));
            }
            return deltaBytesTransferred <= 10;
        }

        private boolean isOperationValid(SyncOperation op) {
            boolean isLoggable = Log.isLoggable("SyncManager", 2);
            SyncStorageEngine.EndPoint target = op.target;
            boolean syncEnabled = SyncManager.this.mSyncStorageEngine.getMasterSyncAutomatically(target.userId);
            AccountAndUser[] accounts = SyncManager.this.mRunningAccounts;
            if (!SyncManager.this.containsAccountAndUser(accounts, target.account, target.userId)) {
                if (isLoggable) {
                    Slog.v("SyncManager", "    Dropping sync operation: account doesn't exist.");
                }
                return false;
            }
            int state = SyncManager.this.getIsSyncable(target.account, target.userId, target.provider);
            if (state == 0) {
                if (isLoggable) {
                    Slog.v("SyncManager", "    Dropping sync operation: isSyncable == 0.");
                }
                return false;
            }
            boolean syncEnabled2 = syncEnabled ? SyncManager.this.mSyncStorageEngine.getSyncAutomatically(target.account, target.userId, target.provider) : false;
            boolean ignoreSystemConfiguration = op.isIgnoreSettings() || state < 0;
            if (!syncEnabled2 && !ignoreSystemConfiguration) {
                if (isLoggable) {
                    Slog.v("SyncManager", "    Dropping sync operation: disallowed by settings/network.");
                }
                return false;
            }
            return true;
        }

        private boolean dispatchSyncOperation(SyncOperation op) {
            if (Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", "dispatchSyncOperation: we are going to sync " + op);
                Slog.v("SyncManager", "num active syncs: " + SyncManager.this.mActiveSyncContexts.size());
                for (ActiveSyncContext syncContext : SyncManager.this.mActiveSyncContexts) {
                    Slog.v("SyncManager", syncContext.toString());
                }
            }
            SyncStorageEngine.EndPoint info = op.target;
            SyncAdapterType syncAdapterType = SyncAdapterType.newKey(info.provider, info.account.type);
            RegisteredServicesCache.ServiceInfo<SyncAdapterType> syncAdapterInfo = SyncManager.this.mSyncAdapters.getServiceInfo(syncAdapterType, info.userId);
            if (syncAdapterInfo == null) {
                Log.d("SyncManager", "can't find a sync adapter for " + syncAdapterType + ", removing settings for it");
                SyncManager.this.mSyncStorageEngine.removeAuthority(info);
                return false;
            }
            int targetUid = syncAdapterInfo.uid;
            ComponentName targetComponent = syncAdapterInfo.componentName;
            ActiveSyncContext activeSyncContext = SyncManager.this.new ActiveSyncContext(op, insertStartSyncEvent(op), targetUid);
            if (Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", "dispatchSyncOperation: starting " + activeSyncContext);
            }
            activeSyncContext.mSyncInfo = SyncManager.this.mSyncStorageEngine.addActiveSync(activeSyncContext);
            SyncManager.this.mActiveSyncContexts.add(activeSyncContext);
            SyncManager.this.postMonitorSyncProgressMessage(activeSyncContext);
            if (!activeSyncContext.bindToSyncAdapter(targetComponent, info.userId)) {
                Slog.e("SyncManager", "Bind attempt failed - target: " + targetComponent);
                closeActiveSyncContext(activeSyncContext);
                return false;
            }
            return true;
        }

        private void runBoundToAdapterH(ActiveSyncContext activeSyncContext, IBinder syncAdapter) {
            SyncOperation syncOperation = activeSyncContext.mSyncOperation;
            try {
                activeSyncContext.mIsLinkedToDeath = true;
                syncAdapter.linkToDeath(activeSyncContext, 0);
                activeSyncContext.mSyncAdapter = ISyncAdapter.Stub.asInterface(syncAdapter);
                activeSyncContext.mSyncAdapter.startSync(activeSyncContext, syncOperation.target.provider, syncOperation.target.account, syncOperation.extras);
            } catch (RemoteException remoteExc) {
                Log.d("SyncManager", "maybeStartNextSync: caught a RemoteException, rescheduling", remoteExc);
                closeActiveSyncContext(activeSyncContext);
                SyncManager.this.increaseBackoffSetting(syncOperation.target);
                SyncManager.this.scheduleSyncOperationH(syncOperation);
            } catch (RuntimeException exc) {
                closeActiveSyncContext(activeSyncContext);
                Slog.e("SyncManager", "Caught RuntimeException while starting the sync " + syncOperation, exc);
            }
        }

        private void cancelActiveSyncH(SyncStorageEngine.EndPoint info, Bundle extras) {
            ArrayList<ActiveSyncContext> activeSyncs = new ArrayList<>(SyncManager.this.mActiveSyncContexts);
            for (ActiveSyncContext activeSyncContext : activeSyncs) {
                if (activeSyncContext != null) {
                    SyncStorageEngine.EndPoint opInfo = activeSyncContext.mSyncOperation.target;
                    if (opInfo.matchesSpec(info) && (extras == null || SyncManager.syncExtrasEquals(activeSyncContext.mSyncOperation.extras, extras, false))) {
                        SyncManager.this.mSyncJobService.callJobFinished(activeSyncContext.mSyncOperation.jobId, false);
                        runSyncFinishedOrCanceledH(null, activeSyncContext);
                    }
                }
            }
        }

        private void reschedulePeriodicSyncH(SyncOperation syncOperation) {
            SyncOperation periodicSync = null;
            List<SyncOperation> ops = SyncManager.this.getAllPendingSyncs();
            Iterator op$iterator = ops.iterator();
            while (true) {
                if (!op$iterator.hasNext()) {
                    break;
                }
                SyncOperation op = (SyncOperation) op$iterator.next();
                if (op.isPeriodic && syncOperation.matchesPeriodicOperation(op)) {
                    periodicSync = op;
                    break;
                }
            }
            if (periodicSync == null) {
                return;
            }
            SyncManager.this.scheduleSyncOperationH(periodicSync);
        }

        private void runSyncFinishedOrCanceledH(SyncResult syncResult, ActiveSyncContext activeSyncContext) {
            String historyMessage;
            int downstreamActivity;
            int upstreamActivity;
            boolean isLoggable = Log.isLoggable("SyncManager", 2);
            SyncOperation syncOperation = activeSyncContext.mSyncOperation;
            SyncStorageEngine.EndPoint info = syncOperation.target;
            if (activeSyncContext.mIsLinkedToDeath) {
                activeSyncContext.mSyncAdapter.asBinder().unlinkToDeath(activeSyncContext, 0);
                activeSyncContext.mIsLinkedToDeath = false;
            }
            closeActiveSyncContext(activeSyncContext);
            long elapsedTime = SystemClock.elapsedRealtime() - activeSyncContext.mStartTime;
            if (!syncOperation.isPeriodic) {
                SyncManager.this.getJobScheduler().cancel(syncOperation.jobId);
            }
            if (syncResult != null) {
                if (isLoggable) {
                    Slog.v("SyncManager", "runSyncFinishedOrCanceled [finished]: " + syncOperation + ", result " + syncResult);
                }
                if (!syncResult.hasError()) {
                    historyMessage = SyncStorageEngine.MESG_SUCCESS;
                    downstreamActivity = 0;
                    upstreamActivity = 0;
                    SyncManager.this.clearBackoffSetting(syncOperation.target);
                    if (syncOperation.isDerivedFromFailedPeriodicSync()) {
                        reschedulePeriodicSyncH(syncOperation);
                    }
                } else {
                    Log.d("SyncManager", "failed sync operation " + syncOperation + ", " + syncResult);
                    SyncManager.this.increaseBackoffSetting(syncOperation.target);
                    if (!syncOperation.isPeriodic) {
                        SyncManager.this.maybeRescheduleSync(syncResult, syncOperation);
                    } else {
                        SyncManager.this.postScheduleSyncMessage(syncOperation.createOneTimeSyncOperation());
                    }
                    historyMessage = ContentResolver.syncErrorToString(syncResultToErrorNumber(syncResult));
                    downstreamActivity = 0;
                    upstreamActivity = 0;
                }
                SyncManager.this.setDelayUntilTime(syncOperation.target, syncResult.delayUntil);
            } else {
                if (isLoggable) {
                    Slog.v("SyncManager", "runSyncFinishedOrCanceled [canceled]: " + syncOperation);
                }
                if (activeSyncContext.mSyncAdapter != null) {
                    try {
                        activeSyncContext.mSyncAdapter.cancelSync(activeSyncContext);
                    } catch (RemoteException e) {
                    }
                }
                historyMessage = SyncStorageEngine.MESG_CANCELED;
                downstreamActivity = 0;
                upstreamActivity = 0;
            }
            stopSyncEvent(activeSyncContext.mHistoryRowId, syncOperation, historyMessage, upstreamActivity, downstreamActivity, elapsedTime);
            if (syncResult != null && syncResult.tooManyDeletions) {
                installHandleTooManyDeletesNotification(info.account, info.provider, syncResult.stats.numDeletes, info.userId);
            } else {
                SyncManager.this.mNotificationMgr.cancelAsUser(null, info.account.hashCode() ^ info.provider.hashCode(), new UserHandle(info.userId));
            }
            if (syncResult == null || !syncResult.fullSyncRequested) {
                return;
            }
            SyncManager.this.scheduleSyncOperationH(new SyncOperation(info.account, info.userId, syncOperation.owningUid, syncOperation.owningPackage, syncOperation.reason, syncOperation.syncSource, info.provider, new Bundle(), syncOperation.allowParallelSyncs));
        }

        private void closeActiveSyncContext(ActiveSyncContext activeSyncContext) {
            activeSyncContext.close();
            SyncManager.this.mActiveSyncContexts.remove(activeSyncContext);
            SyncManager.this.mSyncStorageEngine.removeActiveSync(activeSyncContext.mSyncInfo, activeSyncContext.mSyncOperation.target.userId);
            if (Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", "removing all MESSAGE_MONITOR_SYNC & MESSAGE_SYNC_EXPIRED for " + activeSyncContext.toString());
            }
            SyncManager.this.mSyncHandler.removeMessages(8, activeSyncContext);
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

        private void installHandleTooManyDeletesNotification(Account account, String authority, long numDeletes, int userId) {
            ProviderInfo providerInfo;
            if (SyncManager.this.mNotificationMgr == null || (providerInfo = SyncManager.this.mContext.getPackageManager().resolveContentProvider(authority, 0)) == null) {
                return;
            }
            CharSequence authorityName = providerInfo.loadLabel(SyncManager.this.mContext.getPackageManager());
            Intent clickIntent = new Intent(SyncManager.this.mContext, (Class<?>) SyncActivityTooManyDeletes.class);
            clickIntent.putExtra("account", account);
            clickIntent.putExtra("authority", authority);
            clickIntent.putExtra("provider", authorityName.toString());
            clickIntent.putExtra("numDeletes", numDeletes);
            if (!isActivityAvailable(clickIntent)) {
                Log.w("SyncManager", "No activity found to handle too many deletes.");
                return;
            }
            UserHandle user = new UserHandle(userId);
            PendingIntent pendingIntent = PendingIntent.getActivityAsUser(SyncManager.this.mContext, 0, clickIntent, 268435456, null, user);
            CharSequence tooManyDeletesDescFormat = SyncManager.this.mContext.getResources().getText(R.string.accessibility_shortcut_menu_item_status_off);
            Context contextForUser = SyncManager.this.getContextForUser(user);
            Notification notification = new Notification.Builder(contextForUser).setSmallIcon(R.drawable.list_selector_background_light).setTicker(SyncManager.this.mContext.getString(R.string.accessibility_shortcut_disabling_service)).setWhen(System.currentTimeMillis()).setColor(contextForUser.getColor(R.color.system_accent3_600)).setContentTitle(contextForUser.getString(R.string.accessibility_shortcut_enabling_service)).setContentText(String.format(tooManyDeletesDescFormat.toString(), authorityName)).setContentIntent(pendingIntent).build();
            notification.flags |= 2;
            SyncManager.this.mNotificationMgr.notifyAsUser(null, account.hashCode() ^ authority.hashCode(), notification, user);
        }

        private boolean isActivityAvailable(Intent intent) {
            PackageManager pm = SyncManager.this.mContext.getPackageManager();
            List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
            int listSize = list.size();
            for (int i = 0; i < listSize; i++) {
                ResolveInfo resolveInfo = list.get(i);
                if ((resolveInfo.activityInfo.applicationInfo.flags & 1) != 0) {
                    return true;
                }
            }
            return false;
        }

        public long insertStartSyncEvent(SyncOperation syncOperation) {
            long now = System.currentTimeMillis();
            EventLog.writeEvent(2720, syncOperation.toEventLog(0));
            return SyncManager.this.mSyncStorageEngine.insertStartSyncEvent(syncOperation, now);
        }

        public void stopSyncEvent(long rowId, SyncOperation syncOperation, String resultMessage, int upstreamActivity, int downstreamActivity, long elapsedTime) {
            EventLog.writeEvent(2720, syncOperation.toEventLog(1));
            SyncManager.this.mSyncStorageEngine.stopSyncEvent(rowId, elapsedTime, resultMessage, downstreamActivity, upstreamActivity);
        }
    }

    private boolean isSyncStillActiveH(ActiveSyncContext activeSyncContext) {
        for (ActiveSyncContext sync : this.mActiveSyncContexts) {
            if (sync == activeSyncContext) {
                return true;
            }
        }
        return false;
    }

    public static boolean syncExtrasEquals(Bundle b1, Bundle b2, boolean includeSyncSettings) {
        if (b1 == b2) {
            return true;
        }
        if (includeSyncSettings && b1.size() != b2.size()) {
            return false;
        }
        Bundle bigger = b1.size() > b2.size() ? b1 : b2;
        Bundle smaller = b1.size() > b2.size() ? b2 : b1;
        for (String key : bigger.keySet()) {
            if (includeSyncSettings || !isSyncSetting(key)) {
                if (!smaller.containsKey(key) || !Objects.equals(bigger.get(key), smaller.get(key))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isSyncSetting(String key) {
        return key.equals("expedited") || key.equals("ignore_settings") || key.equals("ignore_backoff") || key.equals("do_not_retry") || key.equals("force") || key.equals("upload") || key.equals("deletions_override") || key.equals("discard_deletions") || key.equals("expected_upload") || key.equals("expected_download") || key.equals("sync_priority") || key.equals("allow_metered") || key.equals("initialize");
    }

    static class PrintTable {
        private final int mCols;
        private ArrayList<Object[]> mTable = Lists.newArrayList();

        PrintTable(int cols) {
            this.mCols = cols;
        }

        void set(int row, int col, Object... values) {
            if (values.length + col > this.mCols) {
                throw new IndexOutOfBoundsException("Table only has " + this.mCols + " columns. can't set " + values.length + " at column " + col);
            }
            for (int i = this.mTable.size(); i <= row; i++) {
                Object[] list = new Object[this.mCols];
                this.mTable.add(list);
                for (int j = 0; j < this.mCols; j++) {
                    list[j] = "";
                }
            }
            System.arraycopy(values, 0, this.mTable.get(row), col, values.length);
        }

        void writeTo(PrintWriter out) {
            String[] formats = new String[this.mCols];
            int totalLength = 0;
            for (int col = 0; col < this.mCols; col++) {
                int maxLength = 0;
                for (Object[] row : this.mTable) {
                    int length = row[col].toString().length();
                    if (length > maxLength) {
                        maxLength = length;
                    }
                }
                totalLength += maxLength;
                formats[col] = String.format("%%-%ds", Integer.valueOf(maxLength));
            }
            formats[this.mCols - 1] = "%s";
            printRow(out, formats, this.mTable.get(0));
            int totalLength2 = totalLength + ((this.mCols - 1) * 2);
            for (int i = 0; i < totalLength2; i++) {
                out.print("-");
            }
            out.println();
            int mTableSize = this.mTable.size();
            for (int i2 = 1; i2 < mTableSize; i2++) {
                Object[] row2 = this.mTable.get(i2);
                printRow(out, formats, row2);
            }
        }

        private void printRow(PrintWriter out, String[] formats, Object[] row) {
            int rowLength = row.length;
            for (int j = 0; j < rowLength; j++) {
                out.printf(String.format(formats[j], row[j].toString()), new Object[0]);
                out.print("  ");
            }
            out.println();
        }

        public int getNumRows() {
            return this.mTable.size();
        }
    }

    private Context getContextForUser(UserHandle user) {
        try {
            return this.mContext.createPackageContextAsUser(this.mContext.getPackageName(), 0, user);
        } catch (PackageManager.NameNotFoundException e) {
            return this.mContext;
        }
    }
}
