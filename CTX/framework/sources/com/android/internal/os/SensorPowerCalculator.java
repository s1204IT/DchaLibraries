package com.android.internal.os;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.BatteryStats;
import android.util.SparseArray;
import java.util.List;

public class SensorPowerCalculator extends PowerCalculator {
    private final double mGpsPower;
    private final List<Sensor> mSensors;

    public SensorPowerCalculator(PowerProfile powerProfile, SensorManager sensorManager, BatteryStats batteryStats, long j, int i) {
        this.mSensors = sensorManager.getSensorList(-1);
        this.mGpsPower = getAverageGpsPower(powerProfile, batteryStats, j, i);
    }

    @Override
    public void calculateApp(BatterySipper batterySipper, BatteryStats.Uid uid, long j, long j2, int i) {
        SparseArray<? extends BatteryStats.Uid.Sensor> sensorStats = uid.getSensorStats();
        int size = sensorStats.size();
        for (int i2 = 0; i2 < size; i2++) {
            BatteryStats.Uid.Sensor sensorValueAt = sensorStats.valueAt(i2);
            int iKeyAt = sensorStats.keyAt(i2);
            long totalTimeLocked = sensorValueAt.getSensorTime().getTotalTimeLocked(j, i) / 1000;
            if (iKeyAt == -10000) {
                batterySipper.gpsTimeMs = totalTimeLocked;
                batterySipper.gpsPowerMah = (batterySipper.gpsTimeMs * this.mGpsPower) / 3600000.0d;
            } else {
                int size2 = this.mSensors.size();
                int i3 = 0;
                while (true) {
                    if (i3 < size2) {
                        Sensor sensor = this.mSensors.get(i3);
                        if (sensor.getHandle() == iKeyAt) {
                            batterySipper.sensorPowerMah += (double) ((totalTimeLocked * sensor.getPower()) / 3600000.0f);
                            break;
                        }
                        i3++;
                    }
                }
            }
        }
    }

    private double getAverageGpsPower(PowerProfile powerProfile, BatteryStats batteryStats, long j, int i) {
        double averagePowerOrDefault = powerProfile.getAveragePowerOrDefault(PowerProfile.POWER_GPS_ON, -1.0d);
        if (averagePowerOrDefault != -1.0d) {
            return averagePowerOrDefault;
        }
        long j2 = 0;
        double averagePower = 0.0d;
        for (int i2 = 0; i2 < 2; i2++) {
            long gpsSignalQualityTime = batteryStats.getGpsSignalQualityTime(i2, j, i);
            j2 += gpsSignalQualityTime;
            averagePower += powerProfile.getAveragePower(PowerProfile.POWER_GPS_SIGNAL_QUALITY_BASED, i2) * gpsSignalQualityTime;
        }
        if (j2 != 0) {
            return averagePower / j2;
        }
        return 0.0d;
    }
}
