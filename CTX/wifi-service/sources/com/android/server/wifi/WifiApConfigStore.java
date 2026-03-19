package com.android.server.wifi;

import android.R;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

public class WifiApConfigStore {

    @VisibleForTesting
    static final int AP_CHANNEL_DEFAULT = 0;
    private static final int AP_CONFIG_FILE_VERSION = 2;
    private static final String DEFAULT_AP_CONFIG_FILE = Environment.getDataDirectory() + "/misc/wifi/softap.conf";

    @VisibleForTesting
    static final int PSK_MAX_LEN = 63;

    @VisibleForTesting
    static final int PSK_MIN_LEN = 8;
    private static final int RAND_SSID_INT_MAX = 9999;
    private static final int RAND_SSID_INT_MIN = 1000;

    @VisibleForTesting
    static final int SSID_MAX_LEN = 32;

    @VisibleForTesting
    static final int SSID_MIN_LEN = 1;
    private static final String TAG = "WifiApConfigStore";
    private ArrayList<Integer> mAllowed2GChannel;
    private final String mApConfigFile;
    private final BackupManagerProxy mBackupManagerProxy;
    private final Context mContext;
    private boolean mRequiresApBandConversion;
    private WifiConfiguration mWifiApConfig;

    WifiApConfigStore(Context context, BackupManagerProxy backupManagerProxy) {
        this(context, backupManagerProxy, DEFAULT_AP_CONFIG_FILE);
    }

    WifiApConfigStore(Context context, BackupManagerProxy backupManagerProxy, String str) {
        this.mWifiApConfig = null;
        this.mAllowed2GChannel = null;
        this.mRequiresApBandConversion = false;
        this.mContext = context;
        this.mBackupManagerProxy = backupManagerProxy;
        this.mApConfigFile = str;
        String string = this.mContext.getResources().getString(R.string.app_category_news);
        Log.d(TAG, "2G band allowed channels are:" + string);
        if (string != null) {
            this.mAllowed2GChannel = new ArrayList<>();
            for (String str2 : string.split(",")) {
                this.mAllowed2GChannel.add(Integer.valueOf(Integer.parseInt(str2)));
            }
        }
        this.mRequiresApBandConversion = this.mContext.getResources().getBoolean(R.^attr-private.preferenceFrameLayoutStyle);
        this.mWifiApConfig = loadApConfiguration(this.mApConfigFile);
        if (this.mWifiApConfig == null) {
            Log.d(TAG, "Fallback to use default AP configuration");
            this.mWifiApConfig = getDefaultApConfiguration();
            writeApConfiguration(this.mApConfigFile, this.mWifiApConfig);
        }
    }

    public synchronized WifiConfiguration getApConfiguration() {
        WifiConfiguration wifiConfigurationApBandCheckConvert = apBandCheckConvert(this.mWifiApConfig);
        if (this.mWifiApConfig != wifiConfigurationApBandCheckConvert) {
            Log.d(TAG, "persisted config was converted, need to resave it");
            this.mWifiApConfig = wifiConfigurationApBandCheckConvert;
            persistConfigAndTriggerBackupManagerProxy(this.mWifiApConfig);
        }
        return this.mWifiApConfig;
    }

    public synchronized void setApConfiguration(WifiConfiguration wifiConfiguration) {
        try {
            if (wifiConfiguration == null) {
                this.mWifiApConfig = getDefaultApConfiguration();
            } else {
                this.mWifiApConfig = apBandCheckConvert(wifiConfiguration);
            }
            persistConfigAndTriggerBackupManagerProxy(this.mWifiApConfig);
        } catch (Throwable th) {
            throw th;
        }
    }

    public ArrayList<Integer> getAllowed2GChannel() {
        return this.mAllowed2GChannel;
    }

    private WifiConfiguration apBandCheckConvert(WifiConfiguration wifiConfiguration) {
        if (this.mRequiresApBandConversion) {
            if (wifiConfiguration.apBand == 1) {
                Log.w(TAG, "Supplied ap config band was 5GHz only, converting to ANY");
                WifiConfiguration wifiConfiguration2 = new WifiConfiguration(wifiConfiguration);
                wifiConfiguration2.apBand = -1;
                wifiConfiguration2.apChannel = 0;
                return wifiConfiguration2;
            }
        } else if (wifiConfiguration.apBand == -1) {
            Log.w(TAG, "Supplied ap config band was ANY, converting to 5GHz");
            WifiConfiguration wifiConfiguration3 = new WifiConfiguration(wifiConfiguration);
            wifiConfiguration3.apBand = 1;
            wifiConfiguration3.apChannel = 0;
            return wifiConfiguration3;
        }
        return wifiConfiguration;
    }

    private void persistConfigAndTriggerBackupManagerProxy(WifiConfiguration wifiConfiguration) {
        writeApConfiguration(this.mApConfigFile, this.mWifiApConfig);
        this.mBackupManagerProxy.notifyDataChanged();
    }

