package com.android.internal.os;

import android.os.BatteryStats;

public class MobileRadioPowerCalculator extends PowerCalculator {
    private static final boolean DEBUG = false;
    private static final String TAG = "MobileRadioPowerController";
    private final double mPowerRadioOn;
    private final double mPowerScan;
    private BatteryStats mStats;
    private final double[] mPowerBins = new double[5];
    private long mTotalAppMobileActiveMs = 0;

    private double getMobilePowerPerPacket(long j, int i) {
        double d;
        double d2 = this.mPowerRadioOn / 3600.0d;
        long networkActivityPackets = this.mStats.getNetworkActivityPackets(0, i) + this.mStats.getNetworkActivityPackets(1, i);
        long mobileRadioActiveTime = this.mStats.getMobileRadioActiveTime(j, i) / 1000;
        if (networkActivityPackets != 0 && mobileRadioActiveTime != 0) {
            d = networkActivityPackets / mobileRadioActiveTime;
        } else {
            d = 12.20703125d;
        }
        return (d2 / d) / 3600.0d;
    }

    public MobileRadioPowerCalculator(PowerProfile powerProfile, BatteryStats batteryStats) {
        double averagePowerOrDefault = powerProfile.getAveragePowerOrDefault(PowerProfile.POWER_RADIO_ACTIVE, -1.0d);
        if (averagePowerOrDefault != -1.0d) {
            this.mPowerRadioOn = averagePowerOrDefault;
        } else {
            double averagePower = powerProfile.getAveragePower(PowerProfile.POWER_MODEM_CONTROLLER_RX) + 0.0d;
            for (int i = 0; i < this.mPowerBins.length; i++) {
                averagePower += powerProfile.getAveragePower(PowerProfile.POWER_MODEM_CONTROLLER_TX, i);
            }
            this.mPowerRadioOn = averagePower / ((double) (this.mPowerBins.length + 1));
        }
        if (powerProfile.getAveragePowerOrDefault(PowerProfile.POWER_RADIO_ON, -1.0d) != -1.0d) {
            for (int i2 = 0; i2 < this.mPowerBins.length; i2++) {
                this.mPowerBins[i2] = powerProfile.getAveragePower(PowerProfile.POWER_RADIO_ON, i2);
            }
        } else {
            double averagePower2 = powerProfile.getAveragePower(PowerProfile.POWER_MODEM_CONTROLLER_IDLE);
            this.mPowerBins[0] = (25.0d * averagePower2) / 180.0d;
            for (int i3 = 1; i3 < this.mPowerBins.length; i3++) {
                this.mPowerBins[i3] = Math.max(1.0d, averagePower2 / 256.0d);
            }
        }
        this.mPowerScan = powerProfile.getAveragePowerOrDefault(PowerProfile.POWER_RADIO_SCANNING, 0.0d);
        this.mStats = batteryStats;
    }

    @Override
    public void calculateApp(BatterySipper batterySipper, BatteryStats.Uid uid, long j, long j2, int i) {
        batterySipper.mobileRxPackets = uid.getNetworkActivityPackets(0, i);
        batterySipper.mobileTxPackets = uid.getNetworkActivityPackets(1, i);
        batterySipper.mobileActive = uid.getMobileRadioActiveTime(i) / 1000;
        batterySipper.mobileActiveCount = uid.getMobileRadioActiveCount(i);
        batterySipper.mobileRxBytes = uid.getNetworkActivityBytes(0, i);
        batterySipper.mobileTxBytes = uid.getNetworkActivityBytes(1, i);
        if (batterySipper.mobileActive > 0) {
            this.mTotalAppMobileActiveMs += batterySipper.mobileActive;
            batterySipper.mobileRadioPowerMah = (batterySipper.mobileActive * this.mPowerRadioOn) / 3600000.0d;
        } else {
            batterySipper.mobileRadioPowerMah = (batterySipper.mobileRxPackets + batterySipper.mobileTxPackets) * getMobilePowerPerPacket(j, i);
        }
    }

    @Override
    public void calculateRemaining(BatterySipper batterySipper, BatteryStats batteryStats, long j, long j2, int i) {
        int i2 = 0;
        double d = 0.0d;
        long j3 = 0;
        long j4 = 0;
        while (i2 < this.mPowerBins.length) {
            long phoneSignalStrengthTime = batteryStats.getPhoneSignalStrengthTime(i2, j, i) / 1000;
            long j5 = j4;
            d += (phoneSignalStrengthTime * this.mPowerBins[i2]) / 3600000.0d;
            j3 += phoneSignalStrengthTime;
            j4 = i2 == 0 ? phoneSignalStrengthTime : j5;
            i2++;
        }
        long j6 = j4;
        double phoneSignalScanningTime = d + (((batteryStats.getPhoneSignalScanningTime(j, i) / 1000) * this.mPowerScan) / 3600000.0d);
        long mobileRadioActiveTime = (this.mStats.getMobileRadioActiveTime(j, i) / 1000) - this.mTotalAppMobileActiveMs;
        if (mobileRadioActiveTime > 0) {
            phoneSignalScanningTime += (this.mPowerRadioOn * mobileRadioActiveTime) / 3600000.0d;
        }
        if (phoneSignalScanningTime != 0.0d) {
            if (j3 != 0) {
                batterySipper.noCoveragePercent = (j6 * 100.0d) / j3;
            }
            batterySipper.mobileActive = mobileRadioActiveTime;
            batterySipper.mobileActiveCount = batteryStats.getMobileRadioActiveUnknownCount(i);
            batterySipper.mobileRadioPowerMah = phoneSignalScanningTime;
        }
    }

    @Override
    public void reset() {
        this.mTotalAppMobileActiveMs = 0L;
    }

    public void reset(BatteryStats batteryStats) {
        reset();
        this.mStats = batteryStats;
    }
}
