package com.android.browser.preferences;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import com.android.browser.R;

/* loaded from: classes.dex */
public class LabPreferencesFragment extends PreferenceFragment {
    @Override // android.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.lab_preferences);
    }
}
