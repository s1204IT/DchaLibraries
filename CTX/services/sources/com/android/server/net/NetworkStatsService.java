package com.android.server.net;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.net.ConnectivityManager;
import android.net.DataUsageRequest;
import android.net.IConnectivityManager;
import android.net.INetworkManagementEventObserver;
import android.net.INetworkStatsService;
import android.net.INetworkStatsSession;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkIdentity;
import android.net.NetworkState;
import android.net.NetworkStats;
import android.net.NetworkStatsHistory;
import android.net.NetworkTemplate;
import android.os.BestClock;
import android.os.Binder;
import android.os.DropBoxManager;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Trace;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.SubscriptionPlan;
import android.telephony.TelephonyManager;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.MathUtils;
import android.util.Slog;
import android.util.SparseIntArray;
import android.util.proto.ProtoOutputStream;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.net.NetworkStatsFactory;
import com.android.internal.net.VpnInfo;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FileRotator;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.server.EventLogTags;
import com.android.server.LocalServices;
import com.android.server.NetworkManagementService;
import com.android.server.NetworkManagementSocketTagger;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.job.controllers.JobStatus;
import com.android.server.usage.AppStandbyController;
import com.android.server.utils.PriorityDump;
import dalvik.system.PathClassLoader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

public class NetworkStatsService extends INetworkStatsService.Stub {

    @VisibleForTesting
    public static final String ACTION_NETWORK_STATS_POLL = "com.android.server.action.NETWORK_STATS_POLL";
    public static final String ACTION_NETWORK_STATS_UPDATED = "com.android.server.action.NETWORK_STATS_UPDATED";
    private static final int DUMP_STATS_SESSION_COUNT = 20;
    private static final int FLAG_PERSIST_ALL = 3;
    private static final int FLAG_PERSIST_FORCE = 256;
    private static final int FLAG_PERSIST_NETWORK = 1;
    private static final int FLAG_PERSIST_UID = 2;
    private static final int MSG_PERFORM_POLL = 1;
    private static final int MSG_REGISTER_GLOBAL_ALERT = 3;
    private static final int MSG_UPDATE_IFACES = 2;
    private static final long POLL_RATE_LIMIT_MS = 15000;
    private static final String PREFIX_DEV = "dev";
    private static final String PREFIX_UID = "uid";
    private static final String PREFIX_UID_TAG = "uid_tag";
    private static final String PREFIX_XT = "xt";
    private static final String TAG_NETSTATS_ERROR = "netstats_error";
    private static int TYPE_RX_BYTES = 0;
    private static int TYPE_RX_PACKETS = 0;
    private static int TYPE_TCP_RX_PACKETS = 0;
    private static int TYPE_TCP_TX_PACKETS = 0;
    private static int TYPE_TX_BYTES = 0;
    private static int TYPE_TX_PACKETS = 0;
    public static final String VT_INTERFACE = "vt_data0";
    private String mActiveIface;
    private final AlarmManager mAlarmManager;
    private final File mBaseDir;
    private Clock mClock;
    private IConnectivityManager mConnManager;
    protected final Context mContext;

    @GuardedBy("mStatsLock")
    private NetworkStatsRecorder mDevRecorder;
    private long mGlobalAlertBytes;
    private Handler mHandler;
    private Handler.Callback mHandlerCallback;
    private long mLastStatsSessionPoll;
    private final INetworkManagementService mNetworkManager;
    private final DropBoxNonMonotonicObserver mNonMonotonicObserver;
    private PendingIntent mPollIntent;
    private final NetworkStatsSettings mSettings;
    private final NetworkStatsObservers mStatsObservers;
    private final File mSystemDir;
    private volatile boolean mSystemReady;
    private final TelephonyManager mTeleManager;

    @GuardedBy("mStatsLock")
    private NetworkStatsRecorder mUidRecorder;

    @GuardedBy("mStatsLock")
    private NetworkStatsRecorder mUidTagRecorder;
    private final PowerManager.WakeLock mWakeLock;

    @GuardedBy("mStatsLock")
    private NetworkStatsRecorder mXtRecorder;

    @GuardedBy("mStatsLock")
    private NetworkStatsCollection mXtStatsCached;
    static final String TAG = "NetworkStats";
    static final boolean LOGD = Log.isLoggable(TAG, 3);
    static final boolean LOGV = Log.isLoggable(TAG, 2);
    private final Object mStatsLock = new Object();

    @GuardedBy("mStatsLock")
    protected final ArrayMap<String, NetworkIdentitySet> mActiveIfaces = new ArrayMap<>();

    @GuardedBy("mStatsLock")
    protected final ArrayMap<String, NetworkIdentitySet> mActiveUidIfaces = new ArrayMap<>();

    @GuardedBy("mStatsLock")
    private String[] mMobileIfaces = new String[0];

    @GuardedBy("mStatsLock")
    private Network[] mDefaultNetworks = new Network[0];
    private SparseIntArray mActiveUidCounterSet = new SparseIntArray();
    private NetworkStats mUidOperations = new NetworkStats(0, 10);
    private long mPersistThreshold = 2097152;

