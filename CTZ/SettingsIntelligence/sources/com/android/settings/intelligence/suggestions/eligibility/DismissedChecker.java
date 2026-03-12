package com.android.settings.intelligence.suggestions.eligibility;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.util.Log;
import com.android.settings.intelligence.overlay.FeatureFactory;
import com.android.settings.intelligence.suggestions.SuggestionFeatureProvider;

public class DismissedChecker {
    static final String META_DATA_DISMISS_CONTROL = "com.android.settings.dismiss";
    static final String SETUP_TIME = "_setup_time";

    public static boolean isEligible(Context context, String str, ResolveInfo resolveInfo, boolean z) {
        int appearDay;
        SuggestionFeatureProvider suggestionFeatureProvider = FeatureFactory.get(context).suggestionFeatureProvider();
        SharedPreferences sharedPrefs = suggestionFeatureProvider.getSharedPrefs(context);
        long jCurrentTimeMillis = System.currentTimeMillis();
        String str2 = str + SETUP_TIME;
        if (!sharedPrefs.contains(str2)) {
            sharedPrefs.edit().putLong(str2, jCurrentTimeMillis).apply();
        }
        if (suggestionFeatureProvider.isSuggestionDismissed(context, str)) {
            return false;
        }
        if (!z) {
            appearDay = parseAppearDay(resolveInfo);
        } else {
            appearDay = 0;
        }
        long j = sharedPrefs.getLong(str2, 0L);
        if (j > jCurrentTimeMillis) {
            j = jCurrentTimeMillis;
        }
        if (jCurrentTimeMillis < getFirstAppearTimeMillis(j, appearDay)) {
            return false;
        }
        suggestionFeatureProvider.markSuggestionNotDismissed(context, str);
        return true;
    }

    private static int parseAppearDay(ResolveInfo resolveInfo) {
        if (!resolveInfo.activityInfo.metaData.containsKey(META_DATA_DISMISS_CONTROL)) {
            return 0;
        }
        Object obj = resolveInfo.activityInfo.metaData.get(META_DATA_DISMISS_CONTROL);
        if (obj instanceof Integer) {
            return ((Integer) obj).intValue();
        }
        try {
            return Integer.parseInt(((String) obj).split(",")[0]);
        } catch (Exception e) {
            Log.w("DismissedChecker", "Failed to parse appear/dismiss rule, fall back to 0");
            return 0;
        }
    }

    private static long getFirstAppearTimeMillis(long j, int i) {
        return j + (((long) i) * 86400000);
    }
}
