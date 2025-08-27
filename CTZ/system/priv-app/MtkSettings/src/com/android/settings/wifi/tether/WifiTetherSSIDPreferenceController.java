package com.android.settings.wifi.tether;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.Preference;
import android.util.Log;
import com.android.settings.widget.ValidatedEditTextPreference;
import com.android.settings.wifi.tether.WifiTetherBasePreferenceController;

/* loaded from: classes.dex */
public class WifiTetherSSIDPreferenceController extends WifiTetherBasePreferenceController implements ValidatedEditTextPreference.Validator {
    static final String DEFAULT_SSID = "AndroidAP";
    private String mSSID;
    private WifiDeviceNameTextValidator mWifiDeviceNameTextValidator;

    public WifiTetherSSIDPreferenceController(Context context, WifiTetherBasePreferenceController.OnTetherConfigUpdateListener onTetherConfigUpdateListener) {
        super(context, onTetherConfigUpdateListener);
        this.mWifiDeviceNameTextValidator = new WifiDeviceNameTextValidator();
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "wifi_tether_network_name";
    }

    @Override // com.android.settings.wifi.tether.WifiTetherBasePreferenceController
    public void updateDisplay() {
        WifiConfiguration wifiApConfiguration = this.mWifiManager.getWifiApConfiguration();
        if (wifiApConfiguration != null) {
            this.mSSID = wifiApConfiguration.SSID;
            Log.d("WifiTetherSsidPref", "Updating SSID in Preference, " + this.mSSID);
        } else {
            this.mSSID = DEFAULT_SSID;
            Log.d("WifiTetherSsidPref", "Updating to default SSID in Preference, " + this.mSSID);
        }
        ((ValidatedEditTextPreference) this.mPreference).setValidator(this);
        updateSsidDisplay((EditTextPreference) this.mPreference);
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        this.mSSID = (String) obj;
        updateSsidDisplay((EditTextPreference) preference);
        this.mListener.onTetherConfigUpdated();
        return true;
    }

    @Override // com.android.settings.widget.ValidatedEditTextPreference.Validator
    public boolean isTextValid(String str) {
        return this.mWifiDeviceNameTextValidator.isTextValid(str);
    }

    public String getSSID() {
        return this.mSSID;
    }

    private void updateSsidDisplay(EditTextPreference editTextPreference) {
        editTextPreference.setText(this.mSSID);
        editTextPreference.setSummary(this.mSSID);
    }

    public void setSSID(String str) {
        this.mSSID = str;
        updateSsidDisplay((EditTextPreference) this.mPreference);
    }
}
