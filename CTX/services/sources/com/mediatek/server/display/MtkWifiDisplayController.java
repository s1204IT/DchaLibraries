package com.mediatek.server.display;

import android.R;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.StatusBarManager;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
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
import android.net.util.NetworkConstants;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pGroupList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pWfdInfo;
import android.os.Handler;
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
import com.android.server.backup.BackupManagerConstants;
import com.android.server.hdmi.HdmiCecKeycode;
import com.android.server.job.controllers.JobStatus;
import com.android.server.usb.descriptors.UsbACInterface;
import com.android.server.usb.descriptors.UsbTerminalTypes;
import com.mediatek.server.MtkSystemServiceFactory;
import com.mediatek.server.powerhal.PowerHalManager;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MtkWifiDisplayController implements DumpUtils.Dump {
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
    private static final String TAG = "MTKWifiDisplayController";
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
    private static final int WFD_HDMI_EXCLUDED_DIALOG_HDMI_UPDATE = 3;
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
    private static final long WIFI_SCAN_TIMER = 100000;
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
    private AlarmManager mAlarmManager;
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
    private boolean mFast_NeedFastRtsp;
    private final Handler mHandler;
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
    private final AlarmManager.OnAlarmListener mWifiScanTimerListener = new AlarmManager.OnAlarmListener() {
        @Override
        public void onAlarm() {
            Slog.i(MtkWifiDisplayController.TAG, "Stop WiFi scan/reconnect due to scan timer timeout");
            MtkWifiDisplayController.this.stopWifiScan(true);
        }
    };
    private boolean mWifiPowerSaving = true;
    private int mP2pOperFreq = 0;
    private int mNetworkId = -1;
    private boolean mSinkEnabled = false;
    public PowerHalManager mPowerHalManager = MtkSystemServiceFactory.getInstance().makePowerHalManager();
    private final Runnable mDiscoverPeers = new Runnable() {
        @Override
        public void run() {
            Slog.d(MtkWifiDisplayController.TAG, "mDiscoverPeers, run()");
            MtkWifiDisplayController.this.tryDiscoverPeers();
        }
    };
    private final Runnable mConnectionTimeout = new Runnable() {
        @Override
        public void run() {
            if (MtkWifiDisplayController.this.mConnectingDevice != null && MtkWifiDisplayController.this.mConnectingDevice == MtkWifiDisplayController.this.mDesiredDevice) {
                Slog.i(MtkWifiDisplayController.TAG, "Timed out waiting for Wifi display connection after 60 seconds: " + MtkWifiDisplayController.this.mConnectingDevice.deviceName);
                MtkWifiDisplayController.this.handleConnectionFailure(true);
            }
        }
    };
    private final Runnable mRtspTimeout = new Runnable() {
        @Override
        public void run() {
            if (MtkWifiDisplayController.this.mConnectedDevice != null && MtkWifiDisplayController.this.mRemoteDisplay != null && !MtkWifiDisplayController.this.mRemoteDisplayConnected) {
                Slog.i(MtkWifiDisplayController.TAG, "Timed out waiting for Wifi display RTSP connection after 75 seconds: " + MtkWifiDisplayController.this.mConnectedDevice.deviceName);
                MtkWifiDisplayController.this.handleConnectionFailure(true);
            }
        }
    };
    private final BroadcastReceiver mWifiP2pReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ChannelConflictEvt channelConflictEvt;
            String action = intent.getAction();
            if (action.equals("android.net.wifi.p2p.STATE_CHANGED")) {
                boolean z = intent.getIntExtra("wifi_p2p_state", 1) == 2;
                Slog.d(MtkWifiDisplayController.TAG, "Received WIFI_P2P_STATE_CHANGED_ACTION: enabled=" + z);
                MtkWifiDisplayController.this.handleStateChanged(z);
                return;
            }
            if (action.equals("android.net.wifi.p2p.PEERS_CHANGED")) {
                if (MtkWifiDisplayController.DEBUG) {
                    Slog.d(MtkWifiDisplayController.TAG, "Received WIFI_P2P_PEERS_CHANGED_ACTION.");
                }
                MtkWifiDisplayController.this.handlePeersChanged();
                return;
            }
            if (action.equals("android.net.wifi.p2p.CONNECTION_STATE_CHANGE")) {
                NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                int intExtra = intent.getIntExtra("reason=", -1);
                if (MtkWifiDisplayController.DEBUG) {
                    Slog.d(MtkWifiDisplayController.TAG, "Received WIFI_P2P_CONNECTION_CHANGED_ACTION: networkInfo=" + networkInfo + ", reason = " + intExtra);
                } else {
                    Slog.d(MtkWifiDisplayController.TAG, "Received WIFI_P2P_CONNECTION_CHANGED_ACTION: isConnected? " + networkInfo.isConnected() + ", reason = " + intExtra);
                }
                MtkWifiDisplayController.this.updateWifiP2pChannelId(networkInfo.isConnected(), intent);
                if (!SystemProperties.get("ro.vendor.mtk_wfd_sink_support").equals("1") || !MtkWifiDisplayController.this.mSinkEnabled) {
                    MtkWifiDisplayController.this.handleConnectionChanged(networkInfo, intExtra);
                    MtkWifiDisplayController.this.mLastTimeConnected = networkInfo.isConnected();
                    return;
                } else {
                    if (intExtra != -2) {
                        MtkWifiDisplayController.this.handleSinkP2PConnection(networkInfo);
                        return;
                    }
                    return;
                }
            }
            if (action.equals("android.net.wifi.p2p.THIS_DEVICE_CHANGED")) {
                MtkWifiDisplayController.this.mThisDevice = (WifiP2pDevice) intent.getParcelableExtra("wifiP2pDevice");
                if (MtkWifiDisplayController.DEBUG) {
                    Slog.d(MtkWifiDisplayController.TAG, "Received WIFI_P2P_THIS_DEVICE_CHANGED_ACTION: mThisDevice= " + MtkWifiDisplayController.this.mThisDevice);
                    return;
                }
                return;
            }
            if (action.equals(MtkWifiDisplayController.DRM_CONTENT_MEDIAPLAYER)) {
                MtkWifiDisplayController.this.mDRMContent_Mediaplayer = intent.getBooleanExtra("isPlaying", false);
                int intExtra2 = intent.getIntExtra("playerId", 0);
                Slog.i(MtkWifiDisplayController.TAG, "Received DRM_CONTENT_MEDIAPLAYER: isPlaying = " + MtkWifiDisplayController.this.mDRMContent_Mediaplayer + ", player = " + intExtra2 + ", isConnected = " + MtkWifiDisplayController.this.mIsWFDConnected + ", isConnecting = " + MtkWifiDisplayController.this.mRTSPConnecting);
                if (true == MtkWifiDisplayController.this.mIsWFDConnected || true == MtkWifiDisplayController.this.mRTSPConnecting) {
                    if (true == MtkWifiDisplayController.this.mDRMContent_Mediaplayer) {
                        MtkWifiDisplayController.this.mPlayerID_Mediaplayer = intExtra2;
                        return;
                    } else {
                        if (MtkWifiDisplayController.this.mPlayerID_Mediaplayer != intExtra2) {
                            Slog.w(MtkWifiDisplayController.TAG, "player ID doesn't match last time: " + MtkWifiDisplayController.this.mPlayerID_Mediaplayer);
                            return;
                        }
                        return;
                    }
                }
                return;
            }
            if (action.equals("android.net.wifi.p2p.DISCOVERY_STATE_CHANGE")) {
                int intExtra3 = intent.getIntExtra("discoveryState", 1);
                if (MtkWifiDisplayController.DEBUG) {
                    Slog.d(MtkWifiDisplayController.TAG, "Received WIFI_P2P_DISCOVERY_CHANGED_ACTION: discoveryState=" + intExtra3);
                }
                if (intExtra3 == 1) {
                    MtkWifiDisplayController.this.handleScanFinished();
                    return;
                }
                return;
            }
            if (action.equals(MtkWifiDisplayController.WFDCONTROLLER_PRE_SHUTDOWN)) {
                Slog.i(MtkWifiDisplayController.TAG, "Received android.intent.action.ACTION_PRE_SHUTDOWN, do disconnect anyway");
                if (MtkWifiDisplayController.this.mWifiP2pManager != null) {
                    MtkWifiDisplayController.this.mWifiP2pManager.removeGroup(MtkWifiDisplayController.this.mWifiP2pChannel, null);
                }
                if (MtkWifiDisplayController.this.mRemoteDisplay != null) {
                    MtkWifiDisplayController.this.mRemoteDisplay.dispose();
                    return;
                }
                return;
            }
            if (action.equals("android.net.wifi.STATE_CHANGE")) {
                NetworkInfo networkInfo2 = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                if (MtkWifiDisplayController.this.mIsWFDConnected) {
                    NetworkInfo.State state = networkInfo2.getState();
                    if (state == NetworkInfo.State.DISCONNECTED && MtkWifiDisplayController.this.mStopWifiScan) {
                        Slog.i(MtkWifiDisplayController.TAG, "Resume WiFi scan/reconnect if WiFi is disconnected");
                        MtkWifiDisplayController.this.stopWifiScan(false);
                        MtkWifiDisplayController.this.mAlarmManager.cancel(MtkWifiDisplayController.this.mWifiScanTimerListener);
                        MtkWifiDisplayController.this.mAlarmManager.set(2, SystemClock.elapsedRealtime() + MtkWifiDisplayController.WIFI_SCAN_TIMER, "Set WiFi scan timer", MtkWifiDisplayController.this.mWifiScanTimerListener, MtkWifiDisplayController.this.mHandler);
                    } else if (state == NetworkInfo.State.CONNECTED && !MtkWifiDisplayController.this.mStopWifiScan) {
                        Slog.i(MtkWifiDisplayController.TAG, "Stop WiFi scan/reconnect if WiFi is connected");
                        MtkWifiDisplayController.this.mAlarmManager.cancel(MtkWifiDisplayController.this.mWifiScanTimerListener);
                        MtkWifiDisplayController.this.stopWifiScan(true);
                    }
                }
                boolean zIsConnected = networkInfo2.isConnected();
                boolean z2 = zIsConnected != MtkWifiDisplayController.this.mWifiApConnected;
                MtkWifiDisplayController.this.mWifiApConnected = zIsConnected;
                if (MtkWifiDisplayController.this.mWifiApConnected) {
                    WifiInfo connectionInfo = MtkWifiDisplayController.this.mWifiManager.getConnectionInfo();
                    if (connectionInfo != null) {
                        if (!connectionInfo.getSSID().equals(MtkWifiDisplayController.this.mWifiApSsid) || connectionInfo.getFrequency() != MtkWifiDisplayController.this.mWifiApFreq || connectionInfo.getNetworkId() != MtkWifiDisplayController.this.mWifiNetworkId) {
                            z2 = true;
                        }
                        MtkWifiDisplayController.this.mWifiApSsid = connectionInfo.getSSID();
                        MtkWifiDisplayController.this.mWifiApFreq = connectionInfo.getFrequency();
                        MtkWifiDisplayController.this.mWifiNetworkId = connectionInfo.getNetworkId();
                    }
                } else {
                    MtkWifiDisplayController.this.mWifiApSsid = null;
                    MtkWifiDisplayController.this.mWifiApFreq = 0;
                    MtkWifiDisplayController.this.mWifiNetworkId = -1;
                }
                Slog.i(MtkWifiDisplayController.TAG, "Received NETWORK_STATE_CHANGED,con:" + MtkWifiDisplayController.this.mWifiApConnected + ",SSID:" + MtkWifiDisplayController.this.mWifiApSsid + ",Freq:" + MtkWifiDisplayController.this.mWifiApFreq + ",netId:" + MtkWifiDisplayController.this.mWifiNetworkId + ", updated:" + z2);
                if (!z2) {
                    return;
                }
                if (SystemProperties.get("ro.vendor.mtk_wfd_sink_support").equals("1") && MtkWifiDisplayController.this.mSinkEnabled) {
                    MtkWifiDisplayController.this.setSinkMiracastMode();
                }
                MtkWifiDisplayController mtkWifiDisplayController = MtkWifiDisplayController.this;
                if (MtkWifiDisplayController.this.mWifiApConnected) {
                    channelConflictEvt = ChannelConflictEvt.EVT_AP_CONNECTED;
                } else {
                    channelConflictEvt = ChannelConflictEvt.EVT_AP_DISCONNECTED;
                }
                mtkWifiDisplayController.handleChannelConflictProcedure(channelConflictEvt);
                return;
            }
            if (action.equals(MtkWifiDisplayController.WFD_SINK_GC_REQUEST_CONNECT)) {
                MtkWifiDisplayController.this.mSinkDeviceName = intent.getStringExtra("deviceName");
                Slog.i(MtkWifiDisplayController.TAG, "Received WFD_SINK_GC_REQUEST_CONNECT, mSinkDeviceName:" + MtkWifiDisplayController.this.mSinkDeviceName);
                if (MtkWifiDisplayController.this.isSinkState(SinkState.SINK_STATE_WAITING_P2P_CONNECTION)) {
                    if (MtkWifiDisplayController.this.mSinkDeviceName != null) {
                        MtkWifiDisplayController.this.showDialog(8);
                        return;
                    }
                    return;
                } else {
                    Slog.d(MtkWifiDisplayController.TAG, "State is wrong. Decline directly !!");
                    MtkWifiDisplayController.this.mWifiP2pManager.setGCInviteResult(MtkWifiDisplayController.this.mWifiP2pChannel, false, 0, null);
                    return;
                }
            }
            if (action.equals(MtkWifiDisplayController.WFD_CHANNEL_CONFLICT_OCCURS)) {
                MtkWifiDisplayController.this.mP2pOperFreq = intent.getIntExtra("p2pOperFreq", -1);
                Slog.i(MtkWifiDisplayController.TAG, "Received WFD_CHANNEL_CONFLICT_OCCURS, p2pOperFreq:" + MtkWifiDisplayController.this.mP2pOperFreq);
                if (MtkWifiDisplayController.this.mP2pOperFreq != -1) {
                    MtkWifiDisplayController.this.startChannelConflictProcedure();
                    return;
                }
                return;
            }
            if (action.equals(MtkWifiDisplayController.WFD_SINK_CHANNEL_CONFLICT_OCCURS)) {
                Slog.i(MtkWifiDisplayController.TAG, "Received WFD_SINK_CHANNEL_CONFLICT_OCCURS, mSinkEnabled:" + MtkWifiDisplayController.this.mSinkEnabled + ", apConnected:" + MtkWifiDisplayController.this.mWifiApConnected);
                if (SystemProperties.get("ro.vendor.mtk_wfd_sink_support").equals("1") && MtkWifiDisplayController.this.mSinkEnabled && MtkWifiDisplayController.this.mWifiApConnected) {
                    MtkWifiDisplayController.this.notifyApDisconnected();
                }
            }
        }
    };
    private final ContentObserver mObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean z, Uri uri) {
            if (!z) {
                MtkWifiDisplayController.this.WFDCONTROLLER_DISPLAY_TOAST_TIME = Settings.Global.getInt(MtkWifiDisplayController.this.mContext.getContentResolver(), MtkWifiDisplayController.this.getMtkSettingsExtGlobalSetting("WIFI_DISPLAY_DISPLAY_TOAST_TIME"), 20);
                MtkWifiDisplayController.this.WFDCONTROLLER_DISPLAY_NOTIFICATION_TIME = Settings.Global.getInt(MtkWifiDisplayController.this.mContext.getContentResolver(), MtkWifiDisplayController.this.getMtkSettingsExtGlobalSetting("WIFI_DISPLAY_DISPLAY_NOTIFICATION_TIME"), MtkWifiDisplayController.RTSP_TIMEOUT_SECONDS_CERT_MODE);
                MtkWifiDisplayController.this.WFDCONTROLLER_SQC_INFO_ON = Settings.Global.getInt(MtkWifiDisplayController.this.mContext.getContentResolver(), MtkWifiDisplayController.this.getMtkSettingsExtGlobalSetting("WIFI_DISPLAY_SQC_INFO_ON"), 0) != 0;
                MtkWifiDisplayController.this.WFDCONTROLLER_QE_ON = Settings.Global.getInt(MtkWifiDisplayController.this.mContext.getContentResolver(), MtkWifiDisplayController.this.getMtkSettingsExtGlobalSetting("WIFI_DISPLAY_QE_ON"), 0) != 0;
                MtkWifiDisplayController.this.mAutoChannelSelection = Settings.Global.getInt(MtkWifiDisplayController.this.mContext.getContentResolver(), MtkWifiDisplayController.this.getMtkSettingsExtGlobalSetting("WIFI_DISPLAY_AUTO_CHANNEL_SELECTION"), 0) != 0;
                Slog.d(MtkWifiDisplayController.TAG, "onChange(), t_time:" + MtkWifiDisplayController.this.WFDCONTROLLER_DISPLAY_TOAST_TIME + ",n_time:" + MtkWifiDisplayController.this.WFDCONTROLLER_DISPLAY_NOTIFICATION_TIME + ",sqc:" + MtkWifiDisplayController.this.WFDCONTROLLER_SQC_INFO_ON + ",qe:" + MtkWifiDisplayController.this.WFDCONTROLLER_QE_ON + ",autoChannel:" + MtkWifiDisplayController.this.mAutoChannelSelection);
                if (SystemProperties.get("ro.vendor.mtk_wfd_support").equals("1")) {
                    MtkWifiDisplayController.this.handleResolutionChange();
                    MtkWifiDisplayController.this.handleLatencyProfilingChange();
                    MtkWifiDisplayController.this.handleSecureOptionChange();
                    MtkWifiDisplayController.this.handlePortraitResolutionSupportChange();
                }
            }
        }
    };
    private final Runnable mLatencyInfo = new Runnable() {
        @Override
        public void run() {
            if (MtkWifiDisplayController.this.mConnectedDevice != null) {
                if (MtkWifiDisplayController.this.mRemoteDisplay != null) {
                    if (MtkWifiDisplayController.this.mLatencyProfiling == 0 || MtkWifiDisplayController.this.mLatencyProfiling == 1 || MtkWifiDisplayController.this.mLatencyProfiling == 3) {
                        int wifiApNum = MtkWifiDisplayController.this.getWifiApNum();
                        String str = MtkWifiDisplayController.this.mWifiP2pChannelId + "," + wifiApNum + "," + MtkWifiDisplayController.this.mWifiScore + "," + MtkWifiDisplayController.this.mWifiRate + "," + MtkWifiDisplayController.this.mRSSI;
                        Slog.d(MtkWifiDisplayController.TAG, "WifiInfo:" + str);
                        int wfdParam = MtkWifiDisplayController.this.mRemoteDisplay.getWfdParam(5);
                        int wfdParam2 = MtkWifiDisplayController.this.mRemoteDisplay.getWfdParam(6);
                        String str2 = wfdParam + ",0,0";
                        Slog.d(MtkWifiDisplayController.TAG, "WFDLatency:" + str2);
                        if (MtkWifiDisplayController.this.mLatencyProfiling != 0 && MtkWifiDisplayController.this.mLatencyProfiling != 1) {
                            if (MtkWifiDisplayController.this.mLatencyProfiling == 3) {
                                MtkWifiDisplayController.this.mTextView.setText("AP:" + wifiApNum + "\nS:" + MtkWifiDisplayController.this.mWifiScore + "\nR:" + MtkWifiDisplayController.this.mWifiRate + "\nRS:" + MtkWifiDisplayController.this.mRSSI + "\nAL:" + wfdParam + "\nSF:" + wfdParam2 + "\n");
                            }
                        } else {
                            Settings.Global.putString(MtkWifiDisplayController.this.mContext.getContentResolver(), MtkWifiDisplayController.this.getMtkSettingsExtGlobalSetting("WIFI_DISPLAY_WIFI_INFO"), str);
                            Settings.Global.putString(MtkWifiDisplayController.this.mContext.getContentResolver(), MtkWifiDisplayController.this.getMtkSettingsExtGlobalSetting("WIFI_DISPLAY_WFD_LATENCY"), str2);
                        }
                        MtkWifiDisplayController.this.mHandler.postDelayed(MtkWifiDisplayController.this.mLatencyInfo, 3000L);
                        return;
                    }
                    Slog.e(MtkWifiDisplayController.TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + ": mLatencyProfiling:" + MtkWifiDisplayController.this.mLatencyProfiling);
                    return;
                }
                Slog.e(MtkWifiDisplayController.TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + ": RemoteDisplay is null");
                return;
            }
            Slog.e(MtkWifiDisplayController.TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + ": ConnectedDevice is null");
        }
    };
    private final Runnable mScanWifiAp = new Runnable() {
        @Override
        public void run() {
            if (MtkWifiDisplayController.this.mConnectedDevice != null) {
                if (MtkWifiDisplayController.this.mRemoteDisplay != null) {
                    if (MtkWifiDisplayController.this.mLatencyProfiling != 0 && MtkWifiDisplayController.this.mLatencyProfiling != 1 && MtkWifiDisplayController.this.mLatencyProfiling != 3) {
                        Slog.e(MtkWifiDisplayController.TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + ": mLatencyProfiling:" + MtkWifiDisplayController.this.mLatencyProfiling);
                        return;
                    }
                    Slog.d(MtkWifiDisplayController.TAG, "call mWifiManager.startScan()");
                    MtkWifiDisplayController.this.mWifiManager.startScan();
                    return;
                }
                Slog.e(MtkWifiDisplayController.TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + ": RemoteDisplay is null");
                return;
            }
            Slog.e(MtkWifiDisplayController.TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + ": ConnectedDevice is null");
        }
    };
    private final Runnable mDelayProfiling = new Runnable() {
        @Override
        public void run() {
            if (MtkWifiDisplayController.this.mLatencyProfiling == 3 && MtkWifiDisplayController.this.mIsWFDConnected) {
                MtkWifiDisplayController.this.startProfilingInfo();
            }
        }
    };
    private final Runnable mDisplayToast = new Runnable() {
        @Override
        public void run() {
            Slog.d(MtkWifiDisplayController.TAG, "mDisplayToast run()" + MtkWifiDisplayController.this.mLevel);
            Resources system = Resources.getSystem();
            if (MtkWifiDisplayController.this.mLevel != 0) {
                Toast.makeText(MtkWifiDisplayController.this.mContext, system.getString(MtkWifiDisplayController.this.getMtkStringResourceId("wifi_display_connection_is_not_steady")), 0).show();
            }
            MtkWifiDisplayController.this.mToastTimerStarted = false;
        }
    };
    private final Runnable mDisplayNotification = new Runnable() {
        @Override
        public void run() {
            Slog.d(MtkWifiDisplayController.TAG, "mDisplayNotification run()" + MtkWifiDisplayController.this.mLevel);
            if (MtkWifiDisplayController.this.mLevel != 0) {
                MtkWifiDisplayController.this.showNotification(MtkWifiDisplayController.this.getMtkStringResourceId("wifi_display_unstable_connection"), MtkWifiDisplayController.this.getMtkStringResourceId("wifi_display_unstable_suggestion"));
            }
            MtkWifiDisplayController.this.mNotiTimerStarted = false;
        }
    };
    private final Runnable mReConnect = new Runnable() {
        @Override
        public void run() {
            Slog.d(MtkWifiDisplayController.TAG, "mReConnect, run()");
            if (MtkWifiDisplayController.this.mReConnectDevice != null) {
                for (WifiP2pDevice wifiP2pDevice : MtkWifiDisplayController.this.mAvailableWifiDisplayPeers) {
                    if (MtkWifiDisplayController.DEBUG) {
                        Slog.d(MtkWifiDisplayController.TAG, "\t" + MtkWifiDisplayController.describeWifiP2pDevice(wifiP2pDevice));
                    }
                    if (wifiP2pDevice.deviceAddress.equals(MtkWifiDisplayController.this.mReConnectDevice.deviceAddress)) {
                        Slog.i(MtkWifiDisplayController.TAG, "connect() in mReConnect. Set mReConnecting as true");
                        MtkWifiDisplayController.this.mReScanning = false;
                        MtkWifiDisplayController.this.mReConnecting = true;
                        MtkWifiDisplayController.this.connect(wifiP2pDevice);
                        return;
                    }
                }
                MtkWifiDisplayController.this.mReConnection_Timeout_Remain_Seconds--;
                if (MtkWifiDisplayController.this.mReConnection_Timeout_Remain_Seconds > 0) {
                    Slog.i(MtkWifiDisplayController.TAG, "post mReconnect, s:" + MtkWifiDisplayController.this.mReConnection_Timeout_Remain_Seconds);
                    MtkWifiDisplayController.this.mHandler.postDelayed(MtkWifiDisplayController.this.mReConnect, 1000L);
                    return;
                }
                Slog.e(MtkWifiDisplayController.TAG, "reconnect timeout!");
                Toast.makeText(MtkWifiDisplayController.this.mContext, MtkWifiDisplayController.this.getMtkStringResourceId("wifi_display_disconnected"), 0).show();
                MtkWifiDisplayController.this.resetReconnectVariable();
                return;
            }
            Slog.w(MtkWifiDisplayController.TAG, "no reconnect device");
        }
    };
    private final Runnable mSinkDiscover = new Runnable() {
        @Override
        public void run() {
            Slog.d(MtkWifiDisplayController.TAG, "mSinkDiscover run(), count:" + MtkWifiDisplayController.this.mSinkDiscoverRetryCount);
            if (!MtkWifiDisplayController.this.isSinkState(SinkState.SINK_STATE_WAITING_P2P_CONNECTION)) {
                Slog.d(MtkWifiDisplayController.TAG, "mSinkState:(" + MtkWifiDisplayController.this.mSinkState + ") is wrong !");
                return;
            }
            MtkWifiDisplayController.this.startWaitConnection();
        }
    };
    private final Runnable mGetSinkIpAddr = new Runnable() {
        @Override
        public void run() {
            Slog.d(MtkWifiDisplayController.TAG, "mGetSinkIpAddr run(), count:" + MtkWifiDisplayController.this.mSinkIpRetryCount);
            if (!MtkWifiDisplayController.this.isSinkState(SinkState.SINK_STATE_WIFI_P2P_CONNECTED)) {
                Slog.d(MtkWifiDisplayController.TAG, "mSinkState:(" + MtkWifiDisplayController.this.mSinkState + ") is wrong !");
                return;
            }
            MtkWifiDisplayController.this.mSinkIpAddress = MtkWifiDisplayController.this.mWifiP2pManager.getPeerIpAddress(MtkWifiDisplayController.this.mSinkMacAddress);
            if (MtkWifiDisplayController.this.mSinkIpAddress == null) {
                if (MtkWifiDisplayController.this.mSinkIpRetryCount > 0) {
                    MtkWifiDisplayController.access$12910(MtkWifiDisplayController.this);
                    MtkWifiDisplayController.this.mHandler.postDelayed(MtkWifiDisplayController.this.mGetSinkIpAddr, 1000L);
                    return;
                } else {
                    Slog.d(MtkWifiDisplayController.TAG, "mGetSinkIpAddr FAIL !!!!!!");
                    return;
                }
            }
            MtkWifiDisplayController.access$13584(MtkWifiDisplayController.this, ":" + MtkWifiDisplayController.this.mSinkPort);
            Slog.i(MtkWifiDisplayController.TAG, "sink Ip address = " + MtkWifiDisplayController.this.mSinkIpAddress);
            MtkWifiDisplayController.this.connectRtsp();
        }
    };
    private final Runnable mRtspSinkTimeout = new Runnable() {
        @Override
        public void run() {
            Slog.d(MtkWifiDisplayController.TAG, "mRtspSinkTimeout, run()");
            MtkWifiDisplayController.this.disconnectWfdSink();
        }
    };
    private AudioManager.OnAudioFocusChangeListener mAudioFocusListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int i) {
            Slog.e(MtkWifiDisplayController.TAG, "onAudioFocusChange(), focus:" + i);
            if (i == -1 || i != 1) {
            }
        }
    };
    private final Runnable mEnableWifiDelay = new Runnable() {
        @Override
        public void run() {
            Slog.d(MtkWifiDisplayController.TAG, "Enable wifi automatically.");
            MtkWifiDisplayController.this.mAutoEnableWifi = true;
            int wifiApState = MtkWifiDisplayController.this.mWifiManager.getWifiApState();
            if (wifiApState == 12 || wifiApState == 13) {
                ((ConnectivityManager) MtkWifiDisplayController.this.mContext.getSystemService("connectivity")).stopTethering(0);
            }
            MtkWifiDisplayController.this.mWifiManager.setWifiEnabled(true);
        }
    };

    enum ChannelConflictEvt {
        EVT_AP_DISCONNECTED,
        EVT_AP_CONNECTED,
        EVT_WFD_P2P_DISCONNECTED,
        EVT_WFD_P2P_CONNECTED
    }

    enum ChannelConflictState {
        STATE_IDLE,
        STATE_AP_DISCONNECTING,
        STATE_WFD_CONNECTING,
        STATE_AP_CONNECTING
    }

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

    enum SinkState {
        SINK_STATE_IDLE,
        SINK_STATE_WAITING_P2P_CONNECTION,
        SINK_STATE_WIFI_P2P_CONNECTED,
        SINK_STATE_WAITING_RTSP,
        SINK_STATE_RTSP_CONNECTED
    }

    static int access$12910(MtkWifiDisplayController mtkWifiDisplayController) {
        int i = mtkWifiDisplayController.mSinkIpRetryCount;
        mtkWifiDisplayController.mSinkIpRetryCount = i - 1;
        return i;
    }

    static int access$13210(MtkWifiDisplayController mtkWifiDisplayController) {
        int i = mtkWifiDisplayController.mSinkDiscoverRetryCount;
        mtkWifiDisplayController.mSinkDiscoverRetryCount = i - 1;
        return i;
    }

    static String access$13584(MtkWifiDisplayController mtkWifiDisplayController, Object obj) {
        String str = mtkWifiDisplayController.mSinkIpAddress + obj;
        mtkWifiDisplayController.mSinkIpAddress = str;
        return str;
    }

    static int access$4620(MtkWifiDisplayController mtkWifiDisplayController, int i) {
        int i2 = mtkWifiDisplayController.mConnectionRetriesLeft - i;
        mtkWifiDisplayController.mConnectionRetriesLeft = i2;
        return i2;
    }

    public MtkWifiDisplayController(Context context, Handler handler, Listener listener) {
        this.mContext = context;
        this.mHandler = handler;
        this.mListener = listener;
        this.mWifiP2pManager = (WifiP2pManager) context.getSystemService("wifip2p");
        this.mWifiP2pChannel = this.mWifiP2pManager.initialize(context, handler.getLooper(), null);
        getWifiLock();
        this.mWfdInfo = new WifiP2pWfdInfo();
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
        ContentObserver contentObserver = new ContentObserver(this.mHandler) {
            @Override
            public void onChange(boolean z, Uri uri) {
                if (!z) {
                    MtkWifiDisplayController.this.updateSettings();
                }
            }
        };
        ContentResolver contentResolver = this.mContext.getContentResolver();
        contentResolver.registerContentObserver(Settings.Global.getUriFor("wifi_display_on"), false, contentObserver);
        contentResolver.registerContentObserver(Settings.Global.getUriFor("wifi_display_certification_on"), false, contentObserver);
        contentResolver.registerContentObserver(Settings.Global.getUriFor("wifi_display_wps_config"), false, contentObserver);
        updateSettings();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getRealMetrics(displayMetrics);
        Slog.i(TAG, "RealMetrics, Width = " + displayMetrics.widthPixels + ", Height = " + displayMetrics.heightPixels);
        if (displayMetrics.widthPixels < displayMetrics.heightPixels) {
            this.mIsNeedRotate = true;
        }
        registerEMObserver(displayMetrics.widthPixels, displayMetrics.heightPixels);
        this.mNotificationManager = (NotificationManager) context.getSystemService("notification");
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        actionAtDisconnected(null);
        updateWfdStatFile(0);
        PowerManager powerManager = (PowerManager) context.getSystemService("power");
        this.mWakeLock = powerManager.newWakeLock(26, "UIBC Source");
        this.mStatusBarManager = (StatusBarManager) context.getSystemService("statusbar");
        this.mWakeLockSink = powerManager.newWakeLock(26, "WFD Sink");
    }

    private void updateSettings() {
        ContentResolver contentResolver = this.mContext.getContentResolver();
        this.mWifiDisplayOnSetting = Settings.Global.getInt(contentResolver, "wifi_display_on", 0) != 0;
        this.mWifiDisplayCertMode = Settings.Global.getInt(contentResolver, "wifi_display_certification_on", 0) != 0;
        this.mWifiDisplayWpsConfig = 4;
        if (this.mWifiDisplayCertMode) {
            this.mWifiDisplayWpsConfig = Settings.Global.getInt(contentResolver, "wifi_display_wps_config", 4);
        }
        loadDebugLevel();
        enableWifiDisplay();
    }

    public void dump(PrintWriter printWriter, String str) {
        printWriter.println("mWifiDisplayOnSetting=" + this.mWifiDisplayOnSetting);
        printWriter.println("mWifiP2pEnabled=" + this.mWifiP2pEnabled);
        printWriter.println("mWfdEnabled=" + this.mWfdEnabled);
        printWriter.println("mWfdEnabling=" + this.mWfdEnabling);
        printWriter.println("mNetworkInfo=" + this.mNetworkInfo);
        printWriter.println("mScanRequested=" + this.mScanRequested);
        printWriter.println("mDiscoverPeersInProgress=" + this.mDiscoverPeersInProgress);
        printWriter.println("mDesiredDevice=" + describeWifiP2pDevice(this.mDesiredDevice));
        printWriter.println("mConnectingDisplay=" + describeWifiP2pDevice(this.mConnectingDevice));
        printWriter.println("mDisconnectingDisplay=" + describeWifiP2pDevice(this.mDisconnectingDevice));
        printWriter.println("mCancelingDisplay=" + describeWifiP2pDevice(this.mCancelingDevice));
        printWriter.println("mConnectedDevice=" + describeWifiP2pDevice(this.mConnectedDevice));
        printWriter.println("mConnectionRetriesLeft=" + this.mConnectionRetriesLeft);
        printWriter.println("mRemoteDisplay=" + this.mRemoteDisplay);
        printWriter.println("mRemoteDisplayInterface=" + this.mRemoteDisplayInterface);
        printWriter.println("mRemoteDisplayConnected=" + this.mRemoteDisplayConnected);
        printWriter.println("mAdvertisedDisplay=" + this.mAdvertisedDisplay);
        printWriter.println("mAdvertisedDisplaySurface=" + this.mAdvertisedDisplaySurface);
        printWriter.println("mAdvertisedDisplayWidth=" + this.mAdvertisedDisplayWidth);
        printWriter.println("mAdvertisedDisplayHeight=" + this.mAdvertisedDisplayHeight);
        printWriter.println("mAdvertisedDisplayFlags=" + this.mAdvertisedDisplayFlags);
        printWriter.println("mBackupShowTouchVal=" + this.mBackupShowTouchVal);
        printWriter.println("mFast_NeedFastRtsp=" + this.mFast_NeedFastRtsp);
        printWriter.println("mIsNeedRotate=" + this.mIsNeedRotate);
        printWriter.println("mIsConnected_OtherP2p=" + this.mIsConnected_OtherP2p);
        printWriter.println("mIsConnecting_P2p_Rtsp=" + this.mIsConnecting_P2p_Rtsp);
        printWriter.println("mIsWFDConnected=" + this.mIsWFDConnected);
        printWriter.println("mDRMContent_Mediaplayer=" + this.mDRMContent_Mediaplayer);
        printWriter.println("mPlayerID_Mediaplayer=" + this.mPlayerID_Mediaplayer);
        printWriter.println("mAvailableWifiDisplayPeers: size=" + this.mAvailableWifiDisplayPeers.size());
        Iterator<WifiP2pDevice> it = this.mAvailableWifiDisplayPeers.iterator();
        while (it.hasNext()) {
            printWriter.println("  " + describeWifiP2pDevice(it.next()));
        }
    }

    public void requestStartScan() {
        Slog.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + ",mSinkEnabled:" + this.mSinkEnabled);
        if ((!SystemProperties.get("ro.vendor.mtk_wfd_sink_support").equals("1") || !this.mSinkEnabled) && !this.mScanRequested) {
            this.mScanRequested = true;
            updateScanState();
        }
    }

    public void requestStopScan() {
        if (this.mScanRequested) {
            this.mScanRequested = false;
            updateScanState();
        }
    }

    public void requestConnect(String str) {
        Slog.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + ", address = " + str);
        resetReconnectVariable();
        if (DEBUG) {
            Slog.d(TAG, "mAvailableWifiDisplayPeers dump:");
        }
        for (WifiP2pDevice wifiP2pDevice : this.mAvailableWifiDisplayPeers) {
            if (DEBUG) {
                Slog.d(TAG, "\t" + describeWifiP2pDevice(wifiP2pDevice));
            }
            if (wifiP2pDevice.deviceAddress.equals(str)) {
                if (this.mIsConnected_OtherP2p) {
                    Slog.i(TAG, "OtherP2P is connected! Show dialog!");
                    advertiseDisplay(createWifiDisplay(wifiP2pDevice), null, 0, 0, 0);
                    showDialog(1);
                    return;
                }
                connect(wifiP2pDevice);
            }
        }
    }

    public void requestPause() {
        if (this.mRemoteDisplay != null) {
            this.mRemoteDisplay.pause();
        }
    }

    public void requestResume() {
        if (this.mRemoteDisplay != null) {
            this.mRemoteDisplay.resume();
        }
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
            if (!this.mWfdEnabled && !this.mWfdEnabling) {
                this.mWfdEnabling = true;
                updateWfdInfo(true);
                return;
            }
            return;
        }
        if (this.mWifiDisplayOnSetting && this.mAutoEnableWifi) {
            return;
        }
        updateWfdInfo(false);
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

    private void updateWfdInfo(boolean z) {
        Slog.i(TAG, "updateWfdInfo(), enable:" + z + ",mWfdEnabling:" + this.mWfdEnabling);
        resetWfdInfo();
        if (!z) {
            this.mWfdInfo.setWfdEnabled(false);
            this.mWifiP2pManager.setWFDInfo(this.mWifiP2pChannel, this.mWfdInfo, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    if (MtkWifiDisplayController.DEBUG) {
                        Slog.d(MtkWifiDisplayController.TAG, "Successfully set WFD info.");
                    }
                }

                @Override
                public void onFailure(int i) {
                    if (MtkWifiDisplayController.DEBUG) {
                        Slog.d(MtkWifiDisplayController.TAG, "Failed to set WFD info with reason " + i + ".");
                    }
                }
            });
            return;
        }
        this.mWfdInfo.setWfdEnabled(true);
        if (!SystemProperties.get("ro.vendor.mtk_wfd_sink_support").equals("1") || !this.mSinkEnabled) {
            this.mWfdInfo.setDeviceType(0);
        } else {
            this.mWfdInfo.setDeviceType(1);
        }
        Slog.i(TAG, "Set session available as true");
        this.mWfdInfo.setSessionAvailable(true);
        this.mWfdInfo.setControlPort(DEFAULT_CONTROL_PORT);
        this.mWfdInfo.setMaxThroughput(50);
        if (SystemProperties.get("ro.vendor.mtk_wfd_sink_support").equals("1") && this.mSinkEnabled && !SystemProperties.get("ro.vendor.mtk_wfd_sink_uibc_support").equals("1")) {
            this.mWfdInfo.setUibcSupported(false);
        } else {
            this.mWfdInfo.setUibcSupported(true);
        }
        if (SystemProperties.get("ro.vendor.mtk_wfd_hdcp_tx_support").equals("1") || SystemProperties.get("ro.vendor.mtk_dx_hdcp_support").equals("1") || SystemProperties.get("ro.vendor.mtk_wfd_hdcp_rx_support").equals("1")) {
            this.mWfdInfo.setContentProtected(true);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("HDCP Tx support? ");
        sb.append(SystemProperties.get("ro.vendor.mtk_wfd_hdcp_tx_support").equals("1") || SystemProperties.get("ro.vendor.mtk_dx_hdcp_support").equals("1"));
        sb.append(", our wfd info: ");
        sb.append(this.mWfdInfo);
        Slog.i(TAG, sb.toString());
        Slog.i(TAG, "HDCP Rx support? " + SystemProperties.get("ro.vendor.mtk_wfd_hdcp_rx_support").equals("1") + ", our wfd info: " + this.mWfdInfo);
        if (this.mWfdEnabling) {
            this.mWifiP2pManager.setWFDInfo(this.mWifiP2pChannel, this.mWfdInfo, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Slog.d(MtkWifiDisplayController.TAG, "Successfully set WFD info.");
                    if (MtkWifiDisplayController.this.mWfdEnabling) {
                        MtkWifiDisplayController.this.mWfdEnabling = false;
                        MtkWifiDisplayController.this.mWfdEnabled = true;
                        MtkWifiDisplayController.this.reportFeatureState();
                        if (SystemProperties.get("ro.vendor.mtk_wfd_support").equals("1") && MtkWifiDisplayController.this.mAutoEnableWifi) {
                            MtkWifiDisplayController.this.mAutoEnableWifi = false;
                            Slog.d(MtkWifiDisplayController.TAG, "scan after enable wifi automatically.");
                        }
                        MtkWifiDisplayController.this.updateScanState();
                    }
                }

                @Override
                public void onFailure(int i) {
                    Slog.d(MtkWifiDisplayController.TAG, "Failed to set WFD info with reason " + i + ".");
                    MtkWifiDisplayController.this.mWfdEnabling = false;
                }
            });
        } else {
            this.mWifiP2pManager.setWFDInfo(this.mWifiP2pChannel, this.mWfdInfo, null);
        }
    }

    private void reportFeatureState() {
        final int iComputeFeatureState = computeFeatureState();
        Slog.d(TAG, "reportFeatureState(), featureState = " + iComputeFeatureState);
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                Slog.d(MtkWifiDisplayController.TAG, "callback onFeatureStateChanged(): featureState = " + iComputeFeatureState);
                MtkWifiDisplayController.this.mListener.onFeatureStateChanged(iComputeFeatureState);
            }
        });
    }

    private int computeFeatureState() {
        if (!this.mWifiP2pEnabled) {
            if (SystemProperties.get("ro.vendor.mtk_wfd_support").equals("1")) {
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
        if (SystemProperties.get("ro.vendor.mtk_wfd_sink_support").equals("1") && this.mSinkEnabled) {
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
        if (this.mDiscoverPeersInProgress) {
            this.mHandler.removeCallbacks(this.mDiscoverPeers);
            if (this.mDesiredDevice == null || this.mDesiredDevice == this.mConnectedDevice) {
                Slog.i(TAG, "Stopping Wifi display scan.");
                this.mDiscoverPeersInProgress = false;
                stopPeerDiscovery();
                handleScanFinished();
            }
        }
    }

    private void tryDiscoverPeers() {
        Slog.d(TAG, "tryDiscoverPeers()");
        this.mWifiP2pManager.discoverPeers(this.mWifiP2pChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                if (MtkWifiDisplayController.DEBUG) {
                    Slog.d(MtkWifiDisplayController.TAG, "Discover peers succeeded.  Requesting peers now.");
                }
                if (MtkWifiDisplayController.this.mDiscoverPeersInProgress) {
                    MtkWifiDisplayController.this.requestPeers();
                }
            }

            @Override
            public void onFailure(int i) {
                if (MtkWifiDisplayController.DEBUG) {
                    Slog.d(MtkWifiDisplayController.TAG, "Discover peers failed with reason " + i + ".");
                }
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
                if (MtkWifiDisplayController.DEBUG) {
                    Slog.d(MtkWifiDisplayController.TAG, "Stop peer discovery succeeded.");
                }
            }

            @Override
            public void onFailure(int i) {
                if (MtkWifiDisplayController.DEBUG) {
                    Slog.d(MtkWifiDisplayController.TAG, "Stop peer discovery failed with reason " + i + ".");
                }
            }
        });
    }

    private void requestPeers() {
        this.mWifiP2pManager.requestPeers(this.mWifiP2pChannel, new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
                if (!SystemProperties.get("ro.vendor.mtk_wfd_sink_support").equals("1") || !MtkWifiDisplayController.this.mSinkEnabled) {
                    if (MtkWifiDisplayController.DEBUG) {
                        Slog.d(MtkWifiDisplayController.TAG, "Received list of peers. mDiscoverPeersInProgress=" + MtkWifiDisplayController.this.mDiscoverPeersInProgress);
                    }
                    MtkWifiDisplayController.this.mAvailableWifiDisplayPeers.clear();
                    for (WifiP2pDevice wifiP2pDevice : wifiP2pDeviceList.getDeviceList()) {
                        if (MtkWifiDisplayController.DEBUG) {
                            Slog.d(MtkWifiDisplayController.TAG, "  " + MtkWifiDisplayController.describeWifiP2pDevice(wifiP2pDevice));
                        }
                        if (MtkWifiDisplayController.this.mConnectedDevice == null || !MtkWifiDisplayController.this.mConnectedDevice.deviceAddress.equals(wifiP2pDevice.deviceAddress)) {
                            if (MtkWifiDisplayController.this.mConnectingDevice == null || !MtkWifiDisplayController.this.mConnectingDevice.deviceAddress.equals(wifiP2pDevice.deviceAddress)) {
                                if (MtkWifiDisplayController.this.mBlockMac == null || !wifiP2pDevice.deviceAddress.equals(MtkWifiDisplayController.this.mBlockMac)) {
                                    if (MtkWifiDisplayController.isWifiDisplay(wifiP2pDevice)) {
                                        MtkWifiDisplayController.this.mAvailableWifiDisplayPeers.add(wifiP2pDevice);
                                    }
                                } else {
                                    Slog.i(MtkWifiDisplayController.TAG, "Block scan result on block mac:" + MtkWifiDisplayController.this.mBlockMac);
                                }
                            } else {
                                MtkWifiDisplayController.this.mAvailableWifiDisplayPeers.add(wifiP2pDevice);
                            }
                        } else {
                            MtkWifiDisplayController.this.mAvailableWifiDisplayPeers.add(wifiP2pDevice);
                        }
                    }
                    MtkWifiDisplayController.this.handleScanResults();
                }
            }
        });
    }

    private void handleScanStarted() {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                Slog.d(MtkWifiDisplayController.TAG, "callback onScanStarted()");
                MtkWifiDisplayController.this.mListener.onScanStarted();
            }
        });
    }

    private void handleScanResults() {
        final int size = this.mAvailableWifiDisplayPeers.size();
        final WifiDisplay[] wifiDisplayArr = (WifiDisplay[]) WifiDisplay.CREATOR.newArray(size);
        for (int i = 0; i < size; i++) {
            WifiP2pDevice wifiP2pDevice = this.mAvailableWifiDisplayPeers.get(i);
            wifiDisplayArr[i] = createWifiDisplay(wifiP2pDevice);
            updateDesiredDevice(wifiP2pDevice);
        }
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                Slog.d(MtkWifiDisplayController.TAG, "callback onScanResults(), count = " + size);
                if (MtkWifiDisplayController.DEBUG) {
                    for (int i2 = 0; i2 < size; i2++) {
                        Slog.d(MtkWifiDisplayController.TAG, "\t" + wifiDisplayArr[i2].getDeviceName() + ": " + wifiDisplayArr[i2].getDeviceAddress());
                    }
                }
                MtkWifiDisplayController.this.mListener.onScanResults(wifiDisplayArr);
            }
        });
    }

    private void handleScanFinished() {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                MtkWifiDisplayController.this.mListener.onScanFinished();
            }
        });
    }

    private void updateDesiredDevice(WifiP2pDevice wifiP2pDevice) {
        String str = wifiP2pDevice.deviceAddress;
        if (this.mDesiredDevice != null && this.mDesiredDevice.deviceAddress.equals(str)) {
            if (DEBUG) {
                Slog.d(TAG, "updateDesiredDevice: new information " + describeWifiP2pDevice(wifiP2pDevice));
            }
            this.mDesiredDevice.update(wifiP2pDevice);
            if (this.mAdvertisedDisplay != null && this.mAdvertisedDisplay.getDeviceAddress().equals(str)) {
                readvertiseDisplay(createWifiDisplay(this.mDesiredDevice));
            }
        }
    }

    private void connect(WifiP2pDevice wifiP2pDevice) {
        Slog.i(TAG, "connect: device name = " + wifiP2pDevice.deviceName);
        if (this.mDesiredDevice != null && !this.mDesiredDevice.deviceAddress.equals(wifiP2pDevice.deviceAddress)) {
            if (DEBUG) {
                Slog.d(TAG, "connect: nothing to do, already connecting to " + describeWifiP2pDevice(this.mDesiredDevice));
                return;
            }
            return;
        }
        if (this.mDesiredDevice != null && this.mDesiredDevice.deviceAddress.equals(wifiP2pDevice.deviceAddress)) {
            if (DEBUG) {
                Slog.d(TAG, "connect: connecting to the same dongle already " + describeWifiP2pDevice(this.mDesiredDevice));
                return;
            }
            return;
        }
        if (this.mConnectedDevice != null && !this.mConnectedDevice.deviceAddress.equals(wifiP2pDevice.deviceAddress) && this.mDesiredDevice == null) {
            if (DEBUG) {
                Slog.d(TAG, "connect: nothing to do, already connected to " + describeWifiP2pDevice(wifiP2pDevice) + " and not part way through connecting to a different device.");
                return;
            }
            return;
        }
        if (!this.mWfdEnabled) {
            Slog.i(TAG, "Ignoring request to connect to Wifi display because the  feature is currently disabled: " + wifiP2pDevice.deviceName);
            return;
        }
        this.mDesiredDevice = wifiP2pDevice;
        this.mConnectionRetriesLeft = 0;
        updateConnection();
    }

    private void disconnect() {
        Slog.i(TAG, "disconnect, mRemoteDisplayInterface = " + this.mRemoteDisplayInterface);
        if (SystemProperties.get("ro.vendor.mtk_wfd_sink_support").equals("1") && this.mSinkEnabled) {
            disconnectWfdSink();
            return;
        }
        this.mDesiredDevice = null;
        updateWfdStatFile(0);
        if (this.mConnectedDevice != null) {
            this.mReConnectDevice = this.mConnectedDevice;
        }
        if (this.mConnectingDevice != null || this.mConnectedDevice != null) {
            removeSpecificPersistentGroup();
        }
        updateConnection();
    }

    private void retryConnection() {
        this.mDesiredDevice = new WifiP2pDevice(this.mDesiredDevice);
        updateConnection();
    }

    private void updateConnection() {
        String str;
        updateScanState();
        if ((this.mRemoteDisplay != null && this.mConnectedDevice != this.mDesiredDevice) || true == this.mIsConnecting_P2p_Rtsp) {
            String str2 = this.mRemoteDisplayInterface != null ? this.mRemoteDisplayInterface : "localhost";
            if (this.mConnectedDevice != null) {
                str = this.mConnectedDevice.deviceName;
            } else {
                str = this.mConnectingDevice != null ? this.mConnectingDevice.deviceName : "N/A";
            }
            Slog.i(TAG, "Stopped listening for RTSP connection on " + str2 + " from Wifi display : " + str);
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
            final WifiP2pDevice wifiP2pDevice = this.mDisconnectingDevice;
            this.mWifiP2pManager.removeGroup(this.mWifiP2pChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Slog.i(MtkWifiDisplayController.TAG, "Disconnected from Wifi display: " + wifiP2pDevice.deviceName);
                    next();
                }

                @Override
                public void onFailure(int i) {
                    Slog.i(MtkWifiDisplayController.TAG, "Failed to disconnect from Wifi display: " + wifiP2pDevice.deviceName + ", reason=" + i);
                    next();
                }

                private void next() {
                    if (MtkWifiDisplayController.this.mDisconnectingDevice == wifiP2pDevice) {
                        MtkWifiDisplayController.this.mDisconnectingDevice = null;
                        if (MtkWifiDisplayController.this.mRemoteDisplay != null) {
                            MtkWifiDisplayController.this.mIsConnecting_P2p_Rtsp = true;
                        }
                        MtkWifiDisplayController.this.updateConnection();
                    }
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
            final WifiP2pDevice wifiP2pDevice2 = this.mCancelingDevice;
            this.mWifiP2pManager.cancelConnect(this.mWifiP2pChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Slog.i(MtkWifiDisplayController.TAG, "Canceled connection to Wifi display: " + wifiP2pDevice2.deviceName);
                    next();
                }

                @Override
                public void onFailure(int i) {
                    Slog.i(MtkWifiDisplayController.TAG, "Failed to cancel connection to Wifi display: " + wifiP2pDevice2.deviceName + ", reason=" + i + ". Do removeGroup()");
                    MtkWifiDisplayController.this.mWifiP2pManager.removeGroup(MtkWifiDisplayController.this.mWifiP2pChannel, null);
                    next();
                }

                private void next() {
                    if (MtkWifiDisplayController.this.mCancelingDevice == wifiP2pDevice2) {
                        MtkWifiDisplayController.this.mCancelingDevice = null;
                        if (MtkWifiDisplayController.this.mRemoteDisplay != null) {
                            MtkWifiDisplayController.this.mIsConnecting_P2p_Rtsp = true;
                        }
                        MtkWifiDisplayController.this.updateConnection();
                    }
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
        if (this.mConnectedDevice == null && this.mConnectingDevice == null) {
            Slog.i(TAG, "Connecting to Wifi display: " + this.mDesiredDevice.deviceName);
            this.mConnectingDevice = this.mDesiredDevice;
            WifiP2pConfig wifiP2pConfig = new WifiP2pConfig();
            WpsInfo wpsInfo = new WpsInfo();
            if (this.mWifiDisplayWpsConfig != 4) {
                wpsInfo.setup = this.mWifiDisplayWpsConfig;
            } else if (this.mConnectingDevice.wpsPbcSupported()) {
                wpsInfo.setup = 0;
            } else if (this.mConnectingDevice.wpsDisplaySupported()) {
                wpsInfo.setup = 2;
            } else if (this.mConnectingDevice.wpsKeypadSupported()) {
                wpsInfo.setup = 1;
            } else {
                wpsInfo.setup = 0;
            }
            wifiP2pConfig.wps = wpsInfo;
            wifiP2pConfig.deviceAddress = this.mConnectingDevice.deviceAddress;
            wifiP2pConfig.groupOwnerIntent = Integer.valueOf(SystemProperties.get("wfd.source.go_intent", String.valueOf(14))).intValue();
            Slog.i(TAG, "Source go_intent:" + wifiP2pConfig.groupOwnerIntent);
            if (this.mConnectingDevice.deviceName.contains("BRAVIA")) {
                wifiP2pConfig.netId = -1;
            }
            advertiseDisplay(createWifiDisplay(this.mConnectingDevice), null, 0, 0, 0);
            updateWfdInfo(true);
            enterCCState(ChannelConflictState.STATE_IDLE);
            this.mWifiP2pManager.setMiracastMode(1);
            stopWifiScan(true);
            final WifiP2pDevice wifiP2pDevice3 = this.mDesiredDevice;
            this.mWifiP2pManager.connect(this.mWifiP2pChannel, wifiP2pConfig, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Slog.i(MtkWifiDisplayController.TAG, "Initiated connection to Wifi display: " + wifiP2pDevice3.deviceName);
                    MtkWifiDisplayController.this.mHandler.postDelayed(MtkWifiDisplayController.this.mConnectionTimeout, 60000L);
                }

                @Override
                public void onFailure(int i) {
                    if (MtkWifiDisplayController.this.mConnectingDevice == wifiP2pDevice3) {
                        Slog.i(MtkWifiDisplayController.TAG, "Failed to initiate connection to Wifi display: " + wifiP2pDevice3.deviceName + ", reason=" + i);
                        MtkWifiDisplayController.this.mConnectingDevice = null;
                        MtkWifiDisplayController.this.handleConnectionFailure(false);
                    }
                }
            });
            this.mRTSPConnecting = true;
            final WifiP2pDevice wifiP2pDevice4 = this.mConnectingDevice;
            String str3 = "127.0.0.1:" + getPortNumber(this.mConnectingDevice);
            this.mRemoteDisplayInterface = str3;
            Slog.i(TAG, "Listening for RTSP connection on " + str3 + " from Wifi display: " + this.mConnectingDevice.deviceName + " , Speed-Up rtsp setup, DRM Content isPlaying = " + this.mDRMContent_Mediaplayer);
            this.mRemoteDisplay = RemoteDisplay.listen(str3, new RemoteDisplay.Listener() {
                public void onDisplayConnected(Surface surface, int i, int i2, int i3, int i4) {
                    if (MtkWifiDisplayController.this.mConnectingDevice != null) {
                        MtkWifiDisplayController.this.mConnectedDevice = MtkWifiDisplayController.this.mConnectingDevice;
                    }
                    if ((MtkWifiDisplayController.this.mConnectedDevice != wifiP2pDevice4 || MtkWifiDisplayController.this.mRemoteDisplayConnected) && MtkWifiDisplayController.DEBUG) {
                        Slog.e(MtkWifiDisplayController.TAG, "!!RTSP connected condition GOT Trobule:\nmConnectedDevice: " + MtkWifiDisplayController.this.mConnectedDevice + "\noldDevice: " + wifiP2pDevice4 + "\nmRemoteDisplayConnected: " + MtkWifiDisplayController.this.mRemoteDisplayConnected);
                    }
                    if (MtkWifiDisplayController.this.mConnectedDevice != null && wifiP2pDevice4 != null && MtkWifiDisplayController.this.mConnectedDevice.deviceAddress.equals(wifiP2pDevice4.deviceAddress) && !MtkWifiDisplayController.this.mRemoteDisplayConnected) {
                        Slog.i(MtkWifiDisplayController.TAG, "Opened RTSP connection with Wifi display: " + MtkWifiDisplayController.this.mConnectedDevice.deviceName);
                        MtkWifiDisplayController.this.mRemoteDisplayConnected = true;
                        MtkWifiDisplayController.this.mHandler.removeCallbacks(MtkWifiDisplayController.this.mRtspTimeout);
                        if (MtkWifiDisplayController.this.mWifiDisplayCertMode) {
                            MtkWifiDisplayController.this.mListener.onDisplaySessionInfo(MtkWifiDisplayController.this.getSessionInfo(MtkWifiDisplayController.this.mConnectedDeviceGroupInfo, i4));
                        }
                        MtkWifiDisplayController.this.updateWfdStatFile(2);
                        MtkWifiDisplayController.this.advertiseDisplay(MtkWifiDisplayController.createWifiDisplay(MtkWifiDisplayController.this.mConnectedDevice), surface, i, i2, i3);
                    }
                    MtkWifiDisplayController.this.mRTSPConnecting = false;
                }

                public void onDisplayDisconnected() {
                    if (MtkWifiDisplayController.this.mConnectedDevice == wifiP2pDevice4) {
                        Slog.i(MtkWifiDisplayController.TAG, "Closed RTSP connection with Wifi display: " + MtkWifiDisplayController.this.mConnectedDevice.deviceName);
                        MtkWifiDisplayController.this.mHandler.removeCallbacks(MtkWifiDisplayController.this.mRtspTimeout);
                        MtkWifiDisplayController.this.disconnect();
                    }
                    MtkWifiDisplayController.this.mRTSPConnecting = false;
                }

                public void onDisplayError(int i) {
                    if (MtkWifiDisplayController.this.mConnectedDevice == wifiP2pDevice4) {
                        Slog.i(MtkWifiDisplayController.TAG, "Lost RTSP connection with Wifi display due to error " + i + ": " + MtkWifiDisplayController.this.mConnectedDevice.deviceName);
                        MtkWifiDisplayController.this.mHandler.removeCallbacks(MtkWifiDisplayController.this.mRtspTimeout);
                        MtkWifiDisplayController.this.handleConnectionFailure(false);
                    }
                    MtkWifiDisplayController.this.mRTSPConnecting = false;
                }

                public void onDisplayKeyEvent(int i, int i2) {
                    Slog.d(MtkWifiDisplayController.TAG, "onDisplayKeyEvent:uniCode=" + i);
                    if (MtkWifiDisplayController.this.mInputMethodManager != null) {
                        try {
                            if (MtkWifiDisplayController.this.mWakeLock != null) {
                                MtkWifiDisplayController.this.mWakeLock.acquire();
                            }
                            MtkWifiDisplayController.this.mInputMethodManager.sendCharacterToCurClient(i);
                            if (MtkWifiDisplayController.this.mWakeLock != null) {
                                MtkWifiDisplayController.this.mWakeLock.release();
                            }
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }

                public void onDisplayGenericMsgEvent(int i) {
                    Slog.d(MtkWifiDisplayController.TAG, "onDisplayGenericMsgEvent: " + i);
                }
            }, this.mHandler, this.mContext.getOpPackageName());
            this.mHandler.postDelayed(this.mRtspTimeout, (this.mWifiDisplayCertMode ? RTSP_TIMEOUT_SECONDS_CERT_MODE : 75) * 1000);
            return;
        }
        if (this.mConnectedDevice != null && this.mRemoteDisplay == null && getInterfaceAddress(this.mConnectedDeviceGroupInfo) == null) {
            Slog.i(TAG, "Failed to get local interface address for communicating with Wifi display: " + this.mConnectedDevice.deviceName);
            handleConnectionFailure(false);
        }
    }

    private WifiDisplaySessionInfo getSessionInfo(WifiP2pGroup wifiP2pGroup, int i) {
        if (wifiP2pGroup == null) {
            return null;
        }
        Inet4Address interfaceAddress = getInterfaceAddress(wifiP2pGroup);
        WifiDisplaySessionInfo wifiDisplaySessionInfo = new WifiDisplaySessionInfo(!wifiP2pGroup.getOwner().deviceAddress.equals(this.mThisDevice.deviceAddress), i, wifiP2pGroup.getOwner().deviceAddress + " " + wifiP2pGroup.getNetworkName(), wifiP2pGroup.getPassphrase(), interfaceAddress != null ? interfaceAddress.getHostAddress() : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        if (DEBUG) {
            Slog.d(TAG, wifiDisplaySessionInfo.toString());
        }
        return wifiDisplaySessionInfo;
    }

    private void handleStateChanged(boolean z) {
        this.mWifiP2pEnabled = z;
        updateWfdEnableState();
        if (!z) {
            dismissDialog();
        }
    }

    private void handlePeersChanged() {
        requestPeers();
    }

    private void handleConnectionChanged(NetworkInfo networkInfo, int i) {
        Slog.i(TAG, "handleConnectionChanged(), mWfdEnabled:" + this.mWfdEnabled);
        this.mNetworkInfo = networkInfo;
        if (this.mWfdEnabled && networkInfo.isConnected()) {
            if (this.mDesiredDevice != null || this.mWifiDisplayCertMode) {
                this.mWifiP2pManager.requestGroupInfo(this.mWifiP2pChannel, new WifiP2pManager.GroupInfoListener() {
                    @Override
                    public void onGroupInfoAvailable(WifiP2pGroup wifiP2pGroup) {
                        if (wifiP2pGroup != null) {
                            if (MtkWifiDisplayController.DEBUG) {
                                Slog.d(MtkWifiDisplayController.TAG, "Received group info: " + MtkWifiDisplayController.describeWifiP2pGroup(wifiP2pGroup));
                            }
                            if (MtkWifiDisplayController.this.mConnectingDevice == null || wifiP2pGroup.contains(MtkWifiDisplayController.this.mConnectingDevice)) {
                                if (MtkWifiDisplayController.this.mDesiredDevice == null || wifiP2pGroup.contains(MtkWifiDisplayController.this.mDesiredDevice)) {
                                    if (MtkWifiDisplayController.this.mWifiDisplayCertMode) {
                                        boolean zEquals = wifiP2pGroup.getOwner().deviceAddress.equals(MtkWifiDisplayController.this.mThisDevice.deviceAddress);
                                        if (!zEquals || !wifiP2pGroup.getClientList().isEmpty()) {
                                            if (MtkWifiDisplayController.this.mConnectingDevice == null && MtkWifiDisplayController.this.mDesiredDevice == null) {
                                                MtkWifiDisplayController.this.mConnectingDevice = MtkWifiDisplayController.this.mDesiredDevice = zEquals ? wifiP2pGroup.getClientList().iterator().next() : wifiP2pGroup.getOwner();
                                            }
                                        } else {
                                            MtkWifiDisplayController.this.mConnectingDevice = MtkWifiDisplayController.this.mDesiredDevice = null;
                                            MtkWifiDisplayController.this.mConnectedDeviceGroupInfo = wifiP2pGroup;
                                            MtkWifiDisplayController.this.updateConnection();
                                        }
                                    }
                                    if (MtkWifiDisplayController.this.mConnectingDevice != null && MtkWifiDisplayController.this.mConnectingDevice == MtkWifiDisplayController.this.mDesiredDevice) {
                                        Slog.i(MtkWifiDisplayController.TAG, "Connected to Wifi display: " + MtkWifiDisplayController.this.mConnectingDevice.deviceName);
                                        MtkWifiDisplayController.this.mHandler.removeCallbacks(MtkWifiDisplayController.this.mConnectionTimeout);
                                        MtkWifiDisplayController.this.mConnectedDeviceGroupInfo = wifiP2pGroup;
                                        MtkWifiDisplayController.this.mConnectedDevice = MtkWifiDisplayController.this.mConnectingDevice;
                                        MtkWifiDisplayController.this.mConnectingDevice = null;
                                        MtkWifiDisplayController.this.updateWfdStatFile(1);
                                        MtkWifiDisplayController.this.updateConnection();
                                        MtkWifiDisplayController.this.handleChannelConflictProcedure(ChannelConflictEvt.EVT_WFD_P2P_CONNECTED);
                                        return;
                                    }
                                    return;
                                }
                                Slog.i(MtkWifiDisplayController.TAG, "Aborting connection to Wifi display because the current P2P group does not contain the device we desired to find: " + MtkWifiDisplayController.this.mDesiredDevice.deviceName + ", group info was: " + MtkWifiDisplayController.describeWifiP2pGroup(wifiP2pGroup));
                                MtkWifiDisplayController.this.disconnect();
                                return;
                            }
                            Slog.i(MtkWifiDisplayController.TAG, "Aborting connection to Wifi display because the current P2P group does not contain the device we expected to find: " + MtkWifiDisplayController.this.mConnectingDevice.deviceName + ", group info was: " + MtkWifiDisplayController.describeWifiP2pGroup(wifiP2pGroup));
                            MtkWifiDisplayController.this.handleConnectionFailure(false);
                            return;
                        }
                        Slog.i(MtkWifiDisplayController.TAG, "Error: group is null !!!");
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
                if (true == this.mLastTimeConnected && this.mReconnectForResolutionChange) {
                    Slog.i(TAG, "requestStartScan() for resolution change.");
                    this.mReScanning = true;
                    updateScanState();
                    this.mReConnection_Timeout_Remain_Seconds = 60;
                    this.mHandler.postDelayed(this.mReConnect, 1000L);
                }
            }
            this.mReconnectForResolutionChange = false;
            if (7 == i && this.mReConnectDevice != null) {
                Slog.i(TAG, "reconnect procedure start, ReConnectDevice = " + this.mReConnectDevice);
                dialogReconnect();
            }
            handleChannelConflictProcedure(ChannelConflictEvt.EVT_WFD_P2P_DISCONNECTED);
        }
        if (this.mDesiredDevice == null) {
            this.mIsConnected_OtherP2p = networkInfo.isConnected();
            if (true == this.mIsConnected_OtherP2p) {
                Slog.w(TAG, "Wifi P2p connection is connected but it does not wifidisplay trigger");
                resetReconnectVariable();
            }
        }
    }

    private void handleConnectionFailure(boolean z) {
        Slog.i(TAG, "Wifi display connection failed!");
        if (this.mDesiredDevice != null) {
            if (this.mConnectionRetriesLeft > 0) {
                final WifiP2pDevice wifiP2pDevice = this.mDesiredDevice;
                this.mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (MtkWifiDisplayController.this.mDesiredDevice == wifiP2pDevice && MtkWifiDisplayController.this.mConnectionRetriesLeft > 0) {
                            MtkWifiDisplayController.access$4620(MtkWifiDisplayController.this, 1);
                            Slog.i(MtkWifiDisplayController.TAG, "Retrying Wifi display connection.  Retries left: " + MtkWifiDisplayController.this.mConnectionRetriesLeft);
                            MtkWifiDisplayController.this.retryConnection();
                        }
                    }
                }, z ? 0L : 500L);
            } else {
                disconnect();
            }
        }
    }

    private void advertiseDisplay(final WifiDisplay wifiDisplay, final Surface surface, final int i, final int i2, final int i3) {
        if (DEBUG) {
            Slog.d(TAG, "advertiseDisplay(): ----->\n\tdisplay: " + wifiDisplay + "\n\tsurface: " + surface + "\n\twidth: " + i + "\n\theight: " + i2 + "\n\tflags: " + i3);
        }
        if (!Objects.equals(this.mAdvertisedDisplay, wifiDisplay) || this.mAdvertisedDisplaySurface != surface || this.mAdvertisedDisplayWidth != i || this.mAdvertisedDisplayHeight != i2 || this.mAdvertisedDisplayFlags != i3) {
            final WifiDisplay wifiDisplay2 = this.mAdvertisedDisplay;
            final Surface surface2 = this.mAdvertisedDisplaySurface;
            this.mAdvertisedDisplay = wifiDisplay;
            this.mAdvertisedDisplaySurface = surface;
            this.mAdvertisedDisplayWidth = i;
            this.mAdvertisedDisplayHeight = i2;
            this.mAdvertisedDisplayFlags = i3;
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (MtkWifiDisplayController.DEBUG) {
                        Slog.d(MtkWifiDisplayController.TAG, "oldSurface = " + surface2 + ", surface = " + surface + ", oldDisplay = " + wifiDisplay2 + ", display = " + wifiDisplay);
                    }
                    if (surface2 != null && surface != surface2) {
                        Slog.d(MtkWifiDisplayController.TAG, "callback onDisplayDisconnected()");
                        MtkWifiDisplayController.this.mListener.onDisplayDisconnected();
                        MtkWifiDisplayController.this.actionAtDisconnected(wifiDisplay2);
                        MtkWifiDisplayController.this.mPowerHalManager.setWFD(false);
                    } else if (wifiDisplay2 != null && !wifiDisplay2.hasSameAddress(wifiDisplay)) {
                        Slog.d(MtkWifiDisplayController.TAG, "callback onDisplayConnectionFailed()");
                        MtkWifiDisplayController.this.mListener.onDisplayConnectionFailed();
                        MtkWifiDisplayController.this.actionAtConnectionFailed();
                        MtkWifiDisplayController.this.mPowerHalManager.setWFD(false);
                    }
                    if (wifiDisplay != null) {
                        if (!wifiDisplay.hasSameAddress(wifiDisplay2)) {
                            Slog.d(MtkWifiDisplayController.TAG, "callback onDisplayConnecting(): display = " + wifiDisplay);
                            MtkWifiDisplayController.this.mListener.onDisplayConnecting(wifiDisplay);
                            MtkWifiDisplayController.this.actionAtConnecting();
                        } else if (!wifiDisplay.equals(wifiDisplay2)) {
                            MtkWifiDisplayController.this.mListener.onDisplayChanged(wifiDisplay);
                        }
                        if (surface != null && surface != surface2) {
                            MtkWifiDisplayController.this.updateIfHdcp(i3);
                            Slog.d(MtkWifiDisplayController.TAG, "callback onDisplayConnected(): display = " + wifiDisplay + ", surface = " + surface + ", width = " + i + ", height = " + i2 + ", flags = " + i3);
                            MtkWifiDisplayController.this.mListener.onDisplayConnected(wifiDisplay, surface, i, i2, i3);
                            MtkWifiDisplayController.this.actionAtConnected(wifiDisplay, i3, i < i2);
                            MtkWifiDisplayController.this.mPowerHalManager.setWFD(true);
                        }
                    }
                }
            });
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, "advertiseDisplay() : no need update!");
        }
    }

    private void unadvertiseDisplay() {
        advertiseDisplay(null, null, 0, 0, 0);
    }

    private void readvertiseDisplay(WifiDisplay wifiDisplay) {
        advertiseDisplay(wifiDisplay, this.mAdvertisedDisplaySurface, this.mAdvertisedDisplayWidth, this.mAdvertisedDisplayHeight, this.mAdvertisedDisplayFlags);
    }

    private static Inet4Address getInterfaceAddress(WifiP2pGroup wifiP2pGroup) {
        try {
            Enumeration<InetAddress> inetAddresses = NetworkInterface.getByName(wifiP2pGroup.getInterface()).getInetAddresses();
            while (inetAddresses.hasMoreElements()) {
                InetAddress inetAddressNextElement = inetAddresses.nextElement();
                if (inetAddressNextElement instanceof Inet4Address) {
                    return (Inet4Address) inetAddressNextElement;
                }
            }
            Slog.w(TAG, "Could not obtain address of network interface " + wifiP2pGroup.getInterface() + " because it had no IPv4 addresses.");
            return null;
        } catch (SocketException e) {
            Slog.w(TAG, "Could not obtain address of network interface " + wifiP2pGroup.getInterface(), e);
            return null;
        }
    }

    private static int getPortNumber(WifiP2pDevice wifiP2pDevice) {
        if (wifiP2pDevice.deviceName.startsWith("DIRECT-") && wifiP2pDevice.deviceName.endsWith("Broadcom")) {
            return 8554;
        }
        return DEFAULT_CONTROL_PORT;
    }

    private static boolean isWifiDisplay(WifiP2pDevice wifiP2pDevice) {
        return wifiP2pDevice.wfdInfo != null && wifiP2pDevice.wfdInfo.isWfdEnabled() && wifiP2pDevice.wfdInfo.isSessionAvailable() && isPrimarySinkDeviceType(wifiP2pDevice.wfdInfo.getDeviceType());
    }

    private static boolean isPrimarySinkDeviceType(int i) {
        return i == 1 || i == 3;
    }

    private static String describeWifiP2pDevice(WifiP2pDevice wifiP2pDevice) {
        return wifiP2pDevice != null ? wifiP2pDevice.toString().replace('\n', ',') : "null";
    }

    private static String describeWifiP2pGroup(WifiP2pGroup wifiP2pGroup) {
        return wifiP2pGroup != null ? wifiP2pGroup.toString().replace('\n', ',') : "null";
    }

    private static WifiDisplay createWifiDisplay(WifiP2pDevice wifiP2pDevice) {
        return new WifiDisplay(wifiP2pDevice.deviceAddress, wifiP2pDevice.deviceName, (String) null, true, wifiP2pDevice.wfdInfo.isSessionAvailable(), false);
    }

    private void sendKeyEvent(int i, int i2) {
        long jUptimeMillis = SystemClock.uptimeMillis();
        if (i2 == 1) {
            injectKeyEvent(new KeyEvent(jUptimeMillis, jUptimeMillis, 0, translateAsciiToKeyCode(i), 0, 0, -1, 0, 0, UsbTerminalTypes.TERMINAL_USB_STREAMING));
        } else {
            injectKeyEvent(new KeyEvent(jUptimeMillis, jUptimeMillis, 1, translateAsciiToKeyCode(i), 0, 0, -1, 0, 0, UsbTerminalTypes.TERMINAL_USB_STREAMING));
        }
    }

    private void sendTap(float f, float f2) {
        long jUptimeMillis = SystemClock.uptimeMillis();
        injectPointerEvent(MotionEvent.obtain(jUptimeMillis, jUptimeMillis, 0, f, f2, 0));
        injectPointerEvent(MotionEvent.obtain(jUptimeMillis, jUptimeMillis, 1, f, f2, 0));
    }

    private void injectKeyEvent(KeyEvent keyEvent) {
        Slog.d(TAG, "InjectKeyEvent: " + keyEvent);
        InputManager.getInstance().injectInputEvent(keyEvent, 2);
    }

    private void injectPointerEvent(MotionEvent motionEvent) {
        motionEvent.setSource(UsbACInterface.FORMAT_II_AC3);
        Slog.d("Input", "InjectPointerEvent: " + motionEvent);
        InputManager.getInstance().injectInputEvent(motionEvent, 2);
    }

    private int translateSpecialCode(int i) {
        if (i == 8) {
            return 67;
        }
        if (i == 16) {
            return 59;
        }
        if (i == 20) {
            return HdmiCecKeycode.CEC_KEYCODE_F3_GREEN;
        }
        if (i != 27) {
            switch (i) {
                case 12:
                case 13:
                    return 66;
                default:
                    switch (i) {
                        case 32:
                            return 62;
                        case 33:
                            return 93;
                        case 34:
                            return 92;
                        default:
                            switch (i) {
                                case 37:
                                    return 19;
                                case 38:
                                    return 20;
                                case 39:
                                    return 22;
                                case 40:
                                    return 21;
                                default:
                                    switch (i) {
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
                                        default:
                                            switch (i) {
                                                case 219:
                                                    return 71;
                                                case 220:
                                                    return 73;
                                                case NetworkManagementService.NetdResponseCode.TetheringStatsResult:
                                                    return 72;
                                                case NetworkManagementService.NetdResponseCode.DnsProxyQueryResult:
                                                    return 75;
                                                default:
                                                    return 0;
                                            }
                                    }
                            }
                    }
            }
        }
        return NetworkManagementService.NetdResponseCode.TetherInterfaceListResult;
    }

    private int translateAsciiToKeyCode(int i) {
        if (i >= 48 && i <= 57) {
            return i - 41;
        }
        if (i >= 65 && i <= 90) {
            return i - 36;
        }
        int iTranslateSpecialCode = translateSpecialCode(i);
        if (iTranslateSpecialCode > 0) {
            Slog.d(TAG, "special code: " + i + ":" + iTranslateSpecialCode);
            return iTranslateSpecialCode;
        }
        Slog.d(TAG, "translateAsciiToKeyCode: ascii is not supported" + i);
        return 0;
    }

    private void getWifiLock() {
        if (this.mWifiManager == null) {
            this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        }
        if (this.mWifiLock == null && this.mWifiManager != null) {
            this.mWifiLock = this.mWifiManager.createWifiLock(1, "WFD_WifiLock");
        }
    }

    private void updateIfHdcp(int i) {
        if ((i & 1) != 0) {
            SystemProperties.set("vendor.media.wfd.hdcp", "1");
        } else {
            SystemProperties.set("vendor.media.wfd.hdcp", "0");
        }
    }

    private void stopWifiScan(boolean z) {
        if (this.mStopWifiScan != z) {
            Slog.i(TAG, "stopWifiScan()," + z);
            try {
                Method declaredMethod = Class.forName("com.android.server.wifi.WifiInjector", false, getClass().getClassLoader()).getDeclaredMethod("getInstance", new Class[0]);
                declaredMethod.setAccessible(true);
                Object objInvoke = declaredMethod.invoke(null, new Object[0]);
                Method declaredMethod2 = objInvoke.getClass().getDeclaredMethod("getWifiStateMachine", new Class[0]);
                declaredMethod2.setAccessible(true);
                Object objInvoke2 = declaredMethod2.invoke(objInvoke, new Object[0]);
                Field declaredField = objInvoke2.getClass().getDeclaredField("mWifiConnectivityManager");
                declaredField.setAccessible(true);
                Object obj = declaredField.get(objInvoke2);
                Method declaredMethod3 = obj.getClass().getDeclaredMethod("enable", Boolean.TYPE);
                declaredMethod3.setAccessible(true);
                declaredMethod3.invoke(obj, Boolean.valueOf(!z));
            } catch (ReflectiveOperationException e) {
                e.printStackTrace();
            }
            this.mStopWifiScan = z;
        }
    }

    private void actionAtConnected(WifiDisplay wifiDisplay, int i, boolean z) {
        Slog.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName());
        this.mIsWFDConnected = true;
        Intent intent = new Intent(WFD_CONNECTION);
        intent.addFlags(67108864);
        intent.putExtra("connected", 1);
        if (wifiDisplay != null) {
            intent.putExtra("device_address", wifiDisplay.getDeviceAddress());
            intent.putExtra("device_name", wifiDisplay.getDeviceName());
            intent.putExtra("device_alias", wifiDisplay.getDeviceAlias());
        } else {
            Slog.e(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + ", null display");
            intent.putExtra("device_address", "00:00:00:00:00:00");
            intent.putExtra("device_name", "wifidisplay dongle");
            intent.putExtra("device_alias", "wifidisplay dongle");
        }
        boolean z2 = (i & 1) != 0;
        if (z2) {
            intent.putExtra("secure", 1);
        } else {
            intent.putExtra("secure", 0);
        }
        Slog.i(TAG, "secure:" + z2);
        int wfdParam = this.mRemoteDisplay.getWfdParam(8);
        if ((wfdParam & 1) != 0 || (wfdParam & 2) != 0) {
            intent.putExtra("uibc_touch_mouse", 1);
        } else {
            intent.putExtra("uibc_touch_mouse", 0);
        }
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        if (true == this.mReConnecting) {
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
            resetSignalParam();
        }
        if (SystemProperties.get("ro.vendor.mtk_wfd_support").equals("1")) {
            if (SystemProperties.getInt("af.policy.r_submix_prio_adjust", 0) == 0) {
                checkA2dpStatus();
            }
            updateChosenCapability(wfdParam, z);
            this.mInputMethodManager = IInputMethodManager.Stub.asInterface(ServiceManager.getService("input_method"));
            if (this.mLatencyProfiling == 3) {
                this.mHandler.postDelayed(this.mDelayProfiling, 2000L);
            }
        }
        notifyClearMotion(true);
    }

    private void actionAtDisconnected(WifiDisplay wifiDisplay) {
        Slog.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName());
        if (this.mIsWFDConnected && wifiDisplay.getDeviceName().contains("Push2TV")) {
            this.mBlockMac = wifiDisplay.getDeviceAddress();
            Slog.i(TAG, "Add block mac:" + this.mBlockMac);
            this.mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Slog.i(MtkWifiDisplayController.TAG, "Remove block mac:" + MtkWifiDisplayController.this.mBlockMac);
                    MtkWifiDisplayController.this.mBlockMac = null;
                }
            }, 15000L);
        }
        this.mIsWFDConnected = false;
        Intent intent = new Intent(WFD_CONNECTION);
        intent.addFlags(67108864);
        intent.putExtra("connected", 0);
        if (wifiDisplay != null) {
            intent.putExtra("device_address", wifiDisplay.getDeviceAddress());
            intent.putExtra("device_name", wifiDisplay.getDeviceName());
            intent.putExtra("device_alias", wifiDisplay.getDeviceAlias());
        } else {
            Slog.e(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + ", null display");
            intent.putExtra("device_address", "00:00:00:00:00:00");
            intent.putExtra("device_name", "wifidisplay dongle");
            intent.putExtra("device_alias", "wifidisplay dongle");
        }
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        if (true == this.mReConnecting) {
            Toast.makeText(this.mContext, getMtkStringResourceId("wifi_display_disconnected"), 0).show();
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
        clearNotify();
        if (SystemProperties.get("ro.vendor.mtk_wfd_support").equals("1")) {
            updateChosenCapability(0, false);
            stopProfilingInfo();
        }
        notifyClearMotion(false);
        this.mAlarmManager.cancel(this.mWifiScanTimerListener);
        stopWifiScan(false);
    }

    private void actionAtConnecting() {
        Slog.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName());
    }

    private void actionAtConnectionFailed() {
        Slog.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName());
        if (true == this.mReConnecting) {
            Toast.makeText(this.mContext, getMtkStringResourceId("wifi_display_disconnected"), 0).show();
            resetReconnectVariable();
        }
        this.mAlarmManager.cancel(this.mWifiScanTimerListener);
        stopWifiScan(false);
    }

    private int loadWfdWpsSetup() {
        String str = SystemProperties.get("wlan.wfd.wps.setup", "1");
        if (DEBUG) {
            Slog.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + ", wfdWpsSetup = " + str);
        }
        switch (Integer.valueOf(str).intValue()) {
        }
        return 0;
    }

    private void loadDebugLevel() {
        String str = SystemProperties.get("wlan.wfd.controller.debug", "0");
        if (DEBUG) {
            Slog.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + ", debugLevel = " + str);
        }
        switch (Integer.valueOf(str).intValue()) {
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
        if (SystemProperties.get("ro.vendor.mtk_wfd_support").equals("1") && this.mWifiDisplayOnSetting && !this.mWifiP2pEnabled) {
            long j = Settings.Global.getLong(this.mContext.getContentResolver(), "wifi_reenable_delay", 500L);
            Slog.d(TAG, "Enable wifi with delay:" + j);
            this.mHandler.postDelayed(this.mEnableWifiDelay, j);
            Toast.makeText(this.mContext, getMtkStringResourceId("wifi_display_wfd_and_wifi_are_turned_on"), 0).show();
            return;
        }
        this.mAutoEnableWifi = false;
        updateWfdEnableState();
    }

    private void updateWfdStatFile(int i) {
    }

    private boolean checkInterference(Matcher matcher) {
        int iIntValue = Float.valueOf(matcher.group(4)).intValue();
        int iIntValue2 = Float.valueOf(matcher.group(6)).intValue();
        int iIntValue3 = Integer.valueOf(matcher.group(7)).intValue();
        int iIntValue4 = Integer.valueOf(matcher.group(8)).intValue();
        int iIntValue5 = Integer.valueOf(matcher.group(9)).intValue();
        int iIntValue6 = Integer.valueOf(matcher.group(10)).intValue();
        int iIntValue7 = Integer.valueOf(matcher.group(11)).intValue();
        int iIntValue8 = Integer.valueOf(matcher.group(12)).intValue();
        if (iIntValue < -50 || iIntValue2 < 58 || iIntValue3 < 10 || iIntValue4 > 3 || iIntValue5 > 2 || iIntValue6 > 2 || iIntValue7 > 2 || iIntValue8 > 1) {
            return true;
        }
        return false;
    }

    private int parseDec(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            Slog.e(TAG, "Failed to parse dec string " + str);
            return 0;
        }
    }

    private int parseFloat(String str) {
        try {
            return (int) Float.parseFloat(str);
        } catch (NumberFormatException e) {
            Slog.e(TAG, "Failed to parse float string " + str);
            return 0;
        }
    }

    private void updateSignalLevel(boolean z) {
        int averageScore = getAverageScore();
        updateScoreLevel(averageScore);
        String str = "W:" + averageScore + ",I:" + z + ",L:" + this.mLevel;
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
        String str2 = str + ">" + this.mLevel;
        handleLevelChange();
        if (this.WFDCONTROLLER_SQC_INFO_ON) {
            Toast.makeText(this.mContext, str2, 0).show();
        }
        Slog.d(TAG, str2);
    }

    private int getAverageScore() {
        this.mScore[this.mScoreIndex % 4] = this.mWifiScore;
        this.mScoreIndex++;
        int i = 0;
        int i2 = 0;
        for (int i3 = 0; i3 < 4; i3++) {
            if (this.mScore[i3] != -1) {
                i += this.mScore[i3];
                i2++;
            }
        }
        return i / i2;
    }

    private void updateScoreLevel(int i) {
        if (i >= 100) {
            if (this.mScoreLevel < 0) {
                this.mScoreLevel = 0;
            }
            this.mScoreLevel += 6;
            return;
        }
        if (i >= 80) {
            if (this.mScoreLevel < 0) {
                this.mScoreLevel = 0;
            }
            this.mScoreLevel += 2;
        } else if (i >= 30) {
            if (this.mScoreLevel > 0) {
                this.mScoreLevel = 0;
            }
            this.mScoreLevel -= 2;
        } else if (i >= 10) {
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

    private void registerEMObserver(int i, int i2) {
        this.WFDCONTROLLER_DISPLAY_TOAST_TIME = this.mContext.getResources().getInteger(getMtkIntegerResourceId("wfd_display_toast_time"));
        this.WFDCONTROLLER_DISPLAY_NOTIFICATION_TIME = this.mContext.getResources().getInteger(getMtkIntegerResourceId("wfd_display_notification_time"));
        this.WFDCONTROLLER_DISPLAY_RESOLUTION = this.mContext.getResources().getInteger(getMtkIntegerResourceId("wfd_display_default_resolution"));
        this.WFDCONTROLLER_DISPLAY_POWER_SAVING_OPTION = this.mContext.getResources().getInteger(getMtkIntegerResourceId("wfd_display_power_saving_option"));
        this.WFDCONTROLLER_DISPLAY_POWER_SAVING_DELAY = this.mContext.getResources().getInteger(getMtkIntegerResourceId("wfd_display_power_saving_delay"));
        this.WFDCONTROLLER_DISPLAY_SECURE_OPTION = this.mContext.getResources().getInteger(getMtkIntegerResourceId("wfd_display_secure_option"));
        Slog.d(TAG, "registerEMObserver(), tt:" + this.WFDCONTROLLER_DISPLAY_TOAST_TIME + ",nt:" + this.WFDCONTROLLER_DISPLAY_NOTIFICATION_TIME + ",res:" + this.WFDCONTROLLER_DISPLAY_RESOLUTION + ",ps:" + this.WFDCONTROLLER_DISPLAY_POWER_SAVING_OPTION + ",psd:" + this.WFDCONTROLLER_DISPLAY_POWER_SAVING_DELAY + ",so:" + this.WFDCONTROLLER_DISPLAY_SECURE_OPTION);
        Settings.Global.putInt(this.mContext.getContentResolver(), getMtkSettingsExtGlobalSetting("WIFI_DISPLAY_DISPLAY_TOAST_TIME"), this.WFDCONTROLLER_DISPLAY_TOAST_TIME);
        Settings.Global.putInt(this.mContext.getContentResolver(), getMtkSettingsExtGlobalSetting("WIFI_DISPLAY_DISPLAY_NOTIFICATION_TIME"), this.WFDCONTROLLER_DISPLAY_NOTIFICATION_TIME);
        Settings.Global.putInt(this.mContext.getContentResolver(), getMtkSettingsExtGlobalSetting("WIFI_DISPLAY_SQC_INFO_ON"), this.WFDCONTROLLER_SQC_INFO_ON ? 1 : 0);
        Settings.Global.putInt(this.mContext.getContentResolver(), getMtkSettingsExtGlobalSetting("WIFI_DISPLAY_QE_ON"), this.WFDCONTROLLER_QE_ON ? 1 : 0);
        if (SystemProperties.get("ro.vendor.mtk_wfd_support").equals("1")) {
            int i3 = Settings.Global.getInt(this.mContext.getContentResolver(), getMtkSettingsExtGlobalSetting("WIFI_DISPLAY_RESOLUTION"), -1);
            if (i3 == -1) {
                if (this.WFDCONTROLLER_DISPLAY_RESOLUTION >= 0 && this.WFDCONTROLLER_DISPLAY_RESOLUTION <= 3) {
                    int i4 = this.WFDCONTROLLER_DISPLAY_RESOLUTION;
                    this.mResolution = i4;
                    this.mPrevResolution = i4;
                } else if (i >= 1080 && i2 >= 1920) {
                    this.mResolution = 2;
                    this.mPrevResolution = 2;
                } else {
                    this.mResolution = 0;
                    this.mPrevResolution = 0;
                }
            } else if (i3 >= 0 && i3 <= 3) {
                this.mResolution = i3;
                this.mPrevResolution = i3;
            } else {
                this.mResolution = 0;
                this.mPrevResolution = 0;
            }
            int resolutionIndex = getResolutionIndex(this.mResolution);
            Slog.i(TAG, "mResolution:" + this.mResolution + ", resolutionIndex: " + resolutionIndex);
            SystemProperties.set("vendor.media.wfd.video-format", String.valueOf(resolutionIndex));
        }
        Settings.Global.putInt(this.mContext.getContentResolver(), getMtkSettingsExtGlobalSetting("WIFI_DISPLAY_AUTO_CHANNEL_SELECTION"), this.mAutoChannelSelection ? 1 : 0);
        Settings.Global.putInt(this.mContext.getContentResolver(), getMtkSettingsExtGlobalSetting("WIFI_DISPLAY_RESOLUTION"), this.mResolution);
        Settings.Global.putInt(this.mContext.getContentResolver(), getMtkSettingsExtGlobalSetting("WIFI_DISPLAY_POWER_SAVING_OPTION"), this.WFDCONTROLLER_DISPLAY_POWER_SAVING_OPTION);
        Settings.Global.putInt(this.mContext.getContentResolver(), getMtkSettingsExtGlobalSetting("WIFI_DISPLAY_POWER_SAVING_DELAY"), this.WFDCONTROLLER_DISPLAY_POWER_SAVING_DELAY);
        Settings.Global.putInt(this.mContext.getContentResolver(), getMtkSettingsExtGlobalSetting("WIFI_DISPLAY_LATENCY_PROFILING"), this.mLatencyProfiling);
        Settings.Global.putString(this.mContext.getContentResolver(), getMtkSettingsExtGlobalSetting("WIFI_DISPLAY_CHOSEN_CAPABILITY"), BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        initPortraitResolutionSupport();
        resetLatencyInfo();
        initSecureOption();
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor(getMtkSettingsExtGlobalSetting("WIFI_DISPLAY_DISPLAY_TOAST_TIME")), false, this.mObserver);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor(getMtkSettingsExtGlobalSetting("WIFI_DISPLAY_DISPLAY_NOTIFICATION_TIME")), false, this.mObserver);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor(getMtkSettingsExtGlobalSetting("WIFI_DISPLAY_SQC_INFO_ON")), false, this.mObserver);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor(getMtkSettingsExtGlobalSetting("WIFI_DISPLAY_QE_ON")), false, this.mObserver);
        if (SystemProperties.get("ro.vendor.mtk_wfd_support").equals("1")) {
            this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor(getMtkSettingsExtGlobalSetting("WIFI_DISPLAY_AUTO_CHANNEL_SELECTION")), false, this.mObserver);
            this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor(getMtkSettingsExtGlobalSetting("WIFI_DISPLAY_RESOLUTION")), false, this.mObserver);
            this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor(getMtkSettingsExtGlobalSetting("WIFI_DISPLAY_LATENCY_PROFILING")), false, this.mObserver);
            this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor(getMtkSettingsExtGlobalSetting("WIFI_DISPLAY_SECURITY_OPTION")), false, this.mObserver);
            this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor(getMtkSettingsExtGlobalSetting("WIFI_DISPLAY_PORTRAIT_RESOLUTION")), false, this.mObserver);
        }
    }

    private void initPortraitResolutionSupport() {
        Settings.Global.putInt(this.mContext.getContentResolver(), getMtkSettingsExtGlobalSetting("WIFI_DISPLAY_PORTRAIT_RESOLUTION"), 0);
        SystemProperties.set("vendor.media.wfd.portrait", String.valueOf(0));
    }

    private void handlePortraitResolutionSupportChange() {
        int i = Settings.Global.getInt(this.mContext.getContentResolver(), getMtkSettingsExtGlobalSetting("WIFI_DISPLAY_PORTRAIT_RESOLUTION"), 0);
        Slog.i(TAG, "handlePortraitResolutionSupportChange:" + i);
        SystemProperties.set("vendor.media.wfd.portrait", String.valueOf(i));
    }

    private void sendPortraitIntent() {
        Slog.d(TAG, "sendPortraitIntent()");
        Intent intent = new Intent(WFD_PORTRAIT);
        intent.addFlags(67108864);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void initSecureOption() {
        Settings.Global.putInt(this.mContext.getContentResolver(), getMtkSettingsExtGlobalSetting("WIFI_DISPLAY_SECURITY_OPTION"), this.WFDCONTROLLER_DISPLAY_SECURE_OPTION);
        SystemProperties.set("wlan.wfd.security.image", String.valueOf(this.WFDCONTROLLER_DISPLAY_SECURE_OPTION));
    }

    private void handleSecureOptionChange() {
        int i = Settings.Global.getInt(this.mContext.getContentResolver(), getMtkSettingsExtGlobalSetting("WIFI_DISPLAY_SECURITY_OPTION"), 1);
        if (i == this.WFDCONTROLLER_DISPLAY_SECURE_OPTION) {
            return;
        }
        Slog.i(TAG, "handleSecureOptionChange:" + i + "->" + this.WFDCONTROLLER_DISPLAY_SECURE_OPTION);
        this.WFDCONTROLLER_DISPLAY_SECURE_OPTION = i;
        SystemProperties.set("ro.sf.security.image", String.valueOf(this.WFDCONTROLLER_DISPLAY_SECURE_OPTION));
    }

    private int getResolutionIndex(int i) {
        switch (i) {
        }
        return 5;
    }

    private void handleResolutionChange() {
        boolean z = false;
        int i = Settings.Global.getInt(this.mContext.getContentResolver(), getMtkSettingsExtGlobalSetting("WIFI_DISPLAY_RESOLUTION"), 0);
        if (i == this.mResolution) {
            return;
        }
        this.mPrevResolution = this.mResolution;
        this.mResolution = i;
        Slog.d(TAG, "handleResolutionChange(), resolution:" + this.mPrevResolution + "->" + this.mResolution);
        int resolutionIndex = getResolutionIndex(this.mResolution);
        int resolutionIndex2 = getResolutionIndex(this.mPrevResolution);
        if (resolutionIndex == resolutionIndex2) {
            return;
        }
        if (Settings.Global.getInt(this.mContext.getContentResolver(), getMtkSettingsExtGlobalSetting("WIFI_DISPLAY_RESOLUTION_DONOT_REMIND"), 0) != 0) {
            z = true;
        }
        Slog.d(TAG, "index:" + resolutionIndex2 + "->" + resolutionIndex + ", doNotRemind:" + z);
        SystemProperties.set("vendor.media.wfd.video-format", String.valueOf(resolutionIndex));
        if (this.mConnectedDevice != null || this.mConnectingDevice != null) {
            if (z) {
                Slog.d(TAG, "-- reconnect for resolution change --");
                disconnect();
                this.mReconnectForResolutionChange = true;
                return;
            }
            showDialog(5);
        }
    }

    private void revertResolutionChange() {
        Slog.d(TAG, "revertResolutionChange(), resolution:" + this.mResolution + "->" + this.mPrevResolution);
        int resolutionIndex = getResolutionIndex(this.mResolution);
        int resolutionIndex2 = getResolutionIndex(this.mPrevResolution);
        Slog.d(TAG, "index:" + resolutionIndex + "->" + resolutionIndex2);
        SystemProperties.set("vendor.media.wfd.video-format", String.valueOf(resolutionIndex2));
        this.mResolution = this.mPrevResolution;
        Settings.Global.putInt(this.mContext.getContentResolver(), getMtkSettingsExtGlobalSetting("WIFI_DISPLAY_RESOLUTION"), this.mResolution);
    }

    private void handleLatencyProfilingChange() {
        int i = Settings.Global.getInt(this.mContext.getContentResolver(), getMtkSettingsExtGlobalSetting("WIFI_DISPLAY_LATENCY_PROFILING"), 2);
        if (i == this.mLatencyProfiling) {
            return;
        }
        Slog.d(TAG, "handleLatencyProfilingChange(), connected:" + this.mIsWFDConnected + ",value:" + this.mLatencyProfiling + "->" + i);
        this.mLatencyProfiling = i;
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
        this.mLatencyPanelView = LayoutInflater.from(this.mContext).inflate(getMtkLayoutResourceId("textpanel"), (ViewGroup) null);
        this.mTextView = (TextView) this.mLatencyPanelView.findViewById(getMtkIdResourceId("bodyText"));
        this.mTextView.setTextColor(-1);
        this.mTextView.setText("AP:\nS:\nR:\nAL:\n");
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.type = 2038;
        layoutParams.flags = 8;
        layoutParams.width = -2;
        layoutParams.height = -2;
        layoutParams.gravity = 51;
        layoutParams.alpha = 0.7f;
        ((WindowManager) this.mContext.getSystemService("window")).addView(this.mLatencyPanelView, layoutParams);
    }

    private void hideLatencyPanel() {
        Slog.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName());
        if (this.mLatencyPanelView != null) {
            ((WindowManager) this.mContext.getSystemService("window")).removeView(this.mLatencyPanelView);
            this.mLatencyPanelView = null;
        }
        this.mTextView = null;
    }

    private void checkA2dpStatus() {
        BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
        if (defaultAdapter == null || !defaultAdapter.isEnabled()) {
            Slog.d(TAG, "checkA2dpStatus(), BT is not enabled");
            return;
        }
        int i = Settings.Global.getInt(this.mContext.getContentResolver(), getMtkSettingsExtGlobalSetting("WIFI_DISPLAY_SOUND_PATH_DONOT_REMIND"), -1);
        Slog.d(TAG, "checkA2dpStatus(), value:" + i);
        if (i == 1) {
            return;
        }
        defaultAdapter.getProfileProxy(this.mContext, new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int i2, BluetoothProfile bluetoothProfile) {
                boolean zIsEmpty = ((BluetoothA2dp) bluetoothProfile).getConnectedDevices().isEmpty();
                Slog.d(MtkWifiDisplayController.TAG, "BluetoothProfile listener is connected, empty:" + zIsEmpty);
                if (!zIsEmpty) {
                    MtkWifiDisplayController.this.showDialog(6);
                }
            }

            @Override
            public void onServiceDisconnected(int i2) {
            }
        }, 2);
    }

    private void updateChosenCapability(int i, boolean z) {
        String str;
        String str2;
        String str3;
        String str4;
        String str5 = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        if (this.mIsWFDConnected) {
            if (this.mRemoteDisplay.getWfdParam(3) == 1) {
                str = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS + "LPCM(2 ch),";
            } else {
                str = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS + "AAC(2 ch),";
            }
            if (this.mRemoteDisplay.getWfdParam(4) == 1) {
                str2 = str + "H.264(CBP level 3.1),";
            } else {
                str2 = str + "H.264(CHP level 4.1),";
            }
            int resolutionIndex = getResolutionIndex(this.mResolution);
            if (resolutionIndex == 5) {
                if (z) {
                    str3 = str2 + "720x1280 30p,";
                } else {
                    str3 = str2 + "1280x720 30p,";
                }
            } else if (resolutionIndex == 7) {
                if (z) {
                    str3 = str2 + "1080x1920 30p,";
                } else {
                    str3 = str2 + "1920x1080 30p,";
                }
            } else {
                str3 = str2 + "640x480 60p,";
            }
            if (this.mRemoteDisplay.getWfdParam(7) == 1) {
                str4 = str3 + "with HDCP,";
            } else {
                str4 = str3 + "without HDCP,";
            }
            if (i != 0) {
                str5 = str4 + "with UIBC";
            } else {
                str5 = str4 + "without UIBC";
            }
        }
        Slog.d(TAG, "updateChosenCapability(), connected:" + this.mIsWFDConnected + ", capability:" + str5 + ", portrait:" + z);
        Settings.Global.putString(this.mContext.getContentResolver(), getMtkSettingsExtGlobalSetting("WIFI_DISPLAY_CHOSEN_CAPABILITY"), str5);
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
        Settings.Global.putString(this.mContext.getContentResolver(), getMtkSettingsExtGlobalSetting("WIFI_DISPLAY_WIFI_INFO"), "0,0,0,0");
        Settings.Global.putString(this.mContext.getContentResolver(), getMtkSettingsExtGlobalSetting("WIFI_DISPLAY_WFD_LATENCY"), "0,0,0");
    }

    private int getWifiApNum() {
        boolean z;
        List<ScanResult> scanResults = this.mWifiManager.getScanResults();
        ArrayList arrayList = new ArrayList();
        if (scanResults == null) {
            return 0;
        }
        int i = 0;
        for (ScanResult scanResult : scanResults) {
            if (scanResult.SSID != null && scanResult.SSID.length() != 0 && !scanResult.capabilities.contains("[IBSS]") && getFreqId(scanResult.frequency) == this.mWifiP2pChannelId) {
                Iterator it = arrayList.iterator();
                while (true) {
                    if (it.hasNext()) {
                        if (((String) it.next()).equals(scanResult.SSID)) {
                            z = true;
                            break;
                        }
                    } else {
                        z = false;
                        break;
                    }
                }
                if (!z) {
                    if (DEBUG) {
                        Slog.d(TAG, "AP SSID: " + scanResult.SSID);
                    }
                    arrayList.add(scanResult.SSID);
                    i++;
                }
            }
        }
        return i;
    }

    private void updateWifiP2pChannelId(boolean z, Intent intent) {
        if (this.mWfdEnabled && z && (this.mDesiredDevice != null || this.mSinkEnabled)) {
            int frequency = ((WifiP2pGroup) intent.getParcelableExtra("p2pGroupInfo")).getFrequency();
            this.mWifiP2pChannelId = getFreqId(frequency);
            Slog.d(TAG, "updateWifiP2pChannelId(), freq:" + frequency + ", id:" + this.mWifiP2pChannelId);
            return;
        }
        this.mWifiP2pChannelId = -1;
        Slog.d(TAG, "updateWifiP2pChannelId(), id:" + this.mWifiP2pChannelId);
    }

    private int getFreqId(int i) {
        switch (i) {
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
                return NetworkConstants.ICMPV6_NEIGHBOR_ADVERTISEMENT;
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
            if (!this.mNotiTimerStarted) {
                this.mHandler.postDelayed(this.mDisplayNotification, this.WFDCONTROLLER_DISPLAY_NOTIFICATION_TIME * 1000);
                this.mNotiTimerStarted = true;
                return;
            }
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
        this.mNotificationManager.cancelAsUser(null, getMtkStringResourceId("wifi_display_unstable_connection"), UserHandle.ALL);
    }

    private void showNotification(int i, int i2) {
        Slog.d(TAG, "showNotification(), titleId:" + i);
        this.mNotificationManager.cancelAsUser(null, i, UserHandle.ALL);
        Resources system = Resources.getSystem();
        this.mNotificationManager.notifyAsUser(null, i, new Notification.BigTextStyle(new Notification.Builder(this.mContext).setContentTitle(system.getString(i)).setContentText(system.getString(i2)).setSmallIcon(getMtkDrawableResourceId("ic_notify_wifidisplay_blink")).setAutoCancel(true)).bigText(system.getString(i2)).build(), UserHandle.ALL);
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
        if (SystemProperties.get("ro.vendor.mtk_wfd_sink_support").equals("1") && this.mSinkEnabled) {
            Slog.d(TAG, "[sink] callback onDisplayConnectionFailed()");
            this.mListener.onDisplayConnectionFailed();
        } else {
            unadvertiseDisplay();
        }
    }

    private void prepareDialog(int i) {
        Resources system = Resources.getSystem();
        if (1 == i) {
            this.mWifiDirectExcludeDialog = new AlertDialog.Builder(this.mContext).setMessage(system.getString(getMtkStringResourceId("wifi_display_wifi_p2p_disconnect_wfd_connect"))).setPositiveButton(system.getString(R.string.bugreport_option_interactive_title), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i2) {
                    Slog.d(MtkWifiDisplayController.TAG, "[Exclude Dialog] disconnect previous WiFi P2p connection");
                    MtkWifiDisplayController.this.mIsConnected_OtherP2p = false;
                    MtkWifiDisplayController.this.mWifiP2pManager.requestGroupInfo(MtkWifiDisplayController.this.mWifiP2pChannel, new WifiP2pManager.GroupInfoListener() {
                        @Override
                        public void onGroupInfoAvailable(WifiP2pGroup wifiP2pGroup) {
                            Slog.i(MtkWifiDisplayController.TAG, "onGroupInfoAvailable() of non wifidisplay p2p");
                            if (wifiP2pGroup == null) {
                                Slog.i(MtkWifiDisplayController.TAG, "group is null !!!");
                            } else if (wifiP2pGroup.getNetworkId() >= 0) {
                                Slog.i(MtkWifiDisplayController.TAG, "deletePersistentGroup of non wifidisplay p2p");
                                MtkWifiDisplayController.this.mWifiP2pManager.deletePersistentGroup(MtkWifiDisplayController.this.mWifiP2pChannel, wifiP2pGroup.getNetworkId(), null);
                                Slog.i(MtkWifiDisplayController.TAG, "removeGroup of non wifidisplay p2p");
                                MtkWifiDisplayController.this.mWifiP2pManager.removeGroup(MtkWifiDisplayController.this.mWifiP2pChannel, new WifiP2pManager.ActionListener() {
                                    @Override
                                    public void onSuccess() {
                                        Slog.i(MtkWifiDisplayController.TAG, "Disconnected from previous Wi-Fi P2p device, succeess");
                                    }

                                    @Override
                                    public void onFailure(int i3) {
                                        Slog.i(MtkWifiDisplayController.TAG, "Disconnected from previous Wi-Fi P2p device, failure = " + i3);
                                    }
                                });
                            }
                        }
                    });
                    MtkWifiDisplayController.this.chooseNo_WifiDirectExcludeDialog();
                    MtkWifiDisplayController.this.mUserDecided = true;
                }
            }).setNegativeButton(system.getString(R.string.badPuk), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i2) {
                    Slog.d(MtkWifiDisplayController.TAG, "[Exclude Dialog] keep previous Wi-Fi P2p connection");
                    MtkWifiDisplayController.this.chooseNo_WifiDirectExcludeDialog();
                    MtkWifiDisplayController.this.mUserDecided = true;
                }
            }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    Slog.d(MtkWifiDisplayController.TAG, "[Exclude Dialog] onCancel(): keep previous Wi-Fi P2p connection");
                    MtkWifiDisplayController.this.chooseNo_WifiDirectExcludeDialog();
                    MtkWifiDisplayController.this.mUserDecided = true;
                }
            }).setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    Slog.d(MtkWifiDisplayController.TAG, "[Exclude Dialog] onDismiss()");
                    if (!MtkWifiDisplayController.this.mUserDecided) {
                        MtkWifiDisplayController.this.chooseNo_WifiDirectExcludeDialog();
                    }
                }
            }).create();
            popupDialog(this.mWifiDirectExcludeDialog);
            return;
        }
        if (4 == i) {
            this.mReConnecteDialog = new AlertDialog.Builder(this.mContext).setTitle(getMtkStringResourceId("wifi_display_reconnect")).setMessage(getMtkStringResourceId("wifi_display_disconnect_then_reconnect")).setPositiveButton(system.getString(R.string.bugreport_option_interactive_title), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i2) {
                    if (MtkWifiDisplayController.DEBUG) {
                        Slog.d(MtkWifiDisplayController.TAG, "user want to reconnect");
                    }
                    MtkWifiDisplayController.this.mReScanning = true;
                    MtkWifiDisplayController.this.updateScanState();
                    MtkWifiDisplayController.this.mReConnection_Timeout_Remain_Seconds = 60;
                    MtkWifiDisplayController.this.mHandler.postDelayed(MtkWifiDisplayController.this.mReConnect, 1000L);
                }
            }).setNegativeButton(system.getString(R.string.badPuk), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i2) {
                    if (MtkWifiDisplayController.DEBUG) {
                        Slog.d(MtkWifiDisplayController.TAG, "user want nothing");
                    }
                }
            }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    if (MtkWifiDisplayController.DEBUG) {
                        Slog.d(MtkWifiDisplayController.TAG, "user want nothing");
                    }
                }
            }).create();
            popupDialog(this.mReConnecteDialog);
            return;
        }
        if (5 == i) {
            View viewInflate = LayoutInflater.from(this.mContext).inflate(getMtkLayoutResourceId("checkbox"), (ViewGroup) null);
            final CheckBox checkBox = (CheckBox) viewInflate.findViewById(getMtkIdResourceId("skip"));
            checkBox.setText(getMtkStringResourceId("wifi_display_do_not_remind_again"));
            this.mChangeResolutionDialog = new AlertDialog.Builder(this.mContext).setView(viewInflate).setMessage(getMtkStringResourceId("wifi_display_change_resolution_reminder")).setPositiveButton(system.getString(R.string.bugreport_option_interactive_title), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i2) {
                    boolean zIsChecked = checkBox.isChecked();
                    Slog.d(MtkWifiDisplayController.TAG, "[Change resolution]: ok. checked:" + zIsChecked);
                    if (zIsChecked) {
                        Settings.Global.putInt(MtkWifiDisplayController.this.mContext.getContentResolver(), MtkWifiDisplayController.this.getMtkSettingsExtGlobalSetting("WIFI_DISPLAY_RESOLUTION_DONOT_REMIND"), 1);
                    } else {
                        Settings.Global.putInt(MtkWifiDisplayController.this.mContext.getContentResolver(), MtkWifiDisplayController.this.getMtkSettingsExtGlobalSetting("WIFI_DISPLAY_RESOLUTION_DONOT_REMIND"), 0);
                    }
                    if (MtkWifiDisplayController.this.mConnectedDevice != null || MtkWifiDisplayController.this.mConnectingDevice != null) {
                        Slog.d(MtkWifiDisplayController.TAG, "-- reconnect for resolution change --");
                        MtkWifiDisplayController.this.disconnect();
                        MtkWifiDisplayController.this.mReconnectForResolutionChange = true;
                    }
                }
            }).setNegativeButton(system.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i2) {
                    Slog.d(MtkWifiDisplayController.TAG, "[Change resolution]: cancel");
                    MtkWifiDisplayController.this.revertResolutionChange();
                }
            }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    Slog.d(MtkWifiDisplayController.TAG, "[Change resolution]: doesn't choose");
                    MtkWifiDisplayController.this.revertResolutionChange();
                }
            }).create();
            popupDialog(this.mChangeResolutionDialog);
            return;
        }
        if (6 == i) {
            View viewInflate2 = LayoutInflater.from(this.mContext).inflate(getMtkLayoutResourceId("checkbox"), (ViewGroup) null);
            final CheckBox checkBox2 = (CheckBox) viewInflate2.findViewById(getMtkIdResourceId("skip"));
            checkBox2.setText(getMtkStringResourceId("wifi_display_do_not_remind_again"));
            if (Settings.Global.getInt(this.mContext.getContentResolver(), getMtkSettingsExtGlobalSetting("WIFI_DISPLAY_SOUND_PATH_DONOT_REMIND"), -1) == -1) {
                checkBox2.setChecked(true);
            }
            this.mSoundPathDialog = new AlertDialog.Builder(this.mContext).setView(viewInflate2).setMessage(getMtkStringResourceId("wifi_display_sound_path_reminder")).setPositiveButton(system.getString(R.string.bugreport_option_interactive_title), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i2) {
                    boolean zIsChecked = checkBox2.isChecked();
                    Slog.d(MtkWifiDisplayController.TAG, "[Sound path reminder]: ok. checked:" + zIsChecked);
                    if (zIsChecked) {
                        Settings.Global.putInt(MtkWifiDisplayController.this.mContext.getContentResolver(), MtkWifiDisplayController.this.getMtkSettingsExtGlobalSetting("WIFI_DISPLAY_SOUND_PATH_DONOT_REMIND"), 1);
                    } else {
                        Settings.Global.putInt(MtkWifiDisplayController.this.mContext.getContentResolver(), MtkWifiDisplayController.this.getMtkSettingsExtGlobalSetting("WIFI_DISPLAY_SOUND_PATH_DONOT_REMIND"), 0);
                    }
                }
            }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    Slog.d(MtkWifiDisplayController.TAG, "[Sound path reminder]: cancel");
                }
            }).create();
            popupDialog(this.mSoundPathDialog);
            return;
        }
        if (7 == i) {
            View viewInflate3 = LayoutInflater.from(this.mContext).inflate(getMtkLayoutResourceId("progress_dialog"), (ViewGroup) null);
            ((ProgressBar) viewInflate3.findViewById(getMtkIdResourceId("progress"))).setIndeterminate(true);
            ((TextView) viewInflate3.findViewById(getMtkIdResourceId("progress_text"))).setText(getMtkStringResourceId("wifi_display_wait_connection"));
            this.mWaitConnectDialog = new AlertDialog.Builder(this.mContext).setView(viewInflate3).setNegativeButton(system.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i2) {
                    Slog.d(MtkWifiDisplayController.TAG, "[Wait connection]: cancel");
                    MtkWifiDisplayController.this.disconnectWfdSink();
                }
            }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    Slog.d(MtkWifiDisplayController.TAG, "[Wait connection]: no choice");
                    MtkWifiDisplayController.this.disconnectWfdSink();
                }
            }).create();
            popupDialog(this.mWaitConnectDialog);
            return;
        }
        if (8 != i) {
            if (9 == i) {
                View viewInflate4 = LayoutInflater.from(this.mContext).inflate(getMtkLayoutResourceId("progress_dialog"), (ViewGroup) null);
                ((ProgressBar) viewInflate4.findViewById(getMtkIdResourceId("progress"))).setIndeterminate(true);
                ((TextView) viewInflate4.findViewById(getMtkIdResourceId("progress_text"))).setText(getMtkStringResourceId("wifi_display_build_connection"));
                this.mBuildConnectDialog = new AlertDialog.Builder(this.mContext).setView(viewInflate4).setNegativeButton(system.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i2) {
                        Slog.d(MtkWifiDisplayController.TAG, "[Build connection]: cancel");
                        MtkWifiDisplayController.this.disconnectWfdSink();
                    }
                }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        Slog.d(MtkWifiDisplayController.TAG, "[Build connection]: no choice");
                        MtkWifiDisplayController.this.disconnectWfdSink();
                    }
                }).create();
                popupDialog(this.mBuildConnectDialog);
                return;
            }
            return;
        }
        dismissDialogDetail(this.mWaitConnectDialog);
        this.mConfirmConnectDialog = new AlertDialog.Builder(this.mContext).setMessage(this.mSinkDeviceName + " " + system.getString(getMtkStringResourceId("wifi_display_confirm_connection"))).setPositiveButton(system.getString(R.string.config_systemSupervision), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i2) {
                Slog.d(MtkWifiDisplayController.TAG, "[GC confirm connection]: accept");
                int iIntValue = Integer.valueOf(SystemProperties.get("wfd.sink.go_intent", String.valueOf(14))).intValue();
                Slog.i(MtkWifiDisplayController.TAG, "Sink go_intent:" + iIntValue);
                MtkWifiDisplayController.this.mWifiP2pManager.setGCInviteResult(MtkWifiDisplayController.this.mWifiP2pChannel, true, iIntValue, null);
                MtkWifiDisplayController.this.showDialog(9);
            }
        }).setNegativeButton(system.getString(R.string.badPuk), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i2) {
                Slog.d(MtkWifiDisplayController.TAG, "[GC confirm connection]: declines");
                MtkWifiDisplayController.this.mWifiP2pManager.setGCInviteResult(MtkWifiDisplayController.this.mWifiP2pChannel, false, 0, null);
                MtkWifiDisplayController.this.disconnectWfdSink();
            }
        }).setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                Slog.d(MtkWifiDisplayController.TAG, "[Confirm connection]: cancel");
                MtkWifiDisplayController.this.mWifiP2pManager.setGCInviteResult(MtkWifiDisplayController.this.mWifiP2pChannel, false, 0, null);
                MtkWifiDisplayController.this.disconnectWfdSink();
            }
        }).create();
        popupDialog(this.mConfirmConnectDialog);
    }

    private void popupDialog(AlertDialog alertDialog) {
        alertDialog.getWindow().setType(2003);
        alertDialog.getWindow().getAttributes().privateFlags |= 16;
        alertDialog.show();
    }

    private void showDialog(int i) {
        this.mUserDecided = false;
        prepareDialog(i);
    }

    private void dismissDialog() {
        dismissDialogDetail(this.mWifiDirectExcludeDialog);
        dismissDialogDetail(this.mReConnecteDialog);
    }

    private void dismissDialogDetail(AlertDialog alertDialog) {
        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
        }
    }

    private void notifyClearMotion(boolean z) {
        if (SystemProperties.get("ro.mtk_clearmotion_support").equals("1")) {
            SystemProperties.set("sys.display.clearMotion.dimmed", z ? "1" : "0");
            Intent intent = new Intent(WFD_CLEARMOTION_DIMMED);
            intent.addFlags(67108864);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    private String nameValueAssign(String[] strArr) {
        if (strArr == null || 2 != strArr.length) {
            return null;
        }
        return strArr[1];
    }

    public boolean getIfSinkEnabled() {
        Slog.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + ",enable = " + this.mSinkEnabled);
        return this.mSinkEnabled;
    }

    public void requestEnableSink(boolean z) {
        Slog.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + ",enable = " + z + ",Connected = " + this.mIsWFDConnected + ", option = " + SystemProperties.get("ro.vendor.mtk_wfd_sink_support").equals("1") + ", WfdEnabled = " + this.mWfdEnabled);
        if (!SystemProperties.get("ro.vendor.mtk_wfd_sink_support").equals("1") || this.mSinkEnabled == z || this.mIsWFDConnected) {
            return;
        }
        if (z && this.mIsConnected_OtherP2p) {
            Slog.i(TAG, "OtherP2P is connected! Only set variable. Ignore !");
            this.mSinkEnabled = z;
            enterSinkState(SinkState.SINK_STATE_IDLE);
            return;
        }
        stopWifiScan(z);
        if (z) {
            requestStopScan();
        }
        this.mSinkEnabled = z;
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
                    MtkWifiDisplayController.this.notifyDisplayConnecting();
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
                if (MtkWifiDisplayController.this.isSinkState(SinkState.SINK_STATE_WAITING_P2P_CONNECTION)) {
                    MtkWifiDisplayController.this.notifyDisplayConnecting();
                }
            }
        });
    }

    public void requestSuspendDisplay(boolean z, Surface surface) {
        Slog.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + ",suspend = " + z);
        this.mSinkSurface = surface;
        if (isSinkState(SinkState.SINK_STATE_RTSP_CONNECTED)) {
            if (this.mRemoteDisplay != null) {
                this.mRemoteDisplay.suspendDisplay(z, surface);
            }
            blockNotificationList(!z);
        } else {
            Slog.i(TAG, "State is wrong !!!, SinkState:" + this.mSinkState);
        }
    }

    public void sendUibcInputEvent(String str) {
        if (SystemProperties.get("ro.vendor.mtk_wfd_sink_uibc_support").equals("1") && this.mSinkEnabled && this.mRemoteDisplay != null) {
            this.mRemoteDisplay.sendUibcEvent(str);
        }
    }

    private synchronized void disconnectWfdSink() {
        Slog.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + ", SinkState = " + this.mSinkState);
        if (isSinkState(SinkState.SINK_STATE_WAITING_P2P_CONNECTION) || isSinkState(SinkState.SINK_STATE_WIFI_P2P_CONNECTED)) {
            this.mHandler.removeCallbacks(this.mGetSinkIpAddr);
            this.mHandler.removeCallbacks(this.mSinkDiscover);
            if (isSinkState(SinkState.SINK_STATE_WAITING_P2P_CONNECTION)) {
                Slog.i(TAG, "WAITING_P2P_CONNECTION cancelConnect");
                enterSinkState(SinkState.SINK_STATE_IDLE);
                this.mWifiP2pManager.cancelConnect(this.mWifiP2pChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Slog.i(MtkWifiDisplayController.TAG, "Canceled connection success");
                        next();
                    }

                    @Override
                    public void onFailure(int i) {
                        Slog.i(MtkWifiDisplayController.TAG, "Failed to cancel connection,remove group + delete group");
                        next();
                    }

                    private void next() {
                        if (MtkWifiDisplayController.this.mWifiP2pManager != null) {
                            MtkWifiDisplayController.this.stopPeerDiscovery();
                            MtkWifiDisplayController.this.deletePersistentGroup_ex();
                            MtkWifiDisplayController.this.updateIfSinkConnected(false);
                            MtkWifiDisplayController.this.mWifiP2pManager.setMiracastMode(0);
                        }
                    }
                });
                return;
            }
            Slog.i(TAG, "Remove P2P group");
            this.mWifiP2pManager.removeGroup(this.mWifiP2pChannel, null);
            stopPeerDiscovery();
            Slog.i(TAG, "Disconnected from WFD sink (P2P).");
            deletePersistentGroup();
            enterSinkState(SinkState.SINK_STATE_IDLE);
            updateIfSinkConnected(false);
            this.mWifiP2pManager.setMiracastMode(0);
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Slog.d(MtkWifiDisplayController.TAG, "[Sink] callback onDisplayDisconnected()");
                    MtkWifiDisplayController.this.mListener.onDisplayDisconnected();
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
                    MtkWifiDisplayController.this.disconnectWfdSink();
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
                MtkWifiDisplayController.this.dismissDialogDetail(MtkWifiDisplayController.this.mWaitConnectDialog);
                MtkWifiDisplayController.this.dismissDialogDetail(MtkWifiDisplayController.this.mConfirmConnectDialog);
                MtkWifiDisplayController.this.dismissDialogDetail(MtkWifiDisplayController.this.mBuildConnectDialog);
                if (MtkWifiDisplayController.this.mWifiDirectExcludeDialog != null && MtkWifiDisplayController.this.mWifiDirectExcludeDialog.isShowing()) {
                    MtkWifiDisplayController.this.chooseNo_WifiDirectExcludeDialog();
                }
                MtkWifiDisplayController.this.dismissDialogDetail(MtkWifiDisplayController.this.mWifiDirectExcludeDialog);
            }
        });
    }

    private void deletePersistentGroup_ex() {
        Slog.i(TAG, "deletePersistentGroup_ex channel:" + this.mWifiP2pChannel);
        if (this.mSinkP2pGroup != null) {
            Slog.d(TAG, "deletePersistentGroup_ex mSinkP2pGroup!=null ");
            if (this.mSinkP2pGroup.getNetworkId() >= 0) {
                this.mWifiP2pManager.deletePersistentGroup(this.mWifiP2pChannel, this.mSinkP2pGroup.getNetworkId(), null);
            }
            this.mSinkP2pGroup = null;
            return;
        }
        this.mWifiP2pManager.requestGroupInfo(this.mWifiP2pChannel, new WifiP2pManager.GroupInfoListener() {
            @Override
            public void onGroupInfoAvailable(WifiP2pGroup wifiP2pGroup) {
                Slog.i(MtkWifiDisplayController.TAG, "deletePersistentGroup_ex onGroupInfoAvailable(), mSinkState:" + MtkWifiDisplayController.this.mSinkState);
                if (wifiP2pGroup == null) {
                    Slog.i(MtkWifiDisplayController.TAG, "group is null !!!");
                } else if (wifiP2pGroup.getNetworkId() >= 0) {
                    Slog.i(MtkWifiDisplayController.TAG, "request requestGroupInfo cb deletePersistentGroup");
                    MtkWifiDisplayController.this.mWifiP2pManager.deletePersistentGroup(MtkWifiDisplayController.this.mWifiP2pChannel, wifiP2pGroup.getNetworkId(), null);
                    Slog.i(MtkWifiDisplayController.TAG, "request requestGroupInfo cb removeGroup");
                    MtkWifiDisplayController.this.mWifiP2pManager.removeGroup(MtkWifiDisplayController.this.mWifiP2pChannel, null);
                }
                MtkWifiDisplayController.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Slog.d(MtkWifiDisplayController.TAG, "[Sink] callback onDisplayDisconnected()");
                        MtkWifiDisplayController.this.mListener.onDisplayDisconnected();
                    }
                });
            }
        });
    }

    private void removeSpecificPersistentGroup() {
        final WifiP2pDevice wifiP2pDevice = this.mConnectingDevice != null ? this.mConnectingDevice : this.mConnectedDevice;
        if (wifiP2pDevice == null || !wifiP2pDevice.deviceName.contains("BRAVIA")) {
            return;
        }
        Slog.d(TAG, "removeSpecificPersistentGroup");
        this.mWifiP2pManager.requestPersistentGroupInfo(this.mWifiP2pChannel, new WifiP2pManager.PersistentGroupInfoListener() {
            public void onPersistentGroupInfoAvailable(WifiP2pGroupList wifiP2pGroupList) {
                Slog.d(MtkWifiDisplayController.TAG, "onPersistentGroupInfoAvailable()");
                for (WifiP2pGroup wifiP2pGroup : wifiP2pGroupList.getGroupList()) {
                    if (wifiP2pDevice.deviceAddress.equalsIgnoreCase(wifiP2pGroup.getOwner().deviceAddress)) {
                        Slog.d(MtkWifiDisplayController.TAG, "deletePersistentGroup(), net id:" + wifiP2pGroup.getNetworkId());
                        MtkWifiDisplayController.this.mWifiP2pManager.deletePersistentGroup(MtkWifiDisplayController.this.mWifiP2pChannel, wifiP2pGroup.getNetworkId(), null);
                    }
                }
            }
        });
    }

    private void deletePersistentGroup() {
        Slog.d(TAG, "deletePersistentGroup");
        if (this.mSinkP2pGroup != null) {
            Slog.d(TAG, "mSinkP2pGroup network id: " + this.mSinkP2pGroup.getNetworkId());
            if (this.mSinkP2pGroup.getNetworkId() >= 0) {
                this.mWifiP2pManager.deletePersistentGroup(this.mWifiP2pChannel, this.mSinkP2pGroup.getNetworkId(), null);
            }
            this.mSinkP2pGroup = null;
        }
    }

    private void handleSinkP2PConnection(NetworkInfo networkInfo) {
        Slog.i(TAG, "handleSinkP2PConnection(), sinkState:" + this.mSinkState);
        if (this.mWifiP2pManager != null && networkInfo.isConnected()) {
            if (!isSinkState(SinkState.SINK_STATE_WAITING_P2P_CONNECTION)) {
                return;
            }
            this.mWifiP2pManager.requestGroupInfo(this.mWifiP2pChannel, new WifiP2pManager.GroupInfoListener() {
                @Override
                public void onGroupInfoAvailable(WifiP2pGroup wifiP2pGroup) {
                    Slog.i(MtkWifiDisplayController.TAG, "onGroupInfoAvailable(), mSinkState:" + MtkWifiDisplayController.this.mSinkState);
                    if (!MtkWifiDisplayController.this.isSinkState(SinkState.SINK_STATE_WAITING_P2P_CONNECTION)) {
                        return;
                    }
                    if (wifiP2pGroup != null) {
                        MtkWifiDisplayController.this.mSinkP2pGroup = wifiP2pGroup;
                        boolean z = true;
                        if (wifiP2pGroup.getOwner().deviceAddress.equals(MtkWifiDisplayController.this.mThisDevice.deviceAddress)) {
                            Slog.i(MtkWifiDisplayController.TAG, "group owner is my self !");
                            Iterator<WifiP2pDevice> it = wifiP2pGroup.getClientList().iterator();
                            while (true) {
                                if (it.hasNext()) {
                                    WifiP2pDevice next = it.next();
                                    Slog.i(MtkWifiDisplayController.TAG, "Client device:" + next);
                                    if (MtkWifiDisplayController.this.isWifiDisplaySource(next) && MtkWifiDisplayController.this.mSinkDeviceName.equals(next.deviceName)) {
                                        MtkWifiDisplayController.this.mSinkMacAddress = next.deviceAddress;
                                        MtkWifiDisplayController.this.mSinkPort = next.wfdInfo.getControlPort();
                                        Slog.i(MtkWifiDisplayController.TAG, "Found ! Sink name:" + MtkWifiDisplayController.this.mSinkDeviceName + ",mac address:" + MtkWifiDisplayController.this.mSinkMacAddress + ",port:" + MtkWifiDisplayController.this.mSinkPort);
                                        break;
                                    }
                                } else {
                                    z = false;
                                    break;
                                }
                            }
                        } else {
                            Slog.i(MtkWifiDisplayController.TAG, "group owner is not my self ! So I am GC.");
                            MtkWifiDisplayController.this.mSinkMacAddress = wifiP2pGroup.getOwner().deviceAddress;
                            MtkWifiDisplayController.this.mSinkPort = wifiP2pGroup.getOwner().wfdInfo.getControlPort();
                            Slog.i(MtkWifiDisplayController.TAG, "Sink name:" + MtkWifiDisplayController.this.mSinkDeviceName + ",mac address:" + MtkWifiDisplayController.this.mSinkMacAddress + ",port:" + MtkWifiDisplayController.this.mSinkPort);
                        }
                        if (z) {
                            MtkWifiDisplayController.this.mSinkIpRetryCount = 50;
                            MtkWifiDisplayController.this.enterSinkState(SinkState.SINK_STATE_WIFI_P2P_CONNECTED);
                            MtkWifiDisplayController.this.mHandler.postDelayed(MtkWifiDisplayController.this.mGetSinkIpAddr, 300L);
                            return;
                        }
                        return;
                    }
                    Slog.i(MtkWifiDisplayController.TAG, "Error: group is null !!!");
                }
            });
        } else if (isSinkState(SinkState.SINK_STATE_WAITING_P2P_CONNECTION) || isSinkState(SinkState.SINK_STATE_WIFI_P2P_CONNECTED)) {
            disconnectWfdSink();
        }
    }

    private boolean isWifiDisplaySource(WifiP2pDevice wifiP2pDevice) {
        boolean z = wifiP2pDevice.wfdInfo != null && wifiP2pDevice.wfdInfo.isWfdEnabled() && wifiP2pDevice.wfdInfo.isSessionAvailable() && isSourceDeviceType(wifiP2pDevice.wfdInfo.getDeviceType());
        if (!z) {
            Slog.e(TAG, "This is not WFD source device !!!!!!");
        }
        return z;
    }

    private void notifyDisplayConnecting() {
        WifiDisplay wifiDisplay = new WifiDisplay("Temp address", "WiFi Display Device", (String) null, true, true, false);
        Slog.d(TAG, "[sink] callback onDisplayConnecting()");
        this.mListener.onDisplayConnecting(wifiDisplay);
    }

    private boolean isSourceDeviceType(int i) {
        return i == 0 || i == 3;
    }

    private void startWaitConnection() {
        Slog.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + ", mSinkState:" + this.mSinkState + ", retryCount:" + this.mSinkDiscoverRetryCount);
        this.mWifiP2pManager.discoverPeers(this.mWifiP2pChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                if (MtkWifiDisplayController.this.isSinkState(SinkState.SINK_STATE_WAITING_P2P_CONNECTION)) {
                    Slog.d(MtkWifiDisplayController.TAG, "[sink] succeed for discoverPeers()");
                    MtkWifiDisplayController.this.showDialog(7);
                }
            }

            @Override
            public void onFailure(int i) {
                if (MtkWifiDisplayController.this.isSinkState(SinkState.SINK_STATE_WAITING_P2P_CONNECTION)) {
                    Slog.e(MtkWifiDisplayController.TAG, "[sink] failed for discoverPeers(), reason:" + i + ", retryCount:" + MtkWifiDisplayController.this.mSinkDiscoverRetryCount);
                    if (i != 2 || MtkWifiDisplayController.this.mSinkDiscoverRetryCount <= 0) {
                        MtkWifiDisplayController.this.enterSinkState(SinkState.SINK_STATE_IDLE);
                        Slog.d(MtkWifiDisplayController.TAG, "[sink] callback onDisplayConnectionFailed()");
                        MtkWifiDisplayController.this.mListener.onDisplayConnectionFailed();
                    } else {
                        MtkWifiDisplayController.access$13210(MtkWifiDisplayController.this);
                        MtkWifiDisplayController.this.mHandler.postDelayed(MtkWifiDisplayController.this.mSinkDiscover, 100L);
                    }
                }
            }
        });
    }

    private void connectRtsp() {
        Slog.d(TAG, "connectRtsp(), mSinkState:" + this.mSinkState);
        this.mRemoteDisplay = RemoteDisplay.connect(this.mSinkIpAddress, this.mSinkSurface, new RemoteDisplay.Listener() {
            public void onDisplayConnected(Surface surface, int i, int i2, int i3, int i4) {
                Slog.i(MtkWifiDisplayController.TAG, "Opened RTSP connection! w:" + i + ",h:" + i2);
                MtkWifiDisplayController.this.dismissDialogDetail(MtkWifiDisplayController.this.mBuildConnectDialog);
                if (MtkWifiDisplayController.this.isSinkState(SinkState.SINK_STATE_WAITING_RTSP)) {
                    MtkWifiDisplayController.this.enterSinkState(SinkState.SINK_STATE_RTSP_CONNECTED);
                    MtkWifiDisplayController.this.mHandler.removeCallbacks(MtkWifiDisplayController.this.mRtspSinkTimeout);
                    WifiDisplay wifiDisplay = new WifiDisplay(MtkWifiDisplayController.this.mSinkMacAddress, MtkWifiDisplayController.this.mSinkDeviceName, (String) null, true, true, false);
                    if (i < i2) {
                        MtkWifiDisplayController.this.sendPortraitIntent();
                    }
                    Slog.d(MtkWifiDisplayController.TAG, "[sink] callback onDisplayConnected(), addr:" + MtkWifiDisplayController.this.mSinkMacAddress + ", name:" + MtkWifiDisplayController.this.mSinkDeviceName);
                    MtkWifiDisplayController.this.updateIfSinkConnected(true);
                    MtkWifiDisplayController.this.mListener.onDisplayConnected(wifiDisplay, null, 0, 0, 0);
                    return;
                }
                Slog.i(MtkWifiDisplayController.TAG, "Opened RTSP connection wrong state return");
            }

            public void onDisplayDisconnected() {
                Slog.i(MtkWifiDisplayController.TAG, "Closed RTSP connection! mSinkState:" + MtkWifiDisplayController.this.mSinkState);
                if (MtkWifiDisplayController.this.isSinkState(SinkState.SINK_STATE_WAITING_RTSP) || MtkWifiDisplayController.this.isSinkState(SinkState.SINK_STATE_RTSP_CONNECTED)) {
                    MtkWifiDisplayController.this.dismissDialogDetail(MtkWifiDisplayController.this.mBuildConnectDialog);
                    MtkWifiDisplayController.this.mHandler.removeCallbacks(MtkWifiDisplayController.this.mRtspSinkTimeout);
                    MtkWifiDisplayController.this.disconnectWfdSink();
                }
            }

            public void onDisplayError(int i) {
                Slog.i(MtkWifiDisplayController.TAG, "Lost RTSP connection! mSinkState:" + MtkWifiDisplayController.this.mSinkState);
                if (MtkWifiDisplayController.this.isSinkState(SinkState.SINK_STATE_WAITING_RTSP) || MtkWifiDisplayController.this.isSinkState(SinkState.SINK_STATE_RTSP_CONNECTED)) {
                    MtkWifiDisplayController.this.dismissDialogDetail(MtkWifiDisplayController.this.mBuildConnectDialog);
                    MtkWifiDisplayController.this.mHandler.removeCallbacks(MtkWifiDisplayController.this.mRtspSinkTimeout);
                    MtkWifiDisplayController.this.disconnectWfdSink();
                }
            }

            public void onDisplayKeyEvent(int i, int i2) {
                Slog.d(MtkWifiDisplayController.TAG, "onDisplayKeyEvent:");
            }

            public void onDisplayGenericMsgEvent(int i) {
            }
        }, this.mHandler);
        enterSinkState(SinkState.SINK_STATE_WAITING_RTSP);
        this.mHandler.postDelayed(this.mRtspSinkTimeout, (this.mWifiDisplayCertMode ? RTSP_TIMEOUT_SECONDS_CERT_MODE : 10) * 1000);
    }

    private void blockNotificationList(boolean z) {
        Slog.i(TAG, "blockNotificationList(), block:" + z);
        if (z) {
            this.mStatusBarManager.disable(65536);
        } else {
            this.mStatusBarManager.disable(0);
        }
    }

    private void enterSinkState(SinkState sinkState) {
        Slog.i(TAG, "enterSinkState()," + this.mSinkState + "->" + sinkState);
        this.mSinkState = sinkState;
    }

    private boolean isSinkState(SinkState sinkState) {
        return this.mSinkState == sinkState;
    }

    private void updateIfSinkConnected(boolean z) {
        if (this.mIsWFDConnected == z) {
            return;
        }
        this.mIsWFDConnected = z;
        blockNotificationList(z);
        StringBuilder sb = new StringBuilder();
        sb.append("Set session available as ");
        sb.append(!z);
        Slog.i(TAG, sb.toString());
        this.mWfdInfo.setSessionAvailable(!z);
        this.mWifiP2pManager.setWFDInfo(this.mWifiP2pChannel, this.mWfdInfo, null);
        if (this.mWakeLockSink != null) {
            if (z) {
                this.mWakeLockSink.acquire();
            } else {
                this.mWakeLockSink.release();
            }
        }
        getAudioFocus(z);
    }

    private void getAudioFocus(boolean z) {
        if (z) {
            if (this.mAudioManager.requestAudioFocus(this.mAudioFocusListener, 3, 1) == 0) {
                Slog.e(TAG, "requestAudioFocus() FAIL !!!");
                return;
            }
            return;
        }
        this.mAudioManager.abandonAudioFocus(this.mAudioFocusListener);
    }

    private void setSinkMiracastMode() {
        Slog.i(TAG, "setSinkMiracastMode(), freq:" + this.mWifiApFreq);
        this.mWifiP2pManager.setMiracastMode(2);
    }

    private void notifyApDisconnected() {
        Slog.e(TAG, "notifyApDisconnected()");
        Toast.makeText(this.mContext, Resources.getSystem().getString(getMtkStringResourceId("wifi_display_wifi_network_disconnected"), this.mWifiApSsid), 0).show();
        showNotification(getMtkStringResourceId("wifi_display_channel_confliction"), getMtkStringResourceId("wifi_display_wifi_network_cannot_coexist"));
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

    private void handleChannelConflictProcedure(ChannelConflictEvt channelConflictEvt) {
        if (isCCState(null) || isCCState(ChannelConflictState.STATE_IDLE)) {
            return;
        }
        Slog.i(TAG, "handleChannelConflictProcedure(), evt:" + channelConflictEvt + ", ccState:" + this.mChannelConflictState);
        if (isCCState(ChannelConflictState.STATE_AP_DISCONNECTING)) {
            if (channelConflictEvt == ChannelConflictEvt.EVT_AP_DISCONNECTED) {
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
            if (channelConflictEvt == ChannelConflictEvt.EVT_WFD_P2P_CONNECTED) {
                Slog.i(TAG, "connect AP, mNetworkId:" + this.mNetworkId);
                this.mWifiManager.connect(this.mNetworkId, null);
                enterCCState(ChannelConflictState.STATE_AP_CONNECTING);
                return;
            }
            enterCCState(ChannelConflictState.STATE_IDLE);
            return;
        }
        if (isCCState(ChannelConflictState.STATE_AP_CONNECTING)) {
            if (channelConflictEvt == ChannelConflictEvt.EVT_AP_CONNECTED) {
                if (this.mDisplayApToast) {
                    Toast.makeText(this.mContext, Resources.getSystem().getString(getMtkStringResourceId("wifi_display_connected_to_wifi_network"), this.mWifiApSsid), 0).show();
                }
                enterCCState(ChannelConflictState.STATE_IDLE);
                return;
            }
            enterCCState(ChannelConflictState.STATE_IDLE);
        }
    }

    private boolean wifiApHasSameFreq() {
        Slog.i(TAG, "wifiApHasSameFreq()");
        boolean z = false;
        if (this.mWifiApSsid == null || this.mWifiApSsid.length() < 2) {
            Slog.e(TAG, "mWifiApSsid is invalid !!");
            return false;
        }
        String strSubstring = this.mWifiApSsid.substring(1, this.mWifiApSsid.length() - 1);
        List<ScanResult> scanResults = this.mWifiManager.getScanResults();
        if (scanResults != null) {
            Iterator<ScanResult> it = scanResults.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                ScanResult next = it.next();
                Slog.i(TAG, "SSID:" + next.SSID + ",Freq:" + next.frequency + ",Level:" + next.level + ",BSSID:" + next.BSSID);
                if (next.SSID != null && next.SSID.length() != 0 && !next.capabilities.contains("[IBSS]") && next.SSID.equals(strSubstring) && next.frequency == this.mP2pOperFreq) {
                    z = true;
                    break;
                }
            }
        }
        Slog.i(TAG, "AP SSID:" + strSubstring + ", sameFreq:" + z);
        return z;
    }

    private int getSameFreqNetworkId() {
        Slog.i(TAG, "getSameFreqNetworkId()");
        List<WifiConfiguration> configuredNetworks = this.mWifiManager.getConfiguredNetworks();
        List<ScanResult> scanResults = this.mWifiManager.getScanResults();
        int i = -1;
        if (scanResults == null || configuredNetworks == null) {
            Slog.i(TAG, "results:" + scanResults + ",everConnecteds:" + configuredNetworks);
            return -1;
        }
        int i2 = -128;
        for (WifiConfiguration wifiConfiguration : configuredNetworks) {
            String strSubstring = wifiConfiguration.SSID.substring(1, wifiConfiguration.SSID.length() - 1);
            Slog.i(TAG, "SSID:" + strSubstring + ",NetId:" + wifiConfiguration.networkId);
            Iterator<ScanResult> it = scanResults.iterator();
            while (true) {
                if (it.hasNext()) {
                    ScanResult next = it.next();
                    if (next.SSID != null && next.SSID.length() != 0 && !next.capabilities.contains("[IBSS]") && strSubstring.equals(next.SSID) && next.frequency == this.mP2pOperFreq && next.level > i2) {
                        i = wifiConfiguration.networkId;
                        i2 = next.level;
                        break;
                    }
                }
            }
        }
        Slog.i(TAG, "Selected Network Id:" + i);
        return i;
    }

    private void enterCCState(ChannelConflictState channelConflictState) {
        Slog.i(TAG, "enterCCState()," + this.mChannelConflictState + "->" + channelConflictState);
        this.mChannelConflictState = channelConflictState;
    }

    private boolean isCCState(ChannelConflictState channelConflictState) {
        return this.mChannelConflictState == channelConflictState;
    }

    private String getMtkSettingsExtGlobalSetting(String str) {
        try {
            Class<?> cls = Class.forName("com.mediatek.provider.MtkSettingsExt$Global", false, ClassLoader.getSystemClassLoader());
            Field field = cls.getField(str);
            field.setAccessible(true);
            return (String) field.get(cls);
        } catch (Exception e) {
            Slog.e(TAG, "Cannot get MTK settings - " + e);
            return BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
    }

    private int getMtkStringResourceId(String str) {
        try {
            Field field = Class.forName("com.mediatek.internal.R$string", false, ClassLoader.getSystemClassLoader()).getField(str);
            field.setAccessible(true);
            return field.getInt(null);
        } catch (Exception e) {
            Slog.e(TAG, "Cannot get MTK resource - " + e);
            return 0;
        }
    }

    private int getMtkIntegerResourceId(String str) {
        try {
            Field field = Class.forName("com.mediatek.internal.R$integer", false, ClassLoader.getSystemClassLoader()).getField(str);
            field.setAccessible(true);
            return field.getInt(null);
        } catch (Exception e) {
            Slog.e(TAG, "Cannot get MTK resource - " + e);
            return 0;
        }
    }

    private int getMtkLayoutResourceId(String str) {
        try {
            Field field = Class.forName("com.mediatek.internal.R$layout", false, ClassLoader.getSystemClassLoader()).getField(str);
            field.setAccessible(true);
            return field.getInt(null);
        } catch (Exception e) {
            Slog.e(TAG, "Cannot get MTK resource - " + e);
            return 0;
        }
    }

    private int getMtkIdResourceId(String str) {
        try {
            Field field = Class.forName("com.mediatek.internal.R$id", false, ClassLoader.getSystemClassLoader()).getField(str);
            field.setAccessible(true);
            return field.getInt(null);
        } catch (Exception e) {
            Slog.e(TAG, "Cannot get MTK resource - " + e);
            return 0;
        }
    }

    private int getMtkDrawableResourceId(String str) {
        try {
            Field field = Class.forName("com.mediatek.internal.R$drawable", false, ClassLoader.getSystemClassLoader()).getField(str);
            field.setAccessible(true);
            return field.getInt(null);
        } catch (Exception e) {
            Slog.e(TAG, "Cannot get MTK resource - " + e);
            return 0;
        }
    }
}
