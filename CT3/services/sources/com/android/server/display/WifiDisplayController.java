package com.android.server.display;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.StatusBarManager;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.display.WifiDisplay;
import android.hardware.display.WifiDisplaySessionInfo;
import android.hardware.input.InputManager;
import android.media.AudioManager;
import android.media.RemoteDisplay;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pWfdInfo;
import android.net.wifi.p2p.link.WifiP2pLinkInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Slog;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.util.DumpUtils;
import com.android.internal.view.IInputMethodManager;
import com.android.server.NetworkManagementService;
import com.android.server.hdmi.HdmiCecKeycode;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.PackageManagerService;
import com.android.server.wm.WindowManagerService;
import com.mediatek.hdmi.IMtkHdmiManager;
import com.mediatek.internal.R;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import libcore.util.Objects;

final class WifiDisplayController implements DumpUtils.Dump {
    private static final int CONNECTION_TIMEOUT_SECONDS = 60;
    private static final int CONNECT_MAX_RETRIES = 3;
    private static final int CONNECT_MIN_RETRIES = 0;
    private static final int CONNECT_RETRY_DELAY_MILLIS = 500;
    private static final int DEFAULT_CONTROL_PORT = 7236;
    private static final int DISCOVER_PEERS_INTERVAL_MILLIS = 10000;
    public static final String DRM_CONTENT_MEDIAPLAYER = "com.mediatek.mediaplayer.DRM_PLAY";
    private static final int MAX_THROUGHPUT = 50;
    private static final int RECONNECT_RETRY_DELAY_MILLIS = 1000;
    private static final int RESCAN_RETRY_DELAY_MILLIS = 2000;
    private static final int RTSP_SINK_TIMEOUT_SECONDS = 10;
    private static final int RTSP_TIMEOUT_SECONDS = 75;
    private static final int RTSP_TIMEOUT_SECONDS_CERT_MODE = 120;
    private static final String TAG = "WifiDisplayController";
    private static final int WFDCONTROLLER_AVERATE_SCORE_COUNT = 4;
    private static final int WFDCONTROLLER_INVALID_VALUE = -1;
    private static final int WFDCONTROLLER_LATENCY_INFO_DELAY_MILLIS = 2000;
    private static final int WFDCONTROLLER_LATENCY_INFO_FIRST_MILLIS = 100;
    private static final int WFDCONTROLLER_LATENCY_INFO_PERIOD_MILLIS = 3000;
    private static final int WFDCONTROLLER_LINK_INFO_PERIOD_MILLIS = 2000;
    private static final String WFDCONTROLLER_PRE_SHUTDOWN = "android.intent.action.ACTION_PRE_SHUTDOWN";
    private static final int WFDCONTROLLER_SCORE_THRESHOLD1 = 100;
    private static final int WFDCONTROLLER_SCORE_THRESHOLD2 = 80;
    private static final int WFDCONTROLLER_SCORE_THRESHOLD3 = 30;
    private static final int WFDCONTROLLER_SCORE_THRESHOLD4 = 10;
    private static final int WFDCONTROLLER_WFD_STAT_DISCONNECT = 0;
    private static final String WFDCONTROLLER_WFD_STAT_FILE = "/proc/wmt_tm/wfd_stat";
    private static final int WFDCONTROLLER_WFD_STAT_STANDBY = 1;
    private static final int WFDCONTROLLER_WFD_STAT_STREAMING = 2;
    private static final int WFDCONTROLLER_WFD_UPDATE = 0;
    private static final int WFDCONTROLLER_WIFI_APP_SCAN_PERIOD_MILLIS = 100;
    private static final int WFD_BLOCK_MAC_TIME = 15000;
    private static final int WFD_BUILD_CONNECT_DIALOG = 9;
    private static final int WFD_CHANGE_RESOLUTION_DIALOG = 5;
    public static final String WFD_CHANNEL_CONFLICT_OCCURS = "com.mediatek.wifi.p2p.OP.channel";
    public static final String WFD_CLEARMOTION_DIMMED = "com.mediatek.clearmotion.DIMMED_UPDATE";
    private static final int WFD_CONFIRM_CONNECT_DIALOG = 8;
    public static final String WFD_CONNECTION = "com.mediatek.wfd.connection";
    private static final int WFD_HDMI_EXCLUDED_DIALOG_WFD_UPDATE = 2;
    public static final String WFD_PORTRAIT = "com.mediatek.wfd.portrait";
    private static final int WFD_RECONNECT_DIALOG = 4;
    public static final String WFD_SINK_CHANNEL_CONFLICT_OCCURS = "com.mediatek.wifi.p2p.freq.conflict";
    private static final int WFD_SINK_DISCOVER_RETRY_COUNT = 5;
    private static final int WFD_SINK_DISCOVER_RETRY_DELAY_MILLIS = 100;
    public static final String WFD_SINK_GC_REQUEST_CONNECT = "com.mediatek.wifi.p2p.GO.GCrequest.connect";
    private static final int WFD_SINK_IP_RETRY_COUNT = 50;
    private static final int WFD_SINK_IP_RETRY_DELAY_MILLIS = 1000;
    private static final int WFD_SINK_IP_RETRY_FIRST_DELAY = 300;
    private static final int WFD_SOUND_PATH_DIALOG = 6;
    private static final int WFD_WAIT_CONNECT_DIALOG = 7;
    private static final int WFD_WIFIP2P_EXCLUDED_DIALOG = 1;
    private int WFDCONTROLLER_DISPLAY_NOTIFICATION_TIME;
    private int WFDCONTROLLER_DISPLAY_POWER_SAVING_DELAY;
    private int WFDCONTROLLER_DISPLAY_POWER_SAVING_OPTION;
    private int WFDCONTROLLER_DISPLAY_RESOLUTION;
    private int WFDCONTROLLER_DISPLAY_SECURE_OPTION;
    private int WFDCONTROLLER_DISPLAY_TOAST_TIME;
    private WifiDisplay mAdvertisedDisplay;
    private int mAdvertisedDisplayFlags;
    private int mAdvertisedDisplayHeight;
    private Surface mAdvertisedDisplaySurface;
    private int mAdvertisedDisplayWidth;
    private AudioManager mAudioManager;
    private boolean mAutoEnableWifi;
    private int mBackupShowTouchVal;
    private String mBlockMac;
    private AlertDialog mBuildConnectDialog;
    private WifiP2pDevice mCancelingDevice;
    private AlertDialog mChangeResolutionDialog;
    private ChannelConflictState mChannelConflictState;
    private AlertDialog mConfirmConnectDialog;
    private WifiP2pDevice mConnectedDevice;
    private WifiP2pGroup mConnectedDeviceGroupInfo;
    private WifiP2pDevice mConnectingDevice;
    private int mConnectionRetriesLeft;
    private final Context mContext;
    private boolean mDRMContent_Mediaplayer;
    private WifiP2pDevice mDesiredDevice;
    private WifiP2pDevice mDisconnectingDevice;
    private boolean mDiscoverPeersInProgress;
    private boolean mDisplayApToast;
    private String mFast_DesiredMac;
    private boolean mFast_NeedFastRtsp;
    private AlertDialog mHDMIExcludeDialog_WfdUpdate;
    private final Handler mHandler;
    private IMtkHdmiManager mHdmiManager;
    private IInputMethodManager mInputMethodManager;
    private boolean mIsConnected_OtherP2p;
    private boolean mIsConnecting_P2p_Rtsp;
    private boolean mIsNeedRotate;
    private boolean mIsWFDConnected;
    private boolean mLastTimeConnected;
    private final Listener mListener;
    private NetworkInfo mNetworkInfo;
    private boolean mNotiTimerStarted;
    private final NotificationManager mNotificationManager;
    private int mPlayerID_Mediaplayer;
    private int mPrevResolution;
    private boolean mRTSPConnecting;
    private WifiP2pDevice mReConnectDevice;
    private AlertDialog mReConnecteDialog;
    private boolean mReConnecting;
    private int mReConnection_Timeout_Remain_Seconds;
    private boolean mReScanning;
    private RemoteDisplay mRemoteDisplay;
    private boolean mRemoteDisplayConnected;
    private String mRemoteDisplayInterface;
    private int mResolution;
    private boolean mScanRequested;
    private String mSinkDeviceName;
    private int mSinkDiscoverRetryCount;
    private String mSinkIpAddress;
    private int mSinkIpRetryCount;
    private String mSinkMacAddress;
    private WifiP2pGroup mSinkP2pGroup;
    private int mSinkPort;
    private SinkState mSinkState;
    private Surface mSinkSurface;
    private AlertDialog mSoundPathDialog;
    StatusBarManager mStatusBarManager;
    private WifiP2pDevice mThisDevice;
    private boolean mToastTimerStarted;
    private boolean mUserDecided;
    private AlertDialog mWaitConnectDialog;
    private PowerManager.WakeLock mWakeLock;
    private PowerManager.WakeLock mWakeLockSink;
    private boolean mWfdEnabled;
    private boolean mWfdEnabling;
    WifiP2pWfdInfo mWfdInfo;
    private AlertDialog mWifiDirectExcludeDialog;
    private boolean mWifiDisplayCertMode;
    private boolean mWifiDisplayOnSetting;
    private WifiManager.WifiLock mWifiLock;
    private WifiManager mWifiManager;
    private final WifiP2pManager.Channel mWifiP2pChannel;
    private boolean mWifiP2pEnabled;
    private final WifiP2pManager mWifiP2pManager;
    private static boolean DEBUG = true;
    private static final Pattern wfdLinkInfoPattern = Pattern.compile("sta_addr=((?:[0-9a-f]{2}:){5}[0-9a-f]{2}|any)\nlink_score=(.*)\nper=(.*)\nrssi=(.*)\nphy=(.*)\nrate=(.*)\ntotal_cnt=(.*)\nthreshold_cnt=(.*)\nfail_cnt=(.*)\ntimeout_cnt=(.*)\napt=(.*)\naat=(.*)\nTC_buf_full_cnt=(.*)\nTC_sta_que_len=(.*)\nTC_avg_que_len=(.*)\nTC_cur_que_len=(.*)\nflag=(.*)\nreserved0=(.*)\nreserved1=(.*)");
    private final ArrayList<WifiP2pDevice> mAvailableWifiDisplayPeers = new ArrayList<>();
    private int mWifiDisplayWpsConfig = 4;
    private boolean WFDCONTROLLER_SQC_INFO_ON = false;
    private boolean WFDCONTROLLER_QE_ON = true;
    private boolean mAutoChannelSelection = false;
    private int mLatencyProfiling = 2;
    private boolean mReconnectForResolutionChange = false;
    private int mWifiP2pChannelId = -1;
    private boolean mWifiApConnected = false;
    private int mWifiApFreq = 0;
    private int mWifiNetworkId = -1;
    private String mWifiApSsid = null;
    View mLatencyPanelView = null;
    TextView mTextView = null;
    private int[] mScore = new int[4];
    private int mScoreIndex = 0;
    private int mScoreLevel = 0;
    private int mLevel = 0;
    private int mWifiScore = 0;
    private int mWifiRate = 0;
    private int mRSSI = 0;
    private boolean mStopWifiScan = false;
    private boolean mWifiPowerSaving = true;
    private int mP2pOperFreq = 0;
    private int mNetworkId = -1;
    private boolean mSinkEnabled = false;
    private final Runnable mDiscoverPeers = new Runnable() {
        @Override
        public void run() {
            Slog.d(WifiDisplayController.TAG, "mDiscoverPeers, run()");
            WifiDisplayController.this.tryDiscoverPeers();
        }
    };
    private final Runnable mConnectionTimeout = new Runnable() {
        @Override
        public void run() {
            if (WifiDisplayController.this.mConnectingDevice == null || WifiDisplayController.this.mConnectingDevice != WifiDisplayController.this.mDesiredDevice) {
                return;
            }
            Slog.i(WifiDisplayController.TAG, "Timed out waiting for Wifi display connection after 60 seconds: " + WifiDisplayController.this.mConnectingDevice.deviceName);
            WifiDisplayController.this.handleConnectionFailure(true);
        }
    };
    private final Runnable mRtspTimeout = new Runnable() {
        @Override
        public void run() {
            if (WifiDisplayController.this.mConnectedDevice == null || WifiDisplayController.this.mRemoteDisplay == null || WifiDisplayController.this.mRemoteDisplayConnected) {
                return;
            }
            Slog.i(WifiDisplayController.TAG, "Timed out waiting for Wifi display RTSP connection after 75 seconds: " + WifiDisplayController.this.mConnectedDevice.deviceName);
            WifiDisplayController.this.handleConnectionFailure(true);
        }
    };
    private final BroadcastReceiver mWifiP2pReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.net.wifi.p2p.STATE_CHANGED")) {
                boolean enabled = intent.getIntExtra("wifi_p2p_state", 1) == 2;
                Slog.d(WifiDisplayController.TAG, "Received WIFI_P2P_STATE_CHANGED_ACTION: enabled=" + enabled);
                WifiDisplayController.this.handleStateChanged(enabled);
                return;
            }
            if (action.equals("android.net.wifi.p2p.PEERS_CHANGED")) {
                if (WifiDisplayController.DEBUG) {
                    Slog.d(WifiDisplayController.TAG, "Received WIFI_P2P_PEERS_CHANGED_ACTION.");
                }
                WifiDisplayController.this.handlePeersChanged();
                return;
            }
            if (action.equals("android.net.wifi.p2p.CONNECTION_STATE_CHANGE")) {
                NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                int reason = intent.getIntExtra("reason=", -1);
                if (WifiDisplayController.DEBUG) {
                    Slog.d(WifiDisplayController.TAG, "Received WIFI_P2P_CONNECTION_CHANGED_ACTION: networkInfo=" + networkInfo + ", reason = " + reason);
                } else {
                    Slog.d(WifiDisplayController.TAG, "Received WIFI_P2P_CONNECTION_CHANGED_ACTION: isConnected? " + networkInfo.isConnected() + ", reason = " + reason);
                }
                WifiDisplayController.this.updateWifiP2pChannelId(networkInfo.isConnected(), intent);
                if (!SystemProperties.get("ro.mtk_wfd_sink_support").equals("1") || !WifiDisplayController.this.mSinkEnabled) {
                    WifiDisplayController.this.handleConnectionChanged(networkInfo, reason);
                    WifiDisplayController.this.mLastTimeConnected = networkInfo.isConnected();
                    return;
                } else {
                    if (reason != -2) {
                        WifiDisplayController.this.handleSinkP2PConnection(networkInfo);
                        return;
                    }
                    return;
                }
            }
            if (action.equals("android.net.wifi.p2p.THIS_DEVICE_CHANGED")) {
                WifiDisplayController.this.mThisDevice = (WifiP2pDevice) intent.getParcelableExtra("wifiP2pDevice");
                if (WifiDisplayController.DEBUG) {
                    Slog.d(WifiDisplayController.TAG, "Received WIFI_P2P_THIS_DEVICE_CHANGED_ACTION: mThisDevice= " + WifiDisplayController.this.mThisDevice);
                    return;
                }
                return;
            }
            if (action.equals(WifiDisplayController.DRM_CONTENT_MEDIAPLAYER)) {
                WifiDisplayController.this.mDRMContent_Mediaplayer = intent.getBooleanExtra("isPlaying", false);
                int playerID = intent.getIntExtra("playerId", 0);
                Slog.i(WifiDisplayController.TAG, "Received DRM_CONTENT_MEDIAPLAYER: isPlaying = " + WifiDisplayController.this.mDRMContent_Mediaplayer + ", player = " + playerID + ", isConnected = " + WifiDisplayController.this.mIsWFDConnected + ", isConnecting = " + WifiDisplayController.this.mRTSPConnecting);
                if (WifiDisplayController.this.mIsWFDConnected || WifiDisplayController.this.mRTSPConnecting) {
                    if (WifiDisplayController.this.mDRMContent_Mediaplayer) {
                        WifiDisplayController.this.mPlayerID_Mediaplayer = playerID;
                        return;
                    } else {
                        if (WifiDisplayController.this.mPlayerID_Mediaplayer != playerID) {
                            Slog.w(WifiDisplayController.TAG, "player ID doesn't match last time: " + WifiDisplayController.this.mPlayerID_Mediaplayer);
                            return;
                        }
                        return;
                    }
                }
                return;
            }
            if (action.equals("android.net.wifi.p2p.DISCOVERY_STATE_CHANGE")) {
                int discoveryState = intent.getIntExtra("discoveryState", 1);
                if (WifiDisplayController.DEBUG) {
                    Slog.d(WifiDisplayController.TAG, "Received WIFI_P2P_DISCOVERY_CHANGED_ACTION: discoveryState=" + discoveryState);
                }
                if (discoveryState == 1) {
                    WifiDisplayController.this.handleScanFinished();
                    return;
                }
                return;
            }
            if (action.equals(WifiDisplayController.WFDCONTROLLER_PRE_SHUTDOWN)) {
                Slog.i(WifiDisplayController.TAG, "Received android.intent.action.ACTION_PRE_SHUTDOWN, do disconnect anyway");
                if (WifiDisplayController.this.mWifiP2pManager != null) {
                    WifiDisplayController.this.mWifiP2pManager.removeGroup(WifiDisplayController.this.mWifiP2pChannel, null);
                }
                if (WifiDisplayController.this.mRemoteDisplay != null) {
                    WifiDisplayController.this.mRemoteDisplay.dispose();
                    return;
                }
                return;
            }
            if (action.equals("android.net.wifi.STATE_CHANGE")) {
                NetworkInfo info = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                boolean connected = info.isConnected();
                boolean updated = connected != WifiDisplayController.this.mWifiApConnected;
                WifiDisplayController.this.mWifiApConnected = connected;
                if (WifiDisplayController.this.mWifiApConnected) {
                    WifiInfo conInfo = WifiDisplayController.this.mWifiManager.getConnectionInfo();
                    if (conInfo != null) {
                        if (!conInfo.getSSID().equals(WifiDisplayController.this.mWifiApSsid) || conInfo.getFrequency() != WifiDisplayController.this.mWifiApFreq || conInfo.getNetworkId() != WifiDisplayController.this.mWifiNetworkId) {
                            updated = true;
                        }
                        WifiDisplayController.this.mWifiApSsid = conInfo.getSSID();
                        WifiDisplayController.this.mWifiApFreq = conInfo.getFrequency();
                        WifiDisplayController.this.mWifiNetworkId = conInfo.getNetworkId();
                    }
                } else {
                    WifiDisplayController.this.mWifiApSsid = null;
                    WifiDisplayController.this.mWifiApFreq = 0;
                    WifiDisplayController.this.mWifiNetworkId = -1;
                }
                Slog.i(WifiDisplayController.TAG, "Received NETWORK_STATE_CHANGED,con:" + WifiDisplayController.this.mWifiApConnected + ",SSID:" + WifiDisplayController.this.mWifiApSsid + ",Freq:" + WifiDisplayController.this.mWifiApFreq + ",netId:" + WifiDisplayController.this.mWifiNetworkId + ", updated:" + updated);
                if (updated) {
                    if (SystemProperties.get("ro.mtk_wfd_sink_support").equals("1") && WifiDisplayController.this.mSinkEnabled) {
                        WifiDisplayController.this.setSinkMiracastMode();
                    }
                    WifiDisplayController.this.handleChannelConflictProcedure(WifiDisplayController.this.mWifiApConnected ? ChannelConflictEvt.EVT_AP_CONNECTED : ChannelConflictEvt.EVT_AP_DISCONNECTED);
                    return;
                }
                return;
            }
            if (action.equals(WifiDisplayController.WFD_SINK_GC_REQUEST_CONNECT)) {
                WifiDisplayController.this.mSinkDeviceName = intent.getStringExtra("deviceName");
                Slog.i(WifiDisplayController.TAG, "Received WFD_SINK_GC_REQUEST_CONNECT, mSinkDeviceName:" + WifiDisplayController.this.mSinkDeviceName);
                if (!WifiDisplayController.this.isSinkState(SinkState.SINK_STATE_WAITING_P2P_CONNECTION)) {
                    Slog.d(WifiDisplayController.TAG, "State is wrong. Decline directly !!");
                    WifiDisplayController.this.mWifiP2pManager.setGCInviteResult(WifiDisplayController.this.mWifiP2pChannel, false, 0, null);
                    return;
                } else {
                    if (WifiDisplayController.this.mSinkDeviceName != null) {
                        WifiDisplayController.this.showDialog(8);
                        return;
                    }
                    return;
                }
            }
            if (action.equals(WifiDisplayController.WFD_CHANNEL_CONFLICT_OCCURS)) {
                WifiDisplayController.this.mP2pOperFreq = intent.getIntExtra("p2pOperFreq", -1);
                Slog.i(WifiDisplayController.TAG, "Received WFD_CHANNEL_CONFLICT_OCCURS, p2pOperFreq:" + WifiDisplayController.this.mP2pOperFreq);
                if (WifiDisplayController.this.mP2pOperFreq != -1) {
                    WifiDisplayController.this.startChannelConflictProcedure();
                    return;
                }
                return;
            }
            if (action.equals(WifiDisplayController.WFD_SINK_CHANNEL_CONFLICT_OCCURS)) {
                Slog.i(WifiDisplayController.TAG, "Received WFD_SINK_CHANNEL_CONFLICT_OCCURS, mSinkEnabled:" + WifiDisplayController.this.mSinkEnabled + ", apConnected:" + WifiDisplayController.this.mWifiApConnected);
                if (SystemProperties.get("ro.mtk_wfd_sink_support").equals("1") && WifiDisplayController.this.mSinkEnabled && WifiDisplayController.this.mWifiApConnected) {
                    WifiDisplayController.this.notifyApDisconnected();
                }
            }
        }
    };
    private final Runnable mWifiLinkInfo = new Runnable() {
        @Override
        public void run() {
            if (WifiDisplayController.this.mConnectedDevice == null) {
                Slog.e(WifiDisplayController.TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + ": ConnectedDevice is null");
            } else if (WifiDisplayController.this.mRemoteDisplay == null) {
                Slog.e(WifiDisplayController.TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + ": RemoteDisplay is null");
            } else {
                WifiDisplayController.this.mWifiP2pManager.requestWifiP2pLinkInfo(WifiDisplayController.this.mWifiP2pChannel, WifiDisplayController.this.mConnectedDevice.deviceAddress, new WifiP2pManager.WifiP2pLinkInfoListener() {
                    public void onLinkInfoAvailable(WifiP2pLinkInfo status) {
                        if (status != null && status.linkInfo != null) {
                            Matcher match = WifiDisplayController.wfdLinkInfoPattern.matcher(status.linkInfo);
                            if (match.find()) {
                                WifiDisplayController.this.mWifiScore = WifiDisplayController.this.parseDec(match.group(2));
                                WifiDisplayController.this.mRSSI = WifiDisplayController.this.parseFloat(match.group(4));
                                WifiDisplayController.this.mWifiRate = WifiDisplayController.this.parseFloat(match.group(6));
                                boolean interference = WifiDisplayController.this.checkInterference(match);
                                WifiDisplayController.this.updateSignalLevel(interference);
                                return;
                            }
                            Slog.e(WifiDisplayController.TAG, "wfdLinkInfoPattern Malformed Pattern, not match String ");
                            return;
                        }
                        Slog.e(WifiDisplayController.TAG, "onLinkInfoAvailable() parameter is null!");
                    }
                });
                WifiDisplayController.this.mHandler.postDelayed(WifiDisplayController.this.mWifiLinkInfo, 2000L);
            }
        }
    };
    private final ContentObserver mObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (selfChange) {
                return;
            }
            WifiDisplayController.this.WFDCONTROLLER_DISPLAY_TOAST_TIME = Settings.Global.getInt(WifiDisplayController.this.mContext.getContentResolver(), "wifi_display_display_toast_time", 20);
            WifiDisplayController.this.WFDCONTROLLER_DISPLAY_NOTIFICATION_TIME = Settings.Global.getInt(WifiDisplayController.this.mContext.getContentResolver(), "wifi_display_notification_time", WifiDisplayController.RTSP_TIMEOUT_SECONDS_CERT_MODE);
            WifiDisplayController.this.WFDCONTROLLER_SQC_INFO_ON = Settings.Global.getInt(WifiDisplayController.this.mContext.getContentResolver(), "wifi_display_sqc_info_on", 0) != 0;
            WifiDisplayController.this.WFDCONTROLLER_QE_ON = Settings.Global.getInt(WifiDisplayController.this.mContext.getContentResolver(), "wifi_display_qe_on", 0) != 0;
            WifiDisplayController.this.mAutoChannelSelection = Settings.Global.getInt(WifiDisplayController.this.mContext.getContentResolver(), "wifi_display_auto_channel_selection", 0) != 0;
            Slog.d(WifiDisplayController.TAG, "onChange(), t_time:" + WifiDisplayController.this.WFDCONTROLLER_DISPLAY_TOAST_TIME + ",n_time:" + WifiDisplayController.this.WFDCONTROLLER_DISPLAY_NOTIFICATION_TIME + ",sqc:" + WifiDisplayController.this.WFDCONTROLLER_SQC_INFO_ON + ",qe:" + WifiDisplayController.this.WFDCONTROLLER_QE_ON + ",autoChannel:" + WifiDisplayController.this.mAutoChannelSelection);
            if (SystemProperties.get("ro.mtk_wfd_support").equals("1")) {
                WifiDisplayController.this.handleResolutionChange();
                WifiDisplayController.this.handleLatencyProfilingChange();
                WifiDisplayController.this.handleSecureOptionChange();
                WifiDisplayController.this.handlePortraitResolutionSupportChange();
            }
        }
    };
    private final Runnable mLatencyInfo = new Runnable() {
        @Override
        public void run() {
            if (WifiDisplayController.this.mConnectedDevice == null) {
                Slog.e(WifiDisplayController.TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + ": ConnectedDevice is null");
                return;
            }
            if (WifiDisplayController.this.mRemoteDisplay == null) {
                Slog.e(WifiDisplayController.TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + ": RemoteDisplay is null");
                return;
            }
            if (WifiDisplayController.this.mLatencyProfiling != 0 && WifiDisplayController.this.mLatencyProfiling != 1 && WifiDisplayController.this.mLatencyProfiling != 3) {
                Slog.e(WifiDisplayController.TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + ": mLatencyProfiling:" + WifiDisplayController.this.mLatencyProfiling);
                return;
            }
            int wifiApNum = WifiDisplayController.this.getWifiApNum();
            String WifiInfo = WifiDisplayController.this.mWifiP2pChannelId + "," + wifiApNum + "," + WifiDisplayController.this.mWifiScore + "," + WifiDisplayController.this.mWifiRate + "," + WifiDisplayController.this.mRSSI;
            Slog.d(WifiDisplayController.TAG, "WifiInfo:" + WifiInfo);
            int avgLatency = WifiDisplayController.this.mRemoteDisplay.getWfdParam(5);
            int sinkFps = WifiDisplayController.this.mRemoteDisplay.getWfdParam(6);
            String WFDLatency = avgLatency + ",0,0";
            Slog.d(WifiDisplayController.TAG, "WFDLatency:" + WFDLatency);
            if (WifiDisplayController.this.mLatencyProfiling == 0 || WifiDisplayController.this.mLatencyProfiling == 1) {
                Settings.Global.putString(WifiDisplayController.this.mContext.getContentResolver(), "wifi_display_wifi_info", WifiInfo);
                Settings.Global.putString(WifiDisplayController.this.mContext.getContentResolver(), "wifi_display_wfd_latency", WFDLatency);
            } else if (WifiDisplayController.this.mLatencyProfiling == 3) {
                WifiDisplayController.this.mTextView.setText("AP:" + wifiApNum + "\nS:" + WifiDisplayController.this.mWifiScore + "\nR:" + WifiDisplayController.this.mWifiRate + "\nRS:" + WifiDisplayController.this.mRSSI + "\nAL:" + avgLatency + "\nSF:" + sinkFps + "\n");
            }
            WifiDisplayController.this.mHandler.postDelayed(WifiDisplayController.this.mLatencyInfo, 3000L);
        }
    };
    private final Runnable mScanWifiAp = new Runnable() {
        @Override
        public void run() {
            if (WifiDisplayController.this.mConnectedDevice == null) {
                Slog.e(WifiDisplayController.TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + ": ConnectedDevice is null");
                return;
            }
            if (WifiDisplayController.this.mRemoteDisplay == null) {
                Slog.e(WifiDisplayController.TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + ": RemoteDisplay is null");
                return;
            }
            if (WifiDisplayController.this.mLatencyProfiling != 0 && WifiDisplayController.this.mLatencyProfiling != 1 && WifiDisplayController.this.mLatencyProfiling != 3) {
                Slog.e(WifiDisplayController.TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + ": mLatencyProfiling:" + WifiDisplayController.this.mLatencyProfiling);
            } else {
                Slog.d(WifiDisplayController.TAG, "call mWifiManager.startScan()");
                WifiDisplayController.this.mWifiManager.startScan();
            }
        }
    };
    private final Runnable mDelayProfiling = new Runnable() {
        @Override
        public void run() {
            if (WifiDisplayController.this.mLatencyProfiling != 3 || !WifiDisplayController.this.mIsWFDConnected) {
                return;
            }
            WifiDisplayController.this.startProfilingInfo();
        }
    };
    private final Runnable mDisplayToast = new Runnable() {
        @Override
        public void run() {
            Slog.d(WifiDisplayController.TAG, "mDisplayToast run()" + WifiDisplayController.this.mLevel);
            Resources mResource = Resources.getSystem();
            if (WifiDisplayController.this.mLevel != 0) {
                Toast.makeText(WifiDisplayController.this.mContext, mResource.getString(134545537), 0).show();
            }
            WifiDisplayController.this.mToastTimerStarted = false;
        }
    };
    private final Runnable mDisplayNotification = new Runnable() {
        @Override
        public void run() {
            Slog.d(WifiDisplayController.TAG, "mDisplayNotification run()" + WifiDisplayController.this.mLevel);
            if (WifiDisplayController.this.mLevel != 0) {
                WifiDisplayController.this.showNotification(134545536, 134545538);
            }
            WifiDisplayController.this.mNotiTimerStarted = false;
        }
    };
    private final Runnable mReConnect = new Runnable() {
        @Override
        public void run() {
            Slog.d(WifiDisplayController.TAG, "mReConnect, run()");
            if (WifiDisplayController.this.mReConnectDevice == null) {
                Slog.w(WifiDisplayController.TAG, "no reconnect device");
                return;
            }
            for (WifiP2pDevice device : WifiDisplayController.this.mAvailableWifiDisplayPeers) {
                if (WifiDisplayController.DEBUG) {
                    Slog.d(WifiDisplayController.TAG, "\t" + WifiDisplayController.describeWifiP2pDevice(device));
                }
                if (device.deviceAddress.equals(WifiDisplayController.this.mReConnectDevice.deviceAddress)) {
                    Slog.i(WifiDisplayController.TAG, "connect() in mReConnect. Set mReConnecting as true");
                    WifiDisplayController.this.mReScanning = false;
                    WifiDisplayController.this.mReConnecting = true;
                    WifiDisplayController.this.connect(device);
                    return;
                }
            }
            WifiDisplayController.this.mReConnection_Timeout_Remain_Seconds--;
            if (WifiDisplayController.this.mReConnection_Timeout_Remain_Seconds > 0) {
                Slog.i(WifiDisplayController.TAG, "post mReconnect, s:" + WifiDisplayController.this.mReConnection_Timeout_Remain_Seconds);
                WifiDisplayController.this.mHandler.postDelayed(WifiDisplayController.this.mReConnect, 1000L);
            } else {
                Slog.e(WifiDisplayController.TAG, "reconnect timeout!");
                Toast.makeText(WifiDisplayController.this.mContext, 134545542, 0).show();
                WifiDisplayController.this.resetReconnectVariable();
            }
        }
    };
    private final Runnable mSinkDiscover = new Runnable() {
        @Override
        public void run() {
            Slog.d(WifiDisplayController.TAG, "mSinkDiscover run(), count:" + WifiDisplayController.this.mSinkDiscoverRetryCount);
            if (!WifiDisplayController.this.isSinkState(SinkState.SINK_STATE_WAITING_P2P_CONNECTION)) {
                Slog.d(WifiDisplayController.TAG, "mSinkState:(" + WifiDisplayController.this.mSinkState + ") is wrong !");
            } else {
                WifiDisplayController.this.startWaitConnection();
            }
        }
    };
    private final Runnable mGetSinkIpAddr = new Runnable() {
        @Override
        public void run() {
            Slog.d(WifiDisplayController.TAG, "mGetSinkIpAddr run(), count:" + WifiDisplayController.this.mSinkIpRetryCount);
            if (!WifiDisplayController.this.isSinkState(SinkState.SINK_STATE_WIFI_P2P_CONNECTED)) {
                Slog.d(WifiDisplayController.TAG, "mSinkState:(" + WifiDisplayController.this.mSinkState + ") is wrong !");
                return;
            }
            WifiDisplayController.this.mSinkIpAddress = WifiDisplayController.this.mWifiP2pManager.getPeerIpAddress(WifiDisplayController.this.mSinkMacAddress);
            if (WifiDisplayController.this.mSinkIpAddress == null) {
                if (WifiDisplayController.this.mSinkIpRetryCount > 0) {
                    WifiDisplayController wifiDisplayController = WifiDisplayController.this;
                    wifiDisplayController.mSinkIpRetryCount--;
                    WifiDisplayController.this.mHandler.postDelayed(WifiDisplayController.this.mGetSinkIpAddr, 1000L);
                    return;
                }
                Slog.d(WifiDisplayController.TAG, "mGetSinkIpAddr FAIL !!!!!!");
                return;
            }
            WifiDisplayController.this.mSinkIpAddress += ":" + WifiDisplayController.this.mSinkPort;
            Slog.i(WifiDisplayController.TAG, "sink Ip address = " + WifiDisplayController.this.mSinkIpAddress);
            WifiDisplayController.this.connectRtsp();
        }
    };
    private final Runnable mRtspSinkTimeout = new Runnable() {
        @Override
        public void run() {
            Slog.d(WifiDisplayController.TAG, "mRtspSinkTimeout, run()");
            WifiDisplayController.this.disconnectWfdSink();
        }
    };
    private AudioManager.OnAudioFocusChangeListener mAudioFocusListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            Slog.e(WifiDisplayController.TAG, "onAudioFocusChange(), focus:" + focusChange);
            switch (focusChange) {
            }
        }
    };
    private final Runnable mEnableWifiDelay = new Runnable() {
        @Override
        public void run() {
            Slog.d(WifiDisplayController.TAG, "Enable wifi automatically.");
            WifiDisplayController.this.mAutoEnableWifi = true;
            int wifiApState = WifiDisplayController.this.mWifiManager.getWifiApState();
            if (wifiApState == 12 || wifiApState == 13) {
                ConnectivityManager cm = (ConnectivityManager) WifiDisplayController.this.mContext.getSystemService("connectivity");
                cm.stopTethering(0);
            }
            WifiDisplayController.this.mWifiManager.setWifiEnabled(true);
        }
    };

    public interface Listener {
        void onDisplayChanged(WifiDisplay wifiDisplay);

        void onDisplayConnected(WifiDisplay wifiDisplay, Surface surface, int i, int i2, int i3);

        void onDisplayConnecting(WifiDisplay wifiDisplay);

        void onDisplayConnectionFailed();

        void onDisplayDisconnected();

        void onDisplayDisconnecting();

        void onDisplaySessionInfo(WifiDisplaySessionInfo wifiDisplaySessionInfo);

        void onFeatureStateChanged(int i);

        void onScanFinished();

        void onScanResults(WifiDisplay[] wifiDisplayArr);

        void onScanStarted();
    }

    enum ChannelConflictState {
        STATE_IDLE,
        STATE_AP_DISCONNECTING,
        STATE_WFD_CONNECTING,
        STATE_AP_CONNECTING;

        public static ChannelConflictState[] valuesCustom() {
            return values();
        }
    }

    enum ChannelConflictEvt {
        EVT_AP_DISCONNECTED,
        EVT_AP_CONNECTED,
        EVT_WFD_P2P_DISCONNECTED,
        EVT_WFD_P2P_CONNECTED;

        public static ChannelConflictEvt[] valuesCustom() {
            return values();
        }
    }

    enum SinkState {
        SINK_STATE_IDLE,
        SINK_STATE_WAITING_P2P_CONNECTION,
        SINK_STATE_WIFI_P2P_CONNECTED,
        SINK_STATE_WAITING_RTSP,
        SINK_STATE_RTSP_CONNECTED;

        public static SinkState[] valuesCustom() {
            return values();
        }
    }

    public WifiDisplayController(Context context, Handler handler, Listener listener) {
        this.mContext = context;
        this.mHandler = handler;
        this.mListener = listener;
        this.mWifiP2pManager = (WifiP2pManager) context.getSystemService("wifip2p");
        this.mWifiP2pChannel = this.mWifiP2pManager.initialize(context, handler.getLooper(), null);
        getWifiLock();
        this.mWfdInfo = new WifiP2pWfdInfo();
        this.mHdmiManager = IMtkHdmiManager.Stub.asInterface(ServiceManager.getService("mtkhdmi"));
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.wifi.p2p.STATE_CHANGED");
        intentFilter.addAction("android.net.wifi.p2p.PEERS_CHANGED");
        intentFilter.addAction("android.net.wifi.p2p.CONNECTION_STATE_CHANGE");
        intentFilter.addAction("android.net.wifi.p2p.THIS_DEVICE_CHANGED");
        intentFilter.addAction(DRM_CONTENT_MEDIAPLAYER);
        intentFilter.addAction("android.net.wifi.p2p.DISCOVERY_STATE_CHANGE");
        intentFilter.addAction(WFDCONTROLLER_PRE_SHUTDOWN);
        intentFilter.addAction("android.net.wifi.STATE_CHANGE");
        intentFilter.addAction(WFD_SINK_GC_REQUEST_CONNECT);
        intentFilter.addAction(WFD_CHANNEL_CONFLICT_OCCURS);
        intentFilter.addAction(WFD_SINK_CHANNEL_CONFLICT_OCCURS);
        context.registerReceiver(this.mWifiP2pReceiver, intentFilter, null, this.mHandler);
        ContentObserver settingsObserver = new ContentObserver(this.mHandler) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                if (selfChange) {
                    return;
                }
                WifiDisplayController.this.updateSettings();
            }
        };
        ContentResolver resolver = this.mContext.getContentResolver();
        resolver.registerContentObserver(Settings.Global.getUriFor("wifi_display_on"), false, settingsObserver);
        resolver.registerContentObserver(Settings.Global.getUriFor("wifi_display_certification_on"), false, settingsObserver);
        resolver.registerContentObserver(Settings.Global.getUriFor("wifi_display_wps_config"), false, settingsObserver);
        updateSettings();
        DisplayMetrics dm = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService("window");
        wm.getDefaultDisplay().getRealMetrics(dm);
        Slog.i(TAG, "RealMetrics, Width = " + dm.widthPixels + ", Height = " + dm.heightPixels);
        if (dm.widthPixels < dm.heightPixels) {
            this.mIsNeedRotate = true;
        }
        registerEMObserver(dm.widthPixels, dm.heightPixels);
        this.mNotificationManager = (NotificationManager) context.getSystemService("notification");
        actionAtDisconnected(null);
        updateWfdStatFile(0);
        PowerManager pm = (PowerManager) context.getSystemService("power");
        this.mWakeLock = pm.newWakeLock(26, "UIBC Source");
        this.mStatusBarManager = (StatusBarManager) context.getSystemService("statusbar");
        this.mWakeLockSink = pm.newWakeLock(26, "WFD Sink");
    }

    private void updateSettings() {
        ContentResolver resolver = this.mContext.getContentResolver();
        this.mWifiDisplayOnSetting = Settings.Global.getInt(resolver, "wifi_display_on", 0) != 0;
        this.mWifiDisplayCertMode = Settings.Global.getInt(resolver, "wifi_display_certification_on", 0) != 0;
        this.mWifiDisplayWpsConfig = 4;
        if (this.mWifiDisplayCertMode) {
            this.mWifiDisplayWpsConfig = Settings.Global.getInt(resolver, "wifi_display_wps_config", 4);
        }
        loadDebugLevel();
        boolean HDMIOn = false;
        if (SystemProperties.get("ro.mtk_hdmi_support").equals("1")) {
            HDMIOn = Settings.System.getInt(resolver, "hdmi_enable_status", 1) != 0;
        }
        if (this.mWifiDisplayOnSetting && HDMIOn) {
            dialogWfdHdmiConflict(0);
        } else {
            enableWifiDisplay();
        }
    }

    public void dump(PrintWriter pw, String prefix) {
        pw.println("mWifiDisplayOnSetting=" + this.mWifiDisplayOnSetting);
        pw.println("mWifiP2pEnabled=" + this.mWifiP2pEnabled);
        pw.println("mWfdEnabled=" + this.mWfdEnabled);
        pw.println("mWfdEnabling=" + this.mWfdEnabling);
        pw.println("mNetworkInfo=" + this.mNetworkInfo);
        pw.println("mScanRequested=" + this.mScanRequested);
        pw.println("mDiscoverPeersInProgress=" + this.mDiscoverPeersInProgress);
        pw.println("mDesiredDevice=" + describeWifiP2pDevice(this.mDesiredDevice));
        pw.println("mConnectingDisplay=" + describeWifiP2pDevice(this.mConnectingDevice));
        pw.println("mDisconnectingDisplay=" + describeWifiP2pDevice(this.mDisconnectingDevice));
        pw.println("mCancelingDisplay=" + describeWifiP2pDevice(this.mCancelingDevice));
        pw.println("mConnectedDevice=" + describeWifiP2pDevice(this.mConnectedDevice));
        pw.println("mConnectionRetriesLeft=" + this.mConnectionRetriesLeft);
        pw.println("mRemoteDisplay=" + this.mRemoteDisplay);
        pw.println("mRemoteDisplayInterface=" + this.mRemoteDisplayInterface);
        pw.println("mRemoteDisplayConnected=" + this.mRemoteDisplayConnected);
        pw.println("mAdvertisedDisplay=" + this.mAdvertisedDisplay);
        pw.println("mAdvertisedDisplaySurface=" + this.mAdvertisedDisplaySurface);
        pw.println("mAdvertisedDisplayWidth=" + this.mAdvertisedDisplayWidth);
        pw.println("mAdvertisedDisplayHeight=" + this.mAdvertisedDisplayHeight);
        pw.println("mAdvertisedDisplayFlags=" + this.mAdvertisedDisplayFlags);
        pw.println("mBackupShowTouchVal=" + this.mBackupShowTouchVal);
        pw.println("mFast_NeedFastRtsp=" + this.mFast_NeedFastRtsp);
        pw.println("mFast_DesiredMac=" + this.mFast_DesiredMac);
        pw.println("mIsNeedRotate=" + this.mIsNeedRotate);
        pw.println("mIsConnected_OtherP2p=" + this.mIsConnected_OtherP2p);
        pw.println("mIsConnecting_P2p_Rtsp=" + this.mIsConnecting_P2p_Rtsp);
        pw.println("mIsWFDConnected=" + this.mIsWFDConnected);
        pw.println("mDRMContent_Mediaplayer=" + this.mDRMContent_Mediaplayer);
        pw.println("mPlayerID_Mediaplayer=" + this.mPlayerID_Mediaplayer);
        pw.println("mAvailableWifiDisplayPeers: size=" + this.mAvailableWifiDisplayPeers.size());
        for (WifiP2pDevice device : this.mAvailableWifiDisplayPeers) {
            pw.println("  " + describeWifiP2pDevice(device));
        }
    }

    public void requestStartScan() {
        Slog.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + ",mSinkEnabled:" + this.mSinkEnabled);
        if ((SystemProperties.get("ro.mtk_wfd_sink_support").equals("1") && this.mSinkEnabled) || this.mScanRequested) {
            return;
        }
        this.mScanRequested = true;
        updateScanState();
    }

    public void requestStopScan() {
        if (!this.mScanRequested) {
            return;
        }
        this.mScanRequested = false;
        updateScanState();
    }

    public void requestConnect(String address) {
        Slog.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + ", address = " + address);
        resetReconnectVariable();
        if (DEBUG) {
            Slog.d(TAG, "mAvailableWifiDisplayPeers dump:");
        }
        for (WifiP2pDevice device : this.mAvailableWifiDisplayPeers) {
            if (DEBUG) {
                Slog.d(TAG, "\t" + describeWifiP2pDevice(device));
            }
            if (device.deviceAddress.equals(address)) {
                if (this.mIsConnected_OtherP2p) {
                    Slog.i(TAG, "OtherP2P is connected! Show dialog!");
                    WifiDisplay display = createWifiDisplay(device);
                    advertiseDisplay(display, null, 0, 0, 0);
                    showDialog(1);
                    return;
                }
                connect(device);
            }
        }
    }

    public void requestPause() {
        if (this.mRemoteDisplay == null) {
            return;
        }
        this.mRemoteDisplay.pause();
    }

    public void requestResume() {
        if (this.mRemoteDisplay == null) {
            return;
        }
        this.mRemoteDisplay.resume();
    }

    public void requestDisconnect() {
        Slog.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName());
        disconnect();
        resetReconnectVariable();
    }

    private void updateWfdEnableState() {
        Slog.i(TAG, "updateWfdEnableState(), mWifiDisplayOnSetting:" + this.mWifiDisplayOnSetting + ", mWifiP2pEnabled:" + this.mWifiP2pEnabled);
        if (this.mWifiDisplayOnSetting && this.mWifiP2pEnabled) {
            this.mSinkEnabled = false;
            if (this.mWfdEnabled || this.mWfdEnabling) {
                return;
            }
            this.mWfdEnabling = true;
            updateWfdInfo(true);
            if (!SystemProperties.get("ro.mtk_wfd_hdcp_tx_support").equals("1") && !SystemProperties.get("ro.mtk_dx_hdcp_support").equals("1")) {
                return;
            }
            updateWifiPowerSavingMode(false);
            return;
        }
        updateWfdInfo(false);
        if (SystemProperties.get("ro.mtk_wfd_hdcp_tx_support").equals("1") || SystemProperties.get("ro.mtk_dx_hdcp_support").equals("1")) {
            updateWifiPowerSavingMode(true);
        }
        this.mWfdEnabling = false;
        this.mWfdEnabled = false;
        reportFeatureState();
        updateScanState();
        disconnect();
        dismissDialog();
        this.mBlockMac = null;
    }

    private void resetWfdInfo() {
        this.mWfdInfo.setWfdEnabled(false);
        this.mWfdInfo.setDeviceType(0);
        this.mWfdInfo.setSessionAvailable(false);
        this.mWfdInfo.setUibcSupported(false);
        this.mWfdInfo.setContentProtected(false);
    }

    private void updateWfdInfo(boolean enable) {
        Slog.i(TAG, "updateWfdInfo(), enable:" + enable + ",mWfdEnabling:" + this.mWfdEnabling);
        resetWfdInfo();
        if (!enable) {
            this.mWfdInfo.setWfdEnabled(false);
            this.mWifiP2pManager.setWFDInfo(this.mWifiP2pChannel, this.mWfdInfo, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    if (!WifiDisplayController.DEBUG) {
                        return;
                    }
                    Slog.d(WifiDisplayController.TAG, "Successfully set WFD info.");
                }

                @Override
                public void onFailure(int reason) {
                    if (!WifiDisplayController.DEBUG) {
                        return;
                    }
                    Slog.d(WifiDisplayController.TAG, "Failed to set WFD info with reason " + reason + ".");
                }
            });
            return;
        }
        this.mWfdInfo.setWfdEnabled(true);
        if (SystemProperties.get("ro.mtk_wfd_sink_support").equals("1") && this.mSinkEnabled) {
            this.mWfdInfo.setDeviceType(1);
        } else {
            this.mWfdInfo.setDeviceType(0);
        }
        Slog.i(TAG, "Set session available as true");
        this.mWfdInfo.setSessionAvailable(true);
        this.mWfdInfo.setControlPort(DEFAULT_CONTROL_PORT);
        this.mWfdInfo.setMaxThroughput(50);
        if (!SystemProperties.get("ro.mtk_wfd_sink_support").equals("1") || !this.mSinkEnabled || SystemProperties.get("ro.mtk_wfd_sink_uibc_support").equals("1")) {
            this.mWfdInfo.setUibcSupported(true);
        } else {
            this.mWfdInfo.setUibcSupported(false);
        }
        if (SystemProperties.get("ro.mtk_wfd_hdcp_tx_support").equals("1") || SystemProperties.get("ro.mtk_dx_hdcp_support").equals("1") || SystemProperties.get("ro.mtk_wfd_hdcp_rx_support").equals("1")) {
            this.mWfdInfo.setContentProtected(true);
        }
        Slog.i(TAG, "HDCP Tx support? " + (SystemProperties.get("ro.mtk_wfd_hdcp_tx_support").equals("1") ? true : SystemProperties.get("ro.mtk_dx_hdcp_support").equals("1")) + ", our wfd info: " + this.mWfdInfo);
        Slog.i(TAG, "HDCP Rx support? " + SystemProperties.get("ro.mtk_wfd_hdcp_rx_support").equals("1") + ", our wfd info: " + this.mWfdInfo);
        if (this.mWfdEnabling) {
            this.mWifiP2pManager.setWFDInfo(this.mWifiP2pChannel, this.mWfdInfo, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Slog.d(WifiDisplayController.TAG, "Successfully set WFD info.");
                    if (!WifiDisplayController.this.mWfdEnabling) {
                        return;
                    }
                    WifiDisplayController.this.mWfdEnabling = false;
                    WifiDisplayController.this.mWfdEnabled = true;
                    WifiDisplayController.this.reportFeatureState();
                    if (SystemProperties.get("ro.mtk_wfd_support").equals("1") && WifiDisplayController.this.mAutoEnableWifi) {
                        WifiDisplayController.this.mAutoEnableWifi = false;
                        Slog.d(WifiDisplayController.TAG, "scan after enable wifi automatically.");
                    }
                    WifiDisplayController.this.updateScanState();
                }

                @Override
                public void onFailure(int reason) {
                    Slog.d(WifiDisplayController.TAG, "Failed to set WFD info with reason " + reason + ".");
                    WifiDisplayController.this.mWfdEnabling = false;
                }
            });
        } else {
            this.mWifiP2pManager.setWFDInfo(this.mWifiP2pChannel, this.mWfdInfo, null);
        }
    }

    private void reportFeatureState() {
        final int featureState = computeFeatureState();
        Slog.d(TAG, "reportFeatureState(), featureState = " + featureState);
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                Slog.d(WifiDisplayController.TAG, "callback onFeatureStateChanged(): featureState = " + featureState);
                WifiDisplayController.this.mListener.onFeatureStateChanged(featureState);
            }
        });
    }

    private int computeFeatureState() {
        if (!this.mWifiP2pEnabled) {
            if (SystemProperties.get("ro.mtk_wfd_support").equals("1")) {
                if (this.mWifiDisplayOnSetting) {
                    Slog.d(TAG, "Wifi p2p is disabled, update WIFI_DISPLAY_ON as false.");
                    Settings.Global.putInt(this.mContext.getContentResolver(), "wifi_display_on", 0);
                    this.mWifiDisplayOnSetting = false;
                }
            } else {
                return 1;
            }
        }
        return this.mWifiDisplayOnSetting ? 3 : 2;
    }

    private void updateScanState() {
        Slog.i(TAG, "updateScanState(), mSinkEnabled:" + this.mSinkEnabled + "mDiscoverPeersInProgress:" + this.mDiscoverPeersInProgress);
        if (SystemProperties.get("ro.mtk_wfd_sink_support").equals("1") && this.mSinkEnabled) {
            return;
        }
        if ((this.mScanRequested && this.mWfdEnabled && this.mDesiredDevice == null) || this.mReScanning) {
            if (!this.mDiscoverPeersInProgress) {
                Slog.i(TAG, "Starting Wifi display scan.");
                this.mDiscoverPeersInProgress = true;
                handleScanStarted();
                tryDiscoverPeers();
                return;
            }
            this.mHandler.removeCallbacks(this.mDiscoverPeers);
            this.mHandler.postDelayed(this.mDiscoverPeers, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
            return;
        }
        if (!this.mDiscoverPeersInProgress) {
            return;
        }
        this.mHandler.removeCallbacks(this.mDiscoverPeers);
        if (this.mDesiredDevice != null && this.mDesiredDevice != this.mConnectedDevice) {
            return;
        }
        Slog.i(TAG, "Stopping Wifi display scan.");
        this.mDiscoverPeersInProgress = false;
        stopPeerDiscovery();
        handleScanFinished();
    }

    private void tryDiscoverPeers() {
        Slog.d(TAG, "tryDiscoverPeers()");
        this.mWifiP2pManager.discoverPeers(this.mWifiP2pChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                if (WifiDisplayController.DEBUG) {
                    Slog.d(WifiDisplayController.TAG, "Discover peers succeeded.  Requesting peers now.");
                }
                if (!WifiDisplayController.this.mDiscoverPeersInProgress) {
                    return;
                }
                WifiDisplayController.this.requestPeers();
            }

            @Override
            public void onFailure(int reason) {
                if (!WifiDisplayController.DEBUG) {
                    return;
                }
                Slog.d(WifiDisplayController.TAG, "Discover peers failed with reason " + reason + ".");
            }
        });
        if (this.mHandler.hasCallbacks(this.mDiscoverPeers)) {
            this.mHandler.removeCallbacks(this.mDiscoverPeers);
        }
        if (this.mReScanning) {
            Slog.d(TAG, "mReScanning is true. post mDiscoverPeers every 2s");
            this.mHandler.postDelayed(this.mDiscoverPeers, 2000L);
        } else {
            this.mHandler.postDelayed(this.mDiscoverPeers, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
        }
    }

    private void stopPeerDiscovery() {
        this.mWifiP2pManager.stopPeerDiscovery(this.mWifiP2pChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                if (!WifiDisplayController.DEBUG) {
                    return;
                }
                Slog.d(WifiDisplayController.TAG, "Stop peer discovery succeeded.");
            }

            @Override
            public void onFailure(int reason) {
                if (!WifiDisplayController.DEBUG) {
                    return;
                }
                Slog.d(WifiDisplayController.TAG, "Stop peer discovery failed with reason " + reason + ".");
            }
        });
    }

    private void requestPeers() {
        this.mWifiP2pManager.requestPeers(this.mWifiP2pChannel, new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList peers) {
                if (SystemProperties.get("ro.mtk_wfd_sink_support").equals("1") && WifiDisplayController.this.mSinkEnabled) {
                    return;
                }
                if (WifiDisplayController.DEBUG) {
                    Slog.d(WifiDisplayController.TAG, "Received list of peers. mDiscoverPeersInProgress=" + WifiDisplayController.this.mDiscoverPeersInProgress);
                }
                WifiDisplayController.this.mAvailableWifiDisplayPeers.clear();
                for (WifiP2pDevice device : peers.getDeviceList()) {
                    if (WifiDisplayController.DEBUG) {
                        Slog.d(WifiDisplayController.TAG, "  " + WifiDisplayController.describeWifiP2pDevice(device));
                    }
                    if (WifiDisplayController.this.mConnectedDevice != null && WifiDisplayController.this.mConnectedDevice.deviceAddress.equals(device.deviceAddress)) {
                        WifiDisplayController.this.mAvailableWifiDisplayPeers.add(device);
                    } else if (WifiDisplayController.this.mConnectingDevice != null && WifiDisplayController.this.mConnectingDevice.deviceAddress.equals(device.deviceAddress)) {
                        WifiDisplayController.this.mAvailableWifiDisplayPeers.add(device);
                    } else if (WifiDisplayController.this.mBlockMac != null && device.deviceAddress.equals(WifiDisplayController.this.mBlockMac)) {
                        Slog.i(WifiDisplayController.TAG, "Block scan result on block mac:" + WifiDisplayController.this.mBlockMac);
                    } else if (WifiDisplayController.isWifiDisplay(device)) {
                        WifiDisplayController.this.mAvailableWifiDisplayPeers.add(device);
                    }
                }
                WifiDisplayController.this.handleScanResults();
            }
        });
    }

    private void handleScanStarted() {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                Slog.d(WifiDisplayController.TAG, "callback onScanStarted()");
                WifiDisplayController.this.mListener.onScanStarted();
            }
        });
    }

    private void handleScanResults() {
        final int count = this.mAvailableWifiDisplayPeers.size();
        final WifiDisplay[] displays = (WifiDisplay[]) WifiDisplay.CREATOR.newArray(count);
        for (int i = 0; i < count; i++) {
            WifiP2pDevice device = this.mAvailableWifiDisplayPeers.get(i);
            displays[i] = createWifiDisplay(device);
            updateDesiredDevice(device);
        }
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                Slog.d(WifiDisplayController.TAG, "callback onScanResults(), count = " + count);
                if (WifiDisplayController.DEBUG) {
                    for (int i2 = 0; i2 < count; i2++) {
                        Slog.d(WifiDisplayController.TAG, "\t" + displays[i2].getDeviceName() + ": " + displays[i2].getDeviceAddress());
                    }
                }
                WifiDisplayController.this.mListener.onScanResults(displays);
            }
        });
    }

    private void handleScanFinished() {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                WifiDisplayController.this.mListener.onScanFinished();
            }
        });
    }

    private void updateDesiredDevice(WifiP2pDevice device) {
        String address = device.deviceAddress;
        if (this.mDesiredDevice == null || !this.mDesiredDevice.deviceAddress.equals(address)) {
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, "updateDesiredDevice: new information " + describeWifiP2pDevice(device));
        }
        this.mDesiredDevice.update(device);
        if (this.mAdvertisedDisplay == null || !this.mAdvertisedDisplay.getDeviceAddress().equals(address)) {
            return;
        }
        readvertiseDisplay(createWifiDisplay(this.mDesiredDevice));
    }

    private void connect(WifiP2pDevice device) {
        Slog.i(TAG, "connect: device name = " + device.deviceName);
        if (this.mDesiredDevice != null && !this.mDesiredDevice.deviceAddress.equals(device.deviceAddress)) {
            if (DEBUG) {
                Slog.d(TAG, "connect: nothing to do, already connecting to " + describeWifiP2pDevice(this.mDesiredDevice));
                return;
            }
            return;
        }
        if (this.mDesiredDevice != null && this.mDesiredDevice.deviceAddress.equals(device.deviceAddress)) {
            if (DEBUG) {
                Slog.d(TAG, "connect: connecting to the same dongle already " + describeWifiP2pDevice(this.mDesiredDevice));
            }
        } else if (this.mConnectedDevice != null && !this.mConnectedDevice.deviceAddress.equals(device.deviceAddress) && this.mDesiredDevice == null) {
            if (DEBUG) {
                Slog.d(TAG, "connect: nothing to do, already connected to " + describeWifiP2pDevice(device) + " and not part way through connecting to a different device.");
            }
        } else {
            if (!this.mWfdEnabled) {
                Slog.i(TAG, "Ignoring request to connect to Wifi display because the  feature is currently disabled: " + device.deviceName);
                return;
            }
            this.mDesiredDevice = device;
            this.mConnectionRetriesLeft = 0;
            updateConnection();
        }
    }

    private void disconnect() {
        Slog.i(TAG, "disconnect, mRemoteDisplayInterface = " + this.mRemoteDisplayInterface);
        if (SystemProperties.get("ro.mtk_wfd_sink_support").equals("1") && this.mSinkEnabled) {
            disconnectWfdSink();
            return;
        }
        this.mDesiredDevice = null;
        updateWfdStatFile(0);
        if (this.mConnectedDevice != null) {
            this.mReConnectDevice = this.mConnectedDevice;
        }
        updateConnection();
    }

    private void retryConnection() {
        this.mDesiredDevice = new WifiP2pDevice(this.mDesiredDevice);
        updateConnection();
    }

    private void updateConnection() {
        updateScanState();
        if ((this.mRemoteDisplay != null && this.mConnectedDevice != this.mDesiredDevice) || this.mIsConnecting_P2p_Rtsp) {
            String localInterface = this.mRemoteDisplayInterface != null ? this.mRemoteDisplayInterface : "localhost";
            String localDeviceName = this.mConnectedDevice != null ? this.mConnectedDevice.deviceName : this.mConnectingDevice != null ? this.mConnectingDevice.deviceName : "N/A";
            Slog.i(TAG, "Stopped listening for RTSP connection on " + localInterface + " from Wifi display : " + localDeviceName);
            this.mIsConnected_OtherP2p = false;
            this.mIsConnecting_P2p_Rtsp = false;
            Slog.i(TAG, "\tbefore dispose() ---> ");
            this.mListener.onDisplayDisconnecting();
            this.mRemoteDisplay.dispose();
            Slog.i(TAG, "\t<--- after dispose()");
            this.mRemoteDisplay = null;
            this.mRemoteDisplayInterface = null;
            this.mRemoteDisplayConnected = false;
            this.mHandler.removeCallbacks(this.mRtspTimeout);
            this.mWifiP2pManager.setMiracastMode(0);
            unadvertiseDisplay();
        }
        if (this.mDisconnectingDevice != null) {
            return;
        }
        if (this.mConnectedDevice != null && this.mConnectedDevice != this.mDesiredDevice) {
            Slog.i(TAG, "Disconnecting from Wifi display: " + this.mConnectedDevice.deviceName);
            this.mDisconnectingDevice = this.mConnectedDevice;
            this.mConnectedDevice = null;
            this.mConnectedDeviceGroupInfo = null;
            unadvertiseDisplay();
            final WifiP2pDevice oldDevice = this.mDisconnectingDevice;
            this.mWifiP2pManager.removeGroup(this.mWifiP2pChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Slog.i(WifiDisplayController.TAG, "Disconnected from Wifi display: " + oldDevice.deviceName);
                    next();
                }

                @Override
                public void onFailure(int reason) {
                    Slog.i(WifiDisplayController.TAG, "Failed to disconnect from Wifi display: " + oldDevice.deviceName + ", reason=" + reason);
                    next();
                }

                private void next() {
                    if (WifiDisplayController.this.mDisconnectingDevice != oldDevice) {
                        return;
                    }
                    WifiDisplayController.this.mDisconnectingDevice = null;
                    if (WifiDisplayController.this.mRemoteDisplay != null) {
                        WifiDisplayController.this.mIsConnecting_P2p_Rtsp = true;
                    }
                    WifiDisplayController.this.updateConnection();
                }
            });
            return;
        }
        if (this.mCancelingDevice != null) {
            return;
        }
        if (this.mConnectingDevice != null && this.mConnectingDevice != this.mDesiredDevice) {
            Slog.i(TAG, "Canceling connection to Wifi display: " + this.mConnectingDevice.deviceName);
            this.mCancelingDevice = this.mConnectingDevice;
            this.mConnectingDevice = null;
            unadvertiseDisplay();
            this.mHandler.removeCallbacks(this.mConnectionTimeout);
            final WifiP2pDevice oldDevice2 = this.mCancelingDevice;
            this.mWifiP2pManager.cancelConnect(this.mWifiP2pChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Slog.i(WifiDisplayController.TAG, "Canceled connection to Wifi display: " + oldDevice2.deviceName);
                    next();
                }

                @Override
                public void onFailure(int reason) {
                    Slog.i(WifiDisplayController.TAG, "Failed to cancel connection to Wifi display: " + oldDevice2.deviceName + ", reason=" + reason + ". Do removeGroup()");
                    WifiDisplayController.this.mWifiP2pManager.removeGroup(WifiDisplayController.this.mWifiP2pChannel, null);
                    next();
                }

                private void next() {
                    if (WifiDisplayController.this.mCancelingDevice != oldDevice2) {
                        return;
                    }
                    WifiDisplayController.this.mCancelingDevice = null;
                    if (WifiDisplayController.this.mRemoteDisplay != null) {
                        WifiDisplayController.this.mIsConnecting_P2p_Rtsp = true;
                    }
                    WifiDisplayController.this.updateConnection();
                }
            });
            return;
        }
        if (this.mDesiredDevice == null) {
            if (this.mWifiDisplayCertMode) {
                this.mListener.onDisplaySessionInfo(getSessionInfo(this.mConnectedDeviceGroupInfo, 0));
            }
            unadvertiseDisplay();
            return;
        }
        if (this.mConnectedDevice != null || this.mConnectingDevice != null) {
            if (this.mConnectedDevice == null || this.mRemoteDisplay != null) {
                return;
            }
            Inet4Address addr = getInterfaceAddress(this.mConnectedDeviceGroupInfo);
            if (addr == null) {
                Slog.i(TAG, "Failed to get local interface address for communicating with Wifi display: " + this.mConnectedDevice.deviceName);
                handleConnectionFailure(false);
                return;
            }
            return;
        }
        Slog.i(TAG, "Connecting to Wifi display: " + this.mDesiredDevice.deviceName);
        this.mConnectingDevice = this.mDesiredDevice;
        WifiP2pConfig config = new WifiP2pConfig();
        WpsInfo wps = new WpsInfo();
        if (this.mWifiDisplayWpsConfig != 4) {
            wps.setup = this.mWifiDisplayWpsConfig;
        } else if (this.mConnectingDevice.wpsPbcSupported()) {
            wps.setup = 0;
        } else if (this.mConnectingDevice.wpsDisplaySupported()) {
            wps.setup = 2;
        } else if (this.mConnectingDevice.wpsKeypadSupported()) {
            wps.setup = 1;
        } else {
            wps.setup = 0;
        }
        config.wps = wps;
        config.deviceAddress = this.mConnectingDevice.deviceAddress;
        String goIntent = SystemProperties.get("wfd.source.go_intent", String.valueOf(0));
        config.groupOwnerIntent = Integer.valueOf(goIntent).intValue();
        Slog.i(TAG, "Source go_intent:" + config.groupOwnerIntent);
        WifiDisplay display = createWifiDisplay(this.mConnectingDevice);
        advertiseDisplay(display, null, 0, 0, 0);
        updateWfdInfo(true);
        setAutoChannelSelection();
        enterCCState(ChannelConflictState.STATE_IDLE);
        this.mWifiP2pManager.setMiracastMode(1);
        stopWifiScan(true);
        final WifiP2pDevice newDevice = this.mDesiredDevice;
        this.mWifiP2pManager.connect(this.mWifiP2pChannel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Slog.i(WifiDisplayController.TAG, "Initiated connection to Wifi display: " + newDevice.deviceName);
                WifiDisplayController.this.mHandler.postDelayed(WifiDisplayController.this.mConnectionTimeout, 60000L);
            }

            @Override
            public void onFailure(int reason) {
                if (WifiDisplayController.this.mConnectingDevice != newDevice) {
                    return;
                }
                Slog.i(WifiDisplayController.TAG, "Failed to initiate connection to Wifi display: " + newDevice.deviceName + ", reason=" + reason);
                WifiDisplayController.this.mConnectingDevice = null;
                WifiDisplayController.this.handleConnectionFailure(false);
            }
        });
        this.mRTSPConnecting = true;
        final WifiP2pDevice oldDevice3 = this.mConnectingDevice;
        int port = getPortNumber(this.mConnectingDevice);
        String iface = "127.0.0.1:" + port;
        this.mRemoteDisplayInterface = iface;
        Slog.i(TAG, "Listening for RTSP connection on " + iface + " from Wifi display: " + this.mConnectingDevice.deviceName + " , Speed-Up rtsp setup, DRM Content isPlaying = " + this.mDRMContent_Mediaplayer);
        this.mRemoteDisplay = RemoteDisplay.listen(iface, new RemoteDisplay.Listener() {
            public void onDisplayConnected(Surface surface, int width, int height, int flags, int session) {
                if (WifiDisplayController.this.mConnectingDevice != null) {
                    WifiDisplayController.this.mConnectedDevice = WifiDisplayController.this.mConnectingDevice;
                }
                if ((WifiDisplayController.this.mConnectedDevice != oldDevice3 || WifiDisplayController.this.mRemoteDisplayConnected) && WifiDisplayController.DEBUG) {
                    Slog.e(WifiDisplayController.TAG, "!!RTSP connected condition GOT Trobule:\nmConnectedDevice: " + WifiDisplayController.this.mConnectedDevice + "\noldDevice: " + oldDevice3 + "\nmRemoteDisplayConnected: " + WifiDisplayController.this.mRemoteDisplayConnected);
                }
                if (WifiDisplayController.this.mConnectedDevice != null && oldDevice3 != null && WifiDisplayController.this.mConnectedDevice.deviceAddress.equals(oldDevice3.deviceAddress) && !WifiDisplayController.this.mRemoteDisplayConnected) {
                    Slog.i(WifiDisplayController.TAG, "Opened RTSP connection with Wifi display: " + WifiDisplayController.this.mConnectedDevice.deviceName);
                    WifiDisplayController.this.mRemoteDisplayConnected = true;
                    WifiDisplayController.this.mHandler.removeCallbacks(WifiDisplayController.this.mRtspTimeout);
                    if (WifiDisplayController.this.mWifiDisplayCertMode) {
                        WifiDisplayController.this.mListener.onDisplaySessionInfo(WifiDisplayController.this.getSessionInfo(WifiDisplayController.this.mConnectedDeviceGroupInfo, session));
                    }
                    WifiDisplayController.this.updateWfdStatFile(2);
                    WifiDisplay display2 = WifiDisplayController.createWifiDisplay(WifiDisplayController.this.mConnectedDevice);
                    WifiDisplayController.this.advertiseDisplay(display2, surface, width, height, flags);
                }
                WifiDisplayController.this.mRTSPConnecting = false;
            }

            public void onDisplayDisconnected() {
                if (WifiDisplayController.this.mConnectedDevice == oldDevice3) {
                    Slog.i(WifiDisplayController.TAG, "Closed RTSP connection with Wifi display: " + WifiDisplayController.this.mConnectedDevice.deviceName);
                    WifiDisplayController.this.mHandler.removeCallbacks(WifiDisplayController.this.mRtspTimeout);
                    WifiDisplayController.this.disconnect();
                }
                WifiDisplayController.this.mRTSPConnecting = false;
            }

            public void onDisplayError(int error) {
                if (WifiDisplayController.this.mConnectedDevice == oldDevice3) {
                    Slog.i(WifiDisplayController.TAG, "Lost RTSP connection with Wifi display due to error " + error + ": " + WifiDisplayController.this.mConnectedDevice.deviceName);
                    WifiDisplayController.this.mHandler.removeCallbacks(WifiDisplayController.this.mRtspTimeout);
                    WifiDisplayController.this.handleConnectionFailure(false);
                }
                WifiDisplayController.this.mRTSPConnecting = false;
            }

            public void onDisplayKeyEvent(int uniCode, int flags) {
                Slog.d(WifiDisplayController.TAG, "onDisplayKeyEvent:uniCode=" + uniCode);
                if (WifiDisplayController.this.mInputMethodManager == null) {
                    return;
                }
                try {
                    if (WifiDisplayController.this.mWakeLock != null) {
                        WifiDisplayController.this.mWakeLock.acquire();
                    }
                    WifiDisplayController.this.mInputMethodManager.sendCharacterToCurClient(uniCode);
                    if (WifiDisplayController.this.mWakeLock == null) {
                        return;
                    }
                    WifiDisplayController.this.mWakeLock.release();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            public void onDisplayGenericMsgEvent(int event) {
                Slog.d(WifiDisplayController.TAG, "onDisplayGenericMsgEvent: " + event);
            }
        }, this.mHandler, this.mContext.getOpPackageName());
        int rtspTimeout = this.mWifiDisplayCertMode ? RTSP_TIMEOUT_SECONDS_CERT_MODE : 75;
        this.mHandler.postDelayed(this.mRtspTimeout, rtspTimeout * 1000);
    }

    private WifiDisplaySessionInfo getSessionInfo(WifiP2pGroup info, int session) {
        if (info == null) {
            return null;
        }
        Inet4Address addr = getInterfaceAddress(info);
        WifiDisplaySessionInfo sessionInfo = new WifiDisplaySessionInfo(!info.getOwner().deviceAddress.equals(this.mThisDevice.deviceAddress), session, info.getOwner().deviceAddress + " " + info.getNetworkName(), info.getPassphrase(), addr != null ? addr.getHostAddress() : "");
        if (DEBUG) {
            Slog.d(TAG, sessionInfo.toString());
        }
        return sessionInfo;
    }

    private void handleStateChanged(boolean enabled) {
        this.mWifiP2pEnabled = enabled;
        updateWfdEnableState();
        if (enabled) {
            return;
        }
        dismissDialog();
    }

    private void handlePeersChanged() {
        requestPeers();
    }

    private void handleConnectionChanged(NetworkInfo networkInfo, int reason) {
        Slog.i(TAG, "handleConnectionChanged(), mWfdEnabled:" + this.mWfdEnabled);
        this.mNetworkInfo = networkInfo;
        if (this.mWfdEnabled && networkInfo.isConnected()) {
            if (this.mDesiredDevice != null || this.mWifiDisplayCertMode) {
                this.mWifiP2pManager.requestGroupInfo(this.mWifiP2pChannel, new WifiP2pManager.GroupInfoListener() {
                    @Override
                    public void onGroupInfoAvailable(WifiP2pGroup info) {
                        if (info == null) {
                            Slog.i(WifiDisplayController.TAG, "Error: group is null !!!");
                            return;
                        }
                        if (WifiDisplayController.DEBUG) {
                            Slog.d(WifiDisplayController.TAG, "Received group info: " + WifiDisplayController.describeWifiP2pGroup(info));
                        }
                        if (WifiDisplayController.this.mConnectingDevice != null && !info.contains(WifiDisplayController.this.mConnectingDevice)) {
                            Slog.i(WifiDisplayController.TAG, "Aborting connection to Wifi display because the current P2P group does not contain the device we expected to find: " + WifiDisplayController.this.mConnectingDevice.deviceName + ", group info was: " + WifiDisplayController.describeWifiP2pGroup(info));
                            WifiDisplayController.this.handleConnectionFailure(false);
                            return;
                        }
                        if (WifiDisplayController.this.mDesiredDevice != null && !info.contains(WifiDisplayController.this.mDesiredDevice)) {
                            Slog.i(WifiDisplayController.TAG, "Aborting connection to Wifi display because the current P2P group does not contain the device we desired to find: " + WifiDisplayController.this.mDesiredDevice.deviceName + ", group info was: " + WifiDisplayController.describeWifiP2pGroup(info));
                            WifiDisplayController.this.disconnect();
                            return;
                        }
                        if (WifiDisplayController.this.mWifiDisplayCertMode) {
                            boolean owner = info.getOwner().deviceAddress.equals(WifiDisplayController.this.mThisDevice.deviceAddress);
                            if (owner && info.getClientList().isEmpty()) {
                                WifiDisplayController.this.mConnectingDevice = WifiDisplayController.this.mDesiredDevice = null;
                                WifiDisplayController.this.mConnectedDeviceGroupInfo = info;
                                WifiDisplayController.this.updateConnection();
                            } else if (WifiDisplayController.this.mConnectingDevice == null && WifiDisplayController.this.mDesiredDevice == null) {
                                WifiDisplayController.this.mConnectingDevice = WifiDisplayController.this.mDesiredDevice = owner ? info.getClientList().iterator().next() : info.getOwner();
                            }
                        }
                        if (WifiDisplayController.this.mConnectingDevice == null || WifiDisplayController.this.mConnectingDevice != WifiDisplayController.this.mDesiredDevice) {
                            return;
                        }
                        Slog.i(WifiDisplayController.TAG, "Connected to Wifi display: " + WifiDisplayController.this.mConnectingDevice.deviceName);
                        WifiDisplayController.this.mHandler.removeCallbacks(WifiDisplayController.this.mConnectionTimeout);
                        WifiDisplayController.this.mConnectedDeviceGroupInfo = info;
                        WifiDisplayController.this.mConnectedDevice = WifiDisplayController.this.mConnectingDevice;
                        WifiDisplayController.this.mConnectingDevice = null;
                        WifiDisplayController.this.updateWfdStatFile(1);
                        WifiDisplayController.this.updateConnection();
                        WifiDisplayController.this.handleChannelConflictProcedure(ChannelConflictEvt.EVT_WFD_P2P_CONNECTED);
                    }
                });
            }
        } else {
            this.mConnectedDeviceGroupInfo = null;
            if (this.mConnectingDevice != null || this.mConnectedDevice != null) {
                disconnect();
            }
            if (this.mWfdEnabled) {
                requestPeers();
                if (this.mLastTimeConnected && this.mReconnectForResolutionChange) {
                    Slog.i(TAG, "requestStartScan() for resolution change.");
                    this.mReScanning = true;
                    updateScanState();
                    this.mReConnection_Timeout_Remain_Seconds = 60;
                    this.mHandler.postDelayed(this.mReConnect, 1000L);
                }
            }
            this.mReconnectForResolutionChange = false;
            if (7 == reason && this.mReConnectDevice != null) {
                Slog.i(TAG, "reconnect procedure start, ReConnectDevice = " + this.mReConnectDevice);
                dialogReconnect();
            }
            handleChannelConflictProcedure(ChannelConflictEvt.EVT_WFD_P2P_DISCONNECTED);
        }
        if (this.mDesiredDevice != null) {
            return;
        }
        this.mIsConnected_OtherP2p = networkInfo.isConnected();
        if (!this.mIsConnected_OtherP2p) {
            return;
        }
        Slog.w(TAG, "Wifi P2p connection is connected but it does not wifidisplay trigger");
        resetReconnectVariable();
    }

    private void handleConnectionFailure(boolean timeoutOccurred) {
        Slog.i(TAG, "Wifi display connection failed!");
        if (this.mDesiredDevice == null) {
            return;
        }
        if (this.mConnectionRetriesLeft > 0) {
            final WifiP2pDevice oldDevice = this.mDesiredDevice;
            this.mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (WifiDisplayController.this.mDesiredDevice != oldDevice || WifiDisplayController.this.mConnectionRetriesLeft <= 0) {
                        return;
                    }
                    WifiDisplayController wifiDisplayController = WifiDisplayController.this;
                    wifiDisplayController.mConnectionRetriesLeft--;
                    Slog.i(WifiDisplayController.TAG, "Retrying Wifi display connection.  Retries left: " + WifiDisplayController.this.mConnectionRetriesLeft);
                    WifiDisplayController.this.retryConnection();
                }
            }, timeoutOccurred ? 0 : 500);
        } else {
            disconnect();
        }
    }

    private void advertiseDisplay(final WifiDisplay display, final Surface surface, final int width, final int height, final int flags) {
        if (DEBUG) {
            Slog.d(TAG, "advertiseDisplay(): ----->\n\tdisplay: " + display + "\n\tsurface: " + surface + "\n\twidth: " + width + "\n\theight: " + height + "\n\tflags: " + flags);
        }
        if (Objects.equal(this.mAdvertisedDisplay, display) && this.mAdvertisedDisplaySurface == surface && this.mAdvertisedDisplayWidth == width && this.mAdvertisedDisplayHeight == height && this.mAdvertisedDisplayFlags == flags) {
            if (DEBUG) {
                Slog.d(TAG, "advertiseDisplay() : no need update!");
                return;
            }
            return;
        }
        final WifiDisplay oldDisplay = this.mAdvertisedDisplay;
        final Surface oldSurface = this.mAdvertisedDisplaySurface;
        this.mAdvertisedDisplay = display;
        this.mAdvertisedDisplaySurface = surface;
        this.mAdvertisedDisplayWidth = width;
        this.mAdvertisedDisplayHeight = height;
        this.mAdvertisedDisplayFlags = flags;
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (WifiDisplayController.DEBUG) {
                    Slog.d(WifiDisplayController.TAG, "oldSurface = " + oldSurface + ", surface = " + surface + ", oldDisplay = " + oldDisplay + ", display = " + display);
                }
                if (oldSurface != null && surface != oldSurface) {
                    Slog.d(WifiDisplayController.TAG, "callback onDisplayDisconnected()");
                    WifiDisplayController.this.mListener.onDisplayDisconnected();
                    WifiDisplayController.this.actionAtDisconnected(oldDisplay);
                } else if (oldDisplay != null && !oldDisplay.hasSameAddress(display)) {
                    Slog.d(WifiDisplayController.TAG, "callback onDisplayConnectionFailed()");
                    WifiDisplayController.this.mListener.onDisplayConnectionFailed();
                    WifiDisplayController.this.actionAtConnectionFailed();
                }
                if (display != null) {
                    if (!display.hasSameAddress(oldDisplay)) {
                        Slog.d(WifiDisplayController.TAG, "callback onDisplayConnecting(): display = " + display);
                        WifiDisplayController.this.mListener.onDisplayConnecting(display);
                        WifiDisplayController.this.actionAtConnecting();
                    } else if (!display.equals(oldDisplay)) {
                        WifiDisplayController.this.mListener.onDisplayChanged(display);
                    }
                    if (surface == null || surface == oldSurface) {
                        return;
                    }
                    WifiDisplayController.this.updateIfHdcp(flags);
                    Slog.d(WifiDisplayController.TAG, "callback onDisplayConnected(): display = " + display + ", surface = " + surface + ", width = " + width + ", height = " + height + ", flags = " + flags);
                    WifiDisplayController.this.mListener.onDisplayConnected(display, surface, width, height, flags);
                    WifiDisplayController.this.actionAtConnected(display, flags, width < height);
                }
            }
        });
    }

    private void unadvertiseDisplay() {
        advertiseDisplay(null, null, 0, 0, 0);
    }

    private void readvertiseDisplay(WifiDisplay display) {
        advertiseDisplay(display, this.mAdvertisedDisplaySurface, this.mAdvertisedDisplayWidth, this.mAdvertisedDisplayHeight, this.mAdvertisedDisplayFlags);
    }

    private static Inet4Address getInterfaceAddress(WifiP2pGroup info) {
        try {
            NetworkInterface iface = NetworkInterface.getByName(info.getInterface());
            Enumeration<InetAddress> addrs = iface.getInetAddresses();
            while (addrs.hasMoreElements()) {
                InetAddress addr = addrs.nextElement();
                if (addr instanceof Inet4Address) {
                    return (Inet4Address) addr;
                }
            }
            Slog.w(TAG, "Could not obtain address of network interface " + info.getInterface() + " because it had no IPv4 addresses.");
            return null;
        } catch (SocketException ex) {
            Slog.w(TAG, "Could not obtain address of network interface " + info.getInterface(), ex);
            return null;
        }
    }

    private static int getPortNumber(WifiP2pDevice device) {
        if (device.deviceName.startsWith("DIRECT-") && device.deviceName.endsWith("Broadcom")) {
            return 8554;
        }
        return DEFAULT_CONTROL_PORT;
    }

    private static boolean isWifiDisplay(WifiP2pDevice device) {
        if (device.wfdInfo != null && device.wfdInfo.isWfdEnabled() && device.wfdInfo.isSessionAvailable()) {
            return isPrimarySinkDeviceType(device.wfdInfo.getDeviceType());
        }
        return false;
    }

    private static boolean isPrimarySinkDeviceType(int deviceType) {
        return deviceType == 1 || deviceType == 3;
    }

    private static String describeWifiP2pDevice(WifiP2pDevice device) {
        return device != null ? device.toString().replace('\n', ',') : "null";
    }

    private static String describeWifiP2pGroup(WifiP2pGroup group) {
        return group != null ? group.toString().replace('\n', ',') : "null";
    }

    private static WifiDisplay createWifiDisplay(WifiP2pDevice device) {
        return new WifiDisplay(device.deviceAddress, device.deviceName, (String) null, true, device.wfdInfo.isSessionAvailable(), false);
    }

    private void sendKeyEvent(int keyCode, int isDown) {
        long now = SystemClock.uptimeMillis();
        if (isDown == 1) {
            injectKeyEvent(new KeyEvent(now, now, 0, translateAsciiToKeyCode(keyCode), 0, 0, -1, 0, 0, 257));
        } else {
            injectKeyEvent(new KeyEvent(now, now, 1, translateAsciiToKeyCode(keyCode), 0, 0, -1, 0, 0, 257));
        }
    }

    private void sendTap(float x, float y) {
        long now = SystemClock.uptimeMillis();
        injectPointerEvent(MotionEvent.obtain(now, now, 0, x, y, 0));
        injectPointerEvent(MotionEvent.obtain(now, now, 1, x, y, 0));
    }

    private void injectKeyEvent(KeyEvent event) {
        Slog.d(TAG, "InjectKeyEvent: " + event);
        InputManager.getInstance().injectInputEvent(event, 2);
    }

    private void injectPointerEvent(MotionEvent event) {
        event.setSource(4098);
        Slog.d("Input", "InjectPointerEvent: " + event);
        InputManager.getInstance().injectInputEvent(event, 2);
    }

    private int translateSpecialCode(int ascii) {
        switch (ascii) {
            case 8:
                return 67;
            case 12:
                return 66;
            case 13:
                return 66;
            case 16:
                return 59;
            case 20:
                return HdmiCecKeycode.CEC_KEYCODE_F3_GREEN;
            case WindowManagerService.H.DO_DISPLAY_ADDED:
                return 111;
            case 32:
                return 62;
            case 33:
                return 93;
            case 34:
                return 92;
            case 37:
                return 19;
            case 38:
                return 20;
            case 39:
                return 22;
            case 40:
                return 21;
            case 186:
                return 74;
            case 187:
                return 70;
            case 188:
                return 55;
            case 189:
                return 69;
            case 190:
                return 56;
            case 191:
                return 76;
            case 192:
                return 68;
            case NetworkManagementService.NetdResponseCode.InterfaceTxThrottleResult:
                return 71;
            case NetworkManagementService.NetdResponseCode.QuotaCounterResult:
                return 73;
            case NetworkManagementService.NetdResponseCode.TetheringStatsResult:
                return 72;
            case NetworkManagementService.NetdResponseCode.DnsProxyQueryResult:
                return 75;
            default:
                return 0;
        }
    }

    private int translateAsciiToKeyCode(int ascii) {
        if (ascii >= 48 && ascii <= 57) {
            return ascii - 41;
        }
        if (ascii >= 65 && ascii <= 90) {
            return ascii - 36;
        }
        int newKeyCode = translateSpecialCode(ascii);
        if (newKeyCode > 0) {
            Slog.d(TAG, "special code: " + ascii + ":" + newKeyCode);
            return newKeyCode;
        }
        Slog.d(TAG, "translateAsciiToKeyCode: ascii is not supported" + ascii);
        return 0;
    }

    private void getWifiLock() {
        if (this.mWifiManager == null) {
            this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        }
        if (this.mWifiLock != null || this.mWifiManager == null) {
            return;
        }
        this.mWifiLock = this.mWifiManager.createWifiLock(1, "WFD_WifiLock");
    }

    private void updateIfHdcp(int flags) {
        boolean secure = (flags & 1) != 0;
        if (secure) {
            SystemProperties.set("media.wfd.hdcp", "1");
        } else {
            SystemProperties.set("media.wfd.hdcp", "0");
        }
    }

    private void stopWifiScan(boolean ifStop) {
        if (this.mStopWifiScan == ifStop) {
            return;
        }
        Slog.i(TAG, "stopWifiScan()," + ifStop);
        this.mWifiManager.stopReconnectAndScan(ifStop, 0, true);
        this.mStopWifiScan = ifStop;
    }

    private void actionAtConnected(WifiDisplay display, int flags, boolean portrait) {
        Slog.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName());
        this.mIsWFDConnected = true;
        Intent intent = new Intent(WFD_CONNECTION);
        intent.addFlags(67108864);
        intent.putExtra("connected", 1);
        if (display != null) {
            intent.putExtra("device_address", display.getDeviceAddress());
            intent.putExtra("device_name", display.getDeviceName());
            intent.putExtra("device_alias", display.getDeviceAlias());
        } else {
            Slog.e(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + ", null display");
            intent.putExtra("device_address", "00:00:00:00:00:00");
            intent.putExtra("device_name", "wifidisplay dongle");
            intent.putExtra("device_alias", "wifidisplay dongle");
        }
        boolean secure = (flags & 1) != 0;
        if (secure) {
            intent.putExtra("secure", 1);
        } else {
            intent.putExtra("secure", 0);
        }
        Slog.i(TAG, "secure:" + secure);
        int usingUIBC = this.mRemoteDisplay.getWfdParam(8);
        if ((usingUIBC & 1) != 0 || (usingUIBC & 2) != 0) {
            intent.putExtra("uibc_touch_mouse", 1);
        } else {
            intent.putExtra("uibc_touch_mouse", 0);
        }
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        if (this.mReConnecting) {
            resetReconnectVariable();
        }
        getWifiLock();
        if (this.mWifiManager != null && this.mWifiLock != null) {
            if (!this.mWifiLock.isHeld()) {
                if (DEBUG) {
                    Slog.i(TAG, "acquire wifilock");
                }
                this.mWifiLock.acquire();
            } else {
                Slog.e(TAG, "WFD connected, and WifiLock is Held!");
            }
        } else {
            Slog.e(TAG, "actionAtConnected(): mWifiManager: " + this.mWifiManager + ", mWifiLock: " + this.mWifiLock);
        }
        if (this.WFDCONTROLLER_QE_ON) {
            this.mHandler.postDelayed(this.mWifiLinkInfo, 2000L);
            resetSignalParam();
        }
        if (SystemProperties.get("ro.mtk_wfd_support").equals("1")) {
            boolean show = SystemProperties.getInt("af.policy.r_submix_prio_adjust", 0) == 0;
            if (show) {
                checkA2dpStatus();
            }
            updateChosenCapability(usingUIBC, portrait);
            IBinder b = ServiceManager.getService("input_method");
            this.mInputMethodManager = IInputMethodManager.Stub.asInterface(b);
            if (this.mLatencyProfiling == 3) {
                this.mHandler.postDelayed(this.mDelayProfiling, 2000L);
            }
        }
        notifyClearMotion(true);
        if (!this.mWifiApConnected) {
            return;
        }
        checkIfWifiApIs11G();
    }

    private void actionAtDisconnected(WifiDisplay display) {
        Slog.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName());
        if (this.mIsWFDConnected && display.getDeviceName().contains("Push2TV")) {
            this.mBlockMac = display.getDeviceAddress();
            Slog.i(TAG, "Add block mac:" + this.mBlockMac);
            this.mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Slog.i(WifiDisplayController.TAG, "Remove block mac:" + WifiDisplayController.this.mBlockMac);
                    WifiDisplayController.this.mBlockMac = null;
                }
            }, 15000L);
        }
        this.mIsWFDConnected = false;
        Intent intent = new Intent(WFD_CONNECTION);
        intent.addFlags(67108864);
        intent.putExtra("connected", 0);
        if (display != null) {
            intent.putExtra("device_address", display.getDeviceAddress());
            intent.putExtra("device_name", display.getDeviceName());
            intent.putExtra("device_alias", display.getDeviceAlias());
        } else {
            Slog.e(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + ", null display");
            intent.putExtra("device_address", "00:00:00:00:00:00");
            intent.putExtra("device_name", "wifidisplay dongle");
            intent.putExtra("device_alias", "wifidisplay dongle");
        }
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        if (this.mReConnecting) {
            Toast.makeText(this.mContext, 134545542, 0).show();
            resetReconnectVariable();
        }
        getWifiLock();
        if (this.mWifiManager != null && this.mWifiLock != null) {
            if (this.mWifiLock.isHeld()) {
                if (DEBUG) {
                    Slog.i(TAG, "release wifilock");
                }
                this.mWifiLock.release();
            } else {
                Slog.e(TAG, "WFD disconnected, and WifiLock isn't Held!");
            }
        } else {
            Slog.e(TAG, "actionAtDisconnected(): mWifiManager: " + this.mWifiManager + ", mWifiLock: " + this.mWifiLock);
        }
        if (this.WFDCONTROLLER_QE_ON) {
            this.mHandler.removeCallbacks(this.mWifiLinkInfo);
        }
        clearNotify();
        if (SystemProperties.get("ro.mtk_wfd_support").equals("1")) {
            updateChosenCapability(0, false);
            stopProfilingInfo();
        }
        notifyClearMotion(false);
        stopWifiScan(false);
    }

    private void actionAtConnecting() {
        Slog.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName());
    }

    private void actionAtConnectionFailed() {
        Slog.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName());
        if (this.mReConnecting) {
            Toast.makeText(this.mContext, 134545542, 0).show();
            resetReconnectVariable();
        }
        stopWifiScan(false);
    }

    private int loadWfdWpsSetup() {
        String wfdWpsSetup = SystemProperties.get("wlan.wfd.wps.setup", "1");
        if (DEBUG) {
            Slog.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + ", wfdWpsSetup = " + wfdWpsSetup);
        }
        switch (Integer.valueOf(wfdWpsSetup).intValue()) {
            case 0:
                return 2;
            case 1:
                return 0;
            default:
                return 0;
        }
    }

    private void loadDebugLevel() {
        String debugLevel = SystemProperties.get("wlan.wfd.controller.debug", "0");
        if (DEBUG) {
            Slog.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + ", debugLevel = " + debugLevel);
        }
        switch (Integer.valueOf(debugLevel).intValue()) {
            case 0:
                DEBUG = false;
                break;
            case 1:
                DEBUG = true;
                break;
            default:
                DEBUG = false;
                break;
        }
    }

    private void enableWifiDisplay() {
        this.mHandler.removeCallbacks(this.mEnableWifiDelay);
        if (SystemProperties.get("ro.mtk_wfd_support").equals("1") && this.mWifiDisplayOnSetting && !this.mWifiP2pEnabled) {
            long delay = Settings.Global.getLong(this.mContext.getContentResolver(), "wifi_reenable_delay", 500L);
            Slog.d(TAG, "Enable wifi with delay:" + delay);
            this.mHandler.postDelayed(this.mEnableWifiDelay, delay);
            Toast.makeText(this.mContext, 134545544, 0).show();
            return;
        }
        this.mAutoEnableWifi = false;
        updateWfdEnableState();
    }

    private void updateWfdStatFile(int wfd_stat) {
    }

    private void dialogWfdHdmiConflict(int which) {
        if (DEBUG) {
            Slog.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + ", which = " + which);
        }
        if (which != 0) {
            return;
        }
        showDialog(2);
    }

    private boolean checkInterference(Matcher match) {
        int rssi = Float.valueOf(match.group(4)).intValue();
        int rate = Float.valueOf(match.group(6)).intValue();
        int totalCnt = Integer.valueOf(match.group(7)).intValue();
        int thresholdCnt = Integer.valueOf(match.group(8)).intValue();
        int failCnt = Integer.valueOf(match.group(9)).intValue();
        int timeoutCnt = Integer.valueOf(match.group(10)).intValue();
        int apt = Integer.valueOf(match.group(11)).intValue();
        int aat = Integer.valueOf(match.group(12)).intValue();
        return rssi < -50 || rate < 58 || totalCnt < 10 || thresholdCnt > 3 || failCnt > 2 || timeoutCnt > 2 || apt > 2 || aat > 1;
    }

    private int parseDec(String decString) {
        try {
            int num = Integer.parseInt(decString);
            return num;
        } catch (NumberFormatException e) {
            Slog.e(TAG, "Failed to parse dec string " + decString);
            return 0;
        }
    }

    private int parseFloat(String floatString) {
        try {
            int num = (int) Float.parseFloat(floatString);
            return num;
        } catch (NumberFormatException e) {
            Slog.e(TAG, "Failed to parse float string " + floatString);
            return 0;
        }
    }

    private void updateSignalLevel(boolean interference) {
        int avarageScore = getAverageScore();
        updateScoreLevel(avarageScore);
        String message = "W:" + avarageScore + ",I:" + interference + ",L:" + this.mLevel;
        if (this.mScoreLevel >= 6) {
            this.mLevel += 2;
            this.mScoreLevel = 0;
        } else if (this.mScoreLevel >= 4) {
            this.mLevel++;
            this.mScoreLevel = 0;
        } else if (this.mScoreLevel <= -6) {
            this.mLevel -= 2;
            this.mScoreLevel = 0;
        } else if (this.mScoreLevel <= -4) {
            this.mLevel--;
            this.mScoreLevel = 0;
        }
        if (this.mLevel > 0) {
            this.mLevel = 0;
        }
        if (this.mLevel < -5) {
            this.mLevel = -5;
        }
        String message2 = message + ">" + this.mLevel;
        handleLevelChange();
        if (this.WFDCONTROLLER_SQC_INFO_ON) {
            Toast.makeText(this.mContext, message2, 0).show();
        }
        Slog.d(TAG, message2);
    }

    private int getAverageScore() {
        this.mScore[this.mScoreIndex % 4] = this.mWifiScore;
        this.mScoreIndex++;
        int count = 0;
        int sum = 0;
        for (int i = 0; i < 4; i++) {
            if (this.mScore[i] != -1) {
                sum += this.mScore[i];
                count++;
            }
        }
        return sum / count;
    }

    private void updateScoreLevel(int score) {
        if (score >= 100) {
            if (this.mScoreLevel < 0) {
                this.mScoreLevel = 0;
            }
            this.mScoreLevel += 6;
            return;
        }
        if (score >= 80) {
            if (this.mScoreLevel < 0) {
                this.mScoreLevel = 0;
            }
            this.mScoreLevel += 2;
        } else if (score >= 30) {
            if (this.mScoreLevel > 0) {
                this.mScoreLevel = 0;
            }
            this.mScoreLevel -= 2;
        } else if (score >= 10) {
            if (this.mScoreLevel > 0) {
                this.mScoreLevel = 0;
            }
            this.mScoreLevel -= 3;
        } else {
            if (this.mScoreLevel > 0) {
                this.mScoreLevel = 0;
            }
            this.mScoreLevel -= 6;
        }
    }

    private void resetSignalParam() {
        this.mLevel = 0;
        this.mScoreLevel = 0;
        this.mScoreIndex = 0;
        for (int i = 0; i < 4; i++) {
            this.mScore[i] = -1;
        }
        this.mNotiTimerStarted = false;
        this.mToastTimerStarted = false;
    }

    private void registerEMObserver(int widthPixels, int heightPixels) {
        this.WFDCONTROLLER_DISPLAY_TOAST_TIME = this.mContext.getResources().getInteger(R.integer.wfd_display_toast_time);
        this.WFDCONTROLLER_DISPLAY_NOTIFICATION_TIME = this.mContext.getResources().getInteger(R.integer.wfd_display_notification_time);
        this.WFDCONTROLLER_DISPLAY_RESOLUTION = this.mContext.getResources().getInteger(R.integer.wfd_display_default_resolution);
        this.WFDCONTROLLER_DISPLAY_POWER_SAVING_OPTION = this.mContext.getResources().getInteger(R.integer.wfd_display_power_saving_option);
        this.WFDCONTROLLER_DISPLAY_POWER_SAVING_DELAY = this.mContext.getResources().getInteger(R.integer.wfd_display_power_saving_delay);
        this.WFDCONTROLLER_DISPLAY_SECURE_OPTION = this.mContext.getResources().getInteger(R.integer.wfd_display_secure_option);
        Slog.d(TAG, "registerEMObserver(), tt:" + this.WFDCONTROLLER_DISPLAY_TOAST_TIME + ",nt:" + this.WFDCONTROLLER_DISPLAY_NOTIFICATION_TIME + ",res:" + this.WFDCONTROLLER_DISPLAY_RESOLUTION + ",ps:" + this.WFDCONTROLLER_DISPLAY_POWER_SAVING_OPTION + ",psd:" + this.WFDCONTROLLER_DISPLAY_POWER_SAVING_DELAY + ",so:" + this.WFDCONTROLLER_DISPLAY_SECURE_OPTION);
        Settings.Global.putInt(this.mContext.getContentResolver(), "wifi_display_display_toast_time", this.WFDCONTROLLER_DISPLAY_TOAST_TIME);
        Settings.Global.putInt(this.mContext.getContentResolver(), "wifi_display_notification_time", this.WFDCONTROLLER_DISPLAY_NOTIFICATION_TIME);
        Settings.Global.putInt(this.mContext.getContentResolver(), "wifi_display_sqc_info_on", this.WFDCONTROLLER_SQC_INFO_ON ? 1 : 0);
        Settings.Global.putInt(this.mContext.getContentResolver(), "wifi_display_qe_on", this.WFDCONTROLLER_QE_ON ? 1 : 0);
        if (SystemProperties.get("ro.mtk_wfd_support").equals("1")) {
            int r = Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_display_max_resolution", -1);
            if (r == -1) {
                if (this.WFDCONTROLLER_DISPLAY_RESOLUTION >= 0 && this.WFDCONTROLLER_DISPLAY_RESOLUTION <= 3) {
                    int i = this.WFDCONTROLLER_DISPLAY_RESOLUTION;
                    this.mResolution = i;
                    this.mPrevResolution = i;
                } else if (widthPixels < 1080 || heightPixels < 1920) {
                    this.mResolution = 0;
                    this.mPrevResolution = 0;
                } else {
                    this.mResolution = 2;
                    this.mPrevResolution = 2;
                }
            } else if (r < 0 || r > 3) {
                this.mResolution = 0;
                this.mPrevResolution = 0;
            } else {
                this.mResolution = r;
                this.mPrevResolution = r;
            }
            int resolutionIndex = getResolutionIndex(this.mResolution);
            Slog.i(TAG, "mResolution:" + this.mResolution + ", resolutionIndex: " + resolutionIndex);
            SystemProperties.set("media.wfd.video-format", String.valueOf(resolutionIndex));
        }
        Settings.Global.putInt(this.mContext.getContentResolver(), "wifi_display_auto_channel_selection", this.mAutoChannelSelection ? 1 : 0);
        Settings.Global.putInt(this.mContext.getContentResolver(), "wifi_display_max_resolution", this.mResolution);
        Settings.Global.putInt(this.mContext.getContentResolver(), "wifi_display_power_saving_option", this.WFDCONTROLLER_DISPLAY_POWER_SAVING_OPTION);
        Settings.Global.putInt(this.mContext.getContentResolver(), "wifi_display_power_saving_delay", this.WFDCONTROLLER_DISPLAY_POWER_SAVING_DELAY);
        Settings.Global.putInt(this.mContext.getContentResolver(), "wifi_display_latency_profiling", this.mLatencyProfiling);
        Settings.Global.putString(this.mContext.getContentResolver(), "wifi_display_chosen_capability", "");
        initPortraitResolutionSupport();
        resetLatencyInfo();
        initSecureOption();
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("wifi_display_display_toast_time"), false, this.mObserver);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("wifi_display_notification_time"), false, this.mObserver);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("wifi_display_sqc_info_on"), false, this.mObserver);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("wifi_display_qe_on"), false, this.mObserver);
        if (SystemProperties.get("ro.mtk_wfd_support").equals("1")) {
            this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("wifi_display_auto_channel_selection"), false, this.mObserver);
            this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("wifi_display_max_resolution"), false, this.mObserver);
            this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("wifi_display_latency_profiling"), false, this.mObserver);
            this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("wifi_display_security_option"), false, this.mObserver);
            this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("wifi_display_portrait_resolution"), false, this.mObserver);
        }
    }

    private void initPortraitResolutionSupport() {
        Settings.Global.putInt(this.mContext.getContentResolver(), "wifi_display_portrait_resolution", 0);
        SystemProperties.set("media.wfd.portrait", String.valueOf(0));
    }

    private void handlePortraitResolutionSupportChange() {
        int value = Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_display_portrait_resolution", 0);
        Slog.i(TAG, "handlePortraitResolutionSupportChange:" + value);
        SystemProperties.set("media.wfd.portrait", String.valueOf(value));
    }

    private void sendPortraitIntent() {
        Slog.d(TAG, "sendPortraitIntent()");
        Intent intent = new Intent(WFD_PORTRAIT);
        intent.addFlags(67108864);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void initSecureOption() {
        Settings.Global.putInt(this.mContext.getContentResolver(), "wifi_display_security_option", this.WFDCONTROLLER_DISPLAY_SECURE_OPTION);
        SystemProperties.set("wlan.wfd.security.image", String.valueOf(this.WFDCONTROLLER_DISPLAY_SECURE_OPTION));
    }

    private void handleSecureOptionChange() {
        int secureOption = Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_display_security_option", 1);
        if (secureOption == this.WFDCONTROLLER_DISPLAY_SECURE_OPTION) {
            return;
        }
        Slog.i(TAG, "handleSecureOptionChange:" + secureOption + "->" + this.WFDCONTROLLER_DISPLAY_SECURE_OPTION);
        this.WFDCONTROLLER_DISPLAY_SECURE_OPTION = secureOption;
        SystemProperties.set("ro.sf.security.image", String.valueOf(this.WFDCONTROLLER_DISPLAY_SECURE_OPTION));
    }

    private int getResolutionIndex(int settingValue) {
        switch (settingValue) {
        }
        return 5;
    }

    private void handleResolutionChange() {
        int r = Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_display_max_resolution", 0);
        if (r == this.mResolution) {
            return;
        }
        this.mPrevResolution = this.mResolution;
        this.mResolution = r;
        Slog.d(TAG, "handleResolutionChange(), resolution:" + this.mPrevResolution + "->" + this.mResolution);
        int idxModified = getResolutionIndex(this.mResolution);
        int idxOriginal = getResolutionIndex(this.mPrevResolution);
        if (idxModified == idxOriginal) {
            return;
        }
        boolean doNotRemind = Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_display_change_resolution_remind", 0) != 0;
        Slog.d(TAG, "index:" + idxOriginal + "->" + idxModified + ", doNotRemind:" + doNotRemind);
        SystemProperties.set("media.wfd.video-format", String.valueOf(idxModified));
        if (this.mConnectedDevice == null && this.mConnectingDevice == null) {
            return;
        }
        if (doNotRemind) {
            Slog.d(TAG, "-- reconnect for resolution change --");
            disconnect();
            this.mReconnectForResolutionChange = true;
            return;
        }
        showDialog(5);
    }

    private void revertResolutionChange() {
        Slog.d(TAG, "revertResolutionChange(), resolution:" + this.mResolution + "->" + this.mPrevResolution);
        int idxModified = getResolutionIndex(this.mResolution);
        int idxOriginal = getResolutionIndex(this.mPrevResolution);
        Slog.d(TAG, "index:" + idxModified + "->" + idxOriginal);
        SystemProperties.set("media.wfd.video-format", String.valueOf(idxOriginal));
        this.mResolution = this.mPrevResolution;
        Settings.Global.putInt(this.mContext.getContentResolver(), "wifi_display_max_resolution", this.mResolution);
    }

    private void handleLatencyProfilingChange() {
        int value = Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_display_latency_profiling", 2);
        if (value == this.mLatencyProfiling) {
            return;
        }
        Slog.d(TAG, "handleLatencyProfilingChange(), connected:" + this.mIsWFDConnected + ",value:" + this.mLatencyProfiling + "->" + value);
        this.mLatencyProfiling = value;
        if (this.mLatencyProfiling != 3) {
            this.mHandler.removeCallbacks(this.mDelayProfiling);
        }
        if ((this.mLatencyProfiling == 0 || this.mLatencyProfiling == 1 || this.mLatencyProfiling == 3) && this.mIsWFDConnected) {
            startProfilingInfo();
        } else {
            stopProfilingInfo();
        }
    }

    private void showLatencyPanel() {
        Slog.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName());
        LayoutInflater adbInflater = LayoutInflater.from(this.mContext);
        this.mLatencyPanelView = adbInflater.inflate(R.layout.textpanel, (ViewGroup) null);
        this.mTextView = (TextView) this.mLatencyPanelView.findViewById(R.id.bodyText);
        this.mTextView.setTextColor(-1);
        this.mTextView.setText("AP:\nS:\nR:\nAL:\n");
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.type = 2037;
        layoutParams.flags = 8;
        layoutParams.width = -2;
        layoutParams.height = -2;
        layoutParams.gravity = 51;
        layoutParams.alpha = 0.7f;
        WindowManager windowManager = (WindowManager) this.mContext.getSystemService("window");
        windowManager.addView(this.mLatencyPanelView, layoutParams);
    }

    private void hideLatencyPanel() {
        Slog.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName());
        if (this.mLatencyPanelView != null) {
            WindowManager windowManager = (WindowManager) this.mContext.getSystemService("window");
            windowManager.removeView(this.mLatencyPanelView);
            this.mLatencyPanelView = null;
        }
        this.mTextView = null;
    }

    private void checkA2dpStatus() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            Slog.d(TAG, "checkA2dpStatus(), BT is not enabled");
            return;
        }
        int value = Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_display_sound_path_do_not_remind", -1);
        Slog.d(TAG, "checkA2dpStatus(), value:" + value);
        if (value == 1) {
            return;
        }
        BluetoothProfile.ServiceListener profileListener = new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                BluetoothA2dp a2dp = (BluetoothA2dp) proxy;
                List<BluetoothDevice> deviceList = a2dp.getConnectedDevices();
                boolean empty = deviceList.isEmpty();
                Slog.d(WifiDisplayController.TAG, "BluetoothProfile listener is connected, empty:" + empty);
                if (empty) {
                    return;
                }
                WifiDisplayController.this.showDialog(6);
            }

            @Override
            public void onServiceDisconnected(int profile) {
            }
        };
        adapter.getProfileProxy(this.mContext, profileListener, 2);
    }

    private void setAutoChannelSelection() {
        Slog.d(TAG, "setAutoChannelSelection(), auto:" + this.mAutoChannelSelection);
        if (this.mAutoChannelSelection) {
            this.mWifiP2pManager.setP2pAutoChannel(this.mWifiP2pChannel, true, null);
        } else {
            this.mWifiP2pManager.setP2pAutoChannel(this.mWifiP2pChannel, false, null);
        }
    }

    private void updateChosenCapability(int usingUIBC, boolean portrait) {
        String capability;
        String capability2;
        String capability3;
        String capability4 = "";
        if (this.mIsWFDConnected) {
            int usingPCMAudio = this.mRemoteDisplay.getWfdParam(3);
            String capability5 = usingPCMAudio == 1 ? "LPCM(2 ch)," : "AAC(2 ch),";
            int isCBPOnly = this.mRemoteDisplay.getWfdParam(4);
            if (isCBPOnly == 1) {
                capability = capability5 + "H.264(CBP level 3.1),";
            } else {
                capability = capability5 + "H.264(CHP level 4.1),";
            }
            int resolutionIndex = getResolutionIndex(this.mResolution);
            if (resolutionIndex == 5) {
                if (portrait) {
                    capability2 = capability + "720x1280 30p,";
                } else {
                    capability2 = capability + "1280x720 30p,";
                }
            } else if (resolutionIndex == 7) {
                if (portrait) {
                    capability2 = capability + "1080x1920 30p,";
                } else {
                    capability2 = capability + "1920x1080 30p,";
                }
            } else {
                capability2 = capability + "640x480 60p,";
            }
            int usingHDCP = this.mRemoteDisplay.getWfdParam(7);
            if (usingHDCP == 1) {
                capability3 = capability2 + "with HDCP,";
            } else {
                capability3 = capability2 + "without HDCP,";
            }
            if (usingUIBC != 0) {
                capability4 = capability3 + "with UIBC";
            } else {
                capability4 = capability3 + "without UIBC";
            }
        }
        Slog.d(TAG, "updateChosenCapability(), connected:" + this.mIsWFDConnected + ", capability:" + capability4 + ", portrait:" + portrait);
        Settings.Global.putString(this.mContext.getContentResolver(), "wifi_display_chosen_capability", capability4);
    }

    private void startProfilingInfo() {
        if (this.mLatencyProfiling == 3) {
            showLatencyPanel();
        } else {
            hideLatencyPanel();
        }
        this.mHandler.removeCallbacks(this.mLatencyInfo);
        this.mHandler.removeCallbacks(this.mScanWifiAp);
        this.mHandler.postDelayed(this.mLatencyInfo, 100L);
        this.mHandler.postDelayed(this.mScanWifiAp, 100L);
    }

    private void stopProfilingInfo() {
        hideLatencyPanel();
        this.mHandler.removeCallbacks(this.mLatencyInfo);
        this.mHandler.removeCallbacks(this.mScanWifiAp);
        this.mHandler.removeCallbacks(this.mDelayProfiling);
        resetLatencyInfo();
    }

    private void resetLatencyInfo() {
        Settings.Global.putString(this.mContext.getContentResolver(), "wifi_display_wifi_info", "0,0,0,0");
        Settings.Global.putString(this.mContext.getContentResolver(), "wifi_display_wfd_latency", "0,0,0");
    }

    private int getWifiApNum() {
        int count = 0;
        List<ScanResult> results = this.mWifiManager.getScanResults();
        ArrayList<String> SSIDList = new ArrayList<>();
        if (results != null) {
            for (ScanResult result : results) {
                if (result.SSID != null && result.SSID.length() != 0 && !result.capabilities.contains("[IBSS]") && getFreqId(result.frequency) == this.mWifiP2pChannelId) {
                    boolean duplicate = false;
                    Iterator ssid$iterator = SSIDList.iterator();
                    while (true) {
                        if (!ssid$iterator.hasNext()) {
                            break;
                        }
                        String ssid = (String) ssid$iterator.next();
                        if (ssid.equals(result.SSID)) {
                            duplicate = true;
                            break;
                        }
                    }
                    if (!duplicate) {
                        if (DEBUG) {
                            Slog.d(TAG, "AP SSID: " + result.SSID);
                        }
                        SSIDList.add(result.SSID);
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private void updateWifiP2pChannelId(boolean connected, Intent intent) {
        if (this.mWfdEnabled && connected && (this.mDesiredDevice != null || this.mSinkEnabled)) {
            WifiP2pGroup wifiP2pGroup = (WifiP2pGroup) intent.getParcelableExtra("p2pGroupInfo");
            int freq = wifiP2pGroup.getFrequency();
            this.mWifiP2pChannelId = getFreqId(freq);
            Slog.d(TAG, "updateWifiP2pChannelId(), freq:" + freq + ", id:" + this.mWifiP2pChannelId);
            return;
        }
        this.mWifiP2pChannelId = -1;
        Slog.d(TAG, "updateWifiP2pChannelId(), id:" + this.mWifiP2pChannelId);
    }

    private int getFreqId(int frequency) {
        switch (frequency) {
            case 2412:
                return 1;
            case 2417:
                return 2;
            case 2422:
                return 3;
            case 2427:
                return 4;
            case 2432:
                return 5;
            case 2437:
                return 6;
            case 2442:
                return 7;
            case 2447:
                return 8;
            case 2452:
                return 9;
            case 2457:
                return 10;
            case 2462:
                return 11;
            case 2467:
                return 12;
            case 2472:
                return 13;
            case 2484:
                return 14;
            case 5180:
                return 36;
            case 5190:
                return 38;
            case 5200:
                return 40;
            case 5210:
                return 42;
            case 5220:
                return 44;
            case 5230:
                return 46;
            case 5240:
                return 48;
            case 5260:
                return 52;
            case 5280:
                return 56;
            case 5300:
                return 60;
            case 5320:
                return 64;
            case 5500:
                return 100;
            case 5520:
                return HdmiCecKeycode.CEC_KEYCODE_SELECT_MEDIA_FUNCTION;
            case 5540:
                return HdmiCecKeycode.CEC_KEYCODE_POWER_OFF_FUNCTION;
            case 5560:
                return 112;
            case 5580:
                return HdmiCecKeycode.CEC_KEYCODE_F4_YELLOW;
            case 5600:
                return RTSP_TIMEOUT_SECONDS_CERT_MODE;
            case 5620:
                return 124;
            case 5640:
                return 128;
            case 5660:
                return 132;
            case 5680:
                return 136;
            case 5700:
                return 140;
            case 5745:
                return 149;
            case 5765:
                return 153;
            case 5785:
                return 157;
            case 5805:
                return 161;
            case 5825:
                return 165;
            default:
                return 0;
        }
    }

    private void handleLevelChange() {
        if (this.mLevel < 0) {
            if (!this.mToastTimerStarted) {
                this.mHandler.postDelayed(this.mDisplayToast, this.WFDCONTROLLER_DISPLAY_TOAST_TIME * 1000);
                this.mToastTimerStarted = true;
            }
            if (this.mNotiTimerStarted) {
                return;
            }
            this.mHandler.postDelayed(this.mDisplayNotification, this.WFDCONTROLLER_DISPLAY_NOTIFICATION_TIME * 1000);
            this.mNotiTimerStarted = true;
            return;
        }
        clearNotify();
    }

    private void clearNotify() {
        if (this.mToastTimerStarted) {
            this.mHandler.removeCallbacks(this.mDisplayToast);
            this.mToastTimerStarted = false;
        }
        if (this.mNotiTimerStarted) {
            this.mHandler.removeCallbacks(this.mDisplayNotification);
            this.mNotiTimerStarted = false;
        }
        this.mNotificationManager.cancelAsUser(null, 134545536, UserHandle.ALL);
    }

    private void showNotification(int titleId, int contentId) {
        Slog.d(TAG, "showNotification(), titleId:" + titleId);
        this.mNotificationManager.cancelAsUser(null, titleId, UserHandle.ALL);
        Resources mResource = Resources.getSystem();
        Notification.Builder builder = new Notification.Builder(this.mContext).setContentTitle(mResource.getString(titleId)).setContentText(mResource.getString(contentId)).setSmallIcon(R.drawable.ic_notify_wifidisplay_blink).setAutoCancel(true);
        Notification notification = new Notification.BigTextStyle(builder).bigText(mResource.getString(contentId)).build();
        this.mNotificationManager.notifyAsUser(null, titleId, notification, UserHandle.ALL);
    }

    private void dialogReconnect() {
        showDialog(4);
    }

    private void resetReconnectVariable() {
        Slog.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName());
        this.mReScanning = false;
        this.mReConnectDevice = null;
        this.mReConnection_Timeout_Remain_Seconds = 0;
        this.mReConnecting = false;
        this.mHandler.removeCallbacks(this.mReConnect);
    }

    private void chooseNo_WifiDirectExcludeDialog() {
        if (SystemProperties.get("ro.mtk_wfd_sink_support").equals("1") && this.mSinkEnabled) {
            Slog.d(TAG, "[sink] callback onDisplayConnectionFailed()");
            this.mListener.onDisplayConnectionFailed();
        } else {
            unadvertiseDisplay();
        }
    }

    private void chooseNo_HDMIExcludeDialog_WfdUpdate() {
        Settings.Global.putInt(this.mContext.getContentResolver(), "wifi_display_on", 0);
        updateWfdEnableState();
    }

    private void turnOffHdmi() {
        if (this.mHdmiManager == null) {
            return;
        }
        try {
            this.mHdmiManager.enableHdmi(false);
        } catch (RemoteException e) {
            Slog.d(TAG, "hdmi manager RemoteException: " + e.getMessage());
        }
    }

    private void prepareDialog(int dialogID) {
        Resources mResource = Resources.getSystem();
        if (1 == dialogID) {
            this.mWifiDirectExcludeDialog = new AlertDialog.Builder(this.mContext).setMessage(mResource.getString(134545526)).setPositiveButton(mResource.getString(android.R.string.face_error_vendor_unknown), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Slog.d(WifiDisplayController.TAG, "[Exclude Dialog] disconnect previous Wi-Fi P2p connection");
                    WifiDisplayController.this.mIsConnected_OtherP2p = false;
                    WifiDisplayController.this.mWifiP2pManager.removeGroup(WifiDisplayController.this.mWifiP2pChannel, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            Slog.i(WifiDisplayController.TAG, "Disconnected from previous Wi-Fi P2p device, succeess");
                        }

                        @Override
                        public void onFailure(int reason) {
                            Slog.i(WifiDisplayController.TAG, "Disconnected from previous Wi-Fi P2p device, failure = " + reason);
                        }
                    });
                    WifiDisplayController.this.chooseNo_WifiDirectExcludeDialog();
                    WifiDisplayController.this.mUserDecided = true;
                }
            }).setNegativeButton(mResource.getString(android.R.string.face_acquired_insufficient), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Slog.d(WifiDisplayController.TAG, "[Exclude Dialog] keep previous Wi-Fi P2p connection");
                    WifiDisplayController.this.chooseNo_WifiDirectExcludeDialog();
                    WifiDisplayController.this.mUserDecided = true;
                }
            }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface arg0) {
                    Slog.d(WifiDisplayController.TAG, "[Exclude Dialog] onCancel(): keep previous Wi-Fi P2p connection");
                    WifiDisplayController.this.chooseNo_WifiDirectExcludeDialog();
                    WifiDisplayController.this.mUserDecided = true;
                }
            }).setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface arg0) {
                    Slog.d(WifiDisplayController.TAG, "[Exclude Dialog] onDismiss()");
                    if (WifiDisplayController.this.mUserDecided) {
                        return;
                    }
                    WifiDisplayController.this.chooseNo_WifiDirectExcludeDialog();
                }
            }).create();
            popupDialog(this.mWifiDirectExcludeDialog);
            return;
        }
        if (2 == dialogID) {
            String messageString = reviseHDMIString(mResource.getString(134545523));
            this.mHDMIExcludeDialog_WfdUpdate = new AlertDialog.Builder(this.mContext).setMessage(messageString).setPositiveButton(mResource.getString(android.R.string.face_error_vendor_unknown), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (WifiDisplayController.DEBUG) {
                        Slog.d(WifiDisplayController.TAG, "WifiDisplay on, user turn off HDMI");
                    }
                    WifiDisplayController.this.turnOffHdmi();
                    WifiDisplayController.this.enableWifiDisplay();
                    WifiDisplayController.this.mUserDecided = true;
                }
            }).setNegativeButton(mResource.getString(android.R.string.face_acquired_insufficient), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (WifiDisplayController.DEBUG) {
                        Slog.d(WifiDisplayController.TAG, "WifiDisplay on, user DON'T turn off HDMI -> turn off WifiDisplay");
                    }
                    WifiDisplayController.this.chooseNo_HDMIExcludeDialog_WfdUpdate();
                    WifiDisplayController.this.mUserDecided = true;
                }
            }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface arg0) {
                    if (WifiDisplayController.DEBUG) {
                        Slog.d(WifiDisplayController.TAG, "onCancel(): WifiDisplay on, user DON'T turn off HDMI -> turn off WifiDisplay");
                    }
                    WifiDisplayController.this.chooseNo_HDMIExcludeDialog_WfdUpdate();
                    WifiDisplayController.this.mUserDecided = true;
                }
            }).setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface arg0) {
                    if (WifiDisplayController.DEBUG) {
                        Slog.d(WifiDisplayController.TAG, "onDismiss()");
                    }
                    if (WifiDisplayController.this.mUserDecided) {
                        return;
                    }
                    WifiDisplayController.this.chooseNo_HDMIExcludeDialog_WfdUpdate();
                }
            }).create();
            popupDialog(this.mHDMIExcludeDialog_WfdUpdate);
            return;
        }
        if (4 == dialogID) {
            this.mReConnecteDialog = new AlertDialog.Builder(this.mContext).setTitle(134545540).setMessage(134545541).setPositiveButton(mResource.getString(android.R.string.face_error_vendor_unknown), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (WifiDisplayController.DEBUG) {
                        Slog.d(WifiDisplayController.TAG, "user want to reconnect");
                    }
                    WifiDisplayController.this.mReScanning = true;
                    WifiDisplayController.this.updateScanState();
                    WifiDisplayController.this.mReConnection_Timeout_Remain_Seconds = 60;
                    WifiDisplayController.this.mHandler.postDelayed(WifiDisplayController.this.mReConnect, 1000L);
                }
            }).setNegativeButton(mResource.getString(android.R.string.face_acquired_insufficient), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (!WifiDisplayController.DEBUG) {
                        return;
                    }
                    Slog.d(WifiDisplayController.TAG, "user want nothing");
                }
            }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface arg0) {
                    if (!WifiDisplayController.DEBUG) {
                        return;
                    }
                    Slog.d(WifiDisplayController.TAG, "user want nothing");
                }
            }).create();
            popupDialog(this.mReConnecteDialog);
            return;
        }
        if (5 == dialogID) {
            LayoutInflater adbInflater = LayoutInflater.from(this.mContext);
            View checkboxLayout = adbInflater.inflate(R.layout.checkbox, (ViewGroup) null);
            final CheckBox checkbox = (CheckBox) checkboxLayout.findViewById(R.id.skip);
            checkbox.setText(134545553);
            this.mChangeResolutionDialog = new AlertDialog.Builder(this.mContext).setView(checkboxLayout).setMessage(134545552).setPositiveButton(mResource.getString(android.R.string.face_error_vendor_unknown), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    boolean checked = checkbox.isChecked();
                    Slog.d(WifiDisplayController.TAG, "[Change resolution]: ok. checked:" + checked);
                    if (checked) {
                        Settings.Global.putInt(WifiDisplayController.this.mContext.getContentResolver(), "wifi_display_change_resolution_remind", 1);
                    } else {
                        Settings.Global.putInt(WifiDisplayController.this.mContext.getContentResolver(), "wifi_display_change_resolution_remind", 0);
                    }
                    if (WifiDisplayController.this.mConnectedDevice == null && WifiDisplayController.this.mConnectingDevice == null) {
                        return;
                    }
                    Slog.d(WifiDisplayController.TAG, "-- reconnect for resolution change --");
                    WifiDisplayController.this.disconnect();
                    WifiDisplayController.this.mReconnectForResolutionChange = true;
                }
            }).setNegativeButton(mResource.getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Slog.d(WifiDisplayController.TAG, "[Change resolution]: cancel");
                    WifiDisplayController.this.revertResolutionChange();
                }
            }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface arg0) {
                    Slog.d(WifiDisplayController.TAG, "[Change resolution]: doesn't choose");
                    WifiDisplayController.this.revertResolutionChange();
                }
            }).create();
            popupDialog(this.mChangeResolutionDialog);
            return;
        }
        if (6 == dialogID) {
            LayoutInflater adbInflater2 = LayoutInflater.from(this.mContext);
            View checkboxLayout2 = adbInflater2.inflate(R.layout.checkbox, (ViewGroup) null);
            final CheckBox checkbox2 = (CheckBox) checkboxLayout2.findViewById(R.id.skip);
            checkbox2.setText(134545553);
            int value = Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_display_sound_path_do_not_remind", -1);
            if (value == -1) {
                checkbox2.setChecked(true);
            }
            this.mSoundPathDialog = new AlertDialog.Builder(this.mContext).setView(checkboxLayout2).setMessage(134545545).setPositiveButton(mResource.getString(android.R.string.face_error_vendor_unknown), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    boolean checked = checkbox2.isChecked();
                    Slog.d(WifiDisplayController.TAG, "[Sound path reminder]: ok. checked:" + checked);
                    if (checked) {
                        Settings.Global.putInt(WifiDisplayController.this.mContext.getContentResolver(), "wifi_display_sound_path_do_not_remind", 1);
                    } else {
                        Settings.Global.putInt(WifiDisplayController.this.mContext.getContentResolver(), "wifi_display_sound_path_do_not_remind", 0);
                    }
                }
            }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface arg0) {
                    Slog.d(WifiDisplayController.TAG, "[Sound path reminder]: cancel");
                }
            }).create();
            popupDialog(this.mSoundPathDialog);
            return;
        }
        if (7 == dialogID) {
            LayoutInflater adbInflater3 = LayoutInflater.from(this.mContext);
            View progressLayout = adbInflater3.inflate(R.layout.progress_dialog, (ViewGroup) null);
            ProgressBar progressBar = (ProgressBar) progressLayout.findViewById(R.id.progress);
            progressBar.setIndeterminate(true);
            TextView progressText = (TextView) progressLayout.findViewById(R.id.progress_text);
            progressText.setText(134545556);
            this.mWaitConnectDialog = new AlertDialog.Builder(this.mContext).setView(progressLayout).setNegativeButton(mResource.getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Slog.d(WifiDisplayController.TAG, "[Wait connection]: cancel");
                    WifiDisplayController.this.disconnectWfdSink();
                }
            }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface arg0) {
                    Slog.d(WifiDisplayController.TAG, "[Wait connection]: no choice");
                    WifiDisplayController.this.disconnectWfdSink();
                }
            }).create();
            popupDialog(this.mWaitConnectDialog);
            return;
        }
        if (8 == dialogID) {
            dismissDialogDetail(this.mWaitConnectDialog);
            String message = this.mSinkDeviceName + " " + mResource.getString(134545557);
            this.mConfirmConnectDialog = new AlertDialog.Builder(this.mContext).setMessage(message).setPositiveButton(mResource.getString(android.R.string.face_acquired_dark_glasses_detected_alt), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Slog.d(WifiDisplayController.TAG, "[GC confirm connection]: accept");
                    String goIntent = SystemProperties.get("wfd.sink.go_intent", String.valueOf(14));
                    int value2 = Integer.valueOf(goIntent).intValue();
                    Slog.i(WifiDisplayController.TAG, "Sink go_intent:" + value2);
                    WifiDisplayController.this.mWifiP2pManager.setGCInviteResult(WifiDisplayController.this.mWifiP2pChannel, true, value2, null);
                    WifiDisplayController.this.showDialog(9);
                }
            }).setNegativeButton(mResource.getString(android.R.string.face_acquired_insufficient), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Slog.d(WifiDisplayController.TAG, "[GC confirm connection]: declines");
                    WifiDisplayController.this.mWifiP2pManager.setGCInviteResult(WifiDisplayController.this.mWifiP2pChannel, false, 0, null);
                    WifiDisplayController.this.disconnectWfdSink();
                }
            }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface arg0) {
                    Slog.d(WifiDisplayController.TAG, "[Confirm connection]: cancel");
                    WifiDisplayController.this.mWifiP2pManager.setGCInviteResult(WifiDisplayController.this.mWifiP2pChannel, false, 0, null);
                    WifiDisplayController.this.disconnectWfdSink();
                }
            }).create();
            popupDialog(this.mConfirmConnectDialog);
            return;
        }
        if (9 == dialogID) {
            LayoutInflater adbInflater4 = LayoutInflater.from(this.mContext);
            View progressLayout2 = adbInflater4.inflate(R.layout.progress_dialog, (ViewGroup) null);
            ProgressBar progressBar2 = (ProgressBar) progressLayout2.findViewById(R.id.progress);
            progressBar2.setIndeterminate(true);
            TextView progressText2 = (TextView) progressLayout2.findViewById(R.id.progress_text);
            progressText2.setText(134545558);
            this.mBuildConnectDialog = new AlertDialog.Builder(this.mContext).setView(progressLayout2).setNegativeButton(mResource.getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Slog.d(WifiDisplayController.TAG, "[Build connection]: cancel");
                    WifiDisplayController.this.disconnectWfdSink();
                }
            }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface arg0) {
                    Slog.d(WifiDisplayController.TAG, "[Build connection]: no choice");
                    WifiDisplayController.this.disconnectWfdSink();
                }
            }).create();
            popupDialog(this.mBuildConnectDialog);
        }
    }

    private void popupDialog(AlertDialog dialog) {
        dialog.getWindow().setType(2003);
        dialog.getWindow().getAttributes().privateFlags |= 16;
        dialog.show();
    }

    private void showDialog(int dialogID) {
        this.mUserDecided = false;
        prepareDialog(dialogID);
    }

    private void dismissDialog() {
        dismissDialogDetail(this.mWifiDirectExcludeDialog);
        dismissDialogDetail(this.mHDMIExcludeDialog_WfdUpdate);
        dismissDialogDetail(this.mReConnecteDialog);
    }

    private void dismissDialogDetail(AlertDialog dialog) {
        if (dialog == null || !dialog.isShowing()) {
            return;
        }
        dialog.dismiss();
    }

    private void notifyClearMotion(boolean connected) {
        if (!SystemProperties.get("ro.mtk_clearmotion_support").equals("1")) {
            return;
        }
        SystemProperties.set("sys.display.clearMotion.dimmed", connected ? "1" : "0");
        Intent intent = new Intent(WFD_CLEARMOTION_DIMMED);
        intent.addFlags(67108864);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void updateWifiPowerSavingMode(boolean enable) {
        if (this.mWifiPowerSaving == enable) {
            return;
        }
        this.mWifiPowerSaving = enable;
        Slog.d(TAG, "setPowerSavingMode():" + this.mWifiPowerSaving);
        this.mWifiManager.setPowerSavingMode(enable);
    }

    private void checkIfWifiApIs11G() {
        Slog.d(TAG, "checkIfWifiApIs11G()");
        String wifiStatus = this.mWifiManager.getWifiStatus();
        if (wifiStatus == null) {
            Slog.d(TAG, "getWifiStatus() return null.");
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, "getWifiStatus() return: " + wifiStatus);
        }
        String[] tokens = wifiStatus.split("\n");
        for (String token : tokens) {
            if (token.startsWith("group_cipher=")) {
                String[] nameValue = token.split("=");
                String cipher = nameValueAssign(nameValue);
                if (cipher == null) {
                    Slog.e(TAG, "cipher is null.");
                    return;
                }
                Slog.d(TAG, "cipher is " + cipher);
                if (!cipher.contains("TKIP") && !cipher.contains("WEP")) {
                    return;
                }
                Toast.makeText(this.mContext, 134545554, 0).show();
                return;
            }
        }
    }

    private String nameValueAssign(String[] nameValue) {
        if (nameValue == null || 2 != nameValue.length) {
            return null;
        }
        return nameValue[1];
    }

    private String reviseHDMIString(String input) {
        try {
            if (this.mHdmiManager != null && (this.mHdmiManager.getDisplayType() == 2 || this.mHdmiManager.getDisplayType() == 1)) {
                return input.replaceAll("HDMI", "MHL");
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "HdmiManager.getDisplayType() RemoteException");
        }
        return input;
    }

    public boolean getIfSinkEnabled() {
        Slog.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + ",enable = " + this.mSinkEnabled);
        return this.mSinkEnabled;
    }

    public void requestEnableSink(boolean enable) {
        Slog.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + ",enable = " + enable + ",Connected = " + this.mIsWFDConnected + ", option = " + SystemProperties.get("ro.mtk_wfd_sink_support").equals("1") + ", WfdEnabled = " + this.mWfdEnabled);
        if (!SystemProperties.get("ro.mtk_wfd_sink_support").equals("1") || this.mSinkEnabled == enable || this.mIsWFDConnected) {
            return;
        }
        if (enable && this.mIsConnected_OtherP2p) {
            Slog.i(TAG, "OtherP2P is connected! Only set variable. Ignore !");
            this.mSinkEnabled = enable;
            enterSinkState(SinkState.SINK_STATE_IDLE);
            return;
        }
        stopWifiScan(enable);
        if (enable) {
            requestStopScan();
        }
        this.mSinkEnabled = enable;
        updateWfdInfo(true);
        if (!this.mSinkEnabled) {
            requestStartScan();
        } else {
            enterSinkState(SinkState.SINK_STATE_IDLE);
        }
    }

    public void requestWaitConnection(Surface surface) {
        Slog.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + ", mSinkState:" + this.mSinkState);
        if (!isSinkState(SinkState.SINK_STATE_IDLE)) {
            Slog.i(TAG, "State is wrong! Ignore the request !");
            return;
        }
        if (this.mIsConnected_OtherP2p) {
            Slog.i(TAG, "OtherP2P is connected! Show dialog!");
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    WifiDisplayController.this.notifyDisplayConnecting();
                }
            });
            showDialog(1);
            return;
        }
        this.mSinkSurface = surface;
        this.mIsWFDConnected = false;
        this.mSinkDiscoverRetryCount = 5;
        startWaitConnection();
        setSinkMiracastMode();
        enterSinkState(SinkState.SINK_STATE_WAITING_P2P_CONNECTION);
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!WifiDisplayController.this.isSinkState(SinkState.SINK_STATE_WAITING_P2P_CONNECTION)) {
                    return;
                }
                WifiDisplayController.this.notifyDisplayConnecting();
            }
        });
    }

    public void requestSuspendDisplay(boolean suspend, Surface surface) {
        Slog.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + ",suspend = " + suspend);
        this.mSinkSurface = surface;
        if (isSinkState(SinkState.SINK_STATE_RTSP_CONNECTED)) {
            if (this.mRemoteDisplay != null) {
                this.mRemoteDisplay.suspendDisplay(suspend, surface);
            }
            blockNotificationList(!suspend);
            return;
        }
        Slog.i(TAG, "State is wrong !!!, SinkState:" + this.mSinkState);
    }

    public void sendUibcInputEvent(String input) {
        if (!SystemProperties.get("ro.mtk_wfd_sink_uibc_support").equals("1") || !this.mSinkEnabled || this.mRemoteDisplay == null) {
            return;
        }
        this.mRemoteDisplay.sendUibcEvent(input);
    }

    private synchronized void disconnectWfdSink() {
        Slog.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + ", SinkState = " + this.mSinkState);
        if (isSinkState(SinkState.SINK_STATE_WAITING_P2P_CONNECTION) || isSinkState(SinkState.SINK_STATE_WIFI_P2P_CONNECTED)) {
            this.mHandler.removeCallbacks(this.mGetSinkIpAddr);
            this.mHandler.removeCallbacks(this.mSinkDiscover);
            stopPeerDiscovery();
            Slog.i(TAG, "Disconnected from WFD sink (P2P).");
            deletePersistentGroup();
            enterSinkState(SinkState.SINK_STATE_IDLE);
            updateIfSinkConnected(false);
            this.mWifiP2pManager.setMiracastMode(0);
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Slog.d(WifiDisplayController.TAG, "[Sink] callback onDisplayDisconnected()");
                    WifiDisplayController.this.mListener.onDisplayDisconnected();
                }
            });
        } else if (isSinkState(SinkState.SINK_STATE_WAITING_RTSP) || isSinkState(SinkState.SINK_STATE_RTSP_CONNECTED)) {
            if (this.mRemoteDisplay != null) {
                Slog.i(TAG, "before dispose()");
                this.mRemoteDisplay.dispose();
                Slog.i(TAG, "after dispose()");
            }
            this.mHandler.removeCallbacks(this.mRtspSinkTimeout);
            enterSinkState(SinkState.SINK_STATE_WIFI_P2P_CONNECTED);
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    WifiDisplayController.this.disconnectWfdSink();
                }
            });
        }
        this.mRemoteDisplay = null;
        this.mSinkDeviceName = null;
        this.mSinkMacAddress = null;
        this.mSinkPort = 0;
        this.mSinkIpAddress = null;
        this.mSinkSurface = null;
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                WifiDisplayController.this.dismissDialogDetail(WifiDisplayController.this.mWaitConnectDialog);
                WifiDisplayController.this.dismissDialogDetail(WifiDisplayController.this.mConfirmConnectDialog);
                WifiDisplayController.this.dismissDialogDetail(WifiDisplayController.this.mBuildConnectDialog);
                if (WifiDisplayController.this.mWifiDirectExcludeDialog != null && WifiDisplayController.this.mWifiDirectExcludeDialog.isShowing()) {
                    WifiDisplayController.this.chooseNo_WifiDirectExcludeDialog();
                }
                WifiDisplayController.this.dismissDialogDetail(WifiDisplayController.this.mWifiDirectExcludeDialog);
            }
        });
    }

    private void deletePersistentGroup() {
        Slog.d(TAG, "deletePersistentGroup");
        if (this.mSinkP2pGroup == null) {
            return;
        }
        Slog.d(TAG, "mSinkP2pGroup network id: " + this.mSinkP2pGroup.getNetworkId());
        if (this.mSinkP2pGroup.getNetworkId() >= 0) {
            this.mWifiP2pManager.deletePersistentGroup(this.mWifiP2pChannel, this.mSinkP2pGroup.getNetworkId(), null);
        }
        this.mSinkP2pGroup = null;
    }

    private void handleSinkP2PConnection(NetworkInfo networkInfo) {
        Slog.i(TAG, "handleSinkP2PConnection(), sinkState:" + this.mSinkState);
        if (this.mWifiP2pManager != null && networkInfo.isConnected()) {
            if (!isSinkState(SinkState.SINK_STATE_WAITING_P2P_CONNECTION)) {
                return;
            }
            this.mWifiP2pManager.requestGroupInfo(this.mWifiP2pChannel, new WifiP2pManager.GroupInfoListener() {
                @Override
                public void onGroupInfoAvailable(WifiP2pGroup group) {
                    Slog.i(WifiDisplayController.TAG, "onGroupInfoAvailable(), mSinkState:" + WifiDisplayController.this.mSinkState);
                    if (!WifiDisplayController.this.isSinkState(SinkState.SINK_STATE_WAITING_P2P_CONNECTION)) {
                        return;
                    }
                    if (group == null) {
                        Slog.i(WifiDisplayController.TAG, "Error: group is null !!!");
                        return;
                    }
                    WifiDisplayController.this.mSinkP2pGroup = group;
                    boolean found = false;
                    if (group.getOwner().deviceAddress.equals(WifiDisplayController.this.mThisDevice.deviceAddress)) {
                        Slog.i(WifiDisplayController.TAG, "group owner is my self !");
                        Iterator c$iterator = group.getClientList().iterator();
                        while (true) {
                            if (!c$iterator.hasNext()) {
                                break;
                            }
                            WifiP2pDevice c = (WifiP2pDevice) c$iterator.next();
                            Slog.i(WifiDisplayController.TAG, "Client device:" + c);
                            if (WifiDisplayController.this.isWifiDisplaySource(c) && WifiDisplayController.this.mSinkDeviceName.equals(c.deviceName)) {
                                WifiDisplayController.this.mSinkMacAddress = c.deviceAddress;
                                WifiDisplayController.this.mSinkPort = c.wfdInfo.getControlPort();
                                Slog.i(WifiDisplayController.TAG, "Found ! Sink name:" + WifiDisplayController.this.mSinkDeviceName + ",mac address:" + WifiDisplayController.this.mSinkMacAddress + ",port:" + WifiDisplayController.this.mSinkPort);
                                found = true;
                                break;
                            }
                        }
                    } else {
                        Slog.i(WifiDisplayController.TAG, "group owner is not my self ! So I am GC.");
                        WifiDisplayController.this.mSinkMacAddress = group.getOwner().deviceAddress;
                        WifiDisplayController.this.mSinkPort = group.getOwner().wfdInfo.getControlPort();
                        Slog.i(WifiDisplayController.TAG, "Sink name:" + WifiDisplayController.this.mSinkDeviceName + ",mac address:" + WifiDisplayController.this.mSinkMacAddress + ",port:" + WifiDisplayController.this.mSinkPort);
                        found = true;
                    }
                    if (!found) {
                        return;
                    }
                    WifiDisplayController.this.mSinkIpRetryCount = 50;
                    WifiDisplayController.this.enterSinkState(SinkState.SINK_STATE_WIFI_P2P_CONNECTED);
                    WifiDisplayController.this.mHandler.postDelayed(WifiDisplayController.this.mGetSinkIpAddr, 300L);
                }
            });
        } else {
            if (!isSinkState(SinkState.SINK_STATE_WAITING_P2P_CONNECTION) && !isSinkState(SinkState.SINK_STATE_WIFI_P2P_CONNECTED)) {
                return;
            }
            disconnectWfdSink();
        }
    }

    private boolean isWifiDisplaySource(WifiP2pDevice device) {
        boolean result = (device.wfdInfo != null && device.wfdInfo.isWfdEnabled() && device.wfdInfo.isSessionAvailable()) ? isSourceDeviceType(device.wfdInfo.getDeviceType()) : false;
        if (!result) {
            Slog.e(TAG, "This is not WFD source device !!!!!!");
        }
        return result;
    }

    private void notifyDisplayConnecting() {
        WifiDisplay display = new WifiDisplay("Temp address", "WiFi Display Device", (String) null, true, true, false);
        Slog.d(TAG, "[sink] callback onDisplayConnecting()");
        this.mListener.onDisplayConnecting(display);
    }

    private boolean isSourceDeviceType(int deviceType) {
        return deviceType == 0 || deviceType == 3;
    }

    private void startWaitConnection() {
        Slog.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + ", mSinkState:" + this.mSinkState + ", retryCount:" + this.mSinkDiscoverRetryCount);
        this.mWifiP2pManager.discoverPeers(this.mWifiP2pChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                if (!WifiDisplayController.this.isSinkState(SinkState.SINK_STATE_WAITING_P2P_CONNECTION)) {
                    return;
                }
                Slog.d(WifiDisplayController.TAG, "[sink] succeed for discoverPeers()");
                WifiDisplayController.this.showDialog(7);
            }

            @Override
            public void onFailure(int reason) {
                if (!WifiDisplayController.this.isSinkState(SinkState.SINK_STATE_WAITING_P2P_CONNECTION)) {
                    return;
                }
                Slog.e(WifiDisplayController.TAG, "[sink] failed for discoverPeers(), reason:" + reason + ", retryCount:" + WifiDisplayController.this.mSinkDiscoverRetryCount);
                if (reason == 2 && WifiDisplayController.this.mSinkDiscoverRetryCount > 0) {
                    WifiDisplayController wifiDisplayController = WifiDisplayController.this;
                    wifiDisplayController.mSinkDiscoverRetryCount--;
                    WifiDisplayController.this.mHandler.postDelayed(WifiDisplayController.this.mSinkDiscover, 100L);
                } else {
                    WifiDisplayController.this.enterSinkState(SinkState.SINK_STATE_IDLE);
                    Slog.d(WifiDisplayController.TAG, "[sink] callback onDisplayConnectionFailed()");
                    WifiDisplayController.this.mListener.onDisplayConnectionFailed();
                }
            }
        });
    }

    private void connectRtsp() {
        Slog.d(TAG, "connectRtsp(), mSinkState:" + this.mSinkState);
        this.mRemoteDisplay = RemoteDisplay.connect(this.mSinkIpAddress, this.mSinkSurface, new RemoteDisplay.Listener() {
            public void onDisplayConnected(Surface surface, int width, int height, int flags, int session) {
                Slog.i(WifiDisplayController.TAG, "Opened RTSP connection! w:" + width + ",h:" + height);
                WifiDisplayController.this.dismissDialogDetail(WifiDisplayController.this.mBuildConnectDialog);
                WifiDisplayController.this.enterSinkState(SinkState.SINK_STATE_RTSP_CONNECTED);
                WifiDisplayController.this.mHandler.removeCallbacks(WifiDisplayController.this.mRtspSinkTimeout);
                WifiDisplay display = new WifiDisplay(WifiDisplayController.this.mSinkMacAddress, WifiDisplayController.this.mSinkDeviceName, (String) null, true, true, false);
                if (width < height) {
                    WifiDisplayController.this.sendPortraitIntent();
                }
                Slog.d(WifiDisplayController.TAG, "[sink] callback onDisplayConnected(), addr:" + WifiDisplayController.this.mSinkMacAddress + ", name:" + WifiDisplayController.this.mSinkDeviceName);
                WifiDisplayController.this.updateIfSinkConnected(true);
                WifiDisplayController.this.mListener.onDisplayConnected(display, null, 0, 0, 0);
            }

            public void onDisplayDisconnected() {
                Slog.i(WifiDisplayController.TAG, "Closed RTSP connection! mSinkState:" + WifiDisplayController.this.mSinkState);
                if (!WifiDisplayController.this.isSinkState(SinkState.SINK_STATE_WAITING_RTSP) && !WifiDisplayController.this.isSinkState(SinkState.SINK_STATE_RTSP_CONNECTED)) {
                    return;
                }
                WifiDisplayController.this.dismissDialogDetail(WifiDisplayController.this.mBuildConnectDialog);
                WifiDisplayController.this.mHandler.removeCallbacks(WifiDisplayController.this.mRtspSinkTimeout);
                WifiDisplayController.this.disconnectWfdSink();
            }

            public void onDisplayError(int error) {
                Slog.i(WifiDisplayController.TAG, "Lost RTSP connection! mSinkState:" + WifiDisplayController.this.mSinkState);
                if (!WifiDisplayController.this.isSinkState(SinkState.SINK_STATE_WAITING_RTSP) && !WifiDisplayController.this.isSinkState(SinkState.SINK_STATE_RTSP_CONNECTED)) {
                    return;
                }
                WifiDisplayController.this.dismissDialogDetail(WifiDisplayController.this.mBuildConnectDialog);
                WifiDisplayController.this.mHandler.removeCallbacks(WifiDisplayController.this.mRtspSinkTimeout);
                WifiDisplayController.this.disconnectWfdSink();
            }

            public void onDisplayKeyEvent(int keyCode, int flags) {
                Slog.d(WifiDisplayController.TAG, "onDisplayKeyEvent:");
            }

            public void onDisplayGenericMsgEvent(int event) {
            }
        }, this.mHandler);
        enterSinkState(SinkState.SINK_STATE_WAITING_RTSP);
        int rtspTimeout = this.mWifiDisplayCertMode ? RTSP_TIMEOUT_SECONDS_CERT_MODE : 10;
        this.mHandler.postDelayed(this.mRtspSinkTimeout, rtspTimeout * 1000);
    }

    private void blockNotificationList(boolean block) {
        Slog.i(TAG, "blockNotificationList(), block:" + block);
        if (block) {
            this.mStatusBarManager.disable(PackageManagerService.DumpState.DUMP_INSTALLS);
        } else {
            this.mStatusBarManager.disable(0);
        }
    }

    private void enterSinkState(SinkState state) {
        Slog.i(TAG, "enterSinkState()," + this.mSinkState + "->" + state);
        this.mSinkState = state;
    }

    private boolean isSinkState(SinkState state) {
        return this.mSinkState == state;
    }

    private void updateIfSinkConnected(boolean connected) {
        if (this.mIsWFDConnected == connected) {
            return;
        }
        this.mIsWFDConnected = connected;
        blockNotificationList(connected);
        Slog.i(TAG, "Set session available as " + (!connected));
        this.mWfdInfo.setSessionAvailable(connected ? false : true);
        this.mWifiP2pManager.setWFDInfo(this.mWifiP2pChannel, this.mWfdInfo, null);
        if (this.mWakeLockSink != null) {
            if (connected) {
                this.mWakeLockSink.acquire();
            } else {
                this.mWakeLockSink.release();
            }
        }
        getAudioFocus(connected);
    }

    private void getAudioFocus(boolean grab) {
        if (grab) {
            int ret = this.mAudioManager.requestAudioFocus(this.mAudioFocusListener, 3, 1);
            if (ret != 0) {
                return;
            }
            Slog.e(TAG, "requestAudioFocus() FAIL !!!");
            return;
        }
        this.mAudioManager.abandonAudioFocus(this.mAudioFocusListener);
    }

    private void setSinkMiracastMode() {
        Slog.i(TAG, "setSinkMiracastMode(), freq:" + this.mWifiApFreq);
        if (this.mWifiApConnected) {
            this.mWifiP2pManager.setMiracastMode(2, this.mWifiApFreq);
        } else {
            this.mWifiP2pManager.setMiracastMode(2);
        }
    }

    private void notifyApDisconnected() {
        Slog.e(TAG, "notifyApDisconnected()");
        Resources r = Resources.getSystem();
        Toast.makeText(this.mContext, r.getString(134545666, this.mWifiApSsid), 0).show();
        showNotification(134545667, 134545668);
    }

    private void startChannelConflictProcedure() {
        Slog.i(TAG, "startChannelConflictProcedure(), mChannelConflictState:" + this.mChannelConflictState + ",mWifiApConnected:" + this.mWifiApConnected);
        if (!isCCState(ChannelConflictState.STATE_IDLE)) {
            Slog.i(TAG, "State is wrong !!");
            return;
        }
        if (!this.mWifiApConnected) {
            Slog.i(TAG, "No WiFi AP Connected. Wrong !!");
            return;
        }
        if (wifiApHasSameFreq()) {
            this.mNetworkId = this.mWifiNetworkId;
            Slog.i(TAG, "Same Network Id:" + this.mNetworkId);
            this.mDisplayApToast = false;
            this.mWifiManager.disconnect();
            enterCCState(ChannelConflictState.STATE_AP_DISCONNECTING);
            return;
        }
        this.mNetworkId = getSameFreqNetworkId();
        if (this.mNetworkId == -1) {
            this.mWifiP2pManager.setFreqConflictExResult(this.mWifiP2pChannel, false, null);
            return;
        }
        this.mDisplayApToast = true;
        this.mWifiManager.disconnect();
        enterCCState(ChannelConflictState.STATE_AP_DISCONNECTING);
    }

    private void handleChannelConflictProcedure(ChannelConflictEvt event) {
        if (isCCState(null) || isCCState(ChannelConflictState.STATE_IDLE)) {
            return;
        }
        Slog.i(TAG, "handleChannelConflictProcedure(), evt:" + event + ", ccState:" + this.mChannelConflictState);
        if (isCCState(ChannelConflictState.STATE_AP_DISCONNECTING)) {
            if (event == ChannelConflictEvt.EVT_AP_DISCONNECTED) {
                this.mWifiP2pManager.setFreqConflictExResult(this.mWifiP2pChannel, true, null);
                enterCCState(ChannelConflictState.STATE_WFD_CONNECTING);
                return;
            } else {
                this.mWifiP2pManager.setFreqConflictExResult(this.mWifiP2pChannel, false, null);
                enterCCState(ChannelConflictState.STATE_IDLE);
                return;
            }
        }
        if (isCCState(ChannelConflictState.STATE_WFD_CONNECTING)) {
            if (event == ChannelConflictEvt.EVT_WFD_P2P_CONNECTED) {
                Slog.i(TAG, "connect AP, mNetworkId:" + this.mNetworkId);
                this.mWifiManager.connect(this.mNetworkId, null);
                enterCCState(ChannelConflictState.STATE_AP_CONNECTING);
                return;
            }
            enterCCState(ChannelConflictState.STATE_IDLE);
            return;
        }
        if (!isCCState(ChannelConflictState.STATE_AP_CONNECTING)) {
            return;
        }
        if (event == ChannelConflictEvt.EVT_AP_CONNECTED) {
            if (this.mDisplayApToast) {
                Resources r = Resources.getSystem();
                Toast.makeText(this.mContext, r.getString(134545664, this.mWifiApSsid), 0).show();
            }
            enterCCState(ChannelConflictState.STATE_IDLE);
            return;
        }
        enterCCState(ChannelConflictState.STATE_IDLE);
    }

    private boolean wifiApHasSameFreq() {
        Slog.i(TAG, "wifiApHasSameFreq()");
        if (this.mWifiApSsid == null || this.mWifiApSsid.length() < 2) {
            Slog.e(TAG, "mWifiApSsid is invalid !!");
            return false;
        }
        String apSsid = this.mWifiApSsid.substring(1, this.mWifiApSsid.length() - 1);
        List<ScanResult> results = this.mWifiManager.getScanResults();
        boolean found = false;
        if (results != null) {
            Iterator result$iterator = results.iterator();
            while (true) {
                if (!result$iterator.hasNext()) {
                    break;
                }
                ScanResult result = (ScanResult) result$iterator.next();
                Slog.i(TAG, "SSID:" + result.SSID + ",Freq:" + result.frequency + ",Level:" + result.level + ",BSSID:" + result.BSSID);
                if (result.SSID != null && result.SSID.length() != 0 && !result.capabilities.contains("[IBSS]") && result.SSID.equals(apSsid) && result.frequency == this.mP2pOperFreq) {
                    found = true;
                    break;
                }
            }
        }
        Slog.i(TAG, "AP SSID:" + apSsid + ", sameFreq:" + found);
        return found;
    }

    private int getSameFreqNetworkId() {
        Slog.i(TAG, "getSameFreqNetworkId()");
        List<WifiConfiguration> everConnecteds = this.mWifiManager.getConfiguredNetworks();
        List<ScanResult> results = this.mWifiManager.getScanResults();
        if (results == null || everConnecteds == null) {
            Slog.i(TAG, "results:" + results + ",everConnecteds:" + everConnecteds);
            return -1;
        }
        int maxRssi = -128;
        int selectedNetworkId = -1;
        for (WifiConfiguration everConnected : everConnecteds) {
            String trim = everConnected.SSID.substring(1, everConnected.SSID.length() - 1);
            Slog.i(TAG, "SSID:" + trim + ",NetId:" + everConnected.networkId);
            Iterator result$iterator = results.iterator();
            while (true) {
                if (result$iterator.hasNext()) {
                    ScanResult result = (ScanResult) result$iterator.next();
                    if (result.SSID != null && result.SSID.length() != 0 && !result.capabilities.contains("[IBSS]") && trim.equals(result.SSID) && result.frequency == this.mP2pOperFreq && result.level > maxRssi) {
                        selectedNetworkId = everConnected.networkId;
                        maxRssi = result.level;
                        break;
                    }
                }
            }
        }
        Slog.i(TAG, "Selected Network Id:" + selectedNetworkId);
        return selectedNetworkId;
    }

    private void enterCCState(ChannelConflictState state) {
        Slog.i(TAG, "enterCCState()," + this.mChannelConflictState + "->" + state);
        this.mChannelConflictState = state;
    }

    private boolean isCCState(ChannelConflictState state) {
        return this.mChannelConflictState == state;
    }
}
