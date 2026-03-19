package com.android.internal.os;

import android.os.BatteryStats;

public class BluetoothPowerCalculator extends PowerCalculator {
    private static final boolean DEBUG = false;
    private static final String TAG = "BluetoothPowerCalculator";
    private double mAppTotalPowerMah = 0.0d;
    private long mAppTotalTimeMs = 0;
    private final double mIdleMa;
    private final double mRxMa;
    private final double mTxMa;

    public BluetoothPowerCalculator(PowerProfile powerProfile) {
        this.mIdleMa = powerProfile.getAveragePower(PowerProfile.POWER_BLUETOOTH_CONTROLLER_IDLE);
        this.mRxMa = powerProfile.getAveragePower(PowerProfile.POWER_BLUETOOTH_CONTROLLER_RX);
        this.mTxMa = powerProfile.getAveragePower(PowerProfile.POWER_BLUETOOTH_CONTROLLER_TX);
    }

    @Override
    public void calculateApp(BatterySipper batterySipper, BatteryStats.Uid uid, long j, long j2, int i) {
        BatteryStats.ControllerActivityCounter bluetoothControllerActivity = uid.getBluetoothControllerActivity();
        if (bluetoothControllerActivity != null) {
            long countLocked = bluetoothControllerActivity.getIdleTimeCounter().getCountLocked(i);
            long countLocked2 = bluetoothControllerActivity.getRxTimeCounter().getCountLocked(i);
            long countLocked3 = bluetoothControllerActivity.getTxTimeCounters()[0].getCountLocked(i);
            long j3 = countLocked + countLocked3 + countLocked2;
            double countLocked4 = bluetoothControllerActivity.getPowerCounter().getCountLocked(i) / 3600000.0d;
            if (countLocked4 == 0.0d) {
                countLocked4 = (((countLocked * this.mIdleMa) + (countLocked2 * this.mRxMa)) + (countLocked3 * this.mTxMa)) / 3600000.0d;
            }
            batterySipper.bluetoothPowerMah = countLocked4;
            batterySipper.bluetoothRunningTimeMs = j3;
            batterySipper.btRxBytes = uid.getNetworkActivityBytes(4, i);
            batterySipper.btTxBytes = uid.getNetworkActivityBytes(5, i);
            this.mAppTotalPowerMah += countLocked4;
            this.mAppTotalTimeMs += j3;
        }
    }

    @Override
    public void calculateRemaining(BatterySipper batterySipper, BatteryStats batteryStats, long j, long j2, int i) {
        BatteryStats.ControllerActivityCounter bluetoothControllerActivity = batteryStats.getBluetoothControllerActivity();
        long countLocked = bluetoothControllerActivity.getIdleTimeCounter().getCountLocked(i);
        long countLocked2 = bluetoothControllerActivity.getTxTimeCounters()[0].getCountLocked(i);
        long countLocked3 = bluetoothControllerActivity.getRxTimeCounter().getCountLocked(i);
        long j3 = countLocked + countLocked2 + countLocked3;
        double countLocked4 = bluetoothControllerActivity.getPowerCounter().getCountLocked(i) / 3600000.0d;
        if (countLocked4 == 0.0d) {
            countLocked4 = (((countLocked * this.mIdleMa) + (countLocked3 * this.mRxMa)) + (countLocked2 * this.mTxMa)) / 3600000.0d;
        }
        batterySipper.bluetoothPowerMah = Math.max(0.0d, countLocked4 - this.mAppTotalPowerMah);
        batterySipper.bluetoothRunningTimeMs = Math.max(0L, j3 - this.mAppTotalTimeMs);
    }

    @Override
    public void reset() {
        this.mAppTotalPowerMah = 0.0d;
        this.mAppTotalTimeMs = 0L;
    }
}
