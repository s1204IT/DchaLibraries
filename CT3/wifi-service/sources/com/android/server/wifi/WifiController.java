package com.android.server.wifi;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.WorkSource;
import android.provider.Settings;
import android.util.Slog;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.wifi.WifiServiceImpl;
import com.mediatek.common.MPlugin;
import com.mediatek.common.wifi.IWifiFwkExt;
import java.io.FileDescriptor;
import java.io.PrintWriter;

class WifiController extends StateMachine {
    private static final String ACTION_DEVICE_IDLE = "com.android.server.WifiManager.action.DEVICE_IDLE";
    private static final int BASE = 155648;
    static final int CMD_AIRPLANE_TOGGLED = 155657;
    static final int CMD_AP_START_FAILURE = 155661;
    static final int CMD_AP_STOPPED = 155663;
    static final int CMD_BATTERY_CHANGED = 155652;
    static final int CMD_DEFERRED_TOGGLE = 155659;
    static final int CMD_DEVICE_IDLE = 155653;
    static final int CMD_EMERGENCY_CALL_STATE_CHANGED = 155662;
    static final int CMD_EMERGENCY_MODE_CHANGED = 155649;
    static final int CMD_LOCKS_CHANGED = 155654;
    static final int CMD_SCAN_ALWAYS_MODE_CHANGED = 155655;
    static final int CMD_SCREEN_OFF = 155651;
    static final int CMD_SCREEN_ON = 155650;
    static final int CMD_SET_AP = 155658;
    static final int CMD_STA_START_FAILURE = 155664;
    static final int CMD_USER_PRESENT = 155660;
    static final int CMD_WIFI_TOGGLED = 155656;
    private static final boolean DBG = true;
    private static final long DEFAULT_IDLE_MS = 900000;
    private static final long DEFAULT_REENABLE_DELAY_MS = 500;
    private static final long DEFER_MARGIN_MS = 5;
    public static final int EPDG_UID = 0;
    private static final int IDLE_REQUEST = 0;
    private static final String TAG = "WifiController";
    private AlarmManager mAlarmManager;
    private ApEnabledState mApEnabledState;
    private ApStaDisabledState mApStaDisabledState;
    private Context mContext;
    private DefaultState mDefaultState;
    private DeviceActiveState mDeviceActiveState;
    private boolean mDeviceIdle;
    private DeviceInactiveState mDeviceInactiveState;
    private EcmState mEcmState;
    private FrameworkFacade mFacade;
    private boolean mFirstUserSignOnSeen;
    private FullHighPerfLockHeldState mFullHighPerfLockHeldState;
    private FullLockHeldState mFullLockHeldState;
    private PendingIntent mIdleIntent;
    private long mIdleMillis;
    final WifiServiceImpl.LockList mLocks;
    NetworkInfo mNetworkInfo;
    private NoLockHeldState mNoLockHeldState;
    private int mPluggedType;
    private long mReEnableDelayMillis;
    private ScanOnlyLockHeldState mScanOnlyLockHeldState;
    private boolean mScreenOff;
    final WifiSettingsStore mSettingsStore;
    private int mSleepPolicy;
    private StaDisabledWithScanState mStaDisabledWithScanState;
    private StaEnabledState mStaEnabledState;
    private int mStayAwakeConditions;
    private final WorkSource mTmpWorkSource;
    private IWifiFwkExt mWifiFwkExt;
    private boolean mWifiIpoOff;
    final WifiStateMachine mWifiStateMachine;

