package com.android.server.telecom;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

public class RespondViaSmsSettings {

    public static class Settings extends PreferenceActivity implements Preference.OnPreferenceChangeListener {
        @Override
        protected void onCreate(Bundle bundle) {
            super.onCreate(bundle);
            Log.d(this, "Settings: onCreate()...", new Object[0]);
            QuickResponseUtils.maybeMigrateLegacyQuickResponses(this);
            getPreferenceManager().setSharedPreferencesName("respond_via_sms_prefs");
            addPreferencesFromResource(R.xml.respond_via_sms_settings);
            EditTextPreference editTextPreference = (EditTextPreference) findPreference("canned_response_pref_1");
            editTextPreference.setTitle(editTextPreference.getText());
            editTextPreference.setOnPreferenceChangeListener(this);
            EditTextPreference editTextPreference2 = (EditTextPreference) findPreference("canned_response_pref_2");
            editTextPreference2.setTitle(editTextPreference2.getText());
            editTextPreference2.setOnPreferenceChangeListener(this);
            EditTextPreference editTextPreference3 = (EditTextPreference) findPreference("canned_response_pref_3");
            editTextPreference3.setTitle(editTextPreference3.getText());
            editTextPreference3.setOnPreferenceChangeListener(this);
            EditTextPreference editTextPreference4 = (EditTextPreference) findPreference("canned_response_pref_4");
            editTextPreference4.setTitle(editTextPreference4.getText());
            editTextPreference4.setOnPreferenceChangeListener(this);
            ActionBar actionBar = getActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object obj) {
            Log.d(this, "onPreferenceChange: key = %s", preference.getKey());
            Log.d(this, "  preference = '%s'", preference);
            Log.d(this, "  newValue = '%s'", obj);
            ((EditTextPreference) preference).setTitle((String) obj);
            return true;
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case android.R.id.home:
                    RespondViaSmsSettings.goUpToTopLevelSetting(this);
                    return true;
                default:
                    return super.onOptionsItemSelected(menuItem);
            }
        }
    }

    public static void goUpToTopLevelSetting(Activity activity) {
        activity.finish();
    }
}
