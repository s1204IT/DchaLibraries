package com.android.settings.wifi;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.utils.AnnotationSpan;
import com.android.settingslib.core.AbstractPreferenceController;

/* loaded from: classes.dex */
public class WifiWakeupPreferenceController extends AbstractPreferenceController {
    private final Fragment mFragment;
    LocationManager mLocationManager;
    SwitchPreference mPreference;

    public WifiWakeupPreferenceController(Context context, DashboardFragment dashboardFragment) {
        super(context);
        this.mFragment = dashboardFragment;
        this.mLocationManager = (LocationManager) context.getSystemService("location");
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mPreference = (SwitchPreference) preferenceScreen.findPreference("enable_wifi_wakeup");
        updateState(this.mPreference);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), "enable_wifi_wakeup") || !(preference instanceof SwitchPreference)) {
            return false;
        }
        if (!this.mLocationManager.isLocationEnabled()) {
            this.mFragment.startActivity(new Intent("android.settings.LOCATION_SOURCE_SETTINGS"));
        } else if (getWifiWakeupEnabled()) {
            setWifiWakeupEnabled(false);
        } else if (!getWifiScanningEnabled()) {
            showScanningDialog();
        } else {
            setWifiWakeupEnabled(true);
        }
        updateState(this.mPreference);
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "enable_wifi_wakeup";
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        if (!(preference instanceof SwitchPreference)) {
            return;
        }
        ((SwitchPreference) preference).setChecked(getWifiWakeupEnabled() && getWifiScanningEnabled() && this.mLocationManager.isLocationEnabled());
        if (!this.mLocationManager.isLocationEnabled()) {
            preference.setSummary(getNoLocationSummary());
        } else {
            preference.setSummary(R.string.wifi_wakeup_summary);
        }
    }

    CharSequence getNoLocationSummary() {
        return AnnotationSpan.linkify(this.mContext.getText(R.string.wifi_wakeup_summary_no_location), new AnnotationSpan.LinkInfo("link", null));
    }

    public void onActivityResult(int i, int i2) {
        if (i != 600) {
            return;
        }
        if (this.mLocationManager.isLocationEnabled()) {
            setWifiWakeupEnabled(true);
        }
        updateState(this.mPreference);
    }

    private boolean getWifiScanningEnabled() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_scan_always_enabled", 0) == 1;
    }

    private void showScanningDialog() {
        WifiScanningRequiredFragment wifiScanningRequiredFragmentNewInstance = WifiScanningRequiredFragment.newInstance();
        wifiScanningRequiredFragmentNewInstance.setTargetFragment(this.mFragment, 600);
        wifiScanningRequiredFragmentNewInstance.show(this.mFragment.getFragmentManager(), "WifiWakeupPrefController");
    }

    private boolean getWifiWakeupEnabled() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_wakeup_enabled", 0) == 1;
    }

    private void setWifiWakeupEnabled(boolean z) {
        Settings.Global.putInt(this.mContext.getContentResolver(), "wifi_wakeup_enabled", z ? 1 : 0);
    }
}
