package com.android.settings.wifi;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiConfiguration;
import android.provider.Settings;
import android.text.TextUtils;
import com.android.settingslib.wrapper.PackageManagerWrapper;

/* loaded from: classes.dex */
public class WifiUtils {
    public static boolean isSSIDTooLong(String str) {
        return !TextUtils.isEmpty(str) && str.length() > 32;
    }

    public static boolean isSSIDTooShort(String str) {
        return TextUtils.isEmpty(str) || str.length() < 1;
    }

    public static boolean isHotspotPasswordValid(String str) {
        int length;
        return !TextUtils.isEmpty(str) && (length = str.length()) >= 8 && length <= 63;
    }

    public static boolean isNetworkLockedDown(Context context, WifiConfiguration wifiConfiguration) {
        boolean z;
        ComponentName deviceOwnerComponentOnAnyUser;
        if (wifiConfiguration == null) {
            return false;
        }
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) context.getSystemService("device_policy");
        PackageManagerWrapper packageManagerWrapper = new PackageManagerWrapper(context.getPackageManager());
        if (packageManagerWrapper.hasSystemFeature("android.software.device_admin") && devicePolicyManager == null) {
            return true;
        }
        if (devicePolicyManager != null && (deviceOwnerComponentOnAnyUser = devicePolicyManager.getDeviceOwnerComponentOnAnyUser()) != null) {
            try {
                z = packageManagerWrapper.getPackageUidAsUser(deviceOwnerComponentOnAnyUser.getPackageName(), devicePolicyManager.getDeviceOwnerUserId()) == wifiConfiguration.creatorUid;
            } catch (PackageManager.NameNotFoundException e) {
            }
        } else {
            z = false;
        }
        return z && Settings.Global.getInt(context.getContentResolver(), "wifi_device_owner_configs_lockdown", 0) != 0;
    }

    public static boolean canSignIntoNetwork(NetworkCapabilities networkCapabilities) {
        return networkCapabilities != null && networkCapabilities.hasCapability(17);
    }
}
