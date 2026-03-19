package com.android.server;

import android.R;
import android.app.BroadcastOptions;
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
import android.net.Network;
import android.net.NetworkAgent;
import android.net.NetworkCapabilities;
import android.net.NetworkConfig;
import android.net.NetworkInfo;
import android.net.NetworkMisc;
import android.net.NetworkPolicyManager;
import android.net.NetworkQuotaInfo;
import android.net.NetworkRequest;
import android.net.NetworkState;
import android.net.NetworkUtils;
import android.net.ProxyInfo;
import android.net.RouteInfo;
import android.net.UidRange;
import android.net.Uri;
import android.net.metrics.DefaultNetworkEvent;
import android.net.metrics.NetworkEvent;
import android.os.BenesseExtension;
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
import android.os.ResultReceiver;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.security.KeyStore;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.LocalLog;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.Xml;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.app.IBatteryStats;
import com.android.internal.net.LegacyVpnInfo;
import com.android.internal.net.NetworkStatsFactory;
import com.android.internal.net.VpnConfig;
import com.android.internal.net.VpnInfo;
import com.android.internal.net.VpnProfile;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.MessageUtils;
import com.android.internal.util.XmlUtils;
import com.android.server.am.BatteryStatsService;
import com.android.server.audio.AudioService;
import com.android.server.connectivity.DataConnectionStats;
import com.android.server.connectivity.KeepaliveTracker;
import com.android.server.connectivity.Nat464Xlat;
import com.android.server.connectivity.NetworkAgentInfo;
import com.android.server.connectivity.NetworkDiagnostics;
import com.android.server.connectivity.NetworkMonitor;
import com.android.server.connectivity.PacManager;
import com.android.server.connectivity.PermissionMonitor;
import com.android.server.connectivity.Tethering;
import com.android.server.connectivity.Vpn;
import com.android.server.net.BaseNetworkObserver;
import com.android.server.net.LockdownVpnTracker;
import com.android.server.net.NetworkHttpMonitor;
import com.android.server.pm.PackageManagerService;
import com.android.server.policy.PhoneWindowManager;
import com.google.android.collect.Lists;
import com.mediatek.common.MPlugin;
import com.mediatek.common.net.IConnectivityServiceExt;
import com.mediatek.datashaping.DataShapingUtils;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class ConnectivityService extends IConnectivityManager.Stub implements PendingIntent.OnFinished {
    private static final String ATTR_MCC = "mcc";
    private static final String ATTR_MNC = "mnc";
    private static final boolean DBG = true;
    private static final String DEFAULT_TCP_BUFFER_SIZES = "4096,87380,110208,4096,16384,110208";
    private static final String DEFAULT_TCP_RWND_KEY = "net.tcp.default_init_rwnd";
    private static final int DISABLED = 0;
    private static final int ENABLED = 1;
    private static final int EVENT_APPLY_GLOBAL_HTTP_PROXY = 9;
    private static final int EVENT_CHANGE_MOBILE_DATA_ENABLED = 2;
    private static final int EVENT_CLEAR_NET_TRANSITION_WAKELOCK = 8;
    private static final int EVENT_CONFIGURE_MOBILE_DATA_ALWAYS_ON = 30;
    private static final int EVENT_ENABLE_MOBILE_DATA_FOR_TETHERING = 100;
    private static final int EVENT_EXPIRE_NET_TRANSITION_WAKELOCK = 24;
    private static final int EVENT_PROMPT_UNVALIDATED = 29;
    private static final int EVENT_PROXY_HAS_CHANGED = 16;
    private static final int EVENT_REGISTER_NETWORK_AGENT = 18;
    private static final int EVENT_REGISTER_NETWORK_FACTORY = 17;
    private static final int EVENT_REGISTER_NETWORK_LISTENER = 21;
    private static final int EVENT_REGISTER_NETWORK_LISTENER_WITH_INTENT = 31;
    private static final int EVENT_REGISTER_NETWORK_REQUEST = 19;
    private static final int EVENT_REGISTER_NETWORK_REQUEST_WITH_INTENT = 26;
    private static final int EVENT_RELEASE_NETWORK_REQUEST = 22;
    private static final int EVENT_RELEASE_NETWORK_REQUEST_WITH_INTENT = 27;
    private static final int EVENT_SET_ACCEPT_UNVALIDATED = 28;
    private static final int EVENT_SYSTEM_READY = 25;
    private static final int EVENT_TIMEOUT_NETWORK_REQUEST = 20;
    private static final int EVENT_UNREGISTER_NETWORK_FACTORY = 23;
    private static final int INET_CONDITION_LOG_MAX_SIZE = 15;
    private static final boolean LOGD_BLOCKED_NETWORKINFO;
    private static final boolean LOGD_RULES;
    private static final int MAX_NETWORK_REQUESTS_PER_UID = 100;
    private static final int MAX_NETWORK_REQUEST_LOGS = 20;
    private static final int MAX_NET_ID = 65535;
    private static final int MAX_VALIDATION_LOGS = 10;
    private static final int MIN_NET_ID = 100;
    private static final String NETWORK_RESTORE_DELAY_PROP_NAME = "android.telephony.apn-restore";
    private static final String NOTIFICATION_ID = "CaptivePortal.Notification";
    private static final int PROMPT_UNVALIDATED_DELAY_MS = 8000;
    private static final String PROP_FORCE_DEBUG_KEY = "persist.log.tag.tel_dbg";
    private static final String PROVISIONING_URL_PATH = "/data/misc/radio/provisioning_urls.xml";
    private static final int RESTORE_DEFAULT_NETWORK_DELAY = 60000;
    private static final String TAG = "ConnectivityService";
    private static final String TAG_PROVISIONING_URL = "provisioningUrl";
    private static final String TAG_PROVISIONING_URLS = "provisioningUrls";
    private static final boolean VDBG;
    private static NetworkHttpMonitor mNetworkHttpMonitor;
    private static boolean mSkipNetworkValidation;
    private static boolean sIsAutoTethering;
    private static final SparseArray<String> sMagicDecoderRing;
    private static ConnectivityService sServiceInstance;
    private final Context mContext;
    private String mCurrentTcpBufferSizes;
    private DataConnectionStats mDataConnectionStats;
    private InetAddress mDefaultDns;
    private final NetworkRequest mDefaultMobileDataRequest;
    private final NetworkRequest mDefaultRequest;
    private final InternalHandler mHandler;
    protected final HandlerThread mHandlerThread;
    private ArrayList mInetLog;
    private Intent mInitialBroadcast;
    private KeepaliveTracker mKeepaliveTracker;
    private KeyStore mKeyStore;
    private Object mLegacyNetworkSyncObject;
    private boolean mLockdownEnabled;
    private LockdownVpnTracker mLockdownTracker;
    NetworkConfig[] mNetConfigs;
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
    private BroadcastReceiver mReceiver;
    private final int mReleasePendingIntentDelayMs;

    @GuardedBy("mRulesLock")
    private boolean mRestrictBackground;
    private final SettingsObserver mSettingsObserver;
    private INetworkStatsService mStatsService;
    private Object mSynchronizedObject;
    private boolean mSystemReady;
    TelephonyManager mTelephonyManager;
    private boolean mTestMode;
    private Tethering mTethering;
    private final NetworkStateTrackerHandler mTrackerHandler;
    private UserManager mUserManager;

    @GuardedBy("mVpns")
    private final SparseArray<Vpn> mVpns = new SparseArray<>();
    private Object mRulesLock = new Object();

    @GuardedBy("mRulesLock")
    private SparseIntArray mUidRules = new SparseIntArray();

    @GuardedBy("mRulesLock")
    private ArraySet<String> mMeteredIfaces = new ArraySet<>();
    private int mDefaultInetConditionPublished = 0;
    private String mNetTransitionWakeLockCausedBy = "";
    private volatile ProxyInfo mDefaultProxy = null;
    private Object mProxyLock = new Object();
    private boolean mDefaultProxyDisabled = false;
    private ProxyInfo mGlobalProxy = null;
    private int mNextNetId = 100;
    private int mNextNetworkRequestId = 1;
    private Object mRequestLock = new Object();
    IConnectivityServiceExt mIcsExt = null;
    private final LocalLog mNetworkRequestInfoLogs = new LocalLog(20);
    private final ArrayDeque<ValidationLog> mValidationLogs = new ArrayDeque<>(10);
    private LegacyTypeTracker mLegacyTypeTracker = new LegacyTypeTracker();
    private INetworkManagementEventObserver mDataActivityObserver = new BaseNetworkObserver() {
        public void interfaceClassDataActivityChanged(String label, boolean active, long tsNanos) {
            int deviceType = Integer.parseInt(label);
            ConnectivityService.this.sendDataActivityBroadcast(deviceType, active, tsNanos);
        }
    };
    private INetworkPolicyListener mPolicyListener = new INetworkPolicyListener.Stub() {
        public void onUidRulesChanged(int uid, int uidRules) {
            if (ConnectivityService.LOGD_RULES) {
                ConnectivityService.log("onUidRulesChanged(uid=" + uid + ", uidRules=" + uidRules + ")");
            }
            synchronized (ConnectivityService.this.mRulesLock) {
                int oldRules = ConnectivityService.this.mUidRules.get(uid, 0);
                if (oldRules == uidRules) {
                    return;
                }
                if (uidRules == 0) {
                    ConnectivityService.this.mUidRules.delete(uid);
                } else {
                    ConnectivityService.this.mUidRules.put(uid, uidRules);
                }
            }
        }

        public void onMeteredIfacesChanged(String[] meteredIfaces) {
            if (ConnectivityService.LOGD_RULES) {
                ConnectivityService.log("onMeteredIfacesChanged(ifaces=" + Arrays.toString(meteredIfaces) + ")");
            }
            synchronized (ConnectivityService.this.mRulesLock) {
                ConnectivityService.this.mMeteredIfaces.clear();
                for (String iface : meteredIfaces) {
                    ConnectivityService.this.mMeteredIfaces.add(iface);
                }
            }
        }

        public void onRestrictBackgroundChanged(boolean restrictBackground) {
            if (ConnectivityService.LOGD_RULES) {
                ConnectivityService.log("onRestrictBackgroundChanged(restrictBackground=" + restrictBackground + ")");
            }
            synchronized (ConnectivityService.this.mRulesLock) {
                ConnectivityService.this.mRestrictBackground = restrictBackground;
            }
            if (!restrictBackground) {
                return;
            }
            ConnectivityService.log("onRestrictBackgroundChanged(true): disabling tethering");
            ConnectivityService.this.mTethering.untetherAll();
        }

        public void onRestrictBackgroundWhitelistChanged(int uid, boolean whitelisted) {
            if (!ConnectivityService.LOGD_RULES) {
                return;
            }
            ConnectivityService.log("onRestrictBackgroundWhitelistChanged(uid=" + uid + ", whitelisted=" + whitelisted + ")");
        }

        public void onRestrictBackgroundBlacklistChanged(int uid, boolean blacklisted) {
            if (!ConnectivityService.LOGD_RULES) {
                return;
            }
            ConnectivityService.log("onRestrictBackgroundBlacklistChanged(uid=" + uid + ", blacklisted=" + blacklisted + ")");
        }
    };
    private final File mProvisioningUrlFile = new File(PROVISIONING_URL_PATH);
    private BroadcastReceiver mUserIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int userId = intent.getIntExtra("android.intent.extra.user_handle", -10000);
            if (userId == -10000) {
                return;
            }
            if ("android.intent.action.USER_STARTED".equals(action)) {
                ConnectivityService.this.onUserStart(userId);
                return;
            }
            if ("android.intent.action.USER_STOPPED".equals(action)) {
                ConnectivityService.this.onUserStop(userId);
                return;
            }
            if ("android.intent.action.USER_ADDED".equals(action)) {
                ConnectivityService.this.onUserAdded(userId);
            } else if ("android.intent.action.USER_REMOVED".equals(action)) {
                ConnectivityService.this.onUserRemoved(userId);
            } else {
                if (!"android.intent.action.USER_UNLOCKED".equals(action)) {
                    return;
                }
                ConnectivityService.this.onUserUnlocked(userId);
            }
        }
    };
    private final HashMap<Messenger, NetworkFactoryInfo> mNetworkFactoryInfos = new HashMap<>();
    private final HashMap<NetworkRequest, NetworkRequestInfo> mNetworkRequests = new HashMap<>();

    @GuardedBy("mUidToNetworkRequestCount")
    private final SparseIntArray mUidToNetworkRequestCount = new SparseIntArray();
    private final SparseArray<NetworkAgentInfo> mNetworkForRequestId = new SparseArray<>();

    @GuardedBy("mNetworkForNetId")
    private final SparseArray<NetworkAgentInfo> mNetworkForNetId = new SparseArray<>();

    @GuardedBy("mNetworkForNetId")
    private final SparseBooleanArray mNetIdInUse = new SparseBooleanArray();
    private final HashMap<Messenger, NetworkAgentInfo> mNetworkAgentInfos = new HashMap<>();

    @GuardedBy("mBlockedAppUids")
    private final HashSet<Integer> mBlockedAppUids = new HashSet<>();

    static {
        VDBG = SystemProperties.getInt(PROP_FORCE_DEBUG_KEY, 0) == 1;
        LOGD_RULES = VDBG;
        LOGD_BLOCKED_NETWORKINFO = VDBG;
        sMagicDecoderRing = MessageUtils.findMessageNames(new Class[]{AsyncChannel.class, ConnectivityService.class, NetworkAgent.class});
        mSkipNetworkValidation = true;
        sIsAutoTethering = SystemProperties.getBoolean("persist.net.auto.tethering", false);
    }

    private enum ReapUnvalidatedNetworks {
        REAP,
        DONT_REAP;

        public static ReapUnvalidatedNetworks[] valuesCustom() {
            return values();
        }
    }

    private static class ValidationLog {
        final LocalLog.ReadOnlyLocalLog mLog;
        final Network mNetwork;
        final String mNetworkExtraInfo;

        ValidationLog(Network network, String networkExtraInfo, LocalLog.ReadOnlyLocalLog log) {
            this.mNetwork = network;
            this.mNetworkExtraInfo = networkExtraInfo;
            this.mLog = log;
        }
    }

    private void addValidationLogs(LocalLog.ReadOnlyLocalLog log, Network network, String networkExtraInfo) {
        synchronized (this.mValidationLogs) {
            while (this.mValidationLogs.size() >= 10) {
                this.mValidationLogs.removeLast();
            }
            this.mValidationLogs.addFirst(new ValidationLog(network, networkExtraInfo, log));
        }
    }

    private class LegacyTypeTracker {
        private static final boolean DBG = true;
        private ArrayList<NetworkAgentInfo>[] mTypeLists = new ArrayList[50];

        public LegacyTypeTracker() {
        }

        public void addSupportedType(int type) {
            if (this.mTypeLists[type] != null) {
                throw new IllegalStateException("legacy list for type " + type + "already initialized");
            }
            this.mTypeLists[type] = new ArrayList<>();
        }

        public boolean isTypeSupported(int type) {
            return ConnectivityManager.isNetworkTypeValid(type) && this.mTypeLists[type] != null;
        }

        public NetworkAgentInfo getNetworkForType(int type) {
            synchronized (ConnectivityService.this.mLegacyNetworkSyncObject) {
                if (ConnectivityService.VDBG) {
                    ConnectivityService.log("getNetworkForType type " + type);
                }
                if (isTypeSupported(type) && !this.mTypeLists[type].isEmpty()) {
                    return this.mTypeLists[type].get(0);
                }
                return null;
            }
        }

        private void maybeLogBroadcast(NetworkAgentInfo nai, NetworkInfo.DetailedState state, int type, boolean isDefaultNetwork) {
            ConnectivityService.log("Sending " + state + " broadcast for type " + type + " " + nai.name() + " isDefaultNetwork=" + isDefaultNetwork);
        }

        public void add(int type, NetworkAgentInfo nai) {
            synchronized (ConnectivityService.this.mLegacyNetworkSyncObject) {
                if (!isTypeSupported(type)) {
                    return;
                }
                if (ConnectivityService.VDBG) {
                    ConnectivityService.log("Adding agent " + nai + " for legacy network type " + type);
                }
                ArrayList<NetworkAgentInfo> list = this.mTypeLists[type];
                if (list.contains(nai)) {
                    ConnectivityService.loge("Attempting to register duplicate agent for type " + type + ": " + nai);
                    return;
                }
                list.add(nai);
                boolean isDefaultNetwork = ConnectivityService.this.isDefaultNetwork(nai);
                if (list.size() == 1 || isDefaultNetwork) {
                    maybeLogBroadcast(nai, NetworkInfo.DetailedState.CONNECTED, type, isDefaultNetwork);
                    ConnectivityService.this.sendLegacyNetworkBroadcast(nai, NetworkInfo.DetailedState.CONNECTED, type);
                }
            }
        }

        public void remove(int type, NetworkAgentInfo nai, boolean wasDefault) {
            synchronized (ConnectivityService.this.mLegacyNetworkSyncObject) {
                ArrayList<NetworkAgentInfo> list = this.mTypeLists[type];
                if (list == null || list.isEmpty()) {
                    return;
                }
                boolean wasFirstNetwork = list.get(0).equals(nai);
                if (!list.remove(nai)) {
                    return;
                }
                NetworkInfo.DetailedState state = NetworkInfo.DetailedState.DISCONNECTED;
                if (wasFirstNetwork || wasDefault) {
                    maybeLogBroadcast(nai, state, type, wasDefault);
                    ConnectivityService.this.sendLegacyNetworkBroadcast(nai, state, type);
                }
                if (!list.isEmpty() && wasFirstNetwork) {
                    ConnectivityService.log("Other network available for type " + type + ", sending connected broadcast");
                    NetworkAgentInfo replacement = list.get(0);
                    maybeLogBroadcast(replacement, state, type, ConnectivityService.this.isDefaultNetwork(replacement));
                    ConnectivityService.this.sendLegacyNetworkBroadcast(replacement, state, type);
                }
            }
        }

        public void remove(NetworkAgentInfo nai, boolean wasDefault) {
            if (ConnectivityService.VDBG) {
                ConnectivityService.log("Removing agent " + nai + " wasDefault=" + wasDefault);
            }
            for (int type = 0; type < this.mTypeLists.length; type++) {
                remove(type, nai, wasDefault);
            }
        }

        public void update(NetworkAgentInfo nai) {
            boolean isDefault = ConnectivityService.this.isDefaultNetwork(nai);
            NetworkInfo.DetailedState state = nai.networkInfo.getDetailedState();
            for (int type = 0; type < this.mTypeLists.length; type++) {
                ArrayList<NetworkAgentInfo> list = this.mTypeLists[type];
                boolean zContains = list != null ? list.contains(nai) : false;
                boolean isFirst = (list == null || list.size() <= 0) ? false : nai == list.get(0);
                if (isFirst || (zContains && isDefault)) {
                    maybeLogBroadcast(nai, state, type, isDefault);
                    ConnectivityService.this.sendLegacyNetworkBroadcast(nai, state, type);
                }
            }
        }

        private String naiToString(NetworkAgentInfo nai) {
            String state;
            String name = nai != null ? nai.name() : "null";
            if (nai.networkInfo != null) {
                state = nai.networkInfo.getState() + "/" + nai.networkInfo.getDetailedState();
            } else {
                state = "???/???";
            }
            return name + " " + state;
        }

        public void dump(IndentingPrintWriter pw) {
            pw.println("mLegacyTypeTracker:");
            pw.increaseIndent();
            pw.print("Supported types:");
            for (int type = 0; type < this.mTypeLists.length; type++) {
                if (this.mTypeLists[type] != null) {
                    pw.print(" " + type);
                }
            }
            pw.println();
            pw.println("Current state:");
            pw.increaseIndent();
            for (int type2 = 0; type2 < this.mTypeLists.length; type2++) {
                if (this.mTypeLists[type2] != null && this.mTypeLists[type2].size() != 0) {
                    for (NetworkAgentInfo nai : this.mTypeLists[type2]) {
                        pw.println(type2 + " " + naiToString(nai));
                    }
                }
            }
            pw.decreaseIndent();
            pw.decreaseIndent();
            pw.println();
        }
    }

    protected HandlerThread createHandlerThread() {
        return new HandlerThread("ConnectivityServiceThread");
    }

    public ConnectivityService(Context context, INetworkManagementService netManager, INetworkStatsService statsService, INetworkPolicyManager policyManager) {
        boolean zEquals;
        String id;
        this.mPacManager = null;
        log("ConnectivityService starting up");
        this.mDefaultRequest = createInternetRequestForTransport(-1);
        NetworkRequestInfo defaultNRI = new NetworkRequestInfo(null, this.mDefaultRequest, new Binder(), NetworkRequestType.REQUEST);
        boolean isCcpMode = SystemProperties.getBoolean("persist.op12.ccp.mode", false);
        if (isCcpMode) {
            log("isCcpMode enabled, don't assign default networkRequest");
        } else if (sIsAutoTethering) {
            log("Delay default network request until boot is completed");
        } else {
            this.mNetworkRequests.put(this.mDefaultRequest, defaultNRI);
        }
        this.mNetworkRequestInfoLogs.log("REGISTER " + defaultNRI);
        this.mDefaultMobileDataRequest = createInternetRequestForTransport(0);
        this.mHandlerThread = createHandlerThread();
        this.mHandlerThread.start();
        this.mHandler = new InternalHandler(this.mHandlerThread.getLooper());
        this.mTrackerHandler = new NetworkStateTrackerHandler(this.mHandlerThread.getLooper());
        if (TextUtils.isEmpty(SystemProperties.get("net.hostname")) && (id = Settings.Secure.getString(context.getContentResolver(), "android_id")) != null && id.length() > 0) {
            String name = new String("android-").concat(id);
            SystemProperties.set("net.hostname", name);
        }
        this.mReleasePendingIntentDelayMs = Settings.Secure.getInt(context.getContentResolver(), "connectivity_release_pending_intent_delay_ms", 5000);
        this.mContext = (Context) checkNotNull(context, "missing Context");
        this.mNetd = (INetworkManagementService) checkNotNull(netManager, "missing INetworkManagementService");
        this.mStatsService = (INetworkStatsService) checkNotNull(statsService, "missing INetworkStatsService");
        this.mPolicyManager = (INetworkPolicyManager) checkNotNull(policyManager, "missing INetworkPolicyManager");
        this.mKeyStore = KeyStore.getInstance();
        this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        try {
            this.mPolicyManager.setConnectivityListener(this.mPolicyListener);
            this.mRestrictBackground = this.mPolicyManager.getRestrictBackground();
        } catch (RemoteException e) {
            loge("unable to register INetworkPolicyListener" + e);
        }
        PowerManager powerManager = (PowerManager) context.getSystemService("power");
        this.mNetTransitionWakeLock = powerManager.newWakeLock(1, TAG);
        this.mNetTransitionWakeLockTimeout = this.mContext.getResources().getInteger(R.integer.config_MaxConcurrentDownloadsAllowed);
        this.mPendingIntentWakeLock = powerManager.newWakeLock(1, TAG);
        this.mNetConfigs = new NetworkConfig[50];
        boolean wifiOnly = SystemProperties.getBoolean("ro.radio.noril", false);
        log("wifiOnly=" + wifiOnly);
        String[] naStrings = context.getResources().getStringArray(R.array.config_ambientThresholdLevels);
        for (String naString : naStrings) {
            try {
                NetworkConfig n = new NetworkConfig(naString);
                if (VDBG) {
                    log("naString=" + naString + " config=" + n);
                }
                if (n.type > 49) {
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
            } catch (Exception e2) {
            }
        }
        if (this.mNetConfigs[17] == null) {
            this.mLegacyTypeTracker.addSupportedType(17);
            this.mNetworksDefined++;
        }
        if (VDBG) {
            log("mNetworksDefined=" + this.mNetworksDefined);
        }
        this.mProtectedNetworks = new ArrayList();
        int[] protectedNetworks = context.getResources().getIntArray(R.array.config_ambientThresholdsOfPeakRefreshRate);
        for (int p : protectedNetworks) {
            if (this.mNetConfigs[p] != null && !this.mProtectedNetworks.contains(Integer.valueOf(p))) {
                this.mProtectedNetworks.add(Integer.valueOf(p));
            } else {
                loge("Ignoring protectedNetwork " + p);
            }
        }
        if (!SystemProperties.get("cm.test.mode").equals("true")) {
            zEquals = false;
        } else {
            zEquals = SystemProperties.get("ro.build.type").equals("eng");
        }
        this.mTestMode = zEquals;
        this.mTethering = new Tethering(this.mContext, this.mNetd, statsService);
        this.mPermissionMonitor = new PermissionMonitor(this.mContext, this.mNetd);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_STARTED");
        intentFilter.addAction("android.intent.action.USER_STOPPED");
        intentFilter.addAction("android.intent.action.USER_ADDED");
        intentFilter.addAction("android.intent.action.USER_REMOVED");
        intentFilter.addAction("android.intent.action.USER_UNLOCKED");
        this.mContext.registerReceiverAsUser(this.mUserIntentReceiver, UserHandle.ALL, intentFilter, null, null);
        try {
            this.mNetd.registerObserver(this.mTethering);
            this.mNetd.registerObserver(this.mDataActivityObserver);
        } catch (RemoteException e3) {
            loge("Error registering observer :" + e3);
        }
        this.mInetLog = new ArrayList();
        this.mSettingsObserver = new SettingsObserver(this.mContext, this.mHandler);
        registerSettingsCallbacks();
        this.mDataConnectionStats = new DataConnectionStats(this.mContext);
        this.mDataConnectionStats.startMonitoring();
        this.mPacManager = new PacManager(this.mContext, this.mHandler, 16);
        this.mUserManager = (UserManager) context.getSystemService("user");
        this.mKeepaliveTracker = new KeepaliveTracker(this.mHandler);
        IntentFilter filterC = new IntentFilter();
        filterC.addAction("android.intent.action.TETHERING_CHANGED");
        if (sIsAutoTethering) {
            filterC.addAction("android.intent.action.BOOT_COMPLETED");
            filterC.addAction("android.intent.action.ACTION_BOOT_IPO");
        }
        this.mReceiver = new ConnectivityServiceReceiver(this, null);
        this.mContext.registerReceiver(this.mReceiver, filterC);
        this.mSynchronizedObject = new Object();
        this.mLegacyNetworkSyncObject = new Object();
        mSkipNetworkValidation = this.mContext.getResources().getBoolean(com.mediatek.internal.R.bool.config_skip_network_validation);
        String dns = this.mContext.getResources().getString(R.string.config_defaultRetailDemo);
        try {
            this.mDefaultDns = NetworkUtils.numericToInetAddress(dns);
        } catch (IllegalArgumentException e4) {
            loge("Error setting defaultDns using " + dns);
        }
    }

    private NetworkRequest createInternetRequestForTransport(int transportType) {
        NetworkCapabilities netCap = new NetworkCapabilities();
        netCap.addCapability(12);
        netCap.addCapability(13);
        if (transportType > -1) {
            netCap.addTransportType(transportType);
        }
        return new NetworkRequest(netCap, -1, nextNetworkRequestId());
    }

    private void handleMobileDataAlwaysOn() {
        boolean enable = Settings.Global.getInt(this.mContext.getContentResolver(), "mobile_data_always_on", 0) == 1;
        boolean isEnabled = this.mNetworkRequests.get(this.mDefaultMobileDataRequest) != null;
        if (enable == isEnabled) {
            return;
        }
        if (enable) {
            handleRegisterNetworkRequest(new NetworkRequestInfo(null, this.mDefaultMobileDataRequest, new Binder(), NetworkRequestType.REQUEST));
        } else {
            handleReleaseNetworkRequest(this.mDefaultMobileDataRequest, 1000);
        }
    }

    private void registerSettingsCallbacks() {
        this.mSettingsObserver.observe(Settings.Global.getUriFor("http_proxy"), 9);
        this.mSettingsObserver.observe(Settings.Global.getUriFor("mobile_data_always_on"), 30);
    }

    private synchronized int nextNetworkRequestId() {
        int i;
        i = this.mNextNetworkRequestId;
        this.mNextNetworkRequestId = i + 1;
        return i;
    }

    protected int reserveNetId() {
        synchronized (this.mNetworkForNetId) {
            for (int i = 100; i <= MAX_NET_ID; i++) {
                int netId = this.mNextNetId;
                int i2 = this.mNextNetId + 1;
                this.mNextNetId = i2;
                if (i2 > MAX_NET_ID) {
                    this.mNextNetId = 100;
                }
                if (!this.mNetIdInUse.get(netId)) {
                    this.mNetIdInUse.put(netId, true);
                    return netId;
                }
            }
            throw new IllegalStateException("No free netIds");
        }
    }

    private NetworkState getFilteredNetworkState(int networkType, int uid, boolean ignoreBlocked) {
        NetworkState state;
        if (this.mLegacyTypeTracker.isTypeSupported(networkType)) {
            NetworkAgentInfo nai = this.mLegacyTypeTracker.getNetworkForType(networkType);
            if (nai != null) {
                state = nai.getNetworkState();
                state.networkInfo.setType(networkType);
            } else {
                NetworkInfo info = new NetworkInfo(networkType, 0, ConnectivityManager.getNetworkTypeName(networkType), "");
                info.setDetailedState(NetworkInfo.DetailedState.DISCONNECTED, null, null);
                info.setIsAvailable(true);
                state = new NetworkState(info, new LinkProperties(), new NetworkCapabilities(), (Network) null, (String) null, (String) null);
            }
            filterNetworkStateForUid(state, uid, ignoreBlocked);
            return state;
        }
        return NetworkState.EMPTY;
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

    private NetworkState getUnfilteredActiveNetworkState(int uid) {
        NetworkAgentInfo nai = getDefaultNetwork();
        Network[] networks = getVpnUnderlyingNetworks(uid);
        if (networks != null) {
            if (networks.length > 0) {
                nai = getNetworkAgentInfoForNetwork(networks[0]);
            } else {
                nai = null;
            }
        }
        if (nai != null) {
            return nai.getNetworkState();
        }
        return NetworkState.EMPTY;
    }

    private boolean isNetworkWithLinkPropertiesBlocked(LinkProperties lp, int uid, boolean ignoreBlocked) {
        boolean networkMetered;
        int uidRules;
        if (ignoreBlocked || isSystem(uid)) {
            return false;
        }
        synchronized (this.mVpns) {
            Vpn vpn = this.mVpns.get(UserHandle.getUserId(uid));
            if (vpn != null) {
                if (vpn.isBlockingUid(uid)) {
                    return true;
                }
            }
            String iface = lp == null ? "" : lp.getInterfaceName();
            synchronized (this.mRulesLock) {
                networkMetered = this.mMeteredIfaces.contains(iface);
                uidRules = this.mUidRules.get(uid, 0);
            }
            boolean allowed = true;
            if (networkMetered) {
                if ((uidRules & 4) != 0) {
                    if (LOGD_RULES) {
                        Log.d(TAG, "uid " + uid + " is blacklisted");
                    }
                    allowed = false;
                } else {
                    allowed = (this.mRestrictBackground && (uidRules & 1) == 0 && (uidRules & 2) == 0) ? false : true;
                    if (LOGD_RULES) {
                        Log.d(TAG, "allowed status for uid " + uid + " when mRestrictBackground=" + this.mRestrictBackground + ", whitelisted=" + ((uidRules & 1) != 0) + ", tempWhitelist= + ((uidRules & RULE_TEMPORARY_ALLOW_METERED) != 0): " + allowed);
                    }
                }
            }
            if (allowed) {
                allowed = (uidRules & 64) == 0;
                if (LOGD_RULES) {
                    Log.d(TAG, "allowed status for uid " + uid + " when rule is " + NetworkPolicyManager.uidRulesToString(uidRules) + ": " + allowed);
                }
            }
            return !allowed;
        }
    }

    private void maybeLogBlockedNetworkInfo(NetworkInfo ni, int uid) {
        if (ni == null || !LOGD_BLOCKED_NETWORKINFO) {
            return;
        }
        boolean removed = false;
        boolean added = false;
        synchronized (this.mBlockedAppUids) {
            if (ni.getDetailedState() == NetworkInfo.DetailedState.BLOCKED && this.mBlockedAppUids.add(Integer.valueOf(uid))) {
                added = true;
            } else if (ni.isConnected()) {
                if (this.mBlockedAppUids.remove(Integer.valueOf(uid))) {
                    removed = true;
                }
            }
        }
        if (added) {
            log("Returning blocked NetworkInfo to uid=" + uid);
        } else if (removed) {
            log("Returning unblocked NetworkInfo to uid=" + uid);
        }
    }

    private void filterNetworkStateForUid(NetworkState state, int uid, boolean ignoreBlocked) {
        if (state == null || state.networkInfo == null || state.linkProperties == null) {
            return;
        }
        if (isNetworkWithLinkPropertiesBlocked(state.linkProperties, uid, ignoreBlocked)) {
            state.networkInfo.setDetailedState(NetworkInfo.DetailedState.BLOCKED, null, null);
        }
        if (this.mLockdownTracker != null) {
            this.mLockdownTracker.augmentNetworkInfo(state.networkInfo);
        }
        long token = Binder.clearCallingIdentity();
        try {
            state.networkInfo.setMetered(this.mPolicyManager.isNetworkMetered(state));
        } catch (RemoteException e) {
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public NetworkInfo getActiveNetworkInfo() {
        enforceAccessPermission();
        int uid = Binder.getCallingUid();
        NetworkState state = getUnfilteredActiveNetworkState(uid);
        filterNetworkStateForUid(state, uid, false);
        maybeLogBlockedNetworkInfo(state.networkInfo, uid);
        log("getActiveNetworkInfo networkInfo = " + state.networkInfo);
        return state.networkInfo;
    }

    public Network getActiveNetwork() {
        enforceAccessPermission();
        return getActiveNetworkForUidInternal(Binder.getCallingUid(), false);
    }

    public Network getActiveNetworkForUid(int uid, boolean ignoreBlocked) {
        enforceConnectivityInternalPermission();
        return getActiveNetworkForUidInternal(uid, ignoreBlocked);
    }

    private Network getActiveNetworkForUidInternal(int uid, boolean ignoreBlocked) {
        NetworkAgentInfo nai;
        int user = UserHandle.getUserId(uid);
        int vpnNetId = 0;
        synchronized (this.mVpns) {
            Vpn vpn = this.mVpns.get(user);
            if (vpn != null && vpn.appliesToUid(uid)) {
                vpnNetId = vpn.getNetId();
            }
        }
        if (vpnNetId != 0) {
            synchronized (this.mNetworkForNetId) {
                nai = this.mNetworkForNetId.get(vpnNetId);
            }
            if (nai != null) {
                return nai.network;
            }
        }
        NetworkAgentInfo nai2 = getDefaultNetwork();
        if (nai2 != null && isNetworkWithLinkPropertiesBlocked(nai2.linkProperties, uid, ignoreBlocked)) {
            nai2 = null;
        }
        log("getActiveNetworkForUidInternal nai = " + (nai2 == null ? "null" : nai2.name()));
        if (nai2 != null) {
            return nai2.network;
        }
        return null;
    }

    public NetworkInfo getActiveNetworkInfoUnfiltered() {
        enforceAccessPermission();
        int uid = Binder.getCallingUid();
        NetworkState state = getUnfilteredActiveNetworkState(uid);
        return state.networkInfo;
    }

    public NetworkInfo getActiveNetworkInfoForUid(int uid, boolean ignoreBlocked) {
        enforceConnectivityInternalPermission();
        NetworkState state = getUnfilteredActiveNetworkState(uid);
        filterNetworkStateForUid(state, uid, ignoreBlocked);
        return state.networkInfo;
    }

    public NetworkInfo getNetworkInfo(int networkType) {
        enforceAccessPermission();
        int uid = Binder.getCallingUid();
        if (getVpnUnderlyingNetworks(uid) != null) {
            NetworkState state = getUnfilteredActiveNetworkState(uid);
            if (state.networkInfo != null && state.networkInfo.getType() == networkType) {
                filterNetworkStateForUid(state, uid, false);
                return state.networkInfo;
            }
        }
        return getFilteredNetworkState(networkType, uid, false).networkInfo;
    }

    public NetworkInfo getNetworkInfoForUid(Network network, int uid, boolean ignoreBlocked) {
        enforceAccessPermission();
        NetworkAgentInfo nai = getNetworkAgentInfoForNetwork(network);
        if (nai == null) {
            return null;
        }
        NetworkState state = nai.getNetworkState();
        filterNetworkStateForUid(state, uid, ignoreBlocked);
        return state.networkInfo;
    }

    public NetworkInfo[] getAllNetworkInfo() {
        enforceAccessPermission();
        ArrayList<NetworkInfo> result = Lists.newArrayList();
        for (int networkType = 0; networkType <= 49; networkType++) {
            NetworkInfo info = getNetworkInfo(networkType);
            if (info != null) {
                result.add(info);
            }
        }
        return (NetworkInfo[]) result.toArray(new NetworkInfo[result.size()]);
    }

    public Network getNetworkForType(int networkType) {
        enforceAccessPermission();
        int uid = Binder.getCallingUid();
        NetworkState state = getFilteredNetworkState(networkType, uid, false);
        if (!isNetworkWithLinkPropertiesBlocked(state.linkProperties, uid, false)) {
            return state.network;
        }
        return null;
    }

    public Network[] getAllNetworks() {
        Network[] result;
        enforceAccessPermission();
        synchronized (this.mNetworkForNetId) {
            result = new Network[this.mNetworkForNetId.size()];
            for (int i = 0; i < this.mNetworkForNetId.size(); i++) {
                result[i] = this.mNetworkForNetId.valueAt(i).network;
            }
        }
        return result;
    }

    public NetworkCapabilities[] getDefaultNetworkCapabilitiesForUser(int userId) {
        Network[] networks;
        enforceAccessPermission();
        HashMap<Network, NetworkCapabilities> result = new HashMap<>();
        NetworkAgentInfo nai = getDefaultNetwork();
        NetworkCapabilities nc = getNetworkCapabilitiesInternal(nai);
        if (nc != null) {
            result.put(nai.network, nc);
        }
        if (!this.mLockdownEnabled) {
            synchronized (this.mVpns) {
                Vpn vpn = this.mVpns.get(userId);
                if (vpn != null && (networks = vpn.getUnderlyingNetworks()) != null) {
                    for (Network network : networks) {
                        NetworkCapabilities nc2 = getNetworkCapabilitiesInternal(getNetworkAgentInfoForNetwork(network));
                        if (nc2 != null) {
                            result.put(network, nc2);
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
        if (nai == null) {
            return null;
        }
        synchronized (nai) {
            linkProperties = new LinkProperties(nai.linkProperties);
        }
        return linkProperties;
    }

    public LinkProperties getLinkProperties(Network network) {
        LinkProperties linkProperties;
        enforceAccessPermission();
        NetworkAgentInfo nai = getNetworkAgentInfoForNetwork(network);
        if (nai == null) {
            return null;
        }
        synchronized (nai) {
            linkProperties = new LinkProperties(nai.linkProperties);
        }
        return linkProperties;
    }

    private NetworkCapabilities getNetworkCapabilitiesInternal(NetworkAgentInfo nai) {
        if (nai != null) {
            synchronized (nai) {
                if (nai.networkCapabilities != null) {
                    return new NetworkCapabilities(nai.networkCapabilities);
                }
            }
        }
        return null;
    }

    public NetworkCapabilities getNetworkCapabilities(Network network) {
        enforceAccessPermission();
        return getNetworkCapabilitiesInternal(getNetworkAgentInfoForNetwork(network));
    }

    public NetworkState[] getAllNetworkState() {
        enforceConnectivityInternalPermission();
        ArrayList<NetworkState> result = Lists.newArrayList();
        for (Network network : getAllNetworks()) {
            NetworkAgentInfo nai = getNetworkAgentInfoForNetwork(network);
            if (nai != null) {
                result.add(nai.getNetworkState());
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
        NetworkInfo info = getActiveNetworkInfo();
        if (info != null) {
            return info.isMetered();
        }
        return false;
    }

    public boolean requestRouteToHostAddress(int networkType, byte[] hostAddress) {
        NetworkInfo.DetailedState netState;
        LinkProperties lp;
        int netId;
        enforceChangePermission();
        if (this.mProtectedNetworks.contains(Integer.valueOf(networkType))) {
            enforceConnectivityInternalPermission();
        }
        try {
            InetAddress addr = InetAddress.getByAddress(hostAddress);
            if (!ConnectivityManager.isNetworkTypeValid(networkType)) {
                log("requestRouteToHostAddress on invalid network: " + networkType);
                return false;
            }
            NetworkAgentInfo nai = this.mLegacyTypeTracker.getNetworkForType(networkType);
            if (nai == null) {
                if (!this.mLegacyTypeTracker.isTypeSupported(networkType)) {
                    log("requestRouteToHostAddress on unsupported network: " + networkType);
                } else {
                    log("requestRouteToHostAddress on down network: " + networkType);
                }
                return false;
            }
            synchronized (nai) {
                netState = nai.networkInfo.getDetailedState();
            }
            if (netState != NetworkInfo.DetailedState.CONNECTED && netState != NetworkInfo.DetailedState.CAPTIVE_PORTAL_CHECK) {
                if (VDBG) {
                    log("requestRouteToHostAddress on down network (" + networkType + ") - dropped netState=" + netState);
                }
                return false;
            }
            int uid = Binder.getCallingUid();
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (nai) {
                    lp = nai.linkProperties;
                    netId = nai.network.netId;
                }
                boolean ok = addLegacyRouteToHost(lp, addr, netId, uid);
                log("requestRouteToHostAddress ok=" + ok);
                return ok;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        } catch (UnknownHostException e) {
            log("requestRouteToHostAddress got " + e.toString());
            return false;
        }
    }

    private boolean addLegacyRouteToHost(LinkProperties lp, InetAddress addr, int netId, int uid) {
        RouteInfo bestRoute;
        RouteInfo bestRoute2 = RouteInfo.selectBestRoute(lp.getAllRoutes(), addr);
        if (bestRoute2 == null) {
            bestRoute = RouteInfo.makeHostRoute(addr, lp.getInterfaceName());
        } else {
            String iface = bestRoute2.getInterface();
            bestRoute = bestRoute2.getGateway().equals(addr) ? RouteInfo.makeHostRoute(addr, iface) : RouteInfo.makeHostRoute(addr, bestRoute2.getGateway(), iface);
        }
        log("Adding legacy route " + bestRoute + " for UID/PID " + uid + "/" + Binder.getCallingPid());
        try {
            this.mNetd.addLegacyRouteForNetId(netId, bestRoute, uid);
            return true;
        } catch (Exception e) {
            loge("Exception trying to add a route: " + e);
            return false;
        }
    }

    private void enforceCrossUserPermission(int userId) {
        if (userId == UserHandle.getCallingUserId()) {
            return;
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", TAG);
    }

    private void enforceInternetPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.INTERNET", TAG);
    }

    private void enforceAccessPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_NETWORK_STATE", TAG);
    }

    private void enforceChangePermission() {
        ConnectivityManager.enforceChangePermission(this.mContext);
    }

    private void enforceTetherAccessPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_NETWORK_STATE", TAG);
    }

    private void enforceConnectivityInternalPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
    }

    private void enforceKeepalivePermission() {
        this.mContext.enforceCallingOrSelfPermission(KeepaliveTracker.PERMISSION, TAG);
    }

    public void sendConnectedBroadcast(NetworkInfo info) {
        enforceConnectivityInternalPermission();
        sendGeneralBroadcast(info, "android.net.conn.CONNECTIVITY_CHANGE");
    }

    private void sendInetConditionBroadcast(NetworkInfo info) {
        sendGeneralBroadcast(info, "android.net.conn.INET_CONDITION_ACTION");
    }

    private Intent makeGeneralIntent(NetworkInfo info, String bcastType) {
        if (this.mLockdownTracker != null) {
            NetworkInfo info2 = new NetworkInfo(info);
            this.mLockdownTracker.augmentNetworkInfo(info2);
            info = info2;
        }
        Intent intent = new Intent(bcastType);
        intent.putExtra("networkInfo", new NetworkInfo(info));
        intent.putExtra("networkType", info.getType());
        if (info.isFailover()) {
            intent.putExtra("isFailover", true);
            info.setFailover(false);
        }
        if (info.getReason() != null) {
            intent.putExtra(PhoneWindowManager.SYSTEM_DIALOG_REASON_KEY, info.getReason());
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
        long identCt = Binder.clearCallingIdentity();
        try {
            this.mContext.sendOrderedBroadcastAsUser(intent, UserHandle.ALL, "android.permission.RECEIVE_DATA_ACTIVITY_CHANGE", null, null, 0, null, null);
            Binder.restoreCallingIdentity(identCt);
            Intent intentCt = new Intent("android.net.conn.DATA_ACTIVITY_CHANGE_CT");
            intentCt.putExtra("deviceType", deviceType);
            intentCt.putExtra("isActive", active);
            intentCt.putExtra("tsNanos", tsNanos);
            identCt = Binder.clearCallingIdentity();
            try {
                this.mContext.sendStickyBroadcastAsUser(intentCt, UserHandle.ALL);
            } finally {
            }
        } finally {
        }
    }

    private void sendStickyBroadcast(Intent intent) {
        synchronized (this) {
            if (!this.mSystemReady) {
                this.mInitialBroadcast = new Intent(intent);
            }
            intent.addFlags(67108864);
            if (VDBG) {
                log("sendStickyBroadcast: action=" + intent.getAction());
            }
            Bundle options = null;
            long ident = Binder.clearCallingIdentity();
            if ("android.net.conn.CONNECTIVITY_CHANGE".equals(intent.getAction())) {
                NetworkInfo ni = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                if (ni.getType() == 3) {
                    intent.setAction("android.net.conn.CONNECTIVITY_CHANGE_SUPL");
                    intent.addFlags(1073741824);
                } else {
                    BroadcastOptions opts = BroadcastOptions.makeBasic();
                    opts.setMaxManifestReceiverApiLevel(23);
                    options = opts.toBundle();
                }
                IBatteryStats bs = BatteryStatsService.getService();
                try {
                    bs.noteConnectivityChanged(intent.getIntExtra("networkType", -1), ni != null ? ni.getState().toString() : "?");
                } catch (RemoteException e) {
                }
            }
            try {
                this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL, options);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    void systemReady() {
        loadGlobalProxy();
        synchronized (this) {
            this.mSystemReady = true;
            if (this.mInitialBroadcast != null) {
                this.mContext.sendStickyBroadcastAsUser(this.mInitialBroadcast, UserHandle.ALL);
                this.mInitialBroadcast = null;
            }
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(9));
        updateLockdownVpn();
        log("Init IConnectivityServiceExt class");
        this.mIcsExt = (IConnectivityServiceExt) MPlugin.createInstance(IConnectivityServiceExt.class.getName(), this.mContext);
        if (this.mIcsExt == null) {
            log("Get IConnectivityServiceExt fail");
        } else {
            this.mIcsExt.init(this.mContext);
        }
        log("End MPlugin createInstance");
        if ("1".equals(SystemProperties.get("ro.mtk_pre_sim_wo_bal_support", "0"))) {
            try {
                log("[NetworkHttpMonitor] allocating memory for new variable");
                mNetworkHttpMonitor = new NetworkHttpMonitor(this.mContext, this.mNetd);
            } catch (Exception e) {
                log("[NetworkHttpMonitor] unable to create the new variable");
                e.printStackTrace();
            }
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(30));
        this.mHandler.sendMessage(this.mHandler.obtainMessage(25));
        this.mPermissionMonitor.startMonitoring();
    }

    private void setupDataActivityTracking(NetworkAgentInfo networkAgent) {
        int timeout;
        String iface = networkAgent.linkProperties.getInterfaceName();
        int type = -1;
        if (networkAgent.networkCapabilities.hasTransport(0)) {
            timeout = Settings.Global.getInt(this.mContext.getContentResolver(), "data_activity_timeout_mobile", 10);
            type = 0;
        } else if (networkAgent.networkCapabilities.hasTransport(1)) {
            timeout = Settings.Global.getInt(this.mContext.getContentResolver(), "data_activity_timeout_wifi", 15);
            type = 1;
        } else {
            timeout = 0;
        }
        if (timeout <= 0 || iface == null || type == -1) {
            return;
        }
        try {
            this.mNetd.addIdleTimer(iface, timeout, type);
        } catch (Exception e) {
            loge("Exception in setupDataActivityTracking " + e);
        }
    }

    private void removeDataActivityTracking(NetworkAgentInfo networkAgent) {
        String iface = networkAgent.linkProperties.getInterfaceName();
        NetworkCapabilities caps = networkAgent.networkCapabilities;
        if (iface == null) {
            return;
        }
        if (!caps.hasTransport(0) && !caps.hasTransport(1)) {
            return;
        }
        try {
            this.mNetd.removeIdleTimer(iface);
        } catch (Exception e) {
            loge("Exception in removeDataActivityTracking " + e);
        }
    }

    private void updateMtu(LinkProperties newLp, LinkProperties oldLp) {
        String iface = newLp.getInterfaceName();
        int mtu = newLp.getMtu();
        if (oldLp != null && newLp.isIdenticalMtu(oldLp)) {
            if (VDBG) {
                log("identical MTU - not setting");
            }
        } else if (!LinkProperties.isValidMtu(mtu, newLp.hasGlobalIPv6Address())) {
            if (mtu != 0) {
                loge("Unexpected mtu value: " + mtu + ", " + iface);
            }
        } else {
            if (TextUtils.isEmpty(iface)) {
                loge("Setting MTU size with null iface.");
                return;
            }
            try {
                if (VDBG) {
                    log("Setting MTU size: " + iface + ", " + mtu);
                }
                this.mNetd.setMtu(iface, mtu);
            } catch (Exception e) {
                Slog.e(TAG, "exception in setMtu()" + e);
            }
        }
    }

    protected int getDefaultTcpRwnd() {
        return SystemProperties.getInt(DEFAULT_TCP_RWND_KEY, 0);
    }

    private void updateTcpBufferSizes(NetworkAgentInfo nai) {
        if (!isDefaultNetwork(nai)) {
            return;
        }
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
        if (tcpBufferSizes.equals(this.mCurrentTcpBufferSizes)) {
            return;
        }
        try {
            if (VDBG) {
                Slog.d(TAG, "Setting tx/rx TCP buffers to " + tcpBufferSizes);
            }
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
        Integer rwndValue = Integer.valueOf(Settings.Global.getInt(this.mContext.getContentResolver(), "tcp_default_init_rwnd", getDefaultTcpRwnd()));
        if (rwndValue.intValue() == 0) {
            return;
        }
        SystemProperties.set("sys.sysctl.tcp_def_init_rwnd", rwndValue.toString());
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
                return Integer.parseInt(restoreDefaultNetworkDelayStr);
            } catch (NumberFormatException e) {
            }
        }
        if (networkType > 49 || this.mNetConfigs[networkType] == null) {
            return RESTORE_DEFAULT_NETWORK_DELAY;
        }
        int ret = this.mNetConfigs[networkType].restoreTime;
        return ret;
    }

    private boolean argsContain(String[] args, String target) {
        for (String arg : args) {
            if (arg.equals(target)) {
                return true;
            }
        }
        return false;
    }

    private void dumpNetworkDiagnostics(IndentingPrintWriter pw) {
        List<NetworkDiagnostics> netDiags = new ArrayList<>();
        for (NetworkAgentInfo nai : this.mNetworkAgentInfos.values()) {
            netDiags.add(new NetworkDiagnostics(nai.network, new LinkProperties(nai.linkProperties), DataShapingUtils.CLOSING_DELAY_BUFFER_FOR_MUSIC));
        }
        for (NetworkDiagnostics netDiag : netDiags) {
            pw.println();
            netDiag.waitForMeasurements();
            netDiag.dump(pw);
        }
    }

    protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        IndentingPrintWriter pw = new IndentingPrintWriter(writer, "  ");
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            pw.println("Permission Denial: can't dump ConnectivityService from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            return;
        }
        if (argsContain(args, "--diag")) {
            dumpNetworkDiagnostics(pw);
            return;
        }
        pw.print("NetworkFactories for:");
        for (NetworkFactoryInfo nfi : this.mNetworkFactoryInfos.values()) {
            pw.print(" " + nfi.name);
        }
        pw.println();
        pw.println();
        NetworkAgentInfo defaultNai = getDefaultNetwork();
        pw.print("Active default network: ");
        if (defaultNai == null) {
            pw.println("none");
        } else {
            pw.println(defaultNai.network.netId);
        }
        pw.println();
        pw.println("Current Networks:");
        pw.increaseIndent();
        for (NetworkAgentInfo nai : this.mNetworkAgentInfos.values()) {
            pw.println(nai.toString());
            pw.increaseIndent();
            pw.println("Requests:");
            pw.increaseIndent();
            for (int i = 0; i < nai.networkRequests.size(); i++) {
                pw.println(nai.networkRequests.valueAt(i).toString());
            }
            pw.decreaseIndent();
            pw.println("Lingered:");
            pw.increaseIndent();
            for (NetworkRequest nr : nai.networkLingered) {
                pw.println(nr.toString());
            }
            pw.decreaseIndent();
            pw.decreaseIndent();
        }
        pw.decreaseIndent();
        pw.println();
        pw.println("Metered Interfaces:");
        pw.increaseIndent();
        for (String value : this.mMeteredIfaces) {
            pw.println(value);
        }
        pw.decreaseIndent();
        pw.println();
        pw.print("Restrict background: ");
        pw.println(this.mRestrictBackground);
        pw.println();
        pw.println("Status for known UIDs:");
        pw.increaseIndent();
        int size = this.mUidRules.size();
        for (int i2 = 0; i2 < size; i2++) {
            int uid = this.mUidRules.keyAt(i2);
            pw.print("UID=");
            pw.print(uid);
            int uidRules = this.mUidRules.get(uid, 0);
            pw.print(" rules=");
            pw.print(NetworkPolicyManager.uidRulesToString(uidRules));
            pw.println();
        }
        pw.println();
        pw.decreaseIndent();
        pw.println("Network Requests:");
        pw.increaseIndent();
        for (NetworkRequestInfo nri : this.mNetworkRequests.values()) {
            pw.println(nri.toString());
        }
        pw.println();
        pw.decreaseIndent();
        this.mLegacyTypeTracker.dump(pw);
        synchronized (this) {
            pw.print("mNetTransitionWakeLock: currently " + (this.mNetTransitionWakeLock.isHeld() ? "" : "not ") + "held");
            if (TextUtils.isEmpty(this.mNetTransitionWakeLockCausedBy)) {
                pw.println(", last requested never");
            } else {
                pw.println(", last requested for " + this.mNetTransitionWakeLockCausedBy);
            }
        }
        pw.println();
        this.mTethering.dump(fd, pw, args);
        pw.println();
        this.mKeepaliveTracker.dump(pw);
        pw.println();
        if (this.mInetLog != null && this.mInetLog.size() > 0) {
            pw.println();
            pw.println("Inet condition reports:");
            pw.increaseIndent();
            for (int i3 = 0; i3 < this.mInetLog.size(); i3++) {
                pw.println(this.mInetLog.get(i3));
            }
            pw.decreaseIndent();
        }
        if (argsContain(args, "--short")) {
            return;
        }
        pw.println();
        synchronized (this.mValidationLogs) {
            pw.println("mValidationLogs (most recent first):");
            for (ValidationLog p : this.mValidationLogs) {
                pw.println(p.mNetwork + " - " + p.mNetworkExtraInfo);
                pw.increaseIndent();
                p.mLog.dump(fd, pw, args);
                pw.decreaseIndent();
            }
        }
        pw.println();
        pw.println("mNetworkRequestInfoLogs (most recent first):");
        pw.increaseIndent();
        this.mNetworkRequestInfoLogs.reverseDump(fd, pw, args);
        pw.decreaseIndent();
    }

    private boolean isLiveNetworkAgent(NetworkAgentInfo nai, int what) {
        if (nai.network == null) {
            return false;
        }
        NetworkAgentInfo officialNai = getNetworkAgentInfoForNetwork(nai.network);
        if (officialNai != null && officialNai.equals(nai)) {
            return true;
        }
        if (officialNai != null || VDBG) {
            String msg = sMagicDecoderRing.get(what, Integer.toString(what));
            loge(msg + " - isLiveNetworkAgent found mismatched netId: " + officialNai + " - " + nai);
        }
        return false;
    }

    private boolean isRequest(NetworkRequest request) {
        return this.mNetworkRequests.get(request).isRequest();
    }

    private class NetworkStateTrackerHandler extends Handler {
        public NetworkStateTrackerHandler(Looper looper) {
            super(looper);
        }

        private boolean maybeHandleAsyncChannelMessage(Message msg) throws Throwable {
            switch (msg.what) {
                case 69632:
                    ConnectivityService.this.handleAsyncChannelHalfConnect(msg);
                    return true;
                case 69633:
                case 69634:
                default:
                    return false;
                case 69635:
                    NetworkAgentInfo nai = (NetworkAgentInfo) ConnectivityService.this.mNetworkAgentInfos.get(msg.replyTo);
                    if (nai != null) {
                        nai.asyncChannel.disconnect();
                        return true;
                    }
                    return true;
                case 69636:
                    ConnectivityService.this.handleAsyncChannelDisconnected(msg);
                    return true;
            }
        }

        private void maybeHandleNetworkAgentMessage(Message msg) {
            NetworkAgentInfo nai = (NetworkAgentInfo) ConnectivityService.this.mNetworkAgentInfos.get(msg.replyTo);
            if (nai == null) {
                if (ConnectivityService.VDBG) {
                    String what = (String) ConnectivityService.sMagicDecoderRing.get(msg.what, Integer.toString(msg.what));
                    ConnectivityService.log(String.format("%s from unknown NetworkAgent", what));
                    return;
                }
                return;
            }
            switch (msg.what) {
                case 528385:
                    NetworkInfo info = (NetworkInfo) msg.obj;
                    ConnectivityService.this.updateNetworkInfo(nai, info);
                    return;
                case 528386:
                    NetworkCapabilities networkCapabilities = (NetworkCapabilities) msg.obj;
                    if (networkCapabilities.hasCapability(17) || networkCapabilities.hasCapability(16)) {
                        Slog.e(ConnectivityService.TAG, "BUG: " + nai + " has CS-managed capability.");
                    }
                    if (nai.everConnected && !nai.networkCapabilities.equalImmutableCapabilities(networkCapabilities)) {
                        Slog.e(ConnectivityService.TAG, "BUG: " + nai + " changed immutable capabilities: " + nai.networkCapabilities + " -> " + networkCapabilities);
                    }
                    ConnectivityService.this.updateCapabilities(nai, networkCapabilities);
                    return;
                case 528387:
                    if (ConnectivityService.VDBG) {
                        ConnectivityService.log("Update of LinkProperties for " + nai.name() + "; created=" + nai.created + "; everConnected=" + nai.everConnected);
                    }
                    LinkProperties oldLp = nai.linkProperties;
                    synchronized (nai) {
                        nai.linkProperties = (LinkProperties) msg.obj;
                    }
                    if (nai.everConnected) {
                        ConnectivityService.this.updateLinkProperties(nai, oldLp);
                        return;
                    }
                    return;
                case 528388:
                    Integer score = (Integer) msg.obj;
                    if (score != null) {
                        ConnectivityService.this.updateNetworkScore(nai, score.intValue());
                        return;
                    }
                    return;
                case 528389:
                    try {
                        ConnectivityService.this.mNetd.addVpnUidRanges(nai.network.netId, (UidRange[]) msg.obj);
                        return;
                    } catch (Exception e) {
                        ConnectivityService.loge("Exception in addVpnUidRanges: " + e);
                        return;
                    }
                case 528390:
                    try {
                        ConnectivityService.this.mNetd.removeVpnUidRanges(nai.network.netId, (UidRange[]) msg.obj);
                        return;
                    } catch (Exception e2) {
                        ConnectivityService.loge("Exception in removeVpnUidRanges: " + e2);
                        return;
                    }
                case 528391:
                case 528393:
                case 528394:
                case 528395:
                case 528396:
                default:
                    return;
                case 528392:
                    if (nai.everConnected && !nai.networkMisc.explicitlySelected) {
                        ConnectivityService.loge("ERROR: already-connected network explicitly selected.");
                    }
                    nai.networkMisc.explicitlySelected = true;
                    nai.networkMisc.acceptUnvalidated = ((Boolean) msg.obj).booleanValue();
                    return;
                case 528397:
                    ConnectivityService.this.mKeepaliveTracker.handleEventPacketKeepalive(nai, msg);
                    return;
            }
        }

        private boolean maybeHandleNetworkMonitorMessage(Message msg) {
            NetworkAgentInfo nai;
            NetworkAgentInfo nai2;
            switch (msg.what) {
                case NetworkMonitor.EVENT_NETWORK_TESTED:
                    synchronized (ConnectivityService.this.mNetworkForNetId) {
                        nai2 = (NetworkAgentInfo) ConnectivityService.this.mNetworkForNetId.get(msg.arg2);
                    }
                    if (nai2 != null) {
                        boolean valid = msg.arg1 == 0;
                        ConnectivityService.log(nai2.name() + " validation " + (valid ? "passed" : "failed") + (msg.obj == null ? "" : " with redirect to " + ((String) msg.obj)));
                        if (valid != nai2.lastValidated) {
                            int oldScore = nai2.getCurrentScore();
                            nai2.lastValidated = valid;
                            nai2.everValidated |= valid;
                            ConnectivityService.this.updateCapabilities(nai2, nai2.networkCapabilities);
                            if (oldScore != nai2.getCurrentScore()) {
                                ConnectivityService.this.sendUpdatedScoreToFactories(nai2);
                            }
                        }
                        ConnectivityService.this.updateInetCondition(nai2);
                        Bundle redirectUrlBundle = new Bundle();
                        redirectUrlBundle.putString(NetworkAgent.REDIRECT_URL_KEY, (String) msg.obj);
                        nai2.asyncChannel.sendMessage(528391, valid ? 1 : 2, 0, redirectUrlBundle);
                    }
                    return true;
                case NetworkMonitor.EVENT_NETWORK_LINGER_COMPLETE:
                    NetworkAgentInfo nai3 = (NetworkAgentInfo) msg.obj;
                    if (ConnectivityService.this.isLiveNetworkAgent(nai3, msg.what)) {
                        ConnectivityService.this.handleLingerComplete(nai3);
                    }
                    return true;
                case NetworkMonitor.EVENT_PROVISIONING_NOTIFICATION:
                    int netId = msg.arg2;
                    boolean visible = msg.arg1 != 0;
                    synchronized (ConnectivityService.this.mNetworkForNetId) {
                        nai = (NetworkAgentInfo) ConnectivityService.this.mNetworkForNetId.get(netId);
                    }
                    if (nai != null && visible != nai.lastCaptivePortalDetected) {
                        nai.lastCaptivePortalDetected = visible;
                        nai.everCaptivePortalDetected |= visible;
                        ConnectivityService.this.updateCapabilities(nai, nai.networkCapabilities);
                    }
                    if (!visible) {
                        ConnectivityService.this.setProvNotificationVisibleIntent(false, netId, null, 0, null, null, false);
                    } else if (nai == null) {
                        ConnectivityService.loge("EVENT_PROVISIONING_NOTIFICATION from unknown NetworkMonitor");
                    } else {
                        ConnectivityService.this.setProvNotificationVisibleIntent(true, netId, NotificationType.SIGN_IN, nai.networkInfo.getType(), nai.networkInfo.getExtraInfo(), (PendingIntent) msg.obj, nai.networkMisc.explicitlySelected);
                    }
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void handleMessage(Message msg) {
            if (maybeHandleAsyncChannelMessage(msg) || maybeHandleNetworkMonitorMessage(msg)) {
                return;
            }
            maybeHandleNetworkAgentMessage(msg);
        }
    }

    private void linger(NetworkAgentInfo nai) {
        nai.lingering = true;
        NetworkEvent.logEvent(nai.network.netId, 5);
        nai.networkMonitor.sendMessage(NetworkMonitor.CMD_NETWORK_LINGER);
        notifyNetworkCallbacks(nai, 524291);
    }

    private void unlinger(NetworkAgentInfo nai) {
        nai.networkLingered.clear();
        if (nai.lingering) {
            nai.lingering = false;
            NetworkEvent.logEvent(nai.network.netId, 6);
            if (VDBG) {
                log("Canceling linger of " + nai.name());
            }
            nai.networkMonitor.sendMessage(NetworkMonitor.CMD_NETWORK_CONNECTED);
        }
    }

    private void handleAsyncChannelHalfConnect(Message msg) {
        AsyncChannel ac = (AsyncChannel) msg.obj;
        if (this.mNetworkFactoryInfos.containsKey(msg.replyTo)) {
            if (msg.arg1 == 0) {
                if (VDBG) {
                    log("NetworkFactory connected");
                }
                for (NetworkRequestInfo nri : this.mNetworkRequests.values()) {
                    if (nri.isRequest()) {
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
        if (!this.mNetworkAgentInfos.containsKey(msg.replyTo)) {
            return;
        }
        if (msg.arg1 == 0) {
            if (VDBG) {
                log("NetworkAgent connected");
            }
            this.mNetworkAgentInfos.get(msg.replyTo).asyncChannel.sendMessage(69633);
            return;
        }
        loge("Error connecting NetworkAgent");
        NetworkAgentInfo nai2 = this.mNetworkAgentInfos.remove(msg.replyTo);
        if (nai2 == null) {
            return;
        }
        boolean wasDefault = isDefaultNetwork(nai2);
        synchronized (this.mNetworkForNetId) {
            this.mNetworkForNetId.remove(nai2.network.netId);
            this.mNetIdInUse.delete(nai2.network.netId);
        }
        this.mLegacyTypeTracker.remove(nai2, wasDefault);
    }

    private void handleAsyncChannelDisconnected(Message msg) throws Throwable {
        NetworkAgentInfo nai = this.mNetworkAgentInfos.get(msg.replyTo);
        if (nai != null) {
            log(nai.name() + " got DISCONNECTED, was satisfying " + nai.networkRequests.size());
            if (nai.networkInfo.isConnected()) {
                nai.networkInfo.setDetailedState(NetworkInfo.DetailedState.DISCONNECTED, null, null);
            }
            boolean wasDefault = isDefaultNetwork(nai);
            if (wasDefault) {
                this.mDefaultInetConditionPublished = 0;
                logDefaultNetworkEvent(null, nai);
            }
            notifyIfacesChangedForNetworkStats();
            notifyNetworkCallbacks(nai, 524292);
            this.mKeepaliveTracker.handleStopAllKeepalives(nai, -20);
            nai.networkMonitor.sendMessage(NetworkMonitor.CMD_NETWORK_DISCONNECTED);
            this.mNetworkAgentInfos.remove(msg.replyTo);
            updateClat(null, nai.linkProperties, nai);
            synchronized (this.mNetworkForNetId) {
                this.mNetworkForNetId.remove(nai.network.netId);
            }
            if (this.mIcsExt != null && nai.networkInfo.getType() == 1) {
                this.mIcsExt.UserPrompt();
            }
            if (this.mIcsExt == null) {
                log("mIcsExt is null");
            }
            for (int i = 0; i < nai.networkRequests.size(); i++) {
                NetworkRequest request = nai.networkRequests.valueAt(i);
                NetworkAgentInfo currentNetwork = this.mNetworkForRequestId.get(request.requestId);
                if (currentNetwork != null && currentNetwork.network.netId == nai.network.netId) {
                    this.mNetworkForRequestId.remove(request.requestId);
                    sendUpdatedScoreToFactories(request, 0);
                }
            }
            if (nai.networkRequests.get(this.mDefaultRequest.requestId) != null) {
                removeDataActivityTracking(nai);
                notifyLockdownVpn(nai);
                requestNetworkTransitionWakelock(nai.name());
            }
            this.mLegacyTypeTracker.remove(nai, wasDefault);
            rematchAllNetworksAndRequests(null, 0);
            if (nai.created) {
                try {
                    this.mNetd.removeNetwork(nai.network.netId);
                } catch (Exception e) {
                    loge("Exception removing network: " + e);
                }
            }
            synchronized (this.mNetworkForNetId) {
                this.mNetIdInUse.delete(nai.network.netId);
            }
            return;
        }
        NetworkFactoryInfo nfi = this.mNetworkFactoryInfos.remove(msg.replyTo);
        if (nfi != null) {
            log("unregisterNetworkFactory for " + nfi.name);
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
        handleRegisterNetworkRequest(nri);
    }

    private void handleRegisterNetworkRequest(NetworkRequestInfo nri) {
        synchronized (this.mRequestLock) {
            this.mNetworkRequests.put(nri.request, nri);
        }
        this.mNetworkRequestInfoLogs.log("REGISTER " + nri);
        if (!nri.isRequest()) {
            for (NetworkAgentInfo network : this.mNetworkAgentInfos.values()) {
                if (nri.request.networkCapabilities.hasSignalStrength() && network.satisfiesImmutableCapabilitiesOf(nri.request)) {
                    updateSignalStrengthThresholds(network, "REGISTER", nri.request);
                }
            }
        }
        rematchAllNetworksAndRequests(null, 0);
        if (!nri.isRequest() || this.mNetworkForRequestId.get(nri.request.requestId) != null) {
            return;
        }
        sendUpdatedScoreToFactories(nri.request, 0);
    }

    private void handleReleaseNetworkRequestWithIntent(PendingIntent pendingIntent, int callingUid) {
        NetworkRequestInfo nri = findExistingNetworkRequestInfo(pendingIntent);
        if (nri == null) {
            return;
        }
        handleReleaseNetworkRequest(nri.request, callingUid);
    }

    private boolean unneeded(NetworkAgentInfo nai) {
        if (!nai.everConnected || nai.isVPN() || nai.lingering) {
            return false;
        }
        for (NetworkRequestInfo nri : this.mNetworkRequests.values()) {
            if (nri.isRequest() && nai.satisfies(nri.request) && (nai.networkRequests.get(nri.request.requestId) != null || this.mNetworkForRequestId.get(nri.request.requestId).getCurrentScore() < nai.getCurrentScoreAsValidated())) {
                return false;
            }
        }
        return true;
    }

    private void handleReleaseNetworkRequest(NetworkRequest request, int callingUid) {
        NetworkRequestInfo nri = this.mNetworkRequests.get(request);
        if (nri != null) {
            if (1000 != callingUid && nri.mUid != callingUid) {
                log("Attempt to release unowned NetworkRequest " + request);
                return;
            }
            if (VDBG || nri.isRequest()) {
                log("releasing NetworkRequest " + request);
            }
            nri.unlinkDeathRecipient();
            synchronized (this.mRequestLock) {
                this.mNetworkRequests.remove(request);
            }
            synchronized (this.mUidToNetworkRequestCount) {
                int requests = this.mUidToNetworkRequestCount.get(nri.mUid, 0);
                if (requests < 1) {
                    Slog.e(TAG, "BUG: too small request count " + requests + " for UID " + nri.mUid);
                } else if (requests == 1) {
                    this.mUidToNetworkRequestCount.removeAt(this.mUidToNetworkRequestCount.indexOfKey(nri.mUid));
                } else {
                    this.mUidToNetworkRequestCount.put(nri.mUid, requests - 1);
                }
            }
            this.mNetworkRequestInfoLogs.log("RELEASE " + nri);
            if (nri.isRequest()) {
                boolean wasKept = false;
                for (NetworkAgentInfo nai : this.mNetworkAgentInfos.values()) {
                    if (nai.networkRequests.get(nri.request.requestId) != null) {
                        nai.networkRequests.remove(nri.request.requestId);
                        if (VDBG) {
                            log(" Removing from current network " + nai.name() + ", leaving " + nai.networkRequests.size() + " requests.");
                        }
                        if (unneeded(nai)) {
                            log("no live requests for " + nai.name() + "; disconnecting");
                            teardownUnneededNetwork(nai);
                        } else {
                            wasKept |= true;
                        }
                    }
                }
                NetworkAgentInfo nai2 = this.mNetworkForRequestId.get(nri.request.requestId);
                if (nai2 != null) {
                    this.mNetworkForRequestId.remove(nri.request.requestId);
                }
                if (nri.request.legacyType != -1 && nai2 != null) {
                    boolean doRemove = true;
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
                        this.mLegacyTypeTracker.remove(nri.request.legacyType, nai2, false);
                    }
                }
                for (NetworkFactoryInfo nfi : this.mNetworkFactoryInfos.values()) {
                    nfi.asyncChannel.sendMessage(536577, nri.request);
                }
            } else {
                for (NetworkAgentInfo nai3 : this.mNetworkAgentInfos.values()) {
                    nai3.networkRequests.remove(nri.request.requestId);
                    if (nri.request.networkCapabilities.hasSignalStrength() && nai3.satisfiesImmutableCapabilitiesOf(nri.request)) {
                        updateSignalStrengthThresholds(nai3, "RELEASE", nri.request);
                    }
                }
            }
            callCallbackForRequest(nri, null, 524296);
        }
    }

    public void setAcceptUnvalidated(Network network, boolean accept, boolean always) {
        enforceConnectivityInternalPermission();
        this.mHandler.sendMessage(this.mHandler.obtainMessage(28, accept ? 1 : 0, always ? 1 : 0, network));
    }

    private void handleSetAcceptUnvalidated(Network network, boolean accept, boolean always) {
        log("handleSetAcceptUnvalidated network=" + network + " accept=" + accept + " always=" + always);
        NetworkAgentInfo nai = getNetworkAgentInfoForNetwork(network);
        if (nai == null || nai.everValidated) {
            return;
        }
        if (!nai.networkMisc.explicitlySelected) {
            Slog.e(TAG, "BUG: setAcceptUnvalidated non non-explicitly selected network");
        }
        if (accept != nai.networkMisc.acceptUnvalidated) {
            int oldScore = nai.getCurrentScore();
            nai.networkMisc.acceptUnvalidated = accept;
            rematchAllNetworksAndRequests(nai, oldScore);
            sendUpdatedScoreToFactories(nai);
        }
        if (always) {
            nai.asyncChannel.sendMessage(528393, accept ? 1 : 0);
        }
        if (accept) {
            return;
        }
        nai.asyncChannel.sendMessage(528399);
        teardownUnneededNetwork(nai);
    }

    private void scheduleUnvalidatedPrompt(NetworkAgentInfo nai) {
        if (VDBG) {
            log("scheduleUnvalidatedPrompt " + nai.network);
        }
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(29, nai.network), 8000L);
    }

    private void handlePromptUnvalidated(Network network) {
        if (VDBG) {
            log("handlePromptUnvalidated " + network);
        }
        NetworkAgentInfo nai = getNetworkAgentInfoForNetwork(network);
        if (nai == null || nai.everValidated || nai.everCaptivePortalDetected || !nai.networkMisc.explicitlySelected || nai.networkMisc.acceptUnvalidated) {
            return;
        }
        Intent intent = new Intent("android.net.conn.PROMPT_UNVALIDATED");
        intent.setData(Uri.fromParts("netId", Integer.toString(network.netId), null));
        intent.addFlags(268435456);
        intent.setClassName("com.android.settings", "com.android.settings.wifi.WifiNoInternetDialog");
        PendingIntent pendingIntent = PendingIntent.getActivityAsUser(this.mContext, 0, intent, 268435456, null, UserHandle.CURRENT);
        setProvNotificationVisibleIntent(true, nai.network.netId, NotificationType.NO_INTERNET, nai.networkInfo.getType(), nai.networkInfo.getExtraInfo(), BenesseExtension.getDchaState() == 0 ? pendingIntent : null, true);
    }

    private class InternalHandler extends Handler {
        public InternalHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 8:
                case 24:
                    synchronized (ConnectivityService.this) {
                        if (msg.arg1 != ConnectivityService.this.mNetTransitionWakeLockSerialNumber || !ConnectivityService.this.mNetTransitionWakeLock.isHeld()) {
                            return;
                        }
                        ConnectivityService.this.mNetTransitionWakeLock.release();
                        String causedBy = ConnectivityService.this.mNetTransitionWakeLockCausedBy;
                        if (!ConnectivityService.VDBG) {
                            return;
                        }
                        if (msg.what == 24) {
                            ConnectivityService.log("Failed to find a new network - expiring NetTransition Wakelock");
                            return;
                        }
                        StringBuilder sbAppend = new StringBuilder().append("NetTransition Wakelock (");
                        if (causedBy == null) {
                            causedBy = "unknown";
                        }
                        ConnectivityService.log(sbAppend.append(causedBy).append(" cleared because we found a replacement network").toString());
                        return;
                    }
                case 9:
                    ConnectivityService.this.handleDeprecatedGlobalHttpProxy();
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
                    ConnectivityService.this.handleRegisterNetworkRequest((NetworkRequestInfo) msg.obj);
                    return;
                case 22:
                    ConnectivityService.this.handleReleaseNetworkRequest((NetworkRequest) msg.obj, msg.arg1);
                    return;
                case 23:
                    ConnectivityService.this.handleUnregisterNetworkFactory((Messenger) msg.obj);
                    return;
                case 25:
                    for (NetworkAgentInfo nai : ConnectivityService.this.mNetworkAgentInfos.values()) {
                        nai.networkMonitor.systemReady = true;
                    }
                    return;
                case 26:
                case 31:
                    ConnectivityService.this.handleRegisterNetworkRequestWithIntent(msg);
                    return;
                case 27:
                    ConnectivityService.this.handleReleaseNetworkRequestWithIntent((PendingIntent) msg.obj, msg.arg1);
                    return;
                case 28:
                    ConnectivityService.this.handleSetAcceptUnvalidated((Network) msg.obj, msg.arg1 != 0, msg.arg2 != 0);
                    return;
                case 29:
                    ConnectivityService.this.handlePromptUnvalidated((Network) msg.obj);
                    return;
                case 30:
                    ConnectivityService.this.handleMobileDataAlwaysOn();
                    return;
                case 100:
                    ConnectivityService.this.handleRegisterNetworkRequest(ConnectivityService.this.new NetworkRequestInfo(null, ConnectivityService.this.mDefaultRequest, new Binder(), NetworkRequestType.REQUEST));
                    return;
                case 528395:
                    ConnectivityService.this.mKeepaliveTracker.handleStartKeepalive(msg);
                    return;
                case 528396:
                    NetworkAgentInfo nai2 = ConnectivityService.this.getNetworkAgentInfoForNetwork((Network) msg.obj);
                    int slot = msg.arg1;
                    int reason = msg.arg2;
                    ConnectivityService.this.mKeepaliveTracker.handleStopKeepalive(nai2, slot, reason);
                    return;
                default:
                    return;
            }
        }
    }

    public int tether(String iface) {
        ConnectivityManager.enforceTetherChangePermission(this.mContext);
        if (isTetheringSupported()) {
            int status = this.mTethering.tether(iface);
            if (status == 0) {
                try {
                    this.mPolicyManager.onTetheringChanged(iface, true);
                } catch (RemoteException e) {
                }
            }
            return status;
        }
        return 3;
    }

    public int untether(String iface) {
        ConnectivityManager.enforceTetherChangePermission(this.mContext);
        if (isTetheringSupported()) {
            int status = this.mTethering.untether(iface);
            if (status == 0) {
                try {
                    this.mPolicyManager.onTetheringChanged(iface, false);
                } catch (RemoteException e) {
                }
            }
            return status;
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
        if (isTetheringSupported()) {
            return this.mTethering.getTetherableUsbRegexs();
        }
        return new String[0];
    }

    public String[] getTetherableWifiRegexs() {
        enforceTetherAccessPermission();
        if (isTetheringSupported()) {
            return this.mTethering.getTetherableWifiRegexs();
        }
        return new String[0];
    }

    public String[] getTetherableBluetoothRegexs() {
        enforceTetherAccessPermission();
        if (isTetheringSupported()) {
            return this.mTethering.getTetherableBluetoothRegexs();
        }
        return new String[0];
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
        if (!tetherEnabledInSettings || !this.mUserManager.isAdminUser()) {
            return false;
        }
        if (this.mTethering.getTetherableUsbRegexs().length == 0 && this.mTethering.getTetherableWifiRegexs().length == 0 && this.mTethering.getTetherableBluetoothRegexs().length == 0) {
            return false;
        }
        return this.mTethering.getUpstreamIfaceTypes().length != 0;
    }

    public void startTethering(int type, ResultReceiver receiver, boolean showProvisioningUi) {
        ConnectivityManager.enforceTetherChangePermission(this.mContext);
        if (!isTetheringSupported()) {
            receiver.send(3, null);
        } else {
            this.mTethering.startTethering(type, receiver, showProvisioningUi);
        }
    }

    public void stopTethering(int type) {
        ConnectivityManager.enforceTetherChangePermission(this.mContext);
        this.mTethering.stopTethering(type);
    }

    private void requestNetworkTransitionWakelock(String forWhom) throws Throwable {
        synchronized (this) {
            try {
                if (this.mNetTransitionWakeLock.isHeld()) {
                    return;
                }
                int serialNum = this.mNetTransitionWakeLockSerialNumber + 1;
                this.mNetTransitionWakeLockSerialNumber = serialNum;
                try {
                    this.mNetTransitionWakeLock.acquire();
                    this.mNetTransitionWakeLockCausedBy = forWhom;
                    this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(24, serialNum, 0), this.mNetTransitionWakeLockTimeout);
                    return;
                } catch (Throwable th) {
                    th = th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
            throw th;
        }
    }

    public void reportInetCondition(int networkType, int percentage) {
        NetworkAgentInfo nai = this.mLegacyTypeTracker.getNetworkForType(networkType);
        if (nai == null) {
            return;
        }
        reportNetworkConnectivity(nai.network, percentage > 50);
    }

    public void reportNetworkConnectivity(Network network, boolean hasConnectivity) {
        NetworkAgentInfo nai;
        enforceAccessPermission();
        enforceInternetPermission();
        if (network == null) {
            nai = getDefaultNetwork();
        } else {
            nai = getNetworkAgentInfoForNetwork(network);
        }
        if (nai == null || nai.networkInfo.getState() == NetworkInfo.State.DISCONNECTING || nai.networkInfo.getState() == NetworkInfo.State.DISCONNECTED || hasConnectivity == nai.lastValidated) {
            return;
        }
        int uid = Binder.getCallingUid();
        log("reportNetworkConnectivity(" + nai.network.netId + ", " + hasConnectivity + ") by " + uid);
        synchronized (nai) {
            if (nai.everConnected) {
                if (isNetworkWithLinkPropertiesBlocked(nai.linkProperties, uid, false)) {
                    return;
                }
                nai.networkMonitor.sendMessage(NetworkMonitor.CMD_FORCE_REEVALUATION, uid);
            }
        }
    }

    private ProxyInfo getDefaultProxy() {
        ProxyInfo ret;
        synchronized (this.mProxyLock) {
            ret = this.mGlobalProxy;
            if (ret == null && !this.mDefaultProxyDisabled) {
                ret = this.mDefaultProxy;
            }
        }
        return ret;
    }

    public ProxyInfo getProxyForNetwork(Network network) {
        NetworkAgentInfo nai;
        if (network == null) {
            return getDefaultProxy();
        }
        ProxyInfo globalProxy = getGlobalProxy();
        if (globalProxy != null) {
            return globalProxy;
        }
        if (!NetworkUtils.queryUserAccess(Binder.getCallingUid(), network.netId) || (nai = getNetworkAgentInfoForNetwork(network)) == null) {
            return null;
        }
        synchronized (nai) {
            ProxyInfo proxyInfo = nai.linkProperties.getHttpProxy();
            if (proxyInfo == null) {
                return null;
            }
            return new ProxyInfo(proxyInfo);
        }
    }

    private ProxyInfo canonicalizeProxyInfo(ProxyInfo proxy) {
        if (proxy != null && TextUtils.isEmpty(proxy.getHost())) {
            if (proxy.getPacFileUrl() == null || Uri.EMPTY.equals(proxy.getPacFileUrl())) {
                return null;
            }
            return proxy;
        }
        return proxy;
    }

    private boolean proxyInfoEqual(ProxyInfo a, ProxyInfo b) {
        ProxyInfo a2 = canonicalizeProxyInfo(a);
        ProxyInfo b2 = canonicalizeProxyInfo(b);
        if (!Objects.equals(a2, b2)) {
            return false;
        }
        if (a2 != null) {
            return Objects.equals(a2.getHost(), b2.getHost());
        }
        return true;
    }

    public void setGlobalProxy(ProxyInfo proxyProperties) {
        enforceConnectivityInternalPermission();
        synchronized (this.mProxyLock) {
            if (proxyProperties == this.mGlobalProxy) {
                return;
            }
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

    private void loadGlobalProxy() {
        ProxyInfo proxyProperties;
        ContentResolver res = this.mContext.getContentResolver();
        String host = Settings.Global.getString(res, "global_http_proxy_host");
        int port = Settings.Global.getInt(res, "global_http_proxy_port", 0);
        String exclList = Settings.Global.getString(res, "global_http_proxy_exclusion_list");
        String pacFileUrl = Settings.Global.getString(res, "global_proxy_pac_url");
        if (TextUtils.isEmpty(host) && TextUtils.isEmpty(pacFileUrl)) {
            return;
        }
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
                if (this.mDefaultProxy == proxy) {
                    return;
                }
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
                if (this.mGlobalProxy != null) {
                    return;
                }
                if (!this.mDefaultProxyDisabled) {
                    sendProxyBroadcast(proxy);
                }
            }
        }
    }

    private void updateProxy(LinkProperties newLp, LinkProperties oldLp, NetworkAgentInfo nai) {
        ProxyInfo httpProxy = newLp == null ? null : newLp.getHttpProxy();
        ProxyInfo oldProxyInfo = oldLp != null ? oldLp.getHttpProxy() : null;
        if (proxyInfoEqual(httpProxy, oldProxyInfo)) {
            return;
        }
        sendProxyBroadcast(getDefaultProxy());
    }

    private void handleDeprecatedGlobalHttpProxy() {
        String proxy = Settings.Global.getString(this.mContext.getContentResolver(), "http_proxy");
        if (TextUtils.isEmpty(proxy)) {
            return;
        }
        String[] data = proxy.split(":");
        if (data.length == 0) {
            return;
        }
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

    private void sendProxyBroadcast(ProxyInfo proxy) {
        if (proxy == null) {
            proxy = new ProxyInfo("", 0, "");
        }
        if (this.mPacManager.setCurrentProxyScriptUrl(proxy)) {
            return;
        }
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

    private static class SettingsObserver extends ContentObserver {
        private final Context mContext;
        private final Handler mHandler;
        private final HashMap<Uri, Integer> mUriEventMap;

        SettingsObserver(Context context, Handler handler) {
            super(null);
            this.mUriEventMap = new HashMap<>();
            this.mContext = context;
            this.mHandler = handler;
        }

        void observe(Uri uri, int what) {
            this.mUriEventMap.put(uri, Integer.valueOf(what));
            ContentResolver resolver = this.mContext.getContentResolver();
            resolver.registerContentObserver(uri, false, this);
        }

        @Override
        public void onChange(boolean selfChange) {
            Slog.e(ConnectivityService.TAG, "Should never be reached.");
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            Integer what = this.mUriEventMap.get(uri);
            if (what != null) {
                this.mHandler.obtainMessage(what.intValue()).sendToTarget();
            } else {
                ConnectivityService.loge("No matching event to send for URI=" + uri);
            }
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

    public boolean prepareVpn(String oldPackage, String newPackage, int userId) {
        enforceCrossUserPermission(userId);
        throwIfLockdownEnabled();
        synchronized (this.mVpns) {
            Vpn vpn = this.mVpns.get(userId);
            if (vpn != null) {
                return vpn.prepare(oldPackage, newPackage);
            }
            return false;
        }
    }

    public void setVpnPackageAuthorization(String packageName, int userId, boolean authorized) {
        enforceCrossUserPermission(userId);
        synchronized (this.mVpns) {
            Vpn vpn = this.mVpns.get(userId);
            if (vpn != null) {
                vpn.setPackageAuthorization(packageName, authorized);
            }
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

    public LegacyVpnInfo getLegacyVpnInfo(int userId) {
        LegacyVpnInfo legacyVpnInfo;
        enforceCrossUserPermission(userId);
        synchronized (this.mVpns) {
            legacyVpnInfo = this.mVpns.get(userId).getLegacyVpnInfo();
        }
        return legacyVpnInfo;
    }

    public VpnInfo[] getAllVpnInfo() {
        VpnInfo[] vpnInfoArr;
        enforceConnectivityInternalPermission();
        if (this.mLockdownEnabled) {
            return new VpnInfo[0];
        }
        synchronized (this.mVpns) {
            List<VpnInfo> infoList = new ArrayList<>();
            for (int i = 0; i < this.mVpns.size(); i++) {
                VpnInfo info = createVpnInfo(this.mVpns.valueAt(i));
                if (info != null) {
                    infoList.add(info);
                }
            }
            vpnInfoArr = (VpnInfo[]) infoList.toArray(new VpnInfo[infoList.size()]);
        }
        return vpnInfoArr;
    }

    private VpnInfo createVpnInfo(Vpn vpn) {
        LinkProperties linkProperties;
        VpnInfo info = vpn.getVpnInfo();
        if (info == null) {
            return null;
        }
        Network[] underlyingNetworks = vpn.getUnderlyingNetworks();
        if (underlyingNetworks == null) {
            NetworkAgentInfo defaultNetwork = getDefaultNetwork();
            if (defaultNetwork != null && defaultNetwork.linkProperties != null) {
                info.primaryUnderlyingIface = getDefaultNetwork().linkProperties.getInterfaceName();
            }
        } else if (underlyingNetworks.length > 0 && (linkProperties = getLinkProperties(underlyingNetworks[0])) != null) {
            info.primaryUnderlyingIface = linkProperties.getInterfaceName();
        }
        if (info.primaryUnderlyingIface == null) {
            return null;
        }
        return info;
    }

    public VpnConfig getVpnConfig(int userId) {
        enforceCrossUserPermission(userId);
        synchronized (this.mVpns) {
            Vpn vpn = this.mVpns.get(userId);
            if (vpn == null) {
                return null;
            }
            return vpn.getVpnConfig();
        }
    }

    public boolean updateLockdownVpn() {
        if (Binder.getCallingUid() != 1000) {
            Slog.w(TAG, "Lockdown VPN only available to AID_SYSTEM");
            return false;
        }
        this.mLockdownEnabled = LockdownVpnTracker.isEnabled();
        if (this.mLockdownEnabled) {
            String profileName = new String(this.mKeyStore.get("LOCKDOWN_VPN"));
            VpnProfile profile = VpnProfile.decode(profileName, this.mKeyStore.get("VPN_" + profileName));
            if (profile == null) {
                loge("Null profile name:" + profileName);
                this.mKeyStore.delete("LOCKDOWN_VPN");
                setLockdownTracker(null);
                return true;
            }
            int user = UserHandle.getUserId(Binder.getCallingUid());
            synchronized (this.mVpns) {
                Vpn vpn = this.mVpns.get(user);
                if (vpn == null) {
                    Slog.w(TAG, "VPN for user " + user + " not ready yet. Skipping lockdown");
                    return false;
                }
                setLockdownTracker(new LockdownVpnTracker(this.mContext, this.mNetd, this, vpn, profile));
            }
        } else {
            setLockdownTracker(null);
        }
        return true;
    }

    private void setLockdownTracker(LockdownVpnTracker tracker) {
        LockdownVpnTracker existing = this.mLockdownTracker;
        this.mLockdownTracker = null;
        if (existing != null) {
            existing.shutdown();
        }
        try {
            if (tracker != null) {
                this.mNetd.setFirewallEnabled(true);
                this.mNetd.setFirewallInterfaceRule("lo", true);
                this.mLockdownTracker = tracker;
                this.mLockdownTracker.init();
            } else {
                this.mNetd.setFirewallEnabled(false);
            }
        } catch (RemoteException e) {
        }
    }

    private void throwIfLockdownEnabled() {
        if (!this.mLockdownEnabled) {
        } else {
            throw new IllegalStateException("Unavailable in lockdown mode");
        }
    }

    private boolean startAlwaysOnVpn(int userId) {
        synchronized (this.mVpns) {
            Vpn vpn = this.mVpns.get(userId);
            if (vpn == null) {
                Slog.wtf(TAG, "User " + userId + " has no Vpn configuration");
                return false;
            }
            return vpn.startAlwaysOnVpn();
        }
    }

    public boolean setAlwaysOnVpnPackage(int userId, String packageName, boolean lockdown) {
        enforceConnectivityInternalPermission();
        enforceCrossUserPermission(userId);
        if (LockdownVpnTracker.isEnabled()) {
            return false;
        }
        synchronized (this.mVpns) {
            Vpn vpn = this.mVpns.get(userId);
            if (vpn == null) {
                Slog.w(TAG, "User " + userId + " has no Vpn configuration");
                return false;
            }
            if (!vpn.setAlwaysOnPackage(packageName, lockdown)) {
                return false;
            }
            if (!startAlwaysOnVpn(userId)) {
                vpn.setAlwaysOnPackage(null, false);
                return false;
            }
            vpn.saveAlwaysOnPackage();
            return true;
        }
    }

    public String getAlwaysOnVpnPackage(int userId) {
        enforceConnectivityInternalPermission();
        enforceCrossUserPermission(userId);
        synchronized (this.mVpns) {
            Vpn vpn = this.mVpns.get(userId);
            if (vpn == null) {
                Slog.w(TAG, "User " + userId + " has no Vpn configuration");
                return null;
            }
            return vpn.getAlwaysOnPackage();
        }
    }

    public int checkMobileProvisioning(int suggestedTimeOutMs) {
        return -1;
    }

    private enum NotificationType {
        SIGN_IN,
        NO_INTERNET;

        public static NotificationType[] valuesCustom() {
            return values();
        }
    }

    private void setProvNotificationVisible(boolean visible, int networkType, String action) {
        Intent intent = new Intent(action);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this.mContext, 0, intent, 0);
        int id = PackageManagerService.DumpState.DUMP_INSTALLS + networkType + 1;
        setProvNotificationVisibleIntent(visible, id, NotificationType.SIGN_IN, networkType, null, pendingIntent, false);
    }

    private void setProvNotificationVisibleIntent(boolean visible, int id, NotificationType notifyType, int networkType, String extraInfo, PendingIntent intent, boolean highPriority) {
        CharSequence title;
        CharSequence details;
        int icon;
        if (VDBG || visible) {
            log("setProvNotificationVisibleIntent " + notifyType + " visible=" + visible + " networkType=" + ConnectivityManager.getNetworkTypeName(networkType) + " extraInfo=" + extraInfo + " highPriority=" + highPriority);
        }
        Resources r = Resources.getSystem();
        NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        if (!visible) {
            try {
                notificationManager.cancelAsUser(NOTIFICATION_ID, id, UserHandle.ALL);
                return;
            } catch (NullPointerException npe) {
                loge("setNotificationVisible: cancel notificationManager npe=" + npe);
                npe.printStackTrace();
                return;
            }
        }
        if (notifyType == NotificationType.NO_INTERNET && networkType == 1) {
            title = r.getString(R.string.ext_media_status_removed, 0);
            details = r.getString(R.string.ext_media_status_unmountable);
            icon = R.drawable.list_selector_background_longpress;
        } else {
            if (notifyType != NotificationType.SIGN_IN) {
                Slog.e(TAG, "Unknown notification type " + notifyType + "on network type " + ConnectivityManager.getNetworkTypeName(networkType));
                return;
            }
            switch (networkType) {
                case 0:
                case 5:
                    title = r.getString(R.string.ext_media_status_mounted, 0);
                    details = this.mTelephonyManager.getNetworkOperatorName();
                    icon = R.drawable.list_selector_background_focused;
                    break;
                case 1:
                    title = r.getString(R.string.ext_media_status_missing, 0);
                    details = r.getString(R.string.ext_media_status_mounted_ro, extraInfo);
                    icon = R.drawable.list_selector_background_longpress;
                    break;
                case 2:
                case 3:
                case 4:
                default:
                    title = r.getString(R.string.ext_media_status_mounted, 0);
                    details = r.getString(R.string.ext_media_status_mounted_ro, extraInfo);
                    icon = R.drawable.list_selector_background_focused;
                    break;
            }
        }
        Notification notification = new Notification.Builder(this.mContext).setWhen(0L).setSmallIcon(icon).setAutoCancel(true).setTicker(title).setColor(this.mContext.getColor(R.color.system_accent3_600)).setContentTitle(title).setContentText(details).setContentIntent(intent).setLocalOnly(true).setPriority(highPriority ? 1 : 0).setDefaults(0).setOnlyAlertOnce(true).build();
        try {
            notificationManager.notifyAsUser(NOTIFICATION_ID, id, notification, UserHandle.ALL);
        } catch (NullPointerException npe2) {
            loge("setNotificationVisible: visible notificationManager npe=" + npe2);
            npe2.printStackTrace();
        }
    }

    private String getProvisioningUrlBaseFromFile() throws Throwable {
        FileReader fileReader;
        String mcc;
        String mnc;
        FileReader fileReader2 = null;
        Configuration config = this.mContext.getResources().getConfiguration();
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
                    return null;
                }
                if (element.equals(TAG_PROVISIONING_URL) && (mcc = parser.getAttributeValue(null, ATTR_MCC)) != null) {
                    try {
                        if (Integer.parseInt(mcc) == config.mcc && (mnc = parser.getAttributeValue(null, ATTR_MNC)) != null && Integer.parseInt(mnc) == config.mnc) {
                            parser.next();
                            if (parser.getEventType() == 4) {
                                String text = parser.getText();
                                if (fileReader != null) {
                                    try {
                                        fileReader.close();
                                    } catch (IOException e5) {
                                    }
                                }
                                return text;
                            }
                            continue;
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
            return null;
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
            return null;
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
            return null;
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
    }

    public String getMobileProvisioningUrl() throws Throwable {
        enforceConnectivityInternalPermission();
        String url = getProvisioningUrlBaseFromFile();
        if (TextUtils.isEmpty(url)) {
            url = this.mContext.getResources().getString(R.string.config_systemDependencyInstaller);
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
            intent.putExtra(AudioService.CONNECT_INTENT_KEY_STATE, enable);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void onUserStart(int userId) {
        synchronized (this.mVpns) {
            if (this.mVpns.get(userId) != null) {
                loge("Starting user already has a VPN");
                return;
            }
            Vpn userVpn = new Vpn(this.mHandler.getLooper(), this.mContext, this.mNetd, userId);
            this.mVpns.put(userId, userVpn);
            ContentResolver cr = this.mContext.getContentResolver();
            String alwaysOnPackage = Settings.Secure.getStringForUser(cr, "always_on_vpn_app", userId);
            boolean alwaysOnLockdown = Settings.Secure.getIntForUser(cr, "always_on_vpn_lockdown", 0, userId) != 0;
            if (alwaysOnPackage != null) {
                userVpn.setAlwaysOnPackage(alwaysOnPackage, alwaysOnLockdown);
            }
            if (!this.mUserManager.getUserInfo(userId).isPrimary() || !LockdownVpnTracker.isEnabled()) {
                return;
            }
            updateLockdownVpn();
        }
    }

    private void onUserStop(int userId) {
        synchronized (this.mVpns) {
            Vpn userVpn = this.mVpns.get(userId);
            if (userVpn == null) {
                loge("Stopped user has no VPN");
            } else {
                userVpn.onUserStopped();
                this.mVpns.delete(userId);
            }
        }
    }

    private void onUserAdded(int userId) {
        synchronized (this.mVpns) {
            int vpnsSize = this.mVpns.size();
            for (int i = 0; i < vpnsSize; i++) {
                Vpn vpn = this.mVpns.valueAt(i);
                vpn.onUserAdded(userId);
            }
        }
    }

    private void onUserRemoved(int userId) {
        synchronized (this.mVpns) {
            int vpnsSize = this.mVpns.size();
            for (int i = 0; i < vpnsSize; i++) {
                Vpn vpn = this.mVpns.valueAt(i);
                vpn.onUserRemoved(userId);
            }
        }
    }

    private void onUserUnlocked(int userId) {
        if (this.mUserManager.getUserInfo(userId).isPrimary() && LockdownVpnTracker.isEnabled()) {
            updateLockdownVpn();
        } else {
            startAlwaysOnVpn(userId);
        }
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

    private enum NetworkRequestType {
        LISTEN,
        TRACK_DEFAULT,
        REQUEST;

        public static NetworkRequestType[] valuesCustom() {
            return values();
        }
    }

    private class NetworkRequestInfo implements IBinder.DeathRecipient {

        private static final int[] f1xe68db0d8 = null;
        final int[] $SWITCH_TABLE$com$android$server$ConnectivityService$NetworkRequestType;
        private final IBinder mBinder;
        final PendingIntent mPendingIntent;
        boolean mPendingIntentSent;
        final int mPid;
        private final NetworkRequestType mType;
        final int mUid;
        final Messenger messenger;
        final NetworkRequest request;

        private static int[] m344x4a09bd7c() {
            if (f1xe68db0d8 != null) {
                return f1xe68db0d8;
            }
            int[] iArr = new int[NetworkRequestType.valuesCustom().length];
            try {
                iArr[NetworkRequestType.LISTEN.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                iArr[NetworkRequestType.REQUEST.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                iArr[NetworkRequestType.TRACK_DEFAULT.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            f1xe68db0d8 = iArr;
            return iArr;
        }

        NetworkRequestInfo(NetworkRequest r, PendingIntent pi, NetworkRequestType type) {
            this.request = r;
            this.mPendingIntent = pi;
            this.messenger = null;
            this.mBinder = null;
            this.mPid = ConnectivityService.getCallingPid();
            this.mUid = ConnectivityService.getCallingUid();
            this.mType = type;
            enforceRequestCountLimit();
        }

        NetworkRequestInfo(Messenger m, NetworkRequest r, IBinder binder, NetworkRequestType type) {
            this.messenger = m;
            this.request = r;
            this.mBinder = binder;
            this.mPid = ConnectivityService.getCallingPid();
            this.mUid = ConnectivityService.getCallingUid();
            this.mType = type;
            this.mPendingIntent = null;
            enforceRequestCountLimit();
            try {
                this.mBinder.linkToDeath(this, 0);
            } catch (RemoteException e) {
                binderDied();
            }
        }

        private void enforceRequestCountLimit() {
            synchronized (ConnectivityService.this.mUidToNetworkRequestCount) {
                int networkRequests = ConnectivityService.this.mUidToNetworkRequestCount.get(this.mUid, 0) + 1;
                if (networkRequests >= 100) {
                    throw new IllegalArgumentException("Too many NetworkRequests filed");
                }
                ConnectivityService.this.mUidToNetworkRequestCount.put(this.mUid, networkRequests);
            }
        }

        private String typeString() {
            switch (m344x4a09bd7c()[this.mType.ordinal()]) {
                case 1:
                    return "Listen";
                case 2:
                    return "Request";
                case 3:
                    return "Track default";
                default:
                    return "unknown type";
            }
        }

        void unlinkDeathRecipient() {
            if (this.mBinder == null) {
                return;
            }
            this.mBinder.unlinkToDeath(this, 0);
        }

        @Override
        public void binderDied() {
            ConnectivityService.log("ConnectivityService NetworkRequestInfo binderDied(" + this.request + ", " + this.mBinder + ")");
            ConnectivityService.this.releaseNetworkRequest(this.request);
        }

        public boolean isRequest() {
            return this.mType == NetworkRequestType.TRACK_DEFAULT || this.mType == NetworkRequestType.REQUEST;
        }

        public String toString() {
            return typeString() + " from uid/pid:" + this.mUid + "/" + this.mPid + " for " + this.request + (this.mPendingIntent == null ? "" : " to trigger " + this.mPendingIntent);
        }
    }

    private void ensureRequestableCapabilities(NetworkCapabilities networkCapabilities) {
        String badCapability = networkCapabilities.describeFirstNonRequestableCapability();
        if (badCapability == null) {
        } else {
            throw new IllegalArgumentException("Cannot request network with " + badCapability);
        }
    }

    private ArrayList<Integer> getSignalStrengthThresholds(NetworkAgentInfo nai) {
        SortedSet<Integer> thresholds = new TreeSet<>();
        synchronized (nai) {
            for (NetworkRequestInfo nri : this.mNetworkRequests.values()) {
                if (nri.request.networkCapabilities.hasSignalStrength() && nai.satisfiesImmutableCapabilitiesOf(nri.request)) {
                    thresholds.add(Integer.valueOf(nri.request.networkCapabilities.getSignalStrength()));
                }
            }
        }
        return new ArrayList<>(thresholds);
    }

    private void updateSignalStrengthThresholds(NetworkAgentInfo nai, String reason, NetworkRequest request) {
        String detail;
        ArrayList<Integer> thresholdsArray = getSignalStrengthThresholds(nai);
        Bundle thresholds = new Bundle();
        thresholds.putIntegerArrayList("thresholds", thresholdsArray);
        if (VDBG || !"CONNECT".equals(reason)) {
            if (request != null && request.networkCapabilities.hasSignalStrength()) {
                detail = reason + " " + request.networkCapabilities.getSignalStrength();
            } else {
                detail = reason;
            }
            log(String.format("updateSignalStrengthThresholds: %s, sending %s to %s", detail, Arrays.toString(thresholdsArray.toArray()), nai.name()));
        }
        nai.asyncChannel.sendMessage(528398, 0, 0, thresholds);
    }

    public NetworkRequest requestNetwork(NetworkCapabilities networkCapabilities, Messenger messenger, int timeoutMs, IBinder binder, int legacyType) {
        NetworkRequestType type;
        NetworkCapabilities networkCapabilities2;
        if (networkCapabilities == null) {
            type = NetworkRequestType.TRACK_DEFAULT;
        } else {
            type = NetworkRequestType.REQUEST;
        }
        if (type == NetworkRequestType.TRACK_DEFAULT) {
            networkCapabilities2 = new NetworkCapabilities(this.mDefaultRequest.networkCapabilities);
            enforceAccessPermission();
        } else {
            NetworkCapabilities networkCapabilities3 = new NetworkCapabilities(networkCapabilities);
            enforceNetworkRequestPermissions(networkCapabilities3);
            enforceMeteredApnPolicy(networkCapabilities3);
            networkCapabilities2 = networkCapabilities3;
        }
        ensureRequestableCapabilities(networkCapabilities2);
        if (timeoutMs < 0 || timeoutMs > 6000000) {
            throw new IllegalArgumentException("Bad timeout specified");
        }
        if ("*".equals(networkCapabilities2.getNetworkSpecifier())) {
            throw new IllegalArgumentException("Invalid network specifier - must not be '*'");
        }
        if (this.mIcsExt != null && this.mIcsExt.ignoreRequest(networkCapabilities2)) {
            log("requestNetwork return null to ignore mms request for OP09");
            return null;
        }
        NetworkRequest networkRequest = new NetworkRequest(networkCapabilities2, legacyType, nextNetworkRequestId());
        NetworkRequestInfo nri = new NetworkRequestInfo(messenger, networkRequest, binder, type);
        log("requestNetwork for " + nri);
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

    public boolean requestBandwidthUpdate(Network network) {
        NetworkAgentInfo nai;
        enforceAccessPermission();
        if (network == null) {
            return false;
        }
        synchronized (this.mNetworkForNetId) {
            nai = this.mNetworkForNetId.get(network.netId);
        }
        if (nai == null) {
            return false;
        }
        nai.asyncChannel.sendMessage(528394);
        return true;
    }

    private boolean isSystem(int uid) {
        return uid < 10000;
    }

    private void enforceMeteredApnPolicy(NetworkCapabilities networkCapabilities) {
        int uidRules;
        int uid = Binder.getCallingUid();
        if (isSystem(uid) || networkCapabilities.hasCapability(11)) {
            return;
        }
        synchronized (this.mRulesLock) {
            uidRules = this.mUidRules.get(uid, 32);
        }
        if (!this.mRestrictBackground || (uidRules & 1) != 0 || (uidRules & 2) != 0) {
            return;
        }
        networkCapabilities.addCapability(11);
    }

    public NetworkRequest pendingRequestForNetwork(NetworkCapabilities networkCapabilities, PendingIntent operation) {
        checkNotNull(operation, "PendingIntent cannot be null.");
        NetworkCapabilities networkCapabilities2 = new NetworkCapabilities(networkCapabilities);
        enforceNetworkRequestPermissions(networkCapabilities2);
        enforceMeteredApnPolicy(networkCapabilities2);
        ensureRequestableCapabilities(networkCapabilities2);
        NetworkRequest networkRequest = new NetworkRequest(networkCapabilities2, -1, nextNetworkRequestId());
        NetworkRequestInfo nri = new NetworkRequestInfo(networkRequest, operation, NetworkRequestType.REQUEST);
        log("pendingRequest for " + nri);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(26, nri));
        return networkRequest;
    }

    private void releasePendingNetworkRequestWithDelay(PendingIntent operation) {
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(27, getCallingUid(), 0, operation), this.mReleasePendingIntentDelayMs);
    }

    public void releasePendingNetworkRequest(PendingIntent operation) {
        checkNotNull(operation, "PendingIntent cannot be null.");
        this.mHandler.sendMessage(this.mHandler.obtainMessage(27, getCallingUid(), 0, operation));
    }

    private boolean hasWifiNetworkListenPermission(NetworkCapabilities nc) {
        if (nc == null) {
            return false;
        }
        int[] transportTypes = nc.getTransportTypes();
        if (transportTypes.length != 1 || transportTypes[0] != 1) {
            return false;
        }
        try {
            this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_WIFI_STATE", TAG);
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }

    public NetworkRequest listenForNetwork(NetworkCapabilities networkCapabilities, Messenger messenger, IBinder binder) {
        if (!hasWifiNetworkListenPermission(networkCapabilities)) {
            enforceAccessPermission();
        }
        NetworkRequest networkRequest = new NetworkRequest(new NetworkCapabilities(networkCapabilities), -1, nextNetworkRequestId());
        NetworkRequestInfo nri = new NetworkRequestInfo(messenger, networkRequest, binder, NetworkRequestType.LISTEN);
        if (VDBG) {
            log("listenForNetwork for " + nri);
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(21, nri));
        return networkRequest;
    }

    public void pendingListenForNetwork(NetworkCapabilities networkCapabilities, PendingIntent operation) {
        checkNotNull(operation, "PendingIntent cannot be null.");
        if (!hasWifiNetworkListenPermission(networkCapabilities)) {
            enforceAccessPermission();
        }
        NetworkRequest networkRequest = new NetworkRequest(new NetworkCapabilities(networkCapabilities), -1, nextNetworkRequestId());
        NetworkRequestInfo nri = new NetworkRequestInfo(networkRequest, operation, NetworkRequestType.LISTEN);
        if (VDBG) {
            log("pendingListenForNetwork for " + nri);
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(21, nri));
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
        return nai == getDefaultNetwork();
    }

    public int registerNetworkAgent(Messenger messenger, NetworkInfo networkInfo, LinkProperties linkProperties, NetworkCapabilities networkCapabilities, int currentScore, NetworkMisc networkMisc) {
        enforceConnectivityInternalPermission();
        NetworkAgentInfo nai = new NetworkAgentInfo(messenger, new AsyncChannel(), new Network(reserveNetId()), new NetworkInfo(networkInfo), new LinkProperties(linkProperties), new NetworkCapabilities(networkCapabilities), currentScore, this.mContext, this.mTrackerHandler, new NetworkMisc(networkMisc), this.mDefaultRequest, this);
        synchronized (this) {
            nai.networkMonitor.systemReady = this.mSystemReady;
        }
        addValidationLogs(nai.networkMonitor.getValidationLogs(), nai.network, networkInfo.getExtraInfo());
        log("registerNetworkAgent " + nai);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(18, nai));
        return nai.network.netId;
    }

    private void handleRegisterNetworkAgent(NetworkAgentInfo na) {
        if (VDBG) {
            log("Got NetworkAgent Messenger");
        }
        this.mNetworkAgentInfos.put(na.messenger, na);
        synchronized (this.mNetworkForNetId) {
            this.mNetworkForNetId.put(na.network.netId, na);
        }
        na.asyncChannel.connect(this.mContext, this.mTrackerHandler, na.messenger);
        NetworkInfo networkInfo = na.networkInfo;
        na.networkInfo = null;
        updateNetworkInfo(na, networkInfo);
    }

    private void updateLinkProperties(NetworkAgentInfo networkAgent, LinkProperties oldLp) {
        LinkProperties newLp = networkAgent.linkProperties;
        int netId = networkAgent.network.netId;
        if (VDBG) {
            log("updateLinkProperties for " + networkAgent.name());
        }
        if (VDBG) {
            log("LinkProperties:" + newLp);
        }
        if (networkAgent.clatd != null) {
            networkAgent.clatd.fixupLinkProperties(oldLp);
        }
        updateInterfaces(newLp, oldLp, netId);
        updateMtu(newLp, oldLp);
        updateTcpBufferSizes(networkAgent);
        updateRoutes(newLp, oldLp, netId);
        updateDnses(newLp, oldLp, netId);
        updateClat(newLp, oldLp, networkAgent);
        if (isDefaultNetwork(networkAgent)) {
            handleApplyDefaultProxy(newLp.getHttpProxy());
        } else {
            updateProxy(newLp, oldLp, networkAgent);
        }
        if (!Objects.equals(newLp, oldLp)) {
            notifyIfacesChangedForNetworkStats();
            notifyNetworkCallbacks(networkAgent, 524295);
            if (isDefaultNetwork(networkAgent) && oldLp != null && newLp != null && (!oldLp.isIdenticalAddresses(newLp) || !oldLp.isIdenticalStackedLinks(newLp))) {
                log("send additional Connectivity Action to notify ip-change");
                this.mLegacyTypeTracker.update(networkAgent);
            }
        }
        this.mKeepaliveTracker.handleCheckKeepalivesStillValid(networkAgent);
    }

    private void updateClat(LinkProperties newLp, LinkProperties oldLp, NetworkAgentInfo nai) {
        boolean wasRunningClat = nai.clatd != null ? nai.clatd.isStarted() : false;
        boolean shouldRunClat = Nat464Xlat.requiresClat(nai);
        if (nai.networkCapabilities.hasCapability(4) || nai.networkCapabilities.hasCapability(10)) {
            shouldRunClat = false;
        }
        if (!wasRunningClat && shouldRunClat) {
            nai.clatd = new Nat464Xlat(this.mContext, this.mNetd, this.mTrackerHandler, nai);
            nai.clatd.start();
        } else {
            if (!wasRunningClat || shouldRunClat) {
                return;
            }
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
                if (VDBG) {
                    log("Adding Route [" + route + "] to network " + netId);
                }
                try {
                    this.mNetd.addRoute(netId, route);
                } catch (Exception e) {
                    if ((route.getDestination().getAddress() instanceof Inet4Address) || VDBG) {
                        loge("Exception in addRoute for non-gateway: " + e);
                    }
                }
            }
        }
        for (RouteInfo route2 : routeDiff.added) {
            if (route2.hasGateway()) {
                if (VDBG) {
                    log("Adding Route [" + route2 + "] to network " + netId);
                }
                try {
                    this.mNetd.addRoute(netId, route2);
                } catch (Exception e2) {
                    if ((route2.getGateway() instanceof Inet4Address) || VDBG) {
                        loge("Exception in addRoute for gateway: " + e2);
                    }
                }
            }
        }
        for (RouteInfo route3 : routeDiff.removed) {
            if (VDBG) {
                log("Removing Route [" + route3 + "] from network " + netId);
            }
            try {
                this.mNetd.removeRoute(netId, route3);
            } catch (Exception e3) {
                loge("Exception in removeRoute: " + e3);
            }
        }
        return (routeDiff.added.isEmpty() && routeDiff.removed.isEmpty()) ? false : true;
    }

    private void updateDnses(LinkProperties newLp, LinkProperties oldLp, int netId) {
        if (oldLp != null && newLp.isIdenticalDnses(oldLp)) {
            return;
        }
        Collection<InetAddress> dnses = newLp.getDnsServers();
        if (this.mDefaultDns != null && !dnses.contains(this.mDefaultDns)) {
            Collection<InetAddress> dnses2 = new ArrayList<>(dnses);
            dnses2.add(this.mDefaultDns);
            log("add default dns: " + this.mDefaultDns.getHostAddress());
            dnses = dnses2;
        }
        if (isOnlyIpv6Address(newLp.getAddresses())) {
            ArrayList<InetAddress> sortedDnses = new ArrayList<>();
            for (InetAddress ia : dnses) {
                if (ia instanceof Inet6Address) {
                    sortedDnses.add(ia);
                }
            }
            for (InetAddress ia2 : dnses) {
                if (ia2 instanceof Inet4Address) {
                    sortedDnses.add(ia2);
                }
            }
            dnses = sortedDnses;
        }
        log("Setting Dns servers for network " + netId + " to " + dnses);
        try {
            this.mNetd.setDnsConfigurationForNetwork(netId, NetworkUtils.makeStrings(dnses), newLp.getDomains());
        } catch (Exception e) {
            loge("Exception in setDnsConfigurationForNetwork: " + e);
        }
        NetworkAgentInfo defaultNai = getDefaultNetwork();
        if (defaultNai != null && defaultNai.network.netId == netId) {
            setDefaultDnsSystemProperties(dnses);
        }
        flushVmDnsCache();
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

    private void updateCapabilities(NetworkAgentInfo nai, NetworkCapabilities networkCapabilities) {
        log("updateCapabilities cap: " + networkCapabilities);
        NetworkCapabilities networkCapabilities2 = new NetworkCapabilities(networkCapabilities);
        if (nai.lastValidated) {
            networkCapabilities2.addCapability(16);
        } else {
            networkCapabilities2.removeCapability(16);
        }
        if (nai.lastCaptivePortalDetected) {
            networkCapabilities2.addCapability(17);
        } else {
            networkCapabilities2.removeCapability(17);
        }
        if (Objects.equals(nai.networkCapabilities, networkCapabilities2)) {
            return;
        }
        int oldScore = nai.getCurrentScore();
        if (nai.networkCapabilities.hasCapability(13) != networkCapabilities2.hasCapability(13)) {
            try {
                this.mNetd.setNetworkPermission(nai.network.netId, networkCapabilities2.hasCapability(13) ? null : NetworkManagementService.PERMISSION_SYSTEM);
            } catch (RemoteException e) {
                loge("Exception in setNetworkPermission: " + e);
            }
        }
        synchronized (nai) {
            nai.networkCapabilities = networkCapabilities2;
        }
        rematchAllNetworksAndRequests(nai, oldScore);
        notifyNetworkCallbacks(nai, 524294);
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
        if (VDBG) {
            log("sending new Min Network Score(" + score + "): " + networkRequest.toString());
        }
        for (NetworkFactoryInfo nfi : this.mNetworkFactoryInfos.values()) {
            nfi.asyncChannel.sendMessage(536576, score, 0, networkRequest);
        }
    }

    private void sendPendingIntentForRequest(NetworkRequestInfo nri, NetworkAgentInfo networkAgent, int notificationType) {
        if (notificationType != 524290 || nri.mPendingIntentSent) {
            return;
        }
        Intent intent = new Intent();
        intent.putExtra("android.net.extra.NETWORK", networkAgent.network);
        intent.putExtra("android.net.extra.NETWORK_REQUEST", nri.request);
        nri.mPendingIntentSent = true;
        sendIntent(nri.mPendingIntent, intent);
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
        if (nri.messenger == null) {
            return;
        }
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
            if (VDBG && (notificationType == 524290 || notificationType == 524292)) {
                log("sending notification " + notifyTypeToName(notificationType) + " for " + nri.request);
            }
            nri.messenger.send(msg);
        } catch (RemoteException e) {
            loge("RemoteException caught trying to send a callback msg for " + nri.request);
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

    private void rematchNetworkAndRequests(NetworkAgentInfo newNetwork, ReapUnvalidatedNetworks reapUnvalidatedNetworks) {
        if (newNetwork.everConnected) {
            boolean keep = newNetwork.isVPN();
            boolean isNewDefault = false;
            NetworkAgentInfo oldDefaultNetwork = null;
            if (VDBG) {
                log("rematching " + newNetwork.name());
            }
            ArrayList<NetworkAgentInfo> affectedNetworks = new ArrayList<>();
            ArrayList<NetworkRequestInfo> addedRequests = new ArrayList<>();
            if (VDBG) {
                log(" network has: " + newNetwork.networkCapabilities);
            }
            for (NetworkRequestInfo nri : this.mNetworkRequests.values()) {
                NetworkAgentInfo currentNetwork = this.mNetworkForRequestId.get(nri.request.requestId);
                boolean satisfies = newNetwork.satisfies(nri.request);
                if (newNetwork == currentNetwork && satisfies) {
                    if (VDBG) {
                        log("Network " + newNetwork.name() + " was already satisfying request " + nri.request.requestId + ". No change.");
                    }
                    keep = true;
                } else {
                    if (VDBG && nri.isRequest()) {
                        log("  checking if request is satisfied: " + nri.request);
                    }
                    if (satisfies) {
                        if (nri.isRequest()) {
                            if (VDBG) {
                                log("currentScore = " + (currentNetwork != null ? currentNetwork.getCurrentScore() : 0) + ", newScore = " + newNetwork.getCurrentScore());
                            }
                            if (currentNetwork == null || currentNetwork.getCurrentScore() < newNetwork.getCurrentScore()) {
                                if (VDBG) {
                                    log("rematch for " + newNetwork.name());
                                }
                                if (currentNetwork != null) {
                                    if (VDBG) {
                                        log("   accepting network in place of " + currentNetwork.name());
                                    }
                                    currentNetwork.networkRequests.remove(nri.request.requestId);
                                    currentNetwork.networkLingered.add(nri.request);
                                    affectedNetworks.add(currentNetwork);
                                } else if (VDBG) {
                                    log("   accepting network in place of null");
                                }
                                unlinger(newNetwork);
                                this.mNetworkForRequestId.put(nri.request.requestId, newNetwork);
                                if (!newNetwork.addRequest(nri.request)) {
                                    Slog.e(TAG, "BUG: " + newNetwork.name() + " already has " + nri.request);
                                }
                                addedRequests.add(nri);
                                keep = true;
                                sendUpdatedScoreToFactories(nri.request, newNetwork.getCurrentScore());
                                if (this.mDefaultRequest.requestId == nri.request.requestId) {
                                    isNewDefault = true;
                                    oldDefaultNetwork = currentNetwork;
                                }
                            }
                        } else if (newNetwork.addRequest(nri.request)) {
                            addedRequests.add(nri);
                        }
                    } else if (newNetwork.networkRequests.get(nri.request.requestId) != null) {
                        log("Network " + newNetwork.name() + " stopped satisfying request " + nri.request.requestId);
                        newNetwork.networkRequests.remove(nri.request.requestId);
                        if (currentNetwork == newNetwork) {
                            if (this.mDefaultRequest.requestId == nri.request.requestId) {
                                try {
                                    this.mNetd.clearDefaultNetId();
                                } catch (RemoteException e) {
                                    loge("clearDefaultNetId err:" + e);
                                }
                                log("clear default network");
                                this.mLegacyTypeTracker.remove(newNetwork.networkInfo.getType(), newNetwork, true);
                                newNetwork.networkLingered.add(nri.request);
                            }
                            this.mNetworkForRequestId.remove(nri.request.requestId);
                            sendUpdatedScoreToFactories(nri.request, 0);
                        } else if (nri.isRequest()) {
                            Slog.e(TAG, "BUG: Removing request " + nri.request.requestId + " from " + newNetwork.name() + " without updating mNetworkForRequestId or factories!");
                        }
                        callCallbackForRequest(nri, newNetwork, 524292);
                    }
                }
            }
            for (NetworkAgentInfo nai : affectedNetworks) {
                if (!nai.lingering) {
                    if (unneeded(nai)) {
                        linger(nai);
                    } else {
                        unlinger(nai);
                    }
                }
            }
            if (isNewDefault) {
                makeDefault(newNetwork);
                logDefaultNetworkEvent(newNetwork, oldDefaultNetwork);
                synchronized (this) {
                    if (this.mNetTransitionWakeLock.isHeld()) {
                        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(8, this.mNetTransitionWakeLockSerialNumber, 0), 1000L);
                    }
                }
            }
            Iterator nri$iterator = addedRequests.iterator();
            while (nri$iterator.hasNext()) {
                notifyNetworkCallback(newNetwork, (NetworkRequestInfo) nri$iterator.next());
            }
            if (isNewDefault) {
                if (oldDefaultNetwork != null) {
                    this.mLegacyTypeTracker.remove(oldDefaultNetwork.networkInfo.getType(), oldDefaultNetwork, true);
                }
                this.mDefaultInetConditionPublished = newNetwork.lastValidated ? 100 : 0;
                this.mLegacyTypeTracker.add(newNetwork.networkInfo.getType(), newNetwork);
                notifyLockdownVpn(newNetwork);
            }
            if (keep) {
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
                } catch (RemoteException e2) {
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
            }
            if (reapUnvalidatedNetworks == ReapUnvalidatedNetworks.REAP) {
                for (NetworkAgentInfo nai2 : this.mNetworkAgentInfos.values()) {
                    if (unneeded(nai2)) {
                        log("Reaping " + nai2.name());
                        teardownUnneededNetwork(nai2);
                    }
                }
            }
        }
    }

    private void rematchAllNetworksAndRequests(NetworkAgentInfo changed, int oldScore) {
        if (changed != null && oldScore < changed.getCurrentScore()) {
            rematchNetworkAndRequests(changed, ReapUnvalidatedNetworks.REAP);
            return;
        }
        NetworkAgentInfo[] nais = (NetworkAgentInfo[]) this.mNetworkAgentInfos.values().toArray(new NetworkAgentInfo[this.mNetworkAgentInfos.size()]);
        Arrays.sort(nais);
        int length = nais.length;
        for (int i = 0; i < length; i++) {
            NetworkAgentInfo nai = nais[i];
            rematchNetworkAndRequests(nai, nai != nais[nais.length + (-1)] ? ReapUnvalidatedNetworks.DONT_REAP : ReapUnvalidatedNetworks.REAP);
        }
    }

    private void updateInetCondition(NetworkAgentInfo nai) {
        if (nai.everValidated && isDefaultNetwork(nai)) {
            int newInetCondition = nai.lastValidated ? 100 : 0;
            if (newInetCondition == this.mDefaultInetConditionPublished) {
                return;
            }
            this.mDefaultInetConditionPublished = newInetCondition;
            sendInetConditionBroadcast(nai.networkInfo);
        }
    }

    private void notifyLockdownVpn(NetworkAgentInfo nai) {
        if (this.mLockdownTracker == null) {
            return;
        }
        if (nai != null && nai.isVPN()) {
            this.mLockdownTracker.onVpnStateChanged(nai.networkInfo);
        } else {
            this.mLockdownTracker.onNetworkInfoChanged();
        }
    }

    private void updateNetworkInfo(NetworkAgentInfo networkAgent, NetworkInfo newInfo) {
        NetworkInfo oldInfo;
        int i;
        NetworkInfo.State state = newInfo.getState();
        int oldScore = networkAgent.getCurrentScore();
        synchronized (networkAgent) {
            oldInfo = networkAgent.networkInfo;
            networkAgent.networkInfo = newInfo;
        }
        notifyLockdownVpn(networkAgent);
        if (oldInfo != null && oldInfo.getState() == state) {
            if (oldInfo.isRoaming() != newInfo.isRoaming()) {
                if (VDBG) {
                    log("roaming status changed, notifying NetworkStatsService");
                }
                notifyIfacesChangedForNetworkStats();
                return;
            } else {
                if (VDBG) {
                    log("ignoring duplicate network state non-change");
                    return;
                }
                return;
            }
        }
        log(networkAgent.name() + " EVENT_NETWORK_INFO_CHANGED, going from " + (oldInfo == null ? "null" : oldInfo.getState()) + " to " + state);
        if (!networkAgent.created && (state == NetworkInfo.State.CONNECTED || (state == NetworkInfo.State.CONNECTING && networkAgent.isVPN()))) {
            try {
                if (networkAgent.isVPN()) {
                    INetworkManagementService iNetworkManagementService = this.mNetd;
                    int i2 = networkAgent.network.netId;
                    boolean z = !networkAgent.linkProperties.getDnsServers().isEmpty();
                    boolean z2 = networkAgent.networkMisc == null || !networkAgent.networkMisc.allowBypass;
                    iNetworkManagementService.createVirtualNetwork(i2, z, z2);
                } else {
                    this.mNetd.createPhysicalNetwork(networkAgent.network.netId, networkAgent.networkCapabilities.hasCapability(13) ? null : NetworkManagementService.PERMISSION_SYSTEM);
                }
                networkAgent.created = true;
            } catch (Exception e) {
                loge("Error creating network " + networkAgent.network.netId + ": " + e.getMessage());
                return;
            }
        }
        if (!networkAgent.everConnected && state == NetworkInfo.State.CONNECTED) {
            networkAgent.everConnected = true;
            updateLinkProperties(networkAgent, null);
            notifyIfacesChangedForNetworkStats();
            networkAgent.networkMonitor.sendMessage(NetworkMonitor.CMD_NETWORK_CONNECTED);
            if (mSkipNetworkValidation) {
                log("don't scheduleUnvalidatedPrompt");
            } else {
                scheduleUnvalidatedPrompt(networkAgent);
            }
            if (networkAgent.isVPN()) {
                synchronized (this.mProxyLock) {
                    if (!this.mDefaultProxyDisabled) {
                        this.mDefaultProxyDisabled = true;
                        if (this.mGlobalProxy == null && this.mDefaultProxy != null) {
                            sendProxyBroadcast(null);
                        }
                    }
                }
            }
            updateSignalStrengthThresholds(networkAgent, "CONNECT", null);
            rematchNetworkAndRequests(networkAgent, ReapUnvalidatedNetworks.REAP);
            notifyNetworkCallbacks(networkAgent, 524289);
        } else if (state == NetworkInfo.State.DISCONNECTED) {
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
        } else if ((oldInfo != null && oldInfo.getState() == NetworkInfo.State.SUSPENDED) || state == NetworkInfo.State.SUSPENDED) {
            if (networkAgent.getCurrentScore() != oldScore) {
                rematchAllNetworksAndRequests(networkAgent, oldScore);
            }
            if (state == NetworkInfo.State.SUSPENDED) {
                i = 524299;
            } else {
                i = 524300;
            }
            notifyNetworkCallbacks(networkAgent, i);
            this.mLegacyTypeTracker.update(networkAgent);
        }
        if ((state != NetworkInfo.State.CONNECTED && state != NetworkInfo.State.DISCONNECTED) || !networkAgent.networkCapabilities.hasCapability(0)) {
            return;
        }
        sendStickyBroadcast(new Intent("com.mediatek.conn.MMS_CONNECTIVITY"));
    }

    private void updateNetworkScore(NetworkAgentInfo nai, int score) {
        if (VDBG) {
            log("updateNetworkScore for " + nai.name() + " to " + score);
        }
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

    private void sendLegacyNetworkBroadcast(NetworkAgentInfo nai, NetworkInfo.DetailedState state, int type) {
        NetworkInfo info = new NetworkInfo(nai.networkInfo);
        info.setType(type);
        if (state != NetworkInfo.DetailedState.DISCONNECTED) {
            info.setDetailedState(state, null, info.getExtraInfo());
            sendConnectedBroadcast(info);
            return;
        }
        info.setDetailedState(state, info.getReason(), info.getExtraInfo());
        Intent intent = new Intent("android.net.conn.CONNECTIVITY_CHANGE");
        intent.putExtra("networkInfo", info);
        intent.putExtra("networkType", info.getType());
        if (info.isFailover()) {
            intent.putExtra("isFailover", true);
            nai.networkInfo.setFailover(false);
        }
        if (info.getReason() != null) {
            log("broadcast DISCONNECTED reason=" + info.getReason());
            intent.putExtra(PhoneWindowManager.SYSTEM_DIALOG_REASON_KEY, info.getReason());
        }
        if (info.getExtraInfo() != null) {
            intent.putExtra("extraInfo", info.getExtraInfo());
        }
        NetworkAgentInfo newDefaultAgent = null;
        if (nai.networkRequests.get(this.mDefaultRequest.requestId) != null) {
            newDefaultAgent = getDefaultNetwork();
            if (newDefaultAgent != null) {
                intent.putExtra("otherNetwork", newDefaultAgent.networkInfo);
            } else {
                intent.putExtra("noConnectivity", true);
            }
        }
        intent.putExtra("inetCondition", this.mDefaultInetConditionPublished);
        intent.putExtra("subId", nai.networkCapabilities.getNetworkSpecifier());
        sendStickyBroadcast(intent);
        if (newDefaultAgent == null) {
            return;
        }
        sendConnectedBroadcast(newDefaultAgent.networkInfo);
    }

    protected void notifyNetworkCallbacks(NetworkAgentInfo networkAgent, int notifyType) {
        if (VDBG) {
            log("notifyType " + notifyTypeToName(notifyType) + " for " + networkAgent.name());
        }
        for (int i = 0; i < networkAgent.networkRequests.size(); i++) {
            NetworkRequest nr = networkAgent.networkRequests.valueAt(i);
            NetworkRequestInfo nri = this.mNetworkRequests.get(nr);
            if (VDBG) {
                log(" sending notification for " + nr);
            }
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

    private void notifyIfacesChangedForNetworkStats() {
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
        boolean success;
        throwIfLockdownEnabled();
        int user = UserHandle.getUserId(Binder.getCallingUid());
        synchronized (this.mVpns) {
            success = this.mVpns.get(user).setUnderlyingNetworks(networks);
        }
        if (success) {
            notifyIfacesChangedForNetworkStats();
        }
        return success;
    }

    public String getCaptivePortalServerUrl() {
        return NetworkMonitor.getCaptivePortalServerUrl(this.mContext);
    }

    public void startNattKeepalive(Network network, int intervalSeconds, Messenger messenger, IBinder binder, String srcAddr, int srcPort, String dstAddr) {
        enforceKeepalivePermission();
        this.mKeepaliveTracker.startNattKeepalive(getNetworkAgentInfoForNetwork(network), intervalSeconds, messenger, binder, srcAddr, srcPort, dstAddr, 4500);
    }

    public void stopKeepalive(Network network, int slot) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(528396, slot, 0, network));
    }

    public void factoryReset() {
        enforceConnectivityInternalPermission();
        if (this.mUserManager.hasUserRestriction("no_network_reset")) {
            return;
        }
        int userId = UserHandle.getCallingUserId();
        setAirplaneMode(false);
        if (!this.mUserManager.hasUserRestriction("no_config_tethering")) {
            for (String tether : getTetheredIfaces()) {
                untether(tether);
            }
        }
        if (this.mUserManager.hasUserRestriction("no_config_vpn")) {
            return;
        }
        synchronized (this.mVpns) {
            String alwaysOnPackage = getAlwaysOnVpnPackage(userId);
            if (alwaysOnPackage != null) {
                setAlwaysOnVpnPackage(userId, null, false);
                setVpnPackageAuthorization(alwaysOnPackage, userId, false);
            }
        }
        VpnConfig vpnConfig = getVpnConfig(userId);
        if (vpnConfig == null) {
            return;
        }
        if (vpnConfig.legacy) {
            prepareVpn("[Legacy VPN]", "[Legacy VPN]", userId);
        } else {
            setVpnPackageAuthorization(vpnConfig.user, userId, false);
            prepareVpn(null, "[Legacy VPN]", userId);
        }
    }

    public NetworkMonitor createNetworkMonitor(Context context, Handler handler, NetworkAgentInfo nai, NetworkRequest defaultRequest) {
        return new NetworkMonitor(context, handler, nai, defaultRequest);
    }

    private static void logDefaultNetworkEvent(NetworkAgentInfo newNai, NetworkAgentInfo prevNai) {
        int newNetid = 0;
        int prevNetid = 0;
        int[] transports = new int[0];
        boolean hadIPv4 = false;
        boolean hadIPv6 = false;
        if (newNai != null) {
            newNetid = newNai.network.netId;
            transports = newNai.networkCapabilities.getTransportTypes();
        }
        if (prevNai != null) {
            prevNetid = prevNai.network.netId;
            LinkProperties lp = prevNai.linkProperties;
            hadIPv4 = lp.hasIPv4Address() ? lp.hasIPv4DefaultRoute() : false;
            hadIPv6 = lp.hasGlobalIPv6Address() ? lp.hasIPv6DefaultRoute() : false;
        }
        DefaultNetworkEvent.logEvent(newNetid, transports, prevNetid, hadIPv4, hadIPv6);
    }

    public boolean isTetheringChangeDone() {
        enforceTetherAccessPermission();
        if (!isTetheringSupported()) {
            return true;
        }
        boolean result = this.mTethering.isTetheringChangeDone();
        return result;
    }

    public String[] getTetheredIfacePairs() {
        enforceTetherAccessPermission();
        return this.mTethering.getTetheredIfacePairs();
    }

    public void setTetheringIpv6Enable(boolean enable) {
        enforceTetherAccessPermission();
        this.mTethering.setIpv6FeatureEnable(enable);
    }

    public boolean getTetheringIpv6Enable() {
        enforceTetherAccessPermission();
        return this.mTethering.getIpv6FeatureEnable();
    }

    private boolean isOnlyIpv6Address(List<InetAddress> list) {
        for (InetAddress ia : list) {
            if (ia instanceof Inet4Address) {
                return false;
            }
        }
        return true;
    }

    private class ConnectivityServiceReceiver extends BroadcastReceiver {
        ConnectivityServiceReceiver(ConnectivityService this$0, ConnectivityServiceReceiver connectivityServiceReceiver) {
            this();
        }

        private ConnectivityServiceReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }
            String action = intent.getAction();
            Slog.d(ConnectivityService.TAG, "received intent ==> " + action);
            synchronized (ConnectivityService.this.mSynchronizedObject) {
                if ("android.intent.action.TETHERING_CHANGED".equals(action)) {
                    boolean isConnected = intent.getBooleanExtra("tethering_isconnected", false);
                    ConnectivityService.this.setUsbTethering(isConnected);
                } else if (("android.intent.action.BOOT_COMPLETED".equals(action) || "android.intent.action.ACTION_BOOT_IPO".equals(action)) && ConnectivityService.sIsAutoTethering) {
                    ConnectivityService.this.setUsbTethering(true);
                    ConnectivityService.this.mHandler.sendMessageDelayed(ConnectivityService.this.mHandler.obtainMessage(100), 3000L);
                }
            }
        }
    }

    public Network getNetworkIfCreated(NetworkRequest nr) {
        NetworkAgentInfo currentNetwork;
        synchronized (this.mRequestLock) {
            HashMap<NetworkRequest, NetworkRequestInfo> requests = new HashMap<>(this.mNetworkRequests);
            for (NetworkRequestInfo nri : requests.values()) {
                if (nri.request.networkCapabilities.equalsNetCapabilities(nr.networkCapabilities) && (currentNetwork = this.mNetworkForRequestId.get(nri.request.requestId)) != null && currentNetwork.created) {
                    log("getNetworkIfCreated");
                    return currentNetwork.network;
                }
            }
            return null;
        }
    }

    public void monitorHttpRedirect(String location) {
        int appUid = Binder.getCallingUid();
        log("[NetworkHttpMonitor] monitorHttpRedirect");
        if (mNetworkHttpMonitor != null) {
            mNetworkHttpMonitor.monitorHttpRedirect(location, appUid);
            log("[NetworkHttpMonitor] calll for monitorHttpRedirect");
        } else {
            loge("Null object for mNetworkHttpMonitor");
        }
    }

    public boolean isFirewallEnabled() {
        log("[NetworkHttpMonitor] isFirewallEnabled");
        return mNetworkHttpMonitor.isFirewallEnabled();
    }

    public String getWebLocation() {
        log("[NetworkHttpMonitor] getWebLocation");
        return mNetworkHttpMonitor.getWebLocation();
    }
}
