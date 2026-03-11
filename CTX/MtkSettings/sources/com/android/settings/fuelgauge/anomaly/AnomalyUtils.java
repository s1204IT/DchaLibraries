package com.android.settings.fuelgauge.anomaly;

import android.content.Context;
import android.util.Pair;
import android.util.SparseIntArray;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.fuelgauge.anomaly.action.AnomalyAction;
import com.android.settings.fuelgauge.anomaly.action.ForceStopAction;
import com.android.settings.fuelgauge.anomaly.action.LocationCheckAction;
import com.android.settings.fuelgauge.anomaly.action.StopAndBackgroundCheckAction;
import com.android.settings.fuelgauge.anomaly.checker.AnomalyDetector;
import com.android.settings.fuelgauge.anomaly.checker.BluetoothScanAnomalyDetector;
import com.android.settings.fuelgauge.anomaly.checker.WakeLockAnomalyDetector;
import com.android.settings.fuelgauge.anomaly.checker.WakeupAlarmAnomalyDetector;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import java.util.ArrayList;
import java.util.List;

public class AnomalyUtils {
    private static final SparseIntArray mMetricArray = new SparseIntArray();
    private static AnomalyUtils sInstance;
    private Context mContext;

    static {
        mMetricArray.append(0, 1235);
        mMetricArray.append(1, 1236);
        mMetricArray.append(2, 1237);
    }

    AnomalyUtils(Context context) {
        this.mContext = context.getApplicationContext();
    }

    public static AnomalyUtils getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new AnomalyUtils(context);
        }
        return sInstance;
    }

    public AnomalyAction getAnomalyAction(Anomaly anomaly) {
        switch (anomaly.type) {
            case 0:
                return new ForceStopAction(this.mContext);
            case 1:
                if (anomaly.targetSdkVersion >= 26 || (anomaly.targetSdkVersion < 26 && anomaly.backgroundRestrictionEnabled)) {
                    return new ForceStopAction(this.mContext);
                }
                return new StopAndBackgroundCheckAction(this.mContext);
            case 2:
                return new LocationCheckAction(this.mContext);
            default:
                return null;
        }
    }

    public AnomalyDetector getAnomalyDetector(int i) {
        switch (i) {
            case 0:
                return new WakeLockAnomalyDetector(this.mContext);
            case 1:
                return new WakeupAlarmAnomalyDetector(this.mContext);
            case 2:
                return new BluetoothScanAnomalyDetector(this.mContext);
            default:
                return null;
        }
    }

    public List<Anomaly> detectAnomalies(BatteryStatsHelper batteryStatsHelper, AnomalyDetectionPolicy anomalyDetectionPolicy, String str) {
        ArrayList arrayList = new ArrayList();
        for (int i : Anomaly.ANOMALY_TYPE_LIST) {
            if (anomalyDetectionPolicy.isAnomalyDetectorEnabled(i)) {
                arrayList.addAll(getAnomalyDetector(i).detectAnomalies(batteryStatsHelper, str));
            }
        }
        return arrayList;
    }

    public void logAnomalies(MetricsFeatureProvider metricsFeatureProvider, List<Anomaly> list, int i) {
        int size = list.size();
        for (int i2 = 0; i2 < size; i2++) {
            logAnomaly(metricsFeatureProvider, list.get(i2), i);
        }
    }

    public void logAnomaly(MetricsFeatureProvider metricsFeatureProvider, Anomaly anomaly, int i) {
        metricsFeatureProvider.action(this.mContext, mMetricArray.get(anomaly.type, 0), anomaly.packageName, Pair.create(833, Integer.valueOf(i)), Pair.create(1234, Integer.valueOf(getAnomalyAction(anomaly).getActionType())));
    }
}
