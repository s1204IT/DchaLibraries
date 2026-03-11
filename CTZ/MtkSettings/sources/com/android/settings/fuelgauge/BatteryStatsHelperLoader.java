package com.android.settings.fuelgauge;

import android.content.Context;
import android.os.UserManager;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settingslib.utils.AsyncLoader;

public class BatteryStatsHelperLoader extends AsyncLoader<BatteryStatsHelper> {
    BatteryUtils mBatteryUtils;
    UserManager mUserManager;

    public BatteryStatsHelperLoader(Context context) {
        super(context);
        this.mUserManager = (UserManager) context.getSystemService("user");
        this.mBatteryUtils = BatteryUtils.getInstance(context);
    }

    @Override
    public BatteryStatsHelper loadInBackground() {
        BatteryStatsHelper batteryStatsHelper = new BatteryStatsHelper(getContext(), true);
        this.mBatteryUtils.initBatteryStatsHelper(batteryStatsHelper, null, this.mUserManager);
        return batteryStatsHelper;
    }

    @Override
    public void onDiscardResult(BatteryStatsHelper batteryStatsHelper) {
    }
}
