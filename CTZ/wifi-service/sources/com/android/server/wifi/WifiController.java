package com.android.server.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.WorkSource;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.util.MessageUtils;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.wifi.ClientModeManager;
import com.android.server.wifi.ScanOnlyModeManager;

public class WifiController extends StateMachine {
    private static final int BASE = 155648;
    static final int CMD_AIRPLANE_TOGGLED = 155657;
    static final int CMD_AP_START_FAILURE = 155661;
    static final int CMD_AP_STOPPED = 155663;
    static final int CMD_DEFERRED_TOGGLE = 155659;
    static final int CMD_EMERGENCY_CALL_STATE_CHANGED = 155662;
    static final int CMD_EMERGENCY_MODE_CHANGED = 155649;
    static final int CMD_RECOVERY_DISABLE_WIFI = 155667;
    static final int CMD_RECOVERY_RESTART_WIFI = 155665;
    private static final int CMD_RECOVERY_RESTART_WIFI_CONTINUE = 155666;
    static final int CMD_SCANNING_STOPPED = 155669;
    static final int CMD_SCAN_ALWAYS_MODE_CHANGED = 155655;
    static final int CMD_SET_AP = 155658;
    static final int CMD_STA_START_FAILURE = 155664;
    static final int CMD_STA_STOPPED = 155668;
    static final int CMD_USER_PRESENT = 155660;
    static final int CMD_WIFI_TOGGLED = 155656;
    private static final boolean DBG = false;
    private static final long DEFAULT_REENABLE_DELAY_MS = 500;
    private static final long DEFER_MARGIN_MS = 5;
    private static final String TAG = "WifiController";
    private static final Class[] sMessageClasses = {WifiController.class};
    private static final SparseArray<String> sSmToString = MessageUtils.findMessageNames(sMessageClasses);
    private ClientModeManager.Listener mClientModeCallback;
    private Context mContext;
    private DefaultState mDefaultState;
    private DeviceActiveState mDeviceActiveState;
    private EcmState mEcmState;
    private FrameworkFacade mFacade;
    private boolean mFirstUserSignOnSeen;
    NetworkInfo mNetworkInfo;
    private long mReEnableDelayMillis;
    private ScanOnlyModeManager.Listener mScanOnlyModeCallback;
    private final WifiSettingsStore mSettingsStore;
    private StaDisabledState mStaDisabledState;
    private StaDisabledWithScanState mStaDisabledWithScanState;
    private StaEnabledState mStaEnabledState;
    private final WorkSource mTmpWorkSource;
    private final WifiStateMachine mWifiStateMachine;
    private final Looper mWifiStateMachineLooper;
    private final WifiStateMachinePrime mWifiStateMachinePrime;

