package com.android.server;

import android.app.ActivityManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.INetd;
import android.net.INetworkManagementEventObserver;
import android.net.ITetheringStatsProvider;
import android.net.InterfaceConfiguration;
import android.net.IpPrefix;
import android.net.LinkAddress;
import android.net.Network;
import android.net.NetworkStats;
import android.net.NetworkUtils;
import android.net.RouteInfo;
import android.net.UidRange;
import android.net.util.NetdService;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.INetworkActivityListener;
import android.os.INetworkManagementService;
import android.os.PersistableBundle;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ServiceSpecificException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.util.Log;
import android.util.Slog;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.IBatteryStats;
import com.android.internal.net.NetworkStatsFactory;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.HexDump;
import com.android.internal.util.Preconditions;
import com.android.server.BatteryService;
import com.android.server.NativeDaemonConnector;
import com.android.server.Watchdog;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.voiceinteraction.DatabaseHelper;
import com.google.android.collect.Maps;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.concurrent.CountDownLatch;

public class NetworkManagementService extends INetworkManagementService.Stub implements Watchdog.Monitor {
    static final int DAEMON_MSG_MOBILE_CONN_REAL_TIME_INFO = 1;
    public static final String LIMIT_GLOBAL_ALERT = "globalAlert";
    private static final int MAX_UID_RANGES_PER_COMMAND = 10;
    static final String NETD_SERVICE_NAME = "netd";
    private static final String NETD_TAG = "NetdConnector";
    public static final String PERMISSION_NETWORK = "NETWORK";
    public static final String PERMISSION_SYSTEM = "SYSTEM";
    static final String SOFT_AP_COMMAND = "softap";
    static final String SOFT_AP_COMMAND_SUCCESS = "Ok";

    @GuardedBy("mQuotaLock")
    private HashMap<String, Long> mActiveAlerts;
    private HashMap<String, IdleTimerParams> mActiveIdleTimers;

    @GuardedBy("mQuotaLock")
    private HashMap<String, Long> mActiveQuotas;
    private volatile boolean mBandwidthControlEnabled;
    private IBatteryStats mBatteryStats;
    private CountDownLatch mConnectedSignal;
    private final NativeDaemonConnector mConnector;
    private final Context mContext;
    private final Handler mDaemonHandler;

    @GuardedBy("mQuotaLock")
    private volatile boolean mDataSaverMode;
    private final Handler mFgHandler;

    @GuardedBy("mRulesLock")
    final SparseBooleanArray mFirewallChainStates;
    private volatile boolean mFirewallEnabled;
    private final Object mIdleTimerLock;
    private int mLastPowerStateFromRadio;
    private int mLastPowerStateFromWifi;
    private boolean mMobileActivityFromRadio;
    private INetd mNetdService;
    private boolean mNetworkActive;
    private final RemoteCallbackList<INetworkActivityListener> mNetworkActivityListeners;
    private final RemoteCallbackList<INetworkManagementEventObserver> mObservers;
    private final Object mQuotaLock;
    private final Object mRulesLock;
    private final SystemServices mServices;
    private final NetworkStatsFactory mStatsFactory;
    private volatile boolean mStrictEnabled;

    @GuardedBy("mTetheringStatsProviders")
    private final HashMap<ITetheringStatsProvider, String> mTetheringStatsProviders;
    private final Thread mThread;

    @GuardedBy("mRulesLock")
    private SparseBooleanArray mUidAllowOnMetered;

    @GuardedBy("mQuotaLock")
    private SparseIntArray mUidCleartextPolicy;

    @GuardedBy("mRulesLock")
    private SparseIntArray mUidFirewallDozableRules;

    @GuardedBy("mRulesLock")
    private SparseIntArray mUidFirewallPowerSaveRules;

    @GuardedBy("mRulesLock")
    private SparseIntArray mUidFirewallRules;

    @GuardedBy("mRulesLock")
    private SparseIntArray mUidFirewallStandbyRules;

    @GuardedBy("mRulesLock")
    private SparseBooleanArray mUidRejectOnMetered;
    private static final String TAG = "NetworkManagement";
    private static final boolean DBG = Log.isLoggable(TAG, 3);

    @FunctionalInterface
    private interface NetworkManagementEventCallback {
        void sendCallback(INetworkManagementEventObserver iNetworkManagementEventObserver) throws RemoteException;
    }

    static class SystemServices {
        SystemServices() {
        }

        public IBinder getService(String str) {
            return ServiceManager.getService(str);
        }

        public void registerLocalService(NetworkManagementInternal networkManagementInternal) {
            LocalServices.addService(NetworkManagementInternal.class, networkManagementInternal);
        }

        public INetd getNetd() {
            return NetdService.get();
        }
    }

    static class NetdResponseCode {
        public static final int BandwidthControl = 601;
        public static final int ClatdStatusResult = 223;
        public static final int DnsProxyQueryResult = 222;
        public static final int InterfaceAddressChange = 614;
        public static final int InterfaceChange = 600;
        public static final int InterfaceClassActivity = 613;
        public static final int InterfaceDnsServerInfo = 615;
        public static final int InterfaceGetCfgResult = 213;
        public static final int InterfaceListResult = 110;
        public static final int InterfaceRxCounterResult = 216;
        public static final int InterfaceTxCounterResult = 217;
        public static final int IpFwdStatusResult = 211;
        public static final int QuotaCounterResult = 220;
        public static final int RouteChange = 616;
        public static final int SoftapStatusResult = 214;
        public static final int StrictCleartext = 617;
        public static final int TetherDnsFwdTgtListResult = 112;
        public static final int TetherInterfaceListResult = 111;
        public static final int TetherStatusResult = 210;
        public static final int TetheringStatsListResult = 114;
        public static final int TetheringStatsResult = 221;
        public static final int TtyListResult = 113;

        NetdResponseCode() {
        }
    }

    private static class IdleTimerParams {
        public int networkCount = 1;
        public final int timeout;
        public final int type;

        IdleTimerParams(int i, int i2) {
            this.timeout = i;
            this.type = i2;
        }
    }

    private NetworkManagementService(Context context, String str, SystemServices systemServices) {
        this.mConnectedSignal = new CountDownLatch(1);
        this.mObservers = new RemoteCallbackList<>();
        this.mStatsFactory = new NetworkStatsFactory();
        this.mTetheringStatsProviders = Maps.newHashMap();
        this.mQuotaLock = new Object();
        this.mRulesLock = new Object();
        this.mActiveQuotas = Maps.newHashMap();
        this.mActiveAlerts = Maps.newHashMap();
        this.mUidRejectOnMetered = new SparseBooleanArray();
        this.mUidAllowOnMetered = new SparseBooleanArray();
        this.mUidCleartextPolicy = new SparseIntArray();
        this.mUidFirewallRules = new SparseIntArray();
        this.mUidFirewallStandbyRules = new SparseIntArray();
        this.mUidFirewallDozableRules = new SparseIntArray();
        this.mUidFirewallPowerSaveRules = new SparseIntArray();
        this.mFirewallChainStates = new SparseBooleanArray();
        this.mIdleTimerLock = new Object();
        this.mActiveIdleTimers = Maps.newHashMap();
        this.mMobileActivityFromRadio = false;
        this.mLastPowerStateFromRadio = 1;
        this.mLastPowerStateFromWifi = 1;
        this.mNetworkActivityListeners = new RemoteCallbackList<>();
        this.mContext = context;
        this.mServices = systemServices;
        this.mFgHandler = new Handler(FgThread.get().getLooper());
        this.mConnector = new NativeDaemonConnector(new NetdCallbackReceiver(), str, 10, NETD_TAG, 160, null, FgThread.get().getLooper());
        this.mThread = new Thread(this.mConnector, NETD_TAG);
        this.mDaemonHandler = new Handler(FgThread.get().getLooper());
        Watchdog.getInstance().addMonitor(this);
        this.mServices.registerLocalService(new LocalService());
        synchronized (this.mTetheringStatsProviders) {
            this.mTetheringStatsProviders.put(new NetdTetheringStatsProvider(), NETD_SERVICE_NAME);
        }
    }

    @VisibleForTesting
    NetworkManagementService() {
        this.mConnectedSignal = new CountDownLatch(1);
        this.mObservers = new RemoteCallbackList<>();
        this.mStatsFactory = new NetworkStatsFactory();
        this.mTetheringStatsProviders = Maps.newHashMap();
        this.mQuotaLock = new Object();
        this.mRulesLock = new Object();
        this.mActiveQuotas = Maps.newHashMap();
        this.mActiveAlerts = Maps.newHashMap();
        this.mUidRejectOnMetered = new SparseBooleanArray();
        this.mUidAllowOnMetered = new SparseBooleanArray();
        this.mUidCleartextPolicy = new SparseIntArray();
        this.mUidFirewallRules = new SparseIntArray();
        this.mUidFirewallStandbyRules = new SparseIntArray();
        this.mUidFirewallDozableRules = new SparseIntArray();
        this.mUidFirewallPowerSaveRules = new SparseIntArray();
        this.mFirewallChainStates = new SparseBooleanArray();
        this.mIdleTimerLock = new Object();
        this.mActiveIdleTimers = Maps.newHashMap();
        this.mMobileActivityFromRadio = false;
        this.mLastPowerStateFromRadio = 1;
        this.mLastPowerStateFromWifi = 1;
        this.mNetworkActivityListeners = new RemoteCallbackList<>();
        this.mConnector = null;
        this.mContext = null;
        this.mDaemonHandler = null;
        this.mFgHandler = null;
        this.mThread = null;
        this.mServices = null;
    }

