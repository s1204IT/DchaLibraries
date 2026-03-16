package com.android.deskclock;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

public class ScreensaverSettingsActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.dream_settings);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        if ("screensaver_clock_style".equals(pref.getKey())) {
            ListPreference listPref = (ListPreference) pref;
            int idx = listPref.findIndexOfValue((String) newValue);
            listPref.setSummary(listPref.getEntries()[idx]);
            return true;
        }
        if ("screensaver_night_mode".equals(pref.getKey())) {
            ((CheckBoxPreference) pref).isChecked();
            return true;
        }
        return true;
    }

    private void refresh() {
        ListPreference listPref = (ListPreference) findPreference("screensaver_clock_style");
        listPref.setSummary(listPref.getEntry());
        listPref.setOnPreferenceChangeListener(this);
        Preference pref = findPreference("screensaver_night_mode");
        ((CheckBoxPreference) pref).isChecked();
        pref.setOnPreferenceChangeListener(this);
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return true;
    }
}
