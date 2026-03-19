package com.android.settings.fuelgauge;

import android.content.Context;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settingslib.utils.AsyncLoader;

public class BatteryInfoLoader extends AsyncLoader<BatteryInfo> {

    @VisibleForTesting
    BatteryUtils batteryUtils;
    BatteryStatsHelper mStatsHelper;

    public BatteryInfoLoader(Context context, BatteryStatsHelper batteryStatsHelper) {
        super(context);
        this.mStatsHelper = batteryStatsHelper;
        this.batteryUtils = BatteryUtils.getInstance(context);
    }

    @Override
    protected void onDiscardResult(BatteryInfo batteryInfo) {
    }

    @Override
    public BatteryInfo loadInBackground() {
        return this.batteryUtils.getBatteryInfo(this.mStatsHelper, "BatteryInfoLoader");
    }
}
