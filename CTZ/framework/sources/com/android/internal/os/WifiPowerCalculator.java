package com.android.internal.os;

import android.os.BatteryStats;

public class WifiPowerCalculator extends PowerCalculator {
    private static final boolean DEBUG = false;
    private static final String TAG = "WifiPowerCalculator";
    private final double mIdleCurrentMa;
    private final double mRxCurrentMa;
    private double mTotalAppPowerDrain = 0.0d;
    private long mTotalAppRunningTime = 0;
    private final double mTxCurrentMa;

    public WifiPowerCalculator(PowerProfile powerProfile) {
        this.mIdleCurrentMa = powerProfile.getAveragePower(PowerProfile.POWER_WIFI_CONTROLLER_IDLE);
        this.mTxCurrentMa = powerProfile.getAveragePower(PowerProfile.POWER_WIFI_CONTROLLER_TX);
        this.mRxCurrentMa = powerProfile.getAveragePower(PowerProfile.POWER_WIFI_CONTROLLER_RX);
    }

    @Override
    public void calculateApp(BatterySipper batterySipper, BatteryStats.Uid uid, long j, long j2, int i) {
        BatteryStats.ControllerActivityCounter wifiControllerActivity = uid.getWifiControllerActivity();
        if (wifiControllerActivity == null) {
            return;
        }
        long countLocked = wifiControllerActivity.getIdleTimeCounter().getCountLocked(i);
        long countLocked2 = wifiControllerActivity.getTxTimeCounters()[0].getCountLocked(i);
        long countLocked3 = wifiControllerActivity.getRxTimeCounter().getCountLocked(i);
        batterySipper.wifiRunningTimeMs = countLocked + countLocked3 + countLocked2;
        this.mTotalAppRunningTime += batterySipper.wifiRunningTimeMs;
        batterySipper.wifiPowerMah = (((countLocked * this.mIdleCurrentMa) + (countLocked2 * this.mTxCurrentMa)) + (countLocked3 * this.mRxCurrentMa)) / 3600000.0d;
        this.mTotalAppPowerDrain += batterySipper.wifiPowerMah;
        batterySipper.wifiRxPackets = uid.getNetworkActivityPackets(2, i);
        batterySipper.wifiTxPackets = uid.getNetworkActivityPackets(3, i);
        batterySipper.wifiRxBytes = uid.getNetworkActivityBytes(2, i);
        batterySipper.wifiTxBytes = uid.getNetworkActivityBytes(3, i);
    }

    @Override
    public void calculateRemaining(BatterySipper batterySipper, BatteryStats batteryStats, long j, long j2, int i) {
        BatteryStats.ControllerActivityCounter wifiControllerActivity = batteryStats.getWifiControllerActivity();
        long countLocked = wifiControllerActivity.getIdleTimeCounter().getCountLocked(i);
        long countLocked2 = wifiControllerActivity.getTxTimeCounters()[0].getCountLocked(i);
        long countLocked3 = wifiControllerActivity.getRxTimeCounter().getCountLocked(i);
        batterySipper.wifiRunningTimeMs = Math.max(0L, ((countLocked + countLocked3) + countLocked2) - this.mTotalAppRunningTime);
        double countLocked4 = wifiControllerActivity.getPowerCounter().getCountLocked(i) / 3600000.0d;
        if (countLocked4 == 0.0d) {
            countLocked4 = (((countLocked * this.mIdleCurrentMa) + (countLocked2 * this.mTxCurrentMa)) + (countLocked3 * this.mRxCurrentMa)) / 3600000.0d;
        }
        batterySipper.wifiPowerMah = Math.max(0.0d, countLocked4 - this.mTotalAppPowerDrain);
    }

    @Override
    public void reset() {
        this.mTotalAppPowerDrain = 0.0d;
        this.mTotalAppRunningTime = 0L;
    }
}
