package com.android.browser.preferences;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import com.android.browser.BrowserSettings;
import com.android.browser.R;

public class DebugPreferencesFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.debug_preferences);
        Preference e = findPreference("reset_prelogin");
        e.setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if ("reset_prelogin".equals(preference.getKey())) {
            BrowserSettings.getInstance().getPreferences().edit().remove("last_autologin_time").apply();
            return true;
        }
        return false;
    }
}
