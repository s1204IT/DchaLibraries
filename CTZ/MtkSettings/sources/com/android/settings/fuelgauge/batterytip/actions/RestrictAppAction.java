package com.android.settings.fuelgauge.batterytip.actions;

import android.content.Context;
import android.util.Pair;
import com.android.internal.util.CollectionUtils;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.fuelgauge.batterytip.AppInfo;
import com.android.settings.fuelgauge.batterytip.BatteryDatabaseManager;
import com.android.settings.fuelgauge.batterytip.tips.RestrictAppTip;
import java.util.Iterator;
import java.util.List;

public class RestrictAppAction extends BatteryTipAction {
    BatteryDatabaseManager mBatteryDatabaseManager;
    BatteryUtils mBatteryUtils;
    private RestrictAppTip mRestrictAppTip;

    public RestrictAppAction(Context context, RestrictAppTip restrictAppTip) {
        super(context);
        this.mRestrictAppTip = restrictAppTip;
        this.mBatteryUtils = BatteryUtils.getInstance(context);
        this.mBatteryDatabaseManager = BatteryDatabaseManager.getInstance(context);
    }

    @Override
    public void handlePositiveAction(int i) {
        List<AppInfo> restrictAppList = this.mRestrictAppTip.getRestrictAppList();
        int size = restrictAppList.size();
        for (int i2 = 0; i2 < size; i2++) {
            AppInfo appInfo = restrictAppList.get(i2);
            String str = appInfo.packageName;
            this.mBatteryUtils.setForceAppStandby(appInfo.uid, str, 1);
            if (CollectionUtils.isEmpty(appInfo.anomalyTypes)) {
                this.mMetricsFeatureProvider.action(this.mContext, 1362, str, Pair.create(833, Integer.valueOf(i)));
            } else {
                Iterator<Integer> it = appInfo.anomalyTypes.iterator();
                while (it.hasNext()) {
                    this.mMetricsFeatureProvider.action(this.mContext, 1362, str, Pair.create(833, Integer.valueOf(i)), Pair.create(1366, Integer.valueOf(it.next().intValue())));
                }
            }
        }
        this.mBatteryDatabaseManager.updateAnomalies(restrictAppList, 1);
    }
}
