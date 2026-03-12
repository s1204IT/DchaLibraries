package com.android.settings.intelligence.suggestions.eligibility;

import android.content.Context;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class ConnectivityEligibilityChecker {
    static final String META_DATA_IS_CONNECTION_REQUIRED = "com.android.settings.require_connection";

    public static boolean isEligible(Context context, String str, ResolveInfo resolveInfo) {
        if (!resolveInfo.activityInfo.metaData.getBoolean(META_DATA_IS_CONNECTION_REQUIRED)) {
            return true;
        }
        NetworkInfo activeNetworkInfo = ((ConnectivityManager) context.getSystemService("connectivity")).getActiveNetworkInfo();
        boolean z = activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
        if (!z) {
            Log.i("ConnectivityEligibility", str + " is missing required connection.");
        }
        return z;
    }
}
