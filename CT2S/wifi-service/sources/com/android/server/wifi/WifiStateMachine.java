package com.android.server.wifi;

import android.R;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.backup.IBackupManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.DhcpResults;
import android.net.DhcpStateMachine;
import android.net.InterfaceConfiguration;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.NetworkAgent;
import android.net.NetworkCapabilities;
import android.net.NetworkFactory;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.NetworkUtils;
import android.net.RouteInfo;
import android.net.StaticIpConfiguration;
import android.net.TrafficStats;
import android.net.Uri;
import android.net.wifi.BatchedScanResult;
import android.net.wifi.BatchedScanSettings;
import android.net.wifi.RssiPacketCountInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.ScanSettings;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiChannel;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConnectionStatistics;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiLinkLayerStats;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiSsid;
import android.net.wifi.WpsInfo;
import android.net.wifi.WpsResult;
import android.net.wifi.p2p.IWifiP2pManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.WorkSource;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.LruCache;
import com.android.internal.app.IBatteryStats;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.net.NetlinkTracker;
import com.android.server.wifi.p2p.WifiP2pServiceImpl;
import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.ksoap2.kdom.Node;

public class WifiStateMachine extends StateMachine {
    private static final String ACTION_DELAYED_DRIVER_STOP = "com.android.server.WifiManager.action.DELAYED_DRIVER_STOP";
    private static final String ACTION_REFRESH_BATCHED_SCAN = "com.android.server.WifiManager.action.REFRESH_BATCHED_SCAN";
    private static final String ACTION_SET_WIFI_LOG_LEVEL = "com.android.LogMaster.setLogLev";
    private static final String ACTION_START_SCAN = "com.android.server.WifiManager.action.START_SCAN";
    static final int BASE = 131072;
    private static final String BATCHED_SETTING = "batched_settings";
    private static final String BATCHED_WORKSOURCE = "batched_worksource";
    private static final String BSSID_STR = "bssid=";
    static final int CMD_ADD_OR_UPDATE_NETWORK = 131124;
    static final int CMD_ASSOCIATED_BSSID = 131219;
    static final int CMD_AUTO_CONNECT = 131215;
    static final int CMD_AUTO_ROAM = 131217;
    static final int CMD_AUTO_SAVE_NETWORK = 131218;
    static final int CMD_BLACKLIST_NETWORK = 131128;
    static final int CMD_BLUETOOTH_ADAPTER_STATE_CHANGE = 131103;
    public static final int CMD_BOOT_COMPLETED = 131206;
    static final int CMD_CLEAR_BLACKLIST = 131129;
    static final int CMD_DELAYED_NETWORK_DISCONNECT = 131159;
    static final int CMD_DELAYED_STOP_DRIVER = 131090;
    static final int CMD_DISABLE_EPHEMERAL_NETWORK = 131170;
    public static final int CMD_DISABLE_P2P_REQ = 131204;
    public static final int CMD_DISABLE_P2P_RSP = 131205;
    static final int CMD_DISCONNECT = 131145;
    static final int CMD_DISCONNECTING_WATCHDOG_TIMER = 131168;
    static final int CMD_DRIVER_START_TIMED_OUT = 131091;
    static final int CMD_ENABLE_ALL_NETWORKS = 131127;
    static final int CMD_ENABLE_NETWORK = 131126;
    public static final int CMD_ENABLE_P2P = 131203;
    static final int CMD_ENABLE_RSSI_POLL = 131154;
    static final int CMD_ENABLE_TDLS = 131164;
    static final int CMD_GET_CAPABILITY_FREQ = 131132;
    static final int CMD_GET_CONFIGURED_NETWORKS = 131131;
    static final int CMD_GET_CONNECTION_STATISTICS = 131148;
    static final int CMD_GET_LINK_LAYER_STATS = 131135;
    static final int CMD_GET_PRIVILEGED_CONFIGURED_NETWORKS = 131134;
    static final int CMD_GET_SUPPORTED_FEATURES = 131133;
    static final int CMD_IP_CONFIGURATION_LOST = 131211;
    static final int CMD_IP_CONFIGURATION_SUCCESSFUL = 131210;
    static final int CMD_NETWORK_STATUS = 131220;
    static final int CMD_NO_NETWORKS_PERIODIC_SCAN = 131160;
    static final int CMD_OBTAINING_IP_ADDRESS_WATCHDOG_TIMER = 131165;
    static final int CMD_PING_SUPPLICANT = 131123;
    public static final int CMD_POLL_BATCHED_SCAN = 131209;
    static final int CMD_REASSOCIATE = 131147;
    static final int CMD_RECONNECT = 131146;
    static final int CMD_RELOAD_TLS_AND_RECONNECT = 131214;
    static final int CMD_REMOVE_NETWORK = 131125;
    static final int CMD_REQUEST_AP_CONFIG = 131099;
    static final int CMD_RESET_SUPPLICANT_STATE = 131183;
    static final int CMD_RESPONSE_AP_CONFIG = 131100;
    static final int CMD_ROAM_WATCHDOG_TIMER = 131166;
    static final int CMD_RSSI_POLL = 131155;
    static final int CMD_SAVE_CONFIG = 131130;
    static final int CMD_SCREEN_STATE_CHANGED = 131167;
    static final int CMD_SET_AP_CONFIG = 131097;
    static final int CMD_SET_AP_CONFIG_COMPLETED = 131098;
    public static final int CMD_SET_BATCHED_SCAN = 131207;
    static final int CMD_SET_COUNTRY_CODE = 131152;
    static final int CMD_SET_FREQUENCY_BAND = 131162;
    static final int CMD_SET_HIGH_PERF_MODE = 131149;
    static final int CMD_SET_OPERATIONAL_MODE = 131144;
    static final int CMD_SET_SUSPEND_OPT_ENABLED = 131158;
    static final int CMD_START_AP = 131093;
    static final int CMD_START_AP_FAILURE = 131095;
    static final int CMD_START_AP_SUCCESS = 131094;
    static final int CMD_START_DRIVER = 131085;
    public static final int CMD_START_NEXT_BATCHED_SCAN = 131208;
    static final int CMD_START_PACKET_FILTERING = 131156;
    static final int CMD_START_SCAN = 131143;
    static final int CMD_START_SUPPLICANT = 131083;
    static final int CMD_STATIC_IP_FAILURE = 131088;
    static final int CMD_STATIC_IP_SUCCESS = 131087;
    static final int CMD_STOP_AP = 131096;
    static final int CMD_STOP_DRIVER = 131086;
    static final int CMD_STOP_HOSTAPD_FAILED = 131104;
    static final int CMD_STOP_PACKET_FILTERING = 131157;
    static final int CMD_STOP_SUPPLICANT = 131084;
    static final int CMD_STOP_SUPPLICANT_FAILED = 131089;
    static final int CMD_TARGET_BSSID = 131213;
    static final int CMD_TEST_NETWORK_DISCONNECT = 131161;
    static final int CMD_TETHER_NOTIFICATION_TIMED_OUT = 131102;
    static final int CMD_TETHER_STATE_CHANGE = 131101;
    static final int CMD_UNWANTED_NETWORK = 131216;
    static final int CMD_UPDATE_LINKPROPERTIES = 131212;
    public static final int CONNECT_MODE = 1;
    private static final String CUSTOMIZED_SCAN_SETTING = "customized_scan_settings";
    private static final String CUSTOMIZED_SCAN_WORKSOURCE = "customized_scan_worksource";
    private static final String DELAYED_STOP_COUNTER = "DelayedStopCounter";
    private static final String DELIMITER_STR = "====";
    public static final int DFS_RESTRICTED_SCAN_REQUEST = -6;
    static final int DISCONNECTING_GUARD_TIMER_MSEC = 5000;
    private static final int DRIVER_START_TIME_OUT_MSECS = 10000;
    private static final int DRIVER_STOP_REQUEST = 0;
    private static final String END_STR = "####";
    private static final int FAILURE = -1;
    private static final String FLAGS_STR = "flags=";
    private static final String FREQ_STR = "freq=";
    private static final int GET_IPV6_INFO_TIME_MSECS = 3000;
    private static final String GOOGLE_OUI = "DA-A1-19";
    private static final String ID_STR = "id=";
    private static final String LEVEL_STR = "level=";
    private static final int LINK_FLAPPING_DEBOUNCE_MSEC = 7000;
    private static final int MIN_INTERVAL_ENABLE_ALL_NETWORKS_MS = 180000;
    static final int MSG_MHS_DEV_NAME = 1;
    static final int MULTICAST_V4 = 0;
    static final int MULTICAST_V6 = 1;
    private static final String NETWORKTYPE = "WIFI";
    private static final String NETWORKTYPE_UNTRUSTED = "WIFI_UT";
    static final int OBTAINING_IP_ADDRESS_GUARD_TIMER_MSEC = 40000;
    private static final int ONE_HOUR_MILLI = 3600000;
    private static final int POLL_RSSI_INTERVAL_MSECS = 3000;
    static final int ROAM_GUARD_TIMER_MSEC = 15000;
    public static final int SCAN_ONLY_MODE = 2;
    public static final int SCAN_ONLY_WITH_WIFI_OFF_MODE = 3;
    private static final int SCAN_REQUEST = 0;
    private static final int SCAN_REQUEST_BUFFER_MAX_SIZE = 10;
    private static final String SCAN_REQUEST_TIME = "scan_request_time";
    private static final int SCAN_RESULT_CACHE_SIZE = 160;
    private static final String SSID_STR = "ssid=";
    private static final int SUCCESS = 1;
    private static final int SUPPLICANT_RESTART_INTERVAL_MSECS = 5000;
    private static final int SUPPLICANT_RESTART_TRIES = 5;
    private static final int SUSPEND_DUE_TO_DHCP = 1;
    private static final int SUSPEND_DUE_TO_HIGH_PERF = 2;
    private static final int SUSPEND_DUE_TO_SCREEN = 4;
    private static final int TETHER_NOTIFICATION_TIME_OUT_MSECS = 5000;
    private static final String TSF_STR = "tsf=";
    private static final int UMTS_AUTH_SUCCESS = 219;
    private static final int UMTS_AUTH_SYNC_FAIL = 220;
    private static final int UNKNOWN_SCAN_SOURCE = -1;
    private static final int WIFI_MODE_AP = 1;
    private static final int WIFI_MODE_P2P_STA = 2;
    private static final int WIFI_MODE_STA = 0;
    static final int frameworkMinScanIntervalSaneValue = 10000;
    private static final long maxFullBandConnectedTimeIntervalMilli = 300000;
    static final int network_status_unwanted_disable_autojoin = 1;
    static final int network_status_unwanted_disconnect = 0;
    private boolean didBlackListBSSID;
    int disconnectingWatchdogCount;
    int emptyScanResultCount;
    private long fullBandConnectedTimeIntervalMilli;
    private long lastConnectAttempt;
    private WifiConfiguration lastForgetConfigurationAttempt;
    private long lastFullBandConnectedTimeMilli;
    private long lastLinkLayerStatsUpdate;
    private long lastOntimeReportTimeStamp;
    private WifiConfiguration lastSavedConfigurationAttempt;
    private long lastScanDuration;
    private String lastScanFreqs;
    private long lastScreenStateChangeTimeStamp;
    private long lastStartScanTimeStamp;
    private boolean linkDebouncing;
    private int mAggressiveHandover;
    private boolean mAlarmEnabled;
    private AlarmManager mAlarmManager;
    private int mAutoRoaming;
    private final boolean mBackgroundScanSupported;
    int mBadLinkspeedcount;
    private int mBatchedScanCsph;
    private PendingIntent mBatchedScanIntervalIntent;
    private long mBatchedScanMinPollTime;
    private int mBatchedScanOwnerUid;
    private final List<BatchedScanResult> mBatchedScanResults;
    private BatchedScanSettings mBatchedScanSettings;
    private WorkSource mBatchedScanWorkSource;
    private final IBatteryStats mBatteryStats;
    private boolean mBluetoothConnectionActive;
    private final Queue<Message> mBufferedScanMsg;
    private ConnectivityManager mCm;
    private State mConnectModeState;
    private State mConnectedState;
    private int mConnectionRequests;
    private Context mContext;
    private final AtomicInteger mCountryCodeSequence;
    private final int mDefaultFrameworkScanIntervalMs;
    private State mDefaultState;
    private AtomicInteger mDelayedScanCounter;
    private int mDelayedStopCounter;
    private final Handler mDevNameHandler;
    private boolean mDhcpActive;
    private DhcpResults mDhcpResults;
    private final Object mDhcpResultsLock;
    private DhcpStateMachine mDhcpStateMachine;
    private int mDisconnectedScanPeriodMs;
    private State mDisconnectedState;
    private long mDisconnectedTimeStamp;
    private State mDisconnectingState;
    private int mDriverStartToken;
    private State mDriverStartedState;
    private State mDriverStartingState;
    private final int mDriverStopDelayMs;
    private PendingIntent mDriverStopIntent;
    private State mDriverStoppedState;
    private State mDriverStoppingState;
    private boolean mEnableBackgroundScan;
    private boolean mEnableRssiPolling;
    private int mExpectedBatchedScans;
    private AtomicBoolean mFilteringMulticastV4Packets;
    private long mFrameworkScanIntervalMs;
    private AtomicInteger mFrequencyBand;
    private int mHostapdStopFailureToken;
    private boolean mInDelayedStop;
    private State mInitialState;
    private String mInterfaceName;
    private boolean mIsFullScanOngoing;
    private boolean mIsRunning;
    private boolean mIsScanOngoing;
    private State mL2ConnectedState;
    private String mLastBssid;
    private long mLastDriverRoamAttempt;
    private long mLastEnableAllNetworksTime;
    private int mLastNetworkId;
    private final WorkSource mLastRunningWifiUids;
    private String mLastSetCountryCode;
    private int mLastSignalLevel;
    private LinkProperties mLinkProperties;
    private NetlinkTracker mNetlinkTracker;
    private WifiNetworkAgent mNetworkAgent;
    private NetworkCapabilities mNetworkCapabilities;
    private final NetworkCapabilities mNetworkCapabilitiesFilter;
    private WifiNetworkFactory mNetworkFactory;
    private NetworkInfo mNetworkInfo;
    private int mNotedBatchedScanCsph;
    private WorkSource mNotedBatchedScanWorkSource;
    private int mNumScanResultsKnown;
    private int mNumScanResultsReturned;
    private INetworkManagementService mNwService;
    private State mObtainingIpState;
    private int mOnTime;
    private int mOnTimeAtLastReport;
    private int mOnTimeLastReport;
    private int mOnTimeScan;
    private int mOnTimeScreenStateChange;
    private int mOnTimeStartScan;
    private int mOnTimeThisScan;
    private int mOperationalMode;
    private final AtomicBoolean mP2pConnected;
    private final boolean mP2pSupported;
    private final WifiP2pDeviceList mPeers;
    private int mPeriodicScanToken;
    private volatile String mPersistedCountryCode;
    private final String mPrimaryDeviceType;
    private AsyncChannel mReplyChannel;
    private boolean mReportedRunning;
    private int mRoamFailCount;
    private State mRoamingState;
    private int mRssiPollToken;
    int mRunningBeaconCount;
    private final WorkSource mRunningWifiUids;
    private int mRxTime;
    private int mRxTimeLastReport;
    private int mRxTimeScan;
    private int mRxTimeStartScan;
    private int mRxTimeThisScan;
    private PendingIntent mScanIntent;
    private State mScanModeState;
    private final LruCache<String, ScanResult> mScanResultCache;
    private List<ScanResult> mScanResults;
    private WorkSource mScanWorkSource;
    private AtomicBoolean mScreenBroadcastReceived;
    private boolean mScreenOn;
    private boolean mSendScanResultsBroadcast;
    private State mSoftApStartedState;
    private State mSoftApStartingState;
    private State mSoftApStoppingState;
    private int mSupplicantRestartCount;
    private long mSupplicantScanIntervalMs;
    private State mSupplicantStartedState;
    private State mSupplicantStartingState;
    private SupplicantStateTracker mSupplicantStateTracker;
    private int mSupplicantStopFailureToken;
    private State mSupplicantStoppingState;
    private int mSuspendOptNeedsDisabled;
    private PowerManager.WakeLock mSuspendWakeLock;
    private String mTargetRoamBSSID;
    private String mTcpBufferSizes;
    private boolean mTemporarilyDisconnectWifi;
    private String mTetherInterfaceName;
    private int mTetherToken;
    private State mTetheredState;
    private State mTetheringState;
    private int mTxTime;
    private int mTxTimeLastReport;
    private int mTxTimeScan;
    private int mTxTimeStartScan;
    private int mTxTimeThisScan;
    private State mUntetheringState;
    private UntrustedWifiNetworkFactory mUntrustedNetworkFactory;
    private AtomicBoolean mUserWantsSuspendOpt;
    private int mVerboseLoggingLevel;
    private State mVerifyingLinkState;
    private State mWaitForP2pDisableState;
    private PowerManager.WakeLock mWakeLock;
    private AsyncChannel mWifiApConfigChannel;
    private final AtomicInteger mWifiApState;
    private WifiAutoJoinController mWifiAutoJoinController;
    private WifiConfigStore mWifiConfigStore;
    private WifiConnectionStatistics mWifiConnectionStatistics;
    private WifiInfo mWifiInfo;
    private int mWifiLinkLayerStatsSupported;
    private WifiMonitor mWifiMonitor;
    private WifiNative mWifiNative;
    private AsyncChannel mWifiP2pChannel;
    private WifiP2pServiceImpl mWifiP2pServiceImpl;
    private final AtomicInteger mWifiState;
    private State mWpsRunningState;
    private int messageHandlingStatus;
    int obtainingIpWatchdogCount;
    int roamWatchdogCount;
    private WifiConfiguration targetWificonfiguration;
    private boolean testNetworkDisconnect;
    private int testNetworkDisconnectCounter;
    String wifiScoringReport;
    private static final boolean DEBUG_PARSE = false;
    private static boolean DBG = DEBUG_PARSE;
    private static boolean VDBG = DEBUG_PARSE;
    private static boolean VVDBG = DEBUG_PARSE;
    private static boolean mLogMessages = DEBUG_PARSE;
    private static boolean PDBG = DEBUG_PARSE;
    private static final Pattern scanResultPattern = Pattern.compile("\t+");
    private static int sScanAlarmIntentCount = 0;
    private static int MESSAGE_HANDLING_STATUS_PROCESSED = 2;
    private static int MESSAGE_HANDLING_STATUS_OK = 1;
    private static int MESSAGE_HANDLING_STATUS_UNKNOWN = 0;
    private static int MESSAGE_HANDLING_STATUS_REFUSED = -1;
    private static final int SCAN_ALARM_SOURCE = -2;
    private static int MESSAGE_HANDLING_STATUS_FAIL = SCAN_ALARM_SOURCE;
    private static final int ADD_OR_UPDATE_SOURCE = -3;
    private static int MESSAGE_HANDLING_STATUS_OBSOLETE = ADD_OR_UPDATE_SOURCE;
    private static final int SET_ALLOW_UNTRUSTED_SOURCE = -4;
    private static int MESSAGE_HANDLING_STATUS_DEFERRED = SET_ALLOW_UNTRUSTED_SOURCE;
    private static final int ENABLE_WIFI = -5;
    private static int MESSAGE_HANDLING_STATUS_DISCARD = ENABLE_WIFI;
    private static int MESSAGE_HANDLING_STATUS_LOOPED = -6;
    private static int MESSAGE_HANDLING_STATUS_HANDLING_ERROR = -7;
    private static Pattern mNotZero = Pattern.compile("[1-9a-fA-F]");

    public static class SimAuthRequestData {
        String[] challenges;
        int networkId;
        int protocol;
        String ssid;
    }

    public static class SimAuthResponseData {
        String Kc1;
        String Kc2;
        String Kc3;
        String SRES1;
        String SRES2;
        String SRES3;
        int id;
    }

    static int access$10108(WifiStateMachine x0) {
        int i = x0.mDelayedStopCounter;
        x0.mDelayedStopCounter = i + 1;
        return i;
    }

    static int access$1404(WifiStateMachine x0) {
        int i = x0.mConnectionRequests + 1;
        x0.mConnectionRequests = i;
        return i;
    }

    static int access$1406(WifiStateMachine x0) {
        int i = x0.mConnectionRequests - 1;
        x0.mConnectionRequests = i;
        return i;
    }

    static int access$17508(WifiStateMachine x0) {
        int i = x0.mRssiPollToken;
        x0.mRssiPollToken = i + 1;
        return i;
    }

    static int access$208() {
        int i = sScanAlarmIntentCount;
        sScanAlarmIntentCount = i + 1;
        return i;
    }

    static int access$21308(WifiStateMachine x0) {
        int i = x0.mRoamFailCount;
        x0.mRoamFailCount = i + 1;
        return i;
    }

    static int access$22208(WifiStateMachine x0) {
        int i = x0.testNetworkDisconnectCounter;
        x0.testNetworkDisconnectCounter = i + 1;
        return i;
    }

    static int access$23804(WifiStateMachine x0) {
        int i = x0.mPeriodicScanToken + 1;
        x0.mPeriodicScanToken = i;
        return i;
    }

    static int access$26704(WifiStateMachine x0) {
        int i = x0.mHostapdStopFailureToken + 1;
        x0.mHostapdStopFailureToken = i;
        return i;
    }

    static int access$27104(WifiStateMachine x0) {
        int i = x0.mTetherToken + 1;
        x0.mTetherToken = i;
        return i;
    }

    static int access$4904(WifiStateMachine x0) {
        int i = x0.mSupplicantRestartCount + 1;
        x0.mSupplicantRestartCount = i;
        return i;
    }

    static int access$8704(WifiStateMachine x0) {
        int i = x0.mSupplicantStopFailureToken + 1;
        x0.mSupplicantStopFailureToken = i;
        return i;
    }

    static int access$9104(WifiStateMachine x0) {
        int i = x0.mDriverStartToken + 1;
        x0.mDriverStartToken = i;
        return i;
    }

    protected void loge(String s) {
        Log.e(getName(), s);
    }

    protected void log(String s) {
        Log.e(getName(), s);
    }

    boolean isRoaming() {
        if (this.mAutoRoaming == 1 || this.mAutoRoaming == 2) {
            return true;
        }
        return DEBUG_PARSE;
    }

    public void autoRoamSetBSSID(int netId, String bssid) {
        autoRoamSetBSSID(this.mWifiConfigStore.getWifiConfiguration(netId), bssid);
    }

    public boolean autoRoamSetBSSID(WifiConfiguration config, String bssid) {
        boolean ret = true;
        if (this.mTargetRoamBSSID == null) {
            this.mTargetRoamBSSID = "any";
        }
        if (bssid == null) {
            bssid = "any";
        }
        if (config == null) {
            return DEBUG_PARSE;
        }
        if (this.mTargetRoamBSSID != null && bssid == this.mTargetRoamBSSID && bssid == config.BSSID) {
            return DEBUG_PARSE;
        }
        if (!this.mTargetRoamBSSID.equals("any") && bssid.equals("any") && !this.mWifiConfigStore.roamOnAny) {
            ret = DEBUG_PARSE;
        }
        if (VDBG) {
            loge("autoRoamSetBSSID " + bssid + " key=" + config.configKey());
        }
        config.autoJoinBSSID = bssid;
        this.mTargetRoamBSSID = bssid;
        this.mWifiConfigStore.saveWifiConfigBSSID(config);
        return ret;
    }

    private class TetherStateChange {
        ArrayList<String> active;
        ArrayList<String> available;

        TetherStateChange(ArrayList<String> av, ArrayList<String> ac) {
            this.available = av;
            this.active = ac;
        }
    }

    public WifiStateMachine(Context context, String wlanInterface, WifiTrafficPoller trafficPoller) {
        super("WifiStateMachine");
        this.didBlackListBSSID = DEBUG_PARSE;
        this.mP2pConnected = new AtomicBoolean(DEBUG_PARSE);
        this.mTemporarilyDisconnectWifi = DEBUG_PARSE;
        this.mScanResults = new ArrayList();
        this.mBatchedScanResults = new ArrayList();
        this.mBatchedScanOwnerUid = -1;
        this.mExpectedBatchedScans = 0;
        this.mBatchedScanMinPollTime = 0L;
        this.mScreenOn = DEBUG_PARSE;
        this.mLastSignalLevel = -1;
        this.linkDebouncing = DEBUG_PARSE;
        this.testNetworkDisconnect = DEBUG_PARSE;
        this.mEnableRssiPolling = DEBUG_PARSE;
        this.mEnableBackgroundScan = DEBUG_PARSE;
        this.mRssiPollToken = 0;
        this.mOperationalMode = 1;
        this.mIsScanOngoing = DEBUG_PARSE;
        this.mIsFullScanOngoing = DEBUG_PARSE;
        this.mSendScanResultsBroadcast = DEBUG_PARSE;
        this.mBufferedScanMsg = new LinkedList();
        this.mScanWorkSource = null;
        this.mScreenBroadcastReceived = new AtomicBoolean(DEBUG_PARSE);
        this.mBluetoothConnectionActive = DEBUG_PARSE;
        this.mSupplicantRestartCount = 0;
        this.mSupplicantStopFailureToken = 0;
        this.mHostapdStopFailureToken = 0;
        this.mTetherToken = 0;
        this.mDriverStartToken = 0;
        this.mPeriodicScanToken = 0;
        this.mDhcpResultsLock = new Object();
        this.mDhcpActive = DEBUG_PARSE;
        this.mWifiLinkLayerStatsSupported = 4;
        this.mCountryCodeSequence = new AtomicInteger();
        this.mAutoRoaming = 0;
        this.mRoamFailCount = 0;
        this.mTargetRoamBSSID = "any";
        this.mLastDriverRoamAttempt = 0L;
        this.targetWificonfiguration = null;
        this.lastSavedConfigurationAttempt = null;
        this.lastForgetConfigurationAttempt = null;
        this.mFrequencyBand = new AtomicInteger(0);
        this.mFilteringMulticastV4Packets = new AtomicBoolean(true);
        this.mReplyChannel = new AsyncChannel();
        this.mConnectionRequests = 0;
        this.mWifiConnectionStatistics = new WifiConnectionStatistics();
        this.mNetworkCapabilitiesFilter = new NetworkCapabilities();
        this.testNetworkDisconnectCounter = 0;
        this.obtainingIpWatchdogCount = 0;
        this.roamWatchdogCount = 0;
        this.disconnectingWatchdogCount = 0;
        this.mSuspendOptNeedsDisabled = 0;
        this.mUserWantsSuspendOpt = new AtomicBoolean(true);
        this.mDisconnectedScanPeriodMs = 10000;
        this.mRunningBeaconCount = 0;
        this.mInDelayedStop = DEBUG_PARSE;
        this.mDefaultState = new DefaultState();
        this.mInitialState = new InitialState();
        this.mSupplicantStartingState = new SupplicantStartingState();
        this.mSupplicantStartedState = new SupplicantStartedState();
        this.mSupplicantStoppingState = new SupplicantStoppingState();
        this.mDriverStartingState = new DriverStartingState();
        this.mDriverStartedState = new DriverStartedState();
        this.mWaitForP2pDisableState = new WaitForP2pDisableState();
        this.mDriverStoppingState = new DriverStoppingState();
        this.mDriverStoppedState = new DriverStoppedState();
        this.mScanModeState = new ScanModeState();
        this.mConnectModeState = new ConnectModeState();
        this.mL2ConnectedState = new L2ConnectedState();
        this.mObtainingIpState = new ObtainingIpState();
        this.mVerifyingLinkState = new VerifyingLinkState();
        this.mConnectedState = new ConnectedState();
        this.mRoamingState = new RoamingState();
        this.mDisconnectingState = new DisconnectingState();
        this.mDisconnectedState = new DisconnectedState();
        this.mWpsRunningState = new WpsRunningState();
        this.mSoftApStartingState = new SoftApStartingState();
        this.mSoftApStartedState = new SoftApStartedState();
        this.mSoftApStoppingState = new SoftApStoppingState();
        this.mTetheringState = new TetheringState();
        this.mTetheredState = new TetheredState();
        this.mUntetheringState = new UntetheringState();
        this.mWifiState = new AtomicInteger(1);
        this.mWifiApState = new AtomicInteger(11);
        this.mIsRunning = DEBUG_PARSE;
        this.mReportedRunning = DEBUG_PARSE;
        this.mRunningWifiUids = new WorkSource();
        this.mLastRunningWifiUids = new WorkSource();
        this.mBatchedScanSettings = null;
        this.mBatchedScanWorkSource = null;
        this.mBatchedScanCsph = 0;
        this.mNotedBatchedScanWorkSource = null;
        this.mNotedBatchedScanCsph = 0;
        this.mTcpBufferSizes = null;
        this.mVerboseLoggingLevel = 0;
        this.mAggressiveHandover = 0;
        this.mAlarmEnabled = DEBUG_PARSE;
        this.mFrameworkScanIntervalMs = 10000L;
        this.mDelayedScanCounter = new AtomicInteger();
        this.mDisconnectedTimeStamp = 0L;
        this.lastStartScanTimeStamp = 0L;
        this.lastScanDuration = 0L;
        this.lastConnectAttempt = 0L;
        this.lastScanFreqs = null;
        this.messageHandlingStatus = 0;
        this.mOnTime = 0;
        this.mTxTime = 0;
        this.mRxTime = 0;
        this.mOnTimeStartScan = 0;
        this.mTxTimeStartScan = 0;
        this.mRxTimeStartScan = 0;
        this.mOnTimeScan = 0;
        this.mTxTimeScan = 0;
        this.mRxTimeScan = 0;
        this.mOnTimeThisScan = 0;
        this.mTxTimeThisScan = 0;
        this.mRxTimeThisScan = 0;
        this.mOnTimeScreenStateChange = 0;
        this.mOnTimeAtLastReport = 0;
        this.lastOntimeReportTimeStamp = 0L;
        this.lastScreenStateChangeTimeStamp = 0L;
        this.mOnTimeLastReport = 0;
        this.mTxTimeLastReport = 0;
        this.mRxTimeLastReport = 0;
        this.lastLinkLayerStatsUpdate = 0L;
        this.emptyScanResultCount = 0;
        this.mBadLinkspeedcount = 0;
        this.wifiScoringReport = null;
        this.mContext = context;
        this.mInterfaceName = wlanInterface;
        this.mNetworkInfo = new NetworkInfo(1, 0, NETWORKTYPE, "");
        this.mBatteryStats = IBatteryStats.Stub.asInterface(ServiceManager.getService("batterystats"));
        IBinder b = ServiceManager.getService("network_management");
        this.mNwService = INetworkManagementService.Stub.asInterface(b);
        this.mP2pSupported = this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi.direct");
        this.mWifiNative = new WifiNative(this.mInterfaceName);
        this.mWifiConfigStore = new WifiConfigStore(context, this.mWifiNative);
        this.mWifiAutoJoinController = new WifiAutoJoinController(context, this, this.mWifiConfigStore, this.mWifiConnectionStatistics, this.mWifiNative);
        this.mWifiMonitor = new WifiMonitor(this, this.mWifiNative);
        this.mWifiInfo = new WifiInfo();
        this.mSupplicantStateTracker = new SupplicantStateTracker(context, this, this.mWifiConfigStore, getHandler());
        this.mLinkProperties = new LinkProperties();
        IBinder s1 = ServiceManager.getService("wifip2p");
        this.mWifiP2pServiceImpl = IWifiP2pManager.Stub.asInterface(s1);
        this.mPeers = new WifiP2pDeviceList();
        this.mDevNameHandler = new Handler(getHandler().getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                WifiP2pDevice peer = (WifiP2pDevice) msg.obj;
                WifiStateMachine.this.mPeers.updateSupplicantDetails(peer);
                WifiStateMachine.this.sendWifiApClientChangedBroadcast();
                WifiStateMachine.this.loge("" + peer.toString());
                WifiStateMachine.this.loge("print out hostname " + peer.deviceName);
            }
        };
        this.mNetworkInfo.setIsAvailable(DEBUG_PARSE);
        this.mLastBssid = null;
        this.mLastNetworkId = -1;
        this.mLastSignalLevel = -1;
        this.mNetlinkTracker = new NetlinkTracker(this.mInterfaceName, new NetlinkTracker.Callback() {
            public void update() {
                WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_UPDATE_LINKPROPERTIES);
            }
        });
        try {
            this.mNwService.registerObserver(this.mNetlinkTracker);
        } catch (RemoteException e) {
            loge("Couldn't register netlink tracker: " + e.toString());
        }
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        this.mScanIntent = getPrivateBroadcast(ACTION_START_SCAN, 0);
        this.mBatchedScanIntervalIntent = getPrivateBroadcast(ACTION_REFRESH_BATCHED_SCAN, 0);
        int period = this.mContext.getResources().getInteger(R.integer.config_autoPowerModeThresholdAngle);
        this.mDefaultFrameworkScanIntervalMs = period < 10000 ? 10000 : period;
        this.mDriverStopDelayMs = this.mContext.getResources().getInteger(R.integer.config_bluetooth_idle_cur_ma);
        this.mBackgroundScanSupported = this.mContext.getResources().getBoolean(R.^attr-private.autofillDatasetPickerMaxWidth);
        this.mPrimaryDeviceType = this.mContext.getResources().getString(R.string.config_helpPackageNameValue);
        this.mUserWantsSuspendOpt.set(Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_suspend_optimizations_enabled", 1) == 1 ? true : DEBUG_PARSE);
        this.mNetworkCapabilitiesFilter.addTransportType(1);
        this.mNetworkCapabilitiesFilter.addCapability(12);
        this.mNetworkCapabilitiesFilter.addCapability(13);
        this.mNetworkCapabilitiesFilter.setLinkUpstreamBandwidthKbps(1048576);
        this.mNetworkCapabilitiesFilter.setLinkDownstreamBandwidthKbps(1048576);
        this.mNetworkCapabilities = new NetworkCapabilities(this.mNetworkCapabilitiesFilter);
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                ArrayList<String> available = intent.getStringArrayListExtra("availableArray");
                ArrayList<String> active = intent.getStringArrayListExtra("activeArray");
                WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_TETHER_STATE_CHANGE, WifiStateMachine.this.new TetherStateChange(available, active));
            }
        }, new IntentFilter("android.net.conn.TETHER_STATE_CHANGED"));
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                WifiStateMachine.access$208();
                if (!WifiStateMachine.this.mP2pConnected.get()) {
                    WifiStateMachine.this.startScan(WifiStateMachine.SCAN_ALARM_SOURCE, WifiStateMachine.this.mDelayedScanCounter.incrementAndGet(), null, null);
                    if (WifiStateMachine.VDBG) {
                        WifiStateMachine.this.loge("WiFiStateMachine SCAN ALARM -> " + WifiStateMachine.this.mDelayedScanCounter.get());
                    }
                }
            }
        }, new IntentFilter(ACTION_START_SCAN));
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SCREEN_ON");
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.addAction(ACTION_REFRESH_BATCHED_SCAN);
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if (action.equals("android.intent.action.SCREEN_ON")) {
                    WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_SCREEN_STATE_CHANGED, 1);
                } else if (action.equals("android.intent.action.SCREEN_OFF")) {
                    WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_SCREEN_STATE_CHANGED, 0);
                } else if (action.equals(WifiStateMachine.ACTION_REFRESH_BATCHED_SCAN)) {
                    WifiStateMachine.this.startNextBatchedScanAsync();
                }
            }
        }, filter);
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                int counter = intent.getIntExtra(WifiStateMachine.DELAYED_STOP_COUNTER, 0);
                WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_DELAYED_STOP_DRIVER, counter, 0);
            }
        }, new IntentFilter(ACTION_DELAYED_DRIVER_STOP));
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                WifiStateMachine.this.setWifiLogLev(context2, intent);
            }
        }, new IntentFilter(ACTION_SET_WIFI_LOG_LEVEL));
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("wifi_suspend_optimizations_enabled"), DEBUG_PARSE, new ContentObserver(getHandler()) {
            @Override
            public void onChange(boolean selfChange) {
                WifiStateMachine.this.mUserWantsSuspendOpt.set(Settings.Global.getInt(WifiStateMachine.this.mContext.getContentResolver(), "wifi_suspend_optimizations_enabled", 1) != 1 ? WifiStateMachine.DEBUG_PARSE : true);
            }
        });
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_BOOT_COMPLETED);
            }
        }, new IntentFilter("android.intent.action.BOOT_COMPLETED"));
        this.mScanResultCache = new LruCache<>(SCAN_RESULT_CACHE_SIZE);
        PowerManager powerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mWakeLock = powerManager.newWakeLock(1, getName());
        this.mSuspendWakeLock = powerManager.newWakeLock(1, "WifiSuspend");
        this.mSuspendWakeLock.setReferenceCounted(DEBUG_PARSE);
        this.mTcpBufferSizes = this.mContext.getResources().getString(R.string.config_systemAutomotiveCalendarSyncManager);
        addState(this.mDefaultState);
        addState(this.mInitialState, this.mDefaultState);
        addState(this.mSupplicantStartingState, this.mDefaultState);
        addState(this.mSupplicantStartedState, this.mDefaultState);
        addState(this.mDriverStartingState, this.mSupplicantStartedState);
        addState(this.mDriverStartedState, this.mSupplicantStartedState);
        addState(this.mScanModeState, this.mDriverStartedState);
        addState(this.mConnectModeState, this.mDriverStartedState);
        addState(this.mL2ConnectedState, this.mConnectModeState);
        addState(this.mObtainingIpState, this.mL2ConnectedState);
        addState(this.mVerifyingLinkState, this.mL2ConnectedState);
        addState(this.mConnectedState, this.mL2ConnectedState);
        addState(this.mRoamingState, this.mL2ConnectedState);
        addState(this.mDisconnectingState, this.mConnectModeState);
        addState(this.mDisconnectedState, this.mConnectModeState);
        addState(this.mWpsRunningState, this.mConnectModeState);
        addState(this.mWaitForP2pDisableState, this.mSupplicantStartedState);
        addState(this.mDriverStoppingState, this.mSupplicantStartedState);
        addState(this.mDriverStoppedState, this.mSupplicantStartedState);
        addState(this.mSupplicantStoppingState, this.mDefaultState);
        addState(this.mSoftApStartingState, this.mDefaultState);
        addState(this.mSoftApStartedState, this.mDefaultState);
        addState(this.mSoftApStoppingState, this.mDefaultState);
        addState(this.mTetheringState, this.mSoftApStartedState);
        addState(this.mTetheredState, this.mSoftApStartedState);
        addState(this.mUntetheringState, this.mSoftApStartedState);
        setInitialState(this.mInitialState);
        setLogRecSize(ActivityManager.isLowRamDeviceStatic() ? 100 : 3000);
        setLogOnlyTransitions(DEBUG_PARSE);
        if (VDBG) {
            setDbg(true);
        }
        start();
        Intent intent = new Intent("wifi_scan_available");
        intent.addFlags(67108864);
        intent.putExtra("scan_enabled", 1);
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    PendingIntent getPrivateBroadcast(String action, int requestCode) {
        Intent intent = new Intent(action, (Uri) null);
        intent.addFlags(67108864);
        intent.setPackage(getClass().getPackage().getName());
        return PendingIntent.getBroadcast(this.mContext, requestCode, intent, 0);
    }

    int getVerboseLoggingLevel() {
        return this.mVerboseLoggingLevel;
    }

    void enableVerboseLogging(int verbose) {
        this.mVerboseLoggingLevel = verbose;
        if (verbose > 0) {
            DBG = true;
            VDBG = true;
            PDBG = true;
            mLogMessages = true;
            this.mWifiNative.setSupplicantLogLevel("DEBUG");
        } else {
            DBG = DEBUG_PARSE;
            VDBG = DEBUG_PARSE;
            PDBG = DEBUG_PARSE;
            mLogMessages = DEBUG_PARSE;
            this.mWifiNative.setSupplicantLogLevel("INFO");
        }
        this.mWifiAutoJoinController.enableVerboseLogging(verbose);
        this.mWifiMonitor.enableVerboseLogging(verbose);
        this.mWifiNative.enableVerboseLogging(verbose);
        this.mWifiConfigStore.enableVerboseLogging(verbose);
        this.mSupplicantStateTracker.enableVerboseLogging(verbose);
    }

    int getAggressiveHandover() {
        return this.mAggressiveHandover;
    }

    void enableAggressiveHandover(int enabled) {
        this.mAggressiveHandover = enabled;
    }

    public void setAllowScansWithTraffic(int enabled) {
        this.mWifiConfigStore.alwaysEnableScansWhileAssociated = enabled;
    }

    public int getAllowScansWithTraffic() {
        return this.mWifiConfigStore.alwaysEnableScansWhileAssociated;
    }

    private void setScanAlarm(boolean enabled) {
        if (PDBG) {
            loge("setScanAlarm " + enabled + " period " + this.mDefaultFrameworkScanIntervalMs + " mBackgroundScanSupported " + this.mBackgroundScanSupported);
        }
        if (!this.mBackgroundScanSupported) {
            enabled = true;
        }
        if (enabled != this.mAlarmEnabled) {
            if (enabled) {
                this.mAlarmManager.set(0, System.currentTimeMillis() + ((long) this.mDefaultFrameworkScanIntervalMs), this.mScanIntent);
                this.mAlarmEnabled = true;
            } else {
                this.mAlarmManager.cancel(this.mScanIntent);
                this.mAlarmEnabled = DEBUG_PARSE;
            }
        }
    }

    private void cancelDelayedScan() {
        this.mDelayedScanCounter.incrementAndGet();
        loge("cancelDelayedScan -> " + this.mDelayedScanCounter);
    }

    private boolean checkAndRestartDelayedScan(int counter, boolean restart, int milli, ScanSettings settings, WorkSource workSource) {
        if (counter != this.mDelayedScanCounter.get()) {
            return DEBUG_PARSE;
        }
        if (restart) {
            startDelayedScan(milli, settings, workSource);
        }
        return true;
    }

    private void startDelayedScan(int milli, ScanSettings settings, WorkSource workSource) {
        if (milli > 0) {
            this.mDelayedScanCounter.incrementAndGet();
            if (this.mScreenOn && (getCurrentState() == this.mDisconnectedState || getCurrentState() == this.mConnectedState)) {
                Bundle bundle = new Bundle();
                bundle.putParcelable(CUSTOMIZED_SCAN_SETTING, settings);
                bundle.putParcelable(CUSTOMIZED_SCAN_WORKSOURCE, workSource);
                bundle.putLong(SCAN_REQUEST_TIME, System.currentTimeMillis());
                sendMessageDelayed(CMD_START_SCAN, SCAN_ALARM_SOURCE, this.mDelayedScanCounter.get(), bundle, milli);
                if (DBG) {
                    loge("startDelayedScan send -> " + this.mDelayedScanCounter + " milli " + milli);
                    return;
                }
                return;
            }
            if (!this.mBackgroundScanSupported && !this.mScreenOn && getCurrentState() == this.mDisconnectedState) {
                setScanAlarm(true);
                if (DBG) {
                    loge("startDelayedScan start scan alarm -> " + this.mDelayedScanCounter + " milli " + milli);
                    return;
                }
                return;
            }
            if (DBG) {
                loge("startDelayedScan unhandled -> " + this.mDelayedScanCounter + " milli " + milli);
            }
        }
    }

    private boolean setRandomMacOui() {
        String oui = this.mContext.getResources().getString(R.string.config_helpIntentExtraKey, GOOGLE_OUI);
        String[] ouiParts = oui.split("-");
        byte[] ouiBytes = {(byte) (Integer.parseInt(ouiParts[0], 16) & 255), (byte) (Integer.parseInt(ouiParts[1], 16) & 255), (byte) (Integer.parseInt(ouiParts[2], 16) & 255)};
        logd("Setting OUI to " + oui);
        WifiNative wifiNative = this.mWifiNative;
        return WifiNative.setScanningMacOui(ouiBytes);
    }

    public Messenger getMessenger() {
        return new Messenger(getHandler());
    }

    public WifiMonitor getWifiMonitor() {
        return this.mWifiMonitor;
    }

    public boolean syncPingSupplicant(AsyncChannel channel) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_PING_SUPPLICANT);
        boolean result = resultMsg.arg1 != -1 ? true : DEBUG_PARSE;
        resultMsg.recycle();
        return result;
    }

    public List<WifiChannel> syncGetChannelList(AsyncChannel channel) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_GET_CAPABILITY_FREQ);
        List<WifiChannel> list = null;
        if (resultMsg.obj != null) {
            list = new ArrayList<>();
            String freqs = (String) resultMsg.obj;
            String[] lines = freqs.split("\n");
            for (String line : lines) {
                if (line.contains("MHz")) {
                    WifiChannel c = new WifiChannel();
                    String[] prop = line.split(" ");
                    if (prop.length >= 5) {
                        try {
                            c.channelNum = Integer.parseInt(prop[1]);
                            c.freqMHz = Integer.parseInt(prop[3]);
                        } catch (NumberFormatException e) {
                        }
                        c.isDFS = line.contains("(DFS)");
                        list.add(c);
                    }
                } else if (line.contains("Mode[B] Channels:")) {
                    break;
                }
            }
        }
        resultMsg.recycle();
        if (list == null || list.size() <= 0) {
            return null;
        }
        return list;
    }

    public void startScanForUntrustedSettingChange() {
        startScan(SET_ALLOW_UNTRUSTED_SOURCE, 0, null, null);
    }

    public void startScan(int callingUid, int scanCounter, ScanSettings settings, WorkSource workSource) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(CUSTOMIZED_SCAN_SETTING, settings);
        bundle.putParcelable(CUSTOMIZED_SCAN_WORKSOURCE, workSource);
        bundle.putLong(SCAN_REQUEST_TIME, System.currentTimeMillis());
        sendMessage(CMD_START_SCAN, callingUid, scanCounter, bundle);
    }

    public void setBatchedScanSettings(BatchedScanSettings settings, int callingUid, int csph, WorkSource workSource) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(BATCHED_SETTING, settings);
        bundle.putParcelable(BATCHED_WORKSOURCE, workSource);
        sendMessage(CMD_SET_BATCHED_SCAN, callingUid, csph, bundle);
    }

    public List<BatchedScanResult> syncGetBatchedScanResultsList() {
        List<BatchedScanResult> batchedScanList;
        synchronized (this.mBatchedScanResults) {
            batchedScanList = new ArrayList<>(this.mBatchedScanResults.size());
            for (BatchedScanResult result : this.mBatchedScanResults) {
                batchedScanList.add(new BatchedScanResult(result));
            }
        }
        return batchedScanList;
    }

    public void requestBatchedScanPoll() {
        sendMessage(CMD_POLL_BATCHED_SCAN);
    }

    private void startBatchedScan() {
        if (this.mBatchedScanSettings != null) {
            if (this.mDhcpActive) {
                if (DBG) {
                    log("not starting Batched Scans due to DHCP");
                    return;
                }
                return;
            }
            retrieveBatchedScanData();
            if (PDBG) {
                loge("try  starting Batched Scans due to DHCP");
            }
            this.mAlarmManager.cancel(this.mBatchedScanIntervalIntent);
            String scansExpected = this.mWifiNative.setBatchedScanSettings(this.mBatchedScanSettings);
            try {
                this.mExpectedBatchedScans = Integer.parseInt(scansExpected);
                setNextBatchedAlarm(this.mExpectedBatchedScans);
                if (this.mExpectedBatchedScans > 0) {
                    noteBatchedScanStart();
                }
            } catch (NumberFormatException e) {
                stopBatchedScan();
                loge("Exception parsing WifiNative.setBatchedScanSettings response " + e);
            }
        }
    }

    private void startNextBatchedScanAsync() {
        sendMessage(CMD_START_NEXT_BATCHED_SCAN);
    }

    private void startNextBatchedScan() {
        retrieveBatchedScanData();
        setNextBatchedAlarm(this.mExpectedBatchedScans);
    }

    private void handleBatchedScanPollRequest() {
        if (DBG) {
            log("handleBatchedScanPoll Request - mBatchedScanMinPollTime=" + this.mBatchedScanMinPollTime + " , mBatchedScanSettings=" + this.mBatchedScanSettings);
        }
        if (this.mBatchedScanMinPollTime != 0 && this.mBatchedScanSettings != null) {
            long now = System.currentTimeMillis();
            if (now > this.mBatchedScanMinPollTime) {
                startNextBatchedScan();
            } else {
                this.mAlarmManager.setExact(0, this.mBatchedScanMinPollTime, this.mBatchedScanIntervalIntent);
                this.mBatchedScanMinPollTime = 0L;
            }
        }
    }

    private boolean recordBatchedScanSettings(int responsibleUid, int csph, Bundle bundle) {
        BatchedScanSettings settings = bundle.getParcelable(BATCHED_SETTING);
        WorkSource responsibleWorkSource = (WorkSource) bundle.getParcelable(BATCHED_WORKSOURCE);
        if (DBG) {
            log("set batched scan to " + settings + " for uid=" + responsibleUid + ", worksource=" + responsibleWorkSource);
        }
        if (settings != null) {
            if (settings.equals(this.mBatchedScanSettings)) {
                return DEBUG_PARSE;
            }
        } else if (this.mBatchedScanSettings == null) {
            return DEBUG_PARSE;
        }
        this.mBatchedScanSettings = settings;
        if (responsibleWorkSource == null) {
            responsibleWorkSource = new WorkSource(responsibleUid);
        }
        this.mBatchedScanWorkSource = responsibleWorkSource;
        this.mBatchedScanCsph = csph;
        return true;
    }

    private void stopBatchedScan() {
        this.mAlarmManager.cancel(this.mBatchedScanIntervalIntent);
        retrieveBatchedScanData();
        this.mWifiNative.setBatchedScanSettings(null);
        noteBatchedScanStop();
    }

    private void setNextBatchedAlarm(int scansExpected) {
        if (this.mBatchedScanSettings != null && scansExpected >= 1) {
            this.mBatchedScanMinPollTime = System.currentTimeMillis() + ((long) (this.mBatchedScanSettings.scanIntervalSec * 1000));
            if (this.mBatchedScanSettings.maxScansPerBatch < scansExpected) {
                scansExpected = this.mBatchedScanSettings.maxScansPerBatch;
            }
            int secToFull = this.mBatchedScanSettings.scanIntervalSec;
            int secToFull2 = secToFull * scansExpected;
            int debugPeriod = SystemProperties.getInt("wifi.batchedScan.pollPeriod", 0);
            if (debugPeriod > 0) {
                secToFull2 = debugPeriod;
            }
            this.mAlarmManager.setExact(0, System.currentTimeMillis() + ((long) ((secToFull2 - (this.mBatchedScanSettings.scanIntervalSec / 2)) * 1000)), this.mBatchedScanIntervalIntent);
        }
    }

    private void retrieveBatchedScanData() {
        String rawData = this.mWifiNative.getBatchedScanResults();
        this.mBatchedScanMinPollTime = 0L;
        if (rawData == null || rawData.equalsIgnoreCase("OK")) {
            loge("Unexpected BatchedScanResults :" + rawData);
            return;
        }
        int scanCount = 0;
        String[] splitData = rawData.split("\n");
        int n = 0;
        if (splitData[0].startsWith("scancount=")) {
            int n2 = 0 + 1;
            try {
                scanCount = Integer.parseInt(splitData[0].substring("scancount=".length()));
                n = n2;
            } catch (NumberFormatException e) {
                loge("scancount parseInt Exception from " + splitData[n2]);
                n = n2;
            }
        } else {
            log("scancount not found");
        }
        if (scanCount == 0) {
            loge("scanCount==0 - aborting");
            return;
        }
        Intent intent = new Intent("android.net.wifi.BATCHED_RESULTS");
        intent.addFlags(67108864);
        synchronized (this.mBatchedScanResults) {
            this.mBatchedScanResults.clear();
            BatchedScanResult batchedScanResult = new BatchedScanResult();
            String bssid = null;
            WifiSsid wifiSsid = null;
            int level = 0;
            int freq = 0;
            long tsf = 0;
            int distSd = -1;
            int dist = -1;
            long now = SystemClock.elapsedRealtime();
            int bssidStrLen = BSSID_STR.length();
            while (true) {
                if (n < splitData.length) {
                    if (splitData[n].equals("----")) {
                        if (n + 1 != splitData.length) {
                            loge("didn't consume " + (splitData.length - n));
                        }
                        if (this.mBatchedScanResults.size() > 0) {
                            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
                        }
                        logd("retrieveBatchedScanResults X");
                        return;
                    }
                    if (splitData[n].equals(END_STR) || splitData[n].equals(DELIMITER_STR)) {
                        if (bssid != null) {
                            batchedScanResult.scanResults.add(new ScanResult(wifiSsid, bssid, "", level, freq, tsf, dist, distSd));
                            wifiSsid = null;
                            bssid = null;
                            level = 0;
                            freq = 0;
                            tsf = 0;
                            distSd = -1;
                            dist = -1;
                        }
                        if (splitData[n].equals(END_STR)) {
                            if (batchedScanResult.scanResults.size() != 0) {
                                this.mBatchedScanResults.add(batchedScanResult);
                                batchedScanResult = new BatchedScanResult();
                            } else {
                                logd("Found empty batch");
                            }
                        }
                    } else if (splitData[n].equals("trunc")) {
                        batchedScanResult.truncated = true;
                    } else if (splitData[n].startsWith(BSSID_STR)) {
                        bssid = new String(splitData[n].getBytes(), bssidStrLen, splitData[n].length() - bssidStrLen);
                    } else if (splitData[n].startsWith(FREQ_STR)) {
                        try {
                            freq = Integer.parseInt(splitData[n].substring(FREQ_STR.length()));
                        } catch (NumberFormatException e2) {
                            loge("Invalid freqency: " + splitData[n]);
                            freq = 0;
                        }
                    } else if (splitData[n].startsWith("age=")) {
                        try {
                            long tsf2 = now - Long.parseLong(splitData[n].substring("age=".length()));
                            tsf = tsf2 * 1000;
                        } catch (NumberFormatException e3) {
                            loge("Invalid timestamp: " + splitData[n]);
                            tsf = 0;
                        }
                    } else if (splitData[n].startsWith(SSID_STR)) {
                        wifiSsid = WifiSsid.createFromAsciiEncoded(splitData[n].substring(SSID_STR.length()));
                    } else if (splitData[n].startsWith(LEVEL_STR)) {
                        try {
                            level = Integer.parseInt(splitData[n].substring(LEVEL_STR.length()));
                            if (level > 0) {
                                level -= 256;
                            }
                        } catch (NumberFormatException e4) {
                            loge("Invalid level: " + splitData[n]);
                            level = 0;
                        }
                    } else if (splitData[n].startsWith("dist=")) {
                        try {
                            dist = Integer.parseInt(splitData[n].substring("dist=".length()));
                        } catch (NumberFormatException e5) {
                            loge("Invalid distance: " + splitData[n]);
                            dist = -1;
                        }
                    } else if (splitData[n].startsWith("distSd=")) {
                        try {
                            distSd = Integer.parseInt(splitData[n].substring("distSd=".length()));
                        } catch (NumberFormatException e6) {
                            loge("Invalid distanceSd: " + splitData[n]);
                            distSd = -1;
                        }
                    } else {
                        loge("Unable to parse batched scan result line: " + splitData[n]);
                    }
                    n++;
                } else {
                    String rawData2 = this.mWifiNative.getBatchedScanResults();
                    if (rawData2 == null) {
                        loge("Unexpected null BatchedScanResults");
                        return;
                    }
                    splitData = rawData2.split("\n");
                    if (splitData.length == 0 || splitData[0].equals("ok")) {
                        break;
                    } else {
                        n = 0;
                    }
                }
            }
            loge("batch scan results just ended!");
            if (this.mBatchedScanResults.size() > 0) {
                this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
            }
        }
    }

    public long getDisconnectedTimeMilli() {
        if (getCurrentState() != this.mDisconnectedState || this.mDisconnectedTimeStamp == 0) {
            return 0L;
        }
        long now_ms = System.currentTimeMillis();
        return now_ms - this.mDisconnectedTimeStamp;
    }

    private boolean checkOrDeferScanAllowed(Message msg) {
        long now = System.currentTimeMillis();
        if (this.lastConnectAttempt == 0 || now - this.lastConnectAttempt >= 10000) {
            return true;
        }
        Message dmsg = Message.obtain(msg);
        sendMessageDelayed(dmsg, 11000 - (now - this.lastConnectAttempt));
        return DEBUG_PARSE;
    }

    String reportOnTime() {
        long now = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        int on = this.mOnTime - this.mOnTimeLastReport;
        this.mOnTimeLastReport = this.mOnTime;
        int tx = this.mTxTime - this.mTxTimeLastReport;
        this.mTxTimeLastReport = this.mTxTime;
        int rx = this.mRxTime - this.mRxTimeLastReport;
        this.mRxTimeLastReport = this.mRxTime;
        int period = (int) (now - this.lastOntimeReportTimeStamp);
        this.lastOntimeReportTimeStamp = now;
        sb.append(String.format("[on:%d tx:%d rx:%d period:%d]", Integer.valueOf(on), Integer.valueOf(tx), Integer.valueOf(rx), Integer.valueOf(period)));
        int on2 = this.mOnTime - this.mOnTimeScreenStateChange;
        int period2 = (int) (now - this.lastScreenStateChangeTimeStamp);
        sb.append(String.format(" from screen [on:%d period:%d]", Integer.valueOf(on2), Integer.valueOf(period2)));
        return sb.toString();
    }

    WifiLinkLayerStats getWifiLinkLayerStats(boolean dbg) {
        WifiLinkLayerStats stats = null;
        if (this.mWifiLinkLayerStatsSupported > 0) {
            WifiNative wifiNative = this.mWifiNative;
            stats = WifiNative.getWifiLinkLayerStats("wlan0");
            if ("wlan0" != 0 && stats == null && this.mWifiLinkLayerStatsSupported > 0) {
                this.mWifiLinkLayerStatsSupported--;
            } else if (stats != null) {
                this.lastLinkLayerStatsUpdate = System.currentTimeMillis();
                this.mOnTime = stats.on_time;
                this.mTxTime = stats.tx_time;
                this.mRxTime = stats.rx_time;
                this.mRunningBeaconCount = stats.beacon_rx;
                if (dbg) {
                    loge(stats.toString());
                }
            }
        }
        if (stats == null || this.mWifiLinkLayerStatsSupported <= 0) {
            long mTxPkts = TrafficStats.getTxPackets(this.mInterfaceName);
            long mRxPkts = TrafficStats.getRxPackets(this.mInterfaceName);
            this.mWifiInfo.updatePacketRates(mTxPkts, mRxPkts);
        } else {
            this.mWifiInfo.updatePacketRates(stats);
        }
        return stats;
    }

    void startRadioScanStats() {
        WifiLinkLayerStats stats = getWifiLinkLayerStats(DEBUG_PARSE);
        if (stats != null) {
            this.mOnTimeStartScan = stats.on_time;
            this.mTxTimeStartScan = stats.tx_time;
            this.mRxTimeStartScan = stats.rx_time;
            this.mOnTime = stats.on_time;
            this.mTxTime = stats.tx_time;
            this.mRxTime = stats.rx_time;
        }
    }

    void closeRadioScanStats() {
        WifiLinkLayerStats stats = getWifiLinkLayerStats(DEBUG_PARSE);
        if (stats != null) {
            this.mOnTimeThisScan = stats.on_time - this.mOnTimeStartScan;
            this.mTxTimeThisScan = stats.tx_time - this.mTxTimeStartScan;
            this.mRxTimeThisScan = stats.rx_time - this.mRxTimeStartScan;
            this.mOnTimeScan += this.mOnTimeThisScan;
            this.mTxTimeScan += this.mTxTimeThisScan;
            this.mRxTimeScan += this.mRxTimeThisScan;
        }
    }

    private void noteScanStart(int callingUid, WorkSource workSource) {
        long now = System.currentTimeMillis();
        this.lastStartScanTimeStamp = now;
        this.lastScanDuration = 0L;
        if (DBG) {
            String ts = String.format("[%,d ms]", Long.valueOf(now));
            if (workSource != null) {
                loge(ts + " noteScanStart" + workSource.toString() + " uid " + Integer.toString(callingUid));
            } else {
                loge(ts + " noteScanstart no scan source uid " + Integer.toString(callingUid));
            }
        }
        startRadioScanStats();
        if (this.mScanWorkSource == null) {
            if ((callingUid != -1 && callingUid != SCAN_ALARM_SOURCE) || workSource != null) {
                if (workSource == null) {
                    workSource = new WorkSource(callingUid);
                }
                this.mScanWorkSource = workSource;
                try {
                    this.mBatteryStats.noteWifiScanStartedFromSource(this.mScanWorkSource);
                } catch (RemoteException e) {
                    log(e.toString());
                }
            }
        }
    }

    private void noteScanEnd() {
        long now = System.currentTimeMillis();
        if (this.lastStartScanTimeStamp != 0) {
            this.lastScanDuration = now - this.lastStartScanTimeStamp;
        }
        this.lastStartScanTimeStamp = 0L;
        if (DBG) {
            String ts = String.format("[%,d ms]", Long.valueOf(now));
            if (this.mScanWorkSource != null) {
                loge(ts + " noteScanEnd " + this.mScanWorkSource.toString() + " onTime=" + this.mOnTimeThisScan);
            } else {
                loge(ts + " noteScanEnd no scan source onTime=" + this.mOnTimeThisScan);
            }
        }
        try {
        } catch (RemoteException e) {
            log(e.toString());
        } finally {
            this.mScanWorkSource = null;
        }
        if (this.mScanWorkSource != null) {
            this.mBatteryStats.noteWifiScanStoppedFromSource(this.mScanWorkSource);
        }
    }

    private void noteBatchedScanStart() {
        if (PDBG) {
            loge("noteBatchedScanstart()");
        }
        if (this.mNotedBatchedScanWorkSource != null && (!this.mNotedBatchedScanWorkSource.equals(this.mBatchedScanWorkSource) || this.mNotedBatchedScanCsph != this.mBatchedScanCsph)) {
            try {
                this.mBatteryStats.noteWifiBatchedScanStoppedFromSource(this.mNotedBatchedScanWorkSource);
            } catch (RemoteException e) {
                log(e.toString());
            } finally {
                this.mNotedBatchedScanWorkSource = null;
                this.mNotedBatchedScanCsph = 0;
            }
        }
        try {
            this.mBatteryStats.noteWifiBatchedScanStartedFromSource(this.mBatchedScanWorkSource, this.mBatchedScanCsph);
            this.mNotedBatchedScanWorkSource = this.mBatchedScanWorkSource;
            this.mNotedBatchedScanCsph = this.mBatchedScanCsph;
        } catch (RemoteException e2) {
            log(e2.toString());
        }
    }

    private void noteBatchedScanStop() {
        if (PDBG) {
            loge("noteBatchedScanstop()");
        }
        try {
        } catch (RemoteException e) {
            log(e.toString());
        } finally {
            this.mNotedBatchedScanWorkSource = null;
            this.mNotedBatchedScanCsph = 0;
        }
        if (this.mNotedBatchedScanWorkSource != null) {
            this.mBatteryStats.noteWifiBatchedScanStoppedFromSource(this.mNotedBatchedScanWorkSource);
        }
    }

    private void handleScanRequest(int type, Message message) {
        ScanSettings settings = null;
        WorkSource workSource = null;
        Bundle bundle = (Bundle) message.obj;
        if (bundle != null) {
            settings = (ScanSettings) bundle.getParcelable(CUSTOMIZED_SCAN_SETTING);
            workSource = (WorkSource) bundle.getParcelable(CUSTOMIZED_SCAN_WORKSOURCE);
        }
        String freqs = null;
        if (settings != null && settings.channelSet != null) {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (WifiChannel channel : settings.channelSet) {
                if (first) {
                    first = DEBUG_PARSE;
                } else {
                    sb.append(',');
                }
                sb.append(channel.freqMHz);
            }
            freqs = sb.toString();
        }
        if (startScanNative(type, freqs)) {
            noteScanStart(message.arg1, workSource);
            if (freqs == null) {
                this.mBufferedScanMsg.clear();
            }
            this.messageHandlingStatus = MESSAGE_HANDLING_STATUS_OK;
            if (workSource != null) {
                this.mSendScanResultsBroadcast = true;
                return;
            }
            return;
        }
        if (!this.mIsScanOngoing) {
            if (this.mBufferedScanMsg.size() > 0) {
                sendMessage(this.mBufferedScanMsg.remove());
            }
            this.messageHandlingStatus = MESSAGE_HANDLING_STATUS_DISCARD;
            return;
        }
        if (!this.mIsFullScanOngoing) {
            if (freqs == null) {
                this.mBufferedScanMsg.clear();
            }
            if (this.mBufferedScanMsg.size() < 10) {
                Message msg = obtainMessage(CMD_START_SCAN, message.arg1, message.arg2, bundle);
                this.mBufferedScanMsg.add(msg);
            } else {
                Bundle bundle2 = new Bundle();
                bundle2.putParcelable(CUSTOMIZED_SCAN_SETTING, null);
                bundle2.putParcelable(CUSTOMIZED_SCAN_WORKSOURCE, workSource);
                Message msg2 = obtainMessage(CMD_START_SCAN, message.arg1, message.arg2, bundle2);
                this.mBufferedScanMsg.clear();
                this.mBufferedScanMsg.add(msg2);
            }
            this.messageHandlingStatus = MESSAGE_HANDLING_STATUS_LOOPED;
            return;
        }
        this.messageHandlingStatus = MESSAGE_HANDLING_STATUS_FAIL;
    }

    private boolean startScanNative(int type, String freqs) {
        boolean z = DEBUG_PARSE;
        if (!this.mWifiNative.scan(type, freqs)) {
            return DEBUG_PARSE;
        }
        this.mIsScanOngoing = true;
        if (freqs == null) {
            z = true;
        }
        this.mIsFullScanOngoing = z;
        this.lastScanFreqs = freqs;
        return true;
    }

    public void setSupplicantRunning(boolean enable) {
        if (enable) {
            sendMessage(CMD_START_SUPPLICANT);
        } else {
            sendMessage(CMD_STOP_SUPPLICANT);
        }
    }

    public synchronized boolean setActiveRoaming(boolean enabled) {
        return this.mWifiState.get() != 3 ? DEBUG_PARSE : this.mWifiNative.setActiveRoamingCommand(enabled);
    }

    public void setWifiLogLev(Context context, Intent intent) {
        int stateMachineExtra = intent.getIntExtra("wifi_statemachine_db_level", 0);
        DBG = stateMachineExtra == 1 ? true : DEBUG_PARSE;
        log("Set stateMachine log enable: " + DBG);
        setDbg(DBG);
        int mrvlHalExtra = intent.getIntExtra("wifi_hal_db_level", 3);
        log("Set mrvl WIFI HAL log level: " + mrvlHalExtra);
        this.mWifiNative.setWifiHalDbg(mrvlHalExtra);
        int new_wpa_stamp = intent.getIntExtra("wpa_db_stamp", 0);
        String supplicantArg = new_wpa_stamp == 1 ? "t" : "";
        int new_wpa_loglevel = intent.getIntExtra("wpa_db_level", 3);
        String db_level_flag = "";
        if (new_wpa_loglevel > 3) {
            db_level_flag = "q";
        } else if (new_wpa_loglevel < 3) {
            db_level_flag = "d";
        }
        for (int i = 0; i < Math.abs(new_wpa_loglevel + ADD_OR_UPDATE_SOURCE); i++) {
            supplicantArg = supplicantArg + db_level_flag;
        }
        if (this.mWifiNative.setSupplicantLogLevel(new_wpa_loglevel, new_wpa_stamp)) {
            log("Set wpa_supplicant log level: " + new_wpa_loglevel + " timeStamp: " + new_wpa_stamp);
        }
        String extraSupplicantArg = intent.getStringExtra("extra_supplicant_arg");
        if (this.mWifiNative.setSupplicantArg(extraSupplicantArg == null ? supplicantArg : supplicantArg + extraSupplicantArg)) {
            StringBuilder sbAppend = new StringBuilder().append("Set supplicant argument: ");
            if (extraSupplicantArg != null) {
                supplicantArg = supplicantArg + extraSupplicantArg;
            }
            log(sbAppend.append(supplicantArg).toString());
        }
        String wifiDrvArg = intent.getStringExtra("wifi_drv_arg");
        if (this.mWifiNative.setWifiDrvArg(wifiDrvArg == null ? "" : wifiDrvArg)) {
            StringBuilder sbAppend2 = new StringBuilder().append("Set WiFi driver argument: ");
            if (wifiDrvArg == null) {
                wifiDrvArg = "NULL";
            }
            log(sbAppend2.append(wifiDrvArg).toString());
        }
    }

    public void setHostApRunning(WifiConfiguration wifiConfig, boolean enable) {
        if (enable) {
            sendMessage(CMD_START_AP, wifiConfig);
        } else {
            sendMessage(CMD_STOP_AP);
        }
    }

    public void setWifiApConfiguration(WifiConfiguration config) {
        this.mWifiApConfigChannel.sendMessage(CMD_SET_AP_CONFIG, config);
    }

    public WifiConfiguration syncGetWifiApConfiguration() {
        Message resultMsg = this.mWifiApConfigChannel.sendMessageSynchronously(CMD_REQUEST_AP_CONFIG);
        WifiConfiguration ret = (WifiConfiguration) resultMsg.obj;
        resultMsg.recycle();
        return ret;
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

    public int syncGetWifiApState() {
        return this.mWifiApState.get();
    }

    public String syncGetWifiApStateByName() {
        switch (this.mWifiApState.get()) {
            case 10:
                return "disabling";
            case 11:
                return "disabled";
            case 12:
                return "enabling";
            case 13:
                return "enabled";
            case 14:
                return "failed";
            default:
                return "[invalid state]";
        }
    }

    public WifiInfo syncRequestConnectionInfo() {
        return this.mWifiInfo;
    }

    public DhcpResults syncGetDhcpResults() {
        DhcpResults dhcpResults;
        synchronized (this.mDhcpResultsLock) {
            dhcpResults = new DhcpResults(this.mDhcpResults);
        }
        return dhcpResults;
    }

    public void setDriverStart(boolean enable) {
        if (enable) {
            sendMessage(CMD_START_DRIVER);
        } else {
            sendMessage(CMD_STOP_DRIVER);
        }
    }

    public void setOperationalMode(int mode) {
        if (DBG) {
            log("setting operational mode to " + String.valueOf(mode));
        }
        sendMessage(CMD_SET_OPERATIONAL_MODE, mode, 0);
    }

    public List<ScanResult> syncGetScanResultsList() {
        List<ScanResult> scanList;
        synchronized (this.mScanResultCache) {
            scanList = new ArrayList<>();
            for (ScanResult result : this.mScanResults) {
                scanList.add(new ScanResult(result));
            }
        }
        return scanList;
    }

    public void disableEphemeralNetwork(String SSID) {
        if (SSID != null) {
            sendMessage(CMD_DISABLE_EPHEMERAL_NETWORK, SSID);
        }
    }

    public List<ScanResult> getScanResultsListNoCopyUnsync() {
        return this.mScanResults;
    }

    public void disconnectCommand() {
        sendMessage(CMD_DISCONNECT);
    }

    public void disconnectCommand(int uid, int reason) {
        sendMessage(CMD_DISCONNECT, uid, reason);
    }

    public void reconnectCommand() {
        sendMessage(CMD_RECONNECT);
    }

    public void reassociateCommand() {
        sendMessage(CMD_REASSOCIATE);
    }

    public void reloadTlsNetworksAndReconnect() {
        sendMessage(CMD_RELOAD_TLS_AND_RECONNECT);
    }

    public int syncAddOrUpdateNetwork(AsyncChannel channel, WifiConfiguration config) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_ADD_OR_UPDATE_NETWORK, config);
        int result = resultMsg.arg1;
        resultMsg.recycle();
        return result;
    }

    public List<WifiConfiguration> syncGetConfiguredNetworks(int uuid, AsyncChannel channel) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_GET_CONFIGURED_NETWORKS, uuid);
        List<WifiConfiguration> result = (List) resultMsg.obj;
        resultMsg.recycle();
        return result;
    }

    public List<WifiConfiguration> syncGetPrivilegedConfiguredNetwork(AsyncChannel channel) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_GET_PRIVILEGED_CONFIGURED_NETWORKS);
        List<WifiConfiguration> result = (List) resultMsg.obj;
        resultMsg.recycle();
        return result;
    }

    public WifiConnectionStatistics syncGetConnectionStatistics(AsyncChannel channel) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_GET_CONNECTION_STATISTICS);
        WifiConnectionStatistics result = (WifiConnectionStatistics) resultMsg.obj;
        resultMsg.recycle();
        return result;
    }

    public int syncGetSupportedFeatures(AsyncChannel channel) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_GET_SUPPORTED_FEATURES);
        int supportedFeatureSet = resultMsg.arg1;
        resultMsg.recycle();
        return supportedFeatureSet;
    }

    public WifiLinkLayerStats syncGetLinkLayerStats(AsyncChannel channel) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_GET_LINK_LAYER_STATS);
        WifiLinkLayerStats result = (WifiLinkLayerStats) resultMsg.obj;
        resultMsg.recycle();
        return result;
    }

    public boolean syncRemoveNetwork(AsyncChannel channel, int networkId) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_REMOVE_NETWORK, networkId);
        boolean result = resultMsg.arg1 != -1 ? true : DEBUG_PARSE;
        resultMsg.recycle();
        return result;
    }

    public boolean syncEnableNetwork(AsyncChannel channel, int netId, boolean disableOthers) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_ENABLE_NETWORK, netId, disableOthers ? 1 : 0);
        boolean result = resultMsg.arg1 != -1;
        resultMsg.recycle();
        return result;
    }

    public boolean syncDisableNetwork(AsyncChannel channel, int netId) {
        Message resultMsg = channel.sendMessageSynchronously(151569, netId);
        boolean result = resultMsg.arg1 != 151570 ? true : DEBUG_PARSE;
        resultMsg.recycle();
        return result;
    }

    public String syncGetWpsNfcConfigurationToken(int netId) {
        return this.mWifiNative.getNfcWpsConfigurationToken(netId);
    }

    void enableBackgroundScan(boolean enable) {
        if (enable) {
            this.mWifiConfigStore.enableAllNetworks();
        }
        this.mWifiNative.enableBackgroundScan(enable);
    }

    public void addToBlacklist(String bssid) {
        sendMessage(CMD_BLACKLIST_NETWORK, bssid);
    }

    public void clearBlacklist() {
        sendMessage(CMD_CLEAR_BLACKLIST);
    }

    public void enableRssiPolling(boolean enabled) {
        sendMessage(CMD_ENABLE_RSSI_POLL, enabled ? 1 : 0, 0);
    }

    public void enableAllNetworks() {
        sendMessage(CMD_ENABLE_ALL_NETWORKS);
    }

    public void startFilteringMulticastV4Packets() {
        this.mFilteringMulticastV4Packets.set(true);
        sendMessage(CMD_START_PACKET_FILTERING, 0, 0);
    }

    public void stopFilteringMulticastV4Packets() {
        this.mFilteringMulticastV4Packets.set(DEBUG_PARSE);
        sendMessage(CMD_STOP_PACKET_FILTERING, 0, 0);
    }

    public void startFilteringMulticastV6Packets() {
        sendMessage(CMD_START_PACKET_FILTERING, 1, 0);
    }

    public void stopFilteringMulticastV6Packets() {
        sendMessage(CMD_STOP_PACKET_FILTERING, 1, 0);
    }

    public void setHighPerfModeEnabled(boolean enable) {
        sendMessage(CMD_SET_HIGH_PERF_MODE, enable ? 1 : 0, 0);
    }

    public void setCountryCode(String countryCode, boolean persist) {
        int countryCodeSequence = this.mCountryCodeSequence.incrementAndGet();
        if (TextUtils.isEmpty(countryCode)) {
            log("Ignoring resetting of country code");
        } else {
            sendMessage(CMD_SET_COUNTRY_CODE, countryCodeSequence, persist ? 1 : 0, countryCode);
        }
    }

    public void setFrequencyBand(int band, boolean persist) {
        if (persist) {
            Settings.Global.putInt(this.mContext.getContentResolver(), "wifi_frequency_band", band);
        }
        sendMessage(CMD_SET_FREQUENCY_BAND, band, 0);
    }

    public void enableTdls(String remoteMacAddress, boolean enable) {
        int enabler = enable ? 1 : 0;
        sendMessage(CMD_ENABLE_TDLS, enabler, 0, remoteMacAddress);
    }

    public int getFrequencyBand() {
        return this.mFrequencyBand.get();
    }

    public String getConfigFile() {
        return this.mWifiConfigStore.getConfigFile();
    }

    public void sendBluetoothAdapterStateChange(int state) {
        sendMessage(CMD_BLUETOOTH_ADAPTER_STATE_CHANGE, state, 0);
    }

    public boolean syncSaveConfig(AsyncChannel channel) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_SAVE_CONFIG);
        boolean result = resultMsg.arg1 != -1 ? true : DEBUG_PARSE;
        resultMsg.recycle();
        return result;
    }

    public void updateBatteryWorkSource(WorkSource newSource) {
        synchronized (this.mRunningWifiUids) {
            if (newSource != null) {
                try {
                    this.mRunningWifiUids.set(newSource);
                } catch (RemoteException e) {
                }
            }
            if (this.mIsRunning) {
                if (this.mReportedRunning) {
                    if (this.mLastRunningWifiUids.diff(this.mRunningWifiUids)) {
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
                this.mReportedRunning = DEBUG_PARSE;
            }
            this.mWakeLock.setWorkSource(newSource);
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        super.dump(fd, pw, args);
        this.mSupplicantStateTracker.dump(fd, pw, args);
        pw.println("mLinkProperties " + this.mLinkProperties);
        pw.println("mWifiInfo " + this.mWifiInfo);
        pw.println("mDhcpResults " + this.mDhcpResults);
        pw.println("mNetworkInfo " + this.mNetworkInfo);
        pw.println("mLastSignalLevel " + this.mLastSignalLevel);
        pw.println("mLastBssid " + this.mLastBssid);
        pw.println("mLastNetworkId " + this.mLastNetworkId);
        pw.println("mOperationalMode " + this.mOperationalMode);
        pw.println("mUserWantsSuspendOpt " + this.mUserWantsSuspendOpt);
        pw.println("mSuspendOptNeedsDisabled " + this.mSuspendOptNeedsDisabled);
        pw.println("Supplicant status " + this.mWifiNative.status(true));
        pw.println("mEnableBackgroundScan " + this.mEnableBackgroundScan);
        pw.println("mLastSetCountryCode " + this.mLastSetCountryCode);
        pw.println("mPersistedCountryCode " + this.mPersistedCountryCode);
        this.mNetworkFactory.dump(fd, pw, args);
        this.mUntrustedNetworkFactory.dump(fd, pw, args);
        pw.println();
        this.mWifiConfigStore.dump(fd, pw, args);
    }

    private void logStateAndMessage(Message message, String state) {
        this.messageHandlingStatus = 0;
        if (mLogMessages) {
            loge(" " + state + " " + getLogRecString(message));
        }
    }

    String printTime() {
        StringBuilder sb = new StringBuilder();
        sb.append(" rt=").append(SystemClock.uptimeMillis());
        sb.append("/").append(SystemClock.elapsedRealtime());
        return sb.toString();
    }

    protected String getLogRecString(Message msg) {
        StringBuilder sb = new StringBuilder();
        if (this.mScreenOn) {
            sb.append("!");
        }
        if (this.messageHandlingStatus != MESSAGE_HANDLING_STATUS_UNKNOWN) {
            sb.append("(").append(this.messageHandlingStatus).append(")");
        }
        sb.append(smToString(msg));
        if (msg.sendingUid > 0 && msg.sendingUid != 1010) {
            sb.append(" uid=" + msg.sendingUid);
        }
        switch (msg.what) {
            case CMD_ADD_OR_UPDATE_NETWORK:
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                if (msg.obj != null) {
                    WifiConfiguration config = (WifiConfiguration) msg.obj;
                    sb.append(" ").append(config.configKey());
                    sb.append(" prio=").append(config.priority);
                    sb.append(" status=").append(config.status);
                    if (config.BSSID != null) {
                        sb.append(" ").append(config.BSSID);
                    }
                    WifiConfiguration curConfig = getCurrentWifiConfiguration();
                    if (curConfig != null) {
                        if (curConfig.configKey().equals(config.configKey())) {
                            sb.append(" is current");
                        } else {
                            sb.append(" current=").append(curConfig.configKey());
                            sb.append(" prio=").append(curConfig.priority);
                            sb.append(" status=").append(curConfig.status);
                        }
                    }
                }
                break;
            case CMD_ENABLE_NETWORK:
            case 151569:
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                String key = this.mWifiConfigStore.getLastSelectedConfiguration();
                if (key != null) {
                    sb.append(" last=").append(key);
                }
                WifiConfiguration config2 = this.mWifiConfigStore.getWifiConfiguration(msg.arg1);
                if (config2 != null && (key == null || !config2.configKey().equals(key))) {
                    sb.append(" target=").append(key);
                }
                break;
            case CMD_GET_CONFIGURED_NETWORKS:
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                sb.append(" num=").append(this.mWifiConfigStore.getConfiguredNetworksSize());
                break;
            case CMD_START_SCAN:
                Long now = Long.valueOf(System.currentTimeMillis());
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                sb.append(" ic=");
                sb.append(Integer.toString(sScanAlarmIntentCount));
                if (msg.obj != null) {
                    Bundle bundle = (Bundle) msg.obj;
                    Long request = Long.valueOf(bundle.getLong(SCAN_REQUEST_TIME, 0L));
                    if (request.longValue() != 0) {
                        sb.append(" proc(ms):").append(now.longValue() - request.longValue());
                    }
                }
                if (this.mIsScanOngoing) {
                    sb.append(" onGoing");
                }
                if (this.mIsFullScanOngoing) {
                    sb.append(" full");
                }
                if (this.lastStartScanTimeStamp != 0) {
                    sb.append(" started:").append(this.lastStartScanTimeStamp);
                    sb.append(",").append(now.longValue() - this.lastStartScanTimeStamp);
                }
                if (this.lastScanDuration != 0) {
                    sb.append(" dur:").append(this.lastScanDuration);
                }
                sb.append(" cnt=").append(this.mDelayedScanCounter);
                sb.append(" rssi=").append(this.mWifiInfo.getRssi());
                sb.append(" f=").append(this.mWifiInfo.getFrequency());
                sb.append(" sc=").append(this.mWifiInfo.score);
                sb.append(" link=").append(this.mWifiInfo.getLinkSpeed());
                sb.append(String.format(" tx=%.1f,", Double.valueOf(this.mWifiInfo.txSuccessRate)));
                sb.append(String.format(" %.1f,", Double.valueOf(this.mWifiInfo.txRetriesRate)));
                sb.append(String.format(" %.1f ", Double.valueOf(this.mWifiInfo.txBadRate)));
                sb.append(String.format(" rx=%.1f", Double.valueOf(this.mWifiInfo.rxSuccessRate)));
                if (this.lastScanFreqs != null) {
                    sb.append(" list=").append(this.lastScanFreqs);
                } else {
                    sb.append(" fiv=").append(this.fullBandConnectedTimeIntervalMilli);
                }
                String report = reportOnTime();
                if (report != null) {
                    sb.append(" ").append(report);
                }
                break;
            case CMD_SET_COUNTRY_CODE:
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                if (msg.obj != null) {
                    sb.append(" ").append((String) msg.obj);
                }
                break;
            case CMD_RSSI_POLL:
            case CMD_UNWANTED_NETWORK:
            case 151572:
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                if (this.mWifiInfo.getSSID() != null && this.mWifiInfo.getSSID() != null) {
                    sb.append(" ").append(this.mWifiInfo.getSSID());
                }
                if (this.mWifiInfo.getBSSID() != null) {
                    sb.append(" ").append(this.mWifiInfo.getBSSID());
                }
                sb.append(" rssi=").append(this.mWifiInfo.getRssi());
                sb.append(" f=").append(this.mWifiInfo.getFrequency());
                sb.append(" sc=").append(this.mWifiInfo.score);
                sb.append(" link=").append(this.mWifiInfo.getLinkSpeed());
                sb.append(String.format(" tx=%.1f,", Double.valueOf(this.mWifiInfo.txSuccessRate)));
                sb.append(String.format(" %.1f,", Double.valueOf(this.mWifiInfo.txRetriesRate)));
                sb.append(String.format(" %.1f ", Double.valueOf(this.mWifiInfo.txBadRate)));
                sb.append(String.format(" rx=%.1f", Double.valueOf(this.mWifiInfo.rxSuccessRate)));
                sb.append(String.format(" bcn=%d", Integer.valueOf(this.mRunningBeaconCount)));
                String report2 = reportOnTime();
                if (report2 != null) {
                    sb.append(" ").append(report2);
                }
                if (this.wifiScoringReport != null) {
                    sb.append(this.wifiScoringReport);
                }
                break;
            case CMD_ROAM_WATCHDOG_TIMER:
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                sb.append(" cur=").append(this.roamWatchdogCount);
                break;
            case CMD_DISCONNECTING_WATCHDOG_TIMER:
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                sb.append(" cur=").append(this.disconnectingWatchdogCount);
                break;
            case CMD_IP_CONFIGURATION_LOST:
                WifiConfiguration c = getCurrentWifiConfiguration();
                int count = c != null ? c.numIpConfigFailures : -1;
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                sb.append(" failures: ");
                sb.append(Integer.toString(count));
                sb.append("/");
                sb.append(Integer.toString(this.mWifiConfigStore.getMaxDhcpRetries()));
                if (this.mWifiInfo.getBSSID() != null) {
                    sb.append(" ").append(this.mWifiInfo.getBSSID());
                }
                if (c != null) {
                    if (c.scanResultCache != null) {
                        for (ScanResult r : c.scanResultCache.values()) {
                            if (r.BSSID.equals(this.mWifiInfo.getBSSID())) {
                                sb.append(" ipfail=").append(r.numIpConfigFailures);
                                sb.append(",st=").append(r.autoJoinStatus);
                            }
                        }
                    }
                    sb.append(" -> ajst=").append(c.autoJoinStatus);
                    sb.append(" ").append(c.disableReason);
                    sb.append(" txpkts=").append(this.mWifiInfo.txSuccess);
                    sb.append(",").append(this.mWifiInfo.txBad);
                    sb.append(",").append(this.mWifiInfo.txRetries);
                }
                sb.append(printTime());
                sb.append(String.format(" bcn=%d", Integer.valueOf(this.mRunningBeaconCount)));
                break;
            case CMD_UPDATE_LINKPROPERTIES:
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                if (this.mLinkProperties != null) {
                    if (this.mLinkProperties.hasIPv4Address()) {
                        sb.append(" v4");
                    }
                    if (this.mLinkProperties.hasGlobalIPv6Address()) {
                        sb.append(" v6");
                    }
                    if (this.mLinkProperties.hasIPv4DefaultRoute()) {
                        sb.append(" v4r");
                    }
                    if (this.mLinkProperties.hasIPv6DefaultRoute()) {
                        sb.append(" v6r");
                    }
                    if (this.mLinkProperties.hasIPv4DnsServer()) {
                        sb.append(" v4dns");
                    }
                    if (this.mLinkProperties.hasIPv6DnsServer()) {
                        sb.append(" v6dns");
                    }
                }
                break;
            case CMD_TARGET_BSSID:
            case CMD_ASSOCIATED_BSSID:
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                if (msg.obj != null) {
                    sb.append(" BSSID=").append((String) msg.obj);
                }
                if (this.mTargetRoamBSSID != null) {
                    sb.append(" Target=").append(this.mTargetRoamBSSID);
                }
                sb.append(" roam=").append(Integer.toString(this.mAutoRoaming));
                sb.append(printTime());
                break;
            case CMD_AUTO_CONNECT:
            case 151553:
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                WifiConfiguration config3 = (WifiConfiguration) msg.obj;
                if (config3 != null) {
                    sb.append(" ").append(config3.configKey());
                    if (config3.visibility != null) {
                        sb.append(" ").append(config3.visibility.toString());
                    }
                }
                if (this.mTargetRoamBSSID != null) {
                    sb.append(" ").append(this.mTargetRoamBSSID);
                }
                sb.append(" roam=").append(Integer.toString(this.mAutoRoaming));
                sb.append(printTime());
                WifiConfiguration config4 = getCurrentWifiConfiguration();
                if (config4 != null) {
                    sb.append(config4.configKey());
                    if (config4.visibility != null) {
                        sb.append(" ").append(config4.visibility.toString());
                    }
                }
                break;
            case CMD_AUTO_ROAM:
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                ScanResult result = (ScanResult) msg.obj;
                if (result != null) {
                    Long now2 = Long.valueOf(System.currentTimeMillis());
                    sb.append(" bssid=").append(result.BSSID);
                    sb.append(" rssi=").append(result.level);
                    sb.append(" freq=").append(result.frequency);
                    if (result.seen > 0 && result.seen < now2.longValue()) {
                        sb.append(" seen=").append(now2.longValue() - result.seen);
                    } else {
                        sb.append(" !seen=").append(result.seen);
                    }
                }
                if (this.mTargetRoamBSSID != null) {
                    sb.append(" ").append(this.mTargetRoamBSSID);
                }
                sb.append(" roam=").append(Integer.toString(this.mAutoRoaming));
                sb.append(" fail count=").append(Integer.toString(this.mRoamFailCount));
                sb.append(printTime());
                break;
            case CMD_AUTO_SAVE_NETWORK:
            case 151559:
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                if (this.lastSavedConfigurationAttempt != null) {
                    sb.append(" ").append(this.lastSavedConfigurationAttempt.configKey());
                    sb.append(" nid=").append(this.lastSavedConfigurationAttempt.networkId);
                    if (this.lastSavedConfigurationAttempt.hiddenSSID) {
                        sb.append(" hidden");
                    }
                    if (this.lastSavedConfigurationAttempt.preSharedKey != null && !this.lastSavedConfigurationAttempt.preSharedKey.equals("*")) {
                        sb.append(" hasPSK");
                    }
                    if (this.lastSavedConfigurationAttempt.ephemeral) {
                        sb.append(" ephemeral");
                    }
                    if (this.lastSavedConfigurationAttempt.selfAdded) {
                        sb.append(" selfAdded");
                    }
                    sb.append(" cuid=").append(this.lastSavedConfigurationAttempt.creatorUid);
                    sb.append(" suid=").append(this.lastSavedConfigurationAttempt.lastUpdateUid);
                }
                break;
            case WifiP2pServiceImpl.P2P_CONNECTION_CHANGED:
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                if (msg.obj != null) {
                    NetworkInfo info = (NetworkInfo) msg.obj;
                    NetworkInfo.State state = info.getState();
                    NetworkInfo.DetailedState detailedState = info.getDetailedState();
                    if (state != null) {
                        sb.append(" st=").append(state);
                    }
                    if (detailedState != null) {
                        sb.append("/").append(detailedState);
                    }
                }
                break;
            case WifiMonitor.NETWORK_CONNECTION_EVENT:
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                sb.append(" ").append(this.mLastBssid);
                sb.append(" nid=").append(this.mLastNetworkId);
                WifiConfiguration config5 = getCurrentWifiConfiguration();
                if (config5 != null) {
                    sb.append(" ").append(config5.configKey());
                }
                sb.append(printTime());
                String key2 = this.mWifiConfigStore.getLastSelectedConfiguration();
                if (key2 != null) {
                    sb.append(" last=").append(key2);
                }
                break;
            case WifiMonitor.NETWORK_DISCONNECTION_EVENT:
                if (msg.obj != null) {
                    sb.append(" ").append((String) msg.obj);
                }
                sb.append(" nid=").append(msg.arg1);
                sb.append(" reason=").append(msg.arg2);
                if (this.mLastBssid != null) {
                    sb.append(" lastbssid=").append(this.mLastBssid);
                }
                if (this.mWifiInfo.getFrequency() != -1) {
                    sb.append(" freq=").append(this.mWifiInfo.getFrequency());
                    sb.append(" rssi=").append(this.mWifiInfo.getRssi());
                }
                if (this.linkDebouncing) {
                    sb.append(" debounce");
                }
                sb.append(printTime());
                break;
            case WifiMonitor.SCAN_RESULTS_EVENT:
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                if (this.mScanResults != null) {
                    sb.append(" found=");
                    sb.append(this.mScanResults.size());
                }
                sb.append(" known=").append(this.mNumScanResultsKnown);
                sb.append(" got=").append(this.mNumScanResultsReturned);
                if (this.lastScanDuration != 0) {
                    sb.append(" dur:").append(this.lastScanDuration);
                }
                if (this.mOnTime != 0) {
                    sb.append(" on:").append(this.mOnTimeThisScan).append(",").append(this.mOnTimeScan);
                    sb.append(",").append(this.mOnTime);
                }
                if (this.mTxTime != 0) {
                    sb.append(" tx:").append(this.mTxTimeThisScan).append(",").append(this.mTxTimeScan);
                    sb.append(",").append(this.mTxTime);
                }
                if (this.mRxTime != 0) {
                    sb.append(" rx:").append(this.mRxTimeThisScan).append(",").append(this.mRxTimeScan);
                    sb.append(",").append(this.mRxTime);
                }
                sb.append(String.format(" bcn=%d", Integer.valueOf(this.mRunningBeaconCount)));
                sb.append(String.format(" con=%d", Integer.valueOf(this.mConnectionRequests)));
                String key3 = this.mWifiConfigStore.getLastSelectedConfiguration();
                if (key3 != null) {
                    sb.append(" last=").append(key3);
                }
                break;
            case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT:
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                sb.append(printTime());
                StateChangeResult stateChangeResult = (StateChangeResult) msg.obj;
                if (stateChangeResult != null) {
                    sb.append(stateChangeResult.toString());
                }
                break;
            case WifiMonitor.SSID_TEMP_DISABLED:
            case WifiMonitor.SSID_REENABLED:
                sb.append(" nid=").append(msg.arg1);
                if (msg.obj != null) {
                    sb.append(" ").append((String) msg.obj);
                }
                WifiConfiguration config6 = getCurrentWifiConfiguration();
                if (config6 != null) {
                    sb.append(" cur=").append(config6.configKey());
                    sb.append(" ajst=").append(config6.autoJoinStatus);
                    if (config6.selfAdded) {
                        sb.append(" selfAdded");
                    }
                    if (config6.status != 0) {
                        sb.append(" st=").append(config6.status);
                        sb.append(" rs=").append(config6.disableReason);
                    }
                    if (config6.lastConnected != 0) {
                        sb.append(" lastconn=").append(Long.valueOf(System.currentTimeMillis()).longValue() - config6.lastConnected).append("(ms)");
                    }
                    if (this.mLastBssid != null) {
                        sb.append(" lastbssid=").append(this.mLastBssid);
                    }
                    if (this.mWifiInfo.getFrequency() != -1) {
                        sb.append(" freq=").append(this.mWifiInfo.getFrequency());
                        sb.append(" rssi=").append(this.mWifiInfo.getRssi());
                        sb.append(" bssid=").append(this.mWifiInfo.getBSSID());
                    }
                }
                sb.append(printTime());
                break;
            case WifiMonitor.ASSOCIATION_REJECTION_EVENT:
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                String bssid = (String) msg.obj;
                if (bssid != null && bssid.length() > 0) {
                    sb.append(" ");
                    sb.append(bssid);
                }
                sb.append(" blacklist=" + Boolean.toString(this.didBlackListBSSID));
                sb.append(printTime());
                break;
            case 151556:
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                if (this.lastForgetConfigurationAttempt != null) {
                    sb.append(" ").append(this.lastForgetConfigurationAttempt.configKey());
                    sb.append(" nid=").append(this.lastForgetConfigurationAttempt.networkId);
                    if (this.lastForgetConfigurationAttempt.hiddenSSID) {
                        sb.append(" hidden");
                    }
                    if (this.lastForgetConfigurationAttempt.preSharedKey != null) {
                        sb.append(" hasPSK");
                    }
                    if (this.lastForgetConfigurationAttempt.ephemeral) {
                        sb.append(" ephemeral");
                    }
                    if (this.lastForgetConfigurationAttempt.selfAdded) {
                        sb.append(" selfAdded");
                    }
                    sb.append(" cuid=").append(this.lastForgetConfigurationAttempt.creatorUid);
                    sb.append(" suid=").append(this.lastForgetConfigurationAttempt.lastUpdateUid);
                    sb.append(" ajst=").append(this.lastForgetConfigurationAttempt.autoJoinStatus);
                }
                break;
            case 196612:
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                sb.append(" txpkts=").append(this.mWifiInfo.txSuccess);
                sb.append(",").append(this.mWifiInfo.txBad);
                sb.append(",").append(this.mWifiInfo.txRetries);
                break;
            case 196613:
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                if (msg.arg1 == 1) {
                    sb.append(" OK ");
                } else if (msg.arg1 == 2) {
                    sb.append(" FAIL ");
                }
                if (this.mLinkProperties != null) {
                    if (this.mLinkProperties.hasIPv4Address()) {
                        sb.append(" v4");
                    }
                    if (this.mLinkProperties.hasGlobalIPv6Address()) {
                        sb.append(" v6");
                    }
                    if (this.mLinkProperties.hasIPv4DefaultRoute()) {
                        sb.append(" v4r");
                    }
                    if (this.mLinkProperties.hasIPv6DefaultRoute()) {
                        sb.append(" v6r");
                    }
                    if (this.mLinkProperties.hasIPv4DnsServer()) {
                        sb.append(" v4dns");
                    }
                    if (this.mLinkProperties.hasIPv6DnsServer()) {
                        sb.append(" v6dns");
                    }
                }
                break;
            default:
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                break;
        }
        return sb.toString();
    }

    private void handleScreenStateChanged(boolean screenOn, boolean startBackgroundScanIfNeeded) {
        this.mScreenOn = screenOn;
        if (PDBG) {
            loge(" handleScreenStateChanged Enter: screenOn=" + screenOn + " mUserWantsSuspendOpt=" + this.mUserWantsSuspendOpt + " state " + getCurrentState().getName() + " suppState:" + this.mSupplicantStateTracker.getSupplicantStateName());
        }
        enableRssiPolling(screenOn);
        if (screenOn) {
            enableAllNetworks();
        }
        if (this.mUserWantsSuspendOpt.get()) {
            if (screenOn) {
                sendMessage(CMD_SET_SUSPEND_OPT_ENABLED, 0, 0);
            } else {
                this.mSuspendWakeLock.acquire(2000L);
                sendMessage(CMD_SET_SUSPEND_OPT_ENABLED, 1, 0);
            }
        }
        this.mScreenBroadcastReceived.set(true);
        getWifiLinkLayerStats(DEBUG_PARSE);
        this.mOnTimeScreenStateChange = this.mOnTime;
        this.lastScreenStateChangeTimeStamp = this.lastLinkLayerStatsUpdate;
        this.mEnableBackgroundScan = DEBUG_PARSE;
        cancelDelayedScan();
        if (screenOn) {
            setScanAlarm(DEBUG_PARSE);
            clearBlacklist();
            this.fullBandConnectedTimeIntervalMilli = this.mWifiConfigStore.associatedPartialScanPeriodMilli;
            if (getCurrentState() == this.mConnectedState && this.mWifiConfigStore.enableAutoJoinScanWhenAssociated) {
                startDelayedScan(500, null, null);
            } else if (getCurrentState() == this.mDisconnectedState) {
                startDelayedScan(200, null, null);
            }
        } else if (startBackgroundScanIfNeeded) {
            if (!this.mBackgroundScanSupported) {
                setScanAlarm(true);
            } else {
                this.mEnableBackgroundScan = true;
            }
        }
        if (DBG) {
            logd("backgroundScan enabled=" + this.mEnableBackgroundScan + " startBackgroundScanIfNeeded:" + startBackgroundScanIfNeeded);
        }
        if (startBackgroundScanIfNeeded) {
            enableBackgroundScan(this.mEnableBackgroundScan);
        }
        if (DBG) {
            log("handleScreenStateChanged Exit: " + screenOn);
        }
    }

    private void checkAndSetConnectivityInstance() {
        if (this.mCm == null) {
            this.mCm = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        }
    }

    private boolean startTethering(ArrayList<String> available) {
        checkAndSetConnectivityInstance();
        String[] wifiRegexs = this.mCm.getTetherableWifiRegexs();
        for (String intf : available) {
            for (String regex : wifiRegexs) {
                if (intf.matches(regex)) {
                    try {
                        InterfaceConfiguration ifcg = this.mNwService.getInterfaceConfig(intf);
                        if (ifcg != null) {
                            ifcg.setLinkAddress(new LinkAddress(NetworkUtils.numericToInetAddress("192.168.43.1"), 24));
                            ifcg.setInterfaceUp();
                            this.mNwService.setInterfaceConfig(intf, ifcg);
                        }
                        if (this.mCm.tether(intf) != 0) {
                            loge("Error tethering on " + intf);
                            return DEBUG_PARSE;
                        }
                        this.mTetherInterfaceName = intf;
                        return true;
                    } catch (Exception e) {
                        loge("Error configuring interface " + intf + ", :" + e);
                        return DEBUG_PARSE;
                    }
                }
            }
        }
        return DEBUG_PARSE;
    }

    private void stopTethering() {
        checkAndSetConnectivityInstance();
        try {
            InterfaceConfiguration ifcg = this.mNwService.getInterfaceConfig(this.mTetherInterfaceName);
            if (ifcg != null) {
                ifcg.setLinkAddress(new LinkAddress(NetworkUtils.numericToInetAddress("0.0.0.0"), 0));
                this.mNwService.setInterfaceConfig(this.mTetherInterfaceName, ifcg);
            }
        } catch (Exception e) {
            loge("Error resetting interface " + this.mTetherInterfaceName + ", :" + e);
        }
        if (this.mCm.untether(this.mTetherInterfaceName) != 0) {
            loge("Untether initiate failed!");
        }
    }

    private boolean isWifiTethered(ArrayList<String> active) {
        checkAndSetConnectivityInstance();
        String[] wifiRegexs = this.mCm.getTetherableWifiRegexs();
        for (String intf : active) {
            for (String regex : wifiRegexs) {
                if (intf.matches(regex)) {
                    return true;
                }
            }
        }
        return DEBUG_PARSE;
    }

    private void setCountryCode() {
        String countryCode = Settings.Global.getString(this.mContext.getContentResolver(), "wifi_country_code");
        if (countryCode != null && !countryCode.isEmpty()) {
            setCountryCode(countryCode, DEBUG_PARSE);
        }
    }

    private void setFrequencyBand() {
        int band = Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_frequency_band", 0);
        setFrequencyBand(band, DEBUG_PARSE);
    }

    private void setSuspendOptimizationsNative(int reason, boolean enabled) {
        if (DBG) {
            log("setSuspendOptimizationsNative: " + reason + " " + enabled + " -want " + this.mUserWantsSuspendOpt.get() + " stack:" + Thread.currentThread().getStackTrace()[2].getMethodName() + " - " + Thread.currentThread().getStackTrace()[3].getMethodName() + " - " + Thread.currentThread().getStackTrace()[4].getMethodName() + " - " + Thread.currentThread().getStackTrace()[5].getMethodName());
        }
        if (enabled) {
            this.mSuspendOptNeedsDisabled &= reason ^ (-1);
            if (this.mSuspendOptNeedsDisabled == 0 && this.mUserWantsSuspendOpt.get()) {
                if (DBG) {
                    log("setSuspendOptimizationsNative do it " + reason + " " + enabled + " stack:" + Thread.currentThread().getStackTrace()[2].getMethodName() + " - " + Thread.currentThread().getStackTrace()[3].getMethodName() + " - " + Thread.currentThread().getStackTrace()[4].getMethodName() + " - " + Thread.currentThread().getStackTrace()[5].getMethodName());
                }
                this.mWifiNative.setSuspendOptimizations(true);
                return;
            }
            return;
        }
        this.mSuspendOptNeedsDisabled |= reason;
        this.mWifiNative.setSuspendOptimizations(DEBUG_PARSE);
    }

    private void setSuspendOptimizations(int reason, boolean enabled) {
        if (DBG) {
            log("setSuspendOptimizations: " + reason + " " + enabled);
        }
        if (enabled) {
            this.mSuspendOptNeedsDisabled &= reason ^ (-1);
        } else {
            this.mSuspendOptNeedsDisabled |= reason;
        }
        if (DBG) {
            log("mSuspendOptNeedsDisabled " + this.mSuspendOptNeedsDisabled);
        }
    }

    private void setWifiState(int wifiState) {
        int previousWifiState = this.mWifiState.get();
        try {
        } catch (RemoteException e) {
            loge("Failed to note battery stats in wifi");
        }
        if (wifiState == 3) {
            this.mBatteryStats.noteWifiOn();
        } else {
            if (wifiState == 1) {
                this.mBatteryStats.noteWifiOff();
            }
            this.mWifiState.set(wifiState);
            if (DBG) {
                log("setWifiState: " + syncGetWifiStateByName());
            }
            Intent intent = new Intent("android.net.wifi.WIFI_STATE_CHANGED");
            intent.addFlags(67108864);
            intent.putExtra("wifi_state", wifiState);
            intent.putExtra("previous_wifi_state", previousWifiState);
            this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }
        this.mWifiState.set(wifiState);
        if (DBG) {
        }
        Intent intent2 = new Intent("android.net.wifi.WIFI_STATE_CHANGED");
        intent2.addFlags(67108864);
        intent2.putExtra("wifi_state", wifiState);
        intent2.putExtra("previous_wifi_state", previousWifiState);
        this.mContext.sendStickyBroadcastAsUser(intent2, UserHandle.ALL);
    }

    private void setWifiApState(int wifiApState) {
        int previousWifiApState = this.mWifiApState.get();
        try {
        } catch (RemoteException e) {
            loge("Failed to note battery stats in wifi");
        }
        if (wifiApState == 13) {
            this.mBatteryStats.noteWifiOn();
        } else {
            if (wifiApState == 11) {
                this.mBatteryStats.noteWifiOff();
            }
            this.mWifiApState.set(wifiApState);
            if (DBG) {
                log("setWifiApState: " + syncGetWifiApStateByName());
            }
            Intent intent = new Intent("android.net.wifi.WIFI_AP_STATE_CHANGED");
            intent.addFlags(67108864);
            intent.putExtra("wifi_state", wifiApState);
            intent.putExtra("previous_wifi_state", previousWifiApState);
            this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }
        this.mWifiApState.set(wifiApState);
        if (DBG) {
        }
        Intent intent2 = new Intent("android.net.wifi.WIFI_AP_STATE_CHANGED");
        intent2.addFlags(67108864);
        intent2.putExtra("wifi_state", wifiApState);
        intent2.putExtra("previous_wifi_state", previousWifiApState);
        this.mContext.sendStickyBroadcastAsUser(intent2, UserHandle.ALL);
    }

    private void setScanResults() {
        this.mNumScanResultsKnown = 0;
        this.mNumScanResultsReturned = 0;
        String bssid = "";
        int level = 0;
        int freq = 0;
        long tsf = 0;
        String flags = "";
        WifiSsid wifiSsid = null;
        StringBuffer scanResultsBuf = new StringBuffer();
        int sid = 0;
        do {
            String tmpResults = this.mWifiNative.scanResults(sid);
            if (TextUtils.isEmpty(tmpResults)) {
                break;
            }
            scanResultsBuf.append(tmpResults);
            scanResultsBuf.append("\n");
            String[] lines = tmpResults.split("\n");
            sid = -1;
            int i = lines.length - 1;
            while (true) {
                if (i < 0 || lines[i].startsWith(END_STR)) {
                    break;
                }
                if (lines[i].startsWith(ID_STR)) {
                    try {
                        sid = Integer.parseInt(lines[i].substring(ID_STR.length())) + 1;
                        break;
                    } catch (NumberFormatException e) {
                    }
                } else {
                    i--;
                }
            }
        } while (sid != -1);
        String scanResults = scanResultsBuf.toString();
        if (TextUtils.isEmpty(scanResults)) {
            this.emptyScanResultCount++;
            if (this.emptyScanResultCount > 10) {
                this.mScanResults = new ArrayList();
                return;
            }
            return;
        }
        this.emptyScanResultCount = 0;
        synchronized (this.mScanResultCache) {
            this.mScanResults = new ArrayList();
            String[] lines2 = scanResults.split("\n");
            int bssidStrLen = BSSID_STR.length();
            int flagLen = FLAGS_STR.length();
            int band = getFrequencyBand();
            for (String line : lines2) {
                if (line.startsWith(BSSID_STR)) {
                    String bssid2 = new String(line.getBytes(), bssidStrLen, line.length() - bssidStrLen);
                    bssid = bssid2;
                } else if (line.startsWith(FREQ_STR)) {
                    try {
                        freq = Integer.parseInt(line.substring(FREQ_STR.length()));
                        if (band == 1) {
                            if (!ScanResult.is5GHz(freq)) {
                                bssid = null;
                            }
                        } else if (band == 2 && !ScanResult.is24GHz(freq)) {
                            bssid = null;
                        }
                    } catch (NumberFormatException e2) {
                        freq = 0;
                    }
                } else if (line.startsWith(LEVEL_STR)) {
                    try {
                        level = Integer.parseInt(line.substring(LEVEL_STR.length()));
                        if (level > 0) {
                            level -= 256;
                        }
                    } catch (NumberFormatException e3) {
                        level = 0;
                    }
                } else if (line.startsWith(TSF_STR)) {
                    try {
                        tsf = Long.parseLong(line.substring(TSF_STR.length()));
                    } catch (NumberFormatException e4) {
                        tsf = 0;
                    }
                } else if (line.startsWith(FLAGS_STR)) {
                    String flags2 = new String(line.getBytes(), flagLen, line.length() - flagLen);
                    flags = flags2;
                } else if (line.startsWith(SSID_STR)) {
                    wifiSsid = WifiSsid.createFromAsciiEncoded(line.substring(SSID_STR.length()));
                } else if (line.startsWith(DELIMITER_STR) || line.startsWith(END_STR)) {
                    Matcher match = null;
                    if (bssid != null) {
                        match = mNotZero.matcher(bssid);
                    }
                    if (match != null && !bssid.isEmpty() && match.find()) {
                        String ssid = wifiSsid != null ? wifiSsid.toString() : "<unknown ssid>";
                        String key = bssid + ssid;
                        ScanResult scanResult = this.mScanResultCache.get(key);
                        if (scanResult != null) {
                            scanResult.level = level;
                            scanResult.wifiSsid = wifiSsid;
                            scanResult.SSID = wifiSsid != null ? wifiSsid.toString() : "<unknown ssid>";
                            scanResult.capabilities = flags;
                            scanResult.frequency = freq;
                            scanResult.timestamp = tsf;
                            scanResult.seen = System.currentTimeMillis();
                        } else {
                            scanResult = new ScanResult(wifiSsid, bssid, flags, level, freq, tsf);
                            scanResult.seen = System.currentTimeMillis();
                            this.mScanResultCache.put(key, scanResult);
                        }
                        this.mNumScanResultsReturned++;
                        this.mScanResults.add(scanResult);
                    } else if (bssid != null) {
                        loge("setScanResults obtaining null BSSID results <" + bssid + ">, discard it");
                    }
                    bssid = null;
                    level = 0;
                    freq = 0;
                    tsf = 0;
                    flags = "";
                    wifiSsid = null;
                }
            }
        }
        boolean attemptAutoJoin = true;
        SupplicantState state = this.mWifiInfo.getSupplicantState();
        String selection = this.mWifiConfigStore.getLastSelectedConfiguration();
        if (getCurrentState() == this.mRoamingState || getCurrentState() == this.mObtainingIpState || getCurrentState() == this.mScanModeState || getCurrentState() == this.mDisconnectingState || ((getCurrentState() == this.mConnectedState && !this.mWifiConfigStore.enableAutoJoinWhenAssociated) || this.linkDebouncing || state == SupplicantState.ASSOCIATING || state == SupplicantState.AUTHENTICATING || state == SupplicantState.FOUR_WAY_HANDSHAKE || state == SupplicantState.GROUP_HANDSHAKE || (this.mConnectionRequests == 0 && selection == null))) {
            attemptAutoJoin = DEBUG_PARSE;
        }
        if (DBG) {
            if (selection == null) {
                selection = "<none>";
            }
            loge("wifi setScanResults state" + getCurrentState() + " sup_state=" + state + " debouncing=" + this.linkDebouncing + " mConnectionRequests=" + this.mConnectionRequests + " selection=" + selection);
        }
        if (attemptAutoJoin) {
            this.messageHandlingStatus = MESSAGE_HANDLING_STATUS_PROCESSED;
        }
        if (getDisconnectedTimeMilli() > this.mWifiConfigStore.wifiConfigLastSelectionHysteresis) {
            this.mWifiConfigStore.setLastSelectedConfiguration(-1);
        }
        if (this.mWifiConfigStore.enableAutoJoinWhenAssociated) {
            synchronized (this.mScanResultCache) {
                this.mNumScanResultsKnown = this.mWifiAutoJoinController.newSupplicantResults(attemptAutoJoin);
            }
        }
        if (this.linkDebouncing) {
            sendMessage(CMD_AUTO_ROAM, this.mLastNetworkId, 1, null);
        }
    }

    private void fetchRssiLinkSpeedAndFrequencyNative() {
        int newRssi = -1;
        int newLinkSpeed = -1;
        int newFrequency = -1;
        String signalPoll = this.mWifiNative.signalPoll();
        if (signalPoll != null) {
            String[] lines = signalPoll.split("\n");
            for (String line : lines) {
                String[] prop = line.split("=");
                if (prop.length >= 2) {
                    try {
                        if (prop[0].equals("RSSI")) {
                            newRssi = Integer.parseInt(prop[1]);
                        } else if (prop[0].equals("LINKSPEED")) {
                            newLinkSpeed = Integer.parseInt(prop[1]);
                        } else if (prop[0].equals("FREQUENCY")) {
                            newFrequency = Integer.parseInt(prop[1]);
                        }
                    } catch (NumberFormatException e) {
                    }
                }
            }
        }
        if (PDBG) {
            loge("fetchRssiLinkSpeedAndFrequencyNative rssi=" + Integer.toString(newRssi) + " linkspeed=" + Integer.toString(newLinkSpeed));
        }
        if (newRssi > -127 && newRssi < 200) {
            if (newRssi > 0) {
                newRssi -= 256;
            }
            this.mWifiInfo.setRssi(newRssi);
            int newSignalLevel = WifiManager.calculateSignalLevel(newRssi, 5);
            if (newSignalLevel != this.mLastSignalLevel) {
                sendRssiChangeBroadcast(newRssi);
            }
            this.mLastSignalLevel = newSignalLevel;
        } else {
            this.mWifiInfo.setRssi(-127);
        }
        if (newLinkSpeed != -1) {
            this.mWifiInfo.setLinkSpeed(newLinkSpeed);
        }
        if (newFrequency > 0) {
            if (ScanResult.is5GHz(newFrequency)) {
                this.mWifiConnectionStatistics.num5GhzConnected++;
            }
            if (ScanResult.is24GHz(newFrequency)) {
                this.mWifiConnectionStatistics.num24GhzConnected++;
            }
            this.mWifiInfo.setFrequency(newFrequency);
        }
        this.mWifiConfigStore.updateConfiguration(this.mWifiInfo);
    }

    boolean shouldSwitchNetwork(int networkDelta) {
        if (networkDelta <= 0) {
            return DEBUG_PARSE;
        }
        int delta = networkDelta;
        if (this.mWifiInfo != null) {
            if (this.mWifiConfigStore.enableAutoJoinWhenAssociated || this.mWifiInfo.getNetworkId() == -1) {
                if (this.mWifiInfo.txSuccessRate > 20.0d || this.mWifiInfo.rxSuccessRate > 80.0d) {
                    delta -= 999;
                } else if (this.mWifiInfo.txSuccessRate > 5.0d || this.mWifiInfo.rxSuccessRate > 30.0d) {
                    delta -= 6;
                }
                loge("WifiStateMachine shouldSwitchNetwork  txSuccessRate=" + String.format("%.2f", Double.valueOf(this.mWifiInfo.txSuccessRate)) + " rxSuccessRate=" + String.format("%.2f", Double.valueOf(this.mWifiInfo.rxSuccessRate)) + " delta " + networkDelta + " -> " + delta);
            } else {
                delta = -1000;
            }
        } else {
            loge("WifiStateMachine shouldSwitchNetwork  delta " + networkDelta + " -> " + delta);
        }
        if (delta > 0) {
            return true;
        }
        return DEBUG_PARSE;
    }

    private void cleanWifiScore() {
        this.mWifiInfo.txBadRate = 0.0d;
        this.mWifiInfo.txSuccessRate = 0.0d;
        this.mWifiInfo.txRetriesRate = 0.0d;
        this.mWifiInfo.rxSuccessRate = 0.0d;
    }

    private void calculateWifiScore(WifiLinkLayerStats stats) {
        StringBuilder sb = new StringBuilder();
        int score = 56;
        boolean isBadLinkspeed = ((!this.mWifiInfo.is24GHz() || this.mWifiInfo.getLinkSpeed() >= this.mWifiConfigStore.badLinkSpeed24) && (!this.mWifiInfo.is5GHz() || this.mWifiInfo.getLinkSpeed() >= this.mWifiConfigStore.badLinkSpeed5)) ? DEBUG_PARSE : true;
        boolean isGoodLinkspeed = ((!this.mWifiInfo.is24GHz() || this.mWifiInfo.getLinkSpeed() < this.mWifiConfigStore.goodLinkSpeed24) && (!this.mWifiInfo.is5GHz() || this.mWifiInfo.getLinkSpeed() < this.mWifiConfigStore.goodLinkSpeed5)) ? DEBUG_PARSE : true;
        if (isBadLinkspeed) {
            if (this.mBadLinkspeedcount < 6) {
                this.mBadLinkspeedcount++;
            }
        } else if (this.mBadLinkspeedcount > 0) {
            this.mBadLinkspeedcount--;
        }
        if (isBadLinkspeed) {
            sb.append(" bl(").append(this.mBadLinkspeedcount).append(")");
        }
        if (isGoodLinkspeed) {
            sb.append(" gl");
        }
        boolean use24Thresholds = DEBUG_PARSE;
        boolean homeNetworkBoost = DEBUG_PARSE;
        WifiConfiguration currentConfiguration = getCurrentWifiConfiguration();
        if (currentConfiguration != null && currentConfiguration.scanResultCache != null) {
            currentConfiguration.setVisibility(12000L);
            if (currentConfiguration.visibility != null && currentConfiguration.visibility.rssi24 != WifiConfiguration.INVALID_RSSI && currentConfiguration.visibility.rssi24 >= currentConfiguration.visibility.rssi5 + SCAN_ALARM_SOURCE) {
                use24Thresholds = true;
            }
            if (currentConfiguration.scanResultCache.size() <= 6 && currentConfiguration.allowedKeyManagement.cardinality() == 1 && currentConfiguration.allowedKeyManagement.get(1)) {
                homeNetworkBoost = true;
            }
        }
        if (homeNetworkBoost) {
            sb.append(" hn");
        }
        if (use24Thresholds) {
            sb.append(" u24");
        }
        int rssi = (this.mWifiInfo.getRssi() - (this.mAggressiveHandover * 6)) + (homeNetworkBoost ? WifiConfiguration.HOME_NETWORK_RSSI_BOOST : 0);
        sb.append(String.format(" rssi=%d ag=%d", Integer.valueOf(rssi), Integer.valueOf(this.mAggressiveHandover)));
        boolean is24GHz = (use24Thresholds || this.mWifiInfo.is24GHz()) ? true : DEBUG_PARSE;
        boolean isBadRSSI = ((!is24GHz || rssi >= this.mWifiConfigStore.thresholdBadRssi24) && (is24GHz || rssi >= this.mWifiConfigStore.thresholdBadRssi5)) ? DEBUG_PARSE : true;
        boolean isLowRSSI = ((!is24GHz || rssi >= this.mWifiConfigStore.thresholdLowRssi24) && (is24GHz || this.mWifiInfo.getRssi() >= this.mWifiConfigStore.thresholdLowRssi5)) ? DEBUG_PARSE : true;
        boolean isHighRSSI = ((!is24GHz || rssi < this.mWifiConfigStore.thresholdGoodRssi24) && (is24GHz || this.mWifiInfo.getRssi() < this.mWifiConfigStore.thresholdGoodRssi5)) ? DEBUG_PARSE : true;
        if (isBadRSSI) {
            sb.append(" br");
        }
        if (isLowRSSI) {
            sb.append(" lr");
        }
        if (isHighRSSI) {
            sb.append(" hr");
        }
        int penalizedDueToUserTriggeredDisconnect = 0;
        if (currentConfiguration != null && (this.mWifiInfo.txSuccessRate > 5.0d || this.mWifiInfo.rxSuccessRate > 5.0d)) {
            if (isBadRSSI) {
                currentConfiguration.numTicksAtBadRSSI++;
                if (currentConfiguration.numTicksAtBadRSSI > 1000) {
                    if (currentConfiguration.numUserTriggeredWifiDisableBadRSSI > 0) {
                        currentConfiguration.numUserTriggeredWifiDisableBadRSSI--;
                    }
                    if (currentConfiguration.numUserTriggeredWifiDisableLowRSSI > 0) {
                        currentConfiguration.numUserTriggeredWifiDisableLowRSSI--;
                    }
                    if (currentConfiguration.numUserTriggeredWifiDisableNotHighRSSI > 0) {
                        currentConfiguration.numUserTriggeredWifiDisableNotHighRSSI--;
                    }
                    currentConfiguration.numTicksAtBadRSSI = 0;
                }
                if (this.mWifiConfigStore.enableWifiCellularHandoverUserTriggeredAdjustment && (currentConfiguration.numUserTriggeredWifiDisableBadRSSI > 0 || currentConfiguration.numUserTriggeredWifiDisableLowRSSI > 0 || currentConfiguration.numUserTriggeredWifiDisableNotHighRSSI > 0)) {
                    score = 56 + ENABLE_WIFI;
                    penalizedDueToUserTriggeredDisconnect = 1;
                    sb.append(" p1");
                }
            } else if (isLowRSSI) {
                currentConfiguration.numTicksAtLowRSSI++;
                if (currentConfiguration.numTicksAtLowRSSI > 1000) {
                    if (currentConfiguration.numUserTriggeredWifiDisableLowRSSI > 0) {
                        currentConfiguration.numUserTriggeredWifiDisableLowRSSI--;
                    }
                    if (currentConfiguration.numUserTriggeredWifiDisableNotHighRSSI > 0) {
                        currentConfiguration.numUserTriggeredWifiDisableNotHighRSSI--;
                    }
                    currentConfiguration.numTicksAtLowRSSI = 0;
                }
                if (this.mWifiConfigStore.enableWifiCellularHandoverUserTriggeredAdjustment && (currentConfiguration.numUserTriggeredWifiDisableLowRSSI > 0 || currentConfiguration.numUserTriggeredWifiDisableNotHighRSSI > 0)) {
                    score = 56 + ENABLE_WIFI;
                    penalizedDueToUserTriggeredDisconnect = 2;
                    sb.append(" p2");
                }
            } else if (!isHighRSSI) {
                currentConfiguration.numTicksAtNotHighRSSI++;
                if (currentConfiguration.numTicksAtNotHighRSSI > 1000) {
                    if (currentConfiguration.numUserTriggeredWifiDisableNotHighRSSI > 0) {
                        currentConfiguration.numUserTriggeredWifiDisableNotHighRSSI--;
                    }
                    currentConfiguration.numTicksAtNotHighRSSI = 0;
                }
                if (this.mWifiConfigStore.enableWifiCellularHandoverUserTriggeredAdjustment && currentConfiguration.numUserTriggeredWifiDisableNotHighRSSI > 0) {
                    score = 56 + ENABLE_WIFI;
                    penalizedDueToUserTriggeredDisconnect = 3;
                    sb.append(" p3");
                }
            }
            sb.append(String.format(" ticks %d,%d,%d", Integer.valueOf(currentConfiguration.numTicksAtBadRSSI), Integer.valueOf(currentConfiguration.numTicksAtLowRSSI), Integer.valueOf(currentConfiguration.numTicksAtNotHighRSSI)));
        }
        if (PDBG) {
            String rssiStatus = "";
            if (isBadRSSI) {
                rssiStatus = " badRSSI ";
            } else if (isHighRSSI) {
                rssiStatus = " highRSSI ";
            } else if (isLowRSSI) {
                rssiStatus = " lowRSSI ";
            }
            if (isBadLinkspeed) {
                rssiStatus = rssiStatus + " lowSpeed ";
            }
            loge("calculateWifiScore freq=" + Integer.toString(this.mWifiInfo.getFrequency()) + " speed=" + Integer.toString(this.mWifiInfo.getLinkSpeed()) + " score=" + Integer.toString(this.mWifiInfo.score) + rssiStatus + " -> txbadrate=" + String.format("%.2f", Double.valueOf(this.mWifiInfo.txBadRate)) + " txgoodrate=" + String.format("%.2f", Double.valueOf(this.mWifiInfo.txSuccessRate)) + " txretriesrate=" + String.format("%.2f", Double.valueOf(this.mWifiInfo.txRetriesRate)) + " rxrate=" + String.format("%.2f", Double.valueOf(this.mWifiInfo.rxSuccessRate)) + " userTriggerdPenalty" + penalizedDueToUserTriggeredDisconnect);
        }
        if (this.mWifiInfo.txBadRate >= 1.0d && this.mWifiInfo.txSuccessRate < 3.0d && (isBadRSSI || isLowRSSI)) {
            if (this.mWifiInfo.linkStuckCount < 5) {
                this.mWifiInfo.linkStuckCount++;
            }
            sb.append(String.format(" ls+=%d", Integer.valueOf(this.mWifiInfo.linkStuckCount)));
            if (PDBG) {
                loge(" bad link -> stuck count =" + Integer.toString(this.mWifiInfo.linkStuckCount));
            }
        } else if (this.mWifiInfo.txSuccessRate > 2.0d || this.mWifiInfo.txBadRate < 0.1d) {
            if (this.mWifiInfo.linkStuckCount > 0) {
                WifiInfo wifiInfo = this.mWifiInfo;
                wifiInfo.linkStuckCount--;
            }
            sb.append(String.format(" ls-=%d", Integer.valueOf(this.mWifiInfo.linkStuckCount)));
            if (PDBG) {
                loge(" good link -> stuck count =" + Integer.toString(this.mWifiInfo.linkStuckCount));
            }
        }
        sb.append(String.format(" [%d", Integer.valueOf(score)));
        if (this.mWifiInfo.linkStuckCount > 1) {
            score -= (this.mWifiInfo.linkStuckCount - 1) * 2;
        }
        sb.append(String.format(",%d", Integer.valueOf(score)));
        if (isBadLinkspeed) {
            score += SET_ALLOW_UNTRUSTED_SOURCE;
            if (PDBG) {
                loge(" isBadLinkspeed   ---> count=" + this.mBadLinkspeedcount + " score=" + Integer.toString(score));
            }
        } else if (isGoodLinkspeed && this.mWifiInfo.txSuccessRate > 5.0d) {
            score += 4;
        }
        sb.append(String.format(",%d", Integer.valueOf(score)));
        if (isBadRSSI) {
            if (this.mWifiInfo.badRssiCount < 7) {
                this.mWifiInfo.badRssiCount++;
            }
        } else if (isLowRSSI) {
            this.mWifiInfo.lowRssiCount = 1;
            if (this.mWifiInfo.badRssiCount > 0) {
                WifiInfo wifiInfo2 = this.mWifiInfo;
                wifiInfo2.badRssiCount--;
            }
        } else {
            this.mWifiInfo.badRssiCount = 0;
            this.mWifiInfo.lowRssiCount = 0;
        }
        int score2 = score - ((this.mWifiInfo.badRssiCount * 2) + this.mWifiInfo.lowRssiCount);
        sb.append(String.format(",%d", Integer.valueOf(score2)));
        if (PDBG) {
            loge(" badRSSI count" + Integer.toString(this.mWifiInfo.badRssiCount) + " lowRSSI count" + Integer.toString(this.mWifiInfo.lowRssiCount) + " --> score " + Integer.toString(score2));
        }
        if (isHighRSSI) {
            score2 += 5;
            if (PDBG) {
                loge(" isHighRSSI       ---> score=" + Integer.toString(score2));
            }
        }
        sb.append(String.format(",%d]", Integer.valueOf(score2)));
        sb.append(String.format(" brc=%d lrc=%d", Integer.valueOf(this.mWifiInfo.badRssiCount), Integer.valueOf(this.mWifiInfo.lowRssiCount)));
        if (score2 > 60) {
            score2 = 60;
        }
        if (score2 < 0) {
            score2 = 0;
        }
        if (score2 != this.mWifiInfo.score) {
            if (DBG) {
                loge("calculateWifiScore() report new score " + Integer.toString(score2));
            }
            this.mWifiInfo.score = score2;
            if (this.mNetworkAgent != null) {
                this.mNetworkAgent.sendNetworkScore(score2);
            }
        }
        this.wifiScoringReport = sb.toString();
    }

    public double getTxPacketRate() {
        if (this.mWifiInfo != null) {
            return this.mWifiInfo.txSuccessRate;
        }
        return -1.0d;
    }

    public double getRxPacketRate() {
        if (this.mWifiInfo != null) {
            return this.mWifiInfo.rxSuccessRate;
        }
        return -1.0d;
    }

    private void fetchPktcntNative(RssiPacketCountInfo info) {
        String pktcntPoll = this.mWifiNative.pktcntPoll();
        if (pktcntPoll != null) {
            String[] lines = pktcntPoll.split("\n");
            for (String line : lines) {
                String[] prop = line.split("=");
                if (prop.length >= 2) {
                    try {
                        if (prop[0].equals("TXGOOD")) {
                            info.txgood = Integer.parseInt(prop[1]);
                        } else if (prop[0].equals("TXBAD")) {
                            info.txbad = Integer.parseInt(prop[1]);
                        }
                    } catch (NumberFormatException e) {
                    }
                }
            }
        }
    }

    private boolean clearIPv4Address(String iface) {
        try {
            InterfaceConfiguration ifcg = new InterfaceConfiguration();
            ifcg.setLinkAddress(new LinkAddress("0.0.0.0/0"));
            this.mNwService.setInterfaceConfig(iface, ifcg);
            return true;
        } catch (RemoteException e) {
            return DEBUG_PARSE;
        }
    }

    private boolean isProvisioned(LinkProperties lp) {
        if (lp.isProvisioned() || (this.mWifiConfigStore.isUsingStaticIp(this.mLastNetworkId) && lp.hasIPv4Address())) {
            return true;
        }
        return DEBUG_PARSE;
    }

    private void updateLinkProperties(int reason) {
        LinkProperties newLp = new LinkProperties();
        newLp.setInterfaceName(this.mInterfaceName);
        newLp.setHttpProxy(this.mWifiConfigStore.getProxyProperties(this.mLastNetworkId));
        LinkProperties netlinkLinkProperties = this.mNetlinkTracker.getLinkProperties();
        newLp.setLinkAddresses(netlinkLinkProperties.getLinkAddresses());
        for (RouteInfo route : netlinkLinkProperties.getRoutes()) {
            newLp.addRoute(route);
        }
        for (InetAddress dns : netlinkLinkProperties.getDnsServers()) {
            newLp.addDnsServer(dns);
        }
        synchronized (this.mDhcpResultsLock) {
            if (this.mDhcpResults != null) {
                for (RouteInfo route2 : this.mDhcpResults.getRoutes(this.mInterfaceName)) {
                    newLp.addRoute(route2);
                }
                for (InetAddress dns2 : this.mDhcpResults.dnsServers) {
                    newLp.addDnsServer(dns2);
                }
                newLp.setDomains(this.mDhcpResults.domains);
            }
        }
        boolean linkChanged = !newLp.equals(this.mLinkProperties) ? true : DEBUG_PARSE;
        boolean wasProvisioned = isProvisioned(this.mLinkProperties);
        boolean isProvisioned = isProvisioned(newLp);
        boolean lostIPv4Provisioning = (!this.mLinkProperties.hasIPv4Address() || newLp.hasIPv4Address()) ? DEBUG_PARSE : true;
        NetworkInfo.DetailedState detailedState = getNetworkDetailedState();
        if (linkChanged) {
            if (DBG) {
                log("Link configuration changed for netId: " + this.mLastNetworkId + " old: " + this.mLinkProperties + " new: " + newLp);
            }
            this.mLinkProperties = newLp;
            if (!TextUtils.isEmpty(this.mTcpBufferSizes)) {
                this.mLinkProperties.setTcpBufferSizes(this.mTcpBufferSizes);
            }
            if (this.mNetworkAgent != null) {
                this.mNetworkAgent.sendLinkProperties(this.mLinkProperties);
            }
        }
        if (DBG) {
            StringBuilder sb = new StringBuilder();
            sb.append("updateLinkProperties nid: " + this.mLastNetworkId);
            sb.append(" state: " + detailedState);
            sb.append(" reason: " + smToString(reason));
            if (this.mLinkProperties != null) {
                if (this.mLinkProperties.hasIPv4Address()) {
                    sb.append(" v4");
                }
                if (this.mLinkProperties.hasGlobalIPv6Address()) {
                    sb.append(" v6");
                }
                if (this.mLinkProperties.hasIPv4DefaultRoute()) {
                    sb.append(" v4r");
                }
                if (this.mLinkProperties.hasIPv6DefaultRoute()) {
                    sb.append(" v6r");
                }
                if (this.mLinkProperties.hasIPv4DnsServer()) {
                    sb.append(" v4dns");
                }
                if (this.mLinkProperties.hasIPv6DnsServer()) {
                    sb.append(" v6dns");
                }
                if (isProvisioned) {
                    sb.append(" isprov");
                }
            }
            loge(sb.toString());
        }
        switch (reason) {
            case 1:
            case CMD_STATIC_IP_SUCCESS:
                sendMessage(CMD_IP_CONFIGURATION_SUCCESSFUL);
                if (!isProvisioned) {
                    loge("IPv4 config succeeded, but not provisioned");
                    return;
                }
                return;
            case 2:
                if (!isProvisioned || lostIPv4Provisioning) {
                    sendMessage(CMD_IP_CONFIGURATION_LOST);
                    return;
                }
                sendMessage(CMD_IP_CONFIGURATION_SUCCESSFUL);
                loge("DHCP failure: provisioned, clearing IPv4 address.");
                if (!clearIPv4Address(this.mInterfaceName)) {
                    sendMessage(CMD_IP_CONFIGURATION_LOST);
                    return;
                }
                return;
            case CMD_STATIC_IP_FAILURE:
                sendMessage(CMD_IP_CONFIGURATION_LOST);
                return;
            case CMD_UPDATE_LINKPROPERTIES:
                if (wasProvisioned && !isProvisioned) {
                    sendMessage(CMD_IP_CONFIGURATION_LOST);
                } else if (!wasProvisioned && isProvisioned) {
                    sendMessage(CMD_IP_CONFIGURATION_SUCCESSFUL);
                }
                if (linkChanged && getNetworkDetailedState() == NetworkInfo.DetailedState.CONNECTED) {
                    sendLinkConfigurationChangedBroadcast();
                    return;
                }
                return;
            default:
                return;
        }
    }

    private void clearLinkProperties() {
        synchronized (this.mDhcpResultsLock) {
            if (this.mDhcpResults != null) {
                this.mDhcpResults.clear();
            }
        }
        this.mNetlinkTracker.clearLinkProperties();
        this.mLinkProperties.clear();
        if (this.mNetworkAgent != null) {
            this.mNetworkAgent.sendLinkProperties(this.mLinkProperties);
        }
    }

    private void configureIPv6LinkProperties(DhcpResults dhcpResults, int reason) {
        if (!this.mWifiConfigStore.isUsingStaticIp(this.mLastNetworkId)) {
            synchronized (this.mDhcpResultsLock) {
                if (this.mDhcpResults != null) {
                    this.mDhcpResults.updateFromDhcpIpv6Info(dhcpResults);
                }
            }
        }
        updateLinkProperties(reason);
        if (DBG) {
            log("netId=" + this.mLastNetworkId + " Link configured: " + this.mLinkProperties);
        }
    }

    private String updateDefaultRouteMacAddress(int timeout) throws Throwable {
        String address = null;
        for (RouteInfo route : this.mLinkProperties.getRoutes()) {
            if (route.isDefaultRoute() && route.hasGateway()) {
                InetAddress gateway = route.getGateway();
                if (gateway instanceof Inet4Address) {
                    if (PDBG) {
                        loge("updateDefaultRouteMacAddress found Ipv4 default :" + gateway.getHostAddress());
                    }
                    address = macAddressFromRoute(gateway.getHostAddress());
                    if (address == null && timeout > 0) {
                        boolean reachable = DEBUG_PARSE;
                        try {
                            try {
                                reachable = gateway.isReachable(timeout);
                            } catch (Exception e) {
                                loge("updateDefaultRouteMacAddress exception reaching :" + gateway.getHostAddress());
                                if (reachable) {
                                    address = macAddressFromRoute(gateway.getHostAddress());
                                    if (PDBG) {
                                        loge("updateDefaultRouteMacAddress reachable (tried again) :" + gateway.getHostAddress() + " found " + address);
                                    }
                                }
                            }
                        } finally {
                            if (reachable) {
                                String address2 = macAddressFromRoute(gateway.getHostAddress());
                                if (PDBG) {
                                    loge("updateDefaultRouteMacAddress reachable (tried again) :" + gateway.getHostAddress() + " found " + address2);
                                }
                            }
                        }
                    }
                    if (address != null) {
                        this.mWifiConfigStore.setDefaultGwMacAddress(this.mLastNetworkId, address);
                    }
                }
            }
        }
        return address;
    }

    private void sendScanResultsAvailableBroadcast() {
        Intent intent = new Intent("android.net.wifi.SCAN_RESULTS");
        intent.addFlags(67108864);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void sendRssiChangeBroadcast(int newRssi) {
        try {
            this.mBatteryStats.noteWifiRssiChanged(newRssi);
        } catch (RemoteException e) {
        }
        Intent intent = new Intent("android.net.wifi.RSSI_CHANGED");
        intent.addFlags(67108864);
        intent.putExtra("newRssi", newRssi);
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void sendNetworkStateChangeBroadcast(String bssid) {
        Intent intent = new Intent("android.net.wifi.STATE_CHANGE");
        intent.addFlags(67108864);
        intent.putExtra("networkInfo", new NetworkInfo(this.mNetworkInfo));
        intent.putExtra("linkProperties", new LinkProperties(this.mLinkProperties));
        if (bssid != null) {
            intent.putExtra("bssid", bssid);
        }
        if (this.mNetworkInfo.getDetailedState() == NetworkInfo.DetailedState.VERIFYING_POOR_LINK || this.mNetworkInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED) {
            intent.putExtra("wifiInfo", new WifiInfo(this.mWifiInfo));
        }
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void sendLinkConfigurationChangedBroadcast() {
        Intent intent = new Intent("android.net.wifi.LINK_CONFIGURATION_CHANGED");
        intent.addFlags(67108864);
        intent.putExtra("linkProperties", new LinkProperties(this.mLinkProperties));
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void sendSupplicantConnectionChangedBroadcast(boolean connected) {
        Intent intent = new Intent("android.net.wifi.supplicant.CONNECTION_CHANGE");
        intent.addFlags(67108864);
        intent.putExtra("connected", connected);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void sendWifiApClientChangedBroadcast() {
        Intent intent = new Intent("android.net.wifi.WIFI_AP_CLIENT_CHANGED");
        intent.addFlags(67108864);
        intent.putExtra("wifi_ap_client", this.mPeers.getDeviceList().size());
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    private boolean setNetworkDetailedState(NetworkInfo.DetailedState state) {
        boolean hidden = DEBUG_PARSE;
        if (this.linkDebouncing || isRoaming()) {
            hidden = true;
        }
        if (DBG) {
            log("setDetailed state, old =" + this.mNetworkInfo.getDetailedState() + " and new state=" + state + " hidden=" + hidden);
        }
        if (this.mNetworkInfo.getExtraInfo() != null && this.mWifiInfo.getSSID() != null && !this.mNetworkInfo.getExtraInfo().equals(this.mWifiInfo.getSSID())) {
            if (DBG) {
                log("setDetailed state send new extra info" + this.mWifiInfo.getSSID());
            }
            this.mNetworkInfo.setExtraInfo(this.mWifiInfo.getSSID());
            sendNetworkStateChangeBroadcast(null);
        }
        if (hidden || state == this.mNetworkInfo.getDetailedState()) {
            return DEBUG_PARSE;
        }
        this.mNetworkInfo.setDetailedState(state, null, this.mWifiInfo.getSSID());
        if (this.mNetworkAgent != null) {
            this.mNetworkAgent.sendNetworkInfo(this.mNetworkInfo);
        }
        sendNetworkStateChangeBroadcast(null);
        return true;
    }

    private NetworkInfo.DetailedState getNetworkDetailedState() {
        return this.mNetworkInfo.getDetailedState();
    }

    private boolean checkSsidConverted(WifiConfiguration config) {
        if (config.SSID == null) {
            return DEBUG_PARSE;
        }
        String[] estr = config.SSID.split("\"");
        List<ScanResult> scanResults = syncGetScanResultsList();
        if (scanResults == null) {
            return DEBUG_PARSE;
        }
        for (int i = scanResults.size() - 1; i >= 0; i--) {
            ScanResult scanResult = scanResults.get(i);
            if (scanResult.SSID.equals(estr[1]) && scanResult.wifiSsid.NOT_UTF8) {
                if (DBG) {
                    loge("target converted SSID found:" + scanResult.SSID);
                }
                return true;
            }
        }
        return DEBUG_PARSE;
    }

    private SupplicantState handleSupplicantStateChange(Message message) {
        StateChangeResult stateChangeResult = (StateChangeResult) message.obj;
        SupplicantState state = stateChangeResult.state;
        this.mWifiInfo.setSupplicantState(state);
        if (SupplicantState.isConnecting(state)) {
            this.mWifiInfo.setNetworkId(stateChangeResult.networkId);
        } else {
            this.mWifiInfo.setNetworkId(-1);
        }
        this.mWifiInfo.setBSSID(stateChangeResult.BSSID);
        this.mWifiInfo.setSSID(stateChangeResult.wifiSsid);
        this.mSupplicantStateTracker.sendMessage(Message.obtain(message));
        return state;
    }

    private void handleNetworkDisconnect() {
        if (DBG) {
            log("handleNetworkDisconnect: Stopping DHCP and clearing IP stack:" + Thread.currentThread().getStackTrace()[2].getMethodName() + " - " + Thread.currentThread().getStackTrace()[3].getMethodName() + " - " + Thread.currentThread().getStackTrace()[4].getMethodName() + " - " + Thread.currentThread().getStackTrace()[5].getMethodName());
        }
        clearCurrentConfigBSSID("handleNetworkDisconnect");
        stopDhcp();
        try {
            this.mNwService.clearInterfaceAddresses(this.mInterfaceName);
            this.mNwService.disableIpv6(this.mInterfaceName);
        } catch (Exception e) {
            loge("Failed to clear addresses or disable ipv6" + e);
        }
        this.mBadLinkspeedcount = 0;
        this.mWifiInfo.reset();
        this.linkDebouncing = DEBUG_PARSE;
        this.mAutoRoaming = 0;
        this.fullBandConnectedTimeIntervalMilli = 20000L;
        setNetworkDetailedState(NetworkInfo.DetailedState.DISCONNECTED);
        if (this.mNetworkAgent != null) {
            this.mNetworkAgent.sendNetworkInfo(this.mNetworkInfo);
            this.mNetworkAgent = null;
        }
        this.mWifiConfigStore.updateStatus(this.mLastNetworkId, NetworkInfo.DetailedState.DISCONNECTED);
        clearLinkProperties();
        sendNetworkStateChangeBroadcast(this.mLastBssid);
        autoRoamSetBSSID(this.mLastNetworkId, "any");
        this.mLastBssid = null;
        registerDisconnected();
        this.mLastNetworkId = -1;
    }

    private void handleHostapdConnectionLoss() {
        this.mWifiMonitor.killSupplicant(1);
        this.mWifiNative.closeSupplicantConnection();
        try {
            this.mNwService.setInterfaceDown(this.mInterfaceName);
        } catch (Exception e) {
            loge("Unable to change interface " + this.mInterfaceName + " settings: " + e);
        }
        this.mPeers.clear();
        setWifiApState(11);
    }

    private void handleSupplicantConnectionLoss() {
        this.mWifiMonitor.killSupplicant(this.mP2pSupported ? 2 : 0);
        this.mWifiNative.closeSupplicantConnection();
        try {
            this.mNwService.setInterfaceDown(this.mInterfaceName);
        } catch (RemoteException e) {
            loge("Unable to change interface " + this.mInterfaceName + " settings: " + e);
        } catch (IllegalStateException ie) {
            loge("Unable to change interface settings: " + ie);
        }
        sendSupplicantConnectionChangedBroadcast(DEBUG_PARSE);
        setWifiState(1);
    }

    void handlePreDhcpSetup() {
        this.mDhcpActive = true;
        if (!this.mBluetoothConnectionActive) {
            WifiNative wifiNative = this.mWifiNative;
            WifiNative wifiNative2 = this.mWifiNative;
            wifiNative.setBluetoothCoexistenceMode(1);
        }
        setSuspendOptimizationsNative(1, DEBUG_PARSE);
        this.mWifiNative.setPowerSave(DEBUG_PARSE);
        stopBatchedScan();
        WifiNative.pauseScan();
        getWifiLinkLayerStats(DEBUG_PARSE);
        Message msg = new Message();
        msg.what = WifiP2pServiceImpl.BLOCK_DISCOVERY;
        msg.arg1 = 1;
        msg.arg2 = 196615;
        msg.obj = this.mDhcpStateMachine;
        this.mWifiP2pChannel.sendMessage(msg);
    }

    void startDhcp() {
        if (this.mDhcpStateMachine == null) {
            this.mDhcpStateMachine = DhcpStateMachine.makeDhcpStateMachine(this.mContext, this, this.mInterfaceName);
        }
        this.mDhcpStateMachine.registerForPreDhcpNotification();
        this.mDhcpStateMachine.sendMessage(196609);
    }

    void renewDhcp() {
        if (this.mDhcpStateMachine == null) {
            this.mDhcpStateMachine = DhcpStateMachine.makeDhcpStateMachine(this.mContext, this, this.mInterfaceName);
        }
        this.mDhcpStateMachine.registerForPreDhcpNotification();
        this.mDhcpStateMachine.sendMessage(196611);
    }

    void stopDhcp() {
        if (this.mDhcpStateMachine != null) {
            handlePostDhcpSetup();
            this.mDhcpStateMachine.sendMessage(196610);
        }
    }

    void handlePostDhcpSetup() {
        setSuspendOptimizationsNative(1, true);
        this.mWifiNative.setPowerSave(true);
        this.mWifiP2pChannel.sendMessage(WifiP2pServiceImpl.BLOCK_DISCOVERY, 0);
        WifiNative wifiNative = this.mWifiNative;
        WifiNative wifiNative2 = this.mWifiNative;
        wifiNative.setBluetoothCoexistenceMode(2);
        this.mDhcpActive = DEBUG_PARSE;
        startBatchedScan();
        WifiNative.restartScan();
    }

    private void handleIPv4Success(DhcpResults dhcpResults, int reason) {
        if (PDBG) {
            loge("wifistatemachine handleIPv4Success <" + dhcpResults.toString() + ">");
            loge("link address " + dhcpResults.ipAddress);
        }
        synchronized (this.mDhcpResultsLock) {
            this.mDhcpResults = dhcpResults;
        }
        Inet4Address addr = (Inet4Address) dhcpResults.ipAddress.getAddress();
        if (isRoaming()) {
            if (addr instanceof Inet4Address) {
                int previousAddress = this.mWifiInfo.getIpAddress();
                int newAddress = NetworkUtils.inetAddressToInt(addr);
                if (previousAddress != newAddress) {
                    loge("handleIPv4Success, roaming and address changed" + this.mWifiInfo + " got: " + addr);
                }
            } else {
                loge("handleIPv4Success, roaming and didnt get an IPv4 address" + addr.toString());
            }
        }
        this.mWifiInfo.setInetAddress(addr);
        this.mWifiInfo.setMeteredHint(dhcpResults.hasMeteredHint());
        updateLinkProperties(reason);
    }

    private void handleSuccessfulIpConfiguration() {
        this.mLastSignalLevel = -1;
        WifiConfiguration c = getCurrentWifiConfiguration();
        if (c != null) {
            c.numConnectionFailures = 0;
            updateCapabilities(c);
        }
        if (c != null) {
            ScanResult result = getCurrentScanResult();
            if (result == null) {
                loge("WifiStateMachine: handleSuccessfulIpConfiguration and no scan results" + c.configKey());
            } else {
                result.numIpConfigFailures = 0;
                this.mWifiNative.clearBlacklist();
            }
        }
    }

    private void handleIPv4Failure(int reason) {
        synchronized (this.mDhcpResultsLock) {
            if (this.mDhcpResults != null) {
                this.mDhcpResults.clear();
            }
        }
        if (PDBG) {
            loge("wifistatemachine handleIPv4Failure");
        }
        updateLinkProperties(reason);
    }

    private void handleIpConfigurationLost() {
        this.mWifiInfo.setInetAddress(null);
        this.mWifiInfo.setMeteredHint(DEBUG_PARSE);
        this.mWifiConfigStore.handleSSIDStateChange(this.mLastNetworkId, DEBUG_PARSE, "DHCP FAILURE", this.mWifiInfo.getBSSID());
        this.mWifiNative.disconnect();
    }

    private void startSoftApWithConfig(final WifiConfiguration config) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    WifiStateMachine.this.mNwService.wifiFirmwareReload(WifiStateMachine.this.mInterfaceName, "AP");
                } catch (Exception e) {
                    WifiStateMachine.this.loge("Failed to reload softap firmware " + e);
                }
                try {
                    WifiStateMachine.this.mNwService.setAccessPoint(config, WifiStateMachine.this.mInterfaceName);
                } catch (Exception e2) {
                    WifiStateMachine.this.loge("Failed to set softap configuration " + e2);
                }
                try {
                    WifiStateMachine.this.mNwService.setInterfaceDown(WifiStateMachine.this.mInterfaceName);
                    WifiStateMachine.this.mNwService.setInterfaceIpv6PrivacyExtensions(WifiStateMachine.this.mInterfaceName, true);
                } catch (RemoteException re) {
                    WifiStateMachine.this.loge("Unable to change interface settings: " + re);
                } catch (IllegalStateException ie) {
                    WifiStateMachine.this.loge("Unable to change interface settings: " + ie);
                }
                WifiNative unused = WifiStateMachine.this.mWifiNative;
                if (WifiNative.startSupplicant(1)) {
                    if (WifiStateMachine.DBG) {
                        WifiStateMachine.this.log("Hostapd start successful");
                    }
                    WifiStateMachine.this.mWifiMonitor.startMonitoring();
                    if (WifiStateMachine.DBG) {
                        WifiStateMachine.this.log("Soft AP start successful");
                    }
                    WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_START_AP_SUCCESS);
                    return;
                }
                WifiStateMachine.this.loge("Failed to start hostapd!");
                WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_START_AP_FAILURE);
            }
        }).start();
    }

    private void stopSoftAp() {
        this.mWifiMonitor.killSupplicant(1);
        this.mWifiNative.closeSupplicantConnection();
        try {
            this.mNwService.setInterfaceDown(this.mInterfaceName);
            this.mNwService.wifiFirmwareReload(this.mInterfaceName, "STA");
        } catch (Exception e) {
            loge("Failed to stop Soft Ap " + e);
        }
    }

    private String macAddressFromRoute(String ipAddress) throws Throwable {
        BufferedReader reader;
        String macAddress = null;
        BufferedReader reader2 = null;
        try {
            try {
                reader = new BufferedReader(new FileReader("/proc/net/arp"));
            } catch (Throwable th) {
                th = th;
            }
            try {
                reader.readLine();
                while (true) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    String[] tokens = line.split("[ ]+");
                    if (tokens.length >= 6) {
                        String ip = tokens[0];
                        String mac = tokens[3];
                        if (ipAddress.equals(ip)) {
                            macAddress = mac;
                            break;
                        }
                    }
                }
                if (macAddress == null) {
                    loge("Did not find remoteAddress {" + ipAddress + "} in /proc/net/arp");
                }
                if (reader != null) {
                    try {
                        reader.close();
                        reader2 = reader;
                    } catch (IOException e) {
                        reader2 = reader;
                    }
                } else {
                    reader2 = reader;
                }
            } catch (FileNotFoundException e2) {
                reader2 = reader;
                loge("Could not open /proc/net/arp to lookup mac address");
                if (reader2 != null) {
                    try {
                        reader2.close();
                    } catch (IOException e3) {
                    }
                }
            } catch (IOException e4) {
                reader2 = reader;
                loge("Could not read /proc/net/arp to lookup mac address");
                if (reader2 != null) {
                    try {
                        reader2.close();
                    } catch (IOException e5) {
                    }
                }
            } catch (Throwable th2) {
                th = th2;
                reader2 = reader;
                if (reader2 != null) {
                    try {
                        reader2.close();
                    } catch (IOException e6) {
                    }
                }
                throw th;
            }
        } catch (FileNotFoundException e7) {
        } catch (IOException e8) {
        }
        return macAddress;
    }

    private class WifiNetworkFactory extends NetworkFactory {
        public WifiNetworkFactory(Looper l, Context c, String TAG, NetworkCapabilities f) {
            super(l, c, TAG, f);
        }

        protected void needNetworkFor(NetworkRequest networkRequest, int score) {
            WifiStateMachine.access$1404(WifiStateMachine.this);
        }

        protected void releaseNetworkFor(NetworkRequest networkRequest) {
            WifiStateMachine.access$1406(WifiStateMachine.this);
        }

        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            pw.println("mConnectionRequests " + WifiStateMachine.this.mConnectionRequests);
        }
    }

    private class UntrustedWifiNetworkFactory extends NetworkFactory {
        private int mUntrustedReqCount;

        public UntrustedWifiNetworkFactory(Looper l, Context c, String tag, NetworkCapabilities f) {
            super(l, c, tag, f);
        }

        protected void needNetworkFor(NetworkRequest networkRequest, int score) {
            if (!networkRequest.networkCapabilities.hasCapability(14)) {
                int i = this.mUntrustedReqCount + 1;
                this.mUntrustedReqCount = i;
                if (i == 1) {
                    WifiStateMachine.this.mWifiAutoJoinController.setAllowUntrustedConnections(true);
                }
            }
        }

        protected void releaseNetworkFor(NetworkRequest networkRequest) {
            if (!networkRequest.networkCapabilities.hasCapability(14)) {
                int i = this.mUntrustedReqCount - 1;
                this.mUntrustedReqCount = i;
                if (i == 0) {
                    WifiStateMachine.this.mWifiAutoJoinController.setAllowUntrustedConnections(WifiStateMachine.DEBUG_PARSE);
                }
            }
        }

        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            pw.println("mUntrustedReqCount " + this.mUntrustedReqCount);
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
                this.mUntrustedNetworkFactory.setScoreFilter(Integer.MAX_VALUE);
                this.mUntrustedNetworkFactory.register();
            }
        }
    }

    class DefaultState extends State {
        DefaultState() {
        }

        public boolean processMessage(Message message) {
            boolean z = WifiStateMachine.DEBUG_PARSE;
            WifiStateMachine.this.logStateAndMessage(message, getClass().getSimpleName());
            switch (message.what) {
                case 69632:
                    AsyncChannel ac = (AsyncChannel) message.obj;
                    if (ac == WifiStateMachine.this.mWifiP2pChannel) {
                        if (message.arg1 == 0) {
                            WifiStateMachine.this.mWifiP2pChannel.sendMessage(69633);
                        } else {
                            WifiStateMachine.this.loge("WifiP2pService connection failure, error=" + message.arg1);
                        }
                    } else {
                        WifiStateMachine.this.loge("got HALF_CONNECTED for unknown channel");
                    }
                    return true;
                case 69636:
                    AsyncChannel ac2 = (AsyncChannel) message.obj;
                    if (ac2 == WifiStateMachine.this.mWifiP2pChannel) {
                        WifiStateMachine.this.loge("WifiP2pService channel lost, message.arg1 =" + message.arg1);
                    }
                    return true;
                case WifiStateMachine.CMD_START_SUPPLICANT:
                case WifiStateMachine.CMD_STOP_SUPPLICANT:
                case WifiStateMachine.CMD_START_DRIVER:
                case WifiStateMachine.CMD_STOP_DRIVER:
                case WifiStateMachine.CMD_STOP_SUPPLICANT_FAILED:
                case WifiStateMachine.CMD_DELAYED_STOP_DRIVER:
                case WifiStateMachine.CMD_DRIVER_START_TIMED_OUT:
                case WifiStateMachine.CMD_START_AP:
                case WifiStateMachine.CMD_START_AP_SUCCESS:
                case WifiStateMachine.CMD_START_AP_FAILURE:
                case WifiStateMachine.CMD_STOP_AP:
                case WifiStateMachine.CMD_SET_AP_CONFIG:
                case WifiStateMachine.CMD_SET_AP_CONFIG_COMPLETED:
                case WifiStateMachine.CMD_REQUEST_AP_CONFIG:
                case WifiStateMachine.CMD_RESPONSE_AP_CONFIG:
                case WifiStateMachine.CMD_TETHER_STATE_CHANGE:
                case WifiStateMachine.CMD_TETHER_NOTIFICATION_TIMED_OUT:
                case WifiStateMachine.CMD_ENABLE_ALL_NETWORKS:
                case WifiStateMachine.CMD_BLACKLIST_NETWORK:
                case WifiStateMachine.CMD_CLEAR_BLACKLIST:
                case WifiStateMachine.CMD_SET_OPERATIONAL_MODE:
                case WifiStateMachine.CMD_DISCONNECT:
                case WifiStateMachine.CMD_RECONNECT:
                case WifiStateMachine.CMD_REASSOCIATE:
                case WifiStateMachine.CMD_SET_COUNTRY_CODE:
                case WifiStateMachine.CMD_RSSI_POLL:
                case WifiStateMachine.CMD_NO_NETWORKS_PERIODIC_SCAN:
                case WifiStateMachine.CMD_TEST_NETWORK_DISCONNECT:
                case WifiStateMachine.CMD_SET_FREQUENCY_BAND:
                case WifiStateMachine.CMD_OBTAINING_IP_ADDRESS_WATCHDOG_TIMER:
                case WifiStateMachine.CMD_ROAM_WATCHDOG_TIMER:
                case WifiStateMachine.CMD_DISCONNECTING_WATCHDOG_TIMER:
                case WifiStateMachine.CMD_DISABLE_EPHEMERAL_NETWORK:
                case WifiStateMachine.CMD_DISABLE_P2P_RSP:
                case WifiStateMachine.CMD_TARGET_BSSID:
                case WifiStateMachine.CMD_RELOAD_TLS_AND_RECONNECT:
                case WifiStateMachine.CMD_AUTO_CONNECT:
                case WifiStateMachine.CMD_UNWANTED_NETWORK:
                case WifiStateMachine.CMD_AUTO_ROAM:
                case WifiStateMachine.CMD_AUTO_SAVE_NETWORK:
                case WifiStateMachine.CMD_ASSOCIATED_BSSID:
                case 135189:
                case 135190:
                case WifiMonitor.SUP_CONNECTION_EVENT:
                case WifiMonitor.SUP_DISCONNECTION_EVENT:
                case WifiMonitor.NETWORK_CONNECTION_EVENT:
                case WifiMonitor.NETWORK_DISCONNECTION_EVENT:
                case WifiMonitor.SCAN_RESULTS_EVENT:
                case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT:
                case WifiMonitor.AUTHENTICATION_FAILURE_EVENT:
                case WifiMonitor.WPS_OVERLAP_EVENT:
                case WifiMonitor.SUP_REQUEST_IDENTITY:
                case WifiMonitor.SUP_REQUEST_SIM_AUTH:
                case WifiMonitor.ASSOCIATION_REJECTION_EVENT:
                case WifiMonitor.AP_DISCONNECTION_EVENT:
                case 196612:
                case 196613:
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DISCARD;
                    return true;
                case WifiStateMachine.CMD_BLUETOOTH_ADAPTER_STATE_CHANGE:
                    WifiStateMachine.this.mBluetoothConnectionActive = message.arg1 != 0;
                    return true;
                case WifiStateMachine.CMD_PING_SUPPLICANT:
                case WifiStateMachine.CMD_ADD_OR_UPDATE_NETWORK:
                case WifiStateMachine.CMD_REMOVE_NETWORK:
                case WifiStateMachine.CMD_ENABLE_NETWORK:
                case WifiStateMachine.CMD_SAVE_CONFIG:
                    WifiStateMachine.this.replyToMessage(message, message.what, -1);
                    return true;
                case WifiStateMachine.CMD_GET_CONFIGURED_NETWORKS:
                    WifiStateMachine.this.replyToMessage(message, message.what, (List) null);
                    return true;
                case WifiStateMachine.CMD_GET_CAPABILITY_FREQ:
                    WifiStateMachine.this.replyToMessage(message, message.what, (Object) null);
                    return true;
                case WifiStateMachine.CMD_GET_SUPPORTED_FEATURES:
                    if (!WifiNative.startHal()) {
                        WifiStateMachine.this.replyToMessage(message, message.what, 0);
                    } else {
                        int featureSet = WifiNative.getSupportedFeatureSet();
                        WifiStateMachine.this.replyToMessage(message, message.what, featureSet);
                    }
                    return true;
                case WifiStateMachine.CMD_GET_PRIVILEGED_CONFIGURED_NETWORKS:
                    WifiStateMachine.this.replyToMessage(message, message.what, (List) null);
                    return true;
                case WifiStateMachine.CMD_GET_LINK_LAYER_STATS:
                    WifiStateMachine.this.replyToMessage(message, message.what, (Object) null);
                    return true;
                case WifiStateMachine.CMD_START_SCAN:
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DISCARD;
                    return true;
                case WifiStateMachine.CMD_GET_CONNECTION_STATISTICS:
                    WifiStateMachine.this.replyToMessage(message, message.what, WifiStateMachine.this.mWifiConnectionStatistics);
                    return true;
                case WifiStateMachine.CMD_SET_HIGH_PERF_MODE:
                    if (message.arg1 == 1) {
                        WifiStateMachine.this.setSuspendOptimizations(2, WifiStateMachine.DEBUG_PARSE);
                    } else {
                        WifiStateMachine.this.setSuspendOptimizations(2, true);
                    }
                    return true;
                case WifiStateMachine.CMD_ENABLE_RSSI_POLL:
                    WifiStateMachine wifiStateMachine = WifiStateMachine.this;
                    if (message.arg1 == 1) {
                        z = true;
                    }
                    wifiStateMachine.mEnableRssiPolling = z;
                    return true;
                case WifiStateMachine.CMD_SET_SUSPEND_OPT_ENABLED:
                    if (message.arg1 == 1) {
                        WifiStateMachine.this.mSuspendWakeLock.release();
                        WifiStateMachine.this.setSuspendOptimizations(4, true);
                    } else {
                        WifiStateMachine.this.setSuspendOptimizations(4, WifiStateMachine.DEBUG_PARSE);
                    }
                    return true;
                case WifiStateMachine.CMD_SCREEN_STATE_CHANGED:
                    WifiStateMachine.this.handleScreenStateChanged(message.arg1 != 0, WifiStateMachine.DEBUG_PARSE);
                    return true;
                case WifiStateMachine.CMD_BOOT_COMPLETED:
                    String countryCode = WifiStateMachine.this.mPersistedCountryCode;
                    if (!TextUtils.isEmpty(countryCode)) {
                        Settings.Global.putString(WifiStateMachine.this.mContext.getContentResolver(), "wifi_country_code", countryCode);
                        int sequenceNum = WifiStateMachine.this.mCountryCodeSequence.incrementAndGet();
                        WifiStateMachine.this.sendMessageAtFrontOfQueue(WifiStateMachine.CMD_SET_COUNTRY_CODE, sequenceNum, 0, countryCode);
                    }
                    WifiStateMachine.this.maybeRegisterNetworkFactory();
                    return true;
                case WifiStateMachine.CMD_SET_BATCHED_SCAN:
                    WifiStateMachine.this.recordBatchedScanSettings(message.arg1, message.arg2, (Bundle) message.obj);
                    return true;
                case WifiStateMachine.CMD_START_NEXT_BATCHED_SCAN:
                    WifiStateMachine.this.startNextBatchedScan();
                    return true;
                case WifiStateMachine.CMD_POLL_BATCHED_SCAN:
                    WifiStateMachine.this.handleBatchedScanPollRequest();
                    return true;
                case WifiStateMachine.CMD_IP_CONFIGURATION_SUCCESSFUL:
                case WifiStateMachine.CMD_IP_CONFIGURATION_LOST:
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DISCARD;
                    return true;
                case WifiStateMachine.CMD_UPDATE_LINKPROPERTIES:
                    WifiStateMachine.this.updateLinkProperties(WifiStateMachine.CMD_UPDATE_LINKPROPERTIES);
                    return true;
                case WifiP2pServiceImpl.P2P_CONNECTION_CHANGED:
                    NetworkInfo info = (NetworkInfo) message.obj;
                    WifiStateMachine.this.mP2pConnected.set(info.isConnected());
                    return true;
                case WifiP2pServiceImpl.DISCONNECT_WIFI_REQUEST:
                    WifiStateMachine wifiStateMachine2 = WifiStateMachine.this;
                    if (message.arg1 == 1) {
                        z = true;
                    }
                    wifiStateMachine2.mTemporarilyDisconnectWifi = z;
                    WifiStateMachine.this.replyToMessage(message, WifiP2pServiceImpl.DISCONNECT_WIFI_RESPONSE);
                    return true;
                case WifiMonitor.DRIVER_HUNG_EVENT:
                    WifiStateMachine.this.setSupplicantRunning(WifiStateMachine.DEBUG_PARSE);
                    WifiStateMachine.this.setSupplicantRunning(true);
                    return true;
                case 151553:
                    WifiStateMachine.this.replyToMessage(message, 151554, 2);
                    return true;
                case 151556:
                    WifiStateMachine.this.replyToMessage(message, 151557, 2);
                    return true;
                case 151559:
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_FAIL;
                    WifiStateMachine.this.replyToMessage(message, 151560, 2);
                    return true;
                case 151562:
                    WifiStateMachine.this.replyToMessage(message, 151564, 2);
                    return true;
                case 151566:
                    WifiStateMachine.this.replyToMessage(message, 151567, 2);
                    return true;
                case 151569:
                    WifiStateMachine.this.replyToMessage(message, 151570, 2);
                    return true;
                case 151572:
                    WifiStateMachine.this.replyToMessage(message, 151574, 2);
                    return true;
                case 196614:
                    WifiStateMachine.this.mDhcpStateMachine = null;
                    return true;
                default:
                    WifiStateMachine.this.loge("Error! unhandled message" + message);
                    return true;
            }
        }
    }

    class InitialState extends State {
        InitialState() {
        }

        public void enter() {
            WifiNative unused = WifiStateMachine.this.mWifiNative;
            WifiNative.unloadDriver();
            if (WifiStateMachine.this.mWifiP2pChannel == null) {
                WifiStateMachine.this.mWifiP2pChannel = new AsyncChannel();
                WifiStateMachine.this.mWifiP2pChannel.connect(WifiStateMachine.this.mContext, WifiStateMachine.this.getHandler(), WifiStateMachine.this.mWifiP2pServiceImpl.getP2pStateMachineMessenger());
            }
            if (WifiStateMachine.this.mWifiApConfigChannel == null) {
                WifiStateMachine.this.mWifiApConfigChannel = new AsyncChannel();
                WifiApConfigStore wifiApConfigStore = WifiApConfigStore.makeWifiApConfigStore(WifiStateMachine.this.mContext, WifiStateMachine.this.getHandler());
                wifiApConfigStore.loadApConfiguration();
                WifiStateMachine.this.mWifiApConfigChannel.connectSync(WifiStateMachine.this.mContext, WifiStateMachine.this.getHandler(), wifiApConfigStore.getMessenger());
            }
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, getClass().getSimpleName());
            switch (message.what) {
                case WifiStateMachine.CMD_START_SUPPLICANT:
                    WifiNative unused = WifiStateMachine.this.mWifiNative;
                    if (WifiNative.loadDriver()) {
                        try {
                            WifiStateMachine.this.mNwService.wifiFirmwareReload(WifiStateMachine.this.mInterfaceName, "STA");
                        } catch (Exception e) {
                            WifiStateMachine.this.loge("Failed to reload STA firmware " + e);
                        }
                        try {
                            WifiStateMachine.this.mNwService.setInterfaceDown(WifiStateMachine.this.mInterfaceName);
                            WifiStateMachine.this.mNwService.clearInterfaceAddresses(WifiStateMachine.this.mInterfaceName);
                            WifiStateMachine.this.mNwService.setInterfaceIpv6PrivacyExtensions(WifiStateMachine.this.mInterfaceName, true);
                            WifiStateMachine.this.mNwService.disableIpv6(WifiStateMachine.this.mInterfaceName);
                        } catch (RemoteException re) {
                            WifiStateMachine.this.loge("Unable to change interface settings: " + re);
                        } catch (IllegalStateException ie) {
                            WifiStateMachine.this.loge("Unable to change interface settings: " + ie);
                        }
                        WifiStateMachine.this.mWifiMonitor.killSupplicant(WifiStateMachine.this.mP2pSupported ? 2 : 0);
                        WifiStateMachine.this.mWifiMonitor.killSupplicant(1);
                        WifiNative unused2 = WifiStateMachine.this.mWifiNative;
                        if (WifiNative.startSupplicant(WifiStateMachine.this.mP2pSupported ? 2 : 0)) {
                            WifiStateMachine.this.setWifiState(2);
                            if (WifiStateMachine.DBG) {
                                WifiStateMachine.this.log("Supplicant start successful");
                            }
                            WifiStateMachine.this.mWifiMonitor.startMonitoring();
                            WifiStateMachine.this.transitionTo(WifiStateMachine.this.mSupplicantStartingState);
                        } else {
                            WifiStateMachine.this.loge("Failed to start supplicant!");
                            WifiStateMachine.this.setWifiState(1);
                        }
                    } else {
                        WifiStateMachine.this.loge("Failed to load driver");
                        WifiStateMachine.this.setWifiState(1);
                    }
                    break;
                case WifiStateMachine.CMD_START_AP:
                    WifiNative unused3 = WifiStateMachine.this.mWifiNative;
                    if (WifiNative.loadDriver()) {
                        WifiStateMachine.this.setWifiApState(12);
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mSoftApStartingState);
                        return WifiStateMachine.DEBUG_PARSE;
                    }
                    WifiStateMachine.this.loge("Failed to load driver for softap");
                    return WifiStateMachine.DEBUG_PARSE;
                case WifiStateMachine.CMD_SET_COUNTRY_CODE:
                    String country = (String) message.obj;
                    boolean persist = message.arg2 == 1;
                    int sequence = message.arg1;
                    if (sequence != WifiStateMachine.this.mCountryCodeSequence.get()) {
                        if (WifiStateMachine.DBG) {
                            WifiStateMachine.this.log("set country code ignored due to sequence num in initial state");
                        }
                    } else {
                        WifiStateMachine.this.loge("process set country code cmd in initialstate " + country);
                        if (persist) {
                            WifiStateMachine.this.loge("set persist country code in initial state");
                            WifiStateMachine.this.mPersistedCountryCode = country;
                            Settings.Global.putString(WifiStateMachine.this.mContext.getContentResolver(), "wifi_country_code", country);
                        }
                    }
                    break;
                default:
                    return WifiStateMachine.DEBUG_PARSE;
            }
            return true;
        }
    }

    class SupplicantStartingState extends State {
        SupplicantStartingState() {
        }

        private void initializeWpsDetails() {
            String detail = SystemProperties.get("ro.product.name", "");
            if (!WifiStateMachine.this.mWifiNative.setDeviceName(detail)) {
                WifiStateMachine.this.loge("Failed to set device name " + detail);
            }
            String detail2 = SystemProperties.get("ro.product.manufacturer", "");
            if (!WifiStateMachine.this.mWifiNative.setManufacturer(detail2)) {
                WifiStateMachine.this.loge("Failed to set manufacturer " + detail2);
            }
            String detail3 = SystemProperties.get("ro.product.model", "");
            if (!WifiStateMachine.this.mWifiNative.setModelName(detail3)) {
                WifiStateMachine.this.loge("Failed to set model name " + detail3);
            }
            String detail4 = SystemProperties.get("ro.product.model", "");
            if (!WifiStateMachine.this.mWifiNative.setModelNumber(detail4)) {
                WifiStateMachine.this.loge("Failed to set model number " + detail4);
            }
            String detail5 = SystemProperties.get("ro.serialno", "");
            if (!WifiStateMachine.this.mWifiNative.setSerialNumber(detail5)) {
                WifiStateMachine.this.loge("Failed to set serial number " + detail5);
            }
            if (!WifiStateMachine.this.mWifiNative.setConfigMethods("physical_display virtual_push_button")) {
                WifiStateMachine.this.loge("Failed to set WPS config methods");
            }
            if (!WifiStateMachine.this.mWifiNative.setDeviceType(WifiStateMachine.this.mPrimaryDeviceType)) {
                WifiStateMachine.this.loge("Failed to set primary device type " + WifiStateMachine.this.mPrimaryDeviceType);
            }
        }

        public boolean processMessage(Message message) throws Throwable {
            WifiStateMachine.this.logStateAndMessage(message, getClass().getSimpleName());
            switch (message.what) {
                case WifiStateMachine.CMD_START_SUPPLICANT:
                case WifiStateMachine.CMD_STOP_SUPPLICANT:
                case WifiStateMachine.CMD_START_DRIVER:
                case WifiStateMachine.CMD_STOP_DRIVER:
                case WifiStateMachine.CMD_START_AP:
                case WifiStateMachine.CMD_STOP_AP:
                case WifiStateMachine.CMD_SET_OPERATIONAL_MODE:
                case WifiStateMachine.CMD_SET_COUNTRY_CODE:
                case WifiStateMachine.CMD_START_PACKET_FILTERING:
                case WifiStateMachine.CMD_STOP_PACKET_FILTERING:
                case WifiStateMachine.CMD_SET_FREQUENCY_BAND:
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DEFERRED;
                    WifiStateMachine.this.deferMessage(message);
                    break;
                case WifiMonitor.SUP_CONNECTION_EVENT:
                    if (WifiStateMachine.DBG) {
                        WifiStateMachine.this.log("Supplicant connection established");
                    }
                    WifiStateMachine.this.setWifiState(3);
                    WifiStateMachine.this.mSupplicantRestartCount = 0;
                    WifiStateMachine.this.mSupplicantStateTracker.sendMessage(WifiStateMachine.CMD_RESET_SUPPLICANT_STATE);
                    WifiStateMachine.this.mLastBssid = null;
                    WifiStateMachine.this.mLastNetworkId = -1;
                    WifiStateMachine.this.mLastSignalLevel = -1;
                    WifiStateMachine.this.mWifiInfo.setMacAddress(WifiStateMachine.this.mWifiNative.getMacAddress());
                    WifiStateMachine.this.mWifiNative.enableSaveConfig();
                    WifiStateMachine.this.mWifiConfigStore.loadAndEnableAllNetworks();
                    if (WifiStateMachine.this.mWifiConfigStore.enableVerboseLogging > 0) {
                        WifiStateMachine.this.enableVerboseLogging(WifiStateMachine.this.mWifiConfigStore.enableVerboseLogging);
                    }
                    if (WifiStateMachine.this.mWifiConfigStore.associatedPartialScanPeriodMilli < 0) {
                        WifiStateMachine.this.mWifiConfigStore.associatedPartialScanPeriodMilli = 0;
                    }
                    initializeWpsDetails();
                    WifiStateMachine.this.sendSupplicantConnectionChangedBroadcast(true);
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDriverStartedState);
                    break;
                case WifiMonitor.SUP_DISCONNECTION_EVENT:
                    if (WifiStateMachine.access$4904(WifiStateMachine.this) > 5) {
                        WifiStateMachine.this.loge("Failed " + WifiStateMachine.this.mSupplicantRestartCount + " times to start supplicant, unload driver");
                        WifiStateMachine.this.mSupplicantRestartCount = 0;
                        WifiStateMachine.this.setWifiState(4);
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mInitialState);
                    } else {
                        WifiStateMachine.this.loge("Failed to setup control channel, restart supplicant");
                        WifiStateMachine.this.handleSupplicantConnectionLoss();
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mInitialState);
                        WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.CMD_START_SUPPLICANT, 5000L);
                    }
                    break;
                case 196617:
                    if (message.arg1 == 1) {
                        if (WifiStateMachine.DBG) {
                            WifiStateMachine.this.log("DHCP successful");
                        }
                        WifiStateMachine.this.configureIPv6LinkProperties((DhcpResults) message.obj, 196617);
                        WifiStateMachine.this.sendLinkConfigurationChangedBroadcast();
                    } else {
                        WifiStateMachine.this.loge("Failed to get IPv6 info");
                    }
                    break;
                default:
                    return WifiStateMachine.DEBUG_PARSE;
            }
            return true;
        }
    }

    class SupplicantStartedState extends State {
        SupplicantStartedState() {
        }

        public void enter() {
            WifiStateMachine.this.mNetworkInfo.setIsAvailable(true);
            if (WifiStateMachine.this.mNetworkAgent != null) {
                WifiStateMachine.this.mNetworkAgent.sendNetworkInfo(WifiStateMachine.this.mNetworkInfo);
            }
            int defaultInterval = WifiStateMachine.this.mContext.getResources().getInteger(R.integer.config_autoBrightnessShortTermModelTimeout);
            WifiStateMachine.this.mSupplicantScanIntervalMs = Settings.Global.getLong(WifiStateMachine.this.mContext.getContentResolver(), "wifi_supplicant_scan_interval_ms", defaultInterval);
            WifiStateMachine.this.mWifiNative.setScanInterval(((int) WifiStateMachine.this.mSupplicantScanIntervalMs) / 1000);
            WifiStateMachine.this.mWifiNative.setExternalSim(true);
            WifiStateMachine.this.setRandomMacOui();
            WifiStateMachine.this.mWifiNative.enableAutoConnect(WifiStateMachine.DEBUG_PARSE);
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, getClass().getSimpleName());
            switch (message.what) {
                case WifiStateMachine.CMD_STOP_SUPPLICANT:
                    if (WifiStateMachine.this.mP2pSupported) {
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mWaitForP2pDisableState);
                        return true;
                    }
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mSupplicantStoppingState);
                    return true;
                case WifiStateMachine.CMD_START_AP:
                    WifiStateMachine.this.loge("Failed to start soft AP with a running supplicant");
                    WifiStateMachine.this.setWifiApState(14);
                    return true;
                case WifiStateMachine.CMD_PING_SUPPLICANT:
                    boolean ok = WifiStateMachine.this.mWifiNative.ping();
                    WifiStateMachine.this.replyToMessage(message, message.what, ok ? 1 : -1);
                    return true;
                case WifiStateMachine.CMD_GET_CAPABILITY_FREQ:
                    String freqs = WifiStateMachine.this.mWifiNative.getFreqCapability();
                    WifiStateMachine.this.replyToMessage(message, message.what, freqs);
                    return true;
                case WifiStateMachine.CMD_GET_LINK_LAYER_STATS:
                    WifiLinkLayerStats stats = WifiStateMachine.this.getWifiLinkLayerStats(WifiStateMachine.DBG);
                    if (stats == null) {
                        stats = new WifiLinkLayerStats();
                    }
                    WifiStateMachine.this.replyToMessage(message, message.what, stats);
                    return true;
                case WifiStateMachine.CMD_SET_OPERATIONAL_MODE:
                    WifiStateMachine.this.mOperationalMode = message.arg1;
                    WifiStateMachine.this.mWifiConfigStore.setLastSelectedConfiguration(-1);
                    return true;
                case WifiStateMachine.CMD_TARGET_BSSID:
                    if (message.obj == null) {
                        return true;
                    }
                    WifiStateMachine.this.mTargetRoamBSSID = (String) message.obj;
                    return true;
                case WifiMonitor.SUP_DISCONNECTION_EVENT:
                    WifiStateMachine.this.loge("Connection lost, restart supplicant");
                    WifiStateMachine.this.handleSupplicantConnectionLoss();
                    WifiStateMachine.this.handleNetworkDisconnect();
                    WifiStateMachine.this.mSupplicantStateTracker.sendMessage(WifiStateMachine.CMD_RESET_SUPPLICANT_STATE);
                    if (WifiStateMachine.this.mP2pSupported) {
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mWaitForP2pDisableState);
                    } else {
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mInitialState);
                    }
                    WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.CMD_START_SUPPLICANT, 5000L);
                    return true;
                case WifiMonitor.SCAN_RESULTS_EVENT:
                    WifiStateMachine.this.maybeRegisterNetworkFactory();
                    WifiStateMachine.this.closeRadioScanStats();
                    WifiStateMachine.this.noteScanEnd();
                    WifiStateMachine.this.setScanResults();
                    if (WifiStateMachine.this.mIsFullScanOngoing || WifiStateMachine.this.mSendScanResultsBroadcast) {
                        WifiStateMachine.this.sendScanResultsAvailableBroadcast();
                    }
                    WifiStateMachine.this.mSendScanResultsBroadcast = WifiStateMachine.DEBUG_PARSE;
                    WifiStateMachine.this.mIsScanOngoing = WifiStateMachine.DEBUG_PARSE;
                    WifiStateMachine.this.mIsFullScanOngoing = WifiStateMachine.DEBUG_PARSE;
                    if (WifiStateMachine.this.mBufferedScanMsg.size() <= 0) {
                        return true;
                    }
                    WifiStateMachine.this.sendMessage((Message) WifiStateMachine.this.mBufferedScanMsg.remove());
                    return true;
                default:
                    return WifiStateMachine.DEBUG_PARSE;
            }
        }

        public void exit() {
            WifiStateMachine.this.mNetworkInfo.setIsAvailable(WifiStateMachine.DEBUG_PARSE);
            if (WifiStateMachine.this.mNetworkAgent != null) {
                WifiStateMachine.this.mNetworkAgent.sendNetworkInfo(WifiStateMachine.this.mNetworkInfo);
            }
        }
    }

    class SupplicantStoppingState extends State {
        SupplicantStoppingState() {
        }

        public void enter() {
            WifiStateMachine.this.handleNetworkDisconnect();
            if (WifiStateMachine.this.mDhcpStateMachine != null) {
                WifiStateMachine.this.mDhcpStateMachine.doQuit();
            }
            String suppState = System.getProperty("init.svc.wpa_supplicant");
            if (suppState == null) {
                suppState = "unknown";
            }
            String p2pSuppState = System.getProperty("init.svc.p2p_supplicant");
            if (p2pSuppState == null) {
                p2pSuppState = "unknown";
            }
            WifiStateMachine.this.loge("SupplicantStoppingState: stopSupplicant  init.svc.wpa_supplicant=" + suppState + " init.svc.p2p_supplicant=" + p2pSuppState);
            WifiStateMachine.this.mWifiMonitor.stopSupplicant();
            WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.this.obtainMessage(WifiStateMachine.CMD_STOP_SUPPLICANT_FAILED, WifiStateMachine.access$8704(WifiStateMachine.this), 0), 5000L);
            WifiStateMachine.this.setWifiState(0);
            WifiStateMachine.this.mSupplicantStateTracker.sendMessage(WifiStateMachine.CMD_RESET_SUPPLICANT_STATE);
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, getClass().getSimpleName());
            switch (message.what) {
                case WifiStateMachine.CMD_START_SUPPLICANT:
                case WifiStateMachine.CMD_STOP_SUPPLICANT:
                case WifiStateMachine.CMD_START_DRIVER:
                case WifiStateMachine.CMD_STOP_DRIVER:
                case WifiStateMachine.CMD_START_AP:
                case WifiStateMachine.CMD_STOP_AP:
                case WifiStateMachine.CMD_SET_OPERATIONAL_MODE:
                case WifiStateMachine.CMD_SET_COUNTRY_CODE:
                case WifiStateMachine.CMD_START_PACKET_FILTERING:
                case WifiStateMachine.CMD_STOP_PACKET_FILTERING:
                case WifiStateMachine.CMD_SET_FREQUENCY_BAND:
                    WifiStateMachine.this.deferMessage(message);
                    return true;
                case WifiStateMachine.CMD_STOP_SUPPLICANT_FAILED:
                    if (message.arg1 == WifiStateMachine.this.mSupplicantStopFailureToken) {
                        WifiStateMachine.this.loge("Timed out on a supplicant stop, kill and proceed");
                        WifiStateMachine.this.handleSupplicantConnectionLoss();
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mInitialState);
                    }
                    return true;
                case WifiMonitor.SUP_CONNECTION_EVENT:
                    WifiStateMachine.this.loge("Supplicant connection received while stopping");
                    return true;
                case WifiMonitor.SUP_DISCONNECTION_EVENT:
                    if (WifiStateMachine.DBG) {
                        WifiStateMachine.this.log("Supplicant connection lost");
                    }
                    WifiStateMachine.this.handleSupplicantConnectionLoss();
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mInitialState);
                    return true;
                default:
                    return WifiStateMachine.DEBUG_PARSE;
            }
        }
    }

    class DriverStartingState extends State {
        private int mTries;

        DriverStartingState() {
        }

        public void enter() {
            this.mTries = 1;
            WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.this.obtainMessage(WifiStateMachine.CMD_DRIVER_START_TIMED_OUT, WifiStateMachine.access$9104(WifiStateMachine.this), 0), 10000L);
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, getClass().getSimpleName());
            switch (message.what) {
                case WifiStateMachine.CMD_START_DRIVER:
                case WifiStateMachine.CMD_STOP_DRIVER:
                case WifiStateMachine.CMD_START_SCAN:
                case WifiStateMachine.CMD_DISCONNECT:
                case WifiStateMachine.CMD_RECONNECT:
                case WifiStateMachine.CMD_REASSOCIATE:
                case WifiStateMachine.CMD_SET_COUNTRY_CODE:
                case WifiStateMachine.CMD_START_PACKET_FILTERING:
                case WifiStateMachine.CMD_STOP_PACKET_FILTERING:
                case WifiStateMachine.CMD_SET_FREQUENCY_BAND:
                case WifiMonitor.NETWORK_CONNECTION_EVENT:
                case WifiMonitor.NETWORK_DISCONNECTION_EVENT:
                case WifiMonitor.AUTHENTICATION_FAILURE_EVENT:
                case WifiMonitor.WPS_OVERLAP_EVENT:
                case WifiMonitor.ASSOCIATION_REJECTION_EVENT:
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DEFERRED;
                    WifiStateMachine.this.deferMessage(message);
                    return true;
                case WifiStateMachine.CMD_DRIVER_START_TIMED_OUT:
                    if (message.arg1 == WifiStateMachine.this.mDriverStartToken) {
                        if (this.mTries >= 2) {
                            WifiStateMachine.this.loge("Failed to start driver after " + this.mTries);
                            WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDriverStoppedState);
                        } else {
                            WifiStateMachine.this.loge("Driver start failed, retrying");
                            WifiStateMachine.this.mWakeLock.acquire();
                            WifiStateMachine.this.mWifiNative.startDriver();
                            WifiStateMachine.this.mWakeLock.release();
                            this.mTries++;
                            WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.this.obtainMessage(WifiStateMachine.CMD_DRIVER_START_TIMED_OUT, WifiStateMachine.access$9104(WifiStateMachine.this), 0), 10000L);
                        }
                    }
                    return true;
                case WifiMonitor.SCAN_RESULTS_EVENT:
                    return true;
                case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT:
                    SupplicantState state = WifiStateMachine.this.handleSupplicantStateChange(message);
                    if (SupplicantState.isDriverActive(state)) {
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDriverStartedState);
                    }
                    return true;
                default:
                    return WifiStateMachine.DEBUG_PARSE;
            }
        }
    }

    class DriverStartedState extends State {
        DriverStartedState() {
        }

        public void enter() {
            boolean z = WifiStateMachine.DEBUG_PARSE;
            if (WifiStateMachine.PDBG) {
                WifiStateMachine.this.loge("DriverStartedState enter");
            }
            WifiStateMachine.this.mIsRunning = true;
            WifiStateMachine.this.mInDelayedStop = WifiStateMachine.DEBUG_PARSE;
            WifiStateMachine.access$10108(WifiStateMachine.this);
            WifiStateMachine.this.updateBatteryWorkSource(null);
            Settings.System.putInt(WifiStateMachine.this.mContext.getContentResolver(), "skip_sta_scan_in_p2p_ui", 0);
            WifiStateMachine.this.mWifiNative.setBluetoothCoexistenceScanMode(WifiStateMachine.this.mBluetoothConnectionActive);
            WifiStateMachine.this.setCountryCode();
            WifiStateMachine.this.setFrequencyBand();
            WifiStateMachine.this.setNetworkDetailedState(NetworkInfo.DetailedState.DISCONNECTED);
            WifiStateMachine.this.mWifiNative.stopFilteringMulticastV6Packets();
            if (WifiStateMachine.this.mFilteringMulticastV4Packets.get()) {
                WifiStateMachine.this.mWifiNative.startFilteringMulticastV4Packets();
            } else {
                WifiStateMachine.this.mWifiNative.stopFilteringMulticastV4Packets();
            }
            WifiStateMachine.this.mDhcpActive = WifiStateMachine.DEBUG_PARSE;
            WifiStateMachine.this.startBatchedScan();
            if (WifiStateMachine.this.mOperationalMode != 1) {
                WifiStateMachine.this.mWifiNative.disconnect();
                WifiStateMachine.this.mWifiConfigStore.disableAllNetworks();
                if (WifiStateMachine.this.mOperationalMode == 3) {
                    WifiStateMachine.this.setWifiState(1);
                }
                WifiStateMachine.this.transitionTo(WifiStateMachine.this.mScanModeState);
            } else {
                WifiStateMachine.this.mWifiNative.status();
                WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectedState);
            }
            if (!WifiStateMachine.this.mScreenBroadcastReceived.get()) {
                PowerManager powerManager = (PowerManager) WifiStateMachine.this.mContext.getSystemService("power");
                WifiStateMachine.this.handleScreenStateChanged(powerManager.isScreenOn(), WifiStateMachine.DEBUG_PARSE);
            } else {
                WifiNative wifiNative = WifiStateMachine.this.mWifiNative;
                if (WifiStateMachine.this.mSuspendOptNeedsDisabled == 0 && WifiStateMachine.this.mUserWantsSuspendOpt.get()) {
                    z = true;
                }
                wifiNative.setSuspendOptimizations(z);
            }
            WifiStateMachine.this.mWifiNative.setPowerSave(true);
            if (WifiStateMachine.this.mP2pSupported && WifiStateMachine.this.mOperationalMode == 1) {
                WifiStateMachine.this.mWifiP2pChannel.sendMessage(WifiStateMachine.CMD_ENABLE_P2P);
            }
            Intent intent = new Intent("wifi_scan_available");
            intent.addFlags(67108864);
            intent.putExtra("scan_enabled", 3);
            WifiStateMachine.this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
            if (WifiStateMachine.PDBG) {
                WifiStateMachine.this.loge("Driverstarted State enter done");
            }
        }

        public boolean processMessage(Message message) {
            boolean persist = WifiStateMachine.DEBUG_PARSE;
            WifiStateMachine.this.logStateAndMessage(message, getClass().getSimpleName());
            switch (message.what) {
                case WifiStateMachine.CMD_START_DRIVER:
                    if (WifiStateMachine.this.mInDelayedStop) {
                        WifiStateMachine.this.mInDelayedStop = WifiStateMachine.DEBUG_PARSE;
                        WifiStateMachine.access$10108(WifiStateMachine.this);
                        WifiStateMachine.this.mAlarmManager.cancel(WifiStateMachine.this.mDriverStopIntent);
                        if (WifiStateMachine.DBG) {
                            WifiStateMachine.this.log("Delayed stop ignored due to start");
                        }
                        if (WifiStateMachine.this.mOperationalMode == 1) {
                            WifiStateMachine.this.mWifiConfigStore.enableAllNetworks();
                        }
                    }
                    break;
                case WifiStateMachine.CMD_STOP_DRIVER:
                    int i = message.arg1;
                    if (WifiStateMachine.this.mInDelayedStop) {
                        if (WifiStateMachine.DBG) {
                            WifiStateMachine.this.log("Already in delayed stop");
                        }
                    } else {
                        WifiStateMachine.this.mWifiConfigStore.disableAllNetworks();
                        WifiStateMachine.this.mInDelayedStop = true;
                        WifiStateMachine.access$10108(WifiStateMachine.this);
                        if (WifiStateMachine.DBG) {
                            WifiStateMachine.this.log("Delayed stop message " + WifiStateMachine.this.mDelayedStopCounter);
                        }
                        Intent driverStopIntent = new Intent(WifiStateMachine.ACTION_DELAYED_DRIVER_STOP, (Uri) null);
                        driverStopIntent.setPackage(getClass().getPackage().getName());
                        driverStopIntent.putExtra(WifiStateMachine.DELAYED_STOP_COUNTER, WifiStateMachine.this.mDelayedStopCounter);
                        WifiStateMachine.this.mDriverStopIntent = PendingIntent.getBroadcast(WifiStateMachine.this.mContext, 0, driverStopIntent, 134217728);
                        WifiStateMachine.this.mAlarmManager.set(0, System.currentTimeMillis() + ((long) WifiStateMachine.this.mDriverStopDelayMs), WifiStateMachine.this.mDriverStopIntent);
                    }
                    break;
                case WifiStateMachine.CMD_DELAYED_STOP_DRIVER:
                    if (WifiStateMachine.DBG) {
                        WifiStateMachine.this.log("delayed stop " + message.arg1 + " " + WifiStateMachine.this.mDelayedStopCounter);
                    }
                    if (message.arg1 == WifiStateMachine.this.mDelayedStopCounter) {
                        if (WifiStateMachine.this.getCurrentState() != WifiStateMachine.this.mDisconnectedState) {
                            WifiStateMachine.this.mWifiNative.disconnect();
                            WifiStateMachine.this.handleNetworkDisconnect();
                        }
                        WifiStateMachine.this.mWakeLock.acquire();
                        WifiStateMachine.this.mWifiNative.stopDriver();
                        WifiStateMachine.this.mWakeLock.release();
                        if (WifiStateMachine.this.mP2pSupported) {
                            WifiStateMachine.this.transitionTo(WifiStateMachine.this.mWaitForP2pDisableState);
                        } else {
                            WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDriverStoppingState);
                        }
                    }
                    break;
                case WifiStateMachine.CMD_BLUETOOTH_ADAPTER_STATE_CHANGE:
                    WifiStateMachine wifiStateMachine = WifiStateMachine.this;
                    if (message.arg1 != 0) {
                        persist = true;
                    }
                    wifiStateMachine.mBluetoothConnectionActive = persist;
                    WifiStateMachine.this.mWifiNative.setBluetoothCoexistenceScanMode(WifiStateMachine.this.mBluetoothConnectionActive);
                    break;
                case WifiStateMachine.CMD_START_SCAN:
                    if (Settings.System.getInt(WifiStateMachine.this.mContext.getContentResolver(), "skip_sta_scan_in_p2p_ui", 0) != 1) {
                        WifiStateMachine.this.handleScanRequest(1, message);
                    } else {
                        WifiStateMachine.this.log("Skip WIFI STA scan in P2P UI.");
                    }
                    break;
                case WifiStateMachine.CMD_SET_HIGH_PERF_MODE:
                    if (message.arg1 == 1) {
                        if (!WifiStateMachine.this.mWifiNative.setSleepPeriodCommand(20)) {
                            WifiStateMachine.this.loge("set sleep peroid failed!");
                        }
                    } else if (!WifiStateMachine.this.mWifiNative.setSleepPeriodCommand(0)) {
                        WifiStateMachine.this.loge("cancel sleep peroid failed!");
                    }
                    break;
                case WifiStateMachine.CMD_SET_COUNTRY_CODE:
                    String country = (String) message.obj;
                    if (message.arg2 == 1) {
                        persist = true;
                    }
                    int sequence = message.arg1;
                    if (sequence != WifiStateMachine.this.mCountryCodeSequence.get()) {
                        if (WifiStateMachine.DBG) {
                            WifiStateMachine.this.log("set country code ignored due to sequnce num");
                        }
                    } else {
                        if (WifiStateMachine.DBG) {
                            WifiStateMachine.this.log("set country code " + country);
                        }
                        if (persist) {
                            WifiStateMachine.this.mPersistedCountryCode = country;
                            Settings.Global.putString(WifiStateMachine.this.mContext.getContentResolver(), "wifi_country_code", country);
                        }
                        String country2 = country.toUpperCase(Locale.ROOT);
                        if (WifiStateMachine.this.mLastSetCountryCode == null || !country2.equals(WifiStateMachine.this.mLastSetCountryCode)) {
                            if (WifiStateMachine.this.mWifiNative.setCountryCode(country2)) {
                                WifiStateMachine.this.mLastSetCountryCode = country2;
                            } else {
                                WifiStateMachine.this.loge("Failed to set country code " + country2);
                            }
                        }
                        WifiStateMachine.this.mWifiP2pChannel.sendMessage(WifiP2pServiceImpl.SET_COUNTRY_CODE, country2);
                    }
                    break;
                case WifiStateMachine.CMD_START_PACKET_FILTERING:
                    if (message.arg1 == 1) {
                        WifiStateMachine.this.mWifiNative.startFilteringMulticastV6Packets();
                    } else if (message.arg1 == 0) {
                        WifiStateMachine.this.mWifiNative.startFilteringMulticastV4Packets();
                    } else {
                        WifiStateMachine.this.loge("Illegal arugments to CMD_START_PACKET_FILTERING");
                    }
                    break;
                case WifiStateMachine.CMD_STOP_PACKET_FILTERING:
                    if (message.arg1 == 1) {
                        WifiStateMachine.this.mWifiNative.stopFilteringMulticastV6Packets();
                    } else if (message.arg1 == 0) {
                        WifiStateMachine.this.mWifiNative.stopFilteringMulticastV4Packets();
                    } else {
                        WifiStateMachine.this.loge("Illegal arugments to CMD_STOP_PACKET_FILTERING");
                    }
                    break;
                case WifiStateMachine.CMD_SET_SUSPEND_OPT_ENABLED:
                    if (message.arg1 == 1) {
                        WifiStateMachine.this.setSuspendOptimizationsNative(4, true);
                        WifiStateMachine.this.mSuspendWakeLock.release();
                    } else {
                        WifiStateMachine.this.setSuspendOptimizationsNative(4, WifiStateMachine.DEBUG_PARSE);
                    }
                    break;
                case WifiStateMachine.CMD_SET_FREQUENCY_BAND:
                    int band = message.arg1;
                    if (WifiStateMachine.DBG) {
                        WifiStateMachine.this.log("set frequency band " + band);
                    }
                    if (WifiStateMachine.this.mWifiNative.setBand(band)) {
                        if (WifiStateMachine.PDBG) {
                            WifiStateMachine.this.loge("did set frequency band " + band);
                        }
                        WifiStateMachine.this.mFrequencyBand.set(band);
                        WifiStateMachine.this.mWifiNative.bssFlush();
                        synchronized (WifiStateMachine.this.mScanResultCache) {
                            WifiStateMachine.this.mScanResultCache.evictAll();
                            break;
                        }
                        WifiStateMachine.this.startScanNative(1, null);
                        WifiStateMachine.this.disconnectCommand();
                        if (WifiStateMachine.PDBG) {
                            WifiStateMachine.this.loge("done set frequency band " + band);
                        }
                    } else {
                        WifiStateMachine.this.loge("Failed to set frequency band " + band);
                    }
                    break;
                case WifiStateMachine.CMD_ENABLE_TDLS:
                    if (message.obj != null) {
                        String remoteAddress = (String) message.obj;
                        boolean enable = message.arg1 == 1;
                        WifiStateMachine.this.mWifiNative.startTdls(remoteAddress, enable);
                    }
                    break;
                case WifiStateMachine.CMD_SET_BATCHED_SCAN:
                    if (WifiStateMachine.this.recordBatchedScanSettings(message.arg1, message.arg2, (Bundle) message.obj)) {
                        if (WifiStateMachine.this.mBatchedScanSettings != null) {
                            WifiStateMachine.this.startBatchedScan();
                        } else {
                            WifiStateMachine.this.stopBatchedScan();
                        }
                    }
                    break;
                default:
                    return WifiStateMachine.DEBUG_PARSE;
            }
            return true;
        }

        public void exit() {
            WifiStateMachine.this.mIsRunning = WifiStateMachine.DEBUG_PARSE;
            WifiStateMachine.this.updateBatteryWorkSource(null);
            WifiStateMachine.this.mScanResults = new ArrayList();
            WifiStateMachine.this.stopBatchedScan();
            Intent intent = new Intent("wifi_scan_available");
            intent.addFlags(67108864);
            intent.putExtra("scan_enabled", 1);
            WifiStateMachine.this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
            WifiStateMachine.this.noteScanEnd();
            WifiStateMachine.this.mBufferedScanMsg.clear();
            WifiStateMachine.this.mLastSetCountryCode = null;
        }
    }

    class WaitForP2pDisableState extends State {
        private State mTransitionToState;

        WaitForP2pDisableState() {
        }

        public void enter() {
            switch (WifiStateMachine.this.getCurrentMessage().what) {
                case WifiStateMachine.CMD_STOP_SUPPLICANT:
                    this.mTransitionToState = WifiStateMachine.this.mSupplicantStoppingState;
                    break;
                case WifiStateMachine.CMD_DELAYED_STOP_DRIVER:
                    this.mTransitionToState = WifiStateMachine.this.mDriverStoppingState;
                    break;
                case WifiMonitor.SUP_DISCONNECTION_EVENT:
                    this.mTransitionToState = WifiStateMachine.this.mInitialState;
                    break;
                default:
                    this.mTransitionToState = WifiStateMachine.this.mDriverStoppingState;
                    break;
            }
            WifiStateMachine.this.mWifiP2pChannel.sendMessage(WifiStateMachine.CMD_DISABLE_P2P_REQ);
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, getClass().getSimpleName());
            switch (message.what) {
                case WifiStateMachine.CMD_START_SUPPLICANT:
                case WifiStateMachine.CMD_STOP_SUPPLICANT:
                case WifiStateMachine.CMD_START_DRIVER:
                case WifiStateMachine.CMD_STOP_DRIVER:
                case WifiStateMachine.CMD_START_AP:
                case WifiStateMachine.CMD_STOP_AP:
                case WifiStateMachine.CMD_START_SCAN:
                case WifiStateMachine.CMD_SET_OPERATIONAL_MODE:
                case WifiStateMachine.CMD_DISCONNECT:
                case WifiStateMachine.CMD_RECONNECT:
                case WifiStateMachine.CMD_REASSOCIATE:
                case WifiStateMachine.CMD_SET_COUNTRY_CODE:
                case WifiStateMachine.CMD_START_PACKET_FILTERING:
                case WifiStateMachine.CMD_STOP_PACKET_FILTERING:
                case WifiStateMachine.CMD_SET_FREQUENCY_BAND:
                case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT:
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DEFERRED;
                    WifiStateMachine.this.deferMessage(message);
                    return true;
                case WifiStateMachine.CMD_DISABLE_P2P_RSP:
                    WifiStateMachine.this.transitionTo(this.mTransitionToState);
                    return true;
                default:
                    return WifiStateMachine.DEBUG_PARSE;
            }
        }
    }

    class DriverStoppingState extends State {
        DriverStoppingState() {
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, getClass().getSimpleName());
            switch (message.what) {
                case WifiStateMachine.CMD_START_DRIVER:
                case WifiStateMachine.CMD_STOP_DRIVER:
                case WifiStateMachine.CMD_START_SCAN:
                case WifiStateMachine.CMD_DISCONNECT:
                case WifiStateMachine.CMD_RECONNECT:
                case WifiStateMachine.CMD_REASSOCIATE:
                case WifiStateMachine.CMD_SET_COUNTRY_CODE:
                case WifiStateMachine.CMD_START_PACKET_FILTERING:
                case WifiStateMachine.CMD_STOP_PACKET_FILTERING:
                case WifiStateMachine.CMD_SET_FREQUENCY_BAND:
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DEFERRED;
                    WifiStateMachine.this.deferMessage(message);
                    return true;
                case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT:
                    SupplicantState state = WifiStateMachine.this.handleSupplicantStateChange(message);
                    if (state == SupplicantState.INTERFACE_DISABLED) {
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDriverStoppedState);
                    }
                    return true;
                default:
                    return WifiStateMachine.DEBUG_PARSE;
            }
        }
    }

    class DriverStoppedState extends State {
        DriverStoppedState() {
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, getClass().getSimpleName());
            switch (message.what) {
                case WifiStateMachine.CMD_START_DRIVER:
                    WifiStateMachine.this.mWakeLock.acquire();
                    WifiStateMachine.this.mWifiNative.startDriver();
                    WifiStateMachine.this.mWakeLock.release();
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDriverStartingState);
                    return true;
                case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT:
                    StateChangeResult stateChangeResult = (StateChangeResult) message.obj;
                    SupplicantState state = stateChangeResult.state;
                    if (SupplicantState.isDriverActive(state)) {
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDriverStartedState);
                    }
                    return true;
                default:
                    return WifiStateMachine.DEBUG_PARSE;
            }
        }
    }

    class ScanModeState extends State {
        private int mLastOperationMode;

        ScanModeState() {
        }

        public void enter() {
            this.mLastOperationMode = WifiStateMachine.this.mOperationalMode;
        }

        public boolean processMessage(Message message) throws Throwable {
            WifiStateMachine.this.logStateAndMessage(message, getClass().getSimpleName());
            switch (message.what) {
                case WifiStateMachine.CMD_START_SCAN:
                    WifiStateMachine.this.handleScanRequest(1, message);
                    return true;
                case WifiStateMachine.CMD_SET_OPERATIONAL_MODE:
                    if (message.arg1 != 1) {
                        return true;
                    }
                    if (this.mLastOperationMode == 3) {
                        WifiStateMachine.this.setWifiState(3);
                        WifiStateMachine.this.mWifiConfigStore.loadAndEnableAllNetworks();
                        WifiStateMachine.this.mWifiP2pChannel.sendMessage(WifiStateMachine.CMD_ENABLE_P2P);
                    } else {
                        WifiStateMachine.this.mWifiConfigStore.enableAllNetworks();
                    }
                    if (!WifiStateMachine.this.mWifiAutoJoinController.attemptAutoJoin()) {
                        WifiStateMachine.this.startScan(WifiStateMachine.ENABLE_WIFI, 0, null, null);
                    }
                    WifiStateMachine.this.mWifiConfigStore.setLastSelectedConfiguration(-1);
                    WifiStateMachine.this.mOperationalMode = 1;
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectedState);
                    return true;
                default:
                    return WifiStateMachine.DEBUG_PARSE;
            }
        }
    }

    String smToString(Message message) {
        return smToString(message.what);
    }

    String smToString(int what) {
        switch (what) {
            case 1:
                return "DHCP_SUCCESS";
            case 2:
                return "DHCP_FAILURE";
            case 69632:
                return "AsyncChannel.CMD_CHANNEL_HALF_CONNECTED";
            case 69636:
                return "AsyncChannel.CMD_CHANNEL_DISCONNECTED";
            case CMD_START_SUPPLICANT:
                return "CMD_START_SUPPLICANT";
            case CMD_STOP_SUPPLICANT:
                return "CMD_STOP_SUPPLICANT";
            case CMD_START_DRIVER:
                return "CMD_START_DRIVER";
            case CMD_STOP_DRIVER:
                return "CMD_STOP_DRIVER";
            case CMD_STATIC_IP_SUCCESS:
                return "CMD_STATIC_IP_SUCCESSFUL";
            case CMD_STATIC_IP_FAILURE:
                return "CMD_STATIC_IP_FAILURE";
            case CMD_STOP_SUPPLICANT_FAILED:
                return "CMD_STOP_SUPPLICANT_FAILED";
            case CMD_REQUEST_AP_CONFIG:
                return "CMD_REQUEST_AP_CONFIG";
            case CMD_RESPONSE_AP_CONFIG:
                return "CMD_RESPONSE_AP_CONFIG";
            case CMD_TETHER_STATE_CHANGE:
                return "CMD_TETHER_STATE_CHANGE";
            case CMD_TETHER_NOTIFICATION_TIMED_OUT:
                return "CMD_TETHER_NOTIFICATION_TIMED_OUT";
            case CMD_BLUETOOTH_ADAPTER_STATE_CHANGE:
                return "CMD_BLUETOOTH_ADAPTER_STATE_CHANGE";
            case CMD_ADD_OR_UPDATE_NETWORK:
                return "CMD_ADD_OR_UPDATE_NETWORK";
            case CMD_REMOVE_NETWORK:
                return "CMD_REMOVE_NETWORK";
            case CMD_ENABLE_NETWORK:
                return "CMD_ENABLE_NETWORK";
            case CMD_ENABLE_ALL_NETWORKS:
                return "CMD_ENABLE_ALL_NETWORKS";
            case CMD_BLACKLIST_NETWORK:
                return "CMD_BLACKLIST_NETWORK";
            case CMD_CLEAR_BLACKLIST:
                return "CMD_CLEAR_BLACKLIST";
            case CMD_SAVE_CONFIG:
                return "CMD_SAVE_CONFIG";
            case CMD_GET_CONFIGURED_NETWORKS:
                return "CMD_GET_CONFIGURED_NETWORKS";
            case CMD_GET_SUPPORTED_FEATURES:
                return "CMD_GET_ADAPTORS";
            case CMD_GET_PRIVILEGED_CONFIGURED_NETWORKS:
                return "CMD_GET_PRIVILEGED_CONFIGURED_NETWORKS";
            case CMD_GET_LINK_LAYER_STATS:
                return "CMD_GET_LINK_LAYER_STATS";
            case CMD_START_SCAN:
                return "CMD_START_SCAN";
            case CMD_SET_OPERATIONAL_MODE:
                return "CMD_SET_OPERATIONAL_MODE";
            case CMD_DISCONNECT:
                return "CMD_DISCONNECT";
            case CMD_RECONNECT:
                return "CMD_RECONNECT";
            case CMD_REASSOCIATE:
                return "CMD_REASSOCIATE";
            case CMD_GET_CONNECTION_STATISTICS:
                return "CMD_GET_CONNECTION_STATISTICS";
            case CMD_SET_HIGH_PERF_MODE:
                return "CMD_SET_HIGH_PERF_MODE";
            case CMD_SET_COUNTRY_CODE:
                return "CMD_SET_COUNTRY_CODE";
            case CMD_ENABLE_RSSI_POLL:
                return "CMD_ENABLE_RSSI_POLL";
            case CMD_RSSI_POLL:
                return "CMD_RSSI_POLL";
            case CMD_START_PACKET_FILTERING:
                return "CMD_START_PACKET_FILTERING";
            case CMD_STOP_PACKET_FILTERING:
                return "CMD_STOP_PACKET_FILTERING";
            case CMD_SET_SUSPEND_OPT_ENABLED:
                return "CMD_SET_SUSPEND_OPT_ENABLED";
            case CMD_DELAYED_NETWORK_DISCONNECT:
                return "CMD_DELAYED_NETWORK_DISCONNECT";
            case CMD_NO_NETWORKS_PERIODIC_SCAN:
                return "CMD_NO_NETWORKS_PERIODIC_SCAN";
            case CMD_TEST_NETWORK_DISCONNECT:
                return "CMD_TEST_NETWORK_DISCONNECT";
            case CMD_SET_FREQUENCY_BAND:
                return "CMD_SET_FREQUENCY_BAND";
            case CMD_OBTAINING_IP_ADDRESS_WATCHDOG_TIMER:
                return "CMD_OBTAINING_IP_ADDRESS_WATCHDOG_TIMER";
            case CMD_ROAM_WATCHDOG_TIMER:
                return "CMD_ROAM_WATCHDOG_TIMER";
            case CMD_SCREEN_STATE_CHANGED:
                return "CMD_SCREEN_STATE_CHANGED";
            case CMD_DISCONNECTING_WATCHDOG_TIMER:
                return "CMD_DISCONNECTING_WATCHDOG_TIMER";
            case CMD_DISABLE_EPHEMERAL_NETWORK:
                return "CMD_DISABLE_EPHEMERAL_NETWORK";
            case CMD_DISABLE_P2P_REQ:
                return "CMD_DISABLE_P2P_REQ";
            case CMD_DISABLE_P2P_RSP:
                return "CMD_DISABLE_P2P_RSP";
            case CMD_BOOT_COMPLETED:
                return "CMD_BOOT_COMPLETED";
            case CMD_SET_BATCHED_SCAN:
                return "CMD_SET_BATCHED_SCAN";
            case CMD_START_NEXT_BATCHED_SCAN:
                return "CMD_START_NEXT_BATCHED_SCAN";
            case CMD_POLL_BATCHED_SCAN:
                return "CMD_POLL_BATCHED_SCAN";
            case CMD_IP_CONFIGURATION_SUCCESSFUL:
                return "CMD_IP_CONFIGURATION_SUCCESSFUL";
            case CMD_IP_CONFIGURATION_LOST:
                return "CMD_IP_CONFIGURATION_LOST";
            case CMD_UPDATE_LINKPROPERTIES:
                return "CMD_UPDATE_LINKPROPERTIES";
            case CMD_TARGET_BSSID:
                return "CMD_TARGET_BSSID";
            case CMD_RELOAD_TLS_AND_RECONNECT:
                return "CMD_RELOAD_TLS_AND_RECONNECT";
            case CMD_AUTO_CONNECT:
                return "CMD_AUTO_CONNECT";
            case CMD_UNWANTED_NETWORK:
                return "CMD_UNWANTED_NETWORK";
            case CMD_AUTO_ROAM:
                return "CMD_AUTO_ROAM";
            case CMD_AUTO_SAVE_NETWORK:
                return "CMD_AUTO_SAVE_NETWORK";
            case CMD_ASSOCIATED_BSSID:
                return "CMD_ASSOCIATED_BSSID";
            case CMD_NETWORK_STATUS:
                return "CMD_NETWORK_STATUS";
            case 135189:
                return "POOR_LINK_DETECTED";
            case 135190:
                return "GOOD_LINK_DETECTED";
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
            case WifiP2pServiceImpl.SET_COUNTRY_CODE:
                return "P2P.SET_COUNTRY_CODE";
            case WifiMonitor.SUP_CONNECTION_EVENT:
                return "SUP_CONNECTION_EVENT";
            case WifiMonitor.SUP_DISCONNECTION_EVENT:
                return "SUP_DISCONNECTION_EVENT";
            case WifiMonitor.NETWORK_CONNECTION_EVENT:
                return "NETWORK_CONNECTION_EVENT";
            case WifiMonitor.NETWORK_DISCONNECTION_EVENT:
                return "NETWORK_DISCONNECTION_EVENT";
            case WifiMonitor.SCAN_RESULTS_EVENT:
                return "SCAN_RESULTS_EVENT";
            case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT:
                return "SUPPLICANT_STATE_CHANGE_EVENT";
            case WifiMonitor.AUTHENTICATION_FAILURE_EVENT:
                return "AUTHENTICATION_FAILURE_EVENT";
            case WifiMonitor.WPS_SUCCESS_EVENT:
                return "WPS_SUCCESS_EVENT";
            case WifiMonitor.WPS_FAIL_EVENT:
                return "WPS_FAIL_EVENT";
            case WifiMonitor.DRIVER_HUNG_EVENT:
                return "DRIVER_HUNG_EVENT";
            case WifiMonitor.SSID_TEMP_DISABLED:
                return "SSID_TEMP_DISABLED";
            case WifiMonitor.SSID_REENABLED:
                return "SSID_REENABLED";
            case WifiMonitor.SUP_REQUEST_IDENTITY:
                return "SUP_REQUEST_IDENTITY";
            case WifiMonitor.ASSOCIATION_REJECTION_EVENT:
                return "ASSOCIATION_REJECTION_EVENT";
            case 151553:
                return "CONNECT_NETWORK";
            case 151556:
                return "FORGET_NETWORK";
            case 151559:
                return "SAVE_NETWORK";
            case 151562:
                return "START_WPS";
            case 151563:
                return "START_WPS_SUCCEEDED";
            case 151564:
                return "WPS_FAILED";
            case 151565:
                return "WPS_COMPLETED";
            case 151566:
                return "CANCEL_WPS";
            case 151567:
                return "CANCEL_WPS_FAILED";
            case 151568:
                return "CANCEL_WPS_SUCCEDED";
            case 151569:
                return "WifiManager.DISABLE_NETWORK";
            case 151572:
                return "RSSI_PKTCNT_FETCH";
            case 196609:
                return "CMD_START_DHCP";
            case 196610:
                return "CMD_STOP_DHCP";
            case 196611:
                return "CMD_RENEW_DHCP";
            case 196612:
                return "CMD_PRE_DHCP_ACTION";
            case 196613:
                return "CMD_POST_DHCP_ACTION";
            case 196614:
                return "CMD_ON_QUIT";
            case 196615:
                return "CMD_PRE_DHCP_ACTION_COMPLETE";
            default:
                String s = "what:" + Integer.toString(what);
                return s;
        }
    }

    void registerConnected() {
        if (this.mLastNetworkId != -1) {
            System.currentTimeMillis();
            WifiConfiguration config = this.mWifiConfigStore.getWifiConfiguration(this.mLastNetworkId);
            if (config != null) {
                config.lastConnected = System.currentTimeMillis();
                config.autoJoinBailedDueToLowRssi = DEBUG_PARSE;
                config.setAutoJoinStatus(0);
                config.numConnectionFailures = 0;
                config.numIpConfigFailures = 0;
                config.numAuthFailures = 0;
                config.numAssociation++;
            }
            this.mBadLinkspeedcount = 0;
        }
    }

    void registerDisconnected() {
        if (this.mLastNetworkId != -1) {
            System.currentTimeMillis();
            WifiConfiguration config = this.mWifiConfigStore.getWifiConfiguration(this.mLastNetworkId);
            if (config != null) {
                config.lastDisconnected = System.currentTimeMillis();
                if (config.ephemeral) {
                    this.mWifiConfigStore.forgetNetwork(this.mLastNetworkId);
                }
            }
        }
    }

    void noteWifiDisabledWhileAssociated() {
        boolean isHighRSSI = true;
        int rssi = this.mWifiInfo.getRssi();
        WifiConfiguration config = getCurrentWifiConfiguration();
        if (getCurrentState() == this.mConnectedState && rssi != -127 && config != null) {
            boolean is24GHz = this.mWifiInfo.is24GHz();
            boolean isBadRSSI = (is24GHz && rssi < this.mWifiConfigStore.thresholdBadRssi24) || (!is24GHz && rssi < this.mWifiConfigStore.thresholdBadRssi5);
            boolean isLowRSSI = (is24GHz && rssi < this.mWifiConfigStore.thresholdLowRssi24) || (!is24GHz && this.mWifiInfo.getRssi() < this.mWifiConfigStore.thresholdLowRssi5);
            if ((!is24GHz || rssi < this.mWifiConfigStore.thresholdGoodRssi24) && (is24GHz || this.mWifiInfo.getRssi() < this.mWifiConfigStore.thresholdGoodRssi5)) {
                isHighRSSI = false;
            }
            if (isBadRSSI) {
                config.numUserTriggeredWifiDisableLowRSSI++;
            } else if (isLowRSSI) {
                config.numUserTriggeredWifiDisableBadRSSI++;
            } else if (!isHighRSSI) {
                config.numUserTriggeredWifiDisableNotHighRSSI++;
            }
        }
    }

    WifiConfiguration getCurrentWifiConfiguration() {
        if (this.mLastNetworkId == -1) {
            return null;
        }
        return this.mWifiConfigStore.getWifiConfiguration(this.mLastNetworkId);
    }

    ScanResult getCurrentScanResult() {
        WifiConfiguration config = getCurrentWifiConfiguration();
        if (config == null) {
            return null;
        }
        String BSSID = this.mWifiInfo.getBSSID();
        if (BSSID == null) {
            BSSID = this.mTargetRoamBSSID;
        }
        if (config.scanResultCache != null) {
            return (ScanResult) config.scanResultCache.get(BSSID);
        }
        return null;
    }

    String getCurrentBSSID() {
        if (this.linkDebouncing) {
            return null;
        }
        return this.mLastBssid;
    }

    class ConnectModeState extends State {
        ConnectModeState() {
        }

        public void enter() {
            if (WifiStateMachine.DBG) {
                WifiStateMachine.this.log(getName() + "\n");
            }
            boolean activeRoaming = Settings.System.getInt(WifiStateMachine.this.mContext.getContentResolver(), "wifi_active_roaming", 0) == 1;
            if (!WifiStateMachine.this.setActiveRoaming(activeRoaming)) {
                WifiStateMachine.this.loge("Fail to [ " + (activeRoaming ? "Enable" : "Disable") + " ] Active Roaming");
            }
        }

        public boolean processMessage(Message message) {
            WpsResult wpsResult;
            WifiConfiguration config;
            WifiStateMachine.this.logStateAndMessage(message, getClass().getSimpleName());
            switch (message.what) {
                case WifiStateMachine.CMD_ADD_OR_UPDATE_NETWORK:
                    WifiConfiguration config2 = (WifiConfiguration) message.obj;
                    int res = WifiStateMachine.this.mWifiConfigStore.addOrUpdateNetwork(config2, message.sendingUid);
                    if (res < 0) {
                        WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_FAIL;
                    } else {
                        WifiConfiguration curConfig = WifiStateMachine.this.getCurrentWifiConfiguration();
                        if (curConfig != null && config2 != null && curConfig.priority < config2.priority && config2.status == 2) {
                            WifiStateMachine.this.mWifiConfigStore.setLastSelectedConfiguration(res);
                            WifiStateMachine.this.lastConnectAttempt = System.currentTimeMillis();
                            WifiStateMachine.this.mWifiConnectionStatistics.numWifiManagerJoinAttempt++;
                            WifiStateMachine.this.startScan(WifiStateMachine.ADD_OR_UPDATE_SOURCE, 0, null, null);
                        }
                    }
                    WifiStateMachine.this.replyToMessage(message, WifiStateMachine.CMD_ADD_OR_UPDATE_NETWORK, res);
                    return true;
                case WifiStateMachine.CMD_REMOVE_NETWORK:
                    boolean ok = WifiStateMachine.this.mWifiConfigStore.removeNetwork(message.arg1);
                    if (!ok) {
                        WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_FAIL;
                    }
                    WifiStateMachine.this.replyToMessage(message, message.what, ok ? 1 : -1);
                    return true;
                case WifiStateMachine.CMD_ENABLE_NETWORK:
                    boolean others = message.arg2 == 1 ? true : WifiStateMachine.DEBUG_PARSE;
                    if (others) {
                        WifiStateMachine.this.mWifiAutoJoinController.updateConfigurationHistory(message.arg1, true, WifiStateMachine.DEBUG_PARSE);
                        WifiStateMachine.this.mWifiConfigStore.setLastSelectedConfiguration(message.arg1);
                        WifiStateMachine.this.lastConnectAttempt = System.currentTimeMillis();
                        WifiStateMachine.this.mWifiConnectionStatistics.numWifiManagerJoinAttempt++;
                    }
                    WifiStateMachine.this.autoRoamSetBSSID(message.arg1, "any");
                    boolean ok2 = WifiStateMachine.this.mWifiConfigStore.enableNetwork(message.arg1, message.arg2 == 1 ? true : WifiStateMachine.DEBUG_PARSE);
                    if (!ok2) {
                        WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_FAIL;
                    }
                    WifiStateMachine.this.replyToMessage(message, message.what, ok2 ? 1 : -1);
                    return true;
                case WifiStateMachine.CMD_ENABLE_ALL_NETWORKS:
                    long time = SystemClock.elapsedRealtime();
                    if (time - WifiStateMachine.this.mLastEnableAllNetworksTime > 180000) {
                        WifiStateMachine.this.mWifiConfigStore.enableAllNetworks();
                        WifiStateMachine.this.startScan(-1, -1, null, null);
                        WifiStateMachine.this.mLastEnableAllNetworksTime = time;
                    }
                    return true;
                case WifiStateMachine.CMD_BLACKLIST_NETWORK:
                    WifiStateMachine.this.mWifiNative.addToBlacklist((String) message.obj);
                    return true;
                case WifiStateMachine.CMD_CLEAR_BLACKLIST:
                    WifiStateMachine.this.mWifiNative.clearBlacklist();
                    return true;
                case WifiStateMachine.CMD_SAVE_CONFIG:
                    boolean ok3 = WifiStateMachine.this.mWifiConfigStore.saveConfig();
                    if (WifiStateMachine.DBG) {
                        WifiStateMachine.this.loge("wifistatemachine did save config " + ok3);
                    }
                    WifiStateMachine.this.replyToMessage(message, WifiStateMachine.CMD_SAVE_CONFIG, ok3 ? 1 : -1);
                    IBackupManager ibm = IBackupManager.Stub.asInterface(ServiceManager.getService("backup"));
                    if (ibm != null) {
                        try {
                            ibm.dataChanged("com.android.providers.settings");
                            break;
                        } catch (Exception e) {
                        }
                    }
                    return true;
                case WifiStateMachine.CMD_GET_CONFIGURED_NETWORKS:
                    WifiStateMachine.this.replyToMessage(message, message.what, WifiStateMachine.this.mWifiConfigStore.getConfiguredNetworks());
                    return true;
                case WifiStateMachine.CMD_GET_PRIVILEGED_CONFIGURED_NETWORKS:
                    WifiStateMachine.this.replyToMessage(message, message.what, WifiStateMachine.this.mWifiConfigStore.getPrivilegedConfiguredNetworks());
                    return true;
                case WifiStateMachine.CMD_DISCONNECT:
                    WifiStateMachine.this.mWifiConfigStore.setLastSelectedConfiguration(-1);
                    WifiStateMachine.this.mWifiNative.disconnect();
                    return true;
                case WifiStateMachine.CMD_RECONNECT:
                    WifiStateMachine.this.mWifiAutoJoinController.attemptAutoJoin();
                    return true;
                case WifiStateMachine.CMD_REASSOCIATE:
                    WifiStateMachine.this.lastConnectAttempt = System.currentTimeMillis();
                    WifiStateMachine.this.mWifiNative.reassociate();
                    return true;
                case WifiStateMachine.CMD_DISABLE_EPHEMERAL_NETWORK:
                    WifiConfiguration config3 = WifiStateMachine.this.mWifiConfigStore.disableEphemeralNetwork((String) message.obj);
                    if (config3 != null && config3.networkId == WifiStateMachine.this.mLastNetworkId) {
                        WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_DISCONNECT);
                    }
                    return true;
                case WifiStateMachine.CMD_RELOAD_TLS_AND_RECONNECT:
                    if (WifiStateMachine.this.mWifiConfigStore.needsUnlockedKeyStore()) {
                        WifiStateMachine.this.logd("Reconnecting to give a chance to un-connected TLS networks");
                        WifiStateMachine.this.mWifiNative.disconnect();
                        WifiStateMachine.this.lastConnectAttempt = System.currentTimeMillis();
                        WifiStateMachine.this.mWifiNative.reconnect();
                    }
                    return true;
                case WifiStateMachine.CMD_AUTO_CONNECT:
                    boolean didDisconnect = WifiStateMachine.DEBUG_PARSE;
                    if (WifiStateMachine.this.getCurrentState() != WifiStateMachine.this.mDisconnectedState) {
                        didDisconnect = true;
                        WifiStateMachine.this.mWifiNative.disconnect();
                    }
                    WifiConfiguration config4 = (WifiConfiguration) message.obj;
                    int netId = message.arg1;
                    int roam = message.arg2;
                    WifiStateMachine.this.loge("CMD_AUTO_CONNECT sup state " + WifiStateMachine.this.mSupplicantStateTracker.getSupplicantStateName() + " my state " + WifiStateMachine.this.getCurrentState().getName() + " nid=" + Integer.toString(netId) + " roam=" + Integer.toString(roam));
                    if (config4 == null) {
                        WifiStateMachine.this.loge("AUTO_CONNECT and no config, bail out...");
                    } else {
                        WifiStateMachine.this.autoRoamSetBSSID(netId, config4.BSSID);
                        WifiStateMachine.this.loge("CMD_AUTO_CONNECT will save config -> " + config4.SSID + " nid=" + Integer.toString(netId));
                        if (WifiStateMachine.this.checkSsidConverted(config4)) {
                            config4.NOT_UTF8 = true;
                        }
                        int netId2 = WifiStateMachine.this.mWifiConfigStore.saveNetwork(config4, -1).getNetworkId();
                        WifiStateMachine.this.loge("CMD_AUTO_CONNECT did save config ->  nid=" + Integer.toString(netId2));
                        WifiStateMachine.this.mWifiConfigStore.enableNetworkWithoutBroadcast(netId2, WifiStateMachine.DEBUG_PARSE);
                        if (WifiStateMachine.this.mWifiConfigStore.selectNetwork(netId2) && WifiStateMachine.this.mWifiNative.reconnect()) {
                            WifiStateMachine.this.lastConnectAttempt = System.currentTimeMillis();
                            WifiStateMachine.this.targetWificonfiguration = WifiStateMachine.this.mWifiConfigStore.getWifiConfiguration(netId2);
                            WifiConfiguration config5 = WifiStateMachine.this.mWifiConfigStore.getWifiConfiguration(netId2);
                            if (config5 != null && !WifiStateMachine.this.mWifiConfigStore.isLastSelectedConfiguration(config5)) {
                                WifiStateMachine.this.mWifiConfigStore.setLastSelectedConfiguration(-1);
                            }
                            WifiStateMachine.this.mAutoRoaming = roam;
                            if (WifiStateMachine.this.isRoaming() || WifiStateMachine.this.linkDebouncing) {
                                WifiStateMachine.this.transitionTo(WifiStateMachine.this.mRoamingState);
                            } else if (didDisconnect) {
                                WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectingState);
                            }
                        } else {
                            WifiStateMachine.this.loge("Failed to connect config: " + config4 + " netId: " + netId2);
                            WifiStateMachine.this.replyToMessage(message, 151554, 0);
                        }
                    }
                    return true;
                case WifiStateMachine.CMD_AUTO_ROAM:
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DISCARD;
                    return true;
                case WifiStateMachine.CMD_AUTO_SAVE_NETWORK:
                    WifiStateMachine.this.lastSavedConfigurationAttempt = null;
                    config = (WifiConfiguration) message.obj;
                    if (config != null) {
                        WifiStateMachine.this.loge("ERROR: SAVE_NETWORK with null configuration" + WifiStateMachine.this.mSupplicantStateTracker.getSupplicantStateName() + " my state " + WifiStateMachine.this.getCurrentState().getName());
                        WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_FAIL;
                        WifiStateMachine.this.replyToMessage(message, 151560, 0);
                    } else {
                        WifiStateMachine.this.lastSavedConfigurationAttempt = new WifiConfiguration(config);
                        int nid = config.networkId;
                        WifiStateMachine.this.loge("SAVE_NETWORK id=" + Integer.toString(nid) + " config=" + config.SSID + " nid=" + config.networkId + " supstate=" + WifiStateMachine.this.mSupplicantStateTracker.getSupplicantStateName() + " my state " + WifiStateMachine.this.getCurrentState().getName());
                        NetworkUpdateResult result = WifiStateMachine.this.mWifiConfigStore.saveNetwork(config, -1);
                        if (result.getNetworkId() != -1) {
                            if (WifiStateMachine.this.mWifiInfo.getNetworkId() == result.getNetworkId()) {
                                if (result.hasIpChanged()) {
                                    WifiStateMachine.this.log("Reconfiguring IP on connection");
                                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mObtainingIpState);
                                }
                                if (result.hasProxyChanged()) {
                                    WifiStateMachine.this.log("Reconfiguring proxy on connection");
                                    WifiStateMachine.this.updateLinkProperties(WifiStateMachine.CMD_UPDATE_LINKPROPERTIES);
                                }
                            }
                            WifiStateMachine.this.replyToMessage(message, 151561);
                            if (WifiStateMachine.VDBG) {
                                WifiStateMachine.this.loge("Success save network nid=" + Integer.toString(result.getNetworkId()));
                            }
                            synchronized (WifiStateMachine.this.mScanResultCache) {
                                boolean user = message.what == 151559 ? true : WifiStateMachine.DEBUG_PARSE;
                                WifiStateMachine.this.mWifiAutoJoinController.updateConfigurationHistory(result.getNetworkId(), user, true);
                                WifiStateMachine.this.mWifiAutoJoinController.attemptAutoJoin();
                            }
                        } else {
                            WifiStateMachine.this.loge("Failed to save network");
                            WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_FAIL;
                            WifiStateMachine.this.replyToMessage(message, 151560, 0);
                        }
                        break;
                    }
                    return true;
                case WifiP2pServiceImpl.DISCONNECT_WIFI_REQUEST:
                    if (message.arg1 == 1) {
                        WifiStateMachine.this.mWifiNative.disconnect();
                        WifiStateMachine.this.mTemporarilyDisconnectWifi = true;
                    } else {
                        WifiStateMachine.this.mWifiNative.reconnect();
                        WifiStateMachine.this.mTemporarilyDisconnectWifi = WifiStateMachine.DEBUG_PARSE;
                    }
                    return true;
                case WifiMonitor.NETWORK_CONNECTION_EVENT:
                    if (WifiStateMachine.DBG) {
                        WifiStateMachine.this.log("Network connection established");
                    }
                    WifiStateMachine.this.mLastNetworkId = message.arg1;
                    WifiStateMachine.this.mLastBssid = (String) message.obj;
                    WifiStateMachine.this.mWifiInfo.setBSSID(WifiStateMachine.this.mLastBssid);
                    WifiStateMachine.this.mWifiInfo.setNetworkId(WifiStateMachine.this.mLastNetworkId);
                    WifiStateMachine.this.fetchRssiLinkSpeedAndFrequencyNative();
                    WifiStateMachine.this.setNetworkDetailedState(NetworkInfo.DetailedState.OBTAINING_IPADDR);
                    WifiStateMachine.this.sendNetworkStateChangeBroadcast(WifiStateMachine.this.mLastBssid);
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mObtainingIpState);
                    return true;
                case WifiMonitor.NETWORK_DISCONNECTION_EVENT:
                    if (WifiStateMachine.DBG) {
                        WifiStateMachine.this.log("ConnectModeState: Network connection lost ");
                    }
                    WifiStateMachine.this.handleNetworkDisconnect();
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectedState);
                    return true;
                case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT:
                    SupplicantState state = WifiStateMachine.this.handleSupplicantStateChange(message);
                    if (!SupplicantState.isDriverActive(state)) {
                        if (WifiStateMachine.this.mNetworkInfo.getState() != NetworkInfo.State.DISCONNECTED) {
                            WifiStateMachine.this.handleNetworkDisconnect();
                        }
                        WifiStateMachine.this.log("Detected an interface down, restart driver");
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDriverStoppedState);
                        WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_START_DRIVER);
                    } else if (!WifiStateMachine.this.linkDebouncing && state == SupplicantState.DISCONNECTED && WifiStateMachine.this.mNetworkInfo.getState() != NetworkInfo.State.DISCONNECTED) {
                        if (WifiStateMachine.DBG) {
                            WifiStateMachine.this.log("Missed CTRL-EVENT-DISCONNECTED, disconnect");
                        }
                        WifiStateMachine.this.handleNetworkDisconnect();
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectedState);
                    }
                    return true;
                case WifiMonitor.AUTHENTICATION_FAILURE_EVENT:
                    WifiStateMachine.this.mSupplicantStateTracker.sendMessage(WifiMonitor.AUTHENTICATION_FAILURE_EVENT);
                    return true;
                case WifiMonitor.SSID_TEMP_DISABLED:
                case WifiMonitor.SSID_REENABLED:
                    String substr = (String) message.obj;
                    String en = message.what == 147469 ? "temp-disabled" : "re-enabled";
                    WifiStateMachine.this.loge("ConnectModeState SSID state=" + en + " nid=" + Integer.toString(message.arg1) + " [" + substr + "]");
                    synchronized (WifiStateMachine.this.mScanResultCache) {
                        WifiStateMachine.this.mWifiConfigStore.handleSSIDStateChange(message.arg1, message.what == 147470 ? true : WifiStateMachine.DEBUG_PARSE, substr, WifiStateMachine.this.mWifiInfo.getBSSID());
                        break;
                    }
                    return true;
                case WifiMonitor.SUP_REQUEST_IDENTITY:
                    String ssid = (String) message.obj;
                    if (WifiStateMachine.this.targetWificonfiguration == null || WifiStateMachine.this.targetWificonfiguration.enterpriseConfig == null) {
                        if (WifiStateMachine.this.targetWificonfiguration != null && ssid != null && WifiStateMachine.this.targetWificonfiguration.SSID != null && WifiStateMachine.this.targetWificonfiguration.SSID.equals("\"" + ssid + "\"")) {
                            WifiStateMachine.this.mWifiConfigStore.handleSSIDStateChange(WifiStateMachine.this.targetWificonfiguration.networkId, WifiStateMachine.DEBUG_PARSE, "AUTH_FAILED no identity", null);
                        }
                        WifiStateMachine.this.mWifiConfigStore.setLastSelectedConfiguration(-1);
                        WifiStateMachine.this.mWifiNative.disconnect();
                    } else {
                        WifiEnterpriseConfig enterpriseConfig = WifiStateMachine.this.targetWificonfiguration.enterpriseConfig;
                        if (enterpriseConfig.getEapMethod() == 4 || enterpriseConfig.getEapMethod() == 5 || enterpriseConfig.getEapMethod() == 6) {
                            WifiStateMachine.this.handleEapSimIdentityRequest(message.arg2, enterpriseConfig.getEapMethod());
                        }
                    }
                    return true;
                case WifiMonitor.SUP_REQUEST_SIM_AUTH:
                    WifiStateMachine.this.logd("Received SUP_REQUEST_SIM_AUTH");
                    SimAuthRequestData requestData = (SimAuthRequestData) message.obj;
                    if (requestData != null) {
                        if (requestData.protocol == 4) {
                            WifiStateMachine.this.handleGsmAuthRequest(requestData);
                        } else if (requestData.protocol == 5) {
                            WifiStateMachine.this.handle3GAuthRequest(requestData);
                        }
                    } else {
                        WifiStateMachine.this.loge("Invalid sim auth request");
                    }
                    return true;
                case WifiMonitor.ASSOCIATION_REJECTION_EVENT:
                    WifiStateMachine.this.didBlackListBSSID = WifiStateMachine.DEBUG_PARSE;
                    String bssid = (String) message.obj;
                    if (bssid == null || TextUtils.isEmpty(bssid)) {
                        bssid = WifiStateMachine.this.mTargetRoamBSSID;
                    }
                    if (bssid != null) {
                        synchronized (WifiStateMachine.this.mScanResultCache) {
                            WifiStateMachine.this.didBlackListBSSID = WifiStateMachine.this.mWifiConfigStore.handleBSSIDBlackList(WifiStateMachine.this.mLastNetworkId, bssid, WifiStateMachine.DEBUG_PARSE);
                            break;
                        }
                    }
                    WifiStateMachine.this.mSupplicantStateTracker.sendMessage(WifiMonitor.ASSOCIATION_REJECTION_EVENT);
                    return true;
                case 151553:
                    int netId3 = message.arg1;
                    WifiConfiguration config6 = (WifiConfiguration) message.obj;
                    WifiStateMachine.this.mWifiConnectionStatistics.numWifiManagerJoinAttempt++;
                    boolean updatedExisting = WifiStateMachine.DEBUG_PARSE;
                    if (config6 != null) {
                        String configKey = config6.configKey(true);
                        WifiConfiguration savedConfig = WifiStateMachine.this.mWifiConfigStore.getWifiConfiguration(configKey);
                        if (savedConfig != null) {
                            config6 = savedConfig;
                            WifiStateMachine.this.loge("CONNECT_NETWORK updating existing config with id=" + config6.networkId + " configKey=" + configKey);
                            config6.ephemeral = WifiStateMachine.DEBUG_PARSE;
                            config6.autoJoinStatus = 0;
                            updatedExisting = true;
                        }
                        netId3 = WifiStateMachine.this.mWifiConfigStore.saveNetwork(config6, message.sendingUid).getNetworkId();
                    }
                    WifiConfiguration config7 = WifiStateMachine.this.mWifiConfigStore.getWifiConfiguration(netId3);
                    if (config7 == null) {
                        WifiStateMachine.this.loge("CONNECT_NETWORK id=" + Integer.toString(netId3) + " " + WifiStateMachine.this.mSupplicantStateTracker.getSupplicantStateName() + " my state " + WifiStateMachine.this.getCurrentState().getName());
                    } else {
                        String wasSkipped = config7.autoJoinBailedDueToLowRssi ? " skipped" : "";
                        WifiStateMachine.this.loge("CONNECT_NETWORK id=" + Integer.toString(netId3) + " config=" + config7.SSID + " cnid=" + config7.networkId + " supstate=" + WifiStateMachine.this.mSupplicantStateTracker.getSupplicantStateName() + " my state " + WifiStateMachine.this.getCurrentState().getName() + " uid = " + message.sendingUid + wasSkipped);
                    }
                    WifiStateMachine.this.autoRoamSetBSSID(netId3, "any");
                    if (message.sendingUid == 1010 || message.sendingUid == 1000) {
                        WifiStateMachine.this.clearConfigBSSID(config7, "CONNECT_NETWORK");
                    }
                    WifiStateMachine.this.mAutoRoaming = 0;
                    WifiStateMachine.this.mWifiAutoJoinController.updateConfigurationHistory(netId3, true, true);
                    WifiStateMachine.this.mWifiConfigStore.setLastSelectedConfiguration(netId3);
                    boolean didDisconnect2 = WifiStateMachine.DEBUG_PARSE;
                    if (WifiStateMachine.this.mLastNetworkId != -1 && WifiStateMachine.this.mLastNetworkId != netId3) {
                        didDisconnect2 = true;
                        WifiStateMachine.this.mWifiNative.disconnect();
                    }
                    WifiStateMachine.this.mWifiConfigStore.enableNetworkWithoutBroadcast(netId3, WifiStateMachine.DEBUG_PARSE);
                    if (WifiStateMachine.this.mWifiConfigStore.selectNetwork(netId3) && WifiStateMachine.this.mWifiNative.reconnect()) {
                        WifiStateMachine.this.lastConnectAttempt = System.currentTimeMillis();
                        WifiStateMachine.this.targetWificonfiguration = WifiStateMachine.this.mWifiConfigStore.getWifiConfiguration(netId3);
                        WifiStateMachine.this.mSupplicantStateTracker.sendMessage(151553);
                        WifiStateMachine.this.replyToMessage(message, 151555);
                        if (didDisconnect2) {
                            WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectingState);
                        } else if (!updatedExisting || WifiStateMachine.this.getCurrentState() != WifiStateMachine.this.mConnectedState || WifiStateMachine.this.getCurrentWifiConfiguration().networkId != netId3) {
                            WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectedState);
                        } else {
                            WifiStateMachine.this.updateCapabilities(config7);
                        }
                    } else {
                        WifiStateMachine.this.loge("Failed to connect config: " + config7 + " netId: " + netId3);
                        WifiStateMachine.this.replyToMessage(message, 151554, 0);
                    }
                    return true;
                case 151556:
                    WifiConfiguration toRemove = WifiStateMachine.this.mWifiConfigStore.getWifiConfiguration(message.arg1);
                    if (toRemove == null) {
                        WifiStateMachine.this.lastForgetConfigurationAttempt = null;
                    } else {
                        WifiStateMachine.this.lastForgetConfigurationAttempt = new WifiConfiguration(toRemove);
                    }
                    if (WifiStateMachine.this.mWifiConfigStore.forgetNetwork(message.arg1)) {
                        WifiStateMachine.this.replyToMessage(message, 151558);
                    } else {
                        WifiStateMachine.this.loge("Failed to forget network");
                        WifiStateMachine.this.replyToMessage(message, 151557, 0);
                    }
                    return true;
                case 151559:
                    WifiStateMachine.this.mWifiConnectionStatistics.numWifiManagerJoinAttempt++;
                    WifiStateMachine.this.lastSavedConfigurationAttempt = null;
                    config = (WifiConfiguration) message.obj;
                    if (config != null) {
                    }
                    return true;
                case 151562:
                    WpsInfo wpsInfo = (WpsInfo) message.obj;
                    switch (wpsInfo.setup) {
                        case 0:
                            wpsResult = WifiStateMachine.this.mWifiConfigStore.startWpsPbc(wpsInfo);
                            break;
                        case 1:
                            wpsResult = WifiStateMachine.this.mWifiConfigStore.startWpsWithPinFromDevice(wpsInfo);
                            break;
                        case 2:
                            wpsResult = WifiStateMachine.this.mWifiConfigStore.startWpsWithPinFromAccessPoint(wpsInfo);
                            break;
                        default:
                            wpsResult = new WpsResult(WpsResult.Status.FAILURE);
                            WifiStateMachine.this.loge("Invalid setup for WPS");
                            break;
                    }
                    WifiStateMachine.this.mWifiConfigStore.setLastSelectedConfiguration(-1);
                    if (wpsResult.status == WpsResult.Status.SUCCESS) {
                        WifiStateMachine.this.replyToMessage(message, 151563, wpsResult);
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mWpsRunningState);
                    } else {
                        WifiStateMachine.this.loge("Failed to start WPS with config " + wpsInfo.toString());
                        WifiStateMachine.this.replyToMessage(message, 151564, 0);
                    }
                    return true;
                case 151569:
                    if (WifiStateMachine.this.mWifiConfigStore.disableNetwork(message.arg1, 5)) {
                        WifiStateMachine.this.replyToMessage(message, 151571);
                    } else {
                        WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_FAIL;
                        WifiStateMachine.this.replyToMessage(message, 151570, 0);
                    }
                    return true;
                default:
                    return WifiStateMachine.DEBUG_PARSE;
            }
        }
    }

    private void updateCapabilities(WifiConfiguration config) {
        if (config.ephemeral) {
            this.mNetworkCapabilities.removeCapability(14);
        } else {
            this.mNetworkCapabilities.addCapability(14);
        }
        this.mNetworkAgent.sendNetworkCapabilities(this.mNetworkCapabilities);
    }

    private class WifiNetworkAgent extends NetworkAgent {
        public WifiNetworkAgent(Looper l, Context c, String TAG, NetworkInfo ni, NetworkCapabilities nc, LinkProperties lp, int score) {
            super(l, c, TAG, ni, nc, lp, score);
        }

        protected void unwanted() {
            if (this == WifiStateMachine.this.mNetworkAgent) {
                if (WifiStateMachine.DBG) {
                    log("WifiNetworkAgent -> Wifi unwanted score " + Integer.toString(WifiStateMachine.this.mWifiInfo.score));
                }
                WifiStateMachine.this.unwantedNetwork(0);
            }
        }

        protected void networkStatus(int status) {
            if (status == 2) {
                if (WifiStateMachine.DBG) {
                    log("WifiNetworkAgent -> Wifi networkStatus invalid, score=" + Integer.toString(WifiStateMachine.this.mWifiInfo.score));
                }
                WifiStateMachine.this.unwantedNetwork(1);
            } else if (status == 1) {
                if (WifiStateMachine.DBG && WifiStateMachine.this.mWifiInfo != null) {
                    log("WifiNetworkAgent -> Wifi networkStatus valid, score= " + Integer.toString(WifiStateMachine.this.mWifiInfo.score));
                }
                WifiStateMachine.this.doNetworkStatus(status);
            }
        }
    }

    void unwantedNetwork(int reason) {
        sendMessage(CMD_UNWANTED_NETWORK, reason);
    }

    void doNetworkStatus(int status) {
        sendMessage(CMD_NETWORK_STATUS, status);
    }

    boolean startScanForConfiguration(WifiConfiguration config, boolean restrictChannelList) {
        if (config == null) {
            return DEBUG_PARSE;
        }
        if (config.scanResultCache == null || !config.allowedKeyManagement.get(1) || config.scanResultCache.size() > 6) {
            return true;
        }
        HashSet<Integer> channels = this.mWifiConfigStore.makeChannelList(config, ONE_HOUR_MILLI, restrictChannelList);
        if (channels != null && channels.size() != 0) {
            StringBuilder freqs = new StringBuilder();
            boolean first = true;
            for (Integer channel : channels) {
                if (!first) {
                    freqs.append(",");
                }
                freqs.append(channel.toString());
                first = DEBUG_PARSE;
            }
            loge("WifiStateMachine starting scan for " + config.configKey() + " with " + ((Object) freqs));
            if (startScanNative(1, freqs.toString())) {
                noteScanStart(SCAN_ALARM_SOURCE, null);
                this.messageHandlingStatus = MESSAGE_HANDLING_STATUS_OK;
            } else {
                this.messageHandlingStatus = MESSAGE_HANDLING_STATUS_HANDLING_ERROR;
            }
            return true;
        }
        if (!DBG) {
            return DEBUG_PARSE;
        }
        loge("WifiStateMachine no channels for " + config.configKey());
        return DEBUG_PARSE;
    }

    void clearCurrentConfigBSSID(String dbg) {
        WifiConfiguration config = getCurrentWifiConfiguration();
        if (config != null) {
            clearConfigBSSID(config, dbg);
        }
    }

    void clearConfigBSSID(WifiConfiguration config, String dbg) {
        if (config != null) {
            if (DBG) {
                loge(dbg + " " + this.mTargetRoamBSSID + " config " + config.configKey() + " config.bssid " + config.BSSID);
            }
            config.autoJoinBSSID = "any";
            config.BSSID = "any";
            if (DBG) {
                loge(dbg + " " + config.SSID + " nid=" + Integer.toString(config.networkId));
            }
            this.mWifiConfigStore.saveWifiConfigBSSID(config);
        }
    }

    class L2ConnectedState extends State {
        L2ConnectedState() {
        }

        public void enter() {
            WifiStateMachine.access$17508(WifiStateMachine.this);
            if (WifiStateMachine.this.mEnableRssiPolling) {
                WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_RSSI_POLL, WifiStateMachine.this.mRssiPollToken, 0);
            }
            if (WifiStateMachine.this.mNetworkAgent != null) {
                WifiStateMachine.this.loge("Have NetworkAgent when entering L2Connected");
                WifiStateMachine.this.setNetworkDetailedState(NetworkInfo.DetailedState.DISCONNECTED);
            }
            WifiStateMachine.this.setNetworkDetailedState(NetworkInfo.DetailedState.CONNECTING);
            if (!TextUtils.isEmpty(WifiStateMachine.this.mTcpBufferSizes)) {
                WifiStateMachine.this.mLinkProperties.setTcpBufferSizes(WifiStateMachine.this.mTcpBufferSizes);
            }
            WifiStateMachine.this.mNetworkAgent = WifiStateMachine.this.new WifiNetworkAgent(WifiStateMachine.this.getHandler().getLooper(), WifiStateMachine.this.mContext, "WifiNetworkAgent", WifiStateMachine.this.mNetworkInfo, WifiStateMachine.this.mNetworkCapabilitiesFilter, WifiStateMachine.this.mLinkProperties, 60);
            WifiStateMachine.this.clearCurrentConfigBSSID("L2ConnectedState");
        }

        public void exit() {
            if (WifiStateMachine.DBG) {
                StringBuilder sb = new StringBuilder();
                sb.append("leaving L2ConnectedState state nid=" + Integer.toString(WifiStateMachine.this.mLastNetworkId));
                if (WifiStateMachine.this.mLastBssid != null) {
                    sb.append(" ").append(WifiStateMachine.this.mLastBssid);
                }
            }
            if (WifiStateMachine.this.mLastBssid != null || WifiStateMachine.this.mLastNetworkId != -1) {
                WifiStateMachine.this.handleNetworkDisconnect();
            }
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, getClass().getSimpleName());
            switch (message.what) {
                case WifiStateMachine.CMD_START_SCAN:
                    WifiStateMachine.this.loge("WifiStateMachine CMD_START_SCAN source " + message.arg1 + " txSuccessRate=" + String.format("%.2f", Double.valueOf(WifiStateMachine.this.mWifiInfo.txSuccessRate)) + " rxSuccessRate=" + String.format("%.2f", Double.valueOf(WifiStateMachine.this.mWifiInfo.rxSuccessRate)) + " targetRoamBSSID=" + WifiStateMachine.this.mTargetRoamBSSID + " RSSI=" + WifiStateMachine.this.mWifiInfo.getRssi());
                    if (message.arg1 == WifiStateMachine.SCAN_ALARM_SOURCE) {
                        boolean shouldScan = (WifiStateMachine.this.mScreenOn && WifiStateMachine.this.mWifiConfigStore.enableAutoJoinScanWhenAssociated) ? true : WifiStateMachine.DEBUG_PARSE;
                        if (!WifiStateMachine.this.checkAndRestartDelayedScan(message.arg2, shouldScan, WifiStateMachine.this.mWifiConfigStore.associatedPartialScanPeriodMilli, null, null)) {
                            WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_OBSOLETE;
                            WifiStateMachine.this.loge("WifiStateMachine L2Connected CMD_START_SCAN source " + message.arg1 + " " + message.arg2 + ", " + WifiStateMachine.this.mDelayedScanCounter + " -> obsolete");
                            return true;
                        }
                        if (WifiStateMachine.this.mP2pConnected.get()) {
                            WifiStateMachine.this.loge("WifiStateMachine L2Connected CMD_START_SCAN source " + message.arg1 + " " + message.arg2 + ", " + WifiStateMachine.this.mDelayedScanCounter + " ignore because P2P is connected");
                            WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DISCARD;
                            return true;
                        }
                        boolean tryFullBandScan = WifiStateMachine.DEBUG_PARSE;
                        boolean restrictChannelList = WifiStateMachine.DEBUG_PARSE;
                        long now_ms = System.currentTimeMillis();
                        if (WifiStateMachine.DBG) {
                            WifiStateMachine.this.loge("WifiStateMachine CMD_START_SCAN with age=" + Long.toString(now_ms - WifiStateMachine.this.lastFullBandConnectedTimeMilli) + " interval=" + WifiStateMachine.this.fullBandConnectedTimeIntervalMilli + " maxinterval=" + WifiStateMachine.maxFullBandConnectedTimeIntervalMilli);
                        }
                        if (WifiStateMachine.this.mWifiInfo != null) {
                            if (WifiStateMachine.this.mWifiConfigStore.enableFullBandScanWhenAssociated && now_ms - WifiStateMachine.this.lastFullBandConnectedTimeMilli > WifiStateMachine.this.fullBandConnectedTimeIntervalMilli) {
                                if (WifiStateMachine.DBG) {
                                    WifiStateMachine.this.loge("WifiStateMachine CMD_START_SCAN try full band scan age=" + Long.toString(now_ms - WifiStateMachine.this.lastFullBandConnectedTimeMilli) + " interval=" + WifiStateMachine.this.fullBandConnectedTimeIntervalMilli + " maxinterval=" + WifiStateMachine.maxFullBandConnectedTimeIntervalMilli);
                                }
                                tryFullBandScan = true;
                            }
                            if (WifiStateMachine.this.mWifiInfo.txSuccessRate > WifiStateMachine.this.mWifiConfigStore.maxTxPacketForFullScans || WifiStateMachine.this.mWifiInfo.rxSuccessRate > WifiStateMachine.this.mWifiConfigStore.maxRxPacketForFullScans) {
                                if (WifiStateMachine.DBG) {
                                    WifiStateMachine.this.loge("WifiStateMachine CMD_START_SCAN prevent full band scan due to pkt rate");
                                }
                                tryFullBandScan = WifiStateMachine.DEBUG_PARSE;
                            }
                            if (WifiStateMachine.this.mWifiInfo.txSuccessRate > WifiStateMachine.this.mWifiConfigStore.maxTxPacketForPartialScans || WifiStateMachine.this.mWifiInfo.rxSuccessRate > WifiStateMachine.this.mWifiConfigStore.maxRxPacketForPartialScans) {
                                restrictChannelList = true;
                                if (WifiStateMachine.this.mWifiConfigStore.alwaysEnableScansWhileAssociated == 0) {
                                    if (WifiStateMachine.DBG) {
                                        WifiStateMachine.this.loge("WifiStateMachine CMD_START_SCAN source " + message.arg1 + " ...and ignore scans tx=" + String.format("%.2f", Double.valueOf(WifiStateMachine.this.mWifiInfo.txSuccessRate)) + " rx=" + String.format("%.2f", Double.valueOf(WifiStateMachine.this.mWifiInfo.rxSuccessRate)));
                                    }
                                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_REFUSED;
                                    return true;
                                }
                            }
                        }
                        WifiConfiguration currentConfiguration = WifiStateMachine.this.getCurrentWifiConfiguration();
                        if (WifiStateMachine.DBG) {
                            WifiStateMachine.this.loge("WifiStateMachine CMD_START_SCAN full=" + tryFullBandScan);
                        }
                        if (currentConfiguration != null) {
                            if (WifiStateMachine.this.fullBandConnectedTimeIntervalMilli < WifiStateMachine.this.mWifiConfigStore.associatedPartialScanPeriodMilli) {
                                WifiStateMachine.this.fullBandConnectedTimeIntervalMilli = WifiStateMachine.this.mWifiConfigStore.associatedPartialScanPeriodMilli;
                            }
                            if (tryFullBandScan) {
                                WifiStateMachine.this.lastFullBandConnectedTimeMilli = now_ms;
                                if (WifiStateMachine.this.fullBandConnectedTimeIntervalMilli < WifiStateMachine.this.mWifiConfigStore.associatedFullScanMaxIntervalMilli) {
                                    WifiStateMachine.this.fullBandConnectedTimeIntervalMilli = (WifiStateMachine.this.fullBandConnectedTimeIntervalMilli * ((long) WifiStateMachine.this.mWifiConfigStore.associatedFullScanBackoff)) / 8;
                                    if (WifiStateMachine.DBG) {
                                        WifiStateMachine.this.loge("WifiStateMachine CMD_START_SCAN bump interval =" + WifiStateMachine.this.fullBandConnectedTimeIntervalMilli);
                                    }
                                }
                                WifiStateMachine.this.handleScanRequest(1, message);
                            } else if (!WifiStateMachine.this.startScanForConfiguration(currentConfiguration, restrictChannelList)) {
                                if (WifiStateMachine.DBG) {
                                    WifiStateMachine.this.loge("WifiStateMachine starting scan,  did not find channels -> full");
                                }
                                WifiStateMachine.this.lastFullBandConnectedTimeMilli = now_ms;
                                if (WifiStateMachine.this.fullBandConnectedTimeIntervalMilli < WifiStateMachine.this.mWifiConfigStore.associatedFullScanMaxIntervalMilli) {
                                    WifiStateMachine.this.fullBandConnectedTimeIntervalMilli = (WifiStateMachine.this.fullBandConnectedTimeIntervalMilli * ((long) WifiStateMachine.this.mWifiConfigStore.associatedFullScanBackoff)) / 8;
                                    if (WifiStateMachine.DBG) {
                                        WifiStateMachine.this.loge("WifiStateMachine CMD_START_SCAN bump interval =" + WifiStateMachine.this.fullBandConnectedTimeIntervalMilli);
                                    }
                                }
                                WifiStateMachine.this.handleScanRequest(1, message);
                            }
                        } else {
                            WifiStateMachine.this.loge("CMD_START_SCAN : connected mode and no configuration");
                            WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_HANDLING_ERROR;
                        }
                        return true;
                    }
                    return WifiStateMachine.DEBUG_PARSE;
                case WifiStateMachine.CMD_SET_OPERATIONAL_MODE:
                    if (message.arg1 != 1) {
                        WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_DISCONNECT);
                        WifiStateMachine.this.deferMessage(message);
                        if (message.arg1 == 3) {
                            WifiStateMachine.this.noteWifiDisabledWhileAssociated();
                        }
                    }
                    WifiStateMachine.this.mWifiConfigStore.setLastSelectedConfiguration(-1);
                    return true;
                case WifiStateMachine.CMD_DISCONNECT:
                    WifiStateMachine.this.mWifiNative.disconnect();
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectingState);
                    return true;
                case WifiStateMachine.CMD_SET_COUNTRY_CODE:
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DEFERRED;
                    WifiStateMachine.this.deferMessage(message);
                    return true;
                case WifiStateMachine.CMD_ENABLE_RSSI_POLL:
                    if (!WifiStateMachine.this.mWifiConfigStore.enableRssiPollWhenAssociated) {
                        WifiStateMachine.this.mEnableRssiPolling = WifiStateMachine.DEBUG_PARSE;
                    } else {
                        WifiStateMachine.this.mEnableRssiPolling = message.arg1 == 1 ? true : WifiStateMachine.DEBUG_PARSE;
                    }
                    WifiStateMachine.access$17508(WifiStateMachine.this);
                    if (WifiStateMachine.this.mEnableRssiPolling) {
                        WifiStateMachine.this.fetchRssiLinkSpeedAndFrequencyNative();
                        WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.this.obtainMessage(WifiStateMachine.CMD_RSSI_POLL, WifiStateMachine.this.mRssiPollToken, 0), 3000L);
                    } else {
                        WifiStateMachine.this.cleanWifiScore();
                    }
                    return true;
                case WifiStateMachine.CMD_RSSI_POLL:
                    if (message.arg1 == WifiStateMachine.this.mRssiPollToken) {
                        if (WifiStateMachine.this.mWifiConfigStore.enableChipWakeUpWhenAssociated) {
                            if (WifiStateMachine.VVDBG) {
                                WifiStateMachine.this.log(" get link layer stats " + WifiStateMachine.this.mWifiLinkLayerStatsSupported);
                            }
                            WifiLinkLayerStats stats = WifiStateMachine.this.getWifiLinkLayerStats(WifiStateMachine.VDBG);
                            if (stats != null && WifiStateMachine.this.mWifiInfo.getRssi() != -127 && (stats.rssi_mgmt == 0 || stats.beacon_rx == 0)) {
                                stats = null;
                            }
                            WifiStateMachine.this.fetchRssiLinkSpeedAndFrequencyNative();
                            WifiStateMachine.this.calculateWifiScore(stats);
                        }
                        WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.this.obtainMessage(WifiStateMachine.CMD_RSSI_POLL, WifiStateMachine.this.mRssiPollToken, 0), 3000L);
                        if (WifiStateMachine.DBG) {
                            WifiStateMachine.this.sendRssiChangeBroadcast(WifiStateMachine.this.mWifiInfo.getRssi());
                        }
                    }
                    return true;
                case WifiStateMachine.CMD_DELAYED_NETWORK_DISCONNECT:
                    if (!WifiStateMachine.this.linkDebouncing && WifiStateMachine.this.mWifiConfigStore.enableLinkDebouncing) {
                        WifiStateMachine.this.loge("CMD_DELAYED_NETWORK_DISCONNECT and not debouncing - ignore " + message.arg1);
                        return true;
                    }
                    WifiStateMachine.this.loge("CMD_DELAYED_NETWORK_DISCONNECT and debouncing - disconnect " + message.arg1);
                    WifiStateMachine.this.linkDebouncing = WifiStateMachine.DEBUG_PARSE;
                    WifiStateMachine.this.handleNetworkDisconnect();
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectedState);
                    return true;
                case WifiStateMachine.CMD_IP_CONFIGURATION_SUCCESSFUL:
                    WifiStateMachine.this.handleSuccessfulIpConfiguration();
                    WifiStateMachine.this.sendConnectedState();
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mConnectedState);
                    return true;
                case WifiStateMachine.CMD_IP_CONFIGURATION_LOST:
                    WifiStateMachine.this.getWifiLinkLayerStats(true);
                    WifiStateMachine.this.handleIpConfigurationLost();
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectingState);
                    return true;
                case WifiStateMachine.CMD_ASSOCIATED_BSSID:
                    if (((String) message.obj) != null) {
                        WifiStateMachine.this.mLastBssid = (String) message.obj;
                        WifiStateMachine.this.mWifiInfo.setBSSID((String) message.obj);
                    } else {
                        WifiStateMachine.this.loge("Associated command w/o BSSID");
                    }
                    return true;
                case WifiP2pServiceImpl.DISCONNECT_WIFI_REQUEST:
                    if (message.arg1 == 1) {
                        WifiStateMachine.this.mWifiNative.disconnect();
                        WifiStateMachine.this.mTemporarilyDisconnectWifi = true;
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectingState);
                    }
                    return true;
                case WifiMonitor.NETWORK_CONNECTION_EVENT:
                    return true;
                case 151553:
                    int netId = message.arg1;
                    if (WifiStateMachine.this.mWifiInfo.getNetworkId() != netId) {
                        return WifiStateMachine.DEBUG_PARSE;
                    }
                    break;
                case 151572:
                    RssiPacketCountInfo info = new RssiPacketCountInfo();
                    WifiStateMachine.this.fetchRssiLinkSpeedAndFrequencyNative();
                    info.rssi = WifiStateMachine.this.mWifiInfo.getRssi();
                    WifiStateMachine.this.fetchPktcntNative(info);
                    WifiStateMachine.this.replyToMessage(message, 151573, info);
                    return true;
                case 196612:
                    WifiStateMachine.this.handlePreDhcpSetup();
                    return true;
                case 196613:
                    WifiStateMachine.this.handlePostDhcpSetup();
                    if (message.arg1 == 1) {
                        if (WifiStateMachine.DBG) {
                            WifiStateMachine.this.log("WifiStateMachine DHCP successful");
                        }
                        WifiStateMachine.this.handleIPv4Success((DhcpResults) message.obj, 1);
                    } else if (message.arg1 == 2) {
                        if (WifiStateMachine.DBG) {
                            int count = -1;
                            WifiConfiguration config = WifiStateMachine.this.getCurrentWifiConfiguration();
                            if (config != null) {
                                count = config.numConnectionFailures;
                            }
                            WifiStateMachine.this.log("WifiStateMachine DHCP failure count=" + count);
                        }
                        WifiStateMachine.this.handleIPv4Failure(2);
                    }
                    return true;
                default:
                    return WifiStateMachine.DEBUG_PARSE;
            }
        }
    }

    class ObtainingIpState extends State {
        ObtainingIpState() {
        }

        public void enter() {
            if (WifiStateMachine.DBG) {
                String key = "";
                if (WifiStateMachine.this.getCurrentWifiConfiguration() != null) {
                    key = WifiStateMachine.this.getCurrentWifiConfiguration().configKey();
                }
                WifiStateMachine.this.log("enter ObtainingIpState netId=" + Integer.toString(WifiStateMachine.this.mLastNetworkId) + " " + key + "  roam=" + WifiStateMachine.this.mAutoRoaming + " static=" + WifiStateMachine.this.mWifiConfigStore.isUsingStaticIp(WifiStateMachine.this.mLastNetworkId) + " watchdog= " + WifiStateMachine.this.obtainingIpWatchdogCount);
            }
            WifiStateMachine.this.linkDebouncing = WifiStateMachine.DEBUG_PARSE;
            WifiStateMachine.this.setNetworkDetailedState(NetworkInfo.DetailedState.OBTAINING_IPADDR);
            WifiStateMachine.this.clearCurrentConfigBSSID("ObtainingIpAddress");
            try {
                WifiStateMachine.this.mNwService.enableIpv6(WifiStateMachine.this.mInterfaceName);
            } catch (RemoteException re) {
                WifiStateMachine.this.loge("Failed to enable IPv6: " + re);
            } catch (IllegalStateException e) {
                WifiStateMachine.this.loge("Failed to enable IPv6: " + e);
            }
            if (!WifiStateMachine.this.mWifiConfigStore.isUsingStaticIp(WifiStateMachine.this.mLastNetworkId)) {
                if (!WifiStateMachine.this.isRoaming()) {
                    WifiStateMachine.this.clearIPv4Address(WifiStateMachine.this.mInterfaceName);
                    WifiStateMachine.this.startDhcp();
                } else {
                    WifiStateMachine.this.renewDhcp();
                }
                WifiStateMachine.this.obtainingIpWatchdogCount++;
                WifiStateMachine.this.loge("Start Dhcp Watchdog " + WifiStateMachine.this.obtainingIpWatchdogCount);
                WifiStateMachine.this.getWifiLinkLayerStats(true);
                WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.this.obtainMessage(WifiStateMachine.CMD_OBTAINING_IP_ADDRESS_WATCHDOG_TIMER, WifiStateMachine.this.obtainingIpWatchdogCount, 0), 40000L);
                return;
            }
            WifiStateMachine.this.stopDhcp();
            StaticIpConfiguration config = WifiStateMachine.this.mWifiConfigStore.getStaticIpConfiguration(WifiStateMachine.this.mLastNetworkId);
            if (config.ipAddress == null) {
                WifiStateMachine.this.loge("Static IP lacks address");
                WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_STATIC_IP_FAILURE);
                return;
            }
            InterfaceConfiguration ifcg = new InterfaceConfiguration();
            ifcg.setLinkAddress(config.ipAddress);
            ifcg.setInterfaceUp();
            try {
                WifiStateMachine.this.mNwService.setInterfaceConfig(WifiStateMachine.this.mInterfaceName, ifcg);
                if (WifiStateMachine.DBG) {
                    WifiStateMachine.this.log("Static IP configuration succeeded");
                }
                DhcpResults dhcpResults = new DhcpResults(config);
                WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_STATIC_IP_SUCCESS, dhcpResults);
            } catch (RemoteException re2) {
                WifiStateMachine.this.loge("Static IP configuration failed: " + re2);
                WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_STATIC_IP_FAILURE);
            } catch (IllegalStateException e2) {
                WifiStateMachine.this.loge("Static IP configuration failed: " + e2);
                WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_STATIC_IP_FAILURE);
            }
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, getClass().getSimpleName());
            switch (message.what) {
                case WifiStateMachine.CMD_STATIC_IP_SUCCESS:
                    WifiStateMachine.this.handleIPv4Success((DhcpResults) message.obj, WifiStateMachine.CMD_STATIC_IP_SUCCESS);
                    return true;
                case WifiStateMachine.CMD_STATIC_IP_FAILURE:
                    WifiStateMachine.this.handleIPv4Failure(WifiStateMachine.CMD_STATIC_IP_FAILURE);
                    return true;
                case WifiStateMachine.CMD_START_SCAN:
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DEFERRED;
                    WifiStateMachine.this.deferMessage(message);
                    return true;
                case WifiStateMachine.CMD_SET_HIGH_PERF_MODE:
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DEFERRED;
                    WifiStateMachine.this.deferMessage(message);
                    return true;
                case WifiStateMachine.CMD_OBTAINING_IP_ADDRESS_WATCHDOG_TIMER:
                    if (message.arg1 == WifiStateMachine.this.obtainingIpWatchdogCount) {
                        WifiStateMachine.this.loge("ObtainingIpAddress: Watchdog Triggered, count=" + WifiStateMachine.this.obtainingIpWatchdogCount);
                        WifiStateMachine.this.handleIpConfigurationLost();
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectingState);
                    } else {
                        WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DISCARD;
                    }
                    return true;
                case WifiStateMachine.CMD_AUTO_CONNECT:
                case WifiStateMachine.CMD_AUTO_ROAM:
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DISCARD;
                    return true;
                case WifiStateMachine.CMD_AUTO_SAVE_NETWORK:
                case 151559:
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DEFERRED;
                    WifiStateMachine.this.deferMessage(message);
                    return true;
                default:
                    return WifiStateMachine.DEBUG_PARSE;
            }
        }
    }

    class VerifyingLinkState extends State {
        VerifyingLinkState() {
        }

        public void enter() {
            WifiStateMachine.this.log(getName() + " enter");
            WifiStateMachine.this.setNetworkDetailedState(NetworkInfo.DetailedState.VERIFYING_POOR_LINK);
            WifiStateMachine.this.mWifiConfigStore.updateStatus(WifiStateMachine.this.mLastNetworkId, NetworkInfo.DetailedState.VERIFYING_POOR_LINK);
            WifiStateMachine.this.sendNetworkStateChangeBroadcast(WifiStateMachine.this.mLastBssid);
            WifiStateMachine.this.mAutoRoaming = 0;
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, getClass().getSimpleName());
            switch (message.what) {
                case 135189:
                    WifiStateMachine.this.log(getName() + " POOR_LINK_DETECTED: no transition");
                    return true;
                case 135190:
                    WifiStateMachine.this.log(getName() + " GOOD_LINK_DETECTED: transition to captive portal check");
                    WifiStateMachine.this.log(getName() + " GOOD_LINK_DETECTED: transition to CONNECTED");
                    WifiStateMachine.this.sendConnectedState();
                    if (WifiStateMachine.this.mDhcpStateMachine != null) {
                        WifiStateMachine.this.mDhcpStateMachine.sendMessageDelayed(196616, 3000L);
                    }
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mConnectedState);
                    return true;
                default:
                    if (WifiStateMachine.DBG) {
                        WifiStateMachine.this.log(getName() + " what=" + message.what + " NOT_HANDLED");
                    }
                    return WifiStateMachine.DEBUG_PARSE;
            }
        }
    }

    private void sendConnectedState() {
        setNetworkDetailedState(NetworkInfo.DetailedState.CAPTIVE_PORTAL_CHECK);
        this.mWifiConfigStore.updateStatus(this.mLastNetworkId, NetworkInfo.DetailedState.CAPTIVE_PORTAL_CHECK);
        sendNetworkStateChangeBroadcast(this.mLastBssid);
        if (this.mWifiConfigStore.getLastSelectedConfiguration() != null && this.mNetworkAgent != null) {
            this.mNetworkAgent.explicitlySelected();
        }
        setNetworkDetailedState(NetworkInfo.DetailedState.CONNECTED);
        this.mWifiConfigStore.updateStatus(this.mLastNetworkId, NetworkInfo.DetailedState.CONNECTED);
        sendNetworkStateChangeBroadcast(this.mLastBssid);
    }

    class RoamingState extends State {
        boolean mAssociated;

        RoamingState() {
        }

        public void enter() {
            if (WifiStateMachine.DBG) {
                WifiStateMachine.this.log("RoamingState Enter mScreenOn=" + WifiStateMachine.this.mScreenOn);
            }
            WifiStateMachine.this.setScanAlarm(WifiStateMachine.DEBUG_PARSE);
            WifiStateMachine.this.roamWatchdogCount++;
            WifiStateMachine.this.loge("Start Roam Watchdog " + WifiStateMachine.this.roamWatchdogCount);
            WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.this.obtainMessage(WifiStateMachine.CMD_ROAM_WATCHDOG_TIMER, WifiStateMachine.this.roamWatchdogCount, 0), 15000L);
            this.mAssociated = WifiStateMachine.DEBUG_PARSE;
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, getClass().getSimpleName());
            switch (message.what) {
                case WifiStateMachine.CMD_START_SCAN:
                    WifiStateMachine.this.deferMessage(message);
                    break;
                case WifiStateMachine.CMD_SET_OPERATIONAL_MODE:
                    if (message.arg1 != 1) {
                        WifiStateMachine.this.deferMessage(message);
                    }
                    break;
                case WifiStateMachine.CMD_ROAM_WATCHDOG_TIMER:
                    if (WifiStateMachine.this.roamWatchdogCount == message.arg1) {
                        if (WifiStateMachine.DBG) {
                            WifiStateMachine.this.log("roaming watchdog! -> disconnect");
                        }
                        WifiStateMachine.access$21308(WifiStateMachine.this);
                        WifiStateMachine.this.handleNetworkDisconnect();
                        WifiStateMachine.this.mWifiNative.disconnect();
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectedState);
                    }
                    break;
                case WifiStateMachine.CMD_IP_CONFIGURATION_LOST:
                    WifiConfiguration config = WifiStateMachine.this.getCurrentWifiConfiguration();
                    if (config == null) {
                        return WifiStateMachine.DEBUG_PARSE;
                    }
                    WifiStateMachine.this.mWifiConfigStore.noteRoamingFailure(config, WifiConfiguration.ROAMING_FAILURE_IP_CONFIG);
                    return WifiStateMachine.DEBUG_PARSE;
                case WifiStateMachine.CMD_UNWANTED_NETWORK:
                    if (WifiStateMachine.DBG) {
                        WifiStateMachine.this.log("Roaming and CS doesnt want the network -> ignore");
                    }
                    return true;
                case 135189:
                    if (WifiStateMachine.DBG) {
                        WifiStateMachine.this.log("Roaming and Watchdog reports poor link -> ignore");
                    }
                    return true;
                case WifiMonitor.NETWORK_CONNECTION_EVENT:
                    if (this.mAssociated) {
                        if (WifiStateMachine.DBG) {
                            WifiStateMachine.this.log("roaming and Network connection established");
                        }
                        WifiStateMachine.this.mLastNetworkId = message.arg1;
                        WifiStateMachine.this.mLastBssid = (String) message.obj;
                        WifiStateMachine.this.mWifiInfo.setBSSID(WifiStateMachine.this.mLastBssid);
                        WifiStateMachine.this.mWifiInfo.setNetworkId(WifiStateMachine.this.mLastNetworkId);
                        WifiStateMachine.this.mWifiConfigStore.handleBSSIDBlackList(WifiStateMachine.this.mLastNetworkId, WifiStateMachine.this.mLastBssid, true);
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mObtainingIpState);
                    } else {
                        WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DISCARD;
                    }
                    break;
                case WifiMonitor.NETWORK_DISCONNECTION_EVENT:
                    String bssid = (String) message.obj;
                    String target = "";
                    if (WifiStateMachine.this.mTargetRoamBSSID != null) {
                        target = WifiStateMachine.this.mTargetRoamBSSID;
                    }
                    WifiStateMachine.this.log("NETWORK_DISCONNECTION_EVENT in roaming state BSSID=" + bssid + " target=" + target);
                    if (bssid != null && bssid.equals(WifiStateMachine.this.mTargetRoamBSSID)) {
                        WifiStateMachine.this.handleNetworkDisconnect();
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectedState);
                    }
                    break;
                case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT:
                    StateChangeResult stateChangeResult = (StateChangeResult) message.obj;
                    if (stateChangeResult.state == SupplicantState.DISCONNECTED || stateChangeResult.state == SupplicantState.INACTIVE || stateChangeResult.state == SupplicantState.INTERFACE_DISABLED) {
                        if (WifiStateMachine.DBG) {
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
                    break;
                case WifiMonitor.SSID_TEMP_DISABLED:
                    WifiStateMachine.this.loge("SSID_TEMP_DISABLED nid=" + Integer.toString(WifiStateMachine.this.mLastNetworkId) + " id=" + Integer.toString(message.arg1) + " isRoaming=" + WifiStateMachine.this.isRoaming() + " roam=" + Integer.toString(WifiStateMachine.this.mAutoRoaming));
                    if (message.arg1 != WifiStateMachine.this.mLastNetworkId) {
                        return WifiStateMachine.DEBUG_PARSE;
                    }
                    WifiConfiguration config2 = WifiStateMachine.this.getCurrentWifiConfiguration();
                    if (config2 != null) {
                        WifiStateMachine.this.mWifiConfigStore.noteRoamingFailure(config2, WifiConfiguration.ROAMING_FAILURE_AUTH_FAILURE);
                    }
                    WifiStateMachine.this.handleNetworkDisconnect();
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectingState);
                    return WifiStateMachine.DEBUG_PARSE;
                default:
                    return WifiStateMachine.DEBUG_PARSE;
            }
            return true;
        }

        public void exit() {
            WifiStateMachine.this.loge("WifiStateMachine: Leaving Roaming state");
        }
    }

    class ConnectedState extends State {
        ConnectedState() {
        }

        public void enter() throws Throwable {
            WifiStateMachine.this.updateDefaultRouteMacAddress(1000);
            if (WifiStateMachine.DBG) {
                WifiStateMachine.this.log("ConnectedState Enter  mScreenOn=" + WifiStateMachine.this.mScreenOn + " scanperiod=" + Integer.toString(WifiStateMachine.this.mWifiConfigStore.associatedPartialScanPeriodMilli));
            }
            if (WifiStateMachine.this.mScreenOn && WifiStateMachine.this.mWifiConfigStore.enableAutoJoinScanWhenAssociated) {
                WifiStateMachine.this.startDelayedScan(WifiStateMachine.this.mWifiConfigStore.associatedPartialScanPeriodMilli, null, null);
            }
            WifiStateMachine.this.registerConnected();
            WifiStateMachine.this.lastConnectAttempt = 0L;
            WifiStateMachine.this.targetWificonfiguration = null;
            WifiStateMachine.this.linkDebouncing = WifiStateMachine.DEBUG_PARSE;
            WifiStateMachine.this.mAutoRoaming = 0;
            if (WifiStateMachine.this.testNetworkDisconnect) {
                WifiStateMachine.access$22208(WifiStateMachine.this);
                WifiStateMachine.this.loge("ConnectedState Enter start disconnect test " + WifiStateMachine.this.testNetworkDisconnectCounter);
                WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.this.obtainMessage(WifiStateMachine.CMD_TEST_NETWORK_DISCONNECT, WifiStateMachine.this.testNetworkDisconnectCounter, 0), 15000L);
            }
            WifiStateMachine.this.mWifiConfigStore.enableAllNetworks();
            WifiStateMachine.this.mLastDriverRoamAttempt = 0L;
        }

        public boolean processMessage(Message message) {
            WifiConfiguration config;
            WifiConfiguration config2;
            WifiStateMachine.this.logStateAndMessage(message, getClass().getSimpleName());
            switch (message.what) {
                case WifiStateMachine.CMD_TEST_NETWORK_DISCONNECT:
                    if (message.arg1 == WifiStateMachine.this.testNetworkDisconnectCounter) {
                        WifiStateMachine.this.mWifiNative.disconnect();
                    }
                    return true;
                case WifiStateMachine.CMD_UNWANTED_NETWORK:
                    if (message.arg1 == 0) {
                        WifiStateMachine.this.mWifiConfigStore.handleBadNetworkDisconnectReport(WifiStateMachine.this.mLastNetworkId, WifiStateMachine.this.mWifiInfo);
                        WifiStateMachine.this.mWifiNative.disconnect();
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectingState);
                    } else if (message.arg1 == 1 && (config2 = WifiStateMachine.this.getCurrentWifiConfiguration()) != null) {
                        config2.numNoInternetAccessReports++;
                    }
                    return true;
                case WifiStateMachine.CMD_AUTO_ROAM:
                    WifiStateMachine.this.mLastDriverRoamAttempt = 0L;
                    ScanResult candidate = (ScanResult) message.obj;
                    String bssid = "any";
                    if (candidate != null && candidate.is5GHz()) {
                        bssid = candidate.BSSID;
                    }
                    int netId = WifiStateMachine.this.mLastNetworkId;
                    WifiConfiguration config3 = WifiStateMachine.this.getCurrentWifiConfiguration();
                    if (config3 != null) {
                        WifiStateMachine.this.loge("CMD_AUTO_ROAM sup state " + WifiStateMachine.this.mSupplicantStateTracker.getSupplicantStateName() + " my state " + WifiStateMachine.this.getCurrentState().getName() + " nid=" + Integer.toString(netId) + " config " + config3.configKey() + " roam=" + Integer.toString(message.arg2) + " to " + bssid + " targetRoamBSSID " + WifiStateMachine.this.mTargetRoamBSSID);
                        if (WifiStateMachine.this.autoRoamSetBSSID(config3, bssid) || WifiStateMachine.this.linkDebouncing) {
                            WifiStateMachine.this.mWifiConfigStore.enableNetworkWithoutBroadcast(netId, WifiStateMachine.DEBUG_PARSE);
                            boolean ret = WifiStateMachine.DEBUG_PARSE;
                            if (WifiStateMachine.this.mLastNetworkId != netId) {
                                if (WifiStateMachine.this.mWifiConfigStore.selectNetwork(netId) && WifiStateMachine.this.mWifiNative.reconnect()) {
                                    ret = true;
                                }
                            } else {
                                ret = WifiStateMachine.this.mWifiNative.reassociate();
                            }
                            if (ret) {
                                WifiStateMachine.this.lastConnectAttempt = System.currentTimeMillis();
                                WifiStateMachine.this.targetWificonfiguration = WifiStateMachine.this.mWifiConfigStore.getWifiConfiguration(netId);
                                WifiStateMachine.this.mAutoRoaming = message.arg2;
                                WifiStateMachine.this.transitionTo(WifiStateMachine.this.mRoamingState);
                            } else {
                                WifiStateMachine.this.loge("Failed to connect config: " + config3 + " netId: " + netId);
                                WifiStateMachine.this.replyToMessage(message, 151554, 0);
                                WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_FAIL;
                            }
                        } else {
                            WifiStateMachine.this.loge("AUTO_ROAM nothing to do");
                            WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DISCARD;
                        }
                    } else {
                        WifiStateMachine.this.loge("AUTO_ROAM and no config, bail out...");
                    }
                    return true;
                case WifiStateMachine.CMD_ASSOCIATED_BSSID:
                    WifiStateMachine.this.mLastDriverRoamAttempt = System.currentTimeMillis();
                    String toBSSID = (String) message.obj;
                    if (toBSSID != null && !toBSSID.equals(WifiStateMachine.this.mWifiInfo.getBSSID())) {
                        WifiStateMachine.this.mWifiConfigStore.driverRoamedFrom(WifiStateMachine.this.mWifiInfo);
                    }
                    return WifiStateMachine.DEBUG_PARSE;
                case WifiStateMachine.CMD_NETWORK_STATUS:
                    if (message.arg1 == 1 && (config = WifiStateMachine.this.getCurrentWifiConfiguration()) != null) {
                        config.numNoInternetAccessReports = 0;
                        config.validatedInternetAccess = true;
                    }
                    return true;
                case 135189:
                    if (WifiStateMachine.DBG) {
                        WifiStateMachine.this.log("Watchdog reports poor link");
                    }
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mVerifyingLinkState);
                    return true;
                case WifiMonitor.NETWORK_DISCONNECTION_EVENT:
                    long lastRoam = 0;
                    if (WifiStateMachine.this.mLastDriverRoamAttempt != 0) {
                        lastRoam = System.currentTimeMillis() - WifiStateMachine.this.mLastDriverRoamAttempt;
                        WifiStateMachine.this.mLastDriverRoamAttempt = 0L;
                    }
                    WifiConfiguration config4 = WifiStateMachine.this.getCurrentWifiConfiguration();
                    if (!WifiStateMachine.this.mScreenOn || WifiStateMachine.this.linkDebouncing || config4 == null || config4.autoJoinStatus != 0 || WifiStateMachine.this.mWifiConfigStore.isLastSelectedConfiguration(config4) || ((message.arg2 == 3 && (lastRoam <= 0 || lastRoam >= 2000)) || ((!ScanResult.is24GHz(WifiStateMachine.this.mWifiInfo.getFrequency()) || WifiStateMachine.this.mWifiInfo.getRssi() <= WifiConfiguration.BAD_RSSI_24) && (!ScanResult.is5GHz(WifiStateMachine.this.mWifiInfo.getFrequency()) || WifiStateMachine.this.mWifiInfo.getRssi() <= WifiConfiguration.BAD_RSSI_5)))) {
                        if (WifiStateMachine.DBG) {
                            int ajst = config4 != null ? config4.autoJoinStatus : -1;
                            WifiStateMachine.this.log("NETWORK_DISCONNECTION_EVENT in connected state BSSID=" + WifiStateMachine.this.mWifiInfo.getBSSID() + " RSSI=" + WifiStateMachine.this.mWifiInfo.getRssi() + " freq=" + WifiStateMachine.this.mWifiInfo.getFrequency() + " was debouncing=" + WifiStateMachine.this.linkDebouncing + " reason=" + message.arg2 + " ajst=" + ajst);
                        }
                        return true;
                    }
                    WifiStateMachine.this.startScanForConfiguration(WifiStateMachine.this.getCurrentWifiConfiguration(), WifiStateMachine.DEBUG_PARSE);
                    WifiStateMachine.this.linkDebouncing = true;
                    WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.this.obtainMessage(WifiStateMachine.CMD_DELAYED_NETWORK_DISCONNECT, 0, WifiStateMachine.this.mLastNetworkId), 7000L);
                    if (WifiStateMachine.DBG) {
                        WifiStateMachine.this.log("NETWORK_DISCONNECTION_EVENT in connected state BSSID=" + WifiStateMachine.this.mWifiInfo.getBSSID() + " RSSI=" + WifiStateMachine.this.mWifiInfo.getRssi() + " freq=" + WifiStateMachine.this.mWifiInfo.getFrequency() + " reason=" + message.arg2 + " -> debounce");
                    }
                    return true;
                default:
                    return WifiStateMachine.DEBUG_PARSE;
            }
        }

        public void exit() {
            WifiStateMachine.this.loge("WifiStateMachine: Leaving Connected state");
            WifiStateMachine.this.setScanAlarm(WifiStateMachine.DEBUG_PARSE);
            WifiStateMachine.this.mLastDriverRoamAttempt = 0L;
        }
    }

    class DisconnectingState extends State {
        DisconnectingState() {
        }

        public void enter() {
            if (WifiStateMachine.PDBG) {
                WifiStateMachine.this.loge(" Enter DisconnectingState State scan interval " + WifiStateMachine.this.mFrameworkScanIntervalMs + " mEnableBackgroundScan= " + WifiStateMachine.this.mEnableBackgroundScan + " screenOn=" + WifiStateMachine.this.mScreenOn);
            }
            WifiStateMachine.this.disconnectingWatchdogCount++;
            WifiStateMachine.this.loge("Start Disconnecting Watchdog " + WifiStateMachine.this.disconnectingWatchdogCount);
            WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.this.obtainMessage(WifiStateMachine.CMD_DISCONNECTING_WATCHDOG_TIMER, WifiStateMachine.this.disconnectingWatchdogCount, 0), 5000L);
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, getClass().getSimpleName());
            switch (message.what) {
                case WifiStateMachine.CMD_START_SCAN:
                    WifiStateMachine.this.deferMessage(message);
                    break;
                case WifiStateMachine.CMD_SET_OPERATIONAL_MODE:
                    if (message.arg1 != 1) {
                        WifiStateMachine.this.deferMessage(message);
                    }
                    break;
                case WifiStateMachine.CMD_DISCONNECTING_WATCHDOG_TIMER:
                    if (WifiStateMachine.this.disconnectingWatchdogCount == message.arg1) {
                        if (WifiStateMachine.DBG) {
                            WifiStateMachine.this.log("disconnecting watchdog! -> disconnect");
                        }
                        WifiStateMachine.this.handleNetworkDisconnect();
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectedState);
                    }
                    break;
                case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT:
                    WifiStateMachine.this.deferMessage(message);
                    WifiStateMachine.this.handleNetworkDisconnect();
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectedState);
                    break;
            }
            return true;
        }
    }

    class DisconnectedState extends State {
        DisconnectedState() {
        }

        public void enter() {
            if (WifiStateMachine.this.mTemporarilyDisconnectWifi) {
                WifiStateMachine.this.mWifiP2pChannel.sendMessage(WifiP2pServiceImpl.DISCONNECT_WIFI_RESPONSE);
                return;
            }
            WifiStateMachine.this.mFrameworkScanIntervalMs = Settings.Global.getLong(WifiStateMachine.this.mContext.getContentResolver(), "wifi_framework_scan_interval_ms", WifiStateMachine.this.mDefaultFrameworkScanIntervalMs);
            if (WifiStateMachine.PDBG) {
                WifiStateMachine.this.loge(" Enter disconnected State scan interval " + WifiStateMachine.this.mFrameworkScanIntervalMs + " mEnableBackgroundScan= " + WifiStateMachine.this.mEnableBackgroundScan + " screenOn=" + WifiStateMachine.this.mScreenOn + " mFrameworkScanIntervalMs=" + WifiStateMachine.this.mFrameworkScanIntervalMs);
            }
            WifiStateMachine.this.mAutoRoaming = 0;
            if (WifiStateMachine.this.mScreenOn) {
                WifiStateMachine.this.startDelayedScan(WifiStateMachine.this.mDisconnectedScanPeriodMs, null, null);
            } else if (WifiStateMachine.this.mEnableBackgroundScan) {
                if (!WifiStateMachine.this.mIsScanOngoing) {
                    WifiStateMachine.this.enableBackgroundScan(true);
                }
            } else {
                WifiStateMachine.this.setScanAlarm(true);
            }
            if (!WifiStateMachine.this.mP2pConnected.get() && WifiStateMachine.this.mWifiConfigStore.getConfiguredNetworks().size() == 0) {
                WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.this.obtainMessage(WifiStateMachine.CMD_NO_NETWORKS_PERIODIC_SCAN, WifiStateMachine.access$23804(WifiStateMachine.this), 0), WifiStateMachine.this.mSupplicantScanIntervalMs);
            }
            WifiStateMachine.this.mDisconnectedTimeStamp = System.currentTimeMillis();
        }

        public boolean processMessage(Message message) {
            boolean z = WifiStateMachine.DEBUG_PARSE;
            boolean ret = true;
            WifiStateMachine.this.logStateAndMessage(message, getClass().getSimpleName());
            switch (message.what) {
                case WifiStateMachine.CMD_REMOVE_NETWORK:
                case 151556:
                    WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.this.obtainMessage(WifiStateMachine.CMD_NO_NETWORKS_PERIODIC_SCAN, WifiStateMachine.access$23804(WifiStateMachine.this), 0), WifiStateMachine.this.mSupplicantScanIntervalMs);
                    ret = WifiStateMachine.DEBUG_PARSE;
                    return ret;
                case WifiStateMachine.CMD_START_SCAN:
                    if (WifiStateMachine.this.checkOrDeferScanAllowed(message)) {
                        if (WifiStateMachine.this.mEnableBackgroundScan) {
                            WifiStateMachine.this.enableBackgroundScan(WifiStateMachine.DEBUG_PARSE);
                        }
                        if (message.arg1 == WifiStateMachine.SCAN_ALARM_SOURCE) {
                            int period = WifiStateMachine.this.mDisconnectedScanPeriodMs;
                            if (WifiStateMachine.this.mP2pConnected.get()) {
                                period = (int) Settings.Global.getLong(WifiStateMachine.this.mContext.getContentResolver(), "wifi_scan_interval_p2p_connected_ms", WifiStateMachine.this.mDisconnectedScanPeriodMs);
                            }
                            if (WifiStateMachine.this.checkAndRestartDelayedScan(message.arg2, true, period, null, null)) {
                                WifiStateMachine.this.handleScanRequest(1, message);
                                ret = true;
                            } else {
                                WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_OBSOLETE;
                                WifiStateMachine.this.loge("WifiStateMachine Disconnected CMD_START_SCAN source " + message.arg1 + " " + message.arg2 + ", " + WifiStateMachine.this.mDelayedScanCounter + " -> obsolete");
                                return true;
                            }
                        } else {
                            ret = WifiStateMachine.DEBUG_PARSE;
                        }
                        return ret;
                    }
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_REFUSED;
                    return true;
                case WifiStateMachine.CMD_SET_OPERATIONAL_MODE:
                    if (message.arg1 != 1) {
                        WifiStateMachine.this.mOperationalMode = message.arg1;
                        WifiStateMachine.this.mWifiConfigStore.disableAllNetworks();
                        if (WifiStateMachine.this.mOperationalMode == 3) {
                            WifiStateMachine.this.mWifiP2pChannel.sendMessage(WifiStateMachine.CMD_DISABLE_P2P_REQ);
                            WifiStateMachine.this.setWifiState(1);
                        }
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mScanModeState);
                    }
                    WifiStateMachine.this.mWifiConfigStore.setLastSelectedConfiguration(-1);
                    return ret;
                case WifiStateMachine.CMD_RECONNECT:
                case WifiStateMachine.CMD_REASSOCIATE:
                    break;
                case WifiStateMachine.CMD_NO_NETWORKS_PERIODIC_SCAN:
                    if (!WifiStateMachine.this.mP2pConnected.get() && message.arg1 == WifiStateMachine.this.mPeriodicScanToken && WifiStateMachine.this.mWifiConfigStore.getConfiguredNetworks().size() == 0) {
                        WifiStateMachine.this.startScan(-1, -1, null, null);
                        WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.this.obtainMessage(WifiStateMachine.CMD_NO_NETWORKS_PERIODIC_SCAN, WifiStateMachine.access$23804(WifiStateMachine.this), 0), WifiStateMachine.this.mSupplicantScanIntervalMs);
                    }
                    return ret;
                case WifiStateMachine.CMD_SCREEN_STATE_CHANGED:
                    WifiStateMachine wifiStateMachine = WifiStateMachine.this;
                    if (message.arg1 != 0) {
                        z = true;
                    }
                    wifiStateMachine.handleScreenStateChanged(z, true);
                    return ret;
                case WifiP2pServiceImpl.P2P_CONNECTION_CHANGED:
                    NetworkInfo info = (NetworkInfo) message.obj;
                    WifiStateMachine.this.mP2pConnected.set(info.isConnected());
                    if (WifiStateMachine.this.mP2pConnected.get()) {
                        int defaultInterval = WifiStateMachine.this.mContext.getResources().getInteger(R.integer.config_autoPowerModeAnyMotionSensor);
                        long scanIntervalMs = Settings.Global.getLong(WifiStateMachine.this.mContext.getContentResolver(), "wifi_scan_interval_p2p_connected_ms", defaultInterval);
                        WifiStateMachine.this.mWifiNative.setScanInterval(((int) scanIntervalMs) / 1000);
                    } else if (WifiStateMachine.this.mWifiConfigStore.getConfiguredNetworks().size() == 0) {
                        if (WifiStateMachine.DBG) {
                            WifiStateMachine.this.log("Turn on scanning after p2p disconnected");
                        }
                        WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.this.obtainMessage(WifiStateMachine.CMD_NO_NETWORKS_PERIODIC_SCAN, WifiStateMachine.access$23804(WifiStateMachine.this), 0), WifiStateMachine.this.mSupplicantScanIntervalMs);
                    } else {
                        WifiStateMachine.this.startDelayedScan(WifiStateMachine.this.mDisconnectedScanPeriodMs, null, null);
                    }
                    break;
                case WifiMonitor.NETWORK_DISCONNECTION_EVENT:
                    return ret;
                case WifiMonitor.SCAN_RESULTS_EVENT:
                    if (WifiStateMachine.this.mEnableBackgroundScan && WifiStateMachine.this.mIsScanOngoing) {
                        WifiStateMachine.this.enableBackgroundScan(true);
                    }
                    ret = WifiStateMachine.DEBUG_PARSE;
                    return ret;
                case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT:
                    StateChangeResult stateChangeResult = (StateChangeResult) message.obj;
                    if (WifiStateMachine.DBG) {
                        WifiStateMachine.this.loge("SUPPLICANT_STATE_CHANGE_EVENT state=" + stateChangeResult.state + " -> state= " + WifiInfo.getDetailedStateOf(stateChangeResult.state) + " debouncing=" + WifiStateMachine.this.linkDebouncing);
                    }
                    WifiStateMachine.this.setNetworkDetailedState(WifiInfo.getDetailedStateOf(stateChangeResult.state));
                    ret = WifiStateMachine.DEBUG_PARSE;
                    return ret;
                default:
                    ret = WifiStateMachine.DEBUG_PARSE;
                    return ret;
            }
            if (!WifiStateMachine.this.mTemporarilyDisconnectWifi) {
                ret = WifiStateMachine.DEBUG_PARSE;
            }
            return ret;
        }

        public void exit() {
            if (WifiStateMachine.this.mEnableBackgroundScan) {
                WifiStateMachine.this.enableBackgroundScan(WifiStateMachine.DEBUG_PARSE);
            }
            WifiStateMachine.this.setScanAlarm(WifiStateMachine.DEBUG_PARSE);
        }
    }

    class WpsRunningState extends State {
        private Message mSourceMessage;

        WpsRunningState() {
        }

        public void enter() {
            this.mSourceMessage = Message.obtain(WifiStateMachine.this.getCurrentMessage());
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, getClass().getSimpleName());
            switch (message.what) {
                case WifiStateMachine.CMD_STOP_DRIVER:
                case WifiStateMachine.CMD_ENABLE_NETWORK:
                case WifiStateMachine.CMD_SET_OPERATIONAL_MODE:
                case WifiStateMachine.CMD_RECONNECT:
                case WifiStateMachine.CMD_REASSOCIATE:
                case 151553:
                    WifiStateMachine.this.deferMessage(message);
                    return true;
                case WifiStateMachine.CMD_START_SCAN:
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DISCARD;
                    return true;
                case WifiStateMachine.CMD_AUTO_CONNECT:
                case WifiStateMachine.CMD_AUTO_ROAM:
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DISCARD;
                    return true;
                case WifiMonitor.NETWORK_CONNECTION_EVENT:
                    WifiStateMachine.this.replyToMessage(this.mSourceMessage, 151565);
                    this.mSourceMessage.recycle();
                    this.mSourceMessage = null;
                    WifiStateMachine.this.deferMessage(message);
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectedState);
                    return true;
                case WifiMonitor.NETWORK_DISCONNECTION_EVENT:
                    if (WifiStateMachine.DBG) {
                        WifiStateMachine.this.log("Network connection lost");
                    }
                    WifiStateMachine.this.handleNetworkDisconnect();
                    return true;
                case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT:
                case WifiMonitor.WPS_SUCCESS_EVENT:
                    return true;
                case WifiMonitor.AUTHENTICATION_FAILURE_EVENT:
                    if (!WifiStateMachine.DBG) {
                        return true;
                    }
                    WifiStateMachine.this.log("Ignore auth failure during WPS connection");
                    return true;
                case WifiMonitor.WPS_FAIL_EVENT:
                    if (message.arg1 != 0 || message.arg2 != 0) {
                        WifiStateMachine.this.replyToMessage(this.mSourceMessage, 151564, message.arg1);
                        this.mSourceMessage.recycle();
                        this.mSourceMessage = null;
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectedState);
                        return true;
                    }
                    if (!WifiStateMachine.DBG) {
                        return true;
                    }
                    WifiStateMachine.this.log("Ignore unspecified fail event during WPS connection");
                    return true;
                case WifiMonitor.WPS_OVERLAP_EVENT:
                    WifiStateMachine.this.replyToMessage(this.mSourceMessage, 151564, 3);
                    this.mSourceMessage.recycle();
                    this.mSourceMessage = null;
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectedState);
                    return true;
                case WifiMonitor.WPS_TIMEOUT_EVENT:
                    WifiStateMachine.this.replyToMessage(this.mSourceMessage, 151564, 7);
                    this.mSourceMessage.recycle();
                    this.mSourceMessage = null;
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectedState);
                    return true;
                case WifiMonitor.ASSOCIATION_REJECTION_EVENT:
                    if (!WifiStateMachine.DBG) {
                        return true;
                    }
                    WifiStateMachine.this.log("Ignore Assoc reject event during WPS Connection");
                    return true;
                case 151562:
                    WifiStateMachine.this.replyToMessage(message, 151564, 1);
                    return true;
                case 151566:
                    if (WifiStateMachine.this.mWifiNative.cancelWps()) {
                        WifiStateMachine.this.replyToMessage(message, 151568);
                    } else {
                        WifiStateMachine.this.replyToMessage(message, 151567, 0);
                    }
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectedState);
                    return true;
                default:
                    return WifiStateMachine.DEBUG_PARSE;
            }
        }

        public void exit() throws Throwable {
            if (WifiStateMachine.this.mDhcpStateMachine != null) {
                if (WifiStateMachine.DBG) {
                    WifiStateMachine.this.log("reset connection");
                }
                WifiStateMachine.this.handleNetworkDisconnect();
            }
            WifiStateMachine.this.mWifiConfigStore.enableAllNetworks();
            WifiStateMachine.this.mWifiConfigStore.loadConfiguredNetworks();
        }
    }

    class SoftApStartingState extends State {
        SoftApStartingState() {
        }

        public void enter() {
            Message message = WifiStateMachine.this.getCurrentMessage();
            if (message.what == WifiStateMachine.CMD_START_AP) {
                WifiConfiguration config = (WifiConfiguration) message.obj;
                if (config == null) {
                    WifiStateMachine.this.mWifiApConfigChannel.sendMessage(WifiStateMachine.CMD_REQUEST_AP_CONFIG);
                    return;
                } else {
                    WifiStateMachine.this.mWifiApConfigChannel.sendMessage(WifiStateMachine.CMD_SET_AP_CONFIG, config);
                    WifiStateMachine.this.startSoftApWithConfig(config);
                    return;
                }
            }
            throw new RuntimeException("Illegal transition to SoftApStartingState: " + message);
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, getClass().getSimpleName());
            switch (message.what) {
                case WifiStateMachine.CMD_START_SUPPLICANT:
                case WifiStateMachine.CMD_STOP_SUPPLICANT:
                case WifiStateMachine.CMD_START_DRIVER:
                case WifiStateMachine.CMD_STOP_DRIVER:
                case WifiStateMachine.CMD_START_AP:
                case WifiStateMachine.CMD_STOP_AP:
                case WifiStateMachine.CMD_TETHER_STATE_CHANGE:
                case WifiStateMachine.CMD_SET_OPERATIONAL_MODE:
                case WifiStateMachine.CMD_SET_COUNTRY_CODE:
                case WifiStateMachine.CMD_START_PACKET_FILTERING:
                case WifiStateMachine.CMD_STOP_PACKET_FILTERING:
                case WifiStateMachine.CMD_SET_FREQUENCY_BAND:
                    WifiStateMachine.this.deferMessage(message);
                    return true;
                case WifiStateMachine.CMD_START_AP_SUCCESS:
                    WifiStateMachine.this.mWifiMonitor.startMonitoring();
                    return true;
                case WifiStateMachine.CMD_START_AP_FAILURE:
                    WifiStateMachine.this.setWifiApState(14);
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mInitialState);
                    return true;
                case WifiStateMachine.CMD_RESPONSE_AP_CONFIG:
                    WifiConfiguration config = (WifiConfiguration) message.obj;
                    if (config != null) {
                        WifiStateMachine.this.startSoftApWithConfig(config);
                    } else {
                        WifiStateMachine.this.loge("Softap config is null!");
                        WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_START_AP_FAILURE);
                    }
                    return true;
                case WifiMonitor.SUP_CONNECTION_EVENT:
                    if (WifiStateMachine.DBG) {
                        WifiStateMachine.this.log("Hostapd connection established");
                    }
                    WifiStateMachine.this.mSupplicantRestartCount = 0;
                    WifiStateMachine.this.setWifiApState(13);
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mSoftApStartedState);
                    return true;
                case WifiMonitor.SUP_DISCONNECTION_EVENT:
                    if (WifiStateMachine.access$4904(WifiStateMachine.this) > 5) {
                        WifiStateMachine.this.loge("Failed " + WifiStateMachine.this.mSupplicantRestartCount + " times to reconnect hostapd");
                        WifiStateMachine.this.mSupplicantRestartCount = 0;
                        WifiStateMachine.this.setWifiApState(14);
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mInitialState);
                    } else {
                        WifiStateMachine.this.loge("Failed to setup control channel, connect to hostapd");
                        WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.CMD_START_AP_SUCCESS, 5000L);
                    }
                    return true;
                default:
                    return WifiStateMachine.DEBUG_PARSE;
            }
        }
    }

    private String getIpByMacAddress(String macAddress) throws Throwable {
        BufferedReader reader;
        String ipAddress = null;
        BufferedReader reader2 = null;
        try {
            try {
                reader = new BufferedReader(new FileReader("/proc/net/arp"));
            } catch (Throwable th) {
                th = th;
            }
            try {
                reader.readLine();
                while (true) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    String[] tokens = line.split("[ ]+");
                    if (tokens.length >= 6) {
                        String ip = tokens[0];
                        String mac = tokens[3];
                        if (macAddress.equals(mac)) {
                            ipAddress = ip;
                            break;
                        }
                    }
                }
                if (ipAddress == null) {
                    loge("Did not find remote ipAddress for {" + macAddress + "} in /proc/net/arp");
                }
                if (reader != null) {
                    try {
                        reader.close();
                        reader2 = reader;
                    } catch (IOException e) {
                        reader2 = reader;
                    }
                } else {
                    reader2 = reader;
                }
            } catch (FileNotFoundException e2) {
                reader2 = reader;
                loge("Could not open /proc/net/arp to lookup ip address");
                if (reader2 != null) {
                    try {
                        reader2.close();
                    } catch (IOException e3) {
                    }
                }
            } catch (IOException e4) {
                reader2 = reader;
                loge("Could not read /proc/net/arp to lookup ip address");
                if (reader2 != null) {
                    try {
                        reader2.close();
                    } catch (IOException e5) {
                    }
                }
            } catch (Throwable th2) {
                th = th2;
                reader2 = reader;
                if (reader2 != null) {
                    try {
                        reader2.close();
                    } catch (IOException e6) {
                    }
                }
                throw th;
            }
        } catch (FileNotFoundException e7) {
        } catch (IOException e8) {
        }
        return ipAddress;
    }

    String getHostname(String macAddress) throws Throwable {
        String hostname = null;
        String ipAddress = getIpByMacAddress(macAddress);
        if (ipAddress == null) {
            return null;
        }
        try {
            hostname = InetAddress.getByName(ipAddress).getHostName();
        } catch (Exception e) {
            loge("getHostname error " + e);
        }
        return hostname;
    }

    class SoftApStartedState extends State {
        SoftApStartedState() {
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, getClass().getSimpleName());
            switch (message.what) {
                case WifiStateMachine.CMD_START_SUPPLICANT:
                    WifiStateMachine.this.loge("Cannot start supplicant with a running soft AP");
                    WifiStateMachine.this.setWifiState(4);
                    return true;
                case WifiStateMachine.CMD_START_AP:
                    return true;
                case WifiStateMachine.CMD_STOP_AP:
                    if (WifiStateMachine.DBG) {
                        WifiStateMachine.this.log("Stopping Soft AP");
                    }
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mSoftApStoppingState);
                    return true;
                case WifiStateMachine.CMD_TETHER_STATE_CHANGE:
                    TetherStateChange stateChange = (TetherStateChange) message.obj;
                    if (WifiStateMachine.this.startTethering(stateChange.available)) {
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mTetheringState);
                    }
                    return true;
                case WifiMonitor.AP_STA_DISCONNECTED_EVENT:
                    WifiP2pDevice disconnected_device = (WifiP2pDevice) message.obj;
                    WifiStateMachine.this.logi("AP_STA_DISCONNECTED_EVENT : " + disconnected_device.toString());
                    if (WifiStateMachine.this.mPeers.remove(disconnected_device)) {
                        WifiStateMachine.this.sendWifiApClientChangedBroadcast();
                    }
                    return true;
                case WifiMonitor.AP_STA_CONNECTED_EVENT:
                    final WifiP2pDevice connected_device = (WifiP2pDevice) message.obj;
                    WifiStateMachine.this.logi("AP_STA_CONNECTED_EVENT : " + connected_device.toString());
                    WifiStateMachine.this.mPeers.updateSupplicantDetails(connected_device);
                    Thread t = new Thread() {
                        @Override
                        public void run() throws Throwable {
                            int retryCount = 10;
                            while (true) {
                                String hostName = WifiStateMachine.this.getHostname(connected_device.deviceAddress);
                                if (hostName != null) {
                                    WifiStateMachine.this.loge("hostname " + hostName);
                                    connected_device.deviceName = hostName;
                                    WifiStateMachine.this.mDevNameHandler.obtainMessage(1, connected_device).sendToTarget();
                                    return;
                                } else {
                                    try {
                                        Thread.sleep(1000L);
                                    } catch (InterruptedException e) {
                                    }
                                    int retryCount2 = retryCount - 1;
                                    if (retryCount <= 0) {
                                        return;
                                    } else {
                                        retryCount = retryCount2;
                                    }
                                }
                            }
                        }
                    };
                    t.start();
                    return true;
                case WifiMonitor.AP_DISCONNECTION_EVENT:
                    WifiStateMachine.this.loge("Connection lost, restart hostapd");
                    WifiStateMachine.this.handleHostapdConnectionLoss();
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mInitialState);
                    WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.CMD_START_AP, 5000L);
                    return true;
                default:
                    return WifiStateMachine.DEBUG_PARSE;
            }
        }
    }

    class SoftApStoppingState extends State {
        SoftApStoppingState() {
        }

        public void enter() {
            if (WifiStateMachine.DBG) {
                WifiStateMachine.this.log("stopping hostapd");
            }
            WifiStateMachine.this.mWifiMonitor.stopSupplicant();
            WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.this.obtainMessage(WifiStateMachine.CMD_STOP_HOSTAPD_FAILED, WifiStateMachine.access$26704(WifiStateMachine.this), 0), 5000L);
            WifiStateMachine.this.setWifiApState(10);
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, getClass().getSimpleName());
            switch (message.what) {
                case WifiStateMachine.CMD_START_SUPPLICANT:
                case WifiStateMachine.CMD_STOP_SUPPLICANT:
                case WifiStateMachine.CMD_START_DRIVER:
                case WifiStateMachine.CMD_STOP_DRIVER:
                case WifiStateMachine.CMD_START_AP:
                case WifiStateMachine.CMD_STOP_AP:
                case WifiStateMachine.CMD_SET_OPERATIONAL_MODE:
                    WifiStateMachine.this.deferMessage(message);
                    return true;
                case WifiStateMachine.CMD_STOP_HOSTAPD_FAILED:
                    if (message.arg1 == WifiStateMachine.this.mSupplicantStopFailureToken) {
                        WifiStateMachine.this.loge("Timed out on a hostapd stop, kill and proceed");
                        WifiStateMachine.this.handleHostapdConnectionLoss();
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mInitialState);
                    }
                    return true;
                case WifiMonitor.AP_STA_CONNECTED_EVENT:
                    WifiStateMachine.this.loge("Hostapd connection received while stopping");
                    return true;
                case WifiMonitor.AP_DISCONNECTION_EVENT:
                    if (WifiStateMachine.DBG) {
                        WifiStateMachine.this.log("Hostapd connection lost");
                    }
                    WifiStateMachine.this.handleHostapdConnectionLoss();
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mInitialState);
                    return true;
                default:
                    return WifiStateMachine.DEBUG_PARSE;
            }
        }
    }

    class TetheringState extends State {
        TetheringState() {
        }

        public void enter() {
            WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.this.obtainMessage(WifiStateMachine.CMD_TETHER_NOTIFICATION_TIMED_OUT, WifiStateMachine.access$27104(WifiStateMachine.this), 0), 5000L);
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, getClass().getSimpleName());
            switch (message.what) {
                case WifiStateMachine.CMD_START_SUPPLICANT:
                case WifiStateMachine.CMD_STOP_SUPPLICANT:
                case WifiStateMachine.CMD_START_DRIVER:
                case WifiStateMachine.CMD_STOP_DRIVER:
                case WifiStateMachine.CMD_START_AP:
                case WifiStateMachine.CMD_STOP_AP:
                case WifiStateMachine.CMD_SET_OPERATIONAL_MODE:
                case WifiStateMachine.CMD_SET_COUNTRY_CODE:
                case WifiStateMachine.CMD_START_PACKET_FILTERING:
                case WifiStateMachine.CMD_STOP_PACKET_FILTERING:
                case WifiStateMachine.CMD_SET_FREQUENCY_BAND:
                    WifiStateMachine.this.deferMessage(message);
                    break;
                case WifiStateMachine.CMD_TETHER_STATE_CHANGE:
                    TetherStateChange stateChange = (TetherStateChange) message.obj;
                    if (WifiStateMachine.this.isWifiTethered(stateChange.active)) {
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mTetheredState);
                    }
                    break;
                case WifiStateMachine.CMD_TETHER_NOTIFICATION_TIMED_OUT:
                    if (message.arg1 == WifiStateMachine.this.mTetherToken) {
                        WifiStateMachine.this.loge("Failed to get tether update, shutdown soft access point");
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mSoftApStartedState);
                        WifiStateMachine.this.sendMessageAtFrontOfQueue(WifiStateMachine.CMD_STOP_AP);
                    }
                    break;
            }
            return true;
        }
    }

    class TetheredState extends State {
        TetheredState() {
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, getClass().getSimpleName());
            switch (message.what) {
                case WifiStateMachine.CMD_STOP_AP:
                    if (WifiStateMachine.DBG) {
                        WifiStateMachine.this.log("Untethering before stopping AP");
                    }
                    WifiStateMachine.this.setWifiApState(10);
                    WifiStateMachine.this.stopTethering();
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mUntetheringState);
                    WifiStateMachine.this.deferMessage(message);
                    return true;
                case WifiStateMachine.CMD_TETHER_STATE_CHANGE:
                    TetherStateChange stateChange = (TetherStateChange) message.obj;
                    if (WifiStateMachine.this.isWifiTethered(stateChange.active)) {
                        return true;
                    }
                    WifiStateMachine.this.loge("Tethering reports wifi as untethered!, shut down soft Ap");
                    WifiStateMachine.this.setHostApRunning(null, WifiStateMachine.DEBUG_PARSE);
                    WifiStateMachine.this.setHostApRunning(null, true);
                    return true;
                default:
                    return WifiStateMachine.DEBUG_PARSE;
            }
        }
    }

    class UntetheringState extends State {
        UntetheringState() {
        }

        public void enter() {
            WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.this.obtainMessage(WifiStateMachine.CMD_TETHER_NOTIFICATION_TIMED_OUT, WifiStateMachine.access$27104(WifiStateMachine.this), 0), 5000L);
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, getClass().getSimpleName());
            switch (message.what) {
                case WifiStateMachine.CMD_START_SUPPLICANT:
                case WifiStateMachine.CMD_STOP_SUPPLICANT:
                case WifiStateMachine.CMD_START_DRIVER:
                case WifiStateMachine.CMD_STOP_DRIVER:
                case WifiStateMachine.CMD_START_AP:
                case WifiStateMachine.CMD_STOP_AP:
                case WifiStateMachine.CMD_SET_OPERATIONAL_MODE:
                case WifiStateMachine.CMD_SET_COUNTRY_CODE:
                case WifiStateMachine.CMD_START_PACKET_FILTERING:
                case WifiStateMachine.CMD_STOP_PACKET_FILTERING:
                case WifiStateMachine.CMD_SET_FREQUENCY_BAND:
                    WifiStateMachine.this.deferMessage(message);
                    return true;
                case WifiStateMachine.CMD_TETHER_STATE_CHANGE:
                    TetherStateChange stateChange = (TetherStateChange) message.obj;
                    if (!WifiStateMachine.this.isWifiTethered(stateChange.active)) {
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mSoftApStartedState);
                    }
                    return true;
                case WifiStateMachine.CMD_TETHER_NOTIFICATION_TIMED_OUT:
                    if (message.arg1 == WifiStateMachine.this.mTetherToken) {
                        WifiStateMachine.this.loge("Failed to get tether update, force stop access point");
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mSoftApStartedState);
                    }
                    return true;
                default:
                    return WifiStateMachine.DEBUG_PARSE;
            }
        }
    }

    private void replyToMessage(Message msg, int what) {
        if (msg.replyTo != null) {
            Message dstMsg = obtainMessageWithArg2(msg);
            dstMsg.what = what;
            this.mReplyChannel.replyToMessage(msg, dstMsg);
        }
    }

    private void replyToMessage(Message msg, int what, int arg1) {
        if (msg.replyTo != null) {
            Message dstMsg = obtainMessageWithArg2(msg);
            dstMsg.what = what;
            dstMsg.arg1 = arg1;
            this.mReplyChannel.replyToMessage(msg, dstMsg);
        }
    }

    private void replyToMessage(Message msg, int what, Object obj) {
        if (msg.replyTo != null) {
            Message dstMsg = obtainMessageWithArg2(msg);
            dstMsg.what = what;
            dstMsg.obj = obj;
            this.mReplyChannel.replyToMessage(msg, dstMsg);
        }
    }

    private Message obtainMessageWithArg2(Message srcMsg) {
        Message msg = Message.obtain();
        msg.arg2 = srcMsg.arg2;
        return msg;
    }

    private static int parseHex(char ch) {
        if ('0' <= ch && ch <= '9') {
            return ch - '0';
        }
        if ('a' <= ch && ch <= 'f') {
            return (ch - 'a') + 10;
        }
        if ('A' <= ch && ch <= 'F') {
            return (ch - 'A') + 10;
        }
        throw new NumberFormatException("" + ch + " is not a valid hex digit");
    }

    private byte[] parseHex(String hex) {
        if (hex == null) {
            return new byte[0];
        }
        if (hex.length() % 2 != 0) {
            throw new NumberFormatException(hex + " is not a valid hex string");
        }
        byte[] result = new byte[(hex.length() / 2) + 1];
        result[0] = (byte) (hex.length() / 2);
        int i = 0;
        int j = 1;
        while (i < hex.length()) {
            int val = (parseHex(hex.charAt(i)) * 16) + parseHex(hex.charAt(i + 1));
            byte b = (byte) (val & 255);
            result[j] = b;
            i += 2;
            j++;
        }
        return result;
    }

    private static String makeHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", Byte.valueOf(b)));
        }
        return sb.toString();
    }

    private static String makeHex(byte[] bytes, int from, int len) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(String.format("%02x", Byte.valueOf(bytes[from + i])));
        }
        return sb.toString();
    }

    private static byte[] concat(byte[] array1, byte[] array2, byte[] array3) {
        int len = array1.length + array2.length + array3.length;
        if (array1.length != 0) {
            len++;
        }
        if (array2.length != 0) {
            len++;
        }
        if (array3.length != 0) {
            len++;
        }
        byte[] result = new byte[len];
        int index = 0;
        if (array1.length != 0) {
            result[0] = (byte) (array1.length & 255);
            index = 0 + 1;
            for (byte b : array1) {
                result[index] = b;
                index++;
            }
        }
        if (array2.length != 0) {
            result[index] = (byte) (array2.length & 255);
            index++;
            for (byte b2 : array2) {
                result[index] = b2;
                index++;
            }
        }
        if (array3.length != 0) {
            result[index] = (byte) (array3.length & 255);
            int index2 = index + 1;
            for (byte b3 : array3) {
                result[index2] = b3;
                index2++;
            }
        }
        return result;
    }

    void handleGsmAuthRequest(SimAuthRequestData requestData) {
        if (this.targetWificonfiguration == null || this.targetWificonfiguration.networkId == requestData.networkId) {
            logd("id matches targetWifiConfiguration");
            TelephonyManager tm = (TelephonyManager) this.mContext.getSystemService("phone");
            if (tm != null) {
                StringBuilder sb = new StringBuilder();
                String[] arr$ = requestData.challenges;
                for (String challenge : arr$) {
                    if (challenge.length() == 0) {
                        loge("skip the first null challenge");
                    } else {
                        logd("RAND = " + challenge);
                        try {
                            byte[] rand = parseHex(challenge);
                            String base64Challenge = Base64.encodeToString(rand, 2);
                            int appType = tm.getUiccAppType();
                            if (appType == 0) {
                                loge("!!!Error unknown app type");
                                return;
                            }
                            String tmResponse = tm.getIccSimChallengeResponse(appType, base64Challenge);
                            logv("Raw Response - " + tmResponse);
                            if (tmResponse != null && tmResponse.length() > 4) {
                                byte[] result = Base64.decode(tmResponse, 0);
                                logv("Hex Response -" + makeHex(result));
                                int sres_len = result[0];
                                String sres = makeHex(result, 1, sres_len);
                                int kc_offset = sres_len + 1;
                                int kc_len = result[kc_offset];
                                String kc = makeHex(result, kc_offset + 1, kc_len);
                                sb.append(":" + kc + ":" + sres);
                                logv("kc:" + kc + " sres:" + sres);
                            } else {
                                loge("bad response - " + tmResponse);
                            }
                        } catch (NumberFormatException e) {
                            loge("malformed challenge");
                        }
                    }
                }
                String response = sb.toString();
                logv("Supplicant Response -" + response);
                this.mWifiNative.simAuthResponse(requestData.networkId, ":GSM-AUTH" + response);
                return;
            }
            loge("could not get telephony manager");
            return;
        }
        logd("id does not match targetWifiConfiguration");
    }

    void handle3GAuthRequest(SimAuthRequestData requestData) {
        if (this.targetWificonfiguration == null || this.targetWificonfiguration.networkId == requestData.networkId) {
            logd("id matches targetWifiConfiguration");
            TelephonyManager tm = (TelephonyManager) this.mContext.getSystemService("phone");
            if (tm != null) {
                StringBuilder sb = new StringBuilder();
                byte[] randAutn = new byte[34];
                int dstPos = 0;
                String[] arr$ = requestData.challenges;
                for (String challenge : arr$) {
                    if (challenge.length() != 32) {
                        loge("!!! invalid rand or autn length !!!");
                        return;
                    }
                    logd("RAND or AUTN = " + challenge);
                    try {
                        byte[] rand = parseHex(challenge);
                        System.arraycopy(rand, 0, randAutn, dstPos, rand.length);
                        dstPos += rand.length;
                    } catch (NumberFormatException e) {
                        loge("malformed challenge");
                    }
                }
                String base64Challenge = Base64.encodeToString(randAutn, 2);
                int appType = tm.getUiccAppType();
                if (appType == 0) {
                    loge("!!!Error unknown app type");
                    return;
                }
                String tmResponse = tm.getIccSimChallengeResponse(appType, base64Challenge);
                logv("Raw Response - " + tmResponse);
                if (tmResponse != null && tmResponse.length() > 4) {
                    byte[] result = Base64.decode(tmResponse, 0);
                    logv("Hex Response -" + makeHex(result));
                    if ((result[0] & 255) == UMTS_AUTH_SUCCESS) {
                        int res_len = result[1];
                        String res = makeHex(result, 2, res_len);
                        int ck_offset = res_len + 2;
                        int ck_len = result[ck_offset];
                        String ck = makeHex(result, ck_offset + 1, ck_len);
                        int ik_offset = ck_offset + 1 + ck_len;
                        int ik_len = result[ik_offset];
                        String ik = makeHex(result, ik_offset + 1, ik_len);
                        sb.append(":" + ik + ":" + ck + ":" + res);
                        logv("ik:" + ik + " ck:" + ck + " res:" + res);
                        String response = sb.toString();
                        logv("Supplicant Response -" + response);
                        this.mWifiNative.simAuthResponse(requestData.networkId, ":UMTS-AUTH" + response);
                        return;
                    }
                    if ((result[0] & 255) == UMTS_AUTH_SYNC_FAIL) {
                        int auts_len = result[1];
                        String auts = makeHex(result, 2, auts_len);
                        sb.append(":" + auts);
                        logv("auts:" + auts);
                        String response2 = sb.toString();
                        logv("Supplicant Response -" + response2);
                        this.mWifiNative.simAuthResponse(requestData.networkId, ":AUTS" + response2);
                        return;
                    }
                    loge("bad response - " + tmResponse);
                    return;
                }
                loge("bad response - " + tmResponse);
                return;
            }
            loge("could not get telephony manager");
            return;
        }
        logd("id does not match targetWifiConfiguration");
    }

    void handleEapSimIdentityRequest(int networkId, int method) {
        String response;
        TelephonyManager tm = (TelephonyManager) this.mContext.getSystemService("phone");
        if (tm.getSimState() != 5) {
            loge("!!!Error SIM is not ready yet!!!");
            return;
        }
        String imsi = tm.getSubscriberId();
        logd("get IMSI from tm: " + imsi);
        if (imsi.length() == 0) {
            loge("get empty IMSI from telephony");
        }
        String mccMnc = tm.getSimOperator();
        String mcc = "";
        String mnc = "";
        switch (mccMnc.length()) {
            case 5:
            case Node.ENTITY_REF:
                mcc = mccMnc.substring(0, 3);
                mnc = mccMnc.substring(3);
                logd("mcc " + mcc + "mnc " + mnc);
                break;
            default:
                loge("!!!invalid mccMnc string returned!!!");
                break;
        }
        if (mnc.length() == 2) {
            mnc = "0" + mnc;
        }
        if (method == 4) {
            response = "1" + imsi + "@wlan.mnc" + mnc + ".mcc" + mcc + ".3gppnetwork.org";
        } else {
            response = "0" + imsi + "@wlan.mnc" + mnc + ".mcc" + mcc + ".3gppnetwork.org";
        }
        this.mWifiNative.simIdentityResponse(networkId, response);
    }
}
