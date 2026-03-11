package com.android.browser.preferences;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import com.android.browser.BrowserSettings;

public class DebugPreferencesFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener {
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(2131099653);
        findPreference("reset_prelogin").setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (!"reset_prelogin".equals(preference.getKey())) {
            return false;
        }
        BrowserSettings.getInstance().getPreferences().edit().remove("last_autologin_time").apply();
        return true;
    }
}
