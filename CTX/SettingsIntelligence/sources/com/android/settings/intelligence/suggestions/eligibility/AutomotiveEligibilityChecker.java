package com.android.settings.intelligence.suggestions.eligibility;

import android.content.Context;
import android.content.pm.ResolveInfo;
import android.util.Log;

public class AutomotiveEligibilityChecker {
    public static boolean isEligible(Context context, String str, ResolveInfo resolveInfo) {
        boolean zHasSystemFeature = context.getPackageManager().hasSystemFeature("android.hardware.type.automotive");
        boolean z = resolveInfo.activityInfo.metaData.getBoolean("com.android.settings.automotive_eligible", false);
        if (zHasSystemFeature) {
            if (!z) {
                Log.i("AutomotiveEligibility", "Suggestion is ineligible for FEATURE_AUTOMOTIVE: " + str);
            }
            return z;
        }
        return true;
    }
}
