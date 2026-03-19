package com.android.server.wifi;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.TrafficStats;
import android.net.Uri;
import android.net.ip.IpClient;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import com.android.internal.app.IBatteryStats;
import com.android.server.wifi.util.WifiAsyncChannel;

public class FrameworkFacade {
    public static final String TAG = "FrameworkFacade";

    public boolean setIntegerSetting(Context context, String str, int i) {
        return Settings.Global.putInt(context.getContentResolver(), str, i);
    }

    public int getIntegerSetting(Context context, String str, int i) {
        return Settings.Global.getInt(context.getContentResolver(), str, i);
    }

    public long getLongSetting(Context context, String str, long j) {
        return Settings.Global.getLong(context.getContentResolver(), str, j);
    }

    public boolean setStringSetting(Context context, String str, String str2) {
        return Settings.Global.putString(context.getContentResolver(), str, str2);
    }

    public String getStringSetting(Context context, String str) {
        return Settings.Global.getString(context.getContentResolver(), str);
    }

    public int getSecureIntegerSetting(Context context, String str, int i) {
        return Settings.Secure.getInt(context.getContentResolver(), str, i);
    }

    public void registerContentObserver(Context context, Uri uri, boolean z, ContentObserver contentObserver) {
        context.getContentResolver().registerContentObserver(uri, z, contentObserver);
    }

    public void unregisterContentObserver(Context context, ContentObserver contentObserver) {
        context.getContentResolver().unregisterContentObserver(contentObserver);
    }

    public IBinder getService(String str) {
        return ServiceManager.getService(str);
    }

    public IBatteryStats getBatteryService() {
        return IBatteryStats.Stub.asInterface(getService("batterystats"));
    }

    public PendingIntent getBroadcast(Context context, int i, Intent intent, int i2) {
        return PendingIntent.getBroadcast(context, i, intent, i2);
    }

    public PendingIntent getActivity(Context context, int i, Intent intent, int i2) {
        return PendingIntent.getActivity(context, i, intent, i2);
    }

    public SupplicantStateTracker makeSupplicantStateTracker(Context context, WifiConfigManager wifiConfigManager, Handler handler) {
        return new SupplicantStateTracker(context, wifiConfigManager, this, handler);
    }

    public boolean getConfigWiFiDisableInECBM(Context context) {
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) context.getSystemService("carrier_config");
        if (carrierConfigManager != null) {
            return carrierConfigManager.getConfig().getBoolean("config_wifi_disable_in_ecbm");
        }
        return true;
    }

    public WifiApConfigStore makeApConfigStore(Context context, BackupManagerProxy backupManagerProxy) {
        return new WifiApConfigStore(context, backupManagerProxy);
    }

    public long getTxPackets(String str) {
        return TrafficStats.getTxPackets(str);
    }

    public long getRxPackets(String str) {
        return TrafficStats.getRxPackets(str);
    }

    public IpClient makeIpClient(Context context, String str, IpClient.Callback callback) {
        return new IpClient(context, str, callback);
    }

    public int checkUidPermission(String str, int i) throws RemoteException {
        return AppGlobals.getPackageManager().checkUidPermission(str, i);
    }

    public WifiAsyncChannel makeWifiAsyncChannel(String str) {
        return new WifiAsyncChannel(str);
    }

    public boolean inStorageManagerCryptKeeperBounce() {
        return StorageManager.inCryptKeeperBounce();
    }

    public boolean isAppForeground(int i) throws RemoteException {
        return ActivityManager.getService().isAppForeground(i);
    }

    public Notification.Builder makeNotificationBuilder(Context context, String str) {
        return new Notification.Builder(context, str);
    }
}
