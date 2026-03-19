package com.android.server.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.util.Log;

public class AppInstallerUtil {
    private static final String LOG_TAG = "AppInstallerUtil";

    private static Intent resolveIntent(Context context, Intent intent) {
        ResolveInfo resolveInfoResolveActivity = context.getPackageManager().resolveActivity(intent, 0);
        if (resolveInfoResolveActivity != null) {
            return new Intent(intent.getAction()).setClassName(resolveInfoResolveActivity.activityInfo.packageName, resolveInfoResolveActivity.activityInfo.name);
        }
        return null;
    }

    public static String getInstallerPackageName(Context context, String str) {
        String installerPackageName;
        try {
            installerPackageName = context.getPackageManager().getInstallerPackageName(str);
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, "Exception while retrieving the package installer of " + str, e);
            installerPackageName = null;
        }
        if (installerPackageName == null) {
            return null;
        }
        return installerPackageName;
    }

    public static Intent createIntent(Context context, String str, String str2) {
        Intent intentResolveIntent = resolveIntent(context, new Intent("android.intent.action.SHOW_APP_INFO").setPackage(str));
        if (intentResolveIntent != null) {
            intentResolveIntent.putExtra("android.intent.extra.PACKAGE_NAME", str2);
            intentResolveIntent.addFlags(268435456);
            return intentResolveIntent;
        }
        return null;
    }

    public static Intent createIntent(Context context, String str) {
        return createIntent(context, getInstallerPackageName(context, str), str);
    }
}
