package com.android.settings.fuelgauge.batterytip.actions;

import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.fuelgauge.RestrictedAppDetails;
import com.android.settings.fuelgauge.batterytip.AppInfo;
import com.android.settings.fuelgauge.batterytip.BatteryDatabaseManager;
import com.android.settings.fuelgauge.batterytip.tips.RestrictAppTip;
import com.android.settingslib.utils.ThreadUtils;
import java.util.List;

public class OpenRestrictAppFragmentAction extends BatteryTipAction {
    BatteryDatabaseManager mBatteryDatabaseManager;
    private final InstrumentedPreferenceFragment mFragment;
    private final RestrictAppTip mRestrictAppTip;

    public OpenRestrictAppFragmentAction(InstrumentedPreferenceFragment instrumentedPreferenceFragment, RestrictAppTip restrictAppTip) {
        super(instrumentedPreferenceFragment.getContext());
        this.mFragment = instrumentedPreferenceFragment;
        this.mRestrictAppTip = restrictAppTip;
        this.mBatteryDatabaseManager = BatteryDatabaseManager.getInstance(this.mContext);
    }

    @Override
    public void handlePositiveAction(int i) {
        this.mMetricsFeatureProvider.action(this.mContext, 1361, i);
        final List<AppInfo> restrictAppList = this.mRestrictAppTip.getRestrictAppList();
        RestrictedAppDetails.startRestrictedAppDetails(this.mFragment, restrictAppList);
        ThreadUtils.postOnBackgroundThread(new Runnable() {
            @Override
            public final void run() {
                this.f$0.mBatteryDatabaseManager.updateAnomalies(restrictAppList, 1);
            }
        });
    }
}
