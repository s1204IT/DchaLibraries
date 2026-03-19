package com.android.settingslib.applications;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.SystemProperties;
import android.util.Log;
import com.android.settingslib.applications.instantapps.InstantAppDataProvider;

public class AppUtils {
    private static InstantAppDataProvider sInstantAppDataProvider = null;

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

    public static CharSequence getApplicationLabel(PackageManager packageManager, String str) {
        try {
            return packageManager.getApplicationInfo(str, 4194816).loadLabel(packageManager);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w("AppUtils", "Unable to find info for package: " + str);
            return null;
        }
    }
}
