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
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.wifi.WifiServiceImpl;
import java.io.FileDescriptor;
import java.io.PrintWriter;

class WifiController extends StateMachine {
    private static final String ACTION_DEVICE_IDLE = "com.android.server.WifiManager.action.DEVICE_IDLE";
    private static final int BASE = 155648;
    static final int CMD_AIRPLANE_TOGGLED = 155657;
    static final int CMD_BATTERY_CHANGED = 155652;
    static final int CMD_DEFERRED_TOGGLE = 155659;
    static final int CMD_DEVICE_IDLE = 155653;
    static final int CMD_EMERGENCY_MODE_CHANGED = 155649;
    static final int CMD_LOCKS_CHANGED = 155654;
    static final int CMD_SCAN_ALWAYS_MODE_CHANGED = 155655;
    static final int CMD_SCREEN_OFF = 155651;
    static final int CMD_SCREEN_ON = 155650;
    static final int CMD_SET_AP = 155658;
    static final int CMD_USER_PRESENT = 155660;
    static final int CMD_WIFI_TOGGLED = 155656;
    private static final boolean DBG = false;
    private static final long DEFAULT_IDLE_MS = 900000;
    private static final long DEFAULT_REENABLE_DELAY_MS = 500;
    private static final long DEFER_MARGIN_MS = 5;
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
    final WifiStateMachine mWifiStateMachine;

