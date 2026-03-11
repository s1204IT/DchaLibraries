package com.android.settings.wifi.tether;

import android.content.Context;
import android.content.res.Resources;
import android.icu.text.ListFormatter;
import android.net.wifi.WifiConfiguration;
import android.support.v7.preference.Preference;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.widget.HotspotApBandSelectionPreference;
import com.android.settings.wifi.tether.WifiTetherBasePreferenceController;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.IWifiTetherSettingsExt;

public class WifiTetherApBandPreferenceController extends WifiTetherBasePreferenceController {
    public static final String[] BAND_VALUES = {String.valueOf(0), String.valueOf(1)};
    private final String[] mBandEntries;
    private int mBandIndex;
    private final String[] mBandSummaries;
    private IWifiTetherSettingsExt mWifiTetherSettingsExt;

    public WifiTetherApBandPreferenceController(Context context, WifiTetherBasePreferenceController.OnTetherConfigUpdateListener onTetherConfigUpdateListener) {
        super(context, onTetherConfigUpdateListener);
        Resources resources = this.mContext.getResources();
        this.mBandEntries = resources.getStringArray(R.array.wifi_ap_band_config_full);
        this.mBandSummaries = resources.getStringArray(R.array.wifi_ap_band_summary_full);
        this.mWifiTetherSettingsExt = UtilsExt.getWifiTetherSettingsExt(context);
    }

    @Override
    public void updateDisplay() {
        WifiConfiguration wifiApConfiguration = this.mWifiManager.getWifiApConfiguration();
        if (wifiApConfiguration == null) {
            this.mBandIndex = 0;
            Log.d("WifiTetherApBandPref", "Updating band index to 0 because no config");
        } else if (is5GhzBandSupported()) {
            this.mBandIndex = wifiApConfiguration.apBand;
            Log.d("WifiTetherApBandPref", "Updating band index to " + this.mBandIndex);
        } else {
            wifiApConfiguration.apBand = 0;
            this.mWifiManager.setWifiApConfiguration(wifiApConfiguration);
            this.mBandIndex = wifiApConfiguration.apBand;
            Log.d("WifiTetherApBandPref", "5Ghz not supported, updating band index to " + this.mBandIndex);
        }
        HotspotApBandSelectionPreference hotspotApBandSelectionPreference = (HotspotApBandSelectionPreference) this.mPreference;
        if (!is5GhzBandSupported()) {
            hotspotApBandSelectionPreference.setEnabled(false);
            hotspotApBandSelectionPreference.setSummary(R.string.wifi_ap_choose_2G);
        } else {
            hotspotApBandSelectionPreference.setExistingConfigValue(wifiApConfiguration.apBand);
            hotspotApBandSelectionPreference.setSummary(getConfigSummary());
        }
    }

    String getConfigSummary() {
        if (this.mBandIndex == -1) {
            return ListFormatter.getInstance().format(this.mBandSummaries);
        }
        return this.mBandSummaries[this.mBandIndex];
    }

    @Override
    public String getPreferenceKey() {
        return "wifi_tether_network_ap_band";
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        this.mBandIndex = ((Integer) obj).intValue();
        Log.d("WifiTetherApBandPref", "Band preference changed, updating band index to " + this.mBandIndex);
        preference.setSummary(getConfigSummary());
        this.mListener.onTetherConfigUpdated();
        this.mWifiTetherSettingsExt.onPrefChangeNotify("wifi_tether_network_ap_band", obj);
        return true;
    }

    private boolean is5GhzBandSupported() {
        String countryCode = this.mWifiManager.getCountryCode();
        if (!this.mWifiManager.isDualBandSupported() || countryCode == null) {
            return false;
        }
        return true;
    }

    public int getBandIndex() {
        return this.mBandIndex;
    }
}
