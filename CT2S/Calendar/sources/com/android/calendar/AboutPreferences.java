package com.android.calendar;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceFragment;

public class AboutPreferences extends PreferenceFragment {
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.about_preferences);
        Activity activity = getActivity();
        try {
            PackageInfo packageInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
            findPreference("build_version").setSummary(packageInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            findPreference("build_version").setSummary("?");
        }
    }
}
