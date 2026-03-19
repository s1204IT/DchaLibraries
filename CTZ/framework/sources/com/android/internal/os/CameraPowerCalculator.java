package com.android.internal.os;

import android.os.BatteryStats;

public class CameraPowerCalculator extends PowerCalculator {
    private final double mCameraPowerOnAvg;

    public CameraPowerCalculator(PowerProfile powerProfile) {
        this.mCameraPowerOnAvg = powerProfile.getAveragePower(PowerProfile.POWER_CAMERA);
    }

    @Override
    public void calculateApp(BatterySipper batterySipper, BatteryStats.Uid uid, long j, long j2, int i) {
        BatteryStats.Timer cameraTurnedOnTimer = uid.getCameraTurnedOnTimer();
        if (cameraTurnedOnTimer != null) {
            long totalTimeLocked = cameraTurnedOnTimer.getTotalTimeLocked(j, i) / 1000;
            batterySipper.cameraTimeMs = totalTimeLocked;
            batterySipper.cameraPowerMah = (totalTimeLocked * this.mCameraPowerOnAvg) / 3600000.0d;
        } else {
            batterySipper.cameraTimeMs = 0L;
            batterySipper.cameraPowerMah = 0.0d;
        }
    }
}
