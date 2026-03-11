package com.android.settingslib.applications;

import android.content.ComponentName;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.IUsbManager;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import com.android.settingslib.R$string;
import com.android.settingslib.applications.ApplicationsState;
import java.util.ArrayList;
import java.util.List;

public class AppUtils {
    public static CharSequence getLaunchByDefaultSummary(ApplicationsState.AppEntry appEntry, IUsbManager usbManager, PackageManager pm, Context context) {
        boolean zHasUsbDefaults;
        int i;
        String packageName = appEntry.info.packageName;
        if (hasPreferredActivities(pm, packageName)) {
            zHasUsbDefaults = true;
        } else {
            zHasUsbDefaults = hasUsbDefaults(usbManager, packageName);
        }
        int status = pm.getIntentVerificationStatusAsUser(packageName, UserHandle.myUserId());
        boolean hasDomainURLsPreference = status != 0;
        if (zHasUsbDefaults || hasDomainURLsPreference) {
            i = R$string.launch_defaults_some;
        } else {
            i = R$string.launch_defaults_none;
        }
        return context.getString(i);
    }

    public static boolean hasUsbDefaults(IUsbManager usbManager, String packageName) {
        if (usbManager != null) {
            try {
                return usbManager.hasDefaults(packageName, UserHandle.myUserId());
            } catch (RemoteException e) {
                Log.e("AppUtils", "mUsbManager.hasDefaults", e);
                return false;
            }
        }
        return false;
    }

    public static boolean hasPreferredActivities(PackageManager pm, String packageName) {
        List<ComponentName> prefActList = new ArrayList<>();
        List<IntentFilter> intentList = new ArrayList<>();
        pm.getPreferredActivities(intentList, prefActList, packageName);
        Log.d("AppUtils", "Have " + prefActList.size() + " number of activities in preferred list");
        return prefActList.size() > 0;
    }
}
