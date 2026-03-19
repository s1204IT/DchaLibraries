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
import android.net.NetworkIdentity;
import android.net.NetworkState;
import android.net.NetworkStats;
import android.net.NetworkStatsHistory;
import android.net.NetworkTemplate;
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
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.MathUtils;
import android.util.NtpTrustedTime;
import android.util.Slog;
import android.util.SparseIntArray;
import android.util.TrustedTime;
import com.android.internal.net.VpnInfo;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FileRotator;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.server.EventLogTags;
import com.android.server.NetworkManagementService;
import com.android.server.NetworkManagementSocketTagger;
import com.android.server.usage.UnixCalendar;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class NetworkStatsService extends INetworkStatsService.Stub {
    public static final String ACTION_NETWORK_STATS_POLL = "com.android.server.action.NETWORK_STATS_POLL";
    public static final String ACTION_NETWORK_STATS_UPDATED = "com.android.server.action.NETWORK_STATS_UPDATED";
    private static final int FLAG_PERSIST_ALL = 3;
    private static final int FLAG_PERSIST_FORCE = 256;
    private static final int FLAG_PERSIST_NETWORK = 1;
    private static final int FLAG_PERSIST_UID = 2;
    private static final boolean LOGV = true;
    private static final int MSG_PERFORM_POLL = 1;
    private static final int MSG_REGISTER_GLOBAL_ALERT = 3;
    private static final int MSG_UPDATE_IFACES = 2;
    private static final String PREFIX_DEV = "dev";
    private static final String PREFIX_UID = "uid";
    private static final String PREFIX_UID_TAG = "uid_tag";
    private static final String PREFIX_XT = "xt";
    private static final String TAG = "NetworkStats";
    private static final String TAG_NETSTATS_ERROR = "netstats_error";
    public static boolean USE_TRUESTED_TIME = false;
    private String mActiveIface;
    private final AlarmManager mAlarmManager;
    private final File mBaseDir;
    private IConnectivityManager mConnManager;
    private final Context mContext;
    private NetworkStatsRecorder mDevRecorder;
    private long mGlobalAlertBytes;
    private Handler mHandler;
    private Handler.Callback mHandlerCallback;
    private final INetworkManagementService mNetworkManager;
    private PendingIntent mPollIntent;
    private final NetworkStatsSettings mSettings;
    private final NetworkStatsObservers mStatsObservers;
    private final File mSystemDir;
    private boolean mSystemReady;
    private final TelephonyManager mTeleManager;
    private final TrustedTime mTime;
    private NetworkStatsRecorder mUidRecorder;
    private NetworkStatsRecorder mUidTagRecorder;
    private final PowerManager.WakeLock mWakeLock;
    private NetworkStatsRecorder mXtRecorder;
    private NetworkStatsCollection mXtStatsCached;
    private final Object mStatsLock = new Object();
    private final ArrayMap<String, NetworkIdentitySet> mActiveIfaces = new ArrayMap<>();
    private final ArrayMap<String, NetworkIdentitySet> mActiveUidIfaces = new ArrayMap<>();
    private String[] mMobileIfaces = new String[0];
    private final DropBoxNonMonotonicObserver mNonMonotonicObserver = new DropBoxNonMonotonicObserver(this, null);
    private SparseIntArray mActiveUidCounterSet = new SparseIntArray();
    private NetworkStats mUidOperations = new NetworkStats(0, 10);
    private long mPersistThreshold = 2097152;
    private boolean mIgnoreAlert = false;
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
            int uid = intent.getIntExtra("android.intent.extra.UID", -1);
            if (uid == -1) {
                return;
            }
            synchronized (NetworkStatsService.this.mStatsLock) {
                NetworkStatsService.this.mWakeLock.acquire();
                try {
                    NetworkStatsService.this.removeUidsLocked(uid);
                } finally {
                    NetworkStatsService.this.mWakeLock.release();
                }
            }
        }
    };
    private BroadcastReceiver mUserReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int userId = intent.getIntExtra("android.intent.extra.user_handle", -1);
            if (userId == -1) {
                return;
            }
            synchronized (NetworkStatsService.this.mStatsLock) {
                NetworkStatsService.this.mWakeLock.acquire();
                try {
                    NetworkStatsService.this.removeUserLocked(userId);
                } finally {
                    NetworkStatsService.this.mWakeLock.release();
                }
            }
        }
    };
    private BroadcastReceiver mIgnoreAlertReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e(NetworkStatsService.TAG, "[powerdebug]ignoreAlertFilter received");
            NetworkStatsService.this.mIgnoreAlert = true;
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
        public void limitReached(String limitName, String iface) {
            NetworkStatsService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", NetworkStatsService.TAG);
            if (NetworkStatsService.this.mIgnoreAlert) {
                Log.d(NetworkStatsService.TAG, "[powerdebug]IGNORE_DATA_USAGE_ALERT received, mIgnoreAlert=" + NetworkStatsService.this.mIgnoreAlert);
            }
            if (!NetworkManagementService.LIMIT_GLOBAL_ALERT.equals(limitName) || NetworkStatsService.this.mIgnoreAlert) {
                return;
            }
            NetworkStatsService.this.mHandler.obtainMessage(1, 1, 0).sendToTarget();
            NetworkStatsService.this.mHandler.obtainMessage(3).sendToTarget();
        }
    };

    public interface NetworkStatsSettings {
        Config getDevConfig();

        long getDevPersistBytes(long j);

        long getGlobalAlertBytes(long j);

        long getPollInterval();

        boolean getSampleEnabled();

        long getTimeCacheMaxAge();

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

            public Config(long bucketDuration, long rotateAgeMillis, long deleteAgeMillis) {
                this.bucketDuration = bucketDuration;
                this.rotateAgeMillis = rotateAgeMillis;
                this.deleteAgeMillis = deleteAgeMillis;
            }
        }
    }

    private static File getDefaultSystemDir() {
        return new File(Environment.getDataDirectory(), "system");
    }

    private static File getDefaultBaseDir() {
        File baseDir = new File(getDefaultSystemDir(), "netstats");
        baseDir.mkdirs();
        return baseDir;
    }

    public static NetworkStatsService create(Context context, INetworkManagementService networkManager) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService("alarm");
        PowerManager powerManager = (PowerManager) context.getSystemService("power");
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(1, TAG);
        NetworkStatsService service = new NetworkStatsService(context, networkManager, alarmManager, wakeLock, NtpTrustedTime.getInstance(context), TelephonyManager.getDefault(), new DefaultNetworkStatsSettings(context), new NetworkStatsObservers(), getDefaultSystemDir(), getDefaultBaseDir());
        HandlerThread handlerThread = new HandlerThread(TAG);
        Handler.Callback callback = new HandlerCallback(service);
        handlerThread.start();
        Handler handler = new Handler(handlerThread.getLooper(), callback);
        service.setHandler(handler, callback);
        return service;
    }

    NetworkStatsService(Context context, INetworkManagementService networkManager, AlarmManager alarmManager, PowerManager.WakeLock wakeLock, TrustedTime time, TelephonyManager teleManager, NetworkStatsSettings settings, NetworkStatsObservers statsObservers, File systemDir, File baseDir) {
        this.mContext = (Context) Preconditions.checkNotNull(context, "missing Context");
        this.mNetworkManager = (INetworkManagementService) Preconditions.checkNotNull(networkManager, "missing INetworkManagementService");
        this.mAlarmManager = (AlarmManager) Preconditions.checkNotNull(alarmManager, "missing AlarmManager");
        this.mTime = (TrustedTime) Preconditions.checkNotNull(time, "missing TrustedTime");
        this.mSettings = (NetworkStatsSettings) Preconditions.checkNotNull(settings, "missing NetworkStatsSettings");
        this.mTeleManager = (TelephonyManager) Preconditions.checkNotNull(teleManager, "missing TelephonyManager");
        this.mWakeLock = (PowerManager.WakeLock) Preconditions.checkNotNull(wakeLock, "missing WakeLock");
        this.mStatsObservers = (NetworkStatsObservers) Preconditions.checkNotNull(statsObservers, "missing NetworkStatsObservers");
        this.mSystemDir = (File) Preconditions.checkNotNull(systemDir, "missing systemDir");
        this.mBaseDir = (File) Preconditions.checkNotNull(baseDir, "missing baseDir");
    }

    void setHandler(Handler handler, Handler.Callback callback) {
        this.mHandler = handler;
        this.mHandlerCallback = callback;
        this.mIgnoreAlert = false;
    }

    public void bindConnectivityManager(IConnectivityManager connManager) {
        this.mConnManager = (IConnectivityManager) Preconditions.checkNotNull(connManager, "missing IConnectivityManager");
    }

    public void systemReady() {
        this.mSystemReady = true;
        if (!isBandwidthControlEnabled()) {
            Slog.w(TAG, "bandwidth controls disabled, unable to track stats");
            return;
        }
        this.mDevRecorder = buildRecorder(PREFIX_DEV, this.mSettings.getDevConfig(), false);
        this.mXtRecorder = buildRecorder(PREFIX_XT, this.mSettings.getXtConfig(), false);
        this.mUidRecorder = buildRecorder("uid", this.mSettings.getUidConfig(), false);
        this.mUidTagRecorder = buildRecorder(PREFIX_UID_TAG, this.mSettings.getUidTagConfig(), true);
        updatePersistThresholds();
        synchronized (this.mStatsLock) {
            maybeUpgradeLegacyStatsLocked();
            this.mXtStatsCached = this.mXtRecorder.getOrLoadCompleteLocked();
            bootstrapStatsLocked();
        }
        IntentFilter tetherFilter = new IntentFilter("android.net.conn.TETHER_STATE_CHANGED");
        this.mContext.registerReceiver(this.mTetherReceiver, tetherFilter, null, this.mHandler);
        IntentFilter pollFilter = new IntentFilter(ACTION_NETWORK_STATS_POLL);
        this.mContext.registerReceiver(this.mPollReceiver, pollFilter, "android.permission.READ_NETWORK_USAGE_HISTORY", this.mHandler);
        IntentFilter removedFilter = new IntentFilter("android.intent.action.UID_REMOVED");
        this.mContext.registerReceiver(this.mRemovedReceiver, removedFilter, null, this.mHandler);
        IntentFilter userFilter = new IntentFilter("android.intent.action.USER_REMOVED");
        this.mContext.registerReceiver(this.mUserReceiver, userFilter, null, this.mHandler);
        IntentFilter shutdownFilter = new IntentFilter("android.intent.action.ACTION_SHUTDOWN");
        shutdownFilter.addAction("android.intent.action.ACTION_SHUTDOWN_IPO");
        this.mContext.registerReceiver(this.mShutdownReceiver, shutdownFilter);
        IntentFilter ignoreAlertFilter = new IntentFilter("android.intent.action.IGNORE_DATA_USAGE_ALERT");
        ignoreAlertFilter.addAction("android.intent.action.IGNORE_DATA_USAGE_ALERT");
        this.mContext.registerReceiver(this.mIgnoreAlertReceiver, ignoreAlertFilter);
        try {
            this.mNetworkManager.registerObserver(this.mAlertObserver);
        } catch (RemoteException e) {
        }
        registerPollAlarmLocked();
        registerGlobalAlert();
    }

    private NetworkStatsRecorder buildRecorder(String prefix, NetworkStatsSettings.Config config, boolean includeTags) {
        DropBoxManager dropBox = (DropBoxManager) this.mContext.getSystemService("dropbox");
        return new NetworkStatsRecorder(new FileRotator(this.mBaseDir, prefix, config.rotateAgeMillis, config.deleteAgeMillis), this.mNonMonotonicObserver, dropBox, prefix, config.bucketDuration, includeTags);
    }

    private void shutdownLocked() {
        long currentTime = (this.mTime.hasCache() && USE_TRUESTED_TIME) ? this.mTime.currentTimeMillis() : System.currentTimeMillis();
        this.mDevRecorder.forcePersistLocked(currentTime);
        this.mXtRecorder.forcePersistLocked(currentTime);
        this.mUidRecorder.forcePersistLocked(currentTime);
        this.mUidTagRecorder.forcePersistLocked(currentTime);
    }

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
            if (!file3.exists()) {
                return;
            }
            this.mUidRecorder.importLegacyUidLocked(file3);
            this.mUidTagRecorder.importLegacyUidLocked(file3);
            file3.delete();
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
        long currentRealtime = SystemClock.elapsedRealtime();
        this.mAlarmManager.setInexactRepeating(3, currentRealtime, this.mSettings.getPollInterval(), this.mPollIntent);
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
        return createSession(null, false);
    }

    public INetworkStatsSession openSessionForUsageStats(String callingPackage) {
        return createSession(callingPackage, true);
    }

    private INetworkStatsSession createSession(final String callingPackage, boolean pollOnCreate) {
        assertBandwidthControlEnabled();
        if (pollOnCreate) {
            long ident = Binder.clearCallingIdentity();
            try {
                performPoll(3);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
        return new INetworkStatsSession.Stub() {
            private String mCallingPackage;
            private NetworkStatsCollection mUidComplete;
            private NetworkStatsCollection mUidTagComplete;

            {
                this.mCallingPackage = callingPackage;
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
                return getUidComplete().getRelevantUids(NetworkStatsService.this.checkAccessLevel(this.mCallingPackage));
            }

            public NetworkStats getDeviceSummaryForNetwork(NetworkTemplate template, long start, long end) {
                int accessLevel = NetworkStatsService.this.checkAccessLevel(this.mCallingPackage);
                if (accessLevel < 2) {
                    throw new SecurityException("Calling package " + this.mCallingPackage + " cannot access device summary network stats");
                }
                NetworkStats result = new NetworkStats(end - start, 1);
                long ident2 = Binder.clearCallingIdentity();
                try {
                    result.combineAllValues(NetworkStatsService.this.internalGetSummaryForNetwork(template, start, end, 3));
                    return result;
                } finally {
                    Binder.restoreCallingIdentity(ident2);
                }
            }

            public NetworkStats getSummaryForNetwork(NetworkTemplate template, long start, long end) {
                int accessLevel = NetworkStatsService.this.checkAccessLevel(this.mCallingPackage);
                return NetworkStatsService.this.internalGetSummaryForNetwork(template, start, end, accessLevel);
            }

            public NetworkStatsHistory getHistoryForNetwork(NetworkTemplate template, int fields) {
                int accessLevel = NetworkStatsService.this.checkAccessLevel(this.mCallingPackage);
                return NetworkStatsService.this.internalGetHistoryForNetwork(template, fields, accessLevel);
            }

            public NetworkStats getSummaryForAllUid(NetworkTemplate template, long start, long end, boolean includeTags) {
                int accessLevel = NetworkStatsService.this.checkAccessLevel(this.mCallingPackage);
                NetworkStats stats = getUidComplete().getSummary(template, start, end, accessLevel);
                if (includeTags) {
                    NetworkStats tagStats = getUidTagComplete().getSummary(template, start, end, accessLevel);
                    stats.combineAllValues(tagStats);
                }
                return stats;
            }

            public NetworkStatsHistory getHistoryForUid(NetworkTemplate template, int uid, int set, int tag, int fields) {
                int accessLevel = NetworkStatsService.this.checkAccessLevel(this.mCallingPackage);
                if (tag == 0) {
                    return getUidComplete().getHistory(template, uid, set, tag, fields, accessLevel);
                }
                return getUidTagComplete().getHistory(template, uid, set, tag, fields, accessLevel);
            }

            public NetworkStatsHistory getHistoryIntervalForUid(NetworkTemplate template, int uid, int set, int tag, int fields, long start, long end) {
                int accessLevel = NetworkStatsService.this.checkAccessLevel(this.mCallingPackage);
                if (tag == 0) {
                    return getUidComplete().getHistory(template, uid, set, tag, fields, start, end, accessLevel);
                }
                if (uid == Binder.getCallingUid()) {
                    return getUidTagComplete().getHistory(template, uid, set, tag, fields, start, end, accessLevel);
                }
                throw new SecurityException("Calling package " + this.mCallingPackage + " cannot access tag information from a different uid");
            }

            public void close() {
                this.mUidComplete = null;
                this.mUidTagComplete = null;
            }

            public long getMobileTotalBytes(int subSim, long start_time, long end_time) {
                String subscriberId = null;
                long totalBytes = 0;
                TelephonyManager tele = TelephonyManager.from(NetworkStatsService.this.mContext);
                if (tele != null) {
                    try {
                        if (tele.getSimState(subSim) == 5) {
                            int subId = -1;
                            int[] subIds = SubscriptionManager.getSubId(subSim);
                            if (subIds != null && subIds.length != 0) {
                                subId = subIds[0];
                            }
                            if (!SubscriptionManager.isValidSubscriptionId(subId)) {
                                Log.e(NetworkStatsService.TAG, "Dual sim subSim:" + subSim + " fetch subID fail, always fetch default");
                                subId = SubscriptionManager.getDefaultDataSubscriptionId();
                            }
                            if (SubscriptionManager.isValidSubscriptionId(subId)) {
                                subscriberId = tele.getSubscriberId(subId);
                                NetworkTemplate templete = NetworkTemplate.buildTemplateMobileAll(subscriberId);
                                totalBytes = NetworkStatsService.this.getNetworkTotalBytes(templete, start_time, end_time);
                            } else {
                                Log.e(NetworkStatsService.TAG, "getMobileTotalBytes subId not valid");
                            }
                        } else {
                            Log.e(NetworkStatsService.TAG, "getMobileTotalBytes SimState != SIM_STATE_READY");
                        }
                    } catch (Exception e) {
                        Log.e(NetworkStatsService.TAG, "getMobileTotalBytes Exception" + e);
                    }
                } else {
                    Log.e(NetworkStatsService.TAG, "getMobileTotalBytes TelephonyManager is null");
                }
                Log.v(NetworkStatsService.TAG, "getMobileTotalBytes subSim=" + subSim + " subscriberId" + subscriberId + " start_time=" + start_time + " end_time" + end_time + " totalBytes=" + totalBytes);
                return totalBytes;
            }
        };
    }

    private int checkAccessLevel(String callingPackage) {
        return NetworkStatsAccess.checkAccessLevel(this.mContext, Binder.getCallingUid(), callingPackage);
    }

    private NetworkStats internalGetSummaryForNetwork(NetworkTemplate template, long start, long end, int accessLevel) {
        return this.mXtStatsCached.getSummary(template, start, end, accessLevel);
    }

    private NetworkStatsHistory internalGetHistoryForNetwork(NetworkTemplate template, int fields, int accessLevel) {
        return this.mXtStatsCached.getHistory(template, -1, -1, 0, fields, accessLevel);
    }

    public long getNetworkTotalBytes(NetworkTemplate template, long start, long end) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_NETWORK_USAGE_HISTORY", TAG);
        assertBandwidthControlEnabled();
        return internalGetSummaryForNetwork(template, start, end, 3).getTotalBytes();
    }

    public NetworkStats getDataLayerSnapshotForUid(int uid) throws RemoteException {
        if (Binder.getCallingUid() != uid) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_NETWORK_STATE", TAG);
        }
        assertBandwidthControlEnabled();
        long token = Binder.clearCallingIdentity();
        try {
            NetworkStats networkLayer = this.mNetworkManager.getNetworkStatsUidDetail(uid);
            Binder.restoreCallingIdentity(token);
            networkLayer.spliceOperationsFrom(this.mUidOperations);
            NetworkStats dataLayer = new NetworkStats(networkLayer.getElapsedRealtime(), networkLayer.size());
            NetworkStats.Entry entry = null;
            for (int i = 0; i < networkLayer.size(); i++) {
                entry = networkLayer.getValues(i, entry);
                entry.iface = NetworkStats.IFACE_ALL;
                dataLayer.combineValues(entry);
            }
            return dataLayer;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
            throw th;
        }
    }

    public String[] getMobileIfaces() {
        return this.mMobileIfaces;
    }

    public void incrementOperationCount(int uid, int tag, int operationCount) {
        if (Binder.getCallingUid() != uid) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.MODIFY_NETWORK_ACCOUNTING", TAG);
        }
        if (operationCount < 0) {
            throw new IllegalArgumentException("operation count can only be incremented");
        }
        if (tag == 0) {
            throw new IllegalArgumentException("operation count must have specific tag");
        }
        synchronized (this.mStatsLock) {
            int set = this.mActiveUidCounterSet.get(uid, 0);
            this.mUidOperations.combineValues(this.mActiveIface, uid, set, tag, 0L, 0L, 0L, 0L, operationCount);
            this.mUidOperations.combineValues(this.mActiveIface, uid, set, 0, 0L, 0L, 0L, 0L, operationCount);
        }
    }

    public void setUidForeground(int uid, boolean uidForeground) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MODIFY_NETWORK_ACCOUNTING", TAG);
        synchronized (this.mStatsLock) {
            int set = uidForeground ? 1 : 0;
            int oldSet = this.mActiveUidCounterSet.get(uid, 0);
            if (oldSet != set) {
                this.mActiveUidCounterSet.put(uid, set);
                Slog.v(TAG, "setKernelCounterSet uid=" + uid + " set=" + set);
                NetworkManagementSocketTagger.setKernelCounterSet(uid, set);
            }
        }
    }

    public void forceUpdateIfaces() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_NETWORK_USAGE_HISTORY", TAG);
        assertBandwidthControlEnabled();
        long token = Binder.clearCallingIdentity();
        try {
            updateIfaces();
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void forceUpdate() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_NETWORK_USAGE_HISTORY", TAG);
        assertBandwidthControlEnabled();
        long token = Binder.clearCallingIdentity();
        try {
            performPoll(3);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void advisePersistThreshold(long thresholdBytes) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MODIFY_NETWORK_ACCOUNTING", TAG);
        assertBandwidthControlEnabled();
        this.mPersistThreshold = MathUtils.constrain(thresholdBytes, 131072L, 2097152L);
        Slog.v(TAG, "advisePersistThreshold() given " + thresholdBytes + ", clamped to " + this.mPersistThreshold);
        long currentTime = (this.mTime.hasCache() && USE_TRUESTED_TIME) ? this.mTime.currentTimeMillis() : System.currentTimeMillis();
        synchronized (this.mStatsLock) {
            if (this.mSystemReady) {
                updatePersistThresholds();
                this.mDevRecorder.maybePersistLocked(currentTime);
                this.mXtRecorder.maybePersistLocked(currentTime);
                this.mUidRecorder.maybePersistLocked(currentTime);
                this.mUidTagRecorder.maybePersistLocked(currentTime);
                registerGlobalAlert();
            }
        }
    }

    public DataUsageRequest registerUsageCallback(String callingPackage, DataUsageRequest request, Messenger messenger, IBinder binder) {
        Preconditions.checkNotNull(callingPackage, "calling package is null");
        Preconditions.checkNotNull(request, "DataUsageRequest is null");
        Preconditions.checkNotNull(request.template, "NetworkTemplate is null");
        Preconditions.checkNotNull(messenger, "messenger is null");
        Preconditions.checkNotNull(binder, "binder is null");
        int callingUid = Binder.getCallingUid();
        int accessLevel = checkAccessLevel(callingPackage);
        long token = Binder.clearCallingIdentity();
        try {
            DataUsageRequest normalizedRequest = this.mStatsObservers.register(request, messenger, binder, callingUid, accessLevel);
            Binder.restoreCallingIdentity(token);
            this.mHandler.sendMessage(this.mHandler.obtainMessage(1, 3));
            return normalizedRequest;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
            throw th;
        }
    }

    public void unregisterUsageRequest(DataUsageRequest request) {
        Preconditions.checkNotNull(request, "DataUsageRequest is null");
        int callingUid = Binder.getCallingUid();
        long token = Binder.clearCallingIdentity();
        try {
            this.mStatsObservers.unregister(request, callingUid);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void updatePersistThresholds() {
        this.mDevRecorder.setPersistThreshold(this.mSettings.getDevPersistBytes(this.mPersistThreshold));
        this.mXtRecorder.setPersistThreshold(this.mSettings.getXtPersistBytes(this.mPersistThreshold));
        this.mUidRecorder.setPersistThreshold(this.mSettings.getUidPersistBytes(this.mPersistThreshold));
        this.mUidTagRecorder.setPersistThreshold(this.mSettings.getUidTagPersistBytes(this.mPersistThreshold));
        this.mGlobalAlertBytes = this.mSettings.getGlobalAlertBytes(this.mPersistThreshold);
    }

    private void updateIfaces() {
        synchronized (this.mStatsLock) {
            this.mWakeLock.acquire();
            try {
                updateIfacesLocked();
            } finally {
                this.mWakeLock.release();
            }
        }
    }

    private void updateIfacesLocked() {
        if (this.mSystemReady) {
            Slog.v(TAG, "updateIfacesLocked()");
            performPollLocked(1);
            try {
                NetworkState[] states = this.mConnManager.getAllNetworkState();
                LinkProperties activeLink = this.mConnManager.getActiveLinkProperties();
                this.mActiveIface = activeLink != null ? activeLink.getInterfaceName() : null;
                this.mActiveIfaces.clear();
                this.mActiveUidIfaces.clear();
                ArraySet<String> mobileIfaces = new ArraySet<>();
                for (NetworkState state : states) {
                    if (state.networkInfo.isConnected()) {
                        boolean isMobile = ConnectivityManager.isNetworkTypeMobile(state.networkInfo.getType());
                        NetworkIdentity ident = NetworkIdentity.buildNetworkIdentity(this.mContext, state);
                        Slog.i(TAG, "NetworkIdentity: " + ident);
                        String baseIface = state.linkProperties.getInterfaceName();
                        if (baseIface != null) {
                            findOrCreateNetworkIdentitySet(this.mActiveIfaces, baseIface).add(ident);
                            findOrCreateNetworkIdentitySet(this.mActiveUidIfaces, baseIface).add(ident);
                            if (isMobile) {
                                mobileIfaces.add(baseIface);
                            }
                        }
                        List<LinkProperties> stackedLinks = state.linkProperties.getStackedLinks();
                        for (LinkProperties stackedLink : stackedLinks) {
                            String stackedIface = stackedLink.getInterfaceName();
                            if (stackedIface != null) {
                                findOrCreateNetworkIdentitySet(this.mActiveUidIfaces, stackedIface).add(ident);
                                if (isMobile) {
                                    mobileIfaces.add(stackedIface);
                                }
                            }
                        }
                    }
                }
                for (int i = 0; i < this.mActiveIfaces.size(); i++) {
                    Slog.i(TAG, "iface:" + this.mActiveIfaces.keyAt(i));
                    for (NetworkIdentity id : this.mActiveIfaces.valueAt(i)) {
                        Slog.i(TAG, "ident:" + id);
                    }
                }
                for (String iface : mobileIfaces) {
                    if (iface != null && !ArrayUtils.contains(this.mMobileIfaces, iface)) {
                        this.mMobileIfaces = (String[]) ArrayUtils.appendElement(String.class, this.mMobileIfaces, iface);
                    }
                }
            } catch (RemoteException e) {
            }
        }
    }

    private static <K> NetworkIdentitySet findOrCreateNetworkIdentitySet(ArrayMap<K, NetworkIdentitySet> map, K key) {
        NetworkIdentitySet ident = map.get(key);
        if (ident == null) {
            NetworkIdentitySet ident2 = new NetworkIdentitySet();
            map.put(key, ident2);
            return ident2;
        }
        return ident;
    }

    private void recordSnapshotLocked(long currentTime) throws RemoteException {
        NetworkStats uidSnapshot = getNetworkStatsUidDetail();
        NetworkStats xtSnapshot = this.mNetworkManager.getNetworkStatsSummaryXt();
        NetworkStats devSnapshot = this.mNetworkManager.getNetworkStatsSummaryDev();
        this.mDevRecorder.recordSnapshotLocked(devSnapshot, this.mActiveIfaces, null, currentTime);
        this.mXtRecorder.recordSnapshotLocked(xtSnapshot, this.mActiveIfaces, null, currentTime);
        VpnInfo[] vpnArray = this.mConnManager.getAllVpnInfo();
        this.mUidRecorder.recordSnapshotLocked(uidSnapshot, this.mActiveUidIfaces, vpnArray, currentTime);
        this.mUidTagRecorder.recordSnapshotLocked(uidSnapshot, this.mActiveUidIfaces, vpnArray, currentTime);
        this.mStatsObservers.updateStats(xtSnapshot, uidSnapshot, new ArrayMap<>(this.mActiveIfaces), new ArrayMap<>(this.mActiveUidIfaces), vpnArray, currentTime);
    }

    private void bootstrapStatsLocked() {
        long currentTime = (this.mTime.hasCache() && USE_TRUESTED_TIME) ? this.mTime.currentTimeMillis() : System.currentTimeMillis();
        try {
            recordSnapshotLocked(currentTime);
        } catch (RemoteException e) {
        } catch (IllegalStateException e2) {
            Slog.w(TAG, "problem reading network stats: " + e2);
        }
    }

    private void performPoll(int flags) {
        if (this.mTime.getCacheAge() > this.mSettings.getTimeCacheMaxAge() && USE_TRUESTED_TIME) {
            this.mTime.forceRefresh();
        }
        synchronized (this.mStatsLock) {
            this.mWakeLock.acquire();
            try {
                performPollLocked(flags);
            } finally {
                this.mWakeLock.release();
            }
        }
    }

    private void performPollLocked(int flags) {
        if (this.mSystemReady) {
            Slog.v(TAG, "performPollLocked(flags=0x" + Integer.toHexString(flags) + ")");
            long startRealtime = SystemClock.elapsedRealtime();
            boolean persistNetwork = (flags & 1) != 0;
            boolean persistUid = (flags & 2) != 0;
            boolean persistForce = (flags & 256) != 0;
            long currentTime = (this.mTime.hasCache() && USE_TRUESTED_TIME) ? this.mTime.currentTimeMillis() : System.currentTimeMillis();
            try {
                recordSnapshotLocked(currentTime);
                if (persistForce) {
                    this.mDevRecorder.forcePersistLocked(currentTime);
                    this.mXtRecorder.forcePersistLocked(currentTime);
                    this.mUidRecorder.forcePersistLocked(currentTime);
                    this.mUidTagRecorder.forcePersistLocked(currentTime);
                } else {
                    if (persistNetwork) {
                        this.mDevRecorder.maybePersistLocked(currentTime);
                        this.mXtRecorder.maybePersistLocked(currentTime);
                    }
                    if (persistUid) {
                        this.mUidRecorder.maybePersistLocked(currentTime);
                        this.mUidTagRecorder.maybePersistLocked(currentTime);
                    }
                }
                long duration = SystemClock.elapsedRealtime() - startRealtime;
                Slog.v(TAG, "performPollLocked() took " + duration + "ms");
                if (this.mSettings.getSampleEnabled()) {
                    performSampleLocked();
                }
                Intent updatedIntent = new Intent(ACTION_NETWORK_STATS_UPDATED);
                updatedIntent.setFlags(1073741824);
                this.mContext.sendBroadcastAsUser(updatedIntent, UserHandle.ALL, "android.permission.READ_NETWORK_USAGE_HISTORY");
            } catch (RemoteException e) {
            } catch (IllegalStateException e2) {
                Log.wtf(TAG, "problem reading network stats", e2);
            }
        }
    }

    private void performSampleLocked() {
        long trustedTime = (this.mTime.hasCache() && USE_TRUESTED_TIME) ? this.mTime.currentTimeMillis() : -1L;
        NetworkTemplate template = NetworkTemplate.buildTemplateMobileWildcard();
        NetworkStats.Entry devTotal = this.mDevRecorder.getTotalSinceBootLocked(template);
        NetworkStats.Entry xtTotal = this.mXtRecorder.getTotalSinceBootLocked(template);
        NetworkStats.Entry uidTotal = this.mUidRecorder.getTotalSinceBootLocked(template);
        EventLogTags.writeNetstatsMobileSample(devTotal.rxBytes, devTotal.rxPackets, devTotal.txBytes, devTotal.txPackets, xtTotal.rxBytes, xtTotal.rxPackets, xtTotal.txBytes, xtTotal.txPackets, uidTotal.rxBytes, uidTotal.rxPackets, uidTotal.txBytes, uidTotal.txPackets, trustedTime);
        NetworkTemplate template2 = NetworkTemplate.buildTemplateWifiWildcard();
        NetworkStats.Entry devTotal2 = this.mDevRecorder.getTotalSinceBootLocked(template2);
        NetworkStats.Entry xtTotal2 = this.mXtRecorder.getTotalSinceBootLocked(template2);
        NetworkStats.Entry uidTotal2 = this.mUidRecorder.getTotalSinceBootLocked(template2);
        EventLogTags.writeNetstatsWifiSample(devTotal2.rxBytes, devTotal2.rxPackets, devTotal2.txBytes, devTotal2.txPackets, xtTotal2.rxBytes, xtTotal2.rxPackets, xtTotal2.txBytes, xtTotal2.txPackets, uidTotal2.rxBytes, uidTotal2.rxPackets, uidTotal2.txBytes, uidTotal2.txPackets, trustedTime);
    }

    private void removeUidsLocked(int... uids) {
        Slog.v(TAG, "removeUidsLocked() for UIDs " + Arrays.toString(uids));
        performPollLocked(3);
        this.mUidRecorder.removeUidsLocked(uids);
        this.mUidTagRecorder.removeUidsLocked(uids);
        for (int uid : uids) {
            NetworkManagementSocketTagger.resetKernelUidStats(uid);
        }
    }

    private void removeUserLocked(int userId) {
        Slog.v(TAG, "removeUserLocked() for userId=" + userId);
        int[] uids = new int[0];
        List<ApplicationInfo> apps = this.mContext.getPackageManager().getInstalledApplications(8704);
        for (ApplicationInfo app : apps) {
            int uid = UserHandle.getUid(userId, app.uid);
            uids = ArrayUtils.appendInt(uids, uid);
        }
        removeUidsLocked(uids);
    }

    protected void dump(FileDescriptor fd, PrintWriter rawWriter, String[] args) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.DUMP", TAG);
        long duration = UnixCalendar.DAY_IN_MILLIS;
        HashSet<String> argSet = new HashSet<>();
        for (String arg : args) {
            argSet.add(arg);
            if (arg.startsWith("--duration=")) {
                try {
                    duration = Long.parseLong(arg.substring(11));
                } catch (NumberFormatException e) {
                }
            }
        }
        boolean zContains = !argSet.contains("--poll") ? argSet.contains("poll") : true;
        boolean checkin = argSet.contains("--checkin");
        boolean zContains2 = !argSet.contains("--full") ? argSet.contains("full") : true;
        boolean zContains3 = !argSet.contains("--uid") ? argSet.contains("detail") : true;
        boolean zContains4 = !argSet.contains("--tag") ? argSet.contains("detail") : true;
        IndentingPrintWriter pw = new IndentingPrintWriter(rawWriter, "  ");
        synchronized (this.mStatsLock) {
            if (zContains) {
                performPollLocked(259);
                pw.println("Forced poll");
                return;
            }
            if (checkin) {
                long end = System.currentTimeMillis();
                long start = end - duration;
                pw.print("v1,");
                pw.print(start / 1000);
                pw.print(',');
                pw.print(end / 1000);
                pw.println();
                pw.println(PREFIX_XT);
                this.mXtRecorder.dumpCheckin(rawWriter, start, end);
                if (zContains3) {
                    pw.println("uid");
                    this.mUidRecorder.dumpCheckin(rawWriter, start, end);
                }
                if (zContains4) {
                    pw.println("tag");
                    this.mUidTagRecorder.dumpCheckin(rawWriter, start, end);
                }
                return;
            }
            pw.println("Active interfaces:");
            pw.increaseIndent();
            for (int i = 0; i < this.mActiveIfaces.size(); i++) {
                pw.printPair("iface", this.mActiveIfaces.keyAt(i));
                pw.printPair("ident", this.mActiveIfaces.valueAt(i));
                pw.println();
            }
            pw.decreaseIndent();
            pw.println("Active UID interfaces:");
            pw.increaseIndent();
            for (int i2 = 0; i2 < this.mActiveUidIfaces.size(); i2++) {
                pw.printPair("iface", this.mActiveUidIfaces.keyAt(i2));
                pw.printPair("ident", this.mActiveUidIfaces.valueAt(i2));
                pw.println();
            }
            pw.decreaseIndent();
            pw.println("Dev stats:");
            pw.increaseIndent();
            this.mDevRecorder.dumpLocked(pw, zContains2);
            pw.decreaseIndent();
            pw.println("Xt stats:");
            pw.increaseIndent();
            this.mXtRecorder.dumpLocked(pw, zContains2);
            pw.decreaseIndent();
            if (zContains3) {
                pw.println("UID stats:");
                pw.increaseIndent();
                this.mUidRecorder.dumpLocked(pw, zContains2);
                pw.decreaseIndent();
            }
            if (zContains4) {
                pw.println("UID tag stats:");
                pw.increaseIndent();
                this.mUidTagRecorder.dumpLocked(pw, zContains2);
                pw.decreaseIndent();
            }
        }
    }

    private NetworkStats getNetworkStatsUidDetail() throws RemoteException {
        NetworkStats uidSnapshot = this.mNetworkManager.getNetworkStatsUidDetail(-1);
        NetworkStats tetherSnapshot = getNetworkStatsTethering();
        uidSnapshot.combineAllValues(tetherSnapshot);
        uidSnapshot.combineAllValues(this.mUidOperations);
        return uidSnapshot;
    }

    private NetworkStats getNetworkStatsTethering() throws RemoteException {
        try {
            return this.mNetworkManager.getNetworkStatsTethering();
        } catch (IllegalStateException e) {
            Log.e(TAG, "problem reading network stats" + e);
            return new NetworkStats(0L, 10);
        }
    }

    static class HandlerCallback implements Handler.Callback {
        private final NetworkStatsService mService;

        HandlerCallback(NetworkStatsService service) {
            this.mService = service;
        }

        @Override
        public boolean handleMessage(Message msg) {
            Log.v(NetworkStatsService.TAG, "handleMessage(): msg=" + msg.what);
            switch (msg.what) {
                case 1:
                    int flags = msg.arg1;
                    this.mService.performPoll(flags);
                    break;
                case 2:
                    this.mService.updateIfaces();
                    break;
                case 3:
                    this.mService.registerGlobalAlert();
                    break;
            }
            return true;
        }
    }

    private void assertBandwidthControlEnabled() {
        if (isBandwidthControlEnabled()) {
        } else {
            throw new IllegalStateException("Bandwidth module disabled");
        }
    }

    private boolean isBandwidthControlEnabled() {
        long token = Binder.clearCallingIdentity();
        try {
            return this.mNetworkManager.isBandwidthControlEnabled();
        } catch (RemoteException e) {
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private class DropBoxNonMonotonicObserver implements NetworkStats.NonMonotonicObserver<String> {
        DropBoxNonMonotonicObserver(NetworkStatsService this$0, DropBoxNonMonotonicObserver dropBoxNonMonotonicObserver) {
            this();
        }

        private DropBoxNonMonotonicObserver() {
        }

        public void foundNonMonotonic(NetworkStats left, int leftIndex, NetworkStats right, int rightIndex, String cookie) {
            Log.w(NetworkStatsService.TAG, "found non-monotonic values; saving to dropbox");
            StringBuilder builder = new StringBuilder();
            builder.append("found non-monotonic ").append(cookie).append(" values at left[").append(leftIndex).append("] - right[").append(rightIndex).append("]\n");
            builder.append("left=").append(left).append('\n');
            builder.append("right=").append(right).append('\n');
            DropBoxManager dropBox = (DropBoxManager) NetworkStatsService.this.mContext.getSystemService("dropbox");
            dropBox.addText(NetworkStatsService.TAG_NETSTATS_ERROR, builder.toString());
        }
    }

    private static class DefaultNetworkStatsSettings implements NetworkStatsSettings {
        private final ContentResolver mResolver;

        public DefaultNetworkStatsSettings(Context context) {
            this.mResolver = (ContentResolver) Preconditions.checkNotNull(context.getContentResolver());
        }

        private long getGlobalLong(String name, long def) {
            return Settings.Global.getLong(this.mResolver, name, def);
        }

        private boolean getGlobalBoolean(String name, boolean def) {
            int defInt = def ? 1 : 0;
            return Settings.Global.getInt(this.mResolver, name, defInt) != 0;
        }

        @Override
        public long getPollInterval() {
            return getGlobalLong("netstats_poll_interval", 1800000L);
        }

        @Override
        public long getTimeCacheMaxAge() {
            return getGlobalLong("netstats_time_cache_max_age", UnixCalendar.DAY_IN_MILLIS);
        }

        @Override
        public long getGlobalAlertBytes(long def) {
            return getGlobalLong("netstats_global_alert_bytes", def);
        }

        @Override
        public boolean getSampleEnabled() {
            return getGlobalBoolean("netstats_sample_enabled", true);
        }

        @Override
        public NetworkStatsSettings.Config getDevConfig() {
            return new NetworkStatsSettings.Config(getGlobalLong("netstats_dev_bucket_duration", 3600000L), getGlobalLong("netstats_dev_rotate_age", 1296000000L), getGlobalLong("netstats_dev_delete_age", 7776000000L));
        }

        @Override
        public NetworkStatsSettings.Config getXtConfig() {
            return getDevConfig();
        }

        @Override
        public NetworkStatsSettings.Config getUidConfig() {
            return new NetworkStatsSettings.Config(getGlobalLong("netstats_uid_bucket_duration", 7200000L), getGlobalLong("netstats_uid_rotate_age", 1296000000L), getGlobalLong("netstats_uid_delete_age", 7776000000L));
        }

        @Override
        public NetworkStatsSettings.Config getUidTagConfig() {
            return new NetworkStatsSettings.Config(getGlobalLong("netstats_uid_tag_bucket_duration", 7200000L), getGlobalLong("netstats_uid_tag_rotate_age", 432000000L), getGlobalLong("netstats_uid_tag_delete_age", 1296000000L));
        }

        @Override
        public long getDevPersistBytes(long def) {
            return getGlobalLong("netstats_dev_persist_bytes", def);
        }

        @Override
        public long getXtPersistBytes(long def) {
            return getDevPersistBytes(def);
        }

        @Override
        public long getUidPersistBytes(long def) {
            return getGlobalLong("netstats_uid_persist_bytes", def);
        }

        @Override
        public long getUidTagPersistBytes(long def) {
            return getGlobalLong("netstats_uid_tag_persist_bytes", def);
        }
    }

    public void setUseTrustedTime(boolean b) {
        USE_TRUESTED_TIME = b;
    }
}
