package com.android.settings.fuelgauge.anomaly.action;

import android.content.Context;
import com.android.settings.fuelgauge.anomaly.Anomaly;

/* loaded from: classes.dex */
public class StopAndBackgroundCheckAction extends AnomalyAction {
    BackgroundCheckAction mBackgroundCheckAction;
    ForceStopAction mForceStopAction;

    public StopAndBackgroundCheckAction(Context context) {
        this(context, new ForceStopAction(context), new BackgroundCheckAction(context));
        this.mActionMetricKey = 1233;
    }

    StopAndBackgroundCheckAction(Context context, ForceStopAction forceStopAction, BackgroundCheckAction backgroundCheckAction) {
        super(context);
        this.mForceStopAction = forceStopAction;
        this.mBackgroundCheckAction = backgroundCheckAction;
    }

    @Override // com.android.settings.fuelgauge.anomaly.action.AnomalyAction
    public void handlePositiveAction(Anomaly anomaly, int i) {
        super.handlePositiveAction(anomaly, i);
        this.mForceStopAction.handlePositiveAction(anomaly, i);
        this.mBackgroundCheckAction.handlePositiveAction(anomaly, i);
    }

    @Override // com.android.settings.fuelgauge.anomaly.action.AnomalyAction
    public boolean isActionActive(Anomaly anomaly) {
        return this.mForceStopAction.isActionActive(anomaly) && this.mBackgroundCheckAction.isActionActive(anomaly);
    }

    @Override // com.android.settings.fuelgauge.anomaly.action.AnomalyAction
    public int getActionType() {
        return 3;
    }
}
