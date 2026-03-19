package com.android.server;

import android.R;
import android.app.BroadcastOptions;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.IConnectivityManager;
import android.net.IIpConnectivityMetrics;
import android.net.INetdEventCallback;
import android.net.INetworkManagementEventObserver;
import android.net.INetworkPolicyListener;
import android.net.INetworkPolicyManager;
import android.net.INetworkStatsService;
import android.net.LinkProperties;
import android.net.MatchAllNetworkSpecifier;
import android.net.Network;
import android.net.NetworkAgent;
import android.net.NetworkCapabilities;
import android.net.NetworkConfig;
import android.net.NetworkInfo;
import android.net.NetworkMisc;
import android.net.NetworkPolicyManager;
import android.net.NetworkQuotaInfo;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.NetworkState;
import android.net.NetworkUtils;
import android.net.NetworkWatchlistManager;
import android.net.ProxyInfo;
import android.net.RouteInfo;
import android.net.UidRange;
import android.net.Uri;
import android.net.metrics.IpConnectivityLog;
import android.net.metrics.NetworkEvent;
import android.net.util.MultinetworkPolicyTracker;
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
import android.os.Parcelable;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ServiceSpecificException;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemClock;
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
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.IBatteryStats;
import com.android.internal.net.LegacyVpnInfo;
import com.android.internal.net.VpnConfig;
import com.android.internal.net.VpnInfo;
import com.android.internal.net.VpnProfile;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.MessageUtils;
import com.android.internal.util.Preconditions;
import com.android.internal.util.WakeupMessage;
import com.android.internal.util.XmlUtils;
import com.android.server.am.BatteryStatsService;
import com.android.server.audio.AudioService;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.connectivity.DataConnectionStats;
import com.android.server.connectivity.DnsManager;
import com.android.server.connectivity.IpConnectivityMetrics;
import com.android.server.connectivity.KeepaliveTracker;
import com.android.server.connectivity.LingerMonitor;
import com.android.server.connectivity.MockableSystemProperties;
import com.android.server.connectivity.MultipathPolicyTracker;
import com.android.server.connectivity.NetworkAgentInfo;
import com.android.server.connectivity.NetworkDiagnostics;
import com.android.server.connectivity.NetworkMonitor;
import com.android.server.connectivity.NetworkNotificationManager;
import com.android.server.connectivity.PacManager;
import com.android.server.connectivity.PermissionMonitor;
import com.android.server.connectivity.Tethering;
import com.android.server.connectivity.Vpn;
import com.android.server.connectivity.tethering.TetheringDependencies;
import com.android.server.net.BaseNetdEventCallback;
import com.android.server.net.BaseNetworkObserver;
import com.android.server.net.LockdownVpnTracker;
import com.android.server.net.NetworkPolicyManagerInternal;
import com.android.server.pm.DumpState;
import com.android.server.policy.PhoneWindowManager;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.utils.PriorityDump;
import com.google.android.collect.Lists;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class ConnectivityService extends IConnectivityManager.Stub implements PendingIntent.OnFinished {
    private static final String ATTR_MCC = "mcc";
    private static final String ATTR_MNC = "mnc";
    private static final boolean DBG = true;
    private static final int DEFAULT_LINGER_DELAY_MS = 30000;
    private static final String DEFAULT_TCP_BUFFER_SIZES = "4096,87380,110208,4096,16384,110208";
    private static final String DEFAULT_TCP_RWND_KEY = "net.tcp.default_init_rwnd";
    public static final String DIAG_ARG = "--diag";
    private static final int DISABLED = 0;
    private static final int ENABLED = 1;
    private static final int EVENT_APPLY_GLOBAL_HTTP_PROXY = 9;
    private static final int EVENT_CHANGE_MOBILE_DATA_ENABLED = 2;
    private static final int EVENT_CLEAR_NET_TRANSITION_WAKELOCK = 8;
    private static final int EVENT_CONFIGURE_MOBILE_DATA_ALWAYS_ON = 30;
    private static final int EVENT_EXPIRE_NET_TRANSITION_WAKELOCK = 24;
    private static final int EVENT_PRIVATE_DNS_SETTINGS_CHANGED = 37;
    private static final int EVENT_PRIVATE_DNS_VALIDATION_UPDATE = 38;
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
    private static final int EVENT_REVALIDATE_NETWORK = 36;
    private static final int EVENT_SET_ACCEPT_UNVALIDATED = 28;
    private static final int EVENT_SET_AVOID_UNVALIDATED = 35;
    private static final int EVENT_SYSTEM_READY = 25;
    private static final int EVENT_TIMEOUT_NETWORK_REQUEST = 20;
    private static final int EVENT_UNREGISTER_NETWORK_FACTORY = 23;
    private static final String LINGER_DELAY_PROPERTY = "persist.netmon.linger";
    private static final boolean LOGD_BLOCKED_NETWORKINFO = true;
    private static final boolean LOGD_RULES = false;
    private static final int MAX_NETWORK_INFO_LOGS = 40;
    private static final int MAX_NETWORK_REQUESTS_PER_UID = 100;
    private static final int MAX_NETWORK_REQUEST_LOGS = 20;
    private static final int MAX_NET_ID = 64511;
    private static final int MAX_VALIDATION_LOGS = 10;
    private static final int MAX_WAKELOCK_LOGS = 20;
    private static final int MIN_NET_ID = 100;
    private static final String NETWORK_RESTORE_DELAY_PROP_NAME = "android.telephony.apn-restore";
    private static final int PROMPT_UNVALIDATED_DELAY_MS = 8000;
    private static final String PROVISIONING_URL_PATH = "/data/misc/radio/provisioning_urls.xml";
    private static final int RESTORE_DEFAULT_NETWORK_DELAY = 60000;
    public static final String SHORT_ARG = "--short";
    private static final String TAG_PROVISIONING_URL = "provisioningUrl";
    private static final String TAG_PROVISIONING_URLS = "provisioningUrls";
    public static final String TETHERING_ARG = "tethering";
    private static final boolean VDBG = false;
    private static ConnectivityService sServiceInstance;

    @GuardedBy("mBlockedAppUids")
    private final HashSet<Integer> mBlockedAppUids;
    private final Context mContext;
    private String mCurrentTcpBufferSizes;
    private INetworkManagementEventObserver mDataActivityObserver;
    private DataConnectionStats mDataConnectionStats;
    private int mDefaultInetConditionPublished;
    private final NetworkRequest mDefaultMobileDataRequest;
    private volatile ProxyInfo mDefaultProxy;
    private boolean mDefaultProxyDisabled;
    private final NetworkRequest mDefaultRequest;
    private final DnsManager mDnsManager;
    private ProxyInfo mGlobalProxy;
    private final InternalHandler mHandler;

    @VisibleForTesting
    protected final HandlerThread mHandlerThread;
    private Intent mInitialBroadcast;
    private IIpConnectivityMetrics mIpConnectivityMetrics;
    private KeepaliveTracker mKeepaliveTracker;
    private KeyStore mKeyStore;
    private long mLastWakeLockAcquireTimestamp;
    private LegacyTypeTracker mLegacyTypeTracker;

    @VisibleForTesting
    protected int mLingerDelayMs;
    private LingerMonitor mLingerMonitor;

    @GuardedBy("mVpns")
    private boolean mLockdownEnabled;

    @GuardedBy("mVpns")
    private LockdownVpnTracker mLockdownTracker;
    private long mMaxWakelockDurationMs;
    private final IpConnectivityLog mMetricsLog;

    @VisibleForTesting
    final MultinetworkPolicyTracker mMultinetworkPolicyTracker;

    @VisibleForTesting
    final MultipathPolicyTracker mMultipathPolicyTracker;
    NetworkConfig[] mNetConfigs;

    @GuardedBy("mNetworkForNetId")
    private final SparseBooleanArray mNetIdInUse;
    private PowerManager.WakeLock mNetTransitionWakeLock;
    private int mNetTransitionWakeLockTimeout;
    private INetworkManagementService mNetd;

    @VisibleForTesting
    protected final INetdEventCallback mNetdEventCallback;
    private final HashMap<Messenger, NetworkAgentInfo> mNetworkAgentInfos;
    private final HashMap<Messenger, NetworkFactoryInfo> mNetworkFactoryInfos;

    @GuardedBy("mNetworkForNetId")
    private final SparseArray<NetworkAgentInfo> mNetworkForNetId;

    @GuardedBy("mNetworkForRequestId")
    private final SparseArray<NetworkAgentInfo> mNetworkForRequestId;
    private final LocalLog mNetworkInfoBlockingLogs;
    private int mNetworkPreference;
    private final LocalLog mNetworkRequestInfoLogs;
    private final HashMap<NetworkRequest, NetworkRequestInfo> mNetworkRequests;
    int mNetworksDefined;
    private int mNextNetId;
    private int mNextNetworkRequestId;
    private NetworkNotificationManager mNotifier;
    private PacManager mPacManager;
    private final PowerManager.WakeLock mPendingIntentWakeLock;
    private final PermissionMonitor mPermissionMonitor;
    private final INetworkPolicyListener mPolicyListener;
    private INetworkPolicyManager mPolicyManager;
    private NetworkPolicyManagerInternal mPolicyManagerInternal;
    private final PriorityDump.PriorityDumper mPriorityDumper;
    List mProtectedNetworks;
    private final File mProvisioningUrlFile;
    private Object mProxyLock;
    private final int mReleasePendingIntentDelayMs;
    private final SettingsObserver mSettingsObserver;
    private INetworkStatsService mStatsService;
    private MockableSystemProperties mSystemProperties;
    private boolean mSystemReady;
    TelephonyManager mTelephonyManager;
    private boolean mTestMode;
    private Tethering mTethering;
    private int mTotalWakelockAcquisitions;
    private long mTotalWakelockDurationMs;
    private int mTotalWakelockReleases;
    private final NetworkStateTrackerHandler mTrackerHandler;

    @GuardedBy("mUidToNetworkRequestCount")
    private final SparseIntArray mUidToNetworkRequestCount;
    private BroadcastReceiver mUserIntentReceiver;
    private UserManager mUserManager;
    private BroadcastReceiver mUserPresentReceiver;
    private final ArrayDeque<ValidationLog> mValidationLogs;

    @GuardedBy("mVpns")
    @VisibleForTesting
    protected final SparseArray<Vpn> mVpns;
    private final LocalLog mWakelockLogs;
    private static final String TAG = ConnectivityService.class.getSimpleName();
    private static final SparseArray<String> sMagicDecoderRing = MessageUtils.findMessageNames(new Class[]{AsyncChannel.class, ConnectivityService.class, NetworkAgent.class, NetworkAgentInfo.class});

    private enum ReapUnvalidatedNetworks {
        REAP,
        DONT_REAP
    }

    private enum UnneededFor {
        LINGER,
        TEARDOWN
    }

    private static String eventName(int i) {
        return sMagicDecoderRing.get(i, Integer.toString(i));
    }

    private static class ValidationLog {
        final LocalLog.ReadOnlyLocalLog mLog;
        final String mName;
        final Network mNetwork;

        ValidationLog(Network network, String str, LocalLog.ReadOnlyLocalLog readOnlyLocalLog) {
            this.mNetwork = network;
            this.mName = str;
            this.mLog = readOnlyLocalLog;
        }
    }

    private void addValidationLogs(LocalLog.ReadOnlyLocalLog readOnlyLocalLog, Network network, String str) {
        synchronized (this.mValidationLogs) {
            while (this.mValidationLogs.size() >= 10) {
                this.mValidationLogs.removeLast();
            }
            this.mValidationLogs.addFirst(new ValidationLog(network, str, readOnlyLocalLog));
        }
    }

    private class LegacyTypeTracker {
        private static final boolean DBG = true;
        private static final boolean VDBG = false;
        private final ArrayList<NetworkAgentInfo>[] mTypeLists = new ArrayList[30];

        public LegacyTypeTracker() {
        }

        public void addSupportedType(int i) {
            if (this.mTypeLists[i] != null) {
                throw new IllegalStateException("legacy list for type " + i + "already initialized");
            }
            this.mTypeLists[i] = new ArrayList<>();
        }

        public boolean isTypeSupported(int i) {
            return ConnectivityManager.isNetworkTypeValid(i) && this.mTypeLists[i] != null;
        }

        public NetworkAgentInfo getNetworkForType(int i) {
            synchronized (this.mTypeLists) {
                if (isTypeSupported(i) && !this.mTypeLists[i].isEmpty()) {
                    return this.mTypeLists[i].get(0);
                }
                return null;
            }
        }

        private void maybeLogBroadcast(NetworkAgentInfo networkAgentInfo, NetworkInfo.DetailedState detailedState, int i, boolean z) {
            ConnectivityService.log("Sending " + detailedState + " broadcast for type " + i + " " + networkAgentInfo.name() + " isDefaultNetwork=" + z);
        }

        public void add(int i, NetworkAgentInfo networkAgentInfo) {
            if (!isTypeSupported(i)) {
                return;
            }
            ArrayList<NetworkAgentInfo> arrayList = this.mTypeLists[i];
            if (arrayList.contains(networkAgentInfo)) {
                return;
            }
            synchronized (this.mTypeLists) {
                arrayList.add(networkAgentInfo);
            }
            boolean zIsDefaultNetwork = ConnectivityService.this.isDefaultNetwork(networkAgentInfo);
            if (arrayList.size() == 1 || zIsDefaultNetwork) {
                maybeLogBroadcast(networkAgentInfo, NetworkInfo.DetailedState.CONNECTED, i, zIsDefaultNetwork);
                ConnectivityService.this.sendLegacyNetworkBroadcast(networkAgentInfo, NetworkInfo.DetailedState.CONNECTED, i);
            }
        }

        public void remove(int i, NetworkAgentInfo networkAgentInfo, boolean z) {
            ArrayList<NetworkAgentInfo> arrayList = this.mTypeLists[i];
            if (arrayList == null || arrayList.isEmpty()) {
                return;
            }
            boolean zEquals = arrayList.get(0).equals(networkAgentInfo);
            synchronized (this.mTypeLists) {
                if (arrayList.remove(networkAgentInfo)) {
                    NetworkInfo.DetailedState detailedState = NetworkInfo.DetailedState.DISCONNECTED;
                    if (zEquals || z) {
                        maybeLogBroadcast(networkAgentInfo, detailedState, i, z);
                        ConnectivityService.this.sendLegacyNetworkBroadcast(networkAgentInfo, detailedState, i);
                    }
                    if (!arrayList.isEmpty() && zEquals) {
                        ConnectivityService.log("Other network available for type " + i + ", sending connected broadcast");
                        NetworkAgentInfo networkAgentInfo2 = arrayList.get(0);
                        maybeLogBroadcast(networkAgentInfo2, detailedState, i, ConnectivityService.this.isDefaultNetwork(networkAgentInfo2));
                        ConnectivityService.this.sendLegacyNetworkBroadcast(networkAgentInfo2, detailedState, i);
                    }
                }
            }
        }

        public void remove(NetworkAgentInfo networkAgentInfo, boolean z) {
            for (int i = 0; i < this.mTypeLists.length; i++) {
                remove(i, networkAgentInfo, z);
            }
        }

        public void update(NetworkAgentInfo networkAgentInfo) {
            boolean zIsDefaultNetwork = ConnectivityService.this.isDefaultNetwork(networkAgentInfo);
            NetworkInfo.DetailedState detailedState = networkAgentInfo.networkInfo.getDetailedState();
            for (int i = 0; i < this.mTypeLists.length; i++) {
                ArrayList<NetworkAgentInfo> arrayList = this.mTypeLists[i];
                boolean z = true;
                boolean z2 = arrayList != null && arrayList.contains(networkAgentInfo);
                if (!z2 || networkAgentInfo != arrayList.get(0)) {
                    z = false;
                }
                if (z || (z2 && zIsDefaultNetwork)) {
                    maybeLogBroadcast(networkAgentInfo, detailedState, i, zIsDefaultNetwork);
                    ConnectivityService.this.sendLegacyNetworkBroadcast(networkAgentInfo, detailedState, i);
                }
            }
        }

        private String naiToString(NetworkAgentInfo networkAgentInfo) {
            String str;
            String strName = networkAgentInfo != null ? networkAgentInfo.name() : "null";
            if (networkAgentInfo.networkInfo != null) {
                str = networkAgentInfo.networkInfo.getState() + SliceClientPermissions.SliceAuthority.DELIMITER + networkAgentInfo.networkInfo.getDetailedState();
            } else {
                str = "???/???";
            }
            return strName + " " + str;
        }

        public void dump(IndentingPrintWriter indentingPrintWriter) {
            indentingPrintWriter.println("mLegacyTypeTracker:");
            indentingPrintWriter.increaseIndent();
            indentingPrintWriter.print("Supported types:");
            for (int i = 0; i < this.mTypeLists.length; i++) {
                if (this.mTypeLists[i] != null) {
                    indentingPrintWriter.print(" " + i);
                }
            }
            indentingPrintWriter.println();
            indentingPrintWriter.println("Current state:");
            indentingPrintWriter.increaseIndent();
            synchronized (this.mTypeLists) {
                for (int i2 = 0; i2 < this.mTypeLists.length; i2++) {
                    if (this.mTypeLists[i2] != null && !this.mTypeLists[i2].isEmpty()) {
                        Iterator<NetworkAgentInfo> it = this.mTypeLists[i2].iterator();
                        while (it.hasNext()) {
                            indentingPrintWriter.println(i2 + " " + naiToString(it.next()));
                        }
                    }
                }
            }
            indentingPrintWriter.decreaseIndent();
            indentingPrintWriter.decreaseIndent();
            indentingPrintWriter.println();
        }
    }

    public ConnectivityService() {
        this.mVpns = new SparseArray<>();
        this.mDefaultInetConditionPublished = 0;
        this.mDefaultProxy = null;
        this.mProxyLock = new Object();
        this.mDefaultProxyDisabled = false;
        this.mGlobalProxy = null;
        this.mPacManager = null;
        this.mNextNetId = 100;
        this.mNextNetworkRequestId = 1;
        this.mNetworkRequestInfoLogs = new LocalLog(20);
        this.mNetworkInfoBlockingLogs = new LocalLog(40);
        this.mWakelockLogs = new LocalLog(20);
        this.mTotalWakelockAcquisitions = 0;
        this.mTotalWakelockReleases = 0;
        this.mTotalWakelockDurationMs = 0L;
        this.mMaxWakelockDurationMs = 0L;
        this.mLastWakeLockAcquireTimestamp = 0L;
        this.mValidationLogs = new ArrayDeque<>(10);
        this.mLegacyTypeTracker = new LegacyTypeTracker();
        this.mPriorityDumper = new PriorityDump.PriorityDumper() {
            @Override
            public void dumpHigh(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr, boolean z) {
                ConnectivityService.this.doDump(fileDescriptor, printWriter, new String[]{ConnectivityService.DIAG_ARG}, z);
                ConnectivityService.this.doDump(fileDescriptor, printWriter, new String[]{ConnectivityService.SHORT_ARG}, z);
            }

            @Override
            public void dumpNormal(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr, boolean z) {
                ConnectivityService.this.doDump(fileDescriptor, printWriter, strArr, z);
            }

            @Override
            public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr, boolean z) {
                ConnectivityService.this.doDump(fileDescriptor, printWriter, strArr, z);
            }
        };
        this.mDataActivityObserver = new BaseNetworkObserver() {
            public void interfaceClassDataActivityChanged(String str, boolean z, long j) {
                ConnectivityService.this.sendDataActivityBroadcast(Integer.parseInt(str), z, j);
            }
        };
        this.mNetdEventCallback = new BaseNetdEventCallback() {
            public void onPrivateDnsValidationEvent(int i, String str, String str2, boolean z) {
                try {
                    ConnectivityService.this.mHandler.sendMessage(ConnectivityService.this.mHandler.obtainMessage(38, new DnsManager.PrivateDnsValidationUpdate(i, InetAddress.parseNumericAddress(str), str2, z)));
                } catch (IllegalArgumentException e) {
                    ConnectivityService.loge("Error parsing ip address in validation event");
                }
            }
        };
        this.mPolicyListener = new NetworkPolicyManager.Listener() {
            public void onUidRulesChanged(int i, int i2) {
            }

            public void onRestrictBackgroundChanged(boolean z) {
                if (z) {
                    ConnectivityService.log("onRestrictBackgroundChanged(true): disabling tethering");
                    ConnectivityService.this.mTethering.untetherAll();
                }
            }
        };
        this.mProvisioningUrlFile = new File(PROVISIONING_URL_PATH);
        this.mUserIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                int intExtra = intent.getIntExtra("android.intent.extra.user_handle", -10000);
                if (intExtra == -10000) {
                    return;
                }
                if ("android.intent.action.USER_STARTED".equals(action)) {
                    ConnectivityService.this.onUserStart(intExtra);
                    return;
                }
                if ("android.intent.action.USER_STOPPED".equals(action)) {
                    ConnectivityService.this.onUserStop(intExtra);
                    return;
                }
                if ("android.intent.action.USER_ADDED".equals(action)) {
                    ConnectivityService.this.onUserAdded(intExtra);
                } else if ("android.intent.action.USER_REMOVED".equals(action)) {
                    ConnectivityService.this.onUserRemoved(intExtra);
                } else if ("android.intent.action.USER_UNLOCKED".equals(action)) {
                    ConnectivityService.this.onUserUnlocked(intExtra);
                }
            }
        };
        this.mUserPresentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ConnectivityService.this.updateLockdownVpn();
                ConnectivityService.this.mContext.unregisterReceiver(this);
            }
        };
        this.mNetworkFactoryInfos = new HashMap<>();
        this.mNetworkRequests = new HashMap<>();
        this.mUidToNetworkRequestCount = new SparseIntArray();
        this.mNetworkForRequestId = new SparseArray<>();
        this.mNetworkForNetId = new SparseArray<>();
        this.mNetIdInUse = new SparseBooleanArray();
        this.mNetworkAgentInfos = new HashMap<>();
        this.mBlockedAppUids = new HashSet<>();
        this.mContext = null;
        this.mDefaultMobileDataRequest = null;
        this.mDefaultRequest = null;
        this.mHandler = null;
        this.mHandlerThread = null;
        this.mMetricsLog = null;
        this.mMultinetworkPolicyTracker = null;
        this.mPendingIntentWakeLock = null;
        this.mPermissionMonitor = null;
        this.mReleasePendingIntentDelayMs = 0;
        this.mSettingsObserver = null;
        this.mTrackerHandler = null;
        this.mMultipathPolicyTracker = null;
        this.mDnsManager = null;
    }

    public ConnectivityService(Context context, INetworkManagementService iNetworkManagementService, INetworkStatsService iNetworkStatsService, INetworkPolicyManager iNetworkPolicyManager) {
        this(context, iNetworkManagementService, iNetworkStatsService, iNetworkPolicyManager, new IpConnectivityLog());
    }

    @VisibleForTesting
    protected ConnectivityService(Context context, INetworkManagementService iNetworkManagementService, INetworkStatsService iNetworkStatsService, INetworkPolicyManager iNetworkPolicyManager, IpConnectivityLog ipConnectivityLog) {
        this.mVpns = new SparseArray<>();
        boolean z = false;
        this.mDefaultInetConditionPublished = 0;
        this.mDefaultProxy = null;
        this.mProxyLock = new Object();
        this.mDefaultProxyDisabled = false;
        this.mGlobalProxy = null;
        this.mPacManager = null;
        this.mNextNetId = 100;
        this.mNextNetworkRequestId = 1;
        this.mNetworkRequestInfoLogs = new LocalLog(20);
        this.mNetworkInfoBlockingLogs = new LocalLog(40);
        this.mWakelockLogs = new LocalLog(20);
        this.mTotalWakelockAcquisitions = 0;
        this.mTotalWakelockReleases = 0;
        this.mTotalWakelockDurationMs = 0L;
        this.mMaxWakelockDurationMs = 0L;
        this.mLastWakeLockAcquireTimestamp = 0L;
        this.mValidationLogs = new ArrayDeque<>(10);
        this.mLegacyTypeTracker = new LegacyTypeTracker();
        this.mPriorityDumper = new PriorityDump.PriorityDumper() {
            @Override
            public void dumpHigh(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr, boolean z2) {
                ConnectivityService.this.doDump(fileDescriptor, printWriter, new String[]{ConnectivityService.DIAG_ARG}, z2);
                ConnectivityService.this.doDump(fileDescriptor, printWriter, new String[]{ConnectivityService.SHORT_ARG}, z2);
            }

            @Override
            public void dumpNormal(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr, boolean z2) {
                ConnectivityService.this.doDump(fileDescriptor, printWriter, strArr, z2);
            }

            @Override
            public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr, boolean z2) {
                ConnectivityService.this.doDump(fileDescriptor, printWriter, strArr, z2);
            }
        };
        this.mDataActivityObserver = new BaseNetworkObserver() {
            public void interfaceClassDataActivityChanged(String str, boolean z2, long j) {
                ConnectivityService.this.sendDataActivityBroadcast(Integer.parseInt(str), z2, j);
            }
        };
        this.mNetdEventCallback = new BaseNetdEventCallback() {
            public void onPrivateDnsValidationEvent(int i, String str, String str2, boolean z2) {
                try {
                    ConnectivityService.this.mHandler.sendMessage(ConnectivityService.this.mHandler.obtainMessage(38, new DnsManager.PrivateDnsValidationUpdate(i, InetAddress.parseNumericAddress(str), str2, z2)));
                } catch (IllegalArgumentException e) {
                    ConnectivityService.loge("Error parsing ip address in validation event");
                }
            }
        };
        this.mPolicyListener = new NetworkPolicyManager.Listener() {
            public void onUidRulesChanged(int i, int i2) {
            }

            public void onRestrictBackgroundChanged(boolean z2) {
                if (z2) {
                    ConnectivityService.log("onRestrictBackgroundChanged(true): disabling tethering");
                    ConnectivityService.this.mTethering.untetherAll();
                }
            }
        };
        this.mProvisioningUrlFile = new File(PROVISIONING_URL_PATH);
        this.mUserIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                int intExtra = intent.getIntExtra("android.intent.extra.user_handle", -10000);
                if (intExtra == -10000) {
                    return;
                }
                if ("android.intent.action.USER_STARTED".equals(action)) {
                    ConnectivityService.this.onUserStart(intExtra);
                    return;
                }
                if ("android.intent.action.USER_STOPPED".equals(action)) {
                    ConnectivityService.this.onUserStop(intExtra);
                    return;
                }
                if ("android.intent.action.USER_ADDED".equals(action)) {
                    ConnectivityService.this.onUserAdded(intExtra);
                } else if ("android.intent.action.USER_REMOVED".equals(action)) {
                    ConnectivityService.this.onUserRemoved(intExtra);
                } else if ("android.intent.action.USER_UNLOCKED".equals(action)) {
                    ConnectivityService.this.onUserUnlocked(intExtra);
                }
            }
        };
        this.mUserPresentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                ConnectivityService.this.updateLockdownVpn();
                ConnectivityService.this.mContext.unregisterReceiver(this);
            }
        };
        this.mNetworkFactoryInfos = new HashMap<>();
        this.mNetworkRequests = new HashMap<>();
        this.mUidToNetworkRequestCount = new SparseIntArray();
        this.mNetworkForRequestId = new SparseArray<>();
        this.mNetworkForNetId = new SparseArray<>();
        this.mNetIdInUse = new SparseBooleanArray();
        this.mNetworkAgentInfos = new HashMap<>();
        this.mBlockedAppUids = new HashSet<>();
        log("ConnectivityService starting up");
        this.mSystemProperties = getSystemProperties();
        this.mMetricsLog = ipConnectivityLog;
        this.mDefaultRequest = createDefaultInternetRequestForTransport(-1, NetworkRequest.Type.REQUEST);
        NetworkRequestInfo networkRequestInfo = new NetworkRequestInfo(null, this.mDefaultRequest, new Binder());
        this.mNetworkRequests.put(this.mDefaultRequest, networkRequestInfo);
        this.mNetworkRequestInfoLogs.log("REGISTER " + networkRequestInfo);
        this.mDefaultMobileDataRequest = createDefaultInternetRequestForTransport(0, NetworkRequest.Type.BACKGROUND_REQUEST);
        this.mHandlerThread = new HandlerThread("ConnectivityServiceThread");
        this.mHandlerThread.start();
        this.mHandler = new InternalHandler(this.mHandlerThread.getLooper());
        this.mTrackerHandler = new NetworkStateTrackerHandler(this.mHandlerThread.getLooper());
        this.mReleasePendingIntentDelayMs = Settings.Secure.getInt(context.getContentResolver(), "connectivity_release_pending_intent_delay_ms", 5000);
        this.mLingerDelayMs = this.mSystemProperties.getInt(LINGER_DELAY_PROPERTY, DEFAULT_LINGER_DELAY_MS);
        this.mContext = (Context) Preconditions.checkNotNull(context, "missing Context");
        this.mNetd = (INetworkManagementService) Preconditions.checkNotNull(iNetworkManagementService, "missing INetworkManagementService");
        this.mStatsService = (INetworkStatsService) Preconditions.checkNotNull(iNetworkStatsService, "missing INetworkStatsService");
        this.mPolicyManager = (INetworkPolicyManager) Preconditions.checkNotNull(iNetworkPolicyManager, "missing INetworkPolicyManager");
        this.mPolicyManagerInternal = (NetworkPolicyManagerInternal) Preconditions.checkNotNull((NetworkPolicyManagerInternal) LocalServices.getService(NetworkPolicyManagerInternal.class), "missing NetworkPolicyManagerInternal");
        this.mKeyStore = KeyStore.getInstance();
        this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        try {
            this.mPolicyManager.registerListener(this.mPolicyListener);
        } catch (RemoteException e) {
            loge("unable to register INetworkPolicyListener" + e);
        }
        PowerManager powerManager = (PowerManager) context.getSystemService("power");
        this.mNetTransitionWakeLock = powerManager.newWakeLock(1, TAG);
        this.mNetTransitionWakeLockTimeout = this.mContext.getResources().getInteger(R.integer.config_default_cellular_usage_setting);
        this.mPendingIntentWakeLock = powerManager.newWakeLock(1, TAG);
        this.mNetConfigs = new NetworkConfig[30];
        boolean z2 = this.mSystemProperties.getBoolean("ro.radio.noril", false);
        log("wifiOnly=" + z2);
        String[] stringArray = context.getResources().getStringArray(R.array.config_displayWhiteBalanceHighLightAmbientBrightnesses);
        int length = stringArray.length;
        for (int i = 0; i < length; i++) {
            try {
                NetworkConfig networkConfig = new NetworkConfig(stringArray[i]);
                if (networkConfig.type > 29) {
                    loge("Error in networkAttributes - ignoring attempt to define type " + networkConfig.type);
                } else if (z2 && ConnectivityManager.isNetworkTypeMobile(networkConfig.type)) {
                    log("networkAttributes - ignoring mobile as this dev is wifiOnly " + networkConfig.type);
                } else if (this.mNetConfigs[networkConfig.type] != null) {
                    loge("Error in networkAttributes - ignoring attempt to redefine type " + networkConfig.type);
                } else {
                    this.mLegacyTypeTracker.addSupportedType(networkConfig.type);
                    this.mNetConfigs[networkConfig.type] = networkConfig;
                    this.mNetworksDefined++;
                }
            } catch (Exception e2) {
            }
        }
        if (this.mNetConfigs[17] == null) {
            this.mLegacyTypeTracker.addSupportedType(17);
            this.mNetworksDefined++;
        }
        if (this.mNetConfigs[9] == null && hasService("ethernet")) {
            this.mLegacyTypeTracker.addSupportedType(9);
            this.mNetworksDefined++;
        }
        this.mProtectedNetworks = new ArrayList();
        for (int i2 : context.getResources().getIntArray(R.array.config_defaultAmbientContextServices)) {
            if (this.mNetConfigs[i2] == null || this.mProtectedNetworks.contains(Integer.valueOf(i2))) {
                loge("Ignoring protectedNetwork " + i2);
            } else {
                this.mProtectedNetworks.add(Integer.valueOf(i2));
            }
        }
        if (this.mSystemProperties.get("cm.test.mode").equals("true") && this.mSystemProperties.get("ro.build.type").equals("eng")) {
            z = true;
        }
        this.mTestMode = z;
        this.mTethering = makeTethering();
        this.mPermissionMonitor = new PermissionMonitor(this.mContext, this.mNetd);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_STARTED");
        intentFilter.addAction("android.intent.action.USER_STOPPED");
        intentFilter.addAction("android.intent.action.USER_ADDED");
        intentFilter.addAction("android.intent.action.USER_REMOVED");
        intentFilter.addAction("android.intent.action.USER_UNLOCKED");
        this.mContext.registerReceiverAsUser(this.mUserIntentReceiver, UserHandle.ALL, intentFilter, null, null);
        this.mContext.registerReceiverAsUser(this.mUserPresentReceiver, UserHandle.SYSTEM, new IntentFilter("android.intent.action.USER_PRESENT"), null, null);
        try {
            this.mNetd.registerObserver(this.mTethering);
            this.mNetd.registerObserver(this.mDataActivityObserver);
        } catch (RemoteException e3) {
            loge("Error registering observer :" + e3);
        }
        this.mSettingsObserver = new SettingsObserver(this.mContext, this.mHandler);
        registerSettingsCallbacks();
        this.mDataConnectionStats = new DataConnectionStats(this.mContext);
        this.mDataConnectionStats.startMonitoring();
        this.mPacManager = new PacManager(this.mContext, this.mHandler, 16);
        this.mUserManager = (UserManager) context.getSystemService("user");
        this.mKeepaliveTracker = new KeepaliveTracker(this.mHandler);
        this.mNotifier = new NetworkNotificationManager(this.mContext, this.mTelephonyManager, (NotificationManager) this.mContext.getSystemService(NotificationManager.class));
        this.mLingerMonitor = new LingerMonitor(this.mContext, this.mNotifier, Settings.Global.getInt(this.mContext.getContentResolver(), "network_switch_notification_daily_limit", 3), Settings.Global.getLong(this.mContext.getContentResolver(), "network_switch_notification_rate_limit_millis", 60000L));
        this.mMultinetworkPolicyTracker = createMultinetworkPolicyTracker(this.mContext, this.mHandler, new Runnable() {
            @Override
            public final void run() {
                this.f$0.rematchForAvoidBadWifiUpdate();
            }
        });
        this.mMultinetworkPolicyTracker.start();
        this.mMultipathPolicyTracker = new MultipathPolicyTracker(this.mContext, this.mHandler);
        this.mDnsManager = new DnsManager(this.mContext, this.mNetd, this.mSystemProperties);
        registerPrivateDnsSettingsCallbacks();
    }

    private Tethering makeTethering() {
        return new Tethering(this.mContext, this.mNetd, this.mStatsService, this.mPolicyManager, IoThread.get().getLooper(), new MockableSystemProperties(), new TetheringDependencies() {
            @Override
            public boolean isTetheringSupported() {
                return ConnectivityService.this.isTetheringSupported();
            }
        });
    }

    private static NetworkCapabilities createDefaultNetworkCapabilitiesForUid(int i) {
        NetworkCapabilities networkCapabilities = new NetworkCapabilities();
        networkCapabilities.addCapability(12);
        networkCapabilities.addCapability(13);
        networkCapabilities.removeCapability(15);
        networkCapabilities.setSingleUid(i);
        return networkCapabilities;
    }

    private NetworkRequest createDefaultInternetRequestForTransport(int i, NetworkRequest.Type type) {
        NetworkCapabilities networkCapabilities = new NetworkCapabilities();
        networkCapabilities.addCapability(12);
        networkCapabilities.addCapability(13);
        if (i > -1) {
            networkCapabilities.addTransportType(i);
        }
        return new NetworkRequest(networkCapabilities, -1, nextNetworkRequestId(), type);
    }

    @VisibleForTesting
    void updateMobileDataAlwaysOn() {
        this.mHandler.sendEmptyMessage(30);
    }

    @VisibleForTesting
    void updatePrivateDnsSettings() {
        this.mHandler.sendEmptyMessage(37);
    }

    private void handleMobileDataAlwaysOn() {
        boolean bool = toBool(Settings.Global.getInt(this.mContext.getContentResolver(), "mobile_data_always_on", 1));
        if (bool == (this.mNetworkRequests.get(this.mDefaultMobileDataRequest) != null)) {
            return;
        }
        if (bool) {
            handleRegisterNetworkRequest(new NetworkRequestInfo(null, this.mDefaultMobileDataRequest, new Binder()));
        } else {
            handleReleaseNetworkRequest(this.mDefaultMobileDataRequest, 1000);
        }
    }

    private void registerSettingsCallbacks() {
        this.mSettingsObserver.observe(Settings.Global.getUriFor("http_proxy"), 9);
        this.mSettingsObserver.observe(Settings.Global.getUriFor("mobile_data_always_on"), 30);
    }

    private void registerPrivateDnsSettingsCallbacks() {
        for (Uri uri : DnsManager.getPrivateDnsSettingsUris()) {
            this.mSettingsObserver.observe(uri, 37);
        }
    }

    private synchronized int nextNetworkRequestId() {
        int i;
        i = this.mNextNetworkRequestId;
        this.mNextNetworkRequestId = i + 1;
        return i;
    }

    @VisibleForTesting
    protected int reserveNetId() {
        synchronized (this.mNetworkForNetId) {
            for (int i = 100; i <= MAX_NET_ID; i++) {
                try {
                    int i2 = this.mNextNetId;
                    int i3 = this.mNextNetId + 1;
                    this.mNextNetId = i3;
                    if (i3 > MAX_NET_ID) {
                        this.mNextNetId = 100;
                    }
                    if (!this.mNetIdInUse.get(i2)) {
                        this.mNetIdInUse.put(i2, true);
                        return i2;
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
            throw new IllegalStateException("No free netIds");
        }
    }

    private NetworkState getFilteredNetworkState(int i, int i2, boolean z) {
        NetworkState networkState;
        if (this.mLegacyTypeTracker.isTypeSupported(i)) {
            NetworkAgentInfo networkForType = this.mLegacyTypeTracker.getNetworkForType(i);
            if (networkForType != null) {
                NetworkState networkState2 = networkForType.getNetworkState();
                networkState2.networkInfo.setType(i);
                networkState = networkState2;
            } else {
                NetworkInfo networkInfo = new NetworkInfo(i, 0, ConnectivityManager.getNetworkTypeName(i), BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                networkInfo.setDetailedState(NetworkInfo.DetailedState.DISCONNECTED, null, null);
                networkInfo.setIsAvailable(true);
                NetworkCapabilities networkCapabilities = new NetworkCapabilities();
                networkCapabilities.setCapability(18, true ^ networkInfo.isRoaming());
                networkState = new NetworkState(networkInfo, new LinkProperties(), networkCapabilities, (Network) null, (String) null, (String) null);
            }
            filterNetworkStateForUid(networkState, i2, z);
            return networkState;
        }
        return NetworkState.EMPTY;
    }

    private NetworkAgentInfo getNetworkAgentInfoForNetwork(Network network) {
        if (network == null) {
            return null;
        }
        return getNetworkAgentInfoForNetId(network.netId);
    }

    private NetworkAgentInfo getNetworkAgentInfoForNetId(int i) {
        NetworkAgentInfo networkAgentInfo;
        synchronized (this.mNetworkForNetId) {
            networkAgentInfo = this.mNetworkForNetId.get(i);
        }
        return networkAgentInfo;
    }

    private Network[] getVpnUnderlyingNetworks(int i) {
        synchronized (this.mVpns) {
            if (!this.mLockdownEnabled) {
                Vpn vpn = this.mVpns.get(UserHandle.getUserId(i));
                if (vpn != null && vpn.appliesToUid(i)) {
                    return vpn.getUnderlyingNetworks();
                }
            }
            return null;
        }
    }

    private NetworkState getUnfilteredActiveNetworkState(int i) {
        NetworkAgentInfo defaultNetwork = getDefaultNetwork();
        Network[] vpnUnderlyingNetworks = getVpnUnderlyingNetworks(i);
        if (vpnUnderlyingNetworks != null) {
            if (vpnUnderlyingNetworks.length > 0) {
                defaultNetwork = getNetworkAgentInfoForNetwork(vpnUnderlyingNetworks[0]);
            } else {
                defaultNetwork = null;
            }
        }
        if (defaultNetwork != null) {
            return defaultNetwork.getNetworkState();
        }
        return NetworkState.EMPTY;
    }

    private boolean isNetworkWithLinkPropertiesBlocked(LinkProperties linkProperties, int i, boolean z) {
        if (z || isSystem(i)) {
            return false;
        }
        synchronized (this.mVpns) {
            Vpn vpn = this.mVpns.get(UserHandle.getUserId(i));
            if (vpn != null && vpn.isBlockingUid(i)) {
                return true;
            }
            return this.mPolicyManagerInternal.isUidNetworkingBlocked(i, linkProperties == null ? BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS : linkProperties.getInterfaceName());
        }
    }

    private void maybeLogBlockedNetworkInfo(NetworkInfo networkInfo, int i) {
        boolean z;
        if (networkInfo == null) {
            return;
        }
        synchronized (this.mBlockedAppUids) {
            if (networkInfo.getDetailedState() != NetworkInfo.DetailedState.BLOCKED || !this.mBlockedAppUids.add(Integer.valueOf(i))) {
                if (!networkInfo.isConnected() || !this.mBlockedAppUids.remove(Integer.valueOf(i))) {
                    return;
                } else {
                    z = false;
                }
            } else {
                z = true;
            }
            String str = z ? "BLOCKED" : "UNBLOCKED";
            log(String.format("Returning %s NetworkInfo to uid=%d", str, Integer.valueOf(i)));
            this.mNetworkInfoBlockingLogs.log(str + " " + i);
        }
    }

    private void filterNetworkStateForUid(NetworkState networkState, int i, boolean z) {
        if (networkState == null || networkState.networkInfo == null || networkState.linkProperties == null) {
            return;
        }
        if (isNetworkWithLinkPropertiesBlocked(networkState.linkProperties, i, z)) {
            networkState.networkInfo.setDetailedState(NetworkInfo.DetailedState.BLOCKED, null, null);
        }
        synchronized (this.mVpns) {
            if (this.mLockdownTracker != null) {
                this.mLockdownTracker.augmentNetworkInfo(networkState.networkInfo);
            }
        }
    }

    public NetworkInfo getActiveNetworkInfo() {
        enforceAccessPermission();
        int callingUid = Binder.getCallingUid();
        NetworkState unfilteredActiveNetworkState = getUnfilteredActiveNetworkState(callingUid);
        filterNetworkStateForUid(unfilteredActiveNetworkState, callingUid, false);
        maybeLogBlockedNetworkInfo(unfilteredActiveNetworkState.networkInfo, callingUid);
        return unfilteredActiveNetworkState.networkInfo;
    }

    public Network getActiveNetwork() {
        enforceAccessPermission();
        return getActiveNetworkForUidInternal(Binder.getCallingUid(), false);
    }

    public Network getActiveNetworkForUid(int i, boolean z) {
        enforceConnectivityInternalPermission();
        return getActiveNetworkForUidInternal(i, z);
    }

    private Network getActiveNetworkForUidInternal(int i, boolean z) {
        int netId;
        NetworkAgentInfo networkAgentInfoForNetId;
        int userId = UserHandle.getUserId(i);
        synchronized (this.mVpns) {
            Vpn vpn = this.mVpns.get(userId);
            netId = (vpn == null || !vpn.appliesToUid(i)) ? 0 : vpn.getNetId();
        }
        if (netId != 0 && (networkAgentInfoForNetId = getNetworkAgentInfoForNetId(netId)) != null && createDefaultNetworkCapabilitiesForUid(i).satisfiedByNetworkCapabilities(networkAgentInfoForNetId.networkCapabilities)) {
            return networkAgentInfoForNetId.network;
        }
        NetworkAgentInfo defaultNetwork = getDefaultNetwork();
        if (defaultNetwork != null && isNetworkWithLinkPropertiesBlocked(defaultNetwork.linkProperties, i, z)) {
            defaultNetwork = null;
        }
        if (defaultNetwork != null) {
            return defaultNetwork.network;
        }
        return null;
    }

    public NetworkInfo getActiveNetworkInfoUnfiltered() {
        enforceAccessPermission();
        return getUnfilteredActiveNetworkState(Binder.getCallingUid()).networkInfo;
    }

    public NetworkInfo getActiveNetworkInfoForUid(int i, boolean z) {
        enforceConnectivityInternalPermission();
        NetworkState unfilteredActiveNetworkState = getUnfilteredActiveNetworkState(i);
        filterNetworkStateForUid(unfilteredActiveNetworkState, i, z);
        return unfilteredActiveNetworkState.networkInfo;
    }

    public NetworkInfo getNetworkInfo(int i) {
        enforceAccessPermission();
        int callingUid = Binder.getCallingUid();
        if (getVpnUnderlyingNetworks(callingUid) != null) {
            NetworkState unfilteredActiveNetworkState = getUnfilteredActiveNetworkState(callingUid);
            if (unfilteredActiveNetworkState.networkInfo != null && unfilteredActiveNetworkState.networkInfo.getType() == i) {
                filterNetworkStateForUid(unfilteredActiveNetworkState, callingUid, false);
                return unfilteredActiveNetworkState.networkInfo;
            }
        }
        return getFilteredNetworkState(i, callingUid, false).networkInfo;
    }

    public NetworkInfo getNetworkInfoForUid(Network network, int i, boolean z) {
        enforceAccessPermission();
        NetworkAgentInfo networkAgentInfoForNetwork = getNetworkAgentInfoForNetwork(network);
        if (networkAgentInfoForNetwork != null) {
            NetworkState networkState = networkAgentInfoForNetwork.getNetworkState();
            filterNetworkStateForUid(networkState, i, z);
            return networkState.networkInfo;
        }
        return null;
    }

    public NetworkInfo[] getAllNetworkInfo() {
        enforceAccessPermission();
        ArrayList arrayListNewArrayList = Lists.newArrayList();
        for (int i = 0; i <= 29; i++) {
            NetworkInfo networkInfo = getNetworkInfo(i);
            if (networkInfo != null) {
                arrayListNewArrayList.add(networkInfo);
            }
        }
        return (NetworkInfo[]) arrayListNewArrayList.toArray(new NetworkInfo[arrayListNewArrayList.size()]);
    }

    public Network getNetworkForType(int i) {
        enforceAccessPermission();
        int callingUid = Binder.getCallingUid();
        NetworkState filteredNetworkState = getFilteredNetworkState(i, callingUid, false);
        if (!isNetworkWithLinkPropertiesBlocked(filteredNetworkState.linkProperties, callingUid, false)) {
            return filteredNetworkState.network;
        }
        return null;
    }

    public Network[] getAllNetworks() {
        Network[] networkArr;
        enforceAccessPermission();
        synchronized (this.mNetworkForNetId) {
            networkArr = new Network[this.mNetworkForNetId.size()];
            for (int i = 0; i < this.mNetworkForNetId.size(); i++) {
                networkArr[i] = this.mNetworkForNetId.valueAt(i).network;
            }
        }
        return networkArr;
    }

    public NetworkCapabilities[] getDefaultNetworkCapabilitiesForUser(int i) {
        Vpn vpn;
        Network[] underlyingNetworks;
        enforceAccessPermission();
        HashMap map = new HashMap();
        NetworkAgentInfo defaultNetwork = getDefaultNetwork();
        NetworkCapabilities networkCapabilitiesInternal = getNetworkCapabilitiesInternal(defaultNetwork);
        if (networkCapabilitiesInternal != null) {
            map.put(defaultNetwork.network, networkCapabilitiesInternal);
        }
        synchronized (this.mVpns) {
            if (!this.mLockdownEnabled && (vpn = this.mVpns.get(i)) != null && (underlyingNetworks = vpn.getUnderlyingNetworks()) != null) {
                for (Network network : underlyingNetworks) {
                    NetworkCapabilities networkCapabilitiesInternal2 = getNetworkCapabilitiesInternal(getNetworkAgentInfoForNetwork(network));
                    if (networkCapabilitiesInternal2 != null) {
                        map.put(network, networkCapabilitiesInternal2);
                    }
                }
            }
        }
        return (NetworkCapabilities[]) map.values().toArray(new NetworkCapabilities[map.size()]);
    }

    public boolean isNetworkSupported(int i) {
        enforceAccessPermission();
        return this.mLegacyTypeTracker.isTypeSupported(i);
    }

    public LinkProperties getActiveLinkProperties() {
        enforceAccessPermission();
        return getUnfilteredActiveNetworkState(Binder.getCallingUid()).linkProperties;
    }

    public LinkProperties getLinkPropertiesForType(int i) {
        LinkProperties linkProperties;
        enforceAccessPermission();
        NetworkAgentInfo networkForType = this.mLegacyTypeTracker.getNetworkForType(i);
        if (networkForType != null) {
            synchronized (networkForType) {
                linkProperties = new LinkProperties(networkForType.linkProperties);
            }
            return linkProperties;
        }
        return null;
    }

    public LinkProperties getLinkProperties(Network network) {
        enforceAccessPermission();
        return getLinkProperties(getNetworkAgentInfoForNetwork(network));
    }

    private LinkProperties getLinkProperties(NetworkAgentInfo networkAgentInfo) {
        LinkProperties linkProperties;
        if (networkAgentInfo == null) {
            return null;
        }
        synchronized (networkAgentInfo) {
            linkProperties = new LinkProperties(networkAgentInfo.linkProperties);
        }
        return linkProperties;
    }

    private NetworkCapabilities getNetworkCapabilitiesInternal(NetworkAgentInfo networkAgentInfo) {
        if (networkAgentInfo != null) {
            synchronized (networkAgentInfo) {
                if (networkAgentInfo.networkCapabilities != null) {
                    return networkCapabilitiesRestrictedForCallerPermissions(networkAgentInfo.networkCapabilities, Binder.getCallingPid(), Binder.getCallingUid());
                }
                return null;
            }
        }
        return null;
    }

    public NetworkCapabilities getNetworkCapabilities(Network network) {
        enforceAccessPermission();
        return getNetworkCapabilitiesInternal(getNetworkAgentInfoForNetwork(network));
    }

    private NetworkCapabilities networkCapabilitiesRestrictedForCallerPermissions(NetworkCapabilities networkCapabilities, int i, int i2) {
        NetworkCapabilities networkCapabilities2 = new NetworkCapabilities(networkCapabilities);
        if (!checkSettingsPermission(i, i2)) {
            networkCapabilities2.setUids(null);
            networkCapabilities2.setSSID(null);
        }
        if (networkCapabilities2.getNetworkSpecifier() != null) {
            networkCapabilities2.setNetworkSpecifier(networkCapabilities2.getNetworkSpecifier().redact());
        }
        return networkCapabilities2;
    }

    private void restrictRequestUidsForCaller(NetworkCapabilities networkCapabilities) {
        if (!checkSettingsPermission()) {
            networkCapabilities.setSingleUid(Binder.getCallingUid());
        }
    }

    private void restrictBackgroundRequestForCaller(NetworkCapabilities networkCapabilities) {
        if (!this.mPermissionMonitor.hasUseBackgroundNetworksPermission(Binder.getCallingUid())) {
            networkCapabilities.addCapability(19);
        }
    }

    public NetworkState[] getAllNetworkState() {
        enforceConnectivityInternalPermission();
        ArrayList arrayListNewArrayList = Lists.newArrayList();
        for (Network network : getAllNetworks()) {
            NetworkAgentInfo networkAgentInfoForNetwork = getNetworkAgentInfoForNetwork(network);
            if (networkAgentInfoForNetwork != null) {
                arrayListNewArrayList.add(networkAgentInfoForNetwork.getNetworkState());
            }
        }
        return (NetworkState[]) arrayListNewArrayList.toArray(new NetworkState[arrayListNewArrayList.size()]);
    }

    @Deprecated
    public NetworkQuotaInfo getActiveNetworkQuotaInfo() {
        Log.w(TAG, "Shame on UID " + Binder.getCallingUid() + " for calling the hidden API getNetworkQuotaInfo(). Shame!");
        return new NetworkQuotaInfo();
    }

    public boolean isActiveNetworkMetered() {
        enforceAccessPermission();
        if (getUnfilteredActiveNetworkState(Binder.getCallingUid()).networkCapabilities != null) {
            return !r0.hasCapability(11);
        }
        return true;
    }

    public boolean requestRouteToHostAddress(int i, byte[] bArr) {
        NetworkInfo.DetailedState detailedState;
        LinkProperties linkProperties;
        int i2;
        enforceChangePermission();
        if (this.mProtectedNetworks.contains(Integer.valueOf(i))) {
            enforceConnectivityInternalPermission();
        }
        try {
            InetAddress byAddress = InetAddress.getByAddress(bArr);
            if (!ConnectivityManager.isNetworkTypeValid(i)) {
                log("requestRouteToHostAddress on invalid network: " + i);
                return false;
            }
            NetworkAgentInfo networkForType = this.mLegacyTypeTracker.getNetworkForType(i);
            if (networkForType == null) {
                if (!this.mLegacyTypeTracker.isTypeSupported(i)) {
                    log("requestRouteToHostAddress on unsupported network: " + i);
                } else {
                    log("requestRouteToHostAddress on down network: " + i);
                }
                return false;
            }
            synchronized (networkForType) {
                detailedState = networkForType.networkInfo.getDetailedState();
            }
            if (detailedState != NetworkInfo.DetailedState.CONNECTED && detailedState != NetworkInfo.DetailedState.CAPTIVE_PORTAL_CHECK) {
                return false;
            }
            int callingUid = Binder.getCallingUid();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (networkForType) {
                    linkProperties = networkForType.linkProperties;
                    i2 = networkForType.network.netId;
                }
                boolean zAddLegacyRouteToHost = addLegacyRouteToHost(linkProperties, byAddress, i2, callingUid);
                log("requestRouteToHostAddress ok=" + zAddLegacyRouteToHost);
                return zAddLegacyRouteToHost;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        } catch (UnknownHostException e) {
            log("requestRouteToHostAddress got " + e.toString());
            return false;
        }
    }

    private boolean addLegacyRouteToHost(LinkProperties linkProperties, InetAddress inetAddress, int i, int i2) {
        RouteInfo routeInfoMakeHostRoute;
        RouteInfo routeInfoSelectBestRoute = RouteInfo.selectBestRoute(linkProperties.getAllRoutes(), inetAddress);
        if (routeInfoSelectBestRoute == null) {
            routeInfoMakeHostRoute = RouteInfo.makeHostRoute(inetAddress, linkProperties.getInterfaceName());
        } else {
            String str = routeInfoSelectBestRoute.getInterface();
            if (routeInfoSelectBestRoute.getGateway().equals(inetAddress)) {
                routeInfoMakeHostRoute = RouteInfo.makeHostRoute(inetAddress, str);
            } else {
                routeInfoMakeHostRoute = RouteInfo.makeHostRoute(inetAddress, routeInfoSelectBestRoute.getGateway(), str);
            }
        }
        log("Adding legacy route " + routeInfoMakeHostRoute + " for UID/PID " + i2 + SliceClientPermissions.SliceAuthority.DELIMITER + Binder.getCallingPid());
        try {
            this.mNetd.addLegacyRouteForNetId(i, routeInfoMakeHostRoute, i2);
            return true;
        } catch (Exception e) {
            loge("Exception trying to add a route: " + e);
            return false;
        }
    }

    @VisibleForTesting
    protected void registerNetdEventCallback() {
        this.mIpConnectivityMetrics = IIpConnectivityMetrics.Stub.asInterface(ServiceManager.getService("connmetrics"));
        if (this.mIpConnectivityMetrics == null) {
            Slog.wtf(TAG, "Missing IIpConnectivityMetrics");
        }
        try {
            this.mIpConnectivityMetrics.addNetdEventCallback(0, this.mNetdEventCallback);
        } catch (Exception e) {
            loge("Error registering netd callback: " + e);
        }
    }

    private void enforceCrossUserPermission(int i) {
        if (i == UserHandle.getCallingUserId()) {
            return;
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "ConnectivityService");
    }

    private void enforceInternetPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.INTERNET", "ConnectivityService");
    }

    private void enforceAccessPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_NETWORK_STATE", "ConnectivityService");
    }

    private void enforceChangePermission() {
        ConnectivityManager.enforceChangePermission(this.mContext);
    }

    private void enforceSettingsPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.NETWORK_SETTINGS", "ConnectivityService");
    }

    private boolean checkSettingsPermission() {
        return this.mContext.checkCallingOrSelfPermission("android.permission.NETWORK_SETTINGS") == 0;
    }

    private boolean checkSettingsPermission(int i, int i2) {
        return this.mContext.checkPermission("android.permission.NETWORK_SETTINGS", i, i2) == 0;
    }

    private void enforceTetherAccessPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_NETWORK_STATE", "ConnectivityService");
    }

    private void enforceConnectivityInternalPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", "ConnectivityService");
    }

    private void enforceConnectivityRestrictedNetworksPermission() {
        try {
            this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_USE_RESTRICTED_NETWORKS", "ConnectivityService");
        } catch (SecurityException e) {
            enforceConnectivityInternalPermission();
        }
    }

    private void enforceKeepalivePermission() {
        this.mContext.enforceCallingOrSelfPermission(KeepaliveTracker.PERMISSION, "ConnectivityService");
    }

    public void sendConnectedBroadcast(NetworkInfo networkInfo) {
        enforceConnectivityInternalPermission();
        sendGeneralBroadcast(networkInfo, "android.net.conn.CONNECTIVITY_CHANGE");
    }

    private void sendInetConditionBroadcast(NetworkInfo networkInfo) {
        sendGeneralBroadcast(networkInfo, "android.net.conn.INET_CONDITION_ACTION");
    }

    private Intent makeGeneralIntent(NetworkInfo networkInfo, String str) {
        synchronized (this.mVpns) {
            if (this.mLockdownTracker != null) {
                NetworkInfo networkInfo2 = new NetworkInfo(networkInfo);
                this.mLockdownTracker.augmentNetworkInfo(networkInfo2);
                networkInfo = networkInfo2;
            }
        }
        Intent intent = new Intent(str);
        intent.putExtra("networkInfo", new NetworkInfo(networkInfo));
        intent.putExtra("networkType", networkInfo.getType());
        if (networkInfo.isFailover()) {
            intent.putExtra("isFailover", true);
            networkInfo.setFailover(false);
        }
        if (networkInfo.getReason() != null) {
            intent.putExtra(PhoneWindowManager.SYSTEM_DIALOG_REASON_KEY, networkInfo.getReason());
        }
        if (networkInfo.getExtraInfo() != null) {
            intent.putExtra("extraInfo", networkInfo.getExtraInfo());
        }
        intent.putExtra("inetCondition", this.mDefaultInetConditionPublished);
        return intent;
    }

    private void sendGeneralBroadcast(NetworkInfo networkInfo, String str) {
        sendStickyBroadcast(makeGeneralIntent(networkInfo, str));
    }

    private void sendDataActivityBroadcast(int i, boolean z, long j) {
        Intent intent = new Intent("android.net.conn.DATA_ACTIVITY_CHANGE");
        intent.putExtra("deviceType", i);
        intent.putExtra("isActive", z);
        intent.putExtra("tsNanos", j);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mContext.sendOrderedBroadcastAsUser(intent, UserHandle.ALL, "android.permission.RECEIVE_DATA_ACTIVITY_CHANGE", null, null, 0, null, null);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void sendStickyBroadcast(Intent intent) {
        synchronized (this) {
            if (!this.mSystemReady) {
                this.mInitialBroadcast = new Intent(intent);
            }
            intent.addFlags(67108864);
            Bundle bundle = null;
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            if ("android.net.conn.CONNECTIVITY_CHANGE".equals(intent.getAction())) {
                NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                if (networkInfo.getType() == 3) {
                    intent.setAction("android.net.conn.CONNECTIVITY_CHANGE_SUPL");
                    intent.addFlags(1073741824);
                } else {
                    BroadcastOptions broadcastOptionsMakeBasic = BroadcastOptions.makeBasic();
                    broadcastOptionsMakeBasic.setMaxManifestReceiverApiLevel(23);
                    bundle = broadcastOptionsMakeBasic.toBundle();
                }
                try {
                    BatteryStatsService.getService().noteConnectivityChanged(intent.getIntExtra("networkType", -1), networkInfo != null ? networkInfo.getState().toString() : "?");
                } catch (RemoteException e) {
                }
                intent.addFlags(DumpState.DUMP_COMPILER_STATS);
            }
            try {
                this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL, bundle);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    protected void systemReady() {
        loadGlobalProxy();
        registerNetdEventCallback();
        synchronized (this) {
            this.mSystemReady = true;
            if (this.mInitialBroadcast != null) {
                this.mContext.sendStickyBroadcastAsUser(this.mInitialBroadcast, UserHandle.ALL);
                this.mInitialBroadcast = null;
            }
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(9));
        updateLockdownVpn();
        this.mHandler.sendMessage(this.mHandler.obtainMessage(30));
        this.mHandler.sendMessage(this.mHandler.obtainMessage(25));
        this.mPermissionMonitor.startMonitoring();
    }

    private void setupDataActivityTracking(NetworkAgentInfo networkAgentInfo) {
        String interfaceName = networkAgentInfo.linkProperties.getInterfaceName();
        int i = 0;
        int i2 = 1;
        if (!networkAgentInfo.networkCapabilities.hasTransport(0)) {
            if (networkAgentInfo.networkCapabilities.hasTransport(1)) {
                i = Settings.Global.getInt(this.mContext.getContentResolver(), "data_activity_timeout_wifi", 15);
            } else {
                i2 = -1;
            }
        } else {
            i2 = 0;
            i = Settings.Global.getInt(this.mContext.getContentResolver(), "data_activity_timeout_mobile", 10);
        }
        if (i > 0 && interfaceName != null && i2 != -1) {
            try {
                this.mNetd.addIdleTimer(interfaceName, i, i2);
            } catch (Exception e) {
                loge("Exception in setupDataActivityTracking " + e);
            }
        }
    }

    private void removeDataActivityTracking(NetworkAgentInfo networkAgentInfo) {
        String interfaceName = networkAgentInfo.linkProperties.getInterfaceName();
        NetworkCapabilities networkCapabilities = networkAgentInfo.networkCapabilities;
        if (interfaceName != null) {
            if (networkCapabilities.hasTransport(0) || networkCapabilities.hasTransport(1)) {
                try {
                    this.mNetd.removeIdleTimer(interfaceName);
                } catch (Exception e) {
                    loge("Exception in removeDataActivityTracking " + e);
                }
            }
        }
    }

    private void updateMtu(LinkProperties linkProperties, LinkProperties linkProperties2) {
        String interfaceName = linkProperties.getInterfaceName();
        int mtu = linkProperties.getMtu();
        if (linkProperties2 == null && mtu == 0) {
            return;
        }
        if (linkProperties2 != null && linkProperties.isIdenticalMtu(linkProperties2)) {
            return;
        }
        if (!LinkProperties.isValidMtu(mtu, linkProperties.hasGlobalIPv6Address())) {
            if (mtu != 0) {
                loge("Unexpected mtu value: " + mtu + ", " + interfaceName);
                return;
            }
            return;
        }
        if (TextUtils.isEmpty(interfaceName)) {
            loge("Setting MTU size with null iface.");
            return;
        }
        try {
            this.mNetd.setMtu(interfaceName, mtu);
        } catch (Exception e) {
            Slog.e(TAG, "exception in setMtu()" + e);
        }
    }

    @VisibleForTesting
    protected MockableSystemProperties getSystemProperties() {
        return new MockableSystemProperties();
    }

    private void updateTcpBufferSizes(NetworkAgentInfo networkAgentInfo) {
        if (!isDefaultNetwork(networkAgentInfo)) {
            return;
        }
        String tcpBufferSizes = networkAgentInfo.linkProperties.getTcpBufferSizes();
        String[] strArrSplit = null;
        if (tcpBufferSizes != null) {
            strArrSplit = tcpBufferSizes.split(",");
        }
        if (strArrSplit == null || strArrSplit.length != 6) {
            log("Invalid tcpBufferSizes string: " + tcpBufferSizes + ", using defaults");
            tcpBufferSizes = DEFAULT_TCP_BUFFER_SIZES;
            strArrSplit = DEFAULT_TCP_BUFFER_SIZES.split(",");
        }
        if (tcpBufferSizes.equals(this.mCurrentTcpBufferSizes)) {
            return;
        }
        try {
            FileUtils.stringToFile("/sys/kernel/ipv4/tcp_rmem_min", strArrSplit[0]);
            FileUtils.stringToFile("/sys/kernel/ipv4/tcp_rmem_def", strArrSplit[1]);
            FileUtils.stringToFile("/sys/kernel/ipv4/tcp_rmem_max", strArrSplit[2]);
            FileUtils.stringToFile("/sys/kernel/ipv4/tcp_wmem_min", strArrSplit[3]);
            FileUtils.stringToFile("/sys/kernel/ipv4/tcp_wmem_def", strArrSplit[4]);
            FileUtils.stringToFile("/sys/kernel/ipv4/tcp_wmem_max", strArrSplit[5]);
            this.mCurrentTcpBufferSizes = tcpBufferSizes;
        } catch (IOException e) {
            loge("Can't set TCP buffer sizes:" + e);
        }
        Integer numValueOf = Integer.valueOf(Settings.Global.getInt(this.mContext.getContentResolver(), "tcp_default_init_rwnd", this.mSystemProperties.getInt(DEFAULT_TCP_RWND_KEY, 0)));
        if (numValueOf.intValue() != 0) {
            this.mSystemProperties.set("sys.sysctl.tcp_def_init_rwnd", numValueOf.toString());
        }
    }

    public int getRestoreDefaultNetworkDelay(int i) {
        String str = this.mSystemProperties.get(NETWORK_RESTORE_DELAY_PROP_NAME);
        if (str != null && str.length() != 0) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException e) {
            }
        }
        if (i > 29 || this.mNetConfigs[i] == null) {
            return RESTORE_DEFAULT_NETWORK_DELAY;
        }
        return this.mNetConfigs[i].restoreTime;
    }

    private void dumpNetworkDiagnostics(IndentingPrintWriter indentingPrintWriter) {
        ArrayList<NetworkDiagnostics> arrayList = new ArrayList();
        for (NetworkAgentInfo networkAgentInfo : this.mNetworkAgentInfos.values()) {
            arrayList.add(new NetworkDiagnostics(networkAgentInfo.network, new LinkProperties(networkAgentInfo.linkProperties), 5000L));
        }
        for (NetworkDiagnostics networkDiagnostics : arrayList) {
            indentingPrintWriter.println();
            networkDiagnostics.waitForMeasurements();
            networkDiagnostics.dump(indentingPrintWriter);
        }
    }

    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        PriorityDump.dump(this.mPriorityDumper, fileDescriptor, printWriter, strArr);
    }

    private void doDump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr, boolean z) {
        PrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "  ");
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, indentingPrintWriter) && !z) {
            if (ArrayUtils.contains(strArr, DIAG_ARG)) {
                dumpNetworkDiagnostics(indentingPrintWriter);
                return;
            }
            if (ArrayUtils.contains(strArr, TETHERING_ARG)) {
                this.mTethering.dump(fileDescriptor, indentingPrintWriter, strArr);
                return;
            }
            indentingPrintWriter.print("NetworkFactories for:");
            Iterator<NetworkFactoryInfo> it = this.mNetworkFactoryInfos.values().iterator();
            while (it.hasNext()) {
                indentingPrintWriter.print(" " + it.next().name);
            }
            indentingPrintWriter.println();
            indentingPrintWriter.println();
            NetworkAgentInfo defaultNetwork = getDefaultNetwork();
            indentingPrintWriter.print("Active default network: ");
            if (defaultNetwork == null) {
                indentingPrintWriter.println("none");
            } else {
                indentingPrintWriter.println(defaultNetwork.network.netId);
            }
            indentingPrintWriter.println();
            indentingPrintWriter.println("Current Networks:");
            indentingPrintWriter.increaseIndent();
            for (NetworkAgentInfo networkAgentInfo : this.mNetworkAgentInfos.values()) {
                indentingPrintWriter.println(networkAgentInfo.toString());
                indentingPrintWriter.increaseIndent();
                indentingPrintWriter.println(String.format("Requests: REQUEST:%d LISTEN:%d BACKGROUND_REQUEST:%d total:%d", Integer.valueOf(networkAgentInfo.numForegroundNetworkRequests()), Integer.valueOf(networkAgentInfo.numNetworkRequests() - networkAgentInfo.numRequestNetworkRequests()), Integer.valueOf(networkAgentInfo.numBackgroundNetworkRequests()), Integer.valueOf(networkAgentInfo.numNetworkRequests())));
                indentingPrintWriter.increaseIndent();
                for (int i = 0; i < networkAgentInfo.numNetworkRequests(); i++) {
                    indentingPrintWriter.println(networkAgentInfo.requestAt(i).toString());
                }
                indentingPrintWriter.decreaseIndent();
                indentingPrintWriter.println("Lingered:");
                indentingPrintWriter.increaseIndent();
                networkAgentInfo.dumpLingerTimers(indentingPrintWriter);
                indentingPrintWriter.decreaseIndent();
                indentingPrintWriter.decreaseIndent();
            }
            indentingPrintWriter.decreaseIndent();
            indentingPrintWriter.println();
            indentingPrintWriter.println("Network Requests:");
            indentingPrintWriter.increaseIndent();
            Iterator<NetworkRequestInfo> it2 = this.mNetworkRequests.values().iterator();
            while (it2.hasNext()) {
                indentingPrintWriter.println(it2.next().toString());
            }
            indentingPrintWriter.println();
            indentingPrintWriter.decreaseIndent();
            this.mLegacyTypeTracker.dump(indentingPrintWriter);
            indentingPrintWriter.println();
            this.mTethering.dump(fileDescriptor, indentingPrintWriter, strArr);
            indentingPrintWriter.println();
            this.mKeepaliveTracker.dump(indentingPrintWriter);
            indentingPrintWriter.println();
            dumpAvoidBadWifiSettings(indentingPrintWriter);
            indentingPrintWriter.println();
            this.mMultipathPolicyTracker.dump(indentingPrintWriter);
            if (!ArrayUtils.contains(strArr, SHORT_ARG)) {
                indentingPrintWriter.println();
                synchronized (this.mValidationLogs) {
                    indentingPrintWriter.println("mValidationLogs (most recent first):");
                    for (ValidationLog validationLog : this.mValidationLogs) {
                        indentingPrintWriter.println(validationLog.mNetwork + " - " + validationLog.mName);
                        indentingPrintWriter.increaseIndent();
                        validationLog.mLog.dump(fileDescriptor, indentingPrintWriter, strArr);
                        indentingPrintWriter.decreaseIndent();
                    }
                }
                indentingPrintWriter.println();
                indentingPrintWriter.println("mNetworkRequestInfoLogs (most recent first):");
                indentingPrintWriter.increaseIndent();
                this.mNetworkRequestInfoLogs.reverseDump(fileDescriptor, indentingPrintWriter, strArr);
                indentingPrintWriter.decreaseIndent();
                indentingPrintWriter.println();
                indentingPrintWriter.println("mNetworkInfoBlockingLogs (most recent first):");
                indentingPrintWriter.increaseIndent();
                this.mNetworkInfoBlockingLogs.reverseDump(fileDescriptor, indentingPrintWriter, strArr);
                indentingPrintWriter.decreaseIndent();
                indentingPrintWriter.println();
                indentingPrintWriter.println("NetTransition WakeLock activity (most recent first):");
                indentingPrintWriter.increaseIndent();
                indentingPrintWriter.println("total acquisitions: " + this.mTotalWakelockAcquisitions);
                indentingPrintWriter.println("total releases: " + this.mTotalWakelockReleases);
                indentingPrintWriter.println("cumulative duration: " + (this.mTotalWakelockDurationMs / 1000) + "s");
                indentingPrintWriter.println("longest duration: " + (this.mMaxWakelockDurationMs / 1000) + "s");
                if (this.mTotalWakelockAcquisitions > this.mTotalWakelockReleases) {
                    indentingPrintWriter.println("currently holding WakeLock for: " + ((SystemClock.elapsedRealtime() - this.mLastWakeLockAcquireTimestamp) / 1000) + "s");
                }
                this.mWakelockLogs.reverseDump(fileDescriptor, indentingPrintWriter, strArr);
                indentingPrintWriter.decreaseIndent();
            }
        }
    }

    private boolean isLiveNetworkAgent(NetworkAgentInfo networkAgentInfo, int i) {
        if (networkAgentInfo.network == null) {
            return false;
        }
        NetworkAgentInfo networkAgentInfoForNetwork = getNetworkAgentInfoForNetwork(networkAgentInfo.network);
        if (networkAgentInfoForNetwork != null && networkAgentInfoForNetwork.equals(networkAgentInfo)) {
            return true;
        }
        if (networkAgentInfoForNetwork != null) {
            loge(eventName(i) + " - isLiveNetworkAgent found mismatched netId: " + networkAgentInfoForNetwork + " - " + networkAgentInfo);
        }
        return false;
    }

    private class NetworkStateTrackerHandler extends Handler {
        public NetworkStateTrackerHandler(Looper looper) {
            super(looper);
        }

        private boolean maybeHandleAsyncChannelMessage(Message message) {
            int i = message.what;
            if (i != 69632) {
                switch (i) {
                    case 69635:
                        NetworkAgentInfo networkAgentInfo = (NetworkAgentInfo) ConnectivityService.this.mNetworkAgentInfos.get(message.replyTo);
                        if (networkAgentInfo != null) {
                            networkAgentInfo.asyncChannel.disconnect();
                            return true;
                        }
                        return true;
                    case 69636:
                        ConnectivityService.this.handleAsyncChannelDisconnected(message);
                        return true;
                    default:
                        return false;
                }
            }
            ConnectivityService.this.handleAsyncChannelHalfConnect(message);
            return true;
        }

        private void maybeHandleNetworkAgentMessage(Message message) {
            NetworkAgentInfo networkAgentInfo = (NetworkAgentInfo) ConnectivityService.this.mNetworkAgentInfos.get(message.replyTo);
            if (networkAgentInfo == null) {
            }
            int i = message.what;
            if (i == 528392) {
                if (networkAgentInfo.everConnected && !networkAgentInfo.networkMisc.explicitlySelected) {
                    ConnectivityService.loge("ERROR: already-connected network explicitly selected.");
                }
                networkAgentInfo.networkMisc.explicitlySelected = true;
                networkAgentInfo.networkMisc.acceptUnvalidated = ((Boolean) message.obj).booleanValue();
                return;
            }
            if (i == 528397) {
                ConnectivityService.this.mKeepaliveTracker.handleEventPacketKeepalive(networkAgentInfo, message);
                return;
            }
            switch (i) {
                case 528385:
                    ConnectivityService.this.updateNetworkInfo(networkAgentInfo, (NetworkInfo) message.obj);
                    break;
                case 528386:
                    NetworkCapabilities networkCapabilities = (NetworkCapabilities) message.obj;
                    if (networkCapabilities.hasCapability(17) || networkCapabilities.hasCapability(16) || networkCapabilities.hasCapability(19)) {
                        Slog.wtf(ConnectivityService.TAG, "BUG: " + networkAgentInfo + " has CS-managed capability.");
                    }
                    ConnectivityService.this.updateCapabilities(networkAgentInfo.getCurrentScore(), networkAgentInfo, networkCapabilities);
                    break;
                case 528387:
                    ConnectivityService.this.handleUpdateLinkProperties(networkAgentInfo, (LinkProperties) message.obj);
                    break;
                case 528388:
                    Integer num = (Integer) message.obj;
                    if (num != null) {
                        ConnectivityService.this.updateNetworkScore(networkAgentInfo, num.intValue());
                    }
                    break;
            }
        }

        private boolean maybeHandleNetworkMonitorMessage(Message message) {
            String str;
            int i = message.what;
            if (i == 532482) {
                NetworkAgentInfo networkAgentInfoForNetId = ConnectivityService.this.getNetworkAgentInfoForNetId(message.arg2);
                if (networkAgentInfoForNetId != null) {
                    boolean z = message.arg1 == 0;
                    boolean z2 = networkAgentInfoForNetId.lastValidated;
                    boolean zIsDefaultNetwork = ConnectivityService.this.isDefaultNetwork(networkAgentInfoForNetId);
                    String str2 = message.obj instanceof String ? (String) message.obj : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
                    if (!TextUtils.isEmpty(str2)) {
                        str = " with redirect to " + str2;
                    } else {
                        str = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append(networkAgentInfoForNetId.name());
                    sb.append(" validation ");
                    sb.append(z ? "passed" : "failed");
                    sb.append(str);
                    ConnectivityService.log(sb.toString());
                    if (z != networkAgentInfoForNetId.lastValidated) {
                        if (zIsDefaultNetwork) {
                            ConnectivityService.this.metricsLogger().defaultNetworkMetrics().logDefaultNetworkValidity(SystemClock.elapsedRealtime(), z);
                        }
                        int currentScore = networkAgentInfoForNetId.getCurrentScore();
                        networkAgentInfoForNetId.lastValidated = z;
                        networkAgentInfoForNetId.everValidated |= z;
                        ConnectivityService.this.updateCapabilities(currentScore, networkAgentInfoForNetId, networkAgentInfoForNetId.networkCapabilities);
                        if (currentScore != networkAgentInfoForNetId.getCurrentScore()) {
                            ConnectivityService.this.sendUpdatedScoreToFactories(networkAgentInfoForNetId);
                        }
                    }
                    ConnectivityService.this.updateInetCondition(networkAgentInfoForNetId);
                    Bundle bundle = new Bundle();
                    bundle.putString(NetworkAgent.REDIRECT_URL_KEY, str2);
                    networkAgentInfoForNetId.asyncChannel.sendMessage(528391, z ? 1 : 2, 0, bundle);
                    if (z2 && !networkAgentInfoForNetId.lastValidated) {
                        ConnectivityService.this.handleNetworkUnvalidated(networkAgentInfoForNetId);
                    }
                }
            } else if (i == 532490) {
                int i2 = message.arg2;
                boolean bool = ConnectivityService.toBool(message.arg1);
                NetworkAgentInfo networkAgentInfoForNetId2 = ConnectivityService.this.getNetworkAgentInfoForNetId(i2);
                if (networkAgentInfoForNetId2 != null && bool != networkAgentInfoForNetId2.lastCaptivePortalDetected) {
                    int currentScore2 = networkAgentInfoForNetId2.getCurrentScore();
                    networkAgentInfoForNetId2.lastCaptivePortalDetected = bool;
                    networkAgentInfoForNetId2.everCaptivePortalDetected |= bool;
                    if (!networkAgentInfoForNetId2.lastCaptivePortalDetected || 2 != getCaptivePortalMode()) {
                        ConnectivityService.this.updateCapabilities(currentScore2, networkAgentInfoForNetId2, networkAgentInfoForNetId2.networkCapabilities);
                        if (bool) {
                        }
                    } else {
                        ConnectivityService.log("Avoiding captive portal network: " + networkAgentInfoForNetId2.name());
                        networkAgentInfoForNetId2.asyncChannel.sendMessage(528399);
                        ConnectivityService.this.teardownUnneededNetwork(networkAgentInfoForNetId2);
                    }
                } else if (bool) {
                    ConnectivityService.this.mNotifier.clearNotification(i2);
                } else if (networkAgentInfoForNetId2 == null) {
                    ConnectivityService.loge("EVENT_PROVISIONING_NOTIFICATION from unknown NetworkMonitor");
                } else if (!networkAgentInfoForNetId2.networkMisc.provisioningNotificationDisabled) {
                    ConnectivityService.this.mNotifier.showNotification(i2, NetworkNotificationManager.NotificationType.SIGN_IN, networkAgentInfoForNetId2, null, (PendingIntent) message.obj, networkAgentInfoForNetId2.networkMisc.explicitlySelected);
                }
            } else {
                if (i != 532494) {
                    return false;
                }
                NetworkAgentInfo networkAgentInfoForNetId3 = ConnectivityService.this.getNetworkAgentInfoForNetId(message.arg2);
                if (networkAgentInfoForNetId3 != null) {
                    ConnectivityService.this.updatePrivateDns(networkAgentInfoForNetId3, (DnsManager.PrivateDnsConfig) message.obj);
                }
            }
            return true;
        }

        private int getCaptivePortalMode() {
            return Settings.Global.getInt(ConnectivityService.this.mContext.getContentResolver(), "captive_portal_mode", 1);
        }

        private boolean maybeHandleNetworkAgentInfoMessage(Message message) {
            if (message.what != 1001) {
                return false;
            }
            NetworkAgentInfo networkAgentInfo = (NetworkAgentInfo) message.obj;
            if (networkAgentInfo != null && ConnectivityService.this.isLiveNetworkAgent(networkAgentInfo, message.what)) {
                ConnectivityService.this.handleLingerComplete(networkAgentInfo);
                return true;
            }
            return true;
        }

        @Override
        public void handleMessage(Message message) {
            if (!maybeHandleAsyncChannelMessage(message) && !maybeHandleNetworkMonitorMessage(message) && !maybeHandleNetworkAgentInfoMessage(message)) {
                maybeHandleNetworkAgentMessage(message);
            }
        }
    }

    private boolean networkRequiresPrivateDnsValidation(NetworkAgentInfo networkAgentInfo) {
        return networkAgentInfo.networkMonitor.isPrivateDnsValidationRequired();
    }

    private void handlePrivateDnsSettingsChanged() {
        DnsManager.PrivateDnsConfig privateDnsConfig = this.mDnsManager.getPrivateDnsConfig();
        for (NetworkAgentInfo networkAgentInfo : this.mNetworkAgentInfos.values()) {
            handlePerNetworkPrivateDnsConfig(networkAgentInfo, privateDnsConfig);
            if (networkRequiresPrivateDnsValidation(networkAgentInfo)) {
                handleUpdateLinkProperties(networkAgentInfo, new LinkProperties(networkAgentInfo.linkProperties));
            }
        }
    }

    private void handlePerNetworkPrivateDnsConfig(NetworkAgentInfo networkAgentInfo, DnsManager.PrivateDnsConfig privateDnsConfig) {
        if (networkRequiresPrivateDnsValidation(networkAgentInfo)) {
            networkAgentInfo.networkMonitor.notifyPrivateDnsSettingsChanged(privateDnsConfig);
            updatePrivateDns(networkAgentInfo, privateDnsConfig);
        }
    }

    private void updatePrivateDns(NetworkAgentInfo networkAgentInfo, DnsManager.PrivateDnsConfig privateDnsConfig) {
        this.mDnsManager.updatePrivateDns(networkAgentInfo.network, privateDnsConfig);
        updateDnses(networkAgentInfo.linkProperties, null, networkAgentInfo.network.netId);
    }

    private void handlePrivateDnsValidationUpdate(DnsManager.PrivateDnsValidationUpdate privateDnsValidationUpdate) {
        NetworkAgentInfo networkAgentInfoForNetId = getNetworkAgentInfoForNetId(privateDnsValidationUpdate.netId);
        if (networkAgentInfoForNetId == null) {
            return;
        }
        this.mDnsManager.updatePrivateDnsValidation(privateDnsValidationUpdate);
        handleUpdateLinkProperties(networkAgentInfoForNetId, new LinkProperties(networkAgentInfoForNetId.linkProperties));
    }

    private void updateLingerState(NetworkAgentInfo networkAgentInfo, long j) {
        networkAgentInfo.updateLingerTimer();
        if (networkAgentInfo.isLingering() && networkAgentInfo.numForegroundNetworkRequests() > 0) {
            log("Unlingering " + networkAgentInfo.name());
            networkAgentInfo.unlinger();
            logNetworkEvent(networkAgentInfo, 6);
            return;
        }
        if (unneeded(networkAgentInfo, UnneededFor.LINGER) && networkAgentInfo.getLingerExpiry() > 0) {
            int lingerExpiry = (int) (networkAgentInfo.getLingerExpiry() - j);
            log("Lingering " + networkAgentInfo.name() + " for " + lingerExpiry + "ms");
            networkAgentInfo.linger();
            logNetworkEvent(networkAgentInfo, 5);
            notifyNetworkCallbacks(networkAgentInfo, 524291, lingerExpiry);
        }
    }

    private void handleAsyncChannelHalfConnect(Message message) {
        AsyncChannel asyncChannel = (AsyncChannel) message.obj;
        if (this.mNetworkFactoryInfos.containsKey(message.replyTo)) {
            if (message.arg1 == 0) {
                for (NetworkRequestInfo networkRequestInfo : this.mNetworkRequests.values()) {
                    if (!networkRequestInfo.request.isListen()) {
                        NetworkAgentInfo networkForRequest = getNetworkForRequest(networkRequestInfo.request.requestId);
                        asyncChannel.sendMessage(536576, networkForRequest != null ? networkForRequest.getCurrentScore() : 0, 0, networkRequestInfo.request);
                    }
                }
                return;
            }
            loge("Error connecting NetworkFactory");
            this.mNetworkFactoryInfos.remove(message.obj);
            return;
        }
        if (this.mNetworkAgentInfos.containsKey(message.replyTo)) {
            if (message.arg1 == 0) {
                this.mNetworkAgentInfos.get(message.replyTo).asyncChannel.sendMessage(69633);
                return;
            }
            loge("Error connecting NetworkAgent");
            NetworkAgentInfo networkAgentInfoRemove = this.mNetworkAgentInfos.remove(message.replyTo);
            if (networkAgentInfoRemove != null) {
                boolean zIsDefaultNetwork = isDefaultNetwork(networkAgentInfoRemove);
                synchronized (this.mNetworkForNetId) {
                    this.mNetworkForNetId.remove(networkAgentInfoRemove.network.netId);
                    this.mNetIdInUse.delete(networkAgentInfoRemove.network.netId);
                }
                this.mLegacyTypeTracker.remove(networkAgentInfoRemove, zIsDefaultNetwork);
            }
        }
    }

    private void handleAsyncChannelDisconnected(Message message) {
        NetworkAgentInfo networkAgentInfo = this.mNetworkAgentInfos.get(message.replyTo);
        if (networkAgentInfo != null) {
            disconnectAndDestroyNetwork(networkAgentInfo);
            return;
        }
        NetworkFactoryInfo networkFactoryInfoRemove = this.mNetworkFactoryInfos.remove(message.replyTo);
        if (networkFactoryInfoRemove != null) {
            log("unregisterNetworkFactory for " + networkFactoryInfoRemove.name);
        }
    }

    private void disconnectAndDestroyNetwork(NetworkAgentInfo networkAgentInfo) {
        log(networkAgentInfo.name() + " got DISCONNECTED, was satisfying " + networkAgentInfo.numNetworkRequests());
        if (networkAgentInfo.networkInfo.isConnected()) {
            networkAgentInfo.networkInfo.setDetailedState(NetworkInfo.DetailedState.DISCONNECTED, null, null);
        }
        boolean zIsDefaultNetwork = isDefaultNetwork(networkAgentInfo);
        if (zIsDefaultNetwork) {
            this.mDefaultInetConditionPublished = 0;
            metricsLogger().defaultNetworkMetrics().logDefaultNetworkEvent(SystemClock.elapsedRealtime(), null, networkAgentInfo);
        }
        notifyIfacesChangedForNetworkStats();
        notifyNetworkCallbacks(networkAgentInfo, 524292);
        this.mKeepaliveTracker.handleStopAllKeepalives(networkAgentInfo, -20);
        Iterator it = networkAgentInfo.linkProperties.getAllInterfaceNames().iterator();
        while (it.hasNext()) {
            wakeupModifyInterface((String) it.next(), networkAgentInfo.networkCapabilities, false);
        }
        networkAgentInfo.networkMonitor.sendMessage(NetworkMonitor.CMD_NETWORK_DISCONNECTED);
        this.mNetworkAgentInfos.remove(networkAgentInfo.messenger);
        networkAgentInfo.maybeStopClat();
        synchronized (this.mNetworkForNetId) {
            this.mNetworkForNetId.remove(networkAgentInfo.network.netId);
        }
        for (int i = 0; i < networkAgentInfo.numNetworkRequests(); i++) {
            NetworkRequest networkRequestRequestAt = networkAgentInfo.requestAt(i);
            NetworkAgentInfo networkForRequest = getNetworkForRequest(networkRequestRequestAt.requestId);
            if (networkForRequest != null && networkForRequest.network.netId == networkAgentInfo.network.netId) {
                clearNetworkForRequest(networkRequestRequestAt.requestId);
                sendUpdatedScoreToFactories(networkRequestRequestAt, 0);
            }
        }
        networkAgentInfo.clearLingerState();
        if (networkAgentInfo.isSatisfyingRequest(this.mDefaultRequest.requestId)) {
            removeDataActivityTracking(networkAgentInfo);
            notifyLockdownVpn(networkAgentInfo);
            ensureNetworkTransitionWakelock(networkAgentInfo.name());
        }
        this.mLegacyTypeTracker.remove(networkAgentInfo, zIsDefaultNetwork);
        if (!networkAgentInfo.networkCapabilities.hasTransport(4)) {
            updateAllVpnsCapabilities();
        }
        rematchAllNetworksAndRequests(null, 0);
        this.mLingerMonitor.noteDisconnect(networkAgentInfo);
        if (networkAgentInfo.created) {
            try {
                this.mNetd.removeNetwork(networkAgentInfo.network.netId);
            } catch (Exception e) {
                loge("Exception removing network: " + e);
            }
            this.mDnsManager.removeNetwork(networkAgentInfo.network);
        }
        synchronized (this.mNetworkForNetId) {
            this.mNetIdInUse.delete(networkAgentInfo.network.netId);
        }
    }

    private NetworkRequestInfo findExistingNetworkRequestInfo(PendingIntent pendingIntent) {
        Intent intent = pendingIntent.getIntent();
        for (Map.Entry<NetworkRequest, NetworkRequestInfo> entry : this.mNetworkRequests.entrySet()) {
            PendingIntent pendingIntent2 = entry.getValue().mPendingIntent;
            if (pendingIntent2 != null && pendingIntent2.getIntent().filterEquals(intent)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private void handleRegisterNetworkRequestWithIntent(Message message) {
        NetworkRequestInfo networkRequestInfo = (NetworkRequestInfo) message.obj;
        NetworkRequestInfo networkRequestInfoFindExistingNetworkRequestInfo = findExistingNetworkRequestInfo(networkRequestInfo.mPendingIntent);
        if (networkRequestInfoFindExistingNetworkRequestInfo != null) {
            log("Replacing " + networkRequestInfoFindExistingNetworkRequestInfo.request + " with " + networkRequestInfo.request + " because their intents matched.");
            handleReleaseNetworkRequest(networkRequestInfoFindExistingNetworkRequestInfo.request, getCallingUid());
        }
        handleRegisterNetworkRequest(networkRequestInfo);
    }

    private void handleRegisterNetworkRequest(NetworkRequestInfo networkRequestInfo) {
        this.mNetworkRequests.put(networkRequestInfo.request, networkRequestInfo);
        this.mNetworkRequestInfoLogs.log("REGISTER " + networkRequestInfo);
        if (networkRequestInfo.request.isListen()) {
            for (NetworkAgentInfo networkAgentInfo : this.mNetworkAgentInfos.values()) {
                if (networkRequestInfo.request.networkCapabilities.hasSignalStrength() && networkAgentInfo.satisfiesImmutableCapabilitiesOf(networkRequestInfo.request)) {
                    updateSignalStrengthThresholds(networkAgentInfo, "REGISTER", networkRequestInfo.request);
                }
            }
        }
        rematchAllNetworksAndRequests(null, 0);
        if (networkRequestInfo.request.isRequest() && getNetworkForRequest(networkRequestInfo.request.requestId) == null) {
            sendUpdatedScoreToFactories(networkRequestInfo.request, 0);
        }
    }

    private void handleReleaseNetworkRequestWithIntent(PendingIntent pendingIntent, int i) {
        NetworkRequestInfo networkRequestInfoFindExistingNetworkRequestInfo = findExistingNetworkRequestInfo(pendingIntent);
        if (networkRequestInfoFindExistingNetworkRequestInfo != null) {
            handleReleaseNetworkRequest(networkRequestInfoFindExistingNetworkRequestInfo.request, i);
        }
    }

    private boolean unneeded(NetworkAgentInfo networkAgentInfo, UnneededFor unneededFor) {
        int iNumRequestNetworkRequests;
        switch (AnonymousClass8.$SwitchMap$com$android$server$ConnectivityService$UnneededFor[unneededFor.ordinal()]) {
            case 1:
                iNumRequestNetworkRequests = networkAgentInfo.numRequestNetworkRequests();
                break;
            case 2:
                iNumRequestNetworkRequests = networkAgentInfo.numForegroundNetworkRequests();
                break;
            default:
                Slog.wtf(TAG, "Invalid reason. Cannot happen.");
                return true;
        }
        if (!networkAgentInfo.everConnected || networkAgentInfo.isVPN() || networkAgentInfo.isLingering() || iNumRequestNetworkRequests > 0) {
            return false;
        }
        for (NetworkRequestInfo networkRequestInfo : this.mNetworkRequests.values()) {
            if (unneededFor != UnneededFor.LINGER || !networkRequestInfo.request.isBackgroundRequest()) {
                if (networkRequestInfo.request.isRequest() && networkAgentInfo.satisfies(networkRequestInfo.request) && (networkAgentInfo.isSatisfyingRequest(networkRequestInfo.request.requestId) || getNetworkForRequest(networkRequestInfo.request.requestId).getCurrentScore() < networkAgentInfo.getCurrentScoreAsValidated())) {
                    return false;
                }
            }
        }
        return true;
    }

    private NetworkRequestInfo getNriForAppRequest(NetworkRequest networkRequest, int i, String str) {
        NetworkRequestInfo networkRequestInfo = this.mNetworkRequests.get(networkRequest);
        if (networkRequestInfo != null && 1000 != i && networkRequestInfo.mUid != i) {
            log(String.format("UID %d attempted to %s for unowned request %s", Integer.valueOf(i), str, networkRequestInfo));
            return null;
        }
        return networkRequestInfo;
    }

    private void handleTimedOutNetworkRequest(NetworkRequestInfo networkRequestInfo) {
        if (this.mNetworkRequests.get(networkRequestInfo.request) == null || getNetworkForRequest(networkRequestInfo.request.requestId) != null) {
            return;
        }
        if (networkRequestInfo.request.isRequest()) {
            log("releasing " + networkRequestInfo.request + " (timeout)");
        }
        handleRemoveNetworkRequest(networkRequestInfo);
        callCallbackForRequest(networkRequestInfo, null, 524293, 0);
    }

    private void handleReleaseNetworkRequest(NetworkRequest networkRequest, int i) {
        NetworkRequestInfo nriForAppRequest = getNriForAppRequest(networkRequest, i, "release NetworkRequest");
        if (nriForAppRequest == null) {
            return;
        }
        if (nriForAppRequest.request.isRequest()) {
            log("releasing " + nriForAppRequest.request + " (release request)");
        }
        handleRemoveNetworkRequest(nriForAppRequest);
    }

    private void handleRemoveNetworkRequest(NetworkRequestInfo networkRequestInfo) {
        boolean z;
        boolean z2;
        networkRequestInfo.unlinkDeathRecipient();
        this.mNetworkRequests.remove(networkRequestInfo.request);
        synchronized (this.mUidToNetworkRequestCount) {
            int i = this.mUidToNetworkRequestCount.get(networkRequestInfo.mUid, 0);
            z = true;
            if (i < 1) {
                Slog.wtf(TAG, "BUG: too small request count " + i + " for UID " + networkRequestInfo.mUid);
            } else if (i != 1) {
                this.mUidToNetworkRequestCount.put(networkRequestInfo.mUid, i - 1);
            } else {
                this.mUidToNetworkRequestCount.removeAt(this.mUidToNetworkRequestCount.indexOfKey(networkRequestInfo.mUid));
            }
        }
        this.mNetworkRequestInfoLogs.log("RELEASE " + networkRequestInfo);
        if (networkRequestInfo.request.isRequest()) {
            NetworkAgentInfo networkForRequest = getNetworkForRequest(networkRequestInfo.request.requestId);
            if (networkForRequest != null) {
                boolean zIsBackgroundNetwork = networkForRequest.isBackgroundNetwork();
                networkForRequest.removeRequest(networkRequestInfo.request.requestId);
                updateLingerState(networkForRequest, SystemClock.elapsedRealtime());
                if (unneeded(networkForRequest, UnneededFor.TEARDOWN)) {
                    log("no live requests for " + networkForRequest.name() + "; disconnecting");
                    teardownUnneededNetwork(networkForRequest);
                    z2 = false;
                } else {
                    z2 = true;
                }
                clearNetworkForRequest(networkRequestInfo.request.requestId);
                if (!zIsBackgroundNetwork && networkForRequest.isBackgroundNetwork()) {
                    updateCapabilities(networkForRequest.getCurrentScore(), networkForRequest, networkForRequest.networkCapabilities);
                }
            } else {
                z2 = false;
            }
            for (NetworkAgentInfo networkAgentInfo : this.mNetworkAgentInfos.values()) {
                if (networkAgentInfo.isSatisfyingRequest(networkRequestInfo.request.requestId) && networkAgentInfo != networkForRequest) {
                    String str = TAG;
                    StringBuilder sb = new StringBuilder();
                    sb.append("Request ");
                    sb.append(networkRequestInfo.request);
                    sb.append(" satisfied by ");
                    sb.append(networkAgentInfo.name());
                    sb.append(", but mNetworkAgentInfos says ");
                    sb.append(networkForRequest != null ? networkForRequest.name() : "null");
                    Slog.wtf(str, sb.toString());
                }
            }
            if (networkRequestInfo.request.legacyType != -1 && networkForRequest != null) {
                if (z2) {
                    for (int i2 = 0; i2 < networkForRequest.numNetworkRequests(); i2++) {
                        NetworkRequest networkRequestRequestAt = networkForRequest.requestAt(i2);
                        if (networkRequestRequestAt.legacyType == networkRequestInfo.request.legacyType && networkRequestRequestAt.isRequest()) {
                            log(" still have other legacy request - leaving");
                            z = false;
                        }
                    }
                }
                if (z) {
                    this.mLegacyTypeTracker.remove(networkRequestInfo.request.legacyType, networkForRequest, false);
                }
            }
            Iterator<NetworkFactoryInfo> it = this.mNetworkFactoryInfos.values().iterator();
            while (it.hasNext()) {
                it.next().asyncChannel.sendMessage(536577, networkRequestInfo.request);
            }
            return;
        }
        for (NetworkAgentInfo networkAgentInfo2 : this.mNetworkAgentInfos.values()) {
            networkAgentInfo2.removeRequest(networkRequestInfo.request.requestId);
            if (networkRequestInfo.request.networkCapabilities.hasSignalStrength() && networkAgentInfo2.satisfiesImmutableCapabilitiesOf(networkRequestInfo.request)) {
                updateSignalStrengthThresholds(networkAgentInfo2, "RELEASE", networkRequestInfo.request);
            }
        }
    }

    public void setAcceptUnvalidated(Network network, boolean z, boolean z2) {
        enforceConnectivityInternalPermission();
        this.mHandler.sendMessage(this.mHandler.obtainMessage(28, encodeBool(z), encodeBool(z2), network));
    }

    public void setAvoidUnvalidated(Network network) {
        enforceConnectivityInternalPermission();
        this.mHandler.sendMessage(this.mHandler.obtainMessage(35, network));
    }

    private void handleSetAcceptUnvalidated(Network network, boolean z, boolean z2) {
        log("handleSetAcceptUnvalidated network=" + network + " accept=" + z + " always=" + z2);
        NetworkAgentInfo networkAgentInfoForNetwork = getNetworkAgentInfoForNetwork(network);
        if (networkAgentInfoForNetwork == null || networkAgentInfoForNetwork.everValidated) {
            return;
        }
        if (!networkAgentInfoForNetwork.networkMisc.explicitlySelected) {
            Slog.wtf(TAG, "BUG: setAcceptUnvalidated non non-explicitly selected network");
        }
        if (z != networkAgentInfoForNetwork.networkMisc.acceptUnvalidated) {
            int currentScore = networkAgentInfoForNetwork.getCurrentScore();
            networkAgentInfoForNetwork.networkMisc.acceptUnvalidated = z;
            rematchAllNetworksAndRequests(networkAgentInfoForNetwork, currentScore);
            sendUpdatedScoreToFactories(networkAgentInfoForNetwork);
        }
        if (z2) {
            networkAgentInfoForNetwork.asyncChannel.sendMessage(528393, encodeBool(z));
        }
        if (!z) {
            networkAgentInfoForNetwork.asyncChannel.sendMessage(528399);
            teardownUnneededNetwork(networkAgentInfoForNetwork);
        }
    }

    private void handleSetAvoidUnvalidated(Network network) {
        NetworkAgentInfo networkAgentInfoForNetwork = getNetworkAgentInfoForNetwork(network);
        if (networkAgentInfoForNetwork != null && !networkAgentInfoForNetwork.lastValidated && !networkAgentInfoForNetwork.avoidUnvalidated) {
            int currentScore = networkAgentInfoForNetwork.getCurrentScore();
            networkAgentInfoForNetwork.avoidUnvalidated = true;
            rematchAllNetworksAndRequests(networkAgentInfoForNetwork, currentScore);
            sendUpdatedScoreToFactories(networkAgentInfoForNetwork);
        }
    }

    private void scheduleUnvalidatedPrompt(NetworkAgentInfo networkAgentInfo) {
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(29, networkAgentInfo.network), 8000L);
    }

    public void startCaptivePortalApp(final Network network) {
        enforceConnectivityInternalPermission();
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                ConnectivityService.lambda$startCaptivePortalApp$1(this.f$0, network);
            }
        });
    }

    public static void lambda$startCaptivePortalApp$1(ConnectivityService connectivityService, Network network) {
        NetworkAgentInfo networkAgentInfoForNetwork = connectivityService.getNetworkAgentInfoForNetwork(network);
        if (networkAgentInfoForNetwork != null && networkAgentInfoForNetwork.networkCapabilities.hasCapability(17)) {
            networkAgentInfoForNetwork.networkMonitor.sendMessage(NetworkMonitor.CMD_LAUNCH_CAPTIVE_PORTAL_APP);
        }
    }

    public boolean avoidBadWifi() {
        return this.mMultinetworkPolicyTracker.getAvoidBadWifi();
    }

    private void rematchForAvoidBadWifiUpdate() {
        rematchAllNetworksAndRequests(null, 0);
        for (NetworkAgentInfo networkAgentInfo : this.mNetworkAgentInfos.values()) {
            if (networkAgentInfo.networkCapabilities.hasTransport(1)) {
                sendUpdatedScoreToFactories(networkAgentInfo);
            }
        }
    }

    private void dumpAvoidBadWifiSettings(IndentingPrintWriter indentingPrintWriter) {
        String str;
        boolean zConfigRestrictsAvoidBadWifi = this.mMultinetworkPolicyTracker.configRestrictsAvoidBadWifi();
        if (!zConfigRestrictsAvoidBadWifi) {
            indentingPrintWriter.println("Bad Wi-Fi avoidance: unrestricted");
            return;
        }
        indentingPrintWriter.println("Bad Wi-Fi avoidance: " + avoidBadWifi());
        indentingPrintWriter.increaseIndent();
        indentingPrintWriter.println("Config restrict:   " + zConfigRestrictsAvoidBadWifi);
        String avoidBadWifiSetting = this.mMultinetworkPolicyTracker.getAvoidBadWifiSetting();
        if ("0".equals(avoidBadWifiSetting)) {
            str = "get stuck";
        } else if (avoidBadWifiSetting == null) {
            str = "prompt";
        } else if ("1".equals(avoidBadWifiSetting)) {
            str = "avoid";
        } else {
            str = avoidBadWifiSetting + " (?)";
        }
        indentingPrintWriter.println("User setting:      " + str);
        indentingPrintWriter.println("Network overrides:");
        indentingPrintWriter.increaseIndent();
        for (NetworkAgentInfo networkAgentInfo : this.mNetworkAgentInfos.values()) {
            if (networkAgentInfo.avoidUnvalidated) {
                indentingPrintWriter.println(networkAgentInfo.name());
            }
        }
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.decreaseIndent();
    }

    static class AnonymousClass8 {
        static final int[] $SwitchMap$com$android$server$ConnectivityService$UnneededFor;
        static final int[] $SwitchMap$com$android$server$connectivity$NetworkNotificationManager$NotificationType = new int[NetworkNotificationManager.NotificationType.values().length];

        static {
            try {
                $SwitchMap$com$android$server$connectivity$NetworkNotificationManager$NotificationType[NetworkNotificationManager.NotificationType.NO_INTERNET.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$server$connectivity$NetworkNotificationManager$NotificationType[NetworkNotificationManager.NotificationType.LOST_INTERNET.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            $SwitchMap$com$android$server$ConnectivityService$UnneededFor = new int[UnneededFor.values().length];
            try {
                $SwitchMap$com$android$server$ConnectivityService$UnneededFor[UnneededFor.TEARDOWN.ordinal()] = 1;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$server$ConnectivityService$UnneededFor[UnneededFor.LINGER.ordinal()] = 2;
            } catch (NoSuchFieldError e4) {
            }
        }
    }

    private void showValidationNotification(NetworkAgentInfo networkAgentInfo, NetworkNotificationManager.NotificationType notificationType) {
        String str;
        switch (AnonymousClass8.$SwitchMap$com$android$server$connectivity$NetworkNotificationManager$NotificationType[notificationType.ordinal()]) {
            case 1:
                str = "android.net.conn.PROMPT_UNVALIDATED";
                break;
            case 2:
                str = "android.net.conn.PROMPT_LOST_VALIDATION";
                break;
            default:
                Slog.wtf(TAG, "Unknown notification type " + notificationType);
                return;
        }
        Intent intent = new Intent(str);
        intent.setData(Uri.fromParts("netId", Integer.toString(networkAgentInfo.network.netId), null));
        intent.addFlags(268435456);
        intent.setClassName("com.android.settings", "com.android.settings.wifi.WifiNoInternetDialog");
        this.mNotifier.showNotification(networkAgentInfo.network.netId, notificationType, networkAgentInfo, null, BenesseExtension.getDchaState() != 0 ? null : PendingIntent.getActivityAsUser(this.mContext, 0, intent, 268435456, null, UserHandle.CURRENT), true);
    }

    private void handlePromptUnvalidated(Network network) {
        NetworkAgentInfo networkAgentInfoForNetwork = getNetworkAgentInfoForNetwork(network);
        if (networkAgentInfoForNetwork == null || networkAgentInfoForNetwork.everValidated || networkAgentInfoForNetwork.everCaptivePortalDetected || !networkAgentInfoForNetwork.networkMisc.explicitlySelected || networkAgentInfoForNetwork.networkMisc.acceptUnvalidated) {
            return;
        }
        showValidationNotification(networkAgentInfoForNetwork, NetworkNotificationManager.NotificationType.NO_INTERNET);
    }

    private void handleNetworkUnvalidated(NetworkAgentInfo networkAgentInfo) {
        NetworkCapabilities networkCapabilities = networkAgentInfo.networkCapabilities;
        log("handleNetworkUnvalidated " + networkAgentInfo.name() + " cap=" + networkCapabilities);
        if (networkCapabilities.hasTransport(1) && this.mMultinetworkPolicyTracker.shouldNotifyWifiUnvalidated()) {
            showValidationNotification(networkAgentInfo, NetworkNotificationManager.NotificationType.LOST_INTERNET);
        }
    }

    public int getMultipathPreference(Network network) {
        enforceAccessPermission();
        NetworkAgentInfo networkAgentInfoForNetwork = getNetworkAgentInfoForNetwork(network);
        if (networkAgentInfoForNetwork != null && networkAgentInfoForNetwork.networkCapabilities.hasCapability(11)) {
            return 7;
        }
        Integer multipathPreference = this.mMultipathPolicyTracker.getMultipathPreference(network);
        if (multipathPreference != null) {
            return multipathPreference.intValue();
        }
        return this.mMultinetworkPolicyTracker.getMeteredMultipathPreference();
    }

    private class InternalHandler extends Handler {
        public InternalHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            int i = message.what;
            switch (i) {
                case 8:
                    break;
                case 9:
                    ConnectivityService.this.handleDeprecatedGlobalHttpProxy();
                    return;
                default:
                    switch (i) {
                        case 16:
                            ConnectivityService.this.handleApplyDefaultProxy((ProxyInfo) message.obj);
                            break;
                        case 17:
                            ConnectivityService.this.handleRegisterNetworkFactory((NetworkFactoryInfo) message.obj);
                            break;
                        case 18:
                            ConnectivityService.this.handleRegisterNetworkAgent((NetworkAgentInfo) message.obj);
                            break;
                        case 19:
                        case 21:
                            ConnectivityService.this.handleRegisterNetworkRequest((NetworkRequestInfo) message.obj);
                            break;
                        case 20:
                            ConnectivityService.this.handleTimedOutNetworkRequest((NetworkRequestInfo) message.obj);
                            break;
                        case 22:
                            ConnectivityService.this.handleReleaseNetworkRequest((NetworkRequest) message.obj, message.arg1);
                            break;
                        case 23:
                            ConnectivityService.this.handleUnregisterNetworkFactory((Messenger) message.obj);
                            break;
                        case 24:
                            break;
                        case 25:
                            Iterator it = ConnectivityService.this.mNetworkAgentInfos.values().iterator();
                            while (it.hasNext()) {
                                ((NetworkAgentInfo) it.next()).networkMonitor.systemReady = true;
                            }
                            ConnectivityService.this.mMultipathPolicyTracker.start();
                            break;
                        case 26:
                        case 31:
                            ConnectivityService.this.handleRegisterNetworkRequestWithIntent(message);
                            break;
                        case ConnectivityService.EVENT_RELEASE_NETWORK_REQUEST_WITH_INTENT:
                            ConnectivityService.this.handleReleaseNetworkRequestWithIntent((PendingIntent) message.obj, message.arg1);
                            break;
                        case 28:
                            ConnectivityService.this.handleSetAcceptUnvalidated((Network) message.obj, ConnectivityService.toBool(message.arg1), ConnectivityService.toBool(message.arg2));
                            break;
                        case 29:
                            ConnectivityService.this.handlePromptUnvalidated((Network) message.obj);
                            break;
                        case 30:
                            ConnectivityService.this.handleMobileDataAlwaysOn();
                            break;
                        default:
                            switch (i) {
                                case 35:
                                    ConnectivityService.this.handleSetAvoidUnvalidated((Network) message.obj);
                                    break;
                                case 36:
                                    ConnectivityService.this.handleReportNetworkConnectivity((Network) message.obj, message.arg1, ConnectivityService.toBool(message.arg2));
                                    break;
                                case 37:
                                    ConnectivityService.this.handlePrivateDnsSettingsChanged();
                                    break;
                                case 38:
                                    ConnectivityService.this.handlePrivateDnsValidationUpdate((DnsManager.PrivateDnsValidationUpdate) message.obj);
                                    break;
                                default:
                                    switch (i) {
                                        case 528395:
                                            ConnectivityService.this.mKeepaliveTracker.handleStartKeepalive(message);
                                            break;
                                        case 528396:
                                            ConnectivityService.this.mKeepaliveTracker.handleStopKeepalive(ConnectivityService.this.getNetworkAgentInfoForNetwork((Network) message.obj), message.arg1, message.arg2);
                                            break;
                                    }
                                    break;
                            }
                            break;
                    }
            }
            ConnectivityService.this.handleReleaseNetworkTransitionWakelock(message.what);
        }
    }

    public int tether(String str, String str2) {
        ConnectivityManager.enforceTetherChangePermission(this.mContext, str2);
        if (isTetheringSupported()) {
            return this.mTethering.tether(str);
        }
        return 3;
    }

    public int untether(String str, String str2) {
        ConnectivityManager.enforceTetherChangePermission(this.mContext, str2);
        if (isTetheringSupported()) {
            return this.mTethering.untether(str);
        }
        return 3;
    }

    public int getLastTetherError(String str) {
        enforceTetherAccessPermission();
        if (isTetheringSupported()) {
            return this.mTethering.getLastTetherError(str);
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

    public int setUsbTethering(boolean z, String str) {
        ConnectivityManager.enforceTetherChangePermission(this.mContext, str);
        if (isTetheringSupported()) {
            return this.mTethering.setUsbTethering(z);
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

    public boolean isTetheringSupported(String str) {
        ConnectivityManager.enforceTetherChangePermission(this.mContext, str);
        return isTetheringSupported();
    }

    private boolean isTetheringSupported() {
        boolean z = toBool(Settings.Global.getInt(this.mContext.getContentResolver(), "tether_supported", encodeBool(this.mSystemProperties.get("ro.tether.denied").equals("true") ^ true))) && !this.mUserManager.hasUserRestriction("no_config_tethering");
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            return z && this.mUserManager.isAdminUser() && this.mTethering.hasTetherableConfiguration();
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void startTethering(int i, ResultReceiver resultReceiver, boolean z, String str) {
        ConnectivityManager.enforceTetherChangePermission(this.mContext, str);
        if (!isTetheringSupported()) {
            resultReceiver.send(3, null);
        } else {
            this.mTethering.startTethering(i, resultReceiver, z);
        }
    }

    public void stopTethering(int i, String str) {
        ConnectivityManager.enforceTetherChangePermission(this.mContext, str);
        this.mTethering.stopTethering(i);
    }

    private void ensureNetworkTransitionWakelock(String str) {
        synchronized (this) {
            if (this.mNetTransitionWakeLock.isHeld()) {
                return;
            }
            this.mNetTransitionWakeLock.acquire();
            this.mLastWakeLockAcquireTimestamp = SystemClock.elapsedRealtime();
            this.mTotalWakelockAcquisitions++;
            this.mWakelockLogs.log("ACQUIRE for " + str);
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(24), (long) this.mNetTransitionWakeLockTimeout);
        }
    }

    private void scheduleReleaseNetworkTransitionWakelock() {
        synchronized (this) {
            if (this.mNetTransitionWakeLock.isHeld()) {
                this.mHandler.removeMessages(24);
                this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(8), 1000L);
            }
        }
    }

    private void handleReleaseNetworkTransitionWakelock(int i) {
        String strEventName = eventName(i);
        synchronized (this) {
            if (!this.mNetTransitionWakeLock.isHeld()) {
                this.mWakelockLogs.log(String.format("RELEASE: already released (%s)", strEventName));
                Slog.w(TAG, "expected Net Transition WakeLock to be held");
                return;
            }
            this.mNetTransitionWakeLock.release();
            long jElapsedRealtime = SystemClock.elapsedRealtime() - this.mLastWakeLockAcquireTimestamp;
            this.mTotalWakelockDurationMs += jElapsedRealtime;
            this.mMaxWakelockDurationMs = Math.max(this.mMaxWakelockDurationMs, jElapsedRealtime);
            this.mTotalWakelockReleases++;
            this.mWakelockLogs.log(String.format("RELEASE (%s)", strEventName));
        }
    }

    public void reportInetCondition(int i, int i2) {
        NetworkAgentInfo networkForType = this.mLegacyTypeTracker.getNetworkForType(i);
        if (networkForType == null) {
            return;
        }
        reportNetworkConnectivity(networkForType.network, i2 > 50);
    }

    public void reportNetworkConnectivity(Network network, boolean z) {
        enforceAccessPermission();
        enforceInternetPermission();
        this.mHandler.sendMessage(this.mHandler.obtainMessage(36, Binder.getCallingUid(), encodeBool(z), network));
    }

    private void handleReportNetworkConnectivity(Network network, int i, boolean z) {
        NetworkAgentInfo networkAgentInfoForNetwork;
        if (network == null) {
            networkAgentInfoForNetwork = getDefaultNetwork();
        } else {
            networkAgentInfoForNetwork = getNetworkAgentInfoForNetwork(network);
        }
        if (networkAgentInfoForNetwork == null || networkAgentInfoForNetwork.networkInfo.getState() == NetworkInfo.State.DISCONNECTING || networkAgentInfoForNetwork.networkInfo.getState() == NetworkInfo.State.DISCONNECTED || z == networkAgentInfoForNetwork.lastValidated) {
            return;
        }
        log("reportNetworkConnectivity(" + networkAgentInfoForNetwork.network.netId + ", " + z + ") by " + i);
        if (!networkAgentInfoForNetwork.everConnected || isNetworkWithLinkPropertiesBlocked(getLinkProperties(networkAgentInfoForNetwork), i, false)) {
            return;
        }
        networkAgentInfoForNetwork.networkMonitor.forceReevaluation(i);
    }

    private ProxyInfo getDefaultProxy() {
        ProxyInfo proxyInfo;
        synchronized (this.mProxyLock) {
            proxyInfo = this.mGlobalProxy;
            if (proxyInfo == null && !this.mDefaultProxyDisabled) {
                proxyInfo = this.mDefaultProxy;
            }
        }
        return proxyInfo;
    }

    public ProxyInfo getProxyForNetwork(Network network) {
        NetworkAgentInfo networkAgentInfoForNetwork;
        if (network == null) {
            return getDefaultProxy();
        }
        ProxyInfo globalProxy = getGlobalProxy();
        if (globalProxy != null) {
            return globalProxy;
        }
        if (!NetworkUtils.queryUserAccess(Binder.getCallingUid(), network.netId) || (networkAgentInfoForNetwork = getNetworkAgentInfoForNetwork(network)) == null) {
            return null;
        }
        synchronized (networkAgentInfoForNetwork) {
            ProxyInfo httpProxy = networkAgentInfoForNetwork.linkProperties.getHttpProxy();
            if (httpProxy == null) {
                return null;
            }
            return new ProxyInfo(httpProxy);
        }
    }

    private ProxyInfo canonicalizeProxyInfo(ProxyInfo proxyInfo) {
        if (proxyInfo != null && TextUtils.isEmpty(proxyInfo.getHost())) {
            if (proxyInfo.getPacFileUrl() == null || Uri.EMPTY.equals(proxyInfo.getPacFileUrl())) {
                return null;
            }
            return proxyInfo;
        }
        return proxyInfo;
    }

    private boolean proxyInfoEqual(ProxyInfo proxyInfo, ProxyInfo proxyInfo2) {
        ProxyInfo proxyInfoCanonicalizeProxyInfo = canonicalizeProxyInfo(proxyInfo);
        ProxyInfo proxyInfoCanonicalizeProxyInfo2 = canonicalizeProxyInfo(proxyInfo2);
        return Objects.equals(proxyInfoCanonicalizeProxyInfo, proxyInfoCanonicalizeProxyInfo2) && (proxyInfoCanonicalizeProxyInfo == null || Objects.equals(proxyInfoCanonicalizeProxyInfo.getHost(), proxyInfoCanonicalizeProxyInfo2.getHost()));
    }

    public void setGlobalProxy(ProxyInfo proxyInfo) {
        enforceConnectivityInternalPermission();
        synchronized (this.mProxyLock) {
            if (proxyInfo == this.mGlobalProxy) {
                return;
            }
            if (proxyInfo == null || !proxyInfo.equals(this.mGlobalProxy)) {
                if (this.mGlobalProxy == null || !this.mGlobalProxy.equals(proxyInfo)) {
                    String host = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
                    int port = 0;
                    String exclusionListAsString = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
                    String string = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
                    if (proxyInfo != null && (!TextUtils.isEmpty(proxyInfo.getHost()) || !Uri.EMPTY.equals(proxyInfo.getPacFileUrl()))) {
                        if (!proxyInfo.isValid()) {
                            log("Invalid proxy properties, ignoring: " + proxyInfo.toString());
                            return;
                        }
                        this.mGlobalProxy = new ProxyInfo(proxyInfo);
                        host = this.mGlobalProxy.getHost();
                        port = this.mGlobalProxy.getPort();
                        exclusionListAsString = this.mGlobalProxy.getExclusionListAsString();
                        if (!Uri.EMPTY.equals(proxyInfo.getPacFileUrl())) {
                            string = proxyInfo.getPacFileUrl().toString();
                        }
                    } else {
                        this.mGlobalProxy = null;
                    }
                    ContentResolver contentResolver = this.mContext.getContentResolver();
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        Settings.Global.putString(contentResolver, "global_http_proxy_host", host);
                        Settings.Global.putInt(contentResolver, "global_http_proxy_port", port);
                        Settings.Global.putString(contentResolver, "global_http_proxy_exclusion_list", exclusionListAsString);
                        Settings.Global.putString(contentResolver, "global_proxy_pac_url", string);
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                        if (this.mGlobalProxy == null) {
                            proxyInfo = this.mDefaultProxy;
                        }
                        sendProxyBroadcast(proxyInfo);
                    } catch (Throwable th) {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                        throw th;
                    }
                }
            }
        }
    }

    private void loadGlobalProxy() {
        ProxyInfo proxyInfo;
        ContentResolver contentResolver = this.mContext.getContentResolver();
        String string = Settings.Global.getString(contentResolver, "global_http_proxy_host");
        int i = Settings.Global.getInt(contentResolver, "global_http_proxy_port", 0);
        String string2 = Settings.Global.getString(contentResolver, "global_http_proxy_exclusion_list");
        String string3 = Settings.Global.getString(contentResolver, "global_proxy_pac_url");
        if (!TextUtils.isEmpty(string) || !TextUtils.isEmpty(string3)) {
            if (!TextUtils.isEmpty(string3)) {
                proxyInfo = new ProxyInfo(string3);
            } else {
                proxyInfo = new ProxyInfo(string, i, string2);
            }
            if (!proxyInfo.isValid()) {
                log("Invalid proxy properties, ignoring: " + proxyInfo.toString());
                return;
            }
            synchronized (this.mProxyLock) {
                this.mGlobalProxy = proxyInfo;
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

    private void handleApplyDefaultProxy(ProxyInfo proxyInfo) {
        if (proxyInfo != null && TextUtils.isEmpty(proxyInfo.getHost()) && Uri.EMPTY.equals(proxyInfo.getPacFileUrl())) {
            proxyInfo = null;
        }
        synchronized (this.mProxyLock) {
            if (this.mDefaultProxy == null || !this.mDefaultProxy.equals(proxyInfo)) {
                if (this.mDefaultProxy == proxyInfo) {
                    return;
                }
                if (proxyInfo != null && !proxyInfo.isValid()) {
                    log("Invalid proxy properties, ignoring: " + proxyInfo.toString());
                    return;
                }
                if (this.mGlobalProxy != null && proxyInfo != null && !Uri.EMPTY.equals(proxyInfo.getPacFileUrl()) && proxyInfo.getPacFileUrl().equals(this.mGlobalProxy.getPacFileUrl())) {
                    this.mGlobalProxy = proxyInfo;
                    sendProxyBroadcast(this.mGlobalProxy);
                    return;
                }
                this.mDefaultProxy = proxyInfo;
                if (this.mGlobalProxy != null) {
                    return;
                }
                if (!this.mDefaultProxyDisabled) {
                    sendProxyBroadcast(proxyInfo);
                }
            }
        }
    }

    private void updateProxy(LinkProperties linkProperties, LinkProperties linkProperties2, NetworkAgentInfo networkAgentInfo) {
        ProxyInfo httpProxy;
        if (linkProperties != null) {
            httpProxy = linkProperties.getHttpProxy();
        } else {
            httpProxy = null;
        }
        if (!proxyInfoEqual(httpProxy, linkProperties2 != null ? linkProperties2.getHttpProxy() : null)) {
            sendProxyBroadcast(getDefaultProxy());
        }
    }

    private void handleDeprecatedGlobalHttpProxy() {
        String string = Settings.Global.getString(this.mContext.getContentResolver(), "http_proxy");
        if (!TextUtils.isEmpty(string)) {
            String[] strArrSplit = string.split(":");
            if (strArrSplit.length == 0) {
                return;
            }
            String str = strArrSplit[0];
            int i = 8080;
            if (strArrSplit.length > 1) {
                try {
                    i = Integer.parseInt(strArrSplit[1]);
                } catch (NumberFormatException e) {
                    return;
                }
            }
            setGlobalProxy(new ProxyInfo(strArrSplit[0], i, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS));
        }
    }

    private void sendProxyBroadcast(ProxyInfo proxyInfo) {
        if (proxyInfo == null) {
            proxyInfo = new ProxyInfo(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, 0, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        }
        if (this.mPacManager.setCurrentProxyScriptUrl(proxyInfo)) {
            return;
        }
        log("sending Proxy Broadcast for " + proxyInfo);
        Intent intent = new Intent("android.intent.action.PROXY_CHANGE");
        intent.addFlags(603979776);
        intent.putExtra("android.intent.extra.PROXY_INFO", proxyInfo);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
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

        void observe(Uri uri, int i) {
            this.mUriEventMap.put(uri, Integer.valueOf(i));
            this.mContext.getContentResolver().registerContentObserver(uri, false, this);
        }

        @Override
        public void onChange(boolean z) {
            Slog.wtf(ConnectivityService.TAG, "Should never be reached.");
        }

        @Override
        public void onChange(boolean z, Uri uri) {
            Integer num = this.mUriEventMap.get(uri);
            if (num != null) {
                this.mHandler.obtainMessage(num.intValue()).sendToTarget();
                return;
            }
            ConnectivityService.loge("No matching event to send for URI=" + uri);
        }
    }

    private static void log(String str) {
        Slog.d(TAG, str);
    }

    private static void loge(String str) {
        Slog.e(TAG, str);
    }

    private static void loge(String str, Throwable th) {
        Slog.e(TAG, str, th);
    }

    public boolean prepareVpn(String str, String str2, int i) {
        enforceCrossUserPermission(i);
        synchronized (this.mVpns) {
            throwIfLockdownEnabled();
            Vpn vpn = this.mVpns.get(i);
            if (vpn != null) {
                return vpn.prepare(str, str2);
            }
            return false;
        }
    }

    public void setVpnPackageAuthorization(String str, int i, boolean z) {
        enforceCrossUserPermission(i);
        synchronized (this.mVpns) {
            Vpn vpn = this.mVpns.get(i);
            if (vpn != null) {
                vpn.setPackageAuthorization(str, z);
            }
        }
    }

    public ParcelFileDescriptor establishVpn(VpnConfig vpnConfig) {
        ParcelFileDescriptor parcelFileDescriptorEstablish;
        int userId = UserHandle.getUserId(Binder.getCallingUid());
        synchronized (this.mVpns) {
            throwIfLockdownEnabled();
            parcelFileDescriptorEstablish = this.mVpns.get(userId).establish(vpnConfig);
        }
        return parcelFileDescriptorEstablish;
    }

    public void startLegacyVpn(VpnProfile vpnProfile) {
        int userId = UserHandle.getUserId(Binder.getCallingUid());
        LinkProperties activeLinkProperties = getActiveLinkProperties();
        if (activeLinkProperties == null) {
            throw new IllegalStateException("Missing active network connection");
        }
        synchronized (this.mVpns) {
            throwIfLockdownEnabled();
            this.mVpns.get(userId).startLegacyVpn(vpnProfile, this.mKeyStore, activeLinkProperties);
        }
    }

    public LegacyVpnInfo getLegacyVpnInfo(int i) {
        LegacyVpnInfo legacyVpnInfo;
        enforceCrossUserPermission(i);
        synchronized (this.mVpns) {
            legacyVpnInfo = this.mVpns.get(i).getLegacyVpnInfo();
        }
        return legacyVpnInfo;
    }

    public VpnInfo[] getAllVpnInfo() {
        enforceConnectivityInternalPermission();
        synchronized (this.mVpns) {
            if (this.mLockdownEnabled) {
                return new VpnInfo[0];
            }
            ArrayList arrayList = new ArrayList();
            for (int i = 0; i < this.mVpns.size(); i++) {
                VpnInfo vpnInfoCreateVpnInfo = createVpnInfo(this.mVpns.valueAt(i));
                if (vpnInfoCreateVpnInfo != null) {
                    arrayList.add(vpnInfoCreateVpnInfo);
                }
            }
            return (VpnInfo[]) arrayList.toArray(new VpnInfo[arrayList.size()]);
        }
    }

    private VpnInfo createVpnInfo(Vpn vpn) {
        LinkProperties linkProperties;
        VpnInfo vpnInfo = vpn.getVpnInfo();
        if (vpnInfo == null) {
            return null;
        }
        Network[] underlyingNetworks = vpn.getUnderlyingNetworks();
        if (underlyingNetworks == null) {
            NetworkAgentInfo defaultNetwork = getDefaultNetwork();
            if (defaultNetwork != null && defaultNetwork.linkProperties != null) {
                vpnInfo.primaryUnderlyingIface = getDefaultNetwork().linkProperties.getInterfaceName();
            }
        } else if (underlyingNetworks.length > 0 && (linkProperties = getLinkProperties(underlyingNetworks[0])) != null) {
            vpnInfo.primaryUnderlyingIface = linkProperties.getInterfaceName();
        }
        if (vpnInfo.primaryUnderlyingIface == null) {
            return null;
        }
        return vpnInfo;
    }

    public VpnConfig getVpnConfig(int i) {
        enforceCrossUserPermission(i);
        synchronized (this.mVpns) {
            Vpn vpn = this.mVpns.get(i);
            if (vpn != null) {
                return vpn.getVpnConfig();
            }
            return null;
        }
    }

    private void updateAllVpnsCapabilities() {
        synchronized (this.mVpns) {
            for (int i = 0; i < this.mVpns.size(); i++) {
                this.mVpns.valueAt(i).updateCapabilities();
            }
        }
    }

    public boolean updateLockdownVpn() {
        if (Binder.getCallingUid() != 1000) {
            Slog.w(TAG, "Lockdown VPN only available to AID_SYSTEM");
            return false;
        }
        synchronized (this.mVpns) {
            this.mLockdownEnabled = LockdownVpnTracker.isEnabled();
            if (this.mLockdownEnabled) {
                byte[] bArr = this.mKeyStore.get("LOCKDOWN_VPN");
                if (bArr == null) {
                    Slog.e(TAG, "Lockdown VPN configured but cannot be read from keystore");
                    return false;
                }
                String str = new String(bArr);
                VpnProfile vpnProfileDecode = VpnProfile.decode(str, this.mKeyStore.get("VPN_" + str));
                if (vpnProfileDecode == null) {
                    Slog.e(TAG, "Lockdown VPN configured invalid profile " + str);
                    setLockdownTracker(null);
                    return true;
                }
                int userId = UserHandle.getUserId(Binder.getCallingUid());
                Vpn vpn = this.mVpns.get(userId);
                if (vpn == null) {
                    Slog.w(TAG, "VPN for user " + userId + " not ready yet. Skipping lockdown");
                    return false;
                }
                setLockdownTracker(new LockdownVpnTracker(this.mContext, this.mNetd, this, vpn, vpnProfileDecode));
            } else {
                setLockdownTracker(null);
            }
            return true;
        }
    }

    @GuardedBy("mVpns")
    private void setLockdownTracker(LockdownVpnTracker lockdownVpnTracker) {
        LockdownVpnTracker lockdownVpnTracker2 = this.mLockdownTracker;
        this.mLockdownTracker = null;
        if (lockdownVpnTracker2 != null) {
            lockdownVpnTracker2.shutdown();
        }
        if (lockdownVpnTracker != null) {
            this.mLockdownTracker = lockdownVpnTracker;
            this.mLockdownTracker.init();
        }
    }

    @GuardedBy("mVpns")
    private void throwIfLockdownEnabled() {
        if (this.mLockdownEnabled) {
            throw new IllegalStateException("Unavailable in lockdown mode");
        }
    }

    private boolean startAlwaysOnVpn(int i) {
        synchronized (this.mVpns) {
            Vpn vpn = this.mVpns.get(i);
            if (vpn == null) {
                Slog.wtf(TAG, "User " + i + " has no Vpn configuration");
                return false;
            }
            return vpn.startAlwaysOnVpn();
        }
    }

    public boolean isAlwaysOnVpnPackageSupported(int i, String str) {
        enforceSettingsPermission();
        enforceCrossUserPermission(i);
        synchronized (this.mVpns) {
            Vpn vpn = this.mVpns.get(i);
            if (vpn == null) {
                Slog.w(TAG, "User " + i + " has no Vpn configuration");
                return false;
            }
            return vpn.isAlwaysOnPackageSupported(str);
        }
    }

    public boolean setAlwaysOnVpnPackage(int i, String str, boolean z) {
        enforceConnectivityInternalPermission();
        enforceCrossUserPermission(i);
        synchronized (this.mVpns) {
            if (LockdownVpnTracker.isEnabled()) {
                return false;
            }
            Vpn vpn = this.mVpns.get(i);
            if (vpn == null) {
                Slog.w(TAG, "User " + i + " has no Vpn configuration");
                return false;
            }
            if (!vpn.setAlwaysOnPackage(str, z)) {
                return false;
            }
            if (!startAlwaysOnVpn(i)) {
                vpn.setAlwaysOnPackage(null, false);
                return false;
            }
            return true;
        }
    }

    public String getAlwaysOnVpnPackage(int i) {
        enforceConnectivityInternalPermission();
        enforceCrossUserPermission(i);
        synchronized (this.mVpns) {
            Vpn vpn = this.mVpns.get(i);
            if (vpn == null) {
                Slog.w(TAG, "User " + i + " has no Vpn configuration");
                return null;
            }
            return vpn.getAlwaysOnPackage();
        }
    }

    public int checkMobileProvisioning(int i) {
        return -1;
    }

    private String getProvisioningUrlBaseFromFile() throws Throwable {
        FileReader fileReader;
        String attributeValue;
        String attributeValue2;
        Configuration configuration = this.mContext.getResources().getConfiguration();
        FileReader fileReader2 = null;
        try {
            try {
                fileReader = new FileReader(this.mProvisioningUrlFile);
                try {
                    XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                    xmlPullParserNewPullParser.setInput(fileReader);
                    XmlUtils.beginDocument(xmlPullParserNewPullParser, TAG_PROVISIONING_URLS);
                    while (true) {
                        XmlUtils.nextElement(xmlPullParserNewPullParser);
                        String name = xmlPullParserNewPullParser.getName();
                        if (name == null) {
                            try {
                                fileReader.close();
                            } catch (IOException e) {
                            }
                            return null;
                        }
                        if (name.equals(TAG_PROVISIONING_URL) && (attributeValue = xmlPullParserNewPullParser.getAttributeValue(null, ATTR_MCC)) != null) {
                            try {
                                if (Integer.parseInt(attributeValue) == configuration.mcc && (attributeValue2 = xmlPullParserNewPullParser.getAttributeValue(null, ATTR_MNC)) != null && Integer.parseInt(attributeValue2) == configuration.mnc) {
                                    xmlPullParserNewPullParser.next();
                                    if (xmlPullParserNewPullParser.getEventType() == 4) {
                                        String text = xmlPullParserNewPullParser.getText();
                                        try {
                                            fileReader.close();
                                        } catch (IOException e2) {
                                        }
                                        return text;
                                    }
                                    continue;
                                }
                            } catch (NumberFormatException e3) {
                                loge("NumberFormatException in getProvisioningUrlBaseFromFile: " + e3);
                            }
                        }
                    }
                } catch (FileNotFoundException e4) {
                    loge("Carrier Provisioning Urls file not found");
                    if (fileReader != null) {
                        try {
                            fileReader.close();
                        } catch (IOException e5) {
                        }
                    }
                    return null;
                } catch (IOException e6) {
                    e = e6;
                    loge("I/O exception reading Carrier Provisioning Urls file: " + e);
                } catch (XmlPullParserException e7) {
                    e = e7;
                    loge("Xml parser exception reading Carrier Provisioning Urls file: " + e);
                    if (fileReader != null) {
                        fileReader.close();
                    }
                    return null;
                }
            } catch (Throwable th) {
                th = th;
                if (0 != 0) {
                    try {
                        fileReader2.close();
                    } catch (IOException e8) {
                    }
                }
                throw th;
            }
        } catch (FileNotFoundException e9) {
            fileReader = null;
        } catch (IOException e10) {
            e = e10;
            fileReader = null;
        } catch (XmlPullParserException e11) {
            e = e11;
            fileReader = null;
        } catch (Throwable th2) {
            th = th2;
            if (0 != 0) {
            }
            throw th;
        }
    }

    public String getMobileProvisioningUrl() throws Throwable {
        enforceConnectivityInternalPermission();
        String provisioningUrlBaseFromFile = getProvisioningUrlBaseFromFile();
        if (TextUtils.isEmpty(provisioningUrlBaseFromFile)) {
            provisioningUrlBaseFromFile = this.mContext.getResources().getString(R.string.ext_media_move_specific_title);
            log("getMobileProvisioningUrl: mobile_provisioining_url from resource =" + provisioningUrlBaseFromFile);
        } else {
            log("getMobileProvisioningUrl: mobile_provisioning_url from File =" + provisioningUrlBaseFromFile);
        }
        if (!TextUtils.isEmpty(provisioningUrlBaseFromFile)) {
            String line1Number = this.mTelephonyManager.getLine1Number();
            if (TextUtils.isEmpty(line1Number)) {
                line1Number = "0000000000";
            }
            return String.format(provisioningUrlBaseFromFile, this.mTelephonyManager.getSimSerialNumber(), this.mTelephonyManager.getDeviceId(), line1Number);
        }
        return provisioningUrlBaseFromFile;
    }

    public void setProvisioningNotificationVisible(boolean z, int i, String str) {
        enforceConnectivityInternalPermission();
        if (!ConnectivityManager.isNetworkTypeValid(i)) {
            return;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mNotifier.setProvNotificationVisible(z, 64512 + i + 1, str);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void setAirplaneMode(boolean z) {
        enforceConnectivityInternalPermission();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            Settings.Global.putInt(this.mContext.getContentResolver(), "airplane_mode_on", encodeBool(z));
            Intent intent = new Intent("android.intent.action.AIRPLANE_MODE");
            intent.putExtra(AudioService.CONNECT_INTENT_KEY_STATE, z);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void onUserStart(int i) {
        synchronized (this.mVpns) {
            if (this.mVpns.get(i) != null) {
                loge("Starting user already has a VPN");
                return;
            }
            this.mVpns.put(i, new Vpn(this.mHandler.getLooper(), this.mContext, this.mNetd, i));
            if (this.mUserManager.getUserInfo(i).isPrimary() && LockdownVpnTracker.isEnabled()) {
                updateLockdownVpn();
            }
        }
    }

    private void onUserStop(int i) {
        synchronized (this.mVpns) {
            Vpn vpn = this.mVpns.get(i);
            if (vpn == null) {
                loge("Stopped user has no VPN");
            } else {
                vpn.onUserStopped();
                this.mVpns.delete(i);
            }
        }
    }

    private void onUserAdded(int i) {
        synchronized (this.mVpns) {
            int size = this.mVpns.size();
            for (int i2 = 0; i2 < size; i2++) {
                this.mVpns.valueAt(i2).onUserAdded(i);
            }
        }
    }

    private void onUserRemoved(int i) {
        synchronized (this.mVpns) {
            int size = this.mVpns.size();
            for (int i2 = 0; i2 < size; i2++) {
                this.mVpns.valueAt(i2).onUserRemoved(i);
            }
        }
    }

    private void onUserUnlocked(int i) {
        synchronized (this.mVpns) {
            if (this.mUserManager.getUserInfo(i).isPrimary() && LockdownVpnTracker.isEnabled()) {
                updateLockdownVpn();
            } else {
                startAlwaysOnVpn(i);
            }
        }
    }

    private static class NetworkFactoryInfo {
        public final AsyncChannel asyncChannel;
        public final Messenger messenger;
        public final String name;

        public NetworkFactoryInfo(String str, Messenger messenger, AsyncChannel asyncChannel) {
            this.name = str;
            this.messenger = messenger;
            this.asyncChannel = asyncChannel;
        }
    }

    private void ensureNetworkRequestHasType(NetworkRequest networkRequest) {
        if (networkRequest.type == NetworkRequest.Type.NONE) {
            throw new IllegalArgumentException("All NetworkRequests in ConnectivityService must have a type");
        }
    }

    private class NetworkRequestInfo implements IBinder.DeathRecipient {
        private final IBinder mBinder;
        final PendingIntent mPendingIntent;
        boolean mPendingIntentSent;
        final int mPid;
        final int mUid;
        final Messenger messenger;
        final NetworkRequest request;

        NetworkRequestInfo(NetworkRequest networkRequest, PendingIntent pendingIntent) {
            this.request = networkRequest;
            ConnectivityService.this.ensureNetworkRequestHasType(this.request);
            this.mPendingIntent = pendingIntent;
            this.messenger = null;
            this.mBinder = null;
            this.mPid = Binder.getCallingPid();
            this.mUid = Binder.getCallingUid();
            enforceRequestCountLimit();
        }

        NetworkRequestInfo(Messenger messenger, NetworkRequest networkRequest, IBinder iBinder) {
            this.messenger = messenger;
            this.request = networkRequest;
            ConnectivityService.this.ensureNetworkRequestHasType(this.request);
            this.mBinder = iBinder;
            this.mPid = Binder.getCallingPid();
            this.mUid = Binder.getCallingUid();
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
                int i = ConnectivityService.this.mUidToNetworkRequestCount.get(this.mUid, 0) + 1;
                if (i < 100) {
                    ConnectivityService.this.mUidToNetworkRequestCount.put(this.mUid, i);
                } else {
                    throw new ServiceSpecificException(1);
                }
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
            String str;
            StringBuilder sb = new StringBuilder();
            sb.append("uid/pid:");
            sb.append(this.mUid);
            sb.append(SliceClientPermissions.SliceAuthority.DELIMITER);
            sb.append(this.mPid);
            sb.append(" ");
            sb.append(this.request);
            if (this.mPendingIntent == null) {
                str = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            } else {
                str = " to trigger " + this.mPendingIntent;
            }
            sb.append(str);
            return sb.toString();
        }
    }

    private void ensureRequestableCapabilities(NetworkCapabilities networkCapabilities) {
        String strDescribeFirstNonRequestableCapability = networkCapabilities.describeFirstNonRequestableCapability();
        if (strDescribeFirstNonRequestableCapability != null) {
            throw new IllegalArgumentException("Cannot request network with " + strDescribeFirstNonRequestableCapability);
        }
    }

    private void ensureSufficientPermissionsForRequest(NetworkCapabilities networkCapabilities, int i, int i2) {
        if (networkCapabilities.getSSID() != null && !checkSettingsPermission(i, i2)) {
            throw new SecurityException("Insufficient permissions to request a specific SSID");
        }
    }

    private ArrayList<Integer> getSignalStrengthThresholds(NetworkAgentInfo networkAgentInfo) {
        TreeSet treeSet = new TreeSet();
        synchronized (networkAgentInfo) {
            for (NetworkRequestInfo networkRequestInfo : this.mNetworkRequests.values()) {
                if (networkRequestInfo.request.networkCapabilities.hasSignalStrength() && networkAgentInfo.satisfiesImmutableCapabilitiesOf(networkRequestInfo.request)) {
                    treeSet.add(Integer.valueOf(networkRequestInfo.request.networkCapabilities.getSignalStrength()));
                }
            }
        }
        return new ArrayList<>(treeSet);
    }

    private void updateSignalStrengthThresholds(NetworkAgentInfo networkAgentInfo, String str, NetworkRequest networkRequest) {
        ArrayList<Integer> signalStrengthThresholds = getSignalStrengthThresholds(networkAgentInfo);
        Bundle bundle = new Bundle();
        bundle.putIntegerArrayList("thresholds", signalStrengthThresholds);
        if (!"CONNECT".equals(str)) {
            if (networkRequest != null && networkRequest.networkCapabilities.hasSignalStrength()) {
                str = str + " " + networkRequest.networkCapabilities.getSignalStrength();
            }
            log(String.format("updateSignalStrengthThresholds: %s, sending %s to %s", str, Arrays.toString(signalStrengthThresholds.toArray()), networkAgentInfo.name()));
        }
        networkAgentInfo.asyncChannel.sendMessage(528398, 0, 0, bundle);
    }

    private void ensureValidNetworkSpecifier(NetworkCapabilities networkCapabilities) {
        NetworkSpecifier networkSpecifier;
        if (networkCapabilities == null || (networkSpecifier = networkCapabilities.getNetworkSpecifier()) == null) {
            return;
        }
        MatchAllNetworkSpecifier.checkNotMatchAllNetworkSpecifier(networkSpecifier);
        networkSpecifier.assertValidFromUid(Binder.getCallingUid());
    }

    public NetworkRequest requestNetwork(NetworkCapabilities networkCapabilities, Messenger messenger, int i, IBinder iBinder, int i2) {
        NetworkRequest.Type type;
        NetworkCapabilities networkCapabilitiesCreateDefaultNetworkCapabilitiesForUid;
        if (networkCapabilities == null) {
            type = NetworkRequest.Type.TRACK_DEFAULT;
        } else {
            type = NetworkRequest.Type.REQUEST;
        }
        if (type == NetworkRequest.Type.TRACK_DEFAULT) {
            networkCapabilitiesCreateDefaultNetworkCapabilitiesForUid = createDefaultNetworkCapabilitiesForUid(Binder.getCallingUid());
            enforceAccessPermission();
        } else {
            NetworkCapabilities networkCapabilities2 = new NetworkCapabilities(networkCapabilities);
            enforceNetworkRequestPermissions(networkCapabilities2);
            enforceMeteredApnPolicy(networkCapabilities2);
            networkCapabilitiesCreateDefaultNetworkCapabilitiesForUid = networkCapabilities2;
        }
        ensureRequestableCapabilities(networkCapabilitiesCreateDefaultNetworkCapabilitiesForUid);
        ensureSufficientPermissionsForRequest(networkCapabilitiesCreateDefaultNetworkCapabilitiesForUid, Binder.getCallingPid(), Binder.getCallingUid());
        restrictRequestUidsForCaller(networkCapabilitiesCreateDefaultNetworkCapabilitiesForUid);
        if (i < 0) {
            throw new IllegalArgumentException("Bad timeout specified");
        }
        ensureValidNetworkSpecifier(networkCapabilitiesCreateDefaultNetworkCapabilitiesForUid);
        NetworkRequest networkRequest = new NetworkRequest(networkCapabilitiesCreateDefaultNetworkCapabilitiesForUid, i2, nextNetworkRequestId(), type);
        NetworkRequestInfo networkRequestInfo = new NetworkRequestInfo(messenger, networkRequest, iBinder);
        log("requestNetwork for " + networkRequestInfo);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(19, networkRequestInfo));
        if (i > 0) {
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(20, networkRequestInfo), i);
        }
        return networkRequest;
    }

    private void enforceNetworkRequestPermissions(NetworkCapabilities networkCapabilities) {
        if (!networkCapabilities.hasCapability(13)) {
            enforceConnectivityRestrictedNetworksPermission();
        } else {
            enforceChangePermission();
        }
    }

    public boolean requestBandwidthUpdate(Network network) {
        NetworkAgentInfo networkAgentInfo;
        enforceAccessPermission();
        if (network == null) {
            return false;
        }
        synchronized (this.mNetworkForNetId) {
            networkAgentInfo = this.mNetworkForNetId.get(network.netId);
        }
        if (networkAgentInfo == null) {
            return false;
        }
        networkAgentInfo.asyncChannel.sendMessage(528394);
        return true;
    }

    private boolean isSystem(int i) {
        return i < 10000;
    }

    private void enforceMeteredApnPolicy(NetworkCapabilities networkCapabilities) {
        int callingUid = Binder.getCallingUid();
        if (!isSystem(callingUid) && !networkCapabilities.hasCapability(11) && this.mPolicyManagerInternal.isUidRestrictedOnMeteredNetworks(callingUid)) {
            networkCapabilities.addCapability(11);
        }
    }

    public NetworkRequest pendingRequestForNetwork(NetworkCapabilities networkCapabilities, PendingIntent pendingIntent) {
        Preconditions.checkNotNull(pendingIntent, "PendingIntent cannot be null.");
        NetworkCapabilities networkCapabilities2 = new NetworkCapabilities(networkCapabilities);
        enforceNetworkRequestPermissions(networkCapabilities2);
        enforceMeteredApnPolicy(networkCapabilities2);
        ensureRequestableCapabilities(networkCapabilities2);
        ensureSufficientPermissionsForRequest(networkCapabilities2, Binder.getCallingPid(), Binder.getCallingUid());
        ensureValidNetworkSpecifier(networkCapabilities2);
        restrictRequestUidsForCaller(networkCapabilities2);
        NetworkRequest networkRequest = new NetworkRequest(networkCapabilities2, -1, nextNetworkRequestId(), NetworkRequest.Type.REQUEST);
        NetworkRequestInfo networkRequestInfo = new NetworkRequestInfo(networkRequest, pendingIntent);
        log("pendingRequest for " + networkRequestInfo);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(26, networkRequestInfo));
        return networkRequest;
    }

    private void releasePendingNetworkRequestWithDelay(PendingIntent pendingIntent) {
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(EVENT_RELEASE_NETWORK_REQUEST_WITH_INTENT, getCallingUid(), 0, pendingIntent), this.mReleasePendingIntentDelayMs);
    }

    public void releasePendingNetworkRequest(PendingIntent pendingIntent) {
        Preconditions.checkNotNull(pendingIntent, "PendingIntent cannot be null.");
        this.mHandler.sendMessage(this.mHandler.obtainMessage(EVENT_RELEASE_NETWORK_REQUEST_WITH_INTENT, getCallingUid(), 0, pendingIntent));
    }

    private boolean hasWifiNetworkListenPermission(NetworkCapabilities networkCapabilities) {
        if (networkCapabilities == null) {
            return false;
        }
        int[] transportTypes = networkCapabilities.getTransportTypes();
        if (transportTypes.length != 1 || transportTypes[0] != 1) {
            return false;
        }
        try {
            this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_WIFI_STATE", "ConnectivityService");
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }

    public NetworkRequest listenForNetwork(NetworkCapabilities networkCapabilities, Messenger messenger, IBinder iBinder) {
        if (!hasWifiNetworkListenPermission(networkCapabilities)) {
            enforceAccessPermission();
        }
        NetworkCapabilities networkCapabilities2 = new NetworkCapabilities(networkCapabilities);
        ensureSufficientPermissionsForRequest(networkCapabilities, Binder.getCallingPid(), Binder.getCallingUid());
        restrictRequestUidsForCaller(networkCapabilities2);
        restrictBackgroundRequestForCaller(networkCapabilities2);
        ensureValidNetworkSpecifier(networkCapabilities2);
        NetworkRequest networkRequest = new NetworkRequest(networkCapabilities2, -1, nextNetworkRequestId(), NetworkRequest.Type.LISTEN);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(21, new NetworkRequestInfo(messenger, networkRequest, iBinder)));
        return networkRequest;
    }

    public void pendingListenForNetwork(NetworkCapabilities networkCapabilities, PendingIntent pendingIntent) {
        Preconditions.checkNotNull(pendingIntent, "PendingIntent cannot be null.");
        if (!hasWifiNetworkListenPermission(networkCapabilities)) {
            enforceAccessPermission();
        }
        ensureValidNetworkSpecifier(networkCapabilities);
        ensureSufficientPermissionsForRequest(networkCapabilities, Binder.getCallingPid(), Binder.getCallingUid());
        NetworkCapabilities networkCapabilities2 = new NetworkCapabilities(networkCapabilities);
        restrictRequestUidsForCaller(networkCapabilities2);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(21, new NetworkRequestInfo(new NetworkRequest(networkCapabilities2, -1, nextNetworkRequestId(), NetworkRequest.Type.LISTEN), pendingIntent)));
    }

    public void releaseNetworkRequest(NetworkRequest networkRequest) {
        ensureNetworkRequestHasType(networkRequest);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(22, getCallingUid(), 0, networkRequest));
    }

    public void registerNetworkFactory(Messenger messenger, String str) {
        enforceConnectivityInternalPermission();
        this.mHandler.sendMessage(this.mHandler.obtainMessage(17, new NetworkFactoryInfo(str, messenger, new AsyncChannel())));
    }

    private void handleRegisterNetworkFactory(NetworkFactoryInfo networkFactoryInfo) {
        log("Got NetworkFactory Messenger for " + networkFactoryInfo.name);
        this.mNetworkFactoryInfos.put(networkFactoryInfo.messenger, networkFactoryInfo);
        networkFactoryInfo.asyncChannel.connect(this.mContext, this.mTrackerHandler, networkFactoryInfo.messenger);
    }

    public void unregisterNetworkFactory(Messenger messenger) {
        enforceConnectivityInternalPermission();
        this.mHandler.sendMessage(this.mHandler.obtainMessage(23, messenger));
    }

    private void handleUnregisterNetworkFactory(Messenger messenger) {
        NetworkFactoryInfo networkFactoryInfoRemove = this.mNetworkFactoryInfos.remove(messenger);
        if (networkFactoryInfoRemove == null) {
            loge("Failed to find Messenger in unregisterNetworkFactory");
            return;
        }
        log("unregisterNetworkFactory for " + networkFactoryInfoRemove.name);
    }

    private NetworkAgentInfo getNetworkForRequest(int i) {
        NetworkAgentInfo networkAgentInfo;
        synchronized (this.mNetworkForRequestId) {
            networkAgentInfo = this.mNetworkForRequestId.get(i);
        }
        return networkAgentInfo;
    }

    private void clearNetworkForRequest(int i) {
        synchronized (this.mNetworkForRequestId) {
            this.mNetworkForRequestId.remove(i);
        }
    }

    private void setNetworkForRequest(int i, NetworkAgentInfo networkAgentInfo) {
        synchronized (this.mNetworkForRequestId) {
            this.mNetworkForRequestId.put(i, networkAgentInfo);
        }
    }

    private NetworkAgentInfo getDefaultNetwork() {
        return getNetworkForRequest(this.mDefaultRequest.requestId);
    }

    private boolean isDefaultNetwork(NetworkAgentInfo networkAgentInfo) {
        return networkAgentInfo == getDefaultNetwork();
    }

    private boolean isDefaultRequest(NetworkRequestInfo networkRequestInfo) {
        return networkRequestInfo.request.requestId == this.mDefaultRequest.requestId;
    }

    public int registerNetworkAgent(Messenger messenger, NetworkInfo networkInfo, LinkProperties linkProperties, NetworkCapabilities networkCapabilities, int i, NetworkMisc networkMisc) {
        enforceConnectivityInternalPermission();
        LinkProperties linkProperties2 = new LinkProperties(linkProperties);
        linkProperties2.ensureDirectlyConnectedRoutes();
        NetworkCapabilities networkCapabilities2 = new NetworkCapabilities(networkCapabilities);
        NetworkAgentInfo networkAgentInfo = new NetworkAgentInfo(messenger, new AsyncChannel(), new Network(reserveNetId()), new NetworkInfo(networkInfo), linkProperties2, networkCapabilities2, i, this.mContext, this.mTrackerHandler, new NetworkMisc(networkMisc), this.mDefaultRequest, this);
        networkAgentInfo.networkCapabilities = mixInCapabilities(networkAgentInfo, networkCapabilities2);
        synchronized (this) {
            networkAgentInfo.networkMonitor.systemReady = this.mSystemReady;
        }
        String extraInfo = networkInfo.getExtraInfo();
        if (TextUtils.isEmpty(extraInfo)) {
            extraInfo = networkAgentInfo.networkCapabilities.getSSID();
        }
        addValidationLogs(networkAgentInfo.networkMonitor.getValidationLogs(), networkAgentInfo.network, extraInfo);
        log("registerNetworkAgent " + networkAgentInfo);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(18, networkAgentInfo));
        return networkAgentInfo.network.netId;
    }

    private void handleRegisterNetworkAgent(NetworkAgentInfo networkAgentInfo) {
        this.mNetworkAgentInfos.put(networkAgentInfo.messenger, networkAgentInfo);
        synchronized (this.mNetworkForNetId) {
            this.mNetworkForNetId.put(networkAgentInfo.network.netId, networkAgentInfo);
        }
        networkAgentInfo.asyncChannel.connect(this.mContext, this.mTrackerHandler, networkAgentInfo.messenger);
        NetworkInfo networkInfo = networkAgentInfo.networkInfo;
        networkAgentInfo.networkInfo = null;
        updateNetworkInfo(networkAgentInfo, networkInfo);
        updateUids(networkAgentInfo, null, networkAgentInfo.networkCapabilities);
    }

    private void updateLinkProperties(NetworkAgentInfo networkAgentInfo, LinkProperties linkProperties) {
        LinkProperties linkProperties2 = new LinkProperties(networkAgentInfo.linkProperties);
        int i = networkAgentInfo.network.netId;
        if (networkAgentInfo.clatd != null) {
            networkAgentInfo.clatd.fixupLinkProperties(linkProperties, linkProperties2);
        }
        updateInterfaces(linkProperties2, linkProperties, i, networkAgentInfo.networkCapabilities);
        updateMtu(linkProperties2, linkProperties);
        updateTcpBufferSizes(networkAgentInfo);
        updateRoutes(linkProperties2, linkProperties, i);
        updateDnses(linkProperties2, linkProperties, i);
        this.mDnsManager.updatePrivateDnsStatus(i, linkProperties2);
        networkAgentInfo.updateClat(this.mNetd);
        if (isDefaultNetwork(networkAgentInfo)) {
            handleApplyDefaultProxy(linkProperties2.getHttpProxy());
        } else {
            updateProxy(linkProperties2, linkProperties, networkAgentInfo);
        }
        if (!Objects.equals(linkProperties2, linkProperties)) {
            synchronized (networkAgentInfo) {
                networkAgentInfo.linkProperties = linkProperties2;
            }
            notifyIfacesChangedForNetworkStats();
            notifyNetworkCallbacks(networkAgentInfo, 524295);
        }
        this.mKeepaliveTracker.handleCheckKeepalivesStillValid(networkAgentInfo);
    }

    private void wakeupModifyInterface(String str, NetworkCapabilities networkCapabilities, boolean z) {
        if (!networkCapabilities.hasTransport(1)) {
            return;
        }
        int integer = this.mContext.getResources().getInteger(R.integer.config_delay_for_ims_dereg_millis);
        int integer2 = this.mContext.getResources().getInteger(R.integer.config_demo_pointing_aligned_duration_millis);
        if (integer == 0 || integer2 == 0) {
            return;
        }
        String str2 = "iface:" + str;
        try {
            if (z) {
                this.mNetd.getNetdService().wakeupAddInterface(str, str2, integer, integer2);
            } else {
                this.mNetd.getNetdService().wakeupDelInterface(str, str2, integer, integer2);
            }
        } catch (Exception e) {
            loge("Exception modifying wakeup packet monitoring: " + e);
        }
    }

    private void updateInterfaces(LinkProperties linkProperties, LinkProperties linkProperties2, int i, NetworkCapabilities networkCapabilities) {
        LinkProperties.CompareResult compareResult = new LinkProperties.CompareResult(linkProperties2 != null ? linkProperties2.getAllInterfaceNames() : null, linkProperties != null ? linkProperties.getAllInterfaceNames() : null);
        for (String str : compareResult.added) {
            try {
                log("Adding iface " + str + " to network " + i);
                this.mNetd.addInterfaceToNetwork(str, i);
                wakeupModifyInterface(str, networkCapabilities, true);
            } catch (Exception e) {
                loge("Exception adding interface: " + e);
            }
        }
        for (String str2 : compareResult.removed) {
            try {
                log("Removing iface " + str2 + " from network " + i);
                wakeupModifyInterface(str2, networkCapabilities, false);
                this.mNetd.removeInterfaceFromNetwork(str2, i);
            } catch (Exception e2) {
                loge("Exception removing interface: " + e2);
            }
        }
    }

    private boolean updateRoutes(LinkProperties linkProperties, LinkProperties linkProperties2, int i) {
        LinkProperties.CompareResult compareResult = new LinkProperties.CompareResult(linkProperties2 != null ? linkProperties2.getAllRoutes() : null, linkProperties != null ? linkProperties.getAllRoutes() : null);
        for (RouteInfo routeInfo : compareResult.added) {
            if (!routeInfo.hasGateway()) {
                try {
                    this.mNetd.addRoute(i, routeInfo);
                } catch (Exception e) {
                    if (routeInfo.getDestination().getAddress() instanceof Inet4Address) {
                        loge("Exception in addRoute for non-gateway: " + e);
                    }
                }
            }
        }
        for (RouteInfo routeInfo2 : compareResult.added) {
            if (routeInfo2.hasGateway()) {
                try {
                    this.mNetd.addRoute(i, routeInfo2);
                } catch (Exception e2) {
                    if (routeInfo2.getGateway() instanceof Inet4Address) {
                        loge("Exception in addRoute for gateway: " + e2);
                    }
                }
            }
        }
        Iterator it = compareResult.removed.iterator();
        while (it.hasNext()) {
            try {
                this.mNetd.removeRoute(i, (RouteInfo) it.next());
            } catch (Exception e3) {
                loge("Exception in removeRoute: " + e3);
            }
        }
        return (compareResult.added.isEmpty() && compareResult.removed.isEmpty()) ? false : true;
    }

    private void updateDnses(LinkProperties linkProperties, LinkProperties linkProperties2, int i) {
        if (linkProperties2 != null && linkProperties.isIdenticalDnses(linkProperties2)) {
            return;
        }
        NetworkAgentInfo defaultNetwork = getDefaultNetwork();
        boolean z = defaultNetwork != null && defaultNetwork.network.netId == i;
        log("Setting DNS servers for network " + i + " to " + linkProperties.getDnsServers());
        try {
            this.mDnsManager.setDnsConfigurationForNetwork(i, linkProperties, z);
        } catch (Exception e) {
            loge("Exception in setDnsConfigurationForNetwork: " + e);
        }
    }

    private String getNetworkPermission(NetworkCapabilities networkCapabilities) {
        if (!networkCapabilities.hasCapability(13)) {
            return "SYSTEM";
        }
        if (!networkCapabilities.hasCapability(19)) {
            return "NETWORK";
        }
        return null;
    }

    private NetworkCapabilities mixInCapabilities(NetworkAgentInfo networkAgentInfo, NetworkCapabilities networkCapabilities) {
        if (networkAgentInfo.everConnected && !networkAgentInfo.isVPN() && !networkAgentInfo.networkCapabilities.satisfiedByImmutableNetworkCapabilities(networkCapabilities)) {
            String strDescribeImmutableDifferences = networkAgentInfo.networkCapabilities.describeImmutableDifferences(networkCapabilities);
            if (!TextUtils.isEmpty(strDescribeImmutableDifferences)) {
                Slog.wtf(TAG, "BUG: " + networkAgentInfo + " lost immutable capabilities:" + strDescribeImmutableDifferences);
            }
        }
        NetworkCapabilities networkCapabilities2 = new NetworkCapabilities(networkCapabilities);
        if (networkAgentInfo.lastValidated) {
            networkCapabilities2.addCapability(16);
        } else {
            networkCapabilities2.removeCapability(16);
        }
        if (networkAgentInfo.lastCaptivePortalDetected) {
            networkCapabilities2.addCapability(17);
        } else {
            networkCapabilities2.removeCapability(17);
        }
        if (networkAgentInfo.isBackgroundNetwork()) {
            networkCapabilities2.removeCapability(19);
        } else {
            networkCapabilities2.addCapability(19);
        }
        if (networkAgentInfo.isSuspended()) {
            networkCapabilities2.removeCapability(21);
        } else {
            networkCapabilities2.addCapability(21);
        }
        return networkCapabilities2;
    }

    private void updateCapabilities(int i, NetworkAgentInfo networkAgentInfo, NetworkCapabilities networkCapabilities) {
        NetworkCapabilities networkCapabilities2;
        NetworkCapabilities networkCapabilitiesMixInCapabilities = mixInCapabilities(networkAgentInfo, networkCapabilities);
        if (Objects.equals(networkAgentInfo.networkCapabilities, networkCapabilitiesMixInCapabilities)) {
            return;
        }
        String networkPermission = getNetworkPermission(networkAgentInfo.networkCapabilities);
        String networkPermission2 = getNetworkPermission(networkCapabilitiesMixInCapabilities);
        if (!Objects.equals(networkPermission, networkPermission2) && networkAgentInfo.created && !networkAgentInfo.isVPN()) {
            try {
                this.mNetd.setNetworkPermission(networkAgentInfo.network.netId, networkPermission2);
            } catch (RemoteException e) {
                loge("Exception in setNetworkPermission: " + e);
            }
        }
        synchronized (networkAgentInfo) {
            networkCapabilities2 = networkAgentInfo.networkCapabilities;
            networkAgentInfo.networkCapabilities = networkCapabilitiesMixInCapabilities;
        }
        updateUids(networkAgentInfo, networkCapabilities2, networkCapabilitiesMixInCapabilities);
        if (networkAgentInfo.getCurrentScore() == i && networkCapabilitiesMixInCapabilities.equalRequestableCapabilities(networkCapabilities2)) {
            processListenRequests(networkAgentInfo, true);
        } else {
            rematchAllNetworksAndRequests(networkAgentInfo, i);
            notifyNetworkCallbacks(networkAgentInfo, 524294);
        }
        if (networkCapabilities2 != null) {
            boolean z = networkCapabilities2.hasCapability(11) != networkCapabilitiesMixInCapabilities.hasCapability(11);
            boolean z2 = networkCapabilities2.hasCapability(18) != networkCapabilitiesMixInCapabilities.hasCapability(18);
            if (z || z2) {
                notifyIfacesChangedForNetworkStats();
            }
        }
        if (!networkCapabilitiesMixInCapabilities.hasTransport(4)) {
            updateAllVpnsCapabilities();
        }
    }

    private void updateUids(NetworkAgentInfo networkAgentInfo, NetworkCapabilities networkCapabilities, NetworkCapabilities networkCapabilities2) {
        Set uids;
        if (networkCapabilities != null) {
            uids = networkCapabilities.getUids();
        } else {
            uids = null;
        }
        Set uids2 = networkCapabilities2 != null ? networkCapabilities2.getUids() : null;
        if (uids == null) {
            uids = new ArraySet();
        }
        if (uids2 == null) {
            uids2 = new ArraySet();
        }
        ArraySet arraySet = new ArraySet(uids);
        uids.removeAll(uids2);
        uids2.removeAll(arraySet);
        try {
            if (!uids2.isEmpty()) {
                UidRange[] uidRangeArr = new UidRange[uids2.size()];
                uids2.toArray(uidRangeArr);
                this.mNetd.addVpnUidRanges(networkAgentInfo.network.netId, uidRangeArr);
            }
            if (!uids.isEmpty()) {
                UidRange[] uidRangeArr2 = new UidRange[uids.size()];
                uids.toArray(uidRangeArr2);
                this.mNetd.removeVpnUidRanges(networkAgentInfo.network.netId, uidRangeArr2);
            }
        } catch (Exception e) {
            loge("Exception in updateUids: " + e);
        }
    }

    public void handleUpdateLinkProperties(NetworkAgentInfo networkAgentInfo, LinkProperties linkProperties) {
        if (getNetworkAgentInfoForNetId(networkAgentInfo.network.netId) != networkAgentInfo) {
            return;
        }
        linkProperties.ensureDirectlyConnectedRoutes();
        LinkProperties linkProperties2 = networkAgentInfo.linkProperties;
        synchronized (networkAgentInfo) {
            networkAgentInfo.linkProperties = linkProperties;
        }
        if (networkAgentInfo.everConnected) {
            updateLinkProperties(networkAgentInfo, linkProperties2);
        }
    }

    private void sendUpdatedScoreToFactories(NetworkAgentInfo networkAgentInfo) {
        for (int i = 0; i < networkAgentInfo.numNetworkRequests(); i++) {
            NetworkRequest networkRequestRequestAt = networkAgentInfo.requestAt(i);
            if (!networkRequestRequestAt.isListen()) {
                sendUpdatedScoreToFactories(networkRequestRequestAt, networkAgentInfo.getCurrentScore());
            }
        }
    }

    private void sendUpdatedScoreToFactories(NetworkRequest networkRequest, int i) {
        Iterator<NetworkFactoryInfo> it = this.mNetworkFactoryInfos.values().iterator();
        while (it.hasNext()) {
            it.next().asyncChannel.sendMessage(536576, i, 0, networkRequest);
        }
    }

    private void sendPendingIntentForRequest(NetworkRequestInfo networkRequestInfo, NetworkAgentInfo networkAgentInfo, int i) {
        if (i == 524290 && !networkRequestInfo.mPendingIntentSent) {
            Intent intent = new Intent();
            intent.putExtra("android.net.extra.NETWORK", networkAgentInfo.network);
            intent.putExtra("android.net.extra.NETWORK_REQUEST", networkRequestInfo.request);
            networkRequestInfo.mPendingIntentSent = true;
            sendIntent(networkRequestInfo.mPendingIntent, intent);
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
    public void onSendFinished(PendingIntent pendingIntent, Intent intent, int i, String str, Bundle bundle) {
        log("Finished sending " + pendingIntent);
        this.mPendingIntentWakeLock.release();
        releasePendingNetworkRequestWithDelay(pendingIntent);
    }

    private void callCallbackForRequest(NetworkRequestInfo networkRequestInfo, NetworkAgentInfo networkAgentInfo, int i, int i2) {
        if (networkRequestInfo.messenger == null) {
            return;
        }
        Bundle bundle = new Bundle();
        putParcelable(bundle, new NetworkRequest(networkRequestInfo.request));
        Message messageObtain = Message.obtain();
        if (i != 524293) {
            putParcelable(bundle, networkAgentInfo.network);
        }
        switch (i) {
            case 524290:
                putParcelable(bundle, networkCapabilitiesRestrictedForCallerPermissions(networkAgentInfo.networkCapabilities, networkRequestInfo.mPid, networkRequestInfo.mUid));
                putParcelable(bundle, new LinkProperties(networkAgentInfo.linkProperties));
                break;
            case 524291:
                messageObtain.arg1 = i2;
                break;
            case 524294:
                putParcelable(bundle, networkCapabilitiesRestrictedForCallerPermissions(networkAgentInfo.networkCapabilities, networkRequestInfo.mPid, networkRequestInfo.mUid));
                break;
            case 524295:
                putParcelable(bundle, new LinkProperties(networkAgentInfo.linkProperties));
                break;
        }
        messageObtain.what = i;
        messageObtain.setData(bundle);
        try {
            networkRequestInfo.messenger.send(messageObtain);
        } catch (RemoteException e) {
            loge("RemoteException caught trying to send a callback msg for " + networkRequestInfo.request);
        }
    }

    private static <T extends Parcelable> void putParcelable(Bundle bundle, T t) {
        bundle.putParcelable(t.getClass().getSimpleName(), t);
    }

    private void teardownUnneededNetwork(NetworkAgentInfo networkAgentInfo) {
        if (networkAgentInfo.numRequestNetworkRequests() != 0) {
            int i = 0;
            while (true) {
                if (i >= networkAgentInfo.numNetworkRequests()) {
                    break;
                }
                NetworkRequest networkRequestRequestAt = networkAgentInfo.requestAt(i);
                if (networkRequestRequestAt.isListen()) {
                    i++;
                } else {
                    loge("Dead network still had at least " + networkRequestRequestAt);
                    break;
                }
            }
        }
        networkAgentInfo.asyncChannel.disconnect();
    }

    private void handleLingerComplete(NetworkAgentInfo networkAgentInfo) {
        if (networkAgentInfo == null) {
            loge("Unknown NetworkAgentInfo in handleLingerComplete");
            return;
        }
        log("handleLingerComplete for " + networkAgentInfo.name());
        networkAgentInfo.clearLingerState();
        if (unneeded(networkAgentInfo, UnneededFor.TEARDOWN)) {
            teardownUnneededNetwork(networkAgentInfo);
        } else {
            updateCapabilities(networkAgentInfo.getCurrentScore(), networkAgentInfo, networkAgentInfo.networkCapabilities);
        }
    }

    private void makeDefault(NetworkAgentInfo networkAgentInfo) {
        log("Switching to new default network: " + networkAgentInfo);
        setupDataActivityTracking(networkAgentInfo);
        try {
            this.mNetd.setDefaultNetId(networkAgentInfo.network.netId);
        } catch (Exception e) {
            loge("Exception setting default network :" + e);
        }
        notifyLockdownVpn(networkAgentInfo);
        handleApplyDefaultProxy(networkAgentInfo.linkProperties.getHttpProxy());
        updateTcpBufferSizes(networkAgentInfo);
        this.mDnsManager.setDefaultDnsSystemProperties(networkAgentInfo.linkProperties.getDnsServers());
        notifyIfacesChangedForNetworkStats();
    }

    private void processListenRequests(NetworkAgentInfo networkAgentInfo, boolean z) {
        for (NetworkRequestInfo networkRequestInfo : this.mNetworkRequests.values()) {
            NetworkRequest networkRequest = networkRequestInfo.request;
            if (networkRequest.isListen() && networkAgentInfo.isSatisfyingRequest(networkRequest.requestId) && !networkAgentInfo.satisfies(networkRequest)) {
                networkAgentInfo.removeRequest(networkRequestInfo.request.requestId);
                callCallbackForRequest(networkRequestInfo, networkAgentInfo, 524292, 0);
            }
        }
        if (z) {
            notifyNetworkCallbacks(networkAgentInfo, 524294);
        }
        for (NetworkRequestInfo networkRequestInfo2 : this.mNetworkRequests.values()) {
            NetworkRequest networkRequest2 = networkRequestInfo2.request;
            if (networkRequest2.isListen() && networkAgentInfo.satisfies(networkRequest2) && !networkAgentInfo.isSatisfyingRequest(networkRequest2.requestId)) {
                networkAgentInfo.addRequest(networkRequest2);
                notifyNetworkAvailable(networkAgentInfo, networkRequestInfo2);
            }
        }
    }

    private void rematchNetworkAndRequests(NetworkAgentInfo networkAgentInfo, ReapUnvalidatedNetworks reapUnvalidatedNetworks, long j) {
        NetworkAgentInfo networkAgentInfo2;
        long j2;
        NetworkAgentInfo networkAgentInfo3;
        Iterator<NetworkRequestInfo> it;
        boolean z;
        NetworkCapabilities networkCapabilities;
        int i;
        int i2;
        NetworkAgentInfo networkAgentInfo4;
        int i3;
        boolean z2;
        NetworkRequestInfo networkRequestInfo;
        long j3 = j;
        if (networkAgentInfo.everConnected) {
            boolean zIsVPN = networkAgentInfo.isVPN();
            boolean zIsBackgroundNetwork = networkAgentInfo.isBackgroundNetwork();
            int currentScore = networkAgentInfo.getCurrentScore();
            ArrayList arrayList = new ArrayList();
            ArrayList arrayList2 = new ArrayList();
            NetworkCapabilities networkCapabilities2 = networkAgentInfo.networkCapabilities;
            Iterator<NetworkRequestInfo> it2 = this.mNetworkRequests.values().iterator();
            NetworkAgentInfo networkAgentInfo5 = null;
            boolean z3 = zIsVPN;
            boolean z4 = false;
            while (it2.hasNext()) {
                NetworkRequestInfo next = it2.next();
                if (!next.request.isListen()) {
                    NetworkAgentInfo networkForRequest = getNetworkForRequest(next.request.requestId);
                    boolean zSatisfies = networkAgentInfo.satisfies(next.request);
                    if (networkAgentInfo == networkForRequest && zSatisfies) {
                        z3 = true;
                    } else if (!zSatisfies) {
                        networkAgentInfo3 = networkAgentInfo5;
                        it = it2;
                        z = zIsBackgroundNetwork;
                        networkCapabilities = networkCapabilities2;
                        i = currentScore;
                        if (networkAgentInfo.isSatisfyingRequest(next.request.requestId)) {
                            log("Network " + networkAgentInfo.name() + " stopped satisfying request " + next.request.requestId);
                            networkAgentInfo.removeRequest(next.request.requestId);
                            if (networkForRequest == networkAgentInfo) {
                                clearNetworkForRequest(next.request.requestId);
                                i2 = 0;
                                sendUpdatedScoreToFactories(next.request, 0);
                            } else {
                                i2 = 0;
                                Slog.wtf(TAG, "BUG: Removing request " + next.request.requestId + " from " + networkAgentInfo.name() + " without updating mNetworkForRequestId or factories!");
                            }
                            callCallbackForRequest(next, networkAgentInfo, 524292, i2);
                        }
                        currentScore = i;
                        it2 = it;
                        zIsBackgroundNetwork = z;
                        networkCapabilities2 = networkCapabilities;
                        networkAgentInfo5 = networkAgentInfo3;
                        j3 = j;
                    } else if (networkForRequest == null || networkForRequest.getCurrentScore() < currentScore) {
                        if (networkForRequest != null) {
                            networkForRequest.removeRequest(next.request.requestId);
                            z = zIsBackgroundNetwork;
                            i3 = currentScore;
                            networkCapabilities = networkCapabilities2;
                            networkRequestInfo = next;
                            networkAgentInfo3 = networkAgentInfo5;
                            long j4 = j3;
                            it = it2;
                            networkAgentInfo4 = networkForRequest;
                            z2 = true;
                            networkForRequest.lingerRequest(next.request, j4, this.mLingerDelayMs);
                            arrayList.add(networkAgentInfo4);
                        } else {
                            networkAgentInfo4 = networkForRequest;
                            networkAgentInfo3 = networkAgentInfo5;
                            it = it2;
                            z = zIsBackgroundNetwork;
                            i3 = currentScore;
                            networkCapabilities = networkCapabilities2;
                            z2 = true;
                            networkRequestInfo = next;
                        }
                        networkAgentInfo.unlingerRequest(networkRequestInfo.request);
                        setNetworkForRequest(networkRequestInfo.request.requestId, networkAgentInfo);
                        if (!networkAgentInfo.addRequest(networkRequestInfo.request)) {
                            Slog.wtf(TAG, "BUG: " + networkAgentInfo.name() + " already has " + networkRequestInfo.request);
                        }
                        arrayList2.add(networkRequestInfo);
                        i = i3;
                        sendUpdatedScoreToFactories(networkRequestInfo.request, i);
                        if (isDefaultRequest(networkRequestInfo)) {
                            if (networkAgentInfo4 != null) {
                                this.mLingerMonitor.noteLingerDefaultNetwork(networkAgentInfo4, networkAgentInfo);
                            }
                            networkAgentInfo3 = networkAgentInfo4;
                            z4 = z2;
                            z3 = z4;
                        } else {
                            z3 = z2;
                        }
                        currentScore = i;
                        it2 = it;
                        zIsBackgroundNetwork = z;
                        networkCapabilities2 = networkCapabilities;
                        networkAgentInfo5 = networkAgentInfo3;
                        j3 = j;
                    } else {
                        networkAgentInfo3 = networkAgentInfo5;
                        it = it2;
                        z = zIsBackgroundNetwork;
                        i = currentScore;
                        networkCapabilities = networkCapabilities2;
                        currentScore = i;
                        it2 = it;
                        zIsBackgroundNetwork = z;
                        networkCapabilities2 = networkCapabilities;
                        networkAgentInfo5 = networkAgentInfo3;
                        j3 = j;
                    }
                }
            }
            NetworkAgentInfo networkAgentInfo6 = networkAgentInfo5;
            boolean z5 = zIsBackgroundNetwork;
            int i4 = currentScore;
            NetworkCapabilities networkCapabilities3 = networkCapabilities2;
            if (z4) {
                makeDefault(networkAgentInfo);
                networkAgentInfo2 = networkAgentInfo6;
                j2 = j;
                metricsLogger().defaultNetworkMetrics().logDefaultNetworkEvent(j2, networkAgentInfo, networkAgentInfo2);
                scheduleReleaseNetworkTransitionWakelock();
            } else {
                networkAgentInfo2 = networkAgentInfo6;
                j2 = j;
            }
            if (!networkAgentInfo.networkCapabilities.equalRequestableCapabilities(networkCapabilities3)) {
                Slog.wtf(TAG, String.format("BUG: %s changed requestable capabilities during rematch: %s -> %s", networkAgentInfo.name(), networkCapabilities3, networkAgentInfo.networkCapabilities));
            }
            if (networkAgentInfo.getCurrentScore() != i4) {
                Slog.wtf(TAG, String.format("BUG: %s changed score during rematch: %d -> %d", networkAgentInfo.name(), Integer.valueOf(i4), Integer.valueOf(networkAgentInfo.getCurrentScore())));
            }
            if (z5 != networkAgentInfo.isBackgroundNetwork()) {
                updateCapabilities(i4, networkAgentInfo, networkAgentInfo.networkCapabilities);
            } else {
                processListenRequests(networkAgentInfo, false);
            }
            Iterator it3 = arrayList2.iterator();
            while (it3.hasNext()) {
                notifyNetworkAvailable(networkAgentInfo, (NetworkRequestInfo) it3.next());
            }
            Iterator it4 = arrayList.iterator();
            while (it4.hasNext()) {
                updateLingerState((NetworkAgentInfo) it4.next(), j2);
            }
            updateLingerState(networkAgentInfo, j2);
            if (z4) {
                if (networkAgentInfo2 != null) {
                    this.mLegacyTypeTracker.remove(networkAgentInfo2.networkInfo.getType(), networkAgentInfo2, true);
                }
                this.mDefaultInetConditionPublished = networkAgentInfo.lastValidated ? 100 : 0;
                this.mLegacyTypeTracker.add(networkAgentInfo.networkInfo.getType(), networkAgentInfo);
                notifyLockdownVpn(networkAgentInfo);
            }
            if (z3) {
                try {
                    IBatteryStats service = BatteryStatsService.getService();
                    int type = networkAgentInfo.networkInfo.getType();
                    service.noteNetworkInterfaceType(networkAgentInfo.linkProperties.getInterfaceName(), type);
                    Iterator it5 = networkAgentInfo.linkProperties.getStackedLinks().iterator();
                    while (it5.hasNext()) {
                        service.noteNetworkInterfaceType(((LinkProperties) it5.next()).getInterfaceName(), type);
                    }
                } catch (RemoteException e) {
                }
                for (int i5 = 0; i5 < networkAgentInfo.numNetworkRequests(); i5++) {
                    NetworkRequest networkRequestRequestAt = networkAgentInfo.requestAt(i5);
                    if (networkRequestRequestAt.legacyType != -1 && networkRequestRequestAt.isRequest()) {
                        this.mLegacyTypeTracker.add(networkRequestRequestAt.legacyType, networkAgentInfo);
                    }
                }
                if (networkAgentInfo.isVPN()) {
                    this.mLegacyTypeTracker.add(17, networkAgentInfo);
                }
            }
            if (reapUnvalidatedNetworks == ReapUnvalidatedNetworks.REAP) {
                for (NetworkAgentInfo networkAgentInfo7 : this.mNetworkAgentInfos.values()) {
                    if (unneeded(networkAgentInfo7, UnneededFor.TEARDOWN)) {
                        if (networkAgentInfo7.getLingerExpiry() > 0) {
                            updateLingerState(networkAgentInfo7, j2);
                        } else {
                            log("Reaping " + networkAgentInfo7.name());
                            teardownUnneededNetwork(networkAgentInfo7);
                        }
                    }
                }
            }
        }
    }

    private void rematchAllNetworksAndRequests(NetworkAgentInfo networkAgentInfo, int i) {
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        if (networkAgentInfo != null && i < networkAgentInfo.getCurrentScore()) {
            rematchNetworkAndRequests(networkAgentInfo, ReapUnvalidatedNetworks.REAP, jElapsedRealtime);
            return;
        }
        NetworkAgentInfo[] networkAgentInfoArr = (NetworkAgentInfo[]) this.mNetworkAgentInfos.values().toArray(new NetworkAgentInfo[this.mNetworkAgentInfos.size()]);
        Arrays.sort(networkAgentInfoArr);
        int length = networkAgentInfoArr.length;
        for (int i2 = 0; i2 < length; i2++) {
            NetworkAgentInfo networkAgentInfo2 = networkAgentInfoArr[i2];
            rematchNetworkAndRequests(networkAgentInfo2, networkAgentInfo2 != networkAgentInfoArr[networkAgentInfoArr.length + (-1)] ? ReapUnvalidatedNetworks.DONT_REAP : ReapUnvalidatedNetworks.REAP, jElapsedRealtime);
        }
    }

    private void updateInetCondition(NetworkAgentInfo networkAgentInfo) {
        if (networkAgentInfo.everValidated && isDefaultNetwork(networkAgentInfo)) {
            int i = networkAgentInfo.lastValidated ? 100 : 0;
            if (i == this.mDefaultInetConditionPublished) {
                return;
            }
            this.mDefaultInetConditionPublished = i;
            sendInetConditionBroadcast(networkAgentInfo.networkInfo);
        }
    }

    private void notifyLockdownVpn(NetworkAgentInfo networkAgentInfo) {
        synchronized (this.mVpns) {
            if (this.mLockdownTracker != null) {
                if (networkAgentInfo != null && networkAgentInfo.isVPN()) {
                    this.mLockdownTracker.onVpnStateChanged(networkAgentInfo.networkInfo);
                } else {
                    this.mLockdownTracker.onNetworkInfoChanged();
                }
            }
        }
    }

    private void updateNetworkInfo(NetworkAgentInfo networkAgentInfo, NetworkInfo networkInfo) {
        NetworkInfo networkInfo2;
        int i;
        NetworkInfo.State state = networkInfo.getState();
        int currentScore = networkAgentInfo.getCurrentScore();
        synchronized (networkAgentInfo) {
            networkInfo2 = networkAgentInfo.networkInfo;
            networkAgentInfo.networkInfo = networkInfo;
        }
        notifyLockdownVpn(networkAgentInfo);
        StringBuilder sb = new StringBuilder();
        sb.append(networkAgentInfo.name());
        sb.append(" EVENT_NETWORK_INFO_CHANGED, going from ");
        sb.append(networkInfo2 == null ? "null" : networkInfo2.getState());
        sb.append(" to ");
        sb.append(state);
        log(sb.toString());
        if (!networkAgentInfo.created && (state == NetworkInfo.State.CONNECTED || (state == NetworkInfo.State.CONNECTING && networkAgentInfo.isVPN()))) {
            networkAgentInfo.networkCapabilities.addCapability(19);
            try {
                if (networkAgentInfo.isVPN()) {
                    this.mNetd.createVirtualNetwork(networkAgentInfo.network.netId, !networkAgentInfo.linkProperties.getDnsServers().isEmpty(), networkAgentInfo.networkMisc == null || !networkAgentInfo.networkMisc.allowBypass);
                } else {
                    this.mNetd.createPhysicalNetwork(networkAgentInfo.network.netId, getNetworkPermission(networkAgentInfo.networkCapabilities));
                }
                networkAgentInfo.created = true;
            } catch (Exception e) {
                loge("Error creating network " + networkAgentInfo.network.netId + ": " + e.getMessage());
                return;
            }
        }
        if (!networkAgentInfo.everConnected && state == NetworkInfo.State.CONNECTED) {
            networkAgentInfo.everConnected = true;
            handlePerNetworkPrivateDnsConfig(networkAgentInfo, this.mDnsManager.getPrivateDnsConfig());
            updateLinkProperties(networkAgentInfo, null);
            notifyIfacesChangedForNetworkStats();
            networkAgentInfo.networkMonitor.sendMessage(NetworkMonitor.CMD_NETWORK_CONNECTED);
            scheduleUnvalidatedPrompt(networkAgentInfo);
            if (networkAgentInfo.isVPN()) {
                synchronized (this.mProxyLock) {
                    if (!this.mDefaultProxyDisabled) {
                        this.mDefaultProxyDisabled = true;
                        if (this.mGlobalProxy == null && this.mDefaultProxy != null) {
                            sendProxyBroadcast(null);
                        }
                    }
                }
            }
            updateSignalStrengthThresholds(networkAgentInfo, "CONNECT", null);
            rematchNetworkAndRequests(networkAgentInfo, ReapUnvalidatedNetworks.REAP, SystemClock.elapsedRealtime());
            notifyNetworkCallbacks(networkAgentInfo, 524289);
            return;
        }
        if (state == NetworkInfo.State.DISCONNECTED) {
            networkAgentInfo.asyncChannel.disconnect();
            if (networkAgentInfo.isVPN()) {
                synchronized (this.mProxyLock) {
                    if (this.mDefaultProxyDisabled) {
                        this.mDefaultProxyDisabled = false;
                        if (this.mGlobalProxy == null && this.mDefaultProxy != null) {
                            sendProxyBroadcast(this.mDefaultProxy);
                        }
                    }
                }
                updateUids(networkAgentInfo, networkAgentInfo.networkCapabilities, null);
            }
            disconnectAndDestroyNetwork(networkAgentInfo);
            return;
        }
        if ((networkInfo2 != null && networkInfo2.getState() == NetworkInfo.State.SUSPENDED) || state == NetworkInfo.State.SUSPENDED) {
            if (networkAgentInfo.getCurrentScore() != currentScore) {
                rematchAllNetworksAndRequests(networkAgentInfo, currentScore);
            }
            updateCapabilities(networkAgentInfo.getCurrentScore(), networkAgentInfo, networkAgentInfo.networkCapabilities);
            if (state == NetworkInfo.State.SUSPENDED) {
                i = 524297;
            } else {
                i = 524298;
            }
            notifyNetworkCallbacks(networkAgentInfo, i);
            this.mLegacyTypeTracker.update(networkAgentInfo);
        }
    }

    private void updateNetworkScore(NetworkAgentInfo networkAgentInfo, int i) {
        if (i < 0) {
            loge("updateNetworkScore for " + networkAgentInfo.name() + " got a negative score (" + i + ").  Bumping score to min of 0");
            i = 0;
        }
        int currentScore = networkAgentInfo.getCurrentScore();
        networkAgentInfo.setCurrentScore(i);
        rematchAllNetworksAndRequests(networkAgentInfo, currentScore);
        sendUpdatedScoreToFactories(networkAgentInfo);
    }

    protected void notifyNetworkAvailable(NetworkAgentInfo networkAgentInfo, NetworkRequestInfo networkRequestInfo) {
        this.mHandler.removeMessages(20, networkRequestInfo);
        if (networkRequestInfo.mPendingIntent != null) {
            sendPendingIntentForRequest(networkRequestInfo, networkAgentInfo, 524290);
        } else {
            callCallbackForRequest(networkRequestInfo, networkAgentInfo, 524290, 0);
        }
    }

    private void sendLegacyNetworkBroadcast(NetworkAgentInfo networkAgentInfo, NetworkInfo.DetailedState detailedState, int i) {
        NetworkInfo networkInfo = new NetworkInfo(networkAgentInfo.networkInfo);
        networkInfo.setType(i);
        NetworkAgentInfo defaultNetwork = null;
        if (detailedState != NetworkInfo.DetailedState.DISCONNECTED) {
            networkInfo.setDetailedState(detailedState, null, networkInfo.getExtraInfo());
            sendConnectedBroadcast(networkInfo);
            return;
        }
        networkInfo.setDetailedState(detailedState, networkInfo.getReason(), networkInfo.getExtraInfo());
        Intent intent = new Intent("android.net.conn.CONNECTIVITY_CHANGE");
        intent.putExtra("networkInfo", networkInfo);
        intent.putExtra("networkType", networkInfo.getType());
        if (networkInfo.isFailover()) {
            intent.putExtra("isFailover", true);
            networkAgentInfo.networkInfo.setFailover(false);
        }
        if (networkInfo.getReason() != null) {
            intent.putExtra(PhoneWindowManager.SYSTEM_DIALOG_REASON_KEY, networkInfo.getReason());
        }
        if (networkInfo.getExtraInfo() != null) {
            intent.putExtra("extraInfo", networkInfo.getExtraInfo());
        }
        if (networkAgentInfo.isSatisfyingRequest(this.mDefaultRequest.requestId)) {
            defaultNetwork = getDefaultNetwork();
            if (defaultNetwork != null) {
                intent.putExtra("otherNetwork", defaultNetwork.networkInfo);
            } else {
                intent.putExtra("noConnectivity", true);
            }
        }
        intent.putExtra("inetCondition", this.mDefaultInetConditionPublished);
        sendStickyBroadcast(intent);
        if (defaultNetwork != null) {
            sendConnectedBroadcast(defaultNetwork.networkInfo);
        }
    }

    protected void notifyNetworkCallbacks(NetworkAgentInfo networkAgentInfo, int i, int i2) {
        for (int i3 = 0; i3 < networkAgentInfo.numNetworkRequests(); i3++) {
            NetworkRequestInfo networkRequestInfo = this.mNetworkRequests.get(networkAgentInfo.requestAt(i3));
            if (networkRequestInfo.mPendingIntent == null) {
                callCallbackForRequest(networkRequestInfo, networkAgentInfo, i, i2);
            } else {
                sendPendingIntentForRequest(networkRequestInfo, networkAgentInfo, i);
            }
        }
    }

    protected void notifyNetworkCallbacks(NetworkAgentInfo networkAgentInfo, int i) {
        notifyNetworkCallbacks(networkAgentInfo, i, 0);
    }

    private Network[] getDefaultNetworks() {
        ArrayList arrayList = new ArrayList();
        NetworkAgentInfo defaultNetwork = getDefaultNetwork();
        for (NetworkAgentInfo networkAgentInfo : this.mNetworkAgentInfos.values()) {
            if (networkAgentInfo.everConnected && (networkAgentInfo == defaultNetwork || networkAgentInfo.isVPN())) {
                arrayList.add(networkAgentInfo.network);
            }
        }
        return (Network[]) arrayList.toArray(new Network[0]);
    }

    private void notifyIfacesChangedForNetworkStats() {
        try {
            this.mStatsService.forceUpdateIfaces(getDefaultNetworks());
        } catch (Exception e) {
        }
    }

    public boolean addVpnAddress(String str, int i) {
        boolean zAddAddress;
        int userId = UserHandle.getUserId(Binder.getCallingUid());
        synchronized (this.mVpns) {
            throwIfLockdownEnabled();
            zAddAddress = this.mVpns.get(userId).addAddress(str, i);
        }
        return zAddAddress;
    }

    public boolean removeVpnAddress(String str, int i) {
        boolean zRemoveAddress;
        int userId = UserHandle.getUserId(Binder.getCallingUid());
        synchronized (this.mVpns) {
            throwIfLockdownEnabled();
            zRemoveAddress = this.mVpns.get(userId).removeAddress(str, i);
        }
        return zRemoveAddress;
    }

    public boolean setUnderlyingNetworksForVpn(Network[] networkArr) {
        boolean underlyingNetworks;
        int userId = UserHandle.getUserId(Binder.getCallingUid());
        synchronized (this.mVpns) {
            throwIfLockdownEnabled();
            underlyingNetworks = this.mVpns.get(userId).setUnderlyingNetworks(networkArr);
        }
        if (underlyingNetworks) {
            this.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    this.f$0.notifyIfacesChangedForNetworkStats();
                }
            });
        }
        return underlyingNetworks;
    }

    public String getCaptivePortalServerUrl() {
        enforceConnectivityInternalPermission();
        return NetworkMonitor.getCaptivePortalServerHttpUrl(this.mContext);
    }

    public void startNattKeepalive(Network network, int i, Messenger messenger, IBinder iBinder, String str, int i2, String str2) {
        enforceKeepalivePermission();
        this.mKeepaliveTracker.startNattKeepalive(getNetworkAgentInfoForNetwork(network), i, messenger, iBinder, str, i2, str2, 4500);
    }

    public void stopKeepalive(Network network, int i) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(528396, i, 0, network));
    }

    public void factoryReset() {
        enforceConnectivityInternalPermission();
        if (this.mUserManager.hasUserRestriction("no_network_reset")) {
            return;
        }
        int callingUserId = UserHandle.getCallingUserId();
        setAirplaneMode(false);
        if (!this.mUserManager.hasUserRestriction("no_config_tethering")) {
            String opPackageName = this.mContext.getOpPackageName();
            for (String str : getTetheredIfaces()) {
                untether(str, opPackageName);
                if (str.contains("rndis")) {
                    loge("stop tethering usb");
                    this.mTethering.stopTethering(1);
                }
            }
        }
        if (!this.mUserManager.hasUserRestriction("no_config_vpn")) {
            synchronized (this.mVpns) {
                String alwaysOnVpnPackage = getAlwaysOnVpnPackage(callingUserId);
                if (alwaysOnVpnPackage != null) {
                    setAlwaysOnVpnPackage(callingUserId, null, false);
                    setVpnPackageAuthorization(alwaysOnVpnPackage, callingUserId, false);
                }
                if (this.mLockdownEnabled && callingUserId == 0) {
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        this.mKeyStore.delete("LOCKDOWN_VPN");
                        this.mLockdownEnabled = false;
                        setLockdownTracker(null);
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    } catch (Throwable th) {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                        throw th;
                    }
                }
                VpnConfig vpnConfig = getVpnConfig(callingUserId);
                if (vpnConfig != null) {
                    if (!vpnConfig.legacy) {
                        setVpnPackageAuthorization(vpnConfig.user, callingUserId, false);
                        prepareVpn(null, "[Legacy VPN]", callingUserId);
                    } else {
                        prepareVpn("[Legacy VPN]", "[Legacy VPN]", callingUserId);
                    }
                }
            }
        }
        Settings.Global.putString(this.mContext.getContentResolver(), "network_avoid_bad_wifi", null);
    }

    public byte[] getNetworkWatchlistConfigHash() {
        NetworkWatchlistManager networkWatchlistManager = (NetworkWatchlistManager) this.mContext.getSystemService(NetworkWatchlistManager.class);
        if (networkWatchlistManager == null) {
            loge("Unable to get NetworkWatchlistManager");
            return null;
        }
        return networkWatchlistManager.getWatchlistConfigHash();
    }

    @VisibleForTesting
    public NetworkMonitor createNetworkMonitor(Context context, Handler handler, NetworkAgentInfo networkAgentInfo, NetworkRequest networkRequest) {
        return new NetworkMonitor(context, handler, networkAgentInfo, networkRequest);
    }

    @VisibleForTesting
    MultinetworkPolicyTracker createMultinetworkPolicyTracker(Context context, Handler handler, Runnable runnable) {
        return new MultinetworkPolicyTracker(context, handler, runnable);
    }

    @VisibleForTesting
    public WakeupMessage makeWakeupMessage(Context context, Handler handler, String str, int i, Object obj) {
        return new WakeupMessage(context, handler, str, i, 0, 0, obj);
    }

    @VisibleForTesting
    public boolean hasService(String str) {
        return ServiceManager.checkService(str) != null;
    }

    @VisibleForTesting
    protected IpConnectivityMetrics.Logger metricsLogger() {
        return (IpConnectivityMetrics.Logger) Preconditions.checkNotNull((IpConnectivityMetrics.Logger) LocalServices.getService(IpConnectivityMetrics.Logger.class), "no IpConnectivityMetrics service");
    }

    private void logNetworkEvent(NetworkAgentInfo networkAgentInfo, int i) {
        this.mMetricsLog.log(networkAgentInfo.network.netId, networkAgentInfo.networkCapabilities.getTransportTypes(), new NetworkEvent(i));
    }

    private static boolean toBool(int i) {
        return i != 0;
    }

    private static int encodeBool(boolean z) {
        return z ? 1 : 0;
    }

    public void onShellCommand(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, FileDescriptor fileDescriptor3, String[] strArr, ShellCallback shellCallback, ResultReceiver resultReceiver) {
        new ShellCmd().exec(this, fileDescriptor, fileDescriptor2, fileDescriptor3, strArr, shellCallback, resultReceiver);
    }

    private class ShellCmd extends ShellCommand {
        private ShellCmd() {
        }

        public int onCommand(String str) {
            if (str == null) {
                return handleDefaultCommands(str);
            }
            PrintWriter outPrintWriter = getOutPrintWriter();
            try {
                if (((str.hashCode() == 144736062 && str.equals("airplane-mode")) ? (byte) 0 : (byte) -1) == 0) {
                    String nextArg = getNextArg();
                    if ("enable".equals(nextArg)) {
                        ConnectivityService.this.setAirplaneMode(true);
                        return 0;
                    }
                    if ("disable".equals(nextArg)) {
                        ConnectivityService.this.setAirplaneMode(false);
                        return 0;
                    }
                    if (nextArg == null) {
                        outPrintWriter.println(Settings.Global.getInt(ConnectivityService.this.mContext.getContentResolver(), "airplane_mode_on") == 0 ? "disabled" : "enabled");
                        return 0;
                    }
                    onHelp();
                    return -1;
                }
                return handleDefaultCommands(str);
            } catch (Exception e) {
                outPrintWriter.println(e);
                return -1;
            }
        }

        public void onHelp() {
            PrintWriter outPrintWriter = getOutPrintWriter();
            outPrintWriter.println("Connectivity service commands:");
            outPrintWriter.println("  help");
            outPrintWriter.println("    Print this help text.");
            outPrintWriter.println("  airplane-mode [enable|disable]");
            outPrintWriter.println("    Turn airplane mode on or off.");
            outPrintWriter.println("  airplane-mode");
            outPrintWriter.println("    Get airplane mode.");
        }
    }
}
