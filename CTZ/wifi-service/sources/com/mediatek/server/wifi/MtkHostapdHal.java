package com.mediatek.server.wifi;

import android.R;
import android.content.Context;
import android.hardware.wifi.hostapd.V1_0.HostapdStatus;
import android.hardware.wifi.hostapd.V1_0.IHostapd;
import android.net.wifi.WifiConfiguration;
import android.os.IHwInterface;
import android.os.RemoteException;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.wifi.HostapdHal;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.util.NativeUtil;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import javax.annotation.concurrent.ThreadSafe;
import mediatek.net.wifi.HotspotClient;
import vendor.mediatek.hardware.wifi.hostapd.V2_0.IHostapd;
import vendor.mediatek.hardware.wifi.hostapd.V2_0.IHostapdCallback;

@ThreadSafe
public class MtkHostapdHal {
    private static final String TAG = "MtkHostapdHal";
    private static String sIfaceName;

    public static boolean addAccessPoint(String str, WifiConfiguration wifiConfiguration) {
        sIfaceName = str;
        Context context = getContext();
        boolean z = context.getResources().getBoolean(R.^attr-private.regularColor);
        boolean z2 = context.getResources().getBoolean(R.^attr-private.relativeTimeDisambiguationText);
        HostapdHal hostapdHal = getHostapdHal();
        synchronized (getLock(hostapdHal)) {
            IHostapd.IfaceParams ifaceParams = new IHostapd.IfaceParams();
            ifaceParams.ifaceName = str;
            ifaceParams.hwModeParams.enable80211N = true;
            ifaceParams.hwModeParams.enable80211AC = z2;
            try {
                ifaceParams.channelParams.band = getBand(hostapdHal, wifiConfiguration);
                if (z) {
                    ifaceParams.channelParams.enableAcs = true;
                    ifaceParams.channelParams.acsShouldExcludeDfs = true;
                } else {
                    if (ifaceParams.channelParams.band == 2) {
                        Log.d(TAG, "ACS is not supported on this device, using 2.4 GHz band.");
                        ifaceParams.channelParams.band = 0;
                    }
                    ifaceParams.channelParams.enableAcs = false;
                    ifaceParams.channelParams.channel = wifiConfiguration.apChannel;
                }
                IHostapd.NetworkParams networkParams = new IHostapd.NetworkParams();
                networkParams.ssid.addAll(NativeUtil.stringToByteArrayList(wifiConfiguration.SSID));
                networkParams.isHidden = wifiConfiguration.hiddenSSID;
                networkParams.encryptionType = getEncryptionType(hostapdHal, wifiConfiguration);
                networkParams.pskPassphrase = wifiConfiguration.preSharedKey != null ? wifiConfiguration.preSharedKey : "";
                networkParams.maxNumSta = Settings.System.getInt(context.getContentResolver(), "wifi_hotspot_max_client_num", 10);
                networkParams.macAddrAcl = Settings.System.getInt(context.getContentResolver(), "wifi_hotspot_is_all_devices_allowed", 1) == 1 ? "0" : "1";
                String str2 = "";
                for (HotspotClient hotspotClient : MtkSoftApManager.getAllowedDevices()) {
                    str2 = str2 + (hotspotClient.isBlocked ? "-" : "") + hotspotClient.deviceAddress + "\n";
                }
                networkParams.acceptMacFileContent = str2;
                if (!checkHostapdAndLogFailure(hostapdHal, "addAccessPoint")) {
                    return false;
                }
                try {
                    vendor.mediatek.hardware.wifi.hostapd.V2_0.IHostapd iHostapdCastFrom = vendor.mediatek.hardware.wifi.hostapd.V2_0.IHostapd.castFrom((IHwInterface) vendor.mediatek.hardware.wifi.hostapd.V2_0.IHostapd.getService());
                    if (iHostapdCastFrom != null) {
                        return checkStatusAndLogFailure(hostapdHal, iHostapdCastFrom.addAccessPoint(ifaceParams, networkParams), "addAccessPoint");
                    }
                    Log.e(TAG, "addAccessPoint: Failed to get IHostapd");
                    return false;
                } catch (RemoteException e) {
                    handleRemoteException(hostapdHal, e, "addAccessPoint");
                    return false;
                }
            } catch (IllegalArgumentException e2) {
                Log.e(TAG, "Unrecognized apBand " + wifiConfiguration.apBand);
                return false;
            }
        }
    }

