package com.android.settings.wifi.details;

import android.app.backup.BackupManager;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.support.v7.preference.DropDownPreference;
import android.support.v7.preference.Preference;
import com.android.settings.core.BasePreferenceController;

/* loaded from: classes.dex */
public class WifiMeteredPreferenceController extends BasePreferenceController implements Preference.OnPreferenceChangeListener {
    private static final String KEY_WIFI_METERED = "metered";
    private WifiConfiguration mWifiConfiguration;
    private WifiManager mWifiManager;

    public WifiMeteredPreferenceController(Context context, WifiConfiguration wifiConfiguration) {
        super(context, KEY_WIFI_METERED);
        this.mWifiConfiguration = wifiConfiguration;
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        DropDownPreference dropDownPreference = (DropDownPreference) preference;
        int meteredOverride = getMeteredOverride();
        dropDownPreference.setValue(Integer.toString(meteredOverride));
        updateSummary(dropDownPreference, meteredOverride);
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        return 0;
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        if (this.mWifiConfiguration != null) {
            this.mWifiConfiguration.meteredOverride = Integer.parseInt((String) obj);
        }
        this.mWifiManager.updateNetwork(this.mWifiConfiguration);
        BackupManager.dataChanged("com.android.providers.settings");
        updateSummary((DropDownPreference) preference, getMeteredOverride());
        return true;
    }

    int getMeteredOverride() {
        if (this.mWifiConfiguration != null) {
            return this.mWifiConfiguration.meteredOverride;
        }
        return 0;
    }

    private void updateSummary(DropDownPreference dropDownPreference, int i) {
        dropDownPreference.setSummary(dropDownPreference.getEntries()[i]);
    }
}
