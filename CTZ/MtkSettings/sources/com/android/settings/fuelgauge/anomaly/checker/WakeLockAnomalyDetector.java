package com.android.settings.fuelgauge.anomaly.checker;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.BatteryStats;
import android.os.SystemClock;
import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.Utils;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.fuelgauge.anomaly.Anomaly;
import com.android.settings.fuelgauge.anomaly.AnomalyDetectionPolicy;
import com.android.settings.fuelgauge.anomaly.AnomalyUtils;
import java.util.ArrayList;
import java.util.List;

public class WakeLockAnomalyDetector implements AnomalyDetector {
    private AnomalyUtils mAnomalyUtils;
    BatteryUtils mBatteryUtils;
    private Context mContext;
    private PackageManager mPackageManager;
    long mWakeLockThresholdMs;

    public WakeLockAnomalyDetector(Context context) {
        this(context, new AnomalyDetectionPolicy(context), AnomalyUtils.getInstance(context));
    }

    WakeLockAnomalyDetector(Context context, AnomalyDetectionPolicy anomalyDetectionPolicy, AnomalyUtils anomalyUtils) {
        this.mContext = context;
        this.mPackageManager = context.getPackageManager();
        this.mBatteryUtils = BatteryUtils.getInstance(context);
        this.mAnomalyUtils = anomalyUtils;
        this.mWakeLockThresholdMs = anomalyDetectionPolicy.wakeLockThreshold;
    }

    @Override
    public List<Anomaly> detectAnomalies(BatteryStatsHelper batteryStatsHelper, String str) {
        int i;
        List usageList = batteryStatsHelper.getUsageList();
        ArrayList arrayList = new ArrayList();
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        int packageUid = this.mBatteryUtils.getPackageUid(str);
        int size = usageList.size();
        for (int i2 = 0; i2 < size; i2 = i + 1) {
            BatterySipper batterySipper = (BatterySipper) usageList.get(i2);
            BatteryStats.Uid uid = batterySipper.uidObj;
            if (uid == null || this.mBatteryUtils.shouldHideSipper(batterySipper) || (packageUid != -1 && packageUid != uid.getUid())) {
                i = i2;
            } else {
                long currentDurationMs = getCurrentDurationMs(uid, jElapsedRealtime);
                long backgroundTotalDurationMs = getBackgroundTotalDurationMs(uid, jElapsedRealtime);
                i = i2;
                if (backgroundTotalDurationMs > this.mWakeLockThresholdMs && currentDurationMs != 0) {
                    String packageName = this.mBatteryUtils.getPackageName(uid.getUid());
                    Anomaly anomalyBuild = new Anomaly.Builder().setUid(uid.getUid()).setType(0).setDisplayName(Utils.getApplicationLabel(this.mContext, packageName)).setPackageName(packageName).setWakeLockTimeMs(backgroundTotalDurationMs).build();
                    if (this.mAnomalyUtils.getAnomalyAction(anomalyBuild).isActionActive(anomalyBuild)) {
                        arrayList.add(anomalyBuild);
                    }
                }
            }
        }
        return arrayList;
    }

    long getCurrentDurationMs(BatteryStats.Uid uid, long j) {
        BatteryStats.Timer aggregatedPartialWakelockTimer = uid.getAggregatedPartialWakelockTimer();
        if (aggregatedPartialWakelockTimer != null) {
            return aggregatedPartialWakelockTimer.getCurrentDurationMsLocked(j);
        }
        return 0L;
    }

    long getBackgroundTotalDurationMs(BatteryStats.Uid uid, long j) {
        BatteryStats.Timer aggregatedPartialWakelockTimer = uid.getAggregatedPartialWakelockTimer();
        BatteryStats.Timer subTimer = aggregatedPartialWakelockTimer != null ? aggregatedPartialWakelockTimer.getSubTimer() : null;
        if (subTimer != null) {
            return subTimer.getTotalDurationMsLocked(j);
        }
        return 0L;
    }
}
