package com.android.deskclock.settings;

import android.annotation.TargetApi;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import com.android.deskclock.R;
import com.android.deskclock.Utils;

public final class ScreensaverSettingsActivity extends AppCompatActivity {
    public static final String KEY_CLOCK_STYLE = "screensaver_clock_style";
    public static final String KEY_NIGHT_MODE = "screensaver_night_mode";

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.screensaver_settings);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 16908332) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    public static class PrefsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
        @Override
        @TargetApi(24)
        public void onCreate(Bundle bundle) {
            super.onCreate(bundle);
            if (Utils.isNOrLater()) {
                getPreferenceManager().setStorageDeviceProtected();
            }
            addPreferencesFromResource(R.xml.screensaver_settings);
        }

        @Override
        public void onResume() {
            super.onResume();
            refresh();
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object obj) {
            if (ScreensaverSettingsActivity.KEY_CLOCK_STYLE.equals(preference.getKey())) {
                ListPreference listPreference = (ListPreference) preference;
                listPreference.setSummary(listPreference.getEntries()[listPreference.findIndexOfValue((String) obj)]);
                return true;
            }
            return true;
        }

        private void refresh() {
            ListPreference listPreference = (ListPreference) findPreference(ScreensaverSettingsActivity.KEY_CLOCK_STYLE);
            listPreference.setSummary(listPreference.getEntry());
            listPreference.setOnPreferenceChangeListener(this);
        }
    }
}
