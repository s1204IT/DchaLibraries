package com.android.browser.preferences;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import com.android.browser.BrowserSettings;
import com.android.browser.R;

public class BandwidthPreferencesFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.bandwidth_preferences);
        getPreferenceScreen().removePreference(findPreference("preload_when"));
    }

    @Override
    public void onResume() {
        ListPreference listPreference;
        ListPreference listPreference2;
        super.onResume();
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        SharedPreferences sharedPreferences = preferenceScreen.getSharedPreferences();
        if (!sharedPreferences.contains("preload_when") && (listPreference2 = (ListPreference) preferenceScreen.findPreference("preload_when")) != null) {
            listPreference2.setValue(BrowserSettings.getInstance().getDefaultPreloadSetting());
        }
        if (!sharedPreferences.contains("link_prefetch_when") && (listPreference = (ListPreference) preferenceScreen.findPreference("link_prefetch_when")) != null) {
            listPreference.setValue(BrowserSettings.getInstance().getDefaultLinkPrefetchSetting());
        }
    }
}
