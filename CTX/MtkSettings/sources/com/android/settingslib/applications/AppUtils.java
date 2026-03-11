package com.android.settingslib.applications;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.usb.IUsbManager;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Log;
import com.android.settingslib.R;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.instantapps.InstantAppDataProvider;
import java.util.ArrayList;

public class AppUtils {
    private static InstantAppDataProvider sInstantAppDataProvider = null;

    public static CharSequence getLaunchByDefaultSummary(ApplicationsState.AppEntry appEntry, IUsbManager iUsbManager, PackageManager packageManager, Context context) {
        int i;
        String str = appEntry.info.packageName;
        boolean z = hasPreferredActivities(packageManager, str) || hasUsbDefaults(iUsbManager, str);
        boolean z2 = packageManager.getIntentVerificationStatusAsUser(str, UserHandle.myUserId()) != 0;
        if (z || z2) {
            i = R.string.launch_defaults_some;
        } else {
            i = R.string.launch_defaults_none;
        }
        return context.getString(i);
    }

    public static boolean hasUsbDefaults(IUsbManager iUsbManager, String str) {
        if (iUsbManager != null) {
            try {
                return iUsbManager.hasDefaults(str, UserHandle.myUserId());
            } catch (RemoteException e) {
                Log.e("AppUtils", "mUsbManager.hasDefaults", e);
                return false;
            }
        }
        return false;
    }

    public static boolean hasPreferredActivities(PackageManager packageManager, String str) {
        ArrayList arrayList = new ArrayList();
        packageManager.getPreferredActivities(new ArrayList(), arrayList, str);
        Log.d("AppUtils", "Have " + arrayList.size() + " number of activities in preferred list");
        return arrayList.size() > 0;
    }

    public static boolean isInstant(ApplicationInfo applicationInfo) {
        String[] strArrSplit;
        if (sInstantAppDataProvider != null) {
            if (sInstantAppDataProvider.isInstantApp(applicationInfo)) {
                return true;
            }
        } else if (applicationInfo.isInstantApp()) {
            return true;
        }
        String str = SystemProperties.get("settingsdebug.instant.packages");
        if (str != null && !str.isEmpty() && applicationInfo.packageName != null && (strArrSplit = str.split(",")) != null) {
            for (String str2 : strArrSplit) {
                if (applicationInfo.packageName.contains(str2)) {
                    return true;
                }
            }
        }
        return false;
    }
}
