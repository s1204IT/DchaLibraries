package com.android.settings.display;

import android.content.Context;
import android.provider.Settings;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.FeatureFlagUtils;
import com.android.settings.core.BasePreferenceController;

public class SystemUiThemePreferenceController extends BasePreferenceController implements Preference.OnPreferenceChangeListener {
    private ListPreference mSystemUiThemePref;

    public SystemUiThemePreferenceController(Context context, String str) {
        super(context, str);
    }

    @Override
    public int getAvailabilityStatus() {
        return !FeatureFlagUtils.isEnabled(this.mContext, "settings_systemui_theme") ? 1 : 0;
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mSystemUiThemePref = (ListPreference) preferenceScreen.findPreference(getPreferenceKey());
        this.mSystemUiThemePref.setValue(Integer.toString(Settings.Secure.getInt(this.mContext.getContentResolver(), "theme_mode", 0)));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        Settings.Secure.putInt(this.mContext.getContentResolver(), "theme_mode", Integer.parseInt((String) obj));
        refreshSummary(preference);
        return true;
    }

    @Override
    public CharSequence getSummary() {
        return this.mSystemUiThemePref.getEntries()[this.mSystemUiThemePref.findIndexOfValue(Integer.toString(Settings.Secure.getInt(this.mContext.getContentResolver(), "theme_mode", 0)))];
    }
}
