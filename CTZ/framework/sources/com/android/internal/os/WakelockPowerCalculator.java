package com.android.internal.os;

import android.os.BatteryStats;
import android.util.ArrayMap;

public class WakelockPowerCalculator extends PowerCalculator {
    private static final boolean DEBUG = false;
    private static final String TAG = "WakelockPowerCalculator";
    private final double mPowerWakelock;
    private long mTotalAppWakelockTimeMs = 0;

    public WakelockPowerCalculator(PowerProfile powerProfile) {
        this.mPowerWakelock = powerProfile.getAveragePower(PowerProfile.POWER_CPU_IDLE);
    }

    @Override
    public void calculateApp(BatterySipper batterySipper, BatteryStats.Uid uid, long j, long j2, int i) {
        ArrayMap<String, ? extends BatteryStats.Uid.Wakelock> wakelockStats = uid.getWakelockStats();
        int size = wakelockStats.size();
        long totalTimeLocked = 0;
        for (int i2 = 0; i2 < size; i2++) {
            BatteryStats.Timer wakeTime = wakelockStats.valueAt(i2).getWakeTime(0);
            if (wakeTime != null) {
                totalTimeLocked += wakeTime.getTotalTimeLocked(j, i);
            }
        }
        batterySipper.wakeLockTimeMs = totalTimeLocked / 1000;
        this.mTotalAppWakelockTimeMs += batterySipper.wakeLockTimeMs;
        batterySipper.wakeLockPowerMah = (batterySipper.wakeLockTimeMs * this.mPowerWakelock) / 3600000.0d;
    }

    @Override
    public void calculateRemaining(BatterySipper batterySipper, BatteryStats batteryStats, long j, long j2, int i) {
        long batteryUptime = (batteryStats.getBatteryUptime(j2) / 1000) - (this.mTotalAppWakelockTimeMs + (batteryStats.getScreenOnTime(j, i) / 1000));
        if (batteryUptime > 0) {
            double d = (batteryUptime * this.mPowerWakelock) / 3600000.0d;
            batterySipper.wakeLockTimeMs += batteryUptime;
            batterySipper.wakeLockPowerMah += d;
        }
    }

    @Override
    public void reset() {
        this.mTotalAppWakelockTimeMs = 0L;
    }
}
