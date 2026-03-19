package mediatek.net.wifi;

import android.net.wifi.IWifiManager;
import android.os.RemoteException;
import java.util.List;

public class WifiHotspotManager {
    public static final String EXTRA_DEVICE_ADDRESS = "deviceAddress";
    public static final String EXTRA_DEVICE_NAME = "deviceName";
    public static final String EXTRA_IP_ADDRESS = "ipAddress";
    private static final String TAG = "WifiHotspotManager";
    public static final String WIFI_HOTSPOT_CLIENTS_CHANGED_ACTION = "android.net.wifi.WIFI_HOTSPOT_CLIENTS_CHANGED";
    IWifiManager mService;

    public WifiHotspotManager(IWifiManager iWifiManager) {
        this.mService = iWifiManager;
    }

    public List<HotspotClient> getHotspotClients() {
        try {
            return this.mService.getHotspotClients();
        } catch (RemoteException e) {
            return null;
        }
    }

    public String getClientIp(String str) {
        try {
            return this.mService.getClientIp(str);
        } catch (RemoteException e) {
            return null;
        }
    }

    public String getClientDeviceName(String str) {
        try {
            return this.mService.getClientDeviceName(str);
        } catch (RemoteException e) {
            return null;
        }
    }

    public boolean blockClient(HotspotClient hotspotClient) {
        try {
            return this.mService.blockClient(hotspotClient);
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean unblockClient(HotspotClient hotspotClient) {
        try {
            return this.mService.unblockClient(hotspotClient);
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean isAllDevicesAllowed() {
        try {
            return this.mService.isAllDevicesAllowed();
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean setAllDevicesAllowed(boolean z, boolean z2) {
        try {
            return this.mService.setAllDevicesAllowed(z, z2);
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean allowDevice(String str, String str2) {
        try {
            return this.mService.allowDevice(str, str2);
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean disallowDevice(String str) {
        try {
            return this.mService.disallowDevice(str);
        } catch (RemoteException e) {
            return false;
        }
    }

    public List<HotspotClient> getAllowedDevices() {
        try {
            return this.mService.getAllowedDevices();
        } catch (RemoteException e) {
            return null;
        }
    }
}
