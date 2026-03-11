package com.android.settings.applications;

import android.os.Bundle;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class SpecialAccessSettings extends SettingsPreferenceFragment {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.special_access);
    }

    @Override
    protected int getMetricsCategory() {
        return 351;
    }
}
