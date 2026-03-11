package jp.co.benesse.dcha.util;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiConfiguration;
import android.provider.Settings;

public class WifiSettings {
    public static boolean isEditabilityLockedDown(Context context, WifiConfiguration config) {
        return !canModifyNetwork(context, config);
    }

    private static boolean canModifyNetwork(Context context, WifiConfiguration config) {
        ComponentName deviceOwner;
        if (config == null) {
            return true;
        }
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService("device_policy");
        PackageManager pm = context.getPackageManager();
        if (pm.hasSystemFeature("android.software.device_admin") && dpm == null) {
            return false;
        }
        boolean isConfigEligibleForLockdown = false;
        if (dpm != null && (deviceOwner = dpm.getDeviceOwnerComponentOnAnyUser()) != null) {
            int deviceOwnerUserId = dpm.getDeviceOwnerUserId();
            try {
                int deviceOwnerUid = pm.getPackageUidAsUser(deviceOwner.getPackageName(), deviceOwnerUserId);
                isConfigEligibleForLockdown = deviceOwnerUid == config.creatorUid;
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        if (!isConfigEligibleForLockdown) {
            return true;
        }
        ContentResolver resolver = context.getContentResolver();
        boolean isLockdownFeatureEnabled = Settings.Global.getInt(resolver, "wifi_device_owner_configs_lockdown", 0) != 0;
        return !isLockdownFeatureEnabled;
    }
}
