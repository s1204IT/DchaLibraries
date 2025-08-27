package com.android.settings.fuelgauge.anomaly;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.preference.Preference;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.fuelgauge.PowerUsageAnomalyDetails;
import java.util.List;

/* loaded from: classes.dex */
public class AnomalySummaryPreferenceController {
    List<Anomaly> mAnomalies;
    Preference mAnomalyPreference;
    BatteryUtils mBatteryUtils;
    private InstrumentedPreferenceFragment mFragment;
    private int mMetricsKey;
    private SettingsActivity mSettingsActivity;

    public AnomalySummaryPreferenceController(SettingsActivity settingsActivity, InstrumentedPreferenceFragment instrumentedPreferenceFragment) {
        this.mFragment = instrumentedPreferenceFragment;
        this.mSettingsActivity = settingsActivity;
        this.mAnomalyPreference = this.mFragment.getPreferenceScreen().findPreference("high_usage");
        this.mMetricsKey = instrumentedPreferenceFragment.getMetricsCategory();
        this.mBatteryUtils = BatteryUtils.getInstance(settingsActivity.getApplicationContext());
        hideHighUsagePreference();
    }

    public boolean onPreferenceTreeClick(Preference preference) {
        if (this.mAnomalies == null || !"high_usage".equals(preference.getKey())) {
            return false;
        }
        if (this.mAnomalies.size() == 1) {
            AnomalyDialogFragment anomalyDialogFragmentNewInstance = AnomalyDialogFragment.newInstance(this.mAnomalies.get(0), this.mMetricsKey);
            anomalyDialogFragmentNewInstance.setTargetFragment(this.mFragment, 0);
            anomalyDialogFragmentNewInstance.show(this.mFragment.getFragmentManager(), "HighUsagePreferenceController");
        } else {
            PowerUsageAnomalyDetails.startBatteryAbnormalPage(this.mSettingsActivity, this.mFragment, this.mAnomalies);
        }
        return true;
    }

    public void updateAnomalySummaryPreference(List<Anomaly> list) throws Resources.NotFoundException {
        String string;
        Context context = this.mFragment.getContext();
        this.mAnomalies = list;
        if (!this.mAnomalies.isEmpty()) {
            this.mAnomalyPreference.setVisible(true);
            int size = this.mAnomalies.size();
            String quantityString = context.getResources().getQuantityString(R.plurals.power_high_usage_title, size, this.mAnomalies.get(0).displayName);
            if (size > 1) {
                string = context.getString(R.string.battery_abnormal_apps_summary, Integer.valueOf(size));
            } else {
                string = context.getString(this.mBatteryUtils.getSummaryResIdFromAnomalyType(this.mAnomalies.get(0).type));
            }
            this.mAnomalyPreference.setTitle(quantityString);
            this.mAnomalyPreference.setSummary(string);
            return;
        }
        this.mAnomalyPreference.setVisible(false);
    }

    public void hideHighUsagePreference() {
        this.mAnomalyPreference.setVisible(false);
    }
}