    WifiController(Context context, WifiStateMachine wsm, WifiSettingsStore wss, WifiServiceImpl.LockList locks, Looper looper, FrameworkFacade f) {
        super(TAG, looper);
        this.mFirstUserSignOnSeen = false;
        this.mNetworkInfo = new NetworkInfo(1, 0, "WIFI", "");
        this.mTmpWorkSource = new WorkSource();
        this.mDefaultState = new DefaultState();
        this.mStaEnabledState = new StaEnabledState();
        this.mApStaDisabledState = new ApStaDisabledState();
        this.mStaDisabledWithScanState = new StaDisabledWithScanState();
        this.mApEnabledState = new ApEnabledState();
        this.mDeviceActiveState = new DeviceActiveState();
        this.mDeviceInactiveState = new DeviceInactiveState();
        this.mScanOnlyLockHeldState = new ScanOnlyLockHeldState();
        this.mFullLockHeldState = new FullLockHeldState();
        this.mFullHighPerfLockHeldState = new FullHighPerfLockHeldState();
        this.mNoLockHeldState = new NoLockHeldState();
        this.mEcmState = new EcmState();
        this.mWifiIpoOff = false;
        this.mFacade = f;
        this.mContext = context;
        this.mWifiStateMachine = wsm;
        this.mSettingsStore = wss;
        this.mLocks = locks;
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        Intent idleIntent = new Intent(ACTION_DEVICE_IDLE, (Uri) null);
        this.mIdleIntent = this.mFacade.getBroadcast(this.mContext, 0, idleIntent, 0);
        this.mWifiFwkExt = (IWifiFwkExt) MPlugin.createInstance(IWifiFwkExt.class.getName(), this.mContext);
        addState(this.mDefaultState);
        addState(this.mApStaDisabledState, this.mDefaultState);
        addState(this.mStaEnabledState, this.mDefaultState);
        addState(this.mDeviceActiveState, this.mStaEnabledState);
        addState(this.mDeviceInactiveState, this.mStaEnabledState);
        addState(this.mScanOnlyLockHeldState, this.mDeviceInactiveState);
        addState(this.mFullLockHeldState, this.mDeviceInactiveState);
        addState(this.mFullHighPerfLockHeldState, this.mDeviceInactiveState);
        addState(this.mNoLockHeldState, this.mDeviceInactiveState);
        addState(this.mStaDisabledWithScanState, this.mDefaultState);
        addState(this.mApEnabledState, this.mDefaultState);
        addState(this.mEcmState, this.mDefaultState);
        boolean isAirplaneModeOn = this.mSettingsStore.isAirplaneModeOn();
        boolean isWifiEnabled = this.mSettingsStore.isWifiToggleEnabled();
        boolean isScanningAlwaysAvailable = this.mSettingsStore.isScanAlwaysAvailable();
        log("isAirplaneModeOn = " + isAirplaneModeOn + ", isWifiEnabled = " + isWifiEnabled + ", isScanningAvailable = " + isScanningAlwaysAvailable);
        if (isScanningAlwaysAvailable) {
            setInitialState(this.mStaDisabledWithScanState);
        } else {
            setInitialState(this.mApStaDisabledState);
        }
        setLogRecSize(100);
        setLogOnlyTransitions(false);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_DEVICE_IDLE);
        filter.addAction("android.net.wifi.STATE_CHANGE");
        filter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
        filter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if (action.equals(WifiController.ACTION_DEVICE_IDLE)) {
                    WifiController.this.sendMessage(WifiController.CMD_DEVICE_IDLE);
                    return;
                }
                if (action.equals("android.net.wifi.STATE_CHANGE")) {
                    WifiController.this.mNetworkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                    return;
                }
                if (action.equals("android.net.wifi.WIFI_AP_STATE_CHANGED")) {
                    int state = intent.getIntExtra("wifi_state", 14);
                    if (state == 14) {
                        WifiController.this.loge("WifiControllerSoftAP start failed");
                        WifiController.this.sendMessage(WifiController.CMD_AP_START_FAILURE);
                        return;
                    } else {
                        if (state != 11) {
                            return;
                        }
                        WifiController.this.sendMessage(WifiController.CMD_AP_STOPPED);
                        return;
                    }
                }
                if (!action.equals("android.net.wifi.WIFI_STATE_CHANGED") || intent.getIntExtra("wifi_state", 4) != 4) {
                    return;
                }
                WifiController.this.loge("WifiControllerWifi turn on failed");
                WifiController.this.sendMessage(WifiController.CMD_STA_START_FAILURE);
            }
        }, new IntentFilter(filter));
        initializeAndRegisterForSettingsChange(looper);
    }

    private void initializeAndRegisterForSettingsChange(Looper looper) {
        Handler handler = new Handler(looper);
        readStayAwakeConditions();
        registerForStayAwakeModeChange(handler);
        readWifiIdleTime();
        registerForWifiIdleTimeChange(handler);
        if (this.mWifiFwkExt != null) {
            this.mWifiFwkExt.setCustomizedWifiSleepPolicy(this.mContext);
        }
        readWifiSleepPolicy();
        registerForWifiSleepPolicyChange(handler);
        readWifiReEnableDelay();
    }

    private void readStayAwakeConditions() {
        this.mStayAwakeConditions = this.mFacade.getIntegerSetting(this.mContext, "stay_on_while_plugged_in", 0);
    }

    private void readWifiIdleTime() {
        this.mIdleMillis = this.mFacade.getLongSetting(this.mContext, "wifi_idle_ms", DEFAULT_IDLE_MS);
    }

    private void readWifiSleepPolicy() {
        this.mSleepPolicy = this.mFacade.getIntegerSetting(this.mContext, "wifi_sleep_policy", 2);
    }

    private void readWifiReEnableDelay() {
        this.mReEnableDelayMillis = this.mFacade.getLongSetting(this.mContext, "wifi_reenable_delay", DEFAULT_REENABLE_DELAY_MS);
    }

    private void registerForStayAwakeModeChange(Handler handler) {
        ContentObserver contentObserver = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange) {
                WifiController.this.readStayAwakeConditions();
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("stay_on_while_plugged_in"), false, contentObserver);
    }

    private void registerForWifiIdleTimeChange(Handler handler) {
        ContentObserver contentObserver = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange) {
                WifiController.this.readWifiIdleTime();
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("wifi_idle_ms"), false, contentObserver);
    }

    private void registerForWifiSleepPolicyChange(Handler handler) {
        ContentObserver contentObserver = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange) {
                WifiController.this.readWifiSleepPolicy();
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("wifi_sleep_policy"), false, contentObserver);
    }

    private boolean shouldWifiStayAwake(int pluggedType) {
        Slog.d(TAG, "wifiSleepPolicy:" + this.mSleepPolicy + ", means: " + (this.mSleepPolicy == 0 ? "Default, check mStayAwakeConditions" : "") + (this.mSleepPolicy == 1 ? "Never sleep while plugged" : "") + (this.mSleepPolicy == 2 ? "Never sleep" : ""));
        if (this.mSleepPolicy == 2) {
            return true;
        }
        if (this.mSleepPolicy != 1 || pluggedType == 0) {
            return shouldDeviceStayAwake(pluggedType);
        }
        return true;
    }

    private boolean shouldDeviceStayAwake(int pluggedType) {
        Slog.d(TAG, "mStayAwakeConditions: " + this.mStayAwakeConditions + ", pluggedType: " + pluggedType);
        return (this.mStayAwakeConditions & pluggedType) != 0;
    }

    private void updateBatteryWorkSource() {
        this.mTmpWorkSource.clear();
        if (this.mDeviceIdle) {
            this.mLocks.updateWorkSource(this.mTmpWorkSource);
        }
        this.mWifiStateMachine.updateBatteryWorkSource(this.mTmpWorkSource);
    }

    class DefaultState extends State {
        DefaultState() {
        }

        public boolean processMessage(Message msg) {
            Slog.d(WifiController.TAG, getName() + msg.toString() + "\n");
            switch (msg.what) {
                case WifiController.CMD_EMERGENCY_MODE_CHANGED:
                case WifiController.CMD_LOCKS_CHANGED:
                case WifiController.CMD_SCAN_ALWAYS_MODE_CHANGED:
                case WifiController.CMD_WIFI_TOGGLED:
                case WifiController.CMD_AIRPLANE_TOGGLED:
                case WifiController.CMD_SET_AP:
                case WifiController.CMD_AP_START_FAILURE:
                case WifiController.CMD_EMERGENCY_CALL_STATE_CHANGED:
                case WifiController.CMD_AP_STOPPED:
                case WifiController.CMD_STA_START_FAILURE:
                    return true;
                case WifiController.CMD_SCREEN_ON:
                    WifiController.this.mAlarmManager.cancel(WifiController.this.mIdleIntent);
                    WifiController.this.mScreenOff = false;
                    WifiController.this.mDeviceIdle = false;
                    WifiController.this.updateBatteryWorkSource();
                    return true;
                case WifiController.CMD_SCREEN_OFF:
                    WifiController.this.mScreenOff = true;
                    if (!WifiController.this.shouldWifiStayAwake(WifiController.this.mPluggedType)) {
                        if (WifiController.this.mNetworkInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED) {
                            Slog.d(WifiController.TAG, "set idle timer: " + WifiController.this.mIdleMillis + " ms");
                            WifiController.this.mAlarmManager.set(0, System.currentTimeMillis() + WifiController.this.mIdleMillis, WifiController.this.mIdleIntent);
                        } else {
                            WifiController.this.sendMessage(WifiController.CMD_DEVICE_IDLE);
                        }
                    }
                    return true;
                case WifiController.CMD_BATTERY_CHANGED:
                    int pluggedType = msg.arg1;
                    Slog.d(WifiController.TAG, "battery changed pluggedType: " + pluggedType);
                    if (WifiController.this.mScreenOff && WifiController.this.shouldWifiStayAwake(WifiController.this.mPluggedType) && !WifiController.this.shouldWifiStayAwake(pluggedType)) {
                        long triggerTime = System.currentTimeMillis() + WifiController.this.mIdleMillis;
                        Slog.d(WifiController.TAG, "set idle timer for " + WifiController.this.mIdleMillis + "ms");
                        WifiController.this.mAlarmManager.set(0, triggerTime, WifiController.this.mIdleIntent);
                    }
                    WifiController.this.mPluggedType = pluggedType;
                    return true;
                case WifiController.CMD_DEVICE_IDLE:
                    WifiController.this.mDeviceIdle = true;
                    WifiController.this.updateBatteryWorkSource();
                    return true;
                case WifiController.CMD_DEFERRED_TOGGLE:
                    WifiController.this.log("DEFERRED_TOGGLE ignored due to state change");
                    return true;
                case WifiController.CMD_USER_PRESENT:
                    WifiController.this.mFirstUserSignOnSeen = true;
                    return true;
                default:
                    throw new RuntimeException("WifiController.handleMessage " + msg.what);
            }
        }
    }

    class ApStaDisabledState extends State {
        private long mDisabledTimestamp;
        private int mDeferredEnableSerialNumber = 0;
        private boolean mHaveDeferredEnable = false;

        ApStaDisabledState() {
        }

        public void enter() {
            Slog.d(WifiController.TAG, getName() + "\n");
            WifiController.this.mWifiStateMachine.setSupplicantRunning(false);
            this.mDisabledTimestamp = SystemClock.elapsedRealtime();
            this.mDeferredEnableSerialNumber++;
            this.mHaveDeferredEnable = false;
            WifiController.this.mWifiStateMachine.clearANQPCache();
        }

        public boolean processMessage(Message msg) {
            Slog.d(WifiController.TAG, getName() + msg.toString() + "\n");
            switch (msg.what) {
                case WifiController.CMD_SCAN_ALWAYS_MODE_CHANGED:
                    if (WifiController.this.mSettingsStore.isScanAlwaysAvailable()) {
                        WifiController.this.transitionTo(WifiController.this.mStaDisabledWithScanState);
                    }
                    return true;
                case WifiController.CMD_WIFI_TOGGLED:
                case WifiController.CMD_AIRPLANE_TOGGLED:
                    boolean wifiIpoOff = msg.arg1 == 1;
                    boolean ipoStateChange = WifiController.this.mWifiIpoOff != wifiIpoOff;
                    WifiController.this.mWifiIpoOff = wifiIpoOff;
                    if (wifiIpoOff) {
                        Slog.d(WifiController.TAG, "ipooff  don't enable wifi\n");
                    } else if (WifiController.this.mSettingsStore.isWifiToggleEnabled()) {
                        if (doDeferEnable(msg)) {
                            if (this.mHaveDeferredEnable) {
                                this.mDeferredEnableSerialNumber++;
                            }
                            this.mHaveDeferredEnable = this.mHaveDeferredEnable ? false : true;
                        } else if (!WifiController.this.mDeviceIdle) {
                            WifiController.this.transitionTo(WifiController.this.mDeviceActiveState);
                        } else {
                            WifiController.this.checkLocksAndTransitionWhenDeviceIdle();
                        }
                    } else if ((ipoStateChange || msg.what == WifiController.CMD_AIRPLANE_TOGGLED) && WifiController.this.mSettingsStore.isScanAlwaysAvailable() && !WifiController.this.mSettingsStore.isAirplaneModeOn()) {
                        Slog.d(WifiController.TAG, "ipoStateChange = " + ipoStateChange + "isAirplaneModeOn= " + WifiController.this.mSettingsStore.isAirplaneModeOn());
                        WifiController.this.transitionTo(WifiController.this.mStaDisabledWithScanState);
                    }
                    return true;
                case WifiController.CMD_SET_AP:
                    if (msg.arg1 == 1) {
                        if (msg.arg2 == 0) {
                            WifiController.this.mSettingsStore.setWifiSavedState(0);
                        }
                        WifiController.this.mWifiStateMachine.setHostApRunning((WifiConfiguration) msg.obj, true);
                        WifiController.this.transitionTo(WifiController.this.mApEnabledState);
                    }
                    return true;
                case WifiController.CMD_DEFERRED_TOGGLE:
                    if (msg.arg1 != this.mDeferredEnableSerialNumber) {
                        WifiController.this.log("DEFERRED_TOGGLE ignored due to serial mismatch");
                    } else {
                        WifiController.this.log("DEFERRED_TOGGLE handled");
                        WifiController.this.sendMessage((Message) msg.obj);
                    }
                    return true;
                default:
                    return false;
            }
        }

        private boolean doDeferEnable(Message msg) {
            long delaySoFar = SystemClock.elapsedRealtime() - this.mDisabledTimestamp;
            if (delaySoFar >= WifiController.this.mReEnableDelayMillis) {
                return false;
            }
            WifiController.this.log("WifiController msg " + msg + " deferred for " + (WifiController.this.mReEnableDelayMillis - delaySoFar) + "ms");
            Message deferredMsg = WifiController.this.obtainMessage(WifiController.CMD_DEFERRED_TOGGLE);
            deferredMsg.obj = Message.obtain(msg);
            int i = this.mDeferredEnableSerialNumber + 1;
            this.mDeferredEnableSerialNumber = i;
            deferredMsg.arg1 = i;
            WifiController.this.sendMessageDelayed(deferredMsg, (WifiController.this.mReEnableDelayMillis - delaySoFar) + WifiController.DEFER_MARGIN_MS);
            return true;
        }
    }

    class StaEnabledState extends State {
        StaEnabledState() {
        }

        public void enter() {
            Slog.d(WifiController.TAG, getName() + "\n");
            WifiController.this.mWifiStateMachine.setSupplicantRunning(true);
        }

        public boolean processMessage(Message msg) {
            Slog.d(WifiController.TAG, getName() + msg.toString() + "\n");
            switch (msg.what) {
                case WifiController.CMD_EMERGENCY_MODE_CHANGED:
                case WifiController.CMD_EMERGENCY_CALL_STATE_CHANGED:
                    boolean getConfigWiFiDisableInECBM = WifiController.this.mFacade.getConfigWiFiDisableInECBM(WifiController.this.mContext);
                    WifiController.this.log("WifiController msg " + msg + " getConfigWiFiDisableInECBM " + getConfigWiFiDisableInECBM);
                    if (msg.arg1 == 1 && getConfigWiFiDisableInECBM) {
                        WifiController.this.transitionTo(WifiController.this.mEcmState);
                    }
                    return true;
                case WifiController.CMD_WIFI_TOGGLED:
                    boolean wifiIpoOff = msg.arg1 == 1;
                    WifiController.this.mWifiIpoOff = wifiIpoOff;
                    if (wifiIpoOff) {
                        WifiController.this.transitionTo(WifiController.this.mApStaDisabledState);
                    } else if (!WifiController.this.mSettingsStore.isWifiToggleEnabled()) {
                        if (WifiController.this.mSettingsStore.isScanAlwaysAvailable()) {
                            WifiController.this.transitionTo(WifiController.this.mStaDisabledWithScanState);
                        } else {
                            WifiController.this.transitionTo(WifiController.this.mApStaDisabledState);
                        }
                    }
                    return true;
                case WifiController.CMD_AIRPLANE_TOGGLED:
                    if (!WifiController.this.mSettingsStore.isWifiToggleEnabled()) {
                        WifiController.this.transitionTo(WifiController.this.mApStaDisabledState);
                    } else {
                        boolean source = msg.arg1 == 1;
                        if (source) {
                            Slog.d(WifiController.TAG, "setWifiDisabled is delayed. And airplane mode is off now");
                            WifiController.this.transitionTo(WifiController.this.mApStaDisabledState);
                            Slog.d(WifiController.TAG, "goto wifi off and send CMD_AIRPLANE_TOGGLED");
                            WifiController.this.sendMessage(WifiController.CMD_AIRPLANE_TOGGLED);
                        }
                    }
                    return true;
                case WifiController.CMD_SET_AP:
                    if (msg.arg1 == 1) {
                        WifiController.this.mSettingsStore.setWifiSavedState(1);
                        WifiController.this.deferMessage(WifiController.this.obtainMessage(msg.what, msg.arg1, 1, msg.obj));
                        WifiController.this.transitionTo(WifiController.this.mApStaDisabledState);
                    }
                    return true;
                case WifiController.CMD_STA_START_FAILURE:
                    if (!WifiController.this.mSettingsStore.isScanAlwaysAvailable()) {
                        WifiController.this.transitionTo(WifiController.this.mApStaDisabledState);
                    } else {
                        WifiController.this.transitionTo(WifiController.this.mStaDisabledWithScanState);
                    }
                    return true;
                default:
                    return false;
            }
        }
    }

    class StaDisabledWithScanState extends State {
        private long mDisabledTimestamp;
        private int mDeferredEnableSerialNumber = 0;
        private boolean mHaveDeferredEnable = false;

        StaDisabledWithScanState() {
        }

        public void enter() {
            Slog.d(WifiController.TAG, getName() + "\n");
            WifiController.this.mWifiStateMachine.setSupplicantRunning(true);
            WifiController.this.mWifiStateMachine.setOperationalMode(3);
            WifiController.this.mWifiStateMachine.setDriverStart(true);
            this.mDisabledTimestamp = SystemClock.elapsedRealtime();
            this.mDeferredEnableSerialNumber++;
            this.mHaveDeferredEnable = false;
            WifiController.this.mWifiStateMachine.clearANQPCache();
        }

        public boolean processMessage(Message msg) {
            Slog.d(WifiController.TAG, getName() + msg.toString() + "\n");
            switch (msg.what) {
                case WifiController.CMD_SCAN_ALWAYS_MODE_CHANGED:
                    if (!WifiController.this.mSettingsStore.isScanAlwaysAvailable()) {
                        WifiController.this.transitionTo(WifiController.this.mApStaDisabledState);
                    }
                    return true;
                case WifiController.CMD_WIFI_TOGGLED:
                    boolean wifiIpoOff = msg.arg1 == 1;
                    WifiController.this.mWifiIpoOff = wifiIpoOff;
                    if (wifiIpoOff) {
                        WifiController.this.log("WifiIpoOff true disable wifi \n");
                        WifiController.this.transitionTo(WifiController.this.mApStaDisabledState);
                    } else if (WifiController.this.mSettingsStore.isWifiToggleEnabled()) {
                        if (doDeferEnable(msg)) {
                            if (this.mHaveDeferredEnable) {
                                this.mDeferredEnableSerialNumber++;
                            }
                            this.mHaveDeferredEnable = this.mHaveDeferredEnable ? false : true;
                        } else if (!WifiController.this.mDeviceIdle) {
                            WifiController.this.transitionTo(WifiController.this.mDeviceActiveState);
                        } else {
                            WifiController.this.checkLocksAndTransitionWhenDeviceIdle();
                        }
                    }
                    return true;
                case WifiController.CMD_AIRPLANE_TOGGLED:
                    if (WifiController.this.mSettingsStore.isAirplaneModeOn() && !WifiController.this.mSettingsStore.isWifiToggleEnabled()) {
                        WifiController.this.transitionTo(WifiController.this.mApStaDisabledState);
                    }
                    return true;
                case WifiController.CMD_SET_AP:
                    if (msg.arg1 == 1) {
                        WifiController.this.mSettingsStore.setWifiSavedState(0);
                        WifiController.this.deferMessage(WifiController.this.obtainMessage(msg.what, msg.arg1, 1, msg.obj));
                        WifiController.this.transitionTo(WifiController.this.mApStaDisabledState);
                    }
                    return true;
                case WifiController.CMD_DEFERRED_TOGGLE:
                    if (msg.arg1 != this.mDeferredEnableSerialNumber) {
                        WifiController.this.log("DEFERRED_TOGGLE ignored due to serial mismatch");
                    } else {
                        WifiController.this.logd("DEFERRED_TOGGLE handled");
                        WifiController.this.sendMessage((Message) msg.obj);
                    }
                    return true;
                default:
                    return false;
            }
        }

        private boolean doDeferEnable(Message msg) {
            long delaySoFar = SystemClock.elapsedRealtime() - this.mDisabledTimestamp;
            if (delaySoFar >= WifiController.this.mReEnableDelayMillis) {
                return false;
            }
            WifiController.this.log("WifiController msg " + msg + " deferred for " + (WifiController.this.mReEnableDelayMillis - delaySoFar) + "ms");
            Message deferredMsg = WifiController.this.obtainMessage(WifiController.CMD_DEFERRED_TOGGLE);
            deferredMsg.obj = Message.obtain(msg);
            int i = this.mDeferredEnableSerialNumber + 1;
            this.mDeferredEnableSerialNumber = i;
            deferredMsg.arg1 = i;
            WifiController.this.sendMessageDelayed(deferredMsg, (WifiController.this.mReEnableDelayMillis - delaySoFar) + WifiController.DEFER_MARGIN_MS);
            return true;
        }
    }

    class ApEnabledState extends State {
        private State mPendingState = null;

        ApEnabledState() {
        }

        private State getNextWifiState() {
            if (WifiController.this.mSettingsStore.getWifiSavedState() == 1) {
                WifiController.this.mSettingsStore.setWifiSavedState(0);
                return WifiController.this.mDeviceActiveState;
            }
            if (WifiController.this.mSettingsStore.isScanAlwaysAvailable()) {
                return WifiController.this.mStaDisabledWithScanState;
            }
            return WifiController.this.mApStaDisabledState;
        }

        public void enter() {
            Slog.d(WifiController.TAG, getName() + "\n");
        }

        public boolean processMessage(Message msg) {
            Slog.d(WifiController.TAG, getName() + msg.toString() + "\n");
            switch (msg.what) {
                case WifiController.CMD_EMERGENCY_MODE_CHANGED:
                case WifiController.CMD_EMERGENCY_CALL_STATE_CHANGED:
                    if (msg.arg1 == 1) {
                        WifiController.this.mWifiStateMachine.setHostApRunning(null, false);
                        this.mPendingState = WifiController.this.mEcmState;
                    }
                    return true;
                case WifiController.CMD_SCREEN_ON:
                case WifiController.CMD_SCREEN_OFF:
                case WifiController.CMD_BATTERY_CHANGED:
                case WifiController.CMD_DEVICE_IDLE:
                case WifiController.CMD_LOCKS_CHANGED:
                case WifiController.CMD_SCAN_ALWAYS_MODE_CHANGED:
                case WifiController.CMD_DEFERRED_TOGGLE:
                case WifiController.CMD_USER_PRESENT:
                default:
                    return false;
                case WifiController.CMD_WIFI_TOGGLED:
                    if (WifiController.this.mSettingsStore.isWifiToggleEnabled()) {
                        WifiController.this.mWifiStateMachine.setHostApRunning(null, false);
                        this.mPendingState = WifiController.this.mDeviceActiveState;
                    }
                    return true;
                case WifiController.CMD_AIRPLANE_TOGGLED:
                    if (WifiController.this.mSettingsStore.isAirplaneModeOn()) {
                        WifiController.this.mWifiStateMachine.setHostApRunning(null, false);
                        this.mPendingState = WifiController.this.mApStaDisabledState;
                    }
                    return true;
                case WifiController.CMD_SET_AP:
                    if (msg.arg1 == 0) {
                        WifiController.this.mWifiStateMachine.setHostApRunning(null, false);
                        this.mPendingState = getNextWifiState();
                    } else if (msg.arg1 == 1 && this.mPendingState == WifiController.this.mStaEnabledState) {
                        WifiController.this.loge("Defer the message, Wi-Fi and Wi-Fi Hotspot are switching quickly");
                        WifiController.this.deferMessage(WifiController.this.obtainMessage(msg.what, msg.arg1, 1, msg.obj));
                    }
                    return true;
                case WifiController.CMD_AP_START_FAILURE:
                    WifiController.this.transitionTo(getNextWifiState());
                    return true;
                case WifiController.CMD_AP_STOPPED:
                    if (this.mPendingState == null) {
                        this.mPendingState = getNextWifiState();
                    }
                    if (this.mPendingState == WifiController.this.mDeviceActiveState && WifiController.this.mDeviceIdle) {
                        WifiController.this.checkLocksAndTransitionWhenDeviceIdle();
                    } else {
                        WifiController.this.loge("Receive CMD_AP_STOPPED and then ransition to mPendingState = " + this.mPendingState);
                        WifiController.this.transitionTo(this.mPendingState);
                    }
                    return true;
            }
        }
    }

    class EcmState extends State {
        private int mEcmEntryCount;

        EcmState() {
        }

        public void enter() {
            Slog.d(WifiController.TAG, getName() + "\n");
            WifiController.this.mWifiStateMachine.setSupplicantRunning(false);
            WifiController.this.mWifiStateMachine.clearANQPCache();
            this.mEcmEntryCount = 1;
        }

        public boolean processMessage(Message msg) {
            Slog.d(WifiController.TAG, getName() + msg.toString() + "\n");
            if (msg.what == WifiController.CMD_EMERGENCY_CALL_STATE_CHANGED) {
                if (msg.arg1 == 1) {
                    this.mEcmEntryCount++;
                } else if (msg.arg1 == 0) {
                    decrementCountAndReturnToAppropriateState();
                }
                return true;
            }
            if (msg.what != WifiController.CMD_EMERGENCY_MODE_CHANGED) {
                return false;
            }
            if (msg.arg1 == 1) {
                this.mEcmEntryCount++;
            } else if (msg.arg1 == 0) {
                decrementCountAndReturnToAppropriateState();
            }
            return true;
        }

        private void decrementCountAndReturnToAppropriateState() {
            boolean exitEcm = false;
            if (this.mEcmEntryCount == 0) {
                WifiController.this.loge("mEcmEntryCount is 0; exiting Ecm");
                exitEcm = true;
            } else {
                int i = this.mEcmEntryCount - 1;
                this.mEcmEntryCount = i;
                if (i == 0) {
                    exitEcm = true;
                }
            }
            if (!exitEcm) {
                return;
            }
            if (WifiController.this.mSettingsStore.isWifiToggleEnabled()) {
                if (!WifiController.this.mDeviceIdle) {
                    WifiController.this.transitionTo(WifiController.this.mDeviceActiveState);
                    return;
                } else {
                    WifiController.this.checkLocksAndTransitionWhenDeviceIdle();
                    return;
                }
            }
            if (WifiController.this.mSettingsStore.isScanAlwaysAvailable()) {
                WifiController.this.transitionTo(WifiController.this.mStaDisabledWithScanState);
            } else {
                WifiController.this.transitionTo(WifiController.this.mApStaDisabledState);
            }
        }
    }

    class DeviceActiveState extends State {
        DeviceActiveState() {
        }

        public void enter() {
            Slog.d(WifiController.TAG, getName() + "\n");
            WifiController.this.mWifiStateMachine.setOperationalMode(1);
            WifiController.this.mWifiStateMachine.setDriverStart(true);
            WifiController.this.mWifiStateMachine.setHighPerfModeEnabled(false);
        }

        public boolean processMessage(Message msg) {
            Slog.d(WifiController.TAG, getName() + msg.toString() + "\n");
            if (msg.what == WifiController.CMD_DEVICE_IDLE) {
                if (!WifiController.this.mScreenOff) {
                    return true;
                }
                WifiController.this.checkLocksAndTransitionWhenDeviceIdle();
                return false;
            }
            if (msg.what == WifiController.CMD_USER_PRESENT) {
                if (!WifiController.this.mFirstUserSignOnSeen) {
                    WifiController.this.mWifiStateMachine.reloadTlsNetworksAndReconnect();
                }
                WifiController.this.mFirstUserSignOnSeen = true;
                return true;
            }
            return false;
        }
    }

    class DeviceInactiveState extends State {
        DeviceInactiveState() {
        }

        public void enter() {
            Slog.d(WifiController.TAG, getName() + "\n");
        }

        public boolean processMessage(Message msg) {
            Slog.d(WifiController.TAG, getName() + msg.toString() + "\n");
            switch (msg.what) {
                case WifiController.CMD_SCREEN_ON:
                    WifiController.this.transitionTo(WifiController.this.mDeviceActiveState);
                    return false;
                case WifiController.CMD_LOCKS_CHANGED:
                    WifiController.this.checkLocksAndTransitionWhenDeviceIdle();
                    WifiController.this.updateBatteryWorkSource();
                    return true;
                default:
                    return false;
            }
        }
    }

    class ScanOnlyLockHeldState extends State {
        ScanOnlyLockHeldState() {
        }

        public void enter() {
            Slog.d(WifiController.TAG, getName() + "\n");
            WifiController.this.mWifiStateMachine.setOperationalMode(2);
            WifiController.this.mWifiStateMachine.setDriverStart(true);
        }
    }

    class FullLockHeldState extends State {
        FullLockHeldState() {
        }

        public void enter() {
            Slog.d(WifiController.TAG, getName() + "\n");
            WifiController.this.mWifiStateMachine.setOperationalMode(1);
            WifiController.this.mWifiStateMachine.setDriverStart(true);
            WifiController.this.mWifiStateMachine.setHighPerfModeEnabled(false);
        }
    }

    class FullHighPerfLockHeldState extends State {
        FullHighPerfLockHeldState() {
        }

        public void enter() {
            Slog.d(WifiController.TAG, getName() + "\n");
            WifiController.this.mWifiStateMachine.setOperationalMode(1);
            WifiController.this.mWifiStateMachine.setDriverStart(true);
            WifiController.this.mWifiStateMachine.setHighPerfModeEnabled(true);
        }
    }

    class NoLockHeldState extends State {
        NoLockHeldState() {
        }

        public void enter() {
            Slog.d(WifiController.TAG, getName() + "\n");
            WifiController.this.mWifiStateMachine.setDriverStart(false);
        }
    }

    private void checkLocksAndTransitionWhenDeviceIdle() {
        if (this.mLocks.hasLocks()) {
            switch (this.mLocks.getStrongestLockMode()) {
                case 1:
                    transitionTo(this.mFullLockHeldState);
                    break;
                case 2:
                    transitionTo(this.mScanOnlyLockHeldState);
                    break;
                case 3:
                    transitionTo(this.mFullHighPerfLockHeldState);
                    break;
                default:
                    loge("Illegal lock " + this.mLocks.getStrongestLockMode());
                    break;
            }
            return;
        }
        if (this.mSettingsStore.isScanAlwaysAvailable()) {
            transitionTo(this.mScanOnlyLockHeldState);
        } else {
            transitionTo(this.mNoLockHeldState);
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        super.dump(fd, pw, args);
        pw.println("mScreenOff " + this.mScreenOff);
        pw.println("mDeviceIdle " + this.mDeviceIdle);
        pw.println("mPluggedType " + this.mPluggedType);
        pw.println("mIdleMillis " + this.mIdleMillis);
        pw.println("mSleepPolicy " + this.mSleepPolicy);
    }
}
