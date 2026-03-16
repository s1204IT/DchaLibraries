package com.android.server;

import android.content.Context;
import android.net.ConnectivityManager;
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
import android.os.SystemClock;
import android.os.SystemProperties;
import android.telephony.DataConnectionRealTimeInfo;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.Slog;
import android.util.SparseBooleanArray;
import com.android.internal.app.IBatteryStats;
import com.android.internal.net.NetworkStatsFactory;
import com.android.internal.util.Preconditions;
import com.android.server.NativeDaemonConnector;
import com.android.server.Watchdog;
import com.android.server.net.LockdownVpnTracker;
import com.android.server.pm.PackageManagerService;
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
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.concurrent.CountDownLatch;

public class NetworkManagementService extends INetworkManagementService.Stub implements Watchdog.Monitor {
    static final int DAEMON_MSG_MOBILE_CONN_REAL_TIME_INFO = 1;
    private static final boolean DBG = false;
    public static final String LIMIT_GLOBAL_ALERT = "globalAlert";
    private static final int MAX_UID_RANGES_PER_COMMAND = 10;
    private static final String NETD_SOCKET_NAME = "netd";
    private static final String NETD_TAG = "NetdConnector";
    private static final String TAG = "NetworkManagementService";
    private volatile boolean mBandwidthControlEnabled;
    private IBatteryStats mBatteryStats;
    private final NativeDaemonConnector mConnector;
    private final Context mContext;
    private final Handler mDaemonHandler;
    private volatile boolean mFirewallEnabled;
    private boolean mNetworkActive;
    private final PhoneStateListener mPhoneStateListener;
    private final Thread mThread;
    private CountDownLatch mConnectedSignal = new CountDownLatch(1);
    private final RemoteCallbackList<INetworkManagementEventObserver> mObservers = new RemoteCallbackList<>();
    private final NetworkStatsFactory mStatsFactory = new NetworkStatsFactory();
    private Object mQuotaLock = new Object();
    private HashMap<String, Long> mActiveQuotas = Maps.newHashMap();
    private HashMap<String, Long> mActiveAlerts = Maps.newHashMap();
    private SparseBooleanArray mUidRejectOnQuota = new SparseBooleanArray();
    private Object mIdleTimerLock = new Object();
    private HashMap<String, IdleTimerParams> mActiveIdleTimers = Maps.newHashMap();
    private boolean mMobileActivityFromRadio = DBG;
    private int mLastPowerStateFromRadio = DataConnectionRealTimeInfo.DC_POWER_STATE_LOW;
    private final RemoteCallbackList<INetworkActivityListener> mNetworkActivityListeners = new RemoteCallbackList<>();
    private final Handler mFgHandler = new Handler(FgThread.get().getLooper());

    class NetdResponseCode {
        public static final int BandwidthControl = 601;
        public static final int ClatdStatusResult = 223;
        public static final int DnsProxyQueryResult = 222;
        public static final int IPv6FwdStatusResult = 226;
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
        public static final int TetherDnsFwdTgtListResult = 112;
        public static final int TetherInterfaceListResult = 111;
        public static final int TetherStatusResult = 210;
        public static final int TetheringStatsListResult = 114;
        public static final int TetheringStatsResult = 221;
        public static final int TtyListResult = 113;
        public static final int V6TetherStatusResult = 227;

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
        if ("simulator".equals(SystemProperties.get("ro.product.device"))) {
            this.mConnector = null;
            this.mThread = null;
            this.mDaemonHandler = null;
            this.mPhoneStateListener = null;
            return;
        }
        this.mConnector = new NativeDaemonConnector(new NetdCallbackReceiver(), socket, 10, NETD_TAG, 160, null, FgThread.get().getLooper());
        this.mThread = new Thread(this.mConnector, NETD_TAG);
        this.mDaemonHandler = new Handler(FgThread.get().getLooper());
        this.mPhoneStateListener = new PhoneStateListener(Integer.MAX_VALUE, this.mDaemonHandler.getLooper()) {
            public void onDataConnectionRealTimeInfoChanged(DataConnectionRealTimeInfo dcRtInfo) {
                NetworkManagementService.this.notifyInterfaceClassActivity(0, dcRtInfo.getDcPowerState(), dcRtInfo.getTime(), true);
            }
        };
        TelephonyManager tm = (TelephonyManager) context.getSystemService("phone");
        if (tm != null) {
            tm.listen(this.mPhoneStateListener, PackageManagerService.DumpState.DUMP_INSTALLS);
        }
        Watchdog.getInstance().addMonitor(this);
    }

