package com.android.internal.os;

import android.os.BatteryStats;

public class BatterySipper implements Comparable<BatterySipper> {
    public double audioPowerMah;
    public long audioTimeMs;
    public double bluetoothPowerMah;
    public long bluetoothRunningTimeMs;
    public long btRxBytes;
    public long btTxBytes;
    public double cameraPowerMah;
    public long cameraTimeMs;
    public long cpuFgTimeMs;
    public double cpuPowerMah;
    public long cpuTimeMs;
    public DrainType drainType;
    public double flashlightPowerMah;
    public long flashlightTimeMs;
    public double gpsPowerMah;
    public long gpsTimeMs;
    public String[] mPackages;
    public long mobileActive;
    public int mobileActiveCount;
    public double mobileRadioPowerMah;
    public long mobileRxBytes;
    public long mobileRxPackets;
    public long mobileTxBytes;
    public long mobileTxPackets;
    public double mobilemspp;
    public double noCoveragePercent;
    public String packageWithHighestDrain;
    public double percent;
    public double proportionalSmearMah;
    public double screenPowerMah;
    public double sensorPowerMah;
    public boolean shouldHide;
    public double totalPowerMah;
    public double totalSmearedPowerMah;
    public BatteryStats.Uid uidObj;
    public double usagePowerMah;
    public long usageTimeMs;
    public int userId;
    public double videoPowerMah;
    public long videoTimeMs;
    public double wakeLockPowerMah;
    public long wakeLockTimeMs;
    public double wifiPowerMah;
    public long wifiRunningTimeMs;
    public long wifiRxBytes;
    public long wifiRxPackets;
    public long wifiTxBytes;
    public long wifiTxPackets;

    public enum DrainType {
        AMBIENT_DISPLAY,
        APP,
        BLUETOOTH,
        CAMERA,
        CELL,
        FLASHLIGHT,
        IDLE,
        MEMORY,
        OVERCOUNTED,
        PHONE,
        SCREEN,
        UNACCOUNTED,
        USER,
        WIFI
    }

    public BatterySipper(DrainType drainType, BatteryStats.Uid uid, double d) {
        this.totalPowerMah = d;
        this.drainType = drainType;
        this.uidObj = uid;
    }

    public void computeMobilemspp() {
        long j = this.mobileRxPackets + this.mobileTxPackets;
        this.mobilemspp = j > 0 ? this.mobileActive / j : 0.0d;
    }

    @Override
    public int compareTo(BatterySipper batterySipper) {
        if (this.drainType != batterySipper.drainType) {
            if (this.drainType == DrainType.OVERCOUNTED) {
                return 1;
            }
            if (batterySipper.drainType == DrainType.OVERCOUNTED) {
                return -1;
            }
        }
        return Double.compare(batterySipper.totalPowerMah, this.totalPowerMah);
    }

    public String[] getPackages() {
        return this.mPackages;
    }

    public int getUid() {
        if (this.uidObj == null) {
            return 0;
        }
        return this.uidObj.getUid();
    }

    public void add(BatterySipper batterySipper) {
        this.totalPowerMah += batterySipper.totalPowerMah;
        this.usageTimeMs += batterySipper.usageTimeMs;
        this.usagePowerMah += batterySipper.usagePowerMah;
        this.audioTimeMs += batterySipper.audioTimeMs;
        this.cpuTimeMs += batterySipper.cpuTimeMs;
        this.gpsTimeMs += batterySipper.gpsTimeMs;
        this.wifiRunningTimeMs += batterySipper.wifiRunningTimeMs;
        this.cpuFgTimeMs += batterySipper.cpuFgTimeMs;
        this.videoTimeMs += batterySipper.videoTimeMs;
        this.wakeLockTimeMs += batterySipper.wakeLockTimeMs;
        this.cameraTimeMs += batterySipper.cameraTimeMs;
        this.flashlightTimeMs += batterySipper.flashlightTimeMs;
        this.bluetoothRunningTimeMs += batterySipper.bluetoothRunningTimeMs;
        this.mobileRxPackets += batterySipper.mobileRxPackets;
        this.mobileTxPackets += batterySipper.mobileTxPackets;
        this.mobileActive += batterySipper.mobileActive;
        this.mobileActiveCount += batterySipper.mobileActiveCount;
        this.wifiRxPackets += batterySipper.wifiRxPackets;
        this.wifiTxPackets += batterySipper.wifiTxPackets;
        this.mobileRxBytes += batterySipper.mobileRxBytes;
        this.mobileTxBytes += batterySipper.mobileTxBytes;
        this.wifiRxBytes += batterySipper.wifiRxBytes;
        this.wifiTxBytes += batterySipper.wifiTxBytes;
        this.btRxBytes += batterySipper.btRxBytes;
        this.btTxBytes += batterySipper.btTxBytes;
        this.audioPowerMah += batterySipper.audioPowerMah;
        this.wifiPowerMah += batterySipper.wifiPowerMah;
        this.gpsPowerMah += batterySipper.gpsPowerMah;
        this.cpuPowerMah += batterySipper.cpuPowerMah;
        this.sensorPowerMah += batterySipper.sensorPowerMah;
        this.mobileRadioPowerMah += batterySipper.mobileRadioPowerMah;
        this.wakeLockPowerMah += batterySipper.wakeLockPowerMah;
        this.cameraPowerMah += batterySipper.cameraPowerMah;
        this.flashlightPowerMah += batterySipper.flashlightPowerMah;
        this.bluetoothPowerMah += batterySipper.bluetoothPowerMah;
        this.screenPowerMah += batterySipper.screenPowerMah;
        this.videoPowerMah += batterySipper.videoPowerMah;
        this.proportionalSmearMah += batterySipper.proportionalSmearMah;
        this.totalSmearedPowerMah += batterySipper.totalSmearedPowerMah;
    }

    public double sumPower() {
        this.totalPowerMah = this.usagePowerMah + this.wifiPowerMah + this.gpsPowerMah + this.cpuPowerMah + this.sensorPowerMah + this.mobileRadioPowerMah + this.wakeLockPowerMah + this.cameraPowerMah + this.flashlightPowerMah + this.bluetoothPowerMah + this.audioPowerMah + this.videoPowerMah;
        this.totalSmearedPowerMah = this.totalPowerMah + this.screenPowerMah + this.proportionalSmearMah;
        return this.totalPowerMah;
    }
}
