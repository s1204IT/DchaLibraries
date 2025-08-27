package com.android.settings.fuelgauge;

import android.content.Context;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settingslib.utils.AsyncLoader;

/* loaded from: classes.dex */
public class BatteryInfoLoader extends AsyncLoader<BatteryInfo> {

    @VisibleForTesting
    BatteryUtils batteryUtils;
    BatteryStatsHelper mStatsHelper;

    public BatteryInfoLoader(Context context, BatteryStatsHelper batteryStatsHelper) {
        super(context);
        this.mStatsHelper = batteryStatsHelper;
        this.batteryUtils = BatteryUtils.getInstance(context);
    }

    /* JADX DEBUG: Method merged with bridge method: onDiscardResult(Ljava/lang/Object;)V */
    @Override // com.android.settingslib.utils.AsyncLoader
    protected void onDiscardResult(BatteryInfo batteryInfo) {
    }

    /* JADX DEBUG: Method merged with bridge method: loadInBackground()Ljava/lang/Object; */
    @Override // android.content.AsyncTaskLoader
    public BatteryInfo loadInBackground() {
        return this.batteryUtils.getBatteryInfo(this.mStatsHelper, "BatteryInfoLoader");
    }
}
