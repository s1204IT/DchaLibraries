package com.android.server;

import android.R;
import android.app.ActivityManagerNative;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.INetd;
import android.net.INetworkManagementEventObserver;
import android.net.InterfaceConfiguration;
import android.net.IpPrefix;
import android.net.LinkAddress;
import android.net.Network;
import android.net.NetworkStats;
import android.net.NetworkUtils;
import android.net.RouteInfo;
import android.net.UidRange;
import android.net.wifi.WifiConfiguration;
import android.os.Binder;
import android.os.Handler;
import android.os.INetworkActivityListener;
import android.os.INetworkManagementService;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ServiceSpecificException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.util.Slog;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.app.IBatteryStats;
import com.android.internal.net.NetworkStatsFactory;
import com.android.internal.util.HexDump;
import com.android.internal.util.Preconditions;
import com.android.server.NativeDaemonConnector;
import com.android.server.Watchdog;
import com.android.server.net.LockdownVpnTracker;
import com.android.server.voiceinteraction.DatabaseHelper;
import com.google.android.collect.Maps;
import com.mediatek.appworkingset.AWSDBHelper;
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
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.concurrent.CountDownLatch;

public class NetworkManagementService extends INetworkManagementService.Stub implements Watchdog.Monitor {
    static final int DAEMON_MSG_MOBILE_CONN_REAL_TIME_INFO = 1;
    private static final boolean DBG = true;
    public static final int DNS_RESOLVER_DEFAULT_MAX_SAMPLES = 64;
    public static final int DNS_RESOLVER_DEFAULT_MIN_SAMPLES = 8;
    public static final int DNS_RESOLVER_DEFAULT_SAMPLE_VALIDITY_SECONDS = 1800;
    public static final int DNS_RESOLVER_DEFAULT_SUCCESS_THRESHOLD_PERCENT = 25;
    public static final String LIMIT_GLOBAL_ALERT = "globalAlert";
    private static final int MAX_UID_RANGES_PER_COMMAND = 10;
    private static final String NETD_SERVICE_NAME = "netd";
    private static final String NETD_TAG = "NetdConnector";
    public static final String PERMISSION_NETWORK = "NETWORK";
    public static final String PERMISSION_SYSTEM = "SYSTEM";
    static final String SOFT_AP_COMMAND = "softap";
    static final String SOFT_AP_COMMAND_SUCCESS = "Ok";
    private static final String TAG = "NetworkManagement";
    private volatile boolean mBandwidthControlEnabled;
    private IBatteryStats mBatteryStats;
    private final NativeDaemonConnector mConnector;
    private final Context mContext;

    @GuardedBy("mQuotaLock")
    private boolean mDataSaverMode;
    private volatile boolean mFirewallEnabled;
    private INetd mNetdService;
    private boolean mNetworkActive;
    private volatile boolean mStrictEnabled;
    private final Thread mThread;
    private CountDownLatch mConnectedSignal = new CountDownLatch(1);
    private final RemoteCallbackList<INetworkManagementEventObserver> mObservers = new RemoteCallbackList<>();
    private final NetworkStatsFactory mStatsFactory = new NetworkStatsFactory();
    private Object mQuotaLock = new Object();

    @GuardedBy("mQuotaLock")
    private HashMap<String, Long> mActiveQuotas = Maps.newHashMap();

    @GuardedBy("mQuotaLock")
    private HashMap<String, Long> mActiveAlerts = Maps.newHashMap();

    @GuardedBy("mQuotaLock")
    private SparseBooleanArray mUidRejectOnMetered = new SparseBooleanArray();

    @GuardedBy("mQuotaLock")
    private SparseBooleanArray mUidAllowOnMetered = new SparseBooleanArray();

    @GuardedBy("mQuotaLock")
    private SparseIntArray mUidCleartextPolicy = new SparseIntArray();

    @GuardedBy("mQuotaLock")
    private SparseIntArray mUidFirewallRules = new SparseIntArray();

    @GuardedBy("mQuotaLock")
    private SparseIntArray mUidFirewallStandbyRules = new SparseIntArray();

    @GuardedBy("mQuotaLock")
    private SparseIntArray mUidFirewallDozableRules = new SparseIntArray();

    @GuardedBy("mQuotaLock")
    private SparseIntArray mUidFirewallPowerSaveRules = new SparseIntArray();

    @GuardedBy("mQuotaLock")
    final SparseBooleanArray mFirewallChainStates = new SparseBooleanArray();
    private Object mIdleTimerLock = new Object();
    private HashMap<String, IdleTimerParams> mActiveIdleTimers = Maps.newHashMap();
    private boolean mMobileActivityFromRadio = false;
    private int mLastPowerStateFromRadio = 1;
    private int mLastPowerStateFromWifi = 1;
    private final RemoteCallbackList<INetworkActivityListener> mNetworkActivityListeners = new RemoteCallbackList<>();
    private final Handler mFgHandler = new Handler(FgThread.get().getLooper());
    private final Handler mDaemonHandler = new Handler(FgThread.get().getLooper());

    class NetdResponseCode {
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
        public static final int InterfaceRxThrottleResult = 218;
        public static final int InterfaceTxCounterResult = 217;
        public static final int InterfaceTxThrottleResult = 219;
        public static final int IpFwdStatusResult = 211;
        public static final int NetInfoSipError = 251;
        public static final int NetInfoSipResult = 250;
        public static final int QuotaCounterResult = 220;
        public static final int RouteChange = 616;
        public static final int SoftapStatusResult = 214;
        public static final int StrictCleartext = 617;
        public static final int StrictSocketConn = 699;
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

        IdleTimerParams(int timeout, int type) {
            this.timeout = timeout;
            this.type = type;
        }
    }

    private NetworkManagementService(Context context, String socket) {
        this.mContext = context;
        this.mConnector = new NativeDaemonConnector(new NetdCallbackReceiver(this, null), socket, 10, NETD_TAG, 160, null, FgThread.get().getLooper());
        this.mThread = new Thread(this.mConnector, NETD_TAG);
        Watchdog.getInstance().addMonitor(this);
    }

    static NetworkManagementService create(Context context, String socket) throws InterruptedException {
        NetworkManagementService service = new NetworkManagementService(context, socket);
        CountDownLatch connectedSignal = service.mConnectedSignal;
        Slog.d(TAG, "Creating NetworkManagementService");
        service.mThread.start();
        Slog.d(TAG, "Awaiting socket connection");
        connectedSignal.await();
        Slog.d(TAG, "Connected");
        service.connectNativeNetdService();
        return service;
    }

    public static NetworkManagementService create(Context context) throws InterruptedException {
        return create(context, NETD_SERVICE_NAME);
    }

    public void systemReady() {
        long start = System.currentTimeMillis();
        prepareNativeDaemon();
        long delta = System.currentTimeMillis() - start;
        Slog.d(TAG, "Prepared in " + delta + "ms");
    }

    private IBatteryStats getBatteryStats() {
        synchronized (this) {
            if (this.mBatteryStats != null) {
                return this.mBatteryStats;
            }
            this.mBatteryStats = IBatteryStats.Stub.asInterface(ServiceManager.getService("batterystats"));
            return this.mBatteryStats;
        }
    }

