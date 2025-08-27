package com.android.settings.fuelgauge.anomaly.checker;

import android.content.Context;
import android.os.BatteryStats;
import android.util.ArrayMap;
import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.Utils;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.fuelgauge.anomaly.Anomaly;
import com.android.settings.fuelgauge.anomaly.AnomalyDetectionPolicy;
import com.android.settings.fuelgauge.anomaly.AnomalyUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/* loaded from: classes.dex */
public class WakeupAlarmAnomalyDetector implements AnomalyDetector {
    private AnomalyUtils mAnomalyUtils;
    BatteryUtils mBatteryUtils;
    private Context mContext;
    private long mWakeupAlarmThreshold;
    private Set<String> mWakeupBlacklistedTags;

    public WakeupAlarmAnomalyDetector(Context context) {
        this(context, new AnomalyDetectionPolicy(context), AnomalyUtils.getInstance(context));
    }

    WakeupAlarmAnomalyDetector(Context context, AnomalyDetectionPolicy anomalyDetectionPolicy, AnomalyUtils anomalyUtils) {
        this.mContext = context;
        this.mBatteryUtils = BatteryUtils.getInstance(context);
        this.mAnomalyUtils = anomalyUtils;
        this.mWakeupAlarmThreshold = anomalyDetectionPolicy.wakeupAlarmThreshold;
        this.mWakeupBlacklistedTags = anomalyDetectionPolicy.wakeupBlacklistedTags;
    }

    @Override // com.android.settings.fuelgauge.anomaly.checker.AnomalyDetector
    public List<Anomaly> detectAnomalies(BatteryStatsHelper batteryStatsHelper, String str) {
        List usageList = batteryStatsHelper.getUsageList();
        ArrayList arrayList = new ArrayList();
        double dCalculateRunningTimeBasedOnStatsType = this.mBatteryUtils.calculateRunningTimeBasedOnStatsType(batteryStatsHelper, 0) / 3600000.0d;
        int packageUid = this.mBatteryUtils.getPackageUid(str);
        if (dCalculateRunningTimeBasedOnStatsType >= 1.0d) {
            int size = usageList.size();
            for (int i = 0; i < size; i++) {
                BatterySipper batterySipper = (BatterySipper) usageList.get(i);
                BatteryStats.Uid uid = batterySipper.uidObj;
                if (uid != null && !this.mBatteryUtils.shouldHideSipper(batterySipper) && (packageUid == -1 || packageUid == uid.getUid())) {
                    int wakeupAlarmCountFromUid = (int) (getWakeupAlarmCountFromUid(uid) / dCalculateRunningTimeBasedOnStatsType);
                    if (wakeupAlarmCountFromUid > this.mWakeupAlarmThreshold) {
                        String packageName = this.mBatteryUtils.getPackageName(uid.getUid());
                        CharSequence applicationLabel = Utils.getApplicationLabel(this.mContext, packageName);
                        int targetSdkVersion = this.mBatteryUtils.getTargetSdkVersion(packageName);
                        Anomaly anomalyBuild = new Anomaly.Builder().setUid(uid.getUid()).setType(1).setDisplayName(applicationLabel).setPackageName(packageName).setTargetSdkVersion(targetSdkVersion).setBackgroundRestrictionEnabled(this.mBatteryUtils.isBackgroundRestrictionEnabled(targetSdkVersion, uid.getUid(), packageName)).setWakeupAlarmCount(wakeupAlarmCountFromUid).build();
                        if (this.mAnomalyUtils.getAnomalyAction(anomalyBuild).isActionActive(anomalyBuild)) {
                            arrayList.add(anomalyBuild);
                        }
                    }
                }
            }
        }
        return arrayList;
    }

    int getWakeupAlarmCountFromUid(BatteryStats.Uid uid) {
        ArrayMap packageStats = uid.getPackageStats();
        int countLocked = 0;
        for (int size = packageStats.size() - 1; size >= 0; size--) {
            for (Map.Entry entry : ((BatteryStats.Uid.Pkg) packageStats.valueAt(size)).getWakeupAlarmStats().entrySet()) {
                if (this.mWakeupBlacklistedTags == null || !this.mWakeupBlacklistedTags.contains(entry.getKey())) {
                    countLocked += ((BatteryStats.Counter) entry.getValue()).getCountLocked(0);
                }
            }
        }
        return countLocked;
    }
}
