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
import android.os.storage.StorageManager;
import android.text.TextUtils;
import android.util.Slog;
import com.android.server.LocalServices;
import com.mediatek.internal.telephony.IMtkTelephonyEx;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

public class DataShapingUtils {
    public static final long CLOSING_DELAY_BUFFER_FOR_MUSIC = 5000;
    private static final String CONFIG_AUTO_POWER = "persist.vendor.AutoPowerModes";
    private static final String FILEPATH = "/system/etc/datashaping.config";
    private static final String LTE_AS_STATE_CONNECTED = "connected";
    private static final String LTE_AS_STATE_IDLE = "idle";
    private static final String LTE_AS_STATE_UNKNOWN = "unknown";
    private static final String SERVICE_NAME_MTK_TELEPHONY_EX = "phoneEx";
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
    private boolean mIsUsbConnected = false;
    private final ArrayList<String> mWhitelist = new ArrayList<>();
    private UsageStatsManagerInternal mUsageStats = (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class);

    public static synchronized DataShapingUtils getInstance(Context context) {
        if (sDataShapingUtils == null) {
            sDataShapingUtils = new DataShapingUtils(context);
        }
        return sDataShapingUtils;
    }

    private DataShapingUtils(Context context) {
        this.mContext = context;
        this.mAppStandbyEnable = this.mContext.getResources().getBoolean(R.^attr-private.floatingToolbarItemBackgroundDrawable);
        if (SystemProperties.get(CONFIG_AUTO_POWER, "0").equals("-1")) {
            this.mAppStandbyEnable = false;
        } else if (SystemProperties.get(CONFIG_AUTO_POWER, "0").equals("1")) {
            this.mAppStandbyEnable = true;
        }
    }

    public void setLteAsReport() {
        Slog.d(TAG, "[setLteAsReport]");
        boolean lteDataOnState = getLteDataOnState();
        if (lteDataOnState) {
            if (lteDataOnState != this.mIsMobileConnection) {
                setLteAccessStratumReport(true);
            }
        } else if (lteDataOnState != this.mIsMobileConnection) {
            setLteAccessStratumReport(false);
        }
        this.mIsMobileConnection = lteDataOnState;
    }

    public void setCurrentNetworkType(Intent intent) {
        if (intent == null) {
            return;
        }
        int intExtra = intent.getIntExtra("psNetworkType", 0);
        Slog.d(TAG, "[setCurrentNetworkTypeIntent] networkType: " + intExtra);
        this.mCurrentNetworkType = intExtra;
    }

    public boolean isScreenOn() {
        if (this.mPowerManager == null) {
            this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        }
        Slog.d(TAG, "[isScreenOn] " + this.mPowerManager.isScreenOn());
        return this.mPowerManager.isScreenOn();
    }

    public boolean isWifiTetheringEnabled() {
        if (!StorageManager.inCryptKeeperBounce()) {
            if (this.mWifiManager == null) {
                this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
            }
            Slog.d(TAG, "[isWifiTetheringEnabled] isWifiApEnabled: " + this.mWifiManager.isWifiApEnabled());
            return this.mWifiManager.isWifiApEnabled();
        }
        Slog.d(TAG, "[isWifiTetheringEnabled] inCryptKeeperBounce!!! ");
        return false;
    }

    public boolean isWifiTetheringEnabled(Intent intent) {
        if (intent == null) {
            return false;
        }
        int intExtra = intent.getIntExtra("wifi_state", 11);
        Slog.d(TAG, "[isWifiTetheringEnabledIntent] state: " + intExtra);
        if (intExtra != 13 && intExtra != 12) {
            return false;
        }
        return true;
    }

    public boolean isUsbConnected() {
        return this.mIsUsbConnected;
    }