    private static WifiConfiguration loadApConfiguration(String str) throws Throwable {
        DataInputStream dataInputStream;
        DataInputStream dataInputStream2 = null;
        try {
            try {
                WifiConfiguration wifiConfiguration = new WifiConfiguration();
                dataInputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(str)));
                try {
                    int i = dataInputStream.readInt();
                    if (i != 1 && i != 2) {
                        Log.e(TAG, "Bad version on hotspot configuration file");
                        try {
                            dataInputStream.close();
                        } catch (IOException e) {
                            Log.e(TAG, "Error closing hotspot configuration during read" + e);
                        }
                        return null;
                    }
                    wifiConfiguration.SSID = dataInputStream.readUTF();
                    if (i >= 2) {
                        wifiConfiguration.apBand = dataInputStream.readInt();
                        wifiConfiguration.apChannel = dataInputStream.readInt();
                    }
                    int i2 = dataInputStream.readInt();
                    wifiConfiguration.allowedKeyManagement.set(i2);
                    if (i2 != 0) {
                        wifiConfiguration.preSharedKey = dataInputStream.readUTF();
                    }
                    try {
                        dataInputStream.close();
                    } catch (IOException e2) {
                        Log.e(TAG, "Error closing hotspot configuration during read" + e2);
                    }
                    return wifiConfiguration;
                } catch (IOException e3) {
                    e = e3;
                    Log.e(TAG, "Error reading hotspot configuration " + e);
                    if (dataInputStream == null) {
                        return null;
                    }
                    try {
                        dataInputStream.close();
                        return null;
                    } catch (IOException e4) {
                        Log.e(TAG, "Error closing hotspot configuration during read" + e4);
                        return null;
                    }
                }
            } catch (Throwable th) {
                th = th;
                if (0 != 0) {
                    try {
                        dataInputStream2.close();
                    } catch (IOException e5) {
                        Log.e(TAG, "Error closing hotspot configuration during read" + e5);
                    }
                }
                throw th;
            }
        } catch (IOException e6) {
            e = e6;
            dataInputStream = null;
        } catch (Throwable th2) {
            th = th2;
            if (0 != 0) {
            }
            throw th;
        }
    }

    private static void writeApConfiguration(String str, WifiConfiguration wifiConfiguration) {
        try {
            DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(str)));
            try {
                dataOutputStream.writeInt(2);
                dataOutputStream.writeUTF(wifiConfiguration.SSID);
                dataOutputStream.writeInt(wifiConfiguration.apBand);
                dataOutputStream.writeInt(wifiConfiguration.apChannel);
                int authType = wifiConfiguration.getAuthType();
                dataOutputStream.writeInt(authType);
                if (authType != 0) {
                    dataOutputStream.writeUTF(wifiConfiguration.preSharedKey);
                }
                dataOutputStream.close();
            } finally {
            }
        } catch (IOException e) {
            Log.e(TAG, "Error writing hotspot configuration" + e);
        }
    }

    private WifiConfiguration getDefaultApConfiguration() {
        WifiConfiguration wifiConfiguration = new WifiConfiguration();
        wifiConfiguration.apBand = 0;
        wifiConfiguration.SSID = this.mContext.getResources().getString(R.string.notification_feedback_indicator) + "_" + getRandomIntForDefaultSsid();
        wifiConfiguration.allowedKeyManagement.set(4);
        String string = UUID.randomUUID().toString();
        wifiConfiguration.preSharedKey = string.substring(0, 8) + string.substring(9, 13);
        return wifiConfiguration;
    }

    private static int getRandomIntForDefaultSsid() {
        return new Random().nextInt(9000) + RAND_SSID_INT_MIN;
    }

    public static WifiConfiguration generateLocalOnlyHotspotConfig(Context context) {
        WifiConfiguration wifiConfiguration = new WifiConfiguration();
        wifiConfiguration.apBand = 0;
        wifiConfiguration.SSID = context.getResources().getString(R.string.notification_channel_retail_mode) + "_" + getRandomIntForDefaultSsid();
        wifiConfiguration.allowedKeyManagement.set(4);
        wifiConfiguration.networkId = -2;
        String string = UUID.randomUUID().toString();
        wifiConfiguration.preSharedKey = string.substring(0, 8) + string.substring(9, 13);
        return wifiConfiguration;
    }

    private static boolean validateApConfigSsid(String str) {
        if (TextUtils.isEmpty(str)) {
            Log.d(TAG, "SSID for softap configuration must be set.");
            return false;
        }
        if (str.length() < 1 || str.length() > 32) {
            Log.d(TAG, "SSID for softap configuration string size must be at least 1 and not more than 32");
            return false;
        }
        try {
            str.getBytes(StandardCharsets.UTF_8);
            return true;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "softap config SSID verification failed: malformed string " + str);
            return false;
        }
    }

    private static boolean validateApConfigPreSharedKey(String str) {
        if (str.length() < 8 || str.length() > 63) {
            Log.d(TAG, "softap network password string size must be at least 8 and no more than 63");
            return false;
        }
        try {
            str.getBytes(StandardCharsets.UTF_8);
            return true;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "softap network password verification failed: malformed string");
            return false;
        }
    }

    static boolean validateApWifiConfiguration(WifiConfiguration wifiConfiguration) {
        if (!validateApConfigSsid(wifiConfiguration.SSID)) {
            return false;
        }
        if (wifiConfiguration.allowedKeyManagement == null) {
            Log.d(TAG, "softap config key management bitset was null");
            return false;
        }
        String str = wifiConfiguration.preSharedKey;
        boolean z = !TextUtils.isEmpty(str);
        try {
            int authType = wifiConfiguration.getAuthType();
            if (authType == 0) {
                if (z) {
                    Log.d(TAG, "open softap network should not have a password");
                    return false;
                }
            } else if (authType == 4) {
                if (!z) {
                    Log.d(TAG, "softap network password must be set");
                    return false;
                }
                if (!validateApConfigPreSharedKey(str)) {
                    return false;
                }
            } else {
                Log.d(TAG, "softap configs must either be open or WPA2 PSK networks");
                return false;
            }
            return true;
        } catch (IllegalStateException e) {
            Log.d(TAG, "Unable to get AuthType for softap config: " + e.getMessage());
            return false;
        }
    }
}