    public void registerObserver(INetworkManagementEventObserver observer) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        this.mObservers.register(observer);
    }

    public void unregisterObserver(INetworkManagementEventObserver observer) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        this.mObservers.unregister(observer);
    }

    private void notifyInterfaceStatusChanged(String iface, boolean up) {
        int length = this.mObservers.beginBroadcast();
        for (int i = 0; i < length; i++) {
            try {
                this.mObservers.getBroadcastItem(i).interfaceStatusChanged(iface, up);
            } catch (RemoteException | RuntimeException e) {
            } catch (Throwable th) {
                this.mObservers.finishBroadcast();
                throw th;
            }
        }
        this.mObservers.finishBroadcast();
    }

    private void notifyInterfaceLinkStateChanged(String iface, boolean up) {
        int length = this.mObservers.beginBroadcast();
        for (int i = 0; i < length; i++) {
            try {
                this.mObservers.getBroadcastItem(i).interfaceLinkStateChanged(iface, up);
            } catch (RemoteException | RuntimeException e) {
            } catch (Throwable th) {
                this.mObservers.finishBroadcast();
                throw th;
            }
        }
        this.mObservers.finishBroadcast();
    }

    private void notifyInterfaceAdded(String iface) {
        int length = this.mObservers.beginBroadcast();
        for (int i = 0; i < length; i++) {
            try {
                this.mObservers.getBroadcastItem(i).interfaceAdded(iface);
            } catch (RemoteException | RuntimeException e) {
            } catch (Throwable th) {
                this.mObservers.finishBroadcast();
                throw th;
            }
        }
        this.mObservers.finishBroadcast();
    }

    private void notifyInterfaceRemoved(String iface) {
        Slog.d(TAG, "notifyInterfaceRemoved, iface=" + iface);
        this.mActiveAlerts.remove(iface);
        this.mActiveQuotas.remove(iface);
        int length = this.mObservers.beginBroadcast();
        for (int i = 0; i < length; i++) {
            try {
                this.mObservers.getBroadcastItem(i).interfaceRemoved(iface);
            } catch (RemoteException | RuntimeException e) {
            } catch (Throwable th) {
                this.mObservers.finishBroadcast();
                throw th;
            }
        }
        this.mObservers.finishBroadcast();
    }

    private void notifyLimitReached(String limitName, String iface) {
        int length = this.mObservers.beginBroadcast();
        for (int i = 0; i < length; i++) {
            try {
                this.mObservers.getBroadcastItem(i).limitReached(limitName, iface);
            } catch (RemoteException | RuntimeException e) {
            } catch (Throwable th) {
                this.mObservers.finishBroadcast();
                throw th;
            }
        }
        this.mObservers.finishBroadcast();
    }

    private void notifyInterfaceClassActivity(int type, int powerState, long tsNanos, int uid, boolean fromRadio) {
        boolean isMobile = ConnectivityManager.isNetworkTypeMobile(type);
        if (isMobile) {
            if (!fromRadio) {
                if (this.mMobileActivityFromRadio) {
                    powerState = this.mLastPowerStateFromRadio;
                }
            } else {
                this.mMobileActivityFromRadio = true;
            }
            if (this.mLastPowerStateFromRadio != powerState) {
                this.mLastPowerStateFromRadio = powerState;
                try {
                    getBatteryStats().noteMobileRadioPowerState(powerState, tsNanos, uid);
                } catch (RemoteException e) {
                }
            }
        }
        if (ConnectivityManager.isNetworkTypeWifi(type) && this.mLastPowerStateFromWifi != powerState) {
            this.mLastPowerStateFromWifi = powerState;
            try {
                getBatteryStats().noteWifiRadioPowerState(powerState, tsNanos);
            } catch (RemoteException e2) {
            }
        }
        boolean isActive = powerState == 2 || powerState == 3;
        if (!isMobile || fromRadio || !this.mMobileActivityFromRadio) {
            int length = this.mObservers.beginBroadcast();
            for (int i = 0; i < length; i++) {
                try {
                    this.mObservers.getBroadcastItem(i).interfaceClassDataActivityChanged(Integer.toString(type), isActive, tsNanos);
                } catch (RemoteException | RuntimeException e3) {
                } catch (Throwable th) {
                    this.mObservers.finishBroadcast();
                    throw th;
                }
            }
            this.mObservers.finishBroadcast();
        }
        boolean report = false;
        synchronized (this.mIdleTimerLock) {
            if (this.mActiveIdleTimers.isEmpty()) {
                isActive = true;
            }
            if (this.mNetworkActive != isActive) {
                this.mNetworkActive = isActive;
                report = isActive;
            }
        }
        if (!report) {
            return;
        }
        reportNetworkActive();
    }

    private void syncFirewallChainLocked(int chain, SparseIntArray uidFirewallRules, String name) {
        int size = uidFirewallRules.size();
        if (size <= 0) {
            return;
        }
        SparseIntArray rules = uidFirewallRules.clone();
        uidFirewallRules.clear();
        Slog.d(TAG, "Pushing " + size + " active firewall " + name + "UID rules");
        for (int i = 0; i < rules.size(); i++) {
            setFirewallUidRuleLocked(chain, rules.keyAt(i), rules.valueAt(i));
        }
    }

    private void connectNativeNetdService() {
        boolean nativeServiceAvailable = false;
        try {
            this.mNetdService = INetd.Stub.asInterface(ServiceManager.getService(NETD_SERVICE_NAME));
            nativeServiceAvailable = this.mNetdService.isAlive();
        } catch (RemoteException e) {
        }
        if (nativeServiceAvailable) {
            return;
        }
        Slog.wtf(TAG, "Can't connect to NativeNetdService netd");
    }

    private void prepareNativeDaemon() {
        this.mConnector.setDebug(true);
        this.mBandwidthControlEnabled = false;
        boolean hasKernelSupport = new File("/proc/net/xt_qtaguid/ctrl").exists();
        if (hasKernelSupport) {
            Slog.d(TAG, "enabling bandwidth control");
            try {
                this.mConnector.execute("bandwidth", "enable");
                this.mBandwidthControlEnabled = true;
            } catch (NativeDaemonConnectorException e) {
                Slog.e(TAG, "problem enabling bandwidth controls");
            }
        } else {
            Slog.i(TAG, "not enabling bandwidth control");
        }
        SystemProperties.set("net.qtaguid_enabled", this.mBandwidthControlEnabled ? "1" : "0");
        if (this.mBandwidthControlEnabled) {
            try {
                getBatteryStats().noteNetworkStatsEnabled();
            } catch (RemoteException e2) {
            }
        }
        try {
            this.mConnector.execute("strict", "enable");
            this.mStrictEnabled = true;
        } catch (NativeDaemonConnectorException e3) {
            Log.wtf(TAG, "Failed strict enable", e3);
        }
        synchronized (this.mQuotaLock) {
            setDataSaverModeEnabled(this.mDataSaverMode);
            int size = this.mActiveQuotas.size();
            if (size > 0) {
                Slog.d(TAG, "Pushing " + size + " active quota rules");
                HashMap<String, Long> activeQuotas = this.mActiveQuotas;
                this.mActiveQuotas = Maps.newHashMap();
                for (Map.Entry<String, Long> entry : activeQuotas.entrySet()) {
                    setInterfaceQuota(entry.getKey(), entry.getValue().longValue());
                }
            }
            int size2 = this.mActiveAlerts.size();
            if (size2 > 0) {
                Slog.d(TAG, "Pushing " + size2 + " active alert rules");
                HashMap<String, Long> activeAlerts = this.mActiveAlerts;
                this.mActiveAlerts = Maps.newHashMap();
                for (Map.Entry<String, Long> entry2 : activeAlerts.entrySet()) {
                    setInterfaceAlert(entry2.getKey(), entry2.getValue().longValue());
                }
            }
            int size3 = this.mUidRejectOnMetered.size();
            if (size3 > 0) {
                Slog.d(TAG, "Pushing " + size3 + " UIDs to metered whitelist rules");
                SparseBooleanArray uidRejectOnQuota = this.mUidRejectOnMetered;
                this.mUidRejectOnMetered = new SparseBooleanArray();
                for (int i = 0; i < uidRejectOnQuota.size(); i++) {
                    setUidMeteredNetworkBlacklist(uidRejectOnQuota.keyAt(i), uidRejectOnQuota.valueAt(i));
                }
            }
            int size4 = this.mUidAllowOnMetered.size();
            if (size4 > 0) {
                Slog.d(TAG, "Pushing " + size4 + " UIDs to metered blacklist rules");
                SparseBooleanArray uidAcceptOnQuota = this.mUidAllowOnMetered;
                this.mUidAllowOnMetered = new SparseBooleanArray();
                for (int i2 = 0; i2 < uidAcceptOnQuota.size(); i2++) {
                    setUidMeteredNetworkWhitelist(uidAcceptOnQuota.keyAt(i2), uidAcceptOnQuota.valueAt(i2));
                }
            }
            int size5 = this.mUidCleartextPolicy.size();
            if (size5 > 0) {
                Slog.d(TAG, "Pushing " + size5 + " active UID cleartext policies");
                SparseIntArray local = this.mUidCleartextPolicy;
                this.mUidCleartextPolicy = new SparseIntArray();
                for (int i3 = 0; i3 < local.size(); i3++) {
                    setUidCleartextNetworkPolicy(local.keyAt(i3), local.valueAt(i3));
                }
            }
            setFirewallEnabled(!this.mFirewallEnabled ? LockdownVpnTracker.isEnabled() : true);
            syncFirewallChainLocked(0, this.mUidFirewallRules, "");
            syncFirewallChainLocked(2, this.mUidFirewallStandbyRules, "standby ");
            syncFirewallChainLocked(1, this.mUidFirewallDozableRules, "dozable ");
            syncFirewallChainLocked(3, this.mUidFirewallPowerSaveRules, "powersave ");
            if (this.mFirewallChainStates.get(2)) {
                setFirewallChainEnabled(2, true);
            }
            if (this.mFirewallChainStates.get(1)) {
                setFirewallChainEnabled(1, true);
            }
            if (this.mFirewallChainStates.get(3)) {
                setFirewallChainEnabled(3, true);
            }
        }
    }

    private void notifyAddressUpdated(String iface, LinkAddress address) {
        int length = this.mObservers.beginBroadcast();
        for (int i = 0; i < length; i++) {
            try {
                this.mObservers.getBroadcastItem(i).addressUpdated(iface, address);
            } catch (RemoteException | RuntimeException e) {
            } catch (Throwable th) {
                this.mObservers.finishBroadcast();
                throw th;
            }
        }
        this.mObservers.finishBroadcast();
    }

    private void notifyAddressRemoved(String iface, LinkAddress address) {
        int length = this.mObservers.beginBroadcast();
        for (int i = 0; i < length; i++) {
            try {
                this.mObservers.getBroadcastItem(i).addressRemoved(iface, address);
            } catch (RemoteException | RuntimeException e) {
            } catch (Throwable th) {
                this.mObservers.finishBroadcast();
                throw th;
            }
        }
        this.mObservers.finishBroadcast();
    }

    private void notifyInterfaceDnsServerInfo(String iface, long lifetime, String[] addresses) {
        int length = this.mObservers.beginBroadcast();
        for (int i = 0; i < length; i++) {
            try {
                this.mObservers.getBroadcastItem(i).interfaceDnsServerInfo(iface, lifetime, addresses);
            } catch (RemoteException | RuntimeException e) {
            } catch (Throwable th) {
                this.mObservers.finishBroadcast();
                throw th;
            }
        }
        this.mObservers.finishBroadcast();
    }

    private void notifyRouteChange(String action, RouteInfo route) {
        int length = this.mObservers.beginBroadcast();
        for (int i = 0; i < length; i++) {
            try {
                if (action.equals("updated")) {
                    this.mObservers.getBroadcastItem(i).routeUpdated(route);
                } else {
                    this.mObservers.getBroadcastItem(i).routeRemoved(route);
                }
            } catch (RemoteException | RuntimeException e) {
            } catch (Throwable th) {
                this.mObservers.finishBroadcast();
                throw th;
            }
        }
        this.mObservers.finishBroadcast();
    }

    private class NetdCallbackReceiver implements INativeDaemonConnectorCallbacks {
        NetdCallbackReceiver(NetworkManagementService this$0, NetdCallbackReceiver netdCallbackReceiver) {
            this();
        }

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
        public boolean onCheckHoldWakeLock(int code) {
            return code == 613;
        }

        @Override
        public boolean onEvent(int code, String raw, String[] cooked) {
            LinkAddress address;
            Slog.d(NetworkManagementService.TAG, "onEvent:" + raw + ":" + cooked.length);
            String errorMessage = String.format("Invalid event from daemon (%s)", raw);
            switch (code) {
                case 600:
                    if (cooked.length < 4 || !cooked[1].equals("Iface")) {
                        throw new IllegalStateException(errorMessage);
                    }
                    if (cooked[2].equals("added")) {
                        NetworkManagementService.this.notifyInterfaceAdded(cooked[3]);
                        return true;
                    }
                    if (cooked[2].equals("removed")) {
                        NetworkManagementService.this.notifyInterfaceRemoved(cooked[3]);
                        return true;
                    }
                    if (cooked[2].equals("changed") && cooked.length == 5) {
                        NetworkManagementService.this.notifyInterfaceStatusChanged(cooked[3], cooked[4].equals("up"));
                        return true;
                    }
                    if (cooked[2].equals("linkstate") && cooked.length == 5) {
                        NetworkManagementService.this.notifyInterfaceLinkStateChanged(cooked[3], cooked[4].equals("up"));
                        return true;
                    }
                    throw new IllegalStateException(errorMessage);
                case NetdResponseCode.BandwidthControl:
                    if (cooked.length < 5 || !cooked[1].equals("limit")) {
                        throw new IllegalStateException(errorMessage);
                    }
                    if (cooked[2].equals("alert")) {
                        NetworkManagementService.this.notifyLimitReached(cooked[3], cooked[4]);
                        return true;
                    }
                    throw new IllegalStateException(errorMessage);
                case NetdResponseCode.InterfaceClassActivity:
                    if (cooked.length < 4 || !cooked[1].equals("IfaceClass")) {
                        throw new IllegalStateException(errorMessage);
                    }
                    long timestampNanos = 0;
                    int processUid = -1;
                    if (cooked.length >= 5) {
                        try {
                            timestampNanos = Long.parseLong(cooked[4]);
                            if (cooked.length == 6) {
                                processUid = Integer.parseInt(cooked[5]);
                            }
                            break;
                        } catch (NumberFormatException e) {
                        }
                    } else {
                        timestampNanos = SystemClock.elapsedRealtimeNanos();
                    }
                    boolean isActive = cooked[2].equals("active");
                    NetworkManagementService.this.notifyInterfaceClassActivity(Integer.parseInt(cooked[3]), isActive ? 3 : 1, timestampNanos, processUid, true);
                    return true;
                case NetdResponseCode.InterfaceAddressChange:
                    if (cooked.length < 7 || !cooked[1].equals("Address")) {
                        throw new IllegalStateException(errorMessage);
                    }
                    String iface = cooked[4];
                    try {
                        int flags = Integer.parseInt(cooked[5]);
                        int scope = Integer.parseInt(cooked[6]);
                        if (cooked.length > 7) {
                            long valid = Long.parseLong(cooked[7]);
                            Slog.d(NetworkManagementService.TAG, "InterfaceAddressChange valid=" + valid);
                            address = new LinkAddress(cooked[3], flags, scope, valid);
                        } else {
                            Slog.d(NetworkManagementService.TAG, "InterfaceAddressChange no valid field");
                            address = new LinkAddress(cooked[3], flags, scope);
                        }
                        if (cooked[2].equals("updated")) {
                            NetworkManagementService.this.notifyAddressUpdated(iface, address);
                            return true;
                        }
                        NetworkManagementService.this.notifyAddressRemoved(iface, address);
                        return true;
                    } catch (NumberFormatException e2) {
                        Slog.d(NetworkManagementService.TAG, "NumberFormatException");
                        throw new IllegalStateException(errorMessage, e2);
                    } catch (IllegalArgumentException e3) {
                        Slog.d(NetworkManagementService.TAG, "IllegalArgumentException");
                        throw new IllegalStateException(errorMessage, e3);
                    }
                case NetdResponseCode.InterfaceDnsServerInfo:
                    if (cooked.length == 6 && cooked[1].equals("DnsInfo") && cooked[2].equals("servers")) {
                        try {
                            long lifetime = Long.parseLong(cooked[4]);
                            String[] servers = cooked[5].split(",");
                            NetworkManagementService.this.notifyInterfaceDnsServerInfo(cooked[3], lifetime, servers);
                            return true;
                        } catch (NumberFormatException e4) {
                            throw new IllegalStateException(errorMessage);
                        }
                    }
                    return true;
                case NetdResponseCode.RouteChange:
                    if (!cooked[1].equals("Route") || cooked.length < 6) {
                        throw new IllegalStateException(errorMessage);
                    }
                    String via = null;
                    String dev = null;
                    boolean valid2 = true;
                    for (int i = 4; i + 1 < cooked.length && valid2; i += 2) {
                        if (cooked[i].equals("dev")) {
                            if (dev == null) {
                                dev = cooked[i + 1];
                            } else {
                                valid2 = false;
                            }
                        } else if (cooked[i].equals("via")) {
                            if (via == null) {
                                via = cooked[i + 1];
                            } else {
                                valid2 = false;
                            }
                        } else {
                            valid2 = false;
                        }
                    }
                    if (valid2) {
                        InetAddress gateway = null;
                        if (via != null) {
                            try {
                                gateway = InetAddress.parseNumericAddress(via);
                            } catch (IllegalArgumentException e5) {
                            }
                        }
                        RouteInfo route = new RouteInfo(new IpPrefix(cooked[3]), gateway, dev);
                        NetworkManagementService.this.notifyRouteChange(cooked[2], route);
                        return true;
                    }
                    throw new IllegalStateException(errorMessage);
                case NetdResponseCode.StrictCleartext:
                    int uid = Integer.parseInt(cooked[1]);
                    byte[] firstPacket = HexDump.hexStringToByteArray(cooked[2]);
                    try {
                        ActivityManagerNative.getDefault().notifyCleartextNetwork(uid, firstPacket);
                        return false;
                    } catch (RemoteException e6) {
                        return false;
                    }
                case NetdResponseCode.StrictSocketConn:
                    int uidApp = Integer.parseInt(cooked[1]);
                    Intent intent = new Intent("com.mediatek.network.socketconn");
                    intent.putExtra(AWSDBHelper.PackageProcessList.KEY_UID, uidApp);
                    NetworkManagementService.this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
                    return false;
                default:
                    return false;
            }
        }
    }

    public String[] listInterfaces() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            return NativeDaemonEvent.filterMessageList(this.mConnector.executeForList("interface", "list"), 110);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public InterfaceConfiguration getInterfaceConfig(String iface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            NativeDaemonEvent event = this.mConnector.execute("interface", "getcfg", iface);
            event.checkCode(NetdResponseCode.InterfaceGetCfgResult);
            StringTokenizer st = new StringTokenizer(event.getMessage());
            try {
                InterfaceConfiguration cfg = new InterfaceConfiguration();
                cfg.setHardwareAddress(st.nextToken(" "));
                InetAddress addr = null;
                int prefixLength = 0;
                try {
                    addr = NetworkUtils.numericToInetAddress(st.nextToken());
                } catch (IllegalArgumentException iae) {
                    Slog.e(TAG, "Failed to parse ipaddr", iae);
                }
                try {
                    prefixLength = Integer.parseInt(st.nextToken());
                } catch (NumberFormatException nfe) {
                    Slog.e(TAG, "Failed to parse prefixLength", nfe);
                }
                cfg.setLinkAddress(new LinkAddress(addr, prefixLength));
                while (st.hasMoreTokens()) {
                    cfg.setFlag(st.nextToken());
                }
                return cfg;
            } catch (NoSuchElementException e) {
                throw new IllegalStateException("Invalid response from daemon: " + event);
            }
        } catch (NativeDaemonConnectorException e2) {
            throw e2.rethrowAsParcelableException();
        }
    }

    public void setInterfaceConfig(String iface, InterfaceConfiguration cfg) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        Slog.d(TAG, "Enter setInterfaceConfig, iface=" + iface);
        LinkAddress linkAddr = cfg.getLinkAddress();
        if (linkAddr == null || linkAddr.getAddress() == null) {
            throw new IllegalStateException("Null LinkAddress given");
        }
        NativeDaemonConnector.Command cmd = new NativeDaemonConnector.Command("interface", "setcfg", iface, linkAddr.getAddress().getHostAddress(), Integer.valueOf(linkAddr.getPrefixLength()));
        for (String flag : cfg.getFlags()) {
            cmd.appendArg(flag);
        }
        try {
            this.mConnector.execute(cmd);
        } catch (NativeDaemonConnectorException e) {
            Slog.e(TAG, "setInterfaceConfig Error");
        }
    }

    public void setInterfaceDown(String iface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        InterfaceConfiguration ifcg = getInterfaceConfig(iface);
        ifcg.setInterfaceDown();
        setInterfaceConfig(iface, ifcg);
    }

    public void setInterfaceUp(String iface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        InterfaceConfiguration ifcg = getInterfaceConfig(iface);
        ifcg.setInterfaceUp();
        setInterfaceConfig(iface, ifcg);
    }

    public void setInterfaceIpv6PrivacyExtensions(String iface, boolean enable) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            NativeDaemonConnector nativeDaemonConnector = this.mConnector;
            Object[] objArr = new Object[3];
            objArr[0] = "ipv6privacyextensions";
            objArr[1] = iface;
            objArr[2] = enable ? "enable" : "disable";
            nativeDaemonConnector.execute("interface", objArr);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void clearInterfaceAddresses(String iface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("interface", "clearaddrs", iface);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void enableIpv6(String iface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("interface", "ipv6", iface, "enable");
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void disableIpv6(String iface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("interface", "ipv6", iface, "disable");
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void setInterfaceIpv6NdOffload(String iface, boolean enable) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            NativeDaemonConnector nativeDaemonConnector = this.mConnector;
            Object[] objArr = new Object[3];
            objArr[0] = "ipv6ndoffload";
            objArr[1] = iface;
            objArr[2] = enable ? "enable" : "disable";
            nativeDaemonConnector.execute("interface", objArr);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void addRoute(int netId, RouteInfo route) {
        modifyRoute("add", "" + netId, route);
    }

    public void removeRoute(int netId, RouteInfo route) {
        modifyRoute("remove", "" + netId, route);
    }

    private void modifyRoute(String action, String netId, RouteInfo route) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        NativeDaemonConnector.Command cmd = new NativeDaemonConnector.Command("network", "route", action, netId);
        cmd.appendArg(route.getInterface());
        cmd.appendArg(route.getDestination().toString());
        switch (route.getType()) {
            case 1:
                if (route.hasGateway()) {
                    cmd.appendArg(route.getGateway().getHostAddress());
                }
                break;
            case 7:
                cmd.appendArg("unreachable");
                break;
            case 9:
                cmd.appendArg("throw");
                break;
        }
        try {
            this.mConnector.execute(cmd);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    private ArrayList<String> readRouteList(String filename) throws Throwable {
        FileInputStream fstream;
        FileInputStream fstream2 = null;
        ArrayList<String> list = new ArrayList<>();
        try {
            fstream = new FileInputStream(filename);
        } catch (IOException e) {
        } catch (Throwable th) {
            th = th;
        }
        try {
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            while (true) {
                String s = br.readLine();
                if (s == null || s.length() == 0) {
                    break;
                }
                list.add(s);
            }
            if (fstream != null) {
                try {
                    fstream.close();
                } catch (IOException e2) {
                }
            }
        } catch (IOException e3) {
            fstream2 = fstream;
            if (fstream2 != null) {
                try {
                    fstream2.close();
                } catch (IOException e4) {
                }
            }
        } catch (Throwable th2) {
            th = th2;
            fstream2 = fstream;
            if (fstream2 != null) {
                try {
                    fstream2.close();
                } catch (IOException e5) {
                }
            }
            throw th;
        }
        return list;
    }

    public void setMtu(String iface, int mtu) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("interface", "setmtu", iface, Integer.valueOf(mtu));
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
            NativeDaemonEvent event = this.mConnector.execute("ipfwd", "status");
            event.checkCode(211);
            return event.getMessage().endsWith("enabled");
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void setIpForwardingEnabled(boolean enable) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            NativeDaemonConnector nativeDaemonConnector = this.mConnector;
            Object[] objArr = new Object[2];
            objArr[0] = enable ? "enable" : "disable";
            objArr[1] = "tethering";
            nativeDaemonConnector.execute("ipfwd", objArr);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void startTethering(String[] dhcpRange) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        NativeDaemonConnector.Command cmd = new NativeDaemonConnector.Command("tether", "start");
        for (String d : dhcpRange) {
            cmd.appendArg(d);
        }
        try {
            this.mConnector.execute(cmd);
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
            NativeDaemonEvent event = this.mConnector.execute("tether", "status");
            event.checkCode(210);
            return event.getMessage().endsWith("started");
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void tetherInterface(String iface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("tether", "interface", "add", iface);
            List<RouteInfo> routes = new ArrayList<>();
            routes.add(new RouteInfo(getInterfaceConfig(iface).getLinkAddress(), null, iface));
            addInterfaceToLocalNetwork(iface, routes);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void untetherInterface(String iface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("tether", "interface", "remove", iface);
            removeInterfaceFromLocalNetwork(iface);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public String[] listTetheredInterfaces() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            return NativeDaemonEvent.filterMessageList(this.mConnector.executeForList("tether", "interface", "list"), 111);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void setDnsForwarders(Network network, String[] dns) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        int netId = network != null ? network.netId : 0;
        NativeDaemonConnector.Command cmd = new NativeDaemonConnector.Command("tether", "dns", "set", Integer.valueOf(netId));
        for (String s : dns) {
            cmd.appendArg(NetworkUtils.numericToInetAddress(s).getHostAddress());
        }
        try {
            this.mConnector.execute(cmd);
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

    private List<InterfaceAddress> excludeLinkLocal(List<InterfaceAddress> addresses) {
        ArrayList<InterfaceAddress> filtered = new ArrayList<>(addresses.size());
        for (InterfaceAddress ia : addresses) {
            if (!ia.getAddress().isLinkLocalAddress()) {
                filtered.add(ia);
            }
        }
        return filtered;
    }

    private void modifyInterfaceForward(boolean add, String fromIface, String toIface) {
        Object[] objArr = new Object[3];
        objArr[0] = add ? "add" : "remove";
        objArr[1] = fromIface;
        objArr[2] = toIface;
        NativeDaemonConnector.Command cmd = new NativeDaemonConnector.Command("ipfwd", objArr);
        try {
            this.mConnector.execute(cmd);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void startInterfaceForwarding(String fromIface, String toIface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        modifyInterfaceForward(true, fromIface, toIface);
    }

    public void stopInterfaceForwarding(String fromIface, String toIface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        modifyInterfaceForward(false, fromIface, toIface);
    }

    private void modifyNat(String action, String internalInterface, String externalInterface) throws SocketException {
        NativeDaemonConnector.Command cmd = new NativeDaemonConnector.Command("nat", action, internalInterface, externalInterface, 0);
        try {
            this.mConnector.execute(cmd);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void enableNat(String internalInterface, String externalInterface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            modifyNat("enable", internalInterface, externalInterface);
        } catch (SocketException e) {
            throw new IllegalStateException(e);
        }
    }

    public void disableNat(String internalInterface, String externalInterface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            modifyNat("disable", internalInterface, externalInterface);
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

    public void attachPppd(String tty, String localAddr, String remoteAddr, String dns1Addr, String dns2Addr) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("pppd", "attach", tty, NetworkUtils.numericToInetAddress(localAddr).getHostAddress(), NetworkUtils.numericToInetAddress(remoteAddr).getHostAddress(), NetworkUtils.numericToInetAddress(dns1Addr).getHostAddress(), NetworkUtils.numericToInetAddress(dns2Addr).getHostAddress());
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void detachPppd(String tty) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("pppd", "detach", tty);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    private void executeOrLogWithMessage(String command, Object[] args, int expectedResponseCode, String expectedResponseMessage, String logMsg) throws NativeDaemonConnectorException {
        NativeDaemonEvent event = this.mConnector.execute(command, args);
        if (event.getCode() == expectedResponseCode && event.getMessage().equals(expectedResponseMessage)) {
            return;
        }
        Log.e(TAG, logMsg + ": event = " + event);
    }

    public void startAccessPoint(WifiConfiguration wifiConfig, String wlanIface) {
        Object[] args;
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            if (wifiConfig == null) {
                args = new Object[]{"set", wlanIface};
            } else {
                args = new Object[]{"set", wlanIface, wifiConfig.SSID, "broadcast", Integer.toString(wifiConfig.apChannel), getSecurityType(wifiConfig), new NativeDaemonConnector.SensitiveArg(wifiConfig.preSharedKey)};
            }
            executeOrLogWithMessage(SOFT_AP_COMMAND, args, NetdResponseCode.SoftapStatusResult, SOFT_AP_COMMAND_SUCCESS, "startAccessPoint Error setting up softap");
            Object[] args2 = {"startap"};
            executeOrLogWithMessage(SOFT_AP_COMMAND, args2, NetdResponseCode.SoftapStatusResult, SOFT_AP_COMMAND_SUCCESS, "startAccessPoint Error starting softap");
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    private static String getSecurityType(WifiConfiguration wifiConfig) {
        switch (wifiConfig.getAuthType()) {
            case 1:
                return "wpa-psk";
            case 2:
            case 3:
            default:
                return "open";
            case 4:
                return "wpa2-psk";
        }
    }

    public void wifiFirmwareReload(String wlanIface, String mode) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        Object[] args = {"fwreload", wlanIface, mode};
        String logMsg = "wifiFirmwareReload Error reloading " + wlanIface + " fw in " + mode + " mode";
        try {
            executeOrLogWithMessage(SOFT_AP_COMMAND, args, NetdResponseCode.SoftapStatusResult, SOFT_AP_COMMAND_SUCCESS, logMsg);
            this.mConnector.waitForCallbacks();
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void stopAccessPoint(String wlanIface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        Object[] args = {"stopap"};
        try {
            executeOrLogWithMessage(SOFT_AP_COMMAND, args, NetdResponseCode.SoftapStatusResult, SOFT_AP_COMMAND_SUCCESS, "stopAccessPoint Error stopping softap");
            wifiFirmwareReload(wlanIface, "STA");
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void setAccessPoint(WifiConfiguration wifiConfig, String wlanIface) {
        Object[] args;
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            if (wifiConfig == null) {
                args = new Object[]{"set", wlanIface};
            } else {
                int clientNum = Settings.System.getInt(this.mContext.getContentResolver(), "wifi_hotspot_max_client_num", 6);
                String hiddenSSid = wifiConfig.hiddenSSID ? "hidden" : "broadcast";
                args = new Object[]{"set", wlanIface, wifiConfig.SSID, hiddenSSid, Integer.valueOf(wifiConfig.channel), getSecurityType(wifiConfig), new NativeDaemonConnector.SensitiveArg(wifiConfig.preSharedKey), Integer.valueOf(wifiConfig.channelWidth), Integer.valueOf(clientNum)};
            }
            executeOrLogWithMessage(SOFT_AP_COMMAND, args, NetdResponseCode.SoftapStatusResult, SOFT_AP_COMMAND_SUCCESS, "startAccessPoint Error setting up softap");
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void addIdleTimer(String iface, int timeout, final int type) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        Slog.d(TAG, "Adding idletimer");
        synchronized (this.mIdleTimerLock) {
            IdleTimerParams params = this.mActiveIdleTimers.get(iface);
            if (params != null) {
                params.networkCount++;
                return;
            }
            try {
                this.mConnector.execute("idletimer", "add", iface, Integer.toString(timeout), Integer.toString(type));
                this.mActiveIdleTimers.put(iface, new IdleTimerParams(timeout, type));
                if (ConnectivityManager.isNetworkTypeMobile(type)) {
                    this.mNetworkActive = false;
                }
                this.mDaemonHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        NetworkManagementService.this.notifyInterfaceClassActivity(type, 3, SystemClock.elapsedRealtimeNanos(), -1, false);
                    }
                });
            } catch (NativeDaemonConnectorException e) {
                throw e.rethrowAsParcelableException();
            }
        }
    }

    public void removeIdleTimer(String iface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        Slog.d(TAG, "Removing idletimer");
        synchronized (this.mIdleTimerLock) {
            final IdleTimerParams params = this.mActiveIdleTimers.get(iface);
            if (params != null) {
                int i = params.networkCount - 1;
                params.networkCount = i;
                if (i <= 0) {
                    try {
                        this.mConnector.execute("idletimer", "remove", iface, Integer.toString(params.timeout), Integer.toString(params.type));
                        this.mActiveIdleTimers.remove(iface);
                        this.mDaemonHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                NetworkManagementService.this.notifyInterfaceClassActivity(params.type, 1, SystemClock.elapsedRealtimeNanos(), -1, false);
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

    public void setInterfaceQuota(String iface, long quotaBytes) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        if (this.mBandwidthControlEnabled) {
            synchronized (this.mQuotaLock) {
                if (this.mActiveQuotas.containsKey(iface)) {
                    throw new IllegalStateException("iface " + iface + " already has quota");
                }
                try {
                    this.mConnector.execute("bandwidth", "setiquota", iface, Long.valueOf(quotaBytes));
                    this.mActiveQuotas.put(iface, Long.valueOf(quotaBytes));
                } catch (NativeDaemonConnectorException e) {
                    throw e.rethrowAsParcelableException();
                }
            }
        }
    }

    public void removeInterfaceQuota(String iface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        if (this.mBandwidthControlEnabled) {
            synchronized (this.mQuotaLock) {
                if (!this.mActiveQuotas.containsKey(iface)) {
                    return;
                }
                this.mActiveQuotas.remove(iface);
                this.mActiveAlerts.remove(iface);
                try {
                    this.mConnector.execute("bandwidth", "removeiquota", iface);
                } catch (NativeDaemonConnectorException e) {
                    throw e.rethrowAsParcelableException();
                }
            }
        }
    }

    public void setInterfaceAlert(String iface, long alertBytes) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        if (this.mBandwidthControlEnabled) {
            if (!this.mActiveQuotas.containsKey(iface)) {
                throw new IllegalStateException("setting alert requires existing quota on iface");
            }
            synchronized (this.mQuotaLock) {
                if (this.mActiveAlerts.containsKey(iface)) {
                    throw new IllegalStateException("iface " + iface + " already has alert");
                }
                try {
                    this.mConnector.execute("bandwidth", "setinterfacealert", iface, Long.valueOf(alertBytes));
                    this.mActiveAlerts.put(iface, Long.valueOf(alertBytes));
                } catch (NativeDaemonConnectorException e) {
                    throw e.rethrowAsParcelableException();
                }
            }
        }
    }

    public void removeInterfaceAlert(String iface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        if (this.mBandwidthControlEnabled) {
            synchronized (this.mQuotaLock) {
                if (!this.mActiveAlerts.containsKey(iface)) {
                    return;
                }
                try {
                    this.mConnector.execute("bandwidth", "removeinterfacealert", iface);
                    this.mActiveAlerts.remove(iface);
                } catch (NativeDaemonConnectorException e) {
                    throw e.rethrowAsParcelableException();
                }
            }
        }
    }

    public void setGlobalAlert(long alertBytes) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        if (this.mBandwidthControlEnabled) {
            try {
                this.mConnector.execute("bandwidth", "setglobalalert", Long.valueOf(alertBytes));
            } catch (NativeDaemonConnectorException e) {
                throw e.rethrowAsParcelableException();
            }
        }
    }

    private void setUidOnMeteredNetworkList(SparseBooleanArray quotaList, int uid, boolean blacklist, boolean enable) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        if (this.mBandwidthControlEnabled) {
            String chain = blacklist ? "naughtyapps" : "niceapps";
            String suffix = enable ? "add" : "remove";
            synchronized (this.mQuotaLock) {
                boolean oldEnable = quotaList.get(uid, false);
                if (oldEnable == enable) {
                    return;
                }
                try {
                    this.mConnector.execute("bandwidth", suffix + chain, Integer.valueOf(uid));
                    if (enable) {
                        quotaList.put(uid, true);
                    } else {
                        quotaList.delete(uid);
                    }
                } catch (NativeDaemonConnectorException e) {
                    throw e.rethrowAsParcelableException();
                }
            }
        }
    }

    public void setUidMeteredNetworkBlacklist(int uid, boolean enable) {
        setUidOnMeteredNetworkList(this.mUidRejectOnMetered, uid, true, enable);
    }

    public void setUidMeteredNetworkWhitelist(int uid, boolean enable) {
        setUidOnMeteredNetworkList(this.mUidAllowOnMetered, uid, false, enable);
    }

    public boolean setDataSaverModeEnabled(boolean enable) {
        Log.d(TAG, "setDataSaverMode: " + enable);
        synchronized (this.mQuotaLock) {
            if (this.mDataSaverMode == enable) {
                Log.w(TAG, "setDataSaverMode(): already " + this.mDataSaverMode);
                return true;
            }
            try {
                boolean changed = this.mNetdService.bandwidthEnableDataSaver(enable);
                if (changed) {
                    this.mDataSaverMode = enable;
                } else {
                    Log.w(TAG, "setDataSaverMode(" + enable + "): netd command silently failed");
                }
                return changed;
            } catch (RemoteException e) {
                Log.w(TAG, "setDataSaverMode(" + enable + "): netd command failed", e);
                return false;
            }
        }
    }

    public void setAllowOnlyVpnForUids(boolean add, UidRange[] uidRanges) throws ServiceSpecificException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mNetdService.networkRejectNonSecureVpn(add, uidRanges);
        } catch (RemoteException e) {
            Log.w(TAG, "setAllowOnlyVpnForUids(" + add + ", " + Arrays.toString(uidRanges) + "): netd command failed", e);
            throw e.rethrowAsRuntimeException();
        } catch (ServiceSpecificException e2) {
            Log.w(TAG, "setAllowOnlyVpnForUids(" + add + ", " + Arrays.toString(uidRanges) + "): netd command failed", e2);
            throw e2;
        }
    }

    public void setUidCleartextNetworkPolicy(int uid, int policy) {
        String policyString;
        if (Binder.getCallingUid() != uid) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        }
        synchronized (this.mQuotaLock) {
            int oldPolicy = this.mUidCleartextPolicy.get(uid, 0);
            if (oldPolicy == policy) {
                return;
            }
            if (!this.mStrictEnabled) {
                this.mUidCleartextPolicy.put(uid, policy);
                return;
            }
            switch (policy) {
                case 0:
                    policyString = "accept";
                    break;
                case 1:
                    policyString = "log";
                    break;
                case 2:
                    policyString = "reject";
                    break;
                default:
                    throw new IllegalArgumentException("Unknown policy " + policy);
            }
            try {
                this.mConnector.execute("strict", "set_uid_cleartext_policy", Integer.valueOf(uid), policyString);
                this.mUidCleartextPolicy.put(uid, policy);
            } catch (NativeDaemonConnectorException e) {
                throw e.rethrowAsParcelableException();
            }
        }
    }

    public boolean isBandwidthControlEnabled() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        return this.mBandwidthControlEnabled;
    }

    public NetworkStats getNetworkStatsUidDetail(int uid) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            return this.mStatsFactory.readNetworkStatsDetail(uid, (String[]) null, -1, (NetworkStats) null);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public NetworkStats getNetworkStatsTethering() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        NetworkStats stats = new NetworkStats(SystemClock.elapsedRealtime(), 1);
        try {
            NativeDaemonEvent[] events = this.mConnector.executeForList("bandwidth", "gettetherstats");
            for (NativeDaemonEvent event : events) {
                if (event.getCode() == 114) {
                    StringTokenizer tok = new StringTokenizer(event.getMessage());
                    try {
                        tok.nextToken();
                        String ifaceOut = tok.nextToken();
                        NetworkStats.Entry entry = new NetworkStats.Entry();
                        entry.iface = ifaceOut;
                        entry.uid = -5;
                        entry.set = 0;
                        entry.tag = 0;
                        entry.rxBytes = Long.parseLong(tok.nextToken());
                        entry.rxPackets = Long.parseLong(tok.nextToken());
                        entry.txBytes = Long.parseLong(tok.nextToken());
                        entry.txPackets = Long.parseLong(tok.nextToken());
                        stats.combineValues(entry);
                    } catch (NumberFormatException e) {
                        throw new IllegalStateException("problem parsing tethering stats: " + event);
                    } catch (NoSuchElementException e2) {
                        throw new IllegalStateException("problem parsing tethering stats: " + event);
                    }
                }
            }
            return stats;
        } catch (NativeDaemonConnectorException e3) {
            throw e3.rethrowAsParcelableException();
        }
    }

    public void setDnsConfigurationForNetwork(int netId, String[] servers, String domains) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        ContentResolver resolver = this.mContext.getContentResolver();
        int sampleValidity = Settings.Global.getInt(resolver, "dns_resolver_sample_validity_seconds", DNS_RESOLVER_DEFAULT_SAMPLE_VALIDITY_SECONDS);
        if (sampleValidity < 0 || sampleValidity > 65535) {
            Slog.w(TAG, "Invalid sampleValidity=" + sampleValidity + ", using default=" + DNS_RESOLVER_DEFAULT_SAMPLE_VALIDITY_SECONDS);
            sampleValidity = DNS_RESOLVER_DEFAULT_SAMPLE_VALIDITY_SECONDS;
        }
        int successThreshold = Settings.Global.getInt(resolver, "dns_resolver_success_threshold_percent", 25);
        if (successThreshold < 0 || successThreshold > 100) {
            Slog.w(TAG, "Invalid successThreshold=" + successThreshold + ", using default=25");
            successThreshold = 25;
        }
        int minSamples = Settings.Global.getInt(resolver, "dns_resolver_min_samples", 8);
        int maxSamples = Settings.Global.getInt(resolver, "dns_resolver_max_samples", 64);
        if (minSamples < 0 || minSamples > maxSamples || maxSamples > 64) {
            Slog.w(TAG, "Invalid sample count (min, max)=(" + minSamples + ", " + maxSamples + "), using default=(8, 64)");
            minSamples = 8;
            maxSamples = 64;
        }
        String[] domainStrs = domains == null ? new String[0] : domains.split(" ");
        int[] params = {sampleValidity, successThreshold, minSamples, maxSamples};
        try {
            this.mNetdService.setResolverConfiguration(netId, servers, domainStrs, params);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void setDnsServersForNetwork(int netId, String[] servers, String domains) {
        NativeDaemonConnector.Command cmd;
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        if (servers.length > 0) {
            Object[] objArr = new Object[3];
            objArr[0] = "setnetdns";
            objArr[1] = Integer.valueOf(netId);
            if (domains == null) {
                domains = "";
            }
            objArr[2] = domains;
            cmd = new NativeDaemonConnector.Command("resolver", objArr);
            for (String s : servers) {
                InetAddress a = NetworkUtils.numericToInetAddress(s);
                if (!a.isAnyLocalAddress()) {
                    cmd.appendArg(a.getHostAddress());
                }
            }
        } else {
            cmd = new NativeDaemonConnector.Command("resolver", "clearnetdns", Integer.valueOf(netId));
        }
        try {
            this.mConnector.execute(cmd);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void addVpnUidRanges(int netId, UidRange[] ranges) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        Object[] argv = new Object[13];
        argv[0] = DatabaseHelper.SoundModelContract.KEY_USERS;
        argv[1] = "add";
        argv[2] = Integer.valueOf(netId);
        int argc = 3;
        for (int i = 0; i < ranges.length; i++) {
            int argc2 = argc + 1;
            argv[argc] = ranges[i].toString();
            if (i == ranges.length - 1 || argc2 == argv.length) {
                try {
                    this.mConnector.execute("network", Arrays.copyOf(argv, argc2));
                    argc = 3;
                } catch (NativeDaemonConnectorException e) {
                    throw e.rethrowAsParcelableException();
                }
            } else {
                argc = argc2;
            }
        }
    }

    public void removeVpnUidRanges(int netId, UidRange[] ranges) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        Object[] argv = new Object[13];
        argv[0] = DatabaseHelper.SoundModelContract.KEY_USERS;
        argv[1] = "remove";
        argv[2] = Integer.valueOf(netId);
        int argc = 3;
        for (int i = 0; i < ranges.length; i++) {
            int argc2 = argc + 1;
            argv[argc] = ranges[i].toString();
            if (i == ranges.length - 1 || argc2 == argv.length) {
                try {
                    this.mConnector.execute("network", Arrays.copyOf(argv, argc2));
                    argc = 3;
                } catch (NativeDaemonConnectorException e) {
                    throw e.rethrowAsParcelableException();
                }
            } else {
                argc = argc2;
            }
        }
    }

    public void setFirewallEnabled(boolean enabled) {
        enforceSystemUid();
        try {
            NativeDaemonConnector nativeDaemonConnector = this.mConnector;
            Object[] objArr = new Object[2];
            objArr[0] = "enable";
            objArr[1] = enabled ? "whitelist" : "blacklist";
            nativeDaemonConnector.execute("firewall", objArr);
            this.mFirewallEnabled = enabled;
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public boolean isFirewallEnabled() {
        enforceSystemUid();
        return this.mFirewallEnabled;
    }

    public void setFirewallInterfaceRule(String iface, boolean allow) {
        enforceSystemUid();
        Preconditions.checkState(this.mFirewallEnabled);
        String rule = allow ? "allow" : "deny";
        try {
            this.mConnector.execute("firewall", "set_interface_rule", iface, rule);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void setFirewallEgressSourceRule(String addr, boolean allow) {
        enforceSystemUid();
        Preconditions.checkState(this.mFirewallEnabled);
        String rule = allow ? "allow" : "deny";
        try {
            this.mConnector.execute("firewall", "set_egress_source_rule", addr, rule);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void setFirewallEgressDestRule(String addr, int port, boolean allow) {
        enforceSystemUid();
        Preconditions.checkState(this.mFirewallEnabled);
        String rule = allow ? "allow" : "deny";
        try {
            this.mConnector.execute("firewall", "set_egress_dest_rule", addr, Integer.valueOf(port), rule);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    private void closeSocketsForFirewallChainLocked(int chain, String chainName) {
        UidRange[] ranges;
        int[] exemptUids;
        SparseIntArray rules = getUidFirewallRules(chain);
        int numUids = 0;
        if (getFirewallType(chain) == 0) {
            ranges = new UidRange[]{new UidRange(10000, Integer.MAX_VALUE)};
            exemptUids = new int[rules.size()];
            for (int i = 0; i < exemptUids.length; i++) {
                if (rules.valueAt(i) == 1) {
                    exemptUids[numUids] = rules.keyAt(i);
                    numUids++;
                }
            }
            if (numUids != exemptUids.length) {
                exemptUids = Arrays.copyOf(exemptUids, numUids);
            }
        } else {
            ranges = new UidRange[rules.size()];
            for (int i2 = 0; i2 < ranges.length; i2++) {
                if (rules.valueAt(i2) == 2) {
                    int uid = rules.keyAt(i2);
                    ranges[numUids] = new UidRange(uid, uid);
                    numUids++;
                }
            }
            if (numUids != ranges.length) {
                ranges = (UidRange[]) Arrays.copyOf(ranges, numUids);
            }
            exemptUids = new int[0];
        }
        try {
            this.mNetdService.socketDestroy(ranges, exemptUids);
        } catch (RemoteException | ServiceSpecificException e) {
            Slog.e(TAG, "Error closing sockets after enabling chain " + chainName + ": " + e);
        }
    }

    public void setFirewallChainEnabled(int chain, boolean enable) {
        String chainName;
        enforceSystemUid();
        synchronized (this.mQuotaLock) {
            if (this.mFirewallChainStates.get(chain) == enable) {
                return;
            }
            if (this.mFirewallChainStates.indexOfKey(chain) < 0 && !enable) {
                return;
            }
            this.mFirewallChainStates.put(chain, enable);
            String operation = enable ? "enable_chain" : "disable_chain";
            switch (chain) {
                case 1:
                    chainName = "dozable";
                    break;
                case 2:
                    chainName = "standby";
                    break;
                case 3:
                    chainName = "powersave";
                    break;
                default:
                    throw new IllegalArgumentException("Bad child chain: " + chain);
            }
            try {
                this.mConnector.execute("firewall", operation, chainName);
                if (enable) {
                    Slog.d(TAG, "Closing sockets after enabling chain " + chainName);
                    closeSocketsForFirewallChainLocked(chain, chainName);
                }
            } catch (NativeDaemonConnectorException e) {
                throw e.rethrowAsParcelableException();
            }
        }
    }

    private int getFirewallType(int chain) {
        switch (chain) {
            case 1:
                return 0;
            case 2:
                return 1;
            case 3:
                return 0;
            default:
                return isFirewallEnabled() ? 0 : 1;
        }
    }

    public void setFirewallUidRules(int chain, int[] uids, int[] rules) {
        enforceSystemUid();
        synchronized (this.mQuotaLock) {
            SparseIntArray uidFirewallRules = getUidFirewallRules(chain);
            SparseIntArray newRules = new SparseIntArray();
            for (int index = uids.length - 1; index >= 0; index--) {
                int uid = uids[index];
                int rule = rules[index];
                updateFirewallUidRuleLocked(chain, uid, rule);
                newRules.put(uid, rule);
            }
            SparseIntArray rulesToRemove = new SparseIntArray();
            for (int index2 = uidFirewallRules.size() - 1; index2 >= 0; index2--) {
                int uid2 = uidFirewallRules.keyAt(index2);
                if (newRules.indexOfKey(uid2) < 0) {
                    rulesToRemove.put(uid2, 0);
                }
            }
            for (int index3 = rulesToRemove.size() - 1; index3 >= 0; index3--) {
                updateFirewallUidRuleLocked(chain, rulesToRemove.keyAt(index3), 0);
            }
            try {
                switch (chain) {
                    case 1:
                        this.mNetdService.firewallReplaceUidChain("fw_dozable", true, uids);
                        break;
                    case 2:
                        this.mNetdService.firewallReplaceUidChain("fw_standby", false, uids);
                        break;
                    case 3:
                        this.mNetdService.firewallReplaceUidChain("fw_powersave", true, uids);
                        break;
                    default:
                        Slog.d(TAG, "setFirewallUidRules() called on invalid chain: " + chain);
                        break;
                }
            } catch (RemoteException e) {
                Slog.w(TAG, "Error flushing firewall chain " + chain, e);
            }
        }
    }

    public void setFirewallUidRule(int chain, int uid, int rule) {
        enforceSystemUid();
        synchronized (this.mQuotaLock) {
            setFirewallUidRuleLocked(chain, uid, rule);
        }
    }

    private void setFirewallUidRuleLocked(int chain, int uid, int rule) {
        if (!updateFirewallUidRuleLocked(chain, uid, rule)) {
            return;
        }
        try {
            this.mConnector.execute("firewall", "set_uid_rule", getFirewallChainName(chain), Integer.valueOf(uid), getFirewallRuleName(chain, rule));
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    private boolean updateFirewallUidRuleLocked(int chain, int uid, int rule) {
        SparseIntArray uidFirewallRules = getUidFirewallRules(chain);
        int oldUidFirewallRule = uidFirewallRules.get(uid, 0);
        Slog.d(TAG, "oldRule = " + oldUidFirewallRule + ", newRule=" + rule + " for uid=" + uid + " on chain " + chain);
        if (oldUidFirewallRule == rule) {
            Slog.d(TAG, "!!!!! Skipping change");
            return false;
        }
        String ruleName = getFirewallRuleName(chain, rule);
        String oldRuleName = getFirewallRuleName(chain, oldUidFirewallRule);
        if (rule == 0) {
            uidFirewallRules.delete(uid);
        } else {
            uidFirewallRules.put(uid, rule);
        }
        return !ruleName.equals(oldRuleName);
    }

    private String getFirewallRuleName(int chain, int rule) {
        if (getFirewallType(chain) == 0) {
            if (rule == 1) {
                return "allow";
            }
            return "deny";
        }
        if (rule == 2) {
            return "deny";
        }
        return "allow";
    }

    private SparseIntArray getUidFirewallRules(int chain) {
        switch (chain) {
            case 0:
                return this.mUidFirewallRules;
            case 1:
                return this.mUidFirewallDozableRules;
            case 2:
                return this.mUidFirewallStandbyRules;
            case 3:
                return this.mUidFirewallPowerSaveRules;
            default:
                throw new IllegalArgumentException("Unknown chain:" + chain);
        }
    }

    public String getFirewallChainName(int chain) {
        switch (chain) {
            case 0:
                return "none";
            case 1:
                return "dozable";
            case 2:
                return "standby";
            case 3:
                return "powersave";
            default:
                throw new IllegalArgumentException("Unknown chain:" + chain);
        }
    }

    private static void enforceSystemUid() {
        int uid = Binder.getCallingUid();
        if (uid == 1000) {
        } else {
            throw new SecurityException("Only available to AID_SYSTEM");
        }
    }

    public void startClatd(String interfaceName) throws IllegalStateException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("clatd", "start", interfaceName);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void stopClatd(String interfaceName) throws IllegalStateException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("clatd", "stop", interfaceName);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public boolean isClatdStarted(String interfaceName) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            NativeDaemonEvent event = this.mConnector.execute("clatd", "status", interfaceName);
            event.checkCode(NetdResponseCode.ClatdStatusResult);
            return event.getMessage().endsWith("started");
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void registerNetworkActivityListener(INetworkActivityListener listener) {
        this.mNetworkActivityListeners.register(listener);
    }

    public void unregisterNetworkActivityListener(INetworkActivityListener listener) {
        this.mNetworkActivityListeners.unregister(listener);
    }

    public boolean isNetworkActive() {
        boolean zIsEmpty;
        synchronized (this.mNetworkActivityListeners) {
            zIsEmpty = !this.mNetworkActive ? this.mActiveIdleTimers.isEmpty() : true;
        }
        return zIsEmpty;
    }

    private void reportNetworkActive() {
        int length = this.mNetworkActivityListeners.beginBroadcast();
        for (int i = 0; i < length; i++) {
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
        if (this.mConnector == null) {
            return;
        }
        this.mConnector.monitor();
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.DUMP", TAG);
        pw.println("NetworkManagementService NativeDaemonConnector Log:");
        this.mConnector.dump(fd, pw, args);
        pw.println();
        pw.print("Bandwidth control enabled: ");
        pw.println(this.mBandwidthControlEnabled);
        pw.print("mMobileActivityFromRadio=");
        pw.print(this.mMobileActivityFromRadio);
        pw.print(" mLastPowerStateFromRadio=");
        pw.println(this.mLastPowerStateFromRadio);
        pw.print("mNetworkActive=");
        pw.println(this.mNetworkActive);
        synchronized (this.mQuotaLock) {
            pw.print("Active quota ifaces: ");
            pw.println(this.mActiveQuotas.toString());
            pw.print("Active alert ifaces: ");
            pw.println(this.mActiveAlerts.toString());
            pw.print("Data saver mode: ");
            pw.println(this.mDataSaverMode);
            dumpUidRuleOnQuotaLocked(pw, "blacklist", this.mUidRejectOnMetered);
            dumpUidRuleOnQuotaLocked(pw, "whitelist", this.mUidAllowOnMetered);
        }
        synchronized (this.mUidFirewallRules) {
            dumpUidFirewallRule(pw, "", this.mUidFirewallRules);
        }
        pw.print("UID firewall standby chain enabled: ");
        pw.println(this.mFirewallChainStates.get(2));
        synchronized (this.mUidFirewallStandbyRules) {
            dumpUidFirewallRule(pw, "standby", this.mUidFirewallStandbyRules);
        }
        pw.print("UID firewall dozable chain enabled: ");
        pw.println(this.mFirewallChainStates.get(1));
        synchronized (this.mUidFirewallDozableRules) {
            dumpUidFirewallRule(pw, "dozable", this.mUidFirewallDozableRules);
        }
        pw.println("UID firewall powersave chain enabled: " + this.mFirewallChainStates.get(3));
        synchronized (this.mUidFirewallPowerSaveRules) {
            dumpUidFirewallRule(pw, "powersave", this.mUidFirewallPowerSaveRules);
        }
        synchronized (this.mIdleTimerLock) {
            pw.println("Idle timers:");
            for (Map.Entry<String, IdleTimerParams> ent : this.mActiveIdleTimers.entrySet()) {
                pw.print("  ");
                pw.print(ent.getKey());
                pw.println(":");
                IdleTimerParams params = ent.getValue();
                pw.print("    timeout=");
                pw.print(params.timeout);
                pw.print(" type=");
                pw.print(params.type);
                pw.print(" networkCount=");
                pw.println(params.networkCount);
            }
        }
        pw.print("Firewall enabled: ");
        pw.println(this.mFirewallEnabled);
        pw.print("Netd service status: ");
        if (this.mNetdService == null) {
            pw.println("disconnected");
            return;
        }
        try {
            boolean alive = this.mNetdService.isAlive();
            pw.println(alive ? "alive" : "dead");
        } catch (RemoteException e) {
            pw.println("unreachable");
        }
    }

    private void dumpUidRuleOnQuotaLocked(PrintWriter pw, String name, SparseBooleanArray list) {
        pw.print("UID bandwith control ");
        pw.print(name);
        pw.print(" rule: [");
        int size = list.size();
        for (int i = 0; i < size; i++) {
            pw.print(list.keyAt(i));
            if (i < size - 1) {
                pw.print(",");
            }
        }
        pw.println("]");
    }

    private void dumpUidFirewallRule(PrintWriter pw, String name, SparseIntArray rules) {
        pw.print("UID firewall ");
        pw.print(name);
        pw.print(" rule: [");
        int size = rules.size();
        for (int i = 0; i < size; i++) {
            pw.print(rules.keyAt(i));
            pw.print(":");
            pw.print(rules.valueAt(i));
            if (i < size - 1) {
                pw.print(",");
            }
        }
        pw.println("]");
    }

    public void createPhysicalNetwork(int netId, String permission) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            if (permission != null) {
                this.mConnector.execute("network", "create", Integer.valueOf(netId), permission);
            } else {
                this.mConnector.execute("network", "create", Integer.valueOf(netId));
            }
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void createVirtualNetwork(int netId, boolean hasDNS, boolean secure) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            NativeDaemonConnector nativeDaemonConnector = this.mConnector;
            Object[] objArr = new Object[5];
            objArr[0] = "create";
            objArr[1] = Integer.valueOf(netId);
            objArr[2] = "vpn";
            objArr[3] = hasDNS ? "1" : "0";
            objArr[4] = secure ? "1" : "0";
            nativeDaemonConnector.execute("network", objArr);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void removeNetwork(int netId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("network", "destroy", Integer.valueOf(netId));
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void addInterfaceToNetwork(String iface, int netId) {
        modifyInterfaceInNetwork("add", "" + netId, iface);
    }

    public void removeInterfaceFromNetwork(String iface, int netId) {
        modifyInterfaceInNetwork("remove", "" + netId, iface);
    }

    private void modifyInterfaceInNetwork(String action, String netId, String iface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("network", "interface", action, netId, iface);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void addLegacyRouteForNetId(int netId, RouteInfo routeInfo, int uid) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        NativeDaemonConnector.Command cmd = new NativeDaemonConnector.Command("network", "route", "legacy", Integer.valueOf(uid), "add", Integer.valueOf(netId));
        LinkAddress la = routeInfo.getDestinationLinkAddress();
        cmd.appendArg(routeInfo.getInterface());
        cmd.appendArg(la.getAddress().getHostAddress() + "/" + la.getPrefixLength());
        if (routeInfo.hasGateway()) {
            cmd.appendArg(routeInfo.getGateway().getHostAddress());
        }
        try {
            this.mConnector.execute(cmd);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void setDefaultNetId(int netId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("network", "default", "set", Integer.valueOf(netId));
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void clearDefaultNetId() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("network", "default", "clear");
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void setNetworkPermission(int netId, String permission) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            if (permission != null) {
                this.mConnector.execute("network", "permission", "network", "set", permission, Integer.valueOf(netId));
            } else {
                this.mConnector.execute("network", "permission", "network", "clear", Integer.valueOf(netId));
            }
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void setPermission(String permission, int[] uids) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        Object[] argv = new Object[14];
        argv[0] = "permission";
        argv[1] = "user";
        argv[2] = "set";
        argv[3] = permission;
        int argc = 4;
        for (int i = 0; i < uids.length; i++) {
            int argc2 = argc + 1;
            argv[argc] = Integer.valueOf(uids[i]);
            if (i == uids.length - 1 || argc2 == argv.length) {
                try {
                    this.mConnector.execute("network", Arrays.copyOf(argv, argc2));
                    argc = 4;
                } catch (NativeDaemonConnectorException e) {
                    throw e.rethrowAsParcelableException();
                }
            } else {
                argc = argc2;
            }
        }
    }

    public void clearPermission(int[] uids) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        Object[] argv = new Object[13];
        argv[0] = "permission";
        argv[1] = "user";
        argv[2] = "clear";
        int argc = 3;
        for (int i = 0; i < uids.length; i++) {
            int argc2 = argc + 1;
            argv[argc] = Integer.valueOf(uids[i]);
            if (i == uids.length - 1 || argc2 == argv.length) {
                try {
                    this.mConnector.execute("network", Arrays.copyOf(argv, argc2));
                    argc = 3;
                } catch (NativeDaemonConnectorException e) {
                    throw e.rethrowAsParcelableException();
                }
            } else {
                argc = argc2;
            }
        }
    }

    public void allowProtect(int uid) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("network", "protect", "allow", Integer.valueOf(uid));
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void denyProtect(int uid) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("network", "protect", "deny", Integer.valueOf(uid));
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void addInterfaceToLocalNetwork(String iface, List<RouteInfo> routes) {
        modifyInterfaceInNetwork("add", "local", iface);
        for (RouteInfo route : routes) {
            if (!route.isDefaultRoute()) {
                modifyRoute("add", "local", route);
            }
        }
    }

    public void removeInterfaceFromLocalNetwork(String iface) {
        modifyInterfaceInNetwork("remove", "local", iface);
    }

    public boolean getIpv6ForwardingEnabled() throws IllegalStateException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            NativeDaemonEvent event = this.mConnector.execute("ipv6fwd", "status");
            event.checkCode(211);
            return event.getMessage().endsWith("enabled");
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void setIpv6ForwardingEnabled(boolean enable) throws IllegalStateException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            NativeDaemonConnector nativeDaemonConnector = this.mConnector;
            Object[] objArr = new Object[1];
            Object[] objArr2 = new Object[1];
            objArr2[0] = enable ? "en" : "dis";
            objArr[0] = String.format("%sable", objArr2);
            nativeDaemonConnector.execute("ipv6fwd", objArr);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    private void modifyNatIpv6(String action, String internalInterface, String externalInterface) throws SocketException {
        NativeDaemonConnector.Command cmd = new NativeDaemonConnector.Command("IPv6Tether", action, internalInterface, externalInterface, 0);
        try {
            this.mConnector.execute(cmd);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void enableNatIpv6(String internalInterface, String externalInterface) throws IllegalStateException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        Log.d(TAG, "enableNatIpv6(" + internalInterface + ", " + externalInterface + ")");
        try {
            modifyNatIpv6("enable", internalInterface, externalInterface);
        } catch (SocketException e) {
            Log.e(TAG, "enableNatIpv6 got Exception " + e.toString());
            throw new IllegalStateException("Unable to communicate to native daemon for enabling Ipv6 NAT interface");
        }
    }

    public void setRouteIpv6(String internalInterface, String externalInterface) throws IllegalStateException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        Log.d(TAG, "setRouteIpv6(" + internalInterface + ", " + externalInterface + ")");
        try {
            this.mConnector.execute("IPv6Tether", "setroute", internalInterface, externalInterface);
        } catch (NativeDaemonConnectorException e) {
            Log.e(TAG, "setRouteIpv6 got Exception " + e.toString());
            throw new IllegalStateException("Unable to communicate to native daemon for setRouteIpv6");
        }
    }

    public void clearRouteIpv6(String internalInterface, String externalInterface) throws IllegalStateException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        Log.d(TAG, "clearRouteIpv6(" + internalInterface + ", " + externalInterface + ")");
        try {
            this.mConnector.execute("IPv6Tether", "clearroute", internalInterface, externalInterface);
        } catch (NativeDaemonConnectorException e) {
            Log.e(TAG, "clearRouteIpv6 got Exception " + e.toString());
            throw new IllegalStateException("Unable to communicate to native daemon for clearRouteIpv6");
        }
    }

    public void setSourceRouteIpv6(String internalInterface, String externalInterface) throws IllegalStateException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        Log.d(TAG, "setSourceRouteIpv6(" + internalInterface + ", " + externalInterface + ")");
        try {
            this.mConnector.execute("IPv6Tether", "setsroute", internalInterface, externalInterface);
        } catch (NativeDaemonConnectorException e) {
            Log.e(TAG, "setSourceRouteIpv6 got Exception " + e.toString());
            throw new IllegalStateException("Unable to communicate to native daemon for setSourceRouteIpv6");
        }
    }

    public void clearSourceRouteIpv6(String internalInterface, String externalInterface) throws IllegalStateException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        Log.d(TAG, "clearSourceRouteIpv6(" + internalInterface + ", " + externalInterface + ")");
        try {
            this.mConnector.execute("IPv6Tether", "clearsroute", internalInterface, externalInterface);
        } catch (NativeDaemonConnectorException e) {
            Log.e(TAG, "clearSourceRouteIpv6 got Exception " + e.toString());
            throw new IllegalStateException("Unable to communicate to native daemon for clearSourceRouteIpv6");
        }
    }

    public void disableNatIpv6(String internalInterface, String externalInterface) throws IllegalStateException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        Log.d(TAG, "disableNatIpv6(" + internalInterface + ", " + externalInterface + ")");
        try {
            modifyNatIpv6("disable", internalInterface, externalInterface);
        } catch (SocketException e) {
            Log.e(TAG, "disableNatIpv6 got Exception " + e.toString());
            throw new IllegalStateException("Unable to communicate to native daemon for disabling Ipv6 NAT interface");
        }
    }

    public void setFirewallEgressProtoRule(String proto, boolean allow) {
        enforceSystemUid();
        Preconditions.checkState(this.mFirewallEnabled);
        String rule = allow ? "allow" : "deny";
        try {
            this.mConnector.execute("firewall", "set_egress_proto_rule", proto, rule);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void setDhcpv6Enabled(boolean enable, String ifc) throws IllegalStateException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CHANGE_NETWORK_STATE", "NetworkManagementService");
        try {
            NativeDaemonConnector nativeDaemonConnector = this.mConnector;
            Object[] objArr = new Object[2];
            Object[] objArr2 = new Object[1];
            objArr2[0] = enable ? "add" : "remove";
            objArr[0] = String.format("%s", objArr2);
            objArr[1] = ifc;
            nativeDaemonConnector.execute("IPv6Tether", objArr);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void enableUdpForwarding(boolean enabled, String internalInterface, String externalInterface, String ipAddr) throws IllegalStateException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            if (enabled) {
                this.mConnector.execute("firewall", "set_udp_forwarding", internalInterface, externalInterface, ipAddr);
            } else {
                this.mConnector.execute("firewall", "clear_udp_forwarding", internalInterface, externalInterface);
            }
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void getUsbClient(String iface) throws IllegalStateException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("firewall", "get_usb_client", iface);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    private boolean isWifi(String iface) {
        String[] tetherableWifiRegexs = this.mContext.getResources().getStringArray(R.array.config_autoBrightnessDisplayValuesNitsIdle);
        for (String regex : tetherableWifiRegexs) {
            if (iface.matches(regex)) {
                return true;
            }
        }
        return false;
    }

    public void setInterfaceThrottle(String iface, int rxKbps, int txKbps) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("interface", "setthrottle", iface, Integer.valueOf(rxKbps), Integer.valueOf(txKbps));
            if (!isWifi(iface)) {
                return;
            }
            Settings.Secure.putInt(this.mContext.getContentResolver(), "interface_throttle_rx_value", rxKbps);
            Settings.Secure.putInt(this.mContext.getContentResolver(), "interface_throttle_tx_value", txKbps);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    private int getInterfaceThrottle(String iface, boolean rx) {
        try {
            NativeDaemonConnector nativeDaemonConnector = this.mConnector;
            Object[] objArr = new Object[3];
            objArr[0] = "getthrottle";
            objArr[1] = iface;
            objArr[2] = rx ? "rx" : "tx";
            NativeDaemonEvent event = nativeDaemonConnector.execute("interface", objArr);
            if (rx) {
                event.checkCode(NetdResponseCode.InterfaceRxThrottleResult);
            } else {
                event.checkCode(NetdResponseCode.InterfaceTxThrottleResult);
            }
            try {
                return Integer.parseInt(event.getMessage());
            } catch (NumberFormatException e) {
                throw new IllegalStateException("unexpected response:" + event);
            }
        } catch (NativeDaemonConnectorException e2) {
            throw e2.rethrowAsParcelableException();
        }
    }

    public int getInterfaceRxThrottle(String iface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        return getInterfaceThrottle(iface, true);
    }

    public int getInterfaceTxThrottle(String iface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        return getInterfaceThrottle(iface, false);
    }

    public void setFirewallUidChainRule(int uid, int networkType, boolean allow) {
        String rule = allow ? "allow" : "deny";
        String chain = networkType == 1 ? "wifi" : "mobile";
        try {
            this.mConnector.execute("firewall", "set_uid_fw_rule", Integer.valueOf(uid), chain, rule);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void clearFirewallChain(String chain) {
        try {
            this.mConnector.execute("firewall", "clear_fw_chain", chain);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void disablePPPOE() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("pppoectl", "stop");
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public String[] getSipInfo(String iface, String service, String protocol) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        Log.e(TAG, "getSipInfo:" + iface + " " + service + " " + protocol);
        try {
            NativeDaemonEvent event = this.mConnector.execute("NetInfo", "getsip", iface, service, protocol);
            event.checkCode(NetdResponseCode.NetInfoSipResult);
            ArrayList<String> result = new ArrayList<>();
            StringTokenizer st = new StringTokenizer(event.getMessage());
            while (st.hasMoreTokens()) {
                result.add(st.nextToken(" "));
            }
            if (!result.isEmpty()) {
                Log.e(TAG, "getSipInfo result:" + result);
                return (String[]) result.toArray(new String[result.size()]);
            }
            throw new IllegalStateException("Got an empty sipinfo response");
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void clearSipInfo(String iface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("NetInfo", "clearsip", iface);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void clearIotFirewall() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("firewall", "clear_nsiot_firewall");
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void setIotFirewall() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("firewall", "set_nsiot_firewall");
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void clearVolteIotFirewall(String ifc) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("firewall", "clear_volte_nsiot_firewall", ifc);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void setVolteIotFirewall(String ifc) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("firewall", "set_volte_nsiot_firewall", ifc);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void addBridge(String bridgeInterface) throws IllegalStateException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("brctl", "addbr", bridgeInterface);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void deleteBridge(String bridgeInterface) throws IllegalStateException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("brctl", "delbr", bridgeInterface);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void addBridgeInterface(String bridgeInterface, String portInterface) throws IllegalStateException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("brctl", "addif", bridgeInterface, portInterface);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void deleteBridgeInterface(String bridgeInterface, String portInterface) throws IllegalStateException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("brctl", "delif", bridgeInterface, portInterface);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void clearBridgeMac(String bridgeInterface) throws IllegalStateException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("brctl", "clear", "mac", bridgeInterface);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }
}
