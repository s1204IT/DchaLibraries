package com.android.providers.contacts.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

public class PackageUtils {
    public static boolean isPackageInstalled(Context context, String str) {
        try {
            ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo(str, 794624);
            if (applicationInfo != null) {
                return (applicationInfo.flags & 8388608) != 0;
            }
            return false;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
