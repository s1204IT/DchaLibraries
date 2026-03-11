package com.android.settings.fuelgauge.anomaly.action;

import android.content.Context;
import android.content.pm.permission.RuntimePermissionPresenter;
import android.support.v4.content.PermissionChecker;
import com.android.settings.fuelgauge.anomaly.Anomaly;

public class LocationCheckAction extends AnomalyAction {
    private final RuntimePermissionPresenter mRuntimePermissionPresenter;

    public LocationCheckAction(Context context) {
        this(context, RuntimePermissionPresenter.getInstance(context));
    }

    LocationCheckAction(Context context, RuntimePermissionPresenter runtimePermissionPresenter) {
        super(context);
        this.mRuntimePermissionPresenter = runtimePermissionPresenter;
        this.mActionMetricKey = 1021;
    }

    @Override
    public void handlePositiveAction(Anomaly anomaly, int i) {
        super.handlePositiveAction(anomaly, i);
        this.mRuntimePermissionPresenter.revokeRuntimePermission(anomaly.packageName, "android.permission.ACCESS_COARSE_LOCATION");
        this.mRuntimePermissionPresenter.revokeRuntimePermission(anomaly.packageName, "android.permission.ACCESS_FINE_LOCATION");
    }

    @Override
    public boolean isActionActive(Anomaly anomaly) {
        return isPermissionGranted(anomaly, "android.permission.ACCESS_COARSE_LOCATION") || isPermissionGranted(anomaly, "android.permission.ACCESS_FINE_LOCATION");
    }

    @Override
    public int getActionType() {
        return 2;
    }

    private boolean isPermissionGranted(Anomaly anomaly, String str) {
        return PermissionChecker.checkPermission(this.mContext, str, -1, anomaly.uid, anomaly.packageName) == 0;
    }
}
