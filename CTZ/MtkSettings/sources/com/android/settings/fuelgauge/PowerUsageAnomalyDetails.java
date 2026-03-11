package com.android.settings.fuelgauge;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.util.IconDrawableFactory;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.fuelgauge.anomaly.Anomaly;
import com.android.settings.fuelgauge.anomaly.AnomalyDialogFragment;
import com.android.settings.fuelgauge.anomaly.AnomalyPreference;
import com.android.settingslib.core.AbstractPreferenceController;
import java.util.List;

public class PowerUsageAnomalyDetails extends DashboardFragment implements AnomalyDialogFragment.AnomalyDialogListener {

    @VisibleForTesting
    static final String EXTRA_ANOMALY_LIST = "anomaly_list";

    @VisibleForTesting
    PreferenceGroup mAbnormalListGroup;

    @VisibleForTesting
    List<Anomaly> mAnomalies;

    @VisibleForTesting
    BatteryUtils mBatteryUtils;

    @VisibleForTesting
    IconDrawableFactory mIconDrawableFactory;

    @VisibleForTesting
    PackageManager mPackageManager;

    public static void startBatteryAbnormalPage(SettingsActivity settingsActivity, InstrumentedPreferenceFragment instrumentedPreferenceFragment, List<Anomaly> list) {
        Bundle bundle = new Bundle();
        bundle.putParcelableList(EXTRA_ANOMALY_LIST, list);
        new SubSettingLauncher(settingsActivity).setDestination(PowerUsageAnomalyDetails.class.getName()).setTitle(R.string.battery_abnormal_details_title).setArguments(bundle).setSourceMetricsCategory(instrumentedPreferenceFragment.getMetricsCategory()).launch();
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Context context = getContext();
        this.mAnomalies = getArguments().getParcelableArrayList(EXTRA_ANOMALY_LIST);
        this.mAbnormalListGroup = (PreferenceGroup) findPreference("app_abnormal_list");
        this.mPackageManager = context.getPackageManager();
        this.mIconDrawableFactory = IconDrawableFactory.newInstance(context);
        this.mBatteryUtils = BatteryUtils.getInstance(context);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshUi();
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference instanceof AnomalyPreference) {
            AnomalyDialogFragment anomalyDialogFragmentNewInstance = AnomalyDialogFragment.newInstance(((AnomalyPreference) preference).getAnomaly(), 987);
            anomalyDialogFragmentNewInstance.setTargetFragment(this, 0);
            anomalyDialogFragmentNewInstance.show(getFragmentManager(), "PowerAbnormalUsageDetail");
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    protected String getLogTag() {
        return "PowerAbnormalUsageDetail";
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.power_abnormal_detail;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return null;
    }

    @Override
    public int getMetricsCategory() {
        return 987;
    }

    void refreshUi() {
        this.mAbnormalListGroup.removeAll();
        int size = this.mAnomalies.size();
        for (int i = 0; i < size; i++) {
            Anomaly anomaly = this.mAnomalies.get(i);
            AnomalyPreference anomalyPreference = new AnomalyPreference(getPrefContext(), anomaly);
            anomalyPreference.setSummary(this.mBatteryUtils.getSummaryResIdFromAnomalyType(anomaly.type));
            Drawable badgedIcon = getBadgedIcon(anomaly.packageName, UserHandle.getUserId(anomaly.uid));
            if (badgedIcon != null) {
                anomalyPreference.setIcon(badgedIcon);
            }
            this.mAbnormalListGroup.addPreference(anomalyPreference);
        }
    }

    @Override
    public void onAnomalyHandled(Anomaly anomaly) {
        this.mAnomalies.remove(anomaly);
        refreshUi();
    }

    @VisibleForTesting
    Drawable getBadgedIcon(String str, int i) {
        return Utils.getBadgedIcon(this.mIconDrawableFactory, this.mPackageManager, str, i);
    }
}
