package com.android.settings.intelligence.suggestions.eligibility;

import android.accounts.AccountManager;
import android.content.Context;
import android.content.pm.ResolveInfo;
import android.util.Log;

public class AccountEligibilityChecker {
    static final String META_DATA_REQUIRE_ACCOUNT = "com.android.settings.require_account";

    public static boolean isEligible(Context context, String str, ResolveInfo resolveInfo) {
        String string = resolveInfo.activityInfo.metaData.getString(META_DATA_REQUIRE_ACCOUNT);
        if (string == null) {
            return true;
        }
        boolean z = AccountManager.get(context).getAccountsByType(string).length > 0;
        if (!z) {
            Log.i("AccountEligibility", str + " requires unavailable account type " + string);
        }
        return z;
    }
}
