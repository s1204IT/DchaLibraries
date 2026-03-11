package com.android.settings.fuelgauge.anomaly.action;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import com.android.settings.fuelgauge.anomaly.Anomaly;

public class ForceStopAction extends AnomalyAction {
    private ActivityManager mActivityManager;
    private PackageManager mPackageManager;

    public ForceStopAction(Context context) {
        super(context);
        this.mActivityManager = (ActivityManager) context.getSystemService("activity");
        this.mPackageManager = context.getPackageManager();
        this.mActionMetricKey = 807;
    }

    @Override
    public void handlePositiveAction(Anomaly anomaly, int i) {
        super.handlePositiveAction(anomaly, i);
        this.mActivityManager.forceStopPackage(anomaly.packageName);
    }

    @Override
    public boolean isActionActive(Anomaly anomaly) {
        try {
            return (2097152 & this.mPackageManager.getApplicationInfo(anomaly.packageName, 128).flags) == 0;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("ForceStopAction", "Cannot find info for app: " + anomaly.packageName);
            return false;
        }
    }

    @Override
    public int getActionType() {
        return 0;
    }
}
