package com.mediatek.datashaping;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.usage.UsageStatsManagerInternal;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Slog;
import android.view.IInputFilter;
import android.view.InputEvent;
import android.view.InputFilter;
import android.view.KeyEvent;
import android.view.WindowManagerInternal;
import com.android.server.LocalServices;
import com.android.server.input.InputManagerService;
import com.mediatek.datashaping.IDataShapingManager;

public class DataShapingServiceImpl extends IDataShapingManager.Stub {
    public static final long ALARM_MANAGER_OPEN_GATE_INTERVAL = 300000;
    private static final String CLOSE_TIME_EXPIRED_ACTION = "com.mediatek.datashaping.CLOSE_TIME_EXPIRED";
    public static final int DATA_SHAPING_STATE_CLOSE = 3;
    public static final int DATA_SHAPING_STATE_OPEN = 2;
    public static final int DATA_SHAPING_STATE_OPEN_LOCKED = 1;
    public static final long GATE_CLOSE_EXPIRED_TIME = 300000;
    public static final int GATE_CLOSE_SAFE_TIMER = 600000;
    private static final int MSG_ALARM_MANAGER_TRIGGER = 14;
    private static final int MSG_APPSTANDBY_CHANGED = 22;
    private static final int MSG_BT_AP_STATE_CHANGED = 19;
    private static final int MSG_CHECK_USER_PREFERENCE = 1;
    private static final int MSG_CONNECTIVITY_CHANGED = 20;
    private static final int MSG_DEVICEIDLE_CHANGED = 21;
    private static final int MSG_GATE_CLOSE_TIMER_EXPIRED = 17;
    private static final int MSG_HEADSETHOOK_CHANGED = 18;
    private static final int MSG_INIT = 2;
    private static final int MSG_INPUTFILTER_STATE_CHANGED = 23;
    private static final int MSG_LTE_AS_STATE_CHANGED = 15;
    private static final int MSG_NETWORK_TYPE_CHANGED = 11;
    private static final int MSG_SCREEN_STATE_CHANGED = 10;
    private static final int MSG_SHARED_DEFAULT_APN_STATE_CHANGED = 16;
    private static final int MSG_STOP = 3;
    private static final int MSG_USB_STATE_CHANGED = 13;
    private static final int MSG_WIFI_AP_STATE_CHANGED = 12;
    private static final int WAKE_LOCK_TIMEOUT = 30000;
    private BroadcastReceiver mBroadcastReceiver;
    private Context mContext;
    private DataShapingState mCurrentState;
    private boolean mDataShapingEnabled;
    private DataShapingHandler mDataShapingHandler;
    private DataShapingUtils mDataShapingUtils;
    private DataShapingState mGateCloseState;
    private DataShapingState mGateOpenLockedState;
    private DataShapingState mGateOpenState;
    private HandlerThread mHandlerThread;
    private DataShapingInputFilter mInputFilter;
    private InputManagerService mInputManagerService;
    private long mLastAlarmTriggerSuccessTime;
    private PendingIntent mPendingIntent;
    private PowerManager.WakeLock mWakelock;
    private WindowManagerInternal mWindowManagerService;
    private final String TAG = "DataShapingService";
    private boolean mRegisterInput = false;
    private final Object mLock = new Object();
    private ContentObserver mSettingsObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            boolean dataShapingEnabled = Settings.System.getInt(DataShapingServiceImpl.this.mContext.getContentResolver(), "background_power_saving_enable", 0) != 0;
            if ("0".equals(SystemProperties.get("persist.datashaping.enable", "-1"))) {
                dataShapingEnabled = false;
                Slog.d("DataShapingService", "persist.datashaping.enable is false");
            } else if ("1".equals(SystemProperties.get("persist.datashaping.enable", "-1"))) {
                dataShapingEnabled = true;
                Slog.d("DataShapingService", "persist.datashaping.enable is true");
            }
            if (dataShapingEnabled == DataShapingServiceImpl.this.mDataShapingEnabled) {
                return;
            }
            if (dataShapingEnabled) {
                Slog.d("DataShapingService", "data shaping enabled, start handler thread!");
                DataShapingServiceImpl.this.mHandlerThread = new HandlerThread("DataShapingService");
                DataShapingServiceImpl.this.mHandlerThread.start();
                DataShapingServiceImpl.this.mDataShapingHandler = DataShapingServiceImpl.this.new DataShapingHandler(DataShapingServiceImpl.this.mHandlerThread.getLooper());
                DataShapingServiceImpl.this.mDataShapingHandler.sendEmptyMessage(2);
                DataShapingServiceImpl.this.setCurrentState(1);
                DataShapingServiceImpl.this.registerReceiver();
            } else {
                if (DataShapingServiceImpl.this.mBroadcastReceiver != null) {
                    DataShapingServiceImpl.this.mContext.unregisterReceiver(DataShapingServiceImpl.this.mBroadcastReceiver);
                }
                if (DataShapingServiceImpl.this.mHandlerThread != null) {
                    DataShapingServiceImpl.this.mHandlerThread.quitSafely();
                }
                DataShapingServiceImpl.this.mDataShapingUtils.reset();
                DataShapingServiceImpl.this.reset();
                Slog.d("DataShapingService", "data shaping disabled, stop handler thread and reset!");
            }
            DataShapingServiceImpl.this.mDataShapingEnabled = dataShapingEnabled;
        }
    };
    private UsageStatsManagerInternal mUsageStats = (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class);

    public DataShapingServiceImpl(Context context) {
        this.mContext = context;
        this.mDataShapingUtils = DataShapingUtils.getInstance(this.mContext);
    }

    public void registerReceiver() {
        AppIdleStateChangeListener appIdleStateChangeListener = null;
        Slog.d("DataShapingService", "registerReceiver start");
        if (this.mBroadcastReceiver == null) {
            this.mBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    Slog.d("DataShapingService", "received broadcast, action is: " + action);
                    if ("android.intent.action.SCREEN_ON" == action) {
                        DataShapingServiceImpl.this.mDataShapingHandler.obtainMessage(10, true).sendToTarget();
                        return;
                    }
                    if ("android.intent.action.SCREEN_OFF" == action) {
                        DataShapingServiceImpl.this.mDataShapingHandler.obtainMessage(10, false).sendToTarget();
                        return;
                    }
                    if ("mediatek.intent.action.PS_NETWORK_TYPE_CHANGED" == action) {
                        DataShapingServiceImpl.this.mDataShapingUtils.setCurrentNetworkType(intent);
                        DataShapingServiceImpl.this.mDataShapingHandler.obtainMessage(11, intent).sendToTarget();
                        return;
                    }
                    if ("android.net.conn.CONNECTIVITY_CHANGE" == action) {
                        DataShapingServiceImpl.this.mDataShapingHandler.obtainMessage(20, intent).sendToTarget();
                        return;
                    }
                    if ("android.net.wifi.WIFI_AP_STATE_CHANGED" == action) {
                        DataShapingServiceImpl.this.mDataShapingHandler.obtainMessage(12, intent).sendToTarget();
                        return;
                    }
                    if ("android.hardware.usb.action.USB_STATE" == action) {
                        DataShapingServiceImpl.this.mDataShapingHandler.obtainMessage(13, intent).sendToTarget();
                        return;
                    }
                    if ("mediatek.intent.action.LTE_ACCESS_STRATUM_STATE_CHANGED" == action) {
                        DataShapingServiceImpl.this.mDataShapingHandler.obtainMessage(15, intent).sendToTarget();
                        return;
                    }
                    if ("mediatek.intent.action.SHARED_DEFAULT_APN_STATE_CHANGED" == action) {
                        DataShapingServiceImpl.this.mDataShapingHandler.obtainMessage(16, intent).sendToTarget();
                        return;
                    }
                    if (DataShapingServiceImpl.CLOSE_TIME_EXPIRED_ACTION == action) {
                        DataShapingServiceImpl.this.getWakeLock();
                        DataShapingServiceImpl.this.mDataShapingHandler.obtainMessage(17).sendToTarget();
                    } else {
                        if (!"android.bluetooth.device.action.ACL_CONNECTED".equals(action) && !"android.bluetooth.device.action.ACL_DISCONNECTED".equals(action)) {
                            return;
                        }
                        DataShapingServiceImpl.this.mDataShapingHandler.obtainMessage(19, intent).sendToTarget();
                    }
                }
            };
        }
        IntentFilter eventsFilter = new IntentFilter();
        eventsFilter.addAction("android.intent.action.SCREEN_ON");
        eventsFilter.addAction("android.intent.action.SCREEN_OFF");
        eventsFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        eventsFilter.addAction("mediatek.intent.action.PS_NETWORK_TYPE_CHANGED");
        eventsFilter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
        eventsFilter.addAction("android.hardware.usb.action.USB_STATE");
        eventsFilter.addAction("mediatek.intent.action.LTE_ACCESS_STRATUM_STATE_CHANGED");
        eventsFilter.addAction("mediatek.intent.action.SHARED_DEFAULT_APN_STATE_CHANGED");
        eventsFilter.addAction(CLOSE_TIME_EXPIRED_ACTION);
        eventsFilter.addAction("android.bluetooth.device.action.ACL_CONNECTED");
        eventsFilter.addAction("android.bluetooth.device.action.ACL_DISCONNECTED");
        this.mContext.registerReceiver(this.mBroadcastReceiver, eventsFilter);
        Slog.d("DataShapingService", "registerReceiver end");
        this.mUsageStats.addAppIdleStateChangeListener(new AppIdleStateChangeListener(this, appIdleStateChangeListener));
        Slog.d("DataShapingService", "addAppIdleStateChangeListener end");
    }

    boolean registerListener() {
        if (this.mWindowManagerService == null || this.mInputManagerService == null) {
            Slog.d("DataShapingService", "registerListener get WindowManager fail !");
            return false;
        }
        synchronized (this.mLock) {
            Slog.d("DataShapingService", "registerListener registerInput Before: " + this.mRegisterInput);
            if (!this.mRegisterInput && !this.mInputManagerService.alreadyHasInputFilter()) {
                Slog.d("DataShapingService", "registerListener!!!");
                this.mWindowManagerService.setInputFilter(this.mInputFilter);
                this.mRegisterInput = true;
            } else if (this.mRegisterInput) {
                Slog.d("DataShapingService", "I have registered it");
            } else {
                Slog.d("DataShapingService", "Someone registered it !!!");
            }
            Slog.d("DataShapingService", "registerListener registerInput After: " + this.mRegisterInput);
        }
        return this.mRegisterInput;
    }

    void unregisterListener() {
        if (this.mWindowManagerService == null) {
            Slog.d("DataShapingService", "unregisterListener get WindowManager fail !");
            return;
        }
        synchronized (this.mLock) {
            if (this.mRegisterInput) {
                Slog.d("DataShapingService", "unregisterListener registerInput is TRUE , Set myself to null!");
                this.mWindowManagerService.setInputFilter((IInputFilter) null);
                this.mRegisterInput = false;
            } else {
                Slog.d("DataShapingService", "unregisterListener registerInput is False , Not to set to null!");
            }
        }
    }

    private class DataShapingInputFilter extends InputFilter {
        private final Context mContext;

        DataShapingInputFilter(Context context) {
            super(context.getMainLooper());
            this.mContext = context;
        }

        public void onInputEvent(InputEvent event, int policyFlags) {
            if (event instanceof KeyEvent) {
                KeyEvent keyEvent = (KeyEvent) event;
                if (keyEvent.getAction() == 0 || keyEvent.getAction() == 1) {
                    Slog.d("DataShapingService", "Received event ACTION_UP or ACTION_DOWN");
                    if (DataShapingServiceImpl.this.mDataShapingHandler != null) {
                        DataShapingServiceImpl.this.mDataShapingHandler.sendEmptyMessage(18);
                    }
                }
            }
            super.onInputEvent(event, policyFlags);
        }

        public void onUninstalled() {
            Slog.d("DataShapingService", "onUninstalled : " + DataShapingServiceImpl.this.mCurrentState);
            synchronized (DataShapingServiceImpl.this.mLock) {
                DataShapingServiceImpl.this.mRegisterInput = false;
                if (DataShapingServiceImpl.this.mCurrentState instanceof GateCloseState) {
                    DataShapingServiceImpl.this.mDataShapingHandler.obtainMessage(23, Boolean.valueOf(DataShapingServiceImpl.this.mRegisterInput)).sendToTarget();
                    Slog.d("DataShapingService", "onUninstalled : Change to Gate Open");
                }
            }
        }
    }

    public void start() {
        this.mGateOpenState = new GateOpenState(this, this.mContext);
        this.mGateOpenLockedState = new GateOpenLockedState(this, this.mContext);
        this.mGateCloseState = new GateCloseState(this, this.mContext);
        setCurrentState(1);
        Slog.d("DataShapingService", "start check user preference");
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("background_power_saving_enable"), true, this.mSettingsObserver);
        this.mSettingsObserver.onChange(false);
    }

    public void setCurrentState(int stateType) {
        switch (stateType) {
            case 1:
                this.mCurrentState = this.mGateOpenLockedState;
                unregisterListener();
                Slog.d("DataShapingService", "[setCurrentState]: set to STATE_OPEN_LOCKED");
                break;
            case 2:
                this.mCurrentState = this.mGateOpenState;
                unregisterListener();
                Slog.d("DataShapingService", "[setCurrentState]: set to STATE_OPEN");
                break;
            case 3:
                this.mCurrentState = this.mGateCloseState;
                Slog.d("DataShapingService", "[setCurrentState]: set to STATE_CLOSE");
                break;
        }
    }

    public void enableDataShaping() {
        Slog.d("DataShapingService", "enableDataShaping");
    }

    public void disableDataShaping() {
        Slog.d("DataShapingService", "disableDataShaping");
    }

    public boolean openLteDataUpLinkGate(boolean isForce) {
        if (!this.mDataShapingEnabled) {
            Slog.d("DataShapingService", "[openLteDataUpLinkGate] DataShaping is Disabled!");
            return false;
        }
        boolean powerSavingEnabled = Settings.System.getInt(this.mContext.getContentResolver(), "background_power_saving_enable", 0) != 0;
        if ("0".equals(SystemProperties.get("persist.alarmgroup.enable", "-1"))) {
            powerSavingEnabled = false;
            Slog.d("DataShapingService", "[openLteDataUpLinkGate] persist.alarmgroup.enable is false");
        } else if ("1".equals(SystemProperties.get("persist.alarmgroup.enable", "-1"))) {
            powerSavingEnabled = true;
            Slog.d("DataShapingService", "[openLteDataUpLinkGate] persist.alarmgroup.enable is true");
        }
        if (!powerSavingEnabled) {
            Slog.d("DataShapingService", "[openLteDataUpLinkGate] powerSaving is Disabled!");
            return false;
        }
        if (System.currentTimeMillis() - this.mLastAlarmTriggerSuccessTime >= 300000) {
            if (this.mDataShapingHandler != null) {
                this.mDataShapingHandler.sendEmptyMessage(14);
            }
            this.mLastAlarmTriggerSuccessTime = System.currentTimeMillis();
            Slog.d("DataShapingService", "Alarm manager openLteDataUpLinkGate: true");
            return true;
        }
        Slog.d("DataShapingService", "Alarm manager openLteDataUpLinkGate: false");
        return false;
    }

    public void setDeviceIdleMode(boolean enabled) {
        if (!this.mDataShapingEnabled) {
            Slog.d("DataShapingService", "[setDeviceIdleMode] Data Shaping isn't enable.");
            return;
        }
        Slog.d("DataShapingService", "setDeviceIdleMode is " + enabled);
        this.mDataShapingUtils.setDeviceIdleState(enabled);
        this.mDataShapingHandler.obtainMessage(21, Boolean.valueOf(enabled)).sendToTarget();
    }

    public void cancelCloseExpiredAlarm() {
        Slog.d("DataShapingService", "[cancelCloseExpiredAlarm]");
        if (this.mPendingIntent == null) {
            return;
        }
        AlarmManager alarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        alarmManager.cancel(this.mPendingIntent);
    }

    public void startCloseExpiredAlarm() {
        Slog.d("DataShapingService", "[startCloseExpiredAlarm] cancel previous alarm");
        cancelCloseExpiredAlarm();
        Slog.d("DataShapingService", "[startCloseExpiredAlarm] start new alarm");
        AlarmManager alarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        if (this.mPendingIntent == null) {
            Intent intent = new Intent(CLOSE_TIME_EXPIRED_ACTION);
            this.mPendingIntent = PendingIntent.getBroadcast(this.mContext, 0, intent, 0);
        }
        alarmManager.set(0, System.currentTimeMillis() + 300000, this.mPendingIntent);
    }

    private class DataShapingHandler extends Handler {
        public DataShapingHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    sendEmptyMessage(2);
                    break;
                case 2:
                    Slog.d("DataShapingService", "[handleMessage] msg_init");
                    DataShapingServiceImpl.this.mWindowManagerService = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
                    DataShapingServiceImpl.this.mInputManagerService = (InputManagerService) ServiceManager.getService("input");
                    DataShapingServiceImpl.this.mInputFilter = DataShapingServiceImpl.this.new DataShapingInputFilter(DataShapingServiceImpl.this.mContext);
                    break;
                case 10:
                    Slog.d("DataShapingService", "[handleMessage] msg_screen_state_changed");
                    DataShapingServiceImpl.this.mCurrentState.onScreenStateChanged(((Boolean) msg.obj).booleanValue());
                    break;
                case 11:
                    Slog.d("DataShapingService", "[handleMessage] msg_network_type_changed");
                    DataShapingServiceImpl.this.mCurrentState.onNetworkTypeChanged((Intent) msg.obj);
                    break;
                case 12:
                    Slog.d("DataShapingService", "[handleMessage] msg_wifi_ap_state_changed");
                    DataShapingServiceImpl.this.mCurrentState.onWifiTetherStateChanged((Intent) msg.obj);
                    break;
                case 13:
                    Slog.d("DataShapingService", "[handleMessage] msg_usb_state_changed");
                    DataShapingServiceImpl.this.mCurrentState.onUsbConnectionChanged((Intent) msg.obj);
                    break;
                case 14:
                    Slog.d("DataShapingService", "[handleMessage] msg_alarm_manager_trigger");
                    DataShapingServiceImpl.this.mCurrentState.onAlarmManagerTrigger();
                    break;
                case 15:
                    Slog.d("DataShapingService", "[handleMessage] msg_lte_as_state_changed");
                    DataShapingServiceImpl.this.mCurrentState.onLteAccessStratumStateChanged((Intent) msg.obj);
                    break;
                case 16:
                    Slog.d("DataShapingService", "[handleMessage] msg_shared_default_apn_state_changed");
                    DataShapingServiceImpl.this.mCurrentState.onSharedDefaultApnStateChanged((Intent) msg.obj);
                    break;
                case 17:
                    Slog.d("DataShapingService", "[handleMessage] msg_gate_close_timer_expired");
                    DataShapingServiceImpl.this.mCurrentState.onCloseTimeExpired();
                    DataShapingServiceImpl.this.releaseWakeLock();
                    break;
                case 18:
                    Slog.d("DataShapingService", "[handleMessage] msg_headsethook_changed");
                    DataShapingServiceImpl.this.mCurrentState.onMediaButtonTrigger();
                    break;
                case 19:
                    Slog.d("DataShapingService", "[handleMessage] msg_bt_ap_state_changed");
                    DataShapingServiceImpl.this.mCurrentState.onBTStateChanged((Intent) msg.obj);
                    break;
                case 20:
                    Slog.d("DataShapingService", "[handleMessage] msg_connectivity_changed");
                    DataShapingServiceImpl.this.mDataShapingUtils.setLteAsReport();
                    break;
                case 21:
                    DataShapingServiceImpl.this.mCurrentState.onDeviceIdleStateChanged(((Boolean) msg.obj).booleanValue());
                    break;
                case 22:
                    DataShapingServiceImpl.this.mCurrentState.onAPPStandbyStateChanged(((Boolean) msg.obj).booleanValue());
                    break;
                case 23:
                    DataShapingServiceImpl.this.mCurrentState.onInputFilterStateChanged(((Boolean) msg.obj).booleanValue());
                    break;
            }
        }
    }

    private void getWakeLock() {
        Slog.d("DataShapingService", "[getWakeLock]");
        releaseWakeLock();
        if (this.mWakelock == null) {
            PowerManager powerManager = (PowerManager) this.mContext.getSystemService("power");
            this.mWakelock = powerManager.newWakeLock(1, getClass().getCanonicalName());
        }
        this.mWakelock.acquire(30000L);
    }

    private void releaseWakeLock() {
        Slog.d("DataShapingService", "[releaseWakeLock]");
        if (this.mWakelock == null || !this.mWakelock.isHeld()) {
            return;
        }
        Slog.d("DataShapingService", "really release WakeLock");
        this.mWakelock.release();
        this.mWakelock = null;
    }

    private void reset() {
        setCurrentState(1);
        releaseWakeLock();
        cancelCloseExpiredAlarm();
    }

    private class AppIdleStateChangeListener extends UsageStatsManagerInternal.AppIdleStateChangeListener {
        AppIdleStateChangeListener(DataShapingServiceImpl this$0, AppIdleStateChangeListener appIdleStateChangeListener) {
            this();
        }

        private AppIdleStateChangeListener() {
        }

        public void onAppIdleStateChanged(String packageName, int userId, boolean idle) {
        }

        public void onParoleStateChanged(boolean isParoleOn) {
            Slog.d("DataShapingService", "onParoleStateChanged is " + isParoleOn);
            DataShapingServiceImpl.this.mDataShapingHandler.obtainMessage(22, Boolean.valueOf(isParoleOn)).sendToTarget();
        }
    }
}
