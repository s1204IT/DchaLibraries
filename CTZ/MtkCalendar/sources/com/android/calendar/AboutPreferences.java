package com.android.calendar;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceFragment;

public class AboutPreferences extends PreferenceFragment {
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.about_preferences);
        Activity activity = getActivity();
        try {
            findPreference("build_version").setSummary(activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
            findPreference("build_version").setSummary("?");
        }
    }
}
