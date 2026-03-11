package jp.co.benesse.dcha.util;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiConfiguration;
import android.provider.Settings;

public class WifiSettings {
    public static boolean isEditabilityLockedDown(Context context, WifiConfiguration wifiConfiguration) {
        return !canModifyNetwork(context, wifiConfiguration);
    }

    private static boolean canModifyNetwork(Context context, WifiConfiguration wifiConfiguration) {
        boolean z;
        ComponentName deviceOwnerComponentOnAnyUser;
        if (wifiConfiguration == null) {
            return true;
        }
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) context.getSystemService("device_policy");
        PackageManager packageManager = context.getPackageManager();
        if (packageManager.hasSystemFeature("android.software.device_admin") && devicePolicyManager == null) {
            return false;
        }
        if (devicePolicyManager != null && (deviceOwnerComponentOnAnyUser = devicePolicyManager.getDeviceOwnerComponentOnAnyUser()) != null) {
            try {
                z = packageManager.getPackageUidAsUser(deviceOwnerComponentOnAnyUser.getPackageName(), devicePolicyManager.getDeviceOwnerUserId()) == wifiConfiguration.creatorUid;
            } catch (PackageManager.NameNotFoundException e) {
                z = false;
            }
        } else {
            z = false;
        }
        if (!z) {
            return true;
        }
        return !(Settings.Global.getInt(context.getContentResolver(), "wifi_device_owner_configs_lockdown", 0) != 0);
    }
}
