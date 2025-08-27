package com.android.settings.wifi;

import android.os.Bundle;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

/* loaded from: classes.dex */
public class WifiInfo extends SettingsPreferenceFragment {
    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.testing_wifi_settings);
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 89;
    }
}
