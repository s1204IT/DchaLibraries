package com.android.settings;

import android.os.Bundle;
import android.os.UserManager;
import android.support.v7.preference.PreferenceScreen;

public class TestingSettings extends SettingsPreferenceFragment {
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.testing_settings);
        if (!UserManager.get(getContext()).isAdminUser()) {
            getPreferenceScreen().removePreference((PreferenceScreen) findPreference("radio_info_settings"));
        }
    }

    @Override
    public int getMetricsCategory() {
        return 89;
    }
}
