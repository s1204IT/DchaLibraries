package com.android.settings.fuelgauge.anomaly;

import android.content.Context;
import android.support.v7.preference.Preference;

public class AnomalyPreference extends Preference {
    private Anomaly mAnomaly;

    public AnomalyPreference(Context context, Anomaly anomaly) {
        super(context);
        this.mAnomaly = anomaly;
        if (anomaly != null) {
            setTitle(anomaly.displayName);
        }
    }

    public Anomaly getAnomaly() {
        return this.mAnomaly;
    }
}
