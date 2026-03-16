package com.android.server.net;

import android.app.AlarmManager;
import android.app.IAlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.net.ConnectivityManager;
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
import android.os.INetworkManagementService;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.MathUtils;
import android.util.NtpTrustedTime;
import android.util.Slog;
import android.util.SparseIntArray;
import android.util.TrustedTime;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FileRotator;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.server.EventLogTags;
import com.android.server.NetworkManagementService;
import com.android.server.NetworkManagementSocketTagger;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;

public class NetworkStatsService extends INetworkStatsService.Stub {
    public static final String ACTION_NETWORK_STATS_POLL = "com.android.server.action.NETWORK_STATS_POLL";
    public static final String ACTION_NETWORK_STATS_UPDATED = "com.android.server.action.NETWORK_STATS_UPDATED";
    private static final int FLAG_PERSIST_ALL = 3;
    private static final int FLAG_PERSIST_FORCE = 256;
    private static final int FLAG_PERSIST_NETWORK = 1;
    private static final int FLAG_PERSIST_UID = 2;
    private static final boolean LOGV = false;
    private static final int MSG_PERFORM_POLL = 1;
    private static final int MSG_REGISTER_GLOBAL_ALERT = 3;
    private static final int MSG_UPDATE_IFACES = 2;
    private static final String PREFIX_DEV = "dev";
    private static final String PREFIX_UID = "uid";
    private static final String PREFIX_UID_TAG = "uid_tag";
    private static final String PREFIX_XT = "xt";
    private static final String TAG = "NetworkStats";
    private static final String TAG_NETSTATS_ERROR = "netstats_error";
    private String mActiveIface;
    private final ArrayMap<String, NetworkIdentitySet> mActiveIfaces;
    private SparseIntArray mActiveUidCounterSet;
    private final ArrayMap<String, NetworkIdentitySet> mActiveUidIfaces;
    private final AlarmManager mAlarmManager;
    private INetworkManagementEventObserver mAlertObserver;
    private final File mBaseDir;
    private IConnectivityManager mConnManager;
    private final Context mContext;
    private NetworkStatsRecorder mDevRecorder;
    private long mGlobalAlertBytes;
    private final Handler mHandler;
    private Handler.Callback mHandlerCallback;
    private String[] mMobileIfaces;
    private final INetworkManagementService mNetworkManager;
    private final DropBoxNonMonotonicObserver mNonMonotonicObserver;
    private long mPersistThreshold;
    private PendingIntent mPollIntent;
    private BroadcastReceiver mPollReceiver;
    private BroadcastReceiver mRemovedReceiver;
    private final NetworkStatsSettings mSettings;
    private BroadcastReceiver mShutdownReceiver;
    private final Object mStatsLock;
    private final File mSystemDir;
    private boolean mSystemReady;
    private final TelephonyManager mTeleManager;
    private BroadcastReceiver mTetherReceiver;
    private final TrustedTime mTime;
    private NetworkStats mUidOperations;
    private NetworkStatsRecorder mUidRecorder;
    private NetworkStatsRecorder mUidTagRecorder;
    private BroadcastReceiver mUserReceiver;
    private final PowerManager.WakeLock mWakeLock;
    private NetworkStatsRecorder mXtRecorder;
    private NetworkStatsCollection mXtStatsCached;

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

    public NetworkStatsService(Context context, INetworkManagementService networkManager, IAlarmManager alarmManager) {
        this(context, networkManager, alarmManager, NtpTrustedTime.getInstance(context), getDefaultSystemDir(), new DefaultNetworkStatsSettings(context));
    }

    private static File getDefaultSystemDir() {
        return new File(Environment.getDataDirectory(), "system");
    }

