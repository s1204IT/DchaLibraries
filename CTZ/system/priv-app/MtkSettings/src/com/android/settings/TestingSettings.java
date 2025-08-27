package com.android.settings;

import android.os.Bundle;
import android.os.UserManager;
import android.support.v7.preference.PreferenceScreen;

/* loaded from: classes.dex */
public class TestingSettings extends SettingsPreferenceFragment {
    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.testing_settings);
        if (!UserManager.get(getContext()).isAdminUser()) {
            getPreferenceScreen().removePreference((PreferenceScreen) findPreference("radio_info_settings"));
        }
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 89;
    }
}
