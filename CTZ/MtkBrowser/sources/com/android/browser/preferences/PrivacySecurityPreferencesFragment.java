package com.android.browser.preferences;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import com.android.browser.R;

public class PrivacySecurityPreferencesFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.privacy_security_preferences);
        findPreference("privacy_clear_history").setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        if (preference.getKey().equals("privacy_clear_history") && ((Boolean) obj).booleanValue()) {
            getActivity().setResult(-1, new Intent().putExtra("android.intent.extra.TEXT", preference.getKey()));
            return true;
        }
        return false;
    }
}
