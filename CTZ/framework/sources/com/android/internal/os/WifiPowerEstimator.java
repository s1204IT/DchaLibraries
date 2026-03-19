package com.android.internal.os;

import android.os.BatteryStats;

public class WifiPowerEstimator extends PowerCalculator {
    private static final boolean DEBUG = false;
    private static final String TAG = "WifiPowerEstimator";
    private long mTotalAppWifiRunningTimeMs = 0;
    private final double mWifiPowerBatchScan;
    private final double mWifiPowerOn;
    private final double mWifiPowerPerPacket;
    private final double mWifiPowerScan;

    public WifiPowerEstimator(PowerProfile powerProfile) {
        this.mWifiPowerPerPacket = getWifiPowerPerPacket(powerProfile);
        this.mWifiPowerOn = powerProfile.getAveragePower(PowerProfile.POWER_WIFI_ON);
        this.mWifiPowerScan = powerProfile.getAveragePower(PowerProfile.POWER_WIFI_SCAN);
        this.mWifiPowerBatchScan = powerProfile.getAveragePower(PowerProfile.POWER_WIFI_BATCHED_SCAN);
    }

    private static double getWifiPowerPerPacket(PowerProfile powerProfile) {
        return (powerProfile.getAveragePower(PowerProfile.POWER_WIFI_ACTIVE) / 3600.0d) / 61.03515625d;
    }

    @Override
    public void calculateApp(BatterySipper batterySipper, BatteryStats.Uid uid, long j, long j2, int i) {
        BatteryStats.Uid uid2 = uid;
        long j3 = j;
        batterySipper.wifiRxPackets = uid2.getNetworkActivityPackets(2, i);
        batterySipper.wifiTxPackets = uid2.getNetworkActivityPackets(3, i);
        batterySipper.wifiRxBytes = uid2.getNetworkActivityBytes(2, i);
        batterySipper.wifiTxBytes = uid2.getNetworkActivityBytes(3, i);
        double d = (batterySipper.wifiRxPackets + batterySipper.wifiTxPackets) * this.mWifiPowerPerPacket;
        batterySipper.wifiRunningTimeMs = uid2.getWifiRunningTime(j3, i) / 1000;
        this.mTotalAppWifiRunningTimeMs += batterySipper.wifiRunningTimeMs;
        double d2 = (batterySipper.wifiRunningTimeMs * this.mWifiPowerOn) / 3600000.0d;
        double wifiScanTime = ((uid2.getWifiScanTime(j3, i) / 1000) * this.mWifiPowerScan) / 3600000.0d;
        int i2 = 0;
        double wifiBatchedScanTime = 0.0d;
        while (i2 < 5) {
            wifiBatchedScanTime += ((uid2.getWifiBatchedScanTime(i2, j3, i) / 1000) * this.mWifiPowerBatchScan) / 3600000.0d;
            i2++;
            uid2 = uid;
            j3 = j;
        }
        batterySipper.wifiPowerMah = d + d2 + wifiScanTime + wifiBatchedScanTime;
    }

    @Override
    public void calculateRemaining(BatterySipper batterySipper, BatteryStats batteryStats, long j, long j2, int i) {
        long globalWifiRunningTime = batteryStats.getGlobalWifiRunningTime(j, i) / 1000;
        double d = ((globalWifiRunningTime - this.mTotalAppWifiRunningTimeMs) * this.mWifiPowerOn) / 3600000.0d;
        batterySipper.wifiRunningTimeMs = globalWifiRunningTime;
        batterySipper.wifiPowerMah = Math.max(0.0d, d);
    }

    @Override
    public void reset() {
        this.mTotalAppWifiRunningTimeMs = 0L;
    }
}