    @GuardedBy("mOpenSessionCallsPerUid")
    private final SparseIntArray mOpenSessionCallsPerUid = new SparseIntArray();
    private BroadcastReceiver mTetherReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            NetworkStatsService.this.performPoll(1);
        }
    };
    private BroadcastReceiver mPollReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            NetworkStatsService.this.performPoll(3);
            NetworkStatsService.this.registerGlobalAlert();
        }
    };
    private BroadcastReceiver mRemovedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int intExtra = intent.getIntExtra("android.intent.extra.UID", -1);
            if (intExtra == -1) {
                return;
            }
            synchronized (NetworkStatsService.this.mStatsLock) {
                NetworkStatsService.this.mWakeLock.acquire();
                try {
                    NetworkStatsService.this.removeUidsLocked(intExtra);
                } finally {
                    NetworkStatsService.this.mWakeLock.release();
                }
            }
        }
    };
    private BroadcastReceiver mUserReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int intExtra = intent.getIntExtra("android.intent.extra.user_handle", -1);
            if (intExtra == -1) {
                return;
            }
            synchronized (NetworkStatsService.this.mStatsLock) {
                NetworkStatsService.this.mWakeLock.acquire();
                try {
                    NetworkStatsService.this.removeUserLocked(intExtra);
                } finally {
                    NetworkStatsService.this.mWakeLock.release();
                }
            }
        }
    };
    private BroadcastReceiver mShutdownReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (NetworkStatsService.this.mStatsLock) {
                NetworkStatsService.this.shutdownLocked();
            }
        }
    };
    private INetworkManagementEventObserver mAlertObserver = new BaseNetworkObserver() {
        public void limitReached(String str, String str2) {
            NetworkStatsService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", NetworkStatsService.TAG);
            if (NetworkManagementService.LIMIT_GLOBAL_ALERT.equals(str)) {
                NetworkStatsService.this.mHandler.obtainMessage(1, 1, 0).sendToTarget();
                NetworkStatsService.this.mHandler.obtainMessage(3).sendToTarget();
            }
        }
    };
    private final boolean mUseBpfTrafficStats = new File("/sys/fs/bpf/traffic_uid_stats_map").exists();

    private static native long nativeGetIfaceStat(String str, int i, boolean z);

    private static native long nativeGetTotalStat(int i, boolean z);

    private static native long nativeGetUidStat(int i, int i2, boolean z);

    public interface NetworkStatsSettings {
        boolean getAugmentEnabled();

        Config getDevConfig();

        long getDevPersistBytes(long j);

        long getGlobalAlertBytes(long j);

        long getPollInterval();

        boolean getSampleEnabled();

        Config getUidConfig();

        long getUidPersistBytes(long j);

        Config getUidTagConfig();

        long getUidTagPersistBytes(long j);

        Config getXtConfig();

        long getXtPersistBytes(long j);

        public static class Config {
            public final long bucketDuration;
            public final long deleteAgeMillis;
            public final long rotateAgeMillis;

            public Config(long j, long j2, long j3) {
                this.bucketDuration = j;
                this.rotateAgeMillis = j2;
                this.deleteAgeMillis = j3;
            }
        }
    }

    private static File getDefaultSystemDir() {
        return new File(Environment.getDataDirectory(), "system");
    }

    private static File getDefaultBaseDir() {
        File file = new File(getDefaultSystemDir(), "netstats");
        file.mkdirs();
        return file;
    }

    private static Clock getDefaultClock() {
        return new BestClock(ZoneOffset.UTC, new Clock[]{SystemClock.currentNetworkTimeClock(), Clock.systemUTC()});
    }

    public static NetworkStatsService create(Context context, INetworkManagementService iNetworkManagementService) {
        NetworkStatsService networkStatsService;
        AlarmManager alarmManager = (AlarmManager) context.getSystemService("alarm");
        PowerManager.WakeLock wakeLockNewWakeLock = ((PowerManager) context.getSystemService("power")).newWakeLock(1, TAG);
        try {
            Constructor constructor = new PathClassLoader("/system/framework/mediatek-framework-net.jar", context.getClassLoader()).loadClass("com.mediatek.server.MtkNetworkStatsService").getConstructor(Context.class, INetworkManagementService.class, AlarmManager.class, PowerManager.WakeLock.class, Clock.class, TelephonyManager.class, NetworkStatsSettings.class, NetworkStatsObservers.class, File.class, File.class);
            constructor.setAccessible(true);
            networkStatsService = (NetworkStatsService) constructor.newInstance(context, iNetworkManagementService, alarmManager, wakeLockNewWakeLock, getDefaultClock(), TelephonyManager.getDefault(), new DefaultNetworkStatsSettings(context), new NetworkStatsObservers(), getDefaultSystemDir(), getDefaultBaseDir());
        } catch (Exception e) {
            Slog.e(TAG, "No MtkNetworkStatsService! Used AOSP for instead!", e);
            networkStatsService = null;
        }
        if (networkStatsService == null) {
            networkStatsService = new NetworkStatsService(context, iNetworkManagementService, alarmManager, wakeLockNewWakeLock, getDefaultClock(), TelephonyManager.getDefault(), new DefaultNetworkStatsSettings(context), new NetworkStatsObservers(), getDefaultSystemDir(), getDefaultBaseDir());
        }
        HandlerThread handlerThread = new HandlerThread(TAG);
        HandlerCallback handlerCallback = new HandlerCallback(networkStatsService);
        handlerThread.start();
        networkStatsService.setHandler(new Handler(handlerThread.getLooper(), handlerCallback), handlerCallback);
        return networkStatsService;
    }

    @VisibleForTesting
    protected NetworkStatsService(Context context, INetworkManagementService iNetworkManagementService, AlarmManager alarmManager, PowerManager.WakeLock wakeLock, Clock clock, TelephonyManager telephonyManager, NetworkStatsSettings networkStatsSettings, NetworkStatsObservers networkStatsObservers, File file, File file2) {
        this.mNonMonotonicObserver = new DropBoxNonMonotonicObserver();
        this.mContext = (Context) Preconditions.checkNotNull(context, "missing Context");
        this.mNetworkManager = (INetworkManagementService) Preconditions.checkNotNull(iNetworkManagementService, "missing INetworkManagementService");
        this.mAlarmManager = (AlarmManager) Preconditions.checkNotNull(alarmManager, "missing AlarmManager");
        this.mClock = (Clock) Preconditions.checkNotNull(clock, "missing Clock");
        this.mSettings = (NetworkStatsSettings) Preconditions.checkNotNull(networkStatsSettings, "missing NetworkStatsSettings");
        this.mTeleManager = (TelephonyManager) Preconditions.checkNotNull(telephonyManager, "missing TelephonyManager");
        this.mWakeLock = (PowerManager.WakeLock) Preconditions.checkNotNull(wakeLock, "missing WakeLock");
        this.mStatsObservers = (NetworkStatsObservers) Preconditions.checkNotNull(networkStatsObservers, "missing NetworkStatsObservers");
        this.mSystemDir = (File) Preconditions.checkNotNull(file, "missing systemDir");
        this.mBaseDir = (File) Preconditions.checkNotNull(file2, "missing baseDir");
        LocalServices.addService(NetworkStatsManagerInternal.class, new NetworkStatsManagerInternalImpl());
    }

    @VisibleForTesting
    void setHandler(Handler handler, Handler.Callback callback) {
        this.mHandler = handler;
        this.mHandlerCallback = callback;
    }

    public void bindConnectivityManager(IConnectivityManager iConnectivityManager) {
        this.mConnManager = (IConnectivityManager) Preconditions.checkNotNull(iConnectivityManager, "missing IConnectivityManager");
    }

    public void systemReady() {
        this.mSystemReady = true;
        if (!isBandwidthControlEnabled()) {
            Slog.w(TAG, "bandwidth controls disabled, unable to track stats");
            return;
        }
        synchronized (this.mStatsLock) {
            this.mDevRecorder = buildRecorder(PREFIX_DEV, this.mSettings.getDevConfig(), false);
            this.mXtRecorder = buildRecorder(PREFIX_XT, this.mSettings.getXtConfig(), false);
            this.mUidRecorder = buildRecorder("uid", this.mSettings.getUidConfig(), false);
            this.mUidTagRecorder = buildRecorder(PREFIX_UID_TAG, this.mSettings.getUidTagConfig(), true);
            updatePersistThresholdsLocked();
            maybeUpgradeLegacyStatsLocked();
            this.mXtStatsCached = this.mXtRecorder.getOrLoadCompleteLocked();
            bootstrapStatsLocked();
        }
        this.mContext.registerReceiver(this.mTetherReceiver, new IntentFilter("android.net.conn.TETHER_STATE_CHANGED"), null, this.mHandler);
        this.mContext.registerReceiver(this.mPollReceiver, new IntentFilter(ACTION_NETWORK_STATS_POLL), "android.permission.READ_NETWORK_USAGE_HISTORY", this.mHandler);
        this.mContext.registerReceiver(this.mRemovedReceiver, new IntentFilter("android.intent.action.UID_REMOVED"), null, this.mHandler);
        this.mContext.registerReceiver(this.mUserReceiver, new IntentFilter("android.intent.action.USER_REMOVED"), null, this.mHandler);
        this.mContext.registerReceiver(this.mShutdownReceiver, new IntentFilter("android.intent.action.ACTION_SHUTDOWN"));
        try {
            this.mNetworkManager.registerObserver(this.mAlertObserver);
        } catch (RemoteException e) {
        }
        registerPollAlarmLocked();
        registerGlobalAlert();
    }

    private NetworkStatsRecorder buildRecorder(String str, NetworkStatsSettings.Config config, boolean z) {
        return new NetworkStatsRecorder(new FileRotator(this.mBaseDir, str, config.rotateAgeMillis, config.deleteAgeMillis), this.mNonMonotonicObserver, (DropBoxManager) this.mContext.getSystemService("dropbox"), str, config.bucketDuration, z);
    }

    @GuardedBy("mStatsLock")
    private void shutdownLocked() {
        this.mContext.unregisterReceiver(this.mTetherReceiver);
        this.mContext.unregisterReceiver(this.mPollReceiver);
        this.mContext.unregisterReceiver(this.mRemovedReceiver);
        this.mContext.unregisterReceiver(this.mUserReceiver);
        this.mContext.unregisterReceiver(this.mShutdownReceiver);
        long jMillis = this.mClock.millis();
        this.mDevRecorder.forcePersistLocked(jMillis);
        this.mXtRecorder.forcePersistLocked(jMillis);
        this.mUidRecorder.forcePersistLocked(jMillis);
        this.mUidTagRecorder.forcePersistLocked(jMillis);
        this.mSystemReady = false;
    }

    @GuardedBy("mStatsLock")
    private void maybeUpgradeLegacyStatsLocked() throws Throwable {
        try {
            File file = new File(this.mSystemDir, "netstats.bin");
            if (file.exists()) {
                this.mDevRecorder.importLegacyNetworkLocked(file);
                file.delete();
            }
            File file2 = new File(this.mSystemDir, "netstats_xt.bin");
            if (file2.exists()) {
                file2.delete();
            }
            File file3 = new File(this.mSystemDir, "netstats_uid.bin");
            if (file3.exists()) {
                this.mUidRecorder.importLegacyUidLocked(file3);
                this.mUidTagRecorder.importLegacyUidLocked(file3);
                file3.delete();
            }
        } catch (IOException e) {
            Log.e(TAG, "problem during legacy upgrade", e);
        } catch (OutOfMemoryError e2) {
            Log.wtf(TAG, "problem during legacy upgrade", e2);
        }
    }

    private void registerPollAlarmLocked() {
        if (this.mPollIntent != null) {
            this.mAlarmManager.cancel(this.mPollIntent);
        }
        this.mPollIntent = PendingIntent.getBroadcast(this.mContext, 0, new Intent(ACTION_NETWORK_STATS_POLL), 0);
        this.mAlarmManager.setInexactRepeating(3, SystemClock.elapsedRealtime(), this.mSettings.getPollInterval(), this.mPollIntent);
    }

    private void registerGlobalAlert() {
        try {
            this.mNetworkManager.setGlobalAlert(this.mGlobalAlertBytes);
        } catch (RemoteException e) {
        } catch (IllegalStateException e2) {
            Slog.w(TAG, "problem registering for global alert: " + e2);
        }
    }

    public INetworkStatsSession openSession() {
        return openSessionInternal(4, null);
    }

    public INetworkStatsSession openSessionForUsageStats(int i, String str) {
        return openSessionInternal(i, str);
    }

    private boolean isRateLimitedForPoll(int i) {
        long j;
        if (i == 1000) {
            return false;
        }
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        synchronized (this.mOpenSessionCallsPerUid) {
            this.mOpenSessionCallsPerUid.put(i, this.mOpenSessionCallsPerUid.get(i, 0) + 1);
            j = this.mLastStatsSessionPoll;
            this.mLastStatsSessionPoll = jElapsedRealtime;
        }
        return jElapsedRealtime - j < POLL_RATE_LIMIT_MS;
    }

    private INetworkStatsSession openSessionInternal(final int i, final String str) {
        assertBandwidthControlEnabled();
        final int callingUid = Binder.getCallingUid();
        if (isRateLimitedForPoll(callingUid)) {
            i &= -2;
        }
        if ((i & 3) != 0) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                performPoll(3);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
        return new INetworkStatsSession.Stub() {
            private final int mAccessLevel;
            private final String mCallingPackage;
            private final int mCallingUid;
            private NetworkStatsCollection mUidComplete;
            private NetworkStatsCollection mUidTagComplete;

            {
                this.mCallingUid = callingUid;
                this.mCallingPackage = str;
                this.mAccessLevel = NetworkStatsService.this.checkAccessLevel(str);
            }

            private NetworkStatsCollection getUidComplete() {
                NetworkStatsCollection networkStatsCollection;
                synchronized (NetworkStatsService.this.mStatsLock) {
                    if (this.mUidComplete == null) {
                        this.mUidComplete = NetworkStatsService.this.mUidRecorder.getOrLoadCompleteLocked();
                    }
                    networkStatsCollection = this.mUidComplete;
                }
                return networkStatsCollection;
            }

            private NetworkStatsCollection getUidTagComplete() {
                NetworkStatsCollection networkStatsCollection;
                synchronized (NetworkStatsService.this.mStatsLock) {
                    if (this.mUidTagComplete == null) {
                        this.mUidTagComplete = NetworkStatsService.this.mUidTagRecorder.getOrLoadCompleteLocked();
                    }
                    networkStatsCollection = this.mUidTagComplete;
                }
                return networkStatsCollection;
            }

            public int[] getRelevantUids() {
                return getUidComplete().getRelevantUids(this.mAccessLevel);
            }

            public NetworkStats getDeviceSummaryForNetwork(NetworkTemplate networkTemplate, long j, long j2) {
                return NetworkStatsService.this.internalGetSummaryForNetwork(networkTemplate, i, j, j2, this.mAccessLevel, this.mCallingUid);
            }

            public NetworkStats getSummaryForNetwork(NetworkTemplate networkTemplate, long j, long j2) {
                return NetworkStatsService.this.internalGetSummaryForNetwork(networkTemplate, i, j, j2, this.mAccessLevel, this.mCallingUid);
            }

            public NetworkStatsHistory getHistoryForNetwork(NetworkTemplate networkTemplate, int i2) {
                return NetworkStatsService.this.internalGetHistoryForNetwork(networkTemplate, i, i2, this.mAccessLevel, this.mCallingUid);
            }

            public NetworkStats getSummaryForAllUid(NetworkTemplate networkTemplate, long j, long j2, boolean z) {
                try {
                    NetworkStats summary = getUidComplete().getSummary(networkTemplate, j, j2, this.mAccessLevel, this.mCallingUid);
                    if (z) {
                        summary.combineAllValues(getUidTagComplete().getSummary(networkTemplate, j, j2, this.mAccessLevel, this.mCallingUid));
                    }
                    return summary;
                } catch (NullPointerException e) {
                    Slog.wtf(NetworkStatsService.TAG, "NullPointerException in getSummaryForAllUid", e);
                    throw e;
                }
            }

            public NetworkStatsHistory getHistoryForUid(NetworkTemplate networkTemplate, int i2, int i3, int i4, int i5) {
                if (i4 == 0) {
                    return getUidComplete().getHistory(networkTemplate, null, i2, i3, i4, i5, Long.MIN_VALUE, JobStatus.NO_LATEST_RUNTIME, this.mAccessLevel, this.mCallingUid);
                }
                return getUidTagComplete().getHistory(networkTemplate, null, i2, i3, i4, i5, Long.MIN_VALUE, JobStatus.NO_LATEST_RUNTIME, this.mAccessLevel, this.mCallingUid);
            }

            public NetworkStatsHistory getHistoryIntervalForUid(NetworkTemplate networkTemplate, int i2, int i3, int i4, int i5, long j, long j2) {
                if (i4 == 0) {
                    return getUidComplete().getHistory(networkTemplate, null, i2, i3, i4, i5, j, j2, this.mAccessLevel, this.mCallingUid);
                }
                if (i2 == Binder.getCallingUid()) {
                    return getUidTagComplete().getHistory(networkTemplate, null, i2, i3, i4, i5, j, j2, this.mAccessLevel, this.mCallingUid);
                }
                throw new SecurityException("Calling package " + this.mCallingPackage + " cannot access tag information from a different uid");
            }

            public void close() {
                this.mUidComplete = null;
                this.mUidTagComplete = null;
            }
        };
    }

    private int checkAccessLevel(String str) {
        return NetworkStatsAccess.checkAccessLevel(this.mContext, Binder.getCallingUid(), str);
    }

    private SubscriptionPlan resolveSubscriptionPlan(NetworkTemplate networkTemplate, int i) {
        if ((i & 4) != 0 && this.mSettings.getAugmentEnabled()) {
            if (LOGD) {
                Slog.d(TAG, "Resolving plan for " + networkTemplate);
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                SubscriptionPlan subscriptionPlan = ((NetworkPolicyManagerInternal) LocalServices.getService(NetworkPolicyManagerInternal.class)).getSubscriptionPlan(networkTemplate);
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                if (!LOGD) {
                    return subscriptionPlan;
                }
                Slog.d(TAG, "Resolved to plan " + subscriptionPlan);
                return subscriptionPlan;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                throw th;
            }
        }
        return null;
    }

    private NetworkStats internalGetSummaryForNetwork(NetworkTemplate networkTemplate, int i, long j, long j2, int i2, int i3) {
        NetworkStatsHistory.Entry values = internalGetHistoryForNetwork(networkTemplate, i, -1, i2, i3).getValues(j, j2, System.currentTimeMillis(), (NetworkStatsHistory.Entry) null);
        NetworkStats networkStats = new NetworkStats(j2 - j, 1);
        networkStats.addValues(new NetworkStats.Entry(NetworkStats.IFACE_ALL, -1, -1, 0, -1, -1, -1, values.rxBytes, values.rxPackets, values.txBytes, values.txPackets, values.operations));
        return networkStats;
    }

    private NetworkStatsHistory internalGetHistoryForNetwork(NetworkTemplate networkTemplate, int i, int i2, int i3, int i4) {
        NetworkStatsHistory history;
        SubscriptionPlan subscriptionPlanResolveSubscriptionPlan = resolveSubscriptionPlan(networkTemplate, i);
        synchronized (this.mStatsLock) {
            history = this.mXtStatsCached.getHistory(networkTemplate, subscriptionPlanResolveSubscriptionPlan, -1, -1, 0, i2, Long.MIN_VALUE, JobStatus.NO_LATEST_RUNTIME, i3, i4);
        }
        return history;
    }

    private long getNetworkTotalBytes(NetworkTemplate networkTemplate, long j, long j2) {
        assertSystemReady();
        assertBandwidthControlEnabled();
        return internalGetSummaryForNetwork(networkTemplate, 4, j, j2, 3, Binder.getCallingUid()).getTotalBytes();
    }

    private NetworkStats getNetworkUidBytes(NetworkTemplate networkTemplate, long j, long j2) {
        NetworkStatsCollection orLoadCompleteLocked;
        assertSystemReady();
        assertBandwidthControlEnabled();
        synchronized (this.mStatsLock) {
            orLoadCompleteLocked = this.mUidRecorder.getOrLoadCompleteLocked();
        }
        return orLoadCompleteLocked.getSummary(networkTemplate, j, j2, 3, 1000);
    }

    public NetworkStats getDataLayerSnapshotForUid(int i) throws RemoteException {
        if (Binder.getCallingUid() != i) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_NETWORK_STATE", TAG);
        }
        assertBandwidthControlEnabled();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            NetworkStats networkStatsUidDetail = this.mNetworkManager.getNetworkStatsUidDetail(i, NetworkStats.INTERFACES_ALL);
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            networkStatsUidDetail.spliceOperationsFrom(this.mUidOperations);
            NetworkStats networkStats = new NetworkStats(networkStatsUidDetail.getElapsedRealtime(), networkStatsUidDetail.size());
            NetworkStats.Entry values = null;
            for (int i2 = 0; i2 < networkStatsUidDetail.size(); i2++) {
                values = networkStatsUidDetail.getValues(i2, values);
                values.iface = NetworkStats.IFACE_ALL;
                networkStats.combineValues(values);
            }
            return networkStats;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
    }

    public NetworkStats getDetailedUidStats(String[] strArr) {
        try {
            return getNetworkStatsUidDetail(NetworkStatsFactory.augmentWithStackedInterfaces(strArr));
        } catch (RemoteException e) {
            Log.wtf(TAG, "Error compiling UID stats", e);
            return new NetworkStats(0L, 0);
        }
    }

    public String[] getMobileIfaces() {
        return this.mMobileIfaces;
    }

    public void incrementOperationCount(int i, int i2, int i3) throws Throwable {
        if (Binder.getCallingUid() != i) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.UPDATE_DEVICE_STATS", TAG);
        }
        if (i3 < 0) {
            throw new IllegalArgumentException("operation count can only be incremented");
        }
        if (i2 == 0) {
            throw new IllegalArgumentException("operation count must have specific tag");
        }
        synchronized (this.mStatsLock) {
            try {
                try {
                    int i4 = this.mActiveUidCounterSet.get(i, 0);
                    long j = i3;
                    this.mUidOperations.combineValues(this.mActiveIface, i, i4, i2, 0L, 0L, 0L, 0L, j);
                    this.mUidOperations.combineValues(this.mActiveIface, i, i4, 0, 0L, 0L, 0L, 0L, j);
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    @VisibleForTesting
    void setUidForeground(int i, boolean z) {
        synchronized (this.mStatsLock) {
            if (this.mActiveUidCounterSet.get(i, 0) != z) {
                this.mActiveUidCounterSet.put(i, z ? 1 : 0);
                NetworkManagementSocketTagger.setKernelCounterSet(i, z ? 1 : 0);
            }
        }
    }

    public void forceUpdateIfaces(Network[] networkArr) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_NETWORK_USAGE_HISTORY", TAG);
        assertBandwidthControlEnabled();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            updateIfaces(networkArr);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void forceUpdate() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_NETWORK_USAGE_HISTORY", TAG);
        assertBandwidthControlEnabled();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            performPoll(3);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    protected void advisePersistThreshold(long j) {
        assertBandwidthControlEnabled();
        this.mPersistThreshold = MathUtils.constrain(j, 131072L, 2097152L);
        if (LOGV) {
            Slog.v(TAG, "advisePersistThreshold() given " + j + ", clamped to " + this.mPersistThreshold);
        }
        long jMillis = this.mClock.millis();
        synchronized (this.mStatsLock) {
            if (this.mSystemReady) {
                updatePersistThresholdsLocked();
                this.mDevRecorder.maybePersistLocked(jMillis);
                this.mXtRecorder.maybePersistLocked(jMillis);
                this.mUidRecorder.maybePersistLocked(jMillis);
                this.mUidTagRecorder.maybePersistLocked(jMillis);
                registerGlobalAlert();
            }
        }
    }

    public DataUsageRequest registerUsageCallback(String str, DataUsageRequest dataUsageRequest, Messenger messenger, IBinder iBinder) {
        Preconditions.checkNotNull(str, "calling package is null");
        Preconditions.checkNotNull(dataUsageRequest, "DataUsageRequest is null");
        Preconditions.checkNotNull(dataUsageRequest.template, "NetworkTemplate is null");
        Preconditions.checkNotNull(messenger, "messenger is null");
        Preconditions.checkNotNull(iBinder, "binder is null");
        int callingUid = Binder.getCallingUid();
        int iCheckAccessLevel = checkAccessLevel(str);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            DataUsageRequest dataUsageRequestRegister = this.mStatsObservers.register(dataUsageRequest, messenger, iBinder, callingUid, iCheckAccessLevel);
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            this.mHandler.sendMessage(this.mHandler.obtainMessage(1, 3));
            return dataUsageRequestRegister;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
    }

    public void unregisterUsageRequest(DataUsageRequest dataUsageRequest) {
        Preconditions.checkNotNull(dataUsageRequest, "DataUsageRequest is null");
        int callingUid = Binder.getCallingUid();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mStatsObservers.unregister(dataUsageRequest, callingUid);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public long getUidStats(int i, int i2) {
        return nativeGetUidStat(i, i2, checkBpfStatsEnable());
    }

    public long getIfaceStats(String str, int i) {
        return nativeGetIfaceStat(str, i, checkBpfStatsEnable());
    }

    public long getTotalStats(int i) {
        return nativeGetTotalStat(i, checkBpfStatsEnable());
    }

    private boolean checkBpfStatsEnable() {
        return this.mUseBpfTrafficStats;
    }

    @GuardedBy("mStatsLock")
    private void updatePersistThresholdsLocked() {
        this.mDevRecorder.setPersistThreshold(this.mSettings.getDevPersistBytes(this.mPersistThreshold));
        this.mXtRecorder.setPersistThreshold(this.mSettings.getXtPersistBytes(this.mPersistThreshold));
        this.mUidRecorder.setPersistThreshold(this.mSettings.getUidPersistBytes(this.mPersistThreshold));
        this.mUidTagRecorder.setPersistThreshold(this.mSettings.getUidTagPersistBytes(this.mPersistThreshold));
        this.mGlobalAlertBytes = this.mSettings.getGlobalAlertBytes(this.mPersistThreshold);
    }

    private void updateIfaces(Network[] networkArr) {
        synchronized (this.mStatsLock) {
            this.mWakeLock.acquire();
            try {
                updateIfacesLocked(networkArr);
            } finally {
                this.mWakeLock.release();
            }
        }
    }

    @GuardedBy("mStatsLock")
    private void updateIfacesLocked(Network[] networkArr) {
        if (this.mSystemReady) {
            if (LOGV) {
                Slog.v(TAG, "updateIfacesLocked()");
            }
            performPollLocked(1);
            try {
                NetworkState[] allNetworkState = this.mConnManager.getAllNetworkState();
                LinkProperties activeLinkProperties = this.mConnManager.getActiveLinkProperties();
                this.mActiveIface = activeLinkProperties != null ? activeLinkProperties.getInterfaceName() : null;
                this.mActiveIfaces.clear();
                this.mActiveUidIfaces.clear();
                if (networkArr != null) {
                    this.mDefaultNetworks = networkArr;
                }
                rebuildActiveVilteIfaceMap();
                ArraySet<String> arraySet = new ArraySet();
                for (NetworkState networkState : allNetworkState) {
                    if (networkState.networkInfo.isConnected()) {
                        boolean zIsNetworkTypeMobile = ConnectivityManager.isNetworkTypeMobile(networkState.networkInfo.getType());
                        NetworkIdentity networkIdentityBuildNetworkIdentity = NetworkIdentity.buildNetworkIdentity(this.mContext, networkState, ArrayUtils.contains(this.mDefaultNetworks, networkState.network));
                        Slog.i(TAG, "NetworkIdentity: " + networkIdentityBuildNetworkIdentity);
                        String interfaceName = networkState.linkProperties.getInterfaceName();
                        if (interfaceName != null) {
                            findOrCreateNetworkIdentitySet(this.mActiveIfaces, interfaceName).add(networkIdentityBuildNetworkIdentity);
                            findOrCreateNetworkIdentitySet(this.mActiveUidIfaces, interfaceName).add(networkIdentityBuildNetworkIdentity);
                            if (LOGV) {
                                Slog.i(TAG, "state.networkCapabilities: " + networkState.networkCapabilities);
                            }
                            if (networkState.networkCapabilities.hasCapability(4) && !networkIdentityBuildNetworkIdentity.getMetered()) {
                                NetworkIdentity networkIdentity = new NetworkIdentity(networkIdentityBuildNetworkIdentity.getType(), networkIdentityBuildNetworkIdentity.getSubType(), networkIdentityBuildNetworkIdentity.getSubscriberId(), networkIdentityBuildNetworkIdentity.getNetworkId(), networkIdentityBuildNetworkIdentity.getRoaming(), true, true);
                                if (!findOrCreateMultipleVilteNetworkIdentitySets(networkIdentity)) {
                                    findOrCreateNetworkIdentitySet(this.mActiveIfaces, VT_INTERFACE).add(networkIdentity);
                                    findOrCreateNetworkIdentitySet(this.mActiveUidIfaces, VT_INTERFACE).add(networkIdentity);
                                }
                            }
                            if (zIsNetworkTypeMobile) {
                                arraySet.add(interfaceName);
                            }
                        }
                        Iterator it = networkState.linkProperties.getStackedLinks().iterator();
                        while (it.hasNext()) {
                            String interfaceName2 = ((LinkProperties) it.next()).getInterfaceName();
                            if (interfaceName2 != null) {
                                findOrCreateNetworkIdentitySet(this.mActiveUidIfaces, interfaceName2).add(networkIdentityBuildNetworkIdentity);
                                if (zIsNetworkTypeMobile) {
                                    arraySet.add(interfaceName2);
                                }
                                NetworkStatsFactory.noteStackedIface(interfaceName2, interfaceName);
                            }
                        }
                    }
                }
                for (String str : arraySet) {
                    if (str != null && !ArrayUtils.contains(this.mMobileIfaces, str)) {
                        this.mMobileIfaces = (String[]) ArrayUtils.appendElement(String.class, this.mMobileIfaces, str);
                    }
                }
            } catch (RemoteException e) {
            }
        }
    }

    protected void rebuildActiveVilteIfaceMap() {
    }

    protected boolean findOrCreateMultipleVilteNetworkIdentitySets(NetworkIdentity networkIdentity) {
        return false;
    }

    protected static <K> NetworkIdentitySet findOrCreateNetworkIdentitySet(ArrayMap<K, NetworkIdentitySet> arrayMap, K k) {
        NetworkIdentitySet networkIdentitySet = arrayMap.get(k);
        if (networkIdentitySet == null) {
            NetworkIdentitySet networkIdentitySet2 = new NetworkIdentitySet();
            arrayMap.put(k, networkIdentitySet2);
            return networkIdentitySet2;
        }
        return networkIdentitySet;
    }

    @GuardedBy("mStatsLock")
    private void recordSnapshotLocked(long j) throws RemoteException {
        Trace.traceBegin(2097152L, "snapshotUid");
        NetworkStats networkStatsUidDetail = getNetworkStatsUidDetail(NetworkStats.INTERFACES_ALL);
        Trace.traceEnd(2097152L);
        Trace.traceBegin(2097152L, "snapshotXt");
        NetworkStats networkStatsXt = getNetworkStatsXt();
        Trace.traceEnd(2097152L);
        Trace.traceBegin(2097152L, "snapshotDev");
        NetworkStats networkStatsSummaryDev = this.mNetworkManager.getNetworkStatsSummaryDev();
        Trace.traceEnd(2097152L);
        Trace.traceBegin(2097152L, "snapshotTether");
        NetworkStats networkStatsTethering = getNetworkStatsTethering(0);
        Trace.traceEnd(2097152L);
        networkStatsXt.combineAllValues(networkStatsTethering);
        networkStatsSummaryDev.combineAllValues(networkStatsTethering);
        Trace.traceBegin(2097152L, "recordDev");
        this.mDevRecorder.recordSnapshotLocked(networkStatsSummaryDev, this.mActiveIfaces, null, j);
        Trace.traceEnd(2097152L);
        Trace.traceBegin(2097152L, "recordXt");
        this.mXtRecorder.recordSnapshotLocked(networkStatsXt, this.mActiveIfaces, null, j);
        Trace.traceEnd(2097152L);
        VpnInfo[] allVpnInfo = this.mConnManager.getAllVpnInfo();
        Trace.traceBegin(2097152L, "recordUid");
        this.mUidRecorder.recordSnapshotLocked(networkStatsUidDetail, this.mActiveUidIfaces, allVpnInfo, j);
        Trace.traceEnd(2097152L);
        Trace.traceBegin(2097152L, "recordUidTag");
        this.mUidTagRecorder.recordSnapshotLocked(networkStatsUidDetail, this.mActiveUidIfaces, allVpnInfo, j);
        Trace.traceEnd(2097152L);
        this.mStatsObservers.updateStats(networkStatsXt, networkStatsUidDetail, new ArrayMap<>(this.mActiveIfaces), new ArrayMap<>(this.mActiveUidIfaces), allVpnInfo, j);
    }

    @GuardedBy("mStatsLock")
    private void bootstrapStatsLocked() {
        try {
            recordSnapshotLocked(this.mClock.millis());
        } catch (RemoteException e) {
        } catch (IllegalStateException e2) {
            Slog.w(TAG, "problem reading network stats: " + e2);
        }
    }

    private void performPoll(int i) {
        synchronized (this.mStatsLock) {
            this.mWakeLock.acquire();
            try {
                performPollLocked(i);
            } finally {
                this.mWakeLock.release();
            }
        }
    }

    @GuardedBy("mStatsLock")
    private void performPollLocked(int i) {
        if (this.mSystemReady) {
            if (LOGV) {
                Slog.v(TAG, "performPollLocked(flags=0x" + Integer.toHexString(i) + ")");
            }
            Trace.traceBegin(2097152L, "performPollLocked");
            boolean z = (i & 1) != 0;
            boolean z2 = (i & 2) != 0;
            boolean z3 = (i & 256) != 0;
            long jMillis = this.mClock.millis();
            try {
                recordSnapshotLocked(jMillis);
                Trace.traceBegin(2097152L, "[persisting]");
                if (z3) {
                    this.mDevRecorder.forcePersistLocked(jMillis);
                    this.mXtRecorder.forcePersistLocked(jMillis);
                    this.mUidRecorder.forcePersistLocked(jMillis);
                    this.mUidTagRecorder.forcePersistLocked(jMillis);
                } else {
                    if (z) {
                        this.mDevRecorder.maybePersistLocked(jMillis);
                        this.mXtRecorder.maybePersistLocked(jMillis);
                    }
                    if (z2) {
                        this.mUidRecorder.maybePersistLocked(jMillis);
                        this.mUidTagRecorder.maybePersistLocked(jMillis);
                    }
                }
                Trace.traceEnd(2097152L);
                if (this.mSettings.getSampleEnabled()) {
                    performSampleLocked();
                }
                Intent intent = new Intent(ACTION_NETWORK_STATS_UPDATED);
                intent.setFlags(1073741824);
                this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, "android.permission.READ_NETWORK_USAGE_HISTORY");
                Trace.traceEnd(2097152L);
            } catch (RemoteException e) {
            } catch (IllegalStateException e2) {
                Log.wtf(TAG, "problem reading network stats", e2);
            }
        }
    }

    @GuardedBy("mStatsLock")
    private void performSampleLocked() {
        long jMillis = this.mClock.millis();
        NetworkTemplate networkTemplateBuildTemplateMobileWildcard = NetworkTemplate.buildTemplateMobileWildcard();
        NetworkStats.Entry totalSinceBootLocked = this.mDevRecorder.getTotalSinceBootLocked(networkTemplateBuildTemplateMobileWildcard);
        NetworkStats.Entry totalSinceBootLocked2 = this.mXtRecorder.getTotalSinceBootLocked(networkTemplateBuildTemplateMobileWildcard);
        NetworkStats.Entry totalSinceBootLocked3 = this.mUidRecorder.getTotalSinceBootLocked(networkTemplateBuildTemplateMobileWildcard);
        EventLogTags.writeNetstatsMobileSample(totalSinceBootLocked.rxBytes, totalSinceBootLocked.rxPackets, totalSinceBootLocked.txBytes, totalSinceBootLocked.txPackets, totalSinceBootLocked2.rxBytes, totalSinceBootLocked2.rxPackets, totalSinceBootLocked2.txBytes, totalSinceBootLocked2.txPackets, totalSinceBootLocked3.rxBytes, totalSinceBootLocked3.rxPackets, totalSinceBootLocked3.txBytes, totalSinceBootLocked3.txPackets, jMillis);
        NetworkTemplate networkTemplateBuildTemplateWifiWildcard = NetworkTemplate.buildTemplateWifiWildcard();
        NetworkStats.Entry totalSinceBootLocked4 = this.mDevRecorder.getTotalSinceBootLocked(networkTemplateBuildTemplateWifiWildcard);
        NetworkStats.Entry totalSinceBootLocked5 = this.mXtRecorder.getTotalSinceBootLocked(networkTemplateBuildTemplateWifiWildcard);
        NetworkStats.Entry totalSinceBootLocked6 = this.mUidRecorder.getTotalSinceBootLocked(networkTemplateBuildTemplateWifiWildcard);
        EventLogTags.writeNetstatsWifiSample(totalSinceBootLocked4.rxBytes, totalSinceBootLocked4.rxPackets, totalSinceBootLocked4.txBytes, totalSinceBootLocked4.txPackets, totalSinceBootLocked5.rxBytes, totalSinceBootLocked5.rxPackets, totalSinceBootLocked5.txBytes, totalSinceBootLocked5.txPackets, totalSinceBootLocked6.rxBytes, totalSinceBootLocked6.rxPackets, totalSinceBootLocked6.txBytes, totalSinceBootLocked6.txPackets, jMillis);
    }

    @GuardedBy("mStatsLock")
    private void removeUidsLocked(int... iArr) {
        if (LOGV) {
            Slog.v(TAG, "removeUidsLocked() for UIDs " + Arrays.toString(iArr));
        }
        performPollLocked(3);
        this.mUidRecorder.removeUidsLocked(iArr);
        this.mUidTagRecorder.removeUidsLocked(iArr);
        for (int i : iArr) {
            NetworkManagementSocketTagger.resetKernelUidStats(i);
        }
    }

    @GuardedBy("mStatsLock")
    private void removeUserLocked(int i) {
        if (LOGV) {
            Slog.v(TAG, "removeUserLocked() for userId=" + i);
        }
        int[] iArrAppendInt = new int[0];
        Iterator<ApplicationInfo> it = this.mContext.getPackageManager().getInstalledApplications(4194816).iterator();
        while (it.hasNext()) {
            iArrAppendInt = ArrayUtils.appendInt(iArrAppendInt, UserHandle.getUid(i, it.next().uid));
        }
        removeUidsLocked(iArrAppendInt);
    }

    private class NetworkStatsManagerInternalImpl extends NetworkStatsManagerInternal {
        private NetworkStatsManagerInternalImpl() {
        }

        @Override
        public long getNetworkTotalBytes(NetworkTemplate networkTemplate, long j, long j2) {
            Trace.traceBegin(2097152L, "getNetworkTotalBytes");
            try {
                return NetworkStatsService.this.getNetworkTotalBytes(networkTemplate, j, j2);
            } finally {
                Trace.traceEnd(2097152L);
            }
        }

        @Override
        public NetworkStats getNetworkUidBytes(NetworkTemplate networkTemplate, long j, long j2) {
            Trace.traceBegin(2097152L, "getNetworkUidBytes");
            try {
                return NetworkStatsService.this.getNetworkUidBytes(networkTemplate, j, j2);
            } finally {
                Trace.traceEnd(2097152L);
            }
        }

        @Override
        public void setUidForeground(int i, boolean z) {
            NetworkStatsService.this.setUidForeground(i, z);
        }

        @Override
        public void advisePersistThreshold(long j) {
            NetworkStatsService.this.advisePersistThreshold(j);
        }

        @Override
        public void forceUpdate() {
            NetworkStatsService.this.forceUpdate();
        }
    }

    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        SparseIntArray sparseIntArrayClone;
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, printWriter)) {
            HashSet hashSet = new HashSet();
            long j = 86400000;
            for (String str : strArr) {
                hashSet.add(str);
                if (str.startsWith("--duration=")) {
                    try {
                        j = Long.parseLong(str.substring(11));
                    } catch (NumberFormatException e) {
                    }
                }
            }
            boolean z = hashSet.contains("--poll") || hashSet.contains("poll");
            boolean zContains = hashSet.contains("--checkin");
            boolean z2 = hashSet.contains("--full") || hashSet.contains("full");
            boolean z3 = hashSet.contains("--uid") || hashSet.contains("detail");
            boolean z4 = hashSet.contains("--tag") || hashSet.contains("detail");
            IndentingPrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "  ");
            synchronized (this.mStatsLock) {
                if (strArr.length > 0 && PriorityDump.PROTO_ARG.equals(strArr[0])) {
                    dumpProtoLocked(fileDescriptor);
                    return;
                }
                if (z) {
                    performPollLocked(259);
                    indentingPrintWriter.println("Forced poll");
                    return;
                }
                if (zContains) {
                    long jCurrentTimeMillis = System.currentTimeMillis();
                    long j2 = jCurrentTimeMillis - j;
                    indentingPrintWriter.print("v1,");
                    indentingPrintWriter.print(j2 / 1000);
                    indentingPrintWriter.print(',');
                    indentingPrintWriter.print(jCurrentTimeMillis / 1000);
                    indentingPrintWriter.println();
                    indentingPrintWriter.println(PREFIX_XT);
                    this.mXtRecorder.dumpCheckin(printWriter, j2, jCurrentTimeMillis);
                    if (z3) {
                        indentingPrintWriter.println("uid");
                        this.mUidRecorder.dumpCheckin(printWriter, j2, jCurrentTimeMillis);
                    }
                    if (z4) {
                        indentingPrintWriter.println("tag");
                        this.mUidTagRecorder.dumpCheckin(printWriter, j2, jCurrentTimeMillis);
                    }
                    return;
                }
                indentingPrintWriter.println("Active interfaces:");
                indentingPrintWriter.increaseIndent();
                for (int i = 0; i < this.mActiveIfaces.size(); i++) {
                    indentingPrintWriter.printPair("iface", this.mActiveIfaces.keyAt(i));
                    indentingPrintWriter.printPair("ident", this.mActiveIfaces.valueAt(i));
                    indentingPrintWriter.println();
                }
                indentingPrintWriter.decreaseIndent();
                indentingPrintWriter.println("Active UID interfaces:");
                indentingPrintWriter.increaseIndent();
                for (int i2 = 0; i2 < this.mActiveUidIfaces.size(); i2++) {
                    indentingPrintWriter.printPair("iface", this.mActiveUidIfaces.keyAt(i2));
                    indentingPrintWriter.printPair("ident", this.mActiveUidIfaces.valueAt(i2));
                    indentingPrintWriter.println();
                }
                indentingPrintWriter.decreaseIndent();
                synchronized (this.mOpenSessionCallsPerUid) {
                    sparseIntArrayClone = this.mOpenSessionCallsPerUid.clone();
                }
                int size = sparseIntArrayClone.size();
                long[] jArr = new long[size];
                int i3 = 0;
                while (i3 < size) {
                    jArr[i3] = (((long) sparseIntArrayClone.valueAt(i3)) << 32) | ((long) sparseIntArrayClone.keyAt(i3));
                    i3++;
                    z3 = z3;
                }
                boolean z5 = z3;
                Arrays.sort(jArr);
                indentingPrintWriter.println("Top openSession callers (uid=count):");
                indentingPrintWriter.increaseIndent();
                int iMax = Math.max(0, size - 20);
                for (int i4 = size - 1; i4 >= iMax; i4--) {
                    int i5 = (int) (jArr[i4] & (-1));
                    int i6 = (int) (jArr[i4] >> 32);
                    indentingPrintWriter.print(i5);
                    indentingPrintWriter.print("=");
                    indentingPrintWriter.println(i6);
                }
                indentingPrintWriter.decreaseIndent();
                indentingPrintWriter.println();
                indentingPrintWriter.println("Dev stats:");
                indentingPrintWriter.increaseIndent();
                this.mDevRecorder.dumpLocked(indentingPrintWriter, z2);
                indentingPrintWriter.decreaseIndent();
                indentingPrintWriter.println("Xt stats:");
                indentingPrintWriter.increaseIndent();
                this.mXtRecorder.dumpLocked(indentingPrintWriter, z2);
                indentingPrintWriter.decreaseIndent();
                if (z5) {
                    indentingPrintWriter.println("UID stats:");
                    indentingPrintWriter.increaseIndent();
                    this.mUidRecorder.dumpLocked(indentingPrintWriter, z2);
                    indentingPrintWriter.decreaseIndent();
                }
                if (z4) {
                    indentingPrintWriter.println("UID tag stats:");
                    indentingPrintWriter.increaseIndent();
                    this.mUidTagRecorder.dumpLocked(indentingPrintWriter, z2);
                    indentingPrintWriter.decreaseIndent();
                }
            }
        }
    }

    @GuardedBy("mStatsLock")
    private void dumpProtoLocked(FileDescriptor fileDescriptor) {
        ProtoOutputStream protoOutputStream = new ProtoOutputStream(fileDescriptor);
        dumpInterfaces(protoOutputStream, 2246267895809L, this.mActiveIfaces);
        dumpInterfaces(protoOutputStream, 2246267895810L, this.mActiveUidIfaces);
        this.mDevRecorder.writeToProtoLocked(protoOutputStream, 1146756268035L);
        this.mXtRecorder.writeToProtoLocked(protoOutputStream, 1146756268036L);
        this.mUidRecorder.writeToProtoLocked(protoOutputStream, 1146756268037L);
        this.mUidTagRecorder.writeToProtoLocked(protoOutputStream, 1146756268038L);
        protoOutputStream.flush();
    }

    private static void dumpInterfaces(ProtoOutputStream protoOutputStream, long j, ArrayMap<String, NetworkIdentitySet> arrayMap) {
        for (int i = 0; i < arrayMap.size(); i++) {
            long jStart = protoOutputStream.start(j);
            protoOutputStream.write(1138166333441L, arrayMap.keyAt(i));
            arrayMap.valueAt(i).writeToProto(protoOutputStream, 1146756268034L);
            protoOutputStream.end(jStart);
        }
    }

    private NetworkStats getNetworkStatsUidDetail(String[] strArr) throws RemoteException {
        NetworkStats networkStatsUidDetail = this.mNetworkManager.getNetworkStatsUidDetail(-1, strArr);
        NetworkStats networkStatsTethering = getNetworkStatsTethering(1);
        networkStatsTethering.filter(-1, strArr, -1);
        NetworkStatsFactory.apply464xlatAdjustments(networkStatsUidDetail, networkStatsTethering);
        networkStatsUidDetail.combineAllValues(networkStatsTethering);
        NetworkStats vtDataUsage = ((TelephonyManager) this.mContext.getSystemService("phone")).getVtDataUsage(1);
        if (vtDataUsage != null) {
            vtDataUsage.filter(-1, strArr, -1);
            NetworkStatsFactory.apply464xlatAdjustments(networkStatsUidDetail, vtDataUsage);
            networkStatsUidDetail.combineAllValues(vtDataUsage);
        }
        networkStatsUidDetail.combineAllValues(this.mUidOperations);
        return networkStatsUidDetail;
    }

    private NetworkStats getNetworkStatsXt() throws RemoteException {
        NetworkStats networkStatsSummaryXt = this.mNetworkManager.getNetworkStatsSummaryXt();
        NetworkStats vtDataUsage = ((TelephonyManager) this.mContext.getSystemService("phone")).getVtDataUsage(0);
        if (vtDataUsage != null) {
            networkStatsSummaryXt.combineAllValues(vtDataUsage);
        }
        return networkStatsSummaryXt;
    }

    private NetworkStats getNetworkStatsTethering(int i) throws RemoteException {
        try {
            return this.mNetworkManager.getNetworkStatsTethering(i);
        } catch (IllegalStateException e) {
            Log.e(TAG, "problem reading network stats", e);
            return new NetworkStats(0L, 10);
        }
    }

    @VisibleForTesting
    static class HandlerCallback implements Handler.Callback {
        private final NetworkStatsService mService;

        HandlerCallback(NetworkStatsService networkStatsService) {
            this.mService = networkStatsService;
        }

        @Override
        public boolean handleMessage(Message message) {
            if (NetworkStatsService.LOGV) {
                Log.v(NetworkStatsService.TAG, "handleMessage(): msg=" + message.what);
            }
            switch (message.what) {
                case 1:
                    this.mService.performPoll(message.arg1);
                    break;
                case 2:
                    this.mService.updateIfaces(null);
                    break;
                case 3:
                    this.mService.registerGlobalAlert();
                    break;
            }
            return true;
        }
    }

    private void assertSystemReady() {
        if (!this.mSystemReady) {
            throw new IllegalStateException("System not ready");
        }
    }

    private void assertBandwidthControlEnabled() {
        if (!isBandwidthControlEnabled()) {
            throw new IllegalStateException("Bandwidth module disabled");
        }
    }

    private boolean isBandwidthControlEnabled() {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            return this.mNetworkManager.isBandwidthControlEnabled();
        } catch (RemoteException e) {
            return false;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private class DropBoxNonMonotonicObserver implements NetworkStats.NonMonotonicObserver<String> {
        private DropBoxNonMonotonicObserver() {
        }

        public void foundNonMonotonic(NetworkStats networkStats, int i, NetworkStats networkStats2, int i2, String str) {
            Log.w(NetworkStatsService.TAG, "Found non-monotonic values; saving to dropbox");
            StringBuilder sb = new StringBuilder();
            sb.append("found non-monotonic " + str + " values at left[" + i + "] - right[" + i2 + "]\n");
            sb.append("left=");
            sb.append(networkStats);
            sb.append('\n');
            sb.append("right=");
            sb.append(networkStats2);
            sb.append('\n');
            ((DropBoxManager) NetworkStatsService.this.mContext.getSystemService(DropBoxManager.class)).addText(NetworkStatsService.TAG_NETSTATS_ERROR, sb.toString());
        }

        public void foundNonMonotonic(NetworkStats networkStats, int i, String str) {
            Log.w(NetworkStatsService.TAG, "Found non-monotonic values; saving to dropbox");
            StringBuilder sb = new StringBuilder();
            sb.append("Found non-monotonic " + str + " values at [" + i + "]\n");
            sb.append("stats=");
            sb.append(networkStats);
            sb.append('\n');
            ((DropBoxManager) NetworkStatsService.this.mContext.getSystemService(DropBoxManager.class)).addText(NetworkStatsService.TAG_NETSTATS_ERROR, sb.toString());
        }
    }

    private static class DefaultNetworkStatsSettings implements NetworkStatsSettings {
        private final ContentResolver mResolver;

        public DefaultNetworkStatsSettings(Context context) {
            this.mResolver = (ContentResolver) Preconditions.checkNotNull(context.getContentResolver());
        }

        private long getGlobalLong(String str, long j) {
            return Settings.Global.getLong(this.mResolver, str, j);
        }

        private boolean getGlobalBoolean(String str, boolean z) {
            return Settings.Global.getInt(this.mResolver, str, z ? 1 : 0) != 0;
        }

        @Override
        public long getPollInterval() {
            return getGlobalLong("netstats_poll_interval", BackupAgentTimeoutParameters.DEFAULT_SHARED_BACKUP_AGENT_TIMEOUT_MILLIS);
        }

        @Override
        public long getGlobalAlertBytes(long j) {
            return getGlobalLong("netstats_global_alert_bytes", j);
        }

        @Override
        public boolean getSampleEnabled() {
            return getGlobalBoolean("netstats_sample_enabled", true);
        }

        @Override
        public boolean getAugmentEnabled() {
            return getGlobalBoolean("netstats_augment_enabled", true);
        }

        @Override
        public NetworkStatsSettings.Config getDevConfig() {
            return new NetworkStatsSettings.Config(getGlobalLong("netstats_dev_bucket_duration", AppStandbyController.SettingsObserver.DEFAULT_STRONG_USAGE_TIMEOUT), getGlobalLong("netstats_dev_rotate_age", 1296000000L), getGlobalLong("netstats_dev_delete_age", 7776000000L));
        }

        @Override
        public NetworkStatsSettings.Config getXtConfig() {
            return getDevConfig();
        }

        @Override
        public NetworkStatsSettings.Config getUidConfig() {
            return new NetworkStatsSettings.Config(getGlobalLong("netstats_uid_bucket_duration", AppStandbyController.SettingsObserver.DEFAULT_SYSTEM_UPDATE_TIMEOUT), getGlobalLong("netstats_uid_rotate_age", 1296000000L), getGlobalLong("netstats_uid_delete_age", 7776000000L));
        }

        @Override
        public NetworkStatsSettings.Config getUidTagConfig() {
            return new NetworkStatsSettings.Config(getGlobalLong("netstats_uid_tag_bucket_duration", AppStandbyController.SettingsObserver.DEFAULT_SYSTEM_UPDATE_TIMEOUT), getGlobalLong("netstats_uid_tag_rotate_age", 432000000L), getGlobalLong("netstats_uid_tag_delete_age", 1296000000L));
        }

        @Override
        public long getDevPersistBytes(long j) {
            return getGlobalLong("netstats_dev_persist_bytes", j);
        }

        @Override
        public long getXtPersistBytes(long j) {
            return getDevPersistBytes(j);
        }

        @Override
        public long getUidPersistBytes(long j) {
            return getGlobalLong("netstats_uid_persist_bytes", j);
        }

        @Override
        public long getUidTagPersistBytes(long j) {
            return getGlobalLong("netstats_uid_tag_persist_bytes", j);
        }
    }
}