    static NetworkManagementService create(Context context, String str, SystemServices systemServices) throws InterruptedException {
        NetworkManagementService networkManagementService = new NetworkManagementService(context, str, systemServices);
        CountDownLatch countDownLatch = networkManagementService.mConnectedSignal;
        if (DBG) {
            Slog.d(TAG, "Creating NetworkManagementService");
        }
        networkManagementService.mThread.start();
        if (DBG) {
            Slog.d(TAG, "Awaiting socket connection");
        }
        countDownLatch.await();
        if (DBG) {
            Slog.d(TAG, "Connected");
        }
        if (DBG) {
            Slog.d(TAG, "Connecting native netd service");
        }
        networkManagementService.connectNativeNetdService();
        if (DBG) {
            Slog.d(TAG, "Connected");
        }
        return networkManagementService;
    }

    public static NetworkManagementService create(Context context) throws InterruptedException {
        return create(context, NETD_SERVICE_NAME, new SystemServices());
    }

    public void systemReady() {
        if (DBG) {
            long jCurrentTimeMillis = System.currentTimeMillis();
            prepareNativeDaemon();
            Slog.d(TAG, "Prepared in " + (System.currentTimeMillis() - jCurrentTimeMillis) + "ms");
            return;
        }
        prepareNativeDaemon();
    }

    private IBatteryStats getBatteryStats() {
        synchronized (this) {
            if (this.mBatteryStats != null) {
                return this.mBatteryStats;
            }
            this.mBatteryStats = IBatteryStats.Stub.asInterface(this.mServices.getService("batterystats"));
            return this.mBatteryStats;
        }
    }

