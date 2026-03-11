package com.android.launcher3;

import android.app.Activity;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;

public class SettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new LauncherSettingsFragment()).commit();
    }

    public static class LauncherSettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.launcher_preferences);
            SwitchPreference pref = (SwitchPreference) findPreference("pref_allowRotation");
            pref.setPersistent(false);
            Bundle extras = new Bundle();
            extras.putBoolean("default_value", false);
            Bundle value = getActivity().getContentResolver().call(LauncherSettings$Settings.CONTENT_URI, "get_boolean_setting", "pref_allowRotation", extras);
            pref.setChecked(value.getBoolean("value"));
            pref.setOnPreferenceChangeListener(this);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            Bundle extras = new Bundle();
            extras.putBoolean("value", ((Boolean) newValue).booleanValue());
            getActivity().getContentResolver().call(LauncherSettings$Settings.CONTENT_URI, "set_boolean_setting", preference.getKey(), extras);
            return true;
        }
    }
}
