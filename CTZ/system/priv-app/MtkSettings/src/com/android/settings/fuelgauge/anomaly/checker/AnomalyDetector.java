package com.android.settings.fuelgauge.anomaly.checker;

import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.fuelgauge.anomaly.Anomaly;
import java.util.List;
/* loaded from: classes.dex */
public interface AnomalyDetector {
    List<Anomaly> detectAnomalies(BatteryStatsHelper batteryStatsHelper, String str);
}
