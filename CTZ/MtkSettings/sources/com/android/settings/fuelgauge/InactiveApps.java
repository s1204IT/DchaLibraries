package com.android.settings.fuelgauge;

import android.app.Activity;
import android.app.usage.UsageStatsManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class InactiveApps extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {
    private static final CharSequence[] SETTABLE_BUCKETS_NAMES = {"ACTIVE", "WORKING_SET", "FREQUENT", "RARE"};
    private static final CharSequence[] SETTABLE_BUCKETS_VALUES = {Integer.toString(10), Integer.toString(20), Integer.toString(30), Integer.toString(40)};
    private UsageStatsManager mUsageStats;

    @Override
    public int getMetricsCategory() {
        return 238;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mUsageStats = (UsageStatsManager) getActivity().getSystemService(UsageStatsManager.class);
        addPreferencesFromResource(R.xml.inactive_apps);
    }

    @Override
    public void onResume() {
        super.onResume();
        init();
    }

    private void init() {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceScreen.removeAll();
        preferenceScreen.setOrderingAsAdded(false);
        Activity activity = getActivity();
        PackageManager packageManager = activity.getPackageManager();
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.LAUNCHER");
        for (ResolveInfo resolveInfo : packageManager.queryIntentActivities(intent, 0)) {
            String str = resolveInfo.activityInfo.applicationInfo.packageName;
            ListPreference listPreference = new ListPreference(getPrefContext());
            listPreference.setTitle(resolveInfo.loadLabel(packageManager));
            listPreference.setIcon(resolveInfo.loadIcon(packageManager));
            listPreference.setKey(str);
            listPreference.setEntries(SETTABLE_BUCKETS_NAMES);
            listPreference.setEntryValues(SETTABLE_BUCKETS_VALUES);
            updateSummary(listPreference);
            listPreference.setOnPreferenceChangeListener(this);
            preferenceScreen.addPreference(listPreference);
        }
    }

    static String bucketToName(int i) {
        if (i == 5) {
            return "EXEMPTED";
        }
        if (i == 10) {
            return "ACTIVE";
        }
        if (i == 20) {
            return "WORKING_SET";
        }
        if (i == 30) {
            return "FREQUENT";
        }
        if (i == 40) {
            return "RARE";
        }
        if (i == 50) {
            return "NEVER";
        }
        return "";
    }

    private void updateSummary(ListPreference listPreference) {
        Resources resources = getActivity().getResources();
        int appStandbyBucket = this.mUsageStats.getAppStandbyBucket(listPreference.getKey());
        boolean z = true;
        listPreference.setSummary(resources.getString(R.string.standby_bucket_summary, bucketToName(appStandbyBucket)));
        if (appStandbyBucket < 10 || appStandbyBucket > 40) {
            z = false;
        }
        if (z) {
            listPreference.setValue(Integer.toString(appStandbyBucket));
        }
        listPreference.setEnabled(z);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        this.mUsageStats.setAppStandbyBucket(preference.getKey(), Integer.parseInt((String) obj));
        updateSummary((ListPreference) preference);
        return false;
    }
}
