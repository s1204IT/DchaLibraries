package com.android.server.wifi;

import android.R;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.DhcpResults;
import android.net.IpConfiguration;
import android.net.KeepalivePacketData;
import android.net.LinkProperties;
import android.net.MacAddress;
import android.net.Network;
import android.net.NetworkAgent;
import android.net.NetworkCapabilities;
import android.net.NetworkFactory;
import android.net.NetworkInfo;
import android.net.NetworkMisc;
import android.net.NetworkRequest;
import android.net.NetworkUtils;
import android.net.RouteInfo;
import android.net.TrafficStats;
import android.net.Uri;
import android.net.dhcp.DhcpClient;
import android.net.ip.IpClient;
import android.net.wifi.RssiPacketCountInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.hotspot2.IProvisioningCallback;
import android.net.wifi.hotspot2.OsuProvider;
import android.net.wifi.hotspot2.PasspointConfiguration;
import android.net.wifi.p2p.IWifiP2pManager;
import android.os.Bundle;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.WorkSource;
import android.provider.Settings;
import android.system.OsConstants;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.IBatteryStats;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.MessageUtils;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.wifi.ClientModeManager;
import com.android.server.wifi.ScoringParams;
import com.android.server.wifi.WifiMulticastLockManager;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.hotspot2.AnqpEvent;
import com.android.server.wifi.hotspot2.IconEvent;
import com.android.server.wifi.hotspot2.NetworkDetail;
import com.android.server.wifi.hotspot2.PasspointManager;
import com.android.server.wifi.hotspot2.WnmData;
import com.android.server.wifi.hotspot2.anqp.Constants;
import com.android.server.wifi.p2p.WifiP2pServiceImpl;
import com.android.server.wifi.scanner.ChannelHelper;
import com.android.server.wifi.util.NativeUtil;
import com.android.server.wifi.util.TelephonyUtil;
import com.android.server.wifi.util.WifiPermissionsUtil;
import com.android.server.wifi.util.WifiPermissionsWrapper;
import com.mediatek.server.wifi.MtkEapSimUtility;
import com.mediatek.server.wifi.MtkIpReachabilityLostMonitor;
import com.mediatek.server.wifi.MtkWfcUtility;
import com.mediatek.server.wifi.MtkWifiApmDelegate;
import com.mediatek.server.wifi.MtkWifiServiceAdapter;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class WifiStateMachine extends StateMachine {
    static final int BASE = 131072;
    static final int CMD_ACCEPT_UNVALIDATED = 131225;
    static final int CMD_ADD_OR_UPDATE_NETWORK = 131124;
    static final int CMD_ADD_OR_UPDATE_PASSPOINT_CONFIG = 131178;
    static final int CMD_ASSOCIATED_BSSID = 131219;
    static final int CMD_BLUETOOTH_ADAPTER_STATE_CHANGE = 131103;
    static final int CMD_BOOT_COMPLETED = 131206;
    static final int CMD_CONFIG_ND_OFFLOAD = 131276;
    private static final int CMD_DIAGS_CONNECT_TIMEOUT = 131324;
    static final int CMD_DISABLE_EPHEMERAL_NETWORK = 131170;
    public static final int CMD_DISABLE_P2P_REQ = 131204;
    public static final int CMD_DISABLE_P2P_RSP = 131205;
    static final int CMD_DISABLE_P2P_WATCHDOG_TIMER = 131184;
    static final int CMD_DISCONNECT = 131145;
    static final int CMD_DISCONNECTING_WATCHDOG_TIMER = 131168;
    static final int CMD_ENABLE_NETWORK = 131126;
    public static final int CMD_ENABLE_P2P = 131203;
    static final int CMD_ENABLE_RSSI_POLL = 131154;
    static final int CMD_ENABLE_TDLS = 131164;
    static final int CMD_ENABLE_WIFI_CONNECTIVITY_MANAGER = 131238;
    static final int CMD_GET_ALL_MATCHING_CONFIGS = 131240;
    static final int CMD_GET_CONFIGURED_NETWORKS = 131131;
    static final int CMD_GET_LINK_LAYER_STATS = 131135;
    static final int CMD_GET_MATCHING_CONFIG = 131171;
    static final int CMD_GET_MATCHING_OSU_PROVIDERS = 131181;
    static final int CMD_GET_PASSPOINT_CONFIGS = 131180;
    static final int CMD_GET_PRIVILEGED_CONFIGURED_NETWORKS = 131134;
    static final int CMD_GET_SUPPORTED_FEATURES = 131133;
    static final int CMD_INITIALIZE = 131207;
    static final int CMD_INSTALL_PACKET_FILTER = 131274;
    static final int CMD_IPV4_PROVISIONING_FAILURE = 131273;
    static final int CMD_IPV4_PROVISIONING_SUCCESS = 131272;
    static final int CMD_IP_CONFIGURATION_LOST = 131211;
    static final int CMD_IP_CONFIGURATION_SUCCESSFUL = 131210;
    static final int CMD_IP_REACHABILITY_LOST = 131221;
    static final int CMD_MATCH_PROVIDER_NETWORK = 131177;
    static final int CMD_NETWORK_STATUS = 131220;
    static final int CMD_QUERY_OSU_ICON = 131176;
    static final int CMD_READ_PACKET_FILTER = 131280;
    static final int CMD_REASSOCIATE = 131147;
    static final int CMD_RECONNECT = 131146;
    static final int CMD_RELOAD_TLS_AND_RECONNECT = 131214;
    static final int CMD_REMOVE_APP_CONFIGURATIONS = 131169;
    static final int CMD_REMOVE_NETWORK = 131125;
    static final int CMD_REMOVE_PASSPOINT_CONFIG = 131179;
    static final int CMD_REMOVE_USER_CONFIGURATIONS = 131224;
    static final int CMD_RESET_SIM_NETWORKS = 131173;
    static final int CMD_RESET_SUPPLICANT_STATE = 131183;
    static final int CMD_ROAM_WATCHDOG_TIMER = 131166;
    static final int CMD_RSSI_POLL = 131155;
    static final int CMD_RSSI_THRESHOLD_BREACHED = 131236;
    static final int CMD_SCREEN_STATE_CHANGED = 131167;
    static final int CMD_SET_FALLBACK_PACKET_FILTERING = 131275;
    static final int CMD_SET_HIGH_PERF_MODE = 131149;
    static final int CMD_SET_OPERATIONAL_MODE = 131144;
    static final int CMD_SET_SUSPEND_OPT_ENABLED = 131158;
    static final int CMD_START_CONNECT = 131215;
    static final int CMD_START_IP_PACKET_OFFLOAD = 131232;
    static final int CMD_START_ROAM = 131217;
    static final int CMD_START_RSSI_MONITORING_OFFLOAD = 131234;
    private static final int CMD_START_SUBSCRIPTION_PROVISIONING = 131326;
    static final int CMD_STATIC_IP_FAILURE = 131088;
    static final int CMD_STATIC_IP_SUCCESS = 131087;
    static final int CMD_STOP_IP_PACKET_OFFLOAD = 131233;
    static final int CMD_STOP_RSSI_MONITORING_OFFLOAD = 131235;
    static final int CMD_TARGET_BSSID = 131213;
    static final int CMD_TEST_NETWORK_DISCONNECT = 131161;
    static final int CMD_UNWANTED_NETWORK = 131216;
    static final int CMD_UPDATE_LINKPROPERTIES = 131212;
    static final int CMD_USER_STOP = 131279;
    static final int CMD_USER_SWITCH = 131277;
    static final int CMD_USER_UNLOCK = 131278;
    public static final int CONNECT_MODE = 1;
    private static final int DEFAULT_POLL_RSSI_INTERVAL_MSECS = 3000;
    private static final long DIAGS_CONNECT_TIMEOUT_MILLIS = 60000;
    public static final int DISABLED_MODE = 4;
    static final int DISABLE_P2P_GUARD_TIMER_MSEC = 2000;
    static final int DISCONNECTING_GUARD_TIMER_MSEC = 5000;
    private static final String EXTRA_OSU_ICON_QUERY_BSSID = "BSSID";
    private static final String EXTRA_OSU_ICON_QUERY_FILENAME = "FILENAME";
    private static final String EXTRA_OSU_PROVIDER = "OsuProvider";
    private static final int FAILURE = -1;
    private static final String GOOGLE_OUI = "DA-A1-19";

    @VisibleForTesting
    public static final int LAST_SELECTED_NETWORK_EXPIRATION_AGE_MILLIS = 30000;
    private static final int LINK_FLAPPING_DEBOUNCE_MSEC = 4000;
    private static final String LOGD_LEVEL_DEBUG = "D";
    private static final String LOGD_LEVEL_VERBOSE = "V";
    private static final int MESSAGE_HANDLING_STATUS_DEFERRED = -4;
    private static final int MESSAGE_HANDLING_STATUS_DISCARD = -5;
    private static final int MESSAGE_HANDLING_STATUS_FAIL = -2;
    private static final int MESSAGE_HANDLING_STATUS_HANDLING_ERROR = -7;
    private static final int MESSAGE_HANDLING_STATUS_LOOPED = -6;
    private static final int MESSAGE_HANDLING_STATUS_OBSOLETE = -3;
    private static final int MESSAGE_HANDLING_STATUS_OK = 1;
    private static final int MESSAGE_HANDLING_STATUS_PROCESSED = 2;
    private static final int MESSAGE_HANDLING_STATUS_REFUSED = -1;
    private static final int MESSAGE_HANDLING_STATUS_UNKNOWN = 0;
    private static final int M_CMD_SET_POWER_SAVING_MODE = 131289;
    private static final int M_CMD_UPDATE_SCAN_STRATEGY = 131296;
    private static final String NETWORKTYPE = "WIFI";
    private static final String NETWORKTYPE_UNTRUSTED = "WIFI_UT";
    private static final int NETWORK_STATUS_UNWANTED_DISABLE_AUTOJOIN = 2;
    private static final int NETWORK_STATUS_UNWANTED_DISCONNECT = 0;
    private static final int NETWORK_STATUS_UNWANTED_VALIDATION_FAILED = 1;

    @VisibleForTesting
    public static final short NUM_LOG_RECS_NORMAL = 100;

    @VisibleForTesting
    public static final short NUM_LOG_RECS_VERBOSE = 3000;

    @VisibleForTesting
    public static final short NUM_LOG_RECS_VERBOSE_LOW_MEMORY = 200;
    private static final int ONE_HOUR_MILLI = 3600000;
    static final int ROAM_GUARD_TIMER_MSEC = 15000;
    public static final int SCAN_ONLY_MODE = 2;
    public static final int SCAN_ONLY_WITH_WIFI_OFF_MODE = 3;
    private static final int SUCCESS = 1;
    public static final String SUPPLICANT_BSSID_ANY = "any";
    private static final int SUPPLICANT_RESTART_INTERVAL_MSECS = 5000;
    private static final int SUPPLICANT_RESTART_TRIES = 5;
    private static final int SUSPEND_DUE_TO_DHCP = 1;
    private static final int SUSPEND_DUE_TO_HIGH_PERF = 2;
    private static final int SUSPEND_DUE_TO_SCREEN = 4;
    private static final String SYSTEM_PROPERTY_LOG_CONTROL_WIFIHAL = "log.tag.WifiHAL";
    private static final String TAG = "WifiStateMachine";
    private boolean didBlackListBSSID;
    int disconnectingWatchdogCount;
    private long lastConnectAttemptTimestamp;
    private long lastLinkLayerStatsUpdate;
    private long lastOntimeReportTimeStamp;
    private Set<Integer> lastScanFreqs;
    private long lastScreenStateChangeTimeStamp;
    private final BackupManagerProxy mBackupManagerProxy;
    private final IBatteryStats mBatteryStats;
    private boolean mBluetoothConnectionActive;
    private final BuildProperties mBuildProperties;
    private ClientModeManager.Listener mClientModeCallback;
    private final Clock mClock;
    private ConnectivityManager mCm;
    private State mConnectModeState;
    private boolean mConnectNetwork;
    private State mConnectedState;

    @GuardedBy("mWifiReqCountLock")
    private int mConnectionReqCount;
    private Context mContext;
    private final WifiCountryCode mCountryCode;
    private State mDefaultState;
    private final NetworkCapabilities mDfltNetworkCapabilities;
    private DhcpResults mDhcpResults;
    private final Object mDhcpResultsLock;
    private long mDiagsConnectionStartMillis;
    int mDisableP2pWatchdogCount;
    private State mDisconnectedState;
    private long mDisconnectedTimeStamp;
    private State mDisconnectingState;
    private final AtomicBoolean mDontReconnect;
    private final AtomicBoolean mDontReconnectAndScan;
    private AtomicBoolean mEnableConnectedMacRandomization;
    private boolean mEnableRssiPolling;
    private FrameworkFacade mFacade;
    private String mInterfaceName;
    private IpClient mIpClient;
    private boolean mIpReachabilityDisconnectEnabled;
    private MtkIpReachabilityLostMonitor mIpReachabilityLostEnhancement;
    private boolean mIsAutoRoaming;
    private boolean mIsRunning;
    private State mL2ConnectedState;
    private String mLastBssid;
    private long mLastDriverRoamAttempt;
    private int mLastNetworkId;
    private final WorkSource mLastRunningWifiUids;
    private int mLastSignalLevel;
    private LinkProperties mLinkProperties;
    private final McastLockManagerFilterController mMcastLockManagerFilterController;
    private boolean mModeChange;
    private WifiNetworkAgent mNetworkAgent;
    private final NetworkCapabilities mNetworkCapabilitiesFilter;
    private WifiNetworkFactory mNetworkFactory;
    private NetworkInfo mNetworkInfo;
    private final NetworkMisc mNetworkMisc;
    private State mObtainingIpState;
    private int mOnTime;
    private int mOnTimeLastReport;
    private int mOnTimeScreenStateChange;
    private int mOperationalMode;
    private final AtomicBoolean mP2pConnected;
    private final boolean mP2pSupported;
    private final PasspointManager mPasspointManager;
    private int mPeriodicScanToken;
    private volatile int mPollRssiIntervalMsecs;
    private final PropertyService mPropertyService;
    private AsyncChannel mReplyChannel;
    private boolean mReportedRunning;
    private int mRoamFailCount;
    private State mRoamingState;
    private int mRssiPollToken;
    private byte[] mRssiRanges;
    int mRunningBeaconCount;
    private final WorkSource mRunningWifiUids;
    private int mRxTime;
    private int mRxTimeLastReport;
    private final SarManager mSarManager;
    private ScanRequestProxy mScanRequestProxy;
    private boolean mScreenOn;
    private final AtomicBoolean mStopScanStarted;
    private long mSupplicantScanIntervalMs;
    private SupplicantStateTracker mSupplicantStateTracker;
    private int mSuspendOptNeedsDisabled;
    private PowerManager.WakeLock mSuspendWakeLock;
    private int mTargetNetworkId;
    private String mTargetRoamBSSID;
    private final String mTcpBufferSizes;
    private TelephonyManager mTelephonyManager;
    private boolean mTemporarilyDisconnectWifi;
    private int mTxTime;
    private int mTxTimeLastReport;
    private UntrustedWifiNetworkFactory mUntrustedNetworkFactory;

    @GuardedBy("mWifiReqCountLock")
    private int mUntrustedReqCount;
    private AtomicBoolean mUserWantsSuspendOpt;
    private boolean mVerboseLoggingEnabled;
    private PowerManager.WakeLock mWakeLock;
    private WifiConfigManager mWifiConfigManager;
    private WifiConnectivityManager mWifiConnectivityManager;
    private BaseWifiDiagnostics mWifiDiagnostics;
    private final ExtendedWifiInfo mWifiInfo;
    private WifiInjector mWifiInjector;
    private WifiMetrics mWifiMetrics;
    private WifiMonitor mWifiMonitor;
    private WifiNative mWifiNative;
    private AsyncChannel mWifiP2pChannel;
    private WifiPermissionsUtil mWifiPermissionsUtil;
    private final WifiPermissionsWrapper mWifiPermissionsWrapper;
    private final Object mWifiReqCountLock;
    private final WifiScoreReport mWifiScoreReport;
    private final AtomicInteger mWifiState;
    private WifiStateTracker mWifiStateTracker;
    private final WrongPasswordNotifier mWrongPasswordNotifier;
    private int messageHandlingStatus;
    int roamWatchdogCount;
    private WifiConfiguration targetWificonfiguration;
    private boolean testNetworkDisconnect;
    private int testNetworkDisconnectCounter;
    private static final Class[] sMessageClasses = {AsyncChannel.class, WifiStateMachine.class, DhcpClient.class};
    private static final SparseArray<String> sSmToString = MessageUtils.findMessageNames(sMessageClasses);
    public static final WorkSource WIFI_WORK_SOURCE = new WorkSource(1010);
    private static int sScanAlarmIntentCount = 0;

    static int access$1004(WifiStateMachine wifiStateMachine) {
        int i = wifiStateMachine.mConnectionReqCount + 1;
        wifiStateMachine.mConnectionReqCount = i;
        return i;
    }

    static int access$1006(WifiStateMachine wifiStateMachine) {
        int i = wifiStateMachine.mConnectionReqCount - 1;
        wifiStateMachine.mConnectionReqCount = i;
        return i;
    }

    static int access$10808(WifiStateMachine wifiStateMachine) {
        int i = wifiStateMachine.mRoamFailCount;
        wifiStateMachine.mRoamFailCount = i + 1;
        return i;
    }

    static int access$11008(WifiStateMachine wifiStateMachine) {
        int i = wifiStateMachine.testNetworkDisconnectCounter;
        wifiStateMachine.testNetworkDisconnectCounter = i + 1;
        return i;
    }

    static int access$1204(WifiStateMachine wifiStateMachine) {
        int i = wifiStateMachine.mUntrustedReqCount + 1;
        wifiStateMachine.mUntrustedReqCount = i;
        return i;
    }

    static int access$1206(WifiStateMachine wifiStateMachine) {
        int i = wifiStateMachine.mUntrustedReqCount - 1;
        wifiStateMachine.mUntrustedReqCount = i;
        return i;
    }

    static int access$8508(WifiStateMachine wifiStateMachine) {
        int i = wifiStateMachine.mRssiPollToken;
        wifiStateMachine.mRssiPollToken = i + 1;
        return i;
    }

    protected void loge(String str) {
        Log.e(getName(), str);
    }

    protected void logd(String str) {
        Log.d(getName(), str);
    }

    protected void log(String str) {
        Log.d(getName(), str);
    }

    public WifiScoreReport getWifiScoreReport() {
        return this.mWifiScoreReport;
    }

    private void processRssiThreshold(byte b, int i, WifiNative.WifiRssiEventHandler wifiRssiEventHandler) {
        if (b == 127 || b == -128) {
            Log.wtf(TAG, "processRssiThreshold: Invalid rssi " + ((int) b));
            return;
        }
        for (int i2 = 0; i2 < this.mRssiRanges.length; i2++) {
            if (b < this.mRssiRanges[i2]) {
                byte b2 = this.mRssiRanges[i2];
                byte b3 = this.mRssiRanges[i2 - 1];
                this.mWifiInfo.setRssi(b);
                updateCapabilities();
                Log.d(TAG, "Re-program RSSI thresholds for " + smToString(i) + ": [" + ((int) b3) + ", " + ((int) b2) + "], curRssi=" + ((int) b) + " ret=" + startRssiMonitoringOffload(b2, b3, wifiRssiEventHandler));
                return;
            }
        }
    }

    int getPollRssiIntervalMsecs() {
        return this.mPollRssiIntervalMsecs;
    }

    void setPollRssiIntervalMsecs(int i) {
        this.mPollRssiIntervalMsecs = i;
    }

    public boolean clearTargetBssid(String str) {
        WifiConfiguration configuredNetwork = this.mWifiConfigManager.getConfiguredNetwork(this.mTargetNetworkId);
        if (configuredNetwork == null) {
            return false;
        }
        String str2 = "any";
        if (configuredNetwork.BSSID != null) {
            str2 = configuredNetwork.BSSID;
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "force BSSID to " + str2 + "due to config");
            }
        }
        if (this.mVerboseLoggingEnabled) {
            logd(str + " clearTargetBssid " + str2 + " key=" + configuredNetwork.configKey());
        }
        this.mTargetRoamBSSID = str2;
        return this.mWifiNative.setConfiguredNetworkBSSID(this.mInterfaceName, str2);
    }

    private boolean setTargetBssid(WifiConfiguration wifiConfiguration, String str) {
        if (wifiConfiguration == null || str == null) {
            return false;
        }
        if (wifiConfiguration.BSSID != null) {
            str = wifiConfiguration.BSSID;
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "force BSSID to " + str + "due to config");
            }
        }
        if (this.mVerboseLoggingEnabled) {
            Log.d(TAG, "setTargetBssid set to " + str + " key=" + wifiConfiguration.configKey());
        }
        this.mTargetRoamBSSID = str;
        wifiConfiguration.getNetworkSelectionStatus().setNetworkSelectionBSSID(str);
        return true;
    }

    private TelephonyManager getTelephonyManager() {
        if (this.mTelephonyManager == null) {
            this.mTelephonyManager = this.mWifiInjector.makeTelephonyManager();
        }
        return this.mTelephonyManager;
    }

    public WifiStateMachine(Context context, FrameworkFacade frameworkFacade, Looper looper, UserManager userManager, WifiInjector wifiInjector, BackupManagerProxy backupManagerProxy, WifiCountryCode wifiCountryCode, WifiNative wifiNative, WrongPasswordNotifier wrongPasswordNotifier, SarManager sarManager) {
        boolean z;
        super(TAG, looper);
        this.mVerboseLoggingEnabled = false;
        this.didBlackListBSSID = false;
        this.mP2pConnected = new AtomicBoolean(false);
        this.mTemporarilyDisconnectWifi = false;
        this.mScreenOn = false;
        this.mLastSignalLevel = -1;
        this.mIpReachabilityDisconnectEnabled = true;
        this.testNetworkDisconnect = false;
        this.mEnableRssiPolling = false;
        this.mPollRssiIntervalMsecs = DEFAULT_POLL_RSSI_INTERVAL_MSECS;
        this.mRssiPollToken = 0;
        this.mOperationalMode = 4;
        this.mModeChange = false;
        this.mClientModeCallback = null;
        this.mBluetoothConnectionActive = false;
        this.mPeriodicScanToken = 0;
        this.mDhcpResultsLock = new Object();
        this.mIsAutoRoaming = false;
        this.mRoamFailCount = 0;
        this.mTargetRoamBSSID = "any";
        this.mTargetNetworkId = -1;
        this.mLastDriverRoamAttempt = 0L;
        this.targetWificonfiguration = null;
        this.mReplyChannel = new AsyncChannel();
        this.mConnectionReqCount = 0;
        this.mUntrustedReqCount = 0;
        this.mWifiReqCountLock = new Object();
        this.mNetworkCapabilitiesFilter = new NetworkCapabilities();
        this.mNetworkMisc = new NetworkMisc();
        this.testNetworkDisconnectCounter = 0;
        this.roamWatchdogCount = 0;
        this.disconnectingWatchdogCount = 0;
        this.mDisableP2pWatchdogCount = 0;
        this.mSuspendOptNeedsDisabled = 0;
        this.mUserWantsSuspendOpt = new AtomicBoolean(true);
        this.mEnableConnectedMacRandomization = new AtomicBoolean(false);
        this.mRunningBeaconCount = 0;
        this.mDefaultState = new DefaultState();
        this.mConnectModeState = new ConnectModeState();
        this.mL2ConnectedState = new L2ConnectedState();
        this.mObtainingIpState = new ObtainingIpState();
        this.mConnectedState = new ConnectedState();
        this.mRoamingState = new RoamingState();
        this.mDisconnectingState = new DisconnectingState();
        this.mDisconnectedState = new DisconnectedState();
        this.mWifiState = new AtomicInteger(1);
        this.mIsRunning = false;
        this.mReportedRunning = false;
        this.mRunningWifiUids = new WorkSource();
        this.mLastRunningWifiUids = new WorkSource();
        this.mStopScanStarted = new AtomicBoolean(false);
        this.mConnectNetwork = false;
        this.mDontReconnectAndScan = new AtomicBoolean(false);
        this.mDontReconnect = new AtomicBoolean(false);
        this.mDisconnectedTimeStamp = 0L;
        this.lastConnectAttemptTimestamp = 0L;
        this.lastScanFreqs = null;
        this.messageHandlingStatus = 0;
        this.mOnTime = 0;
        this.mTxTime = 0;
        this.mRxTime = 0;
        this.mOnTimeScreenStateChange = 0;
        this.lastOntimeReportTimeStamp = 0L;
        this.lastScreenStateChangeTimeStamp = 0L;
        this.mOnTimeLastReport = 0;
        this.mTxTimeLastReport = 0;
        this.mRxTimeLastReport = 0;
        this.lastLinkLayerStatsUpdate = 0L;
        this.mDiagsConnectionStartMillis = -1L;
        this.mWifiInjector = wifiInjector;
        this.mWifiMetrics = this.mWifiInjector.getWifiMetrics();
        this.mClock = wifiInjector.getClock();
        this.mPropertyService = wifiInjector.getPropertyService();
        this.mBuildProperties = wifiInjector.getBuildProperties();
        this.mContext = context;
        this.mFacade = frameworkFacade;
        this.mWifiNative = wifiNative;
        this.mBackupManagerProxy = backupManagerProxy;
        this.mWrongPasswordNotifier = wrongPasswordNotifier;
        this.mSarManager = sarManager;
        this.mNetworkInfo = new NetworkInfo(1, 0, NETWORKTYPE, "");
        this.mBatteryStats = IBatteryStats.Stub.asInterface(this.mFacade.getService("batterystats"));
        this.mWifiStateTracker = wifiInjector.getWifiStateTracker();
        this.mFacade.getService("network_management");
        this.mP2pSupported = this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi.direct");
        this.mWifiPermissionsUtil = this.mWifiInjector.getWifiPermissionsUtil();
        this.mWifiConfigManager = this.mWifiInjector.getWifiConfigManager();
        this.mPasspointManager = this.mWifiInjector.getPasspointManager();
        this.mWifiMonitor = this.mWifiInjector.getWifiMonitor();
        this.mWifiDiagnostics = this.mWifiInjector.getWifiDiagnostics();
        this.mScanRequestProxy = this.mWifiInjector.getScanRequestProxy();
        this.mWifiPermissionsWrapper = this.mWifiInjector.getWifiPermissionsWrapper();
        this.mWifiInfo = new ExtendedWifiInfo();
        this.mSupplicantStateTracker = this.mFacade.makeSupplicantStateTracker(context, this.mWifiConfigManager, getHandler());
        this.mLinkProperties = new LinkProperties();
        this.mMcastLockManagerFilterController = new McastLockManagerFilterController();
        this.mNetworkInfo.setIsAvailable(false);
        this.mLastBssid = null;
        this.mLastNetworkId = -1;
        this.mLastSignalLevel = -1;
        this.mCountryCode = wifiCountryCode;
        this.mWifiScoreReport = new WifiScoreReport(this.mWifiInjector.getScoringParams(), this.mClock);
        this.mNetworkCapabilitiesFilter.addTransportType(1);
        this.mNetworkCapabilitiesFilter.addCapability(12);
        this.mNetworkCapabilitiesFilter.addCapability(11);
        this.mNetworkCapabilitiesFilter.addCapability(18);
        this.mNetworkCapabilitiesFilter.addCapability(20);
        this.mNetworkCapabilitiesFilter.addCapability(13);
        this.mNetworkCapabilitiesFilter.setLinkUpstreamBandwidthKbps(1048576);
        this.mNetworkCapabilitiesFilter.setLinkDownstreamBandwidthKbps(1048576);
        this.mDfltNetworkCapabilities = new NetworkCapabilities(this.mNetworkCapabilitiesFilter);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if (action.equals("android.intent.action.SCREEN_ON")) {
                    WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_SCREEN_STATE_CHANGED, 1);
                } else if (action.equals("android.intent.action.SCREEN_OFF")) {
                    WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_SCREEN_STATE_CHANGED, 0);
                }
            }
        }, intentFilter);
        this.mFacade.registerContentObserver(this.mContext, Settings.Global.getUriFor("wifi_suspend_optimizations_enabled"), false, new ContentObserver(getHandler()) {
            @Override
            public void onChange(boolean z2) {
                WifiStateMachine.this.mUserWantsSuspendOpt.set(WifiStateMachine.this.mFacade.getIntegerSetting(WifiStateMachine.this.mContext, "wifi_suspend_optimizations_enabled", 1) == 1);
            }
        });
        this.mFacade.registerContentObserver(this.mContext, Settings.Global.getUriFor("wifi_connected_mac_randomization_enabled"), false, new ContentObserver(getHandler()) {
            @Override
            public void onChange(boolean z2) {
                WifiStateMachine.this.updateConnectedMacRandomizationSetting();
            }
        });
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_BOOT_COMPLETED);
            }
        }, new IntentFilter("android.intent.action.LOCKED_BOOT_COMPLETED"));
        AtomicBoolean atomicBoolean = this.mUserWantsSuspendOpt;
        if (this.mFacade.getIntegerSetting(this.mContext, "wifi_suspend_optimizations_enabled", 1) != 1) {
            z = false;
        } else {
            z = true;
        }
        atomicBoolean.set(z);
        updateConnectedMacRandomizationSetting();
        PowerManager powerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mWakeLock = powerManager.newWakeLock(1, getName());
        this.mSuspendWakeLock = powerManager.newWakeLock(1, "WifiSuspend");
        this.mSuspendWakeLock.setReferenceCounted(false);
        this.mTcpBufferSizes = this.mContext.getResources().getString(R.string.app_category_video);
        addState(this.mDefaultState);
        addState(this.mConnectModeState, this.mDefaultState);
        addState(this.mL2ConnectedState, this.mConnectModeState);
        addState(this.mObtainingIpState, this.mL2ConnectedState);
        addState(this.mConnectedState, this.mL2ConnectedState);
        addState(this.mRoamingState, this.mL2ConnectedState);
        addState(this.mDisconnectingState, this.mConnectModeState);
        addState(this.mDisconnectedState, this.mConnectModeState);
        setInitialState(this.mDefaultState);
        setLogRecSize(100);
        setLogOnlyTransitions(false);
        start();
        handleScreenStateChanged(powerManager.isInteractive());
        this.mIpReachabilityLostEnhancement = new MtkIpReachabilityLostMonitor(this, this.mWifiMonitor, looper);
        MtkWfcUtility.init(context);
    }

    private void registerForWifiMonitorEvents() {
        this.mWifiMonitor.registerHandler(this.mInterfaceName, CMD_TARGET_BSSID, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, CMD_ASSOCIATED_BSSID, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.ANQP_DONE_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.ASSOCIATION_REJECTION_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.AUTHENTICATION_FAILURE_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.GAS_QUERY_DONE_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.GAS_QUERY_START_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.HS20_REMEDIATION_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.NETWORK_CONNECTION_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.NETWORK_DISCONNECTION_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.RX_HS20_ANQP_ICON_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.SUP_REQUEST_IDENTITY, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.SUP_REQUEST_SIM_AUTH, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.ASSOCIATION_REJECTION_EVENT, this.mWifiMetrics.getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.AUTHENTICATION_FAILURE_EVENT, this.mWifiMetrics.getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.NETWORK_CONNECTION_EVENT, this.mWifiMetrics.getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.NETWORK_DISCONNECTION_EVENT, this.mWifiMetrics.getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT, this.mWifiMetrics.getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, CMD_ASSOCIATED_BSSID, this.mWifiMetrics.getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, CMD_TARGET_BSSID, this.mWifiMetrics.getHandler());
    }

    class McastLockManagerFilterController implements WifiMulticastLockManager.FilterController {
        McastLockManagerFilterController() {
        }

        @Override
        public void startFilteringMulticastPackets() {
            if (WifiStateMachine.this.mIpClient != null) {
                WifiStateMachine.this.mIpClient.setMulticastFilter(true);
            }
        }

        @Override
        public void stopFilteringMulticastPackets() {
            if (WifiStateMachine.this.mIpClient != null) {
                WifiStateMachine.this.mIpClient.setMulticastFilter(false);
            }
        }
    }

    class IpClientCallback extends IpClient.Callback {
        IpClientCallback() {
        }

        public void onPreDhcpAction() {
            WifiStateMachine.this.sendMessage(196611);
        }

        public void onPostDhcpAction() {
            WifiStateMachine.this.sendMessage(196612);
        }

        public void onNewDhcpResults(DhcpResults dhcpResults) {
            if (dhcpResults != null) {
                WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_IPV4_PROVISIONING_SUCCESS, dhcpResults);
            } else {
                WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_IPV4_PROVISIONING_FAILURE);
                WifiStateMachine.this.mWifiInjector.getWifiLastResortWatchdog().noteConnectionFailureAndTriggerIfNeeded(WifiStateMachine.this.getTargetSsid(), WifiStateMachine.this.mTargetRoamBSSID, 3);
            }
        }

        public void onProvisioningSuccess(LinkProperties linkProperties) {
            WifiStateMachine.this.mWifiMetrics.logStaEvent(7);
            WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_UPDATE_LINKPROPERTIES, linkProperties);
            WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_IP_CONFIGURATION_SUCCESSFUL);
        }

        public void onProvisioningFailure(LinkProperties linkProperties) {
            WifiStateMachine.this.mWifiMetrics.logStaEvent(8);
            WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_IP_CONFIGURATION_LOST);
        }

        public void onLinkPropertiesChange(LinkProperties linkProperties) {
            WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_UPDATE_LINKPROPERTIES, linkProperties);
        }

        public void onReachabilityLost(String str) {
            WifiStateMachine.this.mWifiMetrics.logStaEvent(9);
            WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_IP_REACHABILITY_LOST, str);
        }

        public void installPacketFilter(byte[] bArr) {
            WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_INSTALL_PACKET_FILTER, bArr);
        }

        public void startReadPacketFilter() {
            WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_READ_PACKET_FILTER);
        }

        public void setFallbackMulticastFilter(boolean z) {
            WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_SET_FALLBACK_PACKET_FILTERING, Boolean.valueOf(z));
        }

        public void setNeighborDiscoveryOffload(boolean z) {
            WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_CONFIG_ND_OFFLOAD, z ? 1 : 0);
        }
    }

    private void stopIpClient() {
        handlePostDhcpSetup();
        this.mIpClient.stop();
    }

    PendingIntent getPrivateBroadcast(String str, int i) {
        Intent intent = new Intent(str, (Uri) null);
        intent.addFlags(67108864);
        intent.setPackage("android");
        return this.mFacade.getBroadcast(this.mContext, i, intent, 0);
    }

    void setSupplicantLogLevel() {
        this.mWifiNative.setSupplicantLogLevel(this.mVerboseLoggingEnabled);
    }

    public void enableVerboseLogging(int i) {
        if (i > 0) {
            this.mVerboseLoggingEnabled = true;
            setLogRecSize(ActivityManager.isLowRamDeviceStatic() ? ChannelHelper.SCAN_PERIOD_PER_CHANNEL_MS : DEFAULT_POLL_RSSI_INTERVAL_MSECS);
        } else {
            this.mVerboseLoggingEnabled = false;
            setLogRecSize(100);
        }
        configureVerboseHalLogging(this.mVerboseLoggingEnabled);
        setSupplicantLogLevel();
        this.mCountryCode.enableVerboseLogging(i);
        this.mWifiScoreReport.enableVerboseLogging(this.mVerboseLoggingEnabled);
        this.mWifiDiagnostics.startLogging(this.mVerboseLoggingEnabled);
        this.mWifiMonitor.enableVerboseLogging(i);
        this.mWifiNative.enableVerboseLogging(i);
        this.mWifiConfigManager.enableVerboseLogging(i);
        this.mSupplicantStateTracker.enableVerboseLogging(i);
        this.mPasspointManager.enableVerboseLogging(i);
        if (this.mWifiConnectivityManager != null) {
            this.mWifiConnectivityManager.mDbg = i > 0;
        }
    }

    private void configureVerboseHalLogging(boolean z) {
        if (this.mBuildProperties.isUserBuild()) {
            return;
        }
        this.mPropertyService.set(SYSTEM_PROPERTY_LOG_CONTROL_WIFIHAL, z ? LOGD_LEVEL_VERBOSE : LOGD_LEVEL_DEBUG);
    }

    public void clearANQPCache() {
    }

    private boolean setRandomMacOui() {
        String string = this.mContext.getResources().getString(R.string.app_category_social);
        if (TextUtils.isEmpty(string)) {
            string = GOOGLE_OUI;
        }
        String[] strArrSplit = string.split("-");
        byte[] bArr = {(byte) (Integer.parseInt(strArrSplit[0], 16) & Constants.BYTE_MASK), (byte) (Integer.parseInt(strArrSplit[1], 16) & Constants.BYTE_MASK), (byte) (Integer.parseInt(strArrSplit[2], 16) & Constants.BYTE_MASK)};
        logd("Setting OUI to " + string);
        return this.mWifiNative.setScanningMacOui(this.mInterfaceName, bArr);
    }

    private boolean connectToUserSelectNetwork(int i, int i2, boolean z) {
        logd("connectToUserSelectNetwork netId " + i + ", uid " + i2 + ", forceReconnect = " + z);
        if (this.mWifiConfigManager.getConfiguredNetwork(i) == null) {
            loge("connectToUserSelectNetwork Invalid network Id=" + i);
            return false;
        }
        if (!this.mWifiConfigManager.enableNetwork(i, true, i2) || !this.mWifiConfigManager.updateLastConnectUid(i, i2)) {
            logi("connectToUserSelectNetwork Allowing uid " + i2 + " with insufficient permissions to connect=" + i);
        } else if (this.mWifiPermissionsUtil.checkNetworkSettingsPermission(i2)) {
            this.mWifiConnectivityManager.setUserConnectChoice(i);
        }
        if (!z && this.mWifiInfo.getNetworkId() == i) {
            logi("connectToUserSelectNetwork already connecting/connected=" + i);
        } else {
            this.mWifiConnectivityManager.prepareForForcedConnection(i);
            startConnectToNetwork(i, i2, "any");
        }
        return true;
    }

    public Messenger getMessenger() {
        return new Messenger(getHandler());
    }

    public long getDisconnectedTimeMilli() {
        if (getCurrentState() != this.mDisconnectedState || this.mDisconnectedTimeStamp == 0) {
            return 0L;
        }
        return this.mClock.getWallClockMillis() - this.mDisconnectedTimeStamp;
    }

    private boolean checkOrDeferScanAllowed(Message message) {
        long wallClockMillis = this.mClock.getWallClockMillis();
        if (this.lastConnectAttemptTimestamp != 0 && wallClockMillis - this.lastConnectAttemptTimestamp < 10000) {
            sendMessageDelayed(Message.obtain(message), 11000 - (wallClockMillis - this.lastConnectAttemptTimestamp));
            return false;
        }
        return true;
    }

    String reportOnTime() {
        long wallClockMillis = this.mClock.getWallClockMillis();
        StringBuilder sb = new StringBuilder();
        int i = this.mOnTime - this.mOnTimeLastReport;
        this.mOnTimeLastReport = this.mOnTime;
        int i2 = this.mTxTime - this.mTxTimeLastReport;
        this.mTxTimeLastReport = this.mTxTime;
        int i3 = this.mRxTime - this.mRxTimeLastReport;
        this.mRxTimeLastReport = this.mRxTime;
        int i4 = (int) (wallClockMillis - this.lastOntimeReportTimeStamp);
        this.lastOntimeReportTimeStamp = wallClockMillis;
        sb.append(String.format("[on:%d tx:%d rx:%d period:%d]", Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3), Integer.valueOf(i4)));
        sb.append(String.format(" from screen [on:%d period:%d]", Integer.valueOf(this.mOnTime - this.mOnTimeScreenStateChange), Integer.valueOf((int) (wallClockMillis - this.lastScreenStateChangeTimeStamp))));
        return sb.toString();
    }

    WifiLinkLayerStats getWifiLinkLayerStats() {
        if (this.mInterfaceName == null) {
            loge("getWifiLinkLayerStats called without an interface");
            return null;
        }
        this.lastLinkLayerStatsUpdate = this.mClock.getWallClockMillis();
        WifiLinkLayerStats wifiLinkLayerStats = this.mWifiNative.getWifiLinkLayerStats(this.mInterfaceName);
        if (wifiLinkLayerStats != null) {
            this.mOnTime = wifiLinkLayerStats.on_time;
            this.mTxTime = wifiLinkLayerStats.tx_time;
            this.mRxTime = wifiLinkLayerStats.rx_time;
            this.mRunningBeaconCount = wifiLinkLayerStats.beacon_rx;
            this.mWifiInfo.updatePacketRates(wifiLinkLayerStats, this.lastLinkLayerStatsUpdate);
        } else {
            this.mWifiInfo.updatePacketRates(this.mFacade.getTxPackets(this.mInterfaceName), this.mFacade.getRxPackets(this.mInterfaceName), this.lastLinkLayerStatsUpdate);
        }
        return wifiLinkLayerStats;
    }

    private byte[] getDstMacForKeepalive(KeepalivePacketData keepalivePacketData) throws KeepalivePacketData.InvalidPacketException {
        try {
            return NativeUtil.macAddressToByteArray(macAddressFromRoute(RouteInfo.selectBestRoute(this.mLinkProperties.getRoutes(), keepalivePacketData.dstAddress).getGateway().getHostAddress()));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new KeepalivePacketData.InvalidPacketException(-21);
        }
    }

    private static int getEtherProtoForKeepalive(KeepalivePacketData keepalivePacketData) throws KeepalivePacketData.InvalidPacketException {
        if (keepalivePacketData.dstAddress instanceof Inet4Address) {
            return OsConstants.ETH_P_IP;
        }
        if (keepalivePacketData.dstAddress instanceof Inet6Address) {
            return OsConstants.ETH_P_IPV6;
        }
        throw new KeepalivePacketData.InvalidPacketException(-21);
    }

    int startWifiIPPacketOffload(int i, KeepalivePacketData keepalivePacketData, int i2) {
        try {
            int iStartSendingOffloadedPacket = this.mWifiNative.startSendingOffloadedPacket(this.mInterfaceName, i, getDstMacForKeepalive(keepalivePacketData), keepalivePacketData.getPacket(), getEtherProtoForKeepalive(keepalivePacketData), i2 * 1000);
            if (iStartSendingOffloadedPacket != 0) {
                loge("startWifiIPPacketOffload(" + i + ", " + i2 + "): hardware error " + iStartSendingOffloadedPacket);
                return -31;
            }
            return 0;
        } catch (KeepalivePacketData.InvalidPacketException e) {
            return e.error;
        }
    }

    int stopWifiIPPacketOffload(int i) {
        int iStopSendingOffloadedPacket = this.mWifiNative.stopSendingOffloadedPacket(this.mInterfaceName, i);
        if (iStopSendingOffloadedPacket != 0) {
            loge("stopWifiIPPacketOffload(" + i + "): hardware error " + iStopSendingOffloadedPacket);
            return -31;
        }
        return 0;
    }

    int startRssiMonitoringOffload(byte b, byte b2, WifiNative.WifiRssiEventHandler wifiRssiEventHandler) {
        return this.mWifiNative.startRssiMonitoring(this.mInterfaceName, b, b2, wifiRssiEventHandler);
    }

    int stopRssiMonitoringOffload() {
        return this.mWifiNative.stopRssiMonitoring(this.mInterfaceName);
    }

    public void setWifiStateForApiCalls(int i) {
        switch (i) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
                if (this.mVerboseLoggingEnabled) {
                    Log.d(TAG, "setting wifi state to: " + i);
                }
                this.mWifiState.set(i);
                break;
            default:
                Log.d(TAG, "attempted to set an invalid state: " + i);
                break;
        }
    }

    public int syncGetWifiState() {
        return this.mWifiState.get();
    }

    public String syncGetWifiStateByName() {
        switch (this.mWifiState.get()) {
            case 0:
                return "disabling";
            case 1:
                return "disabled";
            case 2:
                return "enabling";
            case 3:
                return "enabled";
            case 4:
                return "unknown state";
            default:
                return "[invalid state]";
        }
    }

    public boolean isConnected() {
        return getCurrentState() == this.mConnectedState;
    }

    public boolean isDisconnected() {
        return getCurrentState() == this.mDisconnectedState;
    }

    public boolean isP2pConnected() {
        return this.mP2pConnected.get();
    }

    public boolean isSupplicantTransientState() {
        SupplicantState supplicantState = this.mWifiInfo.getSupplicantState();
        if (supplicantState == SupplicantState.ASSOCIATING || supplicantState == SupplicantState.AUTHENTICATING || supplicantState == SupplicantState.FOUR_WAY_HANDSHAKE || supplicantState == SupplicantState.GROUP_HANDSHAKE) {
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "Supplicant is under transient state: " + supplicantState);
                return true;
            }
            return true;
        }
        if (this.mVerboseLoggingEnabled) {
            Log.d(TAG, "Supplicant is under steady state: " + supplicantState);
            return false;
        }
        return false;
    }

    public WifiInfo syncRequestConnectionInfo() {
        return new WifiInfo(this.mWifiInfo);
    }

    public WifiInfo getWifiInfo() {
        return this.mWifiInfo;
    }

    public DhcpResults syncGetDhcpResults() {
        DhcpResults dhcpResults;
        synchronized (this.mDhcpResultsLock) {
            dhcpResults = new DhcpResults(this.mDhcpResults);
        }
        return dhcpResults;
    }

    public void handleIfaceDestroyed() {
        handleNetworkDisconnect();
    }

    public void setOperationalMode(int i, String str) {
        if (this.mVerboseLoggingEnabled) {
            log("setting operational mode to " + String.valueOf(i) + " for iface: " + str);
        }
        this.mModeChange = true;
        if (i != 1) {
            transitionTo(this.mDefaultState);
        } else if (str != null) {
            this.mInterfaceName = str;
            transitionTo(this.mDisconnectedState);
        } else {
            Log.e(TAG, "supposed to enter connect mode, but iface is null -> DefaultState");
            transitionTo(this.mDefaultState);
        }
        sendMessageAtFrontOfQueue(CMD_SET_OPERATIONAL_MODE);
    }

    public void takeBugReport(String str, String str2) {
        this.mWifiDiagnostics.takeBugReport(str, str2);
    }

    @VisibleForTesting
    protected int getOperationalModeForTest() {
        return this.mOperationalMode;
    }

    protected WifiMulticastLockManager.FilterController getMcastLockManagerFilterController() {
        return this.mMcastLockManagerFilterController;
    }

    public boolean syncQueryPasspointIcon(AsyncChannel asyncChannel, long j, String str) {
        Bundle bundle = new Bundle();
        bundle.putLong("BSSID", j);
        bundle.putString(EXTRA_OSU_ICON_QUERY_FILENAME, str);
        Message messageSendMessageSynchronously = asyncChannel.sendMessageSynchronously(CMD_QUERY_OSU_ICON, bundle);
        int i = messageSendMessageSynchronously.arg1;
        messageSendMessageSynchronously.recycle();
        return i == 1;
    }

    public int matchProviderWithCurrentNetwork(AsyncChannel asyncChannel, String str) {
        Message messageSendMessageSynchronously = asyncChannel.sendMessageSynchronously(CMD_MATCH_PROVIDER_NETWORK, str);
        int i = messageSendMessageSynchronously.arg1;
        messageSendMessageSynchronously.recycle();
        return i;
    }

    public void deauthenticateNetwork(AsyncChannel asyncChannel, long j, boolean z) {
    }

    public void disableEphemeralNetwork(String str) {
        if (str != null) {
            sendMessage(CMD_DISABLE_EPHEMERAL_NETWORK, str);
        }
    }

    public void disconnectCommand() {
        sendMessage(CMD_DISCONNECT);
    }

    public void disconnectCommand(int i, int i2) {
        sendMessage(CMD_DISCONNECT, i, i2);
    }

    public void reconnectCommand(WorkSource workSource) {
        sendMessage(CMD_RECONNECT, workSource);
    }

    public void reassociateCommand() {
        sendMessage(CMD_REASSOCIATE);
    }

    public void reloadTlsNetworksAndReconnect() {
        sendMessage(CMD_RELOAD_TLS_AND_RECONNECT);
    }

    public int syncAddOrUpdateNetwork(AsyncChannel asyncChannel, WifiConfiguration wifiConfiguration) {
        Message messageSendMessageSynchronously = asyncChannel.sendMessageSynchronously(CMD_ADD_OR_UPDATE_NETWORK, wifiConfiguration);
        int i = messageSendMessageSynchronously.arg1;
        messageSendMessageSynchronously.recycle();
        return i;
    }

    public List<WifiConfiguration> syncGetConfiguredNetworks(int i, AsyncChannel asyncChannel) {
        Message messageSendMessageSynchronously = asyncChannel.sendMessageSynchronously(CMD_GET_CONFIGURED_NETWORKS, i);
        if (messageSendMessageSynchronously == null) {
            return null;
        }
        List<WifiConfiguration> list = (List) messageSendMessageSynchronously.obj;
        messageSendMessageSynchronously.recycle();
        return list;
    }

    public List<WifiConfiguration> syncGetPrivilegedConfiguredNetwork(AsyncChannel asyncChannel) {
        Message messageSendMessageSynchronously = asyncChannel.sendMessageSynchronously(CMD_GET_PRIVILEGED_CONFIGURED_NETWORKS);
        List<WifiConfiguration> list = (List) messageSendMessageSynchronously.obj;
        messageSendMessageSynchronously.recycle();
        return list;
    }

    public WifiConfiguration syncGetMatchingWifiConfig(ScanResult scanResult, AsyncChannel asyncChannel) {
        Message messageSendMessageSynchronously = asyncChannel.sendMessageSynchronously(CMD_GET_MATCHING_CONFIG, scanResult);
        WifiConfiguration wifiConfiguration = (WifiConfiguration) messageSendMessageSynchronously.obj;
        messageSendMessageSynchronously.recycle();
        return wifiConfiguration;
    }

    List<WifiConfiguration> getAllMatchingWifiConfigs(ScanResult scanResult, AsyncChannel asyncChannel) {
        Message messageSendMessageSynchronously = asyncChannel.sendMessageSynchronously(CMD_GET_ALL_MATCHING_CONFIGS, scanResult);
        List<WifiConfiguration> list = (List) messageSendMessageSynchronously.obj;
        messageSendMessageSynchronously.recycle();
        return list;
    }

    public List<OsuProvider> syncGetMatchingOsuProviders(ScanResult scanResult, AsyncChannel asyncChannel) {
        Message messageSendMessageSynchronously = asyncChannel.sendMessageSynchronously(CMD_GET_MATCHING_OSU_PROVIDERS, scanResult);
        List<OsuProvider> list = (List) messageSendMessageSynchronously.obj;
        messageSendMessageSynchronously.recycle();
        return list;
    }

    public boolean syncAddOrUpdatePasspointConfig(AsyncChannel asyncChannel, PasspointConfiguration passpointConfiguration, int i) {
        Message messageSendMessageSynchronously = asyncChannel.sendMessageSynchronously(CMD_ADD_OR_UPDATE_PASSPOINT_CONFIG, i, 0, passpointConfiguration);
        boolean z = messageSendMessageSynchronously.arg1 == 1;
        messageSendMessageSynchronously.recycle();
        return z;
    }

    public boolean syncRemovePasspointConfig(AsyncChannel asyncChannel, String str) {
        Message messageSendMessageSynchronously = asyncChannel.sendMessageSynchronously(CMD_REMOVE_PASSPOINT_CONFIG, str);
        boolean z = messageSendMessageSynchronously.arg1 == 1;
        messageSendMessageSynchronously.recycle();
        return z;
    }

    public List<PasspointConfiguration> syncGetPasspointConfigs(AsyncChannel asyncChannel) {
        Message messageSendMessageSynchronously = asyncChannel.sendMessageSynchronously(CMD_GET_PASSPOINT_CONFIGS);
        List<PasspointConfiguration> list = (List) messageSendMessageSynchronously.obj;
        messageSendMessageSynchronously.recycle();
        return list;
    }

    public boolean syncStartSubscriptionProvisioning(int i, OsuProvider osuProvider, IProvisioningCallback iProvisioningCallback, AsyncChannel asyncChannel) {
        Message messageObtain = Message.obtain();
        messageObtain.what = CMD_START_SUBSCRIPTION_PROVISIONING;
        messageObtain.arg1 = i;
        messageObtain.obj = iProvisioningCallback;
        messageObtain.getData().putParcelable(EXTRA_OSU_PROVIDER, osuProvider);
        Message messageSendMessageSynchronously = asyncChannel.sendMessageSynchronously(messageObtain);
        boolean z = messageSendMessageSynchronously.arg1 != 0;
        messageSendMessageSynchronously.recycle();
        return z;
    }

    public int syncGetSupportedFeatures(AsyncChannel asyncChannel) {
        Message messageSendMessageSynchronously = asyncChannel.sendMessageSynchronously(CMD_GET_SUPPORTED_FEATURES);
        int i = messageSendMessageSynchronously.arg1;
        messageSendMessageSynchronously.recycle();
        if (this.mPropertyService.getBoolean("config.disable_rtt", false)) {
            return i & (-385);
        }
        return i;
    }

    public WifiLinkLayerStats syncGetLinkLayerStats(AsyncChannel asyncChannel) {
        Message messageSendMessageSynchronously = asyncChannel.sendMessageSynchronously(CMD_GET_LINK_LAYER_STATS);
        WifiLinkLayerStats wifiLinkLayerStats = (WifiLinkLayerStats) messageSendMessageSynchronously.obj;
        messageSendMessageSynchronously.recycle();
        return wifiLinkLayerStats;
    }

    public boolean syncRemoveNetwork(AsyncChannel asyncChannel, int i) {
        Message messageSendMessageSynchronously = asyncChannel.sendMessageSynchronously(CMD_REMOVE_NETWORK, i);
        boolean z = messageSendMessageSynchronously.arg1 != -1;
        messageSendMessageSynchronously.recycle();
        return z;
    }

    public boolean syncEnableNetwork(AsyncChannel asyncChannel, int i, boolean z) {
        Message messageSendMessageSynchronously = asyncChannel.sendMessageSynchronously(CMD_ENABLE_NETWORK, i, z ? 1 : 0);
        boolean z2 = messageSendMessageSynchronously.arg1 != -1;
        messageSendMessageSynchronously.recycle();
        return z2;
    }

    public boolean syncDisableNetwork(AsyncChannel asyncChannel, int i) {
        Message messageSendMessageSynchronously = asyncChannel.sendMessageSynchronously(151569, i);
        boolean z = messageSendMessageSynchronously.what != 151570;
        messageSendMessageSynchronously.recycle();
        return z;
    }

    public void enableRssiPolling(boolean z) {
        sendMessage(CMD_ENABLE_RSSI_POLL, z ? 1 : 0, 0);
    }

    public void setHighPerfModeEnabled(boolean z) {
        sendMessage(CMD_SET_HIGH_PERF_MODE, z ? 1 : 0, 0);
    }

    public synchronized void resetSimAuthNetworks(boolean z) {
        sendMessage(CMD_RESET_SIM_NETWORKS, z ? 1 : 0);
    }

    public Network getCurrentNetwork() {
        if (this.mNetworkAgent != null) {
            return new Network(this.mNetworkAgent.netId);
        }
        return null;
    }

    public void enableTdls(String str, boolean z) {
        sendMessage(CMD_ENABLE_TDLS, z ? 1 : 0, 0, str);
    }

    public void sendBluetoothAdapterStateChange(int i) {
        sendMessage(CMD_BLUETOOTH_ADAPTER_STATE_CHANGE, i, 0);
    }

    public void removeAppConfigs(String str, int i) {
        ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.packageName = str;
        applicationInfo.uid = i;
        sendMessage(CMD_REMOVE_APP_CONFIGURATIONS, applicationInfo);
    }

    public void removeUserConfigs(int i) {
        sendMessage(CMD_REMOVE_USER_CONFIGURATIONS, i);
    }

    public void updateBatteryWorkSource(WorkSource workSource) {
        synchronized (this.mRunningWifiUids) {
            if (workSource != null) {
                try {
                    try {
                        this.mRunningWifiUids.set(workSource);
                    } catch (RemoteException e) {
                    }
                } finally {
                }
            }
            if (this.mIsRunning) {
                if (this.mReportedRunning) {
                    if (!this.mLastRunningWifiUids.equals(this.mRunningWifiUids)) {
                        this.mBatteryStats.noteWifiRunningChanged(this.mLastRunningWifiUids, this.mRunningWifiUids);
                        this.mLastRunningWifiUids.set(this.mRunningWifiUids);
                    }
                } else {
                    this.mBatteryStats.noteWifiRunning(this.mRunningWifiUids);
                    this.mLastRunningWifiUids.set(this.mRunningWifiUids);
                    this.mReportedRunning = true;
                }
            } else if (this.mReportedRunning) {
                this.mBatteryStats.noteWifiStopped(this.mLastRunningWifiUids);
                this.mLastRunningWifiUids.clear();
                this.mReportedRunning = false;
            }
            this.mWakeLock.setWorkSource(workSource);
        }
    }

    public void dumpIpClient(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (this.mIpClient != null) {
            this.mIpClient.dump(fileDescriptor, printWriter, strArr);
        }
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        super.dump(fileDescriptor, printWriter, strArr);
        this.mSupplicantStateTracker.dump(fileDescriptor, printWriter, strArr);
        printWriter.println("mLinkProperties " + this.mLinkProperties);
        printWriter.println("mWifiInfo " + this.mWifiInfo);
        printWriter.println("mDhcpResults " + this.mDhcpResults);
        printWriter.println("mNetworkInfo " + this.mNetworkInfo);
        printWriter.println("mLastSignalLevel " + this.mLastSignalLevel);
        printWriter.println("mLastBssid " + this.mLastBssid);
        printWriter.println("mLastNetworkId " + this.mLastNetworkId);
        printWriter.println("mOperationalMode " + this.mOperationalMode);
        printWriter.println("mUserWantsSuspendOpt " + this.mUserWantsSuspendOpt);
        printWriter.println("mSuspendOptNeedsDisabled " + this.mSuspendOptNeedsDisabled);
        this.mCountryCode.dump(fileDescriptor, printWriter, strArr);
        if (this.mNetworkFactory != null) {
            this.mNetworkFactory.dump(fileDescriptor, printWriter, strArr);
        } else {
            printWriter.println("mNetworkFactory is not initialized");
        }
        if (this.mUntrustedNetworkFactory != null) {
            this.mUntrustedNetworkFactory.dump(fileDescriptor, printWriter, strArr);
        } else {
            printWriter.println("mUntrustedNetworkFactory is not initialized");
        }
        printWriter.println("Wlan Wake Reasons:" + this.mWifiNative.getWlanWakeReasonCount());
        printWriter.println();
        this.mWifiConfigManager.dump(fileDescriptor, printWriter, strArr);
        printWriter.println();
        this.mPasspointManager.dump(printWriter);
        printWriter.println();
        this.mWifiDiagnostics.captureBugReportData(7);
        this.mWifiDiagnostics.dump(fileDescriptor, printWriter, strArr);
        dumpIpClient(fileDescriptor, printWriter, strArr);
        if (this.mWifiConnectivityManager != null) {
            this.mWifiConnectivityManager.dump(fileDescriptor, printWriter, strArr);
        } else {
            printWriter.println("mWifiConnectivityManager is not initialized");
        }
        this.mWifiInjector.getWakeupController().dump(fileDescriptor, printWriter, strArr);
    }

    public void handleUserSwitch(int i) {
        sendMessage(CMD_USER_SWITCH, i);
    }

    public void handleUserUnlock(int i) {
        sendMessage(CMD_USER_UNLOCK, i);
    }

    public void handleUserStop(int i) {
        sendMessage(CMD_USER_STOP, i);
    }

    private void logStateAndMessage(Message message, State state) {
        this.messageHandlingStatus = 0;
        if (this.mVerboseLoggingEnabled) {
            logd(" " + state.getClass().getSimpleName() + " " + getLogRecString(message));
        }
    }

    protected boolean recordLogRec(Message message) {
        if (message.what == CMD_RSSI_POLL) {
            return this.mVerboseLoggingEnabled;
        }
        return true;
    }

    protected String getLogRecString(Message message) {
        StringBuilder sb = new StringBuilder();
        if (this.mScreenOn) {
            sb.append("!");
        }
        if (this.messageHandlingStatus != 0) {
            sb.append("(");
            sb.append(this.messageHandlingStatus);
            sb.append(")");
        }
        sb.append(smToString(message));
        if (message.sendingUid > 0 && message.sendingUid != 1010) {
            sb.append(" uid=" + message.sendingUid);
        }
        sb.append(" rt=");
        sb.append(this.mClock.getUptimeSinceBootMillis());
        sb.append("/");
        sb.append(this.mClock.getElapsedSinceBootMillis());
        switch (message.what) {
            case CMD_ADD_OR_UPDATE_NETWORK:
                sb.append(" ");
                sb.append(Integer.toString(message.arg1));
                sb.append(" ");
                sb.append(Integer.toString(message.arg2));
                if (message.obj != null) {
                    WifiConfiguration wifiConfiguration = (WifiConfiguration) message.obj;
                    sb.append(" ");
                    sb.append(wifiConfiguration.configKey());
                    sb.append(" prio=");
                    sb.append(wifiConfiguration.priority);
                    sb.append(" status=");
                    sb.append(wifiConfiguration.status);
                    if (wifiConfiguration.BSSID != null) {
                        sb.append(" ");
                        sb.append(wifiConfiguration.BSSID);
                    }
                    WifiConfiguration currentWifiConfiguration = getCurrentWifiConfiguration();
                    if (currentWifiConfiguration != null) {
                        if (currentWifiConfiguration.configKey().equals(wifiConfiguration.configKey())) {
                            sb.append(" is current");
                        } else {
                            sb.append(" current=");
                            sb.append(currentWifiConfiguration.configKey());
                            sb.append(" prio=");
                            sb.append(currentWifiConfiguration.priority);
                            sb.append(" status=");
                            sb.append(currentWifiConfiguration.status);
                        }
                    }
                }
                break;
            case CMD_ENABLE_NETWORK:
            case 151569:
                sb.append(" ");
                sb.append(Integer.toString(message.arg1));
                sb.append(" ");
                sb.append(Integer.toString(message.arg2));
                String lastSelectedNetworkConfigKey = this.mWifiConfigManager.getLastSelectedNetworkConfigKey();
                if (lastSelectedNetworkConfigKey != null) {
                    sb.append(" last=");
                    sb.append(lastSelectedNetworkConfigKey);
                }
                WifiConfiguration configuredNetwork = this.mWifiConfigManager.getConfiguredNetwork(message.arg1);
                if (configuredNetwork != null && (lastSelectedNetworkConfigKey == null || !configuredNetwork.configKey().equals(lastSelectedNetworkConfigKey))) {
                    sb.append(" target=");
                    sb.append(lastSelectedNetworkConfigKey);
                }
                break;
            case CMD_GET_CONFIGURED_NETWORKS:
                sb.append(" ");
                sb.append(Integer.toString(message.arg1));
                sb.append(" ");
                sb.append(Integer.toString(message.arg2));
                sb.append(" num=");
                sb.append(this.mWifiConfigManager.getConfiguredNetworks().size());
                break;
            case CMD_RSSI_POLL:
            case CMD_UNWANTED_NETWORK:
            case 151572:
                sb.append(" ");
                sb.append(Integer.toString(message.arg1));
                sb.append(" ");
                sb.append(Integer.toString(message.arg2));
                if (this.mWifiInfo.getSSID() != null && this.mWifiInfo.getSSID() != null) {
                    sb.append(" ");
                    sb.append(this.mWifiInfo.getSSID());
                }
                if (this.mWifiInfo.getBSSID() != null) {
                    sb.append(" ");
                    sb.append(this.mWifiInfo.getBSSID());
                }
                sb.append(" rssi=");
                sb.append(this.mWifiInfo.getRssi());
                sb.append(" f=");
                sb.append(this.mWifiInfo.getFrequency());
                sb.append(" sc=");
                sb.append(this.mWifiInfo.score);
                sb.append(" link=");
                sb.append(this.mWifiInfo.getLinkSpeed());
                sb.append(String.format(" tx=%.1f,", Double.valueOf(this.mWifiInfo.txSuccessRate)));
                sb.append(String.format(" %.1f,", Double.valueOf(this.mWifiInfo.txRetriesRate)));
                sb.append(String.format(" %.1f ", Double.valueOf(this.mWifiInfo.txBadRate)));
                sb.append(String.format(" rx=%.1f", Double.valueOf(this.mWifiInfo.rxSuccessRate)));
                sb.append(String.format(" bcn=%d", Integer.valueOf(this.mRunningBeaconCount)));
                String strReportOnTime = reportOnTime();
                if (strReportOnTime != null) {
                    sb.append(" ");
                    sb.append(strReportOnTime);
                }
                sb.append(String.format(" score=%d", Integer.valueOf(this.mWifiInfo.score)));
                break;
            case CMD_ROAM_WATCHDOG_TIMER:
                sb.append(" ");
                sb.append(Integer.toString(message.arg1));
                sb.append(" ");
                sb.append(Integer.toString(message.arg2));
                sb.append(" cur=");
                sb.append(this.roamWatchdogCount);
                break;
            case CMD_DISCONNECTING_WATCHDOG_TIMER:
                sb.append(" ");
                sb.append(Integer.toString(message.arg1));
                sb.append(" ");
                sb.append(Integer.toString(message.arg2));
                sb.append(" cur=");
                sb.append(this.disconnectingWatchdogCount);
                break;
            case CMD_DISABLE_P2P_WATCHDOG_TIMER:
                sb.append(" ");
                sb.append(Integer.toString(message.arg1));
                sb.append(" ");
                sb.append(Integer.toString(message.arg2));
                sb.append(" cur=");
                sb.append(this.mDisableP2pWatchdogCount);
                break;
            case CMD_IP_CONFIGURATION_LOST:
                WifiConfiguration currentWifiConfiguration2 = getCurrentWifiConfiguration();
                int disableReasonCounter = currentWifiConfiguration2 != null ? currentWifiConfiguration2.getNetworkSelectionStatus().getDisableReasonCounter(4) : -1;
                sb.append(" ");
                sb.append(Integer.toString(message.arg1));
                sb.append(" ");
                sb.append(Integer.toString(message.arg2));
                sb.append(" failures: ");
                sb.append(Integer.toString(disableReasonCounter));
                sb.append("/");
                sb.append(Integer.toString(this.mFacade.getIntegerSetting(this.mContext, "wifi_max_dhcp_retry_count", 0)));
                if (this.mWifiInfo.getBSSID() != null) {
                    sb.append(" ");
                    sb.append(this.mWifiInfo.getBSSID());
                }
                sb.append(String.format(" bcn=%d", Integer.valueOf(this.mRunningBeaconCount)));
                break;
            case CMD_UPDATE_LINKPROPERTIES:
                sb.append(" ");
                sb.append(Integer.toString(message.arg1));
                sb.append(" ");
                sb.append(Integer.toString(message.arg2));
                if (this.mLinkProperties != null) {
                    sb.append(" ");
                    sb.append(getLinkPropertiesSummary(this.mLinkProperties));
                }
                break;
            case CMD_TARGET_BSSID:
            case CMD_ASSOCIATED_BSSID:
                sb.append(" ");
                sb.append(Integer.toString(message.arg1));
                sb.append(" ");
                sb.append(Integer.toString(message.arg2));
                if (message.obj != null) {
                    sb.append(" BSSID=");
                    sb.append((String) message.obj);
                }
                if (this.mTargetRoamBSSID != null) {
                    sb.append(" Target=");
                    sb.append(this.mTargetRoamBSSID);
                }
                sb.append(" roam=");
                sb.append(Boolean.toString(this.mIsAutoRoaming));
                break;
            case CMD_START_CONNECT:
            case 151553:
                sb.append(" ");
                sb.append(Integer.toString(message.arg1));
                sb.append(" ");
                sb.append(Integer.toString(message.arg2));
                WifiConfiguration configuredNetwork2 = this.mWifiConfigManager.getConfiguredNetwork(message.arg1);
                if (configuredNetwork2 != null) {
                    sb.append(" ");
                    sb.append(configuredNetwork2.configKey());
                }
                if (this.mTargetRoamBSSID != null) {
                    sb.append(" ");
                    sb.append(this.mTargetRoamBSSID);
                }
                sb.append(" roam=");
                sb.append(Boolean.toString(this.mIsAutoRoaming));
                WifiConfiguration currentWifiConfiguration3 = getCurrentWifiConfiguration();
                if (currentWifiConfiguration3 != null) {
                    sb.append(currentWifiConfiguration3.configKey());
                }
                break;
            case CMD_START_ROAM:
                sb.append(" ");
                sb.append(Integer.toString(message.arg1));
                sb.append(" ");
                sb.append(Integer.toString(message.arg2));
                ScanResult scanResult = (ScanResult) message.obj;
                if (scanResult != null) {
                    Long lValueOf = Long.valueOf(this.mClock.getWallClockMillis());
                    sb.append(" bssid=");
                    sb.append(scanResult.BSSID);
                    sb.append(" rssi=");
                    sb.append(scanResult.level);
                    sb.append(" freq=");
                    sb.append(scanResult.frequency);
                    if (scanResult.seen > 0 && scanResult.seen < lValueOf.longValue()) {
                        sb.append(" seen=");
                        sb.append(lValueOf.longValue() - scanResult.seen);
                    } else {
                        sb.append(" !seen=");
                        sb.append(scanResult.seen);
                    }
                }
                if (this.mTargetRoamBSSID != null) {
                    sb.append(" ");
                    sb.append(this.mTargetRoamBSSID);
                }
                sb.append(" roam=");
                sb.append(Boolean.toString(this.mIsAutoRoaming));
                sb.append(" fail count=");
                sb.append(Integer.toString(this.mRoamFailCount));
                break;
            case CMD_IP_REACHABILITY_LOST:
                if (message.obj != null) {
                    sb.append(" ");
                    sb.append((String) message.obj);
                }
                break;
            case CMD_START_RSSI_MONITORING_OFFLOAD:
            case CMD_STOP_RSSI_MONITORING_OFFLOAD:
            case CMD_RSSI_THRESHOLD_BREACHED:
                sb.append(" rssi=");
                sb.append(Integer.toString(message.arg1));
                sb.append(" thresholds=");
                sb.append(Arrays.toString(this.mRssiRanges));
                break;
            case CMD_IPV4_PROVISIONING_SUCCESS:
                sb.append(" ");
                if (message.arg1 == 1) {
                    sb.append("DHCP_OK");
                } else if (message.arg1 == CMD_STATIC_IP_SUCCESS) {
                    sb.append("STATIC_OK");
                } else {
                    sb.append(Integer.toString(message.arg1));
                }
                break;
            case CMD_IPV4_PROVISIONING_FAILURE:
                sb.append(" ");
                if (message.arg1 == 2) {
                    sb.append("DHCP_FAIL");
                } else if (message.arg1 == CMD_STATIC_IP_FAILURE) {
                    sb.append("STATIC_FAIL");
                } else {
                    sb.append(Integer.toString(message.arg1));
                }
                break;
            case CMD_INSTALL_PACKET_FILTER:
                sb.append(" len=" + ((byte[]) message.obj).length);
                break;
            case CMD_SET_FALLBACK_PACKET_FILTERING:
                sb.append(" enabled=" + ((Boolean) message.obj).booleanValue());
                break;
            case CMD_USER_SWITCH:
                sb.append(" userId=");
                sb.append(Integer.toString(message.arg1));
                break;
            case WifiP2pServiceImpl.P2P_CONNECTION_CHANGED:
                sb.append(" ");
                sb.append(Integer.toString(message.arg1));
                sb.append(" ");
                sb.append(Integer.toString(message.arg2));
                if (message.obj != null) {
                    NetworkInfo networkInfo = (NetworkInfo) message.obj;
                    NetworkInfo.State state = networkInfo.getState();
                    NetworkInfo.DetailedState detailedState = networkInfo.getDetailedState();
                    if (state != null) {
                        sb.append(" st=");
                        sb.append(state);
                    }
                    if (detailedState != null) {
                        sb.append("/");
                        sb.append(detailedState);
                    }
                }
                break;
            case WifiMonitor.NETWORK_CONNECTION_EVENT:
                sb.append(" ");
                sb.append(Integer.toString(message.arg1));
                sb.append(" ");
                sb.append(Integer.toString(message.arg2));
                sb.append(" ");
                sb.append(this.mLastBssid);
                sb.append(" nid=");
                sb.append(this.mLastNetworkId);
                WifiConfiguration currentWifiConfiguration4 = getCurrentWifiConfiguration();
                if (currentWifiConfiguration4 != null) {
                    sb.append(" ");
                    sb.append(currentWifiConfiguration4.configKey());
                }
                String lastSelectedNetworkConfigKey2 = this.mWifiConfigManager.getLastSelectedNetworkConfigKey();
                if (lastSelectedNetworkConfigKey2 != null) {
                    sb.append(" last=");
                    sb.append(lastSelectedNetworkConfigKey2);
                }
                break;
            case WifiMonitor.NETWORK_DISCONNECTION_EVENT:
                if (message.obj != null) {
                    sb.append(" ");
                    sb.append((String) message.obj);
                }
                sb.append(" nid=");
                sb.append(message.arg1);
                sb.append(" reason=");
                sb.append(message.arg2);
                if (this.mLastBssid != null) {
                    sb.append(" lastbssid=");
                    sb.append(this.mLastBssid);
                }
                if (this.mWifiInfo.getFrequency() != -1) {
                    sb.append(" freq=");
                    sb.append(this.mWifiInfo.getFrequency());
                    sb.append(" rssi=");
                    sb.append(this.mWifiInfo.getRssi());
                }
                break;
            case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT:
                sb.append(" ");
                sb.append(Integer.toString(message.arg1));
                sb.append(" ");
                sb.append(Integer.toString(message.arg2));
                StateChangeResult stateChangeResult = (StateChangeResult) message.obj;
                if (stateChangeResult != null) {
                    sb.append(stateChangeResult.toString());
                }
                break;
            case WifiMonitor.ASSOCIATION_REJECTION_EVENT:
                sb.append(" ");
                sb.append(" timedOut=" + Integer.toString(message.arg1));
                sb.append(" ");
                sb.append(Integer.toString(message.arg2));
                String str = (String) message.obj;
                if (str != null && str.length() > 0) {
                    sb.append(" ");
                    sb.append(str);
                }
                sb.append(" blacklist=" + Boolean.toString(this.didBlackListBSSID));
                break;
            case 151556:
                sb.append(" ");
                sb.append(Integer.toString(message.arg1));
                sb.append(" ");
                sb.append(Integer.toString(message.arg2));
                WifiConfiguration wifiConfiguration2 = (WifiConfiguration) message.obj;
                if (wifiConfiguration2 != null) {
                    sb.append(" ");
                    sb.append(wifiConfiguration2.configKey());
                    sb.append(" nid=");
                    sb.append(wifiConfiguration2.networkId);
                    if (wifiConfiguration2.hiddenSSID) {
                        sb.append(" hidden");
                    }
                    if (wifiConfiguration2.preSharedKey != null) {
                        sb.append(" hasPSK");
                    }
                    if (wifiConfiguration2.ephemeral) {
                        sb.append(" ephemeral");
                    }
                    if (wifiConfiguration2.selfAdded) {
                        sb.append(" selfAdded");
                    }
                    sb.append(" cuid=");
                    sb.append(wifiConfiguration2.creatorUid);
                    sb.append(" suid=");
                    sb.append(wifiConfiguration2.lastUpdateUid);
                    WifiConfiguration.NetworkSelectionStatus networkSelectionStatus = wifiConfiguration2.getNetworkSelectionStatus();
                    sb.append(" ajst=");
                    sb.append(networkSelectionStatus.getNetworkStatusString());
                }
                break;
            case 151559:
                sb.append(" ");
                sb.append(Integer.toString(message.arg1));
                sb.append(" ");
                sb.append(Integer.toString(message.arg2));
                WifiConfiguration wifiConfiguration3 = (WifiConfiguration) message.obj;
                if (wifiConfiguration3 != null) {
                    sb.append(" ");
                    sb.append(wifiConfiguration3.configKey());
                    sb.append(" nid=");
                    sb.append(wifiConfiguration3.networkId);
                    if (wifiConfiguration3.hiddenSSID) {
                        sb.append(" hidden");
                    }
                    if (wifiConfiguration3.preSharedKey != null && !wifiConfiguration3.preSharedKey.equals("*")) {
                        sb.append(" hasPSK");
                    }
                    if (wifiConfiguration3.ephemeral) {
                        sb.append(" ephemeral");
                    }
                    if (wifiConfiguration3.selfAdded) {
                        sb.append(" selfAdded");
                    }
                    sb.append(" cuid=");
                    sb.append(wifiConfiguration3.creatorUid);
                    sb.append(" suid=");
                    sb.append(wifiConfiguration3.lastUpdateUid);
                }
                break;
            case 196611:
                sb.append(" ");
                sb.append(Integer.toString(message.arg1));
                sb.append(" ");
                sb.append(Integer.toString(message.arg2));
                sb.append(" txpkts=");
                sb.append(this.mWifiInfo.txSuccess);
                sb.append(",");
                sb.append(this.mWifiInfo.txBad);
                sb.append(",");
                sb.append(this.mWifiInfo.txRetries);
                break;
            case 196612:
                sb.append(" ");
                sb.append(Integer.toString(message.arg1));
                sb.append(" ");
                sb.append(Integer.toString(message.arg2));
                if (message.arg1 == 1) {
                    sb.append(" OK ");
                } else if (message.arg1 == 2) {
                    sb.append(" FAIL ");
                }
                if (this.mLinkProperties != null) {
                    sb.append(" ");
                    sb.append(getLinkPropertiesSummary(this.mLinkProperties));
                }
                break;
            default:
                sb.append(" ");
                sb.append(Integer.toString(message.arg1));
                sb.append(" ");
                sb.append(Integer.toString(message.arg2));
                break;
        }
        return sb.toString();
    }

    private void handleScreenStateChanged(boolean z) {
        this.mScreenOn = z;
        if (this.mVerboseLoggingEnabled) {
            logd(" handleScreenStateChanged Enter: screenOn=" + z + " mUserWantsSuspendOpt=" + this.mUserWantsSuspendOpt + " state " + getCurrentState().getName() + " suppState:" + this.mSupplicantStateTracker.getSupplicantStateName());
        }
        enableRssiPolling(z);
        if (this.mUserWantsSuspendOpt.get()) {
            int i = 0;
            if (z) {
                sendMessage(CMD_SET_SUSPEND_OPT_ENABLED, 0, 0);
            } else {
                if (isConnected()) {
                    this.mSuspendWakeLock.acquire(2000L);
                    i = 1;
                }
                sendMessage(CMD_SET_SUSPEND_OPT_ENABLED, 1, i);
            }
        }
        getWifiLinkLayerStats();
        this.mOnTimeScreenStateChange = this.mOnTime;
        this.lastScreenStateChangeTimeStamp = this.lastLinkLayerStatsUpdate;
        this.mWifiMetrics.setScreenState(z);
        if (this.mWifiConnectivityManager != null) {
            this.mWifiConnectivityManager.handleScreenStateChanged(z);
        }
        if (this.mVerboseLoggingEnabled) {
            log("handleScreenStateChanged Exit: " + z);
        }
    }

    private void checkAndSetConnectivityInstance() {
        if (this.mCm == null) {
            this.mCm = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        }
    }

    private void setSuspendOptimizationsNative(int i, boolean z) {
        if (this.mVerboseLoggingEnabled) {
            log("setSuspendOptimizationsNative: " + i + " " + z + " -want " + this.mUserWantsSuspendOpt.get() + " stack:" + Thread.currentThread().getStackTrace()[2].getMethodName() + " - " + Thread.currentThread().getStackTrace()[3].getMethodName() + " - " + Thread.currentThread().getStackTrace()[4].getMethodName() + " - " + Thread.currentThread().getStackTrace()[5].getMethodName());
        }
        if (z) {
            this.mSuspendOptNeedsDisabled &= ~i;
            if (this.mSuspendOptNeedsDisabled == 0 && this.mUserWantsSuspendOpt.get()) {
                if (this.mVerboseLoggingEnabled) {
                    log("setSuspendOptimizationsNative do it " + i + " " + z + " stack:" + Thread.currentThread().getStackTrace()[2].getMethodName() + " - " + Thread.currentThread().getStackTrace()[3].getMethodName() + " - " + Thread.currentThread().getStackTrace()[4].getMethodName() + " - " + Thread.currentThread().getStackTrace()[5].getMethodName());
                }
                this.mWifiNative.setSuspendOptimizations(this.mInterfaceName, true);
                return;
            }
            return;
        }
        this.mSuspendOptNeedsDisabled = i | this.mSuspendOptNeedsDisabled;
        this.mWifiNative.setSuspendOptimizations(this.mInterfaceName, false);
    }

    private void setSuspendOptimizations(int i, boolean z) {
        if (this.mVerboseLoggingEnabled) {
            log("setSuspendOptimizations: " + i + " " + z);
        }
        if (z) {
            this.mSuspendOptNeedsDisabled = (~i) & this.mSuspendOptNeedsDisabled;
        } else {
            this.mSuspendOptNeedsDisabled = i | this.mSuspendOptNeedsDisabled;
        }
        if (this.mVerboseLoggingEnabled) {
            log("mSuspendOptNeedsDisabled " + this.mSuspendOptNeedsDisabled);
        }
    }

    private void fetchRssiLinkSpeedAndFrequencyNative() {
        WifiNative.SignalPollResult signalPollResultSignalPoll = this.mWifiNative.signalPoll(this.mInterfaceName);
        if (signalPollResultSignalPoll == null) {
            return;
        }
        Integer numValueOf = Integer.valueOf(signalPollResultSignalPoll.currentRssi);
        Integer numValueOf2 = Integer.valueOf(signalPollResultSignalPoll.txBitrate);
        Integer numValueOf3 = Integer.valueOf(signalPollResultSignalPoll.associationFrequency);
        if (this.mVerboseLoggingEnabled) {
            logd("fetchRssiLinkSpeedAndFrequencyNative rssi=" + numValueOf + " linkspeed=" + numValueOf2 + " freq=" + numValueOf3);
        }
        if (numValueOf == null || numValueOf.intValue() <= -127 || numValueOf.intValue() >= 200) {
            this.mWifiInfo.setRssi(WifiMetrics.MIN_RSSI_DELTA);
            updateCapabilities();
        } else {
            if (numValueOf.intValue() > 0) {
                numValueOf = Integer.valueOf(numValueOf.intValue() - 256);
            }
            this.mWifiInfo.setRssi(numValueOf.intValue());
            int iCalculateSignalLevel = WifiManager.calculateSignalLevel(numValueOf.intValue(), 5);
            if (iCalculateSignalLevel != this.mLastSignalLevel) {
                updateCapabilities();
                sendRssiChangeBroadcast(numValueOf.intValue());
            }
            this.mLastSignalLevel = iCalculateSignalLevel;
        }
        MtkWifiServiceAdapter.updateRSSI(numValueOf, this.mWifiInfo.getIpAddress(), this.mLastNetworkId);
        if (numValueOf2 != null) {
            this.mWifiInfo.setLinkSpeed(numValueOf2.intValue());
        }
        if (numValueOf3 != null && numValueOf3.intValue() > 0) {
            this.mWifiInfo.setFrequency(numValueOf3.intValue());
        }
        this.mWifiConfigManager.updateScanDetailCacheFromWifiInfo(this.mWifiInfo);
        if (numValueOf != null && numValueOf2 != null && numValueOf3 != null) {
            this.mWifiMetrics.handlePollResult(this.mWifiInfo);
        }
    }

    private void cleanWifiScore() {
        this.mWifiInfo.txBadRate = 0.0d;
        this.mWifiInfo.txSuccessRate = 0.0d;
        this.mWifiInfo.txRetriesRate = 0.0d;
        this.mWifiInfo.rxSuccessRate = 0.0d;
        this.mWifiScoreReport.reset();
    }

    private void updateLinkProperties(LinkProperties linkProperties) {
        if (this.mVerboseLoggingEnabled) {
            log("Link configuration changed for netId: " + this.mLastNetworkId + " old: " + this.mLinkProperties + " new: " + linkProperties);
        }
        this.mLinkProperties = linkProperties;
        if (this.mNetworkAgent != null) {
            this.mNetworkAgent.sendLinkProperties(this.mLinkProperties);
        }
        if (getNetworkDetailedState() == NetworkInfo.DetailedState.CONNECTED) {
            sendLinkConfigurationChangedBroadcast();
        }
        if (this.mVerboseLoggingEnabled) {
            StringBuilder sb = new StringBuilder();
            sb.append("updateLinkProperties nid: " + this.mLastNetworkId);
            sb.append(" state: " + getNetworkDetailedState());
            if (this.mLinkProperties != null) {
                sb.append(" ");
                sb.append(getLinkPropertiesSummary(this.mLinkProperties));
            }
            logd(sb.toString());
        }
    }

    private void clearLinkProperties() {
        synchronized (this.mDhcpResultsLock) {
            if (this.mDhcpResults != null) {
                this.mDhcpResults.clear();
            }
        }
        this.mLinkProperties.clear();
        if (this.mNetworkAgent != null) {
            this.mNetworkAgent.sendLinkProperties(this.mLinkProperties);
        }
    }

    private String updateDefaultRouteMacAddress(int i) throws Throwable {
        String strMacAddressFromRoute = null;
        for (RouteInfo routeInfo : this.mLinkProperties.getRoutes()) {
            if (routeInfo.isDefaultRoute() && routeInfo.hasGateway()) {
                InetAddress gateway = routeInfo.getGateway();
                if (gateway instanceof Inet4Address) {
                    if (this.mVerboseLoggingEnabled) {
                        logd("updateDefaultRouteMacAddress found Ipv4 default :" + gateway.getHostAddress());
                    }
                    strMacAddressFromRoute = macAddressFromRoute(gateway.getHostAddress());
                    if (strMacAddressFromRoute == null && i > 0) {
                        TrafficStats.setThreadStatsTag(-190);
                        try {
                            try {
                                boolean zIsReachable = gateway.isReachable(i);
                                TrafficStats.clearThreadStatsTag();
                                if (zIsReachable) {
                                    strMacAddressFromRoute = macAddressFromRoute(gateway.getHostAddress());
                                    if (this.mVerboseLoggingEnabled) {
                                        logd("updateDefaultRouteMacAddress reachable (tried again) :" + gateway.getHostAddress() + " found " + strMacAddressFromRoute);
                                    }
                                }
                            } catch (Exception e) {
                                loge("updateDefaultRouteMacAddress exception reaching :" + gateway.getHostAddress());
                                TrafficStats.clearThreadStatsTag();
                            }
                        } catch (Throwable th) {
                            TrafficStats.clearThreadStatsTag();
                            throw th;
                        }
                    }
                    if (strMacAddressFromRoute != null) {
                        this.mWifiConfigManager.setNetworkDefaultGwMacAddress(this.mLastNetworkId, strMacAddressFromRoute);
                    }
                }
            }
        }
        return strMacAddressFromRoute;
    }

    private void sendRssiChangeBroadcast(int i) {
        try {
            this.mBatteryStats.noteWifiRssiChanged(i);
        } catch (RemoteException e) {
        }
        Intent intent = new Intent("android.net.wifi.RSSI_CHANGED");
        intent.addFlags(67108864);
        intent.putExtra("newRssi", i);
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void sendNetworkStateChangeBroadcast(String str) {
        Intent intent = new Intent("android.net.wifi.STATE_CHANGE");
        intent.addFlags(67108864);
        NetworkInfo networkInfo = new NetworkInfo(this.mNetworkInfo);
        networkInfo.setExtraInfo(null);
        intent.putExtra("networkInfo", networkInfo);
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void sendLinkConfigurationChangedBroadcast() {
        Intent intent = new Intent("android.net.wifi.LINK_CONFIGURATION_CHANGED");
        intent.addFlags(67108864);
        intent.putExtra("linkProperties", new LinkProperties(this.mLinkProperties));
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void sendSupplicantConnectionChangedBroadcast(boolean z) {
        Intent intent = new Intent("android.net.wifi.supplicant.CONNECTION_CHANGE");
        intent.addFlags(67108864);
        intent.putExtra("connected", z);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    private boolean setNetworkDetailedState(NetworkInfo.DetailedState detailedState) {
        boolean z = this.mIsAutoRoaming;
        if (this.mVerboseLoggingEnabled) {
            log("setDetailed state, old =" + this.mNetworkInfo.getDetailedState() + " and new state=" + detailedState + " hidden=" + z);
        }
        if (z || detailedState == this.mNetworkInfo.getDetailedState()) {
            return false;
        }
        this.mNetworkInfo.setDetailedState(detailedState, null, null);
        if (this.mNetworkAgent != null) {
            this.mNetworkAgent.sendNetworkInfo(this.mNetworkInfo);
        }
        sendNetworkStateChangeBroadcast(null);
        return true;
    }

    private NetworkInfo.DetailedState getNetworkDetailedState() {
        return this.mNetworkInfo.getDetailedState();
    }

    private SupplicantState handleSupplicantStateChange(Message message) {
        ScanDetail scanDetail;
        StateChangeResult stateChangeResult = (StateChangeResult) message.obj;
        SupplicantState supplicantState = stateChangeResult.state;
        this.mWifiInfo.setSupplicantState(supplicantState);
        if (SupplicantState.isConnecting(supplicantState)) {
            this.mWifiInfo.setNetworkId(stateChangeResult.networkId);
            this.mWifiInfo.setBSSID(stateChangeResult.BSSID);
            this.mWifiInfo.setSSID(stateChangeResult.wifiSsid);
        } else {
            this.mWifiInfo.setNetworkId(-1);
            this.mWifiInfo.setBSSID(null);
            this.mWifiInfo.setSSID(null);
        }
        updateCapabilities();
        WifiConfiguration currentWifiConfiguration = getCurrentWifiConfiguration();
        if (currentWifiConfiguration != null) {
            this.mWifiInfo.setEphemeral(currentWifiConfiguration.ephemeral);
            ScanDetailCache scanDetailCacheForNetwork = this.mWifiConfigManager.getScanDetailCacheForNetwork(currentWifiConfiguration.networkId);
            if (scanDetailCacheForNetwork != null && (scanDetail = scanDetailCacheForNetwork.getScanDetail(stateChangeResult.BSSID)) != null) {
                this.mWifiInfo.setFrequency(scanDetail.getScanResult().frequency);
                NetworkDetail networkDetail = scanDetail.getNetworkDetail();
                if (networkDetail != null && networkDetail.getAnt() == NetworkDetail.Ant.ChargeablePublic) {
                    this.mWifiInfo.setMeteredHint(true);
                }
            }
        }
        this.mSupplicantStateTracker.sendMessage(Message.obtain(message));
        return supplicantState;
    }

    private void handleNetworkDisconnect() {
        if (this.mVerboseLoggingEnabled) {
            log("handleNetworkDisconnect: Stopping DHCP and clearing IP stack:" + Thread.currentThread().getStackTrace()[2].getMethodName() + " - " + Thread.currentThread().getStackTrace()[3].getMethodName() + " - " + Thread.currentThread().getStackTrace()[4].getMethodName() + " - " + Thread.currentThread().getStackTrace()[5].getMethodName());
        }
        stopRssiMonitoringOffload();
        clearTargetBssid("handleNetworkDisconnect");
        stopIpClient();
        this.mWifiScoreReport.reset();
        this.mWifiInfo.reset();
        this.mIsAutoRoaming = false;
        setNetworkDetailedState(NetworkInfo.DetailedState.DISCONNECTED);
        if (this.mNetworkAgent != null) {
            this.mNetworkAgent.sendNetworkInfo(this.mNetworkInfo);
            this.mNetworkAgent = null;
        }
        clearLinkProperties();
        sendNetworkStateChangeBroadcast(this.mLastBssid);
        this.mLastBssid = null;
        registerDisconnected();
        this.mLastNetworkId = -1;
    }

    void handlePreDhcpSetup() {
        if (!this.mBluetoothConnectionActive) {
            this.mWifiNative.setBluetoothCoexistenceMode(this.mInterfaceName, 1);
        }
        setSuspendOptimizationsNative(1, false);
        this.mWifiNative.setPowerSave(this.mInterfaceName, false);
        getWifiLinkLayerStats();
        if (this.mWifiP2pChannel != null) {
            Message message = new Message();
            message.what = WifiP2pServiceImpl.BLOCK_DISCOVERY;
            message.arg1 = 1;
            message.arg2 = 196614;
            message.obj = this;
            this.mWifiP2pChannel.sendMessage(message);
            return;
        }
        sendMessage(196614);
    }

    void handlePostDhcpSetup() {
        setSuspendOptimizationsNative(1, true);
        this.mWifiNative.setPowerSave(this.mInterfaceName, true);
        p2pSendMessage(WifiP2pServiceImpl.BLOCK_DISCOVERY, 0);
        this.mWifiNative.setBluetoothCoexistenceMode(this.mInterfaceName, 2);
    }

    private void reportConnectionAttemptStart(WifiConfiguration wifiConfiguration, String str, int i) {
        this.mWifiMetrics.startConnectionEvent(wifiConfiguration, str, i);
        this.mDiagsConnectionStartMillis = this.mClock.getElapsedSinceBootMillis();
        this.mWifiDiagnostics.reportConnectionEvent(this.mDiagsConnectionStartMillis, (byte) 0);
        this.mWrongPasswordNotifier.onNewConnectionAttempt();
        sendMessageDelayed(CMD_DIAGS_CONNECT_TIMEOUT, Long.valueOf(this.mDiagsConnectionStartMillis), 60000L);
    }

    private void reportConnectionAttemptEnd(int i, int i2) {
        this.mWifiMetrics.endConnectionEvent(i, i2);
        this.mWifiConnectivityManager.handleConnectionAttemptEnded(i);
        if (i == 1) {
            this.mWifiDiagnostics.reportConnectionEvent(this.mDiagsConnectionStartMillis, (byte) 1);
        } else if (i != 5 && i != 8) {
            this.mWifiDiagnostics.reportConnectionEvent(this.mDiagsConnectionStartMillis, (byte) 2);
        }
        this.mDiagsConnectionStartMillis = -1L;
    }

    private void handleIPv4Success(DhcpResults dhcpResults) {
        Inet4Address inet4Address;
        if (this.mVerboseLoggingEnabled) {
            logd("handleIPv4Success <" + dhcpResults.toString() + ">");
            StringBuilder sb = new StringBuilder();
            sb.append("link address ");
            sb.append(dhcpResults.ipAddress);
            logd(sb.toString());
        }
        synchronized (this.mDhcpResultsLock) {
            this.mDhcpResults = dhcpResults;
            inet4Address = (Inet4Address) dhcpResults.ipAddress.getAddress();
        }
        if (this.mIsAutoRoaming && this.mWifiInfo.getIpAddress() != NetworkUtils.inetAddressToInt(inet4Address)) {
            logd("handleIPv4Success, roaming and address changed" + this.mWifiInfo + " got: " + inet4Address);
        }
        this.mWifiInfo.setInetAddress(inet4Address);
        WifiConfiguration currentWifiConfiguration = getCurrentWifiConfiguration();
        if (currentWifiConfiguration != null) {
            this.mWifiInfo.setEphemeral(currentWifiConfiguration.ephemeral);
        }
        if (dhcpResults.hasMeteredHint()) {
            this.mWifiInfo.setMeteredHint(true);
        }
        updateCapabilities(currentWifiConfiguration);
    }

    private void handleSuccessfulIpConfiguration() {
        this.mLastSignalLevel = -1;
        WifiConfiguration currentWifiConfiguration = getCurrentWifiConfiguration();
        if (currentWifiConfiguration != null) {
            currentWifiConfiguration.getNetworkSelectionStatus().clearDisableReasonCounter(4);
            updateCapabilities(currentWifiConfiguration);
        }
    }

    private void handleIPv4Failure() {
        this.mWifiDiagnostics.captureBugReportData(4);
        if (this.mVerboseLoggingEnabled) {
            int disableReasonCounter = -1;
            WifiConfiguration currentWifiConfiguration = getCurrentWifiConfiguration();
            if (currentWifiConfiguration != null) {
                disableReasonCounter = currentWifiConfiguration.getNetworkSelectionStatus().getDisableReasonCounter(4);
            }
            log("DHCP failure count=" + disableReasonCounter);
        }
        reportConnectionAttemptEnd(10, 2);
        synchronized (this.mDhcpResultsLock) {
            if (this.mDhcpResults != null) {
                this.mDhcpResults.clear();
            }
        }
        if (this.mVerboseLoggingEnabled) {
            logd("handleIPv4Failure");
        }
    }

    private void handleIpConfigurationLost() {
        this.mWifiInfo.setInetAddress(null);
        this.mWifiInfo.setMeteredHint(false);
        this.mWifiConfigManager.updateNetworkSelectionStatus(this.mLastNetworkId, 4);
        this.mWifiNative.disconnect(this.mInterfaceName);
        MtkWifiApmDelegate.getInstance().broadcastProvisionFail();
    }

    private void handleIpReachabilityLost() {
        this.mWifiInfo.setInetAddress(null);
        this.mWifiInfo.setMeteredHint(false);
        this.mWifiNative.disconnect(this.mInterfaceName);
    }

    private java.lang.String macAddressFromRoute(java.lang.String r6) throws java.lang.Throwable {
        r0 = 0;
        r0 = 0;
        r0 = 0;
        r0 = 0;
        r0 = 0;
        r0 = 0;
        r1 = new java.io.BufferedReader(new java.io.FileReader("/proc/net/arp"));
        r1.readLine();
        while (true) {
            r2 = r1.readLine();
            if (r2 != null) {
                r2 = r2.split("[ ]+");
                if (r2.length < 6) {
                } else {
                    r3 = r2[0];
                    r2 = r2[3];
                    if (r6.equals(r3)) {
                        r0 = r2;
                    }
                }
            }
        }
        if (r0 == 0) {
            r2 = new java.lang.StringBuilder();
            r2.append("Did not find remoteAddress {");
            r2.append(r6);
            r2.append("} in /proc/net/arp");
            loge(r2.toString());
        }
        r1.close();
        r6 = r0;
        while (true) {
            return r6;
        }
    }

    private boolean isPermanentWrongPasswordFailure(int i, int i2) {
        if (i2 != 2) {
            return false;
        }
        WifiConfiguration configuredNetwork = this.mWifiConfigManager.getConfiguredNetwork(i);
        if (configuredNetwork != null && configuredNetwork.getNetworkSelectionStatus().getHasEverConnected()) {
            return false;
        }
        return true;
    }

    private class WifiNetworkFactory extends NetworkFactory {
        public WifiNetworkFactory(Looper looper, Context context, String str, NetworkCapabilities networkCapabilities) {
            super(looper, context, str, networkCapabilities);
        }

        protected void needNetworkFor(NetworkRequest networkRequest, int i) {
            synchronized (WifiStateMachine.this.mWifiReqCountLock) {
                if (WifiStateMachine.access$1004(WifiStateMachine.this) == 1 && WifiStateMachine.this.mWifiConnectivityManager != null && WifiStateMachine.this.mUntrustedReqCount == 0) {
                    WifiStateMachine.this.mWifiConnectivityManager.enable(true);
                }
            }
        }

        protected void releaseNetworkFor(NetworkRequest networkRequest) {
            synchronized (WifiStateMachine.this.mWifiReqCountLock) {
                if (WifiStateMachine.access$1006(WifiStateMachine.this) == 0 && WifiStateMachine.this.mWifiConnectivityManager != null && WifiStateMachine.this.mUntrustedReqCount == 0) {
                    WifiStateMachine.this.mWifiConnectivityManager.enable(false);
                }
            }
        }

        public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
            printWriter.println("mConnectionReqCount " + WifiStateMachine.this.mConnectionReqCount);
        }
    }

    private class UntrustedWifiNetworkFactory extends NetworkFactory {
        public UntrustedWifiNetworkFactory(Looper looper, Context context, String str, NetworkCapabilities networkCapabilities) {
            super(looper, context, str, networkCapabilities);
        }

        protected void needNetworkFor(NetworkRequest networkRequest, int i) {
            if (!networkRequest.networkCapabilities.hasCapability(14)) {
                synchronized (WifiStateMachine.this.mWifiReqCountLock) {
                    if (WifiStateMachine.access$1204(WifiStateMachine.this) == 1 && WifiStateMachine.this.mWifiConnectivityManager != null) {
                        if (WifiStateMachine.this.mConnectionReqCount == 0) {
                            WifiStateMachine.this.mWifiConnectivityManager.enable(true);
                        }
                        WifiStateMachine.this.mWifiConnectivityManager.setUntrustedConnectionAllowed(true);
                    }
                }
            }
        }

        protected void releaseNetworkFor(NetworkRequest networkRequest) {
            if (!networkRequest.networkCapabilities.hasCapability(14)) {
                synchronized (WifiStateMachine.this.mWifiReqCountLock) {
                    if (WifiStateMachine.access$1206(WifiStateMachine.this) == 0 && WifiStateMachine.this.mWifiConnectivityManager != null) {
                        WifiStateMachine.this.mWifiConnectivityManager.setUntrustedConnectionAllowed(false);
                        if (WifiStateMachine.this.mConnectionReqCount == 0) {
                            WifiStateMachine.this.mWifiConnectivityManager.enable(false);
                        }
                    }
                }
            }
        }

        public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
            printWriter.println("mUntrustedReqCount " + WifiStateMachine.this.mUntrustedReqCount);
        }
    }

    void maybeRegisterNetworkFactory() {
        if (this.mNetworkFactory == null) {
            checkAndSetConnectivityInstance();
            if (this.mCm != null) {
                this.mNetworkFactory = new WifiNetworkFactory(getHandler().getLooper(), this.mContext, NETWORKTYPE, this.mNetworkCapabilitiesFilter);
                this.mNetworkFactory.setScoreFilter(60);
                this.mNetworkFactory.register();
                this.mUntrustedNetworkFactory = new UntrustedWifiNetworkFactory(getHandler().getLooper(), this.mContext, NETWORKTYPE_UNTRUSTED, this.mNetworkCapabilitiesFilter);
                this.mUntrustedNetworkFactory.setScoreFilter(ScoringParams.Values.MAX_EXPID);
                this.mUntrustedNetworkFactory.register();
            }
        }
    }

    private void getAdditionalWifiServiceInterfaces() {
        WifiP2pServiceImpl wifiP2pServiceImplAsInterface;
        if (this.mP2pSupported && (wifiP2pServiceImplAsInterface = IWifiP2pManager.Stub.asInterface(this.mFacade.getService("wifip2p"))) != null) {
            this.mWifiP2pChannel = new AsyncChannel();
            this.mWifiP2pChannel.connect(this.mContext, getHandler(), wifiP2pServiceImplAsInterface.getP2pStateMachineMessenger());
        }
    }

    private void configureRandomizedMacAddress(WifiConfiguration wifiConfiguration) {
        if (wifiConfiguration == null) {
            Log.e(TAG, "No config to change MAC address to");
            return;
        }
        MacAddress macAddressFromString = MacAddress.fromString(this.mWifiNative.getMacAddress(this.mInterfaceName));
        MacAddress orCreateRandomizedMacAddress = wifiConfiguration.getOrCreateRandomizedMacAddress();
        this.mWifiConfigManager.setNetworkRandomizedMacAddress(wifiConfiguration.networkId, orCreateRandomizedMacAddress);
        if (!WifiConfiguration.isValidMacAddressForRandomization(orCreateRandomizedMacAddress)) {
            Log.wtf(TAG, "Config generated an invalid MAC address");
            return;
        }
        if (macAddressFromString.equals(orCreateRandomizedMacAddress)) {
            Log.d(TAG, "No changes in MAC address");
            return;
        }
        this.mWifiMetrics.logStaEvent(17, wifiConfiguration);
        Log.d(TAG, "ConnectedMacRandomization SSID(" + wifiConfiguration.getPrintableSsid() + "). setMacAddress(" + orCreateRandomizedMacAddress.toString() + ") from " + macAddressFromString.toString() + " = " + this.mWifiNative.setMacAddress(this.mInterfaceName, orCreateRandomizedMacAddress));
    }

    private void updateConnectedMacRandomizationSetting() {
        boolean z = true;
        if (this.mFacade.getIntegerSetting(this.mContext, "wifi_connected_mac_randomization_enabled", 0) != 1) {
            z = false;
        }
        this.mEnableConnectedMacRandomization.set(z);
        this.mWifiInfo.setEnableConnectedMacRandomization(z);
        this.mWifiMetrics.setIsMacRandomizationOn(z);
        Log.d(TAG, "EnableConnectedMacRandomization Setting changed to " + z);
    }

    public boolean isConnectedMacRandomizationEnabled() {
        return this.mEnableConnectedMacRandomization.get();
    }

    public void failureDetected(int i) {
        this.mWifiInjector.getSelfRecovery().trigger(2);
    }

    class DefaultState extends State {
        DefaultState() {
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, this);
            switch (message.what) {
                case 0:
                    Log.wtf(WifiStateMachine.TAG, "Error! empty message encountered");
                    return true;
                case 69632:
                    if (((AsyncChannel) message.obj) == WifiStateMachine.this.mWifiP2pChannel) {
                        if (message.arg1 == 0) {
                            WifiStateMachine.this.p2pSendMessage(69633);
                            if (WifiStateMachine.this.mOperationalMode == 1) {
                                WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_ENABLE_P2P);
                            }
                        } else {
                            WifiStateMachine.this.loge("WifiP2pService connection failure, error=" + message.arg1);
                        }
                    } else {
                        WifiStateMachine.this.loge("got HALF_CONNECTED for unknown channel");
                    }
                    return true;
                case 69636:
                    if (((AsyncChannel) message.obj) == WifiStateMachine.this.mWifiP2pChannel) {
                        WifiStateMachine.this.loge("WifiP2pService channel lost, message.arg1 =" + message.arg1);
                    }
                    return true;
                case WifiStateMachine.CMD_BLUETOOTH_ADAPTER_STATE_CHANGE:
                    WifiStateMachine.this.mBluetoothConnectionActive = message.arg1 != 0;
                    return true;
                case WifiStateMachine.CMD_ADD_OR_UPDATE_NETWORK:
                    NetworkUpdateResult networkUpdateResultAddOrUpdateNetwork = WifiStateMachine.this.mWifiConfigManager.addOrUpdateNetwork((WifiConfiguration) message.obj, message.sendingUid);
                    if (!networkUpdateResultAddOrUpdateNetwork.isSuccess()) {
                        WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_FAIL;
                    }
                    WifiStateMachine.this.replyToMessage(message, message.what, networkUpdateResultAddOrUpdateNetwork.getNetworkId());
                    return true;
                case WifiStateMachine.CMD_REMOVE_NETWORK:
                    WifiStateMachine.this.deleteNetworkConfigAndSendReply(message, false);
                    return true;
                case WifiStateMachine.CMD_ENABLE_NETWORK:
                    boolean zEnableNetwork = WifiStateMachine.this.mWifiConfigManager.enableNetwork(message.arg1, message.arg2 == 1, message.sendingUid);
                    if (!zEnableNetwork) {
                        WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_FAIL;
                    }
                    WifiStateMachine.this.replyToMessage(message, message.what, zEnableNetwork ? 1 : -1);
                    return true;
                case WifiStateMachine.CMD_GET_CONFIGURED_NETWORKS:
                    WifiStateMachine.this.replyToMessage(message, message.what, WifiStateMachine.this.mWifiConfigManager.getSavedNetworks());
                    return true;
                case WifiStateMachine.CMD_GET_SUPPORTED_FEATURES:
                    WifiStateMachine.this.replyToMessage(message, message.what, WifiStateMachine.this.mWifiNative.getSupportedFeatureSet(WifiStateMachine.this.mInterfaceName));
                    return true;
                case WifiStateMachine.CMD_GET_PRIVILEGED_CONFIGURED_NETWORKS:
                    WifiStateMachine.this.replyToMessage(message, message.what, WifiStateMachine.this.mWifiConfigManager.getConfiguredNetworksWithPasswords());
                    return true;
                case WifiStateMachine.CMD_GET_LINK_LAYER_STATS:
                    WifiStateMachine.this.replyToMessage(message, message.what, (Object) null);
                    return true;
                case WifiStateMachine.CMD_SET_OPERATIONAL_MODE:
                case WifiStateMachine.M_CMD_SET_POWER_SAVING_MODE:
                    return true;
                case WifiStateMachine.CMD_DISCONNECT:
                case WifiStateMachine.CMD_RECONNECT:
                case WifiStateMachine.CMD_REASSOCIATE:
                case WifiStateMachine.CMD_RSSI_POLL:
                case WifiStateMachine.CMD_TEST_NETWORK_DISCONNECT:
                case WifiStateMachine.CMD_ROAM_WATCHDOG_TIMER:
                case WifiStateMachine.CMD_DISCONNECTING_WATCHDOG_TIMER:
                case WifiStateMachine.CMD_DISABLE_EPHEMERAL_NETWORK:
                case WifiStateMachine.CMD_DISABLE_P2P_WATCHDOG_TIMER:
                case WifiStateMachine.CMD_ENABLE_P2P:
                case WifiStateMachine.CMD_DISABLE_P2P_RSP:
                case WifiStateMachine.CMD_TARGET_BSSID:
                case WifiStateMachine.CMD_RELOAD_TLS_AND_RECONNECT:
                case WifiStateMachine.CMD_START_CONNECT:
                case WifiStateMachine.CMD_UNWANTED_NETWORK:
                case WifiStateMachine.CMD_START_ROAM:
                case WifiStateMachine.CMD_ASSOCIATED_BSSID:
                case WifiMonitor.NETWORK_CONNECTION_EVENT:
                case WifiMonitor.NETWORK_DISCONNECTION_EVENT:
                case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT:
                case WifiMonitor.AUTHENTICATION_FAILURE_EVENT:
                case WifiMonitor.SUP_REQUEST_IDENTITY:
                case WifiMonitor.SUP_REQUEST_SIM_AUTH:
                case WifiMonitor.ASSOCIATION_REJECTION_EVENT:
                case 196611:
                case 196612:
                case 196614:
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DISCARD;
                    return true;
                case WifiStateMachine.CMD_SET_HIGH_PERF_MODE:
                    if (message.arg1 == 1) {
                        WifiStateMachine.this.setSuspendOptimizations(2, false);
                    } else {
                        WifiStateMachine.this.setSuspendOptimizations(2, true);
                    }
                    return true;
                case WifiStateMachine.CMD_ENABLE_RSSI_POLL:
                    WifiStateMachine.this.mEnableRssiPolling = message.arg1 == 1;
                    return true;
                case WifiStateMachine.CMD_SET_SUSPEND_OPT_ENABLED:
                    if (message.arg1 != 1) {
                        WifiStateMachine.this.setSuspendOptimizations(4, false);
                    } else {
                        if (message.arg2 == 1) {
                            WifiStateMachine.this.mSuspendWakeLock.release();
                        }
                        WifiStateMachine.this.setSuspendOptimizations(4, true);
                    }
                    return true;
                case WifiStateMachine.CMD_SCREEN_STATE_CHANGED:
                    WifiStateMachine.this.handleScreenStateChanged(message.arg1 != 0);
                    return true;
                case WifiStateMachine.CMD_REMOVE_APP_CONFIGURATIONS:
                    WifiStateMachine.this.deferMessage(message);
                    return true;
                case WifiStateMachine.CMD_GET_MATCHING_CONFIG:
                    WifiStateMachine.this.replyToMessage(message, message.what);
                    return true;
                case WifiStateMachine.CMD_RESET_SIM_NETWORKS:
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DEFERRED;
                    WifiStateMachine.this.deferMessage(message);
                    return true;
                case WifiStateMachine.CMD_QUERY_OSU_ICON:
                case WifiStateMachine.CMD_MATCH_PROVIDER_NETWORK:
                    WifiStateMachine.this.replyToMessage(message, message.what);
                    return true;
                case WifiStateMachine.CMD_ADD_OR_UPDATE_PASSPOINT_CONFIG:
                    WifiStateMachine.this.replyToMessage(message, message.what, WifiStateMachine.this.mPasspointManager.addOrUpdateProvider((PasspointConfiguration) message.obj, message.arg1) ? 1 : -1);
                    return true;
                case WifiStateMachine.CMD_REMOVE_PASSPOINT_CONFIG:
                    WifiStateMachine.this.replyToMessage(message, message.what, WifiStateMachine.this.mPasspointManager.removeProvider(message.sendingUid, (String) message.obj) ? 1 : -1);
                    return true;
                case WifiStateMachine.CMD_GET_PASSPOINT_CONFIGS:
                    WifiStateMachine.this.replyToMessage(message, message.what, WifiStateMachine.this.mPasspointManager.getProviderConfigs());
                    return true;
                case WifiStateMachine.CMD_GET_MATCHING_OSU_PROVIDERS:
                    WifiStateMachine.this.replyToMessage(message, message.what, new ArrayList());
                    return true;
                case WifiStateMachine.CMD_BOOT_COMPLETED:
                    WifiStateMachine.this.getAdditionalWifiServiceInterfaces();
                    if (!WifiStateMachine.this.mWifiConfigManager.loadFromStore()) {
                        Log.e(WifiStateMachine.TAG, "Failed to load from config store");
                    }
                    WifiStateMachine.this.maybeRegisterNetworkFactory();
                    return true;
                case WifiStateMachine.CMD_INITIALIZE:
                    boolean zInitialize = WifiStateMachine.this.mWifiNative.initialize();
                    WifiStateMachine.this.mPasspointManager.initializeProvisioner(WifiStateMachine.this.mWifiInjector.getWifiServiceHandlerThread().getLooper());
                    WifiStateMachine.this.replyToMessage(message, message.what, zInitialize ? 1 : -1);
                    return true;
                case WifiStateMachine.CMD_IP_CONFIGURATION_SUCCESSFUL:
                case WifiStateMachine.CMD_IP_CONFIGURATION_LOST:
                case WifiStateMachine.CMD_IP_REACHABILITY_LOST:
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DISCARD;
                    return true;
                case WifiStateMachine.CMD_UPDATE_LINKPROPERTIES:
                    WifiStateMachine.this.updateLinkProperties((LinkProperties) message.obj);
                    return true;
                case WifiStateMachine.CMD_REMOVE_USER_CONFIGURATIONS:
                    WifiStateMachine.this.deferMessage(message);
                    return true;
                case WifiStateMachine.CMD_START_IP_PACKET_OFFLOAD:
                    if (WifiStateMachine.this.mNetworkAgent != null) {
                        WifiStateMachine.this.mNetworkAgent.onPacketKeepaliveEvent(message.arg1, -20);
                    }
                    return true;
                case WifiStateMachine.CMD_STOP_IP_PACKET_OFFLOAD:
                    if (WifiStateMachine.this.mNetworkAgent != null) {
                        WifiStateMachine.this.mNetworkAgent.onPacketKeepaliveEvent(message.arg1, -20);
                    }
                    return true;
                case WifiStateMachine.CMD_START_RSSI_MONITORING_OFFLOAD:
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DISCARD;
                    return true;
                case WifiStateMachine.CMD_STOP_RSSI_MONITORING_OFFLOAD:
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DISCARD;
                    return true;
                case WifiStateMachine.CMD_GET_ALL_MATCHING_CONFIGS:
                    WifiStateMachine.this.replyToMessage(message, message.what, new ArrayList());
                    return true;
                case WifiStateMachine.CMD_INSTALL_PACKET_FILTER:
                    WifiStateMachine.this.mWifiNative.installPacketFilter(WifiStateMachine.this.mInterfaceName, (byte[]) message.obj);
                    return true;
                case WifiStateMachine.CMD_SET_FALLBACK_PACKET_FILTERING:
                    if (((Boolean) message.obj).booleanValue()) {
                        WifiStateMachine.this.mWifiNative.startFilteringMulticastV4Packets(WifiStateMachine.this.mInterfaceName);
                    } else {
                        WifiStateMachine.this.mWifiNative.stopFilteringMulticastV4Packets(WifiStateMachine.this.mInterfaceName);
                    }
                    return true;
                case WifiStateMachine.CMD_USER_SWITCH:
                    Set<Integer> setHandleUserSwitch = WifiStateMachine.this.mWifiConfigManager.handleUserSwitch(message.arg1);
                    if (setHandleUserSwitch.contains(Integer.valueOf(WifiStateMachine.this.mTargetNetworkId)) || setHandleUserSwitch.contains(Integer.valueOf(WifiStateMachine.this.mLastNetworkId))) {
                        WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_DISCONNECT);
                    }
                    return true;
                case WifiStateMachine.CMD_USER_UNLOCK:
                    WifiStateMachine.this.mWifiConfigManager.handleUserUnlock(message.arg1);
                    return true;
                case WifiStateMachine.CMD_USER_STOP:
                    WifiStateMachine.this.mWifiConfigManager.handleUserStop(message.arg1);
                    return true;
                case WifiStateMachine.CMD_READ_PACKET_FILTER:
                    WifiStateMachine.this.mIpClient.readPacketFilterComplete(WifiStateMachine.this.mWifiNative.readPacketFilter(WifiStateMachine.this.mInterfaceName));
                    return true;
                case WifiStateMachine.CMD_DIAGS_CONNECT_TIMEOUT:
                    WifiStateMachine.this.mWifiDiagnostics.reportConnectionEvent(((Long) message.obj).longValue(), (byte) 2);
                    return true;
                case WifiStateMachine.CMD_START_SUBSCRIPTION_PROVISIONING:
                    WifiStateMachine.this.replyToMessage(message, message.what, 0);
                    return true;
                case WifiP2pServiceImpl.P2P_CONNECTION_CHANGED:
                    WifiStateMachine.this.mP2pConnected.set(((NetworkInfo) message.obj).isConnected());
                    return true;
                case WifiP2pServiceImpl.DISCONNECT_WIFI_REQUEST:
                    WifiStateMachine.this.mTemporarilyDisconnectWifi = message.arg1 == 1;
                    WifiStateMachine.this.replyToMessage(message, WifiP2pServiceImpl.DISCONNECT_WIFI_RESPONSE);
                    return true;
                case 151553:
                    WifiStateMachine.this.replyToMessage(message, 151554, 2);
                    return true;
                case 151556:
                    WifiStateMachine.this.deleteNetworkConfigAndSendReply(message, true);
                    return true;
                case 151559:
                    WifiStateMachine.this.saveNetworkConfigAndSendReply(message);
                    return true;
                case 151569:
                    WifiStateMachine.this.replyToMessage(message, 151570, 2);
                    return true;
                case 151572:
                    WifiStateMachine.this.replyToMessage(message, 151574, 2);
                    return true;
                case 151612:
                    WifiStateMachine.this.loge("SET_WIFI_NOT_RECONNECT_AND_SCAN " + message);
                    if (message.arg1 == 1 || message.arg1 == 2) {
                        WifiStateMachine.this.loge("set dont_reconnect_scan flag");
                        WifiStateMachine.this.removeMessages(151612);
                        if (message.arg2 > 0) {
                            WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.this.obtainMessage(151612, 0, -1), message.arg2 * 1000);
                        }
                        WifiStateMachine.this.loge("message.arg1: " + message.arg1);
                        if (message.arg1 == 2) {
                            WifiStateMachine.this.loge("isAllowReconnect is false");
                            WifiStateMachine.this.mDontReconnect.set(true);
                        }
                        if (!WifiStateMachine.this.isTemporarilyDontReconnectWifi()) {
                            WifiStateMachine.this.mDontReconnectAndScan.set(true);
                            WifiStateMachine.this.sendMessage(WifiStateMachine.M_CMD_UPDATE_SCAN_STRATEGY);
                            if (WifiStateMachine.this.mWifiConnectivityManager != null) {
                                WifiStateMachine.this.mWifiConnectivityManager.handleScanStrategyChanged();
                            }
                        }
                    } else {
                        WifiStateMachine.this.loge("reset dont_reconnect_scan flag");
                        WifiStateMachine.this.removeMessages(151612);
                        if (WifiStateMachine.this.isTemporarilyDontReconnectWifi()) {
                            WifiStateMachine.this.mDontReconnect.set(false);
                            WifiStateMachine.this.mDontReconnectAndScan.set(false);
                            WifiStateMachine.this.sendMessage(WifiStateMachine.M_CMD_UPDATE_SCAN_STRATEGY);
                            if (WifiStateMachine.this.mWifiConnectivityManager != null) {
                                WifiStateMachine.this.mWifiConnectivityManager.handleScanStrategyChanged();
                            }
                        }
                    }
                    return true;
                default:
                    WifiStateMachine.this.loge("Error! unhandled message" + message);
                    return true;
            }
        }
    }

    String smToString(Message message) {
        return smToString(message.what);
    }

    String smToString(int i) {
        String str = sSmToString.get(i);
        if (str != null) {
            return str;
        }
        switch (i) {
            case 69632:
                return "AsyncChannel.CMD_CHANNEL_HALF_CONNECTED";
            case 69636:
                return "AsyncChannel.CMD_CHANNEL_DISCONNECTED";
            case WifiP2pServiceImpl.GROUP_CREATING_TIMED_OUT:
                return "GROUP_CREATING_TIMED_OUT";
            case WifiP2pServiceImpl.P2P_CONNECTION_CHANGED:
                return "P2P_CONNECTION_CHANGED";
            case WifiP2pServiceImpl.DISCONNECT_WIFI_REQUEST:
                return "WifiP2pServiceImpl.DISCONNECT_WIFI_REQUEST";
            case WifiP2pServiceImpl.DISCONNECT_WIFI_RESPONSE:
                return "P2P.DISCONNECT_WIFI_RESPONSE";
            case WifiP2pServiceImpl.SET_MIRACAST_MODE:
                return "P2P.SET_MIRACAST_MODE";
            case WifiP2pServiceImpl.BLOCK_DISCOVERY:
                return "P2P.BLOCK_DISCOVERY";
            case WifiMonitor.NETWORK_CONNECTION_EVENT:
                return "NETWORK_CONNECTION_EVENT";
            case WifiMonitor.NETWORK_DISCONNECTION_EVENT:
                return "NETWORK_DISCONNECTION_EVENT";
            case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT:
                return "SUPPLICANT_STATE_CHANGE_EVENT";
            case WifiMonitor.AUTHENTICATION_FAILURE_EVENT:
                return "AUTHENTICATION_FAILURE_EVENT";
            case WifiMonitor.SUP_REQUEST_IDENTITY:
                return "SUP_REQUEST_IDENTITY";
            case WifiMonitor.ASSOCIATION_REJECTION_EVENT:
                return "ASSOCIATION_REJECTION_EVENT";
            case WifiMonitor.ANQP_DONE_EVENT:
                return "WifiMonitor.ANQP_DONE_EVENT";
            case WifiMonitor.GAS_QUERY_START_EVENT:
                return "WifiMonitor.GAS_QUERY_START_EVENT";
            case WifiMonitor.GAS_QUERY_DONE_EVENT:
                return "WifiMonitor.GAS_QUERY_DONE_EVENT";
            case WifiMonitor.RX_HS20_ANQP_ICON_EVENT:
                return "WifiMonitor.RX_HS20_ANQP_ICON_EVENT";
            case WifiMonitor.HS20_REMEDIATION_EVENT:
                return "WifiMonitor.HS20_REMEDIATION_EVENT";
            case 151553:
                return "CONNECT_NETWORK";
            case 151556:
                return "FORGET_NETWORK";
            case 151559:
                return "SAVE_NETWORK";
            case 151569:
                return "WifiManager.DISABLE_NETWORK";
            case 151572:
                return "RSSI_PKTCNT_FETCH";
            default:
                return "what:" + Integer.toString(i);
        }
    }

    private void setupClientMode() {
        Log.d(TAG, "setupClientMode() ifacename = " + this.mInterfaceName);
        this.mWifiStateTracker.updateState(0);
        if (this.mWifiConnectivityManager == null) {
            synchronized (this.mWifiReqCountLock) {
                this.mWifiConnectivityManager = this.mWifiInjector.makeWifiConnectivityManager(this.mWifiInfo, hasConnectionRequests());
                this.mWifiConnectivityManager.setUntrustedConnectionAllowed(this.mUntrustedReqCount > 0);
                this.mWifiConnectivityManager.handleScreenStateChanged(this.mScreenOn);
            }
        }
        if (this.mVerboseLoggingEnabled) {
            this.mWifiConnectivityManager.mDbg = true;
        }
        this.mIpClient = this.mFacade.makeIpClient(this.mContext, this.mInterfaceName, new IpClientCallback());
        this.mIpClient.setMulticastFilter(true);
        registerForWifiMonitorEvents();
        this.mIpReachabilityLostEnhancement.registerForWifiMonitorEvents();
        MtkWifiApmDelegate.getInstance().init();
        this.mWifiInjector.getWifiLastResortWatchdog().clearAllFailureCounts();
        setSupplicantLogLevel();
        this.mSupplicantStateTracker.sendMessage(CMD_RESET_SUPPLICANT_STATE);
        this.mLastBssid = null;
        this.mLastNetworkId = -1;
        this.mLastSignalLevel = -1;
        this.mWifiInfo.setMacAddress(this.mWifiNative.getMacAddress(this.mInterfaceName));
        if (!this.mWifiConfigManager.migrateFromLegacyStore()) {
            Log.e(TAG, "Failed to migrate from legacy config store");
        }
        sendSupplicantConnectionChangedBroadcast(true);
        this.mWifiNative.setExternalSim(this.mInterfaceName, true);
        setRandomMacOui();
        this.mCountryCode.setReadyForChange(true);
        this.mWifiDiagnostics.startLogging(this.mVerboseLoggingEnabled);
        this.mIsRunning = true;
        updateBatteryWorkSource(null);
        this.mWifiNative.setBluetoothCoexistenceScanMode(this.mInterfaceName, this.mBluetoothConnectionActive);
        setNetworkDetailedState(NetworkInfo.DetailedState.DISCONNECTED);
        this.mWifiNative.stopFilteringMulticastV4Packets(this.mInterfaceName);
        this.mWifiNative.stopFilteringMulticastV6Packets(this.mInterfaceName);
        this.mWifiNative.setSuspendOptimizations(this.mInterfaceName, this.mSuspendOptNeedsDisabled == 0 && this.mUserWantsSuspendOpt.get());
        this.mWifiNative.setPowerSave(this.mInterfaceName, true);
        if (this.mP2pSupported) {
            p2pSendMessage(CMD_ENABLE_P2P);
        }
        this.mWifiNative.enableStaAutoReconnect(this.mInterfaceName, false);
        this.mWifiNative.setConcurrencyPriority(true);
    }

    private void stopClientMode() {
        this.mWifiDiagnostics.stopLogging();
        if (this.mP2pSupported) {
            p2pSendMessage(CMD_DISABLE_P2P_REQ);
        }
        this.mIsRunning = false;
        updateBatteryWorkSource(null);
        if (this.mIpClient != null) {
            this.mIpClient.shutdown();
            this.mIpClient.awaitShutdown();
        }
        this.mNetworkInfo.setIsAvailable(false);
        if (this.mNetworkAgent != null) {
            this.mNetworkAgent.sendNetworkInfo(this.mNetworkInfo);
        }
        this.mCountryCode.setReadyForChange(false);
        this.mInterfaceName = null;
        sendSupplicantConnectionChangedBroadcast(false);
    }

    void registerConnected() {
        if (this.mLastNetworkId != -1) {
            this.mWifiConfigManager.updateNetworkAfterConnect(this.mLastNetworkId);
            this.mWifiScoreReport.reset();
            WifiConfiguration currentWifiConfiguration = getCurrentWifiConfiguration();
            if (currentWifiConfiguration != null && currentWifiConfiguration.isPasspoint()) {
                this.mPasspointManager.onPasspointNetworkConnected(currentWifiConfiguration.FQDN);
            }
        }
    }

    void registerDisconnected() {
        if (this.mLastNetworkId != -1) {
            this.mWifiConfigManager.updateNetworkAfterDisconnect(this.mLastNetworkId);
            this.mWifiConfigManager.removeAllEphemeralOrPasspointConfiguredNetworks();
        }
    }

    public WifiConfiguration getCurrentWifiConfiguration() {
        if (this.mLastNetworkId == -1) {
            return null;
        }
        return this.mWifiConfigManager.getConfiguredNetwork(this.mLastNetworkId);
    }

    ScanResult getCurrentScanResult() {
        WifiConfiguration currentWifiConfiguration = getCurrentWifiConfiguration();
        if (currentWifiConfiguration == null) {
            return null;
        }
        String bssid = this.mWifiInfo.getBSSID();
        if (bssid == null) {
            bssid = this.mTargetRoamBSSID;
        }
        ScanDetailCache scanDetailCacheForNetwork = this.mWifiConfigManager.getScanDetailCacheForNetwork(currentWifiConfiguration.networkId);
        if (scanDetailCacheForNetwork == null) {
            return null;
        }
        return scanDetailCacheForNetwork.getScanResult(bssid);
    }

    String getCurrentBSSID() {
        return this.mLastBssid;
    }

    class ConnectModeState extends State {
        ConnectModeState() {
        }

        public void enter() {
            Log.d(WifiStateMachine.TAG, "entering ConnectModeState: ifaceName = " + WifiStateMachine.this.mInterfaceName);
            WifiStateMachine.this.mOperationalMode = 1;
            WifiStateMachine.this.setupClientMode();
            if (!WifiStateMachine.this.mWifiNative.removeAllNetworks(WifiStateMachine.this.mInterfaceName)) {
                WifiStateMachine.this.loge("Failed to remove networks on entering connect mode");
            }
            WifiStateMachine.this.mScanRequestProxy.enableScanningForHiddenNetworks(true);
            WifiStateMachine.this.mWifiInfo.reset();
            WifiStateMachine.this.mWifiInfo.setSupplicantState(SupplicantState.DISCONNECTED);
            WifiStateMachine.this.mWifiInjector.getWakeupController().reset();
            WifiStateMachine.this.mNetworkInfo.setIsAvailable(true);
            if (WifiStateMachine.this.mNetworkAgent != null) {
                WifiStateMachine.this.mNetworkAgent.sendNetworkInfo(WifiStateMachine.this.mNetworkInfo);
            }
            WifiStateMachine.this.setNetworkDetailedState(NetworkInfo.DetailedState.DISCONNECTED);
            WifiStateMachine.this.mWifiConnectivityManager.setWifiEnabled(true);
            WifiStateMachine.this.mWifiMetrics.setWifiState(2);
            WifiStateMachine.this.p2pSendMessage(WifiStateMachine.CMD_ENABLE_P2P);
            WifiStateMachine.this.mSarManager.setClientWifiState(3);
        }

        public void exit() {
            WifiStateMachine.this.mOperationalMode = 4;
            WifiStateMachine.this.mNetworkInfo.setIsAvailable(false);
            if (WifiStateMachine.this.mNetworkAgent != null) {
                WifiStateMachine.this.mNetworkAgent.sendNetworkInfo(WifiStateMachine.this.mNetworkInfo);
            }
            WifiStateMachine.this.mWifiConnectivityManager.setWifiEnabled(false);
            WifiStateMachine.this.mWifiMetrics.setWifiState(1);
            WifiStateMachine.this.mSarManager.setClientWifiState(1);
            if (!WifiStateMachine.this.mWifiNative.removeAllNetworks(WifiStateMachine.this.mInterfaceName)) {
                WifiStateMachine.this.loge("Failed to remove networks on exiting connect mode");
            }
            WifiStateMachine.this.mScanRequestProxy.enableScanningForHiddenNetworks(false);
            WifiStateMachine.this.mScanRequestProxy.clearScanResults();
            WifiStateMachine.this.mWifiInfo.reset();
            WifiStateMachine.this.mWifiInfo.setSupplicantState(SupplicantState.DISCONNECTED);
            WifiStateMachine.this.stopClientMode();
        }

        public boolean processMessage(Message message) {
            boolean zEnableNetwork;
            ScanDetailCache scanDetailCacheForNetwork;
            ScanResult scanResult;
            boolean z;
            boolean zHasCredentialChanged;
            int i;
            WifiStateMachine.this.logStateAndMessage(message, this);
            if (MtkWifiServiceAdapter.preProcessMessage(this, message)) {
                return true;
            }
            int i2 = 13;
            NetworkUpdateResult networkUpdateResultAddOrUpdateNetwork = null;
            switch (message.what) {
                case WifiStateMachine.CMD_BLUETOOTH_ADAPTER_STATE_CHANGE:
                    WifiStateMachine.this.mBluetoothConnectionActive = message.arg1 != 0;
                    WifiStateMachine.this.mWifiNative.setBluetoothCoexistenceScanMode(WifiStateMachine.this.mInterfaceName, WifiStateMachine.this.mBluetoothConnectionActive);
                    zEnableNetwork = false;
                    MtkWifiServiceAdapter.postProcessMessage(this, message, Boolean.valueOf(zEnableNetwork), networkUpdateResultAddOrUpdateNetwork);
                    return true;
                case WifiStateMachine.CMD_REMOVE_NETWORK:
                    if (!WifiStateMachine.this.deleteNetworkConfigAndSendReply(message, false)) {
                        WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_FAIL;
                    } else {
                        int i3 = message.arg1;
                        if (i3 == WifiStateMachine.this.mTargetNetworkId || i3 == WifiStateMachine.this.mLastNetworkId) {
                            WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_DISCONNECT);
                        }
                    }
                    zEnableNetwork = false;
                    MtkWifiServiceAdapter.postProcessMessage(this, message, Boolean.valueOf(zEnableNetwork), networkUpdateResultAddOrUpdateNetwork);
                    return true;
                case WifiStateMachine.CMD_ENABLE_NETWORK:
                    boolean z2 = message.arg2 == 1;
                    int i4 = message.arg1;
                    if (z2) {
                        zEnableNetwork = WifiStateMachine.this.connectToUserSelectNetwork(i4, message.sendingUid, false);
                    } else {
                        zEnableNetwork = WifiStateMachine.this.mWifiConfigManager.enableNetwork(i4, false, message.sendingUid);
                    }
                    if (!zEnableNetwork) {
                        WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_FAIL;
                    }
                    WifiStateMachine.this.replyToMessage(message, message.what, zEnableNetwork ? 1 : -1);
                    MtkWifiServiceAdapter.postProcessMessage(this, message, Boolean.valueOf(zEnableNetwork), networkUpdateResultAddOrUpdateNetwork);
                    return true;
                case WifiStateMachine.CMD_GET_LINK_LAYER_STATS:
                    WifiStateMachine.this.replyToMessage(message, message.what, WifiStateMachine.this.getWifiLinkLayerStats());
                    zEnableNetwork = false;
                    MtkWifiServiceAdapter.postProcessMessage(this, message, Boolean.valueOf(zEnableNetwork), networkUpdateResultAddOrUpdateNetwork);
                    return true;
                case WifiStateMachine.CMD_RECONNECT:
                    WifiStateMachine.this.mWifiConnectivityManager.forceConnectivityScan((WorkSource) message.obj);
                    zEnableNetwork = false;
                    MtkWifiServiceAdapter.postProcessMessage(this, message, Boolean.valueOf(zEnableNetwork), networkUpdateResultAddOrUpdateNetwork);
                    return true;
                case WifiStateMachine.CMD_REASSOCIATE:
                    WifiStateMachine.this.lastConnectAttemptTimestamp = WifiStateMachine.this.mClock.getWallClockMillis();
                    WifiStateMachine.this.mWifiNative.reassociate(WifiStateMachine.this.mInterfaceName);
                    zEnableNetwork = false;
                    MtkWifiServiceAdapter.postProcessMessage(this, message, Boolean.valueOf(zEnableNetwork), networkUpdateResultAddOrUpdateNetwork);
                    return true;
                case WifiStateMachine.CMD_SET_HIGH_PERF_MODE:
                    if (message.arg1 == 1) {
                        WifiStateMachine.this.setSuspendOptimizationsNative(2, false);
                    } else {
                        WifiStateMachine.this.setSuspendOptimizationsNative(2, true);
                    }
                    zEnableNetwork = false;
                    MtkWifiServiceAdapter.postProcessMessage(this, message, Boolean.valueOf(zEnableNetwork), networkUpdateResultAddOrUpdateNetwork);
                    return true;
                case WifiStateMachine.CMD_SET_SUSPEND_OPT_ENABLED:
                    if (message.arg1 == 1) {
                        WifiStateMachine.this.setSuspendOptimizationsNative(4, true);
                        if (message.arg2 == 1) {
                            WifiStateMachine.this.mSuspendWakeLock.release();
                        }
                    } else {
                        WifiStateMachine.this.setSuspendOptimizationsNative(4, false);
                    }
                    zEnableNetwork = false;
                    MtkWifiServiceAdapter.postProcessMessage(this, message, Boolean.valueOf(zEnableNetwork), networkUpdateResultAddOrUpdateNetwork);
                    return true;
                case WifiStateMachine.CMD_ENABLE_TDLS:
                    if (message.obj != null) {
                        WifiStateMachine.this.mWifiNative.startTdls(WifiStateMachine.this.mInterfaceName, (String) message.obj, message.arg1 == 1);
                    }
                    zEnableNetwork = false;
                    MtkWifiServiceAdapter.postProcessMessage(this, message, Boolean.valueOf(zEnableNetwork), networkUpdateResultAddOrUpdateNetwork);
                    return true;
                case WifiStateMachine.CMD_REMOVE_APP_CONFIGURATIONS:
                    Set<Integer> setRemoveNetworksForApp = WifiStateMachine.this.mWifiConfigManager.removeNetworksForApp((ApplicationInfo) message.obj);
                    if (setRemoveNetworksForApp.contains(Integer.valueOf(WifiStateMachine.this.mTargetNetworkId)) || setRemoveNetworksForApp.contains(Integer.valueOf(WifiStateMachine.this.mLastNetworkId))) {
                        WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_DISCONNECT);
                    }
                    zEnableNetwork = false;
                    MtkWifiServiceAdapter.postProcessMessage(this, message, Boolean.valueOf(zEnableNetwork), networkUpdateResultAddOrUpdateNetwork);
                    return true;
                case WifiStateMachine.CMD_DISABLE_EPHEMERAL_NETWORK:
                    WifiConfiguration wifiConfigurationDisableEphemeralNetwork = WifiStateMachine.this.mWifiConfigManager.disableEphemeralNetwork((String) message.obj);
                    if (wifiConfigurationDisableEphemeralNetwork != null && (wifiConfigurationDisableEphemeralNetwork.networkId == WifiStateMachine.this.mTargetNetworkId || wifiConfigurationDisableEphemeralNetwork.networkId == WifiStateMachine.this.mLastNetworkId)) {
                        WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_DISCONNECT);
                    }
                    zEnableNetwork = false;
                    MtkWifiServiceAdapter.postProcessMessage(this, message, Boolean.valueOf(zEnableNetwork), networkUpdateResultAddOrUpdateNetwork);
                    return true;
                case WifiStateMachine.CMD_GET_MATCHING_CONFIG:
                    WifiStateMachine.this.replyToMessage(message, message.what, WifiStateMachine.this.mPasspointManager.getMatchingWifiConfig((ScanResult) message.obj));
                    zEnableNetwork = false;
                    MtkWifiServiceAdapter.postProcessMessage(this, message, Boolean.valueOf(zEnableNetwork), networkUpdateResultAddOrUpdateNetwork);
                    return true;
                case WifiStateMachine.CMD_RESET_SIM_NETWORKS:
                    if (WifiStateMachine.this.mVerboseLoggingEnabled) {
                        WifiStateMachine.this.log("resetting EAP-SIM/AKA/AKA' networks since SIM was changed");
                    }
                    MtkEapSimUtility.resetSimNetworks(message.arg1 == 1, message.arg2);
                    zEnableNetwork = false;
                    MtkWifiServiceAdapter.postProcessMessage(this, message, Boolean.valueOf(zEnableNetwork), networkUpdateResultAddOrUpdateNetwork);
                    return true;
                case WifiStateMachine.CMD_QUERY_OSU_ICON:
                    WifiStateMachine.this.mPasspointManager.queryPasspointIcon(((Bundle) message.obj).getLong("BSSID"), ((Bundle) message.obj).getString(WifiStateMachine.EXTRA_OSU_ICON_QUERY_FILENAME));
                    zEnableNetwork = false;
                    MtkWifiServiceAdapter.postProcessMessage(this, message, Boolean.valueOf(zEnableNetwork), networkUpdateResultAddOrUpdateNetwork);
                    return true;
                case WifiStateMachine.CMD_MATCH_PROVIDER_NETWORK:
                    WifiStateMachine.this.replyToMessage(message, message.what, 0);
                    zEnableNetwork = false;
                    MtkWifiServiceAdapter.postProcessMessage(this, message, Boolean.valueOf(zEnableNetwork), networkUpdateResultAddOrUpdateNetwork);
                    return true;
                case WifiStateMachine.CMD_ADD_OR_UPDATE_PASSPOINT_CONFIG:
                    PasspointConfiguration passpointConfiguration = (PasspointConfiguration) message.obj;
                    if (!WifiStateMachine.this.mPasspointManager.addOrUpdateProvider(passpointConfiguration, message.arg1)) {
                        WifiStateMachine.this.replyToMessage(message, message.what, -1);
                    } else {
                        String fqdn = passpointConfiguration.getHomeSp().getFqdn();
                        if (WifiStateMachine.this.isProviderOwnedNetwork(WifiStateMachine.this.mTargetNetworkId, fqdn) || WifiStateMachine.this.isProviderOwnedNetwork(WifiStateMachine.this.mLastNetworkId, fqdn)) {
                            WifiStateMachine.this.logd("Disconnect from current network since its provider is updated");
                            WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_DISCONNECT);
                        }
                        WifiStateMachine.this.replyToMessage(message, message.what, 1);
                    }
                    zEnableNetwork = false;
                    MtkWifiServiceAdapter.postProcessMessage(this, message, Boolean.valueOf(zEnableNetwork), networkUpdateResultAddOrUpdateNetwork);
                    return true;
                case WifiStateMachine.CMD_REMOVE_PASSPOINT_CONFIG:
                    String str = (String) message.obj;
                    if (WifiStateMachine.this.mPasspointManager.removeProvider(message.sendingUid, str)) {
                        if (WifiStateMachine.this.isProviderOwnedNetwork(WifiStateMachine.this.mTargetNetworkId, str) || WifiStateMachine.this.isProviderOwnedNetwork(WifiStateMachine.this.mLastNetworkId, str)) {
                            WifiStateMachine.this.logd("Disconnect from current network since its provider is removed");
                            WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_DISCONNECT);
                        }
                        WifiStateMachine.this.replyToMessage(message, message.what, 1);
                    } else {
                        WifiStateMachine.this.replyToMessage(message, message.what, -1);
                    }
                    zEnableNetwork = false;
                    MtkWifiServiceAdapter.postProcessMessage(this, message, Boolean.valueOf(zEnableNetwork), networkUpdateResultAddOrUpdateNetwork);
                    return true;
                case WifiStateMachine.CMD_GET_MATCHING_OSU_PROVIDERS:
                    WifiStateMachine.this.replyToMessage(message, message.what, WifiStateMachine.this.mPasspointManager.getMatchingOsuProviders((ScanResult) message.obj));
                    zEnableNetwork = false;
                    MtkWifiServiceAdapter.postProcessMessage(this, message, Boolean.valueOf(zEnableNetwork), networkUpdateResultAddOrUpdateNetwork);
                    return true;
                case WifiStateMachine.CMD_ENABLE_P2P:
                    WifiStateMachine.this.p2pSendMessage(WifiStateMachine.CMD_ENABLE_P2P);
                    zEnableNetwork = false;
                    MtkWifiServiceAdapter.postProcessMessage(this, message, Boolean.valueOf(zEnableNetwork), networkUpdateResultAddOrUpdateNetwork);
                    return true;
                case WifiStateMachine.CMD_TARGET_BSSID:
                    if (message.obj != null) {
                        WifiStateMachine.this.mTargetRoamBSSID = (String) message.obj;
                    }
                    zEnableNetwork = false;
                    MtkWifiServiceAdapter.postProcessMessage(this, message, Boolean.valueOf(zEnableNetwork), networkUpdateResultAddOrUpdateNetwork);
                    return true;
                case WifiStateMachine.CMD_RELOAD_TLS_AND_RECONNECT:
                    if (WifiStateMachine.this.mWifiConfigManager.needsUnlockedKeyStore()) {
                        WifiStateMachine.this.logd("Reconnecting to give a chance to un-connected TLS networks");
                        WifiStateMachine.this.mWifiNative.disconnect(WifiStateMachine.this.mInterfaceName);
                        WifiStateMachine.this.lastConnectAttemptTimestamp = WifiStateMachine.this.mClock.getWallClockMillis();
                        WifiStateMachine.this.mWifiNative.reconnect(WifiStateMachine.this.mInterfaceName);
                    }
                    zEnableNetwork = false;
                    MtkWifiServiceAdapter.postProcessMessage(this, message, Boolean.valueOf(zEnableNetwork), networkUpdateResultAddOrUpdateNetwork);
                    return true;
                case WifiStateMachine.CMD_START_CONNECT:
                    int i5 = message.arg1;
                    int i6 = message.arg2;
                    String str2 = (String) message.obj;
                    synchronized (WifiStateMachine.this.mWifiReqCountLock) {
                        if (WifiStateMachine.this.hasConnectionRequests()) {
                            WifiConfiguration configuredNetworkWithoutMasking = WifiStateMachine.this.mWifiConfigManager.getConfiguredNetworkWithoutMasking(i5);
                            WifiStateMachine.this.logd("CMD_START_CONNECT sup state " + WifiStateMachine.this.mSupplicantStateTracker.getSupplicantStateName() + " my state " + WifiStateMachine.this.getCurrentState().getName() + " nid=" + Integer.toString(i5) + " roam=" + Boolean.toString(WifiStateMachine.this.mIsAutoRoaming));
                            if (configuredNetworkWithoutMasking != null) {
                                WifiStateMachine.this.mTargetNetworkId = i5;
                                WifiStateMachine.this.setTargetBssid(configuredNetworkWithoutMasking, str2);
                                if (WifiStateMachine.this.mEnableConnectedMacRandomization.get()) {
                                    WifiStateMachine.this.configureRandomizedMacAddress(configuredNetworkWithoutMasking);
                                }
                                String macAddress = WifiStateMachine.this.mWifiNative.getMacAddress(WifiStateMachine.this.mInterfaceName);
                                WifiStateMachine.this.mWifiInfo.setMacAddress(macAddress);
                                Log.i(WifiStateMachine.TAG, "Connecting with " + macAddress + " as the mac address");
                                WifiStateMachine.this.reportConnectionAttemptStart(configuredNetworkWithoutMasking, WifiStateMachine.this.mTargetRoamBSSID, 5);
                                if (WifiStateMachine.this.mWifiNative.connectToNetwork(WifiStateMachine.this.mInterfaceName, configuredNetworkWithoutMasking)) {
                                    WifiStateMachine.this.mWifiMetrics.logStaEvent(11, configuredNetworkWithoutMasking);
                                    WifiStateMachine.this.lastConnectAttemptTimestamp = WifiStateMachine.this.mClock.getWallClockMillis();
                                    WifiStateMachine.this.targetWificonfiguration = configuredNetworkWithoutMasking;
                                    WifiStateMachine.this.mIsAutoRoaming = false;
                                    if (WifiStateMachine.this.getCurrentState() != WifiStateMachine.this.mDisconnectedState) {
                                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectingState);
                                    }
                                } else {
                                    WifiStateMachine.this.loge("CMD_START_CONNECT Failed to start connection to network " + configuredNetworkWithoutMasking);
                                    WifiStateMachine.this.reportConnectionAttemptEnd(5, 1);
                                    WifiStateMachine.this.replyToMessage(message, 151554, 0);
                                }
                            } else {
                                WifiStateMachine.this.loge("CMD_START_CONNECT and no config, bail out...");
                            }
                        } else if (WifiStateMachine.this.mNetworkAgent != null) {
                            if (!WifiStateMachine.this.mWifiPermissionsUtil.checkNetworkSettingsPermission(i6)) {
                                WifiStateMachine.this.loge("CMD_START_CONNECT but no requests and connected, but app does not have sufficient permissions, bailing");
                            }
                        } else {
                            WifiStateMachine.this.loge("CMD_START_CONNECT but no requests and not connected, bailing");
                        }
                        MtkWifiServiceAdapter.postProcessMessage(this, message, Boolean.valueOf(zEnableNetwork), networkUpdateResultAddOrUpdateNetwork);
                        return true;
                    }
                    zEnableNetwork = false;
                    MtkWifiServiceAdapter.postProcessMessage(this, message, Boolean.valueOf(zEnableNetwork), networkUpdateResultAddOrUpdateNetwork);
                    return true;
                case WifiStateMachine.CMD_START_ROAM:
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DISCARD;
                    return true;
                case WifiStateMachine.CMD_ASSOCIATED_BSSID:
                    String str3 = (String) message.obj;
                    if (str3 != null && (scanDetailCacheForNetwork = WifiStateMachine.this.mWifiConfigManager.getScanDetailCacheForNetwork(WifiStateMachine.this.mTargetNetworkId)) != null) {
                        WifiStateMachine.this.mWifiMetrics.setConnectionScanDetail(scanDetailCacheForNetwork.getScanDetail(str3));
                    }
                    return false;
                case WifiStateMachine.CMD_REMOVE_USER_CONFIGURATIONS:
                    Set<Integer> setRemoveNetworksForUser = WifiStateMachine.this.mWifiConfigManager.removeNetworksForUser(Integer.valueOf(message.arg1).intValue());
                    if (setRemoveNetworksForUser.contains(Integer.valueOf(WifiStateMachine.this.mTargetNetworkId)) || setRemoveNetworksForUser.contains(Integer.valueOf(WifiStateMachine.this.mLastNetworkId))) {
                        WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_DISCONNECT);
                    }
                    zEnableNetwork = false;
                    MtkWifiServiceAdapter.postProcessMessage(this, message, Boolean.valueOf(zEnableNetwork), networkUpdateResultAddOrUpdateNetwork);
                    return true;
                case WifiStateMachine.CMD_STOP_IP_PACKET_OFFLOAD:
                    int i7 = message.arg1;
                    int iStopWifiIPPacketOffload = WifiStateMachine.this.stopWifiIPPacketOffload(i7);
                    if (WifiStateMachine.this.mNetworkAgent != null) {
                        WifiStateMachine.this.mNetworkAgent.onPacketKeepaliveEvent(i7, iStopWifiIPPacketOffload);
                    }
                    zEnableNetwork = false;
                    MtkWifiServiceAdapter.postProcessMessage(this, message, Boolean.valueOf(zEnableNetwork), networkUpdateResultAddOrUpdateNetwork);
                    return true;
                case WifiStateMachine.CMD_ENABLE_WIFI_CONNECTIVITY_MANAGER:
                    WifiStateMachine.this.mWifiConnectivityManager.enable(message.arg1 == 1);
                    zEnableNetwork = false;
                    MtkWifiServiceAdapter.postProcessMessage(this, message, Boolean.valueOf(zEnableNetwork), networkUpdateResultAddOrUpdateNetwork);
                    return true;
                case WifiStateMachine.CMD_GET_ALL_MATCHING_CONFIGS:
                    WifiStateMachine.this.replyToMessage(message, message.what, WifiStateMachine.this.mPasspointManager.getAllMatchingWifiConfigs((ScanResult) message.obj));
                    zEnableNetwork = false;
                    MtkWifiServiceAdapter.postProcessMessage(this, message, Boolean.valueOf(zEnableNetwork), networkUpdateResultAddOrUpdateNetwork);
                    return true;
                case WifiStateMachine.CMD_CONFIG_ND_OFFLOAD:
                    WifiStateMachine.this.mWifiNative.configureNeighborDiscoveryOffload(WifiStateMachine.this.mInterfaceName, message.arg1 > 0);
                    zEnableNetwork = false;
                    MtkWifiServiceAdapter.postProcessMessage(this, message, Boolean.valueOf(zEnableNetwork), networkUpdateResultAddOrUpdateNetwork);
                    return true;
                case WifiStateMachine.CMD_START_SUBSCRIPTION_PROVISIONING:
                    WifiStateMachine.this.replyToMessage(message, message.what, WifiStateMachine.this.mPasspointManager.startSubscriptionProvisioning(message.arg1, message.getData().getParcelable(WifiStateMachine.EXTRA_OSU_PROVIDER), (IProvisioningCallback) message.obj) ? 1 : 0);
                    zEnableNetwork = false;
                    MtkWifiServiceAdapter.postProcessMessage(this, message, Boolean.valueOf(zEnableNetwork), networkUpdateResultAddOrUpdateNetwork);
                    return true;
                case WifiP2pServiceImpl.DISCONNECT_WIFI_REQUEST:
                    if (message.arg1 == 1) {
                        WifiStateMachine.this.mWifiMetrics.logStaEvent(15, 5);
                        WifiStateMachine.this.mWifiNative.disconnect(WifiStateMachine.this.mInterfaceName);
                        WifiStateMachine.this.mTemporarilyDisconnectWifi = true;
                    } else {
                        WifiStateMachine.this.mWifiNative.reconnect(WifiStateMachine.this.mInterfaceName);
                        WifiStateMachine.this.mTemporarilyDisconnectWifi = false;
                    }
                    zEnableNetwork = false;
                    MtkWifiServiceAdapter.postProcessMessage(this, message, Boolean.valueOf(zEnableNetwork), networkUpdateResultAddOrUpdateNetwork);
                    return true;
                case WifiMonitor.NETWORK_CONNECTION_EVENT:
                    if (WifiStateMachine.this.mVerboseLoggingEnabled) {
                        WifiStateMachine.this.log("Network connection established");
                    }
                    WifiStateMachine.this.mLastNetworkId = message.arg1;
                    WifiStateMachine.this.mWifiConfigManager.clearRecentFailureReason(WifiStateMachine.this.mLastNetworkId);
                    WifiStateMachine.this.mLastBssid = (String) message.obj;
                    int i8 = message.arg2;
                    WifiConfiguration currentWifiConfiguration = WifiStateMachine.this.getCurrentWifiConfiguration();
                    if (currentWifiConfiguration != null) {
                        WifiStateMachine.this.mWifiInfo.setBSSID(WifiStateMachine.this.mLastBssid);
                        WifiStateMachine.this.mWifiInfo.setNetworkId(WifiStateMachine.this.mLastNetworkId);
                        WifiStateMachine.this.mWifiInfo.setMacAddress(WifiStateMachine.this.mWifiNative.getMacAddress(WifiStateMachine.this.mInterfaceName));
                        ScanDetailCache scanDetailCacheForNetwork2 = WifiStateMachine.this.mWifiConfigManager.getScanDetailCacheForNetwork(currentWifiConfiguration.networkId);
                        if (scanDetailCacheForNetwork2 != null && WifiStateMachine.this.mLastBssid != null && (scanResult = scanDetailCacheForNetwork2.getScanResult(WifiStateMachine.this.mLastBssid)) != null) {
                            WifiStateMachine.this.mWifiInfo.setFrequency(scanResult.frequency);
                        }
                        WifiStateMachine.this.mWifiConnectivityManager.trackBssid(WifiStateMachine.this.mLastBssid, true, i8);
                        if (currentWifiConfiguration.enterpriseConfig != null && TelephonyUtil.isSimEapMethod(currentWifiConfiguration.enterpriseConfig.getEapMethod())) {
                            String eapAnonymousIdentity = WifiStateMachine.this.mWifiNative.getEapAnonymousIdentity(WifiStateMachine.this.mInterfaceName);
                            if (eapAnonymousIdentity != null) {
                                currentWifiConfiguration.enterpriseConfig.setAnonymousIdentity(eapAnonymousIdentity);
                            } else {
                                Log.d(WifiStateMachine.TAG, "Failed to get updated anonymous identity from supplicant, reset it in WifiConfiguration.");
                                currentWifiConfiguration.enterpriseConfig.setAnonymousIdentity(null);
                            }
                            WifiStateMachine.this.mWifiConfigManager.addOrUpdateNetwork(currentWifiConfiguration, 1010);
                        }
                        WifiStateMachine.this.sendNetworkStateChangeBroadcast(WifiStateMachine.this.mLastBssid);
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mObtainingIpState);
                    } else {
                        WifiStateMachine.this.logw("Connected to unknown networkId " + WifiStateMachine.this.mLastNetworkId + ", disconnecting...");
                        WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_DISCONNECT);
                    }
                    zEnableNetwork = false;
                    MtkWifiServiceAdapter.postProcessMessage(this, message, Boolean.valueOf(zEnableNetwork), networkUpdateResultAddOrUpdateNetwork);
                    return true;
                case WifiMonitor.NETWORK_DISCONNECTION_EVENT:
                    if (WifiStateMachine.this.mVerboseLoggingEnabled) {
                        WifiStateMachine.this.log("ConnectModeState: Network connection lost ");
                    }
                    WifiStateMachine.this.handleNetworkDisconnect();
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectedState);
                    zEnableNetwork = false;
                    MtkWifiServiceAdapter.postProcessMessage(this, message, Boolean.valueOf(zEnableNetwork), networkUpdateResultAddOrUpdateNetwork);
                    return true;
                case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT:
                    SupplicantState supplicantStateHandleSupplicantStateChange = WifiStateMachine.this.handleSupplicantStateChange(message);
                    if (supplicantStateHandleSupplicantStateChange == SupplicantState.DISCONNECTED && WifiStateMachine.this.mNetworkInfo.getState() != NetworkInfo.State.DISCONNECTED) {
                        if (WifiStateMachine.this.mVerboseLoggingEnabled) {
                            WifiStateMachine.this.log("Missed CTRL-EVENT-DISCONNECTED, disconnect");
                        }
                        WifiStateMachine.this.handleNetworkDisconnect();
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectedState);
                    }
                    if (supplicantStateHandleSupplicantStateChange == SupplicantState.COMPLETED) {
                        WifiStateMachine.this.mIpClient.confirmConfiguration();
                        WifiStateMachine.this.mWifiScoreReport.noteIpCheck();
                    }
                    zEnableNetwork = false;
                    MtkWifiServiceAdapter.postProcessMessage(this, message, Boolean.valueOf(zEnableNetwork), networkUpdateResultAddOrUpdateNetwork);
                    return true;
                case WifiMonitor.AUTHENTICATION_FAILURE_EVENT:
                    WifiStateMachine.this.mWifiDiagnostics.captureBugReportData(2);
                    WifiStateMachine.this.mSupplicantStateTracker.sendMessage(WifiMonitor.AUTHENTICATION_FAILURE_EVENT);
                    int i9 = message.arg1;
                    if (WifiStateMachine.this.isPermanentWrongPasswordFailure(WifiStateMachine.this.mTargetNetworkId, i9)) {
                        WifiConfiguration configuredNetwork = WifiStateMachine.this.mWifiConfigManager.getConfiguredNetwork(WifiStateMachine.this.mTargetNetworkId);
                        if (configuredNetwork != null) {
                            WifiStateMachine.this.mWrongPasswordNotifier.onWrongPasswordError(configuredNetwork.SSID);
                        }
                    } else {
                        if (i9 == 3) {
                            WifiStateMachine.this.handleEapAuthFailure(WifiStateMachine.this.mTargetNetworkId, message.arg2);
                        }
                        i2 = 3;
                    }
                    WifiStateMachine.this.mWifiConfigManager.updateNetworkSelectionStatus(WifiStateMachine.this.mTargetNetworkId, i2);
                    WifiStateMachine.this.mWifiConfigManager.clearRecentFailureReason(WifiStateMachine.this.mTargetNetworkId);
                    WifiStateMachine.this.reportConnectionAttemptEnd(3, 1);
                    WifiStateMachine.this.mWifiInjector.getWifiLastResortWatchdog().noteConnectionFailureAndTriggerIfNeeded(WifiStateMachine.this.getTargetSsid(), WifiStateMachine.this.mTargetRoamBSSID, 2);
                    zEnableNetwork = false;
                    MtkWifiServiceAdapter.postProcessMessage(this, message, Boolean.valueOf(zEnableNetwork), networkUpdateResultAddOrUpdateNetwork);
                    return true;
                case WifiMonitor.SUP_REQUEST_IDENTITY:
                    int i10 = message.arg2;
                    if (WifiStateMachine.this.targetWificonfiguration != null && WifiStateMachine.this.targetWificonfiguration.networkId == i10 && TelephonyUtil.isSimConfig(WifiStateMachine.this.targetWificonfiguration)) {
                        Pair<String, String> simIdentity = MtkEapSimUtility.getSimIdentity(WifiStateMachine.this.getTelephonyManager(), new TelephonyUtil(), WifiStateMachine.this.targetWificonfiguration);
                        if (simIdentity != null && simIdentity.first != null) {
                            MtkEapSimUtility.setDefaultSimToUnspecifiedSimSlot();
                            WifiStateMachine.this.log("Send identity: (" + ((String) simIdentity.first) + ", " + ((String) simIdentity.second) + ") to supplicant");
                            WifiStateMachine.this.mWifiNative.simIdentityResponse(WifiStateMachine.this.mInterfaceName, i10, (String) simIdentity.first, (String) simIdentity.second);
                            z = true;
                            if (!z) {
                            }
                            zEnableNetwork = false;
                        } else {
                            Log.e(WifiStateMachine.TAG, "Unable to retrieve identity from Telephony");
                            z = false;
                            if (!z) {
                            }
                            zEnableNetwork = false;
                        }
                    } else {
                        z = false;
                        if (!z) {
                            String str4 = (String) message.obj;
                            if (WifiStateMachine.this.targetWificonfiguration != null && str4 != null && WifiStateMachine.this.targetWificonfiguration.SSID != null) {
                                if (WifiStateMachine.this.targetWificonfiguration.SSID.equals("\"" + str4 + "\"")) {
                                    WifiStateMachine.this.mWifiConfigManager.updateNetworkSelectionStatus(WifiStateMachine.this.targetWificonfiguration.networkId, 9);
                                }
                            }
                            WifiStateMachine.this.mWifiMetrics.logStaEvent(15, 2);
                            WifiStateMachine.this.mWifiNative.disconnect(WifiStateMachine.this.mInterfaceName);
                        }
                        zEnableNetwork = false;
                    }
                    MtkWifiServiceAdapter.postProcessMessage(this, message, Boolean.valueOf(zEnableNetwork), networkUpdateResultAddOrUpdateNetwork);
                    return true;
                case WifiMonitor.SUP_REQUEST_SIM_AUTH:
                    WifiStateMachine.this.logd("Received SUP_REQUEST_SIM_AUTH");
                    TelephonyUtil.SimAuthRequestData simAuthRequestData = (TelephonyUtil.SimAuthRequestData) message.obj;
                    if (simAuthRequestData != null) {
                        if (simAuthRequestData.protocol == 4) {
                            WifiStateMachine.this.handleGsmAuthRequest(simAuthRequestData);
                        } else if (simAuthRequestData.protocol == 5 || simAuthRequestData.protocol == 6) {
                            WifiStateMachine.this.handle3GAuthRequest(simAuthRequestData);
                        }
                    } else {
                        WifiStateMachine.this.loge("Invalid sim auth request");
                    }
                    zEnableNetwork = false;
                    MtkWifiServiceAdapter.postProcessMessage(this, message, Boolean.valueOf(zEnableNetwork), networkUpdateResultAddOrUpdateNetwork);
                    return true;
                case WifiMonitor.ASSOCIATION_REJECTION_EVENT:
                    WifiStateMachine.this.mWifiDiagnostics.captureBugReportData(1);
                    WifiStateMachine.this.didBlackListBSSID = false;
                    String str5 = (String) message.obj;
                    boolean z3 = message.arg1 > 0;
                    int i11 = message.arg2;
                    Log.d(WifiStateMachine.TAG, "Assocation Rejection event: bssid=" + str5 + " reason code=" + i11 + " timedOut=" + Boolean.toString(z3));
                    if (str5 == null || TextUtils.isEmpty(str5)) {
                        str5 = WifiStateMachine.this.mTargetRoamBSSID;
                    }
                    if (str5 != null) {
                        WifiStateMachine.this.didBlackListBSSID = WifiStateMachine.this.mWifiConnectivityManager.trackBssid(str5, false, i11);
                    }
                    WifiStateMachine.this.mWifiConfigManager.updateNetworkSelectionStatus(WifiStateMachine.this.mTargetNetworkId, 2);
                    WifiStateMachine.this.mWifiConfigManager.setRecentFailureAssociationStatus(WifiStateMachine.this.mTargetNetworkId, i11);
                    WifiStateMachine.this.mSupplicantStateTracker.sendMessage(WifiMonitor.ASSOCIATION_REJECTION_EVENT);
                    WifiStateMachine.this.reportConnectionAttemptEnd(z3 ? 11 : 2, 1);
                    WifiStateMachine.this.mWifiInjector.getWifiLastResortWatchdog().noteConnectionFailureAndTriggerIfNeeded(WifiStateMachine.this.getTargetSsid(), str5, 1);
                    zEnableNetwork = false;
                    MtkWifiServiceAdapter.postProcessMessage(this, message, Boolean.valueOf(zEnableNetwork), networkUpdateResultAddOrUpdateNetwork);
                    return true;
                case WifiMonitor.ANQP_DONE_EVENT:
                    WifiStateMachine.this.mPasspointManager.notifyANQPDone((AnqpEvent) message.obj);
                    zEnableNetwork = false;
                    MtkWifiServiceAdapter.postProcessMessage(this, message, Boolean.valueOf(zEnableNetwork), networkUpdateResultAddOrUpdateNetwork);
                    return true;
                case WifiMonitor.RX_HS20_ANQP_ICON_EVENT:
                    WifiStateMachine.this.mPasspointManager.notifyIconDone((IconEvent) message.obj);
                    zEnableNetwork = false;
                    MtkWifiServiceAdapter.postProcessMessage(this, message, Boolean.valueOf(zEnableNetwork), networkUpdateResultAddOrUpdateNetwork);
                    return true;
                case WifiMonitor.HS20_REMEDIATION_EVENT:
                    WifiStateMachine.this.mPasspointManager.receivedWnmFrame((WnmData) message.obj);
                    zEnableNetwork = false;
                    MtkWifiServiceAdapter.postProcessMessage(this, message, Boolean.valueOf(zEnableNetwork), networkUpdateResultAddOrUpdateNetwork);
                    return true;
                case 151553:
                    int networkId = message.arg1;
                    WifiConfiguration wifiConfiguration = (WifiConfiguration) message.obj;
                    if (wifiConfiguration != null) {
                        networkUpdateResultAddOrUpdateNetwork = WifiStateMachine.this.mWifiConfigManager.addOrUpdateNetwork(wifiConfiguration, message.sendingUid);
                        if (!networkUpdateResultAddOrUpdateNetwork.isSuccess()) {
                            WifiStateMachine.this.loge("CONNECT_NETWORK adding/updating config=" + wifiConfiguration + " failed");
                            WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_FAIL;
                            WifiStateMachine.this.replyToMessage(message, 151554, 0);
                            zEnableNetwork = false;
                            MtkWifiServiceAdapter.postProcessMessage(this, message, Boolean.valueOf(zEnableNetwork), networkUpdateResultAddOrUpdateNetwork);
                            return true;
                        }
                        networkId = networkUpdateResultAddOrUpdateNetwork.getNetworkId();
                        zHasCredentialChanged = networkUpdateResultAddOrUpdateNetwork.hasCredentialChanged();
                    } else {
                        zHasCredentialChanged = false;
                    }
                    if (!WifiStateMachine.this.connectToUserSelectNetwork(networkId, message.sendingUid, zHasCredentialChanged)) {
                        WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_FAIL;
                        WifiStateMachine.this.replyToMessage(message, 151554, 9);
                    } else {
                        WifiStateMachine.this.mWifiMetrics.logStaEvent(13, wifiConfiguration);
                        WifiStateMachine.this.broadcastWifiCredentialChanged(0, wifiConfiguration);
                        WifiStateMachine.this.replyToMessage(message, 151555);
                    }
                    zEnableNetwork = false;
                    MtkWifiServiceAdapter.postProcessMessage(this, message, Boolean.valueOf(zEnableNetwork), networkUpdateResultAddOrUpdateNetwork);
                    return true;
                case 151556:
                    if (WifiStateMachine.this.deleteNetworkConfigAndSendReply(message, true) && ((i = message.arg1) == WifiStateMachine.this.mTargetNetworkId || i == WifiStateMachine.this.mLastNetworkId)) {
                        WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_DISCONNECT);
                    }
                    zEnableNetwork = false;
                    MtkWifiServiceAdapter.postProcessMessage(this, message, Boolean.valueOf(zEnableNetwork), networkUpdateResultAddOrUpdateNetwork);
                    return true;
                case 151559:
                    WifiConfiguration currentWifiConfiguration2 = WifiStateMachine.this.getCurrentWifiConfiguration();
                    networkUpdateResultAddOrUpdateNetwork = WifiStateMachine.this.saveNetworkConfigAndSendReply(message);
                    int networkId2 = networkUpdateResultAddOrUpdateNetwork.getNetworkId();
                    if (networkUpdateResultAddOrUpdateNetwork.isSuccess() && WifiStateMachine.this.mWifiInfo.getNetworkId() == networkId2) {
                        if (networkUpdateResultAddOrUpdateNetwork.hasCredentialChanged()) {
                            WifiConfiguration wifiConfiguration2 = (WifiConfiguration) message.obj;
                            if (MtkEapSimUtility.isSimConfigSameAsCurrent(wifiConfiguration2, currentWifiConfiguration2)) {
                                WifiStateMachine.this.logi("SAVE_NETWORK simSlot are the same as current config, break");
                            } else {
                                WifiStateMachine.this.logi("SAVE_NETWORK credential changed for config=" + wifiConfiguration2.configKey() + ", Reconnecting.");
                                WifiStateMachine.this.startConnectToNetwork(networkId2, message.sendingUid, "any");
                            }
                        } else {
                            if (networkUpdateResultAddOrUpdateNetwork.hasProxyChanged()) {
                                WifiStateMachine.this.log("Reconfiguring proxy on connection");
                                WifiStateMachine.this.mIpClient.setHttpProxy(WifiStateMachine.this.getCurrentWifiConfiguration().getHttpProxy());
                            }
                            if (networkUpdateResultAddOrUpdateNetwork.hasIpChanged()) {
                                WifiStateMachine.this.log("Reconfiguring IP on connection");
                                WifiStateMachine.this.transitionTo(WifiStateMachine.this.mObtainingIpState);
                            }
                        }
                    }
                    zEnableNetwork = false;
                    MtkWifiServiceAdapter.postProcessMessage(this, message, Boolean.valueOf(zEnableNetwork), networkUpdateResultAddOrUpdateNetwork);
                    return true;
                case 151569:
                    int i12 = message.arg1;
                    if (WifiStateMachine.this.mWifiConfigManager.disableNetwork(i12, message.sendingUid)) {
                        WifiStateMachine.this.replyToMessage(message, 151571);
                        if (i12 == WifiStateMachine.this.mTargetNetworkId || i12 == WifiStateMachine.this.mLastNetworkId) {
                            WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_DISCONNECT);
                        }
                    } else {
                        WifiStateMachine.this.loge("Failed to disable network");
                        WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_FAIL;
                        WifiStateMachine.this.replyToMessage(message, 151570, 0);
                    }
                    zEnableNetwork = false;
                    MtkWifiServiceAdapter.postProcessMessage(this, message, Boolean.valueOf(zEnableNetwork), networkUpdateResultAddOrUpdateNetwork);
                    return true;
                default:
                    return false;
            }
        }
    }

    public void updateCapabilities() {
        updateCapabilities(getCurrentWifiConfiguration());
    }

    private void updateCapabilities(WifiConfiguration wifiConfiguration) {
        if (this.mNetworkAgent == null) {
            return;
        }
        NetworkCapabilities networkCapabilities = new NetworkCapabilities(this.mDfltNetworkCapabilities);
        if (this.mWifiInfo != null && !this.mWifiInfo.isEphemeral()) {
            networkCapabilities.addCapability(14);
        } else {
            networkCapabilities.removeCapability(14);
        }
        if (this.mWifiInfo != null && !WifiConfiguration.isMetered(wifiConfiguration, this.mWifiInfo)) {
            networkCapabilities.addCapability(11);
        } else {
            networkCapabilities.removeCapability(11);
        }
        if (this.mWifiInfo != null && this.mWifiInfo.getRssi() != -127) {
            networkCapabilities.setSignalStrength(this.mWifiInfo.getRssi());
        } else {
            networkCapabilities.setSignalStrength(Integer.MIN_VALUE);
        }
        if (this.mWifiInfo != null && !this.mWifiInfo.getSSID().equals("<unknown ssid>")) {
            networkCapabilities.setSSID(this.mWifiInfo.getSSID());
        } else {
            networkCapabilities.setSSID(null);
        }
        this.mNetworkAgent.sendNetworkCapabilities(networkCapabilities);
    }

    private boolean isProviderOwnedNetwork(int i, String str) {
        WifiConfiguration configuredNetwork;
        if (i == -1 || (configuredNetwork = this.mWifiConfigManager.getConfiguredNetwork(i)) == null) {
            return false;
        }
        return TextUtils.equals(configuredNetwork.FQDN, str);
    }

    private void handleEapAuthFailure(int i, int i2) {
        WifiConfiguration configuredNetwork = this.mWifiConfigManager.getConfiguredNetwork(this.mTargetNetworkId);
        if (configuredNetwork != null) {
            switch (configuredNetwork.enterpriseConfig.getEapMethod()) {
                case 4:
                case 5:
                case 6:
                    if (i2 == 16385) {
                        getTelephonyManager().resetCarrierKeysForImsiEncryption();
                    }
                    break;
            }
        }
    }

    private class WifiNetworkAgent extends NetworkAgent {
        private int mLastNetworkStatus;

        public WifiNetworkAgent(Looper looper, Context context, String str, NetworkInfo networkInfo, NetworkCapabilities networkCapabilities, LinkProperties linkProperties, int i, NetworkMisc networkMisc) {
            super(looper, context, str, networkInfo, networkCapabilities, linkProperties, i, networkMisc);
            this.mLastNetworkStatus = -1;
        }

        protected void unwanted() {
            if (this != WifiStateMachine.this.mNetworkAgent) {
                return;
            }
            if (WifiStateMachine.this.mVerboseLoggingEnabled) {
                log("WifiNetworkAgent -> Wifi unwanted score " + Integer.toString(WifiStateMachine.this.mWifiInfo.score));
            }
            WifiStateMachine.this.unwantedNetwork(0);
        }

        protected void networkStatus(int i, String str) {
            if (this == WifiStateMachine.this.mNetworkAgent && i != this.mLastNetworkStatus) {
                this.mLastNetworkStatus = i;
                if (i == 2) {
                    if (WifiStateMachine.this.mVerboseLoggingEnabled) {
                        log("WifiNetworkAgent -> Wifi networkStatus invalid, score=" + Integer.toString(WifiStateMachine.this.mWifiInfo.score));
                    }
                    WifiStateMachine.this.unwantedNetwork(1);
                    return;
                }
                if (i == 1) {
                    if (WifiStateMachine.this.mVerboseLoggingEnabled) {
                        log("WifiNetworkAgent -> Wifi networkStatus valid, score= " + Integer.toString(WifiStateMachine.this.mWifiInfo.score));
                    }
                    WifiStateMachine.this.mWifiMetrics.logStaEvent(14);
                    WifiStateMachine.this.doNetworkStatus(i);
                }
            }
        }

        protected void saveAcceptUnvalidated(boolean z) {
            if (this != WifiStateMachine.this.mNetworkAgent) {
                return;
            }
            WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_ACCEPT_UNVALIDATED, z ? 1 : 0);
        }

        protected void startPacketKeepalive(Message message) {
            WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_START_IP_PACKET_OFFLOAD, message.arg1, message.arg2, message.obj);
        }

        protected void stopPacketKeepalive(Message message) {
            WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_STOP_IP_PACKET_OFFLOAD, message.arg1, message.arg2, message.obj);
        }

        protected void setSignalStrengthThresholds(int[] iArr) {
            log("Received signal strength thresholds: " + Arrays.toString(iArr));
            if (iArr.length == 0) {
                WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_STOP_RSSI_MONITORING_OFFLOAD, WifiStateMachine.this.mWifiInfo.getRssi());
                return;
            }
            int[] iArrCopyOf = Arrays.copyOf(iArr, iArr.length + 2);
            iArrCopyOf[iArrCopyOf.length + WifiStateMachine.MESSAGE_HANDLING_STATUS_FAIL] = -128;
            iArrCopyOf[iArrCopyOf.length - 1] = 127;
            Arrays.sort(iArrCopyOf);
            byte[] bArr = new byte[iArrCopyOf.length];
            for (int i = 0; i < iArrCopyOf.length; i++) {
                int i2 = iArrCopyOf[i];
                if (i2 <= 127 && i2 >= -128) {
                    bArr[i] = (byte) i2;
                } else {
                    Log.e(WifiStateMachine.TAG, "Illegal value " + i2 + " for RSSI thresholds: " + Arrays.toString(iArrCopyOf));
                    WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_STOP_RSSI_MONITORING_OFFLOAD, WifiStateMachine.this.mWifiInfo.getRssi());
                    return;
                }
            }
            WifiStateMachine.this.mRssiRanges = bArr;
            WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_START_RSSI_MONITORING_OFFLOAD, WifiStateMachine.this.mWifiInfo.getRssi());
        }

        protected void preventAutomaticReconnect() {
            if (this != WifiStateMachine.this.mNetworkAgent) {
                return;
            }
            WifiStateMachine.this.unwantedNetwork(2);
        }
    }

    void unwantedNetwork(int i) {
        sendMessage(CMD_UNWANTED_NETWORK, i);
    }

    void doNetworkStatus(int i) {
        sendMessage(CMD_NETWORK_STATUS, i);
    }

    private String buildIdentity(int i, String str, String str2) {
        String str3;
        String strSubstring;
        String strSubstring2;
        if (str == null || str.isEmpty()) {
            return "";
        }
        if (i == 4) {
            str3 = "1";
        } else if (i == 5) {
            str3 = "0";
        } else if (i == 6) {
            str3 = "6";
        } else {
            return "";
        }
        if (str2 != null && !str2.isEmpty()) {
            strSubstring = str2.substring(0, 3);
            strSubstring2 = str2.substring(3);
            if (strSubstring2.length() == 2) {
                strSubstring2 = "0" + strSubstring2;
            }
        } else {
            strSubstring = str.substring(0, 3);
            strSubstring2 = str.substring(3, 6);
        }
        return str3 + str + "@wlan.mnc" + strSubstring2 + ".mcc" + strSubstring + ".3gppnetwork.org";
    }

    class L2ConnectedState extends State {
        RssiEventHandler mRssiEventHandler = new RssiEventHandler();

        class RssiEventHandler implements WifiNative.WifiRssiEventHandler {
            RssiEventHandler() {
            }

            @Override
            public void onRssiThresholdBreached(byte b) {
                if (WifiStateMachine.this.mVerboseLoggingEnabled) {
                    Log.e(WifiStateMachine.TAG, "onRssiThresholdBreach event. Cur Rssi = " + ((int) b));
                }
                WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_RSSI_THRESHOLD_BREACHED, b);
            }
        }

        L2ConnectedState() {
        }

        public void enter() {
            NetworkCapabilities networkCapabilities;
            WifiStateMachine.access$8508(WifiStateMachine.this);
            if (WifiStateMachine.this.mEnableRssiPolling) {
                WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_RSSI_POLL, WifiStateMachine.this.mRssiPollToken, 0);
            }
            if (WifiStateMachine.this.mNetworkAgent != null) {
                WifiStateMachine.this.loge("Have NetworkAgent when entering L2Connected");
                WifiStateMachine.this.setNetworkDetailedState(NetworkInfo.DetailedState.DISCONNECTED);
            }
            WifiStateMachine.this.setNetworkDetailedState(NetworkInfo.DetailedState.CONNECTING);
            if (WifiStateMachine.this.mWifiInfo == null || WifiStateMachine.this.mWifiInfo.getSSID().equals("<unknown ssid>")) {
                networkCapabilities = WifiStateMachine.this.mNetworkCapabilitiesFilter;
            } else {
                networkCapabilities = new NetworkCapabilities(WifiStateMachine.this.mNetworkCapabilitiesFilter);
                networkCapabilities.setSSID(WifiStateMachine.this.mWifiInfo.getSSID());
            }
            NetworkCapabilities networkCapabilities2 = networkCapabilities;
            WifiStateMachine.this.mNetworkAgent = WifiStateMachine.this.new WifiNetworkAgent(WifiStateMachine.this.getHandler().getLooper(), WifiStateMachine.this.mContext, "WifiNetworkAgent", WifiStateMachine.this.mNetworkInfo, networkCapabilities2, WifiStateMachine.this.mLinkProperties, 60, WifiStateMachine.this.mNetworkMisc);
            WifiStateMachine.this.clearTargetBssid("L2ConnectedState");
            WifiStateMachine.this.mCountryCode.setReadyForChange(false);
            WifiStateMachine.this.mWifiMetrics.setWifiState(3);
        }

        public void exit() {
            WifiStateMachine.this.mIpClient.stop();
            if (WifiStateMachine.this.mVerboseLoggingEnabled) {
                StringBuilder sb = new StringBuilder();
                sb.append("leaving L2ConnectedState state nid=" + Integer.toString(WifiStateMachine.this.mLastNetworkId));
                if (WifiStateMachine.this.mLastBssid != null) {
                    sb.append(" ");
                    sb.append(WifiStateMachine.this.mLastBssid);
                }
            }
            if (WifiStateMachine.this.mLastBssid != null || WifiStateMachine.this.mLastNetworkId != -1) {
                WifiStateMachine.this.handleNetworkDisconnect();
            }
            WifiStateMachine.this.mCountryCode.setReadyForChange(true);
            WifiStateMachine.this.mWifiMetrics.setWifiState(2);
            WifiStateMachine.this.mWifiStateTracker.updateState(2);
        }

        public boolean processMessage(Message message) {
            int i;
            ScanDetailCache scanDetailCacheForNetwork;
            ScanResult scanResult;
            WifiStateMachine.this.logStateAndMessage(message, this);
            switch (message.what) {
                case WifiStateMachine.CMD_DISCONNECT:
                    WifiStateMachine.this.mWifiMetrics.logStaEvent(15, 2);
                    WifiStateMachine.this.mWifiNative.disconnect(WifiStateMachine.this.mInterfaceName);
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectingState);
                    return true;
                case WifiStateMachine.CMD_RECONNECT:
                    WifiStateMachine.this.log(" Ignore CMD_RECONNECT request because wifi is already connected");
                    return true;
                case WifiStateMachine.CMD_ENABLE_RSSI_POLL:
                    WifiStateMachine.this.cleanWifiScore();
                    WifiStateMachine.this.mEnableRssiPolling = message.arg1 == 1;
                    WifiStateMachine.access$8508(WifiStateMachine.this);
                    if (WifiStateMachine.this.mEnableRssiPolling) {
                        WifiStateMachine.this.fetchRssiLinkSpeedAndFrequencyNative();
                        WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.this.obtainMessage(WifiStateMachine.CMD_RSSI_POLL, WifiStateMachine.this.mRssiPollToken, 0), WifiStateMachine.this.mPollRssiIntervalMsecs);
                    }
                    return true;
                case WifiStateMachine.CMD_RSSI_POLL:
                    if (message.arg1 == WifiStateMachine.this.mRssiPollToken) {
                        WifiStateMachine.this.getWifiLinkLayerStats();
                        WifiStateMachine.this.fetchRssiLinkSpeedAndFrequencyNative();
                        WifiStateMachine.this.mWifiScoreReport.calculateAndReportScore(WifiStateMachine.this.mWifiInfo, WifiStateMachine.this.mNetworkAgent, WifiStateMachine.this.mWifiMetrics);
                        if (WifiStateMachine.this.mWifiScoreReport.shouldCheckIpLayer()) {
                            WifiStateMachine.this.mIpClient.confirmConfiguration();
                            WifiStateMachine.this.mWifiScoreReport.noteIpCheck();
                        }
                        WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.this.obtainMessage(WifiStateMachine.CMD_RSSI_POLL, WifiStateMachine.this.mRssiPollToken, 0), WifiStateMachine.this.mPollRssiIntervalMsecs);
                        if (WifiStateMachine.this.mVerboseLoggingEnabled) {
                            WifiStateMachine.this.sendRssiChangeBroadcast(WifiStateMachine.this.mWifiInfo.getRssi());
                        }
                    }
                    return true;
                case WifiStateMachine.CMD_RESET_SIM_NETWORKS:
                    if (message.arg1 == 0 && WifiStateMachine.this.mLastNetworkId != -1) {
                        WifiConfiguration configuredNetwork = WifiStateMachine.this.mWifiConfigManager.getConfiguredNetwork(WifiStateMachine.this.mLastNetworkId);
                        if (!TelephonyUtil.isSimConfig(configuredNetwork) || MtkEapSimUtility.getIntSimSlot(configuredNetwork) != (i = message.arg2)) {
                            return false;
                        }
                        WifiStateMachine.this.log("Disconnect since sim" + i + " is removed");
                        WifiStateMachine.this.mWifiMetrics.logStaEvent(15, 6);
                        WifiStateMachine.this.mWifiNative.disconnect(WifiStateMachine.this.mInterfaceName);
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectingState);
                    }
                    return false;
                case WifiStateMachine.CMD_IP_CONFIGURATION_SUCCESSFUL:
                    WifiStateMachine.this.handleSuccessfulIpConfiguration();
                    WifiStateMachine.this.reportConnectionAttemptEnd(1, 1);
                    if (WifiStateMachine.this.getCurrentWifiConfiguration() == null) {
                        WifiStateMachine.this.mWifiNative.disconnect(WifiStateMachine.this.mInterfaceName);
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectingState);
                    } else {
                        WifiStateMachine.this.sendConnectedState();
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mConnectedState);
                    }
                    return true;
                case WifiStateMachine.CMD_IP_CONFIGURATION_LOST:
                    WifiStateMachine.this.getWifiLinkLayerStats();
                    WifiStateMachine.this.handleIpConfigurationLost();
                    WifiStateMachine.this.reportConnectionAttemptEnd(10, 1);
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectingState);
                    return true;
                case WifiStateMachine.CMD_ASSOCIATED_BSSID:
                    if (((String) message.obj) == null) {
                        WifiStateMachine.this.logw("Associated command w/o BSSID");
                    } else {
                        WifiStateMachine.this.mLastBssid = (String) message.obj;
                        if (WifiStateMachine.this.mLastBssid != null && (WifiStateMachine.this.mWifiInfo.getBSSID() == null || !WifiStateMachine.this.mLastBssid.equals(WifiStateMachine.this.mWifiInfo.getBSSID()))) {
                            WifiStateMachine.this.mWifiInfo.setBSSID(WifiStateMachine.this.mLastBssid);
                            WifiConfiguration currentWifiConfiguration = WifiStateMachine.this.getCurrentWifiConfiguration();
                            if (currentWifiConfiguration != null && (scanDetailCacheForNetwork = WifiStateMachine.this.mWifiConfigManager.getScanDetailCacheForNetwork(currentWifiConfiguration.networkId)) != null && (scanResult = scanDetailCacheForNetwork.getScanResult(WifiStateMachine.this.mLastBssid)) != null) {
                                WifiStateMachine.this.mWifiInfo.setFrequency(scanResult.frequency);
                            }
                            WifiStateMachine.this.sendNetworkStateChangeBroadcast(WifiStateMachine.this.mLastBssid);
                        }
                    }
                    return true;
                case WifiStateMachine.CMD_IP_REACHABILITY_LOST:
                    if (WifiStateMachine.this.mVerboseLoggingEnabled && message.obj != null) {
                        WifiStateMachine.this.log((String) message.obj);
                    }
                    if (WifiStateMachine.this.mIpReachabilityDisconnectEnabled) {
                        WifiStateMachine.this.handleIpReachabilityLost();
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectingState);
                    } else {
                        WifiStateMachine.this.logd("CMD_IP_REACHABILITY_LOST but disconnect disabled -- ignore");
                    }
                    return true;
                case WifiStateMachine.CMD_START_RSSI_MONITORING_OFFLOAD:
                case WifiStateMachine.CMD_RSSI_THRESHOLD_BREACHED:
                    WifiStateMachine.this.processRssiThreshold((byte) message.arg1, message.what, this.mRssiEventHandler);
                    return true;
                case WifiStateMachine.CMD_STOP_RSSI_MONITORING_OFFLOAD:
                    WifiStateMachine.this.stopRssiMonitoringOffload();
                    return true;
                case WifiStateMachine.CMD_IPV4_PROVISIONING_SUCCESS:
                    WifiStateMachine.this.handleIPv4Success((DhcpResults) message.obj);
                    WifiStateMachine.this.sendNetworkStateChangeBroadcast(WifiStateMachine.this.mLastBssid);
                    return true;
                case WifiStateMachine.CMD_IPV4_PROVISIONING_FAILURE:
                    WifiStateMachine.this.handleIPv4Failure();
                    return true;
                case WifiP2pServiceImpl.DISCONNECT_WIFI_REQUEST:
                    if (message.arg1 == 1) {
                        WifiStateMachine.this.mWifiMetrics.logStaEvent(15, 5);
                        WifiStateMachine.this.mWifiNative.disconnect(WifiStateMachine.this.mInterfaceName);
                        WifiStateMachine.this.mTemporarilyDisconnectWifi = true;
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectingState);
                    }
                    return true;
                case WifiMonitor.NETWORK_CONNECTION_EVENT:
                    WifiStateMachine.this.mWifiInfo.setBSSID((String) message.obj);
                    WifiStateMachine.this.mLastNetworkId = message.arg1;
                    WifiStateMachine.this.mWifiInfo.setNetworkId(WifiStateMachine.this.mLastNetworkId);
                    WifiStateMachine.this.mWifiInfo.setMacAddress(WifiStateMachine.this.mWifiNative.getMacAddress(WifiStateMachine.this.mInterfaceName));
                    if (!WifiStateMachine.this.mLastBssid.equals(message.obj)) {
                        WifiStateMachine.this.mLastBssid = (String) message.obj;
                        WifiStateMachine.this.sendNetworkStateChangeBroadcast(WifiStateMachine.this.mLastBssid);
                    }
                    return true;
                case 151553:
                    if (WifiStateMachine.this.mWifiInfo.getNetworkId() != message.arg1) {
                        return false;
                    }
                    WifiStateMachine.this.replyToMessage(message, 151555);
                    return true;
                case 151572:
                    RssiPacketCountInfo rssiPacketCountInfo = new RssiPacketCountInfo();
                    WifiStateMachine.this.fetchRssiLinkSpeedAndFrequencyNative();
                    rssiPacketCountInfo.rssi = WifiStateMachine.this.mWifiInfo.getRssi();
                    WifiNative.TxPacketCounters txPacketCounters = WifiStateMachine.this.mWifiNative.getTxPacketCounters(WifiStateMachine.this.mInterfaceName);
                    if (txPacketCounters == null) {
                        WifiStateMachine.this.replyToMessage(message, 151574, 0);
                    } else {
                        rssiPacketCountInfo.txgood = txPacketCounters.txSucceeded;
                        rssiPacketCountInfo.txbad = txPacketCounters.txFailed;
                        WifiStateMachine.this.replyToMessage(message, 151573, rssiPacketCountInfo);
                    }
                    return true;
                case 196611:
                    WifiStateMachine.this.handlePreDhcpSetup();
                    return true;
                case 196612:
                    WifiStateMachine.this.handlePostDhcpSetup();
                    return true;
                case 196614:
                    WifiStateMachine.this.mIpClient.completedPreDhcpAction();
                    return true;
                default:
                    return false;
            }
        }
    }

    class ObtainingIpState extends State {
        ObtainingIpState() {
        }

        public void enter() {
            IpClient.ProvisioningConfiguration provisioningConfigurationBuild;
            WifiConfiguration currentWifiConfiguration = WifiStateMachine.this.getCurrentWifiConfiguration();
            boolean z = currentWifiConfiguration.getIpAssignment() == IpConfiguration.IpAssignment.STATIC;
            if (WifiStateMachine.this.mVerboseLoggingEnabled) {
                String strConfigKey = currentWifiConfiguration.configKey();
                WifiStateMachine.this.log("enter ObtainingIpState netId=" + Integer.toString(WifiStateMachine.this.mLastNetworkId) + " " + strConfigKey + "  roam=" + WifiStateMachine.this.mIsAutoRoaming + " static=" + z);
            }
            WifiStateMachine.this.setNetworkDetailedState(NetworkInfo.DetailedState.OBTAINING_IPADDR);
            WifiStateMachine.this.clearTargetBssid("ObtainingIpAddress");
            WifiStateMachine.this.stopIpClient();
            WifiStateMachine.this.mIpClient.setHttpProxy(currentWifiConfiguration.getHttpProxy());
            if (!TextUtils.isEmpty(WifiStateMachine.this.mTcpBufferSizes)) {
                WifiStateMachine.this.mIpClient.setTcpBufferSizes(WifiStateMachine.this.mTcpBufferSizes);
            }
            if (!z) {
                provisioningConfigurationBuild = IpClient.buildProvisioningConfiguration().withPreDhcpAction().withApfCapabilities(WifiStateMachine.this.mWifiNative.getApfCapabilities(WifiStateMachine.this.mInterfaceName)).withNetwork(WifiStateMachine.this.getCurrentNetwork()).withDisplayName(currentWifiConfiguration.SSID).withRandomMacAddress().build();
            } else {
                provisioningConfigurationBuild = IpClient.buildProvisioningConfiguration().withStaticConfiguration(currentWifiConfiguration.getStaticIpConfiguration()).withApfCapabilities(WifiStateMachine.this.mWifiNative.getApfCapabilities(WifiStateMachine.this.mInterfaceName)).withNetwork(WifiStateMachine.this.getCurrentNetwork()).withDisplayName(currentWifiConfiguration.SSID).build();
            }
            WifiStateMachine.this.mIpClient.startProvisioning(provisioningConfigurationBuild);
            WifiStateMachine.this.getWifiLinkLayerStats();
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, this);
            switch (message.what) {
                case WifiStateMachine.CMD_SET_HIGH_PERF_MODE:
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DEFERRED;
                    WifiStateMachine.this.deferMessage(message);
                    return true;
                case WifiStateMachine.CMD_START_CONNECT:
                case WifiStateMachine.CMD_START_ROAM:
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DISCARD;
                    return true;
                case WifiMonitor.NETWORK_DISCONNECTION_EVENT:
                    WifiStateMachine.this.reportConnectionAttemptEnd(6, 1);
                    return false;
                case 151559:
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DEFERRED;
                    WifiStateMachine.this.deferMessage(message);
                    return true;
                default:
                    return false;
            }
        }
    }

    @VisibleForTesting
    public boolean shouldEvaluateWhetherToSendExplicitlySelected(WifiConfiguration wifiConfiguration) {
        if (wifiConfiguration != null) {
            return this.mWifiConfigManager.getLastSelectedNetwork() == wifiConfiguration.networkId && this.mClock.getElapsedSinceBootMillis() - this.mWifiConfigManager.getLastSelectedTimeStamp() < 30000;
        }
        Log.wtf(TAG, "Current WifiConfiguration is null, but IP provisioning just succeeded");
        return false;
    }

    private void sendConnectedState() {
        WifiConfiguration currentWifiConfiguration = getCurrentWifiConfiguration();
        if (shouldEvaluateWhetherToSendExplicitlySelected(currentWifiConfiguration)) {
            boolean zCheckNetworkSettingsPermission = this.mWifiPermissionsUtil.checkNetworkSettingsPermission(currentWifiConfiguration.lastConnectUid);
            if (this.mVerboseLoggingEnabled) {
                log("Network selected by UID " + currentWifiConfiguration.lastConnectUid + " prompt=" + zCheckNetworkSettingsPermission);
            }
            if (zCheckNetworkSettingsPermission) {
                if (this.mVerboseLoggingEnabled) {
                    log("explictlySelected acceptUnvalidated=" + currentWifiConfiguration.noInternetAccessExpected);
                }
                if (this.mNetworkAgent != null) {
                    this.mNetworkAgent.explicitlySelected(currentWifiConfiguration.noInternetAccessExpected);
                }
            }
        }
        setNetworkDetailedState(NetworkInfo.DetailedState.CONNECTED);
        sendNetworkStateChangeBroadcast(this.mLastBssid);
    }

    class RoamingState extends State {
        boolean mAssociated;

        RoamingState() {
        }

        public void enter() {
            if (WifiStateMachine.this.mVerboseLoggingEnabled) {
                WifiStateMachine.this.log("RoamingState Enter mScreenOn=" + WifiStateMachine.this.mScreenOn);
            }
            WifiStateMachine.this.roamWatchdogCount++;
            WifiStateMachine.this.logd("Start Roam Watchdog " + WifiStateMachine.this.roamWatchdogCount);
            WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.this.obtainMessage(WifiStateMachine.CMD_ROAM_WATCHDOG_TIMER, WifiStateMachine.this.roamWatchdogCount, 0), 15000L);
            this.mAssociated = false;
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, this);
            switch (message.what) {
                case WifiStateMachine.CMD_ROAM_WATCHDOG_TIMER:
                    if (WifiStateMachine.this.roamWatchdogCount == message.arg1) {
                        if (WifiStateMachine.this.mVerboseLoggingEnabled) {
                            WifiStateMachine.this.log("roaming watchdog! -> disconnect");
                        }
                        WifiStateMachine.this.mWifiMetrics.endConnectionEvent(9, 1);
                        WifiStateMachine.access$10808(WifiStateMachine.this);
                        WifiStateMachine.this.handleNetworkDisconnect();
                        WifiStateMachine.this.mWifiMetrics.logStaEvent(15, 4);
                        WifiStateMachine.this.mWifiNative.disconnect(WifiStateMachine.this.mInterfaceName);
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectedState);
                    }
                    return true;
                case WifiStateMachine.CMD_IP_CONFIGURATION_LOST:
                    if (WifiStateMachine.this.getCurrentWifiConfiguration() != null) {
                        WifiStateMachine.this.mWifiDiagnostics.captureBugReportData(3);
                    }
                    return false;
                case WifiStateMachine.CMD_UNWANTED_NETWORK:
                    if (WifiStateMachine.this.mVerboseLoggingEnabled) {
                        WifiStateMachine.this.log("Roaming and CS doesnt want the network -> ignore");
                    }
                    return true;
                case WifiMonitor.NETWORK_CONNECTION_EVENT:
                    if (this.mAssociated) {
                        if (WifiStateMachine.this.mVerboseLoggingEnabled) {
                            WifiStateMachine.this.log("roaming and Network connection established");
                        }
                        WifiStateMachine.this.mLastNetworkId = message.arg1;
                        WifiStateMachine.this.mLastBssid = (String) message.obj;
                        WifiStateMachine.this.mWifiInfo.setBSSID(WifiStateMachine.this.mLastBssid);
                        WifiStateMachine.this.mWifiInfo.setNetworkId(WifiStateMachine.this.mLastNetworkId);
                        WifiStateMachine.this.mWifiConnectivityManager.trackBssid(WifiStateMachine.this.mLastBssid, true, message.arg2);
                        WifiStateMachine.this.sendNetworkStateChangeBroadcast(WifiStateMachine.this.mLastBssid);
                        WifiStateMachine.this.reportConnectionAttemptEnd(1, 1);
                        WifiStateMachine.this.clearTargetBssid("RoamingCompleted");
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mConnectedState);
                    } else {
                        WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DISCARD;
                    }
                    return true;
                case WifiMonitor.NETWORK_DISCONNECTION_EVENT:
                    String str = (String) message.obj;
                    String str2 = "";
                    if (WifiStateMachine.this.mTargetRoamBSSID != null) {
                        str2 = WifiStateMachine.this.mTargetRoamBSSID;
                    }
                    WifiStateMachine.this.log("NETWORK_DISCONNECTION_EVENT in roaming state BSSID=" + str + " target=" + str2);
                    if (str != null && str.equals(WifiStateMachine.this.mTargetRoamBSSID)) {
                        WifiStateMachine.this.handleNetworkDisconnect();
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectedState);
                    }
                    return true;
                case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT:
                    StateChangeResult stateChangeResult = (StateChangeResult) message.obj;
                    if (stateChangeResult.state == SupplicantState.DISCONNECTED || stateChangeResult.state == SupplicantState.INACTIVE || stateChangeResult.state == SupplicantState.INTERFACE_DISABLED) {
                        if (WifiStateMachine.this.mVerboseLoggingEnabled) {
                            WifiStateMachine.this.log("STATE_CHANGE_EVENT in roaming state " + stateChangeResult.toString());
                        }
                        if (stateChangeResult.BSSID != null && stateChangeResult.BSSID.equals(WifiStateMachine.this.mTargetRoamBSSID)) {
                            WifiStateMachine.this.handleNetworkDisconnect();
                            WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectedState);
                        }
                    }
                    if (stateChangeResult.state == SupplicantState.ASSOCIATED) {
                        this.mAssociated = true;
                        if (stateChangeResult.BSSID != null) {
                            WifiStateMachine.this.mTargetRoamBSSID = stateChangeResult.BSSID;
                        }
                    }
                    return true;
                default:
                    return false;
            }
        }

        public void exit() {
            WifiStateMachine.this.logd("WifiStateMachine: Leaving Roaming state");
        }
    }

    class ConnectedState extends State {
        ConnectedState() {
        }

        public void enter() {
            if (WifiStateMachine.this.mVerboseLoggingEnabled) {
                WifiStateMachine.this.log("Enter ConnectedState  mScreenOn=" + WifiStateMachine.this.mScreenOn);
            }
            WifiStateMachine.this.mWifiConnectivityManager.handleConnectionStateChanged(1);
            WifiStateMachine.this.registerConnected();
            WifiStateMachine.this.lastConnectAttemptTimestamp = 0L;
            WifiStateMachine.this.targetWificonfiguration = null;
            WifiStateMachine.this.mIsAutoRoaming = false;
            if (WifiStateMachine.this.testNetworkDisconnect) {
                WifiStateMachine.access$11008(WifiStateMachine.this);
                WifiStateMachine.this.logd("ConnectedState Enter start disconnect test " + WifiStateMachine.this.testNetworkDisconnectCounter);
                WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.this.obtainMessage(WifiStateMachine.CMD_TEST_NETWORK_DISCONNECT, WifiStateMachine.this.testNetworkDisconnectCounter, 0), 15000L);
            }
            WifiStateMachine.this.mLastDriverRoamAttempt = 0L;
            WifiStateMachine.this.mTargetNetworkId = -1;
            WifiStateMachine.this.mWifiInjector.getWifiLastResortWatchdog().connectedStateTransition(true);
            WifiStateMachine.this.mWifiStateTracker.updateState(3);
        }

        public boolean processMessage(Message message) {
            String str;
            WifiConfiguration currentWifiConfiguration;
            WifiStateMachine.this.logStateAndMessage(message, this);
            switch (message.what) {
                case WifiStateMachine.CMD_TEST_NETWORK_DISCONNECT:
                    if (message.arg1 == WifiStateMachine.this.testNetworkDisconnectCounter) {
                        WifiStateMachine.this.mWifiNative.disconnect(WifiStateMachine.this.mInterfaceName);
                    }
                    return true;
                case WifiStateMachine.CMD_UNWANTED_NETWORK:
                    if (message.arg1 == 0) {
                        WifiStateMachine.this.mWifiMetrics.logStaEvent(15, 3);
                        WifiStateMachine.this.mWifiNative.disconnect(WifiStateMachine.this.mInterfaceName);
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectingState);
                    } else if (message.arg1 == 2 || message.arg1 == 1) {
                        if (message.arg1 == 2) {
                            str = "NETWORK_STATUS_UNWANTED_DISABLE_AUTOJOIN";
                        } else {
                            str = "NETWORK_STATUS_UNWANTED_VALIDATION_FAILED";
                        }
                        Log.d(WifiStateMachine.TAG, str);
                        WifiConfiguration currentWifiConfiguration2 = WifiStateMachine.this.getCurrentWifiConfiguration();
                        if (currentWifiConfiguration2 != null) {
                            if (message.arg1 == 2) {
                                WifiStateMachine.this.mWifiConfigManager.setNetworkValidatedInternetAccess(currentWifiConfiguration2.networkId, false);
                                WifiStateMachine.this.mWifiConfigManager.updateNetworkSelectionStatus(currentWifiConfiguration2.networkId, 10);
                            } else {
                                WifiStateMachine.this.mWifiConfigManager.incrementNetworkNoInternetAccessReports(currentWifiConfiguration2.networkId);
                                if (WifiStateMachine.this.mWifiConfigManager.getLastSelectedNetwork() != currentWifiConfiguration2.networkId && !currentWifiConfiguration2.noInternetAccessExpected) {
                                    Log.i(WifiStateMachine.TAG, "Temporarily disabling network because ofno-internet access");
                                    WifiStateMachine.this.mWifiConfigManager.updateNetworkSelectionStatus(currentWifiConfiguration2.networkId, 6);
                                }
                            }
                        }
                    }
                    return true;
                case WifiStateMachine.CMD_START_ROAM:
                    WifiStateMachine.this.mLastDriverRoamAttempt = 0L;
                    int i = message.arg1;
                    ScanResult scanResult = (ScanResult) message.obj;
                    String str2 = "any";
                    if (scanResult != null) {
                        str2 = scanResult.BSSID;
                    }
                    WifiConfiguration configuredNetworkWithoutMasking = WifiStateMachine.this.mWifiConfigManager.getConfiguredNetworkWithoutMasking(i);
                    if (configuredNetworkWithoutMasking != null) {
                        WifiStateMachine.this.setTargetBssid(configuredNetworkWithoutMasking, str2);
                        WifiStateMachine.this.mTargetNetworkId = i;
                        WifiStateMachine.this.logd("CMD_START_ROAM sup state " + WifiStateMachine.this.mSupplicantStateTracker.getSupplicantStateName() + " my state " + WifiStateMachine.this.getCurrentState().getName() + " nid=" + Integer.toString(i) + " config " + configuredNetworkWithoutMasking.configKey() + " targetRoamBSSID " + WifiStateMachine.this.mTargetRoamBSSID);
                        WifiStateMachine.this.reportConnectionAttemptStart(configuredNetworkWithoutMasking, WifiStateMachine.this.mTargetRoamBSSID, 3);
                        if (WifiStateMachine.this.mWifiNative.roamToNetwork(WifiStateMachine.this.mInterfaceName, configuredNetworkWithoutMasking)) {
                            WifiStateMachine.this.lastConnectAttemptTimestamp = WifiStateMachine.this.mClock.getWallClockMillis();
                            WifiStateMachine.this.targetWificonfiguration = configuredNetworkWithoutMasking;
                            WifiStateMachine.this.mIsAutoRoaming = true;
                            WifiStateMachine.this.mWifiMetrics.logStaEvent(12, configuredNetworkWithoutMasking);
                            WifiStateMachine.this.transitionTo(WifiStateMachine.this.mRoamingState);
                        } else {
                            WifiStateMachine.this.loge("CMD_START_ROAM Failed to start roaming to network " + configuredNetworkWithoutMasking);
                            WifiStateMachine.this.reportConnectionAttemptEnd(5, 1);
                            WifiStateMachine.this.replyToMessage(message, 151554, 0);
                            WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_FAIL;
                        }
                    } else {
                        WifiStateMachine.this.loge("CMD_START_ROAM and no config, bail out...");
                    }
                    return true;
                case WifiStateMachine.CMD_ASSOCIATED_BSSID:
                    WifiStateMachine.this.mLastDriverRoamAttempt = WifiStateMachine.this.mClock.getWallClockMillis();
                    return false;
                case WifiStateMachine.CMD_NETWORK_STATUS:
                    if (message.arg1 == 1 && (currentWifiConfiguration = WifiStateMachine.this.getCurrentWifiConfiguration()) != null) {
                        WifiStateMachine.this.mWifiConfigManager.updateNetworkSelectionStatus(currentWifiConfiguration.networkId, 0);
                        WifiStateMachine.this.mWifiConfigManager.setNetworkValidatedInternetAccess(currentWifiConfiguration.networkId, true);
                    }
                    return true;
                case WifiStateMachine.CMD_ACCEPT_UNVALIDATED:
                    WifiStateMachine.this.mWifiConfigManager.setNetworkNoInternetAccessExpected(WifiStateMachine.this.mLastNetworkId, message.arg1 != 0);
                    return true;
                case WifiStateMachine.CMD_START_IP_PACKET_OFFLOAD:
                    int i2 = message.arg1;
                    int iStartWifiIPPacketOffload = WifiStateMachine.this.startWifiIPPacketOffload(i2, (KeepalivePacketData) message.obj, message.arg2);
                    if (WifiStateMachine.this.mNetworkAgent != null) {
                        WifiStateMachine.this.mNetworkAgent.onPacketKeepaliveEvent(i2, iStartWifiIPPacketOffload);
                    }
                    return true;
                case WifiMonitor.NETWORK_DISCONNECTION_EVENT:
                    WifiStateMachine.this.reportConnectionAttemptEnd(6, 1);
                    if (WifiStateMachine.this.mLastDriverRoamAttempt != 0) {
                        WifiStateMachine.this.mClock.getWallClockMillis();
                        long unused = WifiStateMachine.this.mLastDriverRoamAttempt;
                        WifiStateMachine.this.mLastDriverRoamAttempt = 0L;
                    }
                    if (WifiStateMachine.unexpectedDisconnectedReason(message.arg2)) {
                        WifiStateMachine.this.mWifiDiagnostics.captureBugReportData(5);
                    }
                    WifiConfiguration currentWifiConfiguration3 = WifiStateMachine.this.getCurrentWifiConfiguration();
                    if (WifiStateMachine.this.mVerboseLoggingEnabled) {
                        WifiStateMachine wifiStateMachine = WifiStateMachine.this;
                        StringBuilder sb = new StringBuilder();
                        sb.append("NETWORK_DISCONNECTION_EVENT in connected state BSSID=");
                        sb.append(WifiStateMachine.this.mWifiInfo.getBSSID());
                        sb.append(" RSSI=");
                        sb.append(WifiStateMachine.this.mWifiInfo.getRssi());
                        sb.append(" freq=");
                        sb.append(WifiStateMachine.this.mWifiInfo.getFrequency());
                        sb.append(" reason=");
                        sb.append(message.arg2);
                        sb.append(" Network Selection Status=");
                        sb.append(currentWifiConfiguration3 == null ? "Unavailable" : currentWifiConfiguration3.getNetworkSelectionStatus().getNetworkStatusString());
                        wifiStateMachine.log(sb.toString());
                    }
                    return true;
                default:
                    return false;
            }
        }

        public void exit() {
            WifiStateMachine.this.logd("WifiStateMachine: Leaving Connected state");
            WifiStateMachine.this.mWifiConnectivityManager.handleConnectionStateChanged(3);
            WifiStateMachine.this.mLastDriverRoamAttempt = 0L;
            WifiStateMachine.this.mWifiInjector.getWifiLastResortWatchdog().connectedStateTransition(false);
        }
    }

    class DisconnectingState extends State {
        DisconnectingState() {
        }

        public void enter() {
            if (WifiStateMachine.this.mVerboseLoggingEnabled) {
                WifiStateMachine.this.logd(" Enter DisconnectingState State screenOn=" + WifiStateMachine.this.mScreenOn);
            }
            WifiStateMachine.this.disconnectingWatchdogCount++;
            WifiStateMachine.this.logd("Start Disconnecting Watchdog " + WifiStateMachine.this.disconnectingWatchdogCount);
            WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.this.obtainMessage(WifiStateMachine.CMD_DISCONNECTING_WATCHDOG_TIMER, WifiStateMachine.this.disconnectingWatchdogCount, 0), 5000L);
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, this);
            int i = message.what;
            if (i == WifiStateMachine.CMD_DISCONNECT) {
                if (WifiStateMachine.this.mVerboseLoggingEnabled) {
                    WifiStateMachine.this.log("Ignore CMD_DISCONNECT when already disconnecting.");
                    return true;
                }
                return true;
            }
            if (i == WifiStateMachine.CMD_DISCONNECTING_WATCHDOG_TIMER) {
                if (WifiStateMachine.this.disconnectingWatchdogCount == message.arg1) {
                    if (WifiStateMachine.this.mVerboseLoggingEnabled) {
                        WifiStateMachine.this.log("disconnecting watchdog! -> disconnect");
                    }
                    WifiStateMachine.this.handleNetworkDisconnect();
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectedState);
                    return true;
                }
                return true;
            }
            if (i == WifiStateMachine.M_CMD_UPDATE_SCAN_STRATEGY) {
                WifiStateMachine.this.deferMessage(message);
                return true;
            }
            if (i == 147462) {
                WifiStateMachine.this.deferMessage(message);
                WifiStateMachine.this.handleNetworkDisconnect();
                WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectedState);
                return true;
            }
            return false;
        }
    }

    class DisconnectedState extends State {
        DisconnectedState() {
        }

        public void enter() {
            Log.i(WifiStateMachine.TAG, "disconnectedstate enter");
            if (WifiStateMachine.this.mTemporarilyDisconnectWifi) {
                WifiStateMachine.this.p2pSendMessage(WifiP2pServiceImpl.DISCONNECT_WIFI_RESPONSE);
                return;
            }
            if (WifiStateMachine.this.mVerboseLoggingEnabled) {
                WifiStateMachine.this.logd(" Enter DisconnectedState screenOn=" + WifiStateMachine.this.mScreenOn);
            }
            WifiStateMachine.this.mIsAutoRoaming = false;
            WifiStateMachine.this.mWifiConnectivityManager.handleConnectionStateChanged(2);
            WifiStateMachine.this.mDisconnectedTimeStamp = WifiStateMachine.this.mClock.getWallClockMillis();
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, this);
            switch (message.what) {
                case WifiStateMachine.CMD_DISCONNECT:
                    WifiStateMachine.this.mWifiMetrics.logStaEvent(15, 2);
                    WifiStateMachine.this.mWifiNative.disconnect(WifiStateMachine.this.mInterfaceName);
                    break;
                case WifiStateMachine.CMD_RECONNECT:
                case WifiStateMachine.CMD_REASSOCIATE:
                    if (!WifiStateMachine.this.mTemporarilyDisconnectWifi) {
                        return false;
                    }
                    break;
                case WifiStateMachine.CMD_SCREEN_STATE_CHANGED:
                    WifiStateMachine.this.handleScreenStateChanged(message.arg1 != 0);
                    break;
                case WifiStateMachine.M_CMD_UPDATE_SCAN_STRATEGY:
                    if (WifiStateMachine.this.isTemporarilyDontReconnectWifi()) {
                        if (WifiStateMachine.this.mConnectNetwork) {
                            WifiStateMachine.this.mConnectNetwork = false;
                        } else {
                            Log.d(WifiStateMachine.TAG, "Disable supplicant auto scan!");
                            WifiStateMachine.this.mWifiNative.disconnect(WifiStateMachine.this.mInterfaceName);
                        }
                    }
                    break;
                case WifiP2pServiceImpl.P2P_CONNECTION_CHANGED:
                    WifiStateMachine.this.mP2pConnected.set(((NetworkInfo) message.obj).isConnected());
                    break;
                case WifiMonitor.NETWORK_DISCONNECTION_EVENT:
                    break;
                case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT:
                    StateChangeResult stateChangeResult = (StateChangeResult) message.obj;
                    if (WifiStateMachine.this.mVerboseLoggingEnabled) {
                        WifiStateMachine.this.logd("SUPPLICANT_STATE_CHANGE_EVENT state=" + stateChangeResult.state + " -> state= " + WifiInfo.getDetailedStateOf(stateChangeResult.state));
                    }
                    WifiStateMachine.this.setNetworkDetailedState(WifiInfo.getDetailedStateOf(stateChangeResult.state));
                    return false;
                default:
                    return false;
            }
            return true;
        }

        public void exit() {
            WifiStateMachine.this.mWifiConnectivityManager.handleConnectionStateChanged(3);
        }
    }

    private void replyToMessage(Message message, int i) {
        if (message.replyTo == null) {
            return;
        }
        this.mReplyChannel.replyToMessage(message, obtainMessageWithWhatAndArg2(message, i));
    }

    private void replyToMessage(Message message, int i, int i2) {
        if (message.replyTo == null) {
            return;
        }
        Message messageObtainMessageWithWhatAndArg2 = obtainMessageWithWhatAndArg2(message, i);
        messageObtainMessageWithWhatAndArg2.arg1 = i2;
        this.mReplyChannel.replyToMessage(message, messageObtainMessageWithWhatAndArg2);
    }

    private void replyToMessage(Message message, int i, Object obj) {
        if (message.replyTo == null) {
            return;
        }
        Message messageObtainMessageWithWhatAndArg2 = obtainMessageWithWhatAndArg2(message, i);
        messageObtainMessageWithWhatAndArg2.obj = obj;
        this.mReplyChannel.replyToMessage(message, messageObtainMessageWithWhatAndArg2);
    }

    private Message obtainMessageWithWhatAndArg2(Message message, int i) {
        Message messageObtain = Message.obtain();
        messageObtain.what = i;
        messageObtain.arg2 = message.arg2;
        return messageObtain;
    }

    private void broadcastWifiCredentialChanged(int i, WifiConfiguration wifiConfiguration) {
        if (wifiConfiguration != null && wifiConfiguration.preSharedKey != null) {
            Intent intent = new Intent("android.net.wifi.WIFI_CREDENTIAL_CHANGED");
            intent.putExtra("ssid", wifiConfiguration.SSID);
            intent.putExtra("et", i);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT, "android.permission.RECEIVE_WIFI_CREDENTIAL_CHANGE");
        }
    }

    void handleGsmAuthRequest(TelephonyUtil.SimAuthRequestData simAuthRequestData) {
        if (this.targetWificonfiguration == null || this.targetWificonfiguration.networkId == simAuthRequestData.networkId) {
            if (this.targetWificonfiguration == null) {
                this.targetWificonfiguration = this.mWifiConfigManager.getConfiguredNetwork(simAuthRequestData.networkId);
                logd("Assign targetWificonfiguration in roaming case since it will be null");
            }
            logd("id matches targetWifiConfiguration");
            String gsmSimAuthResponse = TelephonyUtil.getGsmSimAuthResponse(simAuthRequestData.data, getTelephonyManager());
            if (gsmSimAuthResponse == null) {
                this.mWifiNative.simAuthFailedResponse(this.mInterfaceName, simAuthRequestData.networkId);
                return;
            }
            logv("Supplicant Response -" + gsmSimAuthResponse);
            this.mWifiNative.simAuthResponse(this.mInterfaceName, simAuthRequestData.networkId, WifiNative.SIM_AUTH_RESP_TYPE_GSM_AUTH, gsmSimAuthResponse);
            return;
        }
        logd("id does not match targetWifiConfiguration");
    }

    void handle3GAuthRequest(TelephonyUtil.SimAuthRequestData simAuthRequestData) {
        if (this.targetWificonfiguration == null || this.targetWificonfiguration.networkId == simAuthRequestData.networkId) {
            if (this.targetWificonfiguration == null) {
                this.targetWificonfiguration = this.mWifiConfigManager.getConfiguredNetwork(simAuthRequestData.networkId);
                logd("Assign targetWificonfiguration in roaming case since it will be null");
            }
            logd("id matches targetWifiConfiguration");
            TelephonyUtil.SimAuthResponseData simAuthResponseData = TelephonyUtil.get3GAuthResponse(simAuthRequestData, getTelephonyManager());
            if (simAuthResponseData != null) {
                this.mWifiNative.simAuthResponse(this.mInterfaceName, simAuthRequestData.networkId, simAuthResponseData.type, simAuthResponseData.response);
                return;
            } else {
                this.mWifiNative.umtsAuthFailedResponse(this.mInterfaceName, simAuthRequestData.networkId);
                return;
            }
        }
        logd("id does not match targetWifiConfiguration");
    }

    public void startConnectToNetwork(int i, int i2, String str) {
        sendMessage(CMD_START_CONNECT, i, i2, str);
    }

    public void startRoamToNetwork(int i, ScanResult scanResult) {
        sendMessage(CMD_START_ROAM, i, 0, scanResult);
    }

    public void enableWifiConnectivityManager(boolean z) {
        sendMessage(CMD_ENABLE_WIFI_CONNECTIVITY_MANAGER, z ? 1 : 0);
    }

    static boolean unexpectedDisconnectedReason(int i) {
        return i == 2 || i == 6 || i == 7 || i == 8 || i == 9 || i == 14 || i == 15 || i == 16 || i == 18 || i == 19 || i == 23 || i == 34;
    }

    public void updateWifiMetrics() {
        this.mWifiMetrics.updateSavedNetworks(this.mWifiConfigManager.getSavedNetworks());
        this.mPasspointManager.updateMetrics();
    }

    private boolean deleteNetworkConfigAndSendReply(Message message, boolean z) {
        boolean zRemoveNetwork = this.mWifiConfigManager.removeNetwork(message.arg1, message.sendingUid);
        if (!zRemoveNetwork) {
            loge("Failed to remove network");
        }
        if (z) {
            if (!zRemoveNetwork) {
                replyToMessage(message, 151557, 0);
                return false;
            }
            replyToMessage(message, 151558);
            broadcastWifiCredentialChanged(1, (WifiConfiguration) message.obj);
            return true;
        }
        if (zRemoveNetwork) {
            replyToMessage(message, message.what, 1);
            return true;
        }
        this.messageHandlingStatus = MESSAGE_HANDLING_STATUS_FAIL;
        replyToMessage(message, message.what, -1);
        return false;
    }

    private NetworkUpdateResult saveNetworkConfigAndSendReply(Message message) {
        WifiConfiguration wifiConfiguration = (WifiConfiguration) message.obj;
        if (wifiConfiguration == null) {
            loge("SAVE_NETWORK with null configuration " + this.mSupplicantStateTracker.getSupplicantStateName() + " my state " + getCurrentState().getName());
            this.messageHandlingStatus = MESSAGE_HANDLING_STATUS_FAIL;
            replyToMessage(message, 151560, 0);
            return new NetworkUpdateResult(-1);
        }
        NetworkUpdateResult networkUpdateResultAddOrUpdateNetwork = this.mWifiConfigManager.addOrUpdateNetwork(wifiConfiguration, message.sendingUid);
        if (!networkUpdateResultAddOrUpdateNetwork.isSuccess()) {
            loge("SAVE_NETWORK adding/updating config=" + wifiConfiguration + " failed");
            this.messageHandlingStatus = MESSAGE_HANDLING_STATUS_FAIL;
            replyToMessage(message, 151560, 0);
            return networkUpdateResultAddOrUpdateNetwork;
        }
        if (!this.mWifiConfigManager.enableNetwork(networkUpdateResultAddOrUpdateNetwork.getNetworkId(), false, message.sendingUid)) {
            loge("SAVE_NETWORK enabling config=" + wifiConfiguration + " failed");
            this.messageHandlingStatus = MESSAGE_HANDLING_STATUS_FAIL;
            replyToMessage(message, 151560, 0);
            return new NetworkUpdateResult(-1);
        }
        broadcastWifiCredentialChanged(0, wifiConfiguration);
        replyToMessage(message, 151561);
        return networkUpdateResultAddOrUpdateNetwork;
    }

    private static String getLinkPropertiesSummary(LinkProperties linkProperties) {
        ArrayList arrayList = new ArrayList(6);
        if (linkProperties.hasIPv4Address()) {
            arrayList.add("v4");
        }
        if (linkProperties.hasIPv4DefaultRoute()) {
            arrayList.add("v4r");
        }
        if (linkProperties.hasIPv4DnsServer()) {
            arrayList.add("v4dns");
        }
        if (linkProperties.hasGlobalIPv6Address()) {
            arrayList.add("v6");
        }
        if (linkProperties.hasIPv6DefaultRoute()) {
            arrayList.add("v6r");
        }
        if (linkProperties.hasIPv6DnsServer()) {
            arrayList.add("v6dns");
        }
        return TextUtils.join(" ", arrayList);
    }

    private String getTargetSsid() {
        WifiConfiguration configuredNetwork = this.mWifiConfigManager.getConfiguredNetwork(this.mTargetNetworkId);
        if (configuredNetwork != null) {
            return configuredNetwork.SSID;
        }
        return null;
    }

    private boolean p2pSendMessage(int i) {
        if (this.mWifiP2pChannel != null) {
            this.mWifiP2pChannel.sendMessage(i);
            return true;
        }
        return false;
    }

    private boolean p2pSendMessage(int i, int i2) {
        if (this.mWifiP2pChannel != null) {
            this.mWifiP2pChannel.sendMessage(i, i2);
            return true;
        }
        return false;
    }

    private boolean hasConnectionRequests() {
        return this.mConnectionReqCount > 0 || this.mUntrustedReqCount > 0;
    }

    public boolean getIpReachabilityDisconnectEnabled() {
        return this.mIpReachabilityDisconnectEnabled;
    }

    public void setIpReachabilityDisconnectEnabled(boolean z) {
        this.mIpReachabilityDisconnectEnabled = z;
    }

    public boolean syncInitialize(AsyncChannel asyncChannel) {
        Message messageSendMessageSynchronously = asyncChannel.sendMessageSynchronously(CMD_INITIALIZE);
        MtkWifiServiceAdapter.initialize(this.mContext);
        boolean z = messageSendMessageSynchronously.arg1 != -1;
        messageSendMessageSynchronously.recycle();
        return z;
    }

    public void setPowerSavingMode(boolean z) {
        sendMessage(obtainMessage(M_CMD_SET_POWER_SAVING_MODE, z ? 1 : 0, 0));
    }

    public boolean isTemporarilyDontReconnectWifi() {
        log("stopReconnectWifi StopScan=" + this.mStopScanStarted.get() + " mDontReconnectAndScan=" + this.mDontReconnectAndScan.get());
        if (this.mStopScanStarted.get() || this.mDontReconnectAndScan.get()) {
            return true;
        }
        return false;
    }
}
