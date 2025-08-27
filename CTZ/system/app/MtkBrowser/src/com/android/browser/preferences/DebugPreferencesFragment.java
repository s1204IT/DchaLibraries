package com.android.browser.preferences;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import com.android.browser.BrowserSettings;
import com.android.browser.R;

/* loaded from: classes.dex */
public class DebugPreferencesFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener {
    @Override // android.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.debug_preferences);
        findPreference("reset_prelogin").setOnPreferenceClickListener(this);
    }

    @Override // android.preference.Preference.OnPreferenceClickListener
    public boolean onPreferenceClick(Preference preference) {
        if ("reset_prelogin".equals(preference.getKey())) {
            BrowserSettings.getInstance().getPreferences().edit().remove("last_autologin_time").apply();
            return true;
        }
        return false;
    }
}