    public static boolean registerCallback(IHostapdCallback iHostapdCallback) {
        HostapdHal hostapdHal = getHostapdHal();
        synchronized (getLock(hostapdHal)) {
            if (!checkHostapdAndLogFailure(hostapdHal, "registerCallback")) {
                return false;
            }
            try {
                vendor.mediatek.hardware.wifi.hostapd.V2_0.IHostapd iHostapdCastFrom = vendor.mediatek.hardware.wifi.hostapd.V2_0.IHostapd.castFrom((IHwInterface) vendor.mediatek.hardware.wifi.hostapd.V2_0.IHostapd.getService());
                if (iHostapdCastFrom != null) {
                    return checkStatusAndLogFailure(hostapdHal, iHostapdCastFrom.registerCallback(iHostapdCallback), "registerCallback");
                }
                Log.e(TAG, "registerCallback: Failed to get IHostapd");
                return false;
            } catch (RemoteException e) {
                handleRemoteException(hostapdHal, e, "registerCallback");
                return false;
            }
        }
    }

    public static boolean blockClient(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        HostapdHal hostapdHal = getHostapdHal();
        synchronized (getLock(hostapdHal)) {
            if (!checkHostapdAndLogFailure(hostapdHal, "blockClient")) {
                return false;
            }
            try {
                vendor.mediatek.hardware.wifi.hostapd.V2_0.IHostapd iHostapdCastFrom = vendor.mediatek.hardware.wifi.hostapd.V2_0.IHostapd.castFrom((IHwInterface) vendor.mediatek.hardware.wifi.hostapd.V2_0.IHostapd.getService());
                if (iHostapdCastFrom != null) {
                    return checkStatusAndLogFailure(hostapdHal, iHostapdCastFrom.blockClient(str), "blockClient");
                }
                Log.e(TAG, "blockClient: Failed to get IHostapd");
                return false;
            } catch (RemoteException e) {
                handleRemoteException(hostapdHal, e, "blockClient");
                return false;
            }
        }
    }

    public static boolean unblockClient(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        HostapdHal hostapdHal = getHostapdHal();
        synchronized (getLock(hostapdHal)) {
            if (!checkHostapdAndLogFailure(hostapdHal, "unblockClient")) {
                return false;
            }
            try {
                vendor.mediatek.hardware.wifi.hostapd.V2_0.IHostapd iHostapdCastFrom = vendor.mediatek.hardware.wifi.hostapd.V2_0.IHostapd.castFrom((IHwInterface) vendor.mediatek.hardware.wifi.hostapd.V2_0.IHostapd.getService());
                if (iHostapdCastFrom != null) {
                    return checkStatusAndLogFailure(hostapdHal, iHostapdCastFrom.unblockClient(str), "unblockClient");
                }
                Log.e(TAG, "unblockClient: Failed to get IHostapd");
                return false;
            } catch (RemoteException e) {
                handleRemoteException(hostapdHal, e, "unblockClient");
                return false;
            }
        }
    }

    public static boolean updateAllowedList(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        HostapdHal hostapdHal = getHostapdHal();
        synchronized (getLock(hostapdHal)) {
            if (!checkHostapdAndLogFailure(hostapdHal, "updateAllowedList")) {
                return false;
            }
            try {
                vendor.mediatek.hardware.wifi.hostapd.V2_0.IHostapd iHostapdCastFrom = vendor.mediatek.hardware.wifi.hostapd.V2_0.IHostapd.castFrom((IHwInterface) vendor.mediatek.hardware.wifi.hostapd.V2_0.IHostapd.getService());
                if (iHostapdCastFrom != null) {
                    return checkStatusAndLogFailure(hostapdHal, iHostapdCastFrom.updateAllowedList(str), "updateAllowedList");
                }
                Log.e(TAG, "updateAllowedList: Failed to get IHostapd");
                return false;
            } catch (RemoteException e) {
                handleRemoteException(hostapdHal, e, "updateAllowedList");
                return false;
            }
        }
    }

