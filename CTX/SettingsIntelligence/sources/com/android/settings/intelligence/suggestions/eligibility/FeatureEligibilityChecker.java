package com.android.settings.intelligence.suggestions.eligibility;

import android.content.Context;
import android.content.pm.ResolveInfo;
import android.text.TextUtils;
import android.util.Log;

public class FeatureEligibilityChecker {
    static final String META_DATA_REQUIRE_FEATURE = "com.android.settings.require_feature";

    public static boolean isEligible(Context context, String str, ResolveInfo resolveInfo) {
        String string = resolveInfo.activityInfo.metaData.getString(META_DATA_REQUIRE_FEATURE);
        if (string != null) {
            for (String str2 : string.split(",")) {
                if (TextUtils.isEmpty(str2)) {
                    Log.i("FeatureEligibility", "Found empty substring when parsing required features: " + string);
                } else if (!context.getPackageManager().hasSystemFeature(str2)) {
                    Log.i("FeatureEligibility", str + " requires unavailable feature " + str2);
                    return false;
                }
            }
            return true;
        }
        return true;
    }
}