    public NetworkStatsService(Context context, INetworkManagementService networkManager, IAlarmManager alarmManager, TrustedTime time, File systemDir, NetworkStatsSettings settings) {
        this.mStatsLock = new Object();
        this.mActiveIfaces = new ArrayMap<>();
        this.mActiveUidIfaces = new ArrayMap<>();
        this.mMobileIfaces = new String[0];
        this.mNonMonotonicObserver = new DropBoxNonMonotonicObserver();
        this.mActiveUidCounterSet = new SparseIntArray();
        this.mUidOperations = new NetworkStats(0L, 10);
        this.mPersistThreshold = 2097152L;
        this.mTetherReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                NetworkStatsService.this.performPoll(1);
            }
        };
        this.mPollReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                NetworkStatsService.this.performPoll(3);
                NetworkStatsService.this.registerGlobalAlert();
            }
        };
        this.mRemovedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                int uid = intent.getIntExtra("android.intent.extra.UID", -1);
                if (uid != -1) {
                    synchronized (NetworkStatsService.this.mStatsLock) {
                        NetworkStatsService.this.mWakeLock.acquire();
                        try {
                            NetworkStatsService.this.removeUidsLocked(uid);
                        } finally {
                            NetworkStatsService.this.mWakeLock.release();
                        }
                    }
                }
            }
        };
        this.mUserReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                int userId = intent.getIntExtra("android.intent.extra.user_handle", -1);
                if (userId != -1) {
                    synchronized (NetworkStatsService.this.mStatsLock) {
                        NetworkStatsService.this.mWakeLock.acquire();
                        try {
                            NetworkStatsService.this.removeUserLocked(userId);
                        } finally {
                            NetworkStatsService.this.mWakeLock.release();
                        }
                    }
                }
            }
        };
        this.mShutdownReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                synchronized (NetworkStatsService.this.mStatsLock) {
                    NetworkStatsService.this.shutdownLocked();
                }
            }
        };
        this.mAlertObserver = new BaseNetworkObserver() {
            public void limitReached(String limitName, String iface) {
                NetworkStatsService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", NetworkStatsService.TAG);
                if (NetworkManagementService.LIMIT_GLOBAL_ALERT.equals(limitName)) {
                    NetworkStatsService.this.mHandler.obtainMessage(1, 1, 0).sendToTarget();
                    NetworkStatsService.this.mHandler.obtainMessage(3).sendToTarget();
                }
            }
        };
        this.mHandlerCallback = new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        int flags = msg.arg1;
                        NetworkStatsService.this.performPoll(flags);
                        break;
                    case 2:
                        NetworkStatsService.this.updateIfaces();
                        break;
                    case 3:
                        NetworkStatsService.this.registerGlobalAlert();
                        break;
                }
                return true;
            }
        };
        this.mContext = (Context) Preconditions.checkNotNull(context, "missing Context");
        this.mNetworkManager = (INetworkManagementService) Preconditions.checkNotNull(networkManager, "missing INetworkManagementService");
        this.mTime = (TrustedTime) Preconditions.checkNotNull(time, "missing TrustedTime");
        this.mTeleManager = (TelephonyManager) Preconditions.checkNotNull(TelephonyManager.getDefault(), "missing TelephonyManager");
        this.mSettings = (NetworkStatsSettings) Preconditions.checkNotNull(settings, "missing NetworkStatsSettings");
        this.mAlarmManager = (AlarmManager) context.getSystemService("alarm");
        PowerManager powerManager = (PowerManager) context.getSystemService("power");
        this.mWakeLock = powerManager.newWakeLock(1, TAG);
        HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        this.mHandler = new Handler(thread.getLooper(), this.mHandlerCallback);
        this.mSystemDir = (File) Preconditions.checkNotNull(systemDir);
        this.mBaseDir = new File(systemDir, "netstats");
        this.mBaseDir.mkdirs();
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
        this.mDevRecorder = buildRecorder(PREFIX_DEV, this.mSettings.getDevConfig(), LOGV);
        this.mXtRecorder = buildRecorder(PREFIX_XT, this.mSettings.getXtConfig(), LOGV);
        this.mUidRecorder = buildRecorder(PREFIX_UID, this.mSettings.getUidConfig(), LOGV);
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
        this.mContext.registerReceiver(this.mShutdownReceiver, shutdownFilter);
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
        this.mContext.unregisterReceiver(this.mTetherReceiver);
        this.mContext.unregisterReceiver(this.mPollReceiver);
        this.mContext.unregisterReceiver(this.mRemovedReceiver);
        this.mContext.unregisterReceiver(this.mShutdownReceiver);
        long currentTime = this.mTime.hasCache() ? this.mTime.currentTimeMillis() : System.currentTimeMillis();
        this.mDevRecorder.forcePersistLocked(currentTime);
        this.mXtRecorder.forcePersistLocked(currentTime);
        this.mUidRecorder.forcePersistLocked(currentTime);
        this.mUidTagRecorder.forcePersistLocked(currentTime);
        this.mDevRecorder = null;
        this.mXtRecorder = null;
        this.mUidRecorder = null;
        this.mUidTagRecorder = null;
        this.mXtStatsCached = null;
        this.mSystemReady = LOGV;
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
            if (file3.exists()) {
                this.mUidRecorder.importLegacyUidLocked(file3);
                this.mUidTagRecorder.importLegacyUidLocked(file3);
                file3.delete();
            }
        } catch (IOException e) {
            Log.wtf(TAG, "problem during legacy upgrade", e);
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
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_NETWORK_USAGE_HISTORY", TAG);
        assertBandwidthControlEnabled();
        return new INetworkStatsSession.Stub() {
            private NetworkStatsCollection mUidComplete;
            private NetworkStatsCollection mUidTagComplete;

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

            public NetworkStats getSummaryForNetwork(NetworkTemplate template, long start, long end) {
                return NetworkStatsService.this.internalGetSummaryForNetwork(template, start, end);
            }

            public NetworkStatsHistory getHistoryForNetwork(NetworkTemplate template, int fields) {
                return NetworkStatsService.this.internalGetHistoryForNetwork(template, fields);
            }

            public NetworkStats getSummaryForAllUid(NetworkTemplate template, long start, long end, boolean includeTags) {
                NetworkStats stats = getUidComplete().getSummary(template, start, end);
                if (includeTags) {
                    NetworkStats tagStats = getUidTagComplete().getSummary(template, start, end);
                    stats.combineAllValues(tagStats);
                }
                return stats;
            }

            public NetworkStatsHistory getHistoryForUid(NetworkTemplate template, int uid, int set, int tag, int fields) {
                return tag == 0 ? getUidComplete().getHistory(template, uid, set, tag, fields) : getUidTagComplete().getHistory(template, uid, set, tag, fields);
            }

            public void close() {
                this.mUidComplete = null;
                this.mUidTagComplete = null;
            }
        };
    }

    private NetworkStats internalGetSummaryForNetwork(NetworkTemplate template, long start, long end) {
        return this.mXtStatsCached.getSummary(template, start, end);
    }

    private NetworkStatsHistory internalGetHistoryForNetwork(NetworkTemplate template, int fields) {
        return this.mXtStatsCached.getHistory(template, -1, -1, 0, fields);
    }

    public long getNetworkTotalBytes(NetworkTemplate template, long start, long end) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_NETWORK_USAGE_HISTORY", TAG);
        assertBandwidthControlEnabled();
        return internalGetSummaryForNetwork(template, start, end).getTotalBytes();
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
        long currentTime = this.mTime.hasCache() ? this.mTime.currentTimeMillis() : System.currentTimeMillis();
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
                this.mMobileIfaces = (String[]) mobileIfaces.toArray(new String[mobileIfaces.size()]);
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

    private void bootstrapStatsLocked() {
        long currentTime = this.mTime.hasCache() ? this.mTime.currentTimeMillis() : System.currentTimeMillis();
        try {
            NetworkStats uidSnapshot = getNetworkStatsUidDetail();
            NetworkStats xtSnapshot = this.mNetworkManager.getNetworkStatsSummaryXt();
            NetworkStats devSnapshot = this.mNetworkManager.getNetworkStatsSummaryDev();
            this.mDevRecorder.recordSnapshotLocked(devSnapshot, this.mActiveIfaces, currentTime);
            this.mXtRecorder.recordSnapshotLocked(xtSnapshot, this.mActiveIfaces, currentTime);
            this.mUidRecorder.recordSnapshotLocked(uidSnapshot, this.mActiveUidIfaces, currentTime);
            this.mUidTagRecorder.recordSnapshotLocked(uidSnapshot, this.mActiveUidIfaces, currentTime);
        } catch (RemoteException e) {
        } catch (IllegalStateException e2) {
            Slog.w(TAG, "problem reading network stats: " + e2);
        }
    }

    private void performPoll(int flags) {
        if (this.mTime.getCacheAge() > this.mSettings.getTimeCacheMaxAge()) {
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
            SystemClock.elapsedRealtime();
            boolean persistNetwork = (flags & 1) != 0 ? true : LOGV;
            boolean persistUid = (flags & 2) != 0 ? true : LOGV;
            boolean persistForce = (flags & 256) != 0 ? true : LOGV;
            long currentTime = this.mTime.hasCache() ? this.mTime.currentTimeMillis() : System.currentTimeMillis();
            try {
                NetworkStats uidSnapshot = getNetworkStatsUidDetail();
                NetworkStats xtSnapshot = this.mNetworkManager.getNetworkStatsSummaryXt();
                NetworkStats devSnapshot = this.mNetworkManager.getNetworkStatsSummaryDev();
                this.mDevRecorder.recordSnapshotLocked(devSnapshot, this.mActiveIfaces, currentTime);
                this.mXtRecorder.recordSnapshotLocked(xtSnapshot, this.mActiveIfaces, currentTime);
                this.mUidRecorder.recordSnapshotLocked(uidSnapshot, this.mActiveUidIfaces, currentTime);
                this.mUidTagRecorder.recordSnapshotLocked(uidSnapshot, this.mActiveUidIfaces, currentTime);
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
        long trustedTime = this.mTime.hasCache() ? this.mTime.currentTimeMillis() : -1L;
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
        performPollLocked(3);
        this.mUidRecorder.removeUidsLocked(uids);
        this.mUidTagRecorder.removeUidsLocked(uids);
        for (int uid : uids) {
            NetworkManagementSocketTagger.resetKernelUidStats(uid);
        }
    }

    private void removeUserLocked(int userId) {
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
        long duration = 86400000;
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
        boolean poll = (argSet.contains("--poll") || argSet.contains("poll")) ? true : LOGV;
        boolean checkin = argSet.contains("--checkin");
        boolean fullHistory = (argSet.contains("--full") || argSet.contains("full")) ? true : LOGV;
        boolean includeUid = (argSet.contains("--uid") || argSet.contains("detail")) ? true : LOGV;
        boolean includeTag = (argSet.contains("--tag") || argSet.contains("detail")) ? true : LOGV;
        IndentingPrintWriter pw = new IndentingPrintWriter(rawWriter, "  ");
        synchronized (this.mStatsLock) {
            if (poll) {
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
                if (includeUid) {
                    pw.println(PREFIX_UID);
                    this.mUidRecorder.dumpCheckin(rawWriter, start, end);
                }
                if (includeTag) {
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
            this.mDevRecorder.dumpLocked(pw, fullHistory);
            pw.decreaseIndent();
            pw.println("Xt stats:");
            pw.increaseIndent();
            this.mXtRecorder.dumpLocked(pw, fullHistory);
            pw.decreaseIndent();
            if (includeUid) {
                pw.println("UID stats:");
                pw.increaseIndent();
                this.mUidRecorder.dumpLocked(pw, fullHistory);
                pw.decreaseIndent();
            }
            if (includeTag) {
                pw.println("UID tag stats:");
                pw.increaseIndent();
                this.mUidTagRecorder.dumpLocked(pw, fullHistory);
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
            Log.wtf(TAG, "problem reading network stats", e);
            return new NetworkStats(0L, 10);
        }
    }

    private void assertBandwidthControlEnabled() {
        if (!isBandwidthControlEnabled()) {
            throw new IllegalStateException("Bandwidth module disabled");
        }
    }

    private boolean isBandwidthControlEnabled() {
        long token = Binder.clearCallingIdentity();
        try {
            return this.mNetworkManager.isBandwidthControlEnabled();
        } catch (RemoteException e) {
            return LOGV;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private class DropBoxNonMonotonicObserver implements NetworkStats.NonMonotonicObserver<String> {
        private DropBoxNonMonotonicObserver() {
        }

        public void foundNonMonotonic(NetworkStats left, int leftIndex, NetworkStats right, int rightIndex, String cookie) {
            Log.w(NetworkStatsService.TAG, "found non-monotonic values; saving to dropbox");
            StringBuilder builder = new StringBuilder();
            builder.append("found non-monotonic " + cookie + " values at left[" + leftIndex + "] - right[" + rightIndex + "]\n");
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
            if (Settings.Global.getInt(this.mResolver, name, defInt) != 0) {
                return true;
            }
            return NetworkStatsService.LOGV;
        }

        @Override
        public long getPollInterval() {
            return getGlobalLong("netstats_poll_interval", 1800000L);
        }

        @Override
        public long getTimeCacheMaxAge() {
            return getGlobalLong("netstats_time_cache_max_age", 86400000L);
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
}
