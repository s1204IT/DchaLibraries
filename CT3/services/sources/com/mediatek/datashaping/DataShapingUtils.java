package com.mediatek.datashaping;

import android.R;
import android.app.usage.UsageStatsManagerInternal;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Slog;
import com.android.server.LocalServices;
import com.mediatek.internal.telephony.ITelephonyEx;
import java.util.Set;

public class DataShapingUtils {
    public static final long CLOSING_DELAY_BUFFER_FOR_MUSIC = 5000;
    private static final String CONFIG_AUTO_POWER = "persist.config.AutoPowerModes";
    private static final String LTE_AS_STATE_CONNECTED = "connected";
    private static final String LTE_AS_STATE_IDLE = "idle";
    private static final String LTE_AS_STATE_UNKNOWN = "unknown";
    private static final String TAG = "DataShapingUtils";
    private static DataShapingUtils sDataShapingUtils;
    private boolean mAppStandbyEnable;
    private AudioManager mAudioManager;
    private BluetoothManager mBluetoothManager;
    private long mClosingDelayStartTime;
    private ConnectivityManager mConnectivityManager;
    private Context mContext;
    private boolean mDeviceIdleState;
    private boolean mIsClosingDelayForMusic;
    private boolean mIsMobileConnection;
    private PowerManager mPowerManager;
    private UsbManager mUsbManager;
    private WifiManager mWifiManager;
    private int mCurrentNetworkType = 0;
    private UsageStatsManagerInternal mUsageStats = (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class);

    public static synchronized DataShapingUtils getInstance(Context context) {
        if (sDataShapingUtils == null) {
            sDataShapingUtils = new DataShapingUtils(context);
        }
        return sDataShapingUtils;
    }

    private DataShapingUtils(Context context) {
        this.mContext = context;
        this.mAppStandbyEnable = this.mContext.getResources().getBoolean(R.^attr-private.alwaysFocusable);
        if (SystemProperties.get(CONFIG_AUTO_POWER, "0").equals("-1")) {
            this.mAppStandbyEnable = false;
        } else {
            if (!SystemProperties.get(CONFIG_AUTO_POWER, "0").equals("1")) {
                return;
            }
            this.mAppStandbyEnable = true;
        }
    }

    public void setLteAsReport() {
        Slog.d(TAG, "[setLteAsReport]");
        boolean isLteDataOn = getLteDataOnState();
        if (isLteDataOn) {
            if (isLteDataOn != this.mIsMobileConnection) {
                setLteAccessStratumReport(true);
            }
        } else if (isLteDataOn != this.mIsMobileConnection) {
            setLteAccessStratumReport(false);
        }
        this.mIsMobileConnection = isLteDataOn;
    }

    public void setCurrentNetworkType(Intent intent) {
        if (intent == null) {
            return;
        }
        int networkType = intent.getIntExtra("psNetworkType", 0);
        Slog.d(TAG, "[setCurrentNetworkTypeIntent] networkType: " + networkType);
        this.mCurrentNetworkType = networkType;
    }

    public boolean isScreenOn() {
        if (this.mPowerManager == null) {
            this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        }
        Slog.d(TAG, "[isScreenOn] " + this.mPowerManager.isScreenOn());
        return this.mPowerManager.isScreenOn();
    }

    public boolean isWifiTetheringEnabled() {
        if (this.mWifiManager == null) {
            this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        }
        Slog.d(TAG, "[isWifiTetheringEnabled] isWifiApEnabled: " + this.mWifiManager.isWifiApEnabled());
        return this.mWifiManager.isWifiApEnabled();
    }

    public boolean isWifiTetheringEnabled(Intent intent) {
        if (intent == null) {
            return false;
        }
        int state = intent.getIntExtra("wifi_state", 11);
        Slog.d(TAG, "[isWifiTetheringEnabledIntent] state: " + state);
        return state == 13 || state == 12;
    }

    public boolean isUsbConnected() {
        if (this.mUsbManager == null) {
            this.mUsbManager = (UsbManager) this.mContext.getSystemService("usb");
        }
        Slog.d(TAG, "[isUsbConnected] isUsbConneted: " + (this.mUsbManager.getCurrentState() == 1));
        return this.mUsbManager.getCurrentState() == 1;
    }

    public boolean isUsbConnected(Intent intent) {
        if (intent == null) {
            return false;
        }
        boolean isUsbConnected = intent.getBooleanExtra(LTE_AS_STATE_CONNECTED, false);
        Slog.d(TAG, "[isUsbConnectedIntent] isUsbConnected: " + isUsbConnected);
        return intent.getBooleanExtra(LTE_AS_STATE_CONNECTED, false);
    }

    public boolean isNetworkTypeLte() {
        Slog.d(TAG, "[isNetworkTypeLte] mCurrentNetworkType: " + this.mCurrentNetworkType);
        if (this.mCurrentNetworkType == 13) {
            return true;
        }
        return false;
    }

    public boolean isNetworkTypeLte(Intent intent) {
        if (intent == null) {
            return false;
        }
        int networkType = intent.getIntExtra("psNetworkType", 0);
        Slog.d(TAG, "[isNetworkTypeLteIntent] networkType: " + networkType);
        return networkType == 13;
    }

