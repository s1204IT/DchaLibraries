package com.android.internal.os;

import android.os.BatteryStats;

public class FlashlightPowerCalculator extends PowerCalculator {
    private final double mFlashlightPowerOnAvg;

    public FlashlightPowerCalculator(PowerProfile powerProfile) {
        this.mFlashlightPowerOnAvg = powerProfile.getAveragePower(PowerProfile.POWER_FLASHLIGHT);
    }

    @Override
    public void calculateApp(BatterySipper batterySipper, BatteryStats.Uid uid, long j, long j2, int i) {
        BatteryStats.Timer flashlightTurnedOnTimer = uid.getFlashlightTurnedOnTimer();
        if (flashlightTurnedOnTimer != null) {
            long totalTimeLocked = flashlightTurnedOnTimer.getTotalTimeLocked(j, i) / 1000;
            batterySipper.flashlightTimeMs = totalTimeLocked;
            batterySipper.flashlightPowerMah = (totalTimeLocked * this.mFlashlightPowerOnAvg) / 3600000.0d;
        } else {
            batterySipper.flashlightTimeMs = 0L;
            batterySipper.flashlightPowerMah = 0.0d;
        }
    }
}
