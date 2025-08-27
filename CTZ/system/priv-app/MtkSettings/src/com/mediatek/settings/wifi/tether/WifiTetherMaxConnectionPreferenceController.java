package com.mediatek.settings.wifi.tether;

import android.content.Context;
import android.provider.Settings;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import com.android.settings.wifi.tether.WifiTetherBasePreferenceController;

/* loaded from: classes.dex */
public class WifiTetherMaxConnectionPreferenceController extends WifiTetherBasePreferenceController {
    private int mMaxConnectionsValue;

    public WifiTetherMaxConnectionPreferenceController(Context context, WifiTetherBasePreferenceController.OnTetherConfigUpdateListener onTetherConfigUpdateListener) {
        super(context, onTetherConfigUpdateListener);
        this.mMaxConnectionsValue = 6;
        this.mMaxConnectionsValue = Settings.System.getInt(context.getContentResolver(), "wifi_hotspot_max_client_num", 6);
    }

    @Override // com.android.settings.wifi.tether.WifiTetherBasePreferenceController
    public void updateDisplay() {
        ListPreference listPreference = (ListPreference) this.mPreference;
        listPreference.setSummary(listPreference.getEntries()[this.mMaxConnectionsValue - 1]);
        listPreference.setValue(String.valueOf(this.mMaxConnectionsValue));
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "wifi_tether_network_connections";
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        this.mMaxConnectionsValue = Integer.parseInt((String) obj);
        Settings.System.putInt(this.mContext.getContentResolver(), "wifi_hotspot_max_client_num", this.mMaxConnectionsValue);
        preference.setSummary(((ListPreference) preference).getEntries()[this.mMaxConnectionsValue - 1]);
        this.mListener.onSecurityChanged();
        return true;
    }
}
