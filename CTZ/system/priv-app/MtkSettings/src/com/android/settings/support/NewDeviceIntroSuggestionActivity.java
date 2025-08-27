package com.android.settings.support;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.overlay.SupportFeatureProvider;
import java.util.List;

/* loaded from: classes.dex */
public class NewDeviceIntroSuggestionActivity extends Activity {
    static final long PERMANENT_DISMISS_THRESHOLD = 1209600000;
    static final String PREF_KEY_SUGGGESTION_COMPLETE = "pref_new_device_intro_suggestion_complete";
    static final String PREF_KEY_SUGGGESTION_FIRST_DISPLAY_TIME = "pref_new_device_intro_suggestion_first_display_time_ms";

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Intent launchIntent = getLaunchIntent(this);
        if (launchIntent != null) {
            FeatureFactory.getFactory(this).getSuggestionFeatureProvider(this).getSharedPrefs(this).edit().putBoolean(PREF_KEY_SUGGGESTION_COMPLETE, true).commit();
            startActivity(launchIntent);
        }
        finish();
    }

    public static boolean isSuggestionComplete(Context context) {
        return isTipsInstalledAsSystemApp(context) || !isSupported(context) || isExpired(context) || hasLaunchedBefore(context) || !canOpenUrlInBrowser(context);
    }

    private static boolean isSupported(Context context) {
        return context.getResources().getBoolean(R.bool.config_new_device_intro_suggestion_supported);
    }

    private static boolean isExpired(Context context) {
        long j;
        SharedPreferences sharedPrefs = FeatureFactory.getFactory(context).getSuggestionFeatureProvider(context).getSharedPrefs(context);
        long jCurrentTimeMillis = System.currentTimeMillis();
        if (!sharedPrefs.contains(PREF_KEY_SUGGGESTION_FIRST_DISPLAY_TIME)) {
            sharedPrefs.edit().putLong(PREF_KEY_SUGGGESTION_FIRST_DISPLAY_TIME, jCurrentTimeMillis).commit();
            j = jCurrentTimeMillis;
        } else {
            j = sharedPrefs.getLong(PREF_KEY_SUGGGESTION_FIRST_DISPLAY_TIME, -1L);
        }
        boolean z = jCurrentTimeMillis > j + PERMANENT_DISMISS_THRESHOLD;
        Log.d("NewDeviceIntroSugg", "is suggestion expired: " + z);
        return z;
    }

    private static boolean canOpenUrlInBrowser(Context context) {
        List<ResolveInfo> listQueryIntentActivities;
        Intent launchIntent = getLaunchIntent(context);
        return (launchIntent == null || (listQueryIntentActivities = context.getPackageManager().queryIntentActivities(launchIntent, 0)) == null || listQueryIntentActivities.size() == 0) ? false : true;
    }

    private static boolean hasLaunchedBefore(Context context) {
        return FeatureFactory.getFactory(context).getSuggestionFeatureProvider(context).getSharedPrefs(context).getBoolean(PREF_KEY_SUGGGESTION_COMPLETE, false);
    }

    static Intent getLaunchIntent(Context context) {
        SupportFeatureProvider supportFeatureProvider = FeatureFactory.getFactory(context).getSupportFeatureProvider(context);
        if (supportFeatureProvider == null) {
            return null;
        }
        String newDeviceIntroUrl = supportFeatureProvider.getNewDeviceIntroUrl(context);
        if (TextUtils.isEmpty(newDeviceIntroUrl)) {
            return null;
        }
        return new Intent().setAction("android.intent.action.VIEW").addCategory("android.intent.category.BROWSABLE").setData(Uri.parse(newDeviceIntroUrl));
    }

    private static boolean isTipsInstalledAsSystemApp(Context context) {
        try {
            return context.getPackageManager().getPackageInfo("com.google.android.apps.tips", 1048576) != null;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w("NewDeviceIntroSugg", "Cannot find the package: com.google.android.apps.tips", e);
            return false;
        }
    }
}
