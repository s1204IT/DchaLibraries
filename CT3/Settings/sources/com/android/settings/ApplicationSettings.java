package com.android.settings;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;

public class ApplicationSettings extends SettingsPreferenceFragment {
    private ListPreference mInstallLocation;
    private CheckBoxPreference mToggleAdvancedSettings;

    @Override
    protected int getMetricsCategory() {
        return 16;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.application_settings);
        this.mToggleAdvancedSettings = (CheckBoxPreference) findPreference("toggle_advanced_settings");
        this.mToggleAdvancedSettings.setChecked(isAdvancedSettingsEnabled());
        getPreferenceScreen().removePreference(this.mToggleAdvancedSettings);
        this.mInstallLocation = (ListPreference) findPreference("app_install_location");
        boolean userSetInstLocation = Settings.Global.getInt(getContentResolver(), "set_install_location", 0) != 0;
        if (!userSetInstLocation) {
            getPreferenceScreen().removePreference(this.mInstallLocation);
        } else {
            this.mInstallLocation.setValue(getAppInstallLocation());
            this.mInstallLocation.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String value = (String) newValue;
                    ApplicationSettings.this.handleUpdateAppInstallLocation(value);
                    return false;
                }
            });
        }
    }

    protected void handleUpdateAppInstallLocation(String value) {
        if ("device".equals(value)) {
            Settings.Global.putInt(getContentResolver(), "default_install_location", 1);
        } else if ("sdcard".equals(value)) {
            Settings.Global.putInt(getContentResolver(), "default_install_location", 2);
        } else if ("auto".equals(value)) {
            Settings.Global.putInt(getContentResolver(), "default_install_location", 0);
        } else {
            Settings.Global.putInt(getContentResolver(), "default_install_location", 0);
        }
        this.mInstallLocation.setValue(value);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == this.mToggleAdvancedSettings) {
            boolean value = this.mToggleAdvancedSettings.isChecked();
            setAdvancedSettingsEnabled(value);
        }
        return super.onPreferenceTreeClick(preference);
    }

    private boolean isAdvancedSettingsEnabled() {
        return Settings.System.getInt(getContentResolver(), "advanced_settings", 0) > 0;
    }

    private void setAdvancedSettingsEnabled(boolean enabled) {
        int value = enabled ? 1 : 0;
        Settings.Secure.putInt(getContentResolver(), "advanced_settings", value);
        Intent intent = new Intent("android.intent.action.ADVANCED_SETTINGS");
        intent.putExtra("state", value);
        getActivity().sendBroadcast(intent);
    }

    private String getAppInstallLocation() {
        int selectedLocation = Settings.Global.getInt(getContentResolver(), "default_install_location", 0);
        if (selectedLocation == 1) {
            return "device";
        }
        if (selectedLocation == 2) {
            return "sdcard";
        }
        if (selectedLocation == 0) {
            return "auto";
        }
        return "auto";
    }
}