    WifiController(Context context, WifiServiceImpl service, Looper looper) {
        super(TAG, looper);
        this.mFirstUserSignOnSeen = DBG;
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
        this.mContext = context;
        this.mWifiStateMachine = service.mWifiStateMachine;
        this.mSettingsStore = service.mSettingsStore;
        this.mLocks = service.mLocks;
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        Intent idleIntent = new Intent(ACTION_DEVICE_IDLE, (Uri) null);
        this.mIdleIntent = PendingIntent.getBroadcast(this.mContext, 0, idleIntent, 0);
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
        setLogOnlyTransitions(DBG);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_DEVICE_IDLE);
        filter.addAction("android.net.wifi.STATE_CHANGE");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if (action.equals(WifiController.ACTION_DEVICE_IDLE)) {
                    WifiController.this.sendMessage(WifiController.CMD_DEVICE_IDLE);
                } else if (action.equals("android.net.wifi.STATE_CHANGE")) {
                    WifiController.this.mNetworkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                }
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
        readWifiSleepPolicy();
        registerForWifiSleepPolicyChange(handler);
        readWifiReEnableDelay();
    }

    private void readStayAwakeConditions() {
        this.mStayAwakeConditions = Settings.Global.getInt(this.mContext.getContentResolver(), "stay_on_while_plugged_in", 0);
    }

    private void readWifiIdleTime() {
        this.mIdleMillis = Settings.Global.getLong(this.mContext.getContentResolver(), "wifi_idle_ms", DEFAULT_IDLE_MS);
    }

    private void readWifiSleepPolicy() {
        this.mSleepPolicy = Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_sleep_policy", 2);
    }

    private void readWifiReEnableDelay() {
        this.mReEnableDelayMillis = Settings.Global.getLong(this.mContext.getContentResolver(), "wifi_reenable_delay", DEFAULT_REENABLE_DELAY_MS);
    }

    private void registerForStayAwakeModeChange(Handler handler) {
        ContentObserver contentObserver = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange) {
                WifiController.this.readStayAwakeConditions();
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("stay_on_while_plugged_in"), DBG, contentObserver);
    }

    private void registerForWifiIdleTimeChange(Handler handler) {
        ContentObserver contentObserver = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange) {
                WifiController.this.readWifiIdleTime();
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("wifi_idle_ms"), DBG, contentObserver);
    }

    private void registerForWifiSleepPolicyChange(Handler handler) {
        ContentObserver contentObserver = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange) {
                WifiController.this.readWifiSleepPolicy();
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("wifi_sleep_policy"), DBG, contentObserver);
    }

    private boolean shouldWifiStayAwake(int pluggedType) {
        if (this.mSleepPolicy == 2) {
            return true;
        }
        if (this.mSleepPolicy != 1 || pluggedType == 0) {
            return shouldDeviceStayAwake(pluggedType);
        }
        return true;
    }

    private boolean shouldDeviceStayAwake(int pluggedType) {
        if ((this.mStayAwakeConditions & pluggedType) != 0) {
            return true;
        }
        return DBG;
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
            switch (msg.what) {
                case WifiController.CMD_EMERGENCY_MODE_CHANGED:
                case WifiController.CMD_LOCKS_CHANGED:
                case WifiController.CMD_SCAN_ALWAYS_MODE_CHANGED:
                case WifiController.CMD_WIFI_TOGGLED:
                case WifiController.CMD_AIRPLANE_TOGGLED:
                case WifiController.CMD_SET_AP:
                    return true;
                case WifiController.CMD_SCREEN_ON:
                    WifiController.this.mAlarmManager.cancel(WifiController.this.mIdleIntent);
                    WifiController.this.mScreenOff = WifiController.DBG;
                    WifiController.this.mDeviceIdle = WifiController.DBG;
                    WifiController.this.updateBatteryWorkSource();
                    return true;
                case WifiController.CMD_SCREEN_OFF:
                    WifiController.this.mScreenOff = true;
                    if (!WifiController.this.shouldWifiStayAwake(WifiController.this.mPluggedType)) {
                        if (WifiController.this.mNetworkInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED) {
                            WifiController.this.mAlarmManager.set(0, System.currentTimeMillis() + WifiController.this.mIdleMillis, WifiController.this.mIdleIntent);
                        } else {
                            WifiController.this.sendMessage(WifiController.CMD_DEVICE_IDLE);
                        }
                    }
                    return true;
                case WifiController.CMD_BATTERY_CHANGED:
                    int pluggedType = msg.arg1;
                    if (WifiController.this.mScreenOff && WifiController.this.shouldWifiStayAwake(WifiController.this.mPluggedType) && !WifiController.this.shouldWifiStayAwake(pluggedType)) {
                        long triggerTime = System.currentTimeMillis() + WifiController.this.mIdleMillis;
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
        private boolean mHaveDeferredEnable = WifiController.DBG;

        ApStaDisabledState() {
        }

        public void enter() {
            WifiController.this.mWifiStateMachine.setSupplicantRunning(WifiController.DBG);
            this.mDisabledTimestamp = SystemClock.elapsedRealtime();
            this.mDeferredEnableSerialNumber++;
            this.mHaveDeferredEnable = WifiController.DBG;
        }

        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case WifiController.CMD_SCAN_ALWAYS_MODE_CHANGED:
                    if (WifiController.this.mSettingsStore.isScanAlwaysAvailable()) {
                        WifiController.this.transitionTo(WifiController.this.mStaDisabledWithScanState);
                    }
                    break;
                case WifiController.CMD_WIFI_TOGGLED:
                case WifiController.CMD_AIRPLANE_TOGGLED:
                    if (WifiController.this.mSettingsStore.isWifiToggleEnabled()) {
                        if (!WifiController.this.mDeviceIdle) {
                            WifiController.this.transitionTo(WifiController.this.mDeviceActiveState);
                        } else {
                            WifiController.this.checkLocksAndTransitionWhenDeviceIdle();
                        }
                    } else if (WifiController.this.mSettingsStore.isScanAlwaysAvailable()) {
                        WifiController.this.transitionTo(WifiController.this.mStaDisabledWithScanState);
                    }
                    break;
                case WifiController.CMD_SET_AP:
                    if (msg.arg1 == 1) {
                        WifiController.this.mWifiStateMachine.setHostApRunning((WifiConfiguration) msg.obj, true);
                        WifiController.this.transitionTo(WifiController.this.mApEnabledState);
                    }
                    break;
                case WifiController.CMD_DEFERRED_TOGGLE:
                    if (msg.arg1 != this.mDeferredEnableSerialNumber) {
                        WifiController.this.log("DEFERRED_TOGGLE ignored due to serial mismatch");
                    } else {
                        WifiController.this.log("DEFERRED_TOGGLE handled");
                        WifiController.this.sendMessage((Message) msg.obj);
                    }
                    break;
                default:
                    return WifiController.DBG;
            }
            return true;
        }

        private boolean doDeferEnable(Message msg) {
            long delaySoFar = SystemClock.elapsedRealtime() - this.mDisabledTimestamp;
            if (delaySoFar < WifiController.this.mReEnableDelayMillis) {
                WifiController.this.log("WifiController msg " + msg + " deferred for " + (WifiController.this.mReEnableDelayMillis - delaySoFar) + "ms");
                Message deferredMsg = WifiController.this.obtainMessage(WifiController.CMD_DEFERRED_TOGGLE);
                deferredMsg.obj = Message.obtain(msg);
                int i = this.mDeferredEnableSerialNumber + 1;
                this.mDeferredEnableSerialNumber = i;
                deferredMsg.arg1 = i;
                WifiController.this.sendMessageDelayed(deferredMsg, (WifiController.this.mReEnableDelayMillis - delaySoFar) + WifiController.DEFER_MARGIN_MS);
                return true;
            }
            return WifiController.DBG;
        }
    }

    class StaEnabledState extends State {
        StaEnabledState() {
        }

        public void enter() {
            WifiController.this.mWifiStateMachine.setSupplicantRunning(true);
        }

        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case WifiController.CMD_EMERGENCY_MODE_CHANGED:
                    if (msg.arg1 == 1) {
                        WifiController.this.transitionTo(WifiController.this.mEcmState);
                        return true;
                    }
                    return WifiController.DBG;
                case WifiController.CMD_WIFI_TOGGLED:
                    if (!WifiController.this.mSettingsStore.isWifiToggleEnabled()) {
                        if (WifiController.this.mSettingsStore.isScanAlwaysAvailable()) {
                            WifiController.this.transitionTo(WifiController.this.mStaDisabledWithScanState);
                            return true;
                        }
                        WifiController.this.transitionTo(WifiController.this.mApStaDisabledState);
                        return true;
                    }
                    if (WifiController.this.mWifiStateMachine.syncGetWifiState() != 1) {
                        return true;
                    }
                    WifiController.this.deferMessage(msg);
                    WifiController.this.transitionTo(WifiController.this.mApStaDisabledState);
                    return true;
                case WifiController.CMD_AIRPLANE_TOGGLED:
                    if (WifiController.this.mSettingsStore.isWifiToggleEnabled()) {
                        return true;
                    }
                    WifiController.this.transitionTo(WifiController.this.mApStaDisabledState);
                    return true;
                default:
                    return WifiController.DBG;
            }
        }
    }

    class StaDisabledWithScanState extends State {
        private long mDisabledTimestamp;
        private int mDeferredEnableSerialNumber = 0;
        private boolean mHaveDeferredEnable = WifiController.DBG;

        StaDisabledWithScanState() {
        }

        public void enter() {
            WifiController.this.mWifiStateMachine.setSupplicantRunning(true);
            WifiController.this.mWifiStateMachine.setOperationalMode(3);
            WifiController.this.mWifiStateMachine.setDriverStart(true);
            this.mDisabledTimestamp = SystemClock.elapsedRealtime();
            this.mDeferredEnableSerialNumber++;
            this.mHaveDeferredEnable = WifiController.DBG;
        }

        public boolean processMessage(Message msg) {
            boolean z = WifiController.DBG;
            switch (msg.what) {
                case WifiController.CMD_SCAN_ALWAYS_MODE_CHANGED:
                    if (!WifiController.this.mSettingsStore.isScanAlwaysAvailable()) {
                        WifiController.this.transitionTo(WifiController.this.mApStaDisabledState);
                    }
                    return true;
                case WifiController.CMD_WIFI_TOGGLED:
                    if (WifiController.this.mSettingsStore.isWifiToggleEnabled()) {
                        if (!doDeferEnable(msg)) {
                            if (!WifiController.this.mDeviceIdle) {
                                WifiController.this.transitionTo(WifiController.this.mDeviceActiveState);
                            } else {
                                WifiController.this.checkLocksAndTransitionWhenDeviceIdle();
                            }
                        } else {
                            if (this.mHaveDeferredEnable) {
                                this.mDeferredEnableSerialNumber++;
                            }
                            if (!this.mHaveDeferredEnable) {
                                z = true;
                            }
                            this.mHaveDeferredEnable = z;
                        }
                    }
                    return true;
                case WifiController.CMD_AIRPLANE_TOGGLED:
                    if (WifiController.this.mSettingsStore.isAirplaneModeOn() && !WifiController.this.mSettingsStore.isWifiToggleEnabled()) {
                        WifiController.this.transitionTo(WifiController.this.mApStaDisabledState);
                    }
                    if (!WifiController.this.mSettingsStore.isScanAlwaysAvailable()) {
                    }
                    return true;
                case WifiController.CMD_SET_AP:
                    if (msg.arg1 == 1) {
                        WifiController.this.deferMessage(msg);
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
                    return WifiController.DBG;
            }
        }

        private boolean doDeferEnable(Message msg) {
            long delaySoFar = SystemClock.elapsedRealtime() - this.mDisabledTimestamp;
            if (delaySoFar < WifiController.this.mReEnableDelayMillis) {
                WifiController.this.log("WifiController msg " + msg + " deferred for " + (WifiController.this.mReEnableDelayMillis - delaySoFar) + "ms");
                Message deferredMsg = WifiController.this.obtainMessage(WifiController.CMD_DEFERRED_TOGGLE);
                deferredMsg.obj = Message.obtain(msg);
                int i = this.mDeferredEnableSerialNumber + 1;
                this.mDeferredEnableSerialNumber = i;
                deferredMsg.arg1 = i;
                WifiController.this.sendMessageDelayed(deferredMsg, (WifiController.this.mReEnableDelayMillis - delaySoFar) + WifiController.DEFER_MARGIN_MS);
                return true;
            }
            return WifiController.DBG;
        }
    }

    class ApEnabledState extends State {
        ApEnabledState() {
        }

        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case WifiController.CMD_AIRPLANE_TOGGLED:
                    if (WifiController.this.mSettingsStore.isAirplaneModeOn()) {
                        WifiController.this.mWifiStateMachine.setHostApRunning(null, WifiController.DBG);
                        WifiController.this.transitionTo(WifiController.this.mApStaDisabledState);
                    }
                    return true;
                case WifiController.CMD_SET_AP:
                    if (msg.arg1 == 0) {
                        WifiController.this.mWifiStateMachine.setHostApRunning(null, WifiController.DBG);
                        WifiController.this.transitionTo(WifiController.this.mApStaDisabledState);
                    }
                    return true;
                default:
                    return WifiController.DBG;
            }
        }
    }

    class EcmState extends State {
        EcmState() {
        }

        public void enter() {
            WifiController.this.mWifiStateMachine.setSupplicantRunning(WifiController.DBG);
        }

        public boolean processMessage(Message msg) {
            if (msg.what == WifiController.CMD_EMERGENCY_MODE_CHANGED && msg.arg1 == 0) {
                if (WifiController.this.mSettingsStore.isWifiToggleEnabled()) {
                    if (!WifiController.this.mDeviceIdle) {
                        WifiController.this.transitionTo(WifiController.this.mDeviceActiveState);
                    } else {
                        WifiController.this.checkLocksAndTransitionWhenDeviceIdle();
                    }
                } else if (WifiController.this.mSettingsStore.isScanAlwaysAvailable()) {
                    WifiController.this.transitionTo(WifiController.this.mStaDisabledWithScanState);
                } else {
                    WifiController.this.transitionTo(WifiController.this.mApStaDisabledState);
                }
                return true;
            }
            return WifiController.DBG;
        }
    }

    class DeviceActiveState extends State {
        DeviceActiveState() {
        }

        public void enter() {
            WifiController.this.mWifiStateMachine.setOperationalMode(1);
            WifiController.this.mWifiStateMachine.setDriverStart(true);
            WifiController.this.mWifiStateMachine.setHighPerfModeEnabled(WifiController.DBG);
        }

        public boolean processMessage(Message msg) {
            if (msg.what == WifiController.CMD_DEVICE_IDLE) {
                WifiController.this.checkLocksAndTransitionWhenDeviceIdle();
            } else if (msg.what == WifiController.CMD_USER_PRESENT) {
                if (!WifiController.this.mFirstUserSignOnSeen) {
                    WifiController.this.mWifiStateMachine.reloadTlsNetworksAndReconnect();
                }
                WifiController.this.mFirstUserSignOnSeen = true;
                return true;
            }
            return WifiController.DBG;
        }
    }

    class DeviceInactiveState extends State {
        DeviceInactiveState() {
        }

        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case WifiController.CMD_SCREEN_ON:
                    WifiController.this.transitionTo(WifiController.this.mDeviceActiveState);
                    return WifiController.DBG;
                case WifiController.CMD_LOCKS_CHANGED:
                    WifiController.this.checkLocksAndTransitionWhenDeviceIdle();
                    WifiController.this.updateBatteryWorkSource();
                    return true;
                default:
                    return WifiController.DBG;
            }
        }
    }

    class ScanOnlyLockHeldState extends State {
        ScanOnlyLockHeldState() {
        }

        public void enter() {
            WifiController.this.mWifiStateMachine.setOperationalMode(2);
            WifiController.this.mWifiStateMachine.setDriverStart(true);
        }
    }

    class FullLockHeldState extends State {
        FullLockHeldState() {
        }

        public void enter() {
            WifiController.this.mWifiStateMachine.setOperationalMode(1);
            WifiController.this.mWifiStateMachine.setDriverStart(true);
            WifiController.this.mWifiStateMachine.setHighPerfModeEnabled(WifiController.DBG);
        }
    }

    class FullHighPerfLockHeldState extends State {
        FullHighPerfLockHeldState() {
        }

        public void enter() {
            WifiController.this.mWifiStateMachine.setOperationalMode(1);
            WifiController.this.mWifiStateMachine.setDriverStart(true);
            WifiController.this.mWifiStateMachine.setHighPerfModeEnabled(true);
        }
    }

    class NoLockHeldState extends State {
        NoLockHeldState() {
        }

        public void enter() {
            WifiController.this.mWifiStateMachine.setDriverStart(WifiController.DBG);
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
