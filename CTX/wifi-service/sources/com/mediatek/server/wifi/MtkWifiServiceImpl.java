package com.mediatek.server.wifi;

import android.content.Context;
import android.net.wifi.IWifiManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Slog;
import com.android.internal.util.AsyncChannel;
import com.android.server.wifi.ActiveModeManager;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.WifiStateMachinePrime;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import mediatek.net.wifi.HotspotClient;

public abstract class MtkWifiServiceImpl extends IWifiManager.Stub {
    private static final String TAG = "MtkWifiService";
    private final Context mContext;
    private final WifiInjector mWifiInjector;

    public MtkWifiServiceImpl(Context context, WifiInjector wifiInjector, AsyncChannel asyncChannel) {
        this.mContext = context;
        this.mWifiInjector = wifiInjector;
    }

    private void enforceAccessPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_WIFI_STATE", "WifiService");
    }

    private void enforceChangePermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CHANGE_WIFI_STATE", "WifiService");
    }

    public List<HotspotClient> getHotspotClients() {
        enforceAccessPermission();
        Slog.d(TAG, "getHotspotClients");
        MtkSoftApManager mtkSoftApManager = getMtkSoftApManager();
        if (mtkSoftApManager != null) {
            return mtkSoftApManager.getHotspotClientsList();
        }
        return new ArrayList();
    }

    private ArrayList<String> readClientList(String str) throws Throwable {
        String str2;
        StringBuilder sb;
        FileInputStream fileInputStream;
        ArrayList<String> arrayList = new ArrayList<>();
        FileInputStream fileInputStream2 = null;
        try {
            try {
                fileInputStream = new FileInputStream(str);
            } catch (IOException e) {
                e = e;
            }
        } catch (Throwable th) {
            th = th;
        }
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new DataInputStream(fileInputStream)));
            while (true) {
                String line = bufferedReader.readLine();
                if (line == null || line.length() == 0) {
                    try {
                        break;
                    } catch (IOException e2) {
                        e = e2;
                        str2 = TAG;
                        sb = new StringBuilder();
                        sb.append("IOException:");
                        sb.append(e);
                        Slog.e(str2, sb.toString());
                    }
                } else {
                    arrayList.add(line);
                }
            }
        } catch (IOException e3) {
            e = e3;
            fileInputStream2 = fileInputStream;
            Slog.e(TAG, "IOException:" + e);
            if (fileInputStream2 != null) {
                try {
                    fileInputStream2.close();
                } catch (IOException e4) {
                    e = e4;
                    str2 = TAG;
                    sb = new StringBuilder();
                    sb.append("IOException:");
                    sb.append(e);
                    Slog.e(str2, sb.toString());
                }
            }
        } catch (Throwable th2) {
            th = th2;
            fileInputStream2 = fileInputStream;
            if (fileInputStream2 != null) {
                try {
                    fileInputStream2.close();
                } catch (IOException e5) {
                    Slog.e(TAG, "IOException:" + e5);
                }
            }
            throw th;
        }
        return arrayList;
    }

    public String getClientIp(String str) {
        enforceAccessPermission();
        Slog.d(TAG, "getClientIp deviceAddress = " + str);
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        for (String str2 : readClientList("/data/misc/dhcp/dnsmasq.leases")) {
            if (str2.indexOf(str) != -1) {
                String[] strArrSplit = str2.split(" ");
                if (strArrSplit.length > 3) {
                    return strArrSplit[2];
                }
            }
        }
        return null;
    }

    public String getClientDeviceName(String str) {
        enforceAccessPermission();
        Slog.d(TAG, "getClientDeviceName deviceAddress = " + str);
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        for (String str2 : readClientList("/data/misc/dhcp/dnsmasq.leases")) {
            if (str2.indexOf(str) != -1) {
                String[] strArrSplit = str2.split(" ");
                if (strArrSplit.length > 4) {
                    return strArrSplit[3];
                }
            }
        }
        return null;
    }

    public boolean blockClient(HotspotClient hotspotClient) {
        enforceChangePermission();
        Slog.d(TAG, "blockClient client = " + hotspotClient);
        if (hotspotClient == null || hotspotClient.deviceAddress == null) {
            Slog.e(TAG, "Client is null!");
            return false;
        }
        MtkSoftApManager mtkSoftApManager = getMtkSoftApManager();
        if (mtkSoftApManager == null) {
            return false;
        }
        return mtkSoftApManager.syncBlockClient(hotspotClient);
    }

    public boolean unblockClient(HotspotClient hotspotClient) {
        enforceChangePermission();
        Slog.d(TAG, "unblockClient client = " + hotspotClient);
        if (hotspotClient == null || hotspotClient.deviceAddress == null) {
            Slog.e(TAG, "Client is null!");
            return false;
        }
        MtkSoftApManager mtkSoftApManager = getMtkSoftApManager();
        if (mtkSoftApManager == null) {
            return false;
        }
        return mtkSoftApManager.syncUnblockClient(hotspotClient);
    }

    public boolean isAllDevicesAllowed() {
        enforceAccessPermission();
        Slog.d(TAG, "isAllDevicesAllowed");
        return Settings.System.getInt(this.mContext.getContentResolver(), "wifi_hotspot_is_all_devices_allowed", 1) == 1;
    }

    public boolean setAllDevicesAllowed(boolean z, boolean z2) {
        enforceChangePermission();
        Slog.d(TAG, "setAllDevicesAllowed enabled = " + z + " allowAllConnectedDevices = " + z2);
        Settings.System.putInt(this.mContext.getContentResolver(), "wifi_hotspot_is_all_devices_allowed", z ? 1 : 0);
        MtkSoftApManager mtkSoftApManager = getMtkSoftApManager();
        if (mtkSoftApManager != null) {
            mtkSoftApManager.syncSetAllDevicesAllowed(z, z2);
            return true;
        }
        return true;
    }

    public boolean allowDevice(String str, String str2) {
        enforceChangePermission();
        StringBuilder sb = new StringBuilder();
        sb.append("allowDevice address = ");
        sb.append(str);
        sb.append(", name = ");
        sb.append(str2);
        sb.append("is null?");
        sb.append(str2 == null);
        Slog.d(TAG, sb.toString());
        if (str == null) {
            Slog.e(TAG, "deviceAddress is null!");
            return false;
        }
        MtkSoftApManager.addDeviceToAllowedList(new HotspotClient(str, false, str2));
        MtkSoftApManager mtkSoftApManager = getMtkSoftApManager();
        if (mtkSoftApManager != null) {
            mtkSoftApManager.syncAllowDevice(str);
        }
        return true;
    }

    public boolean disallowDevice(String str) {
        enforceChangePermission();
        Slog.d(TAG, "disallowDevice address = " + str);
        if (str == null) {
            Slog.e(TAG, "deviceAddress is null!");
            return false;
        }
        MtkSoftApManager.removeDeviceFromAllowedList(str);
        MtkSoftApManager mtkSoftApManager = getMtkSoftApManager();
        if (mtkSoftApManager != null) {
            mtkSoftApManager.syncDisallowDevice(str);
            return true;
        }
        return true;
    }

    public List<HotspotClient> getAllowedDevices() {
        enforceAccessPermission();
        Slog.d(TAG, "getAllowedDevices");
        return MtkSoftApManager.getAllowedDevices();
    }

    private MtkSoftApManager getMtkSoftApManager() {
        WifiStateMachinePrime wifiStateMachinePrime = this.mWifiInjector.getWifiStateMachinePrime();
        try {
            Field declaredField = wifiStateMachinePrime.getClass().getSuperclass().getDeclaredField("mActiveModeManagers");
            declaredField.setAccessible(true);
            for (ActiveModeManager activeModeManager : (ArraySet) declaredField.get(wifiStateMachinePrime)) {
                if (activeModeManager instanceof MtkSoftApManager) {
                    return (MtkSoftApManager) activeModeManager;
                }
            }
            return null;
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            return null;
        }
    }
}
