package com.android.server.wifi;

import android.R;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.os.Environment;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Log;
import com.android.server.wifi.hotspot2.omadm.PasspointManagementObjectManager;
import com.mediatek.common.MPlugin;
import com.mediatek.common.wifi.IWifiFwkExt;
import com.mediatek.custom.CustomProperties;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

public class WifiApConfigStore {
    private static final int AP_CONFIG_FILE_VERSION = 2;
    private static final String DEFAULT_AP_CONFIG_FILE = Environment.getDataDirectory() + "/misc/wifi/softap.conf";
    private static final String TAG = "WifiApConfigStore";
    private ArrayList<Integer> mAllowed2GChannel;
    private final String mApConfigFile;
    private final BackupManagerProxy mBackupManagerProxy;
    private final Context mContext;
    private WifiConfiguration mWifiApConfig;

    WifiApConfigStore(Context context, BackupManagerProxy backupManagerProxy) {
        this(context, backupManagerProxy, DEFAULT_AP_CONFIG_FILE);
    }

    WifiApConfigStore(Context context, BackupManagerProxy backupManagerProxy, String apConfigFile) throws Throwable {
        this.mWifiApConfig = null;
        this.mAllowed2GChannel = null;
        this.mContext = context;
        this.mBackupManagerProxy = backupManagerProxy;
        this.mApConfigFile = apConfigFile;
        String ap2GChannelListStr = this.mContext.getResources().getString(R.string.config_systemCompanionDeviceProvider);
        Log.d(TAG, "2G band allowed channels are:" + ap2GChannelListStr);
        if (ap2GChannelListStr != null) {
            this.mAllowed2GChannel = new ArrayList<>();
            String[] channelList = ap2GChannelListStr.split(",");
            for (String tmp : channelList) {
                this.mAllowed2GChannel.add(Integer.valueOf(Integer.parseInt(tmp)));
            }
        }
        this.mWifiApConfig = loadApConfiguration(this.mApConfigFile);
        if (this.mWifiApConfig != null) {
            return;
        }
        Log.d(TAG, "Fallback to use default AP configuration");
        this.mWifiApConfig = getDefaultApConfiguration();
        writeApConfiguration(this.mApConfigFile, this.mWifiApConfig);
    }

    public synchronized WifiConfiguration getApConfiguration() {
        return this.mWifiApConfig;
    }

    public synchronized void setApConfiguration(WifiConfiguration config) {
        if (config == null) {
            this.mWifiApConfig = getDefaultApConfiguration();
        } else {
            this.mWifiApConfig = config;
        }
        writeApConfiguration(this.mApConfigFile, this.mWifiApConfig);
        this.mBackupManagerProxy.notifyDataChanged();
    }

    public ArrayList<Integer> getAllowed2GChannel() {
        return this.mAllowed2GChannel;
    }

    private static WifiConfiguration loadApConfiguration(String filename) throws Throwable {
        WifiConfiguration wifiConfiguration;
        WifiConfiguration config;
        DataInputStream in;
        int version;
        DataInputStream in2 = null;
        try {
            try {
                config = new WifiConfiguration();
                try {
                    in = new DataInputStream(new BufferedInputStream(new FileInputStream(filename)));
                } catch (IOException e) {
                    e = e;
                } catch (Throwable th) {
                    th = th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        } catch (IOException e2) {
            e = e2;
        }
        try {
            version = in.readInt();
        } catch (IOException e3) {
            e = e3;
            in2 = in;
            Log.e(TAG, "Error reading hotspot configuration " + e);
            wifiConfiguration = null;
            if (in2 != null) {
                try {
                    in2.close();
                } catch (IOException e4) {
                    Log.e(TAG, "Error closing hotspot configuration during read" + e4);
                }
            }
        } catch (Throwable th3) {
            th = th3;
            in2 = in;
            if (in2 != null) {
                try {
                    in2.close();
                } catch (IOException e5) {
                    Log.e(TAG, "Error closing hotspot configuration during read" + e5);
                }
            }
            throw th;
        }
        if (version != 1 && version != 2) {
            Log.e(TAG, "Bad version on hotspot configuration file");
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e6) {
                    Log.e(TAG, "Error closing hotspot configuration during read" + e6);
                }
            }
            return null;
        }
        config.SSID = in.readUTF();
        if (version >= 2) {
            config.apBand = in.readInt();
            config.apChannel = in.readInt();
        }
        int authType = in.readInt();
        config.allowedKeyManagement.set(authType);
        if (authType != 0) {
            config.preSharedKey = in.readUTF();
        }
        if (in != null) {
            try {
                in.close();
            } catch (IOException e7) {
                Log.e(TAG, "Error closing hotspot configuration during read" + e7);
            }
        }
        in2 = in;
        wifiConfiguration = config;
        return wifiConfiguration;
    }

    private static void writeApConfiguration(String filename, WifiConfiguration config) throws Throwable {
        DataOutputStream out;
        Throwable th = null;
        DataOutputStream out2 = null;
        try {
            out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename)));
        } catch (Throwable th2) {
            th = th2;
        }
        try {
            out.writeInt(2);
            out.writeUTF(config.SSID);
            out.writeInt(config.apBand);
            out.writeInt(config.apChannel);
            int authType = config.getAuthType();
            out.writeInt(authType);
            if (authType != 0) {
                out.writeUTF(config.preSharedKey);
            }
            if (out != null) {
                try {
                    try {
                        out.close();
                    } catch (IOException e) {
                        e = e;
                        Log.e(TAG, "Error writing hotspot configuration" + e);
                        return;
                    }
                } catch (Throwable th3) {
                    th = th3;
                }
            }
            if (th != null) {
                throw th;
            }
        } catch (Throwable th4) {
            th = th4;
            out2 = out;
            if (out2 != null) {
            }
            if (th != null) {
            }
        }
    }

    private WifiConfiguration getDefaultApConfiguration() {
        WifiConfiguration config = new WifiConfiguration();
        IWifiFwkExt wifiFwkExt = (IWifiFwkExt) MPlugin.createInstance(IWifiFwkExt.class.getName(), this.mContext);
        if (SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
            if (wifiFwkExt != null) {
                config.SSID = wifiFwkExt.getApDefaultSsid();
            } else {
                config.SSID = this.mContext.getResources().getString(R.string.ext_media_unmount_action);
            }
        } else {
            config.SSID = CustomProperties.getString("wlan", PasspointManagementObjectManager.TAG_SSID, this.mContext.getString(R.string.ext_media_unmount_action));
            if (wifiFwkExt != null && wifiFwkExt.needRandomSsid()) {
                Random random = new Random(SystemClock.elapsedRealtime());
                config.SSID += random.nextInt(1000);
                Log.d(TAG, "setDefaultApConfiguration, SSID:" + config.SSID);
            }
        }
        config.allowedKeyManagement.set(4);
        String randomUUID = UUID.randomUUID().toString();
        config.preSharedKey = randomUUID.substring(0, 8) + randomUUID.substring(9, 13);
        return config;
    }
}