    public boolean isBTStateOn(Intent intent) {
        if (intent == null) {
            return false;
        }
        if ("android.bluetooth.device.action.ACL_CONNECTED".equals(intent.getAction())) {
            Slog.d(TAG, "[isBTStateOn] BT ACTION_ACL_CONNECTED !");
            return true;
        }
        return isBTStateOn();
    }

    public boolean isBTStateOn() {
        if (this.mBluetoothManager == null) {
            this.mBluetoothManager = (BluetoothManager) this.mContext.getSystemService("bluetooth");
        }
        if (this.mBluetoothManager == null) {
            Slog.d(TAG, "BluetoothManager is null");
            return false;
        }
        BluetoothAdapter mAdapter = this.mBluetoothManager.getAdapter();
        if (mAdapter == null) {
            Slog.d(TAG, "BluetoothAdapter is null");
            return false;
        }
        int state = mAdapter.getState();
        if (10 == state) {
            Slog.d(TAG, "[isBTStateOn] BT is Off");
            return false;
        }
        Set<BluetoothDevice> bondedDevices = mAdapter.getBondedDevices();
        if (bondedDevices == null) {
            Slog.d(TAG, "[isBTStateOn] No bonded Devices");
            return false;
        }
        for (BluetoothDevice device : bondedDevices) {
            if (device.isConnected()) {
                int deviceType = device.getBluetoothClass().getDeviceClass();
                Slog.d(TAG, "[isBTStateOn] Connected Device = " + device.getName() + ", DeviceType = " + deviceType);
                if (1028 == deviceType) {
                    Slog.d(TAG, "Connected Device is AUDIO_VIDEO_WEARABLE_HEADSET");
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isAppIdleParoleOn() {
        if (this.mUsageStats == null) {
            Slog.d(TAG, "UsageStats is null");
            return false;
        }
        if (this.mAppStandbyEnable) {
            Slog.d(TAG, "[isAppIdleParoleOn] App Standby is enable");
            return this.mUsageStats.isAppIdleParoleOn();
        }
        Slog.d(TAG, "[isAppIdleParoleOn] App Standby isn't enable");
        return false;
    }

    public boolean canTurnFromLockedToOpen() {
        boolean isNetworkTypeLte = isNetworkTypeLte();
        boolean isScreenOn = isScreenOn();
        boolean isSharedDefaultApnEstablished = isSharedDefaultApnEstablished();
        boolean isUsbConnected = isUsbConnected();
        boolean isWifiTetheringEnabled = isWifiTetheringEnabled();
        boolean isAppIdleParoleOn = isAppIdleParoleOn();
        boolean isLteDataOn = getLteDataOnState();
        Slog.d(TAG, "[canTurnFromLockedToOpen] isNetworkTypeLte|" + isNetworkTypeLte + " isScreenOn|" + isScreenOn + " isSharedDefaultApnEstablised|" + isSharedDefaultApnEstablished + " isUsbConnected|" + isUsbConnected + " isWifiTetheringEnabled|" + isWifiTetheringEnabled + " isAppIdleParoleOn|" + isAppIdleParoleOn + " isDeviceIdleEnable|" + this.mDeviceIdleState + " isLteDataOn|" + isLteDataOn);
        boolean isReady = (!isNetworkTypeLte || isScreenOn || isSharedDefaultApnEstablished || isUsbConnected || isWifiTetheringEnabled || isAppIdleParoleOn || this.mDeviceIdleState) ? false : isLteDataOn;
        if (isReady) {
            boolean isBTStateOn = isBTStateOn();
            Slog.d(TAG, "[canTurnFromLockedToOpen] isBTStateOn|" + isBTStateOn);
            isReady = !isBTStateOn;
        }
        Slog.d(TAG, "[canTurnFromLockedToOpen]: " + isReady);
        return isReady;
    }

    public boolean isLteAccessStratumConnected(Intent intent) {
        if (intent == null) {
            return true;
        }
        String lteAsState = intent.getStringExtra("lteAccessStratumState");
        Slog.d(TAG, "[isLteAccessStratumConnectedIntent] lteAsState: " + lteAsState);
        if (LTE_AS_STATE_CONNECTED.equalsIgnoreCase(lteAsState)) {
            return true;
        }
        if (!LTE_AS_STATE_UNKNOWN.equalsIgnoreCase(lteAsState)) {
            return !LTE_AS_STATE_IDLE.equalsIgnoreCase(lteAsState);
        }
        setLteAccessStratumReport(true);
        return true;
    }

    public boolean isLteAccessStratumConnected() {
        ITelephonyEx telephonyExService = ITelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx"));
        if (telephonyExService == null) {
            Slog.d(TAG, "[isLteAccessStratumConnected] mTelephonyExService is null!");
            return true;
        }
        String state = null;
        try {
            state = telephonyExService.getLteAccessStratumState();
        } catch (RemoteException remoteException) {
            Slog.d(TAG, "[isLteAccessStratumConnected] remoteException: " + remoteException);
        }
        Slog.d(TAG, "[isLteAccessStratumConnected] state: " + state);
        if (LTE_AS_STATE_CONNECTED.equalsIgnoreCase(state)) {
            return true;
        }
        if (!LTE_AS_STATE_UNKNOWN.equalsIgnoreCase(state)) {
            return !LTE_AS_STATE_IDLE.equalsIgnoreCase(state);
        }
        setLteAccessStratumReport(true);
        return true;
    }

    public boolean isSharedDefaultApnEstablished() {
        ITelephonyEx telephonyExService = ITelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx"));
        if (telephonyExService == null) {
            Slog.d(TAG, "[isSharedDefaultApnEstablished] mTelephonyExService is null!");
            return true;
        }
        boolean isEstablished = true;
        try {
            isEstablished = telephonyExService.isSharedDefaultApn();
        } catch (RemoteException remoteException) {
            Slog.d(TAG, "[isSharedDefaultApnEstablished] remoteException: " + remoteException);
        }
        Slog.d(TAG, "[isSharedDefaultApnEstablished]: " + isEstablished);
        return isEstablished;
    }

    public boolean isSharedDefaultApnEstablished(Intent intent) {
        if (intent == null) {
            return true;
        }
        boolean isSharedDefaultApn = intent.getBooleanExtra("sharedDefaultApn", true);
        Slog.d(TAG, "[isSharedDefaultApnEstablishedIntent]: " + isSharedDefaultApn);
        return isSharedDefaultApn;
    }

    public boolean setLteUplinkDataTransfer(boolean isOn, int safeTimer) {
        Slog.d(TAG, "[setLteUplinkDataTransfer] isOn: " + isOn);
        ITelephonyEx telephonyExService = ITelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx"));
        if (telephonyExService == null) {
            Slog.d(TAG, "[setLteUplinkDataTransfer] mTelephonyExService is null!");
            return false;
        }
        boolean isSuccess = false;
        try {
            isSuccess = telephonyExService.setLteUplinkDataTransfer(isOn, safeTimer);
        } catch (RemoteException remoteException) {
            Slog.d(TAG, "[setLteUplinkDataTransfer] remoteException: " + remoteException);
        }
        Slog.d(TAG, "[setLteUplinkDataTransfer] TelephonyManager return set result: " + isSuccess);
        return isSuccess;
    }

    public boolean setLteAccessStratumReport(boolean isEnable) {
        Slog.d(TAG, "[setLteAccessStratumReport] enable: " + isEnable);
        ITelephonyEx telephonyExService = ITelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx"));
        if (telephonyExService == null) {
            Slog.d(TAG, "[setLteAccessStratumReport] mTelephonyExService is null!");
            return false;
        }
        boolean isSuccess = false;
        try {
            isSuccess = telephonyExService.setLteAccessStratumReport(isEnable);
        } catch (RemoteException remoteException) {
            Slog.d(TAG, "[setLteAccessStratumReport] remoteException: " + remoteException);
        }
        Slog.d(TAG, "[setLteAccessStratumReport] TelephonyManager return set result: " + isSuccess);
        return isSuccess;
    }

    public boolean isMusicActive() {
        if (this.mAudioManager == null) {
            this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
        }
        boolean isMusicActive = this.mAudioManager.isMusicActive();
        Slog.d(TAG, "[isMusicActive] isMusicActive: " + isMusicActive);
        return isMusicActive;
    }

    public void setClosingDelayForMusic(boolean isClosingDelay) {
        this.mIsClosingDelayForMusic = isClosingDelay;
    }

    public boolean getClosingDelayForMusic() {
        return this.mIsClosingDelayForMusic;
    }

    public void setClosingDelayStartTime(long timeMillis) {
        this.mClosingDelayStartTime = timeMillis;
    }

    public long getClosingDelayStartTime() {
        return this.mClosingDelayStartTime;
    }

    public void reset() {
        Slog.d(TAG, "reset");
        this.mCurrentNetworkType = 0;
        this.mIsMobileConnection = false;
        this.mIsClosingDelayForMusic = false;
        this.mClosingDelayStartTime = 0L;
        setLteUplinkDataTransfer(true, 600000);
        setLteAccessStratumReport(false);
    }

    public void setDeviceIdleState(boolean enable) {
        this.mDeviceIdleState = enable;
    }

    public boolean getLteDataOnState() {
        if (this.mConnectivityManager == null) {
            this.mConnectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        }
        NetworkInfo networkInfo = this.mConnectivityManager.getActiveNetworkInfo();
        boolean isMobile = false;
        boolean isLte = false;
        boolean isDataOn = false;
        if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
            int networkType = networkInfo.getType();
            String networkSubType = networkInfo.getSubtypeName();
            isMobile = ConnectivityManager.isNetworkTypeMobile(networkType);
            isLte = networkSubType.equals("LTE");
            isDataOn = true;
            Slog.d(TAG, "[setLteAsReport] networkType = " + networkType + " networkSubType = " + networkSubType + " isMobile = " + isMobile + " isLte = " + isLte + " isDataOn = true");
        }
        if (isMobile && isLte) {
            return isDataOn;
        }
        return false;
    }
}