    WifiController(Context context, WifiStateMachine wifiStateMachine, Looper looper, WifiSettingsStore wifiSettingsStore, Looper looper2, FrameworkFacade frameworkFacade, WifiStateMachinePrime wifiStateMachinePrime) {
        super(TAG, looper2);
        this.mFirstUserSignOnSeen = DBG;
        this.mNetworkInfo = new NetworkInfo(1, 0, "WIFI", "");
        this.mTmpWorkSource = new WorkSource();
        this.mDefaultState = new DefaultState();
        this.mStaEnabledState = new StaEnabledState();
        this.mStaDisabledState = new StaDisabledState();
        this.mStaDisabledWithScanState = new StaDisabledWithScanState();
        this.mDeviceActiveState = new DeviceActiveState();
        this.mEcmState = new EcmState();
        this.mScanOnlyModeCallback = new ScanOnlyCallback();
        this.mClientModeCallback = new ClientModeCallback();
        this.mFacade = frameworkFacade;
        this.mContext = context;
        this.mWifiStateMachine = wifiStateMachine;
        this.mWifiStateMachineLooper = looper;
        this.mWifiStateMachinePrime = wifiStateMachinePrime;
        this.mSettingsStore = wifiSettingsStore;
        addState(this.mDefaultState);
        addState(this.mStaDisabledState, this.mDefaultState);
        addState(this.mStaEnabledState, this.mDefaultState);
        addState(this.mDeviceActiveState, this.mStaEnabledState);
        addState(this.mStaDisabledWithScanState, this.mDefaultState);
        addState(this.mEcmState, this.mDefaultState);
        log("isAirplaneModeOn = " + this.mSettingsStore.isAirplaneModeOn() + ", isWifiEnabled = " + this.mSettingsStore.isWifiToggleEnabled() + ", isScanningAvailable = " + this.mSettingsStore.isScanAlwaysAvailable() + ", isLocationModeActive = " + (this.mSettingsStore.getLocationModeSetting(this.mContext) == 0));
        if (checkScanOnlyModeAvailable()) {
            setInitialState(this.mStaDisabledWithScanState);
        } else {
            setInitialState(this.mStaDisabledState);
        }
        setLogRecSize(100);
        setLogOnlyTransitions(DBG);
        this.mWifiStateMachinePrime.registerScanOnlyCallback(this.mScanOnlyModeCallback);
        this.mWifiStateMachinePrime.registerClientModeCallback(this.mClientModeCallback);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.wifi.STATE_CHANGE");
        intentFilter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
        intentFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        intentFilter.addAction("android.location.MODE_CHANGED");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if (action.equals("android.net.wifi.STATE_CHANGE")) {
                    WifiController.this.mNetworkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                    return;
                }
                if (!action.equals("android.net.wifi.WIFI_AP_STATE_CHANGED")) {
                    if (action.equals("android.location.MODE_CHANGED")) {
                        WifiController.this.sendMessage(WifiController.CMD_SCAN_ALWAYS_MODE_CHANGED);
                        return;
                    }
                    return;
                }
                int intExtra = intent.getIntExtra("wifi_state", 14);
                if (intExtra == 14) {
                    Log.e(WifiController.TAG, "SoftAP start failed");
                    WifiController.this.sendMessage(WifiController.CMD_AP_START_FAILURE);
                } else if (intExtra == 11) {
                    WifiController.this.sendMessage(WifiController.CMD_AP_STOPPED);
                }
            }
        }, new IntentFilter(intentFilter));
        readWifiReEnableDelay();
    }

    private boolean checkScanOnlyModeAvailable() {
        if (this.mSettingsStore.getLocationModeSetting(this.mContext) == 0) {
            return DBG;
        }
        return this.mSettingsStore.isScanAlwaysAvailable();
    }

    private class ScanOnlyCallback implements ScanOnlyModeManager.Listener {
        private ScanOnlyCallback() {
        }

        @Override
        public void onStateChanged(int i) {
            if (i == 4) {
                Log.d(WifiController.TAG, "ScanOnlyMode unexpected failure: state unknown");
                return;
            }
            if (i == 1) {
                Log.d(WifiController.TAG, "ScanOnlyMode stopped");
                WifiController.this.sendMessage(WifiController.CMD_SCANNING_STOPPED);
            } else {
                if (i == 3) {
                    Log.d(WifiController.TAG, "scan mode active");
                    return;
                }
                Log.d(WifiController.TAG, "unexpected state update: " + i);
            }
        }
    }

    private class ClientModeCallback implements ClientModeManager.Listener {
        private ClientModeCallback() {
        }

        @Override
        public void onStateChanged(int i) {
            if (i == 4) {
                WifiController.this.logd("ClientMode unexpected failure: state unknown");
                WifiController.this.sendMessage(WifiController.CMD_STA_START_FAILURE);
                return;
            }
            if (i == 1) {
                WifiController.this.logd("ClientMode stopped");
                WifiController.this.sendMessage(WifiController.CMD_STA_STOPPED);
            } else {
                if (i == 3) {
                    WifiController.this.logd("client mode active");
                    return;
                }
                WifiController.this.logd("unexpected state update: " + i);
            }
        }
    }

    private void readWifiReEnableDelay() {
        this.mReEnableDelayMillis = this.mFacade.getLongSetting(this.mContext, "wifi_reenable_delay", DEFAULT_REENABLE_DELAY_MS);
    }

    private void updateBatteryWorkSource() {
        this.mTmpWorkSource.clear();
        this.mWifiStateMachine.updateBatteryWorkSource(this.mTmpWorkSource);
    }

    class DefaultState extends State {
        DefaultState() {
        }

        public boolean processMessage(Message message) {
            Log.d(WifiController.TAG, getName() + " " + ((String) WifiController.sSmToString.get(message.what)) + message.toString());
            int i = message.what;
            if (i != WifiController.CMD_EMERGENCY_MODE_CHANGED) {
                switch (i) {
                    case WifiController.CMD_SCAN_ALWAYS_MODE_CHANGED:
                    case WifiController.CMD_WIFI_TOGGLED:
                    case WifiController.CMD_AP_START_FAILURE:
                    case WifiController.CMD_STA_START_FAILURE:
                    case WifiController.CMD_RECOVERY_RESTART_WIFI_CONTINUE:
                    case WifiController.CMD_STA_STOPPED:
                    case WifiController.CMD_SCANNING_STOPPED:
                        break;
                    case WifiController.CMD_AIRPLANE_TOGGLED:
                        if (WifiController.this.mSettingsStore.isAirplaneModeOn()) {
                            WifiController.this.log("Airplane mode toggled, shutdown all modes");
                            WifiController.this.mWifiStateMachinePrime.shutdownWifi();
                            WifiController.this.transitionTo(WifiController.this.mStaDisabledState);
                        } else {
                            WifiController.this.log("Airplane mode disabled, determine next state");
                            if (WifiController.this.mSettingsStore.isWifiToggleEnabled()) {
                                WifiController.this.transitionTo(WifiController.this.mDeviceActiveState);
                            } else if (WifiController.this.checkScanOnlyModeAvailable()) {
                                WifiController.this.transitionTo(WifiController.this.mStaDisabledWithScanState);
                            }
                        }
                        break;
                    case WifiController.CMD_SET_AP:
                        if (WifiController.this.mSettingsStore.isAirplaneModeOn()) {
                            WifiController.this.log("drop softap requests when in airplane mode");
                        } else if (message.arg1 != 1) {
                            WifiController.this.mWifiStateMachinePrime.stopSoftAPMode();
                        } else {
                            WifiController.this.mWifiStateMachinePrime.enterSoftAPMode((SoftApModeConfiguration) message.obj);
                        }
                        break;
                    case WifiController.CMD_DEFERRED_TOGGLE:
                        WifiController.this.log("DEFERRED_TOGGLE ignored due to state change");
                        break;
                    case WifiController.CMD_USER_PRESENT:
                        WifiController.this.mFirstUserSignOnSeen = true;
                        break;
                    case WifiController.CMD_EMERGENCY_CALL_STATE_CHANGED:
                        boolean configWiFiDisableInECBM = WifiController.this.mFacade.getConfigWiFiDisableInECBM(WifiController.this.mContext);
                        WifiController.this.log("WifiController msg " + message + " getConfigWiFiDisableInECBM " + configWiFiDisableInECBM);
                        if (message.arg1 == 1 && configWiFiDisableInECBM) {
                            WifiController.this.transitionTo(WifiController.this.mEcmState);
                        }
                        break;
                    case WifiController.CMD_AP_STOPPED:
                        WifiController.this.log("SoftAp mode disabled, determine next state");
                        if (WifiController.this.mSettingsStore.isWifiToggleEnabled()) {
                            WifiController.this.transitionTo(WifiController.this.mDeviceActiveState);
                        } else if (WifiController.this.checkScanOnlyModeAvailable()) {
                            WifiController.this.transitionTo(WifiController.this.mStaDisabledWithScanState);
                        }
                        break;
                    case WifiController.CMD_RECOVERY_RESTART_WIFI:
                        WifiController.this.deferMessage(WifiController.this.obtainMessage(WifiController.CMD_RECOVERY_RESTART_WIFI_CONTINUE));
                        WifiController.this.mWifiStateMachinePrime.shutdownWifi();
                        WifiController.this.transitionTo(WifiController.this.mStaDisabledState);
                        break;
                    case WifiController.CMD_RECOVERY_DISABLE_WIFI:
                        WifiController.this.log("Recovery has been throttled, disable wifi");
                        WifiController.this.mWifiStateMachinePrime.shutdownWifi();
                        WifiController.this.transitionTo(WifiController.this.mStaDisabledState);
                        break;
                    default:
                        throw new RuntimeException("WifiController.handleMessage " + message.what);
                }
            }
            return true;
        }
    }

    class StaDisabledState extends State {
        private long mDisabledTimestamp;
        private int mDeferredEnableSerialNumber = 0;
        private boolean mHaveDeferredEnable = WifiController.DBG;

        StaDisabledState() {
        }

        public void enter() {
            WifiController.this.log("ApStaDisabledState.enter()");
            WifiController.this.mWifiStateMachinePrime.disableWifi();
            this.mDisabledTimestamp = SystemClock.elapsedRealtime();
            this.mDeferredEnableSerialNumber++;
            this.mHaveDeferredEnable = WifiController.DBG;
            WifiController.this.mWifiStateMachine.clearANQPCache();
        }

        public boolean processMessage(Message message) {
            Log.d(WifiController.TAG, getName() + " " + ((String) WifiController.sSmToString.get(message.what)) + message.toString());
            switch (message.what) {
                case WifiController.CMD_SCAN_ALWAYS_MODE_CHANGED:
                    if (WifiController.this.checkScanOnlyModeAvailable()) {
                        WifiController.this.transitionTo(WifiController.this.mStaDisabledWithScanState);
                    }
                    return true;
                case WifiController.CMD_WIFI_TOGGLED:
                    if (!WifiController.this.mSettingsStore.isWifiToggleEnabled()) {
                        if (WifiController.this.checkScanOnlyModeAvailable() && WifiController.this.mSettingsStore.isAirplaneModeOn()) {
                            WifiController.this.transitionTo(WifiController.this.mStaDisabledWithScanState);
                        }
                    } else if (!doDeferEnable(message)) {
                        WifiController.this.transitionTo(WifiController.this.mDeviceActiveState);
                    } else {
                        if (this.mHaveDeferredEnable) {
                            this.mDeferredEnableSerialNumber++;
                        }
                        this.mHaveDeferredEnable = !this.mHaveDeferredEnable;
                    }
                    return true;
                case WifiController.CMD_SET_AP:
                    if (WifiController.this.mSettingsStore.isAirplaneModeOn()) {
                        WifiController.this.log("drop softap requests when in airplane mode");
                        return true;
                    }
                    if (message.arg1 == 1) {
                        WifiController.this.mSettingsStore.setWifiSavedState(0);
                    }
                    return WifiController.DBG;
                case WifiController.CMD_DEFERRED_TOGGLE:
                    if (message.arg1 != this.mDeferredEnableSerialNumber) {
                        WifiController.this.log("DEFERRED_TOGGLE ignored due to serial mismatch");
                    } else {
                        WifiController.this.log("DEFERRED_TOGGLE handled");
                        WifiController.this.sendMessage((Message) message.obj);
                    }
                    return true;
                case WifiController.CMD_RECOVERY_RESTART_WIFI_CONTINUE:
                    if (WifiController.this.mSettingsStore.isWifiToggleEnabled()) {
                        WifiController.this.transitionTo(WifiController.this.mDeviceActiveState);
                    } else if (WifiController.this.checkScanOnlyModeAvailable()) {
                        WifiController.this.transitionTo(WifiController.this.mStaDisabledWithScanState);
                    }
                    return true;
                default:
                    return WifiController.DBG;
            }
        }

        private boolean doDeferEnable(Message message) {
            long jElapsedRealtime = SystemClock.elapsedRealtime() - this.mDisabledTimestamp;
            if (jElapsedRealtime >= WifiController.this.mReEnableDelayMillis) {
                return WifiController.DBG;
            }
            WifiController.this.log("WifiController msg " + message + " deferred for " + (WifiController.this.mReEnableDelayMillis - jElapsedRealtime) + "ms");
            Message messageObtainMessage = WifiController.this.obtainMessage(WifiController.CMD_DEFERRED_TOGGLE);
            messageObtainMessage.obj = Message.obtain(message);
            int i = this.mDeferredEnableSerialNumber + 1;
            this.mDeferredEnableSerialNumber = i;
            messageObtainMessage.arg1 = i;
            WifiController.this.sendMessageDelayed(messageObtainMessage, (WifiController.this.mReEnableDelayMillis - jElapsedRealtime) + WifiController.DEFER_MARGIN_MS);
            return true;
        }
    }

    class StaEnabledState extends State {
        StaEnabledState() {
        }

        public void enter() {
            WifiController.this.log("StaEnabledState.enter()");
        }

        public boolean processMessage(Message message) {
            Log.d(WifiController.TAG, getName() + " " + ((String) WifiController.sSmToString.get(message.what)) + message.toString());
            switch (message.what) {
                case WifiController.CMD_WIFI_TOGGLED:
                    if (!WifiController.this.mSettingsStore.isWifiToggleEnabled()) {
                        if (WifiController.this.checkScanOnlyModeAvailable()) {
                            WifiController.this.transitionTo(WifiController.this.mStaDisabledWithScanState);
                        } else {
                            WifiController.this.transitionTo(WifiController.this.mStaDisabledState);
                        }
                    }
                    return true;
                case WifiController.CMD_AIRPLANE_TOGGLED:
                    if (WifiController.this.mSettingsStore.isAirplaneModeOn()) {
                        return WifiController.DBG;
                    }
                    WifiController.this.log("airplane mode toggled - and airplane mode is off.  return handled");
                    return true;
                case WifiController.CMD_SET_AP:
                    if (message.arg1 == 1) {
                        WifiController.this.mSettingsStore.setWifiSavedState(1);
                    }
                    return WifiController.DBG;
                case WifiController.CMD_DEFERRED_TOGGLE:
                case WifiController.CMD_USER_PRESENT:
                case WifiController.CMD_EMERGENCY_CALL_STATE_CHANGED:
                case WifiController.CMD_RECOVERY_RESTART_WIFI:
                case WifiController.CMD_RECOVERY_RESTART_WIFI_CONTINUE:
                case WifiController.CMD_RECOVERY_DISABLE_WIFI:
                default:
                    return WifiController.DBG;
                case WifiController.CMD_AP_START_FAILURE:
                case WifiController.CMD_AP_STOPPED:
                    return true;
                case WifiController.CMD_STA_START_FAILURE:
                    if (!WifiController.this.checkScanOnlyModeAvailable()) {
                        WifiController.this.transitionTo(WifiController.this.mStaDisabledState);
                    } else {
                        WifiController.this.transitionTo(WifiController.this.mStaDisabledWithScanState);
                    }
                    return true;
                case WifiController.CMD_STA_STOPPED:
                    WifiController.this.transitionTo(WifiController.this.mStaDisabledState);
                    return true;
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
            WifiController.this.log("StaDisabledWithScanState.enter()");
            WifiController.this.mWifiStateMachinePrime.enterScanOnlyMode();
            this.mDisabledTimestamp = SystemClock.elapsedRealtime();
            this.mDeferredEnableSerialNumber++;
            this.mHaveDeferredEnable = WifiController.DBG;
        }

        public boolean processMessage(Message message) {
            Log.d(WifiController.TAG, getName() + " " + ((String) WifiController.sSmToString.get(message.what)) + message.toString());
            switch (message.what) {
                case WifiController.CMD_SCAN_ALWAYS_MODE_CHANGED:
                    if (!WifiController.this.checkScanOnlyModeAvailable()) {
                        WifiController.this.log("StaDisabledWithScanState: scan no longer available");
                        WifiController.this.transitionTo(WifiController.this.mStaDisabledState);
                    }
                    return true;
                case WifiController.CMD_WIFI_TOGGLED:
                    if (WifiController.this.mSettingsStore.isWifiToggleEnabled()) {
                        if (!doDeferEnable(message)) {
                            WifiController.this.transitionTo(WifiController.this.mDeviceActiveState);
                        } else {
                            if (this.mHaveDeferredEnable) {
                                this.mDeferredEnableSerialNumber++;
                            }
                            this.mHaveDeferredEnable = !this.mHaveDeferredEnable;
                        }
                    }
                    return true;
                case WifiController.CMD_SET_AP:
                    if (message.arg1 == 1) {
                        WifiController.this.mSettingsStore.setWifiSavedState(0);
                    }
                    return WifiController.DBG;
                case WifiController.CMD_DEFERRED_TOGGLE:
                    if (message.arg1 != this.mDeferredEnableSerialNumber) {
                        WifiController.this.log("DEFERRED_TOGGLE ignored due to serial mismatch");
                    } else {
                        WifiController.this.logd("DEFERRED_TOGGLE handled");
                        WifiController.this.sendMessage((Message) message.obj);
                    }
                    return true;
                case WifiController.CMD_AP_START_FAILURE:
                case WifiController.CMD_AP_STOPPED:
                    return true;
                case WifiController.CMD_SCANNING_STOPPED:
                    WifiController.this.log("WifiController: SCANNING_STOPPED when in scan mode -> StaDisabled");
                    WifiController.this.transitionTo(WifiController.this.mStaDisabledState);
                    return true;
                default:
                    return WifiController.DBG;
            }
        }

        private boolean doDeferEnable(Message message) {
            long jElapsedRealtime = SystemClock.elapsedRealtime() - this.mDisabledTimestamp;
            if (jElapsedRealtime >= WifiController.this.mReEnableDelayMillis) {
                return WifiController.DBG;
            }
            WifiController.this.log("WifiController msg " + message + " deferred for " + (WifiController.this.mReEnableDelayMillis - jElapsedRealtime) + "ms");
            Message messageObtainMessage = WifiController.this.obtainMessage(WifiController.CMD_DEFERRED_TOGGLE);
            messageObtainMessage.obj = Message.obtain(message);
            int i = this.mDeferredEnableSerialNumber + 1;
            this.mDeferredEnableSerialNumber = i;
            messageObtainMessage.arg1 = i;
            WifiController.this.sendMessageDelayed(messageObtainMessage, (WifiController.this.mReEnableDelayMillis - jElapsedRealtime) + WifiController.DEFER_MARGIN_MS);
            return true;
        }
    }

    private State getNextWifiState() {
        if (this.mSettingsStore.getWifiSavedState() == 1) {
            return this.mDeviceActiveState;
        }
        if (checkScanOnlyModeAvailable()) {
            return this.mStaDisabledWithScanState;
        }
        return this.mStaDisabledState;
    }

    class EcmState extends State {
        private int mEcmEntryCount;

        EcmState() {
        }

        public void enter() {
            WifiController.this.log("EcmState.enter()");
            WifiController.this.mWifiStateMachinePrime.shutdownWifi();
            WifiController.this.mWifiStateMachine.clearANQPCache();
            this.mEcmEntryCount = 1;
        }

        public boolean processMessage(Message message) {
            Log.d(WifiController.TAG, getName() + " " + ((String) WifiController.sSmToString.get(message.what)) + message.toString());
            if (message.what == WifiController.CMD_EMERGENCY_CALL_STATE_CHANGED) {
                if (message.arg1 == 1) {
                    this.mEcmEntryCount++;
                } else if (message.arg1 == 0) {
                    decrementCountAndReturnToAppropriateState();
                }
                return true;
            }
            if (message.what == WifiController.CMD_EMERGENCY_MODE_CHANGED) {
                if (message.arg1 == 1) {
                    this.mEcmEntryCount++;
                } else if (message.arg1 == 0) {
                    decrementCountAndReturnToAppropriateState();
                }
                return true;
            }
            if (message.what == WifiController.CMD_RECOVERY_RESTART_WIFI || message.what == WifiController.CMD_RECOVERY_DISABLE_WIFI || message.what == WifiController.CMD_AP_STOPPED || message.what == WifiController.CMD_SCANNING_STOPPED || message.what == WifiController.CMD_STA_STOPPED || message.what == WifiController.CMD_SET_AP) {
                return true;
            }
            return WifiController.DBG;
        }

        private void decrementCountAndReturnToAppropriateState() {
            boolean z = true;
            if (this.mEcmEntryCount == 0) {
                WifiController.this.loge("mEcmEntryCount is 0; exiting Ecm");
            } else {
                int i = this.mEcmEntryCount - 1;
                this.mEcmEntryCount = i;
                if (i != 0) {
                    z = WifiController.DBG;
                }
            }
            if (z) {
                if (WifiController.this.mSettingsStore.isWifiToggleEnabled()) {
                    WifiController.this.transitionTo(WifiController.this.mDeviceActiveState);
                } else if (WifiController.this.checkScanOnlyModeAvailable()) {
                    WifiController.this.transitionTo(WifiController.this.mStaDisabledWithScanState);
                } else {
                    WifiController.this.transitionTo(WifiController.this.mStaDisabledState);
                }
            }
        }
    }

    class DeviceActiveState extends State {
        DeviceActiveState() {
        }

        public void enter() {
            WifiController.this.log("DeviceActiveState.enter()");
            WifiController.this.mWifiStateMachinePrime.enterClientMode();
            WifiController.this.mWifiStateMachine.setHighPerfModeEnabled(WifiController.DBG);
        }

        public boolean processMessage(Message message) {
            final String str;
            final String str2;
            Log.d(WifiController.TAG, getName() + " " + ((String) WifiController.sSmToString.get(message.what)) + message.toString());
            if (message.what == WifiController.CMD_USER_PRESENT) {
                if (!WifiController.this.mFirstUserSignOnSeen) {
                    WifiController.this.mWifiStateMachine.reloadTlsNetworksAndReconnect();
                }
                WifiController.this.mFirstUserSignOnSeen = true;
                return true;
            }
            if (message.what != WifiController.CMD_RECOVERY_RESTART_WIFI) {
                return WifiController.DBG;
            }
            if (message.arg1 < SelfRecovery.REASON_STRINGS.length && message.arg1 >= 0) {
                str = SelfRecovery.REASON_STRINGS[message.arg1];
                str2 = "Wi-Fi BugReport: " + str;
            } else {
                str = "";
                str2 = "Wi-Fi BugReport";
            }
            if (message.arg1 != 0) {
                new Handler(WifiController.this.mWifiStateMachineLooper).post(new Runnable() {
                    @Override
                    public final void run() {
                        WifiController.this.mWifiStateMachine.takeBugReport(str2, str);
                    }
                });
            }
            return WifiController.DBG;
        }
    }
}
