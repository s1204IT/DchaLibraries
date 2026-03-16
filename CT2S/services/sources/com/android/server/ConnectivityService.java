package com.android.server;

import android.R;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.IConnectivityManager;
import android.net.INetworkManagementEventObserver;
import android.net.INetworkPolicyListener;
import android.net.INetworkPolicyManager;
import android.net.INetworkStatsService;
import android.net.LinkProperties;
import android.net.MobileDataStateTracker;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkConfig;
import android.net.NetworkInfo;
import android.net.NetworkMisc;
import android.net.NetworkQuotaInfo;
import android.net.NetworkRequest;
import android.net.NetworkState;
import android.net.NetworkStateTracker;
import android.net.NetworkUtils;
import android.net.ProxyInfo;
import android.net.RouteInfo;
import android.net.SamplingDataTracker;
import android.net.UidRange;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.FileUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.security.KeyStore;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.Xml;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.app.IBatteryStats;
import com.android.internal.net.LegacyVpnInfo;
import com.android.internal.net.NetworkStatsFactory;
import com.android.internal.net.VpnConfig;
import com.android.internal.net.VpnProfile;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.XmlUtils;
import com.android.server.am.BatteryStatsService;
import com.android.server.connectivity.DataConnectionStats;
import com.android.server.connectivity.Nat464Xlat;
import com.android.server.connectivity.NetworkAgentInfo;
import com.android.server.connectivity.NetworkMonitor;
import com.android.server.connectivity.PacManager;
import com.android.server.connectivity.PermissionMonitor;
import com.android.server.connectivity.Tethering;
import com.android.server.connectivity.Vpn;
import com.android.server.net.BaseNetworkObserver;
import com.android.server.net.LockdownVpnTracker;
import com.google.android.collect.Lists;
import com.google.android.collect.Sets;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class ConnectivityService extends IConnectivityManager.Stub implements PendingIntent.OnFinished {
    private static final String ACTION_PKT_CNT_SAMPLE_INTERVAL_ELAPSED = "android.net.ConnectivityService.action.PKT_CNT_SAMPLE_INTERVAL_ELAPSED";
    private static final String ATTR_MCC = "mcc";
    private static final String ATTR_MNC = "mnc";
    private static final boolean DBG = true;
    private static final int DEFAULT_FAIL_FAST_TIME_MS = 60000;
    private static final int DEFAULT_SAMPLING_INTERVAL_IN_SECONDS = 720;
    private static final int DEFAULT_START_SAMPLING_INTERVAL_IN_SECONDS = 60;
    private static final String DEFAULT_TCP_BUFFER_SIZES = "4096,87380,110208,4096,16384,110208";
    private static final int DISABLED = 0;
    private static final int ENABLED = 1;
    private static final int EVENT_APPLY_GLOBAL_HTTP_PROXY = 9;
    private static final int EVENT_CHANGE_MOBILE_DATA_ENABLED = 2;
    private static final int EVENT_CLEAR_NET_TRANSITION_WAKELOCK = 8;
    private static final int EVENT_ENABLE_FAIL_FAST_MOBILE_DATA = 14;
    private static final int EVENT_EXPIRE_NET_TRANSITION_WAKELOCK = 24;
    private static final int EVENT_PROXY_HAS_CHANGED = 16;
    private static final int EVENT_REGISTER_NETWORK_AGENT = 18;
    private static final int EVENT_REGISTER_NETWORK_FACTORY = 17;
    private static final int EVENT_REGISTER_NETWORK_LISTENER = 21;
    private static final int EVENT_REGISTER_NETWORK_REQUEST = 19;
    private static final int EVENT_REGISTER_NETWORK_REQUEST_WITH_INTENT = 26;
    private static final int EVENT_RELEASE_NETWORK_REQUEST = 22;
    private static final int EVENT_RELEASE_NETWORK_REQUEST_WITH_INTENT = 27;
    private static final int EVENT_SAMPLE_INTERVAL_ELAPSED = 15;
    private static final int EVENT_SEND_STICKY_BROADCAST_INTENT = 11;
    private static final int EVENT_SET_DEPENDENCY_MET = 10;
    private static final int EVENT_SYSTEM_READY = 25;
    private static final int EVENT_TIMEOUT_NETWORK_REQUEST = 20;
    private static final int EVENT_UNREGISTER_NETWORK_FACTORY = 23;
    private static final String FAIL_FAST_TIME_MS = "persist.radio.fail_fast_time_ms";
    private static final int INET_CONDITION_LOG_MAX_SIZE = 15;
    private static final boolean LOGD_RULES = false;
    private static final int MAX_NET_ID = 65535;
    private static final int MIN_NET_ID = 100;
    private static final String NETWORK_RESTORE_DELAY_PROP_NAME = "android.telephony.apn-restore";
    private static final String NOTIFICATION_ID = "CaptivePortal.Notification";
    private static final int PROVISIONING = 2;
    private static final String PROVISIONING_URL_PATH = "/data/misc/radio/provisioning_urls.xml";
    private static final int REDIRECTED_PROVISIONING = 1;
    private static final int RESTORE_DEFAULT_NETWORK_DELAY = 60000;
    private static final boolean SAMPLE_DBG = false;
    private static final int SAMPLE_INTERVAL_ELAPSED_REQUEST_CODE = 0;
    private static final String TAG = "ConnectivityService";
    private static final String TAG_PROVISIONING_URL = "provisioningUrl";
    private static final String TAG_PROVISIONING_URLS = "provisioningUrls";
    private static final String TAG_REDIRECTED_URL = "redirectedUrl";
    private static final boolean VDBG = false;
    private static ConnectivityService sServiceInstance;
    AlarmManager mAlarmManager;
    private Context mContext;
    private String mCurrentTcpBufferSizes;
    private DataConnectionStats mDataConnectionStats;
    private InetAddress mDefaultDns;
    private final NetworkRequest mDefaultRequest;
    private final InternalHandler mHandler;
    private ArrayList mInetLog;
    private Intent mInitialBroadcast;
    private KeyStore mKeyStore;
    private boolean mLockdownEnabled;
    private LockdownVpnTracker mLockdownTracker;
    NetworkConfig[] mNetConfigs;
    private NetworkStateTracker[] mNetTrackers;
    private PowerManager.WakeLock mNetTransitionWakeLock;
    private int mNetTransitionWakeLockSerialNumber;
    private int mNetTransitionWakeLockTimeout;
    private INetworkManagementService mNetd;
    private int mNetworkPreference;
    int mNetworksDefined;
    private int mNumDnsEntries;
    private PacManager mPacManager;
    private final PowerManager.WakeLock mPendingIntentWakeLock;
    private final PermissionMonitor mPermissionMonitor;
    private INetworkPolicyManager mPolicyManager;
    List mProtectedNetworks;
    private final int mReleasePendingIntentDelayMs;
    private PendingIntent mSampleIntervalElapsedIntent;
    private SettingsObserver mSettingsObserver;
    private INetworkStatsService mStatsService;
    private boolean mSystemReady;
    TelephonyManager mTelephonyManager;
    private boolean mTestMode;
    private Tethering mTethering;
    private final NetworkStateTrackerHandler mTrackerHandler;
    private UserManager mUserManager;

    @GuardedBy("mVpns")
    private final SparseArray<Vpn> mVpns = new SparseArray<>();
    private Object mRulesLock = new Object();
    private SparseIntArray mUidRules = new SparseIntArray();
    private HashSet<String> mMeteredIfaces = Sets.newHashSet();
    private int mDefaultInetConditionPublished = 0;
    private Object mDnsLock = new Object();
    private String mNetTransitionWakeLockCausedBy = "";
    private volatile ProxyInfo mDefaultProxy = null;
    private Object mProxyLock = new Object();
    private boolean mDefaultProxyDisabled = false;
    private ProxyInfo mGlobalProxy = null;
    private AtomicInteger mEnableFailFastMobileDataTag = new AtomicInteger(0);
    private int mNextNetId = 100;
    private int mNextNetworkRequestId = 1;
    private LegacyTypeTracker mLegacyTypeTracker = new LegacyTypeTracker();
    private INetworkManagementEventObserver mDataActivityObserver = new BaseNetworkObserver() {
        public void interfaceClassDataActivityChanged(String label, boolean active, long tsNanos) {
            int deviceType = Integer.parseInt(label);
            ConnectivityService.this.sendDataActivityBroadcast(deviceType, active, tsNanos);
        }
    };
    private INetworkPolicyListener mPolicyListener = new INetworkPolicyListener.Stub() {
        public void onUidRulesChanged(int uid, int uidRules) {
            synchronized (ConnectivityService.this.mRulesLock) {
                int oldRules = ConnectivityService.this.mUidRules.get(uid, 0);
                if (oldRules != uidRules) {
                    ConnectivityService.this.mUidRules.put(uid, uidRules);
                }
            }
        }

        public void onMeteredIfacesChanged(String[] meteredIfaces) {
            synchronized (ConnectivityService.this.mRulesLock) {
                ConnectivityService.this.mMeteredIfaces.clear();
                for (String iface : meteredIfaces) {
                    ConnectivityService.this.mMeteredIfaces.add(iface);
                }
            }
        }

        public void onRestrictBackgroundChanged(boolean restrictBackground) {
        }
    };
    private BroadcastReceiver mUserPresentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ConnectivityService.this.updateLockdownVpn()) {
                ConnectivityService.this.mContext.unregisterReceiver(this);
            }
        }
    };
    private volatile boolean mIsNotificationVisible = false;
    private final File mProvisioningUrlFile = new File(PROVISIONING_URL_PATH);
    private BroadcastReceiver mUserIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int userId = intent.getIntExtra("android.intent.extra.user_handle", -10000);
            if (userId != -10000) {
                if ("android.intent.action.USER_STARTING".equals(action)) {
                    ConnectivityService.this.onUserStart(userId);
                } else if ("android.intent.action.USER_STOPPING".equals(action)) {
                    ConnectivityService.this.onUserStop(userId);
                }
            }
        }
    };
    private final HashMap<Messenger, NetworkFactoryInfo> mNetworkFactoryInfos = new HashMap<>();
    private final HashMap<NetworkRequest, NetworkRequestInfo> mNetworkRequests = new HashMap<>();
    private final SparseArray<NetworkAgentInfo> mNetworkForRequestId = new SparseArray<>();
    private final SparseArray<NetworkAgentInfo> mNetworkForNetId = new SparseArray<>();
    private final HashMap<Messenger, NetworkAgentInfo> mNetworkAgentInfos = new HashMap<>();

    private enum NascentState {
        JUST_VALIDATED,
        NOT_JUST_VALIDATED
    }

    private enum ReapUnvalidatedNetworks {
        REAP,
        DONT_REAP
    }

    private class LegacyTypeTracker {
        private static final boolean DBG = true;
        private static final String TAG = "CSLegacyTypeTracker";
        private static final boolean VDBG = false;
        private ArrayList<NetworkAgentInfo>[] mTypeLists = new ArrayList[18];

        public LegacyTypeTracker() {
        }

        public void addSupportedType(int type) {
            if (this.mTypeLists[type] != null) {
                throw new IllegalStateException("legacy list for type " + type + "already initialized");
            }
            this.mTypeLists[type] = new ArrayList<>();
        }

        public boolean isTypeSupported(int type) {
            if (!ConnectivityManager.isNetworkTypeValid(type) || this.mTypeLists[type] == null) {
                return false;
            }
            return DBG;
        }

        public NetworkAgentInfo getNetworkForType(int type) {
            if (!isTypeSupported(type) || this.mTypeLists[type].isEmpty()) {
                return null;
            }
            return this.mTypeLists[type].get(0);
        }

        private void maybeLogBroadcast(NetworkAgentInfo nai, boolean connected, int type) {
            log("Sending " + (connected ? "connected" : "disconnected") + " broadcast for type " + type + " " + nai.name() + " isDefaultNetwork=" + ConnectivityService.this.isDefaultNetwork(nai));
        }

        public void add(int type, NetworkAgentInfo nai) {
            if (isTypeSupported(type)) {
                ArrayList<NetworkAgentInfo> list = this.mTypeLists[type];
                if (list.contains(nai)) {
                    ConnectivityService.loge("Attempting to register duplicate agent for type " + type + ": " + nai);
                    return;
                }
                list.add(nai);
                if (list.size() == 1 || ConnectivityService.this.isDefaultNetwork(nai)) {
                    maybeLogBroadcast(nai, DBG, type);
                    ConnectivityService.this.sendLegacyNetworkBroadcast(nai, DBG, type);
                }
            }
        }

        public void remove(int type, NetworkAgentInfo nai) {
            ArrayList<NetworkAgentInfo> list = this.mTypeLists[type];
            if (list != null && !list.isEmpty()) {
                boolean wasFirstNetwork = list.get(0).equals(nai);
                if (list.remove(nai)) {
                    if (wasFirstNetwork || ConnectivityService.this.isDefaultNetwork(nai)) {
                        maybeLogBroadcast(nai, false, type);
                        ConnectivityService.this.sendLegacyNetworkBroadcast(nai, false, type);
                    }
                    if (!list.isEmpty() && wasFirstNetwork) {
                        log("Other network available for type " + type + ", sending connected broadcast");
                        maybeLogBroadcast(list.get(0), false, type);
                        ConnectivityService.this.sendLegacyNetworkBroadcast(list.get(0), false, type);
                    }
                }
            }
        }

        public void remove(NetworkAgentInfo nai) {
            for (int type = 0; type < this.mTypeLists.length; type++) {
                remove(type, nai);
            }
        }

        private String naiToString(NetworkAgentInfo nai) {
            String name = nai != null ? nai.name() : "null";
            String state = nai.networkInfo != null ? nai.networkInfo.getState() + "/" + nai.networkInfo.getDetailedState() : "???/???";
            return name + " " + state;
        }

        public void dump(IndentingPrintWriter pw) {
            for (int type = 0; type < this.mTypeLists.length; type++) {
                if (this.mTypeLists[type] != null) {
                    pw.print(type + " ");
                    pw.increaseIndent();
                    if (this.mTypeLists[type].size() == 0) {
                        pw.println("none");
                    }
                    for (NetworkAgentInfo nai : this.mTypeLists[type]) {
                        pw.println(naiToString(nai));
                    }
                    pw.decreaseIndent();
                }
            }
        }

        private void log(String s) {
            Slog.d(TAG, s);
        }
    }

    public ConnectivityService(Context context, INetworkManagementService netManager, INetworkStatsService statsService, INetworkPolicyManager policyManager) {
        String id;
        this.mPacManager = null;
        log("ConnectivityService starting up");
        NetworkCapabilities netCap = new NetworkCapabilities();
        netCap.addCapability(12);
        netCap.addCapability(13);
        this.mDefaultRequest = new NetworkRequest(netCap, -1, nextNetworkRequestId());
        NetworkRequestInfo nri = new NetworkRequestInfo(null, this.mDefaultRequest, new Binder(), DBG);
        this.mNetworkRequests.put(this.mDefaultRequest, nri);
        HandlerThread handlerThread = new HandlerThread("ConnectivityServiceThread");
        handlerThread.start();
        this.mHandler = new InternalHandler(handlerThread.getLooper());
        this.mTrackerHandler = new NetworkStateTrackerHandler(handlerThread.getLooper());
        if (TextUtils.isEmpty(SystemProperties.get("net.hostname")) && (id = Settings.Secure.getString(context.getContentResolver(), "android_id")) != null && id.length() > 0) {
            String name = new String("android-").concat(id);
            SystemProperties.set("net.hostname", name);
        }
        String dns = Settings.Global.getString(context.getContentResolver(), "default_dns_server");
        dns = (dns == null || dns.length() == 0) ? context.getResources().getString(R.string.config_systemShell) : dns;
        try {
            this.mDefaultDns = NetworkUtils.numericToInetAddress(dns);
        } catch (IllegalArgumentException e) {
            loge("Error setting defaultDns using " + dns);
        }
        this.mReleasePendingIntentDelayMs = Settings.Secure.getInt(context.getContentResolver(), "connectivity_release_pending_intent_delay_ms", 5000);
        this.mContext = (Context) checkNotNull(context, "missing Context");
        this.mNetd = (INetworkManagementService) checkNotNull(netManager, "missing INetworkManagementService");
        this.mStatsService = (INetworkStatsService) checkNotNull(statsService, "missing INetworkStatsService");
        this.mPolicyManager = (INetworkPolicyManager) checkNotNull(policyManager, "missing INetworkPolicyManager");
        this.mKeyStore = KeyStore.getInstance();
        this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        try {
            this.mPolicyManager.registerListener(this.mPolicyListener);
        } catch (RemoteException e2) {
            loge("unable to register INetworkPolicyListener" + e2.toString());
        }
        PowerManager powerManager = (PowerManager) context.getSystemService("power");
        this.mNetTransitionWakeLock = powerManager.newWakeLock(1, TAG);
        this.mNetTransitionWakeLockTimeout = this.mContext.getResources().getInteger(R.integer.button_pressed_animation_delay);
        this.mPendingIntentWakeLock = powerManager.newWakeLock(1, TAG);
        this.mNetTrackers = new NetworkStateTracker[18];
        this.mNetConfigs = new NetworkConfig[18];
        boolean wifiOnly = SystemProperties.getBoolean("ro.radio.noril", false);
        log("wifiOnly=" + wifiOnly);
        String[] naStrings = context.getResources().getStringArray(R.array.config_ambientBrighteningThresholds);
        for (String naString : naStrings) {
            try {
                NetworkConfig n = new NetworkConfig(naString);
                if (n.type > 17) {
                    loge("Error in networkAttributes - ignoring attempt to define type " + n.type);
                } else if (wifiOnly && ConnectivityManager.isNetworkTypeMobile(n.type)) {
                    log("networkAttributes - ignoring mobile as this dev is wifiOnly " + n.type);
                } else if (this.mNetConfigs[n.type] != null) {
                    loge("Error in networkAttributes - ignoring attempt to redefine type " + n.type);
                } else {
                    this.mLegacyTypeTracker.addSupportedType(n.type);
                    this.mNetConfigs[n.type] = n;
                    this.mNetworksDefined++;
                }
            } catch (Exception e3) {
            }
        }
        if (this.mNetConfigs[17] == null) {
            this.mLegacyTypeTracker.addSupportedType(17);
            this.mNetworksDefined++;
        }
        this.mProtectedNetworks = new ArrayList();
        int[] protectedNetworks = context.getResources().getIntArray(R.array.config_ambientDarkeningThresholds);
        for (int p : protectedNetworks) {
            if (this.mNetConfigs[p] != null && !this.mProtectedNetworks.contains(Integer.valueOf(p))) {
                this.mProtectedNetworks.add(Integer.valueOf(p));
            } else {
                loge("Ignoring protectedNetwork " + p);
            }
        }
        this.mTestMode = (SystemProperties.get("cm.test.mode").equals("true") && SystemProperties.get("ro.build.type").equals("eng")) ? DBG : false;
        this.mTethering = new Tethering(this.mContext, this.mNetd, statsService, this.mHandler.getLooper());
        this.mPermissionMonitor = new PermissionMonitor(this.mContext, this.mNetd);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_STARTING");
        intentFilter.addAction("android.intent.action.USER_STOPPING");
        this.mContext.registerReceiverAsUser(this.mUserIntentReceiver, UserHandle.ALL, intentFilter, null, null);
        try {
            this.mNetd.registerObserver(this.mTethering);
            this.mNetd.registerObserver(this.mDataActivityObserver);
        } catch (RemoteException e4) {
            loge("Error registering observer :" + e4);
        }
        this.mInetLog = new ArrayList();
        this.mSettingsObserver = new SettingsObserver(this.mHandler, 9);
        this.mSettingsObserver.observe(this.mContext);
        this.mDataConnectionStats = new DataConnectionStats(this.mContext);
        this.mDataConnectionStats.startMonitoring();
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_PKT_CNT_SAMPLE_INTERVAL_ELAPSED);
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if (action.equals(ConnectivityService.ACTION_PKT_CNT_SAMPLE_INTERVAL_ELAPSED)) {
                    ConnectivityService.this.mHandler.sendMessage(ConnectivityService.this.mHandler.obtainMessage(15));
                }
            }
        }, new IntentFilter(filter));
        this.mPacManager = new PacManager(this.mContext, this.mHandler, 16);
        this.mUserManager = (UserManager) context.getSystemService("user");
    }

    private synchronized int nextNetworkRequestId() {
        int i;
        i = this.mNextNetworkRequestId;
        this.mNextNetworkRequestId = i + 1;
        return i;
    }

    private void assignNextNetId(NetworkAgentInfo nai) {
        synchronized (this.mNetworkForNetId) {
            for (int i = 100; i <= MAX_NET_ID; i++) {
                int netId = this.mNextNetId;
                int i2 = this.mNextNetId + 1;
                this.mNextNetId = i2;
                if (i2 > MAX_NET_ID) {
                    this.mNextNetId = 100;
                }
                if (this.mNetworkForNetId.get(netId) == null) {
                    nai.network = new Network(netId);
                    this.mNetworkForNetId.put(netId, nai);
                    return;
                }
            }
            throw new IllegalStateException("No free netIds");
        }
    }

    private boolean teardown(NetworkStateTracker netTracker) {
        if (!netTracker.teardown()) {
            return false;
        }
        netTracker.setTeardownRequested(DBG);
        return DBG;
    }

    private android.net.NetworkState getFilteredNetworkState(int r14, int r15) throws java.lang.Throwable {
        r1 = null;
        r2 = null;
        r3 = null;
        r4 = null;
        r5 = null;
        if (r13.mLegacyTypeTracker.isTypeSupported(r14)) {
            r9 = r13.mLegacyTypeTracker.getNetworkForType(r14);
            if (r9 != null) {
                synchronized (r9) {
                    ;
                    r7 = new android.net.NetworkInfo(r9.networkInfo);
                    r8 = new android.net.LinkProperties(r9.linkProperties);
                    r10 = new android.net.NetworkCapabilities(r9.networkCapabilities);
                    r11 = new android.net.Network(r9.network);
                    if (r9.networkMisc != null) {
                        r5 = r9.networkMisc.subscriberId;
                    } else {
                        r5 = null;
                    }
                    r7.setType(r14);
                    r4 = r11;
                    r3 = r10;
                    r2 = r8;
                    r1 = r7;
                }
            } else {
                r1 = new android.net.NetworkInfo(r14, 0, android.net.ConnectivityManager.getNetworkTypeName(r14), "");
                r1.setDetailedState(android.net.NetworkInfo.DetailedState.DISCONNECTED, null, null);
                r1.setIsAvailable(com.android.server.ConnectivityService.DBG);
                r2 = new android.net.LinkProperties();
                r3 = new android.net.NetworkCapabilities();
                r4 = null;
            }
            r1 = getFilteredNetworkInfo(r1, r2, r15);
        }
        return new android.net.NetworkState(r1, r2, r3, r4, r5, null);
    }

    private NetworkAgentInfo getNetworkAgentInfoForNetwork(Network network) {
        NetworkAgentInfo networkAgentInfo;
        if (network == null) {
            return null;
        }
        synchronized (this.mNetworkForNetId) {
            networkAgentInfo = this.mNetworkForNetId.get(network.netId);
        }
        return networkAgentInfo;
    }

    private Network[] getVpnUnderlyingNetworks(int uid) {
        if (!this.mLockdownEnabled) {
            int user = UserHandle.getUserId(uid);
            synchronized (this.mVpns) {
                Vpn vpn = this.mVpns.get(user);
                if (vpn != null && vpn.appliesToUid(uid)) {
                    return vpn.getUnderlyingNetworks();
                }
            }
        }
        return null;
    }

    private android.net.NetworkState getUnfilteredActiveNetworkState(int r15) throws java.lang.Throwable {
        r1 = null;
        r2 = null;
        r3 = null;
        r4 = null;
        r5 = null;
        r9 = r14.mNetworkForRequestId.get(r14.mDefaultRequest.requestId);
        r12 = getVpnUnderlyingNetworks(r15);
        if (r12 != null) {
            if (r12.length > 0) {
                r9 = getNetworkAgentInfoForNetwork(r12[0]);
            } else {
                r9 = null;
            }
        }
        if (r9 != null) {
            synchronized (r9) {
                ;
                r7 = new android.net.NetworkInfo(r9.networkInfo);
                r8 = new android.net.LinkProperties(r9.linkProperties);
                r10 = new android.net.NetworkCapabilities(r9.networkCapabilities);
                r11 = new android.net.Network(r9.network);
                if (r9.networkMisc != null) {
                    r5 = r9.networkMisc.subscriberId;
                } else {
                    r5 = null;
                }
                r4 = r11;
                r3 = r10;
                r2 = r8;
                r1 = r7;
            }
        }
        return new android.net.NetworkState(r1, r2, r3, r4, r5, null);
    }

    private boolean isNetworkWithLinkPropertiesBlocked(LinkProperties lp, int uid) {
        boolean networkCostly;
        int uidRules;
        String iface = lp == null ? "" : lp.getInterfaceName();
        synchronized (this.mRulesLock) {
            networkCostly = this.mMeteredIfaces.contains(iface);
            uidRules = this.mUidRules.get(uid, 0);
        }
        if (!networkCostly || (uidRules & 1) == 0) {
            return false;
        }
        return DBG;
    }

    private NetworkInfo getFilteredNetworkInfo(NetworkInfo info, LinkProperties lp, int uid) {
        if (info != null && isNetworkWithLinkPropertiesBlocked(lp, uid)) {
            NetworkInfo info2 = new NetworkInfo(info);
            info2.setDetailedState(NetworkInfo.DetailedState.BLOCKED, null, null);
            log("returning Blocked NetworkInfo for ifname=" + lp.getInterfaceName() + ", uid=" + uid);
            info = info2;
        }
        if (info != null && this.mLockdownTracker != null) {
            NetworkInfo info3 = this.mLockdownTracker.augmentNetworkInfo(info);
            log("returning Locked NetworkInfo");
            return info3;
        }
        return info;
    }

    public NetworkInfo getActiveNetworkInfo() throws Throwable {
        enforceAccessPermission();
        int uid = Binder.getCallingUid();
        NetworkState state = getUnfilteredActiveNetworkState(uid);
        return getFilteredNetworkInfo(state.networkInfo, state.linkProperties, uid);
    }

    private NetworkInfo getProvisioningNetworkInfo() throws Throwable {
        enforceAccessPermission();
        NetworkInfo provNi = null;
        NetworkInfo[] arr$ = getAllNetworkInfo();
        int len$ = arr$.length;
        int i$ = 0;
        while (true) {
            if (i$ >= len$) {
                break;
            }
            NetworkInfo ni = arr$[i$];
            if (!ni.isConnectedToProvisioningNetwork()) {
                i$++;
            } else {
                provNi = ni;
                break;
            }
        }
        log("getProvisioningNetworkInfo: X provNi=" + provNi);
        return provNi;
    }

    public NetworkInfo getProvisioningOrActiveNetworkInfo() throws Throwable {
        enforceAccessPermission();
        NetworkInfo provNi = getProvisioningNetworkInfo();
        if (provNi == null) {
            provNi = getActiveNetworkInfo();
        }
        log("getProvisioningOrActiveNetworkInfo: X provNi=" + provNi);
        return provNi;
    }

    public NetworkInfo getActiveNetworkInfoUnfiltered() {
        enforceAccessPermission();
        int uid = Binder.getCallingUid();
        NetworkState state = getUnfilteredActiveNetworkState(uid);
        return state.networkInfo;
    }

    public NetworkInfo getActiveNetworkInfoForUid(int uid) throws Throwable {
        enforceConnectivityInternalPermission();
        NetworkState state = getUnfilteredActiveNetworkState(uid);
        return getFilteredNetworkInfo(state.networkInfo, state.linkProperties, uid);
    }

    public NetworkInfo getNetworkInfo(int networkType) throws Throwable {
        enforceAccessPermission();
        int uid = Binder.getCallingUid();
        if (getVpnUnderlyingNetworks(uid) != null) {
            NetworkState state = getUnfilteredActiveNetworkState(uid);
            if (state.networkInfo != null && state.networkInfo.getType() == networkType) {
                return getFilteredNetworkInfo(state.networkInfo, state.linkProperties, uid);
            }
        }
        return getFilteredNetworkState(networkType, uid).networkInfo;
    }

    public NetworkInfo getNetworkInfoForNetwork(Network network) throws Throwable {
        enforceAccessPermission();
        int uid = Binder.getCallingUid();
        NetworkInfo info = null;
        NetworkAgentInfo nai = getNetworkAgentInfoForNetwork(network);
        if (nai != null) {
            synchronized (nai) {
                try {
                    NetworkInfo info2 = new NetworkInfo(nai.networkInfo);
                    try {
                        info = getFilteredNetworkInfo(info2, nai.linkProperties, uid);
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            }
        }
        return info;
    }

    public NetworkInfo[] getAllNetworkInfo() throws Throwable {
        enforceAccessPermission();
        ArrayList<NetworkInfo> result = Lists.newArrayList();
        for (int networkType = 0; networkType <= 17; networkType++) {
            NetworkInfo info = getNetworkInfo(networkType);
            if (info != null) {
                result.add(info);
            }
        }
        return (NetworkInfo[]) result.toArray(new NetworkInfo[result.size()]);
    }

    public Network getNetworkForType(int networkType) throws Throwable {
        enforceAccessPermission();
        int uid = Binder.getCallingUid();
        NetworkState state = getFilteredNetworkState(networkType, uid);
        if (isNetworkWithLinkPropertiesBlocked(state.linkProperties, uid)) {
            return null;
        }
        return state.network;
    }

    public Network[] getAllNetworks() {
        enforceAccessPermission();
        ArrayList<Network> result = new ArrayList<>();
        synchronized (this.mNetworkForNetId) {
            for (int i = 0; i < this.mNetworkForNetId.size(); i++) {
                result.add(new Network(this.mNetworkForNetId.valueAt(i).network));
            }
        }
        return (Network[]) result.toArray(new Network[result.size()]);
    }

    private NetworkCapabilities getNetworkCapabilitiesAndValidation(NetworkAgentInfo nai) {
        if (nai != null) {
            synchronized (nai) {
                if (nai.created) {
                    NetworkCapabilities nc = new NetworkCapabilities(nai.networkCapabilities);
                    if (nai.lastValidated) {
                        nc.addCapability(16);
                    } else {
                        nc.removeCapability(16);
                    }
                    return nc;
                }
            }
        }
        return null;
    }

    public NetworkCapabilities[] getDefaultNetworkCapabilitiesForUser(int userId) {
        Network[] networks;
        enforceAccessPermission();
        HashMap<Network, NetworkCapabilities> result = new HashMap<>();
        NetworkAgentInfo nai = getDefaultNetwork();
        NetworkCapabilities nc = getNetworkCapabilitiesAndValidation(getDefaultNetwork());
        if (nc != null) {
            result.put(nai.network, nc);
        }
        if (!this.mLockdownEnabled) {
            synchronized (this.mVpns) {
                Vpn vpn = this.mVpns.get(userId);
                if (vpn != null && (networks = vpn.getUnderlyingNetworks()) != null) {
                    for (Network network : networks) {
                        NetworkAgentInfo nai2 = getNetworkAgentInfoForNetwork(network);
                        NetworkCapabilities nc2 = getNetworkCapabilitiesAndValidation(nai2);
                        if (nc2 != null) {
                            result.put(nai2.network, nc2);
                        }
                    }
                }
            }
        }
        NetworkCapabilities[] out = new NetworkCapabilities[result.size()];
        return (NetworkCapabilities[]) result.values().toArray(out);
    }

    public boolean isNetworkSupported(int networkType) {
        enforceAccessPermission();
        return this.mLegacyTypeTracker.isTypeSupported(networkType);
    }

    public LinkProperties getActiveLinkProperties() {
        enforceAccessPermission();
        int uid = Binder.getCallingUid();
        NetworkState state = getUnfilteredActiveNetworkState(uid);
        return state.linkProperties;
    }

    public LinkProperties getLinkPropertiesForType(int networkType) {
        LinkProperties linkProperties;
        enforceAccessPermission();
        NetworkAgentInfo nai = this.mLegacyTypeTracker.getNetworkForType(networkType);
        if (nai != null) {
            synchronized (nai) {
                linkProperties = new LinkProperties(nai.linkProperties);
            }
            return linkProperties;
        }
        return null;
    }

    public LinkProperties getLinkProperties(Network network) {
        LinkProperties linkProperties;
        enforceAccessPermission();
        NetworkAgentInfo nai = getNetworkAgentInfoForNetwork(network);
        if (nai != null) {
            synchronized (nai) {
                linkProperties = new LinkProperties(nai.linkProperties);
            }
            return linkProperties;
        }
        return null;
    }

    public NetworkCapabilities getNetworkCapabilities(Network network) {
        NetworkCapabilities networkCapabilities;
        enforceAccessPermission();
        NetworkAgentInfo nai = getNetworkAgentInfoForNetwork(network);
        if (nai != null) {
            synchronized (nai) {
                networkCapabilities = new NetworkCapabilities(nai.networkCapabilities);
            }
            return networkCapabilities;
        }
        return null;
    }

    public NetworkState[] getAllNetworkState() {
        enforceConnectivityInternalPermission();
        ArrayList<NetworkState> result = Lists.newArrayList();
        Network[] arr$ = getAllNetworks();
        for (Network network : arr$) {
            NetworkAgentInfo nai = getNetworkAgentInfoForNetwork(network);
            if (nai != null) {
                synchronized (nai) {
                    String subscriberId = nai.networkMisc != null ? nai.networkMisc.subscriberId : null;
                    result.add(new NetworkState(nai.networkInfo, nai.linkProperties, nai.networkCapabilities, network, subscriberId, (String) null));
                }
            }
        }
        return (NetworkState[]) result.toArray(new NetworkState[result.size()]);
    }

    public NetworkQuotaInfo getActiveNetworkQuotaInfo() {
        enforceAccessPermission();
        int uid = Binder.getCallingUid();
        long token = Binder.clearCallingIdentity();
        try {
            NetworkState state = getUnfilteredActiveNetworkState(uid);
            if (state.networkInfo != null) {
                try {
                    return this.mPolicyManager.getNetworkQuotaInfo(state);
                } catch (RemoteException e) {
                }
            }
            return null;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public boolean isActiveNetworkMetered() {
        enforceAccessPermission();
        int uid = Binder.getCallingUid();
        long token = Binder.clearCallingIdentity();
        try {
            return isActiveNetworkMeteredUnchecked(uid);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private boolean isActiveNetworkMeteredUnchecked(int uid) throws Throwable {
        NetworkState state = getUnfilteredActiveNetworkState(uid);
        if (state.networkInfo != null) {
            try {
                return this.mPolicyManager.isNetworkMetered(state);
            } catch (RemoteException e) {
            }
        }
        return false;
    }

    public boolean requestRouteToHostAddress(int networkType, byte[] hostAddress) {
        NetworkInfo.DetailedState netState;
        LinkProperties lp;
        int netId;
        boolean ok = false;
        enforceChangePermission();
        if (this.mProtectedNetworks.contains(Integer.valueOf(networkType))) {
            enforceConnectivityInternalPermission();
        }
        try {
            InetAddress addr = InetAddress.getByAddress(hostAddress);
            if (!ConnectivityManager.isNetworkTypeValid(networkType)) {
                log("requestRouteToHostAddress on invalid network: " + networkType);
            } else {
                NetworkAgentInfo nai = this.mLegacyTypeTracker.getNetworkForType(networkType);
                if (nai == null) {
                    if (!this.mLegacyTypeTracker.isTypeSupported(networkType)) {
                        log("requestRouteToHostAddress on unsupported network: " + networkType);
                    } else {
                        log("requestRouteToHostAddress on down network: " + networkType);
                    }
                } else {
                    synchronized (nai) {
                        netState = nai.networkInfo.getDetailedState();
                    }
                    if (netState == NetworkInfo.DetailedState.CONNECTED || netState == NetworkInfo.DetailedState.CAPTIVE_PORTAL_CHECK) {
                        int uid = Binder.getCallingUid();
                        long token = Binder.clearCallingIdentity();
                        try {
                            synchronized (nai) {
                                lp = nai.linkProperties;
                                netId = nai.network.netId;
                            }
                            ok = addLegacyRouteToHost(lp, addr, netId, uid);
                            log("requestRouteToHostAddress ok=" + ok);
                        } finally {
                            Binder.restoreCallingIdentity(token);
                        }
                    }
                }
            }
        } catch (UnknownHostException e) {
            log("requestRouteToHostAddress got " + e.toString());
        }
        return ok;
    }

    private boolean addLegacyRouteToHost(LinkProperties lp, InetAddress addr, int netId, int uid) {
        RouteInfo bestRoute;
        RouteInfo bestRoute2 = RouteInfo.selectBestRoute(lp.getAllRoutes(), addr);
        if (bestRoute2 == null) {
            bestRoute = RouteInfo.makeHostRoute(addr, lp.getInterfaceName());
        } else {
            String iface = bestRoute2.getInterface();
            if (bestRoute2.getGateway().equals(addr)) {
                bestRoute = RouteInfo.makeHostRoute(addr, iface);
            } else {
                bestRoute = RouteInfo.makeHostRoute(addr, bestRoute2.getGateway(), iface);
            }
        }
        log("Adding " + bestRoute + " for interface " + bestRoute.getInterface());
        try {
            this.mNetd.addLegacyRouteForNetId(netId, bestRoute, uid);
            return DBG;
        } catch (Exception e) {
            loge("Exception trying to add a route: " + e);
            return false;
        }
    }

    public void setDataDependency(int networkType, boolean met) {
        enforceConnectivityInternalPermission();
        this.mHandler.sendMessage(this.mHandler.obtainMessage(10, met ? 1 : 0, networkType));
    }

    private void handleSetDependencyMet(int networkType, boolean met) {
        if (this.mNetTrackers[networkType] != null) {
            log("handleSetDependencyMet(" + networkType + ", " + met + ")");
            this.mNetTrackers[networkType].setDependencyMet(met);
        }
    }

    private void enforceInternetPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.INTERNET", TAG);
    }

    private void enforceAccessPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_NETWORK_STATE", TAG);
    }

    private void enforceChangePermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CHANGE_NETWORK_STATE", TAG);
    }

    private void enforceTetherAccessPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_NETWORK_STATE", TAG);
    }

    private void enforceConnectivityInternalPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
    }

    public void sendConnectedBroadcast(NetworkInfo info) {
        enforceConnectivityInternalPermission();
        sendGeneralBroadcast(info, "android.net.conn.CONNECTIVITY_CHANGE_IMMEDIATE");
        sendGeneralBroadcast(info, "android.net.conn.CONNECTIVITY_CHANGE");
    }

    private void sendInetConditionBroadcast(NetworkInfo info) {
        sendGeneralBroadcast(info, "android.net.conn.INET_CONDITION_ACTION");
    }

    private Intent makeGeneralIntent(NetworkInfo info, String bcastType) {
        if (this.mLockdownTracker != null) {
            info = this.mLockdownTracker.augmentNetworkInfo(info);
        }
        Intent intent = new Intent(bcastType);
        intent.putExtra("networkInfo", new NetworkInfo(info));
        intent.putExtra("networkType", info.getType());
        if (info.isFailover()) {
            intent.putExtra("isFailover", DBG);
            info.setFailover(false);
        }
        if (info.getReason() != null) {
            intent.putExtra("reason", info.getReason());
        }
        if (info.getExtraInfo() != null) {
            intent.putExtra("extraInfo", info.getExtraInfo());
        }
        intent.putExtra("inetCondition", this.mDefaultInetConditionPublished);
        return intent;
    }

    private void sendGeneralBroadcast(NetworkInfo info, String bcastType) {
        sendStickyBroadcast(makeGeneralIntent(info, bcastType));
    }

    private void sendDataActivityBroadcast(int deviceType, boolean active, long tsNanos) {
        Intent intent = new Intent("android.net.conn.DATA_ACTIVITY_CHANGE");
        intent.putExtra("deviceType", deviceType);
        intent.putExtra("isActive", active);
        intent.putExtra("tsNanos", tsNanos);
        long ident = Binder.clearCallingIdentity();
        try {
            this.mContext.sendOrderedBroadcastAsUser(intent, UserHandle.ALL, "android.permission.RECEIVE_DATA_ACTIVITY_CHANGE", null, null, 0, null, null);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void sendStickyBroadcast(Intent intent) {
        synchronized (this) {
            if (!this.mSystemReady) {
                this.mInitialBroadcast = new Intent(intent);
            }
            intent.addFlags(67108864);
            log("sendStickyBroadcast: action=" + intent.getAction());
            long ident = Binder.clearCallingIdentity();
            if ("android.net.conn.CONNECTIVITY_CHANGE".equals(intent.getAction())) {
                IBatteryStats bs = BatteryStatsService.getService();
                try {
                    NetworkInfo ni = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                    bs.noteConnectivityChanged(intent.getIntExtra("networkType", -1), ni != null ? ni.getState().toString() : "?");
                } catch (RemoteException e) {
                }
            }
            try {
                this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    void systemReady() {
        Intent intent = new Intent(ACTION_PKT_CNT_SAMPLE_INTERVAL_ELAPSED);
        intent.setPackage(this.mContext.getPackageName());
        this.mSampleIntervalElapsedIntent = PendingIntent.getBroadcast(this.mContext, 0, intent, 0);
        setAlarm(60000, this.mSampleIntervalElapsedIntent);
        loadGlobalProxy();
        synchronized (this) {
            this.mSystemReady = DBG;
            if (this.mInitialBroadcast != null) {
                this.mContext.sendStickyBroadcastAsUser(this.mInitialBroadcast, UserHandle.ALL);
                this.mInitialBroadcast = null;
            }
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(9));
        if (!updateLockdownVpn()) {
            IntentFilter filter = new IntentFilter("android.intent.action.USER_PRESENT");
            this.mContext.registerReceiver(this.mUserPresentReceiver, filter);
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(25));
        this.mPermissionMonitor.startMonitoring();
    }

    public void captivePortalCheckCompleted(NetworkInfo info, boolean isCaptivePortal) {
        enforceConnectivityInternalPermission();
        log("captivePortalCheckCompleted: ni=" + info + " captive=" + isCaptivePortal);
    }

    private void setupDataActivityTracking(NetworkAgentInfo networkAgent) {
        int timeout;
        String iface = networkAgent.linkProperties.getInterfaceName();
        int type = -1;
        if (networkAgent.networkCapabilities.hasTransport(0)) {
            timeout = Settings.Global.getInt(this.mContext.getContentResolver(), "data_activity_timeout_mobile", 5);
            type = 0;
        } else if (networkAgent.networkCapabilities.hasTransport(1)) {
            timeout = Settings.Global.getInt(this.mContext.getContentResolver(), "data_activity_timeout_wifi", 0);
            type = 1;
        } else {
            timeout = 0;
        }
        if (timeout > 0 && iface != null && type != -1) {
            try {
                this.mNetd.addIdleTimer(iface, timeout, type);
            } catch (Exception e) {
                loge("Exception in setupDataActivityTracking " + e);
            }
        }
    }

    private void removeDataActivityTracking(NetworkAgentInfo networkAgent) {
        String iface = networkAgent.linkProperties.getInterfaceName();
        NetworkCapabilities caps = networkAgent.networkCapabilities;
        if (iface != null) {
            if (caps.hasTransport(0) || caps.hasTransport(1)) {
                try {
                    this.mNetd.removeIdleTimer(iface);
                } catch (Exception e) {
                    loge("Exception in removeDataActivityTracking " + e);
                }
            }
        }
    }

    private void updateMtu(LinkProperties newLp, LinkProperties oldLp) {
        String iface = newLp.getInterfaceName();
        int mtu = newLp.getMtu();
        if (oldLp == null || !newLp.isIdenticalMtu(oldLp)) {
            if (!LinkProperties.isValidMtu(mtu, newLp.hasGlobalIPv6Address())) {
                loge("Unexpected mtu value: " + mtu + ", " + iface);
                return;
            }
            if (TextUtils.isEmpty(iface)) {
                loge("Setting MTU size with null iface.");
                return;
            }
            try {
                log("Setting MTU size: " + iface + ", " + mtu);
                this.mNetd.setMtu(iface, mtu);
            } catch (Exception e) {
                Slog.e(TAG, "exception in setMtu()" + e);
            }
        }
    }

    private void updateTcpBufferSizes(NetworkAgentInfo nai) {
        if (isDefaultNetwork(nai)) {
            String tcpBufferSizes = nai.linkProperties.getTcpBufferSizes();
            String[] values = null;
            if (tcpBufferSizes != null) {
                values = tcpBufferSizes.split(",");
            }
            if (values == null || values.length != 6) {
                log("Invalid tcpBufferSizes string: " + tcpBufferSizes + ", using defaults");
                tcpBufferSizes = DEFAULT_TCP_BUFFER_SIZES;
                values = DEFAULT_TCP_BUFFER_SIZES.split(",");
            }
            if (!tcpBufferSizes.equals(this.mCurrentTcpBufferSizes)) {
                try {
                    Slog.d(TAG, "Setting tx/rx TCP buffers to " + tcpBufferSizes);
                    FileUtils.stringToFile("/sys/kernel/ipv4/tcp_rmem_min", values[0]);
                    FileUtils.stringToFile("/sys/kernel/ipv4/tcp_rmem_def", values[1]);
                    FileUtils.stringToFile("/sys/kernel/ipv4/tcp_rmem_max", values[2]);
                    FileUtils.stringToFile("/sys/kernel/ipv4/tcp_wmem_min", values[3]);
                    FileUtils.stringToFile("/sys/kernel/ipv4/tcp_wmem_def", values[4]);
                    FileUtils.stringToFile("/sys/kernel/ipv4/tcp_wmem_max", values[5]);
                    this.mCurrentTcpBufferSizes = tcpBufferSizes;
                } catch (IOException e) {
                    loge("Can't set TCP buffer sizes:" + e);
                }
                int defaultRwndValue = SystemProperties.getInt("net.tcp.default_init_rwnd", 0);
                Integer rwndValue = Integer.valueOf(Settings.Global.getInt(this.mContext.getContentResolver(), "tcp_default_init_rwnd", defaultRwndValue));
                if (rwndValue.intValue() != 0) {
                    SystemProperties.set("sys.sysctl.tcp_def_init_rwnd", rwndValue.toString());
                }
            }
        }
    }

    private void flushVmDnsCache() {
        Intent intent = new Intent("android.intent.action.CLEAR_DNS_CACHE");
        intent.addFlags(536870912);
        intent.addFlags(67108864);
        long ident = Binder.clearCallingIdentity();
        try {
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public int getRestoreDefaultNetworkDelay(int networkType) {
        String restoreDefaultNetworkDelayStr = SystemProperties.get(NETWORK_RESTORE_DELAY_PROP_NAME);
        if (restoreDefaultNetworkDelayStr != null && restoreDefaultNetworkDelayStr.length() != 0) {
            try {
                return Integer.valueOf(restoreDefaultNetworkDelayStr).intValue();
            } catch (NumberFormatException e) {
            }
        }
        if (networkType > 17 || this.mNetConfigs[networkType] == null) {
            return 60000;
        }
        int ret = this.mNetConfigs[networkType].restoreTime;
        return ret;
    }

    protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        PrintWriter indentingPrintWriter = new IndentingPrintWriter(writer, "  ");
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            indentingPrintWriter.println("Permission Denial: can't dump ConnectivityService from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            return;
        }
        indentingPrintWriter.println("NetworkFactories for:");
        indentingPrintWriter.increaseIndent();
        for (NetworkFactoryInfo nfi : this.mNetworkFactoryInfos.values()) {
            indentingPrintWriter.println(nfi.name);
        }
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println();
        NetworkAgentInfo defaultNai = this.mNetworkForRequestId.get(this.mDefaultRequest.requestId);
        indentingPrintWriter.print("Active default network: ");
        if (defaultNai == null) {
            indentingPrintWriter.println("none");
        } else {
            indentingPrintWriter.println(defaultNai.network.netId);
        }
        indentingPrintWriter.println();
        indentingPrintWriter.println("Current Networks:");
        indentingPrintWriter.increaseIndent();
        for (NetworkAgentInfo nai : this.mNetworkAgentInfos.values()) {
            indentingPrintWriter.println(nai.toString());
            indentingPrintWriter.increaseIndent();
            indentingPrintWriter.println("Requests:");
            indentingPrintWriter.increaseIndent();
            for (int i = 0; i < nai.networkRequests.size(); i++) {
                indentingPrintWriter.println(nai.networkRequests.valueAt(i).toString());
            }
            indentingPrintWriter.decreaseIndent();
            indentingPrintWriter.println("Lingered:");
            indentingPrintWriter.increaseIndent();
            for (NetworkRequest nr : nai.networkLingered) {
                indentingPrintWriter.println(nr.toString());
            }
            indentingPrintWriter.decreaseIndent();
            indentingPrintWriter.decreaseIndent();
        }
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println();
        indentingPrintWriter.println("Network Requests:");
        indentingPrintWriter.increaseIndent();
        for (NetworkRequestInfo nri : this.mNetworkRequests.values()) {
            indentingPrintWriter.println(nri.toString());
        }
        indentingPrintWriter.println();
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println("mLegacyTypeTracker:");
        indentingPrintWriter.increaseIndent();
        this.mLegacyTypeTracker.dump(indentingPrintWriter);
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println();
        synchronized (this) {
            indentingPrintWriter.println("NetworkTransitionWakeLock is currently " + (this.mNetTransitionWakeLock.isHeld() ? "" : "not ") + "held.");
            indentingPrintWriter.println("It was last requested for " + this.mNetTransitionWakeLockCausedBy);
        }
        indentingPrintWriter.println();
        this.mTethering.dump(fd, indentingPrintWriter, args);
        if (this.mInetLog != null) {
            indentingPrintWriter.println();
            indentingPrintWriter.println("Inet condition reports:");
            indentingPrintWriter.increaseIndent();
            for (int i2 = 0; i2 < this.mInetLog.size(); i2++) {
                indentingPrintWriter.println(this.mInetLog.get(i2));
            }
            indentingPrintWriter.decreaseIndent();
        }
    }

    private boolean isLiveNetworkAgent(NetworkAgentInfo nai, String msg) {
        if (nai.network == null) {
            return false;
        }
        NetworkAgentInfo officialNai = getNetworkAgentInfoForNetwork(nai.network);
        if (officialNai != null && officialNai.equals(nai)) {
            return DBG;
        }
        if (officialNai == null) {
            return false;
        }
        loge(msg + " - isLiveNetworkAgent found mismatched netId: " + officialNai + " - " + nai);
        return false;
    }

    private boolean isRequest(NetworkRequest request) {
        return this.mNetworkRequests.get(request).isRequest;
    }

    private class NetworkStateTrackerHandler extends Handler {
        public NetworkStateTrackerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) throws Throwable {
            NetworkAgentInfo nai;
            switch (msg.what) {
                case 69632:
                    ConnectivityService.this.handleAsyncChannelHalfConnect(msg);
                    return;
                case 69635:
                    NetworkAgentInfo nai2 = (NetworkAgentInfo) ConnectivityService.this.mNetworkAgentInfos.get(msg.replyTo);
                    if (nai2 != null) {
                        nai2.asyncChannel.disconnect();
                        return;
                    }
                    return;
                case 69636:
                    ConnectivityService.this.handleAsyncChannelDisconnected(msg);
                    return;
                case 458752:
                    NetworkInfo info = (NetworkInfo) msg.obj;
                    NetworkInfo.State state = info.getState();
                    if (state == NetworkInfo.State.CONNECTED || state == NetworkInfo.State.DISCONNECTED || state == NetworkInfo.State.SUSPENDED) {
                        ConnectivityService.log("ConnectivityChange for " + info.getTypeName() + ": " + state + "/" + info.getDetailedState());
                    }
                    EventLogTags.writeConnectivityStateChanged(info.getType(), info.getSubtype(), info.getDetailedState().ordinal());
                    if (info.isConnectedToProvisioningNetwork()) {
                        LinkProperties lp = ConnectivityService.this.getLinkPropertiesForType(info.getType());
                        ConnectivityService.log("EVENT_STATE_CHANGED: connected to provisioning network, lp=" + lp);
                    } else if (state == NetworkInfo.State.DISCONNECTED || state == NetworkInfo.State.SUSPENDED || state == NetworkInfo.State.CONNECTED) {
                    }
                    ConnectivityService.this.notifyLockdownVpn(null);
                    return;
                case 458753:
                    return;
                case 528385:
                    NetworkAgentInfo nai3 = (NetworkAgentInfo) ConnectivityService.this.mNetworkAgentInfos.get(msg.replyTo);
                    if (nai3 == null) {
                        ConnectivityService.loge("EVENT_NETWORK_INFO_CHANGED from unknown NetworkAgent");
                        return;
                    } else {
                        ConnectivityService.this.updateNetworkInfo(nai3, (NetworkInfo) msg.obj);
                        return;
                    }
                case 528386:
                    NetworkAgentInfo nai4 = (NetworkAgentInfo) ConnectivityService.this.mNetworkAgentInfos.get(msg.replyTo);
                    if (nai4 == null) {
                        ConnectivityService.loge("EVENT_NETWORK_CAPABILITIES_CHANGED from unknown NetworkAgent");
                        return;
                    } else {
                        ConnectivityService.this.updateCapabilities(nai4, (NetworkCapabilities) msg.obj);
                        return;
                    }
                case 528387:
                    NetworkAgentInfo nai5 = (NetworkAgentInfo) ConnectivityService.this.mNetworkAgentInfos.get(msg.replyTo);
                    if (nai5 == null) {
                        ConnectivityService.loge("NetworkAgent not found for EVENT_NETWORK_PROPERTIES_CHANGED");
                        return;
                    }
                    LinkProperties oldLp = nai5.linkProperties;
                    synchronized (nai5) {
                        nai5.linkProperties = (LinkProperties) msg.obj;
                        break;
                    }
                    if (nai5.created) {
                        ConnectivityService.this.updateLinkProperties(nai5, oldLp);
                        return;
                    }
                    return;
                case 528388:
                    NetworkAgentInfo nai6 = (NetworkAgentInfo) ConnectivityService.this.mNetworkAgentInfos.get(msg.replyTo);
                    if (nai6 == null) {
                        ConnectivityService.loge("EVENT_NETWORK_SCORE_CHANGED from unknown NetworkAgent");
                        return;
                    }
                    Integer score = (Integer) msg.obj;
                    if (score != null) {
                        ConnectivityService.this.updateNetworkScore(nai6, score.intValue());
                        return;
                    }
                    return;
                case 528389:
                    NetworkAgentInfo nai7 = (NetworkAgentInfo) ConnectivityService.this.mNetworkAgentInfos.get(msg.replyTo);
                    if (nai7 == null) {
                        ConnectivityService.loge("EVENT_UID_RANGES_ADDED from unknown NetworkAgent");
                        return;
                    }
                    try {
                        ConnectivityService.this.mNetd.addVpnUidRanges(nai7.network.netId, (UidRange[]) msg.obj);
                        return;
                    } catch (Exception e) {
                        ConnectivityService.loge("Exception in addVpnUidRanges: " + e);
                        return;
                    }
                case 528390:
                    NetworkAgentInfo nai8 = (NetworkAgentInfo) ConnectivityService.this.mNetworkAgentInfos.get(msg.replyTo);
                    if (nai8 == null) {
                        ConnectivityService.loge("EVENT_UID_RANGES_REMOVED from unknown NetworkAgent");
                        return;
                    }
                    try {
                        ConnectivityService.this.mNetd.removeVpnUidRanges(nai8.network.netId, (UidRange[]) msg.obj);
                        return;
                    } catch (Exception e2) {
                        ConnectivityService.loge("Exception in removeVpnUidRanges: " + e2);
                        return;
                    }
                case 528392:
                    NetworkAgentInfo nai9 = (NetworkAgentInfo) ConnectivityService.this.mNetworkAgentInfos.get(msg.replyTo);
                    if (nai9 == null) {
                        ConnectivityService.loge("EVENT_SET_EXPLICITLY_SELECTED from unknown NetworkAgent");
                        return;
                    }
                    if (nai9.created && !nai9.networkMisc.explicitlySelected) {
                        ConnectivityService.loge("ERROR: created network explicitly selected.");
                    }
                    nai9.networkMisc.explicitlySelected = ConnectivityService.DBG;
                    return;
                case NetworkMonitor.EVENT_NETWORK_TESTED:
                    NetworkAgentInfo nai10 = (NetworkAgentInfo) msg.obj;
                    if (ConnectivityService.this.isLiveNetworkAgent(nai10, "EVENT_NETWORK_VALIDATED")) {
                        boolean valid = msg.arg1 == 0 ? ConnectivityService.DBG : false;
                        nai10.lastValidated = valid;
                        if (valid) {
                            ConnectivityService.log("Validated " + nai10.name());
                            if (!nai10.everValidated) {
                                nai10.everValidated = ConnectivityService.DBG;
                                ConnectivityService.this.rematchNetworkAndRequests(nai10, NascentState.JUST_VALIDATED, ReapUnvalidatedNetworks.REAP);
                                ConnectivityService.this.sendUpdatedScoreToFactories(nai10);
                            }
                        }
                        ConnectivityService.this.updateInetCondition(nai10);
                        nai10.asyncChannel.sendMessage(528391, valid ? 1 : 2, 0, (Object) null);
                        return;
                    }
                    return;
                case NetworkMonitor.EVENT_NETWORK_LINGER_COMPLETE:
                    NetworkAgentInfo nai11 = (NetworkAgentInfo) msg.obj;
                    if (ConnectivityService.this.isLiveNetworkAgent(nai11, "EVENT_NETWORK_LINGER_COMPLETE")) {
                        ConnectivityService.this.handleLingerComplete(nai11);
                        return;
                    }
                    return;
                case NetworkMonitor.EVENT_PROVISIONING_NOTIFICATION:
                    if (msg.arg1 == 0) {
                        ConnectivityService.this.setProvNotificationVisibleIntent(false, msg.arg2, 0, null, null);
                        return;
                    }
                    synchronized (ConnectivityService.this.mNetworkForNetId) {
                        nai = (NetworkAgentInfo) ConnectivityService.this.mNetworkForNetId.get(msg.arg2);
                        break;
                    }
                    if (nai == null) {
                        ConnectivityService.loge("EVENT_PROVISIONING_NOTIFICATION from unknown NetworkMonitor");
                        return;
                    } else {
                        ConnectivityService.this.setProvNotificationVisibleIntent(ConnectivityService.DBG, msg.arg2, nai.networkInfo.getType(), nai.networkInfo.getExtraInfo(), (PendingIntent) msg.obj);
                        return;
                    }
                default:
                    return;
            }
        }
    }

    private void unlinger(NetworkAgentInfo nai) {
        if (nai.everValidated) {
            nai.networkLingered.clear();
            nai.networkMonitor.sendMessage(NetworkMonitor.CMD_NETWORK_CONNECTED);
        }
    }

    private void handleAsyncChannelHalfConnect(Message msg) {
        AsyncChannel ac = (AsyncChannel) msg.obj;
        if (this.mNetworkFactoryInfos.containsKey(msg.replyTo)) {
            if (msg.arg1 == 0) {
                for (NetworkRequestInfo nri : this.mNetworkRequests.values()) {
                    if (nri.isRequest) {
                        NetworkAgentInfo nai = this.mNetworkForRequestId.get(nri.request.requestId);
                        ac.sendMessage(536576, nai != null ? nai.getCurrentScore() : 0, 0, nri.request);
                    }
                }
                return;
            }
            loge("Error connecting NetworkFactory");
            this.mNetworkFactoryInfos.remove(msg.obj);
            return;
        }
        if (this.mNetworkAgentInfos.containsKey(msg.replyTo)) {
            if (msg.arg1 == 0) {
                this.mNetworkAgentInfos.get(msg.replyTo).asyncChannel.sendMessage(69633);
                return;
            }
            loge("Error connecting NetworkAgent");
            NetworkAgentInfo nai2 = this.mNetworkAgentInfos.remove(msg.replyTo);
            if (nai2 != null) {
                synchronized (this.mNetworkForNetId) {
                    this.mNetworkForNetId.remove(nai2.network.netId);
                }
                this.mLegacyTypeTracker.remove(nai2);
            }
        }
    }

    private void handleAsyncChannelDisconnected(Message msg) throws Throwable {
        NetworkAgentInfo nai = this.mNetworkAgentInfos.get(msg.replyTo);
        if (nai != null) {
            log(nai.name() + " got DISCONNECTED, was satisfying " + nai.networkRequests.size());
            if (nai.created) {
                try {
                    this.mNetd.removeNetwork(nai.network.netId);
                } catch (Exception e) {
                    loge("Exception removing network: " + e);
                }
            }
            if (nai.networkInfo.isConnected()) {
                nai.networkInfo.setDetailedState(NetworkInfo.DetailedState.DISCONNECTED, null, null);
            }
            if (isDefaultNetwork(nai)) {
                this.mDefaultInetConditionPublished = 0;
            }
            notifyIfacesChanged();
            notifyNetworkCallbacks(nai, 524292);
            nai.networkMonitor.sendMessage(NetworkMonitor.CMD_NETWORK_DISCONNECTED);
            this.mNetworkAgentInfos.remove(msg.replyTo);
            updateClat(null, nai.linkProperties, nai);
            this.mLegacyTypeTracker.remove(nai);
            synchronized (this.mNetworkForNetId) {
                this.mNetworkForNetId.remove(nai.network.netId);
            }
            ArrayList<NetworkAgentInfo> toActivate = new ArrayList<>();
            for (int i = 0; i < nai.networkRequests.size(); i++) {
                NetworkRequest request = nai.networkRequests.valueAt(i);
                NetworkAgentInfo currentNetwork = this.mNetworkForRequestId.get(request.requestId);
                if (currentNetwork != null && currentNetwork.network.netId == nai.network.netId) {
                    log("Checking for replacement network to handle request " + request);
                    this.mNetworkForRequestId.remove(request.requestId);
                    sendUpdatedScoreToFactories(request, 0);
                    NetworkAgentInfo alternative = null;
                    for (NetworkAgentInfo existing : this.mNetworkAgentInfos.values()) {
                        if (existing.satisfies(request) && (alternative == null || alternative.getCurrentScore() < existing.getCurrentScore())) {
                            alternative = existing;
                        }
                    }
                    if (alternative != null) {
                        log(" found replacement in " + alternative.name());
                        if (!toActivate.contains(alternative)) {
                            toActivate.add(alternative);
                        }
                    }
                }
            }
            if (nai.networkRequests.get(this.mDefaultRequest.requestId) != null) {
                removeDataActivityTracking(nai);
                notifyLockdownVpn(nai);
                requestNetworkTransitionWakelock(nai.name());
            }
            for (NetworkAgentInfo networkToActivate : toActivate) {
                unlinger(networkToActivate);
                rematchNetworkAndRequests(networkToActivate, NascentState.NOT_JUST_VALIDATED, ReapUnvalidatedNetworks.DONT_REAP);
            }
        }
    }

    private NetworkRequestInfo findExistingNetworkRequestInfo(PendingIntent pendingIntent) {
        Intent intent = pendingIntent.getIntent();
        for (Map.Entry<NetworkRequest, NetworkRequestInfo> entry : this.mNetworkRequests.entrySet()) {
            PendingIntent existingPendingIntent = entry.getValue().mPendingIntent;
            if (existingPendingIntent != null && existingPendingIntent.getIntent().filterEquals(intent)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private void handleRegisterNetworkRequestWithIntent(Message msg) {
        NetworkRequestInfo nri = (NetworkRequestInfo) msg.obj;
        NetworkRequestInfo existingRequest = findExistingNetworkRequestInfo(nri.mPendingIntent);
        if (existingRequest != null) {
            log("Replacing " + existingRequest.request + " with " + nri.request + " because their intents matched.");
            handleReleaseNetworkRequest(existingRequest.request, getCallingUid());
        }
        handleRegisterNetworkRequest(msg);
    }

    private void handleRegisterNetworkRequest(Message msg) {
        NetworkRequestInfo nri = (NetworkRequestInfo) msg.obj;
        this.mNetworkRequests.put(nri.request, nri);
        NetworkAgentInfo bestNetwork = null;
        for (NetworkAgentInfo network : this.mNetworkAgentInfos.values()) {
            log("handleRegisterNetworkRequest checking " + network.name());
            if (network.satisfies(nri.request)) {
                log("apparently satisfied.  currentScore=" + network.getCurrentScore());
                if (!nri.isRequest) {
                    network.addRequest(nri.request);
                    notifyNetworkCallback(network, nri);
                } else if (bestNetwork == null || bestNetwork.getCurrentScore() < network.getCurrentScore()) {
                    bestNetwork = network;
                }
            }
        }
        if (bestNetwork != null) {
            log("using " + bestNetwork.name());
            unlinger(bestNetwork);
            bestNetwork.addRequest(nri.request);
            this.mNetworkForRequestId.put(nri.request.requestId, bestNetwork);
            notifyNetworkCallback(bestNetwork, nri);
            if (nri.request.legacyType != -1) {
                this.mLegacyTypeTracker.add(nri.request.legacyType, bestNetwork);
            }
        }
        if (nri.isRequest) {
            log("sending new NetworkRequest to factories");
            int score = bestNetwork == null ? 0 : bestNetwork.getCurrentScore();
            for (NetworkFactoryInfo nfi : this.mNetworkFactoryInfos.values()) {
                nfi.asyncChannel.sendMessage(536576, score, 0, nri.request);
            }
        }
    }

    private void handleReleaseNetworkRequestWithIntent(PendingIntent pendingIntent, int callingUid) {
        NetworkRequestInfo nri = findExistingNetworkRequestInfo(pendingIntent);
        if (nri != null) {
            handleReleaseNetworkRequest(nri.request, callingUid);
        }
    }

    private boolean unneeded(NetworkAgentInfo nai) {
        if (!nai.created || nai.isVPN()) {
            return false;
        }
        boolean unneeded = DBG;
        if (nai.everValidated) {
            for (int i = 0; i < nai.networkRequests.size() && unneeded; i++) {
                NetworkRequest nr = nai.networkRequests.valueAt(i);
                try {
                    if (isRequest(nr)) {
                        unneeded = false;
                    }
                } catch (Exception e) {
                    loge("Request " + nr + " not found in mNetworkRequests.");
                    loge("  it came from request list  of " + nai.name());
                }
            }
            return unneeded;
        }
        for (NetworkRequestInfo nri : this.mNetworkRequests.values()) {
            if (nri.isRequest && nai.satisfies(nri.request) && (nai.networkRequests.get(nri.request.requestId) != null || this.mNetworkForRequestId.get(nri.request.requestId).getCurrentScore() < nai.getCurrentScoreAsValidated())) {
                return false;
            }
        }
        return DBG;
    }

    private void handleReleaseNetworkRequest(NetworkRequest request, int callingUid) {
        NetworkRequestInfo nri = this.mNetworkRequests.get(request);
        if (nri != null) {
            if (1000 != callingUid && nri.mUid != callingUid) {
                log("Attempt to release unowned NetworkRequest " + request);
                return;
            }
            log("releasing NetworkRequest " + request);
            nri.unlinkDeathRecipient();
            this.mNetworkRequests.remove(request);
            if (nri.isRequest) {
                boolean wasKept = false;
                for (NetworkAgentInfo nai : this.mNetworkAgentInfos.values()) {
                    if (nai.networkRequests.get(nri.request.requestId) != null) {
                        nai.networkRequests.remove(nri.request.requestId);
                        log(" Removing from current network " + nai.name() + ", leaving " + nai.networkRequests.size() + " requests.");
                        if (unneeded(nai)) {
                            log("no live requests for " + nai.name() + "; disconnecting");
                            teardownUnneededNetwork(nai);
                        } else {
                            wasKept |= DBG;
                        }
                    }
                }
                NetworkAgentInfo nai2 = this.mNetworkForRequestId.get(nri.request.requestId);
                if (nai2 != null) {
                    this.mNetworkForRequestId.remove(nri.request.requestId);
                }
                if (nri.request.legacyType != -1 && nai2 != null) {
                    boolean doRemove = DBG;
                    if (wasKept) {
                        for (int i = 0; i < nai2.networkRequests.size(); i++) {
                            NetworkRequest otherRequest = nai2.networkRequests.valueAt(i);
                            if (otherRequest.legacyType == nri.request.legacyType && isRequest(otherRequest)) {
                                log(" still have other legacy request - leaving");
                                doRemove = false;
                            }
                        }
                    }
                    if (doRemove) {
                        this.mLegacyTypeTracker.remove(nri.request.legacyType, nai2);
                    }
                }
                for (NetworkFactoryInfo nfi : this.mNetworkFactoryInfos.values()) {
                    nfi.asyncChannel.sendMessage(536577, nri.request);
                }
            } else {
                Iterator<NetworkAgentInfo> it = this.mNetworkAgentInfos.values().iterator();
                while (it.hasNext()) {
                    it.next().networkRequests.remove(nri.request.requestId);
                }
            }
            callCallbackForRequest(nri, null, 524296);
        }
    }

    private class InternalHandler extends Handler {
        public InternalHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            boolean met = ConnectivityService.DBG;
            switch (msg.what) {
                case 8:
                case 24:
                    synchronized (ConnectivityService.this) {
                        if (msg.arg1 == ConnectivityService.this.mNetTransitionWakeLockSerialNumber && ConnectivityService.this.mNetTransitionWakeLock.isHeld()) {
                            ConnectivityService.this.mNetTransitionWakeLock.release();
                            String causedBy = ConnectivityService.this.mNetTransitionWakeLockCausedBy;
                            if (msg.what == 24) {
                                ConnectivityService.log("Failed to find a new network - expiring NetTransition Wakelock");
                            } else {
                                StringBuilder sbAppend = new StringBuilder().append("NetTransition Wakelock (");
                                if (causedBy == null) {
                                    causedBy = "unknown";
                                }
                                ConnectivityService.log(sbAppend.append(causedBy).append(" cleared because we found a replacement network").toString());
                            }
                        }
                    }
                    return;
                case 9:
                    ConnectivityService.this.handleDeprecatedGlobalHttpProxy();
                    return;
                case 10:
                    if (msg.arg1 != 1) {
                        met = false;
                    }
                    ConnectivityService.this.handleSetDependencyMet(msg.arg2, met);
                    return;
                case 11:
                    Intent intent = (Intent) msg.obj;
                    ConnectivityService.this.sendStickyBroadcast(intent);
                    return;
                case 12:
                case 13:
                case 20:
                default:
                    return;
                case 14:
                    int tag = ConnectivityService.this.mEnableFailFastMobileDataTag.get();
                    if (msg.arg1 == tag) {
                        MobileDataStateTracker mobileDst = ConnectivityService.this.mNetTrackers[0];
                        if (mobileDst != null) {
                            mobileDst.setEnableFailFastMobileData(msg.arg2);
                            return;
                        }
                        return;
                    }
                    ConnectivityService.log("EVENT_ENABLE_FAIL_FAST_MOBILE_DATA: stale arg1:" + msg.arg1 + " != tag:" + tag);
                    return;
                case 15:
                    ConnectivityService.this.handleNetworkSamplingTimeout();
                    return;
                case 16:
                    ConnectivityService.this.handleApplyDefaultProxy((ProxyInfo) msg.obj);
                    return;
                case 17:
                    ConnectivityService.this.handleRegisterNetworkFactory((NetworkFactoryInfo) msg.obj);
                    return;
                case 18:
                    ConnectivityService.this.handleRegisterNetworkAgent((NetworkAgentInfo) msg.obj);
                    return;
                case 19:
                case 21:
                    ConnectivityService.this.handleRegisterNetworkRequest(msg);
                    return;
                case 22:
                    ConnectivityService.this.handleReleaseNetworkRequest((NetworkRequest) msg.obj, msg.arg1);
                    return;
                case 23:
                    ConnectivityService.this.handleUnregisterNetworkFactory((Messenger) msg.obj);
                    return;
                case 25:
                    for (NetworkAgentInfo nai : ConnectivityService.this.mNetworkAgentInfos.values()) {
                        nai.networkMonitor.systemReady = ConnectivityService.DBG;
                    }
                    return;
                case 26:
                    ConnectivityService.this.handleRegisterNetworkRequestWithIntent(msg);
                    return;
                case 27:
                    ConnectivityService.this.handleReleaseNetworkRequestWithIntent((PendingIntent) msg.obj, msg.arg1);
                    return;
            }
        }
    }

    public int tether(String iface) {
        ConnectivityManager.enforceTetherChangePermission(this.mContext);
        if (isTetheringSupported()) {
            return this.mTethering.tether(iface);
        }
        return 3;
    }

    public int untether(String iface) {
        ConnectivityManager.enforceTetherChangePermission(this.mContext);
        if (isTetheringSupported()) {
            return this.mTethering.untether(iface);
        }
        return 3;
    }

    public int getLastTetherError(String iface) {
        enforceTetherAccessPermission();
        if (isTetheringSupported()) {
            return this.mTethering.getLastTetherError(iface);
        }
        return 3;
    }

    public String[] getTetherableUsbRegexs() {
        enforceTetherAccessPermission();
        return isTetheringSupported() ? this.mTethering.getTetherableUsbRegexs() : new String[0];
    }

    public String[] getTetherableWifiRegexs() {
        enforceTetherAccessPermission();
        return isTetheringSupported() ? this.mTethering.getTetherableWifiRegexs() : new String[0];
    }

    public String[] getTetherableBluetoothRegexs() {
        enforceTetherAccessPermission();
        return isTetheringSupported() ? this.mTethering.getTetherableBluetoothRegexs() : new String[0];
    }

    public int setUsbTethering(boolean enable) {
        ConnectivityManager.enforceTetherChangePermission(this.mContext);
        if (isTetheringSupported()) {
            return this.mTethering.setUsbTethering(enable);
        }
        return 3;
    }

    public String[] getTetherableIfaces() {
        enforceTetherAccessPermission();
        return this.mTethering.getTetherableIfaces();
    }

    public String[] getTetheredIfaces() {
        enforceTetherAccessPermission();
        return this.mTethering.getTetheredIfaces();
    }

    public String[] getTetheringErroredIfaces() {
        enforceTetherAccessPermission();
        return this.mTethering.getErroredIfaces();
    }

    public String[] getTetheredDhcpRanges() {
        enforceConnectivityInternalPermission();
        return this.mTethering.getTetheredDhcpRanges();
    }

    public boolean isTetheringSupported() {
        enforceTetherAccessPermission();
        int defaultVal = SystemProperties.get("ro.tether.denied").equals("true") ? 0 : 1;
        boolean tetherEnabledInSettings = (Settings.Global.getInt(this.mContext.getContentResolver(), "tether_supported", defaultVal) == 0 || this.mUserManager.hasUserRestriction("no_config_tethering")) ? false : true;
        if (!tetherEnabledInSettings || ((this.mTethering.getTetherableUsbRegexs().length == 0 && this.mTethering.getTetherableWifiRegexs().length == 0 && this.mTethering.getTetherableBluetoothRegexs().length == 0) || this.mTethering.getUpstreamIfaceTypes().length == 0)) {
            return false;
        }
        return DBG;
    }

    private void requestNetworkTransitionWakelock(String forWhom) throws Throwable {
        synchronized (this) {
            try {
                if (!this.mNetTransitionWakeLock.isHeld()) {
                    int serialNum = this.mNetTransitionWakeLockSerialNumber + 1;
                    this.mNetTransitionWakeLockSerialNumber = serialNum;
                    try {
                        this.mNetTransitionWakeLock.acquire();
                        this.mNetTransitionWakeLockCausedBy = forWhom;
                        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(24, serialNum, 0), this.mNetTransitionWakeLockTimeout);
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    public void reportInetCondition(int networkType, int percentage) {
        NetworkAgentInfo nai = this.mLegacyTypeTracker.getNetworkForType(networkType);
        if (nai != null) {
            boolean isGood = percentage > 50 ? DBG : false;
            if (isGood != nai.lastValidated) {
                if (isGood) {
                    log("reportInetCondition: type=" + networkType + " ok, revalidate");
                }
                reportBadNetwork(nai.network);
            }
        }
    }

    public void reportBadNetwork(Network network) {
        enforceAccessPermission();
        enforceInternetPermission();
        if (network != null) {
            int uid = Binder.getCallingUid();
            NetworkAgentInfo nai = getNetworkAgentInfoForNetwork(network);
            if (nai != null) {
                log("reportBadNetwork(" + nai.name() + ") by " + uid);
                synchronized (nai) {
                    if (nai.created) {
                        if (!isNetworkWithLinkPropertiesBlocked(nai.linkProperties, uid)) {
                            nai.networkMonitor.sendMessage(NetworkMonitor.CMD_FORCE_REEVALUATION, uid);
                        }
                    }
                }
            }
        }
    }

    public ProxyInfo getDefaultProxy() {
        ProxyInfo ret;
        synchronized (this.mProxyLock) {
            ret = this.mGlobalProxy;
            if (ret == null && !this.mDefaultProxyDisabled) {
                ret = this.mDefaultProxy;
            }
        }
        return ret;
    }

    private ProxyInfo canonicalizeProxyInfo(ProxyInfo proxy) {
        if (proxy == null || !TextUtils.isEmpty(proxy.getHost())) {
            return proxy;
        }
        if (proxy.getPacFileUrl() == null || Uri.EMPTY.equals(proxy.getPacFileUrl())) {
            return null;
        }
        return proxy;
    }

    private boolean proxyInfoEqual(ProxyInfo a, ProxyInfo b) {
        ProxyInfo a2 = canonicalizeProxyInfo(a);
        ProxyInfo b2 = canonicalizeProxyInfo(b);
        if (Objects.equals(a2, b2) && (a2 == null || Objects.equals(a2.getHost(), b2.getHost()))) {
            return DBG;
        }
        return false;
    }

    public void setGlobalProxy(ProxyInfo proxyProperties) {
        enforceConnectivityInternalPermission();
        synchronized (this.mProxyLock) {
            if (proxyProperties != this.mGlobalProxy) {
                if (proxyProperties == null || !proxyProperties.equals(this.mGlobalProxy)) {
                    if (this.mGlobalProxy == null || !this.mGlobalProxy.equals(proxyProperties)) {
                        String host = "";
                        int port = 0;
                        String exclList = "";
                        String pacFileUrl = "";
                        if (proxyProperties != null && (!TextUtils.isEmpty(proxyProperties.getHost()) || !Uri.EMPTY.equals(proxyProperties.getPacFileUrl()))) {
                            if (!proxyProperties.isValid()) {
                                log("Invalid proxy properties, ignoring: " + proxyProperties.toString());
                                return;
                            }
                            this.mGlobalProxy = new ProxyInfo(proxyProperties);
                            host = this.mGlobalProxy.getHost();
                            port = this.mGlobalProxy.getPort();
                            exclList = this.mGlobalProxy.getExclusionListAsString();
                            if (!Uri.EMPTY.equals(proxyProperties.getPacFileUrl())) {
                                pacFileUrl = proxyProperties.getPacFileUrl().toString();
                            }
                        } else {
                            this.mGlobalProxy = null;
                        }
                        ContentResolver res = this.mContext.getContentResolver();
                        long token = Binder.clearCallingIdentity();
                        try {
                            Settings.Global.putString(res, "global_http_proxy_host", host);
                            Settings.Global.putInt(res, "global_http_proxy_port", port);
                            Settings.Global.putString(res, "global_http_proxy_exclusion_list", exclList);
                            Settings.Global.putString(res, "global_proxy_pac_url", pacFileUrl);
                            Binder.restoreCallingIdentity(token);
                            if (this.mGlobalProxy == null) {
                                proxyProperties = this.mDefaultProxy;
                            }
                            sendProxyBroadcast(proxyProperties);
                        } catch (Throwable th) {
                            Binder.restoreCallingIdentity(token);
                            throw th;
                        }
                    }
                }
            }
        }
    }

    private void loadGlobalProxy() {
        ProxyInfo proxyProperties;
        ContentResolver res = this.mContext.getContentResolver();
        String host = Settings.Global.getString(res, "global_http_proxy_host");
        int port = Settings.Global.getInt(res, "global_http_proxy_port", 0);
        String exclList = Settings.Global.getString(res, "global_http_proxy_exclusion_list");
        String pacFileUrl = Settings.Global.getString(res, "global_proxy_pac_url");
        if (!TextUtils.isEmpty(host) || !TextUtils.isEmpty(pacFileUrl)) {
            if (!TextUtils.isEmpty(pacFileUrl)) {
                proxyProperties = new ProxyInfo(pacFileUrl);
            } else {
                proxyProperties = new ProxyInfo(host, port, exclList);
            }
            if (!proxyProperties.isValid()) {
                log("Invalid proxy properties, ignoring: " + proxyProperties.toString());
                return;
            }
            synchronized (this.mProxyLock) {
                this.mGlobalProxy = proxyProperties;
            }
        }
    }

    public ProxyInfo getGlobalProxy() {
        ProxyInfo proxyInfo;
        synchronized (this.mProxyLock) {
            proxyInfo = this.mGlobalProxy;
        }
        return proxyInfo;
    }

    private void handleApplyDefaultProxy(ProxyInfo proxy) {
        if (proxy != null && TextUtils.isEmpty(proxy.getHost()) && Uri.EMPTY.equals(proxy.getPacFileUrl())) {
            proxy = null;
        }
        synchronized (this.mProxyLock) {
            if (this.mDefaultProxy == null || !this.mDefaultProxy.equals(proxy)) {
                if (this.mDefaultProxy != proxy) {
                    if (proxy != null && !proxy.isValid()) {
                        log("Invalid proxy properties, ignoring: " + proxy.toString());
                        return;
                    }
                    if (this.mGlobalProxy != null && proxy != null && !Uri.EMPTY.equals(proxy.getPacFileUrl()) && proxy.getPacFileUrl().equals(this.mGlobalProxy.getPacFileUrl())) {
                        this.mGlobalProxy = proxy;
                        sendProxyBroadcast(this.mGlobalProxy);
                        return;
                    }
                    this.mDefaultProxy = proxy;
                    if (this.mGlobalProxy == null) {
                        if (!this.mDefaultProxyDisabled) {
                            sendProxyBroadcast(proxy);
                        }
                    }
                }
            }
        }
    }

    private void updateProxy(LinkProperties newLp, LinkProperties oldLp, NetworkAgentInfo nai) {
        ProxyInfo newProxyInfo = newLp == null ? null : newLp.getHttpProxy();
        ProxyInfo oldProxyInfo = oldLp != null ? oldLp.getHttpProxy() : null;
        if (!proxyInfoEqual(newProxyInfo, oldProxyInfo)) {
            sendProxyBroadcast(getDefaultProxy());
        }
    }

    private void handleDeprecatedGlobalHttpProxy() {
        String proxy = Settings.Global.getString(this.mContext.getContentResolver(), "http_proxy");
        if (!TextUtils.isEmpty(proxy)) {
            String[] data = proxy.split(":");
            if (data.length != 0) {
                String str = data[0];
                int proxyPort = 8080;
                if (data.length > 1) {
                    try {
                        proxyPort = Integer.parseInt(data[1]);
                    } catch (NumberFormatException e) {
                        return;
                    }
                }
                ProxyInfo p = new ProxyInfo(data[0], proxyPort, "");
                setGlobalProxy(p);
            }
        }
    }

    private void sendProxyBroadcast(ProxyInfo proxy) {
        if (proxy == null) {
            proxy = new ProxyInfo("", 0, "");
        }
        if (!this.mPacManager.setCurrentProxyScriptUrl(proxy)) {
            log("sending Proxy Broadcast for " + proxy);
            Intent intent = new Intent("android.intent.action.PROXY_CHANGE");
            intent.addFlags(603979776);
            intent.putExtra("android.intent.extra.PROXY_INFO", proxy);
            long ident = Binder.clearCallingIdentity();
            try {
                this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    private static class SettingsObserver extends ContentObserver {
        private Handler mHandler;
        private int mWhat;

        SettingsObserver(Handler handler, int what) {
            super(handler);
            this.mHandler = handler;
            this.mWhat = what;
        }

        void observe(Context context) {
            ContentResolver resolver = context.getContentResolver();
            resolver.registerContentObserver(Settings.Global.getUriFor("http_proxy"), false, this);
        }

        @Override
        public void onChange(boolean selfChange) {
            this.mHandler.obtainMessage(this.mWhat).sendToTarget();
        }
    }

    private static void log(String s) {
        Slog.d(TAG, s);
    }

    private static void loge(String s) {
        Slog.e(TAG, s);
    }

    private static <T> T checkNotNull(T value, String message) {
        if (value == null) {
            throw new NullPointerException(message);
        }
        return value;
    }

    public boolean prepareVpn(String oldPackage, String newPackage) {
        boolean zPrepare;
        throwIfLockdownEnabled();
        int user = UserHandle.getUserId(Binder.getCallingUid());
        synchronized (this.mVpns) {
            zPrepare = this.mVpns.get(user).prepare(oldPackage, newPackage);
        }
        return zPrepare;
    }

    public void setVpnPackageAuthorization(boolean authorized) {
        int user = UserHandle.getUserId(Binder.getCallingUid());
        synchronized (this.mVpns) {
            this.mVpns.get(user).setPackageAuthorization(authorized);
        }
    }

    public ParcelFileDescriptor establishVpn(VpnConfig config) {
        ParcelFileDescriptor parcelFileDescriptorEstablish;
        throwIfLockdownEnabled();
        int user = UserHandle.getUserId(Binder.getCallingUid());
        synchronized (this.mVpns) {
            parcelFileDescriptorEstablish = this.mVpns.get(user).establish(config);
        }
        return parcelFileDescriptorEstablish;
    }

    public void startLegacyVpn(VpnProfile profile) {
        throwIfLockdownEnabled();
        LinkProperties egress = getActiveLinkProperties();
        if (egress == null) {
            throw new IllegalStateException("Missing active network connection");
        }
        int user = UserHandle.getUserId(Binder.getCallingUid());
        synchronized (this.mVpns) {
            this.mVpns.get(user).startLegacyVpn(profile, this.mKeyStore, egress);
        }
    }

    public LegacyVpnInfo getLegacyVpnInfo() {
        LegacyVpnInfo legacyVpnInfo;
        throwIfLockdownEnabled();
        int user = UserHandle.getUserId(Binder.getCallingUid());
        synchronized (this.mVpns) {
            legacyVpnInfo = this.mVpns.get(user).getLegacyVpnInfo();
        }
        return legacyVpnInfo;
    }

    public VpnConfig getVpnConfig() {
        VpnConfig vpnConfig;
        int user = UserHandle.getUserId(Binder.getCallingUid());
        synchronized (this.mVpns) {
            vpnConfig = this.mVpns.get(user).getVpnConfig();
        }
        return vpnConfig;
    }

    public boolean updateLockdownVpn() {
        if (Binder.getCallingUid() != 1000) {
            Slog.w(TAG, "Lockdown VPN only available to AID_SYSTEM");
            return false;
        }
        this.mLockdownEnabled = LockdownVpnTracker.isEnabled();
        if (this.mLockdownEnabled) {
            if (!this.mKeyStore.isUnlocked()) {
                Slog.w(TAG, "KeyStore locked; unable to create LockdownTracker");
                return false;
            }
            String profileName = new String(this.mKeyStore.get("LOCKDOWN_VPN"));
            VpnProfile profile = VpnProfile.decode(profileName, this.mKeyStore.get("VPN_" + profileName));
            int user = UserHandle.getUserId(Binder.getCallingUid());
            synchronized (this.mVpns) {
                setLockdownTracker(new LockdownVpnTracker(this.mContext, this.mNetd, this, this.mVpns.get(user), profile));
            }
        } else {
            setLockdownTracker(null);
        }
        return DBG;
    }

    private void setLockdownTracker(LockdownVpnTracker tracker) {
        LockdownVpnTracker existing = this.mLockdownTracker;
        this.mLockdownTracker = null;
        if (existing != null) {
            existing.shutdown();
        }
        try {
            if (tracker != null) {
                this.mNetd.setFirewallEnabled(DBG);
                this.mNetd.setFirewallInterfaceRule("lo", DBG);
                this.mLockdownTracker = tracker;
                this.mLockdownTracker.init();
            } else {
                this.mNetd.setFirewallEnabled(false);
            }
        } catch (RemoteException e) {
        }
    }

    private void throwIfLockdownEnabled() {
        if (this.mLockdownEnabled) {
            throw new IllegalStateException("Unavailable in lockdown mode");
        }
    }

    public void supplyMessenger(int networkType, Messenger messenger) {
        enforceConnectivityInternalPermission();
        if (ConnectivityManager.isNetworkTypeValid(networkType) && this.mNetTrackers[networkType] != null) {
            this.mNetTrackers[networkType].supplyMessenger(messenger);
        }
    }

    public int findConnectionTypeForIface(String iface) {
        int type = -1;
        enforceConnectivityInternalPermission();
        if (!TextUtils.isEmpty(iface)) {
            synchronized (this.mNetworkForNetId) {
                int i = 0;
                while (true) {
                    if (i >= this.mNetworkForNetId.size()) {
                        break;
                    }
                    NetworkAgentInfo nai = this.mNetworkForNetId.valueAt(i);
                    LinkProperties lp = nai.linkProperties;
                    if (lp == null || !iface.equals(lp.getInterfaceName()) || nai.networkInfo == null) {
                        i++;
                    } else {
                        type = nai.networkInfo.getType();
                        break;
                    }
                }
            }
        }
        return type;
    }

    private void setEnableFailFastMobileData(int enabled) {
        int tag;
        if (enabled == 1) {
            tag = this.mEnableFailFastMobileDataTag.incrementAndGet();
        } else {
            tag = this.mEnableFailFastMobileDataTag.get();
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(14, tag, enabled));
    }

    public int checkMobileProvisioning(int suggestedTimeOutMs) {
        return -1;
    }

    private void setProvNotificationVisible(boolean visible, int networkType, String action) {
        log("setProvNotificationVisible: E visible=" + visible + " networkType=" + networkType + " action=" + action);
        Intent intent = new Intent(action);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this.mContext, 0, intent, 0);
        int id = 65536 + networkType + 1;
        setProvNotificationVisibleIntent(visible, id, networkType, null, pendingIntent);
    }

    private void setProvNotificationVisibleIntent(boolean visible, int id, int networkType, String extraInfo, PendingIntent intent) {
        CharSequence title;
        CharSequence details;
        int icon;
        log("setProvNotificationVisibleIntent: E visible=" + visible + " networkType=" + networkType + " extraInfo=" + extraInfo);
        Resources r = Resources.getSystem();
        NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        if (visible) {
            Notification notification = new Notification();
            switch (networkType) {
                case 0:
                case 5:
                    title = r.getString(R.string.imProtocolJabber, 0);
                    details = this.mTelephonyManager.getNetworkOperatorName();
                    icon = R.drawable.input_method_switch_item_background;
                    break;
                case 1:
                    title = r.getString(R.string.imProtocolIcq, 0);
                    details = r.getString(R.string.imProtocolMsn, extraInfo);
                    icon = R.drawable.item_background_borderless_material;
                    break;
                case 2:
                case 3:
                case 4:
                default:
                    title = r.getString(R.string.imProtocolJabber, 0);
                    details = r.getString(R.string.imProtocolMsn, extraInfo);
                    icon = R.drawable.input_method_switch_item_background;
                    break;
            }
            notification.when = 0L;
            notification.icon = icon;
            notification.flags = 16;
            notification.tickerText = title;
            notification.color = this.mContext.getResources().getColor(R.color.system_accent3_600);
            notification.setLatestEventInfo(this.mContext, title, details, notification.contentIntent);
            notification.contentIntent = intent;
            try {
                notificationManager.notify(NOTIFICATION_ID, id, notification);
            } catch (NullPointerException npe) {
                loge("setNotificaitionVisible: visible notificationManager npe=" + npe);
                npe.printStackTrace();
            }
        } else {
            try {
                notificationManager.cancel(NOTIFICATION_ID, id);
            } catch (NullPointerException npe2) {
                loge("setNotificaitionVisible: cancel notificationManager npe=" + npe2);
                npe2.printStackTrace();
            }
        }
        this.mIsNotificationVisible = visible;
    }

    private String getProvisioningUrlBaseFromFile(int type) throws Throwable {
        String tagType;
        FileReader fileReader;
        String mcc;
        String mnc;
        String text = null;
        FileReader fileReader2 = null;
        Configuration config = this.mContext.getResources().getConfiguration();
        switch (type) {
            case 1:
                tagType = TAG_REDIRECTED_URL;
                break;
            case 2:
                tagType = TAG_PROVISIONING_URL;
                break;
            default:
                throw new RuntimeException("getProvisioningUrlBaseFromFile: Unexpected parameter " + type);
        }
        try {
            try {
                fileReader = new FileReader(this.mProvisioningUrlFile);
            } catch (Throwable th) {
                th = th;
            }
        } catch (FileNotFoundException e) {
        } catch (IOException e2) {
            e = e2;
        } catch (XmlPullParserException e3) {
            e = e3;
        }
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(fileReader);
            XmlUtils.beginDocument(parser, TAG_PROVISIONING_URLS);
            while (true) {
                XmlUtils.nextElement(parser);
                String element = parser.getName();
                if (element == null) {
                    if (fileReader != null) {
                        try {
                            fileReader.close();
                        } catch (IOException e4) {
                        }
                    }
                } else if (element.equals(tagType) && (mcc = parser.getAttributeValue(null, ATTR_MCC)) != null) {
                    try {
                        if (Integer.parseInt(mcc) == config.mcc && (mnc = parser.getAttributeValue(null, ATTR_MNC)) != null && Integer.parseInt(mnc) == config.mnc) {
                            parser.next();
                            if (parser.getEventType() == 4) {
                                text = parser.getText();
                                if (fileReader != null) {
                                    try {
                                        fileReader.close();
                                    } catch (IOException e5) {
                                    }
                                }
                            } else {
                                continue;
                            }
                        }
                    } catch (NumberFormatException e6) {
                        loge("NumberFormatException in getProvisioningUrlBaseFromFile: " + e6);
                    }
                }
            }
        } catch (FileNotFoundException e7) {
            fileReader2 = fileReader;
            loge("Carrier Provisioning Urls file not found");
            if (fileReader2 != null) {
                try {
                    fileReader2.close();
                } catch (IOException e8) {
                }
            }
        } catch (IOException e9) {
            e = e9;
            fileReader2 = fileReader;
            loge("I/O exception reading Carrier Provisioning Urls file: " + e);
            if (fileReader2 != null) {
                try {
                    fileReader2.close();
                } catch (IOException e10) {
                }
            }
        } catch (XmlPullParserException e11) {
            e = e11;
            fileReader2 = fileReader;
            loge("Xml parser exception reading Carrier Provisioning Urls file: " + e);
            if (fileReader2 != null) {
                try {
                    fileReader2.close();
                } catch (IOException e12) {
                }
            }
        } catch (Throwable th2) {
            th = th2;
            fileReader2 = fileReader;
            if (fileReader2 != null) {
                try {
                    fileReader2.close();
                } catch (IOException e13) {
                }
            }
            throw th;
        }
        return text;
    }

    public String getMobileRedirectedProvisioningUrl() throws Throwable {
        enforceConnectivityInternalPermission();
        String url = getProvisioningUrlBaseFromFile(1);
        if (TextUtils.isEmpty(url)) {
            return this.mContext.getResources().getString(R.string.config_customMediaSessionPolicyProvider);
        }
        return url;
    }

    public String getMobileProvisioningUrl() throws Throwable {
        enforceConnectivityInternalPermission();
        String url = getProvisioningUrlBaseFromFile(2);
        if (TextUtils.isEmpty(url)) {
            url = this.mContext.getResources().getString(R.string.config_customMediaKeyDispatcher);
            log("getMobileProvisioningUrl: mobile_provisioining_url from resource =" + url);
        } else {
            log("getMobileProvisioningUrl: mobile_provisioning_url from File =" + url);
        }
        if (!TextUtils.isEmpty(url)) {
            String phoneNumber = this.mTelephonyManager.getLine1Number();
            if (TextUtils.isEmpty(phoneNumber)) {
                phoneNumber = "0000000000";
            }
            return String.format(url, this.mTelephonyManager.getSimSerialNumber(), this.mTelephonyManager.getDeviceId(), phoneNumber);
        }
        return url;
    }

    public void setProvisioningNotificationVisible(boolean visible, int networkType, String action) {
        enforceConnectivityInternalPermission();
        long ident = Binder.clearCallingIdentity();
        try {
            setProvNotificationVisible(visible, networkType, action);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void setAirplaneMode(boolean enable) {
        enforceConnectivityInternalPermission();
        long ident = Binder.clearCallingIdentity();
        try {
            ContentResolver cr = this.mContext.getContentResolver();
            Settings.Global.putInt(cr, "airplane_mode_on", enable ? 1 : 0);
            Intent intent = new Intent("android.intent.action.AIRPLANE_MODE");
            intent.putExtra("state", enable);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void onUserStart(int userId) {
        synchronized (this.mVpns) {
            Vpn userVpn = this.mVpns.get(userId);
            if (userVpn != null) {
                loge("Starting user already has a VPN");
            } else {
                Vpn userVpn2 = new Vpn(this.mHandler.getLooper(), this.mContext, this.mNetd, this, userId);
                this.mVpns.put(userId, userVpn2);
            }
        }
    }

    private void onUserStop(int userId) {
        synchronized (this.mVpns) {
            Vpn userVpn = this.mVpns.get(userId);
            if (userVpn == null) {
                loge("Stopping user has no VPN");
            } else {
                this.mVpns.delete(userId);
            }
        }
    }

    private void handleNetworkSamplingTimeout() {
        SamplingDataTracker.SamplingSnapshot ss;
        String ifaceName;
        Map<String, SamplingDataTracker.SamplingSnapshot> mapIfaceToSample = new HashMap<>();
        NetworkStateTracker[] arr$ = this.mNetTrackers;
        for (NetworkStateTracker tracker : arr$) {
            if (tracker != null && (ifaceName = tracker.getNetworkInterfaceName()) != null) {
                mapIfaceToSample.put(ifaceName, null);
            }
        }
        SamplingDataTracker.getSamplingSnapshots(mapIfaceToSample);
        NetworkStateTracker[] arr$2 = this.mNetTrackers;
        for (NetworkStateTracker tracker2 : arr$2) {
            if (tracker2 != null && (ss = mapIfaceToSample.get(tracker2.getNetworkInterfaceName())) != null) {
                tracker2.stopSampling(ss);
                tracker2.startSampling(ss);
            }
        }
        int samplingIntervalInSeconds = Settings.Global.getInt(this.mContext.getContentResolver(), "connectivity_sampling_interval_in_seconds", DEFAULT_SAMPLING_INTERVAL_IN_SECONDS);
        setAlarm(samplingIntervalInSeconds * 1000, this.mSampleIntervalElapsedIntent);
    }

    void setAlarm(int timeoutInMilliseconds, PendingIntent intent) {
        int alarmType;
        long wakeupTime = SystemClock.elapsedRealtime() + ((long) timeoutInMilliseconds);
        if (Resources.getSystem().getBoolean(R.^attr-private.listItemLayout)) {
            alarmType = 2;
        } else {
            alarmType = 3;
        }
        this.mAlarmManager.set(alarmType, wakeupTime, intent);
    }

    private static class NetworkFactoryInfo {
        public final AsyncChannel asyncChannel;
        public final Messenger messenger;
        public final String name;

        public NetworkFactoryInfo(String name, Messenger messenger, AsyncChannel asyncChannel) {
            this.name = name;
            this.messenger = messenger;
            this.asyncChannel = asyncChannel;
        }
    }

    private class NetworkRequestInfo implements IBinder.DeathRecipient {
        static final boolean LISTEN = false;
        static final boolean REQUEST = true;
        final boolean isRequest;
        private final IBinder mBinder;
        final PendingIntent mPendingIntent;
        boolean mPendingIntentSent;
        final int mPid;
        final int mUid;
        final Messenger messenger;
        final NetworkRequest request;

        NetworkRequestInfo(NetworkRequest r, PendingIntent pi, boolean isRequest) {
            this.request = r;
            this.mPendingIntent = pi;
            this.messenger = null;
            this.mBinder = null;
            this.mPid = Binder.getCallingPid();
            this.mUid = Binder.getCallingUid();
            this.isRequest = isRequest;
        }

        NetworkRequestInfo(Messenger m, NetworkRequest r, IBinder binder, boolean isRequest) {
            this.messenger = m;
            this.request = r;
            this.mBinder = binder;
            this.mPid = Binder.getCallingPid();
            this.mUid = Binder.getCallingUid();
            this.isRequest = isRequest;
            this.mPendingIntent = null;
            try {
                this.mBinder.linkToDeath(this, 0);
            } catch (RemoteException e) {
                binderDied();
            }
        }

        void unlinkDeathRecipient() {
            if (this.mBinder != null) {
                this.mBinder.unlinkToDeath(this, 0);
            }
        }

        @Override
        public void binderDied() {
            ConnectivityService.log("ConnectivityService NetworkRequestInfo binderDied(" + this.request + ", " + this.mBinder + ")");
            ConnectivityService.this.releaseNetworkRequest(this.request);
        }

        public String toString() {
            return (this.isRequest ? "Request" : "Listen") + " from uid/pid:" + this.mUid + "/" + this.mPid + " for " + this.request + (this.mPendingIntent == null ? "" : " to trigger " + this.mPendingIntent);
        }
    }

    public NetworkRequest requestNetwork(NetworkCapabilities networkCapabilities, Messenger messenger, int timeoutMs, IBinder binder, int legacyType) {
        NetworkCapabilities networkCapabilities2 = new NetworkCapabilities(networkCapabilities);
        enforceNetworkRequestPermissions(networkCapabilities2);
        enforceMeteredApnPolicy(networkCapabilities2);
        if (timeoutMs < 0 || timeoutMs > 6000000) {
            throw new IllegalArgumentException("Bad timeout specified");
        }
        NetworkRequest networkRequest = new NetworkRequest(networkCapabilities2, legacyType, nextNetworkRequestId());
        log("requestNetwork for " + networkRequest);
        NetworkRequestInfo nri = new NetworkRequestInfo(messenger, networkRequest, binder, DBG);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(19, nri));
        if (timeoutMs > 0) {
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(20, nri), timeoutMs);
        }
        return networkRequest;
    }

    private void enforceNetworkRequestPermissions(NetworkCapabilities networkCapabilities) {
        if (!networkCapabilities.hasCapability(13)) {
            enforceConnectivityInternalPermission();
        } else {
            enforceChangePermission();
        }
    }

    private void enforceMeteredApnPolicy(NetworkCapabilities networkCapabilities) {
        int uidRules;
        if (!networkCapabilities.hasCapability(11)) {
            int uid = Binder.getCallingUid();
            synchronized (this.mRulesLock) {
                uidRules = this.mUidRules.get(uid, 0);
            }
            if ((uidRules & 1) != 0) {
                networkCapabilities.addCapability(11);
            }
        }
    }

    public NetworkRequest pendingRequestForNetwork(NetworkCapabilities networkCapabilities, PendingIntent operation) {
        checkNotNull(operation, "PendingIntent cannot be null.");
        NetworkCapabilities networkCapabilities2 = new NetworkCapabilities(networkCapabilities);
        enforceNetworkRequestPermissions(networkCapabilities2);
        enforceMeteredApnPolicy(networkCapabilities2);
        NetworkRequest networkRequest = new NetworkRequest(networkCapabilities2, -1, nextNetworkRequestId());
        log("pendingRequest for " + networkRequest + " to trigger " + operation);
        NetworkRequestInfo nri = new NetworkRequestInfo(networkRequest, operation, DBG);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(26, nri));
        return networkRequest;
    }

    private void releasePendingNetworkRequestWithDelay(PendingIntent operation) {
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(27, getCallingUid(), 0, operation), this.mReleasePendingIntentDelayMs);
    }

    public void releasePendingNetworkRequest(PendingIntent operation) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(27, getCallingUid(), 0, operation));
    }

    public NetworkRequest listenForNetwork(NetworkCapabilities networkCapabilities, Messenger messenger, IBinder binder) {
        enforceAccessPermission();
        NetworkRequest networkRequest = new NetworkRequest(new NetworkCapabilities(networkCapabilities), -1, nextNetworkRequestId());
        log("listenForNetwork for " + networkRequest);
        NetworkRequestInfo nri = new NetworkRequestInfo(messenger, networkRequest, binder, false);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(21, nri));
        return networkRequest;
    }

    public void pendingListenForNetwork(NetworkCapabilities networkCapabilities, PendingIntent operation) {
    }

    public void releaseNetworkRequest(NetworkRequest networkRequest) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(22, getCallingUid(), 0, networkRequest));
    }

    public void registerNetworkFactory(Messenger messenger, String name) {
        enforceConnectivityInternalPermission();
        NetworkFactoryInfo nfi = new NetworkFactoryInfo(name, messenger, new AsyncChannel());
        this.mHandler.sendMessage(this.mHandler.obtainMessage(17, nfi));
    }

    private void handleRegisterNetworkFactory(NetworkFactoryInfo nfi) {
        log("Got NetworkFactory Messenger for " + nfi.name);
        this.mNetworkFactoryInfos.put(nfi.messenger, nfi);
        nfi.asyncChannel.connect(this.mContext, this.mTrackerHandler, nfi.messenger);
    }

    public void unregisterNetworkFactory(Messenger messenger) {
        enforceConnectivityInternalPermission();
        this.mHandler.sendMessage(this.mHandler.obtainMessage(23, messenger));
    }

    private void handleUnregisterNetworkFactory(Messenger messenger) {
        NetworkFactoryInfo nfi = this.mNetworkFactoryInfos.remove(messenger);
        if (nfi == null) {
            loge("Failed to find Messenger in unregisterNetworkFactory");
        } else {
            log("unregisterNetworkFactory for " + nfi.name);
        }
    }

    private NetworkAgentInfo getDefaultNetwork() {
        return this.mNetworkForRequestId.get(this.mDefaultRequest.requestId);
    }

    private boolean isDefaultNetwork(NetworkAgentInfo nai) {
        if (nai == getDefaultNetwork()) {
            return DBG;
        }
        return false;
    }

    public void registerNetworkAgent(Messenger messenger, NetworkInfo networkInfo, LinkProperties linkProperties, NetworkCapabilities networkCapabilities, int currentScore, NetworkMisc networkMisc) {
        enforceConnectivityInternalPermission();
        NetworkAgentInfo nai = new NetworkAgentInfo(messenger, new AsyncChannel(), new NetworkInfo(networkInfo), new LinkProperties(linkProperties), new NetworkCapabilities(networkCapabilities), currentScore, this.mContext, this.mTrackerHandler, new NetworkMisc(networkMisc), this.mDefaultRequest);
        synchronized (this) {
            nai.networkMonitor.systemReady = this.mSystemReady;
        }
        log("registerNetworkAgent " + nai);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(18, nai));
    }

    private void handleRegisterNetworkAgent(NetworkAgentInfo na) {
        this.mNetworkAgentInfos.put(na.messenger, na);
        assignNextNetId(na);
        na.asyncChannel.connect(this.mContext, this.mTrackerHandler, na.messenger);
        NetworkInfo networkInfo = na.networkInfo;
        na.networkInfo = null;
        updateNetworkInfo(na, networkInfo);
    }

    private void updateLinkProperties(NetworkAgentInfo networkAgent, LinkProperties oldLp) {
        LinkProperties newLp = networkAgent.linkProperties;
        int netId = networkAgent.network.netId;
        if (networkAgent.clatd != null) {
            networkAgent.clatd.fixupLinkProperties(oldLp);
        }
        updateInterfaces(newLp, oldLp, netId);
        updateMtu(newLp, oldLp);
        updateTcpBufferSizes(networkAgent);
        boolean useDefaultDns = networkAgent.networkCapabilities.hasCapability(12);
        boolean flushDns = updateRoutes(newLp, oldLp, netId);
        updateDnses(newLp, oldLp, netId, flushDns, useDefaultDns);
        updateClat(newLp, oldLp, networkAgent);
        if (isDefaultNetwork(networkAgent)) {
            handleApplyDefaultProxy(newLp.getHttpProxy());
        } else {
            updateProxy(newLp, oldLp, networkAgent);
        }
        if (!Objects.equals(newLp, oldLp)) {
            notifyIfacesChanged();
            notifyNetworkCallbacks(networkAgent, 524295);
        }
    }

    private void updateClat(LinkProperties newLp, LinkProperties oldLp, NetworkAgentInfo nai) {
        boolean wasRunningClat = (nai.clatd == null || !nai.clatd.isStarted()) ? false : DBG;
        boolean shouldRunClat = Nat464Xlat.requiresClat(nai);
        if (!wasRunningClat && shouldRunClat) {
            nai.clatd = new Nat464Xlat(this.mContext, this.mNetd, this.mTrackerHandler, nai);
            nai.clatd.start();
        } else if (wasRunningClat && !shouldRunClat) {
            nai.clatd.stop();
        }
    }

    private void updateInterfaces(LinkProperties newLp, LinkProperties oldLp, int netId) {
        LinkProperties.CompareResult<String> interfaceDiff = new LinkProperties.CompareResult<>();
        if (oldLp != null) {
            interfaceDiff = oldLp.compareAllInterfaceNames(newLp);
        } else if (newLp != null) {
            interfaceDiff.added = newLp.getAllInterfaceNames();
        }
        for (String iface : interfaceDiff.added) {
            try {
                log("Adding iface " + iface + " to network " + netId);
                this.mNetd.addInterfaceToNetwork(iface, netId);
            } catch (Exception e) {
                loge("Exception adding interface: " + e);
            }
        }
        for (String iface2 : interfaceDiff.removed) {
            try {
                log("Removing iface " + iface2 + " from network " + netId);
                this.mNetd.removeInterfaceFromNetwork(iface2, netId);
            } catch (Exception e2) {
                loge("Exception removing interface: " + e2);
            }
        }
    }

    private boolean updateRoutes(LinkProperties newLp, LinkProperties oldLp, int netId) {
        LinkProperties.CompareResult<RouteInfo> routeDiff = new LinkProperties.CompareResult<>();
        if (oldLp != null) {
            routeDiff = oldLp.compareAllRoutes(newLp);
        } else if (newLp != null) {
            routeDiff.added = newLp.getAllRoutes();
        }
        for (RouteInfo route : routeDiff.added) {
            if (!route.hasGateway()) {
                log("Adding Route [" + route + "] to network " + netId);
                try {
                    this.mNetd.addRoute(netId, route);
                } catch (Exception e) {
                    if (route.getDestination().getAddress() instanceof Inet4Address) {
                        loge("Exception in addRoute for non-gateway: " + e);
                    }
                }
            }
        }
        for (RouteInfo route2 : routeDiff.added) {
            if (route2.hasGateway()) {
                log("Adding Route [" + route2 + "] to network " + netId);
                try {
                    this.mNetd.addRoute(netId, route2);
                } catch (Exception e2) {
                    if (route2.getGateway() instanceof Inet4Address) {
                        loge("Exception in addRoute for gateway: " + e2);
                    }
                }
            }
        }
        for (RouteInfo route3 : routeDiff.removed) {
            log("Removing Route [" + route3 + "] from network " + netId);
            try {
                this.mNetd.removeRoute(netId, route3);
            } catch (Exception e3) {
                loge("Exception in removeRoute: " + e3);
            }
        }
        if (routeDiff.added.isEmpty() && routeDiff.removed.isEmpty()) {
            return false;
        }
        return DBG;
    }

    private void updateDnses(LinkProperties newLp, LinkProperties oldLp, int netId, boolean flush, boolean useDefaultDns) {
        if (oldLp == null || !newLp.isIdenticalDnses(oldLp)) {
            Collection<InetAddress> dnses = newLp.getDnsServers();
            if (dnses.size() == 0 && this.mDefaultDns != null && useDefaultDns) {
                dnses = new ArrayList<>();
                dnses.add(this.mDefaultDns);
                loge("no dns provided for netId " + netId + ", so using defaults");
            }
            log("Setting Dns servers for network " + netId + " to " + dnses);
            try {
                this.mNetd.setDnsServersForNetwork(netId, NetworkUtils.makeStrings(dnses), newLp.getDomains());
            } catch (Exception e) {
                loge("Exception in setDnsServersForNetwork: " + e);
            }
            NetworkAgentInfo defaultNai = this.mNetworkForRequestId.get(this.mDefaultRequest.requestId);
            if (defaultNai != null && defaultNai.network.netId == netId) {
                setDefaultDnsSystemProperties(dnses);
            }
            flushVmDnsCache();
            return;
        }
        if (flush) {
            try {
                this.mNetd.flushNetworkDnsCache(netId);
            } catch (Exception e2) {
                loge("Exception in flushNetworkDnsCache: " + e2);
            }
            flushVmDnsCache();
        }
    }

    private void setDefaultDnsSystemProperties(Collection<InetAddress> dnses) {
        int last = 0;
        for (InetAddress dns : dnses) {
            last++;
            String key = "net.dns" + last;
            String value = dns.getHostAddress();
            SystemProperties.set(key, value);
        }
        for (int i = last + 1; i <= this.mNumDnsEntries; i++) {
            String key2 = "net.dns" + i;
            SystemProperties.set(key2, "");
        }
        this.mNumDnsEntries = last;
    }

    private void updateCapabilities(NetworkAgentInfo networkAgent, NetworkCapabilities networkCapabilities) {
        if (!Objects.equals(networkAgent.networkCapabilities, networkCapabilities)) {
            synchronized (networkAgent) {
                networkAgent.networkCapabilities = networkCapabilities;
            }
            rematchAllNetworksAndRequests(networkAgent, networkAgent.getCurrentScore());
            notifyNetworkCallbacks(networkAgent, 524294);
        }
    }

    private void sendUpdatedScoreToFactories(NetworkAgentInfo nai) {
        for (int i = 0; i < nai.networkRequests.size(); i++) {
            NetworkRequest nr = nai.networkRequests.valueAt(i);
            if (isRequest(nr)) {
                sendUpdatedScoreToFactories(nr, nai.getCurrentScore());
            }
        }
    }

    private void sendUpdatedScoreToFactories(NetworkRequest networkRequest, int score) {
        for (NetworkFactoryInfo nfi : this.mNetworkFactoryInfos.values()) {
            nfi.asyncChannel.sendMessage(536576, score, 0, networkRequest);
        }
    }

    private void sendPendingIntentForRequest(NetworkRequestInfo nri, NetworkAgentInfo networkAgent, int notificationType) {
        if (notificationType == 524290 && !nri.mPendingIntentSent) {
            Intent intent = new Intent();
            intent.putExtra("android.net.extra.NETWORK", networkAgent.network);
            intent.putExtra("android.net.extra.NETWORK_REQUEST", nri.request);
            nri.mPendingIntentSent = DBG;
            sendIntent(nri.mPendingIntent, intent);
        }
    }

    private void sendIntent(PendingIntent pendingIntent, Intent intent) {
        this.mPendingIntentWakeLock.acquire();
        try {
            log("Sending " + pendingIntent);
            pendingIntent.send(this.mContext, 0, intent, this, null);
        } catch (PendingIntent.CanceledException e) {
            log(pendingIntent + " was not sent, it had been canceled.");
            this.mPendingIntentWakeLock.release();
            releasePendingNetworkRequest(pendingIntent);
        }
    }

    @Override
    public void onSendFinished(PendingIntent pendingIntent, Intent intent, int resultCode, String resultData, Bundle resultExtras) {
        log("Finished sending " + pendingIntent);
        this.mPendingIntentWakeLock.release();
        releasePendingNetworkRequestWithDelay(pendingIntent);
    }

    private void callCallbackForRequest(NetworkRequestInfo nri, NetworkAgentInfo networkAgent, int notificationType) {
        if (nri.messenger != null) {
            Bundle bundle = new Bundle();
            bundle.putParcelable(NetworkRequest.class.getSimpleName(), new NetworkRequest(nri.request));
            Message msg = Message.obtain();
            if (notificationType != 524293 && notificationType != 524296) {
                bundle.putParcelable(Network.class.getSimpleName(), networkAgent.network);
            }
            switch (notificationType) {
                case 524291:
                    msg.arg1 = 30000;
                    break;
                case 524294:
                    bundle.putParcelable(NetworkCapabilities.class.getSimpleName(), new NetworkCapabilities(networkAgent.networkCapabilities));
                    break;
                case 524295:
                    bundle.putParcelable(LinkProperties.class.getSimpleName(), new LinkProperties(networkAgent.linkProperties));
                    break;
            }
            msg.what = notificationType;
            msg.setData(bundle);
            try {
                nri.messenger.send(msg);
            } catch (RemoteException e) {
                loge("RemoteException caught trying to send a callback msg for " + nri.request);
            }
        }
    }

    private void teardownUnneededNetwork(NetworkAgentInfo nai) {
        int i = 0;
        while (true) {
            if (i >= nai.networkRequests.size()) {
                break;
            }
            NetworkRequest nr = nai.networkRequests.valueAt(i);
            if (!isRequest(nr)) {
                i++;
            } else {
                loge("Dead network still had at least " + nr);
                break;
            }
        }
        nai.asyncChannel.disconnect();
    }

    private void handleLingerComplete(NetworkAgentInfo oldNetwork) {
        if (oldNetwork == null) {
            loge("Unknown NetworkAgentInfo in handleLingerComplete");
        } else {
            log("handleLingerComplete for " + oldNetwork.name());
            teardownUnneededNetwork(oldNetwork);
        }
    }

    private void makeDefault(NetworkAgentInfo newNetwork) {
        log("Switching to new default network: " + newNetwork);
        setupDataActivityTracking(newNetwork);
        try {
            this.mNetd.setDefaultNetId(newNetwork.network.netId);
        } catch (Exception e) {
            loge("Exception setting default network :" + e);
        }
        notifyLockdownVpn(newNetwork);
        handleApplyDefaultProxy(newNetwork.linkProperties.getHttpProxy());
        updateTcpBufferSizes(newNetwork);
        setDefaultDnsSystemProperties(newNetwork.linkProperties.getDnsServers());
    }

    private void rematchNetworkAndRequests(NetworkAgentInfo newNetwork, NascentState nascent, ReapUnvalidatedNetworks reapUnvalidatedNetworks) {
        if (newNetwork.created) {
            if (nascent == NascentState.JUST_VALIDATED && !newNetwork.everValidated) {
                loge("ERROR: nascent network not validated.");
            }
            boolean keep = newNetwork.isVPN();
            boolean isNewDefault = false;
            NetworkAgentInfo oldDefaultNetwork = null;
            log("rematching " + newNetwork.name());
            ArrayList<NetworkAgentInfo> affectedNetworks = new ArrayList<>();
            for (NetworkRequestInfo nri : this.mNetworkRequests.values()) {
                NetworkAgentInfo currentNetwork = this.mNetworkForRequestId.get(nri.request.requestId);
                if (newNetwork == currentNetwork) {
                    log("Network " + newNetwork.name() + " was already satisfying request " + nri.request.requestId + ". No change.");
                    keep = DBG;
                } else if (newNetwork.satisfies(nri.request)) {
                    if (!nri.isRequest) {
                        newNetwork.addRequest(nri.request);
                    } else if (currentNetwork == null || currentNetwork.getCurrentScore() < newNetwork.getCurrentScore()) {
                        if (currentNetwork != null) {
                            log("   accepting network in place of " + currentNetwork.name());
                            currentNetwork.networkRequests.remove(nri.request.requestId);
                            currentNetwork.networkLingered.add(nri.request);
                            affectedNetworks.add(currentNetwork);
                        } else {
                            log("   accepting network in place of null");
                        }
                        unlinger(newNetwork);
                        this.mNetworkForRequestId.put(nri.request.requestId, newNetwork);
                        newNetwork.addRequest(nri.request);
                        keep = DBG;
                        sendUpdatedScoreToFactories(nri.request, newNetwork.getCurrentScore());
                        if (this.mDefaultRequest.requestId == nri.request.requestId) {
                            isNewDefault = DBG;
                            oldDefaultNetwork = currentNetwork;
                        }
                    }
                }
            }
            for (NetworkAgentInfo nai : affectedNetworks) {
                if (nai.everValidated && unneeded(nai)) {
                    nai.networkMonitor.sendMessage(NetworkMonitor.CMD_NETWORK_LINGER);
                    notifyNetworkCallbacks(nai, 524291);
                } else {
                    unlinger(nai);
                }
            }
            if (keep) {
                if (isNewDefault) {
                    makeDefault(newNetwork);
                    synchronized (this) {
                        if (this.mNetTransitionWakeLock.isHeld()) {
                            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(8, this.mNetTransitionWakeLockSerialNumber, 0), 1000L);
                        }
                    }
                }
                notifyNetworkCallbacks(newNetwork, 524290);
                if (isNewDefault) {
                    if (oldDefaultNetwork != null) {
                        this.mLegacyTypeTracker.remove(oldDefaultNetwork.networkInfo.getType(), oldDefaultNetwork);
                    }
                    this.mDefaultInetConditionPublished = newNetwork.everValidated ? 100 : 0;
                    this.mLegacyTypeTracker.add(newNetwork.networkInfo.getType(), newNetwork);
                    notifyLockdownVpn(newNetwork);
                }
                try {
                    IBatteryStats bs = BatteryStatsService.getService();
                    int type = newNetwork.networkInfo.getType();
                    String baseIface = newNetwork.linkProperties.getInterfaceName();
                    bs.noteNetworkInterfaceType(baseIface, type);
                    for (LinkProperties stacked : newNetwork.linkProperties.getStackedLinks()) {
                        String stackedIface = stacked.getInterfaceName();
                        bs.noteNetworkInterfaceType(stackedIface, type);
                        NetworkStatsFactory.noteStackedIface(stackedIface, baseIface);
                    }
                } catch (RemoteException e) {
                }
                for (int i = 0; i < newNetwork.networkRequests.size(); i++) {
                    NetworkRequest nr = newNetwork.networkRequests.valueAt(i);
                    if (nr.legacyType != -1 && isRequest(nr)) {
                        this.mLegacyTypeTracker.add(nr.legacyType, newNetwork);
                    }
                }
                if (newNetwork.isVPN()) {
                    this.mLegacyTypeTracker.add(17, newNetwork);
                }
            } else if (nascent == NascentState.JUST_VALIDATED) {
                log("Validated network turns out to be unwanted.  Tear it down.");
                teardownUnneededNetwork(newNetwork);
            }
            if (reapUnvalidatedNetworks == ReapUnvalidatedNetworks.REAP) {
                for (NetworkAgentInfo nai2 : this.mNetworkAgentInfos.values()) {
                    if (!nai2.everValidated && unneeded(nai2)) {
                        log("Reaping " + nai2.name());
                        teardownUnneededNetwork(nai2);
                    }
                }
            }
        }
    }

    private void rematchAllNetworksAndRequests(NetworkAgentInfo changed, int oldScore) {
        if (changed != null && oldScore < changed.getCurrentScore()) {
            rematchNetworkAndRequests(changed, NascentState.NOT_JUST_VALIDATED, ReapUnvalidatedNetworks.REAP);
            return;
        }
        Iterator<NetworkAgentInfo> it = this.mNetworkAgentInfos.values().iterator();
        while (it.hasNext()) {
            rematchNetworkAndRequests(it.next(), NascentState.NOT_JUST_VALIDATED, it.hasNext() ? ReapUnvalidatedNetworks.DONT_REAP : ReapUnvalidatedNetworks.REAP);
        }
    }

    private void updateInetCondition(NetworkAgentInfo nai) {
        if (nai.everValidated && isDefaultNetwork(nai)) {
            int newInetCondition = nai.lastValidated ? 100 : 0;
            if (newInetCondition != this.mDefaultInetConditionPublished) {
                this.mDefaultInetConditionPublished = newInetCondition;
                sendInetConditionBroadcast(nai.networkInfo);
            }
        }
    }

    private void notifyLockdownVpn(NetworkAgentInfo nai) {
        if (this.mLockdownTracker != null) {
            if (nai != null && nai.isVPN()) {
                this.mLockdownTracker.onVpnStateChanged(nai.networkInfo);
            } else {
                this.mLockdownTracker.onNetworkInfoChanged();
            }
        }
    }

    private void updateNetworkInfo(NetworkAgentInfo networkAgent, NetworkInfo newInfo) {
        NetworkInfo oldInfo;
        NetworkInfo.State state = newInfo.getState();
        synchronized (networkAgent) {
            oldInfo = networkAgent.networkInfo;
            networkAgent.networkInfo = newInfo;
        }
        notifyLockdownVpn(networkAgent);
        if (oldInfo == null || oldInfo.getState() != state) {
            log(networkAgent.name() + " EVENT_NETWORK_INFO_CHANGED, going from " + (oldInfo == null ? "null" : oldInfo.getState()) + " to " + state);
            if (state == NetworkInfo.State.CONNECTED && !networkAgent.created) {
                try {
                    if (networkAgent.isVPN()) {
                        this.mNetd.createVirtualNetwork(networkAgent.network.netId, !networkAgent.linkProperties.getDnsServers().isEmpty(), networkAgent.networkMisc == null || !networkAgent.networkMisc.allowBypass);
                    } else {
                        this.mNetd.createPhysicalNetwork(networkAgent.network.netId);
                    }
                    networkAgent.created = DBG;
                    updateLinkProperties(networkAgent, null);
                    notifyIfacesChanged();
                    notifyNetworkCallbacks(networkAgent, 524289);
                    networkAgent.networkMonitor.sendMessage(NetworkMonitor.CMD_NETWORK_CONNECTED);
                    if (networkAgent.isVPN()) {
                        synchronized (this.mProxyLock) {
                            if (!this.mDefaultProxyDisabled) {
                                this.mDefaultProxyDisabled = DBG;
                                if (this.mGlobalProxy == null && this.mDefaultProxy != null) {
                                    sendProxyBroadcast(null);
                                }
                            }
                        }
                    }
                    rematchNetworkAndRequests(networkAgent, NascentState.NOT_JUST_VALIDATED, ReapUnvalidatedNetworks.REAP);
                    return;
                } catch (Exception e) {
                    loge("Error creating network " + networkAgent.network.netId + ": " + e.getMessage());
                    return;
                }
            }
            if (state == NetworkInfo.State.DISCONNECTED || state == NetworkInfo.State.SUSPENDED) {
                networkAgent.asyncChannel.disconnect();
                if (networkAgent.isVPN()) {
                    synchronized (this.mProxyLock) {
                        if (this.mDefaultProxyDisabled) {
                            this.mDefaultProxyDisabled = false;
                            if (this.mGlobalProxy == null && this.mDefaultProxy != null) {
                                sendProxyBroadcast(this.mDefaultProxy);
                            }
                        }
                    }
                }
            }
        }
    }

    private void updateNetworkScore(NetworkAgentInfo nai, int score) {
        log("updateNetworkScore for " + nai.name() + " to " + score);
        if (score < 0) {
            loge("updateNetworkScore for " + nai.name() + " got a negative score (" + score + ").  Bumping score to min of 0");
            score = 0;
        }
        int oldScore = nai.getCurrentScore();
        nai.setCurrentScore(score);
        rematchAllNetworksAndRequests(nai, oldScore);
        sendUpdatedScoreToFactories(nai);
    }

    protected void notifyNetworkCallback(NetworkAgentInfo nai, NetworkRequestInfo nri) {
        if (nri.mPendingIntent == null) {
            callCallbackForRequest(nri, nai, 524290);
        } else {
            sendPendingIntentForRequest(nri, nai, 524290);
        }
    }

    private void sendLegacyNetworkBroadcast(NetworkAgentInfo nai, boolean connected, int type) {
        NetworkInfo info = new NetworkInfo(nai.networkInfo);
        info.setType(type);
        if (connected) {
            info.setDetailedState(NetworkInfo.DetailedState.CONNECTED, null, info.getExtraInfo());
            sendConnectedBroadcast(info);
            return;
        }
        info.setDetailedState(NetworkInfo.DetailedState.DISCONNECTED, null, info.getExtraInfo());
        Intent intent = new Intent("android.net.conn.CONNECTIVITY_CHANGE");
        intent.putExtra("networkInfo", info);
        intent.putExtra("networkType", info.getType());
        if (info.isFailover()) {
            intent.putExtra("isFailover", DBG);
            nai.networkInfo.setFailover(false);
        }
        if (info.getReason() != null) {
            intent.putExtra("reason", info.getReason());
        }
        if (info.getExtraInfo() != null) {
            intent.putExtra("extraInfo", info.getExtraInfo());
        }
        NetworkAgentInfo newDefaultAgent = null;
        if (nai.networkRequests.get(this.mDefaultRequest.requestId) != null) {
            NetworkAgentInfo newDefaultAgent2 = this.mNetworkForRequestId.get(this.mDefaultRequest.requestId);
            newDefaultAgent = newDefaultAgent2;
            if (newDefaultAgent != null) {
                intent.putExtra("otherNetwork", newDefaultAgent.networkInfo);
            } else {
                intent.putExtra("noConnectivity", DBG);
            }
        }
        intent.putExtra("inetCondition", this.mDefaultInetConditionPublished);
        Intent immediateIntent = new Intent(intent);
        immediateIntent.setAction("android.net.conn.CONNECTIVITY_CHANGE_IMMEDIATE");
        sendStickyBroadcast(immediateIntent);
        sendStickyBroadcast(intent);
        if (newDefaultAgent != null) {
            sendConnectedBroadcast(newDefaultAgent.networkInfo);
        }
    }

    protected void notifyNetworkCallbacks(NetworkAgentInfo networkAgent, int notifyType) {
        log("notifyType " + notifyTypeToName(notifyType) + " for " + networkAgent.name());
        for (int i = 0; i < networkAgent.networkRequests.size(); i++) {
            NetworkRequest nr = networkAgent.networkRequests.valueAt(i);
            NetworkRequestInfo nri = this.mNetworkRequests.get(nr);
            if (nri.mPendingIntent == null) {
                callCallbackForRequest(nri, networkAgent, notifyType);
            } else {
                sendPendingIntentForRequest(nri, networkAgent, notifyType);
            }
        }
    }

    private String notifyTypeToName(int notifyType) {
        switch (notifyType) {
            case 524289:
                return "PRECHECK";
            case 524290:
                return "AVAILABLE";
            case 524291:
                return "LOSING";
            case 524292:
                return "LOST";
            case 524293:
                return "UNAVAILABLE";
            case 524294:
                return "CAP_CHANGED";
            case 524295:
                return "IP_CHANGED";
            case 524296:
                return "RELEASED";
            default:
                return "UNKNOWN";
        }
    }

    private void notifyIfacesChanged() {
        try {
            this.mStatsService.forceUpdateIfaces();
        } catch (Exception e) {
        }
    }

    public boolean addVpnAddress(String address, int prefixLength) {
        boolean zAddAddress;
        throwIfLockdownEnabled();
        int user = UserHandle.getUserId(Binder.getCallingUid());
        synchronized (this.mVpns) {
            zAddAddress = this.mVpns.get(user).addAddress(address, prefixLength);
        }
        return zAddAddress;
    }

    public boolean removeVpnAddress(String address, int prefixLength) {
        boolean zRemoveAddress;
        throwIfLockdownEnabled();
        int user = UserHandle.getUserId(Binder.getCallingUid());
        synchronized (this.mVpns) {
            zRemoveAddress = this.mVpns.get(user).removeAddress(address, prefixLength);
        }
        return zRemoveAddress;
    }

    public boolean setUnderlyingNetworksForVpn(Network[] networks) {
        boolean underlyingNetworks;
        throwIfLockdownEnabled();
        int user = UserHandle.getUserId(Binder.getCallingUid());
        synchronized (this.mVpns) {
            underlyingNetworks = this.mVpns.get(user).setUnderlyingNetworks(networks);
        }
        return underlyingNetworks;
    }
}
