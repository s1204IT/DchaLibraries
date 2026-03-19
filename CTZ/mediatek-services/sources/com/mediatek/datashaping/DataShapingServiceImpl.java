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
import com.android.server.AlarmManagerService;
import com.android.server.LocalServices;
import com.android.server.input.InputManagerService;
import com.android.server.wm.WindowManagerInternal;
import com.mediatek.datashaping.IDataShapingManager;
import java.lang.reflect.Field;
import java.util.ArrayList;

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
        public void onChange(boolean z) {
            boolean z2 = Settings.System.getInt(DataShapingServiceImpl.this.mContext.getContentResolver(), "background_power_saving_enable", 0) != 0;
            if ("0".equals(SystemProperties.get("persist.vendor.datashaping.test", "-1"))) {
                Slog.d("DataShapingService", "persist.vendor.datashaping.test is false");
                z2 = false;
            } else if ("1".equals(SystemProperties.get("persist.vendor.datashaping.test", "-1"))) {
                Slog.d("DataShapingService", "persist.vendor.datashaping.test is true");
                z2 = true;
            }
            if (z2 != DataShapingServiceImpl.this.mDataShapingEnabled) {
                if (!z2) {
                    if (DataShapingServiceImpl.this.mBroadcastReceiver != null) {
                        DataShapingServiceImpl.this.mContext.unregisterReceiver(DataShapingServiceImpl.this.mBroadcastReceiver);
                    }
                    if (DataShapingServiceImpl.this.mHandlerThread != null) {
                        DataShapingServiceImpl.this.mHandlerThread.quitSafely();
                    }
                    DataShapingServiceImpl.this.mDataShapingUtils.reset();
                    DataShapingServiceImpl.this.reset();
                    Slog.d("DataShapingService", "data shaping disabled, stop handler thread and reset!");
                } else {
                    Slog.d("DataShapingService", "data shaping enabled, start handler thread!");
                    DataShapingServiceImpl.this.mHandlerThread = new HandlerThread("DataShapingService");
                    DataShapingServiceImpl.this.mHandlerThread.start();
                    DataShapingServiceImpl.this.mDataShapingHandler = DataShapingServiceImpl.this.new DataShapingHandler(DataShapingServiceImpl.this.mHandlerThread.getLooper());
                    DataShapingServiceImpl.this.mDataShapingHandler.sendEmptyMessage(2);
                    DataShapingServiceImpl.this.setCurrentState(1);
                    DataShapingServiceImpl.this.registerReceiver();
                }
                DataShapingServiceImpl.this.mDataShapingEnabled = z2;
            }
        }
    };
    private UsageStatsManagerInternal mUsageStats = (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class);

    public DataShapingServiceImpl(Context context) {
        this.mContext = context;
        this.mDataShapingUtils = DataShapingUtils.getInstance(this.mContext);
    }

    public void registerReceiver() {
        Slog.d("DataShapingService", "registerReceiver start");
        if (this.mBroadcastReceiver == null) {
            this.mBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    Slog.d("DataShapingService", "received broadcast, action is: " + action);
                    if ("android.intent.action.SCREEN_ON" == action) {
                        DataShapingServiceImpl.this.mDataShapingHandler.obtainMessage(DataShapingServiceImpl.MSG_SCREEN_STATE_CHANGED, true).sendToTarget();
                        return;
                    }
                    if ("android.intent.action.SCREEN_OFF" == action) {
                        DataShapingServiceImpl.this.mDataShapingHandler.obtainMessage(DataShapingServiceImpl.MSG_SCREEN_STATE_CHANGED, false).sendToTarget();
                        return;
                    }
                    if ("mediatek.intent.action.PS_NETWORK_TYPE_CHANGED" == action) {
                        DataShapingServiceImpl.this.mDataShapingUtils.setCurrentNetworkType(intent);
                        DataShapingServiceImpl.this.mDataShapingHandler.obtainMessage(DataShapingServiceImpl.MSG_NETWORK_TYPE_CHANGED, intent).sendToTarget();
                        return;
                    }
                    if ("android.net.conn.CONNECTIVITY_CHANGE" == action) {
                        DataShapingServiceImpl.this.mDataShapingHandler.obtainMessage(DataShapingServiceImpl.MSG_CONNECTIVITY_CHANGED, intent).sendToTarget();
                        return;
                    }
                    if ("android.net.wifi.WIFI_AP_STATE_CHANGED" == action) {
                        DataShapingServiceImpl.this.mDataShapingHandler.obtainMessage(DataShapingServiceImpl.MSG_WIFI_AP_STATE_CHANGED, intent).sendToTarget();
                        return;
                    }
                    if ("android.hardware.usb.action.USB_STATE" == action) {
                        DataShapingServiceImpl.this.mDataShapingHandler.obtainMessage(DataShapingServiceImpl.MSG_USB_STATE_CHANGED, intent).sendToTarget();
                        return;
                    }
                    if ("mediatek.intent.action.LTE_ACCESS_STRATUM_STATE_CHANGED" == action) {
                        DataShapingServiceImpl.this.mDataShapingHandler.obtainMessage(DataShapingServiceImpl.MSG_LTE_AS_STATE_CHANGED, intent).sendToTarget();
                        return;
                    }
                    if ("mediatek.intent.action.SHARED_DEFAULT_APN_STATE_CHANGED" == action) {
                        DataShapingServiceImpl.this.mDataShapingHandler.obtainMessage(DataShapingServiceImpl.MSG_SHARED_DEFAULT_APN_STATE_CHANGED, intent).sendToTarget();
                        return;
                    }
                    if (DataShapingServiceImpl.CLOSE_TIME_EXPIRED_ACTION == action) {
                        DataShapingServiceImpl.this.getWakeLock();
                        DataShapingServiceImpl.this.mDataShapingHandler.obtainMessage(DataShapingServiceImpl.MSG_GATE_CLOSE_TIMER_EXPIRED).sendToTarget();
                    } else if ("android.bluetooth.device.action.ACL_CONNECTED".equals(action) || "android.bluetooth.device.action.ACL_DISCONNECTED".equals(action)) {
                        DataShapingServiceImpl.this.mDataShapingHandler.obtainMessage(DataShapingServiceImpl.MSG_BT_AP_STATE_CHANGED, intent).sendToTarget();
                    }
                }
            };
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        intentFilter.addAction("mediatek.intent.action.PS_NETWORK_TYPE_CHANGED");
        intentFilter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
        intentFilter.addAction("android.hardware.usb.action.USB_STATE");
        intentFilter.addAction("mediatek.intent.action.LTE_ACCESS_STRATUM_STATE_CHANGED");
        intentFilter.addAction("mediatek.intent.action.SHARED_DEFAULT_APN_STATE_CHANGED");
        intentFilter.addAction(CLOSE_TIME_EXPIRED_ACTION);
        intentFilter.addAction("android.bluetooth.device.action.ACL_CONNECTED");
        intentFilter.addAction("android.bluetooth.device.action.ACL_DISCONNECTED");
        this.mContext.registerReceiver(this.mBroadcastReceiver, intentFilter);
        Slog.d("DataShapingService", "registerReceiver end");
        this.mUsageStats.addAppIdleStateChangeListener(new AppIdleStateChangeListener());
        Slog.d("DataShapingService", "addAppIdleStateChangeListener end");
    }

    boolean registerListener() {
        boolean z;
        if (this.mWindowManagerService == null || this.mInputManagerService == null) {
            Slog.d("DataShapingService", "registerListener get WindowManager fail !");
            return false;
        }
        synchronized (this.mLock) {
            Slog.d("DataShapingService", "registerListener registerInput Before: " + this.mRegisterInput);
            if (!this.mRegisterInput && !alreadyHasInputFilter()) {
                if (!this.mRegisterInput) {
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
            z = this.mRegisterInput;
        }
        return z;
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

        public void onInputEvent(InputEvent inputEvent, int i) {
            if ((inputEvent instanceof KeyEvent) && (inputEvent.getAction() == 0 || inputEvent.getAction() == 1)) {
                Slog.d("DataShapingService", "Received event ACTION_UP or ACTION_DOWN");
                if (DataShapingServiceImpl.this.mDataShapingHandler != null) {
                    DataShapingServiceImpl.this.mDataShapingHandler.sendEmptyMessage(DataShapingServiceImpl.MSG_HEADSETHOOK_CHANGED);
                }
            }
            super.onInputEvent(inputEvent, i);
        }

        public void onUninstalled() {
            Slog.d("DataShapingService", "onUninstalled : " + DataShapingServiceImpl.this.mCurrentState);
            synchronized (DataShapingServiceImpl.this.mLock) {
                DataShapingServiceImpl.this.mRegisterInput = false;
                if (DataShapingServiceImpl.this.mCurrentState instanceof GateCloseState) {
                    DataShapingServiceImpl.this.mCurrentState = DataShapingServiceImpl.this.mGateOpenState;
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

    public void setCurrentState(int i) {
        switch (i) {
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

    public boolean openLteDataUpLinkGate(boolean z) {
        if (!this.mDataShapingEnabled) {
            Slog.d("DataShapingService", "[openLteDataUpLinkGate] DataShaping is Disabled!");
            return false;
        }
        boolean z2 = Settings.System.getInt(this.mContext.getContentResolver(), "background_power_saving_enable", 0) != 0;
        if ("0".equals(SystemProperties.get("persist.vendor.alarmgroup", "-1"))) {
            Slog.d("DataShapingService", "[openLteDataUpLinkGate] persist.vendor.alarmgroup is false");
            z2 = false;
        } else if ("1".equals(SystemProperties.get("persist.vendor.alarmgroup", "-1"))) {
            Slog.d("DataShapingService", "[openLteDataUpLinkGate] persist.vendor.alarmgroup is true");
            z2 = true;
        }
        if (z2) {
            Slog.d("DataShapingService", "[openLteDataUpLinkGate] isForce: " + z);
            if (z && this.mDataShapingHandler != null) {
                this.mDataShapingHandler.sendEmptyMessage(MSG_ALARM_MANAGER_TRIGGER);
                Slog.i("DataShapingService", "[openLteDataUpLinkGate] force gate open!!");
                return true;
            }
            if (System.currentTimeMillis() - this.mLastAlarmTriggerSuccessTime >= 300000) {
                if (this.mDataShapingHandler != null) {
                    this.mDataShapingHandler.sendEmptyMessage(MSG_ALARM_MANAGER_TRIGGER);
                }
                this.mLastAlarmTriggerSuccessTime = System.currentTimeMillis();
                Slog.d("DataShapingService", "Alarm manager openLteDataUpLinkGate: true");
                return true;
            }
            Slog.d("DataShapingService", "Alarm manager openLteDataUpLinkGate: false");
            return false;
        }
        Slog.d("DataShapingService", "[openLteDataUpLinkGate] powerSaving is Disabled!");
        return false;
    }

    public void setDeviceIdleMode(boolean z) {
        if (!this.mDataShapingEnabled) {
            Slog.d("DataShapingService", "[setDeviceIdleMode] Data Shaping isn't enable.");
            return;
        }
        Slog.d("DataShapingService", "setDeviceIdleMode is " + z);
        this.mDataShapingUtils.setDeviceIdleState(z);
        this.mDataShapingHandler.obtainMessage(MSG_DEVICEIDLE_CHANGED, Boolean.valueOf(z)).sendToTarget();
    }

    public void cancelCloseExpiredAlarm() {
        Slog.d("DataShapingService", "[cancelCloseExpiredAlarm]");
        if (this.mPendingIntent != null) {
            ((AlarmManager) this.mContext.getSystemService("alarm")).cancel(this.mPendingIntent);
        }
    }

    public void startCloseExpiredAlarm() {
        Slog.d("DataShapingService", "[startCloseExpiredAlarm] cancel previous alarm");
        cancelCloseExpiredAlarm();
        Slog.d("DataShapingService", "[startCloseExpiredAlarm] start new alarm");
        AlarmManager alarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        if (this.mPendingIntent == null) {
            this.mPendingIntent = PendingIntent.getBroadcast(this.mContext, 0, new Intent(CLOSE_TIME_EXPIRED_ACTION), 0);
        }
        alarmManager.set(0, System.currentTimeMillis() + 300000, this.mPendingIntent);
    }

    private class DataShapingHandler extends Handler {
        public DataShapingHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            int i = message.what;
            switch (i) {
                case 1:
                    sendEmptyMessage(2);
                    break;
                case 2:
                    Slog.d("DataShapingService", "[handleMessage] msg_init");
                    DataShapingServiceImpl.this.mWindowManagerService = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
                    DataShapingServiceImpl dataShapingServiceImpl = DataShapingServiceImpl.this;
                    Context unused = DataShapingServiceImpl.this.mContext;
                    dataShapingServiceImpl.mInputManagerService = ServiceManager.getService("input");
                    DataShapingServiceImpl.this.mInputFilter = DataShapingServiceImpl.this.new DataShapingInputFilter(DataShapingServiceImpl.this.mContext);
                    if (DataShapingServiceImpl.this.mDataShapingUtils != null) {
                        DataShapingServiceImpl.this.mDataShapingUtils.initDataShapingWhitelist();
                    }
                    break;
                case 3:
                    break;
                default:
                    switch (i) {
                        case DataShapingServiceImpl.MSG_SCREEN_STATE_CHANGED:
                            Slog.d("DataShapingService", "[handleMessage] msg_screen_state_changed");
                            DataShapingServiceImpl.this.mCurrentState.onScreenStateChanged(((Boolean) message.obj).booleanValue());
                            break;
                        case DataShapingServiceImpl.MSG_NETWORK_TYPE_CHANGED:
                            Slog.d("DataShapingService", "[handleMessage] msg_network_type_changed");
                            DataShapingServiceImpl.this.mCurrentState.onNetworkTypeChanged((Intent) message.obj);
                            break;
                        case DataShapingServiceImpl.MSG_WIFI_AP_STATE_CHANGED:
                            Slog.d("DataShapingService", "[handleMessage] msg_wifi_ap_state_changed");
                            DataShapingServiceImpl.this.mCurrentState.onWifiTetherStateChanged((Intent) message.obj);
                            break;
                        case DataShapingServiceImpl.MSG_USB_STATE_CHANGED:
                            Slog.d("DataShapingService", "[handleMessage] msg_usb_state_changed");
                            DataShapingServiceImpl.this.mCurrentState.onUsbConnectionChanged((Intent) message.obj);
                            break;
                        case DataShapingServiceImpl.MSG_ALARM_MANAGER_TRIGGER:
                            Slog.d("DataShapingService", "[handleMessage] msg_alarm_manager_trigger");
                            DataShapingServiceImpl.this.mCurrentState.onAlarmManagerTrigger();
                            break;
                        case DataShapingServiceImpl.MSG_LTE_AS_STATE_CHANGED:
                            Slog.d("DataShapingService", "[handleMessage] msg_lte_as_state_changed");
                            DataShapingServiceImpl.this.mCurrentState.onLteAccessStratumStateChanged((Intent) message.obj);
                            break;
                        case DataShapingServiceImpl.MSG_SHARED_DEFAULT_APN_STATE_CHANGED:
                            Slog.d("DataShapingService", "[handleMessage] msg_shared_default_apn_state_changed");
                            DataShapingServiceImpl.this.mCurrentState.onSharedDefaultApnStateChanged((Intent) message.obj);
                            break;
                        case DataShapingServiceImpl.MSG_GATE_CLOSE_TIMER_EXPIRED:
                            Slog.d("DataShapingService", "[handleMessage] msg_gate_close_timer_expired");
                            DataShapingServiceImpl.this.mCurrentState.onCloseTimeExpired();
                            DataShapingServiceImpl.this.releaseWakeLock();
                            break;
                        case DataShapingServiceImpl.MSG_HEADSETHOOK_CHANGED:
                            Slog.d("DataShapingService", "[handleMessage] msg_headsethook_changed");
                            DataShapingServiceImpl.this.mCurrentState.onMediaButtonTrigger();
                            break;
                        case DataShapingServiceImpl.MSG_BT_AP_STATE_CHANGED:
                            Slog.d("DataShapingService", "[handleMessage] msg_bt_ap_state_changed");
                            DataShapingServiceImpl.this.mCurrentState.onBTStateChanged((Intent) message.obj);
                            break;
                        case DataShapingServiceImpl.MSG_CONNECTIVITY_CHANGED:
                            Slog.d("DataShapingService", "[handleMessage] msg_connectivity_changed");
                            DataShapingServiceImpl.this.mDataShapingUtils.setLteAsReport();
                            break;
                        case DataShapingServiceImpl.MSG_DEVICEIDLE_CHANGED:
                            DataShapingServiceImpl.this.mCurrentState.onDeviceIdleStateChanged(((Boolean) message.obj).booleanValue());
                            break;
                        case DataShapingServiceImpl.MSG_APPSTANDBY_CHANGED:
                            DataShapingServiceImpl.this.mCurrentState.onAPPStandbyStateChanged(((Boolean) message.obj).booleanValue());
                            break;
                    }
                    break;
            }
        }
    }

    private void getWakeLock() {
        Slog.d("DataShapingService", "[getWakeLock]");
        releaseWakeLock();
        if (this.mWakelock == null) {
            this.mWakelock = ((PowerManager) this.mContext.getSystemService("power")).newWakeLock(1, getClass().getCanonicalName());
        }
        this.mWakelock.acquire(30000L);
    }

    private void releaseWakeLock() {
        Slog.d("DataShapingService", "[releaseWakeLock]");
        if (this.mWakelock != null && this.mWakelock.isHeld()) {
            Slog.d("DataShapingService", "really release WakeLock");
            this.mWakelock.release();
            this.mWakelock = null;
        }
    }

    private void reset() {
        setCurrentState(1);
        releaseWakeLock();
        cancelCloseExpiredAlarm();
    }

    private class AppIdleStateChangeListener extends UsageStatsManagerInternal.AppIdleStateChangeListener {
        private AppIdleStateChangeListener() {
        }

        public void onAppIdleStateChanged(String str, int i, boolean z, int i2, int i3) {
        }

        public void onParoleStateChanged(boolean z) {
            Slog.d("DataShapingService", "onParoleStateChanged is " + z);
            DataShapingServiceImpl.this.mDataShapingHandler.obtainMessage(DataShapingServiceImpl.MSG_APPSTANDBY_CHANGED, Boolean.valueOf(z)).sendToTarget();
        }
    }

    public boolean isDataShapingWhitelistApp(String str) {
        Slog.d("DataShapingService", "[isDataShapingWhitelistApp] packageName: " + str);
        return this.mDataShapingUtils.isDataShapingWhitelistApp(str);
    }

    public void openLteGateByDataShaping(ArrayList<AlarmManagerService.Alarm> arrayList) {
        if (arrayList == null) {
            Slog.e("DataShapingService", "dataShapingManager : ,triggerList null:" + arrayList);
            return;
        }
        int i = 0;
        boolean zIsDataShapingWhitelistApp = false;
        while (true) {
            if (i < arrayList.size()) {
                AlarmManagerService.Alarm alarm = arrayList.get(i);
                if (alarm.operation != null) {
                    String targetPackage = alarm.operation.getTargetPackage();
                    try {
                        zIsDataShapingWhitelistApp = isDataShapingWhitelistApp(targetPackage);
                    } catch (Exception e) {
                        Slog.e("DataShapingService", "Error isDataShapingWhitelistApp false" + e);
                    }
                    if (zIsDataShapingWhitelistApp) {
                        Slog.d("DataShapingService", "alarmPackageName: " + targetPackage + " is Whitelist");
                        break;
                    }
                }
                i++;
            }
        }
        try {
            Slog.i("DataShapingService", "openLteGateSuccess = " + openLteDataUpLinkGate(zIsDataShapingWhitelistApp));
        } catch (Exception e2) {
            Slog.e("DataShapingService", "Error openLteDataUpLinkGate false" + e2);
        }
    }

    private boolean alreadyHasInputFilter() {
        try {
            Field declaredField = InputManagerService.class.getDeclaredField("mInputFilter");
            declaredField.setAccessible(true);
            Slog.d("DataShapingService", "inputFilter: " + declaredField);
            IInputFilter iInputFilter = (IInputFilter) declaredField.get(this.mInputManagerService);
            Slog.d("DataShapingService", "filter: " + iInputFilter + ",");
            if (iInputFilter == null) {
                return false;
            }
            return true;
        } catch (Exception e) {
            Slog.d("DataShapingService", "Exception: " + e);
            return false;
        }
    }
}