    static NetworkManagementService create(Context context, String socket) throws InterruptedException {
        NetworkManagementService service = new NetworkManagementService(context, socket);
        CountDownLatch connectedSignal = service.mConnectedSignal;
        service.mThread.start();
        connectedSignal.await();
        return service;
    }

    public static NetworkManagementService create(Context context) throws InterruptedException {
        return create(context, NETD_SOCKET_NAME);
    }

    public void systemReady() {
        prepareNativeDaemon();
    }

    private IBatteryStats getBatteryStats() {
        IBatteryStats iBatteryStats;
        synchronized (this) {
            if (this.mBatteryStats != null) {
                iBatteryStats = this.mBatteryStats;
            } else {
                this.mBatteryStats = IBatteryStats.Stub.asInterface(ServiceManager.getService("batterystats"));
                iBatteryStats = this.mBatteryStats;
            }
        }
        return iBatteryStats;
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
            } catch (RemoteException e) {
            } catch (RuntimeException e2) {
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
            } catch (RemoteException e) {
            } catch (RuntimeException e2) {
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
            } catch (RemoteException e) {
            } catch (RuntimeException e2) {
            } catch (Throwable th) {
                this.mObservers.finishBroadcast();
                throw th;
            }
        }
        this.mObservers.finishBroadcast();
    }

    private void notifyInterfaceRemoved(String iface) {
        this.mActiveAlerts.remove(iface);
        this.mActiveQuotas.remove(iface);
        int length = this.mObservers.beginBroadcast();
        for (int i = 0; i < length; i++) {
            try {
                this.mObservers.getBroadcastItem(i).interfaceRemoved(iface);
            } catch (RemoteException e) {
            } catch (RuntimeException e2) {
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
            } catch (RemoteException e) {
            } catch (RuntimeException e2) {
            } catch (Throwable th) {
                this.mObservers.finishBroadcast();
                throw th;
            }
        }
        this.mObservers.finishBroadcast();
    }

    private void notifyInterfaceClassActivity(int type, int powerState, long tsNanos, boolean fromRadio) {
        boolean isActive = true;
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
                    getBatteryStats().noteMobileRadioPowerState(powerState, tsNanos);
                } catch (RemoteException e) {
                }
            }
        }
        if (powerState != DataConnectionRealTimeInfo.DC_POWER_STATE_MEDIUM && powerState != DataConnectionRealTimeInfo.DC_POWER_STATE_HIGH) {
            isActive = DBG;
        }
        if (!isMobile || fromRadio || !this.mMobileActivityFromRadio) {
            int length = this.mObservers.beginBroadcast();
            for (int i = 0; i < length; i++) {
                try {
                    this.mObservers.getBroadcastItem(i).interfaceClassDataActivityChanged(Integer.toString(type), isActive, tsNanos);
                } catch (RemoteException e2) {
                } catch (RuntimeException e3) {
                } catch (Throwable th) {
                    this.mObservers.finishBroadcast();
                    throw th;
                }
            }
            this.mObservers.finishBroadcast();
        }
        boolean report = DBG;
        synchronized (this.mIdleTimerLock) {
            if (this.mActiveIdleTimers.isEmpty()) {
                isActive = true;
            }
            if (this.mNetworkActive != isActive) {
                this.mNetworkActive = isActive;
                report = isActive;
            }
        }
        if (report) {
            reportNetworkActive();
        }
    }

    private void prepareNativeDaemon() {
        this.mBandwidthControlEnabled = DBG;
        boolean hasKernelSupport = new File("/proc/net/xt_qtaguid/ctrl").exists();
        if (hasKernelSupport) {
            Slog.d(TAG, "enabling bandwidth control");
            try {
                this.mConnector.execute("bandwidth", "enable");
                this.mBandwidthControlEnabled = true;
            } catch (NativeDaemonConnectorException e) {
                Log.wtf(TAG, "problem enabling bandwidth controls", e);
            }
        } else {
            Slog.d(TAG, "not enabling bandwidth control");
        }
        SystemProperties.set("net.qtaguid_enabled", this.mBandwidthControlEnabled ? "1" : "0");
        if (this.mBandwidthControlEnabled) {
            try {
                getBatteryStats().noteNetworkStatsEnabled();
            } catch (RemoteException e2) {
            }
        }
        synchronized (this.mQuotaLock) {
            int size = this.mActiveQuotas.size();
            if (size > 0) {
                Slog.d(TAG, "pushing " + size + " active quota rules");
                HashMap<String, Long> activeQuotas = this.mActiveQuotas;
                this.mActiveQuotas = Maps.newHashMap();
                for (Map.Entry<String, Long> entry : activeQuotas.entrySet()) {
                    setInterfaceQuota(entry.getKey(), entry.getValue().longValue());
                }
            }
            int size2 = this.mActiveAlerts.size();
            if (size2 > 0) {
                Slog.d(TAG, "pushing " + size2 + " active alert rules");
                HashMap<String, Long> activeAlerts = this.mActiveAlerts;
                this.mActiveAlerts = Maps.newHashMap();
                for (Map.Entry<String, Long> entry2 : activeAlerts.entrySet()) {
                    setInterfaceAlert(entry2.getKey(), entry2.getValue().longValue());
                }
            }
            int size3 = this.mUidRejectOnQuota.size();
            if (size3 > 0) {
                Slog.d(TAG, "pushing " + size3 + " active uid rules");
                SparseBooleanArray uidRejectOnQuota = this.mUidRejectOnQuota;
                this.mUidRejectOnQuota = new SparseBooleanArray();
                for (int i = 0; i < uidRejectOnQuota.size(); i++) {
                    setUidNetworkRules(uidRejectOnQuota.keyAt(i), uidRejectOnQuota.valueAt(i));
                }
            }
        }
        setFirewallEnabled((this.mFirewallEnabled || LockdownVpnTracker.isEnabled()) ? true : DBG);
    }

    private void notifyAddressUpdated(String iface, LinkAddress address) {
        int length = this.mObservers.beginBroadcast();
        for (int i = 0; i < length; i++) {
            try {
                this.mObservers.getBroadcastItem(i).addressUpdated(iface, address);
            } catch (RemoteException e) {
            } catch (RuntimeException e2) {
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
            } catch (RemoteException e) {
            } catch (RuntimeException e2) {
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
            } catch (RemoteException e) {
            } catch (RuntimeException e2) {
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
            } catch (RemoteException e) {
            } catch (RuntimeException e2) {
            } catch (Throwable th) {
                this.mObservers.finishBroadcast();
                throw th;
            }
        }
        this.mObservers.finishBroadcast();
    }

    private class NetdCallbackReceiver implements INativeDaemonConnectorCallbacks {
        private NetdCallbackReceiver() {
        }

        @Override
        public void onDaemonConnected() {
            if (NetworkManagementService.this.mConnectedSignal != null) {
                NetworkManagementService.this.mConnectedSignal.countDown();
                NetworkManagementService.this.mConnectedSignal = null;
            } else {
                NetworkManagementService.this.mFgHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        NetworkManagementService.this.prepareNativeDaemon();
                    }
                });
            }
        }

        @Override
        public boolean onCheckHoldWakeLock(int code) {
            if (code == 613) {
                return true;
            }
            return NetworkManagementService.DBG;
        }

        @Override
        public boolean onEvent(int code, String raw, String[] cooked) {
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
                case 613:
                    if (cooked.length < 4 || !cooked[1].equals("IfaceClass")) {
                        throw new IllegalStateException(errorMessage);
                    }
                    long timestampNanos = 0;
                    if (cooked.length == 5) {
                        try {
                            timestampNanos = Long.parseLong(cooked[4]);
                            break;
                        } catch (NumberFormatException e) {
                        }
                    } else {
                        timestampNanos = SystemClock.elapsedRealtimeNanos();
                    }
                    boolean isActive = cooked[2].equals("active");
                    NetworkManagementService.this.notifyInterfaceClassActivity(Integer.parseInt(cooked[3]), isActive ? DataConnectionRealTimeInfo.DC_POWER_STATE_HIGH : DataConnectionRealTimeInfo.DC_POWER_STATE_LOW, timestampNanos, NetworkManagementService.DBG);
                    return true;
                case 614:
                    if (cooked.length < 7 || !cooked[1].equals("Address")) {
                        throw new IllegalStateException(errorMessage);
                    }
                    String iface = cooked[4];
                    try {
                        int flags = Integer.parseInt(cooked[5]);
                        int scope = Integer.parseInt(cooked[6]);
                        LinkAddress address = new LinkAddress(cooked[3], flags, scope);
                        if (cooked[2].equals("updated")) {
                            NetworkManagementService.this.notifyAddressUpdated(iface, address);
                        } else {
                            NetworkManagementService.this.notifyAddressRemoved(iface, address);
                        }
                        return true;
                    } catch (NumberFormatException e2) {
                        throw new IllegalStateException(errorMessage, e2);
                    } catch (IllegalArgumentException e3) {
                        throw new IllegalStateException(errorMessage, e3);
                    }
                case NetdResponseCode.InterfaceDnsServerInfo:
                    if (cooked.length == 6 && cooked[1].equals("DnsInfo") && cooked[2].equals("servers")) {
                        try {
                            long lifetime = Long.parseLong(cooked[4]);
                            String[] servers = cooked[5].split(",");
                            NetworkManagementService.this.notifyInterfaceDnsServerInfo(cooked[3], lifetime, servers);
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
                    boolean valid = true;
                    for (int i = 4; i + 1 < cooked.length && valid; i += 2) {
                        if (cooked[i].equals("dev")) {
                            if (dev == null) {
                                dev = cooked[i + 1];
                            } else {
                                valid = NetworkManagementService.DBG;
                            }
                        } else if (cooked[i].equals("via")) {
                            if (via == null) {
                                via = cooked[i + 1];
                            } else {
                                valid = NetworkManagementService.DBG;
                            }
                        } else {
                            valid = NetworkManagementService.DBG;
                        }
                    }
                    if (valid) {
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
                default:
                    return NetworkManagementService.DBG;
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
            throw e.rethrowAsParcelableException();
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
        FileInputStream fstream = null;
        ArrayList<String> list = new ArrayList<>();
        try {
            FileInputStream fstream2 = new FileInputStream(filename);
            try {
                DataInputStream in = new DataInputStream(fstream2);
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                while (true) {
                    String s = br.readLine();
                    if (s == null || s.length() == 0) {
                        break;
                    }
                    list.add(s);
                }
                if (fstream2 != null) {
                    try {
                        fstream2.close();
                    } catch (IOException e) {
                    }
                }
            } catch (IOException e2) {
                fstream = fstream2;
                if (fstream != null) {
                    try {
                        fstream.close();
                    } catch (IOException e3) {
                    }
                }
            } catch (Throwable th) {
                th = th;
                fstream = fstream2;
                if (fstream != null) {
                    try {
                        fstream.close();
                    } catch (IOException e4) {
                    }
                }
                throw th;
            }
        } catch (IOException e5) {
        } catch (Throwable th2) {
            th = th2;
        }
        return list;
    }

    public RouteInfo[] getRoutes(String interfaceName) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        ArrayList<RouteInfo> routes = new ArrayList<>();
        for (String s : readRouteList("/proc/net/route")) {
            String[] fields = s.split("\t");
            if (fields.length > 7) {
                String iface = fields[0];
                if (interfaceName.equals(iface)) {
                    String dest = fields[1];
                    String gate = fields[2];
                    String str = fields[3];
                    String mask = fields[7];
                    try {
                        InetAddress destAddr = NetworkUtils.intToInetAddress((int) Long.parseLong(dest, 16));
                        int prefixLength = NetworkUtils.netmaskIntToPrefixLength((int) Long.parseLong(mask, 16));
                        LinkAddress linkAddress = new LinkAddress(destAddr, prefixLength);
                        InetAddress gatewayAddr = NetworkUtils.intToInetAddress((int) Long.parseLong(gate, 16));
                        RouteInfo route = new RouteInfo(linkAddress, gatewayAddr);
                        routes.add(route);
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing route " + s + " : " + e);
                    }
                }
            }
        }
        for (String s2 : readRouteList("/proc/net/ipv6_route")) {
            String[] fields2 = s2.split("\\s+");
            if (fields2.length > 9) {
                String iface2 = fields2[9].trim();
                if (interfaceName.equals(iface2)) {
                    String dest2 = fields2[0];
                    String prefix = fields2[1];
                    String gate2 = fields2[4];
                    try {
                        int prefixLength2 = Integer.parseInt(prefix, 16);
                        InetAddress destAddr2 = NetworkUtils.hexToInet6Address(dest2);
                        LinkAddress linkAddress2 = new LinkAddress(destAddr2, prefixLength2);
                        InetAddress gateAddr = NetworkUtils.hexToInet6Address(gate2);
                        RouteInfo route2 = new RouteInfo(linkAddress2, gateAddr);
                        routes.add(route2);
                    } catch (Exception e2) {
                        Log.e(TAG, "Error parsing route " + s2 + " : " + e2);
                    }
                }
            }
        }
        return (RouteInfo[]) routes.toArray(new RouteInfo[routes.size()]);
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
        Slog.d(TAG, "Shutting down");
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
            Object[] objArr = new Object[1];
            objArr[0] = enable ? "enable" : "disable";
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

    private void modifyNat(String action, String internalInterface, String externalInterface) throws SocketException {
        NativeDaemonConnector.Command cmd = new NativeDaemonConnector.Command("nat", action, internalInterface, externalInterface);
        NetworkInterface internalNetworkInterface = NetworkInterface.getByName(internalInterface);
        if (internalNetworkInterface == null) {
            cmd.appendArg("0");
        } else {
            List<InterfaceAddress> interfaceAddresses = excludeLinkLocal(internalNetworkInterface.getInterfaceAddresses());
            cmd.appendArg(Integer.valueOf(interfaceAddresses.size()));
            for (InterfaceAddress ia : interfaceAddresses) {
                InetAddress addr = NetworkUtils.getNetworkPart(ia.getAddress(), ia.getNetworkPrefixLength());
                cmd.appendArg(addr.getHostAddress() + "/" + ((int) ia.getNetworkPrefixLength()));
            }
        }
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

    public void startAccessPoint(WifiConfiguration wifiConfig, String wlanIface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            wifiFirmwareReload(wlanIface, "AP");
            if (wifiConfig == null) {
                this.mConnector.execute("softap", "set", wlanIface);
            } else {
                NativeDaemonConnector nativeDaemonConnector = this.mConnector;
                Object[] objArr = new Object[7];
                objArr[0] = "set";
                objArr[1] = wlanIface;
                objArr[2] = wifiConfig.SSID;
                objArr[3] = "broadcast";
                objArr[4] = "6";
                objArr[5] = getSecurityType(wifiConfig);
                objArr[6] = getSecurityType(wifiConfig) == "wep128" ? new NativeDaemonConnector.SensitiveArg(wifiConfig.wepKeys[0]) : new NativeDaemonConnector.SensitiveArg(wifiConfig.preSharedKey);
                nativeDaemonConnector.execute("softap", objArr);
            }
            this.mConnector.execute("softap", "startap");
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
                if (wifiConfig.wepKeys[0] != null) {
                    return "wep128";
                }
                return "open";
            case 4:
                return "wpa2-psk";
        }
    }

    public void wifiFirmwareReload(String wlanIface, String mode) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("softap", "fwreload", wlanIface, mode);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void stopAccessPoint(String wlanIface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("softap", "stopap");
            wifiFirmwareReload(wlanIface, "STA");
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void setAccessPoint(WifiConfiguration wifiConfig, String wlanIface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            if (wifiConfig == null) {
                this.mConnector.execute("softap", "set", wlanIface);
                return;
            }
            NativeDaemonConnector nativeDaemonConnector = this.mConnector;
            Object[] objArr = new Object[7];
            objArr[0] = "set";
            objArr[1] = wlanIface;
            objArr[2] = wifiConfig.SSID;
            objArr[3] = "broadcast";
            objArr[4] = Integer.valueOf(wifiConfig.wifiApChannelIndex);
            objArr[5] = getSecurityType(wifiConfig);
            objArr[6] = getSecurityType(wifiConfig) == "wep128" ? new NativeDaemonConnector.SensitiveArg(wifiConfig.wepKeys[0]) : new NativeDaemonConnector.SensitiveArg(wifiConfig.preSharedKey);
            nativeDaemonConnector.execute("softap", objArr);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void addIdleTimer(String iface, int timeout, final int type) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
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
                    this.mNetworkActive = DBG;
                }
                this.mDaemonHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        NetworkManagementService.this.notifyInterfaceClassActivity(type, DataConnectionRealTimeInfo.DC_POWER_STATE_HIGH, SystemClock.elapsedRealtimeNanos(), NetworkManagementService.DBG);
                    }
                });
            } catch (NativeDaemonConnectorException e) {
                throw e.rethrowAsParcelableException();
            }
        }
    }

    public void removeIdleTimer(String iface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
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
                                NetworkManagementService.this.notifyInterfaceClassActivity(params.type, DataConnectionRealTimeInfo.DC_POWER_STATE_LOW, SystemClock.elapsedRealtimeNanos(), NetworkManagementService.DBG);
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
                if (this.mActiveQuotas.containsKey(iface)) {
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
                if (this.mActiveAlerts.containsKey(iface)) {
                    try {
                        this.mConnector.execute("bandwidth", "removeinterfacealert", iface);
                        this.mActiveAlerts.remove(iface);
                    } catch (NativeDaemonConnectorException e) {
                        throw e.rethrowAsParcelableException();
                    }
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

    public void setUidNetworkRules(int uid, boolean rejectOnQuotaInterfaces) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        if (this.mBandwidthControlEnabled) {
            synchronized (this.mQuotaLock) {
                boolean oldRejectOnQuota = this.mUidRejectOnQuota.get(uid, DBG);
                if (oldRejectOnQuota != rejectOnQuotaInterfaces) {
                    try {
                        NativeDaemonConnector nativeDaemonConnector = this.mConnector;
                        Object[] objArr = new Object[2];
                        objArr[0] = rejectOnQuotaInterfaces ? "addnaughtyapps" : "removenaughtyapps";
                        objArr[1] = Integer.valueOf(uid);
                        nativeDaemonConnector.execute("bandwidth", objArr);
                        if (rejectOnQuotaInterfaces) {
                            this.mUidRejectOnQuota.put(uid, true);
                        } else {
                            this.mUidRejectOnQuota.delete(uid);
                        }
                    } catch (NativeDaemonConnectorException e) {
                        throw e.rethrowAsParcelableException();
                    }
                }
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
                        }
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

    public void flushNetworkDnsCache(int netId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("resolver", "flushnet", Integer.valueOf(netId));
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void setFirewallEnabled(boolean enabled) {
        enforceSystemUid();
        try {
            NativeDaemonConnector nativeDaemonConnector = this.mConnector;
            Object[] objArr = new Object[1];
            objArr[0] = enabled ? "enable" : "disable";
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

    public void setFirewallUidRule(int uid, boolean allow) {
        enforceSystemUid();
        Preconditions.checkState(this.mFirewallEnabled);
        String rule = allow ? "allow" : "deny";
        try {
            this.mConnector.execute("firewall", "set_uid_rule", Integer.valueOf(uid), rule);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    private static void enforceSystemUid() {
        int uid = Binder.getCallingUid();
        if (uid != 1000) {
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
        boolean z;
        synchronized (this.mNetworkActivityListeners) {
            z = (this.mNetworkActive || this.mActiveIdleTimers.isEmpty()) ? true : DBG;
        }
        return z;
    }

    private void reportNetworkActive() {
        int length = this.mNetworkActivityListeners.beginBroadcast();
        for (int i = 0; i < length; i++) {
            try {
                this.mNetworkActivityListeners.getBroadcastItem(i).onNetworkActive();
            } catch (RemoteException e) {
            } catch (RuntimeException e2) {
            } catch (Throwable th) {
                this.mNetworkActivityListeners.finishBroadcast();
                throw th;
            }
        }
        this.mNetworkActivityListeners.finishBroadcast();
    }

    public boolean getIPv6ForwardingEnabled() throws IllegalStateException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            NativeDaemonEvent event = this.mConnector.execute("ipv6fwd", "status");
            event.checkCode(NetdResponseCode.IPv6FwdStatusResult);
            return event.getMessage().endsWith("enabled");
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void setIPv6ForwardingEnabled(boolean enable) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            NativeDaemonConnector nativeDaemonConnector = this.mConnector;
            Object[] objArr = new Object[1];
            objArr[0] = enable ? "enable" : "disable";
            nativeDaemonConnector.execute("ipv6fwd", objArr);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void startIPv6Tethering(String downstreamIface, String upstreamIface, String address) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("v6tether", "start", downstreamIface, upstreamIface, address);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void stopIPv6Tethering(String downstreamIface, String upstreamIface, String address) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("v6tether", "stop", downstreamIface, upstreamIface, address);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public boolean isIPv6TetheringStarted() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            NativeDaemonEvent event = this.mConnector.execute("v6tether", "status");
            event.checkCode(NetdResponseCode.V6TetherStatusResult);
            return event.getMessage().endsWith("started");
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    @Override
    public void monitor() {
        if (this.mConnector != null) {
            this.mConnector.monitor();
        }
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
        }
        synchronized (this.mUidRejectOnQuota) {
            pw.print("UID reject on quota ifaces: [");
            int size = this.mUidRejectOnQuota.size();
            for (int i = 0; i < size; i++) {
                pw.print(this.mUidRejectOnQuota.keyAt(i));
                if (i < size - 1) {
                    pw.print(",");
                }
            }
            pw.println("]");
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
    }

    public void createPhysicalNetwork(int netId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("network", "create", Integer.valueOf(netId));
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
}
