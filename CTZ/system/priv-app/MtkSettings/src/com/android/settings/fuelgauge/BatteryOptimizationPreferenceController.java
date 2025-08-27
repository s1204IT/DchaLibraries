package com.android.settings.fuelgauge;

import android.os.Bundle;
import android.support.v7.preference.Preference;
import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.SettingsActivity;
import com.android.settings.applications.manageapplications.ManageApplications;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.fuelgauge.PowerWhitelistBackend;

/* loaded from: classes.dex */
public class BatteryOptimizationPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    private PowerWhitelistBackend mBackend;
    private DashboardFragment mFragment;
    private String mPackageName;
    private SettingsActivity mSettingsActivity;

    public BatteryOptimizationPreferenceController(SettingsActivity settingsActivity, DashboardFragment dashboardFragment, String str) {
        super(settingsActivity);
        this.mFragment = dashboardFragment;
        this.mSettingsActivity = settingsActivity;
        this.mPackageName = str;
        this.mBackend = PowerWhitelistBackend.getInstance(this.mSettingsActivity);
    }

    BatteryOptimizationPreferenceController(SettingsActivity settingsActivity, DashboardFragment dashboardFragment, String str, PowerWhitelistBackend powerWhitelistBackend) {
        super(settingsActivity);
        this.mFragment = dashboardFragment;
        this.mSettingsActivity = settingsActivity;
        this.mPackageName = str;
        this.mBackend = powerWhitelistBackend;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        preference.setSummary(this.mBackend.isWhitelisted(this.mPackageName) ? R.string.high_power_on : R.string.high_power_off);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "battery_optimization";
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!"battery_optimization".equals(preference.getKey())) {
            return false;
        }
        Bundle bundle = new Bundle();
        bundle.putString("classname", Settings.HighPowerApplicationsActivity.class.getName());
        new SubSettingLauncher(this.mSettingsActivity).setDestination(ManageApplications.class.getName()).setArguments(bundle).setTitle(R.string.high_power_apps).setSourceMetricsCategory(this.mFragment.getMetricsCategory()).launch();
        return true;
    }
}