    public boolean isUsbConnected(Intent intent) {
        if (intent == null) {
            return false;
        }
        boolean booleanExtra = intent.getBooleanExtra(LTE_AS_STATE_CONNECTED, false);
        if (this.mIsUsbConnected != booleanExtra) {
            this.mIsUsbConnected = booleanExtra;
        }
        Slog.d(TAG, "[isUsbConnectedIntent] isUsbConnected: " + booleanExtra + ",mIsUsbConnected:" + this.mIsUsbConnected);
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
        int intExtra = intent.getIntExtra("psNetworkType", 0);
        Slog.d(TAG, "[isNetworkTypeLteIntent] networkType: " + intExtra);
        if (intExtra != 13) {
            return false;
        }
        return true;
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
        BluetoothAdapter adapter = this.mBluetoothManager.getAdapter();
        if (adapter == null) {
            Slog.d(TAG, "BluetoothAdapter is null");
            return false;
        }
        if (10 == adapter.getState()) {
            Slog.d(TAG, "[isBTStateOn] BT is Off");
            return false;
        }
        Set<BluetoothDevice> bondedDevices = adapter.getBondedDevices();
        if (bondedDevices == null) {
            Slog.d(TAG, "[isBTStateOn] No bonded Devices");
            return false;
        }
        for (BluetoothDevice bluetoothDevice : bondedDevices) {
            if (bluetoothDevice.isConnected()) {
                int deviceClass = bluetoothDevice.getBluetoothClass().getDeviceClass();
                Slog.d(TAG, "[isBTStateOn] Connected Device = " + bluetoothDevice.getName() + ", DeviceType = " + deviceClass);
                if (1028 == deviceClass) {
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
        boolean z;
        boolean zIsNetworkTypeLte = isNetworkTypeLte();
        boolean zIsScreenOn = isScreenOn();
        boolean zIsSharedDefaultApnEstablished = isSharedDefaultApnEstablished();
        boolean zIsUsbConnected = isUsbConnected();
        boolean zIsWifiTetheringEnabled = isWifiTetheringEnabled();
        boolean zIsAppIdleParoleOn = isAppIdleParoleOn();
        boolean lteDataOnState = getLteDataOnState();
        Slog.d(TAG, "[canTurnFromLockedToOpen] isNetworkTypeLte|" + zIsNetworkTypeLte + " isScreenOn|" + zIsScreenOn + " isSharedDefaultApnEstablised|" + zIsSharedDefaultApnEstablished + " isUsbConnected|" + zIsUsbConnected + " isWifiTetheringEnabled|" + zIsWifiTetheringEnabled + " isAppIdleParoleOn|" + zIsAppIdleParoleOn + " isDeviceIdleEnable|" + this.mDeviceIdleState + " isLteDataOn|" + lteDataOnState);
        if (!zIsNetworkTypeLte || zIsScreenOn || zIsSharedDefaultApnEstablished || zIsUsbConnected || zIsWifiTetheringEnabled || zIsAppIdleParoleOn || this.mDeviceIdleState || !lteDataOnState) {
            z = false;
        } else {
            z = true;
        }
        if (z) {
            boolean zIsBTStateOn = isBTStateOn();
            Slog.d(TAG, "[canTurnFromLockedToOpen] isBTStateOn|" + zIsBTStateOn);
            z = zIsBTStateOn ^ true;
        }
        Slog.d(TAG, "[canTurnFromLockedToOpen]: " + z);
        return z;
    }

    public boolean isLteAccessStratumConnected(Intent intent) {
        if (intent == null) {
            return true;
        }
        String stringExtra = intent.getStringExtra("lteAccessStratumState");
        Slog.d(TAG, "[isLteAccessStratumConnectedIntent] lteAsState: " + stringExtra);
        if (LTE_AS_STATE_CONNECTED.equalsIgnoreCase(stringExtra)) {
            return true;
        }
        if (LTE_AS_STATE_UNKNOWN.equalsIgnoreCase(stringExtra)) {
            setLteAccessStratumReport(true);
            return true;
        }
        if (!LTE_AS_STATE_IDLE.equalsIgnoreCase(stringExtra)) {
            return true;
        }
        return false;
    }

    public boolean isLteAccessStratumConnected() {
        String lteAccessStratumState;
        IMtkTelephonyEx iMtkTelephonyExAsInterface = IMtkTelephonyEx.Stub.asInterface(ServiceManager.getService(SERVICE_NAME_MTK_TELEPHONY_EX));
        if (iMtkTelephonyExAsInterface == null) {
            Slog.d(TAG, "[isLteAccessStratumConnected] mTelephonyExService is null!");
            return true;
        }
        try {
            lteAccessStratumState = iMtkTelephonyExAsInterface.getLteAccessStratumState();
        } catch (RemoteException e) {
            Slog.d(TAG, "[isLteAccessStratumConnected] remoteException: " + e);
            lteAccessStratumState = null;
        }
        Slog.d(TAG, "[isLteAccessStratumConnected] state: " + lteAccessStratumState);
        if (LTE_AS_STATE_CONNECTED.equalsIgnoreCase(lteAccessStratumState)) {
            return true;
        }
        if (LTE_AS_STATE_UNKNOWN.equalsIgnoreCase(lteAccessStratumState)) {
            setLteAccessStratumReport(true);
            return true;
        }
        if (!LTE_AS_STATE_IDLE.equalsIgnoreCase(lteAccessStratumState)) {
            return true;
        }
        return false;
    }

    public boolean isSharedDefaultApnEstablished() {
        boolean zIsSharedDefaultApn;
        IMtkTelephonyEx iMtkTelephonyExAsInterface = IMtkTelephonyEx.Stub.asInterface(ServiceManager.getService(SERVICE_NAME_MTK_TELEPHONY_EX));
        if (iMtkTelephonyExAsInterface == null) {
            Slog.d(TAG, "[isSharedDefaultApnEstablished] mTelephonyExService is null!");
            return true;
        }
        try {
            zIsSharedDefaultApn = iMtkTelephonyExAsInterface.isSharedDefaultApn();
        } catch (RemoteException e) {
            Slog.d(TAG, "[isSharedDefaultApnEstablished] remoteException: " + e);
            zIsSharedDefaultApn = true;
        }
        Slog.d(TAG, "[isSharedDefaultApnEstablished]: " + zIsSharedDefaultApn);
        return zIsSharedDefaultApn;
    }

    public boolean isSharedDefaultApnEstablished(Intent intent) {
        if (intent == null) {
            return true;
        }
        boolean booleanExtra = intent.getBooleanExtra("sharedDefaultApn", true);
        Slog.d(TAG, "[isSharedDefaultApnEstablishedIntent]: " + booleanExtra);
        return booleanExtra;
    }

    public boolean setLteUplinkDataTransfer(boolean z, int i) {
        boolean lteUplinkDataTransfer;
        Slog.d(TAG, "[setLteUplinkDataTransfer] isOn: " + z);
        IMtkTelephonyEx iMtkTelephonyExAsInterface = IMtkTelephonyEx.Stub.asInterface(ServiceManager.getService(SERVICE_NAME_MTK_TELEPHONY_EX));
        if (iMtkTelephonyExAsInterface == null) {
            Slog.d(TAG, "[setLteUplinkDataTransfer] mTelephonyExService is null!");
            return false;
        }
        try {
            lteUplinkDataTransfer = iMtkTelephonyExAsInterface.setLteUplinkDataTransfer(z, i);
        } catch (RemoteException e) {
            Slog.d(TAG, "[setLteUplinkDataTransfer] remoteException: " + e);
            lteUplinkDataTransfer = false;
        }
        Slog.d(TAG, "[setLteUplinkDataTransfer] TelephonyManager return set result: " + lteUplinkDataTransfer);
        return lteUplinkDataTransfer;
    }

    public boolean setLteAccessStratumReport(boolean z) {
        boolean lteAccessStratumReport;
        Slog.d(TAG, "[setLteAccessStratumReport] enable: " + z);
        IMtkTelephonyEx iMtkTelephonyExAsInterface = IMtkTelephonyEx.Stub.asInterface(ServiceManager.getService(SERVICE_NAME_MTK_TELEPHONY_EX));
        if (iMtkTelephonyExAsInterface == null) {
            Slog.d(TAG, "[setLteAccessStratumReport] mTelephonyExService is null!");
            return false;
        }
        try {
            lteAccessStratumReport = iMtkTelephonyExAsInterface.setLteAccessStratumReport(z);
        } catch (RemoteException e) {
            Slog.d(TAG, "[setLteAccessStratumReport] remoteException: " + e);
            lteAccessStratumReport = false;
        }
        Slog.d(TAG, "[setLteAccessStratumReport] TelephonyManager return set result: " + lteAccessStratumReport);
        return lteAccessStratumReport;
    }

    public boolean isMusicActive() {
        if (this.mAudioManager == null) {
            this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
        }
        boolean zIsMusicActive = this.mAudioManager.isMusicActive();
        Slog.d(TAG, "[isMusicActive] isMusicActive: " + zIsMusicActive);
        return zIsMusicActive;
    }

    public void setClosingDelayForMusic(boolean z) {
        this.mIsClosingDelayForMusic = z;
    }

    public boolean getClosingDelayForMusic() {
        return this.mIsClosingDelayForMusic;
    }

    public void setClosingDelayStartTime(long j) {
        this.mClosingDelayStartTime = j;
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
        setLteUplinkDataTransfer(true, DataShapingServiceImpl.GATE_CLOSE_SAFE_TIMER);
        setLteAccessStratumReport(false);
    }

    public void setDeviceIdleState(boolean z) {
        this.mDeviceIdleState = z;
    }

    public boolean getLteDataOnState() {
        boolean z;
        boolean zIsNetworkTypeMobile;
        boolean zEquals;
        if (this.mConnectivityManager == null) {
            this.mConnectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        }
        NetworkInfo activeNetworkInfo = this.mConnectivityManager.getActiveNetworkInfo();
        if (activeNetworkInfo == null || !activeNetworkInfo.isConnectedOrConnecting()) {
            z = false;
            zIsNetworkTypeMobile = false;
            zEquals = false;
        } else {
            int type = activeNetworkInfo.getType();
            String subtypeName = activeNetworkInfo.getSubtypeName();
            zIsNetworkTypeMobile = ConnectivityManager.isNetworkTypeMobile(type);
            zEquals = subtypeName.equals("LTE");
            Slog.d(TAG, "[getLteDataOnState] networkType = " + type + " networkSubType = " + subtypeName + " isMobile = " + zIsNetworkTypeMobile + " isLte = " + zEquals + " isDataOn = true");
            z = true;
        }
        return zIsNetworkTypeMobile && zEquals && z;
    }

    public void initDataShapingWhitelist() {
        File file = new File(FILEPATH);
        if (!file.exists()) {
            Slog.v(TAG, "[initDataShapingWhitelist] has no data shaping whitelist exist !!");
            return;
        }
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            for (String line = bufferedReader.readLine(); line != null; line = bufferedReader.readLine()) {
                this.mWhitelist.add(line);
            }
            bufferedReader.close();
            Slog.d(TAG, "[initDataShapingWhitelist] mWhitelist: " + this.mWhitelist);
        } catch (IOException e) {
            Slog.e(TAG, "[initDataShapingWhitelist] IO Exception happened!!");
            e.printStackTrace();
        }
    }

    public boolean isDataShapingWhitelistApp(String str) {
        Slog.d(TAG, "[isDataShapingWhitelistApp] packageName: " + str + ", mWhitelist: " + this.mWhitelist);
        if (this.mWhitelist != null && !TextUtils.isEmpty(str)) {
            Iterator<String> it = this.mWhitelist.iterator();
            while (it.hasNext()) {
                if (str.equals(it.next())) {
                    Slog.d(TAG, "[isDataShapingWhitelistApp]  " + str + " : True");
                    return true;
                }
            }
            return false;
        }
        return false;
    }
}
