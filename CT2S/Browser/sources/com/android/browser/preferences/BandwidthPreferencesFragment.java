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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.bandwidth_preferences);
    }

    @Override
    public void onResume() {
        ListPreference prefetch;
        ListPreference preload;
        super.onResume();
        PreferenceScreen prefScreen = getPreferenceScreen();
        SharedPreferences sharedPrefs = prefScreen.getSharedPreferences();
        if (!sharedPrefs.contains("preload_when") && (preload = (ListPreference) prefScreen.findPreference("preload_when")) != null) {
            preload.setValue(BrowserSettings.getInstance().getDefaultPreloadSetting());
        }
        if (!sharedPrefs.contains("link_prefetch_when") && (prefetch = (ListPreference) prefScreen.findPreference("link_prefetch_when")) != null) {
            prefetch.setValue(BrowserSettings.getInstance().getDefaultLinkPrefetchSetting());
        }
    }
}
