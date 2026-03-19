package com.android.settingslib.applications;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageItemInfo;
import android.os.SystemProperties;
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
        if (str != null && !str.isEmpty() && ((PackageItemInfo) applicationInfo).packageName != null && (strArrSplit = str.split(",")) != null) {
            for (String str2 : strArrSplit) {
                if (((PackageItemInfo) applicationInfo).packageName.contains(str2)) {
                    return true;
                }
            }
        }
        return false;
    }
}
