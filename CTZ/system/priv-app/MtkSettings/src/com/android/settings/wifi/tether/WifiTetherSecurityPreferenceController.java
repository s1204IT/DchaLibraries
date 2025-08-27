package com.android.settings.wifi.tether;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import com.android.settings.R;
import com.android.settings.wifi.tether.WifiTetherBasePreferenceController;

/* loaded from: classes.dex */
public class WifiTetherSecurityPreferenceController extends WifiTetherBasePreferenceController {
    private final String[] mSecurityEntries;
    private int mSecurityValue;

    public WifiTetherSecurityPreferenceController(Context context, WifiTetherBasePreferenceController.OnTetherConfigUpdateListener onTetherConfigUpdateListener) {
        super(context, onTetherConfigUpdateListener);
        this.mSecurityEntries = this.mContext.getResources().getStringArray(R.array.wifi_tether_security);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "wifi_tether_security";
    }

    @Override // com.android.settings.wifi.tether.WifiTetherBasePreferenceController
    public void updateDisplay() {
        WifiConfiguration wifiApConfiguration = this.mWifiManager.getWifiApConfiguration();
        if (wifiApConfiguration != null && wifiApConfiguration.getAuthType() == 0) {
            this.mSecurityValue = 0;
        } else {
            this.mSecurityValue = 4;
        }
        ListPreference listPreference = (ListPreference) this.mPreference;
        listPreference.setSummary(getSummaryForSecurityType(this.mSecurityValue));
        listPreference.setValue(String.valueOf(this.mSecurityValue));
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        this.mSecurityValue = Integer.parseInt((String) obj);
        preference.setSummary(getSummaryForSecurityType(this.mSecurityValue));
        this.mListener.onTetherConfigUpdated();
        return true;
    }

    public int getSecurityType() {
        return this.mSecurityValue;
    }

    private String getSummaryForSecurityType(int i) {
        if (i == 0) {
            return this.mSecurityEntries[1];
        }
        return this.mSecurityEntries[0];
    }

    public void setSecurityType() {
        ListPreference listPreference = (ListPreference) this.mPreference;
        this.mSecurityValue = 4;
        listPreference.setSummary(this.mSecurityEntries[0]);
    }
}