    public void registerObserver(INetworkManagementEventObserver iNetworkManagementEventObserver) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        this.mObservers.register(iNetworkManagementEventObserver);
    }

    public void unregisterObserver(INetworkManagementEventObserver iNetworkManagementEventObserver) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        this.mObservers.unregister(iNetworkManagementEventObserver);
    }

    private void invokeForAllObservers(NetworkManagementEventCallback networkManagementEventCallback) {
        int iBeginBroadcast = this.mObservers.beginBroadcast();
        for (int i = 0; i < iBeginBroadcast; i++) {
            try {
                networkManagementEventCallback.sendCallback((INetworkManagementEventObserver) this.mObservers.getBroadcastItem(i));
            } catch (RemoteException | RuntimeException e) {
            } catch (Throwable th) {
                this.mObservers.finishBroadcast();
                throw th;
            }
        }
        this.mObservers.finishBroadcast();
    }

    private void notifyInterfaceStatusChanged(final String str, final boolean z) {
        invokeForAllObservers(new NetworkManagementEventCallback() {
            @Override
            public final void sendCallback(INetworkManagementEventObserver iNetworkManagementEventObserver) {
                iNetworkManagementEventObserver.interfaceStatusChanged(str, z);
            }
        });
    }

    private void notifyInterfaceLinkStateChanged(final String str, final boolean z) {
        invokeForAllObservers(new NetworkManagementEventCallback() {
            @Override
            public final void sendCallback(INetworkManagementEventObserver iNetworkManagementEventObserver) {
                iNetworkManagementEventObserver.interfaceLinkStateChanged(str, z);
            }
        });
    }

    private void notifyInterfaceAdded(final String str) {
        invokeForAllObservers(new NetworkManagementEventCallback() {
            @Override
            public final void sendCallback(INetworkManagementEventObserver iNetworkManagementEventObserver) {
                iNetworkManagementEventObserver.interfaceAdded(str);
            }
        });
    }

    private void notifyInterfaceRemoved(final String str) {
        this.mActiveAlerts.remove(str);
        this.mActiveQuotas.remove(str);
        invokeForAllObservers(new NetworkManagementEventCallback() {
            @Override
            public final void sendCallback(INetworkManagementEventObserver iNetworkManagementEventObserver) {
                iNetworkManagementEventObserver.interfaceRemoved(str);
            }
        });
    }

    private void notifyLimitReached(final String str, final String str2) {
        invokeForAllObservers(new NetworkManagementEventCallback() {
            @Override
            public final void sendCallback(INetworkManagementEventObserver iNetworkManagementEventObserver) {
                iNetworkManagementEventObserver.limitReached(str, str2);
            }
        });
    }

    private void notifyInterfaceClassActivity(final int i, int i2, final long j, int i3, boolean z) {
        boolean zIsNetworkTypeMobile = ConnectivityManager.isNetworkTypeMobile(i);
        if (zIsNetworkTypeMobile) {
            if (!z) {
                if (this.mMobileActivityFromRadio) {
                    i2 = this.mLastPowerStateFromRadio;
                }
            } else {
                this.mMobileActivityFromRadio = true;
            }
            if (this.mLastPowerStateFromRadio != i2) {
                this.mLastPowerStateFromRadio = i2;
                try {
                    getBatteryStats().noteMobileRadioPowerState(i2, j, i3);
                } catch (RemoteException e) {
                }
            }
        }
        if (ConnectivityManager.isNetworkTypeWifi(i) && this.mLastPowerStateFromWifi != i2) {
            this.mLastPowerStateFromWifi = i2;
            try {
                getBatteryStats().noteWifiRadioPowerState(i2, j, i3);
            } catch (RemoteException e2) {
            }
        }
        final boolean z2 = i2 == 2 || i2 == 3;
        if (!zIsNetworkTypeMobile || z || !this.mMobileActivityFromRadio) {
            invokeForAllObservers(new NetworkManagementEventCallback() {
                @Override
                public final void sendCallback(INetworkManagementEventObserver iNetworkManagementEventObserver) {
                    iNetworkManagementEventObserver.interfaceClassDataActivityChanged(Integer.toString(i), z2, j);
                }
            });
        }
        synchronized (this.mIdleTimerLock) {
            if (this.mActiveIdleTimers.isEmpty()) {
                z2 = true;
            }
            if (this.mNetworkActive != z2) {
                this.mNetworkActive = z2;
            } else {
                z2 = false;
            }
        }
        if (z2) {
            reportNetworkActive();
        }
    }

    public void registerTetheringStatsProvider(ITetheringStatsProvider iTetheringStatsProvider, String str) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.NETWORK_STACK", TAG);
        Preconditions.checkNotNull(iTetheringStatsProvider);
        synchronized (this.mTetheringStatsProviders) {
            this.mTetheringStatsProviders.put(iTetheringStatsProvider, str);
        }
    }

    public void unregisterTetheringStatsProvider(ITetheringStatsProvider iTetheringStatsProvider) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.NETWORK_STACK", TAG);
        synchronized (this.mTetheringStatsProviders) {
            this.mTetheringStatsProviders.remove(iTetheringStatsProvider);
        }
    }

    public void tetherLimitReached(ITetheringStatsProvider iTetheringStatsProvider) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.NETWORK_STACK", TAG);
        synchronized (this.mTetheringStatsProviders) {
            if (this.mTetheringStatsProviders.containsKey(iTetheringStatsProvider)) {
                notifyLimitReached(LIMIT_GLOBAL_ALERT, null);
            }
        }
    }

    private void syncFirewallChainLocked(int i, String str) {
        SparseIntArray sparseIntArrayClone;
        synchronized (this.mRulesLock) {
            SparseIntArray uidFirewallRulesLR = getUidFirewallRulesLR(i);
            sparseIntArrayClone = uidFirewallRulesLR.clone();
            uidFirewallRulesLR.clear();
        }
        if (sparseIntArrayClone.size() > 0) {
            if (DBG) {
                Slog.d(TAG, "Pushing " + sparseIntArrayClone.size() + " active firewall " + str + "UID rules");
            }
            for (int i2 = 0; i2 < sparseIntArrayClone.size(); i2++) {
                setFirewallUidRuleLocked(i, sparseIntArrayClone.keyAt(i2), sparseIntArrayClone.valueAt(i2));
            }
        }
    }

    private void connectNativeNetdService() {
        this.mNetdService = this.mServices.getNetd();
    }

    private void prepareNativeDaemon() {
        SparseBooleanArray sparseBooleanArray;
        SparseBooleanArray sparseBooleanArray2;
        if (!Build.IS_USER) {
            this.mConnector.setDebug(true);
        }
        this.mBandwidthControlEnabled = false;
        boolean zExists = new File("/proc/net/xt_qtaguid/ctrl").exists();
        synchronized (this.mQuotaLock) {
            try {
                if (zExists) {
                    Slog.d(TAG, "enabling bandwidth control");
                    try {
                        this.mConnector.execute("bandwidth", "enable");
                        this.mBandwidthControlEnabled = true;
                    } catch (NativeDaemonConnectorException e) {
                        Log.wtf(TAG, "problem enabling bandwidth controls", e);
                    }
                } else {
                    Slog.i(TAG, "not enabling bandwidth control");
                }
                SystemProperties.set("net.qtaguid_enabled", this.mBandwidthControlEnabled ? "1" : "0");
                try {
                    this.mConnector.execute("strict", "enable");
                    this.mStrictEnabled = true;
                } catch (NativeDaemonConnectorException e2) {
                    Log.wtf(TAG, "Failed strict enable", e2);
                }
                setDataSaverModeEnabled(this.mDataSaverMode);
                int size = this.mActiveQuotas.size();
                if (size > 0) {
                    if (DBG) {
                        Slog.d(TAG, "Pushing " + size + " active quota rules");
                    }
                    HashMap<String, Long> map = this.mActiveQuotas;
                    this.mActiveQuotas = Maps.newHashMap();
                    for (Map.Entry<String, Long> entry : map.entrySet()) {
                        setInterfaceQuota(entry.getKey(), entry.getValue().longValue());
                    }
                }
                int size2 = this.mActiveAlerts.size();
                if (size2 > 0) {
                    if (DBG) {
                        Slog.d(TAG, "Pushing " + size2 + " active alert rules");
                    }
                    HashMap<String, Long> map2 = this.mActiveAlerts;
                    this.mActiveAlerts = Maps.newHashMap();
                    for (Map.Entry<String, Long> entry2 : map2.entrySet()) {
                        setInterfaceAlert(entry2.getKey(), entry2.getValue().longValue());
                    }
                }
                synchronized (this.mRulesLock) {
                    int size3 = this.mUidRejectOnMetered.size();
                    sparseBooleanArray = null;
                    if (size3 > 0) {
                        if (DBG) {
                            Slog.d(TAG, "Pushing " + size3 + " UIDs to metered blacklist rules");
                        }
                        sparseBooleanArray2 = this.mUidRejectOnMetered;
                        this.mUidRejectOnMetered = new SparseBooleanArray();
                    } else {
                        sparseBooleanArray2 = null;
                    }
                    int size4 = this.mUidAllowOnMetered.size();
                    if (size4 > 0) {
                        if (DBG) {
                            Slog.d(TAG, "Pushing " + size4 + " UIDs to metered whitelist rules");
                        }
                        sparseBooleanArray = this.mUidAllowOnMetered;
                        this.mUidAllowOnMetered = new SparseBooleanArray();
                    }
                }
                if (sparseBooleanArray2 != null) {
                    for (int i = 0; i < sparseBooleanArray2.size(); i++) {
                        setUidMeteredNetworkBlacklist(sparseBooleanArray2.keyAt(i), sparseBooleanArray2.valueAt(i));
                    }
                }
                if (sparseBooleanArray != null) {
                    for (int i2 = 0; i2 < sparseBooleanArray.size(); i2++) {
                        setUidMeteredNetworkWhitelist(sparseBooleanArray.keyAt(i2), sparseBooleanArray.valueAt(i2));
                    }
                }
                int size5 = this.mUidCleartextPolicy.size();
                if (size5 > 0) {
                    if (DBG) {
                        Slog.d(TAG, "Pushing " + size5 + " active UID cleartext policies");
                    }
                    SparseIntArray sparseIntArray = this.mUidCleartextPolicy;
                    this.mUidCleartextPolicy = new SparseIntArray();
                    for (int i3 = 0; i3 < sparseIntArray.size(); i3++) {
                        setUidCleartextNetworkPolicy(sparseIntArray.keyAt(i3), sparseIntArray.valueAt(i3));
                    }
                }
                setFirewallEnabled(this.mFirewallEnabled);
                syncFirewallChainLocked(0, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                syncFirewallChainLocked(2, "standby ");
                syncFirewallChainLocked(1, "dozable ");
                syncFirewallChainLocked(3, "powersave ");
                for (int i4 : new int[]{2, 1, 3}) {
                    if (getFirewallChainState(i4)) {
                        setFirewallChainEnabled(i4, true);
                    }
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        if (this.mBandwidthControlEnabled) {
            try {
                getBatteryStats().noteNetworkStatsEnabled();
            } catch (RemoteException e3) {
            }
        }
    }

    private void notifyAddressUpdated(final String str, final LinkAddress linkAddress) {
        invokeForAllObservers(new NetworkManagementEventCallback() {
            @Override
            public final void sendCallback(INetworkManagementEventObserver iNetworkManagementEventObserver) {
                iNetworkManagementEventObserver.addressUpdated(str, linkAddress);
            }
        });
    }

    private void notifyAddressRemoved(final String str, final LinkAddress linkAddress) {
        invokeForAllObservers(new NetworkManagementEventCallback() {
            @Override
            public final void sendCallback(INetworkManagementEventObserver iNetworkManagementEventObserver) {
                iNetworkManagementEventObserver.addressRemoved(str, linkAddress);
            }
        });
    }

    private void notifyInterfaceDnsServerInfo(final String str, final long j, final String[] strArr) {
        invokeForAllObservers(new NetworkManagementEventCallback() {
            @Override
            public final void sendCallback(INetworkManagementEventObserver iNetworkManagementEventObserver) {
                iNetworkManagementEventObserver.interfaceDnsServerInfo(str, j, strArr);
            }
        });
    }

    private void notifyRouteChange(String str, final RouteInfo routeInfo) {
        if (str.equals("updated")) {
            invokeForAllObservers(new NetworkManagementEventCallback() {
                @Override
                public final void sendCallback(INetworkManagementEventObserver iNetworkManagementEventObserver) {
                    iNetworkManagementEventObserver.routeUpdated(routeInfo);
                }
            });
        } else {
            invokeForAllObservers(new NetworkManagementEventCallback() {
                @Override
                public final void sendCallback(INetworkManagementEventObserver iNetworkManagementEventObserver) {
                    iNetworkManagementEventObserver.routeRemoved(routeInfo);
                }
            });
        }
    }

    private class NetdCallbackReceiver implements INativeDaemonConnectorCallbacks {
        private NetdCallbackReceiver() {
        }

        @Override
        public void onDaemonConnected() {
            Slog.i(NetworkManagementService.TAG, "onDaemonConnected()");
            if (NetworkManagementService.this.mConnectedSignal != null) {
                NetworkManagementService.this.mConnectedSignal.countDown();
                NetworkManagementService.this.mConnectedSignal = null;
            } else {
                NetworkManagementService.this.mFgHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        NetworkManagementService.this.connectNativeNetdService();
                        NetworkManagementService.this.prepareNativeDaemon();
                    }
                });
            }
        }

        @Override
        public boolean onCheckHoldWakeLock(int i) {
            return i == 613;
        }

        @Override
        public boolean onEvent(int i, String str, String[] strArr) {
            long jElapsedRealtimeNanos;
            String str2 = String.format("Invalid event from daemon (%s)", str);
            int i2 = 4;
            switch (i) {
                case 600:
                    if (strArr.length < 4 || !strArr[1].equals("Iface")) {
                        throw new IllegalStateException(str2);
                    }
                    if (strArr[2].equals("added")) {
                        NetworkManagementService.this.notifyInterfaceAdded(strArr[3]);
                        return true;
                    }
                    if (strArr[2].equals("removed")) {
                        NetworkManagementService.this.notifyInterfaceRemoved(strArr[3]);
                        return true;
                    }
                    if (strArr[2].equals("changed") && strArr.length == 5) {
                        NetworkManagementService.this.notifyInterfaceStatusChanged(strArr[3], strArr[4].equals("up"));
                        return true;
                    }
                    if (strArr[2].equals("linkstate") && strArr.length == 5) {
                        NetworkManagementService.this.notifyInterfaceLinkStateChanged(strArr[3], strArr[4].equals("up"));
                        return true;
                    }
                    throw new IllegalStateException(str2);
                case NetdResponseCode.BandwidthControl:
                    if (strArr.length < 5 || !strArr[1].equals("limit")) {
                        throw new IllegalStateException(str2);
                    }
                    if (strArr[2].equals("alert")) {
                        NetworkManagementService.this.notifyLimitReached(strArr[3], strArr[4]);
                        return true;
                    }
                    throw new IllegalStateException(str2);
                default:
                    switch (i) {
                        case NetdResponseCode.InterfaceClassActivity:
                            if (strArr.length < 4 || !strArr[1].equals("IfaceClass")) {
                                throw new IllegalStateException(str2);
                            }
                            int i3 = -1;
                            if (strArr.length >= 5) {
                                try {
                                    jElapsedRealtimeNanos = Long.parseLong(strArr[4]);
                                    try {
                                        if (strArr.length == 6) {
                                            i3 = Integer.parseInt(strArr[5]);
                                        }
                                    } catch (NumberFormatException e) {
                                    }
                                } catch (NumberFormatException e2) {
                                    jElapsedRealtimeNanos = 0;
                                }
                            } else {
                                jElapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos();
                            }
                            NetworkManagementService.this.notifyInterfaceClassActivity(Integer.parseInt(strArr[3]), strArr[2].equals("active") ? 3 : 1, jElapsedRealtimeNanos, i3, false);
                            return true;
                        case NetdResponseCode.InterfaceAddressChange:
                            if (strArr.length < 7 || !strArr[1].equals("Address")) {
                                throw new IllegalStateException(str2);
                            }
                            String str3 = strArr[4];
                            try {
                                LinkAddress linkAddress = new LinkAddress(strArr[3], Integer.parseInt(strArr[5]), Integer.parseInt(strArr[6]));
                                if (strArr[2].equals("updated")) {
                                    NetworkManagementService.this.notifyAddressUpdated(str3, linkAddress);
                                } else {
                                    NetworkManagementService.this.notifyAddressRemoved(str3, linkAddress);
                                }
                                return true;
                            } catch (NumberFormatException e3) {
                                throw new IllegalStateException(str2, e3);
                            } catch (IllegalArgumentException e4) {
                                throw new IllegalStateException(str2, e4);
                            }
                        case NetdResponseCode.InterfaceDnsServerInfo:
                            if (strArr.length == 6 && strArr[1].equals("DnsInfo") && strArr[2].equals("servers")) {
                                try {
                                    NetworkManagementService.this.notifyInterfaceDnsServerInfo(strArr[3], Long.parseLong(strArr[4]), strArr[5].split(","));
                                } catch (NumberFormatException e5) {
                                    throw new IllegalStateException(str2);
                                }
                            }
                            return true;
                        case NetdResponseCode.RouteChange:
                            if (!strArr[1].equals("Route") || strArr.length < 6) {
                                throw new IllegalStateException(str2);
                            }
                            InetAddress numericAddress = null;
                            String str4 = null;
                            String str5 = null;
                            boolean z = true;
                            while (true) {
                                int i4 = i2 + 1;
                                if (i4 < strArr.length && z) {
                                    if (strArr[i2].equals("dev")) {
                                        if (str5 != null) {
                                            z = false;
                                        } else {
                                            str5 = strArr[i4];
                                        }
                                    } else if (strArr[i2].equals("via") && str4 == null) {
                                        str4 = strArr[i4];
                                    }
                                    i2 += 2;
                                }
                            }
                            if (z) {
                                if (str4 != null) {
                                    try {
                                        numericAddress = InetAddress.parseNumericAddress(str4);
                                    } catch (IllegalArgumentException e6) {
                                    }
                                }
                                NetworkManagementService.this.notifyRouteChange(strArr[2], new RouteInfo(new IpPrefix(strArr[3]), numericAddress, str5));
                                return true;
                            }
                            throw new IllegalStateException(str2);
                        case NetdResponseCode.StrictCleartext:
                            try {
                                ActivityManager.getService().notifyCleartextNetwork(Integer.parseInt(strArr[1]), HexDump.hexStringToByteArray(strArr[2]));
                                break;
                            } catch (RemoteException e7) {
                                break;
                            }
                        default:
                            return false;
                    }
                    break;
            }
        }
    }

    public INetd getNetdService() throws RemoteException {
        CountDownLatch countDownLatch = this.mConnectedSignal;
        if (countDownLatch != null) {
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
            }
        }
        return this.mNetdService;
    }

    public String[] listInterfaces() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            return NativeDaemonEvent.filterMessageList(this.mConnector.executeForList("interface", "list"), NetdResponseCode.InterfaceListResult);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public InterfaceConfiguration getInterfaceConfig(String str) {
        int i;
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            NativeDaemonEvent nativeDaemonEventExecute = this.mConnector.execute("interface", "getcfg", str);
            nativeDaemonEventExecute.checkCode(NetdResponseCode.InterfaceGetCfgResult);
            StringTokenizer stringTokenizer = new StringTokenizer(nativeDaemonEventExecute.getMessage());
            try {
                InterfaceConfiguration interfaceConfiguration = new InterfaceConfiguration();
                interfaceConfiguration.setHardwareAddress(stringTokenizer.nextToken(" "));
                InetAddress inetAddressNumericToInetAddress = null;
                try {
                    inetAddressNumericToInetAddress = NetworkUtils.numericToInetAddress(stringTokenizer.nextToken());
                } catch (IllegalArgumentException e) {
                    Slog.e(TAG, "Failed to parse ipaddr", e);
                }
                try {
                    i = Integer.parseInt(stringTokenizer.nextToken());
                } catch (NumberFormatException e2) {
                    Slog.e(TAG, "Failed to parse prefixLength", e2);
                    i = 0;
                }
                interfaceConfiguration.setLinkAddress(new LinkAddress(inetAddressNumericToInetAddress, i));
                while (stringTokenizer.hasMoreTokens()) {
                    interfaceConfiguration.setFlag(stringTokenizer.nextToken());
                }
                return interfaceConfiguration;
            } catch (NoSuchElementException e3) {
                throw new IllegalStateException("Invalid response from daemon: " + nativeDaemonEventExecute);
            }
        } catch (NativeDaemonConnectorException e4) {
            throw e4.rethrowAsParcelableException();
        }
    }

    public void setInterfaceConfig(String str, InterfaceConfiguration interfaceConfiguration) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        LinkAddress linkAddress = interfaceConfiguration.getLinkAddress();
        if (linkAddress == null || linkAddress.getAddress() == null) {
            throw new IllegalStateException("Null LinkAddress given");
        }
        NativeDaemonConnector.Command command = new NativeDaemonConnector.Command("interface", "setcfg", str, linkAddress.getAddress().getHostAddress(), Integer.valueOf(linkAddress.getPrefixLength()));
        Iterator it = interfaceConfiguration.getFlags().iterator();
        while (it.hasNext()) {
            command.appendArg((String) it.next());
        }
        try {
            this.mConnector.execute(command);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void setInterfaceDown(String str) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        InterfaceConfiguration interfaceConfig = getInterfaceConfig(str);
        interfaceConfig.setInterfaceDown();
        setInterfaceConfig(str, interfaceConfig);
    }

    public void setInterfaceUp(String str) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        InterfaceConfiguration interfaceConfig = getInterfaceConfig(str);
        interfaceConfig.setInterfaceUp();
        setInterfaceConfig(str, interfaceConfig);
    }

    public void setInterfaceIpv6PrivacyExtensions(String str, boolean z) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            NativeDaemonConnector nativeDaemonConnector = this.mConnector;
            Object[] objArr = new Object[3];
            objArr[0] = "ipv6privacyextensions";
            objArr[1] = str;
            objArr[2] = z ? "enable" : "disable";
            nativeDaemonConnector.execute("interface", objArr);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void clearInterfaceAddresses(String str) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("interface", "clearaddrs", str);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void enableIpv6(String str) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("interface", "ipv6", str, "enable");
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void setIPv6AddrGenMode(String str, int i) throws ServiceSpecificException {
        try {
            this.mNetdService.setIPv6AddrGenMode(str, i);
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    public void disableIpv6(String str) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("interface", "ipv6", str, "disable");
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void addRoute(int i, RouteInfo routeInfo) {
        modifyRoute("add", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS + i, routeInfo);
    }

    public void removeRoute(int i, RouteInfo routeInfo) {
        modifyRoute("remove", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS + i, routeInfo);
    }

    private void modifyRoute(String str, String str2, RouteInfo routeInfo) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        NativeDaemonConnector.Command command = new NativeDaemonConnector.Command("network", "route", str, str2);
        command.appendArg(routeInfo.getInterface());
        command.appendArg(routeInfo.getDestination().toString());
        int type = routeInfo.getType();
        if (type != 1) {
            if (type == 7) {
                command.appendArg("unreachable");
            } else if (type == 9) {
                command.appendArg("throw");
            }
        } else if (routeInfo.hasGateway()) {
            command.appendArg(routeInfo.getGateway().getHostAddress());
        }
        try {
            this.mConnector.execute(command);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    private ArrayList<String> readRouteList(String str) throws Throwable {
        FileInputStream fileInputStream;
        BufferedReader bufferedReader;
        ArrayList<String> arrayList = new ArrayList<>();
        try {
            fileInputStream = new FileInputStream(str);
            try {
                bufferedReader = new BufferedReader(new InputStreamReader(new DataInputStream(fileInputStream)));
            } catch (IOException e) {
                if (fileInputStream != null) {
                }
                return arrayList;
            } catch (Throwable th) {
                th = th;
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException e2) {
                    }
                }
                throw th;
            }
        } catch (IOException e3) {
            fileInputStream = null;
        } catch (Throwable th2) {
            th = th2;
            fileInputStream = null;
        }
        while (true) {
            String line = bufferedReader.readLine();
            if (line != null && line.length() != 0) {
                arrayList.add(line);
            }
            try {
                fileInputStream.close();
                break;
            } catch (IOException e4) {
            }
        }
        return arrayList;
    }

    public void setMtu(String str, int i) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("interface", "setmtu", str, Integer.valueOf(i));
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void shutdown() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.SHUTDOWN", TAG);
        Slog.i(TAG, "Shutting down");
    }

    public boolean getIpForwardingEnabled() throws IllegalStateException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            NativeDaemonEvent nativeDaemonEventExecute = this.mConnector.execute("ipfwd", "status");
            nativeDaemonEventExecute.checkCode(NetdResponseCode.IpFwdStatusResult);
            return nativeDaemonEventExecute.getMessage().endsWith("enabled");
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void setIpForwardingEnabled(boolean z) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            NativeDaemonConnector nativeDaemonConnector = this.mConnector;
            Object[] objArr = new Object[2];
            objArr[0] = z ? "enable" : "disable";
            objArr[1] = ConnectivityService.TETHERING_ARG;
            nativeDaemonConnector.execute("ipfwd", objArr);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void startTethering(String[] strArr) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        NativeDaemonConnector.Command command = new NativeDaemonConnector.Command("tether", "start");
        for (String str : strArr) {
            command.appendArg(str);
        }
        try {
            this.mConnector.execute(command);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void stopTethering() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("tether", "stop");
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public boolean isTetheringStarted() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            NativeDaemonEvent nativeDaemonEventExecute = this.mConnector.execute("tether", "status");
            nativeDaemonEventExecute.checkCode(NetdResponseCode.TetherStatusResult);
            return nativeDaemonEventExecute.getMessage().endsWith("started");
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void tetherInterface(String str) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("tether", "interface", "add", str);
            ArrayList arrayList = new ArrayList();
            arrayList.add(new RouteInfo(getInterfaceConfig(str).getLinkAddress(), null, str));
            addInterfaceToLocalNetwork(str, arrayList);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void untetherInterface(String str) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            try {
                this.mConnector.execute("tether", "interface", "remove", str);
            } catch (NativeDaemonConnectorException e) {
                throw e.rethrowAsParcelableException();
            }
        } finally {
            removeInterfaceFromLocalNetwork(str);
        }
    }

    public String[] listTetheredInterfaces() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            return NativeDaemonEvent.filterMessageList(this.mConnector.executeForList("tether", "interface", "list"), NetdResponseCode.TetherInterfaceListResult);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void setDnsForwarders(Network network, String[] strArr) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        NativeDaemonConnector.Command command = new NativeDaemonConnector.Command("tether", "dns", "set", Integer.valueOf(network != null ? network.netId : 0));
        for (String str : strArr) {
            command.appendArg(NetworkUtils.numericToInetAddress(str).getHostAddress());
        }
        try {
            this.mConnector.execute(command);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public String[] getDnsForwarders() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            return NativeDaemonEvent.filterMessageList(this.mConnector.executeForList("tether", "dns", "list"), 112);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    private List<InterfaceAddress> excludeLinkLocal(List<InterfaceAddress> list) {
        ArrayList arrayList = new ArrayList(list.size());
        for (InterfaceAddress interfaceAddress : list) {
            if (!interfaceAddress.getAddress().isLinkLocalAddress()) {
                arrayList.add(interfaceAddress);
            }
        }
        return arrayList;
    }

    private void modifyInterfaceForward(boolean z, String str, String str2) {
        Object[] objArr = new Object[3];
        objArr[0] = z ? "add" : "remove";
        objArr[1] = str;
        objArr[2] = str2;
        try {
            this.mConnector.execute(new NativeDaemonConnector.Command("ipfwd", objArr));
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void startInterfaceForwarding(String str, String str2) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        modifyInterfaceForward(true, str, str2);
    }

    public void stopInterfaceForwarding(String str, String str2) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        modifyInterfaceForward(false, str, str2);
    }

    private void modifyNat(String str, String str2, String str3) throws SocketException {
        NativeDaemonConnector.Command command = new NativeDaemonConnector.Command("nat", str, str2, str3);
        NetworkInterface byName = NetworkInterface.getByName(str2);
        if (byName == null) {
            command.appendArg("0");
        } else {
            List<InterfaceAddress> listExcludeLinkLocal = excludeLinkLocal(byName.getInterfaceAddresses());
            command.appendArg(Integer.valueOf(listExcludeLinkLocal.size()));
            for (InterfaceAddress interfaceAddress : listExcludeLinkLocal) {
                command.appendArg(NetworkUtils.getNetworkPart(interfaceAddress.getAddress(), interfaceAddress.getNetworkPrefixLength()).getHostAddress() + SliceClientPermissions.SliceAuthority.DELIMITER + ((int) interfaceAddress.getNetworkPrefixLength()));
            }
        }
        try {
            this.mConnector.execute(command);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void enableNat(String str, String str2) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            modifyNat("enable", str, str2);
        } catch (SocketException e) {
            throw new IllegalStateException(e);
        }
    }

    public void disableNat(String str, String str2) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            modifyNat("disable", str, str2);
        } catch (SocketException e) {
            throw new IllegalStateException(e);
        }
    }

    public String[] listTtys() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            return NativeDaemonEvent.filterMessageList(this.mConnector.executeForList("list_ttys", new Object[0]), 113);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void attachPppd(String str, String str2, String str3, String str4, String str5) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("pppd", "attach", str, NetworkUtils.numericToInetAddress(str2).getHostAddress(), NetworkUtils.numericToInetAddress(str3).getHostAddress(), NetworkUtils.numericToInetAddress(str4).getHostAddress(), NetworkUtils.numericToInetAddress(str5).getHostAddress());
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void detachPppd(String str) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("pppd", "detach", str);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void addIdleTimer(String str, int i, final int i2) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        if (DBG) {
            Slog.d(TAG, "Adding idletimer");
        }
        synchronized (this.mIdleTimerLock) {
            IdleTimerParams idleTimerParams = this.mActiveIdleTimers.get(str);
            if (idleTimerParams != null) {
                idleTimerParams.networkCount++;
                return;
            }
            try {
                this.mConnector.execute("idletimer", "add", str, Integer.toString(i), Integer.toString(i2));
                this.mActiveIdleTimers.put(str, new IdleTimerParams(i, i2));
                if (ConnectivityManager.isNetworkTypeMobile(i2)) {
                    this.mNetworkActive = false;
                }
                this.mDaemonHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        NetworkManagementService.this.notifyInterfaceClassActivity(i2, 3, SystemClock.elapsedRealtimeNanos(), -1, false);
                    }
                });
            } catch (NativeDaemonConnectorException e) {
                throw e.rethrowAsParcelableException();
            }
        }
    }

    public void removeIdleTimer(String str) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        if (DBG) {
            Slog.d(TAG, "Removing idletimer");
        }
        synchronized (this.mIdleTimerLock) {
            final IdleTimerParams idleTimerParams = this.mActiveIdleTimers.get(str);
            if (idleTimerParams != null) {
                int i = idleTimerParams.networkCount - 1;
                idleTimerParams.networkCount = i;
                if (i <= 0) {
                    try {
                        this.mConnector.execute("idletimer", "remove", str, Integer.toString(idleTimerParams.timeout), Integer.toString(idleTimerParams.type));
                        this.mActiveIdleTimers.remove(str);
                        this.mDaemonHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                NetworkManagementService.this.notifyInterfaceClassActivity(idleTimerParams.type, 1, SystemClock.elapsedRealtimeNanos(), -1, false);
                            }
                        });
                    } catch (NativeDaemonConnectorException e) {
                        throw e.rethrowAsParcelableException();
                    }
                }
            }
        }
    }

    public NetworkStats getNetworkStatsSummaryDev() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            return this.mStatsFactory.readNetworkStatsSummaryDev();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public NetworkStats getNetworkStatsSummaryXt() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            return this.mStatsFactory.readNetworkStatsSummaryXt();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public NetworkStats getNetworkStatsDetail() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            return this.mStatsFactory.readNetworkStatsDetail(-1, (String[]) null, -1, (NetworkStats) null);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public void setInterfaceQuota(String str, long j) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        if (this.mBandwidthControlEnabled) {
            synchronized (this.mQuotaLock) {
                try {
                    try {
                        this.mConnector.execute("bandwidth", "setiquota", str, Long.valueOf(j));
                        this.mActiveQuotas.put(str, Long.valueOf(j));
                        synchronized (this.mTetheringStatsProviders) {
                            for (ITetheringStatsProvider iTetheringStatsProvider : this.mTetheringStatsProviders.keySet()) {
                                try {
                                    iTetheringStatsProvider.setInterfaceQuota(str, j);
                                } catch (RemoteException e) {
                                    Log.e(TAG, "Problem setting tethering data limit on provider " + this.mTetheringStatsProviders.get(iTetheringStatsProvider) + ": " + e);
                                }
                            }
                        }
                    } catch (Throwable th) {
                        throw th;
                    }
                } catch (NativeDaemonConnectorException e2) {
                    throw e2.rethrowAsParcelableException();
                }
            }
        }
    }

    public void removeInterfaceQuota(String str) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        if (this.mBandwidthControlEnabled) {
            synchronized (this.mQuotaLock) {
                if (this.mActiveQuotas.containsKey(str)) {
                    this.mActiveQuotas.remove(str);
                    this.mActiveAlerts.remove(str);
                    try {
                        this.mConnector.execute("bandwidth", "removeiquota", str);
                        synchronized (this.mTetheringStatsProviders) {
                            for (ITetheringStatsProvider iTetheringStatsProvider : this.mTetheringStatsProviders.keySet()) {
                                try {
                                    iTetheringStatsProvider.setInterfaceQuota(str, -1L);
                                } catch (RemoteException e) {
                                    Log.e(TAG, "Problem removing tethering data limit on provider " + this.mTetheringStatsProviders.get(iTetheringStatsProvider) + ": " + e);
                                }
                            }
                        }
                    } catch (NativeDaemonConnectorException e2) {
                        throw e2.rethrowAsParcelableException();
                    }
                }
            }
        }
    }

    public void setInterfaceAlert(String str, long j) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        if (this.mBandwidthControlEnabled) {
            if (!this.mActiveQuotas.containsKey(str)) {
                throw new IllegalStateException("setting alert requires existing quota on iface");
            }
            synchronized (this.mQuotaLock) {
                if (this.mActiveAlerts.containsKey(str)) {
                    throw new IllegalStateException("iface " + str + " already has alert");
                }
                try {
                    this.mConnector.execute("bandwidth", "setinterfacealert", str, Long.valueOf(j));
                    this.mActiveAlerts.put(str, Long.valueOf(j));
                } catch (NativeDaemonConnectorException e) {
                    throw e.rethrowAsParcelableException();
                }
            }
        }
    }

    public void removeInterfaceAlert(String str) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        if (this.mBandwidthControlEnabled) {
            synchronized (this.mQuotaLock) {
                if (this.mActiveAlerts.containsKey(str)) {
                    try {
                        this.mConnector.execute("bandwidth", "removeinterfacealert", str);
                        this.mActiveAlerts.remove(str);
                    } catch (NativeDaemonConnectorException e) {
                        throw e.rethrowAsParcelableException();
                    }
                }
            }
        }
    }

    public void setGlobalAlert(long j) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        if (this.mBandwidthControlEnabled) {
            try {
                this.mConnector.execute("bandwidth", "setglobalalert", Long.valueOf(j));
            } catch (NativeDaemonConnectorException e) {
                throw e.rethrowAsParcelableException();
            }
        }
    }

    private void setUidOnMeteredNetworkList(int i, boolean z, boolean z2) {
        SparseBooleanArray sparseBooleanArray;
        boolean z3;
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        if (this.mBandwidthControlEnabled) {
            String str = z ? "naughtyapps" : "niceapps";
            String str2 = z2 ? "add" : "remove";
            synchronized (this.mQuotaLock) {
                synchronized (this.mRulesLock) {
                    try {
                        sparseBooleanArray = z ? this.mUidRejectOnMetered : this.mUidAllowOnMetered;
                        z3 = sparseBooleanArray.get(i, false);
                    } finally {
                    }
                }
                if (z3 == z2) {
                    return;
                }
                Trace.traceBegin(2097152L, "inetd bandwidth");
                try {
                    try {
                        this.mConnector.execute("bandwidth", str2 + str, Integer.valueOf(i));
                        synchronized (this.mRulesLock) {
                            try {
                                if (z2) {
                                    sparseBooleanArray.put(i, true);
                                } else {
                                    sparseBooleanArray.delete(i);
                                }
                            } finally {
                            }
                        }
                    } catch (NativeDaemonConnectorException e) {
                        throw e.rethrowAsParcelableException();
                    }
                } finally {
                    Trace.traceEnd(2097152L);
                }
            }
        }
    }

    public void setUidMeteredNetworkBlacklist(int i, boolean z) {
        setUidOnMeteredNetworkList(i, true, z);
    }

    public void setUidMeteredNetworkWhitelist(int i, boolean z) {
        setUidOnMeteredNetworkList(i, false, z);
    }

    public boolean setDataSaverModeEnabled(boolean z) {
        Context context = this.mContext;
        String str = TAG;
        context.enforceCallingOrSelfPermission("android.permission.NETWORK_SETTINGS", TAG);
        long j = str;
        if (DBG) {
            Log.d(TAG, "setDataSaverMode: " + z);
            j = "setDataSaverMode: ";
        }
        synchronized (this.mQuotaLock) {
            if (this.mDataSaverMode == z) {
                Log.w(TAG, "setDataSaverMode(): already " + this.mDataSaverMode);
                return true;
            }
            try {
                Trace.traceBegin(2097152L, "bandwidthEnableDataSaver");
                try {
                    boolean zBandwidthEnableDataSaver = this.mNetdService.bandwidthEnableDataSaver(z);
                    if (zBandwidthEnableDataSaver) {
                        this.mDataSaverMode = z;
                    } else {
                        Log.w(TAG, "setDataSaverMode(" + z + "): netd command silently failed");
                    }
                    Trace.traceEnd(2097152L);
                    return zBandwidthEnableDataSaver;
                } catch (RemoteException e) {
                    Log.w(TAG, "setDataSaverMode(" + z + "): netd command failed", e);
                    Trace.traceEnd(2097152L);
                    return false;
                }
            } catch (Throwable th) {
                Trace.traceEnd(j);
                throw th;
            }
        }
    }

    public void setAllowOnlyVpnForUids(boolean z, UidRange[] uidRangeArr) throws ServiceSpecificException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.NETWORK_STACK", TAG);
        try {
            this.mNetdService.networkRejectNonSecureVpn(z, uidRangeArr);
        } catch (RemoteException e) {
            Log.w(TAG, "setAllowOnlyVpnForUids(" + z + ", " + Arrays.toString(uidRangeArr) + "): netd command failed", e);
            throw e.rethrowAsRuntimeException();
        } catch (ServiceSpecificException e2) {
            Log.w(TAG, "setAllowOnlyVpnForUids(" + z + ", " + Arrays.toString(uidRangeArr) + "): netd command failed", e2);
            throw e2;
        }
    }

    private void applyUidCleartextNetworkPolicy(int i, int i2) {
        String str;
        switch (i2) {
            case 0:
                str = "accept";
                break;
            case 1:
                str = "log";
                break;
            case 2:
                str = "reject";
                break;
            default:
                throw new IllegalArgumentException("Unknown policy " + i2);
        }
        try {
            this.mConnector.execute("strict", "set_uid_cleartext_policy", Integer.valueOf(i), str);
            this.mUidCleartextPolicy.put(i, i2);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void setUidCleartextNetworkPolicy(int i, int i2) {
        if (Binder.getCallingUid() != i) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        }
        synchronized (this.mQuotaLock) {
            int i3 = this.mUidCleartextPolicy.get(i, 0);
            if (i3 == i2) {
                return;
            }
            if (!this.mStrictEnabled) {
                this.mUidCleartextPolicy.put(i, i2);
                return;
            }
            if (i3 != 0 && i2 != 0) {
                applyUidCleartextNetworkPolicy(i, 0);
            }
            applyUidCleartextNetworkPolicy(i, i2);
        }
    }

    public boolean isBandwidthControlEnabled() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        return this.mBandwidthControlEnabled;
    }

    public NetworkStats getNetworkStatsUidDetail(int i, String[] strArr) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            return this.mStatsFactory.readNetworkStatsDetail(i, strArr, -1, (NetworkStats) null);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private class NetdTetheringStatsProvider extends ITetheringStatsProvider.Stub {
        private NetdTetheringStatsProvider() {
        }

        public NetworkStats getTetherStats(int i) {
            if (i == 1) {
                try {
                    PersistableBundle persistableBundleTetherGetStats = NetworkManagementService.this.mNetdService.tetherGetStats();
                    NetworkStats networkStats = new NetworkStats(SystemClock.elapsedRealtime(), persistableBundleTetherGetStats.size());
                    NetworkStats.Entry entry = new NetworkStats.Entry();
                    for (String str : persistableBundleTetherGetStats.keySet()) {
                        long[] longArray = persistableBundleTetherGetStats.getLongArray(str);
                        try {
                            entry.iface = str;
                            entry.uid = -5;
                            entry.set = 0;
                            entry.tag = 0;
                            entry.rxBytes = longArray[0];
                            entry.rxPackets = longArray[1];
                            entry.txBytes = longArray[2];
                            entry.txPackets = longArray[3];
                            networkStats.combineValues(entry);
                        } catch (ArrayIndexOutOfBoundsException e) {
                            throw new IllegalStateException("invalid tethering stats for " + str, e);
                        }
                    }
                    return networkStats;
                } catch (RemoteException | ServiceSpecificException e2) {
                    throw new IllegalStateException("problem parsing tethering stats: ", e2);
                }
            }
            return new NetworkStats(SystemClock.elapsedRealtime(), 0);
        }

        public void setInterfaceQuota(String str, long j) {
        }
    }

    public NetworkStats getNetworkStatsTethering(int i) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        NetworkStats networkStats = new NetworkStats(SystemClock.elapsedRealtime(), 1);
        synchronized (this.mTetheringStatsProviders) {
            for (ITetheringStatsProvider iTetheringStatsProvider : this.mTetheringStatsProviders.keySet()) {
                try {
                    networkStats.combineAllValues(iTetheringStatsProvider.getTetherStats(i));
                } catch (RemoteException e) {
                    Log.e(TAG, "Problem reading tethering stats from " + this.mTetheringStatsProviders.get(iTetheringStatsProvider) + ": " + e);
                }
            }
        }
        return networkStats;
    }

    public void setDnsConfigurationForNetwork(int i, String[] strArr, String[] strArr2, int[] iArr, String str, String[] strArr3) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mNetdService.setResolverConfiguration(i, strArr, strArr2, iArr, str, strArr3, new String[0]);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void addVpnUidRanges(int i, UidRange[] uidRangeArr) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        Object[] objArr = new Object[13];
        objArr[0] = DatabaseHelper.SoundModelContract.KEY_USERS;
        objArr[1] = "add";
        objArr[2] = Integer.valueOf(i);
        int i2 = 3;
        for (int i3 = 0; i3 < uidRangeArr.length; i3++) {
            int i4 = i2 + 1;
            objArr[i2] = uidRangeArr[i3].toString();
            if (i3 != uidRangeArr.length - 1 && i4 != objArr.length) {
                i2 = i4;
            } else {
                try {
                    this.mConnector.execute("network", Arrays.copyOf(objArr, i4));
                    i2 = 3;
                } catch (NativeDaemonConnectorException e) {
                    throw e.rethrowAsParcelableException();
                }
            }
        }
    }

    public void removeVpnUidRanges(int i, UidRange[] uidRangeArr) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        Object[] objArr = new Object[13];
        objArr[0] = DatabaseHelper.SoundModelContract.KEY_USERS;
        objArr[1] = "remove";
        objArr[2] = Integer.valueOf(i);
        int i2 = 3;
        for (int i3 = 0; i3 < uidRangeArr.length; i3++) {
            int i4 = i2 + 1;
            objArr[i2] = uidRangeArr[i3].toString();
            if (i3 != uidRangeArr.length - 1 && i4 != objArr.length) {
                i2 = i4;
            } else {
                try {
                    this.mConnector.execute("network", Arrays.copyOf(objArr, i4));
                    i2 = 3;
                } catch (NativeDaemonConnectorException e) {
                    throw e.rethrowAsParcelableException();
                }
            }
        }
    }

    public void setFirewallEnabled(boolean z) {
        enforceSystemUid();
        try {
            NativeDaemonConnector nativeDaemonConnector = this.mConnector;
            Object[] objArr = new Object[2];
            objArr[0] = "enable";
            objArr[1] = z ? "whitelist" : "blacklist";
            nativeDaemonConnector.execute("firewall", objArr);
            this.mFirewallEnabled = z;
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public boolean isFirewallEnabled() {
        enforceSystemUid();
        return this.mFirewallEnabled;
    }

    public void setFirewallInterfaceRule(String str, boolean z) {
        enforceSystemUid();
        Preconditions.checkState(this.mFirewallEnabled);
        try {
            this.mConnector.execute("firewall", "set_interface_rule", str, z ? "allow" : "deny");
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    private void closeSocketsForFirewallChainLocked(int i, String str) {
        UidRange[] uidRangeArr;
        int i2;
        int[] iArrCopyOf;
        int i3;
        if (getFirewallType(i) == 0) {
            uidRangeArr = new UidRange[]{new UidRange(10000, Integer.MAX_VALUE)};
            synchronized (this.mRulesLock) {
                SparseIntArray uidFirewallRulesLR = getUidFirewallRulesLR(i);
                iArrCopyOf = new int[uidFirewallRulesLR.size()];
                i3 = 0;
                for (int i4 = 0; i4 < iArrCopyOf.length; i4++) {
                    if (uidFirewallRulesLR.valueAt(i4) == 1) {
                        iArrCopyOf[i3] = uidFirewallRulesLR.keyAt(i4);
                        i3++;
                    }
                }
            }
            if (i3 != iArrCopyOf.length) {
                iArrCopyOf = Arrays.copyOf(iArrCopyOf, i3);
            }
        } else {
            synchronized (this.mRulesLock) {
                SparseIntArray uidFirewallRulesLR2 = getUidFirewallRulesLR(i);
                uidRangeArr = new UidRange[uidFirewallRulesLR2.size()];
                i2 = 0;
                for (int i5 = 0; i5 < uidRangeArr.length; i5++) {
                    if (uidFirewallRulesLR2.valueAt(i5) == 2) {
                        int iKeyAt = uidFirewallRulesLR2.keyAt(i5);
                        uidRangeArr[i2] = new UidRange(iKeyAt, iKeyAt);
                        i2++;
                    }
                }
            }
            if (i2 != uidRangeArr.length) {
                uidRangeArr = (UidRange[]) Arrays.copyOf(uidRangeArr, i2);
            }
            iArrCopyOf = new int[0];
        }
        try {
            this.mNetdService.socketDestroy(uidRangeArr, iArrCopyOf);
        } catch (RemoteException | ServiceSpecificException e) {
            Slog.e(TAG, "Error closing sockets after enabling chain " + str + ": " + e);
        }
    }

    public void setFirewallChainEnabled(int i, boolean z) {
        String str;
        enforceSystemUid();
        synchronized (this.mQuotaLock) {
            synchronized (this.mRulesLock) {
                if (getFirewallChainState(i) == z) {
                    return;
                }
                setFirewallChainState(i, z);
                String str2 = z ? "enable_chain" : "disable_chain";
                switch (i) {
                    case 1:
                        str = "dozable";
                        break;
                    case 2:
                        str = "standby";
                        break;
                    case 3:
                        str = "powersave";
                        break;
                    default:
                        throw new IllegalArgumentException("Bad child chain: " + i);
                }
                try {
                    this.mConnector.execute("firewall", str2, str);
                    if (z) {
                        if (DBG) {
                            Slog.d(TAG, "Closing sockets after enabling chain " + str);
                        }
                        closeSocketsForFirewallChainLocked(i, str);
                    }
                } catch (NativeDaemonConnectorException e) {
                    throw e.rethrowAsParcelableException();
                }
            }
        }
    }

    private int getFirewallType(int i) {
        switch (i) {
            case 1:
                return 0;
            case 2:
                return 1;
            case 3:
                return 0;
            default:
                return !isFirewallEnabled() ? 1 : 0;
        }
    }

    public void setFirewallUidRules(int i, int[] iArr, int[] iArr2) {
        enforceSystemUid();
        synchronized (this.mQuotaLock) {
            synchronized (this.mRulesLock) {
                SparseIntArray uidFirewallRulesLR = getUidFirewallRulesLR(i);
                SparseIntArray sparseIntArray = new SparseIntArray();
                for (int length = iArr.length - 1; length >= 0; length--) {
                    int i2 = iArr[length];
                    int i3 = iArr2[length];
                    updateFirewallUidRuleLocked(i, i2, i3);
                    sparseIntArray.put(i2, i3);
                }
                SparseIntArray sparseIntArray2 = new SparseIntArray();
                for (int size = uidFirewallRulesLR.size() - 1; size >= 0; size--) {
                    int iKeyAt = uidFirewallRulesLR.keyAt(size);
                    if (sparseIntArray.indexOfKey(iKeyAt) < 0) {
                        sparseIntArray2.put(iKeyAt, 0);
                    }
                }
                for (int size2 = sparseIntArray2.size() - 1; size2 >= 0; size2--) {
                    updateFirewallUidRuleLocked(i, sparseIntArray2.keyAt(size2), 0);
                }
            }
            try {
                switch (i) {
                    case 1:
                        this.mNetdService.firewallReplaceUidChain("fw_dozable", true, iArr);
                        break;
                    case 2:
                        this.mNetdService.firewallReplaceUidChain("fw_standby", false, iArr);
                        break;
                    case 3:
                        this.mNetdService.firewallReplaceUidChain("fw_powersave", true, iArr);
                        break;
                    default:
                        Slog.d(TAG, "setFirewallUidRules() called on invalid chain: " + i);
                        break;
                }
            } catch (RemoteException e) {
                Slog.w(TAG, "Error flushing firewall chain " + i, e);
            }
        }
    }

    public void setFirewallUidRule(int i, int i2, int i3) {
        enforceSystemUid();
        synchronized (this.mQuotaLock) {
            setFirewallUidRuleLocked(i, i2, i3);
        }
    }

    private void setFirewallUidRuleLocked(int i, int i2, int i3) {
        if (updateFirewallUidRuleLocked(i, i2, i3)) {
            try {
                this.mConnector.execute("firewall", "set_uid_rule", getFirewallChainName(i), Integer.valueOf(i2), getFirewallRuleName(i, i3));
            } catch (NativeDaemonConnectorException e) {
                throw e.rethrowAsParcelableException();
            }
        }
    }

    private boolean updateFirewallUidRuleLocked(int i, int i2, int i3) {
        synchronized (this.mRulesLock) {
            SparseIntArray uidFirewallRulesLR = getUidFirewallRulesLR(i);
            int i4 = uidFirewallRulesLR.get(i2, 0);
            if (DBG) {
                Slog.d(TAG, "oldRule = " + i4 + ", newRule=" + i3 + " for uid=" + i2 + " on chain " + i);
            }
            if (i4 == i3) {
                if (DBG) {
                    Slog.d(TAG, "!!!!! Skipping change");
                }
                return false;
            }
            String firewallRuleName = getFirewallRuleName(i, i3);
            String firewallRuleName2 = getFirewallRuleName(i, i4);
            if (i3 == 0) {
                uidFirewallRulesLR.delete(i2);
            } else {
                uidFirewallRulesLR.put(i2, i3);
            }
            return !firewallRuleName.equals(firewallRuleName2);
        }
    }

    private String getFirewallRuleName(int i, int i2) {
        if (getFirewallType(i) == 0) {
            if (i2 == 1) {
                return "allow";
            }
            return "deny";
        }
        if (i2 == 2) {
            return "deny";
        }
        return "allow";
    }

    private SparseIntArray getUidFirewallRulesLR(int i) {
        switch (i) {
            case 0:
                return this.mUidFirewallRules;
            case 1:
                return this.mUidFirewallDozableRules;
            case 2:
                return this.mUidFirewallStandbyRules;
            case 3:
                return this.mUidFirewallPowerSaveRules;
            default:
                throw new IllegalArgumentException("Unknown chain:" + i);
        }
    }

    public String getFirewallChainName(int i) {
        switch (i) {
            case 0:
                return "none";
            case 1:
                return "dozable";
            case 2:
                return "standby";
            case 3:
                return "powersave";
            default:
                throw new IllegalArgumentException("Unknown chain:" + i);
        }
    }

    private static void enforceSystemUid() {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Only available to AID_SYSTEM");
        }
    }

    public void startClatd(String str) throws IllegalStateException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("clatd", "start", str);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void stopClatd(String str) throws IllegalStateException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("clatd", "stop", str);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public boolean isClatdStarted(String str) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            NativeDaemonEvent nativeDaemonEventExecute = this.mConnector.execute("clatd", "status", str);
            nativeDaemonEventExecute.checkCode(NetdResponseCode.ClatdStatusResult);
            return nativeDaemonEventExecute.getMessage().endsWith("started");
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void registerNetworkActivityListener(INetworkActivityListener iNetworkActivityListener) {
        this.mNetworkActivityListeners.register(iNetworkActivityListener);
    }

    public void unregisterNetworkActivityListener(INetworkActivityListener iNetworkActivityListener) {
        this.mNetworkActivityListeners.unregister(iNetworkActivityListener);
    }

    public boolean isNetworkActive() {
        boolean z;
        synchronized (this.mNetworkActivityListeners) {
            z = this.mNetworkActive || this.mActiveIdleTimers.isEmpty();
        }
        return z;
    }

    private void reportNetworkActive() {
        int iBeginBroadcast = this.mNetworkActivityListeners.beginBroadcast();
        for (int i = 0; i < iBeginBroadcast; i++) {
            try {
                this.mNetworkActivityListeners.getBroadcastItem(i).onNetworkActive();
            } catch (RemoteException | RuntimeException e) {
            } catch (Throwable th) {
                this.mNetworkActivityListeners.finishBroadcast();
                throw th;
            }
        }
        this.mNetworkActivityListeners.finishBroadcast();
    }

    @Override
    public void monitor() {
        if (this.mConnector != null) {
            this.mConnector.monitor();
        }
    }

    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, printWriter)) {
            printWriter.println("NetworkManagementService NativeDaemonConnector Log:");
            this.mConnector.dump(fileDescriptor, printWriter, strArr);
            printWriter.println();
            printWriter.print("Bandwidth control enabled: ");
            printWriter.println(this.mBandwidthControlEnabled);
            printWriter.print("mMobileActivityFromRadio=");
            printWriter.print(this.mMobileActivityFromRadio);
            printWriter.print(" mLastPowerStateFromRadio=");
            printWriter.println(this.mLastPowerStateFromRadio);
            printWriter.print("mNetworkActive=");
            printWriter.println(this.mNetworkActive);
            synchronized (this.mQuotaLock) {
                printWriter.print("Active quota ifaces: ");
                printWriter.println(this.mActiveQuotas.toString());
                printWriter.print("Active alert ifaces: ");
                printWriter.println(this.mActiveAlerts.toString());
                printWriter.print("Data saver mode: ");
                printWriter.println(this.mDataSaverMode);
                synchronized (this.mRulesLock) {
                    dumpUidRuleOnQuotaLocked(printWriter, "blacklist", this.mUidRejectOnMetered);
                    dumpUidRuleOnQuotaLocked(printWriter, "whitelist", this.mUidAllowOnMetered);
                }
            }
            synchronized (this.mRulesLock) {
                dumpUidFirewallRule(printWriter, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, this.mUidFirewallRules);
                printWriter.print("UID firewall standby chain enabled: ");
                printWriter.println(getFirewallChainState(2));
                dumpUidFirewallRule(printWriter, "standby", this.mUidFirewallStandbyRules);
                printWriter.print("UID firewall dozable chain enabled: ");
                printWriter.println(getFirewallChainState(1));
                dumpUidFirewallRule(printWriter, "dozable", this.mUidFirewallDozableRules);
                printWriter.println("UID firewall powersave chain enabled: " + getFirewallChainState(3));
                dumpUidFirewallRule(printWriter, "powersave", this.mUidFirewallPowerSaveRules);
            }
            synchronized (this.mIdleTimerLock) {
                printWriter.println("Idle timers:");
                for (Map.Entry<String, IdleTimerParams> entry : this.mActiveIdleTimers.entrySet()) {
                    printWriter.print("  ");
                    printWriter.print(entry.getKey());
                    printWriter.println(":");
                    IdleTimerParams value = entry.getValue();
                    printWriter.print("    timeout=");
                    printWriter.print(value.timeout);
                    printWriter.print(" type=");
                    printWriter.print(value.type);
                    printWriter.print(" networkCount=");
                    printWriter.println(value.networkCount);
                }
            }
            printWriter.print("Firewall enabled: ");
            printWriter.println(this.mFirewallEnabled);
            printWriter.print("Netd service status: ");
            if (this.mNetdService == null) {
                printWriter.println("disconnected");
                return;
            }
            try {
                printWriter.println(this.mNetdService.isAlive() ? "alive" : "dead");
            } catch (RemoteException e) {
                printWriter.println("unreachable");
            }
        }
    }

    private void dumpUidRuleOnQuotaLocked(PrintWriter printWriter, String str, SparseBooleanArray sparseBooleanArray) {
        printWriter.print("UID bandwith control ");
        printWriter.print(str);
        printWriter.print(" rule: [");
        int size = sparseBooleanArray.size();
        for (int i = 0; i < size; i++) {
            printWriter.print(sparseBooleanArray.keyAt(i));
            if (i < size - 1) {
                printWriter.print(",");
            }
        }
        printWriter.println("]");
    }

    private void dumpUidFirewallRule(PrintWriter printWriter, String str, SparseIntArray sparseIntArray) {
        printWriter.print("UID firewall ");
        printWriter.print(str);
        printWriter.print(" rule: [");
        int size = sparseIntArray.size();
        for (int i = 0; i < size; i++) {
            printWriter.print(sparseIntArray.keyAt(i));
            printWriter.print(":");
            printWriter.print(sparseIntArray.valueAt(i));
            if (i < size - 1) {
                printWriter.print(",");
            }
        }
        printWriter.println("]");
    }

    public void createPhysicalNetwork(int i, String str) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            if (str == null) {
                this.mConnector.execute("network", "create", Integer.valueOf(i));
            } else {
                this.mConnector.execute("network", "create", Integer.valueOf(i), str);
            }
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void createVirtualNetwork(int i, boolean z, boolean z2) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            NativeDaemonConnector nativeDaemonConnector = this.mConnector;
            Object[] objArr = new Object[5];
            objArr[0] = "create";
            objArr[1] = Integer.valueOf(i);
            objArr[2] = "vpn";
            objArr[3] = z ? "1" : "0";
            objArr[4] = z2 ? "1" : "0";
            nativeDaemonConnector.execute("network", objArr);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void removeNetwork(int i) throws ServiceSpecificException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.NETWORK_STACK", TAG);
        try {
            this.mNetdService.networkDestroy(i);
        } catch (RemoteException e) {
            Log.w(TAG, "removeNetwork(" + i + "): ", e);
            throw e.rethrowAsRuntimeException();
        } catch (ServiceSpecificException e2) {
            Log.w(TAG, "removeNetwork(" + i + "): ", e2);
            throw e2;
        }
    }

    public void addInterfaceToNetwork(String str, int i) {
        modifyInterfaceInNetwork("add", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS + i, str);
    }

    public void removeInterfaceFromNetwork(String str, int i) {
        modifyInterfaceInNetwork("remove", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS + i, str);
    }

    private void modifyInterfaceInNetwork(String str, String str2, String str3) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("network", "interface", str, str2, str3);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void addLegacyRouteForNetId(int i, RouteInfo routeInfo, int i2) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        NativeDaemonConnector.Command command = new NativeDaemonConnector.Command("network", "route", "legacy", Integer.valueOf(i2), "add", Integer.valueOf(i));
        LinkAddress destinationLinkAddress = routeInfo.getDestinationLinkAddress();
        command.appendArg(routeInfo.getInterface());
        command.appendArg(destinationLinkAddress.getAddress().getHostAddress() + SliceClientPermissions.SliceAuthority.DELIMITER + destinationLinkAddress.getPrefixLength());
        if (routeInfo.hasGateway()) {
            command.appendArg(routeInfo.getGateway().getHostAddress());
        }
        try {
            this.mConnector.execute(command);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void setDefaultNetId(int i) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("network", BatteryService.HealthServiceWrapper.INSTANCE_VENDOR, "set", Integer.valueOf(i));
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void clearDefaultNetId() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("network", BatteryService.HealthServiceWrapper.INSTANCE_VENDOR, "clear");
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void setNetworkPermission(int i, String str) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            if (str == null) {
                this.mConnector.execute("network", "permission", "network", "clear", Integer.valueOf(i));
            } else {
                this.mConnector.execute("network", "permission", "network", "set", str, Integer.valueOf(i));
            }
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void setPermission(String str, int[] iArr) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        Object[] objArr = new Object[14];
        objArr[0] = "permission";
        objArr[1] = "user";
        objArr[2] = "set";
        objArr[3] = str;
        int i = 4;
        for (int i2 = 0; i2 < iArr.length; i2++) {
            int i3 = i + 1;
            objArr[i] = Integer.valueOf(iArr[i2]);
            if (i2 != iArr.length - 1 && i3 != objArr.length) {
                i = i3;
            } else {
                try {
                    this.mConnector.execute("network", Arrays.copyOf(objArr, i3));
                    i = 4;
                } catch (NativeDaemonConnectorException e) {
                    throw e.rethrowAsParcelableException();
                }
            }
        }
    }

    public void clearPermission(int[] iArr) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        Object[] objArr = new Object[13];
        objArr[0] = "permission";
        objArr[1] = "user";
        objArr[2] = "clear";
        int i = 3;
        for (int i2 = 0; i2 < iArr.length; i2++) {
            int i3 = i + 1;
            objArr[i] = Integer.valueOf(iArr[i2]);
            if (i2 != iArr.length - 1 && i3 != objArr.length) {
                i = i3;
            } else {
                try {
                    this.mConnector.execute("network", Arrays.copyOf(objArr, i3));
                    i = 3;
                } catch (NativeDaemonConnectorException e) {
                    throw e.rethrowAsParcelableException();
                }
            }
        }
    }

    public void allowProtect(int i) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("network", "protect", "allow", Integer.valueOf(i));
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void denyProtect(int i) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("network", "protect", "deny", Integer.valueOf(i));
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void addInterfaceToLocalNetwork(String str, List<RouteInfo> list) {
        modifyInterfaceInNetwork("add", "local", str);
        for (RouteInfo routeInfo : list) {
            if (!routeInfo.isDefaultRoute()) {
                modifyRoute("add", "local", routeInfo);
            }
        }
    }

    public void removeInterfaceFromLocalNetwork(String str) {
        modifyInterfaceInNetwork("remove", "local", str);
    }

    public int removeRoutesFromLocalNetwork(List<RouteInfo> list) {
        Iterator<RouteInfo> it = list.iterator();
        int i = 0;
        while (it.hasNext()) {
            try {
                modifyRoute("remove", "local", it.next());
            } catch (IllegalStateException e) {
                i++;
            }
        }
        return i;
    }

    public boolean isNetworkRestricted(int i) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        return isNetworkRestrictedInternal(i);
    }

    private boolean isNetworkRestrictedInternal(int i) {
        synchronized (this.mRulesLock) {
            if (getFirewallChainState(2) && this.mUidFirewallStandbyRules.get(i) == 2) {
                if (DBG) {
                    Slog.d(TAG, "Uid " + i + " restricted because of app standby mode");
                }
                return true;
            }
            if (getFirewallChainState(1) && this.mUidFirewallDozableRules.get(i) != 1) {
                if (DBG) {
                    Slog.d(TAG, "Uid " + i + " restricted because of device idle mode");
                }
                return true;
            }
            if (getFirewallChainState(3) && this.mUidFirewallPowerSaveRules.get(i) != 1) {
                if (DBG) {
                    Slog.d(TAG, "Uid " + i + " restricted because of power saver mode");
                }
                return true;
            }
            if (this.mUidRejectOnMetered.get(i)) {
                if (DBG) {
                    Slog.d(TAG, "Uid " + i + " restricted because of no metered data in the background");
                }
                return true;
            }
            if (this.mDataSaverMode && !this.mUidAllowOnMetered.get(i)) {
                if (DBG) {
                    Slog.d(TAG, "Uid " + i + " restricted because of data saver mode");
                }
                return true;
            }
            return false;
        }
    }

    private void setFirewallChainState(int i, boolean z) {
        synchronized (this.mRulesLock) {
            this.mFirewallChainStates.put(i, z);
        }
    }

    private boolean getFirewallChainState(int i) {
        boolean z;
        synchronized (this.mRulesLock) {
            z = this.mFirewallChainStates.get(i);
        }
        return z;
    }

    @VisibleForTesting
    class LocalService extends NetworkManagementInternal {
        LocalService() {
        }

        @Override
        public boolean isNetworkRestrictedForUid(int i) {
            return NetworkManagementService.this.isNetworkRestrictedInternal(i);
        }
    }

    @VisibleForTesting
    Injector getInjector() {
        return new Injector();
    }

    @VisibleForTesting
    class Injector {
        Injector() {
        }

        void setDataSaverMode(boolean z) {
            NetworkManagementService.this.mDataSaverMode = z;
        }

        void setFirewallChainState(int i, boolean z) {
            NetworkManagementService.this.setFirewallChainState(i, z);
        }

        void setFirewallRule(int i, int i2, int i3) {
            synchronized (NetworkManagementService.this.mRulesLock) {
                NetworkManagementService.this.getUidFirewallRulesLR(i).put(i2, i3);
            }
        }

        void setUidOnMeteredNetworkList(boolean z, int i, boolean z2) {
            synchronized (NetworkManagementService.this.mRulesLock) {
                try {
                    if (z) {
                        NetworkManagementService.this.mUidRejectOnMetered.put(i, z2);
                    } else {
                        NetworkManagementService.this.mUidAllowOnMetered.put(i, z2);
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
        }

        void reset() {
            synchronized (NetworkManagementService.this.mRulesLock) {
                setDataSaverMode(false);
                for (int i : new int[]{1, 2, 3}) {
                    setFirewallChainState(i, false);
                    NetworkManagementService.this.getUidFirewallRulesLR(i).clear();
                }
                NetworkManagementService.this.mUidAllowOnMetered.clear();
                NetworkManagementService.this.mUidRejectOnMetered.clear();
            }
        }
    }
}
