package com.android.settings.fuelgauge.anomaly.action;

import android.app.AppOpsManager;
import android.content.Context;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.fuelgauge.anomaly.Anomaly;

/* loaded from: classes.dex */
public class BackgroundCheckAction extends AnomalyAction {
    private AppOpsManager mAppOpsManager;
    BatteryUtils mBatteryUtils;

    public BackgroundCheckAction(Context context) {
        super(context);
        this.mAppOpsManager = (AppOpsManager) context.getSystemService("appops");
        this.mActionMetricKey = 1020;
        this.mBatteryUtils = BatteryUtils.getInstance(context);
    }

    @Override // com.android.settings.fuelgauge.anomaly.action.AnomalyAction
    public void handlePositiveAction(Anomaly anomaly, int i) {
        super.handlePositiveAction(anomaly, i);
        if (anomaly.targetSdkVersion < 26) {
            this.mAppOpsManager.setMode(63, anomaly.uid, anomaly.packageName, 1);
        }
    }

    @Override // com.android.settings.fuelgauge.anomaly.action.AnomalyAction
    public boolean isActionActive(Anomaly anomaly) {
        return !this.mBatteryUtils.isBackgroundRestrictionEnabled(anomaly.targetSdkVersion, anomaly.uid, anomaly.packageName);
    }

    @Override // com.android.settings.fuelgauge.anomaly.action.AnomalyAction
    public int getActionType() {
        return 1;
    }
}