    public static boolean setAllDevicesAllowed(boolean z) {
        HostapdHal hostapdHal = getHostapdHal();
        synchronized (getLock(hostapdHal)) {
            if (!checkHostapdAndLogFailure(hostapdHal, "setAllDevicesAllowed")) {
                return false;
            }
            try {
                vendor.mediatek.hardware.wifi.hostapd.V2_0.IHostapd iHostapdCastFrom = vendor.mediatek.hardware.wifi.hostapd.V2_0.IHostapd.castFrom((IHwInterface) vendor.mediatek.hardware.wifi.hostapd.V2_0.IHostapd.getService());
                if (iHostapdCastFrom != null) {
                    return checkStatusAndLogFailure(hostapdHal, iHostapdCastFrom.setAllDevicesAllowed(z), "setAllDevicesAllowed");
                }
                Log.e(TAG, "setAllDevicesAllowed: Failed to get IHostapd");
                return false;
            } catch (RemoteException e) {
                handleRemoteException(hostapdHal, e, "setAllDevicesAllowed");
                return false;
            }
        }
    }

    public static String getIfaceName() {
        return sIfaceName;
    }

    private static Context getContext() {
        WifiInjector wifiInjector = WifiInjector.getInstance();
        try {
            Field declaredField = wifiInjector.getClass().getDeclaredField("mContext");
            declaredField.setAccessible(true);
            return (Context) declaredField.get(wifiInjector);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static HostapdHal getHostapdHal() {
        WifiInjector wifiInjector = WifiInjector.getInstance();
        try {
            Field declaredField = wifiInjector.getClass().getDeclaredField("mHostapdHal");
            declaredField.setAccessible(true);
            return (HostapdHal) declaredField.get(wifiInjector);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Object getLock(HostapdHal hostapdHal) {
        try {
            Field declaredField = hostapdHal.getClass().getDeclaredField("mLock");
            declaredField.setAccessible(true);
            return declaredField.get(hostapdHal);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            return new Object();
        }
    }

    private static int getEncryptionType(HostapdHal hostapdHal, WifiConfiguration wifiConfiguration) {
        try {
            Method declaredMethod = hostapdHal.getClass().getDeclaredMethod("getEncryptionType", WifiConfiguration.class);
            declaredMethod.setAccessible(true);
            return ((Integer) declaredMethod.invoke(hostapdHal, wifiConfiguration)).intValue();
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private static int getBand(HostapdHal hostapdHal, WifiConfiguration wifiConfiguration) {
        try {
            Method declaredMethod = hostapdHal.getClass().getDeclaredMethod("getBand", WifiConfiguration.class);
            declaredMethod.setAccessible(true);
            return ((Integer) declaredMethod.invoke(hostapdHal, wifiConfiguration)).intValue();
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private static boolean checkHostapdAndLogFailure(HostapdHal hostapdHal, String str) {
        try {
            Method declaredMethod = hostapdHal.getClass().getDeclaredMethod("checkHostapdAndLogFailure", String.class);
            declaredMethod.setAccessible(true);
            return ((Boolean) declaredMethod.invoke(hostapdHal, str)).booleanValue();
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean checkStatusAndLogFailure(HostapdHal hostapdHal, HostapdStatus hostapdStatus, String str) {
        try {
            Method declaredMethod = hostapdHal.getClass().getDeclaredMethod("checkStatusAndLogFailure", HostapdStatus.class, String.class);
            declaredMethod.setAccessible(true);
            return ((Boolean) declaredMethod.invoke(hostapdHal, hostapdStatus, str)).booleanValue();
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void handleRemoteException(HostapdHal hostapdHal, RemoteException remoteException, String str) {
        try {
            Method declaredMethod = hostapdHal.getClass().getDeclaredMethod("handleRemoteException", RemoteException.class, String.class);
            declaredMethod.setAccessible(true);
            declaredMethod.invoke(hostapdHal, remoteException, str);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }
}
