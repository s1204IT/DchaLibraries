package com.android.browser.preferences;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import com.android.browser.R;

public class PrivacySecurityPreferencesFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.privacy_security_preferences);
        Preference e = findPreference("privacy_clear_history");
        e.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object objValue) {
        if (!pref.getKey().equals("privacy_clear_history") || !((Boolean) objValue).booleanValue()) {
            return false;
        }
        getActivity().setResult(-1, new Intent().putExtra("android.intent.extra.TEXT", pref.getKey()));
        return true;
    }
}
