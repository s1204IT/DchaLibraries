package com.android.server.content;

import android.R;
import android.accounts.Account;
import android.accounts.AccountAndUser;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AppGlobals;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ISyncAdapter;
import android.content.ISyncContext;
import android.content.ISyncServiceAdapter;
import android.content.ISyncStatusObserver;
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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.WorkSource;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.EventLog;
import android.util.Log;
import android.util.Pair;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.app.IBatteryStats;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.accounts.AccountManagerService;
import com.android.server.content.SyncStorageEngine;
import com.android.server.job.controllers.JobStatus;
import com.google.android.collect.Lists;
import com.google.android.collect.Maps;
import com.google.android.collect.Sets;
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
    private static final String ACTION_SYNC_ALARM = "android.content.syncmanager.SYNC_ALARM";
    private static final long ACTIVE_SYNC_TIMEOUT_MILLIS = 1800000;
    private static final long DEFAULT_MAX_SYNC_RETRY_TIME_IN_SECONDS = 3600;
    private static final int DELAY_RETRY_SYNC_IN_PROGRESS_IN_SECONDS = 10;
    private static final String HANDLE_SYNC_ALARM_WAKE_LOCK = "SyncManagerHandleSyncAlarm";
    private static final AccountAndUser[] INITIAL_ACCOUNTS_ARRAY;
    private static final long INITIAL_SYNC_RETRY_TIME_IN_MS = 30000;
    private static final long LOCAL_SYNC_DELAY;
    private static final int MAX_SIMULTANEOUS_INITIALIZATION_SYNCS;
    private static final int MAX_SIMULTANEOUS_REGULAR_SYNCS;
    private static final long MAX_TIME_PER_SYNC;
    private static final long SYNC_ALARM_TIMEOUT_MAX = 7200000;
    private static final long SYNC_ALARM_TIMEOUT_MIN = 30000;
    private static final String SYNC_LOOP_WAKE_LOCK = "SyncLoopWakeLock";
    private static final long SYNC_NOTIFICATION_DELAY;
    private static final String SYNC_WAKE_LOCK_PREFIX = "*sync*/";
    private static final String TAG = "SyncManager";
    private final IBatteryStats mBatteryStats;
    private ConnectivityManager mConnManagerDoNotUseDirectly;
    private Context mContext;
    private volatile PowerManager.WakeLock mHandleAlarmWakeLock;
    private final NotificationManager mNotificationMgr;
    private final PowerManager mPowerManager;
    protected SyncAdaptersCache mSyncAdapters;
    private final PendingIntent mSyncAlarmIntent;
    private final SyncHandler mSyncHandler;
    private volatile PowerManager.WakeLock mSyncManagerWakeLock;

    @GuardedBy("mSyncQueue")
    private final SyncQueue mSyncQueue;
    private int mSyncRandomOffsetMillis;
    private SyncStorageEngine mSyncStorageEngine;
    private final UserManager mUserManager;
    private volatile AccountAndUser[] mRunningAccounts = INITIAL_ACCOUNTS_ARRAY;
    private volatile boolean mDataConnectionIsConnected = false;
    private volatile boolean mStorageIsLow = false;
    private AlarmManager mAlarmService = null;
    protected final ArrayList<ActiveSyncContext> mActiveSyncContexts = Lists.newArrayList();
    private boolean mNeedSyncActiveNotification = false;
    private BroadcastReceiver mStorageIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.DEVICE_STORAGE_LOW".equals(action)) {
                if (Log.isLoggable("SyncManager", 2)) {
                    Log.v("SyncManager", "Internal storage is low.");
                }
                SyncManager.this.mStorageIsLow = true;
                SyncManager.this.cancelActiveSync(SyncStorageEngine.EndPoint.USER_ALL_PROVIDER_ALL_ACCOUNTS_ALL, null);
                return;
            }
            if ("android.intent.action.DEVICE_STORAGE_OK".equals(action)) {
                if (Log.isLoggable("SyncManager", 2)) {
                    Log.v("SyncManager", "Internal storage is ok.");
                }
                SyncManager.this.mStorageIsLow = false;
                SyncManager.this.sendCheckAlarmsMessage();
            }
        }
    };
    private BroadcastReceiver mBootCompletedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            SyncManager.this.mSyncHandler.onBootCompleted();
        }
    };
    private BroadcastReceiver mAccountsUpdatedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            SyncManager.this.updateRunningAccounts();
            SyncManager.this.scheduleSync(null, -1, -2, null, null, 0L, 0L, false);
        }
    };
    private BroadcastReceiver mConnectivityIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean wasConnected = SyncManager.this.mDataConnectionIsConnected;
            SyncManager.this.mDataConnectionIsConnected = SyncManager.this.readDataConnectionState();
            if (SyncManager.this.mDataConnectionIsConnected) {
                if (!wasConnected) {
                    if (Log.isLoggable("SyncManager", 2)) {
                        Log.v("SyncManager", "Reconnection detected: clearing all backoffs");
                    }
                    synchronized (SyncManager.this.mSyncQueue) {
                        SyncManager.this.mSyncStorageEngine.clearAllBackoffsLocked(SyncManager.this.mSyncQueue);
                    }
                }
                SyncManager.this.sendCheckAlarmsMessage();
            }
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
            if (userId != -10000) {
                if ("android.intent.action.USER_REMOVED".equals(action)) {
                    SyncManager.this.onUserRemoved(userId);
                } else if ("android.intent.action.USER_STARTING".equals(action)) {
                    SyncManager.this.onUserStarting(userId);
                } else if ("android.intent.action.USER_STOPPING".equals(action)) {
                    SyncManager.this.onUserStopping(userId);
                }
            }
        }
    };
    private volatile boolean mBootCompleted = false;

    static {
        boolean isLargeRAM = !ActivityManager.isLowRamDeviceStatic();
        int defaultMaxInitSyncs = isLargeRAM ? 5 : 2;
        int defaultMaxRegularSyncs = isLargeRAM ? 2 : 1;
        MAX_SIMULTANEOUS_INITIALIZATION_SYNCS = SystemProperties.getInt("sync.max_init_syncs", defaultMaxInitSyncs);
        MAX_SIMULTANEOUS_REGULAR_SYNCS = SystemProperties.getInt("sync.max_regular_syncs", defaultMaxRegularSyncs);
        LOCAL_SYNC_DELAY = SystemProperties.getLong("sync.local_sync_delay", 30000L);
        MAX_TIME_PER_SYNC = SystemProperties.getLong("sync.max_time_per_sync", 300000L);
        SYNC_NOTIFICATION_DELAY = SystemProperties.getLong("sync.notification_delay", 30000L);
        INITIAL_ACCOUNTS_ARRAY = new AccountAndUser[0];
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

    public void updateRunningAccounts() {
        this.mRunningAccounts = AccountManagerService.getSingleton().getRunningAccounts();
        if (this.mBootCompleted) {
            doDatabaseCleanup();
        }
        AccountAndUser[] accounts = this.mRunningAccounts;
        for (ActiveSyncContext currentSyncContext : this.mActiveSyncContexts) {
            if (!containsAccountAndUser(accounts, currentSyncContext.mSyncOperation.target.account, currentSyncContext.mSyncOperation.target.userId)) {
                Log.d("SyncManager", "canceling sync since the account is no longer running");
                sendSyncFinishedOrCanceledMessage(currentSyncContext, null);
            }
        }
        sendCheckAlarmsMessage();
    }

    private void doDatabaseCleanup() {
        for (UserInfo user : this.mUserManager.getUsers(true)) {
            if (!user.partial) {
                Account[] accountsForUser = AccountManagerService.getSingleton().getAccounts(user.id);
                this.mSyncStorageEngine.doDatabaseCleanup(accountsForUser, user.id);
            }
        }
    }

    private boolean readDataConnectionState() {
        NetworkInfo networkInfo = getConnectivityManager().getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
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

    public SyncManager(Context context, boolean factoryTest) {
        this.mContext = context;
        SyncStorageEngine.init(context);
        this.mSyncStorageEngine = SyncStorageEngine.getSingleton();
        this.mSyncStorageEngine.setOnSyncRequestListener(new SyncStorageEngine.OnSyncRequestListener() {
            @Override
            public void onSyncRequest(SyncStorageEngine.EndPoint info, int reason, Bundle extras) {
                if (info.target_provider) {
                    SyncManager.this.scheduleSync(info.account, info.userId, reason, info.provider, extras, 0L, 0L, false);
                } else if (info.target_service) {
                    SyncManager.this.scheduleSync(info.service, info.userId, reason, extras, 0L, 0L);
                }
            }
        });
        this.mSyncAdapters = new SyncAdaptersCache(this.mContext);
        this.mSyncQueue = new SyncQueue(this.mContext.getPackageManager(), this.mSyncStorageEngine, this.mSyncAdapters);
        this.mSyncHandler = new SyncHandler(BackgroundThread.get().getLooper());
        this.mSyncAdapters.setListener(new RegisteredServicesCacheListener<SyncAdapterType>() {
            public void onServiceChanged(SyncAdapterType type, int userId, boolean removed) {
                if (!removed) {
                    SyncManager.this.scheduleSync(null, -1, -3, type.authority, null, 0L, 0L, false);
                }
            }
        }, this.mSyncHandler);
        this.mSyncAlarmIntent = PendingIntent.getBroadcast(this.mContext, 0, new Intent(ACTION_SYNC_ALARM), 0);
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
        intentFilter4.addAction("android.intent.action.USER_STARTING");
        intentFilter4.addAction("android.intent.action.USER_STOPPING");
        this.mContext.registerReceiverAsUser(this.mUserIntentReceiver, UserHandle.ALL, intentFilter4, null, null);
        if (!factoryTest) {
            this.mNotificationMgr = (NotificationManager) context.getSystemService("notification");
            context.registerReceiver(new SyncAlarmIntentReceiver(), new IntentFilter(ACTION_SYNC_ALARM));
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
        this.mSyncStorageEngine.addStatusChangeListener(1, new ISyncStatusObserver.Stub() {
            public void onStatusChanged(int which) {
                SyncManager.this.sendCheckAlarmsMessage();
            }
        });
        if (!factoryTest) {
            this.mContext.registerReceiverAsUser(this.mAccountsUpdatedReceiver, UserHandle.ALL, new IntentFilter("android.accounts.LOGIN_ACCOUNTS_CHANGED"), null, null);
        }
        this.mSyncRandomOffsetMillis = this.mSyncStorageEngine.getSyncRandomOffset() * 1000;
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
        if (userInfo != null && userInfo.isRestricted() && (syncAdapterInfo = this.mSyncAdapters.getServiceInfo(SyncAdapterType.newKey(providerName, account.type), userId)) != null) {
            try {
                PackageInfo pInfo = AppGlobals.getPackageManager().getPackageInfo(syncAdapterInfo.componentName.getPackageName(), 0, userId);
                if (pInfo != null) {
                    if (pInfo.restrictedAccountType == null || !pInfo.restrictedAccountType.equals(account.type)) {
                        return 0;
                    }
                    return isSyncable;
                }
                return isSyncable;
            } catch (RemoteException e) {
                return isSyncable;
            }
        }
        return isSyncable;
    }

    private void ensureAlarmService() {
        if (this.mAlarmService == null) {
            this.mAlarmService = (AlarmManager) this.mContext.getSystemService("alarm");
        }
    }

    public void scheduleSync(ComponentName cname, int userId, int uid, Bundle extras, long beforeRunTimeMillis, long runtimeMillis) {
        boolean isLoggable = Log.isLoggable("SyncManager", 2);
        if (isLoggable) {
            Log.d("SyncManager", "one off sync for: " + cname + " " + extras.toString());
        }
        Boolean expedited = Boolean.valueOf(extras.getBoolean("expedited", false));
        if (expedited.booleanValue()) {
            runtimeMillis = -1;
        }
        boolean ignoreSettings = extras.getBoolean("ignore_settings", false);
        boolean isEnabled = this.mSyncStorageEngine.getIsTargetServiceActive(cname, userId);
        boolean syncAllowed = ignoreSettings || this.mSyncStorageEngine.getMasterSyncAutomatically(userId);
        if (!syncAllowed) {
            if (isLoggable) {
                Log.d("SyncManager", "scheduleSync: sync of " + cname + " not allowed, dropping request.");
            }
        } else {
            if (!isEnabled) {
                if (isLoggable) {
                    Log.d("SyncManager", "scheduleSync: " + cname + " is not enabled, dropping request");
                    return;
                }
                return;
            }
            SyncStorageEngine.EndPoint info = new SyncStorageEngine.EndPoint(cname, userId);
            Pair<Long, Long> backoff = this.mSyncStorageEngine.getBackoff(info);
            long delayUntil = this.mSyncStorageEngine.getDelayUntilTime(info);
            long backoffTime = backoff != null ? ((Long) backoff.first).longValue() : 0L;
            if (isLoggable) {
                Log.v("SyncManager", "schedule Sync:, delay until " + delayUntil + ", run by " + runtimeMillis + ", flex " + beforeRunTimeMillis + ", source 5, sync service " + cname + ", extras " + extras);
            }
            scheduleSyncOperation(new SyncOperation(cname, userId, uid, 5, extras, runtimeMillis, beforeRunTimeMillis, backoffTime, delayUntil));
        }
    }

    public void scheduleSync(Account requestedAccount, int userId, int reason, String requestedAuthority, Bundle extras, long beforeRuntimeMillis, long runtimeMillis, boolean onlyThoseWithUnkownSyncableState) {
        AccountAndUser[] accounts;
        int source;
        RegisteredServicesCache.ServiceInfo<SyncAdapterType> syncAdapterInfo;
        boolean isLoggable = Log.isLoggable("SyncManager", 2);
        if (extras == null) {
            extras = new Bundle();
        }
        if (isLoggable) {
            Log.d("SyncManager", "one-time sync for: " + requestedAccount + " " + extras.toString() + " " + requestedAuthority);
        }
        Boolean expedited = Boolean.valueOf(extras.getBoolean("expedited", false));
        if (expedited.booleanValue()) {
            runtimeMillis = -1;
        }
        if (requestedAccount != null && userId != -1) {
            accounts = new AccountAndUser[]{new AccountAndUser(requestedAccount, userId)};
        } else {
            accounts = this.mRunningAccounts;
            if (accounts.length == 0) {
                if (isLoggable) {
                    Log.v("SyncManager", "scheduleSync: no accounts configured, dropping");
                    return;
                }
                return;
            }
        }
        boolean uploadOnly = extras.getBoolean("upload", false);
        boolean manualSync = extras.getBoolean("force", false);
        if (manualSync) {
            extras.putBoolean("ignore_backoff", true);
            extras.putBoolean("ignore_settings", true);
        }
        boolean ignoreSettings = extras.getBoolean("ignore_settings", false);
        if (uploadOnly) {
            source = 1;
        } else if (manualSync) {
            source = 3;
        } else if (requestedAuthority == null) {
            source = 2;
        } else {
            source = 0;
        }
        AccountAndUser[] arr$ = accounts;
        int len$ = arr$.length;
        int i$ = 0;
        while (true) {
            int i$2 = i$;
            if (i$2 < len$) {
                AccountAndUser account = arr$[i$2];
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
                        boolean allowParallelSyncs = ((SyncAdapterType) syncAdapterInfo.type).allowParallelSyncs();
                        boolean isAlwaysSyncable = ((SyncAdapterType) syncAdapterInfo.type).isAlwaysSyncable();
                        if (isSyncable < 0 && isAlwaysSyncable) {
                            this.mSyncStorageEngine.setIsSyncable(account.account, account.userId, authority, 1);
                            isSyncable = 1;
                        }
                        if (!onlyThoseWithUnkownSyncableState || isSyncable < 0) {
                            if (((SyncAdapterType) syncAdapterInfo.type).supportsUploading() || !uploadOnly) {
                                boolean syncAllowed = isSyncable < 0 || ignoreSettings || (this.mSyncStorageEngine.getMasterSyncAutomatically(account.userId) && this.mSyncStorageEngine.getSyncAutomatically(account.account, account.userId, authority));
                                if (!syncAllowed) {
                                    if (isLoggable) {
                                        Log.d("SyncManager", "scheduleSync: sync of " + account + ", " + authority + " is not allowed, dropping request");
                                    }
                                } else {
                                    SyncStorageEngine.EndPoint info = new SyncStorageEngine.EndPoint(account.account, authority, account.userId);
                                    Pair<Long, Long> backoff = this.mSyncStorageEngine.getBackoff(info);
                                    long delayUntil = this.mSyncStorageEngine.getDelayUntilTime(info);
                                    long backoffTime = backoff != null ? ((Long) backoff.first).longValue() : 0L;
                                    if (isSyncable < 0) {
                                        Bundle newExtras = new Bundle();
                                        newExtras.putBoolean("initialize", true);
                                        if (isLoggable) {
                                            Log.v("SyncManager", "schedule initialisation Sync:, delay until " + delayUntil + ", run by 0, flex 0, source " + source + ", account " + account + ", authority " + authority + ", extras " + newExtras);
                                        }
                                        scheduleSyncOperation(new SyncOperation(account.account, account.userId, reason, source, authority, newExtras, 0L, 0L, backoffTime, delayUntil, allowParallelSyncs));
                                    }
                                    if (!onlyThoseWithUnkownSyncableState) {
                                        if (isLoggable) {
                                            Log.v("SyncManager", "scheduleSync: delay until " + delayUntil + " run by " + runtimeMillis + " flex " + beforeRuntimeMillis + ", source " + source + ", account " + account + ", authority " + authority + ", extras " + extras);
                                        }
                                        scheduleSyncOperation(new SyncOperation(account.account, account.userId, reason, source, authority, extras, runtimeMillis, beforeRuntimeMillis, backoffTime, delayUntil, allowParallelSyncs));
                                    }
                                }
                            }
                        }
                    }
                }
                i$ = i$2 + 1;
            } else {
                return;
            }
        }
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

    private void sendSyncAlarmMessage() {
        if (Log.isLoggable("SyncManager", 2)) {
            Log.v("SyncManager", "sending MESSAGE_SYNC_ALARM");
        }
        this.mSyncHandler.sendEmptyMessage(2);
    }

    private void sendCheckAlarmsMessage() {
        if (Log.isLoggable("SyncManager", 2)) {
            Log.v("SyncManager", "sending MESSAGE_CHECK_ALARMS");
        }
        this.mSyncHandler.removeMessages(3);
        this.mSyncHandler.sendEmptyMessage(3);
    }

    private void sendSyncFinishedOrCanceledMessage(ActiveSyncContext syncContext, SyncResult syncResult) {
        if (Log.isLoggable("SyncManager", 2)) {
            Log.v("SyncManager", "sending MESSAGE_SYNC_FINISHED");
        }
        Message msg = this.mSyncHandler.obtainMessage();
        msg.what = 1;
        msg.obj = new SyncHandlerMessagePayload(syncContext, syncResult);
        this.mSyncHandler.sendMessage(msg);
    }

    private void sendCancelSyncsMessage(SyncStorageEngine.EndPoint info, Bundle extras) {
        if (Log.isLoggable("SyncManager", 2)) {
            Log.v("SyncManager", "sending MESSAGE_CANCEL");
        }
        Message msg = this.mSyncHandler.obtainMessage();
        msg.what = 6;
        msg.setData(extras);
        msg.obj = info;
        this.mSyncHandler.sendMessage(msg);
    }

    private void postSyncExpiryMessage(ActiveSyncContext activeSyncContext) {
        if (Log.isLoggable("SyncManager", 2)) {
            Log.v("SyncManager", "posting MESSAGE_SYNC_EXPIRED in 1800s");
        }
        Message msg = this.mSyncHandler.obtainMessage();
        msg.what = 7;
        msg.obj = activeSyncContext;
        this.mSyncHandler.sendMessageDelayed(msg, ACTIVE_SYNC_TIMEOUT_MILLIS);
    }

    private void removeSyncExpiryMessage(ActiveSyncContext activeSyncContext) {
        if (Log.isLoggable("SyncManager", 2)) {
            Log.v("SyncManager", "removing all MESSAGE_SYNC_EXPIRED for " + activeSyncContext.toString());
        }
        this.mSyncHandler.removeMessages(7, activeSyncContext);
    }

    class SyncHandlerMessagePayload {
        public final ActiveSyncContext activeSyncContext;
        public final SyncResult syncResult;

        SyncHandlerMessagePayload(ActiveSyncContext syncContext, SyncResult syncResult) {
            this.activeSyncContext = syncContext;
            this.syncResult = syncResult;
        }
    }

    class SyncAlarmIntentReceiver extends BroadcastReceiver {
        SyncAlarmIntentReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            SyncManager.this.mHandleAlarmWakeLock.acquire();
            SyncManager.this.sendSyncAlarmMessage();
        }
    }

    private void clearBackoffSetting(SyncOperation op) {
        this.mSyncStorageEngine.setBackoff(op.target, -1L, -1L);
        synchronized (this.mSyncQueue) {
            this.mSyncQueue.onBackoffChanged(op.target, 0L);
        }
    }

    private void increaseBackoffSetting(SyncOperation op) {
        long now = SystemClock.elapsedRealtime();
        Pair<Long, Long> previousSettings = this.mSyncStorageEngine.getBackoff(op.target);
        long newDelayInMs = -1;
        if (previousSettings != null) {
            if (now < ((Long) previousSettings.first).longValue()) {
                if (Log.isLoggable("SyncManager", 2)) {
                    Log.v("SyncManager", "Still in backoff, do not increase it. Remaining: " + ((((Long) previousSettings.first).longValue() - now) / 1000) + " seconds.");
                    return;
                }
                return;
            }
            newDelayInMs = ((Long) previousSettings.second).longValue() * 2;
        }
        if (newDelayInMs <= 0) {
            newDelayInMs = jitterize(30000L, 33000L);
        }
        long maxSyncRetryTimeInSeconds = Settings.Global.getLong(this.mContext.getContentResolver(), "sync_max_retry_delay_in_seconds", DEFAULT_MAX_SYNC_RETRY_TIME_IN_SECONDS);
        if (newDelayInMs > 1000 * maxSyncRetryTimeInSeconds) {
            newDelayInMs = maxSyncRetryTimeInSeconds * 1000;
        }
        long backoff = now + newDelayInMs;
        this.mSyncStorageEngine.setBackoff(op.target, backoff, newDelayInMs);
        op.backoff = backoff;
        op.updateEffectiveRunTime();
        synchronized (this.mSyncQueue) {
            this.mSyncQueue.onBackoffChanged(op.target, backoff);
        }
    }

    private void setDelayUntilTime(SyncOperation op, long delayUntilSeconds) {
        long newDelayUntilTime;
        long delayUntil = delayUntilSeconds * 1000;
        long absoluteNow = System.currentTimeMillis();
        if (delayUntil > absoluteNow) {
            newDelayUntilTime = SystemClock.elapsedRealtime() + (delayUntil - absoluteNow);
        } else {
            newDelayUntilTime = 0;
        }
        this.mSyncStorageEngine.setDelayUntilTime(op.target, newDelayUntilTime);
        synchronized (this.mSyncQueue) {
            this.mSyncQueue.onDelayUntilTimeChanged(op.target, newDelayUntilTime);
        }
    }

    public void cancelActiveSync(SyncStorageEngine.EndPoint info, Bundle extras) {
        sendCancelSyncsMessage(info, extras);
    }

    public void scheduleSyncOperation(SyncOperation syncOperation) {
        boolean queueChanged;
        synchronized (this.mSyncQueue) {
            queueChanged = this.mSyncQueue.add(syncOperation);
        }
        if (queueChanged) {
            if (Log.isLoggable("SyncManager", 2)) {
                Log.v("SyncManager", "scheduleSyncOperation: enqueued " + syncOperation);
            }
            sendCheckAlarmsMessage();
        } else if (Log.isLoggable("SyncManager", 2)) {
            Log.v("SyncManager", "scheduleSyncOperation: dropping duplicate sync operation " + syncOperation);
        }
    }

    public void clearScheduledSyncOperations(SyncStorageEngine.EndPoint info) {
        synchronized (this.mSyncQueue) {
            this.mSyncQueue.remove(info, null);
        }
        this.mSyncStorageEngine.setBackoff(info, -1L, -1L);
    }

    public void cancelScheduledSyncOperation(SyncStorageEngine.EndPoint info, Bundle extras) {
        synchronized (this.mSyncQueue) {
            this.mSyncQueue.remove(info, extras);
        }
        if (!this.mSyncStorageEngine.isSyncPending(info)) {
            this.mSyncStorageEngine.setBackoff(info, -1L, -1L);
        }
    }

    void maybeRescheduleSync(SyncResult syncResult, SyncOperation operation) {
        boolean isLoggable = Log.isLoggable("SyncManager", 3);
        if (isLoggable) {
            Log.d("SyncManager", "encountered error(s) during the sync: " + syncResult + ", " + operation);
        }
        SyncOperation operation2 = new SyncOperation(operation, 0L);
        if (operation2.extras.getBoolean("ignore_backoff", false)) {
            operation2.extras.remove("ignore_backoff");
        }
        if (operation2.extras.getBoolean("do_not_retry", false)) {
            if (isLoggable) {
                Log.d("SyncManager", "not retrying sync operation because SYNC_EXTRAS_DO_NOT_RETRY was specified " + operation2);
                return;
            }
            return;
        }
        if (operation2.extras.getBoolean("upload", false) && !syncResult.syncAlreadyInProgress) {
            operation2.extras.remove("upload");
            if (isLoggable) {
                Log.d("SyncManager", "retrying sync operation as a two-way sync because an upload-only sync encountered an error: " + operation2);
            }
            scheduleSyncOperation(operation2);
            return;
        }
        if (syncResult.tooManyRetries) {
            if (isLoggable) {
                Log.d("SyncManager", "not retrying sync operation because it retried too many times: " + operation2);
                return;
            }
            return;
        }
        if (syncResult.madeSomeProgress()) {
            if (isLoggable) {
                Log.d("SyncManager", "retrying sync operation because even though it had an error it achieved some success");
            }
            scheduleSyncOperation(operation2);
        } else if (syncResult.syncAlreadyInProgress) {
            if (isLoggable) {
                Log.d("SyncManager", "retrying sync operation that failed because there was already a sync in progress: " + operation2);
            }
            scheduleSyncOperation(new SyncOperation(operation2, 10000L));
        } else {
            if (syncResult.hasSoftError()) {
                if (isLoggable) {
                    Log.d("SyncManager", "retrying sync operation because it encountered a soft error: " + operation2);
                }
                scheduleSyncOperation(operation2);
                return;
            }
            Log.d("SyncManager", "not retrying sync operation because the error is a hard error: " + operation2);
        }
    }

    private void onUserStarting(int userId) {
        AccountManagerService.getSingleton().validateAccounts(userId);
        this.mSyncAdapters.invalidateCache(userId);
        updateRunningAccounts();
        synchronized (this.mSyncQueue) {
            this.mSyncQueue.addPendingOperations(userId);
        }
        Account[] accounts = AccountManagerService.getSingleton().getAccounts(userId);
        for (Account account : accounts) {
            scheduleSync(account, userId, -8, null, null, 0L, 0L, true);
        }
        sendCheckAlarmsMessage();
    }

    private void onUserStopping(int userId) {
        updateRunningAccounts();
        cancelActiveSync(new SyncStorageEngine.EndPoint(null, null, userId), null);
    }

    private void onUserRemoved(int userId) {
        updateRunningAccounts();
        this.mSyncStorageEngine.doDatabaseCleanup(new Account[0], userId);
        synchronized (this.mSyncQueue) {
            this.mSyncQueue.removeUserLocked(userId);
        }
    }

    class ActiveSyncContext extends ISyncContext.Stub implements ServiceConnection, IBinder.DeathRecipient {
        boolean mBound;
        String mEventName;
        final long mHistoryRowId;
        final int mSyncAdapterUid;
        SyncInfo mSyncInfo;
        final SyncOperation mSyncOperation;
        final PowerManager.WakeLock mSyncWakeLock;
        boolean mIsLinkedToDeath = false;
        ISyncAdapter mSyncAdapter = null;
        ISyncServiceAdapter mSyncServiceAdapter = null;
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
                Log.v("SyncManager", "onFinished: " + this);
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
            intent.putExtra("android.intent.extra.client_label", R.string.keyguard_accessibility_unlock_area_collapsed);
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
        dumpSyncState(ipw);
        dumpSyncHistory(ipw);
        dumpSyncAdapters(ipw);
    }

    static String formatTime(long time) {
        Time tobj = new Time();
        tobj.set(time);
        return tobj.format("%Y-%m-%d %H:%M:%S");
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
        pw.print("offset: ");
        pw.print(DateUtils.formatElapsedTime(this.mSyncRandomOffsetMillis / 1000));
        pw.println(" (HH:MM:SS)");
        pw.print("uptime: ");
        pw.print(DateUtils.formatElapsedTime(now / 1000));
        pw.println(" (HH:MM:SS)");
        pw.print("time spent syncing: ");
        pw.print(DateUtils.formatElapsedTime(this.mSyncHandler.mSyncTimeTracker.timeSpentSyncing() / 1000));
        pw.print(" (HH:MM:SS), sync ");
        pw.print(this.mSyncHandler.mSyncTimeTracker.mLastWasSyncing ? "" : "not ");
        pw.println("in progress");
        if (this.mSyncHandler.mAlarmScheduleTime != null) {
            pw.print("next alarm time: ");
            pw.print(this.mSyncHandler.mAlarmScheduleTime);
            pw.print(" (");
            pw.print(DateUtils.formatElapsedTime((this.mSyncHandler.mAlarmScheduleTime.longValue() - now) / 1000));
            pw.println(" (HH:MM:SS) from now)");
        } else {
            pw.println("no alarm is scheduled (there had better not be any pending syncs)");
        }
        pw.print("notification info: ");
        StringBuilder sb = new StringBuilder();
        this.mSyncHandler.mSyncNotificationInfo.toString(sb);
        pw.println(sb.toString());
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
        synchronized (this.mSyncQueue) {
            sb.setLength(0);
            this.mSyncQueue.dump(sb);
            getSyncStorageEngine().dumpPendingOperations(sb);
        }
        pw.println();
        pw.print(sb.toString());
        pw.println();
        pw.println("Sync Status");
        for (AccountAndUser account : accounts) {
            pw.printf("Account %s u%d %s\n", account.account.name, Integer.valueOf(account.userId), account.account.type);
            pw.println("=======================================================================");
            PrintTable table = new PrintTable(13);
            table.set(0, 0, "Authority", "Syncable", "Enabled", "Delay", "Loc", "Poll", "Per", "Serv", "User", "Tot", "Time", "Last Sync", "Periodic");
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
                    for (int i = 0; i < settings.periodicSyncs.size(); i++) {
                        PeriodicSync sync = settings.periodicSyncs.get(i);
                        String period = String.format("[p:%d s, f: %d s]", Long.valueOf(sync.period), Long.valueOf(sync.flexTime));
                        String extras = sync.extras.size() > 0 ? sync.extras.toString() : "Bundle[]";
                        String next = "Next sync: " + formatTime(status.getPeriodicSyncTime(i) + (sync.period * 1000));
                        table.set((i * 2) + row, 12, period + " " + extras);
                        table.set((i * 2) + row + 1, 12, next);
                    }
                    int row1 = row;
                    if (settings.delayUntil > now) {
                        int row12 = row1 + 1;
                        table.set(row1, 12, "D: " + ((settings.delayUntil - now) / 1000));
                        if (settings.backoffTime > now) {
                            int row13 = row12 + 1;
                            table.set(row12, 12, "B: " + ((settings.backoffTime - now) / 1000));
                            row12 = row13 + 1;
                            table.set(row13, 12, Long.valueOf(settings.backoffDelay / 1000));
                        }
                        row1 = row12;
                    }
                    if (status.lastSuccessTime != 0) {
                        int row14 = row1 + 1;
                        table.set(row1, 11, SyncStorageEngine.SOURCES[status.lastSuccessSource] + " SUCCESS");
                        row1 = row14 + 1;
                        table.set(row14, 11, formatTime(status.lastSuccessTime));
                    }
                    if (status.lastFailureTime != 0) {
                        int row15 = row1 + 1;
                        table.set(row1, 11, SyncStorageEngine.SOURCES[status.lastFailureSource] + " FAILURE");
                        int row16 = row15 + 1;
                        table.set(row15, 11, formatTime(status.lastFailureTime));
                        int i2 = row16 + 1;
                        table.set(row16, 11, status.lastFailureMesg);
                    }
                }
            }
            table.writeTo(pw);
        }
    }

    private String getLastFailureMessage(int code) {
        switch (code) {
            case 1:
                return "sync already in progress";
            case 2:
                return "authentication error";
            case 3:
                return "I/O error";
            case 4:
                return "parse error";
            case 5:
                return "conflict error";
            case 6:
                return "too many deletions error";
            case 7:
                return "too many retries error";
            case 8:
                return "internal error";
            default:
                return "unknown";
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
        if (items != null && items.size() > 0) {
            Map<String, AuthoritySyncStats> authorityMap = Maps.newHashMap();
            long totalElapsedTime = 0;
            long totalTimes = 0;
            int N = items.size();
            int maxAuthority = 0;
            int maxAccount = 0;
            for (SyncStorageEngine.SyncHistoryItem item : items) {
                SyncStorageEngine.AuthorityInfo authorityInfo = this.mSyncStorageEngine.getAuthority(item.authorityId);
                if (authorityInfo != null) {
                    if (authorityInfo.target.target_provider) {
                        authorityName3 = authorityInfo.target.provider;
                        accountKey3 = authorityInfo.target.account.name + "/" + authorityInfo.target.account.type + " u" + authorityInfo.target.userId;
                    } else if (authorityInfo.target.target_service) {
                        authorityName3 = authorityInfo.target.service.getPackageName() + "/" + authorityInfo.target.service.getClassName() + " u" + authorityInfo.target.userId;
                        accountKey3 = "no account";
                    } else {
                        authorityName3 = "Unknown";
                        accountKey3 = "Unknown";
                    }
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
                    authoritySyncStats = new AuthoritySyncStats(authorityName3);
                    authorityMap.put(authorityName3, authoritySyncStats);
                }
                authoritySyncStats.elapsedTime += elapsedTime;
                authoritySyncStats.times++;
                Map<String, AccountSyncStats> accountMap = authoritySyncStats.accountMap;
                AccountSyncStats accountSyncStats = accountMap.get(accountKey3);
                if (accountSyncStats == null) {
                    accountSyncStats = new AccountSyncStats(accountKey3);
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
                    if (authorityInfo2.target.target_provider) {
                        authorityName2 = authorityInfo2.target.provider;
                        accountKey2 = authorityInfo2.target.account.name + "/" + authorityInfo2.target.account.type + " u" + authorityInfo2.target.userId;
                    } else if (authorityInfo2.target.target_service) {
                        authorityName2 = authorityInfo2.target.service.getPackageName() + "/" + authorityInfo2.target.service.getClassName() + " u" + authorityInfo2.target.userId;
                        accountKey2 = "none";
                    } else {
                        authorityName2 = "Unknown";
                        accountKey2 = "Unknown";
                    }
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
                        if (authorityInfo3.target.target_provider) {
                            authorityName = authorityInfo3.target.provider;
                            accountKey = authorityInfo3.target.account.name + "/" + authorityInfo3.target.account.type + " u" + authorityInfo3.target.userId;
                        } else if (authorityInfo3.target.target_service) {
                            authorityName = authorityInfo3.target.service.getPackageName() + "/" + authorityInfo3.target.service.getClassName() + " u" + authorityInfo3.target.userId;
                            accountKey = "none";
                        } else {
                            authorityName = "Unknown";
                            accountKey = "Unknown";
                        }
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
    }

    private void dumpDayStatistics(PrintWriter pw) {
        int delta;
        SyncStorageEngine.DayStats[] dses = this.mSyncStorageEngine.getDayStatistics();
        if (dses != null && dses[0] != null) {
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
    }

    private void dumpSyncAdapters(IndentingPrintWriter pw) {
        pw.println();
        List<UserInfo> users = getAllUsers();
        if (users != null) {
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
    }

    private static class AuthoritySyncStats {
        Map<String, AccountSyncStats> accountMap;
        long elapsedTime;
        String name;
        int times;

        private AuthoritySyncStats(String name) {
            this.accountMap = Maps.newHashMap();
            this.name = name;
        }
    }

    private static class AccountSyncStats {
        long elapsedTime;
        String name;
        int times;

        private AccountSyncStats(String name) {
            this.name = name;
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
            boolean isSyncInProgress = !SyncManager.this.mActiveSyncContexts.isEmpty();
            if (isSyncInProgress != this.mLastWasSyncing) {
                long now = SystemClock.elapsedRealtime();
                if (isSyncInProgress) {
                    this.mWhenSyncStarted = now;
                } else {
                    this.mTimeSpentSyncing += now - this.mWhenSyncStarted;
                }
                this.mLastWasSyncing = isSyncInProgress;
            }
        }

        public synchronized long timeSpentSyncing() {
            long j;
            if (this.mLastWasSyncing) {
                long now = SystemClock.elapsedRealtime();
                j = this.mTimeSpentSyncing + (now - this.mWhenSyncStarted);
            } else {
                j = this.mTimeSpentSyncing;
            }
            return j;
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
        private static final int MESSAGE_CANCEL = 6;
        private static final int MESSAGE_CHECK_ALARMS = 3;
        private static final int MESSAGE_SERVICE_CONNECTED = 4;
        private static final int MESSAGE_SERVICE_DISCONNECTED = 5;
        private static final int MESSAGE_SYNC_ALARM = 2;
        private static final int MESSAGE_SYNC_EXPIRED = 7;
        private static final int MESSAGE_SYNC_FINISHED = 1;
        private Long mAlarmScheduleTime;
        private List<Message> mBootQueue;
        public final SyncNotificationInfo mSyncNotificationInfo;
        public final SyncTimeTracker mSyncTimeTracker;
        private final HashMap<String, PowerManager.WakeLock> mWakeLocks;

        public void onBootCompleted() {
            if (Log.isLoggable("SyncManager", 2)) {
                Log.v("SyncManager", "Boot completed, clearing boot queue.");
            }
            SyncManager.this.doDatabaseCleanup();
            synchronized (this) {
                for (Message message : this.mBootQueue) {
                    sendMessage(message);
                }
                this.mBootQueue = null;
                SyncManager.this.mBootCompleted = true;
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

        private boolean tryEnqueueMessageUntilReadyToRun(Message msg) {
            boolean z;
            synchronized (this) {
                if (SyncManager.this.mBootCompleted) {
                    z = false;
                } else {
                    this.mBootQueue.add(Message.obtain(msg));
                    z = true;
                }
            }
            return z;
        }

        class SyncNotificationInfo {
            public boolean isActive = false;
            public Long startTime = null;

            SyncNotificationInfo() {
            }

            public void toString(StringBuilder sb) {
                sb.append("isActive ").append(this.isActive).append(", startTime ").append(this.startTime);
            }

            public String toString() {
                StringBuilder sb = new StringBuilder();
                toString(sb);
                return sb.toString();
            }
        }

        public SyncHandler(Looper looper) {
            super(looper);
            this.mSyncNotificationInfo = new SyncNotificationInfo();
            this.mAlarmScheduleTime = null;
            this.mSyncTimeTracker = new SyncTimeTracker();
            this.mWakeLocks = Maps.newHashMap();
            this.mBootQueue = new ArrayList();
        }

        @Override
        public void handleMessage(Message msg) {
            if (!tryEnqueueMessageUntilReadyToRun(msg)) {
                long earliestFuturePollTime = JobStatus.NO_LATEST_RUNTIME;
                long nextPendingSyncTime = JobStatus.NO_LATEST_RUNTIME;
                try {
                    SyncManager.this.mDataConnectionIsConnected = SyncManager.this.readDataConnectionState();
                    SyncManager.this.mSyncManagerWakeLock.acquire();
                    earliestFuturePollTime = scheduleReadyPeriodicSyncs();
                    switch (msg.what) {
                        case 1:
                            if (Log.isLoggable("SyncManager", 2)) {
                                Log.v("SyncManager", "handleSyncHandlerMessage: MESSAGE_SYNC_FINISHED");
                            }
                            SyncHandlerMessagePayload payload = (SyncHandlerMessagePayload) msg.obj;
                            if (!SyncManager.this.isSyncStillActive(payload.activeSyncContext)) {
                                Log.d("SyncManager", "handleSyncHandlerMessage: dropping since the sync is no longer active: " + payload.activeSyncContext);
                            } else {
                                runSyncFinishedOrCanceledLocked(payload.syncResult, payload.activeSyncContext);
                                nextPendingSyncTime = maybeStartNextSyncLocked();
                            }
                            return;
                        case 2:
                            boolean isLoggable = Log.isLoggable("SyncManager", 2);
                            if (isLoggable) {
                                Log.v("SyncManager", "handleSyncHandlerMessage: MESSAGE_SYNC_ALARM");
                            }
                            this.mAlarmScheduleTime = null;
                            try {
                                nextPendingSyncTime = maybeStartNextSyncLocked();
                                SyncManager.this.mHandleAlarmWakeLock.release();
                                return;
                            } catch (Throwable th) {
                                SyncManager.this.mHandleAlarmWakeLock.release();
                                throw th;
                            }
                        case 3:
                            if (Log.isLoggable("SyncManager", 2)) {
                                Log.v("SyncManager", "handleSyncHandlerMessage: MESSAGE_CHECK_ALARMS");
                            }
                            nextPendingSyncTime = maybeStartNextSyncLocked();
                            return;
                        case 4:
                            ServiceConnectionData msgData = (ServiceConnectionData) msg.obj;
                            if (Log.isLoggable("SyncManager", 2)) {
                                Log.d("SyncManager", "handleSyncHandlerMessage: MESSAGE_SERVICE_CONNECTED: " + msgData.activeSyncContext);
                            }
                            if (SyncManager.this.isSyncStillActive(msgData.activeSyncContext)) {
                                runBoundToAdapter(msgData.activeSyncContext, msgData.adapter);
                            }
                            return;
                        case 5:
                            ActiveSyncContext currentSyncContext = ((ServiceConnectionData) msg.obj).activeSyncContext;
                            if (Log.isLoggable("SyncManager", 2)) {
                                Log.d("SyncManager", "handleSyncHandlerMessage: MESSAGE_SERVICE_DISCONNECTED: " + currentSyncContext);
                            }
                            if (SyncManager.this.isSyncStillActive(currentSyncContext)) {
                                try {
                                    if (currentSyncContext.mSyncAdapter != null) {
                                        currentSyncContext.mSyncAdapter.cancelSync(currentSyncContext);
                                    } else if (currentSyncContext.mSyncServiceAdapter != null) {
                                        currentSyncContext.mSyncServiceAdapter.cancelSync(currentSyncContext);
                                    }
                                    break;
                                } catch (RemoteException e) {
                                }
                                SyncResult syncResult = new SyncResult();
                                syncResult.stats.numIoExceptions++;
                                runSyncFinishedOrCanceledLocked(syncResult, currentSyncContext);
                                nextPendingSyncTime = maybeStartNextSyncLocked();
                            }
                            return;
                        case 6:
                            SyncStorageEngine.EndPoint payload2 = (SyncStorageEngine.EndPoint) msg.obj;
                            Bundle extras = msg.peekData();
                            if (Log.isLoggable("SyncManager", 3)) {
                                Log.d("SyncManager", "handleSyncHandlerMessage: MESSAGE_SERVICE_CANCEL: " + payload2 + " bundle: " + extras);
                            }
                            cancelActiveSyncLocked(payload2, extras);
                            nextPendingSyncTime = maybeStartNextSyncLocked();
                            return;
                        case 7:
                            ActiveSyncContext expiredContext = (ActiveSyncContext) msg.obj;
                            if (Log.isLoggable("SyncManager", 3)) {
                                Log.d("SyncManager", "handleSyncHandlerMessage: MESSAGE_SYNC_EXPIRED: expiring " + expiredContext);
                            }
                            SyncManager.this.cancelActiveSync(expiredContext.mSyncOperation.target, expiredContext.mSyncOperation.extras);
                            nextPendingSyncTime = maybeStartNextSyncLocked();
                            return;
                        default:
                            return;
                    }
                } finally {
                    manageSyncNotificationLocked();
                    manageSyncAlarmLocked(earliestFuturePollTime, nextPendingSyncTime);
                    this.mSyncTimeTracker.update();
                    SyncManager.this.mSyncManagerWakeLock.release();
                }
            }
        }

        private boolean isDispatchable(SyncStorageEngine.EndPoint target) {
            boolean isLoggable = Log.isLoggable("SyncManager", 2);
            if (target.target_provider) {
                AccountAndUser[] accounts = SyncManager.this.mRunningAccounts;
                if (!SyncManager.this.containsAccountAndUser(accounts, target.account, target.userId)) {
                    return false;
                }
                if (!SyncManager.this.mSyncStorageEngine.getMasterSyncAutomatically(target.userId) || !SyncManager.this.mSyncStorageEngine.getSyncAutomatically(target.account, target.userId, target.provider)) {
                    if (!isLoggable) {
                        return false;
                    }
                    Log.v("SyncManager", "    Not scheduling periodic operation: sync turned off.");
                    return false;
                }
                if (SyncManager.this.getIsSyncable(target.account, target.userId, target.provider) == 0) {
                    if (!isLoggable) {
                        return false;
                    }
                    Log.v("SyncManager", "    Not scheduling periodic operation: isSyncable == 0.");
                    return false;
                }
            } else if (target.target_service && SyncManager.this.mSyncStorageEngine.getIsTargetServiceActive(target.service, target.userId)) {
                if (!isLoggable) {
                    return false;
                }
                Log.v("SyncManager", "   Not scheduling periodic operation: isEnabled == 0.");
                return false;
            }
            return true;
        }

        private long scheduleReadyPeriodicSyncs() {
            long shiftedNowAbsolute;
            long shiftedLastPollTimeAbsolute;
            long nextPollTimeAbsolute;
            boolean isLoggable = Log.isLoggable("SyncManager", 2);
            if (isLoggable) {
                Log.v("SyncManager", "scheduleReadyPeriodicSyncs");
            }
            long earliestFuturePollTime = JobStatus.NO_LATEST_RUNTIME;
            long nowAbsolute = System.currentTimeMillis();
            if (0 < nowAbsolute - ((long) SyncManager.this.mSyncRandomOffsetMillis)) {
                shiftedNowAbsolute = nowAbsolute - ((long) SyncManager.this.mSyncRandomOffsetMillis);
            } else {
                shiftedNowAbsolute = 0;
            }
            ArrayList<Pair<SyncStorageEngine.AuthorityInfo, SyncStatusInfo>> infos = SyncManager.this.mSyncStorageEngine.getCopyOfAllAuthoritiesWithSyncStatus();
            for (Pair<SyncStorageEngine.AuthorityInfo, SyncStatusInfo> info : infos) {
                SyncStorageEngine.AuthorityInfo authorityInfo = (SyncStorageEngine.AuthorityInfo) info.first;
                SyncStatusInfo status = (SyncStatusInfo) info.second;
                if (TextUtils.isEmpty(authorityInfo.target.provider)) {
                    Log.e("SyncManager", "Got an empty provider string. Skipping: " + authorityInfo.target.provider);
                } else if (isDispatchable(authorityInfo.target)) {
                    int N = authorityInfo.periodicSyncs.size();
                    for (int i = 0; i < N; i++) {
                        PeriodicSync sync = authorityInfo.periodicSyncs.get(i);
                        Bundle extras = sync.extras;
                        Long periodInMillis = Long.valueOf(sync.period * 1000);
                        Long flexInMillis = Long.valueOf(sync.flexTime * 1000);
                        if (periodInMillis.longValue() > 0) {
                            long lastPollTimeAbsolute = status.getPeriodicSyncTime(i);
                            if (0 < lastPollTimeAbsolute - ((long) SyncManager.this.mSyncRandomOffsetMillis)) {
                                shiftedLastPollTimeAbsolute = lastPollTimeAbsolute - ((long) SyncManager.this.mSyncRandomOffsetMillis);
                            } else {
                                shiftedLastPollTimeAbsolute = 0;
                            }
                            long remainingMillis = periodInMillis.longValue() - (shiftedNowAbsolute % periodInMillis.longValue());
                            long timeSinceLastRunMillis = nowAbsolute - lastPollTimeAbsolute;
                            boolean runEarly = remainingMillis <= flexInMillis.longValue() && timeSinceLastRunMillis > periodInMillis.longValue() - flexInMillis.longValue();
                            if (isLoggable) {
                                Log.v("SyncManager", "sync: " + i + " for " + authorityInfo.target + ". period: " + periodInMillis + " flex: " + flexInMillis + " remaining: " + remainingMillis + " time_since_last: " + timeSinceLastRunMillis + " last poll absol: " + lastPollTimeAbsolute + " last poll shifed: " + shiftedLastPollTimeAbsolute + " shifted now: " + shiftedNowAbsolute + " run_early: " + runEarly);
                            }
                            if (remainingMillis == periodInMillis.longValue() || lastPollTimeAbsolute > nowAbsolute || timeSinceLastRunMillis >= periodInMillis.longValue() || runEarly) {
                                SyncStorageEngine.EndPoint target = authorityInfo.target;
                                Pair<Long, Long> backoff = SyncManager.this.mSyncStorageEngine.getBackoff(target);
                                SyncManager.this.mSyncStorageEngine.setPeriodicSyncTime(authorityInfo.ident, authorityInfo.periodicSyncs.get(i), nowAbsolute);
                                if (target.target_provider) {
                                    RegisteredServicesCache.ServiceInfo<SyncAdapterType> syncAdapterInfo = SyncManager.this.mSyncAdapters.getServiceInfo(SyncAdapterType.newKey(target.provider, target.account.type), target.userId);
                                    if (syncAdapterInfo != null) {
                                        SyncManager.this.scheduleSyncOperation(new SyncOperation(target.account, target.userId, -4, 4, target.provider, extras, 0L, 0L, backoff != null ? ((Long) backoff.first).longValue() : 0L, SyncManager.this.mSyncStorageEngine.getDelayUntilTime(target), ((SyncAdapterType) syncAdapterInfo.type).allowParallelSyncs()));
                                    }
                                } else if (target.target_service) {
                                    SyncManager.this.scheduleSyncOperation(new SyncOperation(target.service, target.userId, -4, 4, extras, 0L, 0L, backoff != null ? ((Long) backoff.first).longValue() : 0L, SyncManager.this.mSyncStorageEngine.getDelayUntilTime(target)));
                                }
                                if (!runEarly) {
                                    nextPollTimeAbsolute = periodInMillis.longValue() + nowAbsolute + remainingMillis;
                                } else {
                                    nextPollTimeAbsolute = nowAbsolute + remainingMillis;
                                }
                                if (nextPollTimeAbsolute >= earliestFuturePollTime) {
                                    earliestFuturePollTime = nextPollTimeAbsolute;
                                }
                            } else {
                                if (!runEarly) {
                                }
                                if (nextPollTimeAbsolute >= earliestFuturePollTime) {
                                }
                            }
                        }
                    }
                }
            }
            if (earliestFuturePollTime == JobStatus.NO_LATEST_RUNTIME) {
                return JobStatus.NO_LATEST_RUNTIME;
            }
            return (earliestFuturePollTime < nowAbsolute ? 0L : earliestFuturePollTime - nowAbsolute) + SystemClock.elapsedRealtime();
        }

        private long maybeStartNextSyncLocked() {
            Iterator<SyncOperation> operationIterator;
            boolean roomAvailable;
            boolean isLoggable = Log.isLoggable("SyncManager", 2);
            if (isLoggable) {
                Log.v("SyncManager", "maybeStartNextSync");
            }
            if (SyncManager.this.mDataConnectionIsConnected) {
                if (!SyncManager.this.mStorageIsLow) {
                    if (SyncManager.this.mRunningAccounts == SyncManager.INITIAL_ACCOUNTS_ARRAY) {
                        if (isLoggable) {
                            Log.v("SyncManager", "maybeStartNextSync: accounts not known, skipping");
                        }
                        return JobStatus.NO_LATEST_RUNTIME;
                    }
                    long now = SystemClock.elapsedRealtime();
                    long nextReadyToRunTime = JobStatus.NO_LATEST_RUNTIME;
                    ArrayList<SyncOperation> operations = new ArrayList<>();
                    synchronized (SyncManager.this.mSyncQueue) {
                        if (!isLoggable) {
                            operationIterator = SyncManager.this.mSyncQueue.getOperations().iterator();
                            ActivityManager activityManager = (ActivityManager) SyncManager.this.mContext.getSystemService("activity");
                            Set<Integer> removedUsers = Sets.newHashSet();
                            while (operationIterator.hasNext()) {
                            }
                            while (r12.hasNext()) {
                            }
                        } else {
                            Log.v("SyncManager", "build the operation array, syncQueue size is " + SyncManager.this.mSyncQueue.getOperations().size());
                            operationIterator = SyncManager.this.mSyncQueue.getOperations().iterator();
                            ActivityManager activityManager2 = (ActivityManager) SyncManager.this.mContext.getSystemService("activity");
                            Set<Integer> removedUsers2 = Sets.newHashSet();
                            while (operationIterator.hasNext()) {
                                SyncOperation op = operationIterator.next();
                                if (!activityManager2.isUserRunning(op.target.userId)) {
                                    UserInfo userInfo = SyncManager.this.mUserManager.getUserInfo(op.target.userId);
                                    if (userInfo == null) {
                                        removedUsers2.add(Integer.valueOf(op.target.userId));
                                    }
                                    if (isLoggable) {
                                        Log.v("SyncManager", "    Dropping all sync operations for + " + op.target.userId + ": user not running.");
                                    }
                                } else if (!isOperationValidLocked(op)) {
                                    operationIterator.remove();
                                    SyncManager.this.mSyncStorageEngine.deleteFromPending(op.pendingOperation);
                                } else if (op.effectiveRunTime - op.flexTime > now) {
                                    if (nextReadyToRunTime > op.effectiveRunTime) {
                                        nextReadyToRunTime = op.effectiveRunTime;
                                    }
                                    if (isLoggable) {
                                        Log.v("SyncManager", "    Not running sync operation: Sync too far in future.effective: " + op.effectiveRunTime + " flex: " + op.flexTime + " now: " + now);
                                    }
                                } else {
                                    operations.add(op);
                                }
                            }
                            for (Integer user : removedUsers2) {
                                if (SyncManager.this.mUserManager.getUserInfo(user.intValue()) == null) {
                                    SyncManager.this.onUserRemoved(user.intValue());
                                }
                            }
                        }
                    }
                    if (isLoggable) {
                        Log.v("SyncManager", "sort the candidate operations, size " + operations.size());
                    }
                    Collections.sort(operations);
                    if (isLoggable) {
                        Log.v("SyncManager", "dispatch all ready sync operations");
                    }
                    int N = operations.size();
                    for (int i = 0; i < N; i++) {
                        SyncOperation candidate = operations.get(i);
                        boolean candidateIsInitialization = candidate.isInitialization();
                        int numInit = 0;
                        int numRegular = 0;
                        ActiveSyncContext conflict = null;
                        ActiveSyncContext longRunning = null;
                        ActiveSyncContext toReschedule = null;
                        ActiveSyncContext oldestNonExpeditedRegular = null;
                        for (ActiveSyncContext activeSyncContext : SyncManager.this.mActiveSyncContexts) {
                            SyncOperation activeOp = activeSyncContext.mSyncOperation;
                            if (activeOp.isInitialization()) {
                                numInit++;
                            } else {
                                numRegular++;
                                if (!activeOp.isExpedited() && (oldestNonExpeditedRegular == null || oldestNonExpeditedRegular.mStartTime > activeSyncContext.mStartTime)) {
                                    oldestNonExpeditedRegular = activeSyncContext;
                                }
                            }
                            if (activeOp.isConflict(candidate)) {
                                conflict = activeSyncContext;
                            } else if (candidateIsInitialization == activeOp.isInitialization() && activeSyncContext.mStartTime + SyncManager.MAX_TIME_PER_SYNC < now) {
                                longRunning = activeSyncContext;
                            }
                        }
                        if (isLoggable) {
                            Log.v("SyncManager", "candidate " + (i + 1) + " of " + N + ": " + candidate);
                            Log.v("SyncManager", "  numActiveInit=" + numInit + ", numActiveRegular=" + numRegular);
                            Log.v("SyncManager", "  longRunning: " + longRunning);
                            Log.v("SyncManager", "  conflict: " + conflict);
                            Log.v("SyncManager", "  oldestNonExpeditedRegular: " + oldestNonExpeditedRegular);
                        }
                        if (!candidateIsInitialization) {
                            roomAvailable = numRegular < SyncManager.MAX_SIMULTANEOUS_REGULAR_SYNCS;
                        } else {
                            roomAvailable = numInit < SyncManager.MAX_SIMULTANEOUS_INITIALIZATION_SYNCS;
                        }
                        if (conflict != null) {
                            if (candidateIsInitialization && !conflict.mSyncOperation.isInitialization() && numInit < SyncManager.MAX_SIMULTANEOUS_INITIALIZATION_SYNCS) {
                                toReschedule = conflict;
                                if (Log.isLoggable("SyncManager", 2)) {
                                    Log.v("SyncManager", "canceling and rescheduling sync since an initialization takes higher priority, " + conflict);
                                }
                            } else if (candidate.isExpedited() && !conflict.mSyncOperation.isExpedited() && candidateIsInitialization == conflict.mSyncOperation.isInitialization()) {
                                toReschedule = conflict;
                                if (Log.isLoggable("SyncManager", 2)) {
                                    Log.v("SyncManager", "canceling and rescheduling sync since an expedited takes higher priority, " + conflict);
                                }
                            }
                            if (toReschedule != null) {
                                runSyncFinishedOrCanceledLocked(null, toReschedule);
                                SyncManager.this.scheduleSyncOperation(toReschedule.mSyncOperation);
                            }
                            synchronized (SyncManager.this.mSyncQueue) {
                                SyncManager.this.mSyncQueue.remove(candidate);
                            }
                            dispatchSyncOperation(candidate);
                        } else if (!roomAvailable) {
                            if (candidate.isExpedited() && oldestNonExpeditedRegular != null && !candidateIsInitialization) {
                                toReschedule = oldestNonExpeditedRegular;
                                if (Log.isLoggable("SyncManager", 2)) {
                                    Log.v("SyncManager", "canceling and rescheduling sync since an expedited is ready to run, " + oldestNonExpeditedRegular);
                                }
                            } else if (longRunning != null && candidateIsInitialization == longRunning.mSyncOperation.isInitialization()) {
                                toReschedule = longRunning;
                                if (Log.isLoggable("SyncManager", 2)) {
                                    Log.v("SyncManager", "canceling and rescheduling sync since it ran roo long, " + longRunning);
                                }
                            }
                            if (toReschedule != null) {
                            }
                            synchronized (SyncManager.this.mSyncQueue) {
                            }
                        } else {
                            if (toReschedule != null) {
                            }
                            synchronized (SyncManager.this.mSyncQueue) {
                            }
                        }
                    }
                    return nextReadyToRunTime;
                }
                if (isLoggable) {
                    Log.v("SyncManager", "maybeStartNextSync: memory low, skipping");
                }
                return JobStatus.NO_LATEST_RUNTIME;
            }
            if (isLoggable) {
                Log.v("SyncManager", "maybeStartNextSync: no data connection, skipping");
            }
            return JobStatus.NO_LATEST_RUNTIME;
        }

        private boolean isOperationValidLocked(SyncOperation op) {
            int state;
            int targetUid;
            boolean isLoggable = Log.isLoggable("SyncManager", 2);
            SyncStorageEngine.EndPoint target = op.target;
            boolean syncEnabled = SyncManager.this.mSyncStorageEngine.getMasterSyncAutomatically(target.userId);
            if (target.target_provider) {
                AccountAndUser[] accounts = SyncManager.this.mRunningAccounts;
                if (!SyncManager.this.containsAccountAndUser(accounts, target.account, target.userId)) {
                    if (isLoggable) {
                        Log.v("SyncManager", "    Dropping sync operation: account doesn't exist.");
                    }
                    return false;
                }
                state = SyncManager.this.getIsSyncable(target.account, target.userId, target.provider);
                if (state == 0) {
                    if (isLoggable) {
                        Log.v("SyncManager", "    Dropping sync operation: isSyncable == 0.");
                    }
                    return false;
                }
                syncEnabled = syncEnabled && SyncManager.this.mSyncStorageEngine.getSyncAutomatically(target.account, target.userId, target.provider);
                RegisteredServicesCache.ServiceInfo<SyncAdapterType> syncAdapterInfo = SyncManager.this.mSyncAdapters.getServiceInfo(SyncAdapterType.newKey(target.provider, target.account.type), target.userId);
                if (syncAdapterInfo != null) {
                    targetUid = syncAdapterInfo.uid;
                } else {
                    if (isLoggable) {
                        Log.v("SyncManager", "    Dropping sync operation: No sync adapter registeredfor: " + target);
                    }
                    return false;
                }
            } else if (target.target_service) {
                state = SyncManager.this.mSyncStorageEngine.getIsTargetServiceActive(target.service, target.userId) ? 1 : 0;
                if (state != 0) {
                    try {
                        targetUid = SyncManager.this.mContext.getPackageManager().getServiceInfo(target.service, 0).applicationInfo.uid;
                    } catch (PackageManager.NameNotFoundException e) {
                        if (isLoggable) {
                            Log.v("SyncManager", "    Dropping sync operation: No service registered for: " + target.service);
                        }
                        return false;
                    }
                } else {
                    if (isLoggable) {
                        Log.v("SyncManager", "    Dropping sync operation: isActive == 0.");
                    }
                    return false;
                }
            } else {
                Log.e("SyncManager", "Unknown target for Sync Op: " + target);
                return false;
            }
            boolean ignoreSystemConfiguration = op.extras.getBoolean("ignore_settings", false) || state < 0;
            if (syncEnabled || ignoreSystemConfiguration) {
                NetworkInfo networkInfo = SyncManager.this.getConnectivityManager().getActiveNetworkInfoForUid(targetUid);
                boolean uidNetworkConnected = networkInfo != null && networkInfo.isConnected();
                if (!uidNetworkConnected && !ignoreSystemConfiguration) {
                    if (isLoggable) {
                        Log.v("SyncManager", "    Dropping sync operation: disallowed by settings/network.");
                    }
                    return false;
                }
                if (op.isNotAllowedOnMetered() && SyncManager.this.getConnectivityManager().isActiveNetworkMetered() && !ignoreSystemConfiguration) {
                    if (isLoggable) {
                        Log.v("SyncManager", "    Dropping sync operation: not allowed on metered network.");
                    }
                    return false;
                }
                return true;
            }
            if (isLoggable) {
                Log.v("SyncManager", "    Dropping sync operation: disallowed by settings/network.");
            }
            return false;
        }

        private boolean dispatchSyncOperation(SyncOperation op) {
            int targetUid;
            ComponentName targetComponent;
            if (Log.isLoggable("SyncManager", 2)) {
                Log.v("SyncManager", "dispatchSyncOperation: we are going to sync " + op);
                Log.v("SyncManager", "num active syncs: " + SyncManager.this.mActiveSyncContexts.size());
                for (ActiveSyncContext syncContext : SyncManager.this.mActiveSyncContexts) {
                    Log.v("SyncManager", syncContext.toString());
                }
            }
            SyncStorageEngine.EndPoint info = op.target;
            if (!info.target_provider) {
                try {
                    targetUid = SyncManager.this.mContext.getPackageManager().getServiceInfo(info.service, 0).applicationInfo.uid;
                    targetComponent = info.service;
                } catch (PackageManager.NameNotFoundException e) {
                    Log.d("SyncManager", "Can't find a service for " + info.service + ", removing settings for it");
                    SyncManager.this.mSyncStorageEngine.removeAuthority(info);
                    return false;
                }
            } else {
                SyncAdapterType syncAdapterType = SyncAdapterType.newKey(info.provider, info.account.type);
                RegisteredServicesCache.ServiceInfo<SyncAdapterType> syncAdapterInfo = SyncManager.this.mSyncAdapters.getServiceInfo(syncAdapterType, info.userId);
                if (syncAdapterInfo == null) {
                    Log.d("SyncManager", "can't find a sync adapter for " + syncAdapterType + ", removing settings for it");
                    SyncManager.this.mSyncStorageEngine.removeAuthority(info);
                    return false;
                }
                targetUid = syncAdapterInfo.uid;
                targetComponent = syncAdapterInfo.componentName;
            }
            ActiveSyncContext activeSyncContext = SyncManager.this.new ActiveSyncContext(op, insertStartSyncEvent(op), targetUid);
            activeSyncContext.mSyncInfo = SyncManager.this.mSyncStorageEngine.addActiveSync(activeSyncContext);
            SyncManager.this.mActiveSyncContexts.add(activeSyncContext);
            if (!activeSyncContext.mSyncOperation.isInitialization() && !activeSyncContext.mSyncOperation.isExpedited() && !activeSyncContext.mSyncOperation.isManual() && !activeSyncContext.mSyncOperation.isIgnoreSettings()) {
                SyncManager.this.postSyncExpiryMessage(activeSyncContext);
            }
            if (Log.isLoggable("SyncManager", 2)) {
                Log.v("SyncManager", "dispatchSyncOperation: starting " + activeSyncContext);
            }
            if (!activeSyncContext.bindToSyncAdapter(targetComponent, info.userId)) {
                Log.e("SyncManager", "Bind attempt failed - target: " + targetComponent);
                closeActiveSyncContext(activeSyncContext);
                return false;
            }
            return true;
        }

        private void runBoundToAdapter(ActiveSyncContext activeSyncContext, IBinder syncAdapter) {
            SyncOperation syncOperation = activeSyncContext.mSyncOperation;
            try {
                activeSyncContext.mIsLinkedToDeath = true;
                syncAdapter.linkToDeath(activeSyncContext, 0);
                if (syncOperation.target.target_provider) {
                    activeSyncContext.mSyncAdapter = ISyncAdapter.Stub.asInterface(syncAdapter);
                    activeSyncContext.mSyncAdapter.startSync(activeSyncContext, syncOperation.target.provider, syncOperation.target.account, syncOperation.extras);
                } else if (syncOperation.target.target_service) {
                    activeSyncContext.mSyncServiceAdapter = ISyncServiceAdapter.Stub.asInterface(syncAdapter);
                    activeSyncContext.mSyncServiceAdapter.startSync(activeSyncContext, syncOperation.extras);
                }
            } catch (RemoteException remoteExc) {
                Log.d("SyncManager", "maybeStartNextSync: caught a RemoteException, rescheduling", remoteExc);
                closeActiveSyncContext(activeSyncContext);
                SyncManager.this.increaseBackoffSetting(syncOperation);
                SyncManager.this.scheduleSyncOperation(new SyncOperation(syncOperation, 0L));
            } catch (RuntimeException exc) {
                closeActiveSyncContext(activeSyncContext);
                Log.e("SyncManager", "Caught RuntimeException while starting the sync " + syncOperation, exc);
            }
        }

        private void cancelActiveSyncLocked(SyncStorageEngine.EndPoint info, Bundle extras) {
            ArrayList<ActiveSyncContext> activeSyncs = new ArrayList<>(SyncManager.this.mActiveSyncContexts);
            for (ActiveSyncContext activeSyncContext : activeSyncs) {
                if (activeSyncContext != null) {
                    SyncStorageEngine.EndPoint opInfo = activeSyncContext.mSyncOperation.target;
                    if (opInfo.matchesSpec(info) && (extras == null || SyncManager.syncExtrasEquals(activeSyncContext.mSyncOperation.extras, extras, false))) {
                        runSyncFinishedOrCanceledLocked(null, activeSyncContext);
                    }
                }
            }
        }

        private void runSyncFinishedOrCanceledLocked(SyncResult syncResult, ActiveSyncContext activeSyncContext) {
            String historyMessage;
            int downstreamActivity;
            int upstreamActivity;
            boolean isLoggable = Log.isLoggable("SyncManager", 2);
            SyncOperation syncOperation = activeSyncContext.mSyncOperation;
            SyncStorageEngine.EndPoint info = syncOperation.target;
            if (activeSyncContext.mIsLinkedToDeath) {
                if (info.target_provider) {
                    activeSyncContext.mSyncAdapter.asBinder().unlinkToDeath(activeSyncContext, 0);
                } else {
                    activeSyncContext.mSyncServiceAdapter.asBinder().unlinkToDeath(activeSyncContext, 0);
                }
                activeSyncContext.mIsLinkedToDeath = false;
            }
            closeActiveSyncContext(activeSyncContext);
            long elapsedTime = SystemClock.elapsedRealtime() - activeSyncContext.mStartTime;
            if (syncResult != null) {
                if (isLoggable) {
                    Log.v("SyncManager", "runSyncFinishedOrCanceled [finished]: " + syncOperation + ", result " + syncResult);
                }
                if (!syncResult.hasError()) {
                    historyMessage = SyncStorageEngine.MESG_SUCCESS;
                    downstreamActivity = 0;
                    upstreamActivity = 0;
                    SyncManager.this.clearBackoffSetting(syncOperation);
                } else {
                    Log.d("SyncManager", "failed sync operation " + syncOperation + ", " + syncResult);
                    SyncManager.this.increaseBackoffSetting(syncOperation);
                    SyncManager.this.maybeRescheduleSync(syncResult, syncOperation);
                    historyMessage = ContentResolver.syncErrorToString(syncResultToErrorNumber(syncResult));
                    downstreamActivity = 0;
                    upstreamActivity = 0;
                }
                SyncManager.this.setDelayUntilTime(syncOperation, syncResult.delayUntil);
            } else {
                if (isLoggable) {
                    Log.v("SyncManager", "runSyncFinishedOrCanceled [canceled]: " + syncOperation);
                }
                if (activeSyncContext.mSyncAdapter != null) {
                    try {
                        activeSyncContext.mSyncAdapter.cancelSync(activeSyncContext);
                    } catch (RemoteException e) {
                    }
                } else if (activeSyncContext.mSyncServiceAdapter != null) {
                    try {
                        activeSyncContext.mSyncServiceAdapter.cancelSync(activeSyncContext);
                    } catch (RemoteException e2) {
                    }
                }
                historyMessage = SyncStorageEngine.MESG_CANCELED;
                downstreamActivity = 0;
                upstreamActivity = 0;
            }
            stopSyncEvent(activeSyncContext.mHistoryRowId, syncOperation, historyMessage, upstreamActivity, downstreamActivity, elapsedTime);
            if (info.target_provider) {
                if (syncResult == null || !syncResult.tooManyDeletions) {
                    SyncManager.this.mNotificationMgr.cancelAsUser(null, info.account.hashCode() ^ info.provider.hashCode(), new UserHandle(info.userId));
                } else {
                    installHandleTooManyDeletesNotification(info.account, info.provider, syncResult.stats.numDeletes, info.userId);
                }
                if (syncResult != null && syncResult.fullSyncRequested) {
                    SyncManager.this.scheduleSyncOperation(new SyncOperation(info.account, info.userId, syncOperation.reason, syncOperation.syncSource, info.provider, new Bundle(), 0L, 0L, syncOperation.backoff, syncOperation.delayUntil, syncOperation.allowParallelSyncs));
                    return;
                }
                return;
            }
            if (syncResult != null && syncResult.fullSyncRequested) {
                SyncManager.this.scheduleSyncOperation(new SyncOperation(info.service, info.userId, syncOperation.reason, syncOperation.syncSource, new Bundle(), 0L, 0L, syncOperation.backoff, syncOperation.delayUntil));
            }
        }

        private void closeActiveSyncContext(ActiveSyncContext activeSyncContext) {
            activeSyncContext.close();
            SyncManager.this.mActiveSyncContexts.remove(activeSyncContext);
            SyncManager.this.mSyncStorageEngine.removeActiveSync(activeSyncContext.mSyncInfo, activeSyncContext.mSyncOperation.target.userId);
            SyncManager.this.removeSyncExpiryMessage(activeSyncContext);
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

        private void manageSyncNotificationLocked() {
            boolean shouldCancel;
            boolean shouldInstall;
            if (SyncManager.this.mActiveSyncContexts.isEmpty()) {
                this.mSyncNotificationInfo.startTime = null;
                shouldCancel = this.mSyncNotificationInfo.isActive;
                shouldInstall = false;
            } else {
                long now = SystemClock.elapsedRealtime();
                if (this.mSyncNotificationInfo.startTime == null) {
                    this.mSyncNotificationInfo.startTime = Long.valueOf(now);
                }
                if (this.mSyncNotificationInfo.isActive) {
                    shouldCancel = false;
                    shouldInstall = false;
                } else {
                    shouldCancel = false;
                    boolean timeToShowNotification = now > this.mSyncNotificationInfo.startTime.longValue() + SyncManager.SYNC_NOTIFICATION_DELAY;
                    if (timeToShowNotification) {
                        shouldInstall = true;
                    } else {
                        shouldInstall = false;
                        Iterator<ActiveSyncContext> it = SyncManager.this.mActiveSyncContexts.iterator();
                        while (true) {
                            if (!it.hasNext()) {
                                break;
                            }
                            ActiveSyncContext activeSyncContext = it.next();
                            boolean manualSync = activeSyncContext.mSyncOperation.extras.getBoolean("force", false);
                            if (manualSync) {
                                shouldInstall = true;
                                break;
                            }
                        }
                    }
                }
            }
            if (shouldCancel && !shouldInstall) {
                SyncManager.this.mNeedSyncActiveNotification = false;
                sendSyncStateIntent();
                this.mSyncNotificationInfo.isActive = false;
            }
            if (shouldInstall) {
                SyncManager.this.mNeedSyncActiveNotification = true;
                sendSyncStateIntent();
                this.mSyncNotificationInfo.isActive = true;
            }
        }

        private void manageSyncAlarmLocked(long nextPeriodicEventElapsedTime, long nextPendingEventElapsedTime) {
            long notificationTime;
            if (SyncManager.this.mDataConnectionIsConnected && !SyncManager.this.mStorageIsLow) {
                if (!SyncManager.this.mSyncHandler.mSyncNotificationInfo.isActive && SyncManager.this.mSyncHandler.mSyncNotificationInfo.startTime != null) {
                    notificationTime = SyncManager.this.mSyncHandler.mSyncNotificationInfo.startTime.longValue() + SyncManager.SYNC_NOTIFICATION_DELAY;
                } else {
                    notificationTime = JobStatus.NO_LATEST_RUNTIME;
                }
                long earliestTimeoutTime = JobStatus.NO_LATEST_RUNTIME;
                for (ActiveSyncContext currentSyncContext : SyncManager.this.mActiveSyncContexts) {
                    long currentSyncTimeoutTime = currentSyncContext.mTimeoutStartTime + SyncManager.MAX_TIME_PER_SYNC;
                    if (Log.isLoggable("SyncManager", 2)) {
                        Log.v("SyncManager", "manageSyncAlarm: active sync, mTimeoutStartTime + MAX is " + currentSyncTimeoutTime);
                    }
                    if (earliestTimeoutTime > currentSyncTimeoutTime) {
                        earliestTimeoutTime = currentSyncTimeoutTime;
                    }
                }
                if (Log.isLoggable("SyncManager", 2)) {
                    Log.v("SyncManager", "manageSyncAlarm: notificationTime is " + notificationTime);
                }
                if (Log.isLoggable("SyncManager", 2)) {
                    Log.v("SyncManager", "manageSyncAlarm: earliestTimeoutTime is " + earliestTimeoutTime);
                }
                if (Log.isLoggable("SyncManager", 2)) {
                    Log.v("SyncManager", "manageSyncAlarm: nextPeriodicEventElapsedTime is " + nextPeriodicEventElapsedTime);
                }
                if (Log.isLoggable("SyncManager", 2)) {
                    Log.v("SyncManager", "manageSyncAlarm: nextPendingEventElapsedTime is " + nextPendingEventElapsedTime);
                }
                long alarmTime = Math.min(Math.min(Math.min(notificationTime, earliestTimeoutTime), nextPeriodicEventElapsedTime), nextPendingEventElapsedTime);
                long now = SystemClock.elapsedRealtime();
                if (alarmTime < 30000 + now) {
                    if (Log.isLoggable("SyncManager", 2)) {
                        Log.v("SyncManager", "manageSyncAlarm: the alarmTime is too small, " + alarmTime + ", setting to " + (30000 + now));
                    }
                    alarmTime = now + 30000;
                } else if (alarmTime > SyncManager.SYNC_ALARM_TIMEOUT_MAX + now) {
                    if (Log.isLoggable("SyncManager", 2)) {
                        Log.v("SyncManager", "manageSyncAlarm: the alarmTime is too large, " + alarmTime + ", setting to " + (30000 + now));
                    }
                    alarmTime = now + SyncManager.SYNC_ALARM_TIMEOUT_MAX;
                }
                boolean shouldSet = false;
                boolean shouldCancel = false;
                boolean alarmIsActive = this.mAlarmScheduleTime != null && now < this.mAlarmScheduleTime.longValue();
                boolean needAlarm = alarmTime != JobStatus.NO_LATEST_RUNTIME;
                if (needAlarm) {
                    if (!alarmIsActive || alarmTime < this.mAlarmScheduleTime.longValue()) {
                        shouldSet = true;
                    }
                } else {
                    shouldCancel = alarmIsActive;
                }
                SyncManager.this.ensureAlarmService();
                if (shouldSet) {
                    if (Log.isLoggable("SyncManager", 2)) {
                        Log.v("SyncManager", "requesting that the alarm manager wake us up at elapsed time " + alarmTime + ", now is " + now + ", " + ((alarmTime - now) / 1000) + " secs from now");
                    }
                    this.mAlarmScheduleTime = Long.valueOf(alarmTime);
                    SyncManager.this.mAlarmService.setExact(2, alarmTime, SyncManager.this.mSyncAlarmIntent);
                    return;
                }
                if (shouldCancel) {
                    this.mAlarmScheduleTime = null;
                    SyncManager.this.mAlarmService.cancel(SyncManager.this.mSyncAlarmIntent);
                }
            }
        }

        private void sendSyncStateIntent() {
            Intent syncStateIntent = new Intent("android.intent.action.SYNC_STATE_CHANGED");
            syncStateIntent.addFlags(67108864);
            syncStateIntent.putExtra("active", SyncManager.this.mNeedSyncActiveNotification);
            syncStateIntent.putExtra("failing", false);
            SyncManager.this.mContext.sendBroadcastAsUser(syncStateIntent, UserHandle.OWNER);
        }

        private void installHandleTooManyDeletesNotification(Account account, String authority, long numDeletes, int userId) {
            ProviderInfo providerInfo;
            if (SyncManager.this.mNotificationMgr != null && (providerInfo = SyncManager.this.mContext.getPackageManager().resolveContentProvider(authority, 0)) != null) {
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
                CharSequence tooManyDeletesDescFormat = SyncManager.this.mContext.getResources().getText(R.string.accessibility_autoclick_scroll_up);
                Context contextForUser = SyncManager.this.getContextForUser(user);
                Notification notification = new Notification(R.drawable.item_background_activated_holo_dark, SyncManager.this.mContext.getString(R.string.accessibility_autoclick_scroll_panel_title), System.currentTimeMillis());
                notification.color = contextForUser.getResources().getColor(R.color.system_accent3_600);
                notification.setLatestEventInfo(contextForUser, contextForUser.getString(R.string.accessibility_autoclick_scroll_right), String.format(tooManyDeletesDescFormat.toString(), authorityName), pendingIntent);
                notification.flags |= 2;
                SyncManager.this.mNotificationMgr.notifyAsUser(null, account.hashCode() ^ authority.hashCode(), notification, user);
            }
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

    private boolean isSyncStillActive(ActiveSyncContext activeSyncContext) {
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
                if (smaller.containsKey(key) && Objects.equals(bigger.get(key), smaller.get(key))) {
                }
                return false;
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
