package com.android.server.wifi;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.NetworkInfo;
import android.net.wifi.RssiPacketCountInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Message;
import android.os.Messenger;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.LruCache;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class WifiWatchdogStateMachine extends StateMachine {
    private static final int BASE = 135168;
    private static final int BSSID_STAT_CACHE_SIZE = 20;
    private static final int BSSID_STAT_EMPTY_COUNT = 3;
    private static final int BSSID_STAT_RANGE_HIGH_DBM = -45;
    private static final int BSSID_STAT_RANGE_LOW_DBM = -105;
    private static final int CMD_RSSI_FETCH = 135179;
    private static final int EVENT_BSSID_CHANGE = 135175;
    private static final int EVENT_NETWORK_STATE_CHANGE = 135170;
    private static final int EVENT_RSSI_CHANGE = 135171;
    private static final int EVENT_SCREEN_OFF = 135177;
    private static final int EVENT_SCREEN_ON = 135176;
    private static final int EVENT_SUPPLICANT_STATE_CHANGE = 135172;
    private static final int EVENT_WATCHDOG_SETTINGS_CHANGE = 135174;
    private static final int EVENT_WATCHDOG_TOGGLED = 135169;
    private static final int EVENT_WIFI_RADIO_STATE_CHANGE = 135173;
    private static final double EXP_COEFFICIENT_MONITOR = 0.5d;
    private static final double EXP_COEFFICIENT_RECORD = 0.1d;
    static final int GOOD_LINK_DETECTED = 135190;
    private static final double GOOD_LINK_LOSS_THRESHOLD = 0.1d;
    private static final int GOOD_LINK_RSSI_RANGE_MAX = 20;
    private static final int GOOD_LINK_RSSI_RANGE_MIN = 3;
    private static final int LINK_MONITOR_LEVEL_THRESHOLD = 4;
    private static final long LINK_SAMPLING_INTERVAL_MS = 1000;
    static final int POOR_LINK_DETECTED = 135189;
    private static final double POOR_LINK_LOSS_THRESHOLD = 0.5d;
    private static final double POOR_LINK_MIN_VOLUME = 2.0d;
    private static final int POOR_LINK_SAMPLE_COUNT = 3;
    private static double[] sPresetLoss;
    private BroadcastReceiver mBroadcastReceiver;
    private LruCache<String, BssidStatistics> mBssidCache;
    private ConnectedState mConnectedState;
    private ContentResolver mContentResolver;
    private Context mContext;
    private BssidStatistics mCurrentBssid;
    private VolumeWeightedEMA mCurrentLoss;
    private int mCurrentSignalLevel;
    private DefaultState mDefaultState;
    private IntentFilter mIntentFilter;
    private boolean mIsScreenOn;
    private LinkMonitoringState mLinkMonitoringState;
    private LinkProperties mLinkProperties;
    private NotConnectedState mNotConnectedState;
    private OnlineState mOnlineState;
    private OnlineWatchState mOnlineWatchState;
    private boolean mPoorNetworkDetectionEnabled;
    private int mRssiFetchToken;
    private VerifyingLinkState mVerifyingLinkState;
    private WatchdogDisabledState mWatchdogDisabledState;
    private WatchdogEnabledState mWatchdogEnabledState;
    private WifiInfo mWifiInfo;
    private WifiManager mWifiManager;
    private AsyncChannel mWsmChannel;
    private static final GoodLinkTarget[] GOOD_LINK_TARGET = {new GoodLinkTarget(0, 3, 1800000), new GoodLinkTarget(3, 5, 300000), new GoodLinkTarget(6, 10, 60000), new GoodLinkTarget(9, 30, 0)};
    private static final MaxAvoidTime[] MAX_AVOID_TIME = {new MaxAvoidTime(1800000, -200), new MaxAvoidTime(300000, -70), new MaxAvoidTime(0, -55)};
    private static final boolean DBG = false;
    private static boolean sWifiOnly = DBG;

    static int access$2504(WifiWatchdogStateMachine x0) {
        int i = x0.mRssiFetchToken + 1;
        x0.mRssiFetchToken = i;
        return i;
    }

    private WifiWatchdogStateMachine(Context context, Messenger dstMessenger) {
        super("WifiWatchdogStateMachine");
        this.mWsmChannel = new AsyncChannel();
        this.mBssidCache = new LruCache<>(20);
        this.mRssiFetchToken = 0;
        this.mIsScreenOn = true;
        this.mDefaultState = new DefaultState();
        this.mWatchdogDisabledState = new WatchdogDisabledState();
        this.mWatchdogEnabledState = new WatchdogEnabledState();
        this.mNotConnectedState = new NotConnectedState();
        this.mVerifyingLinkState = new VerifyingLinkState();
        this.mConnectedState = new ConnectedState();
        this.mOnlineWatchState = new OnlineWatchState();
        this.mLinkMonitoringState = new LinkMonitoringState();
        this.mOnlineState = new OnlineState();
        this.mContext = context;
        this.mContentResolver = context.getContentResolver();
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
        this.mWsmChannel.connectSync(this.mContext, getHandler(), dstMessenger);
        setupNetworkReceiver();
        registerForSettingsChanges();
        registerForWatchdogToggle();
        addState(this.mDefaultState);
        addState(this.mWatchdogDisabledState, this.mDefaultState);
        addState(this.mWatchdogEnabledState, this.mDefaultState);
        addState(this.mNotConnectedState, this.mWatchdogEnabledState);
        addState(this.mVerifyingLinkState, this.mWatchdogEnabledState);
        addState(this.mConnectedState, this.mWatchdogEnabledState);
        addState(this.mOnlineWatchState, this.mConnectedState);
        addState(this.mLinkMonitoringState, this.mConnectedState);
        addState(this.mOnlineState, this.mConnectedState);
        if (isWatchdogEnabled()) {
            setInitialState(this.mNotConnectedState);
        } else {
            setInitialState(this.mWatchdogDisabledState);
        }
        setLogRecSize(25);
        setLogOnlyTransitions(true);
        updateSettings();
    }

    public static WifiWatchdogStateMachine makeWifiWatchdogStateMachine(Context context, Messenger dstMessenger) {
        boolean z = DBG;
        ContentResolver contentResolver = context.getContentResolver();
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService("connectivity");
        if (!cm.isNetworkSupported(0)) {
            z = true;
        }
        sWifiOnly = z;
        putSettingsGlobalBoolean(contentResolver, "wifi_watchdog_on", true);
        WifiWatchdogStateMachine wwsm = new WifiWatchdogStateMachine(context, dstMessenger);
        wwsm.start();
        return wwsm;
    }

    private void setupNetworkReceiver() {
        this.mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals("android.net.wifi.RSSI_CHANGED")) {
                    WifiWatchdogStateMachine.this.obtainMessage(WifiWatchdogStateMachine.EVENT_RSSI_CHANGE, intent.getIntExtra("newRssi", -200), 0).sendToTarget();
                    return;
                }
                if (action.equals("android.net.wifi.supplicant.STATE_CHANGE")) {
                    WifiWatchdogStateMachine.this.sendMessage(WifiWatchdogStateMachine.EVENT_SUPPLICANT_STATE_CHANGE, intent);
                    return;
                }
                if (action.equals("android.net.wifi.STATE_CHANGE")) {
                    WifiWatchdogStateMachine.this.sendMessage(WifiWatchdogStateMachine.EVENT_NETWORK_STATE_CHANGE, intent);
                    return;
                }
                if (action.equals("android.intent.action.SCREEN_ON")) {
                    WifiWatchdogStateMachine.this.sendMessage(WifiWatchdogStateMachine.EVENT_SCREEN_ON);
                } else if (action.equals("android.intent.action.SCREEN_OFF")) {
                    WifiWatchdogStateMachine.this.sendMessage(WifiWatchdogStateMachine.EVENT_SCREEN_OFF);
                } else if (action.equals("android.net.wifi.WIFI_STATE_CHANGED")) {
                    WifiWatchdogStateMachine.this.sendMessage(WifiWatchdogStateMachine.EVENT_WIFI_RADIO_STATE_CHANGE, intent.getIntExtra("wifi_state", 4));
                }
            }
        };
        this.mIntentFilter = new IntentFilter();
        this.mIntentFilter.addAction("android.net.wifi.STATE_CHANGE");
        this.mIntentFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        this.mIntentFilter.addAction("android.net.wifi.RSSI_CHANGED");
        this.mIntentFilter.addAction("android.net.wifi.supplicant.STATE_CHANGE");
        this.mIntentFilter.addAction("android.intent.action.SCREEN_ON");
        this.mIntentFilter.addAction("android.intent.action.SCREEN_OFF");
        this.mContext.registerReceiver(this.mBroadcastReceiver, this.mIntentFilter);
    }

    private void registerForWatchdogToggle() {
        ContentObserver contentObserver = new ContentObserver(getHandler()) {
            @Override
            public void onChange(boolean selfChange) {
                WifiWatchdogStateMachine.this.sendMessage(WifiWatchdogStateMachine.EVENT_WATCHDOG_TOGGLED);
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("wifi_watchdog_on"), DBG, contentObserver);
    }

    private void registerForSettingsChanges() {
        ContentObserver contentObserver = new ContentObserver(getHandler()) {
            @Override
            public void onChange(boolean selfChange) {
                WifiWatchdogStateMachine.this.sendMessage(WifiWatchdogStateMachine.EVENT_WATCHDOG_SETTINGS_CHANGE);
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("wifi_watchdog_poor_network_test_enabled"), DBG, contentObserver);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        super.dump(fd, pw, args);
        pw.println("mWifiInfo: [" + this.mWifiInfo + "]");
        pw.println("mLinkProperties: [" + this.mLinkProperties + "]");
        pw.println("mCurrentSignalLevel: [" + this.mCurrentSignalLevel + "]");
        pw.println("mPoorNetworkDetectionEnabled: [" + this.mPoorNetworkDetectionEnabled + "]");
    }

    private boolean isWatchdogEnabled() {
        boolean ret = getSettingsGlobalBoolean(this.mContentResolver, "wifi_watchdog_on", true);
        return ret;
    }

    private void updateSettings() {
        this.mPoorNetworkDetectionEnabled = DBG;
    }

    class DefaultState extends State {
        DefaultState() {
        }

        public void enter() {
        }

        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case WifiWatchdogStateMachine.EVENT_NETWORK_STATE_CHANGE:
                case WifiWatchdogStateMachine.EVENT_SUPPLICANT_STATE_CHANGE:
                case WifiWatchdogStateMachine.EVENT_WIFI_RADIO_STATE_CHANGE:
                case WifiWatchdogStateMachine.EVENT_BSSID_CHANGE:
                case WifiWatchdogStateMachine.CMD_RSSI_FETCH:
                case 151573:
                case 151574:
                    return true;
                case WifiWatchdogStateMachine.EVENT_RSSI_CHANGE:
                    WifiWatchdogStateMachine.this.mCurrentSignalLevel = WifiWatchdogStateMachine.this.calculateSignalLevel(msg.arg1);
                    return true;
                case WifiWatchdogStateMachine.EVENT_WATCHDOG_SETTINGS_CHANGE:
                    WifiWatchdogStateMachine.this.updateSettings();
                    return true;
                case WifiWatchdogStateMachine.EVENT_SCREEN_ON:
                    WifiWatchdogStateMachine.this.mIsScreenOn = true;
                    return true;
                case WifiWatchdogStateMachine.EVENT_SCREEN_OFF:
                    WifiWatchdogStateMachine.this.mIsScreenOn = WifiWatchdogStateMachine.DBG;
                    return true;
                default:
                    WifiWatchdogStateMachine.this.loge("Unhandled message " + msg + " in state " + WifiWatchdogStateMachine.this.getCurrentState().getName());
                    return true;
            }
        }
    }

    class WatchdogDisabledState extends State {
        WatchdogDisabledState() {
        }

        public void enter() {
        }

        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case WifiWatchdogStateMachine.EVENT_WATCHDOG_TOGGLED:
                    if (!WifiWatchdogStateMachine.this.isWatchdogEnabled()) {
                        return true;
                    }
                    WifiWatchdogStateMachine.this.transitionTo(WifiWatchdogStateMachine.this.mNotConnectedState);
                    return true;
                case WifiWatchdogStateMachine.EVENT_NETWORK_STATE_CHANGE:
                    Intent intent = (Intent) msg.obj;
                    NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                    switch (AnonymousClass4.$SwitchMap$android$net$NetworkInfo$DetailedState[networkInfo.getDetailedState().ordinal()]) {
                        case 1:
                            WifiWatchdogStateMachine.this.sendLinkStatusNotification(true);
                        default:
                            return WifiWatchdogStateMachine.DBG;
                    }
            }
        }
    }

    static class AnonymousClass4 {
        static final int[] $SwitchMap$android$net$NetworkInfo$DetailedState = new int[NetworkInfo.DetailedState.values().length];

        static {
            try {
                $SwitchMap$android$net$NetworkInfo$DetailedState[NetworkInfo.DetailedState.VERIFYING_POOR_LINK.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$net$NetworkInfo$DetailedState[NetworkInfo.DetailedState.CONNECTED.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
        }
    }

    class WatchdogEnabledState extends State {
        WatchdogEnabledState() {
        }

        public void enter() {
        }

        public boolean processMessage(Message msg) {
            String bssid;
            switch (msg.what) {
                case WifiWatchdogStateMachine.EVENT_WATCHDOG_TOGGLED:
                    if (!WifiWatchdogStateMachine.this.isWatchdogEnabled()) {
                        WifiWatchdogStateMachine.this.transitionTo(WifiWatchdogStateMachine.this.mWatchdogDisabledState);
                    }
                    break;
                case WifiWatchdogStateMachine.EVENT_NETWORK_STATE_CHANGE:
                    Intent intent = (Intent) msg.obj;
                    NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                    WifiWatchdogStateMachine.this.mWifiInfo = (WifiInfo) intent.getParcelableExtra("wifiInfo");
                    WifiWatchdogStateMachine wifiWatchdogStateMachine = WifiWatchdogStateMachine.this;
                    if (WifiWatchdogStateMachine.this.mWifiInfo != null) {
                        bssid = WifiWatchdogStateMachine.this.mWifiInfo.getBSSID();
                    } else {
                        bssid = null;
                    }
                    wifiWatchdogStateMachine.updateCurrentBssid(bssid);
                    switch (AnonymousClass4.$SwitchMap$android$net$NetworkInfo$DetailedState[networkInfo.getDetailedState().ordinal()]) {
                        case 1:
                            WifiWatchdogStateMachine.this.mLinkProperties = (LinkProperties) intent.getParcelableExtra("linkProperties");
                            if (WifiWatchdogStateMachine.this.mPoorNetworkDetectionEnabled) {
                                if (WifiWatchdogStateMachine.this.mWifiInfo == null || WifiWatchdogStateMachine.this.mCurrentBssid == null) {
                                    WifiWatchdogStateMachine.this.loge("Ignore, wifiinfo " + WifiWatchdogStateMachine.this.mWifiInfo + " bssid " + WifiWatchdogStateMachine.this.mCurrentBssid);
                                    WifiWatchdogStateMachine.this.sendLinkStatusNotification(true);
                                } else {
                                    WifiWatchdogStateMachine.this.transitionTo(WifiWatchdogStateMachine.this.mVerifyingLinkState);
                                }
                            } else {
                                WifiWatchdogStateMachine.this.sendLinkStatusNotification(true);
                            }
                            break;
                        case 2:
                            WifiWatchdogStateMachine.this.transitionTo(WifiWatchdogStateMachine.this.mOnlineWatchState);
                            break;
                        default:
                            WifiWatchdogStateMachine.this.transitionTo(WifiWatchdogStateMachine.this.mNotConnectedState);
                            break;
                    }
                    break;
                case WifiWatchdogStateMachine.EVENT_RSSI_CHANGE:
                default:
                    return WifiWatchdogStateMachine.DBG;
                case WifiWatchdogStateMachine.EVENT_SUPPLICANT_STATE_CHANGE:
                    SupplicantState supplicantState = (SupplicantState) ((Intent) msg.obj).getParcelableExtra("newState");
                    if (supplicantState == SupplicantState.COMPLETED) {
                        WifiWatchdogStateMachine.this.mWifiInfo = WifiWatchdogStateMachine.this.mWifiManager.getConnectionInfo();
                        WifiWatchdogStateMachine.this.updateCurrentBssid(WifiWatchdogStateMachine.this.mWifiInfo.getBSSID());
                    }
                    break;
                case WifiWatchdogStateMachine.EVENT_WIFI_RADIO_STATE_CHANGE:
                    if (msg.arg1 == 0) {
                        WifiWatchdogStateMachine.this.transitionTo(WifiWatchdogStateMachine.this.mNotConnectedState);
                    }
                    break;
            }
            return true;
        }
    }

    class NotConnectedState extends State {
        NotConnectedState() {
        }

        public void enter() {
        }
    }

    class VerifyingLinkState extends State {
        private int mSampleCount;

        VerifyingLinkState() {
        }

        public void enter() {
            this.mSampleCount = 0;
            if (WifiWatchdogStateMachine.this.mCurrentBssid != null) {
                WifiWatchdogStateMachine.this.mCurrentBssid.newLinkDetected();
            }
            WifiWatchdogStateMachine.this.sendMessage(WifiWatchdogStateMachine.this.obtainMessage(WifiWatchdogStateMachine.CMD_RSSI_FETCH, WifiWatchdogStateMachine.access$2504(WifiWatchdogStateMachine.this), 0));
        }

        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case WifiWatchdogStateMachine.EVENT_WATCHDOG_SETTINGS_CHANGE:
                    WifiWatchdogStateMachine.this.updateSettings();
                    if (!WifiWatchdogStateMachine.this.mPoorNetworkDetectionEnabled) {
                        WifiWatchdogStateMachine.this.sendLinkStatusNotification(true);
                    }
                    break;
                case WifiWatchdogStateMachine.EVENT_BSSID_CHANGE:
                    WifiWatchdogStateMachine.this.transitionTo(WifiWatchdogStateMachine.this.mVerifyingLinkState);
                    break;
                case WifiWatchdogStateMachine.CMD_RSSI_FETCH:
                    if (msg.arg1 == WifiWatchdogStateMachine.this.mRssiFetchToken) {
                        WifiWatchdogStateMachine.this.mWsmChannel.sendMessage(151572);
                        WifiWatchdogStateMachine.this.sendMessageDelayed(WifiWatchdogStateMachine.this.obtainMessage(WifiWatchdogStateMachine.CMD_RSSI_FETCH, WifiWatchdogStateMachine.access$2504(WifiWatchdogStateMachine.this), 0), WifiWatchdogStateMachine.LINK_SAMPLING_INTERVAL_MS);
                    }
                    break;
                case 151573:
                    if (WifiWatchdogStateMachine.this.mCurrentBssid != null && msg.obj != null) {
                        RssiPacketCountInfo info = (RssiPacketCountInfo) msg.obj;
                        int rssi = info.rssi;
                        long time = WifiWatchdogStateMachine.this.mCurrentBssid.mBssidAvoidTimeMax - SystemClock.elapsedRealtime();
                        if (time <= 0) {
                            WifiWatchdogStateMachine.this.sendLinkStatusNotification(true);
                        } else if (rssi >= WifiWatchdogStateMachine.this.mCurrentBssid.mGoodLinkTargetRssi) {
                            int i = this.mSampleCount + 1;
                            this.mSampleCount = i;
                            if (i >= WifiWatchdogStateMachine.this.mCurrentBssid.mGoodLinkTargetCount) {
                                WifiWatchdogStateMachine.this.mCurrentBssid.mBssidAvoidTimeMax = 0L;
                                WifiWatchdogStateMachine.this.sendLinkStatusNotification(true);
                            }
                        } else {
                            this.mSampleCount = 0;
                        }
                    }
                    break;
                case 151574:
                    break;
                default:
                    return WifiWatchdogStateMachine.DBG;
            }
            return true;
        }
    }

    class ConnectedState extends State {
        ConnectedState() {
        }

        public void enter() {
        }

        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case WifiWatchdogStateMachine.EVENT_WATCHDOG_SETTINGS_CHANGE:
                    WifiWatchdogStateMachine.this.updateSettings();
                    if (WifiWatchdogStateMachine.this.mPoorNetworkDetectionEnabled) {
                        WifiWatchdogStateMachine.this.transitionTo(WifiWatchdogStateMachine.this.mOnlineWatchState);
                    } else {
                        WifiWatchdogStateMachine.this.transitionTo(WifiWatchdogStateMachine.this.mOnlineState);
                    }
                    return true;
                default:
                    return WifiWatchdogStateMachine.DBG;
            }
        }
    }

    class OnlineWatchState extends State {
        OnlineWatchState() {
        }

        public void enter() {
            if (!WifiWatchdogStateMachine.this.mPoorNetworkDetectionEnabled) {
                WifiWatchdogStateMachine.this.transitionTo(WifiWatchdogStateMachine.this.mOnlineState);
            } else {
                handleRssiChange();
            }
        }

        private void handleRssiChange() {
            if (WifiWatchdogStateMachine.this.mCurrentSignalLevel <= 4 && WifiWatchdogStateMachine.this.mCurrentBssid != null) {
                WifiWatchdogStateMachine.this.transitionTo(WifiWatchdogStateMachine.this.mLinkMonitoringState);
            }
        }

        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case WifiWatchdogStateMachine.EVENT_RSSI_CHANGE:
                    WifiWatchdogStateMachine.this.mCurrentSignalLevel = WifiWatchdogStateMachine.this.calculateSignalLevel(msg.arg1);
                    handleRssiChange();
                    return true;
                default:
                    return WifiWatchdogStateMachine.DBG;
            }
        }
    }

    class LinkMonitoringState extends State {
        private int mLastRssi;
        private int mLastTxBad;
        private int mLastTxGood;
        private int mSampleCount;

        LinkMonitoringState() {
        }

        public void enter() {
            this.mSampleCount = 0;
            WifiWatchdogStateMachine.this.mCurrentLoss = WifiWatchdogStateMachine.this.new VolumeWeightedEMA(0.5d);
            WifiWatchdogStateMachine.this.sendMessage(WifiWatchdogStateMachine.this.obtainMessage(WifiWatchdogStateMachine.CMD_RSSI_FETCH, WifiWatchdogStateMachine.access$2504(WifiWatchdogStateMachine.this), 0));
        }

        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case WifiWatchdogStateMachine.EVENT_RSSI_CHANGE:
                    WifiWatchdogStateMachine.this.mCurrentSignalLevel = WifiWatchdogStateMachine.this.calculateSignalLevel(msg.arg1);
                    if (WifiWatchdogStateMachine.this.mCurrentSignalLevel > 4) {
                        WifiWatchdogStateMachine.this.transitionTo(WifiWatchdogStateMachine.this.mOnlineWatchState);
                    }
                    return true;
                case WifiWatchdogStateMachine.EVENT_BSSID_CHANGE:
                    WifiWatchdogStateMachine.this.transitionTo(WifiWatchdogStateMachine.this.mLinkMonitoringState);
                    return true;
                case WifiWatchdogStateMachine.CMD_RSSI_FETCH:
                    if (!WifiWatchdogStateMachine.this.mIsScreenOn) {
                        WifiWatchdogStateMachine.this.transitionTo(WifiWatchdogStateMachine.this.mOnlineState);
                    } else if (msg.arg1 == WifiWatchdogStateMachine.this.mRssiFetchToken) {
                        WifiWatchdogStateMachine.this.mWsmChannel.sendMessage(151572);
                        WifiWatchdogStateMachine.this.sendMessageDelayed(WifiWatchdogStateMachine.this.obtainMessage(WifiWatchdogStateMachine.CMD_RSSI_FETCH, WifiWatchdogStateMachine.access$2504(WifiWatchdogStateMachine.this), 0), WifiWatchdogStateMachine.LINK_SAMPLING_INTERVAL_MS);
                    }
                    return true;
                case 151573:
                    if (WifiWatchdogStateMachine.this.mCurrentBssid != null) {
                        RssiPacketCountInfo info = (RssiPacketCountInfo) msg.obj;
                        int rssi = info.rssi;
                        int mrssi = (this.mLastRssi + rssi) / 2;
                        int txbad = info.txbad;
                        int txgood = info.txgood;
                        long now = SystemClock.elapsedRealtime();
                        if (now - WifiWatchdogStateMachine.this.mCurrentBssid.mLastTimeSample < 2000) {
                            int dbad = txbad - this.mLastTxBad;
                            int dgood = txgood - this.mLastTxGood;
                            int dtotal = dbad + dgood;
                            if (dtotal > 0) {
                                double loss = ((double) dbad) / ((double) dtotal);
                                WifiWatchdogStateMachine.this.mCurrentLoss.update(loss, dtotal);
                                WifiWatchdogStateMachine.this.mCurrentBssid.updateLoss(mrssi, loss, dtotal);
                                if (WifiWatchdogStateMachine.this.mCurrentLoss.mValue > 0.5d && WifiWatchdogStateMachine.this.mCurrentLoss.mVolume > WifiWatchdogStateMachine.POOR_LINK_MIN_VOLUME) {
                                    int i = this.mSampleCount + 1;
                                    this.mSampleCount = i;
                                    if (i >= 3 && WifiWatchdogStateMachine.this.mCurrentBssid.poorLinkDetected(rssi)) {
                                        WifiWatchdogStateMachine.this.sendLinkStatusNotification(WifiWatchdogStateMachine.DBG);
                                        WifiWatchdogStateMachine.access$2504(WifiWatchdogStateMachine.this);
                                    }
                                } else {
                                    this.mSampleCount = 0;
                                }
                            }
                        }
                        WifiWatchdogStateMachine.this.mCurrentBssid.mLastTimeSample = now;
                        this.mLastTxBad = txbad;
                        this.mLastTxGood = txgood;
                        this.mLastRssi = rssi;
                    }
                    return true;
                case 151574:
                    return true;
                default:
                    return WifiWatchdogStateMachine.DBG;
            }
        }
    }

    class OnlineState extends State {
        OnlineState() {
        }

        public void enter() {
        }

        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case WifiWatchdogStateMachine.EVENT_SCREEN_ON:
                    WifiWatchdogStateMachine.this.mIsScreenOn = true;
                    if (WifiWatchdogStateMachine.this.mPoorNetworkDetectionEnabled) {
                        WifiWatchdogStateMachine.this.transitionTo(WifiWatchdogStateMachine.this.mOnlineWatchState);
                    }
                    break;
            }
            return true;
        }
    }

    private void updateCurrentBssid(String bssid) {
        if (bssid == null) {
            if (this.mCurrentBssid != null) {
                this.mCurrentBssid = null;
                sendMessage(EVENT_BSSID_CHANGE);
                return;
            }
            return;
        }
        if (this.mCurrentBssid == null || !bssid.equals(this.mCurrentBssid.mBssid)) {
            this.mCurrentBssid = this.mBssidCache.get(bssid);
            if (this.mCurrentBssid == null) {
                this.mCurrentBssid = new BssidStatistics(bssid);
                this.mBssidCache.put(bssid, this.mCurrentBssid);
            }
            sendMessage(EVENT_BSSID_CHANGE);
        }
    }

    private int calculateSignalLevel(int rssi) {
        int signalLevel = WifiManager.calculateSignalLevel(rssi, 5);
        return signalLevel;
    }

    private void sendLinkStatusNotification(boolean isGood) {
        if (isGood) {
            this.mWsmChannel.sendMessage(GOOD_LINK_DETECTED);
            if (this.mCurrentBssid != null) {
                this.mCurrentBssid.mLastTimeGood = SystemClock.elapsedRealtime();
                return;
            }
            return;
        }
        this.mWsmChannel.sendMessage(POOR_LINK_DETECTED);
        if (this.mCurrentBssid != null) {
            this.mCurrentBssid.mLastTimePoor = SystemClock.elapsedRealtime();
        }
        logd("Poor link notification is sent");
    }

    private static boolean getSettingsGlobalBoolean(ContentResolver cr, String name, boolean def) {
        if (Settings.Global.getInt(cr, name, def ? 1 : 0) == 1) {
            return true;
        }
        return DBG;
    }

    private static boolean putSettingsGlobalBoolean(ContentResolver cr, String name, boolean value) {
        return Settings.Global.putInt(cr, name, value ? 1 : 0);
    }

    private static class GoodLinkTarget {
        public final int REDUCE_TIME_MS;
        public final int RSSI_ADJ_DBM;
        public final int SAMPLE_COUNT;

        public GoodLinkTarget(int adj, int count, int time) {
            this.RSSI_ADJ_DBM = adj;
            this.SAMPLE_COUNT = count;
            this.REDUCE_TIME_MS = time;
        }
    }

    private static class MaxAvoidTime {
        public final int MIN_RSSI_DBM;
        public final int TIME_MS;

        public MaxAvoidTime(int time, int rssi) {
            this.TIME_MS = time;
            this.MIN_RSSI_DBM = rssi;
        }
    }

    private class VolumeWeightedEMA {
        private final double mAlpha;
        private double mValue = 0.0d;
        private double mVolume = 0.0d;
        private double mProduct = 0.0d;

        public VolumeWeightedEMA(double coefficient) {
            this.mAlpha = coefficient;
        }

        public void update(double newValue, int newVolume) {
            if (newVolume > 0) {
                double newProduct = newValue * ((double) newVolume);
                this.mProduct = (this.mAlpha * newProduct) + ((1.0d - this.mAlpha) * this.mProduct);
                this.mVolume = (this.mAlpha * ((double) newVolume)) + ((1.0d - this.mAlpha) * this.mVolume);
                this.mValue = this.mProduct / this.mVolume;
            }
        }
    }

    private class BssidStatistics {
        private final String mBssid;
        private long mBssidAvoidTimeMax;
        private int mGoodLinkTargetCount;
        private int mGoodLinkTargetIndex;
        private int mGoodLinkTargetRssi;
        private long mLastTimeGood;
        private long mLastTimePoor;
        private long mLastTimeSample;
        private int mRssiBase = WifiWatchdogStateMachine.BSSID_STAT_RANGE_LOW_DBM;
        private int mEntriesSize = 61;
        private VolumeWeightedEMA[] mEntries = new VolumeWeightedEMA[this.mEntriesSize];

        public BssidStatistics(String bssid) {
            this.mBssid = bssid;
            for (int i = 0; i < this.mEntriesSize; i++) {
                this.mEntries[i] = WifiWatchdogStateMachine.this.new VolumeWeightedEMA(0.1d);
            }
        }

        public void updateLoss(int rssi, double value, int volume) {
            int index;
            if (volume > 0 && (index = rssi - this.mRssiBase) >= 0 && index < this.mEntriesSize) {
                this.mEntries[index].update(value, volume);
            }
        }

        public double presetLoss(int rssi) {
            if (rssi <= -90) {
                return 1.0d;
            }
            if (rssi > 0) {
                return 0.0d;
            }
            if (WifiWatchdogStateMachine.sPresetLoss == null) {
                double[] unused = WifiWatchdogStateMachine.sPresetLoss = new double[90];
                for (int i = 0; i < 90; i++) {
                    WifiWatchdogStateMachine.sPresetLoss[i] = 1.0d / Math.pow(90 - i, 1.5d);
                }
            }
            return WifiWatchdogStateMachine.sPresetLoss[-rssi];
        }

        public boolean poorLinkDetected(int rssi) {
            long now = SystemClock.elapsedRealtime();
            long j = now - this.mLastTimeGood;
            long lastPoor = now - this.mLastTimePoor;
            while (this.mGoodLinkTargetIndex > 0 && lastPoor >= WifiWatchdogStateMachine.GOOD_LINK_TARGET[this.mGoodLinkTargetIndex - 1].REDUCE_TIME_MS) {
                this.mGoodLinkTargetIndex--;
            }
            this.mGoodLinkTargetCount = WifiWatchdogStateMachine.GOOD_LINK_TARGET[this.mGoodLinkTargetIndex].SAMPLE_COUNT;
            int from = rssi + 3;
            int to = rssi + 20;
            this.mGoodLinkTargetRssi = findRssiTarget(from, to, 0.1d);
            this.mGoodLinkTargetRssi += WifiWatchdogStateMachine.GOOD_LINK_TARGET[this.mGoodLinkTargetIndex].RSSI_ADJ_DBM;
            if (this.mGoodLinkTargetIndex < WifiWatchdogStateMachine.GOOD_LINK_TARGET.length - 1) {
                this.mGoodLinkTargetIndex++;
            }
            int p = 0;
            int pmax = WifiWatchdogStateMachine.MAX_AVOID_TIME.length - 1;
            while (p < pmax && rssi >= WifiWatchdogStateMachine.MAX_AVOID_TIME[p + 1].MIN_RSSI_DBM) {
                p++;
            }
            long avoidMax = WifiWatchdogStateMachine.MAX_AVOID_TIME[p].TIME_MS;
            if (avoidMax <= 0) {
                return WifiWatchdogStateMachine.DBG;
            }
            this.mBssidAvoidTimeMax = now + avoidMax;
            return true;
        }

        public void newLinkDetected() {
            if (this.mBssidAvoidTimeMax <= 0) {
                this.mGoodLinkTargetRssi = findRssiTarget(WifiWatchdogStateMachine.BSSID_STAT_RANGE_LOW_DBM, WifiWatchdogStateMachine.BSSID_STAT_RANGE_HIGH_DBM, 0.1d);
                this.mGoodLinkTargetCount = 1;
                this.mBssidAvoidTimeMax = SystemClock.elapsedRealtime() + ((long) WifiWatchdogStateMachine.MAX_AVOID_TIME[0].TIME_MS);
            }
        }

        public int findRssiTarget(int from, int to, double threshold) {
            int from2 = from - this.mRssiBase;
            int to2 = to - this.mRssiBase;
            int emptyCount = 0;
            int d = from2 < to2 ? 1 : -1;
            for (int i = from2; i != to2; i += d) {
                if (i >= 0 && i < this.mEntriesSize && this.mEntries[i].mVolume > 1.0d) {
                    emptyCount = 0;
                    if (this.mEntries[i].mValue < threshold) {
                        return this.mRssiBase + i;
                    }
                } else {
                    emptyCount++;
                    if (emptyCount >= 3) {
                        int rssi = this.mRssiBase + i;
                        double lossPreset = presetLoss(rssi);
                        if (lossPreset < threshold) {
                            return rssi;
                        }
                    } else {
                        continue;
                    }
                }
            }
            return this.mRssiBase + to2;
        }
    }
}
