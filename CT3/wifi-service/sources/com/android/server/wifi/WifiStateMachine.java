package com.android.server.wifi;

import android.R;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.DhcpResults;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkAgent;
import android.net.NetworkCapabilities;
import android.net.NetworkFactory;
import android.net.NetworkInfo;
import android.net.NetworkMisc;
import android.net.NetworkRequest;
import android.net.NetworkUtils;
import android.net.RouteInfo;
import android.net.StaticIpConfiguration;
import android.net.Uri;
import android.net.dhcp.DhcpClient;
import android.net.ip.IpManager;
import android.net.wifi.PPPOEConfig;
import android.net.wifi.PPPOEInfo;
import android.net.wifi.PasspointManagementObjectDefinition;
import android.net.wifi.RssiPacketCountInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.ScanSettings;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiChannel;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConnectionStatistics;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiLinkLayerStats;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiScanner;
import android.net.wifi.WpsInfo;
import android.net.wifi.WpsResult;
import android.net.wifi.p2p.IWifiP2pManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.WorkSource;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.app.IBatteryStats;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.MessageUtils;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.connectivity.KeepalivePacketData;
import com.android.server.wifi.SoftApManager;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.hotspot2.IconEvent;
import com.android.server.wifi.hotspot2.NetworkDetail;
import com.android.server.wifi.hotspot2.Utils;
import com.android.server.wifi.hotspot2.omadm.PasspointManagementObjectManager;
import com.android.server.wifi.p2p.WifiP2pServiceImpl;
import com.android.server.wifi.scanner.ChannelHelper;
import com.google.protobuf.nano.Extension;
import com.mediatek.aee.ExceptionLog;
import com.mediatek.common.MPlugin;
import com.mediatek.common.wifi.IWifiFwkExt;
import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class WifiStateMachine extends StateMachine implements WifiNative.WifiRssiEventHandler {
    static final int BASE = 131072;
    static final int CMD_ACCEPT_UNVALIDATED = 131225;
    static final int CMD_ADD_OR_UPDATE_NETWORK = 131124;
    static final int CMD_ADD_PASSPOINT_MO = 131174;
    static final int CMD_AP_STOPPED = 131096;
    static final int CMD_ASSOCIATED_BSSID = 131219;
    static final int CMD_AUTO_CONNECT = 131215;
    static final int CMD_AUTO_ROAM = 131217;
    static final int CMD_AUTO_SAVE_NETWORK = 131218;
    static final int CMD_BLACKLIST_NETWORK = 131128;
    static final int CMD_BLUETOOTH_ADAPTER_STATE_CHANGE = 131103;
    public static final int CMD_BOOT_COMPLETED = 131206;
    static final int CMD_CLEAR_BLACKLIST = 131129;
    static final int CMD_CONFIG_ND_OFFLOAD = 131276;
    static final int CMD_DELAYED_NETWORK_DISCONNECT = 131159;
    static final int CMD_DISABLE_EPHEMERAL_NETWORK = 131170;
    public static final int CMD_DISABLE_P2P_REQ = 131204;
    public static final int CMD_DISABLE_P2P_RSP = 131205;
    static final int CMD_DISCONNECT = 131145;
    static final int CMD_DISCONNECTING_WATCHDOG_TIMER = 131168;
    static final int CMD_DRIVER_START_TIMED_OUT = 131091;
    static final int CMD_ENABLE_ALL_NETWORKS = 131127;
    static final int CMD_ENABLE_AUTOJOIN_WHEN_ASSOCIATED = 131239;
    static final int CMD_ENABLE_NETWORK = 131126;
    public static final int CMD_ENABLE_P2P = 131203;
    static final int CMD_ENABLE_RSSI_POLL = 131154;
    static final int CMD_ENABLE_TDLS = 131164;
    static final int CMD_ENABLE_WIFI_CONNECTIVITY_MANAGER = 131238;
    static final int CMD_FIRMWARE_ALERT = 131172;
    static final int CMD_GET_CAPABILITY_FREQ = 131132;
    static final int CMD_GET_CONFIGURED_NETWORKS = 131131;
    static final int CMD_GET_CONNECTION_STATISTICS = 131148;
    static final int CMD_GET_LINK_LAYER_STATS = 131135;
    static final int CMD_GET_MATCHING_CONFIG = 131171;
    static final int CMD_GET_PRIVILEGED_CONFIGURED_NETWORKS = 131134;
    static final int CMD_GET_SUPPORTED_FEATURES = 131133;
    static final int CMD_INSTALL_PACKET_FILTER = 131274;
    static final int CMD_IPV4_PROVISIONING_FAILURE = 131273;
    static final int CMD_IPV4_PROVISIONING_SUCCESS = 131272;
    static final int CMD_IP_CONFIGURATION_LOST = 131211;
    static final int CMD_IP_CONFIGURATION_SUCCESSFUL = 131210;
    static final int CMD_IP_REACHABILITY_LOST = 131221;
    static final int CMD_MATCH_PROVIDER_NETWORK = 131177;
    static final int CMD_MODIFY_PASSPOINT_MO = 131175;
    static final int CMD_NETWORK_STATUS = 131220;
    static final int CMD_NO_NETWORKS_PERIODIC_SCAN = 131160;
    static final int CMD_OBTAINING_IP_ADDRESS_WATCHDOG_TIMER = 131165;
    static final int CMD_PING_SUPPLICANT = 131123;
    static final int CMD_QUERY_OSU_ICON = 131176;
    static final int CMD_REASSOCIATE = 131147;
    static final int CMD_RECONNECT = 131146;
    static final int CMD_RELOAD_TLS_AND_RECONNECT = 131214;
    static final int CMD_REMOVE_APP_CONFIGURATIONS = 131169;
    static final int CMD_REMOVE_NETWORK = 131125;
    static final int CMD_REMOVE_USER_CONFIGURATIONS = 131224;
    static final int CMD_RESET_SIM_NETWORKS = 131173;
    static final int CMD_RESET_SUPPLICANT_STATE = 131183;
    static final int CMD_ROAM_WATCHDOG_TIMER = 131166;
    static final int CMD_RSSI_POLL = 131155;
    static final int CMD_RSSI_THRESHOLD_BREACH = 131236;
    static final int CMD_SAVE_CONFIG = 131130;
    static final int CMD_SCREEN_STATE_CHANGED = 131167;
    static final int CMD_SET_FALLBACK_PACKET_FILTERING = 131275;
    static final int CMD_SET_FREQUENCY_BAND = 131162;
    static final int CMD_SET_HIGH_PERF_MODE = 131149;
    static final int CMD_SET_OPERATIONAL_MODE = 131144;
    static final int CMD_SET_SUSPEND_OPT_ENABLED = 131158;
    static final int CMD_START_AP = 131093;
    static final int CMD_START_AP_FAILURE = 131094;
    static final int CMD_START_DRIVER = 131085;
    static final int CMD_START_IP_PACKET_OFFLOAD = 131232;
    static final int CMD_START_RSSI_MONITORING_OFFLOAD = 131234;
    static final int CMD_START_SCAN = 131143;
    static final int CMD_START_SUPPLICANT = 131083;
    static final int CMD_STATIC_IP_FAILURE = 131088;
    static final int CMD_STATIC_IP_SUCCESS = 131087;
    static final int CMD_STOP_AP = 131095;
    static final int CMD_STOP_DRIVER = 131086;
    static final int CMD_STOP_IP_PACKET_OFFLOAD = 131233;
    static final int CMD_STOP_RSSI_MONITORING_OFFLOAD = 131235;
    static final int CMD_STOP_SUPPLICANT = 131084;
    static final int CMD_STOP_SUPPLICANT_FAILED = 131089;
    static final int CMD_TARGET_BSSID = 131213;
    static final int CMD_TEST_NETWORK_DISCONNECT = 131161;
    static final int CMD_UNWANTED_NETWORK = 131216;
    static final int CMD_UPDATE_ASSOCIATED_SCAN_PERMISSION = 131230;
    static final int CMD_UPDATE_LINKPROPERTIES = 131212;
    static final int CMD_USER_SWITCH = 131237;
    public static final int CONNECT_MODE = 1;
    private static final int CONNECT_TIMEOUT_MSEC = 3000;
    private static final String CUSTOMIZED_SCAN_SETTING = "customized_scan_settings";
    private static final String CUSTOMIZED_SCAN_WORKSOURCE = "customized_scan_worksource";
    private static final boolean DEBUG_PARSE = false;
    public static final int DFS_RESTRICTED_SCAN_REQUEST = -6;
    static final int DISCONNECTING_GUARD_TIMER_MSEC = 5000;
    private static final int DRIVER_START_TIME_OUT_MSECS = 10000;
    private static final int EVENT_PPPOE_SUCCEEDED = 1;
    private static final int EVENT_START_PPPOE = 0;
    private static final int EVENT_UPDATE_DNS = 2;
    private static final int FAILURE = -1;
    private static final String GOOGLE_OUI = "DA-A1-19";
    static final int IP_REACHABILITY_MONITOR_TIMER_MSEC = 10000;
    private static final int LINK_FLAPPING_DEBOUNCE_MSEC = 4000;
    private static final String LOGD_LEVEL_DEBUG = "D";
    private static final String LOGD_LEVEL_VERBOSE = "V";
    private static final int MAX_RSSI = 256;
    private static final int MIN_INTERVAL_ENABLE_ALL_NETWORKS_MS = 600000;
    private static final int MIN_RSSI = -200;
    private static final int M_CMD_DO_CTIA_TEST_OFF = 131283;
    private static final int M_CMD_DO_CTIA_TEST_ON = 131282;
    private static final int M_CMD_DO_CTIA_TEST_RATE = 131284;
    private static final int M_CMD_ENABLE_EAP_SIM_CONFIG_NETWORK = 131297;
    private static final int M_CMD_FACTORY_RESET = 131294;
    private static final int M_CMD_FLUSH_BSS = 131292;
    private static final int M_CMD_GET_CONNECTING_NETWORK_ID = 131248;
    private static final int M_CMD_GET_DISCONNECT_FLAG = 131260;
    private static final int M_CMD_GET_TEST_ENV = 131293;
    private static final int M_CMD_GET_WIFI_STATUS = 131288;
    private static final int M_CMD_IP_REACHABILITY_MONITOR_TIMER = 131307;
    private static final int M_CMD_NOTIFY_CONNECTION_FAILURE = 131262;
    private static final int M_CMD_SET_POWER_SAVING_MODE = 131289;
    private static final int M_CMD_SET_TDLS_POWER_SAVE = 131295;
    private static final int M_CMD_SET_TX_POWER = 131287;
    private static final int M_CMD_SET_TX_POWER_ENABLED = 131286;
    private static final int M_CMD_SET_WOWLAN_MAGIC_MODE = 131291;
    private static final int M_CMD_SET_WOWLAN_NORMAL_MODE = 131290;
    private static final int M_CMD_UPDATE_BGSCAN = 131285;
    private static final int M_CMD_UPDATE_COUNTRY_CODE = 131244;
    private static final int M_CMD_UPDATE_RSSI = 131249;
    private static final int M_CMD_UPDATE_SCAN_INTERVAL = 131243;
    private static final int M_CMD_UPDATE_SCAN_STRATEGY = 131296;
    private static final int M_CMD_UPDATE_SETTINGS = 131242;
    private static final String NETWORKTYPE = "WIFI";
    private static final String NETWORKTYPE_UNTRUSTED = "WIFI_UT";
    private static final int NETWORK_STATUS_UNWANTED_DISABLE_AUTOJOIN = 2;
    private static final int NETWORK_STATUS_UNWANTED_DISCONNECT = 0;
    private static final int NETWORK_STATUS_UNWANTED_VALIDATION_FAILED = 1;
    public static final short NUM_LOG_RECS_NORMAL = 100;
    public static final short NUM_LOG_RECS_VERBOSE = 3000;
    public static final short NUM_LOG_RECS_VERBOSE_LOW_MEMORY = 200;
    static final int OBTAINING_IP_ADDRESS_GUARD_TIMER_MSEC = 40000;
    private static final int ONE_HOUR_MILLI = 3600000;
    private static final int POLL_RSSI_INTERVAL_MSECS = 3000;
    private static final int PPPOE_NETID = 65500;
    static final int ROAM_GUARD_TIMER_MSEC = 15000;
    public static final int SCAN_ONLY_MODE = 2;
    public static final int SCAN_ONLY_WITH_WIFI_OFF_MODE = 3;
    static final long SCAN_PERMISSION_UPDATE_THROTTLE_MILLI = 20000;
    private static final int SCAN_REQUEST = 0;
    private static final int SCAN_REQUEST_BUFFER_MAX_SIZE = 10;
    private static final String SCAN_REQUEST_TIME = "scan_request_time";
    private static final int SUCCESS = 1;
    private static final int SUPPLICANT_RESTART_INTERVAL_MSECS = 5000;
    private static final int SUPPLICANT_RESTART_TRIES = 5;
    private static final int SUSPEND_DUE_TO_DHCP = 1;
    private static final int SUSPEND_DUE_TO_HIGH_PERF = 2;
    private static final int SUSPEND_DUE_TO_SCREEN = 4;
    private static final String SYSTEM_PROPERTY_LOG_CONTROL_WIFIHAL = "log.tag.WifiHAL";
    private static final String TAG = "WifiStateMachine";
    private static final int TETHER_NOTIFICATION_TIME_OUT_MSECS = 5000;
    private static final int UNKNOWN_SCAN_SOURCE = -1;
    private static final int UPDATE_DNS_DELAY_MS = 500;
    private static final int sFrameworkMinScanIntervalSaneValue = 10000;
    private final int GET_SUBID_NULL_ERROR;
    private boolean didBlackListBSSID;
    int disconnectingWatchdogCount;
    public AtomicBoolean enableIpReachabilityMonitor;
    public AtomicBoolean enableIpReachabilityMonitorEnhancement;
    int ipReachabilityMonitorCount;
    private long lastConnectAttemptTimestamp;
    private WifiConfiguration lastForgetConfigurationAttempt;
    private long lastLinkLayerStatsUpdate;
    private long lastOntimeReportTimeStamp;
    private WifiConfiguration lastSavedConfigurationAttempt;
    private Set<Integer> lastScanFreqs;
    private long lastScreenStateChangeTimeStamp;
    private boolean linkDebouncing;
    private int mAggressiveHandover;
    private AlarmManager mAlarmManager;
    private boolean mAutoRoaming;
    private final boolean mBackgroundScanSupported;
    private final BackupManagerProxy mBackupManagerProxy;
    private final IBatteryStats mBatteryStats;
    private boolean mBluetoothConnectionActive;
    private final Queue<Message> mBufferedScanMsg;
    private final BuildProperties mBuildProperties;
    private final Clock mClock;
    private ConnectivityManager mCm;
    private State mConnectModeState;
    private boolean mConnectNetwork;
    boolean mConnectedModeGScanOffloadStarted;
    private State mConnectedState;
    private int mConnectionRequests;
    private Context mContext;
    private final WifiCountryCode mCountryCode;
    private final int mDefaultFrameworkScanIntervalMs;
    private State mDefaultState;
    private final NetworkCapabilities mDfltNetworkCapabilities;
    private DhcpResults mDhcpResults;
    private final Object mDhcpResultsLock;
    private int mDisconnectNetworkId;
    private boolean mDisconnectOperation;
    private State mDisconnectedState;
    private long mDisconnectedTimeStamp;
    private State mDisconnectingState;
    private final AtomicBoolean mDontReconnect;
    private final AtomicBoolean mDontReconnectAndScan;
    private int mDriverStartToken;
    private State mDriverStartedState;
    private State mDriverStartingState;
    private State mDriverStoppedState;
    private State mDriverStoppingState;
    private boolean mEnableRssiPolling;
    private FrameworkFacade mFacade;
    private AtomicInteger mFrequencyBand;
    private long mGScanPeriodMilli;
    private long mGScanStartTimeMilli;
    private Handler mHandler;
    private boolean mHotspotOptimization;
    private State mInitialState;
    private final String mInterfaceName;
    private boolean mIpConfigLost;
    private final IpManager mIpManager;
    private boolean mIsFullScanOngoing;
    boolean mIsListeningIpReachabilityLost;
    private boolean mIsNewAssociatedBssid;
    private boolean mIsRunning;
    private boolean mIsScanOngoing;
    private State mL2ConnectedState;
    private String mLastBssid;
    private long mLastCheckWeakSignalTime;
    private long mLastDriverRoamAttempt;
    private long mLastEnableAllNetworksTime;
    private int mLastExplicitNetworkId;
    private int mLastNetworkId;
    private final WorkSource mLastRunningWifiUids;
    long mLastScanPermissionUpdate;
    private int mLastSignalLevel;
    private LinkProperties mLinkProperties;
    private boolean mMtkCtpppoe;
    private WifiNetworkAgent mNetworkAgent;
    private final NetworkCapabilities mNetworkCapabilitiesFilter;
    private WifiNetworkFactory mNetworkFactory;
    private NetworkInfo mNetworkInfo;
    private final NetworkMisc mNetworkMisc;
    private final int mNoNetworksPeriodicScan;
    private int mNumScanResultsKnown;
    private int mNumScanResultsReturned;
    private INetworkManagementService mNwService;
    private State mObtainingIpState;
    private int mOnTime;
    private int mOnTimeLastReport;
    private int mOnTimeScreenStateChange;
    private long mOnlineStartTime;
    private int mOperationalMode;
    private final AtomicBoolean mP2pConnected;
    private final boolean mP2pSupported;
    private int mPeriodicScanToken;
    private PPPOEConfig mPppoeConfig;
    private PppoeHandler mPppoeHandler;
    private PPPOEInfo mPppoeInfo;
    private LinkProperties mPppoeLinkProperties;
    private final String mPrimaryDeviceType;
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
    private boolean mScanForWeakSignal;
    private PendingIntent mScanIntent;
    private State mScanModeState;
    private List<ScanDetail> mScanResults;
    private final Object mScanResultsLock;
    private WorkSource mScanWorkSource;
    private AtomicBoolean mScreenBroadcastReceived;
    private boolean mScreenOn;
    private boolean mSendScanResultsBroadcast;
    private boolean mShowReselectDialog;
    private String mSim1IccState;
    private String mSim2IccState;
    private State mSoftApState;
    private final AtomicBoolean mStopScanStarted;
    private int mSupplicantRestartCount;
    private long mSupplicantScanIntervalMs;
    private State mSupplicantStartedState;
    private State mSupplicantStartingState;
    private SupplicantStateTracker mSupplicantStateTracker;
    private int mSupplicantStopFailureToken;
    private State mSupplicantStoppingState;
    private int mSuspendOptNeedsDisabled;
    private PowerManager.WakeLock mSuspendWakeLock;
    private int mSystemUiUid;
    private int mTargetNetworkId;
    private String mTargetRoamBSSID;
    private final String mTcpBufferSizes;
    private boolean mTemporarilyDisconnectWifi;
    private String mTetherInterfaceName;
    private int mTetherToken;
    private int mTxTime;
    private int mTxTimeLastReport;
    private UntrustedWifiNetworkFactory mUntrustedNetworkFactory;
    private AtomicBoolean mUserWantsSuspendOpt;
    private boolean mUsingPppoe;
    private int mVerboseLoggingLevel;
    private State mWaitForP2pDisableState;
    private PowerManager.WakeLock mWakeLock;
    private String[] mWhiteListedSsids;
    private WifiApConfigStore mWifiApConfigStore;
    private final AtomicInteger mWifiApState;
    private WifiConfigManager mWifiConfigManager;
    private WifiConnectionStatistics mWifiConnectionStatistics;
    private WifiConnectivityManager mWifiConnectivityManager;
    private IWifiFwkExt mWifiFwkExt;
    private final WifiInfo mWifiInfo;
    private WifiInjector mWifiInjector;
    private WifiLastResortWatchdog mWifiLastResortWatchdog;
    private int mWifiLinkLayerStatsSupported;
    private BaseWifiLogger mWifiLogger;
    private WifiManager mWifiManager;
    private WifiMetrics mWifiMetrics;
    private WifiMonitor mWifiMonitor;
    private WifiNative mWifiNative;
    private int mWifiOnScanCount;
    private AsyncChannel mWifiP2pChannel;
    private WifiP2pServiceImpl mWifiP2pServiceImpl;
    private WifiQualifiedNetworkSelector mWifiQualifiedNetworkSelector;
    private WifiScanner mWifiScanner;
    WifiScoreReport mWifiScoreReport;
    private final AtomicInteger mWifiState;
    private State mWpsRunningState;
    private int messageHandlingStatus;
    int obtainingIpWatchdogCount;
    int roamWatchdogCount;
    private WifiConfiguration targetWificonfiguration;
    private boolean testNetworkDisconnect;
    private int testNetworkDisconnectCounter;
    private static boolean DBG = true;
    private static boolean MDBG = false;
    private static boolean USE_PAUSE_SCANS = false;
    private static HashMap<String, DhcpResults> mDhcpResultMap = new HashMap<>();
    private static Random mRandom = new Random(Calendar.getInstance().getTimeInMillis());
    private static final Class[] sMessageClasses = {AsyncChannel.class, WifiStateMachine.class, DhcpClient.class};
    private static final SparseArray<String> sSmToString = MessageUtils.findMessageNames(sMessageClasses);
    public static final WorkSource WIFI_WORK_SOURCE = new WorkSource(1010);
    private static int sScanAlarmIntentCount = 0;
    private static int MESSAGE_HANDLING_STATUS_PROCESSED = 2;
    private static int MESSAGE_HANDLING_STATUS_OK = 1;
    private static int MESSAGE_HANDLING_STATUS_UNKNOWN = 0;
    private static int MESSAGE_HANDLING_STATUS_REFUSED = -1;
    private static int MESSAGE_HANDLING_STATUS_FAIL = -2;
    private static final int ADD_OR_UPDATE_SOURCE = -3;
    private static int MESSAGE_HANDLING_STATUS_OBSOLETE = ADD_OR_UPDATE_SOURCE;
    private static final int SET_ALLOW_UNTRUSTED_SOURCE = -4;
    private static int MESSAGE_HANDLING_STATUS_DEFERRED = SET_ALLOW_UNTRUSTED_SOURCE;
    private static final int ENABLE_WIFI = -5;
    private static int MESSAGE_HANDLING_STATUS_DISCARD = ENABLE_WIFI;
    private static int MESSAGE_HANDLING_STATUS_LOOPED = -6;
    private static int MESSAGE_HANDLING_STATUS_HANDLING_ERROR = -7;

    public static class SimAuthRequestData {
        String[] data;
        int networkId;
        int protocol;
        String ssid;
    }

    protected void loge(String s) {
        Log.e(getName(), s);
    }

    protected void logd(String s) {
        Log.d(getName(), s);
    }

    protected void log(String s) {
        Log.d(getName(), s);
    }

    @Override
    public void onRssiThresholdBreached(byte curRssi) {
        if (DBG) {
            Log.e(TAG, "onRssiThresholdBreach event. Cur Rssi = " + ((int) curRssi));
        }
        sendMessage(CMD_RSSI_THRESHOLD_BREACH, curRssi);
    }

    public void processRssiThreshold(byte curRssi, int reason) {
        if (curRssi == 127 || curRssi == -128) {
            Log.wtf(TAG, "processRssiThreshold: Invalid rssi " + ((int) curRssi));
            return;
        }
        for (int i = 0; i < this.mRssiRanges.length; i++) {
            if (curRssi < this.mRssiRanges[i]) {
                byte maxRssi = this.mRssiRanges[i];
                byte minRssi = this.mRssiRanges[i - 1];
                this.mWifiInfo.setRssi(curRssi);
                updateCapabilities(getCurrentWifiConfiguration());
                int ret = startRssiMonitoringOffload(maxRssi, minRssi);
                Log.d(TAG, "Re-program RSSI thresholds for " + smToString(reason) + ": [" + ((int) minRssi) + ", " + ((int) maxRssi) + "], curRssi=" + ((int) curRssi) + " ret=" + ret);
                return;
            }
        }
    }

    boolean isRoaming() {
        return this.mAutoRoaming;
    }

    public void autoRoamSetBSSID(int netId, String bssid) {
        autoRoamSetBSSID(this.mWifiConfigManager.getWifiConfiguration(netId), bssid);
    }

    public boolean autoRoamSetBSSID(WifiConfiguration config, String bssid) {
        boolean ret = true;
        if (this.mTargetRoamBSSID == null) {
            this.mTargetRoamBSSID = WifiLastResortWatchdog.BSSID_ANY;
        }
        if (bssid == null) {
            bssid = WifiLastResortWatchdog.BSSID_ANY;
        }
        if (config == null) {
            return false;
        }
        if (this.mTargetRoamBSSID != null && bssid.equals(this.mTargetRoamBSSID) && bssid.equals(config.BSSID)) {
            return false;
        }
        if (!this.mTargetRoamBSSID.equals(WifiLastResortWatchdog.BSSID_ANY) && bssid.equals(WifiLastResortWatchdog.BSSID_ANY)) {
            ret = false;
        }
        if (config.BSSID != null) {
            bssid = config.BSSID;
            if (DBG) {
                Log.d(TAG, "force BSSID to " + bssid + "due to config");
            }
        }
        if (DBG) {
            logd("autoRoamSetBSSID " + bssid + " key=" + config.configKey());
        }
        this.mTargetRoamBSSID = bssid;
        this.mWifiConfigManager.saveWifiConfigBSSID(config, bssid);
        return ret;
    }

    private boolean setTargetBssid(WifiConfiguration config, String bssid) {
        if (config == null) {
            return false;
        }
        if (config.BSSID != null) {
            bssid = config.BSSID;
            if (DBG) {
                Log.d(TAG, "force BSSID to " + bssid + "due to config");
            }
        }
        if (bssid == null) {
            bssid = WifiLastResortWatchdog.BSSID_ANY;
        }
        String networkSelectionBSSID = config.getNetworkSelectionStatus().getNetworkSelectionBSSID();
        if (networkSelectionBSSID != null && networkSelectionBSSID.equals(bssid)) {
            if (DBG) {
                Log.d(TAG, "Current preferred BSSID is the same as the target one");
            }
            return false;
        }
        if (DBG) {
            Log.d(TAG, "target set to " + config.SSID + ":" + bssid);
        }
        this.mTargetRoamBSSID = bssid;
        this.mWifiConfigManager.saveWifiConfigBSSID(config, bssid);
        return true;
    }

    boolean recordUidIfAuthorized(WifiConfiguration config, int uid, boolean onlyAnnotate) {
        if (!this.mWifiConfigManager.isNetworkConfigured(config)) {
            config.creatorUid = uid;
            config.creatorName = this.mContext.getPackageManager().getNameForUid(uid);
        } else if (!this.mWifiConfigManager.canModifyNetwork(uid, config, onlyAnnotate)) {
            return false;
        }
        config.lastUpdateUid = uid;
        config.lastUpdateName = this.mContext.getPackageManager().getNameForUid(uid);
        return true;
    }

    boolean deferForUserInput(Message message, int netId, boolean allowOverride) {
        WifiConfiguration config = this.mWifiConfigManager.getWifiConfiguration(netId);
        if (config == null) {
            logd("deferForUserInput: configuration for netId=" + netId + " not stored");
            return true;
        }
        switch (config.userApproved) {
            case 1:
            case 2:
                return false;
            default:
                config.userApproved = 1;
                return false;
        }
    }

    public WifiStateMachine(Context context, FrameworkFacade facade, Looper looper, UserManager userManager, WifiInjector wifiInjector, BackupManagerProxy backupManagerProxy, WifiCountryCode countryCode) {
        super(TAG, looper);
        this.mVerboseLoggingLevel = 0;
        this.didBlackListBSSID = false;
        this.mP2pConnected = new AtomicBoolean(false);
        this.mTemporarilyDisconnectWifi = false;
        this.mScanResults = new ArrayList();
        this.mScanResultsLock = new Object();
        this.mScreenOn = false;
        this.mLastSignalLevel = -1;
        this.linkDebouncing = false;
        this.testNetworkDisconnect = false;
        this.mEnableRssiPolling = false;
        this.mRssiPollToken = 0;
        this.mOperationalMode = 1;
        this.mIsScanOngoing = false;
        this.mIsFullScanOngoing = false;
        this.mSendScanResultsBroadcast = false;
        this.mBufferedScanMsg = new LinkedList();
        this.mScanWorkSource = null;
        this.mScreenBroadcastReceived = new AtomicBoolean(false);
        this.mBluetoothConnectionActive = false;
        this.mSupplicantRestartCount = 0;
        this.mSupplicantStopFailureToken = 0;
        this.mTetherToken = 0;
        this.mDriverStartToken = 0;
        this.mPeriodicScanToken = 0;
        this.mDhcpResultsLock = new Object();
        this.mWifiLinkLayerStatsSupported = 4;
        this.mAutoRoaming = false;
        this.mRoamFailCount = 0;
        this.mTargetRoamBSSID = WifiLastResortWatchdog.BSSID_ANY;
        this.mTargetNetworkId = -1;
        this.mLastDriverRoamAttempt = 0L;
        this.targetWificonfiguration = null;
        this.lastSavedConfigurationAttempt = null;
        this.lastForgetConfigurationAttempt = null;
        this.mFrequencyBand = new AtomicInteger(0);
        this.mReplyChannel = new AsyncChannel();
        this.mConnectionRequests = 0;
        this.mWhiteListedSsids = null;
        this.mWifiConnectionStatistics = new WifiConnectionStatistics();
        this.mNetworkCapabilitiesFilter = new NetworkCapabilities();
        this.mNetworkMisc = new NetworkMisc();
        this.testNetworkDisconnectCounter = 0;
        this.roamWatchdogCount = 0;
        this.obtainingIpWatchdogCount = 0;
        this.disconnectingWatchdogCount = 0;
        this.ipReachabilityMonitorCount = 0;
        this.mIsListeningIpReachabilityLost = false;
        this.enableIpReachabilityMonitor = new AtomicBoolean(true);
        this.enableIpReachabilityMonitorEnhancement = new AtomicBoolean(true);
        this.mSuspendOptNeedsDisabled = 0;
        this.mUserWantsSuspendOpt = new AtomicBoolean(true);
        this.mRunningBeaconCount = 0;
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
        this.mConnectedState = new ConnectedState();
        this.mRoamingState = new RoamingState();
        this.mDisconnectingState = new DisconnectingState();
        this.mDisconnectedState = new DisconnectedState();
        this.mWpsRunningState = new WpsRunningState();
        this.mSoftApState = new SoftApState();
        this.mWifiState = new AtomicInteger(1);
        this.mWifiApState = new AtomicInteger(11);
        this.mIsRunning = false;
        this.mReportedRunning = false;
        this.mRunningWifiUids = new WorkSource();
        this.mLastRunningWifiUids = new WorkSource();
        this.mSystemUiUid = -1;
        this.mDisconnectOperation = false;
        this.mScanForWeakSignal = false;
        this.mShowReselectDialog = false;
        this.mIpConfigLost = false;
        this.mDisconnectNetworkId = -1;
        this.mLastExplicitNetworkId = -1;
        this.mLastCheckWeakSignalTime = 0L;
        this.mWifiOnScanCount = 0;
        this.mStopScanStarted = new AtomicBoolean(false);
        this.mConnectNetwork = false;
        this.mUsingPppoe = false;
        this.mOnlineStartTime = 0L;
        this.mMtkCtpppoe = false;
        this.mDontReconnectAndScan = new AtomicBoolean(false);
        this.mDontReconnect = new AtomicBoolean(false);
        this.mHotspotOptimization = false;
        this.mIsNewAssociatedBssid = false;
        this.mSim1IccState = "UNKNOWN";
        this.mSim2IccState = "UNKNOWN";
        this.mHandler = getHandler();
        this.mLastScanPermissionUpdate = 0L;
        this.mConnectedModeGScanOffloadStarted = false;
        this.mAggressiveHandover = 0;
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
        this.mWifiScoreReport = null;
        this.GET_SUBID_NULL_ERROR = -1;
        this.mWifiFwkExt = (IWifiFwkExt) MPlugin.createInstance(IWifiFwkExt.class.getName(), context);
        this.mWifiInjector = wifiInjector;
        this.mWifiMetrics = this.mWifiInjector.getWifiMetrics();
        this.mWifiLastResortWatchdog = wifiInjector.getWifiLastResortWatchdog();
        this.mClock = wifiInjector.getClock();
        this.mPropertyService = wifiInjector.getPropertyService();
        this.mBuildProperties = wifiInjector.getBuildProperties();
        this.mContext = context;
        this.mFacade = facade;
        this.mWifiNative = WifiNative.getWlanNativeInterface();
        this.mBackupManagerProxy = backupManagerProxy;
        this.mWifiNative.initContext(this.mContext);
        this.mInterfaceName = this.mWifiNative.getInterfaceName();
        this.mNetworkInfo = new NetworkInfo(1, 0, NETWORKTYPE, "");
        this.mBatteryStats = IBatteryStats.Stub.asInterface(this.mFacade.getService("batterystats"));
        IBinder b = this.mFacade.getService("network_management");
        this.mNwService = INetworkManagementService.Stub.asInterface(b);
        this.mP2pSupported = this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi.direct");
        this.mWifiConfigManager = this.mFacade.makeWifiConfigManager(context, this.mWifiNative, facade, this.mWifiInjector.getClock(), userManager, this.mWifiInjector.getKeyStore());
        this.mWifiMonitor = WifiMonitor.getInstance();
        boolean enableFirmwareLogs = this.mContext.getResources().getBoolean(R.^attr-private.backgroundRight);
        if (enableFirmwareLogs) {
            this.mWifiLogger = facade.makeRealLogger(this, this.mWifiNative, this.mBuildProperties);
        } else {
            this.mWifiLogger = facade.makeBaseLogger();
        }
        this.mWifiInfo = new WifiInfo();
        this.mWifiQualifiedNetworkSelector = new WifiQualifiedNetworkSelector(this.mWifiConfigManager, this.mContext, this.mWifiInfo, this.mWifiInjector.getClock());
        this.mWifiQualifiedNetworkSelector.setWifiFwkExt(this.mWifiFwkExt);
        this.mSupplicantStateTracker = this.mFacade.makeSupplicantStateTracker(context, this.mWifiConfigManager, getHandler());
        this.mLinkProperties = new LinkProperties();
        IBinder s1 = this.mFacade.getService("wifip2p");
        this.mWifiP2pServiceImpl = IWifiP2pManager.Stub.asInterface(s1);
        this.mNetworkInfo.setIsAvailable(false);
        this.mLastBssid = null;
        this.mLastNetworkId = -1;
        this.mLastSignalLevel = -1;
        this.mIpManager = this.mFacade.makeIpManager(this.mContext, this.mInterfaceName, new IpManagerCallback());
        this.mIpManager.setMulticastFilter(true);
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        if (this.mWifiFwkExt != null) {
            this.mDefaultFrameworkScanIntervalMs = this.mWifiFwkExt.defaultFrameworkScanIntervalMs();
        } else {
            int period = this.mContext.getResources().getInteger(R.integer.config_bg_current_drain_location_min_duration);
            this.mDefaultFrameworkScanIntervalMs = period < 10000 ? 10000 : period;
        }
        this.mNoNetworksPeriodicScan = this.mContext.getResources().getInteger(R.integer.config_bg_current_drain_media_playback_min_duration);
        this.mBackgroundScanSupported = this.mContext.getResources().getBoolean(R.^attr-private.backgroundLeft);
        this.mPrimaryDeviceType = this.mContext.getResources().getString(R.string.config_systemVisualIntelligence);
        this.mCountryCode = countryCode;
        this.mUserWantsSuspendOpt.set(this.mFacade.getIntegerSetting(this.mContext, "wifi_suspend_optimizations_enabled", 1) == 1);
        this.mNetworkCapabilitiesFilter.addTransportType(1);
        this.mNetworkCapabilitiesFilter.addCapability(12);
        this.mNetworkCapabilitiesFilter.addCapability(11);
        this.mNetworkCapabilitiesFilter.addCapability(13);
        this.mNetworkCapabilitiesFilter.setLinkUpstreamBandwidthKbps(WifiLogger.RING_BUFFER_BYTE_LIMIT_LARGE);
        this.mNetworkCapabilitiesFilter.setLinkDownstreamBandwidthKbps(WifiLogger.RING_BUFFER_BYTE_LIMIT_LARGE);
        this.mDfltNetworkCapabilities = new NetworkCapabilities(this.mNetworkCapabilitiesFilter);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SCREEN_ON");
        filter.addAction("android.intent.action.SCREEN_OFF");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                Log.d(WifiStateMachine.TAG, "onReceive, action:" + action);
                if (action.equals("android.intent.action.SCREEN_ON")) {
                    WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_SCREEN_STATE_CHANGED, 1);
                } else {
                    if (!action.equals("android.intent.action.SCREEN_OFF")) {
                        return;
                    }
                    WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_SCREEN_STATE_CHANGED, 0);
                }
            }
        }, filter);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("wifi_suspend_optimizations_enabled"), false, new ContentObserver(getHandler()) {
            @Override
            public void onChange(boolean selfChange) {
                WifiStateMachine.this.mUserWantsSuspendOpt.set(WifiStateMachine.this.mFacade.getIntegerSetting(WifiStateMachine.this.mContext, "wifi_suspend_optimizations_enabled", 1) == 1);
            }
        });
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_BOOT_COMPLETED);
            }
        }, new IntentFilter("android.intent.action.BOOT_COMPLETED"));
        PowerManager powerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mWakeLock = powerManager.newWakeLock(1, getName());
        this.mSuspendWakeLock = powerManager.newWakeLock(1, "WifiSuspend");
        this.mSuspendWakeLock.setReferenceCounted(false);
        this.mTcpBufferSizes = this.mContext.getResources().getString(R.string.EmergencyCallWarningSummary);
        this.enableIpReachabilityMonitor.set(this.mContext.getResources().getBoolean(135004165));
        log("enableIpReachabilityMonitor: " + this.enableIpReachabilityMonitor.get());
        this.enableIpReachabilityMonitorEnhancement.set(this.mContext.getResources().getBoolean(135004166));
        log("enableIpReachabilityMonitorEnhancement: " + this.enableIpReachabilityMonitorEnhancement.get());
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
        addState(this.mConnectedState, this.mL2ConnectedState);
        addState(this.mRoamingState, this.mL2ConnectedState);
        addState(this.mDisconnectingState, this.mConnectModeState);
        addState(this.mDisconnectedState, this.mConnectModeState);
        addState(this.mWpsRunningState, this.mConnectModeState);
        addState(this.mWaitForP2pDisableState, this.mSupplicantStartedState);
        addState(this.mDriverStoppingState, this.mSupplicantStartedState);
        addState(this.mDriverStoppedState, this.mSupplicantStartedState);
        addState(this.mSupplicantStoppingState, this.mDefaultState);
        addState(this.mSoftApState, this.mDefaultState);
        setInitialState(this.mInitialState);
        initializeExtra();
        setLogRecSize(100);
        setLogOnlyTransitions(false);
        start();
        this.mWifiMonitor.registerHandler(this.mInterfaceName, CMD_TARGET_BSSID, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, CMD_ASSOCIATED_BSSID, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.ANQP_DONE_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.ASSOCIATION_REJECTION_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.AUTHENTICATION_FAILURE_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.DRIVER_HUNG_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.GAS_QUERY_DONE_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.GAS_QUERY_START_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.HS20_REMEDIATION_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.NETWORK_CONNECTION_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.NETWORK_DISCONNECTION_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.RX_HS20_ANQP_ICON_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.SCAN_FAILED_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.SCAN_RESULTS_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.SSID_REENABLED, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.SSID_TEMP_DISABLED, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.SUP_CONNECTION_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.SUP_DISCONNECTION_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.SUP_REQUEST_IDENTITY, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.SUP_REQUEST_SIM_AUTH, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.WPS_FAIL_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.WPS_OVERLAP_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.WPS_SUCCESS_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.WPS_TIMEOUT_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.WAPI_NO_CERTIFICATION_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.NEW_PAC_UPDATED_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.WHOLE_CHIP_RESET_FAIL_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.TDLS_CONNECTED_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.TDLS_DISCONNECTED_EVENT, getHandler());
        Intent intent = new Intent("wifi_scan_available");
        intent.addFlags(67108864);
        intent.putExtra("scan_enabled", 1);
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        try {
            this.mSystemUiUid = this.mContext.getPackageManager().getPackageUidAsUser("com.android.systemui", WifiLogger.RING_BUFFER_BYTE_LIMIT_LARGE, 0);
        } catch (PackageManager.NameNotFoundException e) {
            loge("Unable to resolve SystemUI's UID.");
        }
        this.mVerboseLoggingLevel = this.mFacade.getIntegerSetting(this.mContext, "wifi_verbose_logging_enabled", 0);
        updateLoggingLevel();
    }

    class IpManagerCallback extends IpManager.Callback {
        IpManagerCallback() {
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
                WifiStateMachine.this.mWifiLastResortWatchdog.noteConnectionFailureAndTriggerIfNeeded(WifiStateMachine.this.getTargetSsid(), WifiStateMachine.this.mTargetRoamBSSID, 3);
            }
        }

        public void onProvisioningSuccess(LinkProperties newLp) {
            WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_UPDATE_LINKPROPERTIES, newLp);
            WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_IP_CONFIGURATION_SUCCESSFUL);
        }

        public void onProvisioningFailure(LinkProperties newLp) {
            WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_IP_CONFIGURATION_LOST);
        }

        public void onLinkPropertiesChange(LinkProperties newLp) {
            WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_UPDATE_LINKPROPERTIES, newLp);
        }

        public void onReachabilityLost(String logMsg) {
            WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_IP_REACHABILITY_LOST, logMsg);
        }

        public void installPacketFilter(byte[] filter) {
            WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_INSTALL_PACKET_FILTER, filter);
        }

        public void setFallbackMulticastFilter(boolean enabled) {
            WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_SET_FALLBACK_PACKET_FILTERING, Boolean.valueOf(enabled));
        }

        public void setNeighborDiscoveryOffload(boolean enabled) {
            WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_CONFIG_ND_OFFLOAD, enabled ? 1 : 0);
        }
    }

    private void stopIpManager() {
        handlePostDhcpSetup();
        this.mIpManager.stop();
    }

    PendingIntent getPrivateBroadcast(String action, int requestCode) {
        Intent intent = new Intent(action, (Uri) null);
        intent.addFlags(67108864);
        intent.setPackage("android");
        return this.mFacade.getBroadcast(this.mContext, requestCode, intent, 0);
    }

    int getVerboseLoggingLevel() {
        return this.mVerboseLoggingLevel;
    }

    void enableVerboseLogging(int verbose) {
        this.mVerboseLoggingLevel = verbose;
        this.mFacade.setIntegerSetting(this.mContext, "wifi_verbose_logging_enabled", verbose);
        updateLoggingLevel();
    }

    void updateLoggingLevel() {
        if (this.mVerboseLoggingLevel > 0) {
            DBG = true;
            MDBG = true;
            this.mWifiNative.setSupplicantLogLevel("DEBUG");
            setLogRecSize(ActivityManager.isLowRamDeviceStatic() ? ChannelHelper.SCAN_PERIOD_PER_CHANNEL_MS : 3000);
            configureVerboseHalLogging(true);
        } else {
            DBG = false;
            MDBG = false;
            this.mWifiNative.setSupplicantLogLevel("INFO");
            setLogRecSize(100);
            configureVerboseHalLogging(false);
        }
        this.mCountryCode.enableVerboseLogging(this.mVerboseLoggingLevel);
        this.mWifiLogger.startLogging(DBG);
        this.mWifiMonitor.enableVerboseLogging(this.mVerboseLoggingLevel);
        this.mWifiNative.enableVerboseLogging(this.mVerboseLoggingLevel);
        this.mWifiConfigManager.enableVerboseLogging(this.mVerboseLoggingLevel);
        this.mSupplicantStateTracker.enableVerboseLogging(this.mVerboseLoggingLevel);
        this.mWifiQualifiedNetworkSelector.enableVerboseLogging(this.mVerboseLoggingLevel);
        if (this.mWifiConnectivityManager != null) {
            this.mWifiConnectivityManager.enableVerboseLogging(this.mVerboseLoggingLevel);
        }
        DBG = true;
    }

    private void configureVerboseHalLogging(boolean enableVerbose) {
        if (this.mBuildProperties.isUserBuild()) {
            return;
        }
        this.mPropertyService.set(SYSTEM_PROPERTY_LOG_CONTROL_WIFIHAL, enableVerbose ? LOGD_LEVEL_VERBOSE : LOGD_LEVEL_DEBUG);
    }

    void updateAssociatedScanPermission() {
    }

    int getAggressiveHandover() {
        return this.mAggressiveHandover;
    }

    void enableAggressiveHandover(int enabled) {
        this.mAggressiveHandover = enabled;
    }

    public void clearANQPCache() {
        this.mWifiConfigManager.trimANQPCache(true);
    }

    public void setAllowScansWithTraffic(int enabled) {
        this.mWifiConfigManager.mAlwaysEnableScansWhileAssociated.set(enabled);
    }

    public int getAllowScansWithTraffic() {
        return this.mWifiConfigManager.mAlwaysEnableScansWhileAssociated.get();
    }

    public boolean setEnableAutoJoinWhenAssociated(boolean enabled) {
        sendMessage(CMD_ENABLE_AUTOJOIN_WHEN_ASSOCIATED, enabled ? 1 : 0);
        return true;
    }

    public boolean getEnableAutoJoinWhenAssociated() {
        return this.mWifiConfigManager.getEnableAutoJoinWhenAssociated();
    }

    private boolean setRandomMacOui() {
        String oui = this.mContext.getResources().getString(R.string.config_systemActivityRecognizer);
        if (TextUtils.isEmpty(oui)) {
            oui = GOOGLE_OUI;
        }
        String[] ouiParts = oui.split("-");
        byte[] ouiBytes = {(byte) (Integer.parseInt(ouiParts[0], 16) & 255), (byte) (Integer.parseInt(ouiParts[1], 16) & 255), (byte) (Integer.parseInt(ouiParts[2], 16) & 255)};
        logd("Setting OUI to " + oui);
        return this.mWifiNative.setScanningMacOui(ouiBytes);
    }

    public Messenger getMessenger() {
        return new Messenger(getHandler());
    }

    public boolean syncPingSupplicant(AsyncChannel channel) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_PING_SUPPLICANT);
        boolean result = resultMsg.arg1 != -1;
        resultMsg.recycle();
        return result;
    }

    public void startScan(int callingUid, int scanCounter, ScanSettings settings, WorkSource workSource) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(CUSTOMIZED_SCAN_SETTING, settings);
        bundle.putParcelable(CUSTOMIZED_SCAN_WORKSOURCE, workSource);
        bundle.putLong(SCAN_REQUEST_TIME, System.currentTimeMillis());
        sendMessage(CMD_START_SCAN, callingUid, scanCounter, bundle);
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
        if (this.lastConnectAttemptTimestamp != 0 && now - this.lastConnectAttemptTimestamp < 10000) {
            Message dmsg = Message.obtain(msg);
            sendMessageDelayed(dmsg, 11000 - (now - this.lastConnectAttemptTimestamp));
            return false;
        }
        return true;
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
            stats = this.mWifiNative.getWifiLinkLayerStats("wlan0");
            if ("wlan0" != 0 && stats == null && this.mWifiLinkLayerStatsSupported > 0) {
                this.mWifiLinkLayerStatsSupported--;
            } else if (stats != null) {
                this.lastLinkLayerStatsUpdate = System.currentTimeMillis();
                this.mOnTime = stats.on_time;
                this.mTxTime = stats.tx_time;
                this.mRxTime = stats.rx_time;
                this.mRunningBeaconCount = stats.beacon_rx;
            }
        }
        if (stats == null || this.mWifiLinkLayerStatsSupported <= 0) {
            long mTxPkts = this.mFacade.getTxPackets(this.mInterfaceName);
            long mRxPkts = this.mFacade.getRxPackets(this.mInterfaceName);
            this.mWifiInfo.updatePacketRates(mTxPkts, mRxPkts);
        } else {
            this.mWifiInfo.updatePacketRates(stats);
        }
        return stats;
    }

    int startWifiIPPacketOffload(int slot, KeepalivePacketData packetData, int intervalSeconds) {
        int ret = this.mWifiNative.startSendingOffloadedPacket(slot, packetData, intervalSeconds * 1000);
        if (ret == 0) {
            return 0;
        }
        loge("startWifiIPPacketOffload(" + slot + ", " + intervalSeconds + "): hardware error " + ret);
        return -31;
    }

    int stopWifiIPPacketOffload(int slot) {
        int ret = this.mWifiNative.stopSendingOffloadedPacket(slot);
        if (ret == 0) {
            return 0;
        }
        loge("stopWifiIPPacketOffload(" + slot + "): hardware error " + ret);
        return -31;
    }

    int startRssiMonitoringOffload(byte maxRssi, byte minRssi) {
        return this.mWifiNative.startRssiMonitoring(maxRssi, minRssi, this);
    }

    int stopRssiMonitoringOffload() {
        return this.mWifiNative.stopRssiMonitoring();
    }

    private void handleScanRequest(Message message) {
        ScanSettings settings = null;
        WorkSource workSource = null;
        Bundle bundle = (Bundle) message.obj;
        if (bundle != null) {
            settings = (ScanSettings) bundle.getParcelable(CUSTOMIZED_SCAN_SETTING);
            workSource = (WorkSource) bundle.getParcelable(CUSTOMIZED_SCAN_WORKSOURCE);
        }
        Set<Integer> freqs = null;
        if (settings != null && settings.channelSet != null) {
            freqs = new HashSet<>();
            for (WifiChannel channel : settings.channelSet) {
                freqs.add(Integer.valueOf(channel.freqMHz));
            }
        }
        Set<Integer> hiddenNetworkIds = this.mWifiConfigManager.getHiddenConfiguredNetworkIds();
        if (startScanNative(freqs, hiddenNetworkIds, workSource)) {
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

    private boolean startScanNative(Set<Integer> freqs, Set<Integer> hiddenNetworkIds, WorkSource workSource) {
        WifiScanner.ScanSettings settings = new WifiScanner.ScanSettings();
        if (freqs == null) {
            settings.band = 7;
        } else {
            settings.band = 0;
            int index = 0;
            settings.channels = new WifiScanner.ChannelSpec[freqs.size()];
            for (Integer freq : freqs) {
                settings.channels[index] = new WifiScanner.ChannelSpec(freq.intValue());
                index++;
            }
        }
        settings.reportEvents = 3;
        if (hiddenNetworkIds != null && hiddenNetworkIds.size() > 0) {
            int i = 0;
            settings.hiddenNetworkIds = new int[hiddenNetworkIds.size()];
            for (Integer netId : hiddenNetworkIds) {
                settings.hiddenNetworkIds[i] = netId.intValue();
                i++;
            }
        }
        WifiScanner.ScanListener nativeScanListener = new WifiScanner.ScanListener() {
            public void onSuccess() {
            }

            public void onFailure(int reason, String description) {
                WifiStateMachine.this.mIsScanOngoing = false;
                WifiStateMachine.this.mIsFullScanOngoing = false;
            }

            public void onResults(WifiScanner.ScanData[] results) {
            }

            public void onFullResult(ScanResult fullScanResult) {
            }

            public void onPeriodChanged(int periodInMs) {
            }
        };
        this.mWifiScanner.startScan(settings, nativeScanListener, workSource);
        this.mIsScanOngoing = true;
        this.mIsFullScanOngoing = freqs == null;
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

    public void setHostApRunning(WifiConfiguration wifiConfig, boolean enable) {
        if (enable) {
            sendMessage(CMD_START_AP, wifiConfig);
        } else {
            sendMessage(CMD_STOP_AP);
        }
    }

    public void setWifiApConfiguration(WifiConfiguration config) {
        this.mWifiApConfigStore.setApConfiguration(config);
    }

    public WifiConfiguration syncGetWifiApConfiguration() {
        return this.mWifiApConfigStore.getApConfiguration();
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
            case Extension.TYPE_ENUM:
                return "failed";
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

    public boolean isSupplicantTransientState() {
        SupplicantState supplicantState = this.mWifiInfo.getSupplicantState();
        if (SupplicantState.isHandshakeState(supplicantState)) {
            if (DBG) {
                Log.d(TAG, "Supplicant is under transient state: " + supplicantState);
                return true;
            }
            return true;
        }
        if (DBG) {
            Log.d(TAG, "Supplicant is under steady state: " + supplicantState);
            return false;
        }
        return false;
    }

    public boolean isLinkDebouncing() {
        return this.linkDebouncing;
    }

    public WifiInfo syncRequestConnectionInfo() {
        return getWiFiInfoForUid(Binder.getCallingUid());
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
        synchronized (this.mScanResultsLock) {
            scanList = new ArrayList<>();
            for (ScanDetail result : this.mScanResults) {
                scanList.add(new ScanResult(result.getScanResult()));
            }
        }
        return scanList;
    }

    public int syncAddPasspointManagementObject(AsyncChannel channel, String managementObject) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_ADD_PASSPOINT_MO, managementObject);
        int result = resultMsg.arg1;
        resultMsg.recycle();
        return result;
    }

    public int syncModifyPasspointManagementObject(AsyncChannel channel, String fqdn, List<PasspointManagementObjectDefinition> managementObjectDefinitions) {
        Bundle bundle = new Bundle();
        bundle.putString(PasspointManagementObjectManager.TAG_FQDN, fqdn);
        bundle.putParcelableList("MOS", managementObjectDefinitions);
        Message resultMsg = channel.sendMessageSynchronously(CMD_MODIFY_PASSPOINT_MO, bundle);
        int result = resultMsg.arg1;
        resultMsg.recycle();
        return result;
    }

    public boolean syncQueryPasspointIcon(AsyncChannel channel, long bssid, String fileName) {
        Bundle bundle = new Bundle();
        bundle.putLong("BSSID", bssid);
        bundle.putString("FILENAME", fileName);
        Message resultMsg = channel.sendMessageSynchronously(CMD_QUERY_OSU_ICON, bundle);
        int result = resultMsg.arg1;
        resultMsg.recycle();
        return result == 1;
    }

    public int matchProviderWithCurrentNetwork(AsyncChannel channel, String fqdn) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_MATCH_PROVIDER_NETWORK, fqdn);
        int result = resultMsg.arg1;
        resultMsg.recycle();
        return result;
    }

    public void deauthenticateNetwork(AsyncChannel channel, long holdoff, boolean ess) {
    }

    public void disableEphemeralNetwork(String SSID) {
        if (SSID == null) {
            return;
        }
        sendMessage(CMD_DISABLE_EPHEMERAL_NETWORK, SSID);
    }

    public void disconnectCommand() {
        if (hasCustomizedAutoConnect()) {
            this.mDisconnectOperation = true;
        }
        sendMessage(CMD_DISCONNECT);
    }

    public void disconnectCommand(int uid, int reason) {
        if (hasCustomizedAutoConnect()) {
            this.mDisconnectOperation = true;
        }
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

    public WifiConfiguration syncGetMatchingWifiConfig(ScanResult scanResult, AsyncChannel channel) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_GET_MATCHING_CONFIG, scanResult);
        return (WifiConfiguration) resultMsg.obj;
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
        boolean result = resultMsg.arg1 != -1;
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
        boolean result = resultMsg.arg1 != 151570;
        resultMsg.recycle();
        return result;
    }

    public String syncGetWpsNfcConfigurationToken(int netId) {
        return this.mWifiNative.getNfcWpsConfigurationToken(netId);
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

    public void startFilteringMulticastPackets() {
        this.mIpManager.setMulticastFilter(true);
    }

    public void stopFilteringMulticastPackets() {
        this.mIpManager.setMulticastFilter(false);
    }

    public void setHighPerfModeEnabled(boolean enable) {
        sendMessage(CMD_SET_HIGH_PERF_MODE, enable ? 1 : 0, 0);
    }

    public synchronized void resetSimAuthNetworks(int simSlot) {
        sendMessage(CMD_RESET_SIM_NETWORKS, simSlot);
    }

    public Network getCurrentNetwork() {
        if (this.mNetworkAgent != null) {
            return new Network(this.mNetworkAgent.netId);
        }
        return null;
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
        return this.mWifiConfigManager.getConfigFile();
    }

    public void sendBluetoothAdapterStateChange(int state) {
        sendMessage(CMD_BLUETOOTH_ADAPTER_STATE_CHANGE, state, 0);
    }

    public void removeAppConfigs(String packageName, int uid) {
        ApplicationInfo ai = new ApplicationInfo();
        ai.packageName = packageName;
        ai.uid = uid;
        sendMessage(CMD_REMOVE_APP_CONFIGURATIONS, ai);
    }

    public void removeUserConfigs(int userId) {
        sendMessage(CMD_REMOVE_USER_CONFIGURATIONS, userId);
    }

    public boolean syncSaveConfig(AsyncChannel channel) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_SAVE_CONFIG);
        boolean result = resultMsg.arg1 != -1;
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
                this.mReportedRunning = false;
            }
            this.mWakeLock.setWorkSource(newSource);
        }
    }

    public void dumpIpManager(FileDescriptor fd, PrintWriter pw, String[] args) {
        this.mIpManager.dump(fd, pw, args);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        try {
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
            if (this.mCountryCode.getCurrentCountryCode() != null) {
                pw.println("CurrentCountryCode " + this.mCountryCode.getCurrentCountryCode());
            } else {
                pw.println("CurrentCountryCode is not initialized");
            }
            pw.println("mConnectedModeGScanOffloadStarted " + this.mConnectedModeGScanOffloadStarted);
            pw.println("mGScanPeriodMilli " + this.mGScanPeriodMilli);
            if (this.mWhiteListedSsids != null && this.mWhiteListedSsids.length > 0) {
                pw.println("SSID whitelist :");
                for (int i = 0; i < this.mWhiteListedSsids.length; i++) {
                    pw.println("       " + this.mWhiteListedSsids[i]);
                }
            }
            if (this.mNetworkFactory != null) {
                this.mNetworkFactory.dump(fd, pw, args);
            } else {
                pw.println("mNetworkFactory is not initialized");
            }
            if (this.mUntrustedNetworkFactory != null) {
                this.mUntrustedNetworkFactory.dump(fd, pw, args);
            } else {
                pw.println("mUntrustedNetworkFactory is not initialized");
            }
            pw.println("Wlan Wake Reasons:" + this.mWifiNative.getWlanWakeReasonCount());
            pw.println();
            updateWifiMetrics();
            this.mWifiMetrics.dump(fd, pw, args);
            pw.println();
            this.mWifiConfigManager.dump(fd, pw, args);
            pw.println();
            this.mWifiLogger.captureBugReportData(7);
            this.mWifiLogger.dump(fd, pw, args);
            this.mWifiQualifiedNetworkSelector.dump(fd, pw, args);
            dumpIpManager(fd, pw, args);
            if (this.mWifiConnectivityManager == null) {
                return;
            }
            this.mWifiConnectivityManager.dump(fd, pw, args);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public void handleUserSwitch(int userId) {
        sendMessage(CMD_USER_SWITCH, userId);
    }

    private void logStateAndMessage(Message message, State state) {
        this.messageHandlingStatus = 0;
        if (!DBG) {
            return;
        }
        switch (message.what) {
            case CMD_GET_CONFIGURED_NETWORKS:
            case CMD_SET_FALLBACK_PACKET_FILTERING:
            case CMD_CONFIG_ND_OFFLOAD:
                if (!MDBG) {
                    return;
                }
                break;
        }
        logd(" " + state.getClass().getSimpleName() + " " + getLogRecString(message));
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
            sb.append(" uid=").append(msg.sendingUid);
        }
        sb.append(" ").append(printTime());
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
                String key = this.mWifiConfigManager.getLastSelectedConfiguration();
                if (key != null) {
                    sb.append(" last=").append(key);
                }
                WifiConfiguration config2 = this.mWifiConfigManager.getWifiConfiguration(msg.arg1);
                if (config2 != null && (key == null || !config2.configKey().equals(key))) {
                    sb.append(" target=").append(key);
                }
                break;
            case CMD_GET_CONFIGURED_NETWORKS:
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                sb.append(" num=").append(this.mWifiConfigManager.getConfiguredNetworksSize());
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
                sb.append(" rssi=").append(this.mWifiInfo.getRssi());
                sb.append(" f=").append(this.mWifiInfo.getFrequency());
                sb.append(" sc=").append(this.mWifiInfo.score);
                sb.append(" link=").append(this.mWifiInfo.getLinkSpeed());
                sb.append(String.format(" tx=%.1f,", Double.valueOf(this.mWifiInfo.txSuccessRate)));
                sb.append(String.format(" %.1f,", Double.valueOf(this.mWifiInfo.txRetriesRate)));
                sb.append(String.format(" %.1f ", Double.valueOf(this.mWifiInfo.txBadRate)));
                sb.append(String.format(" rx=%.1f", Double.valueOf(this.mWifiInfo.rxSuccessRate)));
                if (this.lastScanFreqs != null) {
                    sb.append(" list=");
                    Iterator freq$iterator = this.lastScanFreqs.iterator();
                    while (freq$iterator.hasNext()) {
                        int freq = ((Integer) freq$iterator.next()).intValue();
                        sb.append(freq).append(",");
                    }
                }
                String report = reportOnTime();
                if (report != null) {
                    sb.append(" ").append(report);
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
                if (this.mWifiScoreReport != null) {
                    sb.append(this.mWifiScoreReport.getReport());
                }
                if (this.mConnectedModeGScanOffloadStarted) {
                    sb.append(" offload-started periodMilli ").append(this.mGScanPeriodMilli);
                } else {
                    sb.append(" offload-stopped");
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
                int count = -1;
                WifiConfiguration c = getCurrentWifiConfiguration();
                if (c != null) {
                    count = c.getNetworkSelectionStatus().getDisableReasonCounter(4);
                }
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                sb.append(" failures: ");
                sb.append(Integer.toString(count));
                sb.append("/");
                sb.append(Integer.toString(this.mWifiConfigManager.getMaxDhcpRetries()));
                if (this.mWifiInfo.getBSSID() != null) {
                    sb.append(" ").append(this.mWifiInfo.getBSSID());
                }
                sb.append(String.format(" bcn=%d", Integer.valueOf(this.mRunningBeaconCount)));
                break;
            case CMD_UPDATE_LINKPROPERTIES:
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                if (this.mLinkProperties != null) {
                    sb.append(" ");
                    sb.append(getLinkPropertiesSummary(this.mLinkProperties));
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
                sb.append(" roam=").append(Boolean.toString(this.mAutoRoaming));
                break;
            case CMD_AUTO_CONNECT:
            case 151553:
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                WifiConfiguration config3 = this.mWifiConfigManager.getWifiConfiguration(msg.arg1);
                if (config3 != null) {
                    sb.append(" ").append(config3.configKey());
                    if (config3.visibility != null) {
                        sb.append(" ").append(config3.visibility.toString());
                    }
                }
                if (this.mTargetRoamBSSID != null) {
                    sb.append(" ").append(this.mTargetRoamBSSID);
                }
                sb.append(" roam=").append(Boolean.toString(this.mAutoRoaming));
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
                sb.append(" roam=").append(Boolean.toString(this.mAutoRoaming));
                sb.append(" fail count=").append(Integer.toString(this.mRoamFailCount));
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
            case CMD_IP_REACHABILITY_LOST:
                if (msg.obj != null) {
                    sb.append(" ").append((String) msg.obj);
                }
                break;
            case CMD_UPDATE_ASSOCIATED_SCAN_PERMISSION:
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                sb.append(" autojoinAllowed=");
                sb.append(this.mWifiConfigManager.getEnableAutoJoinWhenAssociated());
                sb.append(" withTraffic=").append(getAllowScansWithTraffic());
                sb.append(" tx=").append(this.mWifiInfo.txSuccessRate);
                sb.append("/").append(8);
                sb.append(" rx=").append(this.mWifiInfo.rxSuccessRate);
                sb.append("/").append(16);
                sb.append(" -> ").append(this.mConnectedModeGScanOffloadStarted);
                break;
            case CMD_START_RSSI_MONITORING_OFFLOAD:
            case CMD_STOP_RSSI_MONITORING_OFFLOAD:
            case CMD_RSSI_THRESHOLD_BREACH:
                sb.append(" rssi=");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" thresholds=");
                sb.append(Arrays.toString(this.mRssiRanges));
                break;
            case CMD_USER_SWITCH:
                sb.append(" userId=");
                sb.append(Integer.toString(msg.arg1));
                break;
            case CMD_IPV4_PROVISIONING_SUCCESS:
                sb.append(" ");
                if (msg.arg1 == 1) {
                    sb.append("DHCP_OK");
                } else if (msg.arg1 == CMD_STATIC_IP_SUCCESS) {
                    sb.append("STATIC_OK");
                } else {
                    sb.append(Integer.toString(msg.arg1));
                }
                break;
            case CMD_IPV4_PROVISIONING_FAILURE:
                sb.append(" ");
                if (msg.arg1 == 2) {
                    sb.append("DHCP_FAIL");
                } else if (msg.arg1 == CMD_STATIC_IP_FAILURE) {
                    sb.append("STATIC_FAIL");
                } else {
                    sb.append(Integer.toString(msg.arg1));
                }
                break;
            case CMD_INSTALL_PACKET_FILTER:
                sb.append(" len=").append(((byte[]) msg.obj).length);
                break;
            case CMD_SET_FALLBACK_PACKET_FILTERING:
                sb.append(" enabled=").append(((Boolean) msg.obj).booleanValue());
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
                String key2 = this.mWifiConfigManager.getLastSelectedConfiguration();
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
                sb.append(String.format(" bcn=%d", Integer.valueOf(this.mRunningBeaconCount)));
                sb.append(String.format(" con=%d", Integer.valueOf(this.mConnectionRequests)));
                String key3 = this.mWifiConfigManager.getLastSelectedConfiguration();
                if (key3 != null) {
                    sb.append(" last=").append(key3);
                }
                break;
            case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT:
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
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
                    WifiConfiguration.NetworkSelectionStatus netWorkSelectionStatus = config6.getNetworkSelectionStatus();
                    sb.append(" cur=").append(config6.configKey());
                    sb.append(" ajst=").append(netWorkSelectionStatus.getNetworkStatusString());
                    if (config6.selfAdded) {
                        sb.append(" selfAdded");
                    }
                    if (config6.status != 0) {
                        sb.append(" st=").append(config6.status);
                        sb.append(" rs=").append(netWorkSelectionStatus.getNetworkDisableReasonString());
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
                break;
            case WifiMonitor.SCAN_FAILED_EVENT:
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
                sb.append(" blacklist=").append(Boolean.toString(this.didBlackListBSSID));
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
                    sb.append(" ajst=").append(this.lastForgetConfigurationAttempt.getNetworkSelectionStatus().getNetworkStatusString());
                }
                break;
            case 196611:
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                sb.append(" txpkts=").append(this.mWifiInfo.txSuccess);
                sb.append(",").append(this.mWifiInfo.txBad);
                sb.append(",").append(this.mWifiInfo.txRetries);
                break;
            case 196612:
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
                    sb.append(" ");
                    sb.append(getLinkPropertiesSummary(this.mLinkProperties));
                }
                break;
            default:
                sb.append(msg.toString());
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                break;
        }
        return sb.toString();
    }

    private void handleScreenStateChanged(boolean screenOn) {
        this.mScreenOn = screenOn;
        if (DBG) {
            logd(" handleScreenStateChanged Enter: screenOn=" + screenOn + " mUserWantsSuspendOpt=" + this.mUserWantsSuspendOpt + " state " + getCurrentState().getName() + " suppState:" + this.mSupplicantStateTracker.getSupplicantStateName());
        }
        enableRssiPolling(screenOn);
        if (this.mUserWantsSuspendOpt.get()) {
            if (screenOn) {
                sendMessage(CMD_SET_SUSPEND_OPT_ENABLED, 0, 0);
            } else {
                this.mSuspendWakeLock.acquire(2000L);
                sendMessage(CMD_SET_SUSPEND_OPT_ENABLED, 1, 0);
            }
        }
        this.mScreenBroadcastReceived.set(true);
        if (hasCustomizedAutoConnect()) {
            sendMessage(M_CMD_UPDATE_SCAN_INTERVAL);
        }
        getWifiLinkLayerStats(false);
        this.mOnTimeScreenStateChange = this.mOnTime;
        this.lastScreenStateChangeTimeStamp = this.lastLinkLayerStatsUpdate;
        this.mWifiMetrics.setScreenState(screenOn);
        if (this.mWifiConnectivityManager != null) {
            this.mWifiConnectivityManager.handleScreenStateChanged(screenOn);
        }
        if (DBG) {
            log("handleScreenStateChanged Exit: " + screenOn);
        }
    }

    private void checkAndSetConnectivityInstance() {
        if (this.mCm != null) {
            return;
        }
        this.mCm = (ConnectivityManager) this.mContext.getSystemService("connectivity");
    }

    private void setFrequencyBand() {
        if (this.mWifiNative.setBand(0)) {
            this.mFrequencyBand.set(0);
            if (this.mWifiConnectivityManager != null) {
                this.mWifiConnectivityManager.setUserPreferredBand(0);
            }
            if (!DBG) {
                return;
            }
            logd("done set frequency band 0");
            return;
        }
        loge("Failed to set frequency band 0");
    }

    private void setSuspendOptimizationsNative(int reason, boolean enabled) {
        if (DBG) {
            log("setSuspendOptimizationsNative: " + reason + " " + enabled + " -want " + this.mUserWantsSuspendOpt.get() + " stack:" + Thread.currentThread().getStackTrace()[2].getMethodName() + " - " + Thread.currentThread().getStackTrace()[3].getMethodName() + " - " + Thread.currentThread().getStackTrace()[4].getMethodName() + " - " + Thread.currentThread().getStackTrace()[5].getMethodName());
        }
        if (!enabled) {
            this.mSuspendOptNeedsDisabled |= reason;
            this.mWifiNative.setSuspendOptimizations(false);
            return;
        }
        this.mSuspendOptNeedsDisabled &= ~reason;
        if (this.mSuspendOptNeedsDisabled == 0 && this.mUserWantsSuspendOpt.get()) {
            if (DBG) {
                log("setSuspendOptimizationsNative do it " + reason + " " + enabled + " stack:" + Thread.currentThread().getStackTrace()[2].getMethodName() + " - " + Thread.currentThread().getStackTrace()[3].getMethodName() + " - " + Thread.currentThread().getStackTrace()[4].getMethodName() + " - " + Thread.currentThread().getStackTrace()[5].getMethodName());
            }
            this.mWifiNative.setSuspendOptimizations(true);
        }
    }

    private void setSuspendOptimizations(int reason, boolean enabled) {
        if (DBG) {
            log("setSuspendOptimizations: " + reason + " " + enabled);
        }
        if (enabled) {
            this.mSuspendOptNeedsDisabled &= ~reason;
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

    private void setWifiApState(int wifiApState, int reason) {
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
            if (wifiApState == 14) {
                intent.putExtra("wifi_ap_error_code", reason);
            }
            this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }
        this.mWifiApState.set(wifiApState);
        if (DBG) {
        }
        Intent intent2 = new Intent("android.net.wifi.WIFI_AP_STATE_CHANGED");
        intent2.addFlags(67108864);
        intent2.putExtra("wifi_state", wifiApState);
        intent2.putExtra("previous_wifi_state", previousWifiApState);
        if (wifiApState == 14) {
        }
        this.mContext.sendStickyBroadcastAsUser(intent2, UserHandle.ALL);
    }

    private void setScanResults() {
        List<WifiConfiguration> associatedWifiConfigurations;
        this.mNumScanResultsKnown = 0;
        this.mNumScanResultsReturned = 0;
        ArrayList<ScanDetail> scanResults = this.mWifiNative.getScanResults();
        if (scanResults.isEmpty()) {
            this.mScanResults = new ArrayList();
            return;
        }
        this.mWifiConfigManager.trimANQPCache(false);
        boolean connected = this.mLastBssid != null;
        long activeBssid = 0;
        if (connected) {
            try {
                activeBssid = Utils.parseMac(this.mLastBssid);
            } catch (IllegalArgumentException e) {
                connected = false;
            }
        }
        synchronized (this.mScanResultsLock) {
            ScanDetail activeScanDetail = null;
            this.mScanResults = scanResults;
            this.mNumScanResultsReturned = this.mScanResults.size();
            for (ScanDetail resultDetail : this.mScanResults) {
                if (connected && resultDetail.getNetworkDetail().getBSSID() == activeBssid && (activeScanDetail == null || activeScanDetail.getNetworkDetail().getBSSID() != activeBssid || activeScanDetail.getNetworkDetail().getANQPElements() == null)) {
                    activeScanDetail = resultDetail;
                }
                NetworkDetail networkDetail = resultDetail.getNetworkDetail();
                if (networkDetail != null && networkDetail.getDtimInterval() > 0 && (associatedWifiConfigurations = this.mWifiConfigManager.getSavedNetworkFromScanDetail(resultDetail)) != null) {
                    for (WifiConfiguration associatedConf : associatedWifiConfigurations) {
                        if (associatedConf != null) {
                            associatedConf.dtimInterval = networkDetail.getDtimInterval();
                        }
                    }
                }
            }
            this.mWifiConfigManager.setActiveScanDetail(activeScanDetail);
        }
        if (!this.linkDebouncing) {
            return;
        }
        sendMessage(CMD_AUTO_ROAM, this.mLastNetworkId, 1, null);
    }

    private void fetchRssiLinkSpeedAndFrequencyNative() {
        Integer newRssi = null;
        Integer newLinkSpeed = null;
        Integer newFrequency = null;
        String signalPoll = this.mWifiNative.signalPoll();
        if (signalPoll != null) {
            String[] lines = signalPoll.split("\n");
            for (String line : lines) {
                String[] prop = line.split("=");
                if (prop.length >= 2) {
                    try {
                        if (prop[0].equals("RSSI")) {
                            newRssi = Integer.valueOf(Integer.parseInt(prop[1]));
                        } else if (prop[0].equals("LINKSPEED")) {
                            newLinkSpeed = Integer.valueOf(Integer.parseInt(prop[1]));
                        } else if (prop[0].equals("FREQUENCY")) {
                            newFrequency = Integer.valueOf(Integer.parseInt(prop[1]));
                        }
                    } catch (NumberFormatException e) {
                    }
                }
            }
        }
        if (DBG) {
            logd("fetchRssiLinkSpeedAndFrequencyNative rssi=" + newRssi + " linkspeed=" + newLinkSpeed + " freq=" + newFrequency);
        }
        if (newRssi == null || newRssi.intValue() <= -127 || newRssi.intValue() >= 200) {
            this.mWifiInfo.setRssi(-127);
            updateCapabilities(getCurrentWifiConfiguration());
        } else {
            if (newRssi.intValue() > 0) {
                newRssi = Integer.valueOf(newRssi.intValue() - 256);
            }
            this.mWifiInfo.setRssi(newRssi.intValue());
            int newSignalLevel = WifiManager.calculateSignalLevel(newRssi.intValue(), 5);
            if (newSignalLevel != this.mLastSignalLevel) {
                updateCapabilities(getCurrentWifiConfiguration());
                sendRssiChangeBroadcast(newRssi.intValue());
            }
            Log.d(TAG, "mLastSignalLevel:" + this.mLastSignalLevel + ", newSignalLevel:" + newSignalLevel);
            this.mLastSignalLevel = newSignalLevel;
        }
        if (hasCustomizedAutoConnect() && newRssi != null && newRssi.intValue() < -85) {
            int ipAddr = this.mWifiInfo.getIpAddress();
            long time = SystemClock.elapsedRealtime();
            boolean autoConnect = this.mWifiFwkExt.shouldAutoConnect();
            Log.d(TAG, "fetchRssi, ip:" + ipAddr + ", mDisconnectOperation:" + this.mDisconnectOperation + ", time:" + time + ", lasttime:" + this.mLastCheckWeakSignalTime);
            if (ipAddr != 0 && !this.mDisconnectOperation && (time - this.mLastCheckWeakSignalTime > PasspointManagementObjectManager.IntervalFactor || autoConnect)) {
                Log.d(TAG, "Rssi < -85, scan for checking signal!");
                if (!autoConnect) {
                    this.mLastCheckWeakSignalTime = time;
                }
                this.mDisconnectNetworkId = this.mLastNetworkId;
                this.mScanForWeakSignal = true;
                this.mWifiNative.bssFlush();
                startScan(-1, 0, null, null);
            }
        }
        if (newLinkSpeed != null) {
            this.mWifiInfo.setLinkSpeed(newLinkSpeed.intValue());
        }
        if (newFrequency != null && newFrequency.intValue() > 0) {
            if (ScanResult.is5GHz(newFrequency.intValue())) {
                this.mWifiConnectionStatistics.num5GhzConnected++;
            }
            if (ScanResult.is24GHz(newFrequency.intValue())) {
                this.mWifiConnectionStatistics.num24GhzConnected++;
            }
            this.mWifiInfo.setFrequency(newFrequency.intValue());
        }
        this.mWifiConfigManager.updateConfiguration(this.mWifiInfo);
    }

    private void cleanWifiScore() {
        this.mWifiInfo.txBadRate = 0.0d;
        this.mWifiInfo.txSuccessRate = 0.0d;
        this.mWifiInfo.txRetriesRate = 0.0d;
        this.mWifiInfo.rxSuccessRate = 0.0d;
        this.mWifiScoreReport = null;
    }

    public double getTxPacketRate() {
        return this.mWifiInfo.txSuccessRate;
    }

    public double getRxPacketRate() {
        return this.mWifiInfo.rxSuccessRate;
    }

    private void fetchPktcntNative(RssiPacketCountInfo info) {
        String pktcntPoll = this.mWifiNative.pktcntPoll();
        if (pktcntPoll == null) {
            return;
        }
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

    private void updateLinkProperties(LinkProperties newLp) {
        if (DBG) {
            log("Link configuration changed for netId: " + this.mLastNetworkId + " old: " + this.mLinkProperties + " new: " + newLp);
        }
        this.mLinkProperties = newLp;
        if (this.mNetworkAgent != null) {
            this.mNetworkAgent.sendLinkProperties(this.mLinkProperties);
        }
        if (getNetworkDetailedState() == NetworkInfo.DetailedState.CONNECTED) {
            sendLinkConfigurationChangedBroadcast();
        }
        if (DBG) {
            StringBuilder sb = new StringBuilder();
            sb.append("updateLinkProperties nid: ").append(this.mLastNetworkId);
            sb.append(" state: ").append(getNetworkDetailedState());
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

    private String updateDefaultRouteMacAddress(int timeout) throws Throwable {
        String address = null;
        for (RouteInfo route : this.mLinkProperties.getRoutes()) {
            if (route.isDefaultRoute() && route.hasGateway()) {
                InetAddress gateway = route.getGateway();
                if (gateway instanceof Inet4Address) {
                    if (DBG) {
                        logd("updateDefaultRouteMacAddress found Ipv4 default :" + gateway.getHostAddress());
                    }
                    address = macAddressFromRoute(gateway.getHostAddress());
                    if (address == null && timeout > 0) {
                        try {
                            boolean reachable = gateway.isReachable(timeout);
                            if (reachable) {
                                address = macAddressFromRoute(gateway.getHostAddress());
                                if (DBG) {
                                    logd("updateDefaultRouteMacAddress reachable (tried again) :" + gateway.getHostAddress() + " found " + address);
                                }
                            }
                        } catch (Exception e) {
                            loge("updateDefaultRouteMacAddress exception reaching :" + gateway.getHostAddress());
                        }
                    }
                    if (address != null) {
                        this.mWifiConfigManager.setDefaultGwMacAddress(this.mLastNetworkId, address);
                    }
                }
            }
        }
        return address;
    }

    void sendScanResultsAvailableBroadcast(boolean scanSucceeded) {
        Intent intent = new Intent("android.net.wifi.SCAN_RESULTS");
        intent.addFlags(67108864);
        intent.putExtra("resultsUpdated", scanSucceeded);
        intent.putExtra("SHOW_RESELECT_DIALOG", this.mShowReselectDialog);
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
            fetchRssiLinkSpeedAndFrequencyNative();
            WifiInfo sentWifiInfo = new WifiInfo(this.mWifiInfo);
            sentWifiInfo.setMacAddress("02:00:00:00:00:00");
            intent.putExtra("wifiInfo", sentWifiInfo);
        }
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    private WifiInfo getWiFiInfoForUid(int uid) {
        if (Binder.getCallingUid() == Process.myUid()) {
            return this.mWifiInfo;
        }
        WifiInfo result = new WifiInfo(this.mWifiInfo);
        result.setMacAddress("02:00:00:00:00:00");
        IBinder binder = this.mFacade.getService("package");
        IPackageManager packageManager = IPackageManager.Stub.asInterface(binder);
        try {
            if (packageManager.checkUidPermission("android.permission.LOCAL_MAC_ADDRESS", uid) == 0) {
                result.setMacAddress(this.mWifiInfo.getMacAddress());
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error checking receiver permission", e);
        }
        return result;
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

    private boolean setNetworkDetailedState(NetworkInfo.DetailedState state) {
        boolean hidden = this.linkDebouncing || isRoaming();
        if (DBG) {
            log("setDetailed state, old =" + this.mNetworkInfo.getDetailedState() + " and new state=" + state + " hidden=" + hidden);
        }
        if (this.mNetworkInfo.getExtraInfo() != null && this.mWifiInfo.getSSID() != null && !this.mWifiInfo.getSSID().equals("<unknown ssid>") && !this.mNetworkInfo.getExtraInfo().equals(this.mWifiInfo.getSSID())) {
            if (DBG) {
                log("setDetailed state send new extra info" + this.mWifiInfo.getSSID());
            }
            this.mNetworkInfo.setExtraInfo(this.mWifiInfo.getSSID());
            sendNetworkStateChangeBroadcast(null);
        }
        if (hidden || state == this.mNetworkInfo.getDetailedState()) {
            return false;
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

    private SupplicantState handleSupplicantStateChange(Message message) {
        StateChangeResult stateChangeResult = (StateChangeResult) message.obj;
        SupplicantState state = stateChangeResult.state;
        this.mWifiInfo.setSupplicantState(state);
        if ((stateChangeResult.wifiSsid == null || stateChangeResult.wifiSsid.toString().isEmpty()) && this.linkDebouncing) {
            return state;
        }
        if (SupplicantState.isConnecting(state)) {
            this.mWifiInfo.setNetworkId(stateChangeResult.networkId);
        } else {
            this.mWifiInfo.setNetworkId(-1);
        }
        this.mWifiInfo.setBSSID(stateChangeResult.BSSID);
        if (this.mWhiteListedSsids != null && this.mWhiteListedSsids.length > 0 && stateChangeResult.wifiSsid != null) {
            String SSID = stateChangeResult.wifiSsid.toString();
            String currentSSID = this.mWifiInfo.getSSID();
            if (SSID != null && currentSSID != null && !SSID.equals("<unknown ssid>")) {
                if (SSID.length() >= 2 && SSID.charAt(0) == '\"' && SSID.charAt(SSID.length() - 1) == '\"') {
                    SSID = SSID.substring(1, SSID.length() - 1);
                }
                if (currentSSID.length() >= 2 && currentSSID.charAt(0) == '\"' && currentSSID.charAt(currentSSID.length() - 1) == '\"') {
                    currentSSID = currentSSID.substring(1, currentSSID.length() - 1);
                }
                if (!SSID.equals(currentSSID) && getCurrentState() == this.mConnectedState) {
                    this.lastConnectAttemptTimestamp = System.currentTimeMillis();
                    this.targetWificonfiguration = this.mWifiConfigManager.getWifiConfiguration(this.mWifiInfo.getNetworkId());
                    transitionTo(this.mRoamingState);
                }
            }
        }
        this.mWifiInfo.setSSID(stateChangeResult.wifiSsid);
        this.mWifiInfo.setEphemeral(this.mWifiConfigManager.isEphemeral(this.mWifiInfo.getNetworkId()));
        if (!this.mWifiInfo.getMeteredHint()) {
            this.mWifiInfo.setMeteredHint(this.mWifiConfigManager.getMeteredHint(this.mWifiInfo.getNetworkId()));
        }
        this.mSupplicantStateTracker.sendMessage(Message.obtain(message));
        return state;
    }

    private void handleNetworkDisconnect() {
        if (DBG) {
            log("Stopping DHCP and clearing IP");
        }
        if (hasCustomizedAutoConnect()) {
            NetworkInfo.DetailedState state = getNetworkDetailedState();
            Log.d(TAG, "handleNetworkDisconnect, state:" + state + ", mDisconnectOperation:" + this.mDisconnectOperation);
            if (state == NetworkInfo.DetailedState.CONNECTED) {
                this.mDisconnectNetworkId = this.mLastNetworkId;
                if (!this.mDisconnectOperation) {
                    this.mScanForWeakSignal = true;
                    this.mWifiNative.bssFlush();
                    startScan(-1, 0, null, null);
                }
            }
            if (!this.mWifiFwkExt.shouldAutoConnect()) {
                disableLastNetwork();
            }
            this.mDisconnectOperation = false;
            this.mLastCheckWeakSignalTime = 0L;
        }
        if (DBG) {
            log("handleNetworkDisconnect: Stopping DHCP and clearing IP stack:" + Thread.currentThread().getStackTrace()[2].getMethodName() + " - " + Thread.currentThread().getStackTrace()[3].getMethodName() + " - " + Thread.currentThread().getStackTrace()[4].getMethodName() + " - " + Thread.currentThread().getStackTrace()[5].getMethodName());
        }
        stopRssiMonitoringOffload();
        clearCurrentConfigBSSID("handleNetworkDisconnect");
        stopIpManager();
        if (this.mMtkCtpppoe && this.mUsingPppoe) {
            stopPPPoE();
        }
        this.mWifiScoreReport = null;
        this.mWifiInfo.reset();
        this.linkDebouncing = false;
        this.mAutoRoaming = false;
        setNetworkDetailedState(NetworkInfo.DetailedState.DISCONNECTED);
        if (this.mNetworkAgent != null) {
            this.mNetworkAgent.sendNetworkInfo(this.mNetworkInfo);
            this.mNetworkAgent = null;
        }
        this.mWifiConfigManager.updateStatus(this.mLastNetworkId, NetworkInfo.DetailedState.DISCONNECTED);
        clearLinkProperties();
        sendNetworkStateChangeBroadcast(this.mLastBssid);
        autoRoamSetBSSID(this.mLastNetworkId, WifiLastResortWatchdog.BSSID_ANY);
        this.mLastBssid = null;
        registerDisconnected();
        this.mLastNetworkId = -1;
    }

    private void handleSupplicantConnectionLoss(boolean killSupplicant) {
        if (killSupplicant) {
            this.mWifiMonitor.killSupplicant(this.mP2pSupported);
        }
        this.mWifiNative.closeSupplicantConnection();
        sendSupplicantConnectionChangedBroadcast(false);
        setWifiState(1);
    }

    void handlePreDhcpSetup() {
        if (!this.mBluetoothConnectionActive) {
            this.mWifiNative.setBluetoothCoexistenceMode(1);
        }
        setSuspendOptimizationsNative(1, false);
        this.mWifiNative.setPowerSave(false);
        getWifiLinkLayerStats(false);
        Message msg = new Message();
        msg.what = WifiP2pServiceImpl.BLOCK_DISCOVERY;
        msg.arg1 = 1;
        msg.arg2 = 196614;
        msg.obj = this;
        this.mWifiP2pChannel.sendMessage(msg);
    }

    void handlePostDhcpSetup() {
        setSuspendOptimizationsNative(1, true);
        this.mWifiNative.setPowerSave(true);
        this.mWifiP2pChannel.sendMessage(WifiP2pServiceImpl.BLOCK_DISCOVERY, 0);
        this.mWifiNative.setBluetoothCoexistenceMode(2);
    }

    private void reportConnectionAttemptEnd(int level2FailureCode, int connectivityFailureCode) {
        this.mWifiMetrics.endConnectionEvent(level2FailureCode, connectivityFailureCode);
        switch (level2FailureCode) {
            case 1:
            case 8:
                break;
            default:
                this.mWifiLogger.reportConnectionFailure();
                break;
        }
    }

    private void handleIPv4Success(DhcpResults dhcpResults) {
        Inet4Address addr;
        if (DBG) {
            logd("handleIPv4Success <" + dhcpResults.toString() + ">");
            logd("link address " + dhcpResults.ipAddress);
        }
        synchronized (this.mDhcpResultsLock) {
            String ssid = getCurrentWifiConfiguration().getPrintableSsid();
            Log.d(TAG, "IP recover: record put: " + ssid);
            mDhcpResultMap.put(ssid, new DhcpResults(dhcpResults));
            this.mDhcpResults = dhcpResults;
            addr = (Inet4Address) dhcpResults.ipAddress.getAddress();
        }
        if (isRoaming()) {
            int previousAddress = this.mWifiInfo.getIpAddress();
            int newAddress = NetworkUtils.inetAddressToInt(addr);
            if (previousAddress != newAddress) {
                logd("handleIPv4Success, roaming and address changed" + this.mWifiInfo + " got: " + addr);
            }
        }
        this.mWifiInfo.setInetAddress(addr);
        if (this.mWifiInfo.getMeteredHint()) {
            return;
        }
        this.mWifiInfo.setMeteredHint(dhcpResults.hasMeteredHint());
        updateCapabilities(getCurrentWifiConfiguration());
    }

    private void handleSuccessfulIpConfiguration() {
        this.mLastSignalLevel = -1;
        WifiConfiguration c = getCurrentWifiConfiguration();
        if (c != null) {
            c.getNetworkSelectionStatus().clearDisableReasonCounter(4);
            updateCapabilities(c);
        }
        if (c == null) {
            return;
        }
        ScanResult result = getCurrentScanResult();
        if (result == null) {
            logd("WifiStateMachine: handleSuccessfulIpConfiguration and no scan results" + c.configKey());
        } else {
            result.numIpConfigFailures = 0;
            this.mWifiConfigManager.clearBssidBlacklist();
        }
    }

    private void handleIPv4Failure() {
        this.mWifiLogger.captureBugReportData(4);
        if (DBG) {
            int count = -1;
            WifiConfiguration config = getCurrentWifiConfiguration();
            if (config != null) {
                count = config.getNetworkSelectionStatus().getDisableReasonCounter(4);
            }
            log("DHCP failure count=" + count);
        }
        reportConnectionAttemptEnd(10, 2);
        synchronized (this.mDhcpResultsLock) {
            if (this.mDhcpResults != null) {
                this.mDhcpResults.clear();
            }
        }
        if (!DBG) {
            return;
        }
        logd("handleIPv4Failure");
    }

    private void handleIpConfigurationLost() {
        this.mWifiInfo.setInetAddress(null);
        this.mWifiInfo.setMeteredHint(false);
        this.mWifiConfigManager.updateNetworkSelectionStatus(this.mLastNetworkId, 4);
        this.mWifiNative.disconnect();
        if (!hasCustomizedAutoConnect()) {
            return;
        }
        this.mIpConfigLost = true;
        this.mDisconnectOperation = true;
    }

    private void handleIpReachabilityLost() {
        this.mWifiInfo.setInetAddress(null);
        this.mWifiInfo.setMeteredHint(false);
        this.mWifiNative.disconnect();
    }

    private int convertFrequencyToChannelNumber(int frequency) {
        if (frequency >= 2412 && frequency <= 2484) {
            return ((frequency - 2412) / 5) + 1;
        }
        if (frequency >= 5170 && frequency <= 5825) {
            return ((frequency - 5170) / 5) + 34;
        }
        return 0;
    }

    private int chooseApChannel(int apBand) {
        int apChannel;
        if (apBand == 0) {
            ArrayList<Integer> allowed2GChannel = this.mWifiApConfigStore.getAllowed2GChannel();
            if (allowed2GChannel == null || allowed2GChannel.size() == 0) {
                if (DBG) {
                    Log.d(TAG, "No specified 2G allowed channel list");
                }
                apChannel = 6;
            } else {
                int index = mRandom.nextInt(allowed2GChannel.size());
                apChannel = allowed2GChannel.get(index).intValue();
            }
        } else {
            int[] channel = this.mWifiNative.getChannelsForBand(2);
            if (channel != null && channel.length > 0) {
                int apChannel2 = channel[mRandom.nextInt(channel.length)];
                apChannel = convertFrequencyToChannelNumber(apChannel2);
            } else {
                Log.e(TAG, "SoftAp do not get available channel list");
                apChannel = 0;
            }
        }
        if (DBG) {
            Log.d(TAG, "SoftAp set on channel " + apChannel);
        }
        return apChannel;
    }

    private boolean setupDriverForSoftAp() {
        if (!this.mWifiNative.loadDriver()) {
            Log.e(TAG, "Failed to load driver for softap");
            return false;
        }
        int index = this.mWifiNative.queryInterfaceIndex(this.mInterfaceName);
        if (index != -1) {
            if (!this.mWifiNative.setInterfaceUp(false)) {
                Log.e(TAG, "toggleInterface failed");
                return false;
            }
        } else if (DBG) {
            Log.d(TAG, "No interfaces to bring down");
        }
        try {
            this.mNwService.wifiFirmwareReload(this.mInterfaceName, "AP");
            if (DBG) {
                Log.d(TAG, "Firmware reloaded in AP mode");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to reload AP firmware " + e);
        }
        if (!this.mWifiNative.startHal()) {
            Log.e(TAG, "Failed to start HAL");
            return true;
        }
        return true;
    }

    private byte[] macAddressFromString(String macString) {
        String[] macBytes = macString.split(":");
        if (macBytes.length != 6) {
            throw new IllegalArgumentException("MAC address should be 6 bytes long!");
        }
        byte[] mac = new byte[6];
        for (int i = 0; i < macBytes.length; i++) {
            Integer hexVal = Integer.valueOf(Integer.parseInt(macBytes[i], 16));
            mac[i] = hexVal.byteValue();
        }
        return mac;
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
        } catch (FileNotFoundException e) {
        } catch (IOException e2) {
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
                } catch (IOException e3) {
                }
            }
            reader2 = reader;
        } catch (FileNotFoundException e4) {
            reader2 = reader;
            loge("Could not open /proc/net/arp to lookup mac address");
            if (reader2 != null) {
                try {
                    reader2.close();
                } catch (IOException e5) {
                }
            }
        } catch (IOException e6) {
            reader2 = reader;
            loge("Could not read /proc/net/arp to lookup mac address");
            if (reader2 != null) {
                try {
                    reader2.close();
                } catch (IOException e7) {
                }
            }
        } catch (Throwable th2) {
            th = th2;
            reader2 = reader;
            if (reader2 != null) {
                try {
                    reader2.close();
                } catch (IOException e8) {
                }
            }
            throw th;
        }
        return macAddress;
    }

    private class WifiNetworkFactory extends NetworkFactory {
        public WifiNetworkFactory(Looper l, Context c, String TAG, NetworkCapabilities f) {
            super(l, c, TAG, f);
        }

        protected void needNetworkFor(NetworkRequest networkRequest, int score) {
            WifiStateMachine.this.mConnectionRequests++;
        }

        protected void releaseNetworkFor(NetworkRequest networkRequest) {
            WifiStateMachine wifiStateMachine = WifiStateMachine.this;
            wifiStateMachine.mConnectionRequests--;
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
            if (networkRequest.networkCapabilities.hasCapability(14)) {
                return;
            }
            int i = this.mUntrustedReqCount + 1;
            this.mUntrustedReqCount = i;
            if (i != 1 || WifiStateMachine.this.mWifiConnectivityManager == null) {
                return;
            }
            WifiStateMachine.this.mWifiConnectivityManager.setUntrustedConnectionAllowed(true);
        }

        protected void releaseNetworkFor(NetworkRequest networkRequest) {
            if (networkRequest.networkCapabilities.hasCapability(14)) {
                return;
            }
            int i = this.mUntrustedReqCount - 1;
            this.mUntrustedReqCount = i;
            if (i != 0 || WifiStateMachine.this.mWifiConnectivityManager == null) {
                return;
            }
            WifiStateMachine.this.mWifiConnectivityManager.setUntrustedConnectionAllowed(false);
        }

        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            pw.println("mUntrustedReqCount " + this.mUntrustedReqCount);
        }
    }

    void maybeRegisterNetworkFactory() {
        if (this.mNetworkFactory != null) {
            return;
        }
        checkAndSetConnectivityInstance();
        if (this.mCm == null) {
            return;
        }
        this.mNetworkFactory = new WifiNetworkFactory(getHandler().getLooper(), this.mContext, NETWORKTYPE, this.mNetworkCapabilitiesFilter);
        this.mNetworkFactory.setScoreFilter(60);
        this.mNetworkFactory.register();
        this.mUntrustedNetworkFactory = new UntrustedWifiNetworkFactory(getHandler().getLooper(), this.mContext, NETWORKTYPE_UNTRUSTED, this.mNetworkCapabilitiesFilter);
        this.mUntrustedNetworkFactory.setScoreFilter(Integer.MAX_VALUE);
        this.mUntrustedNetworkFactory.register();
    }

    class DefaultState extends State {
        DefaultState() {
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, this);
            switch (message.what) {
                case 69632:
                    AsyncChannel ac = (AsyncChannel) message.obj;
                    if (ac != WifiStateMachine.this.mWifiP2pChannel) {
                        WifiStateMachine.this.loge("got HALF_CONNECTED for unknown channel");
                    } else if (message.arg1 == 0) {
                        WifiStateMachine.this.mWifiP2pChannel.sendMessage(69633);
                    } else {
                        WifiStateMachine.this.loge("WifiP2pService connection failure, error=" + message.arg1);
                    }
                    break;
                case 69636:
                    AsyncChannel ac2 = (AsyncChannel) message.obj;
                    if (ac2 == WifiStateMachine.this.mWifiP2pChannel) {
                        WifiStateMachine.this.loge("WifiP2pService channel lost, message.arg1 =" + message.arg1);
                    }
                    break;
                case WifiStateMachine.CMD_START_SUPPLICANT:
                case WifiStateMachine.CMD_STOP_SUPPLICANT:
                case WifiStateMachine.CMD_START_DRIVER:
                case WifiStateMachine.CMD_STOP_DRIVER:
                case WifiStateMachine.CMD_STOP_SUPPLICANT_FAILED:
                case WifiStateMachine.CMD_DRIVER_START_TIMED_OUT:
                case WifiStateMachine.CMD_START_AP:
                case WifiStateMachine.CMD_START_AP_FAILURE:
                case WifiStateMachine.CMD_STOP_AP:
                case WifiStateMachine.CMD_AP_STOPPED:
                case WifiStateMachine.CMD_ENABLE_ALL_NETWORKS:
                case WifiStateMachine.CMD_BLACKLIST_NETWORK:
                case WifiStateMachine.CMD_CLEAR_BLACKLIST:
                case WifiStateMachine.CMD_SET_OPERATIONAL_MODE:
                case WifiStateMachine.CMD_DISCONNECT:
                case WifiStateMachine.CMD_RECONNECT:
                case WifiStateMachine.CMD_REASSOCIATE:
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
                case WifiStateMachine.CMD_UPDATE_ASSOCIATED_SCAN_PERMISSION:
                case WifiMonitor.SUP_CONNECTION_EVENT:
                case WifiMonitor.SUP_DISCONNECTION_EVENT:
                case WifiMonitor.NETWORK_CONNECTION_EVENT:
                case WifiMonitor.NETWORK_DISCONNECTION_EVENT:
                case WifiMonitor.SCAN_RESULTS_EVENT:
                case WifiMonitor.AUTHENTICATION_FAILURE_EVENT:
                case WifiMonitor.WPS_OVERLAP_EVENT:
                case WifiMonitor.SUP_REQUEST_IDENTITY:
                case WifiMonitor.SUP_REQUEST_SIM_AUTH:
                case WifiMonitor.SCAN_FAILED_EVENT:
                case WifiMonitor.ASSOCIATION_REJECTION_EVENT:
                case 196611:
                case 196612:
                case 196614:
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DISCARD;
                    break;
                case WifiStateMachine.CMD_BLUETOOTH_ADAPTER_STATE_CHANGE:
                    WifiStateMachine.this.mBluetoothConnectionActive = message.arg1 != 0;
                    break;
                case WifiStateMachine.CMD_PING_SUPPLICANT:
                case WifiStateMachine.CMD_ADD_OR_UPDATE_NETWORK:
                case WifiStateMachine.CMD_REMOVE_NETWORK:
                case WifiStateMachine.CMD_ENABLE_NETWORK:
                case WifiStateMachine.CMD_SAVE_CONFIG:
                    WifiStateMachine.this.replyToMessage(message, message.what, -1);
                    break;
                case WifiStateMachine.CMD_GET_CONFIGURED_NETWORKS:
                    WifiStateMachine.this.replyToMessage(message, message.what, (List) null);
                    break;
                case WifiStateMachine.CMD_GET_CAPABILITY_FREQ:
                    WifiStateMachine.this.replyToMessage(message, message.what, (Object) null);
                    break;
                case WifiStateMachine.CMD_GET_SUPPORTED_FEATURES:
                    int featureSet = WifiStateMachine.this.mWifiNative.getSupportedFeatureSet();
                    WifiStateMachine.this.replyToMessage(message, message.what, featureSet);
                    break;
                case WifiStateMachine.CMD_GET_PRIVILEGED_CONFIGURED_NETWORKS:
                    WifiStateMachine.this.replyToMessage(message, message.what, (List) null);
                    break;
                case WifiStateMachine.CMD_GET_LINK_LAYER_STATS:
                    WifiStateMachine.this.replyToMessage(message, message.what, (Object) null);
                    break;
                case WifiStateMachine.CMD_START_SCAN:
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DISCARD;
                    break;
                case WifiStateMachine.CMD_GET_CONNECTION_STATISTICS:
                    WifiStateMachine.this.replyToMessage(message, message.what, WifiStateMachine.this.mWifiConnectionStatistics);
                    break;
                case WifiStateMachine.CMD_SET_HIGH_PERF_MODE:
                    if (message.arg1 == 1) {
                        WifiStateMachine.this.setSuspendOptimizations(2, false);
                    } else {
                        WifiStateMachine.this.setSuspendOptimizations(2, true);
                    }
                    break;
                case WifiStateMachine.CMD_ENABLE_RSSI_POLL:
                    WifiStateMachine.this.mEnableRssiPolling = message.arg1 == 1;
                    break;
                case WifiStateMachine.CMD_SET_SUSPEND_OPT_ENABLED:
                    if (message.arg1 == 1) {
                        WifiStateMachine.this.mSuspendWakeLock.release();
                        WifiStateMachine.this.setSuspendOptimizations(4, true);
                    } else {
                        WifiStateMachine.this.setSuspendOptimizations(4, false);
                    }
                    break;
                case WifiStateMachine.CMD_SCREEN_STATE_CHANGED:
                    WifiStateMachine.this.handleScreenStateChanged(message.arg1 != 0);
                    break;
                case WifiStateMachine.CMD_REMOVE_APP_CONFIGURATIONS:
                    WifiStateMachine.this.deferMessage(message);
                    break;
                case WifiStateMachine.CMD_GET_MATCHING_CONFIG:
                    WifiStateMachine.this.replyToMessage(message, message.what);
                    break;
                case WifiStateMachine.CMD_FIRMWARE_ALERT:
                    if (WifiStateMachine.this.mWifiLogger != null) {
                        byte[] buffer = (byte[]) message.obj;
                        WifiStateMachine.this.mWifiLogger.captureAlertData(message.arg1, buffer);
                    }
                    break;
                case WifiStateMachine.CMD_RESET_SIM_NETWORKS:
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DEFERRED;
                    WifiStateMachine.this.deferMessage(message);
                    break;
                case WifiStateMachine.CMD_ADD_PASSPOINT_MO:
                case WifiStateMachine.CMD_MODIFY_PASSPOINT_MO:
                case WifiStateMachine.CMD_QUERY_OSU_ICON:
                case WifiStateMachine.CMD_MATCH_PROVIDER_NETWORK:
                    WifiStateMachine.this.replyToMessage(message, message.what);
                    break;
                case WifiStateMachine.CMD_BOOT_COMPLETED:
                    WifiStateMachine.this.maybeRegisterNetworkFactory();
                    break;
                case WifiStateMachine.CMD_IP_CONFIGURATION_SUCCESSFUL:
                case WifiStateMachine.CMD_IP_CONFIGURATION_LOST:
                case WifiStateMachine.CMD_IP_REACHABILITY_LOST:
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DISCARD;
                    break;
                case WifiStateMachine.CMD_UPDATE_LINKPROPERTIES:
                    WifiStateMachine.this.updateLinkProperties((LinkProperties) message.obj);
                    break;
                case WifiStateMachine.CMD_REMOVE_USER_CONFIGURATIONS:
                    WifiStateMachine.this.deferMessage(message);
                    break;
                case WifiStateMachine.CMD_START_IP_PACKET_OFFLOAD:
                    if (WifiStateMachine.this.mNetworkAgent != null) {
                        WifiStateMachine.this.mNetworkAgent.onPacketKeepaliveEvent(message.arg1, -20);
                    }
                    break;
                case WifiStateMachine.CMD_STOP_IP_PACKET_OFFLOAD:
                    if (WifiStateMachine.this.mNetworkAgent != null) {
                        WifiStateMachine.this.mNetworkAgent.onPacketKeepaliveEvent(message.arg1, -20);
                    }
                    break;
                case WifiStateMachine.CMD_START_RSSI_MONITORING_OFFLOAD:
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DISCARD;
                    break;
                case WifiStateMachine.CMD_STOP_RSSI_MONITORING_OFFLOAD:
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DISCARD;
                    break;
                case WifiStateMachine.CMD_USER_SWITCH:
                    WifiStateMachine.this.mWifiConfigManager.handleUserSwitch(message.arg1);
                    break;
                case WifiStateMachine.CMD_INSTALL_PACKET_FILTER:
                    WifiStateMachine.this.mWifiNative.installPacketFilter((byte[]) message.obj);
                    break;
                case WifiStateMachine.CMD_SET_FALLBACK_PACKET_FILTERING:
                    if (((Boolean) message.obj).booleanValue()) {
                        WifiStateMachine.this.mWifiNative.startFilteringMulticastV4Packets();
                    } else {
                        WifiStateMachine.this.mWifiNative.stopFilteringMulticastV4Packets();
                    }
                    break;
                case WifiStateMachine.M_CMD_DO_CTIA_TEST_ON:
                case WifiStateMachine.M_CMD_DO_CTIA_TEST_OFF:
                case WifiStateMachine.M_CMD_DO_CTIA_TEST_RATE:
                    WifiStateMachine.this.replyToMessage(message, message.what, -1);
                    break;
                case WifiStateMachine.M_CMD_SET_TX_POWER_ENABLED:
                    WifiNative unused = WifiStateMachine.this.mWifiNative;
                    boolean ok = WifiNative.setTxPowerEnabled(message.arg1 == 1);
                    WifiStateMachine.this.replyToMessage(message, message.what, ok ? 1 : -1);
                    break;
                case WifiStateMachine.M_CMD_SET_TX_POWER:
                    WifiNative unused2 = WifiStateMachine.this.mWifiNative;
                    boolean ok2 = WifiNative.setTxPower(message.arg1);
                    WifiStateMachine.this.replyToMessage(message, message.what, ok2 ? 1 : -1);
                    break;
                case WifiStateMachine.M_CMD_GET_WIFI_STATUS:
                case WifiStateMachine.M_CMD_GET_TEST_ENV:
                    WifiStateMachine.this.replyToMessage(message, message.what, (Object) null);
                    break;
                case WifiStateMachine.M_CMD_SET_POWER_SAVING_MODE:
                case WifiStateMachine.M_CMD_FLUSH_BSS:
                case WifiStateMachine.M_CMD_SET_TDLS_POWER_SAVE:
                    break;
                case WifiStateMachine.M_CMD_SET_WOWLAN_NORMAL_MODE:
                    boolean ok3 = WifiStateMachine.this.mWifiNative.setWoWlanNormalModeCommand();
                    WifiStateMachine.this.replyToMessage(message, message.what, ok3 ? 1 : -1);
                    break;
                case WifiStateMachine.M_CMD_SET_WOWLAN_MAGIC_MODE:
                    boolean ok4 = WifiStateMachine.this.mWifiNative.setWoWlanMagicModeCommand();
                    WifiStateMachine.this.replyToMessage(message, message.what, ok4 ? 1 : -1);
                    break;
                case WifiStateMachine.M_CMD_FACTORY_RESET:
                    WifiStateMachine.this.deferMessage(message);
                    break;
                case WifiStateMachine.M_CMD_ENABLE_EAP_SIM_CONFIG_NETWORK:
                    List<WifiConfiguration> networks = WifiStateMachine.this.mWifiConfigManager.getSavedNetworks();
                    if (networks == null) {
                        WifiStateMachine.this.log("Check for EAP_SIM_AKA, networks is null!");
                        break;
                    } else {
                        boolean isSimConfigExisted = false;
                        for (WifiConfiguration network : networks) {
                            if (WifiStateMachine.this.mWifiConfigManager.isSimConfig(network) && network.getNetworkSelectionStatus().getNetworkSelectionDisableReason() == 10) {
                                WifiConfiguration eapSimConfig = WifiStateMachine.this.mWifiConfigManager.getWifiConfiguration(network.networkId);
                                if (WifiStateMachine.this.isConfigSimCardLoaded(eapSimConfig)) {
                                    WifiStateMachine.this.mWifiConfigManager.updateNetworkSelectionStatus(eapSimConfig, 0);
                                    isSimConfigExisted = true;
                                }
                            }
                        }
                        if (isSimConfigExisted && WifiStateMachine.this.mWifiConnectivityManager != null) {
                            WifiStateMachine.this.mWifiConnectivityManager.handleScanStrategyChanged();
                            break;
                        }
                    }
                    break;
                case WifiP2pServiceImpl.P2P_CONNECTION_CHANGED:
                    NetworkInfo info = (NetworkInfo) message.obj;
                    WifiStateMachine.this.mP2pConnected.set(info.isConnected());
                    break;
                case WifiP2pServiceImpl.DISCONNECT_WIFI_REQUEST:
                    WifiStateMachine.this.mTemporarilyDisconnectWifi = message.arg1 == 1;
                    WifiStateMachine.this.replyToMessage(message, WifiP2pServiceImpl.DISCONNECT_WIFI_RESPONSE);
                    break;
                case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT:
                    if (WifiStateMachine.DBG) {
                        WifiStateMachine.this.log("handle SUPPLICANT_STATE_CHANGE_EVENT in default state");
                    }
                    StateChangeResult stateChangeResult = (StateChangeResult) message.obj;
                    if (WifiStateMachine.DBG) {
                        WifiStateMachine.this.loge("SUPPLICANT_STATE_CHANGE_EVENT state=" + stateChangeResult.state + " -> state= " + WifiInfo.getDetailedStateOf(stateChangeResult.state) + " debouncing=" + WifiStateMachine.this.linkDebouncing);
                    }
                    SupplicantState state = stateChangeResult.state;
                    WifiStateMachine.this.mWifiInfo.setSupplicantState(state);
                    WifiStateMachine.this.mWifiInfo.setNetworkId(-1);
                    WifiStateMachine.this.mWifiInfo.setBSSID(stateChangeResult.BSSID);
                    WifiStateMachine.this.mWifiInfo.setSSID(stateChangeResult.wifiSsid);
                    break;
                case WifiMonitor.DRIVER_HUNG_EVENT:
                    WifiStateMachine.this.setSupplicantRunning(false);
                    WifiStateMachine.this.setSupplicantRunning(true);
                    break;
                case 151553:
                    WifiStateMachine.this.replyToMessage(message, 151554, 2);
                    break;
                case 151556:
                    WifiStateMachine.this.replyToMessage(message, 151557, 2);
                    break;
                case 151559:
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_FAIL;
                    WifiStateMachine.this.replyToMessage(message, 151560, 2);
                    break;
                case 151562:
                    WifiStateMachine.this.replyToMessage(message, 151564, 2);
                    break;
                case 151566:
                    WifiStateMachine.this.replyToMessage(message, 151567, 2);
                    break;
                case 151569:
                    WifiStateMachine.this.replyToMessage(message, 151570, 2);
                    break;
                case 151572:
                    WifiStateMachine.this.replyToMessage(message, 151574, 2);
                    break;
                case 151575:
                    if (WifiStateMachine.this.mMtkCtpppoe) {
                        WifiStateMachine.this.replyToMessage(message, 151577, 2);
                    } else {
                        WifiStateMachine.this.replyToMessage(message, 151577, 0);
                    }
                    break;
                case 151578:
                    if (WifiStateMachine.this.mMtkCtpppoe) {
                        WifiStateMachine.this.replyToMessage(message, 151580, 2);
                    } else {
                        WifiStateMachine.this.replyToMessage(message, 151580, 0);
                    }
                    break;
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
                    break;
                default:
                    WifiStateMachine.this.loge("Error! unhandled message" + message);
                    break;
            }
            return true;
        }
    }

    class InitialState extends State {
        InitialState() {
        }

        public void enter() {
            WifiStateMachine.this.mWifiNative.stopHal();
            WifiStateMachine.this.mWifiNative.unloadDriver();
            if (WifiStateMachine.this.mWifiP2pChannel == null) {
                WifiStateMachine.this.mWifiP2pChannel = new AsyncChannel();
                WifiStateMachine.this.mWifiP2pChannel.connect(WifiStateMachine.this.mContext, WifiStateMachine.this.getHandler(), WifiStateMachine.this.mWifiP2pServiceImpl.getP2pStateMachineMessenger());
            }
            if (WifiStateMachine.this.mWifiApConfigStore == null) {
                WifiStateMachine.this.mWifiApConfigStore = WifiStateMachine.this.mFacade.makeApConfigStore(WifiStateMachine.this.mContext, WifiStateMachine.this.mBackupManagerProxy);
            }
            WifiStateMachine.this.lastConnectAttemptTimestamp = 0L;
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, this);
            switch (message.what) {
                case WifiStateMachine.CMD_START_SUPPLICANT:
                    if (WifiStateMachine.this.mWifiNative.loadDriver()) {
                        try {
                            WifiStateMachine.this.mNwService.wifiFirmwareReload(WifiStateMachine.this.mInterfaceName, "STA");
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
                            WifiStateMachine.this.mWifiMonitor.killSupplicant(WifiStateMachine.this.mP2pSupported);
                            if (!WifiStateMachine.this.mWifiNative.startHal()) {
                                WifiStateMachine.this.loge("Failed to start HAL");
                            }
                            if (WifiStateMachine.this.mWifiNative.startSupplicant(WifiStateMachine.this.mP2pSupported)) {
                                WifiStateMachine.this.setWifiState(2);
                                if (WifiStateMachine.DBG) {
                                    WifiStateMachine.this.log("Supplicant start successful");
                                }
                                WifiStateMachine.this.mWifiMonitor.startMonitoring(WifiStateMachine.this.mInterfaceName);
                                WifiStateMachine.this.transitionTo(WifiStateMachine.this.mSupplicantStartingState);
                            } else {
                                WifiStateMachine.this.loge("Failed to start supplicant!");
                                WifiStateMachine.this.setWifiState(4);
                                ExceptionLog exceptionLog = null;
                                try {
                                    if (WifiStateMachine.this.mPropertyService.get("ro.have_aee_feature", "").equals("1")) {
                                        ExceptionLog exceptionLog2 = new ExceptionLog();
                                        exceptionLog = exceptionLog2;
                                    }
                                    if (exceptionLog != null) {
                                        exceptionLog.systemreport((byte) 1, "CRDISPATCH_KEY:WifiStateMachine", "Failed to start supplicant!", "/data/cursorleak/traces.txt");
                                    }
                                    break;
                                } catch (Exception e) {
                                }
                                return true;
                            }
                        } catch (Exception e2) {
                            WifiStateMachine.this.loge("Failed to reload STA firmware " + e2);
                            WifiStateMachine.this.loge("fwreload fail, unloadDriver");
                            WifiStateMachine.this.mWifiNative.unloadDriver();
                            ExceptionLog exceptionLog3 = null;
                            try {
                                if (WifiStateMachine.this.mPropertyService.get("ro.have_aee_feature", "").equals("1")) {
                                    ExceptionLog exceptionLog4 = new ExceptionLog();
                                    exceptionLog3 = exceptionLog4;
                                }
                                if (exceptionLog3 != null) {
                                    exceptionLog3.systemreport((byte) 1, "CRDISPATCH_KEY:WifiStateMachine", "fwreload fails", "/data/cursorleak/traces.txt");
                                }
                                break;
                            } catch (Exception e3) {
                            }
                            WifiStateMachine.this.setWifiState(4);
                            return true;
                        }
                        break;
                    } else {
                        WifiStateMachine.this.loge("Failed to load driver");
                        ExceptionLog exceptionLog5 = null;
                        try {
                            if (WifiStateMachine.this.mPropertyService.get("ro.have_aee_feature", "").equals("1")) {
                                ExceptionLog exceptionLog6 = new ExceptionLog();
                                exceptionLog5 = exceptionLog6;
                            }
                            if (exceptionLog5 != null) {
                                exceptionLog5.systemreport((byte) 1, "CRDISPATCH_KEY:WifiStateMachine", "loadDriver fails", "/data/cursorleak/traces.txt");
                            }
                            break;
                        } catch (Exception e4) {
                        }
                        WifiStateMachine.this.setWifiState(4);
                    }
                    return true;
                case WifiStateMachine.CMD_START_AP:
                    if (WifiStateMachine.this.setupDriverForSoftAp()) {
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mSoftApState);
                    } else {
                        WifiStateMachine.this.setWifiApState(14, 0);
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mInitialState);
                    }
                    return true;
                default:
                    return false;
            }
        }
    }

    class SupplicantStartingState extends State {
        SupplicantStartingState() {
        }

        public void enter() {
            if (WifiStateMachine.DBG) {
                WifiStateMachine.this.loge(getName() + "\n");
            }
        }

        private void initializeWpsDetails() {
            String detail = WifiStateMachine.this.mPropertyService.get("ro.product.name", "");
            if (!WifiStateMachine.this.mWifiNative.setDeviceName(detail)) {
                WifiStateMachine.this.loge("Failed to set device name " + detail);
            }
            String detail2 = WifiStateMachine.this.mPropertyService.get("ro.product.manufacturer", "");
            if (!WifiStateMachine.this.mWifiNative.setManufacturer(detail2)) {
                WifiStateMachine.this.loge("Failed to set manufacturer " + detail2);
            }
            String detail3 = WifiStateMachine.this.mPropertyService.get("ro.product.model", "");
            if (!WifiStateMachine.this.mWifiNative.setModelName(detail3)) {
                WifiStateMachine.this.loge("Failed to set model name " + detail3);
            }
            String detail4 = WifiStateMachine.this.mPropertyService.get("ro.product.model", "");
            if (!WifiStateMachine.this.mWifiNative.setModelNumber(detail4)) {
                WifiStateMachine.this.loge("Failed to set model number " + detail4);
            }
            String detail5 = WifiStateMachine.this.mPropertyService.get("ro.serialno", "");
            if (!WifiStateMachine.this.mWifiNative.setSerialNumber(detail5)) {
                WifiStateMachine.this.loge("Failed to set serial number " + detail5);
            }
            if (!WifiStateMachine.this.mWifiNative.setConfigMethods("physical_display virtual_push_button")) {
                WifiStateMachine.this.loge("Failed to set WPS config methods");
            }
            if (WifiStateMachine.this.mWifiNative.setDeviceType(WifiStateMachine.this.mPrimaryDeviceType)) {
                return;
            }
            WifiStateMachine.this.loge("Failed to set primary device type " + WifiStateMachine.this.mPrimaryDeviceType);
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, this);
            switch (message.what) {
                case WifiStateMachine.CMD_START_SUPPLICANT:
                case WifiStateMachine.CMD_STOP_SUPPLICANT:
                case WifiStateMachine.CMD_START_DRIVER:
                case WifiStateMachine.CMD_STOP_DRIVER:
                case WifiStateMachine.CMD_START_AP:
                case WifiStateMachine.CMD_STOP_AP:
                case WifiStateMachine.CMD_SET_FREQUENCY_BAND:
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DEFERRED;
                    WifiStateMachine.this.deferMessage(message);
                    return true;
                case WifiStateMachine.CMD_SET_OPERATIONAL_MODE:
                    WifiStateMachine.this.mOperationalMode = message.arg1;
                    WifiStateMachine.this.deferMessage(message);
                    return true;
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
                    WifiStateMachine.this.setFrequencyBand();
                    WifiStateMachine.this.mWifiNative.enableSaveConfig();
                    WifiStateMachine.this.mWifiConfigManager.loadAndEnableAllNetworks();
                    if (WifiStateMachine.this.mWifiConfigManager.mEnableVerboseLogging.get() > 0) {
                        WifiStateMachine.this.enableVerboseLogging(WifiStateMachine.this.mWifiConfigManager.mEnableVerboseLogging.get());
                    }
                    initializeWpsDetails();
                    WifiStateMachine.this.mConnectNetwork = false;
                    WifiStateMachine.this.mLastExplicitNetworkId = -1;
                    WifiStateMachine.this.mOnlineStartTime = 0L;
                    WifiStateMachine.this.mUsingPppoe = false;
                    WifiStateMachine.this.mConnectNetwork = false;
                    if (WifiStateMachine.this.hasCustomizedAutoConnect()) {
                        WifiStateMachine.this.mWifiNative.setBssExpireAge(10);
                        WifiStateMachine.this.mWifiNative.setBssExpireCount(1);
                        WifiStateMachine.this.mDisconnectOperation = false;
                        WifiStateMachine.this.mScanForWeakSignal = false;
                        WifiStateMachine.this.mShowReselectDialog = false;
                        WifiStateMachine.this.mIpConfigLost = false;
                        WifiStateMachine.this.mLastCheckWeakSignalTime = 0L;
                        if (!WifiStateMachine.this.mWifiFwkExt.shouldAutoConnect()) {
                            WifiStateMachine.this.disableAllNetworks(false);
                        }
                    }
                    if (WifiStateMachine.this.isAirplaneModeOn()) {
                        List<WifiConfiguration> networks = WifiStateMachine.this.mWifiConfigManager.getSavedNetworks();
                        if (networks != null) {
                            for (WifiConfiguration network : networks) {
                                if (WifiStateMachine.this.mWifiConfigManager.isSimConfig(network)) {
                                    WifiStateMachine.this.mWifiConfigManager.disableNetwork(network.networkId);
                                    WifiConfiguration eapSimConfig = WifiStateMachine.this.mWifiConfigManager.getWifiConfiguration(network.networkId);
                                    WifiStateMachine.this.mWifiConfigManager.updateNetworkSelectionStatus(eapSimConfig, 10);
                                }
                            }
                        } else {
                            WifiStateMachine.this.log("Check for EAP_SIM_AKA, networks is null!");
                        }
                    } else if (!WifiStateMachine.this.mSim1IccState.equals("LOADED") || !WifiStateMachine.this.mSim2IccState.equals("LOADED")) {
                        WifiStateMachine.this.log("iccState: (" + WifiStateMachine.this.mSim1IccState + "," + WifiStateMachine.this.mSim2IccState + "), check EAP SIM/AKA networks");
                        List<WifiConfiguration> networks2 = WifiStateMachine.this.mWifiConfigManager.getSavedNetworks();
                        if (networks2 != null) {
                            for (WifiConfiguration network2 : networks2) {
                                if (WifiStateMachine.this.mWifiConfigManager.isSimConfig(network2) && !WifiStateMachine.this.isConfigSimCardLoaded(network2)) {
                                    WifiStateMachine.this.log("diable EAP SIM/AKA network let supplicant cannot auto connect, netId: " + network2.networkId);
                                    WifiStateMachine.this.mWifiConfigManager.disableNetwork(network2.networkId);
                                    WifiConfiguration eapSimConfig2 = WifiStateMachine.this.mWifiConfigManager.getWifiConfiguration(network2.networkId);
                                    WifiStateMachine.this.mWifiConfigManager.updateNetworkSelectionStatus(eapSimConfig2, 10);
                                }
                            }
                        } else {
                            WifiStateMachine.this.log("Check for EAP_SIM_AKA, networks is null!");
                        }
                    }
                    WifiStateMachine.this.sendSupplicantConnectionChangedBroadcast(true);
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDriverStartedState);
                    return true;
                case WifiMonitor.SUP_DISCONNECTION_EVENT:
                    if (WifiStateMachine.this.mSupplicantRestartCount++ <= 5) {
                        WifiStateMachine.this.loge("Failed to setup control channel, restart supplicant");
                        WifiStateMachine.this.mWifiMonitor.killSupplicant(WifiStateMachine.this.mP2pSupported);
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mInitialState);
                        WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.CMD_START_SUPPLICANT, 5000L);
                    } else {
                        WifiStateMachine.this.loge("Failed " + WifiStateMachine.this.mSupplicantRestartCount + " times to start supplicant, unload driver");
                        WifiStateMachine.this.mSupplicantRestartCount = 0;
                        WifiStateMachine.this.setWifiState(4);
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mInitialState);
                    }
                    return true;
                default:
                    return false;
            }
        }
    }

    class SupplicantStartedState extends State {
        SupplicantStartedState() {
        }

        public void enter() {
            if (WifiStateMachine.DBG) {
                WifiStateMachine.this.loge(getName() + "\n");
            }
            WifiStateMachine.this.mNetworkInfo.setIsAvailable(true);
            if (WifiStateMachine.this.mNetworkAgent != null) {
                WifiStateMachine.this.mNetworkAgent.sendNetworkInfo(WifiStateMachine.this.mNetworkInfo);
            }
            int defaultInterval = WifiStateMachine.this.mContext.getResources().getInteger(R.integer.config_batterySaver_full_locationMode);
            if (WifiStateMachine.this.hasCustomizedAutoConnect()) {
                WifiStateMachine wifiStateMachine = WifiStateMachine.this;
                ContentResolver contentResolver = WifiStateMachine.this.mContext.getContentResolver();
                if (!WifiStateMachine.this.mScreenOn) {
                    defaultInterval = WifiStateMachine.this.mContext.getResources().getInteger(R.integer.config_bg_current_drain_location_min_duration);
                }
                wifiStateMachine.mSupplicantScanIntervalMs = Settings.Global.getLong(contentResolver, "wifi_supplicant_scan_interval_ms", defaultInterval);
            } else {
                WifiStateMachine.this.mSupplicantScanIntervalMs = WifiStateMachine.this.mFacade.getLongSetting(WifiStateMachine.this.mContext, "wifi_supplicant_scan_interval_ms", defaultInterval);
            }
            WifiStateMachine.this.mWifiNative.setScanInterval(((int) WifiStateMachine.this.mSupplicantScanIntervalMs) / 1000);
            WifiStateMachine.this.mWifiNative.setExternalSim(true);
            WifiStateMachine.this.mWifiNative.setDfsFlag(true);
            WifiStateMachine.this.setRandomMacOui();
            WifiStateMachine.this.mWifiNative.enableAutoConnect(false);
            WifiStateMachine.this.mCountryCode.setReadyForChange(true);
            if (WifiStateMachine.this.mWifiFwkExt == null || WifiStateMachine.this.mWifiFwkExt.hasNetworkSelection() == 0) {
                return;
            }
            WifiStateMachine.this.mWifiNative.disconnect();
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, this);
            switch (message.what) {
                case WifiStateMachine.CMD_STOP_SUPPLICANT:
                    if (WifiStateMachine.this.mP2pSupported) {
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mWaitForP2pDisableState);
                    } else {
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mSupplicantStoppingState);
                    }
                    return true;
                case WifiStateMachine.CMD_START_AP:
                    WifiStateMachine.this.loge("Failed to start soft AP with a running supplicant");
                    WifiStateMachine.this.setWifiApState(14, 0);
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
                    WifiStateMachine.this.replyToMessage(message, message.what, stats);
                    return true;
                case WifiStateMachine.CMD_SET_OPERATIONAL_MODE:
                    WifiStateMachine.this.mOperationalMode = message.arg1;
                    WifiStateMachine.this.mWifiConfigManager.setAndEnableLastSelectedConfiguration(-1);
                    return true;
                case WifiStateMachine.CMD_RESET_SIM_NETWORKS:
                    WifiStateMachine.this.log("resetting EAP-SIM/AKA/AKA' networks since SIM was removed, simSlot: " + message.arg1);
                    WifiStateMachine.this.mWifiConfigManager.resetSimNetworks(message.arg1);
                    return true;
                case WifiStateMachine.CMD_TARGET_BSSID:
                    if (message.obj != null) {
                        WifiStateMachine.this.mTargetRoamBSSID = (String) message.obj;
                    }
                    return true;
                case WifiStateMachine.M_CMD_UPDATE_SETTINGS:
                    WifiStateMachine.this.updateAutoConnectSettings();
                    return true;
                case WifiStateMachine.M_CMD_DO_CTIA_TEST_ON:
                    boolean ok2 = WifiStateMachine.this.mWifiNative.doCtiaTestOn();
                    WifiStateMachine.this.replyToMessage(message, message.what, ok2 ? 1 : -1);
                    return true;
                case WifiStateMachine.M_CMD_DO_CTIA_TEST_OFF:
                    boolean ok3 = WifiStateMachine.this.mWifiNative.doCtiaTestOff();
                    WifiStateMachine.this.replyToMessage(message, message.what, ok3 ? 1 : -1);
                    return true;
                case WifiStateMachine.M_CMD_DO_CTIA_TEST_RATE:
                    boolean ok4 = WifiStateMachine.this.mWifiNative.doCtiaTestRate(message.arg1);
                    WifiStateMachine.this.replyToMessage(message, message.what, ok4 ? 1 : -1);
                    return true;
                case WifiStateMachine.M_CMD_FLUSH_BSS:
                    WifiStateMachine.this.mWifiNative.bssFlush();
                    return true;
                case WifiStateMachine.M_CMD_GET_TEST_ENV:
                    String env = WifiStateMachine.this.mWifiNative.getTestEnv(message.arg1);
                    WifiStateMachine.this.replyToMessage(message, message.what, env);
                    return true;
                case WifiMonitor.SUP_DISCONNECTION_EVENT:
                    WifiStateMachine.this.loge("Connection lost, restart supplicant");
                    WifiStateMachine.this.handleSupplicantConnectionLoss(true);
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
                case WifiMonitor.SCAN_FAILED_EVENT:
                    WifiStateMachine.this.maybeRegisterNetworkFactory();
                    WifiStateMachine.this.setScanResults();
                    if (WifiStateMachine.this.hasCustomizedAutoConnect()) {
                        WifiStateMachine.this.mShowReselectDialog = false;
                        Log.d(WifiStateMachine.TAG, "SCAN_RESULTS_EVENT, mScanForWeakSignal:" + WifiStateMachine.this.mScanForWeakSignal);
                        if (WifiStateMachine.this.mScanForWeakSignal) {
                            WifiStateMachine.this.showReselectionDialog();
                        }
                        WifiStateMachine.this.mDisconnectNetworkId = -1;
                    }
                    WifiStateMachine.this.loge("mIsFullScanOngoing: " + WifiStateMachine.this.mIsFullScanOngoing + ", mSendScanResultsBroadcast: " + WifiStateMachine.this.mSendScanResultsBroadcast);
                    if (WifiStateMachine.this.mIsFullScanOngoing || WifiStateMachine.this.mSendScanResultsBroadcast || WifiStateMachine.this.mWifiOnScanCount < 2) {
                        WifiStateMachine.this.loge("mWifiOnScanCount: " + WifiStateMachine.this.mWifiOnScanCount);
                        boolean scanSucceeded = message.what == 147461;
                        WifiStateMachine.this.sendScanResultsAvailableBroadcast(scanSucceeded);
                    }
                    WifiStateMachine.this.mWifiOnScanCount++;
                    WifiStateMachine.this.mSendScanResultsBroadcast = false;
                    WifiStateMachine.this.mIsScanOngoing = false;
                    WifiStateMachine.this.mIsFullScanOngoing = false;
                    if (WifiStateMachine.this.mBufferedScanMsg.size() > 0) {
                        WifiStateMachine.this.sendMessage((Message) WifiStateMachine.this.mBufferedScanMsg.remove());
                    }
                    return true;
                case WifiMonitor.WHOLE_CHIP_RESET_FAIL_EVENT:
                    Log.e(WifiStateMachine.TAG, "Receive whole chip reset fail, disable wifi!");
                    WifiStateMachine.this.setWifiState(4);
                    return true;
                default:
                    return false;
            }
        }

        public void exit() {
            WifiStateMachine.this.mNetworkInfo.setIsAvailable(false);
            if (WifiStateMachine.this.mNetworkAgent != null) {
                WifiStateMachine.this.mNetworkAgent.sendNetworkInfo(WifiStateMachine.this.mNetworkInfo);
            }
            WifiStateMachine.this.mCountryCode.setReadyForChange(false);
        }
    }

    class SupplicantStoppingState extends State {
        SupplicantStoppingState() {
        }

        public void enter() {
            if (WifiStateMachine.DBG) {
                WifiStateMachine.this.loge(getName() + "\n");
            }
            WifiStateMachine.this.handleNetworkDisconnect();
            String suppState = System.getProperty("init.svc.wpa_supplicant");
            if (suppState == null) {
                suppState = "unknown";
            }
            String p2pSuppState = System.getProperty("init.svc.p2p_supplicant");
            if (p2pSuppState == null) {
                p2pSuppState = "unknown";
            }
            WifiStateMachine.this.logd("SupplicantStoppingState: stopSupplicant  init.svc.wpa_supplicant=" + suppState + " init.svc.p2p_supplicant=" + p2pSuppState);
            WifiStateMachine.this.mWifiMonitor.stopSupplicant();
            WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.this.obtainMessage(WifiStateMachine.CMD_STOP_SUPPLICANT_FAILED, WifiStateMachine.this.mSupplicantStopFailureToken++, 0), 5000L);
            WifiStateMachine.this.setWifiState(0);
            WifiStateMachine.this.mSupplicantStateTracker.sendMessage(WifiStateMachine.CMD_RESET_SUPPLICANT_STATE);
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, this);
            switch (message.what) {
                case WifiStateMachine.CMD_START_SUPPLICANT:
                case WifiStateMachine.CMD_STOP_SUPPLICANT:
                case WifiStateMachine.CMD_START_DRIVER:
                case WifiStateMachine.CMD_STOP_DRIVER:
                case WifiStateMachine.CMD_START_AP:
                case WifiStateMachine.CMD_STOP_AP:
                case WifiStateMachine.CMD_SET_OPERATIONAL_MODE:
                case WifiStateMachine.CMD_SET_FREQUENCY_BAND:
                    WifiStateMachine.this.deferMessage(message);
                    return true;
                case WifiStateMachine.CMD_STOP_SUPPLICANT_FAILED:
                    if (message.arg1 == WifiStateMachine.this.mSupplicantStopFailureToken) {
                        WifiStateMachine.this.loge("Timed out on a supplicant stop, kill and proceed");
                        WifiStateMachine.this.handleSupplicantConnectionLoss(true);
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
                    WifiStateMachine.this.handleSupplicantConnectionLoss(false);
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mInitialState);
                    return true;
                default:
                    return false;
            }
        }
    }

    class DriverStartingState extends State {
        private int mTries;

        DriverStartingState() {
        }

        public void enter() {
            if (WifiStateMachine.DBG) {
                WifiStateMachine.this.loge(getName() + "\n");
            }
            this.mTries = 1;
            WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.this.obtainMessage(WifiStateMachine.CMD_DRIVER_START_TIMED_OUT, WifiStateMachine.this.mDriverStartToken++, 0), 10000L);
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, this);
            switch (message.what) {
                case WifiStateMachine.CMD_START_DRIVER:
                case WifiStateMachine.CMD_STOP_DRIVER:
                case WifiStateMachine.CMD_START_SCAN:
                case WifiStateMachine.CMD_DISCONNECT:
                case WifiStateMachine.CMD_RECONNECT:
                case WifiStateMachine.CMD_REASSOCIATE:
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
                            WifiStateMachine.this.setSupplicantRunning(false);
                            WifiStateMachine.this.setSupplicantRunning(true);
                        } else {
                            WifiStateMachine.this.loge("Driver start failed, retrying");
                            WifiStateMachine.this.mWakeLock.acquire();
                            WifiStateMachine.this.mWifiNative.startDriver();
                            WifiStateMachine.this.mWakeLock.release();
                            this.mTries++;
                            WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.this.obtainMessage(WifiStateMachine.CMD_DRIVER_START_TIMED_OUT, WifiStateMachine.this.mDriverStartToken++, 0), 10000L);
                        }
                    }
                    return true;
                case WifiMonitor.SCAN_RESULTS_EVENT:
                case WifiMonitor.SCAN_FAILED_EVENT:
                    return true;
                case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT:
                    SupplicantState state = WifiStateMachine.this.handleSupplicantStateChange(message);
                    if (SupplicantState.isDriverActive(state)) {
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDriverStartedState);
                    }
                    return true;
                default:
                    return false;
            }
        }
    }

    class DriverStartedState extends State {
        DriverStartedState() {
        }

        public void enter() {
            boolean z;
            if (WifiStateMachine.DBG) {
                WifiStateMachine.this.logd("DriverStartedState enter");
            }
            if (WifiStateMachine.this.mWifiScanner == null) {
                WifiStateMachine.this.mWifiScanner = WifiStateMachine.this.mFacade.makeWifiScanner(WifiStateMachine.this.mContext, WifiStateMachine.this.getHandler().getLooper());
                WifiStateMachine.this.mWifiConnectivityManager = new WifiConnectivityManager(WifiStateMachine.this.mContext, WifiStateMachine.this, WifiStateMachine.this.mWifiScanner, WifiStateMachine.this.mWifiConfigManager, WifiStateMachine.this.mWifiInfo, WifiStateMachine.this.mWifiQualifiedNetworkSelector, WifiStateMachine.this.mWifiInjector, WifiStateMachine.this.getHandler().getLooper());
            }
            WifiStateMachine.this.mWifiLogger.startLogging(WifiStateMachine.DBG);
            WifiStateMachine.this.mIsRunning = true;
            WifiStateMachine.this.updateBatteryWorkSource(null);
            WifiStateMachine.this.mWifiNative.setBluetoothCoexistenceScanMode(WifiStateMachine.this.mBluetoothConnectionActive);
            WifiStateMachine.this.setNetworkDetailedState(NetworkInfo.DetailedState.DISCONNECTED);
            WifiStateMachine.this.mWifiNative.stopFilteringMulticastV4Packets();
            WifiStateMachine.this.mWifiNative.stopFilteringMulticastV6Packets();
            if (WifiStateMachine.this.mOperationalMode != 1) {
                WifiStateMachine.this.mWifiNative.disconnect();
                WifiStateMachine.this.mWifiConfigManager.disableAllNetworksNative();
                if (WifiStateMachine.this.mOperationalMode == 3) {
                    WifiStateMachine.this.setWifiState(1);
                }
                WifiStateMachine.this.transitionTo(WifiStateMachine.this.mScanModeState);
            } else {
                WifiStateMachine.this.mWifiNative.status();
                WifiStateMachine.this.mWifiOnScanCount = 0;
                WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectedState);
            }
            if (!WifiStateMachine.this.mScreenBroadcastReceived.get()) {
                PowerManager powerManager = (PowerManager) WifiStateMachine.this.mContext.getSystemService("power");
                WifiStateMachine.this.handleScreenStateChanged(powerManager.isScreenOn());
            } else {
                WifiNative wifiNative = WifiStateMachine.this.mWifiNative;
                if (WifiStateMachine.this.mSuspendOptNeedsDisabled != 0) {
                    z = false;
                } else {
                    z = WifiStateMachine.this.mUserWantsSuspendOpt.get();
                }
                wifiNative.setSuspendOptimizations(z);
                WifiStateMachine.this.mWifiConnectivityManager.handleScreenStateChanged(WifiStateMachine.this.mScreenOn);
            }
            WifiStateMachine.this.mWifiNative.setPowerSave(true);
            if (WifiStateMachine.this.mP2pSupported && WifiStateMachine.this.mOperationalMode == 1) {
                WifiStateMachine.this.mWifiP2pChannel.sendMessage(WifiStateMachine.CMD_ENABLE_P2P);
            }
            Intent intent = new Intent("wifi_scan_available");
            intent.addFlags(67108864);
            intent.putExtra("scan_enabled", 3);
            WifiStateMachine.this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
            WifiStateMachine.this.mWifiNative.setWifiLinkLayerStats("wlan0", 1);
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, this);
            switch (message.what) {
                case WifiStateMachine.CMD_START_DRIVER:
                    if (WifiStateMachine.this.mOperationalMode == 1) {
                        WifiStateMachine.this.mWifiConfigManager.enableAllNetworks();
                    }
                    return true;
                case WifiStateMachine.CMD_STOP_DRIVER:
                    int i = message.arg1;
                    WifiStateMachine.this.log("stop driver");
                    WifiStateMachine.this.mWifiConfigManager.disableAllNetworksNative();
                    if (WifiStateMachine.this.hasCustomizedAutoConnect()) {
                        WifiStateMachine.this.mDisconnectOperation = true;
                    }
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
                    return true;
                case WifiStateMachine.CMD_BLUETOOTH_ADAPTER_STATE_CHANGE:
                    WifiStateMachine.this.mBluetoothConnectionActive = message.arg1 != 0;
                    WifiStateMachine.this.mWifiNative.setBluetoothCoexistenceScanMode(WifiStateMachine.this.mBluetoothConnectionActive);
                    return true;
                case WifiStateMachine.CMD_START_SCAN:
                    WifiStateMachine.this.handleScanRequest(message);
                    return true;
                case WifiStateMachine.CMD_SET_HIGH_PERF_MODE:
                    if (message.arg1 == 1) {
                        WifiStateMachine.this.setSuspendOptimizationsNative(2, false);
                    } else {
                        WifiStateMachine.this.setSuspendOptimizationsNative(2, true);
                    }
                    return true;
                case WifiStateMachine.CMD_SET_SUSPEND_OPT_ENABLED:
                    if (message.arg1 == 1) {
                        WifiStateMachine.this.setSuspendOptimizationsNative(4, true);
                        WifiStateMachine.this.mSuspendWakeLock.release();
                    } else {
                        WifiStateMachine.this.setSuspendOptimizationsNative(4, false);
                    }
                    return true;
                case WifiStateMachine.CMD_SET_FREQUENCY_BAND:
                    int band = message.arg1;
                    if (WifiStateMachine.DBG) {
                        WifiStateMachine.this.log("set frequency band " + band);
                    }
                    if (WifiStateMachine.this.mWifiNative.setBand(band)) {
                        if (WifiStateMachine.DBG) {
                            WifiStateMachine.this.logd("did set frequency band " + band);
                        }
                        WifiStateMachine.this.mFrequencyBand.set(band);
                        WifiStateMachine.this.mWifiNative.bssFlush();
                        if (WifiStateMachine.DBG) {
                            WifiStateMachine.this.logd("done set frequency band " + band);
                        }
                    } else {
                        WifiStateMachine.this.loge("Failed to set frequency band " + band);
                    }
                    return true;
                case WifiStateMachine.CMD_ENABLE_TDLS:
                    if (message.obj != null) {
                        String remoteAddress = (String) message.obj;
                        boolean enable = message.arg1 == 1;
                        WifiStateMachine.this.mWifiNative.startTdls(remoteAddress, enable);
                    }
                    return true;
                case WifiStateMachine.CMD_STOP_IP_PACKET_OFFLOAD:
                    int slot = message.arg1;
                    int ret = WifiStateMachine.this.stopWifiIPPacketOffload(slot);
                    if (WifiStateMachine.this.mNetworkAgent != null) {
                        WifiStateMachine.this.mNetworkAgent.onPacketKeepaliveEvent(slot, ret);
                    }
                    return true;
                case WifiStateMachine.CMD_ENABLE_WIFI_CONNECTIVITY_MANAGER:
                    if (WifiStateMachine.this.mWifiConnectivityManager != null) {
                        WifiStateMachine.this.mWifiConnectivityManager.enable(message.arg1 == 1);
                    }
                    return true;
                case WifiStateMachine.CMD_ENABLE_AUTOJOIN_WHEN_ASSOCIATED:
                    boolean allowed = message.arg1 > 0;
                    boolean old_state = WifiStateMachine.this.mWifiConfigManager.getEnableAutoJoinWhenAssociated();
                    WifiStateMachine.this.mWifiConfigManager.setEnableAutoJoinWhenAssociated(allowed);
                    if (!old_state && allowed && WifiStateMachine.this.mScreenOn && WifiStateMachine.this.getCurrentState() == WifiStateMachine.this.mConnectedState && WifiStateMachine.this.mWifiConnectivityManager != null) {
                        WifiStateMachine.this.mWifiConnectivityManager.forceConnectivityScan();
                    }
                    return true;
                case WifiStateMachine.CMD_CONFIG_ND_OFFLOAD:
                    boolean enabled = message.arg1 > 0;
                    WifiStateMachine.this.mWifiNative.configureNeighborDiscoveryOffload(enabled);
                    return true;
                case WifiStateMachine.M_CMD_SET_POWER_SAVING_MODE:
                    WifiStateMachine.this.mWifiNative.setPowerSave(message.arg1 == 1);
                    return true;
                case WifiStateMachine.M_CMD_SET_TDLS_POWER_SAVE:
                    WifiStateMachine.this.mWifiNative.setTdlsPowerSave(message.arg1 == 1);
                    return true;
                case WifiMonitor.ANQP_DONE_EVENT:
                    WifiStateMachine.this.mWifiConfigManager.notifyANQPDone((Long) message.obj, message.arg1 != 0);
                    return true;
                case WifiMonitor.RX_HS20_ANQP_ICON_EVENT:
                    WifiStateMachine.this.mWifiConfigManager.notifyIconReceived((IconEvent) message.obj);
                    return true;
                case WifiMonitor.HS20_REMEDIATION_EVENT:
                    WifiStateMachine.this.wnmFrameReceived((WnmData) message.obj);
                    return true;
                default:
                    return false;
            }
        }

        public void exit() {
            if (WifiStateMachine.DBG) {
                WifiStateMachine.this.loge(getName() + " exit\n");
            }
            WifiStateMachine.this.mWifiLogger.stopLogging();
            WifiStateMachine.this.mIsRunning = false;
            WifiStateMachine.this.updateBatteryWorkSource(null);
            WifiStateMachine.this.mScanResults = new ArrayList();
            Intent intent = new Intent("wifi_scan_available");
            intent.addFlags(67108864);
            intent.putExtra("scan_enabled", 1);
            WifiStateMachine.this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
            WifiStateMachine.this.mBufferedScanMsg.clear();
        }
    }

    class WaitForP2pDisableState extends State {
        private State mTransitionToState;

        WaitForP2pDisableState() {
        }

        public void enter() {
            if (WifiStateMachine.DBG) {
                WifiStateMachine.this.loge(getName() + "\n");
            }
            switch (WifiStateMachine.this.getCurrentMessage().what) {
                case WifiStateMachine.CMD_STOP_SUPPLICANT:
                    this.mTransitionToState = WifiStateMachine.this.mSupplicantStoppingState;
                    break;
                case WifiStateMachine.CMD_STOP_DRIVER:
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
            WifiStateMachine.this.logStateAndMessage(message, this);
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
                case WifiStateMachine.CMD_SET_FREQUENCY_BAND:
                case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT:
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DEFERRED;
                    WifiStateMachine.this.deferMessage(message);
                    return true;
                case WifiStateMachine.CMD_DISABLE_P2P_RSP:
                    WifiStateMachine.this.transitionTo(this.mTransitionToState);
                    return true;
                default:
                    return false;
            }
        }
    }

    class DriverStoppingState extends State {
        DriverStoppingState() {
        }

        public void enter() {
            if (WifiStateMachine.DBG) {
                WifiStateMachine.this.loge(getName() + "\n");
            }
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, this);
            switch (message.what) {
                case WifiStateMachine.CMD_START_DRIVER:
                case WifiStateMachine.CMD_STOP_DRIVER:
                case WifiStateMachine.CMD_START_SCAN:
                case WifiStateMachine.CMD_DISCONNECT:
                case WifiStateMachine.CMD_RECONNECT:
                case WifiStateMachine.CMD_REASSOCIATE:
                case WifiStateMachine.CMD_SET_FREQUENCY_BAND:
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DEFERRED;
                    WifiStateMachine.this.deferMessage(message);
                    return true;
                case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT:
                    SupplicantState state = WifiStateMachine.this.handleSupplicantStateChange(message);
                    if (state == SupplicantState.INTERFACE_DISABLED) {
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDriverStoppedState);
                        return true;
                    }
                    return true;
                default:
                    return false;
            }
        }
    }

    class DriverStoppedState extends State {
        DriverStoppedState() {
        }

        public void enter() {
            if (WifiStateMachine.DBG) {
                WifiStateMachine.this.loge(getName() + "\n");
            }
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, this);
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
                        return true;
                    }
                    return true;
                default:
                    return false;
            }
        }
    }

    class ScanModeState extends State {
        private int mLastOperationMode;

        ScanModeState() {
        }

        public void enter() {
            if (WifiStateMachine.DBG) {
                WifiStateMachine.this.loge(getName() + "\n");
            }
            this.mLastOperationMode = WifiStateMachine.this.mOperationalMode;
            WifiStateMachine.this.lastConnectAttemptTimestamp = 0L;
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, this);
            switch (message.what) {
                case WifiStateMachine.CMD_START_SCAN:
                    WifiStateMachine.this.handleScanRequest(message);
                    return true;
                case WifiStateMachine.CMD_SET_OPERATIONAL_MODE:
                    if (message.arg1 != 1) {
                        return true;
                    }
                    if (this.mLastOperationMode == 3) {
                        WifiStateMachine.this.setWifiState(3);
                        WifiStateMachine.this.mWifiConfigManager.loadAndEnableAllNetworks();
                        WifiStateMachine.this.mWifiP2pChannel.sendMessage(WifiStateMachine.CMD_ENABLE_P2P);
                    } else {
                        WifiStateMachine.this.mWifiConfigManager.enableAllNetworks();
                    }
                    WifiStateMachine.this.mWifiConfigManager.setAndEnableLastSelectedConfiguration(-1);
                    WifiStateMachine.this.mOperationalMode = 1;
                    WifiStateMachine.this.mWifiOnScanCount = 0;
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectedState);
                    return true;
                case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT:
                    SupplicantState state = WifiStateMachine.this.handleSupplicantStateChange(message);
                    if (WifiStateMachine.DBG) {
                        WifiStateMachine.this.log("SupplicantState= " + state);
                    }
                    return true;
                default:
                    return false;
            }
        }
    }

    String smToString(Message message) {
        return smToString(message.what);
    }

    String smToString(int what) {
        String s = sSmToString.get(what);
        if (s != null) {
            return s;
        }
        switch (what) {
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
            case WifiMonitor.SCAN_FAILED_EVENT:
                return "SCAN_FAILED_EVENT";
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
            case WifiMonitor.WAPI_NO_CERTIFICATION_EVENT:
                return "WAPI_NO_CERTIFICATION_EVENT";
            case WifiMonitor.TDLS_CONNECTED_EVENT:
                return "TDLS_CONNECTED_EVENT";
            case WifiMonitor.TDLS_DISCONNECTED_EVENT:
                return "TDLS_DISCONNECTED_EVENT";
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
            default:
                return "what:" + Integer.toString(what);
        }
    }

    void registerConnected() {
        if (this.mLastNetworkId == -1) {
            return;
        }
        WifiConfiguration config = this.mWifiConfigManager.getWifiConfiguration(this.mLastNetworkId);
        if (config != null) {
            config.lastConnected = System.currentTimeMillis();
            config.numAssociation++;
            WifiConfiguration.NetworkSelectionStatus networkSelectionStatus = config.getNetworkSelectionStatus();
            networkSelectionStatus.clearDisableReasonCounter();
            networkSelectionStatus.setHasEverConnected(true);
        }
        this.mWifiScoreReport = null;
    }

    void registerDisconnected() {
        WifiConfiguration config;
        if (this.mLastNetworkId == -1 || (config = this.mWifiConfigManager.getWifiConfiguration(this.mLastNetworkId)) == null) {
            return;
        }
        config.lastDisconnected = System.currentTimeMillis();
        if (!config.ephemeral) {
            return;
        }
        this.mWifiConfigManager.forgetNetwork(this.mLastNetworkId);
    }

    void noteWifiDisabledWhileAssociated() {
        boolean isBadRSSI;
        boolean isLowRSSI;
        boolean isHighRSSI;
        int rssi = this.mWifiInfo.getRssi();
        WifiConfiguration config = getCurrentWifiConfiguration();
        if (getCurrentState() != this.mConnectedState || rssi == -127 || config == null) {
            return;
        }
        boolean is24GHz = this.mWifiInfo.is24GHz();
        if (!is24GHz || rssi >= this.mWifiConfigManager.mThresholdMinimumRssi24.get()) {
            isBadRSSI = !is24GHz && rssi < this.mWifiConfigManager.mThresholdMinimumRssi5.get();
        } else {
            isBadRSSI = true;
        }
        if (!is24GHz || rssi >= this.mWifiConfigManager.mThresholdQualifiedRssi24.get()) {
            isLowRSSI = !is24GHz && this.mWifiInfo.getRssi() < this.mWifiConfigManager.mThresholdQualifiedRssi5.get();
        } else {
            isLowRSSI = true;
        }
        if (!is24GHz || rssi < this.mWifiConfigManager.mThresholdSaturatedRssi24.get()) {
            isHighRSSI = !is24GHz && this.mWifiInfo.getRssi() >= this.mWifiConfigManager.mThresholdSaturatedRssi5.get();
        } else {
            isHighRSSI = true;
        }
        if (isBadRSSI) {
            config.numUserTriggeredWifiDisableLowRSSI++;
        } else if (isLowRSSI) {
            config.numUserTriggeredWifiDisableBadRSSI++;
        } else {
            if (isHighRSSI) {
                return;
            }
            config.numUserTriggeredWifiDisableNotHighRSSI++;
        }
    }

    public WifiConfiguration getCurrentWifiConfiguration() {
        if (this.mLastNetworkId == -1) {
            return null;
        }
        return this.mWifiConfigManager.getWifiConfiguration(this.mLastNetworkId);
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
        ScanDetailCache scanDetailCache = this.mWifiConfigManager.getScanDetailCache(config);
        if (scanDetailCache == null) {
            return null;
        }
        return scanDetailCache.get(BSSID);
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
                WifiStateMachine.this.loge(getName() + "\n");
            }
            if (WifiStateMachine.this.mWifiConnectivityManager != null) {
                WifiStateMachine.this.mWifiConnectivityManager.setWifiEnabled(true);
            }
            WifiStateMachine.this.mWifiMetrics.setWifiState(2);
        }

        public void exit() {
            if (WifiStateMachine.this.mWifiConnectivityManager != null) {
                WifiStateMachine.this.mWifiConnectivityManager.setWifiEnabled(false);
            }
            WifiStateMachine.this.mWifiMetrics.setWifiState(1);
        }

        public boolean processMessage(Message message) {
            int res;
            WpsResult wpsResult;
            TelephonyManager tm;
            String mccMnc;
            String imsi;
            WifiStateMachine.this.logStateAndMessage(message, this);
            switch (message.what) {
                case WifiStateMachine.CMD_ADD_OR_UPDATE_NETWORK:
                    if (!WifiStateMachine.this.mWifiConfigManager.isCurrentUserProfile(UserHandle.getUserId(message.sendingUid))) {
                        WifiStateMachine.this.loge("Only the current foreground user can modify networks  currentUserId=" + WifiStateMachine.this.mWifiConfigManager.getCurrentUserId() + " sendingUserId=" + UserHandle.getUserId(message.sendingUid));
                        WifiStateMachine.this.replyToMessage(message, message.what, -1);
                        return true;
                    }
                    WifiConfiguration config = (WifiConfiguration) message.obj;
                    if (!WifiStateMachine.this.recordUidIfAuthorized(config, message.sendingUid, false)) {
                        WifiStateMachine.this.logw("Not authorized to update network  config=" + config.SSID + " cnid=" + config.networkId + " uid=" + message.sendingUid);
                        WifiStateMachine.this.replyToMessage(message, message.what, -1);
                        return true;
                    }
                    WifiStateMachine.this.checkGbkEncoding(config);
                    int res2 = WifiStateMachine.this.mWifiConfigManager.addOrUpdateNetwork(config, message.sendingUid);
                    if (res2 < 0) {
                        WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_FAIL;
                    } else {
                        WifiConfiguration curConfig = WifiStateMachine.this.getCurrentWifiConfiguration();
                        if (curConfig != null && config != null) {
                            WifiConfiguration.NetworkSelectionStatus networkStatus = config.getNetworkSelectionStatus();
                            if (curConfig.priority < config.priority && networkStatus != null && !networkStatus.isNetworkPermanentlyDisabled()) {
                                WifiStateMachine.this.mWifiConfigManager.setAndEnableLastSelectedConfiguration(res2);
                                WifiStateMachine.this.mWifiConfigManager.updateLastConnectUid(config, message.sendingUid);
                                boolean persist = WifiStateMachine.this.mWifiConfigManager.checkConfigOverridePermission(message.sendingUid);
                                if (WifiStateMachine.this.mWifiConnectivityManager != null) {
                                    WifiStateMachine.this.mWifiConnectivityManager.connectToUserSelectNetwork(res2, persist);
                                }
                                WifiStateMachine.this.lastConnectAttemptTimestamp = System.currentTimeMillis();
                                WifiStateMachine.this.mWifiConnectionStatistics.numWifiManagerJoinAttempt++;
                                WifiStateMachine.this.startScan(WifiStateMachine.ADD_OR_UPDATE_SOURCE, 0, null, WifiStateMachine.WIFI_WORK_SOURCE);
                            }
                        }
                        if (WifiStateMachine.this.hasCustomizedAutoConnect()) {
                            WifiStateMachine.this.checkIfEapNetworkChanged(config);
                        }
                    }
                    WifiStateMachine.this.replyToMessage(message, WifiStateMachine.CMD_ADD_OR_UPDATE_NETWORK, res2);
                    return true;
                case WifiStateMachine.CMD_REMOVE_NETWORK:
                    if (!WifiStateMachine.this.mWifiConfigManager.isCurrentUserProfile(UserHandle.getUserId(message.sendingUid))) {
                        WifiStateMachine.this.loge("Only the current foreground user can modify networks  currentUserId=" + WifiStateMachine.this.mWifiConfigManager.getCurrentUserId() + " sendingUserId=" + UserHandle.getUserId(message.sendingUid));
                        WifiStateMachine.this.replyToMessage(message, message.what, -1);
                        return true;
                    }
                    int netId = message.arg1;
                    if (!WifiStateMachine.this.mWifiConfigManager.canModifyNetwork(message.sendingUid, netId, false)) {
                        WifiStateMachine.this.logw("Not authorized to remove network  cnid=" + netId + " uid=" + message.sendingUid);
                        WifiStateMachine.this.replyToMessage(message, message.what, -1);
                        return true;
                    }
                    boolean ok = WifiStateMachine.this.mWifiConfigManager.removeNetwork(message.arg1);
                    if (!ok) {
                        WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_FAIL;
                    }
                    if (WifiStateMachine.this.hasCustomizedAutoConnect()) {
                        WifiStateMachine.this.mWifiConfigManager.removeDisconnectNetwork(message.arg1);
                        if (ok && message.arg1 == WifiStateMachine.this.mWifiInfo.getNetworkId()) {
                            WifiStateMachine.this.mDisconnectOperation = true;
                            WifiStateMachine.this.mScanForWeakSignal = false;
                        }
                    }
                    WifiStateMachine.this.replyToMessage(message, message.what, ok ? 1 : -1);
                    return true;
                case WifiStateMachine.CMD_ENABLE_NETWORK:
                    if (WifiStateMachine.this.hasCustomizedAutoConnect() && message.arg2 == 0) {
                        if (!WifiStateMachine.this.mWifiFwkExt.shouldAutoConnect()) {
                            Log.d(WifiStateMachine.TAG, "Shouldn't auto connect, ignore the enable network operation!");
                            WifiStateMachine.this.replyToMessage(message, message.what, 1);
                            return true;
                        }
                        List<Integer> disconnectNetworks = WifiStateMachine.this.mWifiConfigManager.getDisconnectNetworks();
                        if (disconnectNetworks.contains(Integer.valueOf(message.arg1))) {
                            Log.d(WifiStateMachine.TAG, "Network " + message.arg1 + " is disconnected actively,ignore the enable network operation!");
                            WifiStateMachine.this.replyToMessage(message, message.what, 1);
                            return true;
                        }
                    }
                    if (!WifiStateMachine.this.mWifiConfigManager.isCurrentUserProfile(UserHandle.getUserId(message.sendingUid))) {
                        WifiStateMachine.this.loge("Only the current foreground user can modify networks  currentUserId=" + WifiStateMachine.this.mWifiConfigManager.getCurrentUserId() + " sendingUserId=" + UserHandle.getUserId(message.sendingUid));
                        WifiStateMachine.this.replyToMessage(message, message.what, -1);
                        return true;
                    }
                    boolean disableOthers = message.arg2 == 1;
                    int netId2 = message.arg1;
                    WifiConfiguration config2 = WifiStateMachine.this.mWifiConfigManager.getWifiConfiguration(netId2);
                    if (config2 == null) {
                        WifiStateMachine.this.loge("No network with id = " + netId2);
                        WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_FAIL;
                        WifiStateMachine.this.replyToMessage(message, message.what, -1);
                        return true;
                    }
                    if (disableOthers) {
                        WifiStateMachine.this.lastConnectAttemptTimestamp = System.currentTimeMillis();
                        WifiStateMachine.this.mWifiConnectionStatistics.numWifiManagerJoinAttempt++;
                    }
                    WifiStateMachine.this.autoRoamSetBSSID(netId2, WifiLastResortWatchdog.BSSID_ANY);
                    boolean ok2 = WifiStateMachine.this.mWifiConfigManager.enableNetwork(config2, disableOthers, message.sendingUid);
                    if (!ok2) {
                        WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_FAIL;
                    } else if (disableOthers) {
                        if (WifiStateMachine.this.hasCustomizedAutoConnect()) {
                            WifiStateMachine.this.mWifiConfigManager.removeDisconnectNetwork(message.arg1);
                            WifiStateMachine.this.mDisconnectOperation = true;
                            WifiStateMachine.this.mScanForWeakSignal = false;
                        }
                        WifiStateMachine.this.mLastExplicitNetworkId = message.arg1;
                        WifiStateMachine.this.mConnectNetwork = true;
                        WifiStateMachine.this.mSupplicantStateTracker.sendMessage(151553, message.arg1);
                        WifiStateMachine.this.mTargetNetworkId = netId2;
                    }
                    WifiStateMachine.this.replyToMessage(message, message.what, ok2 ? 1 : -1);
                    return true;
                case WifiStateMachine.CMD_ENABLE_ALL_NETWORKS:
                    long time = SystemClock.elapsedRealtime();
                    if (time - WifiStateMachine.this.mLastEnableAllNetworksTime <= 600000) {
                        return true;
                    }
                    WifiStateMachine.this.mWifiConfigManager.enableAllNetworks();
                    WifiStateMachine.this.mLastEnableAllNetworksTime = time;
                    return true;
                case WifiStateMachine.CMD_BLACKLIST_NETWORK:
                    WifiStateMachine.this.mWifiConfigManager.blackListBssid((String) message.obj);
                    return true;
                case WifiStateMachine.CMD_CLEAR_BLACKLIST:
                    WifiStateMachine.this.mWifiConfigManager.clearBssidBlacklist();
                    return true;
                case WifiStateMachine.CMD_SAVE_CONFIG:
                    boolean ok3 = WifiStateMachine.this.mWifiConfigManager.saveConfig();
                    if (WifiStateMachine.DBG) {
                        WifiStateMachine.this.logd("did save config " + ok3);
                    }
                    WifiStateMachine.this.replyToMessage(message, WifiStateMachine.CMD_SAVE_CONFIG, ok3 ? 1 : -1);
                    WifiStateMachine.this.mBackupManagerProxy.notifyDataChanged();
                    return true;
                case WifiStateMachine.CMD_GET_CONFIGURED_NETWORKS:
                    WifiStateMachine.this.replyToMessage(message, message.what, WifiStateMachine.this.mWifiConfigManager.getSavedNetworks());
                    return true;
                case WifiStateMachine.CMD_GET_PRIVILEGED_CONFIGURED_NETWORKS:
                    WifiStateMachine.this.replyToMessage(message, message.what, WifiStateMachine.this.mWifiConfigManager.getPrivilegedSavedNetworks());
                    return true;
                case WifiStateMachine.CMD_DISCONNECT:
                    WifiStateMachine.this.mWifiConfigManager.setAndEnableLastSelectedConfiguration(-1);
                    WifiStateMachine.this.mWifiNative.disconnect();
                    return true;
                case WifiStateMachine.CMD_RECONNECT:
                    if (WifiStateMachine.this.mWifiConnectivityManager == null) {
                        return true;
                    }
                    WifiStateMachine.this.mWifiConnectivityManager.forceConnectivityScan();
                    return true;
                case WifiStateMachine.CMD_REASSOCIATE:
                    WifiStateMachine.this.lastConnectAttemptTimestamp = System.currentTimeMillis();
                    WifiStateMachine.this.mWifiNative.reassociate();
                    return true;
                case WifiStateMachine.CMD_REMOVE_APP_CONFIGURATIONS:
                    WifiStateMachine.this.mWifiConfigManager.removeNetworksForApp((ApplicationInfo) message.obj);
                    return true;
                case WifiStateMachine.CMD_DISABLE_EPHEMERAL_NETWORK:
                    WifiConfiguration config3 = WifiStateMachine.this.mWifiConfigManager.disableEphemeralNetwork((String) message.obj);
                    if (config3 == null || config3.networkId != WifiStateMachine.this.mLastNetworkId) {
                        return true;
                    }
                    WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_DISCONNECT);
                    return true;
                case WifiStateMachine.CMD_GET_MATCHING_CONFIG:
                    WifiStateMachine.this.replyToMessage(message, message.what, WifiStateMachine.this.mWifiConfigManager.getMatchingConfig((ScanResult) message.obj));
                    return true;
                case WifiStateMachine.CMD_ADD_PASSPOINT_MO:
                    int res3 = WifiStateMachine.this.mWifiConfigManager.addPasspointManagementObject((String) message.obj);
                    WifiStateMachine.this.replyToMessage(message, message.what, res3);
                    return true;
                case WifiStateMachine.CMD_MODIFY_PASSPOINT_MO:
                    if (message.obj != null) {
                        Bundle bundle = (Bundle) message.obj;
                        ArrayList<PasspointManagementObjectDefinition> mos = bundle.getParcelableArrayList("MOS");
                        res = WifiStateMachine.this.mWifiConfigManager.modifyPasspointMo(bundle.getString(PasspointManagementObjectManager.TAG_FQDN), mos);
                    } else {
                        res = 0;
                    }
                    WifiStateMachine.this.replyToMessage(message, message.what, res);
                    return true;
                case WifiStateMachine.CMD_QUERY_OSU_ICON:
                    int res4 = WifiStateMachine.this.mWifiConfigManager.queryPasspointIcon(((Bundle) message.obj).getLong("BSSID"), ((Bundle) message.obj).getString("FILENAME")) ? 1 : 0;
                    WifiStateMachine.this.replyToMessage(message, message.what, res4);
                    return true;
                case WifiStateMachine.CMD_MATCH_PROVIDER_NETWORK:
                    int res5 = WifiStateMachine.this.mWifiConfigManager.matchProviderWithCurrentNetwork((String) message.obj);
                    WifiStateMachine.this.replyToMessage(message, message.what, res5);
                    return true;
                case WifiStateMachine.CMD_RELOAD_TLS_AND_RECONNECT:
                    if (!WifiStateMachine.this.mWifiConfigManager.needsUnlockedKeyStore()) {
                        return true;
                    }
                    WifiStateMachine.this.logd("Reconnecting to give a chance to un-connected TLS networks");
                    WifiStateMachine.this.mWifiNative.disconnect();
                    if (WifiStateMachine.this.hasCustomizedAutoConnect()) {
                        WifiStateMachine.this.mDisconnectOperation = true;
                    }
                    WifiStateMachine.this.lastConnectAttemptTimestamp = System.currentTimeMillis();
                    WifiStateMachine.this.mWifiNative.reconnect();
                    return true;
                case WifiStateMachine.CMD_AUTO_CONNECT:
                    if (WifiStateMachine.this.hasCustomizedAutoConnect() && !WifiStateMachine.this.mWifiFwkExt.shouldAutoConnect()) {
                        Log.d(WifiStateMachine.TAG, "Skip CMD_AUTO_CONNECT for customization!");
                        return true;
                    }
                    boolean didDisconnect = false;
                    if (WifiStateMachine.this.getCurrentState() != WifiStateMachine.this.mDisconnectedState) {
                        didDisconnect = true;
                        WifiStateMachine.this.mWifiNative.disconnect();
                    }
                    int netId3 = message.arg1;
                    WifiStateMachine.this.mTargetNetworkId = netId3;
                    WifiStateMachine.this.mTargetRoamBSSID = (String) message.obj;
                    WifiConfiguration config4 = WifiStateMachine.this.mWifiConfigManager.getWifiConfiguration(netId3);
                    WifiStateMachine.this.logd("CMD_AUTO_CONNECT sup state " + WifiStateMachine.this.mSupplicantStateTracker.getSupplicantStateName() + " my state " + WifiStateMachine.this.getCurrentState().getName() + " nid=" + Integer.toString(netId3) + " roam=" + Boolean.toString(WifiStateMachine.this.mAutoRoaming));
                    if (config4 == null) {
                        WifiStateMachine.this.loge("AUTO_CONNECT and no config, bail out...");
                        return true;
                    }
                    if (WifiStateMachine.this.mWifiConfigManager.isSimConfig(config4)) {
                        if (!WifiStateMachine.this.isConfigSimCardLoaded(config4)) {
                            if (!WifiStateMachine.this.isConfigSimCardAbsent(config4)) {
                                WifiStateMachine.this.loge("AUTO_CONNECT EAP-SIM AP, but modem is not ready, drop this connect");
                                return true;
                            }
                            WifiStateMachine.this.loge("AUTO_CONNECT EAP-SIM AP, iccState: (" + WifiStateMachine.this.mSim1IccState + ", " + WifiStateMachine.this.mSim2IccState + "), set networkStatus to DISABLED_AUTHENTICATION_SIM_CARD_ABSENT, drop this connect");
                            WifiConfiguration eapSimConfig = WifiStateMachine.this.mWifiConfigManager.getWifiConfiguration(config4.networkId);
                            WifiStateMachine.this.mWifiConfigManager.updateNetworkSelectionStatus(eapSimConfig, 10);
                            return true;
                        }
                        if (!WifiStateMachine.this.checkOrCleanIdentity(config4)) {
                            WifiStateMachine.this.logd("checkOrCleanIdentity fail, exist auto connect");
                            return true;
                        }
                        WifiStateMachine.this.logd("checkOrCleanIdentity success, continue auto connect");
                    }
                    WifiStateMachine.this.setTargetBssid(config4, WifiStateMachine.this.mTargetRoamBSSID);
                    WifiStateMachine.this.logd("CMD_AUTO_CONNECT will save config -> " + config4.SSID + " nid=" + Integer.toString(netId3));
                    WifiStateMachine.this.checkGbkEncoding(config4);
                    int netId4 = WifiStateMachine.this.mWifiConfigManager.saveNetwork(config4, -1).getNetworkId();
                    WifiStateMachine.this.logd("CMD_AUTO_CONNECT did save config ->  nid=" + Integer.toString(netId4));
                    WifiConfiguration config5 = WifiStateMachine.this.mWifiConfigManager.getWifiConfiguration(netId4);
                    if (config5 == null) {
                        WifiStateMachine.this.loge("CMD_AUTO_CONNECT couldn't update the config, got null config");
                        return true;
                    }
                    if (netId4 != config5.networkId) {
                        WifiStateMachine.this.loge("CMD_AUTO_CONNECT couldn't update the config, want nid=" + Integer.toString(netId4) + " but got" + config5.networkId);
                        return true;
                    }
                    if (WifiStateMachine.this.deferForUserInput(message, netId4, false)) {
                        return true;
                    }
                    if (WifiStateMachine.this.mWifiConfigManager.getWifiConfiguration(netId4).userApproved == 2) {
                        WifiStateMachine.this.replyToMessage(message, 151554, 9);
                        return true;
                    }
                    int lastConnectUid = -1;
                    boolean tmpResult = WifiStateMachine.this.hasCustomizedAutoConnect() ? WifiStateMachine.this.mWifiConfigManager.enableNetwork(config5, true, -1) : WifiStateMachine.this.mWifiConfigManager.selectNetwork(config5, false, -1);
                    if (!tmpResult) {
                        WifiStateMachine.this.loge("Failed to connect config: " + config5 + " netId: " + netId4);
                        WifiStateMachine.this.replyToMessage(message, 151554, 0);
                        WifiStateMachine.this.reportConnectionAttemptEnd(5, 1);
                        return true;
                    }
                    WifiStateMachine.this.mWifiMetrics.startConnectionEvent(config5, WifiStateMachine.this.mTargetRoamBSSID, 5);
                    if (!didDisconnect) {
                        WifiStateMachine.this.mWifiMetrics.setConnectionEventRoamType(1);
                    }
                    if (WifiStateMachine.this.mWifiConfigManager.isLastSelectedConfiguration(config5) && WifiStateMachine.this.mWifiConfigManager.isCurrentUserProfile(UserHandle.getUserId(config5.lastConnectUid))) {
                        lastConnectUid = config5.lastConnectUid;
                        WifiStateMachine.this.mWifiMetrics.setConnectionEventRoamType(4);
                    }
                    if (!WifiStateMachine.this.mWifiConfigManager.selectNetwork(config5, false, lastConnectUid) || !WifiStateMachine.this.mWifiNative.reconnect()) {
                        WifiStateMachine.this.loge("Failed to connect config: " + config5 + " netId: " + netId4);
                        WifiStateMachine.this.replyToMessage(message, 151554, 0);
                        WifiStateMachine.this.reportConnectionAttemptEnd(5, 1);
                        return true;
                    }
                    WifiStateMachine.this.lastConnectAttemptTimestamp = System.currentTimeMillis();
                    WifiStateMachine.this.targetWificonfiguration = WifiStateMachine.this.mWifiConfigManager.getWifiConfiguration(netId4);
                    WifiConfiguration config6 = WifiStateMachine.this.mWifiConfigManager.getWifiConfiguration(netId4);
                    if (config6 != null && !WifiStateMachine.this.mWifiConfigManager.isLastSelectedConfiguration(config6)) {
                        WifiStateMachine.this.mWifiConfigManager.setAndEnableLastSelectedConfiguration(-1);
                    }
                    WifiStateMachine.this.mAutoRoaming = false;
                    if (WifiStateMachine.this.isRoaming() || WifiStateMachine.this.linkDebouncing) {
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mRoamingState);
                        return true;
                    }
                    if (!didDisconnect) {
                        return true;
                    }
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectingState);
                    return true;
                case WifiStateMachine.CMD_AUTO_ROAM:
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DISCARD;
                    return true;
                case WifiStateMachine.CMD_AUTO_SAVE_NETWORK:
                    break;
                case WifiStateMachine.CMD_ASSOCIATED_BSSID:
                    String someBssid = (String) message.obj;
                    if (someBssid == null) {
                        return false;
                    }
                    WifiConfiguration someConf = WifiStateMachine.this.mWifiConfigManager.getWifiConfiguration(WifiStateMachine.this.mTargetNetworkId);
                    ScanDetailCache scanDetailCache = WifiStateMachine.this.mWifiConfigManager.getScanDetailCache(someConf);
                    if (scanDetailCache == null) {
                        return false;
                    }
                    WifiStateMachine.this.mWifiMetrics.setConnectionScanDetail(scanDetailCache.getScanDetail(someBssid));
                    return false;
                case WifiStateMachine.CMD_REMOVE_USER_CONFIGURATIONS:
                    WifiStateMachine.this.mWifiConfigManager.removeNetworksForUser(message.arg1);
                    return true;
                case WifiStateMachine.M_CMD_FACTORY_RESET:
                    int uid = message.arg1;
                    List<WifiConfiguration> networks = WifiStateMachine.this.mWifiConfigManager.getSavedNetworks();
                    if (networks == null) {
                        WifiStateMachine.this.loge("M_CMD_FACTORY_RESET networks is null");
                        return true;
                    }
                    for (WifiConfiguration c : networks) {
                        if (WifiStateMachine.this.mWifiConfigManager.canModifyNetwork(uid, c.networkId, false)) {
                            boolean ok4 = WifiStateMachine.this.mWifiConfigManager.removeNetwork(c.networkId);
                            if (WifiStateMachine.this.hasCustomizedAutoConnect()) {
                                WifiStateMachine.this.mWifiConfigManager.removeDisconnectNetwork(c.networkId);
                                if (ok4 && c.networkId == WifiStateMachine.this.mWifiInfo.getNetworkId()) {
                                    WifiStateMachine.this.mDisconnectOperation = true;
                                    WifiStateMachine.this.mScanForWeakSignal = false;
                                }
                            }
                        } else {
                            WifiStateMachine.this.logw("Not authorized to remove network  cnid=" + c.networkId + " uid=" + uid);
                        }
                    }
                    WifiStateMachine.this.mWifiConfigManager.saveConfig();
                    WifiStateMachine.this.logd("M_CMD_FACTORY_RESET, " + networks.size() + " configured networks are removed");
                    return true;
                case WifiP2pServiceImpl.DISCONNECT_WIFI_REQUEST:
                    if (message.arg1 == 1) {
                        WifiStateMachine.this.mWifiNative.disconnect();
                        WifiStateMachine.this.mTemporarilyDisconnectWifi = true;
                        return true;
                    }
                    WifiStateMachine.this.mWifiNative.reconnect();
                    WifiStateMachine.this.mTemporarilyDisconnectWifi = false;
                    return true;
                case WifiMonitor.NETWORK_CONNECTION_EVENT:
                    if (WifiStateMachine.DBG) {
                        WifiStateMachine.this.log("Network connection established");
                    }
                    WifiStateMachine.this.mLastNetworkId = message.arg1;
                    WifiStateMachine.this.mLastBssid = (String) message.obj;
                    WifiStateMachine.this.mConnectNetwork = false;
                    if (WifiStateMachine.this.hasCustomizedAutoConnect()) {
                        WifiStateMachine.this.mDisconnectOperation = false;
                    }
                    WifiStateMachine.this.mWifiInfo.setBSSID(WifiStateMachine.this.mLastBssid);
                    WifiStateMachine.this.mWifiInfo.setNetworkId(WifiStateMachine.this.mLastNetworkId);
                    WifiStateMachine.this.mWifiQualifiedNetworkSelector.enableBssidForQualityNetworkSelection(WifiStateMachine.this.mLastBssid, true);
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
                        return true;
                    }
                    if (!WifiStateMachine.this.linkDebouncing && state == SupplicantState.DISCONNECTED && WifiStateMachine.this.mNetworkInfo.getState() != NetworkInfo.State.DISCONNECTED) {
                        if (WifiStateMachine.DBG) {
                            WifiStateMachine.this.log("Missed CTRL-EVENT-DISCONNECTED, disconnect");
                        }
                        WifiStateMachine.this.handleNetworkDisconnect();
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectedState);
                    }
                    if (state != SupplicantState.COMPLETED) {
                        return true;
                    }
                    WifiStateMachine.this.mIpManager.confirmConfiguration();
                    return true;
                case WifiMonitor.AUTHENTICATION_FAILURE_EVENT:
                    WifiStateMachine.this.mWifiLogger.captureBugReportData(2);
                    WifiStateMachine.this.mSupplicantStateTracker.sendMessage(WifiMonitor.AUTHENTICATION_FAILURE_EVENT);
                    if (WifiStateMachine.this.mTargetNetworkId != -1) {
                        WifiStateMachine.this.mWifiConfigManager.updateNetworkSelectionStatus(WifiStateMachine.this.mTargetNetworkId, 3);
                    }
                    if (WifiStateMachine.this.mWifiFwkExt != null) {
                        WifiStateMachine.this.mWifiFwkExt.setNotificationVisible(true);
                    }
                    WifiStateMachine.this.reportConnectionAttemptEnd(3, 1);
                    WifiStateMachine.this.mWifiLastResortWatchdog.noteConnectionFailureAndTriggerIfNeeded(WifiStateMachine.this.getTargetSsid(), WifiStateMachine.this.mTargetRoamBSSID, 2);
                    return true;
                case WifiMonitor.SSID_TEMP_DISABLED:
                    Log.e(WifiStateMachine.TAG, "Supplicant SSID temporary disabled:" + WifiStateMachine.this.mWifiConfigManager.getWifiConfiguration(message.arg1));
                    WifiStateMachine.this.mWifiConfigManager.updateNetworkSelectionStatus(message.arg1, 3);
                    if (WifiStateMachine.this.mWifiFwkExt != null) {
                        WifiStateMachine.this.mWifiFwkExt.setNotificationVisible(true);
                    }
                    WifiStateMachine.this.reportConnectionAttemptEnd(4, 1);
                    WifiStateMachine.this.mWifiLastResortWatchdog.noteConnectionFailureAndTriggerIfNeeded(WifiStateMachine.this.getTargetSsid(), WifiStateMachine.this.mTargetRoamBSSID, 2);
                    return true;
                case WifiMonitor.SSID_REENABLED:
                    Log.d(WifiStateMachine.TAG, "Supplicant SSID reenable:" + WifiStateMachine.this.mWifiConfigManager.getWifiConfiguration(message.arg1));
                    return true;
                case WifiMonitor.SUP_REQUEST_IDENTITY:
                    int networkId = message.arg2;
                    boolean identitySent = false;
                    int eapMethod = -1;
                    if (WifiStateMachine.this.targetWificonfiguration != null && WifiStateMachine.this.targetWificonfiguration.enterpriseConfig != null) {
                        eapMethod = WifiStateMachine.this.targetWificonfiguration.enterpriseConfig.getEapMethod();
                    }
                    if (WifiStateMachine.this.targetWificonfiguration != null && WifiStateMachine.this.targetWificonfiguration.networkId == networkId && WifiStateMachine.this.targetWificonfiguration.allowedKeyManagement.get(3) && ((eapMethod == 4 || eapMethod == 5 || eapMethod == 6) && (tm = (TelephonyManager) WifiStateMachine.this.mContext.getSystemService("phone")) != null)) {
                        mccMnc = "";
                        int slotId = WifiConfigurationUtil.getIntSimSlot(WifiStateMachine.this.targetWificonfiguration);
                        int subId = WifiStateMachine.this.getSubId(slotId);
                        WifiStateMachine.this.log("simSlot: " + WifiStateMachine.this.targetWificonfiguration.simSlot + " " + slotId + "subId: " + subId);
                        if (TelephonyManager.getDefault().getPhoneCount() < 2 || subId == -1) {
                            imsi = tm.getSubscriberId();
                            mccMnc = tm.getSimState() == 5 ? tm.getSimOperator() : "";
                            if (subId == -1) {
                                WifiStateMachine.this.log("config.simSlot is unspecified(-1), so it should be changed to default sim slot");
                                WifiStateMachine.this.targetWificonfiguration.simSlot = "\"" + tm.getDefaultSim() + "\"";
                                if (WifiStateMachine.this.setSimSlotNative(WifiStateMachine.this.targetWificonfiguration)) {
                                    WifiStateMachine.this.log("config.simSlot is changed to " + WifiStateMachine.this.targetWificonfiguration.simSlot);
                                }
                            }
                        } else {
                            imsi = tm.getSubscriberId(subId);
                            if (tm.getSimState(slotId) == 5) {
                                mccMnc = tm.getSimOperator(subId);
                            }
                        }
                        if (WifiStateMachine.DBG) {
                            WifiStateMachine.this.log("imsi: " + imsi + ", mccMnc: " + mccMnc);
                        }
                        String identity = WifiStateMachine.this.buildIdentity(eapMethod, imsi, mccMnc);
                        if (!identity.isEmpty()) {
                            WifiStateMachine.this.mWifiNative.simIdentityResponse(networkId, identity);
                            WifiStateMachine.this.targetWificonfiguration.enterpriseConfig.setIdentity(identity);
                            if (WifiStateMachine.DBG) {
                                WifiStateMachine.this.log("Record identity in framework, for avoiding clear identity when auto connect");
                            }
                            identitySent = true;
                        }
                    }
                    if (identitySent) {
                        return true;
                    }
                    String ssid = (String) message.obj;
                    if (WifiStateMachine.this.targetWificonfiguration != null && ssid != null && WifiStateMachine.this.targetWificonfiguration.SSID != null && WifiStateMachine.this.targetWificonfiguration.SSID.equals("\"" + ssid + "\"")) {
                        WifiStateMachine.this.mWifiConfigManager.updateNetworkSelectionStatus(WifiStateMachine.this.targetWificonfiguration, 7);
                    }
                    WifiStateMachine.this.mWifiConfigManager.setAndEnableLastSelectedConfiguration(-1);
                    if (WifiStateMachine.this.hasCustomizedAutoConnect()) {
                        WifiStateMachine.this.log("Skip SUP_REQUEST_IDENTITY disconnect for customization!");
                        return true;
                    }
                    WifiStateMachine.this.mWifiNative.disconnect();
                    return true;
                case WifiMonitor.SUP_REQUEST_SIM_AUTH:
                    WifiStateMachine.this.logd("Received SUP_REQUEST_SIM_AUTH");
                    SimAuthRequestData requestData = (SimAuthRequestData) message.obj;
                    if (requestData == null) {
                        WifiStateMachine.this.loge("Invalid sim auth request");
                        return true;
                    }
                    if (requestData.protocol == 4) {
                        WifiStateMachine.this.handleGsmAuthRequest(requestData);
                        return true;
                    }
                    if (requestData.protocol != 5 && requestData.protocol != 6) {
                        return true;
                    }
                    WifiStateMachine.this.handle3GAuthRequest(requestData);
                    return true;
                case WifiMonitor.ASSOCIATION_REJECTION_EVENT:
                    WifiStateMachine.this.mWifiLogger.captureBugReportData(1);
                    WifiStateMachine.this.didBlackListBSSID = false;
                    String bssid = (String) message.obj;
                    if (bssid == null || TextUtils.isEmpty(bssid)) {
                        bssid = WifiStateMachine.this.mTargetRoamBSSID;
                    }
                    if (bssid != null && WifiStateMachine.this.mWifiConnectivityManager != null) {
                        WifiStateMachine.this.didBlackListBSSID = WifiStateMachine.this.mWifiConnectivityManager.trackBssid(bssid, false);
                    }
                    WifiStateMachine.this.mWifiConfigManager.updateNetworkSelectionStatus(WifiStateMachine.this.mTargetNetworkId, 2);
                    WifiStateMachine.this.mSupplicantStateTracker.sendMessage(WifiMonitor.ASSOCIATION_REJECTION_EVENT);
                    WifiStateMachine.this.reportConnectionAttemptEnd(2, 1);
                    WifiStateMachine.this.mWifiLastResortWatchdog.noteConnectionFailureAndTriggerIfNeeded(WifiStateMachine.this.getTargetSsid(), bssid, 1);
                    return true;
                case WifiMonitor.WAPI_NO_CERTIFICATION_EVENT:
                    Log.d(WifiStateMachine.TAG, "WAPI no certification!");
                    WifiStateMachine.this.mContext.sendBroadcastAsUser(new Intent("android.net.wifi.NO_CERTIFICATION"), UserHandle.ALL);
                    return true;
                case WifiMonitor.NEW_PAC_UPDATED_EVENT:
                    Log.d(WifiStateMachine.TAG, "EAP-FAST new pac updated!");
                    WifiStateMachine.this.mContext.sendBroadcastAsUser(new Intent("android.net.wifi.NEW_PAC_UPDATED"), UserHandle.ALL);
                    return true;
                case WifiMonitor.TDLS_CONNECTED_EVENT:
                case WifiMonitor.TDLS_DISCONNECTED_EVENT:
                    String event = (String) message.obj;
                    WifiStateMachine.this.logd("event: " + event);
                    String[] tokens = event.split(" ");
                    if (tokens.length < 2) {
                        WifiStateMachine.this.loge("token length < 2, format is wrong");
                        return true;
                    }
                    int peerStrLen = "peer=".length();
                    String token = tokens[1];
                    if (!token.startsWith("peer=")) {
                        WifiStateMachine.this.loge("not start with \"peer\", format is wrong");
                        return true;
                    }
                    String peer = new String(token.getBytes(), peerStrLen, token.length() - peerStrLen);
                    WifiStateMachine.this.logd("peer: " + peer);
                    WifiStateMachine.this.sendTdlsEventBroadcast(message.what == 147539, peer);
                    return true;
                case 151553:
                    if (!WifiStateMachine.this.mWifiConfigManager.isCurrentUserProfile(UserHandle.getUserId(message.sendingUid)) && message.sendingUid != WifiStateMachine.this.mSystemUiUid) {
                        WifiStateMachine.this.loge("Only the current foreground user can modify networks  currentUserId=" + WifiStateMachine.this.mWifiConfigManager.getCurrentUserId() + " sendingUserId=" + UserHandle.getUserId(message.sendingUid));
                        WifiStateMachine.this.replyToMessage(message, 151554, 9);
                        return true;
                    }
                    int netId5 = message.arg1;
                    WifiConfiguration config7 = (WifiConfiguration) message.obj;
                    WifiStateMachine.this.mWifiConnectionStatistics.numWifiManagerJoinAttempt++;
                    boolean updatedExisting = false;
                    if (config7 != null) {
                        if (!WifiStateMachine.this.recordUidIfAuthorized(config7, message.sendingUid, true)) {
                            WifiStateMachine.this.logw("Not authorized to update network  config=" + config7.SSID + " cnid=" + config7.networkId + " uid=" + message.sendingUid);
                            WifiStateMachine.this.replyToMessage(message, 151554, 9);
                            return true;
                        }
                        String configKey = config7.configKey(true);
                        WifiConfiguration savedConfig = WifiStateMachine.this.mWifiConfigManager.getWifiConfiguration(configKey);
                        if (savedConfig != null) {
                            config7 = savedConfig;
                            WifiStateMachine.this.logd("CONNECT_NETWORK updating existing config with id=" + savedConfig.networkId + " configKey=" + configKey);
                            savedConfig.ephemeral = false;
                            WifiStateMachine.this.mWifiConfigManager.updateNetworkSelectionStatus(savedConfig, 0);
                            updatedExisting = true;
                        }
                        WifiStateMachine.this.checkGbkEncoding(config7);
                        netId5 = WifiStateMachine.this.mWifiConfigManager.saveNetwork(config7, message.sendingUid).getNetworkId();
                    }
                    WifiConfiguration config8 = WifiStateMachine.this.mWifiConfigManager.getWifiConfiguration(netId5);
                    if (config8 == null) {
                        WifiStateMachine.this.logd("CONNECT_NETWORK no config for id=" + Integer.toString(netId5) + " " + WifiStateMachine.this.mSupplicantStateTracker.getSupplicantStateName() + " my state " + WifiStateMachine.this.getCurrentState().getName());
                        WifiStateMachine.this.replyToMessage(message, 151554, 0);
                        return true;
                    }
                    WifiStateMachine.this.mTargetNetworkId = netId5;
                    WifiStateMachine.this.autoRoamSetBSSID(netId5, WifiLastResortWatchdog.BSSID_ANY);
                    if (message.sendingUid == 1010 || message.sendingUid == 1000) {
                        WifiStateMachine.this.clearConfigBSSID(config8, "CONNECT_NETWORK");
                    }
                    if (WifiStateMachine.this.deferForUserInput(message, netId5, true)) {
                        return true;
                    }
                    if (WifiStateMachine.this.mWifiConfigManager.getWifiConfiguration(netId5).userApproved == 2) {
                        WifiStateMachine.this.replyToMessage(message, 151554, 9);
                        return true;
                    }
                    WifiStateMachine.this.mAutoRoaming = false;
                    boolean persist2 = WifiStateMachine.this.mWifiConfigManager.checkConfigOverridePermission(message.sendingUid);
                    WifiStateMachine.this.mWifiConfigManager.setAndEnableLastSelectedConfiguration(netId5);
                    if (WifiStateMachine.this.mWifiConnectivityManager != null) {
                        WifiStateMachine.this.mWifiConnectivityManager.connectToUserSelectNetwork(netId5, persist2);
                    }
                    boolean didDisconnect2 = false;
                    if (WifiStateMachine.this.mLastNetworkId != -1 && WifiStateMachine.this.mLastNetworkId != netId5) {
                        didDisconnect2 = true;
                        WifiStateMachine.this.mWifiNative.disconnect();
                    }
                    WifiStateMachine.this.mWifiMetrics.startConnectionEvent(config8, WifiStateMachine.this.mTargetRoamBSSID, 4);
                    if (!WifiStateMachine.this.mWifiConfigManager.selectNetwork(config8, true, message.sendingUid) || !WifiStateMachine.this.mWifiNative.reconnect()) {
                        WifiStateMachine.this.loge("Failed to connect config: " + config8 + " netId: " + netId5);
                        WifiStateMachine.this.replyToMessage(message, 151554, 0);
                        WifiStateMachine.this.reportConnectionAttemptEnd(5, 1);
                        return true;
                    }
                    WifiStateMachine.this.lastConnectAttemptTimestamp = System.currentTimeMillis();
                    WifiStateMachine.this.targetWificonfiguration = WifiStateMachine.this.mWifiConfigManager.getWifiConfiguration(netId5);
                    WifiStateMachine.this.mSupplicantStateTracker.sendMessage(151553);
                    WifiStateMachine.this.replyToMessage(message, 151555);
                    WifiStateMachine.this.mConnectNetwork = true;
                    WifiStateMachine.this.mLastExplicitNetworkId = netId5;
                    if (WifiStateMachine.this.hasCustomizedAutoConnect()) {
                        WifiStateMachine.this.mDisconnectOperation = true;
                        WifiStateMachine.this.mScanForWeakSignal = false;
                        WifiStateMachine.this.mWifiConfigManager.removeDisconnectNetwork(netId5);
                    }
                    if (didDisconnect2) {
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectingState);
                        return true;
                    }
                    if (updatedExisting && ((WifiStateMachine.this.getCurrentState() == WifiStateMachine.this.mConnectedState || WifiStateMachine.this.getCurrentState() == WifiStateMachine.this.mObtainingIpState) && WifiStateMachine.this.getCurrentWifiConfiguration().networkId == netId5)) {
                        WifiStateMachine.this.updateCapabilities(config8);
                        return true;
                    }
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectedState);
                    return true;
                case 151556:
                    if (!WifiStateMachine.this.mWifiConfigManager.isCurrentUserProfile(UserHandle.getUserId(message.sendingUid))) {
                        WifiStateMachine.this.loge("Only the current foreground user can modify networks  currentUserId=" + WifiStateMachine.this.mWifiConfigManager.getCurrentUserId() + " sendingUserId=" + UserHandle.getUserId(message.sendingUid));
                        WifiStateMachine.this.replyToMessage(message, 151557, 9);
                        return true;
                    }
                    WifiConfiguration toRemove = WifiStateMachine.this.mWifiConfigManager.getWifiConfiguration(message.arg1);
                    if (toRemove == null) {
                        WifiStateMachine.this.lastForgetConfigurationAttempt = null;
                    } else {
                        WifiStateMachine.this.lastForgetConfigurationAttempt = new WifiConfiguration(toRemove);
                    }
                    int netId6 = message.arg1;
                    if (!WifiStateMachine.this.mWifiConfigManager.canModifyNetwork(message.sendingUid, netId6, false)) {
                        WifiStateMachine.this.logw("Not authorized to forget network  cnid=" + netId6 + " uid=" + message.sendingUid);
                        WifiStateMachine.this.replyToMessage(message, 151557, 9);
                        return true;
                    }
                    if (!WifiStateMachine.this.mWifiConfigManager.forgetNetwork(message.arg1)) {
                        WifiStateMachine.this.loge("Failed to forget network");
                        WifiStateMachine.this.replyToMessage(message, 151557, 0);
                        return true;
                    }
                    if (WifiStateMachine.this.hasCustomizedAutoConnect()) {
                        WifiStateMachine.this.mWifiConfigManager.removeDisconnectNetwork(message.arg1);
                        if (message.arg1 == WifiStateMachine.this.mWifiInfo.getNetworkId()) {
                            WifiStateMachine.this.mDisconnectOperation = true;
                            WifiStateMachine.this.mScanForWeakSignal = false;
                        }
                    }
                    if (message.arg1 == WifiStateMachine.this.mLastExplicitNetworkId) {
                        WifiStateMachine.this.mLastExplicitNetworkId = -1;
                        WifiStateMachine.this.mConnectNetwork = false;
                        WifiStateMachine.this.mSupplicantStateTracker.sendMessage(151556, message.arg1);
                    }
                    WifiStateMachine.this.replyToMessage(message, 151558);
                    WifiStateMachine.this.broadcastWifiCredentialChanged(1, (WifiConfiguration) message.obj);
                    return true;
                case 151559:
                    WifiStateMachine.this.mWifiConnectionStatistics.numWifiManagerJoinAttempt++;
                    break;
                case 151562:
                    if (WifiStateMachine.this.hasCustomizedAutoConnect()) {
                        WifiStateMachine.this.mDisconnectOperation = true;
                        WifiStateMachine.this.mScanForWeakSignal = false;
                        WifiStateMachine.this.disableLastNetwork();
                    }
                    WpsInfo wpsInfo = (WpsInfo) message.obj;
                    switch (wpsInfo.setup) {
                        case 0:
                            wpsResult = WifiStateMachine.this.mWifiConfigManager.startWpsPbc(wpsInfo);
                            break;
                        case 1:
                            wpsResult = WifiStateMachine.this.mWifiConfigManager.startWpsWithPinFromDevice(wpsInfo);
                            break;
                        case 2:
                            wpsResult = WifiStateMachine.this.mWifiConfigManager.startWpsWithPinFromAccessPoint(wpsInfo);
                            break;
                        default:
                            wpsResult = new WpsResult(WpsResult.Status.FAILURE);
                            WifiStateMachine.this.loge("Invalid setup for WPS");
                            break;
                    }
                    WifiStateMachine.this.mWifiConfigManager.setAndEnableLastSelectedConfiguration(-1);
                    if (wpsResult.status == WpsResult.Status.SUCCESS) {
                        WifiStateMachine.this.replyToMessage(message, 151563, wpsResult);
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mWpsRunningState);
                        return true;
                    }
                    WifiStateMachine.this.loge("Failed to start WPS with config " + wpsInfo.toString());
                    WifiStateMachine.this.replyToMessage(message, 151564, 0);
                    return true;
                case 151569:
                    if (!WifiStateMachine.this.mWifiConfigManager.updateNetworkSelectionStatus(message.arg1, 9)) {
                        WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_FAIL;
                        WifiStateMachine.this.replyToMessage(message, 151570, 0);
                        return true;
                    }
                    if (WifiStateMachine.this.hasCustomizedAutoConnect()) {
                        WifiStateMachine.this.mWifiConfigManager.addDisconnectNetwork(message.arg1);
                        if (message.arg1 == WifiStateMachine.this.mWifiInfo.getNetworkId()) {
                            WifiStateMachine.this.mDisconnectOperation = true;
                            WifiStateMachine.this.mScanForWeakSignal = false;
                        }
                    }
                    WifiStateMachine.this.replyToMessage(message, 151571);
                    return true;
                case 151578:
                    if (!WifiStateMachine.this.mMtkCtpppoe) {
                        WifiStateMachine.this.replyToMessage(message, 151580, 0);
                        return true;
                    }
                    WifiStateMachine.this.stopPPPoE();
                    WifiStateMachine.this.replyToMessage(message, 151579);
                    return true;
                default:
                    return false;
            }
            if (!WifiStateMachine.this.mWifiConfigManager.isCurrentUserProfile(UserHandle.getUserId(message.sendingUid))) {
                WifiStateMachine.this.loge("Only the current foreground user can modify networks  currentUserId=" + WifiStateMachine.this.mWifiConfigManager.getCurrentUserId() + " sendingUserId=" + UserHandle.getUserId(message.sendingUid));
                WifiStateMachine.this.replyToMessage(message, 151560, 9);
                return true;
            }
            WifiStateMachine.this.lastSavedConfigurationAttempt = null;
            WifiConfiguration config9 = (WifiConfiguration) message.obj;
            if (config9 == null) {
                WifiStateMachine.this.loge("ERROR: SAVE_NETWORK with null configuration" + WifiStateMachine.this.mSupplicantStateTracker.getSupplicantStateName() + " my state " + WifiStateMachine.this.getCurrentState().getName());
                WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_FAIL;
                WifiStateMachine.this.replyToMessage(message, 151560, 0);
                return true;
            }
            WifiStateMachine.this.lastSavedConfigurationAttempt = new WifiConfiguration(config9);
            int nid = config9.networkId;
            WifiStateMachine.this.logd("SAVE_NETWORK id=" + Integer.toString(nid) + " config=" + config9.SSID + " nid=" + config9.networkId + " supstate=" + WifiStateMachine.this.mSupplicantStateTracker.getSupplicantStateName() + " my state " + WifiStateMachine.this.getCurrentState().getName());
            boolean checkUid = message.what == 151559;
            if (checkUid && !WifiStateMachine.this.recordUidIfAuthorized(config9, message.sendingUid, false)) {
                WifiStateMachine.this.logw("Not authorized to update network  config=" + config9.SSID + " cnid=" + config9.networkId + " uid=" + message.sendingUid);
                WifiStateMachine.this.replyToMessage(message, 151560, 9);
                return true;
            }
            WifiStateMachine.this.checkGbkEncoding(config9);
            NetworkUpdateResult result = WifiStateMachine.this.mWifiConfigManager.saveNetwork(config9, -1);
            if (result.getNetworkId() == -1) {
                WifiStateMachine.this.loge("Failed to save network");
                WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_FAIL;
                WifiStateMachine.this.replyToMessage(message, 151560, 0);
                return true;
            }
            if (WifiStateMachine.this.mWifiInfo.getNetworkId() == result.getNetworkId()) {
                if (result.hasIpChanged()) {
                    WifiStateMachine.this.log("Reconfiguring IP on connection");
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mObtainingIpState);
                }
                if (result.hasProxyChanged()) {
                    WifiStateMachine.this.log("Reconfiguring proxy on connection");
                    WifiStateMachine.this.mIpManager.setHttpProxy(WifiStateMachine.this.mWifiConfigManager.getProxyProperties(WifiStateMachine.this.mLastNetworkId));
                }
            }
            WifiStateMachine.this.replyToMessage(message, 151561);
            WifiStateMachine.this.broadcastWifiCredentialChanged(0, config9);
            if (WifiStateMachine.DBG) {
                WifiStateMachine.this.logd("Success save network nid=" + Integer.toString(result.getNetworkId()));
            }
            boolean user = message.what == 151559;
            boolean persistConnect = WifiStateMachine.this.mWifiConfigManager.checkConfigOverridePermission(message.sendingUid);
            if (user) {
                WifiStateMachine.this.mWifiConfigManager.updateLastConnectUid(config9, message.sendingUid);
                WifiStateMachine.this.mWifiConfigManager.writeKnownNetworkHistory();
            }
            if (WifiStateMachine.this.mWifiConnectivityManager != null) {
                WifiStateMachine.this.mWifiConnectivityManager.connectToUserSelectNetwork(result.getNetworkId(), persistConnect);
            }
            if (!WifiStateMachine.this.hasCustomizedAutoConnect()) {
                return true;
            }
            WifiStateMachine.this.checkIfEapNetworkChanged(config9);
            return true;
        }
    }

    private void updateCapabilities(WifiConfiguration config) {
        int rssi;
        NetworkCapabilities networkCapabilities = new NetworkCapabilities(this.mDfltNetworkCapabilities);
        if (config != null) {
            if (config.ephemeral) {
                networkCapabilities.removeCapability(14);
            } else {
                networkCapabilities.addCapability(14);
            }
            if (this.mWifiInfo.getRssi() != -127) {
                rssi = this.mWifiInfo.getRssi();
            } else {
                rssi = Integer.MIN_VALUE;
            }
            networkCapabilities.setSignalStrength(rssi);
        }
        if (this.mWifiInfo.getMeteredHint()) {
            networkCapabilities.removeCapability(11);
        }
        this.mNetworkAgent.sendNetworkCapabilities(networkCapabilities);
    }

    private class WifiNetworkAgent extends NetworkAgent {
        public WifiNetworkAgent(Looper l, Context c, String TAG, NetworkInfo ni, NetworkCapabilities nc, LinkProperties lp, int score, NetworkMisc misc) {
            super(l, c, TAG, ni, nc, lp, score, misc);
        }

        protected void unwanted() {
            if (this != WifiStateMachine.this.mNetworkAgent) {
                return;
            }
            if (WifiStateMachine.DBG) {
                log("WifiNetworkAgent -> Wifi unwanted score " + Integer.toString(WifiStateMachine.this.mWifiInfo.score));
            }
            WifiStateMachine.this.unwantedNetwork(0);
        }

        protected void networkStatus(int status, String redirectUrl) {
            if (this != WifiStateMachine.this.mNetworkAgent) {
                return;
            }
            if (status == 2) {
                if (WifiStateMachine.DBG) {
                    log("WifiNetworkAgent -> Wifi networkStatus invalid, score=" + Integer.toString(WifiStateMachine.this.mWifiInfo.score));
                }
                WifiStateMachine.this.unwantedNetwork(1);
            } else {
                if (status != 1) {
                    return;
                }
                if (WifiStateMachine.DBG) {
                    log("WifiNetworkAgent -> Wifi networkStatus valid, score= " + Integer.toString(WifiStateMachine.this.mWifiInfo.score));
                }
                WifiStateMachine.this.doNetworkStatus(status);
            }
        }

        protected void saveAcceptUnvalidated(boolean accept) {
            if (this != WifiStateMachine.this.mNetworkAgent) {
                return;
            }
            WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_ACCEPT_UNVALIDATED, accept ? 1 : 0);
        }

        protected void startPacketKeepalive(Message msg) {
            WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_START_IP_PACKET_OFFLOAD, msg.arg1, msg.arg2, msg.obj);
        }

        protected void stopPacketKeepalive(Message msg) {
            WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_STOP_IP_PACKET_OFFLOAD, msg.arg1, msg.arg2, msg.obj);
        }

        protected void setSignalStrengthThresholds(int[] thresholds) {
            log("Received signal strength thresholds: " + Arrays.toString(thresholds));
            if (thresholds.length == 0) {
                WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_STOP_RSSI_MONITORING_OFFLOAD, WifiStateMachine.this.mWifiInfo.getRssi());
                return;
            }
            int[] rssiVals = Arrays.copyOf(thresholds, thresholds.length + 2);
            rssiVals[rssiVals.length - 2] = -128;
            rssiVals[rssiVals.length - 1] = 127;
            Arrays.sort(rssiVals);
            byte[] rssiRange = new byte[rssiVals.length];
            for (int i = 0; i < rssiVals.length; i++) {
                int val = rssiVals[i];
                if (val <= 127 && val >= -128) {
                    rssiRange[i] = (byte) val;
                } else {
                    Log.e(WifiStateMachine.TAG, "Illegal value " + val + " for RSSI thresholds: " + Arrays.toString(rssiVals));
                    WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_STOP_RSSI_MONITORING_OFFLOAD, WifiStateMachine.this.mWifiInfo.getRssi());
                    return;
                }
            }
            WifiStateMachine.this.mRssiRanges = rssiRange;
            WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_START_RSSI_MONITORING_OFFLOAD, WifiStateMachine.this.mWifiInfo.getRssi());
        }

        protected void preventAutomaticReconnect() {
            if (this != WifiStateMachine.this.mNetworkAgent) {
                return;
            }
            WifiStateMachine.this.unwantedNetwork(2);
        }
    }

    void unwantedNetwork(int reason) {
        sendMessage(CMD_UNWANTED_NETWORK, reason);
    }

    void doNetworkStatus(int status) {
        sendMessage(CMD_NETWORK_STATUS, status);
    }

    private String buildIdentity(int eapMethod, String imsi, String mccMnc) {
        String prefix;
        String mcc;
        String mnc;
        if (imsi == null || imsi.isEmpty()) {
            return "";
        }
        if (eapMethod == 4) {
            prefix = "1";
        } else if (eapMethod == 5) {
            prefix = "0";
        } else if (eapMethod == 6) {
            prefix = "6";
        } else {
            return "";
        }
        if (mccMnc != null && !mccMnc.isEmpty()) {
            mcc = mccMnc.substring(0, 3);
            mnc = mccMnc.substring(3);
            if (mnc.length() == 2) {
                mnc = "0" + mnc;
            }
        } else {
            mcc = imsi.substring(0, 3);
            mnc = imsi.substring(3, 6);
        }
        return prefix + imsi + "@wlan.mnc" + mnc + ".mcc" + mcc + ".3gppnetwork.org";
    }

    boolean startScanForConfiguration(WifiConfiguration config) {
        if (config == null) {
            return false;
        }
        ScanDetailCache scanDetailCache = this.mWifiConfigManager.getScanDetailCache(config);
        if (scanDetailCache == null || !config.allowedKeyManagement.get(1) || scanDetailCache.size() > 6) {
            return true;
        }
        HashSet<Integer> freqs = this.mWifiConfigManager.makeChannelList(config, ONE_HOUR_MILLI);
        if (freqs != null && freqs.size() != 0) {
            logd("starting scan for " + config.configKey() + " with " + freqs);
            Set<Integer> hiddenNetworkIds = new HashSet<>();
            if (config.hiddenSSID) {
                hiddenNetworkIds.add(Integer.valueOf(config.networkId));
            }
            if (startScanNative(freqs, hiddenNetworkIds, WIFI_WORK_SOURCE)) {
                this.messageHandlingStatus = MESSAGE_HANDLING_STATUS_OK;
            } else {
                this.messageHandlingStatus = MESSAGE_HANDLING_STATUS_HANDLING_ERROR;
            }
            return true;
        }
        if (DBG) {
            logd("no channels for " + config.configKey());
        }
        return false;
    }

    void clearCurrentConfigBSSID(String dbg) {
        WifiConfiguration config = getCurrentWifiConfiguration();
        if (config == null) {
            return;
        }
        clearConfigBSSID(config, dbg);
    }

    void clearConfigBSSID(WifiConfiguration config, String dbg) {
        if (config == null) {
            return;
        }
        if (DBG) {
            logd(dbg + " " + this.mTargetRoamBSSID + " config " + config.configKey() + " config.NetworkSelectionStatus.mNetworkSelectionBSSID " + config.getNetworkSelectionStatus().getNetworkSelectionBSSID());
        }
        if (DBG) {
            logd(dbg + " " + config.SSID + " nid=" + Integer.toString(config.networkId));
        }
        this.mWifiConfigManager.saveWifiConfigBSSID(config, WifiLastResortWatchdog.BSSID_ANY);
    }

    class L2ConnectedState extends State {
        L2ConnectedState() {
        }

        public void enter() {
            if (WifiStateMachine.DBG) {
                WifiStateMachine.this.loge(getName() + "\n");
            }
            WifiStateMachine.this.mRssiPollToken++;
            if (WifiStateMachine.this.mEnableRssiPolling) {
                WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_RSSI_POLL, WifiStateMachine.this.mRssiPollToken, 0);
            }
            if (WifiStateMachine.this.mNetworkAgent != null) {
                WifiStateMachine.this.loge("Have NetworkAgent when entering L2Connected");
                WifiStateMachine.this.setNetworkDetailedState(NetworkInfo.DetailedState.DISCONNECTED);
            }
            WifiStateMachine.this.setNetworkDetailedState(NetworkInfo.DetailedState.CONNECTING);
            WifiStateMachine.this.mNetworkAgent = WifiStateMachine.this.new WifiNetworkAgent(WifiStateMachine.this.getHandler().getLooper(), WifiStateMachine.this.mContext, "WifiNetworkAgent", WifiStateMachine.this.mNetworkInfo, WifiStateMachine.this.mNetworkCapabilitiesFilter, WifiStateMachine.this.mLinkProperties, 60, WifiStateMachine.this.mNetworkMisc);
            WifiStateMachine.this.clearCurrentConfigBSSID("L2ConnectedState");
            WifiStateMachine.this.mCountryCode.setReadyForChange(false);
            WifiStateMachine.this.mWifiMetrics.setWifiState(3);
            if (WifiStateMachine.DBG) {
                WifiStateMachine.this.log("Reset mIsListeningIpReachabilityLost");
            }
            WifiStateMachine.this.mIsListeningIpReachabilityLost = false;
        }

        public void exit() {
            WifiStateMachine.this.log("Leaving L2ConnctedState");
            WifiStateMachine.this.mIpManager.stop();
            if (WifiStateMachine.DBG) {
                StringBuilder sb = new StringBuilder();
                sb.append("leaving L2ConnectedState state nid=").append(Integer.toString(WifiStateMachine.this.mLastNetworkId));
                if (WifiStateMachine.this.mLastBssid != null) {
                    sb.append(" ").append(WifiStateMachine.this.mLastBssid);
                }
            }
            if (WifiStateMachine.this.mLastBssid != null || WifiStateMachine.this.mLastNetworkId != -1) {
                WifiStateMachine.this.handleNetworkDisconnect();
            }
            WifiStateMachine.this.mCountryCode.setReadyForChange(true);
            WifiStateMachine.this.mWifiMetrics.setWifiState(2);
            WifiStateMachine.this.mIsNewAssociatedBssid = false;
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, this);
            switch (message.what) {
                case 1:
                    WifiStateMachine.this.handleSuccessfulPppoeConfiguration((DhcpResults) message.obj);
                    return true;
                case WifiStateMachine.CMD_SET_OPERATIONAL_MODE:
                    if (message.arg1 != 1) {
                        WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_DISCONNECT);
                        WifiStateMachine.this.deferMessage(message);
                        if (message.arg1 == 3) {
                            WifiStateMachine.this.noteWifiDisabledWhileAssociated();
                        }
                    }
                    WifiStateMachine.this.mWifiConfigManager.setAndEnableLastSelectedConfiguration(-1);
                    return true;
                case WifiStateMachine.CMD_DISCONNECT:
                    WifiStateMachine.this.mWifiNative.disconnect();
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectingState);
                    return true;
                case WifiStateMachine.CMD_ENABLE_RSSI_POLL:
                    WifiStateMachine.this.cleanWifiScore();
                    if (WifiStateMachine.this.mWifiConfigManager.mEnableRssiPollWhenAssociated.get()) {
                        WifiStateMachine.this.mEnableRssiPolling = message.arg1 == 1;
                    } else {
                        WifiStateMachine.this.mEnableRssiPolling = false;
                    }
                    WifiStateMachine.this.mRssiPollToken++;
                    if (!WifiStateMachine.this.mEnableRssiPolling) {
                        return true;
                    }
                    WifiStateMachine.this.fetchRssiLinkSpeedAndFrequencyNative();
                    WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.this.obtainMessage(WifiStateMachine.CMD_RSSI_POLL, WifiStateMachine.this.mRssiPollToken, 0), 3000L);
                    return true;
                case WifiStateMachine.CMD_RSSI_POLL:
                    if (message.arg1 != WifiStateMachine.this.mRssiPollToken) {
                        return true;
                    }
                    if (WifiStateMachine.this.mWifiConfigManager.mEnableChipWakeUpWhenAssociated.get()) {
                        if (WifiStateMachine.DBG) {
                            WifiStateMachine.this.log(" get link layer stats " + WifiStateMachine.this.mWifiLinkLayerStatsSupported);
                        }
                        WifiLinkLayerStats stats = WifiStateMachine.this.getWifiLinkLayerStats(WifiStateMachine.DBG);
                        if (stats != null && WifiStateMachine.this.mWifiInfo.getRssi() != -127 && (stats.rssi_mgmt == 0 || stats.beacon_rx == 0)) {
                        }
                        WifiStateMachine.this.fetchRssiLinkSpeedAndFrequencyNative();
                        WifiStateMachine.this.mWifiScoreReport = WifiScoreReport.calculateScore(WifiStateMachine.this.mWifiInfo, WifiStateMachine.this.getCurrentWifiConfiguration(), WifiStateMachine.this.mWifiConfigManager, WifiStateMachine.this.mNetworkAgent, WifiStateMachine.this.mWifiScoreReport, WifiStateMachine.this.mAggressiveHandover, WifiStateMachine.this.hasCustomizedAutoConnect());
                    }
                    WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.this.obtainMessage(WifiStateMachine.CMD_RSSI_POLL, WifiStateMachine.this.mRssiPollToken, 0), 3000L);
                    if (!WifiStateMachine.DBG) {
                        return true;
                    }
                    WifiStateMachine.this.sendRssiChangeBroadcast(WifiStateMachine.this.mWifiInfo.getRssi());
                    return true;
                case WifiStateMachine.CMD_DELAYED_NETWORK_DISCONNECT:
                    if (!WifiStateMachine.this.linkDebouncing && WifiStateMachine.this.mWifiConfigManager.mEnableLinkDebouncing) {
                        WifiStateMachine.this.logd("CMD_DELAYED_NETWORK_DISCONNECT and not debouncing - ignore " + message.arg1);
                        return true;
                    }
                    WifiStateMachine.this.logd("CMD_DELAYED_NETWORK_DISCONNECT and debouncing - disconnect " + message.arg1);
                    WifiStateMachine.this.linkDebouncing = false;
                    WifiStateMachine.this.handleNetworkDisconnect();
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectedState);
                    return true;
                case WifiStateMachine.CMD_RESET_SIM_NETWORKS:
                    if (WifiStateMachine.this.mLastNetworkId == -1) {
                        return false;
                    }
                    WifiConfiguration config = WifiStateMachine.this.mWifiConfigManager.getWifiConfiguration(WifiStateMachine.this.mLastNetworkId);
                    int removedSimSlot = message.arg1;
                    int configSimSlot = WifiConfigurationUtil.getIntSimSlot(config);
                    if (!WifiStateMachine.this.mWifiConfigManager.isSimConfig(config) || configSimSlot != removedSimSlot) {
                        return false;
                    }
                    WifiStateMachine.this.log("config.simSlot: " + config.simSlot + "," + configSimSlot + " equals removedSimSlot: " + removedSimSlot);
                    WifiStateMachine.this.mWifiNative.disconnect();
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectingState);
                    return false;
                case WifiStateMachine.CMD_IP_CONFIGURATION_SUCCESSFUL:
                    WifiStateMachine.this.handleSuccessfulIpConfiguration();
                    WifiStateMachine.this.reportConnectionAttemptEnd(1, 1);
                    WifiStateMachine.this.sendConnectedState();
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mConnectedState);
                    return true;
                case WifiStateMachine.CMD_IP_CONFIGURATION_LOST:
                    WifiStateMachine.this.getWifiLinkLayerStats(true);
                    WifiStateMachine.this.handleIpConfigurationLost();
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectingState);
                    return true;
                case WifiStateMachine.CMD_ASSOCIATED_BSSID:
                    if (((String) message.obj) == null) {
                        WifiStateMachine.this.logw("Associated command w/o BSSID");
                        return true;
                    }
                    if (WifiStateMachine.this.mLastBssid != null && !WifiStateMachine.this.mLastBssid.equals(message.obj)) {
                        WifiStateMachine.this.mIsNewAssociatedBssid = true;
                    }
                    WifiStateMachine.this.mLastBssid = (String) message.obj;
                    if (WifiStateMachine.this.mLastBssid == null) {
                        return true;
                    }
                    if (WifiStateMachine.this.mWifiInfo.getBSSID() != null && WifiStateMachine.this.mLastBssid.equals(WifiStateMachine.this.mWifiInfo.getBSSID())) {
                        return true;
                    }
                    WifiStateMachine.this.mWifiInfo.setBSSID((String) message.obj);
                    WifiStateMachine.this.sendNetworkStateChangeBroadcast(WifiStateMachine.this.mLastBssid);
                    return true;
                case WifiStateMachine.CMD_IP_REACHABILITY_LOST:
                    if (WifiStateMachine.this.isTemporarilyDontReconnectWifi()) {
                        WifiStateMachine.this.log("isTemporarilyDontReconnectWifi is true, ignore CMD_IP_REACHABILITY_LOST");
                        return true;
                    }
                    if (!WifiStateMachine.this.enableIpReachabilityMonitor()) {
                        Log.d(WifiStateMachine.TAG, "Ignore CMD_IP_REACHABILITY_LOST due to enableIpReachabilityMonitor is off");
                        return true;
                    }
                    Log.d(WifiStateMachine.TAG, "mIsListeningIpReachabilityLost: " + WifiStateMachine.this.mIsListeningIpReachabilityLost);
                    if (!WifiStateMachine.this.enableIpReachabilityMonitorEnhancement() || WifiStateMachine.this.mIsListeningIpReachabilityLost) {
                        if (WifiStateMachine.DBG && message.obj != null) {
                            WifiStateMachine.this.log((String) message.obj);
                        }
                        WifiStateMachine.this.handleIpReachabilityLost();
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectingState);
                        return true;
                    }
                    Log.d(WifiStateMachine.TAG, "mIsListeningIpReachabilityLost: " + WifiStateMachine.this.mIsListeningIpReachabilityLost);
                    if (!WifiStateMachine.this.mIsListeningIpReachabilityLost) {
                        Log.d(WifiStateMachine.TAG, "Ignore CMD_IP_REACHABILITY_LOST");
                        return true;
                    }
                    if (WifiStateMachine.DBG && message.obj != null) {
                        WifiStateMachine.this.log((String) message.obj);
                    }
                    WifiStateMachine.this.handleIpReachabilityLost();
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectingState);
                    return true;
                case WifiStateMachine.CMD_START_RSSI_MONITORING_OFFLOAD:
                case WifiStateMachine.CMD_RSSI_THRESHOLD_BREACH:
                    byte currRssi = (byte) message.arg1;
                    WifiStateMachine.this.processRssiThreshold(currRssi, message.what);
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
                case WifiStateMachine.M_CMD_GET_WIFI_STATUS:
                    String answer = WifiStateMachine.this.mWifiNative.status();
                    WifiStateMachine.this.replyToMessage(message, message.what, answer);
                    return true;
                case WifiStateMachine.M_CMD_IP_REACHABILITY_MONITOR_TIMER:
                    if (message.arg1 != WifiStateMachine.this.ipReachabilityMonitorCount) {
                        Log.d(WifiStateMachine.TAG, "IpReachabilityMonitor count mismatch, count: " + WifiStateMachine.this.ipReachabilityMonitorCount + ", arg1: " + message.arg1);
                        return true;
                    }
                    Log.d(WifiStateMachine.TAG, "IpReachabilityMonitor timer time out, count: " + WifiStateMachine.this.ipReachabilityMonitorCount);
                    WifiStateMachine.this.mIsListeningIpReachabilityLost = false;
                    return true;
                case WifiP2pServiceImpl.DISCONNECT_WIFI_REQUEST:
                    if (message.arg1 != 1) {
                        return true;
                    }
                    WifiStateMachine.this.mWifiNative.disconnect();
                    if (WifiStateMachine.this.hasCustomizedAutoConnect()) {
                        WifiStateMachine.this.mDisconnectOperation = true;
                    }
                    WifiStateMachine.this.mTemporarilyDisconnectWifi = true;
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectingState);
                    return true;
                case WifiMonitor.NETWORK_CONNECTION_EVENT:
                    Log.d(WifiStateMachine.TAG, "mLastBssid:" + WifiStateMachine.this.mLastBssid + ", newBssid:" + ((String) message.obj) + ", mIsNewAssociatedBssid:" + WifiStateMachine.this.mIsNewAssociatedBssid);
                    if (WifiStateMachine.this.mLastBssid != null && message.obj != null && WifiStateMachine.this.mLastBssid.equals(message.obj) && !WifiStateMachine.this.mIsNewAssociatedBssid) {
                        return true;
                    }
                    WifiStateMachine.this.mIsNewAssociatedBssid = false;
                    WifiStateMachine.this.mWifiInfo.setBSSID((String) message.obj);
                    WifiStateMachine.this.mLastNetworkId = message.arg1;
                    WifiStateMachine.this.mWifiInfo.setNetworkId(WifiStateMachine.this.mLastNetworkId);
                    if (!WifiStateMachine.this.mLastBssid.equals((String) message.obj)) {
                        WifiStateMachine.this.mLastBssid = (String) message.obj;
                        WifiStateMachine.this.sendNetworkStateChangeBroadcast(WifiStateMachine.this.mLastBssid);
                    }
                    if (!WifiStateMachine.this.enableIpReachabilityMonitorEnhancement()) {
                        return true;
                    }
                    WifiStateMachine.this.startListenToIpReachabilityLost();
                    Log.d(WifiStateMachine.TAG, "driver roaming, start to listen ip reachability lost for 10 sec, counter: " + WifiStateMachine.this.ipReachabilityMonitorCount);
                    return true;
                case 151553:
                    int netId = message.arg1;
                    if (WifiStateMachine.this.mWifiInfo.getNetworkId() != netId) {
                        return false;
                    }
                    WifiStateMachine.this.replyToMessage(message, 151555);
                    return true;
                case 151572:
                    RssiPacketCountInfo info = new RssiPacketCountInfo();
                    WifiStateMachine.this.fetchRssiLinkSpeedAndFrequencyNative();
                    info.rssi = WifiStateMachine.this.mWifiInfo.getRssi();
                    WifiStateMachine.this.fetchPktcntNative(info);
                    WifiStateMachine.this.replyToMessage(message, 151573, info);
                    return true;
                case 151575:
                    if (!WifiStateMachine.this.mMtkCtpppoe) {
                        WifiStateMachine.this.replyToMessage(message, 151577, 0);
                        return true;
                    }
                    Log.d(WifiStateMachine.TAG, "mPppoeInfo.status:" + WifiStateMachine.this.mPppoeInfo.status + ", config:" + ((PPPOEConfig) message.obj));
                    if (WifiStateMachine.this.mPppoeInfo.status == PPPOEInfo.Status.ONLINE) {
                        WifiStateMachine.this.replyToMessage(message, 151576);
                        WifiStateMachine.this.sendPppoeCompletedBroadcast("ALREADY_ONLINE", -1);
                        return true;
                    }
                    WifiStateMachine.this.mPppoeConfig = (PPPOEConfig) message.obj;
                    WifiStateMachine.this.mUsingPppoe = true;
                    if (WifiStateMachine.this.mPppoeHandler == null) {
                        HandlerThread pppoeThread = new HandlerThread("PPPoE Handler Thread");
                        pppoeThread.start();
                        WifiStateMachine.this.mPppoeHandler = WifiStateMachine.this.new PppoeHandler(pppoeThread.getLooper(), WifiStateMachine.this);
                    }
                    WifiStateMachine.this.mPppoeHandler.sendEmptyMessage(0);
                    WifiStateMachine.this.replyToMessage(message, 151576);
                    return true;
                case 196611:
                    WifiStateMachine.this.handlePreDhcpSetup();
                    return true;
                case 196612:
                    WifiStateMachine.this.handlePostDhcpSetup();
                    return true;
                case 196614:
                    WifiConfiguration wifiConfig = WifiStateMachine.this.getCurrentWifiConfiguration();
                    WifiStateMachine.this.mIpManager.completedPreDhcpAction(wifiConfig);
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
            IpManager.ProvisioningConfiguration prov;
            IpManager.ProvisioningConfiguration prov2;
            if (WifiStateMachine.DBG) {
                String key = WifiStateMachine.this.getCurrentWifiConfiguration() != null ? WifiStateMachine.this.getCurrentWifiConfiguration().configKey() : "";
                WifiStateMachine.this.log("enter ObtainingIpState netId=" + Integer.toString(WifiStateMachine.this.mLastNetworkId) + " " + key + "  roam=" + WifiStateMachine.this.mAutoRoaming + " static=" + WifiStateMachine.this.mWifiConfigManager.isUsingStaticIp(WifiStateMachine.this.mLastNetworkId) + " watchdog= " + WifiStateMachine.this.obtainingIpWatchdogCount);
            }
            WifiStateMachine.this.linkDebouncing = false;
            WifiStateMachine.this.setNetworkDetailedState(NetworkInfo.DetailedState.OBTAINING_IPADDR);
            WifiStateMachine.this.clearCurrentConfigBSSID("ObtainingIpAddress");
            WifiStateMachine.this.stopIpManager();
            WifiStateMachine.this.mIpManager.setHttpProxy(WifiStateMachine.this.mWifiConfigManager.getProxyProperties(WifiStateMachine.this.mLastNetworkId));
            if (!TextUtils.isEmpty(WifiStateMachine.this.mTcpBufferSizes)) {
                WifiStateMachine.this.mIpManager.setTcpBufferSizes(WifiStateMachine.this.mTcpBufferSizes);
            }
            if (WifiStateMachine.this.mWifiConfigManager.isUsingStaticIp(WifiStateMachine.this.mLastNetworkId)) {
                StaticIpConfiguration config = WifiStateMachine.this.mWifiConfigManager.getStaticIpConfiguration(WifiStateMachine.this.mLastNetworkId);
                if (config.ipAddress == null) {
                    WifiStateMachine.this.logd("Static IP lacks address");
                    WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_IPV4_PROVISIONING_FAILURE);
                    return;
                }
                if (WifiStateMachine.this.enableIpReachabilityMonitor()) {
                    IpManager unused = WifiStateMachine.this.mIpManager;
                    prov = IpManager.buildProvisioningConfiguration().withStaticConfiguration(config).withApfCapabilities(WifiStateMachine.this.mWifiNative.getApfCapabilities()).build();
                } else {
                    IpManager unused2 = WifiStateMachine.this.mIpManager;
                    prov = IpManager.buildProvisioningConfiguration().withStaticConfiguration(config).withApfCapabilities(WifiStateMachine.this.mWifiNative.getApfCapabilities()).withoutIpReachabilityMonitor().build();
                }
                try {
                    Thread.sleep(500L);
                } catch (Exception e) {
                }
                WifiStateMachine.this.mIpManager.startProvisioning(prov);
                return;
            }
            if (WifiStateMachine.this.enableIpReachabilityMonitor()) {
                IpManager unused3 = WifiStateMachine.this.mIpManager;
                prov2 = IpManager.buildProvisioningConfiguration().withPreDhcpAction().withApfCapabilities(WifiStateMachine.this.mWifiNative.getApfCapabilities()).build();
            } else {
                IpManager unused4 = WifiStateMachine.this.mIpManager;
                prov2 = IpManager.buildProvisioningConfiguration().withPreDhcpAction().withApfCapabilities(WifiStateMachine.this.mWifiNative.getApfCapabilities()).withoutIpReachabilityMonitor().build();
            }
            String ssid = WifiStateMachine.this.getCurrentWifiConfiguration().getPrintableSsid();
            DhcpResults record = (DhcpResults) WifiStateMachine.mDhcpResultMap.get(ssid);
            WifiStateMachine.this.logd("IP recover: get DhcpResult for ssid = " + ssid + ", record = " + record);
            WifiStateMachine.this.mIpManager.updatePastSuccessedDhcpResult(record);
            WifiStateMachine.this.mIpManager.startProvisioning(prov2);
            WifiStateMachine.this.obtainingIpWatchdogCount++;
            WifiStateMachine.this.logd("Start Dhcp Watchdog " + WifiStateMachine.this.obtainingIpWatchdogCount);
            WifiStateMachine.this.getWifiLinkLayerStats(true);
            WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.this.obtainMessage(WifiStateMachine.CMD_OBTAINING_IP_ADDRESS_WATCHDOG_TIMER, WifiStateMachine.this.obtainingIpWatchdogCount, 0), 40000L);
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, this);
            switch (message.what) {
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
                        WifiStateMachine.this.logd("ObtainingIpAddress: Watchdog Triggered, count=" + WifiStateMachine.this.obtainingIpWatchdogCount);
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
                case WifiMonitor.NETWORK_DISCONNECTION_EVENT:
                    WifiStateMachine.this.reportConnectionAttemptEnd(6, 1);
                    return false;
                default:
                    return false;
            }
        }
    }

    private void sendConnectedState() {
        WifiConfiguration config = getCurrentWifiConfiguration();
        if (this.mWifiConfigManager.isLastSelectedConfiguration(config)) {
            boolean prompt = this.mWifiConfigManager.checkConfigOverridePermission(config.lastConnectUid);
            if (DBG) {
                log("Network selected by UID " + config.lastConnectUid + " prompt=" + prompt);
            }
            if (prompt) {
                if (DBG) {
                    log("explictlySelected acceptUnvalidated=" + config.noInternetAccessExpected);
                }
                this.mNetworkAgent.explicitlySelected(config.noInternetAccessExpected);
            }
        }
        setNetworkDetailedState(NetworkInfo.DetailedState.CONNECTED);
        this.mWifiConfigManager.updateStatus(this.mLastNetworkId, NetworkInfo.DetailedState.CONNECTED);
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
            WifiStateMachine.this.roamWatchdogCount++;
            WifiStateMachine.this.logd("Start Roam Watchdog " + WifiStateMachine.this.roamWatchdogCount);
            WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.this.obtainMessage(WifiStateMachine.CMD_ROAM_WATCHDOG_TIMER, WifiStateMachine.this.roamWatchdogCount, 0), 15000L);
            this.mAssociated = false;
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, this);
            switch (message.what) {
                case WifiStateMachine.CMD_START_SCAN:
                    WifiStateMachine.this.deferMessage(message);
                    return true;
                case WifiStateMachine.CMD_SET_OPERATIONAL_MODE:
                    if (message.arg1 != 1) {
                        WifiStateMachine.this.deferMessage(message);
                    }
                    return true;
                case WifiStateMachine.CMD_ROAM_WATCHDOG_TIMER:
                    if (WifiStateMachine.this.roamWatchdogCount == message.arg1) {
                        if (WifiStateMachine.DBG) {
                            WifiStateMachine.this.log("roaming watchdog! -> disconnect");
                        }
                        WifiStateMachine.this.mWifiMetrics.endConnectionEvent(9, 1);
                        WifiStateMachine.this.mRoamFailCount++;
                        WifiStateMachine.this.handleNetworkDisconnect();
                        WifiStateMachine.this.mWifiNative.disconnect();
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectedState);
                    }
                    return true;
                case WifiStateMachine.CMD_IP_CONFIGURATION_LOST:
                    WifiConfiguration config = WifiStateMachine.this.getCurrentWifiConfiguration();
                    if (config != null) {
                        WifiStateMachine.this.mWifiLogger.captureBugReportData(3);
                        WifiStateMachine.this.mWifiConfigManager.noteRoamingFailure(config, WifiConfiguration.ROAMING_FAILURE_IP_CONFIG);
                    }
                    return false;
                case WifiStateMachine.CMD_UNWANTED_NETWORK:
                    if (WifiStateMachine.DBG) {
                        WifiStateMachine.this.log("Roaming and CS doesnt want the network -> ignore");
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
                        if (WifiStateMachine.this.mWifiConnectivityManager != null) {
                            WifiStateMachine.this.mWifiConnectivityManager.trackBssid(WifiStateMachine.this.mLastBssid, true);
                        }
                        WifiStateMachine.this.sendNetworkStateChangeBroadcast(WifiStateMachine.this.mLastBssid);
                        WifiStateMachine.this.reportConnectionAttemptEnd(1, 1);
                        WifiStateMachine.this.clearCurrentConfigBSSID("RoamingCompleted");
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mConnectedState);
                    } else {
                        WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DISCARD;
                    }
                    return true;
                case WifiMonitor.NETWORK_DISCONNECTION_EVENT:
                    String bssid = (String) message.obj;
                    String target = WifiStateMachine.this.mTargetRoamBSSID != null ? WifiStateMachine.this.mTargetRoamBSSID : "";
                    WifiStateMachine.this.log("NETWORK_DISCONNECTION_EVENT in roaming state BSSID=" + bssid + " target=" + target);
                    if (bssid != null && bssid.equals(WifiStateMachine.this.mTargetRoamBSSID)) {
                        WifiStateMachine.this.handleNetworkDisconnect();
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectedState);
                    }
                    return true;
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
                        if (stateChangeResult.wifiSsid != null && !stateChangeResult.wifiSsid.toString().isEmpty()) {
                            WifiStateMachine.this.mWifiInfo.setSSID(stateChangeResult.wifiSsid);
                        }
                    }
                    return true;
                case WifiMonitor.SSID_TEMP_DISABLED:
                    WifiStateMachine.this.logd("SSID_TEMP_DISABLED nid=" + Integer.toString(WifiStateMachine.this.mLastNetworkId) + " id=" + Integer.toString(message.arg1) + " isRoaming=" + WifiStateMachine.this.isRoaming() + " roam=" + WifiStateMachine.this.mAutoRoaming);
                    if (message.arg1 == WifiStateMachine.this.mLastNetworkId) {
                        WifiConfiguration config2 = WifiStateMachine.this.getCurrentWifiConfiguration();
                        if (config2 != null) {
                            WifiStateMachine.this.mWifiLogger.captureBugReportData(3);
                            WifiStateMachine.this.mWifiConfigManager.noteRoamingFailure(config2, WifiConfiguration.ROAMING_FAILURE_AUTH_FAILURE);
                        }
                        WifiStateMachine.this.handleNetworkDisconnect();
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectingState);
                    }
                    return false;
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

        public void enter() throws Throwable {
            WifiStateMachine.this.updateDefaultRouteMacAddress(1000);
            if (WifiStateMachine.DBG) {
                WifiStateMachine.this.log("Enter ConnectedState  mScreenOn=" + WifiStateMachine.this.mScreenOn);
            }
            if (WifiStateMachine.this.mWifiConnectivityManager != null) {
                WifiStateMachine.this.mWifiConnectivityManager.handleConnectionStateChanged(1);
            }
            if (WifiStateMachine.this.mMtkCtpppoe) {
                Log.d(WifiStateMachine.TAG, "Enter ConnectedState, mPppoeInfo.status:" + WifiStateMachine.this.mPppoeInfo.status);
                if (WifiStateMachine.this.mPppoeInfo.status == PPPOEInfo.Status.ONLINE) {
                    WifiStateMachine.this.sendMessageDelayed(2, 500L);
                }
            }
            WifiStateMachine.this.registerConnected();
            WifiStateMachine.this.lastConnectAttemptTimestamp = 0L;
            WifiStateMachine.this.targetWificonfiguration = null;
            WifiStateMachine.this.linkDebouncing = false;
            WifiStateMachine.this.mAutoRoaming = false;
            if (WifiStateMachine.this.testNetworkDisconnect) {
                WifiStateMachine.this.testNetworkDisconnectCounter++;
                WifiStateMachine.this.logd("ConnectedState Enter start disconnect test " + WifiStateMachine.this.testNetworkDisconnectCounter);
                WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.this.obtainMessage(WifiStateMachine.CMD_TEST_NETWORK_DISCONNECT, WifiStateMachine.this.testNetworkDisconnectCounter, 0), 15000L);
            }
            WifiStateMachine.this.mWifiConfigManager.enableAllNetworks();
            WifiStateMachine.this.mLastDriverRoamAttempt = 0L;
            WifiStateMachine.this.mTargetNetworkId = -1;
            WifiStateMachine.this.mWifiLastResortWatchdog.connectedStateTransition(true);
        }

        public boolean processMessage(Message message) {
            WifiConfiguration config;
            WifiStateMachine.this.logStateAndMessage(message, this);
            switch (message.what) {
                case 2:
                    Log.d(WifiStateMachine.TAG, "Update DNS for pppoe!");
                    Collection<InetAddress> dnses = WifiStateMachine.this.mPppoeLinkProperties.getDnsServers();
                    ArrayList<String> pppoeDnses = new ArrayList<>();
                    for (InetAddress dns : dnses) {
                        pppoeDnses.add(dns.getHostAddress());
                    }
                    for (int i = 0; i < pppoeDnses.size(); i++) {
                        Log.d(WifiStateMachine.TAG, "Set net.dns" + (i + 1) + " to " + pppoeDnses.get(i));
                        WifiStateMachine.this.mPropertyService.set("net.dns" + (i + 1), pppoeDnses.get(i));
                    }
                    return true;
                case WifiStateMachine.CMD_TEST_NETWORK_DISCONNECT:
                    if (message.arg1 != WifiStateMachine.this.testNetworkDisconnectCounter) {
                        return true;
                    }
                    WifiStateMachine.this.mWifiNative.disconnect();
                    if (!WifiStateMachine.this.hasCustomizedAutoConnect()) {
                        return true;
                    }
                    WifiStateMachine.this.mDisconnectOperation = true;
                    return true;
                case WifiStateMachine.CMD_UNWANTED_NETWORK:
                    if (message.arg1 == 0) {
                        WifiStateMachine.this.mWifiConfigManager.handleBadNetworkDisconnectReport(WifiStateMachine.this.mLastNetworkId, WifiStateMachine.this.mWifiInfo);
                        WifiStateMachine.this.mWifiNative.disconnect();
                        if (WifiStateMachine.this.hasCustomizedAutoConnect()) {
                            WifiStateMachine.this.mDisconnectOperation = true;
                        }
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectingState);
                        return true;
                    }
                    if (message.arg1 != 2 && message.arg1 != 1) {
                        return true;
                    }
                    Log.d(WifiStateMachine.TAG, message.arg1 == 2 ? "NETWORK_STATUS_UNWANTED_DISABLE_AUTOJOIN" : "NETWORK_STATUS_UNWANTED_VALIDATION_FAILED");
                    if (WifiStateMachine.this.hasCustomizedAutoConnect()) {
                        WifiStateMachine.this.log("Skip unwanted operation because of customization!");
                        return true;
                    }
                    WifiConfiguration config2 = WifiStateMachine.this.getCurrentWifiConfiguration();
                    if (config2 == null) {
                        return true;
                    }
                    if (message.arg1 == 2) {
                        config2.validatedInternetAccess = false;
                        if (WifiStateMachine.this.mWifiConfigManager.isLastSelectedConfiguration(config2)) {
                            WifiStateMachine.this.mWifiConfigManager.setAndEnableLastSelectedConfiguration(-1);
                        }
                        WifiStateMachine.this.mWifiConfigManager.updateNetworkSelectionStatus(config2, 8);
                    }
                    config2.numNoInternetAccessReports++;
                    WifiStateMachine.this.mWifiConfigManager.writeKnownNetworkHistory();
                    return true;
                case WifiStateMachine.CMD_AUTO_ROAM:
                    WifiStateMachine.this.mLastDriverRoamAttempt = 0L;
                    if (WifiStateMachine.this.hasCustomizedAutoConnect() && !WifiStateMachine.this.mWifiFwkExt.shouldAutoConnect()) {
                        Log.d(WifiStateMachine.TAG, "Skip CMD_AUTO_ROAM for customization!");
                        return true;
                    }
                    ScanResult candidate = (ScanResult) message.obj;
                    String bssid = WifiLastResortWatchdog.BSSID_ANY;
                    if (candidate != null) {
                        bssid = candidate.BSSID;
                    }
                    int netId = message.arg1;
                    if (netId == -1) {
                        WifiStateMachine.this.loge("AUTO_ROAM and no config, bail out...");
                        return true;
                    }
                    WifiConfiguration config3 = WifiStateMachine.this.mWifiConfigManager.getWifiConfiguration(netId);
                    WifiStateMachine.this.logd("CMD_AUTO_ROAM sup state " + WifiStateMachine.this.mSupplicantStateTracker.getSupplicantStateName() + " my state " + WifiStateMachine.this.getCurrentState().getName() + " nid=" + Integer.toString(netId) + " config " + config3.configKey() + " roam=" + Integer.toString(message.arg2) + " to " + bssid + " targetRoamBSSID " + WifiStateMachine.this.mTargetRoamBSSID);
                    WifiStateMachine.this.setTargetBssid(config3, bssid);
                    WifiStateMachine.this.mTargetNetworkId = netId;
                    WifiConfiguration currentConfig = WifiStateMachine.this.getCurrentWifiConfiguration();
                    if (currentConfig == null || !currentConfig.isLinked(config3)) {
                        WifiStateMachine.this.mWifiMetrics.startConnectionEvent(config3, WifiStateMachine.this.mTargetRoamBSSID, 3);
                    } else {
                        WifiStateMachine.this.mWifiMetrics.startConnectionEvent(config3, WifiStateMachine.this.mTargetRoamBSSID, 2);
                    }
                    if (WifiStateMachine.this.deferForUserInput(message, netId, false)) {
                        WifiStateMachine.this.reportConnectionAttemptEnd(5, 1);
                        return true;
                    }
                    if (WifiStateMachine.this.mWifiConfigManager.getWifiConfiguration(netId).userApproved == 2) {
                        WifiStateMachine.this.replyToMessage(message, 151554, 9);
                        WifiStateMachine.this.reportConnectionAttemptEnd(5, 1);
                        return true;
                    }
                    boolean ret = false;
                    if (WifiStateMachine.this.mLastNetworkId != netId) {
                        boolean tmpResult = WifiStateMachine.this.hasCustomizedAutoConnect() ? WifiStateMachine.this.mWifiConfigManager.enableNetwork(config3, true, -1) : WifiStateMachine.this.mWifiConfigManager.selectNetwork(config3, false, -1);
                        if (tmpResult && WifiStateMachine.this.mWifiConfigManager.selectNetwork(config3, false, -1) && WifiStateMachine.this.mWifiNative.reconnect()) {
                            ret = true;
                        }
                    } else {
                        ret = WifiStateMachine.this.mWifiNative.reassociate();
                    }
                    if (ret) {
                        WifiStateMachine.this.lastConnectAttemptTimestamp = System.currentTimeMillis();
                        WifiStateMachine.this.targetWificonfiguration = WifiStateMachine.this.mWifiConfigManager.getWifiConfiguration(netId);
                        WifiStateMachine.this.mAutoRoaming = true;
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mRoamingState);
                        return true;
                    }
                    WifiStateMachine.this.loge("Failed to connect config: " + config3 + " netId: " + netId);
                    WifiStateMachine.this.replyToMessage(message, 151554, 0);
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_FAIL;
                    WifiStateMachine.this.reportConnectionAttemptEnd(5, 1);
                    return true;
                case WifiStateMachine.CMD_ASSOCIATED_BSSID:
                    if (WifiStateMachine.this.mLastBssid == null || !WifiStateMachine.this.mLastBssid.equals(message.obj)) {
                        WifiStateMachine.this.mLastDriverRoamAttempt = System.currentTimeMillis();
                        return false;
                    }
                    WifiStateMachine.this.log("bssid is the same, it isn't roaming");
                    return false;
                case WifiStateMachine.CMD_NETWORK_STATUS:
                    if (message.arg1 != 1 || (config = WifiStateMachine.this.getCurrentWifiConfiguration()) == null) {
                        return true;
                    }
                    config.numNoInternetAccessReports = 0;
                    config.validatedInternetAccess = true;
                    WifiStateMachine.this.mWifiConfigManager.writeKnownNetworkHistory();
                    return true;
                case WifiStateMachine.CMD_ACCEPT_UNVALIDATED:
                    boolean accept = message.arg1 != 0;
                    WifiConfiguration config4 = WifiStateMachine.this.getCurrentWifiConfiguration();
                    if (config4 == null) {
                        return true;
                    }
                    config4.noInternetAccessExpected = accept;
                    WifiStateMachine.this.mWifiConfigManager.writeKnownNetworkHistory();
                    return true;
                case WifiStateMachine.CMD_UPDATE_ASSOCIATED_SCAN_PERMISSION:
                    WifiStateMachine.this.updateAssociatedScanPermission();
                    return true;
                case WifiStateMachine.CMD_START_IP_PACKET_OFFLOAD:
                    int slot = message.arg1;
                    int intervalSeconds = message.arg2;
                    KeepalivePacketData pkt = (KeepalivePacketData) message.obj;
                    try {
                        InetAddress gateway = RouteInfo.selectBestRoute(WifiStateMachine.this.mLinkProperties.getRoutes(), pkt.dstAddress).getGateway();
                        String dstMacStr = WifiStateMachine.this.macAddressFromRoute(gateway.getHostAddress());
                        byte[] dstMac = WifiStateMachine.this.macAddressFromString(dstMacStr);
                        pkt.dstMac = dstMac;
                        int result = WifiStateMachine.this.startWifiIPPacketOffload(slot, pkt, intervalSeconds);
                        WifiStateMachine.this.mNetworkAgent.onPacketKeepaliveEvent(slot, result);
                        return true;
                    } catch (IllegalArgumentException | NullPointerException e) {
                        WifiStateMachine.this.loge("Can't find MAC address for next hop to " + pkt.dstAddress);
                        WifiStateMachine.this.mNetworkAgent.onPacketKeepaliveEvent(slot, -21);
                        return true;
                    }
                case WifiMonitor.NETWORK_DISCONNECTION_EVENT:
                    long lastRoam = 0;
                    WifiStateMachine.this.reportConnectionAttemptEnd(6, 1);
                    if (WifiStateMachine.this.mLastDriverRoamAttempt != 0) {
                        lastRoam = System.currentTimeMillis() - WifiStateMachine.this.mLastDriverRoamAttempt;
                        WifiStateMachine.this.mLastDriverRoamAttempt = 0L;
                    }
                    if (WifiStateMachine.unexpectedDisconnectedReason(message.arg2)) {
                        WifiStateMachine.this.mWifiLogger.captureBugReportData(5);
                    }
                    WifiConfiguration config5 = WifiStateMachine.this.getCurrentWifiConfiguration();
                    if (!WifiStateMachine.this.mScreenOn || WifiStateMachine.this.linkDebouncing || config5 == null || !config5.getNetworkSelectionStatus().isNetworkEnabled() || WifiStateMachine.this.mWifiConfigManager.isLastSelectedConfiguration(config5) || (((message.arg2 == 3 || message.arg2 == 100) && (lastRoam <= 0 || lastRoam >= 2000)) || (((!ScanResult.is24GHz(WifiStateMachine.this.mWifiInfo.getFrequency()) || WifiStateMachine.this.mWifiInfo.getRssi() <= -73) && (!ScanResult.is5GHz(WifiStateMachine.this.mWifiInfo.getFrequency()) || WifiStateMachine.this.mWifiInfo.getRssi() <= WifiStateMachine.this.mWifiConfigManager.mThresholdQualifiedRssi5.get())) || WifiStateMachine.this.hasCustomizedAutoConnect()))) {
                        if (!WifiStateMachine.DBG) {
                            return true;
                        }
                        WifiStateMachine.this.log("NETWORK_DISCONNECTION_EVENT in connected state BSSID=" + WifiStateMachine.this.mWifiInfo.getBSSID() + " RSSI=" + WifiStateMachine.this.mWifiInfo.getRssi() + " freq=" + WifiStateMachine.this.mWifiInfo.getFrequency() + " was debouncing=" + WifiStateMachine.this.linkDebouncing + " reason=" + message.arg2 + " Network Selection Status=" + (config5 == null ? "Unavailable" : config5.getNetworkSelectionStatus().getNetworkStatusString()));
                        return true;
                    }
                    WifiStateMachine.this.startScanForConfiguration(WifiStateMachine.this.getCurrentWifiConfiguration());
                    WifiStateMachine.this.linkDebouncing = true;
                    WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.this.obtainMessage(WifiStateMachine.CMD_DELAYED_NETWORK_DISCONNECT, 0, WifiStateMachine.this.mLastNetworkId), 4000L);
                    if (!WifiStateMachine.DBG) {
                        return true;
                    }
                    WifiStateMachine.this.log("NETWORK_DISCONNECTION_EVENT in connected state BSSID=" + WifiStateMachine.this.mWifiInfo.getBSSID() + " RSSI=" + WifiStateMachine.this.mWifiInfo.getRssi() + " freq=" + WifiStateMachine.this.mWifiInfo.getFrequency() + " reason=" + message.arg2 + " -> debounce");
                    return true;
                default:
                    return false;
            }
        }

        public void exit() {
            WifiStateMachine.this.logd("WifiStateMachine: Leaving Connected state");
            if (WifiStateMachine.this.mWifiConnectivityManager != null) {
                WifiStateMachine.this.mWifiConnectivityManager.handleConnectionStateChanged(3);
            }
            WifiStateMachine.this.mLastDriverRoamAttempt = 0L;
            WifiStateMachine.this.mWhiteListedSsids = null;
            WifiStateMachine.this.mWifiLastResortWatchdog.connectedStateTransition(false);
        }
    }

    class DisconnectingState extends State {
        DisconnectingState() {
        }

        public void enter() {
            if (WifiStateMachine.DBG) {
                WifiStateMachine.this.logd(" Enter DisconnectingState State screenOn=" + WifiStateMachine.this.mScreenOn);
            }
            WifiStateMachine.this.disconnectingWatchdogCount++;
            WifiStateMachine.this.logd("Start Disconnecting Watchdog " + WifiStateMachine.this.disconnectingWatchdogCount);
            WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.this.obtainMessage(WifiStateMachine.CMD_DISCONNECTING_WATCHDOG_TIMER, WifiStateMachine.this.disconnectingWatchdogCount, 0), 5000L);
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, this);
            switch (message.what) {
                case WifiStateMachine.CMD_START_SCAN:
                    WifiStateMachine.this.deferMessage(message);
                    return true;
                case WifiStateMachine.CMD_SET_OPERATIONAL_MODE:
                    if (message.arg1 != 1) {
                        WifiStateMachine.this.deferMessage(message);
                    }
                    return true;
                case WifiStateMachine.CMD_DISCONNECTING_WATCHDOG_TIMER:
                    if (WifiStateMachine.this.disconnectingWatchdogCount == message.arg1) {
                        if (WifiStateMachine.DBG) {
                            WifiStateMachine.this.log("disconnecting watchdog! -> disconnect");
                        }
                        WifiStateMachine.this.handleNetworkDisconnect();
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectedState);
                    }
                    return true;
                case WifiStateMachine.M_CMD_UPDATE_SCAN_STRATEGY:
                    WifiStateMachine.this.deferMessage(message);
                    return true;
                case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT:
                    WifiStateMachine.this.deferMessage(message);
                    WifiStateMachine.this.handleNetworkDisconnect();
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectedState);
                    return true;
                default:
                    return false;
            }
        }
    }

    class DisconnectedState extends State {
        DisconnectedState() {
        }

        public void enter() {
            if (WifiStateMachine.DBG) {
                WifiStateMachine.this.loge(getName() + "\n");
            }
            if (WifiStateMachine.this.mTemporarilyDisconnectWifi) {
                WifiStateMachine.this.mWifiP2pChannel.sendMessage(WifiP2pServiceImpl.DISCONNECT_WIFI_RESPONSE);
                return;
            }
            if (WifiStateMachine.this.isTemporarilyDontReconnectWifi()) {
                return;
            }
            if (WifiStateMachine.DBG) {
                WifiStateMachine.this.logd(" Enter DisconnectedState screenOn=" + WifiStateMachine.this.mScreenOn);
            }
            WifiStateMachine.this.mAutoRoaming = false;
            if (WifiStateMachine.this.mWifiConnectivityManager != null) {
                WifiStateMachine.this.mWifiConnectivityManager.handleConnectionStateChanged(2);
            }
            if (WifiStateMachine.this.mNoNetworksPeriodicScan != 0 && !WifiStateMachine.this.mP2pConnected.get() && WifiStateMachine.this.mWifiConfigManager.getSavedNetworks().size() == 0) {
                WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.this.obtainMessage(WifiStateMachine.CMD_NO_NETWORKS_PERIODIC_SCAN, WifiStateMachine.this.mPeriodicScanToken++, 0), WifiStateMachine.this.mNoNetworksPeriodicScan);
            }
            WifiStateMachine.this.mDisconnectedTimeStamp = System.currentTimeMillis();
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, this);
            switch (message.what) {
                case WifiStateMachine.CMD_REMOVE_NETWORK:
                case WifiStateMachine.CMD_REMOVE_APP_CONFIGURATIONS:
                case WifiStateMachine.CMD_REMOVE_USER_CONFIGURATIONS:
                case 151556:
                    WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.this.obtainMessage(WifiStateMachine.CMD_NO_NETWORKS_PERIODIC_SCAN, WifiStateMachine.this.mPeriodicScanToken++, 0), WifiStateMachine.this.mNoNetworksPeriodicScan);
                    break;
                case WifiStateMachine.CMD_START_SCAN:
                    if (!WifiStateMachine.this.checkOrDeferScanAllowed(message)) {
                        WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_REFUSED;
                    }
                    break;
                case WifiStateMachine.CMD_SET_OPERATIONAL_MODE:
                    if (message.arg1 != 1) {
                        WifiStateMachine.this.mOperationalMode = message.arg1;
                        WifiStateMachine.this.mWifiConfigManager.disableAllNetworksNative();
                        if (WifiStateMachine.this.mOperationalMode == 3) {
                            WifiStateMachine.this.mWifiP2pChannel.sendMessage(WifiStateMachine.CMD_DISABLE_P2P_REQ);
                            WifiStateMachine.this.setWifiState(1);
                        }
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mScanModeState);
                    }
                    WifiStateMachine.this.mWifiConfigManager.setAndEnableLastSelectedConfiguration(-1);
                    break;
                case WifiStateMachine.CMD_RECONNECT:
                case WifiStateMachine.CMD_REASSOCIATE:
                    if (!WifiStateMachine.this.mTemporarilyDisconnectWifi) {
                    }
                    break;
                case WifiStateMachine.CMD_NO_NETWORKS_PERIODIC_SCAN:
                    if (!WifiStateMachine.this.mP2pConnected.get() && !WifiStateMachine.this.isTemporarilyDontReconnectWifi() && WifiStateMachine.this.mNoNetworksPeriodicScan != 0 && message.arg1 == WifiStateMachine.this.mPeriodicScanToken && WifiStateMachine.this.mWifiConfigManager.getSavedNetworks().size() == 0) {
                        WifiStateMachine.this.startScan(-1, -1, null, WifiStateMachine.WIFI_WORK_SOURCE);
                        WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.this.obtainMessage(WifiStateMachine.CMD_NO_NETWORKS_PERIODIC_SCAN, WifiStateMachine.this.mPeriodicScanToken++, 0), WifiStateMachine.this.mNoNetworksPeriodicScan);
                        break;
                    }
                    break;
                case WifiStateMachine.CMD_SCREEN_STATE_CHANGED:
                    WifiStateMachine.this.handleScreenStateChanged(message.arg1 != 0);
                    break;
                case WifiStateMachine.M_CMD_UPDATE_BGSCAN:
                    if (WifiStateMachine.this.isTemporarilyDontReconnectWifi()) {
                        Log.d(WifiStateMachine.TAG, "isNetworksDisabledDuringConnect:" + WifiStateMachine.this.mSupplicantStateTracker.isNetworksDisabledDuringConnect() + ", mConnectNetwork:" + WifiStateMachine.this.mConnectNetwork);
                        if (WifiStateMachine.this.mConnectNetwork) {
                            WifiStateMachine.this.mConnectNetwork = false;
                        } else if (!WifiStateMachine.this.mSupplicantStateTracker.isNetworksDisabledDuringConnect()) {
                            Log.d(WifiStateMachine.TAG, "Disable supplicant auto scan!");
                            WifiStateMachine.this.mWifiNative.disconnect();
                        }
                    } else if (!WifiStateMachine.this.mTemporarilyDisconnectWifi && WifiStateMachine.this.hasCustomizedAutoConnect()) {
                        WifiStateMachine.this.mWifiNative.reconnect();
                    }
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectedState);
                    break;
                case WifiStateMachine.M_CMD_UPDATE_SCAN_STRATEGY:
                    if (WifiStateMachine.this.isTemporarilyDontReconnectWifi()) {
                        if (!WifiStateMachine.this.mConnectNetwork) {
                            Log.d(WifiStateMachine.TAG, "Disable supplicant auto scan!");
                            WifiStateMachine.this.mWifiNative.disconnect();
                        } else {
                            WifiStateMachine.this.mConnectNetwork = false;
                        }
                    }
                    break;
                case WifiP2pServiceImpl.P2P_CONNECTION_CHANGED:
                    NetworkInfo info = (NetworkInfo) message.obj;
                    WifiStateMachine.this.mP2pConnected.set(info.isConnected());
                    if (WifiStateMachine.this.mP2pConnected.get()) {
                        int defaultInterval = WifiStateMachine.this.mContext.getResources().getInteger(R.integer.config_bg_current_drain_exempted_types);
                        long scanIntervalMs = WifiStateMachine.this.mFacade.getLongSetting(WifiStateMachine.this.mContext, "wifi_scan_interval_p2p_connected_ms", defaultInterval);
                        WifiStateMachine.this.mWifiNative.setScanInterval(((int) scanIntervalMs) / 1000);
                    } else if (WifiStateMachine.this.mWifiConfigManager.getSavedNetworks().size() == 0) {
                        if (WifiStateMachine.DBG) {
                            WifiStateMachine.this.log("Turn on scanning after p2p disconnected");
                        }
                        WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.this.obtainMessage(WifiStateMachine.CMD_NO_NETWORKS_PERIODIC_SCAN, WifiStateMachine.this.mPeriodicScanToken++, 0), WifiStateMachine.this.mNoNetworksPeriodicScan);
                    }
                    break;
                case WifiMonitor.NETWORK_DISCONNECTION_EVENT:
                    break;
                case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT:
                    StateChangeResult stateChangeResult = (StateChangeResult) message.obj;
                    if (WifiStateMachine.DBG) {
                        WifiStateMachine.this.logd("SUPPLICANT_STATE_CHANGE_EVENT state=" + stateChangeResult.state + " -> state= " + WifiInfo.getDetailedStateOf(stateChangeResult.state) + " debouncing=" + WifiStateMachine.this.linkDebouncing);
                    }
                    WifiStateMachine.this.setNetworkDetailedState(WifiInfo.getDetailedStateOf(stateChangeResult.state));
                    break;
            }
            return true;
        }

        public void exit() {
            if (WifiStateMachine.this.mWifiConnectivityManager == null) {
                return;
            }
            WifiStateMachine.this.mWifiConnectivityManager.handleConnectionStateChanged(3);
        }
    }

    class WpsRunningState extends State {
        private Message mSourceMessage;

        WpsRunningState() {
        }

        public void enter() {
            if (WifiStateMachine.DBG) {
                WifiStateMachine.this.loge(getName() + "\n");
            }
            this.mSourceMessage = Message.obtain(WifiStateMachine.this.getCurrentMessage());
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, this);
            switch (message.what) {
                case WifiStateMachine.CMD_STOP_DRIVER:
                case WifiStateMachine.CMD_ENABLE_NETWORK:
                case WifiStateMachine.CMD_ENABLE_ALL_NETWORKS:
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
                    return true;
                case WifiMonitor.AUTHENTICATION_FAILURE_EVENT:
                    if (WifiStateMachine.DBG) {
                        WifiStateMachine.this.log("Ignore auth failure during WPS connection");
                    }
                    return true;
                case WifiMonitor.WPS_SUCCESS_EVENT:
                    WifiStateMachine.this.mSupplicantStateTracker.sendMessage(WifiMonitor.WPS_SUCCESS_EVENT);
                    WifiStateMachine.this.mConnectNetwork = true;
                    return true;
                case WifiMonitor.WPS_FAIL_EVENT:
                    if (message.arg1 != 0 || message.arg2 != 0) {
                        WifiStateMachine.this.replyToMessage(this.mSourceMessage, 151564, message.arg1);
                        this.mSourceMessage.recycle();
                        this.mSourceMessage = null;
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectedState);
                    } else if (WifiStateMachine.DBG) {
                        WifiStateMachine.this.log("Ignore unspecified fail event during WPS connection");
                    }
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
                    if (WifiStateMachine.DBG) {
                        WifiStateMachine.this.log("Ignore Assoc reject event during WPS Connection");
                    }
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
                    return false;
            }
        }

        public void exit() {
            WifiStateMachine.this.mWifiConfigManager.enableAllNetworks();
            WifiStateMachine.this.mWifiConfigManager.loadConfiguredNetworks();
        }
    }

    class SoftApState extends State {
        private SoftApManager mSoftApManager;

        SoftApState() {
        }

        private class SoftApListener implements SoftApManager.Listener {
            SoftApListener(SoftApState this$1, SoftApListener softApListener) {
                this();
            }

            private SoftApListener() {
            }

            @Override
            public void onStateChanged(int state, int reason) {
                if (state == 11) {
                    WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_AP_STOPPED);
                } else if (state == 14) {
                    WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_START_AP_FAILURE);
                }
                WifiStateMachine.this.setWifiApState(state, reason);
            }
        }

        public void enter() {
            SoftApListener softApListener = null;
            if (WifiStateMachine.DBG) {
                WifiStateMachine.this.loge(getName() + "\n");
            }
            Message message = WifiStateMachine.this.getCurrentMessage();
            if (message.what == WifiStateMachine.CMD_START_AP) {
                WifiConfiguration config = (WifiConfiguration) message.obj;
                if (config == null) {
                    config = WifiStateMachine.this.mWifiApConfigStore.getApConfiguration();
                } else {
                    WifiStateMachine.this.mWifiApConfigStore.setApConfiguration(config);
                }
                WifiStateMachine.this.checkAndSetConnectivityInstance();
                this.mSoftApManager = WifiStateMachine.this.mFacade.makeSoftApManager(WifiStateMachine.this.mContext, WifiStateMachine.this.getHandler().getLooper(), WifiStateMachine.this.mWifiNative, WifiStateMachine.this.mNwService, WifiStateMachine.this.mCm, WifiStateMachine.this.mCountryCode.getCurrentCountryCode(), WifiStateMachine.this.mWifiApConfigStore.getAllowed2GChannel(), new SoftApListener(this, softApListener));
                this.mSoftApManager.start(config);
                return;
            }
            throw new RuntimeException("Illegal transition to SoftApState: " + message);
        }

        public void exit() {
            this.mSoftApManager.destroy();
            this.mSoftApManager = null;
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, this);
            switch (message.what) {
                case WifiStateMachine.CMD_START_AP:
                    return true;
                case WifiStateMachine.CMD_START_AP_FAILURE:
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mInitialState);
                    return true;
                case WifiStateMachine.CMD_STOP_AP:
                    this.mSoftApManager.stop();
                    return true;
                case WifiStateMachine.CMD_AP_STOPPED:
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mInitialState);
                    return true;
                default:
                    return false;
            }
        }
    }

    private void replyToMessage(Message msg, int what) {
        if (msg.replyTo == null) {
            return;
        }
        Message dstMsg = obtainMessageWithWhatAndArg2(msg, what);
        this.mReplyChannel.replyToMessage(msg, dstMsg);
    }

    private void replyToMessage(Message msg, int what, int arg1) {
        if (msg.replyTo == null) {
            return;
        }
        Message dstMsg = obtainMessageWithWhatAndArg2(msg, what);
        dstMsg.arg1 = arg1;
        this.mReplyChannel.replyToMessage(msg, dstMsg);
    }

    private void replyToMessage(Message msg, int what, Object obj) {
        if (msg.replyTo == null) {
            return;
        }
        Message dstMsg = obtainMessageWithWhatAndArg2(msg, what);
        dstMsg.obj = obj;
        this.mReplyChannel.replyToMessage(msg, dstMsg);
    }

    private Message obtainMessageWithWhatAndArg2(Message srcMsg, int what) {
        Message msg = Message.obtain();
        msg.what = what;
        msg.arg2 = srcMsg.arg2;
        return msg;
    }

    private void broadcastWifiCredentialChanged(int wifiCredentialEventType, WifiConfiguration config) {
        if (config == null || config.preSharedKey == null) {
            return;
        }
        Intent intent = new Intent("android.net.wifi.WIFI_CREDENTIAL_CHANGED");
        intent.putExtra("ssid", config.SSID);
        intent.putExtra("et", wifiCredentialEventType);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT, "android.permission.RECEIVE_WIFI_CREDENTIAL_CHANGE");
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
            index = 1;
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

    private static byte[] concatHex(byte[] array1, byte[] array2) {
        int len = array1.length + array2.length;
        byte[] result = new byte[len];
        int index = 0;
        if (array1.length != 0) {
            for (byte b : array1) {
                result[index] = b;
                index++;
            }
        }
        if (array2.length != 0) {
            for (byte b2 : array2) {
                result[index] = b2;
                index++;
            }
        }
        return result;
    }

    String getGsmSimAuthResponse(String[] requestData, TelephonyManager tm) {
        return getGsmSimAuthResponse(requestData, tm, -1);
    }

    String getGsmSimAuthResponse(String[] requestData, TelephonyManager tm, int netId) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        int length = requestData.length;
        while (true) {
            int i2 = i;
            if (i2 < length) {
                String challenge = requestData[i2];
                if (challenge != null && !challenge.isEmpty()) {
                    logd("RAND = " + challenge);
                    try {
                        byte[] rand = parseHex(challenge);
                        String base64Challenge = Base64.encodeToString(rand, 2);
                        int appType = 2;
                        String tmResponse = getIccAuthentication(2, 128, base64Challenge, tm, netId);
                        if (tmResponse == null) {
                            appType = 1;
                            tmResponse = getIccAuthentication(1, 128, base64Challenge, tm, netId);
                        }
                        logv("Raw Response - " + tmResponse);
                        if (tmResponse == null || tmResponse.length() <= 4) {
                            break;
                        }
                        byte[] result = Base64.decode(tmResponse, 0);
                        logv("Hex Response -" + makeHex(result));
                        String sres = null;
                        String kc = null;
                        if (appType == 2) {
                            int sres_len = result[0];
                            if (sres_len >= result.length) {
                                loge("malfomed response - " + tmResponse);
                                return null;
                            }
                            sres = makeHex(result, 1, sres_len);
                            int kc_offset = sres_len + 1;
                            if (kc_offset >= result.length) {
                                loge("malfomed response - " + tmResponse);
                                return null;
                            }
                            int kc_len = result[kc_offset];
                            if (kc_offset + kc_len > result.length) {
                                loge("malfomed response - " + tmResponse);
                                return null;
                            }
                            kc = makeHex(result, kc_offset + 1, kc_len);
                        } else if (appType == 1) {
                            if (result.length < 12) {
                                loge("malfomed response - " + tmResponse);
                                return null;
                            }
                            sres = makeHex(result, 0, 4);
                            kc = makeHex(result, 4, 8);
                        }
                        sb.append(":").append(kc).append(":").append(sres);
                        logv("kc:" + kc + " sres:" + sres);
                    } catch (NumberFormatException e) {
                        loge("malformed challenge");
                    }
                }
                i = i2 + 1;
            } else {
                return sb.toString();
            }
        }
    }

    void handleGsmAuthRequest(SimAuthRequestData requestData) {
        if (this.targetWificonfiguration == null || this.targetWificonfiguration.networkId == requestData.networkId) {
            logd("id matches targetWifiConfiguration");
            TelephonyManager tm = (TelephonyManager) this.mContext.getSystemService("phone");
            if (tm == null) {
                loge("could not get telephony manager");
                this.mWifiNative.simAuthFailedResponse(requestData.networkId);
                return;
            }
            String response = getGsmSimAuthResponse(requestData.data, tm, requestData.networkId);
            if (response == null) {
                this.mWifiNative.simAuthFailedResponse(requestData.networkId);
                return;
            } else {
                logv("Supplicant Response -" + response);
                this.mWifiNative.simAuthResponse(requestData.networkId, "GSM-AUTH", response);
                return;
            }
        }
        logd("id does not match targetWifiConfiguration");
    }

    void handle3GAuthRequest(SimAuthRequestData requestData) {
        StringBuilder sb = new StringBuilder();
        byte[] rand = null;
        byte[] authn = null;
        String res_type = "UMTS-AUTH";
        if (this.targetWificonfiguration == null || this.targetWificonfiguration.networkId == requestData.networkId) {
            logd("id matches targetWifiConfiguration");
            if (requestData.data.length == 2) {
                try {
                    rand = parseHex(requestData.data[0]);
                    authn = parseHex(requestData.data[1]);
                } catch (NumberFormatException e) {
                    loge("malformed challenge");
                }
            } else {
                loge("malformed challenge");
            }
            String tmResponse = "";
            if (rand != null && authn != null) {
                String base64Challenge = Base64.encodeToString(concatHex(rand, authn), 2);
                TelephonyManager tm = (TelephonyManager) this.mContext.getSystemService("phone");
                if (tm != null) {
                    tmResponse = getIccAuthentication(2, 129, base64Challenge, tm, requestData.networkId);
                    logv("Raw Response - " + tmResponse);
                } else {
                    loge("could not get telephony manager");
                }
            }
            boolean good_response = false;
            if (tmResponse != null && tmResponse.length() > 4) {
                byte[] result = Base64.decode(tmResponse, 0);
                loge("Hex Response - " + makeHex(result));
                byte tag = result[0];
                if (tag == -37) {
                    logv("successful 3G authentication ");
                    int res_len = result[1];
                    String res = makeHex(result, 2, res_len);
                    int ck_len = result[res_len + 2];
                    String ck = makeHex(result, res_len + 3, ck_len);
                    int ik_len = result[res_len + ck_len + 3];
                    String ik = makeHex(result, res_len + ck_len + 4, ik_len);
                    sb.append(":").append(ik).append(":").append(ck).append(":").append(res);
                    logv("ik:" + ik + "ck:" + ck + " res:" + res);
                    good_response = true;
                } else if (tag == -36) {
                    loge("synchronisation failure");
                    int auts_len = result[1];
                    String auts = makeHex(result, 2, auts_len);
                    res_type = "UMTS-AUTS";
                    sb.append(":").append(auts);
                    logv("auts:" + auts);
                    good_response = true;
                } else {
                    loge("bad response - unknown tag = " + ((int) tag));
                }
            } else {
                loge("bad response - " + tmResponse);
            }
            if (good_response) {
                String response = sb.toString();
                logv("Supplicant Response -" + response);
                this.mWifiNative.simAuthResponse(requestData.networkId, res_type, response);
                return;
            }
            this.mWifiNative.umtsAuthFailedResponse(requestData.networkId);
            return;
        }
        logd("id does not match targetWifiConfiguration");
    }

    public void autoConnectToNetwork(int networkId, String bssid) {
        sendMessage(CMD_AUTO_CONNECT, networkId, 0, bssid);
    }

    public void autoRoamToNetwork(int networkId, ScanResult scanResult) {
        sendMessage(CMD_AUTO_ROAM, networkId, 0, scanResult);
    }

    public void enableWifiConnectivityManager(boolean enabled) {
        sendMessage(CMD_ENABLE_WIFI_CONNECTIVITY_MANAGER, enabled ? 1 : 0);
    }

    static boolean unexpectedDisconnectedReason(int reason) {
        return reason == 2 || reason == 6 || reason == 7 || reason == 8 || reason == 9 || reason == 14 || reason == 15 || reason == 16 || reason == 18 || reason == 19 || reason == 23 || reason == 34;
    }

    void updateWifiMetrics() {
        int numSavedNetworks = this.mWifiConfigManager.getConfiguredNetworksSize();
        int numOpenNetworks = 0;
        int numPersonalNetworks = 0;
        int numEnterpriseNetworks = 0;
        int numNetworksAddedByUser = 0;
        int numNetworksAddedByApps = 0;
        for (WifiConfiguration config : this.mWifiConfigManager.getSavedNetworks()) {
            if (config.allowedKeyManagement.get(0)) {
                numOpenNetworks++;
            } else if (config.isEnterprise()) {
                numEnterpriseNetworks++;
            } else {
                numPersonalNetworks++;
            }
            if (config.selfAdded) {
                numNetworksAddedByUser++;
            } else {
                numNetworksAddedByApps++;
            }
        }
        this.mWifiMetrics.setNumSavedNetworks(numSavedNetworks);
        this.mWifiMetrics.setNumOpenNetworks(numOpenNetworks);
        this.mWifiMetrics.setNumPersonalNetworks(numPersonalNetworks);
        this.mWifiMetrics.setNumEnterpriseNetworks(numEnterpriseNetworks);
        this.mWifiMetrics.setNumNetworksAddedByUser(numNetworksAddedByUser);
        this.mWifiMetrics.setNumNetworksAddedByApps(numNetworksAddedByApps);
    }

    private static String getLinkPropertiesSummary(LinkProperties lp) {
        List<String> attributes = new ArrayList<>(6);
        if (lp.hasIPv4Address()) {
            attributes.add("v4");
        }
        if (lp.hasIPv4DefaultRoute()) {
            attributes.add("v4r");
        }
        if (lp.hasIPv4DnsServer()) {
            attributes.add("v4dns");
        }
        if (lp.hasGlobalIPv6Address()) {
            attributes.add("v6");
        }
        if (lp.hasIPv6DefaultRoute()) {
            attributes.add("v6r");
        }
        if (lp.hasIPv6DnsServer()) {
            attributes.add("v6dns");
        }
        return TextUtils.join(" ", attributes);
    }

    private void wnmFrameReceived(WnmData event) {
        Intent intent = new Intent("android.net.wifi.PASSPOINT_WNM_FRAME_RECEIVED");
        intent.addFlags(67108864);
        intent.putExtra("bssid", event.getBssid());
        intent.putExtra("url", event.getUrl());
        if (event.isDeauthEvent()) {
            intent.putExtra("ess", event.isEss());
            intent.putExtra("delay", event.getDelay());
        } else {
            intent.putExtra("method", event.getMethod());
            WifiConfiguration config = getCurrentWifiConfiguration();
            if (config != null && config.FQDN != null) {
                intent.putExtra("match", this.mWifiConfigManager.matchProviderWithCurrentNetwork(config.FQDN));
            }
        }
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    private String getTargetSsid() {
        WifiConfiguration currentConfig = this.mWifiConfigManager.getWifiConfiguration(this.mTargetNetworkId);
        if (currentConfig != null) {
            return currentConfig.SSID;
        }
        return null;
    }

    public boolean syncDoCtiaTestOn(AsyncChannel channel) {
        Message resultMsg = channel.sendMessageSynchronously(M_CMD_DO_CTIA_TEST_ON);
        boolean result = resultMsg.arg1 != -1;
        resultMsg.recycle();
        return result;
    }

    public boolean syncDoCtiaTestOff(AsyncChannel channel) {
        Message resultMsg = channel.sendMessageSynchronously(M_CMD_DO_CTIA_TEST_OFF);
        boolean result = resultMsg.arg1 != -1;
        resultMsg.recycle();
        return result;
    }

    public boolean syncDoCtiaTestRate(AsyncChannel channel, int rate) {
        Message resultMsg = channel.sendMessageSynchronously(M_CMD_DO_CTIA_TEST_RATE, rate);
        boolean result = resultMsg.arg1 != -1;
        resultMsg.recycle();
        return result;
    }

    public boolean syncSetTxPowerEnabled(AsyncChannel channel, boolean enable) {
        Message resultMsg = channel.sendMessageSynchronously(M_CMD_SET_TX_POWER_ENABLED, enable ? 1 : 0);
        boolean result = resultMsg.arg1 != -1;
        resultMsg.recycle();
        return result;
    }

    public boolean syncSetTxPower(AsyncChannel channel, int offset) {
        Message resultMsg = channel.sendMessageSynchronously(M_CMD_SET_TX_POWER, offset);
        boolean result = resultMsg.arg1 != -1;
        resultMsg.recycle();
        return result;
    }

    public PPPOEInfo syncGetPppoeInfo() {
        if (this.mMtkCtpppoe) {
            this.mPppoeInfo.online_time = (System.currentTimeMillis() / 1000) - this.mOnlineStartTime;
            return this.mPppoeInfo;
        }
        return null;
    }

    public int syncGetConnectingNetworkId(AsyncChannel channel) {
        Message resultMsg = channel.sendMessageSynchronously(M_CMD_GET_CONNECTING_NETWORK_ID);
        int result = resultMsg.arg1;
        resultMsg.recycle();
        return result;
    }

    public List<Integer> syncGetDisconnectNetworks() {
        return this.mWifiConfigManager.getDisconnectNetworks();
    }

    public boolean isNetworksDisabledDuringConnect() {
        return (this.mSupplicantStateTracker.isNetworksDisabledDuringConnect() && isExplicitNetworkExist()) || getCurrentState() == this.mWpsRunningState;
    }

    public boolean hasConnectableAp() {
        sendMessage(M_CMD_FLUSH_BSS);
        if (this.mWifiFwkExt != null) {
            return this.mWifiFwkExt.hasConnectableAp();
        }
        return false;
    }

    public void suspendNotification(int type) {
        if (this.mWifiFwkExt == null) {
            return;
        }
        this.mWifiFwkExt.suspendNotification(type);
    }

    private void initializeExtra() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.mtk.stopscan.activated");
        intentFilter.addAction("com.mtk.stopscan.deactivated");
        intentFilter.addAction("android.intent.action.SIM_STATE_CHANGED");
        intentFilter.addAction("com.mediatek.common.wifi.AUTOCONNECT_SETTINGS_CHANGE");
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d(WifiStateMachine.TAG, "onReceive, action:" + action);
                if (action.equals("com.mtk.stopscan.activated")) {
                    WifiStateMachine.this.mStopScanStarted.set(true);
                    WifiStateMachine.this.sendMessage(WifiStateMachine.M_CMD_UPDATE_SCAN_STRATEGY);
                    return;
                }
                if (action.equals("com.mtk.stopscan.deactivated")) {
                    WifiStateMachine.this.mStopScanStarted.set(false);
                    WifiStateMachine.this.sendMessage(WifiStateMachine.M_CMD_UPDATE_SCAN_STRATEGY);
                    return;
                }
                if (action.equals("android.intent.action.SIM_STATE_CHANGED")) {
                    String iccState = intent.getStringExtra("ss");
                    int simSlot = intent.getIntExtra("slot", -1);
                    WifiStateMachine.this.log("iccState:" + iccState + ", simSlot: " + simSlot);
                    if (simSlot == 0 || -1 == simSlot) {
                        WifiStateMachine.this.mSim1IccState = iccState;
                    } else {
                        WifiStateMachine.this.mSim2IccState = iccState;
                    }
                    if (!iccState.equals("LOADED")) {
                        return;
                    }
                    WifiStateMachine.this.sendMessage(WifiStateMachine.M_CMD_ENABLE_EAP_SIM_CONFIG_NETWORK);
                    return;
                }
                if (!action.equals("com.mediatek.common.wifi.AUTOCONNECT_SETTINGS_CHANGE")) {
                    return;
                }
                WifiStateMachine.this.sendMessage(WifiStateMachine.M_CMD_UPDATE_SETTINGS);
            }
        };
        this.mContext.registerReceiver(receiver, intentFilter);
        if (this.mWifiFwkExt != null) {
            this.mMtkCtpppoe = this.mWifiFwkExt.isPppoeSupported();
        }
        if (!this.mMtkCtpppoe) {
            return;
        }
        this.mPppoeInfo = new PPPOEInfo();
        this.mPppoeLinkProperties = new LinkProperties();
    }

    private void sendPppoeCompletedBroadcast(String status, int errorCode) {
        Intent intent = new Intent("android.net.wifi.PPPOE_COMPLETED_ACTION");
        intent.addFlags(67108864);
        intent.putExtra("pppoe_result_status", status);
        if (status.equals("FAILURE")) {
            intent.putExtra("pppoe_result_error_code", Integer.toString(errorCode));
        }
        Log.d(TAG, "sendPppoeCompletedBroadcast, status:" + status + ", errorCode:" + errorCode);
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void sendPppoeStateChangedBroadcast(String state) {
        Intent intent = new Intent("android.net.wifi.PPPOE_STATE_CHANGED");
        intent.addFlags(67108864);
        intent.putExtra("pppoe_state", state);
        Log.d(TAG, "sendPppoeStateChangedBroadcast, state:" + state);
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    public String getWifiStatus(AsyncChannel channel) {
        Message resultMsg = channel.sendMessageSynchronously(M_CMD_GET_WIFI_STATUS);
        String result = (String) resultMsg.obj;
        resultMsg.recycle();
        return result;
    }

    public void setPowerSavingMode(boolean mode) {
        sendMessage(obtainMessage(M_CMD_SET_POWER_SAVING_MODE, mode ? 1 : 0, 0));
    }

    public void setTdlsPowerSave(boolean enable) {
        sendMessage(obtainMessage(M_CMD_SET_TDLS_POWER_SAVE, enable ? 1 : 0, 0));
    }

    private class PppoeHandler extends Handler {
        private boolean mCancelCallback;
        private StateMachine mController;

        public PppoeHandler(Looper looper, StateMachine target) {
            super(looper);
            this.mController = target;
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(WifiStateMachine.TAG, "Handle start PPPOE message!");
            DhcpResults pppoeResult = new DhcpResults();
            synchronized (this) {
                this.mCancelCallback = false;
            }
            WifiStateMachine.this.mPppoeInfo.status = PPPOEInfo.Status.CONNECTING;
            WifiStateMachine.this.sendPppoeStateChangedBroadcast("PPPOE_STATE_CONNECTING");
            int result = NetworkUtils.runPPPOE(WifiStateMachine.this.mInterfaceName, WifiStateMachine.this.mPppoeConfig.timeout, WifiStateMachine.this.mPppoeConfig.username, WifiStateMachine.this.mPppoeConfig.password, WifiStateMachine.this.mPppoeConfig.lcp_echo_interval, WifiStateMachine.this.mPppoeConfig.lcp_echo_failure, WifiStateMachine.this.mPppoeConfig.mtu, WifiStateMachine.this.mPppoeConfig.mru, WifiStateMachine.this.mPppoeConfig.MSS, pppoeResult);
            Log.d(WifiStateMachine.TAG, "runPPPOE result:" + result);
            if (result == 0) {
                Log.d(WifiStateMachine.TAG, "PPPoE succeeded, pppoeResult:" + pppoeResult);
                synchronized (this) {
                    if (!this.mCancelCallback) {
                        this.mController.sendMessage(1, pppoeResult);
                    }
                }
                return;
            }
            WifiStateMachine.this.stopPPPoE();
            WifiStateMachine.this.sendPppoeCompletedBroadcast("FAILURE", result);
            Log.d(WifiStateMachine.TAG, "PPPoE failed, error:" + NetworkUtils.getPPPOEError());
        }

        public synchronized void setCancelCallback(boolean cancelCallback) {
            this.mCancelCallback = cancelCallback;
        }
    }

    private void stopPPPoE() {
        Log.d(TAG, "stopPPPoE, mPppoeInfo:" + this.mPppoeInfo);
        this.mUsingPppoe = false;
        if (this.mPppoeHandler != null) {
            this.mPppoeHandler.setCancelCallback(true);
            if (this.mPppoeHandler.hasMessages(0)) {
                Log.e(TAG, "hasMessages EVENT_START_PPPOE!");
                this.mPppoeHandler.removeMessages(0);
            }
        } else {
            Log.e(TAG, "mPppoeHandler is null!");
        }
        sendPppoeStateChangedBroadcast("PPPOE_STATE_DISCONNECTING");
        try {
            this.mNwService.removeInterfaceFromNetwork(this.mPppoeLinkProperties.getInterfaceName(), PPPOE_NETID);
            this.mNwService.removeNetwork(PPPOE_NETID);
            Log.d(TAG, "removeNetwork successfully!");
        } catch (Exception e) {
            Log.e(TAG, "Exception in removeNetwork:" + e.toString());
        }
        try {
            this.mNwService.disablePPPOE();
            Log.d(TAG, "Stop PPPOE successfully!");
        } catch (Exception e2) {
            Log.e(TAG, "Exception in disablePPPOE:" + e2.toString());
        }
        this.mPppoeConfig = null;
        this.mPppoeInfo.status = PPPOEInfo.Status.OFFLINE;
        this.mPppoeInfo.online_time = 0L;
        this.mOnlineStartTime = 0L;
        this.mPppoeLinkProperties.clear();
        sendPppoeStateChangedBroadcast("PPPOE_STATE_DISCONNECTED");
        if (this.mPppoeHandler != null) {
            this.mPppoeHandler.getLooper().quit();
            this.mPppoeHandler = null;
        } else {
            Log.e(TAG, "mPppoeHandler is null!");
        }
    }

    private void handleSuccessfulPppoeConfiguration(DhcpResults pppoeResult) {
        this.mPppoeLinkProperties = pppoeResult.toLinkProperties("wlan0");
        Log.d(TAG, "handleSuccessfulPppoeConfiguration, mPppoeLinkProperties:" + this.mPppoeLinkProperties);
        Collection<RouteInfo> oldRouteInfos = this.mLinkProperties.getRoutes();
        Iterator route$iterator = oldRouteInfos.iterator();
        while (route$iterator.hasNext()) {
            Log.d(TAG, "RouteInfo of wlan0:" + ((RouteInfo) route$iterator.next()));
        }
        int wifiNetId = -1;
        Network[] networks = this.mCm.getAllNetworks();
        if (networks != null && networks.length > 0) {
            int i = 0;
            int length = networks.length;
            while (true) {
                if (i >= length) {
                    break;
                }
                Network net = networks[i];
                NetworkInfo info = this.mCm.getNetworkInfo(net);
                if (info == null || info.getType() != 1) {
                    i++;
                } else {
                    wifiNetId = net.netId;
                    break;
                }
            }
        }
        Log.d(TAG, "wifiNetId:" + wifiNetId);
        if (wifiNetId != -1) {
            for (RouteInfo route : oldRouteInfos) {
                if (route.isDefaultRoute()) {
                    try {
                        this.mNwService.removeRoute(wifiNetId, route);
                    } catch (Exception e) {
                        Log.e(TAG, "Exception in removeRoute:" + e.toString());
                    }
                }
            }
        }
        Collection<InetAddress> dnses = this.mPppoeLinkProperties.getDnsServers();
        ArrayList<String> pppoeDnses = new ArrayList<>();
        for (InetAddress dns : dnses) {
            pppoeDnses.add(dns.getHostAddress());
        }
        String[] dnsArr = new String[pppoeDnses.size()];
        pppoeDnses.toArray(dnsArr);
        for (int i2 = 0; i2 < dnsArr.length; i2++) {
            Log.d(TAG, "Set net.dns" + (i2 + 1) + " to " + dnsArr[i2]);
            this.mPropertyService.set("net.dns" + (i2 + 1), dnsArr[i2]);
        }
        try {
            this.mNwService.createPhysicalNetwork(PPPOE_NETID, (String) null);
            this.mNwService.addInterfaceToNetwork("wlan0", PPPOE_NETID);
            this.mNwService.setDnsServersForNetwork(PPPOE_NETID, dnsArr, (String) null);
            this.mNwService.setDefaultNetId(PPPOE_NETID);
            Collection<RouteInfo> newRouteInfos = this.mPppoeLinkProperties.getRoutes();
            for (RouteInfo route2 : newRouteInfos) {
                if (route2.isDefaultRoute()) {
                    this.mNwService.addRoute(PPPOE_NETID, route2);
                }
            }
        } catch (Exception e2) {
            Log.e(TAG, "Exception in config pppoe:" + e2.toString());
        }
        this.mPppoeInfo.status = PPPOEInfo.Status.ONLINE;
        this.mOnlineStartTime = System.currentTimeMillis() / 1000;
        sendPppoeStateChangedBroadcast("PPPOE_STATE_CONNECTED");
        sendPppoeCompletedBroadcast("SUCCESS", 0);
    }

    public boolean syncSetWoWlanNormalMode(AsyncChannel channel) {
        Message resultMsg = channel.sendMessageSynchronously(M_CMD_SET_WOWLAN_NORMAL_MODE);
        if (resultMsg == null) {
            log("syncSetWoWlanNormalMode fail, resultMsg == null");
            return false;
        }
        boolean result = resultMsg.arg1 != -1;
        resultMsg.recycle();
        return result;
    }

    public boolean syncSetWoWlanMagicMode(AsyncChannel channel) {
        Message resultMsg = channel.sendMessageSynchronously(M_CMD_SET_WOWLAN_MAGIC_MODE);
        if (resultMsg == null) {
            log("syncSetWoWlanMagicMode fail, resultMsg == null");
            return false;
        }
        boolean result = resultMsg.arg1 != -1;
        resultMsg.recycle();
        return result;
    }

    public boolean shouldSwitchNetwork() {
        if (isTemporarilyDontReconnectWifi()) {
            loge("mDontReconnect: " + this.mDontReconnect.get());
            if (this.mDontReconnect.get()) {
                Log.d(TAG, "shouldSwitchNetwork don't switch due to mDontReconnect");
                return false;
            }
            Log.d(TAG, "shouldSwitchNetwork  switch! Even isTemporarilyDontReconnectWifi");
        }
        if (this.mTemporarilyDisconnectWifi) {
            Log.d(TAG, "shouldSwitchNetwork don't switch due to mTemporarilyDisconnectWifi");
            return false;
        }
        if (this.mWifiFwkExt != null && this.mWifiFwkExt.hasNetworkSelection() != 0 && this.mWifiInfo.getNetworkId() != -1) {
            Log.d(TAG, "hasNetworkSelection Don't");
            return false;
        }
        return true;
    }

    public boolean isTemporarilyDontReconnectWifi() {
        log("stopReconnectWifi StopScan=" + this.mStopScanStarted.get() + " mDontReconnectAndScan=" + this.mDontReconnectAndScan.get());
        if (this.mStopScanStarted.get() || this.mDontReconnectAndScan.get()) {
            return true;
        }
        return false;
    }

    public void setHotspotOptimization(boolean enable) {
        log("setHotspotOptimization " + enable);
        this.mHotspotOptimization = enable;
    }

    public String syncGetTestEnv(AsyncChannel channel, int wifiChannel) {
        Message resultMsg = channel.sendMessageSynchronously(M_CMD_GET_TEST_ENV, wifiChannel);
        String result = (String) resultMsg.obj;
        resultMsg.recycle();
        return result;
    }

    private int getSubId(int simSlot) {
        int[] subIds = SubscriptionManager.getSubId(simSlot);
        if (subIds != null) {
            return subIds[0];
        }
        return -1;
    }

    public String getIccAuthentication(int appType, int authType, String base64Challenge, TelephonyManager tm, int netId) {
        int subId;
        if (netId != -1) {
            WifiConfiguration config = getConfiguredNetworkByNetId(netId);
            if (TelephonyManager.getDefault().getPhoneCount() >= 2 && config != null && (subId = getSubId(WifiConfigurationUtil.getIntSimSlot(config))) != -1) {
                String tmResponse = tm.getIccAuthentication(subId, appType, authType, base64Challenge);
                return tmResponse;
            }
        }
        String tmResponse2 = tm.getIccAuthentication(appType, authType, base64Challenge);
        return tmResponse2;
    }

    private WifiConfiguration getConfiguredNetworkByNetId(int netId) {
        List<WifiConfiguration> networks = this.mWifiConfigManager.getSavedNetworks();
        if (networks != null) {
            for (WifiConfiguration config : networks) {
                if (config.networkId == netId) {
                    return config;
                }
            }
        }
        log("getConfiguredNetworkByNetId don't found config");
        return null;
    }

    private boolean setSimSlotNative(WifiConfiguration config) {
        if (config.simSlot != null && !this.mWifiNative.setNetworkVariable(config.networkId, "sim_num", removeDoubleQuotes(config.simSlot))) {
            Log.e(TAG, "failed to set simSlot: " + removeDoubleQuotes(config.simSlot));
            return false;
        }
        this.mWifiNative.saveConfig();
        return true;
    }

    private String removeDoubleQuotes(String string) {
        int length = string.length();
        if (length > 1 && string.charAt(0) == '\"' && string.charAt(length - 1) == '\"') {
            return string.substring(1, length - 1);
        }
        return string;
    }

    public void factoryReset(int uid) {
        sendMessage(M_CMD_FACTORY_RESET, uid);
    }

    private void sendTdlsEventBroadcast(boolean isConnectedEvent, String macAddress) {
        Intent intent;
        logd("sendTdlsEventBroadcast peer: " + macAddress);
        if (isConnectedEvent) {
            intent = new Intent("android.net.wifi.TDLS_CONNECTED");
        } else {
            intent = new Intent("android.net.wifi.TDLS_DISCONNECTED");
        }
        if (macAddress != null) {
            intent.putExtra("tdls_bssid", macAddress);
        }
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void startListenToIpReachabilityLost() {
        this.mIsListeningIpReachabilityLost = true;
        this.ipReachabilityMonitorCount++;
        sendMessageDelayed(obtainMessage(M_CMD_IP_REACHABILITY_MONITOR_TIMER, this.ipReachabilityMonitorCount, 0), 10000L);
    }

    private boolean enableIpReachabilityMonitor() {
        boolean enable = this.mPropertyService.get("persist.wifi.IRM.enable", "1").equals("1");
        log("enable IpReachabilityMonitor Enhancement config: " + this.enableIpReachabilityMonitor.get() + ", SystemProperty: " + enable);
        if (this.enableIpReachabilityMonitor.get() || enable) {
            return true;
        }
        Log.d(TAG, "No enable IpReachabilityMonitor");
        return false;
    }

    private boolean enableIpReachabilityMonitorEnhancement() {
        boolean enable = this.mPropertyService.get("persist.wifi.IRM.enhancement", "1").equals("1");
        log("enable IpReachabilityMonitor Enhancement config: " + this.enableIpReachabilityMonitorEnhancement.get() + ", SystemProperty: " + enable);
        if (this.enableIpReachabilityMonitorEnhancement.get() || enable) {
            return true;
        }
        Log.d(TAG, "No enable IpReachabilityMonitor Enhancement");
        return false;
    }

    private boolean checkOrCleanIdentity(WifiConfiguration config) {
        String imsi;
        if (config != null && config.enterpriseConfig != null) {
            int eapMethod = config.enterpriseConfig.getEapMethod();
            TelephonyManager tm = (TelephonyManager) this.mContext.getSystemService("phone");
            if (tm != null) {
                log("TelephonyManager != null");
                String mccMnc = "";
                if (TelephonyManager.getDefault().getPhoneCount() >= 2) {
                    int slotId = WifiConfigurationUtil.getIntSimSlot(config);
                    log("simSlot: " + config.simSlot + " " + slotId);
                    int subId = getSubId(slotId);
                    log("subId: " + subId);
                    imsi = tm.getSubscriberId(subId);
                    if (tm.getSimState(slotId) == 5) {
                        mccMnc = tm.getSimOperator(subId);
                    }
                } else {
                    imsi = tm.getSubscriberId();
                    if (tm.getSimState() == 5) {
                        mccMnc = tm.getSimOperator();
                    }
                }
                log("imsi: " + imsi);
                log("mccMnc: " + mccMnc);
                String identity = buildIdentity(eapMethod, imsi, mccMnc);
                if (!identity.isEmpty()) {
                    if (config.enterpriseConfig.getIdentity().equals(identity)) {
                        log("same card");
                        return true;
                    }
                    log("different identity: " + config.enterpriseConfig.getIdentity() + "new identity: " + identity);
                    this.mWifiConfigManager.resetSimNetwork(config);
                    resetIdentityAndAnonymousId(config);
                    return true;
                }
                loge("identity is empty");
                return false;
            }
            loge("TelephonyManager is null");
            return false;
        }
        loge("config or enterpriseConfig is null");
        return false;
    }

    private void resetIdentityAndAnonymousId(WifiConfiguration config) {
        log("reset identity and anonymous_identity to NULL");
        config.enterpriseConfig.setIdentity("");
        config.enterpriseConfig.setAnonymousIdentity("");
    }

    private boolean isAirplaneModeOn() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) == 1;
    }

    private boolean isConfigSimCardLoaded(WifiConfiguration config) {
        int simSlot = WifiConfigurationUtil.getIntSimSlot(config);
        if (simSlot == -1) {
            log("simSlot: " + simSlot + " is unspecified, assume sim card is loaded");
            return true;
        }
        String iccState = simSlot == 0 ? this.mSim1IccState : this.mSim2IccState;
        return iccState.equals("LOADED");
    }

    private boolean isConfigSimCardAbsent(WifiConfiguration config) {
        int simSlot = WifiConfigurationUtil.getIntSimSlot(config);
        if (simSlot == -1) {
            log("simSlot: " + simSlot + " is unspecified, assume sim card isn't absent");
            return false;
        }
        String iccState = simSlot == 0 ? this.mSim1IccState : this.mSim2IccState;
        if (iccState.equals("ABSENT") || iccState.equals("LOCKED")) {
            return true;
        }
        return iccState.equals("UNKNOWN");
    }

    public void autoConnectInit() {
        if (this.mWifiFwkExt != null) {
            this.mWifiFwkExt.init();
        }
        this.mWifiConfigManager.setWifiFwkExt(this.mWifiFwkExt);
        this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
    }

    public boolean hasCustomizedAutoConnect() {
        if (this.mWifiFwkExt != null) {
            return this.mWifiFwkExt.hasCustomizedAutoConnect();
        }
        return false;
    }

    public boolean isWifiConnecting(int connectingNetworkId) {
        return (this.mWifiFwkExt != null && this.mWifiFwkExt.isWifiConnecting(connectingNetworkId, this.mWifiConfigManager.getDisconnectNetworks())) || getCurrentState() == this.mWpsRunningState;
    }

    private void showReselectionDialog() {
        this.mScanForWeakSignal = false;
        Log.d(TAG, "showReselectionDialog, mLastNetworkId:" + this.mLastNetworkId + ", mDisconnectNetworkId:" + this.mDisconnectNetworkId);
        int networkId = getHighPriorityNetworkId();
        if (networkId == -1) {
            return;
        }
        if (this.mWifiFwkExt.shouldAutoConnect()) {
            Log.d(TAG, "Supplicant state is " + this.mWifiInfo.getSupplicantState() + " when try to connect network " + networkId);
            if (!isNetworksDisabledDuringConnect()) {
                sendMessage(obtainMessage(CMD_ENABLE_NETWORK, networkId, 1));
                return;
            } else {
                Log.d(TAG, "WiFi is connecting!");
                return;
            }
        }
        this.mShowReselectDialog = this.mWifiFwkExt.handleNetworkReselection();
    }

    private int getHighPriorityNetworkId() {
        int networkId = -1;
        int rssi = MIN_RSSI;
        String ssid = null;
        List<WifiConfiguration> networks = this.mWifiConfigManager.getConfiguredNetworks();
        if (networks == null || networks.size() == 0) {
            Log.d(TAG, "No configured networks, ignore!");
            return -1;
        }
        HashMap<Integer, Integer> foundNetworks = new HashMap<>();
        if (this.mScanResults != null) {
            for (WifiConfiguration network : networks) {
                if (network.networkId != this.mDisconnectNetworkId) {
                    for (ScanDetail scanresult : this.mScanResults) {
                        if (network.SSID != null && scanresult.getSSID() != null && network.SSID.equals("\"" + scanresult.getSSID() + "\"") && getSecurity(network) == getSecurity(scanresult.getScanResult()) && scanresult.getScanResult().level > -79) {
                            foundNetworks.put(Integer.valueOf(network.priority), Integer.valueOf(scanresult.getScanResult().level));
                        }
                    }
                }
            }
        }
        if (foundNetworks.size() < 2) {
            Log.d(TAG, "Configured networks number less than two, ignore!");
            return -1;
        }
        Object[] keys = foundNetworks.keySet().toArray();
        Arrays.sort(keys, new Comparator<Object>() {
            @Override
            public int compare(Object obj1, Object obj2) {
                return ((Integer) obj2).intValue() - ((Integer) obj1).intValue();
            }
        });
        int priority = ((Integer) keys[0]).intValue();
        Iterator network$iterator = networks.iterator();
        while (true) {
            if (!network$iterator.hasNext()) {
                break;
            }
            WifiConfiguration network2 = (WifiConfiguration) network$iterator.next();
            if (network2.priority == priority) {
                networkId = network2.networkId;
                ssid = network2.SSID;
                rssi = foundNetworks.get(Integer.valueOf(priority)).intValue();
                break;
            }
        }
        Log.d(TAG, "Found the highest priority AP, networkId:" + networkId + ", priority:" + priority + ", rssi:" + rssi + ", ssid:" + ssid);
        return networkId;
    }

    private void disableAllNetworks(boolean except) {
        Log.d(TAG, "disableAllNetworks, except:" + except);
        List<WifiConfiguration> networks = this.mWifiConfigManager.getConfiguredNetworks();
        if (except) {
            if (networks == null) {
                return;
            }
            for (WifiConfiguration network : networks) {
                if (network.networkId != this.mLastNetworkId && network.status != 1) {
                    this.mWifiConfigManager.disableNetwork(network.networkId);
                }
            }
            return;
        }
        if (networks == null) {
            return;
        }
        for (WifiConfiguration network2 : networks) {
            if (network2.status != 1) {
                this.mWifiConfigManager.disableNetwork(network2.networkId);
            }
        }
    }

    private void checkIfEapNetworkChanged(WifiConfiguration newConfig) {
        Log.d(TAG, "checkIfEapNetworkChanged, mLastNetworkId:" + this.mLastNetworkId + ", newConfig:" + newConfig);
        if (newConfig == null || this.mLastNetworkId == -1 || this.mLastNetworkId != newConfig.networkId) {
            return;
        }
        if (!newConfig.allowedKeyManagement.get(2) && !newConfig.allowedKeyManagement.get(3)) {
            return;
        }
        this.mDisconnectOperation = true;
        this.mScanForWeakSignal = false;
    }

    private boolean isExplicitNetworkExist() {
        List<WifiConfiguration> networks = this.mWifiConfigManager.getConfiguredNetworks();
        if (this.mScanResults != null && networks != null) {
            for (WifiConfiguration network : networks) {
                if (network.networkId == this.mLastExplicitNetworkId) {
                    for (ScanDetail scanresult : this.mScanResults) {
                        if (network.SSID != null && scanresult.getSSID() != null && network.SSID.equals("\"" + scanresult.getSSID() + "\"") && getSecurity(network) == getSecurity(scanresult.getScanResult())) {
                            Log.d(TAG, "Explicit network " + this.mLastExplicitNetworkId + " exists!");
                            return true;
                        }
                    }
                }
            }
        }
        Log.d(TAG, "Explicit network " + this.mLastExplicitNetworkId + " doesn't exist!");
        return false;
    }

    private void disableLastNetwork() {
        Log.d(TAG, "disableLastNetwork, currentState:" + getCurrentState() + ", mLastNetworkId:" + this.mLastNetworkId + ", mLastBssid:" + this.mLastBssid);
        if (getCurrentState() == this.mSupplicantStoppingState || this.mLastNetworkId == -1) {
            return;
        }
        this.mWifiConfigManager.disableNetwork(this.mLastNetworkId);
    }

    private void updateAutoConnectSettings() {
        boolean isConnecting = isNetworksDisabledDuringConnect();
        Log.d(TAG, "updateAutoConnectSettings, isConnecting:" + isConnecting);
        List<WifiConfiguration> networks = this.mWifiConfigManager.getConfiguredNetworks();
        if (networks == null) {
            return;
        }
        if (this.mWifiFwkExt.shouldAutoConnect()) {
            if (isConnecting) {
                return;
            }
            Collections.sort(networks, new Comparator<WifiConfiguration>() {
                @Override
                public int compare(WifiConfiguration obj1, WifiConfiguration obj2) {
                    return obj2.priority - obj1.priority;
                }
            });
            List<Integer> disconnectNetworks = this.mWifiConfigManager.getDisconnectNetworks();
            for (WifiConfiguration network : networks) {
                if (network.networkId != this.mLastNetworkId && !disconnectNetworks.contains(Integer.valueOf(network.networkId))) {
                    this.mWifiConfigManager.enableNetwork(network, false, -1);
                }
            }
            return;
        }
        if (isConnecting) {
            return;
        }
        for (WifiConfiguration network2 : networks) {
            if (network2.networkId != this.mLastNetworkId && network2.status != 1) {
                this.mWifiConfigManager.disableNetwork(network2.networkId);
            }
        }
    }

    public int getSecurity(WifiConfiguration config) {
        return this.mWifiFwkExt.getSecurity(config);
    }

    public int getSecurity(ScanResult result) {
        return this.mWifiFwkExt.getSecurity(result);
    }

    public boolean syncGetDisconnectFlag(AsyncChannel channel) {
        Message resultMsg = channel.sendMessageSynchronously(M_CMD_GET_DISCONNECT_FLAG);
        boolean result = ((Boolean) resultMsg.obj).booleanValue();
        Log.d(TAG, "syncGetDisconnectFlag:" + result);
        resultMsg.recycle();
        return result;
    }

    void checkGbkEncoding(WifiConfiguration config) {
        Log.d(TAG, "checkGbkEncoding for " + config.configKey());
        synchronized (this.mScanResultsLock) {
            for (ScanDetail sd : this.mScanResults) {
                if (sd.getScanResult().wifiSsid.isGBK()) {
                    List<WifiConfiguration> savedConfigs = this.mWifiConfigManager.getSavedNetworkFromScanDetail(sd);
                    for (WifiConfiguration c : savedConfigs) {
                        if (c.configKey().equals(config.configKey())) {
                            Log.d(TAG, "found GBK config:" + config.networkId + " " + config.configKey());
                            config.isGbkEncoding = true;
                        }
                    }
                }
            }
        }
    }
}
