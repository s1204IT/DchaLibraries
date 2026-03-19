package com.android.server.telecom;

import android.app.ActionBar;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.telecom.Log;
import android.view.MenuItem;

public class RespondViaSmsSettings extends PreferenceActivity implements Preference.OnPreferenceChangeListener {
    private SharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Log.d(this, "Settings: onCreate()...", new Object[0]);
        QuickResponseUtils.maybeMigrateLegacyQuickResponses(this);
        getPreferenceManager().setSharedPreferencesName("respond_via_sms_prefs");
        this.mPrefs = getPreferenceManager().getSharedPreferences();
    }

    @Override
    public void onResume() {
        super.onResume();
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (preferenceScreen != null) {
            preferenceScreen.removeAll();
        }
        addPreferencesFromResource(R.xml.respond_via_sms_settings);
        initPref(findPreference("canned_response_pref_1"));
        initPref(findPreference("canned_response_pref_2"));
        initPref(findPreference("canned_response_pref_3"));
        initPref(findPreference("canned_response_pref_4"));
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        Log.d(this, "onPreferenceChange: key = %s", new Object[]{preference.getKey()});
        Log.d(this, "  preference = '%s'", new Object[]{preference});
        Log.d(this, "  newValue = '%s'", new Object[]{obj});
        EditTextPreference editTextPreference = (EditTextPreference) preference;
        String str = (String) obj;
        editTextPreference.setTitle(str);
        this.mPrefs.edit().putString(editTextPreference.getKey(), str).commit();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 16908332) {
            goUpToTopLevelSetting(this);
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    public static void goUpToTopLevelSetting(Activity activity) {
        activity.finish();
    }

    private void initPref(Preference preference) {
        EditTextPreference editTextPreference = (EditTextPreference) preference;
        editTextPreference.setText(this.mPrefs.getString(editTextPreference.getKey(), editTextPreference.getText()));
        editTextPreference.setTitle(editTextPreference.getText());
        editTextPreference.setOnPreferenceChangeListener(this);
    }
}
