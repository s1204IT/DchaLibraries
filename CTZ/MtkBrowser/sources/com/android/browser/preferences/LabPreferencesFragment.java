package com.android.browser.preferences;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import com.android.browser.R;

public class LabPreferencesFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.lab_preferences);
    }
}
