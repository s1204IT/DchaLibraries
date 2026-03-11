package com.android.settings.fuelgauge.anomaly.action;

import android.content.Context;
import android.util.Pair;
import com.android.settings.fuelgauge.anomaly.Anomaly;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

public abstract class AnomalyAction {
    protected int mActionMetricKey;
    protected Context mContext;
    private MetricsFeatureProvider mMetricsFeatureProvider;

    public abstract int getActionType();

    public abstract boolean isActionActive(Anomaly anomaly);

    public AnomalyAction(Context context) {
        this.mContext = context;
        this.mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
    }

    public void handlePositiveAction(Anomaly anomaly, int i) {
        this.mMetricsFeatureProvider.action(this.mContext, this.mActionMetricKey, anomaly.packageName, Pair.create(833, Integer.valueOf(i)));
    }
}
