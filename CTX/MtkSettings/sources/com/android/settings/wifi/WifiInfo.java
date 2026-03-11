package com.android.settings.wifi;

import android.os.Bundle;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class WifiInfo extends SettingsPreferenceFragment {
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.testing_wifi_settings);
    }

    @Override
    public int getMetricsCategory() {
        return 89;
    }
}
