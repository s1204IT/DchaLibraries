package com.android.launcher3.util;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.text.TextUtils;
import com.android.launcher3.Utilities;

public class PackageManagerHelper {
    public static boolean isAppOnSdcard(PackageManager pm, String packageName) {
        return isAppEnabled(pm, packageName, 8192);
    }

    public static boolean isAppEnabled(PackageManager pm, String packageName) {
        return isAppEnabled(pm, packageName, 0);
    }

    public static boolean isAppEnabled(PackageManager pm, String packageName, int flags) {
        try {
            ApplicationInfo info = pm.getApplicationInfo(packageName, flags);
            if (info != null) {
                return info.enabled;
            }
            return false;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static boolean isAppSuspended(PackageManager pm, String packageName) {
        try {
            ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
            if (info != null) {
                return isAppSuspended(info);
            }
            return false;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static boolean isAppSuspended(ApplicationInfo info) {
        return Utilities.ATLEAST_N && (info.flags & 1073741824) != 0;
    }

    public static boolean hasPermissionForActivity(Context context, Intent intent, String srcPackage) {
        PackageManager pm = context.getPackageManager();
        ResolveInfo target = pm.resolveActivity(intent, 0);
        if (target == null) {
            return false;
        }
        if (TextUtils.isEmpty(target.activityInfo.permission)) {
            return true;
        }
        if (TextUtils.isEmpty(srcPackage) || pm.checkPermission(target.activityInfo.permission, srcPackage) != 0) {
            return false;
        }
        if (!Utilities.ATLEAST_MARSHMALLOW || TextUtils.isEmpty(AppOpsManager.permissionToOp(target.activityInfo.permission))) {
            return true;
        }
        try {
            return pm.getApplicationInfo(srcPackage, 0).targetSdkVersion >= 23;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
