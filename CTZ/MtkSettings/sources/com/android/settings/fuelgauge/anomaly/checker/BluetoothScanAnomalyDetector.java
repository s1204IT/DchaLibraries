package com.android.settings.fuelgauge.anomaly.checker;

import android.content.Context;
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

public class BluetoothScanAnomalyDetector implements AnomalyDetector {
    private AnomalyUtils mAnomalyUtils;
    BatteryUtils mBatteryUtils;
    private long mBluetoothScanningThreshold;
    private Context mContext;

    public BluetoothScanAnomalyDetector(Context context) {
        this(context, new AnomalyDetectionPolicy(context), AnomalyUtils.getInstance(context));
    }

    BluetoothScanAnomalyDetector(Context context, AnomalyDetectionPolicy anomalyDetectionPolicy, AnomalyUtils anomalyUtils) {
        this.mContext = context;
        this.mBatteryUtils = BatteryUtils.getInstance(context);
        this.mBluetoothScanningThreshold = anomalyDetectionPolicy.bluetoothScanThreshold;
        this.mAnomalyUtils = anomalyUtils;
    }

    @Override
    public List<Anomaly> detectAnomalies(BatteryStatsHelper batteryStatsHelper, String str) {
        List usageList = batteryStatsHelper.getUsageList();
        ArrayList arrayList = new ArrayList();
        int packageUid = this.mBatteryUtils.getPackageUid(str);
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        int size = usageList.size();
        for (int i = 0; i < size; i++) {
            BatterySipper batterySipper = (BatterySipper) usageList.get(i);
            BatteryStats.Uid uid = batterySipper.uidObj;
            if (uid != null && !this.mBatteryUtils.shouldHideSipper(batterySipper) && (packageUid == -1 || packageUid == uid.getUid())) {
                long bluetoothUnoptimizedBgTimeMs = getBluetoothUnoptimizedBgTimeMs(uid, jElapsedRealtime);
                if (bluetoothUnoptimizedBgTimeMs > this.mBluetoothScanningThreshold) {
                    String packageName = this.mBatteryUtils.getPackageName(uid.getUid());
                    Anomaly anomalyBuild = new Anomaly.Builder().setUid(uid.getUid()).setType(2).setDisplayName(Utils.getApplicationLabel(this.mContext, packageName)).setPackageName(packageName).setBluetoothScanningTimeMs(bluetoothUnoptimizedBgTimeMs).build();
                    if (this.mAnomalyUtils.getAnomalyAction(anomalyBuild).isActionActive(anomalyBuild)) {
                        arrayList.add(anomalyBuild);
                    }
                }
            }
        }
        return arrayList;
    }

    public long getBluetoothUnoptimizedBgTimeMs(BatteryStats.Uid uid, long j) {
        BatteryStats.Timer bluetoothUnoptimizedScanBackgroundTimer = uid.getBluetoothUnoptimizedScanBackgroundTimer();
        if (bluetoothUnoptimizedScanBackgroundTimer != null) {
            return bluetoothUnoptimizedScanBackgroundTimer.getTotalDurationMsLocked(j);
        }
        return 0L;
    }
}
