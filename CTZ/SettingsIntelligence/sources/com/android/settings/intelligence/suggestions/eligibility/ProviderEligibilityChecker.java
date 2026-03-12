package com.android.settings.intelligence.suggestions.eligibility;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.util.Log;

public class ProviderEligibilityChecker {
    public static boolean isEligible(Context context, String str, ResolveInfo resolveInfo) {
        return isSystemApp(str, resolveInfo) && isEnabledInMetadata(context, str, resolveInfo);
    }

    private static boolean isSystemApp(String str, ResolveInfo resolveInfo) {
        boolean z = (resolveInfo == null || resolveInfo.activityInfo == null || resolveInfo.activityInfo.applicationInfo == null || (resolveInfo.activityInfo.applicationInfo.flags & 1) == 0) ? false : true;
        if (!z) {
            Log.i("ProviderEligibility", str + " is not system app, not eligible for suggestion");
        }
        return z;
    }

    private static boolean isEnabledInMetadata(Context context, String str, ResolveInfo resolveInfo) {
        int i = resolveInfo.activityInfo.metaData.getInt("com.android.settings.is_supported");
        try {
            boolean z = i != 0 ? context.getPackageManager().getResourcesForApplication(resolveInfo.activityInfo.applicationInfo).getBoolean(i) : true;
            if (!z) {
                Log.i("ProviderEligibility", str + " requires unsupported resource " + i);
            }
            return z;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w("ProviderEligibility", "Cannot find resources for " + str, e);
            return false;
        } catch (Resources.NotFoundException e2) {
            Log.w("ProviderEligibility", "Cannot find resources for " + str, e2);
            return false;
        }
    }
}
