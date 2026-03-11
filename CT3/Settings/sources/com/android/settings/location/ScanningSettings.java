package com.android.settings.location;

import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class ScanningSettings extends SettingsPreferenceFragment {
    @Override
    protected int getMetricsCategory() {
        return 131;
    }

    @Override
    public void onResume() {
        super.onResume();
        createPreferenceHierarchy();
    }

    private PreferenceScreen createPreferenceHierarchy() {
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        addPreferencesFromResource(R.xml.location_scanning);
        PreferenceScreen root2 = getPreferenceScreen();
        initPreferences();
        return root2;
    }

    private void initPreferences() {
        SwitchPreference wifiScanAlwaysAvailable = (SwitchPreference) findPreference("wifi_always_scanning");
        wifiScanAlwaysAvailable.setChecked(Settings.Global.getInt(getContentResolver(), "wifi_scan_always_enabled", 0) == 1);
        SwitchPreference bleScanAlwaysAvailable = (SwitchPreference) findPreference("bluetooth_always_scanning");
        bleScanAlwaysAvailable.setChecked(Settings.Global.getInt(getContentResolver(), "ble_scan_always_enabled", 0) == 1);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        String key = preference.getKey();
        if ("wifi_always_scanning".equals(key)) {
            Settings.Global.putInt(getContentResolver(), "wifi_scan_always_enabled", ((SwitchPreference) preference).isChecked() ? 1 : 0);
        } else if ("bluetooth_always_scanning".equals(key)) {
            Settings.Global.putInt(getContentResolver(), "ble_scan_always_enabled", ((SwitchPreference) preference).isChecked() ? 1 : 0);
        } else {
            return super.onPreferenceTreeClick(preference);
        }
        return true;
    }
}
