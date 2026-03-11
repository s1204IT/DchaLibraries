package com.android.settings;

import android.os.Bundle;
import android.os.UserManager;
import android.support.v7.preference.PreferenceScreen;

public class TestingSettings extends SettingsPreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.testing_settings);
        UserManager um = UserManager.get(getContext());
        if (um.isAdminUser()) {
            return;
        }
        PreferenceScreen preferenceScreen = (PreferenceScreen) findPreference("radio_info_settings");
        getPreferenceScreen().removePreference(preferenceScreen);
    }

    @Override
    protected int getMetricsCategory() {
        return 89;
    }
}
